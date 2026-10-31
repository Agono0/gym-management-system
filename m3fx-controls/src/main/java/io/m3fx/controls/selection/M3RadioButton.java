package io.m3fx.controls.selection;

import javafx.scene.control.RadioButton;

/**
 * M3 radio button with all states (enabled, hovered, focused, pressed, disabled, error).
 *
 * <p>See <a href="https://m3.material.io/components/radio-button/overview">M3 radio button</a>.
 * Container 20dp circle, selected dot uses primary, 48dp minimum touch target.
 */
// Suppressed this-escape (false positives javac 27 cannot see across the module boundary):
// (1) the JavaFX property-bean idiom new Simple*Property(this, ...) - the property
// constructor only stores the bean reference, exactly like every built-in control;
// (2) constructor calls to JavaFX final methods (getStyleClass(), setAccessibleRole())
// - final, but javac does not honor ACC_FINAL for symbols from another module. This
// class's own overridable methods are never called during construction.
@SuppressWarnings("this-escape")
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
