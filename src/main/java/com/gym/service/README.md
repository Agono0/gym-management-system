# `service` — Business Logic (JavaFX branch)

> ## ⚠️ PLANNED, NOT BUILT
> **This folder contains no `.java` files — only this README.** The 11 services below are the
> agreed design in [`docs/UML.md`](../../../../docs/UML.md) §4, and the 19 business rules they must
> implement identically are in `docs/UML.md` → "Business rules". Today the screens answer with demo
> data and inline demo feedback; no service class exists. Status:
> [`docs/COVERAGE.md`](../../../../docs/COVERAGE.md). Do not cite a service as if it compiles.

## 1. Purpose — the problem this layer solves

A service will be **one use case's worth of business logic**: `bookOpenSlot` is a use case, `freeze`
is a use case, `revenueBetween` is a use case. It loads what it needs through DAO interfaces, applies
the rule, writes the result back, and returns data or throws. It is the only layer allowed to
*decide* anything.

**What breaks if this layer never exists.** The rule lives in the controller — which means in *each*
controller that needs it. `checkIn` needs "ACTIVE customer + ACTIVE, unexpired membership" (rule 5).
Put that in `AttendanceController` and the same three lines get copy-pasted into
`DashboardController`; the copies drift, one forgets the `endDate` comparison, and a suspended member
walks into the gym. No copy is testable without starting JavaFX.

**What this layer deliberately does not do.**

| Not here | Where it lives |
|---|---|
| SQL, JDBC, `Connection`, `PreparedStatement` | `dao/` (planned) |
| FXML, dialogs, scene switching, `ObservableList` | `controller/` (shipped) |
| JavaFX of any kind, including `Platform.runLater` | nobody — a service must not import it |
| Display text, currency symbols, `dd/MM/yyyy` formatting | the controller |
| Entity field definitions | `model/` (planned), `docs/UML.md` §1 |

## 2. What you learn here (OOP concepts)

| Concept | Where it will be practised | Why the project needs it | How to apply it (one line) |
|---|---|---|---|
| **Single responsibility** | 11 services, one use-case cluster each | A change in the freeze rule must not touch booking code | Name the service after the aggregate it owns |
| **Stateless class design** | every service: `private final` collaborators only | Two screens share one instance; a cached `Customer` field would go stale mid-shift | Keep every field `final` |
| **Encapsulation** | services expose methods, never their DAOs | A controller that can reach `CustomerDao` will write SQL-shaped code | No getter for a DAO |
| **Abstraction / dependency inversion** | DAO *interfaces* as field types | The service states *what* it stores, not *how* (`DECISIONS.md` 33) | The composition root builds impls; services receive interfaces |
| **Inheritance — absent on purpose** | no `BaseService` (`UML.md` §7) | `validate` exists on one service, so the base would be empty | Eleven flat classes read better than one hollow parent |
| **Enums as a type system** | `switch` over `PlanType`, `SessionStatus` | `canTransition` becomes a compile-time-checked table | Use exhaustive `switch` expressions |
| **Derived vs stored state** | rule 13: `FULL`, both `EXPIRED` | A stored `FULL` goes stale the moment a session is cancelled | Compute in the read path |
| **Value objects / immutability** | `BigDecimal` money, ISO date strings | A charged `price` is history (rule 19) | Correct by void + re-record, never by edit |
| **Exception design** | `IllegalArgumentException`/`IllegalStateException` for rules; `DaoException` propagates | A raw `SQLException` cannot be acted on by a user | Throw with a message a receptionist can read |
| **Fail-fast vs validation-as-a-list** | `validate` → `List<String>`; single actions throw | A form must mark three empty fields at once; `freeze` must refuse immediately | Lists for forms, throws for actions |
| **Functional Java (streams)** | `ProgressService.series`, conflict filtering | Mapping records to one metric without index bookkeeping | `records.stream().map(...).toList()` |
| **Defensive programming** | guard clauses at the top of every method | An unknown id must fail before a second read | Order guards: null/blank → load → existence → state → cross-aggregate |

## 3. Design patterns in this layer

| Pattern | Where | Why | What goes wrong if used carelessly |
|---|---|---|---|
| **Service layer / use-case method** | every public method | One method = one business action, so each rule has a home | The CRUD pass-through smell: a service method that only forwards to a DAO adds a layer of nothing |
| **Constructor dependency injection** | all 11 services | Collaborators are explicit, final, test-replaceable | `new CustomerDaoImpl(db)` inside a service hard-wires the database |
| **Facade over several DAOs** | `SchedulingService` (4 DAOs), `MembershipService` (2) | One call hides a multi-source rule | A `bookEverything()` façade becomes untestable and untraceable |
| **Guard clause / fail-fast** | top of `checkIn`, `bookOpenSlot`, `record` | The failure is reported where it happened | Mixed-order guards produce confusing "not found" errors |
| **State machine over an enum** | `SchedulingService.canTransition` (rule 18) | One authority for legal `SessionStatus` moves | Transitions scattered across methods drift; a `NO_SHOW` becomes reversible |
| **Cross-aggregate access through the owning service** | `TrainerService` → `MedicalProfileService.getForTrainer` (rule 15) | One writer per table; access stays auditable in one method | `TrainerService` reaching into `MedicalProfileDao` |
| **Static utility** | `Validators`, `DateUtils`, `GuardService`, `ChartUtils` (planned in `util/`) | Cross-cutting, entity-free, no state | Putting a *rule* in a utility: expiry is a rule, `addMonths` is arithmetic |
| **Template Method / Singleton — rejected** | no base service, no `getInstance()` | Shared nothing between the eleven services | An empty base class costs a jump-to-definition everywhere |

`SessionContext` (planned in `util/`) is the one documented piece of static mutable state
(`DECISIONS.md` 34) — it is not a service, and rule 16's role check is applied at the screen.

## 4. Rules you must follow (and the bug each rule prevents)

1. **A service is stateless: every field is `private final`, constructor-injected.** No `static`
   mutable fields at all.
2. **Services never import JavaFX** — not even `Platform.runLater`. This is what keeps the `swing`
   port and the unit tests possible.
3. **The 19 rules in `docs/UML.md` are implemented exactly once, here.** Quote the rule number in the
   Javadoc of the method that owns it; a rule implemented twice has already drifted.
4. **Money is `BigDecimal` (`String` constructor, `compareTo`)** and dates are ISO strings parsed by
   `DateUtils` (`DECISIONS.md` 31, rule 9).
5. **Throw user-readable rule violations** (`IllegalArgumentException` with a receptionist-grade
   message); let `DaoException` propagate — never catch-and-ignore.
6. **Derived state (rule 13) is computed on read**, never written.
7. **Cross-aggregate reads go through the owning service** (rule 15) — never into another aggregate's
   DAO.
8. **No CRUD pass-through methods.** If a service method only forwards to a DAO, delete the method
   and let the caller use the DAO — or find the rule that belongs there.

## 5. How this layer connects to the others

| Will depend on | Will be used by |
|---|---|
| `dao/` interfaces, `model/` entities, `util/` helpers (all planned) | `controller/` (shipped, 20 controllers — their demo-data paths specify what each screen needs); the shell's screens call services directly, never DAOs |

When the backend starts: implement one service per use-case cluster in `UML.md` §4 order, owning its
rules 1–19 slice, and swap the controllers' demo data for service calls screen by screen (the
`WiredFlowSmokeTest` flows are the regression net). `SessionContext.requireRole` (planned) gates
`StaffController` and every override action (rule 16).

## 6. Learning exercises

1. `checkIn` (rule 5) — write the guard clauses in the documented order and say what each prevents.
2. Two controllers both need "expiring memberships this month". Where does the query live, and what
   is the method name? Why is a controller-side copy a bug?
3. Why must a service never import JavaFX even "just for `Platform.runLater`"? Give the two concrete
   costs.
4. A teammate caches a `List<Membership>` in a static field "for performance". Which rule does this
   break and what user-visible bug results?
5. `freeze` then `unfreeze` (rule 7) — narrate the field changes (`frozenDays`, `endDate`) and name
   the state a `CANCELLED` membership must refuse.

## 7. Ownership

| Planned file group | Owner |
|---|---|
| 11 services (per `docs/UML_TEAM.md` split) | per-feature owners (see `docs/UML_TEAM.md`) |

Status: **planned, not built** — track in `docs/COVERAGE.md` → "Backend domain services and SQLite DAOs".
