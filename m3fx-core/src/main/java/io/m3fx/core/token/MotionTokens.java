package io.m3fx.core.token;

import javafx.animation.Interpolator;

/**
 * M3 motion durations and easing curves.
 *
 * <p>See <a href="https://m3.material.io/styles/motion/overview">M3 motion spec</a> and
 * {@code material-web/tokens/_md-sys-motion.scss} (v0.192).
 */
public final class MotionTokens {

    private MotionTokens() {}

    /** Short duration 1 (50ms): micro-utility (fade). */
    public static final int DURATION_SHORT_1 = 50;
    /** Short duration 2 (100ms). */
    public static final int DURATION_SHORT_2 = 100;
    /** Short duration 3 (150ms): small expands. */
    public static final int DURATION_SHORT_3 = 150;
    /** Short duration 4 (200ms): small expands. */
    public static final int DURATION_SHORT_4 = 200;
    /** Medium duration 1 (250ms). */
    public static final int DURATION_MEDIUM_1 = 250;
    /** Medium duration 2 (300ms): medium expands. */
    public static final int DURATION_MEDIUM_2 = 300;
    /** Medium duration 3 (350ms). */
    public static final int DURATION_MEDIUM_3 = 350;
    /** Medium duration 4 (400ms): medium expands. */
    public static final int DURATION_MEDIUM_4 = 400;
    /** Long duration 1 (450ms): large expands. */
    public static final int DURATION_LONG_1 = 450;
    /** Long duration 2 (500ms). */
    public static final int DURATION_LONG_2 = 500;
    /** Long duration 3 (550ms). */
    public static final int DURATION_LONG_3 = 550;
    /** Long duration 4 (600ms). */
    public static final int DURATION_LONG_4 = 600;
    /** Extra-long 1 (700ms): large expands. */
    public static final int DURATION_EXTRA_LONG_1 = 700;

    /**
     * M3 standard easing {@code cubic-bezier(0.2, 0, 0, 1)} approximated with a spline
     * interpolator.
     *
     * @return the standard easing interpolator
     */
    public static Interpolator standard() {
        return Interpolator.SPLINE(0.2, 0, 0, 1);
    }

    /**
     * M3 emphasized easing {@code cubic-bezier(0.05, 0.7, 0.1, 1)} approximated with a spline
     * interpolator.
     *
     * @return the emphasized easing interpolator
     */
    public static Interpolator emphasized() {
        return Interpolator.SPLINE(0.05, 0.7, 0.1, 1);
    }

    /**
     * M3 emphasized-accelerate easing {@code cubic-bezier(0.3, 0, 0.8, 0.15)}.
     *
     * @return the accelerate interpolator
     */
    public static Interpolator emphasizedAccelerate() {
        return Interpolator.SPLINE(0.3, 0, 0.8, 0.15);
    }

    /**
     * M3 emphasized-decelerate easing {@code cubic-bezier(0.05, 0.7, 0.1, 1)} variant for exits.
     *
     * @return the decelerate interpolator
     */
    public static Interpolator emphasizedDecelerate() {
        return Interpolator.SPLINE(0.05, 0.7, 0.1, 1);
    }

    /**
     * M3 Expressive spring interpolator (mild natural overshoot, settles smoothly).
     *
     * @return the default spring interpolator
     */
    public static Interpolator spring() {
        return SpringInterpolator.DEFAULT;
    }

    /**
     * M3 Expressive bouncy spring interpolator (visible playful overshoot).
     *
     * @return the bouncy spring interpolator
     */
    public static Interpolator bouncySpring() {
        return SpringInterpolator.BOUNCY;
    }

    /**
     * M3 Expressive gentle spring interpolator (critically damped, no overshoot).
     *
     * @return the gentle spring interpolator
     */
    public static Interpolator gentleSpring() {
        return SpringInterpolator.GENTLE;
    }
}

