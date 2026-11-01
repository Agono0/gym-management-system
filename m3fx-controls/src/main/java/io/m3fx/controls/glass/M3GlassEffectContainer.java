package io.m3fx.controls.glass;

import io.m3fx.controls.internal.MotionUtil;
import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import io.m3fx.core.token.ShapeTokens;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.QuadCurveTo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Liquid Glass effect container (Apple iOS 26 {@code GlassEffectContainer(spacing:)}).
 *
 * <p>Glass children of this {@link Pane} that come closer than {@link #spacingProperty()} blend
 * their glass shapes together: the container computes the union outline of each merged group and
 * renders a shared glass "bridge" surface (translucent fill + rim) behind the members so the group
 * reads as one continuous blob instead of separate panes. The container also drives the
 * appear/disappear morph (the {@code glassEffectID} + {@code @Namespace} pattern): panes are
 * registered under an id with {@link #registerGlass(Node, Object)}, and
 * {@link #morphTo(Object, Object)} requests that the glass of the {@code fromId} pane stretch over
 * to the {@code toId} pane the next time that remove/add pair happens while the request is
 * pending; {@link GlassMorph#morph(M3GlassPane, M3GlassPane)} performs the fluid animation.
 *
 * <p><strong>Geometry simplification (documented, honest):</strong> the merged outline is the
 * rounded <em>convex hull</em> of the member rounded rects with an area-weighted interpolated
 * corner radius &mdash; not a true metaball / signed-distance-field blend. Two and three member
 * merges are the exercised cases (the hull builder is size-agnostic and handles more), and two
 * same-size panes produce the familiar capsule blob. Concave "neck" curves where two blobs join,
 * like Apple's metaball rendering, are out of scope for this layer.
 *
 * <p>All geometry is recomputed on layout; all mutation must happen on the JavaFX Application
 * Thread.
 */
// Suppressed this-escape (same false positives javac 27 cannot see across the module boundary as
// in every other m3fx control): the JavaFX property-bean idiom new Simple*Property(this, ...) and
// constructor calls to JavaFX final methods (getChildren(), getStyleClass()). This class's own
// overridable methods are never called during construction.
@SuppressWarnings("this-escape")
public class M3GlassEffectContainer extends Pane {

    /** Default blend distance in px (Apple's {@code GlassEffectContainer(spacing:)} default). */
    public static final double DEFAULT_SPACING = 40.0;

    /**
     * Alpha of the shared bridge fill drawn behind merged members (tuned default; Apple does not
     * publish a scrim value for the merged surface).
     */
    static final double BRIDGE_FILL_ALPHA = 0.35;

    /** Rim alpha of the shared bridge outline (spec: 1px edge stroke at 0.45 of outline-variant). */
    static final double BRIDGE_RIM_ALPHA = 0.45;

    /** One merged member rect in container coordinates with its glass corner radius. */
    private record MemberRect(double minX, double minY, double width, double height, double radius) {}

    /** A pending morph request (the glassEffectID pair). */
    private record MorphRequest(Object fromId, Object toId) {}

    private final DoubleProperty spacing =
            new SimpleDoubleProperty(this, "spacing", DEFAULT_SPACING);

    /** Registered morph ids. Registration survives removal so a pane can be re-added. */
    private final Map<Object, Node> morphIds = new HashMap<>();

    /** Last known scene bounds of glass children, stashed so a removed pane can still morph. */
    private final Map<Node, Bounds> lastSceneBounds = new WeakHashMap<>();

    private MorphRequest pending;
    private Node detachedFrom;
    private Node pendingTo;
    private ChangeListener<Bounds> pendingToLayoutGuard;

    private boolean geometryDirty = true;
    private List<List<Node>> mergedGroups = List.of();
    private List<List<MemberRect>> mergedRects = List.of();

    /** Shared glass surfaces for merged groups, drawn below all glass children. */
    private final Pane bridgeLayer = new Pane();

    /** Creates an empty glass effect container. */
    public M3GlassEffectContainer() {
        getStyleClass().add("glass-effect-container");
        bridgeLayer.getStyleClass().add("glass-merge-bridge-layer");
        bridgeLayer.setMouseTransparent(true);
        bridgeLayer.setManaged(false);
        getChildren().add(0, bridgeLayer);

        spacing.addListener((obs, oldV, newV) -> {
            geometryDirty = true;
            requestLayout();
        });
        getChildren().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    for (Node removed : change.getRemoved()) {
                        if (removed == bridgeLayer) {
                            continue;
                        }
                        if (pending != null && morphIds.get(pending.fromId()) == removed) {
                            detachedFrom = removed;
                        }
                    }
                }
                if (change.wasAdded()) {
                    for (Node added : change.getAddedSubList()) {
                        if (added == bridgeLayer) {
                            continue;
                        }
                        if (pending != null && morphIds.get(pending.toId()) == added) {
                            pendingTo = added;
                        }
                    }
                }
            }
            geometryDirty = true;
            tryFireMorph();
            requestLayout();
        });
    }

    // ------------------------------------------------------------------
    // Merging
    // ------------------------------------------------------------------

    /**
     * Blend distance: glass children whose bounds are closer than this (or overlapping) merge
     * their glass shapes into one shared surface. Default {@value #DEFAULT_SPACING}.
     *
     * @return the spacing property in px
     */
    public DoubleProperty spacingProperty() {
        return spacing;
    }

    public double getSpacing() {
        return spacing.get();
    }

    public void setSpacing(double value) {
        spacing.set(value);
    }

    /**
     * True when {@code node} participates in merging: any {@link M3GlassPane}, or any node whose
     * style class list contains {@code "glass-pane"} (custom glass renderers opt in via CSS).
     *
     * @param node the candidate node
     * @return whether the container treats it as a glass node
     */
    public static boolean isGlassNode(Node node) {
        return node instanceof M3GlassPane || node.getStyleClass().contains("glass-pane");
    }

    /**
     * The glass children currently merged into shared surfaces, recomputed on layout. Only groups
     * with two or more members are returned; a lone pane renders its own glass normally.
     *
     * @return unmodifiable list of unmodifiable member groups (never {@code null})
     */
    public List<List<Node>> getMergedGroups() {
        recomputeGeometryIfNeeded();
        return mergedGroups;
    }

    /**
     * The merged group the given glass child belongs to, or an empty list when it is not merged.
     *
     * @param glass a child of this container
     * @return the group members including {@code glass}, or an empty list
     */
    public List<Node> mergedGroupOf(Node glass) {
        recomputeGeometryIfNeeded();
        for (List<Node> group : mergedGroups) {
            if (group.contains(glass)) {
                return group;
            }
        }
        return List.of();
    }

    /**
     * A snapshot of the shared union outline for the merged group containing {@code glass}, in
     * <em>this container's</em> coordinate space, or {@code null} when the node is not merged.
     * Each call rebuilds the {@link Path} from the current geometry; a skin rendering the shared
     * rim from this must convert coordinates (e.g. {@code container.localToScene(...)} then
     * {@code pane.sceneToLocal(...)}) and re-query after layout. The fill/stroke styling of the
     * returned path is already set to the shared glass surface look.
     *
     * @param glass a child of this container
     * @return the union outline path, or {@code null} when the node is not part of a merged group
     */
    public Path getMergedOutline(Node glass) {
        recomputeGeometryIfNeeded();
        for (int i = 0; i < mergedGroups.size(); i++) {
            if (mergedGroups.get(i).contains(glass)) {
                return buildHullPath(mergedRects.get(i), weightedRadius(mergedRects.get(i)));
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Morphing (glassEffectID + namespace pattern)
    // ------------------------------------------------------------------

    /**
     * Registers a glass node under a morph id (the {@code glassEffectID(_:in:)} pattern). Both
     * panes of a morph pair must be registered before {@link #morphTo(Object, Object)} fires.
     * Registration survives removal from the container so a pane can be re-added later.
     *
     * @param glass the glass node (see {@link #isGlassNode(Node)})
     * @param morphId the application-wide morph identity
     */
    public void registerGlass(Node glass, Object morphId) {
        requireFxThread("registerGlass");
        morphIds.put(morphId, glass);
        if (pending != null && pendingTo == null && pending.toId().equals(morphId)) {
            pendingTo = glass;
            tryFireMorph();
        }
        if (pending != null && detachedFrom == null && pending.fromId().equals(morphId)
                && glass.getScene() == null) {
            detachedFrom = glass;
            tryFireMorph();
        }
    }

    /**
     * Removes a morph id registration.
     *
     * @param morphId the id to forget
     */
    public void unregisterGlass(Object morphId) {
        requireFxThread("unregisterGlass");
        morphIds.remove(morphId);
    }

    /**
     * Requests that the glass of the pane registered under {@code fromId} morph fluidly onto the
     * pane registered under {@code toId}. The morph fires as soon as both ends are known: when
     * the from-pane is removed and the to-pane is added (either order), or immediately when both
     * are currently children of the container. The pending request is consumed once fired;
     * calling this again replaces any pending request.
     *
     * <p>Typical appear/disappear flow:
     * <pre>{@code
     * container.registerGlass(paneA, "collapsed");
     * container.registerGlass(paneB, "expanded");
     * container.morphTo("collapsed", "expanded");
     * container.getChildren().remove(paneA);
     * container.getChildren().add(paneB);   // glass stretches from A's bounds to B's bounds
     * }</pre>
     *
     * @param fromId id of the pane whose glass disappears
     * @param toId id of the pane whose glass appears
     */
    public void morphTo(Object fromId, Object toId) {
        requireFxThread("morphTo");
        clearPendingMorph();
        pending = new MorphRequest(fromId, toId);
        detachedFrom = resolveDetached(fromId);
        pendingTo = resolvePresentChild(toId);
        tryFireMorph();
    }

    private Node resolveDetached(Object id) {
        Node node = morphIds.get(id);
        return node != null && node.getScene() == null ? node : null;
    }

    private Node resolvePresentChild(Object id) {
        Node node = morphIds.get(id);
        return node != null && getChildren().contains(node) ? node : null;
    }

    private void tryFireMorph() {
        if (pending == null) {
            return;
        }
        Node from = detachedFrom != null ? detachedFrom : morphIds.get(pending.fromId());
        Node to = pendingTo != null ? pendingTo : morphIds.get(pending.toId());
        if (from == null || to == null || from == to) {
            return;
        }
        if (to.getScene() == null) {
            return; // wait until the target pane joins the scene
        }
        Bounds toBounds = to.getBoundsInParent();
        if (toBounds.getWidth() <= 0 || toBounds.getHeight() <= 0) {
            // Target not laid out yet: retry on its first real bounds instead of morphing from 0x0.
            if (pendingToLayoutGuard == null) {
                pendingToLayoutGuard = (obs, oldB, newB) -> {
                    if (newB.getWidth() > 0 && newB.getHeight() > 0) {
                        to.boundsInParentProperty().removeListener(pendingToLayoutGuard);
                        pendingToLayoutGuard = null;
                        tryFireMorph();
                    }
                };
                to.boundsInParentProperty().addListener(pendingToLayoutGuard);
            }
            return;
        }
        Bounds start = usableStartBounds(from);
        if (start == null) {
            clearPendingMorph();
            return;
        }
        if (from instanceof M3GlassPane fromPane && to instanceof M3GlassPane toPane) {
            GlassMorph.morph(fromPane, toPane);
        } else {
            // Non-M3GlassPane glass nodes: reuse the tracked, scene-safe MotionUtil morph — the
            // target is laid out at its own place and appears to start where the from-pane was.
            MotionUtil.morphFromBounds(to, start, true, null);
        }
        clearPendingMorph();
    }

    private Bounds usableStartBounds(Node from) {
        if (from.getScene() != null) {
            Bounds inScene = from.localToScene(from.getBoundsInLocal());
            return usable(inScene) ? inScene : lastSceneBounds.get(from);
        }
        return lastSceneBounds.get(from);
    }

    private void clearPendingMorph() {
        if (pendingTo != null && pendingToLayoutGuard != null) {
            pendingTo.boundsInParentProperty().removeListener(pendingToLayoutGuard);
        }
        pendingToLayoutGuard = null;
        pending = null;
        detachedFrom = null;
        pendingTo = null;
    }

    private static void requireFxThread(String operation) {
        if (!javafx.application.Platform.isFxApplicationThread()) {
            throw new IllegalStateException(
                    "M3GlassEffectContainer." + operation
                    + " must be called on the JavaFX Application Thread");
        }
    }

    // ------------------------------------------------------------------
    // Layout + geometry
    // ------------------------------------------------------------------

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        if (bridgeLayer.getWidth() != w || bridgeLayer.getHeight() != h) {
            bridgeLayer.resizeRelocate(0, 0, w, h);
        }
        for (Node child : getChildren()) {
            if (child != bridgeLayer && isGlassNode(child) && child.getScene() != null) {
                Bounds inScene = child.localToScene(child.getBoundsInLocal());
                if (usable(inScene)) {
                    lastSceneBounds.put(child, inScene);
                }
            }
        }
        recomputeGeometryIfNeeded();
        rebuildBridge();
    }

    private void recomputeGeometryIfNeeded() {
        if (!geometryDirty) {
            return;
        }
        geometryDirty = false;

        List<Node> glass = new ArrayList<>();
        for (Node child : getChildren()) {
            if (child != bridgeLayer && isGlassNode(child)) {
                glass.add(child);
            }
        }
        int n = glass.size();
        List<MemberRect> rects = new ArrayList<>(n);
        for (Node node : glass) {
            Bounds b = node.getBoundsInParent();
            rects.add(new MemberRect(
                    b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight(), radiusOf(node)));
        }

        // Union-find over pairs whose gap (0 when overlapping) is closer than spacing.
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
        double limit = spacing.get();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (gap(rects.get(i), rects.get(j)) < limit) {
                    parent[findRoot(parent, i)] = findRoot(parent, j);
                }
            }
        }
        Map<Integer, List<Integer>> clusters = new HashMap<>();
        for (int i = 0; i < n; i++) {
            clusters.computeIfAbsent(findRoot(parent, i), k -> new ArrayList<>()).add(i);
        }

        List<List<Node>> groups = new ArrayList<>();
        List<List<MemberRect>> rectsByGroup = new ArrayList<>();
        for (List<Integer> cluster : clusters.values()) {
            if (cluster.size() < 2) {
                continue;
            }
            List<Node> members = new ArrayList<>(cluster.size());
            List<MemberRect> memberRects = new ArrayList<>(cluster.size());
            for (int idx : cluster) {
                members.add(glass.get(idx));
                memberRects.add(rects.get(idx));
            }
            groups.add(List.copyOf(members));
            rectsByGroup.add(List.copyOf(memberRects));
        }
        mergedGroups = List.copyOf(groups);
        mergedRects = List.copyOf(rectsByGroup);
    }

    private void rebuildBridge() {
        bridgeLayer.getChildren().clear();
        Color surface = ThemeColors.resolve(this, ColorRole.SURFACE_CONTAINER_HIGHEST);
        Color rim = ThemeColors.resolve(this, ColorRole.OUTLINE_VARIANT);
        Color fill = Color.color(surface.getRed(), surface.getGreen(), surface.getBlue(),
                BRIDGE_FILL_ALPHA);
        Color stroke = Color.color(rim.getRed(), rim.getGreen(), rim.getBlue(), BRIDGE_RIM_ALPHA);
        for (List<MemberRect> group : mergedRects) {
            Path outline = buildHullPath(group, weightedRadius(group));
            outline.setFill(fill);
            outline.setStroke(stroke);
            outline.setStrokeWidth(1.0);
            bridgeLayer.getChildren().add(outline);
        }
    }

    private static double radiusOf(Node node) {
        if (node instanceof M3GlassPane pane) {
            return Math.max(0, pane.getCornerRadius());
        }
        // Style-class-only glass nodes: fall back to the M3 large corner token.
        return ShapeTokens.LARGE;
    }

    /**
     * Gap between two rounded rects: 0 when they overlap, otherwise the shortest axis-aligned
     * distance between them.
     */
    private static double gap(MemberRect a, MemberRect b) {
        double dx = Math.max(0, Math.max(a.minX - (b.minX + b.width), b.minX - (a.minX + a.width)));
        double dy = Math.max(0, Math.max(a.minY - (b.minY + b.height), b.minY - (a.minY + a.height)));
        return Math.hypot(dx, dy);
    }

    private static int findRoot(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    /** Area-weighted average of the member corner radii (the "interpolated radius"). */
    private static double weightedRadius(List<MemberRect> rects) {
        double areaSum = 0;
        double weighted = 0;
        for (MemberRect r : rects) {
            double area = Math.max(0, r.width * r.height);
            areaSum += area;
            weighted += area * r.radius;
        }
        return areaSum > 0 ? weighted / areaSum : 0;
    }

    /**
     * Builds the merged outline: the convex hull of the member rect corners, rounded at every
     * hull vertex with quadratic curves of the given radius (clamped per corner to half the
     * adjacent edge, so degenerate hulls stay valid). Handles 2, 3 and more members uniformly.
     */
    private static Path buildHullPath(List<MemberRect> rects, double radius) {
        List<double[]> points = new ArrayList<>(rects.size() * 4);
        for (MemberRect r : rects) {
            double x1 = r.minX;
            double y1 = r.minY;
            double x2 = r.minX + r.width;
            double y2 = r.minY + r.height;
            points.add(new double[] {x1, y1});
            points.add(new double[] {x2, y1});
            points.add(new double[] {x2, y2});
            points.add(new double[] {x1, y2});
        }
        List<double[]> hull = convexHull(points);
        Path path = new Path();
        if (hull.size() < 3) {
            // Degenerate (all members identical): draw the first rect.
            MemberRect r = rects.get(0);
            path.getElements().addAll(roundedRectElements(r.minX, r.minY, r.width, r.height,
                    Math.min(r.radius, Math.min(r.width, r.height) / 2)));
            return path;
        }
        int k = hull.size();
        for (int i = 0; i < k; i++) {
            double[] prev = hull.get((i + k - 1) % k);
            double[] v = hull.get(i);
            double[] next = hull.get((i + 1) % k);
            double rIn = Math.min(radius, dist(v, prev) / 2);
            double rOut = Math.min(radius, dist(v, next) / 2);
            double[] a = lerpToward(v, prev, rIn);   // point before the corner on the incoming edge
            double[] b = lerpToward(v, next, rOut);  // point after the corner on the outgoing edge
            if (i == 0) {
                path.getElements().add(new MoveTo(b[0], b[1]));
            } else {
                path.getElements().add(new LineTo(a[0], a[1]));
                path.getElements().add(new QuadCurveTo(v[0], v[1], b[0], b[1]));
            }
        }
        // Close with the rounding of vertex 0 (the path started at its outgoing point).
        double[] prev = hull.get(k - 1);
        double[] v0 = hull.get(0);
        double[] next = hull.get(1);
        double[] a0 = lerpToward(v0, prev, Math.min(radius, dist(v0, prev) / 2));
        path.getElements().add(new LineTo(a0[0], a0[1]));
        path.getElements().add(new QuadCurveTo(v0[0], v0[1],
                lerpToward(v0, next, Math.min(radius, dist(v0, next) / 2))[0],
                lerpToward(v0, next, Math.min(radius, dist(v0, next) / 2))[1]));
        return path;
    }

    private static List<PathElement> roundedRectElements(
            double x, double y, double w, double h, double r) {
        double rr = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        List<PathElement> elements = new ArrayList<>(8);
        elements.add(new MoveTo(x + rr, y));
        elements.add(new LineTo(x + w - rr, y));
        elements.add(new QuadCurveTo(x + w, y, x + w, y + rr));
        elements.add(new LineTo(x + w, y + h - rr));
        elements.add(new QuadCurveTo(x + w, y + h, x + w - rr, y + h));
        elements.add(new LineTo(x + rr, y + h));
        elements.add(new QuadCurveTo(x, y + h, x, y + h - rr));
        elements.add(new LineTo(x, y + rr));
        elements.add(new QuadCurveTo(x, y, x + rr, y));
        return elements;
    }

    /** Andrew's monotone chain convex hull (counter-clockwise, no collinear points kept). */
    private static List<double[]> convexHull(List<double[]> points) {
        List<double[]> sorted = new ArrayList<>(points);
        sorted.sort((p, q) -> p[0] != q[0] ? Double.compare(p[0], q[0]) : Double.compare(p[1], q[1]));
        if (sorted.size() < 3) {
            return sorted;
        }
        List<double[]> hull = new ArrayList<>();
        // Lower hull.
        for (double[] p : sorted) {
            while (hull.size() >= 2 && cross(hull.get(hull.size() - 2), hull.get(hull.size() - 1), p) <= 0) {
                hull.remove(hull.size() - 1);
            }
            hull.add(p);
        }
        // Upper hull.
        int lower = hull.size() + 1;
        for (int i = sorted.size() - 2; i >= 0; i--) {
            double[] p = sorted.get(i);
            while (hull.size() >= lower && cross(hull.get(hull.size() - 2), hull.get(hull.size() - 1), p) <= 0) {
                hull.remove(hull.size() - 1);
            }
            hull.add(p);
        }
        hull.remove(hull.size() - 1); // last point == first point
        return hull;
    }

    private static double cross(double[] o, double[] a, double[] b) {
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
    }

    private static double dist(double[] a, double[] b) {
        return Math.hypot(a[0] - b[0], a[1] - b[1]);
    }

    /** The point {@code d} units from {@code from} toward {@code to}. */
    private static double[] lerpToward(double[] from, double[] to, double d) {
        double len = dist(from, to);
        if (len == 0) {
            return from.clone();
        }
        double t = Math.min(1, d / len);
        return new double[] {from[0] + (to[0] - from[0]) * t, from[1] + (to[1] - from[1]) * t};
    }

    private static boolean usable(Bounds b) {
        return b != null && b.getWidth() > 0 && b.getHeight() > 0
                && Double.isFinite(b.getMinX()) && Double.isFinite(b.getMinY())
                && Double.isFinite(b.getWidth()) && Double.isFinite(b.getHeight());
    }
}
