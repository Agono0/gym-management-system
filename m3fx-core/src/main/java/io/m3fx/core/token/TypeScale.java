/**
 * M3 type scale (15 roles).
 *
 * <p>See <a href="https://m3.material.io/styles/typography/overview">M3 typography spec</a> and
 * {@code material-web/tokens/_md-sys-typescale.scss} (v0.192). Default typeface is Roboto.
 */
package io.m3fx.core.token;

/** The full M3 type scale. */
public final class TypeScale {

    private TypeScale() {}

    /** Default M3 typeface. */
    public static final String DEFAULT_FAMILY = "Roboto";

    /** Display large: 57sp / 64sp. */
    public static final TypeRole DISPLAY_LARGE = new TypeRole("display-large", 57, 64, -0.25, 400);
    /** Display medium: 45sp / 52sp. */
    public static final TypeRole DISPLAY_MEDIUM = new TypeRole("display-medium", 45, 52, 0, 400);
    /** Display small: 36sp / 44sp. */
    public static final TypeRole DISPLAY_SMALL = new TypeRole("display-small", 36, 44, 0, 400);
    /** Headline large: 32sp / 40sp. */
    public static final TypeRole HEADLINE_LARGE = new TypeRole("headline-large", 32, 40, 0, 400);
    /** Headline medium: 28sp / 36sp. */
    public static final TypeRole HEADLINE_MEDIUM = new TypeRole("headline-medium", 28, 36, 0, 400);
    /** Headline small: 24sp / 32sp. */
    public static final TypeRole HEADLINE_SMALL = new TypeRole("headline-small", 24, 32, 0, 400);
    /** Title large: 22sp / 28sp. */
    public static final TypeRole TITLE_LARGE = new TypeRole("title-large", 22, 28, 0, 400);
    /** Title medium: 16sp / 24sp, medium weight. */
    public static final TypeRole TITLE_MEDIUM = new TypeRole("title-medium", 16, 24, 0.15, 500);
    /** Title small: 14sp / 20sp, medium weight. */
    public static final TypeRole TITLE_SMALL = new TypeRole("title-small", 14, 20, 0.1, 500);
    /** Label large: 14sp / 20sp, medium weight. */
    public static final TypeRole LABEL_LARGE = new TypeRole("label-large", 14, 20, 0.1, 500);
    /** Label medium: 12sp / 16sp, medium weight. */
    public static final TypeRole LABEL_MEDIUM = new TypeRole("label-medium", 12, 16, 0.5, 500);
    /** Label small: 11sp / 16sp, medium weight. */
    public static final TypeRole LABEL_SMALL = new TypeRole("label-small", 11, 16, 0.5, 500);
    /** Body large: 16sp / 24sp. */
    public static final TypeRole BODY_LARGE = new TypeRole("body-large", 16, 24, 0.5, 400);
    /** Body medium: 14sp / 20sp. */
    public static final TypeRole BODY_MEDIUM = new TypeRole("body-medium", 14, 20, 0.25, 400);
    /** Body small: 12sp / 16sp. */
    public static final TypeRole BODY_SMALL = new TypeRole("body-small", 12, 16, 0.4, 400);

    /** All 15 roles in spec order. */
    public static final TypeRole[] ALL = {
        DISPLAY_LARGE, DISPLAY_MEDIUM, DISPLAY_SMALL,
        HEADLINE_LARGE, HEADLINE_MEDIUM, HEADLINE_SMALL,
        TITLE_LARGE, TITLE_MEDIUM, TITLE_SMALL,
        LABEL_LARGE, LABEL_MEDIUM, LABEL_SMALL,
        BODY_LARGE, BODY_MEDIUM, BODY_SMALL
    };

    /**
     * Looks up a role by name.
     *
     * @param name role name such as {@code label-large}
     * @return the matching role, or {@link #BODY_MEDIUM} if unknown
     */
    public static TypeRole forName(String name) {
        for (TypeRole role : ALL) {
            if (role.name().equals(name)) {
                return role;
            }
        }
        return BODY_MEDIUM;
    }
}
