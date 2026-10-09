package io.m3fx.controls.common;

import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

/**
 * M3 tooltip: plain and rich variants.
 *
 * <p>See <a href="https://m3.material.io/components/tooltips/overview">M3 tooltips</a>. Plain
 * tooltips use inverse-surface with inverse-on-surface text; rich tooltips use surface-container
 * with a title, body and optional action.
 */
public final class M3Tooltip {

    private M3Tooltip() {}

    /**
     * Creates a plain tooltip.
     *
     * @param text tooltip text
     * @return the tooltip
     */
    public static Tooltip plain(String text) {
        Tooltip tooltip = new Tooltip(text == null ? "" : text);
        tooltip.getStyleClass().add("m3-tooltip");
        tooltip.setShowDelay(Duration.millis(500));
        tooltip.setShowDuration(Duration.seconds(5));
        return tooltip;
    }

    /**
     * Creates a rich tooltip with title, body and an optional action node.
     *
     * @param title title text
     * @param body body text
     * @param action optional action node, may be {@code null}
     * @return the tooltip
     */
    public static Tooltip rich(String title, String body, Node action) {
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(4);
        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label(title == null ? "" : title);
        titleLabel.getStyleClass().add("m3-tooltip-rich-title");
        javafx.scene.control.Label bodyLabel = new javafx.scene.control.Label(body == null ? "" : body);
        bodyLabel.getStyleClass().add("m3-tooltip-rich-body");
        bodyLabel.setWrapText(true);
        box.getChildren().addAll(titleLabel, bodyLabel);
        if (action != null) {
            box.getChildren().add(action);
        }
        Tooltip tooltip = new Tooltip();
        tooltip.setGraphic(box);
        tooltip.getStyleClass().add("m3-tooltip-rich");
        return tooltip;
    }

    /**
     * Installs a plain tooltip on a node.
     *
     * @param node the owner node
     * @param text tooltip text
     */
    public static void install(Node node, String text) {
        Tooltip.install(node, plain(text));
    }
}
