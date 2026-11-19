/*
 * M3 shape corner tokens.
 *
 * <p>See <a href="https://m3.material.io/styles/shape/overview">M3 shape spec</a>,
 * {@code material-web/tokens/_md-sys-shape.scss} (v0.192) and
 * {@code material-web/tokens/versions/latest/sass/_md-sys-shape.scss} (v34.0.21, which adds the
 * increased and extra-extra-large Expressive sizes).
 */
package io.m3fx.core.token;

/**
 * Corner radii in dp for the M3 shape scale.
 *
 * <p>The full scale (spec names, values quoted from the generated v34.0.21 token set — see
 * {@code docs/SPEC_NOTES/shape-scale.md}):
 *
 * <table>
 *   <caption>M3 shape scale</caption>
 *   <tr><th>Token</th><th>dp</th><th>{@code md.sys.shape.corner.*}</th></tr>
 *   <tr><td>{@link #NONE}</td><td>0</td><td>{@code none}</td></tr>
 *   <tr><td>{@link #EXTRA_SMALL}</td><td>4</td><td>{@code extra-small}</td></tr>
 *   <tr><td>{@link #SMALL}</td><td>8</td><td>{@code small}</td></tr>
 *   <tr><td>{@link #MEDIUM}</td><td>12</td><td>{@code medium}</td></tr>
 *   <tr><td>{@link #LARGE}</td><td>16</td><td>{@code large}</td></tr>
 *   <tr><td>{@link #LARGE_INCREASED}</td><td>20</td><td>{@code large-increased}</td></tr>
 *   <tr><td>{@link #EXTRA_LARGE}</td><td>28</td><td>{@code extra-large}</td></tr>
 *   <tr><td>{@link #EXTRA_LARGE_INCREASED}</td><td>32</td><td>{@code extra-large-increased}</td></tr>
 *   <tr><td>{@link #EXTRA_EXTRA_LARGE}</td><td>48</td><td>{@code extra-extra-large}</td></tr>
 *   <tr><td>{@link #FULL}</td><td>9999</td><td>{@code full}</td></tr>
 * </table>
 */
public final class ShapeTokens {

    private ShapeTokens() {}

    /** Sharp corners (0dp), spec {@code md.sys.shape.corner.none}. */
    public static final double NONE = 0;
    /** Extra-small corners (4dp), spec {@code md.sys.shape.corner.extra-small}. */
    public static final double EXTRA_SMALL = 4;
    /** Small corners (8dp), spec {@code md.sys.shape.corner.small}. */
    public static final double SMALL = 8;
    /** Medium corners (12dp), spec {@code md.sys.shape.corner.medium}. */
    public static final double MEDIUM = 12;
    /** Large corners (16dp), spec {@code md.sys.shape.corner.large}. */
    public static final double LARGE = 16;
    /** Large-increased corners (20dp), spec {@code md.sys.shape.corner.large-increased}. */
    public static final double LARGE_INCREASED = 20;
    /** Extra-large corners (28dp), spec {@code md.sys.shape.corner.extra-large}. */
    public static final double EXTRA_LARGE = 28;
    /** Extra-large-increased corners (32dp), spec {@code md.sys.shape.corner.extra-large-increased}. */
    public static final double EXTRA_LARGE_INCREASED = 32;
    /** Extra-extra-large corners (48dp), spec {@code md.sys.shape.corner.extra-extra-large}. */
    public static final double EXTRA_EXTRA_LARGE = 48;
    /**
     * Fully rounded ({@code md.sys.shape.corner.full}, 9999px). JavaFX clamps oversized arc radii
     * proportionally, so the value behaves as a pill / circle sentinel in background fills.
     */
    public static final double FULL = 9999;

    /**
     * Legacy name for the extra-large-increased corners (32dp).
     *
     * @deprecated Use {@link #EXTRA_LARGE_INCREASED} — the spec name is
     *     {@code extra-large-increased}, not "expressive large-increased".
     */
    @Deprecated
    public static final double EXPRESSIVE_LARGE_INCREASED = EXTRA_LARGE_INCREASED;

    /**
     * Legacy name for the extra-extra-large corners (48dp).
     *
     * @deprecated Use {@link #EXTRA_EXTRA_LARGE} — the spec name is
     *     {@code extra-extra-large}, not "expressive extra-extra-large".
     */
    @Deprecated
    public static final double EXPRESSIVE_EXTRA_EXTRA_LARGE = EXTRA_EXTRA_LARGE;

    /**
     * Returns the corner radius for a shape token name.
     *
     * @param name one of none, extra-small, small, medium, large, large-increased, extra-large,
     *     extra-large-increased, extra-extra-large, full
     * @return radius in dp, or {@link #NONE} for unknown or null names
     */
    public static double forName(String name) {
        return switch (name) {
            case "none" -> NONE;
            case "extra-small" -> EXTRA_SMALL;
            case "small" -> SMALL;
            case "medium" -> MEDIUM;
            case "large" -> LARGE;
            case "large-increased" -> LARGE_INCREASED;
            case "extra-large" -> EXTRA_LARGE;
            case "extra-large-increased" -> EXTRA_LARGE_INCREASED;
            case "extra-extra-large" -> EXTRA_EXTRA_LARGE;
            case "full" -> FULL;
            case null, default -> NONE;
        };
    }
}
