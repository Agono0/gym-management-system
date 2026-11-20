# SPEC NOTES — Reduced motion (token layer)

Written for task A3 (reduced-motion support in `MotionTokens`) before coding, per the
multi-agent protocol.

## Sources

- WCAG 2.2, **2.3.3 Animation from Interactions** (Level AAA):
  https://www.w3.org/TR/WCAG22/#animation-from-interactions — animation triggered by interaction
  can be disabled, unless essential to the functionality or the information being conveyed.
- The **`prefers-reduced-motion`** media query (CSS Media Queries Level 5; MDN:
  https://developer.mozilla.org/en-US/docs/Web/CSS/@media/prefers-reduced-motion) — the standard
  plumbing through which web apps learn the OS animation preference. This is the model the flag
  mirrors: the OS owns the preference, the UI library reacts to it.
- Material 3, **Applying transitions**:
  https://m3.material.io/styles/motion/transitions/applying-transitions — "most platforms have a
  reduced animation setting to help users with a sensitivity to motion"; apps should honor it so
  users are not exposed to large or excessive movement. (JS-rendered page; guidance located via
  web search 2026-11-20, quote from search-result excerpt of the page.)
- Windows OS preference: `SPI_GETCLIENTAREAANIMATION` (SystemParametersInfo) exposes the
  "Show animations in Windows" setting. Pure Java cannot read it without a native bridge (JNA);
  a new dependency is forbidden by repo policy — see `docs/agents/OPEN_QUESTIONS.md` #7.

## A3 design (documented contract, consumption lands in lane-B task B7)

- **Flag**: `MotionTokens.reducedMotionProperty()` (read-only JavaFX boolean property) with
  `isReducedMotion()` / `setReducedMotion(boolean)`. JavaFX property listeners are the minimal
  observable mechanism consistent with the library (skins already speak JavaFX observables).
- **Initialization**: from the `m3fx.reducedMotion` system property
  (`-Dm3fx.reducedMotion=true`, case-insensitive; absent or any other value → `false` — same
  semantics as `Boolean.getBoolean`). Apps own the setting UI and call `setReducedMotion`.
- **Threading contract**: like the rest of the library's mutable state, the flag is consumed on
  the JavaFX Application Thread; property listeners fire synchronously on the thread that calls
  `setReducedMotion`, so UI mutations in listeners require the FX thread. Not enforced — the
  core module has no thread-guard utility (guards live in controls internals, lane B).
- **Contract when enabled** (for B7):
  1. springs snap to their target instead of interpolating;
  2. spatial/transform motion is replaced with opacity fades capped at
     `MotionTokens.DURATION_REDUCED_FADE` (150 ms);
  3. instant feedback (state layers, focus rings) is unchanged.

## B7 — consumption in the controls internals (implemented)

The A3 contract is now consumed by the controls layer:

- `SpringAnimation.animate` snaps synchronously (no timer starts) while reduced motion is on,
  and a static listener on `MotionTokens.reducedMotionProperty()` snaps every running spring to
  its target when the flag flips on (completion callbacks fire so chained skin state stays
  consistent).
- `MotionUtil.enter/exit/splashIn/splashOut/slideIn` degrade to opacity-only fades capped at
  `DURATION_REDUCED_FADE` (150 ms, no transforms); `shake` becomes a 150 ms opacity pulse (no
  displacement); `morphFromBounds` takes its no-transform path; press-scale handlers check the
  flag per event and restore the resting scale. The flag is read at invocation time — calls made
  after the flip honor it immediately; fades already in flight end at rest states and are left to
  finish.
- `StateLayer.refresh` settles the overlay opacity directly (no fade) under reduced motion —
  state feedback stays instant.

## B7 — motion scheme mapping (expressive ↔ standard)

`MotionTokens.MotionScheme` (enum + read-only property + `setMotionScheme`, initialized from
`m3fx.motionScheme=expressive|standard`, default expressive — same pattern as the reduced-motion
flag). `SpringAnimation.resolveForScheme` maps the named spatial presets under STANDARD; effects
(`EFFECTS_*`), the Apple-flavored set and custom tunings pass through unchanged:

| Preset (request) | EXPRESSIVE | STANDARD |
|---|---|---|
| `DEFAULT` | `SPATIAL_EXPRESSIVE_DEFAULT` (380, 0.65) | `SPATIAL_STANDARD_DEFAULT` (350, 0.90) |
| `BOUNCY` | `SPATIAL_EXPRESSIVE_BOUNCY` (500, 0.45) | `SPATIAL_STANDARD_DEFAULT` (350, 0.90) — standard language has no bounce |
| `FAST` | `SPATIAL_EXPRESSIVE_FAST` (800, 0.70) | `SPATIAL_STANDARD_FAST` (700, 0.95) |
| `SLOW` | `SPATIAL_EXPRESSIVE_SLOW` (200, 0.60) | `SPATIAL_STANDARD_SLOW` (200, 0.90) |
| `GENTLE` | `EFFECTS_DEFAULT` (300, 1.00) | unchanged |

`MotionUtil.spring()` builds its interpolator through the A4 bridge
(`SPATIAL_EXPRESSIVE_DEFAULT`/`SPATIAL_STANDARD_DEFAULT` `toInterpolator()`), so switching the
scheme changes the feel — the bridged Expressive default (ω ≈ 19.5) settles noticeably faster in
normalized time than the pre-B7 hand-tuned spring (ω = 7.5); the gallery demo (C10) makes that
visible.

## [VERIFY] items

- **The 150 ms fade cap is a library convention, not a verbatim spec number**: it matches the M3
  `DURATION_SHORT_3` token (150 ms, the "small expands" step) and common reduced-motion
  implementations (web `prefers-reduced-motion` patterns typically swap ~200–300 ms transitions
  for ~0–150 ms fades), but no official M3 page names a reduced-motion fade duration. Tagged
  `// [VERIFY]` on the constant; orchestrator should add an OPEN_QUESTIONS entry if it wants the
  number ratified.
