package io.m3fx.controls.layout;

/**
 * M3 window size classes driving adaptive navigation.
 *
 * <p>See <a href="https://m3.material.io/foundations/layout/applying-layout/window-size-classes">M3
 * window size classes</a>. Width breakpoints: compact &lt; 600dp, medium 600-840dp, expanded &gt;
 * 840dp. Height breakpoints: compact &lt; 480dp, medium 480-900dp, expanded &gt; 900dp.
 */
public enum WindowSizeClass {
    /** Small window (phone portrait). */
    COMPACT,
    /** Medium window (tablet / folded). */
    MEDIUM,
    /** Expanded window (desktop / large tablet). */
    EXPANDED;

    /**
     * Classifies a window width.
     *
     * @param widthDp width in dp (px at 1x)
     * @return the size class
     */
    public static WindowSizeClass forWidth(double widthDp) {
        if (widthDp < 600) {
            return COMPACT;
        }
        if (widthDp < 840) {
            return MEDIUM;
        }
        return EXPANDED;
    }

    /**
     * Classifies a window height.
     *
     * @param heightDp height in dp (px at 1x)
     * @return the size class
     */
    public static WindowSizeClass forHeight(double heightDp) {
        if (heightDp < 480) {
            return COMPACT;
        }
        if (heightDp < 900) {
            return MEDIUM;
        }
        return EXPANDED;
    }
}
