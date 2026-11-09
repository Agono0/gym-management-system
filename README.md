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

> **Honest scope (read this first).** The project sits at **Milestone 2** of a three-milestone track:
> - **UI demo (`com.gym`, `com.gym.controller`):** All 19 screens + the shell (`GymApp` → `AppShellController`
>   and the `Screen` enum implementing `ScreenController`) are built in pure Java and run on demo data +
>   demo sign-in (`SHOW_LAUNCH_GATES`). Legacy `.fxml` files in `src/main/resources/fxml/` are retained as
>   reference artifacts only (`javafx.fxml` is not part of the build).
> - **Persistence backend (shipped, COMP 301 Lectures 1–6 beginner scope per `docs/DECISIONS.md` 77):**
>   `com.gym.model` (13 `Serializable` entities + 11 enums), `com.gym.dao` (13 concrete SQLite DAOs using
>   `PreparedStatement` + `try-with-resources`), `com.gym.util` (`DatabaseConnection` with embedded
>   13-table DDL + `SerializationUtil` binary object I/O), and `com.gym.exception` (`GymException` →
>   `ValidationException` / `DatabaseException`).
> - **Simplified architecture & next step:** The separate 11-service layer (`com.gym.service`), 13 DAO
>   interfaces, and app test files under `src/test/` were intentionally retired/removed (`docs/DECISIONS.md` 77).
>   **Milestone 3** connects the 20 UI controllers directly to `com.gym.dao.*` and `DatabaseConnection`.
>   [`docs/COVERAGE.md`](docs/COVERAGE.md) tracks everything in detail.

> ⚠️ **AI usage warning:** AI was used to generate documentation and assist with GUI design.

## 2b. m3fx — reusable Material Design 3 library (this repo builds it)

The `m3fx-core` + `m3fx-controls` Gradle modules are a **separate, reusable M3 library**
(version `0.1.0`, group `io.m3fx`) — usable in any JavaFX project, not just this gym app:

- `m3fx-core`: dynamic color engine (ported material-color-utilities, Apache 2.0 — see `LICENSE` and `NOTICE`),
  `M3Theme.fromSeed(...)`, all color roles, and shape/type/elevation/state/motion tokens.
- `m3fx-controls`: 25+ `M3*` components (buttons → search, see `docs/COVERAGE.md`) + one
  `m3fx.css` that references only `-md-sys-color-*` theme lookups.
- Use it elsewhere: copy the two module folders (or their jars) into another Gradle project and add
  `implementation(project(':m3fx-core'))` / `implementation(project(':m3fx-controls'))`, then call
  `M3Stylesheets.applyTo(scene, M3Theme.baseline(false))` once. Full docs: `docs/SPEC_NOTES/`.

Run the component showcase: `.\gradlew.bat run` (POSIX: `./gradlew run`) — `MainApp` builds the
gallery in code (seed picker, light/dark, spring playground, 3D/effects pages, plus a **"Gym App
Screens (19 Java-Built)"** viewer that instantiates all 19 `Screen` enum entries directly via
`Screen.create().getRoot()`). `src/main/resources/fxml/main.fxml` is a legacy declaration artifact;
no code loads it at runtime.

## 2. Features

These are the product's feature areas. Each has a matching Java-built screen in the demo today and its
matching entity + SQLite DAO already built in `com.gym.model` and `com.gym.dao`. Wiring the UI controls
to live queries and enforcing the 19 business rules in [`docs/UML.md`](docs/UML.md) happens in **Milestone 3**.

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

### Login & administration (owner: Abdelrhman)
- Role-based login (ADMIN / MANAGER / STAFF) with PBKDF2-hashed passwords.
- Admin overrides that bypass ownership checks: reassign trainer, adjust
  payment, void records; staff management and password reset.

### Medical profiles (owner: Abdelrhman)
- One medical profile per customer (blood type, conditions, allergies,
  medications, injuries, doctor contact).
- Editable on the customer profile; read-only view for the assigned trainer.

### Trainer subscriptions & availability (owner: Yousef)
- Customers subscribe to a trainer for a period at a flat trainer-set rate
  (distinct from a gym-access membership).
- Trainers publish bookable days, hourly slots and daily customer caps;
  booking checks slot openness, caps and trainer conflicts.

## 3. Tech Stack

| Layer      | Technology |
|------------|------------|
| Language   | Java 21 bytecode (`options.release = 21` in `build.gradle`, compiled on OpenJDK 27); strict COMP 301 Lectures 1–6 beginner backend style |
| UI         | JavaFX 21.0.4 (`javafx.controls` — 19 pure Java screens via `Screen`/`ScreenController`; legacy `.fxml` files are archived only) |
| Design     | Material Design 3 via the in-repo **m3fx** library (`m3fx-core` + `m3fx-controls`, see §2b); M3 expressive motion where supported |
| Build      | Gradle 9.8.0 via the committed wrapper (`gradlew`/`gradlew.bat` — never install Gradle by hand; see [`docs/BUILD_AND_RUN.md`](docs/BUILD_AND_RUN.md)) + `application` and JavaFX plugins |
| Database & Binary I/O | **SQLite** via `org.xerial:sqlite-jdbc:3.45.1.0` (`DatabaseConnection.initializeDatabase()` creates all 13 tables in runtime `gym.db`) + `SerializationUtil` (`ObjectOutputStream` / `ObjectInputStream`). UI controllers still use demo data until Milestone 3 wiring |
| Error handling | Backend checked hierarchy `GymException` → `ValidationException` / `DatabaseException` (`com.gym.exception`); demo screens use inline field errors, snackbars and dialogs (`README` §11) |
| Testing    | JUnit 5 suites inside `:m3fx-core` and `:m3fx-controls` (`./gradlew test`); the root app has **zero** test files under `src/test/` per `docs/DECISIONS.md` 77 (`:test` reports `NO-SOURCE`) |
| IDE        | IntelliJ IDEA (project SDK: `openjdk-27`, see `.idea/misc.xml`) |

## 4. Project Structure / Files Map

```
(build.gradle, settings.gradle — Gradle build)
src/main/java/com/gym/
  MainApp.java       m3fx gallery entry point (interactive component showcase + Screen viewer)
  GymApp.java        app entry point: Java-built AppShellController, freeze watchdog, launch gates
  M3Samples.java     convenience launcher alias for the gallery (extends MainApp)
  controller/        Screen enum, ScreenController interface, AppShellController, LaunchGateController,
                     and 18 Java-built screen controllers (demo data today; Milestone 3 connects DAOs)
  model/             shipped — 13 Serializable domain entities + 11 enums (docs/UML.md §1, §1b)
  dao/               shipped — 13 concrete SQLite DAOs with Decision 78 validation guards (docs/UML.md §3)
  cli/               shipped — COMP 301 Terminal Workflow Console + 22-scenario TA suite (`GymCliApp`)
  util/              shipped — DatabaseConnection (13-table DDL) + SerializationUtil (binary I/O)
  exception/         shipped — GymException, ValidationException, DatabaseException
  service/           retired per docs/DECISIONS.md 77 (README only — explains removal)
src/main/resources/
  fxml/              20 legacy FXML files retained as reference artifacts only (unused at runtime)
  theme/             theme applied in code — `M3Stylesheets.applyTo(scene, M3Theme)` (no CSS file)
  db/                `.gitkeep` + README only (the 13-table SQL DDL lives in `DatabaseConnection`)
  images/            logo/placeholder conventions + glass textures (`images/glass/*`)
m3fx-core/           reusable M3 color/token library + JUnit 5 tests (`m3fx-core/src/test/`)
m3fx-controls/       reusable M3 JavaFX controls + JUnit 5 tests (`m3fx-controls/src/test/`)
```

Each folder above contains its own `README.md` matching [`docs/DOCS_STANDARD.md`](docs/DOCS_STANDARD.md).
Backend folders (`model/`, `dao/`, `util/`, `exception/`) reflect the shipped Milestone 2 code;
`service/` documents why the 11-service tier was retired (`docs/DECISIONS.md` 77); and `controller/`
describes the pure-Java screens waiting for Milestone 3 DAO wiring. Start from the
[docs index](docs/README.md) or the [learning guide](docs/LEARNING_GUIDE.md);
[`docs/COVERAGE.md`](docs/COVERAGE.md) tracks what is built.

## 5. Installation & Running

Requirements: **JDK 27** and the **committed Gradle wrapper** (Gradle 9.8.0 — do not install Gradle by
hand). On Windows use `.\gradlew.bat`; the `.\` is required in PowerShell. Full walkthrough, IntelliJ
setup and troubleshooting: [`docs/BUILD_AND_RUN.md`](docs/BUILD_AND_RUN.md).

```bash
git clone https://github.com/Agono0/gym-management-system.git
cd gym-management-system
git checkout main
.\gradlew.bat build                        # POSIX: ./gradlew build  (compiles all modules + runs m3fx JUnit suites)
.\gradlew.bat runCli -q --console=plain    # opens interactive CMD workflow + [T] TA suite (or run-cli.bat)
.\gradlew.bat run                          # opens the m3fx component gallery (MainApp)
.\gradlew.bat runGym                       # opens the full Java-built app shell (GymApp → AppShellController, all 19 screens)
```

When the backend persistence layer runs (`DatabaseConnection.initializeDatabase()`), SQLite creates
`gym.db` automatically in the working directory with all 13 tables (`CREATE TABLE IF NOT EXISTS`).
`gym.db` and `.ser` object files are git-ignored — see [`src/main/resources/db/README.md`](src/main/resources/db/README.md).

## 6. Timeline (7 weeks)

> Status against this plan: **Milestone 2 is complete.** The `m3fx` library (`m3fx-core` + `m3fx-controls`),
> all 19 Java-built screens + shell, and the beginner SQLite/serialization backend (`model/`, `dao/`, `util/`,
> `exception/`) are shipped on disk. The 11-service layer was retired (`docs/DECISIONS.md` 77).
> **Milestone 3 (next):** wiring the 20 UI controllers directly to the concrete DAOs and live admin login.

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
4. Run `.\gradlew.bat build` (POSIX: `./gradlew build`) before pushing; validate all inputs and handle every failure (DB errors, bad input) with a user-friendly message — never show stack traces to users.
5. Follow the folder READMEs: keep them in sync with the code in the same change — and record built/planned status in [`docs/COVERAGE.md`](docs/COVERAGE.md), not in prose. When you extend a folder README, follow [`docs/DOCS_STANDARD.md`](docs/DOCS_STANDARD.md) — it fixes the section order, the per-file entry template and the rule that `docs/UML.md` stays the single source of truth for names, types and signatures.
6. M3 styling is mandatory: reference `-md-sys-color-*` theme lookups (applied in code via `M3Stylesheets.applyTo(scene, M3Theme)`; there is no `theme/material-theme.css` file), no hardcoded colors.

## 9. Known Limitations / Future Improvements

- **UI still shows controller demo data until Milestone 3.** The SQLite tables, 13 entities, 11 enums,
  and 13 concrete DAOs (`docs/UML.md` §1, §3) are built in `com.gym.model`, `com.gym.dao`, and
  `com.gym.util`, but Milestone 3 has not yet wired the 20 controllers to read and write `gym.db`.
- **Demo sign-in.** Any email plus a password of 4+ characters enters the shell today (`LaunchGateController`);
  database-backed authentication via `AdministratorDao` lands in Milestone 3.
- SQLite = single-machine only; multi-branch gyms need a client-server DB later.
- Offline QR/barcode check-in and payment-gateway integration are out of scope (manual entry only).
- True M3 expressive animations depend on library support; complex motion may be simplified.
- Future: reports export (PDF), reminder notifications.

## 10. Docs & Folders

Start at the [docs index](docs/README.md). It maps every question to the document that answers it and
states which document wins when two disagree.

| Document | What it is |
|----------|------------|
| [`docs/UML.md`](docs/UML.md) | **The design** — shipped runtime spine (§2), domain model (§1, §1b — 13 entities, 11 enums), 13 concrete DAOs (§3), retired services note (§4), controllers (§5), utilities & exceptions (§6), 19 business rules |
| [`docs/LEARNING_GUIDE.md`](docs/LEARNING_GUIDE.md) | Study path anchored to COMP 301 Lectures 1–6, OOP concept index, glossary, 20 defence questions |
| [`docs/DECISIONS.md`](docs/DECISIONS.md) | Why each non-obvious choice was made (78 numbered entries, append-only) |
| [`docs/BUILD_AND_RUN.md`](docs/BUILD_AND_RUN.md) | `build.gradle`/`settings.gradle` explained, `runCli`/`runGym`/`run` commands, `sqlite-jdbc` + `slf4j-api` classpath note, database paths, troubleshooting |
| [`docs/DOCS_STANDARD.md`](docs/DOCS_STANDARD.md) | The template and authority rules every folder README follows |
| [`docs/UML_TEAM.md`](docs/UML_TEAM.md) · [`docs/COVERAGE.md`](docs/COVERAGE.md) | Ownership + Milestone 2/3 status; honest built-vs-planned checklist |

**Folder-level docs** — each one teaches its layer: purpose, the OOP concepts practised there, design
patterns, rules, per-file explanations, review checklist, and learning exercises:

| Folder | Doc | Covers |
|--------|-----|--------|
| `com.gym` | [`README`](src/main/java/com/gym/README.md) | Entry points: `MainApp` gallery, `GymApp` shell launcher (+ freeze watchdog, launch gates), `Screen`/`ScreenController`, `AppShellController` |
| `model/` | [`README`](src/main/java/com/gym/model/README.md) | **Shipped (Milestone 2).** 13 `Serializable` entities + 11 enums: encapsulation, `equals`/`hashCode`, enums |
| `dao/` | [`README`](src/main/java/com/gym/dao/README.md) | **Shipped (Milestone 2 + Decision 78).** 13 concrete SQLite DAOs: JDBC `PreparedStatement`, `try-with-resources`, strict validation, `DatabaseException`, `ValidationException` |
| `cli/` | [`README`](src/main/java/com/gym/cli/README.md) | **Shipped (Decision 78).** Interactive CMD Terminal Workflow Console + `[T]` 22-scenario TA verification suite (`runCli` / `run-cli.bat`) |
| `service/` | [`README`](src/main/java/com/gym/service/README.md) | **Retired (`DECISIONS.md` 77).** Explains why the 11-service layer was removed for beginner scope |
| `controller/` | [`README`](src/main/java/com/gym/controller/README.md) | The 20 Java-built screen/shell controllers + `Screen`/`ScreenController`: event handling, demo data today, Milestone 3 DAO wiring |
| `util/` + `exception/` | [`README`](src/main/java/com/gym/util/README.md) | **Shipped (Milestone 2).** `DatabaseConnection` (13-table DDL), `SerializationUtil` (binary I/O), and `GymException` → `ValidationException` / `DatabaseException` |
| `resources/` | [`README`](src/main/resources/README.md) | Classpath assets, archived FXML reference files, runtime theming, images — plus sub-docs: [`fxml/`](src/main/resources/fxml/README.md) · [`db/`](src/main/resources/db/README.md) · [`theme/`](src/main/resources/theme/README.md) · [`images/`](src/main/resources/images/README.md) |

> **New to the project?** Read the root README §1–§4, then
> [`docs/LEARNING_GUIDE.md`](docs/LEARNING_GUIDE.md) for a walkthrough of the shipped classes.

## 11. Tests & Error-Handling Standard

**Test scope (`docs/DECISIONS.md` 77).** Following the COMP 301 beginner refactor, there are **zero test
files in the gym app** (`src/test/` was removed, so the root `:test` Gradle task reports `NO-SOURCE`).
Automated JUnit 5 suites remain active inside the reusable `m3fx` library modules (`m3fx-core/src/test/`
and `m3fx-controls/src/test/`) and run on `./gradlew test`. Backend persistence and domain serialization
are verified via live compilation and manual/Milestone 3 integration.

**Error handling.** Demo screens show inline validation messages and snackbars — no stack traces reach
the user. In the shipped Milestone 2 backend:

- DAOs call private `validate(...)` helpers before running `INSERT` or `UPDATE` and throw checked
  `ValidationException` (`extends GymException`) on invalid domain input.
- DAOs wrap `SQLException` in checked `DatabaseException` (`extends GymException`, with original
  cause chained). `DatabaseConnection` and `SerializationUtil` likewise throw `DatabaseException` for
  SQL/IO/class-not-found errors.
- In Milestone 3, controllers catch `ValidationException` and `DatabaseException`, display `e.getMessage()`
  through friendly dialogs/snackbars, and print stack traces to stderr/console only.
