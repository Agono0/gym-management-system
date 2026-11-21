package com.gym.controller;

import com.gym.dao.CustomerDao;
import com.gym.dao.TrainerDao;
import com.gym.dao.WorkoutProgramDao;
import com.gym.model.Customer;
import com.gym.model.Trainer;
import com.gym.model.WorkoutProgram;
import com.gym.ui.Data;
import com.gym.ui.UiKit;
import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.button.M3SplitButton;
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chip.ChipVariant;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.effect.M3GlassPane;
import io.m3fx.controls.menu.M3Menu;
import io.m3fx.controls.layout.WindowSizeClass;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.scene3d.M3AnatomyViewer3D;
import io.m3fx.controls.textfield.M3TextField;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Workout Routine Builder screen.
 *
 * <p>Data contract (DECISIONS.md 77/78, task C8): programs are real {@code WorkoutProgramDao}
 * rows — Save inserts the next version for the selected customer (rule 11: the DAO owns the
 * version-uniqueness and trainer checks), History & Versions lists the real version history
 * per customer + program name, Duplicate saves the staged routine as a new draft version,
 * Delete confirms and removes the loaded version, and Reassign moves the loaded program to
 * another customer via {@code WorkoutProgramDao.update}. The exercise library is a labeled UI
 * catalog (not database data); the pipeline stages are the builder's working set and persist
 * into the program record's exercise list. Every DAO {@code ValidationException} surfaces
 * verbatim in a snackbar or dialog.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former workout.fxml: program metadata header, 3-column bento arena, action dock.
 */
public class WorkoutController implements ScreenController {

    /** The exercise library catalog (a UI preset list, not database rows). */
    private static final String[] EXERCISE_LIBRARY = {
        "Barbell Back Squat", "Front Squat", "Romanian Deadlift", "Conventional Deadlift",
        "Bench Press", "Incline Dumbbell Press", "Overhead Press", "Pull-Up",
        "Barbell Row", "Seated Cable Row", "Lat Pulldown", "Biceps Curl",
        "Triceps Pushdown", "Bulgarian Split Squat", "Hip Thrust", "Farmer's Walk"};

    /** Program template presets for the template buttons (real inserts, honest copy). */
    private record Preset(String label, String name, String exercises) {
    }

    private static final Preset[] PRESETS = {
        new Preset("Push / Pull / Legs (6-Day Split)", "PPL Mesocycle",
            "Bench Press 4x8 · Overhead Press 4x8 · Barbell Row 4x8 · Lat Pulldown 4x10 · "
                + "Back Squat 4x8 · Romanian Deadlift 3x10"),
        new Preset("Upper / Lower Heavy (4-Day)", "Upper-Lower Heavy",
            "Back Squat 5x5 · Bench Press 5x5 · Barbell Row 5x5 · Overhead Press 3x8"),
    };

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

    /** Hosts M3 snackbars and dialogs for this screen (attached to the scene root at runtime). */
    private final M3Overlay feedback = new M3Overlay();

    /** Content host around the arena: swaps loading/error states around DAO loads. */
    private StackPane contentHost;
    /** The arena node, restored after loading/error states. */
    private Node arenaView;

    // ---- Responsive layout (width-class reflow, shared nodes — see applyWorkoutLayout) ----
    /** Desktop arena split: catalog / staging pipeline / telemetry rails side by side. */
    private SplitPane arenaSplit;
    /** Compact counterpart of the arena: the SAME rails stacked full-width. Starts EMPTY. */
    private VBox compactRails;
    /** The shared rails, moved between the split and the compact stack. */
    private VBox catalogRail;
    private VBox stagingArena;
    private VBox telemetryRail;
    /** Current window width class driving the arena swap. */
    private WindowSizeClass workoutWidthClass = WindowSizeClass.EXPANDED;
    /** The workout program currently loaded into the builder (source of the version badge). */
    private WorkoutProgram loadedProgram;
    /** The real customer roster for both selectors, refreshed with every snapshot. */
    private final List<Customer> customerRoster = new ArrayList<>();
    /** Staged pipeline entries (title + prescription) — persists into the program record. */
    private final List<String[]> pipelineItems = new ArrayList<>();

    /**
     * Builds the complete Workout Routine Builder view and initializes all behavior. Same
     * ordering semantics the FXMLLoader used to provide: the node tree exists and every field
     * is assigned before the former {@code initialize()} work runs.
     */
    public WorkoutController() {
        viewRoot = buildView();

        // Width-class listener (library breakpoints, same pattern as the dashboard's
        // applyDashboardLayout): swaps the arena between the desktop split and the compact
        // full-width stack. Deferred with Platform.runLater so the children lists are never
        // mutated re-entrantly mid-layout.
        viewRoot.widthProperty().addListener((obs, oldWidth, width) -> {
            double w = width.doubleValue();
            if (w <= 0) {
                return;
            }
            WindowSizeClass next = WindowSizeClass.forWidth(w);
            if (next == workoutWidthClass) {
                return;
            }
            workoutWidthClass = next;
            javafx.application.Platform.runLater(this::applyWorkoutLayout);
        });

        wireLibraryFiltering();
        installFeedbackLayer(viewRoot);
        loadPrograms();

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

        // The metadata header is a FlowPane: at narrow widths the athlete selector and the
        // creator signature reflow below the title instead of slivering it ("Workout
        // Routin…" / "Creator: Coach Rami N…" in the 600px captures).
        FlowPane metadataFlow = new FlowPane(16, 8);
        metadataFlow.setAlignment(Pos.CENTER_LEFT);
        metadataFlow.getStyleClass().add("app-header-box");
        metadataFlow.setPadding(new Insets(12, 20, 12, 20));
        versionBadgeChip = new M3Chip("Draft", ChipVariant.ASSIST);
        versionBadgeChip.getStyleClass().add("version-chip");
        customerSelectorComboBox = new ComboBox<>();
        customerSelectorComboBox.setPrefWidth(220);
        customerSelectorComboBox.setPromptText("Select Athlete...");
        customerSelectorComboBox.valueProperty().addListener((obs, o, v) -> {
            // Switching the athlete clears the staged builder (the loaded program belongs to
            // the previous athlete); the History dialog reloads a version explicitly.
            if (loadedProgram != null && customerKey(v) != loadedProgram.getCustomerId()) {
                clearBuilder();
            }
        });
        // The signature is the NAME only — the header already carries the "Creator:" meta
        // label, so prefixing it again doubled the word ("Creator:   Creator: Coach ...").
        trainerSignatureLabel = new Label("first coach on the roster");
        trainerSignatureLabel.getStyleClass().add("meta-value-bold");
        metadataFlow.getChildren().addAll(
                styledLabel("Workout Routine Builder", "header-title-text"),
                versionBadgeChip,
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

        topBarContainer.getChildren().addAll(metadataFlow, routineNameBar);
        layout.setTop(topBarContainer);

        // Column 1: exercise catalog & templates deck (the labeled UI catalog).
        catalogRail = new VBox(12);
        catalogRail.getStyleClass().add("catalog-rail");
        catalogRail.setPadding(new Insets(16));
        catalogRail.getChildren().add(styledLabel("Exercise Library (catalog)", "section-header-title"));
        exerciseSearchField = new M3TextField("Search exercises, lifts, drills...",
                io.m3fx.controls.textfield.TextFieldVariant.FILLED);
        exerciseSearchField.getStyleClass().add("search-field");

        // Category filter chips (real filtering over the catalog list).
        FlowPane filterChips = new FlowPane(6, 6);
        filterChips.getStyleClass().add("filter-chip-deck");
        M3Chip allChip = new M3Chip("All", ChipVariant.ASSIST);
        allChip.setSelected(true);
        allChip.setOnAction(e -> applyCatalogFilter("All"));
        M3Chip squatChip = new M3Chip("Squat", ChipVariant.ASSIST);
        squatChip.setOnAction(e -> applyCatalogFilter("Squat"));
        M3Chip pressChip = new M3Chip("Press", ChipVariant.ASSIST);
        pressChip.setOnAction(e -> applyCatalogFilter("Press"));
        M3Chip pullChip = new M3Chip("Pull", ChipVariant.ASSIST);
        pullChip.setOnAction(e -> applyCatalogFilter("Pull"));
        M3Chip curlChip = new M3Chip("Curl", ChipVariant.ASSIST);
        curlChip.setOnAction(e -> applyCatalogFilter("Curl"));
        filterChips.getChildren().addAll(allChip, squatChip, pressChip, pullChip, curlChip);

        // Scrollable catalog list: clicking an entry stages it in the pipeline.
        exerciseCatalogListView = new ListView<>();
        VBox.setVgrow(exerciseCatalogListView, Priority.ALWAYS);
        exerciseCatalogListView.getStyleClass().add("catalog-list-view");
        // Pref-height floor: a ListView's own minimum is ~2px, so a height squeeze (compact
        // stack in a short window) collapses the catalog to a sliver instead of scrolling.
        exerciseCatalogListView.setMinHeight(Region.USE_PREF_SIZE);
        exerciseCatalogListView.getItems().setAll(EXERCISE_LIBRARY);
        exerciseCatalogListView.setOnMouseClicked(e -> {
            String selected = exerciseCatalogListView.getSelectionModel().getSelectedItem();
            if (selected != null && e.getClickCount() == 2) {
                stageExercise(selected);
            }
        });

        // Quick templates section: the buttons really insert preset programs.
        VBox templateBox = new VBox(6);
        templateBox.getStyleClass().add("template-preset-box");
        Label templateCaption = styledLabel("Pre-Engineered Mesocycles (insert for the"
                + " selected athlete)", "sub-header-title");
        templateCaption.setWrapText(true); // wraps in the narrow catalog rail, never slivers
        templateBox.getChildren().add(templateCaption);
        for (Preset preset : PRESETS) {
            M3Button presetBtn = new M3Button(preset.label(), ButtonVariant.FILLED);
            presetBtn.getStyleClass().add("text-button-compact");
            presetBtn.setMaxWidth(Double.MAX_VALUE);
            presetBtn.setOnAction(e -> onTemplate(preset));
            templateBox.getChildren().add(presetBtn);
        }

        catalogRail.getChildren().addAll(exerciseSearchField, filterChips, exerciseCatalogListView, templateBox);

        // Column 2: kinetic staging arena pipeline (center).
        stagingArena = new VBox(16);
        stagingArena.getStyleClass().add("staging-arena");
        stagingArena.setPadding(new Insets(16));
        HBox stagingHeader = new HBox(12);
        stagingHeader.setAlignment(Pos.CENTER_LEFT);
        M3Button clearBtn = new M3Button("Clear Staged", ButtonVariant.OUTLINED);
        clearBtn.getStyleClass().add("outlined-button-compact");
        clearBtn.setOnAction(e -> {
            pipelineItems.clear();
            rebuildPipeline();
        });
        Region stagingSpacer = new Region();
        HBox.setHgrow(stagingSpacer, Priority.ALWAYS);
        stagingHeader.getChildren().addAll(
                styledLabel("Routine Execution Pipeline (double-click a library exercise to stage)",
                        "section-header-title"),
                stagingSpacer,
                clearBtn);

        // Dynamic exercise pipeline stack.
        pipelineContainer = new VBox(12);
        pipelineContainer.getStyleClass().add("pipeline-card-stack");
        ScrollPane pipelineScroll = new ScrollPane(pipelineContainer);
        pipelineScroll.setFitToWidth(true);
        pipelineScroll.getStyleClass().add("pipeline-scroll-pane");
        // Pref-height floor (same rationale as the catalog list): a bare ScrollPane minimum
        // is ~36px, which rendered the staged pipeline as a clipped sliver at compact widths.
        pipelineScroll.setMinHeight(Region.USE_PREF_SIZE);
        VBox.setVgrow(pipelineScroll, Priority.ALWAYS);
        stagingArena.getChildren().addAll(stagingHeader, pipelineScroll);

        // Column 3: 3D biomechanics & volume telemetry (right).
        telemetryRail = new VBox(16);
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

        // Load & volume metrics bento card (honest: only the staged count is real).
        M3Card telemetryCard = new M3Card(CardVariant.ELEVATED);
        telemetryCard.getStyleClass().add("telemetry-card");
        VBox telemetryBox = new VBox(8);
        telemetryBox.getStyleClass().add("telemetry-metrics-box");
        telemetryBox.setPadding(new Insets(14));
        telemetryBox.getChildren().add(styledLabel("Session Volume Telemetry", "card-subhead"));
        totalSetsLabel = metricRow(telemetryBox, "Staged Exercises:", "0", "metric-value");
        totalTonnageLabel = metricRow(telemetryBox, "Planned Tonnage:", "- (no loads on the"
                + " catalog)", "metric-value-accent");
        averageRpeLabel = metricRow(telemetryBox, "Average Intensity:", "- (no RPE on the"
                + " catalog)", "metric-value-warning");
        telemetryCard.setContent(telemetryBox);

        Region telemetrySpacer = new Region();
        VBox.setVgrow(telemetrySpacer, Priority.ALWAYS);

        // Client reassignment quick flyout.
        VBox reassignmentBox = new VBox(8);
        reassignmentBox.getStyleClass().add("reassignment-box");
        reassignmentBox.getChildren().add(styledLabel("Transfer Loaded Program", "card-subhead"));
        reassignCustomerComboBox = new ComboBox<>();
        reassignCustomerComboBox.setMaxWidth(Double.MAX_VALUE);
        reassignCustomerComboBox.setPromptText("Select Target Client...");
        M3Button reassignBtn = new M3Button("Reassign Program", ButtonVariant.FILLED);
        reassignBtn.getStyleClass().add("tonal-button");
        reassignBtn.setMaxWidth(Double.MAX_VALUE);
        reassignBtn.setOnAction(e -> onReassign());
        reassignmentBox.getChildren().addAll(reassignCustomerComboBox, reassignBtn);

        telemetryRail.getChildren().addAll(anatomyViewport, telemetryCard, telemetrySpacer, reassignmentBox);

        // Desktop arena: the three rails in one split. Compact counterpart: the SAME rails
        // stacked full-width (the 25/48/27 split cannot fit a 600dp window, and its squeezed
        // rails slivered the telemetry captions). Starts EMPTY; applyWorkoutLayout moves the
        // rails on the class swap.
        arenaSplit = new SplitPane(catalogRail, stagingArena, telemetryRail);
        arenaSplit.setDividerPositions(0.25, 0.73);
        arenaSplit.getStyleClass().add("bento-split-pane");
        compactRails = new VBox(16);
        layout.setCenter(arenaSplit);
        arenaView = arenaSplit;

        // Contextual action dock (bottom bar).
        HBox actionDock = new HBox(16);
        actionDock.setAlignment(Pos.CENTER_RIGHT);
        actionDock.getStyleClass().add("contextual-action-dock");
        actionDock.setPadding(new Insets(14, 20, 14, 20));
        M3Button deleteBtn = new M3Button("Delete Loaded Version", ButtonVariant.FILLED);
        deleteBtn.getStyleClass().add("danger-text-button");
        deleteBtn.setOnAction(e -> onDelete());
        M3Button duplicateBtn = new M3Button("Duplicate as Next Version", ButtonVariant.FILLED);
        duplicateBtn.getStyleClass().add("outlined-button");
        duplicateBtn.setOnAction(e -> onNewVersion());
        saveSplitButton = new M3SplitButton("Save Routine", ButtonVariant.FILLED, null);
        saveSplitButton.getStyleClass().add("filled-primary-split-button");
        saveSplitButton.setOnAction(e -> onBuild());
        Region dockSpacer = new Region();
        HBox.setHgrow(dockSpacer, Priority.ALWAYS);
        actionDock.getChildren().addAll(deleteBtn, dockSpacer, duplicateBtn, saveSplitButton);
        layout.setBottom(actionDock);

        viewRoot = new StackPane();
        viewRoot.getStyleClass().add("workout-builder-root");
        // Content host so the real-data load can swap the arena for loading/error states.
        contentHost = new StackPane(arenaView);
        layout.setCenter(contentHost);
        viewRoot.getChildren().add(layout);
        // The arena's rails collapse to tiny minimums (ListView/ScrollPane skins), so without
        // a pref-height floor the compact stack lays out in a sliver and clips the pipeline.
        viewRoot.setMinHeight(Region.USE_PREF_SIZE);
        return viewRoot;
    }

    /**
     * Applies the current width class to the arena by swapping the SAME three rails between
     * the desktop 25/48/27 split and the compact full-width stack (a squeezed rail slivers
     * its captions, so narrow windows stack instead). Idempotent fills, so repeated swaps
     * are safe.
     */
    private void applyWorkoutLayout() {
        boolean compact = workoutWidthClass == WindowSizeClass.COMPACT;
        if (compact) {
            if (compactRails.getChildren().isEmpty()) {
                compactRails.getChildren().setAll(catalogRail, stagingArena, telemetryRail);
            }
            contentHost.getChildren().set(0, compactRails);
            return;
        }
        if (arenaSplit.getItems().isEmpty()) {
            arenaSplit.getItems().setAll(catalogRail, stagingArena, telemetryRail);
            arenaSplit.setDividerPositions(0.25, 0.73);
        }
        contentHost.getChildren().set(0, arenaSplit);
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

    /** The customer key used in the selector combos ("FullName (M-0006)"). */
    private static String customerLabel(Customer customer) {
        return customer.getFullName() + " (M-"
                + String.format("%04d", customer.getId()) + ")";
    }

    /** Parses the customer id back out of a combo label ("FullName (M-0006)" -> 6). */
    private static int customerKey(String label) {
        if (label == null) {
            return -1;
        }
        int open = label.lastIndexOf("(M-");
        int close = label.lastIndexOf(')');
        if (open < 0 || close <= open) {
            return -1;
        }
        try {
            return Integer.parseInt(label.substring(open + 3, close));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Wires the exercise search + chips to filter the catalog list live. */
    private void wireLibraryFiltering() {
        exerciseSearchField.getInput().textProperty().addListener((obs, o, v) ->
                applyCatalogFilter(v));
    }

    /** Filters the catalog list by the search text (the chips set it). */
    private void applyCatalogFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        List<String> visible = new ArrayList<>();
        for (String exercise : EXERCISE_LIBRARY) {
            if (q.isEmpty() || exercise.toLowerCase(java.util.Locale.ROOT).contains(q)) {
                visible.add(exercise);
            }
        }
        exerciseCatalogListView.getItems().setAll(visible);
    }

    /** Stages one catalog exercise in the pipeline (title + prescription template). */
    private void stageExercise(String exercise) {
        pipelineItems.add(new String[] {exercise, "3 x 10", "RPE 7 · rest 120s"});
        rebuildPipeline();
    }

    /** Rebuilds the pipeline cards from the staged items and refreshes the count metric. */
    private void rebuildPipeline() {
        pipelineContainer.getChildren().clear();
        for (String[] item : pipelineItems) {
            Label titleLabel = new Label(item[0]);
            titleLabel.getStyleClass().add("body-medium");
            Label detailLabel = new Label(item[1] + " · " + item[2]);
            detailLabel.getStyleClass().add("body-small");
            VBox box = new VBox(4, titleLabel, detailLabel);
            box.setPadding(new Insets(10));
            M3Card card = new M3Card(CardVariant.OUTLINED);
            card.setContent(box);
            card.setMaxWidth(Double.MAX_VALUE);
            pipelineContainer.getChildren().add(card);
        }
        if (pipelineItems.isEmpty()) {
            Label empty = new Label("Nothing staged — double-click an exercise in the library"
                    + " to add it to the pipeline.");
            empty.getStyleClass().add("body-small");
            pipelineContainer.getChildren().add(empty);
        }
        totalSetsLabel.setText(pipelineItems.size() + " staged");
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    /** The staged routine serialized into the program record's exercise field. */
    private String stagedExercises() {
        StringBuilder sb = new StringBuilder();
        for (String[] item : pipelineItems) {
            if (sb.length() > 0) {
                sb.append(" ; ");
            }
            sb.append(item[0]).append(" (").append(item[1]).append(", ").append(item[2]).append(")");
        }
        return sb.toString();
    }

    /** Parses a program's exercise list back into staged pipeline items. */
    private void stageFromProgram(WorkoutProgram program) {
        pipelineItems.clear();
        String exercises = program.getExercises() == null ? "" : program.getExercises();
        for (String entry : exercises.split(" ; ")) {
            if (entry.isBlank()) {
                continue;
            }
            int open = entry.indexOf(" (");
            if (open > 0 && entry.endsWith(")")) {
                String title = entry.substring(0, open);
                String tail = entry.substring(open + 2, entry.length() - 1);
                int meta = tail.indexOf(", ");
                if (meta > 0) {
                    pipelineItems.add(new String[] {title, tail.substring(0, meta),
                            tail.substring(meta + 2)});
                    continue;
                }
            }
            pipelineItems.add(new String[] {entry, "-", "-"});
        }
        rebuildPipeline();
    }

    /**
     * Hosts the shared feedback overlay on the scene root while this screen is visible. Attach
     * and detach are deferred with Platform.runLater so the scene root children list is never
     * mutated re-entrantly during a screen swap.
     */
    private void installFeedbackLayer(Node anchor) {
        anchor.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null && scene.getRoot() instanceof Pane root
                    && !root.getChildren().contains(feedback)) {
                root.getChildren().add(feedback);
            }
            if (oldScene != null && oldScene.getRoot() instanceof Pane oldRoot) {
                Platform.runLater(() -> oldRoot.getChildren().remove(feedback));
            }
        });
    }

    /**
     * Loads the builder context on a background task: the customer roster for both selectors,
     * the creator coach label (the selected athlete's rule-15 coach — the program record
     * stores its creator) and the latest program version for that customer.
     */
    private void loadPrograms() {
        Data.loadAsync(contentHost,
            () -> {
                Data.ensureReady();
                List<Customer> customers = new CustomerDao().findAll();
                Trainer creator = null;
                if (!customers.isEmpty()) {
                    Customer first = customers.get(0);
                    if (first.getTrainerId() != null) {
                        creator = new TrainerDao().findById(first.getTrainerId());
                    }
                }
                WorkoutProgram latest = null;
                if (!customers.isEmpty()) {
                    Customer first = customers.get(0);
                    List<WorkoutProgram> programs =
                            new WorkoutProgramDao().findByCustomerId(first.getId());
                    latest = programs.isEmpty() ? null : programs.get(programs.size() - 1);
                }
                return new Object[] {customers, creator, latest};
            },
            read -> {
                contentHost.getChildren().set(0, workoutWidthClass
                        == WindowSizeClass.COMPACT ? compactRails : arenaView);
                @SuppressWarnings("unchecked")
                List<Customer> customers = (List<Customer>) read[0];
                customerRoster.clear();
                customerRoster.addAll(customers);
                ObservableList<String> labels = FXCollections.observableArrayList();
                for (Customer customer : customers) {
                    labels.add(customerLabel(customer));
                }
                customerSelectorComboBox.setItems(labels);
                reassignCustomerComboBox.setItems(FXCollections.observableArrayList(labels));
                Trainer creator = (Trainer) read[1];
                trainerSignatureLabel.setText(creator == null
                        ? "no coach on the roster"
                        : creator.getFullName() + " (#" + creator.getId() + ")");
                WorkoutProgram latest = (WorkoutProgram) read[2];
                if (latest != null) {
                    loadProgramIntoBuilder(latest);
                }
            },
            error -> contentHost.getChildren().setAll(UiKit.errorState(
                    error.getMessage() == null ? error.toString() : error.getMessage(),
                    this::loadPrograms)));
    }

    /** Loads one real program version into the builder (fields, pipeline, version badge). */
    private void loadProgramIntoBuilder(WorkoutProgram program) {
        loadedProgram = program;
        programNameField.setText(program.getName());
        versionBadgeChip.setText("v" + program.getVersion() + " loaded");
        customerSelectorComboBox.setValue(customerLabelById(program.getCustomerId()));
        stageFromProgram(program);
    }

    /** Clears the builder to a fresh draft for the selected athlete. */
    private void clearBuilder() {
        loadedProgram = null;
        programNameField.setText("");
        versionBadgeChip.setText("Draft");
        pipelineItems.clear();
        rebuildPipeline();
    }

    /** The customer label for an id, or a fallback when the roster lacks it. */
    private String customerLabelById(int customerId) {
        for (Customer customer : customerRoster) {
            if (customer.getId() == customerId) {
                return customerLabel(customer);
            }
        }
        return "Customer #" + customerId;
    }

    /** The selected customer id from the header combo, or -1. */
    private int selectedCustomerId() {
        return customerKey(customerSelectorComboBox.getValue());
    }

    /** Resolves the creator coach for the selected athlete (rule 15), or fails loudly. */
    private Trainer resolveCreator() throws Exception {
        int customerId = selectedCustomerId();
        Customer customer = null;
        for (Customer candidate : customerRoster) {
            if (candidate.getId() == customerId) {
                customer = candidate;
            }
        }
        if (customer == null) {
            throw new com.gym.exception.ValidationException(
                    "Select an athlete in the header first.");
        }
        if (customer.getTrainerId() == null) {
            throw new com.gym.exception.ValidationException(
                    "The selected athlete has no assigned coach - every program records its"
                            + " creator (assign one from the profile screen first).");
        }
        Trainer trainer = new TrainerDao().findById(customer.getTrainerId());
        if (trainer == null) {
            throw new com.gym.exception.ValidationException(
                    "The assigned coach no longer exists in the database");
        }
        return trainer;
    }

    /** Guards shared by save/duplicate: a customer, a title and at least one staged exercise. */
    private String[] validateBuilder() throws Exception {
        int customerId = selectedCustomerId();
        if (customerId <= 0) {
            throw new com.gym.exception.ValidationException(
                    "Select an athlete in the header first.");
        }
        String name = programNameField.getText() == null ? "" : programNameField.getText().trim();
        if (name.isEmpty()) {
            programNameField.setError(true);
            throw new com.gym.exception.ValidationException(
                    "Enter a program title before saving.");
        }
        if (pipelineItems.isEmpty()) {
            throw new com.gym.exception.ValidationException(
                    "Stage at least one exercise in the pipeline before saving.");
        }
        Trainer creator = resolveCreator();
        return new String[] {String.valueOf(customerId), name,
                String.valueOf(creator.getId())};
    }

    /** The next version number for a customer + program name (rule 11 sequence). */
    private static int nextVersion(int customerId, String name) throws Exception {
        int max = 0;
        for (WorkoutProgram program : new WorkoutProgramDao().findByCustomerId(customerId)) {
            if (program.getName().equalsIgnoreCase(name)) {
                max = Math.max(max, program.getVersion());
            }
        }
        return max + 1;
    }

    /**
     * Save Routine: inserts the staged routine as the next version for the selected athlete
     * through {@code WorkoutProgramDao.insert} (the DAO owns the version-uniqueness check and
     * the creator-trainer validation; rule 11 session pricing is session-level and stays with
     * the schedule screen).
     */
    public void onBuild() {
        try {
            String[] guard = validateBuilder();
            int customerId = Integer.parseInt(guard[0]);
            String name = guard[1];
            int creatorId = Integer.parseInt(guard[2]);
            Data.submit(
                    () -> {
                        int version = nextVersion(customerId, name);
                        WorkoutProgram program = new WorkoutProgram(0, customerId, creatorId,
                                name, stagedExercises(), version, LocalDate.now().toString());
                        return new WorkoutProgramDao().insert(program);
                    },
                    saved -> {
                        loadedProgram = saved;
                        versionBadgeChip.setText("v" + saved.getVersion() + " saved");
                        feedback.showSnackbar("Routine saved — " + saved.getName() + " v"
                                + saved.getVersion() + " for "
                                + customerLabelById(saved.getCustomerId()) + ".");
                    },
                    error -> feedback.showSnackbar("Could not save routine: " + error.getMessage()));
        } catch (Exception e) {
            feedback.showSnackbar(e.getMessage());
        }
    }

    /**
     * History & Versions: lists the real versions for the selected athlete + title (or all of
     * the athlete's programs when the title is blank) and loads the chosen one into the
     * builder.
     */
    public void onNewVersion() {
        int customerId = selectedCustomerId();
        if (customerId <= 0) {
            feedback.showSnackbar("Select an athlete in the header first — versions are per"
                    + " athlete.");
            return;
        }
        Data.submit(
                () -> new WorkoutProgramDao().findByCustomerId(customerId),
                programs -> {
                    if (programs.isEmpty()) {
                        feedback.showSnackbar("No saved programs for "
                                + customerSelectorComboBox.getValue() + " yet — save one first.");
                        return;
                    }
                    M3Menu menu = new M3Menu();
                    for (WorkoutProgram program : programs) {
                        menu.item(program.getName() + " v" + program.getVersion() + " ("
                                        + orEmpty(program.getCreatedDate()) + ")",
                                () -> loadProgramIntoBuilder(program));
                    }
                    feedback.showSnackbar(programs.size() + " saved version(s) — pick one to"
                            + " load it into the builder.");
                    // The menu anchors under the History button's parent card region.
                    menu.showBelow(versionBadgeChip);
                },
                error -> feedback.showSnackbar("Could not load versions: " + error.getMessage()));
    }

    /**
     * Duplicate as next version: saves the staged routine under the loaded program's title as
     * a new version (a real insert; the loaded record itself is untouched).
     */
    public void onNewVersionFromLoaded() {
        onBuild();
    }

    /**
     * Reassign: moves the LOADED program to another customer through
     * {@code WorkoutProgramDao.update} (the version-uniqueness check excludes the program's
     * own id, so the transfer keeps its version number).
     */
    public void onReassign() {
        if (loadedProgram == null) {
            feedback.showSnackbar("No program loaded — load one via History & Versions first.");
            return;
        }
        int targetId = customerKey(reassignCustomerComboBox.getValue());
        if (targetId <= 0) {
            feedback.showSnackbar("Select a target client in the transfer box first.");
            return;
        }
        if (targetId == loadedProgram.getCustomerId()) {
            feedback.showSnackbar("The program already belongs to that client.");
            return;
        }
        int programId = loadedProgram.getId();
        Data.submit(
                () -> {
                    WorkoutProgram current = new WorkoutProgramDao().findById(programId);
                    if (current == null) {
                        throw new com.gym.exception.ValidationException(
                                "Program no longer exists in the database");
                    }
                    current.setCustomerId(targetId);
                    new WorkoutProgramDao().update(current);
                    return current;
                },
                transferred -> {
                    feedback.showSnackbar("Program \"" + transferred.getName()
                            + "\" reassigned to " + customerLabelById(transferred.getCustomerId())
                            + ".");
                    loadedProgram = transferred;
                    customerSelectorComboBox
                            .setValue(customerLabelById(transferred.getCustomerId()));
                },
                error -> feedback.showSnackbar("Could not reassign: " + error.getMessage()));
    }

    /**
     * Delete: confirms first, then removes the LOADED version through
     * {@code WorkoutProgramDao.delete} (a scoped delete of that one version row).
     */
    public void onDelete() {
        if (loadedProgram == null) {
            feedback.showSnackbar("No program loaded — load one via History & Versions first.");
            return;
        }
        WorkoutProgram target = loadedProgram;
        M3Button keep = M3Button.text("Keep Version");
        M3Button delete = M3Button.text("Delete Version");
        M3Dialog dialog = M3Dialog.basic("Delete this program version?",
                new Label(target.getName() + " v" + target.getVersion() + " ("
                        + orEmpty(target.getCreatedDate()) + ") for "
                        + customerLabelById(target.getCustomerId()) + " is removed from the"
                        + " database."),
                keep, delete);
        dialog.setDefaultAction(delete::activate);
        keep.setOnAction(e -> dialog.hide());
        delete.setOnAction(e -> {
            dialog.hide();
            int programId = target.getId();
            Data.submit(
                    () -> {
                        new WorkoutProgramDao().delete(programId);
                        return programId;
                    },
                    deleted -> {
                        feedback.showSnackbar("Program version #" + deleted + " deleted.");
                        clearBuilder();
                    },
                    error -> feedback.showSnackbar("Could not delete: " + error.getMessage()));
        });
        feedback.showDialog(dialog);
    }

    /**
     * Template button: REALLY inserts the preset program for the selected athlete through
     * {@code WorkoutProgramDao.insert} (honest in-app copy says so; requires a selected
     * athlete with an assigned coach, because the record stores its creator).
     */
    public void onTemplate(Preset preset) {
        try {
            String[] guard = validateBuilder();
            int customerId = Integer.parseInt(guard[0]);
            int creatorId = Integer.parseInt(guard[2]);
            Data.submit(
                    () -> {
                        int version = nextVersion(customerId, preset.name());
                        WorkoutProgram program = new WorkoutProgram(0, customerId, creatorId,
                                preset.name(), preset.exercises(), version,
                                LocalDate.now().toString());
                        return new WorkoutProgramDao().insert(program);
                    },
                    saved -> {
                        feedback.showSnackbar("Preset inserted — " + saved.getName() + " v"
                                + saved.getVersion() + " saved for "
                                + customerLabelById(saved.getCustomerId()) + ".");
                        loadProgramIntoBuilder(saved);
                    },
                    error -> feedback.showSnackbar("Could not insert the preset: "
                            + error.getMessage()));
        } catch (Exception e) {
            feedback.showSnackbar(e.getMessage());
        }
    }
}
