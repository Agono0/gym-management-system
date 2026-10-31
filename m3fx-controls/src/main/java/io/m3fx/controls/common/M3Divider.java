package io.m3fx.controls.common;

import javafx.scene.control.Separator;

/**
 * M3 divider: a thin horizontal or vertical rule.
 *
 * <p>See <a href="https://m3.material.io/components/divider/overview">M3 divider</a>. Thickness
 * 1dp, outline-variant color, optional middle/start/end insets.
 */
// Suppressed this-escape (false positives javac 27 cannot see across the module boundary):
// (1) the JavaFX property-bean idiom new Simple*Property(this, ...) - the property
// constructor only stores the bean reference, exactly like every built-in control;
// (2) constructor calls to JavaFX final methods (getStyleClass(), setAccessibleRole())
// - final, but javac does not honor ACC_FINAL for symbols from another module. This
// class's own overridable methods are never called during construction.
@SuppressWarnings("this-escape")
public class M3Divider extends Separator {

    /** Creates a horizontal divider. */
    public M3Divider() {
        this(javafx.geometry.Orientation.HORIZONTAL);
    }

    /**
     * Creates a divider with a specific orientation.
     *
     * @param orientation horizontal or vertical
     */
    public M3Divider(javafx.geometry.Orientation orientation) {
        getStyleClass().add("m3-divider");
        setFocusTraversable(false);
        setOrientation(orientation);
        // Lets the stylesheet style the vertical line separately (.m3-divider.vertical .line).
        orientationProperty().addListener((obs, oldV, newV) ->
                syncOrientationClass(newV == javafx.geometry.Orientation.VERTICAL));
        syncOrientationClass(orientation == javafx.geometry.Orientation.VERTICAL);
    }

    private void syncOrientationClass(boolean vertical) {
        if (vertical) {
            if (!getStyleClass().contains("vertical")) {
                getStyleClass().add("vertical");
            }
        } else {
            getStyleClass().remove("vertical");
        }
    }

    /**
     * Creates a divider with insets.
     *
     * @param startInset start inset in px
     * @param endInset end inset in px
     * @return the divider
     */
    public static M3Divider withInsets(double startInset, double endInset) {
        M3Divider divider = new M3Divider();
        divider.setStyle(
                "-fx-padding: 0 " + endInset + "px 0 " + startInset + "px;");
        return divider;
    }
}
