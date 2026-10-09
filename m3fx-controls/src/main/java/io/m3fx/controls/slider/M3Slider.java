package io.m3fx.controls.slider;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Slider;

/**
 * M3 slider: continuous and discrete (step) modes with 48dp touch handling.
 *
 * <p>See <a href="https://m3.material.io/components/sliders/overview">M3 sliders</a>. Active track
 * 4dp, inactive track 16dp... actually both 4dp visually with the inactive track on
 * surface-container-highest; thumb is a 4x44dp handle (20dp gap). Discrete mode snaps to
 * {@code majorTickUnit} steps and shows a value indicator via the label formatter hook.
 */
public class M3Slider extends Slider {

    private final BooleanProperty discrete = new SimpleBooleanProperty(this, "discrete", false);

    /** Creates a continuous slider 0-100. */
    public M3Slider() {
        this(0, 100, 50);
    }

    /**
     * Creates a slider with a range and value.
     *
     * @param min minimum value
     * @param max maximum value
     * @param value initial value
     */
    public M3Slider(double min, double max, double value) {
        super(min, max, value);
        getStyleClass().add("m3-slider");
        setFocusTraversable(true);
        setMinHeight(48);
        discreteProperty().addListener((obs, oldV, isDiscrete) -> {
            setSnapToTicks(isDiscrete);
            pseudoClassStateChanged(
                    javafx.css.PseudoClass.getPseudoClass("discrete"), isDiscrete);
        });
    }

    /**
     * Creates a discrete slider snapping to steps.
     *
     * @param min minimum value
     * @param max maximum value
     * @param value initial value
     * @param step step size
     * @return the slider
     */
    public static M3Slider discrete(double min, double max, double value, double step) {
        M3Slider slider = new M3Slider(min, max, value);
        slider.setMajorTickUnit(step);
        slider.setMinorTickCount(0);
        slider.setShowTickMarks(true);
        slider.setDiscrete(true);
        return slider;
    }

    /** Returns the discrete property. */
    public BooleanProperty discreteProperty() {
        return discrete;
    }

    /** Returns whether the slider snaps to steps. */
    public boolean isDiscrete() {
        return discrete.get();
    }

    /**
     * Sets discrete mode.
     *
     * @param discrete {@code true} to snap to ticks
     */
    public void setDiscrete(boolean discrete) {
        this.discrete.set(discrete);
    }

    /** Returns the value property (re-exposed for Javadoc clarity). */
    public DoubleProperty valuePropertyRef() {
        return valueProperty();
    }
}
