package io.m3fx.controls.sheet;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/**
 * M3 side sheet: supplemental content in a side panel, standard (persistent) or modal.
 *
 * <p>See <a href="https://m3.material.io/components/side-sheets/overview">M3 side sheets</a>.
 * Surface container low, no... actually zero elevation for standard, level 1 for modal; width
 * 256dp (docked) or 400dp.
 */
public class M3SideSheet extends VBox {

    private final BooleanProperty modal = new SimpleBooleanProperty(this, "modal", false);
    private final VBox contentBox = new VBox();

    /** Creates a standard side sheet. */
    public M3SideSheet() {
        getStyleClass().add("m3-side-sheet");
        contentBox.getStyleClass().add("m3-side-sheet-content");
        setPadding(new Insets(16));
        setSpacing(12);
        setMinWidth(256);
        setPrefWidth(320);
        getChildren().add(contentBox);
        modalProperty().addListener((obs, oldV, isModal) ->
                pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("modal"), isModal));
    }

    /** Returns the modal property. */
    public BooleanProperty modalProperty() {
        return modal;
    }

    /** Returns whether the sheet is modal. */
    public boolean isModal() {
        return modal.get();
    }

    /**
     * Sets modal mode.
     *
     * @param modal {@code true} for modal
     */
    public void setModal(boolean modal) {
        this.modal.set(modal);
    }

    /** Content node property (FXML-settable). */
    private final ObjectProperty<Node> content = new SimpleObjectProperty<>(this, "content");

    /** Returns the content property. */
    public ObjectProperty<Node> contentProperty() {
        return content;
    }

    /** Returns the sheet content, may be {@code null}. */
    public Node getContent() {
        return content.get();
    }

    /**
     * Sets the sheet content.
     *
     * @param content content node
     */
    public void setContent(Node content) {
        this.content.set(content);
        contentBox.getChildren().setAll(content);
    }
}
