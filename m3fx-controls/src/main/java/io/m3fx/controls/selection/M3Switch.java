package io.m3fx.controls.selection;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * M3 switch with all states and an optional icon thumb in the selected state.
 *
 * <p>See <a href="https://m3.material.io/components/switch/overview">M3 switch</a>. Track 52x32dp,
 * thumb 16dp (24dp when selected with icon). Keyboard operable via Space/Enter; announces
 * on/off through the accessible role.
 */
public class M3Switch extends Control {

    private static final PseudoClass SELECTED_PC = PseudoClass.getPseudoClass("selected");

    private final BooleanProperty selected = new SimpleBooleanProperty(this, "selected", false);
    private final BooleanProperty showIcon = new SimpleBooleanProperty(this, "showIcon", false);

    /** Creates an unselected switch. */
    public M3Switch() {
        this(false);
    }

    /**
     * Creates a switch.
     *
     * @param selected initial state
     */
    public M3Switch(boolean selected) {
        getStyleClass().add("m3-switch");
        setAccessibleRole(AccessibleRole.CHECK_BOX);
        setFocusTraversable(true);
        setSelected(selected);
        selectedProperty().addListener((obs, oldV, newV) -> {
            pseudoClassStateChanged(SELECTED_PC, newV);
            setAccessibleText(newV ? "On" : "Off");
        });
        pseudoClassStateChanged(SELECTED_PC, isSelected());
        setAccessibleText(isSelected() ? "On" : "Off");
        setMinSize(52, 48);
        setPrefSize(52, 48);
    }

    /** Returns the selected property. */
    public BooleanProperty selectedProperty() {
        return selected;
    }

    /** Returns whether the switch is on. */
    public boolean isSelected() {
        return selected.get();
    }

    /**
     * Sets the switch state.
     *
     * @param selected {@code true} for on
     */
    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    /** Toggles the switch state. */
    public void toggle() {
        if (!isDisabled()) {
            setSelected(!isSelected());
        }
    }

    /** Returns the icon-thumb property (selected state shows an icon dot). */
    public BooleanProperty showIconProperty() {
        return showIcon;
    }

    /** Returns whether the selected thumb shows an icon. */
    public boolean isShowIcon() {
        return showIcon.get();
    }

    /**
     * Sets whether the selected thumb shows an icon.
     *
     * @param showIcon {@code true} to show the icon thumb
     */
    public void setShowIcon(boolean showIcon) {
        this.showIcon.set(showIcon);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new SwitchSkin(this);
    }

    /** Skin: track rectangle plus sliding thumb with motion-token timing. */
    static final class SwitchSkin extends SkinBase<M3Switch> {
        private final Rectangle track = new Rectangle(52, 32);
        private final StackPane thumbHolder = new StackPane();
        private final Circle thumb = new Circle(8);
        private final Circle iconDot = new Circle(4);

        SwitchSkin(M3Switch control) {
            super(control);
            track.getStyleClass().add("m3-switch-track");
            track.setArcWidth(32);
            track.setArcHeight(32);
            thumb.getStyleClass().add("m3-switch-thumb");
            iconDot.getStyleClass().add("m3-switch-thumb-icon");
            iconDot.setVisible(control.isShowIcon() && control.isSelected());
            thumbHolder.getChildren().addAll(thumb, iconDot);
            StackPane root = new StackPane(track, thumbHolder);
            root.getStyleClass().add("m3-switch-root");
            getChildren().add(root);
            layoutThumb(false);
            control.selectedProperty().addListener((obs, oldV, newV) -> layoutThumb(true));
            control.showIconProperty().addListener((obs, oldV, newV) -> layoutThumb(false));
            control.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                if (!control.isDisabled()) {
                    control.toggle();
                }
            });
            control.addEventHandler(
                    javafx.scene.input.KeyEvent.KEY_PRESSED,
                    e -> {
                        if (!control.isDisabled() && (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER)) {
                            control.toggle();
                            e.consume();
                        }
                    });
            control.disabledProperty().addListener((obs, oldV, nowDisabled) -> root.setOpacity(nowDisabled ? 0.38 : 1.0));
            if (control.isDisabled()) {
                root.setOpacity(0.38);
            }
        }

        private void layoutThumb(boolean animate) {
            M3Switch control = getSkinnable();
            double targetX = control.isSelected() ? 12 : -12;
            double targetRadius = control.isSelected() ? 12 : 8;
            iconDot.setVisible(control.isShowIcon() && control.isSelected());
            if (animate) {
                // Real spring for both translation and expansion so thumb smoothly glides and grows
                io.m3fx.controls.internal.SpringAnimation.animate(
                        thumbHolder.translateXProperty(),
                        targetX,
                        new io.m3fx.controls.internal.SpringAnimation.SpringParams(500, 0.75),
                        null);
                io.m3fx.controls.internal.SpringAnimation.animate(
                        thumb.radiusProperty(),
                        targetRadius,
                        new io.m3fx.controls.internal.SpringAnimation.SpringParams(450, 0.8),
                        null);
            } else {
                thumbHolder.setTranslateX(targetX);
                thumb.setRadius(targetRadius);
            }
        }
    }
}
