package io.m3fx.controls.overlay;

import javafx.scene.Node;
import javafx.scene.Scene;

/**
 * Public host surface for overlay content: dialogs, sheets, snackbars and anchored panels shown
 * in-scene (with a scrim) instead of in extra stages.
 *
 * <p>Implemented by the library's internal overlay layer; {@link M3Overlay#getLayer()} returns
 * this interface and the {@code showIn(...)} style APIs accept it, so callers on the module path
 * never need the non-exported {@code io.m3fx.controls.internal} package. Library internals keep
 * passing the concrete layer.
 *
 * <p>All methods must be called on the JavaFX application thread.
 */
public interface OverlayHost {

    /**
     * Shows a modal node above a scrim. Clicking the scrim dismisses it.
     *
     * @param node the dialog or sheet node
     */
    void showModal(Node node);

    /** Hides the active modal node. */
    void hideModal();

    /**
     * Shows a non-modal node (snackbar) in an isolated transient layer, without a scrim.
     *
     * @param node the snackbar or toast node
     */
    void showTransient(Node node);

    /** Hides any active transient node without disturbing modal dialogs or sheets. */
    void hideTransient();

    /**
     * Shows a panel anchored under an anchor node with a dismissing scrim. The panel is stretched
     * to the anchor width.
     *
     * @param anchor the anchor node (must be in a live scene)
     * @param panel the panel to show
     * @param onDismiss optional dismissal callback, may be {@code null}
     */
    void showAnchored(Node anchor, Node panel, Runnable onDismiss);

    /** Hides an anchored panel shown with {@link #showAnchored}. */
    void hideAnchored();

    /** Hides everything (modal, sheet, transient and anchored content). */
    void hide();

    /**
     * Returns whether content is currently shown in any layer.
     *
     * @return {@code true} when something is shown
     */
    boolean isShowing();

    /**
     * Returns the JavaFX scene this host is attached to, used by dialogs to restore the focus
     * owner they displaced.
     *
     * @return the scene, or {@code null} when the host is not attached to one
     */
    Scene getScene();
}
