package io.m3fx.controls.glass;

import io.m3fx.core.theme.ColorRole;
import io.m3fx.core.theme.M3Theme;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Legibility adaptation for Liquid Glass (Apple's "the system adapts so content stays readable"
 * guarantee), as pure static helpers so they are unit-testable without a scene.
 *
 * <p>Apple's published behavior: glass picks a light or dark foreground to match the backdrop and
 * layers a scrim/tint when the backdrop is busy so content remains legible; the exact thresholds
 * are not published. This class models them from one signal &mdash; the average backdrop luminance
 * in {@code [0, 1]} (use {@link #averageLuminance(WritableImage)} to compute it from a backdrop
 * snapshot, or pass your own value):
 *
 * <ul>
 *   <li><strong>Foreground polarity</strong> is chosen by scene theme via
 *       {@link #readableTextOnGlass(boolean)} &mdash; the standard M3 on-surface token of the
 *       matching baseline scheme.</li>
 *   <li><strong>Legibility danger zone</strong> is around the luminance midpoint (0.5), where
 *       neither a light nor a dark foreground has meaningful contrast: there the helpers suggest
 *       the frosted {@link GlassVariant#REGULAR} pane plus the strongest scrim; away from the
 *       midpoint the {@link GlassVariant#CLEAR} pane works and the scrim tapers to zero.</li>
 * </ul>
 *
 * <p>Threshold constants ({@value #CLEAR_BAND}, {@value #SCRIM_REACH}, {@value #MAX_SCRIM}) are
 * our tuned mapping, not Apple values &mdash; recorded here so they can be adjusted from one
 * place. Real Apple adaptation also uses backdrop content variance, not just luminance; callers
 * with more signals should treat these results as suggestions.
 */
public final class GlassAdaptivity {

    /** Luminance midpoint: the worst case for both foreground polarities at once. */
    static final double MIDPOINT = 0.5;

    /** Distance from the midpoint beyond which the clear variant is suggested. */
    static final double CLEAR_BAND = 0.25;

    /** Distance from the midpoint at which the scrim has fully tapered to zero. */
    static final double SCRIM_REACH = 0.35;

    /** Scrim strength at the midpoint (maximum). */
    static final double MAX_SCRIM = 0.55;

    private static final Color DARK_THEME_TEXT = M3Theme.baseline(true).get(ColorRole.ON_SURFACE);
    private static final Color LIGHT_THEME_TEXT = M3Theme.baseline(false).get(ColorRole.ON_SURFACE);

    private GlassAdaptivity() {}

    /**
     * Suggests a glass variant for the given backdrop luminance: {@link GlassVariant#REGULAR}
     * (frosted) near the luminance midpoint where a transparent pane cannot stay legible,
     * {@link GlassVariant#CLEAR} (content-rich backdrop shows through) away from it.
     *
     * @param backdropLuminance average backdrop luminance in {@code [0, 1]} (clamped)
     * @return the suggested variant, never {@code null}
     */
    public static GlassVariant suggestVariant(double backdropLuminance) {
        return Math.abs(clamp01(backdropLuminance) - MIDPOINT) < CLEAR_BAND
                ? GlassVariant.REGULAR
                : GlassVariant.CLEAR;
    }

    /**
     * Scrim strength {@code [0, 1]} for the given backdrop luminance: zero away from the midpoint
     * (a matching-polarity foreground carries legibility alone), rising linearly to
     * {@value #MAX_SCRIM} at the midpoint where both foreground polarities fail.
     *
     * @param backdropLuminance average backdrop luminance in {@code [0, 1]} (clamped)
     * @return the suggested scrim strength in {@code [0, 1]}
     */
    public static double scrimStrength(double backdropLuminance) {
        double distance = Math.abs(clamp01(backdropLuminance) - MIDPOINT);
        if (distance >= SCRIM_REACH) {
            return 0.0;
        }
        return MAX_SCRIM * (1.0 - distance / SCRIM_REACH);
    }

    /**
     * The readable foreground for text sitting on glass: the standard M3 on-surface token of the
     * baseline scheme matching the scene theme (light text on the dark scheme, dark text on the
     * light scheme). Skins should prefer their live-scene resolved token; this static form exists
     * for pure callers and previews.
     *
     * @param darkSceneTheme whether the scene uses the dark scheme
     * @return the on-glass text color, never {@code null}
     */
    public static Color readableTextOnGlass(boolean darkSceneTheme) {
        return darkSceneTheme ? DARK_THEME_TEXT : LIGHT_THEME_TEXT;
    }

    /**
     * Average relative luminance of an image (e.g. a {@code node.snapshot(...)} of the backdrop
     * region behind a glass pane): per-pixel sRGB&rarr;linear conversion, Rec. 709 luma weights,
     * transparent pixels excluded, sampled with a stride so large snapshots stay cheap (stride
     * chosen so the longest edge contributes at most ~256 samples; fully deterministic).
     *
     * @param image the sampled backdrop, may be {@code null}
     * @return mean relative luminance in {@code [0, 1]}; {@code 0.5} (neutral) for null/empty
     *         images so callers degrade to the most defensive suggestions
     */
    public static double averageLuminance(WritableImage image) {
        if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
            return MIDPOINT;
        }
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        int stride = Math.max(1, Math.max(w, h) / 256);
        double sum = 0;
        long count = 0;
        for (int y = 0; y < h; y += stride) {
            for (int x = 0; x < w; x += stride) {
                Color c = image.getPixelReader().getColor(x, y);
                if (c.getOpacity() < 0.05) {
                    continue;
                }
                double r = srgbToLinear(c.getRed());
                double g = srgbToLinear(c.getGreen());
                double b = srgbToLinear(c.getBlue());
                sum += 0.2126 * r + 0.7152 * g + 0.0722 * b;
                count++;
            }
        }
        return count == 0 ? MIDPOINT : clamp01(sum / count);
    }

    private static double srgbToLinear(double channel) {
        return channel <= 0.04045 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) {
            return MIDPOINT;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }
}
