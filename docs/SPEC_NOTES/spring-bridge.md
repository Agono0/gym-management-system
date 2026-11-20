# SPEC NOTES — Spring-dialect bridge (SpringToken k/ζ → SpringInterpolator ζ/ω)

Written for task A4 before coding, per the multi-agent protocol.

## Sources

- androidx compose animation-core, **`SpringSimulation.kt`** (androidx-main, fetched
  2026-11-20): https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/animation/animation-core/src/commonMain/kotlin/androidx/compose/animation/core/SpringSimulation.kt
  - "Stiffness corresponds to the spring constant."
  - The stiffness setter stores `naturalFreq = sqrt(value.toDouble())` and the getter returns
    `naturalFreq * naturalFreq` — i.e. **ω = √k with unit mass (m = 1)**.
  - The acceleration uses `k = naturalFreq * naturalFreq` and `c = 2.0 * naturalFreq * dampingRatio`
    (so ζ = c / (2·√(k·m)), matching the derivation in androidx `SpringEstimation.kt`:
    `criticalDamping = 2.0 * sqrt(springConstant * mass)`).
  - "Spring stiffness constant must be positive." (IllegalArgumentException for k ≤ 0).
  - Damping-ratio semantics: ζ > 1 over-damped (no overshoot), ζ = 1 critically damped (fastest
    settle without overshoot), ζ < 1 under-damped (overshoots and returns).
- androidx **`Spring.kt`** historically documents the same object (StiffnessMediumLow = 200,
  StiffnessMedium = 1500(?) etc. — the named stiffness constants live in the `Spring` object;
  in androidx-main the simulation classes were split into `SpringEstimation.kt` /
  `SpringSimulation.kt`). Only the parameterization (ω = √k, ζ = c/(2√(km))) is load-bearing
  here, not the named presets.

## The bridge

`SpringToken(stiffness k, dampingRatio ζ)` (Compose-parameter model, mass 1) →
`SpringInterpolator(dampingRatio ζ, naturalFrequency ω = √k)` (damped-oscillator model, unit
mass). Same conversion androidx performs internally; it is exact, not a heuristic. In normalized
time a converted spring settles quickly (raw response at t = 1 is fully settled for every grid
token because ζω ≥ 9.3), and normalization keeps `curve(0) == 0`, `curve(1) == 1`.

## Deprecation decision (checked before deprecating)

- `MotionTokens.bouncySpring()` / `SpringInterpolator.BOUNCY` and
  `MotionTokens.gentleSpring()` / `SpringInterpolator.GENTLE` have **zero callers** in the repo
  (grep over m3fx-core, m3fx-controls, app) → deprecated now, warning-free.
- `MotionTokens.spring()` / `SpringInterpolator.DEFAULT` **have live consumers** —
  `m3fx-controls/.../internal/MotionUtil.spring()` and `m3fx-controls/.../sheet/M3BottomSheet`
  — so deprecating them today would emit deprecation warnings in lane-B files A4 may not touch.
  They are documented as divergent hand-tuned values instead; deprecate once B7 reworks
  `MotionUtil` to the token grid (then `spring()`/`DEFAULT` become consumerless).
- `MotionTokens.fluidSpring()` / `SpringInterpolator.FLUID` are the Apple-language tokens
  (deliberately outside the M3 grid, see the Apple section in `MotionTokens`) with a live
  consumer (`GlassMorph`) — not deprecated, they duplicate nothing.

## [VERIFY] items

- None for the mapping (copied from the androidx source verbatim). The M3 Expressive
  `SPATIAL_*`/`EFFECTS_*` grid values themselves remain flagged unverified against the Compose
  `MotionScheme` from the pre-A4 audit (OPEN_QUESTIONS #2) — out of scope here.
