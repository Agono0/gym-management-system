# LEARNING_GUIDE.md — how to learn this project (and defend it in the presentation)

> **Read this once before you read any folder README.** It tells you in which order to study the
> project, which OOP concept each layer teaches, what every abbreviation means, and which questions
> an examiner will ask about our design.
>
> **Repo state today (Milestone 2 shipped):** The UI shell (`GymApp`), gallery (`MainApp`), all 20
> Java-built controllers (`Screen` / `ScreenController`), and the complete Milestone 2 backend
> (`model/`, `dao/`, `util/`, `exception/`) exist and compile cleanly with Java 21 bytecode target.
> Per `docs/DECISIONS.md` 77, the backend follows COMP 301 Lectures 1–6 (concrete SQLite DAOs,
> `java.io.Serializable` entities, `SerializationUtil`, and checked exceptions `GymException` →
> `ValidationException` / `DatabaseException`), while the separate 11-service tier was retired.
> Controllers still run with demo data until Milestone 3 connects them to the DAOs.

---

## 1. The document set, and which one to trust for what

| Document | What it is | Read it when |
|---|---|---|
| `README.md` (root) | The product: idea, features, tech stack, team split, timeline, error-handling standard | First — 15 minutes |
| `docs/UML.md` | **The design.** Domain model (§1–§1b), layered architecture (§2), 13 concrete DAOs (§3), retired services note (§4), controllers (§5), utilities & exceptions (§6), external inheritance (§7), 19 business rules | Second — canonical reference |
| `docs/LEARNING_GUIDE.md` (this file) | Study path, OOP concept index (COMP 301 aligned), glossary, defence questions | Now, and before your presentation |
| `docs/DOCS_STANDARD.md` | How to write/extend folder docs without drifting from `UML.md` | Before you edit any README |
| `docs/DECISIONS.md` | Why each non-obvious choice was made (numbered, append-only) | Whenever you ask "why is it like this?" |
| `docs/UML_TEAM.md` | Who owns which class/feature and current status | When you pick up work |
| `docs/BUILD_AND_RUN.md` | Gradle files, commands, JDBC/SQLite `gym.db` paths, troubleshooting | Before your first build |
| `docs/COVERAGE.md` | Honest checklist of what is built vs pending | Before claiming anything is done |
| Folder READMEs (`model/`, `dao/`, `service/`, `controller/`, `util/`, `resources/`) | Layer teaching notes: purpose, OOP concepts, patterns, rules, per-file breakdown | While working inside a folder |
| `PLAN.md`, `AGENTS.md` | The `m3fx` **library** roadmap and rules — not the gym app | Only if you touch `m3fx-*` |

**Rule of authority:** `docs/UML.md` §1 and §1b win for domain names, types, constructors, and business rules. If anything changes, update `UML.md` and `DECISIONS.md` alongside code and READMEs.

---

## 2. Study path

### 2.1 The 90-minute orientation

1. Root `README.md` §1–§4 — what we are building and with what stack.
2. `docs/UML.md` §2 — the layered architecture: `GymApp`/`MainApp` → `controller/` (UI, Java-built `ScreenController`) → `dao/` (13 concrete SQLite DAOs) → SQLite `gym.db`, supported by `util/` and `exception/`. Remember: `model/`, `dao/`, `util/`, `exception/` never import JavaFX.
3. `docs/UML.md` §1 — skim the 13 entities and 11 enums.
4. `docs/UML.md` → "Business rules" — read all 19 rules to see what Milestone 2 validates today (`ValidationException` + SQL DDL) and what Milestone 3 enforces in controller-to-DAO workflows.
5. `docs/BUILD_AND_RUN.md` §5 — run `.\gradlew.bat runGym` and `.\gradlew.bat run`.

### 2.2 The milestone view

| Stage | Scope | Status | Key files |
|---|---|---|---|
| Library & UI Shell | Dynamic M3 theme (`m3fx`), `MainApp` gallery, `GymApp` shell, 20 Java-built controllers | Done (Demo data) | `MainApp.java`, `GymApp.java`, `controller/*.java` |
| Milestone 2 Backend | 13 `Serializable` entities, 11 enums, 3 checked exceptions, `DatabaseConnection` DDL, `SerializationUtil`, 13 concrete DAOs | **Done** | `model/*.java`, `dao/*.java`, `util/*.java`, `exception/*.java` |
| Milestone 3 Integration | Wire each screen controller directly to the concrete DAOs and `SerializationUtil`; enforce rules 1–19 interactively | Next | `controller/*.java` calling `dao/*.java` |

---

## 3. OOP concept index (COMP 301 Lectures 1–6)

Every concept below appears directly in the codebase.

### 3.1 Classes, objects, state and behaviour

| Concept | Evidence in the project | Why we need it | Exercise |
|---|---|---|---|
| Class vs object | Every entity (`Customer`, `Trainer`, `Payment`) | One class definition serves hundreds of runtime member/trainer instances | Instantiate two `Customer` objects with different IDs and print `toString()` |
| Constructor + overloading | Every entity has `Customer()` (no-arg) **and** `Customer(int, String, ...)` (`UML.md` §1) | DAOs use the no-arg constructor while populating fields from `ResultSet`; callers use the full constructor for convenience | Build a `Customer` both ways and compare fields |
| `this` and field shadowing | `this.fullName = fullName;` in constructors and setters | Without `this`, the parameter hides the field and leaves it `null` | Inspect `Customer.java` setters |
| Static vs instance | `DatabaseConnection.getConnection()` and `SerializationUtil.saveToFile(...)` are `static`; `customer.getEmail()` is an instance method | Opening DB connections or saving files needs no object state; reading a member's email needs *that* member | Notice why `DatabaseConnection` has a `private` constructor |
| Factory methods | `Payment.forMembership(...)`, `Payment.forSubscription(...)`, `Payment.forSession(...)` | Named factory methods avoid swapping three adjacent `Integer` IDs (`membershipId`, `subscriptionId`, `sessionId`) | Construct one `Payment` using `Payment.forSession(...)` and check which IDs are `null` |

### 3.2 Encapsulation and information hiding

| Concept | Evidence | Why we need it | Exercise |
|---|---|---|---|
| Private fields + public accessors | All 13 entities: `private String phone;` + `getPhone()` / `setPhone()` | Hides internal layout and keeps the `UML.md` §1 contract stable | Spot-check `Customer.java` against `UML.md` §1 |
| Not exposing JDBC internals | DAOs return domain entities or `List<Entity>`, never `ResultSet` or `Connection` | Returning a live `ResultSet` leaks open cursors and locks SQLite (`gym.db`) | Check all 13 `*Dao.java` public signatures |
| Package boundaries | `model`, `dao`, `util`, `exception` contain zero `javafx.*` imports | Keeps persistence portable (e.g. to a Swing UI branch or headless CLI verifier) | Search `src/main/java/com/gym/dao/` for `javafx` (0 hits) |

### 3.3 Enums as a type system

| Concept | Evidence | Why we need it | Exercise |
|---|---|---|---|
| Enum instead of magic numbers/strings | 11 enums in `com.gym.model` (`CustomerStatus`, `PlanType`, `SessionStatus`, `StaffRole`, etc.) | The compiler rejects typos like `StaffRole.ADMNI` and lists legal values | Replace one enum field with a `String` in a thought experiment and see what compiler safety is lost |
| Persistence as `name()` / `valueOf()` | Every DAO binds `enum.name()` in `PreparedStatement` and parses with `EnumType.valueOf(str)` | Readable in SQLite rows and safe if enum constants are reordered (unlike `ordinal()`) | Inspect `mapRow` in `CustomerDao.java` |

### 3.4 Exception handling & Serialization (COMP 301 Lecture 6)

| Concept | Evidence | Why we need it | Exercise |
|---|---|---|---|
| Custom checked exceptions (`extends Exception`) | `GymException extends Exception`; `ValidationException` and `DatabaseException` extend `GymException` | Forces callers to handle validation bugs (`ValidationException`) separately from SQLite/file failures (`DatabaseException`) | Trace `CustomerDao.insert(c)`'s `throws DatabaseException, ValidationException` clause |
| Exception chaining | `throw new DatabaseException("Failed to insert customer", e);` | Preserves the original `SQLException` or `IOException` inside `getCause()` for debugging | Catch `DatabaseException` and print `e.getCause()` |
| Successive `catch` blocks | `SerializationUtil.loadFromFile(String)` catches `IOException` then `ClassNotFoundException` separately | Teaches clear Lecture 6 Slide 13 exception handling without compressed multi-catch syntax | Read lines 45–49 of `SerializationUtil.java` |
| `try`-with-resources | Every DAO method opens `Connection`, `PreparedStatement`, and `ResultSet` inside `try (...)` | Automatically closes resources even on exception, preventing SQLite `SQLITE_BUSY` file locks | Check `findAll()` in `PaymentDao.java` |
| `Serializable` & `serialVersionUID` | All 13 domain entities `implements Serializable` with `private static final long serialVersionUID = 1L;` | Required so `ObjectOutputStream` / `ObjectInputStream` can serialize objects cleanly across minor edits | Inspect `Administrator.java` and `Payment.java` |
| Reading `Object` + explicit cast | `SerializationUtil.loadFromFile(String)` returns `Object`; caller casts `(Customer) SerializationUtil.loadFromFile(...)` | Follows Lecture 6 Slide 39 directly without advanced `<T>` generic methods | Load a saved `List<Trainer>` or `Customer` using `SerializationUtil` and cast it |

### 3.5 Collections, money & dates

| Concept | Evidence | Why we need it | Exercise |
|---|---|---|---|
| `List<Entity>` over raw arrays | Every `findAll()` and `findBy*()` returns `List<Entity>` (backed by `ArrayList<>`) | Result sizes vary dynamically per table query | Read `findByCustomerId` in `MembershipDao.java` |
| Loops without Streams/Lambdas (backend) | Backend DAOs and utilities use standard `for` and `while` loops only | Keeps `model/`, `dao/`, `util/`, `exception/` strictly within Lectures 1–6 syntax | Inspect `DatabaseConnection.initializeDatabase()` loop |
| `BigDecimal` for money | `Trainer.hourlyRate`, `Membership.price`, `Payment.amount`, `Session.price`, `TrainerSubscription.ratePerPeriod` | Binary `double` loses cents (`0.1 + 0.2 != 0.3`); `BigDecimal` keeps money exact (`DECISIONS.md` 31) | Remember to construct with `new BigDecimal("45.00")` and compare with `.compareTo(...) == 0` |
| ISO-8601 date/time strings | `yyyy-MM-dd` and `HH:mm` stored as `String` / `TEXT` | SQLite has no native date type, and ISO strings sort chronologically in SQL `ORDER BY` | Inspect `PaymentDao.findByCustomerId` ordering |
| Boxed vs primitive nullability | `Customer.trainerId` (`Integer`), `Payment.membershipId` (`Integer`) vs `int id`, `boolean voided` | Boxed types hold `null` when a column is SQL `NULL` (`rs.wasNull()`); primitives forbid `null` | Read `mapRow` in `CustomerDao.java` (`wasNull()`) |

### 3.6 Architecture & modularity

| Concept | Evidence | Why we need it | Exercise |
|---|---|---|---|
| Flat concrete DAOs | 13 concrete `*Dao` classes (`CustomerDao`, `PaymentDao`, ...) instead of 26 interface/impl files | Matches beginner team size (`DECISIONS.md` 77); each class owns one SQLite table | Compare `UML.md` §3 with `src/main/java/com/gym/dao/` |
| Consolidated service tier | Service classes retired (`DECISIONS.md` 77); basic validation in DAO `validate(...)`, workflows in Milestone 3 controllers | Eliminates 11 pass-through files so students focus directly on Entities, DAOs, Serialization, and Exceptions | Explain why `service/` holds only its `README.md` |
| Pure-Java UI screens | `Screen` enum + `ScreenController` interface (`getRoot()`) built by controllers in code (`fb3486d`) | Removes fragile runtime XML reflection errors and lets every screen assemble M3 controls directly in Java | Read `Screen.java` and `AppShellController.java` |

---

## 4. Glossary

| Term | Meaning in this project |
|---|---|
| **`BigDecimal`** | Exact decimal type for money; construct from `String`, compare with `compareTo`. |
| **Boxed type** | `Integer`/`Double` wrapper allowing `null` to match nullable SQLite columns (`wasNull()`). |
| **DAO** | Data Access Object: a concrete class in `com.gym.dao` owning SQLite CRUD for one entity table (`insert`, `update`, `delete`, `findById`, `findAll`, plus domain lookups). |
| **DDL** | Data Definition Language (`CREATE TABLE IF NOT EXISTS ...`) executed in `DatabaseConnection.initializeDatabase()`. |
| **FK (Foreign Key)** | Numeric ID pointing to another table's row (`customerId`, `trainerId`) instead of nested Java object graphs. |
| **FX thread** | JavaFX's single UI thread where all UI creation and updates happen. |
| **JDBC** | Standard Java database API (`Connection`, `PreparedStatement`, `ResultSet`, `Statement`). |
| **Looked-up color** | Dynamic CSS variable (`-md-sys-color-primary`) generated by `m3fx` so light/dark and seed changes repaint all controls. |
| **Prepared statement** | Parameterized SQL (`?` placeholders) preventing SQL injection and escaping issues. |
| **`Serializable`** | Marker interface implemented by all 13 domain entities (`serialVersionUID = 1L`) for `SerializationUtil`. |
| **`ScreenController`** | UI interface implemented by Java-built screen controllers (`Parent getRoot()`), instantiated via `Screen` enum. |
| **SQLite** | Embedded file database stored in `gym.db` in the working directory at runtime. |
| **XOR link** | Payment links (`membershipId`, `subscriptionId`, `sessionId`) where one target is set via `Payment.forMembership` / `forSubscription` / `forSession` (`UML.md` rule 12). |

---

## 5. Cross-layer exercises

1. **Trace a save:** Write the path from a `Customer` object created in memory, validated in `CustomerDao.insert`, saved into SQLite `gym.db`, and returned with its generated `id`. Which exceptions can `insert` throw?
2. **Trace binary serialization:** Write 4 lines saving a `Payment` to `"backup.ser"` via `SerializationUtil.saveToFile(...)` and loading it back with an explicit `(Payment)` cast via `SerializationUtil.loadFromFile(...)`. What happens if `Payment` lacked `implements Serializable`? (`NotSerializableException` wrapped in `DatabaseException`).
3. **Spot the `BigDecimal` pitfall:** SQLite reads `"450.00"` back from `NUMERIC` as scale `0` (`450`). Why does `new BigDecimal("450.00").equals(fromDb)` return `false` while `.compareTo(fromDb) == 0` returns `true`?
4. **Trace a file lock bug:** Why must `ResultSet`, `PreparedStatement`, and `Connection` always sit inside `try-with-resources` with SQLite?

---

## 6. Key defence questions (presentation prep)

**Domain Modelling & OOP**
1. *Why 13 separate entity classes instead of one giant class?* — Single responsibility: each table (`customers`, `payments`, `sessions`, ...) has distinct fields, identity, and lifecycle.
2. *Why reference other entities by ID (`int`/`Integer`) instead of holding `Customer` objects inside `Session`?* — Keeps entities lightweight `Serializable` POJOs, avoids circular object graphs, and maps 1:1 to SQLite foreign key columns.
3. *Why `Integer` for `trainerId` but `int` for `id`?* — Primitive `int` forbids `null` (every row has an ID), while `Integer` cleanly represents optional `NULL` links in SQLite.
4. *Why is `Payment` constructed with factory methods like `Payment.forMembership(...)`?* — All three link fields are `Integer`, so passing `(null, 5, null)` directly invites silent argument swaps; named factories prevent that mistake.
5. *Why `BigDecimal` instead of `double` for money?* — Binary floating-point `double` drifts on decimal values like `0.1`; receipts and payments require exact arithmetic.
6. *Why store dates as ISO-8601 `String` (`yyyy-MM-dd`) instead of `LocalDate` or SQLite timestamps?* — SQLite has no dedicated date type, and ISO-8601 strings sort chronologically in SQLite text columns without timezone bugs.
7. *Why keep entities as mutable POJOs with no-arg constructors rather than Java `record`s?* — DAOs build entities incrementally from `ResultSet` rows via setters, and all entities follow Lecture 1–6 OOP class structure.

**Serialization & Exceptions (Lecture 6)**
8. *Why do all 13 entities implement `Serializable` with `serialVersionUID = 1L`?* — So `SerializationUtil` can persist entities or lists directly via `ObjectOutputStream` / `ObjectInputStream` without `InvalidClassException` version mismatches.
9. *Why does `SerializationUtil.loadFromFile(String)` return `Object` instead of using generics?* — Follows COMP 301 Lecture 6 Slide 39 directly: read `Object` and let the caller cast explicitly `(Customer)` / `(List<Trainer>)`.
10. *Why build a custom checked exception hierarchy (`GymException` → `ValidationException`, `DatabaseException`)?* — Separates user/input problems (`ValidationException`) from SQLite or file I/O errors (`DatabaseException`), matching Lecture 6 Slides 27–29.
11. *Why chain the cause in `new DatabaseException("...", e)`?* — Preserves the underlying `SQLException` or `IOException` stack trace for debugging while showing a clean top-level message to the caller.
12. *Why two separate `catch (IOException e)` and `catch (ClassNotFoundException e)` blocks in `SerializationUtil`?* — Keeps error messages distinct (disk/file read failure vs missing class definition) using plain Lecture 6 exception syntax.

**Data Access & Simplification (`DECISIONS.md` 77)**
13. *Why concrete DAOs (`CustomerDao`) and no separate `CustomerService` or DAO interface layer?* — After refactoring for beginner COMP 301 clarity, retiring 13 duplicate interfaces + 11 pass-through service classes reduced backend boilerplate while keeping clean separation between SQL (`dao/`) and UI (`controller/`).
14. *Why no `AbstractJdbcDao<T>` superclass?* — Each table has different SQL queries, column mappings, and validation rules; explicit concrete classes stay readable without hidden generic reflection.
15. *How does `DatabaseConnection.initializeDatabase()` prevent missing-table errors?* — Runs `CREATE TABLE IF NOT EXISTS` for all 13 tables in a single startup call.
16. *Why allow `DatabaseConnection.setDbUrl(...)`?* — Lets callers or verification scripts target an isolated SQLite file without modifying the production `jdbc:sqlite:gym.db` default.
17. *How do the DAOs stop SQL injection?* — Every parameterized query uses `PreparedStatement` with `?` placeholders instead of string concatenation.
18. *Why store enums as `.name()` instead of `.ordinal()`?* — Adding or reordering an enum literal changes ordinal numbers silently and corrupts existing rows; names stay human-readable and stable.
19. *Why keep `try-with-resources` on every JDBC call?* — SQLite uses file locks; an unclosed `Connection` or `ResultSet` causes `SQLITE_BUSY` errors.
20. *How can controllers use the backend without mixing SQL into UI screens?* — In Milestone 3, UI controllers only construct DAOs (or call static `SerializationUtil`) and catch `ValidationException` / `DatabaseException` to display user alerts, never writing SQL in `controller/`.

---

## 7. Habits for clean maintainable Java

1. **Follow `UML.md` §1 for domain classes.** Never rename entity fields or break constructor contracts without updating the diagram.
2. **Keep persistence free of JavaFX.** `model/`, `dao/`, `util/`, and `exception/` compile and run cleanly without any UI toolkit.
3. **Validate early, fail clearly.** Reject null/blank required fields before running SQL (`ValidationException`).
4. **Always close external resources.** Files (`ObjectInputStream`/`ObjectOutputStream`) and database connections (`Connection`/`Statement`/`ResultSet`) stay inside `try-with-resources`.
5. **Keep syntax readable for every teammate.** Prefer clear classes, loops, and explicit `catch` blocks from Lectures 1–6 over clever one-liners.
