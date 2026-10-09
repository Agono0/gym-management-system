package io.m3fx.controls.picker;

import javafx.scene.control.DatePicker;
import java.time.LocalDate;

/**
 * M3 date picker: calendar input in filled and outlined field styles.
 *
 * <p>See <a href="https://m3.material.io/components/date-pickers/overview">M3 date pickers</a>.
 * The input field follows {@code M3TextField} styling; the popup calendar uses M3 dialog tokens
 * (surface container high). Docked/modal picker containers are covered by {@code M3Dialog} docs in
 * COVERAGE.md as planned work for the full calendar grid skin.
 */
public class M3DatePicker extends DatePicker {

    /** Creates an empty date picker. */
    public M3DatePicker() {
        this(null);
    }

    /**
     * Creates a date picker with a value.
     *
     * @param value initial date, may be {@code null}
     */
    public M3DatePicker(LocalDate value) {
        super(value);
        getStyleClass().add("m3-date-picker");
        setFocusTraversable(true);
        // Don't stretch to row height when placed in an HBox with taller siblings.
        setMaxHeight(USE_PREF_SIZE);
        Runnable refreshFocus = () ->
                pseudoClassStateChanged(
                        javafx.css.PseudoClass.getPseudoClass("input-focused"),
                        isFocused() || (getEditor() != null && getEditor().isFocused()));
        focusedProperty().addListener((obs, oldV, newV) -> refreshFocus.run());
        getEditor().focusedProperty().addListener((obs, oldV, newV) -> refreshFocus.run());
        refreshFocus.run();
    }
}
