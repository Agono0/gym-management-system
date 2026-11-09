# `dao` — Data Access Layer (JavaFX branch)

> **Status: SHIPPED (Milestone 2 + Decision 78 Validation Upgrade).** All **13 concrete Data Access Objects** in `com.gym.dao` are
> implemented against SQLite (`gym.db`) using standard JDBC `PreparedStatement`, `ResultSet`, and
> `try-with-resources` (`DatabaseConnection`). Per `docs/DECISIONS.md` 77, separate `XxxDao`
> interfaces and `*DaoImpl` subclasses were consolidated into 13 direct concrete classes using
> COMP 301 Lectures 1–6 Java syntax; per `docs/DECISIONS.md` 78, each DAO's `validate(...)` helper
> enforces strict formats, foreign-key existence, uniqueness, and core `UML.md` business rules. Tracked in [`docs/COVERAGE.md`](../../../../docs/COVERAGE.md).

## 1. Purpose — the problem this layer solves

`com.gym.dao` is the **only** place in the application where table queries (`INSERT`, `UPDATE`,
`DELETE`, `SELECT`) are written. Each of the 13 domain entities in `com.gym.model` has one matching
DAO class (`CustomerDao`, `TrainerDao`, `PaymentDao`, etc.) responsible for:

1. Validating required fields before database writes (`validate(...)` throwing `ValidationException`).
2. Executing parameterized SQL via `DatabaseConnection.getConnection()`.
3. Mapping `ResultSet` rows back into entity objects inside a private `mapRow(ResultSet)` helper.
4. Converting checked `SQLException` errors into `DatabaseException`.

**What breaks if this layer is missing.** Putting SQL queries inside JavaFX screen controllers copies
column names across 19 UI files, prevents code reuse on the `swing` branch, invites SQL injection if
strings are concatenated, and leaves open `Connection`/`PreparedStatement` handles that lock `gym.db`
(`SQLITE_BUSY`).

**What this layer deliberately does not do.** No JavaFX imports (keeps the layer portable to `swing`
and CLI tools), no dialog popups, no user-facing alerts, and no separate service wrappers
(`docs/DECISIONS.md` 77). Today's UI screens still show demo data; **Milestone 3** connects the 20 UI
controllers to these DAOs.

## 2. What you learn here (OOP concepts)

| Concept | Where in this folder | Why the project needs it | How to apply it (one line) |
|---|---|---|---|
| Encapsulation | `private mapRow(ResultSet)` and `private validate(Entity)` inside every DAO | Hides SQL columns, `ResultSet` cursors, and null conversions from callers | Expose domain methods (`insert`, `findById`), keep JDBC internal |
| Single responsibility | One DAO class per SQLite table (13 classes total) | Editing a `payments` column touches `PaymentDao.java` and nowhere else | Keep each DAO focused on its owning entity table |
| Resource management (try-with-resources) | Every `Connection`, `PreparedStatement`, `ResultSet` | Closing JDBC handles prevents SQLite file lock (`SQLITE_BUSY`) errors | Always wrap `Connection` and `PreparedStatement` in `try (...)` |
| Checked exceptions (`Lecture 6`) | `throws DatabaseException, ValidationException` | Forces callers (Milestone 3 UI) to handle bad input and DB failures explicitly | Throw `ValidationException` for invalid fields; wrap `SQLException` in `DatabaseException` |
| Enum persistence | Storing `status.name()` and reading with `EnumType.valueOf(...)` | Database rows stay human-readable and safe even if enum declaration order changes | Always store enum names (`TEXT`), never ordinal numbers |
| Boxed nullable FK handling | `ps.setNull(idx, Types.INTEGER)` and `rs.wasNull()` | Distinguishes SQL `NULL` from integer `0` (e.g., `Customer.trainerId`, `Payment.membershipId`) | Check `rs.wasNull()` after `rs.getInt(...)` for nullable `Integer` fields |

## 3. Design patterns in this layer

| Pattern | Where | Why this pattern | What goes wrong if used carelessly |
|---|---|---|---|
| **Concrete Data Access Object (DAO)** | All 13 `*Dao` classes | Centralizes SQL, parameter binding, and row mapping per table without over-engineering interfaces (`DECISIONS.md` 77) | Writing SQL directly in UI controllers breaks separation of concerns |
| **Row Mapper** | `private Xxx mapRow(ResultSet rs)` on every DAO | One edit updates every `find*` method in that file | Duplicating column reads across 4 methods causes column typos |
| **Fail-fast Input Guard** | `private void validate(Entity)` before `insert`/`update` | Rejects missing/negative required fields with `ValidationException` before opening SQLite | Letting invalid `null` fields hit `NOT NULL` SQL errors produces cryptic constraint failures |
| **Abstract generic DAO (`AbstractDao<T>`) — rejected** | Kept 13 explicit concrete classes (`DECISIONS.md` 36, 77) | Beginner readability: every student can read their assigned DAO top-to-bottom in plain Java | Generic wrappers introduce casts/lambdas beyond COMP 301 Lectures 1–6 |

## 4. Rules you must follow (and the bug each rule prevents)

1. **SQL lives here and inside `DatabaseConnection` only.** Keeps queries maintainable in one layer.
2. **Always use `PreparedStatement` with `?` placeholders** — prevents SQL injection and quoting bugs
   in text fields (names, emails, reasons, notes).
3. **Always close `Connection`, `PreparedStatement`, and `ResultSet` with `try-with-resources`** —
   prevents locked SQLite files across successive clicks.
4. **Return empty `List` (`new ArrayList<>()`) on multi-row queries (`findAll`, `findByCustomerId`),
   never `null`** — avoids `NullPointerException` when looping over empty query results. Single-row
   queries (`findById`, `findByUsername`, `findByReceiptNo`) return `null` when no row matches.
5. **Check `rs.wasNull()` for nullable columns** (`Integer` / `Double`) and bind SQL `NULL` via
   `ps.setNull(..., Types.INTEGER)` or `Types.DECIMAL` / `Types.REAL`.
6. **Check enum parsing defensively** (store `.name()`, handle `valueOf` cleanly) — prevents database
   drift from crashing `findAll()`.
7. **Zero JavaFX imports** — preserves `swing` branch compatibility (`docs/UML.md` §2).

## 5. The files in this folder

All 13 concrete DAOs follow the same consistent COMP 301 structure: `insert`, `update`, `delete`,
`findById`, `findAll`, domain-specific lookup queries where defined in `docs/UML.md` §3, `mapRow`,
and `validate`:

| DAO File | Table (`DatabaseConnection`) | Public Methods Beyond `insert`/`update`/`delete`/`findById`/`findAll` | Key `validate(...)` Checks |
|---|---|---|---|
| `CustomerDao.java` | `customers` | — | Non-null, non-blank `fullName`, `@`/`.` email, `>= 7` digits on phone/emergencyPhone, ISO `yyyy-MM-dd` DOB/joinDate, nullable `trainerId` FK check |
| `TrainerDao.java` | `trainers` | — | Non-null, non-blank `fullName`, `@`/`.` email, `>= 7` digits phone, `experienceYears >= 0`, ISO `hireDate`, non-null `hourlyRate >= 0` |
| `AdministratorDao.java` | `administrators` | `findByUsername(String)` | Non-null, `fullName`, `username` (`>= 3` chars, no spaces, unique), non-blank `passwordHash`, non-null `role`, email, ISO `createdDate` |
| `EquipmentDao.java` | `equipment` | — | Non-null, non-blank `name`, ISO `purchaseDate`/`lastMaintenance` with `lastMaintenance >= purchaseDate` |
| `MedicalProfileDao.java` | `medical_profiles` | `findByCustomerId(int)` | Non-null, existing `customerId`, ISO `updatedDate`, max one profile per customer (rule 3) |
| `MembershipDao.java` | `memberships` | `findByCustomerId(int)` | Non-null, existing `customerId`, non-null `plan`/`price >= 0`, `frozenDays >= 0`, ISO `startDate <= endDate`, refuses reactivating/renewing `CANCELLED` (rule 7) |
| `WorkoutProgramDao.java` | `workout_programs` | `findByCustomerId(int)` | Non-null, existing `customerId`/`creatorTrainerId`, `name`, `version >= 1`, unique `(customerId, version)`, ISO `createdDate` |
| `TrainerAvailabilityDao.java` | `trainer_availability` | `findByTrainerId(int)` | Non-null, existing `trainerId`, ISO `date`, `HH:mm` `startTime < endTime` (rule 8), `maxCustomers > 0` (rule 8) |
| `TrainerSubscriptionDao.java` | `trainer_subscriptions` | `findByCustomerId(int)` | Non-null, existing `customerId`/`trainerId`, `subscription.trainerId == customer.trainerId` (rule 11), ISO `startDate <= endDate`, `ratePerPeriod >= 0` |
| `SessionDao.java` | `sessions` | `findByTrainerId(int)` | Non-null, existing `customerId`/`trainerId`, ISO `date` & `HH:mm` `startTime` (rule 9), `durationMin > 0`, `price >= 0`, rule 11 sub match & `price == 0`, rule 18 status transitions on `update` |
| `AttendanceDao.java` | `attendance` | `findByCustomerId(int)` | Non-null, existing `customerId`, ISO `date`, `HH:mm` `checkIn`/`checkOut >= checkIn`, rule 5 (`ACTIVE` member + unexpired plan), blocks duplicate same-day insert |
| `ProgressRecordDao.java` | `progress_records` | `findByCustomerId(int)` | Non-null, existing `customerId`, ISO `date`, `weightKg > 0`, `0 <= bodyFatPct <= 100`, nullable `targetWeightKg > 0` |
| `PaymentDao.java` | `payments` | `findByReceiptNo(String)`, `findByCustomerId(int)` | Non-null, existing `customerId`, `amount > 0`, non-null `method`, ISO `date`/`voidedDate`, unique `receiptNo`, rule 12 strict XOR target link + owner `customerId` match |

### Representative Sketches (authoritative signatures in `docs/UML.md` §3)

#### `CustomerDao.java` — CRUD + nullable `trainerId` mapping

```java
// Sketch — full implementation in src/main/java/com/gym/dao/CustomerDao.java
public Customer findById(int id) throws DatabaseException {
    String sql = "SELECT * FROM customers WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, id);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return mapRow(rs);
            }
        }
        return null;
    } catch (SQLException e) {
        throw new DatabaseException("Failed to find customer with id: " + id, e);
    }
}
```

#### `PaymentDao.java` — `BigDecimal` columns + XOR rule guard

```java
// Sketch — ensures exactly one of membershipId / subscriptionId / sessionId is set (rule 12)
private void validate(Payment p) throws ValidationException {
    if (p == null) throw new ValidationException("Payment cannot be null");
    if (p.getAmount() == null || p.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
        throw new ValidationException("Payment amount must be greater than zero");
    }
    int links = 0;
    if (p.getMembershipId() != null) links++;
    if (p.getSubscriptionId() != null) links++;
    if (p.getSessionId() != null) links++;
    if (links != 1) {
        throw new ValidationException("Payment must link to exactly one target (membership, subscription, or session)");
    }
}
```

## 6. How this layer connects to the others

| Layer | Connection |
|---|---|
| `com.gym.util.DatabaseConnection` | Supplies `getConnection()` and creates all 13 tables via `initializeDatabase()` |
| `com.gym.model.*` | Supplied as parameters (`insert`, `update`) and returned by queries (`findById`, `findAll`) |
| `com.gym.exception.*` | Every DAO throws `ValidationException` and `DatabaseException` (never raw `SQLException`) |
| `com.gym.cli.*` | Calls all 13 DAOs live today in CMD workflows and the 22-scenario TA verification suite (`DECISIONS.md` 78) |
| `com.gym.controller.*` | Desktop JavaFX screens use demo data today; **Milestone 3** connects controllers directly to these 13 DAOs |

## 7. Common mistakes and review checklist

| Symptom | Cause | Fix |
|---|---|---|
| `no such table: customers` | `DatabaseConnection.initializeDatabase()` was never called before querying | Call `DatabaseConnection.initializeDatabase()` once on startup |
| `trainerId` becomes `0` when SQLite column is `NULL` | Called `rs.getInt("trainer_id")` without checking `rs.wasNull()` | Check `int v = rs.getInt(...); entity.setTrainerId(rs.wasNull() ? null : v);` |
| `SQLITE_BUSY` / locked database | Left a `Connection` or `ResultSet` open across queries | Wrap every JDBC resource in `try (...)` |
| String concatenation in `WHERE` clause | Breaks on apostrophes (`O'Connor`) and enables SQL injection | Always use `PreparedStatement` with `?` bindings |

Review checklist:

- [ ] Every SQL query uses `PreparedStatement` with `?` placeholders (except fixed parameterless `findAll`).
- [ ] Every `Connection`, `PreparedStatement`, and `ResultSet` is inside `try-with-resources`.
- [ ] `SQLException` is wrapped in `DatabaseException` with cause chained.
- [ ] `validate(...)` runs before `insert` and `update`, throwing `ValidationException`.
- [ ] Zero JavaFX imports anywhere in `com.gym.dao`.

## 8. Learning exercises (with answers)

1. Why does `CustomerDao.insert` pass `Statement.RETURN_GENERATED_KEYS` when preparing the `INSERT`?
2. Look at `CustomerDao.mapRow(ResultSet)`: why do we check `rs.wasNull()` after reading
   `trainer_id`, instead of trusting `rs.getInt("trainer_id")` directly?
3. Explain the difference between returning `null` in `findById(int)` and returning an empty `List` in
   `findAll()`.
4. Why does `PaymentDao.insert` store `BigDecimal` safely without `double` precision errors?
5. What happens if someone writes `SELECT * FROM customers WHERE id = ` + id using `Statement`
   instead of `PreparedStatement`?

<details>
<summary>Answers</summary>

1. SQLite auto-generates the `INTEGER PRIMARY KEY AUTOINCREMENT` `id`; reading `ps.getGeneratedKeys()`
   lets `insert` set the new `id` back onto the returned entity object.
2. `ResultSet.getInt` returns primitive `0` when a database column is SQL `NULL`. Checking `rs.wasNull()`
   preserves `null` in Java's boxed `Integer trainerId`.
3. `findById` looks up at most one record (`null` = absent), while `findAll` represents zero-to-many rows,
   where an empty list prevents `NullPointerException` in loops.
4. `PaymentDao` passes/reads `BigDecimal` values directly (`ps.setBigDecimal` / string representation)
   and stores them in SQLite `NUMERIC` columns, avoiding IEEE-754 floating-point drift.
5. Concatenation opens SQL injection risk, fails on unescaped special characters, and skips SQLite
   prepared statement caching.

</details>

## 9. Ownership and tasks

| DAO Group | Responsibility | Owner | Status |
|---|---|---|---|
| `CustomerDao`, `MedicalProfileDao`, `AdministratorDao` | Member, medical & admin persistence | Abdelrhman | Done (Milestone 2) |
| `TrainerDao`, `TrainerAvailabilityDao`, `MembershipDao` | Coach, availability & plan persistence | Ziad | Done (Milestone 2) |
| `WorkoutProgramDao`, `SessionDao`, `TrainerSubscriptionDao`, `EquipmentDao` | Program, schedule & gear persistence | Yousef | Done (Milestone 2) |
| `AttendanceDao`, `PaymentDao`, `ProgressRecordDao` | Attendance, billing & progress persistence | Abdel Raouf | Done (Milestone 2) |

`swing` branch rule: because `com.gym.dao` contains zero JavaFX imports, all 13 DAO classes compile and
work identically on the `swing` branch.

### Definition of done for a file in this folder

- [ ] Matches `docs/UML.md` §3 signatures and `DatabaseConnection` column names.
- [ ] Compiles cleanly under `./gradlew compileJava` with `-Xlint:all`.
- [ ] Uses `try-with-resources`, `PreparedStatement`, and `ValidationException` / `DatabaseException`.
- [ ] Zero JavaFX imports.
