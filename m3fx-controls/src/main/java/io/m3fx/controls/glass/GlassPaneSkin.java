package io.m3fx.controls.glass;

import io.m3fx.controls.internal.SpringAnimation;
import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.value.ChangeListener;
import javafx.collections.MapChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.SkinBase;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DisplacementMap;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.geometry.Rectangle2D;
import javafx.scene.shape.Rectangle;

/**
 * Rendering pipeline for {@link M3GlassPane} (spec: docs/SPEC_NOTES/liquid-glass.md, "Rendering
 * pipeline"), bottom-up:
 * <ol>
 *   <li><b>Backdrop capture</b> &mdash; a cached snapshot of the glass node's parent (parent
 *       background + surrounding siblings) with the glass node's own contribution suppressed by
 *       temporarily setting its opacity to 0. Invalidated on scene/parent/bounds/theme changes;
 *       never per frame. Live-animated backdrops refresh only on invalidation
 *       ({@link M3GlassPane#invalidateBackdrop()}). The capture is padded outward by
 *       {@code maxDisplacement + blurRadius} so rim refraction and blur spread can sample real
 *       content.</li>
 *   <li><b>Blur layer</b> &mdash; {@link GaussianBlur} at {@code blurRadius} on the backdrop
 *       image. Variable blur is approximated with a second sharp copy clipped to the inner region
 *       (inset by bezel + blur radius), so the center stays clearer than the diffused rim.</li>
 *   <li><b>Refraction</b> &mdash; {@link DisplacementMap} with the {@link GlassRefraction} float
 *       map, rebuilt only when size/radius/shape/refraction/maxDisplacement change (cached by
 *       key). Pressing animates the map SCALE only (no rebuild).</li>
 *   <li><b>Saturation</b> &mdash; {@link ColorAdjust} +0.18 regular / +0.10 clear (property
 *       override supported).</li>
 *   <li><b>Tint layer</b> &mdash; optional tint color at 0.25 alpha, else the theme surface
 *       container at {@code glassOpacity}.</li>
 *   <li><b>Specular rim</b> &mdash; a rim-light band hugging the shape (linear gradient along
 *       {@code specularAngle}, default 110&deg; = top-left) at {@code specular} intensity, plus a
 *       1px edge stroke at 0.45 of {@code -md-sys-color-outline-variant}.</li>
 *   <li><b>Drop shadow</b> &mdash; 0.30 alpha / radius 10 / +1 offsetY (property-gated), deepened
 *       while pressed.</li>
 *   <li><b>Content</b> &mdash; the content box floats above, unblurred.</li>
 * </ol>
 *
 * <p>Interactive glass (opt-in): press flattens the bevel (~40% less refraction) and deepens the
 * shadow through {@link SpringAnimation} (APPLE_SETTLE in, APPLE_PLAYFUL out &mdash; the
 * APPLE/FLUID-era presets); hover springs the specular angle slightly toward the pointer. All
 * springs and listeners are removed in {@link #dispose()}. FX thread only.
 */
class GlassPaneSkin extends SkinBase<M3GlassPane> {

    /** Press bevel flattening: refraction scale multiplier while fully pressed. */
    private static final double PRESS_FLATTEN = 0.40;

    /** Hover specular shift: fraction of the way toward the pointer direction. */
    private static final double HOVER_SHIFT = 0.35;

    /** Shadow defaults (spec): alpha 0.30, radius 10, offsetY +1; press deepens. */
    private static final double SHADOW_ALPHA = 0.30;
    private static final double SHADOW_RADIUS = 10.0;
    private static final double SHADOW_OFFSET_Y = 1.0;
    private static final double SHADOW_DEEPEN_ALPHA = 0.12;
    private static final double SHADOW_DEEPEN_RADIUS = 4.0;
    private static final double SHADOW_DEEPEN_OFFSET = 2.0;

    /** Edge stroke alpha of the outline-variant rim (spec: 0.45). */
    private static final double EDGE_STROKE_ALPHA = 0.45;

    /** Tint layer alpha when an explicit tint color is set (spec: 0.25). */
    private static final double TINT_ALPHA = 0.25;

    private final StackPane root = new StackPane();
    private final javafx.scene.layout.Pane glassLayers = new javafx.scene.layout.Pane();
    private final ImageView backdropView = new ImageView();
    private final ImageView sharpView = new ImageView();
    private final Region tintLayer = new Region();
    private final Region specularRing = new Region();
    private final Region edgeStroke = new Region();
    private final Rectangle layersClip = new Rectangle();
    private final Rectangle sharpClip = new Rectangle();

    private final GaussianBlur blur = new GaussianBlur();
    private final DisplacementMap displacement = new DisplacementMap();
    private final ColorAdjust saturationAdjust = new ColorAdjust();
    private final DropShadow dropShadow = new DropShadow();

    /** Press spring 0..1: flattens the bevel and deepens the shadow simultaneously. */
    private final javafx.beans.property.DoubleProperty bevelFlatten =
            new javafx.beans.property.SimpleDoubleProperty(this, "bevelFlatten", 0.0);

    private WritableImage backdropImage;
    private double captureOffsetX;
    private double captureOffsetY;
    private boolean backdropDirty = true;
    private boolean layersDirty = true;
    private String mapKey = "";
    private double baseSpecularAngle = 110.0;

    // Listener/handler fields so dispose() can remove everything the skin registered.
    private final ChangeListener<Object> invalidateListener;
    private final ChangeListener<Boolean> hoverListener;
    private final ChangeListener<Boolean> pressedListener;
    private final ChangeListener<Number> specularAngleListener;
    private final ChangeListener<Number> bevelFlattenListener;
    private final MapChangeListener<Object, Object> themeListener;
    private final EventHandler<MouseEvent> pressHandler;
    private final EventHandler<MouseEvent> releaseHandler;
    private final EventHandler<MouseEvent> moveHandler;
    private javafx.scene.Scene currentScene;

    GlassPaneSkin(M3GlassPane control) {
        super(control);
        root.getStyleClass().add("glass-root");
        root.setAlignment(Pos.CENTER);
        glassLayers.getStyleClass().add("glass-layers");
        glassLayers.setClip(layersClip);
        layersClip.widthProperty().bind(glassLayers.widthProperty());
        layersClip.heightProperty().bind(glassLayers.heightProperty());
        layersClip.arcWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                () -> 2.0 * effectiveRadius(),
                control.widthProperty(), control.heightProperty(),
                control.glassShapeProperty(), control.cornerRadiusProperty()));
        layersClip.arcHeightProperty().bind(layersClip.arcWidthProperty());

        // The image views render the padded capture, so they extend past the pane bounds; they
        // must never intercept input outside the glass.
        backdropView.setMouseTransparent(true);
        sharpView.setMouseTransparent(true);
        backdropView.setManaged(false);
        sharpView.setManaged(false);
        backdropView.setSmooth(true);
        sharpView.setSmooth(true);
        sharpView.setClip(sharpClip);
        tintLayer.getStyleClass().add("glass-tint");
        specularRing.getStyleClass().add("glass-specular");
        edgeStroke.getStyleClass().add("glass-edge");
        tintLayer.setMouseTransparent(true);
        specularRing.setMouseTransparent(true);
        edgeStroke.setMouseTransparent(true);
        glassLayers.getChildren().addAll(backdropView, sharpView, tintLayer, specularRing, edgeStroke);

        StackPane content = control.getContentBox();
        if (!content.getStyleClass().contains("glass-content")) {
            content.getStyleClass().addAll("m3-glass-content", "glass-content");
        }
        // Paint order: glass layers below, content floats above unblurred.
        root.getChildren().addAll(glassLayers, content);
        getChildren().add(root);

        // Effect chain: blur -> displacement -> saturation (spec pipeline order).
        saturationAdjust.setInput(displacement);
        displacement.setInput(blur);
        backdropView.setEffect(saturationAdjust);

        invalidateListener = (obs, o, n) -> invalidateAll();
        hoverListener = (obs, oldV, hover) -> {
            if (control.isInteractive() && !hover) {
                SpringAnimation.animate(
                        control.specularAngleProperty(), baseSpecularAngle,
                        SpringAnimation.APPLE_SETTLE, null);
            }
        };
        specularAngleListener = (obs, oldV, n) -> {
            if (!control.isHover()) {
                baseSpecularAngle = n.doubleValue();
            }
        };
        bevelFlattenListener = (obs, o, n) -> updateShadow();
        themeListener = change -> {
            if ("m3fx.theme".equals(change.getKey())) {
                invalidateAll();
            }
        };
        pressHandler = e -> {
            if (control.isInteractive() && !control.isDisabled()) {
                SpringAnimation.animate(bevelFlatten, 1.0, SpringAnimation.APPLE_SETTLE, null);
            }
        };
        releaseHandler = e -> {
            if (control.isInteractive()) {
                SpringAnimation.animate(bevelFlatten, 0.0, SpringAnimation.APPLE_PLAYFUL, null);
            }
        };
        moveHandler = e -> {
            if (!control.isInteractive() || !control.isHover()) {
                return;
            }
            double w = control.getWidth();
            double h = control.getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            double ux = e.getX() - w / 2.0;
            double uy = e.getY() - h / 2.0;
            double len = Math.sqrt(ux * ux + uy * uy);
            if (len < 1e-6) {
                return;
            }
            ux /= len;
            uy /= len;
            // Light "from the pointer": gradient bright start at the pointer side. CSS-convention
            // angle (0 = to top, clockwise): direction d = (sin t, -cos t), here d = -u.
            double target = Math.toDegrees(Math.atan2(-ux, uy));
            double base = control.getSpecularAngle();
            double delta = target - base;
            while (delta > 180.0) {
                delta -= 360.0;
            }
            while (delta < -180.0) {
                delta += 360.0;
            }
            SpringAnimation.animate(
                    control.specularAngleProperty(), base + delta * HOVER_SHIFT,
                    SpringAnimation.APPLE_SETTLE, null);
        };

        // Invalidation sources: geometry (bounds cover position, size and transforms), scene,
        // parent, theme swaps and every visual property. None of these fire per frame.
        control.boundsInParentProperty().addListener(invalidateListener);
        control.sceneProperty().addListener(invalidateListener);
        control.parentProperty().addListener(invalidateListener);
        control.variantProperty().addListener(invalidateListener);
        control.tintProperty().addListener(invalidateListener);
        control.glassShapeProperty().addListener(invalidateListener);
        control.cornerRadiusProperty().addListener(invalidateListener);
        control.blurRadiusProperty().addListener(invalidateListener);
        control.glassOpacityProperty().addListener(invalidateListener);
        control.refractionProperty().addListener(invalidateListener);
        control.maxDisplacementProperty().addListener(invalidateListener);
        control.specularProperty().addListener(invalidateListener);
        control.shadowProperty().addListener(invalidateListener);
        control.specularAngleProperty().addListener(specularAngleListener);
        pressedListener = (obs, o, n) -> updateShadow();
        control.pressedProperty().addListener(pressedListener);
        control.hoverProperty().addListener(hoverListener);
        control.addEventHandler(MouseEvent.MOUSE_PRESSED, pressHandler);
        control.addEventHandler(MouseEvent.MOUSE_RELEASED, releaseHandler);
        control.addEventHandler(MouseEvent.MOUSE_MOVED, moveHandler);
        attachSceneListeners(control.getScene());

        paintLayers();
    }

    /** Marks every cached artifact dirty and schedules a single layout pass. */
    private void invalidateAll() {
        backdropDirty = true;
        layersDirty = true;
        M3GlassPane control = getSkinnable();
        if (control != null) {
            attachSceneListeners(control.getScene());
            control.requestLayout();
        }
    }

    /** Attaches the theme-map listener to the current scene (no-op when unchanged). */
    private void attachSceneListeners(javafx.scene.Scene scene) {
        if (currentScene == scene) {
            return;
        }
        if (currentScene != null) {
            currentScene.getProperties().removeListener(themeListener);
        }
        currentScene = scene;
        if (scene != null) {
            scene.getProperties().addListener(themeListener);
        }
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        M3GlassPane control = getSkinnable();
        if (control != null) {
            if (backdropDirty) {
                captureBackdrop();
            }
            rebuildMapIfNeeded();
            if (layersDirty) {
                paintLayers();
            }
        }
        // SkinBase.layoutChildren fills root to the skin area; glassLayers and content follow
        // StackPane layout, and the unmanaged capture views keep their manual placement.
        super.layoutChildren(x, y, w, h);
    }

    // ------------------------------------------------------------------ backdrop capture

    /** Flags the backdrop capture dirty (the pane requests the layout pulse). */
    void invalidateBackdrop() {
        backdropDirty = true;
    }

    /**
     * Snapshots the parent with this pane's own contribution suppressed (opacity 0 during the
     * capture), padded by {@code maxDisplacement + blurRadius} so rim refraction and blur can
     * sample real pixels. Cached until invalidated.
     */
    private void captureBackdrop() {
        backdropDirty = false;
        M3GlassPane control = getSkinnable();
        Node parent = control.getParent();
        double w = control.getWidth();
        double h = control.getHeight();
        if (parent == null || w <= 1 || h <= 1) {
            backdropImage = null;
            backdropView.setImage(null);
            sharpView.setImage(null);
            return;
        }
        double pad = Math.max(0.0, effectiveMaxDisplacement()) + Math.max(0.0, control.getBlurRadius()) + 2.0;
        Bounds b = control.getBoundsInParent();
        double vw = b.getWidth() + 2.0 * pad;
        double vh = b.getHeight() + 2.0 * pad;
        int iw = (int) Math.ceil(Math.max(1.0, vw));
        int ih = (int) Math.ceil(Math.max(1.0, vh));
        // Hard cap so a giant pane cannot allocate an unbounded image; shrink the pad if needed.
        int maxDim = 4096;
        if (iw > maxDim || ih > maxDim) {
            return; // keep the previous capture rather than thrash memory
        }
        double vx = b.getMinX() - pad;
        double vy = b.getMinY() - pad;
        WritableImage image = new WritableImage(iw, ih);
        javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setViewport(new Rectangle2D(vx, vy, vw, vh));
        // Hide the skin ROOT (never bound) rather than the control: hosts may bind the control's
        // opacity (the command palette binds it to its card), and setting a bound value throws.
        double previousSkinOpacity = root.getOpacity();
        root.setOpacity(0.0);
        try {
            parent.snapshot(params, image);
        } finally {
            root.setOpacity(previousSkinOpacity);
        }
        backdropImage = image;
        captureOffsetX = b.getMinX() - vx;
        captureOffsetY = b.getMinY() - vy;
        backdropView.setImage(image);
        sharpView.setImage(image);
        placeBackdropViews();
    }

    /** Positions the padded capture images under the glass layers. */
    private void placeBackdropViews() {
        double iw = backdropImage == null ? 0 : backdropImage.getWidth();
        double ih = backdropImage == null ? 0 : backdropImage.getHeight();
        backdropView.setLayoutX(-captureOffsetX);
        backdropView.setLayoutY(-captureOffsetY);
        backdropView.setFitWidth(iw);
        backdropView.setFitHeight(ih);
        sharpView.setLayoutX(-captureOffsetX);
        sharpView.setLayoutY(-captureOffsetY);
        sharpView.setFitWidth(iw);
        sharpView.setFitHeight(ih);
        M3GlassPane control = getSkinnable();
        double w = control.getWidth();
        double h = control.getHeight();
        double inset = sharpInset();
        sharpClip.setX(captureOffsetX + inset);
        sharpClip.setY(captureOffsetY + inset);
        sharpClip.setWidth(Math.max(0.0, w - 2.0 * inset));
        sharpClip.setHeight(Math.max(0.0, h - 2.0 * inset));
        sharpClip.setArcWidth(0.0);
        sharpClip.setArcHeight(0.0);
    }

    /**
     * Inset of the sharp (non-blurred) copy: the refraction bezel plus the blur spread, clamped
     * inside the pane. The blurred layer stays full-pane underneath, so the rim reads diffused
     * while the center stays clear ("bezel ring inset" variable-blur approximation).
     */
    private double sharpInset() {
        M3GlassPane control = getSkinnable();
        double halfMin = Math.min(control.getWidth(), control.getHeight()) / 2.0;
        return Math.max(0.0, Math.min(effectiveRadius() + Math.max(0.0, control.getBlurRadius()), halfMin - 0.5));
    }

    // ------------------------------------------------------------------ refraction map

    /** Rebuilds the DisplacementMap only when size/radius/shape/refraction/maxDisplacement changed. */
    private void rebuildMapIfNeeded() {
        M3GlassPane control = getSkinnable();
        double w = backdropImage == null ? control.getWidth() : backdropImage.getWidth();
        double h = backdropImage == null ? control.getHeight() : backdropImage.getHeight();
        double scale = effectiveMaxDisplacement();
        String key = (int) w + "x" + (int) h + "|" + effectiveRadius()
                + "|" + control.getGlassShape() + "|" + scale;
        if (key.equals(mapKey)) {
            return;
        }
        mapKey = key;
        GlassRefraction.Profile profile = GlassRefraction.Profile.CONVEX;
        // Refraction strength flows through maxDisplacement (auto: refraction * min(w,h) * 0.08),
        // so the unit-magnitude map (refraction=1) plus the scale encodes the shift exactly once.
        displacement.setMapData(GlassRefraction.buildFloatMap(
                w, h, control.getCornerRadius(), control.getGlassShape(), profile, 1.0, scale));
        displacement.setOffsetX(0.0);
        displacement.setOffsetY(0.0);
        updateDisplacementScale();
    }

    /** Sets the DisplacementMap scale: maxDisplacement reduced by the press bevel flattening. */
    private void updateDisplacementScale() {
        double scale = Math.max(0.0, effectiveMaxDisplacement()) * (1.0 - PRESS_FLATTEN * bevelFlatten.get());
        displacement.setScaleX(scale);
        displacement.setScaleY(scale);
    }

    // ------------------------------------------------------------------ layers

    /** Repaints tint, specular, edge stroke, shadow and effect chain from the current properties. */
    private void paintLayers() {
        layersDirty = false;
        M3GlassPane control = getSkinnable();
        double radius = effectiveRadius();
        paintTint(radius);
        paintSpecular(radius);
        updateEffects();
        updateShadow();
        placeBackdropViews();
    }

    private void paintTint(double radius) {
        M3GlassPane control = getSkinnable();
        Color fill;
        Color tint = control.getTint();
        if (tint != null) {
            fill = Color.color(
                    clamp01(tint.getRed()), clamp01(tint.getGreen()), clamp01(tint.getBlue()), TINT_ALPHA);
        } else {
            Color surface = ThemeColors.resolve(control, ColorRole.SURFACE_CONTAINER_HIGH);
            fill = Color.color(
                    surface.getRed(), surface.getGreen(), surface.getBlue(),
                    clamp01(control.getGlassOpacity()));
        }
        tintLayer.setBackground(new Background(new BackgroundFill(
                fill, new CornerRadii(radius), null)));
    }

    private void paintSpecular(double radius) {
        M3GlassPane control = getSkinnable();
        double intensity = clamp01(control.getSpecular());
        double band = effectiveRadius();
        if (intensity <= 0.0 || band <= 0.0) {
            specularRing.setBorder(null);
        } else {
            // CSS-convention angle (0 = to top, clockwise); gradient bright start sits at
            // center - d/2 so the highlight faces the light.
            double rad = Math.toRadians(control.getSpecularAngle());
            double dx = Math.sin(rad);
            double dy = -Math.cos(rad);
            Color hot = Color.color(1.0, 1.0, 1.0, intensity);
            Color mid = Color.color(1.0, 1.0, 1.0, intensity * 0.25);
            Color back = Color.color(1.0, 1.0, 1.0, intensity * 0.30);
            LinearGradient gradient = new LinearGradient(
                    0.5 - dx / 2.0, 0.5 - dy / 2.0, 0.5 + dx / 2.0, 0.5 + dy / 2.0,
                    true, CycleMethod.NO_CYCLE,
                    new Stop(0.0, hot), new Stop(0.45, mid),
                    new Stop(0.62, Color.TRANSPARENT), new Stop(1.0, back));
            specularRing.setBorder(new Border(new BorderStroke(
                    gradient, BorderStrokeStyle.SOLID, new CornerRadii(radius),
                    new BorderWidths(band))));
        }
        Color outline = ThemeColors.resolve(control, ColorRole.OUTLINE_VARIANT);
        edgeStroke.setBorder(new Border(new BorderStroke(
                Color.color(outline.getRed(), outline.getGreen(), outline.getBlue(), EDGE_STROKE_ALPHA),
                BorderStrokeStyle.SOLID, new CornerRadii(radius), new BorderWidths(1.0))));
    }

    private void updateEffects() {
        M3GlassPane control = getSkinnable();
        blur.setRadius(Math.max(0.0, control.getBlurRadius()));
        saturationAdjust.setSaturation(effectiveSaturation());
    }

    /** Drop shadow 0.30/10/+1, deepened by the press spring; gated by the shadow property. */
    private void updateShadow() {
        M3GlassPane control = getSkinnable();
        if (control == null) {
            return;
        }
        double flatten = bevelFlatten.get();
        if (control.isShadow()) {
            Color shadow = ThemeColors.resolve(control, ColorRole.SHADOW);
            dropShadow.setColor(Color.color(
                    shadow.getRed(), shadow.getGreen(), shadow.getBlue(),
                    clamp01(SHADOW_ALPHA + SHADOW_DEEPEN_ALPHA * flatten)));
            dropShadow.setRadius(SHADOW_RADIUS + SHADOW_DEEPEN_RADIUS * flatten);
            dropShadow.setOffsetY(SHADOW_OFFSET_Y + SHADOW_DEEPEN_OFFSET * flatten);
            dropShadow.setOffsetX(0.0);
            dropShadow.setSpread(0.0);
            dropShadow.setBlurType(BlurType.GAUSSIAN);
            dropShadow.setInput(null);
            root.setEffect(dropShadow);
        } else {
            root.setEffect(null);
        }
        updateDisplacementScale();
    }

    // ------------------------------------------------------------------ helpers

    private double effectiveRadius() {
        M3GlassPane control = getSkinnable();
        return GlassRefraction.effectiveRadius(
                control.getGlassShape(), control.getCornerRadius(),
                Math.max(1.0, control.getWidth()), Math.max(1.0, control.getHeight()));
    }

    private double effectiveMaxDisplacement() {
        M3GlassPane control = getSkinnable();
        double configured = control.getMaxDisplacement();
        if (configured > 0.0) {
            return configured;
        }
        double size = Math.min(Math.max(1.0, control.getWidth()), Math.max(1.0, control.getHeight()));
        return Math.max(0.0, control.getRefraction()) * size * 0.08;
    }

    private double effectiveSaturation() {
        M3GlassPane control = getSkinnable();
        double s = control.getSaturation();
        if (s >= 0.0) {
            return s;
        }
        return control.getVariant() == GlassVariant.CLEAR ? 0.10 : 0.18;
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    @Override
    public void dispose() {
        M3GlassPane control = getSkinnable();
        if (control != null) {
            control.boundsInParentProperty().removeListener(invalidateListener);
            control.sceneProperty().removeListener(invalidateListener);
            control.parentProperty().removeListener(invalidateListener);
            control.variantProperty().removeListener(invalidateListener);
            control.tintProperty().removeListener(invalidateListener);
            control.glassShapeProperty().removeListener(invalidateListener);
            control.cornerRadiusProperty().removeListener(invalidateListener);
            control.blurRadiusProperty().removeListener(invalidateListener);
            control.glassOpacityProperty().removeListener(invalidateListener);
            control.refractionProperty().removeListener(invalidateListener);
            control.maxDisplacementProperty().removeListener(invalidateListener);
            control.specularProperty().removeListener(invalidateListener);
            control.shadowProperty().removeListener(invalidateListener);
            control.specularAngleProperty().removeListener(specularAngleListener);
            control.pressedProperty().removeListener(pressedListener);
            control.hoverProperty().removeListener(hoverListener);
            control.removeEventHandler(MouseEvent.MOUSE_PRESSED, pressHandler);
            control.removeEventHandler(MouseEvent.MOUSE_RELEASED, releaseHandler);
            control.removeEventHandler(MouseEvent.MOUSE_MOVED, moveHandler);
            if (currentScene != null) {
                currentScene.getProperties().removeListener(themeListener);
                currentScene = null;
            }
        }
        SpringAnimation.stop(bevelFlatten);
        if (control != null) {
            SpringAnimation.stop(control.specularAngleProperty());
        }
        backdropImage = null;
        backdropView.setImage(null);
        sharpView.setImage(null);
        backdropView.setEffect(null);
        root.setEffect(null);
        displacement.setMapData(new javafx.scene.effect.FloatMap(1, 1));
        super.dispose();
    }
}
