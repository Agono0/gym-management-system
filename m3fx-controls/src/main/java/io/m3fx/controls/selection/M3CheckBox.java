package io.m3fx.controls.selection;

import javafx.scene.control.CheckBox;

/**
 * M3 checkbox with all states (enabled, hovered, focused, pressed, disabled, error, indeterminate).
 *
 * <p>See <a href="https://m3.material.io/components/checkbox/overview">M3 checkbox</a>. Container
 * 18dp, touch target 48dp, selected container uses primary with on-primary checkmark.
 */
public class M3CheckBox extends CheckBox {

    /** Creates an unchecked checkbox. */
    public M3CheckBox() {
        this("");
    }

    /**
     * Creates a checkbox with text.
     *
     * @param text label text
     */
    public M3CheckBox(String text) {
        super(text == null ? "" : text);
        getStyleClass().add("m3-checkbox");
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
