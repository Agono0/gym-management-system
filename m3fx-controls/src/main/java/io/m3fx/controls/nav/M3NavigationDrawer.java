package io.m3fx.controls.nav;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.util.List;

/**
 * M3 navigation drawer: modal and standard variants listing destinations with icons and badges.
 *
 * <p>See <a href="https://m3.material.io/components/navigation-drawer/overview">M3 navigation
 * drawer</a>. Standard drawers are always visible (360dp); modal drawers overlay content with a
 * scrim and are shown/hidden via {@link #setOpen(boolean)}.
 */
public class M3NavigationDrawer extends VBox {

    private final IntegerProperty selectedIndex = new SimpleIntegerProperty(this, "selectedIndex", 0);
    private final BooleanProperty modal = new SimpleBooleanProperty(this, "modal", false);
    private final BooleanProperty open = new SimpleBooleanProperty(this, "open", true);
    private final ToggleGroup group = new ToggleGroup();
    private final VBox items = new VBox(4);

    /** Creates a standard (always open) drawer. */
    public M3NavigationDrawer() {
        getStyleClass().add("m3-navigation-drawer");
        setMinWidth(280);
        setPrefWidth(320);
        setPadding(new Insets(12));
        setAlignment(Pos.TOP_LEFT);
        getChildren().add(items);
        modalProperty().addListener((obs, oldV, isModal) ->
                pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("modal"), isModal));
        openProperty().addListener((obs, oldV, isOpen) -> {
            setVisible(isOpen);
            setManaged(isOpen);
        });
        selectedIndex.addListener((obs, oldV, newV) -> selectInGroup(newV.intValue()));
    }

    /**
     * Creates a drawer with destinations.
     *
     * @param destinations destination list
     * @param modal {@code true} for a modal drawer
     */
    public M3NavigationDrawer(List<M3NavDestination> destinations, boolean modal) {
        this();
        setModal(modal);
        setDestinations(destinations);
    }

    /**
     * Sets the destinations.
     *
     * @param destinations destination list
     */
    public void setDestinations(List<M3NavDestination> destinations) {
        items.getChildren().clear();
        if (destinations == null) {
            return;
        }
        for (int i = 0; i < destinations.size(); i++) {
            final int index = i;
            M3NavDestination dest = destinations.get(i);
            javafx.scene.layout.StackPane iconSlot = new javafx.scene.layout.StackPane();
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            Label text = new Label(dest.label());
            row.getChildren().addAll(iconSlot, text);
            ToggleButton button = new ToggleButton("", row);
            button.getStyleClass().add("m3-drawer-item");
            button.setToggleGroup(group);
            button.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(row, Priority.ALWAYS);
            Runnable syncIcon = () -> {
                boolean active = button.isSelected() || getSelectedIndex() == index;
                Node graphic =
                        active && dest.selectedIcon() != null
                                ? dest.selectedIcon().get()
                                : dest.icon().get();
                iconSlot.getChildren().setAll(graphic == null ? new Region() : graphic);
            };
            button.selectedProperty().addListener((obs, oldV, newV) -> syncIcon.run());
            syncIcon.run();
            button.setOnAction(e -> {
                setSelectedIndex(index);
                if (isModal()) {
                    setOpen(false);
                }
            });
            items.getChildren().add(button);
        }
        selectInGroup(Math.min(getSelectedIndex(), group.getToggles().size() - 1));
    }

    /** Returns the selected index property. */
    public IntegerProperty selectedIndexProperty() {
        return selectedIndex;
    }

    /** Returns the selected index. */
    public int getSelectedIndex() {
        return selectedIndex.get();
    }

    /**
     * Sets the selected index.
     *
     * @param selectedIndex the new index
     */
    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex.set(selectedIndex);
    }

    /** Returns the modal property. */
    public BooleanProperty modalProperty() {
        return modal;
    }

    /** Returns whether the drawer is modal. */
    public boolean isModal() {
        return modal.get();
    }

    /**
     * Sets modal mode.
     *
     * @param modal {@code true} for modal
     */
    public void setModal(boolean modal) {
        this.modal.set(modal);
    }

    /** Returns the open property (modal drawers). */
    public BooleanProperty openProperty() {
        return open;
    }

    /** Returns whether a modal drawer is open. */
    public boolean isOpen() {
        return open.get();
    }

    /**
     * Opens or closes a modal drawer.
     *
     * @param open {@code true} to open
     */
    public void setOpen(boolean open) {
        this.open.set(open);
    }

    private void selectInGroup(int index) {
        if (index >= 0 && index < group.getToggles().size()) {
            group.selectToggle(group.getToggles().get(index));
        }
    }
}
