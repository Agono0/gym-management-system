package io.m3fx.core.theme;

/**
 * All M3 color roles exposed by {@link M3Theme}.
 *
 * <p>See <a href="https://m3.material.io/styles/color/roles">M3 color roles</a>. Each enum entry
 * carries the CSS looked-up color name ({@code -md-sys-color-*}) used by control stylesheets.
 */
public enum ColorRole {
    /** Primary brand color. */
    PRIMARY("primary"),
    /** Expressive dim primary color. */
    PRIMARY_DIM("primary-dim"),
    /** Content on top of primary. */
    ON_PRIMARY("on-primary"),
    /** Emphasized primary container. */
    PRIMARY_CONTAINER("primary-container"),
    /** Content on top of primary container. */
    ON_PRIMARY_CONTAINER("on-primary-container"),
    /** Fixed primary tone used across light/dark. */
    PRIMARY_FIXED("primary-fixed"),
    /** Dimmer fixed primary tone. */
    PRIMARY_FIXED_DIM("primary-fixed-dim"),
    /** Content on top of primary fixed. */
    ON_PRIMARY_FIXED("on-primary-fixed"),
    /** Variant content on top of primary fixed. */
    ON_PRIMARY_FIXED_VARIANT("on-primary-fixed-variant"),
    /** Inverse of primary for surfaces. */
    INVERSE_PRIMARY("inverse-primary"),
    /** Secondary brand color. */
    SECONDARY("secondary"),
    /** Expressive dim secondary color. */
    SECONDARY_DIM("secondary-dim"),
    /** Content on top of secondary. */
    ON_SECONDARY("on-secondary"),
    /** Emphasized secondary container. */
    SECONDARY_CONTAINER("secondary-container"),
    /** Content on top of secondary container. */
    ON_SECONDARY_CONTAINER("on-secondary-container"),
    /** Fixed secondary tone. */
    SECONDARY_FIXED("secondary-fixed"),
    /** Dimmer fixed secondary tone. */
    SECONDARY_FIXED_DIM("secondary-fixed-dim"),
    /** Content on top of secondary fixed. */
    ON_SECONDARY_FIXED("on-secondary-fixed"),
    /** Variant content on top of secondary fixed. */
    ON_SECONDARY_FIXED_VARIANT("on-secondary-fixed-variant"),
    /** Tertiary brand color. */
    TERTIARY("tertiary"),
    /** Expressive dim tertiary color. */
    TERTIARY_DIM("tertiary-dim"),
    /** Content on top of tertiary. */
    ON_TERTIARY("on-tertiary"),
    /** Emphasized tertiary container. */
    TERTIARY_CONTAINER("tertiary-container"),
    /** Content on top of tertiary container. */
    ON_TERTIARY_CONTAINER("on-tertiary-container"),
    /** Fixed tertiary tone. */
    TERTIARY_FIXED("tertiary-fixed"),
    /** Dimmer fixed tertiary tone. */
    TERTIARY_FIXED_DIM("tertiary-fixed-dim"),
    /** Content on top of tertiary fixed. */
    ON_TERTIARY_FIXED("on-tertiary-fixed"),
    /** Variant content on top of tertiary fixed. */
    ON_TERTIARY_FIXED_VARIANT("on-tertiary-fixed-variant"),
    /** Error color. */
    ERROR("error"),
    /** Expressive dim error color. */
    ERROR_DIM("error-dim"),
    /** Content on top of error. */
    ON_ERROR("on-error"),
    /** Emphasized error container. */
    ERROR_CONTAINER("error-container"),
    /** Content on top of error container. */
    ON_ERROR_CONTAINER("on-error-container"),
    /** Default background. */
    BACKGROUND("background"),
    /** Content on top of background. */
    ON_BACKGROUND("on-background"),
    /** Default surface. */
    SURFACE("surface"),
    /** Content on top of surface. */
    ON_SURFACE("on-surface"),
    /** Lower-emphasis surface content. */
    SURFACE_VARIANT("surface-variant"),
    /** Content on top of surface variant. */
    ON_SURFACE_VARIANT("on-surface-variant"),
    /** Darkest surface tone. */
    SURFACE_DIM("surface-dim"),
    /** Brightest surface tone. */
    SURFACE_BRIGHT("surface-bright"),
    /** Lowest surface container. */
    SURFACE_CONTAINER_LOWEST("surface-container-lowest"),
    /** Low surface container. */
    SURFACE_CONTAINER_LOW("surface-container-low"),
    /** Default surface container. */
    SURFACE_CONTAINER("surface-container"),
    /** High surface container. */
    SURFACE_CONTAINER_HIGH("surface-container-high"),
    /** Highest surface container. */
    SURFACE_CONTAINER_HIGHEST("surface-container-highest"),
    /** Surface tint color. */
    SURFACE_TINT("surface-tint"),
    /** Inverse surface for contrast elements. */
    INVERSE_SURFACE("inverse-surface"),
    /** Content on top of inverse surface. */
    INVERSE_ON_SURFACE("inverse-on-surface"),
    /** Outline color. */
    OUTLINE("outline"),
    /** Lower-emphasis outline. */
    OUTLINE_VARIANT("outline-variant"),
    /** Scrim behind modals. */
    SCRIM("scrim"),
    /** Shadow color. */
    SHADOW("shadow");

    private final String cssName;

    ColorRole(String cssName) {
        this.cssName = cssName;
    }

    /**
     * Returns the CSS looked-up color name (without the {@code -md-sys-color-} prefix).
     *
     * @return css token name
     */
    public String cssName() {
        return cssName;
    }

    /**
     * Returns the full JavaFX looked-up color reference, e.g. {@code -md-sys-color-primary}.
     *
     * @return full css variable name
     */
    public String cssVariable() {
        return "-md-sys-color-" + cssName;
    }
}
