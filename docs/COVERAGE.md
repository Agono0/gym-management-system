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

## Gym Management System (the `com.gym` app)

The graded deliverable, built on m3fx. Current state: **`MainApp` hosts the comprehensive M3 / M3 Expressive flagship gallery showcase & 19-screen FXML Porting Studio; `GymApp` launches the standalone application shell.**

- [x] `MainApp` flagship M3 gallery (interactive 17-page showcase, seed picker, light/dark, 9 variants, contrast, spring playground, 19 FXML screen studio viewer)
- [x] `GymApp` standalone desktop application launcher for `app-shell.fxml` (`./gradlew runGym`)
- [x] All 19 declarative FXML UI screens implemented in `src/main/resources/fxml/` (`login.fxml`, `app-shell.fxml`, `dashboard.fxml`, `customer-list.fxml`, `customer-profile.fxml`, `medical-profile.fxml`, `progress.fxml`, `trainer-list.fxml`, `trainer-profile.fxml`, `availability.fxml`, `workout.fxml`, `live-workout-hud.fxml`, `schedule.fxml`, `equipment.fxml`, `gym-floor-map.fxml`, `attendance.fxml`, `membership.fxml`, `payment.fxml`, `staff.fxml`)
- [x] All 19 controller UI skeletons in `src/main/java/com/gym/controller/` with 570 `@FXML` bindings
- [x] Responsive Viewport & Side Menu Scrolling: ScrollPane integration for M3NavigationRail and M3NavigationDrawer; nested scroll conflict elimination and computed height bounds across all 19 FXML screens
- [x] Automated FXML integrity, reflection binding, and stress test suites in `src/test/java/com/gym/fxml/`
- [x] `fxml/main.fxml` — m3fx component showcase (FXML declaration testing)
- [ ] Backend domain services and SQLite DAOs (13 entities, 11 enums, 13 DAO pairs, 11 services, 19 business rules)
- [ ] `schema.sql` + seed data

### Documentation for the app (complete — this is what the code is written from)

- [x] **Design spec** — `docs/UML.md`: 13 entities, 11 enums, 13 DAO interfaces + 13 impls, 11 services,
      16 controllers, 10 utilities, plus the 19 business rules every owner implements identically.
- [x] **Per-layer learning docs** (~15 500 lines) — `src/main/java/com/gym/README.md` (entry point) and
      `model/`, `dao/`, `service/`, `controller/`, `util/` READMEs. Each has the same nine sections:
      purpose, the OOP concepts practised there, design patterns, rules with the bug each prevents,
      **one entry per file** (why it is shaped that way, how to write it step by step, a code sketch, how
      to verify it, ≥3 common mistakes), how the layer connects, mistakes + review checklist, exercises
      with answers, ownership + definition of done.
- [x] **Per-file coverage verified** — every planned `.java` file, every screen FXML and every table has
      its own entry (121/121 checked against `docs/UML.md`).
- [x] **Resource docs** — `src/main/resources/README.md` plus `fxml/`, `db/` (13 tables, constraints,
      indexes, seed), `theme/` and `images/`.
- [x] **Project docs** — `docs/README.md` (index), `docs/DOCS_STANDARD.md` (how the folder docs are
      written and which document wins), `docs/LEARNING_GUIDE.md` (study path, OOP concept index, glossary,
      40 defence questions), `docs/BUILD_AND_RUN.md` (build files line by line, commands, troubleshooting).
- [x] **141 "Open detail (decide + log in `DECISIONS.md`)" markers** — every genuinely undecided
      implementation point is listed with its options and trade-offs instead of being invented. Settling
      one is a `docs/DECISIONS.md` entry, and the list is the team's first backlog after Week 1.
- [ ] Each owner settles the open details in their folder while writing that folder's code.

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
- [ ] Known deliberate deviations (documented, not bugs): drawer width 280/320 vs token 360 (app
      shell calls for 220), slider 16dp single track (JavaFX 21 `SliderSkin` has no separate fill
      node), M3Fonts falls back to system fonts (Roboto files not bundled yet), spring token
      constants unverified against v34 motion tokens
