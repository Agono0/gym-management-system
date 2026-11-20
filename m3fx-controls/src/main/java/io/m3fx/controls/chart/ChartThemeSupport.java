package io.m3fx.controls.chart;

import javafx.collections.MapChangeListener;
import javafx.scene.Scene;
import javafx.scene.layout.Region;

/**
 * Wires a canvas-backed chart to repaint when the theme changes. The active theme lives in the
 * scene properties under {@code m3fx.theme} (written by {@code M3Stylesheets.applyTo}); canvas
 * charts resolve their colors through {@code ThemeColors.resolve} at draw time, so without this
 * hook they render stale colors until the next data or size change (B6).
 */
final class ChartThemeSupport {

    private ChartThemeSupport() {}

    /**
     * Attaches the repaint hook to a chart: repaints on scene attach/detach (the pre-B6
     * behavior) and whenever the scene's {@code m3fx.theme} property is (re-)applied.
     *
     * @param chart the chart region
     * @param repaint the chart's top-level render method
     */
    static void attach(Region chart, Runnable repaint) {
        Listener listener = new Listener(repaint);
        chart.sceneProperty().addListener((obs, oldScene, newScene) ->
                listener.sceneChanged(oldScene, newScene));
        listener.sceneChanged(null, chart.getScene());
    }

    private static final class Listener {
        private final Runnable repaint;
        private final MapChangeListener<Object, Object> themeListener;

        Listener(Runnable repaint) {
            this.repaint = repaint;
            this.themeListener = change -> {
                if ("m3fx.theme".equals(change.getKey())) {
                    repaint.run();
                }
            };
        }

        void sceneChanged(Scene oldScene, Scene newScene) {
            if (oldScene == newScene) {
                return;
            }
            if (oldScene != null) {
                oldScene.getProperties().removeListener(themeListener);
            }
            if (newScene != null) {
                newScene.getProperties().addListener(themeListener);
            }
            repaint.run();
        }
    }
}
