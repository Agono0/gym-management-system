package io.m3fx.controls.floorplan;

import static org.junit.jupiter.api.Assertions.*;

import io.m3fx.controls.floorplan.M3GymFloorMap.EquipmentNode;
import io.m3fx.controls.scene3d.M3ModelViewer3D.ModelType;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
}

