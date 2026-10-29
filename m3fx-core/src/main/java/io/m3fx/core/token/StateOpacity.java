package io.m3fx.core.token;

/**
 * M3 state-layer opacities.
 *
 * <p>See <a href="https://m3.material.io/foundations/interaction/states/state-layers">M3 state
 * layers</a> and {@code material-web/tokens/versions/latest/sass/_md-sys-state.scss} (v34.0.21:
 * hover 0.08, focus 0.1, pressed 0.1, dragged 0.16; the older v0.192 release used 0.12 for
 * focus/pressed). Disabled container 0.12 and disabled content 0.38 match the component disabled
 * container/label-text opacity tokens (e.g. {@code _md-comp-filled-button.scss}).
 */
public final class StateOpacity {

    private StateOpacity() {}

    /** Hover overlay opacity (8%). */
    public static final double HOVER = 0.08;
    /** Focus overlay opacity (10%). */
    public static final double FOCUS = 0.10;
    /** Pressed overlay opacity (10%). */
    public static final double PRESSED = 0.10;
    /** Dragged overlay opacity (16%). */
    public static final double DRAGGED = 0.16;
    /** Disabled container opacity (12%). */
    public static final double DISABLED_CONTAINER = 0.12;
    /** Disabled content opacity (38%). */
    public static final double DISABLED_CONTENT = 0.38;
    /** Scrim opacity (32%). */
    public static final double SCRIM = 0.32;
}
