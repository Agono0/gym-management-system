package io.m3fx.controls.slider;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.layout.HBox;

/**
 * M3 range slider: two thumbs selecting a low/high interval.
 *
 * <p>See <a href="https://m3.material.io/components/sliders/overview">M3 sliders</a>. Implemented
 * as a composite of two linked {@link M3Slider} thumbs that cannot cross; styling matches the
 * single slider (active track between thumbs via CSS on the container state).
 */
public class M3RangeSlider extends HBox {

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
        lowSlider.valueProperty().bindBidirectional(lowValue);
        highSlider.valueProperty().bindBidirectional(highValue);
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
