package io.m3fx.core.theme;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.m3fx.core.color.contrast.Contrast;
import io.m3fx.core.color.hct.Hct;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

/**
 * Text-contrast gate: every on-* text role must reach WCAG 4.5:1 against its background role at
 * standard contrast, in light AND dark, for the default SPEC_2021 path, the explicit SPEC_2025
 * path and the CMF (SPEC_2026) path.
 *
 * <p>Ratios are computed with the vendored {@link Contrast} engine from the HCT tones of the
 * resolved sRGB colors: {@code ratioOfTones} = {@code (Y1 + 5) / (Y2 + 5)} on the 0–100
 * luminance scale, which is exactly WCAG's {@code (L1 + 0.05) / (L2 + 0.05)}.
 *
 * <p>Background pairings follow the roles the definitions actually resolve against:
 * {@code onSurface}/{@code onSurfaceVariant} sit on the highest surface (surface-dim in light,
 * surface-bright in dark), container text on its container, and fixed roles on the fixed role
 * (fixed and fixed-dim share the same tone). Note the 2021 spec's own contrast curves put
 * several of these pairs at a 3.0 minimum (onBackground, onSurfaceVariant, the on*Container and
 * on*FixedVariant roles); the resolved colors exceed 4.5:1 for this seed anyway, so the gate
 * holds 4.5:1 unconditionally — any failure would be a real regression to report, not a
 * threshold to tune.
 */
class M3ThemeContrastGateTest {

    private static final Color SEED = Color.web("#6750A4");

    /** Text role paired with the background role it is actually resolved against. */
    private record Gate(ColorRole text, ColorRole background) {}

    @Test
    void allTextRolesMeetWcag45AtStandardContrast() {
        for (boolean dark : new boolean[] {false, true}) {
            // Backgrounds that follow the highest-surface convention.
            ColorRole highestSurface = dark ? ColorRole.SURFACE_BRIGHT : ColorRole.SURFACE_DIM;
            Gate[] gates = {
                new Gate(ColorRole.ON_SURFACE, highestSurface),
                new Gate(ColorRole.ON_SURFACE_VARIANT, highestSurface),
                new Gate(ColorRole.ON_BACKGROUND, ColorRole.BACKGROUND),
                new Gate(ColorRole.ON_PRIMARY, ColorRole.PRIMARY),
                new Gate(ColorRole.ON_PRIMARY_CONTAINER, ColorRole.PRIMARY_CONTAINER),
                new Gate(ColorRole.ON_SECONDARY, ColorRole.SECONDARY),
                new Gate(ColorRole.ON_SECONDARY_CONTAINER, ColorRole.SECONDARY_CONTAINER),
                new Gate(ColorRole.ON_TERTIARY, ColorRole.TERTIARY),
                new Gate(ColorRole.ON_TERTIARY_CONTAINER, ColorRole.TERTIARY_CONTAINER),
                new Gate(ColorRole.ON_ERROR, ColorRole.ERROR),
                new Gate(ColorRole.ON_ERROR_CONTAINER, ColorRole.ERROR_CONTAINER),
                new Gate(ColorRole.INVERSE_ON_SURFACE, ColorRole.INVERSE_SURFACE),
                new Gate(ColorRole.ON_PRIMARY_FIXED, ColorRole.PRIMARY_FIXED),
                new Gate(ColorRole.ON_PRIMARY_FIXED_VARIANT, ColorRole.PRIMARY_FIXED),
                new Gate(ColorRole.ON_SECONDARY_FIXED, ColorRole.SECONDARY_FIXED),
                new Gate(ColorRole.ON_SECONDARY_FIXED_VARIANT, ColorRole.SECONDARY_FIXED),
                new Gate(ColorRole.ON_TERTIARY_FIXED, ColorRole.TERTIARY_FIXED),
                new Gate(ColorRole.ON_TERTIARY_FIXED_VARIANT, ColorRole.TERTIARY_FIXED),
            };
            M3Theme[] themes = {
                M3Theme.fromSeed(SEED, ThemeVariant.TONAL_SPOT, dark),
                M3Theme.fromSeed(SEED, ThemeVariant.TONAL_SPOT, dark, M3Theme.CONTRAST_STANDARD,
                        ColorSpecVersion.SPEC_2025),
                M3Theme.fromSeed(SEED, ThemeVariant.CMF, dark, M3Theme.CONTRAST_STANDARD,
                        ColorSpecVersion.SPEC_2026),
            };
            String[] labels = {"2021", "2025", "CMF-2026"};
            for (int i = 0; i < themes.length; i++) {
                final M3Theme theme = themes[i];
                final String label = labels[i];
                final String mode = dark ? "dark" : "light";
                for (Gate gate : gates) {
                    double ratio =
                            Contrast.ratioOfTones(
                                    Hct.fromInt(M3Theme.toArgb(theme.get(gate.text()))).getTone(),
                                    Hct.fromInt(M3Theme.toArgb(theme.get(gate.background())))
                                            .getTone());
                    assertTrue(
                            ratio >= 4.5,
                            () -> String.format(
                                    "%s %s %s on %s = %.3f:1 (seed #6750A4, standard contrast)",
                                    label, mode, gate.text(), gate.background(), ratio));
                }
            }
        }
    }
}
