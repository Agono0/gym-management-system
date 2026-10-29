package io.m3fx.controls.effect;

import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.animation.AnimationTimer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

/**
 * Futuristic HUD Telemetry Scanline overlay with corner sci-fi bracket markers,
 * horizontal rolling scan sweep, and crosshair reticle.
 */
public class M3Scanline extends Region {

    private final BooleanProperty animated = new SimpleBooleanProperty(this, "animated", true);
    private final DoubleProperty scanSpeed = new SimpleDoubleProperty(this, "scanSpeed", 90.0);

    private final Canvas canvas = new Canvas();
    private double scanlineY = 0;
    private AnimationTimer loop;

    // Cached per attach; rebuilt on scene change. ThemeColors must never be resolved per frame.
    private Color cachedPrimary;
    private LinearGradient beamGradient;

    /** Creates a scanline overlay. */
    public M3Scanline() {
        getStyleClass().add("m3-scanline");
        setMouseTransparent(true);
        getChildren().add(canvas);

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        animatedProperty().addListener((obs, o, n) -> updateAnimation(n));
        widthProperty().addListener((obs, o, n) -> render());
        heightProperty().addListener((obs, o, n) -> render());

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            cachedPrimary = null;
            beamGradient = null;
            updateAnimation(isAnimated());
        });
        visibleProperty().addListener(e -> updateAnimation(isAnimated()));
        disabledProperty().addListener(e -> updateAnimation(isAnimated()));

        updateAnimation(isAnimated());
    }

    /** True when the sweep should currently be animating. */
    private boolean shouldRun() {
        return getScene() != null && isVisible() && !isDisable();
    }

    public BooleanProperty animatedProperty() {
        return animated;
    }

    public boolean isAnimated() {
        return animated.get();
    }

    public void setAnimated(boolean animated) {
        this.animated.set(animated);
    }

    public DoubleProperty scanSpeedProperty() {
        return scanSpeed;
    }

    public double getScanSpeed() {
        return scanSpeed.get();
    }

    public void setScanSpeed(double speed) {
        this.scanSpeed.set(speed);
    }

    private void updateAnimation(boolean run) {
        boolean active = run && shouldRun();
        if (loop != null) loop.stop();
        if (active) {
            loop = new AnimationTimer() {
                private long last = 0;
                @Override
                public void handle(long now) {
                    if (last > 0) {
                        // Clamp dt so a long frame stall cannot fling the sweep.
                        double dt = Math.min((now - last) / 1_000_000_000.0, 0.1);
                        double h = getHeight();
                        if (h > 0) {
                            scanlineY = (scanlineY + dt * Math.max(0.0, getScanSpeed())) % (h + 40);
                            render();
                        }
                    }
                    last = now;
                }
            };
            loop.start();
        } else {
            render();
        }
    }

    private void render() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (cachedPrimary == null) {
            cachedPrimary = ThemeColors.resolve(this, ColorRole.PRIMARY);
        }
        if (beamGradient == null) {
            Color p = cachedPrimary;
            Color beamTop = Color.color(p.getRed(), p.getGreen(), p.getBlue(), 0.0);
            Color beamMid = Color.color(p.getRed(), p.getGreen(), p.getBlue(), 0.25);
            Color beamEdge = Color.color(p.getRed(), p.getGreen(), p.getBlue(), 0.6);
            beamGradient = new LinearGradient(
                    0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, beamTop), new Stop(0.8, beamMid), new Stop(1, beamEdge));
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        Color primary = cachedPrimary;
        Color bracketColor = Color.color(primary.getRed(), primary.getGreen(), primary.getBlue(), 0.7);

        // Draw 4 Sci-Fi Corner Brackets
        double blen = 16.0;
        double pad = 6.0;
        gc.setStroke(bracketColor);
        gc.setLineWidth(1.5);

        // Top-Left
        gc.strokeLine(pad, pad, pad + blen, pad);
        gc.strokeLine(pad, pad, pad, pad + blen);

        // Top-Right
        gc.strokeLine(w - pad, pad, w - pad - blen, pad);
        gc.strokeLine(w - pad, pad, w - pad, pad + blen);

        // Bottom-Left
        gc.strokeLine(pad, h - pad, pad + blen, h - pad);
        gc.strokeLine(pad, h - pad, pad, h - pad - blen);

        // Bottom-Right
        gc.strokeLine(w - pad, h - pad, w - pad - blen, h - pad);
        gc.strokeLine(w - pad, h - pad, w - pad, h - pad - blen);

        // Horizontal Rolling Scan Sweep
        if (isAnimated() && scanlineY <= h) {
            double sy = scanlineY;
            double scanH = 28.0;

            gc.setFill(beamGradient);
            gc.fillRect(0, Math.max(0, sy - scanH), w, Math.min(scanH, sy));

            // Sharp line
            gc.setStroke(Color.color(primary.getRed(), primary.getGreen(), primary.getBlue(), 0.8));
            gc.setLineWidth(1.0);
            gc.strokeLine(0, sy, w, sy);
        }
    }
}

