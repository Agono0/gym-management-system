package io.m3fx.core.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.animation.Interpolator;
import org.junit.jupiter.api.Test;

/**
 * Locks the physics of the {@link MotionTokens.SpringToken#toInterpolator()} bridge: underdamped
 * spatial tokens overshoot the target during the settle, critically damped effects tokens never
 * do, every converted interpolator converges to the target by t = 1, damping monotonically tames
 * the overshoot, and the conversion stays stable across the whole parameter grid.
 *
 * <p>All assertions sample through the public {@code interpolate(0, 1, t)} (house pattern, see
 * {@link MotionTokensTest}); the omega = sqrt(stiffness) mapping is copied verbatim from androidx
 * {@code SpringSimulation} — see {@code docs/SPEC_NOTES/spring-bridge.md}.
 */
class SpringTokenConversionTest {

    /** Sampling resolution for overshoot detection (peak width ~0.1 in normalized time). */
    private static final int SAMPLES = 2000;

    private static MotionTokens.SpringToken[] spatialTokens() {
        return new MotionTokens.SpringToken[] {
            MotionTokens.SPATIAL_EXPRESSIVE_DEFAULT,
            MotionTokens.SPATIAL_EXPRESSIVE_BOUNCY,
            MotionTokens.SPATIAL_EXPRESSIVE_FAST,
            MotionTokens.SPATIAL_EXPRESSIVE_SLOW,
            MotionTokens.SPATIAL_STANDARD_DEFAULT,
            MotionTokens.SPATIAL_STANDARD_FAST,
            MotionTokens.SPATIAL_STANDARD_SLOW,
        };
    }

    /** Returns the maximum progress over the sampled normalized time. */
    private static double maxProgress(Interpolator interpolator) {
        double max = 0;
        for (int i = 0; i <= SAMPLES; i++) {
            max = Math.max(max, interpolator.interpolate(0.0, 1.0, (double) i / SAMPLES));
        }
        return max;
    }

    @Test
    void spatialTokensOvershootTargetDuringSettle() {
        for (MotionTokens.SpringToken token : spatialTokens()) {
            assertTrue(token.dampingRatio() < 1.0, token + " is not underdamped");
            assertTrue(
                    maxProgress(token.toInterpolator()) > 1.0,
                    token + " must overshoot the target");
        }
    }

    @Test
    void effectsTokensNeverOvershoot() {
        for (MotionTokens.SpringToken token : new MotionTokens.SpringToken[] {
            MotionTokens.EFFECTS_DEFAULT, MotionTokens.EFFECTS_FAST, MotionTokens.EFFECTS_SLOW
        }) {
            assertEquals(1.0, token.dampingRatio(), token + " is not critically damped");
            assertTrue(
                    maxProgress(token.toInterpolator()) <= 1.0,
                    token + " must never overshoot");
        }
    }

    @Test
    void convertedInterpolatorConvergesToTarget() {
        for (MotionTokens.SpringToken token : spatialTokens()) {
            Interpolator interpolator = token.toInterpolator();
            assertEquals(1.0, interpolator.interpolate(0.0, 1.0, 1.0), 1e-12,
                    token + " must land exactly on the target");
            assertTrue(
                    Math.abs(interpolator.interpolate(0.0, 1.0, 0.999) - 1.0) < 0.01,
                    token + " must have settled to <1% from the target by t=0.999");
        }
    }

    @Test
    void overshootDecreasesWithDampingAtFixedStiffness() {
        double previous = Double.POSITIVE_INFINITY;
        for (double dampingRatio : new double[] {0.45, 0.65, 0.8, 1.0}) {
            MotionTokens.SpringToken token = new MotionTokens.SpringToken(500, dampingRatio);
            double overshoot = maxProgress(token.toInterpolator());
            assertTrue(
                    overshoot <= previous,
                    "overshoot must not grow with damping at " + dampingRatio);
            previous = overshoot;
        }
        // The final token is critically damped: settled, no overshoot at all.
        assertTrue(previous <= 1.0, "critically damped spring must not overshoot");
    }

    @Test
    void conversionIsStableAcrossTheGrid() {
        double[] stiffnesses = {10, 50, 100, 200, 380, 500, 800, 10000};
        double[] dampingRatios = {0.3, 0.5, 0.65, 0.8, 0.9, 1.0};
        for (double stiffness : stiffnesses) {
            for (double dampingRatio : dampingRatios) {
                Interpolator interpolator =
                        new MotionTokens.SpringToken(stiffness, dampingRatio).toInterpolator();
                assertEquals(0.0, interpolator.interpolate(0.0, 1.0, 0.0), 1e-12);
                assertEquals(1.0, interpolator.interpolate(0.0, 1.0, 1.0), 1e-12);
                double settled = Math.abs(interpolator.interpolate(0.0, 1.0, 0.999) - 1.0);
                assertTrue(
                        settled < 0.01,
                        "k=" + stiffness + " zeta=" + dampingRatio + " settled " + settled);
            }
        }
    }

    @Test
    void rejectsNonPositiveStiffnessAndNonSettlingDamping() {
        // androidx: "Spring stiffness constant must be positive."
        assertThrows(IllegalArgumentException.class,
                () -> new MotionTokens.SpringToken(0, 0.65).toInterpolator());
        assertThrows(IllegalArgumentException.class,
                () -> new MotionTokens.SpringToken(-100, 0.65).toInterpolator());
        // The interpolator cannot render a spring that never settles.
        assertThrows(IllegalArgumentException.class,
                () -> new MotionTokens.SpringToken(380, 0).toInterpolator());
    }
}
