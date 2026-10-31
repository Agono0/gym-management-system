# `com.gym` — application entry points (JavaFX branch)

> **Status note.** This package is **shipped code** — three launcher classes plus the runtime
> spine they create. The *backend wiring* that the older version of this README sketched
> (`SqliteDbConnection`, DAO construction, session bootstrap in `stop()`) is **planned, not
> built**: no `model/`, `dao/`, `service/` or `util/` class exists yet, and the SQLite-JDBC
> dependency has been removed from `build.gradle` until the DAO layer lands. The planned startup
> flow lives in `docs/UML.md` §2; this README documents what actually runs.
>
> **Signature authority:** `docs/UML.md` declares the planned domain classes; the launcher and
> shell classes below are described from the shipped code (`GymApp.java`, `MainApp.java`,
> `M3Samples.java`) and the shipped controllers (`controller/AppShellController.java`,
> `controller/LaunchGateController.java`).

## 1. Purpose — the problem this package solves

`com.gym` contains the classes the JVM and JavaFX start, plus the composition of the two windows
the product has:

| Class | Lines | What it is |
|---|---|---|
| `MainApp` | ~1 600 | The **m3fx component gallery**: an interactive 17-page Material Design 3 / Expressive showcase, plus a viewer for the 19 app FXML screens and a button that launches the full app shell |
| `M3Samples` | 9 | A convenience launcher alias (`extends MainApp`, delegates to `MainApp.main`) |
| `GymApp` | 169 | The **app entry point**: maximizes the window, loads `/fxml/app-shell.fxml`, applies the M3 theme, starts a freeze watchdog and (for real runs) the splash → login → shell launch gates |

Everything else lives in `controller/` (20 controllers). The four planned layers (`model`, `dao`,
`service`, `util`) contain only READMEs for now — the entry points deliberately do **not** create
DAOs, services or a database today (see §7).

**What breaks if the launchers are done wrong.** JavaFX has exactly one `start(Stage)` per
`Application`. If the theme is applied after the first frame, every control flashes unstyled; if a
closing stage force-kills the JVM, a second window opened by the gallery dies with it (this is why
`GymApp.stop()` calls `Platform.exit()` and never `System.exit` — `DECISIONS.md` documents this);
if a UI freeze has no diagnostics, it hangs silently. Each launcher below answers one of these.

**What this package deliberately does not do.** No business rules, no SQL, no data access. The
gallery builds showcase nodes; `GymApp` loads one FXML and hands over to
`AppShellController`. Screens are not opened from here beyond the shell itself.

## 2. What you learn here (OOP concepts)

| Concept | Where | Why the project needs it | How to apply it (one line) |
|---|---|---|---|
| Inheritance from a framework class | `MainApp extends Application`, `GymApp extends Application` | The toolkit calls *your* `start(Stage)` back | Override only what you need; never call `start` yourself |
| Template-method lifecycle | `init()` → `start(Stage)` → `stop()` | A documented place for "before UI", "build UI", "after UI" | Build the scene in `start`, release resources in `stop` |
| Static entry point | `public static void main(String[] args)` → `launch(args)` | The JVM needs `main`; JavaFX needs `launch` | Keep `main` to the launch flag + one call |
| Composition root (light form) | `GymApp.start` picks the theme, loads the shell FXML, applies stylesheets | One place decides seed color, variant and scene setup | Create collaborators in `start`, pass them inward |
| Encapsulation of window state | `Stage` handling stays inside the launcher | The window belongs to the app, not to a screen | Keep `Stage` out of controllers |
| A documented static flag | `GymApp.SHOW_LAUNCH_GATES` | Tests call `start(Stage)` directly and must get the plain shell; `main()` flips the flag for the full gate choreography (`DECISIONS.md` 71) | One mutable static, set in exactly one place, documented in its Javadoc |
| Daemon worker threads | `GymApp.startFreezeWatchdog()` | A monitor thread must never keep the JVM alive | `setDaemon(true)`, sleep-loop, read-only over the scene graph |
| Constants and literals | `SEED_PURPLE = Color.web("#6750A4")`, window sizes | Named once, shared (`MainApp`'s seed presets reuse `GymApp.SEED_PURPLE`) | Define at the top with a comment on why |

## 3. Design patterns in this layer

| Pattern | Where | Why | What goes wrong if used carelessly |
|---|---|---|---|
| **Composition root** | `GymApp.start(Stage)` | One class knows the theme, the scene and the shell; everything below receives them | Screens creating their own themes produce mismatched palettes |
| **Facade over the toolkit** | `M3Stylesheets.applyTo(scene, theme)`, `M3CommandPalette.attachGlobalShortcut(...)` | Screens ask for a result, not for CSS plumbing | Hiding `Stage` completely makes ordinary JavaFX work harder |
| **Template method** (framework-defined) | `Application.init/start/stop` | The toolkit owns the sequence | Long work in `start` freezes the window before it shows |
| **Watchdog** | `startFreezeWatchdog()` | Samples FX-thread CPU; dumps stacks when busy, not when deadlocked | A watchdog that touches the scene graph would *cause* the freeze it watches |
| **Singleton — deliberately avoided** | — | A global `App` would let screens reach global state | Pass what a class needs through constructors |

## 4. Rules you must follow (and the bug each rule prevents)

1. **All scene-graph work happens on the FX application thread.** `Platform.runLater` is the only
   bridge from the watchdog or background work.
2. **No `System.exit` from this package.** `MainApp`'s "Launch Full Application Shell Window"
   action starts a `GymApp` stage *inside the same JVM* as the running gallery — killing the
   process would murder the gallery window too. `GymApp.stop()` calls `Platform.exit()` only; the
   launcher performs the final JVM exit (`GymApp.java` Javadoc documents this).
3. **Apply the theme before the scene is shown.** `GymApp` builds `M3Theme.fromSeed(SEED_PURPLE,
   TONAL_SPOT, true)` and calls `M3Stylesheets.setLatestTheme(theme)` *before* the FXML loads, so
   custom controls resolve colors during inflation, then `applyTo(scene, theme)` before `show()`.
4. **Maximize before `show()`** on Windows, so the OS computes the work area correctly
   (`GymApp.start` does `setMaximized(true)` before `show()`).
5. **`SHOW_LAUNCH_GATES` stays a `main()`-only switch.** Tests drive `start(Stage)` directly and
   must never see the splash/login overlay (the FXML test contracts pin the pre-gate shell).
6. **The watchdog never touches the scene graph.** It reads thread CPU and stack traces only, and
   is a daemon so it cannot hold the JVM open.
7. **No planned-backend classes are named here.** When the DAO layer lands, the composition root
   gains the `DbConnection` wiring described in `docs/UML.md` §2 — not before. A launcher that
   references not-yet-existing classes cannot compile, and a stub one lies.

## 5. The files in this package

### `MainApp.java` — the m3fx gallery (entry point of `gradlew run`)

**Status:** implemented (~1 600 lines). **What it is.** `MainApp extends Application` builds the
showcase entirely in code — no FXML for its own UI:

- `M3Overlay` overlay layer, `M3CommandPalette` (Ctrl/Cmd+K, `attachGlobalShortcut`) and an
  `M3NavigationDrawer` with 17 destinations: tokens, buttons/FABs, split buttons/toolbars, inputs,
  chips/badges, cards/lists, progress/sliders, carousels, overlays/dialogs, a spring-physics
  playground, charts, 3D viewers, effects, data components, live-workout HUD, gym floorplan, and
  "Gym App Screens (19 FXMLs)".
- A theme control strip: seed preset dots (`GymApp.SEED_PURPLE` + five more), a `ColorPicker`,
  `ThemeVariant` combo, contrast combo, and an "Expressive Physics" switch.
- The "Gym App Screens" page is the FXML porting studio: a combo loads any of the 19 app screens
  into a viewport, with buttons to launch the full shell (`new GymApp().start(new Stage())`) or a
  standalone login window.
- Scene 1220×840, themed via `M3Stylesheets.applyTo`, title "m3fx — Material Design 3 & Expressive
  Showcase".

### `M3Samples.java` — the gallery alias

**Status:** implemented (9 lines). `M3Samples extends MainApp` whose `main` delegates to
`MainApp.main` — a stable second entry point for the showcase.

### `GymApp.java` — the app shell launcher (entry point of `gradlew runGym`)

**Status:** implemented (169 lines). **What it does, in order** (`start(Stage)`):

1. `M3Theme.fromSeed(SEED_PURPLE, ThemeVariant.TONAL_SPOT, true)` + `M3Stylesheets.setLatestTheme`
   so FXML inflation resolves looked-up colors.
2. Loads `/fxml/app-shell.fxml` (fails fast with `IllegalStateException` if missing) — the loader
   produces an `AppShellController`.
3. `new Scene(root, 1280, 800)` + `applyTo(scene, theme)`; title
   "GYM-OS // Enterprise Gym Management System (2026)"; min size 1024×700.
4. `setOnCloseRequest(e -> Platform.exit())` so `stop()` cleanup runs; `setMaximized(true)`;
   `show()`; `startFreezeWatchdog()`.
5. If `SHOW_LAUNCH_GATES` (set by `main()` only) and the controller is an `AppShellController`:
   `shellController.beginLaunchGates()` — the splash → login → morph choreography
   (`DECISIONS.md` 71). Invalid sign-in input shakes the card; **demo auth: any email plus a
   password of 4+ characters signs in** (there is no backend by design — `DECISIONS.md` 71).

**The freeze watchdog** (`startFreezeWatchdog`, diagnostics only): a daemon thread samples the
JavaFX Application Thread's CPU via `ThreadMXBean`; if it stays above 60% of a core for 4
consecutive seconds it dumps the FX and `QuantumRenderer-0` stacks to stderr (at most one dump per
30 s), so a repaint-storm freeze self-diagnoses in the `runGym` console instead of hanging silently
(`DECISIONS.md` 70). Read-only over the scene graph.

**`stop()`**: `super.stop(); Platform.exit();` — cleanup only, no `System.exit` (rule 2).

### The runtime spine this package starts

`GymApp` → `app-shell.fxml` → **`AppShellController`** (in `controller/`; see that README for the
full tour): hosts the `M3NavigationRail`, top app bar, `contentArea` swap, command palette and
global snackbar; `loadScreen(String fxmlName)` swaps screens, `openScreenViaRail(...)` wires rail
buttons, `beginLaunchGates()` hands off to **`LaunchGateController`** — built entirely in code (no
FXML, no `fx:id`s, so the FXML test contracts stay byte-identical), with `beginLaunchGates()`,
`showLoginGate()`, `isGateShowing()` as its public surface and `new LaunchGateController(shell)`
as the single constructor.

## 6. How this package connects to the others

| This package | Calls / starts | Called by |
|---|---|---|
| `MainApp` | every m3fx control (gallery), `M3Stylesheets`, `M3Overlay`, `M3CommandPalette`; opens `GymApp` on demand | JVM (`main`), Gradle `run` task |
| `GymApp` | `AppShellController` (via `app-shell.fxml`), `LaunchGateController` (gates), `M3Stylesheets` | JVM (`main`), Gradle `runGym` task, `MainApp`'s shell button |
| `M3Samples` | `MainApp.main` | JVM |

**Planned wiring (not built).** When the backend lands, `GymApp`/the shell gains the composition
root from `docs/UML.md` §2: a `DbConnection` implementation created exactly once, DAOs passed to
services, services reachable from controllers, and `SessionContext` cleared on exit. None of those
types exist yet — this package currently names no class outside `com.gym.controller` and the m3fx
modules.

**Tests.** The 9 harness classes in `src/test/java/com/gym/fxml/` call `GymApp.start(Stage)`
directly with `SHOW_LAUNCH_GATES == false` (its default), which is why the launch-gate flag exists.

## 7. Common mistakes and review checklist

| Symptom | Cause | Fix |
|---|---|---|
| Second window dies when one closes | Someone added `System.exit` to a launcher | Use `Platform.exit()`; the launcher exits the JVM |
| Gallery appears unstyled for one frame | Theme applied after `show()` | Keep `applyTo` before `show()` (rule 3) |
| Tests fail with the splash overlay in the way | `main()` flag leaked into a test path | Tests call `start(Stage)` directly; `SHOW_LAUNCH_GATES` stays `false` |
| JVM will not exit after the window closes | A non-daemon background thread | Watchdog-style threads: `setDaemon(true)` |
| Shell loads blank | `/fxml/app-shell.fxml` missing from resources | `GymApp` throws fast; check `src/main/resources/fxml/` |

Checklist for a change here:

- [ ] Only bootstrap concerns touched (theme, scene, window, shell load, gates).
- [ ] No `System.exit`; no SQL/DAO/business rule; no new static mutable state beyond the
      documented `SHOW_LAUNCH_GATES` flag.
- [ ] Theme applied before `show()`; window title/size set here, not in screens.
- [ ] Freeze watchdog still daemon + read-only.
- [ ] `./gradlew test` green (the harness pins the pre-gate shell behaviour).

## 8. Learning exercises

1. **Trace the launch.** List, in order, the five things `GymApp.start` does before `stage.show()`
   — then explain why the theme must be created *before* the FXML loads.
2. **Explain `SHOW_LAUNCH_GATES`.** Why is a mutable static flag acceptable here when rule 2 of the
   old docs banned static mutable state? (Hint: who writes it, who reads it, and what breaks if a
   test flips it.)
3. **Write the watchdog's acceptance check.** Given "FX thread busy >60% for 4 s", why does the
   watchdog also rate-limit dumps to one per 30 s? What console storm would you otherwise cause?
4. **Find the layering violation.** A teammate adds `new M3Card()` styling helpers to `GymApp` so a
   screen "can reuse them". Why is that wrong, and where do shared visuals belong?
5. **Plan the composition root.** When `model/`/`dao/`/`service/` land, which class may name the
   concrete `DbConnection` implementation, and which must receive only interfaces? Write the two
   lines of the future wiring.

<details>
<summary>Answers</summary>

1. Theme (`M3Theme.fromSeed` + `setLatestTheme`) → load `app-shell.fxml` → scene + `applyTo` →
   title/min-size/close-request → `setMaximized(true)`. The theme comes first because FXML
   inflation constructs custom controls whose colors resolve from the latest theme; applying after
   construction misses the lookups.
2. It is written exactly once (`main()`), read once (`start`), documented in its Javadoc, and its
   default (`false`) is what every test sees. The old ban targets *shared mutable app state* that
   screens reach for; this flag is a launcher-scoped switch whose wrong value only changes whether
   an overlay shows.
3. A freeze loop triggers the dump condition every second; without a rate limit you would emit a
   full stack dump per second, flooding the console and adding its own I/O cost while the FX thread
   is already starving.
4. `GymApp` is a launcher: adding widget-building code there makes it a second gallery and blurs
   the composition root. Shared visuals belong in the m3fx library (or `controller/` helpers), not
   in the `Application` class.
5. The `Application` implementation (`GymApp`/the shell bootstrap) may name
   `SqliteDbConnection`; every DAO and service receives the `DbConnection`/DAO interfaces. Sketch:
   `DbConnection db = new SqliteDbConnection();` then `new CustomerDaoImpl(db)` passed into
   `new CustomerService(dao)`.

</details>

## 9. Ownership and tasks

| File | Responsibility | Owner | Status |
|------|----------------|-------|--------|
| `MainApp.java` | m3fx gallery: pages, theme bar, command palette, FXML screen viewer | Abdelrhman | Done |
| `M3Samples.java` | Gallery alias launcher | Abdelrhman | Done |
| `GymApp.java` | App shell launcher: theme, window, watchdog, launch gates | Abdelrhman | Done |
| Backend composition root (DbConnection → DAOs → services) | Abdelrhman | Planned — see `docs/UML.md` §2 |

### Definition of done for a change in this package

- [ ] `./gradlew build` is green (includes the 59 app-module harness tests).
- [ ] `./gradlew run` opens the gallery; `./gradlew runGym` opens the shell, themed from the first
      frame, maximized, closable without killing a co-open gallery window.
- [ ] Launch gates appear only via `main()`; tests still drive the plain shell.
- [ ] No `System.exit`, no backend classes named, no new static mutable state.
