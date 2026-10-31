package io.m3fx.controls.chart;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * A single data point in an M3 chart with a category/time label and numeric value.
 */
// Suppressed this-escape (false positives javac 27 cannot see across the module boundary):
// (1) the JavaFX property-bean idiom new Simple*Property(this, ...) - the property
// constructor only stores the bean reference, exactly like every built-in control;
// (2) constructor calls to JavaFX final methods (getStyleClass(), setAccessibleRole())
// - final, but javac does not honor ACC_FINAL for symbols from another module. This
// class's own overridable methods are never called during construction.
@SuppressWarnings("this-escape")
public class M3DataPoint {

    private final StringProperty label = new SimpleStringProperty(this, "label", "");
    private final DoubleProperty value = new SimpleDoubleProperty(this, "value", 0.0);

    /**
     * Creates a data point with a label and value.
     *
     * @param label category or timestamp label (e.g., "Mon", "09:00", "Week 1")
     * @param value numeric value
     */
    public M3DataPoint(String label, double value) {
        setLabel(label);
        setValue(value);
    }

    /**
     * Creates a data point with a numeric value.
     *
     * @param value numeric value
     */
    public M3DataPoint(double value) {
        this("", value);
    }

    public StringProperty labelProperty() {
        return label;
    }

    public String getLabel() {
        return label.get();
    }

    public final void setLabel(String label) {
        this.label.set(label == null ? "" : label);
    }

    public DoubleProperty valueProperty() {
        return value;
    }

    public double getValue() {
        return value.get();
    }

    public final void setValue(double value) {
        this.value.set(value);
    }

    @Override
    public String toString() {
        return "M3DataPoint[label='" + getLabel() + "', value=" + getValue() + "]";
    }
}

