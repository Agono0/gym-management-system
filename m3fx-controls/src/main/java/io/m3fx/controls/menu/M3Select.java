package io.m3fx.controls.menu;

import io.m3fx.controls.icon.M3Icon;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

/**
 * M3 select (dropdown): a filled text-field-styled control opening a menu of options.
 *
 * <p>See <a href="https://m3.material.io/components/menus/overview">M3 menus</a> (exposed dropdown
 * menu). Backed by {@link ComboBox} for keyboard support (arrows/type-ahead/ESC), restyled to the
 * M3 filled select tokens. The dropdown list marks the current value with a leading check.
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
        setCellFactory(lv -> new ListCell<>() {
            private final Node check = M3Icon.symbol("check");

            {
                // Re-evaluate while the popup is open if the value changes underneath the list.
                valueProperty().addListener((obs, o, v) -> refreshCheck());
            }

            private void refreshCheck() {
                T item = getItem();
                setGraphic(!isEmpty() && item != null && item.equals(getValue()) ? check : null);
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
                refreshCheck();
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
