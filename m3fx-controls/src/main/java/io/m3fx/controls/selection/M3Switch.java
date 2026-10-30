package io.m3fx.controls.selection;

import io.m3fx.controls.internal.StateLayer;
import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.AccessibleRole;
import javafx.scene.Cursor;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * M3 switch with all states and an optional icon thumb in the selected state.
 *
 * <p>See <a href="https://m3.material.io/components/switch/overview">M3 switch</a>. Track 52x32dp
 * with a 2dp full-radius outline, handle 16dp unselected, 24dp selected, 28dp pressed, with a
 * 40dp circular state layer behind the handle (hover 8% / focus 10% / pressed 10%). Keyboard
 * operable via Space/Enter; announces on/off through the accessible role.
 */
public class M3Switch extends Control {

    private static final PseudoClass SELECTED_PC = PseudoClass.getPseudoClass("selected");

    /** Handle radius in px for the unselected state (16dp diameter). */
    private static final double RADIUS_UNSELECTED = 8;
    /** Handle radius in px for the selected state (24dp diameter). */
    private static final double RADIUS_SELECTED = 12;
    /** Handle radius in px while pressed (28dp diameter, spec pressed-handle). */
    private static final double RADIUS_PRESSED = 14;
    /** State-layer circle size in px (spec state-layer-size 40dp). */
    private static final double STATE_LAYER_SIZE = 40;

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
        setCursor(Cursor.HAND);
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
        private final javafx.scene.layout.Region stateLayer = new javafx.scene.layout.Region();
        private final Circle thumb = new Circle(8);
        private final io.m3fx.controls.icon.M3Icon iconCheck =
                io.m3fx.controls.icon.M3Icon.symbol("check");
        private final javafx.beans.value.ChangeListener<Boolean> selectedListener;
        private final javafx.beans.value.ChangeListener<Boolean> showIconListener;
        private final javafx.beans.value.ChangeListener<Boolean> pressedListener;

        SwitchSkin(M3Switch control) {
            super(control);
            track.getStyleClass().add("m3-switch-track");
            track.setArcWidth(32);
            track.setArcHeight(32);
            thumb.getStyleClass().add("m3-switch-thumb");
            iconCheck.getStyleClass().add("m3-switch-thumb-icon");
            iconCheck.setScaleX(0.55);
            iconCheck.setScaleY(0.55);
            iconCheck.setMouseTransparent(true);
            iconCheck.setVisible(control.isShowIcon() && control.isSelected());
            // 40dp circular state layer behind the handle, moving with it (spec state-layer-size).
            stateLayer.getStyleClass().add("m3-switch-state-layer");
            stateLayer.setMinSize(STATE_LAYER_SIZE, STATE_LAYER_SIZE);
            stateLayer.setMaxSize(STATE_LAYER_SIZE, STATE_LAYER_SIZE);
            thumbHolder.getChildren().addAll(stateLayer, thumb, iconCheck);
            StackPane root = new StackPane(track, thumbHolder);
            root.getStyleClass().add("m3-switch-root");
            getChildren().add(root);
            applyStateLayerColor();
            StateLayer.bind(control, stateLayer);
            layoutThumb(false);
            // Held as fields so dispose() can remove them: anonymous listeners captured here would
            // otherwise survive a skin swap and run stale animations on a dead skin.
            selectedListener = (obs, oldV, newV) -> {
                applyStateLayerColor();
                layoutThumb(true);
            };
            showIconListener = (obs, oldV, newV) -> layoutThumb(false);
            pressedListener = (obs, oldV, pressed) -> applyHandleRadius(true);
            control.selectedProperty().addListener(selectedListener);
            control.showIconProperty().addListener(showIconListener);
            control.pressedProperty().addListener(pressedListener);
            control.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                if (!control.isDisabled()) {
                    control.toggle();
                }
            });
            control.addEventHandler(
                    javafx.scene.input.KeyEvent.KEY_PRESSED,
                    e -> {
                        if (!control.isDisabled()
                                && (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER)) {
                            control.toggle();
                            e.consume();
                        }
                    });
        }

        @Override
        public void dispose() {
            M3Switch control = getSkinnable();
            if (control != null) {
                control.selectedProperty().removeListener(selectedListener);
                control.showIconProperty().removeListener(showIconListener);
                control.pressedProperty().removeListener(pressedListener);
                StateLayer.unbind(control, stateLayer);
            }
            super.dispose();
        }

        /** Re-tints the state layer with the current state's layer color (spec on/off colors). */
        private void applyStateLayerColor() {
            M3Switch control = getSkinnable();
            if (control == null) {
                return;
            }
            Color layerColor =
                    ThemeColors.resolve(
                            control,
                            control.isSelected() ? ColorRole.ON_PRIMARY : ColorRole.ON_SURFACE);
            stateLayer.setBackground(
                    new Background(
                            new BackgroundFill(
                                    layerColor, new CornerRadii(STATE_LAYER_SIZE / 2), Insets.EMPTY)));
        }

        private void layoutThumb(boolean animate) {
            M3Switch control = getSkinnable();
            double targetX = control.isSelected() ? 12 : -12;
            iconCheck.setVisible(control.isShowIcon() && control.isSelected());
            if (animate) {
                // Real spring for translation (M3 Expressive spatial default) so the thumb glides.
                io.m3fx.controls.internal.SpringAnimation.animate(
                        thumbHolder.translateXProperty(),
                        targetX,
                        io.m3fx.controls.internal.SpringAnimation.DEFAULT,
                        null);
            } else {
                thumbHolder.setTranslateX(targetX);
            }
            applyHandleRadius(animate);
        }

        private void applyHandleRadius(boolean animate) {
            M3Switch control = getSkinnable();
            double target =
                    control.isPressed()
                            ? RADIUS_PRESSED
                            : control.isSelected() ? RADIUS_SELECTED : RADIUS_UNSELECTED;
            if (animate) {
                io.m3fx.controls.internal.SpringAnimation.animate(
                        thumb.radiusProperty(),
                        target,
                        io.m3fx.controls.internal.SpringAnimation.GENTLE,
                        null);
            } else {
                thumb.setRadius(target);
            }
        }
    }
}
