package io.m3fx.controls.common;

import io.m3fx.core.token.MotionTokens.SpringToken;

/** Spring tuning: stiffness (mass = 1) plus damping ratio (1 = critically damped, &lt;1 = bounce). */
public record SpringParams(double stiffness, double dampingRatio) {
    /** Creates SpringParams from an M3 {@code MotionTokens.SpringToken}. */
    public static SpringParams fromToken(SpringToken token) {
        return new SpringParams(token.stiffness(), token.dampingRatio());
    }
}
