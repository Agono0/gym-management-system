package io.m3fx.controls.glass;

import javafx.beans.DefaultProperty;
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
 * {@code DisplacementMap} refraction ({@link GlassRefraction}) &rarr; saturation lift &rarr;
 * optional tint &rarr; specular rim + 1px edge stroke &rarr; drop shadow &rarr; content. All
 * colors resolve from {@code -md-sys-color-*} theme roles, so the glass is correct in light and
 * dark and with any seed.
 *
 * <p>Properties cover the whole pipeline: {@link #glassEffectProperty() glassEffect}
 * ({@link GlassEffectParams} bundle), {@link #variantProperty() variant}, {@link #tintProperty()
 * tint}, {@link #shapeProperty() shape}, {@link #blurRadiusProperty() blurRadius},
 * {@link #cornerRadiusProperty() cornerRadius}, {@link #glassOpacityProperty() glassOpacity},
 * {@link #refractionProperty() refraction}, {@link #specularProperty() specular},
 * {@link #specularAngleProperty() specularAngle}, {@link #saturationProperty() saturation},
 * {@link #shadowProperty() shadow} and
 * {@link #interactiveProperty() interactive}. Setting {@code glassEffect} copies the bundle into
 * the individual properties (the bundle itself is immutable). The bundle accessor is named
 * {@code glassEffect} rather than {@code effect} because {@code Node} already owns
 * {@code effectProperty()} ({@code ObjectProperty<Effect>}); an {@code effect} property of a
 * different type could not override it.
 *
 * <p>Backdrop capture is cached and invalidated on scene/parent/layout/size/theme changes &mdash;
 * never per frame. Live-animated backdrops refresh only on invalidation; call
 * {@link #invalidateBackdrop()} to force a re-capture.
 *
 * <p>Factories: {@link #regular()}, {@link #clear()}, {@link #of(GlassEffectParams)}.
 */
// Suppressed this-escape (false positives javac 27 cannot see across the module boundary):
// (1) the JavaFX property-bean idiom new Simple*Property(this, ...) - the property
// constructor only stores the bean reference, exactly like every built-in control;
// (2) constructor calls to JavaFX final methods (getStyleClass()) and to the non-final
// property setters from the factories, which run after full construction on a fresh
// instance. This class's own overridable methods are never called during construction.
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
     * live properties. Named {@code setGlassEffect} (not {@code setEffect}) to stay clear of
     * {@link Node#setEffect(javafx.scene.effect.Effect)}.
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
     * Sets the glass variant (blur/saturation defaults are applied by the factories; this only
     * switches the look used by the skin).
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

    /** Returns the glass shape property. Named {@code glassShape}: {@link Region} already owns a
     * final {@code shapeProperty()} ({@code Shape} clip). */
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
     * Sets whether the glass reacts to the pointer: pressing flattens the bevel (~40% less
     * refraction) and deepens the shadow; hovering shifts the specular highlight toward the
     * pointer. Springs run through {@code SpringAnimation}.
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

    /**
     * Enables or disables the drop shadow (spec default 0.30 alpha / 10 radius / +1 offsetY).
     *
     * @param newShadow {@code true} to show the shadow
     */
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

    /**
     * Sets the backdrop blur radius in px.
     *
     * @param newBlurRadius blur radius in px
     */
    public void setBlurRadius(double newBlurRadius) {
        blurRadius.set(newBlurRadius);
    }

    /** Returns the corner radius property. */
    public DoubleProperty cornerRadiusProperty() {
        return cornerRadius;
    }

    /** Returns the corner radius in px (used by {@link GlassShape#RECT}). */
    public double getCornerRadius() {
        return cornerRadius.get();
    }

    /**
     * Sets the corner radius in px.
     *
     * @param newCornerRadius corner radius in px
     */
    public void setCornerRadius(double newCornerRadius) {
        cornerRadius.set(newCornerRadius);
    }

    /** Returns the glass opacity property. */
    public DoubleProperty glassOpacityProperty() {
        return glassOpacity;
    }

    /** Returns the opacity of the milk/tint layer (legacy default 0.65). */
    public double getGlassOpacity() {
        return glassOpacity.get();
    }

    /**
     * Sets the opacity of the milk/tint layer, clamped to {@code [0,1]} when applied.
     *
     * @param newGlassOpacity opacity in {@code [0,1]}
     */
    public void setGlassOpacity(double newGlassOpacity) {
        glassOpacity.set(newGlassOpacity);
    }

    /** Returns the refraction property. */
    public DoubleProperty refractionProperty() {
        return refraction;
    }

    /** Returns the refraction strength multiplier (default 1.0). */
    public double getRefraction() {
        return refraction.get();
    }

    /**
     * Sets the refraction strength multiplier.
     *
     * @param newRefraction multiplier, 0 disables refraction
     */
    public void setRefraction(double newRefraction) {
        refraction.set(newRefraction);
    }

    /** Returns the map-style property (liquidGL shader vs nikdelvin SVG displacement). */
    public ObjectProperty<GlassEffectParams.MapStyle> mapStyleProperty() {
        return mapStyle;
    }

    /** Returns the displacement-field source. */
    public GlassEffectParams.MapStyle getMapStyle() {
        return mapStyle.get();
    }

    /** Sets the displacement-field source. */
    public void setMapStyle(GlassEffectParams.MapStyle newMapStyle) {
        mapStyle.set(newMapStyle);
    }

    /** Returns the bevel-depth property (UV, the {@code edge^10} rim term). */
    public DoubleProperty bevelDepthProperty() {
        return bevelDepth;
    }

    /** Returns the bevel depth. */
    public double getBevelDepth() {
        return bevelDepth.get();
    }

    /** Sets the bevel depth. */
    public void setBevelDepth(double newBevelDepth) {
        bevelDepth.set(newBevelDepth);
    }

    /** Returns the bevel-width property (fraction of the smaller side). */
    public DoubleProperty bevelWidthProperty() {
        return bevelWidth;
    }

    /** Returns the bevel width. */
    public double getBevelWidth() {
        return bevelWidth.get();
    }

    /** Sets the bevel width. */
    public void setBevelWidth(double newBevelWidth) {
        bevelWidth.set(newBevelWidth);
    }

    /** Returns the frost property (liquidGL {@code u_frost}, 0 = sharp). */
    public DoubleProperty frostProperty() {
        return frost;
    }

    /** Returns the frost strength. */
    public double getFrost() {
        return frost.get();
    }

    /** Sets the frost strength. */
    public void setFrost(double newFrost) {
        frost.set(newFrost);
    }

    /** Returns the chromatic-aberration property (px). */
    public DoubleProperty aberrationProperty() {
        return aberration;
    }

    /** Returns the chromatic aberration. */
    public double getAberration() {
        return aberration.get();
    }

    /** Sets the chromatic aberration. */
    public void setAberration(double newAberration) {
        aberration.set(newAberration);
    }

    /** Returns the magnification property. */
    public DoubleProperty magnifyProperty() {
        return magnify;
    }

    /** Returns the backdrop magnification. */
    public double getMagnify() {
        return magnify.get();
    }

    /** Sets the backdrop magnification. */
    public void setMagnify(double newMagnify) {
        magnify.set(newMagnify);
    }

    /** Returns the nikdelvin bezel-depth property (px). */
    public DoubleProperty nikdelvinDepthProperty() {
        return nikdelvinDepth;
    }

    /** Returns the nikdelvin bezel depth. */
    public double getNikdelvinDepth() {
        return nikdelvinDepth.get();
    }

    /** Sets the nikdelvin bezel depth. */
    public void setNikdelvinDepth(double newDepth) {
        nikdelvinDepth.set(newDepth);
    }

    /** Returns the nikdelvin displacement-scale property (px). */
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

    /** Returns the specular intensity property. */
    public DoubleProperty specularProperty() {
        return specular;
    }

    /** Returns the specular rim intensity (default 0.35). */
    public double getSpecular() {
        return specular.get();
    }

    /**
     * Sets the specular rim intensity.
     *
     * @param newSpecular intensity in {@code [0,1]}
     */
    public void setSpecular(double newSpecular) {
        specular.set(newSpecular);
    }

    /** Returns the specular angle property. */
    public DoubleProperty specularAngleProperty() {
        return specularAngle;
    }

    /** Returns the specular light angle in degrees (CSS convention: 0 = to top, clockwise). */
    public double getSpecularAngle() {
        return specularAngle.get();
    }

    /**
     * Sets the specular light angle in degrees (CSS convention: 0 = to top, clockwise; default
     * 110 = highlight from the top-left).
     *
     * @param newSpecularAngle angle in degrees
     */
    public void setSpecularAngle(double newSpecularAngle) {
        specularAngle.set(newSpecularAngle);
    }

    /** Returns the saturation property. */
    public DoubleProperty saturationProperty() {
        return saturation;
    }

    /** Returns the saturation adjustment, or {@link #SATURATION_AUTO} when variant-derived. */
    public double getSaturation() {
        return saturation.get();
    }

    /**
     * Sets the {@code ColorAdjust} saturation adjustment; negative restores the variant default
     * (+0.18 regular, +0.10 clear).
     *
     * @param newSaturation saturation adjustment
     */
    public void setSaturation(double newSaturation) {
        saturation.set(newSaturation);
    }

    /** Returns the content container floated above the glass layers. */
    public StackPane getContentBox() {
        return contentBox;
    }

    /**
     * Raw-children view of the content box — the FXML default property, so legacy screens that
     * nested children directly inside a {@code <M3GlassPane>} (the old StackPane-based pane
     * allowed exactly that) keep loading and stacking identically.
     *
     * @return the live child list of the content box
     */
    public ObservableList<Node> getGlassChildren() {
        return contentBox.getChildren();
    }

    /** Returns the single content node (for FXML {@code <content>} support), or {@code null}. */
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

    /**
     * Forces a backdrop re-capture on the next layout pulse. The capture is otherwise cached and
     * invalidated automatically on scene/parent/layout/size/theme changes; live-animated backdrops
     * do not refresh continuously, so call this (e.g. from an animation listener) when the content
     * behind the glass moves.
     */
    public void invalidateBackdrop() {
        requestLayout();
        Skin<?> skin = getSkin();
        if (skin instanceof GlassPaneSkin glassSkin) {
            glassSkin.invalidateBackdrop();
        }
    }
}
