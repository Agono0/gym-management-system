# `service` — Business Logic (JavaFX branch)

> **Signature authority:** `docs/UML.md` §4 owns every name, type and signature in this folder.
> This README explains *why* each one exists and *how* to write it. Code here is an illustrative
> sketch, not the specification — if a sketch and `UML.md` disagree, `UML.md` wins.

This layer holds **the 19 business rules** (`docs/UML.md` → "Business rules"). Everything else in the
project is plumbing: entities carry data, DAOs move it, controllers draw it. If a rule is enforced in
two places, or in the wrong place, the rule is not enforced.

Read order: this section, then §4 (rules), then §5 (the eleven entries, in the order given there).
Cross-layer details live in [`../model/README.md`](../model/README.md),
[`../dao/README.md`](../dao/README.md), [`../util/README.md`](../util/README.md) and
[`../controller/README.md`](../controller/README.md) — this file links them instead of repeating them.

---

## 1. Purpose — the problem this layer solves

A service is **one use case's worth of business logic**: `bookOpenSlot` is a use case, `freeze` is a
use case, `revenueBetween` is a use case. It loads what it needs through DAO interfaces, applies the
rule, writes the result back, and returns data or throws. It is the only layer allowed to *decide*
anything.

**What breaks if this layer is missing.** Without it the rule lives in the controller, which means it
lives in *each* controller that needs it. `checkIn` needs "ACTIVE customer + ACTIVE, unexpired
membership" (rule 5). Put that in `AttendanceController` and the same three lines get copy-pasted
into `DashboardController` for the quick-check-in button. The two copies drift, one forgets the
`endDate` comparison, and a suspended member walks into the gym. Worse: no copy is testable without
starting JavaFX, so nobody ever tests it.

**What this layer deliberately does not do.**

| Not here | Where it lives |
|---|---|
| SQL, JDBC, `Connection`, `PreparedStatement` | [`../dao/README.md`](../dao/README.md) |
| FXML, dialogs, `FxUtils`, scene switching, `ObservableList` | [`../controller/README.md`](../controller/README.md) |
| JavaFX of any kind, including `Platform.runLater` | nobody — a service must not import it |
| Display text, currency symbols, `dd/MM/yyyy` formatting | the controller, or `ChartUtils` for chart data |
| Entity field definitions | [`../model/README.md`](../model/README.md) (`docs/UML.md` §1) |

---

## 2. What you learn here (OOP concepts)

Concept names match `docs/LEARNING_GUIDE.md` → "OOP concept index".

| Concept | Where in this folder | Why the project needs it | How to apply it (one line) |
|---|---|---|---|
| **Single responsibility** | 11 services, one use-case cluster each | A change in the freeze rule must not require touching booking code | Name the service after the aggregate it owns, not after the screen that uses it |
| **Stateless class design** | every service: `private final` collaborators only | Two screens share one service instance; a cached `Customer` field would go stale mid-shift | Keep every field `final`; put per-call values in local variables |
| **Encapsulation / information hiding** | services expose methods, never their DAOs | A controller that can reach `CustomerDao` will eventually write SQL-shaped code | Keep DAO fields `private`; there is no getter for a DAO |
| **Abstraction: interface as a contract** | `CustomerDao` (not `CustomerDaoImpl`) is the field type | The service states *what* it stores, not *how* — `DECISIONS.md` 33 | Type every collaborator field as the interface |
| **Polymorphism / dynamic dispatch** | `dao.save(customer)` resolves to the impl at runtime | SQLite today, a fake in a unit test, Postgres later | Never `instanceof` a DAO; call the interface method |
| **Dependency inversion** | DAOs and `DbConnection` interfaces sit above the implementations | The two volatile things (database, UI toolkit) stay behind seams | Let the composition root build the impls; services only receive interfaces |
| **Composition over inheritance** | services *hold* DAOs; nothing extends anything | A collaborator can be replaced; a superclass cannot | Add a constructor parameter, not a base class |
| **Inheritance — absent on purpose** | no `BaseService` | `validate` exists on one service, so the base would be empty — `UML.md` §7, `DECISIONS.md` 36 | Resist the reflex; eleven flat classes read better than one hollow parent |
| **Enums as a type system** | `switch` over `PlanType`, `SessionStatus`, `ProgressMetric` | `canTransition` is a compile-time-checked table, not string comparison | Use exhaustive `switch` *expressions* so a new literal is a compile error |
| **Derived vs stored state** | rule 13: `FULL`, `EXPIRED` recomputed on read | A stored `FULL` goes stale the moment a session is cancelled | Compute it in the read path; never `setStatus(FULL)` |
| **Value objects / immutability** | `BigDecimal` money, ISO-8601 date `String`s | A charged `price` is history; rewriting it breaks reconciliation (rule 19) | Treat money already written to a row as frozen; correct by void + re-record |
| **Exception design** | `IllegalArgumentException`/`IllegalStateException` for rules; `DaoException` propagates | A raw `SQLException` cannot be acted on by a user and leaks internals | Throw with a message a receptionist can read; never catch-and-ignore |
| **Fail-fast vs validation-as-a-list** | `CustomerService.validate` → `List<String>`; single actions throw | A form must mark three empty fields at once; `freeze` must refuse immediately | Return a list when a form has many fields; throw when one action has one reason |
| **Generics and collections** | `List<Customer>`, `List<Session>`, `List<Double>` | Compile-time safety and no casts | Declare the interface type (`List`), not `ArrayList` |
| **Functional Java (streams)** | `ProgressService.series`, conflict filtering | Mapping records to one metric without index bookkeeping | `records.stream().map(ProgressRecord::getWeightKg).toList()` |
| **Static vs instance members** | `Validators.validEmail(...)` static; `booking.book(...)` instance | Validation needs no state; a booking needs this service's collaborators | If a method needs no field, it belongs in `util`, not here |
| **Defensive programming** | guard clauses at the top of every method | Cheapest checks first: an unknown id must fail before a second read | Order guards: null/blank → load → existence → state → cross-aggregate |

---

## 3. Design patterns in this layer

| Pattern | Where | Why this pattern | What goes wrong if used carelessly |
|---|---|---|---|
| **Service layer / use-case method** | every public method in this folder | One method = one business action, so each rule has a home | A method that only forwards to a DAO (`findAll()` on `EquipmentService`) adds a layer of nothing — that is the *CRUD pass-through* smell named in §5 |
| **Constructor dependency injection** | all 11 services | Collaborators are explicit, final, and replaceable in a test | `new CustomerDaoImpl(db)` inside a service hard-wires the database and makes the service untestable |
| **Facade over several DAOs** | `SchedulingService` (4 DAOs), `MembershipService` (2) | One call hides a multi-source rule | Hiding *too much* — a `bookEverything()` façade that also sends mail, updates the slot and writes a payment becomes untestable and untraceable |
| **Guard clause / fail-fast** | top of `checkIn`, `bookOpenSlot`, `record` | The failure is reported where it happened, with the reason | Mixed-order guards: a null id reaching `findById` produces a confusing "not found" instead of "no customer selected" |
| **State machine over an enum** | `SchedulingService.canTransition` (rule 18) | One authority for legal `SessionStatus` moves | Transitions scattered across `cancelSession`/`completeSession` drift, and a `NO_SHOW` becomes reversible |
| **Value object in a service rule** | `BigDecimal` money, ISO date strings | Scale-safe arithmetic; text that sorts chronologically | Doing money math in `double` or re-parsing dates per method (`DECISIONS.md` 31, rule 9) |
| **Static utility** | `Validators`, `DateUtils`, `GuardService`, `ChartUtils` (`«utility»`, `DECISIONS.md` 37/39) | Cross-cutting, entity-free, no state to inject | Putting a *rule* in a utility: `MembershipService`'s expiry test is a rule, `DateUtils.addMonths` is arithmetic |
| **Cross-aggregate access through the owning service** | `TrainerService` → `MedicalProfileService.getForTrainer` (rule 15) | One writer per table; medical access stays auditable in one method | Reaching into `MedicalProfileDao` from `TrainerService` is the review failure this folder exists to prevent |
| **Template Method — rejected** | no `BaseService`, no `AbstractService<T>` | The eleven services share no method body | An empty base class costs a jump-to-definition on every read and hides the wiring (`DECISIONS.md` 36) |
| **Singleton — avoided** | services are constructed and passed in | A global service makes two tests share state | `getInstance()` on a service makes the dependency list invisible |
| **Factory Method — boundary case** | `Payment.forMembership/forSubscription/forSession` (model) | `Payment`'s XOR links need three named constructors | A service that builds a `Payment` positionally can swap two adjacent `Integer` links silently |
| **Repository-like seam** | the 13 DAO interfaces | Storage is swappable without touching services | Adding a service-level cache "for speed" reintroduces the staleness the layer removed |

`SessionContext` is the one documented piece of static mutable state in the project
(`DECISIONS.md` 34). It is a `util` class, not a service, and rule 16's role check is applied at the
screen — see §6.

---

## 4. Rules you must follow (and the bug each rule prevents)

1. **A service is stateless: every field is `private final` and injected through the constructor.**
   *Prevents:* a cached `Membership` going stale between two screens, and two screens sharing an
   instance seeing each other's half-finished operation. No `static` mutable fields at all
   (`SessionContext` is the single, documented exception — `DECISIONS.md` 34).
2. **Never import JavaFX, and never build a user-facing string.** A service returns data or throws.
   *Prevents:* business logic that cannot be tested without a window, and duplicated message wording
   drifting between two screens (`FxUtils.error(...)` is the controller's call).
3. **A service touches only the collaborators declared for it in `docs/UML.md` §4.** Cross-aggregate
   reads and writes go through the other aggregate's *service*.
   *Prevents:* two writers on one table. The canonical example: a trainer reads medical data through
   `MedicalProfileService.getForTrainer`, never through `MedicalProfileDao` (rule 15).
4. **No SQL, no JDBC, no `Connection`.** `DaoException` may propagate out of a service; a raw
   `SQLException` may never appear in a signature here.
   *Prevents:* a checked exception forcing `throws` noise through six layers, and SQL text escaping
   the one package that owns it.
5. **Money is `BigDecimal`, never `double`.** Construct with the `String` constructor, compare with
   `compareTo(...)`, divide with an explicit `RoundingMode`.
   *Prevents:* `0.1 + 0.2 != 0.3` drift in a revenue total, and `new BigDecimal("45.0").equals(new
   BigDecimal("45.00")) == false` producing a "changed" balance that never changed
   (`DECISIONS.md` 31). **Never** `new BigDecimal(45.00)` — it copies the binary-float error.
6. **Dates and times are ISO-8601 strings and `DateUtils` owns every parse.** `yyyy-MM-dd` for dates,
   `HH:mm` for times.
   *Prevents:* two parsers disagreeing on a pattern or locale and producing off-by-one-day bugs; ISO
   strings also sort chronologically as text, which is what makes `findByDay`, `findExpiring` and
   `revenueBetween` work with no date type (rule 9).
7. **Two failure shapes, used deliberately.** `validate(...)` returns `List<String>` when a form has
   many fields; a single action throws `IllegalArgumentException` (bad input) or
   `IllegalStateException` (right input, wrong state, e.g. freezing a `CANCELLED` membership).
   Messages must be safe to display verbatim.
   *Prevents:* a form that can only report one error at a time, and a `NullPointerException` shown to
   a user.
8. **Every `SessionStatus` change goes through `canTransition` first** (rule 18).
   *Prevents:* `markNoShow` on a `CANCELLED` session, which corrupts trainer statistics (rule 2).
9. **Derived state is recomputed on read, never assigned by hand** (rule 13): `FULL`, both `EXPIRED`
   literals. Only `OPEN`/`CLOSED`, `FROZEN`, and the two `CANCELLED` literals are set explicitly.
   *Prevents:* a slot stuck at `FULL` after a cancellation, and an `EXPIRED` membership that was
   renewed yesterday.
10. **Every override needs `StaffRole.ADMIN`** — not only the `AdminService` methods but every
    override path, `SchedulingService.adminReserve` included (rule 16).
    *Prevents:* a `STAFF` account booking outside the published slots and bypassing the cap.
11. **`Attendance` and `Payment` rows are never deleted** (rule 19). Attendance is corrected with
    `correctRecord`; money is corrected with `voidPayment` or `AdminService.adjustPayment` (void +
    new row carrying `previousAmount`).
    *Prevents:* a receipt number that no longer resolves and an attendance history with holes.
12. **Price guards are enforced, not assumed**: a session covered by a subscription must have
    `price` equal to zero (rule 11) and a `Payment` must set exactly one of
    `membershipId`/`subscriptionId`/`sessionId`, matching the linked object's customer (rule 12).
    *Prevents:* double billing and a payment that credits the wrong member.
13. **Enums are types.** No `"ACTIVE"` strings, no `1`/`2` status codes. Prefer exhaustive `switch`
    expressions so a new literal fails compilation instead of falling into a default branch.
    *Prevents:* a typo that compiles, and a forgotten case that silently returns `null`.
14. **Do not invent a rule, a method, or an enum literal.** If the implementation needs one, the
    change goes to `docs/UML.md` first (then `DECISIONS.md`), never into one service silently. Where
    this README finds a gap it marks it **Open detail** and lists the options instead of choosing.
    *Prevents:* two services implementing two different readings of the same rule.
15. **Know the atomicity of a multi-row operation.** A service call that writes two rows (or two
    tables) is not automatically atomic: the DAO surface opens its own connection per call.
    *Prevents:* a session row saved without its follow-up write, and a "fixed" payment that is half
    applied. See the Open detail in §6.
16. **Never format what you return.** `revenueBetween` returns `BigDecimal`, a date leaves as
    `yyyy-MM-dd`. The controller or `ChartUtils` renders it.
    *Prevents:* a hard-coded `"$"` and `Locale` inside business logic, and chart labels that cannot be
    re-formatted per screen.

### Error handling

Every service validates its own inputs and throws a domain exception rather than letting a
`DaoException` reach the UI as a stack trace.

| Situation | Shape | Who shows it |
|---|---|---|
| A form with many fields (new customer, new trainer) | `CustomerService.validate(Customer)` → `List<String>`; empty list means valid | The controller maps each message to its field |
| A single action with one reason (sell, renew, freeze, book, record, check in) | `throw new IllegalStateException("…")` / `IllegalArgumentException("…")` | `catch` → `FxUtils.error(e.getMessage())` |
| Something *is* broken (missing row, no slot) | `IllegalArgumentException("No open slot …")` | same |
| The database failed | `DaoException` propagates unchanged (`DECISIONS.md` 40) | `FxUtils.error(e.getMessage())` — never a stack trace |

Two things follow. First, a message must not name a table, a column or a class — "Membership is not
active" is displayable, "membershipId=7 status=CANCELLED" is not. Second, a service never catches a
`DaoException` only to rethrow it with different wording: the DAO's message is already the friendly
one, and a second wording drifts. Each owner covers their own service.

---

## 5. The files in this folder

Eleven files, all planned (there is no `.java` file here yet — `MainApp.java` is the only Java file
in the app). Each entry gives the responsibility in the domain, the **exact** method list from
`docs/UML.md` §4, the declared dependencies, the rules it enforces, a sketch of the hardest method,
how to verify it by hand, and the mistakes that actually happen.

**Write them in this order.** It is dependency order, not the order of the scope list: a service is
written after everything it must call, so nobody is tempted to stub a collaborator.

| # | File | Why here in the order |
|---|---|---|
| 1 | `EquipmentService.java` | One DAO, no cross-aggregate rule — the skeleton to copy |
| 2 | `MedicalProfileService.java` | One DAO plus one read; owns rule 15's sanctioned access path |
| 3 | `MembershipService.java` | Rules 7 and 13; `CustomerService` needs its rule-14 hook |
| 4 | `CustomerService.java` | Rule 14 (`changeStatus(SUSPENDED)` → `freezeForSuspended`) and the `List<String>` validator |
| 5 | `TrainerService.java` | Rule 8 (slot caps) and rules 15/16 boundaries |
| 6 | `AttendanceService.java` | Rule 5, and the rule-19 correction path |
| 7 | `PaymentService.java` | Rules 6, 12 and 19; needs the membership/session reads to exist |
| 8 | `ProgressService.java` | Metric series; the first service whose return type is chart data |
| 9 | `WorkoutService.java` | Per-customer versioning; depends on customer/trainer existence checks |
| 10 | `SchedulingService.java` | The largest: rules 1, 10, 11, 17, 18 plus the subscription stack |
| 11 | `AdminService.java` | Rules 4 and 16 overrides; calls `PaymentService` and `GuardService` |

The **responsibility map with owners** is in §9 (kept there because that table is the team's tracker).

---

### `EquipmentService.java` — inventory lifecycle (owner: Yousef)

**Status:** planned.
**Canonical:** `docs/UML.md` §4 (signatures), §3 (`EquipmentDao`); `DECISIONS.md` 30 (`Equipment` is a
root aggregate); `docs/UML.md` "Notes on scope" (no maintenance history entity).

**What it is.** The whole inventory story: register an item, correct its details, move it between
`AVAILABLE`, `IN_MAINTENANCE` and `RETIRED`, record its condition, stamp the last maintenance date,
and remove a row entered by mistake. Because `Equipment` has no foreign key to anything
(`DECISIONS.md` 30), this is the only service with **no cross-aggregate rule at all** — which makes it
the right file to write first and the wrong file to copy blindly, because the other ten services all
have rules this one does not.

**Declared dependencies** (the only collaborators it may touch — `docs/UML.md` §4 `..>` list):

| Dependency | Type | Why it is here | What it must not become |
|---|---|---|---|
| `EquipmentDao` | interface | `save`, `update`, `delete`, `findAll`, `findByStatus`, `findByCondition`, `findById` | Never `EquipmentDaoImpl` — `DECISIONS.md` 33 |

`Validators` and `DateUtils` are **not** in this service's `..>` list. That is not an accident to work
around: if `logMaintenance` needs a real date check, the fix is a new `..>` edge in `docs/UML.md` §4
(plus a line in `DECISIONS.md` explaining why this service gained a validator), not an import added in
silence. Null/blank guards are plain Java and need no dependency.

**Methods** (all nine from §4, in the order to write them).

1. `int save(Equipment equipment)` — reject `null` and a blank `name`/`category`; default `status` to
   `AVAILABLE` and `condition` to `NEW` when the caller left them `null` (both are NOT NULL columns, so
   a `null` here becomes a confusing DAO error later); `return dao.save(equipment);` The generated id
   comes back — never set it yourself, the DAO owns that.
2. `void update(Equipment equipment)` — reject `null` and `id <= 0`; re-apply the blank checks;
   `dao.update(equipment);` Status and condition are **not** changed here: they have their own use
   cases, which is what keeps the audit story readable.
3. `void changeStatus(int equipmentId, EquipmentStatus status)` — reject `null status`; load
   (`null` → `IllegalArgumentException("Equipment not found")`); set and update. ⚠️ Read the Open
   detail before adding any "you cannot leave `RETIRED`" check.
4. `void changeCondition(int equipmentId, EquipmentCondition condition)` — the same four steps with the
   condition enum. Condition is independent of status: a `RETIRED` item can still be re-graded when
   somebody finally inspects it.
5. `void logMaintenance(int equipmentId, String date)` — load (`null` → throw); set `lastMaintenance`
   and update. There is **no history table**: `docs/UML.md` "Notes on scope" says per-item maintenance
   history would need a new `MaintenanceRecord` entity and is out of scope. Do not fake a history by
   appending into one string field.
6. `void delete(int equipmentId)` — load first, so a wrong id fails with a message instead of a silent
   no-op; then `dao.delete(equipmentId);` Legal here: rule 19 protects only `Attendance` and `Payment`,
   and rule 4's admin-only gate covers only `Customer`/`Trainer`.
7. `List<Equipment> findAll()` — one step: `return dao.findAll();` The `EquipmentController` table.
8. `List<Equipment> findByStatus(EquipmentStatus status)` — reject `null status` first
   (`findByStatus(null)` either throws inside SQL or returns the wrong rows), then delegate.
9. `List<Equipment> findByCondition(EquipmentCondition condition)` — the same shape as `findByStatus`.

> **Open detail (decide + log in `DECISIONS.md`).** `UML.md` fixes no transition matrix for
> `EquipmentStatus`. Options: **(a)** the honest minimum — `changeStatus` accepts any of the three
> literals and the UI offers the two sensible moves; **(b)** declare `RETIRED` terminal and
> `IN_MAINTENANCE` reachable only from `AVAILABLE` — that is a *new rule* and goes into `docs/UML.md`
> as rule 20 **before** the check appears in code. What you must not do is write half a matrix here
> while `EquipmentController` greys out different buttons: two authorities disagree, and the service is
> the one a test can see.

**Why it is designed this way.** One `update(Equipment)` that writes every field including status was
rejected: it makes the three interesting operations invisible. With separate `changeStatus`,
`changeCondition` and `logMaintenance`, "who retired this treadmill?" has one call site. The thin-layer
trap is real here — `findAll`, `findByStatus` and `findByCondition` *are* one-line delegates. They are
still worth having, because the controller must not be handed a DAO; the moment a second caller appears,
the filtering moves out of the screen and into this class.

**Sketch.**

```java
// Sketch — authoritative signature: docs/UML.md §4.
// Equipment is a root aggregate (DECISIONS.md 30): no other table references it,
// so this service has no cross-aggregate guard. Do not copy that to the other ten.
public void changeStatus(int equipmentId, EquipmentStatus status) {
    if (status == null) {
        throw new IllegalArgumentException("Choose a status for the equipment.");
    }
    Equipment item = dao.findById(equipmentId);
    if (item == null) {
        // Fail fast: a wrong id must never look like a successful no-op.
        throw new IllegalArgumentException("That equipment no longer exists.");
    }
    item.setStatus(status);
    dao.update(item);
}
```

**How to know it is right.**

1. Write a fake `EquipmentDao` in a unit test (an `ArrayList` behind the interface — no SQLite, no
   JavaFX). Insert two items, call `changeStatus(id, IN_MAINTENANCE)`, assert `findByStatus` returns
   exactly one.
2. Call `changeStatus(9999, …)` and `findByStatus(null)`: both must throw with a message you would be
   happy to show a receptionist.
3. `logMaintenance(id, "2026-02-01")` twice with different dates → `findById` shows the second one (the
   intended overwrite, because there is no history table).
4. Grep the file for `javafx` and for `SELECT`: neither may appear.

**Common mistakes.**

- **Writing the transition matrix anyway.** An `if (item.getStatus() == RETIRED) throw …` line is an
  invented rule (§4 rule 14). Either it is in `UML.md` or it is not in the code.
- **Reaching for `Validators`/`DateUtils`.** They are absent from this service's `..>` list; import them
  and `docs/UML.md` §4 stops describing the code. Add the edge first.
- **`save` trusting a `null` status.** The DAO will bind `null` into a NOT NULL column and the
  `DaoException` will talk about SQL instead of the missing status.
- **Deleting equipment that will have history.** There is none today, but the moment `MaintenanceRecord`
  exists this `delete` needs rule 4's `GuardService.countReferences` call. Leave a comment pointing at
  rule 4.
- **Filtering `findByStatus` in Java.** The DAO owns the query; two implementations of "what counts as
  `IN_MAINTENANCE`" is exactly the drift this layer prevents.

---

### `MedicalProfileService.java` — medical records and the trainer read path (owner: Abdelrhman)

**Status:** planned.
**Canonical:** `docs/UML.md` §4 (signatures), §3 (`MedicalProfileDao`, `CustomerDao`); rules **3** and
**15**; `docs/UML.md` "Notes on scope" (medical-access auditing is out of scope).

**What it is.** One medical record per customer at most, created on demand (rule 3), plus the single
sanctioned way a trainer may read it: `getForTrainer(customerId, trainerId)`. Every other read of
medical data in the project goes through this service, and the ownership check that decides "may this
trainer see this file?" lives inside that one method.

**Declared dependencies** (the only collaborators it may touch):

| Dependency | Type | Why it is here | What it must not become |
|---|---|---|---|
| `MedicalProfileDao` | interface | `save`, `update`, `findByCustomer`, `findById`, `delete` | Never the impl, never raw SQL |
| `CustomerDao` | interface | "does this customer exist, and who is their trainer?" — the rule 15 check | A read of `Customer` in this service is allowed **only** for ownership; never write here |

**Methods** (all five from §4).

1. `int save(MedicalProfile profile)` — **create the record at most once (rule 3).**
   1. Reject `null profile` and `profile.getCustomerId() <= 0`.
   2. `CustomerDao.findById(profile.getCustomerId())` → `null` means the id is wrong:
      `IllegalArgumentException("Customer not found")`. Without this, an orphan medical row can exist,
      because the schema declares no cascades (`dao/README.md`).
   3. `MedicalProfileDao.findByCustomer(customerId)` — if a row already comes back, refuse:
      `IllegalStateException("This customer already has a medical profile — edit it instead.")`.
      This is the whole of rule 3, and it is the *service*, not the table, that enforces it.
   4. Stamp `updatedDate` — see the Open detail below.
   5. `return dao.save(profile);`
2. `void update(MedicalProfile profile)` — **edit the existing record.**
   1. Reject `null` and `id <= 0`.
   2. Load the stored row with `findById`; `null` → `IllegalArgumentException("Medical profile not
      found")`.
   3. Refuse a changed owner: if `stored.getCustomerId() != profile.getCustomerId()`, throw. Moving a
      medical record between customers is a data-integrity accident, never an intended edit.
   4. Re-stamp `updatedDate`, `dao.update(profile)`.
3. `MedicalProfile getByCustomer(int customerId)` — **the staff/dashboard read.** One step:
   `return dao.findByCustomer(customerId);`. Returning `null` for "no record yet" *is* the contract of
   rule 3 (`0..1`); the controller renders an empty form. Do not throw here.
4. `MedicalProfile getForTrainer(int customerId, int trainerId)` — **rule 15's access check.** See the
   sketch. Four steps: load the customer (missing → throw), compare
   `customer.getTrainerId()` with the trainer (`null` or a different id → refuse), load the profile,
   return it (`null` allowed).
5. `void delete(int profileId)` — **remove a file entered against the wrong customer.**
   1. `findById`; `null` → `IllegalArgumentException`.
   2. `dao.delete(profileId)`. Rule 19 protects only `Attendance` and `Payment`; rule 4's admin-only
      gate covers only `Customer`/`Trainer`, so this delete is a normal operation. Whether the
      *screen* hides it behind `StaffRole.ADMIN` is a controller decision — if the team wants it
      enforced here, that is a new rule.

**Why it is designed this way.** The tempting shortcut is `TrainerService` reading
`MedicalProfileDao.findByCustomer(...)` directly — one line, no extra service call. It was rejected
because the ownership check would then exist in two places (or in none), and `getForTrainer` would stop
being the answer to "who may see medical data?". The `..>` edge in §4 records the allowed direction:
`TrainerService ..> MedicalProfileService : medical view via`. `MedicalProfileService` itself never
calls back into `TrainerService`, which keeps the dependency graph acyclic.

**Sketch.**

```java
// Sketch — authoritative signature: docs/UML.md §4.
public MedicalProfile getForTrainer(int customerId, int trainerId) {
    Customer customer = customerDao.findById(customerId);
    if (customer == null) {
        throw new IllegalArgumentException("Customer not found.");
    }
    // Rule 15: Customer.trainerId is authoritative for medical access. A null
    // trainerId (no assignment yet) must be refused, not treated as "everyone".
    Integer assigned = customer.getTrainerId();
    if (assigned == null || assigned != trainerId) {
        throw new IllegalStateException("This customer is not assigned to you.");
    }
    // Rule 3: zero or one profile. No profile yet is a normal answer, not an error.
    return dao.findByCustomer(customerId);
}
```

**How to know it is right.**

1. Fake both DAOs. `getForTrainer(1, 2)` with a customer whose `trainerId` is `null` → must throw;
   with `trainerId = 3` → must throw; with `trainerId = 2` → must return whatever the profile DAO
   holds (including `null`).
2. Call `save` twice for the same customer and assert the second call throws `IllegalStateException`
   and the DAO still holds one row.
3. Call `update` with a profile whose `customerId` was changed and assert it throws.
4. Grep the whole project for `MedicalProfileDao` — the only files allowed to mention it are this
   service and its own DAO/test files. A hit in `TrainerService` is the review failure rule 15 names.

**Common mistakes.**

- **Treating `null` `trainerId` as "no restriction".** That is the bug that leaks every unassigned
  customer's file to every trainer.
- **Returning the profile for a trainer from another gym area.** The comparison must be against
  `Customer.trainerId`, *not* against a `TrainerSubscription`: rule 15 says the assigned trainer is
  authoritative and the subscription only records a paid period.
- **Using `==` on `Integer` ids.** `assigned != trainerId` above is an unboxing comparison only
  because one side is `int`. Comparing two `Integer`s with `==` compares references and "works" until
  ids exceed 127.
- **Throwing when there is no profile.** Rule 3 allows zero; a trainer opening an empty file must see
  an empty form, not an error dialog.

> **Open detail (decide + log in `DECISIONS.md`) — three gaps this file meets.** **(1)** `updatedDate`
> must be stamped on save/update, but `DateUtils` is not in this service's `..>` list and `UML.md` §6
> has no "today" member at all (see §7). **(2)** `doctorPhone` is a phone number and `Validators` is
> not declared here either, so nothing validates it today. **(3)** `delete` has no role gate. All three
> are the same class of decision: add the missing `..>` edge (and, for the date, a `DateUtils` member)
> in `docs/UML.md` first, or state in `DECISIONS.md` that the field is currently unvalidated. Do not
> resolve them by importing around the declared list.

---

### `MembershipService.java` — plans, pauses and expiry (owner: Ziad)

**Status:** planned.
**Canonical:** `docs/UML.md` §4 (signatures), §3 (`MembershipDao`, `CustomerDao`); rules **7**, **13**,
**14** (as the hook target); `docs/UML.md` "Enumerations" (`MembershipStatus` behaviour lives here).

**What it is.** The gym-access product: sell a plan, renew it, pause it (`freeze`), resume it
(`unfreeze`), cancel it, sweep the ones whose end date has passed, and answer "which memberships expire
soon?" for reminders. It also owns `freezeForSuspended`, the method `CustomerService.changeStatus`
calls so that a `SUSPENDED` customer's plans stop running (rule 14).

**Declared dependencies** (the only collaborators it may touch):

| Dependency | Type | Why it is here | What it must not become |
|---|---|---|---|
| `MembershipDao` | interface | `save`, `update`, `findById`, `findByCustomer`, `findByStatus`, `findExpiring`, `findAll` | Never the impl |
| `CustomerDao` | interface | rule 6's payer check and "does the customer exist?" before `sell` | A customer *write* here — `Customer.trainerId` and status belong to their own flows (rules 14/15) |
| `DateUtils` | `«utility»`, static | every date computation: `addMonths`, and the comparison helpers | Not a place to build display dates — it returns `yyyy-MM-dd` strings only |

**Methods** (all ten from §4).

1. `Membership sell(int customerId, PlanType plan)` — **open a plan.**
   1. Reject `null plan`; load the customer (`CustomerDao.findById`), `null` → throw.
   2. Decide what to do with a `SUSPENDED` customer — see the Open detail; rule 14 implies the new plan
      would be frozen immediately, which is worse than refusing.
   3. `startDate` = the day of sale. **Source of "today" is an Open detail** (see below); it is not
      acceptable for the controller to pass a hand-built date (rule 9).
   4. `endDate` = `startDate` + the plan's period. The period length per `PlanType` is the same missing
      mapping as the price — one Open detail covers both.
   5. `price` = the plan's price (`BigDecimal`). Same Open detail.
   6. Build the entity with `status = ACTIVE`, `frozenDays = 0`, then
      `Membership membership = ...; membership.setId(dao.save(membership)); return membership;` so the
      caller gets the id it needs for the payment link.
2. `void renew(int membershipId)` — **extend a plan (rule 7's second half).**
   1. Load; `null` → throw.
   2. `CANCELLED` → refuse (`IllegalStateException("A cancelled membership cannot be renewed.")`,
      rule 7).
   3. `EXPIRED` → extend: `endDate = DateUtils.addMonths(endDate, periodMonths(plan))`, and set
      `status = ACTIVE`, because a renewal revives the plan.
   4. `ACTIVE`/`FROZEN` → extend from `endDate` as well; extending from today instead would silently
      discard paid time. Do not touch `frozenDays` here.
   5. `dao.update(membership);`
3. `void freeze(int membershipId)` — **pause a running plan.**
   1. Load; `null` → throw.
   2. Only `ACTIVE` may freeze (rule 7). `FROZEN` → refuse ("already frozen"), `EXPIRED`/`CANCELLED` →
      refuse.
   3. `status = FROZEN` and set the counter this service will shift the end date by — the counter's
      source is the Open detail below.
   4. `dao.update(membership);`
4. `void unfreeze(int membershipId)` — **resume and give the paused time back.**
   1. Load; `null` → throw; `status != FROZEN` → refuse.
   2. `endDate = DateUtils.addMonths/…` shifted forward by `frozenDays` (rule 7).
   3. `status = ACTIVE`; **clear the counter** (`frozenDays = 0`). Rule 7 says "clears the counter", and
      that is what makes a second `unfreeze` harmless instead of double-shifting.
   4. `dao.update(membership);` — see the sketch.
5. `void cancel(int membershipId)` — **end a plan on purpose.**
   1. Load; `null` → throw.
   2. Already `CANCELLED` → refuse (idempotency vs. refusal is an Open detail; refusing is the shape
      the other guards use).
   3. `status = CANCELLED`. This literal is *set explicitly* — unlike `EXPIRED`, it is not derived
      (rule 13).
   4. `dao.update(membership);` A cancellation does not delete rows, and it does not touch payments:
      money already recorded stays recorded (rule 19).
6. `int freezeForSuspended(int customerId)` — **rule 14's hook; returns how many were frozen.**
   1. `dao.findByCustomer(customerId)` — the DAO has `findByStatus` too, but this needs *one customer's*
      plans, so filter the customer's list instead of paging a global status list.
   2. `int frozen = 0;` for each membership with `status == ACTIVE`: freeze it (the same three steps as
      `freeze`), `frozen++`.
   3. Skip `FROZEN` (already paused), `EXPIRED`, `CANCELLED`. Return the count so the controller can
      say "3 memberships frozen".
   4. ⚠️ This is a loop of writes: see §6's transaction Open detail. It must be re-runnable without
      damage — it is, because the guard skips anything not `ACTIVE`.
7. `int expireOverdue()` — **the sweep that persists `EXPIRED`.**
   1. Iterate the plans whose end date is before today. `MembershipDao.findByStatus(ACTIVE)` plus the
      same call for `FROZEN` (or `findExpiring(0)`) — see the Open detail for the cutoff.
   2. For each, set `status = EXPIRED` and update; count them.
   3. Return the count for a dashboard message.
   4. Read §4 rule 9 again: `EXPIRED` is derived **and** swept. Reads must still treat
      `endDate < today` as expired even if the sweep has not run since the last write, because rule 13
      says the value is recomputed on read; the sweep exists so reports and reminders have a stored
      status to query. The UI must never set this literal.
8. `List<Membership> expiryReminders(int withinDays)` — **"who expires this week?"**
   1. Reject `withinDays < 0`.
   2. `return dao.findExpiring(withinDays);` — the DAO signature already carries the window, so the
      cutoff arithmetic belongs there (see the Open detail) rather than in a second implementation
      here.
   3. Do not filter by status in Java if the DAO already excludes `CANCELLED`; two filters drift.
9. `List<Membership> findAll()` — one step: `return dao.findAll();` (the membership screen's table).
10. `List<Membership> findByCustomer(int customerId)` — one step, after rejecting `customerId <= 0`.
    The profile screen calls this; so does `freezeForSuspended` internally.

**Why it is designed this way.** The alternative — letting `CustomerService.changeStatus` walk the
customer's memberships itself — was rejected because then two services write `memberships` and rule 7's
freeze semantics have two homes. The `..>` edge
`CustomerService ..> MembershipService : freezes memberships when suspended` is the direction of
allowed knowledge: the customer flow knows that suspension freezes plans, the membership flow knows
nothing about customer status. The same reasoning keeps `MembershipDao.delete` unused: rule 4 makes
"delete" mean *end the record*, and rule 19 keeps the row.

**Sketch.**

```java
// Sketch — authoritative signatures: docs/UML.md §4. Rule 7 in one place.
public void freeze(int membershipId) {
    Membership m = dao.findById(membershipId);
    if (m == null) {
        throw new IllegalArgumentException("Membership not found.");
    }
    if (m.getStatus() != MembershipStatus.ACTIVE) {
        // Rule 7: only ACTIVE may freeze. An EXPIRED plan cannot be paused,
        // and freezing a CANCELLED one would resurrect a plan the member ended.
        throw new IllegalStateException("Only an active membership can be frozen.");
    }
    m.setStatus(MembershipStatus.FROZEN);
    m.setFrozenDays(pausedDays(m));   // counter source: see Open detail below
    dao.update(m);
}

public void unfreeze(int membershipId) {
    Membership m = dao.findById(membershipId);
    if (m == null) {
        throw new IllegalArgumentException("Membership not found.");
    }
    if (m.getStatus() != MembershipStatus.FROZEN) {
        throw new IllegalStateException("This membership is not frozen.");
    }
    // Rule 7: give the paused time back, then clear the counter so a second
    // unfreeze cannot shift the end date twice.
    m.setEndDate(shiftByDays(m.getEndDate(), m.getFrozenDays()));
    m.setFrozenDays(0);
    m.setStatus(MembershipStatus.ACTIVE);
    dao.update(m);
}
```

`pausedDays(...)` and `shiftByDays(...)` are **not** `UML.md` members: they stand for "whatever the
team decides in the Open detail below, implemented once, through `DateUtils`".

**How to know it is right.**

1. With a fake `MembershipDao`, drive one membership through the legal path:
   `sell → freeze → unfreeze → renew → cancel` and check the status after every step. Then try
   `freeze` on a `CANCELLED` row and `renew` on a `CANCELLED` row: both must throw with a readable
   message.
2. Assert `frozenDays == 0` after `unfreeze`, then call `unfreeze` again and confirm it refuses — that
   is the double-shift guard.
3. Check `renew` on an `EXPIRED` row prints an `endDate` **later** than the old one and that the new
   date came from `DateUtils.addMonths(...)`, not from a hand-built string.
4. `freezeForSuspended(customerId)` on a customer with one `ACTIVE`, one `FROZEN` and one `CANCELLED`
   plan must return `1` and leave the other two untouched.

**Common mistakes.**

- **Shifting `endDate` by subtracting frozen days.** Freezing *pauses* the clock: the paid period is
  preserved by moving the end date **forward** (rule 7). Getting the sign wrong silently shortens a
  paid plan.
- **Forgetting to clear `frozenDays`.** The plan unfreezes, the member is happy, and the next freeze
  doubles the counter and hands out a free month.
- **Setting `EXPIRED` from a read path or the UI.** Rule 13: derive it on read; only the sweep writes
  it. A `MembershipController` that sets the literal has taken over a rule.
- **Renewing from today instead of from `endDate`.** An early renewal must extend the paid period, not
  reset it.
- **Letting `sell` accept a `CANCELLED`/`INACTIVE` customer silently.** Whatever the team decides in
  the Open detail, "we never discussed it" is not a decision.
- **Adding a `delete`.** There is no `MembershipService.delete` in §4 — rule 4 says "delete" means
  *end the record*, and `cancel` is that operation.

> **Open detail (decide + log in `DECISIONS.md`) — five questions the rules do not answer** (row numbers
> are in the §7 register).
>
> **1. Where does the price come from? (blocks `sell`)** `sell(customerId, plan)` has no amount
> parameter, so the plan must carry it. `model/README.md` and `LEARNING_GUIDE.md` both say `PlanType`
> "carries plan price + duration", but `docs/UML.md` §1 shows a bare three-literal enumeration — and
> `UML.md` is the authority. Options: **(a)** add price/period members to `PlanType` in `UML.md` §1 and
> have `sell`/`renew` read them (one authority for price, one-line diagram change); **(b)** a price table
> behind a new DAO (more parts, no rule asks for it); **(c)** the controller passes an amount (rejected —
> the money rule would live in a screen). **Recommend (a)**, edited into `UML.md` first. The same missing
> members are the plan→months mapping `renew` needs.
>
> **2. What fills `frozenDays` at freeze time?** Rule 7 fixes the *shape* (freeze stores the counter,
> unfreeze shifts by it and clears it) but `freeze(int)` has no duration parameter, and `Membership` has
> no freeze-start field. Options: **(a)** stamp the freeze day into an `int` — impossible; **(b)**
> `freeze` records `0` and something else fills the counter — then `unfreeze` gives nothing back;
> **(c)** compute a policy length at freeze time (a rule nobody wrote); **(d)** add a `freezeStartDate`
> field to `Membership` in `UML.md` §1 and compute elapsed days in `unfreeze`. **(d)** is the only one
> that makes rule 7 exactly true; log it and change `UML.md` before writing the method.
>
> **3. Where does "today" come from?** `sell`, `freeze` and `expireOverdue` all need one, and neither
> this service's edge list nor `UML.md` §6 provides it (register item 3).
>
> **4. `expiryReminders` cutoff and the sweep's status set.** `MembershipDao.findExpiring(int)` already
> takes a day window, so letting SQLite compute `DATE('now', '+' || ? || ' days')` keeps one
> implementation — but then "today" is the database's clock, not the JVM's. For the sweep: a `FROZEN`
> membership's `endDate` is provisional until `unfreeze` shifts it, so sweeping it would destroy the
> pause. **Recommend: sweep `ACTIVE` only, and say so in `DECISIONS.md`.**
>
> **5. `cancel` twice, and `sell` to a `SUSPENDED` customer.** Refusing the second `cancel` matches the
> other guards; refusing `sell` to a suspended customer matches rule 14's intent. Both are one-liners —
> the point is that they are decided in writing before they are coded.

---

### `CustomerService.java` — members, validation and status (owner: Abdelrhman)

**Status:** planned.
**Canonical:** `docs/UML.md` §4 (signatures), §3 (`CustomerDao`), §6 (`Validators`); rules **4**,
**13**, **14**, **15**; `DECISIONS.md` 31 (ISO dates).

**What it is.** The member record: the field-by-field validator the new-customer form binds to, search
and lookup for every screen that needs a member, save/update, and the status change that has a
consequence — suspending a customer freezes their memberships (rule 14). It has **no `delete`**: rule 4
puts the hard delete on `AdminService` behind `GuardService`, and says "delete" for a customer means
`INACTIVE`.

**Declared dependencies** (the only collaborators it may touch):

| Dependency | Type | Why it is here | What it must not become |
|---|---|---|---|
| `CustomerDao` | interface | `save`, `update`, `findById`, `findAll`, `search`, `findByTrainer` | Never the impl; never `CustomerDao.delete` in this class (rule 4) |
| `Validators` | `«utility»`, static | `validPhone`, `validEmail`, `validDate` — the shape checks the form needs | Not a place for business rules; "is this customer suspended?" is not validation |
| `MembershipService` | service | rule 14: `changeStatus(SUSPENDED)` must call `freezeForSuspended` | Only that call. Reading memberships for a profile screen is the *controller* calling `MembershipService`, not this service |

**Methods** (all eight from §4).

1. `List<String> validate(Customer customer)` — **the only validator-as-a-list in the project.**
   1. `null customer` → return a one-element list, not a throw: a validator's job is to collect
      problems, and a `null` argument is the caller's bug, not a field error.
   2. Check the required text fields against `UML.md` §1: `fullName`, `phone`, `email`, `address`,
      `dateOfBirth`, `gender`, `emergencyName`, `emergencyPhone`, `joinDate`. In §1 only `trainerId` is
      boxed (`Integer`), so it is the only nullable field on this entity — the rest are NOT NULL and
      must be present.
   3. Shape checks through `Validators`: `validPhone(phone)`, `validPhone(emergencyPhone)`,
      `validEmail(email)`, `validDate(dateOfBirth)`, `validDate(joinDate)`.
   4. `trainerId` is optional; if it is set, its *existence* cannot be checked here — this service has
      no `TrainerDao` edge. See the Open detail.
   5. Return the accumulated messages. **Each message names the field in human words** ("Phone number
      looks wrong"), because the controller maps them onto form rows.
   6. Do not check `status` or `id`: the form does not collect them, and a new customer is `ACTIVE`.
2. `List<Customer> search(String term)` — **the searchable list.**
   1. Trim the term. Decide the empty case (Open detail): returning `findAll()` is the friendliest and
      is what a freshly opened list screen expects.
   2. `return dao.search(trimmed);` — matching (which columns, case sensitivity) is the DAO's business;
      SQLite's `LIKE` is case-insensitive for ASCII, which is why the SQL lives there and not here.
3. `List<Customer> findAll()` — one step: `return dao.findAll();`
4. `Customer findById(int customerId)` — **lookup driven by another screen.**
   1. Reject `customerId <= 0` with `IllegalArgumentException("No customer selected.")` — the message a
      controller can show when a table row lost its selection.
   2. Return `dao.findById(customerId)`. Decide once, and apply in all eleven services, whether
      "not found" returns `null` or throws; the error-handling table in §4 leans towards throwing for a
      single action, and a profile screen that must render a header is the same case.
5. `int save(Customer customer)` — **insert a new member.**
   1. `List<String> problems = validate(customer);` — if it is not empty, throw
      `IllegalArgumentException` with the *first* message (or a short joined summary). The form already
      showed the whole list; this throw is the last line of defence against a caller that skipped it.
   2. Default `status` to `ACTIVE` and `joinDate` to today when they are empty — today's source is the
      same Open detail as everywhere else.
   3. `return dao.save(customer);`
6. `void update(Customer customer)` — **correct a member.**
   1. `null` or `id <= 0` → throw; `validate(customer)` → throw on problems.
   2. `dao.update(customer);` — note what this method *can* write: everything on the entity, including
      `status` and `trainerId`. Two of those have owners (rule 13 gives `CustomerStatus` to
      `changeStatus`; rule 15 gives `trainerId` to assignment), so the controller must not offer them on
      the edit form.
7. `void changeStatus(int customerId, CustomerStatus status)` — **the one status writer (rule 13).**
   See the sketch. Steps: reject `null status`; load (missing → throw); if the new status is
   `SUSPENDED`, call `membershipService.freezeForSuspended(customerId)` (rule 14) and report the count;
   set and update. Reactivating does **not** unfreeze anything — rule 14 is one-directional.
8. `List<Customer> findByTrainer(int trainerId)` — one step after `trainerId <= 0` is rejected:
   `return dao.findByTrainer(trainerId);`. This is rule 15's roster read: it works because
   `Customer.trainerId` is the authoritative assignment, which is exactly why the write side has to be
   disciplined.

**Why it is designed this way.** Two alternatives were rejected. **A validator that throws on the first
problem** cannot fill a form: the user fixes the phone number, submits, and is told about the email.
**A `delete(int)` here** would put a hard delete outside `AdminService`, and rule 4's
`GuardService.countReferences` check with it. The double shape — `List<String>` for the form,
exceptions for the actions — is a deliberate split, and §4's error-handling table is the contract.

**Sketch.**

```java
// Sketch — authoritative signature: docs/UML.md §4. Rule 13 + rule 14 in one method.
public void changeStatus(int customerId, CustomerStatus status) {
    if (status == null) {
        throw new IllegalArgumentException("Choose a status.");
    }
    Customer customer = dao.findById(customerId);
    if (customer == null) {
        throw new IllegalArgumentException("Customer not found.");
    }
    if (status == CustomerStatus.SUSPENDED) {
        // Rule 14: a suspended customer implies frozen memberships. The freeze
        // loop lives in MembershipService — this service must not walk
        // memberships itself, or rule 7 would have two implementations.
        membershipService.freezeForSuspended(customerId);
    }
    // No unfreeze on the way back to ACTIVE: rule 14 only wires the suspend
    // direction, and unfreezing in bulk would lose rule 7's counter semantics.
    customer.setStatus(status);
    dao.update(customer);
}
```

**How to know it is right.**

1. `validate(new Customer())` must return a message for every required field (count them against
   `UML.md` §1 — nine required strings), and `validPhone`/`validEmail`/`validDate` must be the ones
   reporting the shape errors.
2. With fakes: `changeStatus(1, SUSPENDED)` must call the fake `MembershipService` exactly once, and
   `changeStatus(1, ACTIVE)` must call it zero times.
3. `save` with an empty `fullName` must throw *and* leave the fake DAO empty.
4. Grep for `delete(` in this file: zero hits is the correct number (rule 4).

**Common mistakes.**

- **Calling `CustomerDao.delete` "because the screen says delete".** Rule 4: hard delete is
  `AdminService`'s, behind a reference count. `changeStatus(INACTIVE)` is what the button does.
- **Freezing memberships in a loop here.** `MembershipService.freezeForSuspended` exists so rule 7 has
  one implementation. Reaching for `MembershipDao` here would not even compile against the declared
  list — which is the point of the list.
- **Validating in the entity's setters instead.** Then the DAO could not load a legacy row that a
  strict setter rejects (`LEARNING_GUIDE.md` §3.2).
- **Returning `List<String>` from an action method.** `freeze`-style operations have one reason to
  fail and must throw; only a multi-field form gets a list.
- **Comparing dates as `LocalDate`.** The entity stores ISO strings (rule 9); converting here creates
  the second parser the rule exists to prevent.

> **Open detail (decide + log in `DECISIONS.md`).** **(1)** `validate` cannot check that a non-null
> `trainerId` refers to a real trainer: this service declares `CustomerDao`, `Validators` and
> `MembershipService`, and neither `TrainerDao` nor `TrainerService` is among them. Options:
> **(a)** add `CustomerService ..> TrainerService` (a read of `findById`) so the FK is verified where
> the assignment happens — but rule 15 routes assignment through `TrainerService.assignCustomer`, so
> the check arguably belongs there; **(b)** leave the FK unvalidated and rely on the picker UI;
> **(c)** add `CustomerService ..> TrainerDao` — the cross-aggregate read the layer discipline warns
> about. **Recommend (a) or (b) with a written note**; never (c) by habit. **(2)** `search("")`:
> `findAll()` or an empty list — pick one, because the list screen's behaviour depends on it.
> **(3)** Rule 4 says a customer "delete" means `INACTIVE` **and end active records**. Rule 14 only
> wires `SUSPENDED` → freeze, so `INACTIVE` currently has no membership consequence. Options:
> **(a)** cancel active memberships (explicit literal, refuses future renewal per rule 7);
> **(b)** let them run out and be swept to `EXPIRED`; **(c)** do nothing. The rules fix only the
> `SUSPENDED` path — decide this one in writing before implementing it.

---

### `TrainerService.java` — roster, rates and availability slots (owner: Ziad)

**Status:** planned.
**Canonical:** `docs/UML.md` §4 (signatures), §3 (`TrainerDao`, `TrainerAvailabilityDao`); rules **8**,
**13**, **15**; `DECISIONS.md` 31; `docs/UML.md` "Enumerations" (`AvailabilityStatus` behaviour lives
here).

**What it is.** Who coaches whom, what a coach costs, and when a coach is bookable: publish hourly
slots, close them, cap them per slot or per day, list the open ones, and assign a customer to a
trainer. It is also where the project's most subtle rule lives — rule 8's "setDailyCap … re-applies
after any later publish, so it wins regardless of call order".

**Declared dependencies** (the only collaborators it may touch):

| Dependency | Type | Why it is here | What it must not become |
|---|---|---|---|
| `TrainerDao` | interface | `save`, `update`, `findById`, `findAll` for the roster and the rate | Never the impl |
| `TrainerAvailabilityDao` | interface | `save`, `update`, `findById`, `findOpenSlots`, `findByTrainerAndDate` for slots and caps | The only writer of the availability table |
| `MedicalProfileService` | service | the rule-15 sanctioned medical read path for a trainer's own customers | Not a second medical writer, and never a reason to hold `MedicalProfileDao` |
| `Validators` | `«utility»`, static | `validTimeRange(from, to)` rejects `startTime >= endTime` (rule 8); `validPhone`/`validEmail`/`validDate` for the roster form | Not a place for cap arithmetic |
| `DateUtils` | `«utility»`, static | slot date comparisons and any date math on the day's rows | Not a display formatter |

**Methods** (all eleven from §4).

1. `TrainerAvailability publishSlot(int trainerId, String date, String startTime, String endTime, int maxCustomers)` — **the rule-8 entry point.**
   1. `TrainerDao.findById(trainerId)`; `null` → `IllegalArgumentException`.
   2. `!Validators.validTimeRange(startTime, endTime)` → throw. This is rule 8's stated rejection of
      `startTime >= endTime`; do not re-implement the comparison here.
   3. `Validators.validDate(date)` and `maxCustomers > 0` → else throw. A cap of zero is an unusable
      slot, not a closed one (closed is `CLOSED`).
   4. Build the row with `status = OPEN` — only `OPEN` and `CLOSED` are ever set by hand, `FULL` is
      derived (rule 13).
   5. `int id = dao.save(slot);`
   6. **Re-apply the day's cap** (rule 8's order-independence) before returning. See the sketch and the
      Open detail: with the dependencies as declared, the only source for "the cap set for this day" is
      the day's existing rows.
2. `void closeSlot(int availabilityId)` — **stop taking bookings for one slot.**
   1. `dao.findById(availabilityId)`; `null` → throw.
   2. Already `CLOSED` → refuse (consistent with the other guards) or return; decide once.
   3. Set `CLOSED`, `dao.update(slot)`.
   4. ⚠️ Closing does **not** cancel the sessions already booked in it, and rule 17's `bookedCount`
      keeps counting them. The screen must show "closed, 2 confirmed sessions" rather than implying the
      slot is empty; the service has no business inventing cancellations.
3. `void setDailyCap(int trainerId, String date, int maxCustomersPerSlot)` — **one cap for a whole day.**
   1. Reject `maxCustomersPerSlot <= 0`.
   2. `List<TrainerAvailability> slots = dao.findByTrainerAndDate(trainerId, date);` — the DAO returns a
      **list** (`TrainerAvailabilityDao`'s signature, and `UML_TEAM.md` confirms it is deliberate).
   3. For each slot: `setMaxCustomers(maxCustomersPerSlot)` and `dao.update(slot)`. Closed slots
      included: the cap is independent of the status, so reopening a slot later inherits the day's cap.
   4. Empty list → not an error: capping a day that has no slots yet is a legitimate "have it ready"
      action. See the Open detail for what a later publish then reads.
4. `void setSlotCap(int availabilityId, int maxCustomers)` — **override one slot afterwards.**
   1. Load; `null` → throw; `maxCustomers <= 0` → throw.
   2. If the new cap is below the current occupancy, see the Open detail: refusing is usually kinder
      than publishing a slot that is instantly over capacity.
   3. Set and update.
5. `void setRate(int trainerId, BigDecimal hourlyRate)` — **the coach's price.**
   1. Load; `null` → throw.
   2. `hourlyRate == null || hourlyRate.compareTo(BigDecimal.ZERO) <= 0` → throw. Note `compareTo`,
      never `==` or `equals` (§4 rule 5).
   3. Set and update.
   4. Money already charged is *not* touched: existing `Membership.price` and
      `TrainerSubscription.ratePerPeriod` values are history (rule 19's spirit — correcting money means
      a void plus a new row, never a silent rewrite).
6. `List<TrainerAvailability> findOpenSlots(int trainerId, String date)` — **the calendar read.**
   1. `List<TrainerAvailability> slots = dao.findOpenSlots(trainerId, date);`
   2. Remove the ones that are full: a slot at its cap is still stored `OPEN` (rule 13), so this read
      path is where `FULL` "happens". Either drop them or return them with a flag — decide with
      `SchedulingService.getAvailableSlots`, which reads the same table (see the note below).
7. `List<Trainer> findAll()` — one step; the roster screen.
8. `Trainer findById(int trainerId)` — reject `trainerId <= 0`, then `dao.findById(trainerId)`; apply the
   same not-found policy as `CustomerService.findById`.
9. `int save(Trainer trainer)` — validate `fullName`/`phone`/`email` with `Validators`, reject a
   `null`/non-positive `hourlyRate`, default `hireDate` when empty, then `return dao.save(trainer);`
10. `void update(Trainer trainer)` — `id <= 0` → throw, re-run the same checks, `dao.update(trainer);`
11. `void assignCustomer(int customerId, int trainerId)` — **rule 15's assignment.**
    1. Load the trainer (`TrainerDao.findById`); `null` → throw.
    2. The write is to `Customer.trainerId`, and **this service declares no `CustomerDao` and no
       `CustomerService`** — see the blocking Open detail. A reassignment also moves medical access with
       it (rule 15) and can invalidate a live `TrainerSubscription` (rule 11). Do not implement this
       method by adding an undeclared DAO.

**Why it is designed this way.** Availability is *not* a field on `Trainer` — a separate
`TrainerAvailability` row per hourly slot with its own DAO is what makes rule 8 possible at all: several
slots per day, a cap per slot, and a per-slot status. The alternative (one daily window on `Trainer`)
would have made `setSlotCap` meaningless and forced a schema change for every "the 10:00 is full, the
11:00 is not" case. Note also what this service does **not** own: it reads slots, but
`SchedulingService` is the booking authority. `TrainerService.findOpenSlots` and
`SchedulingService.getAvailableSlots` are two names for "the open slots on a day" because a calendar
view and a booking form ask slightly different questions; if they ever disagree about a `FULL` slot, the
booking one is right, because rule 17 lives there.

**Sketch.**

```java
// Sketch — authoritative signature: docs/UML.md §4. Rule 8's two halves.
public TrainerAvailability publishSlot(int trainerId, String date,
                                       String startTime, String endTime, int maxCustomers) {
    if (trainerDao.findById(trainerId) == null) {
        throw new IllegalArgumentException("Trainer not found.");
    }
    if (!Validators.validTimeRange(startTime, endTime)) {
        // Rule 8: startTime >= endTime is rejected, not normalised.
        throw new IllegalArgumentException("The slot must end after it starts.");
    }
    if (!Validators.validDate(date) || maxCustomers <= 0) {
        throw new IllegalArgumentException("Give the slot a valid date and a cap of at least 1.");
    }
    TrainerAvailability slot = new TrainerAvailability();
    slot.setTrainerId(trainerId);
    slot.setDate(date);
    slot.setStartTime(startTime);
    slot.setEndTime(endTime);
    slot.setMaxCustomers(maxCustomers);
    slot.setStatus(AvailabilityStatus.OPEN);   // never FULL: rule 13, it is derived
    slot.setId(availabilityDao.save(slot));

    // Rule 8: the day's cap wins regardless of call order, so a slot published
    // AFTER setDailyCap must still join the day's cap. Read the day's rows and
    // re-apply — see the Open detail for what "the day's cap" can mean here.
    applyDayCap(trainerId, date);
    return slot;
}
```

**How to know it is right.**

1. `publishSlot(1, "2026-03-02", "10:00", "10:00", 3)` and `("11:00", "10:00", 3)` must both throw;
   `("10:00", "11:00", 0)` must throw.
2. `publishSlot` on an unknown `trainerId` must throw before anything is saved (assert the fake DAO is
   still empty).
3. Order test for rule 8: (a) `publishSlot(10:00)` → `setDailyCap(date, 2)` → `publishSlot(12:00)`;
   (b) the reverse order. **Both runs must end with every slot on that day carrying cap 2.** That is
   literally what "wins regardless of call order" means, and it is the assertion that catches a
   cap implementation that only works in one order.
4. `setSlotCap(id, 1)` after a daily cap of 2 must leave the other slots at 2 — the override is per
   row, as rule 8 says.
5. `findOpenSlots` on a slot whose `bookedCount` reached the cap must not offer it for booking.

**Common mistakes.**

- **Storing `FULL`.** Rule 13: `FULL` is derived from occupancy (`bookedCount >= maxCustomers`). Write
  it once and a cancellation leaves the slot permanently unbookable.
- **Capping only the *new* slot in `publishSlot`.** That is the one-order-only implementation rule 8
  exists to forbid.
- **Letting a daily cap live in a static field or a service field.** Services are stateless (§4 rule 1):
  a remembered cap is invisible to the diagram, lost on restart, and shared between screens.
- **Implementing `assignCustomer` by adding `CustomerDao` here.** §4 gives this service no such edge;
  the fix is a documented dependency change (§5 entry for `AdminService` shows the admin path that *is*
  allowed a direct `CustomerDao` write).
- **Rejecting `startTime == endTime` in `publishSlot` with your own comparison.** `Validators` owns it,
  and rule 8 names the method.
- **Comparing `BigDecimal` rates with `==` or `equals`.** `equals` also compares scale, so
  `45.00` and `45.0` look different (§4 rule 5).

> **Open detail (decide + log in `DECISIONS.md`) — four gaps** (row numbers in the §7 register).
>
> **1. `assignCustomer` has no legal write path (blocking).** The method must set
> `Customer.trainerId`, but §4 declares `TrainerDao`, `TrainerAvailabilityDao`, `MedicalProfileService`,
> `Validators`, `DateUtils` for this service — no `CustomerDao`, no `CustomerService`. Options:
> **(a)** add `TrainerService ..> CustomerService` and write through `findById` → `setTrainerId` →
> `update` (the layer-correct answer: the aggregate's owner performs the write); **(b)** add
> `TrainerService ..> CustomerDao` — it works, and it is exactly the cross-aggregate write this folder
> warns about (acceptable only if `DECISIONS.md` says why); **(c)** move assignment to
> `AdminService.reassignTrainer`, which already declares `CustomerDao` — but routine assignment would
> then need an ADMIN account, which rule 16 does not ask for. **Recommend (a).**
>
> **2. Where is a day's cap stored?** Rule 8 says `setDailyCap` "re-applies after any later publish",
> which means remembering the value, and `TrainerAvailability` has no day-cap field. Options: **(a)** a
> new field/row (e.g. a "day cap" row or `capSource`) — a `UML.md` schema change, the only one that
> makes the rule exactly true; **(b)** `publishSlot` infers the cap from the day's rows when they all
> agree — no new state, but it breaks the moment one slot was overridden by `setSlotCap`; **(c)** a
> static map in the service — rejected (stateless rule, lost on restart); **(d)** accept the one-order
> behaviour and warn the operator to publish slots first — honest, but it contradicts rule 8.
> **Recommend (a) if the cap must be exact, else (b)** with a note that mixing the two cap methods on one
> day is unsupported.
>
> **3. Lowering a cap below the occupancy.** `setSlotCap(id, 1)` on a slot with 2 `CONFIRMED` sessions
> makes `isSlotFull` true for an over-full slot and tells nobody. Options: refuse (the operator must
> cancel a session first — there is no "move session" method in §4) or allow and let the calendar render
> the overflow. Rule 17's count does not change either way; log the choice.
>
> **4. Overlapping slots on one day.** Rule 8 allows several slots per day and only rejects
> `startTime >= endTime`, so `10:00–12:00` and `11:00–13:00` can both exist for one trainer — and
> `detectConflicts` compares trainer+day+start time, not windows, so the overlap can be double-booked.
> Either add a rule (and implement the overlap check with `startTime`/`endTime`/`durationMin`) or decide
> that publishing overlaps is the trainer's own mistake. Do not add the check silently: it changes what
> `publishSlot` accepts.

---

### `AttendanceService.java` — the door: check-in, check-out, corrections (owner: Abdel Raouf)

**Status:** planned.
**Canonical:** `docs/UML.md` §4 (signatures), §3 (`AttendanceDao`, `CustomerDao`, `MembershipDao`);
rule **5** (eligibility + one row per day), rule **19** (never delete; `correctRecord` is the fix).

**What it is.** Reception's screen: one check-in per customer per day, one check-out at the end, a
daily list for the report, and a correction path for a mis-keyed time. Rule 5 is the whole reason this
service exists: entry requires an `ACTIVE` customer **and** a membership that is `ACTIVE` and not past
its end date.

**Declared dependencies** (the only collaborators it may touch):

| Dependency | Type | Why it is here | What it must not become |
|---|---|---|---|
| `AttendanceDao` | interface | `save`, `update`, `findById`, `findByDay`, `findByCustomer`, `findByCustomerAndDay` | The only writer of the attendance table |
| `CustomerDao` | interface | rule 5's first half: the customer exists and `status == ACTIVE` | A customer *write* here |
| `MembershipDao` | interface | rule 5's second half: an `ACTIVE`, unexpired membership exists | A membership *write* — freezing/selling is `MembershipService`'s |

Note what is **not** in that list: `DateUtils` and `Validators`. It does not matter for rule 5, because
`checkIn(customerId, date, time)` receives the date (and compares it against `endDate` as an ISO
string). That is not luck: it is what makes the eligibility test reproducible in a unit test without
mocking a clock.

**Methods** (all five from §4).

1. `Attendance checkIn(int customerId, String date, String time)` — **rule 5 in one method.** See the
   sketch. Steps: reject blank `date`/`time`; load the customer (missing → throw); require
   `CustomerStatus.ACTIVE`; find the customer's memberships and require at least one with
   `status == ACTIVE && endDate >= date`; ask `findByCustomerAndDay` whether the day's row already
   exists and **return it instead of inserting** (rule 5); otherwise build and save a new row.
2. `void checkOut(int customerId, String date, String time)` — **close the day.**
   1. Reject blank `date`/`time`.
   2. `List<Attendance> rows = dao.findByCustomerAndDay(customerId, date);` — a **list**, deliberately
      (`dao/README.md`), so a stray duplicate is visible rather than hidden by `LIMIT 1`.
   3. Empty → `IllegalStateException("This customer has not checked in today.")`.
   4. Set `checkOut` on the row and update. See the Open detail for a second check-out.
3. `List<Attendance> dailyReport(String date)` — **the data behind the daily list.**
   1. Reject blank `date`.
   2. `return dao.findByDay(date);` — the service returns the **rows**. Joining customer names into
      display text is the controller's job (§4 rule 2 and rule 16); the controller may resolve names
      through `CustomerService.findById`, which is a read, not a rule.
4. `void correctRecord(int attendanceId, String checkIn, String checkOut)` — **rule 19's correction path.**
   1. `dao.findById(attendanceId)`; `null` → `IllegalArgumentException("Attendance record not found")`.
   2. Set both times on the existing row: this is an update, **never** a delete plus insert
      (rule 19), and the row keeps its id.
   3. A `null`/blank `checkOut` means "still inside" — the entity's `checkOut` is the nullable one
      (`model/README.md`), so reopening a record is a legal correction.
   4. Update and return. There is no audit column for corrections; say so out loud rather than
      pretending the change is traceable (see the Open detail).
5. `List<Attendance> findByCustomer(int customerId)` — one step after rejecting `customerId <= 0`:
   `return dao.findByCustomer(customerId);` The profile screen's attendance tab.

**Why it is designed this way.** The alternative is to trust the membership table to be right and skip
the check — "the membership screen already expired it". That fails twice: `MembershipStatus.EXPIRED`
is *derived* (rule 13), so a plan whose end date passed this morning can still read `ACTIVE` until the
sweep runs; and a `FROZEN` plan must keep the member out even though the customer is `ACTIVE`. Rule 5
therefore tests the membership's **status and end date**, not the sweep's output, and it does so on
every check-in.

**Sketch.**

```java
// Sketch — authoritative signature: docs/UML.md §4. Rule 5, guard order included.
public Attendance checkIn(int customerId, String date, String time) {
    if (date == null || date.isBlank() || time == null || time.isBlank()) {
        throw new IllegalArgumentException("Pick a date and a time.");
    }
    Customer customer = customerDao.findById(customerId);
    if (customer == null) {
        throw new IllegalArgumentException("Customer not found.");
    }
    if (customer.getStatus() != CustomerStatus.ACTIVE) {
        // Rule 5, first half. A SUSPENDED customer's plans are frozen too (rule 14),
        // so this guard also catches the case reception would otherwise argue about.
        throw new IllegalStateException("This customer is not active.");
    }
    boolean covered = false;
    for (Membership m : membershipDao.findByCustomer(customerId)) {
        // Rule 5, second half: ACTIVE *and* not past the end date. FROZEN does not
        // admit entry, and the end date is compared as an ISO string (rule 9).
        if (m.getStatus() == MembershipStatus.ACTIVE && m.getEndDate().compareTo(date) >= 0) {
            covered = true;
            break;
        }
    }
    if (!covered) {
        throw new IllegalStateException("No active, unexpired membership for today.");
    }
    // Rule 5: a second check-in on the same day returns the existing row.
    List<Attendance> today = dao.findByCustomerAndDay(customerId, date);
    if (!today.isEmpty()) {
        return today.get(0);
    }
    Attendance record = new Attendance();
    record.setCustomerId(customerId);
    record.setDate(date);
    record.setCheckIn(time);
    record.setId(dao.save(record));
    return record;
}
```

**How to know it is right.**

1. Table test with fake DAOs. Four customers: `ACTIVE` + `ACTIVE` membership ending today (allowed),
   `ACTIVE` + membership that ended yesterday (refused), `ACTIVE` + `FROZEN` membership (refused),
   `SUSPENDED` + `ACTIVE` membership (refused). That is rule 5 in four lines of test.
2. Call `checkIn` twice with the same date and assert the DAO holds **one** row and both calls return
   the same id.
3. `checkOut` before any `checkIn` must throw; `correctRecord` on a closed row must change the times
   and leave the row count unchanged.
4. Grep for `delete`: `AttendanceService` has no delete, and `AttendanceDao.delete` must not appear
   (rule 19).

**Common mistakes.**

- **Testing only the membership status.** `ACTIVE` + expired end date is the exact hole rule 5 closes.
- **Accepting `FROZEN` as "still a member".** A frozen plan is paused; entry requires `ACTIVE`.
- **Inserting a second row for a repeat check-in.** Rule 5 says return the existing row. Inserting
  makes the daily report count the same person twice, and the "one per day" invariant quietly dies.
- **Deleting the row and inserting a corrected one.** Rule 19: `correctRecord` updates in place; a
  delete creates a gap that no report can explain.
- **Formatting the check-out for the report here.** Return the row; the controller renders
  `checkIn`/`checkOut`. A `DateTimeFormatter` in this file means the layer rule was broken.

> **Open detail (decide + log in `DECISIONS.md`).** **(1) A second `checkOut` on the same day:** does it
> overwrite (idempotent, convenient for a typo) or throw and point the operator at `correctRecord`
> (auditable)? **Recommend throwing**, because `correctRecord` exists as the sanctioned path and rule 19
> treats a record as history. **(2)** `correctRecord` does not validate that `checkIn < checkOut`, and
> `Validators` is not declared for this service — either add the `..>` edge and use
> `validTimeRange(checkIn, checkOut)`, or accept nonsense times. **(3)** There is **no audit trail** for
> a correction (no `correctedBy`/`correctedAt` fields); `UML.md` "Notes on scope" takes the same
> position for medical access. If the college submission needs traceability, that is a new entity and a
> `UML.md` change, not a comment in the code. **(4)** `time` is not checked against `HH:mm` anywhere in
> this service; the format rule (rule 9) currently relies on the time-picker UI.

---

### `PaymentService.java` — money: recording, voiding, balances (owner: Abdel Raouf)

**Status:** planned.
**Canonical:** `docs/UML.md` §4 (signatures), §3 (`PaymentDao`, `CustomerDao`, `MembershipDao`,
`SessionDao`); rules **6**, **11**, **12**, **19**; `DECISIONS.md` 31 (money is `BigDecimal`).

**What it is.** Every euro that enters the gym: record a payment against exactly one of a membership, a
trainer subscription or a session, validate that link, void a payment entered wrongly, compute what a
customer still owes, sum revenue over a date range, and look a receipt up again. Nothing here is ever
deleted (rule 19) and nothing here is ever formatted (it returns `BigDecimal`).

**Declared dependencies** (the only collaborators it may touch):

| Dependency | Type | Why it is here | What it must not become |
|---|---|---|---|
| `PaymentDao` | interface | `save`, `update`, `findById`, `findByCustomer`, `findByReceipt`, `findByMethod`, `revenueBetween` | The only writer of the payments table |
| `CustomerDao` | interface | rule 12's payer check and the rule 6 eligibility read | Not a customer editor |
| `MembershipDao` | interface | the membership arm of `validLink` and of `balance` | Not a membership editor — prices come from stored rows only |
| `SessionDao` | interface | the session arm of `validLink` and of `balance`; ad-hoc session prices | Not a booking service |

**Methods** (all nine from §4).

1. `boolean validLink(Payment payment)` — **rule 12, and it is `public` on purpose.**
   See the sketch. Steps: reject `null payment`; count how many of `membershipId`, `subscriptionId`,
   `sessionId` are non-null and require **exactly one** (XOR — not "at most one"); load the linked row
   and require it to exist; require the linked row's `customerId` to equal `payment.getCustomerId()`.
2. `int record(Payment payment)` — **the only insert path.**
   1. `if (!validLink(payment)) throw new IllegalArgumentException("A payment must link to exactly one
      membership, subscription or session of the same customer.");` — rule 12 says `record` calls it.
   2. Reject a `null` amount and a negative one (see the Open detail for zero).
   3. Reject a `null` method/date/receipt number (`PaymentMethod` and the ISO date are NOT NULL).
   4. `return dao.save(payment);` — the row is born unvoided (`voided` false) and stays in the table
      forever (rule 19). Correcting it later means a void plus a new row.
3. `void voidPayment(int paymentId, String reason)` — **cancel the effect of one payment.**
   1. Load; `null` → throw.
   2. Already `voided` → refuse (consistent with the other guards) — and see the Open detail about
      `voidedDate`, which needs "today".
   3. Set `voided = true`, `voidReason = reason`, `voidedDate` = today; keep every other field, including
      `amount`. A void is a statement ("this was wrong"), not an erasure: the receipt trail must still
      show what was once recorded (rule 19).
   4. `dao.update(payment);` — **never** `dao.delete`.
4. `void voidRecord(String entityName, int id)` — **void the payments attached to a record that is
   going away.**
   1. The `entityName` parameter selects the link column: a membership, a trainer subscription or a
      session. See the Open detail for the exact string vocabulary; it must be one constant in one
      place, and it must match `schema.sql`'s table names.
   2. Collect the non-voided payments whose matching link column equals `id`.
   3. Call the same three steps as `voidPayment` for each, with a reason that names the record type.
   4. Why it exists at all: `AdminService` declares `PaymentService` ("voids and adjusts through"), so
      this is how an override closes a money trail before a record is removed — and because
      `PaymentDao` has no `findByMembership`/`findBySession`, the selection has to be expressed over
      the reads that do exist.
5. `BigDecimal balance(int customerId)` — **rule 6: what this member still owes.**
   1. Verify the customer exists (`CustomerDao`) — a balance for a non-existent member is a bug worth
      reporting.
   2. `List<Payment> payments = dao.findByCustomer(customerId);` and **filter out the voided ones**:
      rule 6 says voided payments never count. `findByCustomer` returns them (the DAO's *revenue* query
      excludes voided; the list query cannot, because the screen must show them).
   3. Membership arm: for each membership of the customer, if no live payment carries
      `membershipId == m.getId()`, add `m.getPrice()`.
   4. Session arm: for each session of the customer that should have been paid for (see the Open
      detail), if no live payment carries `sessionId == s.getId()`, add `s.getPrice()`.
   5. Subscription arm: the same shape with `subscriptionId` — **blocked by the same missing
      `TrainerSubscriptionDao` edge as `validLink`'s third arm** (see the Open detail).
   6. Sum with `total = total.add(x)` starting from `BigDecimal.ZERO`, and return the total with
      `setScale(2, RoundingMode.HALF_UP)` — never `+`, never `double`, and only ever compared with
      `compareTo` (§4 rule 5).
6. `BigDecimal revenueBetween(String from, String to)` — **the dashboard's revenue.**
   1. Reject blank `from`/`to`; if `from.compareTo(to) > 0` (ISO strings sort chronologically — that is
      why they are strings, `DECISIONS.md` 31) throw, or swap them; decide once.
   2. `BigDecimal sum = dao.revenueBetween(from, to);` — voided rows are excluded in SQL
      (`dao/README.md`), and SQLite's `SUM` over zero rows is `NULL`, so **null-check the result and
      return `BigDecimal.ZERO`** rather than letting an NPE escape.
   3. Return the `BigDecimal`. No currency symbol, no thousands separator, no rounding for display: the
      dashboard formats it, and `ChartUtils` formats the chart's copy.
7. `List<Payment> findByCustomer(int customerId)` — reject `customerId <= 0`, then delegate. The
   payment history screen needs the voided rows too, which is why this read does not filter.
8. `Payment findByReceipt(String receiptNo)` — reject blank, then `dao.findByReceipt(receiptNo)`.
   "Not found" is `null` (the project does not use `Optional` — `LEARNING_GUIDE.md` §3.6); the
   controller decides how to say "no such receipt". See the Open detail about a receipt number that
   appears twice after an adjustment.
9. `List<Payment> findByMethod(PaymentMethod method)` — reject `null method`, then delegate. A read used
   by the cash-up screen (`PaymentMethod` is one of the enums reachable only through such a read).

**Why it is designed this way.** `validLink` is `public` and returns `boolean` rather than throwing.
That was a deliberate choice: it lets a unit test state rule 12 as a table of nine cases
(three link columns × set/unset) without catching exceptions, and it lets `record` own the message. The
XOR is checked in the service, not only in the table, even though `dao/README.md` asks for a `CHECK`
constraint — a `CHECK` produces a `DaoException` whose message is about SQL, while the service produces
a sentence the receptionist understands. Two layers, one rule, two audiences: that is not duplication,
it is defence with different messages. `Payment` itself stays a dumb entity and offers the three named
factories (`forMembership`/`forSubscription`/`forSession`), because its three adjacent nullable
`Integer` links swap silently in a positional constructor.

**Sketch.**

```java
// Sketch — authoritative signature: docs/UML.md §4. Rule 12 and rule 6's shape.
public boolean validLink(Payment p) {
    if (p == null) {
        return false;
    }
    int links = 0;
    if (p.getMembershipId() != null) links++;
    if (p.getSubscriptionId() != null) links++;
    if (p.getSessionId() != null) links++;
    if (links != 1) {
        return false;   // rule 12: exactly one — "at most one" is not the rule
    }
    Integer expected = p.getCustomerId();
    if (p.getMembershipId() != null) {
        Membership m = membershipDao.findById(p.getMembershipId());
        return m != null && m.getCustomerId() == expected;
    }
    if (p.getSessionId() != null) {
        Session s = sessionDao.findById(p.getSessionId());
        return s != null && s.getCustomerId() == expected;
    }
    // Third arm: the linked subscription's customer must match too. §4 gives this
    // service no TrainerSubscriptionDao, so the check has no legal read path yet
    // — see the Open detail. Returning true here is a KNOWN HOLE, not a decision.
    return true;
}

// Rule 6, membership arm only; the session and subscription arms have the same shape.
private BigDecimal unpaidTotal(int customerId, List<Payment> live) {
    BigDecimal total = BigDecimal.ZERO;
    for (Membership m : membershipDao.findByCustomer(customerId)) {
        boolean paid = live.stream()
                .anyMatch(p -> p.getMembershipId() != null
                        && p.getMembershipId() == m.getId());   // Integer vs int: unboxes
        if (!paid) {
            total = total.add(m.getPrice());
        }
    }
    return total;
}
```

**How to know it is right.**

1. Table-test `validLink` with nine payments: only the three single-link ones are `true`; zero links and
   any two links are `false`; the "right shape, wrong customer" case is `false` for the membership and
   session arms.
2. `record` with an invalid link must throw **and** leave the fake DAO empty.
3. `balance` test: one membership of `"45.00"` with no payment → `"45.00"`; add a payment of `"45.00"`
   → `"0.00"`; **void that payment → back to `"45.00"`** (rule 6's second sentence).
4. `revenueBetween("2026-01-01","2026-01-31")` on an empty table must return `BigDecimal.ZERO`, not
   throw — that is the null-`SUM` trap.
5. Assert the file contains no `delete(` and no `double`.

**Common mistakes.**

- **Implementing rule 12 as "at most one link".** A payment with zero links is a payment for nothing;
  rule 12 says exactly one.
- **Comparing `Integer` links with `==` on both sides.** `p.getMembershipId() == otherInteger` compares
  references and "works" for ids under 128. Compare with `.equals(...)` or unbox one side deliberately.
- **Letting a voided payment count** because `findByCustomer` returned it. Rule 6 excludes voided rows
  in *every* total.
- **Building a receipt number here, or formatting the amount.** `receiptNo` is a field the caller
  supplies; formatting is the controller's.
- **`dao.delete(paymentId)` to fix a mistake.** Rule 19: void it, and if the money was wrong rather
  than the link, use `AdminService.adjustPayment` (void + replacement carrying `previousAmount`).
- **Rounding with `Math.round` or `double`.** `setScale(2, RoundingMode.HALF_UP)` on the `BigDecimal`,
  once, at the boundary where a total is produced.

> **Open detail (decide + log in `DECISIONS.md`) — five gaps that decide how exact rule 12 and rule 6
> can be.** Row numbers refer to the §7 register.
>
> **1. The subscription arm has no read path (blocking for full rule 12).** §4 declares `PaymentDao`,
> `CustomerDao`, `MembershipDao`, `SessionDao` for this service — no `TrainerSubscriptionDao` — yet rule
> 12 requires the linked object's customer to match for all three arms, and rule 11 requires the
> subscription's `trainerId` to match the assigned trainer. Options: **(a)** add
> `PaymentService ..> TrainerSubscriptionDao` to §4 (one line, symmetric with the other two arms, and
> the DAO already has `findById`); **(b)** route the check through `SchedulingService` — not declared
> either, and this service must not depend on the booking machine to validate a link; **(c)** leave the
> hole and document it. **Recommend (a)**; until it is in `UML.md`, code that "just reads the table" is
> the review failure this folder's rules name.
>
> **2. How `balance` decides an item is unpaid.** Rule 6 names the three sources but not which rows
> count. Memberships: every row, or only `ACTIVE`/`FROZEN` (a `CANCELLED` plan was never owed)?
> Sessions: every row, or only `COMPLETED`/`CONFIRMED` (an unpaid `REQUESTED` is not a debt yet, and a
> `NO_SHOW` may or may not be charged)? Options: **(a)** all non-`CANCELLED` rows — simplest, never
> under-bills; **(b)** a status whitelist per source, which needs a written rule because it changes what
> a customer owes. **This is a money rule: pick one, log it, unit-test it.**
>
> **3. Where `date`, `receiptNo` and `voidedDate` come from.** `record(Payment)` receives them on the
> entity; `voidPayment` must stamp `voidedDate` itself, and there is no `DateUtils` edge and no "today"
> member in §6 (register item 3). Receipt-number *generation* (`R-2026-0001`?) and uniqueness are also
> unspecified — no duplicate check exists in §4.
>
> **4. `findByReceipt` after an adjustment.** Rule 19's `adjustPayment` legitimately leaves **two** rows
> for one receipt (the voided original and the replacement carrying `previousAmount`). Options:
> **(a)** the DAO prefers the non-voided row (`… AND voided = 0 ORDER BY id DESC LIMIT 1` — a SQL
> change, so it belongs in the DAO); **(b)** the replacement gets a fresh receipt number. Decide before
> `adjustPayment` is written, not after the first reconciliation.
>
> **5. Zero and negative amounts.** Rule 11 makes a *covered session's price* zero, which is not the
> same as accepting a zero *payment*. A negative amount is a refund expressed in the wrong place —
> refunds are a void plus (optionally) a new payment. **Recommend: refuse negative; refuse zero unless
> the team says otherwise.**

---

### `ProgressService.java` — body metrics and chart series (owner: Abdel Raouf)

**Status:** planned.
**Canonical:** `docs/UML.md` §4 (signatures), §3 (`ProgressDao`), §6 (`ChartUtils`); `docs/UML.md` §1b
("`targetWeightKg` is the nullable benchmark"); `docs/UML.md` "Enumerations" (`ProgressMetric` is
reachable only through `series`).

**What it is.** The measurements a member tracks over time, and the two things a screen wants from
them: one metric as a list of numbers ready for a chart, and the target weight drawn as a line. This is
the first service whose *return type* is chart data, which is exactly where the "never format here"
rule gets tested.

**Declared dependencies** (the only collaborators it may touch):

| Dependency | Type | Why it is here | What it must not become |
|---|---|---|---|
| `ProgressDao` | interface | `save`, `update`, `delete`, `findById`, `findByCustomer` | The only writer of the progress table |
| `ChartUtils` | `«utility»`, static | turns records into a chart-ready `List<Double>` (`weightSeries`) and owns the formatting decisions | **Not a DAO caller** — it takes values the service already fetched (`util/README.md`) |

**Methods** (all six from §4).

1. `int save(ProgressRecord record)` — **add a measurement.**
   1. Reject `null` and `customerId <= 0`. Note the missing `CustomerDao` edge: an orphan progress row
      cannot be detected here (see the Open detail).
   2. Reject a blank `date` and physically impossible values: `weightKg <= 0` → throw,
      `bodyFatPct < 0 || bodyFatPct > 100` → throw. These are plain Java checks and need no dependency.
   3. `targetWeightKg` is nullable by design (`Double`, not `double`) — a record without a benchmark is
      normal, and `update` must be able to *clear* it.
   4. `return dao.save(record);`
2. `void update(ProgressRecord record)` — same checks plus `id > 0` and an existence load
   (`findById` → `null` → throw). An edit must not silently insert a second row for the same day: if
   the team wants one record per day, that needs a lookup and a rule, not an accident.
3. `void delete(int recordId)` — load (`null` → throw), then `dao.delete(recordId)`.
   **This delete is legal**: rule 19 protects only `Attendance` and `Payment`, and rule 4's
   admin-only gate covers only `Customer`/`Trainer`. A member who typed the wrong weight should be able
   to remove the row, and that asymmetry with attendance/payment is worth explaining out loud in the
   presentation.
4. `List<ProgressRecord> findByCustomer(int customerId)` — reject `customerId <= 0`, then delegate, and
   **sort chronologically** before returning: ISO date strings sort correctly as text
   (`DECISIONS.md` 31), and a chart whose x-axis follows insertion order instead of the calendar is a
   bug nobody notices until the data is entered out of order.
5. `List<Double> series(int customerId, ProgressMetric metric)` — **the chart's y-values.** See the
   sketch. Steps: load and sort; `switch` on the metric (exhaustive, so a new literal is a compile
   error); `WEIGHT_KG` → `ChartUtils.weightSeries(records)` (the declared edge); `BODY_FAT_PCT` → map
   `getBodyFatPct()` (see the Open detail on the asymmetry); return `List<Double>`, never a formatted
   label.
6. `Double targetWeight(int customerId)` — **the benchmark line.**
   1. Load the customer's records, sorted.
   2. Return the benchmark according to the policy in the Open detail; the return type is boxed
      `Double` precisely so "no target set" is representable.
   3. `null` means "draw no target line" — it is not an error and must not become `0.0`, which would
      render a target at the bottom of the chart.

**Why it is designed this way.** The service returns `List<Double>`, not `List<ProgressRecord>` and not
a JavaFX `XYChart.Series`. Two reasons: the controller must be able to change the chart without a
service change, and a `Series` in this layer would drag JavaFX in, ending the layer's testability
(§4 rule 2). The mapping is therefore a pure function of the records — the easiest thing in the whole
folder to unit test, and the one place where a metric choice is validated by the compiler.

**Sketch.**

```java
// Sketch — authoritative signature: docs/UML.md §4.
public List<Double> series(int customerId, ProgressMetric metric) {
    if (metric == null) {
        throw new IllegalArgumentException("Choose a metric.");
    }
    List<ProgressRecord> records = dao.findByCustomer(customerId);
    // Rule 9 / DECISIONS.md 31: ISO date strings sort chronologically as text.
    records.sort(Comparator.comparing(ProgressRecord::getDate));

    return switch (metric) {           // exhaustive: a new literal breaks the build
        case WEIGHT_KG -> ChartUtils.weightSeries(records);
        case BODY_FAT_PCT -> records.stream()
                .map(ProgressRecord::getBodyFatPct)   // double -> Double, boxed on purpose
                .toList();
    };
}
```

**How to know it is right.**

1. Feed three records out of order (2026-03-01, 2026-01-05, 2026-02-10) and assert `series(..., WEIGHT_KG)`
   comes back in date order — the assertion that catches insertion-order charts.
2. `series(..., BODY_FAT_PCT)` must return the fat percentages, not the weights — a one-word copy-paste
   bug that a type-safe `List<Double>` cannot catch, so the test is the only guard.
3. `targetWeight` on a customer with no records must return `null`, and the controller's chart must
   neither draw a line nor throw.
4. `save` with `weightKg = -1` or `bodyFatPct = 250` must throw before touching the DAO.

**Common mistakes.**

- **Building a `XYChart.Series` here.** That imports JavaFX and makes the service untestable without a
  toolkit; the method returns numbers.
- **Returning `0.0` for a missing target weight.** It draws a fake benchmark and hides real progress
  (the boxed `Double` exists to make that impossible).
- **Sorting in the controller "because the chart needs it".** Then two screens can disagree about the
  same series; the service owns the ordering.
- **Using `double` for money-like precision.** Weight is genuinely a `double` here (the entity says so),
  but never sum it into a money field, and never compare `BigDecimal` amounts with `==` in the same file.
- **Reaching for `CustomerDao` to validate the customer.** Not declared for this service: either add
  the edge through `docs/UML.md` §4 or accept orphan rows — see below.

> **Open detail (decide + log in `DECISIONS.md`).** **(1)** `save` cannot verify that `customerId`
> exists: no `CustomerDao`/`CustomerService` edge, so a typo can create a progress row no screen will
> ever show. Options: add the edge (a read of `findById` to reject an unknown customer) or accept it.
> **(2)** `ChartUtils` in §6 has only `weightSeries(List<ProgressRecord>)`, so `BODY_FAT_PCT` has no
> utility method — either add one to `ChartUtils` (a `UML.md` §6 change) or keep the stream in the
> service and say so. The current sketch does the latter; both are honest, silence is not.
> **(3)** `targetWeight`'s selection policy is unspecified: the newest record's value (returns `null`
> when the newest record left it blank, which erases the line) or the most recent **non-null** value
> (the benchmark persists until changed). **Recommend the second** — a benchmark is a setting, not a
> measurement — and write it down, because the two policies disagree exactly when a member skips the
> field once. **(4)** "One progress record per day" is not a rule; if the screens want it, that is a new
> rule plus a DAO lookup, not an assumption.

---

### `WorkoutService.java` — programs, versions and reassignment (owner: Yousef)

**Status:** planned.
**Canonical:** `docs/UML.md` §4 (signatures), §3 (`WorkoutProgramDao`, `CustomerDao`, `TrainerDao`);
`docs/UML.md` §1b note ("`version` is unique per customer") and §3
(`WorkoutProgramDao.findLatestVersion`).

**What it is.** The training plans a member follows: build one, correct the details before it is issued,
issue a new version when the coach changes the plan, move a program to another member, and remove one
entered by mistake. Two ideas carry the design: a program is **never edited in place once issued**
(a new version supersedes it), and `version` is unique **per customer**, not globally.

**Declared dependencies** (the only collaborators it may touch):

| Dependency | Type | Why it is here | What it must not become |
|---|---|---|---|
| `WorkoutProgramDao` | interface | `save`, `update`, `delete`, `findById`, `findByCustomer`, `findLatestVersion` | The only writer of the programs table |
| `CustomerDao` | interface | "does the assigned customer exist?" before a program is created or reassigned | Not a customer editor |
| `TrainerDao` | interface | "does the creator trainer exist?" — `creatorTrainerId` is NOT NULL | Not a roster editor |

**Methods** (all six from §4).

1. `WorkoutProgram buildProgram(int customerId, int trainerId, String name, String exercises)` — **issue a plan.**
   1. `CustomerDao.findById(customerId)`; `null` → `IllegalArgumentException("Customer not found.")`.
   2. `TrainerDao.findById(trainerId)`; `null` → throw. Two existence checks, in that order: the
      customer is the one the user picked, the trainer comes from the session.
   3. Reject a blank `name` or blank `exercises`. A program with no exercises is not a program the
      member can follow, and the failure should read like a form error, not like an empty table.
   4. `int next = dao.findLatestVersion(customerId) + 1;` — versioning is **per customer**
      (`UML.md` §1b), and `findLatestVersion` exists exactly for this line. The DAO's "no rows yet"
      answer must be `0`, not `null` (it returns `int`), which is why the first program is version `1`.
   5. `createdDate` = today. There is **no `DateUtils` edge** for this service — see the Open detail;
      do not build a date string by hand (rule 9).
   6. Save, set the returned id on the entity, return it.
2. `void reassign(int programId, int customerId)` — **move a program to another member.**
   1. Load the program (`null` → throw); load the target customer (`null` → throw).
   2. Refuse a no-op move to the same customer (a message beats a pointless write).
   3. **Recompute the version for the new owner**: `program.setVersion(dao.findLatestVersion(customerId) + 1)`.
      Keeping the old version would break `UML.md` §1b's "unique per customer" and make
      `findLatestVersion` ambiguous — the single most common bug in this file.
   4. Set `customerId`, update. `creatorTrainerId` stays as it is: the coach who wrote the plan did not
      change because the plan moved.
3. `WorkoutProgram newVersion(int programId)` — **supersede an issued plan.** See the sketch. Steps:
   load the source program (`null` → throw); build a **new row** copied from it; bump the version with
   `findLatestVersion(customerId) + 1`; stamp `createdDate` = today; save; return the new program. The
   old row is untouched — "superseded" is a comparison of versions, not a flag anywhere (derived state
   again, rule 13's spirit).
4. `void update(WorkoutProgram program)` — **correct a mistake before the plan is issued.**
   1. Reject `null` and `id <= 0`; `findById` → `null` → throw.
   2. Refuse to change the owner here (that is `reassign`'s use case) and refuse to change `version`
      (that is `newVersion`'s). If the stored row's `customerId` differs, throw; if `version` differs,
      throw with "use New version instead".
   3. Update the text/date fields. This method is deliberately narrow: a service that lets any field
      change is a CRUD pass-through with a rule hidden inside it.
5. `void delete(int programId)` — load (`null` → throw), then `dao.delete(programId)`. Legal: rule 19
   protects only `Attendance` and `Payment`, and rule 4's admin gate covers only `Customer`/`Trainer`.
   Deleting a *superseded* version is the normal case; deleting the only version leaves the member
   without a plan, which the screen may warn about but the service does not forbid.
6. `List<WorkoutProgram> findByCustomer(int customerId)` — reject `customerId <= 0`, then delegate and
   **sort by `version` descending**, so "the current plan" is the first element and the profile screen
   cannot accidentally show last month's programme.

**Why it is designed this way.** The alternative to versioning is a `WorkoutProgram.active` flag that
somebody has to clear. That was rejected: a flag is stored state that can be wrong (two active programs,
or none), whereas `MAX(version)` per customer is always true. The cost is one extra row per revision and
one extra `findLatestVersion` call — cheap, and it leaves a history a coach can point at. `update`
survives only for the gap between typing a plan and issuing it, which is why it refuses to touch the
owner and the version.

**Sketch.**

```java
// Sketch — authoritative signature: docs/UML.md §4. Versioning without a flag.
public WorkoutProgram newVersion(int programId) {
    WorkoutProgram old = dao.findById(programId);
    if (old == null) {
        throw new IllegalArgumentException("Program not found.");
    }
    WorkoutProgram next = new WorkoutProgram();
    // Copy the plan, not the identity: a superseded program keeps its own row.
    next.setCustomerId(old.getCustomerId());
    next.setCreatorTrainerId(old.getCreatorTrainerId());
    next.setName(old.getName());
    next.setExercises(old.getExercises());
    // Version is unique PER CUSTOMER (UML.md §1b) — never a global counter.
    next.setVersion(dao.findLatestVersion(old.getCustomerId()) + 1);
    next.setCreatedDate(today());   // date source: see Open detail below
    next.setId(dao.save(next));
    return next;
}
```

`today()` stands for the decision in the Open detail — it is **not** a `DateUtils` member today.

**How to know it is right.**

1. `buildProgram` three times for one customer → versions `1, 2, 3`; then once for a **different**
   customer → version `1` (not `4`). That second assertion is the whole per-customer rule.
2. `reassign` a version-1 program to a customer who already has versions 1–2 and assert the moved
   program becomes version 3.
3. `newVersion` must leave `findByCustomer` with **one more** row and the new row's `createdDate`
   different from the old one.
4. `update` with a changed `customerId` or `version` must throw, and the DAO must be untouched.
5. `buildProgram` with an unknown `customerId` **or** `trainerId` must throw before any save.

**Common mistakes.**

- **Using a global version counter.** `version` is per customer (`UML.md` §1b); a global counter makes
  "latest" meaningless and hides the plan history.
- **Editing the old row in place for a revision.** Then the history disappears and "which plan was the
  member following in March?" has no answer.
- **Forgetting to recompute the version in `reassign`.** Two rows for one customer share a version and
  `findLatestVersion` returns whichever the SQL happens to order first.
- **Letting `update` change `customerId`.** That is a reassignment with none of the version bookkeeping.
- **Building the creation date with `LocalDate.now().toString()` in three services.** Rule 9 gives
  parsing/formatting to `DateUtils`; three inline copies drift.

> **Open detail (decide + log in `DECISIONS.md`).** **(1)** `createdDate` needs "today", and this
> service declares no `DateUtils` — while `TrainerService`, `MembershipService` and `SchedulingService`
> do. Options: add `WorkoutService ..> DateUtils` in §4 (symmetric, one implementation of "today"), or
> leave `createdDate` to the caller (the signature has no date parameter, so that means a new signature
> — a `UML.md` change). **Recommend the edge**, once the team has decided where "today" comes from at
> all (see §7). **(2)** "One program per customer per day" / "no two versions share a created date" are
> not rules; do not add them. **(3)** If deleting the last remaining program should be refused, that is
> a new rule — the service currently allows it. **(4)** `exercises` is a free-text `String`; nothing
> validates its structure (sets/reps/rest is a UI convention, not a rule), so do not write a parser here.

---

### `SchedulingService.java` — booking, the session state machine and subscriptions (owner: Yousef)

**Status:** planned.
**Canonical:** `docs/UML.md` §4 (signatures), §3 (`SessionDao`, `TrainerAvailabilityDao`,
`TrainerSubscriptionDao`, `CustomerDao`, `TrainerDao`); rules **1**, **2**, **10**, **11**, **13**,
**15**, **16**, **17**, **18**; `docs/UML.md` "Enumerations" (`SessionStatus`, `SubscriptionStatus`).

**What it is.** The busiest file in the layer and the one that decides when the gym is double-booked.
It owns two things that look similar and are not: a **TrainerSubscription** (a paid *engagement* with
one trainer for a period) and a **Session** (one booked training slot). A **Membership** is gym access
and lives in `MembershipService`.

**Declared dependencies** (the only collaborators it may touch):

| Dependency | Type | Why it is here | What it must not become |
|---|---|---|---|
| `SessionDao` | interface | `save`, `update`, `findById`, `findByCustomer`, `findByCustomerAndDay`, `findByTrainerAndDay`, `countByAvailability`, `findAll` | The only writer of the sessions table |
| `TrainerAvailabilityDao` | interface | `findById`, `findOpenSlots`, `findByTrainerAndDate` — the slot being booked | **Never written from here** for capacity: `FULL` is derived (rule 13) |
| `TrainerSubscriptionDao` | interface | `findActive`, `findById`, `save`, `update`, `findByCustomer`, `findByTrainer` | The only writer of the subscriptions table |
| `CustomerDao` | interface | rule 15's assignment check and rule 11's "the session's trainer must be the assigned one" | Not a customer editor |
| `TrainerDao` | interface | `subscriptionPrice` reads the trainer's rate | Not a rate editor (`TrainerService.setRate` writes it) |
| `DateUtils` | `«utility»`, static | rule 10's `hoursUntil`, date math for subscriptions | Not a display formatter |

Note the two absences, because they shape the code: **no `PaymentService`** (this service never writes
money — a booking's price becomes a `Payment` only when somebody records one) and **no
`SessionContext`** (rule 16's role check belongs to the screen — see the `adminReserve` note).

**Methods** (all twenty-one from §4).

1. `int bookedCount(int availabilityId)` — **rule 17, one line.**
   `return sessionDao.countByAvailability(availabilityId, SessionStatus.CONFIRMED);`
   Passing `CONFIRMED` is the rule: `REQUESTED` holds no seat yet, and a `COMPLETED`, `NO_SHOW` or
   `CANCELLED` session releases the seat it took. Counts only — do not load sessions and filter in Java.
2. `boolean isSlotFull(int availabilityId)` — **derived capacity.**
   1. `TrainerAvailabilityDao.findById(availabilityId)`; `null` → `IllegalArgumentException`.
   2. `return bookedCount(availabilityId) >= slot.getMaxCustomers();` — `>=`, never `==`: a cap lowered
      below the occupancy (see `TrainerService`) must read as full, not as "one place left".
   3. This method is the *only* place `FULL` exists (rule 13). A slot at its cap is still stored
      `OPEN`; nothing writes `FULL`, and a cancelled session frees the seat because the count drops.
3. `List<Session> detectConflicts(int customerId, int trainerId, String date, String startTime)` —
   **"is anyone already busy?"**
   1. Reject blank `date`/`startTime`; keep an empty `List<Session> clash`.
   2. Customer side: `findByCustomerAndDay(customerId, date)` — a customer cannot be in two places at
      once. Trainer side: `findByTrainerAndDay(trainerId, date)` — nor can a coach.
   3. Keep the sessions that **occupy** the time (status filter: Open detail 2) and whose `startTime`
      matches. Return the clashes — **not** a boolean, so the screen can say *which* session collides.
   4. This method reads `SessionDao` only: asking `TrainerAvailabilityDao` would make a booking in a
      slot that was later closed look like a conflict.
4. `Session bookOpenSlot(int customerId, int availabilityId, String date, String startTime)` —
   **rule 1's "customer books a published OPEN slot → CONFIRMED".** See the sketch.
5. `Session requestOutsideSlots(int customerId, int trainerId, String date, String startTime)` —
   **rule 1's request path → REQUESTED:** customer and trainer must exist; reject a blank or
   already-past date/time (`DateUtils.isBeforeNow`); `detectConflicts` must be empty; build a `Session`
   with `availabilityId = null` (outside the published slots — that is the point of the method),
   `durationMin = 60`, the same rule-11 price guard as booking, `status = REQUESTED`; save and return.
   The trainer confirms later with `confirmSession`.
6. `Session adminReserve(int customerId, int trainerId, String date, String startTime)` —
   **rule 16's override.**
   1. **ADMIN is required** (rule 16). Per `UML.md` §2/§4 the check is
      `SessionContext.requireRole(StaffRole.ADMIN)` applied by the *screen*; `SessionContext` is not in
      this service's `..>` list, so do not import it here. The review rule: every call site of
      `adminReserve` sits behind `requireRole` (see Open detail 6 for a service-side check).
   2. Customer and trainer must exist. The slot rules are ignored by design: no slot lookup, no
      open-slot check, no cap check, no conflict refusal — a manager is putting somebody into a full or
      closed slot on purpose.
   3. Set the **status** per Open detail 1 (it changes what `bookedCount` reports), apply the rule-11
      price guard, save, return.
7. `boolean canTransition(SessionStatus from, SessionStatus to)` — **rule 18's single authority.** See
   the sketch. `null` on either side → `false`; an exhaustive `switch` over `from`; terminal states
   return `false` for everything.
8. `void confirmSession(int sessionId)` — **REQUESTED → CONFIRMED, with rule 11 applied.**
   1. Load (`null` → throw); `if (!canTransition(session.getStatus(), SessionStatus.CONFIRMED)) throw
      new IllegalStateException("Only a requested session can be confirmed.");` — rule 18 says *every*
      transition calls it.
   2. Re-check rule 11's trainer condition: the session's `trainerId` must equal the customer's assigned
      `trainerId` (`CustomerDao`). A request that arrived through a stale assignment is refused here
      rather than billed wrongly.
   3. `if (coveredBySubscription(session)) { session.setSubscriptionId(sub.getId()); session.setPrice(new
      BigDecimal("0.00")); }` — rule 11's zero price, the guard against double billing. Linking the
      subscription id makes the zero explainable on a receipt; *when* the link is written is Open
      detail 3's neighbour (booking vs. confirmation).
   4. `status = CONFIRMED`; `sessionDao.update(session);`
9. `void cancelSession(int sessionId, String reason)` — **cancel with a reason (rule 10).**
   1. Load; reject a blank `reason` — rule 10 says "with the reason recorded", so an empty reason is a
      rule violation, not a convenience.
   2. `canTransition(current, CANCELLED)` must be `true`, else throw. **Read rule 18 literally:** only
      `CONFIRMED` may become `CANCELLED`, so `REQUESTED` has no exit except `CONFIRMED` — see the
      consequence note below this list.
   3. Record `cancelReason`, set the status, update. Rule 10's two-hour window is checked with
      `DateUtils.hoursUntil(date, startTime)`; a late cancellation is **still** cancelled and still
      recorded — the check refuses nothing (Open detail 5).
   4. The row stays (history) and its seat frees up under rule 17.
10. `void completeSession(int sessionId, String sessionNotes)` — load; `canTransition(current,
    COMPLETED)`; set `sessionNotes` (may be blank — a coach is not obliged to write notes); set status;
    update. Completing frees the seat under rule 17, so the slot may be rebooked.
11. `void markNoShow(int sessionId)` — load; `canTransition(current, NO_SHOW)` (rule 18: only from
    `CONFIRMED`); set status; update. Rule 2 exists so trainer statistics can see it, and this is the
    only method allowed to produce that literal (`UML.md` "Enumerations").
12. `List<TrainerAvailability> getAvailableSlots(int trainerId, String date)` — reject `trainerId <= 0`
    and a blank date; `availabilityDao.findOpenSlots(trainerId, date)`; then **drop the slots where
    `isSlotFull(...)`** — rule 13 means they are stored `OPEN`, so this read path is where `FULL` is
    realised. Return the list; the screen renders time buttons, it does not re-filter.
13. `List<Session> getCustomerSessions(int customerId)` — reject `customerId <= 0`, then
    `sessionDao.findByCustomer(customerId)` (the profile screen's schedule tab). Sort by date, then
    start time, here so two screens cannot disagree about "next session".
14. `List<Session> getTrainerSessions(int trainerId)` — the coach's agenda. **`SessionDao` has no
    `findByTrainer(int)`**: §3 offers `findByTrainerAndDay(int, String)` and `findAll()`. See Open
    detail 4 — SQL in this file is not one of the options.
15. `TrainerSubscription subscribe(int customerId, int trainerId, String startDate, String endDate)` —
    **start a paid engagement.**
    1. Load the customer and the trainer (`null` → throw).
    2. **Rule 11/15:** `customer.getTrainerId()` must be non-null and equal to `trainerId`. A
       subscription to a trainer the customer is not assigned to can never cover a session, so it is a
       billing accident waiting to happen.
    3. Reject blank dates and `startDate.compareTo(endDate) > 0` (ISO strings sort — rule 9).
    4. Refuse a second live engagement: `findActive(customerId, trainerId, startDate) != null` → throw.
       Without this guard rule 6's subscription arm can bill the same period twice.
    5. `ratePerPeriod = subscriptionPrice(trainerId);` `status = ACTIVE` (explicit literal; `EXPIRED`
       is derived, rule 13). Save, set the id, return.
16. `TrainerSubscription activeSubscription(int customerId, int trainerId)` — reject non-positive ids;
    `TrainerSubscriptionDao.findActive(customerId, trainerId, <date>)`, where the date is "today" (the
    §7 register's item 3 — this is its third victim). Return `null` when there is none: "not subscribed"
    is a normal answer, not an error. Do not re-filter the status in Java; the DAO already restricts to
    `ACTIVE` and the date window, and two filters drift.
17. `BigDecimal subscriptionPrice(int trainerId)` — load the trainer (`null` → throw) and return the
    rate. Whether "flat trainer-set rate" means exactly `Trainer.hourlyRate` or a period multiplier is
    Open detail 3's sibling: inventing a multiplier would be inventing money.
18. `void renewSubscription(int subscriptionId, String newEndDate)` — load (`null` → throw); refuse
    `CANCELLED` (a mirror of rule 7's membership refusal — label it as a mirror, Open detail 6); reject a
    `newEndDate` that is not after the current one; set `endDate`; if the row reads `EXPIRED`, set
    `status = ACTIVE` because a renewal revives the engagement; update. Sessions already covered keep
    their zero price — a renewal must not retro-bill (rule 11 and rule 19's spirit).
19. `void cancelSubscription(int subscriptionId)` — load; already `CANCELLED` → refuse; set
    `status = CANCELLED`; update. This literal is set explicitly (`UML.md` "Enumerations":
    "`CANCELLED` is set by `cancelSubscription`"). It does **not** cancel the sessions booked under it:
    they were legitimately zero-priced and are separate history.
20. `boolean coveredBySubscription(Session session)` — **rule 11's guard, in three lines.** Reject
    `null`; `return activeSubscription(session.getCustomerId(), session.getTrainerId()) != null;`
    evaluated **for the session's own date** — a session next month is not covered by a subscription
    that ends this week, which is exactly why `findActive` takes a date. The caller applies the
    consequence (`price` becomes zero).
21. `int expireOverdueSubscriptions()` — sweep the `ACTIVE` engagements whose `endDate < today`, set
    `EXPIRED`, update, count, return. Same derived-and-swept duality as membership expiry, same "where
    is today?" register item, same rule that a read path must not wait for the sweep.

**The consequence of rule 18 you must know.** Rule 18 says: *"Only `CONFIRMED` sessions may become
`COMPLETED`, `NO_SHOW` or `CANCELLED`; only `REQUESTED` may become `CONFIRMED`."* Read literally — and
`UML.md` is authoritative — a `REQUESTED` session **has no way out except `CONFIRMED`**: a rejected
request cannot be cancelled. That is a real operational gap, and the honest handling is *not* to add a
transition in the service: the fix is a rule change in `docs/UML.md` (rule 18) proposed by the owner.
The sketch below implements the rule as written, so the code and the document agree.

**Why it is designed this way.** Three decisions carry this file. **(a)** Capacity is *never* stored:
`bookedCount` + `maxCustomers` is the truth, so a cancellation cannot leave a slot stale (rule 17) and
booking writes exactly one row. **(b)** The state machine is one pure function, `canTransition`, which
makes rule 18 testable as a 5×5 table and gives every transition method the same first line.
**(c)** Subscription coverage is *asked*, not remembered: `coveredBySubscription` recomputes from the
subscription table on every call, so cancelling a subscription cannot leave a session priced as if it
were covered. Two alternatives were rejected: a `Session.isCovered` flag (stored derived state that can
disagree with the subscription row) and a `Slot.full` flag (rule 13 forbids it outright).

**Sketch — the state machine (write this first).**

```java
// Sketch — authoritative signature: docs/UML.md §4. Rule 18, literally.
public boolean canTransition(SessionStatus from, SessionStatus to) {
    if (from == null || to == null) {
        return false;
    }
    return switch (from) {                 // exhaustive switch expression: no default
        case REQUESTED -> to == SessionStatus.CONFIRMED;
        case CONFIRMED -> to == SessionStatus.COMPLETED
                || to == SessionStatus.NO_SHOW
                || to == SessionStatus.CANCELLED;
        // Terminal: rule 18 gives these three no outgoing transition. Note the
        // consequence above: REQUESTED -> CANCELLED is NOT legal as the rule reads.
        case COMPLETED, NO_SHOW, CANCELLED -> false;
    };
}
```

**Sketch — the booking path (`bookOpenSlot`).**

```java
// Sketch — authoritative signature: docs/UML.md §4. Rules 1, 11, 13, 17 in one path.
public Session bookOpenSlot(int customerId, int availabilityId, String date, String startTime) {
    if (customerDao.findById(customerId) == null) {
        throw new IllegalArgumentException("Customer not found.");
    }
    TrainerAvailability slot = availabilityDao.findById(availabilityId);
    if (slot == null) {
        throw new IllegalArgumentException("That slot no longer exists.");
    }
    if (slot.getStatus() != AvailabilityStatus.OPEN) {
        // CLOSED is explicit; FULL never appears here because it is derived (rule 13).
        throw new IllegalStateException("That slot is closed.");
    }
    if (!slot.getDate().equals(date) || !slot.getStartTime().equals(startTime)) {
        // The form was stale: the slot moved. Rule 9 keeps both sides ISO strings,
        // so this comparison is exact and needs no parsing.
        throw new IllegalStateException("The slot changed - reload the day and try again.");
    }
    if (isSlotFull(availabilityId)) {                       // rule 17, CONFIRMED only
        throw new IllegalStateException("That slot is full.");
    }
    if (!detectConflicts(customerId, slot.getTrainerId(), date, startTime).isEmpty()) {
        throw new IllegalStateException("You already have a session at that time.");
    }
    TrainerSubscription sub = activeSubscription(customerId, slot.getTrainerId());
    Session s = new Session();
    s.setCustomerId(customerId);
    s.setTrainerId(slot.getTrainerId());
    s.setDate(date);
    s.setStartTime(startTime);
    s.setDurationMin(60);                                   // rule 1 default
    s.setAvailabilityId(availabilityId);
    s.setSubscriptionId(sub == null ? null : sub.getId());
    // Rule 11: covered by a subscription => price is zero, or a double bill appears.
    s.setPrice(sub == null ? adHocPrice(slot) : new BigDecimal("0.00"));
    s.setStatus(SessionStatus.CONFIRMED);                   // rule 1
    s.setId(sessionDao.save(s));
    // Note what is absent: nothing writes AvailabilityStatus.FULL. Rule 13 + rule 17
    // make the slot's capacity derived, so this whole use case is ONE insert — and
    // the transaction question in §6 never arises here.
    return s;
}
```

`adHocPrice(slot)` is **not** a `UML.md` member; it stands for the price decision in the Open detail
below (it must not be invented silently, because it is money).

**How to know it is right.**

1. `canTransition` as a table: 5×5 = 25 assertions. The three legal pairs are
   `REQUESTED→CONFIRMED`, `CONFIRMED→COMPLETED`, `CONFIRMED→NO_SHOW`, `CONFIRMED→CANCELLED` (four);
   everything else is `false` — including `REQUESTED→CANCELLED`, which is the assertion that documents
   the consequence above.
2. Rule 17 end to end: book a slot with `maxCustomers = 2`, assert `bookedCount == 1`; cancel the
   session, assert `bookedCount == 0` and that `getAvailableSlots` offers the slot again.
3. `bookOpenSlot` on a slot that was closed after the form was rendered must throw, and the DAO must
   hold no new session (`slot.getStatus() != OPEN`).
4. Rule 11: with a fake subscription for the pair, `bookOpenSlot` must store `price` equal to zero, and
   `compareTo(BigDecimal.ZERO) == 0` must be the assertion (never `equals`, §4 rule 5).
5. Rule 16: grep the call sites of `adminReserve` — each one must be preceded by
   `SessionContext.requireRole(StaffRole.ADMIN)`.

**Common mistakes.**

- **Writing `AvailabilityStatus.FULL`.** Rule 13: it is derived from `bookedCount >= maxCustomers`.
  One write and a cancelled session leaves the slot permanently unbookable.
- **Counting all sessions in a slot.** `bookedCount` passes `CONFIRMED` (rule 17); counting
  `REQUESTED` too makes a pending request hold a seat nobody paid for, and counting `CANCELLED` makes
  the slot never free.
- **Checking transitions with `if (status != CANCELLED)` in each method.** That is four authorities for
  one rule; rule 18 names `canTransition`.
- **Confirming a session whose trainer is no longer the customer's assigned trainer.** Rule 11 requires
  the equality, and rule 15 says a reassignment moves the assignment — so the check must run at confirm
  time, not only at booking time.
- **Billing a covered session.** `price` must be zero (rule 11); a non-zero price plus a subscription
  payment is exactly the double bill the rule prevents.
- **Cancelling the sessions when a subscription is cancelled.** The sessions were legitimately
  zero-priced history; cancelling them rewrites the past (rule 11 and rule 19's spirit).
- **Importing `SessionContext` here to enforce ADMIN.** Not in this service's `..>` list; the screen
  gates the override (rule 16), and a service-side check needs a documented dependency change first.

> **Open detail (decide + log in `DECISIONS.md`) — the six that shape this file.** All six row numbers
> are in the §7 register.
>
> **1. `adminReserve`'s resulting status.** Rule 1 says it "ignores slot rules" but not what it
> creates. `CONFIRMED` (a real booking: it consumes a seat under rule 17 and can push a slot over its
> cap) or `REQUESTED` (the override stays visible as an exception, and the trainer still confirms)?
> **This changes capacity arithmetic**, so it must be decided, not discovered.
>
> **2. The conflict status filter and window overlap.** Which statuses occupy a time slot?
> **Recommend `REQUESTED`, `CONFIRMED`, `COMPLETED`** — a `CANCELLED` seat is free and a `NO_SHOW` seat
> was free. And is a clash "same `startTime`" (what today's signature supports) or "overlapping
> windows" (needs `durationMin` arithmetic and a rule)?
>
> **3. The ad-hoc session price.** `Session.price` must be filled for an uncovered booking and for a
> request, and no rule gives the number. Options: **(a)** the trainer's `hourlyRate` pro-rated by
> `durationMin` (needs `BigDecimal` division with `RoundingMode.HALF_UP`); **(b)** `hourlyRate` flat per
> session; **(c)** a gym-wide price constant — no rule and no entity field exists for it; **(d)** leave
> it zero and let the payment screen fill it, which makes rule 6's session arm unable to bill anything.
> **Recommend (a)**, written into `DECISIONS.md` with the rounding rule, because it is the only option
> that uses data the model already has. The same question decides `subscriptionPrice`.
>
> **4. `getTrainerSessions` has no DAO query.** Options: **(a)** add `SessionDao.findByTrainer(int)` to
> §3 (the honest fix for an agenda screen); **(b)** filter `findAll()` in the service (one stream, reads
> the whole table — fine at this scale); **(c)** loop day by day (many queries). SQL in the service is
> not on the list.
>
> **5. "Today" again, and `hoursUntil`'s role.** `activeSubscription`, `expireOverdueSubscriptions`,
> `requestOutsideSlots` and `cancelSession` all need "today" (register item 3). For rule 10: a late
> cancellation is still cancelled, so `hoursUntil` can currently only produce a *warning*, and there is
> no penalty field for "free" to be recorded in. Let the controller show the warning, or add a
> field/rule — never charge silently.
>
> **6. Does the subscription path mirror rule 7, and does a renewal re-activate?** Rule 7's refusals are
> written for *memberships*; the subscription behaviour is a mirror, not a rule. Keep the mirror and
> label it as one in the code until it is decided. If the team wants a service-side ADMIN re-check on
> `adminReserve`, that is a new `SessionContext` dependency edge for this service.

---

### `AdminService.java` — identities, overrides and the only hard deletes (owner: Abdelrhman)

**Status:** planned.
**Canonical:** `docs/UML.md` §4 (signatures), §3 (`AdminDao`, `CustomerDao`, `TrainerDao`), §6
(`PasswordUtils`, `GuardService`); rules **4**, **15**, **16**, **19**; `DECISIONS.md` 39
(`GuardService`), 40 (`DaoException`).

**What it is.** Two jobs in one file. The first is **identity**: authenticate a staff member, change a
password, add/edit/remove operators, list them by role. The second is the **override desk**: everything
that deliberately bypasses an ownership rule — reassign a member to another trainer, adjust money that
was recorded wrongly, and hard-delete a customer or trainer that nothing references. Rule 16 is the
frame for all of it: `StaffRole.ADMIN` is required for every operation here **and** for every override
elsewhere (`SchedulingService.adminReserve` included).

**Declared dependencies** (the only collaborators it may touch):

| Dependency | Type | Why it is here | What it must not become |
|---|---|---|---|
| `AdminDao` | interface | `save`, `update`, `delete`, `findByUsername`, `findById`, `findAll`, `findByRole` | The only writer of the administrators table |
| `PasswordUtils` | `«utility»`, static | `hash(plain)`, `verify(plain, hash)` — PBKDF2, no new dependencies | Not a place for a password *policy*; the policy (if any) is a rule |
| `GuardService` | `«utility»`, static | rule 4: `countReferences(table, id)` before a hard delete | The only reference counter; do not add per-DAO `countBy…` methods (`DECISIONS.md` 39) |
| `PaymentService` | service | rule 19's money path: void and adjust **through** the service that owns payments | Not a second payment writer — never `PaymentDao` here |
| `CustomerDao` | interface | `reassignTrainer` writes `Customer.trainerId`; `deleteCustomer` removes the row | The override path may write here; the *normal* assignment path may not (`TrainerService` has no such edge) |
| `TrainerDao` | interface | `deleteTrainer`; exists-checks before an override | Not a roster editor — rates and slots belong to `TrainerService` |

**Methods** (all eleven from §4).

1. `Administrator authenticate(String username, String password)` — **login, with three null cases.**
   See the sketch. Steps: reject blank inputs **by returning `null`** (not by throwing — the login
   screen shows one message for every failure); `AdminDao.findByUsername(username)`; `null` → `null`;
   `!PasswordUtils.verify(password, admin.getPasswordHash())` → `null`; `!admin.isActive()` → `null`
   (rule 16: `active == false` is rejected); otherwise return the administrator.
   **Never** log the password, never compare hashes with `==`, and never return a partially filled
   object.
2. `void resetPassword(int adminId, String newPassword)` — load (`null` → throw); reject a blank
   password; `admin.setPasswordHash(PasswordUtils.hash(newPassword))`; update. The plain text must not
   reach the entity, a log, or a message. See the Open detail about a minimum-length policy.
3. `int saveStaff(Administrator admin)` — **add an operator.**
   1. Reject `null`, a blank `fullName`/`username`, and a `null` role.
   2. Uniqueness: `AdminDao.findByUsername(admin.getUsername()) != null` → refuse
      (`IllegalStateException("That username is taken.")`). There is no DB unique constraint to rely on
      in `schema.sql` yet, and even when there is, the message must be readable.
   3. **Hash before save**: `admin.setPasswordHash(PasswordUtils.hash(admin.getPasswordHash()))` — the
      controller collects plain text in a password field; this is the one place it becomes a hash.
   4. Default `active = true` and `createdDate` = today (the "today" Open detail in §7 applies here too).
   5. `return dao.save(admin);`
4. `void updateStaff(Administrator admin)` — load (`null` → throw); re-check the blank fields; **do not
   overwrite the stored hash with an empty string** (the classic staff-edit bug: the password field is
   left empty because the operator only changed a phone number, and the account locks itself out). Copy
   `passwordHash` from the stored row unless a new hash was supplied. Update.
5. `void deleteStaff(int adminId)` — **remove an operator.**
   1. Load (`null` → throw).
   2. Consider the two lock-out cases (see the Open detail): deleting your own signed-in account, and
      deleting the last `ADMIN`. Neither is forbidden by a rule today; both are one `if` away and belong
      in `DECISIONS.md` before they are coded.
   3. `dao.delete(adminId);` — no `GuardService` call is needed for *this* table, because no other table
      references an administrator (rule 4's reference check is about customers and trainers).
6. `List<Administrator> findAllStaff()` — one step: `return dao.findAll();` The staff screen's table.
7. `List<Administrator> findByRole(StaffRole role)` — reject `null role`, then delegate. This is the read
   behind the `StaffRole` enum being reachable from a service operation (`UML.md` "Notes on scope").
8. `void reassignTrainer(int customerId, int trainerId)` — **rule 15's override.**
   1. Load the customer and the trainer; either `null` → throw.
   2. Set `customer.setTrainerId(trainerId)`; `CustomerDao.update(customer);`
   3. Because rule 15 makes this field authoritative, the write moves the trainer's roster membership
      **and** their medical access with it. Say so in the confirmation dialog: this is the override that
      changes who may read a medical file.
   4. ⚠️ It can also leave a live `TrainerSubscription` pointing at the *old* trainer, which rule 11
      forbids — and this service cannot even detect it with the dependencies §4 declares. See the Open
      detail; do not "fix" it by reaching for `TrainerSubscriptionDao`.
9. `void adjustPayment(int paymentId, BigDecimal newAmount, String reason)` — **rule 19's money correction.**
   1. Load the payment **through `PaymentService`** (`findByCustomer`? no — see step 2):
      `PaymentService` has no `findById` in §4, so the practical route is `voidPayment(paymentId,
      reason)` followed by recording the replacement through `PaymentService.record(...)`. Check the
      signature list before writing: the declared edge is `AdminService ..> PaymentService`, and the
      methods available there are `record`, `voidPayment`, `voidRecord`, `balance`,
      `revenueBetween`, `findByCustomer`, `findByReceipt`, `findByMethod`.
   2. `newAmount` must be non-null and non-negative; a `null reason` is a rule violation (rule 19 needs
      the trail to be explainable).
   3. Void the original with the reason (`PaymentService.voidPayment`), then create the replacement with
      the **same XOR link** as the original and `previousAmount` set to the original's amount, so a
      receipt can always be reconciled (rule 19).
   4. The replacement keeps the receipt number discussion in `PaymentService`'s Open detail — decide it
      there, once.
10. `void deleteCustomer(int customerId)` — **rule 4's hard delete.** See the sketch. Steps: load
    (`null` → throw); `int refs = GuardService.countReferences(<customers table>, customerId);` if
    `refs > 0` → refuse with a message that says *why* ("This customer has 12 records — deactivate them
    instead."); otherwise `CustomerDao.delete(customerId)`.
11. `void deleteTrainer(int trainerId)` — the same three steps against the trainers table. Note the
    references that matter: `Customer.trainerId`, `Session.trainerId`, `TrainerAvailability.trainerId`,
    and `TrainerSubscription.trainerId` (which is why rule 15's reassignment must happen *before* a
    trainer can ever be deleted).

**Why it is designed this way.** `authenticate` returning `null` instead of throwing looks lax and is
deliberate: login has exactly one thing to say ("username or password is wrong"), and any distinction —
unknown user, wrong password, deactivated account — is a hint an attacker can use and a user cannot act
on. Rule 16 fixes the same behaviour: `active == false` fails exactly like a wrong password. The second
decision is `GuardService` on the delete path: nine tables reference a customer and `schema.sql` has no
cascades, so the reference check has to happen before the delete, in one place, with one vocabulary of
table names (`DECISIONS.md` 39). The third is what is *absent*: no `PaymentDao` here. Money is corrected
**through** `PaymentService`, which is what the `..>` edge `AdminService ..> PaymentService : voids and
adjusts through` means; an admin who writes payments directly would bypass rule 12.

**Sketch.**

```java
// Sketch — authoritative signatures: docs/UML.md §4.
public Administrator authenticate(String username, String password) {
    if (username == null || username.isBlank() || password == null || password.isBlank()) {
        return null;                    // one answer for every failure, by design
    }
    Administrator admin = dao.findByUsername(username.trim());
    if (admin == null) {
        return null;                    // unknown user
    }
    if (!PasswordUtils.verify(password, admin.getPasswordHash())) {
        return null;                    // wrong password
    }
    if (!admin.isActive()) {
        return null;                    // rule 16: active == false is rejected
    }
    return admin;
}

public void deleteCustomer(int customerId) {
    Customer customer = customerDao.findById(customerId);
    if (customer == null) {
        throw new IllegalArgumentException("Customer not found.");
    }
    // Rule 4: refuse while references exist. No FK cascades exist in schema.sql,
    // so this count is the only thing standing between us and orphaned rows.
    int refs = GuardService.countReferences("customers", customerId);
    if (refs > 0) {
        throw new IllegalStateException(
                "This customer still has " + refs + " linked records - deactivate them instead.");
    }
    customerDao.delete(customerId);
}
```

`"customers"` must be the **exact** table name from `schema.sql`; the string vocabulary belongs to
`GuardService` and the DAO layer, not to scattered literals (see the Open detail).

**How to know it is right.**

1. `authenticate` four ways: unknown user, wrong password, correct password + `active == false`,
   correct password + active → the first three `null`, the fourth the administrator. Assert that no
   path throws.
2. `saveStaff` twice with the same username → the second throws; assert the stored `passwordHash` is
   **not** the plain password and that it verifies with `PasswordUtils.verify`.
3. `deleteCustomer` on a customer with one membership → throws, and the fake `CustomerDao` still holds
   the row; on a customer with no references → the row is gone.
4. `adjustPayment` end to end: after the call, `PaymentService` must hold **two** rows for that money —
   the voided original and the replacement with `previousAmount` set — and no row may be deleted
   (rule 19).
5. Grep this file for `PaymentDao` and for `delete(` on payments: neither may appear.

**Common mistakes.**

- **Logging or echoing the plain password, or storing it unhashed.** `PasswordUtils.hash` is the only
  path into `passwordHash`; `authenticate` never touches a hash directly.
- **`updateStaff` blanking the hash.** Leaving the password field empty on an edit form and writing it
  back locks the operator out of the system with no error message anywhere.
- **Hard-deleting without `GuardService`.** The row disappears, the memberships/sessions/payments stay,
  and every later join returns nothing: the orphan bug rule 4 exists to prevent.
- **Reaching for `PaymentDao` to "fix" an amount.** Money is corrected through `PaymentService`
  (void + replacement); a direct write bypasses rule 12 and leaves no void trail.
- **Enforcing ADMIN by trusting that only `StaffController` calls this service.** Rule 16 is satisfied
  by `SessionContext.requireRole(StaffRole.ADMIN)` on every call site — including the `adminReserve`
  path in `ScheduleController`.
- **Reassigning a trainer and calling the old subscription "somebody else's problem".** It leaves a
  live row that violates rule 11; see the Open detail.

> **Open detail (decide + log in `DECISIONS.md`) — four gaps.**
>
> **(1) `reassignTrainer` vs. a live `TrainerSubscription` (rule 11).** After a reassignment, an
> engagement with the previous trainer still exists with a `trainerId` that no longer equals
> `Customer.trainerId`, and `SchedulingService.coveredBySubscription` can no longer cover its sessions.
> §4 gives this service **no `TrainerSubscriptionDao` and no `SchedulingService`**, so it cannot even
> see the problem. Options: **(a)** add `AdminService ..> SchedulingService` and cancel or expire the
> old engagement as part of the override (an explicit, auditable write, and a `UML.md` change);
> **(b)** document a precondition — the operator must cancel the engagement first — and have the screen
> warn about it; **(c)** leave it and let rule 11 be violated silently (not acceptable once it is
> written down). **Recommend (a) or (b), never a direct `TrainerSubscriptionDao` import.**
>
> **(2) `deleteCustomer` and the payments that can never be deleted.** Rule 19 keeps every payment row,
> and a payment has a `customerId`, so any customer with payment history has a non-zero reference count
> **forever** — unless `GuardService` ignores voided payments, which is itself a decision (a voided row
> still references the customer). Options: **(a)** `GuardService` counts only non-voided references
> (needs the table→voided-column mapping, and it is the only reading under which the "void then delete"
> flow works); **(b)** hard delete is effectively impossible for anyone with history, and `INACTIVE` is
> the real answer (rule 4 says exactly that: "delete" *means* `INACTIVE`). **Recommend documenting (b)
> and implementing (a) only if a real delete flow is required** — and note that `voidRecord`'s purpose
> depends on which one you pick.
>
> **(3) `GuardService`'s table vocabulary.** `countReferences(String table, int id)` takes a raw table
> name. The strings must match `schema.sql` exactly, and `schema.sql` is not written yet
> (`dao/README.md` has the Week-1 specs for four tables only). Put the mapping in one place — a private
> constant map inside `GuardService`, or a documented list in `dao/README.md` — and never spell table
> names inline in a service.
>
> **(4) Staff lock-out rules.** Deleting the last `ADMIN`, or the account you are signed in as, is
> currently allowed. Also unspecified: a minimum password length, and whether a `MANAGER` may call any
> of these methods (rule 16 says ADMIN for *every* `AdminService` operation, so until that changes, the
> answer is no). Decide and write it down; these are one-line guards, and they are the difference
> between a demo and a system somebody could operate.

---

## 6. How this layer connects to the others

**The call flow, one request at a time.**

1. A button in an FXML screen fires a handler in an `XxxController` (FX thread).
2. The controller reads the form into an entity or into plain arguments — **no SQL, no rule**.
3. The controller calls **one** service method: `schedulingService.bookOpenSlot(...)`.
4. The service validates its inputs and applies the rules, loading whatever it needs through its
   **declared** DAO interfaces (`availabilityDao.findById`, `sessionDao.countByAvailability`, …).
5. Each DAO impl opens a connection through `DbConnection`, runs its prepared statement, and maps the
   `ResultSet` back into entities.
6. The service returns data (`Session`, `BigDecimal`, `List<Double>`) or throws
   (`IllegalArgumentException`/`IllegalStateException`, or a `DaoException` from below).
7. The controller renders the result — `FxUtils.info/snackbar` for success,
   `FxUtils.error(e.getMessage())` for a failure — and never a stack trace.
8. Nothing above the service layer ever sees a `Connection`, a `ResultSet`, or a `SQLException`.

| This layer | Depends on | Is used by |
|---|---|---|
| `service` | `model` entities, `dao` interfaces (the declared subset per §4), `util` (`Validators`, `DateUtils`, `PasswordUtils`, `GuardService`, `ChartUtils`) | 16 controllers; `AdminService` is used by `LoginController` and `StaffController` |
| — | **never:** JavaFX, `FxUtils`, `m3fx`, SQL, `DbConnection` impls, `SqliteDbConnection` | DAOs do not call services (no upward calls), and entities do not know services exist |

**The seam.** The interfaces are the whole reason this layer is swappable: a service holds
`CustomerDao`, never `CustomerDaoImpl`, and the impl in turn holds `DbConnection`, never
`SqliteDbConnection` (`DECISIONS.md` 33). So a service test needs nothing but hand-written fakes, and
changing the database touches the composition root plus one `DbConnection` implementation
(`LEARNING_GUIDE.md` §3.8 asks you to point at that one place — it is `MainApp`/bootstrap, the only code
allowed to say `new SqliteDbConnection()`).

| Dependency | Direction | What it buys |
|---|---|---|
| `XxxDao` interfaces | service → interface → impl | Unit tests with in-memory fakes instead of SQLite |
| `DbConnection` interface | DAO impl → interface → `SqliteDbConnection` | The database can change in one file |
| `MedicalProfileService.getForTrainer` | `TrainerService` → service → DAO | One enforcement point for medical access (rule 15) |
| `PaymentService` | `AdminService` → service → DAO | Money corrections keep rule 12's validation (rule 19) |

### Cross-service rules worth knowing before you code

- **A service never imports JavaFX and never renders a message.** Return data or throw; the controller
  calls `FxUtils.error(...)`. There is no `FxUtils` dependency in this layer at all.
- **A service only touches DAOs it declares.** If you need another aggregate's data, call its service —
  `TrainerService` reads medical data through `MedicalProfileService.getForTrainer`, not through
  `MedicalProfileDao`. Reaching into a table another service owns is a review failure, and §5 marks the
  three places where the declared list currently has no legal path at all (see §7's register).
- **Rule 18 holds the state machine.** Only `CONFIRMED` may become `COMPLETED`/`NO_SHOW`/`CANCELLED`;
  only `REQUESTED` may become `CONFIRMED`. Every transition calls `canTransition` first (and note the
  literal consequence in the `SchedulingService` entry: `REQUESTED → CANCELLED` is not legal).
- **Overrides need ADMIN** — that includes `SchedulingService.adminReserve`, not just `AdminService`
  (rule 16). The gate is `SessionContext.requireRole(StaffRole.ADMIN)` at the call site.
- **Rule 17's count is the only capacity truth.** `bookedCount` counts `CONFIRMED` only, so cancelling
  or completing a session frees its seat. Never store `FULL` (rule 13).
- **Rule 11 and rule 12 are guards, not documentation.** `coveredBySubscription` keeps a covered
  session's price at zero; `validLink` runs inside `record`. If you find yourself billing around them,
  the rule is what is wrong — change it in `UML.md` first.
- **Rules 19's two tables are append-only.** `Attendance` and `Payment` rows are never deleted; money is
  corrected by void plus a new row carrying `previousAmount`.

> **Open detail (decide + log in `DECISIONS.md`) — transaction boundaries for multi-row operations.**
> The DAO surface has no transaction seam: every call opens its own connection, so a service method
> that writes twice is **not** atomic. The methods affected are small and countable:
> `MembershipService.freezeForSuspended` (n updates), `TrainerService.setDailyCap` (n updates),
> `AdminService.adjustPayment` (a void plus an insert), and any future reassignment that cancels a
> subscription. Options: **(a)** accept per-call atomicity and make every operation **re-runnable and
> idempotent** (freeze skips anything not `ACTIVE`, the daily cap re-applies, a second void is refused)
> — this is what the rules already push you towards, and it costs nothing; **(b)** add a transaction seam
> (`DbConnection.begin/commit/rollback`, or a `runInTransaction` helper) — a new `util` member, so a
> `UML.md` §6 change plus `DECISIONS.md`; **(c)** push the multi-row work into one DAO method — SQL
> inside one method is atomic, but it hides a rule in the storage layer. **Recommend (a) now, (b) if a
> partial failure is ever observed**, and note that rule 13 makes the worst case smaller than it looks:
> because `FULL` is derived, booking is a *single* insert.

---

## 7. Common mistakes and review checklist

### Mistakes we have actually seen (or expect on the first pass)

| Symptom | What is really wrong | Fix |
|---|---|---|
| A slot stays unbookable after a cancellation | `AvailabilityStatus.FULL` was written somewhere | Delete the write; `FULL` is derived (`bookedCount >= maxCustomers`, rule 13) |
| The same customer is billed twice for a month | `price` was left non-zero on a covered session, or `balance` counted a subscription twice | `coveredBySubscription` before saving (rule 11); one unpaid item per source in `balance` (rule 6) |
| Balance shows `450.0` next to a stored `450.00` and "changed" | `equals`/`==` used on `BigDecimal` | `compareTo(...) == 0`, and `setScale(2, RoundingMode.HALF_UP)` when producing a total |
| The revenue report is a few cents off | `double` crept into a total | `BigDecimal` end to end (`DECISIONS.md` 31) |
| Two screens show different dates for the same membership | A second date parser exists (a `SimpleDateFormat` or a `LocalDate.parse`) | `DateUtils` owns every parse (rule 9) |
| A trainer sees a customer's medical file they are not assigned to | `Customer.trainerId` compared wrongly, or `MedicalProfileDao` read outside the service | Compare `Integer` ids with `.equals`, refuse `null` `trainerId`, and keep rule 15's read path in `getForTrainer` |
| `markNoShow` succeeds on a cancelled session | The transition check was skipped | First line of every transition method: `canTransition(...)` (rule 18) |
| `NullPointerException` in the UI | A DAO returned `null` and nobody checked | Not-found policy per service (§4's error-handling table); `SUM` over zero rows is `NULL`, not `0` |
| An admin deletes a customer and the memberships survive | No `GuardService.countReferences` call | Rule 4: count first, refuse while `> 0` |
| `SQLException: [SQLITE_BUSY]` reaches a dialog | A DAO leaked a connection, or a service opened one | `try`-with-resources in the DAO; a service must never touch JDBC |
| Two services disagree about which slots are free | One of them filtered `FULL` in Java, the other did not | One read path per question, same filter, same rule number |
| A validation message reads "membershipId=7 status=CANCELLED" | The service formatted internals | Rewrite as a sentence; the controller shows it verbatim |

### The open-details register

This README found places where the rules genuinely do not say what to do. Each is marked **Open
detail** in §5 and must be decided in `docs/DECISIONS.md` (and, where a signature or field is involved,
in `docs/UML.md` first). Nothing here was invented to fill a gap.

| # | Where | Gap | Who decides |
|---|---|---|---|
| 1 | `MembershipService.sell`/`renew` | Where a plan's price and period length come from (`PlanType` has no members in `UML.md` §1) | Ziad |
| 2 | `MembershipService.freeze`/`unfreeze` | What fills `frozenDays` at freeze time (no duration parameter, no freeze date field) | Ziad |
| 3 | `expireOverdue`, `expiryReminders`, `freeze`, `sell`, `activeSubscription`, `expireOverdueSubscriptions`, `requestOutsideSlots`, `cancelSession`, `WorkoutService.buildProgram`, `MedicalProfileService.save`, `AdminService.saveStaff` | **Where "today" comes from.** `UML.md` §6 has `addMonths`, `formatDate`, `isBeforeNow`, `hoursUntil` — no `today()`. Options: **(a)** add `DateUtils.today() String` to §6 and declare the `DateUtils` edge where it is missing (`WorkoutService`, `MedicalProfileService`, `PaymentService`, `AttendanceService`); **(b)** let each service call `LocalDate.now()` — rejected, it is the second implementation rule 9 forbids; **(c)** let the controller pass the date — impossible for methods that have no date parameter, and rule 9 forbids a controller building one. **Recommend (a)**, one member, one clock | whole team |
| 4 | `MembershipService.cancel`, `sell` | Idempotent vs. refusing the second cancel; selling to a `SUSPENDED` customer | Ziad |
| 5 | `TrainerService.assignCustomer` | **No legal write path** to `Customer.trainerId` (no `CustomerDao`/`CustomerService` edge) | Ziad + Abdelrhman |
| 6 | `TrainerService.setDailyCap`/`publishSlot` | Where a day's cap is stored so rule 8's "re-applies after any later publish" is true | Ziad |
| 7 | `TrainerService.publishSlot` | Overlapping slots on one day are not forbidden, and conflicts compare start times only | Ziad + Yousef |
| 8 | `TrainerService.setSlotCap` | Lowering a cap below the occupancy: refuse or allow? | Ziad |
| 9 | `PaymentService.validLink`/`balance` | **No `TrainerSubscriptionDao` edge**, so rule 12's third arm and rule 6's third source have no read path | Abdel Raouf |
| 10 | `PaymentService.balance` | Which rows count as unpaid per source (status whitelist) — a money rule | Abdel Raouf |
| 11 | `PaymentService.record`/`voidPayment` | Receipt-number generation, `voidedDate`, and zero/negative amounts | Abdel Raouf |
| 12 | `PaymentService.findByReceipt` + `AdminService.adjustPayment` | Which of the two rows sharing a receipt number wins after an adjustment | Abdel Raouf + Abdelrhman |
| 13 | `PaymentService.voidRecord` / `AdminService.deleteCustomer` | Does `GuardService` count voided payments, and does the "void then delete" flow exist at all? | Abdelrhman |
| 14 | `SchedulingService.adminReserve` | The status it creates — `CONFIRMED` consumes a seat in a possibly closed slot (rule 17) | Yousef |
| 15 | `SchedulingService.detectConflicts` | Which statuses occupy a time slot, and whether a clash is an exact start time or an overlapping window | Yousef |
| 16 | `SchedulingService.bookOpenSlot`/`requestOutsideSlots` | The ad-hoc session price for an uncovered session | Yousef |
| 17 | `SchedulingService.getTrainerSessions` | No `SessionDao.findByTrainer(int)` exists | Yousef |
| 18 | `SchedulingService.cancelSession` | Rule 10's "free" has no field to write; `hoursUntil` can only produce a warning | Yousef |
| 19 | `SchedulingService.canTransition` etc. | Rule 18 read literally leaves `REQUESTED` with no exit except `CONFIRMED` — a rule change, not a code change | Yousef (+ `UML.md`) |
| 20 | `AdminService.reassignTrainer` | A live subscription to the old trainer violates rule 11 and this service cannot see it | Abdelrhman |
| 21 | `AdminService.deleteStaff`, `resetPassword` | Deleting the last/own `ADMIN`; minimum password policy | Abdelrhman |
| 22 | `CustomerService.validate`, `search`, `changeStatus(INACTIVE)` | Unvalidated `trainerId` FK; blank search; whether `INACTIVE` ends active memberships (rule 4's wording) | Abdelrhman |
| 23 | `EquipmentService.changeStatus` | No transition matrix for `EquipmentStatus` (rule 20 candidate) | Yousef |
| 24 | `ProgressService` | No customer-existence check; `ChartUtils` has `weightSeries` only; `targetWeight` selection policy | Abdel Raouf |
| 25 | `MedicalProfileService` | `updatedDate` source, unvalidated `doctorPhone`, ungated `delete` | Abdelrhman |
| 26 | all services | Transaction boundaries for multi-row operations (see §6) | whole team |

### Review checklist (run before approving a PR that touches this folder)

- [ ] The file imports **no** JavaFX and mentions no `FxUtils`, no dialog, no `ObservableList`.
- [ ] Every field is `private final`; there is no `static` mutable state; there is no setter for a DAO.
- [ ] Every collaborator appears in that service's `..>` list in `docs/UML.md` §4 — **and the list was
      updated first** if a new one was added.
- [ ] No `new XxxDaoImpl(...)`, no `Connection`, no `PreparedStatement`, no SQL string.
- [ ] No method signature differs from §4 (names, parameter types, return types, `throws` nothing).
- [ ] Every business rule the method enforces is named in a comment by its number (rule 7, rule 17, …).
- [ ] `canTransition` is the first check in every one of the four transition methods (rule 18).
- [ ] No code writes `FULL`, `EXPIRED` or `CANCELLED` outside its owner (rules 13, 18).
- [ ] Money is `BigDecimal` with the `String` constructor; comparisons use `compareTo`; division has a
      `RoundingMode`.
- [ ] Dates/times are ISO strings produced or parsed by `DateUtils`; no `SimpleDateFormat` anywhere.
- [ ] No `.delete(` on attendance or payment (rule 19), and no `delete` at all in `CustomerService`.
- [ ] Every failure message is a sentence a receptionist could read: no ids, no table names, no class
      names, no stack traces.
- [ ] The service formats nothing: money leaves as `BigDecimal`, dates as `yyyy-MM-dd`, series as
      `List<Double>`.
- [ ] Every override (`adminReserve`, `reassignTrainer`, `adjustPayment`, `deleteCustomer`,
      `deleteTrainer`) is reachable only behind `SessionContext.requireRole(StaffRole.ADMIN)` (rule 16).
- [ ] New gaps are logged as **Open detail** in this README **and** in `DECISIONS.md` — not resolved
      silently in code.

---

## 8. Learning exercises (with answers)

Answerable from this README and `docs/UML.md` — the app does not need to run.

**Exercise 1 (read).** Four services declare `DateUtils` in §4. Which ones?
Then list three methods in *other* services that need "today" and explain why the parameter list does
not give it to them.

**Exercise 2 (write).** Write `bookedCount` and `isSlotFull` from memory. Then explain, in one
sentence each, why the status argument is `CONFIRMED` and why the comparison is `>=` and not `==`.

**Exercise 3 (find the bug).** This method was proposed for `SchedulingService`. Name every rule it
breaks.

```java
public Session book(int customerId, int availabilityId, String date, String startTime) {
    TrainerAvailability slot = availabilityDao.findById(availabilityId);
    Session s = new Session();
    s.setCustomerId(customerId);
    s.setTrainerId(slot.getTrainerId());
    s.setDate(date);
    s.setStartTime(startTime);
    s.setAvailabilityId(availabilityId);
    s.setPrice(new BigDecimal(60.00));
    s.setStatus(SessionStatus.CONFIRMED);
    if (bookedCount(availabilityId) == slot.getMaxCustomers()) {
        slot.setStatus(AvailabilityStatus.FULL);
        availabilityDao.update(slot);
    }
    s.setId(sessionDao.save(s));
    return s;
}
```

**Exercise 4 (rules).** Rule 12 is enforced by `PaymentService.validLink`, which is `public` and
returns `boolean`. Give two reasons for the `public` and one reason the method returns a `boolean`
instead of throwing.

**Exercise 5 (extend without breaking a rule).** Product asks for "a pending request can be cancelled".
Nothing in the code needs to change first — say exactly what has to happen, in which document, in which
order, and which one line of `SchedulingService` then changes.

**Exercise 6 (cross-layer).** A receptionist clicks **Check in** on `AttendanceController`. List the
files that participate, in call order, and name the one place the rule-5 eligibility test may live.

<details>
<summary>Answers</summary>

**1.** `TrainerService`, `MembershipService`, `SchedulingService` — and, once the Open detail in its
entry is resolved, `WorkoutService` (which needs `createdDate` and has no date parameter).
`PaymentService.voidPayment` (`voidedDate`), `PaymentService.record` (the ISO date on the entity),
`MedicalProfileService.save`/`update` (`updatedDate`) and `AttendanceService`'s eligibility test all
need a day, and none of their signatures carries one — which is why "where does today come from?" is a
`UML.md` §6 change (add a `today()` member and declare the edge), not a local `LocalDate.now()` call.
`AttendanceService` is the exception that proves the point: `checkIn(customerId, date, time)` *does*
receive the date, so rule 5 is testable without a clock.

**2.**

```java
public int bookedCount(int availabilityId) {
    return sessionDao.countByAvailability(availabilityId, SessionStatus.CONFIRMED);
}
public boolean isSlotFull(int availabilityId) {
    TrainerAvailability slot = availabilityDao.findById(availabilityId);
    return bookedCount(availabilityId) >= slot.getMaxCustomers();
}
```

`CONFIRMED` only, because a `REQUESTED` session does not hold a seat yet and a
`COMPLETED`/`NO_SHOW`/`CANCELLED` one has released it (rule 17). `>=` rather than `==`, because a cap
can be lowered below the current occupancy (`TrainerService.setSlotCap`), and a slot with more bookings
than its cap must still read as full rather than as "one place left".

**3.** Seven problems: (a) it never checks that the slot exists (`slot` may be `null` → NPE, and a null
check must come before `getTrainerId()`); (b) it does not require the slot to be `OPEN`; (c) it does not
verify the slot's own `date`/`startTime` against the arguments; (d) it skips the capacity check —
`bookedCount(...) == maxCustomers` is the *wrong* test *and* it happens after the decision to book;
(e) it writes `AvailabilityStatus.FULL`, which is derived state (rule 13); (f) it never runs
`detectConflicts`, so one customer or one trainer can be double-booked; (g) `new BigDecimal(60.00)` uses
the `double` constructor (binary-float error) instead of `new BigDecimal("60.00")`, it ignores rule 11
(a covered session must be zero, and `subscriptionId` is never set), and it forgets
`durationMin = 60` (rule 1). It also writes two rows (session + slot), which is the transaction
question in §6 — and would not be needed at all once (e) is fixed.

**4.** (a) Rule 12 is a rule about an entity, and rules are unit-tested: a public `boolean` lets the
test state the nine link cases as a table with no exception handling. (b) It is the guard the *screen*
can call to warn before `record` runs, without duplicating the XOR logic in the controller. It returns a
`boolean` rather than throwing because "this link is invalid" is a *value* the caller decides what to do
with, and `record` — the single-object action with one reason to fail — is the method that turns it into
a message (§4's error-handling split).

**5.** First `docs/UML.md`, rule 18: the sentence "only `CONFIRMED` sessions may become `CANCELLED`"
has to gain `REQUESTED` (e.g. "only `CONFIRMED` or `REQUESTED` sessions may become `CANCELLED`"), and
`DECISIONS.md` records why. Only then does the code change: in `canTransition`, the `REQUESTED` arm
becomes `to == CONFIRMED || to == CANCELLED`. `cancelSession` itself needs **no** change — it already
asks `canTransition`, which is the whole point of having a single authority (rule 18). Nothing else in
the project assumes the old table, so no other file moves.

**6.** `attendance.fxml` (the button) → `AttendanceController` (reads the customer/date/time, calls one
method, renders the result) → `AttendanceService.checkIn` → `CustomerDao.findById`,
`MembershipDao.findByCustomer`, `AttendanceDao.findByCustomerAndDay`/`save` → `*DaoImpl` →
`DbConnection` → SQLite. The rule-5 eligibility test may live **only** in
`AttendanceService.checkIn`; the controller may report its failure message, and the DAO may not know the
rule exists.

</details>

---

## 9. Ownership and tasks

### Contents (to be written)

| Class | Responsibility | Owner |
|-------|----------------|-------|
| `CustomerService.java` | Customer validation, search/filter, status rules, list by assigned trainer. **No hard delete** — rule 4 keeps that on `AdminService`. `changeStatus(SUSPENDED)` also freezes the customer's ACTIVE memberships (rule 14). | Abdelrhman |
| `TrainerService.java` | Slot publishing (validates `startTime < endTime`) and per-slot caps, close slot, set rate, assign/reassign customers. `setSlotCap` overrides one slot; `setDailyCap` re-applies across a day and so wins regardless of call order. | Ziad |
| `MembershipService.java` | Sell/renew/freeze/unfreeze/cancel, overdue expiry sweep, expiry reminders, and `freezeForSuspended` as the rule-14 hook. Freeze stores `frozenDays`; unfreeze shifts `endDate` forward by it. Only ACTIVE may freeze; renewing EXPIRED extends, renewing CANCELLED refuses. | Ziad |
| `WorkoutService.java` | Program building rules, reassignment to a customer, versioning per customer. There is no in-place edit of an issued program — `newVersion` supersedes it; `update`/`delete` are for correcting mistakes before issue. | Yousef |
| `SchedulingService.java` | `bookOpenSlot` (needs OPEN slot + free capacity + no trainer or customer conflict), `requestOutsideSlots`, `adminReserve` (ADMIN only), `detectConflicts`, the session state machine (`canTransition` — rule 18), confirm/cancel-with-reason/complete/no-show, subscription lifecycle and pricing. `bookedCount` counts CONFIRMED only (rule 17). Membership (gym access) and subscription (trainer engagement) are distinct concepts. | Yousef |
| `EquipmentService.java` | Add/edit inventory, status and condition transitions, last-maintenance date, delete | Yousef |
| `AttendanceService.java` | Check-in/out rules, eligibility checks (ACTIVE customer + ACTIVE non-expired membership), daily report data, and `correctRecord` for fixing a mis-keyed time. Attendance rows are never deleted (rule 19). | Abdel Raouf |
| `PaymentService.java` | Recording, XOR link validation (`validLink`, public so it is unit-testable), voiding, balances, receipt lookup, revenue by range. Voided payments never count, and payments are never deleted (rule 19) — money is corrected by voiding or by `AdminService.adjustPayment`. | Abdel Raouf |
| `ProgressService.java` | Metric access and chart series per customer, keyed by `ProgressMetric` | Abdel Raouf |
| `MedicalProfileService.java` | Medical CRUD, lookup by customer, assigned-trainer access check (`getForTrainer`) — the only sanctioned read path for a trainer | Abdelrhman |
| `AdminService.java` | Authenticate (rejects `active == false`, returns null), password reset, staff CRUD by role, reassign/adjust overrides, admin-only hard deletes that ask `GuardService.countReferences` first and refuse while references exist | Abdelrhman |

### Tasks

| Task | Owner | Status |
|------|-------|--------|
| Skeleton + this README created | Abdelrhman | Done |
| This README expanded into teaching material (11 entries, rules, sketches, open-details register) | Abdelrhman | Done |
| Service per owned feature (see table) | Each owner | To Do |
| Error handling per owned service: validate inputs, friendly messages for every failure | Each owner | To Do |
| Unit tests per service (no JavaFX, no database) | Each owner | To Do |
| Resolve the open-details register (§7) in `DECISIONS.md`; change `UML.md` first where a signature or field is involved | Each owner, per row | To Do |

Like `dao`, keep this layer UI-free so the `swing` branch reuses the design. That branch has no
Java files yet.

### Definition of done for a file in this folder

- [ ] The class exists at `src/main/java/com/gym/service/<Name>Service.java`, package `com.gym.service`.
- [ ] Every method signature matches `docs/UML.md` §4 exactly (name, parameters, return type), and no
      method was added that §4 does not list.
- [ ] Collaborators are declared in that service's `..>` list in §4 and are `private final`, injected
      through a constructor. No other DAO or service is touched.
- [ ] Each business rule the file enforces is cited by number in a comment next to the check.
- [ ] Failures throw `IllegalArgumentException`/`IllegalStateException` with a message safe to display;
      `CustomerService.validate` returns `List<String>` and nothing else does.
- [ ] No JavaFX import, no formatting, no SQL, no `Connection`, no `SQLException` in a signature.
- [ ] Unit tests exist that use hand-written fake DAOs (no database, no JavaFX) and cover the rule
      numbers named in the file, including the failure paths.
- [ ] Every gap found while writing it is recorded as an **Open detail** in this README and in
      `DECISIONS.md` — never resolved silently in the code.
- [ ] The `swing` branch can take the file as-is: no JavaFX, no `FxUtils`, nothing toolkit-specific.

