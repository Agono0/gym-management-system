package io.m3fx.controls.tabs;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * M3 tabs: primary and secondary variants with an active indicator.
 *
 * <p>See <a href="https://m3.material.io/components/tabs/overview">M3 tabs</a>. Primary tabs use
 * title-small active text with a 3dp primary indicator; secondary tabs are more compact.
 * Keyboard navigation (arrows/Home/End) comes from {@link TabPane}.
 */
public class M3Tabs extends TabPane {

    private final ObjectProperty<TabVariant> variant =
            new SimpleObjectProperty<>(this, "variant", TabVariant.PRIMARY);

    /** Creates primary tabs. */
    public M3Tabs() {
        this(TabVariant.PRIMARY);
    }

    /**
     * Creates tabs with a variant.
     *
     * @param variant the tab variant
     * @param tabs initial tabs
     */
    public M3Tabs(TabVariant variant, Tab... tabs) {
        getStyleClass().add("m3-tabs");
        if (tabs != null) {
            getTabs().addAll(tabs);
        }
        setVariant(variant);
        variantProperty().addListener((obs, oldV, newV) -> updateStyleClass(oldV, newV));
        setFocusTraversable(true);
        // Token container height: 48px for both primary and secondary tabs.
        setTabMinHeight(48);
    }

    /** Returns the variant property. */
    public ObjectProperty<TabVariant> variantProperty() {
        return variant;
    }

    /** Returns the variant. */
    public TabVariant getVariant() {
        return variant.get();
    }

    /**
     * Sets the variant.
     *
     * @param variant the new variant
     */
    public void setVariant(TabVariant variant) {
        TabVariant old = this.variant.get();
        this.variant.set(variant == null ? TabVariant.PRIMARY : variant);
        updateStyleClass(old, this.variant.get());
    }

    private void updateStyleClass(TabVariant oldV, TabVariant newV) {
        if (oldV != null) {
            getStyleClass().remove("m3-tabs-" + oldV.name().toLowerCase());
        }
        if (newV != null && !getStyleClass().contains("m3-tabs-" + newV.name().toLowerCase())) {
            getStyleClass().add("m3-tabs-" + newV.name().toLowerCase());
        }
    }
}
