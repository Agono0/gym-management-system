package io.m3fx.controls.snackbar;

import io.m3fx.controls.internal.OverlayLayer;
import io.m3fx.core.token.MotionTokens;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * M3 snackbar with a message queue, optional action and close button.
 *
 * <p>See <a href="https://m3.material.io/components/snackbar/overview">M3 snackbar</a>.
 * Inverse-surface container (small shape, 4dp), 48dp minimum height; text inverse-on-surface,
 * action inverse-primary. Messages queue FIFO; each shows for {@link #DEFAULT_DURATION_MS} unless
 * it has an action ({@link #ACTION_DURATION_MS} per spec).
 */
public class M3Snackbar {

    /** Default short-message duration in ms. */
    public static final int DEFAULT_DURATION_MS = 4000;
    /** Duration when an action is present. */
    public static final int ACTION_DURATION_MS = 10000;

    /** One queued message. */
    public record Message(String text, String actionText, Runnable action) {}

    private final Deque<Message> queue = new ArrayDeque<>();
    private final OverlayLayer layer;
    private final PauseTransition gap = new PauseTransition(
            Duration.millis(MotionTokens.DURATION_SHORT_2));
    private PauseTransition autoDismiss;
    private Node current;
    private boolean showing;

    /**
     * Creates a snackbar bound to an overlay layer.
     *
     * @param layer the overlay layer transient messages show in
     */
    public M3Snackbar(OverlayLayer layer) {
        if (layer == null) {
            throw new IllegalArgumentException("layer must not be null");
        }
        this.layer = layer;
        gap.setOnFinished(e -> pump());
    }

    /**
     * Queues a plain message.
     *
     * @param text message text
     */
    public void show(String text) {
        queue.add(new Message(text, null, null));
        pump();
    }

    /**
     * Queues a message with an action.
     *
     * @param text message text
     * @param actionText action label
     * @param action action handler
     */
    public void show(String text, String actionText, Runnable action) {
        queue.add(new Message(text, actionText, action));
        pump();
    }

    private void pump() {
        if (showing || queue.isEmpty()) {
            return;
        }
        showing = true;
        Message message = queue.poll();
        HBox bar = new HBox(8);
        bar.getStyleClass().add("m3-snackbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 8, 6, 16));
        Label text = new Label(message.text() == null ? "" : message.text());
        text.getStyleClass().add("m3-snackbar-text");
        text.setWrapText(true);
        HBox.setHgrow(text, Priority.ALWAYS);
        bar.getChildren().add(text);
        if (message.actionText() != null) {
            Button action = new Button(message.actionText());
            action.getStyleClass().add("m3-snackbar-action");
            action.setOnAction(e -> {
                if (message.action() != null) {
                    message.action().run();
                }
                dismiss();
            });
            bar.getChildren().add(action);
        }
        Button close = new Button("✕");
        close.getStyleClass().add("m3-snackbar-close");
        close.setOnAction(e -> dismiss());
        close.setAccessibleText("Dismiss");
        bar.getChildren().add(close);
        Region bottomMargin = new Region();
        bottomMargin.setMinHeight(24);
        javafx.scene.layout.VBox wrapper =
                new javafx.scene.layout.VBox(bar, bottomMargin);
        wrapper.setAlignment(Pos.BOTTOM_CENTER);
        current = wrapper;
        layer.showTransient(wrapper);
        bar.setAccessibleText(message.text());
        autoDismiss = new PauseTransition(
                Duration.millis(
                        message.actionText() != null
                                ? ACTION_DURATION_MS
                                : DEFAULT_DURATION_MS));
        autoDismiss.setOnFinished(e -> dismiss());
        autoDismiss.play();
    }

    /** Dismisses the current message with a fade-out and shows the next queued one. */
    public void dismiss() {
        if (!showing) {
            return;
        }
        showing = false;
        if (autoDismiss != null) {
            autoDismiss.stop();
            autoDismiss = null;
        }
        Node node = current;
        current = null;
        if (node == null || node.getScene() == null) {
            layer.hideTransient();
            gap.playFromStart();
            return;
        }
        FadeTransition fade =
                new FadeTransition(Duration.millis(MotionTokens.DURATION_SHORT_4), node);
        fade.setToValue(0);
        fade.setInterpolator(MotionTokens.emphasizedAccelerate());
        fade.setOnFinished(e -> {
            // Only clear the layer if no newer message replaced this one mid-fade.
            if (current == null) {
                layer.hideTransient();
                gap.playFromStart();
            }
        });
        fade.play();
    }
}
