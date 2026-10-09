package io.m3fx.controls.nav;

import io.m3fx.controls.icon.M3Icon;
import java.util.function.Supplier;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;

/**
 * One destination in an M3 navigation bar, rail or drawer.
 *
 * <p>Icons are {@link Supplier}s (not shared nodes) because a JavaFX node can only have one
 * parent: the same destination list can feed a bar, a rail and a drawer at once, and the adaptive
 * scaffold rebuilds navigation on every size-class change. Each build calls the suppliers for
 * fresh icon instances. The selected icon swaps in automatically when the destination is active.
 */
public record M3NavDestination(String label, Supplier<Node> icon, Supplier<Node> selectedIcon) {

    /**
     * Creates a destination with catalog icons for both states.
     *
     * @param label label text
     * @param icon catalog name (see {@code M3Icons}) used unselected
     * @param selectedIcon catalog name used when selected
     * @return the destination
     */
    public static M3NavDestination of(String label, String icon, String selectedIcon) {
        return new M3NavDestination(
                label,
                () -> M3Icon.symbol(icon),
                selectedIcon == null ? null : () -> M3Icon.symbol(selectedIcon));
    }

    /**
     * Creates a destination with one catalog icon for both states.
     *
     * @param label label text
     * @param icon catalog name (see {@code M3Icons})
     * @return the destination
     */
    public static M3NavDestination of(String label, String icon) {
        return of(label, icon, null);
    }

    /**
     * Creates a destination with a fixed node (single-parent: use only in one nav component).
     *
     * @param label label text
     * @param icon icon node
     * @return the destination
     */
    public static M3NavDestination ofNode(String label, Node icon) {
        return new M3NavDestination(label, () -> icon, null);
    }

    /** Label text property helper for bindings (returns a snapshot property). */
    public StringProperty labelProperty() {
        return new SimpleStringProperty(label);
    }
}
