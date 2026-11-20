package io.m3fx.core.token;

import javafx.animation.Interpolator;

/**
 * Cubic-bezier easing with web {@code cubic-bezier()} semantics: the x control points must be in
 * [0,1], but the y control points may overshoot — which {@link Interpolator#SPLINE} rejects.
 *
 * <p>Solves x(t)=progress for t with Newton-Raphson and a bisection fallback, then evaluates
 * y(t), mirroring the reference UnitBezier implementation. See the CSS easing functions spec
 * (<a href="https://www.w3.org/TR/css-easing-1/#cubic-bezier-easing-functions">css-easing-1</a>:
 * abscissae are restricted to [0,1], ordinates are not) and material-web's
 * {@code progress/internal/_linear-progress.scss} sW2/sW3 curves for the overshooting cases that
 * motivated this class.
 */
public class OvershootSpline extends Interpolator {

    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;

    /**
     * Creates a web-semantics cubic-bezier interpolator.
     *
     * @param x1 first control-point abscissa, in [0,1]
     * @param y1 first control-point ordinate (free; may overshoot outside [0,1])
     * @param x2 second control-point abscissa, in [0,1]
     * @param y2 second control-point ordinate (free; may overshoot outside [0,1])
     */
    public OvershootSpline(double x1, double y1, double x2, double y2) {
        if (x1 < 0 || x1 > 1 || x2 < 0 || x2 > 1) {
            throw new IllegalArgumentException("Web cubic-bezier abscissae must be in [0, 1]");
        }
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    @Override
    protected double curve(double t) {
        if (t <= 0) {
            return 0;
        }
        if (t >= 1) {
            return 1;
        }
        return sampleY(solveTForX(t));
    }

    private double solveTForX(double x) {
        double cx = 3 * x1;
        double bx = 3 * (x2 - x1) - cx;
        double ax = 1 - cx - bx;
        double t = x;
        for (int i = 0; i < 8; i++) {
            double err = sampleX(ax, bx, cx, t) - x;
            if (Math.abs(err) < 1e-6) {
                return t;
            }
            double slope = sampleDX(ax, bx, cx, t);
            if (Math.abs(slope) < 1e-6) {
                break;
            }
            t -= err / slope;
        }
        double lo = 0;
        double hi = 1;
        t = x;
        while (lo < hi) {
            double current = sampleX(ax, bx, cx, t);
            if (Math.abs(current - x) < 1e-6) {
                return t;
            }
            if (current < x) {
                lo = t;
            } else {
                hi = t;
            }
            t = (lo + hi) / 2;
        }
        return t;
    }

    private double sampleX(double ax, double bx, double cx, double t) {
        return ((ax * t + bx) * t + cx) * t;
    }

    private double sampleDX(double ax, double bx, double cx, double t) {
        return (3 * ax * t + 2 * bx) * t + cx;
    }

    private double sampleY(double t) {
        double cy = 3 * y1;
        double by = 3 * (y2 - y1) - cy;
        double ay = 1 - cy - by;
        return ((ay * t + by) * t + cy) * t;
    }
}
