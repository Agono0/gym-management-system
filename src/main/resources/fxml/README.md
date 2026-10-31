# `fxml` — Declarative screens: one file per controller (JavaFX branch)

> **Status note.** All 20 files in this folder exist and load (9 harness test classes verify them
> headlessly — see `TEST_READY.md`). What the screens do *not* have is a persistence backend: the
> controllers hold demo data, and sign-in is demo auth. `docs/UML.md` §5 remains the design source
> for controller behaviour; this README documents the **view**: the real inventory, the load flow,
> and how to write or extend a file.
>
> **Not documented here:** what a controller *does*. Behaviour, demo-data conventions and screen
> wiring live in `src/main/java/com/gym/controller/README.md`.

## 1. Purpose — the problem this folder solves

An FXML file is a **declarative** description of a screen: it says *what the screen contains*, in XML,
instead of *how to build it*, in Java. `FXMLLoader` reads that XML, creates the JavaFX nodes, creates the
controller named in `fx:controller`, injects the nodes the controller asked for, and returns the finished
node tree. FXML is the **View** of MVC; the controller is the Controller; the (planned) `model/` will be
the Model.

**What breaks if this folder is missing.** Every screen becomes a hand-written Java method that
constructs thirty nodes, sets padding on each and wires its own handlers. Layout and behaviour end up in
one file, so a spacing change and a business-rule change produce the same kind of diff; nobody can see a
screen without running it; and there is no file Scene Builder can open.

**What this folder deliberately does not do.** No SQL, no service calls, no business rule, no data
access. An FXML file cannot express rule 5 ("check-in requires an `ACTIVE` membership") and must not try:
it shows the check-in button, and the (planned) `AttendanceService` will decide whether the check-in is
allowed (`docs/UML.md` rules 1–19). It also stores no colours — the palette arrives from the theme at
runtime.

## 2. What you learn here (OOP concepts)

| Concept | Where in this folder | Why the project needs it | How to apply it (one line) |
|---|---|---|---|
| Separation of concerns | the whole folder | Structure in XML, behaviour in Java: each changes without the other | If a change needs a rule, it is not an FXML change |
| Composition | the node tree (containers in containers) | A screen is assembled from small parts (`VBox` → `HBox` → controls) | Nest a container when a group needs its own spacing or alignment |
| Information hiding | `fx:id`, `styleClass` | The view names the few nodes Java must touch and hides the other thirty | Give an `fx:id` only to nodes the controller reads |
| Reflection (framework-side) | `fx:controller`, `fx:id`, `onAction` | The loader instantiates and wires a class the XML names only as a string | Remember that reflection fails at runtime, never at compile time |
| Declarative vs imperative | the whole folder | The layout is data, so it can be diffed and reviewed without compiling | Prefer an attribute over a Java statement |
| Single responsibility | one file per screen | A screen lives in one file, so a bug has one place to be | Never let two screens share one FXML |
| Naming as contract | `fx:id` ↔ `@FXML` field, `onAction="#method"` | The link between XML and Java is a spelling, so both sides must match exactly | Copy the name; never retype it — and see `ControllerBindingContractTest`, which pins these counts |

## 3. Design patterns in this layer

| Pattern | Where | Why this pattern | What goes wrong if used carelessly |
|---|---|---|---|
| MVC (View half) | every `.fxml` file | The view is passive data: it renders what the controller puts into it | A view that computes turns into a second controller |
| Composite | the node tree (`StackPane`, `BorderPane`, `TableView`, …) | One uniform structure: a container holds any node, including another container | Over-nesting to fix spacing produces a tree nobody can reason about |
| Observer | `onAction="#handler"` and the event bubbling that reaches it | The control announces "something happened"; the controller reacts, and the control never knows the controller | Two handlers for one action makes the effect order-dependent |
| Factory (reflective) | `fx:controller="com.gym.controller.LoginController"` | The shell loads a screen by file name and gets a wired controller back | A controller without a public no-arg constructor cannot be built at all |
| Template Method | **rejected** | A `BaseController` would be near-empty: `initialize()` differs in every controller (`DECISIONS.md` 36) | Introducing one "to share code" hides which controller does what |
| Singleton | **avoided** | A screen is created per load; a cached static screen would keep stale rows | A static `Parent` would leak the previous user's data |

## 4. Rules you must follow (and the bug each rule prevents)

1. **One FXML file per controller, file name derived from the controller name.** The shipped
   convention is `CustomerListController` ↔ `customer-list.fxml` (kebab-case). Two spellings mean the
   shell asks for a file that does not exist and fails with a `NullPointerException` from `getResource`.
2. **`fx:controller` appears exactly once, on the root, holding the fully-qualified class name.** Two
   screens may not share a controller class: the loader creates a fresh controller per load, so a shared
   class would receive two different node sets into the same fields. (`main.fxml` is the one deliberate
   exception — it has no controller at all, see §5.)
3. **Every class you name needs its own `<?import ?>` line.** FXML has no wildcard imports. The loader
   fails at parse time with "Class not found" for a class that clearly exists — the real cause is the
   missing import. Import only what you use.
4. **Declare both namespaces on the root and use the `fx:` prefix for every FXML attribute.** JavaFX
   needs `xmlns="http://javafx.com/javafx/21"` (control names) and `xmlns:fx="http://javafx.com/fxml/1"`
   (FXML attributes). Without the `fx` binding, `fx:id` is an unbound prefix and the file fails to load.
5. **Keep the XML prolog and UTF-8.** `<?xml version="1.0" encoding="UTF-8"?>` on line 1.
6. **Give an `fx:id` to every node the controller reads, spelled exactly like the matching `@FXML`
   field.** Injection is by name through reflection: `fx:id="usernameField"` needs
   `@FXML private M3TextField usernameField;`. A typo does not fail the build — the field stays `null`
   and the first use throws a `NullPointerException`. `ControllerBindingContractTest` pins the
   `fx:id`/`@FXML` contract per screen so drift is caught by `./gradlew test`, not by a user.
7. **Shared objects such as a `ToggleGroup` are created once with `<fx:define>` and referenced by
   `$id`.** `main.fxml` lines 100–102 do exactly this: `<fx:define><ToggleGroup
   fx:id="planGroup"/></fx:define>` then `toggleGroup="$planGroup"` on the radio buttons. `$` means
   "the object defined under this `fx:id`". A second inline `ToggleGroup` leaves two independent
   groups, and the buttons stop behaving as one set.
8. **Wire events with `onAction="#methodName"`, and only to a handler FXML can call.** The handler takes
   no arguments or a single `ActionEvent`. Methods that need the selected row's id
   (`onOpenSelectedProfile()`, `onDeleteSelectedCustomer()`, …) are *no-argument here on purpose*: the
   controller reads the selection itself and acts on it. A load-time signature mismatch is the symptom
   of forgetting this.
9. **Text is currently literal; `%key` externalization is planned, not built.** No shipped screen uses
   a resource bundle — labels are literal English text ("Athlete Full Name"). There is no
   `.properties` file yet, so writing `text="%login.title"` today would render the literal `%key` on
   screen. Externalize in one coordinated change (bundle + loader + all files) when translation is
   actually needed.
10. **Style only with `styleClass` and theme lookups.** Add a class name to the node; never write
    `style="-fx-background-color: #6750A4"`. An inline style beats the theme's stylesheet, survives the
    light/dark switch, and produces one light panel in a dark window (`DECISIONS.md` 4). The
    `.title-*` / `.body-*` / `.label-*` typography classes in `m3fx.css` (backed by
    `io.m3fx.core.token.TypeScale`) are the shared type roles the app screens use.
11. **`disable`, `visible` and `text` may be bound in FXML; decisions may not.** Binding a node to a
    controller property for presentation is fine. A binding that needs a domain fact ("is this
    membership frozen?") must get that fact from a controller method — and, after the backend lands,
    from a service through the controller.
12. **Load on the FX thread, with a leading `/`.** `FXMLLoader.load(getClass().getResource(
    "/fxml/customer-list.fxml"))`. Loading builds nodes, which is FX-thread-only work; the leading slash
    means "classpath root".
13. **A controller-free file stays controller-free.** `main.fxml` is the FXML declaration artifact: no
    `fx:controller`, no handlers, and it must not grow either. Note that **no Java code loads
    `main.fxml` today** — the `MainApp` gallery is built in code; the file remains as the
    component-showcase XML and worked example.
14. **A password is never rendered by a plain text field.** The library ships no masked control (only
    `M3TextField`), so the password node is JavaFX's own `javafx.scene.control.PasswordField` with a
    `styleClass` so the theme can style it. Both `login.fxml` and the code-built launch gate follow
    this; the reveal toggle re-parents text, and no controller logs or stores the value.

## 5. The files in this folder

**Inventory — all 20 files, all load cleanly** (verified by `FxmlIntegrityTest` in `./gradlew test`):

| File | Lines | Root | Controller (`fx:controller`) | `fx:id`s | Shows |
|---|---|---|---|---|---|
| `main.fxml` | 154 | `M3Tabs` | — (deliberately none) | 1¹ | m3fx component showcase (declaration artifact) |
| `app-shell.fxml` | 179 | `StackPane` | `AppShellController` | 24 | The application shell: rail, app bar, content area |
| `login.fxml` | 146 | `StackPane` | `LoginController` | 25 | Glass authentication card |
| `dashboard.fxml` | 211 | `StackPane` | `DashboardController` | 23 | KPI cards, charts, end-shift/sign-out |
| `customer-list.fxml` | 151 | `StackPane` | `CustomerListController` | 34 | Athlete roster table + filters |
| `customer-profile.fxml` | 221 | `StackPane` | `CustomerProfileController` | 36 | Full member dossier |
| `medical-profile.fxml` | 182 | `StackPane` | `MedicalProfileController` | 26 | Medical & injury clearance form |
| `progress.fxml` | 197 | `StackPane` | `ProgressController` | 24 | Body-metric trends (VBT-style charts) |
| `trainer-list.fxml` | 138 | `VBox` | `TrainerListController` | 30 | Coach roster & workload |
| `trainer-profile.fxml` | 234 | `StackPane` | `TrainerProfileController` | 39 | Coach master profile & certifications |
| `availability.fxml` | 180 | `StackPane` | `AvailabilityController` | 31 | Coach availability matrix |
| `workout.fxml` | 159 | `StackPane` | `WorkoutController` | 13 | Workout routine builder |
| `live-workout-hud.fxml` | 182 | `StackPane` | `LiveWorkoutController` | 17 | Live workout telemetry HUD |
| `schedule.fxml` | 134 | `StackPane` | `ScheduleController` | 26 | Master schedule & booking |
| `equipment.fxml` | 164 | `StackPane` | `EquipmentController` | 23 | Equipment inventory |
| `gym-floor-map.fxml` | 197 | `StackPane` | `GymFloorMapController` | 14 | Facility blueprint map |
| `attendance.fxml` | 123 | `StackPane` | `AttendanceController` | 17 | Fast-lane attendance check-in |
| `membership.fxml` | 183 | `StackPane` | `MembershipController` | 14 | Membership plan tiers |
| `payment.fxml` | 157 | `StackPane` | `PaymentController` | 20 | POS & billing |
| `staff.fxml` | 121 | `StackPane` | `StaffController` | 12 | Staff RBAC & audit |

¹ `main.fxml`'s single `fx:id` (`planGroup`, line 101) names an `<fx:define>`d `ToggleGroup` that is
referenced via `$planGroup` — it injects nothing and backs no `@FXML` field.

`main.fxml` is not part of the app's screen count: the "19 screens" are `app-shell` + the 18 app
screens above. `TEST_READY.md` tracks the 19-screen test inventory; `main.fxml` sits outside it.

Four worked entries follow (the shell, the login gate's view, and the two newest screens). For the
remaining screens, the per-screen design sketches that used to live in this file described the
pre-implementation design and have been superseded by the built files — the real `fx:id` contracts
per screen are pinned by `ControllerBindingContractTest` and documented screen-by-screen in
`src/main/java/com/gym/controller/README.md`.

### `app-shell.fxml` — the application shell

**Status:** implemented. **Controller:** `com.gym.controller.AppShellController`. Loaded by
`GymApp.start` (`gradlew runGym`) and inspectable via the gallery's screen viewer.

**What it is.** `StackPane` root (`styleClass="app-shell-root"`) hosting the persistent chrome around
every screen: a `BorderPane` with the `M3TopAppBar` (search trigger, DB-sync `M3PulseBeacon`, theme
`M3Switch`, role `M3Chip`, logout `M3IconButton`, global `M3SplitButton`), the `M3NavigationRail`
with one `ToggleButton` per destination (`navDashboard` … `navHelp`), the `StackPane contentArea`
the shell swaps, the `M3CommandPalette`, and the global `StackPane globalSnackbar` for overlay
feedback. 24 `fx:id`s in total.

**Wiring.** `onAction="#onToggleRail"`, `#onOpenCommandPalette`, `#onToggleThemeMode`,
`#onOpenUserMenu` — all no-argument, wired in XML. Screen switches happen in the controller:
`loadScreen(String fxmlName)` / `openScreenViaRail(...)`. The nav-highlight pill and the Apple-style
page enter/exit motion are created in code by the controller (`DECISIONS.md` 69–70) — no `fx:id`,
no extra `@FXML` fields, so the binding contract stays stable.

**Verify.** `gradlew runGym` opens the shell maximized; `GymAppScreenViewerTest` and
`GymAppFullInteractiveAuditTest` drive the real shell headlessly; `AllScreensScrollHeightTest`
pins every screen's scroll height inside `contentArea`.

### `login.fxml` — the authentication screen

**Status:** implemented (was "planned" in older revisions of this file). **Controller:**
`com.gym.controller.LoginController`. Also loadable standalone via the gallery's "Launch Standalone
Login" button.

**What it is.** A `StackPane` root with a `M3ParticleCanvas` backdrop, `M3Scanline` and
`M3PulseBeacon` atmosphere, a `M3GyroRing3D` hero element, and an `M3GlassPane`-canopied `M3Card`
(`authCard`) holding the form. 25 `fx:id`s, including `usernameField` (`M3TextField`),
`passwordField` (**JavaFX `PasswordField`**, masked — rule 14), `passwordToggleBtn`
(reveal/hide), `capsLockAlert`, `rememberMeCheckBox`, `loginButton`, `loginSpinner`
(`M3CircularProgress`), `forgotPasswordButton`, and the `feedbackBox`/`feedbackIcon`/`feedbackLabel`
inline-feedback row inside `authSnackbar`.

**Wiring.** `onAction="#onTogglePasswordVisibility"`, `onAction="#onLogin"`,
`onAction="#onForgotPassword"` — all no-argument.

**Auth behaviour (demo, honestly labelled).** Any email plus a password of 4+ characters signs in;
validation failures answer with inline field errors in the feedback row — no dialogs, no backend
(`DECISIONS.md` 71). The full splash → login → shell morph choreography lives in the code-built
`LaunchGateController` (see `src/main/java/com/gym/README.md` §5), not in this file.

**Verify.** `FXMLLoader` loads it cleanly (`FxmlIntegrityTest`); `WiredFlowSmokeTest`
`.dashboardEndShiftLeadsBackToLoginGate()` proves the shell returns to this screen on sign-out.

### `live-workout-hud.fxml` — the live workout telemetry HUD

**Status:** implemented. **Controller:** `com.gym.controller.LiveWorkoutController`.

**What it is.** A `StackPane` assembling the m3fx workout package over an `M3GlassPane` canopy:
`M3RepCounter` (kinetic spring rep feedback), `M3HeartRateZones` (5-zone cardiac intensity),
`M3IntervalTimer` (HIIT/Tabata countdown), an `M3SplineChart` for the live trend, plus
`M3Button`/`M3IconButton` controls and an `M3Icon` glyph set. 17 `fx:id`s.

**Why it exists as a screen.** It exercises the Expressive workout controls in a realistic
composition — the same controls the gallery demos individually (`Live Workout HUD` page).

### `gym-floor-map.fxml` — the facility blueprint map

**Status:** implemented. **Controller:** `com.gym.controller.GymFloorMapController`.

**What it is.** A `StackPane` with the interactive `M3GymFloorMap` (zones, equipment occupancy
pins), an `M3ModelViewer3D` for the 1-click 3D equipment inspector, `M3LinearProgress` occupancy
gauges, `M3Chip` zone filters, `M3Card` summary tiles and `M3GlassPane` panels. 14 `fx:id`s.

**Verify.** Loads cleanly in the integrity suite; also previewable from the gallery's
"Gym Facility Blueprint Map" page, which demos `M3GymFloorMap` standalone.

### `main.fxml` — the FXML declaration artifact

**Status:** exists (154 lines). **No controller, no handlers — deliberately.** (Its one `fx:id`
names the `<fx:define>`d `ToggleGroup`; see the footnote above the table.)

**What it shows.** A two-tab window (`M3Tabs`, "Members" and "Schedule") declaring one of every
basic m3fx control — buttons, text fields, chips, icon buttons, a search bar, sliders, progress
indicators, pickers, FABs, a FAB menu — with the `<fx:define>`/`$planGroup` `ToggleGroup` pattern
at lines 100–102.

**Who loads it.** Nobody, today: the `MainApp` gallery is built in code, and the app screens load
through `GymApp`/`AppShellController`. The file stays as the smallest complete example of the
folder's conventions (header, imports, namespaces, `fx:define`) and as a parse fixture. Do not
grow it into a screen and do not add a controller (rule 13).

## 6. How this layer connects to the others

The real load flow for a screen, in order:

1. `GymApp.start(Stage)` resolves `/fxml/app-shell.fxml` and loads it once; the theme was already
   registered (`M3Stylesheets.setLatestTheme`) so controls resolve colors during inflation.
2. `AppShellController.loadScreen(fxmlName)` (or `openScreenViaRail(...)`) resolves each screen:
   `getResource("/fxml/<name>.fxml")`.
3. `FXMLLoader` parses the XML, resolves every `<?import ?>`, and instantiates the class named in
   `fx:controller` — public, no-arg constructor (`DECISIONS.md` 36).
4. The loader injects each node into the `@FXML` field whose name matches its `fx:id`, then calls
   `initialize()`, which populates the screen with its **demo data** (no services yet) and attaches
   listeners.
5. A user action runs an `onAction` handler, which mutates the demo data or opens another screen
   through the shell; feedback is inline or via the shell's global snackbar.
6. The gallery (`MainApp`) loads the same files independently in its screen viewer, and
   `src/test/java/com/gym/fxml/` loads all of them headlessly.

**Planned change when the backend lands:** step 5's handlers will call services, and step 4's
population will render service-returned entities — the FXML files themselves should not change.

| This folder | Depends on | Is used by |
|---|---|---|
| `fxml/*.fxml` (the 19 screens) | `src/main/java/com/gym/controller/` (class names in `fx:controller`), m3fx controls, the theme's colour names | `GymApp`, `AppShellController`, `MainApp`'s screen viewer, the test harness |
| `fxml/main.fxml` (artifact) | m3fx controls only | nothing at runtime; documentation + parse fixture |

**The seam.** The file name plus `fx:controller` is the whole contract between the shell and a
screen, so a screen can be renamed or replaced as long as those two stay in step.

## 7. Common mistakes and review checklist

| Symptom | Cause | Fix |
|---|---|---|
| `LoadException: Class not found: …` | Typo in `fx:controller`, or a missing `<?import ?>` for a control | Fix the spelling; add the import the message asks for |
| Window opens, one table stays empty, no error | `fx:id` and the `@FXML` field name differ | Compare the two names; the loader silently skips what it cannot match — and `ControllerBindingContractTest` now catches drift in CI |
| `LoadException: … does not have a method named onLogin()` | `onAction="#onLogin"` with no such method, or a method that takes parameters | Add the no-argument handler; keep id-taking methods out of `onAction` |
| Radio buttons in one group all stay selected | Two `ToggleGroup`s, one per button | Define one group with `<fx:define>` and reference it with `$id` |
| One light-coloured panel in dark mode | An inline `style=` colour or a hardcoded hex | Move it to `styleClass` and a `-md-sys-color-*` lookup |
| A label shows `%dashboard.activeMembers` literally | `%key` externalization used without a resource bundle | No bundle exists yet — use literal text (rule 9) until externalization lands |

**Review checklist — before approving a PR that adds or edits an FXML file:**

- [ ] File name follows the `Controller ↔ file` kebab-case convention.
- [ ] `fx:controller` names a real controller with a public no-arg constructor (or the file is
      deliberately controller-free like `main.fxml`).
- [ ] Every class used has an `<?import ?>`; no unused import left.
- [ ] `xmlns` and `xmlns:fx` on the root; prolog present; file is UTF-8.
- [ ] Every `fx:id` matches an `@FXML` field exactly; `ControllerBindingContractTest` updated if the
      count changed.
- [ ] Every `onAction="#x"` names a no-argument (or single `ActionEvent`) method.
- [ ] No business rule, no SQL and no service call inside the file.
- [ ] Literal text for now (rule 9); no colour literal; no inline `style=`.
- [ ] No password or hash is rendered anywhere; a password node is a masked `PasswordField` (rule 14).
- [ ] Shared objects use `<fx:define>` + `$id`.
- [ ] `./gradlew test` green (integrity, binding-contract and scroll-height tests all load the file).

## 8. Learning exercises (with answers)

1. **Read and answer.** Why does a typo in `fx:id` produce a `NullPointerException` in the controller
   rather than a compile error?
2. **Write this small piece.** Add an `<fx:define>`d `ToggleGroup` and attach two buttons to it, using
   `main.fxml` lines 100–102 as the model.
3. **Find the bug.** A screen has `onAction="#onDelete"` and the controller declares
   `public void onDelete(int equipmentId)`. What happens at load time, and what are the two fixes?
4. **Extend without breaking a rule.** You want the customer table to sort when the user clicks a
   column header. Where does that code go, and why can it not go in the XML?
5. **Decide and justify.** A teammate proposes a second FXML layout for `customer-profile.fxml` (tablet).
   What does that break, and what would you do instead?

<details>
<summary>Answers</summary>

1. Injection is by name through reflection. A name the loader cannot match is simply not injected and no
   exception is raised, so the field keeps its default `null`; the failure appears later, on first use,
   with a stack trace that names the controller line rather than the XML.
2.

```xml
<fx:define>
  <ToggleGroup fx:id="planGroup"/>
</fx:define>
<HBox spacing="16">
  <M3RadioButton text="Monthly" toggleGroup="$planGroup" selected="true"/>
  <M3RadioButton text="Yearly"  toggleGroup="$planGroup"/>
</HBox>
```

3. The loader looks for a method it can call for an `ActionEvent`, finds only a one-`int` method, and
   throws a `LoadException` naming the signature it wanted. Fix either by adding a no-argument handler
   (`onDeleteSelected()`) that reads the selected row and calls `onDelete(id)` — the pattern every
   shipped screen uses (`onOpenSelectedProfile`, `onDeleteSelectedCustomer`) — or by attaching the
   handler in `initialize()` from the button's `fx:id`.
4. In `initialize()`, on the column or the table (`sortOrder`, `setComparator`). A `Comparator` is a
   lambda, so it cannot be written in FXML — the same reason `cellValueFactory` lives in the controller.
5. It breaks rules 1 and 2 together: the loader creates a *new* controller per load, so two layouts mean
   two controllers, each holding only the fields of its own file, and every `fx:id` must be kept in step
   by hand. Keep one FXML and change the layout at runtime (swap container constraints in the
   controller), or let the shell show the same file in a wider container.

</details>

## 9. Ownership and tasks

| Task | Owner | Status |
|------|-------|--------|
| 19 screen FXMLs + `app-shell.fxml` | per-screen owners (see `controller/README.md`) | **Done** — all load; harness-verified |
| `main.fxml` declaration artifact | Abdelrhman | Done (frozen) |
| `%key` text externalization (bundle + loader) | Abdelrhman | Planned — not started |

Rules: no hardcoded colors in FXML — reference `-md-sys-color-*` theme lookups; one file per
controller, matching names; `main.fxml` stays controller-free.

**`swing` branch.** FXML does not exist there: Swing has no `FXMLLoader`, so each screen is rebuilt with
Swing containers. Keep the same file-per-screen granularity and the same controller ↔ view naming so the
two branches stay comparable.
