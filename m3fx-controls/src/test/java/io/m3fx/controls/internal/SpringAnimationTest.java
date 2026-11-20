package io.m3fx.controls.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.m3fx.controls.common.SpringParams;
import io.m3fx.core.token.MotionTokens;
import io.m3fx.core.token.MotionTokens.MotionScheme;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Verifies the spring integrator converges and that underdamped tunings overshoot while
 * critically damped ones do not (pure math — no FX toolkit needed).
 */
class SpringAnimationTest {

    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already started
        }
    }

    /**
     * Runs the body on the JavaFX Application Thread: {@code SpringAnimation.animate} is
     * FX-thread-bound (it starts AnimationTimers and mutates properties), so the one test that
     * calls it must run there. Assertions are unchanged; only the execution thread moves.
     */
    private static void onFx(Runnable body) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                body.run();
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(10, TimeUnit.SECONDS)) {
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

    private static double settle(double stiffness, double dampingRatio) {
        double x = 0;
        double v = 0;
        double max = 0;
        double dt = 1 / 60.0;
        for (int i = 0; i < 600; i++) {
            double[] state = SpringAnimation.step(x, v, 100, stiffness, dampingRatio, dt);
            x = state[0];
            v = state[1];
            max = Math.max(max, x);
        }
        if (Math.abs(x - 100) > 1) {
            throw new AssertionError("did not settle: " + x);
        }
        return max;
    }

    @Test
    void defaultSettles() {
        settle(SpringAnimation.DEFAULT.stiffness(), SpringAnimation.DEFAULT.dampingRatio());
    }

    @Test
    void bouncyOvershoots() {
        double max = settle(SpringAnimation.BOUNCY.stiffness(), SpringAnimation.BOUNCY.dampingRatio());
        assertTrue(max > 100, "bouncy spring should overshoot, max was " + max);
    }

    @Test
    void gentleDoesNotOvershoot() {
        double max = settle(SpringAnimation.GENTLE.stiffness(), SpringAnimation.GENTLE.dampingRatio());
        assertTrue(max <= 100.5, "gentle spring should not overshoot, max was " + max);
    }

    @Test
    void degenerateSpringSnapsWithoutAnimating() throws Exception {
        // Zero stiffness can never settle; the animation must snap to the target instead of
        // running an infinite timer. The snap path is synchronous (no AnimationTimer started).
        onFx(() -> {
            SimpleDoubleProperty property = new SimpleDoubleProperty(5);
            SpringAnimation.animate(property, 42, new SpringParams(0, 0.5), null);
            assertEquals(42, property.get(), 1e-9);
        });
    }

    @Test
    void stepIsFiniteForLargeTimestep() {
        // A clamped-out frame (tab-back) must not poison the state with NaN/Infinite.
        double[] state = SpringAnimation.step(0, 0, 100, 380, 0.65, 1.0);
        assertTrue(Double.isFinite(state[0]));
        assertTrue(Double.isFinite(state[1]));
    }

    @Test
    void reducedMotionSnapsWithoutStartingATimer() throws Exception {
        onFx(() -> {
            MotionTokens.setReducedMotion(true);
            try {
                SimpleDoubleProperty property = new SimpleDoubleProperty(5);
                SpringAnimation.animate(property, 42, SpringAnimation.SLOW, null);
                assertEquals(42, property.get(), 1e-9, "reduced motion must snap synchronously");
                assertFalse(SpringAnimation.isAnimating(property), "no timer may start");
            } finally {
                MotionTokens.setReducedMotion(false);
            }
        });
    }

    @Test
    void flagFlipSnapsRunningSpringsLive() throws Exception {
        onFx(() -> {
            SimpleDoubleProperty property = new SimpleDoubleProperty(0);
            AtomicInteger finished = new AtomicInteger();
            SpringAnimation.animate(property, 5000, SpringAnimation.SLOW, finished::incrementAndGet);
            assertTrue(SpringAnimation.isAnimating(property), "spring must be running");

            MotionTokens.setReducedMotion(true);
            try {
                assertEquals(5000, property.get(), 1e-9, "running spring must snap on the flip");
                assertFalse(SpringAnimation.isAnimating(property), "timer must be stopped");
                assertEquals(1, finished.get(), "completion callback must fire on the snap");
            } finally {
                MotionTokens.setReducedMotion(false);
            }
        });
    }

    @Test
    void schemeSwitchResolvesTheSpatialPresets() {
        try {
            MotionTokens.setMotionScheme(MotionScheme.STANDARD);
            assertEquals(SpringAnimation.STANDARD_DEFAULT,
                    SpringAnimation.resolveForScheme(SpringAnimation.DEFAULT));
            assertEquals(SpringAnimation.STANDARD_DEFAULT,
                    SpringAnimation.resolveForScheme(SpringAnimation.BOUNCY),
                    "standard language has no bounce: bouncy collapses to the default");
            assertEquals(SpringAnimation.STANDARD_FAST,
                    SpringAnimation.resolveForScheme(SpringAnimation.FAST));
            assertEquals(SpringAnimation.STANDARD_SLOW,
                    SpringAnimation.resolveForScheme(SpringAnimation.SLOW));
            assertEquals(SpringAnimation.GENTLE,
                    SpringAnimation.resolveForScheme(SpringAnimation.GENTLE),
                    "effects presets are scheme-agnostic");
            SpringParams custom = new SpringParams(123, 0.7);
            assertSame(custom, SpringAnimation.resolveForScheme(custom),
                    "custom tunings pass through unchanged");
        } finally {
            MotionTokens.setMotionScheme(MotionScheme.EXPRESSIVE);
        }
    }

    @Test
    void expressiveSchemeKeepsTheGridDefaults() {
        assertSame(SpringAnimation.DEFAULT, SpringAnimation.resolveForScheme(SpringAnimation.DEFAULT));
        assertSame(SpringAnimation.BOUNCY, SpringAnimation.resolveForScheme(SpringAnimation.BOUNCY));
        assertSame(SpringAnimation.FAST, SpringAnimation.resolveForScheme(SpringAnimation.FAST));
        assertSame(SpringAnimation.SLOW, SpringAnimation.resolveForScheme(SpringAnimation.SLOW));
    }

    @Test
    void standardSchemePresetsSettleWithoutVisibleBounce() {
        double maxDefault = settle(SpringAnimation.STANDARD_DEFAULT.stiffness(),
                SpringAnimation.STANDARD_DEFAULT.dampingRatio());
        assertTrue(maxDefault <= 100.5, "standard default must settle without bounce, max " + maxDefault);
        double maxFast = settle(SpringAnimation.STANDARD_FAST.stiffness(),
                SpringAnimation.STANDARD_FAST.dampingRatio());
        assertTrue(maxFast <= 100.5, "standard fast must settle without bounce, max " + maxFast);
    }
}
