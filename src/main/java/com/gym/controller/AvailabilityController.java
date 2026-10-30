package com.gym.controller;

import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.button.M3SplitButton;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chart.M3ActivityCalendar;
import io.m3fx.controls.chart.M3Heatmap;
import io.m3fx.controls.data.M3Stepper;
import io.m3fx.controls.effect.M3GlassPane;
import io.m3fx.controls.menu.M3Menu;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.selection.M3Switch;
import io.m3fx.controls.sheet.M3BottomSheet;
import io.m3fx.controls.slider.M3Slider;
import io.m3fx.controls.textfield.M3TextField;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

/**
 * Controller for the Trainer Availability & Slot Capacity Matrix screen.
 * UI-contract only (no backend/service dependencies): owns the week navigation, the demo
 * density heatmap and activity calendar, the daily/slot cap controls and the batch publisher.
 */
public class AvailabilityController {

    private static final DateTimeFormatter RANGE_START_FMT = DateTimeFormatter.ofPattern("MMM dd");
    private static final DateTimeFormatter RANGE_END_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final List<String> DAY_LABELS = List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");

    @FXML private M3TopAppBar topAppBar;
    @FXML private M3GlassPane controlStrip;
    @FXML private M3SplitButton btnCoachSelector;
    @FXML private M3IconButton btnPrevWeek;
    @FXML private Label lblDateRange;
    @FXML private M3IconButton btnNextWeek;
    @FXML private M3Button btnToday;
    @FXML private M3Stepper stepperDailyCap;
    @FXML private M3Button btnApplyDailyCap;
    @FXML private M3Button btnBatchPublish;
    @FXML private M3Card cardDensityBanner;
    @FXML private Label lblDensityStats;
    @FXML private M3Heatmap heatmapDensity;
    @FXML private M3ActivityCalendar calendarGrid;
    @FXML private M3GlassPane bottomCommandBar;
    @FXML private Label lblSelectedSlotTitle;
    @FXML private Label lblSlotOccupancy;
    @FXML private M3Switch switchSlotOpen;
    @FXML private M3Slider sliderSlotCap;
    @FXML private Label lblSlotCapValue;
    @FXML private M3Button btnSaveSlotCap;
    @FXML private M3BottomSheet bottomSheetBatchPublish;
    @FXML private M3IconButton btnCloseBottomSheet;
    @FXML private M3TextField txtBatchStartDate;
    @FXML private M3TextField txtBatchEndDate;
    @FXML private M3TextField txtBatchStartTime;
    @FXML private M3TextField txtBatchEndTime;
    @FXML private M3Stepper stepperBatchCap;
    @FXML private Label lblBatchValidation;
    @FXML private M3Button btnCancelBatch;
    @FXML private M3Button btnConfirmBatchPublish;

    /** Hosts M3 snackbars and dialogs for this screen (attached to the scene root at runtime). */
    private final M3Overlay feedback = new M3Overlay();

    /** Monday of the week shown in the header; refreshed by the navigation buttons. */
    private LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

    /** Runtime replacement for the invisible wizard {@code M3Stepper}: the daily cap picker. */
    private M3Slider dailyCapSlider;

    /** Runtime replacement for the invisible wizard {@code M3Stepper}: the batch cap picker. */
    private M3Slider batchCapSlider;

    public AvailabilityController() {
        // Public no-arg constructor required by FXMLLoader
    }

    @FXML
    public void initialize() {
        installFeedbackLayer(topAppBar);
        installDailyCapSlider();
        installBatchCapSlider();
        wireCoachSelector();
        wireSlotControls();
        populateActivityCalendar();
        refreshWeekView();
    }

    /**
     * The FXML pins {@code stepperDailyCap} as an {@link M3Stepper}, but a wizard stepper cannot
     * express a numeric daily cap and renders empty without step titles. Swap in the same slider
     * pattern the per-slot cap already uses (bottom command bar) and retire the stepper.
     */
    private void installDailyCapSlider() {
        if (stepperDailyCap == null) {
            return;
        }
        dailyCapSlider = M3Slider.discrete(1, 10, 4, 1);
        dailyCapSlider.setShowTickLabels(true);
        dailyCapSlider.setPrefWidth(200);
        if (stepperDailyCap.getParent() instanceof Pane parent) {
            int index = parent.getChildren().indexOf(stepperDailyCap);
            parent.getChildren().set(index, dailyCapSlider);
        }
        stepperDailyCap.setVisible(false);
        stepperDailyCap.setManaged(false);
    }

    /** Same treatment for the batch publisher's default slot capacity. */
    private void installBatchCapSlider() {
        if (stepperBatchCap == null) {
            return;
        }
        batchCapSlider = M3Slider.discrete(1, 10, 4, 1);
        batchCapSlider.setShowTickLabels(true);
        batchCapSlider.setPrefWidth(200);
        if (stepperBatchCap.getParent() instanceof Pane parent) {
            int index = parent.getChildren().indexOf(stepperBatchCap);
            parent.getChildren().set(index, batchCapSlider);
        }
        stepperBatchCap.setVisible(false);
        stepperBatchCap.setManaged(false);
    }

    /** Turns the coach split button into a working selector over the demo coaching staff. */
    private void wireCoachSelector() {
        if (btnCoachSelector == null) {
            return;
        }
        M3Menu coachMenu = new M3Menu()
                .item("Elena Rostova (#101)", () -> selectCoach("Elena Rostova (#101)"))
                .item("Marcus Vance (#102)", () -> selectCoach("Marcus Vance (#102)"))
                .item("Kim Park (#103)", () -> selectCoach("Kim Park (#103)"));
        btnCoachSelector.setMenu(coachMenu);
    }

    /** Switches the visible week to the chosen demo coach and confirms with a snackbar. */
    private void selectCoach(String coach) {
        btnCoachSelector.setText(coach);
        feedback.showSnackbar("Showing availability for " + coach + " (demo)");
        refreshWeekView();
    }

    /** Wires the per-slot cap caption, the open/closed switch and the save action. */
    private void wireSlotControls() {
        if (sliderSlotCap != null && lblSlotCapValue != null) {
            lblSlotCapValue.setText((int) sliderSlotCap.getValue() + " Athletes");
            sliderSlotCap.valueProperty().addListener((obs, o, v) ->
                    lblSlotCapValue.setText(v.intValue() + " Athletes"));
        }
        if (switchSlotOpen != null) {
            switchSlotOpen.selectedProperty().addListener((obs, wasOpen, isOpen) ->
                    feedback.showSnackbar(isOpen
                            ? "Slot " + currentSlotLabel() + " opened for booking (demo)"
                            : "Slot " + currentSlotLabel() + " closed (demo)"));
        }
    }

    /** The demo slot identity shown in the bottom command bar. */
    private String currentSlotLabel() {
        return lblSelectedSlotTitle != null && lblSelectedSlotTitle.getText() != null
                ? lblSelectedSlotTitle.getText().replace("Selected Slot: ", "")
                : "#SLOT-882";
    }

    /** Fills the annual activity calendar with a deterministic demo booking pattern. */
    private void populateActivityCalendar() {
        if (calendarGrid == null) {
            return;
        }
        LocalDate day = LocalDate.of(calendarGrid.getYear(), 1, 1);
        Random random = new Random(42);
        while (day.getYear() == calendarGrid.getYear()) {
            DayOfWeek dow = day.getDayOfWeek();
            boolean booked = dow != DayOfWeek.SUNDAY && random.nextDouble() > 0.25;
            if (booked) {
                calendarGrid.setCount(day, 2 + random.nextInt(8));
            }
            day = day.plusDays(1);
        }
    }

    /** Re-renders the week header, the density heatmap and the banner stats for weekStart. */
    private void refreshWeekView() {
        if (lblDateRange != null) {
            int weekNumber = weekStart.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
            lblDateRange.setText(RANGE_START_FMT.format(weekStart) + " - "
                    + RANGE_END_FMT.format(weekStart.plusDays(6)) + " (Week " + weekNumber + ")");
        }
        if (heatmapDensity != null) {
            heatmapDensity.setDimensions(7, 16);
            heatmapDensity.setRowLabels(DAY_LABELS);
            Random random = new Random(weekStart.toEpochDay());
            int confirmed = 0;
            int peakRow = 0;
            int peakCol = 0;
            double peakValue = -1;
            for (int day = 0; day < 7; day++) {
                for (int slot = 0; slot < 16; slot++) {
                    int hour = slot + 6;
                    double intensity;
                    if ((hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 20)) {
                        intensity = 0.70 + random.nextDouble() * 0.25;
                    } else if (hour >= 12 && hour <= 14) {
                        intensity = 0.40 + random.nextDouble() * 0.20;
                    } else if (day >= 5) {
                        intensity = 0.20 + random.nextDouble() * 0.15;
                    } else {
                        intensity = 0.10 + random.nextDouble() * 0.15;
                    }
                    heatmapDensity.setValue(day, slot, intensity);
                    confirmed += (int) Math.round(intensity * 5);
                    if (intensity > peakValue) {
                        peakValue = intensity;
                        peakRow = day;
                        peakCol = slot;
                    }
                }
            }
            if (lblDensityStats != null) {
                int capacity = 7 * 16 * 5;
                double saturation = 100.0 * confirmed / capacity;
                String peak = DAY_LABELS.get(peakRow) + " " + String.format("%02d:00", peakCol + 6);
                lblDensityStats.setText(String.format(
                        "Confirmed Sessions: %d / %d Capacity (%.1f%% Saturation) | Peak: %s",
                        confirmed, capacity, saturation, peak));
            }
        }
    }

    /** Validates the batch publisher form (Rules 8 & 9) and reports problems inline. */
    private boolean validateBatchForm() {
        if (lblBatchValidation == null) {
            return false;
        }
        LocalDate start = parseIsoDate(txtBatchStartDate);
        LocalDate end = parseIsoDate(txtBatchEndDate);
        LocalTime startTime = parseTime(txtBatchStartTime);
        LocalTime endTime = parseTime(txtBatchEndTime);

        lblBatchValidation.setText("");
        boolean valid = true;
        if (start == null) {
            setFieldError(txtBatchStartDate, true);
            valid = false;
        }
        if (end == null) {
            setFieldError(txtBatchEndDate, true);
            valid = false;
        }
        if (start != null && end != null && end.isBefore(start)) {
            lblBatchValidation.setText("End date must be on or after the start date (Rule 9).");
            valid = false;
        }
        if (startTime == null) {
            setFieldError(txtBatchStartTime, true);
            valid = false;
        }
        if (endTime == null) {
            setFieldError(txtBatchEndTime, true);
            valid = false;
        }
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            lblBatchValidation.setText("End time must be after start time (Rule 8: startTime < endTime).");
            valid = false;
        }
        if (!valid && lblBatchValidation.getText().isEmpty()) {
            lblBatchValidation.setText("Enter dates as yyyy-MM-dd and times as HH:mm.");
        }
        return valid;
    }

    private LocalDate parseIsoDate(M3TextField field) {
        if (field == null) {
            return null;
        }
        try {
            return LocalDate.parse(field.getText().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalTime parseTime(M3TextField field) {
        if (field == null) {
            return null;
        }
        try {
            return LocalTime.parse(field.getText().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private void setFieldError(M3TextField field, boolean error) {
        if (field != null) {
            field.setError(error);
        }
    }

    public void setCalendar(int trainerId, String date) {
        // UI presentation stub
    }

    public void setSlots(int trainerId, String date, String startTime) {
        // UI action stub
    }

    public void setSlotCap(int availabilityId, int maxCustomers) {
        // UI action stub
    }

    /** Applies the chosen daily cap to every day of the visible week. */
    @FXML
    public void onApplyDailyCap() {
        int cap = dailyCapSlider != null ? (int) dailyCapSlider.getValue() : 4;
        feedback.showSnackbar("Daily cap of " + cap + " athletes applied to " + weekButtonText() + " (demo)");
    }

    @FXML
    public void onApplyDailyCap(ActionEvent event) {
        onApplyDailyCap();
    }

    /** Opens the batch publisher sheet with prefilled demo dates for the visible week. */
    @FXML
    public void onBatchPublish() {
        if (bottomSheetBatchPublish != null) {
            if (txtBatchStartDate != null) {
                txtBatchStartDate.setText(weekStart.toString());
            }
            if (txtBatchEndDate != null) {
                txtBatchEndDate.setText(weekStart.plusDays(6).toString());
            }
            bottomSheetBatchPublish.setVisible(true);
        }
    }

    @FXML
    public void onBatchPublish(ActionEvent event) {
        onBatchPublish();
    }

    /** Saves the per-slot cap and confirms with a snackbar. */
    @FXML
    public void onSaveSlotCap() {
        int cap = sliderSlotCap != null ? (int) sliderSlotCap.getValue() : 4;
        feedback.showSnackbar("Slot cap of " + cap + " athletes saved for " + currentSlotLabel() + " (demo)");
    }

    @FXML
    public void onSaveSlotCap(ActionEvent event) {
        onSaveSlotCap();
    }

    @FXML
    public void onCloseBottomSheet() {
        if (bottomSheetBatchPublish != null) {
            bottomSheetBatchPublish.setVisible(false);
        }
    }

    @FXML
    public void onCloseBottomSheet(ActionEvent event) {
        onCloseBottomSheet();
    }

    @FXML
    public void onCancelBatch() {
        onCloseBottomSheet();
    }

    @FXML
    public void onCancelBatch(ActionEvent event) {
        onCancelBatch();
    }

    /** Publishes the batch after validation and answers with the computed slot count. */
    @FXML
    public void onConfirmBatchPublish() {
        if (!validateBatchForm()) {
            return;
        }
        LocalDate start = parseIsoDate(txtBatchStartDate);
        LocalDate end = parseIsoDate(txtBatchEndDate);
        LocalTime startTime = parseTime(txtBatchStartTime);
        LocalTime endTime = parseTime(txtBatchEndTime);
        long days = start != null && end != null ? ChronoUnit.DAYS.between(start, end) + 1 : 1;
        int hours = startTime != null && endTime != null ? endTime.getHour() - startTime.getHour() : 1;
        int cap = batchCapSlider != null ? (int) batchCapSlider.getValue() : 4;
        long slots = Math.max(1, days * Math.max(1, hours));
        setFieldError(txtBatchStartDate, false);
        setFieldError(txtBatchEndDate, false);
        setFieldError(txtBatchStartTime, false);
        setFieldError(txtBatchEndTime, false);
        if (bottomSheetBatchPublish != null) {
            bottomSheetBatchPublish.setVisible(false);
        }
        feedback.showSnackbar(slots + " slots published for " + days + " day(s), " + cap
                + " athletes each (demo)");
    }

    @FXML
    public void onConfirmBatchPublish(ActionEvent event) {
        onConfirmBatchPublish();
    }

    @FXML
    public void onPrevWeek() {
        weekStart = weekStart.minusWeeks(1);
        refreshWeekView();
    }

    @FXML
    public void onPrevWeek(ActionEvent event) {
        onPrevWeek();
    }

    @FXML
    public void onNextWeek() {
        weekStart = weekStart.plusWeeks(1);
        refreshWeekView();
    }

    @FXML
    public void onNextWeek(ActionEvent event) {
        onNextWeek();
    }

    @FXML
    public void onToday() {
        weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        refreshWeekView();
    }

    @FXML
    public void onToday(ActionEvent event) {
        onToday();
    }

    /** Short week label used by the daily-cap snackbar. */
    private String weekButtonText() {
        return "the week of " + RANGE_START_FMT.format(weekStart);
    }

    /**
     * Hosts the shared feedback overlay on the scene root while this screen is visible. Attach
     * and detach are deferred with {@link Platform#runLater(Runnable) Platform.runLater} so the
     * children list of the scene root (or of the pane hosting this screen) is never mutated
     * re-entrantly while the shell swaps screen content.
     */
    private void installFeedbackLayer(Node anchor) {
        anchor.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null && scene.getRoot() instanceof Pane root
                    && !root.getChildren().contains(feedback)) {
                root.getChildren().add(feedback);
            }
            if (oldScene != null && oldScene.getRoot() instanceof Pane oldRoot) {
                // Deferred so a screen swap that is mid-mutation of the pane hosting the screen
                // never re-enters a JavaFX children list from inside this listener.
                Platform.runLater(() -> oldRoot.getChildren().remove(feedback));
            }
        });
    }
}
