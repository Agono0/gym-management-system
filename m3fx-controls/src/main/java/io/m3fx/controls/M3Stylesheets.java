package io.m3fx.controls;

import io.m3fx.core.theme.M3Theme;
import io.m3fx.controls.internal.M3Fonts;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
 *
 * <p>The theme CSS lives in a temp file named from a hash of its content; the file is refcounted
 * per scene using it and is deleted when the last scene releases it (scene collected or
 * re-themed) or by a JVM-shutdown sweep for stragglers.
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

    /** Guards theme temp-file creation and the refcounts below so concurrent scenes agree. */
    private static final Object THEME_FILE_LOCK = new Object();

    /** Live refcount per theme temp file (path → number of scenes still using it). */
    private static final Map<Path, Integer> THEME_FILE_REFS = new HashMap<>();

    /** Weak registrations of the scenes currently using a theme temp file. */
    private static final List<SceneThemeRef> SCENE_THEME_REFS = new ArrayList<>();

    /** Collected scenes are enqueued here; swept on the next theme application. */
    private static final ReferenceQueue<Scene> RELEASED_SCENES = new ReferenceQueue<>();

    static {
        Runtime.getRuntime().addShutdownHook(
                new Thread(M3Stylesheets::deleteStragglerThemeFiles, "m3fx-theme-css-sweep"));
    }

    /** Weak registration of a scene using a theme temp file, so the file can be released. */
    private static final class SceneThemeRef extends WeakReference<Scene> {

        private Path themeFile;

        SceneThemeRef(Scene scene, Path themeFile) {
            super(scene, RELEASED_SCENES);
            this.themeFile = themeFile;
        }
    }

    private M3Stylesheets() {}

    /**
     * Sets the latest active theme for the application. {@code null} is ignored so
     * {@link #getLatestTheme()} keeps providing a baseline for {@code ThemeColors.resolve}.
     *
     * @param theme the theme to set
     */
    public static void setLatestTheme(M3Theme theme) {
        if (theme != null) {
            latestTheme = theme;
        }
    }

    /**
     * Applies a theme to a scene: writes the theme CSS to a temp file, adds it before the
     * component stylesheet, and loads fonts.
     *
     * <p>Before applying, only the URLs this class itself previously added to the scene are
     * removed; a user stylesheet whose URL merely contains {@code m3fx} is left in place. The
     * theme temp file is named from a hash of the theme CSS content, so every scene and call
     * applying the same theme reuses one file; it is recreated only when absent and is deleted
     * when the last scene using it is released (collected or re-themed) or by the JVM-shutdown
     * sweep.
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
                Path themeFile = themeStylesheetFile(theme);
                trackSceneThemeFile(scene, themeFile);
                addTracked(scene, themeFile.toUri().toString());
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

    /** Registers the scene as using the theme file, releasing the file it replaces. */
    private static void trackSceneThemeFile(Scene scene, Path themeFile) {
        synchronized (THEME_FILE_LOCK) {
            sweepReleasedScenes();
            SceneThemeRef existing = findSceneRef(scene);
            if (existing == null) {
                SCENE_THEME_REFS.add(new SceneThemeRef(scene, themeFile));
                acquireThemeFile(themeFile);
            } else if (!existing.themeFile.equals(themeFile)) {
                releaseThemeFile(existing.themeFile);
                acquireThemeFile(themeFile);
                existing.themeFile = themeFile;
            }
        }
    }

    /** Finds the weak registration for a scene, if any. */
    private static SceneThemeRef findSceneRef(Scene scene) {
        for (SceneThemeRef ref : SCENE_THEME_REFS) {
            if (ref.get() == scene) {
                return ref;
            }
        }
        return null;
    }

    /**
     * Releases the theme file the scene holds. Package-private: also invoked by tests to
     * simulate a scene release deterministically.
     *
     * @param scene the scene being released
     */
    static void releaseScene(Scene scene) {
        synchronized (THEME_FILE_LOCK) {
            sweepReleasedScenes();
            SceneThemeRef existing = findSceneRef(scene);
            if (existing != null) {
                SCENE_THEME_REFS.remove(existing);
                releaseThemeFile(existing.themeFile);
            }
        }
    }

    /** Drops collected scenes and releases their theme files. */
    private static void sweepReleasedScenes() {
        Reference<? extends Scene> released;
        while ((released = RELEASED_SCENES.poll()) != null) {
            SceneThemeRef ref = (SceneThemeRef) released;
            SCENE_THEME_REFS.remove(ref);
            releaseThemeFile(ref.themeFile);
        }
    }

    /** Increments the refcount for a theme temp file. */
    private static void acquireThemeFile(Path themeFile) {
        THEME_FILE_REFS.merge(themeFile, 1, Integer::sum);
    }

    /** Decrements the refcount for a theme temp file and deletes it when the last user is gone. */
    private static void releaseThemeFile(Path themeFile) {
        Integer count = THEME_FILE_REFS.get(themeFile);
        if (count == null || count <= 1) {
            THEME_FILE_REFS.remove(themeFile);
            deleteQuietly(themeFile);
        } else {
            THEME_FILE_REFS.put(themeFile, count - 1);
        }
    }

    /** Deletes tracked theme temp files left behind by scenes that never got swept. */
    static void deleteStragglerThemeFiles() {
        synchronized (THEME_FILE_LOCK) {
            for (Path file : List.copyOf(THEME_FILE_REFS.keySet())) {
                deleteQuietly(file);
            }
            THEME_FILE_REFS.clear();
            SCENE_THEME_REFS.clear();
        }
    }

    /** Best-effort delete; the temp dir is shared, so failures are not fatal. */
    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // Best effort: the shutdown sweep or a later re-apply retries.
        }
    }
}
