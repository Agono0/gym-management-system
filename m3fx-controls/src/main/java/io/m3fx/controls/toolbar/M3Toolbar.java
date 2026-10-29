package io.m3fx.controls.toolbar;

import io.m3fx.core.theme.ColorRole;
import io.m3fx.core.token.ElevationLevel;
import io.m3fx.controls.internal.ThemeColors;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * M3 Expressive Toolbar: contextual floating or docked action container.
 *
 * <p>See <a href="https://m3.material.io/components/toolbars/overview">M3 Toolbars</a>.
 * Supports horizontal and vertical orientations, floating pill and docked bar modes,
 * and standard vs vibrant color schemes.
 */
public class M3Toolbar extends Pane {

    /** Toolbar variant: floating above content or docked to an edge. */
    public enum ToolbarVariant {
        FLOATING,
        DOCKED
    }

    /** Color emphasis style: standard (subtle surface) or vibrant (accented container). */
    public enum ToolbarStyle {
        STANDARD,
        VIBRANT
    }

    private final ObjectProperty<ToolbarVariant> variant =
            new SimpleObjectProperty<>(this, "variant", ToolbarVariant.FLOATING);
    private final ObjectProperty<ToolbarStyle> toolbarStyle =
            new SimpleObjectProperty<>(this, "toolbarStyle", ToolbarStyle.STANDARD);
    private final ObjectProperty<Orientation> orientation =
            new SimpleObjectProperty<>(this, "orientation", Orientation.HORIZONTAL);

    private final ObservableList<Node> items = FXCollections.observableArrayList();
    private final Pane container;

    /** Creates an empty horizontal floating toolbar. */
    public M3Toolbar() {
        this(ToolbarVariant.FLOATING, ToolbarStyle.STANDARD, Orientation.HORIZONTAL);
    }

    /**
     * Creates a toolbar with specific variant and style.
     *
     * @param variant floating or docked
     * @param style standard or vibrant
     */
    public M3Toolbar(ToolbarVariant variant, ToolbarStyle style) {
        this(variant, style, Orientation.HORIZONTAL);
    }

    /**
     * Creates a toolbar with variant, style, and orientation.
     *
     * @param variant floating or docked
     * @param style standard or vibrant
     * @param orientation horizontal or vertical
     */
    public M3Toolbar(ToolbarVariant variant, ToolbarStyle style, Orientation orientation) {
        getStyleClass().add("m3-toolbar");
        setAccessibleRole(javafx.scene.AccessibleRole.TOOL_BAR);
        this.container = orientation == Orientation.VERTICAL ? new VBox(8) : new HBox(8);
        this.container.getStyleClass().add("m3-toolbar-container");
        getChildren().add(container);

        setVariant(variant);
        setToolbarStyle(style);
        setOrientation(orientation);

        this.variant.addListener((obs, oldV, newV) -> updateStyles());
        this.toolbarStyle.addListener((obs, oldV, newV) -> updateStyles());
        this.orientation.addListener((obs, oldV, newV) -> rebuildLayout(newV));

        items.addListener((ListChangeListener<Node>) change -> {
            container.getChildren().setAll(items);
        });

        updateStyles();
    }

    /** Lays the item container out across the full toolbar (a plain Pane would not resize it). */
    @Override
    protected void layoutChildren() {
        javafx.geometry.Insets insets = getInsets();
        double x = insets.getLeft();
        double y = insets.getTop();
        double w = Math.max(0, getWidth() - insets.getLeft() - insets.getRight());
        double h = Math.max(0, getHeight() - insets.getTop() - insets.getBottom());
        container.resizeRelocate(x, y, w, h);
    }

    /** Returns the observable list of toolbar items. */
    public ObservableList<Node> getItems() {
        return items;
    }

    public ObjectProperty<ToolbarVariant> variantProperty() {
        return variant;
    }

    public ToolbarVariant getVariant() {
        return variant.get();
    }

    public void setVariant(ToolbarVariant variant) {
        this.variant.set(variant);
    }

    public ObjectProperty<ToolbarStyle> toolbarStyleProperty() {
        return toolbarStyle;
    }

    public ToolbarStyle getToolbarStyle() {
        return toolbarStyle.get();
    }

    public void setToolbarStyle(ToolbarStyle style) {
        this.toolbarStyle.set(style);
    }

    public ObjectProperty<Orientation> orientationProperty() {
        return orientation;
    }

    public Orientation getOrientation() {
        return orientation.get();
    }

    public void setOrientation(Orientation orientation) {
        this.orientation.set(orientation);
    }

    private void updateStyles() {
        getStyleClass().removeIf(s -> s.startsWith("m3-toolbar-"));
        getStyleClass().add("m3-toolbar-" + getVariant().name().toLowerCase());
        getStyleClass().add("m3-toolbar-" + getToolbarStyle().name().toLowerCase());
        getStyleClass().add("m3-toolbar-" + getOrientation().name().toLowerCase());

        if (getVariant() == ToolbarVariant.FLOATING) {
            // Spec: floating toolbar container elevation level 3
            // (md.comp.toolbar.floating.container.elevation).
            Color shadow = ThemeColors.resolve(this, ColorRole.SHADOW);
            setEffect(ElevationLevel.LEVEL3.toEffect(shadow));
        } else {
            setEffect(null);
        }
    }

    private void rebuildLayout(Orientation o) {
        getChildren().clear();
        Pane newContainer;
        if (o == Orientation.VERTICAL) {
            VBox box = new VBox(8);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(8));
            newContainer = box;
        } else {
            HBox box = new HBox(8);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(8));
            newContainer = box;
        }
        newContainer.getStyleClass().add("m3-toolbar-container");
        newContainer.getChildren().setAll(items);
        getChildren().add(newContainer);
        updateStyles();
    }
}

