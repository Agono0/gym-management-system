package io.m3fx.controls.internal;

import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Real M3 Expressive spring physics: a mass-spring-damper integrator driving any
 * {@link DoubleProperty} (translation, scale, rotation, opacity).
 *
 * <p>Unlike the damped-overshoot {@link MotionUtil#spring()} interpolator (a fixed curve), this
 * integrates {@code a = -k(x - target) - c·v} every frame, so duration emerges from stiffness and
 * damping ratio instead of being hardcoded. Presets follow the M3 expressive spring feel.
 */
public final class SpringAnimation {

    /** Spring tuning: stiffness (mass = 1) plus damping ratio (1 = critically damped). */
    public record SpringParams(double stiffness, double dampingRatio) {}

    /** Default spatial spring: mild overshoot, settles fast. */
    public static final SpringParams DEFAULT = new SpringParams(300, 0.8);
    /** Gentle spring: no overshoot for fades and subtle motion. */
    public static final SpringParams GENTLE = new SpringParams(150, 1.0);
    /** Bouncy spring: visible overshoot for playful entrances. */
    public static final SpringParams BOUNCY = new SpringParams(600, 0.45);

    private static final Map<DoubleProperty, AnimationTimer> ACTIVE = new WeakHashMap<>();

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
     * Animates a property toward a target with spring physics, replacing any running spring on the
     * same property.
     *
     * @param property the property to drive (must be written on the FX thread)
     * @param toValue rest value
     * @param params spring tuning
     * @param onFinished optional callback, may be {@code null}
     */
    public static void animate(
            DoubleProperty property, double toValue, SpringParams params, Runnable onFinished) {
        SpringParams p = params == null ? DEFAULT : params;
        AnimationTimer previous = ACTIVE.remove(property);
        if (previous != null) {
            previous.stop();
        }
        double epsilon = Math.max(1e-4, Math.abs(toValue) * 5e-4);
        AnimationTimer timer =
                new AnimationTimer() {
                    private long last = -1;
                    private double x = property.get();
                    private double v = 0;

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
                        if (Math.abs(x - toValue) < epsilon && Math.abs(v) < epsilon * 60) {
                            property.set(toValue);
                            stop();
                            ACTIVE.remove(property);
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
     * Animates a property with {@link #DEFAULT} tuning.
     *
     * @param property the property to drive
     * @param toValue rest value
     */
    public static void animate(DoubleProperty property, double toValue) {
        animate(property, toValue, DEFAULT, null);
    }
}
