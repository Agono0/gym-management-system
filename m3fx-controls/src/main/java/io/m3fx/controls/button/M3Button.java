package io.m3fx.controls.button;

import io.m3fx.controls.internal.ElevationShadow;
import io.m3fx.controls.internal.MotionUtil;
import io.m3fx.controls.internal.PressableControl;
import io.m3fx.controls.internal.Ripple;
import io.m3fx.controls.internal.StateLayer;
import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import io.m3fx.core.token.ElevationLevel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * M3 button with five variants (filled, tonal, outlined, elevated, text), optional icon and toggle
 * support.
 *
 * <p>See <a href="https://m3.material.io/components/buttons/overview">M3 buttons spec</a>. A real
 * {@code Control} + {@code Skin}: container height 40dp, full-corner shape, label-large type, a
 * live ripple on press, state-layer overlays, spring press-scale and animated elevation — all
 * colors resolved from the theme (correct in dark mode). Keyboard operable via Space/Enter, with
 * accessible text set from the label.
 */
public class M3Button extends PressableControl {

    /** Minimum interactive height in px (40dp container). */
    public static final double MIN_HEIGHT = 40;

    private final StringProperty text = new SimpleStringProperty(this, "text", "");
    private final ObjectProperty<Node> graphic = new SimpleObjectProperty<>(this, "graphic");
    private final ObjectProperty<ButtonVariant> variant =
            new SimpleObjectProperty<>(this, "variant", ButtonVariant.FILLED);

    /** Creates a filled button with empty text. */
    public M3Button() {
        this("", ButtonVariant.FILLED);
    }

    /**
     * Creates a button with text and a variant.
     *
     * @param text label text
     * @param variant button variant
     */
    public M3Button(String text, ButtonVariant variant) {
        super();
        getStyleClass().add("m3-button");
        setMinHeight(MIN_HEIGHT);
        setText(text);
        setVariant(variant == null ? ButtonVariant.FILLED : variant);
        variantProperty().addListener((obs, oldV, newV) -> updateStyleClass(oldV, newV));
        accessibleTextProperty().bind(textProperty());
    }

    /**
     * Creates a filled button.
     *
     * @param text label text
     * @return the button
     */
    public static M3Button filled(String text) {
        return new M3Button(text, ButtonVariant.FILLED);
    }

    /**
     * Creates a filled tonal button.
     *
     * @param text label text
     * @return the button
     */
    public static M3Button tonal(String text) {
        return new M3Button(text, ButtonVariant.TONAL);
    }

    /**
     * Creates an outlined button.
     *
     * @param text label text
     * @return the button
     */
    public static M3Button outlined(String text) {
        return new M3Button(text, ButtonVariant.OUTLINED);
    }

    /**
     * Creates an elevated button.
     *
     * @param text label text
     * @return the button
     */
    public static M3Button elevated(String text) {
        return new M3Button(text, ButtonVariant.ELEVATED);
    }

    /**
     * Creates a text button.
     *
     * @param text label text
     * @return the button
     */
    public static M3Button text(String text) {
        return new M3Button(text, ButtonVariant.TEXT);
    }

    /**
     * Sets the leading icon graphic.
     *
     * @param icon the icon node (use {@code M3Icon})
     * @return this button, for chaining
     */
    public M3Button withIcon(Node icon) {
        setGraphic(icon);
        return this;
    }

    /** Returns the text property. */
    public StringProperty textProperty() {
        return text;
    }

    /** Returns the label text. */
    public String getText() {
        return text.get();
    }

    /**
     * Sets the label text.
     *
     * @param text label text
     */
    public void setText(String text) {
        this.text.set(text == null ? "" : text);
    }

    /** Returns the graphic property. */
    public ObjectProperty<Node> graphicProperty() {
        return graphic;
    }

    /** Returns the icon graphic. */
    public Node getGraphic() {
        return graphic.get();
    }

    /**
     * Sets the icon graphic.
     *
     * @param graphic icon node
     */
    public void setGraphic(Node graphic) {
        this.graphic.set(graphic);
    }

    /** Returns the variant property. */
    public ObjectProperty<ButtonVariant> variantProperty() {
        return variant;
    }

    /** Returns the variant. */
    public ButtonVariant getVariant() {
        return variant.get();
    }

    /**
     * Sets the variant and updates the style class.
     *
     * @param variant the new variant
     */
    public void setVariant(ButtonVariant variant) {
        ButtonVariant old = this.variant.get();
        this.variant.set(variant == null ? ButtonVariant.FILLED : variant);
        updateStyleClass(old, this.variant.get());
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new ButtonSkin(this);
    }

    private void updateStyleClass(ButtonVariant oldV, ButtonVariant newV) {
        if (oldV != null) {
            getStyleClass().remove("m3-button-" + oldV.name().toLowerCase());
        }
        if (newV != null && !getStyleClass().contains("m3-button-" + newV.name().toLowerCase())) {
            getStyleClass().add("m3-button-" + newV.name().toLowerCase());
        }
    }

    /** Skin: background + state layer + icon/label content + clipped ripple. */
    static final class ButtonSkin extends SkinBase<M3Button> {
        private final StackPane root = new StackPane();
        private final Region background = new Region();
        private final Region stateLayer = new Region();
        private final HBox content = new HBox(8);
        private final StackPane iconSlot = new StackPane();
        private final javafx.scene.control.Label label = new javafx.scene.control.Label();
        private final Pane ripplePane = new Pane();
        private ElevationLevel currentElevation = ElevationLevel.LEVEL0;
        private final javafx.event.EventHandler<MouseEvent> pressHandler;
        private final javafx.event.EventHandler<MouseEvent> releaseHandler;
        private final javafx.event.EventHandler<KeyEvent> keyHandler;

        ButtonSkin(M3Button control) {
            super(control);
            background.getStyleClass().add("m3-button-bg");
            background.setMouseTransparent(true);
            stateLayer.getStyleClass().add("m3-button-state");
            content.getStyleClass().add("m3-button-content");
            content.setAlignment(Pos.CENTER);
            content.setMouseTransparent(true);
            HBox.setHgrow(label, javafx.scene.layout.Priority.ALWAYS);
            iconSlot.getStyleClass().add("m3-button-icon");
            label.getStyleClass().add("m3-button-label");
            label.textProperty().bind(control.textProperty());
            label.setMouseTransparent(true);
            ripplePane.getStyleClass().add("m3-button-ripple");
            content.getChildren().addAll(iconSlot, label);
            // Paint order: background, ripple, content, state layer — the ripple spreads over
            // the container but never covers the text.
            root.getChildren().addAll(background, ripplePane, content, stateLayer);
            getChildren().add(root);
            syncGraphic();
            control.graphicProperty().addListener((obs, oldV, newV) -> syncGraphic());
            Rectangle clip = new Rectangle();
            clip.setArcWidth(40);
            clip.setArcHeight(40);
            clip.widthProperty().bind(root.widthProperty());
            clip.heightProperty().bind(root.heightProperty());
            ripplePane.setClip(clip);
            StateLayer.bind(control, stateLayer);
            Ripple.attach(control, ripplePane, this::resolveInk);
            MotionUtil.addPressScale(control);
            control.hoverProperty().addListener((obs, oldV, hover) -> updateElevation(true));
            control.pressedProperty().addListener((obs, oldV, pressed) -> updateElevation(true));
            control.variantProperty().addListener((obs, oldV, newV) -> updateElevation(false));
            control.disabledProperty().addListener((obs, oldV, disabled) -> updateElevation(false));
            updateElevation(false);
            pressHandler = control::pressBegin;
            releaseHandler = control::pressEnd;
            keyHandler = control::keyActivate;
            control.addEventHandler(MouseEvent.MOUSE_PRESSED, pressHandler);
            control.addEventHandler(MouseEvent.MOUSE_RELEASED, releaseHandler);
            control.addEventHandler(KeyEvent.KEY_PRESSED, keyHandler);
        }

        @Override
        public void dispose() {
            M3Button control = getSkinnable();
            if (control != null) {
                control.removeEventHandler(MouseEvent.MOUSE_PRESSED, pressHandler);
                control.removeEventHandler(MouseEvent.MOUSE_RELEASED, releaseHandler);
                control.removeEventHandler(KeyEvent.KEY_PRESSED, keyHandler);
                MotionUtil.removePressScale(control);
                StateLayer.unbind(control, stateLayer);
                Ripple.detach(control, ripplePane);
            }
            super.dispose();
        }

        private void syncGraphic() {
            Node graphic = getSkinnable().getGraphic();
            iconSlot.getChildren().setAll(graphic == null ? new Region() : graphic);
            iconSlot.setVisible(graphic != null);
            iconSlot.setManaged(graphic != null);
        }

        private Color resolveInk() {
            M3Button control = getSkinnable();
            ColorRole role =
                    switch (control.getVariant()) {
                        case FILLED ->
                                control.isSelected()
                                        ? ColorRole.ON_SECONDARY_CONTAINER
                                        : ColorRole.ON_PRIMARY;
                        case TONAL -> ColorRole.ON_SECONDARY_CONTAINER;
                        case OUTLINED, TEXT -> ColorRole.PRIMARY;
                        case ELEVATED -> ColorRole.PRIMARY;
                    };
            return ThemeColors.resolve(control, role);
        }

        private void updateElevation(boolean animate) {
            M3Button control = getSkinnable();
            ElevationLevel next =
                    switch (control.getVariant()) {
                        case FILLED -> control.isPressed()
                                ? ElevationLevel.LEVEL0
                                : control.isHover()
                                        ? ElevationLevel.LEVEL1
                                        : ElevationLevel.LEVEL0;
                        case ELEVATED -> control.isPressed()
                                ? ElevationLevel.LEVEL1
                                : control.isHover()
                                        ? ElevationLevel.LEVEL2
                                        : ElevationLevel.LEVEL1;
                        default -> ElevationLevel.LEVEL0;
                    };
            if (control.isDisabled()) {
                next = ElevationLevel.LEVEL0;
            }
            Color shadow = ThemeColors.resolve(control, ColorRole.SHADOW);
            if (animate) {
                ElevationShadow.animateTo(root, currentElevation, next, shadow);
            } else if (next == ElevationLevel.LEVEL0) {
                root.setEffect(null);
            } else {
                root.setEffect(next.toEffect(shadow));
            }
            currentElevation = next;
        }
    }
}
