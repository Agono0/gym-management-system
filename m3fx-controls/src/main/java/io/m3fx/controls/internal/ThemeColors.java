package io.m3fx.controls.internal;

import io.m3fx.core.theme.ColorRole;
import io.m3fx.core.theme.M3Theme;
import io.m3fx.controls.M3Stylesheets;
import javafx.scene.Node;
import javafx.scene.paint.Color;

/**
 * Reads a resolved {@link ColorRole} color for canvas-drawn skins (wavy progress, charts) that
 * cannot use CSS looked-up colors directly.
 *
 * <p>A tiny probe region styled with the looked-up variable is attached to the context node,
 * CSS-applied, and its background fill read back. Falls back to the most recently applied theme
 * (M3Stylesheets always provides a baseline) when the node is not yet in a live scene. Probe once
 * per skin (or on theme change) and cache — never per animation frame.
 */
public final class ThemeColors {

    private ThemeColors() {}

    /**
     * Resolves a color role against the live scene CSS, with latest-theme fallback.
     *
     * @param context a node inside the scene using the M3 theme
     * @param role the role to resolve
     * @return the resolved color
     */
    public static Color resolve(Node context, ColorRole role) {
        if (context != null && context.getScene() != null) {
            M3Theme theme = M3Stylesheets.getTheme(context.getScene());
            if (theme != null) {
                return theme.get(role);
            }
        }
        // No static fallback cache: M3Stylesheets initializes (and never nulls) the latest
        // theme, so it always provides a baseline even before any explicit theme was applied.
        return M3Stylesheets.getLatestTheme().get(role);
    }
}
