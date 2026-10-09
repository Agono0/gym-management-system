package io.m3fx.controls.menu;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

/**
 * M3 select (dropdown): a filled text-field-styled control opening a menu of options.
 *
 * <p>See <a href="https://m3.material.io/components/menus/overview">M3 menus</a> (exposed dropdown
 * menu). Backed by {@link ComboBox} for keyboard support (arrows/type-ahead), restyled to the M3
 * filled select tokens.
 *
 * @param <T> option type
 */
public class M3Select<T> extends ComboBox<T> {

    /** Creates an empty select. */
    public M3Select() {
        this(FXCollections.observableArrayList());
    }

    /**
     * Creates a select with options.
     *
     * @param options the options
     */
    public M3Select(ObservableList<T> options) {
        super(options);
        getStyleClass().add("m3-select");
        setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? getPromptText() : item.toString());
            }
        });
        setFocusTraversable(true);
        setMaxHeight(USE_PREF_SIZE);
    }

    /**
     * Returns the selected value property (alias for {@link #valueProperty()}).
     *
     * @return the selected value property
     */
    public ObjectProperty<T> selectedValueProperty() {
        return valueProperty();
    }
}
