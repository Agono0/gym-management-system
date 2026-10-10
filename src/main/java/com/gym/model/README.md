# `model` — Entity Classes (JavaFX branch)

## Purpose
Plain Java POJOs representing every domain entity. No JavaFX, no SQL, no
business logic here — just fields, constructors, getters/setters,
`equals`/`hashCode`/`toString`.

## Contents (to be written)
| Class | Description | Owner |
|-------|-------------|-------|
| `Customer.java` | Personal info, contact, status, assigned trainer (nullable trainerId); links to membership, trainer, program | Abdelrhman |
| `Trainer.java` | Personal + professional info, specialization, experience, default rate; bookable slots live in `TrainerAvailability` | Ziad |
| `Membership.java` | Plan (`PlanType`: monthly/quarterly/yearly), price, start/end dates, status (active/frozen/expired/cancelled) | Ziad |
| `PlanType.java` | Enum: MONTHLY, QUARTERLY, YEARLY (carries plan price + duration) | Ziad |
| `WorkoutProgram.java` | Exercise list (sets/reps/rest), version, creator trainer | Yousef |
| `Session.java` | Scheduled session: customer, trainer, time slot, subscription link, price, cancel reason, notes | Yousef |
| `Equipment.java` | Inventory item + condition status | Yousef |
| `Attendance.java` | Check-in/out record per customer per day | Abdel Raouf |
| `Payment.java` | Amount, method, date, voided flag; links to at most one of membership / trainer subscription / session (nullable ids) | Abdel Raouf |
| `ProgressRecord.java` | Body metrics with date (weight, body fat, measurements; benchmarks in notes) | Abdel Raouf |
| `MedicalProfile.java` | Blood type, conditions, allergies, medications, injuries, doctor, notes | Abdelrhman |
| `Administrator.java` | System operator: login identity, role (ADMIN/MANAGER/STAFF), active flag | Abdelrhman |
| `TrainerSubscription.java` | Customer sticks with trainer for a period at a flat trainer-set rate | Yousef |
| `TrainerAvailability.java` | Trainer-controlled open days, daily customer caps, hourly slots | Yousef |

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

Keep entities identical in the `swing` branch (copy, don't redesign).
