package io.m3fx.controls.picker;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import java.time.LocalTime;

/**
 * M3 time picker: dial-free input with hour/minute spinners and an AM/PM toggle.
 *
 * <p>See <a href="https://m3.material.io/components/time-pickers/overview">M3 time pickers</a>.
 * Surface container high dial in the full spec; this control implements the text-input mode (hour
 * and minute fields plus AM/PM selector) which the spec supports for keyboard entry.
 */
public class M3TimePicker extends HBox {

    private final ObjectProperty<LocalTime> value =
            new SimpleObjectProperty<>(this, "value", LocalTime.now().withSecond(0).withNano(0));
    private final BooleanProperty twentyFourHour =
            new SimpleBooleanProperty(this, "twentyFourHour", false);

    private final Spinner<Integer> hourSpinner = new Spinner<>(1, 12, 12);
    private final Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, 0);
    private final ToggleButton amPmToggle = new ToggleButton("AM");

    /** Creates a time picker set to now. */
    public M3TimePicker() {
        this(LocalTime.now().withSecond(0).withNano(0));
    }

    /**
     * Creates a time picker.
     *
     * @param value initial time
     */
    public M3TimePicker(LocalTime value) {
        getStyleClass().add("m3-time-picker");
        hourSpinner.getStyleClass().add("m3-time-spinner");
        minuteSpinner.getStyleClass().add("m3-time-spinner");
        amPmToggle.getStyleClass().add("m3-time-ampm");
        hourSpinner.setEditable(true);
        minuteSpinner.setEditable(true);
        Label colon = new Label(":");
        colon.getStyleClass().add("m3-time-colon");
        setAlignment(Pos.CENTER);
        setSpacing(8);
        getChildren().addAll(hourSpinner, colon, minuteSpinner, amPmToggle);
        hourSpinner.setPrefWidth(80);
        minuteSpinner.setPrefWidth(80);
        setValue(value == null ? LocalTime.now().withSecond(0).withNano(0) : value);
        hourSpinner.valueProperty().addListener((obs, oldV, newV) -> pushToValue());
        minuteSpinner.valueProperty().addListener((obs, oldV, newV) -> pushToValue());
        amPmToggle.setOnAction(e -> {
            amPmToggle.setText("AM".equals(amPmToggle.getText()) ? "PM" : "AM");
            pushToValue();
        });
        valueProperty().addListener((obs, oldV, newV) -> pullFromValue());
        twentyFourHourProperty().addListener((obs, oldV, h24) -> {
            amPmToggle.setVisible(!h24);
            amPmToggle.setManaged(!h24);
            if (h24) {
                hourSpinner.setValueFactory(
                        new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(
                                0, 23, getValue() == null ? 0 : getValue().getHour()));
            } else {
                int h12 = getValue() == null ? 12 : (getValue().getHour() % 12 == 0 ? 12 : getValue().getHour() % 12);
                hourSpinner.setValueFactory(
                        new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(
                                1, 12, h12));
            }
            pullFromValue();
        });
        setFocusTraversable(false);
        setMaxHeight(USE_PREF_SIZE);
    }

    /** Returns the time value property. */
    public ObjectProperty<LocalTime> valueProperty() {
        return value;
    }

    /** Returns the time value. */
    public LocalTime getValue() {
        return value.get();
    }

    /**
     * Sets the time value.
     *
     * @param value the new time
     */
    public void setValue(LocalTime value) {
        this.value.set(value == null ? LocalTime.MIDNIGHT : value.withSecond(0).withNano(0));
        pullFromValue();
    }

    /** Returns the 24-hour mode property. */
    public BooleanProperty twentyFourHourProperty() {
        return twentyFourHour;
    }

    /** Returns whether 24-hour mode is on. */
    public boolean isTwentyFourHour() {
        return twentyFourHour.get();
    }

    /**
     * Sets 24-hour mode (hides the AM/PM toggle).
     *
     * @param twentyFourHour {@code true} for 24-hour mode
     */
    public void setTwentyFourHour(boolean twentyFourHour) {
        this.twentyFourHour.set(twentyFourHour);
    }

    private void pushToValue() {
        int hour = hourSpinner.getValue() == null ? (isTwentyFourHour() ? 0 : 12) : hourSpinner.getValue();
        int minute = minuteSpinner.getValue() == null ? 0 : minuteSpinner.getValue();
        if (isTwentyFourHour()) {
            hour = Math.min(23, Math.max(0, hour));
        } else {
            boolean isPm = "PM".equalsIgnoreCase(amPmToggle.getText());
            hour = (hour % 12) + (isPm ? 12 : 0);
        }
        value.set(LocalTime.of(hour % 24, Math.min(59, Math.max(0, minute))));
    }

    private void pullFromValue() {
        LocalTime time = getValue() == null ? LocalTime.MIDNIGHT : getValue();
        if (isTwentyFourHour()) {
            if (hourSpinner.getValueFactory() != null) {
                hourSpinner.getValueFactory().setValue(time.getHour());
            }
        } else {
            int displayHour = time.getHour() % 12 == 0 ? 12 : time.getHour() % 12;
            if (hourSpinner.getValueFactory() != null) {
                hourSpinner.getValueFactory().setValue(displayHour);
            }
            amPmToggle.setText(time.getHour() < 12 ? "AM" : "PM");
        }
        if (minuteSpinner.getValueFactory() != null) {
            minuteSpinner.getValueFactory().setValue(time.getMinute());
        }
    }
}
