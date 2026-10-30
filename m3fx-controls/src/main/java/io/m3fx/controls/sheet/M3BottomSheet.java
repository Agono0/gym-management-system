package io.m3fx.controls.sheet;

import io.m3fx.controls.internal.OverlayLayer;
import io.m3fx.core.token.MotionTokens;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * M3 modal bottom sheet: supplemental content sliding from the bottom above a scrim.
 *
 * <p>See <a href="https://m3.material.io/components/bottom-sheets/overview">M3 bottom sheets</a>.
 * Surface container low, extra-large top corners (28dp), drag handle (32x4dp), elevation level 1.
 * Shown via {@link #showIn(OverlayLayer)}; Escape, a scrim click or dragging the handle down past
 * the dismiss threshold hides the sheet.
 */
public class M3BottomSheet extends VBox {

    /** Dismiss drag distance beyond which the sheet closes (spec "drag to dismiss"). */
    private static final double DISMISS_DRAG_DISTANCE = 120;
    /** Drag velocity in px/ms above which the sheet closes regardless of distance. */
    private static final double DISMISS_VELOCITY = 0.6;

    private final VBox contentBox = new VBox(12);
    private final Region handle = new Region();
    private final javafx.scene.layout.StackPane handleGrip = new javafx.scene.layout.StackPane(handle);

    private OverlayLayer host;
    private Node previousFocus;
    private ParallelTransition exitTransition;

    /** Creates an empty bottom sheet. */
    public M3BottomSheet() {
        getStyleClass().add("m3-bottom-sheet");
        handle.getStyleClass().add("m3-bottom-sheet-handle");
        // Spec: drag handle 32x4dp (md.comp.sheet.bottom.docked.drag-handle.*). The grip pads it
        // so the 4px strip is actually grabbable (touch-target friendly).
        handle.setMinSize(32, 4);
        handle.setMaxSize(32, 4);
        handleGrip.setPadding(new Insets(8, 0, 8, 0));
        handleGrip.setCursor(javafx.scene.Cursor.V_RESIZE);
        contentBox.getStyleClass().add("m3-bottom-sheet-content");
        setPadding(new Insets(8, 16, 24, 16));
        setSpacing(12);
        getChildren().addAll(handleGrip, contentBox);
        setMinWidth(320);
        // Wrap content height; the overlay docks the sheet to the bottom.
        setMaxHeight(USE_PREF_SIZE);
        installDragToDismiss();
        addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE && host != null) {
                e.consume();
                hide();
            }
        });
        // A scrim click removes this sheet from the layer without calling hide(); still return
        // focus to whoever opened the sheet.
        sceneProperty().addListener((obs, oldV, scene) -> {
            if (scene == null && host != null) {
                host = null;
                restoreFocus();
            }
        });
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
     * Shows the sheet docked to the bottom of an overlay layer.
     *
     * @param layer the overlay layer
     */
    public void showIn(OverlayLayer layer) {
        if (exitTransition != null) {
            exitTransition.stop();
            exitTransition = null;
        }
        setOpacity(1);
        setTranslateY(0);
        Scene scene = layer.getScene();
        previousFocus = scene != null ? scene.getFocusOwner() : null;
        this.host = layer;
        layer.showSheet(this);
        Node target = firstFocusable(contentBox);
        if (target != null) {
            target.requestFocus();
        }
    }

    /** Hides the sheet with a slide-down exit animation. */
    public void hide() {
        if (host == null) {
            return;
        }
        OverlayLayer layer = host;
        host = null;
        animateOut(layer, 0);
    }

    /**
     * Drags the sheet down with the handle; releasing past the dismiss distance or with enough
     * velocity closes it, otherwise it springs back (spec drag-to-dismiss).
     */
    private void installDragToDismiss() {
        final double[] startY = new double[1];
        final double[] lastY = new double[1];
        final long[] lastNanos = new long[1];
        final double[] velocity = new double[1];
        handleGrip.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (host == null) {
                return;
            }
            startY[0] = e.getScreenY();
            lastY[0] = startY[0];
            lastNanos[0] = System.nanoTime();
            velocity[0] = 0;
            e.consume();
        });
        handleGrip.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (host == null) {
                return;
            }
            double drag = Math.max(0, e.getScreenY() - startY[0]);
            long now = System.nanoTime();
            long dtMs = Math.max(1, (now - lastNanos[0]) / 1_000_000);
            velocity[0] = (e.getScreenY() - lastY[0]) / dtMs;
            lastY[0] = e.getScreenY();
            lastNanos[0] = now;
            setTranslateY(drag);
            e.consume();
        });
        handleGrip.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (host == null) {
                return;
            }
            double drag = Math.max(0, getTranslateY());
            if (drag > DISMISS_DRAG_DISTANCE || velocity[0] > DISMISS_VELOCITY) {
                OverlayLayer layer = host;
                host = null;
                animateOut(layer, 0);
            } else {
                TranslateTransition back = new TranslateTransition(
                        Duration.millis(MotionTokens.DURATION_MEDIUM_2), this);
                back.setToY(0);
                back.setInterpolator(MotionTokens.spring());
                back.play();
            }
            e.consume();
        });
    }

    /**
     * Plays the exit (slide down + fade) and then clears the overlay layer.
     *
     * @param layer the host overlay layer
     * @param fromY extra starting offset in px (0 hides from the current position)
     */
    private void animateOut(OverlayLayer layer, double fromY) {
        if (getScene() == null) {
            layer.hideSheet();
            restoreFocus();
            return;
        }
        TranslateTransition slide = new TranslateTransition(
                Duration.millis(MotionTokens.DURATION_MEDIUM_2), this);
        slide.setFromY(getTranslateY() + fromY);
        slide.setToY(getHeight() > 0 ? getHeight() : 200);
        slide.setInterpolator(MotionTokens.emphasizedAccelerate());
        FadeTransition fade =
                new FadeTransition(Duration.millis(MotionTokens.DURATION_SHORT_4), this);
        fade.setToValue(0);
        // One combined transition so showIn() can stop the whole exit cleanly.
        ParallelTransition exit = new ParallelTransition(slide, fade);
        exit.setOnFinished(e -> {
            exitTransition = null;
            layer.hideSheet();
            setOpacity(1);
            setTranslateY(0);
            restoreFocus();
        });
        exitTransition = exit;
        exit.play();
    }

    /** Returns focus to the node that owned it before the sheet opened. */
    private void restoreFocus() {
        Node owner = previousFocus;
        previousFocus = null;
        if (owner != null && owner.getScene() != null) {
            owner.requestFocus();
        }
    }

    /** Depth-first search for the first visible, managed, enabled, focus-traversable node. */
    private static Node firstFocusable(Node node) {
        if (node == null || !node.isVisible() || !node.isManaged()) {
            return null;
        }
        if (node instanceof Pane pane) {
            for (Node child : pane.getChildren()) {
                Node found = firstFocusable(child);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }
        return !node.isDisabled() && node.isFocusTraversable() ? node : null;
    }
}
