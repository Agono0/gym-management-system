package com.gym.controller;

import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.button.M3SplitButton;
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chip.ChipVariant;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.effect.M3GlassPane;
import io.m3fx.controls.scene3d.M3AnatomyViewer3D;
import io.m3fx.controls.textfield.M3TextField;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Pure UI skeleton controller for Workout Routine Builder screen.
 * Strictly conforms to Section 7 of docs/ui/workouts/WORKOUT_BUILDER_UI.md and docs/UML.md.
 * Contains only view bindings and no-op event handlers.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former workout.fxml: program metadata header, 3-column bento arena, action dock.
 */
public class WorkoutController implements ScreenController {

    // ---- View fields (the former fx:id injections, now built by buildView) ----
    private StackPane viewRoot;
    private M3Chip versionBadgeChip;
    private ComboBox<String> customerSelectorComboBox;
    private Label trainerSignatureLabel;
    private M3TextField programNameField;
    private M3TextField exerciseSearchField;
    private ListView<String> exerciseCatalogListView;
    private VBox pipelineContainer;
    private M3AnatomyViewer3D anatomyViewer3D;
    private Label totalTonnageLabel;
    private Label totalSetsLabel;
    private Label averageRpeLabel;
    private ComboBox<String> reassignCustomerComboBox;
    private M3SplitButton saveSplitButton;

    /**
     * Builds the complete Workout Routine Builder view and initializes all behavior. Same
     * ordering semantics the FXMLLoader used to provide: the node tree exists and every field
     * is assigned before the former {@code initialize()} work runs.
     */
    public WorkoutController() {
        viewRoot = buildView();

        populateLibraryAndClients();

    // fx:id parity: every node the markup used to tag with fx:id keeps its JavaFX id, so
    // CSS #id selectors, Node.lookup("#..."), the cross-screen rail navigation
    // (findById("navCustomers") and friends) and the interaction tests keep working unchanged.
        versionBadgeChip.setId("versionBadgeChip");
        customerSelectorComboBox.setId("customerSelectorComboBox");
        trainerSignatureLabel.setId("trainerSignatureLabel");
        programNameField.setId("programNameField");
        exerciseSearchField.setId("exerciseSearchField");
        exerciseCatalogListView.setId("exerciseCatalogListView");
        pipelineContainer.setId("pipelineContainer");
        anatomyViewer3D.setId("anatomyViewer3D");
        totalTonnageLabel.setId("totalTonnageLabel");
        totalSetsLabel.setId("totalSetsLabel");
        averageRpeLabel.setId("averageRpeLabel");
        reassignCustomerComboBox.setId("reassignCustomerComboBox");
        saveSplitButton.setId("saveSplitButton");
        buildDemoPipeline();
    }

    /**
     * Constructs the whole node tree (the programmatic replacement for workout.fxml) and wires
     * every handler.
     */
    private StackPane buildView() {
        BorderPane layout = new BorderPane();

        // Top app bar & program metadata header.
        VBox topBarContainer = new VBox();
        topBarContainer.getStyleClass().add("top-bar-container");

        HBox metadataHeader = new HBox(16);
        metadataHeader.setAlignment(Pos.CENTER_LEFT);
        metadataHeader.getStyleClass().add("app-header-box");
        metadataHeader.setPadding(new Insets(12, 20, 12, 20));
        versionBadgeChip = new M3Chip("v1 Active", ChipVariant.ASSIST);
        versionBadgeChip.getStyleClass().add("version-chip");
        customerSelectorComboBox = new ComboBox<>();
        customerSelectorComboBox.setPrefWidth(220);
        customerSelectorComboBox.setPromptText("Select Athlete...");
        trainerSignatureLabel = new Label("Coach Sarah (#12)");
        trainerSignatureLabel.getStyleClass().add("meta-value-bold");
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        metadataHeader.getChildren().addAll(
                styledLabel("Workout Routine Builder", "header-title-text"),
                versionBadgeChip,
                headerSpacer,
                styledLabel("Customer:", "meta-label"),
                customerSelectorComboBox,
                styledLabel("Creator:", "meta-label"),
                trainerSignatureLabel);

        HBox routineNameBar = new HBox(12);
        routineNameBar.setAlignment(Pos.CENTER_LEFT);
        routineNameBar.getStyleClass().add("routine-name-bar");
        routineNameBar.setPadding(new Insets(8, 20, 12, 20));
        programNameField = new M3TextField("Program Title (e.g., Meso 1 - Hypertrophy Pull)",
                io.m3fx.controls.textfield.TextFieldVariant.FILLED);
        programNameField.getStyleClass().add("program-title-field");
        HBox.setHgrow(programNameField, Priority.ALWAYS);
        M3Button historyBtn = new M3Button("History & Versions", ButtonVariant.FILLED);
        historyBtn.getStyleClass().add("tonal-button");
        historyBtn.setOnAction(e -> onNewVersion());
        routineNameBar.getChildren().addAll(programNameField, historyBtn);

        topBarContainer.getChildren().addAll(metadataHeader, routineNameBar);
        layout.setTop(topBarContainer);

        // Column 1: exercise catalog & templates deck.
        VBox catalogRail = new VBox(12);
        catalogRail.getStyleClass().add("catalog-rail");
        catalogRail.setPadding(new Insets(16));
        catalogRail.getChildren().add(styledLabel("Exercise Library", "section-header-title"));
        exerciseSearchField = new M3TextField("Search exercises, lifts, drills...",
                io.m3fx.controls.textfield.TextFieldVariant.FILLED);
        exerciseSearchField.getStyleClass().add("search-field");

        // Category filter chips.
        FlowPane filterChips = new FlowPane(6, 6);
        filterChips.getStyleClass().add("filter-chip-deck");
        M3Chip allChip = new M3Chip("All", ChipVariant.ASSIST);
        allChip.setSelected(true);
        filterChips.getChildren().addAll(
                allChip,
                new M3Chip("Chest", ChipVariant.ASSIST),
                new M3Chip("Back", ChipVariant.ASSIST),
                new M3Chip("Legs", ChipVariant.ASSIST),
                new M3Chip("Shoulders", ChipVariant.ASSIST),
                new M3Chip("Arms", ChipVariant.ASSIST),
                new M3Chip("Core", ChipVariant.ASSIST));

        // Scrollable catalog list.
        exerciseCatalogListView = new ListView<>();
        VBox.setVgrow(exerciseCatalogListView, Priority.ALWAYS);
        exerciseCatalogListView.getStyleClass().add("catalog-list-view");

        // Quick templates section.
        VBox templateBox = new VBox(6);
        templateBox.getStyleClass().add("template-preset-box");
        templateBox.getChildren().add(styledLabel("Pre-Engineered Mesocycles", "sub-header-title"));
        M3Button pushPullBtn = new M3Button("Push / Pull / Legs (6-Day Split)", ButtonVariant.FILLED);
        pushPullBtn.getStyleClass().add("text-button-compact");
        pushPullBtn.setMaxWidth(Double.MAX_VALUE);
        M3Button upperLowerBtn = new M3Button("Upper / Lower Heavy (4-Day)", ButtonVariant.FILLED);
        upperLowerBtn.getStyleClass().add("text-button-compact");
        upperLowerBtn.setMaxWidth(Double.MAX_VALUE);
        templateBox.getChildren().addAll(pushPullBtn, upperLowerBtn);

        catalogRail.getChildren().addAll(exerciseSearchField, filterChips, exerciseCatalogListView, templateBox);

        // Column 2: kinetic staging arena pipeline (center).
        VBox stagingArena = new VBox(16);
        stagingArena.getStyleClass().add("staging-arena");
        stagingArena.setPadding(new Insets(16));
        HBox stagingHeader = new HBox(12);
        stagingHeader.setAlignment(Pos.CENTER_LEFT);
        stagingHeader.getStyleClass().add("staging-header-row");
        M3Button supersetBtn = new M3Button("+ Add Superset Group", ButtonVariant.FILLED);
        supersetBtn.getStyleClass().add("outlined-button-compact");
        supersetBtn.setOnAction(e -> onBuild());
        Region stagingSpacer = new Region();
        HBox.setHgrow(stagingSpacer, Priority.ALWAYS);
        stagingHeader.getChildren().addAll(
                styledLabel("Routine Execution Pipeline", "section-header-title"),
                stagingSpacer,
                supersetBtn);

        // Dynamic exercise pipeline stack.
        pipelineContainer = new VBox(12);
        pipelineContainer.getStyleClass().add("pipeline-card-stack");
        ScrollPane pipelineScroll = new ScrollPane(pipelineContainer);
        pipelineScroll.setFitToWidth(true);
        pipelineScroll.getStyleClass().add("pipeline-scroll-pane");
        VBox.setVgrow(pipelineScroll, Priority.ALWAYS);
        stagingArena.getChildren().addAll(stagingHeader, pipelineScroll);

        // Column 3: 3D biomechanics & volume telemetry (right).
        VBox telemetryRail = new VBox(16);
        telemetryRail.getStyleClass().add("telemetry-rail");
        telemetryRail.setPadding(new Insets(16));
        telemetryRail.getChildren().add(styledLabel("Musculoskeletal Impact", "section-header-title"));

        // 3D anatomy viewport container.
        M3GlassPane anatomyViewport = new M3GlassPane();
        anatomyViewport.setPrefHeight(320);
        anatomyViewport.getStyleClass().add("anatomy-glass-viewport");
        anatomyViewer3D = new M3AnatomyViewer3D();
        anatomyViewer3D.setPrefSize(300, 300);
        anatomyViewport.getGlassChildren().add(anatomyViewer3D);

        // Load & volume metrics bento card.
        M3Card telemetryCard = new M3Card(CardVariant.ELEVATED);
        telemetryCard.getStyleClass().add("telemetry-card");
        VBox telemetryBox = new VBox(8);
        telemetryBox.getStyleClass().add("telemetry-metrics-box");
        telemetryBox.setPadding(new Insets(14));
        telemetryBox.getChildren().add(styledLabel("Session Volume Telemetry", "card-subhead"));
        totalTonnageLabel = metricRow(telemetryBox, "Total Planned Tonnage:", "14,250 kg", "metric-value-accent");
        totalSetsLabel = metricRow(telemetryBox, "Total Working Sets:", "18 Sets", "metric-value");
        averageRpeLabel = metricRow(telemetryBox, "Average Intensity:", "RPE 8.2", "metric-value-warning");
        telemetryCard.setContent(telemetryBox);

        Region telemetrySpacer = new Region();
        VBox.setVgrow(telemetrySpacer, Priority.ALWAYS);

        // Client reassignment quick flyout.
        VBox reassignmentBox = new VBox(8);
        reassignmentBox.getStyleClass().add("reassignment-box");
        reassignmentBox.getChildren().add(styledLabel("Transfer Routine", "card-subhead"));
        reassignCustomerComboBox = new ComboBox<>();
        reassignCustomerComboBox.setMaxWidth(Double.MAX_VALUE);
        reassignCustomerComboBox.setPromptText("Select Target Client...");
        M3Button reassignBtn = new M3Button("Reassign Routine", ButtonVariant.FILLED);
        reassignBtn.getStyleClass().add("tonal-button");
        reassignBtn.setMaxWidth(Double.MAX_VALUE);
        reassignBtn.setOnAction(e -> onReassign());
        reassignmentBox.getChildren().addAll(reassignCustomerComboBox, reassignBtn);

        telemetryRail.getChildren().addAll(anatomyViewport, telemetryCard, telemetrySpacer, reassignmentBox);

        SplitPane arenaSplit = new SplitPane(catalogRail, stagingArena, telemetryRail);
        arenaSplit.setDividerPositions(0.25, 0.73);
        arenaSplit.getStyleClass().add("bento-split-pane");
        layout.setCenter(arenaSplit);

        // Contextual action dock (bottom bar).
        HBox actionDock = new HBox(16);
        actionDock.setAlignment(Pos.CENTER_RIGHT);
        actionDock.getStyleClass().add("contextual-action-dock");
        actionDock.setPadding(new Insets(14, 20, 14, 20));
        M3Button deleteBtn = new M3Button("Delete Routine", ButtonVariant.FILLED);
        deleteBtn.getStyleClass().add("danger-text-button");
        deleteBtn.setOnAction(e -> onDelete());
        M3Button duplicateBtn = new M3Button("Duplicate as Draft", ButtonVariant.FILLED);
        duplicateBtn.getStyleClass().add("outlined-button");
        duplicateBtn.setOnAction(e -> onNewVersion());
        saveSplitButton = new M3SplitButton("Save Routine (v1)", ButtonVariant.FILLED, null);
        saveSplitButton.getStyleClass().add("filled-primary-split-button");
        saveSplitButton.setOnAction(e -> onBuild());
        Region dockSpacer = new Region();
        HBox.setHgrow(dockSpacer, Priority.ALWAYS);
        actionDock.getChildren().addAll(deleteBtn, dockSpacer, duplicateBtn, saveSplitButton);
        layout.setBottom(actionDock);

        viewRoot = new StackPane();
        viewRoot.getStyleClass().add("workout-builder-root");
        viewRoot.getChildren().add(layout);
        return viewRoot;
    }

    /** One label/value telemetry row; returns the value label so the field keeps a handle. */
    private static Label metricRow(VBox container, String labelText, String valueText, String valueStyle) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label(labelText);
        label.getStyleClass().add("metric-label");
        Label value = new Label(valueText);
        value.getStyleClass().add(valueStyle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(label, spacer, value);
        container.getChildren().add(row);
        return value;
    }

    /** Small label with one style class. */
    private static Label styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    @Override
    public Parent getRoot() {
        return viewRoot;
    }

    /** Fills the exercise library and the two athlete selectors with demo entries. */
    private void populateLibraryAndClients() {
        if (exerciseCatalogListView.getItems().isEmpty()) {
            exerciseCatalogListView.setItems(FXCollections.observableArrayList(
                    "Barbell Back Squat", "Front Squat", "Romanian Deadlift", "Conventional Deadlift",
                    "Bench Press", "Incline Dumbbell Press", "Overhead Press", "Pull-Up",
                    "Barbell Row", "Seated Cable Row", "Lat Pulldown", "Biceps Curl",
                    "Triceps Pushdown", "Bulgarian Split Squat", "Hip Thrust", "Farmer's Walk"));
        }
        var clients = FXCollections.observableArrayList(
                "John Doe (#102)", "Ana Silva (#1042)", "Leo Martins (#1177)",
                "Zoe Chen (#2311)", "Kim Park (#3190)", "Diego Ramos (#2871)");
        if (customerSelectorComboBox.getItems().isEmpty()) {
            customerSelectorComboBox.setItems(clients);
        }
        if (reassignCustomerComboBox.getItems().isEmpty()) {
            reassignCustomerComboBox.setItems(clients);
        }
    }

    /** Adds a few sample routine cards to the staging pipeline so the center pane is not empty. */
    private void buildDemoPipeline() {
        if (!pipelineContainer.getChildren().isEmpty()) {
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

    public void onBuild() {
    }

    public void onNewVersion() {
    }

    public void onReassign() {
        // Transfer requires a selected target client; guard against null selection
        if (reassignCustomerComboBox.getValue() == null) {
            return;
        }
    }

    public void onDelete() {
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
