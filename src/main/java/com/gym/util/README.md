# `util` — Shared Helpers (JavaFX branch)

> **Signature authority:** `docs/UML.md` §6 owns every name, type and signature in this folder; §2 owns
> the layer rules and §3 the `DbConnection` seam; the "Business rules" table owns rules 4, 5, 7, 9, 10,
> 14 and 16 that these helpers make implementable. `docs/DECISIONS.md` 31, 33, 34, 37, 39 and 40 record
> *why*. Ownership is in `docs/UML_TEAM.md`; the rules this file follows are in `docs/DOCS_STANDARD.md`.
>
> This README teaches what each helper is for, which alternative we rejected and why, how to write it,
> how to verify it and what breaks it. Code here is an **illustrative sketch, not the specification** —
> if a sketch and `UML.md` disagree, `UML.md` wins.
>
> **Status: every file in this folder is planned.** `src/main/java/com/gym` currently contains
> `MainApp.java` (a stub that loads `main.fxml`) and the layer READMEs.

## 1. Purpose — the problem this layer solves

`util` holds the cross-cutting helpers that belong to no single layer. Each one removes a decision that
would otherwise be made again, differently, in every class that needs it: how to open the database, what
to throw when JDBC fails, what a valid date or phone number looks like, how to add a month to a
membership, who is signed in, how a password is hashed, how a message reaches the user, how progress
numbers become a chart series, and whether a row about to be hard-deleted is still referenced.
Utilities sit **under** the four layers (`UML.md` §2 draws them as `UTIL -. supports .->`): services,
DAOs and controllers call them; they call nobody back.

**What breaks if this layer is missing.** Without `DbConnection` there is no seam, so each DAO names
`SqliteDbConnection` and a database change edits 26 files (`DECISIONS.md` 33: 13 DAO interfaces + 13
impls). Without `DaoException`, the checked `SQLException` enters every DAO signature and every JavaFX
event handler — which cannot declare checked exceptions — stops compiling. Without `DateUtils` two
classes format dates with two patterns and `SessionDao.findByDay` silently returns nothing. Without
`PasswordUtils` the fastest thing to write is plaintext in `passwordHash`. Without `GuardService` the
admin hard delete orphans rows, because `schema.sql` uses no FK cascades (rule 4, `DECISIONS.md` 39).
Without `ChartUtils` the progress screen either duplicates DAO access or cannot build a series.

**What this layer deliberately does not do.** No business rules: a validator answers "is this a date?",
never "may this membership be frozen?" (that is `MembershipService`, rule 7). No entity definitions —
only the three model types §6 puts in a signature are imported: `Administrator` and `StaffRole`
(`SessionContext`), `ProgressRecord` (`ChartUtils`). No general SQL: only `SqliteDbConnection` (DDL) and
`GuardService` (one reference count) touch JDBC. No JavaFX except `FxUtils`. No user-facing wording for a
database problem: the controller owns that.

## 2. What you learn here (OOP concepts)

Concept names follow the `docs/LEARNING_GUIDE.md` "OOP concept index" so the two documents cross-link.

| Concept | Where in this folder | Why the project needs it | How to apply it (one line) |
|---|---|---|---|
| Static vs instance members | Every file except `DbConnection` | A helper has no state, so an instance would be a lie; `SessionContext` is the one place where static *is* state | Write `static` members; mark them `$` and stereotype the class `«utility»` (`DECISIONS.md` 37) |
| Information hiding | `private` constructor in each utility; `private static Administrator current` | A private constructor documents "never instantiated"; a private field is what makes `requireRole` a gate instead of a suggestion | Close the constructor, expose behaviour, keep the data private |
| Abstraction and interfaces | `DbConnection` | DAOs need "a way to get a `Connection`", not "SQLite" | Declare the two methods DAOs call; let one class implement them |
| Dependency inversion | DAO constructors take `DbConnection` | The dependency points at the abstraction, so the concrete class is chosen once (`DECISIONS.md` 33) | Type the field as the interface, never as `SqliteDbConnection` |
| Polymorphism | `DbConnection db = new SqliteDbConnection();` | The substitution point exists even though one implementation ships — say that honestly instead of claiming a swap we have not done | Program to the reference type; a second implementation changes no DAO |
| Exception design | `DaoException` | One unchecked wrapper keeps checked SQL out of signatures and keeps the original error as a cause (`DECISIONS.md` 40) | `extends RuntimeException`, `super(message, cause)`, no fields |
| Checked → unchecked translation | DAO `catch (SQLException e) { throw new DaoException(...); }` | The boundary is one place, so the UI catches one type and the log keeps the cause | Translate where the JDBC call is; never let `SQLException` escape the DAO |
| Encapsulation | `SessionContext` | Controllers read the session; only `setAdministrator`/`clear` write it | No public field, no setter for the role gate |
| Documented static mutable state, thread-confined | `SessionContext.current` | The account must be reachable from every controller, and `FXMLLoader` cannot inject constructor parameters (`UML.md` §7) | Keep it FX-thread confined; clear it on logout **and** exit (`DECISIONS.md` 34) |
| Immutability and defensive copying | `ChartUtils.weightSeries`, `List.of()` returns | Helpers must not corrupt their callers, or be corrupted by them | Copy a collection before sorting; return an empty list, never `null` |
| Mutable secrets we own | `PasswordUtils` (`char[]`, `PBEKeySpec`) | `String` cannot be wiped; the derived-key path can at least be cleared | Clear what you own in a `finally`; never copy a password into more `String`s |
| Exception-free validation | `Validators` | A boolean answer leaves the message with the service | Return primitive `boolean`, never `Boolean`; never throw for a normal invalid input |
| Composition over inheritance | Every helper is `final` | No project class extends another (`UML.md` §7, `DECISIONS.md` 36) | Compose a call into a service; do not extend a utility |
| Constants with a reason | `DB_PATH`, `ITERATIONS`, `ALGORITHM` | One place to change a value that is wrong in two places if duplicated | `private static final`, with a comment saying what changing it costs |

## 3. Design patterns in this layer

| Pattern | Where | Why this pattern | What goes wrong if used carelessly |
|---|---|---|---|
| Utility class (static method holder) | `Validators`, `DateUtils`, `PasswordUtils`, `ChartUtils`, `GuardService` | These are pure functions over their parameters; state would have to be invented | It becomes a dumping ground nobody owns |
| Facade over the UI toolkit | `FxUtils` | Sixteen controllers must produce the same dialog with the same M3 look | Business logic creeps in, or a toolkit type leaks into a signature |
| Interface + single implementation (a seam) | `DbConnection` / `SqliteDbConnection` (`DECISIONS.md` 33) | A database swap, or an in-memory SQLite for tests, touches one file | A "temporary" second implementation appears and DAOs import it directly |
| Strategy — available, not exercised | Any code holding a `DbConnection` reference | The variation point is real with one strategy today | Pretending it is used, then adding `if (db instanceof SqliteDbConnection)` |
| Exception translation | `DaoException` (`DECISIONS.md` 40) | JDBC throws checked `SQLException`; the app wants one unchecked type with the cause intact | Wrapping without the cause, or catching your own wrapper and carrying on |
| Context object (documented global) | `SessionContext` (`DECISIONS.md` 34) | `FXMLLoader` needs a public no-arg controller constructor, so the account cannot be injected | It grows into a general cache and leaks between screens |
| Singleton — **avoided** | Not used here | A `getInstance()` adds a lifecycle question when the members are static anyway | A lazily created singleton needs locking and still tests no better than a static call |
| Template Method — **rejected** | The 13 `*DaoImpl` stay flat (`UML.md` §7, `DECISIONS.md` 36) | DAOs vary in SQL and entity type, not structure; `DbConnection` is the shared seam | An `AbstractJdbcDao<T>` hides the 13 explicit `..> DbConnection` edges that make wiring checkable |
| Self-describing credential string | `PasswordUtils.hash` returns one string | `Administrator` has one `passwordHash` field, so salt and iteration count must travel inside it | A fixed iteration count in code invalidates every stored hash the day it is raised |

## 4. Rules you must follow (and the bug each rule prevents)

Each rule states the failure it prevents. Quote the number when you reject a change in review.

1. **`DbConnection` is an interface, and exactly one place names the concrete class** — the bootstrap
   (`MainApp`, or a step it calls). *Prevents:* a database change touching 26 files (`DECISIONS.md` 33),
   and two DAOs pointing at two different database files.
2. **Every class here except `FxUtils` stays free of JavaFX imports.** *Prevents:* the `swing` branch
   rewriting helpers it should reuse, and tests of `Validators`/`DateUtils` needing a JavaFX toolkit.
3. **`SessionContext` is the single documented piece of static mutable state.** Keep it FX-thread
   confined; clear it on logout **and** on application exit. *Prevents:* the next user inheriting the
   previous user's `StaffRole` (`DECISIONS.md` 34, rule 16).
4. **Utility classes are `final`, stateless, and have a private constructor; their members are
   `static`.** *Prevents:* an instance implying state that does not exist, and a subclass overriding a
   helper so two call sites silently disagree.
5. **Mark static members `$` and stereotype the class `«utility»` in the `UML.md` source.**
   *Prevents:* a reader of the rendered diagram mistaking a static helper for an instance method —
   Mermaid strips `$` (`DECISIONS.md` 37).
6. **`DateUtils` owns every date and time parse and format.** Dates are `yyyy-MM-dd`, times are `HH:mm`
   (rule 9, `DECISIONS.md` 31). *Prevents:* two formatters disagreeing, which shows up as `findByDay`,
   `findExpiring` or `revenueBetween` returning nothing for a day that clearly has rows.
7. **Never feed a display-formatted date back into SQL.** "Lexicographic order equals chronological
   order" holds only for zero-padded ISO-8601 (`2025-03-12`); `12 Mar 2025` matches no stored value and
   sorts by day-of-month first. *Prevents:* empty result sets that look like data loss.
8. **Money is `BigDecimal`, never `double`, and money never passes through `ChartUtils` or
   `Validators`.** *Prevents:* `0.1 + 0.2` drift in a revenue total (`DECISIONS.md` 31).
9. **`DaoException` is unchecked, chains through `super(message, cause)`, and declares no fields.**
   *Prevents:* `getMessage()` returning `null`, `getCause()` returning `null`, and a stack trace with no
   "Caused by:" line — the exact bug `DECISIONS.md` 40 fixes.
10. **Validators return primitive `boolean` and never throw for a normally invalid input.**
    *Prevents:* `Boolean` unboxing NPEs at the call site, and exceptions used for control flow.
11. **`PasswordUtils` never stores or returns plaintext, salts per user with `SecureRandom`, and
    compares with `MessageDigest.isEqual`.** *Prevents:* rainbow-table recovery, one crack unlocking
    every account that reused a password, and a timing side channel that reveals a hash byte by byte.
12. **`FxUtils` takes plain `String`s** — never a `DaoException`, an entity or a `BigDecimal`.
    *Prevents:* the UI layer depending on the persistence layer, and each caller losing control of the
    wording it shows (`UML.md` §2 layer rules).
13. **`ChartUtils` formats only: no DAO access, no SQL, no JavaFX, no money**, and
    **`GuardService.countReferences` fails closed and whitelists its `table` argument against
    `sqlite_master`** (a table name cannot be a bind parameter, so an unchecked one is both a runtime
    error and an injection point). *Prevents:* the progress screen bypassing `ProgressService`, a class
    that needs a database to be tested, a delete proceeding because the check threw, and SQL built from
    a caller's string.
14. **`util` must not import `service` or `controller`** and imports `model` only where §6 puts a model
    type in a signature; **a detail this folder leaves open is logged in `docs/DECISIONS.md`, not
    settled in a PR comment** (entries below mark those places **Open detail**). *Prevents:* a
    dependency cycle, and two owners choosing differently while the docs describe neither.

## 5. The files in this folder

Ten files, one entry each, in **write order**: `DaoException` (nothing to depend on) → `DbConnection`
(the seam DAO constructors are typed against) → `SqliteDbConnection` (needs `schema.sql`) → `Validators`
(rules 8, 9 checks) → `DateUtils` (rules 7, 9, 10) → `GuardService` (rule 4) → `SessionContext`
(rule 16) → `PasswordUtils` (the login path) → `FxUtils` (controllers) → `ChartUtils` (the progress
screen). Nothing blocks on a class that comes later, and the first three unblock every DAO owner in
Week 1. The table is the class table this README has always carried, kept here so the owner sits next to
the entry; the status column is §9.

| Class | Responsibility | Owner |
|-------|----------------|-------|
| `DbConnection.java` | Interface: `getConnection()`, `initializeSchema()` | Abdelrhman |
| `SqliteDbConnection.java` | SQLite implementation; `gym.db` bootstrap, schema init | Abdelrhman |
| `DaoException.java` | Unchecked `SQLException` wrapper, **`extends RuntimeException`** so it chains via `super(message, cause)` and declares no `message`/`cause` fields of its own | Abdelrhman |
| `Validators.java` | Phone/email/number/date and time-range validation. Returns primitive `boolean`, never `Boolean`. | Abdelrhman |
| `SessionContext.java` | Holds the logged-in `Administrator` for role checks; `setAdministrator`/`getAdministrator`/`requireRole`/`clear` | Abdelrhman |
| `PasswordUtils.java` | PBKDF2 password hashing and verification (Java built-in, no new deps) | Abdelrhman |
| `FxUtils.java` | Dialogs, alerts, scene switching, M3 snackbar/toast helper. The only JavaFX-aware utility. Takes plain strings, so it never sees `DaoException`. | Abdelrhman |
| `DateUtils.java` | Membership periods, session slot math, hours-until check, formatting | Yousef |
| `ChartUtils.java` | Progress-chart dataset formatting. Takes already-fetched values; it does no DAO work and no money. | Abdel Raouf |
| `GuardService.java` | `countReferences(table, id)` — the reference check behind the admin delete rule. Reads which tables reference a row and counts them. | Abdelrhman |

### Two things to know before writing these

**`GuardService` is what makes business rule 4 implementable.** Deleting a customer requires checking
memberships, sessions, payments, attendance, progress, medical profile, workout programs,
subscriptions and availability. A `countByCustomer` on every DAO would add ~26 near-identical
methods; one helper that reads which tables reference the row keeps the DAOs to their normal CRUD
shape. See `DECISIONS.md` 39.

**`$` in the diagram does not render.** Mermaid silently strips the static marker, so the diagrams
stereotype these classes `«utility»` instead. Keep the members `static` and mark them `$` in the
source anyway — the marker stays visible in the raw diagram text. See `DECISIONS.md` 37.

### `DaoException.java` — the one unchecked wrapper for JDBC failures (owner: Abdelrhman)

**Status:** planned. **Canonical:** `docs/UML.md` §6 (the single constructor), §7 (its `--|>
RuntimeException` generalization); `DECISIONS.md` 40.

**What it is.** The exception every `*DaoImpl` throws when JDBC fails: a message a human can read plus
the original `SQLException` as its cause. It is the only exception type the controller layer knows about
from persistence, and `UML.md` §2 fixes how it is shown — `FxUtils.error(getMessage())`.

**Members.**

| Member | Signature | Why this shape |
|---|---|---|
| Constructor | `DaoException(String message, Throwable cause)` | The only way to build one: a message **and** the cause. No message-only constructor exists, so a caller cannot drop the cause |
| `getMessage()`, `getCause()`, `printStackTrace()` | inherited from `Throwable` | They return what `super(message, cause)` stored; overriding `getMessage()` is how the first version broke |
| Fields | none | `DECISIONS.md` 40 is explicit: no fields of its own |

**Why it is designed this way.** JDBC throws the **checked** `SQLException`. A DAO declaring
`throws SQLException` pushes that declaration into 13 interfaces, every service and every controller —
and `EventHandler<ActionEvent>.handle` cannot declare checked exceptions, so the sixteen screens would
not compile without their own `try`/`catch`. Translating once, in the DAO, keeps signatures clean and
puts the single `catch (DaoException e)` where a message can be worded (`UML.md` §2 rule 3).

`extends RuntimeException` is one half of `DECISIONS.md` 40; the other half is the *absence* of fields.
The first version declared `private String message` and `private Throwable cause`. That shadowing is
expensive: `getMessage()` returns the shadow, and `printStackTrace()` walks `Throwable.cause`, which is
still `null` — so the log shows a `DaoException` with no "Caused by:" line and the SQL error text is
gone. Passing both to `super` gives chaining, stack traces and `initCause` for free. Rejected:
**not wrapping** (leaks `SQLException` everywhere) and a **checked** `DaoException` (same plumbing cost
as `SQLException`, no benefit).

**How to write it.**

1. Create `src/main/java/com/gym/util/DaoException.java`, `package com.gym.util;`.
2. Import nothing; `SQLException` appears only in the Javadoc as `{@link java.sql.SQLException}`.
3. Declare `public class DaoException extends RuntimeException` — public so every layer can catch it,
   unchecked so handlers compile.
4. Write the one constructor with `super(message, cause);` as its only statement. No fields, no copy.
5. Javadoc the contract: unchecked wrapper; the message is safe to show; the original is `getCause()`;
   the message must not contain SQL text or parameter values.
6. Add nothing else — no `serialVersionUID`, no second constructor, no factory, no override. Each is a
   `UML.md` §6 change first.

**Sketch.**

```java
package com.gym.util;

/**
 * Unchecked wrapper for the checked {@link java.sql.SQLException} JDBC raises.
 *
 * <p>Declares no fields: message and cause live in Throwable, so getMessage(), getCause() and
 * printStackTrace() all work (DECISIONS.md 40). The message must stay safe to show in a dialog.
 */
public class DaoException extends RuntimeException {

    // Sketch - the only member in docs/UML.md 6 is this constructor.
    public DaoException(String message, Throwable cause) {
        super(message, cause);   // chains: printStackTrace() prints "Caused by: java.sql.SQLException: ..."
    }
}
```

**How to know it is right.** Throw one in a scratch `main` with a `java.sql.SQLException("no such table:
customers")` cause inside a `try`/`catch (RuntimeException e)`, then assert four facts: `getMessage()`
is your sentence; `getCause()` is a `SQLException`; `getCause().getMessage()` still says `no such
table`; the printed trace has two frames with a `Caused by:` line. Then grep `src/main/java/com/gym/dao`
for `throws SQLException` — it must find nothing, which is what this class makes possible.

**Common mistakes.**

- **Declaring `message`/`cause` fields.** Tests that only check `getMessage()` pass while the cause
  disappears from the log.
- **Extending `Exception`.** Every DAO, service and FXML handler then has to declare or catch it.
- **`new DaoException(e.getMessage())`.** The trace points at the DAO line instead of the JDBC line, so
  a locked database looks like a typo in SQL.
- **Using it for validation.** Services throw `IllegalArgumentException`/`IllegalStateException` with a
  message safe to show (`../service/README.md`).
- **Putting SQL, a table name or member data in the message.** It reaches a dialog through
  `FxUtils.error`.

### `DbConnection.java` — the seam that keeps SQLite in one file (owner: Abdelrhman)

**Status:** planned. **Canonical:** `docs/UML.md` §6 (the interface), §3 (interface, 13 `..> DbConnection`
edges, the `SqliteDbConnection <|.. DbConnection` realization), §2 (layer diagram); `DECISIONS.md` 33.

**What it is.** A two-method interface — "give me a connection" and "make sure the schema exists" — that
every DAO is typed against. One class implements it today. DAOs never name that class, so the choice of
database is a single line in the bootstrap.

**Members.**

| Member | Signature | Why this shape |
|---|---|---|
| `getConnection` | `Connection getConnection()` | Hands out a `java.sql.Connection` the **caller owns** and closes with try-with-resources. The interface exposes no connection state, so it cannot hand out something shared |
| `initializeSchema` | `void initializeSchema()` | Schema creation is a database concern; it lives beside the connection so the bootstrap calls one object and a test database is created the same way |
| Implementations | `SqliteDbConnection` (only one) | The realization edge is drawn from the implementer side |
| Used by | all 13 `*DaoImpl` | Each declares `..> DbConnection : uses`; those 13 edges are the wiring check |

**Why it is designed this way.** `DECISIONS.md` 33: DAO impls depend on the interface, not on the SQLite
factory, so a database change touches one file instead of twenty-six. The alternative — each `*DaoImpl`
building its own `SqliteDbConnection` — puts the JDBC URL in 26 places; the day the file moves or a test
needs an in-memory database, someone edits 26 files and misses two.

It is an **interface, not an abstract class**, because there is no shared implementation to inherit and
`UML.md` §7 records that no project class extends another; an abstract base would also invite the
`AbstractJdbcDao<T>` template that `DECISIONS.md` 36 rejects. It has **no `close()`**, and that is not an
omission: it tells you the lifetime. `getConnection()` returns a fresh connection that the DAO closes at
the end of its `try`-with-resources block, so one failed query cannot poison the next and nothing needs
a shutdown hook. Caching a connection inside the implementation would require the interface to expose a
way to close or invalidate it (a `UML.md` §6 change), and one shared SQLite connection would serialise
every query in the app.

**Where the concrete class is chosen.** In the bootstrap — `MainApp.start(...)` or a step it calls:
create one `SqliteDbConnection`, call `initializeSchema()`, then pass that object to every DAO
constructor as `DbConnection`. No DAO may ever see the word `SqliteDbConnection`.

**How to write it.**

1. Create `src/main/java/com/gym/util/DbConnection.java`, `package com.gym.util;`, and Javadoc the
   ownership contract ("the caller closes the connection").
2. Import `java.sql.Connection` and `java.sql.SQLException` only — no `javafx.*`, because DAOs implement
   and call this.
3. Declare `public interface DbConnection`, then the two methods with the §6 names and return types.
4. Settle the failure contract once, in the Javadoc: Mermaid cannot render a `throws` clause, so §6 shows
   none. Recommendation: both methods declare `throws SQLException` (mirroring
   `DriverManager.getConnection`), which keeps the translation point inside the DAO's `catch` where
   `UML.md` §2 puts it. Catching inside `SqliteDbConnection` and throwing `DaoException` changes no name
   in §6 and is also legal — pick one and apply it to both methods.
5. Add nothing else: no `close()`, `isOpen()`, `getUrl()`, static factory or default method. Each is a
   `UML.md` §6 change first.

**Sketch.**

```java
package com.gym.util;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * The persistence seam (DECISIONS.md 33): DAOs are typed against this, so exactly one place in the
 * app names a concrete database.
 *
 * <p>Ownership: the caller owns the returned connection and closes it, normally with
 * try-with-resources. Mermaid cannot render a throws clause, so the checked SQLException below is
 * invisible in UML.md 6 - it is our choice, applied to both methods.
 */
public interface DbConnection {

    // Sketch - the two members are authoritative in docs/UML.md 6.
    Connection getConnection() throws SQLException;

    void initializeSchema() throws SQLException;
}
```

**How to know it is right.** `grep -rn "SqliteDbConnection" src/main/java/com/gym/dao` must return
nothing, and `grep -rn "new SqliteDbConnection" src/main/java/com/gym` exactly one line, in `MainApp` or
its bootstrap step. Check that no DAO stores a `Connection` in a field — such a field outlives the
method that got it, which fights the ownership contract. Then answer out loud: "if we moved to
PostgreSQL tomorrow, what changes?" The answer must be "`SqliteDbConnection` and the driver dependency",
not "the DAOs".

**Common mistakes.**

- **Declaring `class DbConnection` and implementing it twice.** The type DAOs import becomes the concrete
  one, undoing `DECISIONS.md` 33 in a single commit.
- **Making it a static holder** (`static Connection get()`). There is then no object to substitute, so a
  test cannot point a DAO at an in-memory database.
- **Putting the JDBC URL, the file name or the driver class in the interface.** An interface that
  mentions `gym.db` has already failed to be a seam.
- **Adding `close()` "for completeness".** The connections are the resources, not the interface; adding
  it forces every DAO to reason about a lifetime the design does not have.
- **Leaking the connection.** One leak per screen leaves SQLite files locked. Fix: try-with-resources in
  every DAO method.

### `SqliteDbConnection.java` — the only class that knows it is SQLite (owner: Abdelrhman)

**Status:** planned. **Canonical:** `docs/UML.md` §6 (same two methods), §3 (implements `DbConnection`);
`DECISIONS.md` 33; `src/main/resources/README.md` (`schema.sql` in `resources/db/`, `gym.db`
git-ignored).

**What it is.** The concrete `DbConnection`: it opens `gym.db` through the JDBC SQLite driver and creates
the tables from `schema.sql`. It is the only file in the project that may mention the URL, the file name
or the DDL.

**Members.**

| Member | Signature | Why this shape |
|---|---|---|
| `getConnection` | `Connection getConnection() throws SQLException` | One fresh `DriverManager.getConnection(URL)` per call; no pool (see `DbConnection`) |
| `initializeSchema` | `void initializeSchema() throws SQLException` | Runs the DDL; must be idempotent, because the bootstrap calls it on every launch |
| Implicit no-arg constructor | — | §6 declares no constructor, so the database path is a constant inside the class |
| Constants | `private static final String URL`, schema resource path | `static final` is not mutable state; nothing writes them after class initialisation |

**Why it is designed this way.** Everything that is "SQLite" and nothing that is "our domain" — which is
what `DECISIONS.md` 33 buys: the day storage changes, this file is the only edit in the persistence
package. The DDL stays in `resources/db/schema.sql` rather than a Java string so it can be pasted into
the `sqlite3` CLI and so `../dao/README.md`'s Week 1 table specs have one file to compare against.
**Idempotence is a requirement, not a nicety:** every statement must be `CREATE TABLE IF NOT EXISTS`, or
the second launch dies with "table customers already exists" before the login screen appears.

**Open detail (decide + log in `DECISIONS.md`).** §6 declares no constructor and no path field, so
*where* `gym.db` lives is unspecified: a relative `gym.db` is simplest but lands wherever the app was
launched from, so a Gradle run and an IDE run can use two databases; a fixed user-home path gives one
database but needs directory creation and a permissions decision; a constructor parameter makes tests
trivial (in-memory URL) but adds a constructor §6 does not have. A second open detail rides along:
SQLite enforces foreign keys **off by default per connection**, so whether `getConnection()` runs
`PRAGMA foreign_keys = ON` is a real choice — on catches a bad `customerId` at write time, off matches
rule 4's `GuardService` design and keeps loose seed data loadable.

**How to write it.**

1. Create `src/main/java/com/gym/util/SqliteDbConnection.java`, `package com.gym.util;`; declare
   `public class SqliteDbConnection implements DbConnection`. Import `java.sql.*` plus
   `java.io.InputStream`/`java.nio.charset.StandardCharsets`. No `javafx.*`.
2. Put the URL and the schema resource path in `private static final String` constants, with a comment
   naming the open decision.
3. `getConnection()`: `DriverManager.getConnection(URL)`. Do **not** call
   `Class.forName("org.sqlite.JDBC")` — JDBC 4 loads drivers from the classpath and the driver is already
   a Gradle dependency (`build.gradle`).
4. `initializeSchema()`: read `/db/schema.sql` from the classpath (leading slash = classpath root),
   decode UTF-8, execute each statement, and close with try-with-resources.
5. Fail loudly when the resource is missing. A swallowed `SQLException` here is the worst bug in the
   folder: every DAO then reports "no such table", three files away from the cause.
6. Add the `PRAGMA` line per the open decision, once, in `getConnection()`. Add no `main`, no static
   instance cache, no `reset()` for tests, no logging.

**Sketch.**

```java
package com.gym.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SqliteDbConnection implements DbConnection {

    // Open detail: where gym.db lives is not fixed by docs/UML.md 6 - decide and log in DECISIONS.md.
    private static final String URL = "jdbc:sqlite:gym.db";     // git-ignored; schema.sql is committed
    private static final String SCHEMA = "/db/schema.sql";      // leading slash = classpath root

    @Override
    public Connection getConnection() throws SQLException {
        Connection c = DriverManager.getConnection(URL);        // JDBC 4 loads the sqlite driver
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");              // second open detail: off by default
        }
        return c;                                               // caller closes it (try-with-resources)
    }

    @Override
    public void initializeSchema() throws SQLException {
        String ddl;
        try (InputStream in = SqliteDbConnection.class.getResourceAsStream(SCHEMA)) {
            if (in == null) throw new IllegalStateException("schema.sql missing from the classpath");
            ddl = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new DaoException("Could not read " + SCHEMA, e);
        }
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            for (String statement : ddl.split(";")) {            // schema.sql has no triggers or ';' in strings
                if (!statement.isBlank()) s.execute(statement);   // every CREATE must be IF NOT EXISTS
            }
        }
    }
}
```

**How to know it is right.** Run the app twice: the second run must not throw "table ... already
exists". Confirm exactly one `gym.db` exists, where you expect it, and that `git status` does not list
it. Inspect the schema from outside Java with `sqlite3 gym.db ".tables"` and
`sqlite3 gym.db ".schema customers"` — the tables must match `resources/db/schema.sql`, not a remembered
version. Then delete `gym.db`, start again and log in — a first-run bootstrap is the case a developer
never tests and a marker always hits. Rename `schema.sql` and confirm you get a message naming the
missing resource rather than a silent success.

**Common mistakes.**

- **A second copy of the path.** A DAO, test helper or seed script hardcoding `gym.db` creates a second
  database when the working directory differs.
- **Calling `initializeSchema()` from a DAO.** Two DAOs racing to create tables produce intermittent
  "database is locked" failures; the bootstrap calls it once, before the first DAO is used.
- **Forgetting `IF NOT EXISTS`.** The second launch crashes before the window appears.
- **Leaking the connection, statement or result set.** SQLite keeps the file locked and the symptom
  appears in a different screen.
- **Splitting the script on `;` without a rule.** Naive splitting breaks when someone adds a trigger or a
  semicolon inside a default value — document the restriction in `schema.sql`.
- **Swallowing the exception in `initializeSchema()`.** Every DAO then fails with "no such table".

### `Validators.java` — cheap, message-free input checks (owner: Abdelrhman)

**Status:** planned. **Canonical:** `docs/UML.md` §6 (five static methods); rule 8 uses `validTimeRange`
in `TrainerService.publishSlot`; rule 9 fixes the date format.

**What it is.** Pure predicates that answer "does this text look like a phone number / email / ISO date /
number / valid slot window?" and nothing else. Services call them before saving; a `false` becomes a
message in the service, never here.

**Members.**

| Member | Signature | What it answers |
|---|---|---|
| `validPhone` | `static boolean validPhone(String phone)` | Is this a usable phone number? Accepted shapes are an open detail |
| `validEmail` | `static boolean validEmail(String email)` | Loose shape check: one `@`, a dot in the domain, no spaces |
| `validDate` | `static boolean validDate(String date)` | Exactly `yyyy-MM-dd` **and** a real calendar date (rule 9) |
| `validNumber` | `static boolean validNumber(String value)` | A number for a form field; the intended domain is an open detail |
| `validTimeRange` | `static boolean validTimeRange(String from, String to)` | Both `HH:mm` and `from` strictly before `to`; rule 8 rejects `startTime >= endTime` |

**Why it is designed this way.** Every method returns the **primitive** `boolean`. A `Boolean` return
allows `null`, so `if (Validators.validEmail(email))` would throw a `NullPointerException` at the call
site — from a validator. Returning `false` for `null` is the contract: a validator never fails the
caller, it answers.

They are also deliberately **dumb about context**. `Validators` does not know what a `Customer` is, so it
cannot check that an email is unique (that needs the DAO) or word "Email is already in use" (that needs
the user's language). `CustomerService.validate(Customer)` returns a `List<String>` of messages and calls
these predicates to build it (`../service/README.md`). That split is why five methods serve every entity.

Three semantics to fix before typing. `validDate` accepts **only** `yyyy-MM-dd` (rule 9), and two JDK
traps matter: `ofPattern("uuuu-MM-dd")` uses resolver style **SMART**, which *adjusts* an impossible day
instead of rejecting it, so `2024-02-31` silently becomes `2024-02-29`; and `ResolverStyle.STRICT` with
`yyyy` (year-of-era) fails on every input. Use `uuuu` with `STRICT`, or
`DateTimeFormatter.ISO_LOCAL_DATE`. `validTimeRange` compares the two strings directly, because `HH:mm`
is fixed-width and zero-padded: `"09:00".compareTo("10:00") < 0` says "nine is before ten" (rule 9) —
the property `DECISIONS.md` 31 chose ISO-8601 for, valid only after the zero-padding is verified, which
is what the round-trip check below does. And `validNumber` is not a back door for money:
`Double.parseDouble` accepts `"NaN"`, `"Infinity"`, `"1e5"` and hex floats such as `"0x1p3"`, and
`new BigDecimal(String)` also accepts `"1e5"`.

**Open detail (decide + log in `DECISIONS.md`).** The accepted formats for `validPhone`, `validEmail` and
`validNumber` are not fixed by `UML.md`: too strict a phone rule rejects an international number, an
unbounded one accepts `"-"`. Propose the shapes in the sketch, check them against the demo data, and log
the final rules — a validator that disagrees with the seed data is how a demo fails in front of a
lecturer. **A tension to log as well:** rule 9 says `DateUtils` owns every parse, and §6 exposes no
`DateUtils.parseTime(String)`. Keep a validation-only formatter here and never use it to build a stored
value (what the sketch does), or add the parse to §6 first — do not leave it implicit.

**How to write it.**

1. Create `src/main/java/com/gym/util/Validators.java`, `package com.gym.util;`.
2. Declare `public final class Validators` — `final`, because a subclass could override a check and make
   two call sites disagree — then `private Validators() { }` with a one-line comment.
3. Declare the `DateTimeFormatter` constants `private static final`, built with `ResolverStyle.STRICT`.
   Formatters are immutable and thread-safe, unlike `SimpleDateFormat`.
4. Write each method as "null check → parse or match in a `try` → `false` on failure"; a
   `DateTimeParseException` must never escape.
5. Add the round-trip check (format the parsed value again and compare with the input). It rejects
   `2024-2-3` and `9:5` regardless of resolver subtleties, and it guarantees the zero-padding that
   rule 9's string comparisons depend on.
6. Javadoc each method with the format it accepts and the rule it enforces. No `Boolean` overload, no
   `validate(Customer)`, no `String` return type.

**Sketch.**

```java
package com.gym.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public final class Validators {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private Validators() { }   // stateless: every member is static (DECISIONS.md 37)

    /** yyyy-MM-dd only (rule 9). STRICT rejects 2024-02-31; the round trip rejects 2024-2-3. */
    public static boolean validDate(String date) {
        if (date == null) return false;
        try {
            return DATE.format(LocalDate.parse(date, DATE)).equals(date);
        } catch (DateTimeParseException notADate) {
            return false;                              // a bad input is an answer, not an exception
        }
    }

    /** Rule 8: startTime strictly before endTime. HH:mm sorts as text (DECISIONS.md 31). */
    public static boolean validTimeRange(String from, String to) {
        return validTime(from) && validTime(to) && from.compareTo(to) < 0;
    }

    private static boolean validTime(String time) {
        if (time == null) return false;
        try {
            return TIME.format(LocalTime.parse(time, TIME)).equals(time);
        } catch (DateTimeParseException notATime) {
            return false;
        }
    }

    /** Open detail: accepted phone shapes. Proposal: optional +, 7-15 digits, separators ignored. */
    public static boolean validPhone(String phone) {
        return phone != null && phone.replaceAll("[\\s()-]", "").matches("\\+?\\d{7,15}");
    }

    /** Deliberately loose: full RFC 5322 validation is unmaintainable and still proves nothing. */
    public static boolean validEmail(String email) {
        return email != null && email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    }

    /** Open detail: "number" means a non-negative integer count. Money uses new BigDecimal(String). */
    public static boolean validNumber(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            return Integer.parseInt(value.trim()) >= 0;
        } catch (NumberFormatException notANumber) {
            return false;
        }
    }
}
```

**How to know it is right.** No database and no toolkit are needed, so a plain JUnit test or a scratch
`main` printing a table of inputs is enough. Include: `validDate("2024-02-29")` → `true` (leap year);
`("2024-02-31")` → `false` (not lenient); `("2024-2-3")` → `false` (zero-padding);
`("12/03/2025")` → `false` (one format only); `validTimeRange("09:00","10:00")` → `true`;
`("10:00","10:00")` → `false` (rule 8); `("09:00","08:00")` → `false`; `validNumber("1e5")` → `false`;
`validPhone(null)` → `false` without throwing. Then grep the file for `Boolean` — an accidental boxed
return is the classic slip.

**Common mistakes.**

- **Returning `Boolean`.** It compiles, tests pass, and one `null` return NPEs at a call site that reads
  like a plain condition.
- **Using `SimpleDateFormat`.** Lenient *and* mutable, so a `static` instance shared between the FX
  thread and a background task returns corrupted dates.
- **Lenient date parsing.** `2024-02-31` becomes a stored membership end date and rule 7's arithmetic
  shifts the wrong day. Fix: `STRICT` plus the round-trip check.
- **Validating the entity here.** `validate(Customer)` drags `model` in and puts user-facing wording in a
  predicate; the service builds the messages.
- **Throwing instead of answering `false`.** Callers then wrap a yes/no question in `try`/`catch`.
- **Copying the `HH:mm` pattern into a third class.** Every new formatter is a chance to disagree with
  rule 9 — see the tension note.

### `DateUtils.java` — every date and time decision in the project (owner: Yousef)

**Status:** planned. **Canonical:** `docs/UML.md` §6 (four static methods); rules 7, 9, 10;
`DECISIONS.md` 31.

**What it is.** The one class that turns membership periods, session slots and "how long until this
session" into strings and numbers. Rule 9 is explicit: dates are `yyyy-MM-dd`, times are `HH:mm`,
`DateUtils` owns parsing, and **controllers never build a date or time string themselves**.

**Members.**

| Member | Signature | What it does |
|---|---|---|
| `addMonths` | `static String addMonths(String date, int months)` | Period arithmetic: `MONTHLY`/`QUARTERLY`/`YEARLY` → 1/3/12 months; renewing an `EXPIRED` membership extends from its end date (rule 7) |
| `formatDate` | `static String formatDate(String date)` | Formatting; which direction (normalise to ISO or render for display) is open detail 1 |
| `isBeforeNow` | `static boolean isBeforeNow(String date, String time)` | Is this slot already past? Used to hide or reject past slots |
| `hoursUntil` | `static long hoursUntil(String date, String time)` | Whole hours from now to the start; rule 10's 2-hour cancellation window |

**Why it is designed this way.** Dates are **strings** (SQLite has no date type) locked to
`yyyy-MM-dd`/`HH:mm` because **ISO-8601 sorts lexicographically in the same order as chronologically**
(`DECISIONS.md` 31). That one property makes three queries work without a date type:
`SessionDao.findByDay` (`WHERE date = ?`), `MembershipDao.findExpiring` (`WHERE endDate <= ?`) and
`PaymentDao.revenueBetween` (`WHERE date BETWEEN ? AND ?`).

**Month-end arithmetic is the other reason this class exists.** `addMonths("2025-01-31", 1)` must not
invent a 31 February: `LocalDate.plusMonths` clamps to the last valid day, giving `2025-02-28`
(`2024-02-29` in a leap year). That is right for a membership — a plan sold on the 31st ends on the last
day of a short month — and doing it with string manipulation or `+ 30 days` makes plans drift by days
over a year and breaks `findExpiring` for exactly the members who renewed in a 31-day month.

**Rule 10 and `hoursUntil`.** Cancelling up to 2 hours before the start is free; later cancellations are
still recorded as `CANCELLED` with the reason kept. `hoursUntil` returns whole hours, truncated:
2 h 30 m → `2`, 1 h 59 m → `1`, an already-started session → negative. The check is
`hoursUntil(date, time) < 2` → require a reason. Two traps: comparing with `== 2`, and printing it in a
label ("1 hour left" with 1 h 59 m remaining). It is a threshold, not a duration.

**Open details (decide + log in `DECISIONS.md`).** (1) **What `formatDate` means:** §6 fixes only
`String → String`; normalising to ISO or rendering `12 Mar 2025` are both defensible. ⚠️ If it renders a
display form, that output must never go back into SQL — `12 Mar 2025` matches no `date` column and does
not sort. (2) **Where "today" and day arithmetic live:** rule 5 needs `endDate >= today`, rule 7's
unfreeze needs `endDate + frozenDays`, and `expiryReminders(int withinDays)` needs "today + N days", but
§6 exposes only `addMonths`. Either inline `LocalDate.now()` in each service (contradicts rule 9's
single-owner intent) or add `today()`/`addDays(String, int)` to `UML.md` §6 first (recommended) — never
silently here. (3) **Time zone:** everything uses the JVM default zone and local wall-clock strings,
correct for a single-city gym and wrong for a server elsewhere; state the assumption in the Javadoc.

**How to write it.**

1. Create `src/main/java/com/gym/util/DateUtils.java`, `package com.gym.util;`. Import `java.time.*`
   only — no `java.util.Date`, no `SimpleDateFormat`, no `javafx.*` — then declare
   `public final class DateUtils` with `private DateUtils() { }`.
2. Declare two `private static final DateTimeFormatter` constants: `ISO_LOCAL_DATE` (strict,
   `uuuu-MM-dd`) and `ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT)`. One constant per
   format is what makes "every parse agrees" true.
3. `addMonths`: parse, `plusMonths(months)`, format back. Let a garbage input throw here, loudly, in
   development.
4. `formatDate`: implement open-detail-1 and Javadoc which direction it is.
5. `isBeforeNow`: build a `LocalDateTime` from date + time and compare with `LocalDateTime.now()`.
6. `hoursUntil`: `Duration.between(LocalDateTime.now(), start).toHours()`, and Javadoc the truncation,
   the negative case and the rule-10 threshold.
7. Add no `parse`/`today`/`addDays`/`formatTime` yet (open detail 2 goes to §6 first), no `Date`-returning
   overload, and no `System.currentTimeMillis()` for slot math.

**Sketch.**

```java
package com.gym.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

public final class DateUtils {

    // ISO_LOCAL_DATE is strict uuuu-MM-dd - the format rule 9 and DECISIONS.md 31 lock us to.
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM uuuu");

    private DateUtils() { }

    /** Plan periods: +1/+3/+12 months. LocalDate.plusMonths clamps 31 Jan + 1 to 28/29 Feb. */
    public static String addMonths(String date, int months) {
        return LocalDate.parse(date, DATE).plusMonths(months).format(DATE);
    }

    /** Open detail 1: this renders for display. Never feed the result back into SQL (rule 7). */
    public static String formatDate(String date) {
        return LocalDate.parse(date, DATE).format(DISPLAY);
    }

    /** True once date+time has passed - used to hide or reject past slots. */
    public static boolean isBeforeNow(String date, String time) {
        return LocalDateTime.of(LocalDate.parse(date, DATE), LocalTime.parse(time, TIME))
                .isBefore(LocalDateTime.now());
    }

    /** Rule 10 threshold. Whole hours, truncated; negative once the session has started. */
    public static long hoursUntil(String date, String time) {
        LocalDateTime start = LocalDateTime.of(LocalDate.parse(date, DATE), LocalTime.parse(time, TIME));
        return Duration.between(LocalDateTime.now(), start).toHours();
    }
}
```

**How to know it is right.** Print `addMonths("2025-01-31", 1)` → `2025-02-28`,
`addMonths("2024-01-31", 1)` → `2024-02-29`, `addMonths("2025-03-15", 12)` → `2026-03-15`. Prove the
sorting claim on the database, not on paper: insert memberships with end dates in different months and
compare `SELECT id FROM memberships WHERE endDate <= '2025-03-31' ORDER BY endDate` with the same filter
applied in Java after sorting the strings — the orders must match, and this fails immediately if anyone
stored `12 Mar 2025`. Walk the window: a session 3 hours ahead → free cancellation, 90 minutes ahead →
reason required, in the past → negative, and `SchedulingService.cancelSession` must use `< 2`. Feed
`formatDate`'s output back into `Validators.validDate`: `false` means open detail 1 is answered "render
for display". Grep `DateTimeFormatter|SimpleDateFormat` in `com.gym`: this class, plus `Validators`'
logged validation-only pair.

**Common mistakes.**

- **Parsing a date in a service or controller.** It works until a second screen uses `dd/MM/yyyy`, and
  then `findByDay` returns nothing for one screen and rows for another.
- **`SimpleDateFormat` or `new Date().toString()`.** Lenient, not thread-safe, locale/zone dependent.
- **Month arithmetic by adding 30 days.** A yearly plan ends 5 days short and `findExpiring` fires early
  for everyone who joined in a 31-day month. Fix: `plusMonths`.
- **Treating `hoursUntil` as an exact duration.** A 119-minute wait reports `1`, so a label built from it
  is wrong while the rule-10 check is right.
- **Building tomorrow's date in a controller.** Rule 9 forbids it and it breaks at month ends; open
  detail 2 adds `today()`/`addDays()` to §6.
- **Mixing in `Instant.now()` somewhere.** The 2-hour window shifts by the zone offset.

### `GuardService.java` — one reference check instead of 26 DAO methods (owner: Abdelrhman)

**Status:** planned. **Canonical:** `docs/UML.md` §6 (`countReferences(String table, int id)`, static,
`«utility»`), §4 (`AdminService ..> GuardService : reference check with`); rule 4; `DECISIONS.md` 39.

**What it is.** The reference counter behind rule 4. Before `AdminService` hard-deletes a customer or a
trainer, it asks this class how many rows elsewhere in the schema still point at that row. A non-zero
answer aborts the delete.

**Members.**

| Member | Signature | Why this shape |
|---|---|---|
| `countReferences` | `static int countReferences(String table, int id)` | One method for nine tables, returning the **total** — the caller needs "is it safe?", not a breakdown |
| `table` argument | `String` | The deleted row's table name, e.g. `"customers"`. Entity names and table names differ, which is where callers get it wrong |
| Private helpers | connection lookup, `tableExists`, `referencingTables`, `foreignKeyColumn` | Not in §6; see the open details |

**Why it is designed this way.** Rule 4 refuses a hard delete while references exist, and references span
nine tables (memberships, sessions, payments, attendance, progress records, medical profile, workout
programs, trainer subscriptions, availability). The obvious implementation adds
`countByCustomer`/`countByTrainer` to every DAO — one method on each of the 13 interfaces plus its
implementation, roughly 26 near-identical bodies whose only difference is a table name
(`DECISIONS.md` 39). One static helper keeps that cross-cutting read in one place and keeps the DAOs to
their normal CRUD shape. It is needed because `schema.sql` declares **no FK cascades**
(`../dao/README.md`): the database will not refuse the delete for us.

**Direction matters, and it is easy to get backwards.** `countReferences("customers", 7)` counts rows
that *point at* customer 7 — not what customer 7 points at. Because `customers.trainerId` and
`sessions.trainerId` exist, `countReferences("trainers", 3)` must count the **customers** assigned to
that trainer, so the helper asks the schema which tables hold a foreign key whose parent is the table
being deleted.

**How discovery works, and why that is the whole design.** SQLite exposes the schema as data: list the
real tables with `SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'`, then
for each candidate child ask `PRAGMA foreign_key_list(<child>)` — columns `id, seq, table, from, to,
on_update, on_delete, match`, where `table` is the **parent** and `from` the child's column. A child
references the parent when some row has `table = <parent>`; its count is
`SELECT COUNT(*) FROM <child> WHERE <from> = ?`. Two consequences: this only works because the schema
*declares* its foreign keys (a forgotten one is silently missed), and the `table` argument cannot be a
`?` bind parameter — SQLite binds values, not identifiers — so it must be checked against the set read
from `sqlite_master` before it is concatenated (rule 13).

**Open details (decide + log in `DECISIONS.md`).** (1) **The reference-table list.** Which tables
reference which, under what exact names, is nowhere yet: `../dao/README.md` fixes four names
(`medical_profiles`, `administrators`, `trainer_subscriptions`, `trainer_availability`) and leaves the
rest to `schema.sql`, which does not exist. Either rely on PRAGMA discovery (nothing to maintain, only as
good as the declared FKs) or keep a documented static list as a second opinion (survives a forgotten
foreign key, rots silently); recommendation: discovery plus a test that every expected child appears for
`customers` and `trainers`. (2) **Where the connection comes from.** §6 gives the method no
`DbConnection` parameter and a static method has no field: options are a private static `DbConnection`
set by the bootstrap (a second piece of documented static mutable state, so `DECISIONS.md` 34 must be
amended), a small holder class in `util` (cleanest, but a new class — `UML.md` §6 first), or
`new SqliteDbConnection()` here (names the concrete class outside the bootstrap and duplicates the path
decision, breaking `DECISIONS.md` 33). Recommendation: the holder class, logged — never widen the §6
signature as a shortcut. (3) **What counts as a reference for historical rows:** payments and attendance
are never deleted (rule 19), so a customer with one past payment can never be hard-deleted. Probably
correct — confirm it and say so in `AdminService`'s Javadoc.

**How to write it.**

1. Create `src/main/java/com/gym/util/GuardService.java`, `package com.gym.util;`.
2. Declare `public final class GuardService` with `private GuardService() { }`. It is named `*Service`
   because rule 4 names it that way, but it lives in `util` and is a static helper — say so in the
   Javadoc so nobody moves it into `service/`.
3. Import `java.sql.*`, `java.util.*` and nothing else — no JavaFX, no `model`; it must not know what a
   `Customer` is.
4. `countReferences`: validate the table name against the live schema, enumerate the referencing
   children, sum their counts, return the total.
5. Translate `SQLException` into `DaoException("Reference check failed for " + table + "#" + id, e)`.
   **Fail closed:** a check that throws must abort the delete, never return 0.
6. Keep the discovery helpers private. If a second caller needs "which tables reference `trainers`",
   promote that through `UML.md` §6 instead of making it public. Any other query belongs in a DAO.

**Sketch.**

```java
package com.gym.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Rule 4's gate: counts rows that reference a row before an admin hard delete (DECISIONS.md 39). */
public final class GuardService {

    private GuardService() { }   // static helper in util, named "Service" by rule 4 - not a service class

    /** Sketch - the one member in docs/UML.md 6. Total rows whose foreign keys point at table#id. */
    public static int countReferences(String table, int id) {
        int total = 0;
        try (Connection c = connection()) {                      // open detail 2: where this comes from
            if (!tableExists(c, table)) {                        // rule 13: identifiers cannot be bound
                throw new IllegalArgumentException("Unknown table: " + table);
            }
            for (String child : referencingTables(c, table)) {   // PRAGMA discovery, see above
                String column = foreignKeyColumn(c, child, table);
                // child and column are schema metadata from sqlite_master/PRAGMA, never user input.
                String sql = "SELECT COUNT(*) FROM " + child + " WHERE " + column + " = ?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) total += rs.getInt(1);    // count only: the caller needs yes/no
                    }
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Reference check failed for " + table + "#" + id, e);   // fail closed
        }
        return total;
    }
    // private helpers: connection(), tableExists(...), referencingTables(...), foreignKeyColumn(...)
}
```

**How to know it is right.** Seed one customer with one membership (rule 4's first referencing table) and
one trainer assigned to that customer: `countReferences("customers", customerId)` and
`countReferences("trainers", trainerId)` must both be ≥ 1 — the second is the direction test people get
backwards. Ask for a row nothing references: `0`, and `AdminService.deleteCustomer` proceeds. Call it
with `"Customer"` and with `"customers; DROP TABLE payments"`: both must fail clearly and run no SQL.
Rename `gym.db` away and call it: it must throw `DaoException` and the delete path must abort — a `0`
here means a delete can orphan rows whenever the database hiccups. Finally add a throwaway table with a
foreign key to `customers`, run the app, and confirm the count changes with no edit to `GuardService`
(the payoff of discovery, and the reason to log open detail 1).

**Common mistakes.**

- **Interpolating `table` without checking it.** An unchecked identifier is both a runtime error and an
  injection point; verify it against `sqlite_master` first (rule 13).
- **Rounding the wrong way on failure.** Returning `0` (or `-1`) after a `SQLException` lets the delete
  through; throw instead.
- **Counting the row's outgoing foreign keys.** For a trainer, "who does this trainer point at?" is the
  wrong question — always count children of the deleted row.
- **Hardcoding the child list in `AdminService`.** That is the 26-method design with extra steps.
- **Passing `"Customer"`, `"customer"` or `"Customers"`.** Which spelling is right is open detail 1; read
  `schema.sql`, agree the convention, and let the `IllegalArgumentException` catch the rest.

### `SessionContext.java` — the documented piece of static mutable state (owner: Abdelrhman)

**Status:** planned. **Canonical:** `docs/UML.md` §6 (field + four methods), §2 "Startup and session
flow" steps 2–4, §7 (why controllers cannot take constructor parameters); rule 16; `DECISIONS.md` 34.

**What it is.** The signed-in `Administrator`, reachable from every controller, plus a gate
(`requireRole`) that refuses a caller whose role does not match. It is the **only** deliberate exception
to the project's no-static-mutable-state rule, and `DECISIONS.md` 34 says so in those words.

**Members.**

| Member | Signature | Why this shape |
|---|---|---|
| `current` | `private static Administrator current` | Private, so the only writes are the two methods below; a public field would let any screen swap the identity |
| `setAdministrator` | `static void setAdministrator(Administrator admin)` | Called by `LoginController` after `AdminService.authenticate` returns non-null (§2 flow step 3) |
| `getAdministrator` | `static Administrator getAdministrator()` | The signed-in operator, or `null`; a controller reads it for a header |
| `requireRole` | `static void requireRole(StaffRole role)` | Returns `void`, so "refuse" can only mean *throw*; this is rule 16's UI gate |
| `clear` | `static void clear()` | Called by `DashboardController.onLogout()` **and** by `MainApp` on exit (step 4) |

**Why it is designed this way.** Passing the `Administrator` to each screen does not work in JavaFX:
`FXMLLoader` builds controllers through a public no-arg constructor, so there is no parameter to inject
(`UML.md` §7 rejects a `BaseController` for the same reason). A controller could receive it in an
`initData(...)` call after loading, but then every screen must remember to make that call, and one
forgotten screen runs with no identity. A static holder is reachable from everywhere by construction, so
the awkwardness is paid once, in one file, with a decision entry naming it (`DECISIONS.md` 34).

It is **static state, not a Singleton**: no `getInstance()`, no lazy initialisation, no lock, because the
object held is just data. The price is discipline, and it has three parts. **(a) FX-thread confinement**
— every write happens in a JavaFX handler (`onLogin`, `onLogout`) and every read in
`initialize`/handler code, so the field needs no `synchronized` and no `volatile`; the moment a
background `Task` reads it, that argument is gone, so snapshot the value into a local before the task
starts. **(b) Cleared on logout** — a shared machine must not hand the next user the previous user's
role. **(c) Cleared on exit** — `MainApp` clears it as a safety net, so a relaunch inside the same JVM
starts with no identity.

**Why `requireRole` throws.** Rule 16 gates every `AdminService` operation and every override —
including `SchedulingService.adminReserve` — behind `StaffRole.ADMIN`. A `boolean requireRole(...)` would
let a caller ignore the answer; the `void` signature in §6 forces the refusal to be an exception that
stops the handler. Unchecked, so a JavaFX handler can throw it and the controller catches it for
`FxUtils.error(...)` (`../controller/README.md`).

**Open details (decide + log in `DECISIONS.md`).** (1) **Which exception types:** proposal —
`IllegalStateException("No signed-in administrator.")` when `current == null`, `SecurityException("ADMIN
role required.")` when the role differs; both unchecked, distinguishable in a log, neither inventing a
type. A dedicated `AccessDeniedException` needs `UML.md` §6 first. (2) **Exact role or hierarchy?**
`StaffRole` is a flat enum (ADMIN, MANAGER, STAFF) and the literal order states no permissions anywhere in
the docs, so implement **exact equality** — `requireRole(StaffRole.ADMIN)` means "ADMIN only", which is
what rule 16 says. If MANAGER should pass too, that is a permissions decision for `UML.md` first; never
compare `ordinal()`. (3) **A documentation mismatch to fix:** `DECISIONS.md` 34 and §2's flow use this as
a static context, so the members must be `static`, but §6 draws the box without the `$` markers the other
utilities carry (correctly without `«utility»`, since it is stateful), leaving the static nature unstated
in the rendered diagram. Add the `$` to the field and the four methods in the `UML.md` source.

**How to write it.**

1. Create `src/main/java/com/gym/util/SessionContext.java`, `package com.gym.util;`. Import
   `com.gym.model.Administrator` and the `StaffRole` type `getRole()` returns — no JavaFX (rule 2: the
   `swing` branch reuses this file), no SQL.
2. Declare `public final class SessionContext` with `private SessionContext() { }` — all four members are
   static, so no instance is ever needed.
3. Declare `private static Administrator current;` with the comment
   "THE documented static mutable state (DECISIONS.md 34)".
4. Write `setAdministrator`/`getAdministrator` as one-liners. No validation in the setter: by then
   `AdminService.authenticate` has rejected unknown usernames, wrong passwords and `active == false`.
5. Write `requireRole` reading the field **once** into a local, then null-check, compare, throw — reading
   the static twice is what lets the value change between the check and the use.
6. Write `clear()` as `current = null;`, Javadoc both call sites by name, and Javadoc the three rules: FX
   thread only, cleared on logout, cleared on exit. Do not add `isLoggedIn()`, `logout()`,
   `setRole(...)`, or a second field for the selected customer.

**Sketch.**

```java
package com.gym.util;

import com.gym.model.Administrator;
import com.gym.model.StaffRole;

/**
 * Who is signed in. THE documented static mutable state (DECISIONS.md 34).
 *
 * <p>Rules: touch it only from the FX thread; clear it on logout AND on application exit; do not use
 * it as a cache for screen state such as the selected customer.
 */
public final class SessionContext {

    private static Administrator current;   // static is the point - reachable from every controller

    private SessionContext() { }            // every call is SessionContext.method(...)

    public static void setAdministrator(Administrator admin) {   // LoginController, after authenticate
        current = admin;
    }

    public static Administrator getAdministrator() {
        return current;                     // null when nobody is signed in
    }

    /** Gate for ADMIN-only screens and override operations (UML.md rule 16). */
    public static void requireRole(StaffRole role) {
        Administrator admin = current;                                // read once: no check-then-use gap
        if (admin == null) throw new IllegalStateException("No signed-in administrator.");
        if (admin.getRole() != role) throw new SecurityException(role + " role required.");
    }

    public static void clear() {            // DashboardController.onLogout() and MainApp on exit
        current = null;
    }
}
```

**How to know it is right.** Walk the gate without a database: `requireRole(StaffRole.ADMIN)` before any
login → `IllegalStateException`; after logging in a `STAFF` administrator → `SecurityException`; after an
`ADMIN` → no exception; after `clear()` → it throws again. Then log in as ADMIN, log out, log in as STAFF
**without restarting**, and open `StaffController`: it must be refused (a pass means `clear()` is missing
or a stale object was stored). Grep the writers —
`grep -rn "SessionContext.setAdministrator\|SessionContext.clear" src/main/java` must show only
`LoginController`, `DashboardController` and `MainApp` — and confirm no `SessionContext.current` exists
outside this file. Finally close the window with a user signed in, check `MainApp` clears the context, and
relaunch in the same JVM: the login screen must show with no identity.

**Common mistakes.**

- **Reading the context from a background thread.** A `Task` sees a value with no happens-before
  guarantee and may see a *later* user's identity; read on the FX thread and pass the value in.
- **Forgetting `clear()` on logout.** The next user inherits ADMIN — a privilege leak, not cosmetics.
- **Letting it hold more than the identity.** Selected customer, current filter, last receipt: all
  screen state, all leaking between screens once global.
- **`getRole().name().equals("ADMIN")`.** A string comparison against an enum name breaks on a rename and
  compiles fine; compare the enum constant.
- **Making `current` public "for convenience".** Then `requireRole` is advisory.
- **Treating the UI gate as enforcement.** `requireRole` protects the *screen*; `AdminService` and the
  override operations check the role themselves (rule 16), because a service is callable without a UI.

### `PasswordUtils.java` — PBKDF2 hashing with the JDK only (owner: Abdelrhman)

**Status:** planned. **Canonical:** `docs/UML.md` §6 (`hash`, `verify`), §2 step 2 (the login call),
rule 16 (`authenticate` rejects an inactive account); `DECISIONS.md` 34 (session identity).
**Related:** `Administrator.passwordHash` (`../model/README.md`) and `AdminService.authenticate`
(`../service/README.md`) — this class is the only thing that turns a typed password into the stored
value and back into a yes/no answer.

**What it is.** Two static methods. `hash(plain)` turns a password into a string that can be stored in
`administrators.passwordHash`; `verify(plain, stored)` answers whether a typed password matches that
stored string. Everything else about passwords — where they are typed, when the field is cleared, who
may reset one — belongs to `LoginController`, `StaffController` and `AdminService`.

**Members.**

| Member | Signature | Why this shape |
|---|---|---|
| `hash` | `static String hash(String plain)` | Returns **one** string that carries the algorithm, the iteration count, the salt and the derived key, so the database column stays a single `TEXT` and a later iteration-count increase still verifies old rows |
| `verify` | `static boolean verify(String plain, String hash)` | Returns `boolean`, so a caller cannot ignore the answer without writing `if (!verify(...))` explicitly; never throws for a *wrong password* — a wrong password is a normal answer, not an error |
| (`$` markers) | — | Both members are `static`; keep the `$` in the `UML.md` source (`DECISIONS.md` 37) |

**Why it is designed this way.** Three decisions, each closing a real attack:

1. **PBKDF2, not a plain hash.** `MessageDigest.getInstance("SHA-256")` is fast by design — a GPU tries
   billions of candidates per second, so a leaked table is cracked in hours. PBKDF2
   (`PBKDF2WithHmacSHA256`) is deliberately slow and its cost is tunable, which is the whole point of a
   password hash (`DECISIONS.md`: Java built-in, no new dependency). HMAC-SHA256 rather than
   HMAC-SHA1 because SHA-1 is no longer a safe default for new code.
2. **A per-user random salt.** Without one, two members who chose `gym1234` produce identical hashes:
   cracking one cracks both, and a precomputed rainbow table attacks everyone at once. `SecureRandom`
   (not `Random`) because `Random` is predictable from a few outputs.
3. **`MessageDigest.isEqual`, not `String.equals`.** `equals` returns as soon as two bytes differ, so
   the time it takes leaks how many leading bytes of a guess were right. `isEqual` compares the whole
   array. This is not theory-paranoia in a library used by a login screen; it is a two-line fix.

Also rejected: **storing the plain text** (obviously), **a single application-wide salt** (same as no
salt once the constant is in the repository), **MD5/SHA-1 single round** (fast, broken), and **a
third-party library** (a new runtime dependency needs approval — `AGENTS.md` §9 — and the JDK already
ships the primitive).

**Open details (decide + log in `DECISIONS.md`).** These are real choices the docs do not fix:

1. **Iteration count.** The OWASP-style baseline for PBKDF2-HMAC-SHA256 is on the order of a few hundred
   thousand iterations; pick a number, measure that one `verify` stays well under ~100 ms on the
   slowest team laptop, write it as a `private static final int ITERATIONS`, and never change it in
   place (a change must keep verifying old rows).
2. **Encoded format.** Proposal: `pbkdf2-sha256$<iterations>$<base64 salt>$<base64 hash>` — one `$`
   separated string, parsed back by `verify`. Any format works as long as `hash` writes it and `verify`
   reads it, and as long as the iteration count and salt travel with the hash.
3. **Salt and key length.** Proposal: 16-byte salt, 256-bit key.
4. **What `verify` does with a malformed stored value** (empty string, wrong number of `$` parts, bad
   Base64). Proposal: return `false` — fail closed, because a corrupt row must never authenticate
   anybody, and a thrown exception would surface as a stack trace at the login screen.
5. **Null/blank input to `hash`.** Proposal: `IllegalArgumentException` (a programming error, caught in
   development) while `verify` returns `false` for blank input (a user's mistake, handled in the UI).
   `CustomerService.validate` owns the user-facing wording for the *reset* form; this class only refuses.

**How to write it.**

1. Create `src/main/java/com/gym/util/PasswordUtils.java`, `package com.gym.util;`. Import
   `javax.crypto.SecretKeyFactory`, `javax.crypto.spec.PBEKeySpec`, `java.security.SecureRandom`,
   `java.security.MessageDigest`, `java.security.spec.InvalidKeySpecException`, `java.util.Base64`,
   `java.nio.charset.StandardCharsets` and `java.util.Arrays`. **No JavaFX, no SQL** (rule 2 — the
   `swing` branch reuses this file unchanged).
2. Declare `public final class PasswordUtils` with `private PasswordUtils() { }` and the constants:
   `ALGORITHM = "PBKDF2WithHmacSHA256"`, `ITERATIONS`, `SALT_BYTES = 16`, `KEY_BITS = 256`.
3. Write `hash(String plain)`: reject `null`/blank, `byte[] salt = new byte[SALT_BYTES]` filled by a
   `private static final SecureRandom`, derive the key (step 4), then format salt and key with
   `Base64.getEncoder().withoutPadding()` into the encoded string.
4. Write a `private static byte[] derive(char[] password, byte[] salt, int iterations)` helper that
   builds the `PBEKeySpec`, calls `SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec)
   .getEncoded()`, and **clears the spec in a `finally`** (`spec.clearPassword()`). Never `return` from
   inside the `try` before the `finally` runs.
5. Write `verify(String plain, String stored)`: split the stored string on `$`, parse the iteration
   count and decode the two Base64 parts, re-derive with the **stored** salt and iterations, and return
   `MessageDigest.isEqual(derived, expected)`. Any parse failure returns `false` (open detail 4).
6. Do not add `hashWithSalt(...)`, `isStrong(String)`, `randomPassword()` or a `main` method. Each is a
   member §6 does not have; if the team wants one, `docs/UML.md` §6 changes first.
7. Javadoc the contract in one line each: "slow, salted, one-way; the returned string contains its own
   parameters" and "false for a wrong password, a blank password or a damaged stored value".

**Sketch.**

```java
// Sketch — signatures are authoritative in docs/UML.md §6.
private static final SecureRandom RANDOM = new SecureRandom();
private static final int ITERATIONS = 210_000;   // Open detail 1: measure on the slowest team laptop
private static final int SALT_BYTES = 16, KEY_BITS = 256;

public static String hash(String plain) {
    if (plain == null || plain.isBlank()) throw new IllegalArgumentException("password is empty");
    byte[] salt = new byte[SALT_BYTES];
    RANDOM.nextBytes(salt);                                       // per-user, never a constant
    byte[] key = derive(plain.toCharArray(), salt, ITERATIONS);
    return "pbkdf2-sha256$" + ITERATIONS + "$"
         + Base64.getEncoder().withoutPadding().encodeToString(salt) + "$"
         + Base64.getEncoder().withoutPadding().encodeToString(key);
}

public static boolean verify(String plain, String stored) {
    if (plain == null || stored == null) return false;
    String[] parts = stored.split("\\$");
    if (parts.length != 4) return false;                          // fail closed (Open detail 4)
    try {
        int iterations = Integer.parseInt(parts[1]);
        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expected = Base64.getDecoder().decode(parts[3]);
        byte[] actual = derive(plain.toCharArray(), salt, iterations);   // stored params, not today's
        return MessageDigest.isEqual(actual, expected);           // constant time, not equals
    } catch (IllegalArgumentException e) {                        // bad Base64 / bad number
        return false;
    }
}
```

**How to know it is right.**

1. `verify("secret", hash("secret"))` is `true`; `verify("Secret", hash("secret"))` is `false` (case
   matters); `verify("", stored)` is `false`.
2. `hash("secret")` twice gives **two different strings**, and both verify — the salt is random.
3. Round-trip a row through the database: store the hash, read it back as `TEXT`, verify. A `TEXT`
   column that truncates (for example to 20 characters) breaks verification, which is how a too-short
   column is caught before the demo.
4. Log the elapsed time of one `verify`: it must be slow enough to be expensive (tens of milliseconds),
   fast enough not to freeze the FX thread.
5. Nothing in the class writes to `System.out`, so no plain text or hash reaches a log.

**Common mistakes.**

- **Comparing with `stored.equals(hash(plain))`.** Re-hashing produces a *different* string (new salt),
  so this always fails; you must re-derive with the stored salt and iterations.
- **`new PBEKeySpec(plain.toCharArray(), ...)` without `clearPassword()`.** The derived copy stays in
  the heap after the call; the `finally` block is not optional.
- **Keeping the password in a `String`.** A `String` cannot be wiped (immutable, interned, copied by the
  garbage collector), which is why the API necessarily takes one; the mitigation available to us is
  clearing the JavaFX `PasswordField` after `authenticate` returns (`../controller/README.md`), and
  never storing the plain text in a field.
- **Using `salt = "gym".getBytes()`.** A constant salt makes the stored table crackable with one
  rainbow table, which defeats the point of hashing.
- **Reusing the iteration count from the constants when verifying.** Reading `ITERATIONS` instead of
  `parts[1]` means every stored row silently breaks the day the constant is raised.
- **Throwing on a wrong password.** `authenticate` returns `null` for a bad login; an exception turns a
  normal mistyped password into an error dialog and a stack trace.
- **Logging the password or the hash** "to debug the login". Both are credentials; the log is not
  protected the way the database is.

---

### `FxUtils.java` — the single JavaFX-aware utility (owner: Abdelrhman)

**Status:** planned. **Canonical:** `docs/UML.md` §6 (five static members), §2 layer rule 3 (`DaoException`
is shown with `FxUtils.error(getMessage())`), §2 startup flow step 1 (the overlay layer), §5
(`MainApp ..> FxUtils`); `DECISIONS.md` 8 (overlay instead of extra `Stage`s) and 35 (the overlay entry
point is not exported yet).
**Related:** every controller — `../controller/README.md` is its only caller layer.

**What it is.** The one class in `util` (and, with the controllers, one of only two places in the whole
project) allowed to import JavaFX. It owns the four things every screen would otherwise re-implement:
an information dialog, a yes/no confirmation, an error dialog, scene switching — plus the M3 snackbar.
It takes **plain `String`s**, so it has no idea that `DaoException`, entities or `BigDecimal` exist.

**Members.**

| Member | Signature | Why this shape |
|---|---|---|
| `info` | `static void info(String message)` | Acknowledgement that needs no answer; `void`, because there is nothing to return |
| `confirm` | `static boolean confirm(String message)` | The only member that returns a value: `true`/`false` is the user's answer, and the caller must handle both |
| `error` | `static void error(String message)` | The single funnel for every failure the user sees — controllers pass `e.getMessage()` |
| `switchScene` | `static void switchScene(String fxml)` | Takes the FXML path, not a `Scene`/`Stage`, so screens do not manage windows |
| `snackbar` | `static void snackbar(String message)` | The light, non-blocking confirmation for a successful action (§4 rule 23 in `../controller/README.md`: one visible answer per gesture) |
| (`$` markers) | — | All five are `static`; keep the `$` in the `UML.md` source (`DECISIONS.md` 37) |

**Why it is designed this way.** JavaFX forces this class to exist, and the design limits the damage:

- **One JavaFX-aware utility, not scattered dialogs.** `Alert` construction is five lines of boilerplate
  that invites copy-paste divergence ("Could not save" in one screen, "Error!" in another). Centralising
  it also gives one place to enforce the project's rule that **a stack trace never reaches the user**
  (`README.md` §11).
- **Plain `String` parameters.** If `error` took a `Throwable`, `util` would depend on which exceptions
  the DAO throws and the wording would move out of the controller's control; §2 requires the controller
  to pass `e.getMessage()` and keep the wording decision at the screen.
- **No state.** It is a `«utility»`: `static` members, private constructor, no field. That is why it can
  be called from any screen without wiring, and why the `swing` branch simply replaces the file.
- **`switchScene(String)` by path, not by node.** A screen that holds a reference to the next screen's
  `Parent` creates the coupling the nav shell exists to avoid; passing the FXML path keeps every screen
  replaceable.

**Open details (decide + log in `DECISIONS.md`).**

1. **How `switchScene` finds the window.** §6 gives it exactly one parameter, so it cannot be handed the
   `Stage`. Proposal: look up the currently focused/shown `Window`
   (`Window.getWindows().stream().filter(Window::isFocused).findFirst()`) and set its scene; the
   alternative — a `static Stage` kept by `MainApp` — is static mutable state that `DECISIONS.md` 34
   permits only for `SessionContext`, and the third alternative (add a `Window`/`Node` parameter) is a
   `UML.md` §6 change. Pick one, write the reason down, and keep `MainApp` as the composition root.
2. **The snackbar's home until the overlay facade exists.** `DECISIONS.md` 35 records that the dialog /
   snackbar / sheet overlay lives in a library package that is not exported, so a true in-window
   snackbar cannot be installed yet. Interim behaviour must be defined rather than improvised:
   proposal — `snackbar(...)` logs the message and falls back to `info(...)`, with one comment naming
   `DECISIONS.md` 35; when the public entry point lands, the fallback is deleted and the overlay call is
   written. Do not fake an overlay with a second `Stage`.
3. **Dialog text length and truncation.** A generated message (for example a `BigDecimal` balance) can
   be arbitrarily long; decide whether `error` wraps at a fixed width (`Alert.setContentText` does not
   wrap by default) and note it, so no screen re-invents its own `Label`.
4. **Thread rule.** `info`/`confirm`/`error`/`snackbar` must be called on the FX application thread;
   from a `Task`, the caller wraps the call in `Platform.runLater(...)`. Write that in the Javadoc —
   JavaFX throws when a dialog is built on another thread, and the stack trace points here, not at the
   caller.

**How to write it.**

1. Create `src/main/java/com/gym/util/FxUtils.java`, `package com.gym.util;`. Import `javafx.scene.control.Alert`,
   `javafx.scene.control.ButtonType`, `javafx.fxml.FXMLLoader`, `javafx.scene.Parent`, `javafx.scene.Scene`,
   `javafx.stage.Window` (only what you use) and `java.io.IOException`/`java.net.URL`.
2. Declare `public final class FxUtils` with `private FxUtils() { }` and a
   `private static final String FXML_ROOT = "/fxml/"` prefix constant, so every path is built the same
   way and the leading slash is documented once (`../../resources/fxml/README.md`).
3. Write a `private static Alert alert(Alert.AlertType type, String message)` helper that builds the
   alert with the project title, sets the header to `null` (the M3 look has no header row), sets the
   content text, and applies the theme style class the app uses. `info`/`error` call it and
   `showAndWait()`; **no dialog is ever shown from a static initialiser or a background thread**.
4. Write `confirm(String)` as the same alert with `AlertType.CONFIRMATION` and
   `ButtonType.OK`/`ButtonType.CANCEL`, returning `result.filter(ButtonType.OK::equals).isPresent()` —
   one expression, and `CANCEL` (or closing the window) means `false`.
5. Write `switchScene(String fxml)` per open detail 1: resolve the URL,
   `FXMLLoader.load(url)`, find the window, `window.getScene().setRoot(root)` (or set a new `Scene` when
   there is none), and wrap `IOException` in a `DaoException`-style unchecked value or a plain
   `IllegalStateException` naming the path — the user sees a sentence through `error`, never a stack
   trace.
6. Write `snackbar(String)` with the fallback from open detail 2 and a comment pointing at
   `DECISIONS.md` 35.
7. Do not add `warn(...)`, `prompt(...)`, `setStage(...)`, `money(BigDecimal)` or a theme parameter.
   Formatting money is the controller's job (`toPlainString()`), and a `setStage` member is the static
   window state the project avoids.

**Sketch.**

```java
// Sketch — signatures are authoritative in docs/UML.md §6.
public static void error(String message) {
    Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);   // plain String, never a Throwable
    a.setTitle("Gym Management System");
    a.setHeaderText(null);                                                // M3 look has no header row
    a.showAndWait();                                                      // FX thread only
}

public static boolean confirm(String message) {
    Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
    a.setHeaderText(null);
    return a.showAndWait().filter(ButtonType.OK::equals).isPresent();     // CANCEL or close -> false
}
```

**How to know it is right.**

1. Trigger each member from a screen: a save shows exactly one success signal, a failure shows one error
   dialog whose text is a sentence (no `Exception`, no class name, no stack trace).
2. `confirm` returns `false` when the user closes the dialog with `Esc` or the window button, not just
   when `Cancel` is clicked.
3. `switchScene` opens a screen twice in a row without leaking a second window, and an FXML path typo
   produces a friendly dialog rather than a stack trace.
4. Dialog text is readable in both light and dark themes (a hardcoded colour in the alert would look
   correct in one theme only).
5. Nothing here runs during class loading, so the app starts even when no window exists yet.

**Common mistakes.**

- **Passing an exception.** `error(e)` or `error(e.toString())` puts a class name and a message chain in
  front of a receptionist; pass `e.getMessage()` and keep the cause in the log.
- **Calling `showAndWait` from a `Task` or a DAO.** JavaFX throws, and the stack trace lands here
  instead of at the offending caller; wrap in `Platform.runLater`.
- **Building a new `Stage` for a dialog "because it is simpler".** It escapes the overlay design
  (`DECISIONS.md` 8), steals focus outside the app, and cannot be themed by the scene stylesheet.
- **Holding the `Stage` in a static field inside `FxUtils`.** That is the static window state the
  project keeps in `MainApp`; it also breaks the moment a second window or a test exists.
- **Formatting values here.** `error("Balance: " + balance)` with a `double` reintroduces the
  binary-float display bug; the controller formats with `BigDecimal.toPlainString()`.
- **Swallowing the failure path.** `try { ... } catch (Exception e) { }` in a UI helper hides the bug
  that the whole error-handling standard exists to surface.
- **Returning `true` from `confirm` by default.** A confirmation that defaults to "yes" turns a
  mis-click into a voided payment (rule 19).

---

### `ChartUtils.java` — numbers into a series, nothing else (owner: Abdel Raouf)

**Status:** planned. **Canonical:** `docs/UML.md` §6 (one member: `weightSeries`), §4
(`ProgressService ..> ChartUtils : charts with`), rule 13 (derived values are computed on read) and
`ProgressMetric` (WEIGHT_KG / BODY_FAT_PCT — see §1).
**Related:** `ProgressService.series(int, ProgressMetric)` (`../service/README.md`) and the
`ProgressController` screen (`../controller/README.md`).

**What it is.** A pure formatting/mapping helper for the progress charts: it takes the progress records
a service already fetched and returns the numbers the chart plots. It does not query, does not know what
a `double` means, and never touches money.

**Members.**

| Member | Signature | Why this shape |
|---|---|---|
| `weightSeries` | `static List<Double> weightSeries(List<ProgressRecord> records)` | Takes the **already-fetched** records (so it needs no DAO and is trivially testable) and returns `List~Double~`, the exact type `ProgressService.series` returns and a chart can consume |
| — | — | It has no second member in §6, so a body-fat chart is an **Open detail**, not an invented method (below) |
| (`$` marker) | — | `static`; keep the `$` in the `UML.md` source (`DECISIONS.md` 37) |

**Why it is designed this way.** The alternative — a `ChartUtils` that reaches into `ProgressDao` — would
make a display helper depend on the database, impossible to test without a `gym.db`, and a second place
where "progress" is defined. Taking a `List<ProgressRecord>` keeps the dependency pointing one way:
`ProgressService` fetches, `ChartUtils` maps, the controller plots.

**The honest seam to keep in mind (`ProgressService.series`).** §1 declares two metrics and §4 declares
`series(int customerId, ProgressMetric metric)`, while §6 gives this class only the weight projection.
The clean reading, which keeps both diagrams true:

- `ProgressService.series` owns the **choice of metric** (it is the only class allowed to know that
  `WEIGHT_KG` and `BODY_FAT_PCT` are different columns) and calls `ChartUtils.weightSeries(records)` for
  the weight case;
- `ChartUtils` owns the **mapping and any presentation of the series** (order, one-decimal rounding for a
  tooltip label, an empty series when there is no history).

**Open detail (decide + log in `DECISIONS.md`).** A body-fat chart needs a second projection. Pick one
and write it down: **(a)** add `bodyFatSeries(List<ProgressRecord>)` to `docs/UML.md` §6 (two named
helpers, most readable); **(b)** replace both with
`series(List<ProgressRecord>, ProgressMetric)` (one helper, and `ProgressService` stops mapping); or
**(c)** keep the mapping entirely inside `ProgressService.series` and leave `ChartUtils` with only
axis/text formatting. Do not add the missing helper to the code while §6 has one member — the two
diagrams would describe different classes.

**How to write it.**

1. Create `src/main/java/com/gym/util/ChartUtils.java`, `package com.gym.util;`. Import only
   `com.gym.model.ProgressRecord`, `java.util.List` and `java.util.ArrayList` (or `stream`). **No
   JavaFX, no SQL, no `BigDecimal`** (rule 13).
2. Declare `public final class ChartUtils` with `private ChartUtils() { }`.
3. Write `weightSeries`: return an empty list for a `null` or empty input rather than `null`, map each
   record to `getWeightKg()`, and keep the input order (the DAO's `ORDER BY date` is what makes the
   chart's x-axis chronological — do not sort here and do not re-parse dates, rule 9).
4. Javadoc the precondition in one line: "records must already be ordered by `date`; this method does not
   query and does not sort".
5. Do not add a `main`, a chart-node builder, a currency formatter, a `targetWeight` accessor or a
   `bodyFatSeries` before the open detail is decided.

**Sketch.**

```java
// Sketch — the only signature is authoritative in docs/UML.md §6.
public static List<Double> weightSeries(List<ProgressRecord> records) {
    if (records == null || records.isEmpty()) return List.of();   // an empty chart, never null
    List<Double> weights = new ArrayList<>(records.size());
    for (ProgressRecord r : records) {
        weights.add(r.getWeightKg());       // primitive double in §1, so no null check is needed
    }
    return weights;                          // order preserved: the DAO ordered by ISO date (rule 9)
}
```

**How to know it is right.**

1. Feed three records with known weights and dates in order; the returned list matches the input order,
   not sorted numerically.
2. Feed an empty list and `null`: both give an empty list (the controller shows "no records yet",
   not a crash and not a `NullPointerException` in the chart).
3. Grep the file for `Dao`, `DriverManager`, `BigDecimal` and `javafx`: none may appear.
4. Delete a record from the database and re-plot: the series loses exactly one point, which proves the
   data comes from the service, not from a cached field.

**Common mistakes.**

- **Querying from here.** A `ProgressDao` field or a `DbConnection` parameter turns a formatter into a
  second data layer and breaks the §4 dependency direction.
- **Sorting here.** Re-sorting by `date` (or, worse, by value) contradicts the DAO's ordering and hides
  the fact that two different orders are in play; ordering is a query decision.
- **Returning `null` for "no data".** Every caller then needs a null check, and one forgotten check is a
  crash on the progress screen of a brand-new member who has no records yet.
- **Rounding or formatting into the series.** Keep the raw numbers; format at the axis/tooltip so a
  chart is not built from strings (and never turn money into a formatted string here).
- **Treating `bodyFatPct` as a weight** in a copy-paste of the loop: the two metrics differ per member
  and a swapped column is invisible in a squashed-looking chart.
- **Adding a JavaFX chart node helper.** `ChartUtils` produces data; building a `LineChart` is the
  controller's (view) job, which is what keeps this class testable without a toolkit.

---

## 6. How this layer connects to the others

`util` has no peers: nothing in it calls a service or a controller, and it is the only package every
other package may import. The three paths that matter:

1. **Login (the identity path).** `LoginController.onLogin()` → `AdminService.authenticate(username,
   password)` → `AdminDao.findByUsername` (background SQL) → `PasswordUtils.verify(plain,
   admin.getPasswordHash())` → `SessionContext.setAdministrator(admin)` → next screen. `PasswordUtils`
   is called **only** from `AdminService`; the controller never hashes anything itself.
2. **Failure (the error path).** any DAO → `catch (SQLException e) → throw new DaoException("...", e)` →
   service (does not catch it) → controller → `FxUtils.error(e.getMessage())`. Exactly one class in the
   project turns an exception into words the user reads, and it is in this folder.
3. **Progress (the presentation path).** `ProgressController` → `ProgressService.series(customerId,
   metric)` → `ProgressDao.findByCustomer` → `ChartUtils.weightSeries(records)` → the chart node the
   controller plots.

| This folder depends on | Who depends on this folder |
|---|---|
| `model` — only where a signature names a model type: `Administrator` (`SessionContext`), `ProgressRecord` (`ChartUtils`) | `dao` — `DbConnection`, `DaoException`, `GuardService` (`AdminService` deletes) |
| The JDK — `java.sql` (`DbConnection`, `SqliteDbConnection`), `javax.crypto`/`java.security` (`PasswordUtils`), `java.time` (`DateUtils`) | `service` — `Validators`, `DateUtils`, `FxUtils`-free by rule, `GuardService` |
| `sqlite-jdbc` at runtime (the driver `DriverManager` loads) | `controller` — `FxUtils` only (`../controller/README.md`) |
| — | `MainApp` — `DbConnection`/`SqliteDbConnection` (the composition root) and `SessionContext.clear()` on exit |

**The two seams in this folder.** `DbConnection` is the *storage* seam: DAO impls are typed against the
interface, the concrete `SqliteDbConnection` is named once, in `MainApp` (`DECISIONS.md` 33).
`PasswordUtils` and `DateUtils` are *policy* seams: the algorithm and the date format are decided in one
file each, so changing them is a one-file change with a decision entry (`DECISIONS.md` 31, 40).

**One honest hole.** `SqliteDbConnection` is in this folder but is *not* one of the ten
`«utility»` classes: it holds no static state, it is instantiated exactly once, and it is the only class
in the project that may contain SQLite-specific text (`gym.db`, `PRAGMA`). Its schema half depends on
`src/main/resources/db/schema.sql` existing on the classpath (`../../resources/db/README.md`).

## 7. Common mistakes and review checklist

**Mistakes we expect (symptom → cause → fix).**

| Symptom | Cause | Fix |
|---|---|---|
| `NullPointerException` from `Validators.validEmail(...)` | a `Boolean` was returned and unboxed at a call site | return primitive `boolean` (§4 rule 10) |
| Date strings compare wrongly: `2025-9-2` sorts after `2025-10-01` | a controller or service formatted a date by hand | ISO-8601 only, and only through `DateUtils` (rule 9) |
| A frozen membership expires a day early | month arithmetic done with `+ 30` days | `DateUtils.addMonths` (§7) |
| The cancellation window is wrong by the machine's time zone | `LocalDate.now()` used inside a query builder | keep "today" in one place and log the choice (dao open detail C) |
| The same password hashes to the same string for two members | salt reused or a constant salt | `SecureRandom` per call (§4 rule 11) |
| A correct password is rejected after a deploy | `verify` used today's iteration count instead of the stored one | store and read the parameters with the hash |
| An admin delete removes rows that other tables referenced | the check threw and was treated as "no references" | `GuardService` fails **closed** (§4 rule 13) |
| The next user of the machine is already signed in | `clear()` skipped on logout or on exit | clear in both places (`DECISIONS.md` 34) |
| A dialog appears behind the window, or the app freezes on a failure | a dialog built off the FX thread | `Platform.runLater`, and keep the FX rule in `FxUtils`'s Javadoc |
| A stack trace appears in a dialog | `FxUtils.error(e.toString())` or the exception passed through | pass `e.getMessage()`, log the cause (root `README.md` §11) |
| The progress chart crashes for a new member | `null` returned for an empty series | return an empty list (`ChartUtils`) |
| `gym.db` is locked and the app appears to hang | a `Connection` opened in `util` and never closed | connections are closed by the DAO's `try`-with-resources; this layer only hands them out |
| The `swing` port fails to compile on a `util` class | a JavaFX import slipped in outside `FxUtils` | rule 2: only `FxUtils` may import JavaFX |

**Review checklist for a PR that touches `util`.**

- [ ] No member was added that `docs/UML.md` §6 does not declare (or `UML.md` was changed first).
- [ ] Every class except `FxUtils` compiles with no `javafx.*` import (`grep -r "javafx" util/`).
- [ ] `util` imports neither `com.gym.service` nor `com.gym.controller` (§4 rule 14).
- [ ] Money appearances are `BigDecimal`; no `double` arithmetic on money and no money in `ChartUtils`.
- [ ] Every date/time parse and format goes through `DateUtils`; the strings are `yyyy-MM-dd` / `HH:mm`.
- [ ] `PasswordUtils` uses a random per-user salt, a stored iteration count, and `MessageDigest.isEqual`;
      `PBEKeySpec` is cleared; nothing writes a password or a hash to a log.
- [ ] `DaoException` still chains through `super(message, cause)` and declares no fields.
- [ ] `GuardService` whitelists its `table` argument and fails closed.
- [ ] `SessionContext` is cleared on logout **and** in `MainApp.stop()`, and its Javadoc states FX-thread
      confinement.
- [ ] `FxUtils` takes only `String`/FXML-path parameters and is called on the FX thread.
- [ ] Utility classes are `final` with a private constructor; `SessionContext` is the documented
      exception in shape (stateful), not in style.
- [ ] Every open question this PR had to answer was written into `docs/DECISIONS.md`, not left in a PR
      comment.
- [ ] Status column in §9 updated in the same PR.

## 8. Learning exercises (with answers)

1. **Read and answer.** Why does `hash(String)` return one string instead of the pair
   `(salt, digest)` that a database designer might expect as two columns? *Answer:* one column keeps
   `administrators` simple and forces the parameters to travel with the hash, so the iteration count can
   be raised later without breaking existing rows; `verify` parses the parameters back out of the same
   string.
2. **Write a small piece.** Write the `derive(...)` helper with the `PBEKeySpec` correctly cleared, and
   say what breaks if the `finally` is omitted. *Answer:*
   ```java
   private static byte[] derive(char[] password, byte[] salt, int iterations) {
       PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
       try {
           return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
       } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
           throw new IllegalStateException("PBKDF2 unavailable", e);   // a JDK problem, not a user error
       } finally {
           spec.clearPassword();     // without this the derived password copy stays in the heap
       }
   }
   ```
   Omitting the `finally` leaves the password copy in memory until the GC decides otherwise — an
   avoidable exposure in a crash dump or a heap snapshot.
3. **Find the bug.** `boolean ok = stored.equals(hash(plain));` — what is wrong, and what does the user
   see? *Answer:* `hash` generates a new salt, so the strings can never be equal; every login fails and
   the user sees "Wrong username or password" with a correct password.
4. **Defend a rule.** A teammate proposes `FxUtils.error(Throwable t)` so controllers write one line.
   Give two reasons the project's design is better. *Answer:* (a) the wording is a screen decision and
   `Error` messages must stay user-friendly while the cause belongs in the log; (b) taking a `Throwable`
   makes the UI helper depend on whatever exception types the lower layers produce, and the layering
   rule says the controller translates, `FxUtils` displays.
5. **Extend without breaking rule 13.** The progress screen must also chart body fat. List the two
   acceptable moves and the one forbidden move. *Answer:* acceptable — add a member to `docs/UML.md` §6
   first, or let `ProgressService.series` map the `ProgressMetric` and keep `ChartUtils` for formatting;
   forbidden — quietly adding a `bodyFatSeries` to the code while the diagram still shows one member.
6. **Trace the failure path.** `AttendanceDaoImpl.checkIn` throws because `gym.db` is locked. Name every
   class that touches the exception, in order, and what each does with it. *Answer:* `AttendanceDaoImpl`
   catches `SQLException` and throws `DaoException("Could not check in", e)`; `AttendanceService` lets it
   propagate (it validates inputs, it does not translate persistence failures);
   `AttendanceController.onCheckIn` catches `DaoException` and calls `FxUtils.error(e.getMessage())`;
   `FxUtils` shows one alert with that sentence; the cause reaches the console/log only.

## 9. Ownership and tasks

| File | Responsibility | Owner |
|------|----------------|-------|
| `DbConnection.java` | Interface: `getConnection()`, `initializeSchema()` | Abdelrhman |
| `SqliteDbConnection.java` | SQLite implementation; `gym.db` bootstrap, schema init | Abdelrhman |
| `DaoException.java` | Unchecked `SQLException` wrapper, extends `RuntimeException`, no own fields | Abdelrhman |
| `Validators.java` | Phone/email/number/date and time-range validation; primitive `boolean` | Abdelrhman |
| `SessionContext.java` | Signed-in `Administrator` + `requireRole`; the documented static state | Abdelrhman |
| `PasswordUtils.java` | PBKDF2 hashing and verification, JDK only | Abdelrhman |
| `FxUtils.java` | Dialogs, scene switching, snackbar; the only JavaFX-aware utility | Abdelrhman |
| `DateUtils.java` | Membership periods, session slot math, `hoursUntil`, formatting | Yousef |
| `ChartUtils.java` | Progress-chart series mapping; no DAO, no money | Abdel Raouf |
| `GuardService.java` | `countReferences(table, id)` for business rule 4 | Abdelrhman |

**Tasks.**

| Task | Owner | Status |
|------|-------|--------|
| Skeleton + this README created | Abdelrhman | Done |
| README expanded into learning material (OOP concepts, per-file how-to, open details) | Abdelrhman | Done |
| `DbConnection` + `SqliteDbConnection` + schema init (Week 1, foundation) | Abdelrhman | To Do |
| `DaoException`, `Validators`, `FxUtils`, `SessionContext`, `PasswordUtils` | Abdelrhman | To Do |
| `GuardService` (needed by `AdminService` deletes) | Abdelrhman | To Do |
| `DateUtils` | Yousef | To Do |
| `ChartUtils` | Abdel Raouf | To Do |
| Settle and log the open details marked in this README (PBKDF2 parameters, `switchScene` window lookup, snackbar fallback, `ChartUtils` body-fat projection) | each owner | To Do |

`DbConnection`, `SqliteDbConnection`, `DaoException`, `Validators`, `GuardService`, `DateUtils`,
`PasswordUtils` and `ChartUtils` must stay UI-toolkit-free so the `swing` branch reuses them as-is.
`FxUtils` is the deliberate exception: the port replaces that one file with a Swing equivalent. That
branch has no Java files yet.

### Definition of done for a file in this folder

- [ ] The class matches `docs/UML.md` §6 exactly (members, signatures, `static`, `«utility»`), and every
      deviation was argued into `UML.md` first.
- [ ] Utility shape: `public final class`, `private` constructor, no instance state.
- [ ] Javadoc on the class and on every public member, stating the contract and the caller's
      responsibility (thread, closed connection, cleared buffer, and so on).
- [ ] No `javafx.*` import outside `FxUtils`; no `com.gym.service`/`com.gym.controller` import anywhere.
- [ ] Money is `BigDecimal`, dates and times are the ISO-8601 strings `DateUtils` owns.
- [ ] Secrets are never logged, never returned, and never stored in plain text.
- [ ] Every open question this file raised is recorded in `docs/DECISIONS.md`.
- [ ] The §9 status row is updated in the same PR, and `docs/COVERAGE.md` if the class is finished.

