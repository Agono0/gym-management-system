package io.m3fx.controls.progress;

import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
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
 * {@link #INDETERMINATE} waves across the full track. Track 4dp on surface-container-highest,
 * wave in primary. Canvas-drawn, so theme colors are resolved via {@code ThemeColors} and cached
 * per frame tick.
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
        setMinHeight(16);
        setPrefHeight(16);
        setMaxHeight(16);
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
        private static final double WAVELENGTH = 28;
        private static final double AMPLITUDE = 5;
        private static final double STROKE = 3;

        private final Pane root = new Pane();
        private final Canvas canvas = new Canvas();
        private final AnimationTimer loop;
        private double phase;

        WavySkin(M3WavyProgress control) {
            super(control);
            root.getStyleClass().add("m3-wavy-root");
            root.getChildren().add(canvas);
            canvas.widthProperty().bind(root.widthProperty());
            canvas.heightProperty().bind(root.heightProperty());
            getChildren().add(root);
            // Frame-synced timer (not a 16ms Timeline) so the wave never stutters on busy pulses.
            loop =
                    new AnimationTimer() {
                        private long last = -1;

                        @Override
                        public void handle(long now) {
                            if (last < 0) {
                                last = now;
                                return;
                            }
                            double dt = (now - last) / 1_000_000_000.0;
                            last = now;
                            phase += dt * 7.5;
                            draw();
                        }
                    };
            loop.start();
            control.progressProperty().addListener((obs, oldV, newV) -> draw());
            control.widthProperty().addListener((obs, oldV, newV) -> draw());
            control.sceneProperty().addListener((obs, oldV, newV) -> draw());
        }

        @Override
        public void dispose() {
            loop.stop();
            super.dispose();
        }

        private void draw() {
            M3WavyProgress control = getSkinnable();
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
            g.fillRoundRect(0, centerY - 2, w, 4, 4, 4);
            double value = control.getProgress();
            double limit = value < 0 ? w : w * Math.min(1, Math.max(0, value));
            g.setStroke(wave);
            g.setLineWidth(STROKE);
            g.setLineCap(StrokeLineCap.ROUND);
            g.beginPath();
            boolean penDown = false;
            for (double x = 0; x <= limit; x += 2) {
                double y = centerY + AMPLITUDE * Math.sin((x / WAVELENGTH) * 2 * Math.PI + phase);
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
