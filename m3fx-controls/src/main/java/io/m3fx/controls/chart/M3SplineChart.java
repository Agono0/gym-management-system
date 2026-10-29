package io.m3fx.controls.chart;

import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.util.ArrayList;
import java.util.List;

/**
 * M3 Spline Chart rendering smooth Catmull-Rom cubic curves, gradient drop fills,
 * crosshair cursor tracking, and floating interactive tooltips.
 */
public class M3SplineChart extends Region {

    private final ObservableList<M3DataSeries> seriesList = FXCollections.observableArrayList();
    private final BooleanProperty areaFill = new SimpleBooleanProperty(this, "areaFill", true);
    private final DoubleProperty fillOpacity = new SimpleDoubleProperty(this, "fillOpacity", 0.20);
    private final BooleanProperty showPoints = new SimpleBooleanProperty(this, "showPoints", true);
    private final BooleanProperty showGrid = new SimpleBooleanProperty(this, "showGrid", true);
    private final BooleanProperty showCrosshair = new SimpleBooleanProperty(this, "showCrosshair", true);

    private final Canvas canvas = new Canvas();

    private double hoverMouseX = -1;
    private double hoverMouseY = -1;
    private boolean isHovering = false;

    /** Creates an empty spline chart. */
    public M3SplineChart() {
        getStyleClass().add("m3-spline-chart");
        getChildren().add(canvas);

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        widthProperty().addListener((obs, o, n) -> render());
        heightProperty().addListener((obs, o, n) -> render());
        sceneProperty().addListener((obs, o, n) -> render());

        seriesList.addListener((ListChangeListener<M3DataSeries>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (M3DataSeries s : c.getAddedSubList()) {
                        s.getData().addListener((ListChangeListener<M3DataPoint>) c2 -> render());
                    }
                }
            }
            render();
        });

        areaFill.addListener((obs, o, n) -> render());
        fillOpacity.addListener((obs, o, n) -> render());
        showPoints.addListener((obs, o, n) -> render());
        showGrid.addListener((obs, o, n) -> render());
        showCrosshair.addListener((obs, o, n) -> render());

        initInteractivity();

        setPrefSize(500, 260);
        setMinSize(200, 140);
    }

    private void initInteractivity() {
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            hoverMouseX = e.getX();
            hoverMouseY = e.getY();
            isHovering = true;
            render();
        });

        canvas.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            isHovering = false;
            hoverMouseX = -1;
            hoverMouseY = -1;
            render();
        });
    }

    /** The data model: one entry per series; the X scale is the shared point index. */
    public ObservableList<M3DataSeries> getSeriesList() {
        return seriesList;
    }

    /** Whether the gradient area under each curve is filled. */
    public BooleanProperty areaFillProperty() {
        return areaFill;
    }

    public boolean isAreaFill() {
        return areaFill.get();
    }

    public void setAreaFill(boolean areaFill) {
        this.areaFill.set(areaFill);
    }

    /** Gradient fill opacity under the curves (clamped to 0..1 when used). */
    public DoubleProperty fillOpacityProperty() {
        return fillOpacity;
    }

    public double getFillOpacity() {
        return fillOpacity.get();
    }

    public void setFillOpacity(double fillOpacity) {
        this.fillOpacity.set(fillOpacity);
    }

    /** Whether vertex dots are drawn at every data point. */
    public BooleanProperty showPointsProperty() {
        return showPoints;
    }

    public boolean isShowPoints() {
        return showPoints.get();
    }

    public void setShowPoints(boolean showPoints) {
        this.showPoints.set(showPoints);
    }

    /** Whether dashed gridlines and Y-axis labels are drawn. */
    public BooleanProperty showGridProperty() {
        return showGrid;
    }

    public boolean isShowGrid() {
        return showGrid.get();
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid.set(showGrid);
    }

    /** Whether the vertical crosshair, node highlights and hover tooltip track the cursor. */
    public BooleanProperty showCrosshairProperty() {
        return showCrosshair;
    }

    public boolean isShowCrosshair() {
        return showCrosshair.get();
    }

    public void setShowCrosshair(boolean showCrosshair) {
        this.showCrosshair.set(showCrosshair);
    }

    private Color resolveSeriesColor(int index, M3DataSeries series) {
        if (series.getColor() != null) {
            return series.getColor();
        }
        return switch (index % 4) {
            case 0 -> ThemeColors.resolve(this, ColorRole.PRIMARY);
            case 1 -> ThemeColors.resolve(this, ColorRole.TERTIARY);
            case 2 -> ThemeColors.resolve(this, ColorRole.SECONDARY);
            default -> ThemeColors.resolve(this, ColorRole.ERROR);
        };
    }

    private void render() {
        double w = getWidth() > 0 ? getWidth() : getPrefWidth();
        double h = getHeight() > 0 ? getHeight() : getPrefHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        double padLeft = 44.0;
        double padRight = 24.0;
        double padTop = 20.0;
        double padBottom = 32.0;

        double plotW = w - padLeft - padRight;
        double plotH = h - padTop - padBottom;
        if (plotW <= 0 || plotH <= 0) {
            return;
        }

        double minVal = Double.MAX_VALUE;
        double maxVal = -Double.MAX_VALUE;
        int maxPoints = 0;

        for (M3DataSeries series : seriesList) {
            if (series.getData().size() > maxPoints) {
                maxPoints = series.getData().size();
            }
            for (M3DataPoint pt : series.getData()) {
                if (pt.getValue() < minVal) minVal = pt.getValue();
                if (pt.getValue() > maxVal) maxVal = pt.getValue();
            }
        }

        if (maxPoints < 2 || minVal == Double.MAX_VALUE) {
            return;
        }

        if (minVal == maxVal) {
            // Flat data: open a 10-unit window on the correct side of zero
            if (minVal > 0) {
                minVal = 0;
                maxVal += 10;
            } else if (minVal < 0) {
                maxVal = 0;
                minVal -= 10;
            } else {
                minVal = 0;
                maxVal = 10;
            }
        } else {
            double range = maxVal - minVal;
            if (minVal >= 0) {
                minVal = 0;                    // keep the zero baseline for all-positive data
            } else {
                minVal -= range * 0.05;        // negative data: pad below the lowest point
            }
            maxVal = maxVal + range * 0.1;
        }

        Color gridColor = ThemeColors.resolve(this, ColorRole.OUTLINE_VARIANT);
        Color textColor = ThemeColors.resolve(this, ColorRole.ON_SURFACE_VARIANT);

        // Draw horizontal grid lines & Y labels
        if (isShowGrid()) {
            gc.setStroke(Color.color(gridColor.getRed(), gridColor.getGreen(), gridColor.getBlue(), 0.3));
            gc.setLineWidth(1.0);
            gc.setLineDashes(4.0, 4.0);

            gc.setFill(textColor);
            gc.setFont(javafx.scene.text.Font.font(11));

            int ySteps = 4;
            for (int i = 0; i <= ySteps; i++) {
                double frac = i / (double) ySteps;
                double val = minVal + (maxVal - minVal) * (1.0 - frac);
                double y = padTop + frac * plotH;

                gc.strokeLine(padLeft, y, padLeft + plotW, y);
                String valStr = String.format("%.0f", val);
                gc.fillText(valStr, 6, y + 4);
            }
            gc.setLineDashes(null);
        }

        // Render each series
        int nearestIdx = -1;
        double nearestDist = Double.MAX_VALUE;

        for (int sIdx = 0; sIdx < seriesList.size(); sIdx++) {
            M3DataSeries series = seriesList.get(sIdx);
            int n = series.getData().size();
            if (n < 2) continue;

            Color seriesColor = resolveSeriesColor(sIdx, series);
            double[] px = new double[n];
            double[] py = new double[n];

            for (int i = 0; i < n; i++) {
                px[i] = padLeft + (i / (double) (n - 1)) * plotW;
                double norm = (series.getData().get(i).getValue() - minVal) / (maxVal - minVal);
                py[i] = padTop + (1.0 - norm) * plotH;

                // Track nearest index to mouse X
                if (isHovering) {
                    double dist = Math.abs(hoverMouseX - px[i]);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearestIdx = i;
                    }
                }
            }

            // Draw Area Fill
            if (isAreaFill() && getFillOpacity() > 0.001) {
                gc.beginPath();
                gc.moveTo(px[0], py[0]);
                drawSmoothCurve(gc, px, py);
                gc.lineTo(px[n - 1], padTop + plotH);
                gc.lineTo(px[0], padTop + plotH);
                gc.closePath();

                Color topColor = Color.color(seriesColor.getRed(), seriesColor.getGreen(), seriesColor.getBlue(), getFillOpacity());
                Color btmColor = Color.color(seriesColor.getRed(), seriesColor.getGreen(), seriesColor.getBlue(), 0.0);
                LinearGradient grad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, topColor), new Stop(1, btmColor));
                gc.setFill(grad);
                gc.fill();
            }

            // Draw Stroke Curve
            gc.beginPath();
            gc.moveTo(px[0], py[0]);
            drawSmoothCurve(gc, px, py);
            gc.setStroke(seriesColor);
            gc.setLineWidth(2.5);
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.setLineJoin(StrokeLineJoin.ROUND);
            gc.stroke();

            // Draw Point Dots
            if (isShowPoints()) {
                gc.setFill(ThemeColors.resolve(this, ColorRole.SURFACE));
                gc.setStroke(seriesColor);
                gc.setLineWidth(2.0);
                for (int i = 0; i < n; i++) {
                    gc.fillOval(px[i] - 3.5, py[i] - 3.5, 7, 7);
                    gc.strokeOval(px[i] - 3.5, py[i] - 3.5, 7, 7);
                }
            }
        }

        // Draw X Category Labels
        if (!seriesList.isEmpty()) {
            M3DataSeries firstSeries = seriesList.get(0);
            int n = firstSeries.getData().size();
            gc.setFill(textColor);
            gc.setFont(javafx.scene.text.Font.font(11));

            int step = Math.max(1, n / 7);
            for (int i = 0; i < n; i += step) {
                double x = padLeft + (i / (double) (n - 1)) * plotW;
                String lbl = firstSeries.getData().get(i).getLabel();
                gc.fillText(lbl, x - 10, padTop + plotH + 18);
            }
        }

        // Crosshair & Tooltip Hover
        if (isShowCrosshair() && isHovering && nearestIdx >= 0 && hoverMouseX >= padLeft - 20 && hoverMouseX <= padLeft + plotW + 20) {
            M3DataSeries firstSeries = seriesList.get(0);
            if (nearestIdx < firstSeries.getData().size()) {
                double targetX = padLeft + (nearestIdx / (double) (firstSeries.getData().size() - 1)) * plotW;

                // Vertical Crosshair Line
                gc.setStroke(Color.color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), 0.5));
                gc.setLineWidth(1.0);
                gc.setLineDashes(3.0, 3.0);
                gc.strokeLine(targetX, padTop, targetX, padTop + plotH);
                gc.setLineDashes(null);

                // Highlight Nodes on target line
                List<String> tooltipLines = new ArrayList<>();
                tooltipLines.add(firstSeries.getData().get(nearestIdx).getLabel());

                for (int sIdx = 0; sIdx < seriesList.size(); sIdx++) {
                    M3DataSeries series = seriesList.get(sIdx);
                    if (nearestIdx < series.getData().size()) {
                        M3DataPoint pt = series.getData().get(nearestIdx);
                        double norm = (pt.getValue() - minVal) / (maxVal - minVal);
                        double py = padTop + (1.0 - norm) * plotH;
                        Color sColor = resolveSeriesColor(sIdx, series);

                        // Highlight halo
                        gc.setFill(Color.color(sColor.getRed(), sColor.getGreen(), sColor.getBlue(), 0.3));
                        gc.fillOval(targetX - 7, py - 7, 14, 14);
                        gc.setFill(sColor);
                        gc.fillOval(targetX - 4, py - 4, 8, 8);

                        tooltipLines.add(series.getName() + ": " + String.format("%.1f", pt.getValue()));
                    }
                }

                // Draw Tooltip Box
                drawTooltip(gc, targetX, hoverMouseY, tooltipLines);
            }
        }
    }

    private void drawTooltip(GraphicsContext gc, double targetX, double targetY, List<String> lines) {
        double lineH = 16.0;
        double pad = 8.0;
        double tipW = 120.0;
        double tipH = pad * 2 + lines.size() * lineH;

        double tipX = targetX + 12;
        if (tipX + tipW > getWidth() - 10) {
            tipX = Math.max(10, targetX - tipW - 12);
        }
        double tipY = Math.max(10, Math.min(getHeight() - tipH - 10, targetY - tipH / 2.0));

        // M3 plain tooltip: inverse surface container, inverse on-surface content, 4dp radius
        Color bg = ThemeColors.resolve(this, ColorRole.INVERSE_SURFACE);
        Color text = ThemeColors.resolve(this, ColorRole.INVERSE_ON_SURFACE);

        gc.setFill(bg);
        gc.fillRoundRect(tipX, tipY, tipW, tipH, 4, 4);

        // Header Line (bold)
        gc.setFill(text);
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 11));
        if (!lines.isEmpty()) {
            gc.fillText(lines.get(0), tipX + pad, tipY + pad + 11);
        }

        // Body Lines
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.NORMAL, 10));
        for (int i = 1; i < lines.size(); i++) {
            gc.fillText(lines.get(i), tipX + pad, tipY + pad + 11 + i * lineH);
        }
    }

    /**
     * Catmull-Rom spline through the points. Control-point Y tangents are clamped to the vertical
     * band of each segment (monotone-style) so sharp peaks cannot overshoot the data range.
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

