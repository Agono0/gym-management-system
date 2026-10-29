package com.gym.controller;

import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chart.M3GaugeChart;
import io.m3fx.controls.chart.M3Heatmap;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.effect.M3Scanline;
import io.m3fx.controls.textfield.M3TextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Controller skeleton for Fast-Lane Attendance & Turnstile Gateway screen.
 * Conforms strictly to docs/ui/facility/ATTENDANCE_CHECKIN_UI.md Section 7 contract.
 * Pure UI stub without backend services or SQL logic.
 */
public class AttendanceController {

    @FXML private Label lblCurrentDate;
    @FXML private M3StatCard cardInsideCount;
    @FXML private M3GaugeChart occupancyGauge;
    @FXML private M3Heatmap peakHoursHeatmap;
    @FXML private M3Card memberDossierCard;
    @FXML private Label lblScanResult;
    @FXML private Label lblScanTimestamp;
    @FXML private Label lblScannedMemberName;
    @FXML private Label lblScannedPlanInfo;
    @FXML private Label lblAssignedTrainer;
    @FXML private M3TextField txtMemberLookup;
    @FXML private M3DataTable<String> attendanceDataTable;

    @FXML private M3Button btnDailyReportAction;
    @FXML private M3Scanline turnstileScanline;
    @FXML private M3Button btnManualCheckIn;
    @FXML private M3Button btnManualCheckOut;
    @FXML private TextField txtTableSearch;

    public AttendanceController() {
        // Public no-arg constructor required by FXMLLoader
    }

    @FXML
    public void initialize() {
        // Show today's date instead of the stale hard-coded value from the FXML
        if (lblCurrentDate != null) {
            lblCurrentDate.setText(LocalDate.now().toString());
        }
        populateAttendanceStream();
    }

    /** Fills the check-in stream table with today's demo rows so the screen is not blank. */
    private void populateAttendanceStream() {
        if (attendanceDataTable == null || !attendanceDataTable.getColumns().isEmpty()) {
            return;
        }
        attendanceDataTable.getColumns().addAll(
                new M3DataTable.Column<>("Time", r -> r.split("\\|")[0], 90),
                new M3DataTable.Column<>("Member", r -> r.split("\\|")[1], 180),
                new M3DataTable.Column<>("Plan", r -> r.split("\\|")[2], 130),
                new M3DataTable.Column<>("Status", r -> r.split("\\|")[3], 110));
        attendanceDataTable.getItems().addAll(
                "07:02|Sarah Connor (#1042)|Yearly VIP|Check-In",
                "07:05|Marcus Vance (#101)|Staff|Check-In",
                "07:11|Priya Nair (#2075)|Monthly|Check-In",
                "07:18|Tom Becker (#1533)|10-Pack|Check-In",
                "07:26|Lena Fischer (#3110)|Yearly|Check-In",
                "07:33|Diego Ramos (#2871)|Trainer|Check-In",
                "07:40|Aisha Khan (#1902)|Monthly|Check-In",
                "07:47|Jonas Weber (#1254)|Day Pass|Check-In");
    }

    @FXML
    public void onDailyReportClick() {
        // No-op daily report click handler
    }

    @FXML
    public void onDailyReportClick(ActionEvent event) {
        onDailyReportClick();
    }

    @FXML
    public void onManualCheckInClick() {
        // No-op manual check-in click handler
    }

    @FXML
    public void onManualCheckInClick(ActionEvent event) {
        onManualCheckInClick();
    }

    @FXML
    public void onManualCheckOutClick() {
        // No-op manual check-out click handler
    }

    @FXML
    public void onManualCheckOutClick(ActionEvent event) {
        onManualCheckOutClick();
    }

    // UML / Service contract signatures
    public void onCheckIn() {}
    public void onCheckIn(int customerId) {}
    public void onCheckOut() {}
    public void onCheckOut(int customerId) {}
    public void showDailyReport() {}
    public void showDailyReport(String date) {}
    public void onCorrectRecord() {}
    public void onCorrectRecord(int attendanceId) {}
}
