package io.m3fx.controls.glass;

import javafx.scene.paint.Color;

/**
 * Immutable bundle of liquid glass configuration, mirroring the SwiftUI
 * {@code glassEffect(...)} modifier surface: variant, optional tint, interactivity, shape and
 * corner radius. Apply a bundle to a pane with {@link M3GlassPane#of(GlassEffectParams)} or
 * {@link M3GlassPane#setEffect(GlassEffectParams)}.
 *
 * <p>Instances are immutable; the fluent methods return new bundles. Identity semantics (no
 * {@code equals}) &mdash; the pane mirrors bundle values into its live properties at
 * {@code setEffect} time, so later property changes on the pane do not rewrite the bundle.
 *
 * <p>See {@code docs/SPEC_NOTES/liquid-glass.md}.
 */
public final class GlassEffectParams {

    private final GlassVariant variant;
    private final Color tint;
    private final boolean interactive;
    private final GlassShape shape;
    private final double cornerRadius;

    /** Creates the default bundle: regular variant, no tint, not interactive, capsule, radius 16. */
    public GlassEffectParams() {
        this(GlassVariant.REGULAR, null, false, GlassShape.CAPSULE, 16.0);
    }

    private GlassEffectParams(
            GlassVariant variant, Color tint, boolean interactive, GlassShape shape,
            double cornerRadius) {
        this.variant = variant == null ? GlassVariant.REGULAR : variant;
        this.tint = tint;
        this.interactive = interactive;
        this.shape = shape == null ? GlassShape.CAPSULE : shape;
        this.cornerRadius = cornerRadius;
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

    /**
     * Returns a copy with the given variant.
     *
     * @param newVariant the variant
     * @return a new immutable bundle
     */
    public GlassEffectParams variant(GlassVariant newVariant) {
        return new GlassEffectParams(newVariant, tint, interactive, shape, cornerRadius);
    }

    /**
     * Returns a copy with the given prominence tint (rendered at 0.25 alpha per the spec).
     *
     * @param tintColor the tint color, {@code null} to clear
     * @return a new immutable bundle
     */
    public GlassEffectParams tint(Color tintColor) {
        return new GlassEffectParams(variant, tintColor, interactive, shape, cornerRadius);
    }

    /**
     * Returns a copy flagged interactive: pressing flattens the bevel (~40% less refraction)
     * and deepens the shadow; hovering shifts the specular highlight toward the pointer.
     *
     * @return a new immutable bundle
     */
    public GlassEffectParams interactive() {
        return new GlassEffectParams(variant, tint, true, shape, cornerRadius);
    }

    /**
     * Returns a copy with the given shape and corner radius.
     *
     * @param newShape the silhouette
     * @param radius the corner radius in px (used by {@link GlassShape#RECT})
     * @return a new immutable bundle
     */
    public GlassEffectParams in(GlassShape newShape, double radius) {
        return new GlassEffectParams(variant, tint, interactive, newShape, radius);
    }
}
