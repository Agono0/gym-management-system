package io.m3fx.controls.internal;

import io.m3fx.controls.common.SpringParams;
import io.m3fx.core.token.MotionTokens;
import io.m3fx.core.token.MotionTokens.MotionScheme;
import io.m3fx.core.token.MotionTokens.SpringToken;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Node;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Real M3 Expressive spring physics: a mass-spring-damper integrator driving any
 * {@link DoubleProperty} (translation, scale, rotation, opacity).
 *
 * <p>Unlike duration-and-bezier tweens, this integrates {@code a = -k(x - target) - c·v} every frame.
 * Durations naturally emerge from physical stiffness and damping ratio. Preserves velocity when
 * interrupted so rapid taps or gesture retargets maintain continuous physical momentum.
 *
 * <p>Scheme-aware: the named spatial presets ({@link #DEFAULT}, {@link #BOUNCY}, {@link #FAST},
 * {@link #SLOW}) are the Expressive grid; under {@link MotionScheme#STANDARD} they resolve to the
 * settle-without-bounce {@code SPATIAL_STANDARD_*} presets inside {@link #animate} (custom
 * tunings, effects presets and the Apple-flavored set pass through unchanged). Reduced motion
 * snaps every animation to its target immediately — and a running animation snaps live when the
 * {@code MotionTokens.reducedMotionProperty()} flag flips on.
 */
public final class SpringAnimation {

    /** Default spatial spring: M3 Expressive standard overshoot, settles fast. */
    public static final SpringParams DEFAULT =
            new SpringParams(MotionTokens.SPATIAL_EXPRESSIVE_DEFAULT.stiffness(),
                    MotionTokens.SPATIAL_EXPRESSIVE_DEFAULT.dampingRatio());

    /** Gentle spring: critically damped for fades and subtle standard motion. */
    public static final SpringParams GENTLE =
            new SpringParams(MotionTokens.EFFECTS_DEFAULT.stiffness(),
                    MotionTokens.EFFECTS_DEFAULT.dampingRatio());

    /** Bouncy spring: visible playful overshoot for high-energy interactions. */
    public static final SpringParams BOUNCY =
            new SpringParams(MotionTokens.SPATIAL_EXPRESSIVE_BOUNCY.stiffness(),
                    MotionTokens.SPATIAL_EXPRESSIVE_BOUNCY.dampingRatio());

    /** Fast spring: rapid micro-interactions and quick snaps. */
    public static final SpringParams FAST =
            new SpringParams(MotionTokens.SPATIAL_EXPRESSIVE_FAST.stiffness(),
                    MotionTokens.SPATIAL_EXPRESSIVE_FAST.dampingRatio());

    /** Slow spring: expansive hero container transitions. */
    public static final SpringParams SLOW =
            new SpringParams(MotionTokens.SPATIAL_EXPRESSIVE_SLOW.stiffness(),
                    MotionTokens.SPATIAL_EXPRESSIVE_SLOW.dampingRatio());

    /** Standard-scheme spatial presets (the {@code SPATIAL_STANDARD_*} grid row). */
    static final SpringParams STANDARD_DEFAULT =
            SpringParams.fromToken(MotionTokens.SPATIAL_STANDARD_DEFAULT);
    static final SpringParams STANDARD_FAST =
            SpringParams.fromToken(MotionTokens.SPATIAL_STANDARD_FAST);
    static final SpringParams STANDARD_SLOW =
            SpringParams.fromToken(MotionTokens.SPATIAL_STANDARD_SLOW);

    /** Apple-style soft settle: damping 0.88 — arrives gently with no visible overshoot. */
    public static final SpringParams APPLE_SETTLE = SpringParams.fromToken(MotionTokens.APPLE_SETTLE);

    /** Apple-style playful: damping 0.65 — slight overshoot, tab-bar highlight morph energy. */
    public static final SpringParams APPLE_PLAYFUL = SpringParams.fromToken(MotionTokens.APPLE_PLAYFUL);

    /** One running spring animation: its timer, target and completion callback. */
    private record ActiveSpring(AnimationTimer timer, double target, Runnable onFinished) {}

    private static final Map<DoubleProperty, ActiveSpring> ACTIVE = new WeakHashMap<>();
    private static final Map<DoubleProperty, Double> VELOCITIES = new WeakHashMap<>();

    static {
        // Reduced motion is a live accessibility switch: flipping it on snaps every running
        // spring to its target (and fires the completion callbacks so chained skin state stays
        // consistent). Listeners fire on the thread that flips the flag — the FX thread.
        MotionTokens.reducedMotionProperty().addListener((obs, was, reduced) -> {
            if (reduced) {
                for (Map.Entry<DoubleProperty, ActiveSpring> entry :
                        List.copyOf(ACTIVE.entrySet())) {
                    ActiveSpring active = entry.getValue();
                    ACTIVE.remove(entry.getKey());
                    VELOCITIES.remove(entry.getKey());
                    active.timer().stop();
                    entry.getKey().set(active.target());
                    if (active.onFinished() != null) {
                        active.onFinished().run();
                    }
                }
            }
        });
    }

    private SpringAnimation() {}

    /**
     * One semi-implicit Euler integration step (pure function — no FX toolkit needed).
     *
     * @param x current value
     * @param v current velocity (units/second)
     * @param target rest value
     * @param stiffness spring stiffness
     * @param dampingRatio damping ratio
     * @param dt timestep in seconds
     * @return {@code {newX, newV}}
     */
    public static double[] step(
            double x, double v, double target, double stiffness, double dampingRatio, double dt) {
        double damping = 2 * dampingRatio * Math.sqrt(stiffness);
        double accel = -stiffness * (x - target) - damping * v;
        double newV = v + accel * dt;
        double newX = x + newV * dt;
        return new double[] {newX, newV};
    }

    /**
     * Animates a property toward a target with spring physics, preserving current velocity
     * if interrupted mid-flight. The preset resolves through the active
     * {@link MotionScheme#STANDARD motion scheme}; with reduced motion on, the property snaps to
     * the target synchronously (no timer starts).
     *
     * @param property the property to drive (must be written on the FX thread)
     * @param toValue rest value
     * @param params spring tuning
     * @param onFinished optional callback, may be {@code null}
     */
    public static void animate(
            DoubleProperty property, double toValue, SpringParams params, Runnable onFinished) {
        requireFxThread("animate");
        SpringParams p = resolveForScheme(params == null ? DEFAULT : params);
        // Reduced motion snaps like the degenerate-spring guard below: no timer ever starts.
        if (MotionTokens.isReducedMotion()
                || p.stiffness() <= 0
                || !Double.isFinite(p.stiffness())
                || !Double.isFinite(p.dampingRatio())
                || !Double.isFinite(toValue)
                || !Double.isFinite(property.get())) {
            stop(property);
            if (Double.isFinite(toValue)) {
                property.set(toValue);
            }
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }
        ActiveSpring previous = ACTIVE.remove(property);
        if (previous != null) {
            previous.timer().stop();
        }

        // Preserve velocity from interrupted animation if available
        double startV = VELOCITIES.getOrDefault(property, 0.0);
        double epsilon = Math.max(1e-4, Math.abs(toValue) * 5e-4);

        AnimationTimer timer =
                new AnimationTimer() {
                    private long last = -1;
                    private double x = property.get();
                    private double v = startV;

                    @Override
                    public void handle(long now) {
                        if (last < 0) {
                            last = now;
                            return;
                        }
                        double dt = Math.min((now - last) / 1_000_000_000.0, 1 / 30.0);
                        last = now;
                        double[] state = step(x, v, toValue, p.stiffness(), p.dampingRatio(), dt);
                        x = state[0];
                        v = state[1];
                        VELOCITIES.put(property, v);

                        if (Math.abs(x - toValue) < epsilon && Math.abs(v) < epsilon * 60) {
                            property.set(toValue);
                            stop();
                            ACTIVE.remove(property);
                            VELOCITIES.remove(property);
                            if (onFinished != null) {
                                onFinished.run();
                            }
                            return;
                        }
                        property.set(x);
                    }
                };
        ACTIVE.put(property, new ActiveSpring(timer, toValue, onFinished));
        timer.start();
    }

    /**
     * Resolves a caller-chosen preset through the active motion scheme: the expressive spatial
     * presets map to their {@code SPATIAL_STANDARD_*} counterparts under
     * {@link MotionScheme#STANDARD} (BOUNCY/DEFAULT to the standard default, FAST to the standard
     * fast, SLOW to the standard slow); effects and Apple presets and custom tunings pass
     * through. Identity-based — pass the named constants.
     *
     * @param params the requested tuning (never null)
     * @return the tuning to animate with
     */
    static SpringParams resolveForScheme(SpringParams params) {
        if (MotionTokens.getMotionScheme() == MotionScheme.STANDARD) {
            if (params == DEFAULT || params == BOUNCY) {
                return STANDARD_DEFAULT;
            }
            if (params == FAST) {
                return STANDARD_FAST;
            }
            if (params == SLOW) {
                return STANDARD_SLOW;
            }
        }
        return params;
    }

    /** Returns whether a spring animation is currently running on the property. */
    static boolean isAnimating(DoubleProperty property) {
        return ACTIVE.containsKey(property);
    }

    /**
     * Animates a property using an M3 {@link SpringToken}.
     *
     * @param property the property to drive
     * @param toValue rest value
     * @param token spring token
     * @param onFinished optional callback, may be {@code null}
     */
    public static void animate(
            DoubleProperty property, double toValue, SpringToken token, Runnable onFinished) {
        animate(property, toValue, SpringParams.fromToken(token), onFinished);
    }

    /**
     * Animates a property with {@link #DEFAULT} tuning.
     *
     * @param property the property to drive
     * @param toValue rest value
     */
    public static void animate(DoubleProperty property, double toValue) {
        animate(property, toValue, DEFAULT, null);
    }

    /**
     * Animates a Node's scale uniformly with spring physics.
     *
     * @param node the node to scale
     * @param targetScale the target scale (1.0 = normal)
     * @param params spring tuning
     */
    public static void animateScale(Node node, double targetScale, SpringParams params) {
        animate(node.scaleXProperty(), targetScale, params, null);
        animate(node.scaleYProperty(), targetScale, params, null);
    }

    /**
     * Animates a Node's translation (transform-only, layout-safe) to the given offsets with
     * spring physics. Both axes are driven by independent springs sharing one tuning, so a
     * retarget mid-flight preserves velocity on each axis separately.
     *
     * @param node the node to translate
     * @param targetX the target translateX in px
     * @param targetY the target translateY in px
     * @param params spring tuning
     */
    public static void animateTranslation(Node node, double targetX, double targetY, SpringParams params) {
        animate(node.translateXProperty(), targetX, params, null);
        animate(node.translateYProperty(), targetY, params, null);
    }

    /**
     * Immediately stops any spring animation on a node's translation and scale properties.
     *
     * @param node the node whose transform springs should halt
     */
    public static void stopTransforms(Node node) {
        stop(node.translateXProperty());
        stop(node.translateYProperty());
        stop(node.scaleXProperty());
        stop(node.scaleYProperty());
    }

    /**
     * Stops any running spring animation on the property (without snapping to its target).
     *
     * @param property the property to halt
     */
    public static void stop(DoubleProperty property) {
        requireFxThread("stop");
        ActiveSpring active = ACTIVE.remove(property);
        if (active != null) {
            active.timer().stop();
        }
        VELOCITIES.remove(property);
    }

    private static void requireFxThread(String operation) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException(
                    "SpringAnimation." + operation
                    + " must be called on the JavaFX Application Thread");
        }
    }
}
