package io.m3fx.controls.menu;

import io.m3fx.controls.internal.MotionUtil;
import io.m3fx.core.token.MotionTokens;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.stage.Popup;

/**
 * M3 menu shown in a {@link Popup} with keyboard navigation.
 *
 * <p>See <a href="https://m3.material.io/components/menus/overview">M3 menus</a>. Surface
 * container, extra-small shape (4dp), elevation level 2; items are label-large with leading/trailing
 * icon support via {@link #item(String, javafx.scene.Node, Runnable)}. Opens with a short
 * token-driven fade-and-rise; ESC closes and focus returns to the opener (JavaFX popup behavior).
 */
public class M3Menu {

    private final javafx.scene.control.ContextMenu popup = new javafx.scene.control.ContextMenu();

    /** Creates an empty menu. */
    public M3Menu() {
        popup.getStyleClass().add("m3-menu");
    }

    /**
     * Adds a text item.
     *
     * @param text item text
     * @param onSelect selection handler, may be {@code null}
     * @return this menu, for chaining
     */
    public M3Menu item(String text, Runnable onSelect) {
        return item(text, null, onSelect);
    }

    /**
     * Adds an item with a leading icon.
     *
     * @param text item text
     * @param icon leading icon, may be {@code null}
     * @param onSelect selection handler, may be {@code null}
     * @return this menu, for chaining
     */
    public M3Menu item(String text, javafx.scene.Node icon, Runnable onSelect) {
        MenuItem menuItem = new MenuItem(text == null ? "" : text, icon);
        menuItem.getStyleClass().add("m3-menu-item");
        if (onSelect != null) {
            menuItem.setOnAction(e -> onSelect.run());
        }
        popup.getItems().add(menuItem);
        return this;
    }

    /**
     * Adds a custom node item.
     *
     * @param content custom content
     * @return this menu, for chaining
     */
    public M3Menu customItem(javafx.scene.Node content) {
        CustomMenuItem custom = new CustomMenuItem(content);
        custom.getStyleClass().add("m3-menu-item");
        popup.getItems().add(custom);
        return this;
    }

    /**
     * Adds a divider between items.
     *
     * @return this menu, for chaining
     */
    public M3Menu divider() {
        popup.getItems().add(new SeparatorMenuItem());
        return this;
    }

    /**
     * Adds a cascading submenu (spec cascading menu support).
     *
     * @param text submenu label
     * @param submenu the menu to open from this item; its current items are moved into the submenu
     * @return this menu, for chaining
     */
    public M3Menu submenu(String text, M3Menu submenu) {
        javafx.scene.control.Menu menu =
                new javafx.scene.control.Menu(text == null ? "" : text);
        menu.getStyleClass().add("m3-menu-item");
        if (submenu != null) {
            menu.getItems().addAll(submenu.popup().getItems());
            submenu.popup().getItems().clear();
        }
        popup.getItems().add(menu);
        return this;
    }

    /**
     * Shows the menu anchored to a node.
     *
     * @param owner the anchor node
     * @param screenX screen x
     * @param screenY screen y
     */
    public void show(javafx.scene.Node owner, double screenX, double screenY) {
        popup.show(owner, screenX, screenY);
        animateOpen();
    }

    /**
     * Shows the menu below its anchor node.
     *
     * @param owner the anchor node
     */
    public void showBelow(javafx.scene.Node owner) {
        javafx.geometry.Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
        if (bounds != null) {
            popup.show(owner, bounds.getMinX(), bounds.getMaxY() + 4);
        } else {
            popup.show(owner, 0, 0);
        }
        animateOpen();
    }

    /**
     * Short token-driven open transition (fade + slight rise) on the popup's root so the menu
     * settles instead of popping. Uses the shared slide-in helper; a menu hidden mid-animation
     * simply discards its detached scene, so no cleanup is required.
     */
    private void animateOpen() {
        javafx.scene.Scene scene = popup.getScene();
        if (scene != null && scene.getRoot() != null) {
            MotionUtil.slideIn(scene.getRoot(), 8, MotionTokens.DURATION_SHORT_3);
        }
    }

    /** Hides the menu. */
    public void hide() {
        popup.hide();
    }

    /**
     * Returns {@code true} while the popup is showing.
     *
     * @return whether showing
     */
    public boolean isShowing() {
        return popup.isShowing();
    }

    /** Returns the underlying popup (for advanced anchoring). */
    public javafx.scene.control.ContextMenu popup() {
        return popup;
    }
}
