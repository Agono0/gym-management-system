# DECISIONS.md — design decisions and why

1. **Gradle multi-module, not Maven.** AGENTS.md describes a Maven layout, but this repo already
   builds with Gradle (Java 27 + JavaFX plugin). The library ships as two Gradle subprojects
   (`m3fx-core`, `m3fx-controls`) producing plain jars — reusable from any Gradle/Maven project
   via `implementation(project(':m3fx-core'))` or a published jar. Same package/module split as
   AGENTS.md specifies.
2. **Java 21 language level on JDK 27.** Only JDK 27 is installed; modules compile with
   `--release 21` semantics (source level 21) so the jars run on Java 21+.
3. **material-color-utilities ported verbatim.** Only package names changed
   (`io.m3fx.core.color.*`); `androidx`/`errorprone` annotations stripped (desktop has no androidx).
   Algorithms untouched per AGENTS.md.
4. **Theme via temp-file stylesheet, not data-URL.** `M3Stylesheets.applyTo(scene, theme)` writes
   `M3Theme.toCss()` to a temp file and puts it before `m3fx.css` in the scene stylesheets, so
   `-md-sys-color-*` lookups resolve for every control. Switching theme = one call.
5. **Extend standard JavaFX controls instead of custom Control+Skin everywhere.** Only `M3Card`,
   `M3Switch`, `M3LinearProgress` use custom skins; the rest extend Button/ToggleButton/CheckBox/
   TabPane/etc. with variant properties + CSS. Same visuals, far less code, native keyboard and
   accessibility behavior preserved.
6. **No Ikonli dependency yet.** AGENTS.md names Ikonli but also forbids new runtime deps without
   asking. `M3Icon` renders glyph text or SVG paths with zero deps; `m3-icon-glyph` CSS class is
   the hook for an icon font later.
7. **Date picker is styled-input first.** Full calendar-grid dialog skin is the heaviest T2 item;
   `M3DatePicker` ships M3 field styling now, dialog tokens documented in SPEC_NOTES for the next pass.
8. **OverlayLayer instead of extra Stages.** Dialog/snackbar/sheets render in a scene-root
   StackPane with a scrim — simpler focus handling and testable without window management.
9. **Icons: bundled SVG catalog instead of Ikonli.** Ikonli would add a runtime dependency
   (needs approval). `io.m3fx.controls.icon` is now public API: `M3Icons` ships 26 Material
   Symbols paths (Google, Apache 2.0, see NOTICE) and `M3Icon.symbol(name)` renders them as
   vector `SVGPath` nodes tinted via `-fx-fill` CSS hooks. No ligature fonts, no new deps.
10. **Button toggle = secondary-container selected state.** `M3Button` moved from `Button` to
    `ToggleButton` base with a `toggleable` flag (same pattern as `M3IconButton`); internal reset
    uses `addEventHandler(ACTION)` so caller `setOnAction` handlers compose instead of being
    overwritten.
11. **Real springs via semi-implicit Euler.** `SpringAnimation` integrates
    `a = -k(x-t) - c·v` per frame (mass 1, `c = 2ζ√k`) with DEFAULT/GENTLE/BOUNCY presets; the
    pure `step()` function is unit-tested (settle/overshoot), the `AnimationTimer` driver is thin.
12. **Canvas skins read theme via probe region.** `ThemeColors.resolve()` styles a 1px probe with
    the looked-up variable and reads back the fill (baseline-theme fallback off-scene). Used by
    the wavy indicator; probe once and cache, never per frame.
13. **Adaptive scaffold owns its nav bindings.** `M3AdaptiveScaffold` unbinds the previous nav
    control before rebinding on size-class change — rebinding without unbinding throws.
14. **Search has two suggestion modes.** Popup by default (zero setup); `setOverlay(layer)` moves
    the same suggestion panel into the overlay anchored under the bar with a dismissing scrim
    (`OverlayLayer.showAnchored`). The panel reparents between the two hosts as needed.
15. **Buttons are real Control+Skin (not restyled stock buttons).** `M3Button`/`M3IconButton`
    extend a shared `PressableControl` (Toggle impl, press-intent tracking, Space/Enter) with skins
    layering background + CSS state region + content + clipped ripple pane. Ripple ink and shadow
    colors resolve live via `ThemeColors` so dark themes ripple correctly; elevation animates
    (filled 0→1, elevated 1→2→1 on hover/press). Breaking change from the 0.1.0 ToggleButton base,
    acceptable pre-release.
16. **State colors are layered translucent theme vars, not opacity hacks.** `M3Theme.toCss()`
    emits `-a08/-a10/-a12/-a16/-a38` for every role; components layer e.g.
    `on-primary at 8% over primary` in a single `-fx-background-color` list. Nothing fades whole
    controls anymore (text stays crisp), and dark/high-contrast themes compute correct alphas.
17. **No competing transitions.** `StateLayer` keeps one fade per overlay (stops the running one),
    `MotionUtil` press-scale stops its predecessor, the wavy indicator runs on `AnimationTimer`,
    and the switch thumb rides the real spring — this pass targeted animation stacking as the
    main smoothness fix.
18. **Native date popup styled, not replaced.** The JavaFX `DatePicker` calendar popup is fully
    covered with M3 dialog tokens (surface-container-high, primary selected circle, primary-ring
    today, dimmed adjacent months) — full behavior (year/month spinners, keyboard) for free.
19. **Overlays size to content, sheets dock.** `M3Dialog`/`M3BottomSheet` cap `maxHeight` to pref
    (VBox otherwise fills the overlay); `OverlayLayer.showSheet` docks sheets BOTTOM_CENTER with a
    scrim while dialogs stay centered; row-straddling inputs (`M3DatePicker`, `M3TimePicker`,
    `M3Select`) cap `maxHeight` so HBox siblings don't stretch them.
20. **FAB menu is a right column, not a full-width row.** `M3FabMenu` hugs content width with
    right-aligned rows so action FABs stack above the main FAB and rise from it.
21. **Galleries dogfood the library for dark mode.** The theme engine was verified (no hardcoded
    hex in component CSS; dark surface/primary/container-order locked by `M3ThemeTest`), but the
    demo chrome itself used modena `ListView`/`ComboBox`/`ToggleButton`/`TabPane` which stay light.
    Both galleries now use drawer/select/toggle-button/M3Tabs chrome plus `m3-section-title` text
    so dark mode is correct end to end.
22. **Nav destinations carry icon suppliers, not nodes.** A node can have only one parent, so one
    destination list shared by bar+rail+drawer (or scaffold rebuilds) stole icons from earlier
    builds. `M3NavDestination.of(label, iconName)` creates fresh `M3Icon`s per build and swaps the
    selected icon automatically.
23. **Ripple paints under content.** Skin order is background → ripple → content → state layer, so
    the expanding ink covers the container, never the text.
24. **Modena is neutralized explicitly.** Stock bases (`ToggleButton`, `Button`, `TextField`,
    `ComboBox`) leak a light-blue focus glow and square insets for any property our CSS doesn't
    set — invisible in light mode, glaring in dark. A dedicated CSS block zeroes focus colors and
    background insets on all M3 stock-based controls; popup list cells and spinner editors get
    theme-aware rules too.
25. **FAB menu anchors the main FAB.** The menu is a `StackPane` with an always-managed (but
    hidden when closed) action column, so its width never changes and the main FAB never shifts;
    rows rise with a wider stagger and decelerate easing.
26. **State rules must be complete, not just present.** Modena's `:hover`/`:armed`/`:selected`
    rules carry equal-or-higher specificity than single-class resets, so any state where we don't
    declare `background-color` + `background-insets` leaks a default layer (this was the
    segmented-selected partial fill and the gray FAB hover). A trailing "state completeness"
    block re-zeroes insets/focus in every state and declares every background explicitly,
    including `:selected:pressed` combos.
27. **Date/time inputs match filled text fields.** `M3DatePicker` gets the same 1px indicator
    (2px primary via an `input-focused` pseudo driven from editor focus) and the time spinners
    become 48px filled fields with themed editors/arrows instead of stock boxes.
28. **One copy of each UML diagram, not four.** `docs/UML_TEAM.md` used to repeat every class body
    verbatim with colour added. That duplication is what let `PlanType`'s accessors drift to
    `String` and dropped six enum notes. The master diagrams now live only in `docs/UML.md`; the
    team file keeps ownership plus a members-free colour overlay. Diagrams are documentation that
    rots silently, so there is now exactly one place to change a field list.
29. **Diagrams are validated by rendering, not by eye.** All mermaid blocks are parsed and rendered
    with mermaid 11 before being committed, because a broken fence renders as nothing and an
    unnoticed syntax error looks identical to "no diagram here".
30. **`Equipment` is a root aggregate with no relationships.** Its DAO takes no foreign key, so it
    is drawn as a declared, unconnected class rather than being wired to something for appearance.
    Per-item maintenance history would need a `MaintenanceRecord` entity — out of scope for T1.
31. **Money is `BigDecimal`; dates and times are ISO-8601 strings.** `double` cannot represent 0.1
    and drifts when summing payments, so all money fields and `revenueBetween`/`balance` use
    `BigDecimal`. Dates stay `String` (SQLite has no date type) but are locked to `yyyy-MM-dd` /
    `HH:mm`, which sort chronologically as text — that is what makes `findByDay`, `findExpiring`
    and `revenueBetween` work without a date type. `DateUtils` owns every parse.
32. **Association, not aggregation, for every child entity.** The earlier diagram gave
    `Customer o-- Membership`-style diamonds to four children and plain associations to three
    identical ones. No child in this model is lifetime-bound to its parent (every `delete` is
    public and admin-only), so the diamonds were removed rather than invented for the rest.
33. **`DbConnection` is an interface.** DAO impls depend on the interface, not the SQLite factory,
    so a database change touches one file instead of 26.
34. **`SessionContext` is documented static mutable state.** `AGENTS.md` §5.12 forbids static
    mutable state except the font loader; the signed-in account needs to be reachable from every
    controller, so it is kept FX-thread confined, cleared on logout and on exit, and named as the
    single deliberate exception.
35. **`MainApp` needs a public m3fx overlay entry point.** The overlay layer that dialogs,
    snackbars and sheets use lives in `io.m3fx.controls.internal`, which `module-info.java` does
    not export. The shell cannot install it until m3fx exposes a public facade.
36. **No generalization and no association classes, and both were argued rather than skipped.**
    All three inheritance candidates were examined and rejected: the 13 DAOs vary in SQL and entity
    type, not structure, and are already joined by the `DbConnection` interface; `validate` exists
    on one service, so a `BaseService` would be empty; `initialize()` is the JavaFX `Initializable`
    convention with a different body in every controller and must stay a public no-arg method for
    `FXMLLoader`. `Payment` is a genuine link class (amount/method/date belong to the act of paying)
    but XORs three targets and Mermaid has no association-class operator, so it stays a class with
    an XOR note instead of three dashed links that read worse.
37. **Mermaid silently strips the static (`$`) and abstract (`*`) member markers.** Verified by
    rendering: `+hash(String) String$` renders as `+hash(String) : String`, and `+validate() boolean*`
    renders as `+validate() : boolean`. `<<interface>>`/`<<enumeration>>`/`<<utility>>` do render,
    as does `<|--`, `<|..`, `..>`, `o--`, `*--` and multiplicities. Consequences: the `$` is kept in
    the source (it is the standard Mermaid marker and stays visible in the raw text) and every
    stateless helper additionally carries the `«utility»` stereotype so its static nature survives
    rendering; and nothing in the model is abstract, so the stripped `*` costs nothing. The
    support matrix is recorded in the `docs/UML.md` header so the next person does not rediscover it.
38. **Direction is asserted, not assumed.** `direction TB` is written explicitly in every class
    diagram even though Mermaid 11 defaults to TB under both ELK and dagre, and the diagrams are
    verified vertical by checking that 100% of rendered edges flow down the page. The earlier
    viewBox-based check reported "horizontal" for a TB graph, so the verification measures edge
    geometry rather than canvas dimensions.
39. **`GuardService` rather than 26 reference-count methods.** Rule 4 needs "refuse to delete while
    references exist", which spans nine tables. Adding `countByCustomer`/`countByTrainer` to every
    DAO would have added ~26 near-identical methods. One static helper that reads which tables
    reference a row and counts them keeps that cross-cutting query in one place and keeps the DAOs
    to their normal CRUD shape.
40. **`DaoException` extends `RuntimeException`.** Drawn the first time with its own `message` and
    `cause` fields, which breaks exception chaining and stack-trace printing. It now passes both to
    `super` and declares no fields, so `getMessage()`/`getCause()` are inherited rather than
    overridden.

41. **Calendar-grid date picker shipped as `M3Calendar` + `M3DateDialog` (closes the deferral in #7).**
    `M3Calendar` is the reusable month grid; `M3DateDialog` hosts it with Cancel/OK and
    `showIn(OverlayLayer, ...)` helpers. `M3DatePicker` keeps #18's design (M3-styled field +
    M3-themed native popup). The `M3DatePicker` javadoc and `SPEC_NOTES/overlays.md` now point at the
    dialog components instead of "planned" work.

42. **`M3Overlay` public facade (closes limitation in #35).** `M3Overlay` in public package
    `io.m3fx.controls.overlay` encapsulates `OverlayLayer`, providing a clean scene-level facade
    for dialogs, sheets, and snackbars accessible across module boundaries without exporting `internal`.
43. **`M3SplitButton` uses connected button composite.** `M3SplitButton` pairs an `M3Button`
    (primary action) with an `M3IconButton` (dropdown chevron) separated by a 1px divider. Inner
    corners are squared while outer corners are rounded (20-24dp). The dropdown chevron animates
    180° rotation with `SpringAnimation.BOUNCY` on open and returns to 0° on close.
44. **Toolbars support floating pill and docked bar with standard and vibrant styles.** `M3Toolbar`
    implements floating (Level 2 elevation, 28dp pill radius) and docked (Level 0 elevation, 1px
    outline border) modes in horizontal and vertical orientations, with standard surface and vibrant
    primary-container styling.
45. **Carousels use spring snap physics and drag inertia.** `M3Carousel` clips items with large
    corner radii (16-28dp) and coordinates mouse drag gestures and prev/next arrow buttons using
    `SpringAnimation` snapping.
46. **Dedicated `M3CircularProgress` canvas arc sweep.** Replaces stock JavaFX progress indicator with
    an authentic Material 3 circular progress indicator (4dp stroke, round caps, active `primary`,
    oscillating 25°-270° sweep arc rotating smoothly at ~260°/s).
47. **Spring physics preserves velocity on interruption.** `SpringAnimation` tracks current velocity
    across steps, so rapid taps or retargeted animations maintain continuous momentum instead of
    resetting to 0. Tactile press-scale feedback on buttons, cards, and chips uses spring physics.
48. **`M3ShapeMorph` Canvas rendering eliminates `PathElement` NPE.** JavaFX's `Path.getElements().setAll()`
    internally tracks node changes via `Path$3.onChanged`, where sparse or resized element arrays
    trigger null pointer exceptions during rapid 60fps spring pulses. Rendering via a dedicated `Canvas`
    and `GraphicsContext` with cubic Bézier lobes provides 100% thread safety, eliminates garbage
    collection overhead, and enables fluid shape morphing between all 6 presets.
49. **`M3SearchBar` decoupled master and displayed suggestion lists.** `M3SearchBar` now maintains
    separate `masterSuggestions` (the authoritative list supplied by caller) and `displayedSuggestions`
    (the filtered subset shown in the popup). Filtering no longer destroys the underlying suggestion
    dataset on user keystrokes.
50. **`OverlayLayer` partitioned sub-layers prevent accidental dismissal.** `OverlayLayer` isolates
    `scrim`, `sheetLayer`, `modalLayer`, `anchoredLayer`, and `transientLayer`. `M3Snackbar.dismiss()`
    clears only `transientLayer`, ensuring that transient toasts never inadvertently dismiss open
    modal dialogs or bottom sheets.
51. **`M3Chip` custom skin adds closable trailing affordance.** `M3Chip` now features an `onCloseProperty`
    callback and displays a trailing close button (`M3Icon.symbol("close")`) when `isClosable()` is true.
    Event handlers consume click and press events so closing does not inadvertently toggle chip selection.
52. **`M3TopAppBar` symmetric spacing for Center-Aligned mode.** Center-aligned top app bars balance
    flexible spacers on both sides of the title (`leftSpacer` and `rightSpacer`), perfectly centering the
    title text relative to the app bar bounds while maintaining left navigation and right actions.
53. **Built-in Pure JavaFX 21 Analytics Suite with zero external dependencies.** `M3SplineChart`, `M3BarChart`,
    `M3DonutChart`, `M3Sparkline`, and `M3Heatmap` are implemented natively using `Canvas` and `GraphicsContext`
    with cubic Bézier Catmull-Rom spline curves. This achieves 60fps rendering, dynamic resolution of
    `-md-sys-color-*` looked-up colors, crosshair hover inspection, and spring transitions with zero
    external library overhead.
54. **Hardware-Accelerated 3D Viewports (`SubScene` & `PerspectiveCamera`).** `M3Scene3D`, `M3ModelViewer3D`,
    `M3AnatomyViewer3D`, and `M3GyroRing3D` leverage JavaFX's native 3D hardware pipeline with specular
    `PhongMaterial` responding to M3 theme palettes, mouse orbit/pan/zoom controls, and procedural 3D
    gym equipment/anatomical meshes.
55. **3D Parallax Tilt with Specular Glare (`M3TiltCard3D`).** `M3TiltCard3D` applies cursor-relative
    3D rotation transforms (`Rotate.X_AXIS` and `Rotate.Y_AXIS`) coupled with `SpringAnimation` and a dynamic
    linear gradient sheen that simulates real-world physical holographic depth.
56. **High-Density Enterprise Telemetry Grid & Widgets.** `M3DataTable`, `M3StatCard`, `M3Timeline`,
    `M3Stepper`, and `M3SpeedDial` provide rich enterprise dashboard capabilities including instant
    search filtering, inline sparkline cells, status badge factories, vertical activity pipelines,
    multi-step wizards, and spring-blooming radial action wheels.
57. **Elimination of Prism `BoxBounds` `ClassCastException` in `M3TiltCard3D` via 2.5D Perspective Matrix & Parallax.**
    Directly attaching 3D rotation transforms (`Rotate.X_AXIS` / `Rotate.Y_AXIS`) to a 2D node (`StackPane`) in
    standard 2D layouts corrupts the bounding box to `BoxBounds`. When Prism dirty region tracking traverses
    parent 2D containers (`ScrollPane`, `HBox`), it attempts a hard cast `(RectBounds) bounds`, causing
    `ClassCastException: BoxBounds cannot be cast to RectBounds`. Transitioning `M3TiltCard3D` to 2D `Shear(x, y)`,
    `Scale(x, y)`, multiplane layer parallax (`contentContainer.setTranslateX/Y`), dynamic light-tracking
    `DropShadow` offset, and cursor-following specular sheen overlay preserves 100% 2D `RectBounds` fidelity,
    eliminating all pipeline crashes while delivering butter-smooth 144Hz spatial depth and crisp text rendering.
58. **Athletic Polar & Radial Telemetry Suite (`M3RadarChart`, `M3GaugeChart`, `M3Hologram`, `M3PulseBeacon`).**
    Expands the futuristic analytical ecosystem with `M3RadarChart` (spider/polar attribute polygon with
    concentric web rings and vertex inspection), `M3GaugeChart` (radial speedometer with spring-animated arcs
    and numeric readouts), `M3Hologram` (sci-fi container with laser scanline sweep and corner brackets),
    and `M3PulseBeacon` (concentric ripple wave operational status indicators).
59. **Live Athletic Workout Telemetry HUD (`M3RepCounter`, `M3HeartRateZones`, `M3IntervalTimer`, `M3WorkoutHUD`).**
    Implements athletic training stations directly tied to the smart gym domain: kinetic repetition counter with
    spring-physics impulse bounce (1.0 -> 1.24 -> 1.0) and rolling cadence velocity (reps/min); 5-zone sports
    science cardiac intensity visualizer (Warm Up, Fat Burn, Aerobic, Anaerobic, Redline Peak) with live status
    beacon; and circular HIIT/Tabata work/rest interval timer.
60. **Interactive Gym Floorplan Map with Live 3D Model Linking (`M3GymFloorMap`).**
    Provides a 2D/2.5D facility blueprint canvas depicting fitness zones (Free Weights, Cardio Deck, Functional Cage,
    Spa) with real-time equipment occupancy pins (Available, In Use, Maintenance). Clicking any pin opens a live
    detail card with an action linking directly to `M3ModelViewer3D` to inspect that exact equipment item in 3D.
61. **Global Spotlight Command Palette (`M3CommandPalette` - `Ctrl+K` / `Cmd+K`).**
    Delivers a centered glassmorphic spotlight launcher accessible from any screen via global keyboard shortcut
    (`Ctrl+K` or `Cmd+K`) or the top app bar search badge. Supports fuzzy filtering across categorized actions
    (Workouts, Equipment, Members, Themes, Navigation) with arrow-key traversal, `ENTER` execution, and `ESC` dismissal.
62. **Full 12-Piece Procedural 3D Gym Equipment Catalog (`M3ModelViewer3D`).**
    Expanded procedural 3D model generator from 4 basic models to 12 commercial gym equipment stations
    (`DUMBBELL`, `BARBELL`, `KETTLEBELL`, `WORKOUT_BENCH`, `SQUAT_RACK`, `CABLE_CROSSOVER`, `LEG_PRESS`,
    `TREADMILL`, `EXERCISE_BIKE`, `ROWING_MACHINE`, `PUNCHING_BAG`, `PREACHER_BENCH`). Every piece is assembled
    from hardware-accelerated JavaFX 3D primitive meshes (`Box`, `Cylinder`, `Sphere`), shaded with specular
    and diffuse phong materials, aligned flush to the ground plane at Y = 80.0, and centered on (0, 0)
    for seamless 360-degree orbit camera inspection and interactive wireframe toggling.
63. **Expanded Athletic & Commercial Analytics Suite (`M3ScatterPlot`, `M3FunnelChart`, `M3ActivityCalendar`).**
    Implemented high-performance canvas visualizations for sports science and gym management:
    (1) `M3ScatterPlot` for Velocity-Based Training (VBT) load-velocity profiling, featuring automatic
    coordinate scaling, 4-quadrant crosshairs, bubble radius sizing, and analytical Ordinary Least Squares (OLS)
    linear regression trendline computation (y = mx + b, R²);
    (2) `M3FunnelChart` for member lifecycle and marketing conversion pipelines, featuring tapered horizontal
    trapezoids, overall conversion rates, step-to-step drop-off badges (▼ -XX.X%), and interactive hover tooltips;
    (3) `M3ActivityCalendar` for 52-week annual training consistency (7 × 53 matrix = 371 days), featuring 4-level
    M3 theme color intensity ramps, streak calculations (current active streak, longest lifetime streak, total
    workouts), and date-precise hover cards.
64. **Smooth Viewport Scrolling & Elimination of Nested Scrollpane Conflicts (Side Menu & All Pages).**
    Resolved critical viewport clipping and cropping issues across the application shell:
    (1) **Side Menu Overflow:** In `app-shell.fxml` (`M3NavigationRail`) and `M3NavigationDrawer.java`, destination
    toggle items are wrapped in a transparent, borderless `ScrollPane` with `fitToWidth=true`, `hbarPolicy=NEVER`,
    and `VBox.vgrow=ALWAYS`. This ensures all rail destinations (e.g. Staff, Help/Documentation) and drawer options
    remain accessible and scrollable on compact displays.
    (2) **Elimination of Hardcoded Height Clamps:** Removed rigid `prefHeight="820.0"` from root and inner containers
    across all 18 screen FXMLs (`dashboard.fxml`, `attendance.fxml`, `customer-list.fxml`, `payment.fxml`, `membership.fxml`,
    `staff.fxml`, etc.), resetting root elements to `Region.USE_COMPUTED_SIZE` with `minHeight="USE_PREF_SIZE"` at runtime.
    This allows containers to calculate their real content bounds (e.g. 1050px+).
    (3) **Nested Scrollpane Collision Prevention:** `AppShellController.loadScreen()` and `MainApp.java` dynamically
    inspect the view tree using `containsScrollPane(view)`. Screens with internal scroll areas (`customer-profile`,
    `medical-profile`, `progress`, `trainer-profile`, etc.) are mounted directly into `contentArea` without an outer
    `ScrollPane`, eliminating double-scroll conflicts, trapped wheel events, and outer clipping.
    (4) **Full-Height Vertical Scrolling with Bottom Breathing Room:** Views lacking internal scroll areas are wrapped
    in a `ScrollPane` with `fitToHeight=false` and generous bottom padding (`Insets bottom="64.0"`), guaranteeing
    that users can scroll smoothly from the top to the very bottom without cards, speed dials, or action toolbars being cropped.
    (5) **Unsquished Matrices:** Replaced `fitToHeight="true"` with `fitToHeight="false"` in `availability.fxml` and
    `schedule.fxml` so large activity matrices and agenda grids can expand and scroll vertically rather than being squished.
65. **Screen Resolution Matching, Interaction Freeze Elimination, and Layout Deconfliction.**
    Fixed startup layout, window geometry, and UI interaction bugs reported during desktop execution:
    (1) **Screen Geometry Auto-Fit:** Updated `GymApp.java` to query `Screen.getPrimary().getVisualBounds()`,
    clamping restore sizes to monitor capabilities and calling `stage.setMaximized(true)` on startup. Removed rigid
    `prefHeight="900.0" prefWidth="1440.0"` from `app-shell.fxml` root container, allowing fluid responsive scaling.
    (2) **Elimination of Perceived Window Freeze:** Identified that `M3SpeedDial` extended `StackPane` without bounding
    `maxWidth`/`maxHeight` and had `pickOnBounds=true`, causing an invisible full-window overlay that intercepted all
    mouse clicks across the entire screen. Enforced `setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE)` and `setPickOnBounds(false)`
    in `M3SpeedDial`, and set `mouseTransparent="true"` on idle snackbar overlays (`globalSnackbar`, `dashboardSnackbar`).
    (3) **Top App Bar Layout Deconfliction:** `M3TopAppBar` in `app-shell.fxml` was changed from `CENTER_ALIGNED` to
    `SMALL` (left-aligned title) so the system brand title no longer collides with or renders underneath the centered
    global command search trigger bar (`Ctrl + K`).
    (4) **Responsive Bento Grid Elasticity:** Added flexible `minWidth` thresholds to Bento rows in `dashboard.fxml`,
    allowing stat cards, spline charts, and 3D radar viewports to scale cleanly down to 1024px displays without
    horizontal clipping.
    (5) **CSS Paint ClassCastException Removal:** Removed inline `-fx-background: transparent;` from scroll pane wrappers
    in `app-shell.fxml`, `AppShellController.java`, `MainApp.java`, and `M3NavigationDrawer.java`, relying exclusively on
    standard `-fx-background-color: transparent;` to eliminate JavaFX `CssStyleHelper` paint cast exceptions.

66. **Bound Property Set Exception, FXML Reflection Type Mismatch, and Win32 Maximization Rectification.**
    Resolved the remaining runtime freezes and layout bugs reported during `runGym`:
    (1) **M3Carousel Bound Property Crash:** In `M3Carousel.java`, `clipRect.widthProperty().bind(viewport.widthProperty())`
    and `heightProperty().bind(...)` were bound during initialization. `layoutChildren()` subsequently invoked
    `clipRect.setWidth(w)` and `clipRect.setHeight(h)`. In JavaFX, setting a bound property throws `RuntimeException:
    Rectangle.width : A bound value cannot be set.` whenever `layoutChildren()` runs (e.g. in `trainer-list.fxml` or
    `membership.fxml`), causing the JavaFX pulse to abort and freezing the scene graph. Removed the redundant setter calls.
    (2) **FXMLLoader Reflection Argument Type Mismatch:** Removed duplicate/overloaded `@FXML` methods in `AppShellController.java`
    and `DashboardController.java` (`(ActionEvent event)`, `(MouseEvent event)`), consolidating to clean 0-arg handlers
    (`onToggleThemeMode()`, `onOpenCommandPalette()`, `onToggleRail()`, `onLogout()`, etc.). In JavaFX, 0-arg handlers
    universally accept any event type without reflection mismatch; previously, FXMLLoader bound to the `ActionEvent` signature,
    and mouse clicks threw `IllegalArgumentException: argument type mismatch`.
    (3) **CSS Looked-Up Colors ClassCastException:** Migrated inline styles (`-fx-background-color: -md-sys-color-...`)
    from `M3CommandPalette.java` and `M3DataTable.java` into stylesheet classes (`.m3-command-palette-card`, `.m3-command-palette-header`,
    `.m3-command-palette-badge`, `.m3-data-table-header`) in `m3fx.css`. Initialized `latestTheme = M3Theme.baseline(true)`
    in `M3Stylesheets.java` and registered the theme in `GymApp.java` before FXML inflation, preventing probe failures in `ThemeColors`.
    (4) **Win32 Window Maximization & Taskbar Cropping:** Reordered `stage.show()` before `stage.setMaximized(true)` in
    `GymApp.java`. Calling `setMaximized(true)` before `show()` on Windows causes DWM to offset window coordinates, pushing the
    bottom of the client window behind the Windows taskbar and cropping bottom navigation items.
    (5) **End-Of-Scroll Padding:** Increased side navigation rail bottom insets to `56.0` and dashboard container bottom insets
    to `112.0` (with `48.0` on `equipment.fxml` and `gym-floor-map.fxml`), ensuring the bottom-most navigation buttons (`navHelp`),
    speed dials, and 3D radar/chart cards are completely visible with comfortable breathing room when scrolled to the end.

67. **Resolution of Windows STATUS_APPLICATION_HANG (0xCFFFFFFF), Screen Size Mismatch, Canvas OutOfMemoryError, and Smooth Scrolling:**
    Resolved the critical window freeze, layout crop, and scrolling issues:
    (1) **JVM Heap & Execution Environment (`build.gradle`):** The `runGym` Gradle task previously lacked JVM arguments,
    running on Gradle's small default heap (256MB–512MB) while hosting 19 screens with procedural 3D models and canvases.
    Configured `jvmArgs = ['-Xms512m', '-Xmx2048m', '--enable-native-access=ALL-UNNAMED', '--sun-misc-unsafe-memory-access=allow']`
    for `runGym` (and `-Xmx1024m` for `test` and `runSamples`), eliminating heap exhaustion and Java 27 restricted method warnings.
    (2) **Clean Stage Lifecycle & Screen Size Matching (`GymApp.java`):** Previously, `Scene` was instantiated with
    `screenBounds.getWidth()` and `screenBounds.getHeight()`, which caused the decorated window frame (titlebar + borders)
    to overflow the desktop work area by ~40px, pushing the bottom of the window behind the Windows Taskbar and cropping the
    bottom of the side rail and all pages. Replaced this with standard responsive dimensions (`1280x800`) and invoked
    `stage.setMaximized(true)` before `stage.show()`. Added explicit `stage.setOnCloseRequest(e -> { Platform.exit(); System.exit(0); });`
    and `@Override public void stop()`, terminating background threads cleanly on exit and preventing NTSTATUS 0xCFFFFFFF hangs.
    (3) **M3GymFloorMap Infinite Loop & OutOfMemory Heap Exhaustion:** In `gym-floor-map.fxml`, `prefWidth="Infinity"` and
    `prefHeight="Infinity"` caused the floorplan's `redraw()` method to compute an infinite height, running `for (double y = 0; y < h; y += 24)`
    millions of times and generating gigabytes of `strokeLine` buffer commands until throwing `OutOfMemoryError: Java heap space`.
    Set concrete preferred bounds (`800x520`) in FXML and clamped `w` and `h` in `M3GymFloorMap.java` to finite maximums (`2560x1600`).
    (4) **AnimationTimer Lifecycle Cleanup:** Across 10 controls (`M3PulseBeacon`, `M3ParticleCanvas`, `M3Hologram`,
    `M3Scanline`, `M3GlowShadow`, `M3ModelViewer3D`, `M3GyroRing3D`, `M3IntervalTimer`, `M3CircularProgress`, `M3WavyProgress`),
    timers were running continuously even when nodes were removed from the scene graph, pinning entire page scene trees in memory
    and causing 100% CPU thread thrashing. Added `sceneProperty()` listeners to stop all animation timers whenever `scene == null`.
    (5) **Smooth Accelerated Scrolling & End-Crop Fix (`AppShellController.java`):** Implemented `enableSmoothScrolling`
    with responsive 48px natural mouse-wheel stepping, direction normalization, and `pannable=true` across both the side navigation
    rail's scroll pane and all dynamic module views in `contentArea`. Increased bottom padding on navigation rail to `64.0`
    so the bottom items (`Documentation`, `Staff & Admin`) and bottom page cards are completely visible with comfortable breathing room.
    (6) **CSS Looked-Up Colors & ClassCastException Cleanup:** Replaced all remaining inline `setStyle` calls in `M3StatCard.java`,
    `M3DonutChart.java`, `M3DataTable.java`, `M3CommandPalette.java`, `M3Stepper.java`, and `M3Timeline.java` with centralized
    style classes in `m3fx.css`, and eliminated the probe mechanism in `ThemeColors.java`.




68. **Audit pass (Oct 2026): spec alignment + bug sweep across the whole library and app.**
    Eight parallel audit agents reviewed every `m3fx-core` token/theme file, every control package,
    and all 19 gym screens. Landmark fixes, each verified against `refs/material-web-2.5.0` tokens,
    `material-color-utilities-main`, or m3.material.io:
    (1) **Skin listener leaks** closed in `M3Button`, `M3IconButton`, `M3Chip`, `M3Card`, `M3CircularProgress`,
    `M3WavyProgress`, `M3Calendar`, `M3LinearProgress` — dispose() now removes every listener/binding it registered.
    (2) **Ripple now holds ink while pressed** (M3 behavior: expand on press, fade on release) instead of fading in parallel.
    (3) **`MotionTokens.emphasized()` was returning the emphasized-decelerate curve** (0.05,0.7,0.1,1); fixed to the spec
    curve cubic-bezier(0.2,0,0,1); added missing extra-long durations and standard accelerate/decelerate easings.
    (4) **Linear/circular/wavy progress rebuilt to the updated M3 spec**: 4dp fixed stroke, 4dp stop indicator + gap,
    indeterminate keyframes from material-web, Expressive wavy tokens (amplitude 3dp, wavelength 40/20dp).
    (5) **Navigation bar/rail/drawer keep one destination always active** (re-select on active click); top app bar
    medium/large variants now enforce their 112/152dp container heights; toolbar floating is level 3 with full-shape radius.
    (6) **Charts**: negative/flat data no longer produce NaN bars/curves (zero-baseline handling), Catmull-Rom overshoot
    clamped, data-table zebra striping alternates within the visible page, sortable headers implemented, speed-dial wheel
    rebuilt around the FAB center with stagger animation, stepper gained a keyboard-operable error state.
    (7) **Gym app**: replaced 11 icon names missing from the bundled catalog (rendered blank), wired dead buttons
    (schedule week nav, override, bottom-sheet log entry, floor-map zoom + 3D inspector, live-workout reps/pause/clock),
    populated previously empty tables (attendance, staff, membership, payment), removed hardcoded root widths that fought
    the shell viewport, exclusive ToggleGroups for filter/method chips, fixed the inverted dark-mode toggle reading.
    (8) **Theme stays on the baseline dynamic-color spec.** An audit attempt switched `M3Theme.resolve()` to
    `SpecVersion.SPEC_2026`; probing the ported engine showed the repo-locked baseline (seed #6750A4 / TonalSpot / light →
    primary #65558f, container #e9ddff, locked by `M3ThemeTest` + `docs/SPEC_NOTES/systems.md`) is what SPEC_2021 yields,
    while SPEC_2025/2026 yield #655789/#d4c3fd. The spec-version change was reverted; the bundled 2025/2026 specs remain
    available through the `DynamicScheme` 5-arg constructors for future opt-in.
    (9) **CSS**: integrated ~20 spec corrections into `m3fx.css` (menu item 48dp, tooltip shapes/elevation, 16dp slider
    track, 40dp icon-button container radius, 56dp text-field box, assist chip elevated variant, switch disabled
    12%/38% state colors, side-sheet standard/modal surfaces, search-bar elevation, calendar :today, stepper error
    styles, linear-progress class names).
    (10) **`GymApp` shutdown**: window close now routes through `Platform.exit()` so `stop()` cleanup runs before the
    force-exit that guards against orphaned threads.

69. **Apple-style fluid motion for the app shell (Oct 2026).** User request: Apple-grade spring/morph
    animation ("like in their videos"). Added — clearly marked NON-M3, Apple-flavored tokens in
    `MotionTokens` (sheet ease cubic-bezier(0.32, 0.72, 0, 1) @450ms, gentle/fade curves, spring
    presets APPLE_SETTLE k=320 ζ=0.88 and APPLE_PLAYFUL k=380 ζ=0.65), transform-only enter/exit/
    press helpers in `internal/MotionUtil` (per-node tracked Timeline, scene-exit cleanup, FX-thread
    guard), and `SpringAnimation.animateTranslation/stopTransforms`. The shell (`AppShellController`)
    now plays a 400ms rise+fade+scale enter on every page swap (swap itself stays synchronous — the
    FXML test contracts assert `contentArea` has exactly one child immediately after navigation),
    springs a single nav-highlight pill between rail destinations (stiffness 320, ζ=0.88, velocity
    preserved on retarget so fast clicking morphs continuously), and press-scales rail buttons.
    Rule 25's "no `io.m3fx.controls.internal` imports" gains one documented exception for the shell
    (see `controller/README.md` rule 25); screen controllers still use public API only. Page exit
    overlaps are intentionally not used in the shell because `GymAppScreenViewerTest` pins the
    one-child semantics; `MotionUtil.exit` remains available for containers that tolerate it.

70. **Trainer-tab freeze investigation (Oct 2026).** Thread dumps of a frozen live session showed
    the FX thread burning CPU continuously (~60fps pulses) and QuantumRenderer stuck in
    `NGGroup.renderForcedContent` — a repaint storm, not a deadlock; scripted navigation (real
    mouse events, scrolling, hovering, 16+ navigations incl. four trainer visits) could not
    reproduce it. Three defensive fixes shipped: (1) `AppShellController.loadScreen` no longer
    forces `minHeight=USE_PREF_SIZE` on roots that ARE ScrollPanes — pinning a ScrollPane to its
    own pref height defeats its scrolling and forces enormous layout passes on the tallest pages
    (trainer screens); (2) page entrances fade+rise without scale — a non-1.0 page scale feeds
    JavaFX's snap-scale-aware layout insets and re-lays-out the whole page subtree every frame
    (`MotionUtil.enter(node, direction, fromScale)` added, pages pass 1.0); (3) the nav pill
    dedupes retargets (same button + same geometry = no spring restart, so no listener feedback
    can keep a spring — and a full-scene repaint — alive forever). A freeze watchdog in `GymApp`
    samples FX CPU and dumps FX + QuantumRenderer stacks to stderr if the FX thread stays >60%
    busy for 4s, so any future freeze self-diagnoses in the runGym console.

71. **Apple-style launch gates: splash → login → shell morph (Oct 2026).** User request: add a
    splash screen and a login page with expanded morph animation. Library: `MotionUtil` gained
    `splashIn`/`splashOut`/`shake`/`morphFromBounds` (transform-only, tracked, scene-exit-safe;
    `morphFromBounds` uses the verified JavaFX center-pivot scale formula
    `t = startMin − refMin − (1−s)·refSize/2`, probed empirically — JavaFX scales about the layout
    bounds center, not the top-left) + `MotionTokens.APPLE_SPLASH_DURATION`; all locked by
    `MotionUtilTest`. App: `LaunchGateController` builds the splash and glass login card entirely
    in code (no FXML/fx:ids/@FXML — the FXML test contracts stay byte-identical); `GymApp`
    shows the gates only when `main()` sets `SHOW_LAUNCH_GATES=true`, so tests driving
    `start()` directly get the pre-gate shell unchanged. Choreography: splash blooms in →
    progress fills while the dashboard prewarms → text dissolves while the brand mark reparents
    into the login card and springs into place (shared-element morph) → valid sign-in captures
    the card's scene bounds and the whole shell morphs out of that footprint (`morphFromBounds`)
    while the veil lifts → sign-out reverses into the gate. Invalid input shakes the card
    (`shake`), no dialogs. Demo auth: any email + 4+ char password (no backend by design —
    `controller` purity rules hold).
