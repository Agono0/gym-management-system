package io.m3fx.controls.iconbutton;

import io.m3fx.controls.common.M3Tooltip;
import io.m3fx.controls.internal.MotionUtil;
import io.m3fx.controls.common.PressableControl;
import io.m3fx.controls.internal.Ripple;
import io.m3fx.controls.internal.StateLayer;
import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.util.Objects;

/**
 * M3 icon button: a 48x48 touch target with a 40dp container circle and an icon, in
 * standard/filled/tonal/outlined variants, with toggle support.
 *
 * <p>See <a href="https://m3.material.io/components/icon-buttons/overview">M3 icon buttons</a>. A
 * real {@code Control} + {@code Skin} with live ripple, state-layer overlays and spring
 * press-scale, all resolved from the theme (correct in dark mode). Toggle mode keeps a selected
 * state rendered like the filled variant. Keyboard operable via Space/Enter.
 *
 * <p>Accessibility: an {@link M3Icon} graphic announces its symbol name capitalized
 * ("delete" &rarr; "Delete"); an {@code accessibleText} set by the caller always wins. A tooltip
 * with the announced text is installed automatically unless the caller already installed one.
 */
// Suppressed this-escape (false positives javac 27 cannot see across the module boundary):
// (1) the JavaFX property-bean idiom new Simple*Property(this, ...) - the property
// constructor only stores the bean reference, exactly like every built-in control;
// (2) constructor calls to JavaFX final methods (getStyleClass(), setAccessibleRole())
// - final, but javac does not honor ACC_FINAL for symbols from another module. This
// class's own overridable methods are never called during construction.
@SuppressWarnings("this-escape")
public class M3IconButton extends PressableControl {

    /** M3 touch target size in px. */
    public static final double TOUCH_TARGET = 48;

    private final ObjectProperty<Node> graphic = new SimpleObjectProperty<>(this, "graphic");
    private final ObjectProperty<IconButtonVariant> variant =
            new SimpleObjectProperty<>(this, "variant", IconButtonVariant.STANDARD);

    /** The accessible text this control last derived from its graphic; caller changes win. */
    private String lastDerivedText;
    /** Set once the caller writes an accessible text that differs from the derived one. */
    private boolean accessibleTextOverridden;
    /** The auto-installed tooltip; stops being managed if the caller replaces it. */
    private Tooltip autoTooltip;
    private M3Icon watchedIcon;
    private ChangeListener<String> symbolListener;

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
        // Accessible label: derived from an M3Icon graphic; a caller-written accessible text
        // (anything different from what we derived) disables auto-derivation permanently.
        accessibleTextProperty().addListener((obs, oldV, newV) -> {
            if (!Objects.equals(newV, lastDerivedText)) {
                accessibleTextOverridden = true;
            }
            refreshAutoTooltip();
        });
        graphicProperty().addListener((obs, oldGraphic, newGraphic) -> watchGraphicIcon(newGraphic));
        watchGraphicIcon(graphic);
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

    /** Re-derives the accessible label from the graphic's icon and refreshes the tooltip. */
    private void deriveAccessibleText() {
        if (accessibleTextOverridden) {
            return;
        }
        String derived = null;
        if (watchedIcon != null) {
            // M3Icon announces its symbol name (renderSymbol sets it as the icon's accessible
            // text even when built through M3Icon.symbol, whose name property stays unset).
            String announced = watchedIcon.getSymbolName() != null
                    ? watchedIcon.getSymbolName()
                    : watchedIcon.getAccessibleText();
            if (announced != null && !announced.startsWith(UNKNOWN_ICON_PREFIX)) {
                derived = capitalize(announced);
            }
        }
        lastDerivedText = derived;
        setAccessibleText(derived);
        refreshAutoTooltip();
    }

    /** Tracks the current graphic's icon name so label changes re-derive the announcement. */
    private void watchGraphicIcon(Node graphic) {
        if (watchedIcon != null && symbolListener != null) {
            watchedIcon.symbolNameProperty().removeListener(symbolListener);
        }
        watchedIcon = graphic instanceof M3Icon icon ? icon : null;
        if (watchedIcon != null) {
            symbolListener = (obs, oldV, newV) -> deriveAccessibleText();
            watchedIcon.symbolNameProperty().addListener(symbolListener);
        } else {
            symbolListener = null;
        }
        deriveAccessibleText();
    }

    /** Keeps the auto-installed tooltip in sync with the announced text. */
    private void refreshAutoTooltip() {
        if (autoTooltip != null && getTooltip() != autoTooltip) {
            // The caller replaced the tooltip; stop managing it.
            autoTooltip = null;
        }
        String text = getAccessibleText();
        if (text == null || text.isBlank()) {
            return;
        }
        if (autoTooltip == null) {
            if (getTooltip() != null) {
                return; // a caller-installed tooltip wins, never double-install
            }
            autoTooltip = M3Tooltip.plain(text);
            setTooltip(autoTooltip);
        } else {
            autoTooltip.setText(text);
        }
    }

    /** Prefix M3Icon announces for catalog names it does not render; not a usable label. */
    private static final String UNKNOWN_ICON_PREFIX = "unknown icon: ";

    /** "delete" -> "Delete": screen readers announce the icon name with a leading capital. */
    private static String capitalize(String symbolName) {
        if (symbolName == null || symbolName.isEmpty()) {
            return symbolName;
        }
        return Character.toUpperCase(symbolName.charAt(0)) + symbolName.substring(1);
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
        /** Spec container size: a 40dp circle inside the 48dp touch target. */
        private static final double CONTAINER_SIZE = 40;

        /** Keyboard-press feedback: mirrors the pressed state-layer while Space/Enter is held. */
        private static final javafx.css.PseudoClass KEY_ARMED_PC =
                javafx.css.PseudoClass.getPseudoClass("key-armed");

        private final StackPane root = new StackPane();
        private final Region background = new Region();
        private final Region stateLayer = new Region();
        private final StackPane iconSlot = new StackPane();
        private final Pane ripplePane = new Pane();
        private final javafx.event.EventHandler<MouseEvent> pressHandler;
        private final javafx.event.EventHandler<MouseEvent> releaseHandler;
        private final javafx.event.EventHandler<KeyEvent> keyHandler;
        private final javafx.event.EventHandler<KeyEvent> keyArmPressed;
        private final javafx.event.EventHandler<KeyEvent> keyArmReleased;
        private final javafx.beans.value.ChangeListener<Boolean> keyArmFocusListener;
        private final javafx.beans.value.ChangeListener<Node> graphicListener;

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
            // Spec: filled/tonal/outlined container is 40dp inside the 48dp touch target. Pref +
            // max (no min) so composite hosts (e.g. M3SplitButton's smaller trailing half) can
            // still shrink it below 40.
            background.setPrefSize(CONTAINER_SIZE, CONTAINER_SIZE);
            background.setMaxSize(CONTAINER_SIZE, CONTAINER_SIZE);
            stateLayer.setPrefSize(CONTAINER_SIZE, CONTAINER_SIZE);
            stateLayer.setMaxSize(CONTAINER_SIZE, CONTAINER_SIZE);
            // Paint order: background, ripple, icon, state layer — ripple never covers the icon.
            root.getChildren().addAll(background, ripplePane, iconSlot, stateLayer);
            getChildren().add(root);
            syncGraphic();
            graphicListener = (obs, oldV, newV) -> syncGraphic();
            control.graphicProperty().addListener(graphicListener);
            Rectangle clip = new Rectangle();
            // Full-corner container: the clip follows half the actual height (20px at the 40dp
            // container), agreeing with .m3-icon-button-bg's 20px radius.
            clip.arcWidthProperty().bind(root.heightProperty().divide(2));
            clip.arcHeightProperty().bind(root.heightProperty().divide(2));
            clip.widthProperty().bind(root.widthProperty());
            clip.heightProperty().bind(root.heightProperty());
            ripplePane.setClip(clip);
            StateLayer.bind(control, stateLayer);
            Ripple.attach(control, ripplePane, this::resolveInk);
            MotionUtil.addPressScale(control);
            pressHandler = control::pressBegin;
            releaseHandler = control::pressEnd;
            keyHandler = control::keyActivate;
            // The shared state layer tracks mouse presses only; mirror Space/Enter holds onto a
            // pseudo-class so keyboard activation shows the same pressed feedback.
            keyArmPressed = e -> {
                if (!control.isDisabled()
                        && (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER)) {
                    control.pseudoClassStateChanged(KEY_ARMED_PC, true);
                }
            };
            keyArmReleased = e -> {
                if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER) {
                    control.pseudoClassStateChanged(KEY_ARMED_PC, false);
                }
            };
            keyArmFocusListener = (obs, oldV, focused) -> {
                if (!focused) {
                    control.pseudoClassStateChanged(KEY_ARMED_PC, false);
                }
            };
            control.addEventHandler(MouseEvent.MOUSE_PRESSED, pressHandler);
            control.addEventHandler(MouseEvent.MOUSE_RELEASED, releaseHandler);
            control.addEventHandler(KeyEvent.KEY_PRESSED, keyHandler);
            control.addEventHandler(KeyEvent.KEY_PRESSED, keyArmPressed);
            control.addEventHandler(KeyEvent.KEY_RELEASED, keyArmReleased);
            control.focusedProperty().addListener(keyArmFocusListener);
        }

        @Override
        public void dispose() {
            M3IconButton control = getSkinnable();
            if (control != null) {
                control.removeEventHandler(MouseEvent.MOUSE_PRESSED, pressHandler);
                control.removeEventHandler(MouseEvent.MOUSE_RELEASED, releaseHandler);
                control.removeEventHandler(KeyEvent.KEY_PRESSED, keyHandler);
                control.removeEventHandler(KeyEvent.KEY_PRESSED, keyArmPressed);
                control.removeEventHandler(KeyEvent.KEY_RELEASED, keyArmReleased);
                control.focusedProperty().removeListener(keyArmFocusListener);
                control.pseudoClassStateChanged(KEY_ARMED_PC, false);
                control.graphicProperty().removeListener(graphicListener);
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
