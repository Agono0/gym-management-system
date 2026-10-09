package io.m3fx.core.theme;

/**
 * M3 dynamic-color variants.
 *
 * <p>See {@code material-color-utilities/java/dynamiccolor/Variant.java}. Each variant maps to one
 * {@code Scheme*} class in {@code io.m3fx.core.color.scheme}.
 */
public enum ThemeVariant {
    /** Calm, low-chroma default. */
    TONAL_SPOT,
    /** High-chroma, colorful. */
    VIBRANT,
    /** Playful, hue-shifted secondary palettes. */
    EXPRESSIVE,
    /** Near-grayscale with a hint of the seed. */
    NEUTRAL,
    /** Pure grayscale seed. */
    MONOCHROME,
    /** Faithful seed reproduction. */
    FIDELITY,
    /** Derived from image content. */
    CONTENT,
    /** Rainbow gradients. */
    RAINBOW,
    /** Fruit-salad gradients. */
    FRUIT_SALAD
}
