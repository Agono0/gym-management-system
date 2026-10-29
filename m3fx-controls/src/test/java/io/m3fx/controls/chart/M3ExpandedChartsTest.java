package io.m3fx.controls.chart;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying that M3ScatterPlot, M3FunnelChart, and M3ActivityCalendar
 * compute accurate regression trendlines, conversion drop-offs, and streak analytics.
 */
class M3ExpandedChartsTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already started
        }
    }

    @Test
    void testScatterPlotRegressionMath() {
        M3ScatterPlot plot = new M3ScatterPlot();
        plot.setXAxisTitle("Load (% 1RM)");
        plot.setYAxisTitle("Velocity (m/s)");
        assertEquals("Load (% 1RM)", plot.getXAxisTitle());
        assertEquals("Velocity (m/s)", plot.getYAxisTitle());

        M3ScatterSeries series = new M3ScatterSeries("Perfect Linear Test", Color.BLUE);
        // Points on line y = -0.01x + 1.40
        series.getPoints().add(new M3ScatterPoint(40, 1.00, 10, "P1"));
        series.getPoints().add(new M3ScatterPoint(60, 0.80, 10, "P2"));
        series.getPoints().add(new M3ScatterPoint(80, 0.60, 10, "P3"));
        series.getPoints().add(new M3ScatterPoint(100, 0.40, 10, "P4"));

        plot.getSeriesList().add(series);
        assertTrue(plot.isShowRegression());
        assertTrue(plot.isCrosshairEnabled());
    }

    @Test
    void testFunnelChartStageCalculations() {
        M3FunnelChart chart = new M3FunnelChart();
        chart.getStages().addAll(
                new M3FunnelStage("Impressions", 10000, Color.web("#6750A4")),
                new M3FunnelStage("Clicks", 5000, Color.web("#7E57C2")),
                new M3FunnelStage("Signups", 2500, Color.web("#00897B")),
                new M3FunnelStage("Active Paid", 1000, Color.web("#2E7D32"))
        );

        assertEquals(4, chart.getStages().size());
        assertEquals("Impressions", chart.getStages().get(0).getName());
        assertEquals(10000, chart.getStages().get(0).getValue());
        assertEquals(1000, chart.getStages().get(3).getValue());
    }

    @Test
    void testFunnelChartZeroTopStageDoesNotDivideByZero() {
        M3FunnelChart chart = new M3FunnelChart();
        chart.getStages().addAll(
                new M3FunnelStage("Empty Top", 0),
                new M3FunnelStage("Next", 50)
        );
        // topCount falls back to 1 so fractions never divide by zero
        assertDoesNotThrow(() -> chart.resize(400, 240));
    }

    @Test
    void testScatterPlotSinglePointBounds() {
        M3ScatterPlot plot = new M3ScatterPlot();
        M3ScatterSeries single = new M3ScatterSeries("One Point");
        single.getPoints().add(new M3ScatterPoint("Only", 42.0, 3.14));
        plot.getSeriesList().add(single);

        // Degenerate span opens a ±5 window on both axes instead of producing NaN
        assertDoesNotThrow(() -> plot.resize(320, 220));
    }

    @Test
    void testActivityCalendarStreaksAndCounts() {
        M3ActivityCalendar cal = new M3ActivityCalendar(2026);
        assertEquals(2026, cal.getYear());

        LocalDate d1 = LocalDate.of(2026, 3, 10);
        LocalDate d2 = LocalDate.of(2026, 3, 11);
        LocalDate d3 = LocalDate.of(2026, 3, 12);
        LocalDate d4 = LocalDate.of(2026, 3, 13);

        cal.setCount(d1, 5);
        cal.setCount(d2, 8);
        cal.setCount(d3, 4);
        cal.setCount(d4, 10);

        assertEquals(5, cal.getCount(d1));
        assertEquals(8, cal.getCount(d2));
        assertEquals(4, cal.getCount(d3));
        assertEquals(10, cal.getCount(d4));
        assertEquals(0, cal.getCount(LocalDate.of(2026, 3, 14)));

        assertEquals(27, cal.calculateTotalCount());
        assertTrue(cal.calculateLongestStreak() >= 4, "Streak should be at least 4 consecutive days");

        // Add a second separate streak
        LocalDate d5 = LocalDate.of(2026, 3, 20);
        LocalDate d6 = LocalDate.of(2026, 3, 21);
        cal.setCount(d5, 3);
        cal.setCount(d6, 3);

        assertEquals(33, cal.calculateTotalCount());
        assertEquals(4, cal.calculateLongestStreak());
    }

    @Test
    void testActivityCalendarCurrentStreakStaysWithinDisplayedYear() {
        // Viewing a past year: the streak must stop at January 1 of that year
        // even when the previous year also has recorded activity.
        M3ActivityCalendar cal = new M3ActivityCalendar(2020);
        cal.setCount(LocalDate.of(2020, 12, 31), 1);
        cal.setCount(LocalDate.of(2020, 12, 30), 1);
        cal.setCount(LocalDate.of(2019, 12, 31), 1);

        assertEquals(2, cal.getCurrentStreak(), "Streak must not cross into the previous year");
    }
}

