# `db` — SQLite Schema & Runtime Storage (`gym.db`)

> **Status: SHIPPED IN CODE (Milestone 2).** All 13 table definitions (`CREATE TABLE IF NOT EXISTS`)
> live in [`DatabaseConnection.initializeDatabase()`](../../java/com/gym/util/DatabaseConnection.java)
> (`org.xerial:sqlite-jdbc:3.45.1.0` in `build.gradle`), rather than a standalone `schema.sql` file.
> This folder intentionally holds `.gitkeep` and this `README.md` only. Tracked in
> [`docs/COVERAGE.md`](../../../docs/COVERAGE.md).

## 1. Purpose — the problem this layer solves

SQLite is an **embedded**, file-based relational database stored at runtime in `gym.db` (created in
the working directory). Keeping the schema definition centralized in
`DatabaseConnection.initializeDatabase()` ensures all 13 DAOs in `com.gym.dao` agree on exact table
names, column names, types, uniqueness rules, and defaults.

**What breaks without shared schema initialization.** Every query (`SELECT * FROM customers`,
`INSERT INTO payments ...`) throws `SQLException: no such table` if `gym.db` starts empty without
running `DatabaseConnection.initializeDatabase()`. Meanwhile, committing a local `gym.db` file into
git creates unmergeable binary conflicts and overwrites teammates' local test data.

**Why SQLite fits this app.** Zero server installation, single file portability for demos, ACID
transactions, and pure JDBC support via `sqlite-jdbc`. Its deliberate limitation is single-machine
use only (recorded in root `README.md` §9).

**What this folder deliberately does not do.** No runtime `gym.db` files are committed here
(`.gitignore` excludes `gym.db`, `gym.db-*`, and `*.db`). All business validation lives in Java
(`com.gym.dao.*` `validate(...)` helpers throwing `ValidationException`), not SQL stored procedures.

## 2. What you learn here (SQLite & Schema Concepts)

| Concept | Where in `DatabaseConnection` | Why the project needs it | How to apply it |
|---|---|---|---|
| Data Definition Language (DDL) | `CREATE TABLE IF NOT EXISTS ...` in `initializeDatabase()` | Defines all 13 tables in one place | Match every column name read by the 13 DAOs |
| Idempotent initialization | `IF NOT EXISTS` on all 13 statements | Startup initialization can run safely even when `gym.db` already has data | Never run `DROP TABLE` on startup |
| Primary Keys (`AUTOINCREMENT`) | `id INTEGER PRIMARY KEY AUTOINCREMENT` on all 13 tables | Assigns unique database IDs returned via `Statement.RETURN_GENERATED_KEYS` | Read back generated IDs in `DAO.insert(...)` |
| Unique constraints | `payments.receipt_no UNIQUE`, `medical_profiles.customer_id UNIQUE`, `administrators.username UNIQUE` | Prevents duplicate receipts, multiple medical profiles per member, or duplicate logins | Handle constraint violations cleanly via `DatabaseException` |
| Type affinity (`TEXT`, `INTEGER`, `REAL`, `NUMERIC`) | ISO strings as `TEXT`; IDs/counts as `INTEGER`; body metrics as `REAL`; money as `NUMERIC` | Keeps schema self-documenting and consistent with Java types | Map `Enum.name()` → `TEXT`; `BigDecimal` → `NUMERIC` |
| Schema vs runtime file separation | DDL committed in Java vs `gym.db` git-ignored | Version-controls structure without committing changing user rows | Keep `gym.db` out of git (`*.db` in `.gitignore`) |

## 3. Design patterns in this layer

| Pattern | Where | Why this pattern | What goes wrong if used carelessly |
|---|---|---|---|
| **Embedded DDL in `DatabaseConnection`** | `DatabaseConnection.initializeDatabase()` | Zero file-path or classpath reading bugs for a beginner SQLite setup; no external script parser needed | Letting table columns drift between `initializeDatabase()` and a DAO breaks `mapRow(ResultSet)` |
| **Idempotent Bootstrap** | `CREATE TABLE IF NOT EXISTS` | Calling `initializeDatabase()` repeatedly preserves existing user data | Running destructive resets destroys saved records |
| **Local Runtime Database Artifact** | `./gym.db` in working directory | Each teammate keeps independent local data cleanly | Saving `gym.db` inside `src/main/resources/` bakes a read-only copy into the jar |
| **Guarded / Flat Relational Tables** | Flat FKs without `ON DELETE CASCADE` | Keeps deletes explicit inside DAO callers (`rule 4`) | Silent cascades erase payment or attendance history |

## 4. Rules you must follow (and the bug each rule prevents)

1. **Never commit `gym.db` or `*.db` files.** `.gitignore` blocks them so binary conflicts never reach `main`.
2. **Every table in `DatabaseConnection.initializeDatabase()` stays idempotent** (`CREATE TABLE IF NOT EXISTS`).
3. **Money columns use `NUMERIC` (`BigDecimal`)** (`hourly_rate`, `price`, `amount`, `previous_amount`, `rate_per_period`) — never float `REAL`.
4. **Dates and times are ISO-8601 `TEXT`** (`yyyy-MM-dd`, `HH:mm`) so SQL sorting matches chronological order.
5. **Enums are persisted as `.name()` `TEXT`** (`status`, `plan`, `method`, `role`, `condition`).
6. **Nullable boxed fields stay nullable in SQL** (`trainer_id INTEGER`, `membership_id INTEGER`, `subscription_id INTEGER`, `session_id INTEGER`, `target_weight_kg REAL`); required fields declare `NOT NULL`.

## 5. The 13 tables created by `DatabaseConnection`

| # | SQLite Table | Entity (`com.gym.model`) | Owning DAO (`com.gym.dao`) | Notable Constraints / Columns |
|---|---|---|---|---|
| 1 | `customers` | `Customer` | `CustomerDao` | `full_name NOT NULL`, nullable `trainer_id` |
| 2 | `trainers` | `Trainer` | `TrainerDao` | `hourly_rate NUMERIC NOT NULL` |
| 3 | `memberships` | `Membership` | `MembershipDao` | `customer_id`, `plan`, `price NUMERIC NOT NULL`, `frozen_days DEFAULT 0` |
| 4 | `workout_programs` | `WorkoutProgram` | `WorkoutProgramDao` | `customer_id`, `creator_trainer_id`, `version INTEGER NOT NULL` |
| 5 | `sessions` | `Session` | `SessionDao` | `duration_min NOT NULL`, nullable `availability_id`, `subscription_id`, `price NUMERIC` |
| 6 | `equipment` | `Equipment` | `EquipmentDao` | `name`, `condition`, `status` all `NOT NULL` |
| 7 | `attendance` | `Attendance` | `AttendanceDao` | `customer_id NOT NULL`, `attendance_date`, `check_in`, `check_out` |
| 8 | `payments` | `Payment` | `PaymentDao` | `receipt_no TEXT UNIQUE NOT NULL`, `voided INTEGER DEFAULT 0`, `previous_amount NUMERIC`, nullable XOR FKs |
| 9 | `progress_records` | `ProgressRecord` | `ProgressRecordDao` | `weight_kg REAL NOT NULL`, `body_fat_pct REAL NOT NULL`, nullable `target_weight_kg REAL` |
| 10 | `medical_profiles` | `MedicalProfile` | `MedicalProfileDao` | `customer_id INTEGER UNIQUE NOT NULL` |
| 11 | `administrators` | `Administrator` | `AdministratorDao` | `username TEXT UNIQUE NOT NULL`, `role TEXT NOT NULL`, `active INTEGER DEFAULT 1` |
| 12 | `trainer_subscriptions` | `TrainerSubscription` | `TrainerSubscriptionDao` | `rate_per_period NUMERIC NOT NULL` |
| 13 | `trainer_availability` | `TrainerAvailability` | `TrainerAvailabilityDao` | `max_customers INTEGER NOT NULL` |

## 6. How this connects to the others

| Layer | Connection |
|---|---|
| `com.gym.util.DatabaseConnection` | Declares and executes all 13 `CREATE TABLE IF NOT EXISTS` statements |
| `com.gym.dao.*` (13 DAOs) | Queries and updates these exact 13 tables via `PreparedStatement` |
| `.gitignore` | Keeps runtime `gym.db` and `*.ser` files out of git history |

## 7. Common mistakes and review checklist

- **Mistake:** Editing a field in a DAO without updating `DatabaseConnection.initializeDatabase()`.
- **Mistake:** Putting `gym.db` inside `src/main/resources/db/` (files inside `resources/` become
  read-only entries inside packaged `.jar` archives).

Checklist:

- [ ] Every column queried in `com.gym.dao.*` matches `DatabaseConnection.initializeDatabase()`.
- [ ] Zero `*.db` or `*.ser` files tracked by git (`git status` clean).
- [ ] All money columns remain `NUMERIC`; dates remain ISO `TEXT`.

## 8. Learning exercises (with answers)

1. Why do `DatabaseConnection.initializeDatabase()` statements all start with `CREATE TABLE IF NOT EXISTS`?
2. Which three columns enforce `UNIQUE` at the SQLite table level across the 13 tables?
3. Why is `gym.db` excluded in `.gitignore` while `DatabaseConnection.java` is committed?

<details>
<summary>Answers</summary>

1. So running `initializeDatabase()` on subsequent app launches creates missing tables without deleting
   or failing on tables that already hold saved data.
2. `payments.receipt_no`, `medical_profiles.customer_id`, and `administrators.username`.
3. Source code defines the shared empty schema cleanly across machines, whereas `gym.db` contains
   per-developer binary runtime rows that cannot be cleanly merged in Git.

</details>

## 9. Ownership and tasks

| Item | Owner | Status |
|---|---|---|
| Embedded 13-table SQLite schema (`DatabaseConnection.initializeDatabase()`) | Abdelrhman | Done (Milestone 2) |
| Seed data / Milestone 3 controller integration | All (per feature slice) | Planned (Milestone 3) |

`swing` branch rule: identical SQLite table schema (`gym.db`) works unchanged on both `main`
(JavaFX) and `swing` branches.
