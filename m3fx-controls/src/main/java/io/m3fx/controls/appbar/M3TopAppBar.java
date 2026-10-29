package io.m3fx.controls.appbar;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
 * Surface color, on-surface title; medium/large show a second headline row (title-large /
 * headline-medium on scroll in the full spec — here both rows render and callers collapse on
 * scroll by switching the variant).
 */
public class M3TopAppBar extends VBox {

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
                    setMinHeight(112);
                    setPrefHeight(112);
                }
                case LARGE -> {
                    setMinHeight(152);
                    setPrefHeight(152);
                }
                default -> {
                    setMinHeight(64);
                    setPrefHeight(64);
                }
            }
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
