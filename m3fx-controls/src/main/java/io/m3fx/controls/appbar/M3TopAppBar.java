package io.m3fx.controls.appbar;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * M3 top app bar: small, center-aligned, medium and large variants.
 *
 * <p>See <a href="https://m3.material.io/components/top-app-bar/overview">M3 top app bar</a>.
 * Accessibility: a TOOL_BAR-role container that is not a tab stop (the nav icon and actions are
 * the keyboard targets) announcing "<title> toolbar, <n> navigation item(s), <m> action(s)",
 * live with title and control changes.
 * Surface color, on-surface title; medium/large show a second headline row (title-large /
 * headline-medium on scroll in the full spec — here both rows render and callers collapse on
 * scroll by switching the variant).
 */
// Suppressed this-escape (false positives javac 27 cannot see across the module boundary):
// (1) the JavaFX property-bean idiom new Simple*Property(this, ...) - the property
// constructor only stores the bean reference, exactly like every built-in control;
// (2) constructor calls to JavaFX final methods (getStyleClass(), setAccessibleRole())
// - final, but javac does not honor ACC_FINAL for symbols from another module. This
// class's own overridable methods are never called during construction.
@SuppressWarnings("this-escape")
public class M3TopAppBar extends VBox {

    /** Spec container height in px (md.comp.top-app-bar.small.container.height: 64dp). */
    static final double HEIGHT_SMALL = 64;
    /** Spec container height in px (md.comp.top-app-bar.medium.container.height: 112dp). */
    static final double HEIGHT_MEDIUM = 112;
    /** Spec container height in px (md.comp.top-app-bar.large.container.height: 152dp). */
    static final double HEIGHT_LARGE = 152;

    private final ObjectProperty<TopAppBarVariant> variant =
            new SimpleObjectProperty<>(this, "variant", TopAppBarVariant.SMALL);
    private final StringProperty title = new SimpleStringProperty(this, "title", "");

    private final Label titleLabel = new Label();
    private final Label headlineLabel = new Label();
    private final HBox row = new HBox(8);
    private final HBox leadingBox = new HBox();
    private final HBox trailingBox = new HBox(8);
    private final Region leftSpacer = new Region();
    private final Region rightSpacer = new Region();

    /** Creates a small top app bar. */
    public M3TopAppBar() {
        this("", TopAppBarVariant.SMALL);
    }

    /**
     * Creates a top app bar.
     *
     * @param title bar title
     * @param variant bar variant
     */
    public M3TopAppBar(String title, TopAppBarVariant variant) {
        getStyleClass().add("m3-top-app-bar");
        titleLabel.getStyleClass().add("m3-top-app-bar-title");
        titleLabel.textProperty().bind(titleProperty());
        headlineLabel.getStyleClass().add("m3-top-app-bar-headline");
        headlineLabel.textProperty().bind(titleProperty());
        headlineLabel.setWrapText(true);
        leadingBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        trailingBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        row.getStyleClass().add("m3-top-app-bar-row");
        row.getChildren().addAll(leadingBox, leftSpacer, titleLabel, rightSpacer, trailingBox);
        setTitle(title);
        setVariant(variant);
        variantProperty().addListener((obs, oldV, newV) -> rebuild(oldV, newV));
        rebuild(null, getVariant());
        // Accessibility: a presentational TOOL_BAR container. The bar itself stays out of the
        // tab-focus cycle (its nav icon and action buttons are the keyboard targets) and
        // announces a summary - "<title> toolbar, <n> navigation item(s), <m> action(s)" - so
        // screen readers introduce it before the first focusable child.
        setAccessibleRole(AccessibleRole.TOOL_BAR);
        setFocusTraversable(false);
        titleProperty().addListener((obs, oldV, newV) -> refreshAccessibleText());
        leadingBox.getChildren().addListener(
                (ListChangeListener<Node>) c -> refreshAccessibleText());
        trailingBox.getChildren().addListener(
                (ListChangeListener<Node>) c -> refreshAccessibleText());
        refreshAccessibleText();
    }

    /** Rebuilds the announcement: "&lt;title&gt; toolbar, &lt;n&gt; navigation item(s), &lt;m&gt; action(s)". */
    private void refreshAccessibleText() {
        String title = getTitle().strip();
        StringBuilder announcement =
                new StringBuilder(title.isEmpty() ? "Toolbar" : title + " toolbar");
        int navItems = leadingBox.getChildren().size();
        if (navItems > 0) {
            announcement.append(", ").append(navItems).append(" navigation item")
                    .append(navItems > 1 ? "s" : "");
        }
        int actions = trailingBox.getChildren().size();
        if (actions > 0) {
            announcement.append(", ").append(actions).append(" action")
                    .append(actions > 1 ? "s" : "");
        }
        setAccessibleText(announcement.toString());
    }

    /** Returns the title property. */
    public StringProperty titleProperty() {
        return title;
    }

    /** Returns the title. */
    public String getTitle() {
        return title.get();
    }

    /**
     * Sets the title.
     *
     * @param title the title
     */
    public void setTitle(String title) {
        this.title.set(title == null ? "" : title);
    }

    /** Returns the variant property. */
    public ObjectProperty<TopAppBarVariant> variantProperty() {
        return variant;
    }

    /** Returns the variant. */
    public TopAppBarVariant getVariant() {
        return variant.get();
    }

    /**
     * Sets the variant.
     *
     * @param variant the new variant
     */
    public void setVariant(TopAppBarVariant variant) {
        TopAppBarVariant old = this.variant.get();
        this.variant.set(variant == null ? TopAppBarVariant.SMALL : variant);
        rebuild(old, this.variant.get());
    }

    /** Navigation icon property (FXML-settable). */
    private final ObjectProperty<Node> navigationIcon =
            new SimpleObjectProperty<>(this, "navigationIcon");

    /** Returns the navigation icon property. */
    public ObjectProperty<Node> navigationIconProperty() {
        return navigationIcon;
    }

    /** Returns the navigation icon, may be {@code null}. */
    public Node getNavigationIcon() {
        return navigationIcon.get();
    }

    /**
     * Sets the navigation icon (menu/back).
     *
     * @param icon navigation icon node
     */
    public void setNavigationIcon(Node icon) {
        navigationIcon.set(icon);
        leadingBox.getChildren().clear();
        if (icon != null) {
            if (icon.getId() == null || icon.getId().isBlank()) {
                icon.setId("m3-appbar-nav");
            }
            leadingBox.getChildren().add(icon);
        }
    }

    /**
     * Sets trailing action icons.
     *
     * @param actions action nodes
     */
    public void setActions(Node... actions) {
        trailingBox.getChildren().clear();
        for (Node action : actions) {
            if (action != null) {
                action.setId("m3-appbar-action");
                trailingBox.getChildren().add(action);
            }
        }
    }

    private void rebuild(TopAppBarVariant oldV, TopAppBarVariant newV) {
        if (oldV != null) {
            getStyleClass().remove("m3-top-app-bar-" + oldV.name().toLowerCase().replace('_', '-'));
        }
        if (newV != null) {
            String cls = "m3-top-app-bar-" + newV.name().toLowerCase().replace('_', '-');
            if (!getStyleClass().contains(cls)) {
                getStyleClass().add(cls);
            }
        }
        getChildren().setAll(row);
        if (newV == TopAppBarVariant.MEDIUM || newV == TopAppBarVariant.LARGE) {
            getChildren().add(headlineLabel);
        }
        // Spec container heights: small/center-aligned 64dp, medium 112dp, large 152dp
        // (md.comp.top-app-bar.{small|medium|large}.container.height).
        if (newV != null) {
            switch (newV) {
                case MEDIUM -> {
                    setMinHeight(HEIGHT_MEDIUM);
                    setPrefHeight(HEIGHT_MEDIUM);
                }
                case LARGE -> {
                    setMinHeight(HEIGHT_LARGE);
                    setPrefHeight(HEIGHT_LARGE);
                }
                default -> {
                    setMinHeight(HEIGHT_SMALL);
                    setPrefHeight(HEIGHT_SMALL);
                }
            }
            // The bar is a VBox: children get their PREF heights, so the single row needs its
            // spec height as a pref too — the CSS min-height alone left a text-height row that
            // clipped title descenders (Q2 finding on the availability screen).
            row.setMinHeight(HEIGHT_SMALL);
            row.setPrefHeight(HEIGHT_SMALL);
        }

        if (newV == TopAppBarVariant.CENTER_ALIGNED) {
            HBox.setHgrow(leftSpacer, Priority.ALWAYS);
            leftSpacer.setMinWidth(0);
            leftSpacer.setPrefWidth(Region.USE_COMPUTED_SIZE);
            leftSpacer.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(rightSpacer, Priority.ALWAYS);
            rightSpacer.setMinWidth(0);
            rightSpacer.setPrefWidth(Region.USE_COMPUTED_SIZE);
            rightSpacer.setMaxWidth(Double.MAX_VALUE);
            row.setAlignment(javafx.geometry.Pos.CENTER);
        } else {
            HBox.setHgrow(leftSpacer, Priority.NEVER);
            leftSpacer.setMinWidth(0);
            leftSpacer.setPrefWidth(0);
            leftSpacer.setMaxWidth(0);
            HBox.setHgrow(rightSpacer, Priority.ALWAYS);
            rightSpacer.setMinWidth(0);
            rightSpacer.setPrefWidth(Region.USE_COMPUTED_SIZE);
            rightSpacer.setMaxWidth(Double.MAX_VALUE);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        }
    }
}
