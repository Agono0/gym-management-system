package io.m3fx.controls.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.sheet.M3BottomSheet;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/**
 * Verifies the {@link M3Overlay} facade end to end: each show* call lands in its isolated layer
 * (modal, sheet, transient), dismiss/isShowing semantics hold across combinations, and ESC closes
 * dialogs and snackbars through their own handlers without disturbing the other layers. The
 * anchoring math of the exposed {@link OverlayHost} is covered in {@link OverlayLayerTest}.
 */
class M3OverlayFacadeTest {

    @BeforeAll
    static void initFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX Platform failed to initialize");
    }

    private static void runOnFx(Runnable action) throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] error = new Throwable[1];
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS), "FX task timed out");
        if (error[0] != null) {
            throw error[0];
        }
    }

    /** Polls an FX-thread condition until it holds or the timeout elapses. */
    private static boolean awaitFxCondition(BooleanSupplier condition, long timeoutMs)
            throws Throwable {
        long deadline = System.currentTimeMillis() + timeoutMs;
        AtomicBoolean satisfied = new AtomicBoolean(false);
        while (System.currentTimeMillis() < deadline) {
            runOnFx(() -> satisfied.set(condition.getAsBoolean()));
            if (satisfied.get()) {
                return true;
            }
            Thread.sleep(25);
        }
        return false;
    }

    private static OverlayLayer layerOf(M3Overlay overlay) {
        return (OverlayLayer) overlay.getLayer();
    }

    private static M3Overlay overlayInScene() {
        M3Overlay overlay = new M3Overlay();
        StackPane root = new StackPane(overlay, new Group());
        new Scene(root, 480, 320);
        return overlay;
    }

    private static KeyEvent escape() {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false,
                false);
    }

    @Test
    void showDialogPresentsTheModalAndEscClosesIt() throws Throwable {
        AtomicReference<M3Overlay> overlayRef = new AtomicReference<>();
        runOnFx(() -> {
            M3Overlay overlay = overlayInScene();
            overlayRef.set(overlay);
            M3Dialog dialog = new M3Dialog();
            dialog.setTitle("Confirm");
            overlay.showDialog(dialog);
            assertTrue(overlay.isShowing(), "modal shown");
            assertEquals(1, layerOf(overlay).modalCountForTesting(), "the dialog sits in the modal layer");
            assertTrue(dialog.getScene() != null, "the dialog is adopted into the layer's scene");
            dialog.fireEvent(escape());
        });
        // hide() fades the dialog out asynchronously; poll until the modal layer clears.
        assertTrue(awaitFxCondition(
                        () -> layerOf(overlayRef.get()).modalCountForTesting() == 0, 3000),
                "ESC must close the dialog through its own handler");
        runOnFx(() -> assertFalse(overlayRef.get().isShowing(), "nothing else was showing"));
    }

    @Test
    void escClosesTheDialogWithoutTouchingTheSnackbarLayer() throws Throwable {
        AtomicReference<M3Overlay> overlayRef = new AtomicReference<>();
        runOnFx(() -> {
            M3Overlay overlay = overlayInScene();
            overlayRef.set(overlay);
            M3Dialog dialog = new M3Dialog();
            dialog.setTitle("Modal");
            overlay.showDialog(dialog);
            overlay.showSnackbar("Saved");
            assertEquals(1, layerOf(overlay).modalCountForTesting(), "dialog in the modal layer");
            assertEquals(1, layerOf(overlay).transientChildrenCountForTesting(),
                    "snackbar in the transient layer");

            dialog.fireEvent(escape());
        });
        assertTrue(awaitFxCondition(
                        () -> layerOf(overlayRef.get()).modalCountForTesting() == 0, 3000),
                "ESC dismisses the dialog");
        runOnFx(() -> assertEquals(1, layerOf(overlayRef.get()).transientChildrenCountForTesting(),
                        "the snackbar layer must survive the dialog's ESC (partitioning)"));
    }

    @Test
    void snackbarEscClosesOnlyTheTransientLayer() throws Throwable {
        AtomicReference<M3Overlay> overlayRef = new AtomicReference<>();
        runOnFx(() -> {
            M3Overlay overlay = overlayInScene();
            overlayRef.set(overlay);
            M3Dialog dialog = new M3Dialog();
            dialog.setTitle("Modal");
            overlay.showDialog(dialog);
            overlay.showSnackbar("Saved");
            assertEquals(1, layerOf(overlay).transientChildrenCountForTesting());

            // ESC is delivered to the snackbar bar itself (its own key handler), not the dialog.
            VBox wrapper = (VBox) layerOf(overlay).transientNodeForTesting();
            javafx.scene.Node bar = wrapper.getChildren().get(0);
            bar.fireEvent(escape());
        });
        assertTrue(awaitFxCondition(
                        () -> layerOf(overlayRef.get()).transientChildrenCountForTesting() == 0, 3000),
                "ESC dismisses the snackbar");
        runOnFx(() -> assertEquals(1, layerOf(overlayRef.get()).modalCountForTesting(),
                "the modal dialog stays open (partitioning)"));
    }

    @Test
    void showSheetPresentsTheBottomSheetAboveTheScrim() throws Throwable {
        runOnFx(() -> {
            M3Overlay overlay = overlayInScene();
            M3BottomSheet sheet = new M3BottomSheet();
            sheet.setContent(new Rectangle(200, 80));
            overlay.showSheet(sheet);
            assertTrue(overlay.isShowing(), "sheet shown");
            assertEquals(1, layerOf(overlay).sheetCountForTesting(), "the sheet sits in the sheet layer");
            overlay.dismiss();
            assertEquals(0, layerOf(overlay).sheetCountForTesting(), "dismiss clears the sheet");
            assertFalse(overlay.isShowing());
        });
    }

    @Test
    void showModalArbitraryNodeAndDismiss() throws Throwable {
        runOnFx(() -> {
            M3Overlay overlay = overlayInScene();
            Rectangle node = new Rectangle(120, 80);
            overlay.showModal(node);
            assertTrue(overlay.isShowing());
            assertEquals(1, layerOf(overlay).modalCountForTesting());
            overlay.dismiss();
            assertEquals(0, layerOf(overlay).modalCountForTesting());
            assertFalse(overlay.isShowing());
        });
    }

    @Test
    void getLayerExposesTheHostForAnchoredPanels() throws Throwable {
        runOnFx(() -> {
            M3Overlay overlay = overlayInScene();
            OverlayHost host = overlay.getLayer();
            assertTrue(host instanceof OverlayLayer, "the host is the internal layer");
            Rectangle anchor = new Rectangle(100, 24);
            StackPane root = (StackPane) overlay.getParent();
            root.getChildren().add(anchor);
            Region panel = new Region();
            panel.setPrefSize(100, 16);
            host.showAnchored(anchor, panel, null);
            assertTrue(overlay.isShowing(), "anchored panel counts as showing");
            host.hideAnchored();
            assertFalse(overlay.isShowing());
        });
    }

    @Test
    void dialogAndSnackbarAdoptTheSameLayerInstance() throws Throwable {
        runOnFx(() -> {
            M3Overlay overlay = overlayInScene();
            assertSame(overlay.getLayer(), overlay.getLayer(), "stable host instance");
            overlay.showSnackbar("Hello");
            // The facade owns one snackbar instance for the lifetime of the overlay.
            assertTrue(layerOf(overlay).transientChildrenCountForTesting() == 1);
        });
    }
}
