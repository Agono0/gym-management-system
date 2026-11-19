package io.m3fx.core.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.beans.value.ChangeListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Locks the reduced-motion flag semantics: system-property initialization, default off, listener
 * delivery on change, and the fade-cap constant consumed by the reduced-motion contract (see
 * {@code docs/SPEC_NOTES/reduced-motion.md}).
 *
 * <p>The flag is global static state, so every test sets its precondition explicitly and
 * {@link #reset()} restores the off state afterwards.
 */
class ReducedMotionTest {

    @AfterEach
    void reset() {
        MotionTokens.setReducedMotion(false);
    }

    @Test
    void flagStartsOffAndIsMutable() {
        // Initialization path: with the property unset (Gradle test JVM), the flag starts off.
        assertFalse(MotionTokens.fromSystemProperty(), "property unset must default to false");
        MotionTokens.setReducedMotion(false);
        assertFalse(MotionTokens.isReducedMotion());
        MotionTokens.setReducedMotion(true);
        assertTrue(MotionTokens.isReducedMotion());
    }

    @Test
    void systemPropertyHonored() {
        System.setProperty("m3fx.reducedMotion", "true");
        try {
            assertTrue(MotionTokens.fromSystemProperty(), "-Dm3fx.reducedMotion=true");
            System.setProperty("m3fx.reducedMotion", "False");
            assertFalse(MotionTokens.fromSystemProperty(), "only lowercase-true enables");
            System.setProperty("m3fx.reducedMotion", "garbage");
            assertFalse(MotionTokens.fromSystemProperty(), "invalid values disable silently");
            System.clearProperty("m3fx.reducedMotion");
            assertFalse(MotionTokens.fromSystemProperty());
        } finally {
            System.clearProperty("m3fx.reducedMotion");
        }
    }

    @Test
    void listenerFiresOnChangeOnly() {
        MotionTokens.setReducedMotion(false);
        int[] fires = {0};
        boolean[] lastValue = {false};
        ChangeListener<Boolean> listener =
                (obs, was, is) -> {
                    fires[0]++;
                    lastValue[0] = is;
                };
        MotionTokens.reducedMotionProperty().addListener(listener);
        try {
            MotionTokens.setReducedMotion(true);
            MotionTokens.setReducedMotion(true);
            MotionTokens.setReducedMotion(false);
            assertEquals(2, fires[0], "no-op writes must not fire");
            assertFalse(lastValue[0], "last fire must carry the new value");
        } finally {
            MotionTokens.reducedMotionProperty().removeListener(listener);
        }
    }

    @Test
    void reducedFadeCapIsSane() {
        assertEquals(150, MotionTokens.DURATION_REDUCED_FADE);
        assertTrue(MotionTokens.DURATION_REDUCED_FADE <= MotionTokens.DURATION_SHORT_3,
                "reduced-motion fades must stay within the 150ms short-duration step");
        assertTrue(MotionTokens.DURATION_REDUCED_FADE > 0);
    }
}
