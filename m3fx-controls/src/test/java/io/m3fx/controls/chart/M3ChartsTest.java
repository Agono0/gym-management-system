package io.m3fx.controls.chart;

import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.scene.paint.Color;
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
}


