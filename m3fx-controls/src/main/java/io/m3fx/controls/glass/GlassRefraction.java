package io.m3fx.controls.glass;

import javafx.scene.effect.FloatMap;

/**
 * Pure math behind the Liquid Glass refraction rim (Apple iOS 26). No scene graph dependency
 * beyond {@link FloatMap}, so every method is unit-testable.
 *
 * <p><b>Height profile.</b> The glass surface is a slab whose rim curves down to the edge over a
 * bezel band. Over the bezel, with {@code x} in {@code [0,1]} (0 = outer edge, 1 = bezel end):
 * <pre>
 *   convex(x) = (1 - (1-x)^4)^(1/4)      squircle falloff (chosen; a circle would be
 *                                        (1-(1-x)^2)^(1/2))
 *   concave(x) = 1 - convex(x)
 *   lip(x)     = mix(convex, concave, smootherstep(x)), smootherstep(t) = t^3 (t(6t-15)+10)
 * </pre>
 *
 * <p><b>Normals.</b> The surface normal is taken from the numeric central difference of the
 * profile ({@code dx = 0.001}), rotated -90&deg; so it points out of the glass (y-up, profile
 * space).
 *
 * <p><b>Displacement simulation.</b> An orthographic (straight-down) viewer ray refracts once at
 * the sloped rim surface (Snell's law, n = 1.5). The lateral drift accumulated while descending
 * through the local glass thickness is {@code drift(t) = (Tx/|Ty|) * thickness(t)} where
 * {@code thickness(t) = BEZEL_THICKNESS_RATIO * c(t)}. 127 rays are simulated per bezel; the
 * magnitudes are mirrored around the bezel midpoint (the back surface is the mirror of the front,
 * so the bend is symmetric) and normalized to {@code [0,1]} &mdash; the real pixel shift is the
 * map scale ({@code maxDisplacement}).
 *
 * <p><b>Polar &rarr; cartesian.</b> {@link #buildFloatMap} evaluates the profile by distance to
 * the shape edge (rounded-rect signed distance field) and pushes the sample along the inward edge
 * normal, which maps the symmetric polar magnitude onto x/y with exact mirror symmetry.
 *
 * <p><b>Encoding.</b> {@link #buildFloatMap} returns a {@link FloatMap} with the x displacement in
 * the R channel and the y displacement in the G channel, offset by 0.5, i.e.
 * {@code value = 0.5 + offsetPx / maxDisplacement}, matching the JavaFX {@code DisplacementMap}
 * (and SVG {@code feDisplacementMap}) convention
 * {@code displaced = sample + (value - offset) * scale}. Pair the map with a {@code DisplacementMap}
 * whose {@code scaleX}/{@code scaleY} equal {@code maxDisplacement}. Channel values may leave
 * {@code [0,1]} (they encode signed pixel offsets; DisplacementMap does not clamp them).
 *
 * <p>Sources: {@code docs/SPEC_NOTES/liquid-glass.md}; kube.io/blog/liquid-glass-css-svg
 * (Snell's-law displacement math); github.com/ybouane/liquidglass (pipeline + defaults);
 * Apple WWDC25 "Meet Liquid Glass" (behavior framing). The {@code BEZEL_THICKNESS_RATIO} and the
 * squircle-vs-circle choice are m3fx mapping decisions, not Apple-published values.
 */
public final class GlassRefraction {

    /**
     * Rim curvature profile. {@link #CONVEX} is the iOS-26-like rounded rim; {@link #CONCAVE}
     * inverts it; {@link #LIP} blends convex to concave with a smootherstep.
     */
    public enum Profile {
        /** Squircle rim, rising from the edge to the bezel end. */
        CONVEX,
        /** Inverted rim, falling from the edge to the bezel end. */
        CONCAVE,
        /** Convex near the edge, easing to concave at the bezel end (smootherstep mix). */
        LIP
    }

    /** Rays simulated across the bezel (spec: 127 per radius). */
    public static final int RAY_COUNT = 127;

    /** Glass refractive index used by the simulation (spec: n = 1.5). */
    public static final double REFRACTIVE_INDEX = 1.5;

    /** Central-difference step for numeric normals (spec: dx = 0.001). */
    public static final double DERIVATIVE_DX = 0.001;

    /**
     * Full glass thickness as a multiple of the bezel width. m3fx tuning constant (the map is
     * normalized, so this only shapes the falloff, not the peak shift).
     */
    public static final double BEZEL_THICKNESS_RATIO = 1.0;

    private GlassRefraction() {}

    /**
     * Squircle height profile: {@code (1 - (1-x)^4)^(1/4)}, clamped to {@code [0,1]}.
     *
     * @param x bezel position, 0 = outer edge, 1 = bezel end
     * @return normalized height
     */
    public static double convex(double x) {
        double t = clamp01(x);
        return Math.pow(1.0 - Math.pow(1.0 - t, 4.0), 0.25);
    }

    /**
     * Inverted profile: {@code 1 - convex(x)}.
     *
     * @param x bezel position
     * @return normalized height
     */
    public static double concave(double x) {
        return 1.0 - convex(x);
    }

    /**
     * Lip profile: mix of convex and concave by a smootherstep in {@code x}.
     *
     * @param x bezel position
     * @return normalized height
     */
    public static double lip(double x) {
        double t = clamp01(x);
        double s = t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
        return convex(t) + (concave(t) - convex(t)) * s;
    }

    /**
     * Height profile at a bezel position for the given profile kind.
     *
     * @param x bezel position (clamped to {@code [0,1]})
     * @param profile profile kind, {@code null} treated as {@link Profile#CONVEX}
     * @return normalized height
     */
    public static double heightProfile(double x, Profile profile) {
        return switch (profile == null ? Profile.CONVEX : profile) {
            case CONVEX -> convex(x);
            case CONCAVE -> concave(x);
            case LIP -> lip(x);
        };
    }

    /**
     * Surface slope (dh/dx) from a numeric central difference, {@code dx = 0.001}.
     *
     * @param x bezel position
     * @param profile profile kind
     * @return dh/dx at {@code x}
     */
    public static double slope(double x, Profile profile) {
        double d = DERIVATIVE_DX;
        double up = heightProfile(x + d, profile);
        double down = heightProfile(x - d, profile);
        return (up - down) / (2.0 * d);
    }

    /**
     * Unit surface normal at a bezel position: the numeric-derivative tangent rotated -90&deg;,
     * normalized, oriented OUT of the glass (y-up in profile space). At a flat interior
     * ({@code x = 1}) this is {@code (0, 1)}; at a vertical edge wall it turns horizontal.
     *
     * @param x bezel position
     * @param profile profile kind
     * @return {@code {nx, ny}} unit outward normal
     */
    public static double[] normal(double x, Profile profile) {
        double s = slope(x, profile);
        if (!Double.isFinite(s)) {
            return new double[] {0.0, 1.0};
        }
        double k = Math.sqrt(1.0 + s * s);
        return new double[] {-s / k, 1.0 / k};
    }

    /**
     * Simulates the refraction displacement magnitudes across the bezel and returns them
     * normalized to {@code [0,1]}, symmetric around the bezel midpoint (mirror-simulated bezel).
     *
     * <p>Per ray {@code t}: Snell refraction of an orthographic ray at the sloped surface
     * (n = {@link #REFRACTIVE_INDEX}), drift {@code (Tx/|Ty|) * thickness(t)} with
     * {@code thickness(t) = BEZEL_THICKNESS_RATIO * c(t)}. Total internal reflection is clamped
     * to a grazing ray ({@code sin = 0.999}) so the profile stays finite at the vertical edge
     * wall. The raw drift peaks just inside the outer edge (steepest curvature) and decays to
     * zero at the bezel end. Because the back bezel mirrors the front, boundary rays (knife
     * edge, flat interior) get ~zero total bend while mid-bezel rays collect both kicks; the
     * simulated decay tail is therefore centered on the bezel midpoint, making the magnitudes
     * symmetric around the bezel: peak at the bezel center, zero at the outer edge and at the
     * interior boundary.
     *
     * @param profile profile kind, {@code null} treated as {@link Profile#CONVEX}
     * @return {@link #RAY_COUNT} magnitudes in {@code [0,1]}, index 0 = outer edge
     */
    public static double[] displacementProfile(Profile profile) {
        Profile p = profile == null ? Profile.CONVEX : profile;
        double n = REFRACTIVE_INDEX;
        double eta = 1.0 / n;
        double[] raw = new double[RAY_COUNT];
        for (int i = 0; i < RAY_COUNT; i++) {
            double t = (double) i / (RAY_COUNT - 1);
            double s = slope(t, p);
            double k = Math.sqrt(1.0 + s * s);
            double sinTheta1 = Math.min(1.0, Math.abs(s) / k);
            double sinTheta2 = Math.min(0.999, sinTheta1 / n);
            double cosTheta2 = Math.sqrt(1.0 - sinTheta2 * sinTheta2);
            double cosTheta1 = 1.0 / k;
            // Refracted direction T = eta*I + (eta*cos1 - cos2)*N, I = (0,-1) orthographic,
            // N = (-s, 1)/k outward normal. The lateral component points toward the glass body
            // (inward), which is what pulls the backdrop image inward at the rim.
            double bend = eta * cosTheta1 - cosTheta2;
            double tx = bend * (-s / k);
            double ty = -eta + bend * (1.0 / k);
            double thickness = BEZEL_THICKNESS_RATIO * heightProfile(t, p);
            raw[i] = Math.abs(tx) < 1e-12 || Math.abs(ty) < 1e-12
                    ? 0.0
                    : Math.abs(tx / ty) * thickness;
        }
        // Mirror-simulated bezel: the front and back bezels are mirror images, so boundary rays
        // (knife edge, flat interior) get ~zero total bend while mid-bezel rays collect both
        // kicks. Center the simulated decay tail on the bezel midpoint so the magnitudes are
        // symmetric around the bezel: peak at the bezel center, zero at both boundaries.
        int peak = 0;
        for (int i = 1; i < RAY_COUNT; i++) {
            if (raw[i] > raw[peak]) {
                peak = i;
            }
        }
        int mid = (RAY_COUNT - 1) / 2;
        double[] symmetric = new double[RAY_COUNT];
        double max = raw[peak];
        if (max <= 0.0) {
            return symmetric; // degenerate (flat profile): all zeros
        }
        for (int i = 0; i < RAY_COUNT; i++) {
            int d = Math.abs(i - mid);
            int src = peak + (int) Math.round(d * (double) (RAY_COUNT - 1 - peak) / mid);
            symmetric[i] = raw[Math.min(src, RAY_COUNT - 1)] / max;
        }
        return symmetric;
    }

    /**
     * Builds the {@link FloatMap} for a glass pane of the given pixel size: x displacement in the
     * R channel, y in G, offset 0.5, scaled by {@code maxDisplacement} (pair with a
     * {@code DisplacementMap} whose scale equals {@code maxDisplacement}). Pixels outside the
     * shape or inside the flat interior (at or beyond the bezel) encode zero displacement.
     *
     * <p>The bezel band width equals the shape's effective curvature radius: {@code min(w,h)/2}
     * for {@link GlassShape#CAPSULE}/{@link GlassShape#CIRCLE}, the clamped {@code cornerRadius}
     * for {@link GlassShape#RECT}. A zero radius or non-positive
     * {@code refraction}/{@code maxDisplacement} yields a neutral (no-op) map.
     *
     * @param width map width in px
     * @param height map height in px
     * @param cornerRadius corner radius in px (RECT shape)
     * @param shape glass silhouette, {@code null} treated as {@link GlassShape#CAPSULE}
     * @param profile rim profile, {@code null} treated as {@link Profile#CONVEX}
     * @param refraction refraction strength multiplier (&ge; 1.0 recommended; 0 disables)
     * @param maxDisplacement maximum pixel shift the map can encode (the DisplacementMap scale)
     * @return float map of size {@code ceil(width) x ceil(height)}
     */
    public static FloatMap buildFloatMap(
            double width, double height, double cornerRadius, GlassShape shape, Profile profile,
            double refraction, double maxDisplacement) {
        int w = Math.max(1, (int) Math.ceil(width));
        int h = Math.max(1, (int) Math.ceil(height));
        FloatMap map = new FloatMap(w, h);
        double radius = effectiveRadius(shape, cornerRadius, w, h);
        if (radius <= 0.0 || refraction <= 0.0 || maxDisplacement <= 0.0) {
            return fillNeutral(map);
        }
        double bezel = radius;
        double[] mags = displacementProfile(profile);
        double cx = w / 2.0;
        double cy = h / 2.0;
        double hw = Math.max(0.0, w / 2.0 - radius);
        double hh = Math.max(0.0, h / 2.0 - radius);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double px = x + 0.5;
                double py = y + 0.5;
                double ox = Math.abs(px - cx) - hw;
                double oy = Math.abs(py - cy) - hh;
                // Signed distance to the rounded-rect boundary (negative inside).
                double outsideX = Math.max(ox, 0.0);
                double outsideY = Math.max(oy, 0.0);
                double sdf = Math.sqrt(outsideX * outsideX + outsideY * outsideY)
                        + Math.min(Math.max(ox, oy), 0.0)
                        - radius;
                if (sdf >= 0.0) {
                    map.setSamples(x, y, 0.5f, 0.5f, 0.0f, 1.0f); // outside the glass
                    continue;
                }
                double edgeDist = -sdf;
                if (edgeDist >= bezel) {
                    map.setSamples(x, y, 0.5f, 0.5f, 0.0f, 1.0f); // flat interior: no refraction
                    continue;
                }
                double t = edgeDist / bezel;
                double mag = sampleProfile(mags, t) * refraction;
                // Inward normal = negative gradient of the SDF (polar -> cartesian with mirror
                // symmetry built in).
                double gx;
                double gy;
                if (outsideX > 0.0 || outsideY > 0.0) {
                    double len = Math.sqrt(outsideX * outsideX + outsideY * outsideY);
                    gx = Math.signum(px - cx) * outsideX / len;
                    gy = Math.signum(py - cy) * outsideY / len;
                } else if (ox > oy) {
                    gx = Math.signum(px - cx);
                    gy = 0.0;
                } else {
                    gx = 0.0;
                    gy = Math.signum(py - cy);
                }
                float r = (float) (0.5 - gx * mag);
                float g = (float) (0.5 - gy * mag);
                map.setSamples(x, y, r, g, 0.0f, 1.0f);
            }
        }
        return map;
    }

    /**
     * Returns the shape's effective curvature radius in px, clamped to half the smaller side.
     *
     * @param shape glass silhouette
     * @param cornerRadius corner radius (RECT)
     * @param width pane width in px
     * @param height pane height in px
     * @return effective radius (&ge; 0)
     */
    public static double effectiveRadius(GlassShape shape, double cornerRadius, double width, double height) {
        double halfMin = Math.max(0.0, Math.min(width, height) / 2.0);
        return switch (shape == null ? GlassShape.CAPSULE : shape) {
            case CAPSULE, CIRCLE -> halfMin;
            case RECT -> Math.max(0.0, Math.min(cornerRadius, halfMin));
        };
    }

    /** Linear interpolation into a ray table, {@code t} in {@code [0,1]}. */
    private static double sampleProfile(double[] mags, double t) {
        double pos = Math.max(0.0, Math.min(1.0, t)) * (mags.length - 1);
        int i = (int) pos;
        if (i >= mags.length - 1) {
            return mags[mags.length - 1];
        }
        double frac = pos - i;
        return mags[i] + (mags[i + 1] - mags[i]) * frac;
    }

    private static FloatMap fillNeutral(FloatMap map) {
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                map.setSamples(x, y, 0.5f, 0.5f, 0.0f, 1.0f);
            }
        }
        return map;
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
