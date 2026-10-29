package io.m3fx.controls.workout;

import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.slider.M3Slider;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Unified Live Smart Gym Workout HUD Cockpit bringing together kinetic rep counting,
 * cardiac intensity zones, and HIIT interval timing.
 */
public class M3WorkoutHUD extends VBox {

    private final M3RepCounter repCounter = new M3RepCounter(12, 4);
    private final M3IntervalTimer intervalTimer = new M3IntervalTimer(45, 15, 8);
    private final M3HeartRateZones heartRateZones = new M3HeartRateZones(190);

    private final Label exerciseTitle = new Label("BARBELL BENCH PRESS — HYPERTROPHY PROTOCOL");

    /** Creates a unified live workout HUD station. */
    public M3WorkoutHUD() {
        getStyleClass().add("m3-workout-hud");
        setSpacing(20);
        setPadding(new Insets(16));

        exerciseTitle.setFont(Font.font("Roboto", FontWeight.BOLD, 16));
        exerciseTitle.setStyle("-fx-text-fill: -md-sys-color-primary;");

        // Upper Row: Rep Counter Card & Interval Timer Card
        M3Card repCard = new M3Card(CardVariant.OUTLINED);
        VBox repBox = new VBox(10, new Label("Kinetic Rep Counter & Cadence"), repCounter);
        repBox.setAlignment(Pos.CENTER);
        repBox.setPadding(new Insets(14));
        repCard.setContent(repBox);
        HBox.setHgrow(repCard, Priority.ALWAYS);

        M3Card timerCard = new M3Card(CardVariant.OUTLINED);
        VBox timerBox = new VBox(10, new Label("HIIT / Tabata Interval Work-Rest Timer"), intervalTimer);
        timerBox.setAlignment(Pos.CENTER);
        timerBox.setPadding(new Insets(14));
        timerCard.setContent(timerBox);
        HBox.setHgrow(timerCard, Priority.ALWAYS);

        HBox upperRow = new HBox(18, repCard, timerCard);
        upperRow.setAlignment(Pos.CENTER);

        // Lower Row: Cardiac Zones & Heart Rate Simulator
        M3Card hrCard = new M3Card(CardVariant.OUTLINED);
        M3Slider bpmSlider = new M3Slider(60, 200, 142);
        bpmSlider.setPrefWidth(260);
        bpmSlider.valueProperty().addListener((obs, o, n) -> heartRateZones.setCurrentBpm(n.intValue()));

        HBox sliderRow = new HBox(12, new Label("Simulate Live Pulse:"), bpmSlider);
        sliderRow.setAlignment(Pos.CENTER_LEFT);
        sliderRow.setPadding(new Insets(0, 14, 10, 14));

        VBox hrBox = new VBox(8, heartRateZones, sliderRow);
        hrCard.setContent(hrBox);

        getChildren().addAll(exerciseTitle, upperRow, hrCard);
    }

    /** Returns the kinetic rep counter panel. */
    public M3RepCounter getRepCounter() {
        return repCounter;
    }

    /** Returns the HIIT / Tabata interval timer panel. */
    public M3IntervalTimer getIntervalTimer() {
        return intervalTimer;
    }

    /** Returns the cardiac zone visualizer panel. */
    public M3HeartRateZones getHeartRateZones() {
        return heartRateZones;
    }

    /**
     * Sets the exercise heading, upper-cased for the HUD style.
     *
     * @param name exercise name; {@code null} resets it to empty
     */
    public void setExerciseName(String name) {
        exerciseTitle.setText(name == null ? "" : name.toUpperCase());
    }
}
