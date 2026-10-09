package io.m3fx.core.token;

/**
 * M3 state-layer opacities.
 *
 * <p>See <a href="https://m3.material.io/foundations/interaction/states/overview">M3 interaction
 * states</a> and {@code material-web/tokens/_md-sys-state.scss} (v0.192).
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
