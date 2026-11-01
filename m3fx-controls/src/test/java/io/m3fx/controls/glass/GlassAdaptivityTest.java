package io.m3fx.controls.glass;

import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for the documented legibility adaptation of liquid glass
 * ({@link GlassAdaptivity}): the luminance thresholds must produce exactly the suggestions the
 * class Javadoc documents —
 * <ul>
 *   <li>REGULAR variant plus the strongest scrim near the luminance midpoint (0.5), where neither
 *       foreground polarity has contrast; the danger band is &plusmn;0.25 around the midpoint;</li>
 *   <li>CLEAR variant and a zero scrim away from the midpoint, the scrim tapering linearly to
 *       zero at &plusmn;0.35 with a maximum of 0.55;</li>
 *   <li>a light on-glass foreground on the dark scheme and a dark one on the light scheme.</li>
 * </ul>
 * The threshold values (0.5 midpoint, 0.25 clear band, 0.35 scrim reach, 0.55 max scrim) are
 * pinned here on purpose: they are recorded in the class Javadoc as the single place to adjust.
 */
class GlassAdaptivityTest {

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
    // Variant suggestion vs luminance
    // ------------------------------------------------------------------

    @Test
    void midpointLuminanceSuggestsFrostedRegularGlass() {
        assertEquals(GlassVariant.REGULAR, GlassAdaptivity.suggestVariant(0.5),
                "the midpoint is the worst case for both polarities: frosted glass");
        assertEquals(GlassVariant.REGULAR, GlassAdaptivity.suggestVariant(0.26),
                "inside the 0.25 clear band: still regular");
        assertEquals(GlassVariant.REGULAR, GlassAdaptivity.suggestVariant(0.74),
                "inside the 0.25 clear band on the bright side: still regular");
    }

    @Test
    void luminanceAwayFromTheMidpointSuggestsClearGlass() {
        assertEquals(GlassVariant.CLEAR, GlassAdaptivity.suggestVariant(0.0),
                "a very dark backdrop reads as content-rich: clear");
        assertEquals(GlassVariant.CLEAR, GlassAdaptivity.suggestVariant(1.0),
                "a very bright backdrop reads as content-rich: clear");
        assertEquals(GlassVariant.CLEAR, GlassAdaptivity.suggestVariant(0.25),
                "exactly at the clear band edge the frosted suggestion ends");
        assertEquals(GlassVariant.CLEAR, GlassAdaptivity.suggestVariant(0.75),
                "exactly at the clear band edge the frosted suggestion ends");
    }

    @Test
    void variantSuggestionClampsPathologicalLuminance() {
        assertEquals(GlassVariant.CLEAR, GlassAdaptivity.suggestVariant(-3.0),
                "negative luminance clamps to 0 (far from the midpoint)");
        assertEquals(GlassVariant.CLEAR, GlassAdaptivity.suggestVariant(7.0),
                "luminance above 1 clamps to 1 (far from the midpoint)");
        assertEquals(GlassVariant.REGULAR, GlassAdaptivity.suggestVariant(Double.NaN),
                "non-finite luminance degrades to the defensive midpoint suggestion");
    }

    // ------------------------------------------------------------------
    // Scrim taper
    // ------------------------------------------------------------------

    @Test
    void scrimIsStrongestAtTheMidpointAndZeroOutsideTheReach() {
        assertEquals(0.55, GlassAdaptivity.scrimStrength(0.5), EPS,
                "documented maximum scrim 0.55 at the midpoint");
        assertEquals(0.0, GlassAdaptivity.scrimStrength(0.15), EPS,
                "documented scrim reach 0.35: zero at the reach boundary (dark side)");
        assertEquals(0.0, GlassAdaptivity.scrimStrength(0.85), EPS,
                "documented scrim reach 0.35: zero at the reach boundary (bright side)");
        assertEquals(0.0, GlassAdaptivity.scrimStrength(1.0), EPS,
                "far from the midpoint the matching foreground carries legibility alone");
    }

    @Test
    void scrimTapersLinearlyTowardTheReach() {
        // 0.65 is 0.15 from the midpoint: 0.55 * (1 - 0.15 / 0.35) = 0.55 * 4/7.
        assertEquals(0.55 * 4.0 / 7.0, GlassAdaptivity.scrimStrength(0.65), 1e-12,
                "the taper is linear in the distance from the midpoint");
        // Monotone: nearer the midpoint, stronger the scrim.
        double nearer = GlassAdaptivity.scrimStrength(0.45);
        double middle = GlassAdaptivity.scrimStrength(0.4);
        double edge = GlassAdaptivity.scrimStrength(0.35);
        assertTrue(nearer > middle && middle > edge,
                "scrim must grow monotonically toward the midpoint");
        for (int i = 0; i <= 100; i++) {
            double scrim = GlassAdaptivity.scrimStrength(i / 100.0);
            assertTrue(scrim >= 0.0 && scrim <= 0.55, "scrim stays in [0, 0.55]");
        }
    }

    // ------------------------------------------------------------------
    // Foreground polarity + luminance measurement
    // ------------------------------------------------------------------

    @Test
    void onGlassForegroundMatchesTheSceneTheme() {
        Color darkThemeText = GlassAdaptivity.readableTextOnGlass(true);
        Color lightThemeText = GlassAdaptivity.readableTextOnGlass(false);
        double darkSum = darkThemeText.getRed() + darkThemeText.getGreen()
                + darkThemeText.getBlue();
        double lightSum = lightThemeText.getRed() + lightThemeText.getGreen()
                + lightThemeText.getBlue();
        assertTrue(darkSum > lightSum + 1.0,
                "the dark scheme must suggest a light on-glass foreground (M3 on-surface tokens)");
    }

    @Test
    void averageLuminanceHandlesNullEmptyAndUniformBackdrops() throws Throwable {
        runOnFx(() -> {
            assertEquals(0.5, GlassAdaptivity.averageLuminance(null), EPS,
                    "null backdrop degrades to the defensive neutral midpoint");

            WritableImage white = uniformImage(2, 2, Color.WHITE);
            assertEquals(1.0, GlassAdaptivity.averageLuminance(white), 1e-9, "white is 1.0");
            WritableImage black = uniformImage(2, 2, Color.BLACK);
            assertEquals(0.0, GlassAdaptivity.averageLuminance(black), 1e-9, "black is 0.0");

            WritableImage transparent = uniformImage(2, 2, Color.color(1, 1, 1, 0));
            assertEquals(0.5, GlassAdaptivity.averageLuminance(transparent), EPS,
                    "a fully transparent backdrop has no samples: neutral midpoint");

            WritableImage halfAndHalf = new WritableImage(2, 2);
            PixelWriter writer = halfAndHalf.getPixelWriter();
            writer.setColor(0, 0, Color.WHITE);
            writer.setColor(1, 0, Color.WHITE);
            writer.setColor(0, 1, Color.BLACK);
            writer.setColor(1, 1, Color.BLACK);
            assertEquals(0.5, GlassAdaptivity.averageLuminance(halfAndHalf), 1e-9,
                    "an even white/black mix averages to the midpoint");
        });
    }

    /** Builds a uniform image of the given color (no scene graph needed, just the toolkit). */
    private static WritableImage uniformImage(int width, int height, Color color) {
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setColor(x, y, color);
            }
        }
        return image;
    }
}
