package io.m3fx.controls.chart;

import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * M3 Heatmap matrix displaying cell intensity levels across two axes (e.g., days of week vs. hours of day),
 * with smooth color grading and interactive hover inspection.
 */
public class M3Heatmap extends Region {

    private int rows = 7;
    private int cols = 16;
    private double[][] values = new double[7][16];
    private final List<String> rowLabels = new ArrayList<>(Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"));
    private final List<String> colLabels = new ArrayList<>();

    private final ObjectProperty<Color> maxColor = new SimpleObjectProperty<>(this, "maxColor", null);
    private final Canvas canvas = new Canvas();

    private int hoveredRow = -1;
    private int hoveredCol = -1;
    private double hoverMouseX = -1;
    private double hoverMouseY = -1;
    private boolean isHovering = false;

    /** Creates a standard weekly peak-hours heatmap (Mon-Sun × 6:00 to 22:00). */
    public M3Heatmap() {
        getStyleClass().add("m3-heatmap");
        getChildren().add(canvas);

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        for (int h = 6; h < 22; h++) {
            colLabels.add(String.format("%02d:00", h));
        }

        widthProperty().addListener((obs, o, n) -> render());
        heightProperty().addListener((obs, o, n) -> render());
        sceneProperty().addListener((obs, o, n) -> render());
        maxColor.addListener((obs, o, n) -> render());

        initInteractivity();

        setPrefSize(500, 220);
        setMinSize(280, 140);
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
            hoveredRow = -1;
            hoveredCol = -1;
            render();
        });
    }

    /**
     * Resizes the matrix; all cell values reset to 0.
     *
     * @param rows number of rows (min 1)
     * @param cols number of columns (min 1)
     */
    public void setDimensions(int rows, int cols) {
        this.rows = Math.max(1, rows);
        this.cols = Math.max(1, cols);
        this.values = new double[this.rows][this.cols];
        render();
    }

    /**
     * Replaces the row labels (drawn on the left, one per row).
     *
     * @param labels row labels, or {@code null} to clear
     */
    public void setRowLabels(List<String> labels) {
        rowLabels.clear();
        if (labels != null) rowLabels.addAll(labels);
        render();
    }

    /**
     * Replaces the column labels (drawn underneath, thinned to fit).
     *
     * @param labels column labels, or {@code null} to clear
     */
    public void setColLabels(List<String> labels) {
        colLabels.clear();
        if (labels != null) colLabels.addAll(labels);
        render();
    }

    /**
     * Sets one cell's intensity (clamped to 0..1).
     *
     * @param row row index
     * @param col column index
     * @param val intensity from 0 (idle) to 1 (peak)
     */
    public void setValue(int row, int col, double val) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            values[row][col] = Math.max(0.0, Math.min(1.0, val));
            render();
        }
    }

    /**
     * Reads one cell's intensity.
     *
     * @param row row index
     * @param col column index
     * @return the stored intensity, or 0 when out of bounds
     */
    public double getValue(int row, int col) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            return values[row][col];
        }
        return 0.0;
    }

    /** Custom color for the maximum-intensity cells, or {@code null} for theme primary. */
    public ObjectProperty<Color> maxColorProperty() {
        return maxColor;
    }

    public Color getMaxColor() {
        return maxColor.get();
    }

    public void setMaxColor(Color color) {
        this.maxColor.set(color);
    }

    private void render() {
        double w = getWidth() > 0 ? getWidth() : getPrefWidth();
        double h = getHeight() > 0 ? getHeight() : getPrefHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        double padLeft = 40.0;
        double padTop = 16.0;
        double padBottom = 28.0;
        double padRight = 16.0;

        double plotW = w - padLeft - padRight;
        double plotH = h - padTop - padBottom;
        if (plotW <= 0 || plotH <= 0) return;

        double cellW = plotW / cols;
        double cellH = plotH / rows;
        double gap = 3.0;

        Color baseLow = ThemeColors.resolve(this, ColorRole.SURFACE_CONTAINER_HIGH);
        Color highColor = getMaxColor() != null ? getMaxColor() : ThemeColors.resolve(this, ColorRole.PRIMARY);
        Color textColor = ThemeColors.resolve(this, ColorRole.ON_SURFACE_VARIANT);

        // Draw Row Labels (left)
        gc.setFill(textColor);
        gc.setFont(javafx.scene.text.Font.font(10));
        for (int r = 0; r < rows; r++) {
            if (r < rowLabels.size()) {
                String lbl = rowLabels.get(r);
                double y = padTop + r * cellH + cellH / 2.0 + 3.0;
                gc.fillText(lbl, 6, y);
            }
        }

        hoveredRow = -1;
        hoveredCol = -1;

        // Draw Cells
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double x = padLeft + c * cellW + gap / 2.0;
                double y = padTop + r * cellH + gap / 2.0;
                double rw = cellW - gap;
                double rh = cellH - gap;

                boolean isCellHovered = isHovering
                        && hoverMouseX >= x && hoverMouseX <= x + rw
                        && hoverMouseY >= y && hoverMouseY <= y + rh;

                if (isCellHovered) {
                    hoveredRow = r;
                    hoveredCol = c;
                }

                double val = values[r][c];
                Color cellColor = interpolateColor(baseLow, highColor, val);
                if (isCellHovered) {
                    cellColor = cellColor.brighter();
                }

                gc.setFill(cellColor);
                gc.fillRoundRect(x, y, rw, rh, 4, 4);

                if (isCellHovered) {
                    gc.setStroke(ThemeColors.resolve(this, ColorRole.ON_SURFACE));
                    gc.setLineWidth(1.5);
                    gc.strokeRoundRect(x, y, rw, rh, 4, 4);
                }
            }
        }

        // Draw Col Labels (bottom), thinned so labels never overlap
        int step = Math.max(1, cols / 6);
        gc.setTextAlign(TextAlignment.CENTER);
        for (int c = 0; c < cols; c += step) {
            if (c < colLabels.size()) {
                String lbl = colLabels.get(c);
                double x = padLeft + c * cellW + cellW / 2.0;
                gc.fillText(lbl, x, padTop + plotH + 18);
            }
        }
        gc.setTextAlign(TextAlignment.LEFT);

        // Draw Hover Tooltip
        if (hoveredRow >= 0 && hoveredCol >= 0) {
            String rowName = hoveredRow < rowLabels.size() ? rowLabels.get(hoveredRow) : "Row " + hoveredRow;
            String colName = hoveredCol < colLabels.size() ? colLabels.get(hoveredCol) : "Col " + hoveredCol;
            double val = values[hoveredRow][hoveredCol];
            drawTooltip(gc, hoverMouseX, hoverMouseY, rowName, colName, val);
        }
    }

    private void drawTooltip(GraphicsContext gc, double mouseX, double mouseY,
                            String rowName, String colName, double val) {
        double tipW = 140.0;
        double tipH = 46.0;
        double tipX = mouseX + 12;
        if (tipX + tipW > getWidth() - 10) tipX = Math.max(10, mouseX - tipW - 12);
        double tipY = Math.max(8, mouseY - tipH - 8);

        // M3 plain tooltip: inverse surface container, inverse on-surface content, 4dp radius
        Color bg = ThemeColors.resolve(this, ColorRole.INVERSE_SURFACE);
        Color text = ThemeColors.resolve(this, ColorRole.INVERSE_ON_SURFACE);

        gc.setFill(bg);
        gc.fillRoundRect(tipX, tipY, tipW, tipH, 4, 4);

        gc.setFill(text);
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 10));
        gc.fillText(rowName + " " + colName, tipX + 8, tipY + 16);

        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.NORMAL, 10));
        String desc = val > 0.8 ? "Peak Occupancy" : (val > 0.5 ? "Moderate" : "Light / Idle");
        gc.fillText(String.format("Intensity: %.0f%% (%s)", val * 100, desc), tipX + 8, tipY + 32);
    }

    private static Color interpolateColor(Color c1, Color c2, double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        double r = c1.getRed() + (c2.getRed() - c1.getRed()) * t;
        double g = c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t;
        double b = c1.getBlue() + (c2.getBlue() - c1.getBlue()) * t;
        return Color.color(r, g, b);
    }
}

