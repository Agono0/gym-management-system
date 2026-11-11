# `util` & `exception` — Persistence, Serialization & Exceptions (JavaFX branch)

> **Status: SHIPPED (Milestone 2).** Both packages are built using COMP 301 Lectures 1–6 Java:
> - `com.gym.util.DatabaseConnection`: SQLite connection manager + idempotent 13-table `CREATE TABLE IF NOT EXISTS` DDL.
> - `com.gym.util.SerializationUtil`: binary `ObjectOutputStream` / `ObjectInputStream` file helper (Lecture 6).
> - `com.gym.exception`: checked exception hierarchy `GymException` → `ValidationException` / `DatabaseException`.
>
> Per `docs/DECISIONS.md` 77, previously planned extra helpers (`DbConnection` interface, `DateUtils`,
> `Validators`, `PasswordUtils`, `GuardService`, `SessionContext`, `FxUtils`, `ChartUtils`) were removed
> to match COMP 301 scope. Tracked in [`docs/COVERAGE.md`](../../../../docs/COVERAGE.md).

## 1. Purpose — the problem these packages solve

The backend needs shared database setup, portable binary file backups, and domain exceptions that UI
callers can catch cleanly without handling low-level `SQLException` or `IOException` details.

**What breaks if these packages are missing.** Without `DatabaseConnection`, every DAO would hardcode
`"jdbc:sqlite:gym.db"` and repeat table creation DDL 13 times (or crash with `no such table`).
Without `SerializationUtil`, saving snapshots of serialized entities requires repeating stream opening
and nested `try-catch` logic everywhere. Without `GymException` subclasses, checked `SQLException`,
`IOException`, and `ClassNotFoundException` types would leak directly into controllers.

**What this layer deliberately does not do.** Zero business rules (rule checks belong in DAO
validation or Milestone 3 workflow logic), zero JavaFX imports (`swing`-branch portable), and no
extra framework wrappers or generic `<T>` type erasure tricks beyond Lecture 6.

## 2. What you learn here (OOP concepts)

| Concept | Where in `util/` & `exception/` | Why the project needs it | How to apply it (one line) |
|---|---|---|---|
| Static utility classes + `private` constructor | `DatabaseConnection`, `SerializationUtil` | Helpers have no per-object instance state to allocate (`new SerializationUtil()` blocked) | Make constructor `private`; make helper methods `public static` |
| JDBC Connection + DDL (`Statement`) | `DatabaseConnection.getConnection()`, `initializeDatabase()` | Centralizes SQLite URL and ensures all 13 tables exist idempotently | Use `DriverManager.getConnection(dbUrl)` and `CREATE TABLE IF NOT EXISTS` |
| Object Serialization (`Lecture 6`, Slides 37–45) | `SerializationUtil.saveToFile`, `loadFromFile` | Saves and reloads `Serializable` entities/lists in compact binary format | Wrap `FileOutputStream`/`FileInputStream` with `ObjectOutputStream`/`ObjectInputStream` |
| Casting `Object` (`Lecture 6`, Slide 39) | `Object loadFromFile(String filePath)` | Keeps signature beginner-level without Java generic `<T>` methods | Callers explicitly cast: `(Customer) SerializationUtil.loadFromFile(path)` |
| Checked Exception Inheritance (`Lecture 6`, Slides 5–16) | `GymException` → `ValidationException`, `DatabaseException` | Lets callers distinguish user input mistakes from SQL/I/O failures | Extend `Exception`, chain causes via `super(message, cause)` |
| Separate `catch` blocks (`Lecture 6`, Slide 13) | `SerializationUtil.loadFromFile` (`IOException` then `ClassNotFoundException`) | Avoids Java 7+ multi-catch while giving clear error messages per failure cause | Catch specific exceptions sequentially before translating |

## 3. Design patterns in this layer

| Pattern | Where | Why this pattern | What goes wrong if used carelessly |
|---|---|---|---|
| **Static Utility Class** | `DatabaseConnection`, `SerializationUtil` | Central shared operations with a private constructor so callers cannot instantiate | Turning utilities into dumping grounds for business rules |
| **Exception Hierarchy & Chaining** | `GymException` → `ValidationException`, `DatabaseException` | Hides SQLite and `java.io` checked details behind clear domain exception types (`super(msg, e)`) | Swallowing `e` (`catch (Exception e) {}`) destroys root cause diagnostics |
| **Configurable URL hook** | `DatabaseConnection.setDbUrl` / `resetDbUrl` | Allows redirecting SQLite to a separate file URL when needed without editing DAOs | Leaving a temporary path active forgets to `resetDbUrl()` back to `gym.db` |

## 4. Rules you must follow (and the bug each rule prevents)

1. **Private constructors on utility classes** (`private DatabaseConnection()`, `private SerializationUtil()`).
2. **`initializeDatabase()` uses `CREATE TABLE IF NOT EXISTS` for every table** — running twice on
   an existing `gym.db` never wipes member records.
3. **Always guard `saveToFile` against `null` objects and missing parent directories** — avoids
   writing corrupt null streams or failing when a backup subdirectory does not exist yet.
4. **Keep separate `catch (IOException)` and `catch (ClassNotFoundException)` blocks** in
   `SerializationUtil.loadFromFile` (Lecture 6 style, easy to read and debug).
5. **Always pass the underlying exception (`cause`) when wrapping in `DatabaseException`** — keeps the
   original stack trace intact for logs while showing a clear top-level message.
6. **Zero JavaFX imports** in `com.gym.util` and `com.gym.exception`.

## 5. The files in `util/` and `exception/`

Write order: **exceptions first** (`GymException` → `ValidationException`, `DatabaseException`), then
**utilities** (`DatabaseConnection` → `SerializationUtil`).

### `com.gym.exception` (3 files)

| File | Inherits | `serialVersionUID` | Constructors | Purpose |
|---|---|---|---|---|
| `GymException.java` | `Exception` | `1L` | `()`, `(String)`, `(String, Throwable)` | Common checked base exception for backend failures |
| `ValidationException.java` | `GymException` | `1L` | `()`, `(String)`, `(String, Throwable)` | Input/domain check failure inside DAO `validate(...)` |
| `DatabaseException.java` | `GymException` | `1L` | `()`, `(String)`, `(String, Throwable)` | Wraps `SQLException`, `IOException`, and `ClassNotFoundException` |

### `DatabaseConnection.java` — SQLite connection & 13-table schema (owner: Abdelrhman)

**Status:** done (`191` lines). **Canonical:** `docs/UML.md` §3 & §6.

**What it does:** Holds `DEFAULT_DB_URL = "jdbc:sqlite:gym.db"`, exposes `setDbUrl`, `resetDbUrl`,
`getDbUrl`, `getConnection()`, and creates all 13 SQLite tables in `initializeDatabase()`
(`customers`, `trainers`, `memberships`, `workout_programs`, `sessions`, `equipment`, `attendance`,
`payments`, `progress_records`, `medical_profiles`, `administrators`, `trainer_subscriptions`,
`trainer_availability`).

```java
// Sketch — matches src/main/java/com/gym/util/DatabaseConnection.java
public static Connection getConnection() throws DatabaseException {
    try {
        return DriverManager.getConnection(dbUrl);
    } catch (SQLException e) {
        throw new DatabaseException("Could not connect to SQLite database: " + dbUrl, e);
    }
}
```

### `SerializationUtil.java` — Binary object I/O (owner: Abdelrhman)

**Status:** done (`51` lines). **Canonical:** `docs/UML.md` §6, COMP 301 Lecture 6 (Slides 37–45).

**What it does:** Saves any `Serializable` object (or `List`) with `saveToFile(Object, String)` and
reloads it with `Object loadFromFile(String)`. Caller casts the returned `Object` (no `<T>` generic method).

```java
// Sketch — caller casts returned Object (Lecture 6 Slide 39)
SerializationUtil.saveToFile(customer, "backups/customer.ser");
Customer loaded = (Customer) SerializationUtil.loadFromFile("backups/customer.ser");
```

### `PhoneUtil.java` — Egyptian mobile number rule (owner: Abdelrhman)

**Status:** done (`32` lines). **Canonical:** `docs/DECISIONS.md` 79.

**What it does:** One shared rule for every phone field: exactly 11 digits matching `01[0125]\d{8}`
(prefixes 010, 011, 012, 015). `isValid(String)` returns `true`/`false` for inline UI errors;
`requireValid(String, String)` throws `ValidationException` (blank or wrong format) for the DAOs.

```java
// Sketch — DAO validate(...) and controller usage
PhoneUtil.requireValid(c.getPhone(), "Customer phone");   // DAO: throws ValidationException
boolean phoneOk = PhoneUtil.isValid(phoneField.getText()); // controller: inline field error
```

## 6. How this layer connects to the others

| Package / Class | Depends on | Used by |
|---|---|---|
| `com.gym.exception` | Standard `java.lang.Exception` | `com.gym.util`, `com.gym.dao`, and Milestone 3 controllers |
| `com.gym.util.DatabaseConnection` | `java.sql.*`, `sqlite-jdbc`, `DatabaseException` | All 13 classes in `com.gym.dao` (and startup initialization in Milestone 3) |
| `com.gym.util.SerializationUtil` | `java.io.*`, `DatabaseException` | Backup/export utilities and Milestone 3 callers |
| `com.gym.util.PhoneUtil` | `ValidationException` | `CustomerDao`, `TrainerDao`, `AdministratorDao`, `MedicalProfileDao`, `CustomerListController`, `CustomerProfileController` |

## 7. Common mistakes and review checklist

| Symptom | Cause | Fix |
|---|---|---|
| `NotSerializableException` inside `saveToFile` | Passing a non-`Serializable` object (e.g., an un-serializable JavaFX control) | Save only `com.gym.model` entities or standard `ArrayList` collections of entities |
| `ClassCastException` after `loadFromFile` | Casting loaded `Object` to the wrong class | Match the cast class with what was written (or check with `instanceof`) |
| `DatabaseException: Could not connect to SQLite` | Missing `sqlite-jdbc` or `slf4j-api` on runtime classpath | Always launch via `./gradlew run` / `runGym`, or include `sqlite-jdbc` + `slf4j-api` jars on CLI classpath |
| Schema missing error (`no such table`) | Forgot `DatabaseConnection.initializeDatabase()` | Run `initializeDatabase()` once before executing DAO queries |

Checklist:

- [ ] `DatabaseConnection` and `SerializationUtil` keep `private` constructors.
- [ ] All 13 table DDLs match field types and column names read by the 13 DAOs.
- [ ] Both `IOException` and `ClassNotFoundException` are caught cleanly in `loadFromFile`.
- [ ] `GymException`, `ValidationException`, and `DatabaseException` each preserve `(message, cause)` constructors.

## 8. Learning exercises (with answers)

1. Why does `SerializationUtil.loadFromFile(String filePath)` return `Object` instead of a generic
   `<T> T`?
2. Look at `DatabaseConnection.initializeDatabase()`: why must every SQL string say
   `CREATE TABLE IF NOT EXISTS` instead of `CREATE TABLE`?
3. Explain why `ValidationException` and `DatabaseException` both extend `GymException` instead of
   `RuntimeException`.
4. What two things does `SerializationUtil.saveToFile` check before opening `FileOutputStream`?
5. Why is `serialVersionUID = 1L` declared on `GymException`, `ValidationException`, and
   `DatabaseException`?

<details>
<summary>Answers</summary>

1. COMP 301 Lecture 6 (Slide 39) uses `ObjectInputStream.readObject()` returning `Object` with an
   explicit caller cast, keeping syntax strictly within Lectures 1–6 beginner scope.
2. Calling `initializeDatabase()` on every application startup must be harmless when `gym.db` and its
   tables already exist; plain `CREATE TABLE` would fail on the second launch.
3. Extending checked `GymException` forces callers to handle input validation vs persistence errors
   explicitly (`try / catch`), making error paths visible in method signatures (`throws`).
4. First, it checks `object == null` and throws `DatabaseException`. Second, it checks `parent != null
   && !parent.exists()` and calls `parent.mkdirs()` so missing directories are created automatically.
5. `Exception` itself implements `java.io.Serializable`, so declaring `serialVersionUID = 1L` avoids
   `-Xlint:serial` compiler warnings.

</details>

## 9. Ownership and tasks

| File | Responsibility | Owner | Status |
|---|---|---|---|
| `DatabaseConnection.java` | SQLite URL, `getConnection()`, 13-table `CREATE TABLE IF NOT EXISTS` DDL | Abdelrhman | Done (Milestone 2) |
| `SerializationUtil.java` | Binary object I/O (`saveToFile`, `loadFromFile`) | Abdelrhman | Done (Milestone 2) |
| `PhoneUtil.java` | Egyptian 11-digit mobile rule (`isValid`, `requireValid`) | Abdelrhman | Done |
| `GymException.java`, `ValidationException.java`, `DatabaseException.java` | Checked backend exceptions | Abdelrhman | Done (Milestone 2) |

`swing` branch rule: both `com.gym.util` and `com.gym.exception` are pure Java with zero UI toolkit
imports and transfer unchanged to the `swing` branch.

### Definition of done for a file in these packages

- [ ] Compiles cleanly under `./gradlew compileJava` with `-Xlint:all` (including `-Xlint:serial`).
- [ ] Zero JavaFX imports.
- [ ] Every exception preserves chained root causes (`super(message, e)`).
