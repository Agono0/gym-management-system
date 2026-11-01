package io.m3fx.controls.glass;

import javafx.scene.paint.Color;

/**
 * Immutable bundle of liquid glass configuration, mirroring the SwiftUI
 * {@code glassEffect(...)} modifier surface plus the reference-engine physics knobs
 * (liquidGL's {@code refraction/bevelDepth/bevelWidth/frost/aberration/magnify/interaction} and
 * nikdelvin's {@code depth/strength}). Apply a bundle to a pane with
 * {@link M3GlassPane#of(GlassEffectParams)} or {@link M3GlassPane#setEffect(GlassEffectParams)}.
 *
 * <p>Instances are immutable; the fluent methods return new bundles. Identity semantics (no
 * {@code equals}) &mdash; the pane mirrors bundle values into its live properties at
 * {@code setEffect} time, so later property changes on the pane do not rewrite the bundle.
 *
 * <p>Defaults are the reference engines' verbatim: liquidGL {@code refraction 0.01},
 * {@code bevelDepth 0.08}, {@code bevelWidth 0.15}, {@code magnify 1}; nikdelvin
 * {@code depth 10}, {@code strength 100}. See {@code docs/SPEC_NOTES/liquid-glass.md}.
 */
public final class GlassEffectParams {

    /** Displacement-field source. */
    public enum MapStyle {
        /** liquidGL shader port: SDF rim refraction (default). */
        LIQUIDGL,
        /** nikdelvin SVG displacement-map port (gradient screen construction). */
        SVG_DISPLACEMENT
    }

    private final GlassVariant variant;
    private final Color tint;
    private final boolean interactive;
    private final GlassShape shape;
    private final double cornerRadius;
    private final MapStyle mapStyle;
    private final double refraction;
    private final double bevelDepth;
    private final double bevelWidth;
    private final double frost;
    private final double aberration;
    private final double magnify;
    private final double nikdelvinDepth;
    private final double nikdelvinStrength;
    private final boolean fluidInteraction;
    private final double interactionRadius;

    /** Creates the default bundle: regular variant, reference-engine defaults, capsule, radius 16. */
    public GlassEffectParams() {
        this(GlassVariant.REGULAR, null, false, GlassShape.CAPSULE, 16.0,
                MapStyle.LIQUIDGL, GlassRefraction.DEFAULT_REFRACTION,
                GlassRefraction.DEFAULT_BEVEL_DEPTH, GlassRefraction.DEFAULT_BEVEL_WIDTH,
                0.0, 0.0, GlassRefraction.DEFAULT_MAGNIFY,
                GlassRefraction.DEFAULT_NIKDELVIN_DEPTH, GlassRefraction.DEFAULT_NIKDELVIN_STRENGTH,
                false, 0.35);
    }

    @SuppressWarnings("this-escape")
    private GlassEffectParams(
            GlassVariant variant, Color tint, boolean interactive, GlassShape shape,
            double cornerRadius, MapStyle mapStyle, double refraction, double bevelDepth,
            double bevelWidth, double frost, double aberration, double magnify,
            double nikdelvinDepth, double nikdelvinStrength, boolean fluidInteraction,
            double interactionRadius) {
        this.variant = variant == null ? GlassVariant.REGULAR : variant;
        this.tint = tint;
        this.interactive = interactive;
        this.shape = shape == null ? GlassShape.CAPSULE : shape;
        this.cornerRadius = cornerRadius;
        this.mapStyle = mapStyle == null ? MapStyle.LIQUIDGL : mapStyle;
        this.refraction = refraction;
        this.bevelDepth = bevelDepth;
        this.bevelWidth = bevelWidth;
        this.frost = frost;
        this.aberration = aberration;
        this.magnify = magnify;
        this.nikdelvinDepth = nikdelvinDepth;
        this.nikdelvinStrength = nikdelvinStrength;
        this.fluidInteraction = fluidInteraction;
        this.interactionRadius = interactionRadius;
    }

    /** Returns the glass variant (never {@code null}). */
    public GlassVariant variant() {
        return variant;
    }

    /** Returns the tint color, or {@code null} for untinted glass. */
    public Color tint() {
        return tint;
    }

    /** Returns whether the glass reacts to the pointer (bevel flatten + specular shift). */
    public boolean isInteractive() {
        return interactive;
    }

    /** Returns the glass silhouette (never {@code null}). */
    public GlassShape shape() {
        return shape;
    }

    /** Returns the corner radius in px for {@link GlassShape#RECT}. */
    public double cornerRadius() {
        return cornerRadius;
    }

    /** Returns the displacement-field source (never {@code null}). */
    public MapStyle mapStyle() {
        return mapStyle;
    }

    /** Returns the liquidGL refraction strength in UV units (default 0.01). */
    public double refraction() {
        return refraction;
    }

    /** Returns the bevel depth (UV, the {@code edge^10} term, default 0.08). */
    public double bevelDepth() {
        return bevelDepth;
    }

    /** Returns the bevel width as a fraction of the smaller side (default 0.15). */
    public double bevelWidth() {
        return bevelWidth;
    }

    /** Returns the frost blur strength (0 = sharp, liquidGL {@code u_frost}). */
    public double frost() {
        return frost;
    }

    /** Returns the chromatic-aberration strength in px (0 = off). */
    public double aberration() {
        return aberration;
    }

    /** Returns the backdrop magnification (default 1). */
    public double magnify() {
        return magnify;
    }

    /** Returns the nikdelvin bezel depth in px (SVG_DISPLACEMENT style). */
    public double nikdelvinDepth() {
        return nikdelvinDepth;
    }

    /** Returns the nikdelvin displacement scale in px (SVG_DISPLACEMENT style). */
    public double nikdelvinStrength() {
        return nikdelvinStrength;
    }

    /** Returns whether the pointer-deformed fluid interaction is enabled. */
    public boolean isFluidInteraction() {
        return fluidInteraction;
    }

    /** Returns the fluid-interaction reach as a fraction of the smaller side (default 0.35). */
    public double interactionRadius() {
        return interactionRadius;
    }

    /**
     * Returns a copy with the given variant.
     *
     * @param newVariant the variant
     * @return a new immutable bundle
     */
    public GlassEffectParams variant(GlassVariant newVariant) {
        return copy(newVariant, shape, cornerRadius);
    }

    private GlassEffectParams copy(GlassVariant newVariant, GlassShape newShape, double radius) {
        return new GlassEffectParams(newVariant, tint, interactive, newShape, radius, mapStyle,
                refraction, bevelDepth, bevelWidth, frost, aberration, magnify, nikdelvinDepth,
                nikdelvinStrength, fluidInteraction, interactionRadius);
    }

    private GlassEffectParams copyAll(MapStyle newMapStyle, Double newRefraction,
                                      Double newBevelDepth, Double newBevelWidth, Double newFrost,
                                      Double newAberration, Double newMagnify,
                                      Double newNikdelvinDepth, Double newNikdelvinStrength,
                                      Boolean newFluidInteraction, Double newInteractionRadius) {
        return new GlassEffectParams(variant, tint, interactive, shape, cornerRadius,
                newMapStyle == null ? mapStyle : newMapStyle,
                newRefraction == null ? refraction : newRefraction,
                newBevelDepth == null ? bevelDepth : newBevelDepth,
                newBevelWidth == null ? bevelWidth : newBevelWidth,
                newFrost == null ? frost : newFrost,
                newAberration == null ? aberration : newAberration,
                newMagnify == null ? magnify : newMagnify,
                newNikdelvinDepth == null ? nikdelvinDepth : newNikdelvinDepth,
                newNikdelvinStrength == null ? nikdelvinStrength : newNikdelvinStrength,
                newFluidInteraction == null ? fluidInteraction : newFluidInteraction,
                newInteractionRadius == null ? interactionRadius : newInteractionRadius);
    }

    /**
     * Returns a copy with the given prominence tint (rendered at 0.25 alpha per the spec).
     *
     * @param tintColor the tint color, {@code null} to clear
     * @return a new immutable bundle
     */
    public GlassEffectParams tint(Color tintColor) {
        return new GlassEffectParams(variant, tintColor, interactive, shape, cornerRadius,
                mapStyle, refraction, bevelDepth, bevelWidth, frost, aberration, magnify,
                nikdelvinDepth, nikdelvinStrength, fluidInteraction, interactionRadius);
    }

    /**
     * Returns a copy flagged interactive: pressing flattens the bevel (~40% less refraction)
     * and deepens the shadow; hovering shifts the specular highlight toward the pointer.
     *
     * @return a new immutable bundle
     */
    public GlassEffectParams interactive() {
        return new GlassEffectParams(variant, tint, true, shape, cornerRadius, mapStyle,
                refraction, bevelDepth, bevelWidth, frost, aberration, magnify,
                nikdelvinDepth, nikdelvinStrength, fluidInteraction, interactionRadius);
    }

    /**
     * Returns a copy with the given shape and corner radius.
     *
     * @param newShape the silhouette
     * @param radius the corner radius in px (used by {@link GlassShape#RECT})
     * @return a new immutable bundle
     */
    public GlassEffectParams in(GlassShape newShape, double radius) {
        return copy(variant, newShape, radius);
    }

    /**
     * Returns a copy with the given displacement-field source.
     *
     * @param newMapStyle the map style
     * @return a new immutable bundle
     */
    public GlassEffectParams mapStyle(MapStyle newMapStyle) {
        return copyAll(newMapStyle, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Returns a copy with the given liquidGL refraction strength (UV units).
     *
     * @param value refraction strength
     * @return a new immutable bundle
     */
    public GlassEffectParams refraction(double value) {
        return copyAll(null, value, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Returns a copy with the given bevel depth (UV, the {@code edge^10} rim term).
     *
     * @param value bevel depth
     * @return a new immutable bundle
     */
    public GlassEffectParams bevelDepth(double value) {
        return copyAll(null, null, value, null, null, null, null, null, null, null, null);
    }

    /**
     * Returns a copy with the given bevel width (fraction of the smaller side).
     *
     * @param value bevel width
     * @return a new immutable bundle
     */
    public GlassEffectParams bevelWidth(double value) {
        return copyAll(null, null, null, value, null, null, null, null, null, null, null);
    }

    /**
     * Returns a copy with the given frost blur strength (liquidGL {@code u_frost}).
     *
     * @param value frost strength (0 = sharp)
     * @return a new immutable bundle
     */
    public GlassEffectParams frost(double value) {
        return copyAll(null, null, null, null, value, null, null, null, null, null, null);
    }

    /**
     * Returns a copy with the given chromatic-aberration strength in px.
     *
     * @param value aberration strength (0 = off)
     * @return a new immutable bundle
     */
    public GlassEffectParams aberration(double value) {
        return copyAll(null, null, null, null, null, value, null, null, null, null, null);
    }

    /**
     * Returns a copy with the given backdrop magnification.
     *
     * @param value magnification factor (1 = none)
     * @return a new immutable bundle
     */
    public GlassEffectParams magnify(double value) {
        return copyAll(null, null, null, null, null, null, value, null, null, null, null);
    }

    /**
     * Returns a copy with the nikdelvin bezel depth in px (SVG_DISPLACEMENT style).
     *
     * @param value bezel depth in px
     * @return a new immutable bundle
     */
    public GlassEffectParams nikdelvinDepth(double value) {
        return copyAll(null, null, null, null, null, null, null, value, null, null, null);
    }

    /**
     * Returns a copy with the nikdelvin displacement scale in px (SVG_DISPLACEMENT style).
     *
     * @param value displacement scale in px
     * @return a new immutable bundle
     */
    public GlassEffectParams nikdelvinStrength(double value) {
        return copyAll(null, null, null, null, null, null, null, null, value, null, null);
    }

    /**
     * Returns a copy with the pointer-deformed fluid interaction enabled or disabled
     * (liquidGL {@code interaction: "fluid"}).
     *
     * @param enabled {@code true} to enable
     * @return a new immutable bundle
     */
    public GlassEffectParams fluidInteraction(boolean enabled) {
        return copyAll(null, null, null, null, null, null, null, null, null, enabled, null);
    }

    /**
     * Returns a copy with the fluid-interaction reach (fraction of the smaller side).
     *
     * @param value reach fraction (default 0.35)
     * @return a new immutable bundle
     */
    public GlassEffectParams interactionRadius(double value) {
        return copyAll(null, null, null, null, null, null, null, null, null, null, value);
    }
}
