package io.m3fx.core.token;

import javafx.animation.Interpolator;

/**
 * M3 Expressive spring interpolator based on the damped harmonic oscillator step response.
 *
 * <p>Unlike fixed bezier curves, this accurately models spring physics over normalized time {@code t}
 * in {@code [0, 1]}, strictly satisfying the JavaFX {@link Interpolator} contract:
 * <ul>
 *   <li>{@code curve(0.0) == 0.0}</li>
 *   <li>{@code curve(1.0) == 1.0}</li>
 * </ul>
 * with a smooth overshoot and settling in between, matching the M3 Expressive motion feel.
 */
public class SpringInterpolator extends Interpolator {

    /** Default M3 Expressive spring: subtle ~4-6% overshoot, settles naturally. */
    public static final SpringInterpolator DEFAULT = new SpringInterpolator(0.70, 7.5);

    /** Bouncy spring: visible ~15% overshoot for playful entrances and icons. */
    public static final SpringInterpolator BOUNCY = new SpringInterpolator(0.50, 9.0);

    /** Gentle spring: critically damped (no overshoot) for fades and subtle changes. */
    public static final SpringInterpolator GENTLE = new SpringInterpolator(1.00, 8.0);

    private final double dampingRatio;
    private final double naturalFrequency;
    private final double rawZero;
    private final double rawOne;

    /**
     * Creates a spring interpolator with specified damping ratio and natural frequency.
     *
     * @param dampingRatio damping ratio (&zeta; &gt; 0, where 1.0 is critically damped)
     * @param naturalFrequency angular frequency (&omega; &gt; 0)
     */
    public SpringInterpolator(double dampingRatio, double naturalFrequency) {
        if (dampingRatio <= 0 || naturalFrequency <= 0) {
            throw new IllegalArgumentException("Damping ratio and frequency must be positive");
        }
        this.dampingRatio = dampingRatio;
        this.naturalFrequency = naturalFrequency;
        this.rawZero = calculateRaw(0.0);
        this.rawOne = calculateRaw(1.0);
    }

    private double calculateRaw(double t) {
        if (t <= 0.0) {
            return 0.0;
        }
        if (dampingRatio >= 1.0) {
            // Critically damped or overdamped
            return 1.0 - (1.0 + naturalFrequency * t) * Math.exp(-naturalFrequency * t);
        } else {
            // Underdamped: damped harmonic oscillation
            double dampedFreq = naturalFrequency * Math.sqrt(1.0 - dampingRatio * dampingRatio);
            double decay = Math.exp(-dampingRatio * naturalFrequency * t);
            double envelope = Math.cos(dampedFreq * t) + (dampingRatio * naturalFrequency / dampedFreq) * Math.sin(dampedFreq * t);
            return 1.0 - decay * envelope;
        }
    }

    @Override
    protected double curve(double t) {
        if (t <= 0.0) {
            return 0.0;
        }
        if (t >= 1.0) {
            return 1.0;
        }
        double raw = calculateRaw(t);
        // Normalize so curve(0) is exactly 0 and curve(1) is exactly 1
        return (raw - rawZero) / (rawOne - rawZero);
    }

    /** Returns the damping ratio (&zeta;). */
    public double getDampingRatio() {
        return dampingRatio;
    }

    /** Returns the natural frequency (&omega;). */
    public double getNaturalFrequency() {
        return naturalFrequency;
    }
}
