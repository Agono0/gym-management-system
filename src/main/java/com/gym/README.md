# `com.gym` — application entry points (JavaFX branch)

> **Status note.** This package hosts the three JavaFX launcher classes plus the Java-built UI
> runtime spine (`Screen`, `ScreenController`, `AppShellController`, `LaunchGateController`), alongside
> the companion **Terminal Workflow Console (`com.gym.cli`, `GymCliApp`)** added per `docs/DECISIONS.md` 78.
> The Milestone 2 backend packages (`model/`, `dao/`, `util/`, `exception/`) are shipped with SQLite
> persistence (`gym.db`) and binary serialization (`SerializationUtil`). The separate service layer
> (`service/`) and app tests (`src/test/`) were removed per `docs/DECISIONS.md` 77 to fit COMP 301
> Lectures 1–6 beginner scope. Wiring the JavaFX UI controllers to the live DAOs is **Milestone 3**.
>
> **Signature authority:** `docs/UML.md` owns the domain model, DAOs, and runtime spine; the
> launcher and shell details below match the shipped code (`GymApp.java`, `MainApp.java`,
> `M3Samples.java`, `cli/GymCliApp.java`, `controller/Screen.java`, `controller/AppShellController.java`,
> `controller/LaunchGateController.java`).

## 1. Purpose — the problem this package solves

`com.gym` contains the entry classes the JVM and JavaFX start, plus the top-level windows the
application owns:

| Class | Lines | What it is |
|---|---|---|
| `MainApp.java` | ~2 100 | The **m3fx component gallery**: an interactive 17-page Material Design 3 / Expressive showcase, plus a viewer for all 19 Java-built app screens (`Screen.values()`) and a button that launches the full app shell |
| `M3Samples.java` | 9 | A convenience launcher alias (`extends MainApp`, delegates to `MainApp.main`) |
| `GymApp.java` | 166 | The **app entry point**: registers the M3 theme, constructs `AppShellController`, maximizes the window, starts a daemon freeze watchdog, and (when `main()` sets `SHOW_LAUNCH_GATES`) runs splash → login → shell launch gates |

Everything visually inside the shell lives in `controller/` (`Screen`, `ScreenController`,
`AppShellController`, `LaunchGateController`, and 18 screen controllers). The Milestone 2 backend
(`model/`, `dao/`, `util/`, `exception/`) is implemented and waiting for **Milestone 3**, when
controllers and startup will call `DatabaseConnection.initializeDatabase()` and concrete `*Dao`
classes. Today, launchers and controllers deliberately do **not** read or write SQLite rows at startup.

**What breaks if the launchers are done wrong.** JavaFX has one `start(Stage)` per `Application`.
If the theme is applied after controls are built or shown, controls flash unstyled; if closing one
window calls `System.exit(...)`, a co-open gallery window dies in the same JVM (which is why
`GymApp.stop()` calls `Platform.exit()` only); and if a UI freeze has no diagnostics, it hangs
silently without console stack traces.

**What this package deliberately does not do.** No SQL, no business validation, no FXML loading.
`GymApp` creates `AppShellController` and passes the `Stage` lifecycle over to JavaFX.

## 2. What you learn here (OOP concepts)

| Concept | Where | Why the project needs it | How to apply it (one line) |
|---|---|---|---|
| Inheritance from a framework class | `MainApp extends Application`, `GymApp extends Application` | JavaFX invokes *your* `start(Stage)` and `stop()` hooks | Override lifecycle hooks; call `launch(args)` instead of `start` |
| Lifecycle hooks | `start(Stage)` → `stop()` | Clear setup vs teardown points for windows and threads | Apply theme and build UI in `start`; clean exit in `stop` |
| Static entry point | `public static void main(String[] args)` → `launch(args)` | JVM needs `main`; JavaFX starts the toolkit via `launch` | Keep `main` to flag setup and one `launch(args)` call |
| Composition root (UI) | `GymApp.start` creates `M3Theme`, `AppShellController`, and `Scene` | One class picks the theme, variant, and initial shell | Configure shared setup at the top, not inside individual screens |
| Encapsulation of window state | `Stage` configuration stays in `GymApp` / `MainApp` | Screens belong inside the shell workspace, not owning OS windows | Keep `Stage` out of screen controllers |
| Guarded static switch | `GymApp.SHOW_LAUNCH_GATES` | Programmatic/direct calls to `start(Stage)` show the plain shell; `main()` enables the splash/login gates (`DECISIONS.md` 71) | Keep one documented switch written only in `main()` |
| Daemon worker threads | `GymApp.startFreezeWatchdog()` | Monitoring thread must never prevent JVM exit when windows close | Call `setDaemon(true)` and keep reads off the scene graph |
| Constants | `SEED_PURPLE = Color.web("#6750A4")` | Shared purple seed reused across `GymApp` and `MainApp` presets | Name shared constants once in `public static final` form |

## 3. Design patterns in this layer

| Pattern | Where | Why | What goes wrong if used carelessly |
|---|---|---|---|
| **Composition root** | `GymApp.start(Stage)` | One place knows the theme, scene, watchdog, and shell root | Screens inventing their own themes create mismatched palettes |
| **Facade over the toolkit** | `M3Stylesheets.applyTo(scene, theme)`, `M3CommandPalette.attachGlobalShortcut(...)` | Application code asks for a themed scene once instead of wiring CSS internals | Duplicating stylesheet setup per screen causes cascade conflicts |
| **Template method** (framework-defined) | `Application.start` / `Application.stop` | JavaFX controls startup threading and shutdown order | Blocking I/O inside `start` stalls the initial frame |
| **Watchdog** | `startFreezeWatchdog()` | Samples FX thread CPU time; prints stack traces if busy > 60% for 4 s | Touching scene nodes from a background thread causes FX exceptions or freezes |
| **Singleton — avoided** | No global `App.getInstance()` | Hidden globals tightly couple screens and survive across runs | Wire dependencies explicitly when Milestone 3 connects DAOs |

## 4. Rules you must follow (and the bug each rule prevents)

1. **All scene-graph work happens on the FX application thread.** `Platform.runLater` is the only
   bridge from background code.
2. **No `System.exit` from this package.** Opening `GymApp` from `MainApp`'s gallery shares one JVM;
   calling `System.exit` closes both windows abruptly (`GymApp.stop()` uses `Platform.exit()`).
3. **Register theme before constructing the UI root.** `GymApp` builds `M3Theme.fromSeed(SEED_PURPLE,
   ThemeVariant.TONAL_SPOT, true)` and calls `M3Stylesheets.setLatestTheme(theme)` *before* `new
   AppShellController()`, so custom M3 controls resolve theme colors during construction.
4. **Maximize before `show()`** on Windows (`stage.setMaximized(true)` before `stage.show()`), so
   the OS computes work-area bounds cleanly.
5. **`SHOW_LAUNCH_GATES` stays a `main()`-only switch.** Calling `start(Stage)` directly skips launch
   gates unless `main()` enabled them (`DECISIONS.md` 71).
6. **The freeze watchdog never touches the scene graph.** It reads CPU metrics and thread stack
   traces only and stays `setDaemon(true)`.
7. **Database startup belongs at composition boundaries (Milestone 3).** `DatabaseConnection.initializeDatabase()`
   creates SQLite tables once when Milestone 3 activates live DAO persistence, never inside draw loops.

## 5. The files in this package

### `MainApp.java` — the m3fx gallery (entry point of `gradlew run`)

**Status:** implemented (~2 100 lines). **What it is.** `MainApp extends Application` builds the
component showcase directly in Java code:

- `M3Overlay` overlay layer, `M3CommandPalette` (`Ctrl/Cmd+K`), and an `M3NavigationDrawer` with
  17 destinations covering M3 tokens, controls, pickers, overlays, spring physics, charts, 3D viewers,
  effects, live workout HUD, gym floorplan, and **"Gym App Screens (19 Java-Built)"**.
- Top theme strip with seed preset dots (`GymApp.SEED_PURPLE` plus 5 presets), `ColorPicker`,
  `ThemeVariant` selector, contrast selector, and physics toggle.
- The "Gym App Screens (19 Java-Built)" page iterates `Screen.values()`, instantiates any screen via
  `screen.create().getRoot()`, and offers buttons to launch `new GymApp().start(new Stage())` or a
  standalone `Screen.LOGIN` window.
- Scene `1220×840`, themed via `M3Stylesheets.applyTo`, title `"m3fx — Material Design 3 & Expressive Showcase"`.

### `M3Samples.java` — the gallery alias

**Status:** implemented (9 lines). `M3Samples extends MainApp` delegating `main(String[] args)` to
`MainApp.main(args)` (target of `./gradlew runSamples`).

### `GymApp.java` — the app shell launcher (entry point of `gradlew runGym`)

**Status:** implemented (166 lines). **What it does, in order** (`start(Stage)`):

1. Creates `M3Theme.fromSeed(SEED_PURPLE, ThemeVariant.TONAL_SPOT, true)` and registers it via
   `M3Stylesheets.setLatestTheme(theme)`.
2. Constructs `AppShellController shellController = new AppShellController()` and gets
   `Parent root = shellController.getRoot()` (pure Java view — zero FXML loaded).
3. Creates `new Scene(root, 1280, 800)` and calls `M3Stylesheets.applyTo(scene, theme)`; title
   `"GYM-OS // Enterprise Gym Management System (2026)"`; min size `1024×700`.
4. Sets `setOnCloseRequest(e -> Platform.exit())`, `setMaximized(true)`, `stage.show()`, and
   `startFreezeWatchdog()`.
5. If `SHOW_LAUNCH_GATES` is `true` (set by `main()`), calls `shellController.beginLaunchGates()` for
   the splash → login → shell morph choreography (`DECISIONS.md` 71; demo login accepts any email +
   4+ character password until Milestone 3).

**The freeze watchdog** (`startFreezeWatchdog`): daemon thread sampling JavaFX Application Thread CPU
via `ThreadMXBean`; dumps FX and `QuantumRenderer-0` stacks to stderr (at most once every 30 s) if
busy above 60% for 4 consecutive seconds (`DECISIONS.md` 70).

**`stop()`**: `super.stop(); Platform.exit();` — cleanup without `System.exit` (rule 2).

### The runtime spine this package starts

`GymApp` → `AppShellController` → `Screen` (`byKey`, `create()`) → `ScreenController` (`getRoot()`)
→ 18 screen controllers + `LaunchGateController`. See [`controller/README.md`](controller/README.md).

## 6. How this package connects to the others

| Class | Calls / starts | Called by |
|---|---|---|
| `MainApp` | m3fx gallery pages, `M3Stylesheets`, `M3Overlay`, `M3CommandPalette`, `Screen.values()`, `new GymApp()` | JVM (`main`), Gradle `run` task |
| `GymApp` | `AppShellController`, `LaunchGateController`, `M3Stylesheets` | JVM (`main`), Gradle `runGym` task, `MainApp` shell launch button |
| `M3Samples` | `MainApp.main` | JVM, Gradle `runSamples` task |
| `cli.GymCliApp` | Interactive CMD menus (`com.gym.cli.*`), `GymRulesHelper`, `SampleDataSeeder`, all 13 `com.gym.dao.*`, `SerializationUtil` | JVM (`main`), Gradle `runCli` task, `run-cli.bat` (`cli/README.md`) |

**Milestone 3 planned wiring:** At startup (or in `GymApp.start`), calling
`DatabaseConnection.initializeDatabase()` prepares `gym.db`, and screen controllers instantiate and call
concrete `com.gym.dao.*` classes cleanly wrapped in `ValidationException` / `DatabaseException`
handlers.

## 7. Common mistakes and review checklist

| Symptom | Cause | Fix |
|---|---|---|
| Second window closes unexpectedly | `System.exit` added inside `stop()` or a handler | Use `Platform.exit()` and let the launcher finish JVM shutdown |
| Gallery or shell shows unstyled controls briefly | `setLatestTheme` / `applyTo` called too late | Register `M3Stylesheets.setLatestTheme(theme)` before building controllers and `applyTo(scene, theme)` before `show()` |
| Splash/login never shows on `./gradlew runGym` | `SHOW_LAUNCH_GATES` accidentally removed from `main()` | Keep `SHOW_LAUNCH_GATES = true` in `GymApp.main` |
| JVM lingers after window closes | Non-daemon background thread | Always call `setDaemon(true)` on utility/watchdog threads |
| App screen changes missing in `GymApp` | Edited a legacy `.fxml` instead of Java controller | Edit the Java controller in `com.gym.controller`; `.fxml` files are legacy artifacts |

Checklist for a change here:

- [ ] Only bootstrap/gallery concerns touched (theme, scene, window, shell instantiation, gates).
- [ ] No `System.exit`; watchdog stays daemon and read-only.
- [ ] Theme registered before controller construction and applied before `show()`.
- [ ] `./gradlew build` passes cleanly (`run` and `runGym` launch cleanly).

## 8. Learning exercises (with answers)

1. **Trace the launch.** List the five main steps `GymApp.start(Stage)` executes before showing the
   launch gates, and explain why `M3Stylesheets.setLatestTheme(theme)` comes first.
2. **Explain `SHOW_LAUNCH_GATES`.** Why does `GymApp` keep a static boolean set inside `main()` instead
   of showing launch gates unconditionally in `start(Stage)`?
3. **Daemon thread purpose.** Why does `startFreezeWatchdog()` call `watchdog.setDaemon(true)` before
   `watchdog.start()`?
4. **Single-JVM launch.** Inside `MainApp`, clicking "Launch Full Application Shell Window" calls
   `new GymApp().start(new Stage())`. What bug would appear if `GymApp.stop()` called `System.exit(0)`?
5. **Milestone 3 connection.** Look at `DatabaseConnection.initializeDatabase()` in `com.gym.util`.
   Where should startup database table creation happen once Milestone 3 begins, and which checked
   exception must be caught?

<details>
<summary>Answers</summary>

1. Create + register `M3Theme` (`setLatestTheme`) → construct `new AppShellController()` and fetch
   `getRoot()` → build `Scene(root, 1280, 800)` + `M3Stylesheets.applyTo` → configure stage title,
   min dimensions, close request, maximize → `stage.show()` + `startFreezeWatchdog()`. Theme comes
   first so custom M3 controls resolve theme colors as `AppShellController` builds them.
2. Launching via `main()` sets `SHOW_LAUNCH_GATES = true` for the real user flow (splash + login),
   while direct programmatic invocations of `start(Stage)` receive the immediate shell without a
   blocking overlay.
3. Non-daemon threads block JVM termination even after every JavaFX `Stage` closes. Setting daemon
   `true` ensures the watchdog dies cleanly when the application exits.
4. Both `MainApp` and `GymApp` share the same JVM process; `System.exit(0)` would force-terminate the
   entire VM and kill the user's open gallery window.
5. Call `DatabaseConnection.initializeDatabase()` once before screens query tables (in bootstrap /
   startup), catching `DatabaseException` cleanly with a readable message instead of crashing silently.

</details>

## 9. Ownership and tasks

| File | Responsibility | Owner | Status |
|---|---|---|---|
| `MainApp.java` | m3fx gallery: pages, theme bar, command palette, 19-screen viewer | Abdelrhman | Done |
| `M3Samples.java` | Gallery alias launcher | Abdelrhman | Done |
| `GymApp.java` | App shell launcher: theme, window, watchdog, launch gates | Abdelrhman | Done |
| Startup `DatabaseConnection.initializeDatabase()` call (Milestone 3) | Abdelrhman | Planned — Milestone 3 |

### Definition of done for a change in this package

- [ ] `./gradlew build` passes with zero errors (`./gradlew run` opens `MainApp`, `./gradlew runGym` opens `GymApp`).
- [ ] Window closes cleanly without leaving background threads alive and without calling `System.exit`.
- [ ] Theme applies from the very first visible frame.
- [ ] Documentation matches actual code in `src/main/java/com/gym/`.
