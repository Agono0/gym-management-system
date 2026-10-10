# `model` — Entity Classes (JavaFX branch)

Canonical design: `docs/UML.md` §1. This file tracks who writes what — it does not restate the
diagram, so field lists cannot drift from it.

## Purpose

Plain Java POJOs representing every domain entity. No JavaFX, no SQL, no business logic here — just
fields, constructors, getters/setters, `equals`/`hashCode`/`toString`.

Conventions that apply to every entity (see `docs/UML.md` header):

- Money is `BigDecimal`, never `double`.
- Boxed `Integer`/`Boolean` marks a nullable column; primitives mark NOT NULL.
- Dates are ISO-8601 strings: `yyyy-MM-dd` for dates, `HH:mm` for times, `yyyy-MM-dd HH:mm` for
  date-times. `DateUtils` owns every parse and format.
- `m3fx` is a UI dependency and must never be imported here.

## Contents (to be written)

| Class | Description | Owner |
|-------|-------------|-------|
| `Customer.java` | Personal info, contact, status, assigned trainer (nullable `trainerId`); links to membership, trainer, program | Abdelrhman |
| `Trainer.java` | Personal + professional info, specialization, experience, `hourlyRate`; bookable slots live in `TrainerAvailability` | Ziad |
| `Membership.java` | Plan (`PlanType`: monthly/quarterly/yearly), `price`, start/end dates, status, `frozenDays` (how long the plan is currently paused) | Ziad |
| `PlanType.java` | Enum: MONTHLY, QUARTERLY, YEARLY (carries plan price + duration) | Ziad |
| `WorkoutProgram.java` | Exercise list (sets/reps/rest), `version`, creator trainer | Yousef |
| `Session.java` | Scheduled session: customer, trainer, `date` + `startTime`, `durationMin`, slot link (`availabilityId`, nullable), subscription link (nullable), price, cancel reason, notes. `price` must be 0 when covered by a subscription. | Yousef |
| `Equipment.java` | Standalone inventory item + condition + status + last maintenance. No foreign key to any other entity. | Yousef |
| `Attendance.java` | One check-in/out record per customer per day. `checkOut` is nullable (still inside). | Abdel Raouf |
| `Payment.java` | `amount` (`BigDecimal`), method, date, `receiptNo`, `voided`; **exactly one** of membership / trainer subscription / session is set (XOR). Build it with the `forMembership` / `forSubscription` / `forSession` factories, not the positional constructor. | Abdel Raouf |
| `ProgressRecord.java` | Body metrics with date (weight, body fat, measurements, `targetWeightKg` benchmark, notes) | Abdel Raouf |
| `MedicalProfile.java` | Blood type, conditions, allergies, medications, injuries, doctor, notes. At most one per customer, created on demand. | Abdelrhman |
| `Administrator.java` | System operator: login identity, role (ADMIN/MANAGER/STAFF), `active` flag. Use primitive `boolean active`. | Abdelrhman |
| `TrainerSubscription.java` | Customer sticks with trainer for a period at a flat `ratePerPeriod` | Yousef |
| `TrainerAvailability.java` | **One hourly slot per row**: date, start/end time, `maxCustomers` (cap for *this slot*), status. A trainer may publish several per day. | Yousef |

### Enumerations

`CustomerStatus`, `MembershipStatus`, `PlanType`, `SessionStatus` (includes `NO_SHOW`),
`SubscriptionStatus`, `AvailabilityStatus`, `StaffRole`, `PaymentMethod`, `EquipmentStatus`,
`EquipmentCondition`.

`AvailabilityStatus.FULL`, `MembershipStatus.EXPIRED` and `SubscriptionStatus.EXPIRED` are
**derived** — recomputed on read, never set by hand. Only `OPEN`/`CLOSED`, `FROZEN`, and the
`CANCELLED` literals are assigned explicitly.

## Tasks

| Task | Owner | Status |
|------|-------|--------|
| Skeleton + this README created | Abdelrhman | Done |
| Write `Customer` | Abdelrhman | To Do |
| Write `Trainer`, `PlanType` | Ziad | To Do |
| Write `Membership` | Ziad | To Do |
| Write `WorkoutProgram`, `Session`, `Equipment` | Yousef | To Do |
| Write `Attendance`, `Payment`, `ProgressRecord` | Abdel Raouf | To Do |
| Write `MedicalProfile`, `Administrator` | Abdelrhman | To Do |
| Write `TrainerSubscription`, `TrainerAvailability` | Yousef | To Do |
| Write the 10 enumeration types | each owner, as needed | To Do |

Keep entities identical in the `swing` branch (copy, don't redesign). That branch has no Java files
yet, so the copy is still pending for every row above.
