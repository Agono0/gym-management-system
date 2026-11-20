# docs/agents/QUEUE.md — pull-based multi-agent task queue

Protocol (from the M3FX multi-agent prompt §6.2, adapted for this harness):

- **One task = one branch-worktree commit series, reviewed by the Orchestrator before merge.**
- Status flow: `READY → CLAIMED(<worker>) → REVIEW → DONE` (or `BLOCKED` + reason).
- A task is READY only when every task in its `deps:` field is DONE.
- **File ownership**: a worker edits only the files listed in its task (plus that task's tests
  and the `docs/SPEC_NOTES/*.md` note it writes before coding). Shared files — `build.gradle`,
  `module-info.java`, `docs/agents/*`, `docs/DECISIONS.md`, `docs/COVERAGE.md`, layer READMEs —
  are edited by the **Orchestrator only**.
- **Claiming in this harness**: workers claim by reporting to the Orchestrator; the
  Orchestrator flips `claimed-by`/`status` here (worktrees are separate checkouts).
- Workers must read the matching official m3.material.io page (Styles → Specs/Guidelines/
  Accessibility) and write/update `docs/SPEC_NOTES/<item>.md` **before** coding. Values that
  cannot be verified get `// [VERIFY]` in code and an entry in `OPEN_QUESTIONS.md`.
- Every finished task: compile + tests green in the worker worktree, conventional commit
  (`fix(controls): …` / `feat(core): …` / `feat(app): …`), §10 report to the Orchestrator.

Legend — lane A = m3fx-core, lane B = m3fx-controls, lane C = gym app UI, lane Q = QA passes.

> **Push policy (user directive, 2026-11-20):** the Orchestrator keeps reviewing and merging
> locally after every green gate, but does **not** push to origin until **all tasks are DONE**;
> then a single final `git pull --rebase origin main && git push` ships everything at once.

---

## Lane A — m3fx-core

- [ ] A1 | lane:A | deps:— | files: m3fx-core/src/main/java/io/m3fx/core/theme/{M3Theme,ThemeVariant,ColorRole}.java, m3fx-core/.../color/dynamiccolor/{MaterialDynamicColors,DynamicScheme,ColorSpec}.java, m3fx-core/src/test/.../M3ThemeTest.java
  goal: Expose the vendored color-spec versions (2021/2025/2026) and the CMF variant through the
  public theme API (additive — default stays SPEC_2021 so rendered colors do not shift); make
  `MaterialDynamicColors` role *definitions* follow the scheme's spec version instead of the
  pinned `ColorSpec2026` field; fix the misleading "2026 spec" comments in `M3ThemeTest`.
  done-when: new API documented with Javadoc, default output byte-identical (test asserts
  baseline colors unchanged), contrast-level 0.5/1.0 tests added, full :m3fx-core:test green.
  claimed-by: A | status: DONE (round 1, commit 1b82828 — findings: pinned-ColorSpec2026 is
  upstream mcu-main design, documented not changed; contrast gate 120 assertions pass)

- [ ] A2 | lane:A | deps:— | files: m3fx-core/.../token/ShapeTokens.java + new ShapeTokensTest.java
  goal: Fix the spec naming and lookup gaps: `forName` gains `none`, 32 and 48; rename 32 to the
  spec name *extra-large-increased* keeping `EXPRESSIVE_LARGE_INCREASED` as a deprecated alias;
  document the full M3 shape scale in the Javadoc.
  done-when: lookup table covers the whole scale, alias compiles with @Deprecated, tests green.
  claimed-by: A | status: DONE (round 2, commit 356c3c2 — scale sourced from upstream
  material-web v34 generated tokens; zero external callers verified pre-rename; core tests 28)

- [ ] A3 | lane:A | deps:— | files: m3fx-core/.../token/MotionTokens.java + new ReducedMotionTest (or MotionTokensTest extension)
  goal: Add reduced-motion support to the token layer: a global `MotionTokens.isReducedMotion()`
  / `setReducedMotion(boolean)` (with listener so skins can react), an OS-preference detector
  helper (Windows `SystemParametersInfo` SPI_GETCLIENTAREAANIMATION is NOT available from pure
  Java — use a documented app-level setting + documented `m3fx.reducedMotion` system property),
  and semantics: reduced motion ⇒ springs snap/fade ≤150 ms, effects-only.
  done-when: flag propagates, tests cover snap semantics, Javadoc documents the system property.
  claimed-by: A | status: DONE (round 2, commit 0a794ff — ReadOnlyBooleanProperty-based flag,
  system-property init, DURATION_REDUCED_FADE=150 [VERIFY] tag noted for B7 ratification;
  core tests 32)

- [ ] A4 | lane:A | deps:— | files: m3fx-core/.../token/{SpringInterpolator,MotionTokens}.java
  goal: Bridge the two spring dialects: add `SpringToken.toInterpolator()` (ω = √k) and a
  `SpringInterpolator.from(SpringToken)` factory; deprecate the divergent legacy presets that
  duplicate SPATIAL_* semantics; tests proving spatial tokens can overshoot and EFFECTS_* tokens
  never do (ζ=1).
  done-when: one canonical conversion, tests prove overshoot/no-overshoot, no behavior change
  for existing callers.
  claimed-by: A | status: DONE (round 2, commit 26a86e9 — ω=√k mapping cited from androidx
  SpringSimulation.kt; zero-caller presets deprecated; physics suite 6 tests; core tests 38;
  note for B7/C10: bridged SPATIAL presets settle faster in normalized time)

- [ ] A5 | lane:A | deps:— | files: m3fx-controls/.../internal/M3Stylesheets.java, m3fx-controls/.../internal/ThemeColors.java, m3fx-core/.../token/ElevationLevel.java
  goal: Hygiene: track + delete the generated theme temp CSS when a scene is disposed (and on JVM
  shutdown for stragglers); fix the `ThemeColors` static dark-baseline fallback cache; cache
  `ElevationLevel.values()` (or index math) in `of()`.
  done-when: no temp file leak per theme re-apply, all three fixes tested or verified by run.
  claimed-by: — | status: READY

- [ ] A7 | lane:A | deps:— | files: m3fx-core/.../token/SpringInterpolator.java (or a new shared easing class), m3fx-controls/.../progress/M3LinearProgress.java + its test
  goal: Promote the web-semantics overshoot bezier (`OvershootSpline`, born nested in
  `LinearSkin` during B1) to a shared token-layer easing: cubic-bezier with x∈[0,1], y free,
  for material-web curves whose y control points leave [0,1] (JavaFX `Interpolator.SPLINE`
  throws on those). `M3LinearProgress` switches to the shared class; core test pins the spline
  math against Interpolator.SPLINE for in-range points.
  done-when: one canonical easing class in core, LinearProgress uses it, tests green.
  claimed-by: — | status: READY

- [ ] A6 | lane:A | deps:A2,A3,A4 | files: m3fx-core/src/test/**
  goal: Test the currently untested core tokens: ShapeTokens lookup, StateOpacity values,
  ElevationLevel mapping, TypeScale completeness (15 roles), SpringInterpolator endpoints.
  done-when: new test classes green, no product code changed.
  claimed-by: — | status: BLOCKED (A2,A3,A4)

## Lane B — m3fx-controls

- [ ] B1 | lane:B | deps:— | files: m3fx-controls/.../selection/M3Switch.java, progress/M3LinearProgress.java, glass/GlassPaneSkin.java
  goal: Leak + perf fixes: `SwitchSkin.dispose()` removes the MOUSE_CLICKED/KEY_PRESSED handlers
  it added; `LinearSkin.dispose()` removes the sceneProperty listener; indeterminate loop not
  torn down/rebuilt on every width change (reuse + clamp instead); `GlassPaneSkin` starts the
  physics `AnimationTimer` only when the pane is shown/visible and stops it when hidden.
  done-when: dispose leaves zero stray handlers/listeners (test), resize churn gone, glass panes
  that are off-screen cost no pulses; full :m3fx-controls:test green.
  claimed-by: B | status: DONE (round 1, commits c00630a+c846913 — bonus: found+fixed
  M3LinearProgress indeterminate SPLINE crash (y outside [0,1]) + invisible-bars-on-first-use;
  new OvershootSpline; 168 controls tests green)

- [ ] B2 | lane:B | deps:— | files: m3fx-controls/.../chip/M3Chip.java, iconbutton/M3IconButton.java, common/M3Tooltip.java, snackbar/M3Snackbar.java, data/M3DataTable.java, carousel/M3Carousel.java
  goal: Keyboard + a11y: `M3Chip` Space/Enter fires ACTION (toggle semantics for FILTER) +
  test; `M3IconButton` accessible text derived from its graphic icon name (no more hardcoded
  "Icon button"); icon-only controls auto-install tooltips (icon button, snackbar close, table
  pagination) via `M3Tooltip.install`; `M3Carousel` gains arrow-key navigation + a11y role.
  done-when: keyboard walkthrough of chips/carousel works, tooltip present, tests added.
  claimed-by: — | status: READY

- [ ] B3 | lane:B | deps:— | files: m3fx-controls/.../icon/M3Icons.java, icon/M3IconsTest.java
  goal: Expand the Material Symbols catalog with every name the app/gallery uses plus a common
  set (fitness, pan_tool, play_arrow, pause, water_drop, monitor_heart, ecg_heart?, timer,
  trophy?, scale?, body?, sport icons…). Every path copied from Material Symbols (Apache 2.0)
  and verified to render; unknown-icon audit across all call sites ends with zero
  `[m3fx] M3Icon: unknown icon name` warnings.
  done-when: `grep -rn "M3Icon.symbol(\|M3Icon.ofGlyph(" src/main/java` names ⊆ `M3Icons.names()`
  (enforced by a new test that reflects over the catalog), M3IconsTest covers the new paths.
  claimed-by: B | status: DONE (round 2, commit 949e301 — 37→57 glyphs, all verbatim from
  google/material-design-icons 24px.svg; 138 call sites audited; full 120-capture matrix with
  ZERO unknown-icon warnings; controls tests 172)

- [ ] B4 | lane:B | deps:— | files: m3fx-controls/.../button/M3Button.java, card/M3Card.java, iconbutton/M3IconButton.java, fab/M3Fab.java, appbar/M3TopAppBar.java, selection/M3Switch.java, resources/io/m3fx/css/m3fx.css, module-info.java (orchestrator applies)
  goal: Token hygiene: ripple/card/switch clip radii and hardcoded control dimensions move to
  `ShapeTokens`/documented constants that agree with the CSS (fix ripple clip 40 vs CSS radius
  20 mismatch); fix `.side-nav-rail` `-fx-font-weight: 500` (invalid) to a supported weight; add
  `.headline-large/medium/small` classes mirroring `.title-*`; export `io.m3fx.controls.glass`
  in `module-info.java`.
  done-when: no visual regression in screenshots, CSS warnings clean, tests green.
  claimed-by: — | status: READY

- [ ] B5 | lane:B | deps:— | files: m3fx-controls/.../overlay/OverlayLayer.java, menu/M3Menu.java, snackbar/M3Snackbar.java
  goal: Overlay correctness: anchored panels reposition on window move/resize (listen to scene/
  x/y/width and recompute); `M3Menu.showBelow` no longer falls back to screen origin (clamps to
  owner bounds or hides); snackbar hover-pause gets a pause cap so a hovered snackbar cannot live
  forever.
  done-when: manual overlay test at moved/resized window passes, tests added.
  claimed-by: — | status: READY

- [ ] B6 | lane:B | deps:— | files: m3fx-controls/.../chart/M3*Chart.java (theme repaint), data/M3DataTable.java
  goal: Charts repaint when the theme changes (listen to scene theme property / `M3Stylesheets`
  change and redraw canvas charts — currently stale until data change); `M3DataTable` search
  filters with a short debounce instead of a full `rebuildRows()` per keystroke.
  done-when: seed-switch repaint verified in gallery, typing in a 1000-row table stays smooth,
  tests green.
  claimed-by: — | status: READY

- [ ] B10 | lane:B | deps:— | files: m3fx-controls/.../data/M3StatCard.java + its test
  goal: Stat-card truncation pass (found in round-1 screenshots on merged main): title, badge
  and supporting labels truncate to unreadable slivers at 1024/1280 ("TOTAL A…",
  "of 20 members on r…", "Requires Foll…"). Per M3 card spec the label must ellipsize at a
  sensible minimum or wrap — pick the spec-conformant behavior, verify in the gallery page and
  the dashboard/customer-list KPI strips at 1024 and 1280.
  done-when: screenshots at both widths show readable labels/badges, test added, suite green.
  claimed-by: — | status: READY

- [ ] B11 | lane:B | deps:— | files: m3fx-controls/.../textfield/M3TextField.java + test (m3fx.css if needed)
  goal: Resting-state label bug (found in the round-1 full-matrix screenshots, reproducible on
  equipment/membership/schedule/staff screens): an EMPTY, unfocused M3TextField renders its
  label floating ABOVE the field instead of inside it, contradicting the M3 text-field spec
  (label floats only on focus or when non-empty; at rest it sits inside the field for the
  filled variant). Investigate the `:floated` pseudo-class logic and fix; verify in the gallery
  inputs page + equipment screen.
  done-when: empty+unfocused field shows the label inside per spec; focused/non-empty floats;
  test added; suite green; screenshots clean.
  claimed-by: — | status: READY

- [ ] B12 | lane:B | deps:— | files: m3fx-controls/.../glass/** (GlassPaneSkin + related), + test
  goal: **HIGH — systemic ghost text** (reproduced on login, availability, attendance screens
  in the baseline matrix): text inside/near `M3GlassPane` renders TWICE with a slight offset
  ("GYM-OS // ATHLETIC ENGINE", "STATION: FRONT_DESK_01", "TLS 1.3 / WAL ACTIVE", the
  availability title, the attendance scanner panel copy). Likely the glass content pipeline
  adding content to two parents or a reflection/backdrop layer painting real text. Find the
  root cause in GlassPaneSkin, fix, and prove each of the three screens renders single text
  (screenshots at 1280).
  done-when: no duplicate text on the three reproducer screens, glass tests green, no
  performance regression in the glass path.
  claimed-by: — | status: READY

- [ ] B7 | lane:B | deps:A3 | files: m3fx-controls/.../internal/MotionUtil.java, internal/SpringAnimation.java, internal/StateLayer.java
  goal: Honor `MotionTokens.isReducedMotion()` in the controls internals: `MotionUtil.enter/exit/
  splash/shake/pressScale` degrade to ≤150 ms fades; `SpringAnimation` snaps to target; state
  layers keep instant feedback. Also add the public motion-scheme switch (expressive/standard)
  where `MotionUtil` picks presets, reading a documented property + API.
  done-when: with reduced motion on, no spring/overshoot animation runs (test), gallery demo
  works.
  claimed-by: — | status: BLOCKED (A3)

- [ ] B8 | lane:B | deps:B1,B2,B3,B4,B5,B6,B7 | files: m3fx-controls/build.gradle (orchestrator applies), m3fx-controls/CHANGELOG.md
  goal: Version bump to 0.1.1 and CHANGELOG.md entry listing every additive API + fix from
  B1–B7 (migration notes where behavior changed, e.g. icon-button accessible text).
  done-when: changelog accurate, version bumped, orchestrator informed.
  claimed-by: — | status: BLOCKED (B1..B7)

- [ ] B9 | lane:B | deps:— | files: m3fx-controls/src/test/... (new: textfield, appbar, overlay facade tests)
  goal: Cover the untested components the app depends on: `M3TextField` (floated/error/counter),
  `M3TopAppBar` (variants/heights), `M3Overlay` facade (showDialog/showSheet/showSnackbar
  partitioning).
  done-when: new test classes green, no product changes needed (report if bugs found → new task).
  claimed-by: — | status: READY

## Lane C — gym app UI (pure Java, m3fx only, real data)

- [ ] C0 | lane:C | deps:— | files: src/main/java/com/gym/ui/** (new UiKit + Data), src/main/java/com/gym/tools/** (new), build.gradle (orchestrator applies)
  goal: Shared foundation: (1) `com.gym.ui.UiKit` — findById, feedback layer, confirm-dialog
  factory, rail-nav helper, empty/error/loading state builders, form-row builder; (2)
  `com.gym.ui.Data` — first-run `DatabaseConnection.initializeDatabase()` + demo seeding
  (reuse the CLI seeder logic; document in DECISIONS.md proposal), per-controller DAO access
  with `javafx.concurrent.Task` background loading and `Platform.runLater` result application;
  (3) `com.gym.tools.ScreenShooter` — snapshot every `Screen` (+ shell with DASHBOARD) at
  1024/1280/1600 × dark/light into build/screens/ using WritableImage→ImageIO (no javafx.swing);
  gradle task `screenshots`.
  done-when: `./gradlew screenshots` produces PNGs for all screens + shell; UiKit adopted by at
  least one existing screen as proof (CustomerList feedback/nav); compile + existing tests green.
  claimed-by: C | status: DONE (round 1, commit f6198fc — ScreenShooter built by orchestrator;
  UiKit/Data/DemoSeeder shipped; CustomerList loads real 20-member roster with loading/empty/
  error states; verified by screenshots + sqlite counts on merged main)

- [ ] C1 | lane:C | deps:C0 | files: src/main/java/com/gym/controller/AppShellController.java, GymApp.java, LoginController/GateController (login path only)
  goal: Shell responsiveness + real login: width-class listener (compact <600 → M3NavigationBar,
  600-839 medium → collapsed rail, ≥840 → expanded rail; ≥1200 expanded paddings) using
  `M3AdaptiveScaffold`/`WindowSizeClass` semantics; `navHelp` stops mapping to GYM_FLOOR_MAP
  (real docs/help destination); launch-gate login validates against `AdministratorDao`
  (rule 16: inactive accounts cannot log in; error copy per spec); reduced-motion toggle in the
  shell theme strip.
  done-when: shell adapts at 600/840/1200 (screenshots), login rejects wrong/inactive
  credentials with proper states.
  claimed-by: — | status: BLOCKED (C0)

- [ ] C2 | lane:C | deps:C0,B3 | files: src/main/java/com/gym/controller/{Dashboard,Progress,Schedule,GymFloorMap,LiveWorkout,Equipment,Membership}Controller.java
  goal: Anti-slop sweep (mechanical, no behavior change): every emoji glyph → `M3Icon`
  (LiveWorkout ⚠️⏸️🔄✅, Equipment 🛠️🗑️, Membership ✕); every hardcoded `Color.web("#…")` →
  theme roles via UiKit helper (dashboard green/red, progress purple, schedule cyan, floor-map
  legend) keeping the semantic (success/error/tint) mapping documented.
  done-when: `grep -rn "Color.web(\"#" src/main/java/com/gym/controller` returns nothing except
  GymApp SEED_PURPLE; `grep -rnP "[\x{1F300}-\x{1FAFF}\x{2600}-\x{27BF}]"` over controllers is
  empty; screenshots show no regression.
  claimed-by: — | status: BLOCKED (C0,B3)

- [ ] C3 | lane:C | deps:C0,C2 | files: src/main/java/com/gym/controller/{DashboardController,CustomerListController,CustomerProfileController}.java
  goal: Real-data rebuild: Dashboard KPIs/charts/tables query CustomerDao/MembershipDao/
  PaymentDao/SessionDao on background Tasks (loading skeletons, error + retry states); Customer
  list = CustomerDao.findAll() with live search/filter; freeze/renew/assign per rules 4/6/7/15
  via DAO calls, ValidationException surfaced in dialogs/snackbars; check-in feed reflects
  AttendanceDao (rule 5). One primary action per region; tooltips on icon buttons.
  done-when: create→edit→freeze→renew a real customer round-trips through SQLite; empty DB
  shows designed empty state; screenshots reviewed.
  claimed-by: — | status: BLOCKED (C0,C2)

- [ ] C4 | lane:C | deps:C0,C2 | files: src/main/java/com/gym/controller/{LiveWorkoutController,ScheduleController}.java
  goal: Real sessions: Schedule books/confirms/cancels/completes through SessionDao +
  TrainerAvailabilityDao (rules 1/10/17/18 — no GUI re-implementation), ≥2h cancel rule copied
  verbatim from UML; week navigation moves the agenda (real date math); LiveWorkout HUD binds to
  the selected CONFIRMED session, COMPLETE SET → COMPLETED via rule 18, ABORT → CANCELLED with
  reason; simulated telemetry stays simulated (labeled) but all buttons work.
  done-when: book→confirm→complete a real session round-trips; cancel<2h shows rule copy;
  screenshots reviewed.
  claimed-by: — | status: BLOCKED (C0,C2)

- [ ] C5 | lane:C | deps:C0,C2 | files: src/main/java/com/gym/controller/{EquipmentController,GymFloorMapController}.java
  goal: Equipment skeleton → real CRUD: stock TableView → M3DataTable bound to EquipmentDao;
  add/edit/condition/status/maintenance/delete handlers call the DAO (rules 4/13 semantics for
  status), search filters, export produces real CSV; floor-map occupancy derives from
  AttendanceDao counts + EquipmentDao maintenance status instead of hardcoded percentages;
  layer toggles and pin placement actually change the map.
  done-when: add→maintain→delete an equipment item persists; floor numbers match DB; screenshots.
  claimed-by: — | status: BLOCKED (C0,C2)

- [ ] C6 | lane:C | deps:C0,C2 | files: src/main/java/com/gym/controller/{MembershipController,PaymentController}.java
  goal: Membership + Payment on real data: tier carousel reflects PlanType reality; renew/freeze/
  unfreeze/cancel via MembershipDao (rules 7/14) **with confirm dialog on destructive cancel**;
  outstanding balance computed per rule 6; Payment records via PaymentDao (rule 12: exactly one
  linked object, customer match), void per rule 19 (no hard delete), dead "Print Thermal Copy"
  button either works (receipt preview dialog) or is removed.
  done-when: freeze→unfreeze→renew round-trip; payment→void→replacement round-trip; screenshots.
  claimed-by: — | status: BLOCKED (C0,C2)

- [ ] C7 | lane:C | deps:C0,C2 | files: src/main/java/com/gym/controller/{AttendanceController,StaffController}.java
  goal: Attendance on AttendanceDao (rule 5: ACTIVE customer + unexpired membership, same-day
  re-check-in updates row; rule 19: no hard delete, corrections via void fields); occupancy cap
  from config not magic 200; Staff screen actions act on the *selected* row (bug: always row 0),
  administrator CRUD via AdministratorDao with RBAC-aware copy (rule 16).
  done-when: check-in→correct→check-out round-trips; staff add/edit/delete targets selection;
  screenshots.
  claimed-by: — | status: BLOCKED (C0,C2)

- [ ] C8 | lane:C | deps:C0,C2 | files: src/main/java/com/gym/controller/{TrainerListController,TrainerProfileController,AvailabilityController,WorkoutController}.java
  goal: Trainer + program flows: pipe-string rows → typed model from TrainerDao; trainer profile
  data follows the selected coach; medical access follows rule 15 (trainerId authoritative);
  availability matrix writes via TrainerAvailabilityDao (rule 8: multiple hourly slots, cap,
  start<end) replacing the dead steppers with real slot editing; Workout builder persists
  WorkoutProgramDao programs/versions, exercises assignable, no empty handlers remain.
  done-when: assign trainer→availability slot→book slot→program build round-trips; screenshots.
  claimed-by: — | status: BLOCKED (C0,C2)

- [ ] C9 | lane:C | deps:C0,C2 | files: src/main/java/com/gym/controller/{ProgressController,MedicalProfileController,LoginController}.java
  goal: Progress reads ProgressRecordDao (chart series from real records, target weight from
  data not baked 80.0; celebration triggers on real PR); MedicalProfile per rule 3 (zero or one
  per customer, created on demand) via MedicalProfileDao; Login gate credential check (shared
  with C1's AdministratorDao work — coordinate, C1 owns the shell path).
  done-when: log weight→chart updates; medical create→edit→delete(undo) round-trips; screenshots.
  claimed-by: — | status: BLOCKED (C0,C2)

- [ ] C10 | lane:C | deps:B3,C0 | files: src/main/java/com/gym/MainApp.java
  goal: Gallery pass: demo pages updated for the new library behavior (icon additions, reduced
  motion, headline classes); motion-scheme toggle + reduced-motion demo page; unknown-icon
  audit clean; remove dead `WritableImage` import; keep `run`/`runSamples` launching clean.
  done-when: gallery screenshots reviewed, zero stderr warnings.
  claimed-by: — | status: BLOCKED (B3,C0)

## Lane Q — QA passes (P5)

- [ ] Q1 | lane:Q | deps:C3,C4,C5,C6,C7,C8,C9 | files: app-wide
  goal: Reduced-motion + motion-scheme pass over the whole app (springs only where spec says,
  interruptible, timers stop, reduced-motion respected).
  done-when: checklist in STATE.md ticked with evidence.
  claimed-by: — | status: BLOCKED (C3..C9)
- [ ] Q2 | lane:Q | deps:C3,C4,C5,C6,C7,C8,C9 | files: app-wide
  goal: Dark/light + contrast pass: every screen in both themes, text ≥4.5:1, borders visible.
  done-when: screenshots both themes reviewed, issues fixed.
  claimed-by: — | status: BLOCKED (C3..C9)
- [ ] Q3 | lane:Q | deps:C3,C4,C5,C6,C7,C8,C9 | files: app-wide
  goal: Keyboard-only pass: tab order, focus rings, Enter/Space/Esc/arrows per screen; 48dp hit
  targets.
  done-when: walkthrough log in STATE.md, gaps fixed.
  claimed-by: — | status: BLOCKED (C3..C9)
- [ ] Q4 | lane:Q | deps:C3,C4,C5,C6,C7,C8,C9 | files: app-wide
  goal: Resize pass at 600/840/1200/1600 widths: no clipped text, no overflow, navigation swaps
  correctly, long strings ellipsize per spec.
  done-when: screenshot matrix reviewed at all widths.
  claimed-by: — | status: BLOCKED (C3..C9)
- [ ] Q5 | lane:Q | deps:C3,C4,C5,C6,C7,C8,C9 | files: app-wide
  goal: Performance/memory pass: idle CPU ~0%, no jank while resizing/animating, 50× screen
  open/close without growth (watchdog clean).
  done-when: measurements recorded in STATE.md.
  claimed-by: — | status: BLOCKED (C3..C9)
- [ ] Q6 | lane:Q | deps:Q1..Q5 | files: docs/
  goal: Final visual acceptance: full screenshot matrix vs spec notes + anti-slop list; update
  COVERAGE.md, DECISIONS.md, layer READMEs; CHANGELOG.
  done-when: all docs updated, everything green and pushed.
  claimed-by: — | status: BLOCKED (Q1..Q5)

---

## Round log (orchestrator)

| Round | A | B | C | Merged at |
|---|---|---|---|---|
| 1 | A1 (1b82828) | B1 (6164e24) | C0 (a7f23de) | a7f23de, pushed — build green, core tests 23, controls 168, customer-list real-data render verified |
| 2 | A2 (356c3c2), A3 (0a794ff), A4 (26a86e9) | B3 (949e301) | C1 | A2-A4+B3 merged locally 154f7b1 (push deferred per user directive); C1 in progress |
