package com.gym.controller;

import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chart.M3GaugeChart;
import io.m3fx.controls.chart.M3Heatmap;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.effect.M3GlassPane;
import io.m3fx.controls.effect.M3Scanline;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.textfield.M3TextField;
import io.m3fx.controls.textfield.TextFieldVariant;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Controller for Fast-Lane Attendance &amp; Turnstile Gateway screen.
 * UI-contract only (no backend services or SQL logic): the check-in stream is a working demo —
 * manual and programmatic check-in/out append or update the visible stream rows (never toast
 * only), the occupancy KPI and gauge move with them, the daily report dialog is computed from
 * the visible stream, and record corrections rewrite the row time with a confirm dialog.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former attendance.fxml: top bar, bento command grid (gauge HUD, peak-hours heatmap, scanner
 * viewport, member dossier, live check-in stream).
 */
public class AttendanceController implements ScreenController {

    /** Matches the time format the stream rows use ("07:02"), so corrections stay uniform. */
    private static final String TIME_PATTERN = "^([01]\\d|2[0-3]):[0-5]\\d$";
    /** Plan recorded for a manual check-in whose member is not in the demo stream yet. */
    private static final String WALK_IN_PLAN = "Day Pass";

    // ---- View fields (the former fx:id injections, now built by buildView) ----
    private StackPane viewRoot;
    private Label lblCurrentDate;
    private M3StatCard cardInsideCount;
    private M3GaugeChart occupancyGauge;
    private M3Heatmap peakHoursHeatmap;
    private M3Card memberDossierCard;
    private Label lblScanResult;
    private Label lblScanTimestamp;
    private Label lblScannedMemberName;
    private Label lblScannedPlanInfo;
    private Label lblAssignedTrainer;
    private M3TextField txtMemberLookup;
    private M3DataTable<String> attendanceDataTable;
    private M3Button btnDailyReportAction;
    private M3Scanline turnstileScanline;
    private M3Button btnManualCheckIn;
    private M3Button btnManualCheckOut;
    private TextField txtTableSearch;

    /** Hosts M3 snackbars and dialogs for this screen (attached to the scene root at runtime). */
    private final M3Overlay feedback = new M3Overlay();

    /** Master stream (newest first for manual entries); the table shows the search-filtered view. */
    private final List<String> streamRows = new ArrayList<>();
    /** Athletes currently inside, seeded from the occupancy KPI value (142) and moved by check-ins. */
    private int insideCount = 142;

    /**
     * Builds the complete Fast-Lane Attendance view and initializes all behavior. Same ordering
     * semantics the FXMLLoader used to provide: the node tree exists and every field is assigned
     * before the former {@code initialize()} work runs.
     */
    public AttendanceController() {
        viewRoot = buildView();

        // Show today's date instead of the stale hard-coded seed value
        lblCurrentDate.setText(LocalDate.now().toString());
        installFeedbackLayer(attendanceDataTable);
        seedOccupancyKpis();
        populateAttendanceStream();

    // fx:id parity: every node the markup used to tag with fx:id keeps its JavaFX id, so
    // CSS #id selectors, Node.lookup("#..."), the cross-screen rail navigation
    // (findById("navCustomers") and friends) and the interaction tests keep working unchanged.
        lblCurrentDate.setId("lblCurrentDate");
        cardInsideCount.setId("cardInsideCount");
        occupancyGauge.setId("occupancyGauge");
        peakHoursHeatmap.setId("peakHoursHeatmap");
        memberDossierCard.setId("memberDossierCard");
        lblScanResult.setId("lblScanResult");
        lblScanTimestamp.setId("lblScanTimestamp");
        lblScannedMemberName.setId("lblScannedMemberName");
        lblScannedPlanInfo.setId("lblScannedPlanInfo");
        lblAssignedTrainer.setId("lblAssignedTrainer");
        txtMemberLookup.setId("txtMemberLookup");
        attendanceDataTable.setId("attendanceDataTable");
        btnDailyReportAction.setId("btnDailyReportAction");
        turnstileScanline.setId("turnstileScanline");
        btnManualCheckIn.setId("btnManualCheckIn");
        btnManualCheckOut.setId("btnManualCheckOut");
        txtTableSearch.setId("txtTableSearch");
        txtTableSearch.textProperty().addListener((obs, ov, nv) -> refreshStream());
    }

    /**
     * Constructs the whole node tree (the programmatic replacement for attendance.fxml) and
     * wires every handler.
     */
    private StackPane buildView() {
        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("attendance-layout");

        // Top app bar & turnstile status.
        VBox topBarContainer = new VBox(8);
        topBarContainer.getStyleClass().add("top-bar-container");
        HBox topRow = new HBox(16);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(14, 24, 12, 24));
        lblCurrentDate = new Label("2026-10-28");
        lblCurrentDate.getStyleClass().add("title-medium");
        btnDailyReportAction = new M3Button("Daily Report", ButtonVariant.TONAL);
        btnDailyReportAction.getStyleClass().add("m3-button-tonal");
        btnDailyReportAction.setOnAction(e -> onDailyReportClick());
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        topRow.getChildren().addAll(
                styledLabel("FAST-LANE ATTENDANCE & TURNSTILE GATEWAY", "title-large"),
                topSpacer,
                lblCurrentDate,
                btnDailyReportAction);
        topBarContainer.getChildren().add(topRow);
        layout.setTop(topBarContainer);

        // Center bento command grid.
        GridPane bentoGrid = new GridPane();
        bentoGrid.setHgap(16);
        bentoGrid.setVgap(16);
        bentoGrid.getStyleClass().add("bento-command-grid");
        bentoGrid.setPadding(new Insets(8, 24, 64, 24));
        ColumnConstraints leftColumn = new ColumnConstraints();
        leftColumn.setPercentWidth(42);
        ColumnConstraints rightColumn = new ColumnConstraints();
        rightColumn.setPercentWidth(58);
        RowConstraints topRowConstraint = new RowConstraints();
        topRowConstraint.setPercentHeight(26);
        RowConstraints bottomRowConstraint = new RowConstraints();
        bottomRowConstraint.setPercentHeight(74);
        bentoGrid.getColumnConstraints().addAll(leftColumn, rightColumn);
        bentoGrid.getRowConstraints().addAll(topRowConstraint, bottomRowConstraint);

        // Top left: occupancy gauge HUD.
        HBox occupancyHud = new HBox(16);
        occupancyHud.setAlignment(Pos.CENTER_LEFT);
        cardInsideCount = new M3StatCard("Currently Inside", "142", "Safety Cap: 200");
        HBox.setHgrow(cardInsideCount, Priority.ALWAYS);
        occupancyGauge = new M3GaugeChart();
        occupancyGauge.setPrefSize(120, 120);
        occupancyHud.getChildren().addAll(cardInsideCount, occupancyGauge);
        bentoGrid.add(occupancyHud, 0, 0);

        // Top right: peak hours density heatmap.
        VBox heatmapCard = new VBox(8);
        heatmapCard.getStyleClass().add("heatmap-container-card");
        heatmapCard.setPadding(new Insets(10, 14, 10, 14));
        peakHoursHeatmap = new M3Heatmap();
        VBox.setVgrow(peakHoursHeatmap, Priority.ALWAYS);
        heatmapCard.getChildren().addAll(
                styledLabel("HOURLY ATTENDANCE DENSITY (PEAK HOURS)", "label-small"),
                peakHoursHeatmap);
        bentoGrid.add(heatmapCard, 1, 0);

        // Bottom left: fast-lane scanner & member dossier.
        VBox scannerColumn = new VBox(14);

        // Laser scanner viewport.
        StackPane scanViewport = new StackPane();
        scanViewport.setPrefHeight(200);
        scanViewport.getStyleClass().add("scan-viewport-pane");
        M3GlassPane scannerGlass = new M3GlassPane();
        scannerGlass.setOpacity(0.8);
        turnstileScanline = new M3Scanline();
        VBox scanPrompt = new VBox(8);
        scanPrompt.setAlignment(Pos.CENTER);
        scanPrompt.getChildren().addAll(
                styledLabel("PRESENT RFID BADGE / QR CODE", "label-large"),
                styledLabel("Align barcode within illuminated sensor zone", "body-small"));
        scanViewport.getChildren().addAll(scannerGlass, turnstileScanline, scanPrompt);

        // Last scanned member dossier.
        memberDossierCard = new M3Card(CardVariant.ELEVATED);
        memberDossierCard.getStyleClass().add("dossier-pass-granted");
        VBox.setVgrow(memberDossierCard, Priority.ALWAYS);
        VBox dossierContent = new VBox(10);
        dossierContent.setPadding(new Insets(16));
        HBox dossierHeader = new HBox();
        dossierHeader.setAlignment(Pos.CENTER_LEFT);
        lblScanResult = new Label("ACCESS GRANTED");
        lblScanResult.getStyleClass().add("title-medium");
        Region dossierSpacer = new Region();
        HBox.setHgrow(dossierSpacer, Priority.ALWAYS);
        lblScanTimestamp = new Label("07:02 AM");
        lblScanTimestamp.getStyleClass().add("body-small");
        dossierHeader.getChildren().addAll(lblScanResult, dossierSpacer, lblScanTimestamp);
        lblScannedMemberName = new Label("Sarah Connor (ID #1042)");
        lblScannedMemberName.getStyleClass().add("headline-small");
        lblScannedPlanInfo = new Label("Plan: Yearly VIP · Valid until 2027-04-12");
        lblScannedPlanInfo.getStyleClass().add("body-medium");
        lblAssignedTrainer = new Label("Assigned Coach: Marcus Vance");
        lblAssignedTrainer.getStyleClass().add("body-small");
        dossierContent.getChildren().addAll(dossierHeader,
                lblScannedMemberName, lblScannedPlanInfo, lblAssignedTrainer);
        memberDossierCard.setContent(dossierContent);

        // Manual fast check-in row.
        HBox manualRow = new HBox(10);
        manualRow.setAlignment(Pos.CENTER_LEFT);
        txtMemberLookup = new M3TextField("Enter Member ID or Phone", TextFieldVariant.FILLED);
        HBox.setHgrow(txtMemberLookup, Priority.ALWAYS);
        btnManualCheckIn = new M3Button("Check-In", ButtonVariant.FILLED);
        btnManualCheckIn.getStyleClass().add("m3-button-filled");
        btnManualCheckIn.setOnAction(e -> onManualCheckInClick());
        btnManualCheckOut = new M3Button("Check-Out", ButtonVariant.OUTLINED);
        btnManualCheckOut.getStyleClass().add("m3-button-outlined");
        btnManualCheckOut.setOnAction(e -> onManualCheckOutClick());
        manualRow.getChildren().addAll(txtMemberLookup, btnManualCheckIn, btnManualCheckOut);

        scannerColumn.getChildren().addAll(scanViewport, memberDossierCard, manualRow);
        bentoGrid.add(scannerColumn, 0, 1);

        // Bottom right: live check-in stream table.
        VBox streamCard = new VBox(10);
        streamCard.getStyleClass().add("table-container-card");
        HBox streamHeader = new HBox(12);
        streamHeader.setAlignment(Pos.CENTER_LEFT);
        txtTableSearch = new TextField();
        txtTableSearch.setPromptText("Filter attendees...");
        txtTableSearch.setPrefWidth(220);
        Region streamSpacer = new Region();
        HBox.setHgrow(streamSpacer, Priority.ALWAYS);
        streamHeader.getChildren().addAll(
                styledLabel("TODAY'S ATTENDANCE STREAM", "title-medium"),
                streamSpacer,
                txtTableSearch);
        attendanceDataTable = new M3DataTable<>();
        VBox.setVgrow(attendanceDataTable, Priority.ALWAYS);
        streamCard.getChildren().addAll(streamHeader, attendanceDataTable);
        bentoGrid.add(streamCard, 1, 1);

        layout.setCenter(bentoGrid);

        StackPane root = new StackPane();
        root.getStyleClass().add("attendance-root");
        root.getChildren().add(layout);
        return root;
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

    /** Reads the occupancy KPI seed from the card value and bounds the gauge to the safety cap. */
    private void seedOccupancyKpis() {
        if (cardInsideCount.getValue() != null) {
            try {
                insideCount = Integer.parseInt(cardInsideCount.getValue().trim());
            } catch (NumberFormatException ignored) {
                // keep the 142 demo seed
            }
        }
        occupancyGauge.setTitle("OCCUPANCY");
        occupancyGauge.setMaxValue(200); // the KPI's "Safety Cap: 200"
        occupancyGauge.setValue(insideCount);
    }

    /** Fills the check-in stream with today's demo rows so the screen is not blank. */
    private void populateAttendanceStream() {
        if (!attendanceDataTable.getColumns().isEmpty()) {
            return;
        }
        attendanceDataTable.getColumns().addAll(List.of(
                new M3DataTable.Column<>("Time", r -> r.split("\\|")[0], 90),
                new M3DataTable.Column<>("Member", r -> r.split("\\|")[1], 180),
                new M3DataTable.Column<>("Plan", r -> r.split("\\|")[2], 130),
                new M3DataTable.Column<>("Status", r -> r.split("\\|")[3], 110)));
        for (String row : new String[] {
                "07:02|Sarah Connor (#1042)|Yearly VIP|Check-In",
                "07:05|Marcus Vance (#101)|Staff|Check-In",
                "07:11|Priya Nair (#2075)|Monthly|Check-In",
                "07:18|Tom Becker (#1533)|10-Pack|Check-In",
                "07:26|Lena Fischer (#3110)|Yearly|Check-In",
                "07:33|Diego Ramos (#2871)|Trainer|Check-In",
                "07:40|Aisha Khan (#1902)|Monthly|Check-In",
                "07:47|Jonas Weber (#1254)|Day Pass|Check-In"}) {
            streamRows.add(row);
        }
        refreshStream();
    }

    /** Applies the stream table filter box to the visible rows (every visible control answers). */
    private void refreshStream() {
        String query = txtTableSearch.getText() == null ? "" : txtTableSearch.getText().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            attendanceDataTable.getItems().setAll(streamRows);
            return;
        }
        List<String> filtered = new ArrayList<>();
        for (String row : streamRows) {
            if (row.toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(row);
            }
        }
        attendanceDataTable.getItems().setAll(filtered);
    }

    // ------------------------------------------------------------------
    // Event handlers
    // ------------------------------------------------------------------

    /** Opens the daily report dialog computed from the visible stream. */
    public void onDailyReportClick() {
        showDailyReport();
    }

    /** Manual check-in through the member lookup field (validation inline + snackbar). */
    public void onManualCheckInClick() {
        performCheckIn(lookupQuery());
    }

    /** Manual check-out through the member lookup field (validation inline + snackbar). */
    public void onManualCheckOutClick() {
        performCheckOut(lookupQuery());
    }

    // ------------------------------------------------------------------
    // UML / Service contract signatures
    // ------------------------------------------------------------------

    /** Programmatic check-in using the member lookup field (same core as the manual button). */
    public void onCheckIn() {
        performCheckIn(lookupQuery());
    }

    /** Programmatic check-in for a member id; unknown ids enter the stream as a walk-in. */
    public void onCheckIn(int customerId) {
        String[] identity = identityOf("#" + customerId);
        if (identity == null) {
            identity = new String[] {"Member #" + customerId, WALK_IN_PLAN};
        }
        checkInMember(identity[0], identity[1]);
    }

    /** Programmatic check-out using the member lookup field (same core as the manual button). */
    public void onCheckOut() {
        performCheckOut(lookupQuery());
    }

    /** Programmatic check-out for a member id; answers when no active check-in exists. */
    public void onCheckOut(int customerId) {
        performCheckOut("#" + customerId);
    }

    /** Opens the daily report dialog for today. */
    public void showDailyReport() {
        showDailyReport(LocalDate.now().toString());
    }

    /** Opens the daily report dialog computed from the visible stream rows. */
    public void showDailyReport(String date) {
        int checkIns = 0;
        int checkOuts = 0;
        int corrections = 0;
        Set<String> members = new HashSet<>();
        for (String row : streamRows) {
            String[] parts = row.split("\\|");
            if (parts.length > 3) {
                members.add(parts[1]);
                switch (parts[3]) {
                    case "Check-In" -> checkIns++;
                    case "Check-Out" -> checkOuts++;
                    case "Corrected" -> corrections++;
                    default -> { /* statuses the report does not split out */ }
                }
            }
        }
        Label intro = new Label("Turnstile activity for " + date + " (demo data):");
        intro.setWrapText(true);
        VBox content = new VBox(8, intro,
                reportLine("Unique members", members.size() + " distinct"),
                reportLine("Check-ins", String.valueOf(checkIns)),
                reportLine("Check-outs", String.valueOf(checkOuts)),
                reportLine("Corrections", String.valueOf(corrections)),
                reportLine("Currently inside", insideCount + " of the 200 safety cap"));
        content.setPrefWidth(380);
        M3Button close = M3Button.text("Close");
        M3Dialog dialog = M3Dialog.basic("Daily attendance report", content, close);
        dialog.setDefaultAction(close::activate);
        close.setOnAction(e -> dialog.hide());
        feedback.showDialog(dialog);
    }

    /** Opens the correction dialog for the newest stream row. */
    public void onCorrectRecord() {
        correctRow(streamRows.isEmpty() ? null : streamRows.get(0));
    }

    /** Opens the correction dialog for the stream row carrying the given attendance/member id. */
    public void onCorrectRecord(int attendanceId) {
        correctRow(streamRows.stream()
                .filter(r -> r.contains("#" + attendanceId + ")"))
                .findFirst().orElse(null));
    }

    // ------------------------------------------------------------------
    // Check-in / check-out cores (visible-row mutation, never toast only)
    // ------------------------------------------------------------------

    /** Check-in core: validates the query, dedupes active check-ins, updates the stream + KPIs. */
    private void performCheckIn(String query) {
        if (requireLookup(query)) {
            return;
        }
        String[] identity = identityOf(query);
        String member = identity == null ? query : identity[0];
        String plan = identity == null ? WALK_IN_PLAN : identity[1];
        checkInMember(member, plan);
    }

    /** Adds the Check-In row for the given member unless they are already inside. */
    private void checkInMember(String member, String plan) {
        if (findLatestRow(member, "Check-In") != null) {
            feedback.showSnackbar(member + " is already checked in (demo).");
            return;
        }
        String time = clockText();
        streamRows.add(0, time + "|" + member + "|" + plan + "|Check-In");
        insideCount++;
        updateOccupancyKpis();
        refreshStream();
        feedback.showSnackbar(member + " checked in at " + time + " (" + plan + ", demo).");
    }

    /** Check-out core: validates the query and flips the member's active check-in row. */
    private void performCheckOut(String query) {
        if (requireLookup(query)) {
            return;
        }
        String row = findLatestRow(query, "Check-In");
        if (row == null) {
            feedback.showSnackbar("No active check-in found for \"" + query + "\" (demo).");
            return;
        }
        String[] parts = row.split("\\|");
        String time = clockText();
        replaceRow(row, time + "|" + parts[1] + "|" + parts[2] + "|Check-Out");
        insideCount = Math.max(0, insideCount - 1);
        updateOccupancyKpis();
        refreshStream();
        feedback.showSnackbar(parts[1] + " checked out at " + time + " (demo).");
    }

    /**
     * Two-step record correction (destructive rewrite): an M3Dialog validates the corrected
     * time, then the row's time visibly updates and its status flips to Corrected.
     */
    private void correctRow(String row) {
        if (row == null) {
            feedback.showSnackbar("No attendance record matches (demo).");
            return;
        }
        String[] parts = row.split("\\|");
        M3TextField timeField = new M3TextField("Corrected time (HH:mm)", TextFieldVariant.OUTLINED);
        timeField.setSupportingText("24h format, e.g. 07:15 — currently " + parts[0]);
        M3Button cancel = M3Button.text("Keep Record");
        M3Button correct = M3Button.text("Correct Record");
        M3Dialog dialog = M3Dialog.basic("Correct attendance record",
                new VBox(10, new Label("Record: " + parts[1] + " (" + parts[2] + ", " + parts[3] + ")"),
                        timeField),
                cancel, correct);
        dialog.setDefaultAction(correct::activate);
        cancel.setOnAction(e -> dialog.hide());
        correct.setOnAction(e -> {
            String time = timeField.getText() == null ? "" : timeField.getText().trim();
            if (!time.matches(TIME_PATTERN)) {
                timeField.setError(true);
                timeField.setSupportingText("Enter the time as HH:mm (for example 07:15).");
                return;
            }
            dialog.hide();
            replaceRow(row, time + "|" + parts[1] + "|" + parts[2] + "|Corrected");
            refreshStream();
            feedback.showSnackbar("Record corrected for " + parts[1] + " — " + parts[0] + " → " + time + " (demo).");
        });
        feedback.showDialog(dialog);
    }

    // ------------------------------------------------------------------
    // Stream helpers
    // ------------------------------------------------------------------

    /** Validates the lookup query inline and reports it; true when the input was rejected. */
    private boolean requireLookup(String query) {
        boolean ok = query != null && !query.isBlank();
        txtMemberLookup.setError(!ok);
        txtMemberLookup.setSupportingText(ok ? "" : "Enter the member ID, phone or name to continue.");
        if (ok) {
            return false;
        }
        feedback.showSnackbar("Enter a member to continue (demo).");
        return true;
    }

    /** The lookup field's current query (never null). */
    private String lookupQuery() {
        return txtMemberLookup.getText() == null ? "" : txtMemberLookup.getText().trim();
    }

    /**
     * Resolves a query against the stream: returns {@code member|plan} of the first matching
     * row (any status), or null when no row matches.
     */
    private String[] identityOf(String query) {
        String lower = query.toLowerCase(Locale.ROOT);
        for (String row : streamRows) {
            String[] parts = row.split("\\|");
            if (parts.length > 2 && parts[1].toLowerCase(Locale.ROOT).contains(lower)) {
                return new String[] {parts[1], parts[2]};
            }
        }
        return null;
    }

    /** Newest stream row for the query that carries the given status, or null. */
    private String findLatestRow(String query, String status) {
        String lower = query.toLowerCase(Locale.ROOT);
        for (String row : streamRows) {
            String[] parts = row.split("\\|");
            if (parts.length > 3 && parts[3].equals(status)
                    && parts[1].toLowerCase(Locale.ROOT).contains(lower)) {
                return row;
            }
        }
        return null;
    }

    /** Replaces a stream row in place (by identity) and keeps the search-filtered view in sync. */
    private void replaceRow(String oldRow, String newRow) {
        int index = streamRows.indexOf(oldRow);
        if (index >= 0) {
            streamRows.set(index, newRow);
        }
        refreshStream();
    }

    /** Moves the inside-count KPI and the occupancy gauge with every check-in/out. */
    private void updateOccupancyKpis() {
        cardInsideCount.setValue(String.valueOf(insideCount));
        occupancyGauge.setValue(insideCount);
    }

    private static String clockText() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /** One headline/supporting line of the report dialog. */
    private Label reportLine(String headline, String value) {
        Label line = new Label(headline + ": " + value);
        line.setWrapText(true);
        return line;
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
}
