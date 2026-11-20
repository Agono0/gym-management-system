package io.m3fx.controls.overlay;

import io.m3fx.controls.internal.MotionUtil;
import io.m3fx.core.token.MotionTokens;

import io.m3fx.controls.overlay.OverlayHost;
import io.m3fx.core.token.StateOpacity;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared overlay layer (StackPane-based) for dialog, snackbar, bottom/side sheets and menus.
 *
 * <p>Place one {@link OverlayLayer} at the root of a scene; dialogs and snackbars show themselves
 * in it with a scrim, instead of opening extra stages. Must be constructed on the FX thread.
 * Sub-layers ensure that transient toasts/snackbars do not dismiss open modal dialogs or sheets.
 *
 * <p>Anchored panels ({@link #showAnchored}) track their anchor: window moves and resizes and
 * anchor layout changes recompute the panel translation while the panel is shown, so the panel
 * never stays detached from its anchor.
 *
 * <p>Implements the exported {@link OverlayHost} interface, which is how public API signatures
 * ({@code M3Overlay.getLayer()}, {@code M3Dialog.showIn}, ...) accept this layer without exposing
 * this internal package.
 */
public final class OverlayLayer extends StackPane implements OverlayHost {

    private final Pane scrim = new Pane();
    private final StackPane sheetLayer = new StackPane();
    private final StackPane modalLayer = new StackPane();
    private final Pane anchoredLayer = new Pane();
    private final StackPane transientLayer = new StackPane();
    private Node anchoredPanel;
    private Runnable anchoredDismiss;
    private Node anchoredAnchor;
    /** Node-level watchers (anchor bounds + scene) for the anchored panel. */
    private final List<Observable> anchoredNodeWatchers = new ArrayList<>();
    /** Window-level watchers (window position/size) for the anchored panel's window. */
    private final List<Observable> anchoredWindowWatchers = new ArrayList<>();
    /** Full-recompute invalidations (layout/resize); window moves use the cheaper delta. */
    private final InvalidationListener anchoredRepositionListener = obs -> repositionAnchoredPanel();
    private final InvalidationListener anchoredWindowMoveListener = obs -> applyWindowMoveDelta();
    /** Panel translation captured at the last recompute, plus the window origin then. */
    private double anchoredBaseTranslateX;
    private double anchoredBaseTranslateY;
    private double anchoredWindowX;
    private double anchoredWindowY;

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
     * anchored menus). The panel is stretched to the anchor width and follows the anchor while
     * shown: window moves/resizes and anchor layout changes recompute the translation.
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
        anchoredAnchor = anchor;
        anchoredLayer.getChildren().setAll(panel);
        positionAnchoredPanel(anchor, panel);
        watchAnchor(anchor);
        panel.requestFocus();
    }

    /** Hides an anchored panel shown with {@link #showAnchored}. */
    public void hideAnchored() {
        unwatchAnchor();
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

    /** Returns whether the anchored-panel reposition watchers are currently attached. */
    boolean anchoredPanelIsWatched() {
        return !anchoredNodeWatchers.isEmpty() || !anchoredWindowWatchers.isEmpty();
    }

    /** Number of nodes currently in the transient layer (test seam). */
    public int transientChildrenCountForTesting() {
        return transientLayer.getChildren().size();
    }

    /**
     * Recomputes the anchored panel translation from its anchor's current screen bounds and
     * rebases the window-move delta on the freshly computed position.
     */
    private void positionAnchoredPanel(Node anchor, Node panel) {
        Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        if (bounds == null) {
            return;
        }
        Point2D origin = anchoredLayer.screenToLocal(bounds.getMinX(), bounds.getMinY());
        if (origin == null) {
            return;
        }
        panel.setTranslateX(origin.getX());
        panel.setTranslateY(origin.getY() + bounds.getHeight() + 4);
        if (panel instanceof Region region) {
            region.setMinWidth(bounds.getWidth());
            region.setMaxWidth(bounds.getWidth());
        }
        anchoredBaseTranslateX = panel.getTranslateX();
        anchoredBaseTranslateY = panel.getTranslateY();
        Scene scene = anchor.getScene();
        if (scene != null && scene.getWindow() != null) {
            anchoredWindowX = scene.getWindow().getX();
            anchoredWindowY = scene.getWindow().getY();
        }
    }

    /**
     * Applies a window move exactly: the panel translation shifts by the window delta. Reading
     * the anchor through {@code localToScreen} mid-pulse can observe stale peer coordinates, but
     * the window x/y properties are always current, so a pure delta never lags.
     */
    private void applyWindowMoveDelta() {
        if (anchoredPanel == null || anchoredAnchor == null) {
            return;
        }
        Scene scene = anchoredAnchor.getScene();
        if (scene == null || scene.getWindow() == null) {
            return;
        }
        double dx = scene.getWindow().getX() - anchoredWindowX;
        double dy = scene.getWindow().getY() - anchoredWindowY;
        if (dx != 0 || dy != 0) {
            anchoredPanel.setTranslateX(anchoredBaseTranslateX + dx);
            anchoredPanel.setTranslateY(anchoredBaseTranslateY + dy);
        }
    }

    /** Attaches the reposition watchers (anchor bounds, scene and window geometry). */
    private void watchAnchor(Node anchor) {
        unwatchAnchor();
        anchoredAnchor = anchor;
        watch(anchor.layoutBoundsProperty(), anchoredNodeWatchers, anchoredRepositionListener);
        watch(anchor.boundsInParentProperty(), anchoredNodeWatchers, anchoredRepositionListener);
        anchor.sceneProperty().addListener(this::anchoredSceneChanged);
        anchoredNodeWatchers.add(anchor.sceneProperty());
        anchoredSceneChanged(null, null, anchor.getScene());
    }

    /** Detaches every anchored-panel watcher. */
    private void unwatchAnchor() {
        for (Observable observable : anchoredNodeWatchers) {
            observable.removeListener(anchoredRepositionListener);
        }
        for (Observable observable : anchoredWindowWatchers) {
            observable.removeListener(anchoredRepositionListener);
            observable.removeListener(anchoredWindowMoveListener);
        }
        anchoredNodeWatchers.clear();
        anchoredWindowWatchers.clear();
        if (anchoredAnchor != null) {
            anchoredAnchor.sceneProperty().removeListener(this::anchoredSceneChanged);
            anchoredAnchor = null;
        }
    }

    private void watch(Observable observable, List<Observable> into, InvalidationListener listener) {
        observable.addListener(listener);
        into.add(observable);
    }

    /**
     * Re-attaches the window-level watchers when the anchor's scene (or its window) changes:
     * window position/size drive the panel translation. {@code null} scenes or windows simply
     * leave no window watchers until they exist.
     */
    private void anchoredSceneChanged(Observable observable, Scene oldScene, Scene newScene) {
        for (Observable watcher : anchoredWindowWatchers) {
            watcher.removeListener(anchoredRepositionListener);
        }
        anchoredWindowWatchers.clear();
        Scene scene = newScene != null ? newScene : anchoredAnchor.getScene();
        if (scene == null) {
            repositionAnchoredPanel();
            return;
        }
        watch(scene.windowProperty(), anchoredWindowWatchers, anchoredRepositionListener);
        if (scene.getWindow() instanceof Window window) {
            watch(window.xProperty(), anchoredWindowWatchers, anchoredWindowMoveListener);
            watch(window.yProperty(), anchoredWindowWatchers, anchoredWindowMoveListener);
            watch(window.widthProperty(), anchoredWindowWatchers, anchoredRepositionListener);
            watch(window.heightProperty(), anchoredWindowWatchers, anchoredRepositionListener);
        }
        repositionAnchoredPanel();
    }

    /** Repositions the shown anchored panel, if any. */
    private void repositionAnchoredPanel() {
        if (anchoredPanel != null && anchoredAnchor != null) {
            positionAnchoredPanel(anchoredAnchor, anchoredPanel);
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
