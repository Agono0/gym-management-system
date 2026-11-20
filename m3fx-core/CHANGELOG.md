# m3fx-core changelog

Release notes for the token/theme layer. `m3fx-controls` shares this file's version line:
the two modules release together and 0.1.1 of controls depends on 0.1.1 of core.

## 0.1.1 — 2026-11-21

First tagged core release. Everything below landed after the initial 0.1.0 snapshot.

### Added

- **Color-spec versions and the CMF variant are reachable through the theme API.** `M3Theme`
  exposes the vendored `SPEC_2021` / `SPEC_2025` / `SPEC_2026` color-spec versions and the
  `CMF` variant; `MaterialDynamicColors` role definitions now follow the scheme's spec version
  instead of a pinned 2026 spec. The default stays `SPEC_2021`, so rendered colors do not
  shift for existing callers.
- **`ShapeTokens` covers the full M3 shape scale**: `none`, `extra-small` (4), `small` (8),
  `medium` (12), `large` (16), `large-increased` (20), `extra-large` (28),
  `extra-large-increased` (32) and `extra-extra-large` (48), with the scale documented in the
  class Javadoc. `EXPRESSIVE_LARGE_INCREASED` remains as a deprecated alias.
- **Reduced-motion support**: `MotionTokens.reducedMotionProperty()` is the global
  accessibility flag, initialized from the `m3fx.reducedMotion` system property; reduced
  motion makes springs snap and fades stay at or under 150 ms (effects only).
- **Motion scheme switch**: `MotionScheme.EXPRESSIVE` (default, bouncing
  `SPATIAL_EXPRESSIVE_*` presets) vs `MotionScheme.STANDARD` (settling
  `SPATIAL_STANDARD_*`), initialized from the `m3fx.motionScheme` system property.
- **`SpringToken.toInterpolator()`** bridges the two spring dialects (`SpringToken` to a
  `SpringInterpolator` with the ω = √k mapping, cited from androidx).
- **`io.m3fx.core.token.OvershootSpline`** — cubic-bezier easing with web `cubic-bezier()`
  semantics (x control points in [0,1], y free to overshoot), promoted from the controls
  module where it fixed the linear-progress indeterminate animation.
- Test coverage for the token layer: ShapeTokens lookup, StateOpacity values, ElevationLevel
  mapping, TypeScale completeness and SpringInterpolator endpoints.

### Deprecated

- `MotionTokens.spring(...)` presets and `SpringInterpolator.DEFAULT` — the divergent legacy
  dialects; use `SpringToken.toInterpolator()` and the motion-scheme presets instead.
