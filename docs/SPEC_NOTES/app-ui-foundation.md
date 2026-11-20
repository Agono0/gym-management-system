# SPEC NOTES — app-ui-foundation (task C0)

Scope: the new `com.gym.ui` layer (`UiKit`, `Data`, `DemoSeeder`) that the 19 Java-built
screens adopt incrementally, plus the adoption proof in `CustomerListController`.
Authoritative sources: `docs/DECISIONS.md` 77 (controller -> DAO directly, no services),
78 (DAO `validate(...)` owns every rule check), `docs/UML.md` rules 1-19,
`docs/agents/APP_MAP.md` (boilerplate audit: 40-line `findById` tree-walks + feedback
layers duplicated across ~8 controllers).

## 1. Why controller -> DAO with background Tasks (DECISIONS 77/78)

- The service layer is retired (DECISIONS 77.5). UI screens call the 13 concrete DAOs
  directly; every business rule (required fields, FK existence, rules 1-19) throws a checked
  `ValidationException`/`DatabaseException` from the DAO's `validate(...)` (DECISIONS 78).
- The UI therefore has exactly two jobs: (a) render entities and (b) surface the DAO's
  exception messages verbatim in snackbars/dialogs. No rule is ever re-implemented in
  `com.gym.ui` or `com.gym.controller` — when a rule changes, only the DAO changes.
- DAO calls are blocking JDBC with connection-per-operation (SQLite `SQLITE_BUSY` safety).
  On the JavaFX Application Thread that would freeze layout and trip the app's freeze
  watchdog, so every DAO call runs on a `javafx.concurrent.Task` and results are applied on
  the FX thread (`Data.loadAsync` / `Data.submit`).

## 2. `com.gym.ui.Data` contract

- `Data.ensureReady()` — one-time-per-JVM-run bootstrap: `DatabaseConnection.initializeDatabase()`
  (13-table DDL, `CREATE TABLE IF NOT EXISTS`) then `DemoSeeder.seedIfEmpty()`. `synchronized`
  so two screens loading concurrently cannot double-seed; the seeders themselves also guard on
  table emptiness, so the bootstrap is idempotent. Safe to call from a background `Task` (never
  the FX thread) as the first line of every screen's work supplier.
- `Data.loadAsync(Pane contentHost, ThrowingSupplier<T> work, Consumer<T> onSuccess,
  Consumer<Exception> onError)` — the standard screen-load pattern:
  1. Must be called on the FX thread (enforced with an `IllegalStateException` otherwise).
  2. Saves the current children of `contentHost` and swaps in `UiKit.loadingState()`.
  3. Runs `work` on a `Task` background thread; `work` may throw (checked exceptions are
     allowed via the `ThrowingSupplier` functional interface).
  4. On success/failure the helper **restores the original `contentHost` children** first
     (so the screen never loses its node tree), then invokes `onSuccess`/`onError` on the
     FX thread (`Task.setOnSucceeded`/`setOnFailed` are FX-thread callbacks per the
     `javafx.concurrent` contract; results are applied through them — no raw field access
     from the background thread, no extra `Platform.runLater` needed).
  5. A per-container generation counter (stored in `Node.getProperties()`) makes a stale
     task's callback a no-op when the user navigated away or re-loaded before it finished.
  6. `onError` receives `ValidationException`, `DatabaseException` or any `Exception`;
     the screen decides the answer, typically `UiKit.errorState(cause, retry)` where
     `retry` re-runs the screen's own load method.
- `Data.submit(ThrowingSupplier<T> work, Consumer<T> onSuccess, Consumer<Exception> onError)`
  — the same plumbing for **writes** (insert/update) without touching a container: run the
  DAO write on a `Task`, callback on the FX thread. Screens keep their layout; the answer
  goes to a snackbar/dialog.

## 3. `com.gym.ui.UiKit` contract

Static, Javadoc'd, zero SQL, zero business rules:

- `findById(Parent, String)` — the recursive id lookup copied across ~8 controllers
  (`Node.lookup("#id")` cannot cross unrelated sub-graphs and silently fails mid-tree).
- `installFeedback(Pane root)` — creates the screen's `M3Overlay`, adds it to the root so it
  overlays everything, returns it (replaces the per-controller `installFeedbackLayer` copies).
- `confirmDialog(M3Overlay, title, body, confirmLabel, cancelLabel, onConfirm)` — the
  destructive-action contract (M3 dialog, confirm is the default action) used by delete/cancel flows.
- `emptyState(message, actionLabel, action)` / `errorState(cause, retry)` / `loadingState()`
  — real-state node builders (anti-slop: no screen shows a silent blank pane). Icons come
  from the existing `M3Icons` catalog only (`group`, `info`, `refresh`); B3 expands the
  catalog, so no invented glyph names. `loadingState()` uses `M3CircularProgress`
  (indeterminate) because `M3LinearProgress`'s indeterminate timeline is currently broken —
  see BUGS FOUND in the C0 report (JavaFX `Interpolator.SPLINE` forbids control points
  outside [0,1]; the skin copies CSS cubic-bezier values verbatim).
- `formRow(String, Node)` — label + field row used by entry dialogs.
- `lookedUpColor(Node context, String cssVariable)` — resolves a looked-up `-md-sys-color-*`
  variable to a concrete `javafx.scene.paint.Color`. **The trick**: create a scratch `Region`
  with inline style `-fx-background-color: <var>`, attach it to the scene root, call
  `applyCss()` so the CSS engine resolves the looked-up color into a concrete `Background`
  fill, read that fill, then detach the probe. Requires a shown scene + FX thread; returns
  `null` (never a hardcoded color) when the context has no scene or the variable does not
  resolve. Screens that need a `Paint` for canvas/pseudo-node tinting call this instead of
  `Color.web("#...")`.

## 4. `com.gym.ui.DemoSeeder` contract

First-run demo dataset built **through the DAOs, not around them** (every insert passes the
DECISIONS 78 validation — that is the point of seeding through the rules):

- Reuses `com.gym.cli.SampleDataSeeder.seedIfEmpty()` for the base dataset (4 administrators
  incl. `admin`/ADMIN for rule 16, 3 trainers, 5 customers, memberships, medical profile,
  progress, programs, availability slots, subscription, sessions with one REQUESTED and one
  CONFIRMED (rule 1), equipment, attendance, payments incl. a voided+replacement pair
  (rules 12/19)). The CLI seeder has no JavaFX dependency, so calling it from `com.gym.ui`
  adds no layering violation; `com.gym.cli` stays the source of the base seed.
- `DemoSeeder.seedIfEmpty()` extends the base to the volume the GUI screens need:
  6 trainers total, ~20 customers total (Egyptian phones via `PhoneUtil` rule: 11 digits,
  `01[0125]` prefix), memberships across `PlanType` (incl. one ending within 7 days so the
  "Expiring" state is real), rule 14 consistency (SUSPENDED customers carry FROZEN
  memberships), rule 4 demo (the INACTIVE customer keeps a CANCELLED membership), medical
  profiles for some customers only (rule 3), progress records spread over the past weeks,
  sessions this week, attendance check-ins over the past 8 weeks (only where rule 5 passes:
  ACTIVE customer + unexpired ACTIVE membership, one row per day), and payments per rule 12
  (exactly one linked target, owner match, unique receipt numbers).
- Idempotency: base guard = administrators table empty (inside `SampleDataSeeder`); extension
  guard = existing customer phones (`findAll` set). Extension-dependent rows (memberships,
  medical, progress, attendance, sessions, payments) are only inserted for customers created
  in the same run, so a partially completed run resumes instead of duplicating.

## 5. Adoption proof — `CustomerListController`

- Roster, KPI strip, quick-peek and search suggestions come from real DAO data loaded with
  `Data.loadAsync` (`CustomerDao.findAll()` + `TrainerDao.findAll()` + `MembershipDao.findAll()`
  + `AttendanceDao.findAll()` in one background task). `loadingState()` while loading,
  `emptyState()` when the DB has no customers, `errorState()` with retry on failure.
- KPI cards are computed from the loaded rows (active/suspended/expiring<7d/joined this
  month) — no more "1,428 members" next to a 20-row table.
- Presentation-only derivations (documented, owned by C3 to revisit): display status
  "Expiring" = ACTIVE customer whose latest ACTIVE membership ends within 7 days;
  "8-Wk Activity %" = share of the last 8 ISO weeks (of 8) with at least one attendance
  check-in. These are UI metrics, not business rules.
- Add Member and the two-step delete are wired through `CustomerDao` via `Data.submit`
  (create renders the DAO `ValidationException`; delete implements rule 4 as a status change
  to INACTIVE with undo restoring the previous status). Freeze/renew/assign (rules 6/7/15)
  and the profile flows stay C3 scope.
- Everything else is untouched: search + chips, quick-peek side sheet, export-to-clipboard,
  help dialog, speed dial, `fx:id` parity ids, `ScreenController` contract and the
  `showMember` handoff to `CustomerProfileController`.

## 6. Shell responsiveness + real login (task C1, extends this note)

### 6.1 Width classes — reuse, not reinvention

The breakpoint authority is the library's `io.m3fx.controls.layout.WindowSizeClass.forWidth`
(compact < 600, medium 600-839, expanded >= 840; "large" is this shell's >= 1200 refinement
inside EXPANDED). `M3AdaptiveScaffold` does NOT fit this shell: it owns its own plain
rail/drawer/bar and would discard the hand-built rail (highlight pill springs, split button,
scroll pane, tooltips, manual collapse animation), so the shell listens to its own width and
maps each class onto the existing chrome:

- COMPACT (< 600): rail hidden (`visible`/`managed` false), a bottom `M3NavigationBar` shown,
  and the top-bar menu button opens the overflow menu. `M3NavigationBar` is specced for 3-5
  destinations (`iconBox` is a fixed 64dp wide, so 10 destinations cannot fit a 600dp window
  without clipped labels); the bar therefore carries the 5 primary destinations
  (Dashboard, Members, Trainers, Schedule, Plans) plus a "More" destination that opens an
  `M3Menu` with the remaining 5 (Equipment, Attendance, Finance, Staff & Admin, Documentation)
  - all 10 destinations stay reachable, and the component contract is respected.
- MEDIUM (600-839): rail shown, default collapsed (icon-only 80px, the existing `.collapsed`
  CSS class and spring animation).
- EXPANDED (840-1199): rail shown, default expanded (220px).
- Large (>= 1200): expanded plus a slightly wider workspace padding.
- Manual override: `onToggleRail` still flips collapsed/expanded and now records the override,
  which wins over the class default until the width class changes again (each class starts
  from its default - predictable, no sticky surprise states). At COMPACT there is no rail, so
  the toggle becomes the overflow menu.

`GymApp` lowers the stage min width 1024 -> 600 so the compact class is actually reachable in
the real app (the ScreenShooter renders it regardless).

### 6.2 Real login (rule 16)

`Data.authenticate(String username, String password)` is the headless-testable credential
check: `ensureReady()` first (bootstrap + seeding on the same background thread), then
`AdministratorDao.findByUsername` with the same semantics as the CLI's
`GymRulesHelper.authenticate` (Decision 78; stored `passwordHash` compared to the entered
password - the project keeps plaintext credentials by design). It returns an `AuthResult`
record with a status enum (OK / INVALID_CREDENTIALS / INACTIVE) plus the account's full name
and role - DB/bootstrap failures still throw `DatabaseException`/`ValidationException`.
`LaunchGateController.attemptSignIn` runs it via `Data.submit` (background thread, FX-thread
callback): success completes the existing morph choreography and hands the real identity to
`AppShellController.setSignedInUser` (user chip shows the signed-in name and role); wrong
credentials highlight the password field with short sentence-case copy; an inactive account
shows the rule 16 copy without blaming a field. The demo hint now states the real seeded
credentials (admin/admin123, manager/mgr123, staff/staff123, oldstaff/old123 refused) and the
gate signs in by username, matching the DAO lookup.
`src/test/java/com/gym/ui/DataAuthenticateTest.java` covers all outcomes headlessly against a
temp SQLite file (the root `test` task was already wired for JUnit 5, only NO-SOURCE).

### 6.3 Documentation destination

`onNavHelp` no longer navigates to GYM_FLOOR_MAP. It shows an `M3Dialog` with real content:
keyboard shortcuts (Ctrl+K command palette, arrow keys on the rail / bottom bar, Enter, Esc),
the seeded admin credentials, the app version, and a pointer to docs/UML.md rules 1-19.

## 7. Chart color semantics (task C2)

Charts are built in screen constructors before any scene attaches, so the CSS-based
`lookedUpColor` trick cannot run there; `UiKit.themeRole(ColorRole)` reads the role straight
from the latest applied theme (`M3Stylesheets.getLatestTheme()`, set by GymApp, the shell dark
toggle and the ScreenShooter before any screen builds; baseline-dark fallback — never a
hardcoded hex). The semantic mapping, applied inline at each call site:

| UI semantic | ColorRole | Why |
|---|---|---|
| success (operational, available, lean mass) | `TERTIARY` | M3's contrasting accent; lands in the green family under this TONAL_SPOT seed |
| error (occupied, live set, in-maintenance slices) | `ERROR` | the standard negative state role |
| warn / neutral accent (maintenance service, waitlist) | `SECONDARY` | distinct muted accent with no good/bad semantics |
| primary data series (VBT load points) | `PRIMARY` | the seed-purple family the whole UI uses |

Icon rule after C2: no emoji anywhere in the controllers — HUD/equipment buttons carry
`M3Icon.symbol` graphics (`stop`, `pause`, `refresh`, `check`, `play_arrow`, `edit`, `delete`,
`close`), the hand-copied visibility/lock SVG paths in the login surfaces are replaced by the
catalog's `visibility` / `visibility_off` / `lock`, and icon-only buttons carry tooltips
(`M3Tooltip`) plus accessible text.

## 8. Dashboard compact reflow (task C2)

The dashboard is a fixed-desktop bento; at 600dp its card rows cannot fit, and clipped
cards/buttons read as broken. The fix follows the C1 shell pattern (width-class swap of shared
nodes, `WindowSizeClass.forWidth`, deferred with `Platform.runLater` so `bentoGrid` children
are never mutated re-entrantly mid-layout):

- COMPACT: the hero stacks (greeting above the action bar, so no button is cut), the KPI strip
  swaps its HBox for a 2x2 `GridPane`, and the viz/diagnostics rows stack their cards
  full-width (`vizStack` / `diagnosticsStack`).
- MEDIUM/EXPANDED: the original rows (byte-identical 1280 layout).
- Both layouts share the SAME nodes (cards move between containers, constraints persist on the
  child), so state and handlers are untouched; `applyDashboardLayout` is idempotent.
- Deliberately rejected: width-bound ScrollPane wrappers — measured evidence showed binding a
  row's pref width to the scroll pane corrupts the HBox's computed pref height (716px for a
  ~110px row), collapsing the rows at every width.

## 9. Dashboard + profile real data (task C3)

### 9.1 Dashboard

One `Data.loadAsync` snapshot per view: `buildSnapshot()` runs off the FX thread (bootstrap +
Customer/Membership/Payment/Session/Trainer/Attendance/Equipment/TrainerAvailability queries)
and returns an immutable `DashboardSnapshot` record; `applySnapshot` is the only FX-touching
step. Figures: ACTIVE members of the roster, today's non-voided revenue (rules 6/19), today's
session list with per-slot capacity (rule 17: CONFIRMED-only occupancy against the slot cap;
ad-hoc sessions show "-"), equipment availability, ACTIVE plan distribution, the hourly
check-in load for today against the trailing-14-day hourly baseline, 14-day attendance/revenue
sparklines, and live occupancy ("inside" = checked in today with no check-out). REFRESH
re-queries the database; the demo `Random` jitter is deleted. QUICK CHECK-IN lists the rule-5
eligible members (ACTIVE customer + unexpired ACTIVE membership at snapshot time) and runs the
check-in through `AttendanceDao`: a same-day re-check-in UPDATES the existing row (rule 5)
rather than inserting a duplicate; every answer re-queries the snapshot. The session agenda's
row action confirms a REQUESTED session through `SessionDao.update` (rule 18 owned by the DAO).

### 9.2 Customer profile

`showMember` (the roster handoff) parses the "M-0012"-style id and loads the dossier via
`Data.loadAsync` (`ProfileSnapshot`: customer, latest membership by end date, all biometric
readings, assigned coach, medical profile). Standalone previews without a handoff show the
designed empty state (no demo person). Actions, all via `Data.submit` with the DAO's
`ValidationException` surfaced verbatim: save -> `CustomerDao.update`; freeze -> status FROZEN
with the stored day counter (rule 7, ACTIVE-only); unfreeze -> end date shifted by the frozen
days, counter cleared (rule 7 arithmetic mirrors the CLI's `GymRulesHelper`); renew -> end date
extended by the plan window from today, CANCELLED refused (rule 7); coach change/unassign ->
`CustomerDao.update` (rule 15); biometric log -> `ProgressRecordDao.insert` with the target
weight carried over. Program assign/view uses `WorkoutProgramDao` (version continues the
customer's sequence; the record names its creator coach, so assigning requires a coach first).
The VBT 1RM card stays an honest dash — no data source records it yet.

## 10. Schedule + live workout on real sessions (task C4)

### 10.1 Schedule

One `Data.loadAsync` week snapshot per view (`SessionDao` + `TrainerAvailabilityDao` +
customers + coaches, filtered to the rendered Monday-Sunday) rendered into the full 7-column
agenda matrix; week navigation re-queries with real date math. The snapshot records carry the
resolved display strings plus per-slot rule-17 data (CONFIRMED-only occupancy vs the slot cap);
ad-hoc sessions (no slot) show "-" capacity. Every lifecycle action goes through
`Data.submit` -> `SessionDao.update` with the DAO owning rule 18 (transitions) and the rule-17
full-slot refusal mirrored from the CLI's `GymRulesHelper.confirmSession`. Cancel mirrors the
CLI's rule-10 logic: reason required, >= 2h notice is a free cancellation, later cancels are
still CANCELLED with the reason recorded. Booking follows rule 1 via the Book Slot menu:
inside a published OPEN slot -> CONFIRMED with the slot attached (cap-checked), outside the
slots -> REQUESTED, plus an admin-override path (CONFIRMED, ignores slot rules; RBAC wiring of
*who* is admin stays with C1's login/C7's staff screen). Default duration 60 minutes. The
filter chips operate on the card status tags. "Run Live" hands the selected CONFIRMED session
to the HUD via the shell's `openScreenViaRail` pathway.

### 10.2 Live workout HUD

`startSession(sessionId)` is the handoff entry (schedule or the automatic pick of the first
CONFIRMED session from today onward); the HUD reads the session row and the athlete name from
the database. COMPLETE SESSION = rule 18 CONFIRMED -> COMPLETED via `SessionDao.update`;
ABORT SESSION = CONFIRMED -> CANCELLED with the reason recorded (rule 18 + the rule-10 notice
copy); both drive a terminal HUD state (clocks paused, lifecycle buttons disabled). Skip Rest
restarts the interval countdown from the top; RE-CALIBRATE re-reads the session row and
athlete name and re-seeds the instruments. Rep counter + and - both work. The velocity/BPM/
cadence telemetry stays SIMULATED and carries a visible "SIMULATED TELEMETRY" badge — no fake
sensor claims. Rule-18 note: the DAO treats a same-status save as a no-op update, not a
transition, so a re-save of an unchanged session is legal.
## 11. Equipment screen on real data (task C5)

- **Ledger**: the stock `TableView<String>` (and its fake "Showing 12 of 84 equipment items"
  footer) is replaced by `M3DataTable<Equipment>` bound to `EquipmentDao.findAll()` via
  `Data.loadAsync`. Search, sorting and pagination come from the table itself — the former
  app-bar `searchField` fx:id is retired with the stock table (the table ships its own
  search; nothing looks up `#searchField` in CSS/tests). The stock TableView column ids
  (`colId`…`colLastMaintenance`) are retired for the same reason. The table stays mounted in
  a dedicated `ledgerStateHost` through every load; empty (`UiKit.emptyState` with an Add
  action) and error (`UiKit.errorState` with retry) states render in a sibling overlay so the
  loadAsync restore snapshot never loses the table.
- **Category filter chips**: "All Assets" + the four real seeded categories (CARDIO,
  STRENGTH, FREE WEIGHTS, RECOVERY) in a `ToggleGroup` with re-select-on-deselect radio
  semantics; filtering rewrites the table's data model (`getItems()`), so the table's own
  search still applies inside the filtered set. A category invented through the Add dialog
  simply shows under "All Assets" (the chips are static; documented, not dynamic).
- **Add Equipment**: `M3Dialog.basic` + `UiKit.formRow` (name, category with the seeded
  values as guidance, optional purchase date) → `EquipmentDao.insert` via `Data.submit`;
  inline mirrors set the field `error` states, a DAO `ValidationException` surfaces verbatim
  in a snackbar and the dialog stays open (C3 contract). New equipment starts
  NEW/AVAILABLE; `lastMaintenance` starts empty.
- **Condition/status inspector chips**: the stock RadioButtons became `M3Chip` FILTER
  groups in `ToggleGroup`s (radio semantics: one selected, clicking the selected one
  re-selects). Each change is one `EquipmentDao.update` through `Data.submit`; failures
  surface verbatim and the chips re-derive from the in-memory row (which still holds the
  pre-write state). The former radio fx:ids (`conditionNewRadio`…) live on the chips.
- **Log Today's Maintenance (rule 13 reading)**: one atomic `EquipmentDao.update` setting
  status `IN_MAINTENANCE` **and** `lastMaintenance = today` together. Rule 13's principle
  ("derived state is never two independent hand edits") applied to the pair: the UI action
  changes both fields as one workflow; there is no separate "set last maintained" control.
- **Delete**: the Equipment model has no INACTIVE-like status (unlike Customer, rule 4's
  soft-delete target), and `EquipmentDao.delete(int)` is a real hard delete — so Delete is
  the DAO's hard delete behind the `UiKit.confirmDialog` contract, with the dialog body
  pointing at status RETIRED as the non-destructive alternative. A DAO/schema failure
  (references) surfaces verbatim.
- **Export**: writes a real CSV (`build/exports/equipment-ledger-<stamp>.csv`, RFC-style
  escaping) via `Data.submit`; the snackbar announces the absolute path. The button label
  changed from the fake "(PDF)" to "(CSV)".
- **3D twin**: presentational; binds the selected row's name and maps the category to a
  display model (CARDIO→TREADMILL, STRENGTH→SQUAT_RACK, FREE WEIGHTS→DUMBBELL,
  RECOVERY→KETTLEBELL, anything else → WORKOUT_BENCH).

## 12. Gym floor map on real data (task C5)

- **Header chip**: "Inside now: N | Sessions today: M (K confirmed)" — N = AttendanceDao
  rows for today with an empty checkOut (people currently inside), M/K = SessionDao rows
  for today and their CONFIRMED subset. The fake "64 / 120 (53%)" is gone.
- **Zone→category mapping** (documented choice): Cardio Deck↔CARDIO, Free Weights
  Arena↔FREE WEIGHTS, Olympic Platforms↔STRENGTH, Functional Turf↔RECOVERY — 1:1 with the
  seeded categories. **Data-source limitation (honest)**: Session and Attendance carry no
  zone/equipment link, so people-per-zone cannot be derived; the zone cards instead show
  **equipment readiness** per zone ("n of m operational", ratio = share not RETIRED and not
  IN_MAINTENANCE). A zone whose category has no rows shows "No equipment data / No <category>
  rows in the ledger" — never a random percentage. Wiring sessions/attendance to zones needs
  a schema change (flagged for a future task, not invented here).
- **Layer toggles**: "Machine Pins" hides/shows the map's equipment pins by clearing and
  restoring `M3GymFloorMap.getEquipmentList()`; because the component redraws on
  scene/size/mouse events but not on list changes, the controller re-attaches the map node to
  force a redraw (public-API workaround). "Occupancy Heatmap", "Zone Boundaries" and "IoT
  Sensors" are **disabled with explanatory tooltips**: `M3GymFloorMap` exposes no such layer
  toggles (component gap → lane-B follow-up).
- **Place Equipment Pin**: disabled with the explanatory tooltip — the Equipment model has
  no floor-location fields, so there is nothing to place (no fake pin placement). The map's
  built-in demo pins remain component-owned demo content.
- **Inspection card**: "Mark Maintenance" flips the inspected pin into the map's MAINTENANCE
  state (map-local, presentational — the pins are the component's demo nodes, not DB rows);
  "Full Asset Page" navigates through the shell's rail-key handoff
  (`AppShellController.openScreenViaRail("navEquipment", EQUIPMENT)`) with a snackbar
  fallback for standalone previews.
