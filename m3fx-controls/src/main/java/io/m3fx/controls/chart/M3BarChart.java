package io.m3fx.controls.chart;

import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

/**
 * M3 Expressive Bar Chart featuring rounded pill-top columns, grouped series,
 * dashed gridlines, and interactive hover tooltips.
 */
public class M3BarChart extends Region {

    private final ObservableList<M3DataSeries> seriesList = FXCollections.observableArrayList();
    private final BooleanProperty showGrid = new SimpleBooleanProperty(this, "showGrid", true);

    private final Canvas canvas = new Canvas();

    private double hoverMouseX = -1;
    private double hoverMouseY = -1;
    private boolean isHovering = false;

    /** Creates an empty bar chart. */
    public M3BarChart() {
        getStyleClass().add("m3-bar-chart");
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

        showGrid.addListener((obs, o, n) -> render());

        initInteractivity();

        setPrefSize(480, 240);
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

    /**
     * The data model: one entry per series; each series holds one point per category.
     *
     * @return the observable list of series
     */
    public ObservableList<M3DataSeries> getSeriesList() {
        return seriesList;
    }

    /** Whether dashed horizontal gridlines and Y-axis labels are drawn. */
    public BooleanProperty showGridProperty() {
        return showGrid;
    }

    public boolean isShowGrid() {
        return showGrid.get();
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid.set(showGrid);
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
        if (plotW <= 0 || plotH <= 0 || seriesList.isEmpty()) {
            return;
        }

        double maxVal = 0;
        double minVal = 0;
        int numCategories = 0;
        for (M3DataSeries s : seriesList) {
            if (s.getData().size() > numCategories) {
                numCategories = s.getData().size();
            }
            for (M3DataPoint pt : s.getData()) {
                if (pt.getValue() > maxVal) maxVal = pt.getValue();
                if (pt.getValue() < minVal) minVal = pt.getValue();
            }
        }
        if (numCategories == 0) return;
        if (maxVal <= 0 && minVal >= 0) {
            maxVal = 10; // flat zero data: fall back to a 0..10 axis so the frame still renders
        } else if (maxVal <= 0) {
            maxVal = 0;  // all-negative data: axis spans minVal..0
        } else {
            maxVal = maxVal * 1.15; // headroom above the tallest positive bar
        }
        double range = maxVal - minVal; // strictly positive in every branch above

        Color gridColor = ThemeColors.resolve(this, ColorRole.OUTLINE_VARIANT);
        Color textColor = ThemeColors.resolve(this, ColorRole.ON_SURFACE_VARIANT);

        // Gridlines & Y-axis labels
        if (isShowGrid()) {
            gc.setStroke(Color.color(gridColor.getRed(), gridColor.getGreen(), gridColor.getBlue(), 0.3));
            gc.setLineWidth(1.0);
            gc.setLineDashes(4.0, 4.0);

            gc.setFill(textColor);
            gc.setFont(javafx.scene.text.Font.font(11));

            int ySteps = 4;
            for (int i = 0; i <= ySteps; i++) {
                double frac = i / (double) ySteps;
                double val = maxVal + (minVal - maxVal) * frac;
                double y = padTop + frac * plotH;

                gc.strokeLine(padLeft, y, padLeft + plotW, y);
                gc.fillText(String.format("%.0f", val), 6, y + 4);
            }
            gc.setLineDashes(null);
        }

        // Baseline where value == 0 (equals the plot bottom for non-negative data)
        double zeroY = padTop + plotH * (maxVal / range);

        double categorySlotW = plotW / numCategories;
        double groupPad = Math.max(4.0, categorySlotW * 0.15);
        double usableGroupW = categorySlotW - groupPad * 2;
        int numSeries = seriesList.size();
        double barW = Math.max(4.0, usableGroupW / numSeries);
        double barSpacing = 2.0;
        if (numSeries > 1) {
            barW = Math.max(3.0, (usableGroupW - (numSeries - 1) * barSpacing) / numSeries);
        }

        String hoveredLabel = null;
        String hoveredSeriesName = null;
        double hoveredValue = 0;
        double hoveredBarX = 0;
        double hoveredBarY = 0;
        boolean hasHoveredBar = false;

        boolean drawXLabels = categorySlotW >= 24.0; // fade out labels before they overlap

        // Draw bars per category & series
        for (int c = 0; c < numCategories; c++) {
            double groupStartX = padLeft + c * categorySlotW + groupPad;

            for (int s = 0; s < numSeries; s++) {
                M3DataSeries series = seriesList.get(s);
                if (c >= series.getData().size()) continue;

                M3DataPoint pt = series.getData().get(c);
                double v = pt.getValue();
                double barH = Math.abs(v) / range * plotH;
                double valueY = padTop + plotH * ((maxVal - v) / range);
                double barY = Math.min(valueY, zeroY); // top edge (bottom edge for negative bars)
                double barX = groupStartX + s * (barW + barSpacing);

                Color barColor = resolveSeriesColor(s, series);

                // Check hover
                boolean barHover = isHovering
                        && hoverMouseX >= barX && hoverMouseX <= barX + barW
                        && hoverMouseY >= barY && hoverMouseY <= barY + barH;

                if (barHover) {
                    barColor = barColor.brighter();
                    hasHoveredBar = true;
                    hoveredLabel = pt.getLabel();
                    hoveredSeriesName = series.getName();
                    hoveredValue = v;
                    hoveredBarX = barX + barW / 2.0;
                    hoveredBarY = barY;
                }

                if (barH < 0.5) continue;

                gc.setFill(barColor);
                // M3 Expressive pill shape: fully rounded leading corners
                fillPillBar(gc, barX, barY, barW, barH, barW / 2.0, v >= 0);
            }

            // X-axis Category Label
            if (drawXLabels) {
                M3DataSeries first = seriesList.get(0);
                if (c < first.getData().size()) {
                    String lbl = first.getData().get(c).getLabel();
                    gc.setFill(textColor);
                    gc.setFont(javafx.scene.text.Font.font(11));
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.fillText(lbl, groupStartX + usableGroupW / 2.0, padTop + plotH + 18);
                    gc.setTextAlign(TextAlignment.LEFT);
                }
            }
        }

        // Draw hover tooltip
        if (hasHoveredBar) {
            drawTooltip(gc, hoveredBarX, hoveredBarY, hoveredLabel, hoveredSeriesName, hoveredValue);
        }
    }

    /**
     * Fills a bar whose two leading corners are fully rounded (M3 Expressive pill shape) and whose
     * trailing corners are square, so the bar stays flush with the zero baseline.
     *
     * @param gc canvas context
     * @param x left edge
     * @param y top edge
     * @param w bar width
     * @param h bar height
     * @param radius corner radius (clamped to fit)
     * @param roundTop {@code true} to round the top corners (positive bars), {@code false} the bottom
     */
    private static void fillPillBar(GraphicsContext gc, double x, double y, double w, double h,
                                    double radius, boolean roundTop) {
        double r = Math.max(0.0, Math.min(radius, Math.min(w / 2.0, h)));
        gc.beginPath();
        if (roundTop) {
            gc.moveTo(x, y + h);
            gc.lineTo(x, y + r);
            gc.arcTo(x, y, x + r, y, r);
            gc.lineTo(x + w - r, y);
            gc.arcTo(x + w, y, x + w, y + r, r);
            gc.lineTo(x + w, y + h);
        } else {
            gc.moveTo(x, y);
            gc.lineTo(x, y + h - r);
            gc.arcTo(x, y + h, x + r, y + h, r);
            gc.lineTo(x + w - r, y + h);
            gc.arcTo(x + w, y + h, x + w, y + h - r, r);
            gc.lineTo(x + w, y);
        }
        gc.closePath();
        gc.fill();
    }

    private void drawTooltip(GraphicsContext gc, double barCenterX, double barTopY,
                            String category, String seriesName, double value) {
        double tipW = 110.0;
        double tipH = 46.0;
        double tipX = barCenterX - tipW / 2.0;
        tipX = Math.max(10, Math.min(getWidth() - tipW - 10, tipX));
        double tipY = Math.max(8, barTopY - tipH - 8);

        // M3 plain tooltip: inverse surface container, inverse on-surface content, 4dp radius
        Color bg = ThemeColors.resolve(this, ColorRole.INVERSE_SURFACE);
        Color text = ThemeColors.resolve(this, ColorRole.INVERSE_ON_SURFACE);

        gc.setFill(bg);
        gc.fillRoundRect(tipX, tipY, tipW, tipH, 4, 4);

        gc.setFill(text);
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 10));
        gc.fillText(category == null || category.isEmpty() ? seriesName : category, tipX + 8, tipY + 16);

        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.NORMAL, 10));
        gc.fillText(seriesName + ": " + String.format("%.1f", value), tipX + 8, tipY + 32);
    }
}

