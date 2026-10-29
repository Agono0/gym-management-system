package io.m3fx.controls.internal;

import io.m3fx.core.token.MotionTokens;
import io.m3fx.core.token.MotionTokens.SpringToken;
import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Node;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Real M3 Expressive spring physics: a mass-spring-damper integrator driving any
 * {@link DoubleProperty} (translation, scale, rotation, opacity).
 *
 * <p>Unlike duration-and-bezier tweens, this integrates {@code a = -k(x - target) - c·v} every frame.
 * Durations naturally emerge from physical stiffness and damping ratio. Preserves velocity when
 * interrupted so rapid taps or gesture retargets maintain continuous physical momentum.
 */
public final class SpringAnimation {

    /** Spring tuning: stiffness (mass = 1) plus damping ratio (1 = critically damped, &lt;1 = bounce). */
    public record SpringParams(double stiffness, double dampingRatio) {
        /** Creates SpringParams from an M3 {@link SpringToken}. */
        public static SpringParams fromToken(SpringToken token) {
            return new SpringParams(token.stiffness(), token.dampingRatio());
        }
    }

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

    /** Apple-style soft settle: damping 0.88 — arrives gently with no visible overshoot. */
    public static final SpringParams APPLE_SETTLE = SpringParams.fromToken(MotionTokens.APPLE_SETTLE);

    /** Apple-style playful: damping 0.65 — slight overshoot, tab-bar highlight morph energy. */
    public static final SpringParams APPLE_PLAYFUL = SpringParams.fromToken(MotionTokens.APPLE_PLAYFUL);

    private static final Map<DoubleProperty, AnimationTimer> ACTIVE = new WeakHashMap<>();
    private static final Map<DoubleProperty, Double> VELOCITIES = new WeakHashMap<>();

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
     * if interrupted mid-flight.
     *
     * @param property the property to drive (must be written on the FX thread)
     * @param toValue rest value
     * @param params spring tuning
     * @param onFinished optional callback, may be {@code null}
     */
    public static void animate(
            DoubleProperty property, double toValue, SpringParams params, Runnable onFinished) {
        SpringParams p = params == null ? DEFAULT : params;
        // Degenerate springs (zero/negative stiffness or non-finite inputs) would never settle:
        // snap to the target instead of running an infinite AnimationTimer.
        if (p.stiffness() <= 0
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
        AnimationTimer previous = ACTIVE.remove(property);
        if (previous != null) {
            previous.stop();
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
        ACTIVE.put(property, timer);
        timer.start();
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
     * Stops any running spring animation on the property.
     *
     * @param property the property to halt
     */
    public static void stop(DoubleProperty property) {
        AnimationTimer timer = ACTIVE.remove(property);
        if (timer != null) {
            timer.stop();
        }
        VELOCITIES.remove(property);
    }
}
