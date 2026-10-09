package io.m3fx.controls.internal;

import io.m3fx.core.token.MotionTokens;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import java.util.function.Supplier;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Shared M3 ripple helper: an expanding circle clipped to the host, fading out on release.
 *
 * <p>Components call {@link #attach(Node, Region)} with an overlay region; the ripple circle is
 * added to that region on press and animated with {@link MotionTokens} timing.
 */
public final class Ripple {

    private static final String HANDLER = "m3fx.ripple-handler";

    private Ripple() {}

    /**
     * Attaches a press ripple to a node.
     *
     * @param target the interactive node
     * @param layer a full-size pane the ripple circle is drawn inside (must be mouse-transparent)
     */
    public static void attach(Node target, javafx.scene.layout.Pane layer) {
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
    public static void attach(Node target, javafx.scene.layout.Pane layer, Supplier<Color> ink) {
        detach(target, layer);
        layer.setMouseTransparent(true);
        javafx.event.EventHandler<javafx.scene.input.MouseEvent> handler =
            event -> {
                    if (target.isDisabled() || event.getButton() != javafx.scene.input.MouseButton.PRIMARY) {
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
                    Duration scaleDuration = Duration.millis(MotionTokens.DURATION_MEDIUM_2);
                    ScaleTransition scale = new ScaleTransition(scaleDuration, ripple);
                    scale.setToX(targetScale);
                    scale.setToY(targetScale);
                    scale.setInterpolator(MotionTokens.emphasizedDecelerate());
                    FadeTransition fade =
                            new FadeTransition(
                                    Duration.millis(MotionTokens.DURATION_LONG_2), ripple);
                    fade.setFromValue(1.0);
                    fade.setToValue(0.0);
                    fade.setInterpolator(MotionTokens.standard());
                    ParallelTransition both = new ParallelTransition(scale, fade);
                    both.setOnFinished(e -> layer.getChildren().remove(ripple));
                    both.play();
                };
        target.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_PRESSED, handler);
        layer.getProperties().put(HANDLER, handler);
    }

    /** Removes the ripple press handler and any active ripple nodes from the layer. */
    public static void detach(Node target, javafx.scene.layout.Pane layer) {
        Object registered = layer.getProperties().remove(HANDLER);
        if (registered instanceof javafx.event.EventHandler<?> handler) {
            @SuppressWarnings("unchecked")
            javafx.event.EventHandler<javafx.scene.input.MouseEvent> mouseHandler =
                    (javafx.event.EventHandler<javafx.scene.input.MouseEvent>) handler;
            target.removeEventHandler(javafx.scene.input.MouseEvent.MOUSE_PRESSED, mouseHandler);
        }
        layer.getChildren().clear();
    }
}
