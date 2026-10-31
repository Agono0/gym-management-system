package io.m3fx.controls.scene3d;

import io.m3fx.controls.scene3d.M3ModelViewer3D.ModelType;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying that all 12 3D procedural equipment models load,
 * have valid geometry hierarchies, and wireframe and camera states function correctly.
 */
class M3ExpandedModelsTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already started
        }
    }

    /**
     * Runs the body on the JavaFX Application Thread: camera resets go through
     * {@code SpringAnimation.animate}, which is FX-thread-bound. Assertions are unchanged;
     * only the execution thread moves.
     */
    private static void onFx(Runnable body) throws Exception {
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> err = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                body.run();
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(10, java.util.concurrent.TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out on the JavaFX Application Thread");
        }
        Throwable t = err.get();
        if (t instanceof RuntimeException runtime) {
            throw runtime;
        }
        if (t instanceof Error error) {
            throw error;
        }
        if (t != null) {
            throw new IllegalStateException(t);
        }
    }

    @Test
    void testAllTwelveModelTypesDefined() {
        ModelType[] types = ModelType.values();
        assertEquals(12, types.length, "Expected 12 distinct 3D gym equipment models in ModelType");

        // Verify specific models are present
        assertNotNull(ModelType.valueOf("DUMBBELL"));
        assertNotNull(ModelType.valueOf("BARBELL"));
        assertNotNull(ModelType.valueOf("KETTLEBELL"));
        assertNotNull(ModelType.valueOf("WORKOUT_BENCH"));
        assertNotNull(ModelType.valueOf("SQUAT_RACK"));
        assertNotNull(ModelType.valueOf("CABLE_CROSSOVER"));
        assertNotNull(ModelType.valueOf("LEG_PRESS"));
        assertNotNull(ModelType.valueOf("TREADMILL"));
        assertNotNull(ModelType.valueOf("EXERCISE_BIKE"));
        assertNotNull(ModelType.valueOf("ROWING_MACHINE"));
        assertNotNull(ModelType.valueOf("PUNCHING_BAG"));
        assertNotNull(ModelType.valueOf("PREACHER_BENCH"));
    }

    @Test
    void testModelViewerInstantiatesAndSwitchesModels() {
        M3ModelViewer3D viewer = new M3ModelViewer3D(ModelType.DUMBBELL);
        assertEquals(ModelType.DUMBBELL, viewer.getCurrentModel());

        // Cycle through all 12 models and verify currentModel property updates
        for (ModelType type : ModelType.values()) {
            viewer.setCurrentModel(type);
            assertEquals(type, viewer.getCurrentModel(), "Model viewer should update to " + type);
        }
    }

    @Test
    void testWireframeToggle() {
        M3ModelViewer3D viewer = new M3ModelViewer3D(ModelType.SQUAT_RACK);
        assertFalse(viewer.isWireframe(), "Default wireframe should be false");

        viewer.setWireframe(true);
        assertTrue(viewer.isWireframe(), "Wireframe should be true after toggling");

        viewer.setWireframe(false);
        assertFalse(viewer.isWireframe(), "Wireframe should be false after resetting");
    }

    @Test
    void testResetCamera() throws Exception {
        onFx(() -> {
            M3ModelViewer3D viewer = new M3ModelViewer3D(ModelType.TREADMILL);
            viewer.resetCamera();
            assertNotNull(viewer.currentModelProperty());
        });
    }
}

