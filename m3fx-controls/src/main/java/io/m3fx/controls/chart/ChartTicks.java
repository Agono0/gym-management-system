package io.m3fx.controls.chart;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Nice-number axis tick generation shared by the canvas charts (bar, spline, scatter).
 *
 * <p>Rounding each axis division independently with a fixed format produced degenerate tick
 * labels: duplicates on small ranges ("1, 1, 0, 0, 0" on a 0..1 axis) and uneven steps on
 * padded ranges ("9, 7, 5, 2, 0"). The helpers here snap the axis to a "nice" step
 * (1, 2, 2.5 and 5 times a power of ten) so every tick is a distinct, evenly spaced value,
 * and format the values without trailing zeros ("10", "7.5", "0.25").
 */
public final class ChartTicks {

    /** Safety cap on generated tick count so degenerate inputs cannot explode the loop. */
    private static final int MAX_TICKS = 24;

    /** Scale kept when formatting non-integer labels (well above the charts' needs). */
    private static final int LABEL_SCALE = 10;

    private ChartTicks() {}

    /**
     * Picks the "nice" step closest to the raw step, from 1, 2, 2.5 and 5 times a power of ten.
     *
     * <p>Choosing the closest (rather than the smallest nice step above the raw step) keeps the
     * resulting tick count near the chart's requested division count while still snapping the
     * axis to readable values.
     *
     * @param rawStep the unrounded step the chart would need (must be positive; non-positive or
     *     non-finite input falls back to 1)
     * @return the closest nice step, always positive
     */
    public static double niceStep(double rawStep) {
        if (!Double.isFinite(rawStep) || rawStep <= 0) {
            return 1.0;
        }
        int exponent = (int) Math.floor(Math.log10(rawStep));
        double mantissa = rawStep / Math.pow(10.0, exponent); // in [1, 10)
        // 10 represents one decade up: 10 x 10^exponent == 1 x 10^(exponent+1)
        double[] candidates = {1.0, 2.0, 2.5, 5.0, 10.0};
        double best = candidates[0];
        double bestDistance = Double.MAX_VALUE;
        for (double candidate : candidates) {
            double distance = Math.abs(candidate - mantissa);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best * Math.pow(10.0, exponent);
    }

    /**
     * Computes evenly spaced, distinct tick values covering the given data range.
     *
     * <p>The first tick is at or below {@code min} and the last tick at or above {@code max},
     * so charts can use the returned ends as their axis bounds and keep every data point inside
     * the plot. Tick spacing is {@link #niceStep(double) nice}, so the formatted labels are
     * guaranteed distinct (no "1, 1, 0, 0, 0" runs).
     *
     * @param min the smallest data value the axis must cover
     * @param max the largest data value the axis must cover
     * @param targetDivisions roughly how many grid divisions the chart wants (clamped to 1)
     * @return strictly increasing tick values, length 2 or more (degenerate inputs yield 0..1)
     */
    public static double[] compute(double min, double max, int targetDivisions) {
        if (!Double.isFinite(min) || !Double.isFinite(max) || !(max > min)) {
            return new double[] {0.0, 1.0};
        }
        int divisions = Math.max(1, targetDivisions);
        double step = niceStep((max - min) / divisions);
        double first = Math.floor(min / step) * step;
        // Round-off safe: derive the tick count from the snapped axis end, then cap it.
        int count = (int) Math.round((Math.ceil(max / step) * step - first) / step);
        if (count < 1) {
            count = 1;
        } else if (count > MAX_TICKS - 1) {
            count = MAX_TICKS - 1;
        }
        double[] ticks = new double[count + 1];
        for (int i = 0; i <= count; i++) {
            ticks[i] = first + i * step;
        }
        // The last tick must cover the data exactly even when floating error rounds short.
        if (ticks[count] < max) {
            ticks[count] = first + (count + 1) * step;
        }
        return ticks;
    }

    /**
     * Formats a tick value for display without trailing zeros or decimal noise.
     *
     * @param value the tick value
     * @return "10", "7.5", "0.25" and so on — distinct values always format distinctly
     */
    public static String label(double value) {
        if (Double.isFinite(value) && value == Math.rint(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        return new BigDecimal(value)
                .setScale(LABEL_SCALE, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
