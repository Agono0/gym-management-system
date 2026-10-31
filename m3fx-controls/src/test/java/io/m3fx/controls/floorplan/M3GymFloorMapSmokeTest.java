package io.m3fx.controls.floorplan;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.m3fx.controls.floorplan.M3GymFloorMap.EquipmentNode;
import io.m3fx.controls.floorplan.M3GymFloorMap.EquipmentStatus;
import io.m3fx.controls.scene3d.M3ModelViewer3D.ModelType;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Smoke test for the exported {@link M3GymFloorMap} package: the map constructs with sensible
 * defaults, its equipment pins are observable through the public list, and adding a pin
 * mutates the model that the canvas renders from. (Interaction coverage lives in
 * {@link M3GymFloorMapTest}.)
 *
 * <p>Note on zones: {@code ZoneArea}s are kept in a private list with no public add API, so
 * only the equipment-pin side of the model is asserted here.</p>
 */
class M3GymFloorMapSmokeTest {

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
    void floorMapConstructsWithSensibleDefaults() throws Throwable {
        runOnFx(() -> {
            M3GymFloorMap map = new M3GymFloorMap();
            assertTrue(map.getStyleClass().contains("m3-gym-floor-map"),
                    "the map carries its style class");
            assertEquals(720, map.getPrefWidth(), 0.001, "design-space default width");
            assertEquals(420, map.getPrefHeight(), 0.001, "design-space default height");

            assertFalse(map.getEquipmentList().isEmpty(), "the map ships default equipment pins");
            for (EquipmentNode node : map.getEquipmentList()) {
                assertNotNull(node.id, "every default pin carries an id");
                assertNotNull(node.name, "every default pin carries a name");
                assertNotNull(node.status, "every default pin carries a status");
            }
        });
    }

    @Test
    void floorMapAcceptsAddedPins() throws Throwable {
        runOnFx(() -> {
            M3GymFloorMap map = new M3GymFloorMap();
            int before = map.getEquipmentList().size();

            EquipmentNode pin = new EquipmentNode("EQ-SMOKE", "Smoke Test Rower",
                    ModelType.ROWING_MACHINE, 300, 150, EquipmentStatus.AVAILABLE, null, 0);
            map.getEquipmentList().add(pin);

            assertEquals(before + 1, map.getEquipmentList().size(),
                    "adding a pin grows the observable equipment list");
            assertSame(pin, map.getEquipmentList().get(map.getEquipmentList().size() - 1),
                    "the added pin is retrievable through the public list");

            // Mutable live state round-trips so the canvas repaint shows the new status.
            pin.status = EquipmentStatus.MAINTENANCE;
            assertSame(EquipmentStatus.MAINTENANCE,
                    map.getEquipmentList().get(map.getEquipmentList().size() - 1).status,
                    "pin status updates are visible through the list");

            // Resizing triggers the redraw over the updated model and must not throw.
            map.resize(600, 400);
            assertDoesNotThrow(() -> map.layout());
        });
    }
}
