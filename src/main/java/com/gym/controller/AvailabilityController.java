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
import io.m3fx.controls.selection.M3Switch;
import io.m3fx.controls.sheet.M3BottomSheet;
import io.m3fx.controls.slider.M3Slider;
import io.m3fx.controls.textfield.M3TextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Controller skeleton for Trainer Availability & Slot Capacity Matrix screen.
 * Strictly UI contract stubs matching Section 7 without backend/service dependencies.
 */
public class AvailabilityController {

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

    public AvailabilityController() {
        // Public no-arg constructor
    }

    @FXML
    public void initialize() {
        // Keep the per-slot cap caption in sync while the slider moves
        if (sliderSlotCap != null && lblSlotCapValue != null) {
            lblSlotCapValue.setText((int) sliderSlotCap.getValue() + " Athletes");
            sliderSlotCap.valueProperty().addListener((obs, o, v) ->
                    lblSlotCapValue.setText(v.intValue() + " Athletes"));
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

    @FXML
    public void onApplyDailyCap() {
        // UI action stub
    }

    @FXML
    public void onApplyDailyCap(ActionEvent event) {
        onApplyDailyCap();
    }

    @FXML
    public void onBatchPublish() {
        if (bottomSheetBatchPublish != null) {
            bottomSheetBatchPublish.setVisible(true);
        }
    }

    @FXML
    public void onBatchPublish(ActionEvent event) {
        onBatchPublish();
    }

    @FXML
    public void onSaveSlotCap() {
        // UI action stub
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

    @FXML
    public void onConfirmBatchPublish() {
        if (validateBatchForm() && bottomSheetBatchPublish != null) {
            setFieldError(txtBatchStartDate, false);
            setFieldError(txtBatchEndDate, false);
            setFieldError(txtBatchStartTime, false);
            setFieldError(txtBatchEndTime, false);
            bottomSheetBatchPublish.setVisible(false);
        }
    }

    @FXML
    public void onConfirmBatchPublish(ActionEvent event) {
        onConfirmBatchPublish();
    }

    @FXML
    public void onPrevWeek() {
        // UI navigation stub
    }

    @FXML
    public void onPrevWeek(ActionEvent event) {
        onPrevWeek();
    }

    @FXML
    public void onNextWeek() {
        // UI navigation stub
    }

    @FXML
    public void onNextWeek(ActionEvent event) {
        onNextWeek();
    }

    @FXML
    public void onToday() {
        // UI navigation stub
    }

    @FXML
    public void onToday(ActionEvent event) {
        onToday();
    }
}
