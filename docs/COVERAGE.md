# COVERAGE.md — m3fx component + feature checklist

> Honest scope: M3 is a design spec, not a code package. This file tracks what is actually built.

## Systems

- [x] Color / dynamic color (9 variants, all roles, contrast levels)
- [x] Dark theme (M3Theme dark flag + demo toggle)
- [x] Contrast levels (0.0 / 0.5 / 1.0 passthrough)
- [x] Typography (15 roles, Roboto bundling hook via M3Fonts)
- [x] Shape (7 sizes + 2 Expressive)
- [x] Elevation (levels 0-5 → DropShadow + animate)
- [x] State layers (CSS layered alpha vars + shared overlay helper, focus rings everywhere)
- [x] Ripple (shared helper, wired into buttons, icon buttons and clickable cards)
- [x] Motion easing (standard/emphasized/accelerate/decelerate + spring interpolator)
- [x] Motion spring physics (SpringAnimation mass-spring-damper integrator + presets)
- [x] Icons (M3Icon glyph/SVG wrapper + bundled 26-path Material Symbols catalog, Apache 2.0)
- [x] Adaptive layout (window size classes + M3AdaptiveScaffold bar↔rail↔drawer)

## T1

- [x] Button (filled/tonal/outlined/elevated/text, icon+label, toggleable)
- [x] Icon button (standard/filled/tonal/outlined + toggle)
- [x] FAB (small/medium/large/extended + Expressive FAB menu)
- [x] Wavy progress (Expressive) + loading handled by indeterminate modes
- [x] Text field (filled/outlined, floating label, supporting text, icons, error, counter)
- [x] Checkbox (incl. indeterminate) / Radio / Switch (incl. icon thumb)
- [x] Chips (assist/filter/input/suggestion)
- [x] Divider / Badge / Tooltip (plain+rich)
- [x] Progress (linear determinate+indeterminate, circular, wavy Expressive)
- [x] Slider (continuous/discrete with stops, range)

## T2

- [x] Top app bar (small/center-aligned/medium/large)
- [x] Navigation bar / rail / drawer (standard+modal)
- [x] Tabs (primary/secondary)
- [x] Dialog (basic/fullscreen, overlay layer)
- [x] Snackbar (queue + action)
- [x] Menu / Select (dropdown)
- [x] List item (1/2/3-line, leading/trailing)
- [x] Bottom sheet / Side sheet (standard+modal)
- [x] Date picker (M3-styled input + native popup fully M3-themed + full M3DateDialog calendar grid)
- [x] Time picker (input mode + AM/PM)
- [x] Search bar (suggestions view, popup + overlay-with-scrim modes)
- [x] Segmented button (+ Expressive button-group look)

## T3 (stretch)

- [ ] Split button / toolbar / carousel
- [ ] Shape morphing

## Gym Management System (the `com.gym` app)

The graded deliverable, built on m3fx. Current state: **3 of ~85 planned classes exist.**

- [x] `MainApp` skeleton (placeholder: hardcoded title, fixed 600x400 scene, no theme call)
- [x] `fxml/main.fxml` — m3fx component showcase (no controller; testing only)
- [ ] Everything in `docs/UML.md` §1 and §3–§6 (13 entities, 10 enums, 13 DAO pairs, 11 services,
      16 controllers, 9 utilities)
- [ ] Nav shell + M3 theme wired into `MainApp` (blocked on a public m3fx overlay entry point —
      see `DECISIONS.md` 35)
- [ ] `schema.sql` + seed data

Track per-owner progress in `docs/UML_TEAM.md` → "Who does what".
