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
    private final HBox row = new HBox();

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
        row.getStyleClass().add("m3-top-app-bar-row");
        row.getChildren().add(titleLabel);
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

    /**
     * Sets the navigation icon (menu/back).
     *
     * @param icon navigation icon node
     */
    public void setNavigationIcon(Node icon) {
        row.getChildren().removeIf(n -> "m3-appbar-nav".equals(n.getId()));
        if (icon != null) {
            icon.setId("m3-appbar-nav");
            row.getChildren().add(0, icon);
        }
    }

    /**
     * Sets trailing action icons.
     *
     * @param actions action nodes
     */
    public void setActions(Node... actions) {
        row.getChildren().removeIf(n -> "m3-appbar-action".equals(n.getId()));
        Region spacer = new Region();
        spacer.setId("m3-appbar-action");
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().add(spacer);
        for (Node action : actions) {
            if (action != null) {
                action.setId("m3-appbar-action");
                row.getChildren().add(action);
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
        row.setAlignment(
                newV == TopAppBarVariant.CENTER_ALIGNED
                        ? javafx.geometry.Pos.CENTER
                        : javafx.geometry.Pos.CENTER_LEFT);
    }
}
