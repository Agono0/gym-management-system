package io.m3fx.controls.list;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * M3 list item: one/two/three-line rows with leading and trailing content.
 *
 * <p>See <a href="https://m3.material.io/components/lists/overview">M3 lists</a>. One-line 56dp
 * (with icon) / 56dp, two-line 72dp, three-line 88dp; headline/body/supporting type roles;
 * 16dp... actually 24dp leading padding with icon, 16dp without.
 */
public class M3ListItem extends HBox {

    /** Number of text lines (1-3). */
    public static final int ONE_LINE = 1;
    /** Two-line variant. */
    public static final int TWO_LINES = 2;
    /** Three-line variant. */
    public static final int THREE_LINES = 3;

    private final StringProperty headline = new SimpleStringProperty(this, "headline", "");
    private final StringProperty supporting = new SimpleStringProperty(this, "supporting", "");
    private final StringProperty overline = new SimpleStringProperty(this, "overline", "");

    private final Label overlineLabel = new Label();
    private final Label headlineLabel = new Label();
    private final Label supportingLabel = new Label();
    private final HBox leadingBox = new HBox();
    private final HBox trailingBox = new HBox();
    private final VBox textBox = new VBox(2);

    /** Creates a one-line item. */
    public M3ListItem() {
        this("", "", ONE_LINE);
    }

    /**
     * Creates an item.
     *
     * @param headline headline text
     * @param supporting supporting text (empty for one-line)
     * @param lines 1-3
     */
    public M3ListItem(String headline, String supporting, int lines) {
        getStyleClass().add("m3-list-item");
        leadingBox.getStyleClass().add("m3-list-leading");
        trailingBox.getStyleClass().add("m3-list-trailing");
        overlineLabel.getStyleClass().add("m3-list-overline");
        headlineLabel.getStyleClass().add("m3-list-headline");
        supportingLabel.getStyleClass().add("m3-list-supporting");
        overlineLabel.textProperty().bind(overlineProperty());
        headlineLabel.textProperty().bind(headlineProperty());
        supportingLabel.textProperty().bind(supportingProperty());
        textBox.getChildren().addAll(overlineLabel, headlineLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);
        setHeadline(headline);
        setSupporting(supporting);
        setLines(lines);
        rebuild();
    }

    private void rebuild() {
        getChildren().setAll(leadingBox, textBox, trailingBox);
        boolean twoOrThree = textBox.getChildren().contains(supportingLabel);
        String support = getSupporting();
        boolean needsSupport = support != null && !support.isEmpty();
        if (needsSupport && !twoOrThree) {
            textBox.getChildren().add(supportingLabel);
        } else if (!needsSupport && twoOrThree) {
            textBox.getChildren().remove(supportingLabel);
        }
        boolean needsOverline = getOverline() != null && !getOverline().isEmpty();
        boolean hasOverline = textBox.getChildren().contains(overlineLabel);
        if (needsOverline && !hasOverline) {
            textBox.getChildren().add(0, overlineLabel);
        } else if (!needsOverline && hasOverline) {
            textBox.getChildren().remove(overlineLabel);
        }
    }

    /** Returns the headline property. */
    public StringProperty headlineProperty() {
        return headline;
    }

    /** Returns the headline. */
    public String getHeadline() {
        return headline.get();
    }

    /**
     * Sets the headline.
     *
     * @param headline headline text
     */
    public void setHeadline(String headline) {
        this.headline.set(headline == null ? "" : headline);
        rebuild();
    }

    /** Returns the supporting property. */
    public StringProperty supportingProperty() {
        return supporting;
    }

    /** Returns the supporting text. */
    public String getSupporting() {
        return supporting.get();
    }

    /**
     * Sets the supporting text.
     *
     * @param supporting supporting text
     */
    public void setSupporting(String supporting) {
        this.supporting.set(supporting == null ? "" : supporting);
        rebuild();
    }

    /** Returns the overline property. */
    public StringProperty overlineProperty() {
        return overline;
    }

    /** Returns the overline text. */
    public String getOverline() {
        return overline.get();
    }

    /**
     * Sets the overline text.
     *
     * @param overline overline text
     */
    public void setOverline(String overline) {
        this.overline.set(overline == null ? "" : overline);
        rebuild();
    }

    /**
     * Sets the line count (affects min height: 56/72/88dp).
     *
     * @param lines 1-3
     */
    public void setLines(int lines) {
        int clamped = Math.min(3, Math.max(1, lines));
        pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("two-lines"), clamped >= 2);
        pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("three-lines"), clamped == 3);
        setMinHeight(clamped == 1 ? 56 : clamped == 2 ? 72 : 88);
    }

    /** Leading content property (FXML-settable). */
    private final ObjectProperty<Node> leading = new SimpleObjectProperty<>(this, "leading");
    /** Trailing content property (FXML-settable). */
    private final ObjectProperty<Node> trailing = new SimpleObjectProperty<>(this, "trailing");

    /** Returns the leading content property. */
    public ObjectProperty<Node> leadingProperty() {
        return leading;
    }

    /** Returns the leading content, may be {@code null}. */
    public Node getLeading() {
        return leading.get();
    }

    /**
     * Sets the leading content (icon, avatar, image).
     *
     * @param leading leading node, may be {@code null}
     */
    public void setLeading(Node leading) {
        this.leading.set(leading);
        leadingBox.getChildren().setAll(leading == null ? new Region() : leading);
    }

    /** Returns the trailing content property. */
    public ObjectProperty<Node> trailingProperty() {
        return trailing;
    }

    /** Returns the trailing content, may be {@code null}. */
    public Node getTrailing() {
        return trailing.get();
    }

    /**
     * Sets the trailing content (checkbox, switch, text, icon).
     *
     * @param trailing trailing node, may be {@code null}
     */
    public void setTrailing(Node trailing) {
        this.trailing.set(trailing);
        trailingBox.getChildren().setAll(trailing == null ? new Region() : trailing);
    }
}
