package io.m3fx.controls.glass;

import io.m3fx.controls.common.SpringParams;
import io.m3fx.controls.internal.SpringAnimation;
import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import io.m3fx.core.token.MotionTokens;
import io.m3fx.core.token.ShapeTokens;
import io.m3fx.core.token.SpringInterpolator;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Fluid morph animations for Liquid Glass (the appear/disappear half of Apple's
 * {@code glassEffectID} + {@code @Namespace} pattern).
 *
 * <p>{@link #morph(M3GlassPane, M3GlassPane)} animates a full-size ghost pane between the two
 * panes' bounds while cross-fading from/to: the glass visually stretches from where the old pane
 * was to where the new pane lands, instead of popping. {@link #morphToBounds(Node, Bounds)}
 * performs the toolbar-style variant where a single glass node springs onto an arbitrary scene
 * rect. All motion is driven by {@link SpringAnimation} with the
 * {@link MotionTokens#fluidSpring()} tuning and is transform-only (translate/scale/opacity), so
 * it never fights layout.
 *
 * <p>All methods are FX-thread only, cancel-safe (a repeated call cancels the previous ghost for
 * the same target), and stop automatically when the target leaves the scene.
 */
public final class GlassMorph {

    /**
     * Node property key ({@link Node#getProperties()}) under which the container stashes a glass
     * node's last known scene bounds, so a pane that was just REMOVED from the scene graph can
     * still act as the morph source. Value type {@link Bounds} in scene coordinates.
     */
    public static final String LAST_SCENE_BOUNDS_KEY = "m3fx.glass.last-scene-bounds";

    /**
     * The FLUID morph spring, derived from the {@link MotionTokens#fluidSpring()} interpolator:
     * stiffness = &omega;&sup2; (unit mass) so one second of real-time physics reproduces the
     * interpolator's damped oscillator, damping ratio = the interpolator's. Result: stiffness 36,
     * damping ratio 0.82 &mdash; a smooth, liquid settle of roughly 0.8s with minimal overshoot.
     */
    public static final SpringParams FLUID = fluidParams();

    private static final String GHOST_KEY = "m3fx.glass.morph-ghost";
    private static final String SCENE_GUARD_KEY = "m3fx.glass.morph-scene-guard";

    /** Ghost fill alpha (spec surface glass feel; tuned, Apple does not publish values). */
    private static final double GHOST_FILL_ALPHA = 0.55;
    /** Ghost rim alpha (spec: 1px edge stroke at 0.45 of outline-variant). */
    private static final double GHOST_RIM_ALPHA = 0.45;
    /** Ghost shadow per spec defaults: 0.30 opacity / 10 radius / +1 offsetY. */
    private static final double GHOST_SHADOW_OPACITY = 0.30;
    private static final double GHOST_SHADOW_RADIUS = 10.0;
    private static final double GHOST_SHADOW_OFFSET_Y = 1.0;

    private GlassMorph() {}

    private static SpringParams fluidParams() {
        SpringInterpolator fluid = (SpringInterpolator) MotionTokens.fluidSpring();
        double omega = fluid.getNaturalFrequency();
        return new SpringParams(omega * omega, fluid.getDampingRatio());
    }

    // ------------------------------------------------------------------
    // Pane-to-pane morph (container appear/disappear)
    // ------------------------------------------------------------------

    /**
     * Morphs the glass of {@code from} onto {@code to}: a full-size ghost pane sized like
     * {@code from} springs from {@code from}'s bounds to {@code to}'s bounds with the
     * {@link #FLUID} spring while {@code to} cross-fades in underneath and {@code from} (when
     * still attached to the scene) cross-fades out; the ghost is then removed.
     *
     * <p>Bounds: {@code from}'s are taken from the live scene when attached, otherwise from the
     * {@link #LAST_SCENE_BOUNDS_KEY} stash (the container caches them each layout pass, which is
     * how a just-removed pane can still be the morph source). When {@code from} is still present
     * its opacity is restored to 1.0 at the end &mdash; the cross-fade is visual only.
     *
     * @param from the pane whose glass disappears (may already be detached)
     * @param to the pane whose glass appears (must be in a scene)
     */
    public static void morph(M3GlassPane from, M3GlassPane to) {
        requireFxThread("morph");
        if (from == null || to == null || from == to) {
            return;
        }
        if (to.getScene() == null) {
            return;
        }
        Pane host = hostOf(to);
        if (host == null) {
            return;
        }
        cancelGhost(to);

        Bounds startScene = usableStartBounds(from);
        Bounds targetScene = to.localToScene(to.getBoundsInLocal());
        if (startScene == null || !usable(targetScene)) {
            return;
        }
        Bounds start = host.sceneToLocal(startScene);
        Bounds target = host.sceneToLocal(targetScene);
        if (start == null || target == null || !usable(start) || !usable(target)) {
            return;
        }

        Region ghost = buildGhost(from);
        ghost.setManaged(false);
        ghost.resize(start.getWidth(), start.getHeight());
        ghost.relocate(start.getMinX(), start.getMinY());
        host.getChildren().add(ghost);
        to.getProperties().put(GHOST_KEY, ghost);
        installSceneGuard(to);

        // Transform-only travel: scale about the ghost's layout-bounds center (JavaFX pivot) plus
        // the center-to-center translation lands the ghost exactly on the target rect.
        double sx = target.getWidth() / start.getWidth();
        double sy = target.getHeight() / start.getHeight();
        double tx = centerX(target) - centerX(start);
        double ty = centerY(target) - centerY(start);

        // Cross-fade: the target fades in under the arriving ghost; the source fades out while
        // its look travels (restored to full opacity at the end when it stays attached).
        to.setOpacity(0.0);
        SpringAnimation.animate(to.opacityProperty(), 1.0, FLUID, null);
        boolean fromAttached = from.getScene() != null;
        if (fromAttached) {
            SpringAnimation.animate(from.opacityProperty(), 0.0, FLUID, null);
        }

        boolean[] done = {false};
        Runnable finish = () -> {
            if (done[0]) {
                return;
            }
            done[0] = true;
            SpringAnimation.animate(ghost.opacityProperty(), 0.0, SpringAnimation.GENTLE, () -> {
                SpringAnimation.stopTransforms(ghost);
                SpringAnimation.stop(ghost.opacityProperty());
                Pane ghostHost = ghost.getParent() instanceof Pane p ? p : null;
                if (ghostHost != null) {
                    ghostHost.getChildren().remove(ghost);
                }
                to.getProperties().remove(GHOST_KEY);
            });
            SpringAnimation.animate(to.opacityProperty(), 1.0, SpringAnimation.GENTLE, null);
            if (fromAttached) {
                SpringAnimation.animate(from.opacityProperty(), 1.0, SpringAnimation.GENTLE, null);
            }
        };
        SpringAnimation.animate(ghost.translateXProperty(), tx, FLUID, finish);
        SpringAnimation.animate(ghost.translateYProperty(), ty, FLUID, null);
        SpringAnimation.animate(ghost.scaleXProperty(), sx, FLUID, null);
        SpringAnimation.animate(ghost.scaleYProperty(), sy, FLUID, null);
    }

    // ------------------------------------------------------------------
    // Toolbar-style morph (single node to a target rect)
    // ------------------------------------------------------------------

    /**
     * Springs the glass node from its current on-screen rect onto {@code target} (scene
     * coordinates), transform-only, with the {@link #FLUID} spring. When the spring settles the
     * node is still at its natural layout place with translate/scale transforms applied on top;
     * callers either keep the transforms (overlay-style expansion) or re-layout to
     * {@code target} and call {@link #resetMorph(Node)} to hand over to real layout.
     *
     * <p>Note: {@code internal/MotionUtil.morphFromBounds} was considered for this but animates
     * the opposite direction (start at given bounds, settle at the node's natural place, fixed
     * Apple-sheet timeline), so a dedicated FLUID-spring implementation is used here; the
     * container's fallback path for non-{@link M3GlassPane} glass nodes does reuse
     * {@code morphFromBounds} where its direction fits. Same untransformed-ancestors assumption
     * as {@code morphFromBounds}: with transformed ancestors the target rect lands offset by the
     * ancestor transform.
     *
     * @param glass the glass node to move (must be in a scene)
     * @param target the destination rect in scene coordinates
     */
    public static void morphToBounds(Node glass, Bounds target) {
        requireFxThread("morphToBounds");
        if (glass == null || target == null || glass.getScene() == null || !usable(target)) {
            return;
        }
        Bounds current = glass.localToScene(glass.getBoundsInLocal());
        if (!usable(current)) {
            return;
        }
        installSceneGuard(glass);
        SpringAnimation.animate(glass.translateXProperty(),
                centerX(target) - centerX(current), FLUID, null);
        SpringAnimation.animate(glass.translateYProperty(),
                centerY(target) - centerY(current), FLUID, null);
        SpringAnimation.animate(glass.scaleXProperty(),
                target.getWidth() / current.getWidth(), FLUID, null);
        SpringAnimation.animate(glass.scaleYProperty(),
                target.getHeight() / current.getHeight(), FLUID, null);
    }

    /**
     * Stops any in-flight morph on the node and restores identity transforms and full opacity.
     *
     * @param glass the node to reset
     */
    public static void resetMorph(Node glass) {
        requireFxThread("resetMorph");
        if (glass == null) {
            return;
        }
        cancelGhost(glass);
        stopMorphTransforms(glass);
        glass.setTranslateX(0);
        glass.setTranslateY(0);
        glass.setScaleX(1.0);
        glass.setScaleY(1.0);
        glass.setOpacity(1.0);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private static Pane hostOf(Node node) {
        if (node.getParent() instanceof Pane parent) {
            return parent;
        }
        return node.getScene().getRoot() instanceof Pane root ? root : null;
    }

    private static Bounds usableStartBounds(Node from) {
        if (from.getScene() != null) {
            Bounds inScene = from.localToScene(from.getBoundsInLocal());
            return usable(inScene) ? inScene : null;
        }
        Object stashed = from.getProperties().get(LAST_SCENE_BOUNDS_KEY);
        return stashed instanceof Bounds bounds && usable(bounds) ? bounds : null;
    }

    private static Region buildGhost(M3GlassPane source) {
        Region ghost = new Region();
        ghost.getStyleClass().add("glass-morph-ghost");
        Color surface = ThemeColors.resolve(source, ColorRole.SURFACE_CONTAINER_HIGHEST);
        Color rim = ThemeColors.resolve(source, ColorRole.OUTLINE_VARIANT);
        double radius = Math.max(0, Math.min(source.getCornerRadius(), ShapeTokens.FULL));
        CornerRadii radii = new CornerRadii(radius);
        ghost.setBackground(new Background(new BackgroundFill(
                Color.color(surface.getRed(), surface.getGreen(), surface.getBlue(),
                        GHOST_FILL_ALPHA),
                radii, null)));
        ghost.setBorder(new Border(new BorderStroke(
                Color.color(rim.getRed(), rim.getGreen(), rim.getBlue(), GHOST_RIM_ALPHA),
                BorderStrokeStyle.SOLID, radii, new BorderWidths(1))));
        DropShadow shadow = new DropShadow();
        shadow.setRadius(GHOST_SHADOW_RADIUS);
        shadow.setOffsetY(GHOST_SHADOW_OFFSET_Y);
        shadow.setColor(Color.color(0, 0, 0, GHOST_SHADOW_OPACITY));
        ghost.setEffect(shadow);
        ghost.setMouseTransparent(true);
        return ghost;
    }

    private static void cancelGhost(Node target) {
        Object previous = target.getProperties().remove(GHOST_KEY);
        if (previous instanceof Region oldGhost) {
            SpringAnimation.stopTransforms(oldGhost);
            SpringAnimation.stop(oldGhost.opacityProperty());
            if (oldGhost.getParent() instanceof Pane host) {
                host.getChildren().remove(oldGhost);
            }
        }
    }

    private static void stopMorphTransforms(Node node) {
        SpringAnimation.stop(node.translateXProperty());
        SpringAnimation.stop(node.translateYProperty());
        SpringAnimation.stop(node.scaleXProperty());
        SpringAnimation.stop(node.scaleYProperty());
        SpringAnimation.stop(node.opacityProperty());
    }

    /**
     * One-shot-per-node scene guard: when the node leaves the scene mid-morph, all morph motion
     * stops, the ghost (if any) is removed and the node is restored to identity.
     */
    private static void installSceneGuard(Node node) {
        if (node.getProperties().containsKey(SCENE_GUARD_KEY)) {
            return;
        }
        // Array holder so the listener can remove itself (a lambda cannot reference its own name).
        @SuppressWarnings("unchecked")
        ChangeListener<javafx.scene.Scene>[] guard = new ChangeListener[1];
        guard[0] = (obs, oldScene, newScene) -> {
            if (newScene == null) {
                node.getProperties().remove(SCENE_GUARD_KEY);
                node.sceneProperty().removeListener(guard[0]);
                resetMorph(node);
            }
        };
        node.sceneProperty().addListener(guard[0]);
        node.getProperties().put(SCENE_GUARD_KEY, guard[0]);
    }

    private static double centerX(Bounds b) {
        return b.getMinX() + b.getWidth() / 2;
    }

    private static double centerY(Bounds b) {
        return b.getMinY() + b.getHeight() / 2;
    }

    private static boolean usable(Bounds b) {
        return b != null && b.getWidth() > 0 && b.getHeight() > 0
                && Double.isFinite(b.getMinX()) && Double.isFinite(b.getMinY())
                && Double.isFinite(b.getWidth()) && Double.isFinite(b.getHeight());
    }

    private static void requireFxThread(String operation) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException(
                    "GlassMorph." + operation + " must be called on the JavaFX Application Thread");
        }
    }
}
