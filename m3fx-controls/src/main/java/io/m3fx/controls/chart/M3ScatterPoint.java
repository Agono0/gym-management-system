package io.m3fx.controls.chart;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * A multidimensional data point in an {@link M3ScatterPlot}.
 */
// Suppressed this-escape (false positives javac 27 cannot see across the module boundary):
// (1) the JavaFX property-bean idiom new Simple*Property(this, ...) - the property
// constructor only stores the bean reference, exactly like every built-in control;
// (2) constructor calls to JavaFX final methods (getStyleClass(), setAccessibleRole())
// - final, but javac does not honor ACC_FINAL for symbols from another module. This
// class's own overridable methods are never called during construction.
@SuppressWarnings("this-escape")
public class M3ScatterPoint {

    private final DoubleProperty x = new SimpleDoubleProperty(this, "x", 0.0);
    private final DoubleProperty y = new SimpleDoubleProperty(this, "y", 0.0);
    private final DoubleProperty radius = new SimpleDoubleProperty(this, "radius", 6.0);
    private final StringProperty label = new SimpleStringProperty(this, "label", "");
    private final StringProperty extra = new SimpleStringProperty(this, "extra", "");

    /**
     * Creates an unlabeled point at the given coordinates.
     *
     * @param x X value
     * @param y Y value
     */
    public M3ScatterPoint(double x, double y) {
        this("", x, y, 6.0, "");
    }

    /**
     * Creates a labeled point with default radius and no extra note.
     *
     * @param label point label (tooltip title)
     * @param x X value
     * @param y Y value
     */
    public M3ScatterPoint(String label, double x, double y) {
        this(label, x, y, 6.0, "");
    }

    /**
     * Creates a labeled point with a custom bubble radius.
     *
     * @param label point label (tooltip title)
     * @param x X value
     * @param y Y value
     * @param radius bubble radius in px (min 3 applied at render time)
     */
    public M3ScatterPoint(String label, double x, double y, double radius) {
        this(label, x, y, radius, "");
    }

    /**
     * Creates an unlabeled point with a custom bubble radius.
     *
     * @param x X value
     * @param y Y value
     * @param radius bubble radius in px
     * @param label point label (tooltip title)
     */
    public M3ScatterPoint(double x, double y, double radius, String label) {
        this(label, x, y, radius, "");
    }

    /**
     * Creates a fully specified point.
     *
     * @param label point label (tooltip title)
     * @param x X value
     * @param y Y value
     * @param radius bubble radius in px
     * @param extra extra note shown in the tooltip
     */
    public M3ScatterPoint(String label, double x, double y, double radius, String extra) {
        setLabel(label);
        setX(x);
        setY(y);
        setRadius(radius);
        setExtra(extra);
    }

    /** The X value in data units. */
    public DoubleProperty xProperty() {
        return x;
    }

    /** Gets the X value in data units. */
    public double getX() {
        return x.get();
    }

    /** Sets the X value in data units. */
    public final void setX(double x) {
        this.x.set(x);
    }

    /** The Y value in data units. */
    public DoubleProperty yProperty() {
        return y;
    }

    /** Gets the Y value in data units. */
    public double getY() {
        return y.get();
    }

    /** Sets the Y value in data units. */
    public final void setY(double y) {
        this.y.set(y);
    }

    /** The bubble radius in px (clamped to a 3px minimum at render time). */
    public DoubleProperty radiusProperty() {
        return radius;
    }

    /** Gets the bubble radius in px. */
    public double getRadius() {
        return radius.get();
    }

    /** Sets the bubble radius in px (clamped to a 3px minimum at render time). */
    public final void setRadius(double radius) {
        this.radius.set(radius);
    }

    /** The point label used as the tooltip title. */
    public StringProperty labelProperty() {
        return label;
    }

    /** Gets the point label used as the tooltip title. */
    public String getLabel() {
        return label.get();
    }

    /** Sets the point label used as the tooltip title; {@code null} becomes an empty string. */
    public final void setLabel(String label) {
        this.label.set(label == null ? "" : label);
    }

    /** An optional extra note appended to the tooltip. */
    public StringProperty extraProperty() {
        return extra;
    }

    /** Gets the optional extra note appended to the tooltip. */
    public String getExtra() {
        return extra.get();
    }

    /** Sets the optional extra note appended to the tooltip; {@code null} becomes an empty string. */
    public final void setExtra(String extra) {
        this.extra.set(extra == null ? "" : extra);
    }

    @Override
    public String toString() {
        return String.format("M3ScatterPoint['%s', x=%.2f, y=%.2f, r=%.1f]", getLabel(), getX(), getY(), getRadius());
    }
}
