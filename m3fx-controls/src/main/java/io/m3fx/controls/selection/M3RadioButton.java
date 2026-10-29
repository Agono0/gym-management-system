package io.m3fx.controls.selection;

import javafx.scene.control.RadioButton;

/**
 * M3 radio button with all states (enabled, hovered, focused, pressed, disabled, error).
 *
 * <p>See <a href="https://m3.material.io/components/radio-button/overview">M3 radio button</a>.
 * Container 20dp circle, selected dot uses primary, 48dp minimum touch target.
 */
public class M3RadioButton extends RadioButton {

    /** Minimum touch target in px (spec: 48dp). */
    public static final double TOUCH_TARGET = 48;

    /** Creates an unselected radio button. */
    public M3RadioButton() {
        this("");
    }

    /**
     * Creates a radio button with text.
     *
     * @param text label text
     */
    public M3RadioButton(String text) {
        super(text == null ? "" : text);
        getStyleClass().add("m3-radio");
        setMinHeight(TOUCH_TARGET);
        setFocusTraversable(true);
    }

    /**
     * Creates a selected radio button.
     *
     * @param text label text
     * @param selected initial state
     * @return the radio button
     */
    public static M3RadioButton selected(String text, boolean selected) {
        M3RadioButton radio = new M3RadioButton(text);
        radio.setSelected(selected);
        return radio;
    }
}
