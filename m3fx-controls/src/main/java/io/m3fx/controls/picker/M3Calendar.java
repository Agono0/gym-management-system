package io.m3fx.controls.picker;

import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.M3IconButton;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.Locale;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * M3 calendar month grid: weekday header, day cells with a primary-circle selected state, and
 * month navigation.
 *
 * <p>See <a href="https://m3.material.io/components/date-pickers/overview">M3 date pickers</a>.
 * Surface-container-high dialog styling comes from CSS; the week starts on the locale's first day.
 * Selecting a day sets {@link #selectedDateProperty()} and fires an action event.
 */
public class M3Calendar extends Control {

    private final ObjectProperty<YearMonth> yearMonth =
            new SimpleObjectProperty<>(this, "yearMonth", YearMonth.now());
    private final ObjectProperty<LocalDate> selectedDate =
            new SimpleObjectProperty<>(this, "selectedDate", LocalDate.now());
    private final ObjectProperty<DayOfWeek> firstDayOfWeek =
            new SimpleObjectProperty<>(
                    this, "firstDayOfWeek",
                    WeekFields.of(Locale.getDefault()).getFirstDayOfWeek());

    /** Creates a calendar on the current month with today selected. */
    public M3Calendar() {
        getStyleClass().add("m3-calendar");
        setFocusTraversable(true);
        setMinSize(320, 360);
        setPrefSize(320, 400);
    }

    /** Returns the visible month property. */
    public ObjectProperty<YearMonth> yearMonthProperty() {
        return yearMonth;
    }

    /** Returns the visible month. */
    public YearMonth getYearMonth() {
        return yearMonth.get();
    }

    /**
     * Sets the visible month.
     *
     * @param yearMonth the month to show
     */
    public void setYearMonth(YearMonth yearMonth) {
        this.yearMonth.set(yearMonth == null ? YearMonth.now() : yearMonth);
    }

    /** Returns the selected date property. */
    public ObjectProperty<LocalDate> selectedDateProperty() {
        return selectedDate;
    }

    /** Returns the selected date. */
    public LocalDate getSelectedDate() {
        return selectedDate.get();
    }

    /**
     * Sets the selected date (and navigates to its month).
     *
     * @param selectedDate the date, may be {@code null} to clear
     */
    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate.set(selectedDate);
        if (selectedDate != null) {
            setYearMonth(YearMonth.from(selectedDate));
        }
    }

    /** Returns the first-day-of-week property (locale default). */
    public ObjectProperty<DayOfWeek> firstDayOfWeekProperty() {
        return firstDayOfWeek;
    }

    /** Shows the previous month. */
    public void previousMonth() {
        setYearMonth(getYearMonth().minusMonths(1));
    }

    /** Shows the next month. */
    public void nextMonth() {
        setYearMonth(getYearMonth().plusMonths(1));
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new CalendarSkin(this);
    }

    /** Skin: header with month label + nav arrows, weekday row, 6x7 day grid. */
    static final class CalendarSkin extends SkinBase<M3Calendar> {
        private final Label monthLabel = new Label();
        private final HBox weekdays = new HBox();
        private final GridPane days = new GridPane();
        private final ToggleGroup group = new ToggleGroup();

        CalendarSkin(M3Calendar control) {
            super(control);
            monthLabel.getStyleClass().add("m3-calendar-month");
            M3IconButton prev = new M3IconButton(M3Icon.symbol("back"), null);
            prev.getStyleClass().add("m3-calendar-nav");
            prev.setAccessibleText("Previous month");
            prev.setOnAction(e -> control.previousMonth());
            M3Icon nextIcon = M3Icon.symbol("back");
            nextIcon.setRotate(180);
            M3IconButton next = new M3IconButton(nextIcon, null);
            next.getStyleClass().add("m3-calendar-nav");
            next.setAccessibleText("Next month");
            next.setOnAction(e -> control.nextMonth());
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox header = new HBox(monthLabel, spacer, prev, next);
            header.setAlignment(Pos.CENTER_LEFT);
            header.getStyleClass().add("m3-calendar-header");
            weekdays.getStyleClass().add("m3-calendar-weekdays");
            days.getStyleClass().add("m3-calendar-days");
            VBox root = new VBox(8, header, weekdays, days);
            root.getStyleClass().add("m3-calendar-root");
            getChildren().add(root);
            control.yearMonthProperty().addListener((obs, oldV, newV) -> rebuild());
            control.selectedDateProperty().addListener((obs, oldV, newV) -> rebuild());
            control.firstDayOfWeekProperty().addListener((obs, oldV, newV) -> rebuild());
            rebuild();
        }

        private void rebuild() {
            M3Calendar control = getSkinnable();
            YearMonth month = control.getYearMonth();
            monthLabel.setText(
                    month.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
                            + " "
                            + month.getYear());
            weekdays.getChildren().clear();
            DayOfWeek first = control.firstDayOfWeekProperty().get();
            for (int i = 0; i < 7; i++) {
                DayOfWeek day = first.plus(i);
                Label label =
                        new Label(day.getDisplayName(TextStyle.NARROW, Locale.getDefault()));
                label.getStyleClass().add("m3-calendar-weekday");
                label.setMinSize(40, 32);
                label.setAlignment(Pos.CENTER);
                weekdays.getChildren().add(label);
            }
            weekdays.setAlignment(Pos.CENTER);
            days.getChildren().clear();
            LocalDate firstOfMonth = month.atDay(1);
            int lead = (firstOfMonth.getDayOfWeek().getValue() - first.getValue() + 7) % 7;
            LocalDate cell = firstOfMonth.minusDays(lead);
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 7; col++) {
                    final LocalDate date = cell;
                    ToggleButton dayButton = new ToggleButton(String.valueOf(date.getDayOfMonth()));
                    dayButton.getStyleClass().add("m3-calendar-day");
                    dayButton.setToggleGroup(group);
                    dayButton.setMinSize(40, 40);
                    dayButton.setMaxSize(40, 40);
                    boolean inMonth = date.getMonth() == month.getMonth();
                    dayButton.setOpacity(inMonth ? 1.0 : 0.38);
                    dayButton.setSelected(date.equals(control.getSelectedDate()));
                    dayButton.setOnAction(e -> {
                        control.setSelectedDate(date);
                        control.fireEvent(new javafx.event.ActionEvent(control, null));
                    });
                    days.add(dayButton, col, row);
                    cell = cell.plusDays(1);
                }
            }
            days.setAlignment(Pos.CENTER);
            // Soft cross-fade on month change so the grid never pops.
            javafx.animation.FadeTransition fade =
                    new javafx.animation.FadeTransition(
                            javafx.util.Duration.millis(140), days);
            fade.setFromValue(0.35);
            fade.setToValue(1.0);
            fade.play();
        }
    }
}
