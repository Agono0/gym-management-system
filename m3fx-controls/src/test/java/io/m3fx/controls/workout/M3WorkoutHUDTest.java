package io.m3fx.controls.workout;

import static org.junit.jupiter.api.Assertions.*;

import io.m3fx.controls.workout.M3HeartRateZones.Zone;
import io.m3fx.controls.workout.M3IntervalTimer.IntervalState;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for Smart Gym Live Workout HUD & Biometric Telemetry Suite.
 */
class M3WorkoutHUDTest {

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
    void repCounterCountAndSets() throws Throwable {
        runOnFx(() -> {
            M3RepCounter counter = new M3RepCounter(10, 3);
            assertEquals(0, counter.getReps());
            assertEquals(10, counter.getTargetReps());
            assertEquals(1, counter.getCurrentSet());
            assertEquals(3, counter.getTotalSets());

            counter.countRep();
            assertEquals(1, counter.getReps());

            for (int i = 0; i < 9; i++) {
                counter.countRep();
            }
            assertEquals(10, counter.getReps());

            // Advance to set 2
            counter.nextSet();
            assertEquals(2, counter.getCurrentSet());
            assertEquals(0, counter.getReps());

            counter.reset();
            assertEquals(1, counter.getCurrentSet());
            assertEquals(0, counter.getReps());
        });
    }

    @Test
    void heartRateZonesClassification() throws Throwable {
        runOnFx(() -> {
            M3HeartRateZones hr = new M3HeartRateZones(200);

            hr.setCurrentBpm(105);
            assertEquals(Zone.ZONE_1, hr.getCurrentZone());

            hr.setCurrentBpm(125);
            assertEquals(Zone.ZONE_2, hr.getCurrentZone());

            hr.setCurrentBpm(145);
            assertEquals(Zone.ZONE_3, hr.getCurrentZone());

            hr.setCurrentBpm(165);
            assertEquals(Zone.ZONE_4, hr.getCurrentZone());

            hr.setCurrentBpm(190);
            assertEquals(Zone.ZONE_5, hr.getCurrentZone());
        });
    }

    @Test
    void intervalTimerControlsAndPhases() throws Throwable {
        runOnFx(() -> {
            M3IntervalTimer timer = new M3IntervalTimer(30, 10, 5);
            assertEquals(30, timer.getWorkSeconds());
            assertEquals(10, timer.getRestSeconds());
            assertEquals(5, timer.getTotalRounds());
            assertEquals(IntervalState.STOPPED, timer.getState());
            assertFalse(timer.isRunning());

            timer.start();
            assertTrue(timer.isRunning());
            assertEquals(IntervalState.WORK, timer.getState());

            timer.pause();
            assertFalse(timer.isRunning());

            timer.reset();
            assertEquals(IntervalState.STOPPED, timer.getState());
            assertEquals(1, timer.getCurrentRound());
        });
    }

    @Test
    void workoutHUDInstantiation() throws Throwable {
        runOnFx(() -> {
            M3WorkoutHUD hud = new M3WorkoutHUD();
            assertNotNull(hud.getRepCounter());
            assertNotNull(hud.getIntervalTimer());
            assertNotNull(hud.getHeartRateZones());

            hud.setExerciseName("Olympic Deadlift");
            assertDoesNotThrow(() -> hud.layout());
        });
    }
}

