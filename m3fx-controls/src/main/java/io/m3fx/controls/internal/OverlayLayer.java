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
 * Sub-layers ensure that transient toasts/snackbars do not dismiss open modal dialogs or sheets.
 */
public final class OverlayLayer extends StackPane {

    private final Pane scrim = new Pane();
    private final StackPane sheetLayer = new StackPane();
    private final StackPane modalLayer = new StackPane();
    private final Pane anchoredLayer = new Pane();
    private final StackPane transientLayer = new StackPane();
    private Node anchoredPanel;
    private Runnable anchoredDismiss;

    /** Creates an empty, mouse-transparent-when-idle overlay layer. */
    public OverlayLayer() {
        getStyleClass().add("m3-overlay");
        scrim.getStyleClass().add("m3-scrim");
        scrim.setVisible(false);
        scrim.setOpacity(StateOpacity.SCRIM);

        sheetLayer.setAlignment(Pos.BOTTOM_CENTER);
        sheetLayer.setPickOnBounds(false);

        modalLayer.setAlignment(Pos.CENTER);
        modalLayer.setPickOnBounds(false);

        anchoredLayer.setPickOnBounds(false);

        transientLayer.setAlignment(Pos.BOTTOM_CENTER);
        transientLayer.setPickOnBounds(false);

        getChildren().addAll(scrim, sheetLayer, modalLayer, anchoredLayer, transientLayer);
        setPickOnBounds(false);
    }

    /**
     * Shows a modal node above a scrim. Clicking the scrim dismisses it.
     *
     * @param node the dialog/sheet node
     */
    public void showModal(Node node) {
        modalLayer.getChildren().setAll(node);
        scrim.setVisible(true);
        scrim.setOnMouseClicked(e -> hideModal());
        MotionUtil.slideIn(node, 24, MotionTokens.DURATION_MEDIUM_2);
        node.requestFocus();
    }

    /** Hides the active modal dialog. */
    public void hideModal() {
        modalLayer.getChildren().clear();
        updateScrim();
    }

    /**
     * Shows a non-modal node (snackbar) in the isolated transient layer without a scrim.
     *
     * @param node the snackbar node
     */
    public void showTransient(Node node) {
        transientLayer.getChildren().setAll(node);
        StackPane.setAlignment(node, Pos.BOTTOM_CENTER);
        MotionUtil.slideIn(node, 16, MotionTokens.DURATION_SHORT_4);
    }

    /** Hides any active transient snackbar/toast without disturbing modal dialogs or sheets. */
    public void hideTransient() {
        transientLayer.getChildren().clear();
    }

    /**
     * Shows a bottom or side sheet above a scrim.
     *
     * @param node the sheet node
     */
    public void showSheet(Node node) {
        sheetLayer.getChildren().setAll(node);
        StackPane.setAlignment(node, Pos.BOTTOM_CENTER);
        scrim.setVisible(true);
        scrim.setOnMouseClicked(e -> hideSheet());
        if (node instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        MotionUtil.slideIn(node, 48, MotionTokens.DURATION_MEDIUM_2);
        node.requestFocus();
    }

    /** Hides the active bottom/side sheet. */
    public void hideSheet() {
        sheetLayer.getChildren().clear();
        updateScrim();
    }

    /** Hides all layers (modals, sheets, transient toasts, anchored popups). */
    public void hide() {
        hideAnchored();
        hideModal();
        hideSheet();
        hideTransient();
        updateScrim();
    }

    private void updateScrim() {
        boolean hasModal = !modalLayer.getChildren().isEmpty()
                || !sheetLayer.getChildren().isEmpty()
                || anchoredPanel != null;
        scrim.setVisible(hasModal);
        if (hasModal) {
            // The scrim dismisses the topmost layer. Stale handlers from a hidden modal must not
            // survive (they would make scrim clicks no-ops while a sheet is still open).
            scrim.setOnMouseClicked(e -> {
                if (!modalLayer.getChildren().isEmpty()) {
                    hideModal();
                } else if (!sheetLayer.getChildren().isEmpty()) {
                    hideSheet();
                } else {
                    hideAnchored();
                }
            });
        } else {
            scrim.setOnMouseClicked(null);
        }
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
        hideAnchored();
        scrim.setVisible(true);
        anchoredDismiss = onDismiss;
        // hideAnchored() runs the dismiss callback exactly once, no matter who dismisses.
        scrim.setOnMouseClicked(e -> hideAnchored());
        anchoredPanel = panel;
        anchoredLayer.getChildren().setAll(panel);
        javafx.geometry.Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        if (bounds != null) {
            javafx.geometry.Point2D origin =
                    anchoredLayer.screenToLocal(bounds.getMinX(), bounds.getMinY());
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
            anchoredLayer.getChildren().remove(anchoredPanel);
            anchoredPanel = null;
            Runnable dismissed = anchoredDismiss;
            anchoredDismiss = null;
            updateScrim();
            if (dismissed != null) {
                dismissed.run();
            }
        }
    }

    /**
     * Returns {@code true} when something is shown.
     *
     * @return whether content is present in any layer
     */
    public boolean isShowing() {
        return !modalLayer.getChildren().isEmpty()
                || !sheetLayer.getChildren().isEmpty()
                || !transientLayer.getChildren().isEmpty()
                || anchoredPanel != null;
    }
}
