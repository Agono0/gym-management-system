package io.m3fx.controls.dialog;

import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.internal.OverlayLayer;
import io.m3fx.core.token.MotionTokens;
import javafx.animation.FadeTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * M3 basic dialog (plus a fullscreen mode) shown in an {@link OverlayLayer}, never a new stage.
 *
 * <p>See <a href="https://m3.material.io/components/dialogs/overview">M3 dialogs</a>. Surface
 * container high, extra-large shape (28dp), headline-small title, icon, content and text-button
 * actions. Call {@link #showIn(OverlayLayer)} and {@link #hide()}. Escape closes the dialog and
 * focus returns to the node that owned it before the dialog opened.
 */
public class M3Dialog extends VBox {

    private final Label iconLabel = new Label();
    private final Label titleLabel = new Label();
    private final VBox contentBox = new VBox();
    private final HBox actionsBox = new HBox(8);

    private OverlayLayer host;
    private Node previousFocus;
    private FadeTransition exitTransition;

    /** Creates an empty dialog. */
    public M3Dialog() {
        getStyleClass().add("m3-dialog");
        iconLabel.getStyleClass().add("m3-dialog-icon");
        iconLabel.setVisible(false);
        iconLabel.setManaged(false);
        titleLabel.getStyleClass().add("m3-dialog-title");
        contentBox.getStyleClass().add("m3-dialog-content");
        actionsBox.getStyleClass().add("m3-dialog-actions");
        setPadding(new Insets(24));
        setSpacing(16);
        // Spec: container min 280dp / max 560dp (m3.material.io/components/dialogs/spec).
        setMinWidth(280);
        setMaxWidth(560);
        // Wrap content: without this a VBox fills the whole overlay height.
        setMaxHeight(USE_PREF_SIZE);
        getChildren().addAll(iconLabel, titleLabel, contentBox, actionsBox);
        actionsBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE && host != null) {
                e.consume();
                hide();
            }
        });
    }

    /**
     * Creates a dialog with title, content and actions.
     *
     * @param title dialog title
     * @param content content node
     * @param actions action buttons (usually {@code M3Button.text(...)})
     * @return the dialog
     */
    public static M3Dialog basic(String title, Node content, M3Button... actions) {
        M3Dialog dialog = new M3Dialog();
        dialog.setTitle(title);
        if (content != null) {
            dialog.setContent(content);
        }
        if (actions != null) {
            dialog.getActionsBox().getChildren().addAll(actions);
        }
        return dialog;
    }

    /**
     * Sets the title.
     *
     * @param title title text
     */
    public void setTitle(String title) {
        titleLabel.setText(title == null ? "" : title);
        setAccessibleText(title);
    }

    /**
     * Sets an icon shown above the title.
     *
     * @param icon icon node
     */
    public void setIcon(Node icon) {
        iconLabel.setGraphic(icon);
        iconLabel.setVisible(icon != null);
        iconLabel.setManaged(icon != null);
    }

    /** Content node property (FXML-settable). */
    private final ObjectProperty<Node> content = new SimpleObjectProperty<>(this, "content");

    /** Returns the content property. */
    public ObjectProperty<Node> contentProperty() {
        return content;
    }

    /** Returns the dialog content, may be {@code null}. */
    public Node getContent() {
        return content.get();
    }

    /**
     * Sets the dialog content.
     *
     * @param content content node
     */
    public void setContent(Node content) {
        this.content.set(content);
        contentBox.getChildren().setAll(content);
    }

    /** Returns the actions row for adding buttons. */
    public HBox getActionsBox() {
        return actionsBox;
    }

    /**
     * Makes the dialog fill its overlay (fullscreen variant).
     *
     * @param fullscreen {@code true} for the fullscreen variant
     */
    public void setFullscreen(boolean fullscreen) {
        pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("fullscreen"), fullscreen);
        if (fullscreen) {
            setMaxWidth(Double.MAX_VALUE);
            setMaxHeight(Double.MAX_VALUE);
        } else {
            setMaxWidth(560);
            setMaxHeight(USE_PREF_SIZE);
        }
    }

    /**
     * Shows this dialog in an overlay layer with a scrim. Focus moves into the dialog and returns
     * to the previous owner when it hides.
     *
     * @param layer the overlay layer
     */
    public void showIn(OverlayLayer layer) {
        if (exitTransition != null) {
            exitTransition.stop();
            exitTransition = null;
        }
        setOpacity(1);
        Scene scene = layer.getScene();
        previousFocus = scene != null ? scene.getFocusOwner() : null;
        this.host = layer;
        layer.showModal(this);
    }

    /** Hides this dialog with a short fade-out, then restores the previous focus owner. */
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
        exitTransition = new FadeTransition(Duration.millis(MotionTokens.DURATION_SHORT_4), this);
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
