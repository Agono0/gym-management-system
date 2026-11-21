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
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * M3 text field: filled and outlined variants with a floating label, supporting text, optional
 * leading/trailing icons, error state and character counter.
 *
 * <p>See <a href="https://m3.material.io/components/text-fields/overview">M3 text fields</a>. The
 * floating label shrinks to the top when the field has text or focus; supporting text and the
 * counter sit below the input; error state recolors the indicator, label and supporting text.
 */
// Suppressed this-escape (false positives javac 27 cannot see across the module boundary):
// (1) the JavaFX property-bean idiom new Simple*Property(this, ...) - the property
// constructor only stores the bean reference, exactly like every built-in control;
// (2) constructor calls to JavaFX final methods (getStyleClass(), setAccessibleRole())
// - final, but javac does not honor ACC_FINAL for symbols from another module. This
// class's own overridable methods are never called during construction.
@SuppressWarnings("this-escape")
public class M3TextField extends VBox {

    /** Container height in px (spec: 56dp). */
    public static final double CONTAINER_HEIGHT = 56;

    /**
     * Square hit target for leading/trailing icons. 48dp so an icon-only affordance meets the
     * M3 48dp minimum touch target guide on its own (40dp fell short of the guide).
     */
    private static final double ICON_HIT_SIZE = 48;

    private static final javafx.css.PseudoClass OVER_PC = javafx.css.PseudoClass.getPseudoClass("over");

    private final ObjectProperty<TextFieldVariant> variant =
            new SimpleObjectProperty<>(this, "variant", TextFieldVariant.FILLED);
    private final StringProperty labelText = new SimpleStringProperty(this, "labelText", "");
    private final StringProperty supportingText =
            new SimpleStringProperty(this, "supportingText", "");
    private final BooleanProperty error = new SimpleBooleanProperty(this, "error", false);
    private final IntegerProperty maxLength = new SimpleIntegerProperty(this, "maxLength", -1);

    private final Label floatingLabel = new Label();
    private final TextField input = new TextField();
    /** Alternate input (password-reveal swap) participating in the float state, may be null. */
    private TextField alternateInput;
    private javafx.beans.value.ChangeListener<String> alternateTextListener;
    private javafx.beans.value.ChangeListener<Boolean> alternateFocusListener;
    private final Label supportLabel = new Label();
    private final Label counterLabel = new Label();
    /** The 56dp container: input content with the floating label overlaid inside it. */
    private final StackPane inputBox = new StackPane();
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
        // M3 selection colors (secondary-container highlight, on-secondary-container text) come
        // from the .m3-text-field-input rule in m3fx.css; no inline style.
        inputRow.getChildren().add(input);
        HBox.setHgrow(input, Priority.ALWAYS);
        input.textProperty().addListener((obs, oldV, newV) -> refreshStates());
        input.focusedProperty().addListener((obs, oldV, newV) -> refreshStates());

        // B11: the label lives INSIDE the 56dp container (centered at rest, floated to the top
        // via the :floated CSS), overlaying the input - spec resting position for both variants.
        inputBox.getChildren().addAll(inputRow, floatingLabel);
        inputBox.getStyleClass().add("m3-text-field-box");
        StackPane.setAlignment(floatingLabel, Pos.CENTER_LEFT);
        // Spec container height: 56dp for both filled and outlined variants.
        inputBox.setMinHeight(CONTAINER_HEIGHT);

        // Supporting text left, counter pinned to the end (spec layout).
        Region counterSpacer = new Region();
        HBox.setHgrow(counterSpacer, Priority.ALWAYS);
        HBox bottomRow = new HBox(supportLabel, counterSpacer, counterLabel);
        bottomRow.getStyleClass().add("m3-text-field-bottom");
        supportLabel.getStyleClass().add("m3-text-field-support");
        supportLabel.textProperty().bind(supportingTextProperty());
        counterLabel.getStyleClass().add("m3-text-field-counter");

        setLabelText(labelText);
        setSpacing(0);
        setPadding(Insets.EMPTY);
        getChildren().addAll(inputBox, bottomRow);

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

    /** Returns the text property of the input. */
    public StringProperty textProperty() {
        return input.textProperty();
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
     * Sets the leading icon, wrapped in a 48px square hit target (the M3 minimum touch target)
     * so it stays comfortably touchable inside the 56px container.
     *
     * @param icon icon node, may be {@code null} to remove
     */
    public void setLeadingIcon(Node icon) {
        inputRow.getChildren().removeIf(n -> "m3-text-field-leading".equals(n.getId()));
        if (icon != null) {
            inputRow.getChildren().add(0, iconHitArea(icon, "m3-text-field-leading"));
        }
        // The resting/floated label aligns with the input text, which starts after a leading
        // icon (48px hit target).
        floatingLabel.setTranslateX(icon == null ? 0 : ICON_HIT_SIZE);
    }

    /**
     * Sets the trailing icon, wrapped in a 48px square hit target (the M3 minimum touch target)
     * so attached handlers (clear button, password toggle...) stay comfortably touchable.
     *
     * @param icon icon node, may be {@code null} to remove
     */
    public void setTrailingIcon(Node icon) {
        inputRow.getChildren().removeIf(n -> "m3-text-field-trailing".equals(n.getId()));
        if (icon != null) {
            inputRow.getChildren().add(iconHitArea(icon, "m3-text-field-trailing"));
        }
    }

    /** Wraps an icon in a fixed square hit target, keeping the icon visually centered. */
    private Node iconHitArea(Node icon, String id) {
        javafx.scene.layout.StackPane hit = new javafx.scene.layout.StackPane(icon);
        hit.setId(id);
        hit.getStyleClass().add(id);
        hit.setMinSize(ICON_HIT_SIZE, ICON_HIT_SIZE);
        hit.setPrefSize(ICON_HIT_SIZE, ICON_HIT_SIZE);
        hit.setMaxSize(ICON_HIT_SIZE, ICON_HIT_SIZE);
        hit.setAlignment(Pos.CENTER);
        return hit;
    }

    private void refreshStates() {
        // The password-reveal swap pattern mirrors text between the primary input and an
        // alternate TextField; the float state must consider whichever input is live.
        boolean floated = isInputFocused() || isAnyInputNonEmpty();
        pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("floated"), floated);
        // B11: the label moves between the resting center and the container top in code -
        // Node.translateY is not CSS-styleable and StackPane child constraints conflict
        // with Label's own -fx-alignment, so the CSS drives color/size only.
        floatingLabel.setTranslateY(floated ? -13 : 0);
        pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("input-focused"), isInputFocused());
        if (getMaxLength() > 0) {
            int length = input.getText() == null ? 0 : input.getText().length();
            counterLabel.setText(length + " / " + getMaxLength());
            // Input past the limit is truncated immediately, so "at limit" is the reachable
            // exceeded state: hard-truncation blocks further typing, and the counter flags it in
            // the error color (M3 text-field spec: counter turns error color when exceeded).
            boolean atOrOverLimit = length >= getMaxLength();
            // The :over pseudo-class drives the .m3-text-field-counter:over error-color rule
            // in m3fx.css.
            counterLabel.pseudoClassStateChanged(OVER_PC, atOrOverLimit);
            if (length > getMaxLength()) {
                input.setText(input.getText().substring(0, getMaxLength()));
                input.positionCaret(getMaxLength());
            }
        } else {
            counterLabel.setText("");
            counterLabel.pseudoClassStateChanged(OVER_PC, false);
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

    /**
     * Registers an alternate text input (e.g. the revealed side of a password show/hide swap)
     * whose focus and text participate in the float state alongside the primary input. Style
     * the alternate with the {@code m3-text-field-input} style class so it inherits the token
     * styling, and call {@code getInput().setText(...)} through the normal text mirroring to
     * keep both sides in sync.
     *
     * @param alternate the swapped-in input; {@code null} clears the tracking
     */
    public void trackAlternateInput(TextField alternate) {
        if (alternateInput != null) {
            alternateInput.textProperty().removeListener(alternateTextListener);
            alternateInput.focusedProperty().removeListener(alternateFocusListener);
        }
        alternateInput = alternate;
        if (alternate != null) {
            alternateTextListener = (obs, ov, nv) -> refreshStates();
            alternateFocusListener = (obs, ov, nv) -> refreshStates();
            alternate.textProperty().addListener(alternateTextListener);
            alternate.focusedProperty().addListener(alternateFocusListener);
        }
        refreshStates();
    }

    private boolean isInputFocused() {
        return input.isFocused() || (alternateInput != null && alternateInput.isFocused());
    }

    private boolean isAnyInputNonEmpty() {
        return nonEmpty(input) || (alternateInput != null && nonEmpty(alternateInput));
    }

    private static boolean nonEmpty(TextField field) {
        return field.getText() != null && !field.getText().isEmpty();
    }
}
