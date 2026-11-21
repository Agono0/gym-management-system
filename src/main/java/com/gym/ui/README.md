# `ui` — shared real-data layer for the Java-built screens (Milestone 3)

> Added per `docs/DECISIONS.md` 80-82 (tasks C0-C10). Three classes own everything the 19 screen
> controllers share when talking to the DAO layer — so no screen keeps its own copy of the
> threading contract, the state builders or the seed. The per-screen pattern and the state
> policy below are binding for new screens; the controller-layer README documents how each
> screen applies them.

## 1. What each class owns

| Class | Owns | Must never do |
|---|---|---|
| [`Data`](Data.java) | `ensureReady()` (schema bootstrap + demo seed), `loadAsync` (screen loads), `submit` (writes), `authenticate` (rule-16 login returning an `AuthResult`) | Touch the scene graph; hold SQL; decide business rules |
| [`UiKit`](UiKit.java) | `findById`, feedback-overlay install, confirm-dialog factory, loading/empty/error builders, form rows, `lookedUpColor`, `themeRole(ColorRole)` | Hold SQL; decide business rules; hardcode hex colors |
| [`DemoSeeder`](DemoSeeder.java) | First-run demo dataset through the DAOs (every row passes the 78 validation) | Insert rows that bypass a DAO |

## 2. The threading contract (the reason `Data` exists)

Every DAO call opens a JDBC connection and blocks on SQLite I/O, so no DAO call may run on the
JavaFX Application Thread. The contract, per screen:

1. `buildView()` builds static structure with honest placeholders (`-`, "No X loaded").
2. `loadX()` calls `Data.loadAsync(contentHost, work, onSuccess, onError)` — the content host
   swaps to `UiKit.loadingState()`, `work` runs off-thread (queries + immutable snapshot record),
   and the callbacks land on the FX thread with the original content restored.
3. Actions call `Data.submit(...)` — same contract without a content host; the success path
   re-queries (the screen re-loads its snapshot instead of patching rows in place).
4. `Data.ensureReady()` runs once inside the first load's `work` (bootstrap + idempotent seed).
5. Snapshot records (immutable `record`s with display-ready values) are built off-thread so the
   FX callback is a pure render step. Worker threads are non-daemon on purpose: JVM exit must
   wait for in-flight database work.

## 3. State policy (anti-slop)

- Every content region implements the three real states: loading (`UiKit.loadingState()`),
  error with retry (`UiKit.errorState(cause, retry)` — the DAO message rendered verbatim), and
  empty (`UiKit.emptyState(...)` or explicit copy) — a silent blank pane is a defect.
- Confirm dialogs back every destructive action (cancel/void/delete), with rule copy.
- Async answers surface the DAO's `ValidationException` message verbatim; the UI adds no
  business judgment.
- Feedback overlays attach to the scene root on attach and detach on screen swap, deferred with
  `Platform.runLater` so the children list is never mutated re-entrantly (see
  `ScheduleController.installFeedbackLayer` for the canonical pattern).

## 4. What breaks if this layer is misused

- DAO calls on the FX thread freeze layout and trip the freeze watchdog.
- Scene-graph access from a background thread crashes the toolkit.
- Business rules re-implemented in a screen drift from the DAOs (the single source of truth per
  `DECISIONS.md` 77/78).
- Trusting pre-CSS `prefHeight` values, or binding container sizes to pre-CSS content, produces
  collapsed layouts (measured: 716px for a 110px row) — container sizes come from laid-out nodes
  only.
- Sharing nodes between layout variants: fill the compact variant's containers only when the
  width-class swap fires (the C3 shared-node lesson) — filling them at construction drains the
  desktop variant's rows.
