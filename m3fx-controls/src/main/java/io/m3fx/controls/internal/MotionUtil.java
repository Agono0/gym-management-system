package io.m3fx.controls.internal;

import io.m3fx.core.token.MotionTokens;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 * Shared M3 motion helpers: press-scale feedback and the Expressive spring interpolator.
 *
 * <p>See <a href="https://m3.material.io/styles/motion/overview">M3 motion spec</a>. Components
 * must use these helpers instead of scattering ad-hoc timelines.
 */
public final class MotionUtil {

    private static final String PRESS_SCALE_HANDLERS = "m3fx.press-scale-handlers";

    private MotionUtil() {}

    /**
     * Adds a subtle press-scale (0.98) feedback animation to a node.
     *
     * @param node the interactive node
     */
    public static void addPressScale(Node node) {
        removePressScale(node);
        javafx.event.EventHandler<MouseEvent> pressed =
            e -> playScale(node, 0.98, MotionTokens.DURATION_SHORT_2);
        javafx.event.EventHandler<MouseEvent> released =
            e -> playScale(node, 1.0, MotionTokens.DURATION_SHORT_3);
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, pressed);
        node.addEventHandler(MouseEvent.MOUSE_RELEASED, released);
        node.getProperties().put(PRESS_SCALE_HANDLERS, new javafx.event.EventHandler<?>[] {pressed, released});
        }

        /** Removes press-scale handlers and stops the current transition. */
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
        Object existing = node.getProperties().remove("m3fx.press-scale");
        if (existing instanceof ScaleTransition running) {
            running.stop();
        }
    }

    private static void playScale(Node node, double to, int millis) {
        // Stop the running press-scale first so quick taps never fight each other mid-flight.
        Object existing = node.getProperties().get("m3fx.press-scale");
        if (existing instanceof ScaleTransition running) {
            running.stop();
        }
        ScaleTransition scale = new ScaleTransition(Duration.millis(millis), node);
        node.getProperties().put("m3fx.press-scale", scale);
        scale.setToX(to);
        scale.setToY(to);
        scale.setInterpolator(MotionTokens.spring());
        scale.play();
    }

    /**
     * Slides and fades a node in (used by snackbar, sheets, menus).
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
     * press/size animations where a full physics spring is overkill).
     *
     * @return a spring-like interpolator
     */
    public static Interpolator spring() {
        return MotionTokens.spring();
    }
}
