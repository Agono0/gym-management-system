package io.m3fx.core.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Locks the M3 state-layer opacity tokens: hover 0.08, focus/pressed 0.10, dragged 0.16 per the
 * v34.0.21 state tokens ({@code _md-sys-state.scss}), plus the disabled-container (0.12) /
 * disabled-content (0.38) component opacities and the 0.32 scrim.
 */
class StateOpacityTest {

    @Test
    void stateLayerOpacitiesMatchTheVTokens() {
        assertEquals(0.08, StateOpacity.HOVER);
        assertEquals(0.10, StateOpacity.FOCUS);
        assertEquals(0.10, StateOpacity.PRESSED);
        assertEquals(0.16, StateOpacity.DRAGGED);
    }

    @Test
    void disabledOpacitiesMatchTheComponentTokens() {
        assertEquals(0.12, StateOpacity.DISABLED_CONTAINER);
        assertEquals(0.38, StateOpacity.DISABLED_CONTENT);
    }

    @Test
    void scrimMatchesTheM3ScrimToken() {
        assertEquals(0.32, StateOpacity.SCRIM);
    }

    @Test
    void opacitiesAreValidAlphas() {
        double[] values = {
            StateOpacity.HOVER, StateOpacity.FOCUS, StateOpacity.PRESSED, StateOpacity.DRAGGED,
            StateOpacity.DISABLED_CONTAINER, StateOpacity.DISABLED_CONTENT, StateOpacity.SCRIM
        };
        for (double value : values) {
            assertTrue(value > 0 && value <= 1.0, "opacity " + value + " outside (0, 1]");
        }
    }
}
