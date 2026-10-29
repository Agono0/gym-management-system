package io.m3fx.controls.chart;

import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Compact high-density mini sparkline chart with smooth cubic curves, gradient drop,
 * and illuminated endpoint dot. Perfect for metric cards and data tables.
 */
public class M3Sparkline extends Region {

    private final ObservableList<Double> data = FXCollections.observableArrayList();
    private final ObjectProperty<Color> strokeColor = new SimpleObjectProperty<>(this, "strokeColor", null);
    private final BooleanProperty showGlowDot = new SimpleBooleanProperty(this, "showGlowDot", true);
    private final DoubleProperty fillOpacity = new SimpleDoubleProperty(this, "fillOpacity", 0.25);
    private final DoubleProperty lineWidth = new SimpleDoubleProperty(this, "lineWidth", 2.0);

    private final Canvas canvas = new Canvas();

    /** Creates an empty sparkline. */
    public M3Sparkline() {
        this(new double[0]);
    }

    /**
     * Creates a sparkline with initial data values.
     *
     * @param values double values
     */
    public M3Sparkline(double... values) {
        getStyleClass().add("m3-sparkline");
        getChildren().add(canvas);

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        widthProperty().addListener((obs, o, n) -> render());
        heightProperty().addListener((obs, o, n) -> render());
        sceneProperty().addListener((obs, o, n) -> render());

        data.addListener((ListChangeListener<Double>) c -> render());
        strokeColor.addListener((obs, o, n) -> render());
        showGlowDot.addListener((obs, o, n) -> render());
        fillOpacity.addListener((obs, o, n) -> render());
        lineWidth.addListener((obs, o, n) -> render());

        if (values != null && values.length > 0) {
            for (double v : values) {
                data.add(v);
            }
        }

        setPrefSize(100, 32);
        setMinSize(40, 20);
    }

    /**
     * Factory creating a sparkline from values.
     *
     * @param values values
     * @return new sparkline
     */
    public static M3Sparkline of(double... values) {
        return new M3Sparkline(values);
    }

    /**
     * Factory creating a sparkline with custom color and values.
     *
     * @param color stroke color
     * @param values values
     * @return new sparkline
     */
    public static M3Sparkline of(Color color, double... values) {
        M3Sparkline s = new M3Sparkline(values);
        s.setStrokeColor(color);
        return s;
    }

    /** The observable list of values; the line renders once at least two values exist. */
    public ObservableList<Double> getData() {
        return data;
    }

    /** Stroke color, or {@code null} for theme primary. */
    public ObjectProperty<Color> strokeColorProperty() {
        return strokeColor;
    }

    public Color getStrokeColor() {
        return strokeColor.get();
    }

    public void setStrokeColor(Color color) {
        this.strokeColor.set(color);
    }

    /** Whether the glowing endpoint dot is drawn. */
    public BooleanProperty showGlowDotProperty() {
        return showGlowDot;
    }

    public boolean isShowGlowDot() {
        return showGlowDot.get();
    }

    public void setShowGlowDot(boolean show) {
        this.showGlowDot.set(show);
    }

    /** Gradient fill opacity under the curve (clamped to 0..1 at render time). */
    public DoubleProperty fillOpacityProperty() {
        return fillOpacity;
    }

    public double getFillOpacity() {
        return fillOpacity.get();
    }

    public void setFillOpacity(double opacity) {
        this.fillOpacity.set(opacity);
    }

    /** Stroke width in px (floored at 0.5 at render time). */
    public DoubleProperty lineWidthProperty() {
        return lineWidth;
    }

    public double getLineWidth() {
        return lineWidth.get();
    }

    public void setLineWidth(double width) {
        this.lineWidth.set(width);
    }

    private void render() {
        double w = getWidth() > 0 ? getWidth() : getPrefWidth();
        double h = getHeight() > 0 ? getHeight() : getPrefHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        if (data.size() < 2) {
            return;
        }

        Color color = getStrokeColor();
        if (color == null) {
            color = ThemeColors.resolve(this, ColorRole.PRIMARY);
        }

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double v : data) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        if (min == max) {
            min -= 1.0;
            max += 1.0;
        }

        double padX = 4.0;
        double padY = 4.0;
        double usableW = w - padX * 2;
        double usableH = h - padY * 2;

        int n = data.size();
        double[] px = new double[n];
        double[] py = new double[n];

        for (int i = 0; i < n; i++) {
            px[i] = padX + (i / (double) (n - 1)) * usableW;
            double norm = (data.get(i) - min) / (max - min);
            py[i] = padY + (1.0 - norm) * usableH;
        }

        // Draw area fill
        double fillAlpha = Math.max(0.0, Math.min(1.0, getFillOpacity()));
        if (fillAlpha > 0.001) {
            gc.beginPath();
            gc.moveTo(px[0], py[0]);
            drawSmoothCurve(gc, px, py);
            gc.lineTo(px[n - 1], h);
            gc.lineTo(px[0], h);
            gc.closePath();

            Color topColor = Color.color(color.getRed(), color.getGreen(), color.getBlue(), fillAlpha);
            Color btmColor = Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.0);
            LinearGradient fillGrad = new LinearGradient(
                    0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, topColor), new Stop(1, btmColor)
            );
            gc.setFill(fillGrad);
            gc.fill();
        }

        // Draw curve stroke
        gc.beginPath();
        gc.moveTo(px[0], py[0]);
        drawSmoothCurve(gc, px, py);
        gc.setStroke(color);
        gc.setLineWidth(Math.max(0.5, getLineWidth()));
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.stroke();

        // Draw glowing end dot
        if (isShowGlowDot() && n > 0) {
            double endX = px[n - 1];
            double endY = py[n - 1];

            // Outer soft glow halo
            Color halo = Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.35);
            gc.setFill(halo);
            gc.fillOval(endX - 5, endY - 5, 10, 10);

            // Inner solid core
            gc.setFill(color);
            gc.fillOval(endX - 2.5, endY - 2.5, 5, 5);
        }
    }

    /**
     * Catmull-Rom spline through the points. Control-point Y tangents are clamped to the vertical
     * band of each segment (monotone-style) so sharp spikes cannot overshoot the data range.
     */
    private static void drawSmoothCurve(GraphicsContext gc, double[] x, double[] y) {
        int n = x.length;
        if (n == 2) {
            gc.lineTo(x[1], y[1]);
            return;
        }
        for (int i = 0; i < n - 1; i++) {
            double p0x = i > 0 ? x[i - 1] : x[i];
            double p0y = i > 0 ? y[i - 1] : y[i];
            double p1x = x[i];
            double p1y = y[i];
            double p2x = x[i + 1];
            double p2y = y[i + 1];
            double p3x = i < n - 2 ? x[i + 2] : p2x;
            double p3y = i < n - 2 ? y[i + 2] : p2y;

            double cp1x = p1x + (p2x - p0x) / 6.0;
            double cp1y = p1y + (p2y - p0y) / 6.0;
            double cp2x = p2x - (p3x - p1x) / 6.0;
            double cp2y = p2y - (p3y - p1y) / 6.0;

            // Keep the curve inside the segment's vertical band: no false overshoot spikes
            double bandLo = Math.min(p1y, p2y);
            double bandHi = Math.max(p1y, p2y);
            cp1y = Math.max(bandLo, Math.min(bandHi, cp1y));
            cp2y = Math.max(bandLo, Math.min(bandHi, cp2y));

            gc.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, p2x, p2y);
        }
    }
}

