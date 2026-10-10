# `util` — Shared Helpers (JavaFX branch)

Canonical design: `docs/UML.md` §6. This file tracks ownership and responsibilities — it does not
restate method signatures.

## Purpose

Cross-cutting helpers used by all layers: database connection management, exception wrapping, input
validation, date/time handling, session state, and small JavaFX utilities.

## Rules

- `DbConnection` is an **interface**. DAO impls depend on it; only `MainApp` (or a bootstrap step)
  names the concrete `SqliteDbConnection`. This is the one seam that lets the persistence layer be
  swapped without touching 26 files.
- **Every class here except `FxUtils` must stay free of JavaFX imports**, so the `swing` branch
  reuses them as-is. `FxUtils` is the deliberate exception and only controllers may depend on it.
- `SessionContext` holds the signed-in account. It is static mutable state — the single documented
  exception to the no-static-state rule — so it must stay FX-thread confined and be cleared on
  logout and on application exit.
- Utility classes are stateless: their members are `static` and their constructors are private.
  They are never instantiated.
- `DateUtils` owns **every** date parse and format in the project. ISO-8601 strings
  (`yyyy-MM-dd`, `HH:mm`) sort chronologically as strings, which is what makes `findByDay`,
  `findExpiring` and `revenueBetween` work without a date type.
- `ChartUtils` does formatting only — it takes values that a service already fetched. It must not
  reach into a DAO.

## Contents (to be written)

| Class | Responsibility | Owner |
|-------|----------------|-------|
| `DbConnection.java` | Interface: `getConnection()`, `initializeSchema()` | Abdelrhman |
| `SqliteDbConnection.java` | SQLite implementation; `gym.db` bootstrap, schema init | Abdelrhman |
| `Validators.java` | Phone/email/number/date validation. Returns primitive `boolean`, never `Boolean`. | Abdelrhman |
| `SessionContext.java` | Holds the logged-in `Administrator` for role checks; `set`/`get`/`requireRole`/`clear` | Abdelrhman |
| `PasswordUtils.java` | PBKDF2 password hashing and verification (Java built-in, no new deps) | Abdelrhman |
| `FxUtils.java` | Dialogs, alerts, scene switching, M3 snackbar/toast helper. The only JavaFX-aware utility. | Abdelrhman |
| `DateUtils.java` | Membership periods, session slot math, overlap tests, formatting | Yousef |
| `ChartUtils.java` | Progress-chart and revenue-series dataset formatting | Abdel Raouf |
| `DaoException.java` | Unchecked `SQLException` wrapper; thrown by DAOs, rendered by `FxUtils` | Abdelrhman |

## Tasks

| Task | Owner | Status |
|------|-------|--------|
| Skeleton + this README created | Abdelrhman | Done |
| `DbConnection` + `SqliteDbConnection` + schema init (Week 1, foundation) | Abdelrhman | To Do |
| `DaoException`, `Validators`, `FxUtils`, `SessionContext`, `PasswordUtils` | Abdelrhman | To Do |
| `DateUtils` | Yousef | To Do |
| `ChartUtils` | Abdel Raouf | To Do |

`DbConnection`, `SqliteDbConnection`, `DaoException`, `Validators`, `DateUtils`, `PasswordUtils` and
`ChartUtils` must stay UI-toolkit-free so the `swing` branch reuses them as-is. That branch has no
Java files yet.
