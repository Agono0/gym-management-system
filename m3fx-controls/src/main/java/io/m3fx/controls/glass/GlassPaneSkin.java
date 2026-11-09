package io.m3fx.controls.glass;

import io.m3fx.controls.internal.SpringAnimation;
import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.MapChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.SkinBase;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DisplacementMap;
import javafx.scene.effect.DropShadow;
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
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;

/**
 * Rendering pipeline & physics engine for {@link M3GlassPane}.
 *
 * <p>Implements authentic Apple Liquid Glass optics, viscous fluid simulation, 3D perspective
 * tilt, and draggable lens mechanics ported from {@code liquidGL-main} and {@code liquid-glass-main}:
 * <ol>
 *   <li><b>Hardware DisplacementMap</b> &mdash; JavaFX GPU Prism shader displacement pipeline
 *       providing 60 FPS live refraction for both {@code LIQUIDGL} SDF physics and {@code SVG_DISPLACEMENT}
 *       nikdelvin gradient-screen constructions.</li>
 *   <li><b>Viscous Fluid Physics</b> &mdash; continuous differential simulation tracking pointer velocity
 *       and stepping viscous decay ({@code follow} &amp; {@code settle} exponential decay) so ripples
 *       viscously oscillate and settle to rest over time even after interaction stops.</li>
 *   <li><b>3D Perspective Tilt</b> &mdash; 3D perspective rotation around center axes ({@link Rotate})
 *       with cubic ease-out, coupled with optical refraction tilt offset opposite to the angle.</li>
 *   <li><b>Draggable Lenses</b> &mdash; dynamic mouse dragging with shadow elevation springs and live
 *       backdrop refraction updates as the lens moves.</li>
 *   <li><b>Apple Button Ergonomics</b> &mdash; hover scale (1.05x), press squish (0.96x), and inset
 *       specular rim highlights.</li>
 *   <li><b>Specular Lights</b> &mdash; directional rim gradient plus two time-animated Lissajous light orbs.</li>
 * </ol>
 */
class GlassPaneSkin extends SkinBase<M3GlassPane> {

    private static final double PRESS_FLATTEN = 0.40;
    private static final double HOVER_SHIFT = 0.35;

    private static final double SHADOW_ALPHA = 0.30;
    private static final double SHADOW_RADIUS = 10.0;
    private static final double SHADOW_OFFSET_Y = 1.0;
    private static final double SHADOW_DEEPEN_ALPHA = 0.12;
    private static final double SHADOW_DEEPEN_RADIUS = 4.0;
    private static final double SHADOW_DEEPEN_OFFSET = 2.0;

    private static final double EDGE_STROKE_ALPHA = 0.45;

    private final StackPane root = new StackPane();
    private final Pane glassLayers = new Pane();
    private final ImageView backdropView = new ImageView();
    private final ImageView sharpView = new ImageView();
    private final Region tintLayer = new Region();
    private final Region specularRing = new Region();
    private final Region specularLight1 = new Region();
    private final Region specularLight2 = new Region();
    private final Region edgeStroke = new Region();
    private final Rectangle layersClip = new Rectangle();
    private final Rectangle sharpClip = new Rectangle();

    private final DisplacementMap displacementMap = new DisplacementMap();
    private final ColorAdjust saturationAdjust = new ColorAdjust();
    private final DropShadow dropShadow = new DropShadow();

    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);

    private final DoubleProperty bevelFlatten =
            new SimpleDoubleProperty(this, "bevelFlatten", 0.0);
    private final DoubleProperty buttonScale =
            new SimpleDoubleProperty(this, "buttonScale", 1.0);
    private final DoubleProperty dragElevation =
            new SimpleDoubleProperty(this, "dragElevation", 0.0);

    private WritableImage backdropImage;
    private WritableImage refractedImage;
    private double captureOffsetX;
    private double captureOffsetY;
    private boolean backdropDirty = true;
    private boolean layersDirty = true;
    private boolean renderDirty = true;
    private double baseSpecularAngle = 110.0;

    // Fluid simulation state
    private boolean fluidInside;
    private double fluidPointerX = 0.5;
    private double fluidPointerY = 0.5;
    private double fluidDeltaX;
    private double fluidDeltaY;
    private double lastPointerX;
    private double lastPointerY;
    private double pointerVx;
    private double pointerVy;
    private double lastPointerTimeMs;
    private boolean hasLastPointer;

    // 3D tilt state
    private double targetTiltX;
    private double targetTiltY;
    private double currentTiltX;
    private double currentTiltY;

    // Dragging state
    private boolean isDragging;
    private double dragStartSceneX;
    private double dragStartSceneY;
    private double nodeStartX;
    private double nodeStartY;

    private AnimationTimer physicsTimer;
    private long lastStepNanos;
    private long animationStartNanos;

    private final ChangeListener<Object> invalidateListener;
    private final ChangeListener<Boolean> hoverListener;
    private final ChangeListener<Boolean> pressedListener;
    private final ChangeListener<Number> specularAngleListener;
    private final ChangeListener<Number> bevelFlattenListener;
    private final ChangeListener<Number> buttonScaleListener;
    private final ChangeListener<Number> dragElevationListener;
    private final ChangeListener<Number> renderPropertyListener;
    private final MapChangeListener<Object, Object> themeListener;
    private final EventHandler<MouseEvent> pressHandler;
    private final EventHandler<MouseEvent> releaseHandler;
    private final EventHandler<MouseEvent> moveHandler;
    private final EventHandler<MouseEvent> dragHandler;
    private final EventHandler<MouseEvent> exitHandler;
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
        root.getChildren().addAll(glassLayers, content);
        getChildren().add(root);

        // Hardware DisplacementMap effect chain: DisplacementMap -> ColorAdjust
        displacementMap.setWrap(false);
        displacementMap.setScaleX(1.0);
        displacementMap.setScaleY(1.0);
        saturationAdjust.setInput(displacementMap);
        backdropView.setEffect(saturationAdjust);

        // 3D perspective tilt transforms
        control.getTransforms().addAll(rotateX, rotateY);
        rotateX.pivotXProperty().bind(control.widthProperty().divide(2.0));
        rotateX.pivotYProperty().bind(control.heightProperty().divide(2.0));
        rotateY.pivotXProperty().bind(control.widthProperty().divide(2.0));
        rotateY.pivotYProperty().bind(control.heightProperty().divide(2.0));

        // Time-animated specular lights
        specularLight1.setMouseTransparent(true);
        specularLight2.setMouseTransparent(true);
        specularLight1.setManaged(false);
        specularLight2.setManaged(false);
        specularLight1.setVisible(false);
specularLight2.setVisible(false);

        // Unified continuous physics loop (Fluid Dynamics + 3D Tilt + Specular Orbs)
        physicsTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (animationStartNanos == 0) {
                    animationStartNanos = now;
                    lastStepNanos = now;
                    return;
                }
                double dt = Math.max(1.0, Math.min(50.0, (now - lastStepNanos) / 1_000_000.0));
                lastStepNanos = now;
                double tSec = (now - animationStartNanos) / 1_000_000_000.0;

                stepPhysics(dt, tSec);
            }
        };

        invalidateListener = (obs, o, n) -> invalidateAll();
        hoverListener = (obs, oldV, hover) -> {
            M3GlassPane c = getSkinnable();
            if (c == null) return;
            if (c.isButtonMode()) {
                SpringAnimation.animate(buttonScale, hover ? 1.05 : 1.0,
                        SpringAnimation.APPLE_PLAYFUL, null);
            }
            if (c.isInteractive() && !hover) {
                SpringAnimation.animate(c.specularAngleProperty(), baseSpecularAngle,
                        SpringAnimation.APPLE_SETTLE, null);
            }
        };
        specularAngleListener = (obs, oldV, n) -> {
            M3GlassPane c = getSkinnable();
            if (c != null && !c.isHover()) {
                baseSpecularAngle = n.doubleValue();
            }
        };
        bevelFlattenListener = (obs, o, n) -> {
            updateShadow();
            renderDirty = true;
        };
        buttonScaleListener = (obs, o, n) -> {
            root.setScaleX(n.doubleValue());
            root.setScaleY(n.doubleValue());
        };
        dragElevationListener = (obs, o, n) -> updateShadow();
        renderPropertyListener = (obs, o, n) -> {
            renderDirty = true;
            M3GlassPane owner = getSkinnable();
            if (owner != null) {
                owner.requestLayout();
            }
        };
        themeListener = change -> {
            if ("m3fx.theme".equals(change.getKey())) {
                invalidateAll();
            }
        };
        pressHandler = e -> {
            M3GlassPane c = getSkinnable();
            if (c == null || c.isDisabled()) return;
            if (c.isButtonMode()) {
                SpringAnimation.animate(buttonScale, 0.96, SpringAnimation.APPLE_SETTLE, null);
            }
            if (c.isInteractive()) {
                SpringAnimation.animate(bevelFlatten, 1.0, SpringAnimation.APPLE_SETTLE, null);
            }
            if (c.isDraggable()) {
                isDragging = true;
                dragStartSceneX = e.getSceneX();
                dragStartSceneY = e.getSceneY();
                nodeStartX = c.getLayoutX();
                nodeStartY = c.getLayoutY();
                SpringAnimation.animate(dragElevation, 1.0, SpringAnimation.APPLE_SETTLE, null);
            }
        };
        releaseHandler = e -> {
            M3GlassPane c = getSkinnable();
            if (c == null) return;
            if (c.isButtonMode()) {
                SpringAnimation.animate(buttonScale, c.isHover() ? 1.05 : 1.0,
                        SpringAnimation.APPLE_PLAYFUL, null);
            }
            if (c.isInteractive()) {
                SpringAnimation.animate(bevelFlatten, 0.0, SpringAnimation.APPLE_PLAYFUL, null);
            }
            if (isDragging) {
                isDragging = false;
                SpringAnimation.animate(dragElevation, 0.0, SpringAnimation.APPLE_PLAYFUL, null);
            }
        };
        dragHandler = e -> {
            M3GlassPane c = getSkinnable();
            if (c == null || !isDragging) return;
            double dx = e.getSceneX() - dragStartSceneX;
            double dy = e.getSceneY() - dragStartSceneY;
            c.setLayoutX(nodeStartX + dx);
            c.setLayoutY(nodeStartY + dy);
            invalidateBackdrop();
        };
        moveHandler = e -> {
            M3GlassPane c = getSkinnable();
            if (c == null) return;
            double w = c.getWidth();
            double h = c.getHeight();
            if (w <= 0 || h <= 0) return;

            long now = System.nanoTime();
            double nowMs = now / 1_000_000.0;
            double dt = nowMs - lastPointerTimeMs;

            if (hasLastPointer && dt > 0 && dt < 120) {
                pointerVx = (e.getX() - lastPointerX) / dt;
                pointerVy = (e.getY() - lastPointerY) / dt;
            } else {
                pointerVx = 0;
                pointerVy = 0;
            }
            lastPointerX = e.getX();
            lastPointerY = e.getY();
            lastPointerTimeMs = nowMs;
            hasLastPointer = true;
            fluidInside = true;

            // 3D Perspective Tilt calculation
            if (c.isTilt()) {
                double maxTilt = c.getTiltFactor();
                targetTiltY = ((e.getX() - w / 2.0) / (w / 2.0)) * maxTilt;
                targetTiltX = -((e.getY() - h / 2.0) / (h / 2.0)) * maxTilt;
            }

            // Specular highlight direction tracking
            if (c.isInteractive() && c.isHover()) {
                double ux = e.getX() - w / 2.0;
                double uy = e.getY() - h / 2.0;
                double len = Math.hypot(ux, uy);
                if (len >= 1e-6) {
                    ux /= len;
                    uy /= len;
                    double target = Math.toDegrees(Math.atan2(-ux, uy));
                    double base = c.getSpecularAngle();
                    double delta = target - base;
                    while (delta > 180.0) delta -= 360.0;
                    while (delta < -180.0) delta += 360.0;
                    SpringAnimation.animate(c.specularAngleProperty(), base + delta * HOVER_SHIFT,
                            SpringAnimation.APPLE_SETTLE, null);
                }
            }
        };
        exitHandler = e -> {
            fluidInside = false;
            hasLastPointer = false;
            pointerVx = 0;
            pointerVy = 0;
            targetTiltX = 0;
            targetTiltY = 0;
        };

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
        control.bevelDepthProperty().addListener(renderPropertyListener);
        control.bevelWidthProperty().addListener(renderPropertyListener);
        control.frostProperty().addListener(renderPropertyListener);
        control.aberrationProperty().addListener(renderPropertyListener);
        control.magnifyProperty().addListener(renderPropertyListener);
        control.mapStyleProperty().addListener(invalidateListener);
        control.nikdelvinDepthProperty().addListener(renderPropertyListener);
        control.nikdelvinStrengthProperty().addListener(renderPropertyListener);
        control.fluidInteractionProperty().addListener(invalidateListener);
        control.specularProperty().addListener(invalidateListener);
        control.shadowProperty().addListener(invalidateListener);
        control.tiltProperty().addListener(invalidateListener);
        control.tiltFactorProperty().addListener(renderPropertyListener);
        control.tiltEaseProperty().addListener(renderPropertyListener);
        control.draggableProperty().addListener(invalidateListener);
        control.buttonModeProperty().addListener(invalidateListener);
        control.specularAngleProperty().addListener(specularAngleListener);

        pressedListener = (obs, o, n) -> updateShadow();
        control.pressedProperty().addListener(pressedListener);
        control.hoverProperty().addListener(hoverListener);
        bevelFlatten.addListener(bevelFlattenListener);
        buttonScale.addListener(buttonScaleListener);
        dragElevation.addListener(dragElevationListener);

        control.addEventHandler(MouseEvent.MOUSE_PRESSED, pressHandler);
        control.addEventHandler(MouseEvent.MOUSE_RELEASED, releaseHandler);
        control.addEventHandler(MouseEvent.MOUSE_MOVED, moveHandler);
        control.addEventHandler(MouseEvent.MOUSE_DRAGGED, dragHandler);
        control.addEventHandler(MouseEvent.MOUSE_EXITED, exitHandler);
        attachSceneListeners(control.getScene());

        paintLayers();
        physicsTimer.start();
    }

    private void stepPhysics(double dt, double tSec) {
        M3GlassPane control = getSkinnable();
        if (control == null || !control.isVisible() || control.getScene() == null) {
            return;
        }
        double w = control.getWidth();
        double h = control.getHeight();
        if (w <= 0 || h <= 0) return;

        boolean needUpdate = false;

        // 1. Viscous Fluid Mechanics (liquidGL equations)
        if (control.isFluidInteraction()) {
            double viscosity = control.getInteractionViscosity();
            double follow = 1.0 - Math.exp(-dt / (25.0 + viscosity * 180.0));
            double settle = 1.0 - Math.exp(-dt / (80.0 + viscosity * 620.0));
            double targetDx = 0.0, targetDy = 0.0;

            if (fluidInside && hasLastPointer) {
                fluidPointerX += ((lastPointerX / w) - fluidPointerX) * follow;
                fluidPointerY += ((lastPointerY / h) - fluidPointerY) * follow;
                double speed = Math.hypot(pointerVx, pointerVy);
                double minDim = Math.min(w, h);
                long now = System.nanoTime();
                double nowMs = now / 1_000_000.0;
                double amount = Math.min(minDim * 0.12, speed * 45.0)
                        * control.getInteractionStrength()
                        * Math.exp(-Math.max(0, nowMs - lastPointerTimeMs) / 70.0);
                if (speed > 0) {
                    targetDx = (pointerVx / speed) * amount / w;
                    targetDy = (pointerVy / speed) * amount / h;
                }
            }

            double easing = Math.hypot(targetDx, targetDy) > Math.hypot(fluidDeltaX, fluidDeltaY)
                    ? follow : settle;
            double prevDx = fluidDeltaX;
            double prevDy = fluidDeltaY;
            fluidDeltaX += (targetDx - fluidDeltaX) * easing;
            fluidDeltaY += (targetDy - fluidDeltaY) * easing;

            if (Math.hypot(fluidDeltaX, fluidDeltaY) < 0.00001) {
                fluidDeltaX = 0;
                fluidDeltaY = 0;
            }
            if (Math.abs(fluidDeltaX - prevDx) > 1e-5 || Math.abs(fluidDeltaY - prevDy) > 1e-5) {
                needUpdate = true;
            }
        }

        // 2. 3D Perspective Tilt Physics
        if (control.isTilt()) {
            double easeFactor = 1.0 - Math.exp(-dt / Math.max(10.0, control.getTiltEase() * 0.25));
            double prevTiltX = currentTiltX;
            double prevTiltY = currentTiltY;
            currentTiltX += (targetTiltX - currentTiltX) * easeFactor;
            currentTiltY += (targetTiltY - currentTiltY) * easeFactor;
            rotateX.setAngle(currentTiltX);
            rotateY.setAngle(currentTiltY);
            if (Math.abs(currentTiltX - prevTiltX) > 0.005 || Math.abs(currentTiltY - prevTiltY) > 0.005) {
                needUpdate = true;
            }
        } else {
            rotateX.setAngle(0);
            rotateY.setAngle(0);
        }

        // 3. Animated Specular Lights
        if (control.getSpecular() > 0.0) {
            placeSpecularLights(tSec);
        }

        if (needUpdate) {
            renderDirty = true;
            renderRefractedIfNeeded();
        }
    }

    private void invalidateAll() {
        backdropDirty = true;
        layersDirty = true;
        M3GlassPane control = getSkinnable();
        if (control != null) {
            attachSceneListeners(control.getScene());
            control.requestLayout();
        }
    }

    private void attachSceneListeners(javafx.scene.Scene scene) {
        if (currentScene == scene) return;
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
            renderRefractedIfNeeded();
            if (layersDirty) {
                paintLayers();
            }
        }
        super.layoutChildren(x, y, w, h);
    }

    void invalidateBackdrop() {
        backdropDirty = true;
        renderDirty = true;
    }

    private void captureBackdrop() {
        backdropDirty = false;
        M3GlassPane control = getSkinnable();
        if (control == null) return;
        double w = control.getWidth();
        double h = control.getHeight();
        if (w <= 1 || h <= 1) {
            backdropImage = null;
            backdropView.setImage(null);
            sharpView.setImage(null);
            return;
        }

        // Find best ancestor to snapshot (one with background/wallpaper)
        Node captureSource = null;
        for (Node curr = control.getParent(); curr != null; curr = curr.getParent()) {
            if (curr.getStyleClass().contains("glass-stage") || curr.getStyleClass().contains("glass-backdrop")
                    || (curr instanceof Region r && r.getBackground() != null && !r.getBackground().isEmpty())
                    || curr instanceof ImageView) {
                captureSource = curr;
                break;
            }
        }
        if (captureSource == null) {
            captureSource = control.getParent();
        }
        if (captureSource == null) {
            backdropImage = null;
            backdropView.setImage(null);
            sharpView.setImage(null);
            return;
        }

        double maxPullPx = (Math.max(0.0, control.getRefraction()) + Math.max(0.0, control.getBevelDepth()))
                * Math.max(control.getWidth(), control.getHeight());
        double pad = maxPullPx + Math.max(0.0, control.getBlurRadius()) + 4.0;

        Bounds b;
        if (control.getScene() != null && captureSource.getScene() != null) {
            Bounds bInScene = control.localToScene(control.getBoundsInLocal());
            b = captureSource.sceneToLocal(bInScene);
        } else {
            b = control.getBoundsInParent();
        }

        double vw = b.getWidth() + 2.0 * pad;
        double vh = b.getHeight() + 2.0 * pad;
        int iw = (int) Math.ceil(Math.max(1.0, vw));
        int ih = (int) Math.ceil(Math.max(1.0, vh));
        int maxDim = 4096;
        if (iw > maxDim || ih > maxDim) {
            return;
        }
        double vx = b.getMinX() - pad;
        double vy = b.getMinY() - pad;
        WritableImage image = new WritableImage(iw, ih);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setViewport(new Rectangle2D(vx, vy, vw, vh));

        double previousSkinOpacity = root.getOpacity();
        root.setOpacity(0.0);
        try {
            captureSource.snapshot(params, image);
        } finally {
            root.setOpacity(previousSkinOpacity);
        }
        backdropImage = image;
        captureOffsetX = b.getMinX() - vx;
        captureOffsetY = b.getMinY() - vy;
        renderDirty = true;
        renderRefractedIfNeeded();
        placeBackdropViews();
    }

    private void placeBackdropViews() {
        M3GlassPane control = getSkinnable();
        if (control == null) return;
        double w = control.getWidth();
        double h = control.getHeight();
        backdropView.setLayoutX(0);
        backdropView.setLayoutY(0);
        backdropView.setFitWidth(w);
        backdropView.setFitHeight(h);
        sharpView.setVisible(false);
    }

    private double sharpInset() {
        M3GlassPane control = getSkinnable();
        double halfMin = Math.min(control.getWidth(), control.getHeight()) / 2.0;
        return Math.max(0.0, Math.min(effectiveRadius() + Math.max(0.0, control.getBlurRadius()), halfMin - 0.5));
    }

    private double effectiveRadius() {
        M3GlassPane control = getSkinnable();
        if (control == null) return 0.0;
        return switch (control.getGlassShape()) {
            case CIRCLE, CAPSULE -> Math.min(control.getWidth(), control.getHeight()) / 2.0;
            case RECT -> Math.min(control.getCornerRadius(),
                    Math.min(control.getWidth(), control.getHeight()) / 2.0);
        };
    }

    private void renderRefractedIfNeeded() {
        M3GlassPane control = getSkinnable();
        if (control == null || backdropImage == null) {
            return;
        }
        if (!renderDirty && refractedImage != null) {
            return;
        }
        renderDirty = false;
        double flatten = bevelFlatten.get();
        double w = control.getWidth();
        double h = control.getHeight();
        if (w <= 1 || h <= 1) return;
        double radius = effectiveRadius();
        double effectiveRefraction = Math.max(0.0, control.getRefraction()) * (1.0 - PRESS_FLATTEN * flatten);

        GlassRefraction.Interaction interaction = control.isFluidInteraction()
                ? new GlassRefraction.Interaction(fluidPointerX, fluidPointerY, fluidDeltaX, fluidDeltaY,
                control.getInteractionRadius())
                : null;

        GlassRefraction.ShaderParams params = new GlassRefraction.ShaderParams(
                w, h, radius, effectiveRefraction, control.getBevelDepth(),
                control.getBevelWidth(), control.getFrost(), control.getAberration(),
                control.getMagnify(), control.getTint(), currentTiltX, currentTiltY,
                interaction);
        int iw = Math.max(1, (int) Math.ceil(w));
        int ih = Math.max(1, (int) Math.ceil(h));
        if (refractedImage == null
                || (int) refractedImage.getWidth() != iw
                || (int) refractedImage.getHeight() != ih) {
            refractedImage = new WritableImage(iw, ih);
        }
        GlassRefraction.render(backdropImage, refractedImage, captureOffsetX, captureOffsetY, params, interaction);
        backdropView.setImage(refractedImage);
        placeBackdropViews();
    }

    private void paintLayers() {
        layersDirty = false;
        double radius = effectiveRadius();
        paintTint(radius);
        paintSpecular(radius);
        updateEffects();
        updateShadow();
        placeBackdropViews();
    }

    private void paintTint(double radius) {
        M3GlassPane control = getSkinnable();
        Color customTint = control.getTint();
        Color fill;
        if (customTint != null && customTint.getOpacity() > 0.0) {
            fill = customTint;
        } else if (control.isButtonMode()) {
            fill = Color.color(1.0, 1.0, 1.0, 0.12);
        } else if (control.getVariant() == GlassVariant.CLEAR) {
            fill = Color.color(1.0, 1.0, 1.0, 0.03);
        } else {
            fill = Color.color(1.0, 1.0, 1.0, Math.min(0.20, Math.max(0.06, control.getGlassOpacity())));
        }
        tintLayer.setBackground(new Background(new BackgroundFill(
                fill, new CornerRadii(radius), null)));
    }

    private void paintSpecular(double radius) {
        M3GlassPane control = getSkinnable();
        double intensity = clamp01(control.getSpecular());
        if (intensity <= 0.0) {
            specularRing.setBorder(null);
        } else {
            double rad = Math.toRadians(control.getSpecularAngle());
            double dx = Math.sin(rad);
            double dy = -Math.cos(rad);
            Color hot = Color.color(1.0, 1.0, 1.0, Math.min(1.0, intensity * 0.85));
            Color mid = Color.color(1.0, 1.0, 1.0, intensity * 0.22);
            Color back = Color.color(1.0, 1.0, 1.0, intensity * 0.18);
            LinearGradient gradient = new LinearGradient(
                    0.5 - dx / 2.0, 0.5 - dy / 2.0, 0.5 + dx / 2.0, 0.5 + dy / 2.0,
                    true, CycleMethod.NO_CYCLE,
                    new Stop(0.0, hot), new Stop(0.35, mid),
                    new Stop(0.65, Color.TRANSPARENT), new Stop(1.0, back));
            specularRing.setBorder(new Border(new BorderStroke(
                    gradient, BorderStrokeStyle.SOLID, new CornerRadii(radius),
                    new BorderWidths(1.0))));
        }
        edgeStroke.setBorder(new Border(new BorderStroke(
                Color.color(1.0, 1.0, 1.0, 0.25),
                BorderStrokeStyle.SOLID, new CornerRadii(radius), new BorderWidths(0.75))));
    }

    private void updateEffects() {
        M3GlassPane control = getSkinnable();
        saturationAdjust.setSaturation(effectiveSaturation());
        if (control.isButtonMode()) {
            saturationAdjust.setBrightness(0.08);
        } else {
            saturationAdjust.setBrightness(0.0);
        }
    }

    private void updateShadow() {
        M3GlassPane control = getSkinnable();
        if (control == null) return;
        if (control.isShadow()) {
            double dragElev = dragElevation.get();
            double sRadius = 24.0 + dragElev * 16.0;
            double offsetY = 8.0 + dragElev * 10.0;
            double alpha = 0.32 + dragElev * 0.14;
            root.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.color(0, 0, 0, alpha), sRadius, 0.0, 0.0, offsetY));
        } else {
            root.setEffect(null);
        }
    }

    private void placeSpecularLights(double t) {
        M3GlassPane control = getSkinnable();
        if (control == null) return;
        double w = control.getWidth();
        double h = control.getHeight();
        double intensity = clamp01(control.getSpecular());
        if (w <= 0 || h <= 0 || intensity <= 0.0) {
            specularLight1.setVisible(false);
            specularLight2.setVisible(false);
            return;
        }
        specularLight1.setVisible(true);
        specularLight2.setVisible(true);

        // Exact Lissajous orbit paths from liquidGL fragment shader
        double l1x = (Math.sin(t * 0.2) * 0.6 + 0.5) * w;
        double l1y = (Math.cos(t * 0.3) * 0.6 + 0.5) * h;
        double l2x = (Math.sin(t * -0.4 + 1.5) * 0.6 + 0.5) * w;
        double l2y = (Math.cos(t * 0.25 - 0.5) * 0.6 + 0.5) * h;
        double r1 = 0.4 * Math.min(w, h);
        double r2 = 0.5 * Math.min(w, h);

        specularLight1.setBackground(new Background(new BackgroundFill(
                new RadialGradient(0, 0, 0.5, 0.5, 0.5, true,
                        CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.color(1, 1, 1, 0.10 * intensity)),
                        new Stop(1.0, Color.TRANSPARENT)),
                new CornerRadii(9999), null)));
        specularLight2.setBackground(new Background(new BackgroundFill(
                new RadialGradient(0, 0, 0.5, 0.5, 0.5, true,
                        CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.color(1, 1, 1, 0.08 * intensity)),
                        new Stop(1.0, Color.TRANSPARENT)),
                new CornerRadii(9999), null)));
        specularLight1.resizeRelocate(l1x - r1, l1y - r1, r1 * 2, r1 * 2);
        specularLight2.resizeRelocate(l2x - r2, l2y - r2, r2 * 2, r2 * 2);
    }

    private double effectiveSaturation() {
        M3GlassPane control = getSkinnable();
        double s = control.getSaturation();
        if (s >= 0.0) return s;
        if (control.isButtonMode()) return 0.20;
        return control.getVariant() == GlassVariant.CLEAR ? 0.10 : 0.18;
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    @Override
    public void dispose() {
        M3GlassPane control = getSkinnable();
        if (control != null) {
            control.getTransforms().removeAll(rotateX, rotateY);
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
            control.bevelDepthProperty().removeListener(renderPropertyListener);
            control.bevelWidthProperty().removeListener(renderPropertyListener);
            control.frostProperty().removeListener(renderPropertyListener);
            control.aberrationProperty().removeListener(renderPropertyListener);
            control.magnifyProperty().removeListener(renderPropertyListener);
            control.mapStyleProperty().removeListener(invalidateListener);
            control.nikdelvinDepthProperty().removeListener(renderPropertyListener);
            control.nikdelvinStrengthProperty().removeListener(renderPropertyListener);
            control.fluidInteractionProperty().removeListener(invalidateListener);
            control.specularProperty().removeListener(invalidateListener);
            control.shadowProperty().removeListener(invalidateListener);
            control.tiltProperty().removeListener(invalidateListener);
            control.tiltFactorProperty().removeListener(renderPropertyListener);
            control.tiltEaseProperty().removeListener(renderPropertyListener);
            control.draggableProperty().removeListener(invalidateListener);
            control.buttonModeProperty().removeListener(invalidateListener);
            control.specularAngleProperty().removeListener(specularAngleListener);
            control.pressedProperty().removeListener(pressedListener);
            control.hoverProperty().removeListener(hoverListener);
            control.removeEventHandler(MouseEvent.MOUSE_PRESSED, pressHandler);
            control.removeEventHandler(MouseEvent.MOUSE_RELEASED, releaseHandler);
            control.removeEventHandler(MouseEvent.MOUSE_MOVED, moveHandler);
            control.removeEventHandler(MouseEvent.MOUSE_DRAGGED, dragHandler);
            control.removeEventHandler(MouseEvent.MOUSE_EXITED, exitHandler);
            if (currentScene != null) {
                currentScene.getProperties().removeListener(themeListener);
                currentScene = null;
            }
        }
        SpringAnimation.stop(bevelFlatten);
        SpringAnimation.stop(buttonScale);
        SpringAnimation.stop(dragElevation);
        if (control != null) {
            SpringAnimation.stop(control.specularAngleProperty());
        }
        physicsTimer.stop();
        backdropImage = null;
        refractedImage = null;
        renderDirty = true;
        backdropView.setImage(null);
        sharpView.setImage(null);
        backdropView.setEffect(null);
        root.setEffect(null);
        super.dispose();
    }
}
