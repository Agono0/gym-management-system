# `dao` — Data Access Layer (JavaFX branch)

## Purpose
Persistence behind small interfaces (`CustomerDao`, `TrainerDao`, …) with
SQLite implementations (`CustomerDaoImpl`, …). Services and controllers must
never contain SQL — everything goes through DAOs. Common connection handling
lives in `util/DbConnection`.

## Contents (to be written)
- One `XxxDao` interface + one `XxxDaoImpl` (SQLite/JDBC) per entity:
  Customer, Trainer, Membership, WorkoutProgram, Session, Equipment,
  Attendance, Payment, ProgressRecord, MedicalProfile, Administrator,
  TrainerSubscription, TrainerAvailability.
- `DaoException.java` (unchecked wrapper for `SQLException`).
- Week 1 table specs for `schema.sql`:
  `medical_profiles(id, customerId, bloodType, conditions, allergies, medications,
  injuries, doctorName, doctorPhone, notes, updatedDate)`,
  `administrators(id, fullName, username, passwordHash, role, phone, email, active,
  createdDate)`,
  `trainer_subscriptions(id, customerId, trainerId, startDate, endDate, rate, status)`,
  `trainer_availability(id, trainerId, date, startTime, endTime, maxCustomers, status)`.

## Tasks
| Task | Owner | Status |
|------|-------|--------|
| Skeleton + this README created | Abdelrhman | Done |
| `DbConnection` + `schema.sql` v1 + DAO base pattern (Week 1, foundation) | Abdelrhman | To Do |
| `CustomerDao` + impl | Abdelrhman | To Do |
| `TrainerDao`, `MembershipDao` + impls | Ziad | To Do |
| `WorkoutProgramDao`, `SessionDao`, `EquipmentDao` + impls | Yousef | To Do |
| `AttendanceDao`, `PaymentDao`, `ProgressRecordDao` + impls | Abdel Raouf | To Do |
| `MedicalProfileDao`, `AdminDao` + impls | Abdelrhman | To Do |
| `TrainerSubscriptionDao`, `TrainerAvailabilityDao` + impls | Yousef | To Do |

Mirror in the `swing` branch: DAO + service layers should be portable with
minimal changes (no JavaFX imports allowed here — enforced in review).
