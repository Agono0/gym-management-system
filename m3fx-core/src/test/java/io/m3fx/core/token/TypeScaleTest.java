package io.m3fx.core.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Locks the full 15-role M3 type scale: completeness of {@link TypeScale#ALL}, spec-unique role
 * names, {@code forName} round-trips with the {@code body-medium} default, and the exact
 * size/line-height/tracking/weight values.
 *
 * <p>Values are the M3 type-scale table (display-large 57/64/-0.25/400 … body-small 12/16/0.4/400)
 * and were cross-checked against the generated material-web v34.0.21 type-scale tokens
 * ({@code _md-sys-typescale.scss}: display-large size 3.5625rem = 57sp, line-height 4rem = 64sp,
 * tracking -0.015625rem = -0.25sp, wght 400).
 */
class TypeScaleTest {

    @Test
    void allFifteenRolesExistInSpecOrder() {
        assertEquals(15, TypeScale.ALL.length);
        TypeRole[] expected = {
            TypeScale.DISPLAY_LARGE, TypeScale.DISPLAY_MEDIUM, TypeScale.DISPLAY_SMALL,
            TypeScale.HEADLINE_LARGE, TypeScale.HEADLINE_MEDIUM, TypeScale.HEADLINE_SMALL,
            TypeScale.TITLE_LARGE, TypeScale.TITLE_MEDIUM, TypeScale.TITLE_SMALL,
            TypeScale.LABEL_LARGE, TypeScale.LABEL_MEDIUM, TypeScale.LABEL_SMALL,
            TypeScale.BODY_LARGE, TypeScale.BODY_MEDIUM, TypeScale.BODY_SMALL,
        };
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], TypeScale.ALL[i], "role " + i + " out of spec order");
        }
    }

    @Test
    void roleNamesAreUniqueAndSpecNamed() {
        Set<String> names = new HashSet<>();
        for (TypeRole role : TypeScale.ALL) {
            assertTrue(names.add(role.name()), "duplicate name " + role.name());
        }
        // Spot-check the kebab-case spec names.
        assertEquals("display-large", TypeScale.DISPLAY_LARGE.name());
        assertEquals("title-medium", TypeScale.TITLE_MEDIUM.name());
        assertEquals("label-small", TypeScale.LABEL_SMALL.name());
        assertEquals("body-medium", TypeScale.BODY_MEDIUM.name());
    }

    @Test
    void forNameRoundTripsEveryRoleAndDefaultsToBodyMedium() {
        for (TypeRole role : TypeScale.ALL) {
            assertEquals(role, TypeScale.forName(role.name()));
        }
        assertEquals(TypeScale.BODY_MEDIUM, TypeScale.forName("nonsense"));
        assertEquals(TypeScale.BODY_MEDIUM, TypeScale.forName(null));
        assertEquals(TypeScale.BODY_MEDIUM, TypeScale.forName("Display-Large"),
                "lookup is case-sensitive by design, unknown names fall back");
    }

    @Test
    void rolesCarryTheSpecValues() {
        check("display-large", 57, 64, -0.25, 400, TypeScale.DISPLAY_LARGE);
        check("display-medium", 45, 52, 0, 400, TypeScale.DISPLAY_MEDIUM);
        check("display-small", 36, 44, 0, 400, TypeScale.DISPLAY_SMALL);
        check("headline-large", 32, 40, 0, 400, TypeScale.HEADLINE_LARGE);
        check("headline-medium", 28, 36, 0, 400, TypeScale.HEADLINE_MEDIUM);
        check("headline-small", 24, 32, 0, 400, TypeScale.HEADLINE_SMALL);
        check("title-large", 22, 28, 0, 400, TypeScale.TITLE_LARGE);
        check("title-medium", 16, 24, 0.15, 500, TypeScale.TITLE_MEDIUM);
        check("title-small", 14, 20, 0.1, 500, TypeScale.TITLE_SMALL);
        check("label-large", 14, 20, 0.1, 500, TypeScale.LABEL_LARGE);
        check("label-medium", 12, 16, 0.5, 500, TypeScale.LABEL_MEDIUM);
        check("label-small", 11, 16, 0.5, 500, TypeScale.LABEL_SMALL);
        check("body-large", 16, 24, 0.5, 400, TypeScale.BODY_LARGE);
        check("body-medium", 14, 20, 0.25, 400, TypeScale.BODY_MEDIUM);
        check("body-small", 12, 16, 0.4, 400, TypeScale.BODY_SMALL);
    }

    @Test
    void lineHeightAndTrackingSanityAcrossTheScale() {
        // Line height always exceeds the font size and tracking stays within the M3 range.
        for (TypeRole role : TypeScale.ALL) {
            assertTrue(role.lineHeight() > role.size(),
                    role.name() + " line height must exceed the font size");
            assertTrue(role.tracking() >= -0.5 && role.tracking() <= 0.5,
                    role.name() + " tracking outside the M3 ±0.5sp range");
            assertTrue(role.weight() == 400 || role.weight() == 500,
                    role.name() + " weight must be 400 or 500");
        }
    }

    private static void check(
            String name, double size, double lineHeight, double tracking, int weight,
            TypeRole role) {
        assertEquals(name, role.name());
        assertEquals(size, role.size(), 1e-9, name + " size");
        assertEquals(lineHeight, role.lineHeight(), 1e-9, name + " line height");
        assertEquals(tracking, role.tracking(), 1e-9, name + " tracking");
        assertEquals(weight, role.weight(), name + " weight");
    }
}
