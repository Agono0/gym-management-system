package io.m3fx.controls.chart;

import javafx.beans.property.*;
import javafx.scene.paint.Color;

/**
 * A discrete tier or milestone in an {@link M3FunnelChart}.
 *
 * <p>{@code count} is the volumetric metric (users, sessions, sign-ups) that drives the tier
 * width and the conversion math; {@code value} is an optional monetary metric shown in the
 * stage subtext.
 */
public class M3FunnelStage {

    private final StringProperty name = new SimpleStringProperty(this, "name", "");
    private final DoubleProperty count = new SimpleDoubleProperty(this, "count", 0.0);
    private final DoubleProperty value = new SimpleDoubleProperty(this, "value", 0.0);
    private final ObjectProperty<Color> color = new SimpleObjectProperty<>(this, "color", null);

    /**
     * Creates a stage with no monetary value.
     *
     * @param name stage label
     * @param count volumetric metric (drives tier width)
     */
    public M3FunnelStage(String name, double count) {
        this(name, count, 0.0, null);
    }

    /**
     * Creates a stage with a monetary value.
     *
     * @param name stage label
     * @param count volumetric metric (drives tier width)
     * @param value monetary metric shown in the subtext
     */
    public M3FunnelStage(String name, double count, double value) {
        this(name, count, value, null);
    }

    /**
     * Creates a colored stage; the monetary value defaults to the count.
     *
     * @param name stage label
     * @param count volumetric metric (drives tier width and the value subtext)
     * @param color accent color, or {@code null} for the automatic theme palette
     */
    public M3FunnelStage(String name, double count, Color color) {
        this(name, count, count, color);
    }

    /**
     * Creates a fully specified stage.
     *
     * @param name stage label
     * @param count volumetric metric (drives tier width)
     * @param value monetary metric shown in the subtext
     * @param color accent color, or {@code null} for the automatic theme palette
     */
    public M3FunnelStage(String name, double count, double value, Color color) {
        setName(name);
        setCount(count);
        setValue(value);
        setColor(color);
    }

    /** The stage label. */
    public StringProperty nameProperty() {
        return name;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name == null ? "" : name);
    }

    /** The volumetric metric (users, sessions) driving the tier width. */
    public DoubleProperty countProperty() {
        return count;
    }

    public double getCount() {
        return count.get();
    }

    public void setCount(double count) {
        this.count.set(count);
    }

    /** The optional monetary metric shown in the stage subtext. */
    public DoubleProperty valueProperty() {
        return value;
    }

    public double getValue() {
        return value.get();
    }

    public void setValue(double value) {
        this.value.set(value);
    }

    /** The stage accent color, or {@code null} to use the automatic theme palette. */
    public ObjectProperty<Color> colorProperty() {
        return color;
    }

    public Color getColor() {
        return color.get();
    }

    public void setColor(Color color) {
        this.color.set(color);
    }

    @Override
    public String toString() {
        return String.format("M3FunnelStage['%s', count=%.0f, val=%.1f]", getName(), getCount(), getValue());
    }
}
