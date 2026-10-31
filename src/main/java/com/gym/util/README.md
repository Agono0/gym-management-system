# `util` — Shared Helpers (JavaFX branch)

> ## ⚠️ PLANNED, NOT BUILT
> **This folder contains no `.java` files — only this README.** The 10 helpers below are the agreed
> design in [`docs/UML.md`](../../../../docs/UML.md) §6 (each stereotyped `«utility»` there). The
> shipped app has no database, no session store and no hashing yet — screens answer inline in demo
> mode. Status: [`docs/COVERAGE.md`](../../../../docs/COVERAGE.md). Do not cite a helper as if it
> compiles.

## 1. Purpose — the problem this layer solves

`util` will hold the cross-cutting helpers that belong to no single layer. Each one removes a
decision that would otherwise be made again, differently, in every class that needs it: how to open
the database, what to throw when JDBC fails, what a valid date or phone number looks like, how to add
a month to a membership, who is signed in, how a password is hashed, how a message reaches the user,
how progress numbers become a chart series, and whether a row about to be hard-deleted is still
referenced. Utilities sit **under** the four layers (`UML.md` §2 draws them as
`UTIL -. supports .->`): services, DAOs and controllers call them; they call nobody back.

**What breaks if this layer never exists.** Without `DbConnection` there is no seam, so each DAO
would name `SqliteDbConnection` and a database change edits 26 files (`DECISIONS.md` 33). Without
`DaoException` the checked `SQLException` enters every JavaFX event handler — which cannot declare
checked exceptions. Without `DateUtils` two classes format dates differently and `findByDay` silently
returns nothing. Without `PasswordUtils` the fastest thing to write is plaintext in `passwordHash`.
Without `GuardService` the admin hard delete orphans rows (rule 4). The **shipped demo** already
feels the smaller cousins of these costs: each screen owns its own date formatting and inline
validation.

**What this layer deliberately does not do.** No business rules — a validator answers "is this a
date?", never "may this membership be frozen?" (that is `MembershipService`, rule 7). No entity
definitions — only `Administrator`/`StaffRole` (`SessionContext`) and `ProgressRecord` (`ChartUtils`)
appear in signatures. No general SQL — only `SqliteDbConnection` (DDL) and `GuardService` (one
reference count) touch JDBC. No JavaFX except `FxUtils`.

## 2. What you learn here (OOP concepts)

| Concept | Where it will be practised | Why the project needs it | How to apply it (one line) |
|---|---|---|---|
| Static vs instance members | every file except `DbConnection` | A helper has no state, so an instance would be a lie; `SessionContext` is the one place where static *is* state | `static` members; stereotype the class `«utility»` (`DECISIONS.md` 37) |
| Information hiding | `private` constructor per utility; `private static Administrator current` | A private constructor documents "never instantiated"; a private field is what makes `requireRole` a gate | Close the constructor; expose behaviour |
| Abstraction + dependency inversion | `DbConnection` interface | DAOs need "a way to get a `Connection`", not "SQLite"; the concrete class is chosen once (`DECISIONS.md` 33) | Type fields as the interface |
| Exception design | `DaoException` (`DECISIONS.md` 40) | One unchecked wrapper keeps checked SQL out of signatures, cause intact | `extends RuntimeException`, `super(message, cause)`, no fields |
| Checked → unchecked translation | the DAO `catch` boundary | The UI catches one type; the log keeps the cause | Translate where the JDBC call is |
| Encapsulated global state | `SessionContext` (`DECISIONS.md` 34) | `FXMLLoader` needs no-arg controllers, so the account cannot be injected | FX-thread confined; clear on logout **and** exit |
| Immutable/defensive returns | `ChartUtils` series, `List.of()` | Helpers must not corrupt callers or be corrupted | Copy before sorting; empty list, never `null` |
| Mutable secrets we own | `PasswordUtils` (`char[]`, `PBEKeySpec`) | `String` cannot be wiped | Clear in `finally`; minimal `String` copies |
| Exception-free validation | `Validators` | A boolean answer leaves the message with the service | Primitive `boolean`; never throw for a normal invalid input |
| Composition over inheritance | every helper `final` | No project class extends another (`UML.md` §7) | Compose; never extend a utility |

## 3. Design patterns in this layer

| Pattern | Where | Why | What goes wrong if used carelessly |
|---|---|---|---|
| Utility class (static holder) | `Validators`, `DateUtils`, `PasswordUtils`, `ChartUtils`, `GuardService` | Pure functions over parameters; state would have to be invented | It becomes a dumping ground nobody owns |
| Facade over the UI toolkit | `FxUtils` | Twenty controllers must produce the same dialog with the same M3 look | Business logic creeps in; toolkit types leak into signatures |
| Interface + single implementation (a seam) | `DbConnection` / `SqliteDbConnection` (`DECISIONS.md` 33) | A database swap or an in-memory test SQLite touches one file | A "temporary" second implementation appears and DAOs import it directly |
| Strategy — available, not exercised | any `DbConnection` reference | The variation point is real with one strategy today | `if (db instanceof SqliteDbConnection)` |
| Context object (documented global) | `SessionContext` (`DECISIONS.md` 34) | The one static that *is* state, and why | It grows into a general cache |
| Singleton / Template Method — rejected | none | Statics need no singleton; the DAO layer stays flat (`DECISIONS.md` 36) | Locking for a lazily made singleton that tests no better |
| Self-describing credential string | `PasswordUtils.hash` returns one string | One `passwordHash` field, so salt + iterations travel inside it | A fixed iteration count invalidates every stored hash the day it is raised |

## 4. Rules you must follow (and the bug each rule prevents)

1. **`DbConnection` is an interface, and exactly one place names the concrete class** — the bootstrap
   (`GymApp`/the shell, once the backend lands). Prevents a database change touching 26 files.
2. **`DaoException` is the only type that crosses the DAO boundary**, cause chained.
3. **`DateUtils` owns every parse and format** of `yyyy-MM-dd` / `HH:mm` (rule 9) — a second formatter
   is how `findByDay` starts silently returning nothing.
4. **`Validators` return `boolean` and never throw**; the service that called them owns the message.
5. **`SessionContext` is FX-thread confined, cleared on logout and on exit** (`DECISIONS.md` 34) — a
   surviving session on a shared reception PC is a security bug.
6. **`PasswordUtils` never stores or logs plaintext**; PBKDF2 with per-hash salt and iterations
   (`DECISIONS.md` 38) — no new dependency.
7. **`GuardService` is the single reference counter** behind the admin hard delete (rule 4).
8. **No business rule, no JavaFX (except `FxUtils`), no static mutable state** beyond
   `SessionContext`.

## 5. How this layer connects to the others

| Will depend on | Will be used by |
|---|---|
| `java.sql`/`javax.crypto` only; no project layer above it | `dao/` (`DbConnection`, `DaoException`), `service/` (`Validators`, `DateUtils`, `GuardService`, `PasswordUtils`), `controller/` (`FxUtils`, `SessionContext`, `ChartUtils`) |

When the backend starts: `DbConnection` + `DaoException` first (the DAOs need them), then
`Validators`/`DateUtils` (the services), then `PasswordUtils` + `SessionContext` + `FxUtils` (the
login and role gates), then `ChartUtils`/`GuardService`. The demo controllers' inline validation and
feedback code shows what each helper must cover.

## 6. Learning exercises

1. Why is `SessionContext` allowed to be static mutable state while a static `Customer` cache is not?
   Cite the decision that documents the exception.
2. Write the `DaoException` class (three lines) and explain why the cause must be chained.
3. A teammate formats a date with `SimpleDateFormat("dd/MM/yyyy")` inside a controller. Which rule
   breaks, and what does `findByDay` do afterwards?
4. Why must `Validators.validEmail` return `boolean` instead of throwing with a nice message?
5. Design the self-describing hash string for `PasswordUtils.hash`: which fields must it embed and
   why must the iteration count travel with the hash?

## 7. Ownership

| Planned file group | Owner |
|---|---|
| 10 helpers (per `docs/UML_TEAM.md` split) | per-feature owners (see `docs/UML_TEAM.md`) |

Status: **planned, not built** — track in `docs/COVERAGE.md` → "Backend domain services and SQLite DAOs".
