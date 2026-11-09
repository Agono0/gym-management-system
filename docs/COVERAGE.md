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

## T3 (Expressive & Stretch Completed)

- [x] Split button (M3SplitButton: filled/tonal/outlined/elevated, XS/S/M/L, animated chevron, attached M3Menu)
- [x] Toolbar (M3Toolbar: docked and floating, horizontal/vertical, standard & vibrant styles)
- [x] Carousel (M3Carousel: multi-browse & hero layouts, drag/swipe gestures, spring snap physics)
- [x] Circular progress (M3CircularProgress: canvas arc sweep with authentic M3 indeterminate physics + determinate)
- [x] Shape morphing (M3ShapeMorph: animated polygon & corner interpolation via Canvas GraphicsContext, 0 NPEs)
- [x] Motion spring physics with velocity preservation (interruption resilience across rapid taps & retargets)
- [x] Public overlay facade (M3Overlay with isolated modal, sheet, and transient layers resolving cross-dismissal bugs)
- [x] Chips (M3Chip: assist, filter, suggestion, and closable input chips with onClose callback & trailing close icon)
- [x] Search Bar (M3SearchBar: robust suggestion filtering with preserved master dataset)
- [x] Top App Bar (M3TopAppBar: true symmetric center-aligned layout for title + nav + actions)
- [x] Bundled Material Symbols Catalog (M3Icons: 33 vector icons including navigation, settings, chevrons)
- [x] Cyber Analytics & Charts (M3SplineChart, M3BarChart, M3DonutChart, M3Sparkline, M3Heatmap, M3RadarChart, M3GaugeChart, M3ScatterPlot with OLS regression, M3FunnelChart with stage drop-off badges, M3ActivityCalendar 52-week annual punch-card heatmap)
- [x] Hardware-Accelerated 3D & Hologram System (M3TiltCard3D with 2.5D perspective shear matrix & zero BoxBounds CCE, M3Scene3D, M3ModelViewer3D with 12 commercial gym equipment models, M3AnatomyViewer3D, M3GyroRing3D)
- [x] Futuristic Visual Effects & Shaders (M3GlassPane, M3GlowShadow, M3ParticleCanvas, M3Scanline, M3Hologram, M3PulseBeacon)
- [x] Enterprise Data & Telemetry (M3DataTable, M3StatCard, M3Timeline, M3Stepper, M3SpeedDial)
- [x] Smart Gym Live Workout HUD (M3WorkoutHUD, M3RepCounter with kinetic spring bounce & cadence, M3HeartRateZones 5-zone cardiac intensity, M3IntervalTimer HIIT/Tabata countdown)
- [x] Interactive Gym Facility Floorplan (M3GymFloorMap: 2D/2.5D blueprint, zone status, equipment occupancy pins, and 1-click 3D inspector linking)
- [x] Global Spotlight Command Palette (M3CommandPalette: Ctrl+K / Cmd+K floating glassmorphic spotlight, fuzzy search, categorized action runner)

## Liquid Glass (Apple iOS 26)

The `io.m3fx.controls.glass` package per `docs/SPEC_NOTES/liquid-glass.md` (closes the
`DECISIONS.md` 72 follow-up):

- [x] `M3GlassPane` (Control + `GlassPaneSkin`): cached backdrop snapshot → Gaussian blur
      (16px regular / 4px clear) → `DisplacementMap` refraction → saturation lift (+0.18 / +0.10)
      → optional 0.25-alpha tint → specular rim (0.35, 110°) + 1px edge stroke → drop shadow →
      unblurred content on top; factories `regular()` / `clear()` / `of(params)`; full property
      pipeline (`glassEffect`, `variant`, `tint`, `shape`, `blurRadius`, `cornerRadius`,
      `glassOpacity`, `refraction`, `specular`, `specularAngle`, `saturation`,
      `maxDisplacement`, `interactive`, `shadow`); `invalidateBackdrop()` for animated backdrops
- [x] `GlassEffectParams` — immutable fluent bundle (`variant` / `tint` / `interactive` /
      `in(shape, radius)`), spec defaults (regular, untinted, non-interactive, capsule, r16)
- [x] `GlassRefraction` — pure math: convex/concave/lip height profiles, Snell n=1.5 ray
      simulation (127 rays per bezel, mirror-symmetric, normalized), `FloatMap` encoding
      (offset 0.5, x→R / y→G, `value = 0.5 + offsetPx / maxDisplacement`)
- [x] `GlassMorph` — FLUID-spring (`MotionTokens.fluidSpring()`, 0.82/6.0) pane-to-pane morph and
      `morphToBounds(Node, Bounds)`; transform-only, cancel-safe
- [x] `M3GlassEffectContainer` — spacing merge (default 40), shared union-outline bridge,
      `registerGlass` / `morphTo` (the `glassEffectID` + `@Namespace` pattern)
- [x] `GlassAdaptivity` — luminance → variant/scrim/foreground suggestions (documented
      thresholds: 0.5 midpoint, 0.25 clear band, 0.35 scrim reach, 0.55 max scrim)
- [x] Tests (FX-latch pattern): `GlassRefractionTest` (profile endpoints + monotonicity, FloatMap
      dimensions + neutral 0.5 offsets, mirror-symmetric bezel displacement,
      refraction-linear / maxDisplacement-as-external-scale encoding, radius clamping),
      `GlassEffectParamsTest` (defaults, fluent immutability, null fallbacks),
      `GlassAdaptivityTest` (documented threshold → suggestion table, scrim taper, luminance
      measurement, foreground polarity)
- [x] Gym app integration — floating chrome only (why: `DECISIONS.md` 74): splash CLEAR glass
      panel over a particle backdrop, REGULAR interactive login card, command-palette card,
      payment receipt bottom sheet, membership detail side sheet

Simplified vs Apple (honest scope):

- [ ] Merged outline is the rounded **convex hull** with an area-weighted radius — not a true
      metaball / signed-distance-field blend; no concave "neck" curves where two blobs join.
- [ ] Backdrop is a **cached snapshot** (invalidated on scene/parent/layout/size/theme change),
      not a live per-frame blur; content animating behind glass needs `invalidateBackdrop()`.
- [ ] Chromatic aberration at the rim — **deferred** (spec pipeline item; no JavaFX-native
      per-channel displacement without stacking three DisplacementMaps).
- [ ] Adaptation is single-signal (average luminance); Apple also weighs backdrop variance.
- [ ] `GLASS` / `GLASS_PROMINENT` button + card variant styling and `ToolbarStyle.GLASS` via
      `glass.css` — variant wiring lands separately from the core package.

## Gym Management System (the `com.gym` app)

The graded deliverable, built on m3fx. Current state:
`MainApp` hosts the M3 / M3 Expressive flagship gallery showcase & 19-screen interactive viewer;
`GymApp` launches the standalone application shell. The backend persistence layer (`model/`, `dao/`,
`util/`, `exception/`) is built for Milestone 2 with SQLite-JDBC (`gym.db`), automated 13-table DDL,
and binary object serialization (`SerializationUtil`). The UI controllers build their screens in
pure Java (`Screen` + `ScreenController`, commit `fb3486d`) using demo data today and are prepared for
Milestone 3 DAO wiring.

- [x] `MainApp` flagship M3 gallery (interactive showcase, seed picker, light/dark, 9 variants, contrast, spring playground, 19 Java-built screen viewer)
- [x] `GymApp` standalone desktop application launcher (`./gradlew runGym`) — theme pre-registered, maximized-before-show, `Platform.exit()`-only shutdown, daemon freeze watchdog
- [x] Launch gates (splash → glass login → morph into the shell) via `LaunchGateController`, shown when `main()` sets `SHOW_LAUNCH_GATES=true`
- [x] All 19 Java-built UI screens implemented in `src/main/java/com/gym/controller/` and registered in `Screen.java`
- [x] All 20 controllers in `src/main/java/com/gym/controller/` (`AppShellController` + 18 screen controllers implementing `ScreenController`, plus `LaunchGateController`)
- [x] Wired UI demo flows across screens (trainer list → trainer profile, customer list → customer profile, dashboard end-shift → login gate, payment record, attendance check-in)
- [x] Responsive Viewport & Side Menu Scrolling: `ScrollPane` integration for `M3NavigationRail` and `M3NavigationDrawer`
- [x] Strict compile — `-Xlint:all,-processing` enabled everywhere with no truncation (`-Xmaxwarns 10000`), UTF-8 forced, and the build is warning-clean
- [x] Backend domain model, concrete SQLite DAOs, and exceptions (`13` `Serializable` entities, `11` enums, `13` concrete DAOs with `UML.md` format/FK/uniqueness/rule validation guards, `DatabaseConnection`, `SerializationUtil`, `GymException`/`ValidationException`/`DatabaseException`) — **shipped (Milestone 2 + Decision 78)**; abstract service tier retired for beginner simplicity (`DECISIONS.md` 77)
- [x] SQLite auto-bootstrapping schema (`DatabaseConnection.initializeDatabase()` DDL creating all 13 tables) + binary serialization (`SerializationUtil.saveToFile` / `loadFromFile`) — **shipped (Milestone 2)**
- [x] Terminal UI Workflow Console (`com.gym.cli`, `runCli` / `run-cli.bat`, `DECISIONS.md` 78): SQLite Login Gate (`Rule 16` active/RBAC checks), interactive Modules `[1]`–`[10]` (Member 360, Coaches/Slots/Subs, Memberships Freeze/Unfreeze/Renew, Schedule/State Machine, Live Workout HUD, Attendance Rule 5, Payments XOR/Void/Balance, Equipment Floor Map, Staff RBAC, `.ser` Export/Load + `DatabaseException` Traps), and `[T]` TA Showcase (22/22 automated sandbox suite + category picker) — **shipped**
- [ ] Milestone 3: Connect the 20 UI controllers from demo data to the shipped SQLite DAOs and `SerializationUtil`
- Note on tests (`DECISIONS.md` 77): `src/test/` was removed from the gym app root module (`./gradlew test` on root reports `NO-SOURCE`), while the JUnit 5 suites for `:m3fx-core` and `:m3fx-controls` remain active.

### Documentation for the app

- [x] **Design spec** — `docs/UML.md`: §1 and §1b (13 domain entities and 11 enums) preserved unchanged; §2, §3, §4, §5, §6, §7, `com.gym.cli` descriptions, and business-rule enforcement updated (`DECISIONS.md` 77, 78).
- [x] **Entry-point doc** — `src/main/java/com/gym/README.md`: launcher classes (`MainApp`, `GymApp`, `M3Samples`, `cli.GymCliApp`) and Milestone 2 backend overview.
- [x] **Terminal workflow doc** — `src/main/java/com/gym/cli/README.md`: 13 pure-ASCII COMP 301 terminal classes and TA verification suite.
- [x] **Controller-layer doc** — `src/main/java/com/gym/controller/README.md`: the 20 Java-built controllers (`Screen` / `ScreenController`).
- [x] **Backend-layer & resource docs** — `model/`, `dao/`, `service/`, `util/`, `resources/db/`, `resources/fxml/` READMEs synced with Milestone 2 shipped files and `fb3486d`.
- [x] **Project docs** — `docs/README.md` (index), `docs/DOCS_STANDARD.md`, `docs/LEARNING_GUIDE.md`,
      `docs/BUILD_AND_RUN.md`, `docs/UML_TEAM.md`, root `README.md`, and `PLAN.md` updated (`TEST_READY.md` / `TEST_INFRA.md` marked archived).

Track per-owner progress in `docs/UML_TEAM.md` → "Who does what".

## Audit pass (Oct 2026) — spec alignment + bug sweep

Full-library audit (every m3fx-core token/theme file, every control package, all 19 screens) with
fixes verified against material-web tokens, material-color-utilities and m3.material.io:

- [x] Skin `dispose()` removes all registered listeners/bindings (buttons, icon buttons, chips,
      cards, progress indicators, calendar) — no more skin-replacement leaks
- [x] Ripple spec behavior: ink expands on press, fades on release (was fading while pressed)
- [x] Motion tokens: `emphasized()` = cubic-bezier(0.2,0,0,1); extra-long durations + standard
      accelerate/decelerate added; locked by `MotionTokensTest`
- [x] Linear progress: stop indicator + gaps, material-web indeterminate keyframes; circular:
      fixed 4dp stroke, MDC arc timing; wavy: Expressive tokens (3dp amplitude, 40/20dp wavelength)
- [x] Navigation controls keep one active destination; top app bar enforces variant heights;
      toolbar floating level 3
- [x] Sheets/dialogs/snackbar: exit animations, Esc-to-close, drag-to-dismiss (120px/0.6px·ms⁻¹),
      focus return on close; scrim dispatch fixed in OverlayLayer
- [x] Charts: negative/flat data safe (no NaN), spline overshoot clamped, data table sortable +
      page-local zebra striping, speed dial centered on FAB with stagger, stepper error state
- [x] Gym screens: no blank icons, no dead buttons, no empty tables, exclusive chip groups,
      dark-mode toggle reads correctly, hardcoded root widths removed, membership plans screen
      wired with telemetry funnel stages and searchable/filterable subscription ledger
- [x] Carousel (`M3Carousel`): fixed viewport clipping without bound-property mutations, dynamic
      auto-centering when items fit, hide prev/next buttons when non-overflowing, strict scroll clamping
- [x] `./gradlew test` green across m3fx-core, m3fx-controls and the app (incl. new
      MotionTokensTest, M3ButtonTest, M3SegmentedButtonTest, M3CarouselTest, expanded chart/data tests)

## Ship-readiness round (Nov 2026) — wave fixes verified

- [x] **Module exports** — `module-info.java` now exports `io.m3fx.controls.command`,
      `io.m3fx.controls.floorplan` and `io.m3fx.controls.workout` (previously used by the app while
      unexported).
- [x] **Public-API internal-type leak closed** — the library's public API no longer exposes
      `io.m3fx.controls.internal` types. The app's shell/gallery still *import* `MotionUtil` and
      `SpringAnimation` internally — the one documented exception (`DECISIONS.md` 69, controller
      README rule 25); screen controllers use public API only.
- [x] **Honest unknown-icon handling** — `M3Icon` renders a neutral ring placeholder for an unknown
      symbol name, announces `"unknown icon: <name>"` through the accessible text and logs one
      warning per distinct unknown name; it never throws and never shows a blank box.
- [x] **`MotionUtil.slideIn` is tracked** — in-flight motion cancels first, the animation is
      registered in the per-node tracker, and scene-exit cleanup resets transforms if the node
      leaves the scene mid-flight (`MotionUtil.java` ~114).
- [x] **`StateLayer` leak pattern fixed** — `unbind` removes the hover/pressed/focus listeners it
      registered and stops the running `FadeTransition`, so replaced skins stop leaking listeners.
- [x] **FAB disable propagation** — `M3FabMenu` mirrors `isDisable() || mainFab.isDisable()` onto
      the action FAB and gates its outside-click dismiss and actions on it.
- [x] **Icon-button accessible text** — `M3IconButton` sets a default accessible text so an icon
      button without an explicit label is still announced.
- [x] **M3 type-role CSS classes** — `m3fx.css` ships `.title-*` / `.body-*` / `.label-*` classes
      derived from `TypeScale`, used by the app FXMLs (≈100 uses across the screens).
- [x] **Test honesty + strict compile** — see the app section above.

## Known deliberate deviations / open items (documented, not bugs)

- [ ] Drawer width 280/320 vs token 360 (app shell calls for 220).
- [ ] Slider 16dp single track (JavaFX 21 `SliderSkin` has no separate fill node).
- [ ] M3Fonts falls back to system fonts (Roboto files not bundled yet).
- [ ] Spring token constants unverified against v34 motion tokens.
- [ ] Charts do not re-render on a theme/seed change until a data or control change repaints them.
- [ ] `.headline-small/medium/large` style classes are referenced by app FXMLs but not defined in
      `m3fx.css` (only `.title-*`/`.body-*`/`.label-*` exist) — those nodes fall back to default
      label styling.
- [ ] `module-info.java` does not yet export `io.m3fx.controls.glass` (the package compiled after
      the export wave of `DECISIONS.md` 73; classpath consumers like the gym app are unaffected,
      modular consumers need the export line added).
- [ ] Liquid-glass core shipped (see the Liquid Glass section above); the `GLASS`/`GLASS_PROMINENT`
      button + card variant styling and `ToolbarStyle.GLASS` (`glass.css`) remain the open tail of
      the `DECISIONS.md` 72 follow-up.
