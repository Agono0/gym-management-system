package io.m3fx.controls.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Verifies the anchored-panel behavior: the panel tracks its anchor while shown (window moves
 * and resizes recompute the translation), the reposition watchers attach on show and detach on
 * hide, and dismissal runs exactly once.
 */
class OverlayLayerTest {

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

    @Test
    void anchoredPanelFollowsWindowMovement() throws Throwable {
        AtomicReference<Stage> stageRef = new AtomicReference<>();
        try {
            AtomicReference<Rectangle> anchorRef = new AtomicReference<>();
            AtomicReference<Region> panelRef = new AtomicReference<>();
            AtomicReference<OverlayLayer> layerRef = new AtomicReference<>();
            AtomicReference<Double> translateBefore = new AtomicReference<>();
            AtomicReference<Double> translateBeforeX = new AtomicReference<>();
            runOnFx(() -> {
                OverlayLayer layer = new OverlayLayer();
                Rectangle anchor = new Rectangle(120, 32);
                StackPane root = new StackPane(layer, anchor);
                root.setPadding(new javafx.geometry.Insets(40));
                Scene scene = new Scene(root, 420, 300);
                Stage stage = new Stage();
                stage.setScene(scene);
                stage.show();
                assertTrue(stage.isShowing(), "the test stage must be visible for screen math");
                stageRef.set(stage);

                Region panel = new Region();
                panel.setPrefSize(120, 20);
                panel.setStyle("-fx-background-color: #123456;");
                layer.showAnchored(anchor, panel, null);
                assertTrue(layer.anchoredPanelIsWatched(), "watchers attach while shown");

                anchorRef.set(anchor);
                panelRef.set(panel);
                layerRef.set(layer);
                translateBefore.set(panel.getTranslateY());
                translateBeforeX.set(panel.getTranslateX());
            });
            runOnFx(() -> {
                // Moving the window must move the anchored panel with it (synchronous delta
                // from the window x/y listeners).
                Stage stage = stageRef.get();
                stage.setX(stage.getX() + 120);
                stage.setY(stage.getY() + 60);
                Region panel = panelRef.get();
                // The window-move delta is exact (window x/y are always current; localToScreen
                // can lag one pulse behind the peer), so assert the pure delta.
                assertEquals(translateBefore.get() + 60, panel.getTranslateY(), 0.5,
                        "moving the window down 60px moves the panel down 60px");
                assertEquals(translateBeforeX.get() + 120, panel.getTranslateX(), 0.5,
                        "moving the window right 120px moves the panel right 120px");
            });
        } finally {
            // A failed assertion must not leave the stage open: the FX thread would keep the
            // test JVM alive and hang Gradle.
            try {
                runOnFx(() -> {
                    if (stageRef.get() != null) {
                        stageRef.get().hide();
                    }
                });
            } catch (Throwable cleanupFailure) {
                // best effort
            }
        }
    }

    @Test
    void watchersAttachOnShowAndDetachOnHide() throws Throwable {
        runOnFx(() -> {
            OverlayLayer layer = new OverlayLayer();
            Rectangle anchor = new Rectangle(120, 32);
            new Scene(new Group(layer, anchor));
            assertFalse(layer.anchoredPanelIsWatched(), "nothing watched before show");

            Region panel = new Region();
            layer.showAnchored(anchor, panel, null);
            assertTrue(layer.anchoredPanelIsWatched(), "watchers attach on show");

            layer.hideAnchored();
            assertFalse(layer.anchoredPanelIsWatched(), "watchers detach on hide");
        });
    }

    @Test
    void anchoredDismissRunsExactlyOnceWhoeverDismisses() throws Throwable {
        runOnFx(() -> {
            OverlayLayer layer = new OverlayLayer();
            Rectangle anchor = new Rectangle(120, 32);
            new Scene(new Group(layer, anchor));
            int[] calls = {0};
            Region panel = new Region();
            layer.showAnchored(anchor, panel, () -> calls[0]++);
            layer.hideAnchored();
            layer.hideAnchored();
            assertEquals(1, calls[0], "hideAnchored is idempotent");
        });
    }
}
