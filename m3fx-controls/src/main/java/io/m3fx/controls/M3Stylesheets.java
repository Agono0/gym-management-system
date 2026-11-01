package io.m3fx.controls;

import io.m3fx.core.theme.M3Theme;
import io.m3fx.controls.internal.M3Fonts;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javafx.scene.Scene;

/**
 * One-call helper that applies the M3 theme plus all component stylesheets to a scene.
 *
 * <p>Theme switching = calling this once with a new {@link M3Theme}; every control references the
 * {@code -md-sys-color-*} looked-up colors, so all controls update together.
 *
 * <p>This class only ever removes the stylesheet URLs it added itself (tracked per scene), so
 * application stylesheets are never touched &mdash; even when their URL happens to contain
 * {@code m3fx}.
 */
public final class M3Stylesheets {

    /** Classpath location of the bundled component stylesheet. */
    public static final String M3FX_CSS = "/io/m3fx/css/m3fx.css";

    /** Classpath location of the bundled liquid glass stylesheet. */
    public static final String GLASS_CSS = "/io/m3fx/css/glass.css";

    private static volatile M3Theme latestTheme = M3Theme.baseline(true);

    /**
     * Stylesheet URLs this class added per scene. Weak scene keys, so discarded scenes do not leak
     * through this map.
     */
    private static final Map<Scene, List<String>> APPLIED_URLS =
            Collections.synchronizedMap(new WeakHashMap<>());

    /** Guards theme temp-file creation so concurrent scenes reuse one file. */
    private static final Object THEME_FILE_LOCK = new Object();

    private M3Stylesheets() {}

    /**
     * Sets the latest active theme for the application.
     *
     * @param theme the theme to set
     */
    public static void setLatestTheme(M3Theme theme) {
        latestTheme = theme;
    }

    /**
     * Applies a theme to a scene: writes the theme CSS to a temp file, adds it before the
     * component stylesheet, and loads fonts.
     *
     * <p>Before applying, only the URLs this class itself previously added to the scene are
     * removed; a user stylesheet whose URL merely contains {@code m3fx} is left in place. The
     * theme temp file is named from a hash of the theme CSS content, so every scene and call
     * applying the same theme reuses one file; it is recreated only when absent and is not
     * deleted on exit so it can be reused across runs.
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
            removeAppliedUrls(scene);
            try {
                addTracked(scene, themeStylesheetFile(theme).toUri().toString());
            } catch (IOException e) {
                throw new IllegalStateException("Could not write M3 theme stylesheet", e);
            }
            var css = M3Stylesheets.class.getResource(M3FX_CSS);
            if (css != null) {
                addTracked(scene, css.toExternalForm());
            }
            var glassCss = M3Stylesheets.class.getResource(GLASS_CSS);
            if (glassCss != null) {
                addTracked(scene, glassCss.toExternalForm());
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
     * Re-applies the component stylesheet only (no theme change). The URL is registered as
     * managed by this class so a later {@link #applyTo(Scene, M3Theme)} removes and re-adds it
     * exactly.
     *
     * @param scene the scene
     */
    public static void applyComponentsOnly(Scene scene) {
        var css = M3Stylesheets.class.getResource(M3FX_CSS);
        if (css != null) {
            addTrackedIfAbsent(scene, css.toExternalForm());
        }
        var glassCss = M3Stylesheets.class.getResource(GLASS_CSS);
        if (glassCss != null) {
            addTrackedIfAbsent(scene, glassCss.toExternalForm());
        }
    }

    /** Removes exactly the stylesheet URLs this class previously added to the scene. */
    private static void removeAppliedUrls(Scene scene) {
        List<String> applied = APPLIED_URLS.remove(scene);
        if (applied != null) {
            scene.getStylesheets().removeAll(applied);
        }
    }

    /** Adds the URL to the scene and records it as managed by this class. */
    private static void addTracked(Scene scene, String url) {
        // Remove an identical URL first (e.g. added by hand) so the list never carries a
        // duplicate; appending keeps the theme-before-components order this class established.
        scene.getStylesheets().remove(url);
        scene.getStylesheets().add(url);
        APPLIED_URLS.computeIfAbsent(scene, s -> Collections.synchronizedList(new ArrayList<>()))
                .add(url);
    }

    /** Adds the URL and records it as managed, unless the scene already carries it. */
    private static void addTrackedIfAbsent(Scene scene, String url) {
        List<String> applied =
                APPLIED_URLS.computeIfAbsent(
                        scene, s -> Collections.synchronizedList(new ArrayList<>()));
        if (!applied.contains(url) && !scene.getStylesheets().contains(url)) {
            scene.getStylesheets().add(url);
        }
        if (!applied.contains(url)) {
            applied.add(url);
        }
    }

    /**
     * Returns the temp stylesheet file holding the theme CSS, creating it only when absent. The
     * file name is derived from a SHA-256 hash of the CSS content, so every scene applying the
     * same theme reuses a single file instead of one temp file per call.
     *
     * @param theme the theme whose CSS the file must contain
     * @return path to the reusable theme stylesheet file
     * @throws IOException when the file must be created and writing fails
     */
    private static Path themeStylesheetFile(M3Theme theme) throws IOException {
        String css = theme.toCss();
        Path file = Path.of(
                System.getProperty("java.io.tmpdir"),
                "m3fx-theme-" + contentHash(css) + ".css");
        if (Files.notExists(file)) {
            synchronized (THEME_FILE_LOCK) {
                if (Files.notExists(file)) {
                    Files.writeString(file, css, StandardCharsets.UTF_8);
                }
            }
        }
        return file;
    }

    /** Returns the first 16 hex characters of the SHA-256 digest of the content. */
    private static String contentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
