package io.m3fx.controls.chart;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

import java.util.Collection;

/**
 * A named series of data points in an M3 chart with an optional custom accent color.
 */
public class M3DataSeries {

    private final StringProperty name = new SimpleStringProperty(this, "name", "");
    private final ObjectProperty<Color> color = new SimpleObjectProperty<>(this, "color", null);
    private final ObservableList<M3DataPoint> data = FXCollections.observableArrayList();

    /**
     * Creates an empty series.
     *
     * @param name series name (shown in legends/tooltips)
     */
    public M3DataSeries(String name) {
        setName(name);
    }

    /**
     * Creates a series with points.
     *
     * @param name series name
     * @param points initial points
     */
    public M3DataSeries(String name, Collection<M3DataPoint> points) {
        this(name);
        if (points != null) {
            this.data.addAll(points);
        }
    }

    /**
     * Creates a series with a custom accent color and points.
     *
     * @param name series name
     * @param color series color, or {@code null} to use automatic theme role
     * @param points initial points
     */
    public M3DataSeries(String name, Color color, Collection<M3DataPoint> points) {
        this(name, points);
        setColor(color);
    }

    /**
     * Convenience factory for an array of numeric values.
     *
     * @param name series name
     * @param values double values
     * @return new data series
     */
    public static M3DataSeries of(String name, double... values) {
        M3DataSeries series = new M3DataSeries(name);
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                series.getData().add(new M3DataPoint(String.valueOf(i + 1), values[i]));
            }
        }
        return series;
    }

    /**
     * Convenience factory with custom color and values.
     *
     * @param name series name
     * @param color accent color
     * @param values double values
     * @return new data series
     */
    public static M3DataSeries of(String name, Color color, double... values) {
        M3DataSeries series = of(name, values);
        series.setColor(color);
        return series;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name == null ? "" : name);
    }

    public ObjectProperty<Color> colorProperty() {
        return color;
    }

    public Color getColor() {
        return color.get();
    }

    public void setColor(Color color) {
        this.color.set(color);
    }

    public ObservableList<M3DataPoint> getData() {
        return data;
    }

    @Override
    public String toString() {
        return "M3DataSeries[name='" + getName() + "', points=" + data.size() + "]";
    }
}

