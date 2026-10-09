package io.m3fx.controls.chip;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;

/**
 * M3 chip: assist, filter, input and suggestion variants with icon support and selection state.
 *
 * <p>See <a href="https://m3.material.io/components/chips/overview">M3 chips</a>. Height 32dp,
 * small shape (8dp), label-large type. Filter chips toggle selection; input chips expose a close
 * action; all variants meet the 48dp touch target via padding.
 */
public class M3Chip extends ToggleButton {

    private final ObjectProperty<ChipVariant> variant =
            new SimpleObjectProperty<>(this, "variant", ChipVariant.ASSIST);
    private final BooleanProperty closable = new SimpleBooleanProperty(this, "closable", false);

    /** Creates an assist chip. */
    public M3Chip() {
        this("", ChipVariant.ASSIST);
    }

    /**
     * Creates a chip with text and variant.
     *
     * @param text label text
     * @param variant chip variant
     */
    public M3Chip(String text, ChipVariant variant) {
        super(text == null ? "" : text);
        getStyleClass().add("m3-chip");
        setMinHeight(32);
        setVariant(variant);
        variantProperty().addListener((obs, oldV, newV) -> updateStyleClass(oldV, newV));
        closableProperty().addListener((obs, oldV, nowClosable) -> {
            pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("closable"), nowClosable);
        });
        addEventHandler(javafx.event.ActionEvent.ACTION, e -> {
            if (getVariant() != ChipVariant.FILTER && getVariant() != ChipVariant.INPUT) {
                setSelected(false);
            }
        });
        setFocusTraversable(true);
    }

    /**
     * Creates an assist chip.
     *
     * @param text label text
     * @return the chip
     */
    public static M3Chip assist(String text) {
        return new M3Chip(text, ChipVariant.ASSIST);
    }

    /**
     * Creates a filter chip.
     *
     * @param text label text
     * @return the chip
     */
    public static M3Chip filter(String text) {
        return new M3Chip(text, ChipVariant.FILTER);
    }

    /**
     * Creates an input chip.
     *
     * @param text label text
     * @return the chip
     */
    public static M3Chip input(String text) {
        M3Chip chip = new M3Chip(text, ChipVariant.INPUT);
        chip.setClosable(true);
        return chip;
    }

    /**
     * Creates a suggestion chip.
     *
     * @param text label text
     * @return the chip
     */
    public static M3Chip suggestion(String text) {
        return new M3Chip(text, ChipVariant.SUGGESTION);
    }

    /**
     * Sets the leading icon graphic.
     *
     * @param icon icon node
     * @return this chip, for chaining
     */
    public M3Chip withIcon(Node icon) {
        setGraphic(icon);
        return this;
    }

    /** Returns the variant property. */
    public ObjectProperty<ChipVariant> variantProperty() {
        return variant;
    }

    /** Returns the variant. */
    public ChipVariant getVariant() {
        return variant.get();
    }

    /**
     * Sets the variant.
     *
     * @param variant the new variant
     */
    public void setVariant(ChipVariant variant) {
        ChipVariant old = this.variant.get();
        this.variant.set(variant == null ? ChipVariant.ASSIST : variant);
        updateStyleClass(old, this.variant.get());
    }

    /** Returns the closable property (input chips show a close affordance). */
    public BooleanProperty closableProperty() {
        return closable;
    }

    /** Returns whether the chip is closable. */
    public boolean isClosable() {
        return closable.get();
    }

    /**
     * Sets whether the chip shows a close affordance.
     *
     * @param closable {@code true} for closable
     */
    public void setClosable(boolean closable) {
        this.closable.set(closable);
    }

    private void updateStyleClass(ChipVariant oldV, ChipVariant newV) {
        if (oldV != null) {
            getStyleClass().remove("m3-chip-" + oldV.name().toLowerCase());
        }
        if (newV != null && !getStyleClass().contains("m3-chip-" + newV.name().toLowerCase())) {
            getStyleClass().add("m3-chip-" + newV.name().toLowerCase());
        }
    }
}
