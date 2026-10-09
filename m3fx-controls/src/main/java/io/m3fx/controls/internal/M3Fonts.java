package io.m3fx.controls.internal;

import javafx.scene.text.Font;

/**
 * Shared M3 font loader. Roboto is the M3 default typeface (Apache 2.0 / OFL); call
 * {@link #load()} once at startup. If Roboto files are bundled under
 * {@code /io/m3fx/fonts/}, they are registered; otherwise the platform sans-serif is used and the
 * theme's {@code -md-sys-typescale-*-font} lookups still resolve.
 */
public final class M3Fonts {

    private static volatile boolean loaded;

    private M3Fonts() {}

    /** Default font family used when Roboto is unavailable. */
    public static final String FALLBACK_FAMILY = "System";

    /**
     * Loads bundled fonts exactly once (thread-safe). Safe to call repeatedly.
     */
    public static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        String[] candidates = {
            "/io/m3fx/fonts/Roboto-Regular.ttf",
            "/io/m3fx/fonts/Roboto-Medium.ttf",
        };
        for (String path : candidates) {
            try (var stream = M3Fonts.class.getResourceAsStream(path)) {
                if (stream != null) {
                    Font.loadFont(stream, 12);
                }
            } catch (Exception ignored) {
                // Bundled font optional; fall back to system fonts.
            }
        }
    }

    /**
     * Returns {@code true} once {@link #load()} has run.
     *
     * @return whether fonts were loaded
     */
    public static boolean isLoaded() {
        return loaded;
    }
}
