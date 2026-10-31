package io.m3fx.controls.picker;

import javafx.scene.control.DatePicker;
import java.time.LocalDate;

/**
 * M3 date picker: calendar input in filled and outlined field styles.
 *
 * <p>See <a href="https://m3.material.io/components/date-pickers/overview">M3 date pickers</a>.
 * The input field follows {@code M3TextField} styling; the popup calendar uses M3 dialog tokens
 * (surface container high). The calendar-grid layout is covered by {@link M3DateDialog} and its
 * {@code M3Calendar} month grid (same package).
 */
// Suppressed this-escape (false positives javac 27 cannot see across the module boundary):
// (1) the JavaFX property-bean idiom new Simple*Property(this, ...) - the property
// constructor only stores the bean reference, exactly like every built-in control;
// (2) constructor calls to JavaFX final methods (getStyleClass(), setAccessibleRole())
// - final, but javac does not honor ACC_FINAL for symbols from another module. This
// class's own overridable methods are never called during construction.
@SuppressWarnings("this-escape")
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
        if (getEditor() != null) {
            getEditor().focusedProperty().addListener((obs, oldV, newV) -> refreshFocus.run());
        }
        refreshFocus.run();
    }
}
