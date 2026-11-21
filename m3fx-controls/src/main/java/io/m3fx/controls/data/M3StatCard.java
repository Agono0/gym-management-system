package io.m3fx.controls.data;

import io.m3fx.controls.card.CardVariant;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.chart.M3Sparkline;
import io.m3fx.controls.effect.M3GlowShadow;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * High-impact M3 Telemetry Metric Card displaying an analytic headline, delta badge,
 * optional embedded sparkline or graphic, and glowing border hover response.
 */
// Suppressed this-escape (false positives javac 27 cannot see across the module boundary):
// (1) the JavaFX property-bean idiom new Simple*Property(this, ...) - the property
// constructor only stores the bean reference, exactly like every built-in control;
// (2) constructor calls to JavaFX final methods (getStyleClass(), setAccessibleRole())
// - final, but javac does not honor ACC_FINAL for symbols from another module. This
// class's own overridable methods are never called during construction.
@SuppressWarnings("this-escape")
public class M3StatCard extends M3Card {

    /** Sane minimum title width in px: below it the wrapped title ellipsizes. */
    static final double TITLE_MIN_WIDTH = 72;
    /** Horizontal insets around the header row in px (card 16+16 CSS + layout box 16+16). */
    static final double CONTENT_HORIZONTAL_INSETS = 64;
    /** Header-row gap between title and badge in px. */
    static final double HEADER_GAP = 8;

    private final StringProperty title = new SimpleStringProperty(this, "title", "");
    private final StringProperty value = new SimpleStringProperty(this, "value", "");
    private final StringProperty deltaText = new SimpleStringProperty(this, "deltaText", "");
    private final BooleanProperty deltaPositive = new SimpleBooleanProperty(this, "deltaPositive", true);
    private final BooleanProperty glowOnHover = new SimpleBooleanProperty(this, "glowOnHover", true);

    private final Label titleLabel = new Label();
    private final Label valueLabel = new Label();
    private final Label deltaBadge = new Label();
    private final HBox headerRow = new HBox(8);
    private final HBox valueRow = new HBox(12);
    private final VBox layoutBox = new VBox(10);
    private M3Sparkline sparkline;

    /** Creates an empty stat card. */
    public M3StatCard() {
        this("", "", "");
    }

    /**
     * Creates a stat card with title and value.
     *
     * @param title metric title
     * @param value large formatted headline value
     */
    public M3StatCard(String title, String value) {
        this(title, value, "");
    }

    /**
     * Creates a stat card with title, value, and delta change percentage.
     *
     * @param title metric title
     * @param value large formatted headline value
     * @param delta delta change string (e.g. "+14.5%")
     */
    public M3StatCard(String title, String value, String delta) {
        super(CardVariant.ELEVATED);
        getStyleClass().add("m3-stat-card");

        titleLabel.getStyleClass().add("m3-stat-title");
        // B10: the title never slivers - it keeps at least the sane minimum width and its
        // full text stays reachable through the title tooltip ("title - delta").
        titleLabel.setMinWidth(TITLE_MIN_WIDTH);
        titleLabel.textProperty().bind(titleProperty());

        valueLabel.getStyleClass().add("m3-stat-value");
        valueLabel.textProperty().bind(valueProperty());

        deltaBadge.getStyleClass().add("m3-stat-delta");
        // Trend indicator: ▲ for positive delta, ▼ for negative, always before the delta text
        deltaBadge.textProperty().bind(Bindings.createStringBinding(
                () -> (isDeltaPositive() ? "\u25B2 " : "\u25BC ") + getDeltaText(),
                deltaTextProperty(), deltaPositiveProperty()));
        // Spoken form replaces the ▲/▼ glyphs with words so screen readers announce the trend.
        deltaBadge.accessibleTextProperty().bind(Bindings.createStringBinding(
                () -> (isDeltaPositive() ? "up " : "down ") + getDeltaText(),
                deltaTextProperty(), deltaPositiveProperty()));
        updateDeltaBadgeStyle(isDeltaPositive());
        deltaPositiveProperty().addListener((obs, o, pos) -> updateDeltaBadgeStyle(pos));

        deltaBadge.managedProperty().bind(deltaBadge.visibleProperty());
        // B10: the badge never shrinks below its full text (no mid-word cuts). When it cannot
        // fit beside the title minimum it hides entirely - its text stays reachable through
        // the title tooltip, which always carries "title — delta".
        deltaBadge.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        deltaBadge.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> deltaTextProperty().get().isEmpty() || deltaBadge.prefWidth(-1)
                        <= Math.max(0, getWidth() - CONTENT_HORIZONTAL_INSETS
                                - TITLE_MIN_WIDTH - HEADER_GAP),
                deltaTextProperty(), deltaBadge.textProperty(), widthProperty(),
                // The CSS font/padding and the label's own skin all change the badge's pref
                // width after the first pulse; without these the fit decision goes stale on
                // pre-CSS zero values (B10).
                deltaBadge.paddingProperty(), deltaBadge.fontProperty(),
                deltaBadge.skinProperty()));
        deltaTextProperty().addListener((obs, o, text) ->
                io.m3fx.controls.common.M3Tooltip.install(deltaBadge, text));
        io.m3fx.controls.common.M3Tooltip.install(deltaBadge, getDeltaText());

        headerRow.setAlignment(Pos.CENTER_LEFT);
        // The title grows to fill the header, pushing the badge to the trailing edge.
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        headerRow.getChildren().addAll(titleLabel, deltaBadge);
        updateTitleTooltip();
        titleProperty().addListener((obs, o, text) -> updateTitleTooltip());
        deltaTextProperty().addListener((obs, o, text) -> updateTitleTooltip());

        valueRow.setAlignment(Pos.CENTER_LEFT);
        valueRow.getChildren().add(valueLabel);

        layoutBox.getChildren().addAll(headerRow, valueRow);
        layoutBox.setPadding(new javafx.geometry.Insets(14, 16, 14, 16));
        setContent(layoutBox);

        setTitle(title);
        setValue(value);
        setDeltaText(delta);

        hoverProperty().addListener((obs, o, hover) -> {
            if (isGlowOnHover()) {
                if (hover) {
                    M3GlowShadow.apply(this, ColorRole.PRIMARY, 16.0, 0.3);
                } else {
                    M3GlowShadow.stopPulse(this);
                }
            }
        });

        setPrefWidth(240);
    }

    private void updateDeltaBadgeStyle(boolean positive) {
        deltaBadge.getStyleClass().removeAll("positive", "negative");
        deltaBadge.getStyleClass().add(positive ? "positive" : "negative");
    }

    /**
     * Keeps the title tooltip current: "title — delta". It is the always-available channel for
     * the full delta text when the badge hides on narrow cards (B10).
     */
    private void updateTitleTooltip() {
        String text = getTitle();
        // Null-safe: callers (e.g. CustomerProfileController's placeholder cards) may pass a
        // null delta — treat it like an empty one instead of NPE-ing the FX thread.
        String delta = getDeltaText();
        if (delta != null && !delta.isEmpty()) {
            text += " — " + delta;
        }
        if (!text.isEmpty()) {
            io.m3fx.controls.common.M3Tooltip.install(titleLabel, text);
        }
    }

    /**
     * Attaches (or replaces) a mini sparkline next to the headline value.
     *
     * @param values sparkline data values
     */
    public void setSparkline(double... values) {
        if (sparkline != null) {
            valueRow.getChildren().remove(sparkline);
        }
        sparkline = M3Sparkline.of(values);
        sparkline.setPrefSize(90, 28);
        HBox.setHgrow(sparkline, Priority.ALWAYS);
        valueRow.getChildren().add(sparkline);
    }

    /** The metric title in the card header. */
    public StringProperty titleProperty() {
        return title;
    }

    public String getTitle() {
        return title.get();
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    /** The large headline value. */
    public StringProperty valueProperty() {
        return value;
    }

    public String getValue() {
        return value.get();
    }

    public void setValue(String value) {
        this.value.set(value);
    }

    /** The delta text shown in the trend badge (e.g. {@code "+14.5%"}). */
    public StringProperty deltaTextProperty() {
        return deltaText;
    }

    public String getDeltaText() {
        return deltaText.get();
    }

    public void setDeltaText(String delta) {
        this.deltaText.set(delta);
    }

    /** Whether the delta is an improvement (positive styling with a ▲ trend arrow). */
    public BooleanProperty deltaPositiveProperty() {
        return deltaPositive;
    }

    public boolean isDeltaPositive() {
        return deltaPositive.get();
    }

    public void setDeltaPositive(boolean positive) {
        this.deltaPositive.set(positive);
    }

    /** Whether the card glows with a primary halo while hovered. */
    public BooleanProperty glowOnHoverProperty() {
        return glowOnHover;
    }

    public boolean isGlowOnHover() {
        return glowOnHover.get();
    }

    public void setGlowOnHover(boolean glow) {
        this.glowOnHover.set(glow);
    }
}

