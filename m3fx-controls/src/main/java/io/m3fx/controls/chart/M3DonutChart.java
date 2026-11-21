package io.m3fx.controls.chart;

import io.m3fx.controls.internal.SpringAnimation;
import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;

/**
 * M3 Expressive concentric multi-ring radial progress gauge with spring-animated arcs,
 * center telemetry headline, and rounded track caps.
 */
// Suppressed this-escape (false positives javac 27 cannot see across the module boundary):
// (1) the JavaFX property-bean idiom new Simple*Property(this, ...) - the property
// constructor only stores the bean reference, exactly like every built-in control;
// (2) constructor calls to JavaFX final methods (getStyleClass(), setAccessibleRole())
// - final, but javac does not honor ACC_FINAL for symbols from another module. This
// class's own overridable methods are never called during construction.
@SuppressWarnings("this-escape")
public class M3DonutChart extends StackPane {

    /**
     * Model for a single radial ring gauge. The ring animates from zero to its target fraction
     * with the M3 Expressive bouncy spring as soon as it is created.
     */
    public static class Ring {
        private final String label;
        private final double value;
        private final double maxValue;
        private final Color color;
        final DoubleProperty animatedProgress = new SimpleDoubleProperty(0.0);

        /**
         * Creates a ring.
         *
         * @param label metric label
         * @param value current value
         * @param maxValue maximum value (100% capacity; values &le; 0 fall back to 100)
         * @param color accent color
         */
        public Ring(String label, double value, double maxValue, Color color) {
            this.label = label;
            this.value = value;
            this.maxValue = maxValue <= 0 ? 100 : maxValue;
            this.color = color;
            SpringAnimation.animate(animatedProgress, Math.min(1.0, Math.max(0.0, value / this.maxValue)), SpringAnimation.BOUNCY, null);
        }

        /** @return the metric label */
        public String getLabel() { return label; }

        /** @return the current value */
        public double getValue() { return value; }

        /** @return the maximum value (100% capacity) */
        public double getMaxValue() { return maxValue; }

        /** @return the accent color */
        public Color getColor() { return color; }
    }

    private final ObservableList<Ring> rings = FXCollections.observableArrayList();
    private final StringProperty centerHeadline = new SimpleStringProperty(this, "centerHeadline", "");
    private final StringProperty centerSubtitle = new SimpleStringProperty(this, "centerSubtitle", "");

    private final Canvas canvas = new Canvas();
    private final Label headlineLabel = new Label();
    private final Label subtitleLabel = new Label();

    /** Creates an empty donut chart. */
    public M3DonutChart() {
        getStyleClass().add("m3-donut-chart");

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        headlineLabel.getStyleClass().add("m3-donut-headline");
        headlineLabel.textProperty().bind(centerHeadlineProperty());

        subtitleLabel.getStyleClass().add("m3-donut-subtitle");
        subtitleLabel.textProperty().bind(centerSubtitleProperty());

        javafx.scene.layout.VBox centerBox = new javafx.scene.layout.VBox(2, headlineLabel, subtitleLabel);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setMouseTransparent(true);

        getChildren().addAll(canvas, centerBox);

        widthProperty().addListener((obs, o, n) -> render());
        heightProperty().addListener((obs, o, n) -> render());
        // The center text must re-fit when its content changes ("—" -> "84.8 kg").
        centerHeadlineProperty().addListener((obs, o, n) -> render());
        centerSubtitleProperty().addListener((obs, o, n) -> render());
        ChartThemeSupport.attach(this, this::render);

        rings.addListener((ListChangeListener<Ring>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Ring r : c.getAddedSubList()) {
                        r.animatedProgress.addListener((obs, o, n) -> render());
                    }
                }
            }
            render();
        });

        setPrefSize(220, 220);
        setMinSize(140, 140);
    }

    /** @return the observable list of radial rings */
    public ObservableList<Ring> getRings() {
        return rings;
    }

    /** The bold headline shown in the donut center. */
    public StringProperty centerHeadlineProperty() {
        return centerHeadline;
    }

    public String getCenterHeadline() {
        return centerHeadline.get();
    }

    public void setCenterHeadline(String headline) {
        this.centerHeadline.set(headline);
    }

    /** The supporting line shown under the center headline. */
    public StringProperty centerSubtitleProperty() {
        return centerSubtitle;
    }

    public String getCenterSubtitle() {
        return centerSubtitle.get();
    }

    public void setCenterSubtitle(String subtitle) {
        this.centerSubtitle.set(subtitle);
    }

    /**
     * Adds a ring gauge to the chart.
     *
     * @param label metric label
     * @param value current value
     * @param maxValue maximum value (100% capacity)
     * @param color accent color, or {@code null} for theme primary/tertiary/secondary
     */
    public void addRing(String label, double value, double maxValue, Color color) {
        rings.add(new Ring(label, value, maxValue, color));
    }

    private Color resolveRingColor(int index, Ring ring) {
        if (ring.getColor() != null) {
            return ring.getColor();
        }
        return switch (index % 3) {
            case 0 -> ThemeColors.resolve(this, ColorRole.PRIMARY);
            case 1 -> ThemeColors.resolve(this, ColorRole.TERTIARY);
            default -> ThemeColors.resolve(this, ColorRole.SECONDARY);
        };
    }

    /** Render invocations, counted by the B6 theme-repaint tests (package-private). */
    int renderCountForTesting;

    /**
     * Radius of the innermost hole from the last render (package-private): the visual-QA tests
     * pin the center text to fit inside it.
     */
    double innerHoleRadiusForTesting;

    private void render() {
        renderCountForTesting++;
        double w = getWidth() > 0 ? getWidth() : getPrefWidth();
        double h = getHeight() > 0 ? getHeight() : getPrefHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        int count = rings.size();
        if (count == 0) return;

        double cx = w / 2.0;
        double cy = h / 2.0;
        double maxOuterRadius = Math.min(w, h) / 2.0 - 12.0;

        // Reserve a center hub so the rings never encroach on the hole the headline lives in,
        // then fit all the ring bands into the radial space between hub and outer edge
        // (thickness + 0.35 spacing per ring, last band ending exactly at the hub).
        double hubRadius = Math.max(24.0, maxOuterRadius * 0.42);
        double radialSpan = maxOuterRadius - hubRadius;
        double ringThickness = Math.max(6.0, Math.min(18.0,
                radialSpan / (count + (count - 1) * 0.35)));
        double ringSpacing = Math.max(3.0, ringThickness * 0.35);

        Color trackColor = ThemeColors.resolve(this, ColorRole.SURFACE_CONTAINER_HIGHEST);
        gc.setLineCap(StrokeLineCap.ROUND);

        double innermostInnerEdge = maxOuterRadius;
        for (int i = 0; i < count; i++) {
            // Band [innerEdge, outerEdge]; outer edges stack inward from maxOuterRadius so
            // every band stays between the hub and the reserved outer margin, and a thick
            // segment can no longer cross the inner radius.
            double outerEdge = maxOuterRadius - i * (ringThickness + ringSpacing);
            double innerEdge = outerEdge - ringThickness;
            if (innerEdge <= 1.0) break;
            innermostInnerEdge = innerEdge;
            double currentRadius = (outerEdge + innerEdge) / 2.0;

            Ring ring = rings.get(i);
            double arcX = cx - currentRadius;
            double arcY = cy - currentRadius;
            double arcDiam = currentRadius * 2.0;

            // Background Track
            gc.setStroke(Color.color(trackColor.getRed(), trackColor.getGreen(), trackColor.getBlue(), 0.35));
            gc.setLineWidth(ringThickness);
            gc.strokeArc(arcX, arcY, arcDiam, arcDiam, 0, 360, ArcType.OPEN);

            // Active Arc
            double progress = ring.animatedProgress.get();
            if (progress > 0.005) {
                double sweep = progress * 360.0;
                Color activeColor = resolveRingColor(i, ring);
                gc.setStroke(activeColor);
                gc.setLineWidth(ringThickness);

                // Start from 90° (top, JavaFX angles run counter-clockwise from 3 o'clock)
                // and sweep clockwise (negative extent)
                gc.strokeArc(arcX, arcY, arcDiam, arcDiam, 90, -sweep, ArcType.OPEN);
            }
        }

        innerHoleRadiusForTesting = Math.max(0.0, innermostInnerEdge);
        fitCenterTextToHub();
    }

    /**
     * Clamps the center headline/subtitle to the hub hole: the labels' CSS sizes are the
     * maximum, and each label shrinks (via inline style) until its rendered width fits inside
     * the hole, so "84.8 kg" can no longer collide with the ring on a small donut.
     */
    private void fitCenterTextToHub() {
        double holeDiameter = innerHoleRadiusForTesting * 2.0;
        fitLabel(headlineLabel, holeDiameter);
        fitLabel(subtitleLabel, holeDiameter);
    }

    /** Fits one center label inside the hub hole using its own (CSS-resolved) font. */
    private static void fitLabel(Label label, double holeDiameter) {
        String text = label.getText();
        if (text == null || text.isBlank() || holeDiameter <= 0) {
            return;
        }
        double currentSize = label.getFont().getSize();
        double available = Math.max(20.0, holeDiameter * 0.92 - 8.0);
        javafx.scene.text.Text measure = new javafx.scene.text.Text(text);
        measure.setFont(label.getFont());
        double measuredWidth = measure.getLayoutBounds().getWidth();
        if (currentSize <= 0 || measuredWidth <= 0 || measuredWidth <= available) {
            return;
        }
        // Text width scales ~linearly with font size, so the fitted size is independent of
        // which size the measurement ran at, and re-runs of render() are stable (no drift).
        double fitted = Math.max(10.0, currentSize * available / measuredWidth);
        label.setStyle(String.format(java.util.Locale.ROOT, "-fx-font-size: %.1fpx;", fitted));
    }
}

