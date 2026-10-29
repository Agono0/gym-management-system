package io.m3fx.controls.scene3d;

import static org.junit.jupiter.api.Assertions.*;

import io.m3fx.controls.scene3d.M3AnatomyViewer3D.MuscleGroup;
import io.m3fx.controls.scene3d.M3ModelViewer3D.ModelType;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for Phase 2 3D Hardware-Accelerated & Holographic System.
 */
class M3Scene3DTest {

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
    void tiltCard3DCreationAndMaxAngle() throws Throwable {
        runOnFx(() -> {
            Label label = new Label("Holographic Tilt");
            M3TiltCard3D card = new M3TiltCard3D(label);
            assertEquals(14.0, card.getMaxTiltAngle());

            card.setMaxTiltAngle(20.0);
            assertEquals(20.0, card.getMaxTiltAngle());

            assertNotNull(card.getContentContainer());
            assertTrue(card.getContentContainer().getChildren().contains(label));

            // Verify bounds stay 2D RectBounds
            card.resize(280, 180);
            javafx.geometry.Bounds bounds = card.getBoundsInParent();
            assertTrue(bounds.getDepth() <= 0.0001, "TiltCard must remain in 2D bounds to prevent Prism BoxBounds crash");

            // Simulate mouse moved
            javafx.scene.input.MouseEvent moveEvent = new javafx.scene.input.MouseEvent(
                    javafx.scene.input.MouseEvent.MOUSE_MOVED,
                    180, 120, 180, 120,
                    javafx.scene.input.MouseButton.NONE, 0,
                    false, false, false, false, false, false, false, false, false, false, null
            );
            assertDoesNotThrow(() -> card.fireEvent(moveEvent));

            // Simulate mouse exited
            javafx.scene.input.MouseEvent exitEvent = new javafx.scene.input.MouseEvent(
                    javafx.scene.input.MouseEvent.MOUSE_EXITED,
                    0, 0, 0, 0,
                    javafx.scene.input.MouseButton.NONE, 0,
                    false, false, false, false, false, false, false, false, false, false, null
            );
            assertDoesNotThrow(() -> card.fireEvent(exitEvent));
        });
    }

    @Test
    void scene3DCameraControls() throws Throwable {
        runOnFx(() -> {
            M3Scene3D scene = new M3Scene3D(300, 200);
            assertNotNull(scene.getWorldGroup());
            assertNotNull(scene.cameraPitchProperty());
            assertNotNull(scene.cameraYawProperty());
            assertNotNull(scene.cameraDistanceProperty());

            scene.cameraDistanceProperty().set(500.0);
            assertEquals(500.0, scene.cameraDistanceProperty().get());

            assertDoesNotThrow(scene::resetCamera);
        });
    }

    @Test
    void modelViewerAllModelPresets() throws Throwable {
        runOnFx(() -> {
            M3ModelViewer3D viewer = new M3ModelViewer3D(ModelType.DUMBBELL);
            assertEquals(ModelType.DUMBBELL, viewer.getCurrentModel());

            for (ModelType model : ModelType.values()) {
                assertDoesNotThrow(() -> viewer.setCurrentModel(model));
                assertEquals(model, viewer.getCurrentModel());
            }

            viewer.setWireframe(true);
            assertTrue(viewer.isWireframe());

            viewer.setAutoRotate(false);
            assertFalse(viewer.isAutoRotate());
        });
    }

    @Test
    void anatomyViewerMuscleSelection() throws Throwable {
        runOnFx(() -> {
            M3AnatomyViewer3D anatomy = new M3AnatomyViewer3D();
            assertEquals(MuscleGroup.CHEST, anatomy.getActiveMuscle());

            for (MuscleGroup group : MuscleGroup.values()) {
                assertDoesNotThrow(() -> anatomy.setActiveMuscle(group));
                assertEquals(group, anatomy.getActiveMuscle());
            }
        });
    }

    @Test
    void gyroRingSpinningToggle() throws Throwable {
        runOnFx(() -> {
            M3GyroRing3D gyro = new M3GyroRing3D();
            assertTrue(gyro.isSpinning());

            gyro.setSpinning(false);
            assertFalse(gyro.isSpinning());
        });
    }
}

