package io.m3fx.controls.glass;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * B1 lifecycle fixes: the glass physics {@code AnimationTimer} only runs while the pane is
 * attached to a shown window with a fully visible node tree, and {@code dispose()} always
 * stops it.
 */
class GlassPaneLifecycleTest {

    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized
        }
        // These tests show and close real stages; without this the close of the last window
        // would implicitly exit the toolkit and kill the FX thread for every later test.
        Platform.setImplicitExit(false);
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
        // 30s: the first Stage.show() of the test JVM creates a real native window, which can
        // block the FX thread far longer than the usual 5s window on a cold machine. While this
        // task is queued the shared FX thread is busy, so other tests simply wait behind it.
        assertTrue(latch.await(30, TimeUnit.SECONDS), "FX task timed out");
        if (error[0] != null) {
            throw error[0];
        }
    }

    @Test
    void physicsTimerStaysIdleUntilThePaneIsShown() throws Throwable {
        runOnFx(() -> {
            M3GlassPane pane = new M3GlassPane();
            StackPane holder = new StackPane(pane);
            Scene scene = new Scene(holder);
            pane.applyCss(); // skin created with a scene but no window yet
            GlassPaneSkin skin = (GlassPaneSkin) pane.getSkin();
            assertNotNull(skin, "the skin is created during applyCss");
            assertFalse(skin.isPhysicsTimerActive(),
                    "a pane without a showing window must not wake the timer every pulse");

            Stage stage = new Stage();
            try {
                stage.setScene(scene);
                assertFalse(skin.isPhysicsTimerActive(),
                        "attaching a hidden stage does not start the timer");
                stage.show();
                assertTrue(skin.isPhysicsTimerActive(),
                        "a shown window starts the physics timer");
                pane.setVisible(false);
                assertFalse(skin.isPhysicsTimerActive(), "hiding the pane stops the timer");
                pane.setVisible(true);
                assertTrue(skin.isPhysicsTimerActive(),
                        "re-showing the pane restarts the timer");
            } finally {
                stage.hide();
            }
            assertFalse(skin.isPhysicsTimerActive(), "hiding the window stops the timer");
            stage.close();
        });
    }

    @Test
    void disposeStopsThePhysicsTimerAndIsIdempotent() throws Throwable {
        runOnFx(() -> {
            M3GlassPane pane = new M3GlassPane();
            StackPane holder = new StackPane(pane);
            Scene scene = new Scene(holder);
            Stage stage = new Stage();
            try {
                stage.setScene(scene);
                stage.show();
                pane.applyCss(); // skin created while the window is already showing
                GlassPaneSkin skin = (GlassPaneSkin) pane.getSkin();
                assertTrue(skin.isPhysicsTimerActive(),
                        "a pane created into a shown window runs the physics timer");

                skin.dispose();
                skin.dispose(); // must be idempotent
                assertFalse(skin.isPhysicsTimerActive(), "dispose stops the physics timer");
            } finally {
                stage.hide();
            }
            stage.close();
        });
    }
}
