package io.m3fx.core.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.animation.Interpolator;
import org.junit.jupiter.api.Test;

/**
 * Locks the {@link Interpolator} contract of {@link SpringInterpolator}: exact endpoints (0 at
 * t=0, 1 at t=1, clamped outside), finite progress in between, and a strictly non-decreasing
 * critically damped response (no overshoot for &zeta; = 1).
 */
class SpringInterpolatorEndpointsTest {

    @Test
    void presetsSatisfyTheInterpolatorEndpoints() {
        // Only the non-deprecated presets here; the deprecated ones are covered by construction.
        for (SpringInterpolator spring : new SpringInterpolator[] {
            SpringInterpolator.DEFAULT, SpringInterpolator.FLUID,
        }) {
            assertEquals(0.0, spring.interpolate(0.0, 1.0, 0.0), 1e-12,
                    spring.getDampingRatio() + " must start at 0");
            assertEquals(1.0, spring.interpolate(0.0, 1.0, 1.0), 1e-12,
                    spring.getDampingRatio() + " must end at 1");
        }
    }

    @Test
    void endpointsHoldAcrossTheDampingAndFrequencyGrid() {
        for (double dampingRatio : new double[] {0.1, 0.5, 0.70, 0.82, 1.0}) {
            for (double frequency : new double[] {1.0, 7.5, 20.0, 100.0}) {
                SpringInterpolator spring = new SpringInterpolator(dampingRatio, frequency);
                assertEquals(0.0, spring.interpolate(0.0, 1.0, 0.0), 1e-12);
                assertEquals(1.0, spring.interpolate(0.0, 1.0, 1.0), 1e-12);
                for (double t = 0; t <= 1.0; t += 0.05) {
                    double value = spring.interpolate(0.0, 1.0, t);
                    assertTrue(Double.isFinite(value),
                            "progress must be finite at " + dampingRatio + "/" + frequency);
                }
            }
        }
    }

    @Test
    void fractionOutsideUnitRangeIsClamped() {
        SpringInterpolator spring = new SpringInterpolator(0.70, 7.5);
        assertEquals(0.0, spring.interpolate(0.0, 1.0, -0.25), 1e-12, "t < 0 clamps to 0");
        assertEquals(1.0, spring.interpolate(0.0, 1.0, 1.5), 1e-12, "t > 1 clamps to 1");
    }

    @Test
    void criticallyDampedResponseIsMonotonic() {
        SpringInterpolator spring = new SpringInterpolator(1.0, 8.0);
        double previous = 0.0;
        for (double t = 0; t <= 1.0; t += 0.01) {
            double value = spring.interpolate(0.0, 1.0, t);
            assertTrue(value >= previous - 1e-12,
                    "critically damped spring must never backtrack at t=" + t);
            assertTrue(value <= 1.0 + 1e-12,
                    "critically damped spring must not overshoot at t=" + t);
            previous = value;
        }
    }

    @Test
    void mappingIsConsistentWithThePhysicalModel() {
        // The parameterization is (zeta, omega): two springs with the same zeta and proportional
        // omega move at the same shape, just faster in real time — normalized time compresses the
        // response, so a higher omega must reach the same progress earlier or equal.
        SpringInterpolator slow = new SpringInterpolator(1.0, 4.0);
        SpringInterpolator fast = new SpringInterpolator(1.0, 8.0);
        for (double t = 0; t <= 1.0; t += 0.05) {
            assertTrue(
                    fast.interpolate(0.0, 1.0, t) >= slow.interpolate(0.0, 1.0, t) - 1e-12,
                    "higher omega must not lag behind at t=" + t);
        }
    }
}
