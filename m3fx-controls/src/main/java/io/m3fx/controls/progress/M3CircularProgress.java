package io.m3fx.controls.progress;

import io.m3fx.core.theme.ColorRole;
import io.m3fx.controls.internal.ThemeColors;
import javafx.animation.AnimationTimer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.AccessibleRole;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;

/**
 * Dedicated M3 Circular Progress indicator: authentic Material 3 rotating sweep
 * arc physics in indeterminate mode, and smooth 0-1 progress arc in determinate mode.
 *
 * <p>See <a href="https://m3.material.io/components/progress-indicators/overview">M3 Progress indicators</a>.
 * 4dp stroke thickness, rounded stroke caps, active color {@code -md-sys-color-primary}.
 */
public class M3CircularProgress extends Control {

    public static final double INDETERMINATE = -1.0;

    private final DoubleProperty progress =
            new SimpleDoubleProperty(this, "progress", INDETERMINATE);
    private final BooleanProperty indeterminate =
            new SimpleBooleanProperty(this, "indeterminate", true);

    /** Creates an indeterminate circular progress indicator. */
    public M3CircularProgress() {
        this(INDETERMINATE);
    }

    /**
     * Creates a circular progress indicator with a progress value.
     *
     * @param progress 0.0 to 1.0, or {@link #INDETERMINATE}
     */
    public M3CircularProgress(double progress) {
        getStyleClass().add("m3-circular-progress");
        setAccessibleRole(AccessibleRole.PROGRESS_INDICATOR);
        setFocusTraversable(false);
        setMinSize(24, 24);
        setPrefSize(48, 48);
        setMaxSize(120, 120);

        setProgress(progress);
        progressProperty().addListener((obs, oldV, newV) -> {
            boolean isIndet = newV.doubleValue() < 0;
            indeterminate.set(isIndet);
            pseudoClassStateChanged(
                    PseudoClass.getPseudoClass("indeterminate"), isIndet);
        });
        this.indeterminate.set(progress < 0);
        pseudoClassStateChanged(PseudoClass.getPseudoClass("indeterminate"), progress < 0);
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public double getProgress() {
        return progress.get();
    }

    public void setProgress(double progress) {
        this.progress.set(progress);
    }

    public BooleanProperty indeterminateProperty() {
        return indeterminate;
    }

    public boolean isIndeterminate() {
        return indeterminate.get();
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new CircularSkin(this);
    }

    static final class CircularSkin extends SkinBase<M3CircularProgress> {
        // Spec values (material-web/progress/internal/_circular-progress.scss, which documents
        // them as matching current gm3 values): ARCSIZE = 270deg, ARCTIME = 1333ms,
        // ARCSTARTROT = 216deg, linear-rotate duration = ARCTIME * 360 / 306.
        private static final double STROKE = 4; // md.comp.circular-progress active-indicator-width
        private static final double SWEEP_MIN = 25;
        private static final double SWEEP_MAX = 270;
        private static final double ROTATE_DEG_PER_SEC = 306_000.0 / 1333; // 360deg / ~1568ms
        private static final double SWEEP_DEG_PER_SEC =
                (SWEEP_MAX - SWEEP_MIN) * 1000.0 / 1333; // one sweep leg per ARCTIME

        private final Canvas canvas = new Canvas(48, 48);
        private AnimationTimer timer;
        private double rotationAngle = 0;
        private double sweepAngle = SWEEP_MIN;
        private boolean expanding = true;
        private long lastTime = -1;

        private final ChangeListener<Number> widthListener = (o, ov, nv) -> redraw();
        private final ChangeListener<Number> heightListener = (o, ov, nv) -> redraw();
        private final ChangeListener<Number> progressListener = (o, ov, nv) -> redraw();
        private final ChangeListener<Boolean> indeterminateListener = (o, ov, nv) ->
                handleModeChange(nv);
        private final ChangeListener<javafx.scene.Scene> sceneListener = (o, ov, nv) -> {
            M3CircularProgress control = getSkinnable();
            if (nv == null) {
                stopTimer();
            } else if (control != null && control.isIndeterminate()) {
                startTimer();
            }
        };

        CircularSkin(M3CircularProgress control) {
            super(control);
            getChildren().add(canvas);

            canvas.widthProperty().bind(control.widthProperty());
            canvas.heightProperty().bind(control.heightProperty());

            control.widthProperty().addListener(widthListener);
            control.heightProperty().addListener(heightListener);
            control.progressProperty().addListener(progressListener);
            control.indeterminateProperty().addListener(indeterminateListener);
            control.sceneProperty().addListener(sceneListener);

            handleModeChange(control.isIndeterminate());
        }

        private void handleModeChange(boolean indeterminate) {
            if (indeterminate) {
                startTimer();
            } else {
                stopTimer();
                redraw();
            }
        }

        private void startTimer() {
            if (timer != null) {
                return;
            }
            timer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    if (lastTime < 0) {
                        lastTime = now;
                        return;
                    }
                    // Clamp dt so a long tab-out cannot make the arc jump.
                    double dt = Math.min((now - lastTime) / 1_000_000_000.0, 0.1);
                    lastTime = now;

                    // Base circle rotation: 360deg per linear-rotate duration (~1568ms).
                    rotationAngle = (rotationAngle + dt * ROTATE_DEG_PER_SEC) % 360;

                    // Sweep the arc between SWEEP_MIN and SWEEP_MAX (one leg per ARCTIME).
                    double sweepSpeed = dt * SWEEP_DEG_PER_SEC;
                    if (expanding) {
                        sweepAngle += sweepSpeed;
                        if (sweepAngle >= SWEEP_MAX) {
                            sweepAngle = SWEEP_MAX;
                            expanding = false;
                        }
                    } else {
                        sweepAngle -= sweepSpeed;
                        if (sweepAngle <= SWEEP_MIN) {
                            sweepAngle = SWEEP_MIN;
                            expanding = true;
                        }
                    }

                    redraw();
                }
            };
            timer.start();
        }

        private void stopTimer() {
            if (timer != null) {
                timer.stop();
                timer = null;
            }
            lastTime = -1;
        }

        private void redraw() {
            M3CircularProgress c = getSkinnable();
            if (c == null) {
                return;
            }
            double w = canvas.getWidth();
            double h = canvas.getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }

            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, w, h);

            Color primary = ThemeColors.resolve(c, ColorRole.PRIMARY);
            Color trackColor = ThemeColors.resolve(c, ColorRole.SURFACE_CONTAINER_HIGHEST);

            // md.comp.circular-progress: 48px size, 4px active-indicator width (stroke).
            double strokeWidth = STROKE;
            double pad = strokeWidth + 2;
            double diam = Math.min(w, h) - pad * 2;
            if (diam <= 0) {
                return;
            }

            double x = (w - diam) / 2.0;
            double y = (h - diam) / 2.0;

            gc.setLineCap(StrokeLineCap.ROUND);
            gc.setLineWidth(strokeWidth);

            // Draw background track ring
            gc.setStroke(trackColor);
            gc.strokeOval(x, y, diam, diam);

            // Draw active arc
            gc.setStroke(primary);
            if (c.isIndeterminate()) {
                double startAngle = (rotationAngle - (expanding ? 0 : (270 - sweepAngle))) % 360;
                gc.strokeArc(x, y, diam, diam, -startAngle, -sweepAngle, ArcType.OPEN);
            } else {
                double frac = Math.max(0.0, Math.min(1.0, c.getProgress()));
                double extent = frac * 360.0;
                // Start from top (-90 degrees)
                gc.strokeArc(x, y, diam, diam, 90, -extent, ArcType.OPEN);
            }
        }

        @Override
        public void dispose() {
            stopTimer();
            M3CircularProgress control = getSkinnable();
            if (control != null) {
                control.widthProperty().removeListener(widthListener);
                control.heightProperty().removeListener(heightListener);
                control.progressProperty().removeListener(progressListener);
                control.indeterminateProperty().removeListener(indeterminateListener);
                control.sceneProperty().removeListener(sceneListener);
            }
            canvas.widthProperty().unbind();
            canvas.heightProperty().unbind();
            super.dispose();
        }
    }
}

