package io.m3fx.controls.chart;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

import java.util.Collection;

/**
 * A named series of scatter/bubble data points with custom accent color and trendline toggle.
 */
public class M3ScatterSeries {

    private final StringProperty name = new SimpleStringProperty(this, "name", "");
    private final ObjectProperty<Color> color = new SimpleObjectProperty<>(this, "color", null);
    private final BooleanProperty showTrendline = new SimpleBooleanProperty(this, "showTrendline", true);
    private final ObservableList<M3ScatterPoint> data = FXCollections.observableArrayList();

    /**
     * Creates an unnamed series.
     *
     * @param name series name (legend/tooltip)
     */
    public M3ScatterSeries(String name) {
        setName(name);
    }

    /**
     * Creates a colored series.
     *
     * @param name series name
     * @param color accent color, or {@code null} for the automatic theme palette
     */
    public M3ScatterSeries(String name, Color color) {
        this(name);
        setColor(color);
    }

    /**
     * Creates a series with points.
     *
     * @param name series name
     * @param points initial points
     */
    public M3ScatterSeries(String name, Collection<M3ScatterPoint> points) {
        this(name);
        if (points != null) {
            this.data.addAll(points);
        }
    }

    /**
     * Creates a colored series with points.
     *
     * @param name series name
     * @param color accent color, or {@code null} for the automatic theme palette
     * @param points initial points
     */
    public M3ScatterSeries(String name, Color color, Collection<M3ScatterPoint> points) {
        this(name, points);
        setColor(color);
    }

    /** The series name (legend/tooltip). */
    public StringProperty nameProperty() {
        return name;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name == null ? "" : name);
    }

    /** The series accent color, or {@code null} to use the automatic theme palette. */
    public ObjectProperty<Color> colorProperty() {
        return color;
    }

    public Color getColor() {
        return color.get();
    }

    public void setColor(Color color) {
        this.color.set(color);
    }

    /** Whether this series draws its OLS regression trendline. */
    public BooleanProperty showTrendlineProperty() {
        return showTrendline;
    }

    public boolean isShowTrendline() {
        return showTrendline.get();
    }

    public void setShowTrendline(boolean show) {
        this.showTrendline.set(show);
    }

    /** The observable list of points in this series. */
    public ObservableList<M3ScatterPoint> getData() {
        return data;
    }

    /** Alias for {@link #getData()}. */
    public ObservableList<M3ScatterPoint> getPoints() {
        return data;
    }
}
