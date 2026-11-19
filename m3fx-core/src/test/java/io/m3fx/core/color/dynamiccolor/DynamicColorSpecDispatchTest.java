package io.m3fx.core.color.dynamiccolor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.m3fx.core.color.hct.Hct;
import io.m3fx.core.color.scheme.SchemeCmf;
import io.m3fx.core.color.scheme.SchemeFidelity;
import io.m3fx.core.color.scheme.SchemeTonalSpot;
import org.junit.jupiter.api.Test;

/**
 * Verifies that role resolution follows the scheme's declared spec version: a SPEC_2021 scheme
 * must resolve the 2021 role definitions even though {@link MaterialDynamicColors} builds
 * superset definition chains (2021 base ⊕ 2025 ⊕ 2026 extensions), the {@code *dim} roles must
 * fall back to their nearest-newer definition for SPEC_2021 schemes, and SPEC_2025/SPEC_2026
 * schemes must engage their own definitions.
 *
 * <p>Tone pins are the values the vendored spec classes define for TonalSpot seed #6750A4
 * (e.g. the 2021 neutral-variant text tones T30/T80 documented in
 * {@code MaterialDynamicColors.controlNormal()}); see
 * {@code docs/SPEC_NOTES/color-system-versions.md}.
 */
class DynamicColorSpecDispatchTest {

    private static final int SEED = 0xFF6750A4;

    private static DynamicScheme tonalSpot(
            ColorSpec.SpecVersion specVersion, boolean isDark, double contrastLevel) {
        return new SchemeTonalSpot(
                Hct.fromInt(SEED), isDark, contrastLevel, specVersion,
                DynamicScheme.Platform.PHONE);
    }

    @Test
    void spec2021SchemeResolvesWith2021Definitions() {
        MaterialDynamicColors mdc = new MaterialDynamicColors();
        DynamicScheme light = tonalSpot(ColorSpec.SpecVersion.SPEC_2021, false, 0.0);
        DynamicScheme dark = tonalSpot(ColorSpec.SpecVersion.SPEC_2021, true, 0.0);
        // 2021 spec tones for TonalSpot; the 2025 definitions (engaged by ≥2025 schemes) solve
        // different tones for these roles, so a wrong dispatch would shift these pins.
        assertEquals(98.0, mdc.surface().getTone(light), 0.001);
        assertEquals(87.0, mdc.surfaceDim().getTone(light), 0.001);
        assertEquals(10.0, mdc.onSurface().getTone(light), 0.001);
        assertEquals(30.0, mdc.onSurfaceVariant().getTone(light), 0.001);
        assertEquals(40.0, mdc.primary().getTone(light), 0.001);
        assertEquals(6.0, mdc.surface().getTone(dark), 0.001);
        assertEquals(90.0, mdc.onSurface().getTone(dark), 0.001);
        assertEquals(80.0, mdc.onSurfaceVariant().getTone(dark), 0.001);
        assertEquals(80.0, mdc.primary().getTone(dark), 0.001);
    }

    @Test
    void dimRolesHaveNo2021DefinitionAndFallBackToNearestNewer() {
        // The 2021 spec does not define the *dim roles (ColorSpec2021 returns null); the m3fx
        // theme layer still publishes PRIMARY_DIM/SECONDARY_DIM/TERTIARY_DIM/ERROR_DIM, so for
        // SPEC_2021 schemes they resolve through the 2025 definition embedded in the superset
        // chain. See MaterialDynamicColors' Javadoc.
        ColorSpec spec2021 = ColorSpecs.get(ColorSpec.SpecVersion.SPEC_2021);
        assertNull(spec2021.primaryDim());
        assertNull(spec2021.secondaryDim());
        assertNull(spec2021.tertiaryDim());
        assertNull(spec2021.errorDim());

        MaterialDynamicColors mdc = new MaterialDynamicColors();
        DynamicScheme scheme = tonalSpot(ColorSpec.SpecVersion.SPEC_2021, false, 0.0);
        for (DynamicColor dim :
                new DynamicColor[] {
                    mdc.primaryDim(), mdc.secondaryDim(), mdc.tertiaryDim(), mdc.errorDim()
                }) {
            double tone = dim.getTone(scheme);
            assertTrue(tone >= 0.0 && tone <= 100.0, dim.name + " tone " + tone);
            assertNotNull(dim.getHct(scheme));
        }
    }

    @Test
    void spec2025SchemeEngages2025Definitions() {
        MaterialDynamicColors mdc = new MaterialDynamicColors();
        DynamicScheme light = tonalSpot(ColorSpec.SpecVersion.SPEC_2025, false, 0.0);
        DynamicScheme dark = tonalSpot(ColorSpec.SpecVersion.SPEC_2025, true, 0.0);
        // background/onBackground are remapped to surface/onSurface in the 2025 spec.
        assertEquals(mdc.surface().getArgb(light), mdc.background().getArgb(light));
        assertEquals(mdc.onSurface().getArgb(light), mdc.onBackground().getArgb(light));
        // The 2025 surface ramp darkens the dark surfaces to T4 (2021 uses T6) — same neutral
        // palette, so the tone alone discriminates the dispatch.
        assertEquals(4.0, mdc.surfaceDim().getTone(dark), 0.001);
        assertEquals(4.0, mdc.surface().getTone(dark), 0.001);
        assertEquals(87.0, mdc.surfaceDim().getTone(light), 0.001);
        // onSurfaceVariant no longer sits at the fixed 2021 T30/T80.
        DynamicScheme light2021 = tonalSpot(ColorSpec.SpecVersion.SPEC_2021, false, 0.0);
        DynamicScheme dark2021 = tonalSpot(ColorSpec.SpecVersion.SPEC_2021, true, 0.0);
        assertNotEquals(
                mdc.onSurfaceVariant().getTone(light2021), mdc.onSurfaceVariant().getTone(light));
        assertNotEquals(
                mdc.onSurfaceVariant().getTone(dark2021), mdc.onSurfaceVariant().getTone(dark));
    }

    @Test
    void spec2026CmfSchemeEngages2026Definitions() {
        MaterialDynamicColors mdc = new MaterialDynamicColors();
        DynamicScheme light =
                new SchemeCmf(
                        Hct.fromInt(SEED), false, 0.0, ColorSpec.SpecVersion.SPEC_2026,
                        DynamicScheme.Platform.PHONE);
        DynamicScheme dark =
                new SchemeCmf(
                        Hct.fromInt(SEED), true, 0.0, ColorSpec.SpecVersion.SPEC_2026,
                        DynamicScheme.Platform.PHONE);
        // 2026 CMF surface ramp: T98/T4 surfaces, T100/T0 lowest containers.
        assertEquals(98.0, mdc.surface().getTone(light), 0.001);
        assertEquals(4.0, mdc.surface().getTone(dark), 0.001);
        assertEquals(100.0, mdc.surfaceContainerLowest().getTone(light), 0.001);
        assertEquals(0.0, mdc.surfaceContainerLowest().getTone(dark), 0.001);
        // 2026 remaps primary_dim onto primary.
        assertEquals(mdc.primary().getArgb(light), mdc.primaryDim().getArgb(light));
        assertEquals(mdc.primary().getArgb(dark), mdc.primaryDim().getArgb(dark));
    }

    @Test
    void specVersionFallbackIsSilentButRecordedOnTheScheme() {
        // Requesting SPEC_2026 with TonalSpot silently resolves as SPEC_2025; the scheme keeps
        // the effective version so callers can observe what actually happened. SPEC_2025 stays
        // as requested.
        assertEquals(
                ColorSpec.SpecVersion.SPEC_2025,
                tonalSpot(ColorSpec.SpecVersion.SPEC_2026, false, 0.0).specVersion);
        assertEquals(
                ColorSpec.SpecVersion.SPEC_2025,
                tonalSpot(ColorSpec.SpecVersion.SPEC_2025, false, 0.0).specVersion);
        // Fidelity-style variants always resolve as SPEC_2021 regardless of the request.
        SchemeFidelity fidelity =
                new SchemeFidelity(
                        Hct.fromInt(SEED), false, 0.0, ColorSpec.SpecVersion.SPEC_2025,
                        DynamicScheme.Platform.PHONE);
        assertEquals(ColorSpec.SpecVersion.SPEC_2021, fidelity.specVersion);
    }
}
