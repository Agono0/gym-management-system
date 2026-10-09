package io.m3fx.controls.internal;

import io.m3fx.core.theme.ColorRole;
import io.m3fx.core.theme.M3Theme;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Reads a resolved {@link ColorRole} color for canvas-drawn skins (wavy progress, charts) that
 * cannot use CSS looked-up colors directly.
 *
 * <p>A tiny probe region styled with the looked-up variable is attached to the context node,
 * CSS-applied, and its background fill read back. Falls back to the baseline light theme when the
 * node is not yet in a live scene. Probe once per skin (or on theme change) and cache — never per
 * animation frame.
 */
public final class ThemeColors {

    private static volatile M3Theme fallback;

    private ThemeColors() {}

    /**
     * Resolves a color role against the live scene CSS, with baseline fallback.
     *
     * @param context a node inside the scene using the M3 theme
     * @param role the role to resolve
     * @return the resolved color
     */
    public static Color resolve(Node context, ColorRole role) {
        if (context != null && context.getScene() != null) {
            M3Theme theme = io.m3fx.controls.M3Stylesheets.getTheme(context.getScene());
            if (theme != null) {
                return theme.get(role);
            }
        }
        Color found = probe(context, role);
        if (found != null) {
            return found;
        }
        if (fallback == null) {
            fallback = M3Theme.baseline(false);
        }
        return fallback.get(role);
    }

    private static Color probe(Node context, ColorRole role) {
        try {
            if (context == null || context.getScene() == null) {
                return null;
            }
            Region probe = new Region();
            probe.setStyle("-fx-background-color: " + role.cssVariable() + ";");
            probe.setManaged(false);
            probe.setVisible(false);
            javafx.scene.Parent parent =
                    context.getParent() != null
                            ? context.getParent()
                            : (context instanceof javafx.scene.Parent p ? p : null);
            boolean attached = false;
            if (parent instanceof javafx.scene.layout.Pane pane) {
                pane.getChildren().add(probe);
                attached = true;
            }
            if (!attached) {
                return null;
            }
            probe.applyCss();
            Background background = probe.getBackground();
            ((javafx.scene.layout.Pane) parent).getChildren().remove(probe);
            if (background == null || background.getFills().isEmpty()) {
                return null;
            }
            BackgroundFill fill = background.getFills().get(0);
            return fill.getFill() instanceof Color color ? color : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
