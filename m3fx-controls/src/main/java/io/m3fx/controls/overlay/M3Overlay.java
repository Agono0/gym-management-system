package io.m3fx.controls.overlay;

import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.internal.OverlayLayer;
import io.m3fx.controls.sheet.M3BottomSheet;
import io.m3fx.controls.sheet.M3SideSheet;
import io.m3fx.controls.snackbar.M3Snackbar;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

/**
 * Public M3 overlay facade resolving Decision #35.
 *
 * <p>Wraps the internal {@link OverlayLayer} to expose a clean public API for showing dialogs,
 * bottom sheets, side sheets, and snackbars without stage popups.
 */
public class M3Overlay extends StackPane {

    private final OverlayLayer layer = new OverlayLayer();
    private final M3Snackbar snackbar;

    /** Creates a new public M3 overlay container. */
    public M3Overlay() {
        getChildren().add(layer);
        setPickOnBounds(false);
        this.snackbar = new M3Snackbar(layer);
    }

    /**
     * Returns the underlying {@link OverlayLayer}.
     *
     * @return the overlay layer
     */
    public OverlayLayer getLayer() {
        return layer;
    }

    /**
     * Shows an {@link M3Dialog} modally above a scrim.
     *
     * @param dialog the dialog to present
     */
    public void showDialog(M3Dialog dialog) {
        dialog.showIn(layer);
    }

    /**
     * Shows a modal bottom sheet.
     *
     * @param sheet the bottom sheet
     */
    public void showSheet(M3BottomSheet sheet) {
        sheet.showIn(layer);
    }

    /**
     * Shows a modal side sheet.
     *
     * @param sheet the side sheet
     */
    public void showSheet(M3SideSheet sheet) {
        sheet.showIn(layer);
    }

    /**
     * Displays a snackbar message.
     *
     * @param message text message
     */
    public void showSnackbar(String message) {
        snackbar.show(message);
    }

    /**
     * Displays a snackbar message with an action button.
     *
     * @param message text message
     * @param actionLabel action button text
     * @param onAction action callback
     */
    public void showSnackbar(String message, String actionLabel, Runnable onAction) {
        snackbar.show(message, actionLabel, onAction);
    }

    /**
     * Shows any arbitrary node modally above the scrim.
     *
     * @param node the node to present
     */
    public void showModal(Node node) {
        layer.showModal(node);
    }

    /** Dismisses whatever modal or sheet is currently displayed. */
    public void dismiss() {
        layer.hide();
    }

    /** Returns whether any overlay content is currently displayed. */
    public boolean isShowing() {
        return layer.isShowing();
    }
}

