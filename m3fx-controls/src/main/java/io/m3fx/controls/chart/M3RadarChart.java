package io.m3fx.controls.chart;

import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.Arrays;

/**
 * M3 Multidimensional Radar / Spider Polar Chart for athletic biometrics, performance profiling,
 * and skill matrix visualization.
 */
public class M3RadarChart extends Region {

    private final ObservableList<M3DataSeries> seriesList = FXCollections.observableArrayList();
    private final ObservableList<String> axes = FXCollections.observableArrayList();

    private final DoubleProperty maxValue = new SimpleDoubleProperty(this, "maxValue", 100.0);
    private final IntegerProperty webRings = new SimpleIntegerProperty(this, "webRings", 5);
    private final BooleanProperty filled = new SimpleBooleanProperty(this, "filled", true);
    private final BooleanProperty showPoints = new SimpleBooleanProperty(this, "showPoints", true);
    private final BooleanProperty showLabels = new SimpleBooleanProperty(this, "showLabels", true);

    private final Canvas canvas = new Canvas();
    private final Tooltip tooltip = new Tooltip();

    private static final ColorRole[] DEFAULT_ROLES = {
            ColorRole.PRIMARY,
            ColorRole.TERTIARY,
            ColorRole.SECONDARY,
            ColorRole.ERROR
    };

    /** Creates an empty radar chart. */
    public M3RadarChart() {
        getStyleClass().add("m3-radar-chart");

        getChildren().add(canvas);
        Tooltip.install(this, tooltip);
        tooltip.getStyleClass().add("m3-tooltip"); // M3 inverse-surface tooltip styling
        tooltip.setShowDelay(javafx.util.Duration.millis(80));

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        widthProperty().addListener(e -> redraw());
        heightProperty().addListener(e -> redraw());
        maxValue.addListener(e -> redraw());
        webRings.addListener(e -> redraw());
        filled.addListener(e -> redraw());
        showPoints.addListener(e -> redraw());
        showLabels.addListener(e -> redraw());

        seriesList.addListener((ListChangeListener<M3DataSeries>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (M3DataSeries s : c.getAddedSubList()) {
                        s.getData().addListener((ListChangeListener<M3DataPoint>) ch -> redraw());
                    }
                }
            }
            redraw();
        });

        axes.addListener((ListChangeListener<String>) c -> redraw());

        sceneProperty().addListener((obs, o, n) -> redraw());

        initHoverInspection();

        setPrefSize(420, 360);
        setMinSize(220, 200);
    }

    /**
     * Creates a radar chart with predefined axis labels.
     *
     * @param axisLabels labels for polar vertices
     */
    public M3RadarChart(String... axisLabels) {
        this();
        if (axisLabels != null) {
            axes.addAll(Arrays.asList(axisLabels));
        }
    }

    private void initHoverInspection() {
        addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            double w = getWidth();
            double h = getHeight();
            int nAxes = axes.size();
            if (w <= 0 || h <= 0 || nAxes < 3) return;

            double cx = w / 2.0;
            double cy = h / 2.0;
            double radius = Math.min(cx, cy) - (isShowLabels() ? 44.0 : 20.0);
            if (radius <= 10) return;

            double mx = e.getX();
            double my = e.getY();
            double maxVal = Math.max(1.0, getMaxValue());

            M3DataPoint closestPoint = null;
            String closestSeriesName = "";
            String closestAxis = "";
            double minDistanceSq = 180.0; // hit test threshold radius ~13px

            for (int sIdx = 0; sIdx < seriesList.size(); sIdx++) {
                M3DataSeries s = seriesList.get(sIdx);
                for (int i = 0; i < Math.min(nAxes, s.getData().size()); i++) {
                    M3DataPoint pt = s.getData().get(i);
                    double ratio = Math.max(0.0, Math.min(1.0, pt.getValue() / maxVal));
                    double angle = (2 * Math.PI / nAxes) * i - Math.PI / 2.0;

                    double px = cx + (radius * ratio) * Math.cos(angle);
                    double py = cy + (radius * ratio) * Math.sin(angle);

                    double distSq = (mx - px) * (mx - px) + (my - py) * (my - py);
                    if (distSq < minDistanceSq) {
                        minDistanceSq = distSq;
                        closestPoint = pt;
                        closestSeriesName = s.getName();
                        closestAxis = (i < axes.size()) ? axes.get(i) : ("Axis " + (i + 1));
                    }
                }
            }

            if (closestPoint != null) {
                tooltip.setText(String.format("%s\n%s: %.1f", closestSeriesName, closestAxis, closestPoint.getValue()));
                if (!tooltip.isShowing()) {
                    tooltip.show(this, e.getScreenX() + 14, e.getScreenY() + 14);
                }
            } else {
                tooltip.hide();
            }
        });

        addEventHandler(MouseEvent.MOUSE_EXITED, e -> tooltip.hide());
    }

    private void redraw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        int nAxes = axes.size();
        if (nAxes < 3) {
            // Draw empty placeholder text
            gc.setFill(ThemeColors.resolve(this, ColorRole.OUTLINE));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.setFont(Font.font("Roboto", FontWeight.NORMAL, 13));
            gc.fillText("Define at least 3 axes for Radar Chart", w / 2.0, h / 2.0);
            return;
        }

        double cx = w / 2.0;
        double cy = h / 2.0;
        double labelPadding = isShowLabels() ? 44.0 : 18.0;
        double radius = Math.min(cx, cy) - labelPadding;
        if (radius <= 10) return;

        Color webColor = ThemeColors.resolve(this, ColorRole.OUTLINE_VARIANT);
        Color onSurfaceVariant = ThemeColors.resolve(this, ColorRole.ON_SURFACE_VARIANT);
        double angleStep = (2 * Math.PI) / nAxes;

        // 1. Draw concentric polygon web rings
        int rings = Math.max(1, getWebRings());
        gc.setLineWidth(1.0);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        for (int r = 1; r <= rings; r++) {
            double ringRadius = radius * (r / (double) rings);
            gc.setStroke(Color.color(webColor.getRed(), webColor.getGreen(), webColor.getBlue(), 0.35));

            double[] xPoints = new double[nAxes];
            double[] yPoints = new double[nAxes];
            for (int i = 0; i < nAxes; i++) {
                double angle = angleStep * i - Math.PI / 2.0;
                xPoints[i] = cx + ringRadius * Math.cos(angle);
                yPoints[i] = cy + ringRadius * Math.sin(angle);
            }
            gc.strokePolygon(xPoints, yPoints, nAxes);
        }

        // 2. Draw radial axis spokes
        for (int i = 0; i < nAxes; i++) {
            double angle = angleStep * i - Math.PI / 2.0;
            double endX = cx + radius * Math.cos(angle);
            double endY = cy + radius * Math.sin(angle);

            gc.setStroke(Color.color(webColor.getRed(), webColor.getGreen(), webColor.getBlue(), 0.45));
            gc.strokeLine(cx, cy, endX, endY);

            // Draw axis text label
            if (isShowLabels() && i < axes.size()) {
                double labelDist = radius + 18.0;
                double lx = cx + labelDist * Math.cos(angle);
                double ly = cy + labelDist * Math.sin(angle);

                gc.setFill(onSurfaceVariant);
                gc.setFont(Font.font("Roboto", FontWeight.BOLD, 11.5));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setTextBaseline(VPos.CENTER);
                gc.fillText(axes.get(i), lx, ly);
            }
        }

        // 3. Draw Series Polygons
        double maxVal = Math.max(1.0, getMaxValue());

        for (int sIdx = 0; sIdx < seriesList.size(); sIdx++) {
            M3DataSeries series = seriesList.get(sIdx);
            Color seriesColor = series.getColor();
            if (seriesColor == null) {
                ColorRole role = DEFAULT_ROLES[sIdx % DEFAULT_ROLES.length];
                seriesColor = ThemeColors.resolve(this, role);
            }

            int count = Math.min(nAxes, series.getData().size());
            if (count < 3) continue;

            double[] px = new double[count];
            double[] py = new double[count];

            for (int i = 0; i < count; i++) {
                double val = series.getData().get(i).getValue();
                double ratio = Math.max(0.0, Math.min(1.0, val / maxVal));
                double angle = angleStep * i - Math.PI / 2.0;

                px[i] = cx + (radius * ratio) * Math.cos(angle);
                py[i] = cy + (radius * ratio) * Math.sin(angle);
            }

            // Polygon fill
            if (isFilled()) {
                Color fillColor = Color.color(
                        seriesColor.getRed(),
                        seriesColor.getGreen(),
                        seriesColor.getBlue(),
                        0.25
                );
                gc.setFill(fillColor);
                gc.fillPolygon(px, py, count);
            }

            // Polygon stroke
            gc.setStroke(seriesColor);
            gc.setLineWidth(2.4);
            gc.strokePolygon(px, py, count);

            // Vertex dots
            if (isShowPoints()) {
                double dotR = 4.0;
                Color dotFill = ThemeColors.resolve(this, ColorRole.SURFACE);
                for (int i = 0; i < count; i++) {
                    gc.setFill(dotFill);
                    gc.fillOval(px[i] - dotR, py[i] - dotR, dotR * 2, dotR * 2);
                    gc.setStroke(seriesColor);
                    gc.setLineWidth(2.0);
                    gc.strokeOval(px[i] - dotR, py[i] - dotR, dotR * 2, dotR * 2);
                }
            }
        }
    }

    /** The data model: one series per polygon. */
    public ObservableList<M3DataSeries> getSeriesList() {
        return seriesList;
    }

    /** Axis labels; the polygon needs at least three axes to render. */
    public ObservableList<String> getAxes() {
        return axes;
    }

    /** The value mapped to the outermost web ring. */
    public DoubleProperty maxValueProperty() {
        return maxValue;
    }

    public double getMaxValue() {
        return maxValue.get();
    }

    public void setMaxValue(double maxValue) {
        this.maxValue.set(maxValue);
    }

    /** Number of concentric web rings drawn behind the data. */
    public IntegerProperty webRingsProperty() {
        return webRings;
    }

    public int getWebRings() {
        return webRings.get();
    }

    public void setWebRings(int webRings) {
        this.webRings.set(webRings);
    }

    /** Whether series polygons are filled with a translucent tint of the series color. */
    public BooleanProperty filledProperty() {
        return filled;
    }

    public boolean isFilled() {
        return filled.get();
    }

    public void setFilled(boolean filled) {
        this.filled.set(filled);
    }

    /** Whether ringed vertex dots are drawn at each data point. */
    public BooleanProperty showPointsProperty() {
        return showPoints;
    }

    public boolean isShowPoints() {
        return showPoints.get();
    }

    public void setShowPoints(boolean showPoints) {
        this.showPoints.set(showPoints);
    }

    /** Whether axis labels are drawn outside the web. */
    public BooleanProperty showLabelsProperty() {
        return showLabels;
    }

    public boolean isShowLabels() {
        return showLabels.get();
    }

    public void setShowLabels(boolean showLabels) {
        this.showLabels.set(showLabels);
    }
}

