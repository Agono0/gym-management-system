package io.m3fx.controls.common;

import javafx.scene.control.Separator;

/**
 * M3 divider: a thin horizontal or vertical rule.
 *
 * <p>See <a href="https://m3.material.io/components/divider/overview">M3 divider</a>. Thickness
 * 1dp, outline-variant color, optional middle/start/end insets.
 */
public class M3Divider extends Separator {

    /** Creates a horizontal divider. */
    public M3Divider() {
        getStyleClass().add("m3-divider");
        setFocusTraversable(false);
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
