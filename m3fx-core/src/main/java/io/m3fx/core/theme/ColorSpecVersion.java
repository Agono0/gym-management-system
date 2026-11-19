package io.m3fx.core.theme;

/**
 * Dynamic-color spec version the theme engine resolves roles with.
 *
 * <p>The vendored {@code material-color-utilities} engine ships three color-spec generations
 * ({@code ColorSpec2021}, {@code ColorSpec2025}, {@code ColorSpec2026}). The default is
 * {@link #SPEC_2021}, so existing callers keep today's rendered colors; requesting a newer
 * version re-resolves every {@link ColorRole} with that spec's tone and contrast rules.
 *
 * <p>Not every variant supports every version — {@code DynamicScheme} falls back silently
 * (see the table below, mirrored from
 * {@code DynamicScheme.maybeFallbackSpecVersion}), and the CMF variant
 * ({@link ThemeVariant#CMF}) rejects anything but {@link #SPEC_2026} outright:
 *
 * <table>
 *   <caption>Requested spec version per variant (effective version after fallback)</caption>
 *   <tr><th>Requested</th><th>TONAL_SPOT, VIBRANT, EXPRESSIVE, NEUTRAL</th>
 *       <th>FIDELITY, CONTENT, RAINBOW, FRUIT_SALAD, MONOCHROME</th><th>CMF</th></tr>
 *   <tr><td>SPEC_2021</td><td>SPEC_2021</td><td>SPEC_2021</td><td>rejected</td></tr>
 *   <tr><td>SPEC_2025</td><td>SPEC_2025</td><td>SPEC_2021</td><td>rejected</td></tr>
 *   <tr><td>SPEC_2026</td><td>SPEC_2025 (fallback)</td><td>SPEC_2021 (fallback)</td>
 *       <td>SPEC_2026</td></tr>
 * </table>
 *
 * <p>CMF has no 2021 resolution at all, so the overloads without an explicit spec version
 * default it to {@link #SPEC_2026} instead of throwing.
 *
 * <p>See {@code docs/SPEC_NOTES/color-system-versions.md} for what each generation changes.
 */
public enum ColorSpecVersion {
    /**
     * Original dynamic-color spec (Material 3 launch): the default for the classic variants;
     * the CMF variant defaults to SPEC_2026 instead.
     */
    SPEC_2021,
    /**
     * 2025 dynamic-color spec: remapped surfaces/background, higher default contrast curves,
     * defined {@code *dim} roles. Valid for TONAL_SPOT, VIBRANT, EXPRESSIVE and NEUTRAL; the
     * other classic variants fall back to SPEC_2021.
     */
    SPEC_2025,
    /**
     * 2026 dynamic-color spec: definitions written for the Color Midpoint Fusion variant
     * ({@link ThemeVariant#CMF}). Other variants never resolve with it — TONAL_SPOT, VIBRANT,
     * EXPRESSIVE and NEUTRAL fall back to SPEC_2025, the rest to SPEC_2021.
     */
    SPEC_2026
}
