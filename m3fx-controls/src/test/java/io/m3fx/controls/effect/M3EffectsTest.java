package io.m3fx.controls.effect;

import static org.junit.jupiter.api.Assertions.*;

import io.m3fx.core.theme.ColorRole;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for Phase 3 Futuristic Visual Effects & Shaders.
 */
class M3EffectsTest {

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
    void glassPaneCreationAndStyling() throws Throwable {
        runOnFx(() -> {
            Label label = new Label("Frosted Glass Content");
            M3GlassPane glass = new M3GlassPane(label);

            assertEquals(16.0, glass.getBlurRadius());
            assertEquals(16.0, glass.getCornerRadius());
            assertEquals(0.65, glass.getGlassOpacity());

            glass.setGlassOpacity(0.8);
            assertEquals(0.8, glass.getGlassOpacity());

            assertNotNull(glass.getContentBox());
            assertTrue(glass.getContentBox().getChildren().contains(label));
        });
    }

    @Test
    void glowShadowApplicationAndPulse() throws Throwable {
        runOnFx(() -> {
            Label btn = new Label("Glow Button");

            M3GlowShadow.apply(btn, ColorRole.PRIMARY, 18.0, 0.4);
            assertNotNull(btn.getEffect());

            M3GlowShadow.animatePulse(btn, Color.CYAN, 10.0, 24.0);
            assertNotNull(btn.getEffect());

            M3GlowShadow.stopPulse(btn);
            assertNull(btn.getEffect());
        });
    }

    @Test
    void particleCanvasProperties() throws Throwable {
        runOnFx(() -> {
            M3ParticleCanvas canvas = new M3ParticleCanvas();
            assertEquals(48, canvas.getParticleCount());
            assertTrue(canvas.isInteractive());

            canvas.setParticleCount(64);
            assertEquals(64, canvas.getParticleCount());

            canvas.setInteractive(false);
            assertFalse(canvas.isInteractive());
        });
    }

    @Test
    void scanlineProperties() throws Throwable {
        runOnFx(() -> {
            M3Scanline scan = new M3Scanline();
            assertTrue(scan.isAnimated());
            assertEquals(90.0, scan.getScanSpeed());

            scan.setScanSpeed(120.0);
            assertEquals(120.0, scan.getScanSpeed());

            scan.setAnimated(false);
            assertFalse(scan.isAnimated());
        });
    }

    @Test
    void hologramContainerProperties() throws Throwable {
        runOnFx(() -> {
            Label inner = new Label("Holo Data");
            M3Hologram holo = new M3Hologram(inner);

            assertEquals("HOLO-FIELD // ONLINE", holo.getTitle());
            assertTrue(holo.isScanning());
            assertTrue(holo.getContentWrapper().getChildren().contains(inner));

            holo.setTitle("TELEMETRY SENSOR");
            assertEquals("TELEMETRY SENSOR", holo.getTitle());

            holo.setScanning(false);
            assertFalse(holo.isScanning());

            holo.resize(300, 180);
            assertDoesNotThrow(() -> holo.layout());
        });
    }

    @Test
    void pulseBeaconStatusAndPulsing() throws Throwable {
        runOnFx(() -> {
            M3PulseBeacon beacon = new M3PulseBeacon(M3PulseBeacon.BeaconStatus.ONLINE, "Engine Status");
            assertEquals(M3PulseBeacon.BeaconStatus.ONLINE, beacon.getStatus());
            assertEquals("Engine Status", beacon.getLabelText());
            assertTrue(beacon.isPulsing());

            beacon.setStatus(M3PulseBeacon.BeaconStatus.WARNING);
            assertEquals(M3PulseBeacon.BeaconStatus.WARNING, beacon.getStatus());

            beacon.setPulsing(false);
            assertFalse(beacon.isPulsing());
        });
    }
}


