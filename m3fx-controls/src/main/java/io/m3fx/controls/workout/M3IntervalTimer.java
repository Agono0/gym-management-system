package io.m3fx.controls.workout;

import io.m3fx.controls.button.ButtonVariant;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.animation.AnimationTimer;
import javafx.beans.property.*;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * HIIT / Tabata High-Intensity Interval Countdown Timer featuring circular progress sweep,
 * Work/Rest state transitions, and round tracking.
 */
// Suppressed this-escape (false positives javac 27 cannot see across the module boundary):
// (1) the JavaFX property-bean idiom new Simple*Property(this, ...) - the property
// constructor only stores the bean reference, exactly like every built-in control;
// (2) constructor calls to JavaFX final methods (getStyleClass(), setAccessibleRole())
// - final, but javac does not honor ACC_FINAL for symbols from another module. This
// class's own overridable methods are never called during construction.
@SuppressWarnings("this-escape")
public class M3IntervalTimer extends VBox {

    /** Interval states. */
    public enum IntervalState {
        /** Not started or reset; the dial shows the full work interval. */
        STOPPED,
        /** High-intensity phase; arc drawn with the theme primary color. */
        WORK,
        /** Recovery phase; arc drawn with the theme secondary color. */
        REST,
        /** All rounds finished; the dial is drained and the loop stops. */
        COMPLETE
    }

    private final IntegerProperty workSeconds = new SimpleIntegerProperty(this, "workSeconds", 45);
    private final IntegerProperty restSeconds = new SimpleIntegerProperty(this, "restSeconds", 15);
    private final IntegerProperty totalRounds = new SimpleIntegerProperty(this, "totalRounds", 8);
    private final IntegerProperty currentRound = new SimpleIntegerProperty(this, "currentRound", 1);
    private final DoubleProperty secondsRemaining = new SimpleDoubleProperty(this, "secondsRemaining", 45.0);

    private final ObjectProperty<IntervalState> state =
            new SimpleObjectProperty<>(this, "state", IntervalState.STOPPED);
    private final BooleanProperty running =
            new SimpleBooleanProperty(this, "running", false);

    private final Canvas canvas = new Canvas(200, 200);
    private final Label roundLabel = new Label();
    private final M3Button startPauseBtn = new M3Button("Start Interval", ButtonVariant.FILLED);
    private final M3Button resetBtn = new M3Button("Reset", ButtonVariant.OUTLINED);

    private AnimationTimer loop;
    private long lastNanos = 0;

    private static final Font DIGITS_FONT = Font.font("Roboto", FontWeight.BOLD, 36);
    private static final Font PHASE_FONT = Font.font("Roboto", FontWeight.BOLD, 12);

    /** Creates an interval timer with 45s work / 15s rest across 8 rounds. */
    public M3IntervalTimer() {
        this(45, 15, 8);
    }

    /**
     * Creates an interval timer with custom work/rest/rounds configuration.
     *
     * @param workSec seconds for work interval
     * @param restSec seconds for rest interval
     * @param rounds total interval rounds
     */
    public M3IntervalTimer(int workSec, int restSec, int rounds) {
        getStyleClass().add("m3-interval-timer");
        setAlignment(Pos.CENTER);
        setSpacing(10);

        setWorkSeconds(workSec);
        setRestSeconds(restSec);
        setTotalRounds(rounds);
        setSecondsRemaining(workSec);

        roundLabel.getStyleClass().add("m3-interval-timer-round");
        roundLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 13));
        updateRoundLabel();

        startPauseBtn.setOnAction(e -> toggleRunning());
        resetBtn.setOnAction(e -> reset());

        HBox buttonBar = new HBox(8, startPauseBtn, resetBtn);
        buttonBar.setAlignment(Pos.CENTER);

        getChildren().addAll(roundLabel, canvas, buttonBar);

        secondsRemaining.addListener(e -> redraw());
        state.addListener(e -> redraw());
        currentRound.addListener(e -> updateRoundLabel());
        // Keep the round label and the countdown arc in sync with config changes.
        totalRounds.addListener(e -> updateRoundLabel());
        workSeconds.addListener(e -> {
            if (getState() == IntervalState.STOPPED) {
                setSecondsRemaining(getWorkSeconds());
            }
            redraw();
        });
        restSeconds.addListener(e -> redraw());
        sceneProperty().addListener((obs, o, n) -> {
            redraw();
            if (n == null) {
                pause();
            }
        });
        visibleProperty().addListener(e -> syncLoopWithVisibility());
        disabledProperty().addListener(e -> syncLoopWithVisibility());

        redraw();
    }

    /** True when the timer dial is currently on screen and interactive. */
    private boolean isShowing() {
        return getScene() != null && isVisible() && !isDisable();
    }

    /**
     * Starts or stops the tick loop to match the running flag and the node's visibility,
     * without touching the user-facing running state (a hidden workout tab resumes cleanly).
     */
    private void syncLoopWithVisibility() {
        if (isRunning() && isShowing()) {
            if (loop != null) {
                loop.start();
            }
        } else if (loop != null) {
            loop.stop();
        }
    }

    /** Toggles the running timer state. */
    public void toggleRunning() {
        if (isRunning()) {
            pause();
        } else {
            start();
        }
    }

    /** Starts the countdown. */
    public void start() {
        if (getState() == IntervalState.STOPPED || getState() == IntervalState.COMPLETE) {
            setState(IntervalState.WORK);
            setSecondsRemaining(getWorkSeconds());
        }
        setRunning(true);
        startPauseBtn.setText("Pause");
        lastNanos = 0;

        if (loop == null) {
            loop = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    if (lastNanos == 0) {
                        lastNanos = now;
                        return;
                    }
                    double dt = (now - lastNanos) / 1_000_000_000.0;
                    lastNanos = now;

                    tick(dt);
                }
            };
        }
        // Only tick while actually on screen; syncLoopWithVisibility resumes on re-show.
        if (isShowing()) {
            loop.start();
        }
    }

    /** Pauses the timer. */
    public void pause() {
        setRunning(false);
        startPauseBtn.setText("Resume");
        if (loop != null) {
            loop.stop();
        }
    }

    /** Resets the timer back to start of round 1. */
    public void reset() {
        pause();
        setState(IntervalState.STOPPED);
        setCurrentRound(1);
        setSecondsRemaining(getWorkSeconds());
        startPauseBtn.setText("Start Interval");
        redraw();
    }

    private void tick(double dt) {
        double rem = getSecondsRemaining() - dt;
        if (rem <= 0) {
            transitionNextPhase();
            // Carry the overshoot into the new phase so wall-clock time is preserved
            // (a long frame stall must not silently reset the new interval to full length).
            if (isRunning()) {
                setSecondsRemaining(Math.max(0.0, getSecondsRemaining() + rem));
            }
        } else {
            setSecondsRemaining(rem);
        }
    }

    private void transitionNextPhase() {
        if (getState() == IntervalState.WORK) {
            // Work finished -> start rest
            setState(IntervalState.REST);
            setSecondsRemaining(getRestSeconds());
        } else if (getState() == IntervalState.REST) {
            // Rest finished -> next round or complete
            if (getCurrentRound() < getTotalRounds()) {
                setCurrentRound(getCurrentRound() + 1);
                setState(IntervalState.WORK);
                setSecondsRemaining(getWorkSeconds());
            } else {
                setState(IntervalState.COMPLETE);
                setSecondsRemaining(0);
                pause();
                startPauseBtn.setText("Completed");
            }
        }
    }

    private void updateRoundLabel() {
        roundLabel.setText(String.format("ROUND %d OF %d", getCurrentRound(), getTotalRounds()));
    }

    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, 200, 200);

        double cx = 100.0;
        double cy = 100.0;
        double radius = 80.0;
        double trackW = 10.0;

        IntervalState st = getState();
        double totalSec = (st == IntervalState.REST) ? getRestSeconds() : getWorkSeconds();
        totalSec = Math.max(1.0, totalSec);
        double progress = Math.max(0.0, Math.min(1.0, getSecondsRemaining() / totalSec));

        // Background Track
        Color trackColor = ThemeColors.resolve(this, ColorRole.OUTLINE_VARIANT);
        gc.setStroke(Color.color(trackColor.getRed(), trackColor.getGreen(), trackColor.getBlue(), 0.3));
        gc.setLineWidth(trackW);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, -360, ArcType.OPEN);

        // Progress Arc
        Color arcColor;
        switch (st) {
            case WORK -> arcColor = ThemeColors.resolve(this, ColorRole.PRIMARY);
            case REST -> arcColor = ThemeColors.resolve(this, ColorRole.SECONDARY);
            case COMPLETE -> arcColor = ThemeColors.resolve(this, ColorRole.TERTIARY);
            default -> arcColor = ThemeColors.resolve(this, ColorRole.OUTLINE);
        }

        if (progress > 0) {
            gc.setStroke(arcColor);
            gc.setLineWidth(trackW);
            gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, -360 * progress, ArcType.OPEN);
        }

        // Center Time Readout
        Color onSurface = ThemeColors.resolve(this, ColorRole.ON_SURFACE);
        int remSecInt = (int) Math.ceil(getSecondsRemaining());
        int mins = remSecInt / 60;
        int secs = remSecInt % 60;
        String timeStr = String.format("%02d:%02d", mins, secs);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        // Digits
        gc.setFill(onSurface);
        gc.setFont(DIGITS_FONT);
        gc.fillText(timeStr, cx, cy - 8);

        // Phase tag
        gc.setFill(arcColor);
        gc.setFont(PHASE_FONT);
        gc.fillText(st.name(), cx, cy + 24);
    }

    /** Length of the high-intensity phase in seconds. */
    public IntegerProperty workSecondsProperty() {
        return workSeconds;
    }

    /** Gets the length of the high-intensity phase in seconds. */
    public int getWorkSeconds() {
        return workSeconds.get();
    }

    /** Sets the length of the high-intensity phase in seconds. */
    public void setWorkSeconds(int workSeconds) {
        this.workSeconds.set(workSeconds);
    }

    /** Length of the recovery phase in seconds. */
    public IntegerProperty restSecondsProperty() {
        return restSeconds;
    }

    /** Gets the length of the recovery phase in seconds. */
    public int getRestSeconds() {
        return restSeconds.get();
    }

    /** Sets the length of the recovery phase in seconds. */
    public void setRestSeconds(int restSeconds) {
        this.restSeconds.set(restSeconds);
    }

    /** Total number of work/rest rounds before the timer completes. */
    public IntegerProperty totalRoundsProperty() {
        return totalRounds;
    }

    /** Gets the total number of rounds. */
    public int getTotalRounds() {
        return totalRounds.get();
    }

    /** Sets the total number of work/rest rounds. */
    public void setTotalRounds(int totalRounds) {
        this.totalRounds.set(totalRounds);
    }

    /** The round currently in progress, starting at 1. */
    public IntegerProperty currentRoundProperty() {
        return currentRound;
    }

    /** Gets the round currently in progress, starting at 1. */
    public int getCurrentRound() {
        return currentRound.get();
    }

    /** Sets the round currently in progress. */
    public void setCurrentRound(int currentRound) {
        this.currentRound.set(currentRound);
    }

    /** Seconds left in the current phase, driving the countdown arc. */
    public DoubleProperty secondsRemainingProperty() {
        return secondsRemaining;
    }

    /** Gets the seconds left in the current phase. */
    public double getSecondsRemaining() {
        return secondsRemaining.get();
    }

    /** Sets the seconds left in the current phase. */
    public void setSecondsRemaining(double secondsRemaining) {
        this.secondsRemaining.set(secondsRemaining);
    }

    /** The current {@link IntervalState} of the timer. */
    public ObjectProperty<IntervalState> stateProperty() {
        return state;
    }

    /** Gets the current interval state. */
    public IntervalState getState() {
        return state.get();
    }

    /** Sets the current interval state. */
    public void setState(IntervalState state) {
        this.state.set(state);
    }

    /** Whether the countdown is actively ticking. */
    public BooleanProperty runningProperty() {
        return running;
    }

    /** Whether the countdown is actively ticking. */
    public boolean isRunning() {
        return running.get();
    }

    /** Sets whether the countdown is actively ticking. */
    public void setRunning(boolean running) {
        this.running.set(running);
    }
}

