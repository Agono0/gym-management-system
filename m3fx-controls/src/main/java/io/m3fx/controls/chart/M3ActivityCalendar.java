package io.m3fx.controls.chart;

import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.property.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * M3 52-Week Workout Consistency Punch Card Heatmap.
 * Renders a 7 rows (Mon-Sun) × 53 columns (weeks of year) matrix with a
 * 4-level color intensity ramp, month and day labels, streak calculations,
 * and date-precise hover inspection tooltips.
 */
public class M3ActivityCalendar extends Region {

    private static final int WEEKS = 53;
    private static final int DAYS_IN_WEEK = 7;

    private final Map<LocalDate, Integer> activityMap = new HashMap<>();

    private final IntegerProperty year = new SimpleIntegerProperty(this, "year", LocalDate.now().getYear());
    private final ObjectProperty<Color> accentColor = new SimpleObjectProperty<>(this, "accentColor", null);
    private final BooleanProperty showDayLabels = new SimpleBooleanProperty(this, "showDayLabels", true);
    private final BooleanProperty showMonthLabels = new SimpleBooleanProperty(this, "showMonthLabels", true);
    private final BooleanProperty showStatsFooter = new SimpleBooleanProperty(this, "showStatsFooter", true);

    private final Canvas canvas = new Canvas();

    private LocalDate hoveredDate = null;
    private int hoveredWorkoutCount = 0;
    private double hoverMouseX = -1;
    private double hoverMouseY = -1;
    private boolean isHovering = false;

    private static final DateTimeFormatter TOOLTIP_DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy");

    public M3ActivityCalendar() {
        getStyleClass().add("m3-activity-calendar");
        getChildren().add(canvas);

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        widthProperty().addListener((obs, o, n) -> render());
        heightProperty().addListener((obs, o, n) -> render());
        sceneProperty().addListener((obs, o, n) -> render());

        year.addListener((obs, o, n) -> render());
        accentColor.addListener((obs, o, n) -> render());
        showDayLabels.addListener((obs, o, n) -> render());
        showMonthLabels.addListener((obs, o, n) -> render());
        showStatsFooter.addListener((obs, o, n) -> render());

        initInteractivity();

        setPrefSize(680, 190);
        setMinSize(420, 140);
    }

    public M3ActivityCalendar(int year) {
        this();
        setYear(year);
    }

    private void initInteractivity() {
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            hoverMouseX = e.getX();
            hoverMouseY = e.getY();
            isHovering = true;
            render();
        });

        canvas.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            isHovering = false;
            hoveredDate = null;
            hoveredWorkoutCount = 0;
            hoverMouseX = -1;
            hoverMouseY = -1;
            render();
        });
    }

    /**
     * Records (or replaces) the workout count for one date.
     *
     * @param date calendar date (ignored when {@code null})
     * @param count workout count (negative values clamped to 0)
     */
    public void setActivity(LocalDate date, int count) {
        if (date != null) {
            activityMap.put(date, Math.max(0, count));
            render();
        }
    }

    /**
     * Reads the recorded workout count for one date.
     *
     * @param date calendar date
     * @return the stored count, or 0 when no activity was recorded
     */
    public int getActivity(LocalDate date) {
        return activityMap.getOrDefault(date, 0);
    }

    /** Alias for {@link #setActivity(LocalDate, int)}. */
    public void setCount(LocalDate date, int count) {
        setActivity(date, count);
    }

    /** Alias for {@link #getActivity(LocalDate)}. */
    public int getCount(LocalDate date) {
        return getActivity(date);
    }

    /** Alias for {@link #getTotalWorkouts()}. */
    public int calculateTotalCount() {
        return getTotalWorkouts();
    }

    /** Alias for {@link #getCurrentStreak()}. */
    public int calculateCurrentStreak() {
        return getCurrentStreak();
    }

    /** Alias for {@link #getLongestStreak()}. */
    public int calculateLongestStreak() {
        return getLongestStreak();
    }

    /**
     * Replaces the whole activity map.
     *
     * @param activities date-to-count map, or {@code null} to clear
     */
    public void setActivities(Map<LocalDate, Integer> activities) {
        activityMap.clear();
        if (activities != null) {
            activityMap.putAll(activities);
        }
        render();
    }

    /** Clears all recorded activity. */
    public void clearActivities() {
        activityMap.clear();
        render();
    }

    /** The calendar year being displayed. */
    public IntegerProperty yearProperty() {
        return year;
    }

    public int getYear() {
        return year.get();
    }

    public void setYear(int year) {
        this.year.set(year);
    }

    /** Custom accent color for the intensity ramp, or {@code null} for theme primary. */
    public ObjectProperty<Color> accentColorProperty() {
        return accentColor;
    }

    public Color getAccentColor() {
        return accentColor.get();
    }

    public void setAccentColor(Color color) {
        this.accentColor.set(color);
    }

    /** Whether the Mon/Wed/Fri day labels are drawn on the left. */
    public BooleanProperty showDayLabelsProperty() {
        return showDayLabels;
    }

    public boolean isShowDayLabels() {
        return showDayLabels.get();
    }

    public void setShowDayLabels(boolean show) {
        this.showDayLabels.set(show);
    }

    /** Whether the month labels are drawn across the top. */
    public BooleanProperty showMonthLabelsProperty() {
        return showMonthLabels;
    }

    public boolean isShowMonthLabels() {
        return showMonthLabels.get();
    }

    public void setShowMonthLabels(boolean show) {
        this.showMonthLabels.set(show);
    }

    /** Whether the totals/streaks footer and Less–More legend are drawn. */
    public BooleanProperty showStatsFooterProperty() {
        return showStatsFooter;
    }

    public boolean isShowStatsFooter() {
        return showStatsFooter.get();
    }

    public void setShowStatsFooter(boolean show) {
        this.showStatsFooter.set(show);
    }

    /** Total workout count recorded for the displayed year. */
    public int getTotalWorkouts() {
        int sum = 0;
        int targetYear = getYear();
        for (Map.Entry<LocalDate, Integer> entry : activityMap.entrySet()) {
            if (entry.getKey().getYear() == targetYear) {
                sum += entry.getValue();
            }
        }
        return sum;
    }

    /**
     * Consecutive active days ending today (or ending December 31 when viewing a past year).
     * The walk never crosses below January 1 of the displayed year.
     *
     * @return current streak length in days
     */
    public int getCurrentStreak() {
        LocalDate current = LocalDate.now();
        if (current.getYear() != getYear()) {
            current = LocalDate.of(getYear(), 12, 31);
        }
        LocalDate earliest = LocalDate.of(getYear(), 1, 1);
        int streak = 0;
        while (!current.isBefore(earliest) && activityMap.getOrDefault(current, 0) > 0) {
            streak++;
            current = current.minusDays(1);
        }
        return streak;
    }

    /**
     * Longest run of consecutive active days anywhere in the displayed year.
     *
     * @return longest streak length in days
     */
    public int getLongestStreak() {
        LocalDate date = LocalDate.of(getYear(), 1, 1);
        LocalDate end = LocalDate.of(getYear(), 12, 31);
        int maxStreak = 0;
        int current = 0;

        while (!date.isAfter(end)) {
            if (activityMap.getOrDefault(date, 0) > 0) {
                current++;
                if (current > maxStreak) maxStreak = current;
            } else {
                current = 0;
            }
            date = date.plusDays(1);
        }
        return maxStreak;
    }

    private Color resolveIntensityColor(int count) {
        Color base = getAccentColor() != null ? getAccentColor() : ThemeColors.resolve(this, ColorRole.PRIMARY);
        Color empty = ThemeColors.resolve(this, ColorRole.SURFACE_CONTAINER_HIGH);

        if (count <= 0) {
            return empty;
        } else if (count == 1) {
            return Color.color(base.getRed(), base.getGreen(), base.getBlue(), 0.32);
        } else if (count == 2) {
            return Color.color(base.getRed(), base.getGreen(), base.getBlue(), 0.65);
        } else {
            return base;
        }
    }

    private void render() {
        double w = getWidth() > 0 ? getWidth() : getPrefWidth();
        double h = getHeight() > 0 ? getHeight() : getPrefHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        double padLeft = isShowDayLabels() ? 32.0 : 12.0;
        double padTop = isShowMonthLabels() ? 22.0 : 10.0;
        double padBottom = isShowStatsFooter() ? 26.0 : 10.0;
        double padRight = 12.0;

        double gridW = w - padLeft - padRight;
        double gridH = h - padTop - padBottom;
        if (gridW <= 0 || gridH <= 0) return;

        double cellSize = Math.min((gridW / WEEKS) - 2.5, (gridH / DAYS_IN_WEEK) - 2.5);
        if (cellSize < 4.0) cellSize = 4.0;
        double gap = 2.5;

        Color textColor = ThemeColors.resolve(this, ColorRole.ON_SURFACE_VARIANT);

        // Day Labels (Mon, Wed, Fri)
        if (isShowDayLabels()) {
            gc.setFill(textColor);
            gc.setFont(Font.font("System", FontWeight.NORMAL, 9));
            gc.fillText("Mon", 6, padTop + 0 * (cellSize + gap) + cellSize * 0.8);
            gc.fillText("Wed", 6, padTop + 2 * (cellSize + gap) + cellSize * 0.8);
            gc.fillText("Fri", 6, padTop + 4 * (cellSize + gap) + cellSize * 0.8);
        }

        // Calendar Date Setup
        LocalDate jan1 = LocalDate.of(getYear(), 1, 1);
        int jan1Dow = jan1.getDayOfWeek().getValue(); // 1 = Mon, 7 = Sun
        LocalDate startDate = jan1.minusDays(jan1Dow - 1);

        hoveredDate = null;
        hoveredWorkoutCount = 0;
        double hoveredCellX = 0, hoveredCellY = 0;

        int prevMonth = -1;

        // Draw 53 Columns × 7 Rows
        for (int col = 0; col < WEEKS; col++) {
            double cellX = padLeft + col * (cellSize + gap);

            for (int row = 0; row < DAYS_IN_WEEK; row++) {
                LocalDate date = startDate.plusDays((long) col * DAYS_IN_WEEK + row);
                double cellY = padTop + row * (cellSize + gap);

                // Month Label across top
                if (isShowMonthLabels() && row == 0 && date.getYear() == getYear()) {
                    int m = date.getMonthValue();
                    if (m != prevMonth && col < WEEKS - 2) {
                        prevMonth = m;
                        String monthName = date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                        gc.setFill(textColor);
                        gc.setFont(Font.font("System", FontWeight.BOLD, 9));
                        gc.fillText(monthName, cellX, padTop - 6);
                    }
                }

                // If outside the active year, render dimmed placeholder
                if (date.getYear() != getYear()) {
                    gc.setFill(Color.color(
                            textColor.getRed(), textColor.getGreen(), textColor.getBlue(), 0.08));
                    gc.fillRoundRect(cellX, cellY, cellSize, cellSize, 3, 3);
                    continue;
                }

                int count = activityMap.getOrDefault(date, 0);

                // Check hover
                boolean isCellHovered = isHovering
                        && hoverMouseX >= cellX && hoverMouseX <= cellX + cellSize
                        && hoverMouseY >= cellY && hoverMouseY <= cellY + cellSize;

                if (isCellHovered) {
                    hoveredDate = date;
                    hoveredWorkoutCount = count;
                    hoveredCellX = cellX;
                    hoveredCellY = cellY;
                }

                Color fill = resolveIntensityColor(count);
                if (isCellHovered) fill = fill.brighter();

                gc.setFill(fill);
                gc.fillRoundRect(cellX, cellY, cellSize, cellSize, 3, 3);

                if (isCellHovered) {
                    gc.setStroke(ThemeColors.resolve(this, ColorRole.ON_SURFACE));
                    gc.setLineWidth(1.2);
                    gc.strokeRoundRect(cellX, cellY, cellSize, cellSize, 3, 3);
                }
            }
        }

        // Stats Footer & Legend
        if (isShowStatsFooter()) {
            double footerY = padTop + DAYS_IN_WEEK * (cellSize + gap) + 16.0;

            gc.setFill(textColor);
            gc.setFont(Font.font("System", FontWeight.NORMAL, 10));
            String statsStr = String.format("Year %d: %d total workouts  •  Current streak: %d days  •  Longest streak: %d days",
                    getYear(), getTotalWorkouts(), getCurrentStreak(), getLongestStreak());
            gc.fillText(statsStr, padLeft, footerY);

            // Legend on right (Less -> More)
            double legendX = w - padRight - 100.0;
            gc.fillText("Less", legendX - 26, footerY);
            for (int lvl = 0; lvl <= 3; lvl++) {
                gc.setFill(resolveIntensityColor(lvl));
                gc.fillRoundRect(legendX + lvl * 12.0, footerY - 8.0, 9.0, 9.0, 2, 2);
            }
            gc.setFill(textColor);
            gc.fillText("More", legendX + 52.0, footerY);
        }

        // Tooltip
        if (hoveredDate != null) {
            drawTooltip(gc, hoveredCellX, hoveredCellY, hoveredDate, hoveredWorkoutCount);
        }
    }

    private void drawTooltip(GraphicsContext gc, double cellX, double cellY, LocalDate date, int count) {
        double tipW = 180.0;
        double tipH = 46.0;
        double tipX = cellX + 12;
        if (tipX + tipW > getWidth() - 10) tipX = Math.max(10, cellX - tipW - 12);
        double tipY = Math.max(8, Math.min(getHeight() - tipH - 8, cellY - tipH / 2.0));

        // M3 plain tooltip: inverse surface container, inverse on-surface content, 4dp radius
        Color bg = ThemeColors.resolve(this, ColorRole.INVERSE_SURFACE);
        Color text = ThemeColors.resolve(this, ColorRole.INVERSE_ON_SURFACE);
        Color textVariant = ThemeColors.resolve(this, ColorRole.INVERSE_ON_SURFACE);

        gc.setFill(bg);
        gc.fillRoundRect(tipX, tipY, tipW, tipH, 4, 4);

        gc.setFill(text);
        gc.setFont(Font.font("System", FontWeight.BOLD, 10));
        String countText = (count == 0) ? "No workouts recorded" : (count + (count == 1 ? " workout completed" : " workouts completed"));
        gc.fillText(countText, tipX + 8, tipY + 16);

        gc.setFont(Font.font("System", FontWeight.NORMAL, 9.5));
        gc.setFill(textVariant);
        gc.fillText(date.format(TOOLTIP_DATE_FMT), tipX + 8, tipY + 32);
    }
}
