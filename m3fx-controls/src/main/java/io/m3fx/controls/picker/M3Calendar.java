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
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
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
 * Selecting a day sets {@link #selectedDateProperty()} and fires an action event. Today's cell
 * carries the {@code :today} pseudo-class, dates outside {@link #minDateProperty()}/
 * {@link #maxDateProperty()} are disabled, and the grid is keyboard navigable (arrows move the
 * selection by day/week, {@code Home}/{@code End} jump within the month, {@code PgUp}/{@code PgDn}
 * change month, {@code Enter} fires the action).
 */
public class M3Calendar extends Control {

    private static final PseudoClass TODAY_PC = PseudoClass.getPseudoClass("today");

    private final ObjectProperty<YearMonth> yearMonth =
            new SimpleObjectProperty<>(this, "yearMonth", YearMonth.now());
    private final ObjectProperty<LocalDate> selectedDate =
            new SimpleObjectProperty<>(this, "selectedDate", LocalDate.now());
    private final ObjectProperty<DayOfWeek> firstDayOfWeek =
            new SimpleObjectProperty<>(
                    this, "firstDayOfWeek",
                    WeekFields.of(Locale.getDefault()).getFirstDayOfWeek());
    private final ObjectProperty<LocalDate> minDate =
            new SimpleObjectProperty<>(this, "minDate");
    private final ObjectProperty<LocalDate> maxDate =
            new SimpleObjectProperty<>(this, "maxDate");

    /** Creates a calendar on the current month with today selected. */
    public M3Calendar() {
        getStyleClass().add("m3-calendar");
        setFocusTraversable(true);
        setMinSize(320, 360);
        setPrefSize(320, 400);
        setOnKeyPressed(this::handleKey);
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

    /** Returns the earliest selectable date, or {@code null} when unrestricted. */
    public LocalDate getMinDate() {
        return minDate.get();
    }

    /**
     * Sets the earliest selectable date; earlier day cells are disabled.
     *
     * @param minDate the minimum date, may be {@code null} to clear
     */
    public void setMinDate(LocalDate minDate) {
        this.minDate.set(minDate);
    }

    /** Returns the minimum date property. */
    public ObjectProperty<LocalDate> minDateProperty() {
        return minDate;
    }

    /** Returns the latest selectable date, or {@code null} when unrestricted. */
    public LocalDate getMaxDate() {
        return maxDate.get();
    }

    /**
     * Sets the latest selectable date; later day cells are disabled.
     *
     * @param maxDate the maximum date, may be {@code null} to clear
     */
    public void setMaxDate(LocalDate maxDate) {
        this.maxDate.set(maxDate);
    }

    /** Returns the maximum date property. */
    public ObjectProperty<LocalDate> maxDateProperty() {
        return maxDate;
    }

    /** Returns whether the date is outside the configured min/max range. */
    public boolean isDisabledDate(LocalDate date) {
        LocalDate min = minDate.get();
        LocalDate max = maxDate.get();
        return (min != null && date.isBefore(min)) || (max != null && date.isAfter(max));
    }

    /** Shows the previous month. */
    public void previousMonth() {
        setYearMonth(getYearMonth().minusMonths(1));
    }

    /** Shows the next month. */
    public void nextMonth() {
        setYearMonth(getYearMonth().plusMonths(1));
    }

    private void handleKey(javafx.scene.input.KeyEvent event) {
        KeyCode code = event.getCode();
        LocalDate current = selectedDate.get();
        if (current == null) {
            current = clampToRange(LocalDate.now());
        }
        LocalDate next = null;
        switch (code) {
            case LEFT -> next = current.minusDays(1);
            case RIGHT -> next = current.plusDays(1);
            case UP -> next = current.minusDays(7);
            case DOWN -> next = current.plusDays(7);
            case HOME -> next = current.withDayOfMonth(1);
            case END -> next = current.withDayOfMonth(YearMonth.from(current).lengthOfMonth());
            case PAGE_UP -> {
                // Same range guard as the header's previous-month button, so keyboard month
                // navigation can never land on a month that is entirely disabled.
                YearMonth target = getYearMonth().minusMonths(1);
                if (getMinDate() == null || YearMonth.from(getMinDate()).compareTo(target) <= 0) {
                    setYearMonth(target);
                }
                event.consume();
            }
            case PAGE_DOWN -> {
                YearMonth target = getYearMonth().plusMonths(1);
                if (getMaxDate() == null || YearMonth.from(getMaxDate()).compareTo(target) >= 0) {
                    setYearMonth(target);
                }
                event.consume();
            }
            case ENTER, SPACE -> {
                fireEvent(new javafx.event.ActionEvent(this, null));
                event.consume();
            }
            default -> {
                // Not a navigation key.
            }
        }
        if (next != null && !isDisabledDate(next)) {
            setSelectedDate(next);
            event.consume();
        }
    }

    private LocalDate clampToRange(LocalDate date) {
        if (isDisabledDate(date)) {
            if (minDate.get() != null && date.isBefore(minDate.get())) {
                return minDate.get();
            }
            if (maxDate.get() != null && date.isAfter(maxDate.get())) {
                return maxDate.get();
            }
        }
        return date;
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
        private final M3IconButton prev;
        private final M3IconButton next;
        private javafx.animation.FadeTransition fade;

        private final ChangeListener<Object> rebuildListener = (obs, oldV, newV) -> rebuild();

        CalendarSkin(M3Calendar control) {
            super(control);
            monthLabel.getStyleClass().add("m3-calendar-month");
            prev = new M3IconButton(M3Icon.symbol("back"), null);
            prev.getStyleClass().add("m3-calendar-nav");
            prev.setAccessibleText("Previous month");
            prev.setOnAction(e -> control.previousMonth());
            M3Icon nextIcon = M3Icon.symbol("back");
            nextIcon.setRotate(180);
            next = new M3IconButton(nextIcon, null);
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
            control.yearMonthProperty().addListener(rebuildListener);
            control.selectedDateProperty().addListener(rebuildListener);
            control.firstDayOfWeekProperty().addListener(rebuildListener);
            control.minDateProperty().addListener(rebuildListener);
            control.maxDateProperty().addListener(rebuildListener);
            rebuild();
        }

        private void rebuild() {
            M3Calendar control = getSkinnable();
            if (control == null) {
                return;
            }
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
                    boolean disabled = control.isDisabledDate(date);
                    dayButton.setDisable(disabled);
                    dayButton.pseudoClassStateChanged(TODAY_PC, date.equals(LocalDate.now()));
                    boolean selectedToday = date.equals(control.getSelectedDate())
                            && date.equals(LocalDate.now());
                    if (selectedToday) {
                        // The stylesheet's :today rule ties with :selected and loads later, so
                        // today+selected would render primary-on-primary (invisible). Keep the
                        // on-primary text until the combined :selected:today rule lands in
                        // m3fx.css (see CSS REQUESTS), which should replace this workaround.
                        dayButton.setStyle("-fx-text-fill: -md-sys-color-on-primary;");
                    } else {
                        dayButton.setStyle("");
                    }
                    dayButton.setAccessibleText(date.getDayOfMonth() + " "
                            + date.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            + (date.equals(LocalDate.now()) ? ", today" : "")
                            + (disabled ? ", disabled" : ""));
                    dayButton.setOnAction(e -> {
                        control.setSelectedDate(date);
                        control.fireEvent(new javafx.event.ActionEvent(control, null));
                    });
                    days.add(dayButton, col, row);
                    cell = cell.plusDays(1);
                }
            }
            days.setAlignment(Pos.CENTER);
            // Disable month navigation when the adjacent month is entirely out of range.
            prev.setDisable(control.getMinDate() != null
                    && YearMonth.from(control.getMinDate()).compareTo(month) >= 0);
            next.setDisable(control.getMaxDate() != null
                    && YearMonth.from(control.getMaxDate()).compareTo(month) <= 0);
            // Soft cross-fade on month change so the grid never pops; restart, never stack.
            if (fade != null) {
                fade.stop();
            }
            fade = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(140), days);
            fade.setFromValue(0.35);
            fade.setToValue(1.0);
            fade.play();
        }

        @Override
        public void dispose() {
            M3Calendar control = getSkinnable();
            if (control != null) {
                control.yearMonthProperty().removeListener(rebuildListener);
                control.selectedDateProperty().removeListener(rebuildListener);
                control.firstDayOfWeekProperty().removeListener(rebuildListener);
                control.minDateProperty().removeListener(rebuildListener);
                control.maxDateProperty().removeListener(rebuildListener);
            }
            if (fade != null) {
                fade.stop();
                fade = null;
            }
            super.dispose();
        }
    }
}
