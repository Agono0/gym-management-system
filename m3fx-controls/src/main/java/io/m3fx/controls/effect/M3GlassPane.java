package io.m3fx.controls.effect;

import javafx.scene.Node;

/**
 * Cyber-glass / Acrylic Glassmorphism container featuring a frosted backdrop blur,
 * translucent surface container tinting, and specular rim highlight.
 *
 * <p>Backward-compatible wrapper: the real liquid glass now lives in
 * {@link io.m3fx.controls.glass.M3GlassPane} (Control + skin with backdrop capture, blur,
 * refraction, specular rim and theme-aware tint); this class simply extends it with the legacy
 * acrylic defaults (blur 16, corner radius 16, glass opacity 0.65). All legacy properties and
 * accessors keep their meaning.
 */
// Suppressed this-escape: the constructors call the non-final property setters (setBlurRadius
// and friends) on this during construction; they only write bean state, and this class adds no
// overridable members of its own.
@SuppressWarnings("this-escape")
public class M3GlassPane extends io.m3fx.controls.glass.M3GlassPane {

    /** Creates an empty glass pane with default 16px blur. */
    public M3GlassPane() {
        this(null);
    }

    /**
     * Creates a glass pane with content.
     *
     * @param content child content
     */
    public M3GlassPane(Node content) {
        super(content);
        setBlurRadius(16.0);
        setCornerRadius(16.0);
        setGlassOpacity(0.65);
    }

    /**
     * Creates a glass pane with content, corner radius, and blur radius.
     *
     * @param content child content
     * @param cornerRadius corner radius in px
     * @param blurRadius blur radius in px
     */
    public M3GlassPane(Node content, double cornerRadius, double blurRadius) {
        super(content, cornerRadius, blurRadius);
    }
}
