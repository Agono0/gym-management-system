package com.gym.controller;

import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chart.M3GaugeChart;
import io.m3fx.controls.chart.M3Heatmap;
import io.m3fx.controls.chart.M3RadarChart;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.data.M3Timeline;
import io.m3fx.controls.effect.M3GlassPane;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.scene3d.M3AnatomyViewer3D;
import io.m3fx.controls.sheet.M3SideSheet;
import io.m3fx.controls.tabs.M3Tabs;
import io.m3fx.controls.textfield.M3TextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextArea;

/**
 * Controller skeleton for Trainer Profile screen.
 * Strictly UI contract stubs matching Section 7 without backend/service dependencies.
 */
public class TrainerProfileController {

    private static int selectedTrainerId;

    @FXML private M3TopAppBar topAppBar;
    @FXML private M3GlassPane heroGlassPane;
    @FXML private Label lblCoachName;
    @FXML private Label lblSpecialization;
    @FXML private Label lblEmail;
    @FXML private Label lblPhone;
    @FXML private Label lblExperience;
    @FXML private M3TextField txtHourlyRate;
    @FXML private M3GaugeChart gaugeCapacity;
    @FXML private Label lblCapacityText;
    @FXML private M3Tabs profileTabs;
    @FXML private Tab tabOverview;
    @FXML private M3Card cardRadar;
    @FXML private M3RadarChart chartCompetency;
    @FXML private M3Card cardScheduleDensity;
    @FXML private M3Heatmap heatmapWeeklySchedule;
    @FXML private Tab tabAthletes;
    @FXML private M3DataTable<?> tableAssignedAthletes;
    @FXML private TableColumn<?, ?> colAthleteId;
    @FXML private TableColumn<?, ?> colAthleteName;
    @FXML private TableColumn<?, ?> colAthleteStatus;
    @FXML private TableColumn<?, ?> colAthleteSubscription;
    @FXML private TableColumn<?, ?> colAthleteProgram;
    @FXML private TableColumn<?, ?> colAthleteMedicalAction;
    @FXML private Tab tabCertifications;
    @FXML private M3Card cardTimeline;
    @FXML private M3Timeline timelineCertifications;
    @FXML private Label lblValidationStatus;
    @FXML private M3Button btnCancel;
    @FXML private M3Button btnSave;
    @FXML private M3SideSheet sideSheetMedicalProfile;
    @FXML private M3IconButton btnCloseMedicalFlyout;
    @FXML private Label lblMedAthleteName;
    @FXML private Label lblMedBloodType;
    @FXML private Label lblMedDoctor;
    @FXML private M3AnatomyViewer3D anatomyViewer;
    @FXML private TextArea txtConditions;
    @FXML private TextArea txtAllergies;
    @FXML private M3Button btnPrintMedical;

    public TrainerProfileController() {
        // Public no-arg constructor
    }

    public static void setSelectedTrainerId(int id) {
        selectedTrainerId = id;
    }

    public static int getSelectedTrainerId() {
        return selectedTrainerId;
    }

    @FXML
    public void initialize() {
        wireCohortColumns();
    }

    /**
     * Maps the FXML-declared TableColumn definitions (header text + preferred width) onto the
     * M3DataTable: its own {@code Column} model has no FXML-constructible constructor, so without
     * this wiring the athlete cohort table renders with no header row. Extractors enable header
     * sorting; the Clinical Records column passes no extractor, matching its {@code sortable="false"}.
     */
    @SuppressWarnings("unchecked")
    private void wireCohortColumns() {
        if (tableAssignedAthletes == null) {
            return;
        }
        javafx.collections.ObservableList<M3DataTable.Column<String>> columns =
                ((M3DataTable<String>) tableAssignedAthletes).getColumns();
        columns.add(new M3DataTable.Column<>(colAthleteId.getText(), String::toString, colAthleteId.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colAthleteName.getText(), String::toString, colAthleteName.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colAthleteStatus.getText(), String::toString, colAthleteStatus.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colAthleteSubscription.getText(), String::toString, colAthleteSubscription.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colAthleteProgram.getText(), String::toString, colAthleteProgram.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colAthleteMedicalAction.getText(), null, colAthleteMedicalAction.getPrefWidth()));
    }

    @FXML
    public void onSave() {
        // UI save stub
    }

    @FXML
    public void onSave(ActionEvent event) {
        // UI save stub overload
    }

    @FXML
    public void onCancel() {
        // UI cancel stub
    }

    @FXML
    public void onCancel(ActionEvent event) {
        // UI cancel stub overload
    }

    @FXML
    public void onCloseMedical() {
        if (sideSheetMedicalProfile != null) {
            sideSheetMedicalProfile.setVisible(false);
        }
    }

    @FXML
    public void onCloseMedical(ActionEvent event) {
        onCloseMedical();
    }

    @FXML
    public void onPrintMedical() {
        // UI print stub
    }

    @FXML
    public void onPrintMedical(ActionEvent event) {
        // UI print stub overload
    }

    public void showTrainer(int trainerId) {
        // UI presentation stub
    }

    public void showMedicalProfile(int customerId) {
        // UI presentation stub
    }
}
