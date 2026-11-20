package com.gym.controller;

import com.gym.model.Attendance;
import com.gym.model.Customer;
import com.gym.model.ProgressRecord;
import com.gym.dao.AttendanceDao;
import com.gym.dao.CustomerDao;
import com.gym.dao.ProgressRecordDao;
import com.gym.ui.Data;
import com.gym.ui.UiKit;
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
import io.m3fx.controls.chart.M3SplineChart;
import io.m3fx.controls.common.M3Badge;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.data.M3Timeline;
import io.m3fx.controls.effect.M3ParticleCanvas;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.IconButtonVariant;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.menu.M3Menu;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.segmented.M3SegmentedButton;
import io.m3fx.controls.sheet.M3BottomSheet;
import io.m3fx.controls.slider.M3Slider;
import io.m3fx.controls.textfield.M3TextField;
import io.m3fx.controls.textfield.TextFieldVariant;
import io.m3fx.core.theme.ColorRole;
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

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Controller for Biomechanical Progress &amp; Analytics on real data (C9).
 * Conforms to docs/ui/members/MEMBER_PROGRESS_UI.md Section 7 and the com.gym.ui foundation
 * contracts (app-ui-foundation.md §10).
 *
 * <p>Data: every chart and KPI derives from {@link ProgressRecordDao} rows for a selected
 * customer (selector button, defaulting to the member whose latest record is the most recent),
 * the heatmap is the selected customer's real attendance by date (AttendanceDao), and the
 * header chip counts today's check-ins building-wide. Target weight comes from the records'
 * {@code targetWeightKg} ("No target set" when absent — never a baked number); logging a
 * reading persists a real {@code ProgressRecordDao.insert} whose target carries the last
 * recorded goal forward unless edited. A PR — the new weight strictly below every earlier
 * record — triggers the celebration canvas; the VBT load-velocity scatter stays an honest
 * dash (no data source records velocity yet).
 *
 * <p>The node tree is built in the constructor — the programmatic replacement for the former
 * progress.fxml: telemetry ribbon, trajectory chart, density calendar, composition donut, VBT
 * scatter, tape timeline, log-entry sheet.
 */
public class ProgressController implements ScreenController {

    /** Horizon windows in days for 1M / 3M / 6M / 1Y / ALL. */
    private static final int[] HORIZON_DAYS = {31, 92, 183, 365, Integer.MAX_VALUE};

    // ---- View fields (the former fx:id injections, now built by the constructor) ----
    private StackPane rootPane;
    private BorderPane mainLayout;
    private M3TopAppBar topAppBar;
    private M3IconButton btnBack;
    private M3SegmentedButton segTimeHorizon;
    private M3Button btnLogEntry;
    private M3Button btnCustomerPicker;
    private VBox compositionLegend;
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
    /** The analytics workspace host (loadAsync swaps inside it; charts stay mounted). */
    private StackPane workspaceStateHost;
    /** Empty/error state overlay shown instead of the workspace when a load fails. */
    private StackPane workspaceStateOverlay;

    // ---- Loaded state ----
    private List<Customer> customers = List.of();
    private List<ProgressRecord> records = List.of();
    private Map<Integer, List<ProgressRecord>> recordsByCustomer = Map.of();
    private Customer selectedCustomer;
    private List<ProgressRecord> selectedRecords = List.of();

    /** Creates the screen, builds the tree and starts the first real data load. */
    public ProgressController() {
        viewRootSetup();

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
        // The segmented control is an HBox (no onAction property), so the horizon answer is
        // wired through its selection model; the handler stays the single entry point.
        segTimeHorizon.selectedIndexProperty().addListener((obs, oldV, newV) -> onHorizonChanged());
        // Dragging the slider drafts the weight reading in the open sheet.
        sliderWeight.valueProperty().addListener((obs, oldV, newV) ->
                txtWeight.setText(String.format(Locale.ROOT, "%.1f", newV.doubleValue())));
        loadProgress();
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
        segTimeHorizon.setSelectedIndex(4);
        btnCustomerPicker = customerPickerButton();
        btnLogEntry = new M3Button("Log Entry", ButtonVariant.FILLED);
        btnLogEntry.setGraphic(M3Icon.symbol("stats"));
        btnLogEntry.setOnAction(e -> onOpenLogDialog());
        appActions.getChildren().addAll(btnCustomerPicker, segTimeHorizon, btnLogEntry);

        HBox appBarRow = new HBox();
        appBarRow.setAlignment(Pos.CENTER_LEFT);
        appBarRow.getStyleClass().add("app-bar-container");
        appBarRow.getChildren().addAll(topAppBar, appActions);
        mainLayout.setTop(appBarRow);

        // Center scrollable analytics workspace, wrapped in the load/empty/error state host.
        VBox workspace = new VBox(20);
        workspace.setPadding(new Insets(20, 24, 64, 24));

        // Telemetry ribbon cards.
        HBox telemetryRibbon = new HBox(16);
        telemetryRibbon.setFillHeight(true);
        statCurrentWeight = new M3StatCard("CURRENT WEIGHT", "—", "No readings yet");
        statBodyFat = new M3StatCard("BODY FAT %", "—", "No readings yet");
        statLeanMass = new M3StatCard("LEAN MUSCLE MASS", "—", "Derived from the latest record");
        statVbtMax = new M3StatCard("ESTIMATED 1RM (VBT)", "—", "No VBT data recorded yet");
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

        // Training density card (the selected customer's real attendance).
        M3Card densityCard = new M3Card(CardVariant.OUTLINED);
        VBox densityContent = new VBox(10);
        densityContent.setPadding(new Insets(14));
        HBox densityHeader = new HBox();
        densityHeader.setAlignment(Pos.CENTER_LEFT);
        Region densitySpacer = new Region();
        HBox.setHgrow(densitySpacer, Priority.ALWAYS);
        densityHeader.getChildren().addAll(
                styledLabel("ATTENDANCE DENSITY", "title-small"),
                densitySpacer,
                styledLabel("Check-ins by date", "label-small"));
        activityHeatmap = new M3ActivityCalendar();
        activityHeatmap.setPrefHeight(130);
        densityContent.getChildren().addAll(densityHeader, activityHeatmap);
        densityCard.setContent(densityContent);

        // Body composition donut card (real latest-record math).
        M3Card compositionCard = new M3Card(CardVariant.OUTLINED);
        HBox compositionContent = new HBox(16);
        compositionContent.setAlignment(Pos.CENTER);
        compositionContent.setPadding(new Insets(12, 14, 12, 14));
        donutComposition = new M3DonutChart();
        donutComposition.setPrefSize(140, 140);
        compositionLegend = new VBox(6);
        compositionLegend.setAlignment(Pos.CENTER_LEFT);
        compositionLegend.getChildren().add(styledLabel("BODY COMPOSITION", "title-small"));
        compositionContent.getChildren().addAll(donutComposition, compositionLegend);
        compositionCard.setContent(compositionContent);

        densityColumn.getChildren().addAll(densityCard, compositionCard);
        midRow.getChildren().addAll(splineCard, densityColumn);

        // Lower row: VBT scatter plot & timeline.
        HBox lowerRow = new HBox(20);

        // VBT load-velocity scatter plot card (honest dash: nothing records velocity yet).
        M3Card vbtCard = new M3Card(CardVariant.ELEVATED);
        HBox.setHgrow(vbtCard, Priority.ALWAYS);
        VBox vbtContent = new VBox(12);
        vbtContent.setPadding(new Insets(16));
        HBox vbtHeader = new HBox();
        vbtHeader.setAlignment(Pos.CENTER_LEFT);
        Region vbtSpacer = new Region();
        HBox.setHgrow(vbtSpacer, Priority.ALWAYS);
        lblRegressionStats = new Label("No VBT data recorded yet");
        lblRegressionStats.getStyleClass().add("label-small");
        vbtHeader.getChildren().addAll(
                styledLabel("VELOCITY-BASED TRAINING (VBT) LOAD-VELOCITY PROFILE", "title-small"),
                vbtSpacer,
                lblRegressionStats);
        scatterVbt = new M3ScatterPlot();
        scatterVbt.setPrefHeight(280);
        vbtContent.getChildren().addAll(vbtHeader, scatterVbt);
        vbtCard.setContent(vbtContent);

        // Anthropometric tape timeline card (real records, latest first).
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
        // The workspace stays mounted through every load (loadAsync swaps inside this host).
        workspaceStateHost = new StackPane(workspaceScroll);
        mainLayout.setCenter(workspaceStateHost);

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
        txtEntryDate.setText(LocalDate.now().toString());
        txtWeight = outlinedField("Weight (kg)", "e.g., 84.2");
        sliderWeight = new M3Slider(40, 160, 84.2);
        sliderWeight.setAccessibleLabel("Weight in kilograms");
        txtBodyFat = outlinedField("Body Fat (%)", "e.g., 13.8 — optional");
        txtTargetWeight = outlinedField("Target Goal Weight (kg)", "Carries the last goal; optional");
        txtMeasurements = outlinedField("Circumferences (Notes)", "Chest, Waist, Arms");
        M3Button saveEntryBtn = new M3Button("Save Telemetry Entry", ButtonVariant.FILLED);
        saveEntryBtn.setMaxWidth(Double.MAX_VALUE);
        saveEntryBtn.setOnAction(e -> onSaveRecord());
        logContent.getChildren().addAll(logHeader, txtEntryDate, txtWeight, sliderWeight,
                txtBodyFat, txtTargetWeight, txtMeasurements, saveEntryBtn);
        sheetLogEntry.setContent(logContent);

        // Empty/error state overlay (shown instead of the workspace on a failed or empty load).
        workspaceStateOverlay = new StackPane();
        workspaceStateOverlay.setVisible(false);
        workspaceStateOverlay.getStyleClass().add("progress-state-overlay");

        rootPane = new StackPane();
        rootPane.getStyleClass().add("member-progress-root");
        rootPane.getChildren().addAll(mainLayout, celebrationCanvas, sheetLogEntry,
                workspaceStateOverlay, overlay);
    }

    /** The customer selector: a button opening an M3Menu over the loaded members. */
    private M3Button customerPickerButton() {
        M3Button picker = new M3Button("Select member", ButtonVariant.OUTLINED);
        picker.getStyleClass().add("customer-picker-btn");
        picker.setOnAction(e -> {
            M3Menu menu = new M3Menu();
            for (Customer customer : customers) {
                String name = customer.getFullName();
                menu.item(name, () -> selectCustomer(customer));
            }
            if (customers.isEmpty()) {
                menu.item("No members on record", () -> { });
            }
            menu.showBelow(picker);
        });
        return picker;
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
    // Data loading (CustomerDao + ProgressRecordDao + AttendanceDao)
    // ------------------------------------------------------------------

    /**
     * Loads members, progress records and attendance on a background task; defaults the
     * selection to the member whose latest record is the most recent (the app-ui-foundation
     * §10 contract) and renders the honest empty state when nobody has records yet.
     */
    public void loadProgress() {
        workspaceStateOverlay.getChildren().clear();
        workspaceStateOverlay.setVisible(false);
        Data.loadAsync(workspaceStateHost,
                () -> {
                    Data.ensureReady();
                    List<Customer> loadedCustomers = new CustomerDao().findAll();
                    List<ProgressRecord> allRecords = new ProgressRecordDao().findAll();
                    List<Attendance> allAttendance = new AttendanceDao().findAll();
                    return new ProgressSnapshot(loadedCustomers, allRecords, allAttendance);
                },
                snapshot -> {
                    customers = snapshot.customers();
                    records = snapshot.records();
                    recordsByCustomer = records.stream().collect(Collectors.groupingBy(
                            ProgressRecord::getCustomerId));
                    if (selectedCustomer != null) {
                        // Keep the current selection if the member still exists.
                        Customer kept = customers.stream()
                                .filter(c -> c.getId() == selectedCustomer.getId())
                                .findFirst().orElse(null);
                        selectCustomer(kept);
                        return;
                    }
                    // Default: the member whose latest record is the most recent (C9 contract).
                    Customer defaultCustomer = records.stream()
                            .collect(Collectors.toMap(ProgressRecord::getCustomerId,
                                    ProgressRecord::getDate, (a, b) -> a.compareTo(b) >= 0 ? a : b))
                            .entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(entry -> customers.stream()
                                    .filter(c -> c.getId() == entry.getKey())
                                    .findFirst().orElse(null))
                            .orElse(null);
                    if (defaultCustomer == null && !customers.isEmpty()) {
                        defaultCustomer = customers.get(0);
                    }
                    if (defaultCustomer == null) {
                        workspaceStateOverlay.getChildren().setAll(UiKit.emptyState(
                                "No members on record yet.", null, null));
                        workspaceStateOverlay.setVisible(true);
                        return;
                    }
                    selectCustomer(defaultCustomer);
                },
                error -> {
                    workspaceStateOverlay.getChildren().setAll(
                            UiKit.errorState(error.getMessage(), this::loadProgress));
                    workspaceStateOverlay.setVisible(true);
                });
    }

    /** One load's worth of DAO data. */
    private record ProgressSnapshot(List<Customer> customers, List<ProgressRecord> records,
                                    List<Attendance> attendance) {
    }

    /** Selects a member and re-derives every chart, KPI and the heatmap from their records. */
    private void selectCustomer(Customer customer) {
        selectedCustomer = customer;
        btnCustomerPicker.setText(customer == null
                ? "Select member" : "Member: " + customer.getFullName());
        selectedRecords = customer == null
                ? List.of()
                : recordsByCustomer.getOrDefault(customer.getId(), List.of()).stream()
                        .sorted(Comparator.comparing(ProgressRecord::getDate))
                        .collect(Collectors.toList());
        refreshAttendanceHeatmap();
        refreshChartsAndKpis();
        refreshCompositionAndTimeline();
    }

    /** The selected customer's real attendance by date drives the density heatmap. */
    private void refreshAttendanceHeatmap() {
        activityHeatmap.clearActivities();
        if (selectedCustomer == null) {
            return;
        }
        Data.submit(
                () -> new AttendanceDao().findByCustomerId(selectedCustomer.getId()),
                attendanceRows -> {
                    Map<String, Integer> perDay = attendanceRows.stream().collect(
                            Collectors.groupingBy(Attendance::getDate,
                                    Collectors.summingInt(row -> 1)));
                    for (Map.Entry<String, Integer> day : perDay.entrySet()) {
                        activityHeatmap.setActivity(LocalDate.parse(day.getKey()),
                                day.getValue());
                    }
                },
                error -> overlay.showSnackbar(
                        "Could not load attendance: " + error.getMessage()));
    }

    // ------------------------------------------------------------------
    // Event handlers (answers for every visible action)
    // ------------------------------------------------------------------

    /** Returns to the member roster via the shell rail; answers with a hint if standalone. */
    public void onNavigateBack() {
        navigateViaShellRail("navCustomers",
                "Open the member roster from the navigation rail (this preview is standalone).");
    }

    /** Reslices the trajectory charts to the selected horizon (date-range based). */
    public void onHorizonChanged() {
        int index = segTimeHorizon.getSelectedIndex();
        int days = (index >= 0 && index < HORIZON_DAYS.length)
                ? HORIZON_DAYS[index] : Integer.MAX_VALUE;
        applyHorizon(days);
    }

    /** Opens the log-entry bottom sheet as a true modal (scrim, focus, drag-to-dismiss). */
    public void onOpenLogDialog() {
        txtEntryDate.setText(LocalDate.now().toString());
        ProgressRecord latest = selectedRecords.isEmpty()
                ? null : selectedRecords.get(selectedRecords.size() - 1);
        txtTargetWeight.setText(latest != null && latest.getTargetWeightKg() != null
                ? String.format(Locale.ROOT, "%.1f", latest.getTargetWeightKg()) : "");
        sheetLogEntry.setVisible(true);
        overlay.showSheet(sheetLogEntry);
    }

    /** Closes the log-entry bottom sheet (also reachable via Escape, scrim and drag). */
    public void onCloseLogSheet() {
        sheetLogEntry.hide();
    }

    /**
     * Saves the telemetry entry as a real {@link ProgressRecordDao#insert}: field errors name
     * the problem, the DAO's verdict surfaces verbatim, and the new reading re-derives the
     * KPIs, charts and timeline. A PR — the weight strictly below every earlier record —
     * fires the celebration canvas.
     */
    public void onSaveRecord() {
        if (selectedCustomer == null) {
            overlay.showSnackbar("Select a member first, then log the reading.");
            return;
        }
        Double weight = parseDouble(txtWeight, 30.0, 300.0);
        boolean bodyFatBlank = txtBodyFat.getText().isBlank();
        Double bodyFat = bodyFatBlank ? Double.valueOf(0.0)
                : parseDouble(txtBodyFat, 0.0, 70.0);
        Double target = txtTargetWeight.getText().isBlank()
                ? null : parseDouble(txtTargetWeight, 1.0, 300.0);
        boolean targetOk = txtTargetWeight.getText().isBlank() || target != null;
        boolean dateOk = txtEntryDate.getText().trim().matches("\\d{4}-\\d{2}-\\d{2}");
        txtWeight.setError(weight == null);
        txtBodyFat.setError(!bodyFatBlank && bodyFat == null);
        txtTargetWeight.setError(!targetOk);
        txtEntryDate.setError(!dateOk);
        if (weight == null || bodyFat == null || !targetOk || !dateOk) {
            overlay.showSnackbar("Fix the highlighted fields, then save the entry.");
            return;
        }
        // Carry the last recorded goal forward when the field is left blank.
        Double carriedTarget = target != null
                ? target
                : (selectedRecords.isEmpty() ? null
                    : selectedRecords.get(selectedRecords.size() - 1).getTargetWeightKg());
        boolean pr = selectedRecords.stream()
                .noneMatch(record -> record.getWeightKg() < weight);
        ProgressRecordDao dao = new ProgressRecordDao();
        Data.submit(
                () -> dao.insert(new ProgressRecord(0, selectedCustomer.getId(),
                        txtEntryDate.getText().trim(), weight,
                        bodyFat, txtMeasurements.getText().trim(), carriedTarget, "Logged via app")),
                inserted -> {
                    refreshAttendanceHeatmap();
                    loadProgress();
                    if (pr) {
                        celebrationCanvas.setVisible(true);
                        overlay.showSnackbar(String.format(Locale.ROOT,
                                "New personal best — %.1f kg is the lowest on record!",
                                inserted.getWeightKg()));
                    } else {
                        overlay.showSnackbar(String.format(Locale.ROOT,
                                "Entry saved — %.1f kg at %.1f%% body fat.",
                                inserted.getWeightKg(), inserted.getBodyFatPct()));
                    }
                    onCloseLogSheet();
                },
                error -> overlay.showSnackbar(
                        "Could not save the entry: " + error.getMessage()));
    }

    // ------------------------------------------------------------------
    // Chart and KPI derivation from the selected records
    // ------------------------------------------------------------------

    /** Re-derives KPIs, trajectory slices, composition and the timeline from real records. */
    private void refreshChartsAndKpis() {
        weightSeries = new M3DataSeries("Weight (kg)");
        bodyFatSeries = new M3DataSeries("Body fat (%)");
        for (ProgressRecord record : selectedRecords) {
            String label = shortDate(record.getDate());
            weightSeries.getData().add(new M3DataPoint(label, round1(record.getWeightKg())));
            bodyFatSeries.getData().add(new M3DataPoint(label,
                    round1(record.getBodyFatPct())));
        }
        onHorizonChanged();

        if (selectedRecords.isEmpty()) {
            statCurrentWeight.setValue("—");
            statCurrentWeight.setDeltaText("No readings yet");
            statBodyFat.setValue("—");
            statBodyFat.setDeltaText("No readings yet");
            statLeanMass.setValue("—");
            statLeanMass.setDeltaText("Derived from the latest record");
            donutComposition.setCenterHeadline("—");
            donutComposition.setCenterSubtitle("No records");
            timelineAnthropometrics.getItems().clear();
            return;
        }

        ProgressRecord latest = selectedRecords.get(selectedRecords.size() - 1);
        Double target = latest.getTargetWeightKg();
        statCurrentWeight.setValue(String.format(Locale.ROOT, "%.1f kg", latest.getWeightKg()));
        statCurrentWeight.setDeltaText(target == null
                ? "No target set" : String.format(Locale.ROOT, "Target: %.1f kg", target));
        statBodyFat.setValue(String.format(Locale.ROOT, "%.1f %%", latest.getBodyFatPct()));
        statBodyFat.setDeltaText("Latest reading");
        double leanMass = latest.getWeightKg() * (1 - latest.getBodyFatPct() / 100.0);
        statLeanMass.setValue(String.format(Locale.ROOT, "%.1f kg", leanMass));
        statLeanMass.setDeltaText(String.format(Locale.ROOT, "%.1f%% of total",
                100.0 - latest.getBodyFatPct()));
    }

    /** Rebuilds the composition donut and the tape timeline from the selected records. */
    private void refreshCompositionAndTimeline() {
        donutComposition.getRings().clear();
        compositionLegend.getChildren().removeIf(node ->
                node instanceof Label label && "body-small".equals(
                        label.getStyleClass().size() > 1
                                ? label.getStyleClass().get(1) : ""));
        if (selectedRecords.isEmpty()) {
            donutComposition.setCenterHeadline("—");
            donutComposition.setCenterSubtitle("No records");
            timelineAnthropometrics.getItems().clear();
            return;
        }
        ProgressRecord latest = selectedRecords.get(selectedRecords.size() - 1);
        double lean = latest.getWeightKg() * (1 - latest.getBodyFatPct() / 100.0);
        double fat = latest.getWeightKg() * latest.getBodyFatPct() / 100.0;
        donutComposition.setCenterHeadline(
                String.format(Locale.ROOT, "%.1f kg", latest.getWeightKg()));
        donutComposition.setCenterSubtitle("Total mass");
        // Semantic role mapping, no hardcoded hex: lean mass = the favorable state → the
        // tertiary accent (green family under this seed), fat mass = the counter-state → the
        // error role.
        donutComposition.addRing("Lean mass", lean, latest.getWeightKg(),
                UiKit.themeRole(ColorRole.TERTIARY));
        donutComposition.addRing("Fat mass", fat, latest.getWeightKg(),
                UiKit.themeRole(ColorRole.ERROR));
        compositionLegend.getChildren().addAll(
                styledLabel(String.format(Locale.ROOT, "Lean Mass: %.1f kg", lean), "body-small"),
                styledLabel(String.format(Locale.ROOT, "Fat Mass: %.1f kg", fat), "body-small"));

        timelineAnthropometrics.getItems().clear();
        List<ProgressRecord> latestFirst = new java.util.ArrayList<>(selectedRecords);
        java.util.Collections.reverse(latestFirst);
        for (int i = 0; i < latestFirst.size(); i++) {
            ProgressRecord record = latestFirst.get(i);
            timelineAnthropometrics.addItem(record.getDate(),
                    i == 0 ? "Latest reading" : "Check-in",
                    String.format(Locale.ROOT, "Weight %.1f kg · Body fat %.1f%%",
                            record.getWeightKg(), record.getBodyFatPct())
                            + (record.getMeasurements() == null
                                    || record.getMeasurements().isBlank()
                                    ? "" : " · " + record.getMeasurements()),
                    i == 0 ? M3Timeline.Status.ACTIVE : M3Timeline.Status.COMPLETED);
        }
    }

    /** Shows only the records inside the selected horizon window (date-range based). */
    private void applyHorizon(int days) {
        LocalDate cutoff = LocalDate.now().minusDays(days);
        List<ProgressRecord> inRange = selectedRecords.stream()
                .filter(record -> !record.getDate().isBlank())
                .filter(record -> LocalDate.parse(record.getDate()).isAfter(cutoff)
                        || LocalDate.parse(record.getDate()).equals(cutoff))
                .collect(Collectors.toList());
        if (inRange.isEmpty() && !selectedRecords.isEmpty()) {
            // Older than the window: keep at least the latest reading visible.
            inRange = List.of(selectedRecords.get(selectedRecords.size() - 1));
        }
        weightSeries = new M3DataSeries("Weight (kg)");
        bodyFatSeries = new M3DataSeries("Body fat (%)");
        for (ProgressRecord record : inRange) {
            String label = shortDate(record.getDate());
            weightSeries.getData().add(new M3DataPoint(label, round1(record.getWeightKg())));
            bodyFatSeries.getData().add(new M3DataPoint(label,
                    round1(record.getBodyFatPct())));
        }
        splineBiometrics.getSeriesList().setAll(weightSeries, bodyFatSeries);
    }

    /** "2026-11-21" → "11-21" chart labels. */
    private static String shortDate(String isoDate) {
        return isoDate.length() == 10 ? isoDate.substring(5) : isoDate;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
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
