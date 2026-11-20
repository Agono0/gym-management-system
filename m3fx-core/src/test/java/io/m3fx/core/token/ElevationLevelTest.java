package io.m3fx.core.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

/**
 * Locks the M3 elevation shadow mapping: levels 0-5 with their offset/blur/opacity values,
 * clamped {@code of()} lookup, and the {@link DropShadow} geometry produced by {@code toEffect}.
 *
 * <p>Values are the M3 elevation shadow tokens (level 1: y 1 / blur 3 … level 5: y 8 / blur 16)
 * quoted in the {@link ElevationLevel} constants.
 */
class ElevationLevelTest {

    @Test
    void ofMapsLevelsAndClampsToTheRange() {
        assertSame(ElevationLevel.LEVEL0, ElevationLevel.of(0));
        assertSame(ElevationLevel.LEVEL1, ElevationLevel.of(1));
        assertSame(ElevationLevel.LEVEL2, ElevationLevel.of(2));
        assertSame(ElevationLevel.LEVEL3, ElevationLevel.of(3));
        assertSame(ElevationLevel.LEVEL4, ElevationLevel.of(4));
        assertSame(ElevationLevel.LEVEL5, ElevationLevel.of(5));
        assertSame(ElevationLevel.LEVEL0, ElevationLevel.of(-3), "below the range clamps to 0");
        assertSame(ElevationLevel.LEVEL5, ElevationLevel.of(9), "above the range clamps to 5");
    }

    @Test
    void levelNumbersAreZeroBasedAndOrdered() {
        ElevationLevel[] values = ElevationLevel.values();
        assertEquals(6, values.length);
        for (int i = 0; i < values.length; i++) {
            assertEquals(i, values[i].level());
        }
    }

    @Test
    void toEffectProducesTheTokenShadowGeometry() {
        DropShadow shadow = ElevationLevel.LEVEL3.toEffect(Color.web("#010101"));
        assertEquals(0, shadow.getOffsetX(), "M3 key shadows are vertically offset only");
        assertEquals(4, shadow.getOffsetY(), 1e-9, "level 3 offset y");
        assertEquals(8, shadow.getRadius(), 1e-9, "level 3 blur radius");
        assertEquals(0, shadow.getSpread(), 1e-9);
        // DropShadow stores the color channels as floats, so compare with a float-safe delta.
        assertEquals(0.16, shadow.getColor().getOpacity(), 1e-6, "level 3 shadow opacity");
        assertEquals(Color.web("#010101").getRed(), shadow.getColor().getRed(), 1e-6,
                "shadow keeps the given color hue with the token alpha");
    }

    @Test
    void toEffectFallsBackToBlackOnNullShadowColor() {
        DropShadow shadow = ElevationLevel.LEVEL1.toEffect(null);
        assertEquals(Color.BLACK.getRed(), shadow.getColor().getRed(), 1e-6);
        assertEquals(0.15, shadow.getColor().getOpacity(), 1e-6);
    }

    @Test
    void shadowDeepensWithElevation() {
        // Level order must deepen offset and blur monotonically (level 0 casts no shadow).
        assertEquals(0, ElevationLevel.LEVEL0.toEffect(Color.BLACK).getRadius(), 1e-9);
        for (int i = 1; i < ElevationLevel.values().length; i++) {
            DropShadow lower = ElevationLevel.values()[i - 1].toEffect(Color.BLACK);
            DropShadow higher = ElevationLevel.values()[i].toEffect(Color.BLACK);
            assertTrue(higher.getOffsetY() > lower.getOffsetY(),
                    "offset must grow from level " + (i - 1));
            assertTrue(higher.getRadius() > lower.getRadius(),
                    "blur must grow from level " + (i - 1));
        }
    }
}
