package io.m3fx.controls.glass;

import javafx.scene.paint.Color;

/**
 * Immutable bundle of liquid glass configuration, mirroring the SwiftUI
 * {@code glassEffect(...)} modifier surface plus the reference-engine physics knobs
 * (liquidGL's {@code refraction/bevelDepth/bevelWidth/frost/aberration/magnify/interaction/tilt/draggable} and
 * nikdelvin's {@code depth/strength/button}). Apply a bundle to a pane with
 * {@link M3GlassPane#of(GlassEffectParams)} or {@link M3GlassPane#setEffect(GlassEffectParams)}.
 *
 * <p>Instances are immutable; the fluent methods return new bundles. Identity semantics (no
 * {@code equals}) &mdash; the pane mirrors bundle values into its live properties at
 * {@code setEffect} time, so later property changes on the pane do not rewrite the bundle.
 *
 * <p>Defaults are the reference engines' verbatim: liquidGL {@code refraction 0.01},
 * {@code bevelDepth 0.08}, {@code bevelWidth 0.15}, {@code magnify 1}, {@code tiltFactor 5},
 * {@code tiltEase 400}, {@code interactionStrength 0.5}, {@code interactionViscosity 0.65};
 * nikdelvin {@code depth 10}, {@code strength 100}. See {@code docs/SPEC_NOTES/liquid-glass.md}.
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
    private final double interactionStrength;
    private final double interactionViscosity;
    private final boolean tilt;
    private final double tiltFactor;
    private final double tiltEase;
    private final boolean draggable;
    private final boolean buttonMode;

    /** Creates the default bundle: regular variant, reference-engine defaults, capsule, radius 16. */
    public GlassEffectParams() {
        this(GlassVariant.REGULAR, null, false, GlassShape.CAPSULE, 16.0,
                MapStyle.LIQUIDGL, GlassRefraction.DEFAULT_REFRACTION,
                GlassRefraction.DEFAULT_BEVEL_DEPTH, GlassRefraction.DEFAULT_BEVEL_WIDTH,
                0.0, 0.0, GlassRefraction.DEFAULT_MAGNIFY,
                GlassRefraction.DEFAULT_NIKDELVIN_DEPTH, GlassRefraction.DEFAULT_NIKDELVIN_STRENGTH,
                false, 0.35, 0.5, 0.65, false, 5.0, 400.0, false, false);
    }

    @SuppressWarnings("this-escape")
    private GlassEffectParams(
            GlassVariant variant, Color tint, boolean interactive, GlassShape shape,
            double cornerRadius, MapStyle mapStyle, double refraction, double bevelDepth,
            double bevelWidth, double frost, double aberration, double magnify,
            double nikdelvinDepth, double nikdelvinStrength, boolean fluidInteraction,
            double interactionRadius, double interactionStrength, double interactionViscosity,
            boolean tilt, double tiltFactor, double tiltEase, boolean draggable, boolean buttonMode) {
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
        this.interactionStrength = interactionStrength;
        this.interactionViscosity = interactionViscosity;
        this.tilt = tilt;
        this.tiltFactor = tiltFactor;
        this.tiltEase = tiltEase;
        this.draggable = draggable;
        this.buttonMode = buttonMode;
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

    /** Returns the fluid-interaction displacement strength multiplier (default 0.5). */
    public double interactionStrength() {
        return interactionStrength;
    }

    /** Returns the fluid-interaction viscosity rate (default 0.65). */
    public double interactionViscosity() {
        return interactionViscosity;
    }

    /** Returns whether interactive 3D perspective tilt is enabled. */
    public boolean isTilt() {
        return tilt;
    }

    /** Returns the maximum 3D tilt angle in degrees (default 5.0). */
    public double tiltFactor() {
        return tiltFactor;
    }

    /** Returns the 3D tilt settle duration in milliseconds (default 400.0). */
    public double tiltEase() {
        return tiltEase;
    }

    /** Returns whether the lens can be dragged across its container. */
    public boolean isDraggable() {
        return draggable;
    }

    /** Returns whether Apple liquid glass button styling/mechanics are active. */
    public boolean isButtonMode() {
        return buttonMode;
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
                nikdelvinStrength, fluidInteraction, interactionRadius, interactionStrength,
                interactionViscosity, tilt, tiltFactor, tiltEase, draggable, buttonMode);
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
                nikdelvinDepth, nikdelvinStrength, fluidInteraction, interactionRadius,
                interactionStrength, interactionViscosity, tilt, tiltFactor, tiltEase, draggable,
                buttonMode);
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
                nikdelvinDepth, nikdelvinStrength, fluidInteraction, interactionRadius,
                interactionStrength, interactionViscosity, tilt, tiltFactor, tiltEase, draggable,
                buttonMode);
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
        return new GlassEffectParams(variant, tint, interactive, shape, cornerRadius,
                newMapStyle == null ? mapStyle : newMapStyle, refraction, bevelDepth, bevelWidth,
                frost, aberration, magnify, nikdelvinDepth, nikdelvinStrength, fluidInteraction,
                interactionRadius, interactionStrength, interactionViscosity, tilt, tiltFactor,
                tiltEase, draggable, buttonMode);
    }

    /**
     * Returns a copy with the given liquidGL refraction strength (UV units).
     *
     * @param value refraction strength
     * @return a new immutable bundle
     */
    public GlassEffectParams refraction(double value) {
        return new GlassEffectParams(variant, tint, interactive, shape, cornerRadius,
                mapStyle, value, bevelDepth, bevelWidth, frost, aberration, magnify,
                nikdelvinDepth, nikdelvinStrength, fluidInteraction, interactionRadius,
                interactionStrength, interactionViscosity, tilt, tiltFactor, tiltEase, draggable,
                buttonMode);
    }

    /**
     * Returns a copy with the given bevel depth (UV, the {@code edge^10} rim term).
     *
     * @param value bevel depth
     * @return a new immutable bundle
     */
    public GlassEffectParams bevelDepth(double value) {
        return new GlassEffectParams(variant, tint, interactive, shape, cornerRadius,
                mapStyle, refraction, value, bevelWidth, frost, aberration, magnify,
                nikdelvinDepth, nikdelvinStrength, fluidInteraction, interactionRadius,
                interactionStrength, interactionViscosity, tilt, tiltFactor, tiltEase, draggable,
                buttonMode);
    }

    /**
     * Returns a copy with the given bevel width (fraction of the smaller side).
     *
     * @param value bevel width
     * @return a new immutable bundle
     */
    public GlassEffectParams bevelWidth(double value) {
        return new GlassEffectParams(variant, tint, interactive, shape, cornerRadius,
                mapStyle, refraction, bevelDepth, value, frost, aberration, magnify,
                nikdelvinDepth, nikdelvinStrength, fluidInteraction, interactionRadius,
                interactionStrength, interactionViscosity, tilt, tiltFactor, tiltEase, draggable,
                buttonMode);
    }

    /**
     * Returns a copy with the given frost blur strength (liquidGL {@code u_frost}).
     *
     * @param value frost strength (0 = sharp)
     * @return a new immutable bundle
     */
    public GlassEffectParams frost(double value) {
        return new GlassEffectParams(variant, tint, interactive, shape, cornerRadius,
                mapStyle, refraction, bevelDepth, bevelWidth, value, aberration, magnify,
                nikdelvinDepth, nikdelvinStrength, fluidInteraction, interactionRadius,
                interactionStrength, interactionViscosity, tilt, tiltFactor, tiltEase, draggable,
                buttonMode);
    }

    /**
     * Returns a copy with the given chromatic-aberration strength in px.
     *
     * @param value aberration strength (0 = off)
     * @return a new immutable bundle
     */
    public GlassEffectParams aberration(double value) {
        return new GlassEffectParams(variant, tint, interactive, shape, cornerRadius,
                mapStyle, refraction, bevelDepth, bevelWidth, frost, value, magnify,
                nikdelvinDepth, nikdelvinStrength, fluidInteraction, interactionRadius,
                interactionStrength, interactionViscosity, tilt, tiltFactor, tiltEase, draggable,
                buttonMode);
    }

    /**
     * Returns a copy with the given backdrop magnification.
     *
     * @param value magnification factor (1 = none)
     * @return a new immutable bundle
     */
    public GlassEffectParams magnify(double value) {
        return new GlassEffectParams(variant, tint, interactive, shape, cornerRadius,
                mapStyle, refraction, bevelDepth, bevelWidth, frost, aberration, value,
                nikdelvinDepth, nikdelvinStrength, fluidInteraction, interactionRadius,
                interactionStrength, interactionViscosity, tilt, tiltFactor, tiltEase, draggable,
                buttonMode);
    }

    /**
     * Returns a copy with the nikdelvin bezel depth in px (SVG_DISPLACEMENT style).
     *
     * @param value bezel depth in px
     * @return a new immutable bundle
     */
    public GlassEffectParams nikdelvinDepth(double value) {
        return new GlassEffectParams(variant, tint, interactive, shape, cornerRadius,
                mapStyle, refraction, bevelDepth, bevelWidth, frost, aberration, magnify,
                value, nikdelvinStrength, fluidInteraction, interactionRadius,
                interactionStrength, interactionViscosity, tilt, tiltFactor, tiltEase, draggable,
                buttonMode);
    }

    /**
     * Returns a copy with the nikdelvin displacement scale in px (SVG_DISPLACEMENT style).
     *
     * @param value displacement scale in px
     * @return a new immutable bundle
     */
    public GlassEffectParams nikdelvinStrength(double value) {
        return new GlassEffectParams(variant, tint, interactive, shape, cornerRadius,
                mapStyle, refraction, bevelDepth, bevelWidth, frost, aberration, magnify,
                nikdelvinDepth, value, fluidInteraction, interactionRadius,
                interactionStrength, interactionViscosity, tilt, tiltFactor, tiltEase, draggable,
                buttonMode);
    }

    /**
     * Returns a copy with the pointer-deformed fluid interaction enabled or disabled
     * (liquidGL {@code interaction: "fluid"}).
     *
     * @param enabled {@code true} to enable
     * @return a new immutable bundle
     */
    public GlassEffectParams fluidInteraction(boolean enabled) {
        return new GlassEffectParams(variant, tint, interactive, shape, cornerRadius,
                mapStyle, refraction, bevelDepth, bevelWidth, frost, aberration, magnify,
                nikdelvinDepth, nikdelvinStrength, enabled, interactionRadius,
                interactionStrength, interactionViscosity, tilt, tiltFactor, tiltEase, draggable,
                buttonMode);
    }

    /**
     * Returns a copy with fluid interaction configured with reach, strength and viscosity.
     *
     * @param reach radius reach fraction
     * @param strength displacement strength multiplier
     * @param viscosity settling viscosity
     * @return a new immutable bundle
     */
    public GlassEffectParams fluidInteraction(double reach, double strength, double viscosity) {
        return new GlassEffectParams(variant, tint, interactive, shape, cornerRadius,
                mapStyle, refraction, bevelDepth, bevelWidth, frost, aberration, magnify,
                nikdelvinDepth, nikdelvinStrength, true, reach, strength, viscosity,
                tilt, tiltFactor, tiltEase, draggable, buttonMode);
    }

    /**
     * Returns a copy with the fluid-interaction reach (fraction of the smaller side).
     *
     * @param value reach fraction (default 0.35)
     * @return a new immutable bundle
     */
    public GlassEffectParams interactionRadius(double value) {
        return new GlassEffectParams(variant, tint, interactive, shape, cornerRadius,
                mapStyle, refraction, bevelDepth, bevelWidth, frost, aberration, magnify,
                nikdelvinDepth, nikdelvinStrength, fluidInteraction, value,
                interactionStrength, interactionViscosity, tilt, tiltFactor, tiltEase, draggable,
                buttonMode);
    }

    /**
     * Returns a copy with interactive 3D perspective tilt enabled or disabled.
     *
     * @param enabled {@code true} to enable 3D tilt
     * @return a new immutable bundle
     */
    public GlassEffectParams tilt(boolean enabled) {
        return new GlassEffectParams(variant, tint, interactive, shape, cornerRadius,
                mapStyle, refraction, bevelDepth, bevelWidth, frost, aberration, magnify,
                nikdelvinDepth, nikdelvinStrength, fluidInteraction, interactionRadius,
                interactionStrength, interactionViscosity, enabled, tiltFactor, tiltEase, draggable,
                buttonMode);
    }

    /**
     * Returns a copy with interactive 3D perspective tilt configured.
     *
     * @param factor maximum tilt angle in degrees
     * @param ease settle duration in ms
     * @return a new immutable bundle
     */
    public GlassEffectParams tilt(double factor, double ease) {
        return new GlassEffectParams(variant, tint, interactive, shape, cornerRadius,
                mapStyle, refraction, bevelDepth, bevelWidth, frost, aberration, magnify,
                nikdelvinDepth, nikdelvinStrength, fluidInteraction, interactionRadius,
                interactionStrength, interactionViscosity, true, factor, ease, draggable,
                buttonMode);
    }

    /**
     * Returns a copy with draggable lens capability enabled or disabled.
     *
     * @param enabled {@code true} to enable drag
     * @return a new immutable bundle
     */
    public GlassEffectParams draggable(boolean enabled) {
        return new GlassEffectParams(variant, tint, interactive, shape, cornerRadius,
                mapStyle, refraction, bevelDepth, bevelWidth, frost, aberration, magnify,
                nikdelvinDepth, nikdelvinStrength, fluidInteraction, interactionRadius,
                interactionStrength, interactionViscosity, tilt, tiltFactor, tiltEase, enabled,
                buttonMode);
    }

    /**
     * Returns a copy configured as an Apple Liquid Glass button.
     *
     * @return a new immutable bundle
     */
    public GlassEffectParams asButton() {
        return new GlassEffectParams(variant, tint, true, shape, cornerRadius,
                mapStyle, refraction, bevelDepth, bevelWidth, frost, aberration, magnify,
                nikdelvinDepth, nikdelvinStrength, fluidInteraction, interactionRadius,
                interactionStrength, interactionViscosity, tilt, tiltFactor, tiltEase, draggable,
                true);
    }
}
