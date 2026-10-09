# AGENTS.md: m3fx (Material Design 3 for JavaFX)

You are helping build **m3fx**, an open-source Material Design 3 component library for JavaFX.
Read this whole file before every task. Also read `PLAN.md` to see the current phase.

---

## 1. Goal

A Java library that JavaFX developers add as a dependency to get:

- M3 color system (dynamic color from a seed, light/dark, contrast levels)
- M3 typography, shape, elevation, motion, and state-layer systems
- M3 components as real JavaFX controls (`Control` + `Skin` + CSS)
- M3 Expressive additions where they can be implemented in JavaFX
- A demo/gallery app showing every component in every state

Honest scope: M3 is a design spec, not a code package. We implement the spec in JavaFX.
Do not claim "100% M3". Track coverage in `docs/COVERAGE.md`.

---

## 2. Tech stack

- Java 21 (use records, sealed types, switch expressions where they help)
- JavaFX 21 LTS or newer (pin the version once in the parent POM `<properties>`)
- Maven multi-module build
- JUnit 5 for logic tests, TestFX for UI tests, `Node.snapshot()` for visual checks
- Ikonli for icon rendering (`ikonli-javafx`); Material Symbols SVG paths where Ikonli lacks an icon
- No other runtime dependencies in `m3fx-core` or `m3fx-controls` without asking me

---

## 3. Repository layout

```
m3fx/
├── AGENTS.md
├── PLAN.md
├── docs/
│   ├── COVERAGE.md        component + feature checklist, updated every PR
│   ├── SPEC_NOTES/        one .md per component: numbers extracted from the spec
│   └── DECISIONS.md       short log of design decisions and why
├── refs/                  READ-ONLY reference repos (do not edit, do not ship)
│   ├── material-color-utilities/
│   └── material-web/
├── m3fx-core/             no UI controls; pure logic + tokens
│   └── src/main/java/.../m3fx/core/
│       ├── color/         ported material-color-utilities (HCT, schemes, dynamic color)
│       ├── token/         ShapeTokens, TypeScale, ElevationLevel, MotionTokens, StateOpacity
│       └── theme/         M3Theme, ColorRole, ThemeVariant, contrast level
├── m3fx-controls/
│   └── src/main/java/.../m3fx/controls/
│       ├── button/ card/ textfield/ selection/ chip/ nav/ dialog/ ...
│       └── internal/      Ripple, StateLayer, ElevationShadow, MotionUtil
│   └── src/main/resources/.../m3fx/css/
├── m3fx-demo/             gallery app, one page per component
└── pom.xml                parent POM
```

Use a single base package, e.g. `io.github.<username>.m3fx`. Add `module-info.java`
to core and controls, and export only public API packages (never `internal`).

---

## 4. Using the reference repos

### material-color-utilities (`refs/material-color-utilities/java`)
- Copy the Java sources into `m3fx-core/.../color/`. Keep the Apache 2.0 license headers.
- Add a `NOTICE` entry crediting Google. Do not remove copyright headers.
- Do not rewrite the algorithms. Only adjust package names and fix compile issues.
- Wrap it in a clean API: `M3Theme.fromSeed(Color seed, ThemeVariant variant, boolean dark, double contrast)`.
- Expose all M3 color roles as JavaFX `Color` values, including `surface-container-lowest..highest`,
  `*-fixed`, `inverse-*`, `scrim`, `shadow`, `surface-tint`.

### material-web (`refs/material-web`)
- It is a **reference only**. Never copy its Lit/TypeScript code into the library.
- Read it to extract: dimensions, padding, corner radii, state-layer opacities, colors per state,
  font roles per component. Look at each component's styles (SCSS) and the token files.
- Write the extracted numbers to `docs/SPEC_NOTES/<component>.md` BEFORE writing Java.
- **Important:** material-web is in maintenance mode and does NOT include M3 Expressive.
  For Expressive and newer components, use the official spec at https://m3.material.io
  and the Jetpack Compose Material3 source (androidx.compose.material3, Apache 2.0)
  as secondary references. If a number cannot be found in a source, ask me; never invent values.

### Source priority when sources disagree
1. m3.material.io spec pages
2. androidx.compose.material3 source (most up to date)
3. material-web tokens (baseline M3 only, may lag)

---

## 5. Architecture rules

1. **Tokens, never literals.** No hardcoded colors, radii, font sizes, or durations in controls.
   Everything comes from `M3Theme`, `ShapeTokens`, `TypeScale`, `MotionTokens`, or `StateOpacity`.
2. **Theme through CSS looked-up colors.** `M3Theme` generates a CSS string that defines
   `-md-sys-color-primary`, `-md-sys-color-on-primary`, etc. on `.root`. Control CSS references
   those names only. Switching theme = swapping one stylesheet; all controls update.
3. **Control / Skin / CSS split.**
    - `XxxControl extends Control` (or `ButtonBase`, etc.): state and public API, properties, no visuals.
    - `XxxSkin extends SkinBase<XxxControl>`: layout and node tree.
    - `xxx.css`: visual styling.
      Prefer extending existing JavaFX controls (`Button`, `TextField`, `CheckBox`) with a custom skin
      when behavior matches. Build from `Control` only when it does not.
4. **State layers are shared.** One `StateLayer` helper handles hover (8%), focus (10%),
   pressed (10%), dragged (16%) overlays. Do not reimplement per component.
5. **Ripple is shared.** One `Ripple` helper (clipped circle + animation). Components opt in.
6. **Elevation is shared.** `ElevationLevel` (0 to 5) maps to `DropShadow` settings in one place.
7. **Motion.** Use `MotionTokens` for durations and easing. For Expressive spring motion, implement
   a small `SpringInterpolator`; do not scatter ad-hoc `Timeline`s.
8. **Accessibility.** Every control sets an accessible role/text, is keyboard operable, shows a visible
   focus indicator, and meets 48x48 minimum touch target where the spec says so.
9. **Public API style.** JavaFX property pattern (`xxxProperty()`, getter, setter). Variants via
   an enum property (e.g. `ButtonVariant.FILLED`) plus matching style class. Factory methods like
   `M3Button.filled("Save")`. Fluent where natural.
10. **Typography.** Bundle Roboto (Apache 2.0 / OFL) in controls; allow overriding the font family
    in `M3Theme`. Load fonts once via `M3Fonts.load()`.
11. **No ligature icon fonts.** JavaFX does not support Material Symbols ligatures. Use Ikonli
    or SVG `Region` shapes through an `M3Icon` wrapper.
12. **Thread safety.** All UI code on the FX thread. No static mutable state except the font loader.

---

## 6. Code quality rules

- Javadoc on every public class and method (one-liner minimum, link to spec page).
- Small classes. If a skin exceeds ~300 lines, extract helpers.
- No unused code, no commented-out blocks, no TODOs without a `COVERAGE.md` entry.
- Naming: `M3Button`, `M3Card`, `M3TextField`... prefix public controls with `M3`.
- Tests: each component gets at least (a) property/state logic tests, (b) a TestFX smoke test,
  (c) a demo page.
- Format with a single formatter config (add `spotless` or `google-java-format` in Phase 0).

---

## 7. Workflow for every task

1. State which phase and component you are working on.
2. Read relevant spec sources and write/update `docs/SPEC_NOTES/<component>.md` (numbers + source links).
3. Implement: control, skin, CSS, tokens usage.
4. Add the component to the demo app with ALL variants and ALL states (enabled, hover, focus,
   pressed, disabled, error where applicable) in both light and dark.
5. Add tests.
6. Run `mvn -q verify`. Fix failures. Run the demo (`mvn -pl m3fx-demo javafx:run`) and, if you can,
   save snapshots to `docs/snapshots/` for me to inspect.
7. Update `docs/COVERAGE.md` and `docs/DECISIONS.md` if you made a non-obvious choice.
8. Summarize in plain language: what the classes do and how they connect, so I can explain it
   in my college presentation. List anything you were unsure about.

One component per task. Do not start the next one without being asked.

---

## 8. Definition of done (per component)

- [ ] Matches spec dimensions, shapes, colors, and typography (cite the source in SPEC_NOTES)
- [ ] All variants and states implemented
- [ ] Works in light and dark and with a different seed color
- [ ] Keyboard + accessibility verified
- [ ] State layer, ripple, and elevation use shared helpers
- [ ] Demo page present
- [ ] Tests pass, build passes
- [ ] COVERAGE.md updated
- [ ] Javadoc written

---

## 9. Do not

- Do not copy Android/Flutter/web code and "translate" it line by line.
- Do not edit anything in `refs/`.
- Do not add dependencies without asking.
- Do not invent spec numbers. Ask or cite.
- Do not use `localStorage`-style hacks, reflection into JavaFX internals, or `com.sun.*` APIs
  unless unavoidable. If unavoidable, isolate it in `internal/` and document it in DECISIONS.md.
- Do not refactor unrelated code during a component task.
- Do not claim a component is done if it was not run visually.

---

## 10. Licensing

- Library license: Apache 2.0.
- Include `NOTICE` for material-color-utilities (Google, Apache 2.0).
- Bundled Roboto font: keep its license file in resources.
- Material icons/symbols: Apache 2.0; keep attribution.
