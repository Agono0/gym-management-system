package io.m3fx.controls.glass;

import javafx.beans.DefaultProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * Liquid Glass pane (Apple iOS 26): blurs and refracts what is BEHIND it, adds a specular rim
 * light, optional tint and drop shadow, and floats its content above unblurred.
 *
 * <p>Real {@code Control} + {@link GlassPaneSkin}. The skin implements the pipeline from
 * {@code docs/SPEC_NOTES/liquid-glass.md}: cached backdrop snapshot &rarr; Gaussian blur &rarr;
 * {@code DisplacementMap} hardware refraction ({@link GlassRefraction}) &rarr; saturation/brightness
 * lift &rarr; optional tint &rarr; specular rim + 1px edge stroke &rarr; drop shadow &rarr; content.
 * All colors resolve from {@code -md-sys-color-*} theme roles, so the glass is correct in light and
 * dark and with any seed.
 *
 * <p>Full reference physics from {@code liquidGL-main} and {@code liquid-glass-main} are provided:
 * continuous viscous fluid dynamics with ripple relaxation, 3D perspective tilt with optical
 * refraction offset, draggable lenses with elevation springs, and Apple glass button ergonomics.
 *
 * <p>Factories: {@link #regular()}, {@link #clear()}, {@link #button(String)}, {@link #interactive()},
 * and {@link #of(GlassEffectParams)}.
 */
@SuppressWarnings("this-escape")
@DefaultProperty("glassChildren")
public class M3GlassPane extends Control {

    /** Saturation sentinel: negative means "derive from the variant" (0.18 regular / 0.10 clear). */
    public static final double SATURATION_AUTO = -1.0;

    private final ObjectProperty<GlassEffectParams> effect =
            new SimpleObjectProperty<>(this, "effect");
    private final ObjectProperty<GlassVariant> variant =
            new SimpleObjectProperty<>(this, "variant", GlassVariant.REGULAR);
    private final ObjectProperty<Color> tint = new SimpleObjectProperty<>(this, "tint");
    private final ObjectProperty<GlassShape> shape =
            new SimpleObjectProperty<>(this, "shape", GlassShape.CAPSULE);
    private final BooleanProperty interactive =
            new SimpleBooleanProperty(this, "interactive", false);
    private final BooleanProperty shadow = new SimpleBooleanProperty(this, "shadow", true);
    private final DoubleProperty blurRadius = new SimpleDoubleProperty(this, "blurRadius", 16.0);
    private final DoubleProperty cornerRadius = new SimpleDoubleProperty(this, "cornerRadius", 16.0);
    private final DoubleProperty glassOpacity = new SimpleDoubleProperty(this, "glassOpacity", 0.65);
    private final DoubleProperty refraction =
            new SimpleDoubleProperty(this, "refraction", GlassRefraction.DEFAULT_REFRACTION);
    private final DoubleProperty specular = new SimpleDoubleProperty(this, "specular", 0.35);
    private final DoubleProperty specularAngle = new SimpleDoubleProperty(this, "specularAngle", 110.0);
    private final DoubleProperty saturation =
            new SimpleDoubleProperty(this, "saturation", SATURATION_AUTO);

    private final ObjectProperty<GlassEffectParams.MapStyle> mapStyle =
            new SimpleObjectProperty<>(this, "mapStyle", GlassEffectParams.MapStyle.LIQUIDGL);
    private final DoubleProperty bevelDepth =
            new SimpleDoubleProperty(this, "bevelDepth", GlassRefraction.DEFAULT_BEVEL_DEPTH);
    private final DoubleProperty bevelWidth =
            new SimpleDoubleProperty(this, "bevelWidth", GlassRefraction.DEFAULT_BEVEL_WIDTH);
    private final DoubleProperty frost = new SimpleDoubleProperty(this, "frost", 0.0);
    private final DoubleProperty aberration = new SimpleDoubleProperty(this, "aberration", 0.0);
    private final DoubleProperty magnify =
            new SimpleDoubleProperty(this, "magnify", GlassRefraction.DEFAULT_MAGNIFY);
    private final DoubleProperty nikdelvinDepth =
            new SimpleDoubleProperty(this, "nikdelvinDepth", GlassRefraction.DEFAULT_NIKDELVIN_DEPTH);
    private final DoubleProperty nikdelvinStrength =
            new SimpleDoubleProperty(this, "nikdelvinStrength", GlassRefraction.DEFAULT_NIKDELVIN_STRENGTH);
    private final BooleanProperty fluidInteraction =
            new SimpleBooleanProperty(this, "fluidInteraction", false);
    private final DoubleProperty interactionRadius =
            new SimpleDoubleProperty(this, "interactionRadius", 0.35);
    private final DoubleProperty interactionStrength =
            new SimpleDoubleProperty(this, "interactionStrength", 0.5);
    private final DoubleProperty interactionViscosity =
            new SimpleDoubleProperty(this, "interactionViscosity", 0.65);

    private final BooleanProperty tilt = new SimpleBooleanProperty(this, "tilt", false);
    private final DoubleProperty tiltFactor = new SimpleDoubleProperty(this, "tiltFactor", 5.0);
    private final DoubleProperty tiltEase = new SimpleDoubleProperty(this, "tiltEase", 400.0);

    private final BooleanProperty draggable = new SimpleBooleanProperty(this, "draggable", false);
    private final BooleanProperty buttonMode = new SimpleBooleanProperty(this, "buttonMode", false);

    /** Content holder floated above the glass layers (owned by the control so it exists pre-skin). */
    final StackPane contentBox = new StackPane();

    /** Creates an empty regular glass pane. */
    public M3GlassPane() {
        this(null, 16.0, 16.0);
    }

    /**
     * Creates a glass pane with content.
     *
     * @param content child content, may be {@code null}
     */
    public M3GlassPane(Node content) {
        this(content, 16.0, 16.0);
    }

    /**
     * Creates a glass pane with content, corner radius and blur radius.
     *
     * @param content child content, may be {@code null}
     * @param cornerRadius corner radius in px
     * @param blurRadius backdrop blur radius in px
     */
    public M3GlassPane(Node content, double cornerRadius, double blurRadius) {
        getStyleClass().addAll("m3-glass-pane", "glass-pane");
        setCornerRadius(cornerRadius);
        setBlurRadius(blurRadius);
        if (content != null) {
            setContent(content);
        }
        setAccessibleRole(javafx.scene.AccessibleRole.PARENT);
    }

    /**
     * Creates a regular (frosted) glass pane: Gaussian blur 16px, saturation +0.18.
     *
     * @return the pane
     */
    public static M3GlassPane regular() {
        M3GlassPane pane = new M3GlassPane();
        pane.setVariant(GlassVariant.REGULAR);
        pane.setBlurRadius(16.0);
        return pane;
    }

    /**
     * Creates a clear glass pane for content-rich backdrops: Gaussian blur 4px, saturation +0.10.
     *
     * @return the pane
     */
    public static M3GlassPane clear() {
        M3GlassPane pane = new M3GlassPane();
        pane.setVariant(GlassVariant.CLEAR);
        pane.setBlurRadius(4.0);
        return pane;
    }

    /**
     * Creates an interactive glass pane with fluid interaction and 3D tilt.
     *
     * @return the interactive pane
     */
    public static M3GlassPane interactive() {
        M3GlassPane pane = new M3GlassPane();
        pane.setInteractive(true);
        pane.setFluidInteraction(true);
        pane.setTilt(true);
        return pane;
    }

    /**
     * Creates an Apple Liquid Glass button with hover scale, press squish and inset specular highlight.
     *
     * @param text button text
     * @return the button pane
     */
    public static M3GlassPane button(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: -md-sys-color-on-surface;");
        M3GlassPane pane = new M3GlassPane(label);
        pane.setButtonMode(true);
        pane.setInteractive(true);
        pane.setGlassShape(GlassShape.CAPSULE);
        pane.getStyleClass().add("liquid-glass-button");
        return pane;
    }

    /**
     * Creates a pane configured from an immutable {@link GlassEffectParams} bundle.
     *
     * @param params the bundle
     * @return the pane
     */
    public static M3GlassPane of(GlassEffectParams params) {
        M3GlassPane pane = new M3GlassPane();
        if (params != null) {
            pane.setGlassEffect(params);
        }
        return pane;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new GlassPaneSkin(this);
    }

    /** Returns the glass effect params property (bulk configuration; see class doc). */
    public ObjectProperty<GlassEffectParams> glassEffectProperty() {
        return effect;
    }

    /** Returns the glass effect params, may be {@code null}. */
    public GlassEffectParams getGlassEffect() {
        return effect.get();
    }

    /**
     * Applies an immutable {@link GlassEffectParams} bundle by copying its values into the pane's
     * live properties.
     *
     * @param params the bundle, may be {@code null} to clear
     */
    public void setGlassEffect(GlassEffectParams params) {
        effect.set(params);
        if (params != null) {
            setVariant(params.variant());
            setTint(params.tint());
            setInteractive(params.isInteractive());
            setGlassShape(params.shape());
            setCornerRadius(params.cornerRadius());
            setMapStyle(params.mapStyle());
            setRefraction(params.refraction());
            setBevelDepth(params.bevelDepth());
            setBevelWidth(params.bevelWidth());
            setFrost(params.frost());
            setAberration(params.aberration());
            setMagnify(params.magnify());
            setNikdelvinDepth(params.nikdelvinDepth());
            setNikdelvinStrength(params.nikdelvinStrength());
            setFluidInteraction(params.isFluidInteraction());
            setInteractionRadius(params.interactionRadius());
            setInteractionStrength(params.interactionStrength());
            setInteractionViscosity(params.interactionViscosity());
            setTilt(params.isTilt());
            setTiltFactor(params.tiltFactor());
            setTiltEase(params.tiltEase());
            setDraggable(params.isDraggable());
            setButtonMode(params.isButtonMode());
        }
    }

    /** Returns the variant property. */
    public ObjectProperty<GlassVariant> variantProperty() {
        return variant;
    }

    /** Returns the glass variant. */
    public GlassVariant getVariant() {
        return variant.get();
    }

    /**
     * Sets the glass variant.
     *
     * @param newVariant the variant, {@code null} resets to {@link GlassVariant#REGULAR}
     */
    public void setVariant(GlassVariant newVariant) {
        variant.set(newVariant == null ? GlassVariant.REGULAR : newVariant);
    }

    /** Returns the tint property. */
    public ObjectProperty<Color> tintProperty() {
        return tint;
    }

    /** Returns the tint color, or {@code null} for untinted glass. */
    public Color getTint() {
        return tint.get();
    }

    /**
     * Sets the tint color (rendered at 0.25 alpha per the spec), {@code null} to clear.
     *
     * @param newTint tint color or {@code null}
     */
    public void setTint(Color newTint) {
        tint.set(newTint);
    }

    /** Returns the glass shape property. */
    public ObjectProperty<GlassShape> glassShapeProperty() {
        return shape;
    }

    /** Returns the glass silhouette. */
    public GlassShape getGlassShape() {
        return shape.get();
    }

    /**
     * Sets the glass silhouette.
     *
     * @param newShape the shape, {@code null} resets to {@link GlassShape#CAPSULE}
     */
    public void setGlassShape(GlassShape newShape) {
        shape.set(newShape == null ? GlassShape.CAPSULE : newShape);
    }

    /** Returns the interactive property. */
    public BooleanProperty interactiveProperty() {
        return interactive;
    }

    /** Returns whether the glass reacts to the pointer. */
    public boolean isInteractive() {
        return interactive.get();
    }

    /**
     * Sets whether the glass reacts to the pointer.
     *
     * @param newInteractive {@code true} to enable pointer reaction
     */
    public void setInteractive(boolean newInteractive) {
        interactive.set(newInteractive);
    }

    /** Returns the shadow property (default on). */
    public BooleanProperty shadowProperty() {
        return shadow;
    }

    /** Returns whether the drop shadow is enabled. */
    public boolean isShadow() {
        return shadow.get();
    }

    /** Sets whether the drop shadow is enabled. */
    public void setShadow(boolean newShadow) {
        shadow.set(newShadow);
    }

    /** Returns the blur radius property. */
    public DoubleProperty blurRadiusProperty() {
        return blurRadius;
    }

    /** Returns the backdrop blur radius in px. */
    public double getBlurRadius() {
        return blurRadius.get();
    }

    /** Sets the backdrop blur radius in px. */
    public void setBlurRadius(double newRadius) {
        blurRadius.set(newRadius);
    }

    /** Returns the corner radius property (used by {@link GlassShape#RECT}). */
    public DoubleProperty cornerRadiusProperty() {
        return cornerRadius;
    }

    /** Returns the corner radius in px. */
    public double getCornerRadius() {
        return cornerRadius.get();
    }

    /** Sets the corner radius in px. */
    public void setCornerRadius(double newRadius) {
        cornerRadius.set(newRadius);
    }

    /** Returns the glass opacity property (the neutral surface fill). */
    public DoubleProperty glassOpacityProperty() {
        return glassOpacity;
    }

    /** Returns the neutral surface opacity in {@code [0,1]}. */
    public double getGlassOpacity() {
        return glassOpacity.get();
    }

    /** Sets the neutral surface opacity. */
    public void setGlassOpacity(double newOpacity) {
        glassOpacity.set(newOpacity);
    }

    /** Returns the refraction strength property (UV units). */
    public DoubleProperty refractionProperty() {
        return refraction;
    }

    /** Returns the liquidGL refraction strength in UV units (default 0.01). */
    public double getRefraction() {
        return refraction.get();
    }

    /** Sets the liquidGL refraction strength. */
    public void setRefraction(double newRefraction) {
        refraction.set(newRefraction);
    }

    /** Returns the map style property. */
    public ObjectProperty<GlassEffectParams.MapStyle> mapStyleProperty() {
        return mapStyle;
    }

    /** Returns the displacement-field source. */
    public GlassEffectParams.MapStyle getMapStyle() {
        return mapStyle.get();
    }

    /** Sets the displacement-field source. */
    public void setMapStyle(GlassEffectParams.MapStyle newMapStyle) {
        mapStyle.set(newMapStyle == null ? GlassEffectParams.MapStyle.LIQUIDGL : newMapStyle);
    }

    /** Returns the bevel depth property. */
    public DoubleProperty bevelDepthProperty() {
        return bevelDepth;
    }

    /** Returns the bevel depth in UV units (default 0.08). */
    public double getBevelDepth() {
        return bevelDepth.get();
    }

    /** Sets the bevel depth in UV units. */
    public void setBevelDepth(double newDepth) {
        bevelDepth.set(newDepth);
    }

    /** Returns the bevel width property. */
    public DoubleProperty bevelWidthProperty() {
        return bevelWidth;
    }

    /** Returns the bevel width as a fraction of the smaller side (default 0.15). */
    public double getBevelWidth() {
        return bevelWidth.get();
    }

    /** Sets the bevel width as a fraction of the smaller side. */
    public void setBevelWidth(double newWidth) {
        bevelWidth.set(newWidth);
    }

    /** Returns the frost blur property. */
    public DoubleProperty frostProperty() {
        return frost;
    }

    /** Returns the frost blur strength (0 = sharp). */
    public double getFrost() {
        return frost.get();
    }

    /** Sets the frost blur strength. */
    public void setFrost(double newFrost) {
        frost.set(newFrost);
    }

    /** Returns the chromatic-aberration strength property. */
    public DoubleProperty aberrationProperty() {
        return aberration;
    }

    /** Returns the chromatic-aberration strength in px (0 = off). */
    public double getAberration() {
        return aberration.get();
    }

    /** Sets the chromatic-aberration strength in px. */
    public void setAberration(double newAberration) {
        aberration.set(newAberration);
    }

    /** Returns the backdrop magnification property. */
    public DoubleProperty magnifyProperty() {
        return magnify;
    }

    /** Returns the backdrop magnification (default 1). */
    public double getMagnify() {
        return magnify.get();
    }

    /** Sets the backdrop magnification. */
    public void setMagnify(double newMagnify) {
        magnify.set(newMagnify);
    }

    /** Returns the nikdelvin bezel depth property. */
    public DoubleProperty nikdelvinDepthProperty() {
        return nikdelvinDepth;
    }

    /** Returns the nikdelvin bezel depth in px (default 10). */
    public double getNikdelvinDepth() {
        return nikdelvinDepth.get();
    }

    /** Sets the nikdelvin bezel depth in px. */
    public void setNikdelvinDepth(double newDepth) {
        nikdelvinDepth.set(newDepth);
    }

    /** Returns the nikdelvin displacement scale property. */
    public DoubleProperty nikdelvinStrengthProperty() {
        return nikdelvinStrength;
    }

    /** Returns the nikdelvin displacement scale. */
    public double getNikdelvinStrength() {
        return nikdelvinStrength.get();
    }

    /** Sets the nikdelvin displacement scale. */
    public void setNikdelvinStrength(double newStrength) {
        nikdelvinStrength.set(newStrength);
    }

    /** Returns the fluid-interaction property (pointer-deformed SDF). */
    public BooleanProperty fluidInteractionProperty() {
        return fluidInteraction;
    }

    /** Returns whether the fluid interaction is enabled. */
    public boolean isFluidInteraction() {
        return fluidInteraction.get();
    }

    /** Enables or disables the fluid interaction. */
    public void setFluidInteraction(boolean enabled) {
        fluidInteraction.set(enabled);
    }

    /** Returns the interaction-reach property (fraction of the smaller side). */
    public DoubleProperty interactionRadiusProperty() {
        return interactionRadius;
    }

    /** Returns the interaction reach. */
    public double getInteractionRadius() {
        return interactionRadius.get();
    }

    /** Sets the interaction reach. */
    public void setInteractionRadius(double newRadius) {
        interactionRadius.set(newRadius);
    }

    /** Returns the fluid interaction strength property. */
    public DoubleProperty interactionStrengthProperty() {
        return interactionStrength;
    }

    /** Returns the fluid interaction strength multiplier. */
    public double getInteractionStrength() {
        return interactionStrength.get();
    }

    /** Sets the fluid interaction strength multiplier. */
    public void setInteractionStrength(double newStrength) {
        interactionStrength.set(newStrength);
    }

    /** Returns the fluid interaction viscosity property. */
    public DoubleProperty interactionViscosityProperty() {
        return interactionViscosity;
    }

    /** Returns the fluid interaction settling viscosity. */
    public double getInteractionViscosity() {
        return interactionViscosity.get();
    }

    /** Sets the fluid interaction settling viscosity. */
    public void setInteractionViscosity(double newViscosity) {
        interactionViscosity.set(newViscosity);
    }

    /** Returns the 3D perspective tilt property. */
    public BooleanProperty tiltProperty() {
        return tilt;
    }

    /** Returns whether 3D perspective tilt is enabled. */
    public boolean isTilt() {
        return tilt.get();
    }

    /** Enables or disables 3D perspective tilt. */
    public void setTilt(boolean enabled) {
        tilt.set(enabled);
    }

    /** Returns the maximum 3D tilt factor in degrees. */
    public DoubleProperty tiltFactorProperty() {
        return tiltFactor;
    }

    /** Returns the maximum 3D tilt factor in degrees. */
    public double getTiltFactor() {
        return tiltFactor.get();
    }

    /** Sets the maximum 3D tilt factor in degrees. */
    public void setTiltFactor(double newFactor) {
        tiltFactor.set(newFactor);
    }

    /** Returns the 3D tilt settle duration in milliseconds. */
    public DoubleProperty tiltEaseProperty() {
        return tiltEase;
    }

    /** Returns the 3D tilt settle duration in milliseconds. */
    public double getTiltEase() {
        return tiltEase.get();
    }

    /** Sets the 3D tilt settle duration in milliseconds. */
    public void setTiltEase(double newEase) {
        tiltEase.set(newEase);
    }

    /** Returns the draggable property. */
    public BooleanProperty draggableProperty() {
        return draggable;
    }

    /** Returns whether the lens can be dragged across its container. */
    public boolean isDraggable() {
        return draggable.get();
    }

    /** Sets whether the lens can be dragged across its container. */
    public void setDraggable(boolean enabled) {
        draggable.set(enabled);
    }

    /** Returns the button mode property. */
    public BooleanProperty buttonModeProperty() {
        return buttonMode;
    }

    /** Returns whether Apple liquid glass button styling/mechanics are active. */
    public boolean isButtonMode() {
        return buttonMode.get();
    }

    /** Sets whether Apple liquid glass button styling/mechanics are active. */
    public void setButtonMode(boolean enabled) {
        buttonMode.set(enabled);
    }

    /** Returns the specular intensity property. */
    public DoubleProperty specularProperty() {
        return specular;
    }

    /** Returns the specular rim intensity (default 0.35). */
    public double getSpecular() {
        return specular.get();
    }

    /** Sets the specular rim intensity. */
    public void setSpecular(double newSpecular) {
        specular.set(newSpecular);
    }

    /** Returns the specular angle property. */
    public DoubleProperty specularAngleProperty() {
        return specularAngle;
    }

    /** Returns the specular light angle in degrees. */
    public double getSpecularAngle() {
        return specularAngle.get();
    }

    /** Sets the specular light angle in degrees. */
    public void setSpecularAngle(double newSpecularAngle) {
        specularAngle.set(newSpecularAngle);
    }

    /** Returns the saturation property. */
    public DoubleProperty saturationProperty() {
        return saturation;
    }

    /** Returns the saturation adjustment. */
    public double getSaturation() {
        return saturation.get();
    }

    /** Sets the saturation adjustment. */
    public void setSaturation(double newSaturation) {
        saturation.set(newSaturation);
    }

    /** Returns the content container floated above the glass layers. */
    public StackPane getContentBox() {
        return contentBox;
    }

    /** Raw-children view of the content box. */
    public ObservableList<Node> getGlassChildren() {
        return contentBox.getChildren();
    }

    /** Returns the single content node, or {@code null}. */
    public Node getContent() {
        return contentBox.getChildren().isEmpty() ? null : contentBox.getChildren().get(0);
    }

    /** Replaces the glass pane content. */
    public void setContent(Node node) {
        contentBox.getChildren().setAll(node);
    }

    /** Forces a backdrop re-capture on the next layout pulse. */
    public void invalidateBackdrop() {
        requestLayout();
        Skin<?> skin = getSkin();
        if (skin instanceof GlassPaneSkin glassSkin) {
            glassSkin.invalidateBackdrop();
        }
    }
}
