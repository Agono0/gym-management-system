package io.m3fx.core.token;

import javafx.animation.Interpolator;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

/**
 * M3 motion durations and easing curves.
 *
 * <p>See <a href="https://m3.material.io/styles/motion/easing-and-duration/tokens-specs">M3 motion
 * easing and duration tokens</a> and {@code material-web/tokens/_md-sys-motion.scss} (v0.192 and
 * v34.0.21).
 *
 * <p>Reduced motion: {@link #reducedMotionProperty()} exposes the global accessibility flag
 * (WCAG 2.3.3; M3 applying-transitions guidance — see {@code docs/SPEC_NOTES/reduced-motion.md}).
 * When {@link #isReducedMotion()} is {@code true}, consumers must snap springs to their target
 * instead of interpolating, replace spatial/transform motion with opacity fades capped at
 * {@link #DURATION_REDUCED_FADE}, and keep instant feedback (state layers, focus rings)
 * unchanged. Pure Java cannot read the OS animation preference (Windows
 * {@code SPI_GETCLIENTAREAANIMATION}) without a native bridge, which the library forbids, so the
 * flag initializes from the {@code m3fx.reducedMotion} system property and apps wire their own
 * setting UI.
 *
 * <p>Motion scheme: {@link #motionSchemeProperty()} selects the spring language —
 * {@link MotionScheme#EXPRESSIVE} (default, bouncing {@code SPATIAL_EXPRESSIVE_*}) or
 * {@link MotionScheme#STANDARD} (settling {@code SPATIAL_STANDARD_*}); initialized from
 * {@code m3fx.motionScheme}. The animation layer maps its presets through the scheme (see
 * {@code docs/SPEC_NOTES/reduced-motion.md}, B7 section).
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
     * @deprecated Hand-tuned pre-token-grid values that diverge from the SpringToken grid; use
     *     {@code SPATIAL_EXPRESSIVE_DEFAULT.toInterpolator()} (the canonical A4 bridge) or
     *     {@code MotionUtil.spring()} in the controls layer.
     * @return the default spring interpolator
     */
    @Deprecated
    public static Interpolator spring() {
        return SpringInterpolator.DEFAULT;
    }

    /**
     * M3 Expressive bouncy spring interpolator (visible playful overshoot).
     *
     * @deprecated Diverges from the token grid; use
     *     {@code MotionTokens.SPATIAL_EXPRESSIVE_BOUNCY.toInterpolator()}.
     * @return the bouncy spring interpolator
     */
    @Deprecated
    public static Interpolator bouncySpring() {
        return SpringInterpolator.BOUNCY;
    }

    /**
     * M3 Expressive gentle spring interpolator (critically damped, no overshoot).
     *
     * @deprecated Duplicates the effects-language semantics; use
     *     {@code MotionTokens.EFFECTS_DEFAULT.toInterpolator()}.
     * @return the gentle spring interpolator
     */
    @Deprecated
    public static Interpolator gentleSpring() {
        return SpringInterpolator.GENTLE;
    }

    /**
     * Liquid glass fluid spring interpolator (smooth Apple-style liquid settling).
     *
     * @return the fluid spring interpolator
     */
    public static Interpolator fluidSpring() {
        return SpringInterpolator.FLUID;
    }

    /**
     * M3 spring physics token parameters (stiffness and damping ratio), modeled after Compose's
     * spring: stiffness is the spring constant k with unit mass, damping ratio is ζ.
     *
     * @param stiffness spring stiffness k (higher = faster response; must be positive)
     * @param dampingRatio damping ratio (1.0 = critically damped, &lt;1.0 = overshoot bounce)
     */
    public record SpringToken(double stiffness, double dampingRatio) {

        /**
         * Converts this token into the equivalent damped-oscillator interpolator: the canonical
         * bridge from the token grid to {@link SpringInterpolator}, using the same parameter
         * mapping androidx applies internally — unit mass makes the natural frequency
         * &omega; = &radic;(k/m) = &radic;k, and the damping coefficient follows from
         * &zeta; directly.
         *
         * @return the interpolator for this token
         * @throws IllegalArgumentException if {@code stiffness <= 0} (androidx: "Spring stiffness
         *     constant must be positive") or the damping ratio is not settling (&le; 0)
         */
        public SpringInterpolator toInterpolator() {
            if (stiffness <= 0) {
                throw new IllegalArgumentException("Spring stiffness constant must be positive.");
            }
            return new SpringInterpolator(dampingRatio, Math.sqrt(stiffness));
        }
    }

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

    // ------------------------------------------------------------------
    // Reduced motion.
    //
    // Global accessibility flag (WCAG 2.3.3 Animation from Interactions; M3
    // applying-transitions guidance). Consumption contract lives in the class
    // Javadoc and docs/SPEC_NOTES/reduced-motion.md; controls wire it in lane-B
    // task B7. Pure Java cannot read the OS animation preference without a
    // native bridge (forbidden), so the documented fallback is the
    // `m3fx.reducedMotion` system property + an app-level setting UI.
    // ------------------------------------------------------------------

    /**
     * Cap (150ms) for fade durations while reduced motion is active. // [VERIFY] library
     * convention aligned with the M3 DURATION_SHORT_3 step, not a verbatim spec number (see
     * {@code docs/SPEC_NOTES/reduced-motion.md}).
     */
    public static final int DURATION_REDUCED_FADE = 150;

    /**
     * The reduced-motion flag, initialized from the {@code m3fx.reducedMotion} system property.
     * Mutable like the rest of the token layer's state: read and write it on the JavaFX
     * Application Thread.
     */
    private static final ReadOnlyBooleanWrapper REDUCED_MOTION =
            new ReadOnlyBooleanWrapper(null, "reducedMotion", fromSystemProperty());

    /**
     * The observed reduced-motion flag. Consumers (skins) can attach listeners to react to
     * accessibility changes at runtime.
     *
     * @return the read-only reduced-motion property
     */
    public static ReadOnlyBooleanProperty reducedMotionProperty() {
        return REDUCED_MOTION.getReadOnlyProperty();
    }

    /**
     * Returns whether motion should be reduced. Semantics: springs snap to target, transitions
     * degrade to fades of at most {@link #DURATION_REDUCED_FADE}, instant feedback is unchanged.
     *
     * @return {@code true} when reduced motion is active
     */
    public static boolean isReducedMotion() {
        return REDUCED_MOTION.get();
    }

    /**
     * Sets the reduced-motion flag. Call on the JavaFX Application Thread — property listeners
     * fire synchronously on the calling thread.
     *
     * @param reducedMotion {@code true} to reduce motion
     */
    public static void setReducedMotion(boolean reducedMotion) {
        REDUCED_MOTION.set(reducedMotion);
    }

    /**
     * Reads the initial flag value. Only the value {@code true} (case-insensitive) enables it;
     * the property being absent or any other value disables it, matching
     * {@link Boolean#getBoolean(String)}.
     *
     * @return the initial reduced-motion flag from the {@code m3fx.reducedMotion} property
     */
    static boolean fromSystemProperty() {
        return Boolean.getBoolean("m3fx.reducedMotion");
    }

    // ------------------------------------------------------------------
    // Motion scheme.
    //
    // Expressive (default) vs standard spring language: which spatial presets
    // from the SpringToken grid the animation layer picks. Effects presets
    // (opacity/color) and the Apple-flavored set are scheme-agnostic. Same
    // pattern as the reduced-motion flag: system property initializes, apps
    // can switch at runtime via the setter, consumers read the property.
    // ------------------------------------------------------------------

    /** The observed motion scheme, initialized from {@code m3fx.motionScheme}. */
    private static final ReadOnlyObjectWrapper<MotionScheme> MOTION_SCHEME =
            new ReadOnlyObjectWrapper<>(null, "motionScheme", motionSchemeFromSystemProperty());

    /**
     * Returns the observed motion scheme. Expressive (default) plays the bouncing
     * {@code SPATIAL_EXPRESSIVE_*} presets; standard plays the settle-without-bounce
     * {@code SPATIAL_STANDARD_*} presets. Effects and Apple-flavored presets are unaffected.
     *
     * @return the active motion scheme, never {@code null}
     */
    public static MotionScheme getMotionScheme() {
        return MOTION_SCHEME.get();
    }

    /**
     * The observed motion scheme. Consumers can attach listeners to react to scheme switches.
     *
     * @return the read-only motion-scheme property
     */
    public static ReadOnlyObjectProperty<MotionScheme> motionSchemeProperty() {
        return MOTION_SCHEME.getReadOnlyProperty();
    }

    /**
     * Sets the motion scheme. Call on the JavaFX Application Thread — property listeners fire
     * synchronously on the calling thread. {@code null} is ignored so the scheme never reads
     * {@code null}.
     *
     * @param scheme the scheme to activate
     */
    public static void setMotionScheme(MotionScheme scheme) {
        if (scheme != null) {
            MOTION_SCHEME.set(scheme);
        }
    }

    /**
     * Reads the initial scheme from the {@code m3fx.motionScheme} property: {@code expressive}
     * (default) or {@code standard}, case-insensitive; the property being absent or any other
     * value keeps the expressive default.
     *
     * @return the initial motion scheme from the {@code m3fx.motionScheme} property
     */
    static MotionScheme motionSchemeFromSystemProperty() {
        String value = System.getProperty("m3fx.motionScheme");
        return "standard".equalsIgnoreCase(value) ? MotionScheme.STANDARD : MotionScheme.EXPRESSIVE;
    }

    /** The motion language the animation layer plays. */
    public enum MotionScheme {
        /** M3 Expressive: under-damped spatial springs with natural overshoot (default). */
        EXPRESSIVE,
        /** M3 standard: settle without bounce — spatial springs use the standard grid. */
        STANDARD
    }
}

