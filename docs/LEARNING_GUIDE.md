# LEARNING_GUIDE.md — how to learn this project (and defend it in the presentation)

> **Read this once before you read any folder README.** It tells you in which order to study the
> project, which OOP concept each layer teaches, what every abbreviation means, and which questions
> an examiner (or a senior engineer) will ask about our design.
>
> **Repo state today:** the documentation describes the **agreed design**, not shipped code. Only
> `com.gym.MainApp` (a placeholder) and `src/main/resources/fxml/main.fxml` (a component showcase)
> exist. Everything else in `docs/UML.md` is planned work with an owner — see `docs/COVERAGE.md` and
> `docs/UML_TEAM.md`. That is normal for week 1 of a company project: the design is written down and
> reviewed before the code is typed.

---

## 1. The document set, and which one to trust for what

| Document | What it is | Read it when |
|---|---|---|
| `README.md` (root) | The product: idea, features, tech stack, team split, timeline, error-handling standard | First — 15 minutes |
| `docs/UML.md` | **The design.** Domain model, layer diagrams, every class with its fields/signatures, the 19 business rules | Second — this is the reference you return to daily |
| `docs/LEARNING_GUIDE.md` (this file) | Study path, OOP concept index, glossary, exam questions | Now, and again before the defence |
| `docs/DOCS_STANDARD.md` | How to write/extend the folder docs so they cannot drift from `UML.md` | Before you edit any README |
| `docs/DECISIONS.md` | Why each non-obvious choice was made (numbered) | Whenever you ask "why is it like this?" |
| `docs/UML_TEAM.md` | Who owns which class/feature, ownership overlay | When you pick up work |
| `docs/BUILD_AND_RUN.md` | Gradle files line by line, commands, JDBC/SQLite paths, troubleshooting | Before your first build |
| `docs/COVERAGE.md` | What actually exists vs what is planned | Before claiming anything is "done" |
| Folder READMEs (`model/`, `dao/`, `service/`, `controller/`, `util/`, `resources/`) | Teaching material per layer: purpose, OOP concepts, patterns, per-file how-to, examples, mistakes, checklists, exercises | While you write that layer |
| `PLAN.md`, `AGENTS.md` | The `m3fx` **library** roadmap and rules — not the gym app | Only if you touch the library |

**Rule of authority:** `docs/UML.md` wins over every other document for names, types, signatures and
business rules. If you think the design is wrong, argue it in `docs/DECISIONS.md` and change
`UML.md` — never write code (or docs) that quietly disagrees.

---

## 2. Study path

### 2.1 The 90-minute orientation (do this on day 1)

1. Root `README.md` §1–§4 — what we are building and with what.
2. `docs/UML.md` §2 — the layered architecture picture and the four layer rules. Memorize these four:
   controllers never contain SQL and never touch a DAO; services and DAOs never import JavaFX; the
   `util` layer supports everyone; `FxUtils` is the only JavaFX-aware utility.
3. `docs/UML.md` §1 — skim the class boxes; you are not memorizing fields yet, you are learning the
   13 entities and 11 enums by name.
4. `docs/UML.md` → "Business rules" — read all 19. These are the rules that make the project a *system*
   instead of a set of CRUD screens.
5. `docs/BUILD_AND_RUN.md` §5 — run `gradlew run` once and watch the showcase window open.

### 2.2 The full path (matches the 7-week timeline in the root README)

| Week | Study | Build | You should be able to explain |
|---|---|---|---|
| 1 | Root README, `UML.md` §1–§3, `model/README.md`, `dao/README.md`, `util/README.md`, `BUILD_AND_RUN.md` | Enums, `Customer`, `DbConnection` + `SqliteDbConnection`, `DaoException`, `Validators`, `schema.sql` v1 | Why money is `BigDecimal`; why dates are strings; why `DbConnection` is an interface; what a prepared statement prevents |
| 2 | `controller/README.md`, `service/README.md` (Customer/Trainer parts), `resources/fxml/README.md` | `CustomerDao` + `CustomerService` + list/profile screens | The full path of one click: FXML handler → controller → service → DAO → SQLite → entity → `ObservableList` → table |
| 3 | `service/README.md` (Membership, Workout), `UML.md` rules 7, 14 | Membership + plan enum with behaviour, program versioning | Why renewing an expired membership differs from renewing a cancelled one; why freezing stores `frozenDays` |
| 4 | `service/README.md` (Scheduling, Equipment), `UML.md` rules 1, 8, 10, 11, 17, 18 | Slots, booking, conflict detection, the session state machine | Why `canTransition` is the single authority; why `bookedCount` counts only `CONFIRMED` |
| 5 | `service/README.md` (Attendance, Payment, Progress), `UML.md` rules 5, 6, 12, 19; `resources/db/README.md` | Attendance, payments with XOR links, charts | Why payments are never deleted; how a void plus a new payment keeps a receipt reconcilable |
| 6 | `controller/README.md` (all screens), `util/README.md` (PasswordUtils, SessionContext, GuardService), `UML.md` rules 4, 16 | Admin/override screens, hard-delete guard, error-handling sweep | Why an admin delete asks `GuardService.countReferences` first; what `requireRole(ADMIN)` protects |
| 7 | This guide §4–§6; `docs/DECISIONS.md` end to end | Documentation finalization, presentation, demo data | The 40 questions in §6 |

### 2.3 How to read one layer README

Each layer README has the same nine sections. Use them in this order:

1. §1 Purpose → *why the layer exists* (read the "what breaks if it is missing" paragraph twice).
2. §2 OOP concepts → *what you are practising here* (then jump to this file's §3 for the theory).
3. §3 Patterns → *the vocabulary senior engineers use*.
4. §4 Rules → *the constraints you must not break*, each with the bug it prevents.
5. §5 Files → *the actual how-to*: fields, why, steps, sketch, verification, mistakes. **Write your
   class while reading its entry, not after.**
6. §6 Connections → *where your class sits in the call flow*.
7. §7 Mistakes/checklist → *review your own PR before someone else does*.
8. §8 Exercises → *prove to yourself that you learned it*.
9. §9 Ownership → *your row, your status*.

---

## 3. OOP concept index — where each concept is learned in this project

Every concept below is **used**, not just mentioned. "Evidence" points at the class that proves it;
"exercise" is the smallest thing you can do to make it yours.

### 3.1 Classes, objects, state and behaviour

| Concept | Evidence in the project | Why we need it | Exercise |
|---|---|---|---|
| Class vs object | Any entity: `Customer` is the class; the row that reception loads is an object | A gym has hundreds of members — one *type*, many *instances* | Create two `Customer` objects with different ids and print both `toString()`s |
| Fields = state, methods = behaviour | `Membership` stores `startDate`/`endDate`/`status`; `MembershipService` computes renewal | Keep data where it belongs; computation where it belongs | Add a temporary `main` that builds a `Membership` and prints its `endDate` |
| Constructor + overloading | Every entity has `Customer()` **and** `Customer(int, String, …)` (`UML.md` §1) | A no-arg constructor is required by JavaFX/DAO mapping; the full one is convenient for tests and seeding | Call both constructors and compare the resulting objects |
| `this` and field shadowing | `this.fullName = fullName;` in every setter | Without `this`, the parameter hides the field and you assign a value to itself | Delete `this.` in one setter and explain the bug (the field stays `null`) |
| Static vs instance | `Validators.validEmail(...)` is static; `customer.getEmail()` is an instance call | Validation needs no object state; a customer's email needs *that* customer | Move `validEmail` to an instance method and count how many call sites break |
| Constants | `StaffRole.ADMIN` instead of the string `"ADMIN"`; `PlanType.MONTHLY` instead of `1` | A typo in `"ADMIN"` compiles; `StaffRole.ADMNI` does not | Replace an enum argument with a `String` in a sketch and find where the compiler stops helping you |

### 3.2 Encapsulation and information hiding

| Concept | Evidence | Why we need it | Exercise |
|---|---|---|---|
| Private fields + public accessors | All 13 entities: `private String phone;` + `getPhone()`/`setPhone()` (`«get/set»` in `UML.md` §1) | The public API of a class is a promise; the fields are an implementation detail we may change | Change a field's internal representation (e.g. store `phone` trimmed) without touching callers |
| Validation at the boundary, not in the setter | Rules 5, 7, 11, 12 live in services; entities stay dumb POJOs | Two callers would otherwise validate differently; also the DAO must be able to *load* a legacy row that a strict setter would reject | Explain why `setStatus(EXPIRED)` must stay possible for the DAO even though rule 13 forbids a human setting it |
| Not exposing internals | DAOs return entities, never `ResultSet` or `Connection`; SQL text never leaves `*DaoImpl` | A leaked `ResultSet` keeps a database cursor and a connection open | Find any method in `UML.md` §3 that returns a JDBC type (there is none — say why) |
| Package-level cohesion | `model`, `dao`, `service`, `controller`, `util` — one folder per responsibility | The package a class lives in tells you what it may depend on | Name the one package that may import JavaFX (it is `controller` + `util/FxUtils`) |

### 3.3 Abstraction, interfaces and polymorphism

| Concept | Evidence | Why we need it | Exercise |
|---|---|---|---|
| Interface as a contract | 13 DAO interfaces; `DbConnection` (`UML.md` §3, §6) | The service knows *what* is stored, not *how* | Write the six lines of `CustomerDao` from memory, then check against §3 |
| Programming to an interface | `CustomerDao dao = new CustomerDaoImpl(connection);` — the service field is typed as the interface | The database can be replaced (or faked in a test) without touching 13 services (`DECISIONS.md` 33) | Swap `new CustomerDaoImpl(...)` for a hand-written in-memory fake in a sketch and explain what else must change (nothing) |
| Runtime polymorphism / dynamic dispatch | Every `DaoImpl` realises its `Dao`; the call is resolved by the object, not the variable type | One service works with SQLite today and Postgres tomorrow | Add `System.out` to `CustomerDaoImpl.save` and to a fake impl; call both through a `CustomerDao` variable |
| Interface segregation | `ProgressDao` has only progress methods; no `GymDao` god interface | A service depends on the narrowest contract it needs; nothing forces a fake to implement 60 unused methods | List which interfaces `ProgressService` depends on (one) vs `SchedulingService` (five) |
| Composition over inheritance | Services hold DAOs as `final` fields; a `Session` holds `customerId`, not a `Customer` object (`DECISIONS.md` 32) | Deleting or replacing a collaborator is possible; a subclass hierarchy is not | Explain what breaks if `Session` held a `Customer` object (lazy loading, JDBC in the model, cycles) |
| Inheritance — and why it is absent | `UML.md` §7: only `MainApp extends Application` and `DaoException extends RuntimeException`; three candidate hierarchies were examined and rejected (`DECISIONS.md` 36) | "We could have" is not a reason; an empty base class costs clarity and hides the wiring | Argue *for* an `AbstractJdbcDao<T>` in one paragraph, then read `DECISIONS.md` 36 and answer it |
| `extends RuntimeException` | `DaoException` (`DECISIONS.md` 40) | Unchecked, so 13 DAOs do not each declare `throws`; and it chains via `super(message, cause)` so the original `SQLException` survives | Write `DaoException` in 6 lines, then show `e.getCause()` returning the original `SQLException` |

### 3.4 Enums as a type system

| Concept | Evidence | Why we need it | Exercise |
|---|---|---|---|
| Enum instead of magic numbers/strings | 11 enums in `UML.md` §1 | The compiler rejects `MembershipStatus.ACTVE` and IDE autocomplete lists the legal values | Replace `status` with an `int` in a sketch and count the places a wrong value would compile |
| Enum with state and behaviour | `PlanType` carries the plan price and duration (see `model/README.md`); `SessionStatus` drives `canTransition` | The plan's price belongs *to the plan*, not to a `switch` scattered in the controller | Add a `label()` method to `PlanType` and use it in a sketch instead of `if (plan == MONTHLY) "Monthly"` |
| `switch` over enums (exhaustive) | Renewal logic per `PlanType`; `canTransition(from, to)` | An exhaustive `switch` expression makes a forgotten case a compile error, not a silent `null` | Write `canTransition` as a `switch` expression and try deleting a case |
| Derived vs stored state | Rule 13: `FULL`, `EXPIRED` are derived on read; only `OPEN`/`CLOSED`, `FROZEN`, `CANCELLED` are set by hand | Storing a derived value means it goes stale the moment the underlying data changes (an `EXPIRED` membership that was renewed, a `FULL` slot after a cancellation) | Write `boolean isExpired(Membership m)` comparing `endDate` with today's ISO string — note there is no field to update |
| Enum literal producers | Each literal has exactly one owner (`UML.md` "Enumerations" table) | One writer per value is how you avoid "who set this to NO_SHOW?" | For `SessionStatus.NO_SHOW`, name the single method allowed to set it (`SchedulingService.markNoShow`) |

### 3.5 Exception design and defensive programming

| Concept | Evidence | Why we need it | Exercise |
|---|---|---|---|
| Checked vs unchecked | `SQLException` is checked (JDBC); `DaoException` is unchecked | Checked exceptions force `throws` noise through every layer; a UI cannot act on a raw `SQLException` anyway | Count how many signatures would gain `throws SQLException` if we did not wrap |
| Exception translation | `catch (SQLException e) { throw new DaoException("Could not save customer", e); }` in every `*DaoImpl` | The DAO knows the technical cause; the service/controller needs a message it can show | Write the one-line wrapper and note which layer adds the friendly wording |
| Exception chaining | `super(message, cause)` + `getCause()` (`DECISIONS.md` 40) | Without chaining you lose the stack trace that tells you *which* SQL failed | Print `e.getCause().getMessage()` in a snippet |
| Fail-fast validation | Services throw `IllegalArgumentException`/`IllegalStateException` with a displayable message; `CustomerService.validate` returns `List<String>` for form fields | A bad value must stop before it reaches SQL, and the UI must know *which field* is wrong | Decide which of the 19 rules need the list form (many fields validated at once) and which need fail-fast (single action) |
| `try`-with-resources | Every DAO method: `try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) { … }` | A leaked `Connection` locks `gym.db` (SQLite file lock) and the app appears to hang | Write one DAO method without try-with-resources and describe the symptom |
| Never show a stack trace to a user | Root `README.md` §11; controllers call `FxUtils.error(e.getMessage())` | Users cannot act on a stack trace, and it leaks internals | Rewrite `SQLException: [SQLITE_BUSY] database is locked` as a sentence a receptionist can act on |

### 3.6 Collections, generics and functional Java

| Concept | Evidence | Why we need it | Exercise |
|---|---|---|---|
| `List<T>` over arrays | Every `findAll()`/`findBy…` returns `List<Customer>` | Sizes vary at runtime; lists carry search results and tables | Write `findByStatus` returning `ArrayList<Customer>`; note why the interface (not the concrete class) is the declared return type |
| Why `findByCustomerAndDay` returns a **list** | `UML.md` §3; `dao/README.md` | A stray duplicate row must be visible, not silently dropped; rule 5 depends on it | Write the SQL with `LIMIT 1` and explain what bug that hides |
| Generics (used, not abused) | `List<ProgressRecord>`, `List<Double>` | Compile-time type safety; no casts | Explain why `ProgressService.series` returns `List<Double>` and not `List<ProgressRecord>` |
| Generics considered and rejected | No `AbstractJdbcDao<T>` (`DECISIONS.md` 36) | A generic base would hide the SQL differences and the 13 explicit wiring edges | Name the two things every DAO impl actually varies in (SQL, entity type) |
| Lambdas / functional interfaces | JavaFX event handlers (`setOnAction(e -> onSave())`), `ObservableList` listeners, cell value factories | The UI layer is event-driven; a lambda is the shortest honest way to pass behaviour | Convert an anonymous `new EventHandler<ActionEvent>() { … }` into a lambda |
| Streams where they genuinely help | `ProgressService.series` mapping records to one metric; `ChartUtils` formatting values | Filtering/mapping a list without index bookkeeping | Write `records.stream().map(ProgressRecord::getWeightKg).toList()` |
| `Optional` — deliberately not used | The design returns `null` for "not found" (`AdminService.authenticate` returns `null`) with a documented meaning | Consistency matters more than novelty; `null` from a DAO lookup is checked by the service before use | Decide where `Optional` *would* have been better (`findByReceipt` returning no payment) and note it as a future improvement, not a silent change |

### 3.7 Money, dates and correctness

| Concept | Evidence | Why we need it | Exercise |
|---|---|---|---|
| `BigDecimal` for money, never `double` | All money fields; `revenueBetween`, `balance` (`DECISIONS.md` 31) | `double` cannot represent `0.1`; summing 10 000 payments drifts and a receipt stops reconciling | Print `0.1 + 0.2` and `new BigDecimal("0.1").add(new BigDecimal("0.2"))` |
| Always the `String` constructor | `new BigDecimal("45.00")`, never `new BigDecimal(45.00)` | `new BigDecimal(45.00)` copies the binary-float error you were trying to avoid | Print both values with `toPlainString()` |
| `compareTo`, not `==`/`equals` | Balance and revenue checks | `equals` compares scale too, so `45.0` and `45.00` are "different" | Show `new BigDecimal("45.0").equals(new BigDecimal("45.00"))` → `false` |
| Division needs a rounding mode | Any per-day or per-period price split | `divide` without a `RoundingMode` throws for non-terminating decimals | Split `100` over `3` days with `RoundingMode.HALF_UP` |
| Dates as ISO-8601 **strings** | `yyyy-MM-dd`, `HH:mm`, `yyyy-MM-dd HH:mm` (`DECISIONS.md` 31, rule 9) | SQLite has no date type, and ISO strings sort chronologically as text — that is what makes `findByDay`, `findExpiring` and `revenueBetween` work with plain comparisons, and `String` is not affected by time zones or locale | Sort `["2025-9-2", "2025-10-01"]` as strings, then as ISO strings, and explain the difference |
| One owner for parsing | `DateUtils` owns every parse and format (rule 9) | Two parse implementations disagree (pattern, locale, leniency) and produce off-by-one-day bugs | Find in `UML.md` §6 every `DateUtils` member and name which rule needs it |
| Month-end arithmetic | `DateUtils.addMonths` for membership periods | `2025-01-31` + 1 month must not become `2025-03-03` | Decide the project's rule for month-end and write it as a note in `DECISIONS.md` |

### 3.8 Architecture-level concepts

| Concept | Evidence | Why we need it | Exercise |
|---|---|---|---|
| Layered architecture | `UML.md` §2 | Each layer changes for one reason: UI look, business rule, storage, or platform helper | Draw the four layers from memory and mark which direction dependencies may point |
| MVC in JavaFX | FXML = View, entity = Model, controller + service = Controller | Designers can edit FXML without touching Java; logic is testable without a window | Name the three files one screen touches |
| Separation of concerns / SRP | DAO = persistence only; service = rules only; controller = rendering only | A change in the tax/price rule must not require touching SQL or FXML | Take one rule (rule 11) and name exactly one method allowed to enforce it |
| Dependency inversion | Services depend on DAO interfaces; DAO impls depend on `DbConnection`; the concrete class is named once at startup | The two volatile things (database, UI toolkit) sit behind stable interfaces | Point at the single place where `new SqliteDbConnection()` may appear (`MainApp`/bootstrap) |
| SOLID, letter by letter | SRP: layers and one-class-per-concern. OCP: a new `PaymentMethod` literal needs no change in `PaymentService.record`. LSP: any `DbConnection` impl works for all 13 DAOs. ISP: 13 narrow DAO interfaces. DIP: interfaces above | This is the vocabulary used in interviews; the project gives a concrete example of each | Write one sentence per letter using a class from this project |
| DRY | `DateUtils` (one parse), `GuardService` (one reference-count query, `DECISIONS.md` 39), `FxUtils` (one dialog helper) | Duplicated logic drifts; duplicated SQL drifts faster | Find the 26 near-identical methods `GuardService` replaced |
| KISS / YAGNI | No ORM, no `AbstractJdbcDao`, no association classes, no `MaintenanceRecord` entity yet | Every abstraction must pay for itself in a 7-week project | Read the rejected candidates in `DECISIONS.md` 36 and `UML.md` "Notes on scope" |
| Threading | All UI on the FX application thread; slow work off it (`Task`) | Touching a control from another thread throws or corrupts the scene graph | Name where a long query would freeze the window if run on the FX thread |
| Single source of truth for docs | `UML.md` holds each diagram once (`DECISIONS.md` 28) | Two copies of a design drift; the copy nobody edits becomes a lie | Explain what drifted last time (the `PlanType` accessors, six enum notes) |

---

## 4. Glossary

| Term | Meaning in this project |
|---|---|
| **Aggregate** | A cluster of data treated as one unit; here mostly one entity plus its rows (`Membership` belongs to one `Customer`). `Equipment` is a root aggregate with no relationships (`DECISIONS.md` 30) |
| **`BigDecimal`** | Java's exact decimal type. All money. Constructed from a `String` |
| **Boxed type** | `Integer`/`Double`/`Boolean` — an object wrapper. In this project a boxed field means the column is nullable (`UML.md` header) |
| **DAO** | Data Access Object: an interface + implementation pair that hides SQL behind methods such as `save`, `findById`, `findAll` |
| **Derived state** | A value computed on read instead of stored (`AvailabilityStatus.FULL`, `…EXPIRED`) — rule 13 |
| **DTO / entity** | A plain data class with fields and accessors. Our 13 `model` classes |
| **FK (foreign key)** | A column holding another table's primary key (`customerId`). We store ids, never object references |
| **FXML** | XML that declares a JavaFX scene; loaded by `FXMLLoader` and bound to a controller |
| **FX thread** | JavaFX's single UI thread. All scene-graph work happens there |
| **JDBC** | Java's standard database API: `Connection`, `PreparedStatement`, `ResultSet` |
| **Looked-up color** | A CSS variable (`-md-sys-color-primary`) resolved at render time so one theme swap recolours everything |
| **PBKDF2** | Password-Based Key Derivation Function 2 — the JDK's slow-hash algorithm we use for password storage, with a per-user salt |
| **Prepared statement** | A pre-compiled parameterised SQL statement. Prevents SQL injection and handles quoting |
| **Salt** | Random bytes stored with a password hash so identical passwords produce different hashes |
| **Seam** | A place where behaviour can be swapped without editing callers — here `DbConnection` (database) and the DAO interfaces (storage) |
| **Service** | A stateless class holding one use case's business rules; the only layer allowed to decide |
| **SQLite** | An embedded, file-based SQL database. No server, one file (`gym.db`) |
| **Toolchain** | Gradle's declaration of which JDK to compile with (`build.gradle` → Java 27) |
| **Vertical slice** | One feature built through every layer (entity → DAO → service → screen). Each teammate owns 2–3 |
| **XOR link** | Exactly one of three nullable payment targets is set — rule 12 |

---

## 5. Cross-layer exercises (do these after the layer exercises)

1. **Trace a click.** "Reception clicks *Check in* for member 12." Write the full path: FXML `onAction`
   → `AttendanceController.onCheckIn(12)` → `AttendanceService.checkIn(...)` → eligibility checks
   (rule 5) → `AttendanceDao.findByCustomerAndDay` → insert or return the existing row → controller
   refreshes the list → `FxUtils.info("Checked in")`. Name every class in order and say which layer
   each belongs to. *Answer:* `controller/README.md` §6 and `service/README.md` (AttendanceService).
2. **Trace a failure.** The database file is locked. Where is `SQLException` first caught, what type
   travels up, and what does the user finally see? *Answer:* caught in `AttendanceDaoImpl`, wrapped in
   `DaoException`, propagates through the service untouched, caught in the controller, shown as a
   friendly sentence via `FxUtils.error(...)`.
3. **Break a rule deliberately.** Set `price = 45` on a session that has a `subscriptionId`
   (rule 11) and explain which method should have refused it (`coveredBySubscription`) and where the
   check belongs (service, before insert).
4. **Add a feature without breaking the design.** "Add a `CASH_ON_DELIVERY` payment method."
   List every file you must touch. *Answer:* the `PaymentMethod` enum, the seed/docs, and the UI list
   that renders method names — **no** DAO change (stored as text), **no** service change. That is the
   Open/Closed Principle in practice.
5. **Change the storage engine.** "Move from SQLite to PostgreSQL." List what changes. *Answer:* a new
   `DbConnection` implementation and the SQL dialect details inside the 13 `*DaoImpl` classes; the
   interface, services, controllers and entities are untouched (`DECISIONS.md` 33).
6. **Find the single writer.** For each of `SessionStatus.NO_SHOW`, `MembershipStatus.EXPIRED`,
   `AvailabilityStatus.FULL`, name the one method allowed to produce it. *Answer:* `markNoShow`; the
   derived expiry check (never written); `isSlotFull`/`bookedCount` (never written).

---

## 6. Forty questions you should be able to answer

**Modelling**
1. Why 13 entities and not one big "Record" table? — Each entity has its own identity, lifecycle and owner; one table would make every rule depend on every column.
2. Why do entities reference other entities by id only? — No object graph means no lazy loading, no JDBC in the model, and no cycles; `UML.md` entity contract.
3. Why does `Payment` need `equals`/`hashCode` specifically? — Rule 12's XOR is defined over payments and tests compare them (`model/README.md`).
4. Why is `Equipment` unconnected to other entities? — It is a root aggregate; maintenance history would need a new entity (`DECISIONS.md` 30).
5. Why boxed `Integer` for `trainerId` but primitive `int` for `id`? — Boxed marks a nullable column; `id` is never null (`UML.md` header).
6. Why is `PlanType` an enum with fields instead of a `switch` on strings? — Price and duration belong to the plan; the compiler then guards every use.

**Layers**
7. What are the four layer rules? — `UML.md` §2.
8. Why can't a controller contain SQL? — Then every screen owns its own query, nothing is reusable, and a schema change touches the UI.
9. Why must services and DAOs stay JavaFX-free? — They must be unit-testable and portable to the Swing branch.
10. What is the only JavaFX-aware utility, and why? — `FxUtils`; dialogs need JavaFX, and it takes plain strings so it never sees `DaoException`.
11. Where is the theme applied? — `MainApp.start` calls `M3Stylesheets.applyTo(scene, theme)` (`DECISIONS.md` 4).

**Data access**
12. Why is `DbConnection` an interface? — The database is the most likely thing to change; one seam protects 26 DAO files (`DECISIONS.md` 33).
13. Why does `DaoException` extend `RuntimeException`? — 13 DAOs would otherwise declare `throws` everywhere, and the UI cannot act on a raw `SQLException` (`DECISIONS.md` 40).
14. Why does `DaoException` declare no `message`/`cause` fields? — It chains through `super(message, cause)`; duplicating the fields breaks `getMessage()`/`getCause()` (`DECISIONS.md` 40).
15. Why is there no `AbstractJdbcDao<T>`? — The impls vary in SQL and entity type, not structure; the base would hide the wiring (`DECISIONS.md` 36).
16. Why does `AttendanceDao.findByCustomerAndDay` return a list? — A duplicate row must be visible, and rule 5's "one row per day" check needs to see it.
17. What stops SQL injection? — Every query uses a `PreparedStatement` with bound parameters; no string concatenation of user input.
18. Why is `countByAvailability` given a `SessionStatus` argument? — Rule 17 counts `CONFIRMED` only, so cancelling frees capacity.

**Services**
19. Why are services stateless? — A screen can create one per action; no request state leaks between users or calls.
20. Why does `SchedulingService.canTransition` exist instead of a check in each method? — Rule 18 needs one authority; scattered checks drift.
21. Why does `PaymentService.validLink` stay public? — So rule 12 is unit-testable without a database (`service/README.md`).
22. Why does `CustomerService.validate` return a list while booking throws? — A form has many fields and must show all errors; a booking is one action that must fail fast.
23. Why is `MembershipService.freezeForSuspended` on the membership side? — Rule 14: a suspended customer implies frozen memberships; the owner of the membership state changes it.
24. Why must a service call `MedicalProfileService.getForTrainer` instead of the DAO? — Rule 15's ownership check belongs to the aggregate that owns medical data; reaching into the DAO bypasses it.
25. Why does `AdminService.adjustPayment` store `previousAmount`? — Rule 19: money is corrected, never deleted, so a receipt stays reconcilable.

**JavaFX/UI**
26. Why is `MainApp` not a screen? — It owns the window, theme, nav shell and overlay layer; screens are controllers (§5).
27. How do FXML and a controller find each other? — `fx:controller` in FXML + `@FXML` fields + `initialize()`; a typo in `fx:id` is a runtime NPE, not a compile error.
28. Why pass `customerId` to `onOpenProfile(int)` instead of sharing the controller? — Explicit parameters keep screens independent; static/shared controllers leak state and break the second window.
29. Why must the FXML file name match the controller name? — Convention: you can find either from the other, and `docs/UML.md` §5 maps them one-to-one.
30. What does `SessionContext.requireRole(StaffRole.ADMIN)` protect? — Every override path, including `SchedulingService.adminReserve` (rule 16).
31. Why is `SessionContext` allowed to be static mutable state? — The signed-in account must be reachable from every controller; it is FX-thread confined and cleared on logout and exit (`DECISIONS.md` 34).

**Correctness and quality**
32. Why `BigDecimal` for money? — `double` drifts; receipts must reconcile (`DECISIONS.md` 31).
33. Why ISO-8601 strings instead of `LocalDate`? — SQLite has no date type and ISO strings sort chronologically as text, which is what makes the day queries work; `DateUtils` owns every parse.
34. Why is `AvailabilityStatus.FULL` never stored? — It is derived from occupancy; a stored flag goes stale after a cancellation (rule 13).
35. Which records are never deleted, and what replaces deletion? — `Attendance` and `Payment` (rule 19): `correctRecord`, void + new payment, `adjustPayment`.
36. What does `GuardService.countReferences` do and why is it not 26 DAO methods? — It counts rows referencing a target across the tables, so an admin hard delete can refuse while references exist (`DECISIONS.md` 39, rule 4).
37. Why does the project have no app test suite, and what replaces it? — Root `README.md` §11: disciplined validation and a manual error-path sweep; the `m3fx` modules carry JUnit suites.
38. How do we keep two branches (JavaFX/Swing) from diverging? — `model`/`dao`/`service`/most of `util` stay toolkit-free and are ported unchanged; only the view layer is rewritten.
39. What would you do differently with more time? — Record it in `DECISIONS.md` as a considered option (e.g. `Optional` returns, a `MaintenanceRecord` entity, per-role permissions).
40. Where do you look before writing any class? — `docs/UML.md` for its exact shape, then that folder's README for the how-to, then `DECISIONS.md` for the why.

---

## 7. Habits that make the code look professional

1. **Write the design first, then the code.** Every class in this project exists in `UML.md` before it
   exists in Java. A PR that adds a class not in the design must update `UML.md` in the same PR.
2. **One reason to change per class.** If you cannot describe a class in one sentence without "and",
   it is doing two jobs.
3. **Name things after the domain.** `expireOverdue`, `freezeForSuspended`, `coveredBySubscription` —
   a rule should be greppable by its name.
4. **Validate at the boundary, fail fast, message in plain language.**
5. **Never lose the cause.** Wrap, chain, log; do not swallow.
6. **Prefer an enum to a `String` constant, a `BigDecimal` to a `double`, and an interface to a
   concrete class at a seam.**
7. **Do not add an abstraction until the second copy exists** (see the rejected hierarchies in
   `DECISIONS.md` 36). Duplication is cheaper than the wrong abstraction.
8. **Review with the folder checklist** before opening the PR, and update the Status column in the
   same PR.
9. **Keep the docs honest.** If the code and `UML.md` disagree, one of them is a bug — fix both.
