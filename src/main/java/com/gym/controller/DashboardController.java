package com.gym.controller;

import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.button.M3SplitButton;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chart.M3BarChart;
import io.m3fx.controls.chart.M3DataPoint;
import io.m3fx.controls.chart.M3DataSeries;
import io.m3fx.controls.chart.M3DonutChart;
import io.m3fx.controls.chart.M3Sparkline;
import io.m3fx.controls.chart.M3SplineChart;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.data.M3SpeedDial;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.effect.M3PulseBeacon;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.menu.M3Menu;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.scene3d.M3TiltCard3D;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * Controller for Executive Operations Dashboard (dashboard.fxml).
 * Conforms to docs/ui/dashboard/DASHBOARD_UI.md Section 7 contract.
 * Pure UI: strictly no backend service, DAO, or database logic.
 *
 * <p>Every visible action answers: telemetry refresh and the speed dial / quick check-in
 * mutate the on-screen telemetry and confirm via an {@link M3Snackbar}; the destructive
 * END SHIFT action confirms through an {@link M3Dialog} first.
 */
public class DashboardController {

    private static final int SESSIONS_DONE_AT_LOAD = 38;
    private static final int SESSIONS_TOTAL = 46;

    @FXML private StackPane dashboardRoot;
    @FXML private M3Card heroBannerCard;
    @FXML private Label welcomeGreetingLabel;
    @FXML private M3PulseBeacon occupancyBeacon;
    @FXML private Label capacityStatusLabel;
    @FXML private Label liveTimeLabel;
    @FXML private M3Button refreshButton;
    @FXML private M3Icon refreshIcon;
    @FXML private M3SplitButton quickActionSplitBtn;
    @FXML private M3Button logoutButton;

    @FXML private M3StatCard statActiveMembers;
    @FXML private M3Sparkline memberSparkline;
    @FXML private M3StatCard statDailyRevenue;
    @FXML private M3Sparkline revenueSparkline;
    @FXML private M3StatCard statTodaySessions;
    @FXML private M3StatCard statEquipmentHealth;

    @FXML private M3SplineChart trafficSplineChart;
    @FXML private M3DataTable<?> agendaTable;
    @FXML private M3DonutChart equipmentDonutChart;
    @FXML private M3BarChart planBarChart;
    @FXML private M3TiltCard3D facilityRadarCard;

    @FXML private M3SpeedDial quickActionSpeedDial;
    @FXML private StackPane dashboardSnackbar;

    /** Overlay host for snackbars and dialogs, added to the screen root in code (no fx:id). */
    private M3Overlay overlay;
    /** Running session counters so the check-in answer updates the visible KPI coherently. */
    private int sessionsDone = SESSIONS_DONE_AT_LOAD;
    /** Day-wide pending confirmations shown on the sessions KPI (8 at load). */
    private int pendingConfirmations = 8;
    /** Deterministic jitter source for the refresh answer. */
    private final Random telemetryNoise = new Random();

    public DashboardController() {}

    @FXML
    public void initialize() {
        overlay = new M3Overlay();
        dashboardRoot.getChildren().add(overlay);

        refreshGreetingAndClock();
        attachTelemetrySparklines();
        populateTrafficChart();
        populateAgendaTable();
        populateEquipmentDonut();
        populatePlanBarChart();
        wireQuickCheckIn();
        wireSpeedDial();
    }

    /**
     * Answers the REFRESH TELEMETRY action: advances the live clock and occupancy readout,
     * redraws the hourly traffic chart, and confirms via snackbar.
     */
    @FXML
    public void refreshData() {
        refreshGreetingAndClock();
        int occupancy = 76 + telemetryNoise.nextInt(13);
        capacityStatusLabel.setText("Floor Occupancy: " + occupancy
                + "% [Optimal Dynamic Range]");
        populateTrafficChart();
        overlay.showSnackbar("Telemetry refreshed at " + clockText());
    }

    /**
     * Ends the shift through a two-step confirm (destructive action) and answers with a
     * snackbar; the button disables so the completed state is visible.
     */
    @FXML
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
            logoutButton.setDisable(true);
            overlay.showSnackbar("Shift ended at " + clockText()
                    + " — check-ins handed to the next administrator.");
        });
        cancel.setOnAction(e -> dialog.hide());
        overlay.showDialog(dialog);
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

    /** Mounts the two FXML-declared sparklines inside their KPI cards with real history. */
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

    @SuppressWarnings("unchecked")
    private M3DataTable<Object[]> agenda() {
        return (M3DataTable<Object[]>) (M3DataTable<?>) agendaTable;
    }

    /**
     * Builds the session agenda: real columns (the table's Column model is not
     * FXML-constructible), demo rows, and a row action that confirms pending sessions —
     * the visible answer updates the row, the sessions KPI and a snackbar.
     */
    private void populateAgendaTable() {
        M3DataTable<Object[]> table = agenda();
        table.getColumns().add(new M3DataTable.Column<>("Time", row -> (String) row[0], 76.0));
        table.getColumns().add(new M3DataTable.Column<>("Session", row -> (String) row[1], 190.0));
        table.getColumns().add(new M3DataTable.Column<>("Coach", row -> (String) row[2], 150.0));
        table.getColumns().add(new M3DataTable.Column<>("Capacity", row -> (String) row[3], 92.0));
        table.getColumns().add(new M3DataTable.Column<>("Status", row -> (String) row[4], 104.0));

        table.getItems().setAll(List.of(
                new Object[] {"07:00", "HIIT Circuit", "Coach Sarah", "14 / 16", "Confirmed"},
                new Object[] {"08:30", "Powerlifting Club", "Coach Marcus", "10 / 12", "Confirmed"},
                new Object[] {"10:00", "Senior Mobility", "Coach Elena", "8 / 10", "Confirmed"},
                new Object[] {"12:30", "Express Lunch Cardio", "Coach Sarah", "11 / 14", "Confirmed"},
                new Object[] {"14:00", "VBT Strength Lab", "Coach Marcus", "6 / 8", "Pending"},
                new Object[] {"17:30", "Spin Interval", "Coach Elena", "18 / 20", "Pending"},
                new Object[] {"19:00", "Boxing Fundamentals", "Coach Yusuf", "12 / 16", "Confirmed"},
                new Object[] {"20:30", "Open Mat Recovery", "Coach Yusuf", "9 / 12", "Confirmed"}));

        table.setOnRowAction(row -> confirmPendingSession(table, row));
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
        quickActionSpeedDial.addAction("Refresh Telemetry", M3Icon.symbol("refresh"), () -> refreshData());
    }
}
