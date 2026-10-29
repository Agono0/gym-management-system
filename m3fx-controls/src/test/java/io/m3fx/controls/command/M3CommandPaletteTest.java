package io.m3fx.controls.command;

import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unit tests for Global Spotlight Command Palette.
 */
class M3CommandPaletteTest {

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
    void paletteRegistrationAndToggle() throws Throwable {
        runOnFx(() -> {
            M3CommandPalette palette = new M3CommandPalette();
            assertFalse(palette.isOpen());

            AtomicBoolean executed = new AtomicBoolean(false);
            palette.registerAction("Workouts", "Start Session", "Live Telemetry", "fitness", () -> executed.set(true));

            assertEquals(1, palette.getMasterActions().size());

            palette.show();
            assertTrue(palette.isOpen());

            palette.close();
            assertFalse(palette.isOpen());

            palette.toggle();
            assertTrue(palette.isOpen());
        });
    }
}

