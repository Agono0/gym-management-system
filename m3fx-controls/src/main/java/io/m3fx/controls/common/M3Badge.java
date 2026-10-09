package io.m3fx.controls.common;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Label;

/**
 * M3 badge: a small error-colored count or dot anchored to its parent (typically an icon).
 *
 * <p>See <a href="https://m3.material.io/components/badges/overview">M3 badges</a>. Size 16dp for
 * numbers (6dp dot when the count is zero and {@code showDotWhenZero} behavior applies); label-small
 * type in on-error on error.
 */
public class M3Badge extends Label {

    private final IntegerProperty count = new SimpleIntegerProperty(this, "count", 0);

    /** Creates an empty (dot) badge. */
    public M3Badge() {
        this(0);
    }

    /**
     * Creates a badge with a count.
     *
     * @param count notification count (0 renders a dot)
     */
    public M3Badge(int count) {
        getStyleClass().add("m3-badge");
        setCount(count);
        countProperty().addListener((obs, oldV, newV) -> refreshText());
        setMouseTransparent(true);
        setFocusTraversable(false);
        refreshText();
    }

    /** Returns the count property. */
    public IntegerProperty countProperty() {
        return count;
    }

    /** Returns the count. */
    public int getCount() {
        return count.get();
    }

    /**
     * Sets the count; values above 999 render as {@code 999+}.
     *
     * @param count the new count
     */
    public void setCount(int count) {
        this.count.set(Math.max(0, count));
    }

    private void refreshText() {
        int value = getCount();
        if (value <= 0) {
            setText("");
            pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("dot"), true);
        } else {
            pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("dot"), false);
            setText(value > 999 ? "999+" : String.valueOf(value));
        }
        setAccessibleText(value <= 0 ? "New notification" : value + " new notifications");
    }
}
