package com.gym.controller;

import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.button.M3SplitButton;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.scene3d.M3AnatomyViewer3D;
import io.m3fx.controls.textfield.M3TextField;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

/**
 * Pure UI skeleton controller for Workout Routine Builder screen.
 * Strictly conforms to Section 7 of docs/ui/workouts/WORKOUT_BUILDER_UI.md and docs/UML.md.
 * Contains only declarative @FXML bindings and empty no-op event handlers.
 */
public class WorkoutController {

    @FXML private M3Chip versionBadgeChip;
    @FXML private ComboBox<String> customerSelectorComboBox;
    @FXML private Label trainerSignatureLabel;
    @FXML private M3TextField programNameField;
    @FXML private M3TextField exerciseSearchField;
    @FXML private ListView<String> exerciseCatalogListView;
    @FXML private VBox pipelineContainer;
    @FXML private M3AnatomyViewer3D anatomyViewer3D;
    @FXML private Label totalTonnageLabel;
    @FXML private Label totalSetsLabel;
    @FXML private Label averageRpeLabel;
    @FXML private ComboBox<String> reassignCustomerComboBox;
    @FXML private M3SplitButton saveSplitButton;

    public WorkoutController() {
    }

    @FXML
    public void initialize() {
        populateLibraryAndClients();
        buildDemoPipeline();
    }

    /** Fills the exercise library and the two athlete selectors with demo entries. */
    private void populateLibraryAndClients() {
        if (exerciseCatalogListView != null && exerciseCatalogListView.getItems().isEmpty()) {
            exerciseCatalogListView.setItems(FXCollections.observableArrayList(
                    "Barbell Back Squat", "Front Squat", "Romanian Deadlift", "Conventional Deadlift",
                    "Bench Press", "Incline Dumbbell Press", "Overhead Press", "Pull-Up",
                    "Barbell Row", "Seated Cable Row", "Lat Pulldown", "Biceps Curl",
                    "Triceps Pushdown", "Bulgarian Split Squat", "Hip Thrust", "Farmer's Walk"));
        }
        var clients = FXCollections.observableArrayList(
                "John Doe (#102)", "Ana Silva (#1042)", "Leo Martins (#1177)",
                "Zoe Chen (#2311)", "Kim Park (#3190)", "Diego Ramos (#2871)");
        if (customerSelectorComboBox != null && customerSelectorComboBox.getItems().isEmpty()) {
            customerSelectorComboBox.setItems(clients);
        }
        if (reassignCustomerComboBox != null && reassignCustomerComboBox.getItems().isEmpty()) {
            reassignCustomerComboBox.setItems(clients);
        }
    }

    /** Adds a few sample routine cards to the staging pipeline so the center pane is not empty. */
    private void buildDemoPipeline() {
        if (pipelineContainer == null || !pipelineContainer.getChildren().isEmpty()) {
            return;
        }
        addPipelineCard("A1 · Barbell Back Squat", "4 x 8 @ 140 kg", "RPE 8 · rest 180s");
        addPipelineCard("A2 · Bulgarian Split Squat", "3 x 10 @ 24 kg", "RPE 7 · rest 90s");
        addPipelineCard("B1 · Romanian Deadlift", "4 x 8 @ 100 kg", "RPE 8 · rest 150s");
    }

    private void addPipelineCard(String title, String prescription, String meta) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("body-medium");
        Label detailLabel = new Label(prescription + " · " + meta);
        detailLabel.getStyleClass().add("body-small");
        VBox box = new VBox(4, titleLabel, detailLabel);
        box.setPadding(new Insets(10));
        M3Card card = new M3Card(CardVariant.OUTLINED);
        card.setContent(box);
        card.setMaxWidth(Double.MAX_VALUE);
        pipelineContainer.getChildren().add(card);
    }

    @FXML
    public void onBuild() {
    }

    @FXML
    public void onBuild(ActionEvent event) {
    }

    @FXML
    public void onNewVersion() {
    }

    @FXML
    public void onNewVersion(ActionEvent event) {
    }

    @FXML
    public void onReassign() {
        // Transfer requires a selected target client; guard against null selection
        if (reassignCustomerComboBox == null || reassignCustomerComboBox.getValue() == null) {
            return;
        }
    }

    @FXML
    public void onReassign(ActionEvent event) {
    }

    @FXML
    public void onDelete() {
    }

    @FXML
    public void onDelete(ActionEvent event) {
    }

    public M3Chip getVersionBadgeChip() { return versionBadgeChip; }
    public ComboBox<?> getCustomerSelectorComboBox() { return customerSelectorComboBox; }
    public Label getTrainerSignatureLabel() { return trainerSignatureLabel; }
    public M3TextField getProgramNameField() { return programNameField; }
    public M3TextField getExerciseSearchField() { return exerciseSearchField; }
    public ListView<String> getExerciseCatalogListView() { return exerciseCatalogListView; }
    public VBox getPipelineContainer() { return pipelineContainer; }
    public M3AnatomyViewer3D getAnatomyViewer3D() { return anatomyViewer3D; }
    public Label getTotalTonnageLabel() { return totalTonnageLabel; }
    public Label getTotalSetsLabel() { return totalSetsLabel; }
    public Label getAverageRpeLabel() { return averageRpeLabel; }
    public ComboBox<?> getReassignCustomerComboBox() { return reassignCustomerComboBox; }
    public M3SplitButton getSaveSplitButton() { return saveSplitButton; }
}
