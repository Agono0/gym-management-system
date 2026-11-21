package io.m3fx.controls.chart;

import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for Phase 1 Cyber Charts & Analytics Suite.
 */
class M3ChartsTest {

    @BeforeAll
    static void initFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX failed to initialize");
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
        assertTrue(latch.await(5, TimeUnit.SECONDS), "FX task timed out");
        if (error[0] != null) {
            throw error[0];
        }
    }

    @Test
    void dataPointAndSeriesModel() {
        M3DataPoint pt = new M3DataPoint("Mon", 42.5);
        assertEquals("Mon", pt.getLabel());
        assertEquals(42.5, pt.getValue());

        M3DataSeries series = M3DataSeries.of("Revenue", Color.BLUE, 10, 20, 30);
        assertEquals("Revenue", series.getName());
        assertEquals(Color.BLUE, series.getColor());
        assertEquals(3, series.getData().size());
        assertEquals(20.0, series.getData().get(1).getValue());
    }

    @Test
    void sparklineCreationAndRender() throws Throwable {
        runOnFx(() -> {
            M3Sparkline spark = M3Sparkline.of(12.0, 18.5, 14.2, 22.0, 30.1);
            assertEquals(5, spark.getData().size());
            assertTrue(spark.isShowGlowDot());
            spark.resize(100, 32);
            assertDoesNotThrow(() -> spark.layout());
        });
    }

    @Test
    void sparklineDegenerateInputDoesNotThrow() throws Throwable {
        runOnFx(() -> {
            // Empty and single-value sparklines must render blank, not NaN or throw
            M3Sparkline empty = new M3Sparkline();
            empty.resize(80, 24);
            assertDoesNotThrow(() -> empty.layout());

            M3Sparkline single = M3Sparkline.of(7.5);
            single.resize(80, 24);
            assertDoesNotThrow(() -> single.layout());

            // Out-of-range style values are clamped, not passed to the canvas
            M3Sparkline clamped = M3Sparkline.of(1, 5, 3);
            clamped.setFillOpacity(-0.5);
            clamped.setLineWidth(-2);
            assertDoesNotThrow(() -> clamped.layout());

            // Flat data autoscales to a visible band
            M3Sparkline flat = M3Sparkline.of(4, 4, 4, 4);
            assertDoesNotThrow(() -> flat.layout());
        });
    }

    @Test
    void splineChartSeriesManagement() throws Throwable {
        runOnFx(() -> {
            M3SplineChart chart = new M3SplineChart();
            M3DataSeries s1 = M3DataSeries.of("Active Members", 100, 150, 130, 210, 280);
            M3DataSeries s2 = M3DataSeries.of("Casual Passes", 40, 55, 35, 70, 95);

            chart.getSeriesList().addAll(s1, s2);
            assertEquals(2, chart.getSeriesList().size());
            assertTrue(chart.isAreaFill());
            assertTrue(chart.isShowGrid());
            chart.resize(400, 200);
            assertDoesNotThrow(() -> chart.layout());
        });
    }

    @Test
    void splineChartNegativeAndFlatDataRender() throws Throwable {
        runOnFx(() -> {
            // Negative values must scale below the zero baseline instead of clamping to 0
            M3SplineChart negative = new M3SplineChart();
            negative.getSeriesList().add(M3DataSeries.of("Balance", -20, -5, -35, -10));
            negative.resize(300, 180);
            assertDoesNotThrow(() -> negative.layout());

            // All-equal values must open a window, never divide by a zero range
            M3SplineChart flat = new M3SplineChart();
            flat.getSeriesList().add(M3DataSeries.of("Flat", 7, 7, 7, 7));
            flat.resize(300, 180);
            assertDoesNotThrow(() -> flat.layout());

            M3SplineChart flatNegative = new M3SplineChart();
            flatNegative.getSeriesList().add(M3DataSeries.of("Flat Negative", -7, -7, -7));
            flatNegative.resize(300, 180);
            assertDoesNotThrow(() -> flatNegative.layout());
        });
    }

    @Test
    void barChartGroupedSeries() throws Throwable {
        runOnFx(() -> {
            M3BarChart barChart = new M3BarChart();
            M3DataSeries monthly = M3DataSeries.of("Monthly", 45, 60, 75);
            barChart.getSeriesList().add(monthly);
            assertEquals(1, barChart.getSeriesList().size());
            barChart.resize(300, 180);
            assertDoesNotThrow(() -> barChart.layout());
        });
    }

    @Test
    void barChartNegativeAndFlatDataRender() throws Throwable {
        runOnFx(() -> {
            // Mixed signs: bars hang from the zero baseline instead of being dropped
            M3BarChart mixed = new M3BarChart();
            mixed.getSeriesList().add(M3DataSeries.of("PnL", 45, -30, 60, -10));
            mixed.resize(320, 200);
            assertDoesNotThrow(() -> mixed.layout());

            // All-negative data
            M3BarChart negative = new M3BarChart();
            negative.getSeriesList().add(M3DataSeries.of("Debt", -5, -15, -8));
            negative.resize(320, 200);
            assertDoesNotThrow(() -> negative.layout());

            // Flat zero data falls back to a 0..10 axis
            M3BarChart flat = new M3BarChart();
            flat.getSeriesList().add(M3DataSeries.of("Idle", 0, 0, 0));
            flat.resize(320, 200);
            assertDoesNotThrow(() -> flat.layout());
        });
    }

    @Test
    void donutChartRingsAndHeadline() throws Throwable {
        runOnFx(() -> {
            M3DonutChart donut = new M3DonutChart();
            donut.setCenterHeadline("85%");
            donut.setCenterSubtitle("Target Met");
            donut.addRing("Cardio", 85, 100, Color.CYAN);
            donut.addRing("Strength", 70, 100, Color.MAGENTA);

            assertEquals("85%", donut.getCenterHeadline());
            assertEquals("Target Met", donut.getCenterSubtitle());
            assertEquals(2, donut.getRings().size());
            donut.resize(200, 200);
            assertDoesNotThrow(() -> donut.layout());
        });
    }

    @Test
    void donutChartRingValuesClampToFullCircle() throws Throwable {
        runOnFx(() -> {
            M3DonutChart donut = new M3DonutChart();
            // Over-100% and negative values must clamp to [0, 1] progress, never throw
            donut.addRing("Over", 250, 100, Color.CYAN);
            donut.addRing("Negative", -25, 100, Color.MAGENTA);
            donut.addRing("BadMax", 50, 0, Color.BLUE); // maxValue <= 0 falls back to 100
            assertEquals(100.0, donut.getRings().get(2).getMaxValue());
            donut.resize(200, 200);
            assertDoesNotThrow(() -> donut.layout());
        });
    }

    @Test
    void heatmapMatrixAndLookup() throws Throwable {
        runOnFx(() -> {
            M3Heatmap heatmap = new M3Heatmap();
            heatmap.setDimensions(7, 16);
            heatmap.setValue(2, 4, 0.92);

            assertEquals(0.92, heatmap.getValue(2, 4), 0.001);
            assertEquals(0.0, heatmap.getValue(0, 0), 0.001);
            heatmap.resize(500, 220);
            assertDoesNotThrow(() -> heatmap.layout());
        });
    }

    @Test
    void radarChartAxesAndSeries() throws Throwable {
        runOnFx(() -> {
            M3RadarChart radar = new M3RadarChart("STR", "END", "AGI", "SPD");
            assertEquals(4, radar.getAxes().size());

            M3DataSeries series = new M3DataSeries("Athletics");
            series.getData().addAll(
                    new M3DataPoint("STR", 80),
                    new M3DataPoint("END", 75),
                    new M3DataPoint("AGI", 90),
                    new M3DataPoint("SPD", 85)
            );
            radar.getSeriesList().add(series);

            assertEquals(1, radar.getSeriesList().size());
            assertEquals(100.0, radar.getMaxValue());
            assertTrue(radar.isFilled());
            assertTrue(radar.isShowPoints());

            radar.resize(360, 300);
            assertDoesNotThrow(() -> radar.layout());
        });
    }

    @Test
    void gaugeChartValuesAndClamping() throws Throwable {
        runOnFx(() -> {
            M3GaugeChart gauge = new M3GaugeChart("Heart Rate", 40, 200, "BPM");
            assertEquals(40.0, gauge.getMinValue());
            assertEquals(200.0, gauge.getMaxValue());
            assertEquals("BPM", gauge.getUnit());
            assertEquals("Heart Rate", gauge.getTitle());

            gauge.setValue(160.0);
            assertEquals(160.0, gauge.getValue());

            gauge.resize(220, 220);
            assertDoesNotThrow(() -> gauge.layout());
        });
    }

    // =========================================================================
    // Visual QA regressions: tick degeneration, hub overflow, label fill state
    // =========================================================================

    @Test
    void chartTicksAreEvenlySpacedDistinctNiceValues() {
        // "1, 1, 0, 0, 0" evidence: a 0..1.05 axis (4 divisions) degenerated when each
        // division rounded independently. Nice steps keep every label distinct.
        double[] ticks = ChartTicks.compute(0.0, 1.05, 4);
        assertTrue(ticks.length >= 2, "at least two ticks");
        assertTrue(ticks[0] <= 0.0 && ticks[ticks.length - 1] >= 1.05,
                "ticks cover the data range");
        for (int i = 1; i < ticks.length; i++) {
            assertTrue(ticks[i] > ticks[i - 1], "ticks strictly increase");
            double step = ticks[i] - ticks[i - 1];
            assertEquals(ticks[1] - ticks[0], step, 1e-9, "evenly spaced ticks");
        }
        // "9, 7, 5, 2, 0" evidence: an uneven padded range (0..9.2, 4 divisions) now snaps
        // to a nice step with clean labels.
        double[] uneven = ChartTicks.compute(0.0, 9.2, 4);
        assertArrayEquals(new double[] {0.0, 2.5, 5.0, 7.5, 10.0}, uneven, 1e-9,
                "the padded range snaps to 2.5 steps");
        assertArrayEquals(new String[] {"0", "2.5", "5", "7.5", "10"},
                streamLabels(uneven), "labels trim trailing zeros and stay distinct");
    }

    private static String[] streamLabels(double[] values) {
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = ChartTicks.label(values[i]);
        }
        return labels;
    }

    @Test
    void chartTickLabelsNeverRepeatAcrossDegenerateRanges() {
        for (double[] range : new double[][] {{0, 1}, {0, 0.5}, {0, 1000}, {-5, 5}, {3, 3.2}}) {
            double[] ticks = ChartTicks.compute(range[0], range[1], 4);
            java.util.Set<String> labels = new java.util.HashSet<>();
            for (double tick : ticks) {
                labels.add(ChartTicks.label(tick));
            }
            assertEquals(ticks.length, labels.size(),
                    "range " + range[0] + ".." + range[1] + " formats to distinct labels");
        }
    }




    @Test
    void heatmapColumnLabelsUseTheTextTokenNotAStaleCellColor() throws Throwable {
        runOnFx(() -> {
            M3Heatmap heatmap = new M3Heatmap();
            heatmap.resize(500, 220);
            heatmap.layout();

            Color expected = ThemeColors.resolve(heatmap, ColorRole.ON_SURFACE_VARIANT);
            assertNotNull(heatmap.columnLabelFillForTesting, "render ran and captured the fill");
            assertEquals(expected, heatmap.columnLabelFillForTesting,
                    "column labels paint in the on-surface-variant token, not the last "
                            + "cell color (the near-invisible attendance/trainer evidence)");
        });
    }

    @Test
    void donutRingsStayBetweenHubAndOuterEdgeAndCenterTextFitsTheHole() throws Throwable {
        runOnFx(() -> {
            // The progress-screen evidence: two rings at 140px squeezed the hub to ~49px while
            // the 22px headline overflowed it and collided with the ring.
            M3DonutChart donut = new M3DonutChart();
            donut.setCenterHeadline("84.8 kg");
            donut.setCenterSubtitle("Total mass");
            donut.addRing("Lean mass", 70, 84.8, Color.CYAN);
            donut.addRing("Fat mass", 14.8, 84.8, Color.RED);
            // A scene carries the CSS (m3fx.css sizes the headline 22px bold). CSS first, then
            // the size change, matching the real pulse order so the render measures with the
            // CSS-resolved font; the next CSS pulse lands the inline fitted size.
            javafx.scene.Scene scene = new javafx.scene.Scene(new javafx.scene.layout.StackPane(donut));
            io.m3fx.controls.M3Stylesheets.applyTo(scene, io.m3fx.core.theme.M3Theme.baseline(false));
            donut.applyCss();
            donut.resize(140, 140);
            donut.applyCss();
            donut.layout();

            double maxOuterRadius = 140 / 2.0 - 12.0;
            double hole = donut.innerHoleRadiusForTesting;
            assertTrue(hole >= maxOuterRadius * 0.35,
                    "a two-ring donut reserves a real center hub, hole=" + hole);
            assertTrue(hole < maxOuterRadius,
                    "the hole stays inside the outer edge");

            Label headline = (Label) donut.lookup(".m3-donut-headline");
            Label subtitle = (Label) donut.lookup(".m3-donut-subtitle");
            assertNotNull(headline);
            assertNotNull(subtitle);
            for (Label label : new Label[] {headline, subtitle}) {
                javafx.scene.text.Text measure = new javafx.scene.text.Text(label.getText());
                measure.setFont(label.getFont());
                double textWidth = measure.getLayoutBounds().getWidth();
                assertTrue(textWidth <= hole * 2.0,
                        "center text fits inside the hub hole, width=" + textWidth
                                + " hole=" + (hole * 2.0));
            }
            assertTrue(headline.getFont().getSize() < 22.0,
                    "the CSS headline size shrinks to fit the small hub");
        });
    }

    @Test
    void donutCenterTextKeepsCssSizeWhenTheHubIsRoomy() throws Throwable {
        runOnFx(() -> {
            M3DonutChart donut = new M3DonutChart();
            donut.setCenterHeadline("83%");
            donut.addRing("Operational", 5, 6, Color.CYAN);
            javafx.scene.Scene scene = new javafx.scene.Scene(new javafx.scene.layout.StackPane(donut));
            io.m3fx.controls.M3Stylesheets.applyTo(scene, io.m3fx.core.theme.M3Theme.baseline(false));
            donut.applyCss();
            donut.resize(240, 240);
            donut.applyCss();
            donut.layout();

            Label headline = (Label) donut.lookup(".m3-donut-headline");
            assertNotNull(headline);
            assertEquals("", headline.getStyle(),
                    "a roomy hub leaves the CSS font size (no inline override)");
        });
    }
}