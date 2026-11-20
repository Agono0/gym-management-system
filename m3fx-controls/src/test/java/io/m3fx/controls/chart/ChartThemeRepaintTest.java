package io.m3fx.controls.chart;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.m3fx.controls.M3Stylesheets;
import io.m3fx.core.theme.M3Theme;
import io.m3fx.core.theme.ThemeVariant;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * B6: canvas charts resolve their colors at draw time, so a theme change must trigger a repaint.
 * Every chart in the package wires {@link ChartThemeSupport}; this test flips the scene's
 * {@code m3fx.theme} for each chart and asserts the render counter advanced.
 */
class ChartThemeRepaintTest {

    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized
        }
        Platform.setImplicitExit(false);
    }

    private static void runOnFx(Runnable action) throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] error = new Throwable[1];
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(30, TimeUnit.SECONDS), "FX task timed out");
        if (error[0] != null) {
            throw error[0];
        }
    }

    private static M3Theme theme(boolean dark) {
        return M3Theme.fromSeed(Color.web("#6750A4"), ThemeVariant.TONAL_SPOT, dark);
    }

    /** One case per chart: a factory plus the package-private render counter accessor. */
    private record ChartCase(String name, Supplier<Region> factory,
                             ToIntFunction<Region> renderCount) {
    }

    private static final ChartCase[] CASES = {
        new ChartCase("M3BarChart", M3BarChart::new,
                r -> ((M3BarChart) r).renderCountForTesting),
        new ChartCase("M3SplineChart", M3SplineChart::new,
                r -> ((M3SplineChart) r).renderCountForTesting),
        new ChartCase("M3ScatterPlot", M3ScatterPlot::new,
                r -> ((M3ScatterPlot) r).renderCountForTesting),
        new ChartCase("M3DonutChart", M3DonutChart::new,
                r -> ((M3DonutChart) r).renderCountForTesting),
        new ChartCase("M3GaugeChart", M3GaugeChart::new,
                r -> ((M3GaugeChart) r).renderCountForTesting),
        new ChartCase("M3RadarChart", M3RadarChart::new,
                r -> ((M3RadarChart) r).renderCountForTesting),
        new ChartCase("M3FunnelChart", M3FunnelChart::new,
                r -> ((M3FunnelChart) r).renderCountForTesting),
        new ChartCase("M3Heatmap", M3Heatmap::new,
                r -> ((M3Heatmap) r).renderCountForTesting),
        new ChartCase("M3Sparkline", M3Sparkline::new,
                r -> ((M3Sparkline) r).renderCountForTesting),
        new ChartCase("M3ActivityCalendar", M3ActivityCalendar::new,
                r -> ((M3ActivityCalendar) r).renderCountForTesting),
    };

    @Test
    void everyChartRepaintsWhenTheSceneThemeChanges() throws Throwable {
        for (ChartCase c : CASES) {
            Region[] regionHolder = new Region[1];
            Stage[] stageHolder = new Stage[1];
            int[] before = new int[1];
            int[] after = new int[1];
            try {
                runOnFx(() -> {
                    Region chart = c.factory().get();
                    regionHolder[0] = chart;
                    Stage stage = new Stage();
                    stage.setScene(new Scene(new StackPane(chart), 600, 400));
                    M3Stylesheets.applyTo(stage.getScene(), theme(false));
                    stage.show();
                    stageHolder[0] = stage;
                });
                runOnFx(() -> before[0] = c.renderCount().applyAsInt(regionHolder[0]));
                runOnFx(() -> M3Stylesheets.applyTo(stageHolder[0].getScene(), theme(true)));
                runOnFx(() -> after[0] = c.renderCount().applyAsInt(regionHolder[0]));
                assertTrue(after[0] > before[0],
                        c.name() + " did not repaint on a theme change (before="
                                + before[0] + ", after=" + after[0] + ")");
            } finally {
                runOnFx(() -> {
                    stageHolder[0].hide();
                    stageHolder[0].close();
                });
            }
        }
    }
}
