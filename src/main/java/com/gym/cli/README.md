# `cli` — Terminal Workflow Console & TA Verification Suite

> **Signature authority:** `docs/UML.md` owns every domain entity (§1), enum (§1b), DAO signature (§3), and the 19 business rules. This README explains *why* `com.gym.cli` exists and *how* its 13 console classes work using COMP 301 Lectures 1–6 Java syntax (`docs/DECISIONS.md` 78).

## 1. Purpose — the problem this layer solves

`com.gym.cli` is a **pure-terminal (CMD) workflow application** built for end-to-end testing, Teaching Assistant grading, and live command-line verification of every feature in the Gym Management System without launching JavaFX. From `GymCliApp.main(String[])` (`./gradlew runCli` or `run-cli.bat`), the console boots SQLite persistence (`DatabaseConnection.initializeDatabase()`), seeds repeatable sample records if empty (`SampleDataSeeder`), authenticates against real `administrators` rows (rule 16), and lets the user or grader run every CRUD operation, trigger every validation rule, test `ValidationException` / `DatabaseException` paths cleanly, and verify 13-table binary `.ser` snapshots via `SerializationUtil`.

**What breaks if this layer is missing.** When JavaFX screens still run on demo data before Milestone 3 wiring (`docs/DECISIONS.md` 77), an instructor testing the persistence backend has no interactive way in CMD to walk through a real check-in, freeze/unfreeze shift, XOR payment voiding, or staff permission denial — every test would require editing source files and recompiling ad-hoc `main` methods.

**What this layer deliberately does not do.** Zero JavaFX imports, zero raw SQL `INSERT`/`UPDATE`/`DELETE` mutations bypassing DAOs (only `SampleDataSeeder.resetAndSeed()` clears rows on reset and `GymRulesHelper.countReferences`/`countConfirmedInSlot` run read-only `COUNT(*)` checks), and zero test files under `src/test/` (`docs/DECISIONS.md` 77).

## 2. What you learn here (OOP concepts)

| Concept | Where in this folder | Why the project needs it | How to apply it (one line) |
|---|---|---|---|
| Encapsulation | `CliSession` flags (`returnHome`, `logoutRequested`, `exitRequested`) | Navigation state across multi-level sub-menus must change cleanly without passing booleans back up 4 stack frames | Keep navigation state private in `CliSession` and expose explicit helper methods like `shouldStopSubMenu()` |
| Object Serialization (`Lecture 6`) | `GymBackupSnapshot implements Serializable` + `SerializationUtil` | Full 13-table system snapshots must survive disk export and reload in a single `.ser` file | Declare `serialVersionUID = 1L` and store concrete `Serializable` `ArrayList<...>` fields (avoid non-`Serializable` `List` fields under `-Xlint:serial`) |
| Checked Exception Handling (`Lecture 6`) | Sequential `catch (ValidationException e)` + `catch (DatabaseException e)` in every `*CliMenu` | Bad input must print `[! VALIDATION ERROR]` and stay inside the menu, while storage/file faults print `[! DATABASE ERROR]` | Never catch generic `Exception` when separate checked catch blocks distinguish domain errors from I/O errors |
| Composition | Menu handlers hold `CliInput`, `CliSession`, `GymRulesHelper`, and specific concrete `*Dao` fields | Screens compose shared terminal helpers instead of subclassing a monolithic menu base class | Inject shared `input` and `session` via constructors |
| Enum State Machines | `ScheduleWorkoutCliMenu` + `MembershipBillingCliMenu` (`SessionStatus`, `MembershipStatus`) | Prevents impossible transitions such as confirming a `COMPLETED` session or renewing a `CANCELLED` plan | Validate old vs new enum values before calling DAO `update(...)` |
| Static vs Instance Members | `SampleDataSeeder` / `CliTheme` utilities vs stateful `CliSession` / `*CliMenu` objects | Formatting and DB seeding need no instance state, whereas active login identity and `Scanner` streams belong to one running session | Use `private` constructors on utility classes; pass instance dependencies explicitly |
| Single Responsibility | Split across `CliTheme` (rendering), `CliInput` (parsing), `GymRulesHelper` (cross-entity rules), and 5 domain menu classes | Keeps each file short (~150–610 lines) so any teammate can read their module's CLI workflow cleanly | Never mix SQL queries inside input-parsing loops |

## 3. Design patterns in this layer

| Pattern | Where | Why this pattern | What goes wrong if used carelessly |
|---|---|---|---|
| **Console Router & Navigation State** | `GymCliApp`, `CliSession`, 5 `*CliMenu` classes | Deep sub-menus (e.g., `Main Menu > Members > Profile #1 > Medical`) need reliable `[0] Back`, `[H] Main Menu`, and `[L] Logout` jumps | Calling `System.exit(0)` or `break` arbitrarily skips logout/sandbox cleanup and leaves SQLite/file handles unmanaged |
| **Snapshot DTO** | `GymBackupSnapshot` + `GymRulesHelper.exportSnapshotToFile` / `loadSnapshotFromFile` | Packages all 13 entity `ArrayList` collections and metadata into one `Serializable` object for `SerializationUtil.saveToFile` | Declaring field types as the interface `List<...>` triggers 13 `-Xlint:serial` compiler warnings; concrete `ArrayList<...>` avoids this cleanly |
| **Cross-Entity Rules Helper** | `GymRulesHelper` (over the 13 concrete DAOs) | Rules 1, 2, 4, 5, 6, 7, 10, 11, 13–19 touch multiple DAOs at once (e.g., rule 14 suspension cascading freeze across memberships) | Scattering cascade logic into menu prompts makes interactive menus and automated TA verification drift apart |
| **Isolated Verification Sandbox** | `TaShowcaseRunner` (`gym_ta_showcase.db`) | Grading checks must run repeatably without corrupting the user's live `gym.db` | Forgetting `DatabaseConnection.setDbUrl(previousUrl)` in a `finally` block leaves interactive menus pointing at the deleted temp DB |
| **Inheritance — avoided** | Menu classes (`MemberCliMenu`, etc.) do not extend a `BaseMenu` class | Plain COMP 301 Java classes stay flat and readable without abstract template hooks (`DECISIONS.md` 77) | Adding heavy generic menu frameworks obscures control flow for beginners |

## 4. Rules you must follow (and the bug each rule prevents)

1. **Strict COMP 301 Lectures 1–6 Java syntax** — no streams, no lambdas, no `var`, no `record`, no generic `<T>` helper methods, and no JavaFX imports; keeps the package readable to beginners and runnable without GUI libraries.
2. **Pure 7-bit ASCII output (`CliTheme` + prompts/messages)** — Windows `cmd.exe` defaults to legacy codepages (Cp1252 / OEM-437) where Unicode em-dashes (`—`) or box drawing characters print as `?` or ``.
3. **Sequential checked `catch` blocks (`Lecture 6` slide 13)** — catch `ValidationException` before `DatabaseException` (no multi-catch `A | B`) so users see `[! VALIDATION ERROR]` separately from `[! DATABASE ERROR]`.
4. **Always pair validation lookups before writes cleanly** — close lookup `Connection`/`PreparedStatement` handles inside `try-with-resources` before `Dao.insert`/`Dao.update` opens its write connection, preventing `SQLITE_BUSY`.
5. **Check `Scanner.hasNextLine()` / `null` return from `CliInput`** — prevents `NoSuchElementException` crashes when stdin scripts or piped CMD input reach EOF.
6. **Preserve sandbox `finally` restoration** — `TaShowcaseRunner` must always restore `DatabaseConnection.setDbUrl(previousUrl)` and remove `gym_ta_showcase.db` / `ta_showcase_snapshot.ser` after running scenarios.

## 5. The files in this folder

Write/dependency order: Foundation formatting/input/state (`CliTheme` → `CliInput` → `CliSession`) → Serialization bean & seeding (`GymBackupSnapshot` → `SampleDataSeeder`) → Cross-entity logic (`GymRulesHelper`) → Domain menus (`MemberCliMenu`, `TrainerCliMenu`, `MembershipBillingCliMenu`, `ScheduleWorkoutCliMenu`, `FacilityAdminCliMenu`) → Grading suite (`TaShowcaseRunner`) → Launcher (`GymCliApp`).

### `CliTheme.java` — Pure ASCII header, breadcrumb, table & banner renderer

**Status:** done.
**Canonical:** terminal display helper (`docs/DECISIONS.md` 78).
**What it is.** Provides fixed-width 80-column ASCII double/single lines, screen headers with active user label, DB target, breadcrumb path, tabular aligned column printing (`printTable`), and status prefixes (`[OK]`, `[! VALIDATION ERROR]`, `[! DATABASE ERROR]`).
**Why it is designed this way.** Using plain `+---+` and `|...|` ASCII avoids Unicode garbling in standard Windows `cmd.exe` fonts without requiring `chcp 65001`.
**How to write it.** Define `LINE_WIDTH = 80`, add `printDoubleLine()`/`printSingleLine()`, implement header/footer helpers, and write a padded truncation helper (`padRight`, `truncate`, `safeText`) before formatting table rows.
**Sketch.**
```java
// Sketch — src/main/java/com/gym/cli/CliTheme.java
public static void printHeader(String title, String userLabel, String dbUrl, String breadcrumb) {
    printDoubleLine();
    String pathLine = " Path: " + safeText(breadcrumb);
    System.out.println("|" + padRight(truncate(pathLine, LINE_WIDTH - 2), LINE_WIDTH - 2) + "|");
    printDoubleLine();
}
```
**How to know it is right.** Visually inspect any menu banner in `cmd /c run-cli.bat`; left/right `+` borders align cleanly at column 80 with zero `?` / `` glyphs.
**Common mistakes.**
- Embedding `\u2014` (`—`) in titles or messages → renders as `` in CMD → replace with ASCII `-`.
- Printing raw `\n` inside table cells → breaks `|...|` alignment → strip via `safeText(...)`.
- Truncating without checking `maxLen <= 3` → `substring` index error on narrow columns → guard `maxLen <= 3`.

### `CliInput.java` — EOF-safe `Scanner` input & format parsers

**Status:** done.
**Canonical:** terminal input helper (`docs/UML.md` rule 9 ISO formats, `BigDecimal` money).
**What it is.** Wraps `java.util.Scanner` with `readLine`, `readRequiredString`, `readRawString` (allows blank submission to test DAO `ValidationException`), optional defaults, numeric parsers (`BigDecimal`, `Integer`, `Double`), ISO date (`LocalDate.parse`), ISO time (`LocalTime.parse`), `readYesNo`, and `pause`.
**Why it is designed this way.** Always reading full lines via `nextLine()` eliminates the classic `Scanner.nextInt()` newline leftover bug taught in introductory Java.
**How to write it.** Store `private final Scanner scanner`, check `scanner.hasNextLine()` before reading every line (returning `null` at EOF), and loop on `NumberFormatException` / `DateTimeParseException`.
**Sketch.**
```java
// Sketch — always check hasNextLine() before nextLine()
public String readRawString(String prompt) {
    return readLine(prompt); // callers can pass empty string to trigger DAO ValidationException
}
```
**How to know it is right.** Pipe fewer lines than a full session into `./gradlew runCli`; the app exits cleanly without `NoSuchElementException`.
**Common mistakes.**
- Mixing `nextInt()` and `nextLine()` → skips subsequent string prompts → use `Integer.parseInt(readLine(...))` only.
- Using `readRequiredString` on validation-test prompts → prevents testing blank-name `ValidationException` → use `readRawString`.
- Parsing money as `Double.parseDouble` → binary float error (`DECISIONS.md` 31) → use `new BigDecimal(raw)`.

### `CliSession.java` — Active user & menu navigation flags

**Status:** done.
**Canonical:** holds `Administrator` from `docs/UML.md` §1 for rule 16 RBAC checks.
**What it is.** Simple JavaBean storing the signed-in `Administrator` plus three flags (`returnHome`, `logoutRequested`, `exitRequested`) and helpers `userLabel()`, `clearTransientFlags()`, and `shouldStopSubMenu()`.
**Why it is designed this way.** Sub-menus nested 3–4 levels deep inspect `session.shouldStopSubMenu()` in their `while` loops so `[H] Main Menu` or `[L] Logout` unwinds cleanly.
**How to write it.** Private fields + getters/setters + helper methods returning combined boolean flags.
**Sketch.**
```java
// Sketch — src/main/java/com/gym/cli/CliSession.java
public boolean shouldStopSubMenu() {
    return returnHome || logoutRequested || exitRequested;
}
```
**How to know it is right.** Selecting `H` inside `Members > Profile #1 > Medical` returns straight to `Main Menu` in one keypress.
**Common mistakes.**
- Forgetting to reset `returnHome = false` at the top of `GymCliApp.runMainDashboardLoop()` → re-entering any menu immediately bounces home.
- Leaving `currentUser` unguarded on `userLabel()` → NPE on the Login Gate before sign-in → return `"Guest"` if `null`.
- Checking only `logoutRequested` in inner menu loops → breaks `[H]` shortcut from nested screens → use `shouldStopSubMenu()`.

### `GymBackupSnapshot.java` — Serializable 13-table container

**Status:** done.
**Canonical:** holds concrete `ArrayList` copies of all 13 `Serializable` entities (`docs/UML.md` §1).
**What it is.** A `Serializable` POJO (`serialVersionUID = 1L`) containing `createdAt`, `createdBy`, and one `ArrayList<...>` field per entity table, plus `totalRecordsCount()`.
**Why it is designed this way.** Using concrete `ArrayList<...>` field types instead of `java.util.List<...>` satisfies Java's `-Xlint:serial` compiler lint (since `List` does not extend `Serializable`).
**How to write it.** Implement `Serializable`, declare `serialVersionUID = 1L`, add private `ArrayList` fields and JavaBean accessors, and sum sizes safely (null-checked) in `totalRecordsCount()`.
**Sketch.**
```java
// Sketch — concrete ArrayList preserves Serializable contract under -Xlint:serial
public class GymBackupSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;
    private ArrayList<Customer> customers;
}
```
**How to know it is right.** Compile with `-Xlint:all,-processing` (0 serial warnings) and export/reload `.ser` in Menu `[10]`.
**Common mistakes.**
- Declaring fields as `List<Customer>` → 13 `[serial]` javac warnings → declare `ArrayList<Customer>`.
- Omitting `serialVersionUID = 1L` → class edits invalidate existing `.ser` backups (`InvalidClassException`).
- Calling `.size()` without `!= null` in `totalRecordsCount()` → NPE on partial snapshots.

### `SampleDataSeeder.java` — Repeatable gym seed & reset utility

**Status:** done.
**Canonical:** seeds all 13 tables via the 13 `com.gym.dao` classes (`docs/UML.md` §1, §3).
**What it is.** Populates 4 staff accounts (`admin`, `manager`, `staff`, inactive `oldstaff`), 3 coaches, 5 customers across `ACTIVE`/`SUSPENDED`/`INACTIVE`, 5 memberships (`ACTIVE`/`EXPIRED`/`FROZEN`/`CANCELLED`), 1 on-demand medical profile, 3 progress logs, 2 workout versions, 3 slots, 1 subscription, 4 sessions, 6 equipment units, 2 attendance logs, and 4 payments (including a voided + replacement pair).
**Why it is designed this way.** Relative `LocalDate.now()` anchors keep expiry, 2h cancellation notice, and daily check-in demos accurate on any calendar date the TA runs the project.
**How to write it.** In `seedIfEmpty()`, check `adminDao.findAll().isEmpty()`; in `resetAndSeed()`, delete tables in child-to-parent order, reset `sqlite_sequence`, and call `seedRecords()`.
**Sketch.**
```java
// Sketch — safe empty check before seeding startup DB
public static void seedIfEmpty() throws DatabaseException, ValidationException {
    if (!new AdministratorDao().findAll().isEmpty()) {
        return;
    }
    seedRecords();
}
```
**How to know it is right.** Deleting `gym.db` and launching `runCli` displays live KPIs (`Members 5 (3 Active) | Coaches: 3 | Revenue: $680`).
**Common mistakes.**
- Hardcoding stale calendar years (`"2024-05-01"`) → expiry and 2h notice tests drift over time → compute relative to `LocalDate.now()`.
- Calling the removed 13-arg `Payment` constructor → compile error (`Payment` uses 10-arg constructor + setters for void fields).
- Resetting parent tables before child tables without FK order → risks integrity errors when constraints are enabled.

### `GymRulesHelper.java` — Cross-entity engine for rules 1–19 & serialization

**Status:** done.
**Canonical:** implements `docs/UML.md` business rules 1–19 using concrete `com.gym.dao.*` and `SerializationUtil`.
**What it is.** Coordinates all multi-table workflows: `authenticate`/`requireAdmin` (rule 16), `softDeleteCustomer`/`hardDeleteCustomer`/`hardDeleteTrainer` (rule 4), `suspendCustomer` (rule 14), `freezeMembership`/`unfreezeMembership`/`renewMembership` (rule 7), `effectiveMembershipStatus`/`effectiveSlotStatus` (rule 13), `bookOpenSlot`/`requestOutsideSlots`/`adminReserve`/`confirmSession`/`completeSession`/`markNoShow`/`cancelSession` (rules 1, 2, 10, 11, 17, 18), `checkIn`/`checkOut` (rule 5), `getMedicalProfileForTrainer` (rules 3, 15), `computeOutstandingBalance` (rule 6), `voidAndReplacePayment` (rule 19), and `.ser` export/load.
**Why it is designed this way.** Isolates cross-entity coordination in plain COMP 301 loops so both interactive menus and `TaShowcaseRunner` share identical business behavior without bringing back the retired `com.gym.service` layer (`DECISIONS.md` 77).
**How to write it.** Instantiate DAOs as fields, write `for (int i = 0; i < list.size(); i++)` loops using `BigDecimal.ZERO` for monetary sums, and close `countReferences` / `countConfirmedInSlot` connections with `try-with-resources`.
**Sketch.**
```java
// Sketch — rule 16 active & role enforcement
public void requireAdmin(Administrator actor) throws ValidationException {
    if (actor == null || actor.getRole() != StaffRole.ADMIN) {
        throw new ValidationException("Rule 16: StaffRole.ADMIN required");
    }
}
```
**How to know it is right.** Category `#1–#11` scenarios in `TaShowcaseRunner` pass `22 / 22` against real SQLite rows.
**Common mistakes.**
- Using `double` when summing unpaid balances in `computeOutstandingBalance` → rounding drift → always use `BigDecimal.add`/`subtract`.
- Counting non-`CONFIRMED` sessions in `countConfirmedInSlot` → violates rule 17 (`REQUESTED` or `CANCELLED` must not fill a slot).
- Allowing `Subscription` balance calculation for `subscriptionId != null` sessions → violates rule 11 (covered sessions are free).

### `MemberCliMenu.java` — Interactive screens for Members, Medical & Progress

**Status:** done.
**Canonical:** interacts with `CustomerDao`, `MedicalProfileDao`, `ProgressRecordDao`, `MembershipDao`, `WorkoutProgramDao` (`docs/UML.md` §1, §3).
**What it is.** Covers Module `[1]` (`List All`, `Search`, `Member 360 Profile`, `Register Member`), nested Medical Profile manager (creating on demand, enforcing rule 3 & rule 15 trainer access simulation), Progress log CRUD + ASCII weight bar chart, and soft/hard delete + suspension cascade (rules 4, 14).
**Why it is designed this way.** Groups member-centric sub-screens into a single 360 hub matching `CustomerProfileController`.
**How to write it.** Outer menu loop checks `session.shouldStopSubMenu()`, opens `openMemberProfile(id)`, and delegates to `openMedicalMenu(c)` and `openProgressMenu(c)` catching `ValidationException` and `DatabaseException` separately.
**Sketch.**
```java
// Sketch — sequential exception catches keep UI responsive on bad input
try {
    Customer saved = customerDao.insert(c);
    CliTheme.printSuccess("Registered Member #" + saved.getId());
} catch (ValidationException e) {
    CliTheme.printValidationError(e.getMessage());
} catch (DatabaseException e) {
    CliTheme.printDatabaseError(e.getMessage());
}
```
**How to know it is right.** Register a blank customer (prints `[! VALIDATION ERROR]`), then test Member `#1` Medical Trainer `#2` access (blocked by rule 15) vs `#1` (granted).
**Common mistakes.**
- Calling `readRequiredString` when prompting for fields during validation testing → blocks demonstrating DAO checks.
- Leaving `openMedicalMenu` stuck in a loop on `[H]` → always test `!session.shouldStopSubMenu()` in nested loops.
- Forgetting `input.pause()` after prints → error messages scroll off screen when returning to the menu header.

### `TrainerCliMenu.java` — Interactive screens for Coaches, Availability & Subscriptions

**Status:** done.
**Canonical:** interacts with `TrainerDao`, `TrainerAvailabilityDao`, `TrainerSubscriptionDao`, `CustomerDao` (`docs/UML.md` §3).
**What it is.** Implements Module `[2]`: coach listing with live rule 2 `NO_SHOW` counter, coach profile with assigned roster (`Customer.trainerId`), medical read verification (rule 15), admin hard-delete reference guard (rule 4), availability slot management (`OPEN`/`FULL`/`CLOSED`, rules 8, 13, 17), and trainer subscription create/renew/cancel (rule 11).
**Why it is designed this way.** Keeps coach staffing, bookable time windows, and recurring period subscriptions under one coherent terminal menu.
**How to write it.** Render coach and slot tables with derived `effectiveSlotStatus` (`booked/maxCustomers`), parse `BigDecimal` hourly rates, and catch `ValidationException` / `DatabaseException`.
**Sketch.**
```java
// Sketch — rule 13/17 live occupancy formatting in slot table
int booked = rules.countConfirmedInSlot(s.getId());
AvailabilityStatus eff = rules.effectiveSlotStatus(s);
```
**How to know it is right.** Booking Slot `#2` (`maxCustomers = 1`) flips displayed `Effective` from `OPEN` to `FULL` and rejects a second customer.
**Common mistakes.**
- Checking `s.getStatus() == AvailabilityStatus.FULL` raw column instead of `rules.effectiveSlotStatus(s)` → misses rule 13 derived capacity.
- Allowing subscription renew after `CANCELLED` → violate period lifecycle rules → block when `CANCELLED`.
- Inserting availability slots without catching rule 8 time-order `ValidationException`.

### `MembershipBillingCliMenu.java` — Interactive screens for Memberships & Payments

**Status:** done.
**Canonical:** interacts with `MembershipDao` & `PaymentDao` (`docs/UML.md` §1, §3).
**What it is.** Hosts Module `[3]` (`Sell`, `Freeze`, `Unfreeze`, `Renew`, `Cancel` plans respecting rule 7 & rule 13 derived `EXPIRED`) and Module `[7]` (`Record Payment`, deliberate Rule 12 XOR/owner tester, Rule 6 `computeOutstandingBalance`, receipt lookup, and Rule 19 void + replacement invoice flow).
**Why it is designed this way.** Separates plan lifecycle management (`[3]`) and POS billing/void ledger (`[7]`) cleanly while sharing fee/balance helpers.
**How to write it.** Suggest ISO end dates using `rules.daysForPlan(plan)`, use `Payment.forMembership` / `forSubscription` / `forSession` factories on normal entry, and use the 10-arg constructor in Option `[2]` when deliberately creating 0-link/2-link exception tests.
**Sketch.**
```java
// Sketch — Option [2] tests Rule 12 XOR guard cleanly
Payment p = new Payment(0, cid, memId, subId, sesId, amount, PaymentMethod.CARD, today, receipt, false);
paymentDao.insert(p); // throws ValidationException if linked targets != 1
```
**How to know it is right.** Unfreezing Member `#4`'s frozen plan (`frozenDays = 10`) shifts `endDate` +10 days and zeroes `frozenDays`; renewing `#5` (`CANCELLED`) throws `ValidationException`.
**Common mistakes.**
- Using `double` for plan prices → always use `BigDecimal`.
- Computing unpaid balance including voided payments → violates rule 6 & 19.
- Allowing direct delete of payments in billing menu → violates rule 19 audit trail.

### `ScheduleWorkoutCliMenu.java` — Interactive screens for Sessions, Workouts & Live HUD

**Status:** done.
**Canonical:** interacts with `SessionDao`, `TrainerAvailabilityDao`, `WorkoutProgramDao`, `EquipmentDao` (`docs/UML.md` §3).
**What it is.** Implements Module `[4]` (Slot Booking -> `CONFIRMED`, Request Outside Slots -> `REQUESTED`, Admin Override Reserve -> rule 1 & 16, and Rule 18 transitions: Confirm / Complete / Mark `NO_SHOW` / Cancel with Rule 10 2-hour notice check) and Module `[5]` (`WorkoutProgram` version history + interactive Terminal Live Workout HUD Simulator).
**Why it is designed this way.** Pairs session booking with workout execution in one class to stay well under 400 lines per module file.
**How to write it.** Display current slot occupancy before prompting for open-slot bookings, prompt `[y/n]` step-by-step per comma-separated exercise block inside `runLiveWorkoutHudSimulator()`, and wrap all actions in `ValidationException`/`DatabaseException` handlers.
**Sketch.**
```java
// Sketch — Step-by-step Terminal HUD loop over comma-separated exercises
String[] blocks = wp.getExercises().split(",");
for (int i = 0; i < blocks.length; i++) {
    int pct = ((i + 1) * 100) / blocks.length;
    System.out.println("  [HUD Block " + (i + 1) + "/" + blocks.length + "] " + blocks[i].trim() + " | " + pct + "%");
}
```
**How to know it is right.** Confirming an already-`CONFIRMED` session or completing a `REQUESTED` session fails loudly with rule 18 `ValidationException`.
**Common mistakes.**
- Calling `split(",")` without guarding `exercises == null` → NPE on empty workout notes → default to `"Warmup 10m"`.
- Passing non-zero price when attaching a `subscriptionId` → violates rule 11 (covered session must cost `0`).
- Cancelling with blank reason → fails rule 10 `ValidationException`.

### `FacilityAdminCliMenu.java` — Interactive screens for Attendance, Equipment, Staff RBAC & Backup/DB Tools

**Status:** done.
**Canonical:** interacts with `AttendanceDao`, `EquipmentDao`, `AdministratorDao`, `SerializationUtil`, `DatabaseConnection` (`docs/UML.md` §3, §6).
**What it is.** Provides four terminal modules: `[6]` Fast Check-In / Check-Out & Daily Report (rule 5 + rule 19 correction-only workflow), `[8]` Equipment CRUD + 4-Zone ASCII Floor Blueprint Map (`CARDIO`, `STRENGTH`, `FREE WEIGHTS`, `RECOVERY`), `[9]` Staff Management gated behind `rules.requireAdmin(session.getCurrentUser())` (rule 16), and `[10]` `.ser` full snapshot Export/Load, deliberate 3-way `DatabaseException` trap tester, and DB Reset & Re-Seed.
**Why it is designed this way.** Groups operational facility/admin utilities cleanly so every infrastructure check (`SerializationUtil`, `DatabaseConnection` failures, RBAC) is one menu click away.
**How to write it.** Gate all `[9]` Staff Admin actions right after navigation input via `rules.requireAdmin(...)`, restore `DatabaseConnection.setDbUrl(savedUrl)` inside `finally` when demonstrating unreachable-DB `DatabaseException`, and guard null EOF reads before constructing `Administrator`.
**Sketch.**
```java
// Sketch — deliberate DatabaseException test restores URL safely
String savedUrl = DatabaseConnection.getDbUrl();
try {
    DatabaseConnection.setDbUrl("jdbc:sqlite:/nonexistent_dir_xyz/bad.db");
    DatabaseConnection.getConnection();
} catch (DatabaseException e) {
    CliTheme.printSuccess("Caught expected unreachable-DB DatabaseException -> " + e.getMessage());
} finally {
    DatabaseConnection.setDbUrl(savedUrl);
}
```
**How to know it is right.** Running Option `[10]` → `[3]` prints three `[OK]` catches (missing `.ser`, null object, bad JDBC URL) and restores `gym.db` cleanly.
**Common mistakes.**
- Leaving the intentionally broken JDBC URL active without `finally` reset → breaks every menu afterward.
- Allowing a logged-in admin to deactivate or delete their own session account → add guard `target.getId() == currentUser.getId()`.
- Calling `Administrator.setPasswordHash` with null on EOF → always null-check `readRawString` returns.

### `TaShowcaseRunner.java` — Automated 22-Scenario Sandbox & Category Picker (`[T]`)

**Status:** done.
**Canonical:** verifies all 13 DAOs, rules 1–19, `ValidationException`, `DatabaseException`, and `.ser` round-trips (`docs/DECISIONS.md` 78).
**What it is.** Self-contained terminal verification suite invoked via `[T]` from the Main Menu. Supports Option `[1]` (automated 22-check suite printing `[PASS]`/`[FAIL]` and `SCOREBOARD: 22 / 22`) and Option `[2]` (individual test category `#1–#11`). Executes strictly inside isolated temporary `gym_ta_showcase.db` and `backups/ta_showcase_snapshot.ser` so live `gym.db` is never touched.
**Why it is designed this way.** Replaces JUnit files under `src/test/` (removed per Decision 77) with a beginner-friendly in-app CMD grading flow callable directly during oral defence.
**How to write it.** Save `previousUrl = DatabaseConnection.getDbUrl()`, call `prepareSandbox()` (pointing `DatabaseConnection` to `jdbc:sqlite:gym_ta_showcase.db` + `SampleDataSeeder.resetAndSeed()`), execute numbered `test01`..`test22` boolean checks, and clean up temp files inside `finally`.
**Sketch.**
```java
// Sketch — sandbox DB prevents test data leaking into gym.db
String previousUrl = DatabaseConnection.getDbUrl();
try {
    prepareSandbox();
    if (test01CustomerValidation()) passed++;
    // ... checks #02 through #22 ...
} finally {
    cleanupSandbox(previousUrl);
}
```
**How to know it is right.** Run `[T]` → `[1]`; output prints `SCOREBOARD: 22 / 22 verification checks PASSED.` and deletes `gym_ta_showcase.db` afterward.
**Common mistakes.**
- Forgetting `cleanupSandbox(previousUrl)` inside `finally` on exception → traps user in sandbox DB.
- Reusing `gym.db` instead of `gym_ta_showcase.db` → destructive tests wipe user edits.
- Asserting outdated calendar strings instead of relative `LocalDate.now()` checks → causes false failures over time.

### `GymCliApp.java` — Terminal Login Gate & Main Menu Entry Point

**Status:** done.
**Canonical:** target of Gradle `runCli` (`build.gradle`) and `run-cli.bat`.
**What it is.** The application entry class (`main(String[] args)`): calls `DatabaseConnection.initializeDatabase()` and `SampleDataSeeder.seedIfEmpty()`, renders the real SQLite Login Gate (including quick-login presets and active-flag enforcement), shows live database KPIs (`Members`, `Coaches`, `Active Plans`, `Today Check-Ins`, `Sessions`, `Revenue`, `Gear Maint`), and routes Modules `[1]`–`[10]`, `[T]`, `[L]`, and `[0]`.
**Why it is designed this way.** Separates login lifecycle and main routing cleanly from individual domain workflows.
**How to write it.** Construct `Scanner`, `CliInput`, `CliSession`, and `GymRulesHelper`; loop login until `exitRequested`; enter `runMainDashboardLoop()` until logout or exit; exit cleanly without `System.exit`.
**Sketch.**
```java
// Sketch — composition root for CMD execution
public static void main(String[] args) {
    GymCliApp app = new GymCliApp();
    app.start();
}
```
**How to know it is right.** Launch via `.\gradlew.bat runCli -q --console=plain` or `run-cli.bat`; signing in as `oldstaff/old123` prints `[! VALIDATION ERROR] Rule 16: Account 'oldstaff' is inactive and cannot log in`.
**Common mistakes.**
- Launching `runCli` without `standardInput = System.in` in `build.gradle` → Gradle swallows keyboard input so `Scanner` receives immediate EOF.
- Calling `System.exit(0)` inside menus → bypasses outer logout/cleanup flow.
- Querying DAOs in `printLiveKpiBanner()` before `DatabaseConnection.initializeDatabase()` runs → `no such table` error.

## 6. How this layer connects to the others

1. `GymCliApp.main` → `DatabaseConnection.initializeDatabase()` → `SampleDataSeeder.seedIfEmpty()`.
2. `GymCliApp` → `GymRulesHelper.authenticate(...)` → `AdministratorDao.findByUsername(...)`.
3. `GymCliApp` → dispatches `*CliMenu` / `TaShowcaseRunner` → `GymRulesHelper` / concrete `*Dao` classes → SQLite `gym.db` (or `gym_ta_showcase.db`) & `SerializationUtil`.

| Direction | Package / Class | Interaction |
|---|---|---|
| **Depends on** | `com.gym.model.*`, `com.gym.dao.*`, `com.gym.util.*`, `com.gym.exception.*` | Domain POJOs, 13 concrete SQLite DAOs, `DatabaseConnection`, `SerializationUtil`, `ValidationException`, `DatabaseException` |
| **Independent of** | `com.gym.controller.*`, `m3fx-core`, `m3fx-controls`, JavaFX | Zero GUI/FX toolkit dependencies; compiles and runs pure headless Java SE + SQLite JDBC |
| **Started by** | `./gradlew runCli -q --console=plain`, `run-cli.bat`, or `GymCliApp.main` | Registered as Gradle `JavaExec` task with `standardInput = System.in` |

## 7. Common mistakes and review checklist

| Symptom | Cause | Fix |
|---|---|---|
| Terminal hangs or reads blank EOF immediately on `runCli` | Missing `standardInput = System.in` in `build.gradle` `runCli` task | Keep `standardInput = System.in` in `build.gradle` (`DECISIONS.md` 78) |
| Em-dash or special symbol shows as `` or `?` in Windows `cmd.exe` | Non-ASCII punctuation inside terminal strings | Use 7-bit ASCII only (`-`, `*`, `[OK]`) in `com.gym.cli` and DAO error messages |
| Gradle `-Xlint:serial` warnings on `GymBackupSnapshot` | Field typed as `List<...>` instead of concrete `ArrayList<...>` | Type every collection field in `GymBackupSnapshot` as `ArrayList<...>` |
| TA Showcase corrupts `gym.db` on crash | Missed `DatabaseConnection.setDbUrl(previousUrl)` in `finally` | Keep sandbox swap and restore inside `try { ... } finally { cleanupSandbox(previousUrl); }` |

Review checklist:

- [ ] Only COMP 301 Lectures 1–6 constructs (no streams, lambdas, `var`, `record`, generic `<T>` helpers, or multi-catch).
- [ ] Pure 7-bit ASCII strings across `com.gym.cli` and `com.gym.dao` messages.
- [ ] Zero JavaFX imports anywhere in `com.gym.cli`.
- [ ] `.\gradlew.bat clean build` compiles cleanly with `-Xlint:all,-processing` and zero warnings in `com.gym.cli` / `com.gym.dao`.
- [ ] `[T]` → `[1]` automated showcase outputs `SCOREBOARD: 22 / 22 verification checks PASSED.` live in CMD.

## 8. Learning exercises (with answers)

1. **Trace `shouldStopSubMenu()`.** Suppose the user is inside `Members > Profile #1 > Medical` and types `H`. Explain step by step which `while` loops terminate and which method resets `returnHome` back to `false`.
2. **Debug `-Xlint:serial`.** Why does `javac` emit a `[serial]` warning if `GymBackupSnapshot` stores `private List<Customer> customers;` instead of `private ArrayList<Customer> customers;`?
3. **Rule 16 vs DAO validation.** In `GymCliApp`, signing in as `oldstaff` / `old123` throws `ValidationException`. Why didn't `AdministratorDao.insert` reject `active == false` when seeding `oldstaff`?
4. **Why a separate sandbox URL?** Explain what happens if `TaShowcaseRunner` runs `SampleDataSeeder.resetAndSeed()` without switching `DatabaseConnection.setDbUrl("jdbc:sqlite:gym_ta_showcase.db")` first.

<details>
<summary>Answers</summary>

1. Pressing `H` calls `session.setReturnHome(true)` and returns from `openMedicalMenu`. The `while (!session.shouldStopSubMenu())` loop in `openMemberProfile` stops immediately, then `MemberCliMenu.openMenu()` stops too, returning to `GymCliApp.runMainDashboardLoop()`, whose first statement resets `session.setReturnHome(false)`.
2. `java.util.List` is an interface and does not extend `java.io.Serializable`, so the compiler warns that a non-transient `Serializable` field could hold a non-serializable implementation at runtime. `ArrayList` implements `Serializable`, removing the warning.
3. Inactive accounts (`active = false`) must still exist in `administrators` for audit history and de-activation management; rule 16 forbids inactive accounts from *logging in*, so `GymRulesHelper.authenticate(...)` rejects `active == false` while keeping the stored row valid.
4. `SampleDataSeeder.resetAndSeed()` deletes every row in all 13 tables before inserting seed records; running it directly on `gym.db` would wipe all records entered interactively before opening `[T]`.

</details>

## 9. Ownership and tasks

| File | Responsibility | Owner | Status |
|---|---|---|---|
| `CliTheme.java`, `CliInput.java`, `CliSession.java`, `GymCliApp.java` | ASCII rendering, Scanner safety, session flags, Login Gate & Dashboard | All (COMP 301 console layer) | Done |
| `GymBackupSnapshot.java`, `SampleDataSeeder.java`, `GymRulesHelper.java` | 13-table `.ser` DTO, sample data, cross-entity Rules 1–19 engine | All | Done |
| `MemberCliMenu.java`, `TrainerCliMenu.java`, `MembershipBillingCliMenu.java`, `ScheduleWorkoutCliMenu.java`, `FacilityAdminCliMenu.java` | Modules `[1]`–`[10]` interactive workflows & Floor Map / Live HUD | All | Done |
| `TaShowcaseRunner.java` | Automated 22-scenario suite + interactive category picker `[T]` | All | Done |

### Definition of done for a file in this folder

- [ ] Uses plain COMP 301 Lectures 1–6 syntax, sequential `catch` blocks, and zero JavaFX imports.
- [ ] Renders strictly in 80-column 7-bit ASCII cleanly inside Windows `cmd.exe` (`run-cli.bat`).
- [ ] Compiles cleanly under `.\gradlew.bat build` with `-Xlint:all,-processing`.
- [ ] Tested live via `.\gradlew.bat runCli -q --console=plain` and `run-cli.bat`.

