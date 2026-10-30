package com.gym.controller;

import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.chart.M3ActivityCalendar;
import io.m3fx.controls.chart.M3DataPoint;
import io.m3fx.controls.chart.M3DataSeries;
import io.m3fx.controls.chart.M3DonutChart;
import io.m3fx.controls.chart.M3ScatterPlot;
import io.m3fx.controls.chart.M3ScatterPoint;
import io.m3fx.controls.chart.M3ScatterSeries;
import io.m3fx.controls.chart.M3SplineChart;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.data.M3Timeline;
import io.m3fx.controls.effect.M3ParticleCanvas;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.segmented.M3SegmentedButton;
import io.m3fx.controls.sheet.M3BottomSheet;
import io.m3fx.controls.slider.M3Slider;
import io.m3fx.controls.textfield.M3TextField;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Controller for Biomechanical Progress & Analytics (progress.fxml).
 * Conforms to docs/ui/members/MEMBER_PROGRESS_UI.md Section 7 contract.
 * Pure UI: strictly no backend service, DAO, or database logic.
 *
 * <p>UX contract: the time-horizon segments actually reslice the trajectory charts, the log
 * entry bottom sheet opens modally with scrim and drag-to-dismiss, saving validates and
 * answers with a snackbar while appending the reading to the chart, and all demo telemetry
 * renders real data instead of placeholder dashes.
 */
public class ProgressController {

    /** Weekly sample count for the full horizon (ALL); shorter horizons slice the tail. */
    private static final int WEEKLY_SAMPLES = 64;
    /** Points shown per horizon: 1M / 3M / 6M / 1Y / ALL. */
    private static final int[] HORIZON_POINTS = {4, 13, 26, 52, WEEKLY_SAMPLES};

    @FXML private StackPane rootPane;
    @FXML private BorderPane mainLayout;
    @FXML private M3TopAppBar topAppBar;
    @FXML private M3IconButton btnBack;
    @FXML private M3SegmentedButton segTimeHorizon;
    @FXML private M3Button btnLogEntry;

    @FXML private M3StatCard statCurrentWeight;
    @FXML private M3StatCard statBodyFat;
    @FXML private M3StatCard statLeanMass;
    @FXML private M3StatCard statVbtMax;

    @FXML private M3SplineChart splineBiometrics;
    @FXML private M3ActivityCalendar activityHeatmap;
    @FXML private M3DonutChart donutComposition;
    @FXML private Label lblRegressionStats;
    @FXML private M3ScatterPlot scatterVbt;
    @FXML private M3Timeline timelineAnthropometrics;

    @FXML private M3ParticleCanvas celebrationCanvas;
    @FXML private M3BottomSheet sheetLogEntry;
    @FXML private M3TextField txtEntryDate;
    @FXML private M3TextField txtWeight;
    @FXML private M3Slider sliderWeight;
    @FXML private M3TextField txtBodyFat;
    @FXML private M3TextField txtTargetWeight;
    @FXML private M3TextField txtMeasurements;

    /** Overlay host for snackbars and the modal bottom sheet (no fx:id, added in code). */
    private M3Overlay overlay;
    /** Trajectory series kept as fields so horizons and new log entries can reslice them. */
    private M3DataSeries weightSeries;
    private M3DataSeries bodyFatSeries;

    public ProgressController() {
        // Public no-arg constructor for FXMLLoader
    }

    @FXML
    public void initialize() {
        overlay = new M3Overlay();
        rootPane.getChildren().add(overlay);

        prefillTelemetry();
        populateTrajectoryChart();
        populateActivityHeatmap();
        populateCompositionDonut();
        populateVbtScatter();
        populateTapeTimeline();
        // The segmented control is an HBox (no onAction property), so the horizon answer is
        // wired through its selection model; the @FXML handler stays the single entry point.
        segTimeHorizon.selectedIndexProperty().addListener((obs, oldV, newV) -> onHorizonChanged());
        // Dragging the slider drafts the weight reading in the open sheet.
        if (sliderWeight != null && txtWeight != null) {
            sliderWeight.valueProperty().addListener((obs, oldV, newV) ->
                    txtWeight.setText(String.format(Locale.ROOT, "%.1f", newV.doubleValue())));
        }
        onHorizonChanged();
    }

    // ------------------------------------------------------------------
    // Event handlers (answers for every visible action)
    // ------------------------------------------------------------------

    /** Returns to the member roster via the shell rail; answers with a hint if standalone. */
    @FXML
    public void onNavigateBack() {
        navigateViaShellRail("navCustomers",
                "Open the member roster from the navigation rail (this preview is standalone).");
    }

    /** Reslices the trajectory charts to the selected horizon (the visible answer). */
    @FXML
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
    @FXML
    public void onOpenLogDialog() {
        if (sheetLogEntry == null) {
            return;
        }
        sheetLogEntry.setVisible(true);
        overlay.showSheet(sheetLogEntry);
    }

    /** Closes the log-entry bottom sheet (also reachable via Escape, scrim and drag). */
    @FXML
    public void onCloseLogSheet() {
        if (sheetLogEntry != null) {
            sheetLogEntry.hide();
        }
    }

    /**
     * Saves the telemetry entry: field errors name the problem, success updates the KPIs,
     * appends the reading to the trajectory chart and confirms via snackbar.
     */
    @FXML
    public void onSaveRecord() {
        Double weight = parseDouble(txtWeight, 30.0, 300.0);
        Double bodyFat = txtBodyFat == null || txtBodyFat.getText().isBlank()
                ? Double.valueOf(0.0) : parseDouble(txtBodyFat, 0.0, 70.0);
        boolean valid = weight != null && bodyFat != null;
        if (txtWeight != null) {
            txtWeight.setError(weight == null);
        }
        if (txtBodyFat != null) {
            txtBodyFat.setError(bodyFat == null);
        }
        if (!valid) {
            if (overlay != null) {
                overlay.showSnackbar("Fix the highlighted fields, then save the entry.");
            }
            return;
        }
        // Reflect the entry across the screen: slider, KPI, lean-mass math, chart, timeline.
        if (sliderWeight != null) {
            sliderWeight.setValue(weight);
        }
        if (statCurrentWeight != null) {
            statCurrentWeight.setValue(String.format("%.1f kg", weight));
            statCurrentWeight.setDeltaPositive(weight <= 80.0);
        }
        if (bodyFat > 0 && statLeanMass != null) {
            statLeanMass.setValue(String.format(Locale.ROOT, "%.1f kg", weight * (1 - bodyFat / 100.0)));
        }
        appendReading(weight, bodyFat);
        boolean goalReached = weight <= 80.0;
        if (overlay != null) {
            overlay.showSnackbar(goalReached
                    ? String.format(Locale.ROOT, "Entry saved — %.1f kg hits the 80.0 kg goal!", weight)
                    : String.format(Locale.ROOT, "Entry saved — %.1f kg at %.1f%% body fat.", weight, bodyFat));
        }
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
        if (txtEntryDate != null) {
            txtEntryDate.setText(LocalDate.now().toString());
        }
        if (txtTargetWeight != null) {
            txtTargetWeight.setText("80.0");
        }
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
        if (timelineAnthropometrics != null && LocalDate.now().toString().equals(
                txtEntryDate == null ? "" : txtEntryDate.getText())) {
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
        if (field == null) {
            return null;
        }
        try {
            double value = Double.parseDouble(field.getText().trim());
            return (value >= min && value <= max) ? Double.valueOf(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Navigates by firing the shell's own pinned rail button (same pathway as clicking the
     * rail). Outside the shell — standalone previews and FXML tests — it answers with a
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
        if (rootPane == null || rootPane.getScene() == null) {
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
