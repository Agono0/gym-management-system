package io.m3fx.controls.internal;

import io.m3fx.core.token.MotionTokens;
import io.m3fx.core.token.StateOpacity;
import java.util.Map;
import java.util.WeakHashMap;
import javafx.beans.value.ChangeListener;
import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Shared M3 state-layer overlay (hover 8%, focus 10%, pressed 10%, dragged 16%).
 *
 * <p>Components place one full-size overlay {@link Region} above their content and call
 * {@link #bind(Node, Region)}; the overlay tints itself on hover/focus/press. Components opt in
 * instead of reimplementing overlays.
 */
public final class StateLayer {

    private static final Map<Region, FadeTransition> ACTIVE = new WeakHashMap<>();
    private static final String LISTENERS = "m3fx.state-layer-listeners";

    private StateLayer() {}

    /**
     * Binds hover, focus and pressed opacity handling to an overlay region.
     *
     * <p>The overlay must already have its background color set (usually via CSS to the component's
     * content color); only its opacity is animated here.
     *
     * @param target the interactive node receiving mouse/keyboard focus events
     * @param overlay a full-size, mouse-transparent region stacked above the target content
     */
    public static void bind(Node target, Region overlay) {
        unbind(target, overlay);
        overlay.setMouseTransparent(true);
        overlay.setOpacity(0);
        ChangeListener<Boolean> hover = (obs, oldV, newV) -> refresh(target, overlay);
        ChangeListener<Boolean> pressed = (obs, oldV, newV) -> refresh(target, overlay);
        ChangeListener<Boolean> focused = (obs, oldV, newV) -> refresh(target, overlay);
        target.hoverProperty().addListener(hover);
        target.pressedProperty().addListener(pressed);
        target.focusedProperty().addListener(focused);
        overlay.getProperties().put(LISTENERS, new ChangeListener<?>[] {hover, pressed, focused});
    }

    /**
     * Binds hover, focus and pressed handling with an explicit layer color applied to the overlay.
     *
     * @param target the interactive node
     * @param overlay the overlay region
     * @param layerColor the state-layer color
     */
    public static void bind(Node target, Region overlay, Color layerColor) {
        if (layerColor != null) {
            overlay.setStyle(
                    "-fx-background-color: "
                            + toWeb(layerColor)
                            + "; -fx-background-radius: inherit;");
        }
        bind(target, overlay);
    }

    /** Removes state listeners and stops the overlay fade transition. */
    public static void unbind(Node target, Region overlay) {
        Object listeners = overlay.getProperties().remove(LISTENERS);
        if (listeners instanceof ChangeListener<?>[] registered) {
            @SuppressWarnings("unchecked")
            ChangeListener<Boolean> hover = (ChangeListener<Boolean>) registered[0];
            @SuppressWarnings("unchecked")
            ChangeListener<Boolean> pressed = (ChangeListener<Boolean>) registered[1];
            @SuppressWarnings("unchecked")
            ChangeListener<Boolean> focused = (ChangeListener<Boolean>) registered[2];
            target.hoverProperty().removeListener(hover);
            target.pressedProperty().removeListener(pressed);
            target.focusedProperty().removeListener(focused);
        }
        FadeTransition running = ACTIVE.remove(overlay);
        if (running != null) {
            running.stop();
        }
    }

    private static void refresh(Node target, Region overlay) {
        double to;
        if (target.isPressed()) {
            to = StateOpacity.PRESSED;
        } else if (target.isHover()) {
            to = StateOpacity.HOVER;
        } else if (target.isFocused()) {
            to = StateOpacity.FOCUS;
        } else {
            to = 0;
        }
        // One transition per overlay: stop the running one so rapid hover/press flapping
        // never stacks competing fades (the main cause of flicker).
        FadeTransition running = ACTIVE.get(overlay);
        if (running != null) {
            running.stop();
        }
        FadeTransition fade =
                new FadeTransition(Duration.millis(MotionTokens.DURATION_SHORT_2), overlay);
        fade.setFromValue(overlay.getOpacity());
        fade.setToValue(to);
        ACTIVE.put(overlay, fade);
        fade.play();
    }

    private static String toWeb(Color color) {
        return String.format(
                "#%02x%02x%02x",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }
}
