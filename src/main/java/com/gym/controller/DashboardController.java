package com.gym.controller;

import com.gym.dao.AttendanceDao;
import com.gym.dao.CustomerDao;
import com.gym.dao.EquipmentDao;
import com.gym.dao.MembershipDao;
import com.gym.dao.PaymentDao;
import com.gym.dao.SessionDao;
import com.gym.dao.TrainerAvailabilityDao;
import com.gym.dao.TrainerDao;
import com.gym.model.Attendance;
import com.gym.model.Customer;
import com.gym.model.CustomerStatus;
import com.gym.model.Membership;
import com.gym.model.MembershipStatus;
import com.gym.model.Payment;
import com.gym.model.PlanType;
import com.gym.model.Session;
import com.gym.model.SessionStatus;
import com.gym.model.Trainer;
import com.gym.model.TrainerAvailability;
import com.gym.ui.Data;
import com.gym.ui.UiKit;
import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.button.M3SplitButton;
import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chart.M3BarChart;
import io.m3fx.controls.chart.M3DataPoint;
import io.m3fx.controls.chart.M3DataSeries;
import io.m3fx.controls.chart.M3DonutChart;
import io.m3fx.controls.chart.M3Sparkline;
import io.m3fx.controls.chart.M3SplineChart;
import io.m3fx.controls.chip.ChipVariant;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.data.M3SpeedDial;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.effect.M3PulseBeacon;
import io.m3fx.core.theme.ColorRole;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.layout.WindowSizeClass;
import io.m3fx.controls.menu.M3Menu;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.progress.M3LinearProgress;
import io.m3fx.controls.scene3d.M3TiltCard3D;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Executive Operations Dashboard.
 * Conforms to docs/ui/dashboard/DASHBOARD_UI.md Section 7 contract.
 *
 * <p>Data contract (DECISIONS.md 77/78, task C3): every KPI, chart and table queries the real
 * SQLite database through the DAOs on a background task ({@link Data#loadAsync}) — no demo
 * numbers, no jitter. The quick check-in enforces rule 5 through {@code AttendanceDao} (ACTIVE
 * customer + unexpired membership; a same-day re-check-in updates the existing row), the
 * session agenda confirms through {@code SessionDao} under rule 18, and slot capacity follows
 * rule 17 (CONFIRMED-only occupancy). Refresh re-queries the database — no simulated telemetry.
 *
 * <p>Every visible action answers: telemetry refresh re-queries, quick check-in and the speed
 * dial route into the rule-5 check-in, and the destructive END SHIFT action confirms through
 * an {@link M3Dialog} first.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former dashboard.fxml: hero banner, KPI bento, traffic/agenda split, facility diagnostics,
 * floating speed dial and toast host.
 */
public class DashboardController implements ScreenController {

    // ---- View fields (the former fx:id injections, now built by buildView) ----
    private StackPane dashboardRoot;
    private M3Card heroBannerCard;
    private Label welcomeGreetingLabel;
    private M3PulseBeacon occupancyBeacon;
    private Label capacityStatusLabel;
    private Label liveTimeLabel;
    private M3Button refreshButton;
    private M3Icon refreshIcon;
    private M3SplitButton quickActionSplitBtn;
    private M3Button logoutButton;
    private M3StatCard statActiveMembers;
    private M3Sparkline memberSparkline;
    private M3StatCard statDailyRevenue;
    private M3Sparkline revenueSparkline;
    private M3StatCard statTodaySessions;
    private M3StatCard statEquipmentHealth;
    private M3SplineChart trafficSplineChart;
    private final M3DataTable<Object[]> agendaTable;
    private M3DonutChart equipmentDonutChart;
    private M3BarChart planBarChart;
    private M3TiltCard3D facilityRadarCard;
    private M3SpeedDial quickActionSpeedDial;
    private StackPane dashboardSnackbar;

    // ---- Responsive layout (width-class reflow, shared nodes — see applyDashboardLayout) ----
    private VBox bentoGrid;
    private HBox heroRow;
    /** Q4: wraps the hero action cluster below the greeting at compact widths. */
    private javafx.scene.layout.FlowPane heroActionsWrap;
    private VBox heroColumn;
    private Region heroSpacer;
    private VBox greetingBlock;
    private HBox actionsBar;
    private M3Card trafficCard;
    private M3Card agendaCard;
    private M3Card donutCard;
    private M3Card barCard;
    private HBox kpiRow;
    private GridPane kpiGrid;
    private HBox vizRow;
    private VBox vizStack;
    private HBox diagnosticsRow;
    private VBox diagnosticsStack;
    /** Current window width class driving the hero/bento layout swaps. */
    private WindowSizeClass dashboardWidthClass = WindowSizeClass.EXPANDED;

    /**
     * Content host around the bento grid: {@link Data#loadAsync} swaps it to a loading state
     * while the DAO snapshot is built and to an error state with retry when a load fails.
     */
    private StackPane contentHost;
    /** Customers eligible for a rule-5 check-in at the last successful load. */
    private List<Customer> checkableCustomers = new ArrayList<>();
    /** Maintenance unit count of the last snapshot, re-applied on width-class swaps. */
    private int lastMaintenanceUnits;

    /** Overlay host for snackbars and dialogs, added to the screen root in code (no fx:id). */
    private final M3Overlay overlay = new M3Overlay();

    /**
     * Builds the complete Executive Operations Dashboard and initializes all behavior. Same
     * ordering semantics the FXMLLoader used to provide: the node tree exists and every field
     * is assigned before the former {@code initialize()} work runs.
     */
    public DashboardController() {
        // Main bento grid container.
        bentoGrid = new VBox(20);
        bentoGrid.setPadding(new Insets(20, 24, 112, 24));

        // TOP: hero header banner. Two layouts share the same nodes: the side-by-side row at
        // desktop widths and a stacked column at compact widths (the compact hero row would cut
        // the action buttons) — swapped by the width-class listener at the bottom of buildView.
        heroBannerCard = new M3Card(CardVariant.ELEVATED);
        heroBannerCard.getStyleClass().add("hero-banner");
        heroBannerCard.setPadding(new Insets(16, 24, 16, 24));
        heroRow = new HBox(16);
        heroRow.setAlignment(Pos.CENTER_LEFT);
        // Q4: the action cluster moves below the greeting at compact widths instead of
        // clipping at the window edge (600px dashboard shots) — HBox cannot wrap, so the
        // hero actions live in a FlowPane that reflows when the width runs out.
        heroActionsWrap = new javafx.scene.layout.FlowPane(12, 8);
        heroActionsWrap.setMaxWidth(Double.MAX_VALUE);

        greetingBlock = new VBox(4);
        welcomeGreetingLabel = new Label("Good Morning, Administrator");
        welcomeGreetingLabel.getStyleClass().add("greeting-title");
        HBox statusRow = new HBox(12);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        occupancyBeacon = new M3PulseBeacon();
        occupancyBeacon.setStatus(M3PulseBeacon.BeaconStatus.ONLINE);
        occupancyBeacon.setAccessibleText("Live floor status beacon: online");
        capacityStatusLabel = new Label("Floor Occupancy: 82% [Optimal Dynamic Range]");
        capacityStatusLabel.getStyleClass().add("telemetry-subtitle");
        Label dividerPipe = new Label("|");
        dividerPipe.getStyleClass().add("divider-pipe");
        liveTimeLabel = new Label("10:42 AM");
        liveTimeLabel.getStyleClass().add("telemetry-subtitle");
        statusRow.getChildren().addAll(occupancyBeacon, capacityStatusLabel, dividerPipe, liveTimeLabel);
        greetingBlock.getChildren().addAll(welcomeGreetingLabel, statusRow);

        // Action controls bar.
        actionsBar = new HBox(12);
        actionsBar.setAlignment(Pos.CENTER_RIGHT);
        refreshButton = new M3Button("REFRESH TELEMETRY", ButtonVariant.TONAL);
        refreshIcon = M3Icon.symbol("refresh");
        refreshButton.setGraphic(refreshIcon);
        refreshButton.setOnAction(e -> refreshData());
        quickActionSplitBtn = new M3SplitButton("QUICK CHECK-IN", ButtonVariant.FILLED, null);
        logoutButton = new M3Button("END SHIFT", ButtonVariant.OUTLINED);
        logoutButton.setGraphic(M3Icon.symbol("close"));
        logoutButton.setOnAction(e -> onLogout());
        actionsBar.getChildren().addAll(refreshButton, quickActionSplitBtn, logoutButton);
        heroActionsWrap.getChildren().add(actionsBar);

        heroSpacer = new Region();
        HBox.setHgrow(heroSpacer, Priority.ALWAYS);
        heroRow.getChildren().addAll(greetingBlock, heroSpacer, heroActionsWrap);
        heroColumn = new VBox(12);
        heroColumn.setAlignment(Pos.CENTER_LEFT);
        heroBannerCard.setContent(heroRow);
        heroBannerCard.setPrefHeight(84);

        // BENTO ROW 1: 4 key metric stat cards. Desktop widths keep the stretching row; compact
        // widths swap the same cards into a 2x2 grid (below), so nothing is ever clipped.
        // Values fill from the database once the load task returns.
        kpiRow = new HBox(16);
        statActiveMembers = new M3StatCard("ACTIVE MEMBERS", "-", "of the loaded roster");
        statActiveMembers.getStyleClass().add("stat-card-teal");
        statDailyRevenue = new M3StatCard("TODAY'S REVENUE", "-", "from recorded payments");
        statDailyRevenue.getStyleClass().add("stat-card-azure");
        statTodaySessions = new M3StatCard("TODAY'S SESSIONS", "-", "Pending Confirmation");
        statTodaySessions.getStyleClass().add("stat-card-purple");
        statEquipmentHealth = new M3StatCard("EQUIPMENT OPERATIONAL", "-", "Units in Maintenance");
        statEquipmentHealth.getStyleClass().add("stat-card-coral");
        for (M3StatCard card : List.of(statActiveMembers, statDailyRevenue, statTodaySessions, statEquipmentHealth)) {
            HBox.setHgrow(card, Priority.ALWAYS);
        }
        kpiRow.getChildren().addAll(statActiveMembers, statDailyRevenue, statTodaySessions, statEquipmentHealth);
        kpiGrid = new GridPane();
        kpiGrid.setHgap(16);
        kpiGrid.setVgap(16);
        ColumnConstraints kpiColumn = new ColumnConstraints();
        kpiColumn.setPercentWidth(50);
        ColumnConstraints kpiColumnRight = new ColumnConstraints();
        kpiColumnRight.setPercentWidth(50);
        kpiGrid.getColumnConstraints().addAll(kpiColumn, kpiColumnRight);
        GridPane.setConstraints(statActiveMembers, 0, 0);
        GridPane.setConstraints(statDailyRevenue, 1, 0);
        GridPane.setConstraints(statTodaySessions, 0, 1);
        GridPane.setConstraints(statEquipmentHealth, 1, 1);

        // BENTO ROW 2: core visualizations (spline chart & agenda table).
        vizRow = new HBox(16);
        VBox.setVgrow(vizRow, Priority.ALWAYS);

        trafficCard = new M3Card(CardVariant.ELEVATED);
        trafficCard.setPrefWidth(640);
        trafficCard.setMinWidth(320);
        trafficCard.getStyleClass().add("chart-bento-card");
        HBox.setHgrow(trafficCard, Priority.ALWAYS);
        trafficCard.setPadding(new Insets(20, 24, 20, 24));
        VBox trafficContent = new VBox(16);
        HBox trafficHeader = new HBox();
        trafficHeader.setAlignment(Pos.CENTER_LEFT);
        VBox trafficTitles = new VBox(4);
        trafficTitles.getChildren().addAll(
                sectionTitle("Hourly Attendance & Floor Traffic"),
                sectionSubtitle("Real-time check-in density plotted against 14-day rolling average"));
        Region trafficSpacer = new Region();
        HBox.setHgrow(trafficSpacer, Priority.ALWAYS);
        trafficHeader.getChildren().addAll(trafficTitles, trafficSpacer,
                new M3Chip("LIVE VELOCITY: +8/MIN", ChipVariant.ASSIST));
        trafficSplineChart = new M3SplineChart();
        trafficSplineChart.setPrefHeight(260);
        VBox.setVgrow(trafficSplineChart, Priority.ALWAYS);
        trafficContent.getChildren().addAll(trafficHeader, trafficSplineChart);
        trafficCard.setContent(trafficContent);

        agendaCard = new M3Card(CardVariant.ELEVATED);
        agendaCard.setPrefWidth(600);
        agendaCard.setMinWidth(320);
        agendaCard.getStyleClass().add("table-bento-card");
        HBox.setHgrow(agendaCard, Priority.ALWAYS);
        agendaCard.setPadding(new Insets(20, 24, 20, 24));
        VBox agendaContent = new VBox(16);
        HBox agendaHeader = new HBox();
        agendaHeader.setAlignment(Pos.CENTER_LEFT);
        VBox agendaTitles = new VBox(4);
        agendaTitles.getChildren().addAll(
                sectionTitle("Today's Session Schedule"),
                sectionSubtitle("Click a pending row to confirm it; headers sort the agenda"));
        Region agendaSpacer = new Region();
        HBox.setHgrow(agendaSpacer, Priority.ALWAYS);
        agendaHeader.getChildren().addAll(agendaTitles, agendaSpacer);
        agendaTable = new M3DataTable<>();
        agendaTable.setPrefHeight(260);
        VBox.setVgrow(agendaTable, Priority.ALWAYS);
        agendaContent.getChildren().addAll(agendaHeader, agendaTable);
        agendaCard.setContent(agendaContent);

        vizRow.getChildren().addAll(trafficCard, agendaCard);

        // BENTO ROW 3: secondary facility diagnostics.
        diagnosticsRow = new HBox(16);

        donutCard = new M3Card(CardVariant.ELEVATED);
        donutCard.setPrefWidth(400);
        donutCard.setMinWidth(240);
        donutCard.getStyleClass().add("mini-bento-card");
        HBox.setHgrow(donutCard, Priority.ALWAYS);
        donutCard.setPadding(new Insets(16, 20, 16, 20));
        VBox donutContent = new VBox(12);
        equipmentDonutChart = new M3DonutChart();
        equipmentDonutChart.setPrefHeight(140);
        donutContent.getChildren().addAll(sectionTitle("Equipment Health Distribution"), equipmentDonutChart);
        donutCard.setContent(donutContent);

        barCard = new M3Card(CardVariant.ELEVATED);
        barCard.setPrefWidth(400);
        barCard.setMinWidth(240);
        barCard.getStyleClass().add("mini-bento-card");
        HBox.setHgrow(barCard, Priority.ALWAYS);
        barCard.setPadding(new Insets(16, 20, 16, 20));
        VBox barContent = new VBox(12);
        planBarChart = new M3BarChart();
        planBarChart.setPrefHeight(140);
        barContent.getChildren().addAll(sectionTitle("Active Membership Plans"), planBarChart);
        barCard.setContent(barContent);

        // 3D kinetic facility radar card.
        facilityRadarCard = new M3TiltCard3D();
        facilityRadarCard.setPrefSize(400, 180);
        facilityRadarCard.setMinWidth(240);
        facilityRadarCard.getStyleClass().add("mini-bento-card");
        HBox.setHgrow(facilityRadarCard, Priority.ALWAYS);
        facilityRadarCard.setPadding(new Insets(16, 20, 16, 20));
        VBox radarContent = new VBox(8);
        radarContent.getChildren().addAll(
                sectionTitle("Floor Density Radar"),
                sectionSubtitle("Live zone utilisation right now"));
        Region radarSpacer = new Region();
        VBox.setVgrow(radarSpacer, Priority.ALWAYS);
        radarContent.getChildren().add(radarSpacer);
        radarContent.getChildren().addAll(
                densityBar("Cardio", "90%", 0.90),
                densityBar("Free Weights", "75%", 0.75),
                densityBar("Studio", "40%", 0.40));
        facilityRadarCard.setContent(radarContent);

        diagnosticsRow.getChildren().addAll(donutCard, barCard, facilityRadarCard);

        // Compact counterparts of the bento rows: the SAME cards stacked full-width (a card
        // row cannot fit a 600dp window, and a clipped card is worse than a stacked one).
        // The containers start EMPTY — filling them here would steal the cards out of the
        // desktop rows; applyDashboardLayout moves the nodes on the first class swap.
        vizStack = new VBox(16);
        diagnosticsStack = new VBox(16);

        bentoGrid.getChildren().addAll(heroBannerCard, kpiRow, vizRow, diagnosticsRow);

        // Sparkline definitions for KPI telemetry cards (mounted into their cards below).
        memberSparkline = new M3Sparkline();
        revenueSparkline = new M3Sparkline();

        // FLOATING actions & toasts.
        quickActionSpeedDial = new M3SpeedDial();
        quickActionSpeedDial.setMaxSize(56, 56);
        quickActionSpeedDial.setPickOnBounds(false);
        StackPane.setAlignment(quickActionSpeedDial, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(quickActionSpeedDial, new Insets(0, 32, 32, 0));

        dashboardSnackbar = new StackPane();
        dashboardSnackbar.setPickOnBounds(false);
        dashboardSnackbar.setMouseTransparent(true);
        dashboardSnackbar.setMaxSize(480, 48);
        StackPane.setAlignment(dashboardSnackbar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(dashboardSnackbar, new Insets(0, 0, 24, 0));

        dashboardRoot = new StackPane();
        dashboardRoot.getStyleClass().add("dashboard-root");
        // The bento grid lives in a content host so the real-data load can swap it for
        // loading/error states without touching the floating chrome around it. The speed
        // dial pins to THIS host's bottom edge — the host is exactly the scroll content's
        // height, so the FAB always sits in the reserved band below the last bento row (a
        // root-pinned FAB would land mid-content wherever the scene clips the root).
        contentHost = new StackPane(bentoGrid, quickActionSpeedDial);
        dashboardRoot.getChildren().addAll(contentHost, dashboardSnackbar, overlay);

        // Width-class listener (library breakpoints, same pattern as the shell's
        // applyResponsiveChrome): swaps the hero and bento rows between the desktop and compact
        // layouts. Deferred with Platform.runLater so the children lists are never mutated
        // re-entrantly while bentoGrid is mid-layout.
        dashboardRoot.widthProperty().addListener((obs, oldWidth, width) -> {
            double w = width.doubleValue();
            if (w <= 0) {
                return;
            }
            WindowSizeClass next = WindowSizeClass.forWidth(w);
            if (next == dashboardWidthClass) {
                return;
            }
            dashboardWidthClass = next;
            javafx.application.Platform.runLater(this::applyDashboardLayout);
        });

        refreshGreetingAndClock();
        wireAgendaColumns();
        wireQuickCheckIn();
        loadDashboard();

    // fx:id parity: every node the markup used to tag with fx:id keeps its JavaFX id, so
    // CSS #id selectors, Node.lookup("#..."), the cross-screen rail navigation
    // (findById("navCustomers") and friends) and the interaction tests keep working unchanged.
        dashboardRoot.setId("dashboardRoot");
        heroBannerCard.setId("heroBannerCard");
        welcomeGreetingLabel.setId("welcomeGreetingLabel");
        occupancyBeacon.setId("occupancyBeacon");
        capacityStatusLabel.setId("capacityStatusLabel");
        liveTimeLabel.setId("liveTimeLabel");
        refreshButton.setId("refreshButton");
        refreshIcon.setId("refreshIcon");
        quickActionSplitBtn.setId("quickActionSplitBtn");
        logoutButton.setId("logoutButton");
        statActiveMembers.setId("statActiveMembers");
        memberSparkline.setId("memberSparkline");
        statDailyRevenue.setId("statDailyRevenue");
        revenueSparkline.setId("revenueSparkline");
        statTodaySessions.setId("statTodaySessions");
        statEquipmentHealth.setId("statEquipmentHealth");
        trafficSplineChart.setId("trafficSplineChart");
        agendaTable.setId("agendaTable");
        equipmentDonutChart.setId("equipmentDonutChart");
        planBarChart.setId("planBarChart");
        facilityRadarCard.setId("facilityRadarCard");
        quickActionSpeedDial.setId("quickActionSpeedDial");
        dashboardSnackbar.setId("dashboardSnackbar");
        wireSpeedDial();
    }

    /** One zone-density row of the 3D radar card. */
    private static HBox densityBar(String zone, String percent, double progress) {
        Label zoneLabel = new Label(zone);
        zoneLabel.setMinWidth(82);
        Label percentLabel = new Label(percent);
        percentLabel.setMinWidth(36);
        percentLabel.getStyleClass().add("m3-list-supporting");
        M3LinearProgress bar = new M3LinearProgress(progress);
        HBox.setHgrow(bar, Priority.ALWAYS);
        HBox row = new HBox(10, zoneLabel, percentLabel, bar);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("card-section-title");
        return label;
    }

    private static Label sectionSubtitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("card-section-subtitle");
        return label;
    }

    @Override
    public Parent getRoot() {
        return dashboardRoot;
    }

    /**
     * Answers the REFRESH TELEMETRY action: re-queries the database (fresh snapshot) and
     * confirms via snackbar — no simulated telemetry anywhere.
     */
    public void refreshData() {
        refreshGreetingAndClock();
        loadDashboard();
        overlay.showSnackbar("Telemetry refreshed at " + clockText());
    }

    /**
     * Ends the shift through a two-step confirm (destructive action), then signs out through
     * the owning shell: the shell routes into the Apple-style login gate
     * ({@code LaunchGateController#showLoginGate}) in real runs. Standalone previews and tests
     * without the shell keep the previous visible answer (button disables + snackbar).
     */
    public void onLogout() {
        M3Button cancel = M3Button.text("Stay on Shift");
        M3Button confirm = M3Button.text("End Shift");
        M3Dialog dialog = M3Dialog.basic("End the current shift?",
                new Label("Live telemetry stays on the wall display, but your operator session "
                        + "closes and check-ins hand over to the next administrator."),
                cancel, confirm);
        dialog.setDefaultAction(confirm::activate);
        confirm.setOnAction(e -> {
            dialog.hide();
            if (signOutViaShell()) {
                return;
            }
            logoutButton.setDisable(true);
            overlay.showSnackbar("Shift ended at " + clockText()
                    + " — check-ins handed to the next administrator.");
        });
        cancel.setOnAction(e -> dialog.hide());
        overlay.showDialog(dialog);
    }

    /**
     * Signs out via the shell controller, published in the scene-root properties by
     * {@link AppShellController} ({@code AppShellController.SHELL_CONTROLLER_KEY}): calling its
     * public {@code onLogout} routes into the launch gate exactly like the shell's own logout
     * button. Returns false when no shell is reachable (standalone preview / test).
     */
    private boolean signOutViaShell() {
        if (dashboardRoot.getScene() == null
                || !(dashboardRoot.getScene().getRoot() instanceof javafx.scene.Parent sceneRoot)) {
            return false;
        }
        Object shell = sceneRoot.getProperties().get(AppShellController.SHELL_CONTROLLER_KEY);
        if (shell instanceof AppShellController controller) {
            controller.onLogout();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Real telemetry: DAO snapshot on a background task (task C3)
    // ------------------------------------------------------------------

    /**
     * The immutable result of one background snapshot: every number, row and series the
     * dashboard renders, computed off the FX thread from the DAOs. Building it as a value
     * keeps all FX access inside {@link #applySnapshot}.
     */
    private record DashboardSnapshot(int totalMembers, int activeMembers,
                                     double revenueToday, int paymentsToday,
                                     List<Object[]> agendaRows, int pendingSessions,
                                     double[] todayHourlyLoad, double[] baselineHourlyLoad,
                                     double[] attendanceTrend, double[] revenueTrend,
                                     int equipmentAvailable, int equipmentInMaintenance,
                                     int equipmentRetired, int equipmentTotal,
                                     Map<PlanType, Integer> activePlans,
                                     int insideNow, int checkedInToday,
                                     List<Customer> checkableCustomers) {
    }

    /**
     * Loads the dashboard from the database on a background task: the bootstrap, the DAO
     * queries and the snapshot computation run off the FX thread; the result lands on the
     * cards, charts and agenda on the FX thread. Shows {@link UiKit#loadingState()} while
     * running and {@link UiKit#errorState()} with retry on failure.
     */
    private void loadDashboard() {
        Data.loadAsync(contentHost,
            () -> {
                Data.ensureReady();
                return buildSnapshot();
            },
            snapshot -> {
                contentHost.getChildren().set(0, bentoGrid);
                applySnapshot(snapshot);
            },
            error -> contentHost.getChildren().setAll(UiKit.errorState(
                    error.getMessage() == null ? error.toString() : error.getMessage(),
                    this::loadDashboard)));
    }

    /**
     * Queries the DAOs and computes every dashboard figure. Runs on the background thread —
     * no JavaFX types are touched here.
     */
    private DashboardSnapshot buildSnapshot() throws Exception {
        Data.ensureReady();
        LocalDate today = LocalDate.now();

        List<Customer> customers = new CustomerDao().findAll();
        List<Membership> memberships = new MembershipDao().findAll();
        List<Payment> payments = new PaymentDao().findAll();
        List<Session> sessions = new SessionDao().findAll();
        List<Trainer> trainers = new TrainerDao().findAll();
        List<Attendance> attendance = new AttendanceDao().findAll();
        List<com.gym.model.Equipment> equipment = new EquipmentDao().findAll();
        List<TrainerAvailability> slots = new TrainerAvailabilityDao().findAll();

        int activeMembers = 0;
        Map<Integer, String> customerNames = new HashMap<>();
        for (Customer customer : customers) {
            customerNames.put(customer.getId(), customer.getFullName());
            if (customer.getStatus() == CustomerStatus.ACTIVE) {
                activeMembers++;
            }
        }

        // Revenue today (voided payments never count, rules 6/19) and the 14-day revenue trend.
        double revenueToday = 0;
        int paymentsToday = 0;
        double[] revenueTrend = new double[14];
        for (Payment payment : payments) {
            if (payment.isVoided() || payment.getDate() == null) {
                continue;
            }
            long daysAgo = ChronoUnit.DAYS.between(LocalDate.parse(payment.getDate()), today);
            if (daysAgo == 0) {
                revenueToday += payment.getAmount().doubleValue();
                paymentsToday++;
            }
            if (daysAgo >= 0 && daysAgo < 14) {
                revenueTrend[13 - (int) daysAgo] += payment.getAmount().doubleValue();
            }
        }

        // Attendance: today's hourly load, the trailing-14-day hourly baseline, the daily
        // trend and the live occupancy figures.
        double[] todayHourly = new double[15];
        double[] baselineHourly = new double[15];
        double[] attendanceTrend = new double[14];
        int checkedInToday = 0;
        int insideNow = 0;
        for (Attendance record : attendance) {
            if (record.getDate() == null || record.getCheckIn() == null) {
                continue;
            }
            LocalDate date = LocalDate.parse(record.getDate());
            long daysAgo = ChronoUnit.DAYS.between(date, today);
            int hourBucket = hourBucket(record.getCheckIn());
            if (daysAgo == 0) {
                checkedInToday++;
                if (record.getCheckOut() == null || record.getCheckOut().isBlank()) {
                    insideNow++;
                }
                todayHourly[hourBucket] += 1;
            }
            if (daysAgo >= 1 && daysAgo < 15) {
                baselineHourly[hourBucket] += 1;
            }
            if (daysAgo >= 0 && daysAgo < 14) {
                attendanceTrend[13 - (int) daysAgo] += 1;
            }
        }
        for (int i = 0; i < baselineHourly.length; i++) {
            baselineHourly[i] = Math.round(baselineHourly[i] / 14.0 * 10.0) / 10.0;
        }

        // Sessions today: rule 17 capacity counts CONFIRMED occupancy per published slot only.
        Map<Integer, String> trainerNames = new HashMap<>();
        for (Trainer trainer : trainers) {
            trainerNames.put(trainer.getId(), trainer.getFullName());
        }
        Map<Integer, Integer> slotCaps = new HashMap<>();
        for (TrainerAvailability slot : slots) {
            slotCaps.put(slot.getId(), slot.getMaxCustomers());
        }
        Map<Integer, Integer> confirmedBySlot = new HashMap<>();
        List<Session> todaysSessions = new ArrayList<>();
        for (Session session : sessions) {
            if (today.toString().equals(session.getDate())) {
                todaysSessions.add(session);
            }
            if (session.getStatus() == SessionStatus.CONFIRMED
                    && session.getAvailabilityId() != null) {
                confirmedBySlot.merge(session.getAvailabilityId(), 1, Integer::sum);
            }
        }
        todaysSessions.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));
        List<Object[]> agendaRows = new ArrayList<>();
        int pendingSessions = 0;
        for (Session session : todaysSessions) {
            String status = sessionStatusLabel(session.getStatus());
            if (session.getStatus() == SessionStatus.REQUESTED) {
                pendingSessions++;
            }
            String capacity = "—";
            if (session.getAvailabilityId() != null
                    && slotCaps.containsKey(session.getAvailabilityId())) {
                int occupied = confirmedBySlot.getOrDefault(session.getAvailabilityId(), 0);
                capacity = occupied + " / " + slotCaps.get(session.getAvailabilityId());
            }
            agendaRows.add(new Object[] {
                    session.getStartTime(),
                    customerNames.getOrDefault(session.getCustomerId(),
                            "Member #" + session.getCustomerId()),
                    trainerNames.getOrDefault(session.getTrainerId(),
                            "Coach #" + session.getTrainerId()),
                    capacity,
                    status,
                    session.getId()});
        }

        // Equipment health and active plan distribution (active = unexpired ACTIVE).
        int equipmentTotal = equipment.size();
        int equipmentAvailable = 0;
        int equipmentInMaintenance = 0;
        int equipmentRetired = 0;
        for (com.gym.model.Equipment item : equipment) {
            switch (item.getStatus()) {
                case AVAILABLE -> equipmentAvailable++;
                case IN_MAINTENANCE -> equipmentInMaintenance++;
                case RETIRED -> equipmentRetired++;
            }
        }
        Map<PlanType, Integer> activePlans = new HashMap<>();
        for (Membership membership : memberships) {
            if (membership.getStatus() == MembershipStatus.ACTIVE
                    && membership.getEndDate() != null
                    && !LocalDate.parse(membership.getEndDate()).isBefore(today)) {
                activePlans.merge(membership.getPlan(), 1, Integer::sum);
            }
        }

        // Rule-5 check-in candidates: ACTIVE customer with an unexpired ACTIVE membership.
        List<Customer> checkable = new ArrayList<>();
        for (Customer customer : customers) {
            if (customer.getStatus() != CustomerStatus.ACTIVE) {
                continue;
            }
            for (Membership membership : memberships) {
                if (membership.getCustomerId() == customer.getId()
                        && membership.getStatus() == MembershipStatus.ACTIVE
                        && membership.getEndDate() != null
                        && !LocalDate.parse(membership.getEndDate()).isBefore(today)) {
                    checkable.add(customer);
                    break;
                }
            }
        }

        return new DashboardSnapshot(customers.size(), activeMembers, revenueToday,
                paymentsToday, agendaRows, pendingSessions, todayHourly, baselineHourly,
                attendanceTrend, revenueTrend, equipmentAvailable, equipmentInMaintenance,
                equipmentRetired, equipmentTotal, activePlans, insideNow, checkedInToday,
                checkable);
    }

    /** Buckets a check-in time (HH:mm) into the 07:00-21:00 chart hour (clamped). */
    private static int hourBucket(String checkIn) {
        int hour;
        try {
            hour = Integer.parseInt(checkIn.trim().substring(0, 2));
        } catch (RuntimeException e) {
            return 0;
        }
        return Math.max(0, Math.min(14, hour - 7));
    }

    /** User-facing agenda copy for a session status (sentence case, no enum shout). */
    private static String sessionStatusLabel(SessionStatus status) {
        return switch (status) {
            case REQUESTED -> "Pending";
            case CONFIRMED -> "Confirmed";
            case COMPLETED -> "Completed";
            case NO_SHOW -> "No-show";
            case CANCELLED -> "Cancelled";
        };
    }

    /** Applies a finished snapshot to the cards, charts, agenda and telemetry label. */
    private void applySnapshot(DashboardSnapshot snapshot) {
        checkableCustomers = snapshot.checkableCustomers();

        statActiveMembers.setValue(String.valueOf(snapshot.activeMembers()));
        statActiveMembers.setDeltaText("of " + snapshot.totalMembers() + " members on roster");
        statDailyRevenue.setValue(String.format("$%,.2f", snapshot.revenueToday()));
        statDailyRevenue.setDeltaText("from " + snapshot.paymentsToday() + " payments today");
        statTodaySessions.setValue(snapshot.agendaRows().size() + " today");
        statTodaySessions.setDeltaText(snapshot.pendingSessions() + " Pending Confirmation");
        double operational = snapshot.equipmentTotal() == 0 ? 0
                : snapshot.equipmentAvailable() * 100.0 / snapshot.equipmentTotal();
        statEquipmentHealth.setValue(String.format("%.1f%%", operational));
        lastMaintenanceUnits = snapshot.equipmentInMaintenance();
        applyEquipmentKpi(dashboardWidthClass == WindowSizeClass.COMPACT);

        refreshGreetingAndClock();
        capacityStatusLabel.setText("Floor Occupancy: " + snapshot.insideNow() + " of "
                + snapshot.activeMembers() + " members inside · " + snapshot.checkedInToday()
                + " checked in today");

        mountSparkline(statActiveMembers, memberSparkline, snapshot.attendanceTrend());
        mountSparkline(statDailyRevenue, revenueSparkline, snapshot.revenueTrend());

        trafficSplineChart.getSeriesList().setAll(
                M3DataSeries.of("Today", snapshot.todayHourlyLoad()),
                M3DataSeries.of("14-day average", snapshot.baselineHourlyLoad()));

        agendaTable.getItems().setAll(snapshot.agendaRows());

        populateEquipmentDonut(snapshot);
        populatePlanBarChart(snapshot);
    }

    private void refreshGreetingAndClock() {
        int hour = LocalTime.now().getHour();
        String dayPart = hour < 12 ? "Morning" : hour < 18 ? "Afternoon" : "Evening";
        welcomeGreetingLabel.setText("Good " + dayPart + ", Administrator");
        liveTimeLabel.setText(clockText());
    }

    private String clockText() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"));
    }

    /** Mounts one sparkline inside its KPI card with the given real history. */
    private void mountSparkline(M3StatCard card, M3Sparkline sparkline, double... values) {
        sparkline.getData().clear();
        for (double value : values) {
            sparkline.getData().add(value);
        }
        sparkline.setPrefSize(110, 26);
        sparkline.setMaxWidth(110);
        if (card.getContent() instanceof VBox layoutBox
                && !layoutBox.getChildren().contains(sparkline)) {
            layoutBox.getChildren().add(sparkline);
        }
    }

    /** Wires the agenda columns and the confirm row action; rows come from the snapshot. */
    private void wireAgendaColumns() {
        agendaTable.getColumns().add(new M3DataTable.Column<>("Time", row -> (String) row[0], 76.0));
        agendaTable.getColumns().add(new M3DataTable.Column<>("Session", row -> (String) row[1], 190.0));
        agendaTable.getColumns().add(new M3DataTable.Column<>("Coach", row -> (String) row[2], 150.0));
        agendaTable.getColumns().add(new M3DataTable.Column<>("Capacity", row -> (String) row[3], 92.0));
        agendaTable.getColumns().add(new M3DataTable.Column<>("Status", row -> (String) row[4], 104.0));
        agendaTable.setOnRowAction(this::confirmPendingSession);
    }

    /**
     * Confirms a pending (REQUESTED) session through {@code SessionDao} under rule 18 — the
     * DAO owns the transition check, the UI renders its answer and re-queries so the row, the
     * capacity and the KPI delta all reflect the database.
     */
    private void confirmPendingSession(Object[] row) {
        if (!"Pending".equals(row[4])) {
            overlay.showSnackbar(row[1] + " at " + row[0] + " is already " + row[4] + ".");
            return;
        }
        int sessionId = (Integer) row[5];
        String description = row[1] + " at " + row[0] + " with " + row[2];
        Data.submit(
                () -> {
                    Session session = new SessionDao().findById(sessionId);
                    if (session == null) {
                        throw new com.gym.exception.ValidationException(
                                "Session no longer exists in the database");
                    }
                    session.setStatus(SessionStatus.CONFIRMED);
                    new SessionDao().update(session);
                    return session;
                },
                confirmed -> {
                    overlay.showSnackbar("Session confirmed: " + description + ".");
                    loadDashboard();
                },
                error -> overlay.showSnackbar("Could not confirm session: " + error.getMessage()));
    }

    /** Singular/plural maintenance delta copy ("1 Unit in Maintenance"). */
    private static String maintenanceUnitsLabel(int units) {
        return units + (units == 1 ? " Unit in Maintenance" : " Units in Maintenance");
    }

    /**
     * Applies the equipment KPI's title and delta for the current width class: the compact
     * cards carry the short pair ("EQUIPMENT HEALTH" / "1 Unit") because the full pair
     * slivers the title mid-word on a narrow card — the full delta copy stays reachable
     * through the stat card's own tooltips.
     */
    private void applyEquipmentKpi(boolean compact) {
        statEquipmentHealth.setTitle(compact ? "EQUIPMENT HEALTH" : "EQUIPMENT OPERATIONAL");
        int units = lastMaintenanceUnits;
        statEquipmentHealth.setDeltaText(compact
                ? units + (units == 1 ? " Unit" : " Units")
                : maintenanceUnitsLabel(units));
    }

    /**
     * Applies the current width class to the hero and bento rows by swapping between two
     * layouts that SHARE the same nodes (the M3AdaptiveScaffold swap pattern): desktop widths
     * keep the side-by-side hero and stretching card rows, compact widths stack the hero and
     * the cards full-width, because a card row cannot fit a 600dp window and a clipped card
     * reads as broken. Idempotent fills, so repeated swaps are safe.
     */
    private void applyDashboardLayout() {
        boolean compact = dashboardWidthClass == WindowSizeClass.COMPACT;
        applyEquipmentKpi(compact);
        if (compact) {
            heroColumn.getChildren().setAll(greetingBlock, actionsBar);
            heroBannerCard.setContent(heroColumn);
            heroBannerCard.setPrefHeight(124);
        } else {
            heroRow.getChildren().setAll(greetingBlock, heroSpacer, actionsBar);
            heroBannerCard.setContent(heroRow);
            heroBannerCard.setPrefHeight(84);
        }
        if (compact) {
            kpiGrid.getChildren().setAll(statActiveMembers, statDailyRevenue,
                    statTodaySessions, statEquipmentHealth);
            vizStack.getChildren().setAll(trafficCard, agendaCard);
            diagnosticsStack.getChildren().setAll(donutCard, barCard, facilityRadarCard);
            bentoGrid.getChildren().set(1, kpiGrid);
            bentoGrid.getChildren().set(2, vizStack);
            bentoGrid.getChildren().set(3, diagnosticsStack);
            return;
        }
        kpiRow.getChildren().setAll(statActiveMembers, statDailyRevenue,
                statTodaySessions, statEquipmentHealth);
        vizRow.getChildren().setAll(trafficCard, agendaCard);
        diagnosticsRow.getChildren().setAll(donutCard, barCard, facilityRadarCard);
        bentoGrid.getChildren().set(1, kpiRow);
        bentoGrid.getChildren().set(2, vizRow);
        bentoGrid.getChildren().set(3, diagnosticsRow);
    }

    private void populateEquipmentDonut(DashboardSnapshot snapshot) {
        if (snapshot.equipmentTotal() == 0) {
            equipmentDonutChart.setCenterHeadline("—");
            equipmentDonutChart.setCenterSubtitle("No equipment recorded");
            equipmentDonutChart.getRings().clear();
            return;
        }
        equipmentDonutChart.setCenterHeadline(String.format("%.1f%%",
                snapshot.equipmentAvailable() * 100.0 / snapshot.equipmentTotal()));
        equipmentDonutChart.setCenterSubtitle("Operational");
        equipmentDonutChart.getRings().clear();
        // Semantic role mapping, no hardcoded hex: available = the success state → the tertiary
        // accent (green family under this seed), in-maintenance = the problem state → the error
        // role. Retired units close the ring in the neutral secondary accent.
        equipmentDonutChart.addRing("Operational", snapshot.equipmentAvailable(),
                snapshot.equipmentTotal(), UiKit.themeRole(ColorRole.TERTIARY));
        equipmentDonutChart.addRing("In Maintenance", snapshot.equipmentInMaintenance(),
                snapshot.equipmentTotal(), UiKit.themeRole(ColorRole.ERROR));
        if (snapshot.equipmentRetired() > 0) {
            equipmentDonutChart.addRing("Retired", snapshot.equipmentRetired(),
                    snapshot.equipmentTotal(), UiKit.themeRole(ColorRole.SECONDARY));
        }
    }

    private void populatePlanBarChart(DashboardSnapshot snapshot) {
        M3DataSeries plans = new M3DataSeries("Active plans");
        plans.getData().addAll(List.of(
                new M3DataPoint("Monthly",
                        snapshot.activePlans().getOrDefault(PlanType.MONTHLY, 0)),
                new M3DataPoint("Quarterly",
                        snapshot.activePlans().getOrDefault(PlanType.QUARTERLY, 0)),
                new M3DataPoint("Annual",
                        snapshot.activePlans().getOrDefault(PlanType.YEARLY, 0))));
        planBarChart.getSeriesList().setAll(plans);
    }

    /** Quick check-in: the split button's leading action opens the eligible-member menu. */
    private void wireQuickCheckIn() {
        quickActionSplitBtn.setOnAction(e -> openCheckInMenu(quickActionSplitBtn));
        quickActionSplitBtn.setMenu(null);
    }

    /**
     * The check-in menu lists the rule-5 eligible members at the last load (ACTIVE customer
     * with an unexpired ACTIVE membership). Choosing one runs the real check-in through
     * {@code AttendanceDao}; without eligible members the answer says exactly that.
     */
    private void openCheckInMenu(Node anchor) {
        M3Menu menu = new M3Menu();
        if (checkableCustomers.isEmpty()) {
            menu.item("No members are eligible to check in right now", () -> { });
        } else {
            for (Customer customer : checkableCustomers) {
                menu.item(customer.getFullName() + " · " + customer.getPhone(),
                        () -> checkInCustomer(customer));
            }
        }
        if (anchor.getScene() != null && anchor.getScene().getWindow() != null) {
            menu.showBelow(anchor);
        } else {
            overlay.showSnackbar("Check-in opens inside the app window.");
        }
    }

    /**
     * Rule-5 check-in through {@code AttendanceDao}: an ACTIVE customer with an unexpired
     * membership checks in; a same-day re-check-in updates the existing row instead of
     * inserting a duplicate. Success re-queries the dashboard so occupancy and trends follow.
     */
    private void checkInCustomer(Customer customer) {
        Data.submit(
                () -> {
                    String now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                    String today = LocalDate.now().toString();
                    AttendanceDao attendanceDao = new AttendanceDao();
                    for (Attendance existing : attendanceDao.findByCustomerId(customer.getId())) {
                        if (today.equals(existing.getDate())) {
                            existing.setCheckIn(now);
                            attendanceDao.update(existing);
                            return "updated";
                        }
                    }
                    attendanceDao.insert(new Attendance(0, customer.getId(), today, now, null));
                    return "checked in";
                },
                outcome -> {
                    overlay.showSnackbar(customer.getFullName() + (outcome.equals("updated")
                            ? " re-checked in — the same-day row was updated (rule 5)."
                            : " checked in at " + clockText() + "."));
                    loadDashboard();
                },
                error -> overlay.showSnackbar("Could not check in "
                        + customer.getFullName() + ": " + error.getMessage()));
    }

    /** The floating speed dial mirrors the two real dashboard answers. */
    private void wireSpeedDial() {
        quickActionSpeedDial.addAction("Quick Check-In", M3Icon.symbol("person"),
                () -> openCheckInMenu(quickActionSpeedDial));
        quickActionSpeedDial.addAction("Refresh Telemetry", M3Icon.symbol("refresh"), this::refreshData);
    }
}
