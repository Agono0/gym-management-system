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

## [VERIFY] items

- **The 150 ms fade cap is a library convention, not a verbatim spec number**: it matches the M3
  `DURATION_SHORT_3` token (150 ms, the "small expands" step) and common reduced-motion
  implementations (web `prefers-reduced-motion` patterns typically swap ~200–300 ms transitions
  for ~0–150 ms fades), but no official M3 page names a reduced-motion fade duration. Tagged
  `// [VERIFY]` on the constant; orchestrator should add an OPEN_QUESTIONS entry if it wants the
  number ratified.
