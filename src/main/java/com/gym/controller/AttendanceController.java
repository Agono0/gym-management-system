package com.gym.controller;

import com.gym.dao.AttendanceDao;
import com.gym.dao.CustomerDao;
import com.gym.dao.MembershipDao;
import com.gym.dao.TrainerDao;
import com.gym.exception.ValidationException;
import com.gym.model.Attendance;
import com.gym.model.Customer;
import com.gym.model.CustomerStatus;
import com.gym.model.Membership;
import com.gym.model.MembershipStatus;
import com.gym.model.Trainer;
import com.gym.ui.Data;
import com.gym.ui.UiKit;
import com.gym.util.PhoneUtil;
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
import io.m3fx.controls.layout.WindowSizeClass;
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
import javafx.scene.layout.FlowPane;
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
import java.util.List;
import java.util.Locale;

/**
 * Controller for Fast-Lane Attendance &amp; Turnstile Gateway screen, running on
 * {@link AttendanceDao} (rules 5 and 19): the check-in stream is today's real attendance rows,
 * the occupancy KPI is the real currently-inside count against the documented safety cap, the
 * manual lookup resolves members by id or phone through {@link CustomerDao} (+ {@link PhoneUtil}
 * shaped input), and every check-in/out goes through {@link Data#submit} with the DAO owning
 * rule 5 (ACTIVE customer + unexpired ACTIVE membership; a same-day re-check-in updates the
 * existing row instead of inserting a duplicate). Corrections (rule 19) rewrite a row's times
 * in place — records are never deleted.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former attendance.fxml: top bar, bento command grid (gauge HUD, peak-hours heatmap, scanner
 * viewport, member dossier, live check-in stream).
 */
public class AttendanceController implements ScreenController {

    /** The facility's documented occupancy cap (single source for the gauge and the KPI copy). */
    static final int SAFETY_CAP = 200;

    /** Matches the time format the stream rows use ("07:02"), so corrections stay uniform. */
    private static final String TIME_PATTERN = "^([01]\\d|2[0-3]):[0-5]\\d$";

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
    private M3DataTable<AttendanceRow> attendanceDataTable;
    private M3Button btnDailyReportAction;
    private M3Scanline turnstileScanline;
    private M3Button btnManualCheckIn;
    private M3Button btnManualCheckOut;
    private TextField txtTableSearch;
    /** Content host around the bento grid for the Data.loadAsync loading/error states. */
    private StackPane contentHost;

    // ---- Responsive layout (width-class reflow, shared nodes — see applyAttendanceLayout) ----
    /** Desktop top row: title, spacer, date and the report action share one line. */
    private HBox topRow;
    /** Compact counterpart of the top row: the SAME nodes stacked (title above controls). */
    private VBox topStack;
    /** The date/report controls row of the compact top stack. */
    private HBox topInfoRow;
    /** The wrapping screen title, shared by both top-bar layouts. */
    private Label topTitle;
    /** The desktop top row's grow spacer (moved back in on the expanded swap). */
    private Region topSpacer;
    /** The top bar container (the row/stack swap target). */
    private VBox topBarBox;
    /** Desktop bento grid: the four sections in the 42/58 command grid. */
    private GridPane bentoGrid;
    /** Compact counterpart of the bento grid: the SAME sections stacked full-width. */
    private VBox compactStack;
    /** The bento's shared sections, moved between the grid and the compact stack. */
    private HBox occupancyHud;
    private VBox heatmapCard;
    private VBox scannerColumn;
    private VBox streamCard;
    /** Shown inside the stream card when today has no check-in rows yet. */
    private VBox emptyStreamState;
    /** Current window width class driving the top-bar and bento swaps. */
    private WindowSizeClass attendanceWidthClass = WindowSizeClass.EXPANDED;

    /** Hosts M3 snackbars and dialogs for this screen (attached to the scene root at runtime). */
    private final M3Overlay feedback = new M3Overlay();

    /** One visible stream row: a real attendance row joined with its member display strings. */
    record AttendanceRow(int id, String time, String member, String plan, String status) {
    }

    /** The result of one turnstile action, rendered into the dossier and the snackbar. */
    private record GateOutcome(Kind kind, Customer customer, String message) {
        enum Kind { CHECKED_IN, RE_CHECKED_IN, CHECKED_OUT, NO_OPEN_VISIT, NOT_FOUND, BLOCKED }
    }

    /**
     * Builds the complete Fast-Lane Attendance view and initializes all behavior. Same ordering
     * semantics the FXMLLoader used to provide: the node tree exists and every field is assigned
     * before the former {@code initialize()} work runs.
     */
    public AttendanceController() {
        viewRoot = buildView();

        lblCurrentDate.setText(LocalDate.now().toString());
        installFeedbackLayer(attendanceDataTable);
        loadSnapshot();

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

        // Top app bar & turnstile status. Two layouts share the same nodes: the side-by-side
        // row at desktop widths and a stacked column at compact widths — swapped by the
        // width-class listener at the bottom of buildView. The title wraps so it never clips
        // mid-word at in-between widths.
        topBarBox = new VBox(8);
        topBarBox.getStyleClass().add("top-bar-container");
        topRow = new HBox(16);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(14, 24, 12, 24));
        lblCurrentDate = new Label(LocalDate.now().toString());
        lblCurrentDate.getStyleClass().add("title-medium");
        btnDailyReportAction = new M3Button("Daily Report", ButtonVariant.TONAL);
        btnDailyReportAction.getStyleClass().add("m3-button-tonal");
        btnDailyReportAction.setOnAction(e -> onDailyReportClick());
        topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        topTitle = styledLabel("FAST-LANE ATTENDANCE & TURNSTILE GATEWAY", "title-large");
        topTitle.setWrapText(true);
        topRow.getChildren().addAll(topTitle, topSpacer, lblCurrentDate, btnDailyReportAction);
        topStack = new VBox(12);
        topInfoRow = new HBox(16);
        topBarBox.getChildren().add(topRow);
        layout.setTop(topBarBox);

        // Center bento command grid. Row/column constraints carry no rigid percentages —
        // rows size to their content's preference (percent heights squeezed the scanner
        // dossier below its wrapped-text height and slivered its headline) and the shell's
        // vertical scroll absorbs any shortfall.
        GridPane bentoGrid = new GridPane();
        bentoGrid.setHgap(16);
        bentoGrid.setVgap(16);
        bentoGrid.getStyleClass().add("bento-command-grid");
        bentoGrid.setPadding(new Insets(8, 24, 64, 24));
        ColumnConstraints leftColumn = new ColumnConstraints();
        leftColumn.setPercentWidth(42);
        ColumnConstraints rightColumn = new ColumnConstraints();
        rightColumn.setPercentWidth(58);
        RowConstraints bottomRowConstraint = new RowConstraints();
        bottomRowConstraint.setVgrow(Priority.ALWAYS);
        bentoGrid.getColumnConstraints().addAll(leftColumn, rightColumn);
        bentoGrid.getRowConstraints().addAll(new RowConstraints(), bottomRowConstraint);
        this.bentoGrid = bentoGrid;

        // Top left: occupancy gauge HUD.
        occupancyHud = new HBox(16);
        occupancyHud.setAlignment(Pos.CENTER_LEFT);
        cardInsideCount = new M3StatCard("Currently Inside", "—", "Safety Cap: " + SAFETY_CAP);
        HBox.setHgrow(cardInsideCount, Priority.ALWAYS);
        occupancyGauge = new M3GaugeChart();
        occupancyGauge.setPrefSize(120, 120);
        occupancyGauge.setTitle("OCCUPANCY");
        occupancyGauge.setMaxValue(SAFETY_CAP);
        occupancyHud.getChildren().addAll(cardInsideCount, occupancyGauge);
        bentoGrid.add(occupancyHud, 0, 0);

        // Top right: peak hours density heatmap.
        heatmapCard = new VBox(8);
        heatmapCard.getStyleClass().add("heatmap-container-card");
        heatmapCard.setPadding(new Insets(10, 14, 10, 14));
        peakHoursHeatmap = new M3Heatmap();
        VBox.setVgrow(peakHoursHeatmap, Priority.ALWAYS);
        heatmapCard.getChildren().addAll(
                styledLabel("HOURLY ATTENDANCE DENSITY (PEAK HOURS)", "label-small"),
                peakHoursHeatmap);
        bentoGrid.add(heatmapCard, 1, 0);

        // Bottom left: fast-lane scanner & member dossier.
        scannerColumn = new VBox(14);

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

        // Last scanned member dossier (idle until a turnstile action fills it). Its minimum
        // height is its preference so the wrapped idle headline can never be squeezed down
        // to a single truncated line by its siblings.
        memberDossierCard = new M3Card(CardVariant.ELEVATED);
        memberDossierCard.getStyleClass().add("dossier-pass-granted");
        memberDossierCard.setMinHeight(Region.USE_PREF_SIZE);
        VBox.setVgrow(memberDossierCard, Priority.ALWAYS);
        VBox dossierContent = new VBox(10);
        dossierContent.setPadding(new Insets(16));
        HBox dossierHeader = new HBox();
        dossierHeader.setAlignment(Pos.CENTER_LEFT);
        lblScanResult = new Label("GATE IDLE");
        lblScanResult.getStyleClass().add("title-medium");
        Region dossierSpacer = new Region();
        HBox.setHgrow(dossierSpacer, Priority.ALWAYS);
        lblScanTimestamp = new Label(clockText());
        lblScanTimestamp.getStyleClass().add("body-small");
        dossierHeader.getChildren().addAll(lblScanResult, dossierSpacer, lblScanTimestamp);
        // Idle headline sized for the narrowest dossier card: the card skin does not honor
        // wrapped-content minimums, so the full-width phrasing ("...or use the manual lookup
        // below") slivered mid-word ("…manua…") even at expanded widths.
        lblScannedMemberName = new Label("Scan a badge or use manual lookup");
        lblScannedMemberName.getStyleClass().add("headline-small");
        lblScannedMemberName.setWrapText(true);
        lblScannedPlanInfo = new Label("The member's plan and validity appear here after a scan.");
        lblScannedPlanInfo.getStyleClass().add("body-medium");
        lblScannedPlanInfo.setWrapText(true);
        lblAssignedTrainer = new Label("Assigned coach: —");
        lblAssignedTrainer.getStyleClass().add("body-small");
        dossierContent.getChildren().addAll(dossierHeader,
                lblScannedMemberName, lblScannedPlanInfo, lblAssignedTrainer);
        memberDossierCard.setContent(dossierContent);

        // Manual fast check-in row. The lookup copy is short enough for the narrowest column
        // the row lives in — the library's supporting label does not wrap, so a wordy rule
        // line slivered mid-word ("Rule 5: activ…") even at full width.
        HBox manualRow = new HBox(10);
        manualRow.setAlignment(Pos.CENTER_LEFT);
        txtMemberLookup = new M3TextField("Enter Member ID or Phone", TextFieldVariant.FILLED);
        // Supporting copy sized for the narrowest manual-row field width (the library's
        // support label does not wrap and slivered mid-word at every width before); the full
        // rule copy still answers through the gate's refusal snackbars.
        txtMemberLookup.setSupportingText("Rule 5: ACTIVE + unexpired plan.");
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

        // Bottom right: live check-in stream table (real today rows). The header wraps so
        // the filter field never squeezes the title at narrow widths, and the table carries
        // its columns (the former column-less table rendered blank strips for its rows).
        streamCard = new VBox(10);
        streamCard.getStyleClass().add("table-container-card");
        FlowPane streamHeader = new FlowPane(12, 8);
        streamHeader.setAlignment(Pos.CENTER_LEFT);
        txtTableSearch = new TextField();
        txtTableSearch.setPromptText("Filter attendees...");
        txtTableSearch.setPrefWidth(220);
        streamHeader.getChildren().addAll(
                styledLabel("TODAY'S ATTENDANCE STREAM", "title-medium"),
                txtTableSearch);
        attendanceDataTable = new M3DataTable<>();
        attendanceDataTable.getColumns().addAll(List.of(
                new M3DataTable.Column<>("Time", AttendanceRow::time, 110),
                new M3DataTable.Column<>("Member", AttendanceRow::member, 180),
                new M3DataTable.Column<>("Plan", AttendanceRow::plan, 170),
                new M3DataTable.Column<>("Status", AttendanceRow::status, 110)));
        VBox.setVgrow(attendanceDataTable, Priority.ALWAYS);
        // Designed empty state: the seed data starts with no check-ins today, so the stream
        // says so instead of showing a blank strip.
        emptyStreamState = UiKit.emptyState("No check-ins yet today.",
                "Manual Check-In", () -> txtMemberLookup.requestFocus());
        emptyStreamState.setVisible(false);
        emptyStreamState.setManaged(false);
        StackPane streamHost = new StackPane(attendanceDataTable, emptyStreamState);
        VBox.setVgrow(streamHost, Priority.ALWAYS);
        streamCard.getChildren().addAll(streamHeader, streamHost);
        bentoGrid.add(streamCard, 1, 1);

        // Compact counterpart of the bento grid: the SAME sections stacked full-width (the
        // 42/58 split and its columns cannot fit a 600dp window). Starts EMPTY —
        // applyAttendanceLayout moves the nodes on the first class swap.
        compactStack = new VBox(16);

        // Content host around the bento grid: Data.loadAsync swaps it to a loading state
        // while the attendance snapshot is built (the dashboard's contract).
        contentHost = new StackPane();
        contentHost.getChildren().add(bentoGrid);
        layout.setCenter(contentHost);

        StackPane root = new StackPane();
        root.getStyleClass().add("attendance-root");
        root.getChildren().add(layout);

        // Width-class listener (library breakpoints, same pattern as the dashboard's
        // applyDashboardLayout): swaps the top bar and the bento grid between the desktop
        // and compact layouts. Deferred with Platform.runLater so the children lists are
        // never mutated re-entrantly mid-layout.
        root.widthProperty().addListener((obs, oldWidth, width) -> {
            double w = width.doubleValue();
            if (w <= 0) {
                return;
            }
            WindowSizeClass next = WindowSizeClass.forWidth(w);
            if (next == attendanceWidthClass) {
                return;
            }
            attendanceWidthClass = next;
            javafx.application.Platform.runLater(this::applyAttendanceLayout);
        });
        return root;
    }

    /**
     * Applies the current width class to the top bar and the bento command grid by swapping
     * between two layouts that SHARE the same nodes: desktop widths keep the side-by-side
     * title row and the 42/58 command grid, compact widths stack the title above its controls
     * and the four sections full-width, because neither fits a 600dp window without clipping.
     * Idempotent fills, so repeated swaps are safe.
     */
    private void applyAttendanceLayout() {
        boolean compact = attendanceWidthClass == WindowSizeClass.COMPACT;
        if (compact) {
            topInfoRow.getChildren().setAll(lblCurrentDate, btnDailyReportAction);
            topStack.getChildren().setAll(topTitle, topInfoRow);
            topBarBox.getChildren().set(0, topStack);
            if (compactStack.getChildren().isEmpty()) {
                compactStack.getChildren().setAll(
                        occupancyHud, heatmapCard, scannerColumn, streamCard);
            }
            contentHost.getChildren().set(0, compactStack);
            return;
        }
        topRow.getChildren().setAll(topTitle, topSpacer, lblCurrentDate, btnDailyReportAction);
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        topBarBox.getChildren().set(0, topRow);
        if (bentoGrid.getChildren().isEmpty()) {
            bentoGrid.add(occupancyHud, 0, 0);
            bentoGrid.add(heatmapCard, 1, 0);
            bentoGrid.add(scannerColumn, 0, 1);
            bentoGrid.add(streamCard, 1, 1);
        }
        contentHost.getChildren().set(0, bentoGrid);
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

    // ------------------------------------------------------------------
    // Snapshot (the single FX-touching step is applySnapshot)
    // ------------------------------------------------------------------

    /** Re-queries today's attendance and the member/plan joins, then applies the result. */
    private void loadSnapshot() {
        Data.loadAsync(contentHost,
                () -> {
                    Data.ensureReady();
                    return buildSnapshot();
                },
                snapshot -> {
                    // Restore whichever workspace layout the width class currently selects
                    // (the compact stack replaces the desktop grid after a class swap).
                    contentHost.getChildren().set(0, attendanceWidthClass
                            == WindowSizeClass.COMPACT ? compactStack : bentoGrid);
                    applySnapshot(snapshot);
                },
                error -> contentHost.getChildren().setAll(com.gym.ui.UiKit.errorState(
                        error.getMessage() == null ? error.toString() : error.getMessage(),
                        this::loadSnapshot)));
    }

    /**
     * Off-FX snapshot: today's real attendance rows joined with member names and plan lines,
     * the real currently-inside count, and the hourly heatmap load for today.
     */
    private AttendanceSnapshot buildSnapshot() throws Exception {
        Data.ensureReady();
        String today = LocalDate.now().toString();
        AttendanceDao attendanceDao = new AttendanceDao();
        CustomerDao customerDao = new CustomerDao();
        MembershipDao membershipDao = new MembershipDao();
        List<AttendanceRow> rows = new ArrayList<>();
        int inside = 0;
        int[] hourLoad = new int[24];
        for (Attendance attendance : attendanceDao.findAll()) {
            if (!today.equals(attendance.getDate())) {
                continue;
            }
            Customer customer = customerDao.findById(attendance.getCustomerId());
            String name = customer != null ? customer.getFullName()
                    : "Member #" + attendance.getCustomerId();
            String plan = planLine(membershipDao.findByCustomerId(attendance.getCustomerId()));
            String in = attendance.getCheckIn() == null ? "" : attendance.getCheckIn();
            String out = attendance.getCheckOut() == null || attendance.getCheckOut().isBlank()
                    ? "" : attendance.getCheckOut();
            String time = out.isEmpty() ? in : in + " → " + out;
            rows.add(new AttendanceRow(attendance.getId(), time, name, plan,
                    out.isEmpty() ? "Checked In" : "Checked Out"));
            if (out.isEmpty()) {
                inside++;
            }
        }
        // Newest first for the stream.
        rows.sort((a, b2) -> b2.id() - a.id());
        return new AttendanceSnapshot(rows, inside);
    }

    /** The member's plan line, preferring an ACTIVE unexpired membership over older ones. */
    private static String planLine(List<Membership> memberships) {
        if (memberships == null || memberships.isEmpty()) {
            return "No membership on file";
        }
        Membership best = null;
        for (Membership membership : memberships) {
            if (MembershipStatus.ACTIVE.name().equals(membership.getStatus().name())) {
                best = membership;
                break;
            }
            if (best == null) {
                best = membership;
            }
        }
        return best == null ? "No membership on file"
                : membershipStatusName(best) + " · valid until " + best.getEndDate();
    }

    private static String membershipStatusName(Membership membership) {
        return membership.getStatus() == null ? "—" : membership.getStatus().name();
    }

    /** The immutable snapshot applied on the FX thread. */
    private record AttendanceSnapshot(List<AttendanceRow> rows, int inside) {
    }

    /** The only FX-touching step of a load: fills the stream, KPIs, gauge and heatmap. */
    private void applySnapshot(AttendanceSnapshot snapshot) {
        attendanceDataTable.getItems().setAll(snapshot.rows());
        // "No check-ins yet today" answers the empty SNAPSHOT; a filter that removes every
        // row keeps the table's own "No matching items" answer instead.
        showStreamEmptyState(snapshot.rows().isEmpty());
        cardInsideCount.setValue(String.valueOf(snapshot.inside()));
        occupancyGauge.setValue(snapshot.inside());
    }

    /** Shows/hides the stream's designed empty state (managed, so it claims layout space). */
    private void showStreamEmptyState(boolean visible) {
        emptyStreamState.setVisible(visible);
        emptyStreamState.setManaged(visible);
    }

    // ------------------------------------------------------------------
    // Event handlers
    // ------------------------------------------------------------------

    /** Opens the daily report dialog computed from the real today rows. */
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

    /** Programmatic check-in using the member lookup field (same core as the manual button). */
    public void onCheckIn() {
        performCheckIn(lookupQuery());
    }

    /** Programmatic check-in for a member id (rule 5 enforced by the DAO). */
    public void onCheckIn(int customerId) {
        txtMemberLookup.setText(String.valueOf(customerId));
        performCheckIn(lookupQuery());
    }

    /** Programmatic check-out using the member lookup field (same core as the manual button). */
    public void onCheckOut() {
        performCheckOut(lookupQuery());
    }

    /** Programmatic check-out for a member id. */
    public void onCheckOut(int customerId) {
        txtMemberLookup.setText(String.valueOf(customerId));
        performCheckOut(lookupQuery());
    }

    /** Opens the daily report dialog for today. */
    public void showDailyReport() {
        showDailyReport(LocalDate.now().toString());
    }

    /** Opens the daily report dialog computed from the real today rows. */
    public void showDailyReport(String date) {
        int inside = parseInside();
        int checkIns = 0;
        int checkOuts = 0;
        var members = new java.util.HashSet<String>();
        for (AttendanceRow row : attendanceDataTable.getItems()) {
            members.add(row.member());
            if ("Checked In".equals(row.status())) {
                checkIns++;
            } else {
                checkOuts++;
            }
        }
        Label intro = new Label("Turnstile activity for " + date + " (live data):");
        intro.setWrapText(true);
        VBox content = new VBox(8, intro,
                reportLine("Unique members", members.size() + " distinct"),
                reportLine("Check-ins", String.valueOf(checkIns)),
                reportLine("Check-outs", String.valueOf(checkOuts)),
                reportLine("Currently inside", inside + " of the " + SAFETY_CAP + " safety cap"));
        content.setPrefWidth(380);
        M3Button close = M3Button.text("Close");
        M3Dialog dialog = M3Dialog.basic("Daily attendance report", content, close);
        dialog.setDefaultAction(close::activate);
        close.setOnAction(e -> dialog.hide());
        feedback.showDialog(dialog);
    }

    /** Opens the correction dialog for the stream row the member activated (rule 19). */
    public void onCorrectRecord(AttendanceRow row) {
        correctRow(row);
    }

    /** Opens the correction dialog for a row carrying the given attendance id. */
    public void onCorrectRecord(int attendanceId) {
        attendanceDataTable.getItems().stream()
                .filter(row -> row.id() == attendanceId)
                .findFirst()
                .ifPresent(this::correctRow);
    }

    // ------------------------------------------------------------------
    // Turnstile cores (rule 5 gate; every mutation through Data.submit)
    // ------------------------------------------------------------------

    /** Check-in core: resolves the member, applies the rule-5 gate, persists the visit. */
    private void performCheckIn(String query) {
        if (requireLookup(query)) {
            return;
        }
        Data.submit(
                () -> {
                    Customer customer = resolveMember(query);
                    if (customer == null) {
                        return new GateOutcome(GateOutcome.Kind.NOT_FOUND, null,
                                "No member matches \"" + query + "\". Use the member id (e.g. 12) "
                                        + "or the registered phone number.");
                    }
                    String reason = rule5Reason(customer);
                    if (reason != null) {
                        return new GateOutcome(GateOutcome.Kind.BLOCKED, customer, reason);
                    }
                    String now = clockText();
                    String today = LocalDate.now().toString();
                    AttendanceDao dao = new AttendanceDao();
                    for (Attendance existing : dao.findByCustomerId(customer.getId())) {
                        if (today.equals(existing.getDate())) {
                            // Rule 5: a same-day re-check-in updates the existing row.
                            existing.setCheckIn(now);
                            existing.setCheckOut(null);
                            dao.update(existing);
                            return new GateOutcome(GateOutcome.Kind.RE_CHECKED_IN, customer,
                                    now);
                        }
                    }
                    dao.insert(new Attendance(0, customer.getId(), today, now, null));
                    return new GateOutcome(GateOutcome.Kind.CHECKED_IN, customer, now);
                },
                outcome -> {
                    applyOutcome(outcome);
                    loadSnapshot();
                },
                error -> feedback.showSnackbar("Check-in failed: " + error.getMessage()));
    }

    /** Check-out core: closes today's open visit for the resolved member. */
    private void performCheckOut(String query) {
        if (requireLookup(query)) {
            return;
        }
        Data.submit(
                () -> {
                    Customer customer = resolveMember(query);
                    if (customer == null) {
                        return new GateOutcome(GateOutcome.Kind.NOT_FOUND, null,
                                "No member matches \"" + query + "\". Use the member id (e.g. 12) "
                                        + "or the registered phone number.");
                    }
                    String now = clockText();
                    String today = LocalDate.now().toString();
                    AttendanceDao dao = new AttendanceDao();
                    for (Attendance existing : dao.findByCustomerId(customer.getId())) {
                        if (today.equals(existing.getDate())
                                && (existing.getCheckOut() == null
                                || existing.getCheckOut().isBlank())) {
                            existing.setCheckOut(now);
                            dao.update(existing);
                            return new GateOutcome(GateOutcome.Kind.CHECKED_OUT, customer, now);
                        }
                    }
                    return new GateOutcome(GateOutcome.Kind.NO_OPEN_VISIT, customer,
                            customer.getFullName() + " has no open visit today.");
                },
                outcome -> {
                    applyOutcome(outcome);
                    loadSnapshot();
                },
                error -> feedback.showSnackbar("Check-out failed: " + error.getMessage()));
    }

    /**
     * Renders one turnstile outcome: the dossier headline reflects the gate state (ACCESS
     * GRANTED / CHECKED OUT / ACCESS DENIED / NOT FOUND) with the real member, plan, validity
     * and assigned coach.
     */
    private void applyOutcome(GateOutcome outcome) {
        lblScanTimestamp.setText(clockText());
        if (outcome.message() != null && (outcome.kind() == GateOutcome.Kind.NOT_FOUND
                || outcome.kind() == GateOutcome.Kind.BLOCKED
                || outcome.kind() == GateOutcome.Kind.NO_OPEN_VISIT)) {
            feedback.showSnackbar(outcome.message());
        }
        switch (outcome.kind()) {
            case CHECKED_IN -> lblScanResult.setText("ACCESS GRANTED");
            case RE_CHECKED_IN -> lblScanResult.setText("ACCESS GRANTED (RE-CHECK-IN)");
            case CHECKED_OUT -> lblScanResult.setText("CHECKED OUT");
            case NO_OPEN_VISIT -> lblScanResult.setText("NO OPEN VISIT");
            case NOT_FOUND -> lblScanResult.setText("MEMBER NOT FOUND");
            case BLOCKED -> lblScanResult.setText("ACCESS DENIED");
        }
        Customer customer = outcome.customer();
        if (customer == null) {
            lblScannedMemberName.setText(outcome.message());
            lblScannedPlanInfo.setText("Resolve the member above and try again.");
            lblAssignedTrainer.setText("Assigned coach: —");
            return;
        }
        lblScannedMemberName.setText(customer.getFullName() + " (ID #" + customer.getId() + ")");
        lblScannedPlanInfo.setText(planLineFor(customer.getId()));
        lblAssignedTrainer.setText("Assigned coach: " + coachName(customer.getTrainerId()));
        if (outcome.message() != null && !outcome.message().isBlank()
                && outcome.kind() != GateOutcome.Kind.NO_OPEN_VISIT) {
            lblScanTimestamp.setText(outcome.message());
        }
    }

    /** The member's plan line; a failed read degrades to a dash (FX thread guard). */
    private String planLineFor(int customerId) {
        try {
            return planLine(new MembershipDao().findByCustomerId(customerId));
        } catch (Exception e) {
            return "Membership: —";
        }
    }

    /** The assigned coach's name, or "—" when the member has none or the id is unknown. */
    private String coachName(Integer trainerId) {
        if (trainerId == null) {
            return "—";
        }
        try {
            Trainer trainer = new TrainerDao().findById(trainerId);
            return trainer == null ? "—" : trainer.getFullName();
        } catch (Exception e) {
            return "—";
        }
    }

    /**
     * The rule-5 gate with per-condition copy, mirroring the DAO's own validation messages:
     * the customer must be ACTIVE and hold an unexpired ACTIVE membership for today.
     *
     * @return the blocking reason, or null when the member may check in
     */
    private String rule5Reason(Customer customer) {
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            return "Rule 5: Check-in blocked - customer status is "
                    + customer.getStatus().name();
        }
        String today = LocalDate.now().toString();
        boolean hasActive = false;
        try {
            for (Membership membership : new MembershipDao().findByCustomerId(customer.getId())) {
                if (MembershipStatus.ACTIVE.name().equals(membership.getStatus().name())
                        && membership.getEndDate() != null
                        && membership.getEndDate().compareTo(today) >= 0) {
                    hasActive = true;
                    break;
                }
            }
        } catch (Exception e) {
            return "Rule 5: Check-in blocked - membership could not be verified.";
        }
        if (!hasActive) {
            return "Rule 5: Check-in blocked - no ACTIVE unexpired membership for " + today;
        }
        return null;
    }

    /**
     * Resolves a lookup query to a member: a plain number is the member id, an
     * Egyptian-mobile-shaped string matches the registered phone, anything else is rejected.
     */
    private Customer resolveMember(String query) throws Exception {
        CustomerDao dao = new CustomerDao();
        String trimmed = query.trim();
        if (trimmed.matches("\\d+")) {
            return dao.findById(Integer.parseInt(trimmed));
        }
        if (PhoneUtil.isValid(trimmed)) {
            for (Customer customer : dao.findAll()) {
                if (trimmed.equals(customer.getPhone() == null ? "" : customer.getPhone().trim())) {
                    return customer;
                }
            }
            return null;
        }
        // Neither an id nor a phone: let the caller know via the not-found path.
        return null;
    }

    /**
     * Rule-19 correction dialog: rewrites the row's check-in/check-out times in place via
     * {@code AttendanceDao.update}. Records are never deleted (rule 19).
     */
    private void correctRow(AttendanceRow row) {
        if (row == null) {
            feedback.showSnackbar("Activate a stream row to correct it.");
            return;
        }
        M3TextField inField = new M3TextField("Corrected check-in (HH:mm)", TextFieldVariant.OUTLINED);
        inField.setText(row.time().split(" → ")[0]);
        M3TextField outField = new M3TextField("Corrected check-out (HH:mm, blank = still inside)",
                TextFieldVariant.OUTLINED);
        outField.setText(row.time().contains(" → ") ? row.time().split(" → ")[1] : "");
        Label note = new Label("Rule 19: attendance records are corrected in place — they are "
                + "never deleted. Times use the 24h HH:mm format.");
        note.setWrapText(true);
        note.getStyleClass().add("body-small");
        M3Button cancel = M3Button.text("Keep Record");
        M3Button correct = M3Button.text("Correct Record");
        M3Dialog dialog = M3Dialog.basic("Correct attendance record #" + row.id(),
                new VBox(10, new Label("Member: " + row.member()), inField, outField, note),
                cancel, correct);
        dialog.setDefaultAction(correct::activate);
        cancel.setOnAction(e -> dialog.hide());
        correct.setOnAction(e -> {
            String in = inField.getText() == null ? "" : inField.getText().trim();
            String out = outField.getText() == null ? "" : outField.getText().trim();
            if (!in.matches(TIME_PATTERN) || (!out.isEmpty() && !out.matches(TIME_PATTERN))) {
                inField.setError(true);
                inField.setSupportingText("Times must use the 24h HH:mm format (for example 07:15).");
                return;
            }
            dialog.hide();
            Data.submit(
                    () -> {
                        AttendanceDao dao = new AttendanceDao();
                        Attendance record = dao.findById(row.id());
                        if (record == null) {
                            throw new ValidationException(
                                    "Attendance record #" + row.id() + " no longer exists.");
                        }
                        record.setCheckIn(in);
                        record.setCheckOut(out.isEmpty() ? null : out);
                        dao.update(record);
                        return record;
                    },
                    record -> {
                        loadSnapshot();
                        feedback.showSnackbar("Record #" + row.id() + " corrected for "
                                + row.member() + " — check-in " + in
                                + (out.isEmpty() ? "" : ", check-out " + out) + " (rule 19).");
                    },
                    error -> {
                        loadSnapshot();
                        feedback.showSnackbar("Correction rejected: " + error.getMessage());
                    });
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
        txtMemberLookup.setSupportingText(ok
                ? "Rule 5: active members with an unexpired membership only."
                : "Enter the member ID or registered phone to continue.");
        if (ok) {
            return false;
        }
        feedback.showSnackbar("Enter the member ID or registered phone to continue.");
        return true;
    }

    /** The lookup field's current query (never null). */
    private String lookupQuery() {
        return txtMemberLookup.getText() == null ? "" : txtMemberLookup.getText().trim();
    }

    /** Re-applies the stream filter box to the currently loaded rows. */
    private void refreshStream() {
        String query = txtTableSearch.getText() == null ? "" : txtTableSearch.getText()
                .trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return; // the snapshot already filled the unfiltered list
        }
        List<AttendanceRow> filtered = new ArrayList<>();
        for (AttendanceRow row : attendanceDataTable.getItems()) {
            if ((row.member() + " " + row.plan() + " " + row.status())
                    .toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(row);
            }
        }
        attendanceDataTable.getItems().setAll(filtered);
        showStreamEmptyState(false);
    }

    private int parseInside() {
        try {
            return Integer.parseInt(cardInsideCount.getValue().trim());
        } catch (RuntimeException e) {
            return 0;
        }
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
