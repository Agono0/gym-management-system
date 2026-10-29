package io.m3fx.controls.sheet;

import io.m3fx.controls.internal.OverlayLayer;
import io.m3fx.core.token.MotionTokens;
import javafx.animation.FadeTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * M3 side sheet: supplemental content in a side panel, standard (persistent) or modal.
 *
 * <p>See <a href="https://m3.material.io/components/side-sheets/overview">M3 side sheets</a>.
 * Standard: surface container, elevation level 0, docked width 256dp. Modal: surface container
 * low, elevation level 1, shown over a scrim. {@link #showIn(OverlayLayer)} presents the modal
 * variant; Escape or a scrim click hides it and focus returns to its previous owner.
 */
public class M3SideSheet extends VBox {

    private final BooleanProperty modal = new SimpleBooleanProperty(this, "modal", false);
    private final VBox contentBox = new VBox(12);

    private OverlayLayer host;
    private Node previousFocus;
    private FadeTransition exitTransition;

    /** Creates a standard side sheet. */
    public M3SideSheet() {
        getStyleClass().add("m3-side-sheet");
        contentBox.getStyleClass().add("m3-side-sheet-content");
        setPadding(new Insets(16));
        setSpacing(12);
        // Spec: docked container width 256dp (md.comp.sheet.side.docked.container.width).
        setMinWidth(256);
        setPrefWidth(320);
        getChildren().add(contentBox);
        modalProperty().addListener((obs, oldV, isModal) ->
                pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("modal"), isModal));
        addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE && host != null) {
                e.consume();
                hide();
            }
        });
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

    /**
     * Shows the modal side sheet docked to the right of an overlay layer.
     *
     * @param layer the overlay layer
     */
    public void showIn(io.m3fx.controls.internal.OverlayLayer layer) {
        if (exitTransition != null) {
            exitTransition.stop();
            exitTransition = null;
        }
        setOpacity(1);
        Scene scene = layer.getScene();
        previousFocus = scene != null ? scene.getFocusOwner() : null;
        this.host = layer;
        setModal(true);
        layer.showModal(this);
        javafx.scene.layout.StackPane.setAlignment(this, javafx.geometry.Pos.CENTER_RIGHT);
    }

    /** Hides the sheet with a short fade-out, then restores the previous focus owner. */
    public void hide() {
        if (host == null) {
            return;
        }
        OverlayLayer layer = host;
        host = null;
        if (getScene() == null) {
            layer.hideModal();
            restoreFocus();
            return;
        }
        exitTransition =
                new FadeTransition(Duration.millis(MotionTokens.DURATION_SHORT_4), this);
        exitTransition.setToValue(0);
        exitTransition.setInterpolator(MotionTokens.emphasizedAccelerate());
        exitTransition.setOnFinished(e -> {
            exitTransition = null;
            layer.hideModal();
            setOpacity(1);
            restoreFocus();
        });
        exitTransition.play();
    }

    private void restoreFocus() {
        Node owner = previousFocus;
        previousFocus = null;
        if (owner != null && owner.getScene() != null) {
            owner.requestFocus();
        }
    }
}
