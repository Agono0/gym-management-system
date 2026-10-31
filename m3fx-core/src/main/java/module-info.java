/** M3 core: dynamic color, tokens and theme (no UI controls). */
module io.m3fx.core {
    // 'transitive': the exported API (Color in ElevationLevel, Interpolator in MotionTokens,
    // ...) exposes javafx types, so readers of io.m3fx.core must be able to read javafx too.
    requires transitive javafx.base;
    requires transitive javafx.graphics;

    exports io.m3fx.core.token;
    exports io.m3fx.core.theme;
}
