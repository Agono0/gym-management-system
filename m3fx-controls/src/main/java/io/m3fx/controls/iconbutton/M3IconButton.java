package io.m3fx.controls.iconbutton;

import io.m3fx.controls.internal.MotionUtil;
import io.m3fx.controls.internal.PressableControl;
import io.m3fx.controls.internal.Ripple;
import io.m3fx.controls.internal.StateLayer;
import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * M3 icon button: a 48x48 touch target with an icon, in standard/filled/tonal/outlined variants,
 * with toggle support.
 *
 * <p>See <a href="https://m3.material.io/components/icon-buttons/overview">M3 icon buttons</a>. A
 * real {@code Control} + {@code Skin} with live ripple, state-layer overlays and spring
 * press-scale, all resolved from the theme (correct in dark mode). Toggle mode keeps a selected
 * state rendered like the filled variant. Keyboard operable via Space/Enter.
 */
public class M3IconButton extends PressableControl {

    /** M3 touch target size in px. */
    public static final double TOUCH_TARGET = 48;

    private final ObjectProperty<Node> graphic = new SimpleObjectProperty<>(this, "graphic");
    private final ObjectProperty<IconButtonVariant> variant =
            new SimpleObjectProperty<>(this, "variant", IconButtonVariant.STANDARD);

    /** Creates a standard icon button. */
    public M3IconButton() {
        this(null, IconButtonVariant.STANDARD);
    }

    /**
     * Creates an icon button with a graphic and variant.
     *
     * @param graphic the icon node (use {@code M3Icon})
     * @param variant the variant
     */
    public M3IconButton(Node graphic, IconButtonVariant variant) {
        super();
        getStyleClass().add("m3-icon-button");
        setMinSize(TOUCH_TARGET, TOUCH_TARGET);
        setPrefSize(TOUCH_TARGET, TOUCH_TARGET);
        setMaxSize(TOUCH_TARGET, TOUCH_TARGET);
        setGraphic(graphic);
        setVariant(variant == null ? IconButtonVariant.STANDARD : variant);
        variantProperty().addListener((obs, oldV, newV) -> updateStyleClass(oldV, newV));
        setAccessibleText("Icon button");
    }

    /**
     * Creates a standard icon button.
     *
     * @param graphic icon node
     * @return the button
     */
    public static M3IconButton standard(Node graphic) {
        return new M3IconButton(graphic, IconButtonVariant.STANDARD);
    }

    /**
     * Creates a filled icon button.
     *
     * @param graphic icon node
     * @return the button
     */
    public static M3IconButton filled(Node graphic) {
        return new M3IconButton(graphic, IconButtonVariant.FILLED);
    }

    /**
     * Creates a tonal icon button.
     *
     * @param graphic icon node
     * @return the button
     */
    public static M3IconButton tonal(Node graphic) {
        return new M3IconButton(graphic, IconButtonVariant.TONAL);
    }

    /**
     * Creates an outlined icon button.
     *
     * @param graphic icon node
     * @return the button
     */
    public static M3IconButton outlined(Node graphic) {
        return new M3IconButton(graphic, IconButtonVariant.OUTLINED);
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
    public ObjectProperty<IconButtonVariant> variantProperty() {
        return variant;
    }

    /** Returns the variant. */
    public IconButtonVariant getVariant() {
        return variant.get();
    }

    /**
     * Sets the variant.
     *
     * @param variant the new variant
     */
    public void setVariant(IconButtonVariant variant) {
        IconButtonVariant old = this.variant.get();
        this.variant.set(variant == null ? IconButtonVariant.STANDARD : variant);
        updateStyleClass(old, this.variant.get());
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new IconButtonSkin(this);
    }

    private void updateStyleClass(IconButtonVariant oldV, IconButtonVariant newV) {
        if (oldV != null) {
            getStyleClass().remove("m3-icon-button-" + oldV.name().toLowerCase());
        }
        if (newV != null && !getStyleClass().contains("m3-icon-button-" + newV.name().toLowerCase())) {
            getStyleClass().add("m3-icon-button-" + newV.name().toLowerCase());
        }
    }

    /** Skin: circular background + state layer + centered icon + clipped ripple. */
    static final class IconButtonSkin extends SkinBase<M3IconButton> {
        private final StackPane root = new StackPane();
        private final Region background = new Region();
        private final Region stateLayer = new Region();
        private final StackPane iconSlot = new StackPane();
        private final Pane ripplePane = new Pane();
        private final javafx.event.EventHandler<MouseEvent> pressHandler;
        private final javafx.event.EventHandler<MouseEvent> releaseHandler;
        private final javafx.event.EventHandler<KeyEvent> keyHandler;

        IconButtonSkin(M3IconButton control) {
            super(control);
            background.getStyleClass().add("m3-icon-button-bg");
            background.setMouseTransparent(true);
            stateLayer.getStyleClass().add("m3-icon-button-state");
            iconSlot.getStyleClass().add("m3-icon-button-icon");
            iconSlot.setAlignment(Pos.CENTER);
            iconSlot.setMouseTransparent(true);
            ripplePane.getStyleClass().add("m3-icon-button-ripple");
            root.setAlignment(Pos.CENTER);
            // Paint order: background, ripple, icon, state layer — ripple never covers the icon.
            root.getChildren().addAll(background, ripplePane, iconSlot, stateLayer);
            getChildren().add(root);
            syncGraphic();
            control.graphicProperty().addListener((obs, oldV, newV) -> syncGraphic());
            Rectangle clip = new Rectangle();
            clip.setArcWidth(48);
            clip.setArcHeight(48);
            clip.widthProperty().bind(root.widthProperty());
            clip.heightProperty().bind(root.heightProperty());
            ripplePane.setClip(clip);
            StateLayer.bind(control, stateLayer);
            Ripple.attach(control, ripplePane, this::resolveInk);
            MotionUtil.addPressScale(control);
            pressHandler = control::pressBegin;
            releaseHandler = control::pressEnd;
            keyHandler = control::keyActivate;
            control.addEventHandler(MouseEvent.MOUSE_PRESSED, pressHandler);
            control.addEventHandler(MouseEvent.MOUSE_RELEASED, releaseHandler);
            control.addEventHandler(KeyEvent.KEY_PRESSED, keyHandler);
        }

        @Override
        public void dispose() {
            M3IconButton control = getSkinnable();
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
        }

        private Color resolveInk() {
            M3IconButton control = getSkinnable();
            ColorRole role =
                    switch (control.getVariant()) {
                        case FILLED -> ColorRole.ON_PRIMARY;
                        case TONAL -> ColorRole.ON_SECONDARY_CONTAINER;
                        case OUTLINED, STANDARD -> ColorRole.ON_SURFACE_VARIANT;
                    };
            if (control.isSelected()) {
                role = ColorRole.ON_PRIMARY;
            }
            return ThemeColors.resolve(control, role);
        }
    }
}
