package io.m3fx.core.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

/**
 * Verifies the ported dynamic-color engine against the M3 baseline: seed #6750A4 with TonalSpot
 * must reproduce the spec's baseline light theme (primary #6750A4, primary-container #EADDFF).
 */
class M3ThemeTest {

    @Test
    void baselineLightSeedReproducesSpecColors() {
        // Seed #6750A4 is the spec's example seed. With the bundled 2026 color spec the baseline
        // light values are #65558f / #e9ddff (the older #6750a4 / #eaddff pair was the 2021 spec).
        M3Theme theme = M3Theme.fromSeed(Color.web("#6750A4"), ThemeVariant.TONAL_SPOT, false);
        assertEquals("#65558f", M3Theme.toWeb(theme.get(ColorRole.PRIMARY)));
        assertEquals("#e9ddff", M3Theme.toWeb(theme.get(ColorRole.PRIMARY_CONTAINER)));
    }

    @Test
    void darkSchemeUsesDarkSurfacesAndLightPrimary() {
        M3Theme light = M3Theme.fromSeed(Color.web("#6750A4"), ThemeVariant.TONAL_SPOT, false);
        M3Theme dark = M3Theme.fromSeed(Color.web("#6750A4"), ThemeVariant.TONAL_SPOT, true);
        // Dark surfaces must actually be dark, otherwise the whole dark gallery is wrong.
        assertTrue(
                dark.get(ColorRole.SURFACE).getBrightness() < 0.25,
                "dark surface was " + M3Theme.toWeb(dark.get(ColorRole.SURFACE)));
        // Dark primary is a light tone of the same hue family, not the dark light-theme color.
        assertTrue(
                dark.get(ColorRole.PRIMARY).getBrightness()
                        > light.get(ColorRole.PRIMARY).getBrightness(),
                "dark primary should be lighter than light primary");
        // Containers converge toward the surface from opposite ends: in light mode brightness
        // falls along lowest->highest (lowest is pure white), in dark mode it rises.
        ColorRole[] containers =
                new ColorRole[] {
                    ColorRole.SURFACE_CONTAINER_LOWEST,
                    ColorRole.SURFACE_CONTAINER_LOW,
                    ColorRole.SURFACE_CONTAINER,
                    ColorRole.SURFACE_CONTAINER_HIGH,
                    ColorRole.SURFACE_CONTAINER_HIGHEST
                };
        for (M3Theme theme : new M3Theme[] {light, dark}) {
            double previous = theme.dark() ? -1 : 2;
            for (ColorRole role : containers) {
                double brightness = theme.get(role).getBrightness();
                boolean ordered =
                        theme.dark()
                                ? brightness >= previous - 0.02
                                : brightness <= previous + 0.02;
                assertTrue(
                        ordered,
                        role + " out of order at " + brightness + " (dark=" + theme.dark() + ")");
                previous = brightness;
            }
        }
    }

    @Test
    void allVariantsResolveEveryRole() {
        for (ThemeVariant variant : ThemeVariant.values()) {
            for (boolean dark : new boolean[] {false, true}) {
                M3Theme theme = M3Theme.fromSeed(Color.web("#6750A4"), variant, dark);
                for (ColorRole role : ColorRole.values()) {
                    Color color = theme.get(role);
                    if (color == null) {
                        throw new AssertionError(variant + (dark ? " dark " : " light ") + role);
                    }
                }
                if (!theme.toCss().contains("-md-sys-color-primary")) {
                    throw new AssertionError("CSS missing primary for " + variant);
                }
            }
        }
    }

    @Test
    void expressiveDimRolesAreResolvedAndPublished() {
        M3Theme theme = M3Theme.fromSeed(Color.web("#6750A4"), ThemeVariant.EXPRESSIVE, true);
        for (ColorRole role : new ColorRole[] {
            ColorRole.PRIMARY_DIM, ColorRole.SECONDARY_DIM, ColorRole.TERTIARY_DIM, ColorRole.ERROR_DIM
        }) {
            assertTrue(theme.get(role) != null, "missing " + role);
            assertTrue(theme.toCss().contains(role.cssVariable()), "CSS missing " + role);
        }
    }
}
