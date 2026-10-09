package io.m3fx.controls.icon;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

/**
 * Shared M3 icon wrapper. JavaFX cannot render Material Symbols ligature fonts, so icons are
 * supplied either as catalog symbols ({@link M3Icons}), raw SVG path content, or short glyph text
 * when an icon font is set via CSS.
 */
public final class M3Icon extends StackPane {

    /** Default M3 icon size in dp. */
    public static final double DEFAULT_SIZE = 24;
    /** Small M3 icon size in dp. */
    public static final double SMALL_SIZE = 20;
    /** Extra-small M3 icon size in dp. */
    public static final double XSMALL_SIZE = 18;

    private final StringProperty symbolName = new SimpleStringProperty(this, "symbolName");

    /** Creates an empty icon node. */
    public M3Icon() {
        getStyleClass().add("m3-icon");
        setMinSize(DEFAULT_SIZE, DEFAULT_SIZE);
        setPrefSize(DEFAULT_SIZE, DEFAULT_SIZE);
        setMaxSize(DEFAULT_SIZE, DEFAULT_SIZE);
        // FXML/Scene Builder friendly: <M3Icon symbolName="search"/> renders the catalog icon.
        symbolNameProperty().addListener((obs, oldV, name) -> {
            String path = M3Icons.pathFor(name);
            setSvgContent(path == null ? "" : path);
            setAccessibleText(name);
        });
    }

    /** Returns the catalog symbol name property (FXML-settable). */
    public StringProperty symbolNameProperty() {
        return symbolName;
    }

    /** Returns the catalog symbol name. */
    public String getSymbolName() {
        return symbolName.get();
    }

    /**
     * Sets the catalog symbol name and renders it.
     *
     * @param symbolName catalog name such as {@code search}, may be {@code null} to clear
     */
    public void setSymbolName(String symbolName) {
        this.symbolName.set(symbolName);
    }

    /**
     * Creates an icon from the bundled {@link M3Icons} catalog.
     *
     * @param name catalog name such as {@code search}
     * @return the icon node
     */
    public static M3Icon symbol(String name) {
        M3Icon icon = new M3Icon();
        String path = M3Icons.pathFor(name);
        icon.setSvgContent(path == null ? "" : path);
        icon.setAccessibleText(name);
        return icon;
    }

    /**
     * Creates an icon from a single glyph character (rendered with the current icon font).
     *
     * @param glyph the glyph text
     * @return the icon node
     */
    public static M3Icon ofGlyph(String glyph) {
        M3Icon icon = new M3Icon();
        javafx.scene.control.Label label = new javafx.scene.control.Label(glyph == null ? "" : glyph);
        label.getStyleClass().add("m3-icon-glyph");
        icon.getChildren().add(label);
        return icon;
    }

    /**
     * Creates an icon from an SVG path string.
     *
     * @param svgPath path content (the {@code d} attribute)
     * @return the icon node
     */
    public static M3Icon ofSvg(String svgPath) {
        M3Icon icon = new M3Icon();
        icon.setSvgContent(svgPath);
        return icon;
    }

    /**
     * Sets SVG path content on this icon.
     *
     * @param svgPath path content
     */
    public void setSvgContent(String svgPath) {
        getChildren().clear();
        SVGPath svg = new SVGPath();
        svg.setContent(svgPath == null ? "" : svgPath);
        svg.getStyleClass().add("m3-icon-svg");
        getChildren().add(svg);
    }

    /**
     * Wraps any node as a fixed-size icon.
     *
     * @param node the graphic node
     * @param size icon box size in px
     * @return a sized wrapper
     */
    public static Region wrap(Node node, double size) {
        StackPane box = new StackPane(node);
        box.getStyleClass().add("m3-icon");
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        return box;
    }
}
