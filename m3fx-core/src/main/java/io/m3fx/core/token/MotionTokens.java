package io.m3fx.core.token;

import javafx.animation.Interpolator;

/**
 * M3 motion durations and easing curves.
 *
 * <p>See <a href="https://m3.material.io/styles/motion/easing-and-duration/tokens-specs">M3 motion
 * easing and duration tokens</a> and {@code material-web/tokens/_md-sys-motion.scss} (v0.192 and
 * v34.0.21).
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
    /** Extra-long 2 (800ms): full-screen transitions. */
    public static final int DURATION_EXTRA_LONG_2 = 800;
    /** Extra-long 3 (900ms): full-screen transitions. */
    public static final int DURATION_EXTRA_LONG_3 = 900;
    /** Extra-long 4 (1000ms): full-screen transitions. */
    public static final int DURATION_EXTRA_LONG_4 = 1000;

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
     * M3 standard-accelerate easing {@code cubic-bezier(0.3, 0, 1, 1)} for exiting elements.
     *
     * @return the standard accelerate interpolator
     */
    public static Interpolator standardAccelerate() {
        return Interpolator.SPLINE(0.3, 0, 1, 1);
    }

    /**
     * M3 standard-decelerate easing {@code cubic-bezier(0, 0, 0, 1)} for entering elements.
     *
     * @return the standard decelerate interpolator
     */
    public static Interpolator standardDecelerate() {
        return Interpolator.SPLINE(0, 0, 0, 1);
    }

    /**
     * M3 emphasized easing {@code cubic-bezier(0.2, 0, 0, 1)}. The token pipeline defines
     * emphasized as the same curve as standard; the expressive shape comes from pairing it with
     * {@link #emphasizedAccelerate()} (exits) and {@link #emphasizedDecelerate()} (entrances).
     *
     * @return the emphasized easing interpolator
     */
    public static Interpolator emphasized() {
        return Interpolator.SPLINE(0.2, 0, 0, 1);
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
     * M3 emphasized-decelerate easing {@code cubic-bezier(0.05, 0.7, 0.1, 1)}.
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

    /**
     * M3 spring physics token parameters (stiffness and damping ratio).
     *
     * @param stiffness spring stiffness (higher = faster response)
     * @param dampingRatio damping ratio (1.0 = critically damped, &lt;1.0 = overshoot bounce)
     */
    public record SpringToken(double stiffness, double dampingRatio) {}

    /** Spatial Expressive Default: mild natural overshoot (stiffness 380, damping 0.65). */
    public static final SpringToken SPATIAL_EXPRESSIVE_DEFAULT = new SpringToken(380, 0.65);
    /** Spatial Expressive Bouncy: energetic playful overshoot (stiffness 500, damping 0.45). */
    public static final SpringToken SPATIAL_EXPRESSIVE_BOUNCY = new SpringToken(500, 0.45);
    /** Spatial Expressive Fast: quick micro-interaction (stiffness 800, damping 0.70). */
    public static final SpringToken SPATIAL_EXPRESSIVE_FAST = new SpringToken(800, 0.70);
    /** Spatial Expressive Slow: expansive hero motion (stiffness 200, damping 0.60). */
    public static final SpringToken SPATIAL_EXPRESSIVE_SLOW = new SpringToken(200, 0.60);

    /** Spatial Standard Default: smooth settling without bounce (stiffness 350, damping 0.90). */
    public static final SpringToken SPATIAL_STANDARD_DEFAULT = new SpringToken(350, 0.90);
    /** Spatial Standard Fast: quick utilitarian settling (stiffness 700, damping 0.95). */
    public static final SpringToken SPATIAL_STANDARD_FAST = new SpringToken(700, 0.95);
    /** Spatial Standard Slow: large standard motion (stiffness 200, damping 0.90). */
    public static final SpringToken SPATIAL_STANDARD_SLOW = new SpringToken(200, 0.90);

    /** Effects Default: critically damped for opacity/fades (stiffness 300, damping 1.00). */
    public static final SpringToken EFFECTS_DEFAULT = new SpringToken(300, 1.00);
    /** Effects Fast: fast fade/color transition (stiffness 600, damping 1.00). */
    public static final SpringToken EFFECTS_FAST = new SpringToken(600, 1.00);
    /** Effects Slow: gradual fade transition (stiffness 150, damping 1.00). */
    public static final SpringToken EFFECTS_SLOW = new SpringToken(150, 1.00);

    // ------------------------------------------------------------------
    // Apple-flavored fluid-motion tokens.
    //
    // NOT M3 spec values. These are hand-tuned to match the feel of Apple's
    // "fluid interface" motion (iOS sheet presentation, tab-bar highlights):
    // long decelerating tails, no snappy linear segments, springs that settle
    // without visible bounce. They deliberately coexist with the M3 tokens
    // above so consumers can pick a motion language per transition.
    // ------------------------------------------------------------------

    /** Apple sheet/push presentation duration (450ms): page-level container transitions. */
    public static final int APPLE_SHEET_DURATION = 450;
    /** Apple page-enter duration (400ms): fade + rise + settle for entering content. */
    public static final int APPLE_TRANSITION_DURATION = 400;
    /** Apple gentle emphasized duration (350ms): soft medium transitions. */
    public static final int APPLE_GENTLE_DURATION = 350;
    /** Apple quick fade duration (150ms): exits and micro crossfades. */
    public static final int APPLE_FADE_DURATION = 150;
    /** Apple splash reveal duration (600ms): app-launch fade + scale settle into the first container. */
    public static final int APPLE_SPLASH_DURATION = 600;

    /**
     * Apple sheet ease {@code cubic-bezier(0.32, 0.72, 0, 1)}: the iOS sheet/modal
     * presentation curve. Fast, confident start with a long, almost imperceptible
     * deceleration tail — never linear, never snappy at the end.
     *
     * @return the Apple sheet easing interpolator
     */
    public static Interpolator appleSheetEase() {
        return Interpolator.SPLINE(0.32, 0.72, 0, 1);
    }

    /**
     * Apple gentle emphasized ease {@code cubic-bezier(0.25, 0.1, 0.25, 1)}: the classic
     * smooth ease-in-out used for medium transitions where both ends must feel cushioned.
     *
     * @return the Apple gentle easing interpolator
     */
    public static Interpolator appleGentleEase() {
        return Interpolator.SPLINE(0.25, 0.1, 0.25, 1);
    }

    /**
     * Apple quick-fade ease {@code cubic-bezier(0.42, 0, 0.58, 1)}: a symmetric
     * ease-in-out so even short 150ms fades never start or stop abruptly.
     *
     * @return the Apple fade easing interpolator
     */
    public static Interpolator appleFadeEase() {
        return Interpolator.SPLINE(0.42, 0, 0.58, 1);
    }

    /** Apple-style soft settle spring: no visible overshoot, gentle arrival (stiffness 320, damping 0.88). */
    public static final SpringToken APPLE_SETTLE = new SpringToken(320, 0.88);
    /** Apple-style playful spring: slight overshoot, tab-bar morph energy (stiffness 380, damping 0.65). */
    public static final SpringToken APPLE_PLAYFUL = new SpringToken(380, 0.65);
}

