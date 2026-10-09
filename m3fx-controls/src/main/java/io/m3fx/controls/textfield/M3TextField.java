package io.m3fx.controls.textfield;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * M3 text field: filled and outlined variants with a floating label, supporting text, optional
 * leading/trailing icons, error state and character counter.
 *
 * <p>See <a href="https://m3.material.io/components/text-fields/overview">M3 text fields</a>. The
 * floating label shrinks to the top when the field has text or focus; supporting text and the
 * counter sit below the input; error state recolors the indicator, label and supporting text.
 */
public class M3TextField extends VBox {

    private final ObjectProperty<TextFieldVariant> variant =
            new SimpleObjectProperty<>(this, "variant", TextFieldVariant.FILLED);
    private final StringProperty labelText = new SimpleStringProperty(this, "labelText", "");
    private final StringProperty supportingText =
            new SimpleStringProperty(this, "supportingText", "");
    private final BooleanProperty error = new SimpleBooleanProperty(this, "error", false);
    private final IntegerProperty maxLength = new SimpleIntegerProperty(this, "maxLength", -1);

    private final Label floatingLabel = new Label();
    private final TextField input = new TextField();
    private final Label supportLabel = new Label();
    private final Label counterLabel = new Label();
    private final HBox inputRow = new HBox();

    /** Creates a filled text field. */
    public M3TextField() {
        this("", TextFieldVariant.FILLED);
    }

    /**
     * Creates a text field with a label and variant.
     *
     * @param labelText the floating label text
     * @param variant the variant
     */
    public M3TextField(String labelText, TextFieldVariant variant) {
        getStyleClass().add("m3-text-field");
        setVariant(variant);
        variantProperty().addListener((obs, oldV, newV) -> updateStyleClass(oldV, newV));

        floatingLabel.getStyleClass().add("m3-text-field-label");
        floatingLabel.textProperty().bind(labelTextProperty());
        floatingLabel.setMouseTransparent(true);

        input.getStyleClass().add("m3-text-field-input");
        inputRow.getChildren().add(input);
        inputRow.getStyleClass().add("m3-text-field-box");
        HBox.setHgrow(input, Priority.ALWAYS);
        input.textProperty().addListener((obs, oldV, newV) -> refreshStates());
        input.focusedProperty().addListener((obs, oldV, newV) -> refreshStates());

        HBox bottomRow = new HBox(supportLabel, counterLabel);
        bottomRow.getStyleClass().add("m3-text-field-bottom");
        supportLabel.getStyleClass().add("m3-text-field-support");
        supportLabel.textProperty().bind(supportingTextProperty());
        counterLabel.getStyleClass().add("m3-text-field-counter");

        setLabelText(labelText);
        setSpacing(0);
        setPadding(Insets.EMPTY);
        getChildren().addAll(floatingLabel, inputRow, bottomRow);

        errorProperty().addListener((obs, oldV, isError) -> {
            pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("error"), isError);
            refreshStates();
        });
        setFocusTraversable(false);
        refreshStates();
    }

    /** Returns the inner JavaFX text input (for prompt text, bindings, tests). */
    public TextField getInput() {
        return input;
    }

    /** Returns the variant property. */
    public ObjectProperty<TextFieldVariant> variantProperty() {
        return variant;
    }

    /** Returns the variant. */
    public TextFieldVariant getVariant() {
        return variant.get();
    }

    /**
     * Sets the variant.
     *
     * @param variant the new variant
     */
    public void setVariant(TextFieldVariant variant) {
        TextFieldVariant old = this.variant.get();
        this.variant.set(variant == null ? TextFieldVariant.FILLED : variant);
        updateStyleClass(old, this.variant.get());
    }

    /** Returns the floating label text property. */
    public StringProperty labelTextProperty() {
        return labelText;
    }

    /** Returns the floating label text. */
    public String getLabelText() {
        return labelText.get();
    }

    /**
     * Sets the floating label text.
     *
     * @param labelText label text
     */
    public void setLabelText(String labelText) {
        this.labelText.set(labelText == null ? "" : labelText);
    }

    /** Returns the supporting text property. */
    public StringProperty supportingTextProperty() {
        return supportingText;
    }

    /** Returns the supporting text. */
    public String getSupportingText() {
        return supportingText.get();
    }

    /**
     * Sets the supporting text shown below the input.
     *
     * @param supportingText supporting text
     */
    public void setSupportingText(String supportingText) {
        this.supportingText.set(supportingText == null ? "" : supportingText);
    }

    /** Returns the error property. */
    public BooleanProperty errorProperty() {
        return error;
    }

    /** Returns whether the field is in error state. */
    public boolean isError() {
        return error.get();
    }

    /**
     * Sets the error state.
     *
     * @param error {@code true} for error styling
     */
    public void setError(boolean error) {
        this.error.set(error);
    }

    /** Returns the max length property (-1 = unlimited). */
    public IntegerProperty maxLengthProperty() {
        return maxLength;
    }

    /** Returns the max length. */
    public int getMaxLength() {
        return maxLength.get();
    }

    /**
     * Sets the max length; the counter shows {@code length/max}.
     *
     * @param maxLength max characters, or -1 for unlimited
     */
    public void setMaxLength(int maxLength) {
        this.maxLength.set(maxLength);
        refreshStates();
    }

    /** Returns the current text. */
    public String getText() {
        return input.getText();
    }

    /**
     * Sets the current text.
     *
     * @param text text
     */
    public void setText(String text) {
        input.setText(text);
    }

    /**
     * Sets the leading icon.
     *
     * @param icon icon node
     */
    public void setLeadingIcon(Node icon) {
        inputRow.getChildren().removeIf(n -> "m3-text-field-leading".equals(n.getId()));
        if (icon != null) {
            icon.setId("m3-text-field-leading");
            inputRow.getChildren().add(0, icon);
        }
    }

    /**
     * Sets the trailing icon.
     *
     * @param icon icon node
     */
    public void setTrailingIcon(Node icon) {
        inputRow.getChildren().removeIf(n -> "m3-text-field-trailing".equals(n.getId()));
        if (icon != null) {
            icon.setId("m3-text-field-trailing");
            inputRow.getChildren().add(icon);
        }
    }

    private void refreshStates() {
        boolean floated = input.isFocused() || (input.getText() != null && !input.getText().isEmpty());
        pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("floated"), floated);
        pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("input-focused"), input.isFocused());
        if (getMaxLength() > 0) {
            counterLabel.setText(input.getText().length() + " / " + getMaxLength());
            if (input.getText().length() > getMaxLength()) {
                input.setText(input.getText().substring(0, getMaxLength()));
                input.positionCaret(getMaxLength());
            }
        } else {
            counterLabel.setText("");
        }
    }

    private void updateStyleClass(TextFieldVariant oldV, TextFieldVariant newV) {
        if (oldV != null) {
            getStyleClass().remove("m3-text-field-" + oldV.name().toLowerCase());
        }
        if (newV != null && !getStyleClass().contains("m3-text-field-" + newV.name().toLowerCase())) {
            getStyleClass().add("m3-text-field-" + newV.name().toLowerCase());
        }
    }
}
