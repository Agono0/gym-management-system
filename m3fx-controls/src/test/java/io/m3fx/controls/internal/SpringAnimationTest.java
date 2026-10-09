package io.m3fx.controls.internal;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
