package io.m3fx.controls.floorplan;

import static org.junit.jupiter.api.Assertions.*;

import io.m3fx.controls.floorplan.M3GymFloorMap.EquipmentNode;
import io.m3fx.controls.floorplan.M3GymFloorMap.EquipmentStatus;
import io.m3fx.controls.scene3d.M3ModelViewer3D.ModelType;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit tests for Interactive Gym Floorplan & Equipment Map.
 */
class M3GymFloorMapTest {

    @BeforeAll
    static void initFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX failed to initialize");
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
        assertTrue(latch.await(5, TimeUnit.SECONDS), "FX task timed out");
        if (error[0] != null) {
            throw error[0];
        }
    }

    @Test
    void floorMapDefaultNodesAndInspection() throws Throwable {
        runOnFx(() -> {
            M3GymFloorMap map = new M3GymFloorMap();
            assertFalse(map.getEquipmentList().isEmpty());

            EquipmentNode first = map.getEquipmentList().get(0);
            assertNotNull(first.id);
            assertNotNull(first.name);
            assertNotNull(first.status);

            AtomicBoolean inspected = new AtomicBoolean(false);
            map.setOnInspect3D(node -> {
                assertEquals(first.id, node.id);
                inspected.set(true);
            });

            assertNotNull(map.getOnInspect3D());
            map.getOnInspect3D().accept(first);
            assertTrue(inspected.get());

            map.resize(600, 400);
            assertDoesNotThrow(() -> map.layout());
        });
    }

    // ------------------------------------------------------------------
    // B16: layer visibility properties + list-change redraw (pixel evidence)
    // ------------------------------------------------------------------

    /** Lets one FX pulse land so deferred Canvas drawing commands flush into the buffer. */
    private static void awaitPulse() {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("pulse timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("pulse interrupted", e);
        }
    }

    private static M3GymFloorMap shownMap(Stage stage) {
        M3GymFloorMap map = new M3GymFloorMap(); // default 720x420 = design scale 1.0
        map.setPrefSize(720, 420);
        StackPane root = new StackPane(map);
        Scene scene = new Scene(root, 720, 420);
        stage.setScene(scene);
        stage.show();
        return map;
    }

    /** Alpha at (x, y) of the map's snapshot: opaque where a pin or zone fill is drawn. */
    private static int alphaAt(M3GymFloorMap map, double x, double y) {
        javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
        params.setFill(javafx.scene.paint.Color.TRANSPARENT);
        javafx.scene.image.WritableImage image = map.snapshot(params, null);
        return (int) Math.round(
                image.getPixelReader().getColor((int) x, (int) y).getOpacity() * 255);
    }

    @Test
    void pinsLayerPropertyTogglesTheRenderedPins() throws Throwable {
        Platform.setImplicitExit(false);
        AtomicReference<Stage> stageRef = new AtomicReference<>();
        try {
            AtomicReference<M3GymFloorMap> mapRef = new AtomicReference<>();
            runOnFx(() -> {
                Stage stage = new Stage();
                M3GymFloorMap map = shownMap(stage);
                stageRef.set(stage);
                mapRef.set(map);
                assertTrue(map.isPinsVisible(), "pins default to visible");
                // EQ-01 pin center (80, 80) is opaque while the pins layer is drawn.
                assertTrue(alphaAt(map, 80, 80) > 200, "pin body renders");
                map.setPinsVisible(false);
            });
            awaitPulse();
            runOnFx(() -> assertTrue(alphaAt(mapRef.get(), 80, 80) < 40,
                    "hiding the pins layer clears the pin body"));
        } finally {
            try {
                runOnFx(() -> {
                    if (stageRef.get() != null) {
                        stageRef.get().hide();
                    }
                });
            } catch (Throwable cleanupFailure) {
                // best effort - a failed assertion must not leave the stage open
            }
        }
    }

    @Test
    void equipmentListChangeRedrawsTheCanvas() throws Throwable {
        Platform.setImplicitExit(false);
        AtomicReference<Stage> stageRef = new AtomicReference<>();
        try {
            AtomicReference<M3GymFloorMap> mapRef = new AtomicReference<>();
            runOnFx(() -> {
                Stage stage = new Stage();
                M3GymFloorMap map = shownMap(stage);
                stageRef.set(stage);
                mapRef.set(map);
                assertTrue(alphaAt(map, 80, 80) > 200, "seeded pin renders");
                // Clearing the observable list must redraw (B16 hook) - no re-mount needed.
                map.getEquipmentList().clear();
            });
            awaitPulse();
            runOnFx(() -> assertTrue(alphaAt(mapRef.get(), 80, 80) < 40,
                    "cleared pins redraw the canvas"));
            runOnFx(() -> mapRef.get().getEquipmentList().add(new EquipmentNode("EQ-X",
                    "Re-added Machine", ModelType.DUMBBELL, 80, 80,
                    EquipmentStatus.AVAILABLE, null, 0)));
            awaitPulse();
            runOnFx(() -> assertTrue(alphaAt(mapRef.get(), 80, 80) > 200,
                    "re-added pin renders"));
        } finally {
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
    void zonesLayerPropertyTogglesTheRenderedBoundaries() throws Throwable {
        Platform.setImplicitExit(false);
        AtomicReference<Stage> stageRef = new AtomicReference<>();
        try {
            AtomicReference<M3GymFloorMap> mapRef = new AtomicReference<>();
            runOnFx(() -> {
                Stage stage = new Stage();
                M3GymFloorMap map = shownMap(stage);
                stageRef.set(stage);
                mapRef.set(map);
                assertTrue(map.isZonesVisible(), "zones default to visible");
                // Point inside the FREE WEIGHTS ARENA fill, away from pins/labels/grid lines:
                // the 8%-alpha zone tint makes the pixel translucent while zones are drawn.
                assertTrue(alphaAt(map, 320, 60) > 0, "zone fill renders");
                map.setZonesVisible(false);
            });
            awaitPulse();
            runOnFx(() -> assertTrue(alphaAt(mapRef.get(), 320, 60) == 0,
                    "hiding the zones layer clears the fill"));
        } finally {
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
}
