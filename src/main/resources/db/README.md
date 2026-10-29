# `db` — The SQLite schema: `schema.sql` and the seed (JavaFX branch)

> **Signature authority:** `docs/UML.md` §1 owns every entity field and its type, §3 owns every DAO
> query the schema must be able to serve, and "Business rules" owns rules 4, 9, 12, 13 and 19, which the
> DDL implements. This README explains *why* the schema looks the way it does and *how* to write it. The
> SQL here is an illustrative sketch, not the specification — if a sketch and `docs/UML.md` disagree,
> `docs/UML.md` wins.
>
> **Not documented here:** DAO classes, JDBC calls, `PreparedStatement` usage and exception translation.
> That is `src/main/java/com/gym/dao/README.md`. This README documents the **file** the DAOs query.

## 1. Purpose — the problem this folder solves

`db/` holds the committed definition of the database: `schema.sql`, the DDL (**Data Definition
Language** — `CREATE TABLE`, `CREATE INDEX`) that creates the 13 tables, plus the demo seed rows. The
database *file* is not here: SQLite is an **embedded** database, so "the database" is one ordinary file
(`gym.db`) that the app creates on first run, next to the working directory, and that `.gitignore`
excludes.

**What breaks if this folder is missing.** No DAO can run a single query: `SqliteDbConnection.
initializeSchema()` has no DDL to execute, so the first `findAll()` fails with `no such table:
customers`. There is also no shared definition of the columns, so each of the 13 DAO implementations
would guess its own column names and the app would disagree with itself about what a "customer" row is.

**Why SQLite fits this app.** No server to install, no connection string, no credentials, one file to
copy for a demo, and the whole database is transactional and consistent after a crash. A college desktop
app that runs on one machine at a time gets a real relational database with ACID guarantees and zero
deployment work.

**The limitation, stated honestly.** One SQLite file is readable and writable by one machine at a time.
A gym with several branches that must share live data needs a client-server database later — this is
recorded as a known limitation in `README.md` §9 of the repository root ("SQLite = single-machine only;
multi-branch gyms need a client-server DB later"), and it is *not* something this folder can fix.

**What this folder deliberately does not do.** It stores no behaviour. A `CHECK` constraint refuses a row
that is already invalid; it never decides what is valid. The decision lives in
`src/main/java/com/gym/service/` (rules 1–19), and the constraint is the last line of defence.

## 2. What you learn here (OOP concepts)

Concept names match `docs/LEARNING_GUIDE.md` → "OOP concept index".

| Concept | Where in this folder | Why the project needs it | How to apply it (one line) |
|---|---|---|---|
| Abstraction | the whole schema | A table is the shared contract between 13 DAOs and 13 entities; nobody re-invents a column | Change a column in one place — `schema.sql` — and update the entity in the same commit |
| Information hiding | column names and `CHECK` constraints | A caller sees `status = 'ACTIVE'`, never how the row is stored | Never write a query outside a DAO that depends on the physical layout |
| Invariants (design by contract) | `NOT NULL`, `CHECK`, `UNIQUE`, FK declarations | A rule that the database enforces cannot be broken by a buggy screen at 2 a.m. | Every invariant you can express in one row, express it once as a constraint |
| Referential integrity | `FOREIGN KEY … REFERENCES` | A session cannot point at a customer who does not exist | Declare the FK, and understand that SQLite needs `PRAGMA foreign_keys = ON` to enforce it |
| Type affinity | `TEXT`, `INTEGER`, `REAL` | SQLite has no strict column types; naming the intended type documents intent and drives conversions | Pick the type that matches the Java type: `String` → `TEXT`, enum → `TEXT`, count → `INTEGER` |
| Idempotency | `CREATE TABLE IF NOT EXISTS`, `INSERT OR IGNORE` | The schema script and the seed run on every start, so running them twice must be harmless | Write every init statement so a second run changes nothing |
| Single source of truth | `schema.sql` (committed) vs `gym.db` (runtime) | A committed database overwrites a teammate's demo data and creates merge conflicts forever | Version the definition, ignore the data |

## 3. Design patterns in this layer

| Pattern | Where | Why this pattern | What goes wrong if used carelessly |
|---|---|---|---|
| DDL as a checked-in script (schema-as-code) | `schema.sql` | The table definition is reviewable in a diff and identical on every machine | Hand-editing `gym.db` in a browser tool creates a schema nobody can reproduce |
| Idempotent bootstrap | `IF NOT EXISTS` + `INSERT OR IGNORE` | `initializeSchema()` may run on every start; it must be a no-op the second time | A `DROP TABLE` "to be safe" deletes the demo database during a presentation |
| Assertion through constraints | `CHECK` (rule 12's XOR), `NOT NULL`, `UNIQUE` | The XOR rule is enforced even if a future service forgets to call `validLink` | A constraint that duplicates a rule the service also checks is fine; a constraint that *contradicts* one is a bug |
| Guarded delete (no cascades) | the absence of `ON DELETE CASCADE` | Rule 4's admin hard delete must stop while references exist, and `GuardService.countReferences` does the counting | A cascade deletes the evidence and the guard's count silently becomes zero |
| Repository / DAO | **not here — `src/main/java/com/gym/dao/`** | SQL statements in Java are the DAO layer's job; this folder owns only the structure | Putting `SELECT` statements in a `.sql` file per screen splits persistence across two folders |
| ORM / migration framework | **rejected** | 13 tables and one developer per area: an ORM adds a dependency, and a migration tool adds a version table nobody would read | Introducing either later is a decision to log, not a quiet addition |

## 4. Rules you must follow (and the bug each rule prevents)

1. **`schema.sql` is committed; `gym.db` never is.** `.gitignore` lists `gym.db`, `gym.db-*` and `*.db`.
   A committed database overwrites a teammate's demo rows on the next `git pull` and produces a binary
   merge conflict nothing can resolve.
2. **`gym.db` is created at runtime, next to the working directory.** Never inside `src/main/resources`
   or the build output: the classpath is read-only after `gradlew build`
   (`src/main/resources/README.md` §4 rule 2). `SqliteDbConnection` owns the path — exactly one class.
3. **Every `CREATE` is idempotent: `CREATE TABLE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`.** The
   schema script runs on every start. Without `IF NOT EXISTS`, the second launch throws
   `table customers already exists` and the app cannot open.
4. **Use `INTEGER PRIMARY KEY AUTOINCREMENT` for every surrogate key.** `AUTOINCREMENT` guarantees an id
   is never reused after a delete. With plain `INTEGER PRIMARY KEY`, SQLite may hand a deleted id to a new
   row, and an old receipt number then points at a different person — an error nobody notices.
5. **Declare every foreign key, and never add `ON DELETE CASCADE`.** Rule 4 makes deletion a *checked*
   operation: `AdminService` calls `GuardService.countReferences(table, id)` and refuses while references
   exist. A cascade would delete child rows silently and make that guard always return zero.
6. **Turn FK enforcement on per connection with `PRAGMA foreign_keys = ON`.** SQLite parses a
   `FOREIGN KEY` clause but ignores it unless the pragma is set for that connection. The pragma is not
   persistent, so `SqliteDbConnection.getConnection()` sets it every time. Without it, the declared FKs
   are documentation only.
7. **Encode every enum as `TEXT` holding the Java literal's name, plus a `CHECK` listing the literals.**
   `status TEXT NOT NULL CHECK (status IN ('ACTIVE','INACTIVE','SUSPENDED'))`. The spellings must match
   `docs/UML.md` §1 exactly (`IN_MAINTENANCE`, `NO_SHOW`, …) or `Enum.valueOf` throws at read time.
8. **Derived literals are never written by hand.** `AvailabilityStatus.FULL` and both `EXPIRED` literals
   are computed on read (rule 13). A `CHECK` may list them so an old row cannot become unreadable, but no
   `INSERT`/`UPDATE` should ever set them. Whether the expiry sweep persists `MEMBERSHIP`'s `EXPIRED` or
   only recomputes it is an **Open detail (decide + log in `DECISIONS.md`)**.
9. **Dates and times are ISO-8601 `TEXT`: `yyyy-MM-dd` for dates, `HH:mm` for times** (rule 9,
   `DECISIONS.md` 31). They sort chronologically as text, which is what makes `findByDay`,
   `findExpiring` and `revenueBetween` work without a date type. `DateUtils` owns every parse, so the
   schema must never store `12/06/2025` or `9:5`.
10. **Money is exact — never `REAL`.** Java holds money as `BigDecimal` (`DECISIONS.md` 31) because
    `double` cannot represent `0.1`, so a `REAL` column would silently re-introduce the error the Java
    side avoids. Storage is either `TEXT` (the `BigDecimal` string) or `INTEGER` (cents) — the choice is
    an **Open detail (decide + log in `DECISIONS.md`)**; §5 spells out the trade-off.
11. **The payment XOR is a real constraint, not a convention** (rule 12). The schema enforces **at most
    one** of `membershipId`, `subscriptionId`, `sessionId` being non-null, and `PaymentService.validLink`
    enforces the **exactly one** half on purpose: a `CHECK` failure reaches the user as
    `DaoException` → "Could not record payment", while `validLink` can say *"choose exactly one of
    membership, subscription or session"*. A payment with two links double-counts revenue; a payment
    with none is money that belongs to nothing — both are refused, by the layer that can explain why.
    See [`../../java/com/gym/dao/README.md`](../../java/com/gym/dao/README.md) for the same split.
12. **`attendance` and `payments` are never deleted** (rule 19). No `DELETE` statement for these two
    tables, in code or in a script. Corrections go through `AttendanceService.correctRecord` and through
    void-plus-new-payment, which is why `payments` carries `voided`, `voidReason`, `voidedDate` and
    `previousAmount` instead of a delete path.
13. **Add an index only for a column a DAO actually filters on.** Every index costs space and slows
    inserts and the seed. In particular, do **not** index a `LIKE '%text%'` search column
    (`CustomerDao.search`): a leading wildcard cannot use an index, so the index only slows writes.
14. **Wrap multi-statement writes in one transaction.** `PaymentService.record` may write a payment and
    update related rows; if the second statement fails, the first must roll back. Use
    `connection.setAutoCommit(false)` … `commit()` / `rollback()` in the DAO, and keep the schema's
    `PRAGMA` calls outside the transaction.
15. **No schema change without a `docs/UML.md` change.** A new column is a new entity field; the diagram
    is the authority (`docs/DOCS_STANDARD.md` §1). Write the field into §1 first, then the DDL, then the
    model class, then the DAO mapping — in that order.
16. **The seed is idempotent and uses fixed ids.** `INSERT OR IGNORE INTO customers (id, …) VALUES
    (1, …)` with literal ids, or a separate `--reset` script that deletes `gym.db` first. A seed that
    appends on every start turns a demo into 40 duplicate customers.

## 5. The files in this folder

Read order: `schema.sql` first (what the tables are), then the seed (what the demo shows), then the
per-table entries in **creation order** — a table can only reference one that already exists, so
`trainers` and `equipment` come before `customers`, and `sessions` before `payments`.

Every table entry uses the same shape: *Purpose · Columns · Constraints · Indexes · DAO · Rules*. Two
shorthands appear in the column tables:

| Shorthand | Means |
|---|---|
| **money** | `TEXT` (the `BigDecimal` string, e.g. `'45.00'`) **or** `INTEGER` (cents, e.g. `4500`). One spelling for all six money columns, decided once — **Open detail (decide + log in `DECISIONS.md`)** |
| **iso date** / **iso time** | `TEXT`, `yyyy-MM-dd` / `HH:mm` (rule 9, `DECISIONS.md` 31) |

Column names are the entity field names from `docs/UML.md` §1 (`customerId`, `passwordHash`,
`ratePerPeriod`), the same spelling `src/main/java/com/gym/dao/README.md` already publishes in its "Week 1
table specs". SQLite identifiers are case-insensitive, so `customerid` would also work — but pick one
spelling and use it in every statement, because the `ResultSet` mapping is written by hand.

### `schema.sql` — the 13 tables, their constraints and indexes (owner: Abdelrhman)

**Status:** planned (Week 1, foundation — everyone blocks on it).
**Canonical:** `docs/UML.md` §1 (fields), §3 (the queries it must serve), rules 4, 9, 12, 13, 19.

**What it is.** One text file of DDL that creates every table, index and constraint this app needs. It
contains no data and no `SELECT`.

**Why it is designed this way.** A single file means one command recreates the whole database, and a
reviewer sees the entire structure in one diff. Splitting it per entity would need a load order anyway
because of the foreign keys, and would hide cross-table rules such as rule 12.

**How to write it.** 1. Header comment: what the file is, who owns it, that it is idempotent. 2. The
tables in dependency order (below). 3. Constraints inline in `CREATE TABLE`: `NOT NULL`, `DEFAULT`,
`CHECK`, `UNIQUE`, `FOREIGN KEY`. 4. `CREATE INDEX IF NOT EXISTS` for every column a DAO filters on.
5. No `DROP`, no `DELETE`, no `PRAGMA foreign_keys` (that belongs to `SqliteDbConnection`, per
connection). 6. Keep every statement terminated with `;` and one statement per line group, so a partial
failure points at one statement.

**Sketch.** Abridged to two tables plus the parts that are easy to get wrong:

```sql
-- db/schema.sql — Gym Management System (SQLite)
-- Idempotent: safe to run on every start. No data lives here. Team-owned; see docs/UML.md §1.

CREATE TABLE IF NOT EXISTS trainers (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  fullName        TEXT    NOT NULL,
  phone           TEXT    NOT NULL,
  email           TEXT,
  specialization  TEXT,
  experienceYears INTEGER NOT NULL DEFAULT 0,
  certifications  TEXT,
  hireDate        TEXT,                      -- iso date
  hourlyRate      TEXT    NOT NULL DEFAULT '0.00'   -- money (see the open storage choice)
);

CREATE TABLE IF NOT EXISTS customers (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  fullName       TEXT    NOT NULL,
  phone          TEXT    NOT NULL,
  email          TEXT,
  address        TEXT,
  dateOfBirth    TEXT,                       -- iso date
  gender         TEXT,
  emergencyName  TEXT,
  emergencyPhone TEXT,
  status         TEXT    NOT NULL DEFAULT 'ACTIVE'
                 CHECK (status IN ('ACTIVE','INACTIVE','SUSPENDED')),
  joinDate       TEXT    NOT NULL,           -- iso date
  trainerId      INTEGER,                    -- nullable: a customer may have no trainer (rule 15)
  FOREIGN KEY (trainerId) REFERENCES trainers(id)   -- declared, never cascaded (rule 4)
);

CREATE INDEX IF NOT EXISTS idx_customers_trainer ON customers(trainerId);
CREATE INDEX IF NOT EXISTS idx_customers_status  ON customers(status);
-- NOT indexed: fullName. CustomerDao.search uses LIKE '%text%', which cannot use an index (rule 13).

CREATE TABLE IF NOT EXISTS payments (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  customerId     INTEGER NOT NULL,
  membershipId   INTEGER,
  subscriptionId INTEGER,
  sessionId      INTEGER,
  amount         TEXT    NOT NULL,           -- money
  method         TEXT    NOT NULL CHECK (method IN ('CASH','CARD','TRANSFER')),
  date           TEXT    NOT NULL,           -- iso date
  receiptNo      TEXT    NOT NULL UNIQUE,
  voided         INTEGER NOT NULL DEFAULT 0 CHECK (voided IN (0,1)),
  voidReason     TEXT,
  voidedDate     TEXT,
  previousAmount TEXT,                       -- money; admin adjust keeps the old value (rule 19)
  FOREIGN KEY (customerId)     REFERENCES customers(id),
  FOREIGN KEY (membershipId)   REFERENCES memberships(id),
  FOREIGN KEY (subscriptionId) REFERENCES trainer_subscriptions(id),
  FOREIGN KEY (sessionId)      REFERENCES sessions(id),
  -- Rule 12: at most one link. In SQLite a comparison is 1/0, so the three terms add up and the row is
  -- refused if two are set. The "exactly one" half is PaymentService.validLink's job (it can name the
  -- problem in a dialog); see ../../java/com/gym/dao/README.md.
  CHECK ((membershipId IS NOT NULL) + (subscriptionId IS NOT NULL) + (sessionId IS NOT NULL) <= 1)
);

CREATE INDEX IF NOT EXISTS idx_payments_customer ON payments(customerId);
CREATE INDEX IF NOT EXISTS idx_payments_date     ON payments(date);   -- revenueBetween(from, to)
-- receiptNo is already indexed by its UNIQUE constraint: PaymentDao.findByReceipt
```

⚠️ If money is stored as `TEXT`, `CHECK (subscriptionId IS NULL OR price = 0)` in `sessions` compares a
string with a number. Write it as `CAST(price AS NUMERIC) = 0`, or store cents as `INTEGER` and compare
directly. This is the clearest argument for deciding the storage question early (rule 10).

**How to know it is right.** Delete `gym.db`, run the app, and confirm: the file is re-created next to
the working directory; `SELECT name FROM sqlite_master WHERE type='table'` lists 13 tables; running the
app twice does not throw; one insert per table succeeds with the correct enum spelling.

**Common mistakes.** Forgetting `IF NOT EXISTS` so the second launch fails; adding
`ON DELETE CASCADE` "to keep the data tidy" and destroying rule 4; using `REAL` for money; indexing
`fullName`; writing `AUTOINCREMENT` on a column that is not exactly `INTEGER PRIMARY KEY` (SQLite rejects
it); putting `INSERT` statements for the demo data in this file, so schema review and data review become
one commit.

### the seed — demo rows with fixed ids (owner: Abdelrhman)

**Status:** planned. **Canonical:** `docs/UML.md` §1 (the field lists the rows must satisfy), rules 5, 12.

**What it is.** A small set of rows that makes every screen show something: a few trainers, a handful of
customers with memberships, sessions across a day, attendance, payments (including one voided), progress
records and one medical profile.

**Why it is designed this way.** The seed is *not* part of the schema: the app must work with an empty
database, and the seed must be re-runnable without doubling the data. Fixed ids make the rows
referencable in the docs and in a bug report ("customer 3 has no membership").

**How to write it.** 1. Use `INSERT OR IGNORE INTO <table> (id, …) VALUES (…);` with literal ids, so a
second run is a no-op. 2. Insert parents before children (`trainers`, `equipment`, `administrators`, then
`customers`, then the rest). 3. Give each row a comment saying what it demonstrates — "customer 3: frozen
membership, exercises rule 7" is worth more than ten random rows. 4. Keep the seed small enough to read.

**Sketch.**

```sql
-- db/seed_demo.sql — idempotent demo data. Parents first, literal ids, OR IGNORE everywhere.
INSERT OR IGNORE INTO trainers  (id, fullName, phone, specialization, experienceYears, hireDate, hourlyRate)
VALUES (1, 'Ziad Hassan', '01000000001', 'Strength', 6, '2021-03-01', '120.00');

INSERT OR IGNORE INTO customers (id, fullName, phone, status, joinDate, trainerId)
VALUES (3, 'Yousef Adel', '01000000003', 'SUSPENDED', '2024-01-15', 1);  -- rule 14: memberships must be FROZEN

INSERT OR IGNORE INTO payments  (id, customerId, membershipId, amount, method, date, receiptNo)
VALUES (1, 3, 3, '450.00', 'CASH', '2025-01-15', 'R-2025-0001');         -- rule 12: one link set
```

**How to know it is right.** Running the seed twice leaves the same row counts; every FK resolves; every
enum value in the seed appears in a `CHECK` list; income totals in the dashboard match the seed's
non-voided payments.

**Common mistakes.** Plain `INSERT` (second run throws on the primary key); random or
`datetime('now')`-derived ids, so the docs cannot name a row; a payment with two links; seeding a
`SUSPENDED` customer with an `ACTIVE` membership, which contradicts rule 14 on the very first screen.

**Open details for the seed.** Two choices are not made yet, and both must be logged in
`docs/DECISIONS.md` before the file lands:

| Open detail | Options | Why it matters |
|---|---|---|
| Seed file name and location | `db/seed_demo.sql` in this folder · `db/seed.sql` · a `--reset` script beside `schema.sql` | Decides whether `SqliteDbConnection.initializeSchema()` runs it, or a human does |
| Seeding mechanism | `SqliteDbConnection.initializeSchema()` executes it from the classpath on every start · a Gradle task · a manual `sqlite3 < db/seed_demo.sql` | A Java bootstrap keeps one command for the team but puts demo data on the production path; a script keeps the app honest but adds a manual step |

### `.gitkeep` — keeps the folder tracked

**Status:** exists. **What it is.** A zero-byte file, because Git does not track empty directories.
Delete it in the commit that adds `schema.sql`. **Mistakes.** Deleting it while the folder is still empty
(the folder disappears from every teammate's clone); leaving it after real files exist (harmless noise,
but reviewers ask).

### Not in this folder

`gym.db`, `gym.db-wal`, `gym.db-shm` and every `*.db` file. They are runtime artefacts of SQLite: the
database, its write-ahead log and its shared-memory file. They are created next to the working directory
(after `gradlew run`, that is the project root) and ignored by `.gitignore`. If one ever appears here,
delete it and check that nobody re-introduced a resource-path write (`src/main/resources/README.md` §4
rule 2).

### The 13 tables

Each table is written by the owner of the entity it stores (`docs/UML_TEAM.md`); `schema.sql` as a whole
is Abdelrhman's Week 1 foundation task. Every column name and type comes from `docs/UML.md` §1 — if the
entity changes there, the DDL changes here in the same commit (rule 15).

#### `trainers` — the coach roster (owner: Ziad)

**Purpose.** One row per coach. Bookable hours are *not* here: they are rows in `trainer_availability`,
because a trainer may publish several slots per day (rule 8).

| Column | Type | Notes |
|---|---|---|
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | Surrogate key |
| `fullName` | `TEXT NOT NULL` | |
| `phone` | `TEXT NOT NULL` | Stored as text: a phone number is an identifier, not a number |
| `email` | `TEXT` | Nullable |
| `specialization` | `TEXT` | Free text (e.g. "Strength") |
| `experienceYears` | `INTEGER NOT NULL DEFAULT 0` | Count, never negative in practice; the service validates |
| `certifications` | `TEXT` | Free text list |
| `hireDate` | `TEXT` | ISO date |
| `hourlyRate` | **money** `NOT NULL DEFAULT '0.00'` | `BigDecimal` on the Java side (`DECISIONS.md` 31) |

**Constraints.** `NOT NULL` on the four required fields. No `UNIQUE` on `phone` or `email`: uniqueness
would be a *new* business rule, so it goes into `docs/UML.md` first.
**Indexes.** None beyond the primary key. `TrainerDao` reads by id or all rows — indexing a column no
query filters on only slows the seed (rule 13).
**DAO.** `TrainerDao` / `TrainerDaoImpl`.
**Rules.** Rule 15 (`Customer.trainerId` is the authoritative assignment); rule 4 (a trainer is retired,
not deleted, and the hard delete asks `GuardService` first).

#### `equipment` — the standalone inventory (owner: Yousef)

**Purpose.** One row per inventory item. It references nothing and nothing references it
(`DECISIONS.md` 30), which is why no other table's `FOREIGN KEY` mentions it.

| Column | Type | Notes |
|---|---|---|
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | |
| `name` | `TEXT NOT NULL` | |
| `category` | `TEXT` | Free text; category icons are matched by name in `images/` |
| `purchaseDate` | `TEXT` | ISO date |
| `condition` | `TEXT NOT NULL` | `CHECK (condition IN ('NEW','GOOD','FAIR','POOR'))` — `EquipmentCondition` |
| `status` | `TEXT NOT NULL` | `CHECK (status IN ('AVAILABLE','IN_MAINTENANCE','RETIRED'))` — `EquipmentStatus` |
| `lastMaintenance` | `TEXT` | Nullable: "never serviced" is a real state, so the column must allow it |

**Constraints.** The two enum `CHECK`s. No foreign keys at all.
**Indexes.** `idx_equipment_status`, `idx_equipment_condition` — `EquipmentDao.findByStatus` and
`findByCondition`.
**DAO.** `EquipmentDao` / `EquipmentDaoImpl`.
**Rules.** Rule 4 (`RETIRED` is the normal end state; the delete is admin-only and now also needs
`GuardService` to find no references — for `equipment` there are none).

#### `administrators` — staff identities (owner: Abdelrhman)

**Purpose.** Who may sign in, with which role. This table is the only source of authentication data.

| Column | Type | Notes |
|---|---|---|
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | |
| `fullName` | `TEXT NOT NULL` | |
| `username` | `TEXT NOT NULL UNIQUE` | `UNIQUE` is what makes `AdminDao.findByUsername` return one row |
| `passwordHash` | `TEXT NOT NULL` | PBKDF2 hash from `PasswordUtils`. **Never a password** (`src/main/resources/README.md` §4 rule 8) |
| `role` | `TEXT NOT NULL` | `CHECK (role IN ('ADMIN','MANAGER','STAFF'))` — `StaffRole` |
| `phone` / `email` | `TEXT` | Nullable |
| `active` | `INTEGER NOT NULL DEFAULT 1` | SQLite has no boolean type: `CHECK (active IN (0,1))`. Primitive `boolean active` in Java (`model/README.md`) |
| `createdDate` | `TEXT` | ISO date |

**Constraints.** `UNIQUE(username)`, the role `CHECK` and the 0/1 `CHECK` on `active`.
**Indexes.** The `UNIQUE` constraint indexes `username`; add `idx_administrators_role` for
`AdminDao.findByRole`.
**DAO.** `AdminDao` / `AdminDaoImpl`.
**Rules.** Rule 16 (`authenticate` rejects `active = 0` and returns `null`; every `AdminService`
operation needs `StaffRole.ADMIN`).

#### `customers` — the members (owner: Abdelrhman)

**Purpose.** One row per member: contact details, status, join date and the assigned trainer.

| Column | Type | Notes |
|---|---|---|
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | |
| `fullName`, `phone` | `TEXT NOT NULL` | The two fields every screen shows |
| `email`, `address`, `gender` | `TEXT` | Nullable |
| `dateOfBirth` | `TEXT` | ISO date |
| `emergencyName`, `emergencyPhone` | `TEXT` | Nullable; the medical screen shows the doctor's phone instead |
| `status` | `TEXT NOT NULL DEFAULT 'ACTIVE'` | `CHECK (status IN ('ACTIVE','INACTIVE','SUSPENDED'))` |
| `joinDate` | `TEXT NOT NULL` | ISO date |
| `trainerId` | `INTEGER` | **Nullable FK** → `trainers(id)`: a customer may have no trainer yet (rule 15) |

**Constraints.** The status `CHECK`; the nullable FK; `DEFAULT 'ACTIVE'` so a new member is usable
immediately (the service still sets it explicitly).
**Indexes.** `idx_customers_trainer` (`findByTrainer`), `idx_customers_status` (`findByStatus`). Do **not**
index `fullName`: `CustomerDao.search` uses `LIKE '%text%'`, which cannot use an index (rule 13).
**DAO.** `CustomerDao` / `CustomerDaoImpl`.
**Rules.** Rule 4 ("delete" means `INACTIVE`); rule 14 (`SUSPENDED` implies the memberships are frozen —
enforced by `CustomerService.changeStatus`, not by a column here).

#### `memberships` — gym access periods (owner: Ziad)

**Purpose.** One row per sold plan. A customer may hold several over time; the active one governs gym
access (rule 5).

| Column | Type | Notes |
|---|---|---|
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | |
| `customerId` | `INTEGER NOT NULL` | FK → `customers(id)` |
| `plan` | `TEXT NOT NULL` | `CHECK (plan IN ('MONTHLY','QUARTERLY','YEARLY'))` — `PlanType` |
| `price` | **money** `NOT NULL` | |
| `startDate`, `endDate` | `TEXT NOT NULL` | ISO dates; they compare as text, which is what makes `findExpiring` work |
| `status` | `TEXT NOT NULL DEFAULT 'ACTIVE'` | `CHECK (status IN ('ACTIVE','FROZEN','EXPIRED','CANCELLED'))` |
| `frozenDays` | `INTEGER NOT NULL DEFAULT 0` | Written by `freeze`, cleared by `unfreeze` (rule 7) |

**Constraints.** The two `CHECK`s and the FK. There is deliberately **no** `CHECK (endDate >= startDate)`:
the period is `MembershipService.sell`/`renew`'s business, and rule 7 owns how `freeze`/`unfreeze` shift
`endDate`.
**Indexes.** `idx_memberships_customer` (`findByCustomer`), `idx_memberships_status` (`findByStatus`),
`idx_memberships_end` (`findExpiring(int days)`).
**DAO.** `MembershipDao` / `MembershipDaoImpl`.
**Rules.** Rule 7 (freeze/unfreeze, `frozenDays`), rule 13 (`EXPIRED` is derived from `endDate`, never
set by hand — see the open detail in §4 rule 8), rule 14, rule 5 (access needs `ACTIVE` **and**
`endDate >= today`).

#### `workout_programs` — versioned training plans (owner: Yousef)

**Purpose.** One row per program *version*: a new version is a new row, so history is never overwritten.

| Column | Type | Notes |
|---|---|---|
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | |
| `customerId` | `INTEGER NOT NULL` | FK → `customers(id)` |
| `creatorTrainerId` | `INTEGER NOT NULL` | FK → `trainers(id)`; reassignment changes this column only |
| `name` | `TEXT` | |
| `exercises` | `TEXT` | One text field, not a child table (sets/reps stay inside it) |
| `version` | `INTEGER NOT NULL DEFAULT 1` | Assigned by `WorkoutService`; the user never types it |
| `createdDate` | `TEXT` | ISO date |

**Constraints.** `UNIQUE (customerId, version)` — `docs/UML.md` §1b: *"`version` is unique per
customer."* The database, not the service, is what makes a duplicate version impossible.
**Indexes.** The `UNIQUE (customerId, version)` index serves both `findByCustomer` and
`findLatestVersion(int)` (`MAX(version)` for one customer).
**DAO.** `WorkoutProgramDao` / `WorkoutProgramDaoImpl`.
**Rules.** Per-customer versioning (`WorkoutService`); rule 15 for the creator link.

#### `trainer_subscriptions` — paid trainer engagements (owner: Yousef)

**Purpose.** One row per period a customer pays a trainer for, at a flat rate. It records *the paid
period*, not who is assigned to whom (rule 15).

| Column | Type | Notes |
|---|---|---|
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | |
| `customerId` | `INTEGER NOT NULL` | FK → `customers(id)` |
| `trainerId` | `INTEGER NOT NULL` | FK → `trainers(id)` |
| `startDate`, `endDate` | `TEXT NOT NULL` | ISO dates |
| `ratePerPeriod` | **money** `NOT NULL` | Flat rate for the period |
| `status` | `TEXT NOT NULL DEFAULT 'ACTIVE'` | `CHECK (status IN ('ACTIVE','EXPIRED','CANCELLED'))` |

**Constraints.** The `CHECK`s and both FKs. Rule 11's "`trainerId` must equal the customer's assigned
`trainerId`" **cannot** be a `CHECK`: it compares columns in two different tables (`customers`,
`trainer_subscriptions`). Single-row invariants go in the DDL; cross-table invariants go in the service.
**Indexes.** `idx_subs_customer` (`findByCustomer`), `idx_subs_trainer` (`findByTrainer`),
`idx_subs_active` on `(customerId, trainerId, endDate)` for
`findActive(int, int, String)` — "is this customer already subscribed to this trainer?".
**DAO.** `TrainerSubscriptionDao` / `TrainerSubscriptionDaoImpl`.
**Rules.** Rule 11 (covered sessions carry `price = 0`, and only this trainer's sessions can be covered),
rule 13 (`EXPIRED` derived).

#### `trainer_availability` — published hourly slots (owner: Yousef)

**Purpose.** One row per published slot: a trainer may publish several per day, and `maxCustomers` is the
cap **for that slot** (rule 8).

| Column | Type | Notes |
|---|---|---|
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | |
| `trainerId` | `INTEGER NOT NULL` | FK → `trainers(id)` |
| `date` | `TEXT NOT NULL` | ISO date |
| `startTime`, `endTime` | `TEXT NOT NULL` | ISO times; `publishSlot` rejects `startTime >= endTime` via `Validators.validTimeRange` |
| `maxCustomers` | `INTEGER NOT NULL` | `CHECK (maxCustomers > 0)` — a slot with cap 0 could never be booked |
| `status` | `TEXT NOT NULL DEFAULT 'OPEN'` | `CHECK (status IN ('OPEN','FULL','CLOSED'))` |

**Constraints.** The two `CHECK`s and the FK. There is **no** `UNIQUE (trainerId, date)`: that would
forbid several slots per day and contradict rule 8. Whether two slots for one trainer may *start* at the
same time (a `UNIQUE (trainerId, date, startTime)` index) is an **Open detail (decide + log in
`DECISIONS.md`)**.
**Indexes.** `idx_avail_trainer_date` on `(trainerId, date)` — `findByTrainerAndDate` and
`findOpenSlots(int, String)`. `findByTrainerAndDate` returns a **list**, not one row (rule 8).
**DAO.** `TrainerAvailabilityDao` / `TrainerAvailabilityDaoImpl`.
**Rules.** Rule 8 (several slots, per-slot cap, daily cap applied across the day's slots), rule 13
(`FULL` is derived from occupancy and never written by hand), rule 17 (`bookedCount` counts `CONFIRMED`
sessions only, which is a query against `sessions`, not a counter column here).

#### `sessions` — booked training slots (owner: Yousef)

**Purpose.** One row per booked or requested session, with its optional link to the published slot and to
the subscription that covers it.

| Column | Type | Notes |
|---|---|---|
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | |
| `customerId`, `trainerId` | `INTEGER NOT NULL` | FKs → `customers(id)`, `trainers(id)` |
| `date` | `TEXT NOT NULL` | ISO date (`findByDay`) |
| `startTime` | `TEXT NOT NULL` | ISO time |
| `durationMin` | `INTEGER NOT NULL DEFAULT 60` | Rule 1: the default duration is 60 minutes |
| `status` | `TEXT NOT NULL` | `CHECK (status IN ('REQUESTED','CONFIRMED','COMPLETED','NO_SHOW','CANCELLED'))`. **No `DEFAULT`**: the service must set the initial status explicitly (a default would hide a service that forgot) |
| `availabilityId` | `INTEGER` | Nullable FK → `trainer_availability(id)`: `adminReserve` may book outside published slots (rule 1) |
| `subscriptionId` | `INTEGER` | Nullable FK → `trainer_subscriptions(id)` |
| `price` | **money** `NOT NULL DEFAULT '0.00'` | Must be 0 when `subscriptionId` is set (rule 11) |
| `cancelReason` | `TEXT` | Recorded even for a late cancellation (rule 10) |
| `sessionNotes` | `TEXT` | |

**Constraints.** The status `CHECK`, the three FKs, and rule 11's price rule:
`CHECK (subscriptionId IS NULL OR CAST(price AS NUMERIC) = 0)` — or the plain `price = 0` form if money is
stored as `INTEGER` cents. Rule 18's legal transitions are **not** expressible in a `CHECK` (they depend
on the previous value over time), so the state machine stays in `SchedulingService.canTransition`.
**Indexes.** `idx_sessions_date` (`findByDay`), `idx_sessions_customer` on `(customerId, date)`
(`findByCustomerAndDay`), `idx_sessions_trainer` on `(trainerId, date)` (`findByTrainerAndDay`),
`idx_sessions_slot` on `(availabilityId, status)` (`countByAvailability` — pass `CONFIRMED` so cancelling
frees capacity, rule 17).
**DAO.** `SessionDao` / `SessionDaoImpl`.
**Rules.** Rules 1, 10, 11, 17, 18.

#### `attendance` — one check-in record per customer per day (owner: Abdel Raouf)

**Purpose.** The daily attendance log: when a member arrived and left.

| Column | Type | Notes |
|---|---|---|
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | |
| `customerId` | `INTEGER NOT NULL` | FK → `customers(id)` |
| `date` | `TEXT NOT NULL` | ISO date |
| `checkIn` | `TEXT NOT NULL` | ISO time |
| `checkOut` | `TEXT` | **Nullable**: still inside the gym is a real state |

**Constraints.** Only the FK and the `NOT NULL`s. There is deliberately **no** `UNIQUE (customerId, date)`:
rule 5 is enforced by the service through `AttendanceDao.findByCustomerAndDay`, which returns a **list** so
that a stray duplicate is visible rather than silently rejected by the database
(`src/main/java/com/gym/dao/README.md`).
**Indexes.** `idx_attendance_customer_date` on `(customerId, date)` — serves `findByCustomerAndDay` and
`findByCustomer`; `idx_attendance_date` for the daily report (`findByDay`).
**DAO.** `AttendanceDao` / `AttendanceDaoImpl`.
**Rules.** Rule 5 (needs `CustomerStatus.ACTIVE` and a valid `ACTIVE` membership; a second check-in the
same day returns the existing row), rule 19 (never deleted — a mistake is fixed with
`AttendanceService.correctRecord`).

#### `progress_records` — dated body measurements (owner: Abdel Raouf)

**Purpose.** The measurement history a chart is drawn from, with an optional target.

| Column | Type | Notes |
|---|---|---|
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | |
| `customerId` | `INTEGER NOT NULL` | FK → `customers(id)` |
| `date` | `TEXT NOT NULL` | ISO date |
| `weightKg` | `REAL NOT NULL` | A measurement, not money: `REAL` is correct here (`DECISIONS.md` 31 is about money) |
| `bodyFatPct` | `REAL NOT NULL` | Same |
| `measurements` | `TEXT` | Free text (chest, waist, …) |
| `targetWeightKg` | `REAL` | **Nullable** benchmark: `ProgressMetric` selects which measurement `ProgressService.series` returns |
| `notes` | `TEXT` | |

**Constraints.** FK and `NOT NULL`s. `targetWeightKg` must stay nullable — a record without a goal is
normal, and a `0` would draw a false line to the axis.
**Indexes.** `idx_progress_customer_date` on `(customerId, date)` — the series query is "one customer,
ordered by date".
**DAO.** `ProgressDao` / `ProgressDaoImpl` (note the name: `ProgressDao`, for `ProgressRecord`).
**Rules.** Rule 13's `ProgressMetric` decides which column the chart reads; `ChartUtils` formats the
values and never queries.

#### `medical_profiles` — at most one per customer (owner: Abdelrhman)

**Purpose.** Sensitive health data, created on demand and readable by the assigned trainer only.

| Column | Type | Notes |
|---|---|---|
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | |
| `customerId` | `INTEGER NOT NULL UNIQUE` | FK → `customers(id)`; **`UNIQUE` is how rule 3 ("zero or one") is enforced** |
| `bloodType` | `TEXT` | |
| `conditions`, `allergies`, `medications`, `injuries` | `TEXT` | Multi-line text; each may list several entries |
| `doctorName`, `doctorPhone` | `TEXT` | |
| `notes` | `TEXT` | |
| `updatedDate` | `TEXT` | ISO date; shown as "last updated" |

**Constraints.** `UNIQUE (customerId)` plus the FK. The access rule ("only the assigned trainer may
read") is **not** a constraint — it is `MedicalProfileService.getForTrainer`, because it depends on who
is signed in, not on the row.
**Indexes.** The `UNIQUE (customerId)` index serves `findByCustomer`.
**DAO.** `MedicalProfileDao` / `MedicalProfileDaoImpl`.
**Rules.** Rule 3 (created on demand, zero or one), rule 15 (the authoritative trainer link is
`customers.trainerId`), rule 4 (a profile is one of the references that blocks a hard delete —
`GuardService` counts it).

#### `payments` — money in, and how it was corrected (owner: Abdel Raouf)

**Purpose.** One row per payment, linked to exactly one thing it pays for, and never deleted: corrections
are a void plus a new payment (rule 19).

| Column | Type | Notes |
|---|---|---|
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | |
| `customerId` | `INTEGER NOT NULL` | FK → `customers(id)`; must equal the linked object's customer (rule 12) |
| `membershipId` | `INTEGER` | Nullable FK → `memberships(id)` |
| `subscriptionId` | `INTEGER` | Nullable FK → `trainer_subscriptions(id)` |
| `sessionId` | `INTEGER` | Nullable FK → `sessions(id)` |
| `amount` | **money** `NOT NULL` | `BigDecimal` in Java (`DECISIONS.md` 31) |
| `method` | `TEXT NOT NULL` | `CHECK (method IN ('CASH','CARD','TRANSFER'))` — `PaymentMethod` |
| `date` | `TEXT NOT NULL` | ISO date; `revenueBetween(from, to)` compares these strings |
| `receiptNo` | `TEXT NOT NULL UNIQUE` | `UNIQUE` is what makes `findByReceipt` return one row |
| `voided` | `INTEGER NOT NULL DEFAULT 0` | `CHECK (voided IN (0,1))`; revenue queries exclude `voided = 1` |
| `voidReason`, `voidedDate` | `TEXT` | Why and when it was voided |
| `previousAmount` | **money** | Written by `AdminService.adjustPayment` so a receipt can always be reconciled |

**Constraints.** `UNIQUE (receiptNo)`, the `method` and `voided` `CHECK`s, the four FKs, and rule 12's
XOR: `CHECK ((membershipId IS NOT NULL) + (subscriptionId IS NOT NULL) + (sessionId IS NOT NULL) <= 1)`.
In SQLite a comparison is `1` or `0`, so two links sum to `2` and the row is refused — a payment with two
links double-counts revenue. The other half of rule 12 ("exactly one") is enforced in Java by
`PaymentService.validLink`, which rule 12 requires `record` to call: the database can refuse a bad row,
but only the service can tell the user which link to fix. A payment with no link at all is therefore
refused by the service, with a message, before it ever reaches SQLite.
**Indexes.** `idx_payments_customer` (`findByCustomer`), `idx_payments_date` (`revenueBetween`), and
`idx_payments_method` (`findByMethod`). `receiptNo` is already indexed by its `UNIQUE` constraint.
**DAO.** `PaymentDao` / `PaymentDaoImpl`.
**Rules.** Rule 6 (`balance` = unpaid memberships plus uncovered subscriptions and ad-hoc sessions;
voided payments never count), rule 12 (XOR and the matching customer), rule 19 (void, never delete).

---

## 6. How this layer connects to the others

The bootstrap flow, in order:

1. One class chooses the concrete connection: only `MainApp` (or the bootstrap step) names
   `SqliteDbConnection`; everything else depends on the `DbConnection` interface (`DECISIONS.md` 33).
2. `SqliteDbConnection.initializeSchema()` reads `/db/schema.sql` from the classpath and executes it.
   Every statement is `IF NOT EXISTS`, so the second and hundredth run change nothing (rule 3).
3. `getConnection()` opens `gym.db` next to the working directory — creating the file on first use — and
   sets `PRAGMA foreign_keys = ON` for that connection (rule 6).
4. A screen calls a service, the service calls a DAO, and the DAO prepares a statement against these
   tables. The column names in the SQL must match the DDL, because the `ResultSet` mapping is written by
   hand (`src/main/java/com/gym/dao/README.md`).
5. Values come back typed by Java, not by SQLite: **money** is read with
   `rs.getString(...)`/`rs.getInt(...)` and rebuilt as a `BigDecimal`, and every date/time arrives as the
   ISO string the schema stored (`DECISIONS.md` 31).
6. A `SQLException` becomes a `DaoException` with a readable message; the controller shows it through
   `FxUtils.error(...)`. The schema itself never produces a user-facing message.

| This layer | Depends on | Is used by |
|---|---|---|
| `schema.sql` | `docs/UML.md` §1 (fields, types) and rules 4, 9, 12, 13, 19 | `SqliteDbConnection.initializeSchema()`, every DAO query, `GuardService.countReferences` (it counts across these tables) |
| the seed | `docs/UML.md` §1 and the `CHECK` lists in `schema.sql` | the demo/presentation path only — never on the critical path of a real run |
| the money-storage decision | `DECISIONS.md` 31 (`BigDecimal` in Java, no `double`) | the six money columns and every DAO that converts them |

**The seam.** `DbConnection` is the one interface that hides *which* database is behind the DAOs
(`DECISIONS.md` 33). Moving to a client-server database for a multi-branch gym (root `README.md` §9)
means writing a second implementation of that interface and re-expressing this DDL as a migration script —
26 DAO files stay untouched. Nothing in this folder needs to change; the file simply stops being opened by
SQLite.

## 7. Common mistakes and review checklist

| Symptom | Cause | Fix |
|---|---|---|
| `no such table: customers` on the first query | `schema.sql` was never executed, or was executed against another file | Confirm `initializeSchema()` runs before the first DAO call and that `gym.db` is where you think it is |
| The second launch throws `table customers already exists` | A `CREATE TABLE` without `IF NOT EXISTS` | Add `IF NOT EXISTS` to every `CREATE` (rule 3) |
| A foreign key violation never happens, even with a bad id | `PRAGMA foreign_keys = ON` is missing on that connection | Set the pragma in `getConnection()`, not once at startup (rule 6) |
| `IllegalArgumentException: No enum constant …` at read time | The stored text is spelled differently from `docs/UML.md` §1 (`INMAINTENANCE`, `NO-SHOW`, `Active`) | Make the `CHECK` list and the Java enum literals identical, then fix the existing rows |
| Revenue is off by a cent or two | Money was stored as `REAL` | Store **money** as `TEXT` or `INTEGER` cents and keep `BigDecimal` in Java (rule 10) |
| Deleting a customer made their payments disappear | An `ON DELETE CASCADE` was added, so `GuardService` counted zero | Remove the cascade: rule 4 wants the delete to be *refused* while references exist |
| The demo has 40 identical customers | A plain `INSERT` seed ran on every start | `INSERT OR IGNORE` with fixed ids (rule 16) |
| A renamed column breaks one screen only | The DDL and that DAO drifted apart | Change `docs/UML.md` §1, the DDL, the model and the DAO mapping in one commit (rule 15) |
| The list screen is slow but nothing is obviously wrong | The index is on a column the query does not filter, or on a `LIKE '%…%'` search | Index the filtered column; accept that a leading wildcard cannot use an index (rule 13) |

**Review checklist — run this before approving a PR that touches this folder:**

- [ ] Every `CREATE` is `IF NOT EXISTS`; no `DROP`, no `DELETE`, no `UPDATE` anywhere in the schema.
- [ ] No `ON DELETE CASCADE`, and `PRAGMA foreign_keys = ON` is set per connection.
- [ ] Every enum column has a `CHECK` whose literals match `docs/UML.md` §1 exactly.
- [ ] The payment XOR `CHECK` is present, and `receiptNo` is `UNIQUE`.
- [ ] No money column is `REAL`; the money-storage choice, if it was decided, is logged in
      `docs/DECISIONS.md`.
- [ ] Dates are `yyyy-MM-dd`, times are `HH:mm`; no locale format anywhere.
- [ ] Every index is justified by a named DAO method; no index on a `LIKE '%…%'` search column.
- [ ] `attendance` and `payments` have no delete path (rule 19).
- [ ] Any schema change is already in `docs/UML.md` (rule 15), with `DECISIONS.md` if it was a choice.
- [ ] The seed is idempotent, uses fixed ids, and its enums satisfy the `CHECK`s.
- [ ] `gym.db` and its `-wal`/`-shm` files are still ignored; no `.db` file is staged in the commit.

## 8. Learning exercises (with answers)

1. **Read and answer.** Why `INTEGER PRIMARY KEY AUTOINCREMENT` instead of plain
   `INTEGER PRIMARY KEY`?
2. **Write this small piece.** Write the `CHECK` that enforces rule 12 on `payments`, and say what happens
   to an `INSERT` that sets both `membershipId` and `sessionId`.
3. **Find the bug.** A teammate commits this, plus `CREATE INDEX idx_customers_name ON customers(fullName);`:
   `status TEXT NOT NULL CHECK (status IN ('Active','ACTIVE'))`. Name both defects and their symptoms.
4. **Extend without breaking rule 4.** The team wants per-item maintenance history for equipment
   (`DECISIONS.md` 30 says that needs a new entity). List, in order, what must change before a
   `maintenance_records` table may be created.
5. **Decide and justify.** Should the demo seed be executed by `SqliteDbConnection.initializeSchema()` or
   by a separate script a human runs? Give the argument for each side and pick one for a college demo.

<details>
<summary>Answers</summary>

1. `AUTOINCREMENT` guarantees that an id is never reused after a row is deleted (rule 4). Without it,
   SQLite may hand a deleted row's id to the next insert, and a printed receipt ("R-2025-0001 paid by
   customer 3") would then point at somebody else.
2.

```sql
CHECK ((membershipId IS NOT NULL) + (subscriptionId IS NOT NULL) + (sessionId IS NOT NULL) <= 1)
```

In SQLite each comparison evaluates to `1` or `0`, so setting both `membershipId` and `sessionId` sums to
`2` and the `INSERT` is refused with `CHECK constraint failed: payments` — the row never exists, so no
query can double-count revenue. Note what the constraint deliberately does **not** cover: a row with zero
links passes the `CHECK`, and refusing it is `PaymentService.validLink`'s job, because only a service can
answer with a sentence the user understands (rule 12).
3. First defect: the `CHECK` allows the literal `'Active'`, which is not a `CustomerStatus` literal, so a
   row inserted by hand or by a buggy statement makes `Enum.valueOf("Active")` throw at read time — the
   list screen fails with an `IllegalArgumentException` that names the enum, not the row. Second defect:
   `CustomerDao.search` filters with `LIKE '%text%'`, and a leading wildcard cannot use an index, so the
   index only slows every insert and the seed down.
4. In order: (1) add the entity to `docs/UML.md` §1 with its fields, (2) record the scope change in
   `docs/DECISIONS.md` (30 says it is deliberately out of scope for T1), (3) add the model class, (4) add
   the DDL and its index, (5) add the DAO and the service, (6) decide whether a maintenance record blocks a
   hard delete and teach `GuardService.countReferences` about the table (rule 4).
5. For the Java bootstrap: one command, nothing to remember, every teammate gets the same demo data.
   For a script: the shipped app never writes demo rows, and a presentation cannot accidentally seed a
   database someone was using. For a college demo the script wins on honesty and on safety, and the
   `--reset` variant is one line (`delete gym.db`, then run it) — but whichever way the team chooses, it
   must be logged in `docs/DECISIONS.md` before the code relies on it (see the open details in §5).

</details>

## 9. Ownership and tasks

| Task | Owner | Status |
|------|-------|--------|
| `schema.sql` v1 + seed data (Week 1, foundation) | Abdelrhman | To Do |
| `DbConnection` + `SqliteDbConnection` + schema init (Week 1, foundation) — see `src/main/java/com/gym/util/README.md` | Abdelrhman | To Do |

Per-area table specs are published in `src/main/java/com/gym/dao/README.md` → "Week 1 table specs for
`schema.sql`": `medical_profiles`, `administrators`, `trainer_subscriptions` and `trainer_availability`
were the four the team wrote down first; the remaining nine follow the same rule — one column per entity
field in `docs/UML.md` §1.

Rules: `gym.db` is git-ignored, only `schema.sql` is committed; no FK cascades anywhere; enum literals in
`CHECK` lists are spelled exactly as in `docs/UML.md`; every schema change goes through `docs/UML.md`
first.

**`swing` branch.** This folder is the only part of `src/main/resources` that the `swing` branch reuses
**unchanged**: the schema is toolkit-independent, and both apps must create the same 13 tables or they
disagree about the same `gym.db`. The seed is reusable for the same reason. What does *not* transfer is
any Java that reads it through `DbConnection` — the `swing` branch re-implements nothing here, but it must
still depend on the same `DbConnection` interface if the two branches are ever merged. That branch has
**no `.java` files yet**.

**Definition of done for a change in this folder:**

- [ ] `gym.db` deleted, app started, all 13 tables created, app started a second time with no error.
- [ ] Every enum value the app writes appears in the matching `CHECK` list.
- [ ] One `SELECT` per DAO method in `docs/UML.md` §3 runs without an error.
- [ ] The XOR rule refuses a two-link payment (tried by hand, not only in theory).
- [ ] The seed runs twice without changing the row counts.
- [ ] `docs/UML.md` and `docs/DECISIONS.md` updated in the same change if a column or a choice moved.
