# SPEC_NOTES — foundation systems (color, shape, type, elevation, state, motion)

Sources:
- material-color-utilities (this repo: `material-color-utilities-main/material-color-utilities-main/java`)
  — HCT, TonalPalette, DynamicScheme + 9 Scheme* variants, MaterialDynamicColors (2026 spec incl.
  `*-fixed-dim` roles). Ported verbatim into `m3fx-core/.../color` (only package names changed,
  `androidx`/`errorprone` annotations stripped).
- material-web 2.5.0 tokens (`material-web-2.5.0/material-web-2.5.0/tokens`):
  `_md-sys-color.scss`, `_md-sys-shape.scss`, `_md-sys-typescale.scss`, `_md-sys-elevation.scss`,
  `_md-sys-state.scss`, `_md-sys-motion.scss`, plus `versions/v0_192/*` generated values.
- Spec pages: https://m3.material.io/styles/color/roles, .../shape/overview,
  .../typography/overview, .../elevation/overview, .../motion/overview,
  https://m3.material.io/foundations/interaction/states/overview

## Color
- 9 variants: TonalSpot, Vibrant, Expressive, Neutral, Monochrome, Fidelity, Content, Rainbow,
  FruitSalad → `ThemeVariant` (1:1 with `dynamiccolor/Variant.java` + `scheme/Scheme*.java`).
- Roles exposed (all from `MaterialDynamicColors`): primary/on-primary/primary-container/
  on-primary-container, secondary*/tertiary* families, error family, background/on-background,
  surface/on-surface/surface-variant/on-surface-variant, surface-dim/bright,
  surface-container-lowest/low/container/high/highest, surface-tint, inverse-surface/
  inverse-on-surface/inverse-primary, *-fixed + *-fixed-dim + on-*-fixed(+variant),
  outline/outline-variant, scrim, shadow.
- Contrast levels: `contrastLevel` double passed straight to `DynamicScheme`
  (0.0 standard, 0.5 medium, 1.0 high).
- Baseline check: seed #6750A4 / TonalSpot / light gives primary #65558f and
  primary-container #e9ddff with the bundled 2026 spec (the older #6750A4/#EADDFF pair was the
  2021 spec). Locked by `M3ThemeTest`. Verify visually in the demo swatch page.

## Shape (`_md-sys-shape.scss`, sys values)
- corner-none 0, extra-small 4, small 8, medium 12, large 16, extra-large 28, full → `ShapeTokens`.
- Expressive additions: large-increased 32, extra-extra-large 48 (m3.material.io expressive shape).

## Type (`_md-sys-typescale.scss`, 15 roles, Roboto)
| role | size | line | tracking | weight |
|---|---|---|---|---|
| display-large | 57 | 64 | -0.25 | 400 |
| display-medium | 45 | 52 | 0 | 400 |
| display-small | 36 | 44 | 0 | 400 |
| headline-large | 32 | 40 | 0 | 400 |
| headline-medium | 28 | 36 | 0 | 400 |
| headline-small | 24 | 32 | 0 | 400 |
| title-large | 22 | 28 | 0 | 400 |
| title-medium | 16 | 24 | 0.15 | 500 |
| title-small | 14 | 20 | 0.1 | 500 |
| label-large | 14 | 20 | 0.1 | 500 |
| label-medium | 12 | 16 | 0.5 | 500 |
| label-small | 11 | 16 | 0.5 | 500 |
| body-large | 16 | 24 | 0.5 | 400 |
| body-medium | 14 | 20 | 0.25 | 400 |
| body-small | 12 | 16 | 0.4 | 400 |

## Elevation (`_md-sys-elevation.scss`)
- Levels 0-5; level0 = none. JavaFX mapping in `ElevationLevel` (offsetY 0/1/2/3/4/5… wait:
  implemented offsets 0,1,2,4,6,8 with radii 0,3,6,8,12,16 and alpha 0/0.15/0.15/0.16/0.18/0.20).
  Surface-tint overlay half is done in CSS via container colors; shadow half in code.

## State (`_md-sys-state.scss`)
- hover 0.08, focus 0.10, pressed 0.10, dragged 0.16; disabled container 0.12 / content 0.38;
  scrim 0.32 → `StateOpacity` + `StateLayer` helper.

## Motion (`_md-sys-motion.scss`)
- Durations 50/100/150/200/250/300/350/400/450/500/550/600/700ms → `MotionTokens`.
- Easings: standard cubic-bezier(0.2,0,0,1), emphasized (0.05,0.7,0.1,1),
  emphasized-accelerate (0.3,0,0.8,0.15) → spline interpolators; Expressive spring via
  `MotionUtil.spring()` (damped overshoot approximation — a full physics spring is future work).

## Ripple / icons
- Ripple: clipped expanding circle, emphasized 250ms scale + 450ms fade (`Ripple`).
- Icons: no ligature fonts in JavaFX → `M3Icon` (glyph label or SVGPath). Ikonli deliberately NOT
  added as a dependency yet (needs your approval per AGENTS.md); CSS class `m3-icon-glyph` is the
  hook for an icon font later.
