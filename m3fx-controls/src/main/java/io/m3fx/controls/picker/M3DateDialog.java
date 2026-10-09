package io.m3fx.controls.picker;

import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.internal.OverlayLayer;
import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * M3 date picker dialog: an {@link M3Calendar} month grid with Cancel/OK actions, shown in an
 * {@link OverlayLayer}.
 *
 * <p>See <a href="https://m3.material.io/components/date-pickers/overview">M3 date pickers</a>.
 * Surface-container-high container, 28dp radius, headline + weekday + day cells (selected day is a
 * primary circle), text-button confirmation row.
 */
public class M3DateDialog extends M3Dialog {

    private final M3Calendar calendar = new M3Calendar();

    /** Creates a dialog with today selected. */
    public M3DateDialog() {
        this(LocalDate.now());
    }

    /**
     * Creates a dialog with an initial date.
     *
     * @param initial initially selected date, may be {@code null}
     */
    public M3DateDialog(LocalDate initial) {
        getStyleClass().add("m3-date-dialog");
        setTitle("Select date");
        calendar.setSelectedDate(initial == null ? LocalDate.now() : initial);
        setContent(calendar);
    }

    /** Returns the calendar grid (for styling or pre-selecting). */
    public M3Calendar getCalendar() {
        return calendar;
    }

    /** Returns the currently selected date. */
    public LocalDate getSelectedDate() {
        return calendar.getSelectedDate();
    }

    /**
     * Shows the dialog and reports the confirmed date.
     *
     * @param layer the overlay layer
     * @param onConfirm receives the selected date on OK (never {@code null} date), may be
     *     {@code null} for no callback
     * @return the dialog
     */
    public static M3DateDialog showIn(OverlayLayer layer, Consumer<LocalDate> onConfirm) {
        return showIn(layer, LocalDate.now(), onConfirm);
    }

    /**
     * Shows the dialog with an initial date and reports the confirmed date.
     *
     * @param layer the overlay layer
     * @param initial initially selected date
     * @param onConfirm receives the selected date on OK, may be {@code null}
     * @return the dialog
     */
    public static M3DateDialog showIn(
            OverlayLayer layer, LocalDate initial, Consumer<LocalDate> onConfirm) {
        M3DateDialog dialog = new M3DateDialog(initial);
        M3Button cancel = M3Button.text("Cancel");
        M3Button ok = M3Button.text("OK");
        cancel.setOnAction(e -> dialog.hide());
        ok.setOnAction(e -> {
            dialog.hide();
            if (onConfirm != null && dialog.getSelectedDate() != null) {
                onConfirm.accept(dialog.getSelectedDate());
            }
        });
        dialog.getActionsBox().getChildren().addAll(cancel, ok);
        dialog.showIn(layer);
        return dialog;
    }
}
