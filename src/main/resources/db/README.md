# `db` — The SQLite schema: `schema.sql` and the seed (JavaFX branch)

> ## ⚠️ PLANNED, NOT BUILT
> **This folder contains no `schema.sql` and no seed data — only this README.** The 13-table design
> below is the agreed schema in [`docs/UML.md`](../../../docs/UML.md) §1 (entities) and §3 (DAO
> surface); the SQLite-JDBC dependency has been removed from `build.gradle` until the DAO layer
> lands, and nothing calls `initializeSchema()` (it does not exist yet). Status:
> [`docs/COVERAGE.md`](../../../docs/COVERAGE.md). Do not cite `schema.sql` as if it is committed.

## 1. Purpose — the problem this folder solves

`db/` will hold the committed definition of the database: `schema.sql`, the DDL (**Data Definition
Language** — `CREATE TABLE`, `CREATE INDEX`) that creates the 13 tables (one per entity in
`docs/UML.md` §1), plus the demo seed rows. The database *file* is not here: SQLite is an
**embedded** database, so "the database" is one ordinary file (`gym.db`) that the app will create on
first run in the working directory, excluded by `.gitignore`.

**What breaks while this folder is empty** — exactly what the planned `SqliteDbConnection.
initializeSchema()` would hit: with no DDL to execute, the first `findAll()` fails with
`no such table: customers`, and without a shared column definition each of the 13 DAO
implementations would guess its own column names.

**Why SQLite fits this app.** No server to install, no connection string, no credentials, one file
to copy for a demo, and the whole database is transactional and consistent after a crash. A college
desktop app that runs on one machine at a time gets a real relational database with ACID guarantees
and zero deployment work.

**The limitation, stated honestly.** One SQLite file is readable and writable by one machine at a
time. A gym with several branches that must share live data needs a client-server database later —
recorded as a known limitation in the root `README.md` §9. It is *not* something this folder can fix.

**What this folder deliberately does not do.** It stores no behaviour. A `CHECK` constraint refuses a
row that is already invalid; it never decides what is valid. The decision lives in
`service/` (rules 1–19, planned), and the constraint is the last line of defence.

## 2. What you learn here (OOP concepts)

| Concept | Where it will be practised | Why the project needs it | How to apply it (one line) |
|---|---|---|---|
| Abstraction | the whole schema | A table is the shared contract between 13 DAOs and 13 entities; nobody re-invents a column | Change a column in `schema.sql` and update the entity in the same commit |
| Information hiding | column names and `CHECK` constraints | A caller sees `status = 'ACTIVE'`, never how the row is stored | No query outside a DAO depends on the physical layout |
| Invariants (design by contract) | `NOT NULL`, `CHECK`, `UNIQUE`, FK declarations | A rule the database enforces cannot be broken by a buggy screen at 2 a.m. | Every single-row invariant becomes a constraint |
| Referential integrity | `FOREIGN KEY … REFERENCES` | A session cannot point at a customer who does not exist | Declare the FK — and remember SQLite needs `PRAGMA foreign_keys = ON` to enforce it |
| Type affinity | `TEXT`, `INTEGER`, `REAL` | SQLite has no strict column types; naming the intended type documents intent | Match the Java type: `String` → `TEXT`, enum → `TEXT`, count → `INTEGER` |
| Idempotency | `CREATE TABLE IF NOT EXISTS`, `INSERT OR IGNORE` | The schema and seed will run on every start; running twice must be harmless | Every init statement is a no-op the second time |
| Single source of truth | `schema.sql` (committed) vs `gym.db` (runtime) | A committed database overwrites a teammate's demo data forever | Version the definition, ignore the data |

## 3. Design patterns in this layer

| Pattern | Where | Why | What goes wrong if used carelessly |
|---|---|---|---|
| Schema-as-code (checked-in DDL) | `schema.sql` | The table definition is reviewable in a diff and identical on every machine | Hand-editing `gym.db` in a browser tool creates an unreproducible schema |
| Idempotent bootstrap | `IF NOT EXISTS` + `INSERT OR IGNORE` | `initializeSchema()` may run on every start | A `DROP TABLE` "to be safe" deletes the demo database during a presentation |
| Assertion through constraints | `CHECK` (rule 12's XOR), `NOT NULL`, `UNIQUE` | The XOR rule is enforced even if a future service forgets to call `validLink` | A constraint that *contradicts* a service rule is a bug; one that duplicates it is defence in depth |
| Guarded delete (no cascades) | the absence of `ON DELETE CASCADE` | Rule 4's admin hard delete must stop while references exist (`GuardService.countReferences`) | A cascade deletes the evidence and the guard's count silently becomes zero |
| ORM / migration framework | **rejected** | 13 tables and one developer per area: an ORM adds a dependency; a migration tool adds a version table nobody would read | Introducing either later is a decision to log, not a quiet addition |

## 4. Rules you must follow (and the bug each rule prevents)

1. **`schema.sql` is committed; `gym.db` never is.** `.gitignore` lists `gym.db`, `gym.db-*` and
   `*.db`. A committed database produces a binary merge conflict nothing can resolve.
2. **`gym.db` is created at runtime in the working directory** — never inside
   `src/main/resources`, which would ship a stale database inside the jar.
3. **Every init statement is idempotent** — the schema and seed must be no-ops on the second run.
4. **One table per entity in `docs/UML.md` §1; the entity is the column list's authority.** A column
   that exists only in SQL has no reader; one that exists only in Java has no storage.
5. **Boxed-Java ↔ nullable-SQL:** a nullable column (`Integer` in `UML.md` §1) has no `NOT NULL`; a
   primitive-backed column does.
6. **Money columns are `TEXT`/`NUMERIC` holding `BigDecimal`'s `String` form** — never a `REAL`
   `double` (`DECISIONS.md` 31).
7. **Enums are stored as their `name()`** — readable, diffable, and never `ordinal()`.
8. **No FK cascades.** Deletes are guarded (rule 4) so reference counts are honest.

## 5. How this folder connects to the others

| Will feed | How |
|---|---|
| `util/SqliteDbConnection` (planned) | executes `schema.sql` + the seed idempotently on first run |
| `dao/*DaoImpl` (planned) | every query's column names come from here |
| `model/` (planned) | entity fields mirror columns 1:1 (`UML.md` §1 is the shared authority) |
| root `README.md` §5 | documents where `gym.db` appears and why it is ignored |

When the backend starts, the order is fixed: **`schema.sql` first**, then `DbConnection` (which can
finally run it), then the DAO pairs. The seed rows replace the demo data currently hard-coded in the
controllers, screen by screen.

## 6. Learning exercises

1. Name the three things that break if `gym.db` is committed by accident, and the `.gitignore` lines
   that prevent each.
2. Rule 12's XOR (exactly one of membership/subscription/session on a payment) — draft the `CHECK`
   constraint that enforces it, and explain why the service still calls `validLink`.
3. Why does SQLite need `PRAGMA foreign_keys = ON`, and where would the app turn it on?
4. A teammate proposes `DROP TABLE IF EXISTS` before every `CREATE TABLE`. Give the demo-day scenario
   this ruins, and the idempotent alternative.
5. Explain why a `REAL` column for money quietly corrupts a revenue report, and what to store instead.

## 7. Ownership

| Planned file | Owner |
|---|---|
| `schema.sql` + seed rows (13 tables per `docs/UML.md` §1) | Abdelrhman (foundation track, see `docs/UML_TEAM.md`) |

Status: **planned, not built** — track in `docs/COVERAGE.md` → "`schema.sql` + seed data".
