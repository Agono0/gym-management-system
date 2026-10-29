package io.m3fx.controls.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.beans.property.SimpleDoubleProperty;
import org.junit.jupiter.api.Test;

/**
 * Verifies the spring integrator converges and that underdamped tunings overshoot while
 * critically damped ones do not (pure math — no FX toolkit needed).
 */
class SpringAnimationTest {

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
    void degenerateSpringSnapsWithoutAnimating() {
        // Zero stiffness can never settle; the animation must snap to the target instead of
        // running an infinite timer. The snap path is synchronous (no AnimationTimer started).
        SimpleDoubleProperty property = new SimpleDoubleProperty(5);
        SpringAnimation.animate(property, 42, new SpringAnimation.SpringParams(0, 0.5), null);
        assertEquals(42, property.get(), 1e-9);
    }

    @Test
    void stepIsFiniteForLargeTimestep() {
        // A clamped-out frame (tab-back) must not poison the state with NaN/Infinite.
        double[] state = SpringAnimation.step(0, 0, 100, 380, 0.65, 1.0);
        assertTrue(Double.isFinite(state[0]));
        assertTrue(Double.isFinite(state[1]));
    }
}
