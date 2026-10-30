package com.gym.controller;

import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chart.M3GaugeChart;
import io.m3fx.controls.chart.M3DataPoint;
import io.m3fx.controls.chart.M3DataSeries;
import io.m3fx.controls.chart.M3Heatmap;
import io.m3fx.controls.chart.M3RadarChart;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.data.M3Timeline;
import io.m3fx.controls.effect.M3GlassPane;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.scene3d.M3AnatomyViewer3D;
import io.m3fx.controls.sheet.M3SideSheet;
import io.m3fx.controls.tabs.M3Tabs;
import io.m3fx.controls.textfield.M3TextField;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;

import java.util.List;

/**
 * Controller for the Trainer Profile screen.
 * UI-contract only (no backend/service dependencies): owns the demo athlete cohort, the
 * certification timeline, the capacity gauge and the clinical-dossier side sheet.
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

    /** Hosts M3 snackbars and dialogs for this screen (attached to the scene root at runtime). */
    private final M3Overlay feedback = new M3Overlay();

    /** Demo hourly rate the Cancel action restores. */
    private static final String DEFAULT_RATE = "74.50";

    /** Demo cohort rows in "id|name|status|subscription|program" shape (matches hero counts). */
    private final List<String> demoAthletes = List.of(
            "312|Maya Lin|Active|Elite PT|Olympic Lifting Block",
            "314|John Doe|Active|Premium PT|Powerbuilding 5x5",
            "317|Ana Silva|Active|Elite PT|VO2 Max Builder",
            "319|Leo Martins|Active|Premium PT|Hypertrophy Split",
            "321|Zoe Chen|Active|Standard PT|Mobility Foundations",
            "323|Omar Haddad|Active|Elite PT|Conditioning Circuit",
            "325|Sofia Reyes|Active|Premium PT|Return-to-Play Phase 2",
            "328|Nils Berg|Active|Standard PT|Strength Basics",
            "330|Dana Wolfe|Active|Elite PT|Peaking Cycle",
            "332|Tomas Rivera|Active|Premium PT|Fat Loss Circuit",
            "334|Ava Kowalski|Active|Standard PT|Foundations On-Ramp",
            "336|Jonas Weber|Active|Elite PT|Powerlifting Meet Prep",
            "338|Priya Nair|Active|Premium PT|Rehab Shoulder Stability",
            "340|Igor Petrov|Active|Elite PT|Hypertrophy PPL",
            "342|Lucia Moretti|Active|Premium PT|Kettlebell Conditioning",
            "345|Ethan Brooks|Active|Standard PT|Cardio Base Builder",
            "347|Hana Sato|Active|Elite PT|Olympic Lifting Block",
            "349|Pavel Novak|Active|Premium PT|Strength Peaking",
            "351|Rita Almeida|Active|Standard PT|Mobility Foundations",
            "353|Yusuf Demir|Active|Premium PT|Powerbuilding 5x5",
            "355|Grace Okafor|Active|Elite PT|Conditioning Circuit",
            "357|Ben Carter|Active|Standard PT|Fat Loss Circuit",
            "359|Lena Fischer|Active|Premium PT|Return-to-Play Phase 1");

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
        installFeedbackLayer(topAppBar);
        wireCohortColumns();
        populateHeroMetrics();
        populateOverviewCharts();
        populateCohortTable();
        populateCertificationTimeline();
    }

    /**
     * Maps the FXML-declared TableColumn definitions (header text + preferred width) onto the
     * M3DataTable: its own {@code Column} model has no FXML-constructible constructor, so without
     * this wiring the athlete cohort table renders with no header row. Extractors are pipe-index
     * based so each column shows its own field and header sorting works; the Clinical Records
     * column carries a blank extractor (the table requires one once rows exist) and renders an
     * Open button per row via its cell factory instead.
     */
    @SuppressWarnings("unchecked")
    private void wireCohortColumns() {
        if (tableAssignedAthletes == null) {
            return;
        }
        M3DataTable<String> table = (M3DataTable<String>) tableAssignedAthletes;
        javafx.collections.ObservableList<M3DataTable.Column<String>> columns = table.getColumns();
        columns.add(new M3DataTable.Column<>("ID", r -> field(r, 0), colAthleteId.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colAthleteName.getText(), r -> field(r, 1), colAthleteName.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colAthleteStatus.getText(), r -> field(r, 2), colAthleteStatus.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colAthleteSubscription.getText(), r -> field(r, 3), colAthleteSubscription.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colAthleteProgram.getText(), r -> field(r, 4), colAthleteProgram.getPrefWidth()));
        columns.add(new M3DataTable.Column<String>(colAthleteMedicalAction.getText(), r -> "", colAthleteMedicalAction.getPrefWidth())
                .withCellFactory((row, s) -> {
                    M3Button open = M3Button.text("Open");
                    open.setOnAction(e -> showMedicalProfile(Integer.parseInt(field(row, 0))));
                    return open;
                }));
        table.setOnRowAction(this::openMedicalForRow);
    }

    /** Fills the capacity gauge, demo rate and overview charts so no hero/overview slot is empty. */
    private void populateHeroMetrics() {
        if (txtHourlyRate != null && (txtHourlyRate.getText() == null || txtHourlyRate.getText().isBlank())) {
            txtHourlyRate.setText(DEFAULT_RATE);
        }
        if (gaugeCapacity != null) {
            gaugeCapacity.setTitle("CAPACITY");
            gaugeCapacity.setValue(92);
        }
    }

    /** Fills the competency radar and weekly booking heatmap on the Overview tab. */
    private void populateOverviewCharts() {
        if (chartCompetency != null) {
            chartCompetency.getAxes().addAll("Strength", "Endurance", "Agility", "Flexibility", "Speed", "Recovery");
            M3DataSeries profile = new M3DataSeries("Elena Rostova");
            profile.getData().addAll(
                    new M3DataPoint("Strength", 90),
                    new M3DataPoint("Endurance", 74),
                    new M3DataPoint("Agility", 82),
                    new M3DataPoint("Flexibility", 68),
                    new M3DataPoint("Speed", 88),
                    new M3DataPoint("Recovery", 79));
            chartCompetency.getSeriesList().addAll(profile);
        }
        if (heatmapWeeklySchedule != null) {
            heatmapWeeklySchedule.setDimensions(7, 16);
            heatmapWeeklySchedule.setRowLabels(List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"));
            for (int day = 0; day < 7; day++) {
                for (int slot = 0; slot < 16; slot++) {
                    int hour = slot + 6;
                    double intensity = 0.12;
                    if ((hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 19)) {
                        intensity = 0.80 - (day % 2 == 0 ? 0.0 : 0.15);
                    } else if (hour >= 12 && hour <= 14) {
                        intensity = 0.45;
                    }
                    heatmapWeeklySchedule.setValue(day, slot, intensity);
                }
            }
        }
    }

    /** Fills the cohort table with the demo athletes so Rule 15 counts match the hero banner. */
    @SuppressWarnings("unchecked")
    private void populateCohortTable() {
        if (tableAssignedAthletes == null) {
            return;
        }
        ((M3DataTable<String>) tableAssignedAthletes).getItems().setAll(demoAthletes);
    }

    /** Fills the certification timeline with verified credentials. */
    private void populateCertificationTimeline() {
        if (timelineCertifications == null) {
            return;
        }
        timelineCertifications.addItem("2026-03", "USAW Level 2 Advanced", "USA Weightlifting, verified", M3Timeline.Status.COMPLETED);
        timelineCertifications.addItem("2024-11", "CSCS Renewal", "NSCA, verified", M3Timeline.Status.COMPLETED);
        timelineCertifications.addItem("2023-06", "FMS Level 1", "Functional Movement Systems, verified", M3Timeline.Status.COMPLETED);
        timelineCertifications.addItem("2027-01", "FMS Level 2", "Scheduled assessment", M3Timeline.Status.ACTIVE);
        timelineCertifications.addItem("2027-08", "Precision Nutrition L1", "Planned continuing education", M3Timeline.Status.PENDING);
    }

    /** Returns the requested pipe field of a demo row, or an empty string when missing. */
    private static String field(String row, int index) {
        String[] parts = row.split("\\|");
        return index < parts.length ? parts[index] : "";
    }

    /** Row-activation entry point shared by the row click and the per-row Open button. */
    private void openMedicalForRow(String row) {
        showMedicalProfile(Integer.parseInt(field(row, 0)));
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

    /** Validates the hourly rate, reports the result inline and confirms with a snackbar. */
    private void saveProfile() {
        if (txtHourlyRate == null) {
            return;
        }
        String raw = txtHourlyRate.getText() == null ? "" : txtHourlyRate.getText().trim();
        double rate;
        try {
            rate = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            rate = -1;
        }
        if (rate <= 0) {
            txtHourlyRate.setError(true);
            txtHourlyRate.setSupportingText("Enter a positive amount in USD (for example 74.50).");
            if (lblValidationStatus != null) {
                lblValidationStatus.setText("Hourly rate must be a positive number.");
            }
            return;
        }
        txtHourlyRate.setError(false);
        txtHourlyRate.setSupportingText("USD per session hour");
        if (lblValidationStatus != null) {
            lblValidationStatus.setText("Saved. All trainer credentials and rates verified.");
        }
        feedback.showSnackbar("Profile changes saved (demo)");
    }

    /** Restores the demo rate and clears validation feedback. */
    private void discardChanges() {
        if (txtHourlyRate != null) {
            txtHourlyRate.setText(DEFAULT_RATE);
            txtHourlyRate.setError(false);
            txtHourlyRate.setSupportingText("USD per session hour");
        }
        if (lblValidationStatus != null) {
            lblValidationStatus.setText("All trainer credentials and rates verified.");
        }
        feedback.showSnackbar("Changes discarded");
    }

    @FXML
    public void onSave() {
        saveProfile();
    }

    @FXML
    public void onSave(ActionEvent event) {
        onSave();
    }

    @FXML
    public void onCancel() {
        discardChanges();
    }

    @FXML
    public void onCancel(ActionEvent event) {
        onCancel();
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

    /** Confirms the print action with a visible answer instead of a silent stub. */
    @FXML
    public void onPrintMedical() {
        feedback.showSnackbar("Clinical sheet sent to the printer (demo)");
    }

    @FXML
    public void onPrintMedical(ActionEvent event) {
        onPrintMedical();
    }

    public void showTrainer(int trainerId) {
        setSelectedTrainerId(trainerId);
    }

    /**
     * Opens the clinical-dossier side sheet for an athlete (Rules 3 and 15): fills the demo
     * medical fields and the anatomy viewer, then makes the sheet visible.
     */
    public void showMedicalProfile(int customerId) {
        setSelectedTrainerId(customerId);
        if (lblMedAthleteName != null) {
            lblMedAthleteName.setText("Athlete: " + athleteName(customerId) + " (#" + customerId + ")");
        }
        if (lblMedBloodType != null) {
            lblMedBloodType.setText("Blood Type: O+");
        }
        if (lblMedDoctor != null) {
            lblMedDoctor.setText("Physician: Dr. Robert Chen (555-0199)");
        }
        if (txtConditions != null) {
            txtConditions.setText("Right shoulder impingement (rehab phase 2). No loaded overhead work above 60% 1RM until cleared.");
        }
        if (txtAllergies != null) {
            txtAllergies.setText("Penicillin. Uses inhaler before conditioning blocks (exercise-induced asthma).");
        }
        if (anatomyViewer != null) {
            anatomyViewer.setActiveMuscle(M3AnatomyViewer3D.MuscleGroup.SHOULDERS);
        }
        if (sideSheetMedicalProfile != null) {
            sideSheetMedicalProfile.setVisible(true);
        }
    }

    /** Looks up the demo athlete name by id, falling back to the cohort id itself. */
    private String athleteName(int customerId) {
        return demoAthletes.stream()
                .filter(r -> field(r, 0).equals(String.valueOf(customerId)))
                .map(r -> field(r, 1))
                .findFirst()
                .orElse("Athlete " + customerId);
    }
}
