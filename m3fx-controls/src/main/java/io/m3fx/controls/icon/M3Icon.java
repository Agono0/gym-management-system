package io.m3fx.controls.icon;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
 *
 * <p>Unknown catalog names are reported, not swallowed: they render a small neutral placeholder
 * ring, log a single warning per unknown name to {@code System.err} and announce themselves as
 * {@code "unknown icon: <name>"} through the accessible text. They never throw.
 */
public final class M3Icon extends StackPane {

    /** Default M3 icon size in dp. */
    public static final double DEFAULT_SIZE = 24;
    /** Small M3 icon size in dp. */
    public static final double SMALL_SIZE = 20;
    /** Extra-small M3 icon size in dp. */
    public static final double XSMALL_SIZE = 18;

    /**
     * Neutral ring placeholder rendered for unknown symbol names (24x24 view box, matching the
     * bundled catalog paths).
     */
    private static final String UNKNOWN_PLACEHOLDER_PATH =
            "M12 2a10 10 0 1 0 0 20 10 10 0 1 0 0-20zm0 2.5a7.5 7.5 0 1 1 0 15 7.5 7.5 0 1 1 0-15z";

    /** Unknown names already warned about, so each distinct name logs exactly one warning. */
    private static final Set<String> WARNED_UNKNOWN_NAMES = ConcurrentHashMap.newKeySet();

    private final StringProperty symbolName = new SimpleStringProperty(this, "symbolName");

    /** Creates an empty icon node. */
    public M3Icon() {
        getStyleClass().add("m3-icon");
        setMinSize(DEFAULT_SIZE, DEFAULT_SIZE);
        setPrefSize(DEFAULT_SIZE, DEFAULT_SIZE);
        setMaxSize(DEFAULT_SIZE, DEFAULT_SIZE);
        // FXML/Scene Builder friendly: <M3Icon symbolName="search"/> renders the catalog icon.
        symbolNameProperty().addListener((obs, oldV, name) -> renderSymbol(name));
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
     * <p>Never throws. If {@code name} is not in the catalog, the icon renders a small neutral
     * ring placeholder, announces itself through the accessible text as
     * {@code "unknown icon: <name>"} and logs one warning per distinct unknown name to
     * {@code System.err} so typos surface during development without spamming the log when an app
     * builds icons dynamically from data. A {@code null} name renders an empty icon without a
     * warning (the documented "clear" behavior). See {@link M3Icons#names()} for the valid names.
     *
     * @param name catalog name such as {@code search}
     * @return the icon node
     */
    public static M3Icon symbol(String name) {
        M3Icon icon = new M3Icon();
        icon.renderSymbol(name);
        return icon;
    }

    /**
     * Renders a catalog name on this icon: the bundled SVG path for known names, the neutral
     * placeholder ring plus a one-time warning for unknown ones, and an empty glyph when the name
     * is {@code null} (the documented way to clear {@code setSymbolName}).
     *
     * @param name catalog name, may be {@code null} to clear
     */
    private void renderSymbol(String name) {
        if (name == null) {
            setSvgContent("");
            setAccessibleText(null);
            return;
        }
        String path = M3Icons.pathFor(name);
        if (path == null) {
            warnUnknownIcon(name);
            setSvgContent(UNKNOWN_PLACEHOLDER_PATH);
            setAccessibleText("unknown icon: " + name);
        } else {
            setSvgContent(path);
            setAccessibleText(name);
        }
    }

    /** Logs a single warning per distinct unknown icon name to {@code System.err}. */
    private static void warnUnknownIcon(String name) {
        if (WARNED_UNKNOWN_NAMES.add(name)) {
            System.err.println("[m3fx] M3Icon: unknown icon name '" + name
                    + "' - rendering a neutral placeholder. See M3Icons.names() for the catalog.");
        }
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
