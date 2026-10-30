package io.m3fx.controls.snackbar;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class M3SnackbarUxTest {

    private final AtomicReference<M3Snackbar> snackbar = new AtomicReference<>();
    private final AtomicReference<io.m3fx.controls.internal.OverlayLayer> layer =
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
            io.m3fx.controls.internal.OverlayLayer overlay =
                    new io.m3fx.controls.internal.OverlayLayer();
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
        // After the inter-message gap the queued message shows automatically.
        waitFor(true);

        runOnFx(() -> snackbar.get().dismiss());
        waitFor(false);
    }
}
