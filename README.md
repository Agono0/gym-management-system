# Gym Management System — Swing (Material Design 3)

> **Branch:** `swing` — classic version.
> Primary / modern version lives on [`main`](../../tree/main).
> Feature work lands on `main` first and is ported here.

## 1. Project Idea & Description

A desktop Gym Management System that digitalizes daily gym operations: managing
customers and trainers, selling and tracking memberships, building workout
programs, scheduling sessions, tracking equipment, recording attendance,
processing payments, and following each customer's fitness progress.

This branch is the **classic implementation** using **Java Swing** with
**Material Design 3–inspired** styling. Features are designed and built first
on the `main` branch and then **ported here screen by screen** by the same
feature owner. Target users are gym receptionists, trainers, and managers on
standard desktops where Swing's zero-dependency footprint is an advantage.

> ⚠️ **AI usage warning:** AI was used to generate the documentation and to
> assist with GUI design.

## 2. Features

Mirrors the `main` branch scope (same behavior, Swing rendering):

### Customer management (owner: Abdelrhman)
- Register / edit / deactivate customers; full customer profile (personal info,
  membership, trainer, program, upcoming schedule, attendance, progress).

### Trainer management (owner: Ziad)
- Trainer profiles (info, specialization, experience, availability, schedule,
  assigned customers, programs created).

### Membership management (owner: Ziad)
- Plans, sell / renew / freeze / cancel, expiry reminders.

### Workout programs (owner: Yousef)
- Program builder (exercises, sets/reps/rest), assignment, per-customer history.

### Scheduling (owner: Yousef)
- Session booking with trainer conflict detection; day/week agenda.

### Equipment management (owner: Yousef)
- Inventory + status (available / in maintenance / retired) + maintenance log.

### Attendance tracking (owner: Abdel Raouf)
- Check-in/out + history + daily report.

### Payments (owner: Abdel Raouf)
- Payment recording, receipts, balances, revenue summary.

### Progress tracking & dashboard (owner: Abdel Raouf)
- Body metrics, benchmarks, charts, gym-wide dashboard.

## 3. Tech Stack

| Layer      | Technology |
|------------|------------|
| Language   | Java 27 (OpenJDK 27) |
| UI         | Java Swing (JFrame/JPanel, GroupLayout/MigLayout-style layouts) |
| Design     | Material Design 3–inspired via **FlatLaf** + custom M3 theme properties; expressive animations simplified (see limitations) |
| Build      | Gradle 8+ (application plugin) |
| Database   | SQLite (file `gym.db`, via SQLite-JDBC); schema shared with `main` branch (`resources/db/schema.sql` Week 1) |
| Error handling | input validation + `DaoException` → user-friendly messages (no stack traces to users) |
| IDE        | IntelliJ IDEA (project SDK: `openjdk-27`, see `.idea/misc.xml`) |

Porting rule: `model`, `dao`, `service`, and toolkit-free `util` classes are
copied from `main` with minimal changes; only `view/` + theme are rewritten.

## 4. Project Structure / Files Map

```
(build.gradle, settings.gradle — Gradle build)
src/main/java/com/gym/
  MainApp.java            application entry point (to be written)
  model/      entity POJOs — copied from javafx, identical → README
  dao/        DAO interfaces + SQLite impls — ported from javafx → README
  service/    business logic — ported from javafx → README
  view/       Swing frames/panels/dialogs, one per screen → README
              (DashboardFrame, CustomerPanel, TrainerPanel, MembershipPanel,
               WorkoutPanel, SchedulePanel, EquipmentPanel, AttendancePanel,
               PaymentPanel, ProgressPanel)
  util/       DbConnection, validators, date/time + Swing helpers → README
src/main/resources/
  theme/      material-swing.properties (FlatLaf M3 tokens) + custom UI defaults
  db/         schema.sql (shared design with javafx, created Week 1)
  images/     logo, icons, placeholders
```

Each folder above contains its own `README.md` with purpose, contents,
task list, owner, and status. **This skeleton is docs-only: packages contain
no `.java` files yet — code is ported here after it lands on `main`.**

## 5. Installation & Running

Requirements: JDK 27, Gradle 8+ (or the wrapper once added in Week 1).

```bash
git clone https://github.com/Agono0/gym-management-system.git
cd gym-management-system
git checkout swing
gradle run            # once MainApp exists
```

The SQLite file (`gym.db`) is created automatically on first run and is
git-ignored — never commit it.

## 6. Timeline (7 weeks)

| Week | Focus | Who |
|------|-------|-----|
| 1 | Foundation with `main` (schema, DAO base, theme tokens); set up Swing shell + FlatLaf theme | All (lead: Abdelrhman) |
| 2 | Port Customer screens (Abdelrhman) · port Trainer screens (Ziad) | Abdelrhman, Ziad |
| 3 | Port Membership screens (Ziad) · port Workout screens (Yousef) | Ziad, Yousef |
| 4 | Port Scheduling + Equipment (Yousef) · port Attendance (Abdel Raouf) | Yousef, Abdel Raouf |
| 5 | Port Payments + Progress + Dashboard (Abdel Raouf) · port backlog catch-up | All |
| 6 | Integration, error-handling review, bugfix, UI polish, demo seed data | All |
| 7 | Buffer: docs finalization, presentation, release | All |

Rule: **never build a Swing screen before its counterpart on `main` is merged —
port, don't design twice.**

## 7. Team Task Distribution

| Area | Owner | Notes |
|------|-------|-------|
| Foundation + Customer screens | Abdelrhman | Shell, FlatLaf theme baseline, customer port |
| Trainer + Membership screens | Ziad | Ports of own JavaFX features |
| Workout + Scheduling + Equipment screens | Yousef | Ports of own JavaFX features |
| Attendance + Payments + Progress + Dashboard | Abdel Raouf | Ports of own JavaFX features |
| `model`/`dao`/`service` reuse | Each owner | Copy own modules from `main` |
| Error handling in own modules, reviews, demo | All | — |

## 8. Contribution Guidelines

1. Branch per task: `feat/swing-<module>-<short-desc>` off `swing`.
2. One PR per ported screen with a screenshot; link the original `main` PR.
3. Keep `model`/`dao`/`service` identical to `main` — behavioral differences are bugs.
4. Run `gradle build` before pushing; validate all inputs and handle every failure (DB errors, bad input) with a user-friendly message — never show stack traces to users.
5. Update the folder README Status column (`To Do` → `In Progress` → `Done`).
6. M3 styling is mandatory: FlatLaf theme properties only, no hardcoded colors;
   keep the same visual language as the JavaFX screens.

## 9. Known Limitations / Future Improvements

- Swing cannot fully reproduce M3 expressive motion — animations are simplified
  to fades/highlights; documented per screen.
- Same data-layer limits as `main`: single-machine SQLite, manual payments,
  single operator mode (see `main` README §9).
- FlatLaf theming is close to M3 but not pixel-identical to the JavaFX version.
- Future: feature parity checklist per release.

## 10. Docs & Folders

Folder-level docs: [`model`](src/main/java/com/gym/model/README.md) ·
[`dao`](src/main/java/com/gym/dao/README.md) ·
[`service`](src/main/java/com/gym/service/README.md) ·
[`view`](src/main/java/com/gym/view/README.md) ·
[`util`](src/main/java/com/gym/util/README.md) ·
[`resources`](src/main/resources/README.md)

## 11. Error-Handling Standard (no test suite — handle errors instead)

This branch ships without automated tests. Quality comes from disciplined
error handling, mirroring `main`:

- Validate every user input at the service layer (`Validators`) before it
  reaches the DAO; reject bad data with a clear field-level message.
- DAOs wrap `SQLException` in `DaoException`; services translate it into a
  plain-language message for the UI.
- Panels show friendly dialogs (see `util/SwingUtils`); stack traces go to
  the log/console only, never to the user.
- Week 6 includes a full error-handling review: each owner walks every
  panel of their modules and triggers each failure path manually.
