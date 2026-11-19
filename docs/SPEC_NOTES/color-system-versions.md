# SPEC NOTES — Color system spec versions (2021 / 2025 / 2026) and the CMF variant

Written for task A1 (theme API alignment) before coding, per the multi-agent protocol.
Scope: how `m3fx-core` picks a dynamic-color spec version, what actually changes between the
three vendored specs, which variant × spec combinations are meaningful, and provenance of the
2026/CMF code.

## Sources

- m3.material.io — "Color system overview" (https://m3.material.io/styles/color/system/overview)
  and "Color roles" (https://m3.material.io/styles/color/roles). Both pages are JS-rendered
  (Angular); on 2026-11-20 only headings/meta could be extracted, so they are cited for the
  conceptual model (seed → variant → tone roles → contrast levels) only.
- material-color-utilities (Google, Apache-2.0), upstream `main` raw files, fetched and compared
  on 2026-11-20:
  - `java/dynamiccolor/ColorSpec2026.java` — https://raw.githubusercontent.com/material-foundation/material-color-utilities/main/java/dynamiccolor/ColorSpec2026.java
  - `java/dynamiccolor/MaterialDynamicColors.java` — …/MaterialDynamicColors.java
  - `java/dynamiccolor/Variant.java`, `java/dynamiccolor/DynamicScheme.java`, `java/dynamiccolor/DynamicColor.java`,
    `java/scheme/SchemeCmf.java` (same base URL)
- The vendored fork in `m3fx-core/src/main/java/io/m3fx/core/color/**` (package-renamed copy of the
  above; behavior-identical).

The implementation is the most precise public definition of the three spec versions (the m3
site documents outcomes, not algorithms), so tone/curve values below are quoted from the code.

## How a spec version is chosen

- `DynamicScheme.specVersion` defaults to `SPEC_2021` (`DEFAULT_SPEC_VERSION`); the 3-arg
  `Scheme*` constructors use it, so every pre-A1 m3fx theme was a SPEC_2021 scheme.
- Role *definitions* carry per-spec variants: each `ColorSpec2025`/`ColorSpec2026` override
  returns `super.role().toBuilder().extendSpecVersion(SPEC_n, color202n).build()`, i.e. one
  `DynamicColor` embedding the 2021 base ⊕ 2025 ⊕ 2026 definitions. `MaterialDynamicColors`
  (upstream and fork) pins `colorSpec = new ColorSpec2026()` — the superset factory. This is
  upstream-parity behavior, **not** a local bug (verified against upstream `main` 2026-11-20).
- Role *resolution* dispatches on the scheme's declared version:
  `DynamicColor.getHct(scheme)` → `ColorSpecs.get(scheme.specVersion).getHct(scheme, color)`,
  and every embedded definition selects its variant via
  `s.specVersion.compareTo(specVersion) >= 0` (a SPEC_2021 scheme therefore uses the 2021 base
  definitions; a SPEC_2025 scheme the ≥2025 extensions; SPEC_2026 the ≥2026 extensions).
- Palette *construction* uses `ColorSpecs.get(specVersion).getXxxPalette(...)` at scheme build
  time (in each `Scheme*` constructor).

## What changes 2021 → 2025 (from ColorSpec2025)

- `background`/`onBackground` are remapped to `surface`/`onSurface`.
- Surfaces become an explicit tone ramp on the neutral palette (phone): dark
  `4 / 0 / 6 / 9 / 12 / 15` for surface / lowest / low / container / high / highest; light
  `98 / 100 / 96 / 94 / 92 / 90` (light phone `surface` is 99 for "yellow" hues, 97 for
  VIBRANT, else 98 — hue-dependent, per code).
- Default contrast curves are raised across the board (standard-contrast value first):
  - `onSurface`: 2021 `4.5` → 2025 `9` light / `11` dark (phone).
  - `onPrimary`/`onSecondary`/`onTertiary`/`onError` and the four `on*Container` roles:
    2021 `4.5` / `3.0` → 2025 `6.0`.
  - `onSurfaceVariant`: 2021 `3.0` → 2025 `4.5` light / `6.0` dark (phone).
- The four `*Dim` roles (`primary_dim`, `secondary_dim`, `tertiary_dim`, `error_dim`) get real
  definitions in 2025; `ColorSpec2021` returns `null` for them. Consequence documented in
  `MaterialDynamicColors`: for SPEC_2021 schemes these roles resolve through their nearest-newer
  (2025) definition embedded in the superset chain — m3fx keeps this because its theme layer
  publishes `PRIMARY_DIM`/`SECONDARY_DIM`/`TERTIARY_DIM`/`ERROR_DIM` as extra roles beyond the
  standard 2021 set (back-compat, byte-identical output requirement of task A1).
- Platform branches appear (`PHONE` vs `WATCH`) for several tones and curves.

## What changes 2025 → 2026 (from ColorSpec2026)

- 2026 role definitions are written for the **CMF variant only** — every surface role and most
  chroma multipliers branch `if (s.variant == Variant.CMF) { … } else { /* Undefined use case
  */ return 0.0; }`. This is why `DynamicScheme.maybeFallbackSpecVersion` never lets a non-CMF
  variant keep SPEC_2026.
- Surface ramp gains chroma multipliers (CMF): dark `1.0/1.25/1.4/1.5/1.7`, light
  `1.7` on surface-dim/bright etc.; text/icon roles get `1.7` chroma multiplier.
- `primary` (CMF): tone = source tone (chroma ≤ 12 → dark 80 / light 40), background =
  highest surface, contrast curve 4.5, tone-delta pair with `primaryContainer` (Δ5).
  `onPrimary`/`on*Container` curves stay 6.0. Container tones clamp the source tone into
  fixed bands (`61–90` / `30–49` etc.).
- `primaryDim`/`secondaryDim`/`tertiaryDim`/`errorDim` remap to their base roles (2026).
- Fixed roles compute their tone from the light-scheme container tone
  (`DynamicScheme.from(s, false, 0.0)` snapshot), tone-delta 5 between fixed-dim and fixed.

## Variant × spec compatibility (maybeFallbackSpecVersion)

`DynamicScheme.maybeFallbackSpecVersion(specVersion, variant)` silently rewrites the requested
version (kept as-is per task A1; documented here and in Javadoc):

| Requested | TONAL_SPOT / VIBRANT / EXPRESSIVE / NEUTRAL | FIDELITY / CONTENT / RAINBOW / FRUIT_SALAD / MONOCHROME | CMF |
|---|---|---|---|
| SPEC_2021 | SPEC_2021 | SPEC_2021 | SPEC_2021¹ |
| SPEC_2025 | SPEC_2025 | SPEC_2021 | SPEC_2025¹ |
| SPEC_2026 | SPEC_2025 (fallback) | SPEC_2021 (fallback) | SPEC_2026 |

¹ `SchemeCmf` throws `IllegalArgumentException` unless the effective version is SPEC_2026, so
**CMF is only meaningful with SPEC_2026**. Meaningful combos exposed by the m3fx theme API:
all 9 classic variants with SPEC_2021 or SPEC_2025; TONAL_SPOT/VIBRANT/EXPRESSIVE/NEUTRAL get
SPEC_2026 only by falling back to SPEC_2025; CMF only with SPEC_2026. Because CMF has no 2021
resolution at all, `M3Theme`'s overloads without an explicit spec version default CMF to
SPEC_2026 (instead of throwing), while explicit non-2026 requests still throw.

Drift note (upstream `main`, 2026-11-20): upstream's `maybeFallbackSpecVersion` gained a third
parameter and keeps `SPEC_2025` for `WATCH` + `CONTENT`. The vendored snapshot predates that
arm; it is unreachable through the m3fx theme API (all schemes are constructed with
`Platform.PHONE`). Left untouched to keep A1 output-neutral; align on the next vendor refresh.

## Provenance correction (supersedes OPEN_QUESTIONS item 6)

`ColorSpec2026.java`, `Variant.CMF` and `SchemeCmf.java` are **not** locally authored: the
vendored copies are byte-identical to upstream `material-color-utilities` `main` modulo
mechanical package renames and `androidx.annotation` stripping (verified by diff on 2026-11-20;
files listed under Sources). Their "Copyright 2026 Google LLC" headers are genuine upstream
headers and were left untouched. The claim in `docs/agents/OPEN_QUESTIONS.md` item 6 ("upstream
mcu only has 2021+2025") is outdated — upstream main now carries the 2026 spec + CMF.

## Contrast measurement basis used by the gate

`Contrast.ratioOfTones(t1, t2)` = `(Y1 + 5) / (Y2 + 5)` with `Y` = XYZ relative luminance on the
0–100 scale, i.e. exactly WCAG's `(L1 + 0.05) / (L2 + 0.05)` — so the mcu ratio *is* the WCAG
ratio when tones are the true HCT tones of the resolved sRGB colors. Designed standard-contrast
minimums (2021 spec): `onSurface`, `onPrimary`, `onSecondary`, `onTertiary`, `onError`,
`on*Fixed`, `inverseOnSurface` = **4.5**; `onBackground`, `onSurfaceVariant`, `on*Container`,
`on*FixedVariant` = **3.0** (raised to ≥4.5/6.0 in 2025+). The A1 text-contrast gate asserts
4.5 for the 4.5-by-design pairs and the declared curve minimum for the 3.0-by-design pairs
(any failure below the *declared* minimum is an engine bug and gets reported).

## [VERIFY] items

- Exact light-surface tone adjustments for "yellow" hues (`Hct.isYellow`) and WATCH branches:
  transcribed from vendored code, not cross-checked against published documentation.
- The upstream date at which the 2026 spec/CMF landed on `main` was not investigated (irrelevant
  to A1; content identity is what matters).
