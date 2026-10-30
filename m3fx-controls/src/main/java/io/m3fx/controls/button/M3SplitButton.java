package io.m3fx.controls.button;

import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.IconButtonVariant;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.internal.SpringAnimation;
import io.m3fx.controls.menu.M3Menu;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/**
 * M3 Expressive Split Button: connects a primary action button with a dropdown menu button.
 *
 * <p>See <a href="https://m3.material.io/components/buttons/overview">M3 Split Buttons</a>.
 * The leading button triggers the primary action; the trailing button triggers an attached
 * {@link M3Menu} with animated chevron rotation driven by {@link SpringAnimation}.
 */
public class M3SplitButton extends HBox {

    /** Split button sizing options. */
    public enum SplitButtonSize {
        SMALL(36, 18, 13),
        MEDIUM(40, 20, 14),
        LARGE(48, 24, 16);

        private final double height;
        private final double iconSize;
        private final double fontSize;

        SplitButtonSize(double height, double iconSize, double fontSize) {
            this.height = height;
            this.iconSize = iconSize;
            this.fontSize = fontSize;
        }

        /** Returns the total control height in px. */
        public double getHeight() {
            return height;
        }

        /** Returns the icon size in px. */
        public double getIconSize() {
            return iconSize;
        }

        /** Returns the label font size in px. */
        public double getFontSize() {
            return fontSize;
        }
    }

    private final ObjectProperty<ButtonVariant> variant =
            new SimpleObjectProperty<>(this, "variant", ButtonVariant.FILLED);
    private final ObjectProperty<SplitButtonSize> size =
            new SimpleObjectProperty<>(this, "size", SplitButtonSize.MEDIUM);
    private final StringProperty text = new SimpleStringProperty(this, "text", "");
    private final ObjectProperty<Node> leadingIcon = new SimpleObjectProperty<>(this, "leadingIcon");
    private final ObjectProperty<M3Menu> menu = new SimpleObjectProperty<>(this, "menu");

    private final M3Button leadingButton;
    private final Region divider = new Region();
    private final M3IconButton trailingButton;
    private final M3Icon chevronIcon = M3Icon.symbol("arrow_drop_down");

    /** Creates an empty split button. */
    public M3SplitButton() {
        this("", ButtonVariant.FILLED, null);
    }

    /**
     * Creates a split button with text, variant, and attached menu.
     *
     * @param text label text
     * @param variant visual variant
     * @param menu attached dropdown menu
     */
    public M3SplitButton(String text, ButtonVariant variant, M3Menu menu) {
        ButtonVariant safeVariant = variant == null ? ButtonVariant.FILLED : variant;
        getStyleClass().addAll("m3-split-button", "m3-split-button-" + safeVariant.name().toLowerCase());
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(0);
        setMinWidth(Region.USE_PREF_SIZE);

        this.leadingButton = new M3Button(text, safeVariant);
        this.leadingButton.getStyleClass().add("m3-split-button-leading");
        this.leadingButton.textProperty().bind(this.text);

        this.divider.getStyleClass().add("m3-split-button-divider");
        this.divider.setMouseTransparent(true);

        IconButtonVariant iconVariant = toIconButtonVariant(safeVariant);
        this.trailingButton = new M3IconButton(chevronIcon, iconVariant);
        this.trailingButton.getStyleClass().add("m3-split-button-trailing");
        this.trailingButton.setAccessibleText("Open more options");

        getChildren().addAll(leadingButton, divider, trailingButton);

        setText(text);
        setVariant(safeVariant);
        setMenu(menu);

        leadingIcon.addListener((obs, oldV, newV) -> leadingButton.setGraphic(newV));
        this.variant.addListener((obs, oldV, newV) -> {
            getStyleClass().remove("m3-split-button-" + oldV.name().toLowerCase());
            getStyleClass().add("m3-split-button-" + newV.name().toLowerCase());
            leadingButton.setVariant(newV);
            trailingButton.setVariant(toIconButtonVariant(newV));
        });

        this.size.addListener((obs, oldV, newV) -> updateSize(newV));
        updateSize(getSize());

        trailingButton.setOnAction(e -> toggleMenu());
    }

    /** Creates a filled split button. */
    public static M3SplitButton filled(String text, M3Menu menu) {
        return new M3SplitButton(text, ButtonVariant.FILLED, menu);
    }

    /** Creates a tonal split button. */
    public static M3SplitButton tonal(String text, M3Menu menu) {
        return new M3SplitButton(text, ButtonVariant.TONAL, menu);
    }

    /** Creates an outlined split button. */
    public static M3SplitButton outlined(String text, M3Menu menu) {
        return new M3SplitButton(text, ButtonVariant.OUTLINED, menu);
    }

    /** Creates an elevated split button. */
    public static M3SplitButton elevated(String text, M3Menu menu) {
        return new M3SplitButton(text, ButtonVariant.ELEVATED, menu);
    }

    private void updateSize(SplitButtonSize s) {
        double h = s.getHeight();
        setMinHeight(h);
        setPrefHeight(h);
        setMaxHeight(h);
        leadingButton.setMinHeight(h);
        leadingButton.setPrefHeight(h);
        leadingButton.setMaxHeight(h);
        trailingButton.setMinHeight(h);
        trailingButton.setPrefHeight(h);
        trailingButton.setMaxHeight(h);
        trailingButton.setMinWidth(h * 0.9);
        trailingButton.setPrefWidth(h * 0.9);
        trailingButton.setMaxWidth(h * 0.9);
    }

    private void toggleMenu() {
        M3Menu m = getMenu();
        if (m == null) {
            return;
        }
        if (m.isShowing()) {
            m.hide();
            SpringAnimation.animate(chevronIcon.rotateProperty(), 0.0, SpringAnimation.DEFAULT, null);
        } else {
            SpringAnimation.animate(chevronIcon.rotateProperty(), 180.0, SpringAnimation.BOUNCY, null);
            m.popup().setOnHidden(e ->
                SpringAnimation.animate(chevronIcon.rotateProperty(), 0.0, SpringAnimation.DEFAULT, null));
            m.showBelow(this);
        }
    }

    private static IconButtonVariant toIconButtonVariant(ButtonVariant v) {
        return switch (v) {
            case FILLED -> IconButtonVariant.FILLED;
            case TONAL -> IconButtonVariant.TONAL;
            case OUTLINED -> IconButtonVariant.OUTLINED;
            case ELEVATED -> IconButtonVariant.FILLED;
            case TEXT -> IconButtonVariant.STANDARD;
        };
    }

    /** Sets primary action handler. */
    public void setOnAction(EventHandler<ActionEvent> handler) {
        leadingButton.setOnAction(handler);
    }

    /** Returns primary action handler. */
    public EventHandler<ActionEvent> getOnAction() {
        return leadingButton.getOnAction();
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
     * Sets the variant (restyles both halves; {@code null} falls back to {@code FILLED}).
     *
     * @param variant the new variant
     */
    public void setVariant(ButtonVariant variant) {
        this.variant.set(variant == null ? ButtonVariant.FILLED : variant);
    }

    /** Returns the size property. */
    public ObjectProperty<SplitButtonSize> sizeProperty() {
        return size;
    }

    /** Returns the size. */
    public SplitButtonSize getSize() {
        return size.get();
    }

    /**
     * Sets the size (applies height to both halves).
     *
     * @param size the new size
     */
    public void setSize(SplitButtonSize size) {
        this.size.set(size);
    }

    /** Returns the label text property. */
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
        this.text.set(text);
    }

    /** Returns the leading icon property. */
    public ObjectProperty<Node> leadingIconProperty() {
        return leadingIcon;
    }

    /** Returns the leading icon. */
    public Node getLeadingIcon() {
        return leadingIcon.get();
    }

    /**
     * Sets the leading icon shown in the primary half.
     *
     * @param icon icon node, may be {@code null}
     */
    public void setLeadingIcon(Node icon) {
        this.leadingIcon.set(icon);
    }

    /** Returns the attached menu property. */
    public ObjectProperty<M3Menu> menuProperty() {
        return menu;
    }

    /** Returns the attached menu. */
    public M3Menu getMenu() {
        return menu.get();
    }

    /**
     * Attaches the dropdown menu shown by the trailing half.
     *
     * @param menu the menu, may be {@code null}
     */
    public void setMenu(M3Menu menu) {
        this.menu.set(menu);
    }

    /** Returns the primary action half. */
    public M3Button getLeadingButton() {
        return leadingButton;
    }

    /** Returns the dropdown trigger half. */
    public M3IconButton getTrailingButton() {
        return trailingButton;
    }
}

