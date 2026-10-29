package io.m3fx.controls.slider;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Slider;

/**
 * M3 slider: continuous and discrete (step) modes with 48dp touch handling.
 *
 * <p>See <a href="https://m3.material.io/components/sliders/overview">M3 sliders</a>. The updated
 * M3 Expressive slider (androidx {@code SliderTokens}) uses a 16dp active track, a 4dp inactive
 * track, a 4x44dp handle and 4dp stop indicators; the baseline material-web v0_192 tokens define
 * 4dp tracks with a 20dp round handle. The handle dimensions come from the CSS (4x44 handle),
 * so both generations render. Discrete mode snaps to {@code majorTickUnit} steps and styles the
 * axis ticks as stop indicators via the {@code .axis} CSS hooks.
 */
public class M3Slider extends Slider {

    /** Minimum touch target in px (spec: 48dp). */
    public static final double TOUCH_TARGET = 48;

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
        setMinHeight(TOUCH_TARGET);
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
        // Arrow keys move one step, matching the snapped drag behavior.
        if (step > 0) {
            slider.setBlockIncrement(step);
        }
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
