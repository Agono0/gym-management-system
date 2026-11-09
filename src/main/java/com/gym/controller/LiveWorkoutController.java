package com.gym.controller;

import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.chart.M3SplineChart;
import io.m3fx.controls.effect.M3GlassPane;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.workout.M3HeartRateZones;
import io.m3fx.controls.workout.M3IntervalTimer;
import io.m3fx.controls.workout.M3RepCounter;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.layout.RowConstraints;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Pure UI skeleton controller for Live Workout HUD &amp; Kinetic Cockpit screen.
 * Strictly conforms to Section 7 of docs/ui/workouts/LIVE_WORKOUT_HUD_UI.md and docs/UML.md.
 * All animation runs on the FX application thread via javafx.animation.Timeline.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former live-workout-hud.fxml: cardiac zone bar, telemetry cockpit grid, action dock.
 */
public class LiveWorkoutController implements ScreenController {

    /** Session clock baseline matching the "00:34:12" shown in the markup. */
    private static final long INITIAL_ELAPSED_SECONDS = 34L * 60 + 12;

    // ---- View fields (the former fx:id injections, now built by buildView) ----
    private StackPane viewRoot;
    private M3HeartRateZones heartRateZonesBar;
    private Label athleteNameLabel;
    private Label exerciseTitleLabel;
    private Label setTrackerLabel;
    private Label targetLoadLabel;
    private Label elapsedSessionTimerLabel;
    private Label meanVelocityLabel;
    private Label peakVelocityLabel;
    private Label velocityLossLabel;
    private Label currentBpmLabel;
    private Label cardiacZoneLabel;
    private M3RepCounter repCounterDisplay;
    private Label cadenceDisplayLabel;
    private M3SplineChart cadenceSplineChart;
    private M3IntervalTimer intervalCountdownTimer;
    private Label timerStateLabel;
    private M3Button completeSetButton;

    /** FX-thread ticker driving the elapsed session clock; never touches the scene graph off-thread. */
    private Timeline sessionClock;
    private long elapsedSeconds = INITIAL_ELAPSED_SECONDS;

    /**
     * Builds the complete Live Workout HUD and initializes all behavior. Same ordering
     * semantics the FXMLLoader used to provide: the node tree exists and every field is
     * assigned before the former {@code initialize()} work runs.
     */
    public LiveWorkoutController() {
        viewRoot = buildView();

        // Match the configured rest window announced in the HUD ("Target Rest: 180s")
        // and start the rest countdown; M3IntervalTimer animates via its own FX AnimationTimer.
        intervalCountdownTimer.setRestSeconds(180);
        intervalCountdownTimer.start();

    // fx:id parity: every node the markup used to tag with fx:id keeps its JavaFX id, so
    // CSS #id selectors, Node.lookup("#..."), the cross-screen rail navigation
    // (findById("navCustomers") and friends) and the interaction tests keep working unchanged.
        heartRateZonesBar.setId("heartRateZonesBar");
        athleteNameLabel.setId("athleteNameLabel");
        exerciseTitleLabel.setId("exerciseTitleLabel");
        setTrackerLabel.setId("setTrackerLabel");
        targetLoadLabel.setId("targetLoadLabel");
        elapsedSessionTimerLabel.setId("elapsedSessionTimerLabel");
        meanVelocityLabel.setId("meanVelocityLabel");
        peakVelocityLabel.setId("peakVelocityLabel");
        velocityLossLabel.setId("velocityLossLabel");
        currentBpmLabel.setId("currentBpmLabel");
        cardiacZoneLabel.setId("cardiacZoneLabel");
        repCounterDisplay.setId("repCounterDisplay");
        cadenceDisplayLabel.setId("cadenceDisplayLabel");
        cadenceSplineChart.setId("cadenceSplineChart");
        intervalCountdownTimer.setId("intervalCountdownTimer");
        timerStateLabel.setId("timerStateLabel");
        completeSetButton.setId("completeSetButton");
        startSessionClock();
    }

    /**
     * Constructs the whole node tree (the programmatic replacement for live-workout-hud.fxml)
     * and wires every handler.
     */
    private StackPane buildView() {
        BorderPane layout = new BorderPane();

        // Top: 5-zone cardiac intensity gauge.
        VBox topContainer = new VBox();
        topContainer.getStyleClass().add("hud-top-container");
        heartRateZonesBar = new M3HeartRateZones();
        heartRateZonesBar.setPrefHeight(28);
        heartRateZonesBar.setMaxWidth(Double.MAX_VALUE);

        HBox metaHeader = new HBox(20);
        metaHeader.setAlignment(Pos.CENTER_LEFT);
        metaHeader.getStyleClass().add("hud-meta-header");
        metaHeader.setPadding(new Insets(8, 20, 8, 20));
        athleteNameLabel = new Label("Marcus Vance (#104)");
        athleteNameLabel.getStyleClass().add("hud-meta-value");
        exerciseTitleLabel = new Label("Barbell Back Squat");
        exerciseTitleLabel.getStyleClass().add("hud-meta-value-accent");
        setTrackerLabel = new Label("3 of 4");
        setTrackerLabel.getStyleClass().add("hud-meta-value");
        targetLoadLabel = new Label("140.0 kg");
        targetLoadLabel.getStyleClass().add("hud-meta-value-bold");
        elapsedSessionTimerLabel = new Label("00:34:12");
        elapsedSessionTimerLabel.getStyleClass().add("hud-clock-label");
        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);
        metaHeader.getChildren().addAll(
                styledLabel("ATHLETE:", "hud-meta-label"),
                athleteNameLabel,
                new Separator(Orientation.VERTICAL),
                styledLabel("EXERCISE:", "hud-meta-label"),
                exerciseTitleLabel,
                new Separator(Orientation.VERTICAL),
                styledLabel("SET:", "hud-meta-label"),
                setTrackerLabel,
                new Separator(Orientation.VERTICAL),
                styledLabel("LOAD:", "hud-meta-label"),
                targetLoadLabel,
                metaSpacer,
                elapsedSessionTimerLabel);
        topContainer.getChildren().addAll(heartRateZonesBar, metaHeader);
        layout.setTop(topContainer);

        // Center: bento cockpit telemetry grid.
        GridPane bentoGrid = new GridPane(16, 16);
        bentoGrid.getStyleClass().add("hud-bento-grid");
        bentoGrid.setPadding(new Insets(16));
        ColumnConstraints leftColumn = new ColumnConstraints();
        leftColumn.setPercentWidth(25);
        ColumnConstraints centerColumn = new ColumnConstraints();
        centerColumn.setPercentWidth(50);
        ColumnConstraints rightColumn = new ColumnConstraints();
        rightColumn.setPercentWidth(25);
        RowConstraints fullRow = new RowConstraints();
        fullRow.setPercentHeight(100);
        bentoGrid.getColumnConstraints().addAll(leftColumn, centerColumn, rightColumn);
        bentoGrid.getRowConstraints().add(fullRow);

        // Column 0: VBT & muscular power telemetry.
        VBox leftColumnBox = new VBox(12);
        leftColumnBox.getStyleClass().add("telemetry-column");

        M3GlassPane vbtVisor = new M3GlassPane();
        vbtVisor.getStyleClass().add("hud-glass-visor");
        VBox.setVgrow(vbtVisor, Priority.ALWAYS);
        VBox vbtContent = new VBox(10);
        vbtContent.getStyleClass().add("hud-card-padded");
        vbtContent.setPadding(new Insets(14));
        vbtContent.getChildren().add(styledLabel("Velocity-Based Training", "hud-card-header"));
        meanVelocityLabel = metricRow(vbtContent, "Mean Velocity:", "0.68 m/s", "hud-metric-value-green");
        peakVelocityLabel = metricRow(vbtContent, "Peak Velocity:", "0.92 m/s", "hud-metric-value");
        velocityLossLabel = metricRow(vbtContent, "Velocity Loss:", "-14.2%", "hud-metric-value-warning");
        vbtContent.getChildren().addAll(
                new Separator(),
                styledLabel("Estimated 1RM: 168.5 kg", "hud-metric-subtext"),
                styledLabel("Accumulated Tonnage: 11,200 kg", "hud-metric-subtext"));
        vbtVisor.getGlassChildren().add(vbtContent);

        M3GlassPane cardiacVisor = new M3GlassPane();
        cardiacVisor.getStyleClass().add("hud-glass-visor");
        cardiacVisor.setPrefHeight(160);
        VBox cardiacContent = new VBox(8);
        cardiacContent.getStyleClass().add("hud-card-padded");
        cardiacContent.setPadding(new Insets(12));
        cardiacContent.getChildren().add(styledLabel("Live Cardiac Stream", "hud-card-header"));
        HBox bpmRow = new HBox();
        bpmRow.setAlignment(Pos.CENTER);
        currentBpmLabel = new Label("184");
        currentBpmLabel.getStyleClass().add("hud-bpm-large");
        bpmRow.getChildren().addAll(currentBpmLabel, styledLabel(" BPM", "hud-bpm-unit"));
        cardiacZoneLabel = new Label("Zone 5: VO2 Max Strain");
        cardiacZoneLabel.setAlignment(Pos.CENTER);
        cardiacZoneLabel.setMaxWidth(Double.MAX_VALUE);
        cardiacZoneLabel.getStyleClass().add("hud-zone-pill-red");
        cardiacContent.getChildren().addAll(bpmRow, cardiacZoneLabel);
        cardiacVisor.getGlassChildren().add(cardiacContent);

        leftColumnBox.getChildren().addAll(vbtVisor, cardiacVisor);

        // Column 1: center kinetic rep counter & cadence visor.
        VBox centerColumnBox = new VBox(12);
        centerColumnBox.setAlignment(Pos.CENTER);
        centerColumnBox.getStyleClass().add("center-cockpit-column");
        M3GlassPane cockpitVisor = new M3GlassPane();
        cockpitVisor.getStyleClass().add("hud-glass-visor");
        VBox.setVgrow(cockpitVisor, Priority.ALWAYS);
        VBox cockpitContent = new VBox(16);
        cockpitContent.setAlignment(Pos.CENTER);
        cockpitContent.getStyleClass().add("hud-center-deck");
        cockpitContent.setPadding(new Insets(16));
        cockpitContent.getChildren().add(styledLabel("CONCENTRIC KINETIC IMPULSE", "hud-cockpit-subhead"));

        // Core M3RepCounter component with manual controls.
        HBox repControlsRow = new HBox(24);
        repControlsRow.setAlignment(Pos.CENTER);
        M3IconButton decrementBtn = new M3IconButton();
        decrementBtn.getStyleClass().add("hud-rep-pill-btn");
        Label minusPill = new Label("-");
        minusPill.getStyleClass().add("hud-rep-pill-text");
        decrementBtn.setGraphic(minusPill);
        decrementBtn.setOnAction(e -> onManualRepDecrement());
        repCounterDisplay = new M3RepCounter();
        repCounterDisplay.setPrefSize(240, 180);
        M3IconButton incrementBtn = new M3IconButton();
        incrementBtn.getStyleClass().add("hud-rep-pill-btn");
        incrementBtn.setGraphic(M3Icon.symbol("add"));
        incrementBtn.setOnAction(e -> onManualRepIncrement());
        repControlsRow.getChildren().addAll(decrementBtn, repCounterDisplay, incrementBtn);

        cadenceDisplayLabel = new Label("Cadence: 3.2s | Ecc: 2.1s | Con: 0.9s");
        cadenceDisplayLabel.getStyleClass().add("hud-cadence-caption");
        cadenceSplineChart = new M3SplineChart();
        cadenceSplineChart.setPrefHeight(100);
        cadenceSplineChart.getStyleClass().add("hud-cadence-chart");
        cockpitContent.getChildren().addAll(repControlsRow, cadenceDisplayLabel, cadenceSplineChart);
        cockpitVisor.getGlassChildren().add(cockpitContent);
        centerColumnBox.getChildren().add(cockpitVisor);

        // Column 2: concentric interval timer & quick RPE logging.
        VBox rightColumnBox = new VBox(12);
        rightColumnBox.getStyleClass().add("telemetry-column");
        M3GlassPane restVisor = new M3GlassPane();
        restVisor.getStyleClass().add("hud-glass-visor");
        VBox.setVgrow(restVisor, Priority.ALWAYS);
        VBox restContent = new VBox(12);
        restContent.setAlignment(Pos.CENTER);
        restContent.getStyleClass().add("hud-card-padded");
        restContent.setPadding(new Insets(14));
        restContent.getChildren().add(styledLabel("Rest Interval Countdown", "hud-card-header"));
        intervalCountdownTimer = new M3IntervalTimer();
        intervalCountdownTimer.setPrefSize(180, 180);
        timerStateLabel = new Label("Target Rest: 180s");
        timerStateLabel.getStyleClass().add("hud-metric-subtext");
        M3Button skipRestBtn = new M3Button("Skip Rest Window", ButtonVariant.FILLED);
        skipRestBtn.getStyleClass().add("hud-secondary-btn");
        skipRestBtn.setOnAction(e -> onSkipRest());
        restContent.getChildren().addAll(intervalCountdownTimer, timerStateLabel, skipRestBtn);
        restVisor.getGlassChildren().add(restContent);

        M3GlassPane rpeVisor = new M3GlassPane();
        rpeVisor.getStyleClass().add("hud-glass-visor");
        rpeVisor.setPrefHeight(160);
        VBox rpeContent = new VBox(8);
        rpeContent.getStyleClass().add("hud-card-padded");
        rpeContent.setPadding(new Insets(12));
        rpeContent.getChildren().add(styledLabel("Perceived Exertion (RPE)", "hud-card-header"));
        HBox rpeRow = new HBox(6);
        rpeRow.setAlignment(Pos.CENTER);
        rpeRow.getChildren().addAll(
                rpeButton("7", "rpe-btn"),
                rpeButton("8", "rpe-btn"),
                rpeButton("8.5", "rpe-btn-active"),
                rpeButton("9", "rpe-btn"),
                rpeButton("10", "rpe-btn-red"));
        rpeContent.getChildren().add(rpeRow);
        rpeVisor.getGlassChildren().add(rpeContent);

        rightColumnBox.getChildren().addAll(restVisor, rpeVisor);

        bentoGrid.add(leftColumnBox, 0, 0);
        bentoGrid.add(centerColumnBox, 1, 0);
        bentoGrid.add(rightColumnBox, 2, 0);
        layout.setCenter(bentoGrid);

        // Bottom: high-visibility contextual cockpit controls.
        HBox actionDock = new HBox(20);
        actionDock.setAlignment(Pos.CENTER_RIGHT);
        actionDock.getStyleClass().add("hud-bottom-action-dock");
        actionDock.setPadding(new Insets(14, 20, 14, 20));
        M3Button abortBtn = new M3Button("⚠️ ABORT SET / REST", ButtonVariant.FILLED);
        abortBtn.getStyleClass().add("hud-danger-btn");
        abortBtn.setOnAction(e -> onAbortSet());
        M3Button pauseBtn = new M3Button("⏸️ PAUSE", ButtonVariant.FILLED);
        pauseBtn.getStyleClass().add("hud-neutral-btn");
        pauseBtn.setOnAction(this::onTogglePause);
        M3Button recalibrateBtn = new M3Button("🔄 RE-CALIBRATE", ButtonVariant.FILLED);
        recalibrateBtn.getStyleClass().add("hud-neutral-btn");
        recalibrateBtn.setOnAction(e -> onRecalibrateSensor());
        completeSetButton = new M3Button("✅ COMPLETE SET (140kg)", ButtonVariant.FILLED);
        completeSetButton.getStyleClass().add("hud-complete-set-btn");
        completeSetButton.setOnAction(e -> onCompleteSet());
        Region dockSpacer = new Region();
        HBox.setHgrow(dockSpacer, Priority.ALWAYS);
        actionDock.getChildren().addAll(abortBtn, dockSpacer, pauseBtn, recalibrateBtn, completeSetButton);
        layout.setBottom(actionDock);

        viewRoot = new StackPane();
        viewRoot.getStyleClass().add("live-hud-root");
        viewRoot.getChildren().add(layout);
        return viewRoot;
    }

    /** One RPE quick-log button (the active one carries {@code rpe-btn-active}). */
    private M3Button rpeButton(String label, String styleClass) {
        M3Button button = new M3Button(label, ButtonVariant.FILLED);
        button.getStyleClass().add(styleClass);
        button.setOnAction(this::onSelectRpe);
        return button;
    }

    /** One metric label/value row; returns the value label so the field keeps a handle. */
    private static Label metricRow(VBox container, String labelText, String valueText, String valueStyle) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label(labelText);
        label.getStyleClass().add("hud-metric-label");
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

    private void startSessionClock() {
        if (sessionClock != null) {
            sessionClock.stop();
        }
        sessionClock = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickSessionClock()));
        sessionClock.setCycleCount(Animation.INDEFINITE);
        sessionClock.play();
        // Pause the clock while the HUD is not on screen (screen swap) so no pulse is wasted
        elapsedSessionTimerLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (sessionClock == null) {
                return;
            }
            if (newScene != null) {
                sessionClock.play();
            } else {
                sessionClock.pause();
            }
        });
    }

    private void tickSessionClock() {
        elapsedSeconds++;
        elapsedSessionTimerLabel.setText(String.format("%02d:%02d:%02d",
                elapsedSeconds / 3600, (elapsedSeconds % 3600) / 60, elapsedSeconds % 60));
    }

    public void onManualRepDecrement() {
        repCounterDisplay.setReps(Math.max(0, repCounterDisplay.getReps() - 1));
    }

    public void onManualRepIncrement() {
        repCounterDisplay.countRep();
    }

    public void onSkipRest() {
        // No skip-to-next-round API exists on M3IntervalTimer yet; restarting would lose the
        // round progress, so the control's own start/pause/reset bar remains the interface.
    }

    /** Moves the active highlight to the tapped RPE button. */
    public void onSelectRpe(ActionEvent event) {
        if (event.getSource() instanceof M3Button selected) {
            Node row = selected.getParent();
            if (row instanceof Parent rowParent) {
                for (Node child : rowParent.getChildrenUnmodifiable()) {
                    if (child instanceof M3Button button) {
                        button.getStyleClass().remove("rpe-btn-active");
                    }
                }
            }
            if (!selected.getStyleClass().contains("rpe-btn-active")) {
                selected.getStyleClass().add("rpe-btn-active");
            }
        }
    }

    public void onAbortSet() {
        // Aborting stops both clocks and resets the current set
        if (sessionClock != null && sessionClock.getStatus() == Animation.Status.PAUSED) {
            sessionClock.play();
        }
        repCounterDisplay.reset();
        intervalCountdownTimer.reset();
    }

    public void onTogglePause(ActionEvent event) {
        boolean paused = sessionClock != null && sessionClock.getStatus() == Animation.Status.PAUSED;
        if (sessionClock != null) {
            if (paused) {
                sessionClock.play();
            } else {
                sessionClock.pause();
            }
        }
        if (paused) {
            intervalCountdownTimer.start();
        } else {
            intervalCountdownTimer.pause();
        }
        if (event.getSource() instanceof M3Button pauseButton) {
            pauseButton.setText(paused ? "⏸️ PAUSE" : "▶ RESUME");
        }
    }

    public void onRecalibrateSensor() {
        // No-op recalibrate handler
    }

    public void onCompleteSet() {
        // A completed set advances the rep counter to the next set and restarts the rest window
        repCounterDisplay.nextSet();
        intervalCountdownTimer.reset();
        intervalCountdownTimer.start();
    }

    public M3HeartRateZones getHeartRateZonesBar() { return heartRateZonesBar; }
    public Label getAthleteNameLabel() { return athleteNameLabel; }
    public Label getExerciseTitleLabel() { return exerciseTitleLabel; }
    public Label getSetTrackerLabel() { return setTrackerLabel; }
    public Label getTargetLoadLabel() { return targetLoadLabel; }
    public Label getElapsedSessionTimerLabel() { return elapsedSessionTimerLabel; }
    public Label getMeanVelocityLabel() { return meanVelocityLabel; }
    public Label getPeakVelocityLabel() { return peakVelocityLabel; }
    public Label getVelocityLossLabel() { return velocityLossLabel; }
    public Label getCurrentBpmLabel() { return currentBpmLabel; }
    public Label getCardiacZoneLabel() { return cardiacZoneLabel; }
    public M3RepCounter getRepCounterDisplay() { return repCounterDisplay; }
    public Label getCadenceDisplayLabel() { return cadenceDisplayLabel; }
    public M3SplineChart getCadenceSplineChart() { return cadenceSplineChart; }
    public M3IntervalTimer getIntervalCountdownTimer() { return intervalCountdownTimer; }
    public Label getTimerStateLabel() { return timerStateLabel; }
    public M3Button getCompleteSetButton() { return completeSetButton; }
}
