package com.gym.controller;

import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.chart.M3SplineChart;
import io.m3fx.controls.workout.M3HeartRateZones;
import io.m3fx.controls.workout.M3IntervalTimer;
import io.m3fx.controls.workout.M3RepCounter;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * Pure UI skeleton controller for Live Workout HUD & Kinetic Cockpit screen.
 * Strictly conforms to Section 7 of docs/ui/workouts/LIVE_WORKOUT_HUD_UI.md and docs/UML.md.
 * All animation runs on the FX application thread via javafx.animation.Timeline.
 */
public class LiveWorkoutController {

    /** Session clock baseline matching the "00:34:12" shown in the FXML. */
    private static final long INITIAL_ELAPSED_SECONDS = 34L * 60 + 12;

    @FXML private M3HeartRateZones heartRateZonesBar;
    @FXML private Label athleteNameLabel;
    @FXML private Label exerciseTitleLabel;
    @FXML private Label setTrackerLabel;
    @FXML private Label targetLoadLabel;
    @FXML private Label elapsedSessionTimerLabel;

    @FXML private Label meanVelocityLabel;
    @FXML private Label peakVelocityLabel;
    @FXML private Label velocityLossLabel;
    @FXML private Label currentBpmLabel;
    @FXML private Label cardiacZoneLabel;

    @FXML private M3RepCounter repCounterDisplay;
    @FXML private Label cadenceDisplayLabel;
    @FXML private M3SplineChart cadenceSplineChart;

    @FXML private M3IntervalTimer intervalCountdownTimer;
    @FXML private Label timerStateLabel;
    @FXML private M3Button completeSetButton;

    /** FX-thread ticker driving the elapsed session clock; never touches the scene graph off-thread. */
    private Timeline sessionClock;
    private long elapsedSeconds = INITIAL_ELAPSED_SECONDS;

    public LiveWorkoutController() {
    }

    @FXML
    public void initialize() {
        // Match the configured rest window announced in the FXML ("Target Rest: 180s")
        // and start the rest countdown; M3IntervalTimer animates via its own FX AnimationTimer.
        if (intervalCountdownTimer != null) {
            intervalCountdownTimer.setRestSeconds(180);
            intervalCountdownTimer.start();
        }
        startSessionClock();
    }

    private void startSessionClock() {
        if (sessionClock != null) {
            sessionClock.stop();
        }
        sessionClock = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickSessionClock()));
        sessionClock.setCycleCount(Animation.INDEFINITE);
        sessionClock.play();
        // Pause the clock while the HUD is not on screen (screen swap) so no pulse is wasted
        if (elapsedSessionTimerLabel != null) {
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
    }

    private void tickSessionClock() {
        elapsedSeconds++;
        if (elapsedSessionTimerLabel != null) {
            elapsedSessionTimerLabel.setText(String.format("%02d:%02d:%02d",
                    elapsedSeconds / 3600, (elapsedSeconds % 3600) / 60, elapsedSeconds % 60));
        }
    }

    @FXML
    public void onManualRepDecrement() {
        if (repCounterDisplay != null) {
            repCounterDisplay.setReps(Math.max(0, repCounterDisplay.getReps() - 1));
        }
    }

    @FXML
    public void onManualRepDecrement(ActionEvent event) {
        onManualRepDecrement();
    }

    @FXML
    public void onManualRepIncrement() {
        if (repCounterDisplay != null) {
            repCounterDisplay.countRep();
        }
    }

    @FXML
    public void onManualRepIncrement(ActionEvent event) {
        onManualRepIncrement();
    }

    @FXML
    public void onSkipRest() {
        // No skip-to-next-round API exists on M3IntervalTimer yet; restarting would lose the
        // round progress, so the control's own start/pause/reset bar remains the interface.
    }

    @FXML
    public void onSkipRest(ActionEvent event) {
        onSkipRest();
    }

    @FXML
    public void onSelectRpe() {
        // No-op RPE handler
    }

    @FXML
    public void onSelectRpe(ActionEvent event) {
        // Move the active highlight to the tapped RPE button
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

    @FXML
    public void onAbortSet() {
        // Aborting stops both clocks and resets the current set
        if (sessionClock != null && sessionClock.getStatus() == Animation.Status.PAUSED) {
            sessionClock.play();
        }
        if (repCounterDisplay != null) {
            repCounterDisplay.reset();
        }
        if (intervalCountdownTimer != null) {
            intervalCountdownTimer.reset();
        }
    }

    @FXML
    public void onAbortSet(ActionEvent event) {
        onAbortSet();
    }

    @FXML
    public void onTogglePause() {
        // No-op toggle pause handler
    }

    @FXML
    public void onTogglePause(ActionEvent event) {
        boolean paused = sessionClock != null && sessionClock.getStatus() == Animation.Status.PAUSED;
        if (sessionClock != null) {
            if (paused) {
                sessionClock.play();
            } else {
                sessionClock.pause();
            }
        }
        if (intervalCountdownTimer != null) {
            if (paused) {
                intervalCountdownTimer.start();
            } else {
                intervalCountdownTimer.pause();
            }
        }
        if (event.getSource() instanceof M3Button pauseButton) {
            pauseButton.setText(paused ? "⏸️ PAUSE" : "▶ RESUME");
        }
    }

    @FXML
    public void onRecalibrateSensor() {
        // No-op recalibrate handler
    }

    @FXML
    public void onRecalibrateSensor(ActionEvent event) {
        onRecalibrateSensor();
    }

    @FXML
    public void onCompleteSet() {
        // A completed set advances the rep counter to the next set and restarts the rest window
        if (repCounterDisplay != null) {
            repCounterDisplay.nextSet();
        }
        if (intervalCountdownTimer != null) {
            intervalCountdownTimer.reset();
            intervalCountdownTimer.start();
        }
    }

    @FXML
    public void onCompleteSet(ActionEvent event) {
        onCompleteSet();
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
