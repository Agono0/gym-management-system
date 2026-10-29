package io.m3fx.controls.nav;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.util.List;

/**
 * M3 navigation rail: a vertical column of destinations with an optional FAB and menu button.
 *
 * <p>See <a href="https://m3.material.io/components/navigation-rail/overview">M3 navigation
 * rail</a>. Width 80dp, surface color, secondary-container pill on the selected icon.
 */
public class M3NavigationRail extends VBox {

    private static final String HEADER_KEY = "m3fx.rail-header";

    private final IntegerProperty selectedIndex = new SimpleIntegerProperty(this, "selectedIndex", 0);
    private final ToggleGroup group = new ToggleGroup();
    private final VBox items = new VBox(4);

    /** Creates an empty navigation rail. */
    public M3NavigationRail() {
        getStyleClass().add("m3-navigation-rail");
        setAlignment(Pos.TOP_CENTER);
        // Spec: container width 80dp (md.comp.navigation-rail.container.width).
        setMinWidth(80);
        items.setAlignment(Pos.TOP_CENTER);
        items.managedProperty().bind(javafx.beans.binding.Bindings.isNotEmpty(items.getChildren()));
        items.visibleProperty().bind(javafx.beans.binding.Bindings.isNotEmpty(items.getChildren()));
        getChildren().add(items);
        selectedIndex.addListener((obs, oldV, newV) -> selectInGroup(newV.intValue()));
    }

    /**
     * Creates a rail with destinations.
     *
     * @param destinations destination list
     */
    public M3NavigationRail(List<M3NavDestination> destinations) {
        this();
        setDestinations(destinations);
    }

    /**
     * Sets the header node shown above the destinations (menu button or FAB).
     *
     * @param header header node, may be {@code null}
     */
    public void setHeader(Node header) {
        getChildren().removeIf(n -> n.getProperties().get(HEADER_KEY) == Boolean.TRUE);
        if (header != null) {
            header.getProperties().put(HEADER_KEY, Boolean.TRUE);
            getChildren().add(0, header);
        }
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
            StackPane iconBox = new StackPane();
            iconBox.getStyleClass().add("m3-navigation-icon");
            // Spec: rail active indicator 56x32dp full shape.
            iconBox.setMinSize(56, 32);
            iconBox.setMaxSize(56, 32);
            Label label = new Label(dest.label());
            label.getStyleClass().add("m3-navigation-label");
            VBox cell = new VBox(iconBox, label);
            cell.setAlignment(Pos.CENTER);
            ToggleButton button = new ToggleButton("", cell);
            button.getStyleClass().add("m3-navigation-item");
            button.setToggleGroup(group);
            // M3: one destination is always active; re-select if the group deselects it.
            button.setOnAction(e -> {
                if (!button.isSelected()) {
                    button.setSelected(true);
                }
                setSelectedIndex(index);
            });
            Runnable syncIcon = () -> {
                boolean active = button.isSelected() || getSelectedIndex() == index;
                Node graphic =
                        active && dest.selectedIcon() != null
                                ? dest.selectedIcon().get()
                                : dest.icon().get();
                iconBox.getChildren().setAll(graphic == null ? new Region() : graphic);
            };
            button.selectedProperty().addListener((obs, oldV, newV) -> syncIcon.run());
            syncIcon.run();
            button.setAccessibleText(dest.label());
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

    private void selectInGroup(int index) {
        if (index >= 0 && index < group.getToggles().size()) {
            group.selectToggle(group.getToggles().get(index));
        }
    }
}
