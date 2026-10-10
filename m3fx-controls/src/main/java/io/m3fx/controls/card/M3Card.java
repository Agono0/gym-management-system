package io.m3fx.controls.card;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * M3 card: elevated, filled and outlined variants with clickable support.
 *
 * <p>See <a href="https://m3.material.io/components/cards/overview">M3 cards</a>. Medium shape
 * (12dp), label/body type for content. Clickable cards fire an action on click, Space and Enter.
 */
public class M3Card extends Control {

    private final ObjectProperty<CardVariant> variant =
            new SimpleObjectProperty<>(this, "variant", CardVariant.ELEVATED);
    private final BooleanProperty clickable = new SimpleBooleanProperty(this, "clickable", false);

    /** Content holder filled by the skin. */
    final StackPane contentHolder = new StackPane();

    /** Creates an elevated card. */
    public M3Card() {
        this(CardVariant.ELEVATED);
    }

    /**
     * Creates a card with a variant.
     *
     * @param variant the card variant
     */
    public M3Card(CardVariant variant) {
        getStyleClass().add("m3-card");
        setVariant(variant);
        variantProperty().addListener((obs, oldV, newV) -> updateStyleClass(oldV, newV));
        clickableProperty().addListener((obs, oldV, nowClickable) -> {
            setFocusTraversable(nowClickable);
            pseudoClassStateChanged(
                    javafx.css.PseudoClass.getPseudoClass("clickable"), nowClickable);
        });
        setAccessibleRole(AccessibleRole.NODE);
        setFocusTraversable(false);
    }

    /**
     * Creates a card wrapping content.
     *
     * @param variant the variant
     * @param content the card content
     * @return the card
     */
    public static M3Card of(CardVariant variant, Node content) {
        M3Card card = new M3Card(variant);
        card.setContent(content);
        return card;
    }

    /** Returns the variant property. */
    public ObjectProperty<CardVariant> variantProperty() {
        return variant;
    }

    /** Returns the variant. */
    public CardVariant getVariant() {
        return variant.get();
    }

    /**
     * Sets the variant.
     *
     * @param variant the new variant
     */
    public void setVariant(CardVariant variant) {
        CardVariant old = this.variant.get();
        this.variant.set(variant == null ? CardVariant.ELEVATED : variant);
        updateStyleClass(old, this.variant.get());
    }

    /** Returns the clickable property. */
    public BooleanProperty clickableProperty() {
        return clickable;
    }

    /** Returns whether the card is clickable. */
    public boolean isClickable() {
        return clickable.get();
    }

    /**
     * Sets whether the card reacts to click/keyboard activation like a button.
     *
     * @param clickable {@code true} to make the card clickable
     */
    public void setClickable(boolean clickable) {
        this.clickable.set(clickable);
    }

    /** Content node property (FXML-settable). */
    private final ObjectProperty<Node> content = new SimpleObjectProperty<>(this, "content");

    /** Returns the content property. */
    public ObjectProperty<Node> contentProperty() {
        return content;
    }

    /** Returns the card content, may be {@code null}. */
    public Node getContent() {
        return content.get();
    }

    /**
     * Sets the card content.
     *
     * @param content content node
     */
    public void setContent(Node content) {
        this.content.set(content);
        contentHolder.getChildren().setAll(content);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new CardSkin(this);
    }

    private void updateStyleClass(CardVariant oldV, CardVariant newV) {
        if (oldV != null) {
            getStyleClass().remove("m3-card-" + oldV.name().toLowerCase());
        }
        if (newV != null && !getStyleClass().contains("m3-card-" + newV.name().toLowerCase())) {
            getStyleClass().add("m3-card-" + newV.name().toLowerCase());
        }
    }

    /** Skin laying out the card content with padding, ripple and hover elevation. */
    static final class CardSkin extends SkinBase<M3Card> {
        private final StackPane root = new StackPane();
        private final Pane ripplePane = new Pane();
        private io.m3fx.core.token.ElevationLevel currentElevation =
                io.m3fx.core.token.ElevationLevel.LEVEL1;
        private final javafx.event.EventHandler<javafx.scene.input.MouseEvent> clickHandler;
        private final javafx.event.EventHandler<javafx.scene.input.KeyEvent> keyHandler;

        CardSkin(M3Card card) {
            super(card);
            card.contentHolder.getStyleClass().add("m3-card-content");
            ripplePane.getStyleClass().add("m3-card-ripple");
            // Paint order: ripple under content — the card background shows the ripple,
            // the content text stays on top.
            root.getChildren().addAll(ripplePane, card.contentHolder);
            getChildren().add(root);
            Rectangle clip = new Rectangle();
            clip.setArcWidth(24);
            clip.setArcHeight(24);
            clip.widthProperty().bind(root.widthProperty());
            clip.heightProperty().bind(root.heightProperty());
            ripplePane.setClip(clip);
            io.m3fx.controls.internal.Ripple.attach(
                    card,
                    ripplePane,
                    () -> io.m3fx.controls.internal.ThemeColors.resolve(
                            card, io.m3fx.core.theme.ColorRole.ON_SURFACE));
            card.hoverProperty().addListener((obs, oldV, hover) -> updateElevation());
            card.variantProperty().addListener((obs, oldV, newV) -> updateElevation());
            card.clickableProperty().addListener((obs, oldV, newV) -> updateElevation());
            updateElevation();
            clickHandler = e -> {
                if (card.isClickable()) {
                    card.fireEvent(new javafx.event.ActionEvent(card, null));
                }
            };
            keyHandler = e -> {
                if (card.isClickable()
                        && (e.getCode() == javafx.scene.input.KeyCode.SPACE
                                || e.getCode() == javafx.scene.input.KeyCode.ENTER)) {
                    card.fireEvent(new javafx.event.ActionEvent(card, null));
                    e.consume();
                }
            };
            card.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, clickHandler);
            card.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, keyHandler);
        }

        @Override
        public void dispose() {
            M3Card card = getSkinnable();
            if (card != null) {
                card.removeEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, clickHandler);
                card.removeEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, keyHandler);
                io.m3fx.controls.internal.Ripple.detach(card, ripplePane);
            }
            super.dispose();
        }

        private void updateElevation() {
            M3Card card = getSkinnable();
            io.m3fx.core.token.ElevationLevel next =
                    card.getVariant() == CardVariant.ELEVATED
                            ? ((card.isClickable() && card.isHover())
                                    ? io.m3fx.core.token.ElevationLevel.LEVEL2
                                    : io.m3fx.core.token.ElevationLevel.LEVEL1)
                            : io.m3fx.core.token.ElevationLevel.LEVEL0;
            javafx.scene.paint.Color shadow =
                    io.m3fx.controls.internal.ThemeColors.resolve(
                            card, io.m3fx.core.theme.ColorRole.SHADOW);
            io.m3fx.controls.internal.ElevationShadow.animateTo(
                    card, currentElevation, next, shadow);
            currentElevation = next;
        }
    }
}
