package io.m3fx.controls.progress;

import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.AccessibleRole;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

/**
 * M3 Expressive wavy progress indicator: the active indicator is a travelling sine wave instead of
 * a straight bar.
 *
 * <p>Determinate mode ({@code progress} 0-1) clips the wave to the completed fraction;
 * {@link #INDETERMINATE} waves across the full track. Values follow the Jetpack Compose material3
 * Expressive tokens (v0_7_0 {@code LinearProgressIndicatorTokens}): 4dp stroke, 3dp active wave
 * amplitude, 40dp determinate / 20dp indeterminate wavelength, 10dp container height, and the wave
 * flattens outside the 10%-95% progress range (the {@code indicatorAmplitude} default). Track 4dp
 * on surface-container-highest, wave in primary. Canvas-drawn, so theme colors are resolved via
 * {@code ThemeColors} and cached per frame tick.
 */
public class M3WavyProgress extends Control {

    /** Value meaning "indeterminate". */
    public static final double INDETERMINATE = -1;

    private final DoubleProperty progress =
            new SimpleDoubleProperty(this, "progress", INDETERMINATE);

    /** Creates an indeterminate wavy indicator. */
    public M3WavyProgress() {
        this(INDETERMINATE);
    }

    /**
     * Creates a wavy indicator.
     *
     * @param progress 0-1, or {@link #INDETERMINATE}
     */
    public M3WavyProgress(double progress) {
        getStyleClass().add("m3-wavy-progress");
        setProgress(progress);
        setAccessibleRole(AccessibleRole.PROGRESS_INDICATOR);
        setFocusTraversable(false);
        // WaveHeight token: 10dp container (3dp amplitude + 4dp stroke).
        setMinHeight(10);
        setPrefHeight(10);
        setMaxHeight(10);
    }

    /** Returns the progress property. */
    public DoubleProperty progressProperty() {
        return progress;
    }

    /** Returns the progress value. */
    public double getProgress() {
        return progress.get();
    }

    /**
     * Sets progress (0-1) or {@link #INDETERMINATE}.
     *
     * @param progress the new value
     */
    public void setProgress(double progress) {
        this.progress.set(progress);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new WavySkin(this);
    }

    /** Skin drawing the animated wave on a canvas. */
    static final class WavySkin extends SkinBase<M3WavyProgress> {
        // Jetpack Compose material3 Expressive tokens (LinearProgressIndicatorTokens v0_7_0).
        private static final double WAVELENGTH_DETERMINATE = 40; // ActiveWaveWavelength
        private static final double WAVELENGTH_INDETERMINATE = 20; // IndeterminateActiveWave...
        private static final double AMPLITUDE = 3; // ActiveWaveAmplitude
        private static final double STROKE = 4; // TrackThickness / ActiveThickness
        private static final double FLAT_BELOW = 0.10; // indicatorAmplitude default range
        private static final double FLAT_ABOVE = 0.95;

        private final Pane root = new Pane();
        private final Canvas canvas = new Canvas();
        private double phase;
        // Frame-synced timer (not a 16ms Timeline) so the wave never stutters on busy pulses.
        private final AnimationTimer loop =
                new AnimationTimer() {
                    private long last = -1;

                    @Override
                    public void handle(long now) {
                        if (last < 0) {
                            last = now;
                            return;
                        }
                        // Clamp dt so a long tab-out cannot make the phase jump wildly.
                        double dt = Math.min((now - last) / 1_000_000_000.0, 0.1);
                        last = now;
                        phase += dt * 7.5;
                        draw();
                    }
                };

        private final ChangeListener<Number> progressListener = (obs, oldV, newV) -> draw();
        private final ChangeListener<Number> widthListener = (obs, oldV, newV) -> draw();
        private final ChangeListener<javafx.scene.Scene> sceneListener = (obs, oldV, newV) -> {
            if (newV == null) {
                loop.stop();
            } else {
                loop.start();
                draw();
            }
        };

        WavySkin(M3WavyProgress control) {
            super(control);
            root.getStyleClass().add("m3-wavy-root");
            root.getChildren().add(canvas);
            canvas.widthProperty().bind(root.widthProperty());
            canvas.heightProperty().bind(root.heightProperty());
            getChildren().add(root);
            loop.start();
            control.progressProperty().addListener(progressListener);
            control.widthProperty().addListener(widthListener);
            control.sceneProperty().addListener(sceneListener);
        }

        @Override
        public void dispose() {
            loop.stop();
            M3WavyProgress control = getSkinnable();
            if (control != null) {
                control.progressProperty().removeListener(progressListener);
                control.widthProperty().removeListener(widthListener);
                control.sceneProperty().removeListener(sceneListener);
            }
            canvas.widthProperty().unbind();
            canvas.heightProperty().unbind();
            super.dispose();
        }

        private void draw() {
            M3WavyProgress control = getSkinnable();
            if (control == null) {
                return;
            }
            double w = Math.max(0, canvas.getWidth());
            double h = Math.max(0, canvas.getHeight());
            if (w <= 0 || h <= 0) {
                return;
            }
            Color track = ThemeColors.resolve(control, ColorRole.SURFACE_CONTAINER_HIGHEST);
            Color wave = ThemeColors.resolve(control, ColorRole.PRIMARY);
            GraphicsContext g = canvas.getGraphicsContext2D();
            g.clearRect(0, 0, w, h);
            double centerY = h / 2;
            g.setFill(track);
            g.fillRoundRect(0, centerY - STROKE / 2, w, STROKE, STROKE, STROKE);
            double value = control.getProgress();
            boolean indeterminate = value < 0;
            double limit = indeterminate ? w : w * Math.min(1, Math.max(0, value));
            double wavelength = indeterminate ? WAVELENGTH_INDETERMINATE : WAVELENGTH_DETERMINATE;
            // The wave flattens outside 10%-95% progress (Compose indicatorAmplitude default).
            double amplitude = AMPLITUDE;
            if (!indeterminate && (value < FLAT_BELOW || value > FLAT_ABOVE)) {
                amplitude = 0;
            }
            g.setStroke(wave);
            g.setLineWidth(STROKE);
            g.setLineCap(StrokeLineCap.ROUND);
            g.beginPath();
            boolean penDown = false;
            for (double x = 0; x <= limit; x += 2) {
                double y = centerY + amplitude * Math.sin((x / wavelength) * 2 * Math.PI + phase);
                if (!penDown) {
                    g.moveTo(x, y);
                    penDown = true;
                } else {
                    g.lineTo(x, y);
                }
            }
            g.stroke();
        }
    }
}
