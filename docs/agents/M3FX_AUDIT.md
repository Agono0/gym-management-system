# docs/agents/M3FX_AUDIT.md — library audit (Phase 0, 2026-11-20)

Audited by 3 recon agents against the official Material 3 / M3 Expressive spec (m3.material.io).
Scope: `m3fx-core` (tokens + color engine), `m3fx-controls` (components), integration points.
Findings below are the source of QUEUE.md tasks A1–B9.

## m3fx-core (`io.m3fx.core`, ~245 KB color engine + tokens)

Solid: full material-color-utilities fork (HCT/CAM16/dynamic color, 9 scheme variants, SPEC_2021
+ 2025 + 2026 vendored), 58 `ColorRole`s published as `-md-sys-color-*` looked-up colors with
alpha variants; 15-role `TypeScale`; `ShapeTokens`; `ElevationLevel` 0–5; `StateOpacity`
(0.08/0.10/0.10/0.16/0.12/0.38 — matches material-web v34); `MotionTokens` (16 durations, spec
beziers, `SpringToken` expressive grid, plus a documented Apple-flavored set used by MotionUtil);
`SpringInterpolator` (time-normalized oscillator). No hardcoded values in core; no leaks (static
layer). Tests: 8 green (M3ThemeTest, MotionTokensTest).

Bugs / gaps → tasks:
1. **SPEC_2025/2026 + CMF unreachable**: `M3Theme` builds schemes via 3-arg `Scheme*` ctors;
   `DynamicScheme.DEFAULT_SPEC_VERSION = SPEC_2021` → ~106 KB of vendored 2025/2026 spec code is
   dead from the public API. `ThemeVariant` has no CMF entry though `Variant.CMF`/`SchemeCmf`
   exist. (A1)
2. **Mixed-spec risk**: `MaterialDynamicColors.colorSpec` is a static `ColorSpec2026` (role
   *definitions* always 2026) while tone *resolution* dispatches on `scheme.specVersion`. (A1)
3. **Misleading test comment**: `M3ThemeTest` attributes baseline `#65558f`/`#e9ddff` to "the
   bundled 2026 spec" but the path is SPEC_2021. (A1)
4. **ShapeTokens lookup gap**: `forName()` misses `none`, 32 and 48; 32 is named
   `EXPRESSIVE_LARGE_INCREASED` while the spec calls it extra-large-increased (and
   systems.md documents it inconsistently too). (A2)
5. **No reduced-motion support** anywhere in the library. (A3 core + B7 controls)
6. **Two spring dialects**: `SpringToken(stiffness, ζ)` vs `SpringInterpolator(ζ, ω)` with no
   bridge; `MotionTokens.spring()` presets ≠ SPATIAL_* tokens. (A4)
7. Hygiene: theme temp CSS files (`m3fx-theme-<sha>.css`) never deleted; `ThemeColors` caches a
   dark-baseline fallback statically; `ElevationLevel.of()` clones `values()` per call. (A5)

## m3fx-controls (`io.m3fx.controls`, 113 files ≈ 25,240 lines, ~60 components)

Solid: 10 real skins (button, icon button, chip, card, switch, linear/circular/wavy progress,
calendar, glass pane); centralized `StateLayer`/`Ripple`/`SpringAnimation`/`MotionUtil`/
`ElevationShadow` internals with FX-thread guards, self-cleaning WeakHashMaps, dispose paths;
476 token references in `m3fx.css` (zero literal hex); broad pseudo-class coverage; ESC/Enter/
Space/arrows on most interactive components; 37 test classes / 158 tests green; every component
missing from the prompt's §5.2 list actually exists (text field, switch, checkbox, radio,
linear/circular/wavy progress, nav rail/bar/drawer, adaptive scaffold, tabs, dialog, snackbar,
tooltip, badge, divider, list item, date/time picker, search bar, FAB/FAB menu, segmented button
as Expressive button group, carousel, toolbar, command palette).

Bugs / gaps → tasks:
1. **Leaks**: `SwitchSkin` adds MOUSE_CLICKED/KEY_PRESSED it never removes (M3Switch.java:177–190
   vs dispose 194–207); `LinearSkin` sceneProperty listener (M3LinearProgress.java:214 vs 339–350).
   (B1)
2. **Perf**: indeterminate timeline torn down + rebuilt on every width change
   (M3LinearProgress.java:153–158); `GlassPaneSkin` starts its physics `AnimationTimer`
   unconditionally (GlassPaneSkin.java:419) — every pane in the scene wakes every pulse;
   `M3DataTable` rebuilds all rows per search keystroke (:144–156, :238–262). (B1, B6)
3. **A11y/keyboard**: `M3Chip` Space/Enter lost when the custom skin replaced ToggleButton's
   (M3Chip.java:70–81 only handles Delete/Backspace); `M3IconButton` hardcodes accessible text
   `"Icon button"` (M3IconButton.java:69); tooltips exist (`M3Tooltip`) but nothing but
   RadarChart uses them; `M3Carousel` is mouse-only. (B2)
4. **Icons**: `M3Icons` = 37 paths; the app uses `fitness`, `pan_tool`, `play_arrow`,
   `water_drop` (+ more) that are missing → placeholder ring + stderr warning per unknown name.
   (B3)
5. **Token hygiene in controls**: ripple clip arc 40 (M3Button.java:297) vs CSS radius 20;
   hardcoded sizes in icon button (48 clip), card (24 clip), FAB (40/56/96), top app bar
   (64/112/152), switch (52×32), table rows (56/52), text field (56), icon (24/20/18) — mostly
   documented spec values but literals, not tokens, and clip-vs-CSS mismatches; `m3fx.css:573`
   `-fx-font-weight: 500` (unsupported); `.headline-*` classes missing (COVERAGE open item);
   `io.m3fx.controls.glass` not exported in module-info. (B4)
6. **Overlay/menu**: anchored overlay panels computed once from `localToScreen` — detached after
   window move/resize (OverlayLayer.java:155–176); `M3Menu.showBelow` falls back to
   `popup.show(owner, 0, 0)` screen origin (M3Menu.java:119–121); snackbar hover-pause can pause
   indefinitely (M3Snackbar.java:161–164). (B5)
7. **Charts**: canvas charts don't repaint on theme change until data/control change (COVERAGE
   open item). (B6)
8. **Test gaps**: textfield, appbar, checkbox/radio, linear/wavy progress, pickers, menu/select,
   list item, badge/divider/tooltip, overlay facade, M3Stylesheets. (B9)

## Integration (app ↔ library)

- Theme: `GymApp` seeds `#6750A4` TONAL_SPOT dark=true → `M3Stylesheets.setLatestTheme`; shell
  dark toggle re-applies. No reduced-motion, no size classes anywhere.
- `M3Icon.symbol("fitness"|"pan_tool"|"play_arrow"|"water_drop")` unknown → placeholder rings.
- `M3AdaptiveScaffold` + `WindowSizeClass` exist in the library but the app shell never uses
  them.
