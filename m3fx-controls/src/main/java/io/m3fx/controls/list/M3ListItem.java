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
import javafx.scene.layout.VBox;

/**
 * M3 list item: one/two/three-line rows with leading and trailing content.
 *
 * <p>See <a href="https://m3.material.io/components/lists/overview">M3 lists</a>. One-line 56dp,
 * two-line 72dp, three-line 88dp (container heights from {@code _md-comp-list.scss}); adding
 * supporting or overline text grows the height even when the item was built as one-line. Leading
 * and trailing content are optional: when absent their slots are unmanaged so no dead spacing is
 * reserved (token leading/trailing space is 16dp).
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
    private int lines = ONE_LINE;

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
        // Optional leading/trailing slots collapse when empty instead of reserving spacing.
        leadingBox.setManaged(getLeading() != null);
        trailingBox.setManaged(getTrailing() != null);
        applyHeight();
    }

    /** Applies the M3 container height for the effective line count (56/72/88dp). */
    private void applyHeight() {
        int natural = ONE_LINE;
        if (!getSupporting().isEmpty()) {
            natural++;
        }
        if (!getOverline().isEmpty()) {
            natural++;
        }
        int effective = Math.min(THREE_LINES, Math.max(lines, natural));
        setMinHeight(switch (effective) {
            case THREE_LINES -> 88;
            case TWO_LINES -> 72;
            default -> 56;
        });
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
        this.lines = Math.min(THREE_LINES, Math.max(ONE_LINE, lines));
        pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("two-lines"), this.lines >= TWO_LINES);
        pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("three-lines"), this.lines == THREE_LINES);
        applyHeight();
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
     * Sets the leading content (icon, avatar, image); {@code null} collapses the leading slot.
     *
     * @param leading leading node, may be {@code null}
     */
    public void setLeading(Node leading) {
        this.leading.set(leading);
        if (leading == null) {
            leadingBox.getChildren().clear();
        } else {
            leadingBox.getChildren().setAll(leading);
        }
        leadingBox.setManaged(leading != null);
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
     * Sets the trailing content (checkbox, switch, text, icon); {@code null} collapses the
     * trailing slot.
     *
     * @param trailing trailing node, may be {@code null}
     */
    public void setTrailing(Node trailing) {
        this.trailing.set(trailing);
        if (trailing == null) {
            trailingBox.getChildren().clear();
        } else {
            trailingBox.getChildren().setAll(trailing);
        }
        trailingBox.setManaged(trailing != null);
    }
}
