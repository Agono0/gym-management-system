package io.m3fx.controls.glass;

import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the Liquid Glass shader port (liquidGL {@code fs} math) and the nikdelvin SVG-map
 * construction: SDF, edge factor, the flat-centre/active-rim offset field, the deform point,
 * render-level masks, and the reference-engine defaults.
 */
class GlassRefractionTest {

    private static final double EPS = 1e-9;

    @Test
    void sdfRoundBoxMatchesTheReferenceFormula() {
        // Inside the straight segment: negative distance to the edge.
        assertEquals(-40.0, GlassRefraction.sdfRoundBox(0, 0, 60, 40, 12), EPS,
                "centre SDF = min(|p|-b) - r (40px to the nearest edge)");
        // On the border: zero.
        assertEquals(0.0, GlassRefraction.sdfRoundBox(60, 0, 60, 40, 12), EPS);
        // Outside: positive euclidean distance.
        assertEquals(15.0, GlassRefraction.sdfRoundBox(75, 0, 60, 40, 12), EPS);
    }

    @Test
    void edgeFactorIsOneAtTheBorderAndZeroPastTheBevel() {
        double bevelWidth = 0.15;
        double boxW = 400, boxH = 200;
        double bevelPx = bevelWidth * Math.min(boxW, boxH);
        double halfW = boxW / 2.0, halfH = boxH / 2.0;
        double r = 20.0;
        assertEquals(1.0, GlassRefraction.edgeFactor(halfW, 0, halfW, halfH, r, bevelWidth, boxW, boxH),
                EPS, "the border itself is fully edge");
        assertEquals(0.0, GlassRefraction.edgeFactor(halfW - bevelPx - 1, 0, halfW, halfH, r,
                bevelWidth, boxW, boxH), EPS, "past the bevel the surface is flat");
        double mid = GlassRefraction.edgeFactor(halfW - bevelPx / 2.0, 0, halfW, halfH, r,
                bevelWidth, boxW, boxH);
        assertTrue(mid > 0.0 && mid < 1.0, "mid-bevel interpolates, got " + mid);
    }

    @Test
    void refractionOffsetIsZeroAtTheCentreAndOutwardAtTheRim() {
        double boxW = 400, boxH = 200, r = 24.0;
        double refraction = 0.01, bevelDepth = 0.08, bevelWidth = 0.15;
        // Centre: centreBlend smoothstep(0.15, 0.45, |p|/boxH) is 0 for |p|/boxH < 0.15.
        double[] centre = GlassRefraction.refractionOffset(5, 5, 5, 5, boxW, boxH, r,
                refraction, bevelDepth, bevelWidth, null);
        assertEquals(0.0, centre[0], EPS, "flat centre: no x offset");
        assertEquals(0.0, centre[1], EPS, "flat centre: no y offset");
        // Rim (right border, mid-height): direction is outward (+x), offset nonzero.
        double[] rim = GlassRefraction.refractionOffset(boxW / 2.0 - 1, 0, boxW / 2.0 - 1, 0,
                boxW, boxH, r, refraction, bevelDepth, bevelWidth, null);
        assertTrue(rim[0] > 0.0, "right-rim offset points outward (+x), got " + rim[0]);
        assertEquals(0.0, rim[1], EPS, "mid-height rim has no y component");
        // The offset magnitude honours the liquidGL defaults: edge*refraction + edge^10*depth.
        assertTrue(Math.hypot(rim[0], rim[1]) <= refraction + bevelDepth + 1e-12,
                "offset bounded by refraction + bevelDepth");
    }

    @Test
    void refractionOffsetIsMirrorSymmetricAcrossTheAxes() {
        double boxW = 400, boxH = 200, r = 24.0;
        double[] right = GlassRefraction.refractionOffset(boxW / 2.0 - 2, 0, boxW / 2.0 - 2, 0,
                boxW, boxH, r, 0.01, 0.08, 0.15, null);
        double[] left = GlassRefraction.refractionOffset(-(boxW / 2.0 - 2), 0, -(boxW / 2.0 - 2), 0,
                boxW, boxH, r, 0.01, 0.08, 0.15, null);
        assertEquals(right[0], -left[0], 1e-9, "x offsets mirror");
        assertEquals(right[1], left[1], 1e-9, "y offsets mirror");
    }

    @Test
    void deformPointPullsPointsTowardThePointerDelta() {
        var interaction = new GlassRefraction.Interaction(0.5, 0.5, 0.1, 0.0, 0.5);
        double boxW = 400, boxH = 200;
        // Centre point sits exactly at the pointer: full influence, pulled by -delta*box.
        double[] centre = GlassRefraction.deformPoint(0, 0, boxW, boxH, interaction);
        assertEquals(-0.1 * boxW, centre[0], 1e-9, "x pulled by the interaction delta");
        assertEquals(0.0, centre[1], 1e-9, "y delta is zero");
        // Far outside the reach: untouched.
        double[] far = GlassRefraction.deformPoint(boxW / 2.0 - 1, boxH / 2.0 - 1, boxW, boxH,
                interaction);
        assertEquals(boxW / 2.0 - 1, far[0], 1e-9, "beyond reach the point is undeformed");
        // Radius <= 0: identity.
        double[] none = GlassRefraction.deformPoint(0, 0, boxW, boxH,
                GlassRefraction.Interaction.NONE);
        assertEquals(0.0, none[0], EPS);
    }

    @Test
    void renderMasksOutsidePixelsAndKeepsTheCentreFaithful() {
        int w = 60, h = 30;
        WritableImage backdrop = new WritableImage(w + 40, h + 40);
        var writer = backdrop.getPixelWriter();
        for (int y = 0; y < (int) backdrop.getHeight(); y++) {
            for (int x = 0; x < (int) backdrop.getWidth(); x++) {
                writer.setColor(x, y, Color.color(0.2, 0.4, 0.8, 1.0));
            }
        }
        WritableImage out = new WritableImage(w, h);
        var params = new GlassRefraction.ShaderParams(w, h, 10.0, 0.01, 0.08, 0.15, 0.0, 0.0,
                1.0, null, null);
        GlassRefraction.render(backdrop, out, 20, 20, params, null);
        // Corner pixel lies outside the rounded shape: transparent (the shadow pass owns it).
        assertEquals(0.0, out.getPixelReader().getColor(0, 0).getOpacity(),
                "outside-shape corner is masked transparent");
        // Centre pixel: uniform backdrop + flat centre => the backdrop colour, fully opaque.
        Color centre = out.getPixelReader().getColor(w / 2, h / 2);
        assertEquals(0.2, centre.getRed(), 1e-6, "uniform backdrop passes through at the centre");
        assertEquals(1.0, centre.getOpacity(), "centre stays opaque");
    }

    @Test
    void renderFrostBlurMixesNeighbouringSamples() {
        int w = 60, h = 30;
        WritableImage backdrop = new WritableImage(w + 40, h + 40);
        var writer = backdrop.getPixelWriter();
        for (int y = 0; y < (int) backdrop.getHeight(); y++) {
            for (int x = 0; x < (int) backdrop.getWidth(); x++) {
                // 4px vertical stripes: the sharp render keeps the red/blue alternation, while
                // frost's 16 taps within an 8px radius average them toward gray.
                writer.setColor(x, y, (x / 4) % 2 == 0 ? Color.RED : Color.BLUE);
            }
        }
        WritableImage sharp = new WritableImage(w, h);
        WritableImage frosted = new WritableImage(w, h);
        GlassRefraction.render(backdrop, sharp, 20, 20, new GlassRefraction.ShaderParams(
                w, h, 10.0, 0.01, 0.08, 0.15, 0.0, 0.0, 1.0, null, null), null);
        GlassRefraction.render(backdrop, frosted, 20, 20, new GlassRefraction.ShaderParams(
                w, h, 10.0, 0.01, 0.08, 0.15, 2.0, 0.0, 1.0, null, null), null);
        // Mean red/blue spread over the central region: frost must pull it toward gray.
        double sharpSpread = 0.0;
        double frostedSpread = 0.0;
        int count = 0;
        for (int y = h / 4; y < 3 * h / 4; y++) {
            for (int x = w / 4; x < 3 * w / 4; x++) {
                Color s = sharp.getPixelReader().getColor(x, y);
                Color f = frosted.getPixelReader().getColor(x, y);
                sharpSpread += Math.abs(s.getRed() - s.getBlue());
                frostedSpread += Math.abs(f.getRed() - f.getBlue());
                count++;
            }
        }
        sharpSpread /= count;
        frostedSpread /= count;
        assertTrue(frostedSpread < sharpSpread,
                "frost averages the striped backdrop toward gray (sharp=" + sharpSpread
                        + ", frosted=" + frostedSpread + ")");
    }

    @Test
    void nikdelvinFieldEncodesTheGradientScreenConstruction() {
        var channels = GlassRefraction.nikdelvinChannels(200, 100, 30, 10);
        double[][] rField = channels[0];
        double[][] gField = channels[1];
        assertEquals(200, rField[0].length, "pixel resolution");
        // Bezel (outside the depth-inset rect): R follows the raw X gradient, G the Y gradient.
        assertTrue(rField[50][2] > rField[50][197], "R channel falls left -> right (the X gradient)");
        assertTrue(gField[2][100] > gField[97][100], "G channel falls top -> bottom (the Y gradient)");
        // Inside the gray screen region the centre is lifted toward neutral by the 0.5 blend.
        assertTrue(gField[50][100] > 0.5, "the gray rect screen lifts the centre toward neutral");
    }

    @Test
    void referenceEngineDefaultsAreLocked() {
        assertEquals(0.01, GlassRefraction.DEFAULT_REFRACTION, EPS, "liquidGL refraction");
        assertEquals(0.08, GlassRefraction.DEFAULT_BEVEL_DEPTH, EPS, "liquidGL bevel depth");
        assertEquals(0.15, GlassRefraction.DEFAULT_BEVEL_WIDTH, EPS, "liquidGL bevel width");
        assertEquals(10.0, GlassRefraction.DEFAULT_NIKDELVIN_DEPTH, EPS, "nikdelvin depth");
        assertEquals(100.0, GlassRefraction.DEFAULT_NIKDELVIN_STRENGTH, EPS, "nikdelvin strength");
    }

    @Test
    void effectiveRadiusClampsToHalfTheSmallerSide() {
        assertEquals(50.0, GlassRefraction.effectiveRadius(GlassShape.CAPSULE, 999, 400, 100), EPS);
        assertEquals(50.0, GlassRefraction.effectiveRadius(GlassShape.CIRCLE, 0, 400, 100), EPS);
        assertEquals(24.0, GlassRefraction.effectiveRadius(GlassShape.RECT, 24, 400, 100), EPS);
        assertEquals(50.0, GlassRefraction.effectiveRadius(GlassShape.RECT, 999, 400, 100), EPS);
    }

    @Test
    void refractedRenderIsDeterministic() {
        int w = 40, h = 20;
        WritableImage backdrop = new WritableImage(w + 40, h + 40);
        var writer = backdrop.getPixelWriter();
        for (int y = 0; y < (int) backdrop.getHeight(); y++) {
            for (int x = 0; x < (int) backdrop.getWidth(); x++) {
                writer.setColor(x, y, Color.color(
                        (x % 7) / 7.0, (y % 5) / 5.0, ((x + y) % 11) / 11.0, 1.0));
            }
        }
        var params = new GlassRefraction.ShaderParams(w, h, 12.0, 0.02, 0.08, 0.15, 0.5, 0.0,
                1.0, null, null);
        WritableImage a = new WritableImage(w, h);
        WritableImage b = new WritableImage(w, h);
        GlassRefraction.render(backdrop, a, 20, 20, params, null);
        GlassRefraction.render(backdrop, b, 20, 20, params, null);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(a.getPixelReader().getColor(x, y), b.getPixelReader().getColor(x, y),
                        "two renders of the same input must be identical");
            }
        }
    }
}
