# `util` — Shared Helpers (JavaFX branch)

## Purpose
Cross-cutting helpers used by all layers: database connection management,
input validation, date/time handling, and small JavaFX utilities.

## Contents (to be written)
| Class | Responsibility | Owner |
|-------|----------------|-------|
| `DbConnection.java` | SQLite connection factory, `gym.db` bootstrap, schema init | Abdelrhman |
| `Validators.java` | Phone/email/number/date validation | Abdelrhman |
| `SessionContext.java` | Holds the logged-in `Administrator` for role checks; set/get/clear | Abdelrhman |
| `PasswordUtils.java` | PBKDF2 password hashing and verification (Java built-in, no new deps) | Abdelrhman |
| `DateUtils.java` | Membership periods, session slot math, formatting | Yousef |
| `FxUtils.java` | Dialogs, alerts, scene switching, M3 snackbar/toast helper | Abdelrhman |
| `ChartUtils.java` | Progress-chart dataset builders | Abdel Raouf |

## Tasks
| Task | Owner | Status |
|------|-------|--------|
| Skeleton + this README created | Abdelrhman | Done |
| `DbConnection` + schema init (Week 1, foundation) | Abdelrhman | To Do |
| `Validators`, `FxUtils`, `SessionContext`, `PasswordUtils` | Abdelrhman | To Do |
| `DateUtils` | Yousef | To Do |
| `ChartUtils` | Abdel Raouf | To Do |

`DbConnection`, `Validators`, `DateUtils`, `PasswordUtils` should stay
UI-toolkit-free so the `swing` branch reuses them as-is.
