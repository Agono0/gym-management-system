# `service` — Business Logic (JavaFX branch)

## Purpose
Validation and business rules between controllers and DAOs: membership expiry
calculation, session conflict detection, payment balance math, progress
aggregation for charts. No JavaFX and no SQL here — pure Java, fully unit
testable.

## Contents (to be written)
| Class | Responsibility | Owner |
|-------|----------------|-------|
| `CustomerService.java` | Customer validation, search/filter, status rules | Abdelrhman |
| `TrainerService.java` | Availability rules, assign/reassign customers | Ziad |
| `MembershipService.java` | Sell/renew/freeze/cancel, expiry reminders | Ziad |
| `WorkoutService.java` | Program building rules, versioning per customer | Yousef |
| `SchedulingService.java` | Booking + trainer conflict detection | Yousef |
| `EquipmentService.java` | Status transitions, maintenance log rules | Yousef |
| `AttendanceService.java` | Check-in/out rules, daily report data | Abdel Raouf |
| `PaymentService.java` | Balances, receipts data, revenue summary | Abdel Raouf |
| `ProgressService.java` | Metric aggregation for charts | Abdel Raouf |

## Tasks
| Task | Owner | Status |
|------|-------|--------|
| Skeleton + this README created | Abdelrhman | Done |
| Service per owned feature (see table) | Each owner | To Do |
| Error handling per owned service: validate inputs, friendly messages for every failure | Each owner | To Do |

Like `dao`, keep this layer UI-free so the `swing` branch reuses the design.
