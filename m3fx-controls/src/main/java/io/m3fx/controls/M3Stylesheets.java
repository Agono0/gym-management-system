package io.m3fx.controls;

import io.m3fx.core.theme.M3Theme;
import io.m3fx.controls.internal.M3Fonts;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.scene.Scene;

/**
 * One-call helper that applies the M3 theme plus all component stylesheets to a scene.
 *
 * <p>Theme switching = calling this once with a new {@link M3Theme}; every control references the
 * {@code -md-sys-color-*} looked-up colors, so all controls update together.
 */
public final class M3Stylesheets {

    private M3Stylesheets() {}

    /** Classpath location of the bundled component stylesheet. */
    public static final String M3FX_CSS = "/io/m3fx/css/m3fx.css";

    private static volatile M3Theme latestTheme;

    /**
     * Applies a theme to a scene: writes the theme CSS to a temp file, adds it before the
     * component stylesheet, and loads fonts.
     *
     * @param scene the scene to style
     * @param theme the theme to apply
     */
    public static void applyTo(Scene scene, M3Theme theme) {
        M3Fonts.load();
        latestTheme = theme;
        if (scene != null) {
            scene.getProperties().put("m3fx.theme", theme);
            scene.setFill(theme.get(io.m3fx.core.theme.ColorRole.SURFACE));
            scene.getStylesheets().removeIf(s -> s.contains("m3fx"));
            try {
                Path themeFile = Files.createTempFile("m3fx-theme-", ".css");
                themeFile.toFile().deleteOnExit();
                Files.writeString(themeFile, theme.toCss(), StandardCharsets.UTF_8);
                scene.getStylesheets().add(themeFile.toUri().toString());
            } catch (IOException e) {
                throw new IllegalStateException("Could not write M3 theme stylesheet", e);
            }
            var css = M3Stylesheets.class.getResource(M3FX_CSS);
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }
        }
    }

    /**
     * Returns the active theme associated with the scene, or {@code null} if none is set.
     *
     * @param scene the scene
     * @return the active theme or null
     */
    public static M3Theme getTheme(Scene scene) {
        if (scene != null && scene.getProperties().get("m3fx.theme") instanceof M3Theme t) {
            return t;
        }
        return null;
    }

    /**
     * Returns the most recently applied theme, or {@code null} if none has been applied yet.
     *
     * @return the latest applied theme
     */
    public static M3Theme getLatestTheme() {
        return latestTheme;
    }

    /**
     * Re-applies the component stylesheet only (no theme change).
     *
     * @param scene the scene
     */
    public static void applyComponentsOnly(Scene scene) {
        var css = M3Stylesheets.class.getResource(M3FX_CSS);
        if (css != null && !scene.getStylesheets().contains(css.toExternalForm())) {
            scene.getStylesheets().add(css.toExternalForm());
        }
    }
}
