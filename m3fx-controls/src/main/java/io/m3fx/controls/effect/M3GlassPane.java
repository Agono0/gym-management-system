package io.m3fx.controls.effect;

import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.effect.BoxBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Cyber-glass / Acrylic Glassmorphism container featuring a frosted backdrop blur,
 * translucent surface container tinting, and 1px specular rim highlight.
 */
public class M3GlassPane extends StackPane {

    private final DoubleProperty blurRadius = new SimpleDoubleProperty(this, "blurRadius", 16.0);
    private final DoubleProperty cornerRadius = new SimpleDoubleProperty(this, "cornerRadius", 16.0);
    private final DoubleProperty glassOpacity = new SimpleDoubleProperty(this, "glassOpacity", 0.65);

    private final Region backdropFilter = new Region();
    private final StackPane contentBox = new StackPane();
    private final Rectangle clipRect = new Rectangle();

    /** Creates an empty glass pane. */
    public M3GlassPane() {
        this(null);
    }

    /**
     * Creates a glass pane with content.
     *
     * @param content child content
     */
    public M3GlassPane(Node content) {
        getStyleClass().add("m3-glass-pane");

        clipRect.widthProperty().bind(widthProperty());
        clipRect.heightProperty().bind(heightProperty());
        clipRect.arcWidthProperty().bind(cornerRadius.multiply(2));
        clipRect.arcHeightProperty().bind(cornerRadius.multiply(2));
        setClip(clipRect);

        BoxBlur blur = new BoxBlur();
        blur.widthProperty().bind(blurRadius);
        blur.heightProperty().bind(blurRadius);
        blur.setIterations(2);
        backdropFilter.setEffect(blur);
        backdropFilter.setMouseTransparent(true);

        contentBox.getStyleClass().add("m3-glass-content");
        if (content != null) {
            contentBox.getChildren().add(content);
        }

        getChildren().addAll(backdropFilter, contentBox);

        blurRadius.addListener((obs, o, n) -> updateGlassStyling());
        cornerRadius.addListener((obs, o, n) -> updateGlassStyling());
        glassOpacity.addListener((obs, o, n) -> updateGlassStyling());
        sceneProperty().addListener((obs, o, n) -> updateGlassStyling());

        updateGlassStyling();
    }

    private void updateGlassStyling() {
        Color surfaceTint = ThemeColors.resolve(this, ColorRole.SURFACE_CONTAINER_HIGH);
        Color borderRim = ThemeColors.resolve(this, ColorRole.OUTLINE_VARIANT);

        // Color.color throws IllegalArgumentException outside [0,1]; clamp defensive values.
        double op = Math.max(0.0, Math.min(1.0, getGlassOpacity()));
        Color translucentBg = Color.color(
                surfaceTint.getRed(), surfaceTint.getGreen(), surfaceTint.getBlue(), op
        );

        Color translucentRim = Color.color(
                borderRim.getRed(), borderRim.getGreen(), borderRim.getBlue(), 0.45
        );

        double cr = getCornerRadius();
        CornerRadii radii = new CornerRadii(cr);

        setBackground(new Background(new BackgroundFill(translucentBg, radii, null)));
        setBorder(new Border(new BorderStroke(translucentRim, BorderStrokeStyle.SOLID, radii, new BorderWidths(1.0))));
    }

    /** Backdrop blur radius in px. */
    public DoubleProperty blurRadiusProperty() {
        return blurRadius;
    }

    public double getBlurRadius() {
        return blurRadius.get();
    }

    public void setBlurRadius(double blur) {
        this.blurRadius.set(blur);
    }

    /** Corner radius in px (drives the rounded clip and border). */
    public DoubleProperty cornerRadiusProperty() {
        return cornerRadius;
    }

    public double getCornerRadius() {
        return cornerRadius.get();
    }

    public void setCornerRadius(double radius) {
        this.cornerRadius.set(radius);
    }

    /** Translucent surface tint opacity, clamped to {@code [0,1]} when applied. */
    public DoubleProperty glassOpacityProperty() {
        return glassOpacity;
    }

    public double getGlassOpacity() {
        return glassOpacity.get();
    }

    public void setGlassOpacity(double opacity) {
        this.glassOpacity.set(opacity);
    }

    /** Returns the content container inside the glass surface. */
    public StackPane getContentBox() {
        return contentBox;
    }

    /** Returns the single content node (for FXML &lt;content&gt; support), or null if empty. */
    public Node getContent() {
        return contentBox.getChildren().isEmpty() ? null : contentBox.getChildren().get(0);
    }

    /**
     * Replaces the glass pane content.
     *
     * @param node new content node
     */
    public void setContent(Node node) {
        contentBox.getChildren().setAll(node);
    }
}

