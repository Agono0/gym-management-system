package io.m3fx.controls.snackbar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class M3SnackbarUxTest {

    private final AtomicReference<M3Snackbar> snackbar = new AtomicReference<>();
    private final AtomicReference<io.m3fx.controls.overlay.OverlayLayer> layer =
            new AtomicReference<>();

    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized
        }
    }

    private void runOnFx(Runnable action) throws Throwable {
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
        assertTrue(latch.await(5, TimeUnit.SECONDS), "FX task timed out");
        if (error[0] != null) {
            throw error[0];
        }
    }

    private boolean isShowing() throws Throwable {
        AtomicBooleanHolder holder = new AtomicBooleanHolder();
        runOnFx(() -> holder.value = layer.get().isShowing());
        return holder.value;
    }

    private void waitFor(boolean expected) throws Throwable {
        long deadline = System.currentTimeMillis() + 4000;
        while (System.currentTimeMillis() < deadline) {
            if (isShowing() == expected) {
                return;
            }
            Thread.sleep(50);
        }
        assertTrue(false, "Overlay layer never reached showing=" + expected);
    }

    private static final class AtomicBooleanHolder {
        boolean value;
    }

    @Test
    void queueOrderIsPreservedAcrossDismissals() throws Throwable {
        runOnFx(() -> {
            StackPane root = new StackPane();
            io.m3fx.controls.overlay.OverlayLayer overlay =
                    new io.m3fx.controls.overlay.OverlayLayer();
            root.getChildren().add(overlay);
            new Scene(root, 480, 320);
            layer.set(overlay);
            snackbar.set(new M3Snackbar(overlay));

            snackbar.get().show("First message");
            snackbar.get().show("Second message", "Undo", null);
        });
        waitFor(true);

        runOnFx(() -> snackbar.get().dismiss());
        waitFor(false);
    }

    @Test
    void hoverPausesAutoDismissButNotTheDisplayCap() throws Throwable {
        runOnFx(() -> {
            StackPane root = new StackPane();
            io.m3fx.controls.overlay.OverlayLayer overlay =
                    new io.m3fx.controls.overlay.OverlayLayer();
            root.getChildren().add(overlay);
            new Scene(root, 480, 320);
            M3Snackbar snackbar = new M3Snackbar(overlay);
            snackbar.show("Reading a long message");
            javafx.animation.PauseTransition cap = snackbar.maxDisplayForTesting();
            assertTrue(cap != null, "the display-cap timer exists while a message shows");
            assertEquals(M3Snackbar.MAX_DISPLAY_MS, (int) cap.getDuration().toMillis(),
                    "the cap is the documented 30s deadline");

            // Hovering pauses the countdown but never the cap.
            javafx.scene.Node bar = ((javafx.scene.layout.VBox)
                    snackbar.currentNodeForTesting()).getChildren().get(0);
            javafx.scene.input.MouseEvent entered = new javafx.scene.input.MouseEvent(
                    javafx.scene.input.MouseEvent.MOUSE_ENTERED, 0, 0, 0, 0,
                    javafx.scene.input.MouseButton.PRIMARY, 1, false, false, false, false,
                    false, true, false, false, false, false, null);
            bar.fireEvent(entered);
            javafx.animation.PauseTransition autoDismiss = snackbar.autoDismissForTesting();
            assertTrue(autoDismiss != null && autoDismiss.getStatus()
                            == javafx.animation.Animation.Status.PAUSED,
                    "hover pauses the auto-dismiss countdown");
            assertEquals(javafx.animation.Animation.Status.RUNNING, cap.getStatus(),
                    "the display cap keeps running while hovered");
        });
    }

    @Test
    void displayCapDismissesAHoveredSnackbar() throws Throwable {
        AtomicReference<io.m3fx.controls.overlay.OverlayLayer> overlayRef =
                new AtomicReference<>();
        runOnFx(() -> {
            StackPane root = new StackPane();
            io.m3fx.controls.overlay.OverlayLayer overlay =
                    new io.m3fx.controls.overlay.OverlayLayer();
            root.getChildren().add(overlay);
            new Scene(root, 480, 320);
            layer.set(overlay);
            overlayRef.set(overlay);
            M3Snackbar snackbar = new M3Snackbar(overlay);
            snackbar.show("Stuck open?");
            javafx.animation.PauseTransition cap = snackbar.maxDisplayForTesting();
            assertTrue(cap != null);
            assertTrue(overlay.transientChildrenCountForTesting() == 1, "snackbar shown");

            // Simulate 30s elapsing while hovered: shrink the cap to 1ms and let it fire.
            cap.setDuration(javafx.util.Duration.millis(1));
            cap.playFromStart();
        });
        // The dismissal is asynchronous (fade-out); poll for the transient layer to clear.
        long deadline = System.currentTimeMillis() + 4000;
        while (System.currentTimeMillis() < deadline) {
            AtomicBoolean cleared = new AtomicBoolean(false);
            runOnFx(() -> cleared.set(overlayRef.get().transientChildrenCountForTesting() == 0));
            if (cleared.get()) {
                return;
            }
            Thread.sleep(50);
        }
        assertTrue(false, "the display cap must dismiss the snackbar");
    }


    @Test
    void closeButtonCarriesADismissTooltip() throws Throwable {
        runOnFx(() -> {
            StackPane root = new StackPane();
            io.m3fx.controls.overlay.OverlayLayer overlay =
                    new io.m3fx.controls.overlay.OverlayLayer();
            root.getChildren().add(overlay);
            new Scene(root, 480, 320);
            M3Snackbar snackbar = new M3Snackbar(overlay);
            snackbar.show("Saved");
            // The snackbar wrapper is the transient node on the overlay layer.
            // The ctor stacks scrim, sheet, modal, anchored and transient layers (index 4).
            javafx.scene.Node transientLayer = overlay.getChildren().get(4);
            javafx.scene.layout.VBox wrapper = (javafx.scene.layout.VBox)
                    ((javafx.scene.layout.Pane) transientLayer).getChildren().get(0);
            javafx.scene.layout.HBox bar = (javafx.scene.layout.HBox)
                    wrapper.getChildren().get(0);
            javafx.scene.control.Button close = (javafx.scene.control.Button)
                    bar.getChildren().get(bar.getChildren().size() - 1);
            assertEquals("Dismiss", close.getAccessibleText());
            javafx.scene.control.Tooltip tooltip = close.getTooltip();
            assertTrue(tooltip != null && "Dismiss".equals(tooltip.getText()),
                    "the snackbar close button installs a Dismiss tooltip");
        });
    }
}
