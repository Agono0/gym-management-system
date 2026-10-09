package io.m3fx.core.token;

import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

/**
 * M3 elevation levels 0-5 mapped to JavaFX {@link DropShadow} settings.
 *
 * <p>See <a href="https://m3.material.io/styles/elevation/overview">M3 elevation spec</a> and
 * {@code material-web/tokens/_md-sys-elevation.scss} (v0.192). M3 elevation is a combination of
 * surface-tint color overlay (handled in CSS) plus a shadow; this helper owns the shadow half.
 */
public enum ElevationLevel {
    /** No shadow. */
    LEVEL0(0, 0, 0, 0),
    /** 0dp offset shadow for hover/high elements. */
    LEVEL1(1, 1, 3, 0.15),
    /** Cards, menus. */
    LEVEL2(2, 2, 6, 0.15),
    /** FAB, snackbar. */
    LEVEL3(3, 4, 8, 0.16),
    /** Dialog, drawer. */
    LEVEL4(4, 6, 12, 0.18),
    /** Modal sheets, pickers. */
    LEVEL5(5, 8, 16, 0.20);

    private final int level;
    private final double offsetY;
    private final double radius;
    private final double opacity;

    ElevationLevel(int level, double offsetY, double radius, double opacity) {
        this.level = level;
        this.offsetY = offsetY;
        this.radius = radius;
        this.opacity = opacity;
    }

    /** Returns the numeric level 0-5. */
    public int level() {
        return level;
    }

    /**
     * Creates a {@link DropShadow} for this level tinted with the M3 shadow color.
     *
     * @param shadowColor the M3 shadow color (usually {@code ColorRole.SHADOW})
     * @return a new drop shadow effect
     */
    public DropShadow toEffect(Color shadowColor) {
        Color c = shadowColor == null ? Color.BLACK : shadowColor;
        DropShadow shadow = new DropShadow();
        shadow.setOffsetX(0);
        shadow.setOffsetY(offsetY);
        shadow.setRadius(radius);
        shadow.setSpread(0);
        shadow.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), opacity));
        return shadow;
    }

    /**
     * Returns the level for a number, clamped to 0-5.
     *
     * @param level requested level
     * @return the matching level
     */
    public static ElevationLevel of(int level) {
        return values()[Math.min(5, Math.max(0, level))];
    }
}
