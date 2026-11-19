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
