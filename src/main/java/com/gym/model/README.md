# `model` — Entity Classes (JavaFX branch)

> **Status: SHIPPED (Milestone 2).** All **13 domain entities and 11 enums** from
> [`docs/UML.md`](../../../../docs/UML.md) §1 and §1b are implemented in `com.gym.model` using
> COMP 301 Lectures 1–6 syntax. Every entity implements `java.io.Serializable` with
> `private static final long serialVersionUID = 1L;`. Field names, constructors, factory methods, and
> enum literals strictly match `docs/UML.md`. Tracked in [`docs/COVERAGE.md`](../../../../docs/COVERAGE.md).

## 1. Purpose — the problem this layer solves

`model` is the shared vocabulary of the entire application. Each of the 13 entity classes represents
the Java shape of one table row in SQLite `gym.db`, and the 11 enumerations define the closed sets of
allowed values (`CustomerStatus`, `MembershipStatus`, `PlanType`, etc.). Because every class
implements `Serializable`, entities can also be saved to and loaded from binary `.ser` files via
`SerializationUtil`.

**What breaks if this layer is missing.** Without shared model classes, DAOs and screens would pass
raw strings or `Object[]` arrays around: renaming a database column or mistyping a status like
`"ACTVE"` would silently fail at runtime with zero compiler checks, and using `double` for prices
would introduce floating-point rounding drift (`docs/DECISIONS.md` 31).

**What this layer deliberately does not do.** No SQL, no `Connection`, no JavaFX imports, and no
validation exceptions. DAOs validate input via `ValidationException` before writing and must still be
able to construct entities via the no-arg constructor when reading any stored database row. Entities
do not extend any parent base class (`docs/UML.md` §7, `docs/DECISIONS.md` 36) and reference related
entities **by ID only** (`customerId`, `trainerId`, etc. — `docs/DECISIONS.md` 32).

## 2. What you learn here (OOP concepts)

Concepts follow COMP 301 Lectures 1–6 and [`docs/LEARNING_GUIDE.md`](../../../../docs/LEARNING_GUIDE.md):

| Concept | Where in this folder | Why the project needs it | How to apply it (one line) |
|---|---|---|---|
| Encapsulation (Lectures 2–3) | Private fields + public `get`/`set` accessors on all 13 entities | Controls how data is read and modified cleanly without exposing internal storage | Keep fields `private`; expose `public` getters/setters matching `UML.md` §1 |
| Constructors & Overloading | No-arg constructor + parameterized constructor on all 13 entities | `mapRow(ResultSet)` uses the no-arg constructor; tests/callers use the full constructor | Always keep both constructors |
| Serialization (`Lecture 6`) | `implements Serializable` + `serialVersionUID = 1L` | Allows entities to be saved/loaded with `ObjectOutputStream` / `ObjectInputStream` | Keep `serialVersionUID = 1L` across versions |
| Boxed vs primitive types | `Integer trainerId`, `Double targetWeightKg` vs `int id`, `boolean voided` | Distinguished nullable database columns (`NULL`) from `NOT NULL` columns | Use boxed wrappers only where `UML.md` allows `null` |
| Exact decimal money (`BigDecimal`) | `Membership.price`, `Trainer.hourlyRate`, `Payment.amount`, `TrainerSubscription.ratePerPeriod` | Avoids binary floating-point rounding error (`0.1 + 0.2 != 0.3`) | Always construct from `String` (`new BigDecimal("45.00")`) |
| `equals` / `hashCode` / `toString` | Overridden on every entity (`Objects.hash(...)`, readable summaries) | Lets lists/collections compare entities by identity and print useful debug logs | Keep sensitive fields (e.g., `passwordHash`) out of `toString()` |
| Enums as fixed vocabularies | All 11 enums (`StaffRole`, `PaymentMethod`, `PlanType`, …) | Compile-time safety prevents typos like `"MNTNANCE"` | Store `.name()` in SQLite and parse back with `EnumType.valueOf(...)` |
| Static factory methods | `Payment.forMembership`, `forSubscription`, `forSession` | Makes mutually exclusive payment links clear at call sites | Call the named factory instead of manually juggling 3 `null`/non-`null` IDs |

## 3. Design patterns in this layer

| Pattern | Where | Why this pattern | What goes wrong if used carelessly |
|---|---|---|---|
| Entity / POJO | All 13 domain classes | Plain carriers shared between `dao/`, `SerializationUtil`, and (Milestone 3) `controller/` | Adding JDBC or JavaFX imports breaks `Swing` portability |
| Static Factory Method | `Payment.forMembership(...)`, `forSubscription(...)`, `forSession(...)` | Prevents accidentally swapping adjacent `Integer` link parameters (`rule 12`) | Putting business rules inside factories duplicates validation |
| Nullable Foreign Key (XOR) | `Payment.membershipId` / `subscriptionId` / `sessionId` | `null` cleanly means "not linked"; primitive `0` collides with IDs | Treating `0` as "unset" causes invalid joins |
| Inheritance — deliberately absent | No `BaseEntity` parent (`docs/UML.md` §7, `DECISIONS.md` 36) | Keeps every class self-contained with its exact fields visible in one file | A base `id` field hides column ownership and adds unnecessary hierarchy |

## 4. Rules you must follow (and the bug each rule prevents)

1. **Fields are `private`; getters/setters are `public`.** Prevents external callers from mutating
   data unpredictably without passing through accessors.
2. **Keep both no-arg and full constructors** matching `docs/UML.md` §1 — DAOs construct blank
   objects inside `mapRow(ResultSet)` using setters.
3. **Keep `implements Serializable` and `serialVersionUID = 1L`** — prevents `InvalidClassException`
   when reloading saved `.ser` snapshots via `SerializationUtil`.
4. **Money uses `BigDecimal` (never `double`)** constructed from `String` (`DECISIONS.md` 31) —
   prevents penny drift in membership fees, session rates, and payment totals.
5. **Dates and times are ISO-8601 `String`s** (`yyyy-MM-dd`, `HH:mm`) — keeps SQLite `TEXT` storage
   simple and lexicographically sortable without extra parser classes.
6. **Boxed wrappers (`Integer`, `Double`) mark nullable fields; primitives mark non-null fields** —
   prevents sentinel `0` values masquerading as valid foreign keys or benchmarks.
7. **Reference other entities by ID only** (`customerId`, `trainerId`) — avoids loading whole object
   graphs inside simple POJOs (`DECISIONS.md` 32).
8. **Zero JavaFX imports, zero SQL imports** — guarantees backend portability (`swing` branch and CLI tools).

## 5. The files in this folder

Write / dependency order: **enums first** (zero dependencies), then **core actors** (`Customer`, `Trainer`,
`Administrator`, `Equipment`), then **dependent entities** (`MedicalProfile`, `Membership`, `WorkoutProgram`,
`TrainerAvailability`, `TrainerSubscription`, `Session`, `Attendance`, `ProgressRecord`, `Payment`).

### Enumerations (11 files)

| File | Literals (`docs/UML.md` §1b) | Purpose |
|---|---|---|
| `CustomerStatus.java` | `ACTIVE`, `INACTIVE`, `SUSPENDED` | Tracks member standing |
| `MembershipStatus.java` | `ACTIVE`, `FROZEN`, `EXPIRED`, `CANCELLED` | Membership lifecycle state |
| `PlanType.java` | `MONTHLY`, `QUARTERLY`, `YEARLY` | Membership duration tier (pure enum in code, matching COMP 301 beginner scope) |
| `SessionStatus.java` | `REQUESTED`, `CONFIRMED`, `COMPLETED`, `NO_SHOW`, `CANCELLED` | Training session state machine |
| `SubscriptionStatus.java` | `ACTIVE`, `EXPIRED`, `CANCELLED` | Personal trainer subscription standing |
| `AvailabilityStatus.java` | `OPEN`, `FULL`, `CLOSED` | Availability slot capacity state |
| `StaffRole.java` | `ADMIN`, `MANAGER`, `STAFF` | Operator permission role |
| `PaymentMethod.java` | `CASH`, `CARD`, `TRANSFER` | Accepted payment tenders |
| `EquipmentStatus.java` | `AVAILABLE`, `IN_MAINTENANCE`, `RETIRED` | Floor availability of gym equipment |
| `EquipmentCondition.java` | `NEW`, `GOOD`, `FAIR`, `POOR` | Physical wear assessment |
| `ProgressMetric.java` | `WEIGHT_KG`, `BODY_FAT_PCT` | Chart metric selector (`docs/UML.md` §1b) |

### Domain Entities (13 `Serializable` POJOs)

| File | Key Fields / Link Highlight | Authoritative Spec |
|---|---|---|
| `Customer.java` | Member profile; nullable `Integer trainerId`, `CustomerStatus status` | `docs/UML.md` §1 |
| `Trainer.java` | Coach profile; `BigDecimal hourlyRate`, `experienceYears` | `docs/UML.md` §1 |
| `Administrator.java` | Login account; `StaffRole role`, `boolean active` (note: `passwordHash` omitted from `toString()`) | `docs/UML.md` §1 |
| `Equipment.java` | Machine inventory; `EquipmentCondition condition`, `EquipmentStatus status` | `docs/UML.md` §1 |
| `MedicalProfile.java` | Unique per `customerId` (`bloodType`, `conditions`, `allergies`, `medications`, `injuries`, doctor info) | `docs/UML.md` §1 |
| `Membership.java` | Linked to `customerId`; `PlanType plan`, `BigDecimal price`, `MembershipStatus status`, `int frozenDays` | `docs/UML.md` §1 |
| `WorkoutProgram.java` | Linked to `customerId` and `creatorTrainerId`; `int version` | `docs/UML.md` §1 |
| `TrainerAvailability.java` | Linked to `trainerId`; `int maxCustomers`, `AvailabilityStatus status` | `docs/UML.md` §1 |
| `TrainerSubscription.java` | Linked to `customerId` and `trainerId`; `BigDecimal ratePerPeriod`, `SubscriptionStatus status` | `docs/UML.md` §1 |
| `Session.java` | Linked to `customerId`, `trainerId`; nullable `Integer availabilityId`, `Integer subscriptionId`, `BigDecimal price` | `docs/UML.md` §1 |
| `Attendance.java` | Linked to `customerId`; `date`, `checkIn`, `checkOut` | `docs/UML.md` §1 |
| `ProgressRecord.java` | Linked to `customerId`; `double weightKg`, `double bodyFatPct`, nullable `Double targetWeightKg` | `docs/UML.md` §1 |
| `Payment.java` | Linked to `customerId` + nullable XOR FKs (`membershipId`, `subscriptionId`, `sessionId`), `BigDecimal amount`, `voidReason`, `voidedDate`, `previousAmount`, plus 3 factory methods | `docs/UML.md` §1 |

### Representative Sketches (canonical definitions live in `docs/UML.md` §1)

#### `Customer.java` — member profile entity (owner: Abdelrhman)

**Status:** done. **Canonical:** `docs/UML.md` §1 (`Customer`).

```java
// Sketch — field list is authoritative in docs/UML.md §1
public class Customer implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String fullName;
    private CustomerStatus status;
    private Integer trainerId; // nullable FK: null means no assigned trainer

    public Customer() {
    }

    public Customer(int id, String fullName, /* ... remaining UML fields ... */ Integer trainerId) {
        this.id = id;
        this.fullName = fullName;
        this.trainerId = trainerId;
    }

    // getters, setters, equals(id), hashCode(), toString()
}
```

#### `Payment.java` — financial receipt with XOR links (owner: Abdel Raouf)

**Status:** done. **Canonical:** `docs/UML.md` §1 (`Payment`), rule 12, rule 19.

```java
// Sketch — shows factory methods matching rule 12
public class Payment implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int customerId;
    private Integer membershipId;
    private Integer subscriptionId;
    private Integer sessionId;
    private BigDecimal amount;
    // voided, voidReason, voidedDate, previousAmount...

    public static Payment forMembership(int customerId, int membershipId, BigDecimal amount,
                                        PaymentMethod method, String date, String receiptNo) {
        return new Payment(0, customerId, membershipId, null, null, amount, method, date, receiptNo, false);
    }
}
```

**Verify:** Confirm `model/*.java` compiles under Java 21 (`./gradlew compileJava`), serializes and
reloads via `SerializationUtil`, and preserves every field and constructor in `docs/UML.md` §1.

## 6. How this layer connects to the others

| Layer / Utility | How it uses `com.gym.model` |
|---|---|
| `com.gym.dao` (shipped) | Reads/writes entity objects to SQLite rows in `gym.db`; enforces `ValidationException` rules before SQL execution |
| `com.gym.util.SerializationUtil` (shipped) | Saves and loads `Serializable` entities (or `List`s of entities) to binary `.ser` backup files |
| `com.gym.controller` (Milestone 3) | Currently uses UI demo models; Milestone 3 binds table/form views directly to these entities |
| `docs/UML.md` §1 & §1b | Single source of truth for every class name, field name, type, constructor, and enum literal |

## 7. Common mistakes and review checklist

- **Floating-point money bug:** Passing a Java `double` to `new BigDecimal(45.50)` stores binary error;
  always pass a `String` (`new BigDecimal("45.50")`).
- **Primitive `0` vs `null` FK:** Writing `int trainerId` makes unassigned customers look like they belong
  to trainer `0`; keep nullable FKs boxed (`Integer`).
- **Leaking passwords in `toString()`:** Printing `Administrator.toString()` should display `username`
  and `role`, never `passwordHash`.
- **Missing `serialVersionUID = 1L`:** Editing a class later causes `SerializationUtil.loadFromFile`
  to fail on older `.ser` files.

Checklist:

- [ ] All fields are `private` with `public` getters and setters.
- [ ] Every entity implements `Serializable` with `private static final long serialVersionUID = 1L;`.
- [ ] No SQL (`java.sql.*`) or JavaFX (`javafx.*`) imports anywhere in `com.gym.model`.
- [ ] Every class/enum matches `docs/UML.md` §1 and §1b verbatim.

## 8. Learning exercises (with answers)

1. Why do every entity's constructors include a no-arg constructor (`public Customer()`) even when a
   full constructor already exists?
2. Why does `Payment` store `customerId` (`int`) and `membershipId` (`Integer`) instead of storing a
   `Customer` reference and a `Membership` reference?
3. What happens during `SerializationUtil.loadFromFile(...)` if an entity in `com.gym.model` forgets
   `implements Serializable`?
4. Explain why `Administrator.toString()` omits `passwordHash`.
5. Look at `PlanType.java` vs `Payment.java`. Which one is an enum and which one holds factory methods?

<details>
<summary>Answers</summary>

1. DAO `mapRow(ResultSet)` methods construct an empty entity first (`new Customer()`) and populate fields
   from SQLite columns using setter methods.
2. Storing IDs keeps entities lightweight, JDBC-free, and straightforward to serialize cleanly without
   pulling cyclic or whole-database object graphs into memory (`DECISIONS.md` 32).
3. `ObjectOutputStream.writeObject(...)` throws `NotSerializableException` (wrapped into
   `DatabaseException` by `SerializationUtil.saveToFile`).
4. `toString()` is often printed in logs or debug output; omitting `passwordHash` prevents leaking
   stored credentials.
5. `PlanType` is an enum (`MONTHLY`, `QUARTERLY`, `YEARLY`); `Payment` is a domain entity providing
   `forMembership`, `forSubscription`, and `forSession` static factories.

</details>

## 9. Ownership and tasks

| File Group | Responsibility | Owner(s) | Status |
|---|---|---|---|
| `Customer`, `CustomerStatus`, `MedicalProfile`, `Administrator`, `StaffRole` | Core members, medical & admin models | Abdelrhman | Done (Milestone 2) |
| `Trainer`, `TrainerAvailability`, `AvailabilityStatus`, `Membership`, `MembershipStatus`, `PlanType` | Trainers, schedules & plans | Ziad | Done (Milestone 2) |
| `WorkoutProgram`, `Session`, `SessionStatus`, `TrainerSubscription`, `SubscriptionStatus`, `Equipment`, `EquipmentStatus`, `EquipmentCondition` | Workouts, sessions & equipment | Yousef | Done (Milestone 2) |
| `Attendance`, `Payment`, `PaymentMethod`, `ProgressRecord`, `ProgressMetric` | Attendance, billing & progress | Abdel Raouf | Done (Milestone 2) |

`swing` branch rule: every `.java` file in `com.gym.model` uses zero JavaFX imports, so the exact same
domain files copy directly to the `swing` branch unchanged.

### Definition of done for a file in this folder

- [ ] Matches `docs/UML.md` §1 / §1b exact names, types, and constructors.
- [ ] Implements `Serializable` (`serialVersionUID = 1L`) for all 13 entities.
- [ ] Compiles cleanly under `./gradlew compileJava` with `-Xlint:all`.
- [ ] Zero JavaFX or `java.sql` imports.
