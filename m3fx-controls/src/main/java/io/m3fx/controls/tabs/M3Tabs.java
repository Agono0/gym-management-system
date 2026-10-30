package io.m3fx.controls.tabs;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * M3 tabs: primary and secondary variants with an active indicator.
 *
 * <p>See <a href="https://m3.material.io/components/tabs/overview">M3 tabs</a>. Both variants use a
 * 48dp container; primary tabs show a 3dp active indicator with label-large text, secondary tabs a
 * 2dp indicator with title-small text. Keyboard navigation (arrows/Home/End) comes from
 * {@link TabPane}.
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
        // TabPane already moves selection with left/right arrows; this filter adds the missing
        // pieces: arrows wrap at the ends and Home/End jump to the first/last tab.
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isConsumed() || isDisabled()) {
                return;
            }
            int count = getTabs().size();
            if (count < 2) {
                return;
            }
            int current = getSelectionModel().getSelectedIndex();
            int target;
            switch (e.getCode()) {
                case HOME -> target = 0;
                case END -> target = count - 1;
                case LEFT, KP_LEFT -> {
                    if (current <= 0) {
                        target = count - 1;
                    } else {
                        return;
                    }
                }
                case RIGHT, KP_RIGHT -> {
                    if (current >= count - 1) {
                        target = 0;
                    } else {
                        return;
                    }
                }
                default -> {
                    return;
                }
            }
            e.consume();
            selectEnabledTab(target);
        });
    }

    /**
     * Selects the nearest enabled tab at or after {@code startIndex} (wrapping) and moves keyboard
     * focus to its header, so the roving focus follows the selection.
     */
    private void selectEnabledTab(int startIndex) {
        int count = getTabs().size();
        for (int tried = 0; tried < count; tried++) {
            int index = Math.floorMod(startIndex + tried, count);
            Tab tab = getTabs().get(index);
            if (!tab.isDisabled()) {
                getSelectionModel().select(index);
                focusHeader(index);
                return;
            }
        }
    }

    /** Best-effort focus move to the tab header matching the tab order (no-op if not laid out). */
    private void focusHeader(int index) {
        List<Node> headers = new ArrayList<>(lookupAll(".headers-region .tab"));
        if (headers.size() == getTabs().size() && index < headers.size()) {
            headers.get(index).requestFocus();
        }
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
