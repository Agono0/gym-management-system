# `dao` — Data Access Layer (JavaFX branch)

> ## ⚠️ PLANNED, NOT BUILT
> **This folder contains no `.java` files — only this README.** The 13 interface/impl pairs below
> are the agreed design in [`docs/UML.md`](../../../../docs/UML.md) §3 ("§3 is the full DAO
> surface"), written here so the layer can be built straight from the documentation. There is no
> SQLite dependency in `build.gradle` today and no `schema.sql` yet — see
> [`src/main/resources/db/README.md`](../../../resources/db/README.md) (also planned). Status:
> [`docs/COVERAGE.md`](../../../../docs/COVERAGE.md). Do not cite a DAO as if it compiles.

## 1. Purpose — the problem this layer solves

This layer will be the **only** place in the application where SQL is written: 13 interfaces
(`CustomerDao`, `TrainerDao`, …) and 13 implementations (`CustomerDaoImpl`, …), one pair per entity.
Each interface is a small contract in domain words — `save`, `findById`, `findByCustomerAndDay`,
`revenueBetween` — and each implementation is the JDBC code that keeps that promise against one
SQLite file, `gym.db`. A DAO **maps rows to objects** and decides nothing about the gym.

**Why the seam matters.** Put a customer-list `SELECT` inside `CustomerListController` and four
things break at once: table names spread across 19 screens; no query is reused; nothing is testable
without a `Stage`; and concatenated SQL strings invite injection and wrong-row bugs. The DAO layer
exists so the UI asks a question in domain words and one file answers it. (Today's demo screens hold
their data in the controllers — this layer is what replaces that.)

**What this layer deliberately does not do.** No business rules (no validation, no state
transitions, no pricing — that is `service/`). No JavaFX — not one import, which is also what lets
the `swing` branch reuse every file unchanged. No user-facing messages — a DAO says which operation
failed and carries the technical cause; the controller formats it. No caching or lazy loading; no
`begin`/`commit` — one statement per method (a two-statement atomic operation is a `UML.md` §3
change, not a loop). `findById`/`findAll`/`delete` exist on almost every interface even where no
screen calls them yet, so a future screen does not reopen the interface (`UML.md` §3, "Notes on
scope").

## 2. What you learn here (OOP concepts)

| Concept | Where it will be practised | Why the project needs it | How to apply it (one line) |
|---|---|---|---|
| Abstraction / interface as a contract | the 13 `XxxDao` interfaces | A service says *what* it needs stored without knowing *how* | Declare the method on the interface; write SQL only in the impl |
| Programming to an interface | `CustomerDao dao = new CustomerDaoImpl(conn);` | The database can be replaced or faked without touching 13 services (`DECISIONS.md` 33) | Type fields as `XxxDao`; name the impl once, at the composition root |
| Runtime polymorphism | every `XxxDaoImpl implements XxxDao` | One service works with SQLite today and a fake tomorrow | Never branch on `instanceof` |
| Encapsulation | SQL text, `DbConnection` field and `mapRow` all `private` | A leaked `ResultSet` keeps a cursor open; leaked SQL gets edited twice | Expose entities and `List`s only |
| Single responsibility | one impl per entity | A change to `payments` touches exactly one file | If a method needs a second table, ask whether it belongs elsewhere |
| Dependency injection by constructor | `public CustomerDaoImpl(DbConnection db)`, `private final` | The DAO does not choose its database | Take collaborators as constructor parameters |
| Composition over inheritance | 13 impls that each *hold* a `DbConnection` | No `AbstractJdbcDao<T>` (`DECISIONS.md` 36): the shared part is already the seam | Keep the five-CRUD repetition readable per file |
| Generics, used sparingly | `List<Customer>` returns | Type-safe with no casts; `T findById(int)` would still need a per-entity mapper, so a generic base saves nothing | Return `List<Entity>`; never a raw list, never a live `ResultSet` |
| Enums as a type system | `status.name()` bound, `valueOf` read | A new literal needs no DAO change | Never store `ordinal()` |
| Checked vs unchecked exceptions | `SQLException` checked; `DaoException` unchecked (`DECISIONS.md` 40) | A checked `SQLException` would poison every JavaFX handler signature | Catch the checked one at the boundary; throw the unchecked one |
| Exception translation + chaining | `throw new DaoException("…", e)` | The log needs the technical cause; the UI catches one type | Pass the `SQLException` as the cause |

## 3. Design patterns in this layer

| Pattern | Where | Why | What goes wrong if used carelessly |
|---|---|---|---|
| **DAO (Data Access Object)** | the 13 pairs | One class per entity hides one table and gives services domain words | A DAO that validates or prices becomes a second service |
| **Facade over JDBC** | `Connection` → `PreparedStatement` → `ResultSet` → entity | Callers ask for a `Customer`, not for rows and cursors | Leaking a JDBC type hands the caller a resource to close |
| **Dependency injection / Strategy at the `DbConnection` seam** | the composition root names `SqliteDbConnection` once | "Which database" is a strategy the impls never see | A DAO that builds its own connection is a second source of truth |
| **Row Mapper** | one `private Xxx mapRow(ResultSet)` per impl | A new column is one edit in one place | Inline mapping in five methods = five chances to misread a column |
| **Template Method — rejected** | no `AbstractJdbcDao<T>` (`DECISIONS.md` 36) | A base class would hide the SQL that differs behind a superclass call | The 13 explicit `..> DbConnection` edges stop being checkable |
| **Singleton — avoided** | no `DbProvider.get()` | A global makes every DAO untestable without a real file | "It is only one connection" |
| **Repository — not claimed** | the name is DAO on purpose | The contracts are table-shaped CRUD, not aggregates | Calling it a repository sets an expectation it does not meet |

## 4. Rules you must follow (and the bug each rule prevents)

1. **SQL lives here and nowhere else.** A screen or service holding SQL ends the single-place-to-fix
   guarantee.
2. **Every statement is a `PreparedStatement` with bound parameters.** A concatenated search box is
   how one member's query returns another member's rows.
3. **`DaoException` is the only exception that crosses this boundary**, with the original
   `SQLException` chained as the cause.
4. **Return empty lists, never `null`, never a live `ResultSet`.** "None found" is a normal answer.
5. **Resources are closed with try-with-resources** — a leaked connection locks `gym.db`
   (`SQLITE_BUSY`).
6. **One statement per method; no transactions, no cascades.** Hard delete stays admin-only behind
   `GuardService.countReferences` (rule 4) because the schema uses no `ON DELETE CASCADE`.
7. **Bind enums as `name()`**, read with `valueOf`.
8. **No JavaFX import, no static mutable state, no business rule.** The `swing` branch reuses these
   files only if this holds.

## 5. How this layer connects to the others

| Will depend on | Will be used by |
|---|---|
| `model/` (planned) entities; `util/` (planned) `DbConnection` + `DaoException`; `java.sql` only | `service/` (planned) — the only caller of DAO methods; the composition root (`GymApp`/the shell bootstrap) names the impls once |

When the backend starts: write `schema.sql` first (`db/README.md`), then one DAO pair per entity in
`docs/UML.md` §3 order, each interface exactly as §3 declares it. The demo controllers' data paths
are the acceptance tests' specification for what each screen needs (`src/test/java/com/gym/fxml/`
pins the screens' behaviour).

## 6. Learning exercises

1. Explain to a teammate why the field type is `CustomerDao` but the `new` is `CustomerDaoImpl` —
   and where the one `new` is allowed to appear.
2. A reviewer sees `stmt.executeUpdate("… WHERE name = '" + input + "'")`. State the two bugs and the
   one-line fix.
3. Why does `DaoException` extend `RuntimeException` instead of `Exception`? What would break in
   JavaFX event handlers otherwise?
4. Propose `AbstractJdbcDao<T>` and then argue against it using `DECISIONS.md` 36 — what does the
   base class hide?
5. A `delete` must not orphan referenced rows. Which rule and which planned helper enforce it, and
   why is there no `ON DELETE CASCADE` in the schema?

## 7. Ownership

| Planned file group | Owner |
|---|---|
| 13 `XxxDao` / `XxxDaoImpl` pairs (per `docs/UML_TEAM.md` split) | per-feature owners (see `docs/UML_TEAM.md`) |

Status: **planned, not built** — track in `docs/COVERAGE.md` → "Backend domain services and SQLite DAOs".
