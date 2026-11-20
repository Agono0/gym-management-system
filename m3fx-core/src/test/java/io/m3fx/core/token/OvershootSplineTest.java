package io.m3fx.core.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.animation.Interpolator;
import org.junit.jupiter.api.Test;

/**
 * Locks the math of the shared web-semantics {@link OvershootSpline}: identical to
 * {@link Interpolator#SPLINE} wherever the control points fit inside [0,1]², exact endpoints,
 * overshoot beyond [0,1] for the web curves that need it (the material-web sW3 progress easing),
 * and validation of the abscissae per the CSS easing spec.
 */
class OvershootSplineTest {

    @Test
    void matchesInterpolatorSplineForInRangeControlPoints() {
        double[][] curves = {
            {0.06, 0.11, 0.6, 1},
            {0.2, 0, 0, 1}, // the M3 standard curve, in-range
            {0.5, 0.5, 0.5, 0.5}, // degenerate linear shape
        };
        for (double[] c : curves) {
            OvershootSpline mine = new OvershootSpline(c[0], c[1], c[2], c[3]);
            Interpolator reference = Interpolator.SPLINE(c[0], c[1], c[2], c[3]);
            for (double t = 0; t <= 1.0; t += 0.05) {
                assertEquals(
                        reference.interpolate(0.0, 1.0, t),
                        mine.interpolate(0.0, 1.0, t),
                        1e-3,
                        "same easing as Interpolator.SPLINE for in-range points at t=" + t);
            }
        }
    }

    @Test
    void endpointsAreExact() {
        OvershootSpline spline = new OvershootSpline(0.257759, -0.003163, 0.211762, 1.38179);
        assertEquals(0.0, spline.interpolate(0.0, 1.0, 0.0), 1e-12, "starts at 0");
        assertEquals(1.0, spline.interpolate(0.0, 1.0, 1.0), 1e-12, "ends at 1");
        assertEquals(0.0, spline.interpolate(0.0, 1.0, -0.5), 1e-12, "clamps below");
        assertEquals(1.0, spline.interpolate(0.0, 1.0, 1.5), 1e-12, "clamps above");
    }

    @Test
    void sW3OvershootsLikeTheWebBezier() {
        // sW3 from material-web _linear-progress.scss: y1 dips below 0, y2 overshoots above 1.
        OvershootSpline sW3 = new OvershootSpline(0.257759, -0.003163, 0.211762, 1.38179);
        double max = 0;
        double min = 1;
        for (double t = 0; t <= 1.0; t += 0.002) {
            double v = sW3.interpolate(0.0, 1.0, t);
            max = Math.max(max, v);
            min = Math.min(min, v);
        }
        assertTrue(max > 1.0, "y2=1.38179 must overshoot above 1, max=" + max);
        assertTrue(min < 1.0, "the curve passes below 1 before overshooting, min=" + min);
    }

    @Test
    void rejectsAbscissaeOutsideTheWebRange() {
        // CSS cubic-bezier restricts the abscissae to [0,1]; ordinates are free.
        assertThrows(IllegalArgumentException.class,
                () -> new OvershootSpline(-0.1, 0, 0.5, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new OvershootSpline(0, 0, 1.1, 1));
        // Free ordinates (the whole point of the class) must be accepted.
        new OvershootSpline(0.257759, -0.003163, 0.211762, 1.38179);
    }
}
