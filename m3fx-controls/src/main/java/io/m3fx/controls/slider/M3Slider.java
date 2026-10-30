package io.m3fx.controls.slider;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

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
    private final StringProperty accessibleLabel =
            new SimpleStringProperty(this, "accessibleLabel", "");

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
        // Announce the current value so screen readers track drag and keyboard changes.
        valueProperty().addListener((obs, oldV, newV) -> updateAccessibleText());
        accessibleLabel.addListener((obs, oldV, newV) -> updateAccessibleText());
        updateAccessibleText();
        // Shift+arrows step finely (a tenth of the block increment), complementing the native
        // arrows (block step), Home/End (min/max) and PageUp/PageDown.
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (!e.isShiftDown() || isDisabled()) {
                return;
            }
            boolean increase = e.getCode() == KeyCode.RIGHT || e.getCode() == KeyCode.UP;
            boolean decrease = e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.DOWN;
            if (!increase && !decrease) {
                return;
            }
            double span = getMax() - getMin();
            double fine =
                    getBlockIncrement() > 0 ? getBlockIncrement() / 10 : (span > 0 ? span / 100 : 1);
            double target = getValue() + (increase ? fine : -fine);
            setValue(Math.clamp(target, getMin(), getMax()));
            e.consume();
        });
    }

    /** Keeps the announced text in sync with the current value. */
    private void updateAccessibleText() {
        double value = getValue();
        String rendered =
                !Double.isInfinite(value) && value == Math.rint(value)
                        ? Long.toString((long) value)
                        : Double.toString(value);
        String label = accessibleLabel.get();
        setAccessibleText((label == null || label.isEmpty() ? "Value" : label) + ": " + rendered);
    }

    /** Returns the accessible label property (announced before the value; e.g. "Low value"). */
    public StringProperty accessibleLabelProperty() {
        return accessibleLabel;
    }

    /** Returns the accessible label (empty uses the default "Value"). */
    public String getAccessibleLabel() {
        return accessibleLabel.get();
    }

    /**
     * Sets the accessible label announced before the value (e.g. {@code "Low value"} for range
     * slider thumbs).
     *
     * @param label the label, may be {@code null} to use the default
     */
    public void setAccessibleLabel(String label) {
        accessibleLabel.set(label == null ? "" : label);
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
