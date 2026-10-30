package io.m3fx.controls.nav;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
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
    private final BooleanProperty hideInactiveLabels =
            new SimpleBooleanProperty(this, "hideInactiveLabels", false);
    private final ToggleGroup group = new ToggleGroup();

    /** Creates an empty navigation bar. */
    public M3NavigationBar() {
        getStyleClass().add("m3-navigation-bar");
        setAlignment(Pos.CENTER);
        // Spec: container height 80dp (md.comp.navigation-bar.container.height).
        setMinHeight(80);
        selectedIndex.addListener((obs, oldV, newV) -> selectInGroup(newV.intValue()));
        // Radio-group style arrow navigation between destinations (wraps at the ends).
        addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            List<ToggleButton> buttons = destinationButtons();
            int count = buttons.size();
            if (count < 2 || isDisabled()) {
                return;
            }
            int target;
            switch (e.getCode()) {
                case LEFT, KP_LEFT -> target = Math.floorMod(anchor(buttons) - 1, count);
                case RIGHT, KP_RIGHT -> target = Math.floorMod(anchor(buttons) + 1, count);
                case HOME -> target = 0;
                case END -> target = count - 1;
                default -> {
                    return;
                }
            }
            e.consume();
            selectDestination(buttons, target);
        });
    }

    /** Selects the destination at {@code index} and moves focus to its button. */
    private void selectDestination(List<ToggleButton> buttons, int index) {
        ToggleButton button = buttons.get(index);
        button.requestFocus();
        button.setSelected(true);
        setSelectedIndex(index);
    }

    /** The destination buttons, in destination order. */
    private List<ToggleButton> destinationButtons() {
        List<ToggleButton> buttons = new ArrayList<>();
        for (javafx.scene.Node child : getChildren()) {
            if (child instanceof ToggleButton button) {
                buttons.add(button);
            }
        }
        return buttons;
    }

    /** Index to navigate from: the selected destination, else the focused one, else 0. */
    private int anchor(List<ToggleButton> buttons) {
        int selected = getSelectedIndex();
        if (selected >= 0 && selected < buttons.size()) {
            return selected;
        }
        javafx.scene.Node focusOwner = getScene() != null ? getScene().getFocusOwner() : null;
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i) == focusOwner) {
                return i;
            }
        }
        return 0;
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
            // Spec: active indicator 64x32dp full shape (radius 16 on 32px height).
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
            // M3: one destination is always active; JavaFX ToggleGroups allow clicking the
            // selected toggle off, so re-select it before reporting the index.
            button.setOnAction(e -> {
                if (!button.isSelected()) {
                    button.setSelected(true);
                }
                setSelectedIndex(index);
            });
            // Labels show on all destinations by default; hideInactiveLabels trims the
            // inactive ones (spec label behavior "on" vs "on and off").
            label.visibleProperty().bind(
                    button.selectedProperty().or(hideInactiveLabels.not()));
            label.managedProperty().bind(label.visibleProperty());
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
            HBox.setHgrow(button, Priority.ALWAYS);
            getChildren().add(button);
        }
        selectInGroup(Math.min(getSelectedIndex(), getChildren().size() - 1));
    }

    /** Returns the selected index property. */
    public IntegerProperty selectedIndexProperty() {
        return selectedIndex;
    }

    /** Returns the hide-inactive-labels property. */
    public BooleanProperty hideInactiveLabelsProperty() {
        return hideInactiveLabels;
    }

    /** Returns whether inactive destination labels are hidden. */
    public boolean isHideInactiveLabels() {
        return hideInactiveLabels.get();
    }

    /**
     * Hides labels on inactive destinations (spec label behavior "on and off"; default shows all
     * labels, matching the material-web {@code hideInactiveLabels = false} default).
     *
     * @param hide {@code true} to show the label only on the active destination
     */
    public void setHideInactiveLabels(boolean hide) {
        hideInactiveLabels.set(hide);
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
