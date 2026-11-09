# `service` — Retired Service Layer (JavaFX branch)

> **Status: RETIRED (`docs/DECISIONS.md` 77).** This package intentionally contains **no `.java` files**
> (only this README). Per Architecture Decision #77 (COMP 301 Lectures 1–6 beginner scope refactor),
> the planned 11-service tier (`AdminService`, `CustomerService`, `PaymentService`, etc.) was retired.
> Validation lives inside concrete DAOs (`com.gym.dao.*` throwing `ValidationException`), and screen
> workflows will connect directly from UI controllers (`com.gym.controller.*`) to concrete DAOs in
> **Milestone 3**.

## 1. Purpose — why this folder exists in documentation

Originally, the project UML drafted an intermediate 11-class service layer (`controller -> service ->
dao -> sqlite`) alongside 13 DAO interfaces and 13 DAO implementations (`40+` extra files). While clean
for large enterprise teams, that design required generics, lambda-heavy stream pipelines, and heavy
boilerplate beyond COMP 301 Lectures 1–6.

**What replaced this layer (`docs/DECISIONS.md` 77).** Instead of duplicating 13 entities across
13 DAO interfaces + 13 DAO implementations + 11 service pass-through classes:
- **13 concrete DAOs** in `com.gym.dao` own JDBC persistence and basic `validate(...)` guards.
- **20 UI controllers** in `com.gym.controller` will call concrete DAOs directly in **Milestone 3**.
- **2 checked exceptions** (`ValidationException`, `DatabaseException` under `GymException`) give the
  UI readable errors without extra service wrapper layers.

## 2. What you learn here (OOP concepts)

| Concept | Architectural lesson | Why this project switched | Decision reference |
|---|---|---|---|
| Pragmatic layering (YAGNI) | Every extra layer adds files, constructors, and boilerplate | For a 4-student beginner project using Lectures 1–6, thin direct DAOs prevent pass-through clutter | `docs/DECISIONS.md` 77 |
| Direct responsibility | Validation guards (`validate(...)`) live next to `insert`/`update` in each DAO | Keeps input checking close to persistence without splitting simple CRUD across three files | `docs/UML.md` §3–§4 |
| Checked exception propagation | DAOs throw `ValidationException` and `DatabaseException` directly to controllers | Controllers catch domain/storage failures cleanly in one `try/catch` block | `docs/UML.md` §6 |

## 3. Design patterns in this layer

| Pattern | Status | Reason |
|---|---|---|
| Enterprise Service Layer (11 stateless services) | **Retired** (`DECISIONS.md` 77) | Removed pure CRUD pass-through boilerplate to keep code natural and beginner-accessible |
| Direct Controller → Concrete DAO | **Active target (Milestone 3)** | Reduces cognitive load while preserving clean separation of UI vs SQL (`controller/` vs `dao/`) |

## 4. Rules you must follow

1. **Do not recreate `*Service.java` classes here** unless the team explicitly logs a reversal in
   `docs/DECISIONS.md` and `docs/UML.md` §4 first.
2. **Do not embed SQL inside controllers** — removing the service tier does *not* permit writing SQL
   in UI classes; all SQL stays inside `com.gym.dao` (`docs/UML.md` §2).
3. **Rule enforcement for Milestone 3:** When controllers wire the DAOs in Milestone 3, cross-check
   the 19 business rules in `docs/UML.md` using helper methods or DAO queries without inventing Java
   features beyond the team's agreed scope.

## 5. The files in this folder

This folder holds only `README.md` (`*.java` count: **0**). All legacy service specifications from
earlier drafts remain recorded in git history and summarized as retired in `docs/UML.md` §4.

## 6. How this folder relates to active layers

| Former Planned Role | Where It Lives Now |
|---|---|
| `CustomerService`, `TrainerService`, `PaymentService`, … validation | Private `validate(...)` methods in concrete `com.gym.dao.*` classes (`ValidationException`) |
| JDBC / SQLite persistence | Concrete `com.gym.dao.*` classes (`DatabaseConnection`) |
| UI event coordination & Milestone 3 business-rule wiring | `com.gym.controller.*` (`Milestone 3`) |

## 7. Common mistakes and review checklist

- **Mistake:** Importing `com.gym.service.*` in new code or tutorials — those classes do not exist on disk.
- **Mistake:** Writing `DriverManager.getConnection(...)` directly inside a controller because the
  service tier is gone — use concrete `com.gym.dao.*` classes instead.

Checklist:

- [ ] No unused `com.gym.service` imports anywhere in `src/`.
- [ ] New validation logic goes in `com.gym.dao.*` (throwing `ValidationException`) or Milestone 3
      controller coordination, never raw SQL in UI code.

## 8. Learning exercises (with answers)

1. Why did the team retire the planned 11-service layer in Decision #77?
2. After retiring `com.gym.service`, where must SQL queries still live, and why?
3. If `CustomerDao.insert(customer)` receives an empty `fullName`, what happens instead of a
   `CustomerService.validate(...)` call?

<details>
<summary>Answers</summary>

1. To eliminate ~25 redundant interface and service pass-through files and keep the codebase aligned with
   COMP 301 Lectures 1–6 beginner scope.
2. Exclusively inside `com.gym.dao.*` (and table DDL inside `DatabaseConnection`). Putting SQL in UI
   controllers scatters table schemas across 19 screens and leaks open connections.
3. `CustomerDao.validate(customer)` runs inside `insert` and throws checked `ValidationException`.

</details>

## 9. Ownership and tasks

| Area | Status | Notes |
|---|---|---|
| Planned 11 services (`AdminService` … `WorkoutService`) | **Retired (`DECISIONS.md` 77)** | Logic consolidated into `com.gym.dao` + Milestone 3 controller wiring |
