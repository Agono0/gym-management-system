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
| `CustomerService.java` | Customer validation, search/filter, status rules, list by assigned trainer | Abdelrhman |
| `TrainerService.java` | Slot publishing and per-slot caps, close slot, set rate, assign/reassign customers, revoke medical access | Ziad |
| `MembershipService.java` | Sell/renew/freeze/unfreeze/cancel, overdue expiry sweep, expiry reminders. Freeze stores `frozenDays`; unfreeze shifts `endDate` forward by it. | Ziad |
| `WorkoutService.java` | Program building rules, reassignment to a customer, versioning per customer | Yousef |
| `SchedulingService.java` | `bookOpenSlot` (needs OPEN slot + free capacity + no trainer or customer conflict), `requestOutsideSlots`, `adminReserve`, `detectConflicts`, the session state machine (`canTransition`), confirm/cancel-with-reason/complete/no-show, subscription lifecycle and pricing. Membership (gym access) and subscription (trainer engagement) are distinct concepts. | Yousef |
| `EquipmentService.java` | Add/edit inventory, status and condition transitions, last-maintenance date | Yousef |
| `AttendanceService.java` | Check-in/out rules, eligibility checks (ACTIVE customer + ACTIVE non-expired membership), daily report data | Abdel Raouf |
| `PaymentService.java` | Recording, XOR link validation (`validLink`), voiding, balances, receipt lookup, revenue by range. Voided payments never count. | Abdel Raouf |
| `ProgressService.java` | Metric access and chart series per customer | Abdel Raouf |
| `MedicalProfileService.java` | Medical CRUD, lookup by customer, assigned-trainer access check (`getForTrainer`) | Abdelrhman |
| `AdminService.java` | Authenticate (rejects `active == false`), password reset, staff management by role, reassign/adjust/void overrides, admin-only hard deletes that refuse while references exist | Abdelrhman |

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
