package io.m3fx.controls.chart;

import io.m3fx.controls.internal.SpringAnimation;
import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.property.*;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * M3 Radial Gauge / Speedometer Chart for real-time telemetry, athletic biometrics,
 * heart rate monitoring, and output metrics with spring-interpolated animations.
 */
public class M3GaugeChart extends Region {

    private final DoubleProperty value = new SimpleDoubleProperty(this, "value", 0.0);
    private final DoubleProperty animatedValue = new SimpleDoubleProperty(this, "animatedValue", 0.0);
    private final DoubleProperty minValue = new SimpleDoubleProperty(this, "minValue", 0.0);
    private final DoubleProperty maxValue = new SimpleDoubleProperty(this, "maxValue", 100.0);

    private final StringProperty title = new SimpleStringProperty(this, "title", "METRIC");
    private final StringProperty unit = new SimpleStringProperty(this, "unit", "%");

    private final DoubleProperty startAngle = new SimpleDoubleProperty(this, "startAngle", 135.0);
    private final DoubleProperty sweepAngle = new SimpleDoubleProperty(this, "sweepAngle", 270.0);

    private final ObjectProperty<Color> trackColor = new SimpleObjectProperty<>(this, "trackColor", null);
    private final ObjectProperty<Color> progressColor = new SimpleObjectProperty<>(this, "progressColor", null);

    private final Canvas canvas = new Canvas();

    /** Creates a default percentage gauge (0 to 100%). */
    public M3GaugeChart() {
        this("METRIC", 0.0, 100.0, "%");
    }

    /**
     * Creates a customized radial gauge.
     *
     * @param title gauge metric title
     * @param min minimum value
     * @param max maximum value
     * @param unit value unit string
     */
    public M3GaugeChart(String title, double min, double max, String unit) {
        getStyleClass().add("m3-gauge-chart");

        setTitle(title);
        setMinValue(min);
        setMaxValue(max);
        setUnit(unit);

        getChildren().add(canvas);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        widthProperty().addListener(e -> redraw());
        heightProperty().addListener(e -> redraw());
        minValue.addListener(e -> redraw());
        maxValue.addListener(e -> redraw());
        this.title.addListener(e -> redraw());
        this.unit.addListener(e -> redraw());
        startAngle.addListener(e -> redraw());
        sweepAngle.addListener(e -> redraw());

        value.addListener((obs, o, n) -> {
            SpringAnimation.animate(animatedValue, n.doubleValue(), SpringAnimation.GENTLE, null);
        });

        animatedValue.addListener(e -> redraw());

        sceneProperty().addListener((obs, o, n) -> redraw());

        setPrefSize(240, 240);
        setMinSize(160, 160);
    }

    private void redraw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        double cx = w / 2.0;
        double cy = h / 2.0;
        double radius = Math.min(cx, cy) - 24.0;
        if (radius <= 10) return;

        double min = getMinValue();
        double max = Math.max(min + 0.001, getMaxValue());
        double curVal = getAnimatedValue();
        double norm = Math.max(0.0, Math.min(1.0, (curVal - min) / (max - min)));

        double sAngle = getStartAngle();
        double swAngle = getSweepAngle();

        double trackWidth = Math.max(8.0, radius * 0.14);

        // 1. Draw Background Track Arc
        Color bgTrack = getTrackColor();
        if (bgTrack == null) {
            Color outline = ThemeColors.resolve(this, ColorRole.OUTLINE_VARIANT);
            bgTrack = Color.color(outline.getRed(), outline.getGreen(), outline.getBlue(), 0.35);
        }

        gc.setLineWidth(trackWidth);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setStroke(bgTrack);

        double arcX = cx - radius;
        double arcY = cy - radius;
        double arcDiam = radius * 2.0;

        // Note: JavaFX canvas arc angles: 0 is 3 o'clock, counter-clockwise positive
        gc.strokeArc(arcX, arcY, arcDiam, arcDiam, sAngle, -swAngle, ArcType.OPEN);

        // 2. Draw Progress Arc
        if (norm > 0.001) {
            Color progColor = getProgressColor();
            Color beadColor;
            if (progColor == null) {
                // Dynamic threshold coloring: Primary -> Tertiary -> Error
                if (norm < 0.65) {
                    progColor = ThemeColors.resolve(this, ColorRole.PRIMARY);
                    beadColor = ThemeColors.resolve(this, ColorRole.ON_PRIMARY);
                } else if (norm < 0.85) {
                    progColor = ThemeColors.resolve(this, ColorRole.TERTIARY);
                    beadColor = ThemeColors.resolve(this, ColorRole.ON_TERTIARY);
                } else {
                    progColor = ThemeColors.resolve(this, ColorRole.ERROR);
                    beadColor = ThemeColors.resolve(this, ColorRole.ON_ERROR);
                }
            } else {
                // Custom accent: use the surface color so the bead stays legible in both modes
                beadColor = ThemeColors.resolve(this, ColorRole.SURFACE);
            }

            gc.setStroke(progColor);
            gc.setLineWidth(trackWidth);
            gc.strokeArc(arcX, arcY, arcDiam, arcDiam, sAngle, -swAngle * norm, ArcType.OPEN);

            // Small indicator bead at the tip
            double currentAngleDeg = sAngle - (swAngle * norm);
            double currentAngleRad = Math.toRadians(currentAngleDeg);
            double tipX = cx + radius * Math.cos(currentAngleRad);
            double tipY = cy - radius * Math.sin(currentAngleRad);

            gc.setFill(beadColor);
            gc.fillOval(tipX - trackWidth * 0.45, tipY - trackWidth * 0.45, trackWidth * 0.9, trackWidth * 0.9);
        }

        // 3. Draw Center Numerical Value & Unit
        Color onSurface = ThemeColors.resolve(this, ColorRole.ON_SURFACE);
        Color onSurfaceVar = ThemeColors.resolve(this, ColorRole.ON_SURFACE_VARIANT);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        // Value
        gc.setFill(onSurface);
        gc.setFont(Font.font("Roboto", FontWeight.BOLD, Math.max(16.0, radius * 0.34)));
        String valText = (Math.abs(curVal - Math.round(curVal)) < 0.05)
                ? String.format("%.0f", curVal)
                : String.format("%.1f", curVal);
        gc.fillText(valText, cx, cy - 6.0);

        // Unit
        gc.setFill(onSurfaceVar);
        gc.setFont(Font.font("Roboto", FontWeight.NORMAL, Math.max(10.0, radius * 0.16)));
        gc.fillText(getUnit(), cx, cy + (radius * 0.22));

        // Title at bottom
        gc.setFill(onSurfaceVar);
        gc.setFont(Font.font("Roboto", FontWeight.BOLD, Math.max(9.0, radius * 0.13)));
        gc.fillText(getTitle().toUpperCase(), cx, cy + (radius * 0.58));
    }

    /** The raw gauge value; changes animate the arc with the M3 gentle spring. */
    public DoubleProperty valueProperty() {
        return value;
    }

    public double getValue() {
        return value.get();
    }

    public void setValue(double value) {
        this.value.set(value);
    }

    /** The spring-animated value currently driving the rendered arc. */
    public DoubleProperty animatedValueProperty() {
        return animatedValue;
    }

    public double getAnimatedValue() {
        return animatedValue.get();
    }

    /** The lower bound of the gauge scale. */
    public DoubleProperty minValueProperty() {
        return minValue;
    }

    public double getMinValue() {
        return minValue.get();
    }

    public void setMinValue(double minValue) {
        this.minValue.set(minValue);
    }

    /** The upper bound of the gauge scale. */
    public DoubleProperty maxValueProperty() {
        return maxValue;
    }

    public double getMaxValue() {
        return maxValue.get();
    }

    public void setMaxValue(double maxValue) {
        this.maxValue.set(maxValue);
    }

    /** The metric title rendered under the value. */
    public StringProperty titleProperty() {
        return title;
    }

    public String getTitle() {
        return title.get();
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    /** The unit suffix rendered under the value. */
    public StringProperty unitProperty() {
        return unit;
    }

    public String getUnit() {
        return unit.get();
    }

    public void setUnit(String unit) {
        this.unit.set(unit);
    }

    /** Start angle of the track in JavaFX arc degrees (0 = 3 o'clock, positive = counter-clockwise). */
    public DoubleProperty startAngleProperty() {
        return startAngle;
    }

    public double getStartAngle() {
        return startAngle.get();
    }

    public void setStartAngle(double startAngle) {
        this.startAngle.set(startAngle);
    }

    /** Clockwise angular extent of the track in degrees. */
    public DoubleProperty sweepAngleProperty() {
        return sweepAngle;
    }

    public double getSweepAngle() {
        return sweepAngle.get();
    }

    public void setSweepAngle(double sweepAngle) {
        this.sweepAngle.set(sweepAngle);
    }

    /** Custom track color, or {@code null} for a dimmed outline-variant track. */
    public ObjectProperty<Color> trackColorProperty() {
        return trackColor;
    }

    public Color getTrackColor() {
        return trackColor.get();
    }

    public void setTrackColor(Color trackColor) {
        this.trackColor.set(trackColor);
    }

    /**
     * Custom progress color, or {@code null} for the dynamic threshold ramp
     * (primary &rarr; tertiary &rarr; error as the value approaches the maximum).
     */
    public ObjectProperty<Color> progressColorProperty() {
        return progressColor;
    }

    public Color getProgressColor() {
        return progressColor.get();
    }

    public void setProgressColor(Color progressColor) {
        this.progressColor.set(progressColor);
    }
}

