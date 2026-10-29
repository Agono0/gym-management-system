package io.m3fx.controls.chart;

import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

/**
 * M3 Scatter and Bubble Plot for Load-Velocity Profiling, VBT Analytics, and Biometric Distributions.
 * Features auto-scaling, dashed gridlines, 4-quadrant crosshairs, OLS regression trendlines,
 * and interactive floating tooltips.
 */
public class M3ScatterPlot extends Region {

    private final ObservableList<M3ScatterSeries> seriesList = FXCollections.observableArrayList();

    private final BooleanProperty showGrid = new SimpleBooleanProperty(this, "showGrid", true);
    private final BooleanProperty showQuadrants = new SimpleBooleanProperty(this, "showQuadrants", true);
    private final DoubleProperty quadrantX = new SimpleDoubleProperty(this, "quadrantX", Double.NaN);
    private final DoubleProperty quadrantY = new SimpleDoubleProperty(this, "quadrantY", Double.NaN);
    private final BooleanProperty showTrendline = new SimpleBooleanProperty(this, "showTrendline", true);
    private final BooleanProperty showTrendlineStats = new SimpleBooleanProperty(this, "showTrendlineStats", true);

    private final StringProperty xAxisTitle = new SimpleStringProperty(this, "xAxisTitle", "% 1RM (Load)");
    private final StringProperty yAxisTitle = new SimpleStringProperty(this, "yAxisTitle", "Velocity (m/s)");
    private final StringProperty xUnit = new SimpleStringProperty(this, "xUnit", "%");
    private final StringProperty yUnit = new SimpleStringProperty(this, "yUnit", "m/s");

    private final Canvas canvas = new Canvas();

    private double hoverMouseX = -1;
    private double hoverMouseY = -1;
    private boolean isHovering = false;

    public M3ScatterPlot() {
        getStyleClass().add("m3-scatter-plot");
        getChildren().add(canvas);

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        widthProperty().addListener((obs, o, n) -> render());
        heightProperty().addListener((obs, o, n) -> render());
        sceneProperty().addListener((obs, o, n) -> render());

        seriesList.addListener((ListChangeListener<M3ScatterSeries>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (M3ScatterSeries s : c.getAddedSubList()) {
                        s.getData().addListener((ListChangeListener<M3ScatterPoint>) c2 -> render());
                    }
                }
            }
            render();
        });

        showGrid.addListener((obs, o, n) -> render());
        showQuadrants.addListener((obs, o, n) -> render());
        quadrantX.addListener((obs, o, n) -> render());
        quadrantY.addListener((obs, o, n) -> render());
        showTrendline.addListener((obs, o, n) -> render());
        showTrendlineStats.addListener((obs, o, n) -> render());
        xAxisTitle.addListener((obs, o, n) -> render());
        yAxisTitle.addListener((obs, o, n) -> render());

        initInteractivity();

        setPrefSize(540, 320);
        setMinSize(260, 180);
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

    /** The data model: one entry per series of scatter/bubble points. */
    public ObservableList<M3ScatterSeries> getSeriesList() {
        return seriesList;
    }

    /** Whether dashed gridlines and axis tick labels are drawn. */
    public BooleanProperty showGridProperty() {
        return showGrid;
    }

    public boolean isShowGrid() {
        return showGrid.get();
    }

    public void setShowGrid(boolean show) {
        this.showGrid.set(show);
    }

    /** Whether the dashed quadrant crosshair lines and quadrant captions are drawn. */
    public BooleanProperty showQuadrantsProperty() {
        return showQuadrants;
    }

    public boolean isShowQuadrants() {
        return showQuadrants.get();
    }

    public void setShowQuadrants(boolean show) {
        this.showQuadrants.set(show);
    }

    /** Vertical crosshair position in data units, or {@link Double#NaN} for the plot midpoint. */
    public DoubleProperty quadrantXProperty() {
        return quadrantX;
    }

    public double getQuadrantX() {
        return quadrantX.get();
    }

    public void setQuadrantX(double qx) {
        this.quadrantX.set(qx);
    }

    /** Horizontal crosshair position in data units, or {@link Double#NaN} for the plot midpoint. */
    public DoubleProperty quadrantYProperty() {
        return quadrantY;
    }

    public double getQuadrantY() {
        return quadrantY.get();
    }

    public void setQuadrantY(double qy) {
        this.quadrantY.set(qy);
    }

    /** Whether per-series OLS regression trendlines are drawn. */
    public BooleanProperty showTrendlineProperty() {
        return showTrendline;
    }

    public boolean isShowTrendline() {
        return showTrendline.get();
    }

    public void setShowTrendline(boolean show) {
        this.showTrendline.set(show);
    }

    /** Whether the regression equation/R² badge is drawn in the plot corner. */
    public BooleanProperty showTrendlineStatsProperty() {
        return showTrendlineStats;
    }

    public boolean isShowTrendlineStats() {
        return showTrendlineStats.get();
    }

    public void setShowTrendlineStats(boolean show) {
        this.showTrendlineStats.set(show);
    }

    /** Alias for {@link #setShowTrendline(boolean)}. */
    public void setShowRegression(boolean show) {
        setShowTrendline(show);
    }

    /** Alias for {@link #isShowTrendline()}. */
    public boolean isShowRegression() {
        return isShowTrendline();
    }

    /** Alias for {@link #showTrendlineProperty()}. */
    public BooleanProperty showRegressionProperty() {
        return showTrendlineProperty();
    }

    /** Alias for {@link #setShowQuadrants(boolean)}. */
    public void setCrosshairEnabled(boolean enabled) {
        setShowQuadrants(enabled);
    }

    /** Alias for {@link #isShowQuadrants()}. */
    public boolean isCrosshairEnabled() {
        return isShowQuadrants();
    }

    /** Alias for {@link #showQuadrantsProperty()}. */
    public BooleanProperty crosshairEnabledProperty() {
        return showQuadrantsProperty();
    }

    /** Label rendered under the X axis. */
    public StringProperty xAxisTitleProperty() {
        return xAxisTitle;
    }

    public String getXAxisTitle() {
        return xAxisTitle.get();
    }

    public void setXAxisTitle(String title) {
        this.xAxisTitle.set(title);
    }

    /** Label rendered (rotated) beside the Y axis. */
    public StringProperty yAxisTitleProperty() {
        return yAxisTitle;
    }

    public String getYAxisTitle() {
        return yAxisTitle.get();
    }

    public void setYAxisTitle(String title) {
        this.yAxisTitle.set(title);
    }

    /** Unit suffix appended to X values in tooltips and tick labels. */
    public StringProperty xUnitProperty() {
        return xUnit;
    }

    public String getXUnit() {
        return xUnit.get();
    }

    public void setXUnit(String unit) {
        this.xUnit.set(unit);
    }

    /** Unit suffix appended to Y values in tooltips and tick labels. */
    public StringProperty yUnitProperty() {
        return yUnit;
    }

    public String getYUnit() {
        return yUnit.get();
    }

    public void setYUnit(String unit) {
        this.yUnit.set(unit);
    }

    private Color resolveSeriesColor(int index, M3ScatterSeries series) {
        if (series.getColor() != null) return series.getColor();
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
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        double padLeft = 56.0;
        double padRight = 32.0;
        double padTop = 28.0;
        double padBottom = 44.0;

        double plotW = w - padLeft - padRight;
        double plotH = h - padTop - padBottom;
        if (plotW <= 0 || plotH <= 0 || seriesList.isEmpty()) return;

        // Compute global data bounds
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        int totalPoints = 0;

        for (M3ScatterSeries series : seriesList) {
            for (M3ScatterPoint pt : series.getData()) {
                if (pt.getX() < minX) minX = pt.getX();
                if (pt.getX() > maxX) maxX = pt.getX();
                if (pt.getY() < minY) minY = pt.getY();
                if (pt.getY() > maxY) maxY = pt.getY();
                totalPoints++;
            }
        }

        if (totalPoints == 0) return;

        double spanX = maxX - minX;
        double spanY = maxY - minY;
        if (spanX <= 1e-6) { minX -= 5; maxX += 5; spanX = 10; }
        else { minX -= spanX * 0.08; maxX += spanX * 0.08; spanX = maxX - minX; }

        if (spanY <= 1e-6) { minY -= 5; maxY += 5; spanY = 10; }
        else { minY -= spanY * 0.08; maxY += spanY * 0.08; spanY = maxY - minY; }

        Color gridColor = ThemeColors.resolve(this, ColorRole.OUTLINE_VARIANT);
        Color textColor = ThemeColors.resolve(this, ColorRole.ON_SURFACE_VARIANT);

        // Dashed grid lines
        if (isShowGrid()) {
            gc.setStroke(Color.color(gridColor.getRed(), gridColor.getGreen(), gridColor.getBlue(), 0.35));
            gc.setLineWidth(1.0);
            gc.setLineDashes(4.0, 4.0);
            gc.setFill(textColor);
            gc.setFont(Font.font(10));

            int ySteps = 5;
            for (int i = 0; i <= ySteps; i++) {
                double frac = i / (double) ySteps;
                double val = minY + spanY * (1.0 - frac);
                double y = padTop + frac * plotH;
                gc.strokeLine(padLeft, y, padLeft + plotW, y);
                gc.fillText(String.format("%.2f", val), 10, y + 4);
            }

            int xSteps = 5;
            for (int i = 0; i <= xSteps; i++) {
                double frac = i / (double) xSteps;
                double val = minX + spanX * frac;
                double x = padLeft + frac * plotW;
                gc.strokeLine(x, padTop, x, padTop + plotH);
                gc.fillText(String.format("%.1f", val), x - 12, padTop + plotH + 16);
            }
            gc.setLineDashes(null);
        }

        // Axis Titles
        gc.setFill(textColor);
        gc.setFont(Font.font("System", FontWeight.BOLD, 11));
        gc.fillText(getXAxisTitle(), padLeft + plotW / 2.0 - 40, padTop + plotH + 34);

        gc.save();
        gc.translate(14, padTop + plotH / 2.0 + 30);
        gc.rotate(-90);
        gc.fillText(getYAxisTitle(), 0, 0);
        gc.restore();

        // Quadrant Crosshairs
        double effQuadX = Double.isNaN(getQuadrantX()) ? (minX + spanX * 0.5) : getQuadrantX();
        double effQuadY = Double.isNaN(getQuadrantY()) ? (minY + spanY * 0.5) : getQuadrantY();

        if (isShowQuadrants()) {
            double scrQuadX = padLeft + ((effQuadX - minX) / spanX) * plotW;
            double scrQuadY = padTop + (1.0 - (effQuadY - minY) / spanY) * plotH;

            Color quadColor = ThemeColors.resolve(this, ColorRole.TERTIARY);
            gc.setStroke(Color.color(quadColor.getRed(), quadColor.getGreen(), quadColor.getBlue(), 0.55));
            gc.setLineWidth(1.5);
            gc.setLineDashes(6.0, 4.0);

            if (scrQuadX >= padLeft && scrQuadX <= padLeft + plotW) {
                gc.strokeLine(scrQuadX, padTop, scrQuadX, padTop + plotH);
            }
            if (scrQuadY >= padTop && scrQuadY <= padTop + plotH) {
                gc.strokeLine(padLeft, scrQuadY, padLeft + plotW, scrQuadY);
            }
            gc.setLineDashes(null);

            gc.setFill(Color.color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), 0.45));
            gc.setFont(Font.font("System", FontWeight.NORMAL, 9));
            gc.fillText("Q2 (High Vel / Low Load)", padLeft + 6, padTop + 14);
            gc.fillText("Q1 (High Vel / High Load)", padLeft + plotW - 120, padTop + 14);
            gc.fillText("Q3 (Low Vel / Low Load)", padLeft + 6, padTop + plotH - 8);
            gc.fillText("Q4 (High Load / Low Vel)", padLeft + plotW - 116, padTop + plotH - 8);
        }

        // Draw Points, Regression Line, and Hover Proximity
        M3ScatterPoint nearestPoint = null;
        M3ScatterSeries nearestSeries = null;
        double nearestDistSq = Double.MAX_VALUE;
        double nearestPx = 0, nearestPy = 0;

        List<String> trendlineStatsList = new ArrayList<>();

        for (int sIdx = 0; sIdx < seriesList.size(); sIdx++) {
            M3ScatterSeries series = seriesList.get(sIdx);
            Color sColor = resolveSeriesColor(sIdx, series);

            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;
            int n = series.getData().size();

            for (M3ScatterPoint pt : series.getData()) {
                double px = padLeft + ((pt.getX() - minX) / spanX) * plotW;
                double py = padTop + (1.0 - (pt.getY() - minY) / spanY) * plotH;
                double r = Math.max(3.0, pt.getRadius());

                if (isHovering) {
                    double distSq = (hoverMouseX - px) * (hoverMouseX - px) + (hoverMouseY - py) * (hoverMouseY - py);
                    double hitThreshold = (r + 7.0) * (r + 7.0);
                    if (distSq <= hitThreshold && distSq < nearestDistSq) {
                        nearestDistSq = distSq;
                        nearestPoint = pt;
                        nearestSeries = series;
                        nearestPx = px;
                        nearestPy = py;
                    }
                }

                Color fill = Color.color(sColor.getRed(), sColor.getGreen(), sColor.getBlue(), 0.75);
                gc.setFill(fill);
                gc.fillOval(px - r, py - r, r * 2, r * 2);

                gc.setStroke(sColor);
                gc.setLineWidth(1.5);
                gc.strokeOval(px - r, py - r, r * 2, r * 2);

                gc.setFill(ThemeColors.resolve(this, ColorRole.SURFACE));
                gc.fillOval(px - 1.5, py - 1.5, 3.0, 3.0);

                sumX += pt.getX();
                sumY += pt.getY();
                sumXY += pt.getX() * pt.getY();
                sumX2 += pt.getX() * pt.getX();
                sumY2 += pt.getY() * pt.getY();
            }

            // OLS Linear Regression Trendline
            if (isShowTrendline() && series.isShowTrendline() && n >= 2) {
                double denom = n * sumX2 - sumX * sumX;
                if (Math.abs(denom) > 1e-9) {
                    double slope = (n * sumXY - sumX * sumY) / denom;
                    double intercept = (sumY - slope * sumX) / n;

                    double numR = n * sumXY - sumX * sumY;
                    double denR = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY));
                    double r2 = (denR > 1e-9) ? (numR / denR) * (numR / denR) : 0.0;

                    double x1 = minX;
                    double y1 = slope * x1 + intercept;
                    double x2 = maxX;
                    double y2 = slope * x2 + intercept;

                    double sx1 = padLeft + ((x1 - minX) / spanX) * plotW;
                    double sy1 = padTop + (1.0 - (y1 - minY) / spanY) * plotH;
                    double sx2 = padLeft + ((x2 - minX) / spanX) * plotW;
                    double sy2 = padTop + (1.0 - (y2 - minY) / spanY) * plotH;

                    gc.save();
                    // Clip the extrapolated line to the plot area so it never paints over axes
                    gc.beginPath();
                    gc.rect(padLeft, padTop, plotW, plotH);
                    gc.clip();
                    gc.setStroke(sColor);
                    gc.setLineWidth(2.2);
                    gc.setLineCap(StrokeLineCap.ROUND);
                    gc.strokeLine(sx1, sy1, sx2, sy2);
                    gc.restore();

                    trendlineStatsList.add(String.format("%s: y = %.3fx %s %.2f (R² = %.3f)",
                            series.getName(), slope, (intercept >= 0 ? "+" : "-"), Math.abs(intercept), r2));
                }
            }
        }

        // Draw Trendline Equation Badge
        if (isShowTrendlineStats() && !trendlineStatsList.isEmpty()) {
            double badgeW = 210.0;
            double badgeH = 14.0 + trendlineStatsList.size() * 14.0;
            double badgeX = padLeft + plotW - badgeW - 6.0;
            double badgeY = padTop + 8.0;

            Color bg = ThemeColors.resolve(this, ColorRole.SURFACE_CONTAINER_HIGH);
            Color border = ThemeColors.resolve(this, ColorRole.OUTLINE_VARIANT);
            gc.setFill(Color.color(bg.getRed(), bg.getGreen(), bg.getBlue(), 0.85));
            gc.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 6, 6);
            gc.setStroke(border);
            gc.setLineWidth(0.8);
            gc.strokeRoundRect(badgeX, badgeY, badgeW, badgeH, 6, 6);

            gc.setFont(Font.font("System", FontWeight.BOLD, 9));
            for (int i = 0; i < trendlineStatsList.size(); i++) {
                gc.setFill(textColor);
                gc.fillText(trendlineStatsList.get(i), badgeX + 8, badgeY + 12 + i * 14);
            }
        }

        // Highlight nearest hovered point
        if (nearestPoint != null && nearestSeries != null) {
            Color sColor = nearestSeries.getColor() != null ? nearestSeries.getColor() : ThemeColors.resolve(this, ColorRole.PRIMARY);
            double r = Math.max(3.0, nearestPoint.getRadius());

            gc.setFill(Color.color(sColor.getRed(), sColor.getGreen(), sColor.getBlue(), 0.30));
            gc.fillOval(nearestPx - (r + 7), nearestPy - (r + 7), (r + 7) * 2, (r + 7) * 2);

            gc.setFill(sColor.brighter());
            gc.fillOval(nearestPx - (r + 1.5), nearestPy - (r + 1.5), (r + 1.5) * 2, (r + 1.5) * 2);

            drawTooltip(gc, nearestPx, nearestPy, nearestPoint, nearestSeries);
        }
    }

    private void drawTooltip(GraphicsContext gc, double px, double py, M3ScatterPoint pt, M3ScatterSeries series) {
        double tipW = 150.0;
        double tipH = 58.0;
        double tipX = px + 12;
        if (tipX + tipW > getWidth() - 10) tipX = Math.max(10, px - tipW - 12);
        double tipY = Math.max(10, Math.min(getHeight() - tipH - 10, py - tipH / 2.0));

        // M3 plain tooltip: inverse surface container, inverse on-surface content, 4dp radius
        Color bg = ThemeColors.resolve(this, ColorRole.INVERSE_SURFACE);
        Color text = ThemeColors.resolve(this, ColorRole.INVERSE_ON_SURFACE);
        Color textVariant = ThemeColors.resolve(this, ColorRole.INVERSE_ON_SURFACE);

        gc.setFill(bg);
        gc.fillRoundRect(tipX, tipY, tipW, tipH, 4, 4);

        gc.setFill(text);
        gc.setFont(Font.font("System", FontWeight.BOLD, 10));
        String title = pt.getLabel().isEmpty() ? series.getName() : pt.getLabel();
        gc.fillText(title, tipX + 8, tipY + 16);

        gc.setFont(Font.font("System", FontWeight.NORMAL, 10));
        gc.setFill(textVariant);
        gc.fillText(String.format("X: %.2f %s", pt.getX(), getXUnit()), tipX + 8, tipY + 32);
        gc.fillText(String.format("Y: %.2f %s", pt.getY(), getYUnit()), tipX + 8, tipY + 46);

        if (!pt.getExtra().isEmpty()) {
            gc.fillText("Note: " + pt.getExtra(), tipX + 80, tipY + 32);
        }
    }
}
