package com.gym.controller;

import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chart.M3ActivityCalendar;
import io.m3fx.controls.chart.M3DataPoint;
import io.m3fx.controls.chart.M3DataSeries;
import io.m3fx.controls.chart.M3DonutChart;
import io.m3fx.controls.chart.M3ScatterPlot;
import io.m3fx.controls.chart.M3ScatterPoint;
import io.m3fx.controls.chart.M3ScatterSeries;
import io.m3fx.controls.chart.M3SplineChart;
import io.m3fx.controls.common.M3Badge;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.data.M3Timeline;
import io.m3fx.controls.effect.M3ParticleCanvas;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.IconButtonVariant;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.segmented.M3SegmentedButton;
import io.m3fx.controls.sheet.M3BottomSheet;
import io.m3fx.controls.slider.M3Slider;
import io.m3fx.controls.textfield.M3TextField;
import io.m3fx.controls.textfield.TextFieldVariant;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Controller for Biomechanical Progress &amp; Analytics.
 * Conforms to docs/ui/members/MEMBER_PROGRESS_UI.md Section 7 contract.
 * Pure UI: strictly no backend service, DAO, or database logic.
 *
 * <p>UX contract: the time-horizon segments actually reslice the trajectory charts, the log
 * entry bottom sheet opens modally with scrim and drag-to-dismiss, saving validates and
 * answers with a snackbar while appending the reading to the chart, and all demo telemetry
 * renders real data instead of placeholder dashes.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former progress.fxml: telemetry ribbon, trajectory chart, density calendar, composition
 * donut, VBT scatter, tape timeline, log-entry sheet.
 */
public class ProgressController implements ScreenController {

    /** Weekly sample count for the full horizon (ALL); shorter horizons slice the tail. */
    private static final int WEEKLY_SAMPLES = 64;
    /** Points shown per horizon: 1M / 3M / 6M / 1Y / ALL. */
    private static final int[] HORIZON_POINTS = {4, 13, 26, 52, WEEKLY_SAMPLES};

    // ---- View fields (the former fx:id injections, now built by buildView) ----
    private StackPane rootPane;
    private BorderPane mainLayout;
    private M3TopAppBar topAppBar;
    private M3IconButton btnBack;
    private M3SegmentedButton segTimeHorizon;
    private M3Button btnLogEntry;
    private M3StatCard statCurrentWeight;
    private M3StatCard statBodyFat;
    private M3StatCard statLeanMass;
    private M3StatCard statVbtMax;
    private M3SplineChart splineBiometrics;
    private M3ActivityCalendar activityHeatmap;
    private M3DonutChart donutComposition;
    private Label lblRegressionStats;
    private M3ScatterPlot scatterVbt;
    private M3Timeline timelineAnthropometrics;
    private M3ParticleCanvas celebrationCanvas;
    private M3BottomSheet sheetLogEntry;
    private M3TextField txtEntryDate;
    private M3TextField txtWeight;
    private M3Slider sliderWeight;
    private M3TextField txtBodyFat;
    private M3TextField txtTargetWeight;
    private M3TextField txtMeasurements;

    /** Overlay host for snackbars and the modal bottom sheet (no fx:id, added in code). */
    private final M3Overlay overlay = new M3Overlay();
    /** Trajectory series kept as fields so horizons and new log entries can reslice them. */
    private M3DataSeries weightSeries;
    private M3DataSeries bodyFatSeries;

    /**
     * Builds the complete Biomechanical Progress view and initializes all behavior. Same
     * ordering semantics the FXMLLoader used to provide: the node tree exists and every field
     * is assigned before the former {@code initialize()} work runs.
     */
    public ProgressController() {
        viewRootSetup();

        prefillTelemetry();
        populateTrajectoryChart();
        populateActivityHeatmap();
        populateCompositionDonut();
        populateVbtScatter();

    // fx:id parity: every node the markup used to tag with fx:id keeps its JavaFX id, so
    // CSS #id selectors, Node.lookup("#..."), the cross-screen rail navigation
    // (findById("navCustomers") and friends) and the interaction tests keep working unchanged.
        rootPane.setId("rootPane");
        mainLayout.setId("mainLayout");
        topAppBar.setId("topAppBar");
        btnBack.setId("btnBack");
        segTimeHorizon.setId("segTimeHorizon");
        btnLogEntry.setId("btnLogEntry");
        statCurrentWeight.setId("statCurrentWeight");
        statBodyFat.setId("statBodyFat");
        statLeanMass.setId("statLeanMass");
        statVbtMax.setId("statVbtMax");
        splineBiometrics.setId("splineBiometrics");
        activityHeatmap.setId("activityHeatmap");
        donutComposition.setId("donutComposition");
        lblRegressionStats.setId("lblRegressionStats");
        scatterVbt.setId("scatterVbt");
        timelineAnthropometrics.setId("timelineAnthropometrics");
        celebrationCanvas.setId("celebrationCanvas");
        sheetLogEntry.setId("sheetLogEntry");
        txtEntryDate.setId("txtEntryDate");
        txtWeight.setId("txtWeight");
        sliderWeight.setId("sliderWeight");
        txtBodyFat.setId("txtBodyFat");
        txtTargetWeight.setId("txtTargetWeight");
        txtMeasurements.setId("txtMeasurements");
        populateTapeTimeline();
        // The segmented control is an HBox (no onAction property), so the horizon answer is
        // wired through its selection model; the handler stays the single entry point.
        segTimeHorizon.selectedIndexProperty().addListener((obs, oldV, newV) -> onHorizonChanged());
        // Dragging the slider drafts the weight reading in the open sheet.
        sliderWeight.valueProperty().addListener((obs, oldV, newV) ->
                txtWeight.setText(String.format(Locale.ROOT, "%.1f", newV.doubleValue())));
        onHorizonChanged();
    }

    /** Builds the whole node tree and mounts the overlay (the constructor's first step). */
    private void viewRootSetup() {
        mainLayout = new BorderPane();

        // Top app bar.
        topAppBar = new M3TopAppBar("BIOMECHANICAL PROGRESS & ANALYTICS",
                io.m3fx.controls.appbar.TopAppBarVariant.CENTER_ALIGNED);
        HBox.setHgrow(topAppBar, Priority.ALWAYS);
        btnBack = new M3IconButton(M3Icon.symbol("arrow_back"), IconButtonVariant.STANDARD);
        btnBack.setOnAction(e -> onNavigateBack());
        btnBack.setAccessibleText("Back to member roster");
        topAppBar.setNavigationIcon(btnBack);

        HBox appActions = new HBox(12);
        appActions.setAlignment(Pos.CENTER_RIGHT);
        appActions.setPadding(new Insets(0, 16, 0, 0));
        segTimeHorizon = new M3SegmentedButton(List.of("1M", "3M", "6M", "1Y", "ALL"));
        segTimeHorizon.setSelectedIndex(1);
        btnLogEntry = new M3Button("Log Entry", ButtonVariant.FILLED);
        btnLogEntry.setGraphic(M3Icon.symbol("stats"));
        btnLogEntry.setOnAction(e -> onOpenLogDialog());
        appActions.getChildren().addAll(segTimeHorizon, btnLogEntry);

        HBox appBarRow = new HBox();
        appBarRow.setAlignment(Pos.CENTER_LEFT);
        appBarRow.getStyleClass().add("app-bar-container");
        appBarRow.getChildren().addAll(topAppBar, appActions);
        mainLayout.setTop(appBarRow);

        // Center scrollable analytics workspace.
        VBox workspace = new VBox(20);
        workspace.setPadding(new Insets(20, 24, 64, 24));

        // Telemetry ribbon cards.
        HBox telemetryRibbon = new HBox(16);
        telemetryRibbon.setFillHeight(true);
        statCurrentWeight = new M3StatCard("CURRENT WEIGHT", "-- kg", "Target: 80.0 kg");
        statBodyFat = new M3StatCard("BODY FAT %", "-- %", "Optimal Range");
        statLeanMass = new M3StatCard("LEAN MUSCLE MASS", "-- kg", "86.1% of Total");
        statVbtMax = new M3StatCard("ESTIMATED 1RM (VBT)", "-- kg", "Back Squat (R²=0.98)");
        for (M3StatCard card : List.of(statCurrentWeight, statBodyFat, statLeanMass, statVbtMax)) {
            HBox.setHgrow(card, Priority.ALWAYS);
        }
        telemetryRibbon.getChildren().addAll(statCurrentWeight, statBodyFat, statLeanMass, statVbtMax);

        // Mid row: spline chart & heatmap + donut.
        HBox midRow = new HBox(20);

        // Dual spline biometrics card.
        M3Card splineCard = new M3Card(CardVariant.ELEVATED);
        HBox.setHgrow(splineCard, Priority.ALWAYS);
        VBox splineContent = new VBox(12);
        splineContent.setPadding(new Insets(16));
        HBox splineHeader = new HBox();
        splineHeader.setAlignment(Pos.CENTER_LEFT);
        Region splineSpacer = new Region();
        HBox.setHgrow(splineSpacer, Priority.ALWAYS);
        M3Badge dualSplineBadge = new M3Badge();
        dualSplineBadge.setText("DUAL SPLINE");
        splineHeader.getChildren().addAll(
                styledLabel("BIOMETRIC TRAJECTORY (WEIGHT & BODY FAT)", "title-small"),
                splineSpacer,
                dualSplineBadge);
        splineBiometrics = new M3SplineChart();
        splineBiometrics.setPrefHeight(320);
        splineContent.getChildren().addAll(splineHeader, splineBiometrics);
        splineCard.setContent(splineContent);

        // Training density and body composition column.
        VBox densityColumn = new VBox(16);
        densityColumn.setPrefWidth(420);
        densityColumn.setMinWidth(380);

        // 52-week training density card.
        M3Card densityCard = new M3Card(CardVariant.OUTLINED);
        VBox densityContent = new VBox(10);
        densityContent.setPadding(new Insets(14));
        HBox densityHeader = new HBox();
        densityHeader.setAlignment(Pos.CENTER_LEFT);
        Region densitySpacer = new Region();
        HBox.setHgrow(densitySpacer, Priority.ALWAYS);
        densityHeader.getChildren().addAll(
                styledLabel("52-WEEK TRAINING DENSITY", "title-small"),
                densitySpacer,
                styledLabel("2026 Year To Date", "label-small"));
        activityHeatmap = new M3ActivityCalendar();
        activityHeatmap.setPrefHeight(130);
        densityContent.getChildren().addAll(densityHeader, activityHeatmap);
        densityCard.setContent(densityContent);

        // Body composition donut card.
        M3Card compositionCard = new M3Card(CardVariant.OUTLINED);
        HBox compositionContent = new HBox(16);
        compositionContent.setAlignment(Pos.CENTER);
        compositionContent.setPadding(new Insets(12, 14, 12, 14));
        donutComposition = new M3DonutChart();
        donutComposition.setPrefSize(140, 140);
        VBox compositionLegend = new VBox(6);
        compositionLegend.setAlignment(Pos.CENTER_LEFT);
        compositionLegend.getChildren().addAll(
                styledLabel("BODY COMPOSITION", "title-small"),
                styledLabel("Lean Mass: 72.5 kg", "body-small"),
                styledLabel("Fat Mass: 11.7 kg", "body-small"),
                styledLabel("Water: 52.4 L", "body-small"));
        compositionContent.getChildren().addAll(donutComposition, compositionLegend);
        compositionCard.setContent(compositionContent);

        densityColumn.getChildren().addAll(densityCard, compositionCard);
        midRow.getChildren().addAll(splineCard, densityColumn);

        // Lower row: VBT scatter plot & timeline.
        HBox lowerRow = new HBox(20);

        // VBT load-velocity scatter plot card.
        M3Card vbtCard = new M3Card(CardVariant.ELEVATED);
        HBox.setHgrow(vbtCard, Priority.ALWAYS);
        VBox vbtContent = new VBox(12);
        vbtContent.setPadding(new Insets(16));
        HBox vbtHeader = new HBox();
        vbtHeader.setAlignment(Pos.CENTER_LEFT);
        Region vbtSpacer = new Region();
        HBox.setHgrow(vbtSpacer, Priority.ALWAYS);
        lblRegressionStats = new Label("v = -0.0094L + 1.71 | R² = 0.982");
        lblRegressionStats.getStyleClass().add("label-small");
        vbtHeader.getChildren().addAll(
                styledLabel("VELOCITY-BASED TRAINING (VBT) LOAD-VELOCITY PROFILE", "title-small"),
                vbtSpacer,
                lblRegressionStats);
        scatterVbt = new M3ScatterPlot();
        scatterVbt.setPrefHeight(280);
        vbtContent.getChildren().addAll(vbtHeader, scatterVbt);
        vbtCard.setContent(vbtContent);

        // Anthropometric tape timeline card.
        M3Card tapeCard = new M3Card(CardVariant.OUTLINED);
        tapeCard.setPrefWidth(420);
        tapeCard.setMinWidth(380);
        VBox tapeContent = new VBox(12);
        tapeContent.setPadding(new Insets(16));
        tapeContent.getChildren().add(styledLabel("ANTHROPOMETRIC TAPE TIMELINE", "title-small"));
        timelineAnthropometrics = new M3Timeline();
        timelineAnthropometrics.setPrefHeight(260);
        tapeContent.getChildren().add(timelineAnthropometrics);
        tapeCard.setContent(tapeContent);

        lowerRow.getChildren().addAll(vbtCard, tapeCard);

        workspace.getChildren().addAll(telemetryRibbon, midRow, lowerRow);
        ScrollPane workspaceScroll = new ScrollPane(workspace);
        workspaceScroll.setFitToWidth(true);
        workspaceScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        workspaceScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        mainLayout.setCenter(workspaceScroll);

        // Celebration confetti particle canvas.
        celebrationCanvas = new M3ParticleCanvas();
        celebrationCanvas.setVisible(false);
        celebrationCanvas.setMouseTransparent(true);

        // Bottom sheet for logging biometric entries.
        sheetLogEntry = new M3BottomSheet();
        sheetLogEntry.setVisible(false);
        StackPane.setAlignment(sheetLogEntry, Pos.BOTTOM_CENTER);
        VBox logContent = new VBox(16);
        logContent.setPrefWidth(480);
        logContent.setPadding(new Insets(20, 24, 24, 24));
        HBox logHeader = new HBox();
        logHeader.setAlignment(Pos.CENTER_LEFT);
        M3IconButton closeLogBtn = new M3IconButton(M3Icon.symbol("close"), IconButtonVariant.STANDARD);
        closeLogBtn.setOnAction(e -> onCloseLogSheet());
        closeLogBtn.setAccessibleText("Close log entry sheet");
        Region logSpacer = new Region();
        HBox.setHgrow(logSpacer, Priority.ALWAYS);
        logHeader.getChildren().addAll(
                styledLabel("LOG BIOMETRIC TELEMETRY", "title-medium"),
                logSpacer,
                closeLogBtn);
        txtEntryDate = outlinedField("Entry Date", "YYYY-MM-DD");
        txtWeight = outlinedField("Weight (kg)", "e.g., 84.2");
        sliderWeight = new M3Slider(40, 160, 84.2);
        sliderWeight.setAccessibleLabel("Weight in kilograms");
        txtBodyFat = outlinedField("Body Fat (%)", "e.g., 13.8");
        txtTargetWeight = outlinedField("Target Goal Weight (kg)", "e.g., 80.0");
        txtMeasurements = outlinedField("Circumferences (JSON/Notes)", "Chest, Waist, Arms");
        M3Button saveEntryBtn = new M3Button("Save Telemetry Entry", ButtonVariant.FILLED);
        saveEntryBtn.setMaxWidth(Double.MAX_VALUE);
        saveEntryBtn.setOnAction(e -> onSaveRecord());
        logContent.getChildren().addAll(logHeader, txtEntryDate, txtWeight, sliderWeight,
                txtBodyFat, txtTargetWeight, txtMeasurements, saveEntryBtn);
        sheetLogEntry.setContent(logContent);

        rootPane = new StackPane();
        rootPane.getStyleClass().add("member-progress-root");
        rootPane.getChildren().addAll(mainLayout, celebrationCanvas, sheetLogEntry, overlay);
    }

    /** One M3TextField with an outlined variant, label and supporting text. */
    private static M3TextField outlinedField(String labelText, String supportingText) {
        M3TextField field = new M3TextField(labelText, TextFieldVariant.OUTLINED);
        field.setSupportingText(supportingText);
        return field;
    }

    /** Small label with one style class. */
    private static Label styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    @Override
    public Parent getRoot() {
        return rootPane;
    }

    // ------------------------------------------------------------------
    // Event handlers (answers for every visible action)
    // ------------------------------------------------------------------

    /** Returns to the member roster via the shell rail; answers with a hint if standalone. */
    public void onNavigateBack() {
        navigateViaShellRail("navCustomers",
                "Open the member roster from the navigation rail (this preview is standalone).");
    }

    /** Reslices the trajectory charts to the selected horizon (the visible answer). */
    public void onHorizonChanged() {
        if (weightSeries == null || bodyFatSeries == null) {
            return;
        }
        int index = segTimeHorizon.getSelectedIndex();
        int points = (index >= 0 && index < HORIZON_POINTS.length)
                ? HORIZON_POINTS[index] : WEEKLY_SAMPLES;
        applyHorizon(points);
    }

    /** Opens the log-entry bottom sheet as a true modal (scrim, focus, drag-to-dismiss). */
    public void onOpenLogDialog() {
        sheetLogEntry.setVisible(true);
        overlay.showSheet(sheetLogEntry);
    }

    /** Closes the log-entry bottom sheet (also reachable via Escape, scrim and drag). */
    public void onCloseLogSheet() {
        sheetLogEntry.hide();
    }

    /**
     * Saves the telemetry entry: field errors name the problem, success updates the KPIs,
     * appends the reading to the trajectory chart and confirms via snackbar.
     */
    public void onSaveRecord() {
        Double weight = parseDouble(txtWeight, 30.0, 300.0);
        Double bodyFat = txtBodyFat.getText().isBlank()
                ? Double.valueOf(0.0) : parseDouble(txtBodyFat, 0.0, 70.0);
        boolean valid = weight != null && bodyFat != null;
        txtWeight.setError(weight == null);
        txtBodyFat.setError(bodyFat == null);
        if (!valid) {
            overlay.showSnackbar("Fix the highlighted fields, then save the entry.");
            return;
        }
        // Reflect the entry across the screen: slider, KPI, lean-mass math, chart, timeline.
        sliderWeight.setValue(weight);
        statCurrentWeight.setValue(String.format("%.1f kg", weight));
        statCurrentWeight.setDeltaPositive(weight <= 80.0);
        if (bodyFat > 0) {
            statLeanMass.setValue(String.format(Locale.ROOT, "%.1f kg", weight * (1 - bodyFat / 100.0)));
        }
        appendReading(weight, bodyFat);
        boolean goalReached = weight <= 80.0;
        overlay.showSnackbar(goalReached
                ? String.format(Locale.ROOT, "Entry saved — %.1f kg hits the 80.0 kg goal!", weight)
                : String.format(Locale.ROOT, "Entry saved — %.1f kg at %.1f%% body fat.", weight, bodyFat));
        onCloseLogSheet();
    }

    // ------------------------------------------------------------------
    // Demo telemetry
    // ------------------------------------------------------------------

    private void prefillTelemetry() {
        statCurrentWeight.setValue("84.2 kg");
        statBodyFat.setValue("13.8 %");
        statLeanMass.setValue("72.5 kg");
        statVbtMax.setValue("142.5 kg");
        txtEntryDate.setText(LocalDate.now().toString());
        txtTargetWeight.setText("80.0");
    }

    /** Weekly weight/body-fat trajectory trending toward the target. */
    private void populateTrajectoryChart() {
        weightSeries = new M3DataSeries("Weight (kg)");
        bodyFatSeries = new M3DataSeries("Body fat (%)");
        for (int week = 1; week <= WEEKLY_SAMPLES; week++) {
            double progress = week / (double) WEEKLY_SAMPLES;
            double weight = 86.4 - 2.2 * progress + weeklyNoise(week, 0.12);
            double bodyFat = 15.2 - 1.4 * progress + weeklyNoise(week * 7, 0.08);
            weightSeries.getData().add(new M3DataPoint("W" + week, round1(weight)));
            bodyFatSeries.getData().add(new M3DataPoint("W" + week, round1(bodyFat)));
        }
    }

    private double weeklyNoise(int seed, double amplitude) {
        return Math.sin(seed * 12.9898) * amplitude;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    /** Shows only the tail of each series for the chosen horizon. */
    private void applyHorizon(int points) {
        splineBiometrics.getSeriesList().setAll(
                slice(weightSeries, points), slice(bodyFatSeries, points));
    }

    private M3DataSeries slice(M3DataSeries source, int points) {
        int from = Math.max(0, source.getData().size() - points);
        M3DataSeries sliced = new M3DataSeries(source.getName());
        sliced.setColor(source.getColor());
        sliced.getData().addAll(source.getData().subList(from, source.getData().size()));
        return sliced;
    }

    /** Deterministic 2026 training density up to today (184 planned sessions). */
    private void populateActivityHeatmap() {
        LocalDate today = LocalDate.of(2026, 10, 29);
        LocalDate day = LocalDate.of(2026, 1, 1);
        while (!day.isAfter(today)) {
            int dayOfYear = day.getDayOfYear();
            int count = dayOfYear % 7 < 4 ? 1 + dayOfYear % 2 : 0;
            if (count > 0) {
                activityHeatmap.setActivity(day, count);
            }
            day = day.plusDays(1);
        }
    }

    private void populateCompositionDonut() {
        donutComposition.setCenterHeadline("84.2 kg");
        donutComposition.setCenterSubtitle("Total mass");
        donutComposition.addRing("Lean mass", 72.5, 84.2, Color.web("#4C8C4A"));
        donutComposition.addRing("Fat mass", 11.7, 84.2, Color.web("#C2543B"));
    }

    /** Load-velocity samples around the regression v = -0.0094L + 1.71. */
    private void populateVbtScatter() {
        M3ScatterSeries loads = new M3ScatterSeries("Back squat sets", Color.web("#7C4DBE"));
        double[] barLoads = {40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170};
        for (int i = 0; i < barLoads.length; i++) {
            double load = barLoads[i];
            double velocity = Math.max(0.1, 1.71 - 0.0094 * load + weeklyNoise(i * 3 + 1, 0.04));
            loads.getPoints().add(new M3ScatterPoint(load, round2(velocity)));
        }
        scatterVbt.getSeriesList().setAll(loads);
        scatterVbt.setShowTrendline(true);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void populateTapeTimeline() {
        timelineAnthropometrics.addItem("2026-01-05", "Baseline tape",
                "Weight 86.4 kg · Body fat 15.2% · Waist 84 cm", M3Timeline.Status.COMPLETED);
        timelineAnthropometrics.addItem("2026-04-18", "Mid-cycle check",
                "Weight 85.1 kg · Body fat 14.4% · Waist 82 cm", M3Timeline.Status.COMPLETED);
        timelineAnthropometrics.addItem("2026-08-02", "Strength block end",
                "Weight 84.6 kg · Body fat 14.0% · Waist 81 cm", M3Timeline.Status.COMPLETED);
        timelineAnthropometrics.addItem("2026-10-29", "Current reading",
                "Weight 84.2 kg · Body fat 13.8% · Waist 80 cm", M3Timeline.Status.ACTIVE);
    }

    /** Appends the saved reading as the newest weekly sample (visible in every horizon). */
    private void appendReading(double weight, double bodyFat) {
        int next = weightSeries.getData().size() + 1;
        weightSeries.getData().add(new M3DataPoint("W" + next, round1(weight)));
        bodyFatSeries.getData().add(new M3DataPoint("W" + next,
                bodyFat > 0 ? round1(bodyFat)
                        : bodyFatSeries.getData().get(bodyFatSeries.getData().size() - 1).getValue()));
        onHorizonChanged();
        if (LocalDate.now().toString().equals(txtEntryDate.getText())) {
            timelineAnthropometrics.addItem(txtEntryDate.getText(), "Logged today",
                    String.format(Locale.ROOT, "Weight %.1f kg%s", weight,
                            bodyFat > 0 ? String.format(Locale.ROOT, " · Body fat %.1f%%", bodyFat) : ""),
                    M3Timeline.Status.ACTIVE);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Double parseDouble(M3TextField field, double min, double max) {
        try {
            double value = Double.parseDouble(field.getText().trim());
            return (value >= min && value <= max) ? Double.valueOf(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Navigates by firing the shell's own pinned rail button (same pathway as clicking the
     * rail). Outside the shell — standalone previews and tests — it answers with a
     * snackbar instead of pretending to navigate.
     */
    private void navigateViaShellRail(String railButtonId, String fallbackText) {
        Node railButton = findInScene(railButtonId);
        if (railButton instanceof ButtonBase button && !button.isDisabled()) {
            button.fire();
            return;
        }
        overlay.showSnackbar(fallbackText);
    }

    private Node findInScene(String id) {
        if (rootPane.getScene() == null) {
            return null;
        }
        Parent sceneRoot = rootPane.getScene().getRoot();
        return sceneRoot == null ? null : findById(sceneRoot, id);
    }

    /** Id-based tree search (no CSS dependency, unlike Node.lookup). */
    private Node findById(Parent parent, String id) {
        if (id.equals(parent.getId())) {
            return parent;
        }
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (id.equals(child.getId())) {
                return child;
            }
            if (child instanceof Parent childParent) {
                Node found = findById(childParent, id);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
