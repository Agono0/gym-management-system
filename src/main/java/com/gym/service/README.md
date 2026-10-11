# `service` — Business Logic (JavaFX branch)

Canonical design: `docs/UML.md` §4, with the agreed rules in `docs/UML.md` → "Business rules".
This file tracks ownership and responsibilities — it does not restate method signatures.

## Purpose

Validation and business rules between controllers and DAOs: membership expiry calculation, session
conflict detection, payment balance math, progress aggregation for charts. **No JavaFX and no SQL
here** — pure Java, fully unit testable.

## Rules

- Services never import JavaFX. A service returns data or throws; the *controller* renders the
  message through `FxUtils`. There is no `FxUtils` dependency in this layer.
- Services never format a user-facing string. `PaymentService.revenueBetween` returns
  `BigDecimal`; formatting is the controller's or `ChartUtils`' job.
- Money is `BigDecimal`. Never `double`.
- A service that needs another aggregate's data declares that DAO dependency explicitly — see the
  dependency list in `docs/UML.md` §4. Silently reaching into a table another service owns
  (e.g. `TrainerService` writing `MedicalProfile` directly instead of calling
  `MedicalProfileService.getForTrainer`) is a review failure.

## Contents (to be written)

| Class | Responsibility | Owner |
|-------|----------------|-------|
| `CustomerService.java` | Customer validation, search/filter, status rules, list by assigned trainer. **No hard delete** — rule 4 keeps that on `AdminService`. `changeStatus(SUSPENDED)` also freezes the customer's ACTIVE memberships (rule 14). | Abdelrhman |
| `TrainerService.java` | Slot publishing (validates `startTime < endTime`) and per-slot caps, close slot, set rate, assign/reassign customers. `setSlotCap` overrides one slot; `setDailyCap` re-applies across a day and so wins regardless of call order. | Ziad |
| `MembershipService.java` | Sell/renew/freeze/unfreeze/cancel, overdue expiry sweep, expiry reminders, and `freezeForSuspended` as the rule-14 hook. Freeze stores `frozenDays`; unfreeze shifts `endDate` forward by it. Only ACTIVE may freeze; renewing EXPIRED extends, renewing CANCELLED refuses. | Ziad |
| `WorkoutService.java` | Program building rules, reassignment to a customer, versioning per customer. There is no in-place edit of an issued program — `newVersion` supersedes it; `update`/`delete` are for correcting mistakes before issue. | Yousef |
| `SchedulingService.java` | `bookOpenSlot` (needs OPEN slot + free capacity + no trainer or customer conflict), `requestOutsideSlots`, `adminReserve` (ADMIN only), `detectConflicts`, the session state machine (`canTransition` — rule 18), confirm/cancel-with-reason/complete/no-show, subscription lifecycle and pricing. `bookedCount` counts CONFIRMED only (rule 17). Membership (gym access) and subscription (trainer engagement) are distinct concepts. | Yousef |
| `EquipmentService.java` | Add/edit inventory, status and condition transitions, last-maintenance date, delete | Yousef |
| `AttendanceService.java` | Check-in/out rules, eligibility checks (ACTIVE customer + ACTIVE non-expired membership), daily report data, and `correctRecord` for fixing a mis-keyed time. Attendance rows are never deleted (rule 19). | Abdel Raouf |
| `PaymentService.java` | Recording, XOR link validation (`validLink`, public so it is unit-testable), voiding, balances, receipt lookup, revenue by range. Voided payments never count, and payments are never deleted (rule 19) — money is corrected by voiding or by `AdminService.adjustPayment`. | Abdel Raouf |
| `ProgressService.java` | Metric access and chart series per customer, keyed by `ProgressMetric` | Abdel Raouf |
| `MedicalProfileService.java` | Medical CRUD, lookup by customer, assigned-trainer access check (`getForTrainer`) — the only sanctioned read path for a trainer | Abdelrhman |
| `AdminService.java` | Authenticate (rejects `active == false`, returns null), password reset, staff CRUD by role, reassign/adjust overrides, admin-only hard deletes that ask `GuardService.countReferences` first and refuse while references exist | Abdelrhman |

### Cross-service rules worth knowing before you code

- **A service never imports JavaFX and never renders a message.** Return data or throw; the
  controller calls `FxUtils.error(...)`.
- **A service only touches DAOs it declares.** If you need another aggregate's data, call its service
  — `TrainerService` reads medical data through `MedicalProfileService.getForTrainer`, not through
  `MedicalProfileDao`.
- **Rule 18 holds the state machine.** Only `CONFIRMED` may become `COMPLETED`/`NO_SHOW`/`CANCELLED`;
  only `REQUESTED` may become `CONFIRMED`. Every transition calls `canTransition` first.
- **Overrides need ADMIN** — that includes `SchedulingService.adminReserve`, not just `AdminService`
  (rule 16).

## Error handling

Every service validates its inputs and throws a domain exception rather than letting a
`DaoException` reach the UI as a stack trace. `CustomerService.validate(Customer)` returns a
`List<String>` of messages; the single-object operations (membership sell/renew/freeze, session
booking, payment recording, attendance check-in) throw `IllegalStateException`/`IllegalArgumentException`
with a message that is safe to show. The controller catches and calls `FxUtils.error(...)`. Each
owner covers their own service.

## Tasks

| Task | Owner | Status |
|------|-------|--------|
| Skeleton + this README created | Abdelrhman | Done |
| Service per owned feature (see table) | Each owner | To Do |
| Error handling per owned service: validate inputs, friendly messages for every failure | Each owner | To Do |
| Unit tests per service (no JavaFX, no database) | Each owner | To Do |

Like `dao`, keep this layer UI-free so the `swing` branch reuses the design. That branch has no
Java files yet.
