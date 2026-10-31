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

> **Honest scope (read this first).** What runs today is an **interactive M3 demo** of the whole
> product: 19 FXML screens (including the app shell) behind a live launcher (`GymApp`), driven by
> demo data held in the controllers and a demo sign-in (any email + a password of 4+ characters —
> labelled as demo in the UI copy). The persistence backend (`model/`, `dao/`, `service/`, `util/`,
> SQLite) is **planned, not built**: `docs/UML.md` is its agreed design, and the folder READMEs
> teach how each layer will be written. [`docs/COVERAGE.md`](docs/COVERAGE.md) tracks exactly what
> exists.

> ⚠️ **AI usage warning:** AI was used to generate the documentation and to
> assist with GUI design.

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

Run the component showcase: `gradlew run` — `MainApp` builds the gallery in code (seed picker,
light/dark, spring playground, 3D/effects pages, plus a "Gym App Screens" viewer that loads the 19
app FXML screens). `fxml/main.fxml` is the controller-free FXML declaration artifact; no code
loads it today.

## 2. Features

These are the product's feature areas. Each has a matching screen in the demo today; the
business logic behind them (validation, persistence, the 19 business rules in
[`docs/UML.md`](docs/UML.md)) is the planned backend described by that design.

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
| Language   | Java 27 (OpenJDK 27) |
| UI         | JavaFX 21+ (FXML + CSS) |
| Design     | Material Design 3 via the in-repo **m3fx** library (`m3fx-core` + `m3fx-controls`, see §2b); M3 expressive motion where supported |
| Build      | Gradle 9.8.0 via the committed wrapper (`gradlew`/`gradlew.bat` — never install Gradle by hand; see [`docs/BUILD_AND_RUN.md`](docs/BUILD_AND_RUN.md)) + `application` and JavaFX plugins |
| Database   | **none yet** — the app runs on demo data in the controllers; SQLite (`gym.db`, JDBC) is the planned backend (`docs/UML.md` §3). The SQLite-JDBC dependency was removed from `build.gradle` until the DAO layer lands |
| Error handling | demo screens answer with inline field errors, snackbars and dialogs; the service-layer validation + `DaoException` translation standard is planned with the backend (`README` §11) |
| Testing    | JUnit 5 + the project's own JavaFX harness (`FXMLLoader`-driven, headless): 9 test classes / 59 tests in the app module, 195 tests across the 3 Gradle modules — `./gradlew test` |
| IDE        | IntelliJ IDEA (project SDK: `openjdk-27`, see `.idea/misc.xml`) |

## 4. Project Structure / Files Map

```
(build.gradle, settings.gradle — Gradle build)
src/main/java/com/gym/
  MainApp.java            m3fx gallery entry point (interactive component showcase)
  GymApp.java             app entry point: launches app-shell.fxml, freeze watchdog, launch gates
  M3Samples.java          convenience launcher alias for the gallery (extends MainApp)
  controller/  20 controllers: AppShellController (nav shell), 18 FXML screen controllers,
               LaunchGateController (code-built splash + login gate) → README
  model/       planned — entity POJOs (Customer, Trainer, …); README only
  dao/         planned — DAO interfaces + SQLite impls; README only
  service/     planned — business logic + validation per feature; README only
  util/        planned — DbConnection, validators, date/time + FX helpers; README only
src/main/resources/
  fxml/       20 FXML files: 19 screens (incl. app-shell) + main.fxml (component artifact)
  theme/      theme applied in code — `M3Stylesheets.applyTo(scene, M3Theme)` (no CSS file)
  db/         planned — schema.sql + seed data; README only
  images/     logo, icons, placeholders
src/test/java/com/gym/fxml/   9 JUnit 5 harness classes, 59 tests (headless JavaFX)
```

Each folder above contains its own `README.md`. The shipped folders' docs teach the real code;
`model/`, `dao/`, `service/`, `util/` and `db/` are clearly marked **planned, not built** — they
keep the agreed design from [`docs/UML.md`](docs/UML.md) and the how-to/learning material, without
pretending the code exists. Start from the [docs index](docs/README.md) or the
[learning guide](docs/LEARNING_GUIDE.md); [`docs/COVERAGE.md`](docs/COVERAGE.md) tracks what is
actually built.

## 5. Installation & Running

Requirements: **JDK 27** and the **committed Gradle wrapper** (Gradle 9.8.0 — do not install Gradle by
hand). On Windows use `.\gradlew.bat`; the `.\` is required in PowerShell. Full walkthrough, IntelliJ
setup and troubleshooting: [`docs/BUILD_AND_RUN.md`](docs/BUILD_AND_RUN.md).

```bash
git clone https://github.com/Agono0/gym-management-system.git
cd gym-management-system
git checkout main
.\gradlew.bat build    # POSIX: ./gradlew build  (also runs the test suites)
.\gradlew.bat run      # opens the m3fx component gallery (MainApp)
.\gradlew.bat runGym   # opens the full app shell (GymApp → app-shell.fxml, all 19 screens)
```

There is no database to create: the demo keeps its data in the controllers. When the DAO layer
lands, `gym.db` will be created in the working directory at first run and git-ignored, and
`schema.sql` will be the committed half — see the (planned) design in
[`src/main/resources/db/README.md`](src/main/resources/db/README.md).

## 6. Timeline (7 weeks)

> Status against this plan: the UI track ran ahead of the original schedule (all 19 screens, the
> app shell and the m3fx library are built and tested), while the Week-1 backend milestones
> (SQLite schema v1, `DbConnection`, DAO base) have not started — they are the next track, per
> [`docs/UML.md`](docs/UML.md) §3–§4 and `PLAN.md` "Current phase".

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

- **No persistence.** Everything the demo shows is demo data in the controllers; nothing survives a
  restart. The SQLite backend is designed (`docs/UML.md` §3–§4, 19 business rules) but not built.
- **Demo sign-in.** Any email plus a password of 4+ characters signs in; there are no roles yet.
  Role-based login (ADMIN/MANAGER/STAFF) is designed (`Administrator`, `AdminService`,
  `SessionContext.requireRole`) and lands with the backend.
- SQLite = single-machine only; multi-branch gyms need a client-server DB later.
- Offline QR/barcode check-in and payment-gateway integration are out of scope (manual entry only).
- True M3 expressive animations depend on library support; complex motion may be simplified.
- Future: reports export (PDF), reminder notifications.

## 10. Docs & Folders

Start at the [docs index](docs/README.md). It maps every question to the document that answers it and
states which document wins when two disagree.

| Document | What it is |
|----------|------------|
| [`docs/UML.md`](docs/UML.md) | **The design** — the shipped runtime spine (§5a), domain model, layer diagrams (backend layers marked planned), every class with its fields/signatures, the 19 business rules |
| [`docs/LEARNING_GUIDE.md`](docs/LEARNING_GUIDE.md) | Study path, OOP concept index (where each concept is practised), glossary, 40 defence questions |
| [`docs/DECISIONS.md`](docs/DECISIONS.md) | Why each non-obvious choice was made (numbered, append-only) |
| [`docs/BUILD_AND_RUN.md`](docs/BUILD_AND_RUN.md) | `build.gradle`/`settings.gradle` explained, commands, database paths, IntelliJ setup, troubleshooting |
| [`docs/DOCS_STANDARD.md`](docs/DOCS_STANDARD.md) | The template and authority rules every folder README follows |
| [`docs/UML_TEAM.md`](docs/UML_TEAM.md) · [`docs/COVERAGE.md`](docs/COVERAGE.md) | Ownership + status; honest built-vs-planned checklist |

**Folder-level docs** — each one teaches its layer: purpose, the OOP concepts practised there, design
patterns, the rules and the bug each rule prevents, then one detailed entry per file (why it is shaped
that way, how to write it step by step, a sketch, how to verify it, common mistakes), a review
checklist and learning exercises:

| Folder | Doc | Covers |
|--------|-----|--------|
| `com.gym` | [`README`](src/main/java/com/gym/README.md) | Entry points: `MainApp` gallery, `GymApp` shell launcher (+ freeze watchdog, launch gates), `AppShellController`, `LaunchGateController` |
| `model/` | [`README`](src/main/java/com/gym/model/README.md) | **Planned, not built.** 13 entities + 11 enums: encapsulation, `equals`/`hashCode`, enums with behaviour |
| `dao/` | [`README`](src/main/java/com/gym/dao/README.md) | **Planned, not built.** 13 interface/impl pairs + JDBC: polymorphism at a seam, prepared statements, exception translation |
| `service/` | [`README`](src/main/java/com/gym/service/README.md) | **Planned, not built.** 11 services: business rules 1–19, state machine, `BigDecimal` money |
| `controller/` | [`README`](src/main/java/com/gym/controller/README.md) | The 20 shipped controllers: MVC in JavaFX, FXML binding, event handling, thin-controller discipline, demo-data convention |
| `util/` | [`README`](src/main/java/com/gym/util/README.md) | **Planned, not built.** 10 helpers: static utilities, `DbConnection` seam, PBKDF2 hashing, ISO date handling, session state |
| `resources/` | [`README`](src/main/resources/README.md) | Classpath assets, FXML, dynamic theming, images — plus one doc per sub-folder: [`fxml/`](src/main/resources/fxml/README.md) · [`db/`](src/main/resources/db/README.md) (planned schema) · [`theme/`](src/main/resources/theme/README.md) · [`images/`](src/main/resources/images/README.md) |

> **New to the project?** Read the root README §1–§4, then
> [`docs/LEARNING_GUIDE.md`](docs/LEARNING_GUIDE.md) §2 for the 90-minute orientation.

## 11. Tests & Error-Handling Standard

**The app does have automated tests.** `src/test/java/com/gym/fxml/` holds 9 JUnit 5 harness
classes with 59 tests that run the real FXML screens headlessly: integrity/loading of all screens
(strict by default), `fx:id`/`@FXML` binding contracts, scroll-height bounds, shell flows
(trainer → profile, customer → profile, dashboard → sign-out, payment and attendance actions), and
stress loads. Together with the m3fx suites (8 tests in `m3fx-core`, 128 in `m3fx-controls`) that is
195 tests across 3 modules — `./gradlew test`, all green. `TEST_READY.md` documents the harness.

**Error handling.** The demo screens answer invalid input with inline field errors, snackbars and
dialogs — no stack traces reach the user. The service-layer standard below is what the planned
backend adds on top (it is written out in the folder READMEs so the DAOs can follow it from day
one):

- Validate every user input at the service layer (`Validators`); reject bad
  data with a clear field-level message before it reaches the DAO.
- DAOs wrap `SQLException` in `DaoException`; services translate it into a
  plain-language message for the UI (e.g. "Could not save customer — check
  the database file and try again.").
- Controllers show friendly dialogs; stack traces go to the log/console only,
  never to the user.
- Before release, each owner walks every screen of their modules and triggers
  each failure path manually.
