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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * M3 Funnel Chart for Gym Member Lifecycle and Conversion Analytics.
 * Renders stacked tapered horizontal trapezoids or centered pill bars with
 * drop-off indicators, conversion chips, and interactive stage breakdown tooltips.
 */
public class M3FunnelChart extends Region {

    /** Rendering visual styles for the funnel. */
    public enum FunnelMode {
        /** Tapered trapezoid tiers connected into a classic funnel silhouette. */
        TRAPEZOID,
        /** Centered horizontal pill bars, one per stage. */
        PILL_BARS
    }

    private final ObservableList<M3FunnelStage> stages = FXCollections.observableArrayList();

    private final ObjectProperty<FunnelMode> mode = new SimpleObjectProperty<>(this, "mode", FunnelMode.TRAPEZOID);
    private final DoubleProperty gap = new SimpleDoubleProperty(this, "gap", 6.0);
    private final BooleanProperty showDropOffChips = new SimpleBooleanProperty(this, "showDropOffChips", true);
    private final BooleanProperty showConversionChips = new SimpleBooleanProperty(this, "showConversionChips", true);

    private final Canvas canvas = new Canvas();

    private int hoveredIndex = -1;
    private double hoverMouseX = -1;
    private double hoverMouseY = -1;
    private boolean isHovering = false;

    public M3FunnelChart() {
        getStyleClass().add("m3-funnel-chart");
        getChildren().add(canvas);

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        widthProperty().addListener((obs, o, n) -> render());
        heightProperty().addListener((obs, o, n) -> render());
        sceneProperty().addListener((obs, o, n) -> render());

        stages.addListener((ListChangeListener<M3FunnelStage>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (M3FunnelStage stage : c.getAddedSubList()) {
                        stage.countProperty().addListener((o, ov, nv) -> render());
                        stage.valueProperty().addListener((o, ov, nv) -> render());
                    }
                }
            }
            render();
        });

        mode.addListener((obs, o, n) -> render());
        gap.addListener((obs, o, n) -> render());
        showDropOffChips.addListener((obs, o, n) -> render());
        showConversionChips.addListener((obs, o, n) -> render());

        initInteractivity();

        setPrefSize(560, 300);
        setMinSize(300, 180);
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
            hoveredIndex = -1;
            hoverMouseX = -1;
            hoverMouseY = -1;
            render();
        });
    }

    /** @return the observable list of funnel stages (top of funnel first) */
    public ObservableList<M3FunnelStage> getStages() {
        return stages;
    }

    /** Visual style of the funnel tiers: tapered trapezoids or centered pill bars. */
    public ObjectProperty<FunnelMode> modeProperty() {
        return mode;
    }

    public FunnelMode getMode() {
        return mode.get();
    }

    public void setMode(FunnelMode mode) {
        this.mode.set(mode);
    }

    /** Vertical spacing between tiers in px. */
    public DoubleProperty gapProperty() {
        return gap;
    }

    public double getGap() {
        return gap.get();
    }

    public void setGap(double gap) {
        this.gap.set(gap);
    }

    /** Whether the red inter-stage drop-off indicators are drawn. */
    public BooleanProperty showDropOffChipsProperty() {
        return showDropOffChips;
    }

    public boolean isShowDropOffChips() {
        return showDropOffChips.get();
    }

    public void setShowDropOffChips(boolean show) {
        this.showDropOffChips.set(show);
    }

    /** Whether the overall-conversion percentage chips are drawn to the right of each tier. */
    public BooleanProperty showConversionChipsProperty() {
        return showConversionChips;
    }

    public boolean isShowConversionChips() {
        return showConversionChips.get();
    }

    public void setShowConversionChips(boolean show) {
        this.showConversionChips.set(show);
    }

    private Color resolveStageColor(int index, M3FunnelStage stage) {
        if (stage.getColor() != null) return stage.getColor();
        return switch (index % 5) {
            case 0 -> ThemeColors.resolve(this, ColorRole.PRIMARY);
            case 1 -> ThemeColors.resolve(this, ColorRole.TERTIARY);
            case 2 -> ThemeColors.resolve(this, ColorRole.SECONDARY);
            case 3 -> ThemeColors.resolve(this, ColorRole.PRIMARY_CONTAINER);
            default -> ThemeColors.resolve(this, ColorRole.SECONDARY_CONTAINER);
        };
    }

    private void render() {
        double w = getWidth() > 0 ? getWidth() : getPrefWidth();
        double h = getHeight() > 0 ? getHeight() : getPrefHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        int n = stages.size();
        if (n == 0) return;

        double padLeft = 16.0;
        double padRight = 16.0;
        double padTop = 16.0;
        double padBottom = 16.0;

        double leftColW = 150.0;
        double rightColW = isShowConversionChips() ? 120.0 : 20.0;
        double centerW = w - padLeft - padRight - leftColW - rightColW;
        if (centerW <= 40) centerW = 40;

        double plotH = h - padTop - padBottom;
        double spacing = getGap();
        double tierH = Math.max(16.0, (plotH - (n - 1) * spacing) / n);

        double topCount = stages.get(0).getCount();
        if (topCount <= 0) topCount = 1.0;

        Color textColor = ThemeColors.resolve(this, ColorRole.ON_SURFACE);
        Color textVariant = ThemeColors.resolve(this, ColorRole.ON_SURFACE_VARIANT);
        Color errorColor = ThemeColors.resolve(this, ColorRole.ERROR);

        hoveredIndex = -1;

        for (int i = 0; i < n; i++) {
            M3FunnelStage stage = stages.get(i);
            double y = padTop + i * (tierH + spacing);

            double currentFrac = Math.max(0.08, Math.min(1.0, stage.getCount() / topCount));
            double nextFrac = (i < n - 1)
                    ? Math.max(0.06, Math.min(1.0, stages.get(i + 1).getCount() / topCount))
                    : currentFrac * 0.85;

            double topWidth = currentFrac * centerW;
            double btmWidth = (getMode() == FunnelMode.TRAPEZOID) ? (nextFrac * centerW) : topWidth;

            double cx = padLeft + leftColW + centerW / 2.0;

            boolean isTierHovered = isHovering
                    && hoverMouseY >= y && hoverMouseY <= y + tierH
                    && hoverMouseX >= padLeft && hoverMouseX <= w - padRight;

            if (isTierHovered) {
                hoveredIndex = i;
            }

            // Left Stage Label
            gc.setFill(isTierHovered ? textColor : textVariant);
            gc.setFont(Font.font("System", FontWeight.BOLD, 11));
            gc.fillText(stage.getName(), padLeft, y + tierH * 0.45);

            gc.setFont(Font.font("System", FontWeight.NORMAL, 10));
            gc.setFill(textVariant);
            String subtext = String.format("%.0f units", stage.getCount());
            if (stage.getValue() > 0) {
                subtext += String.format(" • $%,.0f", stage.getValue());
            }
            gc.fillText(subtext, padLeft, y + tierH * 0.85);

            // Center Visual Funnel Tier
            Color tierColor = resolveStageColor(i, stage);
            if (isTierHovered) tierColor = tierColor.brighter();

            gc.setFill(tierColor);

            if (getMode() == FunnelMode.TRAPEZOID) {
                double[] xPoints = {
                        cx - topWidth / 2.0,
                        cx + topWidth / 2.0,
                        cx + btmWidth / 2.0,
                        cx - btmWidth / 2.0
                };
                double[] yPoints = {
                        y,
                        y,
                        y + tierH,
                        y + tierH
                };
                gc.fillPolygon(xPoints, yPoints, 4);

                if (isTierHovered) {
                    gc.setStroke(ThemeColors.resolve(this, ColorRole.ON_SURFACE));
                    gc.setLineWidth(1.5);
                    gc.strokePolygon(xPoints, yPoints, 4);
                }
            } else {
                double barX = cx - topWidth / 2.0;
                double radius = Math.min(tierH, 12.0);
                gc.fillRoundRect(barX, y, topWidth, tierH, radius, radius);

                if (isTierHovered) {
                    gc.setStroke(ThemeColors.resolve(this, ColorRole.ON_SURFACE));
                    gc.setLineWidth(1.5);
                    gc.strokeRoundRect(barX, y, topWidth, tierH, radius, radius);
                }
            }

            // Right Conversion Chips
            if (isShowConversionChips()) {
                double overallConv = (stage.getCount() / topCount) * 100.0;
                double chipX = padLeft + leftColW + centerW + 16.0;
                double chipY = y + tierH * 0.20;
                double chipW = 56.0;
                double chipH = tierH * 0.60;

                Color chipBg = ThemeColors.resolve(this, ColorRole.SURFACE_CONTAINER_HIGH);
                gc.setFill(chipBg);
                gc.fillRoundRect(chipX, chipY, chipW, chipH, 6, 6);

                gc.setFill(textColor);
                gc.setFont(Font.font("System", FontWeight.BOLD, 10));
                gc.fillText(String.format("%.1f%%", overallConv), chipX + 8, chipY + chipH * 0.70);
            }

            // Inter-stage Drop-off indicator
            if (isShowDropOffChips() && i < n - 1) {
                M3FunnelStage nextStage = stages.get(i + 1);
                double dropCount = stage.getCount() - nextStage.getCount();
                double dropPct = (stage.getCount() > 0) ? (dropCount / stage.getCount()) * 100.0 : 0.0;

                if (dropCount > 0) {
                    double dropY = y + tierH + (spacing / 2.0);
                    gc.setFill(Color.color(errorColor.getRed(), errorColor.getGreen(), errorColor.getBlue(), 0.70));
                    gc.setFont(Font.font("System", FontWeight.NORMAL, 8.5));
                    String dropStr = String.format("▼ -%.1f%% (-%.0f)", dropPct, dropCount);
                    gc.fillText(dropStr, padLeft + leftColW + centerW + 16.0, dropY + 3.0);
                }
            }
        }

        // Draw Interactive Hover Tooltip
        if (hoveredIndex >= 0 && hoveredIndex < stages.size()) {
            M3FunnelStage stage = stages.get(hoveredIndex);
            double overallConv = (stage.getCount() / topCount) * 100.0;
            double stepConv = 100.0;
            double dropPct = 0.0;

            if (hoveredIndex > 0) {
                double prevCount = stages.get(hoveredIndex - 1).getCount();
                stepConv = prevCount > 0 ? (stage.getCount() / prevCount) * 100.0 : 0;
                dropPct = 100.0 - stepConv;
            }

            drawTooltip(gc, hoverMouseX, hoverMouseY, stage, overallConv, stepConv, dropPct);
        }
    }

    private void drawTooltip(GraphicsContext gc, double mouseX, double mouseY,
                            M3FunnelStage stage, double overallConv, double stepConv, double dropPct) {
        double tipW = 165.0;
        double tipH = 74.0;
        double tipX = mouseX + 12;
        if (tipX + tipW > getWidth() - 10) tipX = mouseX - tipW - 12;
        double tipY = Math.max(10, Math.min(getHeight() - tipH - 10, mouseY - tipH / 2.0));

        // M3 plain tooltip: inverse surface container, inverse on-surface content, 4dp radius
        Color bg = ThemeColors.resolve(this, ColorRole.INVERSE_SURFACE);
        Color text = ThemeColors.resolve(this, ColorRole.INVERSE_ON_SURFACE);
        Color textVariant = ThemeColors.resolve(this, ColorRole.INVERSE_ON_SURFACE);

        gc.setFill(bg);
        gc.fillRoundRect(tipX, tipY, tipW, tipH, 4, 4);

        gc.setFill(text);
        gc.setFont(Font.font("System", FontWeight.BOLD, 10.5));
        gc.fillText(stage.getName(), tipX + 8, tipY + 16);

        gc.setFont(Font.font("System", FontWeight.NORMAL, 9.5));
        gc.setFill(textVariant);
        gc.fillText(String.format("Volume: %.0f (%.1f%% of Total)", stage.getCount(), overallConv), tipX + 8, tipY + 32);
        gc.fillText(String.format("Step Conversion: %.1f%%", stepConv), tipX + 8, tipY + 46);
        gc.fillText(String.format("Step Drop-off: %.1f%%", dropPct), tipX + 8, tipY + 60);
    }
}

