package io.m3fx.controls.glass;

/**
 * Silhouette of a liquid glass element. The shape drives the refraction bezel
 * ({@link GlassRefraction#buildFloatMap}) and the clip of the glass layers.
 *
 * <p>See {@code docs/SPEC_NOTES/liquid-glass.md}.
 */
public enum GlassShape {
    /** Pill shape: fully rounded ends, effective radius = min(width, height) / 2. */
    CAPSULE,
    /** Rounded rectangle using {@code cornerRadius} (clamped to half the smaller side). */
    RECT,
    /** Circle: effective radius = min(width, height) / 2 (use a square pane). */
    CIRCLE
}
