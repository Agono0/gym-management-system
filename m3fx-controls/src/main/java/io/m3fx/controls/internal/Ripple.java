package io.m3fx.controls.internal;

import io.m3fx.core.token.MotionTokens;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import java.util.function.Supplier;

/**
 * Shared M3 ripple helper: an expanding circle that grows from the press point, stays expanded
 * while pressed, and fades out on release.
 *
 * <p>Components call {@link #attach(Node, Pane)} with an overlay region; the ripple circle is
 * added to that region on press and animated with {@link MotionTokens} timing. The region should
 * be clipped by the owning control (rounded clip) so the ink never leaves the component bounds.
 * Ripples self-clean: the fade removes the circle, and {@link #detach(Node, Pane)} stops any
 * active ripple (call from skin dispose).
 */
public final class Ripple {

    private static final String HANDLER = "m3fx.ripple-handler";
    private static final String ACTIVE = "m3fx.ripple-active";

    private Ripple() {}

    /**
     * Attaches a press ripple to a node.
     *
     * @param target the interactive node
     * @param layer a full-size pane the ripple circle is drawn inside (must be mouse-transparent)
     */
    public static void attach(Node target, Pane layer) {
        attach(target, layer, () -> new Color(1, 1, 1, 1));
    }

    /**
     * Attaches a press ripple with a per-press ink color (usually the component's content color,
     * resolved live so dark themes ripple correctly).
     *
     * @param target the interactive node
     * @param layer a full-size pane the ripple circle is drawn inside (must be mouse-transparent)
     * @param ink supplier for the ripple ink color
     */
    public static void attach(Node target, Pane layer, Supplier<Color> ink) {
        detach(target, layer);
        layer.setMouseTransparent(true);
        EventHandler<MouseEvent> pressed =
            event -> {
                    if (target.isDisabled() || event.getButton() != MouseButton.PRIMARY) {
                        return;
                    }
                    double w =
                            layer.getWidth() <= 0
                                    ? target.getLayoutBounds().getWidth()
                                    : layer.getWidth();
                    double h =
                            layer.getHeight() <= 0
                                    ? target.getLayoutBounds().getHeight()
                                    : layer.getHeight();
                    double pressX = event.getX();
                    double pressY = event.getY();
                    double maxDist = Math.hypot(Math.max(pressX, w - pressX), Math.max(pressY, h - pressY));
                    double initialRadius = 5.0;
                    double targetScale = Math.max(1.0, (maxDist * 1.08) / initialRadius);
                    Color base = ink == null ? Color.WHITE : ink.get();
                    if (base == null) {
                        base = Color.WHITE;
                    }
                    Circle ripple =
                            new Circle(
                                    pressX,
                                    pressY,
                                    initialRadius,
                                    new Color(
                                            base.getRed(),
                                            base.getGreen(),
                                            base.getBlue(),
                                            0.14));
                    ripple.setMouseTransparent(true);
                    layer.getChildren().add(ripple);
                    ScaleTransition scale =
                            new ScaleTransition(
                                    Duration.millis(MotionTokens.DURATION_MEDIUM_2), ripple);
                    scale.setToX(targetScale);
                    scale.setToY(targetScale);
                    scale.setInterpolator(MotionTokens.emphasizedDecelerate());
                    scale.play();
                    // Only one ripple may be held at a time; retire any previous one.
                    fadeOutActive(layer);
                    layer.getProperties().put(ACTIVE, new ActiveRipple(ripple, scale, null));
                };
        EventHandler<MouseEvent> released = event -> fadeOutActive(layer);
        // Insurance: a press whose release is lost (e.g. the window loses focus) must not leave
        // the ink stuck expanded.
        javafx.beans.value.ChangeListener<Scene> sceneGone = (obs, oldV, newV) -> {
            if (newV == null) {
                fadeOutActive(layer);
            }
        };
        target.addEventHandler(MouseEvent.MOUSE_PRESSED, pressed);
        target.addEventHandler(MouseEvent.MOUSE_RELEASED, released);
        target.sceneProperty().addListener(sceneGone);
        layer.getProperties().put(HANDLER, new Object[] {pressed, released, sceneGone});
    }

    /** Removes the ripple press handler and any active ripple nodes from the layer. */
    public static void detach(Node target, Pane layer) {
        Object registered = layer.getProperties().remove(HANDLER);
        if (registered instanceof Object[] handlers && handlers.length == 3) {
            @SuppressWarnings("unchecked")
            EventHandler<MouseEvent> pressed = (EventHandler<MouseEvent>) handlers[0];
            @SuppressWarnings("unchecked")
            EventHandler<MouseEvent> released = (EventHandler<MouseEvent>) handlers[1];
            @SuppressWarnings("unchecked")
            javafx.beans.value.ChangeListener<Scene> sceneGone =
                    (javafx.beans.value.ChangeListener<Scene>) handlers[2];
            target.removeEventHandler(MouseEvent.MOUSE_PRESSED, pressed);
            target.removeEventHandler(MouseEvent.MOUSE_RELEASED, released);
            target.sceneProperty().removeListener(sceneGone);
        }
        stopActive(layer);
        layer.getChildren().clear();
    }

    /** Fades out the held ripple (on release, cancel or a newer press). */
    private static void fadeOutActive(Pane layer) {
        Object active = layer.getProperties().remove(ACTIVE);
        if (!(active instanceof ActiveRipple ripple) || ripple.fade != null) {
            return;
        }
        FadeTransition fade =
                new FadeTransition(Duration.millis(MotionTokens.DURATION_LONG_2), ripple.circle);
        fade.setFromValue(ripple.circle.getOpacity());
        fade.setToValue(0.0);
        fade.setInterpolator(MotionTokens.standard());
        fade.setOnFinished(e -> layer.getChildren().remove(ripple.circle));
        ripple.fade = fade;
        fade.play();
    }

    /** Stops and removes the active ripple without a fade (skin dispose). */
    private static void stopActive(Pane layer) {
        Object active = layer.getProperties().remove(ACTIVE);
        if (active instanceof ActiveRipple ripple) {
            ripple.scale.stop();
            if (ripple.fade != null) {
                ripple.fade.stop();
            }
            layer.getChildren().remove(ripple.circle);
        }
    }

    /** One in-flight ripple: the expanding circle plus its transitions. */
    private static final class ActiveRipple {
        final Circle circle;
        final ScaleTransition scale;
        FadeTransition fade; // null while the ripple is still held

        ActiveRipple(Circle circle, ScaleTransition scale, FadeTransition fade) {
            this.circle = circle;
            this.scale = scale;
            this.fade = fade;
        }
    }
}
