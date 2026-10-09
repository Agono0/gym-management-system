package io.m3fx.controls.sheet;

import io.m3fx.controls.internal.OverlayLayer;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/**
 * M3 modal bottom sheet: supplemental content sliding from the bottom above a scrim.
 *
 * <p>See <a href="https://m3.material.io/components/bottom-sheets/overview">M3 bottom sheets</a>.
 * Surface container low, extra-large top corners (28dp), drag handle, elevation level 1. Shown via
 * {@link #showIn(OverlayLayer)}.
 */
public class M3BottomSheet extends VBox {

    private final VBox contentBox = new VBox();
    private OverlayLayer host;

    /** Creates an empty bottom sheet. */
    public M3BottomSheet() {
        getStyleClass().add("m3-bottom-sheet");
        javafx.scene.layout.Region handle = new javafx.scene.layout.Region();
        handle.getStyleClass().add("m3-bottom-sheet-handle");
        handle.setMinSize(32, 4);
        handle.setMaxSize(32, 4);
        contentBox.getStyleClass().add("m3-bottom-sheet-content");
        setPadding(new Insets(8, 16, 24, 16));
        setSpacing(12);
        getChildren().addAll(handle, contentBox);
        setMinWidth(320);
        // Wrap content height; the overlay docks the sheet to the bottom.
        setMaxHeight(USE_PREF_SIZE);
    }

    /**
     * Sets the sheet content.
     *
     * @param content content node
     */
    public void setContent(Node content) {
        contentBox.getChildren().setAll(content);
    }

    /**
     * Shows the sheet docked to the bottom of an overlay layer.
     *
     * @param layer the overlay layer
     */
    public void showIn(OverlayLayer layer) {
        this.host = layer;
        layer.showSheet(this);
    }

    /** Hides the sheet. */
    public void hide() {
        if (host != null) {
            host.hide();
            host = null;
        }
    }
}
