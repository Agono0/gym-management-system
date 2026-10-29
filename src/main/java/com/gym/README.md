# `com.gym` — application entry point (JavaFX branch)

> **Signature authority:** `docs/UML.md` §1 declares `MainApp`, §2 declares the startup/session flow,
> §5 shows the `MainApp ..> LoginController` edge. This README explains *why* the class exists, what
> the JavaFX lifecycle does around it, and how to write it. Code here is an illustrative sketch — if a
> sketch and `UML.md` disagree, `UML.md` wins.

## 1. Purpose — the problem this package solves

`com.gym` contains **one class**: `MainApp`. It is the process entry point and the *composition root*
of the application — the single place where the concrete parts of the system are created and wired
together (the theme, the window, the navigation shell, the overlay layer, and the first screen).

Everything else in the project lives in one of four layers (`model`, `dao`, `service`, `controller`)
plus the cross-cutting `util` package. `MainApp` deliberately sits **outside** those layers, because
it is the class that decides *which* implementation of each layer will be used, and a layer is not the
right place to make that decision: a DAO impl must not choose itself, and a service must not know
about `Stage`s.

**What breaks if this class is done wrong.** A JavaFX application has exactly one `start(Stage)` call.
If the theme is applied there but the session is not cleared on exit, a second run of the app can
inherit a signed-in administrator (a real security bug in a shared reception machine). If the overlay
layer is not installed before the first screen opens, dialogs and snackbars silently fall back to
separate windows or disappear. If a controller is opened before the theme stylesheet is on the scene,
the first frame renders unstyled and then jumps. All three failures are invisible in unit tests and
obvious to a user — which is why this file must follow the documented startup order exactly.

**What this package deliberately does not do.** It contains no business rules, no SQL, no DAO access,
no FXML layout, and no navigation *logic* beyond opening the first screen and hosting the shell. It is
a bootstrap, not a screen.

## 2. What you learn here (OOP concepts)

| Concept | Where in this file | Why the project needs it | How to apply it (one line) |
|---|---|---|---|
| Inheritance from a framework class | `MainApp extends Application` | JavaFX requires it: the toolkit calls *your* `start` back | Extend, override only what you need, never call `start` yourself |
| The template-method idea (lifecycle hooks) | `init()` → `start(Stage)` → `stop()` are called by the framework in that order | Gives a documented place for "before UI", "build UI", "after UI" work | Do heavy/shared setup in `init`, build the scene in `start`, release resources in `stop` |
| Composition root / manual dependency injection | `MainApp` creates `SqliteDbConnection`, passes it to DAOs, DAOs to services | Nothing else in the app is allowed to `new` infrastructure; one place to swap it (`DECISIONS.md` 33) | Create collaborators in `start`, pass them inward, keep them off static fields |
| Polymorphism at the seam | The field/variable type is `DbConnection`, the object is `SqliteDbConnection` | The database can be replaced without touching 26 DAO files | Declare the interface type, instantiate the implementation once |
| Static entry point | `public static void main(String[] args)` → `launch(args)` | The JVM needs a static `main`; JavaFX needs `launch` to boot the toolkit | Keep `main` to one line — no logic turns up there |
| Encapsulation of window state | `primaryStage`, the theme object and the shell live as private state of this class | The window belongs to the app, not to a screen or a global | Keep `Stage` out of every other class |
| Documented mutable state | `SessionContext` is static mutable state owned by `util` (`DECISIONS.md` 34) — `MainApp` reads it once, at exit | One place to clear the session means no stale login | Clear it in `stop()` even though logout already clears it (a safety net) |
| Interfaces over singletons | No `App.getInstance()` singleton anywhere | A singleton would make tests and the Swing port depend on global state | Pass what a class needs through its constructor |
| Constants and literals | Window title, minimum size, initial scene size are named constants or one obvious literal | Magic numbers scattered in code cannot be reviewed or changed consistently | Define them at the top with a comment on why that value |

## 3. Design patterns in this layer

| Pattern | Where | Why this pattern | What goes wrong if used carelessly |
|---|---|---|---|
| **Composition root** (a.k.a. "poor man's DI") | `start(Stage)` | One class knows how to assemble the object graph; everything below takes its collaborators as constructor parameters | If screens start creating their own DAOs, the wiring is no longer reviewable and the database choice spreads |
| **Facade over the toolkit** | Thin helper calls (`M3Stylesheets.applyTo`, `FxUtils` dialogs) rather than raw toolkit juggling in every screen | Screens ask for a *result*, not for JavaFX plumbing | Hiding too much (e.g. wrapping `Stage` completely) makes ordinary JavaFX work harder than it should be |
| **Template method** (framework-defined) | `Application.init/start/stop` | The toolkit owns the sequence; we fill in the steps | Putting long queries in `start` freezes the splash; put them in a `Task` and show progress |
| **Singleton — deliberately avoided** | — | A global `App` would be the only way a service could reach the UI — exactly the dependency the layering forbids | Explain in review: we pass `Stage` to the shell, not to services |

## 4. Rules you must follow (and the bug each rule prevents)

1. **All scene-graph work happens on the FX application thread.** Touching a control from a worker
   thread either throws or corrupts the scene; `Task`/`Platform.runLater` are the two allowed bridges.
2. **No static mutable state in this package.** The only static state in the app is
   `util/SessionContext` (documented in `DECISIONS.md` 34); a second one makes the app's behaviour
   depend on run order.
3. **Apply the theme before the first screen is shown.** Otherwise the window paints unstyled and then
   jumps, and any control created before the stylesheet loads misses its lookups.
4. **Clear `SessionContext` in `stop()`.** Prevents a signed-in session from surviving the window
   (and, on a shared machine, from leaking to the next user).
5. **Do not touch DAOs or SQL from here.** Create the DAO inside the composition root and pass it into
   the service that owns it; a `MainApp` that runs queries means the layering has already failed.
6. **Do not put business decisions in this class.** "Which screen opens first" is bootstrap; "who may
   see it" is `SessionContext.requireRole` (rule 16).
7. **The window title and sizes are set here and nowhere else.** A screen that renames the window is a
   review failure — the user's window is app state.
8. **`main` stays one line.** `launch(args)` only. Logic in `main` cannot be tested through the
   JavaFX lifecycle.

## 5. The files in this package

Write order: there is only one file, and it comes **after** `util/DbConnection` (so the composition
root has something to wire) but **before** the screens, so there is always a window to run.

### `MainApp.java` — the JavaFX application and composition root (owner: Abdelrhman)

**Status:** exists as a placeholder — it loads the FXML showcase, with no theme, no shell, no session.
**Canonical:** `docs/UML.md` §1 (declaration), §2 (startup flow, steps 1–4), §5 (`..>` edges).

**What it is.** The class the JVM starts. `main` hands over to JavaFX, JavaFX constructs `MainApp` and
calls `init()`, `start(Stage)` and (on exit) `stop()`. In `start` we assemble the app: theme →
navigation shell → overlay layer → first screen.

**Fields.** A `MainApp` needs almost none, and each one must earn its place:

| Field | Type | Nullable? | Why this type/name |
|---|---|---|---|
| (none required) | — | — | Window/theme/connection objects are locals inside `start` unless `stop()` needs them |
| optional: the bootstrap connection | `DbConnection` | only if `stop()` must close it | Declared as the **interface** so the concrete SQLite class is named once (rule: `DECISIONS.md` 33) |

The notable design choice is the *absence* of fields: everything created in `start` and not needed
afterwards stays a local variable, so nothing can be accidentally reused by a screen.

**Why it is designed this way.**

- **Why `Application` and not a plain `main` that builds a `Stage`.** JavaFX requires
  `Application.launch` to start the toolkit, the render thread and the implicit exit behaviour. Doing
  it by hand is possible and fragile; the framework path is the documented one.
- **Why the composition root is here and not in a service.** The choice "SQLite, and therefore
  `SqliteDbConnection`" is a deployment decision. Services must be testable without a database, so
  they receive a DAO; only the bootstrap names a concrete database (`DECISIONS.md` 33).
- **Why it is not one of the 16 screens.** `docs/UML.md` §5 lists `MainApp` only as the edge that
  opens `LoginController`. A screen that also owns the window cannot be reopened or embedded, and the
  nav shell would have to live inside it.
- **Why the overlay layer is installed before the first screen** (`DECISIONS.md` 8, 35): dialogs,
  snackbars and sheets render into a `StackPane` above the shell, so that stack must exist before any
  screen tries to show one. Today the overlay layer is not exported by the library, which is why the
  shell step is still blocked (`DECISIONS.md` 35, `COVERAGE.md`) — the honest status, not a shortcut.
- **Why the session is cleared twice** (logout *and* `stop`): logout is the normal path; `stop` covers
  the close-button path, where no logout handler runs.

**How to write it.** Steps, in order:

1. Declare `public class MainApp extends Application`.
2. Add the constants the window needs (title, initial width/height, minimum size) as `private static final`, each with a short comment on why that value.
   **Open detail (decide + log in `DECISIONS.md`):** the exact initial and minimum window size is not
   fixed by `docs/UML.md`; agree one pair of values with the team (the nav shell needs room for a
   navigation rail plus a content pane), then treat it as the app's single source for window size.
3. Write `public static void main(String[] args) { launch(args); }`.
4. Override `start(Stage primaryStage)`:
   1. build the composition root: `DbConnection db = new SqliteDbConnection(); db.initializeSchema();`
   2. create the DAOs and services the shell needs, passing the DAO through the constructor;
   3. load the shell/nav FXML with `FXMLLoader` (`getClass().getResource("/fxml/…")` — the leading `/` means "classpath root", see `src/main/resources/fxml/README.md`);
   4. call the theme entry point on the new `Scene`: `M3Stylesheets.applyTo(scene, M3Theme.baseline(false));`
      (light/dark comes from the user's preference; a UI switch just calls it again);
   5. install the overlay layer above the shell (blocked on the library entry point — `DECISIONS.md` 35);
   6. set the title, size and minimum size on `primaryStage`, then `primaryStage.show()`;
   7. open the first screen (`LoginController` / its FXML).
5. Override `stop()` and clear `SessionContext`.
6. Override `init()` only if something must exist before the toolkit builds a window (rare; nothing in
   this project needs it today).

**Sketch.**

```java
// Sketch — the canonical declaration is docs/UML.md §1; the startup ORDER is §2.
public class MainApp extends Application {

    private static final String WINDOW_TITLE = "Gym Management System";

    public static void main(String[] args) {
        launch(args);                       // one line: the JVM entry, nothing else belongs here
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1) composition root: the ONLY place that names the concrete database (DECISIONS.md 33)
        DbConnection db = new SqliteDbConnection();
        db.initializeSchema();              // creates gym.db + tables on first run

        // 2) wire one vertical slice's collaborators; services take DAO *interfaces*
        CustomerDao customerDao = new CustomerDaoImpl(db);
        CustomerService customerService = new CustomerService(customerDao /*, … */);

        // 3) build the shell and apply the theme BEFORE showing anything (rule 3)
        Parent shell = new FXMLLoader(getClass().getResource("/fxml/main.fxml")).load();
        Scene scene = new Scene(shell);
        M3Stylesheets.applyTo(scene, M3Theme.baseline(false));

        // 4) overlay layer, then the window, then the first screen
        //    OverlayLayer install: blocked on the m3fx public facade (DECISIONS.md 35)
        primaryStage.setTitle(WINDOW_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(720);
        primaryStage.show();

        // 5) first screen: LoginController → AdminService.authenticate → SessionContext
        FxUtils.switchScene("/fxml/login.fxml");
    }

    @Override
    public void stop() {
        SessionContext.clear();             // the close-button path never runs onLogout()
    }
}
```

**Line-by-line: the placeholder that exists today.** `MainApp.java` is 24 lines (4 of them blank) and
is worth reading as a lesson in what is missing (root `README` §4 says "placeholder"):

| Current line | What it does | What is missing / why it will change |
|---|---|---|
| `FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));` | Loads the component showcase from the classpath | The leading `/` is correct and important; the file it loads is the library gallery, not a screen of ours |
| `Parent root = loader.load();` | Builds the node tree | No controller is attached (`main.fxml` has no `fx:controller`), so nothing is wired |
| `primaryStage.setTitle("JavaFX Application");` | Window title | Must be the product name; a title is user-visible app state |
| `primaryStage.setScene(new Scene(root, 600, 400));` | Fixed 600×400 scene | No theme applied (`M3Stylesheets.applyTo` missing) → controls render unstyled; a hardcoded size cannot host the nav shell |
| `primaryStage.show();` | Shows the window | No shell, no overlay layer, no first screen, no session handling |
| `main → launch(args)` | Correct already | Keep it exactly like this |

**How to know it is right.**

1. `gradlew run` — the window opens with the product title, at the intended size, already themed (no
   unstyled first frame).
2. `gym.db` appears in the working directory on the first run (`BUILD_AND_RUN.md` §6).
3. Log in, close the window with the X (not via logout), run the app again — the login screen appears,
   **not** the dashboard. That single check proves `stop()` clears the session.
4. Trigger a dialog on a screen: it must render inside the window (overlay), not as a second OS window.
5. Console shows no stack trace; any database failure is a friendly dialog (`FxUtils.error`).

**Common mistakes.**

- **`getResource("fxml/main.fxml")` without the leading slash.** The path is then relative to the
  class's package and `loader.load()` throws `NullPointerException`/`IOException` even though the file
  exists in `src/main/resources`.
- **Creating the scene before the stylesheet is applied.** The first frame is unstyled; users see a
  flash. Apply the theme to the `Scene` first.
- **Putting the theme call on the root node's `getStylesheets()`.** A look-up colour resolves from the
  root of the *scene*; styling only the root node means popups and dialogs (separate scenes) stay
  unthemed.
- **Forgetting `stop()`.** The session survives the window; on a shared reception PC the next person
  lands on the dashboard.
- **Queries in `start`.** They run before the window is visible, so the app looks crashed. Use a
  `Task` with a progress indicator, or move the query into the screen's `initialize()`.
- **`new SqliteDbConnection()` inside a DAO or service.** Then the composition root is not the root any
  more, and swapping the database touches many files (`DECISIONS.md` 33).
- **Static `Stage`/`Scene` fields "for convenience".** They are mutable global state and they break the
  moment the Swing branch or a test creates a second window.
- **Adding the tenth responsibility here** (e.g. "just this one query because it is only the
  dashboard's number"). Bootstrap code grows one exception at a time; the layering rule is absolute.

**How it connects.**

- **Calls:** `SqliteDbConnection` → `initializeSchema`, the DAO constructors, `M3Stylesheets.applyTo`,
  `FxUtils.switchScene`, `SessionContext.clear`, and it opens `LoginController` (`UML.md` §2, §5).
- **Is called by:** the JVM (`main`) and the JavaFX toolkit (`init`/`start`/`stop`). Nothing else.
- **Must not depend on:** any `controller` class's internals, any DAO SQL, any business rule.

## 6. How this package connects to the others

Startup and session flow (the canonical version is `docs/UML.md` §2):

1. `MainApp.start(Stage)` applies the theme, builds the nav shell and the overlay layer, and opens
   `LoginController`.
2. `LoginController.onLogin()` calls `AdminService.authenticate(username, password)`. It returns
   `null` for an unknown username, a wrong password, or `Administrator.active == false` (rule 16).
3. On success the controller stores the returned `Administrator` in `SessionContext` and opens
   `DashboardController`; `SessionContext.requireRole(StaffRole.ADMIN)` later gates `StaffController`.
4. `DashboardController.onLogout()` calls `SessionContext.clear()` and returns to `LoginController`;
   `MainApp.stop()` clears it again as a safety net.

| This package depends on | It is used by |
|---|---|
| `util/DbConnection`, `util/SqliteDbConnection`, `util/SessionContext`, `util/FxUtils` (owned by `util`, described in `../util/README.md`) | The JVM and the JavaFX toolkit only |
| `dao/*DaoImpl` constructors (to wire services) | — |
| `service/*Service` constructors (to hand to the shell) | — |
| The library's public theme entry point (`M3Stylesheets.applyTo`) | — |
| FXML files in `src/main/resources/fxml/` (`../../../resources/fxml/README.md`) | — |

**The seam:** `DbConnection`. `MainApp` is the only place allowed to name `SqliteDbConnection`; every
DAO below takes the interface (`DECISIONS.md` 33).

## 7. Common mistakes and review checklist

**Mistakes we expect (and their symptoms).**

| Symptom | Cause | Fix |
|---|---|---|
| Window opens unstyled, then "pops" into M3 | Theme applied after `show()`, or applied to the root node instead of the `Scene` | Apply to the `Scene` before `show()` |
| `NullPointerException` at `loader.load()` | Resource path wrong (missing leading `/`, wrong folder, file not in `src/main/resources`) | Check the path against `src/main/resources/fxml/` |
| Signed-in user survives a restart | `stop()` does not clear `SessionContext` | Clear it in `stop()` as well as on logout |
| Dialogs appear as separate OS windows | Overlay layer not installed before screens open (`DECISIONS.md` 8/35) | Install it in `start`, before the first screen |
| App "hangs" for seconds at startup | A query or file scan in `start`, on the FX thread | Move it off the FX thread (`Task`) or into the screen |
| Second screen cannot be opened twice | A static `Stage`/controller field was introduced | Pass data explicitly; keep window state in `MainApp` only |
| `gym.db` locked / `SQLITE_BUSY` | A connection opened here and never closed, or two app instances | Close connections in the DAO (`try`-with-resources); one instance per file |

**Review checklist for a change to this package.**

- [ ] Only bootstrap concerns touched (theme, window, shell, overlay, first screen, session clear).
- [ ] No SQL, no DAO call, no business rule added here.
- [ ] Theme applied to the `Scene` before `show()`.
- [ ] Title/size/min-size set in one place, not in screens.
- [ ] `SessionContext.clear()` present in `stop()`.
- [ ] No new `static` mutable field.
- [ ] `main` is still a single `launch(args)` call.
- [ ] The class still matches `docs/UML.md` §1 (declaration) and §2 (order of the startup steps); if
      the order changed, `UML.md` §2 was updated in the same PR.

## 8. Learning exercises

1. **Read the placeholder** and list, in order, the five startup steps from `UML.md` §2 that it does
   not implement yet.
2. **Explain the leading slash.** Why does `getResource("/fxml/main.fxml")` work while
   `getResource("fxml/main.fxml")` fails, even though both "look" like the same path?
3. **Write `stop()`.** Three lines: clear the session and release the bootstrap connection if you kept
   one. Then explain which user-visible bug it prevents.
4. **Find the layering violation.** This snippet is wrong — say why and rewrite it:
   ```java
   @Override public void start(Stage stage) throws Exception {
       CustomerDao dao = new CustomerDaoImpl(new SqliteDbConnection());
       long active = dao.findAll().stream().filter(c -> c.getStatus() == CustomerStatus.ACTIVE).count();
       stage.setTitle("Active members: " + active);
       stage.show();
   }
   ```
5. **Defend the composition root.** A teammate proposes `DbProvider.get()` returning a static
   singleton so DAOs can fetch a connection themselves. List three concrete problems this creates in
   *this* project (hint: `DECISIONS.md` 33, the Swing branch, unit-testability of services).

<details>
<summary>Answers</summary>

1. (a) apply the theme with `M3Stylesheets.applyTo(scene, theme)`, (b) build the navigation shell,
   (c) install the overlay layer, (d) open `LoginController`, (e) clear `SessionContext` on exit.
   Today it does none of them: it loads the showcase with a hardcoded title and a fixed 600×400 scene.
2. `getResource` resolves a path **relative to the class's package** unless it starts with `/`. With
   the leading slash it is resolved from the classpath root, which is where `fxml/` sits inside the
   built jar (`src/main/resources` is the classpath root at runtime).
3. `SessionContext.clear();` — without it the signed-in administrator survives the window closing, and
   the next launch (or the next user of a shared machine) starts already authenticated.
4. The violation is a `MainApp` doing data access **and** a business decision ("which customers are
   active"). The DAO must be created here but used by `CustomerService`; the count belongs in a service
   method (or `CustomerDao.findByStatus` called by the service), and the wording is the dashboard
   controller's job.
5. A static provider hides the dependency (a caller cannot see what it needs), makes services
   untestable without a real database file, and hard-wires SQLite into 26 DAO classes, so the Swing
   port and any database change stop being one-file changes (`DECISIONS.md` 33).

</details>

## 9. Ownership and tasks

| File | Responsibility | Owner | Status |
|------|----------------|-------|--------|
| `MainApp.java` | Entry point: window, nav shell, theme, overlay layer | Abdelrhman | Skeleton only (placeholder described in §5) |

| Task | Owner | Status |
|------|-------|--------|
| Skeleton created | Abdelrhman | Done |
| Apply the M3 theme (`M3Stylesheets.applyTo(scene, theme)`) | Abdelrhman | To Do |
| Install the overlay layer and nav shell | Abdelrhman | To Do (blocked on the library's public overlay entry point — `DECISIONS.md` 35) |
| Open `LoginController` and clear `SessionContext` on exit | Abdelrhman | To Do |

### Definition of done for a file in this package

- [ ] `gradlew build` is green and `gradlew run` opens the window.
- [ ] Startup follows `docs/UML.md` §2 in order (theme → shell → overlay → first screen).
- [ ] Theme applied to the `Scene` before `show()`; no unstyled first frame.
- [ ] `SessionContext.clear()` in `stop()`; verified by closing the window and restarting.
- [ ] No SQL, no DAO access, no business rule, no new static mutable state in this package.
- [ ] `main` is one line; window title/size defined once.
- [ ] Placeholder replaced (title, size, theme call, real shell) — the five gaps in §5 closed.
- [ ] No stack trace reaches the user; failures surface through `FxUtils`.

Rules: all UI code runs on the FX thread. No static mutable state in this package. Do not
reference DAOs or SQL from here — controller → service → DAO only.
