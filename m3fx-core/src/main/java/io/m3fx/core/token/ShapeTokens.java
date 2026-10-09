/**
 * M3 shape corner tokens.
 *
 * <p>See <a href="https://m3.material.io/styles/shape/overview">M3 shape spec</a> and
 * {@code material-web/tokens/_md-sys-shape.scss} (v0.192).
 */
package io.m3fx.core.token;

/** Corner radii in dp for the M3 shape scale, plus Expressive large sizes. */
public final class ShapeTokens {

    private ShapeTokens() {}

    /** Sharp corners (0dp). */
    public static final double NONE = 0;
    /** Extra-small corners (4dp). */
    public static final double EXTRA_SMALL = 4;
    /** Small corners (8dp). */
    public static final double SMALL = 8;
    /** Medium corners (12dp). */
    public static final double MEDIUM = 12;
    /** Large corners (16dp). */
    public static final double LARGE = 16;
    /** Extra-large corners (28dp). */
    public static final double EXTRA_LARGE = 28;
    /** Fully rounded (pill / circle). Used as a large sentinel radius in JavaFX backgrounds. */
    public static final double FULL = 999;

    /** Expressive extra-large increased (32dp). */
    public static final double EXPRESSIVE_LARGE_INCREASED = 32;
    /** Expressive extra-extra-large (48dp). */
    public static final double EXPRESSIVE_EXTRA_EXTRA_LARGE = 48;

    /**
     * Returns the corner radius for a shape token name.
     *
     * @param name one of none, extra-small, small, medium, large, extra-large, full
     * @return radius in dp
     */
    public static double forName(String name) {
        return switch (name) {
            case "extra-small" -> EXTRA_SMALL;
            case "small" -> SMALL;
            case "medium" -> MEDIUM;
            case "large" -> LARGE;
            case "extra-large" -> EXTRA_LARGE;
            case "full" -> FULL;
            default -> NONE;
        };
    }
}
