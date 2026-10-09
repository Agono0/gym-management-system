package io.m3fx.core.theme;

import io.m3fx.core.color.dynamiccolor.DynamicScheme;
import io.m3fx.core.color.dynamiccolor.MaterialDynamicColors;
import io.m3fx.core.color.hct.Hct;
import io.m3fx.core.color.scheme.SchemeContent;
import io.m3fx.core.color.scheme.SchemeExpressive;
import io.m3fx.core.color.scheme.SchemeFidelity;
import io.m3fx.core.color.scheme.SchemeFruitSalad;
import io.m3fx.core.color.scheme.SchemeMonochrome;
import io.m3fx.core.color.scheme.SchemeNeutral;
import io.m3fx.core.color.scheme.SchemeRainbow;
import io.m3fx.core.color.scheme.SchemeTonalSpot;
import io.m3fx.core.color.scheme.SchemeVibrant;
import java.util.EnumMap;
import java.util.Map;
import javafx.scene.paint.Color;

/**
 * An M3 dynamic-color theme generated from a seed color.
 *
 * <p>See <a href="https://m3.material.io/styles/color/dynamic">M3 dynamic color</a>. Wraps the
 * ported {@code material-color-utilities} schemes (Google, Apache 2.0) in a JavaFX-friendly API:
 * every {@link ColorRole} resolves to a {@link Color}, and {@link #toCss()} emits the
 * {@code -md-sys-color-*} looked-up colors that all control stylesheets reference, so switching
 * themes restyles every control at once.
 */
public final class M3Theme {

    /** Standard contrast. */
    public static final double CONTRAST_STANDARD = 0.0;
    /** Medium contrast. */
    public static final double CONTRAST_MEDIUM = 0.5;
    /** High contrast. */
    public static final double CONTRAST_HIGH = 1.0;

    private final Color seed;
    private final ThemeVariant variant;
    private final boolean dark;
    private final double contrast;
    private final Map<ColorRole, Color> colors;

    private M3Theme(Color seed, ThemeVariant variant, boolean dark, double contrast) {
        this.seed = seed;
        this.variant = variant;
        this.dark = dark;
        this.contrast = contrast;
        this.colors = resolve(seed, variant, dark, contrast);
    }

    /**
     * Creates a theme from a seed color.
     *
     * @param seed source color the palettes are derived from
     * @param variant dynamic-color variant
     * @param dark {@code true} for the dark scheme
     * @param contrast contrast level, one of {@link #CONTRAST_STANDARD}, {@link #CONTRAST_MEDIUM},
     *     {@link #CONTRAST_HIGH}
     * @return the resolved theme
     */
    public static M3Theme fromSeed(Color seed, ThemeVariant variant, boolean dark, double contrast) {
        if (seed == null) {
            throw new IllegalArgumentException("seed must not be null");
        }
        if (variant == null) {
            throw new IllegalArgumentException("variant must not be null");
        }
        return new M3Theme(seed, variant, dark, contrast);
    }

    /**
     * Creates a theme with standard contrast.
     *
     * @param seed source color
     * @param variant dynamic-color variant
     * @param dark {@code true} for dark
     * @return the resolved theme
     */
    public static M3Theme fromSeed(Color seed, ThemeVariant variant, boolean dark) {
        return fromSeed(seed, variant, dark, CONTRAST_STANDARD);
    }

    /**
     * Creates the baseline M3 theme (TonalSpot, seed #6750A4) used by the spec examples.
     *
     * @param dark {@code true} for dark
     * @return the baseline theme
     */
    public static M3Theme baseline(boolean dark) {
        return fromSeed(Color.web("#6750A4"), ThemeVariant.TONAL_SPOT, dark, CONTRAST_STANDARD);
    }

    /** Returns the seed color. */
    public Color seed() {
        return seed;
    }

    /** Returns the variant. */
    public ThemeVariant variant() {
        return variant;
    }

    /** Returns {@code true} for the dark scheme. */
    public boolean dark() {
        return dark;
    }

    /** Returns the contrast level. */
    public double contrast() {
        return contrast;
    }

    /**
     * Returns the resolved color for a role.
     *
     * @param role the color role
     * @return the JavaFX color
     */
    public Color get(ColorRole role) {
        return colors.get(role);
    }

    /**
     * Generates the theme stylesheet body: a {@code .root} block defining every
     * {@code -md-sys-color-*} looked-up color plus the seed/variant marker classes.
     *
     * <p>For every role, translucent state variants are also emitted
     * ({@code -md-sys-color-<role>-a08/-a10/-a12/-a16/-a38} for hover, focus/pressed, disabled
     * container, dragged and disabled content). Because they are computed from the resolved theme
     * colors, layered state backgrounds stay correct in dark and high-contrast themes.
     *
     * @return CSS text to add to a scene (via {@code scene.getStylesheets()} data URL or a
     *     generated file)
     */
    public String toCss() {
        StringBuilder sb = new StringBuilder(8192);
        sb.append(".root {\n");
        for (ColorRole role : ColorRole.values()) {
            Color color = get(role);
            sb.append("  ").append(role.cssVariable()).append(": ").append(toWeb(color)).append(";\n");
            sb.append("  ").append(role.cssVariable()).append("-a08: ").append(toRgba(color, 0.08)).append(";\n");
            sb.append("  ").append(role.cssVariable()).append("-a10: ").append(toRgba(color, 0.10)).append(";\n");
            sb.append("  ").append(role.cssVariable()).append("-a12: ").append(toRgba(color, 0.12)).append(";\n");
            sb.append("  ").append(role.cssVariable()).append("-a16: ").append(toRgba(color, 0.16)).append(";\n");
            sb.append("  ").append(role.cssVariable()).append("-a15: ").append(toRgba(color, 0.15)).append(";\n");
            sb.append("  ").append(role.cssVariable()).append("-a18: ").append(toRgba(color, 0.18)).append(";\n");
            sb.append("  ").append(role.cssVariable()).append("-a20: ").append(toRgba(color, 0.20)).append(";\n");
            sb.append("  ").append(role.cssVariable()).append("-a22: ").append(toRgba(color, 0.22)).append(";\n");
            sb.append("  ").append(role.cssVariable()).append("-a25: ").append(toRgba(color, 0.25)).append(";\n");
            sb.append("  ").append(role.cssVariable()).append("-a38: ").append(toRgba(color, 0.38)).append(";\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Formats a color as a CSS hex string.
     *
     * @param color the color
     * @return {@code #rrggbb} text
     */
    public static String toWeb(Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    /**
     * Formats a color as a CSS rgba string.
     *
     * @param color the color
     * @param alpha alpha 0-1
     * @return {@code rgba(r, g, b, a)} text
     */
    public static String toRgba(Color color, double alpha) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("rgba(%d, %d, %d, %.2f)", r, g, b, alpha);
    }

    /**
     * Converts a JavaFX color to the ARGB int used by material-color-utilities.
     *
     * @param color the color
     * @return ARGB int
     */
    public static int toArgb(Color color) {
        int a = (int) Math.round(color.getOpacity() * 255);
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Converts an ARGB int from material-color-utilities to a JavaFX color.
     *
     * @param argb ARGB int
     * @return the JavaFX color
     */
    public static Color fromArgb(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return Color.rgb(r, g, b, a / 255.0);
    }

    private static Map<ColorRole, Color> resolve(
            Color seed, ThemeVariant variant, boolean dark, double contrast) {
        Hct source = Hct.fromInt(toArgb(seed));
        DynamicScheme scheme =
                switch (variant) {
                    case VIBRANT -> new SchemeVibrant(source, dark, contrast);
                    case EXPRESSIVE -> new SchemeExpressive(source, dark, contrast);
                    case NEUTRAL -> new SchemeNeutral(source, dark, contrast);
                    case MONOCHROME -> new SchemeMonochrome(source, dark, contrast);
                    case FIDELITY -> new SchemeFidelity(source, dark, contrast);
                    case CONTENT -> new SchemeContent(source, dark, contrast);
                    case RAINBOW -> new SchemeRainbow(source, dark, contrast);
                    case FRUIT_SALAD -> new SchemeFruitSalad(source, dark, contrast);
                    default -> new SchemeTonalSpot(source, dark, contrast);
                };
        MaterialDynamicColors mdc = new MaterialDynamicColors();
        Map<ColorRole, Color> map = new EnumMap<>(ColorRole.class);
        map.put(ColorRole.PRIMARY, fromArgb(mdc.primary().getArgb(scheme)));
        map.put(ColorRole.PRIMARY_DIM, fromArgb(mdc.primaryDim().getArgb(scheme)));
        map.put(ColorRole.ON_PRIMARY, fromArgb(mdc.onPrimary().getArgb(scheme)));
        map.put(ColorRole.PRIMARY_CONTAINER, fromArgb(mdc.primaryContainer().getArgb(scheme)));
        map.put(ColorRole.ON_PRIMARY_CONTAINER, fromArgb(mdc.onPrimaryContainer().getArgb(scheme)));
        map.put(ColorRole.PRIMARY_FIXED, fromArgb(mdc.primaryFixed().getArgb(scheme)));
        map.put(ColorRole.PRIMARY_FIXED_DIM, fromArgb(mdc.primaryFixedDim().getArgb(scheme)));
        map.put(ColorRole.ON_PRIMARY_FIXED, fromArgb(mdc.onPrimaryFixed().getArgb(scheme)));
        map.put(
                ColorRole.ON_PRIMARY_FIXED_VARIANT,
                fromArgb(mdc.onPrimaryFixedVariant().getArgb(scheme)));
        map.put(ColorRole.INVERSE_PRIMARY, fromArgb(mdc.inversePrimary().getArgb(scheme)));
        map.put(ColorRole.SECONDARY, fromArgb(mdc.secondary().getArgb(scheme)));
        map.put(ColorRole.SECONDARY_DIM, fromArgb(mdc.secondaryDim().getArgb(scheme)));
        map.put(ColorRole.ON_SECONDARY, fromArgb(mdc.onSecondary().getArgb(scheme)));
        map.put(ColorRole.SECONDARY_CONTAINER, fromArgb(mdc.secondaryContainer().getArgb(scheme)));
        map.put(
                ColorRole.ON_SECONDARY_CONTAINER,
                fromArgb(mdc.onSecondaryContainer().getArgb(scheme)));
        map.put(ColorRole.SECONDARY_FIXED, fromArgb(mdc.secondaryFixed().getArgb(scheme)));
        map.put(ColorRole.SECONDARY_FIXED_DIM, fromArgb(mdc.secondaryFixedDim().getArgb(scheme)));
        map.put(ColorRole.ON_SECONDARY_FIXED, fromArgb(mdc.onSecondaryFixed().getArgb(scheme)));
        map.put(
                ColorRole.ON_SECONDARY_FIXED_VARIANT,
                fromArgb(mdc.onSecondaryFixedVariant().getArgb(scheme)));
        map.put(ColorRole.TERTIARY, fromArgb(mdc.tertiary().getArgb(scheme)));
        map.put(ColorRole.TERTIARY_DIM, fromArgb(mdc.tertiaryDim().getArgb(scheme)));
        map.put(ColorRole.ON_TERTIARY, fromArgb(mdc.onTertiary().getArgb(scheme)));
        map.put(ColorRole.TERTIARY_CONTAINER, fromArgb(mdc.tertiaryContainer().getArgb(scheme)));
        map.put(
                ColorRole.ON_TERTIARY_CONTAINER,
                fromArgb(mdc.onTertiaryContainer().getArgb(scheme)));
        map.put(ColorRole.TERTIARY_FIXED, fromArgb(mdc.tertiaryFixed().getArgb(scheme)));
        map.put(ColorRole.TERTIARY_FIXED_DIM, fromArgb(mdc.tertiaryFixedDim().getArgb(scheme)));
        map.put(ColorRole.ON_TERTIARY_FIXED, fromArgb(mdc.onTertiaryFixed().getArgb(scheme)));
        map.put(
                ColorRole.ON_TERTIARY_FIXED_VARIANT,
                fromArgb(mdc.onTertiaryFixedVariant().getArgb(scheme)));
        map.put(ColorRole.ERROR, fromArgb(mdc.error().getArgb(scheme)));
        map.put(ColorRole.ERROR_DIM, fromArgb(mdc.errorDim().getArgb(scheme)));
        map.put(ColorRole.ON_ERROR, fromArgb(mdc.onError().getArgb(scheme)));
        map.put(ColorRole.ERROR_CONTAINER, fromArgb(mdc.errorContainer().getArgb(scheme)));
        map.put(ColorRole.ON_ERROR_CONTAINER, fromArgb(mdc.onErrorContainer().getArgb(scheme)));
        map.put(ColorRole.BACKGROUND, fromArgb(mdc.background().getArgb(scheme)));
        map.put(ColorRole.ON_BACKGROUND, fromArgb(mdc.onBackground().getArgb(scheme)));
        map.put(ColorRole.SURFACE, fromArgb(mdc.surface().getArgb(scheme)));
        map.put(ColorRole.ON_SURFACE, fromArgb(mdc.onSurface().getArgb(scheme)));
        map.put(ColorRole.SURFACE_VARIANT, fromArgb(mdc.surfaceVariant().getArgb(scheme)));
        map.put(ColorRole.ON_SURFACE_VARIANT, fromArgb(mdc.onSurfaceVariant().getArgb(scheme)));
        map.put(ColorRole.SURFACE_DIM, fromArgb(mdc.surfaceDim().getArgb(scheme)));
        map.put(ColorRole.SURFACE_BRIGHT, fromArgb(mdc.surfaceBright().getArgb(scheme)));
        map.put(
                ColorRole.SURFACE_CONTAINER_LOWEST,
                fromArgb(mdc.surfaceContainerLowest().getArgb(scheme)));
        map.put(
                ColorRole.SURFACE_CONTAINER_LOW,
                fromArgb(mdc.surfaceContainerLow().getArgb(scheme)));
        map.put(ColorRole.SURFACE_CONTAINER, fromArgb(mdc.surfaceContainer().getArgb(scheme)));
        map.put(
                ColorRole.SURFACE_CONTAINER_HIGH,
                fromArgb(mdc.surfaceContainerHigh().getArgb(scheme)));
        map.put(
                ColorRole.SURFACE_CONTAINER_HIGHEST,
                fromArgb(mdc.surfaceContainerHighest().getArgb(scheme)));
        map.put(ColorRole.SURFACE_TINT, fromArgb(mdc.surfaceTint().getArgb(scheme)));
        map.put(ColorRole.INVERSE_SURFACE, fromArgb(mdc.inverseSurface().getArgb(scheme)));
        map.put(ColorRole.INVERSE_ON_SURFACE, fromArgb(mdc.inverseOnSurface().getArgb(scheme)));
        map.put(ColorRole.OUTLINE, fromArgb(mdc.outline().getArgb(scheme)));
        map.put(ColorRole.OUTLINE_VARIANT, fromArgb(mdc.outlineVariant().getArgb(scheme)));
        map.put(ColorRole.SCRIM, fromArgb(mdc.scrim().getArgb(scheme)));
        map.put(ColorRole.SHADOW, fromArgb(mdc.shadow().getArgb(scheme)));
        return map;
    }
}
