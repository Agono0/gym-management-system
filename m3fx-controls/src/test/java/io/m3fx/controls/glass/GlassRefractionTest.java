package io.m3fx.controls.glass;

import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.scene.effect.FloatMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for the pure refraction math behind the liquid glass rim
 * ({@link GlassRefraction}): the height profile endpoints and monotonicity, the
 * {@link FloatMap} dimensions and 0.5 channel offsets, the mirror symmetry of the bezel
 * displacement, and the documented refraction / maxDisplacement encoding semantics.
 *
 * <p>All expectations come from the documented contract in the class Javadoc and
 * {@code docs/SPEC_NOTES/liquid-glass.md} — no invented values.
 */
class GlassRefractionTest {

    private static final double EPS = 1e-9;

    @BeforeAll
    static void initFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX failed to initialize");
    }

    private static void runOnFx(Runnable action) throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] error = new Throwable[1];
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "FX task timed out");
        if (error[0] != null) {
            throw error[0];
        }
    }

    // ------------------------------------------------------------------
    // Height profile
    // ------------------------------------------------------------------

    @Test
    void convexProfileEndpoints() {
        assertEquals(0.0, GlassRefraction.convex(0.0), EPS, "convex(0) must start at the edge");
        assertEquals(1.0, GlassRefraction.convex(1.0), EPS, "convex(1) must reach the bezel end");
        assertEquals(1.0, GlassRefraction.concave(0.0), EPS, "concave mirrors convex at 0");
        assertEquals(0.0, GlassRefraction.concave(1.0), EPS, "concave mirrors convex at 1");
    }

    @Test
    void convexProfileIsMonotone() {
        double previous = -1;
        for (int i = 0; i <= 100; i++) {
            double x = i / 100.0;
            double v = GlassRefraction.convex(x);
            assertTrue(v >= previous - EPS, "convex must be non-decreasing at x=" + x);
            assertTrue(v >= 0.0 && v <= 1.0, "convex must stay in [0,1] at x=" + x);
            previous = v;
        }
        // concave is the exact mirror, so it must be non-increasing over the same range.
        previous = 2;
        for (int i = 0; i <= 100; i++) {
            double v = GlassRefraction.concave(i / 100.0);
            assertTrue(v <= previous + EPS, "concave must be non-increasing at x=" + i / 100.0);
            previous = v;
        }
    }

    @Test
    void heightProfileDispatchesAndDefaultsToConvex() {
        for (int i = 0; i <= 10; i++) {
            double x = i / 10.0;
            assertEquals(GlassRefraction.convex(x), GlassRefraction.heightProfile(x, null), EPS,
                    "null profile is documented as CONVEX");
            assertEquals(GlassRefraction.convex(x),
                    GlassRefraction.heightProfile(x, GlassRefraction.Profile.CONVEX), EPS);
            assertEquals(GlassRefraction.concave(x),
                    GlassRefraction.heightProfile(x, GlassRefraction.Profile.CONCAVE), EPS);
            assertEquals(GlassRefraction.lip(x),
                    GlassRefraction.heightProfile(x, GlassRefraction.Profile.LIP), EPS);
        }
    }

    @Test
    void normalIsVerticalAtFlatInterior() {
        double[] n = GlassRefraction.normal(1.0, GlassRefraction.Profile.CONVEX);
        assertEquals(0.0, n[0], 1e-6, "flat interior normal has no lateral component");
        assertEquals(1.0, n[1], 1e-6, "flat interior normal points out of the glass");
    }

    // ------------------------------------------------------------------
    // Displacement profile (ray simulation)
    // ------------------------------------------------------------------

    @Test
    void displacementProfileIsNormalizedAndMirrorSymmetric() {
        double[] mags = GlassRefraction.displacementProfile(GlassRefraction.Profile.CONVEX);
        assertEquals(GlassRefraction.RAY_COUNT, mags.length, "one magnitude per simulated ray");
        double max = 0;
        for (int i = 0; i < mags.length; i++) {
            assertTrue(mags[i] >= 0.0 && mags[i] <= 1.0, "magnitudes are normalized to [0,1]");
            assertEquals(mags[mags.length - 1 - i], mags[i], EPS,
                    "bezel magnitudes are symmetric around the midpoint (index " + i + ")");
            max = Math.max(max, mags[i]);
        }
        assertEquals(1.0, max, EPS, "normalization means the peak magnitude is exactly 1");
    }

    // ------------------------------------------------------------------
    // FloatMap
    // ------------------------------------------------------------------

    @Test
    void floatMapHasPixelDimensionsAndNeutralCenter() throws Throwable {
        runOnFx(() -> {
            FloatMap map = GlassRefraction.buildFloatMap(
                    200, 120, 28, GlassShape.RECT, GlassRefraction.Profile.CONVEX, 1.0, 6.0);
            assertEquals(200, map.getWidth(), "map width is the ceiling of the pane width");
            assertEquals(120, map.getHeight(), "map height is the ceiling of the pane height");

            // Flat interior (far from the bezel): zero displacement encodes as the 0.5 offset.
            assertEquals(0.5f, getSample(map, 100, 60, 0), 1e-6f, "R channel offset is 0.5");
            assertEquals(0.5f, getSample(map, 100, 60, 1), 1e-6f, "G channel offset is 0.5");
            // Outer corner pixel lies outside the rounded shape: neutral as well.
            assertEquals(0.5f, getSample(map, 0, 0, 0), 1e-6f);
            assertEquals(0.5f, getSample(map, 0, 0, 1), 1e-6f);
        });
    }

    @Test
    void bezelDisplacementIsMirrorSymmetric() throws Throwable {
        runOnFx(() -> {
            FloatMap map = GlassRefraction.buildFloatMap(
                    200, 120, 28, GlassShape.RECT, GlassRefraction.Profile.CONVEX, 1.0, 6.0);
            // Left vs right bezel at the vertical middle (x displacement in R).
            float left = getSample(map, 8, 60, 0);
            float right = getSample(map, 191, 60, 0);
            assertTrue(Math.abs(left - 0.5f) > 1e-3f, "the left bezel must displace");
            assertEquals(1.0f, left + right, 1e-4f,
                    "R deviations mirror: (vLeft-0.5) == -(vRight-0.5)");
            // Top vs bottom bezel at the horizontal middle (y displacement in G).
            float top = getSample(map, 100, 8, 1);
            float bottom = getSample(map, 100, 111, 1);
            assertTrue(Math.abs(top - 0.5f) > 1e-3f, "the top bezel must displace");
            assertEquals(1.0f, top + bottom, 1e-4f,
                    "G deviations mirror: (vTop-0.5) == -(vBottom-0.5)");
            // On the vertical midline inside the bezel band the x displacement stays neutral
            // (the inward edge normal there is purely vertical).
            assertEquals(0.5f, getSample(map, 100, 8, 0), 1e-6f);
        });
    }

    @Test
    void zeroRefractionAndZeroMaxDisplacementYieldNeutralMap() throws Throwable {
        runOnFx(() -> {
            for (double disabled : new double[] {0.0, -1.0}) {
                FloatMap noRefraction = GlassRefraction.buildFloatMap(
                        200, 120, 28, GlassShape.RECT, GlassRefraction.Profile.CONVEX,
                        disabled, 6.0);
                assertNeutral(noRefraction, "refraction " + disabled);
                FloatMap noScale = GlassRefraction.buildFloatMap(
                        200, 120, 28, GlassShape.RECT, GlassRefraction.Profile.CONVEX,
                        1.0, disabled);
                assertNeutral(noScale, "maxDisplacement " + disabled);
            }
        });
    }

    /**
     * The documented encoding: {@code value = 0.5 + offsetPx / maxDisplacement}, with the real
     * pixel shift applied by the paired DisplacementMap's scale — so the map itself is normalized
     * and must be invariant to {@code maxDisplacement}, while it scales linearly with
     * {@code refraction}.
     */
    @Test
    void maxDisplacementIsTheExternalScaleAndRefractionScalesLinearly() throws Throwable {
        runOnFx(() -> {
            FloatMap small = GlassRefraction.buildFloatMap(
                    200, 120, 28, GlassShape.RECT, GlassRefraction.Profile.CONVEX, 1.0, 4.0);
            FloatMap large = GlassRefraction.buildFloatMap(
                    200, 120, 28, GlassShape.RECT, GlassRefraction.Profile.CONVEX, 1.0, 40.0);
            // A bezel pixel on the left edge: gx = -1, so value = 0.5 + normalized.
            for (int[] pixel : new int[][] {{8, 60}, {16, 60}, {100, 8}}) {
                assertEquals(getSample(small, pixel[0], pixel[1], 0),
                        getSample(large, pixel[0], pixel[1], 0), 1e-6f,
                        "maxDisplacement must not change the encoded values at " + pixel[0] + ","
                                + pixel[1]);
            }

            FloatMap stronger = GlassRefraction.buildFloatMap(
                    200, 120, 28, GlassShape.RECT, GlassRefraction.Profile.CONVEX, 2.0, 6.0);
            double base = getSample(small, 8, 60, 0) - 0.5f;
            double doubled = getSample(stronger, 8, 60, 0) - 0.5f;
            assertEquals(base * 2.0, doubled, 1e-6, "refraction scales the deviation linearly");
        });
    }

    @Test
    void effectiveRadiusIsClampedToHalfTheSmallerSide() {
        // RECT keeps its requested corner radius (the SDF and bezel band are built with it);
        // only oversized or negative radii clamp, per the GlassRefraction Javadoc.
        assertEquals(28.0, GlassRefraction.effectiveRadius(
                GlassShape.RECT, 28, 200, 120), EPS, "RECT keeps a valid radius");
        assertEquals(60.0, GlassRefraction.effectiveRadius(
                GlassShape.RECT, 999, 200, 120), EPS, "RECT radius clamps to min/2");
        assertEquals(60.0, GlassRefraction.effectiveRadius(
                GlassShape.CAPSULE, 0, 200, 120), EPS, "CAPSULE uses min/2");
        assertEquals(60.0, GlassRefraction.effectiveRadius(
                GlassShape.CIRCLE, 0, 200, 120), EPS, "CIRCLE uses min/2");
        assertEquals(0.0, GlassRefraction.effectiveRadius(
                GlassShape.RECT, -5, 200, 120), EPS, "negative radius clamps to 0");
    }

    private static void assertNeutral(FloatMap map, String what) {
        for (int y = 0; y < map.getHeight(); y += 7) {
            for (int x = 0; x < map.getWidth(); x += 7) {
                assertEquals(0.5f, getSample(map, x, y, 0), 1e-6f,
                        "R channel neutral when disabled (" + what + ") at " + x + "," + y);
                assertEquals(0.5f, getSample(map, x, y, 1), 1e-6f,
                        "G channel neutral when disabled (" + what + ") at " + x + "," + y);
            }
        }
    }

    /**
     * Reads a channel sample back from the map's backing buffer. {@code FloatMap} has no public
     * getter, so the test reads its {@code buf} field reflectively (setSamples writes it
     * synchronously; no toolkit needed).
     */
    private static float getSample(FloatMap map, int x, int y, int channel) {
        try {
            java.lang.reflect.Field buffer = FloatMap.class.getDeclaredField("buf");
            buffer.setAccessible(true);
            float[] buf = (float[]) buffer.get(map);
            return buf[(y * map.getWidth() + x) * 4 + channel];
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read FloatMap backing buffer", e);
        }
    }
}
