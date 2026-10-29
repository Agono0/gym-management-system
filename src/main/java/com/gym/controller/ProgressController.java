package com.gym.controller;

import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.chart.M3ActivityCalendar;
import io.m3fx.controls.chart.M3DonutChart;
import io.m3fx.controls.chart.M3ScatterPlot;
import io.m3fx.controls.chart.M3SplineChart;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.data.M3Timeline;
import io.m3fx.controls.effect.M3ParticleCanvas;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.segmented.M3SegmentedButton;
import io.m3fx.controls.sheet.M3BottomSheet;
import io.m3fx.controls.slider.M3Slider;
import io.m3fx.controls.textfield.M3TextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

/**
 * Controller skeleton for Biomechanical Progress & Analytics (progress.fxml).
 * Conforms to docs/ui/members/MEMBER_PROGRESS_UI.md Section 7 contract.
 * Pure UI stub: strictly no backend service, DAO, or database logic.
 */
public class ProgressController {

    @FXML private StackPane rootPane;
    @FXML private BorderPane mainLayout;
    @FXML private M3TopAppBar topAppBar;
    @FXML private M3IconButton btnBack;
    @FXML private M3SegmentedButton segTimeHorizon;
    @FXML private M3Button btnLogEntry;

    @FXML private M3StatCard statCurrentWeight;
    @FXML private M3StatCard statBodyFat;
    @FXML private M3StatCard statLeanMass;
    @FXML private M3StatCard statVbtMax;

    @FXML private M3SplineChart splineBiometrics;
    @FXML private M3ActivityCalendar activityHeatmap;
    @FXML private M3DonutChart donutComposition;
    @FXML private Label lblRegressionStats;
    @FXML private M3ScatterPlot scatterVbt;
    @FXML private M3Timeline timelineAnthropometrics;

    @FXML private M3ParticleCanvas celebrationCanvas;
    @FXML private M3BottomSheet sheetLogEntry;
    @FXML private M3TextField txtEntryDate;
    @FXML private M3TextField txtWeight;
    @FXML private M3Slider sliderWeight;
    @FXML private M3TextField txtBodyFat;
    @FXML private M3TextField txtTargetWeight;
    @FXML private M3TextField txtMeasurements;

    public ProgressController() {
        // Public no-arg constructor for FXMLLoader
    }

    @FXML
    public void initialize() {
        // UI initialization stub
    }

    @FXML
    public void onNavigateBack() {
        // UI event handler stub
    }

    @FXML
    public void onHorizonChanged() {
        // UI event handler stub
    }

    @FXML
    public void onOpenLogDialog() {
        if (sheetLogEntry != null) {
            sheetLogEntry.setVisible(true);
        }
    }

    @FXML
    public void onCloseLogSheet() {
        if (sheetLogEntry != null) {
            sheetLogEntry.setVisible(false);
        }
    }

    @FXML
    public void onSaveRecord() {
        Double weight = parseDouble(txtWeight, 30.0, 300.0);
        Double bodyFat = txtBodyFat == null || txtBodyFat.getText().isBlank()
                ? Double.valueOf(0.0) : parseDouble(txtBodyFat, 0.0, 70.0);
        boolean valid = weight != null && bodyFat != null;
        if (txtWeight != null) {
            txtWeight.setError(weight == null);
        }
        if (txtBodyFat != null) {
            txtBodyFat.setError(bodyFat == null);
        }
        if (!valid) {
            return;
        }
        // Reflect the entry in the dashboard telemetry and close the sheet
        if (sliderWeight != null) {
            sliderWeight.setValue(weight);
        }
        if (statCurrentWeight != null) {
            statCurrentWeight.setValue(String.format("%.1f kg", weight));
            statCurrentWeight.setDeltaPositive(weight <= 80.0);
        }
        onCloseLogSheet();
    }

    private Double parseDouble(M3TextField field, double min, double max) {
        if (field == null) {
            return null;
        }
        try {
            double value = Double.parseDouble(field.getText().trim());
            return (value >= min && value <= max) ? Double.valueOf(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
