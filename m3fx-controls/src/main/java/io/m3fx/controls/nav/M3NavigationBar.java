package io.m3fx.controls.nav;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.util.List;

/**
 * M3 navigation bar: 3-5 short destinations in a bottom row with an active pill indicator.
 *
 * <p>See <a href="https://m3.material.io/components/navigation-bar/overview">M3 navigation
 * bar</a>. Height 80dp, surface-container color, secondary-container pill on the selected icon,
 * label-medium text.
 */
public class M3NavigationBar extends HBox {

    private final IntegerProperty selectedIndex = new SimpleIntegerProperty(this, "selectedIndex", 0);
    private final ToggleGroup group = new ToggleGroup();

    /** Creates an empty navigation bar. */
    public M3NavigationBar() {
        getStyleClass().add("m3-navigation-bar");
        setAlignment(Pos.CENTER);
        setMinHeight(80);
        selectedIndex.addListener((obs, oldV, newV) -> selectInGroup(newV.intValue()));
    }

    /**
     * Creates a navigation bar with destinations.
     *
     * @param destinations 3-5 destinations
     */
    public M3NavigationBar(List<M3NavDestination> destinations) {
        this();
        setDestinations(destinations);
    }

    /**
     * Sets the destinations (rebuilds the buttons).
     *
     * @param destinations destination list
     */
    public void setDestinations(List<M3NavDestination> destinations) {
        getChildren().clear();
        if (destinations == null) {
            return;
        }
        for (int i = 0; i < destinations.size(); i++) {
            final int index = i;
            M3NavDestination dest = destinations.get(i);
            StackPane iconBox = new StackPane();
            iconBox.getStyleClass().add("m3-navigation-icon");
            iconBox.setMinSize(64, 32);
            iconBox.setMaxSize(64, 32);
            Label label = new Label(dest.label());
            label.getStyleClass().add("m3-navigation-label");
            VBox cell = new VBox(iconBox, label);
            cell.setAlignment(Pos.CENTER);
            ToggleButton button = new ToggleButton("", cell);
            button.getStyleClass().add("m3-navigation-item");
            button.setToggleGroup(group);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(e -> setSelectedIndex(index));
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
            HBox.setHgrow(button, Priority.ALWAYS);
            getChildren().add(button);
        }
        selectInGroup(Math.min(getSelectedIndex(), getChildren().size() - 1));
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
