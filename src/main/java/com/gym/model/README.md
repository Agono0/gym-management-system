# `model` — Entity Classes (JavaFX branch)

> ## ⚠️ PLANNED, NOT BUILT
> **This folder contains no `.java` files — only this README.** The 13 entities and 11 enums below
> are the agreed design in [`docs/UML.md`](../../../../docs/UML.md) §1 and §1b, written here so the
> layer can be built straight from the documentation. The shipped app runs on demo data held in the
> controllers; nothing in this package exists yet. Status lives in
> [`docs/COVERAGE.md`](../../../../docs/COVERAGE.md). Do not cite a class in this folder as if it
> compiles.

## 1. Purpose — the problem this layer solves

`model` is the vocabulary of the application. Each of the 13 entity classes will be the Java shape of
one row in `gym.db`, and the 11 enumerations are the closed sets of values those rows are allowed to
hold. Nothing else in the project may invent a second copy of a member, a payment or a session: the
DAO fills these objects from SQLite, the service validates and mutates them, the controller renders
them and the FXML table columns read them. One type, one definition, five users.

**What breaks if this layer is missing** — currently visible in the shipped demo: the controllers
pass loose UI values and hard-coded demo rows around. Once the backend lands, `CustomerDao.findById`
returning a `Map<String, Object>` instead of a `Customer` would mean a column rename breaks the DAO,
the service and the screen at once, with no compiler help; money summed as `double` stops
reconciling (`DECISIONS.md` 31); a misspelled status is a valid `String` no `switch` recognises.

**What this layer deliberately does not do.** No SQL or JDBC types, no JavaFX imports, no
computation. It does not validate — the (planned) services and `Validators` own that, because two
callers would otherwise validate differently and the DAO must still be able to *load* a row that a
strict setter would reject. It does not inherit (`UML.md` §7, `DECISIONS.md` 36) and it holds object
references to other entities **by id only** (`customerId`, `trainerId`, …) — no object graph
(`DECISIONS.md` 32).

**The entity contract** (applies to all 13 classes when written): a plain POJO — **not a record** —
with a no-arg constructor (what the DAO needs to build the object row by row) plus a full
constructor, and `toString`/`equals`/`hashCode` as part of the contract, not extras. `Payment` needs
`equals` most: rule 12 (the XOR link) is defined over it and tests compare payments.

**Conventions** (`docs/UML.md` header): money is `BigDecimal`, never `double`; boxed
`Integer`/`Double`/`Boolean` marks a nullable column and primitives mark NOT NULL; dates and times
are ISO-8601 `String`s (`yyyy-MM-dd`, `HH:mm`) owned by the planned `DateUtils`.

**The 11 enums** (`UML.md` §1b has the full literal lists): `CustomerStatus`, `MembershipStatus`,
`PlanType`, `SessionStatus` (includes `NO_SHOW`), `SubscriptionStatus`, `AvailabilityStatus`,
`StaffRole`, `PaymentMethod`, `EquipmentStatus`, `EquipmentCondition`, `ProgressMetric`.
`AvailabilityStatus.FULL` and both `EXPIRED` literals are **derived** — recomputed on read, never
set by hand (rule 13). `PlanType` is the only enum with state and behaviour (price + duration).

## 2. What you learn here (OOP concepts)

| Concept | Where it will be practised | Why the project needs it | How to apply it (one line) |
|---|---|---|---|
| Encapsulation / information hiding | the ~113 `«get/set»` fields of `UML.md` §1 | The public API of a class is a promise; the field is an implementation detail | Private field + public getter/setter; never a public field; never a getter added "just for logging" |
| Constructor + overloading | `Customer()` **and** `Customer(int, String, …)` on all 13 entities | The no-arg constructor is required by DAO mapping; the full one is convenient for tests | Write both, in that order |
| Boxed vs primitive (nullability signalling) | `Customer.trainerId` is `Integer`; `Session.durationMin` is `int`; `ProgressRecord.targetWeightKg` is `Double` | The type *is* the schema: boxed = column may be `NULL`, primitive = NOT NULL | Choose the type from the column |
| Value objects | `PlanType` (price + duration), `BigDecimal` money | A plan's price belongs to the plan; a shared immutable value cannot drift | Enums/`BigDecimal` for values; the 13 entities are mutable and carry identity — not value objects |
| `equals`/`hashCode` contract | every entity, `Payment` most of all | Two payments for the same receipt must compare equal or rule 12's tests are a coin flip | Write the pair together, keep them consistent |
| `toString` for debugging | every entity | A failing test must print *which* object, without opening the DB | Identity + the few fields a human needs — never a password hash or a medical list |
| Enums as types instead of magic strings | all 11 enums replace `String status` / `int role` | `"ADMNI"` compiles; `StaffRole.ADMNI` does not, and the IDE lists legal values | Type the field with the enum; never compare `name()` to a literal |
| Derived vs stored state | rule 13: `FULL`, both `EXPIRED` | A stored derived value goes stale the moment the row changes | Compute on read; one documented producer |
| Composition, FK-by-id | `Session.customerId`, `Payment.sessionId` | An object reference would force lazy loading and drag JDBC in (`DECISIONS.md` 32) | Store the id; ask the service for both rows |
| Inheritance — absent on purpose | no `BaseEntity`/`AbstractEntity` (`UML.md` §7, `DECISIONS.md` 36) | A base class holding `id` would put a field where §1 shows none and hide column ownership | Reject the base class; argue in `DECISIONS.md` first |
| Exhaustive `switch` over enums | consumers a layer up (`canTransition`, `series`) | A forgotten case becomes a compile error instead of a silent `null` | Use `switch` expressions |
| `Optional` — deliberately not used | entity getters | The design returns `null` for "not found" everywhere; mixing styles makes every caller guess | Return the boxed type or `null` |

## 3. Design patterns in this layer

| Pattern | Where | Why | What goes wrong if used carelessly |
|---|---|---|---|
| Value Object | `PlanType`, the pure enums, `BigDecimal` amounts | The value is the identity; immutable values cannot drift | Settable static state on an enum turns a shared value into global state |
| Entity / DTO (plain POJO) | all 13 classes | One dumb carrier the DAO and service both trust; behaviour here would move rules into a layer that must load any row | A validating setter stops the DAO loading legacy rows |
| Factory Method | `Payment.forMembership(...)` / `forSubscription(...)` / `forSession(...)` | Three adjacent `Integer` links swap silently; a named factory states intent | A factory that also validates hides the XOR bug rule 12 exists to catch |
| State machine over an enum | `SessionStatus`; transitions enforced by `SchedulingService.canTransition` (rule 18) | Legal states are a closed set; *transitions* are a business rule | A controller moving `NO_SHOW` → `CONFIRMED` via a setter — the enum will not stop it |
| Nullable FK with an XOR link | `Payment.membershipId`/`subscriptionId`/`sessionId` (boxed) | `null` says "no link"; an `int` needs a colliding sentinel | Treating `0` as "no link" |
| Template Method / inheritance, Singleton, Builder | **all rejected** (`UML.md` §7, `DECISIONS.md` 36) | The design is 13 flat POJOs with a fixed constructor list | Each alternative hides the field list that must match `UML.md` §1 |

## 4. Rules you must follow (and the bug each rule prevents)

1. **Fields are `private`; accessors are `public`.** A public field makes the memory layout the API.
2. **Plain classes, never records** — the DAO needs the no-arg constructor and setters.
3. **Money is `BigDecimal` from the `String` constructor** (`new BigDecimal("45.00")`), compared with
   `compareTo` — `double` drifts and `equals` compares scale too (`DECISIONS.md` 31).
4. **Dates/times are ISO-8601 `String`s; no `java.time` type appears here** — one parser
   (`DateUtils`) means `findByDay` and slot comparisons agree (rule 9).
5. **Boxed = nullable, primitive = NOT NULL** — an `int` FK forces a sentinel `0` that collides with
   real ids.
6. **`equals`/`hashCode` written together** and consistent.
7. **Reference other entities by id only** — no object graph, no collections in entities.
8. **No JavaFX import, no static mutable state** in this package — ever.

Full field-by-field authority: `docs/UML.md` §1 (fields) and §1b (descriptions). When you write the
first entity, keep `UML.md` and this folder in sync in the same commit.

## 5. How this layer connects to the others

| Will be used by | How |
|---|---|
| `dao/` (planned) | `mapRow(ResultSet)` builds entities; DAOs take and return them |
| `service/` (planned) | validates and mutates entities; owns every rule 1–19 |
| `controller/` (shipped) | renders entity fields; today holds demo data that will be replaced by service-returned entities |
| `docs/UML.md` | the single source of truth for every field name, type and nullability mark |

Write order when the backend starts: the 11 enums first (`UML.md` "Enumerations" is the complete
literal list), then the entities in dependency order (`Customer` → the rest), one per file, each
against `UML.md` §1. [`../dao/README.md`](../dao/README.md) and
[`../service/README.md`](../service/README.md) describe how these objects will be consumed.

## 6. Learning exercises

1. Explain, in one sentence each, why a `record` is rejected and why `equals`/`hashCode` are contract,
   not extras. Which rule depends on `Payment.equals`?
2. A teammate proposes `abstract class BaseEntity { protected int id; }`. Give three concrete reasons
   from `UML.md` §7 / `DECISIONS.md` 36 to refuse.
3. `MembershipStatus.EXPIRED` is derived. What breaks if a controller writes it after a renewal — and
   who is the documented producer instead?
4. Why is `Customer.trainerId` an `Integer` while `Session.durationMin` is an `int`? What does the
   boxed type cost and buy?
5. Sketch the two constructors of `Payment` and name the three factory methods rule 12 needs.

## 7. Ownership

| Planned file group | Owner |
|---|---|
| 13 entities + 11 enums (per `docs/UML_TEAM.md` split) | per-feature owners (see `docs/UML_TEAM.md`) |

Status: **planned, not built** — track in `docs/COVERAGE.md` → "Backend domain services and SQLite DAOs".
