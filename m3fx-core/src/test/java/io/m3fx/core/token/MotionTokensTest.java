package io.m3fx.core.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.animation.Interpolator;
import org.junit.jupiter.api.Test;

/**
 * Locks the M3 motion token values: the 16 duration steps (50..1000ms) and the easing curves.
 *
 * <p>Easing curves are sampled through {@link Interpolator#interpolate(double, double, double)}.
 * For the M3 standard/emphasized curve cubic-bezier(0.2, 0, 0, 1) the progress at input fraction
 * 0.2 is exactly 0.5; the accelerate curves land far below it and the decelerate curves far above.
 * Sampling keeps the assertions independent of spline solver internals.
 */
class MotionTokensTest {

    @Test
    void durationTokensMatchSpecSteps() {
        // short1-4, medium1-4, long1-4, extra-long1-4 per the M3 motion tokens.
        assertEquals(50, MotionTokens.DURATION_SHORT_1);
        assertEquals(100, MotionTokens.DURATION_SHORT_2);
        assertEquals(150, MotionTokens.DURATION_SHORT_3);
        assertEquals(200, MotionTokens.DURATION_SHORT_4);
        assertEquals(250, MotionTokens.DURATION_MEDIUM_1);
        assertEquals(300, MotionTokens.DURATION_MEDIUM_2);
        assertEquals(350, MotionTokens.DURATION_MEDIUM_3);
        assertEquals(400, MotionTokens.DURATION_MEDIUM_4);
        assertEquals(450, MotionTokens.DURATION_LONG_1);
        assertEquals(500, MotionTokens.DURATION_LONG_2);
        assertEquals(550, MotionTokens.DURATION_LONG_3);
        assertEquals(600, MotionTokens.DURATION_LONG_4);
        assertEquals(700, MotionTokens.DURATION_EXTRA_LONG_1);
        assertEquals(800, MotionTokens.DURATION_EXTRA_LONG_2);
        assertEquals(900, MotionTokens.DURATION_EXTRA_LONG_3);
        assertEquals(1000, MotionTokens.DURATION_EXTRA_LONG_4);
    }

    @Test
    void appleSplashDurationTokenValue() {
        // Apple-flavored (non-M3) launch reveal duration consumed by MotionUtil.splashIn.
        assertEquals(600, MotionTokens.APPLE_SPLASH_DURATION);
    }

    @Test
    void standardAndEmphasizedUseCubicBezier02001() {
        // cubic-bezier(0.2, 0, 0, 1): x(0.5)=0.2, y(0.5)=0.5 -> progress 0.5 at fraction 0.2.
        assertEquals(0.5, MotionTokens.standard().interpolate(0.0, 1.0, 0.2), 0.02);
        assertEquals(
                MotionTokens.standard().interpolate(0.0, 1.0, 0.2),
                MotionTokens.emphasized().interpolate(0.0, 1.0, 0.2),
                0.001,
                "the token pipeline defines emphasized as the same curve as standard");
    }

    @Test
    void accelerateAndDecelerateDifferFromStandard() {
        // Progress at input fraction 0.2: standard-accelerate (0.3, 0, 1, 1) ~0.09,
        // standard-decelerate (0, 0, 0, 1) ~0.63, emphasized-accelerate (0.3, 0, 0.8, 0.15)
        // ~0.02, emphasized-decelerate (0.05, 0.7, 0.1, 1) ~0.78.
        assertTrue(
                MotionTokens.standardAccelerate().interpolate(0.0, 1.0, 0.2) < 0.3,
                "standard-accelerate should lag well behind standard at 0.2");
        assertTrue(
                MotionTokens.standardDecelerate().interpolate(0.0, 1.0, 0.2) > 0.55,
                "standard-decelerate should run well ahead of standard at 0.2");
        assertTrue(
                MotionTokens.emphasizedAccelerate().interpolate(0.0, 1.0, 0.2) < 0.3,
                "emphasized-accelerate should lag well behind standard at 0.2");
        assertTrue(
                MotionTokens.emphasizedDecelerate().interpolate(0.0, 1.0, 0.2) > 0.55,
                "emphasized-decelerate should run well ahead of standard at 0.2");
    }
}
