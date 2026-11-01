package io.m3fx.controls.glass;

/**
 * Liquid Glass variants (Apple iOS 26 "Meet Liquid Glass"): {@code regular} is the frosted
 * default for most surfaces, {@code clear} is more transparent for content-rich backdrops
 * where the glass should not compete with the media behind it.
 *
 * <p>See {@code docs/SPEC_NOTES/liquid-glass.md} for the JavaFX mapping.
 */
public enum GlassVariant {
    /** Frosted glass (default): stronger blur and saturation lift for legibility. */
    REGULAR,
    /** Clear glass: lighter blur and saturation so a rich backdrop shows through. */
    CLEAR
}
