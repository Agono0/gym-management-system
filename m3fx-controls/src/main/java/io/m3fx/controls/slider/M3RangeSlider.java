package io.m3fx.controls.slider;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.layout.HBox;

/**
 * M3 range slider: two thumbs selecting a low/high interval.
 *
 * <p>See <a href="https://m3.material.io/components/sliders/overview">M3 sliders</a>. Implemented
 * as a composite of two linked {@link M3Slider} thumbs that cannot cross; styling matches the
 * single slider (active track between thumbs via CSS on the container state). The {@code min} and
 * {@code max} properties mirror the range bounds of both thumbs (FXML-friendly, matching the
 * {@code M3Slider} attribute set).
 */
public class M3RangeSlider extends HBox {

    private final DoubleProperty min = new SimpleDoubleProperty(this, "min", 0);
    private final DoubleProperty max = new SimpleDoubleProperty(this, "max", 100);
    private final DoubleProperty lowValue = new SimpleDoubleProperty(this, "lowValue", 25);
    private final DoubleProperty highValue = new SimpleDoubleProperty(this, "highValue", 75);

    private final M3Slider lowSlider = new M3Slider();
    private final M3Slider highSlider = new M3Slider();

    /** Creates a range slider 0-100 with low=25, high=75. */
    public M3RangeSlider() {
        this(0, 100, 25, 75);
    }

    /**
     * Creates a range slider.
     *
     * @param min minimum value
     * @param max maximum value
     * @param low initial low value
     * @param high initial high value
     */
    public M3RangeSlider(double min, double max, double low, double high) {
        getStyleClass().add("m3-range-slider");
        lowSlider.setMin(min);
        lowSlider.setMax(max);
        highSlider.setMin(min);
        highSlider.setMax(max);
        setMin(min);
        setMax(max);
        lowSlider.valueProperty().bindBidirectional(lowValue);
        highSlider.valueProperty().bindBidirectional(highValue);
        // Name each thumb so screen readers announce which one is being adjusted.
        lowSlider.setAccessibleLabel("Low value");
        highSlider.setAccessibleLabel("High value");
        lowValue.addListener((obs, oldV, newV) -> {
            if (newV.doubleValue() > getHighValue()) {
                setLowValue(getHighValue());
            }
        });
        highValue.addListener((obs, oldV, newV) -> {
            if (newV.doubleValue() < getLowValue()) {
                setHighValue(getLowValue());
            }
        });
        setLowValue(Math.min(low, high));
        setHighValue(Math.max(low, high));
        getChildren().addAll(lowSlider, highSlider);
        setSpacing(8);
    }

    /** Returns the minimum value property (range lower bound of both thumbs). */
    public DoubleProperty minProperty() {
        return min;
    }

    /** Returns the minimum value. */
    public double getMin() {
        return min.get();
    }

    /**
     * Sets the minimum value for both thumbs.
     *
     * @param min the minimum value
     */
    public void setMin(double min) {
        this.min.set(min);
        lowSlider.setMin(min);
        highSlider.setMin(min);
    }

    /** Returns the maximum value property (range upper bound of both thumbs). */
    public DoubleProperty maxProperty() {
        return max;
    }

    /** Returns the maximum value. */
    public double getMax() {
        return max.get();
    }

    /**
     * Sets the maximum value for both thumbs.
     *
     * @param max the maximum value
     */
    public void setMax(double max) {
        this.max.set(max);
        lowSlider.setMax(max);
        highSlider.setMax(max);
    }

    /** Returns the low value property. */
    public DoubleProperty lowValueProperty() {
        return lowValue;
    }

    /** Returns the low value. */
    public double getLowValue() {
        return lowValue.get();
    }

    /**
     * Sets the low value (clamped to high).
     *
     * @param lowValue the low value
     */
    public void setLowValue(double lowValue) {
        this.lowValue.set(Math.min(lowValue, getHighValue()));
    }

    /** Returns the high value property. */
    public DoubleProperty highValueProperty() {
        return highValue;
    }

    /** Returns the high value. */
    public double getHighValue() {
        return highValue.get();
    }

    /**
     * Sets the high value (clamped to low).
     *
     * @param highValue the high value
     */
    public void setHighValue(double highValue) {
        this.highValue.set(Math.max(highValue, getLowValue()));
    }
}
