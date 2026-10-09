package io.m3fx.controls.internal;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Control;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * Shared press/toggle behavior for custom M3 button skins (real {@code Control} + {@code Skin},
 * not restyled stock buttons).
 *
 * <p>Implements {@link Toggle} so toggle groups work, tracks press intent (press must start on the
 * control), fires {@link ActionEvent} on click/Space/Enter, and mirrors selection to the
 * {@code :selected} pseudo-class. Skins add visuals (background, state layer, ripple) on top.
 */
public abstract class PressableControl extends Control implements Toggle {

    private static final PseudoClass SELECTED_PC = PseudoClass.getPseudoClass("selected");

    private final BooleanProperty selected = new SimpleBooleanProperty(this, "selected", false);
    private final BooleanProperty toggleable = new SimpleBooleanProperty(this, "toggleable", false);
    private final ObjectProperty<ToggleGroup> toggleGroup =
            new SimpleObjectProperty<>(this, "toggleGroup");
    private final ObjectProperty<EventHandler<ActionEvent>> onAction =
            new SimpleObjectProperty<>(this, "onAction");

    private boolean pressArmed;

    /** Creates a focusable button-role control. */
    protected PressableControl() {
        setAccessibleRole(AccessibleRole.BUTTON);
        setFocusTraversable(true);
        selectedProperty().addListener((obs, oldV, newV) -> pseudoClassStateChanged(SELECTED_PC, newV));
        onActionProperty().addListener((obs, oldV, handler) -> {
            if (oldV != null) {
                removeEventHandler(ActionEvent.ACTION, oldV);
            }
            if (handler != null) {
                addEventHandler(ActionEvent.ACTION, handler);
            }
        });
    }

    /** Returns the selected property. */
    @Override
    public BooleanProperty selectedProperty() {
        return selected;
    }

    /** Returns whether the control is selected. */
    @Override
    public boolean isSelected() {
        return selected.get();
    }

    /**
     * Sets the selected state (mirrors the {@code :selected} pseudo-class and syncs toggle groups).
     *
     * @param selected the new state
     */
    @Override
    public void setSelected(boolean selected) {
        this.selected.set(selected);
        if (selected && getToggleGroup() != null && getToggleGroup().getSelectedToggle() != this) {
            getToggleGroup().selectToggle(this);
        }
    }

    /** Returns the toggleable property. */
    public BooleanProperty toggleableProperty() {
        return toggleable;
    }

    /** Returns whether toggle selection is enabled. */
    public boolean isToggleable() {
        return toggleable.get();
    }

    /**
     * Sets whether the control keeps a selected state.
     *
     * @param toggleable {@code true} to enable toggle behavior
     */
    public void setToggleable(boolean toggleable) {
        this.toggleable.set(toggleable);
        if (!toggleable && getToggleGroup() == null) {
            setSelected(false);
        }
    }

    /** Returns the toggle group property. */
    @Override
    public ObjectProperty<ToggleGroup> toggleGroupProperty() {
        return toggleGroup;
    }

    /** Returns the toggle group. */
    @Override
    public ToggleGroup getToggleGroup() {
        return toggleGroup.get();
    }

    /**
     * Moves this control between toggle groups.
     *
     * @param group the new group, may be {@code null}
     */
    @Override
    public void setToggleGroup(ToggleGroup group) {
        ToggleGroup old = getToggleGroup();
        if (old != null) {
            old.getToggles().remove(this);
        }
        toggleGroup.set(group);
        if (group != null && !group.getToggles().contains(this)) {
            group.getToggles().add(this);
        }
    }

    /** Returns the action handler property. */
    public ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
        return onAction;
    }

    /** Returns the action handler. */
    public EventHandler<ActionEvent> getOnAction() {
        return onAction.get();
    }

    /**
     * Sets the action handler (fires on click, Space and Enter).
     *
     * @param handler the handler, may be {@code null}
     */
    public void setOnAction(EventHandler<ActionEvent> handler) {
        onAction.set(handler);
    }

    /**
     * Activates the control: toggles selection when toggleable/grouped, then fires an action event.
     */
    public void activate() {
        if (isDisabled()) {
            return;
        }
        if (isToggleable() || getToggleGroup() != null) {
            setSelected(!isSelected());
        }
        fireEvent(new ActionEvent(this, null));
    }

    /**
     * Records a press start (skins call this from {@code MOUSE_PRESSED}).
     *
     * @param event the press event
     */
    protected void pressBegin(MouseEvent event) {
        if (isDisabled() || event.getButton() != MouseButton.PRIMARY) {
            pressArmed = false;
            return;
        }
        pressArmed = true;
        requestFocus();
    }

    /**
     * Completes a press, activating only if it started here (skins call this from
     * {@code MOUSE_RELEASED}).
     *
     * @param event the release event
     */
    protected void pressEnd(MouseEvent event) {
        if (!pressArmed) {
            return;
        }
        pressArmed = false;
        if (!isDisabled() && (isHover() || isFocused())) {
            activate();
        }
    }

    /**
     * Handles keyboard activation (skins call this from {@code KEY_PRESSED}).
     *
     * @param event the key event
     */
    protected void keyActivate(KeyEvent event) {
        if (!isDisabled()
                && (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER)) {
            activate();
            event.consume();
        }
    }
}
