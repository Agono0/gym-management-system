package io.m3fx.core.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

/**
 * Verifies the additive spec-version exposure: explicit SPEC_2025 / SPEC_2026 requests resolve
 * every role and differ from the default SPEC_2021 path where the newer specs change values, the
 * documented variant fallbacks hold, and the CMF variant resolves end to end.
 *
 * <p>Pinned hex values are regression pins of the vendored engine's output (observed for seed
 * #6750A4), not numbers quoted from m3.material.io; see
 * {@code docs/SPEC_NOTES/color-system-versions.md}.
 */
class M3ThemeSpecVersionsTest {

    private static final Color SEED = Color.web("#6750A4");

    @Test
    void explicitSpec2025ResolvesEveryRoleAndDiffersFrom2021() {
        for (boolean dark : new boolean[] {false, true}) {
            M3Theme theme =
                    M3Theme.fromSeed(SEED, ThemeVariant.TONAL_SPOT, dark, M3Theme.CONTRAST_STANDARD,
                            ColorSpecVersion.SPEC_2025);
            assertEquals(ColorSpecVersion.SPEC_2025, theme.specVersion());
            for (ColorRole role : ColorRole.values()) {
                assertNotNull(theme.get(role), () -> role + " unresolved (dark=" + dark + ")");
            }
            assertTrue(theme.toCss().contains("-md-sys-color-primary"));
            M3Theme baseline = M3Theme.fromSeed(SEED, ThemeVariant.TONAL_SPOT, dark);
            // Roles the 2025 spec changes for this seed (observed): primary, its container,
            // on-surface, on-surface-variant, and the dark surface ramp (lowest container drops
            // to T0 in dark, where 2021 uses T4). Light surface-container-lowest stays T100.
            assertEquals(dark ? "#cdc0ec" : "#655789", M3Theme.toWeb(theme.get(ColorRole.PRIMARY)));
            assertEquals(
                    dark ? "#574d72" : "#d4c3fd",
                    M3Theme.toWeb(theme.get(ColorRole.PRIMARY_CONTAINER)));
            assertEquals(
                    dark ? "#eae3ef" : "#34313a", M3Theme.toWeb(theme.get(ColorRole.ON_SURFACE)));
            assertEquals(
                    dark ? "#aea9b4" : "#615d68",
                    M3Theme.toWeb(theme.get(ColorRole.ON_SURFACE_VARIANT)));
            assertEquals(
                    dark ? "#000000" : "#ffffff",
                    M3Theme.toWeb(theme.get(ColorRole.SURFACE_CONTAINER_LOWEST)));
            if (!dark) {
                assertEquals(
                        baseline.get(ColorRole.SURFACE_CONTAINER_LOWEST),
                        theme.get(ColorRole.SURFACE_CONTAINER_LOWEST));
            }
        }
    }

    @Test
    void requestingSpec2026ForTonalSpotFallsBackToSpec2025() {
        // 2026 role definitions are written for the CMF variant only, so TonalSpot keeps the
        // requested version only up to 2025 (DynamicScheme.maybeFallbackSpecVersion).
        M3Theme theme =
                M3Theme.fromSeed(SEED, ThemeVariant.TONAL_SPOT, false, M3Theme.CONTRAST_STANDARD,
                        ColorSpecVersion.SPEC_2026);
        assertEquals(ColorSpecVersion.SPEC_2025, theme.specVersion());
        M3Theme explicit2025 =
                M3Theme.fromSeed(SEED, ThemeVariant.TONAL_SPOT, false, M3Theme.CONTRAST_STANDARD,
                        ColorSpecVersion.SPEC_2025);
        for (ColorRole role : ColorRole.values()) {
            assertEquals(explicit2025.get(role), theme.get(role), "role " + role + " diverged");
        }
    }

    @Test
    void fidelityContentRainbowFruitSaladMonochromeFallBackToSpec2021() {
        ThemeVariant[] fallbackVariants = {
            ThemeVariant.FIDELITY,
            ThemeVariant.CONTENT,
            ThemeVariant.RAINBOW,
            ThemeVariant.FRUIT_SALAD,
            ThemeVariant.MONOCHROME
        };
        for (ThemeVariant variant : fallbackVariants) {
            for (ColorSpecVersion requested :
                    new ColorSpecVersion[] {ColorSpecVersion.SPEC_2025, ColorSpecVersion.SPEC_2026}) {
                M3Theme theme = M3Theme.fromSeed(SEED, variant, false, M3Theme.CONTRAST_STANDARD,
                        requested);
                assertEquals(ColorSpecVersion.SPEC_2021, theme.specVersion(),
                        () -> variant + " with " + requested);
                M3Theme baseline = M3Theme.fromSeed(SEED, variant, false);
                for (ColorRole role : ColorRole.values()) {
                    assertEquals(baseline.get(role), theme.get(role),
                            () -> variant + " role " + role + " diverged");
                }
            }
        }
    }

    @Test
    void cmfRequiresSpec2026() {
        assertThrows(IllegalArgumentException.class,
                () -> M3Theme.fromSeed(SEED, ThemeVariant.CMF, false, M3Theme.CONTRAST_STANDARD,
                        ColorSpecVersion.SPEC_2021));
        assertThrows(IllegalArgumentException.class,
                () -> M3Theme.fromSeed(SEED, ThemeVariant.CMF, false, M3Theme.CONTRAST_STANDARD,
                        ColorSpecVersion.SPEC_2025));
    }

    @Test
    void legacyOverloadDefaultsCmfToSpec2026() {
        // CMF has no 2021 resolution, so the pre-A1 overload (no explicit spec version) resolves
        // it with its only defined spec; callers of the default path (e.g. the gallery's variant
        // picker) keep working.
        for (boolean dark : new boolean[] {false, true}) {
            M3Theme legacy = M3Theme.fromSeed(SEED, ThemeVariant.CMF, dark);
            assertEquals(ColorSpecVersion.SPEC_2026, legacy.specVersion());
            M3Theme explicit =
                    M3Theme.fromSeed(SEED, ThemeVariant.CMF, dark, M3Theme.CONTRAST_STANDARD,
                            ColorSpecVersion.SPEC_2026);
            for (ColorRole role : ColorRole.values()) {
                assertEquals(explicit.get(role), legacy.get(role), "role " + role + " diverged");
            }
        }
    }

    @Test
    void cmfResolvesEveryRoleAndEmitsCss() {
        for (boolean dark : new boolean[] {false, true}) {
            M3Theme theme =
                    M3Theme.fromSeed(SEED, ThemeVariant.CMF, dark, M3Theme.CONTRAST_STANDARD,
                            ColorSpecVersion.SPEC_2026);
            assertEquals(ColorSpecVersion.SPEC_2026, theme.specVersion());
            for (ColorRole role : ColorRole.values()) {
                assertNotNull(theme.get(role), () -> role + " unresolved (dark=" + dark + ")");
            }
            assertTrue(theme.toCss().contains("-md-sys-color-primary"));
            assertTrue(theme.toCss().contains("-md-sys-color-on-primary"));
        }
    }

    @Test
    void cmfUses2026RoleDefinitions() {
        M3Theme light =
                M3Theme.fromSeed(SEED, ThemeVariant.CMF, false, M3Theme.CONTRAST_STANDARD,
                        ColorSpecVersion.SPEC_2026);
        M3Theme dark =
                M3Theme.fromSeed(SEED, ThemeVariant.CMF, true, M3Theme.CONTRAST_STANDARD,
                        ColorSpecVersion.SPEC_2026);
        // 2026 remaps the *dim roles onto their base roles (primary_dim == primary).
        assertEquals(light.get(ColorRole.PRIMARY), light.get(ColorRole.PRIMARY_DIM));
        assertEquals(dark.get(ColorRole.PRIMARY), dark.get(ColorRole.PRIMARY_DIM));
        // 2026 surface ramp pins the container tones: dark lowest = T0, light highest = T90.
        assertEquals("#000000", M3Theme.toWeb(dark.get(ColorRole.SURFACE_CONTAINER_LOWEST)));
        assertEquals("#ffffff", M3Theme.toWeb(light.get(ColorRole.SURFACE_CONTAINER_LOWEST)));
    }

    @Test
    void mediumAndHighContrastResolveForEveryExposedPath() {
        ThemeVariant[] variants = {ThemeVariant.TONAL_SPOT, ThemeVariant.EXPRESSIVE,
            ThemeVariant.CMF};
        ColorSpecVersion[] versions = {ColorSpecVersion.SPEC_2021, ColorSpecVersion.SPEC_2025,
            ColorSpecVersion.SPEC_2026};
        for (ThemeVariant variant : variants) {
            for (ColorSpecVersion version : versions) {
                if (variant == ThemeVariant.CMF && version != ColorSpecVersion.SPEC_2026) {
                    continue; // invalid combo, covered by cmfRequiresSpec2026
                }
                for (double contrast :
                        new double[] {M3Theme.CONTRAST_MEDIUM, M3Theme.CONTRAST_HIGH}) {
                    M3Theme theme =
                            M3Theme.fromSeed(SEED, variant, true, contrast, version);
                    for (ColorRole role : ColorRole.values()) {
                        assertNotNull(theme.get(role),
                                () -> variant + "/" + version + "/" + contrast + " " + role);
                    }
                }
            }
        }
    }
}
