package io.m3fx.core.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/**
 * Locks the M3 shape scale: all ten corner tokens (values quoted from the generated v34.0.21
 * token set), the {@code forName} lookup over the complete scale with the {@code none} default,
 * the deprecated pre-A2 aliases, and strict scale ordering.
 *
 * <p>Values and spec names: see {@code docs/SPEC_NOTES/shape-scale.md}.
 */
class ShapeTokensTest {

    private static final String[] NAMES = {
        "none",
        "extra-small",
        "small",
        "medium",
        "large",
        "large-increased",
        "extra-large",
        "extra-large-increased",
        "extra-extra-large",
        "full",
    };

    private static final double[] VALUES = {0, 4, 8, 12, 16, 20, 28, 32, 48, 9999};

    @Test
    void forNameRoundTripsEveryNamedToken() {
        for (int i = 0; i < NAMES.length; i++) {
            assertEquals(VALUES[i], ShapeTokens.forName(NAMES[i]), NAMES[i]);
        }
    }

    @Test
    void tokenConstantsMatchTheScale() {
        assertEquals(0, ShapeTokens.NONE);
        assertEquals(4, ShapeTokens.EXTRA_SMALL);
        assertEquals(8, ShapeTokens.SMALL);
        assertEquals(12, ShapeTokens.MEDIUM);
        assertEquals(16, ShapeTokens.LARGE);
        assertEquals(20, ShapeTokens.LARGE_INCREASED);
        assertEquals(28, ShapeTokens.EXTRA_LARGE);
        assertEquals(32, ShapeTokens.EXTRA_LARGE_INCREASED);
        assertEquals(48, ShapeTokens.EXTRA_EXTRA_LARGE);
        assertEquals(9999, ShapeTokens.FULL);
    }

    @Test
    void unknownOrNullNamesResolveToNone() {
        assertEquals(ShapeTokens.NONE, ShapeTokens.forName("nonsense"));
        assertEquals(ShapeTokens.NONE, ShapeTokens.forName(""));
        assertEquals(ShapeTokens.NONE, ShapeTokens.forName("Extra-Small"));
        assertEquals(ShapeTokens.NONE, ShapeTokens.forName(null));
    }

    @Test
    void legacyExpressiveAliasesPointAtSpecNamedConstants() throws Exception {
        // The pre-A2 constants were misnamed ("expressive large-increased" etc.); they must keep
        // compiling as @Deprecated aliases of the spec-named tokens. Reflection reads the
        // deprecated constants without triggering deprecation warnings.
        double largeIncreased = deprecatedConstant("EXPRESSIVE_LARGE_INCREASED");
        double extraExtraLarge = deprecatedConstant("EXPRESSIVE_EXTRA_EXTRA_LARGE");
        assertEquals(ShapeTokens.EXTRA_LARGE_INCREASED, largeIncreased);
        assertEquals(ShapeTokens.EXTRA_EXTRA_LARGE, extraExtraLarge);
        assertEquals(32, largeIncreased);
        assertEquals(48, extraExtraLarge);
    }

    private static double deprecatedConstant(String name) throws Exception {
        Field field = ShapeTokens.class.getField(name);
        assertTrue(Modifier.isPublic(field.getModifiers()), name + " must stay public");
        assertNotNull(field.getAnnotation(Deprecated.class), name + " must be @Deprecated");
        return (double) field.get(null);
    }

    @Test
    void scaleIsStrictlyIncreasing() {
        double previous = Double.NEGATIVE_INFINITY;
        for (double value : VALUES) {
            assertTrue(value > previous, "scale not increasing at " + value);
            previous = value;
        }
    }
}
