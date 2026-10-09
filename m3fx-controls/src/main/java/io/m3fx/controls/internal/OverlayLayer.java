package io.m3fx.controls.internal;

import io.m3fx.core.token.MotionTokens;
import io.m3fx.core.token.StateOpacity;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Shared overlay layer (StackPane-based) for dialog, snackbar, bottom/side sheets and menus.
 *
 * <p>Place one {@link OverlayLayer} at the root of a scene; dialogs and snackbars show themselves
 * in it with a scrim, instead of opening extra stages. Must be constructed on the FX thread.
 */
public final class OverlayLayer extends StackPane {

    private final Pane scrim = new Pane();
    private final StackPane content = new StackPane();
    private Node anchoredPanel;

    /** Creates an empty, mouse-transparent-when-idle overlay layer. */
    public OverlayLayer() {
        getStyleClass().add("m3-overlay");
        scrim.getStyleClass().add("m3-scrim");
        scrim.setVisible(false);
        scrim.setOpacity(StateOpacity.SCRIM);
        content.setAlignment(Pos.CENTER);
        content.setPickOnBounds(false);
        getChildren().addAll(scrim, content);
        setPickOnBounds(false);
    }

    /**
     * Shows a modal node above a scrim. Clicking the scrim dismisses it.
     *
     * @param node the dialog/sheet node
     */
    public void showModal(Node node) {
        content.getChildren().setAll(node);
        scrim.setVisible(true);
        scrim.setOnMouseClicked(e -> hide());
        MotionUtil.slideIn(node, 24, MotionTokens.DURATION_MEDIUM_2);
        node.requestFocus();
    }

    /**
     * Shows a non-modal node (snackbar) without a scrim.
     *
     * @param node the snackbar node
     */
    public void showTransient(Node node) {
        content.getChildren().setAll(node);
        StackPane.setAlignment(node, Pos.BOTTOM_CENTER);
        MotionUtil.slideIn(node, 16, MotionTokens.DURATION_SHORT_4);
    }

    /**
     * Shows a bottom sheet docked to the bottom above a scrim.
     *
     * @param node the sheet node
     */
    public void showSheet(Node node) {
        hide();
        content.getChildren().setAll(node);
        StackPane.setAlignment(node, Pos.BOTTOM_CENTER);
        scrim.setVisible(true);
        scrim.setOnMouseClicked(e -> hide());
        if (node instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        MotionUtil.slideIn(node, 48, MotionTokens.DURATION_MEDIUM_2);
        node.requestFocus();
    }

    /** Hides whatever is currently shown. */
    public void hide() {
        hideAnchored();
        content.getChildren().clear();
        scrim.setVisible(false);
        scrim.setOnMouseClicked(null);
    }

    /**
     * Shows a panel anchored under an anchor node with a dismissing scrim (search suggestions,
     * anchored menus). The panel is stretched to the anchor width.
     *
     * @param anchor the anchor node (must be in a live scene)
     * @param panel the panel to show
     * @param onDismiss optional dismissal callback, may be {@code null}
     */
    public void showAnchored(Node anchor, Node panel, Runnable onDismiss) {
        hide();
        scrim.setVisible(true);
        scrim.setOnMouseClicked(e -> {
            hideAnchored();
            if (onDismiss != null) {
                onDismiss.run();
            }
        });
        anchoredPanel = panel;
        content.getChildren().setAll(panel);
        StackPane.setAlignment(panel, Pos.TOP_LEFT);
        javafx.geometry.Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        if (bounds != null) {
            javafx.geometry.Point2D origin =
                    content.screenToLocal(bounds.getMinX(), bounds.getMinY());
            if (origin != null) {
                panel.setTranslateX(origin.getX());
                panel.setTranslateY(origin.getY() + bounds.getHeight() + 4);
            }
            if (panel instanceof Region region) {
                region.setMinWidth(bounds.getWidth());
                region.setMaxWidth(bounds.getWidth());
            }
        }
        panel.requestFocus();
    }

    /** Hides an anchored panel shown with {@link #showAnchored}. */
    public void hideAnchored() {
        if (anchoredPanel != null) {
            content.getChildren().remove(anchoredPanel);
            anchoredPanel = null;
        }
    }

    /**
     * Returns {@code true} when something is shown.
     *
     * @return whether content is present
     */
    public boolean isShowing() {
        return !content.getChildren().isEmpty();
    }
}
