package io.m3fx.controls.selection;

import javafx.scene.control.CheckBox;

/**
 * M3 checkbox with all states (enabled, hovered, focused, pressed, disabled, error, indeterminate).
 *
 * <p>See <a href="https://m3.material.io/components/checkbox/overview">M3 checkbox</a>. Container
 * 18dp (2dp corner radius), 48dp minimum touch target, selected container uses primary with
 * on-primary checkmark.
 */
// Suppressed this-escape (false positives javac 27 cannot see across the module boundary):
// (1) the JavaFX property-bean idiom new Simple*Property(this, ...) - the property
// constructor only stores the bean reference, exactly like every built-in control;
// (2) constructor calls to JavaFX final methods (getStyleClass(), setAccessibleRole())
// - final, but javac does not honor ACC_FINAL for symbols from another module. This
// class's own overridable methods are never called during construction.
@SuppressWarnings("this-escape")
public class M3CheckBox extends CheckBox {

    /** Minimum touch target in px (spec: 48dp). */
    public static final double TOUCH_TARGET = 48;

    /** Creates an unchecked checkbox. */
    public M3CheckBox() {
        this("");
    }

    /**
     * Creates an icon-only checkbox with initial state.
     *
     * @param selected initial state
     */
    public M3CheckBox(boolean selected) {
        this("");
        setSelected(selected);
    }

    /**
     * Creates a checkbox with text.
     *
     * @param text label text
     */
    public M3CheckBox(String text) {
        super(text == null ? "" : text);
        getStyleClass().add("m3-checkbox");
        setMinHeight(TOUCH_TARGET);
        setFocusTraversable(true);
    }

    /**
     * Creates a selected checkbox.
     *
     * @param text label text
     * @param selected initial state
     * @return the checkbox
     */
    public static M3CheckBox selected(String text, boolean selected) {
        M3CheckBox box = new M3CheckBox(text);
        box.setSelected(selected);
        return box;
    }
}
