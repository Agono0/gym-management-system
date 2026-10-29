package io.m3fx.controls.chip;

import io.m3fx.controls.icon.M3Icon;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

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
    private final ObjectProperty<Runnable> onClose = new SimpleObjectProperty<>(this, "onClose");

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
        if (this.variant.get() == ChipVariant.INPUT) {
            setClosable(true);
        }
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

    /** Returns the on-close callback property. */
    public ObjectProperty<Runnable> onCloseProperty() {
        return onClose;
    }

    /** Returns the on-close callback. */
    public Runnable getOnClose() {
        return onClose.get();
    }

    /**
     * Sets the on-close callback run when the close affordance is clicked.
     *
     * @param onClose the callback
     */
    public void setOnClose(Runnable onClose) {
        this.onClose.set(onClose);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new ChipSkin(this);
    }

    private void updateStyleClass(ChipVariant oldV, ChipVariant newV) {
        if (oldV != null) {
            getStyleClass().remove("m3-chip-" + oldV.name().toLowerCase());
        }
        if (newV != null && !getStyleClass().contains("m3-chip-" + newV.name().toLowerCase())) {
            getStyleClass().add("m3-chip-" + newV.name().toLowerCase());
        }
    }

    /** Skin laying out leading icon, chip label and trailing close affordance. */
    static final class ChipSkin extends SkinBase<M3Chip> {
        private final HBox root = new HBox(6);
        private final StackPane leadingSlot = new StackPane();
        private final Label textLabel = new Label();
        private final StackPane closeSlot = new StackPane();
        private final javafx.beans.value.ChangeListener<Node> graphicListener;
        private final javafx.beans.value.ChangeListener<Boolean> closableListener;
        private final javafx.beans.value.ChangeListener<Boolean> selectedListener;
        private final javafx.beans.value.ChangeListener<ChipVariant> variantListener;

        ChipSkin(M3Chip chip) {
            super(chip);
            root.setAlignment(Pos.CENTER);
            root.getStyleClass().add("m3-chip-content");
            root.setMouseTransparent(false);

            leadingSlot.getStyleClass().add("m3-chip-leading");
            textLabel.getStyleClass().add("m3-chip-text");
            textLabel.textProperty().bind(chip.textProperty());
            textLabel.textFillProperty().bind(chip.textFillProperty());
            textLabel.fontProperty().bind(chip.fontProperty());

            M3Icon closeIcon = M3Icon.symbol("close");
            closeIcon.setScaleX(0.7);
            closeIcon.setScaleY(0.7);
            closeSlot.getStyleClass().add("m3-chip-close");
            closeSlot.getChildren().add(closeIcon);
            closeSlot.setCursor(javafx.scene.Cursor.HAND);

            closeSlot.addEventHandler(MouseEvent.MOUSE_PRESSED, javafx.event.Event::consume);
            closeSlot.addEventHandler(MouseEvent.MOUSE_RELEASED, javafx.event.Event::consume);
            closeSlot.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                e.consume();
                Runnable onClose = chip.getOnClose();
                if (onClose != null) {
                    onClose.run();
                }
            });

            root.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                if (!chip.isDisabled()) {
                    chip.fire();
                }
            });

            graphicListener = (o, ov, nv) -> updateSlots(chip);
            closableListener = (o, ov, nv) -> updateSlots(chip);
            selectedListener = (o, ov, nv) -> updateSlots(chip);
            variantListener = (o, ov, nv) -> updateSlots(chip);
            chip.graphicProperty().addListener(graphicListener);
            chip.closableProperty().addListener(closableListener);
            chip.selectedProperty().addListener(selectedListener);
            chip.variantProperty().addListener(variantListener);

            updateSlots(chip);
            root.getChildren().addAll(leadingSlot, textLabel, closeSlot);
            getChildren().add(root);
        }

        @Override
        public void dispose() {
            M3Chip chip = getSkinnable();
            if (chip != null) {
                chip.graphicProperty().removeListener(graphicListener);
                chip.closableProperty().removeListener(closableListener);
                chip.selectedProperty().removeListener(selectedListener);
                chip.variantProperty().removeListener(variantListener);
                textLabel.textProperty().unbind();
                textLabel.textFillProperty().unbind();
                textLabel.fontProperty().unbind();
            }
            super.dispose();
        }

        private void updateSlots(M3Chip chip) {
            // Selected filter chips replace any leading icon with the spec checkmark (18px box).
            boolean showCheck = chip.getVariant() == ChipVariant.FILTER && chip.isSelected();
            if (showCheck) {
                M3Icon check = M3Icon.symbol("check");
                check.setMinSize(18, 18);
                check.setPrefSize(18, 18);
                check.setMaxSize(18, 18);
                leadingSlot.getChildren().setAll(check);
                leadingSlot.setVisible(true);
                leadingSlot.setManaged(true);
                return;
            }
            Node g = chip.getGraphic();
            leadingSlot.getChildren().clear();
            if (g != null) {
                leadingSlot.getChildren().add(g);
                leadingSlot.setVisible(true);
                leadingSlot.setManaged(true);
            } else {
                leadingSlot.setVisible(false);
                leadingSlot.setManaged(false);
            }

            boolean isClose = chip.isClosable();
            closeSlot.setVisible(isClose);
            closeSlot.setManaged(isClose);
        }
    }
}
