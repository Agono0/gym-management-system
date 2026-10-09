# Gym Management System — JavaFX (Material Design 3)

> **Branch:** `main` — JavaFX, primary / modern version.
> Classic version lives on the [`swing`](../../tree/swing) branch.
> Feature development happens on [`javafx`](../../tree/javafx) and merges here.

## 1. Project Idea & Description

A desktop Gym Management System that digitalizes daily gym operations: managing
customers and trainers, selling and tracking memberships, building workout
programs, scheduling sessions, tracking equipment, recording attendance,
processing payments, and following each customer's fitness progress.

This branch is the **primary implementation** using **JavaFX** with
**Material Design 3 (M3)** styling (including M3 expressive motion where the
toolkit supports it). It is the reference version: features land here first
and are then ported to the `swing` branch. Target users are gym receptionists,
trainers, and managers running the app on a Windows/Linux/macOS desktop.

> ⚠️ **AI usage warning:** AI was used to generate the documentation and to
> assist with GUI design.

## 2b. m3fx — reusable Material Design 3 library (this repo builds it)

The `m3fx-core` + `m3fx-controls` Gradle modules are a **separate, reusable M3 library**
(version `0.1.0`, group `io.m3fx`) — usable in any JavaFX project, not just this gym app:

- `m3fx-core`: dynamic color engine (ported material-color-utilities, Apache 2.0 — see `NOTICE`),
  `M3Theme.fromSeed(...)`, all color roles, and shape/type/elevation/state/motion tokens.
- `m3fx-controls`: 25+ `M3*` components (buttons → search, see `docs/COVERAGE.md`) + one
  `m3fx.css` that references only `-md-sys-color-*` theme lookups.
- Use it elsewhere: copy the two module folders (or their jars) into another Gradle project and add
  `implementation(project(':m3fx-core'))` / `implementation(project(':m3fx-controls'))`, then call
  `M3Stylesheets.applyTo(scene, M3Theme.baseline(false))` once. Full docs: `docs/SPEC_NOTES/`.

Run the gallery demo: `gradlew run` (seed picker + light/dark toggle + all components).

## 2. Features

### Customer management (owner: Abdelrhman)
- Register / edit / deactivate customers (personal info, contact, emergency contact).
- Customer profile: personal info, membership details, assigned trainer, workout
  program, upcoming schedule, attendance history, fitness progress.
- Search and filter customers by name, status, membership type.

### Trainer management (owner: Ziad)
- Trainer profiles: personal + professional info, specialization, experience,
  certifications, availability.
- Trainer schedule view; assign / reassign customers to trainers.
- Workout programs created per customer, visible on both profiles.

### Membership management (owner: Ziad)
- Membership plans (monthly / quarterly / yearly) with pricing and benefits.
- Sell, renew, freeze, and cancel memberships; expiry reminders.
- Membership status badges on customer profiles.

### Workout programs (owner: Yousef)
- Build programs from exercise templates (sets, reps, rest, notes).
- Assign programs to customers; version history per customer.
- Trainer attribution: which trainer created each program.

### Scheduling (owner: Yousef)
- Book training sessions (customer + trainer + time slot).
- Future workouts list on the customer profile; conflict detection for trainers.
- Day / week agenda views.

### Equipment management (owner: Yousef)
- Equipment inventory (name, category, purchase date, condition).
- Status tracking: available / in maintenance / retired.
- Maintenance log.

### Attendance tracking (owner: Abdel Raouf)
- Check-in / check-out recording (manual + optional QR/barcode later).
- Attendance history per customer; daily attendance report.

### Payments (owner: Abdel Raouf)
- Record membership and service payments; receipts.
- Outstanding balances; simple revenue summary.

### Progress tracking & dashboard (owner: Abdel Raouf)
- Log body metrics (weight, body fat, measurements) and strength benchmarks.
- Progress charts per customer; gym-wide dashboard (active members,
  today's sessions, revenue snapshot).

## 3. Tech Stack

| Layer      | Technology |
|------------|------------|
| Language   | Java 27 (OpenJDK 27) |
| UI         | JavaFX 21+ (FXML + CSS) |
| Design     | Material Design 3 via MaterialFX (components) / AtlantaFX (theme); M3 expressive motion where supported |
| Build      | Gradle 8+ (application plugin + JavaFX Gradle plugin) |
| Database   | SQLite (file `gym.db`, via SQLite-JDBC); schema in `src/main/resources/db/` (Week 1) |
| Error handling | input validation + `DaoException` → user-friendly messages (no stack traces to users) |
| IDE        | IntelliJ IDEA (project SDK: `openjdk-27`, see `.idea/misc.xml`) |

## 4. Project Structure / Files Map

```
(build.gradle, settings.gradle — Gradle build)
src/main/java/com/gym/
  MainApp.java            application entry point (to be written)
  model/      entity POJOs: Customer, Trainer, Membership, WorkoutProgram,
              Session, Equipment, Attendance, Payment, ProgressRecord → README
  dao/        DAO interfaces + SQLite implementations (one per entity) → README
  service/    business logic + validation per feature → README
  controller/ JavaFX FXML controllers, one per screen → README
  util/       DbConnection, validators, date/time + FX helpers → README
src/main/resources/
  fxml/       FXML layouts (one per screen, mirrors controller/) 
  theme/      material-theme.css (M3 tokens) + animations
  db/         schema.sql (created Week 1) + seed data for demo
  images/     logo, icons, placeholders
```

Each folder above contains its own `README.md` with purpose, contents,
task list, owner, and status. **This skeleton is docs-only: packages contain
no `.java` files yet — the team writes them following the timeline below.**

## 5. Installation & Running

Requirements: JDK 27, Gradle 8+ (or the wrapper once added in Week 1).

```bash
git clone https://github.com/Agono0/gym-management-system.git
cd gym-management-system
git checkout main
gradle run            # once MainApp exists (Week 1+)
```

The SQLite file (`gym.db`) is created automatically on first run and is
git-ignored — never commit it.

## 6. Timeline (7 weeks)

| Week | Focus | Who |
|------|-------|-----|
| 1 | Foundation (branches, Gradle runs, SQLite schema v1, `DbConnection`, DAO base, M3 theme tokens, docs) | All (lead: Abdelrhman) |
| 2 | Customer CRUD + profiles (Abdelrhman) · Trainer CRUD + availability (Ziad) | Abdelrhman, Ziad |
| 3 | Memberships: plans, sell/renew/freeze (Ziad) · Workout program builder (Yousef) | Ziad, Yousef |
| 4 | Scheduling + conflict detection (Yousef) · Equipment + maintenance log (Yousef) · Attendance check-in/out (Abdel Raouf) | Yousef, Abdel Raouf |
| 5 | Payments + receipts (Abdel Raouf) · Progress tracking + charts (Abdel Raouf) · start Swing port of own modules | All |
| 6 | Integration, error-handling review, bugfix, UI polish, demo seed data | All |
| 7 | Buffer: docs finalization, presentation, release | All |

Rule: **JavaFX first, then port your own modules to `swing`.**

## 7. Team Task Distribution

| Area | Owner | Branch(es) |
|------|-------|------------|
| Foundation: Gradle, SQLite schema, `DbConnection`, M3 theme baseline | Abdelrhman | both |
| Customer management + profiles | Abdelrhman | both |
| Trainer management + assignment | Ziad | both |
| Membership management | Ziad | both |
| Workout programs | Yousef | both |
| Scheduling | Yousef | both |
| Equipment management | Yousef | both |
| Attendance tracking | Abdel Raouf | both |
| Payments | Abdel Raouf | both |
| Progress tracking + dashboard | Abdel Raouf | both |
| Error handling in own modules, reviews, demo | All | both |

Workload is balanced by vertical slices: each member owns 2–3 features
end-to-end (model → DAO → service → UI) in both branches.

## 8. Contribution Guidelines

1. Branch per task: `feat/<module>-<short-desc>` off `main` (or `swing` for ports).
2. One PR per feature with a screenshot/GIF of the UI change.
3. The other branch's port is a separate PR — never mix JavaFX and Swing code.
4. Run `gradle build` before pushing; validate all inputs and handle every failure (DB errors, bad input) with a user-friendly message — never show stack traces to users.
5. Follow the folder READMEs: update the Status column (`To Do` → `In Progress` → `Done`) when you start/finish a task.
6. M3 styling is mandatory: use theme tokens from `theme/material-theme.css`, no hardcoded colors.

## 9. Known Limitations / Future Improvements

- SQLite = single-machine only; multi-branch gyms need a client-server DB later.
- Offline QR/barcode check-in and payment-gateway integration are out of scope (manual entry only).
- True M3 expressive animations depend on library support; complex motion may be simplified.
- No role-based login yet (single operator mode) — planned improvement.
- Future: reports export (PDF), dark/light theme toggle, reminder notifications.

## 10. Docs & Folders

Folder-level docs: [`model`](src/main/java/com/gym/model/README.md) ·
[`dao`](src/main/java/com/gym/dao/README.md) ·
[`service`](src/main/java/com/gym/service/README.md) ·
[`controller`](src/main/java/com/gym/controller/README.md) ·
[`util`](src/main/java/com/gym/util/README.md) ·
[`resources`](src/main/resources/README.md)

## 11. Error-Handling Standard (no test suite — handle errors instead)

This project ships without automated tests. Quality comes from disciplined
error handling:

- Validate every user input at the service layer (`Validators`); reject bad
  data with a clear field-level message before it reaches the DAO.
- DAOs wrap `SQLException` in `DaoException`; services translate it into a
  plain-language message for the UI (e.g. "Could not save customer — check
  the database file and try again.").
- Controllers show friendly dialogs (see `util/FxUtils`); stack traces go to
  the log/console only, never to the user.
- Week 6 includes a full error-handling review: each owner walks every
  screen of their modules and triggers each failure path manually.
