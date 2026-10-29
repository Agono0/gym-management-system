# `dao` — Data Access Layer (JavaFX branch)

> **Signature authority:** `docs/UML.md` §3 owns every name, type and signature in this folder. This
> README explains *why* each one exists and *how* to write it. Code here is an illustrative sketch,
> not the specification — if a sketch and `UML.md` disagree, `UML.md` wins. **§3 is the full DAO
> surface**: a service uses a subset of it, and nothing in this folder may add a method §3 does not
> have.
>
> **Status: planned.** No `.java` file exists in this folder yet; `docs/COVERAGE.md` is the honest
> built-vs-planned list. The three foundation classes this layer calls (`DbConnection`,
> `SqliteDbConnection`, `DaoException`) are owned by `util/` and documented in
> [`../util/README.md`](../util/README.md) — this file documents only how the DAOs **use** them.

## 1. Purpose — the problem this layer solves

This layer is the **only** place in the application where SQL is written. It holds 26 files: 13
interfaces (`CustomerDao`, `TrainerDao`, …) and 13 implementations (`CustomerDaoImpl`,
`TrainerDaoImpl`, …), one pair per entity. Each interface is a small contract in domain words —
`save`, `findById`, `findByCustomerAndDay`, `revenueBetween` — and each implementation is the JDBC
code that keeps that promise against one SQLite file, `gym.db`. A DAO **maps rows to objects**: it
writes a `Customer` object's fields into a row and later reads a row back into a new object. It
decides nothing about the gym.

**What breaks if this layer is missing.** Put the customer-list `SELECT` inside
`CustomerListController` and four things break at once. First, table names and SQL spread across 16
screens, so a schema change becomes a 16-file change and one forgotten screen fails only at runtime.
Second, no query can be reused: "who is assigned to trainer 7?" runs in the roster screen, the
medical-access check and the reassignment dialog, written three times, each slightly different.
Third, nothing can be tested or faked, because a screen needs a `Stage` before it can run. Fourth,
hand-built SQL strings invite concatenation, and a concatenated parameter is how a search box becomes
`DROP TABLE` or how one member's search returns another member's row. The DAO layer exists so the UI
asks a question in domain words and one file — one place to read, one place to fix — answers it.

**What this layer deliberately does not do.** No **business rules**: no validation, no state
transitions, no pricing, no permissions. No **JavaFX** — not one import, which is also what lets the
`swing` branch reuse every file unchanged. It shows the user **nothing**: a DAO says which operation
failed and carries the technical cause; it never formats a message for a dialog. No **caching and no
lazy loading**: every call hits the file and returns finished objects, which is why entities hold
`trainerId` (a number) and never a `Trainer` object ([`../model/README.md`](../model/README.md)). And
no **transaction control**: none of the 13 interfaces has `begin`/`commit`, so each method is one
statement. If an operation ever needs two statements to be atomic, that is a change to `docs/UML.md`
§3 — not a loop that leaves half a state behind.

`findById`, `findAll` and `delete` exist on almost every interface even where no screen calls them
today, so a future screen does not have to reopen the interface (`docs/UML.md` §3, "Notes on scope").

## 2. What you learn here (OOP concepts)

Concept names are the ones in `docs/LEARNING_GUIDE.md` → "OOP concept index", so the two documents
can be read side by side.

| Concept | Where in this folder | Why the project needs it | How to apply it (one line) |
|---|---|---|---|
| Abstraction / interface as a contract (`LEARNING_GUIDE` §3.3) | The 13 `XxxDao` interfaces | A service must say *what* it needs stored without knowing *how* | Declare the method on the interface; write the SQL only in the impl |
| Programming to an interface | `CustomerDao dao = new CustomerDaoImpl(conn);` — service fields are typed as the interface | The database can be replaced, or faked in a test, without touching 13 services (`DECISIONS.md` 33) | Type field, parameter and variable as `XxxDao`; let `MainApp` name the impl once |
| Runtime polymorphism / dynamic dispatch | Every `XxxDaoImpl implements XxxDao` | The call is resolved by the *object*, so one service works with SQLite today and a fake tomorrow | Never branch on `instanceof`; put the difference in a second implementation |
| Encapsulation + information hiding | SQL text, the `DbConnection` field and `mapRow` are all `private` | A leaked `ResultSet` keeps a cursor and a connection open; leaked SQL gets edited in two places | Expose entities and `List`s only — never a JDBC type, never a table name |
| Single responsibility | One impl per entity; no `GymDao` touching every table | A change to `payments` must touch exactly one file | If a method needs a second table, ask whether it belongs elsewhere |
| Dependency injection by constructor | `public CustomerDaoImpl(DbConnection db)`, stored `private final` | The DAO does not choose its database, so it need not change when the database does | Take collaborators as constructor parameters; never `new` infrastructure inside a DAO |
| Composition over inheritance | 13 impls that each *hold* a `DbConnection` | An object that delegates can be swapped; a subclass hierarchy cannot | Hold the collaborator as a field; do not extend a class to reuse two lines |
| Inheritance — and why it is absent | No `AbstractJdbcDao<T>` (`DECISIONS.md` 36) | A base class would hide the SQL that differs and the 13 explicit `..> DbConnection` edges that make the wiring checkable | Keep the repetition: five CRUD methods per entity, each readable on its own |
| Generics — used, not abused | `List<Customer>`, `List<ProgressRecord>`, `BigDecimal` returns | `List` returns are type-safe with no casts and need no invented wrapper type | Return `List<Entity>`; never `List<Object>` or a raw `List` |
| Generics considered and rejected | No generic base class, no generic `findById` | `T findById(int)` still needs a per-entity mapper, so the generic saves nothing and hides the type | Read `DECISIONS.md` 36 before proposing one |
| Collections as return types | Every `find…` that can match several rows | Callers loop without a null check, and "none found" is a normal answer | Return an empty `List`, never `null`; never return a live `ResultSet` |
| Enums as a type system | `CustomerStatus`, `SessionStatus`, `PaymentMethod`, … bound as `status.name()` | A column stores a readable word, and a new literal needs no DAO change | Bind `enum.name()`; read with `valueOf`; never store `ordinal()` |
| Checked vs unchecked exceptions | `SQLException` is checked; `DaoException` is unchecked (`DECISIONS.md` 40) | A checked `SQLException` would add `throws` to every service and controller, and no UI can act on it | Catch the checked one at the boundary; throw the unchecked one |
| Exception translation + chaining | `catch (SQLException e) { throw new DaoException("…", e); }` | The DAO knows the technical cause; the log needs it and it must not be lost | Pass the `SQLException` as the second argument so `getCause()` works |
| Static vs instance members | All DAOs are instances with a constructor, though a static helper would "work" | A static DAO hard-wires one database into the class and makes a test impossible | Keep state in fields; only genuine helpers are static |
| Value objects and immutability | `BigDecimal` money and `String` dates crossing the boundary | An immutable value cannot be changed behind the DAO's back | Return the value; never hand out an internal mutable structure |
| Package-level cohesion | This package depends on `model`, `util` and `java.sql` only | The package a class lives in tells you what it may depend on | Check the imports before the code in review |

## 3. Design patterns in this layer

| Pattern | Where | Why this pattern | What goes wrong if used carelessly |
|---|---|---|---|
| **DAO (Data Access Object)** | The 13 `XxxDao` / `XxxDaoImpl` pairs | One class per entity is the smallest unit that can hide a table, and it gives services domain words instead of SQL | A DAO that also validates or prices becomes a second service, and the rule gets two homes |
| **Facade over JDBC** | Every impl: `Connection` → `PreparedStatement` → `ResultSet` → entity | Callers ask for a `Customer`, not for rows and cursors | Leaking a JDBC type hands the caller a resource they must close |
| **Dependency injection by constructor** | `new CustomerDaoImpl(conn)` in `MainApp` | The concrete database is chosen once, at the composition root, and is invisible below it | A DAO that builds its own connection creates a second source of truth (`DECISIONS.md` 33) |
| **Strategy at a seam** | The `DbConnection` interface | "Which database" is a strategy; the 13 impls know only the interface | Putting SQLite-only assumptions in a service locks the strategy in |
| **Row Mapper** | One `private Xxx mapRow(ResultSet rs)` per impl | One place maps columns to fields, so a new column is one edit | Mapping inline in five methods means five chances to read a column in the wrong order |
| **Template Method — considered and rejected** | No `AbstractJdbcDao<T>` (`DECISIONS.md` 36) | The shared part (open, close, wrap) is already supplied by `DbConnection` and try-with-resources | A base class would hide the one thing that differs — the SQL — behind a superclass call |
| **Singleton — deliberately avoided** | No `DbProvider.get()` anywhere | A global would make every DAO untestable without a real file and hard-wire SQLite into 26 classes | "It is only one connection" is how a project loses the ability to run a test |
| **Repository — the pattern we are *not* claiming** | None; the name is DAO on purpose | Our contracts are table-shaped CRUD (`findAll`, `delete(int)`); a repository would speak in aggregates and hide those | Calling it a repository while exposing `findAll` sets an expectation it does not meet |

## 4. Rules you must follow (and the bug each rule prevents)

1. **One interface + one impl per entity, named for the *concept*, not always the class.** It is
   `ProgressDao` for `ProgressRecord` and `AdminDao` for `Administrator`; the other eleven match their
   entity class name. A `ProgressRecordDao` would make `ProgressService` read as another feature and
   the folder would stop matching `docs/UML.md` §3.
2. **The impl declares `implements XxxDao` and marks every method `@Override`.** Without it a typo —
   `findbyId` — compiles as a *new* method, the interface method stays unimplemented, and the failure
   appears as an `AbstractMethodError` at runtime instead of a red line in the editor.
3. **The impl takes a `DbConnection` in its constructor and stores it `private final`.** Never call
   `new SqliteDbConnection()` here: the concrete database is named once, in `MainApp`
   (`DECISIONS.md` 33). A DAO that creates its own makes the database a hidden second decision and
   breaks the `swing` port and every test.
4. **SQL lives only here.** Never return a `Connection`, `PreparedStatement`, `ResultSet` or SQL text,
   and never let a service build SQL. Those types are resources: a `ResultSet` that escapes keeps a
   cursor and a connection open, which is how `gym.db` ends up locked.
5. **No business rules.** The DAO must not validate, price, transition a status or check a permission.
   What belongs in the **service** instead: `price == 0` for a subscription-covered session
   (`coveredBySubscription`, rule 11); the payment XOR (`validLink`, rule 12); derived state —
   `AvailabilityStatus.FULL` and both `EXPIRED` literals (rule 13); the session state machine
   (`SchedulingService.canTransition`, rule 18); "delete means deactivate" and the reference check
   (rule 4); check-in eligibility (rule 5). The SQL may encode *what a query means* —
   `revenueBetween` excludes `voided = 1` because a revenue figure containing voided money is simply
   wrong (rule 6) — but never whether an operation is *allowed*.
6. **No JavaFX imports, and no user-facing strings.** `FxUtils` is the only JavaFX-aware utility and
   only controllers depend on it. A DAO message is read by a human through
   `FxUtils.error(e.getMessage())` (`docs/UML.md` §2), so it is one plain sentence naming the
   operation — "Could not save customer" — with the cause attached for the log. Never a stack trace,
   never a table name, never SQL.
7. **Every statement is a `PreparedStatement` with bound parameters.** This is the rule that keeps the
   search box from being an attack:

   ```java
   // WRONG — the user's text becomes SQL. A search for  ' OR 1=1 --  returns every customer,
   // and a name like O'Brien breaks the quoting and the query.
   String sql = "SELECT * FROM customers WHERE fullName LIKE '%" + term + "%'";
   try (Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql)) { … }
   ```

   ```java
   // RIGHT — the SQL text is fixed and compiled once; the value travels as a parameter.
   String sql = "SELECT id, fullName, phone, email, address, dateOfBirth, gender, emergencyName, "
              + "emergencyPhone, status, joinDate, trainerId "
              + "FROM customers WHERE fullName LIKE ? OR phone LIKE ?";
   try (PreparedStatement ps = conn.prepareStatement(sql)) {
       String like = "%" + term + "%";   // the wildcards belong to the VALUE, not to the SQL
       ps.setString(1, like);
       ps.setString(2, like);
       try (ResultSet rs = ps.executeQuery()) { … }
   }
   ```

   The prepared form also fixes the quoting bug for free: `O'Brien` is just a value.
8. **Every resource is closed by try-with-resources, declared `Connection`, then
   `PreparedStatement`, then `ResultSet`.** Java closes them in reverse order automatically, so the
   `ResultSet` goes before its statement and the statement before its connection. One forgotten
   `ResultSet` is the usual cause of `SQLITE_BUSY` / "database is locked" in this project.
9. **`save` returns the generated key.** The `int` in `save(Customer) int` is the new row's id, not an
   update count, and it comes back with the `INSERT`:

   ```java
   try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
       ps.setString(1, c.getFullName());
       // … one setX per placeholder, in the same order as the column list …
       ps.setString(9, c.getStatus().name());
       if (c.getTrainerId() == null) ps.setNull(11, Types.INTEGER); else ps.setInt(11, c.getTrainerId());
       ps.executeUpdate();
       try (ResultSet keys = ps.getGeneratedKeys()) {
           if (keys.next()) return keys.getInt(1);
           throw new DaoException("Could not save customer", null);   // no key = the row is unusable
       }
   }
   ```

   Never follow the insert with `SELECT last_insert_rowid()`: a second round trip, and it only means
   "the last row inserted on this connection" — an assumption you do not want to depend on.
10. **`update` and `delete` are `void`, so a DAO cannot report "0 rows changed".** That is the
    signature §3 gives, and it has a consequence: a caller cannot tell "updated customer 7" from
    "customer 7 does not exist". Where the difference matters — the admin delete path — the service
    checks first with `findById` or `GuardService.countReferences(table, id)` (rule 4).
11. **Money is `BigDecimal`, never `double`** (`DECISIONS.md` 31). `double` cannot represent `0.1`
    exactly, so summing receipts drifts and a revenue report stops reconciling. Build it from a
    `String` or a factory, never a floating-point literal: `new BigDecimal("45.00")` — **not**
    `new BigDecimal(45.00)`, which copies the binary-float error you were avoiding. SQLite has no
    `DECIMAL` type, so the storage form is a decision:

    ```sql
    -- Option A: money as TEXT. Round-trips exactly through BigDecimal.
    amount TEXT NOT NULL                 -- '45.00'
    -- read:  new BigDecimal(rs.getString("amount"))
    -- write: ps.setString(i, amount.toPlainString())

    -- Option B: money as INTEGER cents. Also exact, and SUM() stays exact.
    amountCents INTEGER NOT NULL         -- 4500
    -- read:  BigDecimal.valueOf(rs.getLong("amountCents"), 2)
    -- write: ps.setLong(i, amount.movePointRight(2).longValueExact())
    ```

    It matters most for `PaymentDao.revenueBetween`, which must sum in SQL because §3 has no
    "rows between two dates" method to sum in Java:

    ```sql
    SELECT COALESCE(SUM(amount), 0) FROM payments WHERE voided = 0 AND date BETWEEN ? AND ?
    -- Option A: SQLite has no decimal type to sum in; the running total is a REAL (binary float),
    -- and the exactness BigDecimal bought is lost inside the database.
    -- Option B: SUM() over INTEGER is exact integer addition; convert once, after the sum.
    ```

    > **Open detail (decide + log in `DECISIONS.md`).** TEXT + `new BigDecimal(rs.getString(...))` or
    > INTEGER cents — pick one and write it down, because all five money columns
    > (`trainers.hourlyRate`, `memberships.price`, `sessions.price`, `payments.amount`,
    > `trainer_subscriptions.ratePerPeriod`) and `revenueBetween` must use the same one. Option A is
    > easier to read in a database browser; Option B is the only one whose revenue total in SQL is
    > exact. Neither is in `UML.md`, so the choice is a real decision and `DECISIONS.md` is where it
    > goes.
12. **Dates and times are ISO-8601 strings** — `yyyy-MM-dd`, `HH:mm` (`DECISIONS.md` 31, rule 9) — and
    `DateUtils` owns every parse. No `java.util.Date`, no `LocalDate` in a DAO signature. Comparisons
    then need no conversion, because ISO-8601 sorts lexicographically in the same order as
    chronologically:

    ```sql
    WHERE date = ?                          -- today's list (findByDay)
    WHERE startDate <= ? AND endDate >= ?   -- "active as of this date" (findActive)
    WHERE date >= ? AND date <= ?           -- a revenue range (revenueBetween)
    ORDER BY date, startTime                -- agenda order, and it is also time order
    ```

    Sort `2025-9-2` against `2025-10-01` as text and you see why the format is locked: the unpadded
    form sorts wrong, the padded form cannot.

    > **Open detail (decide + log in `DECISIONS.md`).** `findExpiring(int days)` takes only a number
    > of days, so the DAO must work out "today" itself — `DATE('now','localtime')` in SQL, or a value
    > computed in Java. `DATE('now')` is UTC and can be a day off for a gym that closes after
    > midnight, so whichever form we choose must say `localtime` or say why not. This is the one place
    > a DAO legitimately does date arithmetic, and it deserves a line in `DECISIONS.md`.
13. **A primitive field means a `NOT NULL` column; a boxed field means a nullable one** (`docs/UML.md`
    header). `rs.getInt` on a column holding SQL `NULL` returns `0`, which is indistinguishable from
    a real `0` — for `Customer.trainerId` that reads as "assigned to trainer 0" instead of "no trainer
    assigned":

    ```java
    // RIGHT — a boxed Integer field reads a nullable column
    Integer trainerId = (Integer) rs.getObject("trainerId");   // SQL NULL -> null, not 0
    c.setTrainerId(trainerId);

    // also correct, and portable when you already called a getX
    int raw = rs.getInt("availabilityId");
    Integer availabilityId = rs.wasNull() ? null : raw;

    // RIGHT — a primitive double field reads a NOT NULL column
    p.setWeightKg(rs.getDouble("weightKg"));

    // WRONG — a nullable column through a primitive getter: null silently becomes 0
    s.setAvailabilityId(rs.getInt("availabilityId"));
    ```

    The nullable columns in this model are: `Customer.trainerId`, `Session.availabilityId`,
    `Session.subscriptionId`, `Session.cancelReason`, `Session.sessionNotes`, `Attendance.checkOut`,
    `Payment.membershipId`, `Payment.subscriptionId`, `Payment.sessionId`, `Payment.voidReason`,
    `Payment.voidedDate`, `ProgressRecord.targetWeightKg`, and the free-text fields a profile may
    leave empty. For a `String` column, `getString` already returns `null`, so only the boxed numeric
    and boolean columns need the extra care.
14. **Each impl has exactly one `private Xxx mapRow(ResultSet rs)` helper**, it declares
    `throws SQLException`, and every `find…` uses it:

    ```java
    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setFullName(rs.getString("fullName"));
        c.setStatus(CustomerStatus.valueOf(rs.getString("status")));
        c.setTrainerId((Integer) rs.getObject("trainerId"));   // nullable FK
        return c;
    }
    ```

    Three consequences. The `throws SQLException` is fine because `mapRow` is private and only called
    inside the `try`, so the checked exception still cannot escape the impl. `SELECT` lists are
    written out explicitly — never `SELECT *`, which silently changes meaning when a column is added
    and hides how much data the query moves. Columns are read **by name**, not by index:
    `rs.getString(2)` keeps compiling when the `SELECT` list is reordered, and then returns the phone
    number in the email field.
15. **Enum columns store `name()` as text and are read back with `valueOf`.** Never `ordinal()`:
    inserting a literal in the middle of an enum would renumber every stored row. Storing the word
    also means adding `CASH_ON_DELIVERY` to `PaymentMethod` needs **no DAO change at all** — the
    Open/Closed Principle doing real work (`docs/LEARNING_GUIDE.md` §5, exercise 4). A *renamed*
    literal is different: it needs a data migration, because `valueOf` will throw for the old word.
16. **Return collections, and return them empty.** A `find…` that matches nothing returns an empty
    `List`, never `null`, so callers loop without a null check — "no members in this status" is a
    normal answer. A method returning a **single object** (`findById`, `findByUsername`,
    `findByReceipt`, `findByCustomer` on `MedicalProfileDao`, `findActive`) returns `null` when there
    is no row, because §3 gives it no `Optional` and no "not found" exception; the service turns that
    `null` into a decision, exactly as `AdminService.authenticate` returns `null` for an unknown
    username (rule 16). Never return the live `ResultSet` to dodge the null question.
17. **Never hard-delete a `Customer` or `Trainer` row as a normal operation.** "Delete" for those two
    means `INACTIVE` / end the active records (rule 4). The one caller of `delete(int)` is the admin
    path, which asks `util/GuardService.countReferences(table, id)` first and refuses while the count
    is non-zero — possible only because `schema.sql` declares no `ON DELETE CASCADE`. Without that
    check and without cascades, a delete either fails on the foreign key or leaves orphan rows no
    screen can explain.
18. **`AttendanceDao.delete(int)` and `PaymentDao.delete(int)` exist on the interface and are
    implemented — but no service path calls them.** `Attendance` and `Payment` rows are never deleted
    (rule 19): a wrong time is fixed by `AttendanceService.correctRecord`, wrong money by a void plus
    a new payment (`AdminService.adjustPayment` stores `previousAmount` so the receipt reconciles).
    Implement `delete` faithfully because §3 declares it; do not "enforce" the rule by throwing from
    it — the rule is a service-layer decision — and do not add a `deleted` flag to the table. The
    review check is a grep for callers.

### Open details this folder must settle before the first impl is merged

| # | Question | Where it is decided | Why it cannot be left to each DAO |
|---|---|---|---|
| A | Money storage: TEXT + `BigDecimal(String)`, or INTEGER cents | `DECISIONS.md` (append a numbered entry) | Five money columns and `revenueBetween` must agree, or a total mixes scales |
| B | Connection lifetime: does `SqliteDbConnection.getConnection()` return a new connection per call (which the DAO closes) or a shared one? | `DECISIONS.md` + [`../util/README.md`](../util/README.md) | It decides where `PRAGMA foreign_keys = ON` runs and whether try-with-resources closes the database |
| C | Who computes "today" for `findExpiring(int)`: SQL `DATE('now','localtime')` or Java | `DECISIONS.md` | A UTC-versus-local mix-up turns a 7-day reminder sweep into 6 or 8 days |

## 5. The files in this folder

**Write order** (foundation first, then one vertical slice after another, so each owner can work
without waiting): the schema → the connection pair → the exception → then the 13 pairs in this order —
`Customer`, `Trainer`, `Membership`, `WorkoutProgram`, `Session`, `Equipment`, `Attendance`,
`Payment`, `Progress`, `MedicalProfile`, `Admin`, `TrainerSubscription`, `TrainerAvailability`. That
is also the order in the team's task table (§9), so it is the order in which work is actually
unblocked: `Customer` first because it is the hub the other tables point at, then the money tables
once `Customer` and `Membership` can be saved at all.

Owners and status per file are in §9 (that is the team's tracker — one copy only).

### Queries the business rules depend on

Eight methods exist because a rule cannot be implemented without them, and they are the ones most
easily forgotten. Without them the service that needs them looks like it is over-complicating a
simple lookup.

| DAO method | Rule it enables | What makes it non-obvious |
|---|---|---|
| `AttendanceDao.findByCustomerAndDay(int, String)` | One check-in record per customer per day (rule 5) | It returns a **list**, so a stray duplicate row cannot be silently dropped |
| `SessionDao.findByCustomerAndDay(int, String)` | Customer-side double-booking detection | Same day, same customer, any trainer — the conflict decision is the service's |
| `SessionDao.countByAvailability(int, SessionStatus)` | Slot capacity check, rule 17 | Pass `CONFIRMED` so cancelling or completing a session frees capacity |
| `WorkoutProgramDao.findLatestVersion(int)` | Per-customer program versioning | Highest version wins; the next program is that value + 1 |
| `PaymentDao.revenueBetween(String from, String to)` | Revenue by range, voided excluded (rule 6) | Sums in SQL, so the money storage decision (rule 11) decides whether it is exact |
| `PaymentDao.findByReceipt(String)` | Looking a receipt up again | Returns one payment, so `receiptNo` has to be unique |
| `MembershipDao.findByStatus(MembershipStatus)` | Expiry sweep and frozen memberships (rules 7, 13) | `EXPIRED` is derived; the stored value can be stale, so the date window matters too |
| `TrainerSubscriptionDao.findActive(int, int, String)` | "Already subscribed to this trainer?" | Two ids **and** an as-of date; status alone is not enough (rule 13) |

`§3` is the **full DAO surface**: a service uses a subset. `findById`, `findAll` and `delete` exist so
a future screen does not have to reopen the interface.

### `schema.sql` — the 13 tables every query is written against (owner: Abdelrhman)

**Status:** planned Week 1 foundation. **Canonical:** `docs/UML.md` §1 for the field lists; the DDL
itself is written here. It lives in `src/main/resources/db/`
([`../../../../resources/README.md`](../../../../resources/README.md)); `gym.db` is git-ignored and
only `schema.sql` is committed. It is documented in this README because every `SELECT` in this folder
is written against it.

**What it is.** The SQLite DDL for the whole application: 13 tables, one per entity, plus the indexes
the queries need and the `PRAGMA` that makes the foreign keys real. `SqliteDbConnection.
initializeSchema()` runs it on startup. Table names are plural `snake_case`; column names are the
entity field names in `camelCase`, which is what makes `mapRow` mechanical. Nothing else may create a
table — a DAO that issues `CREATE TABLE` is a second schema.

| Table | Columns | Primary key | Index worth having |
|---|---|---|---|
| `customers` | id, fullName, phone, email, address, dateOfBirth, gender, emergencyName, emergencyPhone, status, joinDate, trainerId → `trainers(id)` (nullable) | `id` | `(status)`, `(trainerId)` for the roster |
| `trainers` | id, fullName, phone, email, specialization, experienceYears, certifications, hireDate, hourlyRate | `id` | — (the roster is small; `findAll` is fine) |
| `memberships` | id, customerId → `customers(id)`, plan, price, startDate, endDate, status, frozenDays | `id` | `(status, endDate)` for the expiry sweep, `(customerId)` |
| `workout_programs` | id, customerId → `customers(id)`, creatorTrainerId → `trainers(id)`, name, exercises, version, createdDate | `id` | unique `(customerId, version)` — §1b says version is unique per customer |
| `sessions` | id, customerId → `customers(id)`, trainerId → `trainers(id)`, date, startTime, durationMin, status, availabilityId → `trainer_availability(id)` (nullable), subscriptionId → `trainer_subscriptions(id)` (nullable), price, cancelReason, sessionNotes | `id` | `(trainerId, date)`, `(customerId, date)`, `(availabilityId)` |
| `equipment` | id, name, category, purchaseDate, condition, status, lastMaintenance | `id` | `(status)`, `(condition)` |
| `attendance` | id, customerId → `customers(id)`, date, checkIn, checkOut (nullable) | `id` | `(customerId, date)` for the one-per-day lookup, `(date)` for the daily report |
| `payments` | id, customerId → `customers(id)`, membershipId / subscriptionId / sessionId (all nullable), amount, method, date, receiptNo, voided, voidReason, voidedDate, previousAmount | `id` | `(date)` for `revenueBetween`, `(customerId)`, unique `(receiptNo)` |
| `progress_records` | id, customerId → `customers(id)`, date, weightKg, bodyFatPct, measurements, targetWeightKg (nullable), notes | `id` | `(customerId, date)` for the chart series |
| `medical_profiles` | id, customerId → `customers(id)`, bloodType, conditions, allergies, medications, injuries, doctorName, doctorPhone, notes, updatedDate | `id` | unique `(customerId)` — rule 3 |
| `administrators` | id, fullName, username, passwordHash, role, phone, email, active, createdDate | `id` | unique `(username)` — the login lookup |
| `trainer_subscriptions` | id, customerId → `customers(id)`, trainerId → `trainers(id)`, startDate, endDate, ratePerPeriod, status | `id` | `(customerId, trainerId)` for `findActive` |
| `trainer_availability` | id, trainerId → `trainers(id)`, date, startTime, endTime, maxCustomers, status | `id` | `(trainerId, date)` for `findOpenSlots` and `findByTrainerAndDate` |

The four tables specified in Week 1, kept verbatim so nobody re-derives them:

> `medical_profiles(id, customerId, bloodType, conditions, allergies, medications, injuries,
> doctorName, doctorPhone, notes, updatedDate)`,
> `administrators(id, fullName, username, passwordHash, role, phone, email, active, createdDate)`,
> `trainer_subscriptions(id, customerId, trainerId, startDate, endDate, ratePerPeriod, status)`,
> `trainer_availability(id, trainerId, date, startTime, endTime, maxCustomers, status)`.

**Payments need `CHECK` constraints, not just columns.** At most one of
`membershipId` / `subscriptionId` / `sessionId` may be non-null, and `voided` defaults to `0`:

```sql
-- Sketch — the DDL is authoritative here; the field list is docs/UML.md §1.
CREATE TABLE payments (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    customerId     INTEGER NOT NULL REFERENCES customers(id),
    membershipId   INTEGER REFERENCES memberships(id),              -- XOR link, rule 12
    subscriptionId INTEGER REFERENCES trainer_subscriptions(id),
    sessionId      INTEGER REFERENCES sessions(id),
    amount         TEXT    NOT NULL,                                -- money: rule 11 (open detail A)
    method         TEXT    NOT NULL,                                -- PaymentMethod.name()
    date           TEXT    NOT NULL,                                -- 'yyyy-MM-dd'
    receiptNo      TEXT    NOT NULL,
    voided         INTEGER NOT NULL DEFAULT 0,                      -- 0/1, not a boolean type
    voidReason     TEXT,
    voidedDate     TEXT,
    previousAmount TEXT,                                            -- adjustPayment keeps the old figure
    CHECK ((membershipId IS NOT NULL) + (subscriptionId IS NOT NULL)
           + (sessionId IS NOT NULL) <= 1)                          -- rule 12, at most one
);

CREATE UNIQUE INDEX idx_payments_receipt ON payments(receiptNo);
CREATE INDEX idx_payments_date ON payments(date);
```

`IS NOT NULL` evaluates to `0`/`1` in SQLite, so the three tests can be added. The `CHECK` enforces
**at most one** link; rule 12 requires **exactly one**, and that half stays in
`PaymentService.validLink` on purpose: a `CHECK` failure arrives as a `SQLException` wrapped in a
`DaoException` naming a constraint, while `validLink` refuses the specific combination with a message
about the links the user picked. A partial index for the hot case is possible
(`CREATE INDEX … WHERE voided = 0`); we skip it — the table is small and an index per query is how a
schema stops being readable.

```sql
-- Sketch — the two "never deleted" tables, the busiest indexes, and the pragma.
CREATE TABLE attendance (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    customerId INTEGER NOT NULL REFERENCES customers(id),
    date       TEXT    NOT NULL,          -- 'yyyy-MM-dd', rule 9
    checkIn    TEXT    NOT NULL,          -- 'HH:mm'
    checkOut   TEXT                       -- nullable: still inside (boxed field in the model)
);
CREATE INDEX idx_attendance_customer_date ON attendance(customerId, date);

CREATE INDEX idx_sessions_trainer_date  ON sessions(trainerId, date);   -- the agenda
CREATE INDEX idx_sessions_customer_date ON sessions(customerId, date);  -- double-booking

PRAGMA foreign_keys = ON;   -- SQLite defaults to OFF; nothing above is enforced without it
```

**Why it is written this way.**

- **`INTEGER PRIMARY KEY AUTOINCREMENT`.** SQLite reuses the id of a deleted row unless
  `AUTOINCREMENT` is declared. Ids travel into dialogs and, for money, into receipts, so a reused id
  can make an old reference point at a new row. The cost is one `sqlite_sequence` table.
- **No `ON DELETE CASCADE` anywhere.** "Delete" means deactivate (rule 4), and the admin hard delete
  refuses while references exist (`util/GuardService.countReferences`). A cascade would delete a
  customer's payment history silently, and then a receipt could not be reconciled (rule 19).
- **`PRAGMA foreign_keys = ON`.** SQLite has foreign keys, but they are **off by default and the
  setting is per connection** — declaring `REFERENCES` without the pragma gives a schema that looks
  enforced and is not. It must run on every connection the app opens, which is why open detail B
  (connection lifetime) is a schema question as much as a `util` one.
- **`date`, `status`, `method`, `conditions` are fine as column names.** They are function names in
  SQLite, not reserved words, so no quoting is needed. Check the keyword list before adding one that
  is (`order`, `group`, `index`, `references` would need `"…"`).

**How to know it is right.**

1. `gradlew run` creates `gym.db` on first run, and the second run does not fail on `CREATE TABLE`
   (use `CREATE TABLE IF NOT EXISTS`).
2. `PRAGMA foreign_keys;` returns `1` on the connection a DAO uses — not on a database-browser
   connection you opened by hand.
3. `PRAGMA index_list('sessions');` shows the two composite indexes; `PRAGMA table_info('payments');`
   shows `voided` defaulting to `0` and the three nullable link columns.
4. Insert a payment with two links set: SQLite refuses it (the `CHECK`). Insert one with zero links:
   SQLite accepts it, which is correct — rule 12's "exactly one" is the service's job.
5. `EXPLAIN QUERY PLAN SELECT * FROM sessions WHERE trainerId = 1 AND date = '2025-05-04';` mentions
   `idx_sessions_trainer_date` instead of a full scan.

**Common mistakes.**

- **One `CREATE TABLE` per feature branch.** Three people adding tables in three files means
  `initializeSchema()` runs one of them. One file, and `IF NOT EXISTS`.
- **Forgetting `PRAGMA foreign_keys = ON`.** Every `REFERENCES` becomes decoration and orphans become
  possible.
- **Making the nullable links `NOT NULL`, or a required column nullable.** `Payment.customerId` is
  required; the three link columns are not. The boxed/primitive rule in the model is the schema rule.
- **Storing `voided` as `'true'`/`'false'` text.** Comparisons then run against `'false'`, which is
  not false; use `0`/`1` and `rs.getInt("voided") != 0`.
- **Adding a column without touching the DAO.** The `INSERT` column list and `mapRow` change together,
  and `SELECT *` hides the omission.

### `DbConnection.java` + `SqliteDbConnection.java` — the seam every DAO depends on (owner: Abdelrhman, in `util/`)

**Status:** planned Week 1 foundation. **These two files belong to `util/`** and are documented in
full in [`../util/README.md`](../util/README.md) — what follows is only how a DAO uses them. Do not
duplicate the `util` doc here. **Canonical:** `docs/UML.md` §3 and §6: `getConnection() Connection`,
`initializeSchema() void`; `DECISIONS.md` 33 is what makes the interface the seam.

**What it is.** `DbConnection` is a *factory* for JDBC connections. A DAO asks it for a `Connection`,
uses it inside try-with-resources, and never learns whether that connection came from a SQLite file, a
pool or a test double. `SqliteDbConnection` is the one implementation the app ships: it points at
`gym.db`, creates the file on first run and runs `schema.sql` through `initializeSchema()`.

```java
// Sketch — this is the shape of every method in every impl in this folder.
public class CustomerDaoImpl implements CustomerDao {

    private final DbConnection db;   // the INTERFACE, never SqliteDbConnection (DECISIONS.md 33)

    public CustomerDaoImpl(DbConnection db) {
        this.db = db;
    }

    @Override
    public Customer findById(int id) {
        String sql = "SELECT id, fullName, phone, email, address, dateOfBirth, gender, emergencyName, "
                   + "emergencyPhone, status, joinDate, trainerId FROM customers WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;   // one object: null means "no such row"
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load customer " + id, e);   // translation, rule 7
        }
    }
}
```

**Why the impls depend on the interface.**

- **It is the swappability seam** (`DECISIONS.md` 33). Moving to PostgreSQL, or pointing a test at an
  in-memory database, is one new `DbConnection` implementation instead of an edit in 26 files.
- **It keeps the concrete class named once.** `new SqliteDbConnection()` appears in `MainApp` (and in
  a test's setup), nowhere else. A DAO that names it has made a deployment decision it has no business
  making ([`../README.md`](../README.md)).
- **It makes a DAO constructible without a database file.** `new CustomerDaoImpl(fakeDb)` is a
  two-line test double, which is the entire reason a service can be unit tested at all.
- **It gives the DAO two things and no more.** A DAO may call `getConnection()`; it must **not** call
  `initializeSchema()` — that is startup work owned by `MainApp`, and re-initialising the schema on a
  query path is a bug waiting for a race.

**How to know it is right.** Swap in a `DbConnection` that hands out an in-memory SQLite connection
and run the DAO's methods against it: nothing in the impl changes, which is the property the seam
exists for. In review, `grep -r "SqliteDbConnection" src/main/java` returns `MainApp` and `util` only.

**Common mistakes.**

- **Declaring the field as `SqliteDbConnection`.** It compiles, and every test then needs a real file.
- **Calling `db.getConnection()` without closing it.** `gym.db` stays locked and the next screen fails
  with `SQLITE_BUSY`; always the try-with-resources header.
- **Caching the `Connection` in a field "to avoid opening it again".** A JDBC `Connection` is not
  thread-safe and is not meant to live for the process.
- **Calling `initializeSchema()` from a DAO constructor.** It runs DDL on a request path and hides a
  startup failure inside the first query.

### `DaoException.java` — the unchecked wrapper every impl throws (owner: Abdelrhman, in `util/`)

**Status:** planned Week 1 foundation, owned by `util/` and documented there
([`../util/README.md`](../util/README.md)). Kept here only because this is the layer that throws it.
**Canonical:** `docs/UML.md` §6 — one constructor, `DaoException(String message, Throwable cause)`.
`DECISIONS.md` 40: it `extends RuntimeException`, passes both arguments to `super` and declares no
fields, so `getMessage()`/`getCause()` are inherited and the stack trace survives.

```java
try (Connection conn = db.getConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setInt(1, customerId);
    ps.executeUpdate();
} catch (SQLException e) {
    throw new DaoException("Could not save customer", e);   // message for the user, cause for the log
}
```

- **`SQLException` is checked**, so letting it out would add `throws SQLException` to every service
  and controller signature — a change in 40 classes to report a failure none of them can handle.
- **The UI cannot act on it.** `SQLITE_BUSY` is not something a receptionist fixes by clicking OK, so
  the layer above sees one predictable unchecked type and shows one readable sentence.
- **The cause must be chained.** `DECISIONS.md` 40 records the first version of this class, which
  declared its own `message`/`cause` fields and passed nothing to `super`; the trace that says *which*
  statement failed was lost. `super(message, cause)` keeps it.
- **The message is the sentence a human reads** (`FxUtils.error(e.getMessage())`, `docs/UML.md` §2): a
  short statement of the operation that failed. Never SQL, never a table name, never a stack trace.

**Common mistakes.** `new DaoException(e.getMessage(), e)` (a driver string such as
`[SQLITE_CONSTRAINT] …` reaches the dialog); a bare `RuntimeException`, so the layer above cannot tell
a database failure from a bug; and swallowing the exception into `return null`, which turns a locked
database into "customer not found".

### `CustomerDao.java` + `CustomerDaoImpl.java` — the hub table (owner: Abdelrhman)

**Status:** planned. **Canonical:** `docs/UML.md` §3 (8 methods), §1 (`Customer`), rules 4, 5 and 15.

**What it is.** The contract for the `customers` table — the one table every other entity points at. It
is also the table with the only nullable foreign key in the model (`trainerId`), which makes it the
best first DAO: it contains every JDBC problem this layer has (nullable column, enum column, search,
generated key) at the smallest size.

**Methods** (exactly as §3 declares them; the impl repeats them all with `@Override`):

| Method | Returns | Why it exists / rule it serves |
|---|---|---|
| `save(Customer)` | `int` (new id) | Register a member; the id is the key every other table will store |
| `findById(int)` | `Customer` | Load one member; `null` when the row is gone |
| `search(String)` | `List<Customer>` | The list screen's one search box — name or phone |
| `findAll()` | `List<Customer>` | The unfiltered list; also the admin path's first look |
| `update(Customer)` | `void` | Contact details, status, reassignment |
| `delete(int)` | `void` | Admin-only hard delete; rule 4 — the service asks `GuardService` first |
| `findByStatus(CustomerStatus)` | `List<Customer>` | Active/inactive/suspended lists; the suspend flow starts here (rule 14) |
| `findByTrainer(int)` | `List<Customer>` | The trainer's roster — and, through rule 15, the medical-access set |

**Why it is designed this way.** `search` takes one string rather than separate name/phone parameters
because the screen has one box, and matching either field is a *query meaning*, not a business rule.
`findByTrainer` is not "the trainer's customers" as a business fact — `Customer.trainerId` is
authoritative (rule 15) — so the roster and the medical-access check are the same query, which is why
there is one method and not two. `delete` exists even though rule 4 makes it rare: §3 declares it, and
leaving it out would push the admin path into writing its own SQL.

**How to write it.**

1. Create the interface in `com.gym.dao` with the eight signatures exactly as §3 prints them — no extra
   parameter, no renamed method.
2. Create the impl: `public class CustomerDaoImpl implements CustomerDao`, a `private final
   DbConnection db`, and a constructor that takes it.
3. Write `mapRow(ResultSet)` first (rule 14): `id`, `fullName`, `phone`, `email`, `address`,
   `dateOfBirth`, `gender`, `emergencyName`, `emergencyPhone`, `CustomerStatus.valueOf(...)`,
   `joinDate`, and `(Integer) rs.getObject("trainerId")`.
4. Write the five CRUD methods, each one statement, each in try-with-resources, each wrapping
   `SQLException` in a `DaoException` naming the operation — and never touching `joinDate`, which the
   service supplies.
5. Write `search` with `LIKE ?` twice and the `%` characters added to the bound value (rule 7),
   `findByStatus` binding `status.name()`, and `findByTrainer` binding the id.
6. `SELECT` explicit columns everywhere and `ORDER BY fullName` on the list methods, so the screen has
   a stable order without sorting in the controller.
7. Re-read the eight signatures against `docs/UML.md` §3 before committing.

**Sketch** — the two methods that carry the interesting problems:

```java
@Override
public int save(Customer c) {   // sketch — signatures are authoritative in docs/UML.md §3
    String sql = "INSERT INTO customers (fullName, phone, email, address, dateOfBirth, gender, "
               + "emergencyName, emergencyPhone, status, joinDate, trainerId) "
               + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Connection conn = db.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        ps.setString(1, c.getFullName());
        // … fields 2–8 …
        ps.setString(9, c.getStatus().name());          // enum as text, never ordinal()
        ps.setString(10, c.getJoinDate());              // 'yyyy-MM-dd' from the service
        if (c.getTrainerId() == null) ps.setNull(11, Types.INTEGER);   // "no trainer" is NULL, not 0
        else ps.setInt(11, c.getTrainerId());
        ps.executeUpdate();
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) return keys.getInt(1);
            throw new DaoException("Could not save customer", null);
        }
    } catch (SQLException e) {
        throw new DaoException("Could not save customer", e);
    }
}

@Override
public List<Customer> search(String term) {
    String sql = "SELECT id, fullName, phone, email, address, dateOfBirth, gender, emergencyName, "
               + "emergencyPhone, status, joinDate, trainerId FROM customers "
               + "WHERE fullName LIKE ? OR phone LIKE ? ORDER BY fullName";
    List<Customer> found = new ArrayList<>();
    try (Connection conn = db.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        String like = "%" + term + "%";                 // wildcards in the value, not in the SQL
        ps.setString(1, like);
        ps.setString(2, like);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) found.add(mapRow(rs));
        }
    } catch (SQLException e) {
        throw new DaoException("Could not search customers", e);
    }
    return found;                                       // empty list, never null
}
```

**How to know it is right.**

1. Save a customer with `trainerId == null` and read it back: `getTrainerId()` is `null`, not `0`.
2. Save one with `trainerId = 3`; it reads back `3`, and `findByTrainer(3)` returns it.
3. `search("O'Brien")` returns that row — the apostrophe no longer breaks the query (rule 7).
4. `search("")` returns everything; `search("zzz")` returns an empty list and no exception.
5. `save` returns the id that `findById` then finds, on the first row and on the hundredth.
6. `findByStatus(CustomerStatus.SUSPENDED)` returns exactly the suspended rows; if `valueOf` throws, a
   row holds a literal that is not in the enum (a data problem, not a mapping problem).

**Common mistakes.**

- **`rs.getInt("trainerId")`.** The most expensive small bug in this folder: an unassigned customer
  becomes "assigned to trainer 0", the roster query misses them, and rule 15's medical access then
  answers for the wrong trainer.
- **Building the `LIKE` pattern into the SQL string.** `LIKE '%" + term + "%'` compiles, passes a
  manual test and is an injection hole; bind it (rule 7).
- **Putting validation here** ("phone must be 11 digits"). The service validates; the DAO stores what
  it is given, or a legacy row can never be loaded.
- **Returning `null` from `search` when nothing matches, or forgetting `ORDER BY`** — the list order
  would then depend on SQLite's internal row order and change after a `VACUUM`.

### `TrainerDao.java` + `TrainerDaoImpl.java` — the smallest pair (owner: Ziad)

**Status:** planned. **Canonical:** `docs/UML.md` §3 (5 methods), §1 (`Trainer`), rule 4.

**What it is.** The contract for the `trainers` table. It is the smallest DAO in the folder — five
methods, no query beyond `findAll` — and the right second file to write, because it proves the CRUD
shape before the interesting queries start.

**Methods.**

| Method | Returns | Why it exists / rule it serves |
|---|---|---|
| `save(Trainer)` | `int` (new id) | Add a coach; the id is what `Customer.trainerId`, `Session.trainerId`, `WorkoutProgram.creatorTrainerId` and `TrainerAvailability.trainerId` all store |
| `findById(int)` | `Trainer` | One coach's profile |
| `findAll()` | `List<Trainer>` | The roster screen's list |
| `update(Trainer)` | `void` | Contact details, specialization, `hourlyRate` |
| `delete(int)` | `void` | Admin-only hard delete; rule 4 — "delete" for a trainer normally means ending the assignments |

**Why it is designed this way.** There is no `findBySpecialization` and no `search`, and that is the
honest reading of §3: the roster is small enough that `findAll` answers it. If a screen genuinely
needs a filter, the method goes into `docs/UML.md` §3 **first** (§8 exercise 4) — the surface is the
design, and a DAO that grows a method the diagram does not have is a second design. `hourlyRate` is the
first money column in the project, so the money storage decision (rule 11, open detail A) shows up
here first.

**How to write it.**

1. Interface with the five signatures verbatim from §3; nothing extra, not even a convenience
   `findByName`.
2. Impl with the `DbConnection` field, the constructor, and `@Override` on all five.
3. `mapRow`: `id`, `fullName`, `phone`, `email`, `specialization`, `experienceYears` (`getInt` — a
   primitive field, so a `NOT NULL` column), `certifications`, `hireDate`, then `hourlyRate` through
   the money form chosen in open detail A, read in one place so the whole project can change together.
4. `save` with `RETURN_GENERATED_KEYS`; `findAll` with `ORDER BY fullName`; `update` and `delete` by
   `id`; `findById` returns `null` when there is no row.
5. Wrap every `SQLException` in a `DaoException` naming the operation.

```java
private Trainer mapRow(ResultSet rs) throws SQLException {   // sketch — §3 owns the signatures
    Trainer t = new Trainer();
    t.setId(rs.getInt("id"));
    t.setFullName(rs.getString("fullName"));
    t.setSpecialization(rs.getString("specialization"));
    t.setExperienceYears(rs.getInt("experienceYears"));           // primitive -> NOT NULL column
    t.setHireDate(rs.getString("hireDate"));
    t.setHourlyRate(new BigDecimal(rs.getString("hourlyRate")));  // open detail A: TEXT form
    return t;
}
```

**How to know it is right.** Save a trainer; `findAll` returns it in name order; `findById` returns the
same rate to the cent; and deleting a trainer who has sessions is refused by the service's
`GuardService.countReferences` check rather than by a database error.

> **`BigDecimal` comparison trap.** `new BigDecimal("45.00").equals(new BigDecimal("45.0"))` is
> `false` — `equals` compares scale as well as value. Compare money with `compareTo(...) == 0`, and
> remember this when asserting a rate read back from the database.

**Common mistakes.**

- **Adding `findBySpecialization` to the impl only.** The service holds a `TrainerDao` reference and
  cannot call it; the method is invisible. Surface changes go through `docs/UML.md` §3.
- **Storing `hourlyRate` as a `double` "because it is only a rate".** It is money, and it will be
  multiplied by sessions and summed on the dashboard (`DECISIONS.md` 31).
- **Reading `experienceYears` through `getObject`.** It is a primitive field, so the column is
  `NOT NULL`; `getObject` only adds a null check that can never fire.
- **Deleting a trainer row in a normal flow.** Their sessions, availability and programs still point at
  the id; rule 4 says end the assignments instead.

### `MembershipDao.java` + `MembershipDaoImpl.java` — plans, expiry and the frozen state (owner: Ziad)

**Status:** planned. **Canonical:** `docs/UML.md` §3 (8 methods), §1 (`Membership`), rules 7, 13 and 14.

**What it is.** The contract for the `memberships` table: who bought which plan, from when to when, and
whether it is currently active, frozen, expired or cancelled. Membership is *gym access* — not the same
thing as a trainer subscription, which is a paid engagement with one coach
([`../service/README.md`](../service/README.md)).

**Methods.**

| Method | Returns | Why it exists / rule it serves |
|---|---|---|
| `save(Membership)` | `int` (new id) | Sell a plan |
| `findExpiring(int)` | `List<Membership>` | The reminder sweep: memberships ending within N days (rule 7's renewal prompt) |
| `findById(int)` | `Membership` | Load one plan |
| `findAll()` | `List<Membership>` | The membership screen's list |
| `update(Membership)` | `void` | Renew, freeze, unfreeze, cancel, status changes |
| `delete(int)` | `void` | Admin-only; a cancelled plan is normally kept for history |
| `findByCustomer(int)` | `List<Membership>` | A member's plan history |
| `findByStatus(MembershipStatus)` | `List<Membership>` | `ACTIVE` for the expiry sweep, `FROZEN` for the suspended-member flow (rules 13, 14) |

**Why it is designed this way.** `findExpiring(int)` takes a number of days rather than two dates
because its caller is a reminder sweep ("ending within a week"), not a report; the DAO derives the
window, which is exactly why open detail C exists — the lower bound has to be *local* today. The stored
`status` is not the whole truth: `MembershipStatus.EXPIRED` is derived from `endDate` and never set by
hand (rule 13), so a sweep looks at the date as well as the word, and `findByStatus` must not be read
as "these rows are definitely still valid". `frozenDays` is stored, not derived: it is the pause length
that `unfreeze` adds to `endDate` (rule 7).

**How to write it.**

1. Interface with the eight signatures from §3; `save`'s `int` is the new id.
2. Impl with the `DbConnection` field and the constructor; `mapRow` maps `customerId`, `plan`
   (`PlanType.valueOf`), `price` (money), `startDate`, `endDate`, `status`
   (`MembershipStatus.valueOf`) and `frozenDays` (`getInt`).
3. `findByCustomer` and `findByStatus` bind one parameter each; order history lists by `endDate DESC`
   so the current plan is first.
4. `findExpiring` uses `status = ?` **and** the date window, with the lower bound on local today
   (open detail C):

   ```java
   // Sketch — the window's lower bound is the part under discussion (open detail C).
   String sql = "SELECT id, customerId, plan, price, startDate, endDate, status, frozenDays "
              + "FROM memberships "
              + "WHERE status = ? AND endDate >= DATE('now','localtime') "
              + "AND endDate <= DATE('now','localtime', '+' || ? || ' day') ORDER BY endDate";
   ```

5. Bind `MembershipStatus.ACTIVE.name()` first — the sweep is about plans that could still be renewed,
   and `findByStatus` already covers asking for any other literal.
6. Wrap `SQLException`; return an empty list from `findExpiring` when nothing is due.

**How to know it is right.**

1. Insert a plan ending today + 5 days and one ending today + 40 days; `findExpiring(7)` returns the
   first only.
2. Compare `DATE('now')` with `DATE('now','localtime')` by hand: the sweep must not shift by a day near
   local midnight.
3. `findByStatus(MembershipStatus.FROZEN)` returns the frozen plans with `frozenDays` unchanged.
4. `findByCustomer(12)` returns every plan for member 12, newest first, and an empty list for a member
   with none.
5. `price` survives a round trip to the cent (compared with `compareTo`).

**Common mistakes.**

- **Reading `status` and trusting it.** A row can say `ACTIVE` past its `endDate` (rule 13 derives
  `EXPIRED` on read), so any rule about validity must compare dates too.
- **`DATE('now')` without `localtime`.** UTC is a day ahead or behind for part of the day, so a 7-day
  sweep becomes 6 or 8 days for a gym that closes after midnight.
- **Reformatting a stored date with `DATE()`/`strftime()` in the `SELECT` list.** That breaks the plain
  string comparison the rest of the layer relies on (rule 9).
- **Computing `frozenDays` in SQL** (for example `frozenDays + 1` inside an `UPDATE`). The pause policy
  belongs to `MembershipService`; the DAO writes the value it was given.

### `WorkoutProgramDao.java` + `WorkoutProgramDaoImpl.java` — programs and per-customer versioning (owner: Yousef)

**Status:** planned. **Canonical:** `docs/UML.md` §3 (7 methods), §1 (`WorkoutProgram`), and the §1b
note "version is unique per customer".

**What it is.** The contract for the `workout_programs` table: a named exercise list assigned to one
customer, created by one trainer, with a version number. A program is never edited in place once issued
— a change is a new version (`WorkoutService.newVersion`, [`../service/README.md`](../service/README.md))
— so this is the pair that must answer "what is the highest version for this customer?".

**Methods.**

| Method | Returns | Why it exists / rule it serves |
|---|---|---|
| `save(WorkoutProgram)` | `int` (new id) | Issue a program |
| `findById(int)` | `WorkoutProgram` | Load one program |
| `findAll()` | `List<WorkoutProgram>` | The program screen's list |
| `update(WorkoutProgram)` | `void` | Correcting a mistake **before** the program is issued |
| `delete(int)` | `void` | Admin-only; a superseded version is normally kept |
| `findByCustomer(int)` | `List<WorkoutProgram>` | A member's program history |
| `findLatestVersion(int)` | `int` | The next version number; the only aggregate query in this pair |

**Why it is designed this way.** `findLatestVersion(int)` returns a primitive `int`, so it cannot answer
`null` for "this customer has no programs at all". `COALESCE(MAX(version), 0)` makes the empty case `0`,
so the service's `findLatestVersion(customerId) + 1` is a valid first version — with `MAX` alone the
empty case would be SQL `NULL`, `rs.getInt` would still return `0`, and the intent would be invisible.
Write the `COALESCE` with a comment so nobody "fixes" it to `-1` and then has to fix the service's
arithmetic too. `exercises` is a single `String` because that is the field type in §1: whatever format
the program text uses is the service's business, and the DAO stores it unchanged.

**How to write it.**

1. Interface, seven signatures, `findLatestVersion` last (it reads as the extra one).
2. Impl with the `DbConnection` field; `mapRow` maps `customerId` and `creatorTrainerId` (plain `int`s,
   `NOT NULL`), `name`, `exercises`, `version` (`getInt`) and `createdDate`.
3. `save` with `RETURN_GENERATED_KEYS`, binding `version` as given — the DAO does not compute it.
4. `findByCustomer` ordered `version DESC` so the newest program is first.

   ```java
   // Sketch — signature is authoritative in docs/UML.md §3.
   @Override
   public int findLatestVersion(int customerId) {
       String sql = "SELECT COALESCE(MAX(version), 0) FROM workout_programs WHERE customerId = ?";
       try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
           ps.setInt(1, customerId);
           try (ResultSet rs = ps.executeQuery()) {
               return rs.next() ? rs.getInt(1) : 0;   // no rows -> 0 -> the service's +1 makes v1
           }
       } catch (SQLException e) {
           throw new DaoException("Could not read the latest program version", e);
       }
   }
   ```

5. Make `(customerId, version)` a **unique** index in `schema.sql`, since §1b states version is unique
   per customer — that is what turns the sentence into something the database enforces.
6. `ORDER BY` on the list methods; wrap `SQLException` everywhere.

**How to know it is right.**

1. `findLatestVersion(7)` on a customer with no programs returns `0`; after one program, `1`; after a
   second, `2`.
2. Saving the same `(customerId, version)` twice fails on the unique index.
3. `findByCustomer(7)` returns newest-first, and an empty list for a customer with none.
4. Updating a program's `name` changes only that row and leaves `version` and `createdDate` intact.

**Common mistakes.**

- **Computing the version in the DAO** (`MAX(version) + 1` inside the insert). The version is a service
  decision; storing what it is given is what keeps the two from disagreeing.
- **`MAX(version)` without `COALESCE`.** It returns SQL `NULL` for an empty set; the next reader cannot
  tell whether the `0` that arrives was intended.
- **Forgetting the unique index.** Two `v2` rows for one customer make "latest" ambiguous, and which
  one `ORDER BY version DESC` returns depends on insert order.
- **Letting `update` change `version`.** A version number that moves makes every later "latest" query
  wrong; the correct choice is the service's.
- **Loading `exercises` in the list query** when the screen shows three columns. It is the longest text
  in the table, and `SELECT *` fetches it for nothing.

### `SessionDao.java` + `SessionDaoImpl.java` — the busiest pair (owner: Yousef)

**Status:** planned. **Canonical:** `docs/UML.md` §3 (11 methods), §1 (`Session`), rules 1, 2, 11, 13,
17 and 18.

**What it is.** The contract for the `sessions` table: which member trains with which coach on which day
and hour, whether the session is requested, confirmed, completed, a no-show or cancelled, which
published slot it fits into, and which subscription pays for it. It is the largest interface in the
folder because the agenda, the booking rules and the occupancy count all read this one table.

**Methods.**

| Method | Returns | Why it exists / rule it serves |
|---|---|---|
| `save(Session)` | `int` (new id) | Book or request a session (rule 1) |
| `findByDay(String)` | `List<Session>` | The day's agenda for the front desk |
| `findById(int)` | `Session` | One session, before a transition |
| `findAll()` | `List<Session>` | The full list; also the trainer's statistics source (rule 2 counts `NO_SHOW`) |
| `update(Session)` | `void` | Every status change, price fix and cancel reason (rule 18 decides, this writes) |
| `delete(int)` | `void` | Admin-only; a cancelled session is normally kept for history |
| `findByStatus(SessionStatus)` | `List<Session>` | Requested vs confirmed lists |
| `findByCustomer(int)` | `List<Session>` | One member's history |
| `findByCustomerAndDay(int, String)` | `List<Session>` | Customer-side double-booking detection |
| `findByTrainerAndDay(int, String)` | `List<Session>` | The coach's agenda and trainer-side conflict detection |
| `countByAvailability(int, SessionStatus)` | `int` | Slot occupancy; rule 17 passes `CONFIRMED` |

**Why it is designed this way.** The two `findBy…AndDay` methods take the date as a `String`, not a
`Date`, so the comparison stays a text comparison on `yyyy-MM-dd` (rule 9) — and that is also why
`ORDER BY date, startTime` is a correct agenda order with no parsing. `countByAvailability` takes the
status as a *parameter* instead of hard-coding `CONFIRMED`, which is what lets rule 17 live in the
service while the SQL stays a general count. `availabilityId` and `subscriptionId` are boxed `Integer`
in the model because both are genuinely optional: a session requested outside published slots has no
slot (rule 1), and a session not covered by a subscription has no subscription (rule 11). This is the
pair with the most nullable columns, so re-read rule 13 before typing.

**How to write it.**

1. Interface, eleven signatures as §3 prints them. Resist "while I am here" additions such as
   `findByTrainerAndRange` — not in §3.
2. Impl with the `DbConnection` field; `mapRow` maps `customerId`, `trainerId`, `date`, `startTime`,
   `durationMin`, `SessionStatus.valueOf(...)`, both nullable ids through `rs.getObject(...)`, `price`
   (money), `cancelReason` and `sessionNotes` (plain `getString` — a `String` getter already returns
   `null` for SQL `NULL`).
3. `save` with `RETURN_GENERATED_KEYS`; a `null` reason or note is written with `setString` (which
   stores SQL `NULL`) or `setNull(index, Types.VARCHAR)`.
4. `findByDay`, `findByCustomerAndDay` and `findByTrainerAndDay` order by `startTime`; the two
   `…AndDay` methods are single-date equality queries and use the `(customerId, date)` /
   `(trainerId, date)` indexes.
5. `countByAvailability`:

   ```java
   // Sketch — signature is authoritative in docs/UML.md §3.
   @Override
   public int countByAvailability(int availabilityId, SessionStatus status) {
       String sql = "SELECT COUNT(*) FROM sessions WHERE availabilityId = ? AND status = ?";
       try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
           ps.setInt(1, availabilityId);
           ps.setString(2, status.name());     // the service passes CONFIRMED in rule 17
           try (ResultSet rs = ps.executeQuery()) {
               return rs.next() ? rs.getInt(1) : 0;
           }
       } catch (SQLException e) {
           throw new DaoException("Could not count booked sessions for the slot", e);
       }
   }
   ```

6. Translate every `SQLException`; order every list; return empty lists.

**How to know it is right.**

1. Book a session with `availabilityId == null` and `subscriptionId == null`; both read back `null`,
   and `getAvailabilityId()` does not return `0`.
2. Book two `CONFIRMED` sessions into one slot with `maxCustomers = 2`, then cancel one:
   `countByAvailability(slot, SessionStatus.CONFIRMED)` drops from 2 to 1 (rule 17).
3. `findByCustomerAndDay(12, "2025-05-04")` finds a session booked with a *different* trainer — the
   double-booking check must not be scoped to the trainer.
4. `findByDay("2025-05-04")` returns that day's sessions in `startTime` order.
5. A session whose `price` is `0` reads back as `0`, and `compareTo` (not `equals`) confirms it — as
   `TEXT`, `"0.00"` and `"0"` are different `BigDecimal` scales.

**Common mistakes.**

- **`rs.getInt` on `availabilityId` or `subscriptionId`.** SQL `NULL` becomes `0`, which looks like a
  link to slot 0 — a row that does not exist — and rule 11's pricing check reads the wrong
  subscription.
- **Enforcing the state machine here** ("only `CONFIRMED` may be cancelled: add `WHERE status =
  'CONFIRMED'` to the update"). `SchedulingService.canTransition` is the single authority (rule 18),
  and because `update` is `void` (rule 10) a refused transition would look like a successful one.
- **Hard-coding `CONFIRMED` inside `countByAvailability`.** The interface promises a count for the
  status you pass; a fixed value makes the parameter a lie.
- **Comparing dates as `Date` objects.** Converting to `java.util.Date` reintroduces the time zones and
  locale formatting rule 9 removed; compare the strings.
- **Deleting a session row to "cancel" it.** Cancel is a status plus a reason (rule 10); the delete is
  an admin path that erases why the slot was freed.

### `EquipmentDao.java` + `EquipmentDaoImpl.java` — the standalone table (owner: Yousef)

**Status:** planned. **Canonical:** `docs/UML.md` §3 (7 methods), §1 (`Equipment`), `DECISIONS.md` 30 and
`docs/UML.md` "Notes on scope".

**What it is.** The contract for the `equipment` table: inventory items with a condition, a status and a
last-maintenance date. It is the only entity with **no relationship at all** — no foreign key in either
direction — so this is the one pair whose SQL joins nothing and cannot.

**Methods.**

| Method | Returns | Why it exists / rule it serves |
|---|---|---|
| `save(Equipment)` | `int` (new id) | Add an item to the inventory |
| `findById(int)` | `Equipment` | One item |
| `findAll()` | `List<Equipment>` | The inventory screen |
| `update(Equipment)` | `void` | Edit an item, change condition or status, record maintenance |
| `delete(int)` | `void` | Remove an item — the one place a real row delete is the normal operation |
| `findByStatus(EquipmentStatus)` | `List<Equipment>` | Available vs in-maintenance vs retired |
| `findByCondition(EquipmentCondition)` | `List<Equipment>` | The repair list: `POOR` and `FAIR` items |

**Why it is designed this way.** `DECISIONS.md` 30 keeps `Equipment` a root aggregate instead of
inventing a relationship to make the diagram look connected: a treadmill belongs to no member, trainer
or session. The consequence is pleasant — no nullable foreign key, no join, no ordering dependency on
another table, and `delete` can mean `DELETE FROM equipment WHERE id = ?` because nothing references
the row. A per-item maintenance *history* would need a new entity and table (`MaintenanceRecord`),
explicitly out of scope, so `lastMaintenance` stays one date on the item. The two filters are separate
methods because §3 declares them separately; there is no combined status-and-condition query.

**How to write it.**

1. Interface, seven signatures; `mapRow` maps `name`, `category`, `purchaseDate`,
   `EquipmentCondition.valueOf(...)`, `EquipmentStatus.valueOf(...)` and `lastMaintenance`.
2. `save` with `RETURN_GENERATED_KEYS`; both enum columns bound with `.name()`.
3. `findAll` ordered by `name`, and the two filters ordered the same way, so switching a filter does not
   reshuffle the list.
4. `delete` is a real `DELETE`, still wrapped — a failed delete is a `DaoException`, not a silent no-op:

   ```java
   // Sketch — signature is authoritative in docs/UML.md §3.
   @Override
   public void delete(int id) {
       String sql = "DELETE FROM equipment WHERE id = ?";   // nothing references equipment (DECISIONS.md 30)
       try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
           ps.setInt(1, id);
           ps.executeUpdate();
       } catch (SQLException e) {
           throw new DaoException("Could not delete equipment " + id, e);
       }
   }
   ```

5. Leave the choice between deleting and setting `RETIRED` to `EquipmentService`; the DAO offers both
   because §3 declares both.

**How to know it is right.**

1. Save an item with `status = RETIRED`; the enum word round-trips.
2. `findByStatus(EquipmentStatus.IN_MAINTENANCE)` and `findByCondition(EquipmentCondition.POOR)`
   return exactly those rows.
3. `delete(id)` on an unreferenced item removes exactly one row and leaves the rest untouched.
4. `lastMaintenance` accepts a new date, and no code invents "today" when it is empty.

**Common mistakes.**

- **Adding a `customerId` or `trainerId` column** so the table "fits in". `DECISIONS.md` 30 rejected it.
- **Storing the enums as `ordinal()`.** `EquipmentCondition.NEW` is `0`; adding a literal at the front
  renumbers every stored row.
- **Returning `null` from the two filters** when nothing matches. An empty list is normal.
- **Re-implementing "delete means deactivate" here.** Rule 4 is about `Customer` and `Trainer`;
  equipment has no references, so a real delete is the documented path.
- **Forgetting `WHERE id = ?` in the update.** It is one clause away from rewriting every row's status,
  and because the method is `void` nobody gets an error (rule 10).

### `AttendanceDao.java` + `AttendanceDaoImpl.java` — one row per member per day (owner: Abdel Raouf)

**Status:** planned. **Canonical:** `docs/UML.md` §3 (8 methods), §1 (`Attendance`), rules 5 and 19.

**What it is.** The contract for the `attendance` table: check-in and check-out times per member per
day. Rule 5 makes it the table where a duplicate is a *bug to be detected*, not a row to be
overwritten, and rule 19 makes it a table whose rows are never deleted.

**Methods.**

| Method | Returns | Why it exists / rule it serves |
|---|---|---|
| `save(Attendance)` | `int` (new id) | Record a check-in |
| `findByDay(String)` | `List<Attendance>` | The daily report and the "who is in the gym" view |
| `findById(int)` | `Attendance` | One record, before a correction |
| `findAll()` | `List<Attendance>` | The full list; the weekly export |
| `update(Attendance)` | `void` | Setting `checkOut`, and `correctRecord`'s fix of a mis-keyed time |
| `delete(int)` | `void` | Declared by §3 but **not called by any service** — rule 19 |
| `findByCustomer(int)` | `List<Attendance>` | One member's attendance history |
| `findByCustomerAndDay(int, String)` | `List<Attendance>` | Rule 5's "already checked in today?" lookup |

**Why it is designed this way.** `findByCustomerAndDay` returns a **list**, not a single row, and that is
a deliberate design statement: with `LIMIT 1` a stray duplicate would be invisible, and the check-in
flow would happily add a third row. Returning the list means the service can see that the data is
already wrong and say so. `checkOut` is nullable because a member who is still inside has no check-out
time yet; for a `String` column `getString` already returns `null`, so no `wasNull()` is needed.
`delete` is implemented because §3 declares it, and left uncalled because rule 19 says an error is
corrected with `AttendanceService.correctRecord`.

**How to write it.**

1. Interface, eight signatures; `findByDay(String)` and `findByCustomerAndDay(int, String)` both take
   the ISO date as text (rule 9).
2. Impl with the `DbConnection` field; `mapRow` maps `customerId`, `date`, `checkIn` and `checkOut`
   (`getString` — `null` while the member is still inside).
3. `save` with `RETURN_GENERATED_KEYS`; a `null` `checkOut` is stored with `setString` (a `null`
   argument stores SQL `NULL`).
4. `findByCustomerAndDay` **without** `LIMIT`:

   ```java
   // Sketch — signature is authoritative in docs/UML.md §3.
   @Override
   public List<Attendance> findByCustomerAndDay(int customerId, String date) {
       String sql = "SELECT id, customerId, date, checkIn, checkOut FROM attendance "
                  + "WHERE customerId = ? AND date = ? ORDER BY checkIn";
       List<Attendance> rows = new ArrayList<>();     // a LIST: a duplicate must be visible (rule 5)
       try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
           ps.setInt(1, customerId);
           ps.setString(2, date);
           try (ResultSet rs = ps.executeQuery()) {
               while (rs.next()) rows.add(mapRow(rs));
           }
       } catch (SQLException e) {
           throw new DaoException("Could not load today's attendance for this customer", e);
       }
       return rows;   // service decides: 0 -> insert, 1 -> return it, 2+ -> report the bad data
   }
   ```

5. `findByDay` ordered by `checkIn`; `(customerId, date)` and `(date)` indexes in `schema.sql`.
6. Wrap `SQLException` in every method; never add a `deleted` column or a delete caller.

**How to know it is right.**

1. `findByCustomerAndDay(12, today)` returns an empty list for a fresh member, one row after a
   check-in, and **two** after a hand-inserted duplicate — the flow must be able to see that.
2. A check-in with `checkOut == null` reads back `null`, not `""` and not `"00:00"`.
3. `findByDay(today)` returns everyone in check-in order.
4. `grep -rn "AttendanceDao" src/main/java` shows the service and the wiring, and no caller of
   `delete` (rule 19).

**Common mistakes.**

- **Adding `LIMIT 1` to `findByCustomerAndDay`** to make the result easier to use. That is exactly the
  bug rule 5's list return prevents.
- **Storing `checkOut` as an empty string** instead of `NULL`; every later comparison then has to
  special-case `''`.
- **Calling `delete` from the check-in flow to "reset" a day.** Rule 19 forbids it: a wrong time is
  corrected, so the day still shows what happened.
- **Deriving attendance from sessions.** They are different tables on purpose: a member can walk in
  without a booked session, and a session can be a `NO_SHOW` with no attendance row (rule 2).
- **Using a `DATE`/timestamp column type.** SQLite would accept it, and rule 9's plain string
  comparisons would stop working in the one place the daily report lives.

### `PaymentDao.java` + `PaymentDaoImpl.java` — money that must reconcile (owner: Abdel Raouf)

**Status:** planned. **Canonical:** `docs/UML.md` §3 (9 methods), §1 (`Payment`), rules 6, 12 and 19;
`DECISIONS.md` 31.

**What it is.** The contract for the `payments` table. A payment records an *act of paying*: an amount,
a method, a date, a receipt number, and exactly one link to what was paid for — a membership, a trainer
subscription or a single session (rule 12). It is also the table the dashboard's revenue figure comes
from, which makes it the pair where the money storage decision (rule 11, open detail A) has a
consequence you can see on screen.

**Methods.**

| Method | Returns | Why it exists / rule it serves |
|---|---|---|
| `save(Payment)` | `int` (new id) | Record a payment |
| `findById(int)` | `Payment` | One payment, before a void or an adjustment |
| `findAll()` | `List<Payment>` | The payments screen |
| `update(Payment)` | `void` | Voiding (`voided`, `voidReason`, `voidedDate`) and `adjustPayment` (`previousAmount`) |
| `delete(int)` | `void` | Declared by §3 but **not called by any service** — rule 19 |
| `findByCustomer(int)` | `List<Payment>` | A member's payment history and their balance working |
| `findByReceipt(String)` | `Payment` | Looking a receipt up again, months later (rule 19) |
| `findByMethod(PaymentMethod)` | `List<Payment>` | The cash-up at the end of the day |
| `revenueBetween(String, String)` | `BigDecimal` | The dashboard's revenue figure, voided excluded (rule 6) |

**Why it is designed this way.** The three link columns are read, never guessed: the DAO stores the
combination the service validated and must read SQL `NULL` back as `null` (rule 13), or a membership
payment starts looking like a session payment. `findByReceipt` returns a single payment, which only
works if `receiptNo` is unique — hence the unique index in `schema.sql`; without it the query silently
returns whichever duplicate SQLite reaches first, and a receipt lookup becomes a lottery.
`revenueBetween` sums **in SQL** because §3 has no "all payments in a range" method, so this is the one
method where the storage form of money decides whether the answer is exact. `delete` exists on the
interface but rule 19 keeps it uncalled: wrong money is corrected by voiding and recording a new
payment, or by `AdminService.adjustPayment` storing `previousAmount`, so the receipt still reconciles.

**How to write it.**

1. Interface, nine signatures. `revenueBetween` takes two `String` dates, not a date type (rule 9), and
   `from`/`to` are inclusive.
2. Impl with the `DbConnection` field; `mapRow` maps `customerId`, the three link ids through
   `rs.getObject(...)`, `amount` (money, open detail A), `PaymentMethod.valueOf(...)`, `date`,
   `receiptNo`, `voided` as `rs.getInt("voided") != 0`, plus `voidReason`, `voidedDate` and
   `previousAmount`.
3. `save` with `RETURN_GENERATED_KEYS`; each link column written with `setInt(...)` or
   `setNull(index, Types.INTEGER)` depending on the field.
4. `findByCustomer` and `findByMethod` ordered `date DESC`; `findAll` ordered `date DESC, id DESC` so
   same-day rows have a stable order.
5. `revenueBetween`, with the `voided = 0` filter that makes the number mean revenue (rule 6):

   ```java
   // Sketch — signature is authoritative in docs/UML.md §3. Open detail A decides the storage form;
   // this shows the INTEGER-cents variant, the only one whose SUM stays exact.
   @Override
   public BigDecimal revenueBetween(String from, String to) {
       String sql = "SELECT COALESCE(SUM(amountCents), 0) FROM payments "
                  + "WHERE voided = 0 AND date >= ? AND date <= ?";   // voided money is not revenue
       try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
           ps.setString(1, from);              // ISO strings compare like dates (rule 9)
           ps.setString(2, to);
           try (ResultSet rs = ps.executeQuery()) {
               long cents = rs.next() ? rs.getLong(1) : 0L;
               return BigDecimal.valueOf(cents, 2);   // 12345 -> 123.45, exactly
           }
       } catch (SQLException e) {
           throw new DaoException("Could not total revenue for the period", e);
       }
   }
   ```

   `COALESCE(SUM(...), 0)` is deliberate: `SUM` over an empty range is SQL `NULL`, and returning `null`
   here would become a `NullPointerException` in the dashboard's label.

6. `findByReceipt` with `WHERE receiptNo = ?`, returning `null` for an unknown receipt, plus the unique
   index on `receiptNo`.
7. Wrap every `SQLException`; never add a delete call anywhere above this layer.

**How to know it is right.**

1. Record a membership payment with `subscriptionId` and `sessionId` `null`; exactly one link reads
   back set, and it is the right one.
2. `revenueBetween("2025-05-01", "2025-05-31")` over payments of `45.00` and `19.99` returns `64.99` —
   to the cent, compared with `compareTo`.
3. Void one of them (`voided = 1` through `update`) and re-run the query: it drops out, while
   `findByCustomer` still returns the voided row with its `voidReason` and `voidedDate`.
4. A month with no payments returns `0.00` and the dashboard shows `0.00` rather than crashing.
5. `findByReceipt("R-2025-0007")` finds the payment; an unknown receipt returns `null`, no exception.
6. `grep -rn "\.delete(" src/main/java` shows no caller on this DAO (rule 19).

**Common mistakes.**

- **`rs.getInt("subscriptionId")`.** A membership payment then looks like it also has
  `subscriptionId = 0`, and rule 12's XOR appears broken when rows are read back.
- **Summing money with `double` in Java** (`rs.getDouble("amount")`). That discards the exactness the
  `BigDecimal` field exists for, in the one method whose output is a currency amount
  (`DECISIONS.md` 31).
- **Forgetting `voided = 0`.** The revenue figure then includes money that was refunded or corrected,
  and the receipts no longer add up to the dashboard (rule 6).
- **Adding `LIMIT 1` to `findByReceipt`** instead of a unique index. The lookup still returns
  something, so the duplicate goes unnoticed until two members argue about one receipt.
- **Voiding by deleting the row.** Rule 19: a void is a state plus a reason, and `adjustPayment` keeps
  `previousAmount` so the audit trail survives.
- **`WHERE date > ? AND date < ?`** when the caller means an inclusive month. The off-by-one-day report
  is invisible except at month boundaries.

### `ProgressDao.java` + `ProgressDaoImpl.java` — body metrics over time (owner: Abdel Raouf)

**Status:** planned. **Canonical:** `docs/UML.md` §3 (6 methods), §1 (`ProgressRecord`), and the naming
rule in §4 (the DAO is `ProgressDao`, the entity is `ProgressRecord`).

**What it is.** The contract for the `progress_records` table: dated body measurements — weight, body
fat, free-text measurements, an optional target weight and notes. It is the data source behind the
progress chart, but it does no chart work: `ChartUtils` formats values a service has already fetched
([`../util/README.md`](../util/README.md)).

**Methods.**

| Method | Returns | Why it exists / rule it serves |
|---|---|---|
| `save(ProgressRecord)` | `int` (new id) | Record a measurement |
| `findById(int)` | `ProgressRecord` | One record, before an edit |
| `findAll()` | `List<ProgressRecord>` | The full list |
| `update(ProgressRecord)` | `void` | Correcting a measurement |
| `delete(int)` | `void` | Removing a mistaken record — allowed here; rule 19 covers attendance and payments, not progress |
| `findByCustomer(int)` | `List<ProgressRecord>` | A member's series, in date order, for the chart |

**Why it is designed this way.** The name mismatch is the one place where the class name and the DAO
name differ, and it is deliberate: the DAO is named for the **concept** (progress), not for the row
class. This pair is also the clearest example of the model's nullable-column convention: `weightKg` and
`bodyFatPct` are primitive `double`s, so those columns are `NOT NULL` and `getDouble` is correct;
`targetWeightKg` is a boxed `Double`, so that column is nullable and must be read with `getObject`
(rule 13). One entity, both cases — a good pair to read before writing any other. `delete` is a genuine
delete here: a mis-keyed weight is not a financial or attendance record.

**How to write it.**

1. Interface, six signatures; the class name is `ProgressDao`, not `ProgressRecordDao`.
2. Impl with the `DbConnection` field; `mapRow` maps `customerId`, `date`, `weightKg` (`getDouble`),
   `bodyFatPct` (`getDouble`), `measurements`, `targetWeightKg`
   (`(Double) rs.getObject("targetWeightKg")`) and `notes`.
3. `save` with `RETURN_GENERATED_KEYS`; a `null` target is written with `setNull(index, Types.REAL)`.
4. `findByCustomer` ordered `date` ascending — the chart reads left to right, and ascending is the
   natural order for a "progress" question; a newest-first table view is the screen's job, not the
   query's.
5. Wrap every `SQLException`; return an empty list for a member with no records.

   ```java
   private ProgressRecord mapRow(ResultSet rs) throws SQLException {   // sketch — §3 owns the signatures
       ProgressRecord p = new ProgressRecord();
       p.setId(rs.getInt("id"));
       p.setCustomerId(rs.getInt("customerId"));
       p.setDate(rs.getString("date"));                     // 'yyyy-MM-dd'
       p.setWeightKg(rs.getDouble("weightKg"));             // primitive double -> NOT NULL column
       p.setBodyFatPct(rs.getDouble("bodyFatPct"));         // primitive double -> NOT NULL column
       p.setMeasurements(rs.getString("measurements"));
       p.setTargetWeightKg((Double) rs.getObject("targetWeightKg"));   // boxed Double -> nullable
       p.setNotes(rs.getString("notes"));
       return p;
   }
   ```

**How to know it is right.**

1. Save a record with `targetWeightKg == null`; `getTargetWeightKg()` reads back `null`, not `0.0`.
2. Save one with `targetWeightKg == 70.5`; it reads back `70.5`.
3. `findByCustomer(12)` returns records oldest-first — the order the chart plots.
4. A zero weight or zero body-fat percentage reads back `0.0` and stays distinguishable from the `null`
   target: the whole point of the boxed/primitive split.
5. `delete(id)` removes one row and leaves the series otherwise unchanged.

**Common mistakes.**

- **`rs.getDouble("targetWeightKg")`.** A `null` target becomes `0.0`, which the chart then plots as the
  member's goal being zero kilos.
- **Naming the class `ProgressRecordDao`.** It breaks the naming rule and the §3 file list, so the next
  person creates a second copy of the pair.
- **Filtering the metric here** (`WHERE weightKg IS NOT NULL`). Which measurement a series shows is
  `ProgressService.series(..., ProgressMetric)` — the DAO returns the whole record (`docs/UML.md` §6).
- **Formatting values in the DAO.** `ChartUtils` formats; the DAO returns numbers, and a
  locale-formatted string in the model would break every comparison.
- **Sorting by `id` instead of `date`.** Records are entered in any order, so a back-dated measurement
  would land at the end of the chart line.

### `MedicalProfileDao.java` + `MedicalProfileDaoImpl.java` — the sensitive one (owner: Abdelrhman)

**Status:** planned. **Canonical:** `docs/UML.md` §3 (6 methods), §1 (`MedicalProfile`), rules 3 and 15.

**What it is.** The contract for the `medical_profiles` table: blood type, conditions, allergies,
medications, injuries, the doctor's name and phone, notes, and the date the profile was last updated. It
holds the most sensitive data in the application, and rule 3 makes it optional — a member may have
**zero or one** profile, created on demand rather than at registration.

**Methods.**

| Method | Returns | Why it exists / rule it serves |
|---|---|---|
| `save(MedicalProfile)` | `int` (new id) | Create the profile the first time it is needed (rule 3) |
| `findByCustomer(int)` | `MedicalProfile` | The lookup every medical view and every trainer check starts from |
| `findById(int)` | `MedicalProfile` | Load one profile by its own id |
| `findAll()` | `List<MedicalProfile>` | The full list; used by reporting, not by a trainer's screen |
| `update(MedicalProfile)` | `void` | Edit the medical fields and `updatedDate` |
| `delete(int)` | `void` | Delete a profile that was created by mistake |

**Why it is designed this way.** The interface is deliberately tiny and contains **no access control**,
because access control is a business rule: `MedicalProfileService.getForTrainer` is the only sanctioned
read path for a trainer, and `Customer.trainerId` is what makes a trainer "assigned" (rule 15). A DAO
cannot enforce that — it does not know who is asking — so the rule that matters is about *callers*: no
controller, and no other service, may hold a `MedicalProfileDao` reference. A `UNIQUE(customerId)` index
in `schema.sql` turns rule 3's "at most one" into something the database enforces, and it is also what
makes `findByCustomer`'s single-object return honest. The DAO must not write `updatedDate` itself
(`DATE('now')` inside the `UPDATE`): a hidden write makes the date the caller saw differ from the date
stored, and "when was this last changed?" is the service's question to answer.

**How to write it.**

1. Interface, six signatures. Keep §3's order even where it looks odd (`findByCustomer` before
   `findById`) — the file is a contract, and matching the diagram line for line is how a reviewer
   checks it.
2. Impl with the `DbConnection` field; `mapRow` maps `customerId`, `bloodType`, `conditions`,
   `allergies`, `medications`, `injuries`, `doctorName`, `doctorPhone`, `notes` and `updatedDate` — all
   plain `getString`, all nullable in practice.
3. `save` with `RETURN_GENERATED_KEYS`. The service calls it only when `findByCustomer` returned `null`;
   the unique index is the backstop if two paths race.
4. `findByCustomer` returns the profile or `null` — do not throw a "not found" exception and do not
   return an empty profile object, because an all-blank profile is a real state (created, nothing filled
   in) and must stay distinguishable from "no profile".
5. Wrap every `SQLException` with a message that does **not** name the medical fields or their values:

   ```java
   // Sketch — signature is authoritative in docs/UML.md §3.
   @Override
   public MedicalProfile findByCustomer(int customerId) {
       String sql = "SELECT id, customerId, bloodType, conditions, allergies, medications, injuries, "
                  + "doctorName, doctorPhone, notes, updatedDate "
                  + "FROM medical_profiles WHERE customerId = ?";   // UNIQUE: at most one (rule 3)
       try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
           ps.setInt(1, customerId);
           try (ResultSet rs = ps.executeQuery()) {
               return rs.next() ? mapRow(rs) : null;   // null = no profile created yet
           }
       } catch (SQLException e) {
           throw new DaoException("Could not load the medical profile", e);   // no field values here
       }
   }
   ```

**How to know it is right.**

1. `findByCustomer(12)` returns `null` for a member with no profile, and an object with all-`null`
   fields for one created but left blank — the two are different.
2. Saving a second profile for the same customer fails on the unique index.
3. `update` changes `updatedDate` only when the service passes a new value; the DAO never rewrites it.
4. `grep -rn "MedicalProfileDao" src/main/java` shows `MedicalProfileService` and `MainApp`'s wiring,
   and no controller.

**Common mistakes.**

- **Calling this DAO from `TrainerService` or a trainer screen.** Rule 15's access check lives in
  `MedicalProfileService.getForTrainer`; bypassing it turns a design rule into a code comment.
- **Returning an empty `MedicalProfile` instead of `null`.** The UI then cannot tell "no profile yet"
  from "profile with nothing filled in", and the create-on-demand path (rule 3) breaks.
- **Adding a `findByTrainer(int)`** so a trainer view can list patients. Assignment is
  `Customer.trainerId`, so the patient list is `CustomerDao.findByTrainer`; a second path to the same
  data is how medical access grows a hole.
- **Putting `DATE('now')` in the `UPDATE`.** The service owns `updatedDate`; a database-generated value
  is invisible to the caller and untestable.
- **Echoing medical values into the exception message.** `DaoException`'s message is displayed
  (`FxUtils.error(e.getMessage())`, `docs/UML.md` §2); conditions and medications must never reach a
  dialog or a log line anyone can read over a shoulder.

### `AdminDao.java` + `AdminDaoImpl.java` — login identity and roles (owner: Abdelrhman)

**Status:** planned. **Canonical:** `docs/UML.md` §3 (7 methods), §1 (`Administrator`), rule 16.

**What it is.** The contract for the `administrators` table: staff accounts with a username, a password
**hash**, a role (`StaffRole.ADMIN`/`MANAGER`/`STAFF`), contact fields, an `active` flag and a creation
date. `AdminService.authenticate` is built on `findByUsername`, and
`SessionContext.requireRole(StaffRole.ADMIN)` gates the screens (rule 16).

**Methods.**

| Method | Returns | Why it exists / rule it serves |
|---|---|---|
| `save(Administrator)` | `int` (new id) | Create a staff account |
| `findByUsername(String)` | `Administrator` | The login lookup; `null` for an unknown username (rule 16) |
| `findById(int)` | `Administrator` | One account |
| `findAll()` | `List<Administrator>` | The staff screen |
| `update(Administrator)` | `void` | Password reset, role change, contact details, deactivation |
| `delete(int)` | `void` | Admin-only; deactivating with `active = 0` is normally the right answer |
| `findByRole(StaffRole)` | `List<Administrator>` | The staff screen filtered by role |

**Why it is designed this way.** The DAO stores a hash and never sees a password: `PasswordUtils.hash`
and `verify` are called by `AdminService`, so plaintext exists only inside that one method
([`../util/README.md`](../util/README.md), root `README.md` §11). `active` is a primitive `boolean` in
the model, so the column is `NOT NULL` and holds `0`/`1`, and reading it with
`rs.getInt("active") != 0` avoids depending on how a driver maps integers to booleans.
`findByUsername` returning `null` is what makes rule 16's "`authenticate` returns null for an unknown
username, a wrong password, or an inactive account" possible without an exception for a normal case.
The DAO decides nothing about roles: it returns the row, and `SessionContext`/`AdminService` apply the
rule.

**How to write it.**

1. Interface, seven signatures, `findByUsername` second as §3 prints it.
2. Impl with the `DbConnection` field; `mapRow` maps `fullName`, `username`, `passwordHash`,
   `StaffRole.valueOf(...)`, `phone`, `email`, `active` (`rs.getInt("active") != 0`) and `createdDate`.
3. `save` with `RETURN_GENERATED_KEYS`; bind the **hash** the service put in the object, never anything
   derived from a password in this layer.
4. `findByUsername` with `WHERE username = ?` and a unique index on `username` — two accounts with one
   username make `authenticate` depend on row order.
5. `findByRole` binds `role.name()`; both list methods ordered by `fullName`.
6. Wrap `SQLException` with a message that never contains the hash.

   ```java
   // Sketch — signature is authoritative in docs/UML.md §3.
   @Override
   public Administrator findByUsername(String username) {
       String sql = "SELECT id, fullName, username, passwordHash, role, phone, email, active, "
                  + "createdDate FROM administrators WHERE username = ?";
       try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
           ps.setString(1, username);
           try (ResultSet rs = ps.executeQuery()) {
               return rs.next() ? mapRow(rs) : null;   // null -> authenticate() returns null (rule 16)
           }
       } catch (SQLException e) {
           throw new DaoException("Could not check the sign-in details", e);   // never the hash
       }
   }
   ```

**How to know it is right.**

1. `findByUsername("nobody")` returns `null` and no exception.
2. `active` round-trips: `0` reads back `false`, `1` reads back `true`.
3. `findByRole(StaffRole.ADMIN)` returns exactly the admin accounts.
4. Two rows with the same username cannot be inserted (unique index).
5. `grep -rn "passwordHash" src/main/java` shows this DAO and the service — and the hash never appears
   in a `DaoException` message or a debug print.

**Common mistakes.**

- **Hashing in the DAO.** Then "who hashes?" has two answers and one path stores plaintext.
- **`rs.getBoolean("active")` without checking the driver.** `!= 0` on an `int` is unambiguous, and the
  `0`/`1` convention is what `schema.sql` documents.
- **Passing a whole row to a debug log.** The row contains the hash, and debug output gets committed
  more often than anyone expects.
- **Throwing when a username is unknown.** Unknown usernames are normal input; the `null` return is the
  interface's answer and rule 16 builds on it.
- **Enforcing roles here** ("only an ADMIN may be saved"). A role check needs to know *who is asking*,
  which is `SessionContext.requireRole` (rule 16) — a DAO would have to read global state to guess.

### `TrainerSubscriptionDao.java` + `TrainerSubscriptionDaoImpl.java` — paid engagements with a coach (owner: Yousef)

**Status:** planned. **Canonical:** `docs/UML.md` §3 (8 methods), §1 (`TrainerSubscription`), rules 11,
13 and 15.

**What it is.** The contract for the `trainer_subscriptions` table: a member pays one coach a flat
`ratePerPeriod` for a period, and sessions inside that period are covered (so their `price` is `0`,
rule 11). It is the table that makes "is this member already subscribed to this trainer?" a query rather
than a loop.

**Methods.**

| Method | Returns | Why it exists / rule it serves |
|---|---|---|
| `save(TrainerSubscription)` | `int` (new id) | Start an engagement |
| `findActive(int, int, String)` | `TrainerSubscription` | "Already subscribed to this trainer as of this date?" |
| `findById(int)` | `TrainerSubscription` | One engagement |
| `findAll()` | `List<TrainerSubscription>` | The full list |
| `update(TrainerSubscription)` | `void` | Renew (new `endDate`), cancel (`CANCELLED`) |
| `delete(int)` | `void` | Admin-only; a cancelled engagement is normally kept |
| `findByCustomer(int)` | `List<TrainerSubscription>` | A member's engagement history |
| `findByTrainer(int)` | `List<TrainerSubscription>` | A coach's subscribers |

**Why it is designed this way.** `findActive` takes **two ids and a date**, which is the whole point:
`status` alone cannot answer the question, because `SubscriptionStatus.EXPIRED` is derived from `endDate`
and never set by hand (rule 13) — a row can still say `ACTIVE` while its period has ended. So the query
asks for the stored `ACTIVE` word **and** the date window around the as-of date, and the service derives
`EXPIRED` for display. The DAO does not check rule 15's "the subscription's trainer must be the
customer's assigned trainer": that compares two rows, it needs the customer, and it belongs where the
decision is made (`SchedulingService`, with `CustomerDao.findById` in hand). `ratePerPeriod` is the
fourth money column and follows open detail A.

**How to write it.**

1. Interface, eight signatures; document `findActive(int, int, String)` as
   `(customerId, trainerId, asOfDate)` in a one-line Javadoc so nobody guesses the parameter order.
2. Impl with the `DbConnection` field; `mapRow` maps `customerId`, `trainerId`, `startDate`, `endDate`,
   `ratePerPeriod` (money) and `SubscriptionStatus.valueOf(...)`.
3. `save` with `RETURN_GENERATED_KEYS`; `findByCustomer` and `findByTrainer` ordered `startDate DESC`.
4. `findActive`:

   ```java
   // Sketch — signature is authoritative in docs/UML.md §3.
   @Override
   public TrainerSubscription findActive(int customerId, int trainerId, String date) {
       String sql = "SELECT id, customerId, trainerId, startDate, endDate, ratePerPeriod, status "
                  + "FROM trainer_subscriptions "
                  + "WHERE customerId = ? AND trainerId = ? AND status = ? "
                  + "AND startDate <= ? AND endDate >= ?";     // as-of date, ISO strings (rule 9)
       try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
           ps.setInt(1, customerId);
           ps.setInt(2, trainerId);
           ps.setString(3, SubscriptionStatus.ACTIVE.name());   // EXPIRED is derived (rule 13)
           ps.setString(4, date);
           ps.setString(5, date);
           try (ResultSet rs = ps.executeQuery()) {
               return rs.next() ? mapRow(rs) : null;   // one engagement, or none
           }
       } catch (SQLException e) {
           throw new DaoException("Could not check the trainer subscription", e);
       }
   }
   ```

5. Add `(customerId, trainerId)` as an index; a **`UNIQUE`** index is wrong here — a member may renew
   with the same coach for a later period, so overlapping periods are the service's date check, not
   uniqueness.
6. Wrap every `SQLException`; return empty lists from the two history queries.

**How to know it is right.**

1. An engagement from `2025-01-01` to `2025-03-31`: `findActive(c, t, "2025-02-15")` returns it, and
   `findActive(c, t, "2025-04-01")` returns `null` even though the stored status is still `ACTIVE`.
2. `findActive` with a different trainer id returns `null` — the second parameter matters.
3. `findByCustomer(c)` and `findByTrainer(t)` return newest-first history and empty lists when there is
   none.
4. `ratePerPeriod` round-trips to the cent and compares with `compareTo`.
5. Renewing writes a new `endDate` and leaves `startDate` alone.

**Common mistakes.**

- **Filtering on `status = 'ACTIVE'` only.** Rule 13 derives `EXPIRED`, so a stale row counts as active
  coverage and a session is priced `0` when it should not be (rule 11).
- **Swapping the two ids** in `findActive`. Both are `int`s, the compiler is happy, and the answer is
  quietly about the wrong pair — which is why the parameter order is documented on the interface.
- **Enforcing rule 15 in the SQL** with a join to `customers`. That would make this DAO the authority on
  assignment; the assignment is `Customer.trainerId` and the service compares them.
- **A `UNIQUE(customerId, trainerId)` index.** Renewals are legitimate; uniqueness would make the second
  period impossible to store.
- **Writing `EXPIRED` into the column during a sweep.** Rule 13 makes it derived — writing it makes the
  row lie the moment it is renewed.

### `TrainerAvailabilityDao.java` + `TrainerAvailabilityDaoImpl.java` — hourly slots and per-slot caps (owner: Yousef)

**Status:** planned. **Canonical:** `docs/UML.md` §3 (7 methods), §1 (`TrainerAvailability`), rules 8,
13 and 17.

**What it is.** The contract for the `trainer_availability` table: one published hourly slot per row — a
date, a start and end time, a `maxCustomers` cap for *that slot*, and a status a trainer sets to `OPEN`
or `CLOSED`. A trainer may publish several slots in a day (rule 8), so the day's query returns a list.

**Methods.**

| Method | Returns | Why it exists / rule it serves |
|---|---|---|
| `save(TrainerAvailability)` | `int` (new id) | Publish a slot |
| `findOpenSlots(int, String)` | `List<TrainerAvailability>` | The bookable slots for a coach on a date |
| `findById(int)` | `TrainerAvailability` | One slot, before an edit or a cap change |
| `findAll()` | `List<TrainerAvailability>` | The full list |
| `update(TrainerAvailability)` | `void` | Change `maxCustomers`, open or close the slot |
| `delete(int)` | `void` | Remove a slot that was published by mistake |
| `findByTrainerAndDate(int, String)` | `List<TrainerAvailability>` | Every slot of that day — the calendar view, and `setDailyCap`'s target set |

**Why it is designed this way.** `maxCustomers` is the cap **per slot**, not per day, so `setDailyCap`
is a service loop over `findByTrainerAndDate` rather than a column somewhere (rule 8). `findOpenSlots`
filters `status = 'OPEN'` and cannot filter occupancy: `AvailabilityStatus.FULL` is derived from
`bookedCount >= maxCustomers` and is never written to the table (rule 13), so a slot that looks open
here may already be full. Combining the two is the service's job, using
`SessionDao.countByAvailability(slotId, SessionStatus.CONFIRMED)` — the pair of queries rule 17 is
written in terms of. That split is deliberate: occupancy sums live in `SessionDao`, the only place that
knows what a session status means.

**How to write it.**

1. Interface, seven signatures; document `findOpenSlots(int, String)` as `(trainerId, date)`.
2. Impl with the `DbConnection` field; `mapRow` maps `trainerId`, `date`, `startTime`, `endTime`,
   `maxCustomers` (`getInt`) and `AvailabilityStatus.valueOf(...)`.
3. `save` with `RETURN_GENERATED_KEYS`; the service has already rejected `startTime >= endTime` through
   `Validators.validTimeRange` (rule 8), and the DAO stores what it is given.
4. Both list queries ordered `startTime` so the day reads top to bottom; both are `(trainerId, date)`
   lookups and use the composite index.

   ```java
   // Sketch — signature is authoritative in docs/UML.md §3.
   @Override
   public List<TrainerAvailability> findOpenSlots(int trainerId, String date) {
       String sql = "SELECT id, trainerId, date, startTime, endTime, maxCustomers, status "
                  + "FROM trainer_availability WHERE trainerId = ? AND date = ? AND status = ? "
                  + "ORDER BY startTime";
       List<TrainerAvailability> slots = new ArrayList<>();   // several slots per day (rule 8)
       try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
           ps.setInt(1, trainerId);
           ps.setString(2, date);
           ps.setString(3, AvailabilityStatus.OPEN.name());   // FULL is derived (rule 13)
           try (ResultSet rs = ps.executeQuery()) {
               while (rs.next()) slots.add(mapRow(rs));
           }
       } catch (SQLException e) {
           throw new DaoException("Could not load the trainer's open slots", e);
       }
       return slots;
   }
   ```

5. Wrap `SQLException`; return empty lists for a day with nothing published.

**How to know it is right.**

1. Publish three slots on one date: `findByTrainerAndDate(t, date)` returns three in `startTime` order,
   and `findOpenSlots(t, date)` returns the `OPEN` ones.
2. Close one slot (`status = CLOSED` through `update`) and re-run `findOpenSlots`: it is gone, with no
   row deleted.
3. Fill one `OPEN` slot to `maxCustomers` with `CONFIRMED` sessions: `findOpenSlots` still lists it —
   correct, because `FULL` is derived — and the service's `countByAvailability` check refuses the next
   booking (rule 17).
4. `maxCustomers` round-trips per slot; changing one slot's cap leaves the day's other slots alone.
5. A date with nothing published returns an empty list, not `null`.

**Common mistakes.**

- **Writing `FULL` into the status column** when a slot fills up. Rule 13 forbids it: cancelling a
  session (rule 17) would leave the slot permanently `FULL`, and capacity would never come back.
- **Returning a single slot from `findByTrainerAndDate`.** Rule 8 allows several per day; a single-row
  query silently drops the rest of the day.
- **Counting sessions in this DAO** (a `JOIN sessions` with `COUNT(*)` inside `findOpenSlots`). That
  puts a second table's rule about `CONFIRMED` here, and rule 17's single count query stops being the
  one place occupancy is computed.
- **Validating `startTime < endTime` here.** `Validators.validTimeRange` is the service's check
  (rule 8); the DAO would duplicate the message and the exception type.
- **Deleting a slot that has sessions pointing at it.** `sessions.availabilityId` references it, so
  either the foreign key refuses it or the session loses its slot — close the slot instead.

## 6. How this layer connects to the others

The call flow, top to bottom (`docs/UML.md` §2 is the canonical version of the layer rules):

1. `MainApp.start(Stage)` builds the composition root: `DbConnection db = new SqliteDbConnection();`
   then `db.initializeSchema()`, which creates `gym.db` and runs `schema.sql` on first run.
2. It constructs each DAO impl with that connection — `CustomerDao customerDao = new
   CustomerDaoImpl(db);` — and passes the **interface** into the service that owns it:
   `new CustomerService(customerDao, …)`. This is the only place a `*DaoImpl` is named.
3. A controller calls a service method. Controllers never hold a DAO and never contain SQL
   (`docs/UML.md` §2).
4. The service applies the business rules, then calls one or more DAO methods on its declared
   dependencies. A service needing another aggregate's data calls that aggregate's *service*
   ([`../service/README.md`](../service/README.md)), not its DAO.
5. The DAO opens a connection through `DbConnection.getConnection()`, binds parameters, executes one
   statement, maps rows into entities with `mapRow`, and closes everything through try-with-resources.
6. On `SQLException` the DAO throws `DaoException` with the operation in the message and the
   `SQLException` as the cause. The service lets it propagate unless it is translating a rule failure
   into `IllegalStateException`/`IllegalArgumentException`.
7. The controller catches it and calls `FxUtils.error(e.getMessage())` — a plain sentence, never a
   stack trace (`docs/UML.md` §2; root `README.md` §11).
8. What travels back up is always entities and `List`s, never a JDBC type and never SQL text.

| This folder depends on | It is used by |
|---|---|
| `util/DbConnection` (interface only, injected) — [`../util/README.md`](../util/README.md) | `service/*Service` (11 services, through the DAO interfaces) |
| `util/DaoException` (thrown here, defined there) | `controller/*` (catches it) and `util/GuardService` |
| `util/SqliteDbConnection` — **not here**; named only in `MainApp` | `MainApp` (constructs the impls) |
| `model/*` entities + the 11 enums — [`../model/README.md`](../model/README.md) | `util/GuardService.countReferences` (reads the same tables through JDBC) |
| `java.sql` (JDBC) and `java.math.BigDecimal` | — |
| `src/main/resources/db/schema.sql` — [`../../../../resources/README.md`](../../../../resources/README.md) | the SQLite file `gym.db` (git-ignored) |

**The seams.** There are two, and they are different seams. `DbConnection` is the *database* seam: one
interface, so "which database" is decided once (`DECISIONS.md` 33). The 13 `XxxDao` interfaces are the
*storage* seam: a service depends on a contract, so a test can hand it a hand-written fake with no file
and no JDBC. Both are the same idea — programme to what a collaborator *promises*, and keep the concrete
class at the edge of the system.

## 7. Common mistakes and review checklist

**Mistakes we expect (and the fix for each).**

| Symptom | Cause | Fix |
|---|---|---|
| `AbstractMethodError`, or a method that "does nothing" | The impl's method has a different name or parameter type than the interface (`findbyId`, `findById(Integer)`) | Add `@Override` — the compiler then refuses the typo |
| `UnsupportedOperationException` when a caller adds to a returned list | A DAO returned `List.of()`/`Collections.emptyList()` and the caller mutated it | Return a fresh `ArrayList<>`, or copy on the way out |
| `NullPointerException` in a controller loop | A `find…` returned `null` for "no rows" | Return an empty list; keep `null` for single-object methods only (rule 16) |
| `gym.db` locked / `SQLITE_BUSY` | A `ResultSet` or `Statement` was not closed, or a `Connection` was cached in a field | try-with-resources around `Connection`, `PreparedStatement`, `ResultSet`, in that order |
| A revenue figure that is a cent or two off | Money summed as `double` (in Java, or by SQL over a `TEXT` column) | Settle open detail A and use `BigDecimal` end to end (`DECISIONS.md` 31) |
| A member with no trainer appears assigned to "trainer 0" | `rs.getInt` on the nullable `trainerId` column | `(Integer) rs.getObject("trainerId")`, or `getInt` + `wasNull()` (rule 13) |
| `IllegalArgumentException: No enum constant …` | A stored literal was renamed, or the column holds free text | Migrate the data; never map unknown values to a default, which makes the enum lie |
| The same customer checked in three times in a day | `LIMIT 1` in `findByCustomerAndDay` hid the duplicate | Return the list and let the service report it (rule 5) |
| An agenda that changes order between runs | No `ORDER BY`, so SQLite's row order decides | Order every list query explicitly |
| A full slot never frees after a cancellation | `FULL` was written to the status column instead of derived | Keep `FULL` derived (rule 13) and count `CONFIRMED` in the service (rule 17) |
| A stack trace in a dialog | The driver's message was re-thrown as the `DaoException` message | Wrap with a short operation sentence and chain the cause |
| Two screens showing different totals for the same money | One screen sums in SQL, another in Java, over a different set of rows | One query, one place, with `voided = 0` in it (rule 6) |

**Review checklist for a change to this folder.**

- [ ] The class implements the interface it claims to, `@Override` is on every method, and the signature
      list matches `docs/UML.md` §3 exactly — no added, renamed or reordered parameters.
- [ ] The constructor takes a `DbConnection` and stores it `private final`; nothing in the file mentions
      `SqliteDbConnection`.
- [ ] Every method is one statement inside try-with-resources, and every `SQLException` becomes a
      `DaoException` with the cause chained.
- [ ] Every user-supplied value is a bound `?`; no value is concatenated into SQL.
- [ ] `INSERT`s use `Statement.RETURN_GENERATED_KEYS` and `save` returns the new id.
- [ ] There is exactly one `private mapRow(ResultSet)`, `SELECT` lists are explicit, and columns are read
      by name.
- [ ] Nullable columns are read through `getObject`/`wasNull()`; primitives read `NOT NULL` columns.
- [ ] Enum columns store `name()`, never `ordinal()`.
- [ ] Money is `BigDecimal` in the agreed storage form; no `double` anywhere in the file.
- [ ] Dates are ISO-8601 strings compared as strings; no `Date`/`LocalDate` in a signature.
- [ ] List methods return empty lists, never `null`; single-object methods document `null` as "not
      found".
- [ ] No JavaFX import, no validation, no status transition, no pricing, no user-facing wording, and no
      `System.out.println` left behind.
- [ ] `AttendanceDao.delete` and `PaymentDao.delete` still have no caller (rule 19).
- [ ] The SQL matches `schema.sql`: table names, column names and the `CHECK` constraint.

## 8. Learning exercises

1. **Read §3 and find every method that returns a single object.** For each, write down what `null`
   means and which service decision it forces.
2. **Write the `search` query twice** — once as a concatenated string, once with bound parameters — then
   explain in one sentence what an attacker gains from the first form.
3. **Find the three bugs in this snippet** and name the rule each one breaks:

   ```java
   public Session findById(int id) {
       Connection conn = db.getConnection();
       Statement st = conn.createStatement();
       ResultSet rs = st.executeQuery("SELECT * FROM sessions WHERE id = " + id);
       Session s = new Session();
       s.setId(rs.getInt(1));
       s.setAvailabilityId(rs.getInt(8));
       return s;
   }
   ```

4. **Add a filter without breaking the design.** A screen needs "trainers who specialize in yoga", and
   §3 has no such method. List, in order, every file that must change, and explain why editing only
   `TrainerDaoImpl.java` does not work.
5. **Make the revenue figure exact.** With money stored as `TEXT` (`'45.00'`), explain why `SUM(amount)`
   in SQLite cannot be trusted to the cent, then write the storage form and the `revenueBetween`
   conversion that can.
6. **Trace a failure.** The database file is read-only when a member is saved. Name the first method that
   sees the problem, the type that travels up through the service, the type the controller catches, and
   what the receptionist finally reads on screen.

<details>
<summary>Answers</summary>

1. Every `findById(int)`, plus `MedicalProfileDao.findByCustomer(int)`, `PaymentDao.findByReceipt
   (String)`, `AdminDao.findByUsername(String)` and `TrainerSubscriptionDao.findActive(int, int,
   String)`. In all five, `null` means "no such row", and each feeds a real decision: create the profile
   on demand (rule 3), look the receipt up again (rule 19), return `null` from `authenticate` for an
   unknown username (rule 16), and price a session as covered or not (rule 11). None throws for "not
   found", because "not found" is normal input.
2. The concatenated form injects: with `term = "' OR 1=1 --"` the `WHERE` clause is true for every row,
   and `"'; DROP TABLE customers; --"` is a statement if the driver allows multiple statements. The
   bound form sends SQL text and value separately, so the value can only ever be a value — and
   `O'Brien` stops breaking the query as a bonus (rule 7).
3. (a) `Statement` plus concatenation instead of a `PreparedStatement` with a bound parameter — SQL
   injection and the `O'Brien` break (rule 7). (b) No try-with-resources: the connection, statement and
   result set all leak, which is what locks `gym.db` (rule 8). (c) `SELECT *` with index-based reads
   (`getInt(8)`) and `getInt` on the nullable `availabilityId`, which turns SQL `NULL` into `0`; the
   missing `SQLException` translation is part of the same picture, since a checked exception cannot
   escape that method (rules 13, 14, 7).
4. In order: `docs/UML.md` §3 (add `findBySpecialization(String)` to `TrainerDao`), `DECISIONS.md` only
   if the team had a real choice to record (it does not — this is an addition to the agreed surface),
   `TrainerDao.java`, `TrainerDaoImpl.java`, `TrainerService`, then this README's method table. Editing
   only the impl fails because the service holds a `TrainerDao` reference: the new method is invisible
   at the call site, and the surface in §3 stops describing the code (`DOCS_STANDARD.md` §1: `UML.md`
   wins, and changes go there first).
5. SQLite has no decimal type, so `SUM` over `TEXT` converts each value to a `REAL` (an IEEE 754 binary
   float) and accumulates there; the rounding error `DECISIONS.md` 31 banned with `double` reappears
   inside the database. Store money as `INTEGER` cents, `SUM(amountCents)` (exact integer addition),
   then convert once: `BigDecimal.valueOf(totalCents, 2)`. On the way in,
   `movePointRight(2).longValueExact()` also refuses an amount with a third decimal instead of silently
   truncating it.
6. `CustomerDaoImpl.save` catches the `SQLException` first and throws
   `DaoException("Could not save customer", e)`. The service lets it propagate (it is unchecked, and the
   service's own failures are `IllegalArgumentException`/`IllegalStateException`). The controller catches
   `DaoException` and calls `FxUtils.error(e.getMessage())`, so the receptionist reads a plain sentence
   such as "Could not save customer" — the SQLite text goes to the log through `getCause()` (root
   `README.md` §11).

</details>

## 9. Ownership and tasks

Owners are as in `docs/UML_TEAM.md` (Abdelrhman, Ziad, Yousef, Abdel Raouf), and the write order is the
order of the entries above: schema → connection pair → exception → `Customer` → `Trainer` →
`Membership` → `WorkoutProgram` → `Session` → `Equipment` → `Attendance` → `Payment` → `Progress` →
`MedicalProfile` → `Admin` → `TrainerSubscription` → `TrainerAvailability`.

| Task | Owner | Status |
|------|-------|--------|
| Skeleton + this README created | Abdelrhman | Done |
| `DbConnection` + `SqliteDbConnection` + `schema.sql` v1 + DAO base pattern (Week 1, foundation) | Abdelrhman | To Do |
| `CustomerDao` + impl | Abdelrhman | To Do |
| `TrainerDao`, `MembershipDao` + impls | Ziad | To Do |
| `WorkoutProgramDao`, `SessionDao`, `EquipmentDao` + impls | Yousef | To Do |
| `AttendanceDao`, `PaymentDao` + impls | Abdel Raouf | To Do |
| `ProgressDao`, `ChartUtils` support + impls | Abdel Raouf | To Do |
| `MedicalProfileDao`, `AdminDao` + impls | Abdelrhman | To Do |
| `TrainerSubscriptionDao`, `TrainerAvailabilityDao` + impls | Yousef | To Do |

"DAO base pattern" in that table means the shared *shape* every pair uses — a `DbConnection` constructor
parameter, one `private mapRow`, try-with-resources and `DaoException` translation — not an
`AbstractJdbcDao<T>` base class: the 13 impls stay flat, for the reasons in `DECISIONS.md` 36 and
`docs/UML.md` "Notes on scope".

Mirror in the `swing` branch: DAO + service layers should be portable with minimal changes (no
JavaFX imports allowed here — enforced in review). That branch has no Java files yet.

### Definition of done for a file in this folder

- [ ] The interface and the impl exist, the impl `implements` the interface, and every method has
      `@Override`.
- [ ] Every method signature matches `docs/UML.md` §3 character for character (name, parameter types,
      return type, order).
- [ ] Every method opens its own resources in try-with-resources and translates every `SQLException`
      into a `DaoException` with the cause chained (`DECISIONS.md` 40).
- [ ] Every parameter is bound; no SQL string is built from a value.
- [ ] `save` returns the generated key; `findById` returns `null` for a missing row; every list query
      returns an empty list rather than `null`.
- [ ] One `private mapRow(ResultSet)`; explicit column lists; columns read by name; nullable columns
      read through `getObject`/`wasNull()`.
- [ ] Enums stored as `name()`; money as `BigDecimal` in the agreed storage form; dates as ISO-8601
      strings compared as strings.
- [ ] No JavaFX import, no business rule, no user-facing wording, no validation, no `System.out`.
- [ ] The SQL matches `schema.sql` (table, columns, constraint), and the queries the business rules need
      from §5 are present and take the documented parameters.
- [ ] `gradlew build` is green, and the DAO's own operations were exercised by hand against `gym.db`
      (save → read → update → read) before the task is marked Done.
