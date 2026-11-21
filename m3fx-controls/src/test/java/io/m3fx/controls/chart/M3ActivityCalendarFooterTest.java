package io.m3fx.controls.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.m3fx.core.token.TypeScale;
import javafx.application.Platform;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Q7: the stats/legend footer is measured — below ~600px card width the two segments
 * no longer fit side by side, so the calendar stacks them (stats line, legend line) and
 * ellipsizes the stats copy; the full string stays on accessibleText. Verified at the
 * 420px card minimum via measured text bounds.
 */
class M3ActivityCalendarFooterTest {

    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> { });
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

    private static M3ActivityCalendar seededCalendar() {
        M3ActivityCalendar cal = new M3ActivityCalendar();
        for (int day = 3; day <= 14; day++) {
            cal.setActivity(LocalDate.of(cal.getYear(), 1, day), 1 + (day % 4));
        }
        return cal;
    }

    private static Font footerFont() {
        return Font.font(TypeScale.DEFAULT_FAMILY, FontWeight.NORMAL,
                TypeScale.LABEL_SMALL.size());
    }

    @Test
    void footerStacksAt420CardWidthAndEllipsizes() throws Throwable {
        runOnFx(() -> {
            M3ActivityCalendar cal = seededCalendar();
            cal.resize(420, 300);
            assertTrue(cal.footerStacked,
                    "420px card width must stack the stats line and the legend line");
            // The full stats string survives on accessibleText.
            String accessible = cal.getAccessibleText();
            assertNotNull(accessible, "accessibleText must be populated");
            assertTrue(accessible.contains("total workouts") && accessible.contains("streak"),
                    "accessibleText must carry the full stats string");
            // The drawn (ellipsized) stats copy fits the measured grid width.
            double gridWidth = 420 - 32 - 12;
            String drawn = M3ActivityCalendar.ellipsize(footerFont(), accessible, gridWidth);
            assertTrue(M3ActivityCalendar.textWidth(footerFont(), drawn) <= gridWidth,
                    "ellipsized stats copy must fit the measured grid width");
        });
    }

    @Test
    void footerStaysOnOneLineWhenWide() throws Throwable {
        runOnFx(() -> {
            M3ActivityCalendar cal = seededCalendar();
            cal.resize(1280, 400);
            assertFalse(cal.footerStacked,
                    "wide cards keep the single-line stats + legend footer");
        });
    }

    @Test
    void ellipsizeKeepsEllipsisAndFits() throws Throwable {
        runOnFx(() -> {
            Font font = footerFont();
            String stats = "Year 2026: 123 total workouts  •  Current streak: 45 days  •  Longest streak: 67 days";
            String cut = M3ActivityCalendar.ellipsize(font, stats, 300);
            assertTrue(cut.endsWith("…"), "ellipsized copy must end with the ellipsis");
            assertTrue(M3ActivityCalendar.textWidth(font, cut) <= 300,
                    "ellipsized copy must fit the max width");
            assertEquals(stats, M3ActivityCalendar.ellipsize(font, stats,
                            M3ActivityCalendar.textWidth(font, stats) + 1),
                    "copy that already fits must come back unchanged");
        });
    }
}
