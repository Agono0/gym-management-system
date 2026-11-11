# Gym-OS — Bug-Fix & Cleanup Summary

**Date:** 2026-11-10 · **Scope:** `submission/` only (55 Java files, CLI app)
**Verification:** clean compile (`javac --release 21 -Xlint:all`, 0 warnings) · full 22-scenario suite **22/22 PASS** · smoke tests of all new guards passed · zero leftover old wording (grep-verified).

---

## 1. Crash fixes (errors that could kill the whole app)

| # | Where | What was wrong | What changed |
|---|-------|----------------|--------------|
| 1.1 | `cli/GymOperations.java` (unfreeze, renew, cancelSession) | `LocalDate.parse(...)` / `LocalTime.parse(...)` called directly on values read from the DB. One malformed stored date → `DateTimeParseException` → app crashes. | Added two safe helpers `parseStoredDate()` / `parseStoredTime()` that catch the parse error and throw a clean `ValidationException` instead. All 4 call sites now use them. |
| 1.2 | `cli/TrainerCliMenu.java` (subscription renew) | Same unguarded `LocalDate.parse(s.getEndDate())`. | Wrapped in try/catch; shows a validation error and skips the renewal instead of crashing. |
| 1.3 | `cli/GymCliApp.java` (dashboard loop) | Menu dispatch had no catch-all. Any unexpected `RuntimeException` (e.g. NPE) reached `main` and terminated the app with a raw stack trace. | Wrapped the dispatch in `try { } catch (Exception e)` → prints "Unexpected error … The app recovered" + pause. The app can no longer die mid-session. |
| 1.4 | `cli/ValidationSuiteRunner.java` (was TaShowcaseRunner) | Tests used hard-coded seed IDs (`findById(4)`, slot #2, payment #4…) with no null check — if seed data ever changed, `NullPointerException` killed the app. | Every `findById` result is null-checked → prints `[FAIL] Seed data missing` instead. |
| 1.5 | `cli/ValidationSuiteRunner.java` | One thrown exception mid-suite skipped all remaining scenarios; the scoreboard understated results. | Each scenario now runs through `runCheck(...)` which catches unexpected exceptions per-scenario, counts it as FAIL, and keeps going. |
| 1.6 | `cli/CliInput.java` (`readDouble`, `readBigDecimal`, `readIsoDate`, `readIsoTime`) | Pressing **Enter** with no default value parsed the empty string → `NumberFormatException` → infinite re-prompt loop (e.g. correcting an attendance row whose check-in was NULL). | Empty input is now handled **before** parsing: shows "A value is required…" and re-prompts cleanly. |

## 2. Misleading success messages / missing guards

| # | Where | What was wrong | What changed |
|---|-------|----------------|--------------|
| 2.1 | `MemberCliMenu` progress delete | Typing any ID printed "Deleted ProgressRecord #999" even if it didn't exist — and a member could delete **another member's** record. | Now: existence check → ownership check (record must belong to the opened member) → confirmation prompt → delete. |
| 2.2 | `TrainerCliMenu` slot delete | Deleting any ID printed success; deleting a slot with CONFIRMED sessions orphaned those sessions. | Now: existence check + refuses when `countConfirmedInSlot > 0` + confirmation prompt. |
| 2.3 | `FacilityAdminCliMenu` equipment delete & staff delete | Success printed for nonexistent IDs. | `findById` first → "Equipment/Staff not found: #id"; delete also asks for confirmation (equipment). |
| 2.4 | `TrainerCliMenu` cancel subscription | Nonexistent ID → **nothing** printed (silent no-op). | Added "Subscription not found: #id" message. |
| 2.5 | `MemberCliMenu` soft delete | Set a member INACTIVE with no confirmation. | Added `readYesNo` confirmation. |
| 2.6 | `FacilityAdminCliMenu` staff edit (option 3) | An admin could set **their own** role to STAFF, locking themselves out of admin features mid-session. | Own account role can no longer be changed away from ADMIN while logged in. |
| 2.7 | `GymOperations.voidAndReplacePayment` | The old payment was voided **before** the replacement was attempted — a failed replacement left the payment voided with nothing replacing it. | Receipt/method/amount of the replacement are validated **first** (including duplicate-receipt check via `findByReceiptNo`); only then is the old payment voided. |
| 2.8 | `MembershipBillingCliMenu.voidAndReplaceFlow` | Choosing "issue replacement" but leaving the receipt blank silently skipped the replacement, yet the menu printed "Rule 19 completed…". | Receipt is now required (`readRequiredString`) when a replacement was promised, so the success message can no longer lie. |

## 3. Business-rule logic fixes

| # | Where | What was wrong | What changed |
|---|-------|----------------|--------------|
| 3.1 | `GymOperations.computeOutstandingBalance` | Outstanding balance billed **REQUESTED** sessions — a member was shown as owing money for a session that was never even confirmed. | REQUESTED sessions are now skipped (only CANCELLED was skipped before). |
| 3.2 | `GymOperations.bookOpenSlot` | Allowed booking a slot dated **in the past** and booking for **suspended/inactive** members. | Now rejects past-dated slots and non-ACTIVE customers (mirrors the check-in status logic). |
| 3.3 | `AttendanceDao.update` | The same-day duplicate check was skipped on updates (`sameDayRowExists` even had an `excludeId` parameter that was never used). Editing a row's date could create two same-day rows. | Update path now runs `sameDayRowExists(..., excludeId = row id)`. *Deliberate choice:* the customer-status/membership re-check is **not** re-run on update, because correcting the time of an old record must not fail just because the member is suspended *today*. |
| 3.4 | `GymOperations` (confirm/complete/markNoShow) | The methods relied entirely on the DAO's status machine; the comments overpromised what they checked. | Each method now explicitly checks the current status (REQUESTED→confirm, CONFIRMED→complete/NO_SHOW) with clear messages. |

## 4. Data integrity & robustness

| # | Where | What was wrong | What changed |
|---|-------|----------------|--------------|
| 4.1 | `CustomerDao.delete` / `TrainerDao.delete` | Deleting left orphan rows in every referencing table (no FK constraints in SQLite). | Second-layer reference guard inside the DAO: refuses when memberships/payments/sessions/etc. still reference the id (the existing `hardDeleteCustomer`/`hardDeleteTrainer` guard in `GymOperations` remains the first layer). |
| 4.2 | **All 13 DAOs** — `update()` / `delete()` | `executeUpdate()` result ignored → updating/deleting a nonexistent ID silently "succeeded". | Row count checked; 0 rows → `ValidationException("No X found with id: N")`. |
| 4.3 | `AdministratorDao.insert`, `PaymentDao.insert` | A rare race on the UNIQUE username/receipt constraints surfaced as a cryptic `DatabaseException("Failed to insert…")`. | `SQLException` containing "UNIQUE" is converted to the same friendly message as the app-level check. |
| 4.4 | `util/DatabaseConnection.getConnection()` | No `busy_timeout` → a second app instance failed instantly with "database is locked". | Sets `PRAGMA busy_timeout = 5000` on every new connection. |
| 4.5 | `GymCliApp.start()` | `gym.db` path is relative to the launch folder; running `java` from another directory silently created an empty second database ("all my data is gone!"). | Startup now prints the **absolute** database file path. |
| 4.6 | `SessionDao` insert/update | NULL price was stored as `0.00`, making "unset" and "free" indistinguishable. | Null price stored as SQL NULL (mirrors the other DAOs). |

## 5. Wording cleanup (neutral, general-purpose app)

- `TaShowcaseRunner.java` → **`ValidationSuiteRunner.java`**; sandbox files `gym_ta_showcase.db` → `gym_sandbox.db`, `ta_showcase_snapshot.ser` → `sandbox_snapshot.ser`; all headers/breadcrumbs "TA … Showcase" → "Validation Suite".
- `GymCliApp`: menu key `[T]` → `[V]` "Validation & Business Rules Suite (1-19)"; "COMP 301" comment removed; "Seeded Accounts (ready for … tests)" → plain description.
- Test-instruction prompts neutralized in `MemberCliMenu`, `TrainerCliMenu`, `FacilityAdminCliMenu`, `MembershipBillingCliMenu` (e.g. "try 0 or invalid end <= start to test Rule 8" → "Max Customers (>=1)"; "Test Deliberate Bad Payment" → "Record Payment with Manual Links"; "Test Deliberate DatabaseException Handling" → "Simulate Database Errors").
- "Module N" comments removed from 3 menu files; `SampleDataSeeder` note "…(for Rule 2 & 6 demo)" → "Member did not arrive" (also fixed inside the existing `gym.db` row).
- **"Rule N" numbering removed (follow-up pass, same day):** all 162 numbered labels were stripped from exception messages, menu labels, comments, seeded-data comments, and the validation suite; `GymRulesHelper` was renamed to **`GymOperations`**. Every check itself is untouched — behavior is identical (see section 8).
- `out/` was wiped and rebuilt, so no stale `TaShowcaseRunner.class` remains.

## 6. Known limitations we kept on purpose (be ready to explain these)

1. **No transactions** — multi-step operations (e.g. void → insert) use separate connections. Acceptable for a single-user CLI; SQLite's UNIQUE constraints are the backstop.
2. **Enum fallback in `mapRow`** — corrupt status strings silently map to a default (e.g. ACTIVE) instead of failing. Resilient, but hides DB corruption.
3. **No overlapping-slot / trainer double-booking checks** — two overlapping OPEN slots or two sessions at the same time can coexist; the slot-capacity check still applies.
4. **Multiple ACTIVE memberships per customer allowed** — blocking it would break renewals; "latest wins" is the de-facto behavior (`findByCustomerId … ORDER BY id DESC`).
5. **Plaintext password field** — column is named `password_hash` but stores plain text for demo simplicity; real hashing (BCrypt) would be the production answer.
6. **Payment hard-delete exists in the DAO but is not exposed in any menu** — Rule 19 void/replace is the workflow.
7. **`readIsoTime` accepts seconds too** (`14:30:45` parses) — cosmetic; the error message advertises `HH:mm`.
8. **Java serialization (.ser)** — old backups become unloadable if a model class changes shape; `serialVersionUID = 1L` keeps round-trips stable for unchanged classes.

## 7. How it was verified

1. `rm -rf out` → full recompile with `javac --release 21 -encoding UTF-8 -Xlint:all` → **0 errors, 0 warnings** (55 files).
2. Scripted session: login → `[V]` → full 22-scenario automated run → **SCOREBOARD: 22 / 22 PASSED** → clean exit.
3. Scripted session: staff delete #999 → "Staff not found"; equipment delete #999 → "Equipment not found"; progress record delete #999 → "Progress record not found" → clean exit.
4. `grep -rniE "showcase|\bTA\b|COMP [0-9]|Module [0-9]|teaching"` over `src/` + `run.bat` → **no hits**.
5. `gym.db` session note updated in place; no leftover sandbox files.

## 8. Follow-up pass — "Rule" numbering removed (2026-11-10)

**Why:** the `Rule 1–19` numbering read like course material. The numbering was pure labeling — every actual check stays.

**What changed:**
- All 162 `Rule N` labels stripped from exception messages, menu labels, comments, seeded-data comments, and the validation suite (including test scenario method names like `test18MedicalProfileRules` → `test18MedicalProfileChecks`). Messages are now plain English, e.g. `"Rule 7: Only ACTIVE memberships can be frozen (current: …)"` → `"Only ACTIVE memberships can be frozen (current: …)"`.
- `GymRulesHelper` → **`GymOperations`** (class file renamed + the 8 files that used it; field/variable `rules` → `ops`).
- `CliTheme.printRuleNotice` (unused helper) → `printNotice`.

**What did NOT change:** any validation logic, `gym.db`, `run.bat`, or the suite's pass/fail behavior.

**SLF4J startup warning fixed:** `lib/` had the SLF4J API jar (`slf4j-api-1.7.36.jar`) but no implementation, so every run printed `SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder"`. Added the matching no-op binding `slf4j-nop-1.7.36.jar` to `lib/` (from Maven Central) — picked up automatically by `run.bat`'s `lib\*` classpath. Warning gone; logging stays off as before; no code change.

**Verification:** `grep -rni rule src/` → 0 hits · clean rebuild (`javac --release 21 -Xlint:all`) → 0 errors/warnings · full suite → **SCOREBOARD 22 / 22 PASSED** with the reworded messages · startup output contains zero `SLF4J` lines.

**Note:** the team PowerPoint was produced before this pass and still shows the old "Rule 1–19" numbering and `GymRulesHelper` name — it was intentionally left as-is on request.
