package io.m3fx.controls.workout;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Smoke test for the exported {@link M3WorkoutHUD} / {@link M3RepCounter} package: the HUD
 * constructs with its telemetry panels wired and the rep counter's key state transitions hold.
 * (Heart-rate zone and interval-timer coverage lives in {@link M3WorkoutHUDTest}.)
 *
 * <p>Note on "set reps clamps": {@code M3RepCounter.setReps(int)} is a plain property setter —
 * the current API does NOT clamp the value (the clamp lives in the arc rendering and in the
 * last-set guard of {@code nextSet()}). This smoke test asserts the transitions the real API
 * provides: set/count, the last-set guard, and reset.</p>
 */
class M3WorkoutHUDSmokeTest {

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
    void workoutHUDConstructsWithWiredTelemetryPanels() throws Throwable {
        runOnFx(() -> {
            M3WorkoutHUD hud = new M3WorkoutHUD();
            assertTrue(hud.getStyleClass().contains("m3-workout-hud"),
                    "the HUD carries its style class");
            assertNotNull(hud.getRepCounter(), "the rep counter panel must be wired");
            assertNotNull(hud.getIntervalTimer(), "the interval timer panel must be wired");
            assertNotNull(hud.getHeartRateZones(), "the heart-rate zones panel must be wired");

            hud.setExerciseName("Back Squat 5x5");
            assertDoesNotThrow(() -> hud.layout(), "layout must hold after setting the exercise name");
        });
    }

    @Test
    void repCounterSetCountAndLastSetGuard() throws Throwable {
        runOnFx(() -> {
            M3RepCounter counter = new M3RepCounter();
            assertEquals(0, counter.getReps(), "a fresh counter starts at zero reps");
            assertEquals(12, counter.getTargetReps(), "standard target is 12 reps");
            assertEquals(1, counter.getCurrentSet(), "a fresh counter starts on set 1");
            assertEquals(4, counter.getTotalSets(), "standard plan is 4 sets");

            // setReps is an unclamped property setter: the value round-trips exactly.
            counter.setReps(7);
            assertEquals(7, counter.getReps());
            counter.setReps(0);
            assertEquals(0, counter.getReps());

            counter.countRep();
            counter.countRep();
            assertEquals(2, counter.getReps(), "countRep registers one rep per call");

            // Last-set guard: nextSet() never advances past the final set.
            counter.setCurrentSet(counter.getTotalSets());
            counter.nextSet();
            assertEquals(counter.getTotalSets(), counter.getCurrentSet(),
                    "nextSet() must not advance past the last set");
            assertFalse(counter.getCurrentSet() > counter.getTotalSets());

            counter.reset();
            assertEquals(1, counter.getCurrentSet(), "reset returns to set 1");
            assertEquals(0, counter.getReps(), "reset clears the reps");
        });
    }
}
