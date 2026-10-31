package io.m3fx.controls.workout;

import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.internal.SpringAnimation;
import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.property.*;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;

import java.util.ArrayList;
import java.util.List;

/**
 * Kinetic M3 Repetition Counter featuring spring-assisted scale bounce physics,
 * real-time cadence velocity estimation (reps/min), circular set progress, and target celebrations.
 */
// Suppressed this-escape (false positives javac 27 cannot see across the module boundary):
// (1) the JavaFX property-bean idiom new Simple*Property(this, ...) - the property
// constructor only stores the bean reference, exactly like every built-in control;
// (2) constructor calls to JavaFX final methods (getStyleClass(), setAccessibleRole())
// - final, but javac does not honor ACC_FINAL for symbols from another module. This
// class's own overridable methods are never called during construction.
@SuppressWarnings("this-escape")
public class M3RepCounter extends VBox {

    private final IntegerProperty reps = new SimpleIntegerProperty(this, "reps", 0);
    private final IntegerProperty targetReps = new SimpleIntegerProperty(this, "targetReps", 12);
    private final IntegerProperty currentSet = new SimpleIntegerProperty(this, "currentSet", 1);
    private final IntegerProperty totalSets = new SimpleIntegerProperty(this, "totalSets", 4);
    private final DoubleProperty cadence = new SimpleDoubleProperty(this, "cadence", 0.0);

    private final Canvas canvas = new Canvas(220, 220);
    private final Scale bounceScale = new Scale(1.0, 1.0, 110, 110);
    private final Label setLabel = new Label();
    private final Label cadenceLabel = new Label();

    private final List<Long> repTimestamps = new ArrayList<>();

    private static final Font REP_FONT = Font.font("Roboto", FontWeight.BOLD, 46);
    private static final Font GOAL_FONT = Font.font("Roboto", FontWeight.BOLD, 12);

    /** Creates a rep counter with standard 12 target reps across 4 sets. */
    public M3RepCounter() {
        this(12, 4);
    }

    /**
     * Creates a rep counter with configured targets.
     *
     * @param targetReps goal reps per set
     * @param totalSets total workout sets
     */
    public M3RepCounter(int targetReps, int totalSets) {
        getStyleClass().add("m3-rep-counter");
        setAlignment(Pos.CENTER);
        setSpacing(12);

        setTargetReps(targetReps);
        setTotalSets(totalSets);

        canvas.getTransforms().add(bounceScale);

        setLabel.getStyleClass().add("m3-rep-counter-set");
        setLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 13));
        updateSetLabel();

        cadenceLabel.getStyleClass().add("m3-rep-counter-cadence");
        cadenceLabel.setFont(Font.font("Roboto", FontWeight.NORMAL, 12));
        cadenceLabel.setText("Cadence: 0 reps/min");

        // Action Buttons
        M3Button repBtn = new M3Button("Count Rep (+1)", ButtonVariant.FILLED);
        repBtn.setOnAction(e -> countRep());

        M3Button nextSetBtn = new M3Button("Next Set", ButtonVariant.TONAL);
        nextSetBtn.setOnAction(e -> nextSet());

        M3Button resetBtn = new M3Button("Reset", ButtonVariant.OUTLINED);
        resetBtn.setOnAction(e -> reset());

        HBox buttonBar = new HBox(8, repBtn, nextSetBtn, resetBtn);
        buttonBar.setAlignment(Pos.CENTER);

        getChildren().addAll(setLabel, canvas, cadenceLabel, buttonBar);

        reps.addListener((obs, o, n) -> {
            redraw();
            triggerBounce();
        });
        this.targetReps.addListener(e -> redraw());
        this.currentSet.addListener(e -> {
            updateSetLabel();
            redraw();
        });
        sceneProperty().addListener((obs, o, n) -> redraw());

        redraw();
    }

    /** Registers a completed repetition with spring bounce and cadence update. */
    public void countRep() {
        long now = System.currentTimeMillis();
        repTimestamps.add(now);

        // Prune timestamps older than 30 seconds
        repTimestamps.removeIf(t -> now - t > 30_000);

        if (repTimestamps.size() >= 2) {
            long durationMs = now - repTimestamps.get(0);
            if (durationMs > 1000) {
                double rpm = (repTimestamps.size() - 1) / (durationMs / 60000.0);
                cadence.set(Math.round(rpm * 10.0) / 10.0);
            }
        }
        cadenceLabel.setText(String.format("Cadence: %.1f reps/min", cadence.get()));

        setReps(getReps() + 1);
    }

    /** Advances to the next set. */
    public void nextSet() {
        if (getCurrentSet() < getTotalSets()) {
            setCurrentSet(getCurrentSet() + 1);
            setReps(0);
            repTimestamps.clear();
            cadence.set(0.0);
            cadenceLabel.setText("Cadence: 0 reps/min");
        }
    }

    /** Resets the counter. */
    public void reset() {
        setReps(0);
        setCurrentSet(1);
        repTimestamps.clear();
        cadence.set(0.0);
        cadenceLabel.setText("Cadence: 0 reps/min");
    }

    private void triggerBounce() {
        // Kinetic scale impulse 1.0 -> 1.24 -> 1.0
        bounceScale.setX(1.24);
        bounceScale.setY(1.24);
        SpringAnimation.animate(bounceScale.xProperty(), 1.0, SpringAnimation.BOUNCY, null);
        SpringAnimation.animate(bounceScale.yProperty(), 1.0, SpringAnimation.BOUNCY, null);
    }

    private void updateSetLabel() {
        setLabel.setText(String.format("SET %d OF %d", getCurrentSet(), getTotalSets()));
    }

    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, 220, 220);

        double cx = 110.0;
        double cy = 110.0;
        double radius = 90.0;
        double trackW = 12.0;

        int curReps = getReps();
        int target = Math.max(1, getTargetReps());
        // Clamp: negative rep values would flip the arc direction and draw backwards.
        double progress = Math.max(0.0, Math.min(1.0, (double) curReps / target));

        // Track Ring
        Color trackColor = ThemeColors.resolve(this, ColorRole.OUTLINE_VARIANT);
        gc.setStroke(Color.color(trackColor.getRed(), trackColor.getGreen(), trackColor.getBlue(), 0.3));
        gc.setLineWidth(trackW);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, -360, ArcType.OPEN);

        // Progress Arc
        Color primary = ThemeColors.resolve(this, ColorRole.PRIMARY);
        Color accent = (curReps >= target) ? ThemeColors.resolve(this, ColorRole.TERTIARY) : primary;

        if (progress > 0) {
            gc.setStroke(accent);
            gc.setLineWidth(trackW);
            gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, -360 * progress, ArcType.OPEN);
        }

        // Center Readout
        Color onSurface = ThemeColors.resolve(this, ColorRole.ON_SURFACE);
        Color onSurfaceVar = ThemeColors.resolve(this, ColorRole.ON_SURFACE_VARIANT);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        // Rep count
        gc.setFill(onSurface);
        gc.setFont(REP_FONT);
        gc.fillText(String.valueOf(curReps), cx, cy - 8);

        // Reps label
        gc.setFill(onSurfaceVar);
        gc.setFont(GOAL_FONT);
        String sub = (curReps >= target) ? "TARGET MET!" : ("GOAL: " + target);
        gc.fillText(sub, cx, cy + 28);
    }

    /** The rep count in the current set, resetting on set change. */
    public IntegerProperty repsProperty() {
        return reps;
    }

    /** Gets the rep count in the current set. */
    public int getReps() {
        return reps.get();
    }

    /** Sets the rep count in the current set. */
    public void setReps(int reps) {
        this.reps.set(reps);
    }

    /** The goal reps per set; the arc turns tertiary once reached. */
    public IntegerProperty targetRepsProperty() {
        return targetReps;
    }

    /** Gets the goal reps per set. */
    public int getTargetReps() {
        return targetReps.get();
    }

    /** Sets the goal reps per set. */
    public void setTargetReps(int targetReps) {
        this.targetReps.set(targetReps);
    }

    /** The set currently in progress, starting at 1. */
    public IntegerProperty currentSetProperty() {
        return currentSet;
    }

    /** Gets the set currently in progress, starting at 1. */
    public int getCurrentSet() {
        return currentSet.get();
    }

    /** Sets the set currently in progress. */
    public void setCurrentSet(int currentSet) {
        this.currentSet.set(currentSet);
    }

    /** The total number of sets in the workout. */
    public IntegerProperty totalSetsProperty() {
        return totalSets;
    }

    /** Gets the total number of sets in the workout. */
    public int getTotalSets() {
        return totalSets.get();
    }

    /** Sets the total number of sets in the workout. */
    public void setTotalSets(int totalSets) {
        this.totalSets.set(totalSets);
    }

    /** Estimated tempo from recent reps in reps/minute (0 until two reps are recorded). */
    public DoubleProperty cadenceProperty() {
        return cadence;
    }

    /** Gets the estimated cadence in reps/minute. */
    public double getCadence() {
        return cadence.get();
    }
}
