package com.gym.controller;

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
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.menu.M3Menu;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.progress.M3LinearProgress;
import io.m3fx.controls.scene3d.M3TiltCard3D;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * Controller for Executive Operations Dashboard.
 * Conforms to docs/ui/dashboard/DASHBOARD_UI.md Section 7 contract.
 * Pure UI: strictly no backend service, DAO, or database logic.
 *
 * <p>Every visible action answers: telemetry refresh and the speed dial / quick check-in
 * mutate the on-screen telemetry and confirm via an {@link M3Overlay} snackbar; the destructive
 * END SHIFT action confirms through an {@link M3Dialog} first.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former dashboard.fxml: hero banner, KPI bento, traffic/agenda split, facility diagnostics,
 * floating speed dial and toast host.
 */
public class DashboardController implements ScreenController {

    private static final int SESSIONS_DONE_AT_LOAD = 38;
    private static final int SESSIONS_TOTAL = 46;

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

    /** Overlay host for snackbars and dialogs, added to the screen root in code (no fx:id). */
    private final M3Overlay overlay = new M3Overlay();
    /** Running session counters so the check-in answer updates the visible KPI coherently. */
    private int sessionsDone = SESSIONS_DONE_AT_LOAD;
    /** Day-wide pending confirmations shown on the sessions KPI (8 at load). */
    private int pendingConfirmations = 8;
    /** Deterministic jitter source for the refresh answer. */
    private final Random telemetryNoise = new Random();

    /**
     * Builds the complete Executive Operations Dashboard and initializes all behavior. Same
     * ordering semantics the FXMLLoader used to provide: the node tree exists and every field
     * is assigned before the former {@code initialize()} work runs.
     */
    public DashboardController() {
        // Main bento grid container.
        VBox bentoGrid = new VBox(20);
        bentoGrid.setPadding(new Insets(20, 24, 112, 24));

        // TOP: hero header banner.
        heroBannerCard = new M3Card(CardVariant.ELEVATED);
        heroBannerCard.setPrefHeight(84);
        heroBannerCard.getStyleClass().add("hero-banner");
        heroBannerCard.setPadding(new Insets(16, 24, 16, 24));
        HBox heroContent = new HBox(16);
        heroContent.setAlignment(Pos.CENTER_LEFT);

        VBox greetingBlock = new VBox(4);
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
        HBox actionsBar = new HBox(12);
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

        Region heroSpacer = new Region();
        HBox.setHgrow(heroSpacer, Priority.ALWAYS);
        heroContent.getChildren().addAll(greetingBlock, heroSpacer, actionsBar);
        heroBannerCard.setContent(heroContent);

        // BENTO ROW 1: 4 key metric stat cards.
        HBox kpiRow = new HBox(16);
        statActiveMembers = new M3StatCard("ACTIVE MEMBERS", "1,428", "+4.2% vs last month");
        statActiveMembers.getStyleClass().add("stat-card-teal");
        statDailyRevenue = new M3StatCard("TODAY'S REVENUE", "$3,840.00", "Target: $4,500.00 (85%)");
        statDailyRevenue.getStyleClass().add("stat-card-azure");
        statTodaySessions = new M3StatCard("TODAY'S SESSIONS", "38 / 46", "8 Pending Confirmation");
        statTodaySessions.getStyleClass().add("stat-card-purple");
        statEquipmentHealth = new M3StatCard("EQUIPMENT OPERATIONAL", "94.2%", "2 Units in Maintenance");
        statEquipmentHealth.getStyleClass().add("stat-card-coral");
        for (M3StatCard card : List.of(statActiveMembers, statDailyRevenue, statTodaySessions, statEquipmentHealth)) {
            HBox.setHgrow(card, Priority.ALWAYS);
        }
        kpiRow.getChildren().addAll(statActiveMembers, statDailyRevenue, statTodaySessions, statEquipmentHealth);

        // BENTO ROW 2: core visualizations (spline chart & agenda table).
        HBox vizRow = new HBox(16);
        VBox.setVgrow(vizRow, Priority.ALWAYS);

        M3Card trafficCard = new M3Card(CardVariant.ELEVATED);
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

        M3Card agendaCard = new M3Card(CardVariant.ELEVATED);
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
        HBox diagnosticsRow = new HBox(16);

        M3Card donutCard = new M3Card(CardVariant.ELEVATED);
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

        M3Card barCard = new M3Card(CardVariant.ELEVATED);
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
        dashboardRoot.getChildren().addAll(bentoGrid, quickActionSpeedDial, dashboardSnackbar, overlay);

        refreshGreetingAndClock();
        attachTelemetrySparklines();
        populateTrafficChart();
        populateAgendaTable();
        populateEquipmentDonut();
        populatePlanBarChart();
        wireQuickCheckIn();

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
     * Answers the REFRESH TELEMETRY action: advances the live clock and occupancy readout,
     * redraws the hourly traffic chart, and confirms via snackbar.
     */
    public void refreshData() {
        refreshGreetingAndClock();
        int occupancy = 76 + telemetryNoise.nextInt(13);
        capacityStatusLabel.setText("Floor Occupancy: " + occupancy
                + "% [Optimal Dynamic Range]");
        populateTrafficChart();
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
    // Demo telemetry wiring (no backend: static demo data)
    // ------------------------------------------------------------------

    private void refreshGreetingAndClock() {
        int hour = LocalTime.now().getHour();
        String dayPart = hour < 12 ? "Morning" : hour < 18 ? "Afternoon" : "Evening";
        welcomeGreetingLabel.setText("Good " + dayPart + ", Administrator");
        liveTimeLabel.setText(clockText());
    }

    private String clockText() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"));
    }

    /** Mounts the two sparklines inside their KPI cards with real history. */
    private void attachTelemetrySparklines() {
        mountSparkline(statActiveMembers, memberSparkline,
                1385, 1392, 1401, 1398, 1410, 1404, 1417, 1428);
        mountSparkline(statDailyRevenue, revenueSparkline,
                2820, 3150, 2960, 3410, 3260, 3690, 3510, 3840);
    }

    private void mountSparkline(M3StatCard card, M3Sparkline sparkline, double... values) {
        sparkline.getData().clear();
        for (double value : values) {
            sparkline.getData().add(value);
        }
        sparkline.setPrefSize(110, 26);
        sparkline.setMaxWidth(110);
        if (card.getContent() instanceof VBox layoutBox) {
            layoutBox.getChildren().add(sparkline);
        }
    }

    private void populateTrafficChart() {
        M3DataSeries today = M3DataSeries.of("Today", hourlyLoad(58));
        M3DataSeries baseline = M3DataSeries.of("14-day average", hourlyLoad(46));
        trafficSplineChart.getSeriesList().setAll(today, baseline);
    }

    /** Smooth 07:00–21:00 attendance curve around the given midday peak. */
    private double[] hourlyLoad(double peak) {
        double[] base = {peak * 0.28, peak * 0.44, peak * 0.72, peak * 0.95, peak,
                peak * 0.86, peak * 0.64, peak * 0.78, peak * 0.92, peak * 0.70,
                peak * 0.48, peak * 0.36, peak * 0.30, peak * 0.24, peak * 0.18};
        double[] jittered = new double[base.length];
        for (int i = 0; i < base.length; i++) {
            jittered[i] = Math.round(base[i] * (0.92 + telemetryNoise.nextDouble() * 0.16));
        }
        return jittered;
    }

    /**
     * Builds the session agenda: real columns, demo rows, and a row action that confirms
     * pending sessions — the visible answer updates the row, the sessions KPI and a snackbar.
     */
    private void populateAgendaTable() {
        agendaTable.getColumns().add(new M3DataTable.Column<>("Time", row -> (String) row[0], 76.0));
        agendaTable.getColumns().add(new M3DataTable.Column<>("Session", row -> (String) row[1], 190.0));
        agendaTable.getColumns().add(new M3DataTable.Column<>("Coach", row -> (String) row[2], 150.0));
        agendaTable.getColumns().add(new M3DataTable.Column<>("Capacity", row -> (String) row[3], 92.0));
        agendaTable.getColumns().add(new M3DataTable.Column<>("Status", row -> (String) row[4], 104.0));

        agendaTable.getItems().setAll(List.of(
                new Object[] {"07:00", "HIIT Circuit", "Coach Sarah", "14 / 16", "Confirmed"},
                new Object[] {"08:30", "Powerlifting Club", "Coach Marcus", "10 / 12", "Confirmed"},
                new Object[] {"10:00", "Senior Mobility", "Coach Elena", "8 / 10", "Confirmed"},
                new Object[] {"12:30", "Express Lunch Cardio", "Coach Sarah", "11 / 14", "Confirmed"},
                new Object[] {"14:00", "VBT Strength Lab", "Coach Marcus", "6 / 8", "Pending"},
                new Object[] {"17:30", "Spin Interval", "Coach Elena", "18 / 20", "Pending"},
                new Object[] {"19:00", "Boxing Fundamentals", "Coach Yusuf", "12 / 16", "Confirmed"},
                new Object[] {"20:30", "Open Mat Recovery", "Coach Yusuf", "9 / 12", "Confirmed"}));

        agendaTable.setOnRowAction(row -> confirmPendingSession(agendaTable, row));
    }

    private void confirmPendingSession(M3DataTable<Object[]> table, Object[] row) {
        if (!"Pending".equals(row[4])) {
            overlay.showSnackbar(row[1] + " at " + row[0] + " is already " + row[4] + ".");
            return;
        }
        row[4] = "Confirmed";
        table.getItems().set(table.getItems().indexOf(row), row);
        pendingConfirmations = Math.max(0, pendingConfirmations - 1);
        statTodaySessions.setDeltaText(pendingConfirmations + " Pending Confirmation");
        overlay.showSnackbar("Session confirmed: " + row[1] + " at " + row[0]
                + " with " + row[2] + ".");
    }

    private void populateEquipmentDonut() {
        equipmentDonutChart.setCenterHeadline("94.2%");
        equipmentDonutChart.setCenterSubtitle("Operational");
        equipmentDonutChart.addRing("Operational", 26, 28, Color.web("#4C8C4A"));
        equipmentDonutChart.addRing("In Maintenance", 2, 28, Color.web("#C2543B"));
    }

    private void populatePlanBarChart() {
        M3DataSeries plans = new M3DataSeries("Active members");
        plans.getData().addAll(List.of(
                new M3DataPoint("Monthly", 612),
                new M3DataPoint("Quarterly", 298),
                new M3DataPoint("Annual", 421),
                new M3DataPoint("Student", 97)));
        planBarChart.getSeriesList().setAll(plans);
    }

    /** Quick check-in: the split button's leading action plus its two menu shortcuts. */
    private void wireQuickCheckIn() {
        quickActionSplitBtn.setOnAction(e -> checkIn("Walk-in member"));
        M3Menu menu = new M3Menu();
        menu.item("Check in walk-in member", () -> checkIn("Walk-in member"));
        menu.item("Check in class attendee", () -> checkIn("Class attendee"));
        quickActionSplitBtn.setMenu(menu);
    }

    private void checkIn(String kind) {
        if (sessionsDone >= SESSIONS_TOTAL) {
            overlay.showSnackbar("All " + SESSIONS_TOTAL + " sessions today are checked in.");
            return;
        }
        sessionsDone++;
        statTodaySessions.setValue(sessionsDone + " / " + SESSIONS_TOTAL);
        overlay.showSnackbar(kind + " checked in — session " + sessionsDone
                + " of " + SESSIONS_TOTAL + " today.");
    }

    /** The floating speed dial mirrors the two real dashboard answers. */
    private void wireSpeedDial() {
        quickActionSpeedDial.addAction("Quick Check-In", M3Icon.symbol("person"), () -> checkIn("Walk-in member"));
        quickActionSpeedDial.addAction("Refresh Telemetry", M3Icon.symbol("refresh"), this::refreshData);
    }
}
