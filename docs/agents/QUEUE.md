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
  claimed-by: A | status: DONE (round 3, commit 696a73a — refcount + ReferenceQueue + shutdown
  sweep; ThemeColors fallback was unreachable dead code, removed with null-guard documented;
  ElevationLevel array cached)

- [ ] A7 | lane:A | deps:— | files: m3fx-core/.../token/SpringInterpolator.java (or a new shared easing class), m3fx-controls/.../progress/M3LinearProgress.java + its test
  goal: Promote the web-semantics overshoot bezier (`OvershootSpline`, born nested in
  `LinearSkin` during B1) to a shared token-layer easing: cubic-bezier with x∈[0,1], y free,
  for material-web curves whose y control points leave [0,1] (JavaFX `Interpolator.SPLINE`
  throws on those). `M3LinearProgress` switches to the shared class; core test pins the spline
  math against Interpolator.SPLINE for in-range points.
  done-when: one canonical easing class in core, LinearProgress uses it, tests green.
  claimed-by: A | status: DONE (round 3, commit 5b667e9 — `io.m3fx.core.token.OvershootSpline`
  with CSS-spec abscissa validation; LinearProgress rewired; 4 core tests)

- [ ] A6 | lane:A | deps:A2,A3,A4 | files: m3fx-core/src/test/**
  goal: Test the currently untested core tokens: ShapeTokens lookup, StateOpacity values,
  ElevationLevel mapping, TypeScale completeness (15 roles), SpringInterpolator endpoints.
  done-when: new test classes green, no product code changed.
  claimed-by: A | status: DONE (round 3, commit f73b4f7 — StateOpacity/TypeScale/
  SpringInterpolatorEndpoints pinned against v34 tokens; ElevationLevel covered by A5's test;
  all values agree, zero product changes)

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
  claimed-by: A (pooled) | status: DONE (round 4, commit 9b17a06 — chip keyboard was
  architecturally missing (SkinBase dropped ButtonBehavior); icon-button a11y derives from
  icon name; installIfAbsent tooltips; carousel keys+role; MotionTokens.spring()/DEFAULT
  deprecated consumerless; controls 197 + core 66 tests. NOTE: M3Button accessibleText is now
  bound — callers must not setAccessibleText on text buttons (Worker C hit this, worked around))

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
  claimed-by: A (pooled) | status: DONE (round 5, commit 5bf9571 — anchored panels follow
  window moves via delta (localToScreen lags a pulse), menu defers instead of origin fallback,
  snackbar 30 s cap; controls tests 208)

- [ ] B6 | lane:B | deps:— | files: m3fx-controls/.../chart/M3*Chart.java (theme repaint), data/M3DataTable.java
  goal: Charts repaint when the theme changes (listen to scene theme property / `M3Stylesheets`
  change and redraw canvas charts — currently stale until data change); `M3DataTable` search
  filters with a short debounce instead of a full `rebuildRows()` per keystroke.
  done-when: seed-switch repaint verified in gallery, typing in a 1000-row table stays smooth,
  tests green.
  claimed-by: B | status: DONE (round 5, commit 6afb2ca — ChartThemeSupport repaint hook wired
  into all 10 canvas charts + per-chart repaint counters in tests; 200 ms search debounce with
  Enter-immediate; controls tests 205)

- [ ] B10 | lane:B | deps:— | files: m3fx-controls/.../data/M3StatCard.java + its test
  goal: Stat-card truncation pass (found in round-1 screenshots on merged main): title, badge
  and supporting labels truncate to unreadable slivers at 1024/1280 ("TOTAL A…",
  "of 20 members on r…", "Requires Foll…"). Per M3 card spec the label must ellipsize at a
  sensible minimum or wrap — pick the spec-conformant behavior, verify in the gallery page and
  the dashboard/customer-list KPI strips at 1024 and 1280.
  done-when: screenshots at both widths show readable labels/badges, test added, suite green.
  claimed-by: B | status: DONE (round 9, commit 7adc969 — header space policy: title ≥72px
  grows, badge hides whole instead of cutting mid-word, tooltips carry full text; stale
  pre-CSS binding dependency fixed; full titles verified at 1024/1280)

- [ ] B11 | lane:B | deps:— | files: m3fx-controls/.../textfield/M3TextField.java + test (m3fx.css if needed)
  goal: Resting-state label bug (found in the round-1 full-matrix screenshots, reproducible on
  equipment/membership/schedule/staff screens): an EMPTY, unfocused M3TextField renders its
  label floating ABOVE the field instead of inside it, contradicting the M3 text-field spec
  (label floats only on focus or when non-empty; at rest it sits inside the field for the
  filled variant). Investigate the `:floated` pseudo-class logic and fix; verify in the gallery
  inputs page + equipment screen.
  done-when: empty+unfocused field shows the label inside per spec; focused/non-empty floats;
  test added; suite green; screenshots clean.
  claimed-by: B | status: DONE (round 9, commit 14cd60b → main 869bbb6 — root cause was CSS
  having no positional styles at all, not the state logic; label now overlays inside the 56dp
  box with translateY float; leading-icon 40px alignment. Merge note: A's and B's
  M3TextFieldTest both existed → B's split to M3TextFieldRestingLabelTest, A's lookups
  adapted (a37e16c))

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
  claimed-by: B | status: DONE (round 3, salvage commit 42c2a51 → main 2fa2fa5 — Worker B hit
  the quota limit after implementing + green tests; orchestrator verified reproducer
  screenshots (single text on login/availability/attendance) and committed)

- [ ] B13 | lane:B | deps:— | files: m3fx-controls/src/main/resources/io/m3fx/css/m3fx.css (+ fab section)
  goal: FAB shape per spec (found by B4): `.m3-fab-small` 12px / `.m3-fab-large` 28px /
  `.m3-fab-extended` 16px / default `.m3-fab` NO radius — M3 spec: FAB shape = large (16dp)
  for all sizes. Fix the radii, verify in gallery/screenshots.
  done-when: radii 16 across sizes, screenshots clean, suite green.
  claimed-by: B | status: DONE (round 6, commit 5740d81 — the two divergent size-variant rules
  fixed; the default-rule part of the queue note was stale, already correct on main; visual
  probe confirmed 16px corners on all four sizes)

- [ ] B14 | lane:B | deps:— | files: m3fx-controls/.../icon/M3Icon.java + test
  goal: One-line hygiene (found by B2): `M3Icon.symbol(name)` calls `renderSymbol` directly and
  never sets `symbolNameProperty`, so property-based consumers see null. Route `symbol()` through
  `setSymbolName` (property listener renders), keep behavior identical, test the property path.
  done-when: `symbol()` sets the property, property changes re-render, tests green.
  claimed-by: B | status: DONE (round 6, commit 86c8ba9 — property path + re-render + warn-once
  pinned; ofSvg/ofGlyph raw-content semantics documented by test; B6 test file rawtypes cleaned;
  controls tests 209)

- [ ] B15 | lane:B | deps:B9 | files: m3fx-controls/.../appbar/M3TopAppBar.java (+ its test)
  goal: Fix the a11y gap B9 pinned: M3TopAppBar publishes no accessible text and only a generic
  role. Give it a TOOL_BAR role + accessible text summarizing the title (and nav/actions count);
  keep it out of the tab-focus cycle (it is presentational) unless a child is focusable.
  done-when: B9's flagged assertion flips green (updated to spec behavior), suite green.
  claimed-by: A | status: DONE (round 8, commit 1cce386 — TOOL_BAR role + live summary text,
  focus contract; bonus: fixed the M3TopAppBarTest missing-FX-bootstrap ordering-luck hole;
  controls tests 241)

- [ ] B16 | lane:B | deps:— | files: m3fx-controls/.../floorplan/M3GymFloorMap.java (+ test)
  goal: Layer API gap (found by C5): the floor map has no layer-visibility API and does not
  redraw on list changes — pins/heatmap/zones/sensors draw unconditionally in redraw(). Add
  per-layer visibility properties (pins/heatmap/zones/sensors) honored by redraw, plus a
  redraw hook on equipment-list changes (drop the app's re-mount workaround — app side can be
  simplified in Q-pass).
  done-when: toggling a layer property changes the render, list change triggers redraw, test
  covers both; suite green.
  claimed-by: A | status: DONE (round 11, commit 63942b6 → main b0616ac — pinsVisible/
  zonesVisible properties honored in redraw (hidden pins leave hit-testing), observed
  equipmentList redraws on change; honest scope: no heatmap/sensor layers invented — those
  toggles stay disabled UI; pixel-evidence tests with transparent-snapshot + pulse lessons
  recorded; controls tests 241)

- [ ] B7 | lane:B | deps:A3 | files: m3fx-controls/.../internal/MotionUtil.java, internal/SpringAnimation.java, internal/StateLayer.java
  goal: Honor `MotionTokens.isReducedMotion()` in the controls internals: `MotionUtil.enter/exit/
  splash/shake/pressScale` degrade to ≤150 ms fades; `SpringAnimation` snaps to target; state
  layers keep instant feedback. Also add the public motion-scheme switch (expressive/standard)
  where `MotionUtil` picks presets, reading a documented property + API.
  done-when: with reduced motion on, no spring/overshoot animation runs (test), gallery demo
  works.
  claimed-by: A (pooled) | status: DONE (round 3, commit 57ccd5c — MotionScheme enum in core,
  snap semantics + live flag-flip in SpringAnimation, 150 ms fades in MotionUtil, instant
  StateLayer; 13 new tests; M3BottomSheet one-line migration pending in B2)

- [ ] B8 | lane:B | deps:B1,B2,B3,B4,B5,B6,B7 | files: m3fx-controls/build.gradle (orchestrator applies), m3fx-controls/CHANGELOG.md
  goal: Version bump to 0.1.1 and CHANGELOG.md entry listing every additive API + fix from
  B1–B7 (migration notes where behavior changed, e.g. icon-button accessible text).
  done-when: changelog accurate, version bumped, orchestrator informed.
  claimed-by: B | status: DONE (round 10, commit 40b7699 — coordinated 0.1.1 release: both
  modules bumped, two changelogs written with migration notes; artifacts build as 0.1.1 jars.
  **LANE B COMPLETE — B1-B16 all DONE.**)

- [ ] B9 | lane:B | deps:— | files: m3fx-controls/src/test/... (new: textfield, appbar, overlay facade tests)
  goal: Cover the untested components the app depends on: `M3TextField` (floated/error/counter),
  `M3TopAppBar` (variants/heights), `M3Overlay` facade (showDialog/showSheet/showSnackbar
  partitioning).
  done-when: new test classes green, no product changes needed (report if bugs found → new task).
  claimed-by: A | status: DONE (round 7, commit 211f1cc — textfield 9 / top-app-bar 7 /
  overlay-facade 7 tests; red-first evidence pinned for B11 (floating label never announced)
  and a new B15 (app bar publishes no accessible text/role); controls tests 233)

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
  claimed-by: C | status: DONE (round 2, commit 1d60941 → main 2802906 — WindowSizeClass-driven
  chrome with 4-primaries+More bottom bar (M3NavigationBar is specced 3–5 destinations; 10-in-bar
  would need a library change — orchestrator-approved interpretation); real rule-16 login with
  headless auth tests (7 green); navHelp dialog; title/search overlap FIXED; GymApp min width
  600. Follow-ups noted: dashboard 600 horizontal clip → C2; StatCard truncation → B10)

- [ ] C2 | lane:C | deps:C0,B3 | files: src/main/java/com/gym/controller/{Dashboard,Progress,Schedule,GymFloorMap,LiveWorkout,Equipment,Membership}Controller.java
  goal: Anti-slop sweep (mechanical, no behavior change): every emoji glyph → `M3Icon`
  (LiveWorkout ⚠️⏸️🔄✅, Equipment 🛠️🗑️, Membership ✕); every hardcoded `Color.web("#…")` →
  theme roles via UiKit helper (dashboard green/red, progress purple, schedule cyan, floor-map
  legend) keeping the semantic (success/error/tint) mapping documented. PLUS the dashboard
  600px clip fix (from C1 findings): make the KPI strip/action row reflow at compact width.
  **HANDOFF NOTE (quota interruption)**: `../gym-wt-C` holds a DIRTY reference implementation —
  the emoji/hex purge portion there passed ALL greps (zero emoji, zero `Color.web("#`) and
  compiles, but the dashboard-600 reflow is BROKEN mid-pivot (overlapping content at 600).
  Worker C's verified lesson before dying: binding content pref-height to width corrupts layout
  (measured 716px) — use width-class-driven reflow with shared nodes (like the C1
  `applyResponsiveChrome` swap pattern), NO width-bound ScrollPanes. Redo the sweep fresh in
  your own worktree (inspect the reference for the mapping choices), and finish the reflow
  properly.
  done-when: `grep -rn "Color.web(\"#" src/main/java/com/gym/controller` returns nothing except
  GymApp SEED_PURPLE; `grep -rnP "[\x{1F300}-\x{1FAFF}\x{2600}-\x{27BF}]"` over controllers is
  empty; dashboard renders clean at 600 AND 1280 (screenshots); no regression on touched screens.
  claimed-by: C | status: DONE (round 4, commits ea92798+d4db029 → main b16dc17 — all greps
  clean, raw-SVG dedupe done, UiKit.themeRole(ColorRole) added, dashboard 600 = width-class
  reflow (stacked hero, 2×2 KPI grid) verified by screenshots on merged main)

- [ ] C3 | lane:C | deps:C0,C2 | files: src/main/java/com/gym/controller/{DashboardController,CustomerListController,CustomerProfileController}.java
  goal: Real-data rebuild: Dashboard KPIs/charts/tables query CustomerDao/MembershipDao/
  PaymentDao/SessionDao on background Tasks (loading skeletons, error + retry states); Customer
  list = CustomerDao.findAll() with live search/filter; freeze/renew/assign per rules 4/6/7/15
  via DAO calls, ValidationException surfaced in dialogs/snackbars; check-in feed reflects
  AttendanceDao (rule 5). One primary action per region; tooltips on icon buttons.
  done-when: create→edit→freeze→renew a real customer round-trips through SQLite; empty DB
  shows designed empty state; screenshots reviewed.
  claimed-by: C | status: DONE (round 6, commits 083c538+7871e19 → main 5b1b606 — real
  DashboardSnapshot (8 DAO queries, rule-5 check-in proven by sqlite round-trip: same-day
  re-check-in updates not inserts), real profile dossier (freeze/unfreeze/renew/coach/biometrics/
  program via DAOs, rule-7 arithmetic mirrored from GymRulesHelper), honest empty states; fake
  numbers + Random jitter deleted. **Lane-wide lesson for C4-C8 adopters: shared-node layout
  swaps must build compact containers EMPTY and fill them only in the width-class apply —
  draining desktop containers at construction leaves them blank unless a class change fires;
  and never trust pre-CSS prefHeight measurements**)

- [ ] C4 | lane:C | deps:C0,C2 | files: src/main/java/com/gym/controller/{LiveWorkoutController,ScheduleController}.java
  goal: Real sessions: Schedule books/confirms/cancels/completes through SessionDao +
  TrainerAvailabilityDao (rules 1/10/17/18 — no GUI re-implementation), ≥2h cancel rule copied
  verbatim from UML; week navigation moves the agenda (real date math); LiveWorkout HUD binds to
  the selected CONFIRMED session, COMPLETE SET → COMPLETED via rule 18, ABORT → CANCELLED with
  reason; simulated telemetry stays simulated (labeled) but all buttons work.
  done-when: book→confirm→complete a real session round-trips; cancel<2h shows rule copy;
  screenshots reviewed.
  claimed-by: C | status: DONE (round 8, commits 80a2537+b250df5 → main d3540b6 — real 7-day
  WeekSnapshot, rule-1 booking menu, rule-18 lifecycle + rule-10 cancel-with-reason, rule-17
  capacity, Run Live handoff to the HUD; COMPLETE/ABORT drive real terminal transitions;
  SIMULATED TELEMETRY badge; sqlite round-trip evidence. Residuals for Q4: day headers
  truncate at 1280 ("Mo…"), coach-roster list items truncate; inspector panel sparse at some
  widths)

- [ ] C5 | lane:C | deps:C0,C2 | files: src/main/java/com/gym/controller/{EquipmentController,GymFloorMapController}.java
  goal: Equipment skeleton → real CRUD: stock TableView → M3DataTable bound to EquipmentDao;
  add/edit/condition/status/maintenance/delete handlers call the DAO (rules 4/13 semantics for
  status), search filters, export produces real CSV; floor-map occupancy derives from
  AttendanceDao counts + EquipmentDao maintenance status instead of hardcoded percentages;
  layer toggles and pin placement actually change the map.
  done-when: add→maintain→delete an equipment item persists; floor numbers match DB; screenshots.
  claimed-by: A (pooled) | status: DONE (round 8, commit 48eef75 → main 7f6d12f — Equipment
  real CRUD (M3DataTable, rule-13 atomic maintenance, confirmed hard-delete with RETIRED
  alternative in the dialog, real CSV export), floor map real telemetry with honest disabled
  toggles + zone readiness from EquipmentDao; sqlite round-trip evidence. Findings: B16 queued
  (M3GymFloorMap lacks layer API + list-change redraw); zone→people occupancy needs a schema
  change (documented, not invented); spec-note conflict with C4 resolved (sections renumbered))

- [ ] C6 | lane:C | deps:C0,C2 | files: src/main/java/com/gym/controller/{MembershipController,PaymentController}.java
  goal: Membership + Payment on real data: tier carousel reflects PlanType reality; renew/freeze/
  unfreeze/cancel via MembershipDao (rules 7/14) **with confirm dialog on destructive cancel**;
  outstanding balance computed per rule 6; Payment records via PaymentDao (rule 12: exactly one
  linked object, customer match), void per rule 19 (no hard delete), dead "Print Thermal Copy"
  button either works (receipt preview dialog) or is removed.
  done-when: freeze→unfreeze→renew round-trip; payment→void→replacement round-trip; screenshots.
  claimed-by: C | status: DONE (round 11, commits 45e9366+f3f83c9 → main 73c72c9 — real tier
  stats, fake funnel replaced by an honest plan-distribution bar chart, rule-7 lifecycle with
  destructive-cancel confirm, rule-6 outstanding balance, rule-12 single-link payments with
  rule-19 void+replace, real receipt preview; sqlite round-trip evidence for both flows.
  Finding: M3FunnelChart silently renders non-monotonic stages (fold-back) — candidate lane-B
  guard, parked; inspector re-reads rule-6 per selection (fine, noted for Q5))

- [ ] C7 | lane:C | deps:C0,C2 | files: src/main/java/com/gym/controller/{AttendanceController,StaffController}.java
  goal: Attendance on AttendanceDao (rule 5: ACTIVE customer + unexpired membership, same-day
  re-check-in updates row; rule 19: no hard delete, corrections via void fields); occupancy cap
  from config not magic 200; Staff screen actions act on the *selected* row (bug: always row 0),
  administrator CRUD via AdministratorDao with RBAC-aware copy (rule 16).
  done-when: check-in→correct→check-out round-trips; staff add/edit/delete targets selection;
  screenshots.
  claimed-by: B (pooled) | status: DONE (round 13, commits 10e6507+7c1d06b → main 08fe808 —
  real rule-5 turnstile (same-day update-not-insert proven), rule-19 in-place corrections,
  real occupancy vs SAFETY_CAP constant, staff selection bug fixed (row-0 die), real CRUD with
  rule-16 copy, honest empty audit timeline. Finding: StaffController viewer badge still
  hardcodes "Alex Vance" — wiring the real signed-in identity queued for the Q lane)

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
  claimed-by: A (pooled) | status: DONE (round 12, commits bcfb32f+769c98b+ce47801 → main
  ce47801 — real record-driven analytics with honest no-target/VBT dashes, real PR celebration,
  rule-3 create-on-demand medical console with real Undo delete; sqlite evidence for both;
  VBT stays a dash (no schema source). Residual for Q2/Q4: activity-calendar footer text
  still overlaps/garbles at 1280 ("streaks…longest streak" collision))

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
  claimed-by: B (pooled) | status: DONE (round 13, commits 0c4de37+da68830 → main da68830 —
  full 19-screen walkthrough table; fixed: the shell's promised-but-unwired Ctrl+K accelerator,
  mouse-only search trigger, theme toggle keyboard handler; signed-in identity wired into the
  Staff badge via shell accessors. Observations parked: M3TextField 40px icon hit target
  (<48dp guide), 3D viewer/map rotation mouse-only)
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
| 2 | A2 (356c3c2), A3 (0a794ff), A4 (26a86e9) | B3 (949e301) | C1 (1d60941) | all merged locally 2802906 — pushes deferred per user directive |
| 3 | A5 (696a73a), A6 (f73b4f7), A7 (5b667e9) | B12 | C2 | A batch merged locally 168bbb3 — lane A queue EMPTY (A1-A7 done); core tests 61, controls 174 |
| 3b | B7 (57ccd5c, pooled to A) | — | — | merged locally 57ccd5c; core tests 62, controls 185 |
| 3c | — | B12 (42c2a51, salvaged) | — | merged locally 2fa2fa5 — ghost text fixed; workers A/B/C hit quota limits; remaining tasks handed to zcode1/zcode2 |
| 4 | B2 (9b17a06, pooled to A) | B4 (f945adb) | C2 (ea92798+d4db029) | all merged locally b16dc17; runSamples fixed (M3Samples tracked, 273fbe6); controls 197, core 66 tests |
| 5 | B5 (5bf9571, pooled to A) | B6 (6afb2ca) | C3 | B5+B6 merged locally 6afb2ca→943fcf5 |
| 6 | B9 | B13 (5740d81) + B14 (86c8ba9) | — | merged locally 46814e0; controls tests 209 |
| 6b | — | — | C3 (083c538+7871e19) | merged locally 5b1b606 — dashboard+profile fully real |
| 7 | B9 (211f1cc) | B10+B11 | C4 | B9 merged locally 18f3955; controls tests 233 |
| 8 | B15 (1cce386) | B10 (088e259→7adc969) + B11 (869bbb6) | C4 (d3540b6), C5 (7f6d12f) | B10+B11 merged; A's text-field test lookups adapted (a37e16c); B16 queued |
| 9 | B16 | B8 (40b7699) | C6 | **LANE B COMPLETE** (B1-B16); merged locally 40b7699 |
| 10 | B16 (in flight) | C7 (in flight) | C6 (73c72c9) | C6 merged after spec-note renumber resolution |
| 11 | B16 (63942b6) | C7 (in flight) | C8 (in flight) | B16 merged locally b0616ac |
| 12 | C9 (bcfb32f+769c98b+ce47801, pooled to A) | — | — | merged locally ce47801 |
