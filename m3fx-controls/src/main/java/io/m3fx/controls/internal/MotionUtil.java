package io.m3fx.controls.internal;

import io.m3fx.core.token.MotionTokens;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 * Shared M3 motion helpers: press-scale feedback and the Expressive spring physics.
 *
 * <p>See <a href="https://m3.material.io/styles/motion/overview">M3 motion spec</a>. Components
 * use these helpers instead of scattering ad-hoc timelines. Apple-flavored page-transition
 * helpers ({@link #enter(Node, MotionDirection)}, {@link #exit(Node, Runnable)}, {@link #splashIn(Node)},
 * {@link #splashOut(Node, Runnable)}, {@link #shake(Node)}, {@link #morphFromBounds(Node, Bounds, boolean, Runnable)})
 * are also here; they are intentionally not M3 spec values (see {@link MotionTokens} Apple tokens).
 *
 * <p>All helpers run on the FX thread, animate transform-only properties only (opacity,
 * translate, scale — never layout-affecting ones), keep at most one tracked timeline per node,
 * and stop/reset automatically when the node leaves the scene.
 */
public final class MotionUtil {

    private static final String PRESS_SCALE_HANDLERS = "m3fx.press-scale-handlers";
    private static final String TRACKED_TIMELINE_KEY = "m3fx.motion.tracked-timeline";
    private static final String SCENE_CLEANUP_LISTENER_KEY = "m3fx.motion.scene-cleanup-listener";

    /** Direction a node travels from while entering (the node moves toward rest at 0,0). */
    public enum MotionDirection {
        /** Enters from below, rising up (iOS sheet presentation). */
        UP,
        /** Enters from above, settling down. */
        DOWN,
        /** Enters from the right, sliding left (forward push). */
        LEFT,
        /** Enters from the left, sliding right (back push). */
        RIGHT
    }

    private static final double ENTER_OFFSET_PX = 14.0;
    private static final double ENTER_SCALE_FROM = 0.98;
    private static final double EXIT_LIFT_PX = 8.0;
    private static final double EXIT_SCALE_TO = 0.99;
    private static final double SPLASH_IN_SCALE_FROM = 1.06;
    private static final double SPLASH_OUT_SCALE_TO = 1.04;
    private static final double SHAKE_OFFSET_PX = 12.0;

    private MotionUtil() {}

    /**
     * Adds a tactile spring press-scale (0.97 on press, spring bounce on release) to a node.
     *
     * @param node the interactive node
     */
    public static void addPressScale(Node node) {
        addPressScale(node, 0.97);
    }

    /**
     * Adds a tactile spring press-scale with a custom pressed scale (spring bounce on release).
     *
     * @param node the interactive node
     * @param pressedScale the scale while pressed (e.g. 0.96 for subtle, 0.94 for pronounced)
     */
    public static void addPressScale(Node node, double pressedScale) {
        removePressScale(node);
        javafx.event.EventHandler<MouseEvent> pressed =
                e -> SpringAnimation.animateScale(node, pressedScale, SpringAnimation.FAST);
        javafx.event.EventHandler<MouseEvent> released =
                e -> SpringAnimation.animateScale(node, 1.0, SpringAnimation.BOUNCY);
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, pressed);
        node.addEventHandler(MouseEvent.MOUSE_RELEASED, released);
        node.getProperties().put(PRESS_SCALE_HANDLERS, new javafx.event.EventHandler<?>[] {pressed, released});
    }

    /** Removes press-scale handlers and stops any running spring scale. */
    public static void removePressScale(Node node) {
        Object handlers = node.getProperties().remove(PRESS_SCALE_HANDLERS);
        if (handlers instanceof javafx.event.EventHandler<?>[] registered) {
            @SuppressWarnings("unchecked")
            javafx.event.EventHandler<MouseEvent> pressed =
                    (javafx.event.EventHandler<MouseEvent>) registered[0];
            @SuppressWarnings("unchecked")
            javafx.event.EventHandler<MouseEvent> released =
                    (javafx.event.EventHandler<MouseEvent>) registered[1];
            node.removeEventHandler(MouseEvent.MOUSE_PRESSED, pressed);
            node.removeEventHandler(MouseEvent.MOUSE_RELEASED, released);
        }
        SpringAnimation.stop(node.scaleXProperty());
        SpringAnimation.stop(node.scaleYProperty());
        node.setScaleX(1.0);
        node.setScaleY(1.0);
    }

    /**
     * Slides and fades a node in using spring physics (used by snackbar, sheets, menus).
     *
     * @param node the entering node
     * @param fromY starting vertical offset in px
     * @param millis duration in ms
     */
    public static void slideIn(Node node, double fromY, int millis) {
        node.setOpacity(0);
        TranslateTransition slide =
                new TranslateTransition(Duration.millis(millis), node);
        slide.setFromY(fromY);
        slide.setToY(0);
        slide.setInterpolator(MotionTokens.emphasizedDecelerate());
        slide.play();
        javafx.animation.FadeTransition fade =
                new javafx.animation.FadeTransition(Duration.millis(millis), node);
        fade.setToValue(1);
        fade.play();
    }

    /**
     * M3 Expressive spring interpolator (slight overshoot, matches the spec's spring feel for
     * transitions where a full physics spring timer is overkill).
     *
     * @return a spring-like interpolator
     */
    public static Interpolator spring() {
        return MotionTokens.spring();
    }

    /**
     * Apple-style page entrance with default direction ({@link MotionDirection#UP}): fade
     * 0&rarr;1, rise ~14px to rest, scale 0.98&rarr;1, over ~400ms with the Apple sheet ease.
     * Transform-only; any in-flight motion on the node is cancelled first.
     *
     * @param node the entering node
     */
    public static void enter(Node node) {
        enter(node, MotionDirection.UP);
    }

    /**
     * Apple-style page entrance: fade 0&rarr;1, offset ~14px toward rest, scale 0.98&rarr;1,
     * over ~400ms with the Apple sheet ease {@code cubic-bezier(0.32, 0.72, 0, 1)}. The long
     * decelerating tail is what makes the motion read as fluid rather than snappy.
     *
     * <p>Use {@link #enter(Node, MotionDirection, double)} with a from-scale of 1.0 for large
     * subtrees (whole pages): a non-1.0 scale feeds JavaFX's snap-scale-aware layout insets and
     * forces a full subtree re-layout on every animation frame, which on tall pages shows up as
     * a visible hitch.
     *
     * @param node the entering node
     * @param direction the direction the node travels from
     */
    public static void enter(Node node, MotionDirection direction) {
        enter(node, direction, ENTER_SCALE_FROM);
    }

    /**
     * Apple-style page entrance with a configurable starting scale (1.0 disables scaling).
     *
     * @param node the entering node
     * @param direction the direction the node travels from
     * @param fromScale the scale the node starts at (1.0 = no scale animation)
     */
    public static void enter(Node node, MotionDirection direction, double fromScale) {
        requireFxThread("enter");
        stopTracked(node);
        SpringAnimation.stopTransforms(node);
        installSceneExitCleanup(node);

        node.setOpacity(0);
        node.setTranslateX(offsetX(direction));
        node.setTranslateY(offsetY(direction));
        node.setScaleX(fromScale);
        node.setScaleY(fromScale);

        Interpolator ease = MotionTokens.appleSheetEase();
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(MotionTokens.APPLE_TRANSITION_DURATION),
                new KeyValue(node.opacityProperty(), 1.0, ease),
                new KeyValue(node.translateXProperty(), 0, ease),
                new KeyValue(node.translateYProperty(), 0, ease),
                new KeyValue(node.scaleXProperty(), 1.0, ease),
                new KeyValue(node.scaleYProperty(), 1.0, ease)));
        timeline.setOnFinished(e -> clearTracked(node));
        track(node, timeline);
        timeline.play();
    }

    /**
     * Apple-style quick exit: fade 1&rarr;0 with a slight ~8px lift and scale 0.99 over
     * ~150ms, then resets the node's transforms to identity and invokes {@code onDone}.
     * If the node leaves the scene mid-exit the animation is cancelled and transforms are
     * reset instead; {@code onDone} is not invoked in that case (the node is already gone).
     *
     * @param node the exiting node
     * @param onDone optional callback run after the exit completes, may be {@code null}
     */
    public static void exit(Node node, Runnable onDone) {
        requireFxThread("exit");
        stopTracked(node);
        SpringAnimation.stopTransforms(node);
        installSceneExitCleanup(node);

        boolean[] done = {false};
        Runnable finish = () -> {
            if (done[0]) {
                return;
            }
            done[0] = true;
            resetTransforms(node);
            clearTracked(node);
            if (onDone != null) {
                onDone.run();
            }
        };

        Interpolator ease = MotionTokens.appleFadeEase();
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(MotionTokens.APPLE_FADE_DURATION),
                new KeyValue(node.opacityProperty(), 0.0, ease),
                new KeyValue(node.translateYProperty(), node.getTranslateY() - EXIT_LIFT_PX, ease),
                new KeyValue(node.scaleXProperty(), EXIT_SCALE_TO, ease),
                new KeyValue(node.scaleYProperty(), EXIT_SCALE_TO, ease)));
        timeline.setOnFinished(e -> finish.run());
        track(node, timeline);
        timeline.play();
    }

    /**
     * Apple-style splash reveal (app launch): fade 0&rarr;1 while settling scale
     * 1.06&rarr;1 over {@link MotionTokens#APPLE_SPLASH_DURATION} (600ms) with the Apple sheet
     * ease. The node starts slightly oversized and breathes down to rest while fading in, which
     * reads as the content arriving from behind the window rather than snapping into it.
     *
     * <p>Any in-flight motion on the node is cancelled first. On finish the node is left at
     * opacity 1 and identity transforms (translate 0, scale 1) and the tracked timeline is
     * cleared. Non-M3, Apple-flavored (see {@link MotionTokens} Apple block).
     *
     * @param node the node revealing in (typically the app's root container)
     */
    public static void splashIn(Node node) {
        requireFxThread("splashIn");
        stopTracked(node);
        SpringAnimation.stopTransforms(node);
        installSceneExitCleanup(node);

        resetTransforms(node);
        node.setOpacity(0.0);
        node.setScaleX(SPLASH_IN_SCALE_FROM);
        node.setScaleY(SPLASH_IN_SCALE_FROM);

        Interpolator ease = MotionTokens.appleSheetEase();
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(MotionTokens.APPLE_SPLASH_DURATION),
                new KeyValue(node.opacityProperty(), 1.0, ease),
                new KeyValue(node.scaleXProperty(), 1.0, ease),
                new KeyValue(node.scaleYProperty(), 1.0, ease)));
        timeline.setOnFinished(e -> clearTracked(node));
        track(node, timeline);
        timeline.play();
    }

    /**
     * Apple-style splash dismissal: fade 1&rarr;0 while swelling scale&rarr;1.04 over
     * ~300ms ({@link MotionTokens#DURATION_MEDIUM_2}) with the Apple fade ease, then resets the
     * node's transforms and opacity to identity/1.0, clears tracking, and invokes {@code onDone}
     * exactly once.
     *
     * <p>If the node leaves the scene mid-flight the animation is cancelled and transforms are
     * reset instead; {@code onDone} is not invoked in that case (the node is already gone),
     * matching {@link #exit(Node, Runnable)}.
     *
     * @param node the node dismissing (typically the splash/login overlay)
     * @param onDone callback run exactly once after the exit completes, may be {@code null};
     *               not invoked if the node leaves the scene mid-flight
     */
    public static void splashOut(Node node, Runnable onDone) {
        requireFxThread("splashOut");
        stopTracked(node);
        SpringAnimation.stopTransforms(node);
        installSceneExitCleanup(node);

        boolean[] done = {false};
        Runnable finish = () -> {
            if (done[0]) {
                return;
            }
            done[0] = true;
            resetTransforms(node);
            clearTracked(node);
            if (onDone != null) {
                onDone.run();
            }
        };

        Interpolator ease = MotionTokens.appleFadeEase();
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(MotionTokens.DURATION_MEDIUM_2),
                new KeyValue(node.opacityProperty(), 0.0, ease),
                new KeyValue(node.scaleXProperty(), SPLASH_OUT_SCALE_TO, ease),
                new KeyValue(node.scaleYProperty(), SPLASH_OUT_SCALE_TO, ease)));
        timeline.setOnFinished(e -> finish.run());
        track(node, timeline);
        timeline.play();
    }

    /**
     * Apple-style validation shake: kicks the node 12px left, then springs translateX back to 0
     * with the {@link SpringAnimation#BOUNCY} spring. The under-damped overshoot produces the
     * natural two-bump Apple-style shake and lands exactly at translateX 0 (the spring's settle
     * logic sets the target value exactly, and degenerate inputs snap to the target).
     *
     * <p>Any in-flight motion on the node is cancelled first; a repeated call retargets the same
     * spring without stacking. Use it to reject an invalid form field or login attempt.
     *
     * @param node the node to shake (typically the field or card being rejected)
     */
    public static void shake(Node node) {
        requireFxThread("shake");
        stopTracked(node);
        SpringAnimation.stopTransforms(node);
        installSceneExitCleanup(node);

        node.setTranslateX(-SHAKE_OFFSET_PX);
        SpringAnimation.animate(node.translateXProperty(), 0, SpringAnimation.BOUNCY, null);
    }

    /**
     * Shared-element morph: makes {@code node} initially appear to occupy
     * {@code startBoundsInScene} (scene coordinates) and then fluidly morphs it to its natural
     * place over {@link MotionTokens#APPLE_SHEET_DURATION} (450ms) with the Apple sheet ease.
     * Intended for container-to-container shared-element transitions, e.g. the login card
     * morphing into the main app shell.
     *
     * <p>Mechanics: the node's translate/scale are first reset to identity and its layout bounds
     * are measured in scene coordinates as the reference rect {@code (refMin, refSize)}. The node
     * is then scaled by {@code startBounds.size / refSize} and translated by
     * {@code startBounds.min - refMin - (1 - scale) * refSize / 2}, which places the node's layout
     * bounds exactly on {@code startBoundsInScene}. The animation then drives scale&rarr;1 and
     * translate&rarr;0.
     *
     * <p>Pivot note: JavaFX applies {@code scaleX}/{@code scaleY} about the <em>center</em> of the
     * node's layout bounds (there is no pivot property on {@link Node}), so the translate formula
     * above includes the {@code (1 - scale) * refSize / 2} term that cancels the centered pivot's
     * offset; with a naive top-left-pivot formula the start rect would land off by exactly that
     * term. Ancestor transforms between the node and the scene root are assumed identity (the
     * common case for a container placed directly under a scene root); with a transformed
     * ancestor the start rect lands off by the ancestor's scale/translation &mdash; keep ancestors
     * untransformed or pre-normalize them.
     *
     * <p>If {@code animate} is false, the node is not in a scene, or the bounds are
     * {@code null}/empty/non-finite, no transform is changed and {@code onDone} is invoked once
     * (if non-null). If the node leaves the scene mid-morph the animation is cancelled and
     * transforms are reset instead; {@code onDone} is not invoked in that case, matching
     * {@link #exit(Node, Runnable)}. Transform-only (scale/translate; opacity untouched).
     *
     * @param node the node morphing into place
     * @param startBoundsInScene the scene-space rect the node should appear to start from
     * @param animate false to skip the transform and jump straight to the callback
     * @param onDone callback run exactly once after the settle completes, may be {@code null}
     */
    public static void morphFromBounds(Node node, Bounds startBoundsInScene, boolean animate, Runnable onDone) {
        requireFxThread("morphFromBounds");
        if (!animate || node.getScene() == null || !isUsableBounds(startBoundsInScene)) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }

        stopTracked(node);
        SpringAnimation.stopTransforms(node);
        installSceneExitCleanup(node);

        // Measure the reference rect with the node's own transforms at identity so the math
        // below is relative to the natural, untransformed placement (see Javadoc).
        node.setTranslateX(0);
        node.setTranslateY(0);
        node.setScaleX(1.0);
        node.setScaleY(1.0);
        Bounds layout = node.getLayoutBounds();
        Point2D refMin = node.localToScene(layout.getMinX(), layout.getMinY());
        double refWidth = layout.getWidth();
        double refHeight = layout.getHeight();
        if (refMin == null
                || refWidth <= 0
                || refHeight <= 0
                || !Double.isFinite(refMin.getX())
                || !Double.isFinite(refMin.getY())) {
            // Degenerate node (no measurable bounds): leave it at identity, no morph.
            if (onDone != null) {
                onDone.run();
            }
            return;
        }

        double scaleX = startBoundsInScene.getWidth() / refWidth;
        double scaleY = startBoundsInScene.getHeight() / refHeight;
        // JavaFX scales about the CENTER of the layout bounds (no pivot property exists on Node),
        // so the translate must cancel the (1 - scale) * size / 2 offset the centered pivot adds:
        // parent(p) = scale * p + (1 - scale) * center + translate  =>  translate =
        // startMin - refMin - (1 - scale) * refSize / 2. Verified against localToScene on JDK 27 /
        // JavaFX 21.0.4; see MotionUtilTest#morphFromBoundsStartsAtGivenBoundsAndSettlesToNaturalPlace.
        double translateX = startBoundsInScene.getMinX() - refMin.getX() - (1 - scaleX) * refWidth / 2;
        double translateY = startBoundsInScene.getMinY() - refMin.getY() - (1 - scaleY) * refHeight / 2;
        if (!Double.isFinite(scaleX)
                || !Double.isFinite(scaleY)
                || !Double.isFinite(translateX)
                || !Double.isFinite(translateY)) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        node.setScaleX(scaleX);
        node.setScaleY(scaleY);
        node.setTranslateX(translateX);
        node.setTranslateY(translateY);

        boolean[] done = {false};
        Runnable finish = () -> {
            if (done[0]) {
                return;
            }
            done[0] = true;
            node.setScaleX(1.0);
            node.setScaleY(1.0);
            node.setTranslateX(0);
            node.setTranslateY(0);
            clearTracked(node);
            if (onDone != null) {
                onDone.run();
            }
        };

        Interpolator ease = MotionTokens.appleSheetEase();
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(MotionTokens.APPLE_SHEET_DURATION),
                new KeyValue(node.scaleXProperty(), 1.0, ease),
                new KeyValue(node.scaleYProperty(), 1.0, ease),
                new KeyValue(node.translateXProperty(), 0, ease),
                new KeyValue(node.translateYProperty(), 0, ease)));
        timeline.setOnFinished(e -> finish.run());
        track(node, timeline);
        timeline.play();
    }

    /**
     * Cancels any tracked MotionUtil transition on the node, stops its transform springs,
     * and resets opacity/translate/scale to identity. Safe to call repeatedly.
     *
     * @param node the node to reset
     */
    public static void cancelNodeMotion(Node node) {
        requireFxThread("cancelNodeMotion");
        stopTracked(node);
        SpringAnimation.stopTransforms(node);
        resetTransforms(node);
    }

    private static double offsetX(MotionDirection direction) {
        return switch (direction) {
            case LEFT -> -ENTER_OFFSET_PX;
            case RIGHT -> ENTER_OFFSET_PX;
            default -> 0;
        };
    }

    private static double offsetY(MotionDirection direction) {
        return switch (direction) {
            case UP -> ENTER_OFFSET_PX;
            case DOWN -> -ENTER_OFFSET_PX;
            default -> 0;
        };
    }

    /**
     * True when the bounds are non-null, have area and are finite (safe morph input). A
     * zero-sized box is rejected even though {@link Bounds#isEmpty()} only reports negative
     * dimensions &mdash; a zero-area morph target would produce a degenerate scale of 0.
     */
    private static boolean isUsableBounds(Bounds bounds) {
        return bounds != null
                && bounds.getWidth() > 0
                && bounds.getHeight() > 0
                && Double.isFinite(bounds.getMinX())
                && Double.isFinite(bounds.getMinY())
                && Double.isFinite(bounds.getWidth())
                && Double.isFinite(bounds.getHeight());
    }

    private static void resetTransforms(Node node) {
        node.setOpacity(1.0);
        node.setTranslateX(0);
        node.setTranslateY(0);
        node.setScaleX(1.0);
        node.setScaleY(1.0);
    }

    private static void track(Node node, Timeline timeline) {
        node.getProperties().put(TRACKED_TIMELINE_KEY, timeline);
    }

    private static void stopTracked(Node node) {
        Object tracked = node.getProperties().remove(TRACKED_TIMELINE_KEY);
        if (tracked instanceof Timeline timeline) {
            // Null the finished handler first so a stale stop cannot fire stale callbacks.
            timeline.setOnFinished(null);
            timeline.stop();
        }
    }

    private static void clearTracked(Node node) {
        Object tracked = node.getProperties().get(TRACKED_TIMELINE_KEY);
        if (tracked == null || tracked instanceof Timeline t && t.getStatus() == Timeline.Status.STOPPED) {
            node.getProperties().remove(TRACKED_TIMELINE_KEY);
        }
    }

    /**
     * Installs a one-time-per-node listener that cancels motion and resets transforms when
     * the node leaves the scene, so no animation outlives its node.
     */
    private static void installSceneExitCleanup(Node node) {
        if (node.getProperties().containsKey(SCENE_CLEANUP_LISTENER_KEY)) {
            return;
        }
        ChangeListener<javafx.scene.Scene> cleanup = (obs, oldScene, newScene) -> {
            if (newScene == null) {
                stopTracked(node);
                SpringAnimation.stopTransforms(node);
                resetTransforms(node);
            }
        };
        node.sceneProperty().addListener(cleanup);
        node.getProperties().put(SCENE_CLEANUP_LISTENER_KEY, cleanup);
    }

    private static void requireFxThread(String operation) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException(
                    "MotionUtil." + operation + " must be called on the JavaFX Application Thread");
        }
    }
}
