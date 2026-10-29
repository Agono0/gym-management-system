# `resources` — Classpath assets: FXML, DB schema, theme, images (JavaFX branch)

> **Signature authority:** `docs/UML.md` §2 owns the startup flow (`MainApp` → theme → nav shell →
> overlay layer), §3 owns every DAO query that `schema.sql` must be able to serve, and §5 owns the
> 16 screens that the FXML files must match. This README explains *why* each asset folder exists and
> *how* to write what goes in it. XML and SQL here are illustrative sketches, not the specification —
> if a sketch and `docs/UML.md` disagree, `docs/UML.md` wins.

## 1. Purpose — the problem this folder solves

`src/main/resources` holds everything the application needs that is **not Java code**: the FXML
layouts that describe each screen, the SQLite DDL that creates the database, the images, and whatever
theme assets the app itself owns. Gradle copies this whole tree into the jar under a namespace called
the **classpath** — the flat, read-only list of files a running program can look up by name — so Java
code reaches these files by *name*, not by *path*.

**What breaks if this folder is missing.** The window cannot be built at all. `MainApp.start` calls
`getClass().getResource("/fxml/main.fxml")` and hands the URL to `FXMLLoader`; with no
`src/main/resources` tree there is no URL, the loader cannot instantiate a controller, and the app
dies on the first line of `start(Stage)`. There is also nowhere for `schema.sql` to live, so
`SqliteDbConnection.initializeSchema()` has no DDL to run and every DAO query fails with
`no such table`.

**What this folder deliberately does not do.** It carries no behaviour. No business rule lives in an
FXML file, a stylesheet or a `CREATE TABLE` statement. Business rules live in
`src/main/java/com/gym/service/` (rules 1–19 in `docs/UML.md` → "Business rules"); this folder only
*describes* the shape of the UI and the shape of the tables that those services use.

Three facts that drive every rule below:

1. **Resources are packaged into the jar.** After `gradlew build`, `fxml/`, `db/`, `theme/` and
   `images/` sit *inside* `build/libs/*.jar`.
2. **The inside of a jar is read-only.** You cannot write a file back into the classpath. Anything the
   app writes at runtime goes to the file system, outside this tree.
3. **A missing resource is a runtime failure, not a compile error.** The compiler never checks a
   string like `"/fxml/customer-list.fxml"`, so a typo survives `gradlew build` and fails in front of
   the user.

## 2. What you learn here (OOP concepts)

The concept names below match `docs/LEARNING_GUIDE.md` → "OOP concept index".

| Concept | Where in this folder | Why the project needs it | How to apply it (one line) |
|---|---|---|---|
| Abstraction | The classpath itself | Code asks for `"/fxml/login.fxml"`; where that file physically sits (build dir, jar, IDE) is not the code's problem | Always name a resource from the classpath root, never from `src/main/resources` |
| Information hiding | `theme/` and the `-md-sys-color-*` lookups | A screen says "primary", not `#6750A4`, so one theme change repaints 16 screens | Reference the theme name, never the colour value |
| Separation of concerns | `fxml/` vs `controller/` | The view says *what is on screen*; the controller says *what happens*. Mixing them makes both untestable | Structure in FXML, behaviour in Java, wiring via `fx:id` |
| Naming as contract | Controller ↔ FXML pairs, table names | The name is the only link between the two halves, so it must be predictable | Follow the naming table in §5 and never invent a second spelling |
| Single source of truth | `db/schema.sql` vs `db/gym.db` | One committed definition of the tables; one throwaway file that holds rows | Change the schema in `schema.sql`; delete `gym.db` to re-create it |
| Fail fast | Missing `<?import ?>`, wrong `fx:id` | A silent default (empty table, null label) hides the bug until a demo | Load every new FXML once before you commit it |

## 3. Design patterns in this layer

| Pattern | Where | Why this pattern | What goes wrong if used carelessly |
|---|---|---|---|
| MVC (Model–View–Controller) | `fxml/` is the View; `controller/` is the Controller; `model/` is the Model | Three separate jobs, three separate files: a layout change does not touch Java, a rule change does not touch XML | Putting logic in FXML (`onAction` calling a service through a `script` element) puts business rules where no compiler can see them |
| Convention over configuration | Controller ↔ FXML file names, `snake_case` table names | Nobody has to register a mapping table; the name *is* the mapping | Two naming styles coexisting means `FXMLLoader` finds the wrong file and the failure is a `NullPointerException` far from the cause |
| Indirection through looked-up colours | `theme/` (`-md-sys-color-*` names resolved at runtime) | Decouples 16 screens from one palette and from light/dark | Hardcoding a hex value creates a second source of colour truth that dark mode cannot reach |
| Factory (framework-managed instantiation) | `fx:controller="com.gym.controller.LoginController"` | `FXMLLoader` instantiates the controller, so the shell never news up screens | A controller with no public no-arg constructor cannot be built — see `DECISIONS.md` 36 |
| ActiveRecord / DAO-by-entity | **not here** | Persistence lives in `src/main/java/com/gym/dao/` | Putting SQL in `db/` (a `.sql` file per screen, say) would split persistence across two folders |
| Singleton | **avoided here** | A resource is looked up by name; nothing needs a global instance | A cached static loader holds the UI alive after logout |

## 4. Rules you must follow (and the bug each rule prevents)

1. **Name every resource from the classpath root, with a leading `/`.** `getResource("/fxml/login.fxml")`
   works; `getResource("fxml/login.fxml")` resolves *relative to the calling class's package*
   (`com/gym/fxml/...`) and returns `null`. The `null` is not an exception until you pass it to
   `FXMLLoader`, which then throws a bare `NullPointerException` with no file name in it.
   `src/main/java/com/gym/MainApp.java` lines 12–13 carry this comment for a reason.
2. **Never write to a path inside this folder at runtime.** The same URL is a directory during
   development and a read-only entry inside a jar after `gradlew build`, so a "works on my machine"
   write fails for every user. Runtime files go to the file system — that is why `gym.db` is created
   next to the working directory and git-ignored while `db/schema.sql` is committed.
3. **No hardcoded colours in FXML or CSS.** Style with `styleClass` and let the theme's
   `-md-sys-color-*` lookups supply the value (`DECISIONS.md` 4). A hex value in a layout survives the
   light/dark switch, so half the app stays light and the rest turns dark — the failure looks like a
   bug in the theme, not in the file that caused it.
4. **User-facing text is not scattered across FXML files.** Text belongs in one place so it can be
   reviewed and translated: a `%key` in FXML resolved from a resource bundle. Today `fxml/main.fxml`
   holds literal `text="..."` because it is the testing showcase and nothing is translated yet; a
   screen that a user will see should use keys. The bundle's file name and location is an
   **Open detail (decide + log in `DECISIONS.md`)**.
5. **One FXML file per controller, with a name derived from the controller's name.** The pairing is
   the whole contract; see the naming table in §5.
6. **Never put a business rule in FXML or SQL.** A `CHECK` constraint is a last-resort guard against
   corrupt rows (rule 12), not the place where a rule is *decided* — the service decides, and the
   constraint only refuses what should never have been written.
7. **Never commit generated or user-specific files.** `gym.db`, `*.db`, build output and local
   `.css` dumps are git-ignored; a committed database overwrites a teammate's demo data on the next
   `git pull`.
8. **Keep secrets out of resources.** No password, token or connection string: everything in this
   tree is shipped inside the jar and can be read with any zip tool. `administrators.passwordHash`
   stores a PBKDF2 hash, never a password (see `src/main/java/com/gym/util/README.md`).
9. **Every resource is loaded on the FX thread.** `FXMLLoader.load` builds JavaFX nodes, and JavaFX
   nodes must be created and mutated on the FX thread. Loading a screen from a worker thread throws
   `IllegalStateException: Not on FX application thread`.
10. **An empty folder keeps a `.gitkeep`.** Git does not track directories, so `db/`, `theme/` and
    `images/` hold a zero-byte `.gitkeep` until their first real file lands. Delete it in the same
    commit that adds the real file.

## 5. The files in this folder

There is one real asset today (`fxml/main.fxml`) plus four sub-folders, each with its own focused
README. Read them in this order: `fxml/` → `db/` → `theme/` → `images/`. The order follows what a
screen needs first: a layout, then a table behind it, then a palette, then a picture.

### `fxml/` — the declarative screens (owned by [`fxml/README.md`](fxml/README.md))

**Status:** folder exists; `main.fxml` only.
**Canonical:** `docs/UML.md` §5 (the 16 controllers, each with a matching FXML file).

**What it is.** One XML file per screen describing the node tree: containers, controls, their
`fx:id`s, and which controller method each action calls. Sixteen screens are planned.

**Why it is designed this way.** Hand-built node trees in Java hide the layout in 60 lines of
`setPadding`/`setSpacing` calls that nobody can diff. FXML keeps the structure readable and lets the
whole screen be inspected without running it.

**How to write it.** Declare `xmlns`/`xmlns:fx`, add one `<?import ?>` per class you name, set
`fx:controller` to the fully-qualified controller, give every node the controller reads an `fx:id`,
and wire events with `onAction="#method"`. The full how-to, a worked example and a per-screen entry
for all 16 files are in [`fxml/README.md`](fxml/README.md) — this README does not repeat them.

**How to know it is right.** `FXMLLoader.load(getClass().getResource("/fxml/<file>.fxml"))` returns a
`Parent` without throwing, and every `fx:id` you declared has a non-`null` field in the controller.

**Common mistakes.** A missing `<?import ?>` (the loader reports "Class not found" for a class that
clearly exists, because FXML has no wildcard imports); a `fx:id` typo (compiles, then NPE); an
`onAction="#handler"` pointing at a method with parameters (the loader reports a signature mismatch).

### `db/` — the committed schema, not the data (owned by [`db/README.md`](db/README.md))

**Status:** folder exists; `schema.sql` is Week 1 foundation work.
**Canonical:** `docs/UML.md` §3 (the DAO surface each table must serve) and the 13 entities in §1.

**What it is.** `schema.sql` — the DDL (**Data Definition Language**: `CREATE TABLE`, `CREATE INDEX`)
that creates the 13 tables — plus the demo seed rows. The database file itself is **not** here.

**Why it is designed this way.** Splitting definition from data means the schema can be reviewed and
versioned like code, while `gym.db` (the actual SQLite file, created on first run) stays a local
artefact that anyone can delete and rebuild. `.gitignore` lists `gym.db`, `gym.db-*` and `*.db`.

**How to write it.** One `CREATE TABLE IF NOT EXISTS` per entity, column names and types taken from
the entity fields, `CHECK` constraints for the enum columns and the payment XOR rule, foreign keys
declared **without cascades** (rule 4), and an index for each column a DAO actually filters on.

**How to know it is right.** Delete `gym.db`, run the app, confirm it is re-created next to the
working directory, and confirm a query per DAO method in `docs/UML.md` §3 succeeds.

**Common mistakes.** Committing `gym.db`; adding `ON DELETE CASCADE` "to be safe" (it silently
destroys the reference check that rule 4 depends on); indexing a `LIKE '%text%'` search column, where
the index can never be used.

### `theme/` — empty until the app owns a stylesheet (owned by [`theme/README.md`](theme/README.md))

**Status:** folder exists, `.gitkeep` only.
**Canonical:** `DECISIONS.md` 4.

**What it is.** The app-level stylesheet folder. It stays empty because the M3 theme is **generated at
runtime** by `M3Stylesheets.applyTo(scene, theme)` from an external library, not read from a static
`.css` file here.

**Why it is designed this way.** A checked-in `material-theme.css` would be a second copy of the
palette that nobody regenerates after a seed-colour change; the generated stylesheet cannot drift
because it *is* the theme.

**How to write it.** Add a file here only for app-specific layout/chrome rules that reference
`-md-sys-color-*` names. Switching light/dark or the seed colour is a runtime call with a new theme,
never a file swap. Examples and the counter-example are in [`theme/README.md`](theme/README.md).

**Common mistakes.** Creating `theme/material-theme.css` and loading it manually (dead file, drift);
writing a hex value in an app stylesheet; putting component-level rules here, which belong to the
library.

### `images/` — icon and illustration assets (owned by [`images/README.md`](images/README.md))

**Status:** folder exists, `.gitkeep` only.
**Canonical:** `docs/UML.md` §1 (`Equipment.category`, `Administrator.fullName` — the data an avatar or
category icon stands in for).

**What it is.** The logo, the placeholder avatar, equipment category icons and empty-state
illustrations, loaded as image resources.

**Why it is designed this way.** Images are read-only assets with a licence and a file size, so they
need naming and sizing rules of their own instead of being dropped anywhere in the tree.

**How to write it.** Icons that must be *rendered* come from the external library's icon catalog by name
(`M3Icon.symbol(...)`, Material Symbols path data — `DECISIONS.md` 9), because JavaFX's `Image` cannot
load `.svg`; an `.svg` kept here is a design source (and where the path data is copied from), while
anything the code loads is a PNG, optionally with an `@2x` variant, lower-case `kebab-case` names, one
licence note per non-original file. Details in [`images/README.md`](images/README.md).

**Common mistakes.** A 2 MB PNG for a 24 px icon; a logo whose background colour is baked in, so it
turns into a light rectangle in dark mode; an asset with no licence record.

### `fxml/main.fxml` — the m3fx showcase (testing only, to be replaced)

**Status:** exists (155 lines), loaded by `MainApp.start` today.
**Canonical:** `docs/UML.md` §5 (the real shell is one of the 16 screens; `main.fxml` is not).

**What it is.** A two-tab window that renders every component of the external M3 library so the team
can eyeball them (`M3Tabs`, `M3Button`, `M3SearchBar`, `M3Calendar`, `M3FabMenu`, …). Its own header
comment says it: *"Testing only: no `fx:controller`, no `onAction` handlers."*

**Why it is designed this way.** The library is being built in this repo, so a gallery is the fastest
way to see a styling regression. It is deliberately controller-free: adding a controller would tie a
throwaway file to production code.

**How to write it.** Do not extend it. When the real shell lands, `MainApp` loads the shell FXML, and
this file either moves to `../java/com/gym/M3Samples.java`'s test window (the git-ignored
`M3Samples` class already exists for that, see `build.gradle` → `runSamples`) or is deleted.

**How to know it is right.** `gradlew run` shows both tabs and nothing throws. It must never be the
file the shipped app opens.

**Common mistakes.** Adding a `fx:controller` to it and growing screen logic inside; treating its
`text="..."` literals as the text convention (they are showcase labels, not externalized strings);
leaving it as the app entry point after the nav shell is written.

### Resource naming conventions

| Artifact | Convention | Example | Defined in |
|---|---|---|---|
| FXML file | Controller name without the `Controller` suffix, one file per controller | `CustomerListController` ↔ `customer-list.fxml` | [`fxml/README.md`](fxml/README.md) — the exact spelling of the mapping is an **Open detail (decide + log in `DECISIONS.md`)** |
| FXML root path | Always loaded with a leading `/` | `getResource("/fxml/customer-list.fxml")` | this README §4, rule 1 |
| `fx:id` | Lower camelCase, matching the controller field name exactly | `fx:id="customerTable"` ↔ `@FXML private TableView<Customer> customerTable;` | [`fxml/README.md`](fxml/README.md) |
| `onAction` handler | `#` + the handler method name, no parentheses | `onAction="#onSearch"` | [`fxml/README.md`](fxml/README.md) |
| SQL table | `snake_case`, plural | `workout_programs`, `trainer_availability` | [`db/README.md`](db/README.md) |
| SQL index | `idx_<table>_<column>` | `idx_sessions_date` | [`db/README.md`](db/README.md) |
| Stylesheet (if ever added) | lower-case `kebab-case`, `.css` | `gym-app.css` | [`theme/README.md`](theme/README.md) |
| Image | lower-case `kebab-case`, prefix by kind | `logo-app.png`, `avatar-placeholder.png`, `icon-equipment-dumbbell.svg` | [`images/README.md`](images/README.md) |
| Resource-bundle key (planned) | `<screen>.<element>` | `login.title`, `customerList.search` | **Open detail (decide + log in `DECISIONS.md`)** |

## 6. How this layer connects to the others

The call flow, in the order it happens at startup:

1. `MainApp.start(Stage)` runs on the FX thread and calls `M3Stylesheets.applyTo(scene, theme)` — the
   theme is written as a stylesheet for this scene, so `-md-sys-color-*` names resolve
   (`docs/UML.md` §2, `DECISIONS.md` 4).
2. `MainApp` calls `getClass().getResource("/fxml/login.fxml")`; the classpath lookup returns a URL
   regardless of whether the app runs from `build/` or from a jar.
3. `FXMLLoader` parses the XML, instantiates `LoginController` because of its `fx:controller`
   attribute, and injects every `@FXML` field whose name matches an `fx:id`.
4. The controller's `initialize()` runs, then user actions call `onAction` handlers, which call
   services (`LoginController` → `AdminService.authenticate`), never DAOs.
5. A service asks a DAO; the DAO asks `DbConnection.getConnection()`; `SqliteDbConnection` opens
   `gym.db` next to the working directory — created on first use from `db/schema.sql`.
6. Errors travel back as a `DaoException` message string, shown by `FxUtils.error(...)`; nothing from
   this folder takes part in error handling.

| This folder | Depends on | Is used by |
|---|---|---|
| `fxml/` | `src/main/java/com/gym/controller/` (class names in `fx:controller`), the theme's colour names | `MainApp` and every screen switch in `FxUtils` |
| `db/` | `docs/UML.md` §1 entity fields, §3 DAO queries | `SqliteDbConnection.initializeSchema()` and every DAO |
| `theme/` | the external library's public theming API | `MainApp` only (theme is applied once per scene) |
| `images/` | nothing (assets are self-contained) | controllers that render a logo/avatar/icon |

**The seam.** Every file here is reached by *name* through the classpath, so the seam is the name.
Renaming a resource is a breaking change for the Java code that looks it up — grep for the old name
before you rename a file.

## 7. Common mistakes and review checklist

**Mistakes we have actually seen, or realistically expect:**

| Symptom | Cause | Fix |
|---|---|---|
| `NullPointerException` with no message, thrown from `FXMLLoader.load` | The `getResource` call returned `null` — usually a missing leading `/` or a misspelled file name | Print the URL before loading; fix the name so it matches the file on disk exactly |
| `javafx.fxml.LoadException: Class not found` for a class that exists | A missing `<?import ?>` line, or a typo in the package | Add the import; FXML has no wildcard imports |
| The window opens but one control stays empty | The `fx:id` does not match the `@FXML` field name, so the field stayed `null` | Compare the two names character by character; the mismatch is not reported |
| Dark mode leaves a light strip | A hardcoded colour or a baked-in image background | Replace it with an `-md-sys-color-*` reference or a transparent PNG |
| A teammate's demo data disappears | `gym.db` was committed by mistake | `git rm --cached gym.db`, confirm `.gitignore` covers `*.db` |
| The app writes a file into `build/resources/main` locally and fails in the jar | Runtime write into the classpath | Write to the working directory instead |

**Review checklist — run this before approving a PR that touches `src/main/resources`:**

- [ ] Every new resource is loaded with a leading `/` and the name matches the file exactly.
- [ ] No runtime write target inside this tree.
- [ ] No hex colour in FXML or CSS; styling goes through `styleClass` and theme lookups.
- [ ] No user-facing string left in a screen FXML without a `%key` (except the showcase).
- [ ] Each new FXML has a matching controller name and a matching `fx:controller`.
- [ ] Every `fx:id` in the FXML has a matching `@FXML` field, and every `onAction="#x"` names a real
      no-argument (or single `ActionEvent`) method.
- [ ] SQL changes go into `schema.sql` only; no data file is committed; no FK cascade was added.
- [ ] Any new image follows the naming and size rules and carries a licence note if it is not ours.
- [ ] The relevant sub-folder README (`fxml/`, `db/`, `theme/`, `images/`) was updated in the same PR.
- [ ] No `<!-- PART-MARKER -->` left behind (this README was written in parts).

## 8. Learning exercises (with answers)

1. **Read and answer.** `MainApp.java` line 13 uses `getResource("/fxml/main.fxml")`. What changes if
   the leading `/` is removed, and why is the resulting error message unhelpful?
2. **Write this small piece.** Add the two header lines and one `<?import ?>` needed for a file
   `fxml/login.fxml` whose controller is `com.gym.controller.LoginController` and whose root is a
   `VBox`.
3. **Find the bug.** A teammate adds `theme/material-theme.css` with `.root { -fx-background-color:
   #FFFBFE; }` and loads it from `MainApp`. Give two reasons this is wrong.
4. **Extend without breaking rule X.** You want `gym.db` to live in the user's home directory instead
   of the working directory. Which rule in §4 constrains you, and which class would you change?
5. **Decide and justify.** `main.fxml` holds its labels as literals. Name the smallest change that
   makes one screen ready for translation without touching any Java file.

<details>
<summary>Answers</summary>

1. Without the leading `/`, `getResource` resolves relative to the package of the calling class
   (`com/gym/`), so it looks for `com/gym/fxml/main.fxml`, finds nothing, and returns `null`. The
   `null` travels into `FXMLLoader`, which throws a `NullPointerException` that names neither the file
   nor the path — the stack trace points at the loader, not at the typo.
2.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.VBox?>
<VBox xmlns="http://javafx.com/javafx/21" xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="com.gym.controller.LoginController"/>
```

3. First, the theme is generated at runtime by `M3Stylesheets.applyTo(scene, theme)`
   (`DECISIONS.md` 4), so a static file is a second source of truth that no seed-colour change
   updates. Second, it hardcodes a hex value, so the light/dark switch cannot reach it — half the
   window stays light.
4. Rule 2 in §4 (*never write to a path inside this folder*) is not the constraint here, because the
   home directory is outside the classpath; rule 1 and the "runtime files go to the file system" half
   of rule 2 are what you keep. The class to change is `SqliteDbConnection`, which owns the database
   path — not a DAO and not a controller.
5. Replace one literal with a `%key` (`text="%login.title"`) and load the FXML with resources:
   `FXMLLoader.load(url, ResourceBundle.getBundle("..."))`. No Java change to the controller is
   needed; the bundle file name/location is the open detail to log in `DECISIONS.md`.

</details>

## 9. Ownership and tasks

| Task | Owner | Status |
|------|-------|--------|
| Skeleton + this README created | Abdelrhman | Done |
| `schema.sql` v1 + seed data (Week 1, foundation) | Abdelrhman | To Do |
| M3 theme wiring via `M3Stylesheets.applyTo(scene, theme)` + overlay layer install (Week 1, foundation) | Abdelrhman | To Do |
| Replace the `main.fxml` showcase with the real nav shell | Abdelrhman | To Do |
| FXML per owned screen (Weeks 2–5) | Each owner | To Do |
| Logo + placeholder images | Abdel Raouf | To Do |

Rules: no hardcoded colors in FXML/controllers — reference `-md-sys-color-*` theme lookups;
`gym.db` is git-ignored, only `schema.sql` is committed.

Note: the overlay layer for dialogs, snackbars and sheets lives in
`io.m3fx.controls.internal`, which the `m3fx` module does **not** export. `MainApp` needs a public
entry point from m3fx before those components can be used — either upstream a public
`M3Overlay`/`OverlayLayer` facade or reword the shell to use the exported `M3Dialog` API
(`DECISIONS.md` 35).

**`swing` branch rules.** This folder is the JavaFX half of the project, and the `swing` branch is a
second UI toolkit over the same domain. Port each finished piece per vertical slice instead of
batching it at the end, and keep the shared parts identical:

| Asset | On the `swing` branch |
|---|---|
| `db/schema.sql` | **Reuse unchanged.** The schema is toolkit-independent; both branches must create the same 13 tables or the two apps disagree about the same file. |
| `db` seed data | Reuse unchanged for the same reason as the schema. |
| `fxml/` | **Not portable.** Swing has no FXML; the branch rebuilds screens with Swing containers. Keep the naming convention (controller ↔ view) so the two branches stay readable side by side. |
| `theme/` | **Not portable.** The M3 theme is a JavaFX stylesheet; the Swing branch needs its own look-and-feel, but the *colour role names* should keep matching so screens are described the same way. |
| `images/` | Reuse unchanged — image files are toolkit-independent. |

That branch has **no `.java` files yet**, so none of the above has been tested end to end; treat the
table as the plan, not as history.

**Definition of done for a file in this folder:**

- [ ] The file loads at runtime (FXML opened, SQL executed, image rendered) — not just present on disk.
- [ ] The matching sub-folder README entry is updated in the same PR.
- [ ] No hardcoded colour, no business rule, no committed data file, no secret.
- [ ] Any non-obvious choice is logged in `docs/DECISIONS.md` with a number.
