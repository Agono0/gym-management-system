package com.gym.controller;

import com.gym.dao.CustomerDao;
import com.gym.dao.SessionDao;
import com.gym.model.Customer;
import com.gym.model.Session;
import com.gym.model.SessionStatus;
import com.gym.ui.Data;
import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.chart.M3SplineChart;
import io.m3fx.controls.effect.M3GlassPane;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.overlay.M3Overlay;
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
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import io.m3fx.controls.dialog.M3Dialog;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Controller for Live Workout HUD &amp; Kinetic Cockpit screen.
 * Strictly conforms to Section 7 of docs/ui/workouts/LIVE_WORKOUT_HUD_UI.md and docs/UML.md.
 * All animation runs on the FX application thread via javafx.animation.Timeline.
 *
 * <p>Data contract (DECISIONS.md 77/78, task C4): the HUD runs a real CONFIRMED session —
 * handed off from the schedule ({@link #startSession}) or picked automatically (the first
 * CONFIRMED session from today onward). COMPLETE SESSION transitions the session to COMPLETED
 * through {@code SessionDao.update} (rule 18); ABORT cancels it with a recorded reason (rule
 * 18 transition, rule-10 notice copy mirrored from the CLI). The performance telemetry
 * (velocities, BPM, cadence) stays simulated and is LABELED as simulated — no fake sensor
 * claims — while everything that touches the session record is real.
 *
 * <p>The node tree is built in {@link #buildView()} — the programmatic replacement for the
 * former live-workout-hud.fxml: cardiac zone bar, telemetry cockpit grid, action dock.
 */
public class LiveWorkoutController implements ScreenController {

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
    private M3Button abortButton;
    private Label sessionStatusLabel;

    /** FX-thread ticker driving the elapsed session clock; never touches the scene graph off-thread. */
    private Timeline sessionClock;
    private long elapsedSeconds;

    /** Hosts M3 snackbars and dialogs (attached to the scene root at runtime). */
    private final M3Overlay feedback = new M3Overlay();
    /** The CONFIRMED session this HUD is running, or null in the idle/empty state. */
    private Session activeSession;
    /** True once the session reaches a terminal status (COMPLETED/CANCELLED) in the database. */
    private boolean sessionFinished;

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
        installFeedbackLayer(viewRoot);
        loadDefaultSession();
    }

    /** Shows a snackbar through the HUD's feedback overlay (attached once a scene exists). */
    private void notice(String message) {
        feedback.showSnackbar(message);
    }

    /**
     * Hosts the shared feedback overlay on the scene root while this screen is visible.
     * Attach/detach are deferred with Platform.runLater so the scene root children list is
     * never mutated re-entrantly during a screen swap (same pattern as the schedule screen).
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
        athleteNameLabel = new Label("No session loaded");
        athleteNameLabel.getStyleClass().add("hud-meta-value");
        exerciseTitleLabel = new Label("—");
        exerciseTitleLabel.getStyleClass().add("hud-meta-value-accent");
        setTrackerLabel = new Label("—");
        setTrackerLabel.getStyleClass().add("hud-meta-value");
        targetLoadLabel = new Label("—");
        targetLoadLabel.getStyleClass().add("hud-meta-value-bold");
        elapsedSessionTimerLabel = new Label("00:00:00");
        elapsedSessionTimerLabel.getStyleClass().add("hud-clock-label");
        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);
        // Honest telemetry: the velocities/BPM/cadence below are simulated instruments — the
        // badge says so, and everything that touches the session record is real.
        Label simulatedBadge = new Label("SIMULATED TELEMETRY");
        simulatedBadge.getStyleClass().add("hud-meta-label");
        simulatedBadge.setStyle("-fx-text-fill: -md-sys-color-tertiary;"
                + " -fx-border-color: -md-sys-color-tertiary; -fx-border-radius: 6;"
                + " -fx-padding: 2 6 2 6;");
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
                simulatedBadge,
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

        // Bottom: high-visibility contextual cockpit controls. The leading status label carries
        // the real session state; the telemetry buttons stay simulated per the badge above.
        HBox actionDock = new HBox(20);
        actionDock.setAlignment(Pos.CENTER_RIGHT);
        actionDock.getStyleClass().add("hud-bottom-action-dock");
        actionDock.setPadding(new Insets(14, 20, 14, 20));
        sessionStatusLabel = new Label("No CONFIRMED session running");
        sessionStatusLabel.getStyleClass().add("hud-metric-subtext");
        M3Button abortBtn = new M3Button("ABORT SESSION", ButtonVariant.FILLED);
        abortButton = abortBtn;
        abortBtn.setGraphic(M3Icon.symbol("stop"));
        abortBtn.getStyleClass().add("hud-danger-btn");
        abortBtn.setOnAction(e -> onAbortSession());
        M3Button pauseBtn = new M3Button("PAUSE", ButtonVariant.FILLED);
        pauseBtn.setGraphic(M3Icon.symbol("pause"));
        pauseBtn.getStyleClass().add("hud-neutral-btn");
        pauseBtn.setOnAction(this::onTogglePause);
        M3Button recalibrateBtn = new M3Button("RE-CALIBRATE", ButtonVariant.FILLED);
        recalibrateBtn.setGraphic(M3Icon.symbol("refresh"));
        recalibrateBtn.getStyleClass().add("hud-neutral-btn");
        recalibrateBtn.setOnAction(e -> onRecalibrateSensor());
        completeSetButton = new M3Button("COMPLETE SESSION", ButtonVariant.FILLED);
        completeSetButton.setGraphic(M3Icon.symbol("check"));
        completeSetButton.getStyleClass().add("hud-complete-set-btn");
        completeSetButton.setOnAction(e -> onCompleteSession());
        Region dockSpacer = new Region();
        HBox.setHgrow(dockSpacer, Priority.ALWAYS);
        actionDock.getChildren().addAll(
                sessionStatusLabel, dockSpacer, abortBtn, pauseBtn, recalibrateBtn, completeSetButton);
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
        // Skip = restart the rest window from the top (the timer exposes start/pause/reset,
        // not a skip-to-end): the remaining rest is forfeit and the next countdown begins now.
        intervalCountdownTimer.reset();
        intervalCountdownTimer.start();
        timerStateLabel.setText("Rest window restarted — next round begins at 180s.");
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
        onAbortSession();
    }

    /**
     * ABORT SESSION: rule 18 transition CONFIRMED -> CANCELLED through {@code SessionDao}
     * with the reason recorded (rule 10 notice copy mirrored from the CLI). Destructive, so
     * it confirms with a reason field first. The in-HUD clocks keep running as instruments.
     */
    public void onAbortSession() {
        if (activeSession == null || sessionFinished) {
            notice("No CONFIRMED session is running — nothing to abort.");
            return;
        }
        TextField reasonField = new TextField();
        reasonField.setPromptText("Required (rule 10) — recorded on the session");
        reasonField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().length() <= 120 ? change : null));
        M3Button keep = M3Button.text("Keep running");
        M3Button abort = M3Button.text("Abort session");
        M3Dialog dialog = M3Dialog.basic("Abort this session?",
                new VBox(10,
                        new Label("Session #" + activeSession.getId() + " will be marked"
                                + " CANCELLED and the athlete is notified."),
                        reasonField),
                keep, abort);
        dialog.setDefaultAction(abort::activate);
        keep.setOnAction(e -> dialog.hide());
        abort.setOnAction(e -> {
            String reason = reasonField.getText() == null ? "" : reasonField.getText().trim();
            if (reason.isEmpty()) {
                reasonField.setStyle("-fx-border-color: -md-sys-color-error;");
                return;
            }
            dialog.hide();
            int sessionId = activeSession.getId();
            Data.submit(
                    () -> {
                        Session current = new SessionDao().findById(sessionId);
                        if (current == null) {
                            throw new com.gym.exception.ValidationException(
                                    "Session no longer exists in the database");
                        }
                        long minutesUntilStart = java.time.Duration.between(
                                java.time.LocalDateTime.now(),
                                java.time.LocalDateTime.of(LocalDate.parse(current.getDate()),
                                        LocalTime.parse(current.getStartTime()))).toMinutes();
                        current.setStatus(SessionStatus.CANCELLED);
                        current.setCancelReason(reason);
                        new SessionDao().update(current);
                        return minutesUntilStart >= 120
                                ? "Rule 10: Free cancellation (>= 2h notice)."
                                : "Rule 10: Late cancellation (< 2h before start). Reason recorded.";
                    },
                    verdict -> endSessionLocally("Session aborted. " + verdict),
                    error -> notice("Could not abort session: " + error.getMessage()));
        });
        feedback.showDialog(dialog);
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
            // The clocks just flipped: what was paused now resumes, so the button offers the
            // opposite action (Material Symbols pause / play_arrow instead of emoji glyphs).
            pauseButton.setText(paused ? "PAUSE" : "RESUME");
            pauseButton.setGraphic(M3Icon.symbol(paused ? "pause" : "play_arrow"));
        }
    }

    /**
     * RE-CALIBRATE: a real re-read of the session state — the session row is re-queried from
     * {@code SessionDao} and the HUD labels re-apply from it, while the simulated instruments
     * (velocity, BPM, cadence) re-seed to their baseline values.
     */
    public void onRecalibrateSensor() {
        if (activeSession == null) {
            notice("No session loaded — nothing to calibrate.");
            return;
        }
        int sessionId = activeSession.getId();
        Data.submit(
                () -> {
                    Session current = new SessionDao().findById(sessionId);
                    String athlete = null;
                    if (current != null) {
                        Customer customer = new CustomerDao().findById(current.getCustomerId());
                        athlete = customer == null ? null : customer.getFullName();
                    }
                    return new Object[] {current, athlete};
                },
                read -> {
                    Session current = (Session) read[0];
                    String athlete = (String) read[1];
                    if (current == null) {
                        notice("The session record no longer exists — the HUD keeps its last state.");
                        return;
                    }
                    activeSession = current;
                    applySessionLabels(current, athlete);
                    timerStateLabel.setText("Calibrated against the session record — rest restarted.");
                    notice("Telemetry re-read from the session record (#" + sessionId + ").");
                },
                error -> notice("Could not re-read the session: " + error.getMessage()));
    }

    /** The schedule handoff: runs this HUD against the given CONFIRMED session. */
    public void startSession(int sessionId) {
        Data.submit(
                () -> {
                    Session session = new SessionDao().findById(sessionId);
                    String athlete = null;
                    if (session != null) {
                        Customer customer = new CustomerDao().findById(session.getCustomerId());
                        athlete = customer == null ? null : customer.getFullName();
                    }
                    return new Object[] {session, athlete};
                },
                read -> {
                    Session session = (Session) read[0];
                    String athlete = (String) read[1];
                    if (session == null) {
                        notice("Session #" + sessionId + " no longer exists.");
                        return;
                    }
                    if (session.getStatus() != SessionStatus.CONFIRMED) {
                        notice("Only CONFIRMED sessions run in the HUD — session #"
                                + sessionId + " is " + statusLabel(session.getStatus()) + ".");
                        return;
                    }
                    activeSession = session;
                    applySessionLabels(session, athlete);
                    notice("Running session #" + sessionId + " — telemetry is simulated.");
                },
                error -> notice("Could not load session #" + sessionId + ": " + error.getMessage()));
    }

    /**
     * Standalone entry (no schedule handoff): runs the first CONFIRMED session from today
     * onward, or drops to the honest empty state when none is booked.
     */
    private void loadDefaultSession() {
        Data.submit(
                () -> {
                    List<Session> sessions = new SessionDao().findAll();
                    Session first = null;
                    for (Session session : sessions) {
                        if (session.getStatus() != SessionStatus.CONFIRMED
                                || session.getDate() == null) {
                            continue;
                        }
                        if (first == null
                                || session.getDate().compareTo(first.getDate()) < 0) {
                            first = session;
                        }
                    }
                    String athlete = null;
                    if (first != null) {
                        Customer customer = new CustomerDao().findById(first.getCustomerId());
                        athlete = customer == null ? null : customer.getFullName();
                    }
                    return new Object[] {first, athlete};
                },
                read -> {
                    Session first = (Session) read[0];
                    String athlete = (String) read[1];
                    if (first == null) {
                        sessionStatusLabel.setText("No CONFIRMED session booked — book one from"
                                + " the schedule screen to run it here.");
                        notice("No CONFIRMED session found — book one from the schedule screen.");
                        return;
                    }
                    activeSession = first;
                    applySessionLabels(first, athlete);
                },
                error -> sessionStatusLabel.setText(
                        "Could not load sessions: " + error.getMessage()));
    }

    /** Applies a loaded session to the HUD labels and resets the in-cockpit instruments. */
    private void applySessionLabels(Session session, String athleteName) {
        athleteNameLabel.setText(athleteName == null || athleteName.isBlank()
                ? "Member #" + session.getCustomerId() : athleteName);
        exerciseTitleLabel.setText(session.getSessionNotes() == null
                || session.getSessionNotes().isBlank()
                ? "Session #" + session.getId() : session.getSessionNotes());
        targetLoadLabel.setText(session.getPrice() == null
                ? "—" : "$" + session.getPrice().toPlainString());
        setTrackerLabel.setText("Set 1 · " + session.getDurationMin() + " min block");
        sessionStatusLabel.setText("Session #" + session.getId() + " · "
                + session.getDate() + " at " + session.getStartTime() + " · "
                + statusLabel(session.getStatus()));
        sessionFinished = false;
        completeSetButton.setDisable(false);
        abortButton.setDisable(false);
        elapsedSeconds = 0;
        repCounterDisplay.reset();
        intervalCountdownTimer.reset();
        intervalCountdownTimer.start();
    }

    /** Terminal state: pauses the clocks and disables the lifecycle buttons. */
    private void endSessionLocally(String message) {
        sessionFinished = true;
        sessionClock.pause();
        intervalCountdownTimer.pause();
        completeSetButton.setDisable(true);
        abortButton.setDisable(true);
        sessionStatusLabel.setText(message);
        notice(message);
    }

    /** Session-status copy shared by the labels and snackbars (sentence case). */
    private static String statusLabel(SessionStatus status) {
        return switch (status) {
            case REQUESTED -> "Requested";
            case CONFIRMED -> "Confirmed";
            case COMPLETED -> "Completed";
            case NO_SHOW -> "No-show";
            case CANCELLED -> "Cancelled";
        };
    }

    /**
     * COMPLETE SESSION: rule 18 transition CONFIRMED -> COMPLETED through {@code SessionDao}
     * (the DAO owns the transition check); the HUD enters its terminal state and the schedule
     * data is the source of truth on the next visit.
     */
    public void onCompleteSession() {
        if (activeSession == null || sessionFinished) {
            notice("No CONFIRMED session is running — nothing to complete.");
            return;
        }
        int sessionId = activeSession.getId();
        Data.submit(
                () -> {
                    Session current = new SessionDao().findById(sessionId);
                    if (current == null) {
                        throw new com.gym.exception.ValidationException(
                                "Session no longer exists in the database");
                    }
                    current.setStatus(SessionStatus.COMPLETED);
                    new SessionDao().update(current);
                    return current;
                },
                completed -> endSessionLocally("Session #" + completed.getId()
                        + " completed — saved as COMPLETED in the database."),
                error -> notice("Could not complete session: " + error.getMessage()));
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
