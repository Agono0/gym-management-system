# `dao` — Data Access Layer (JavaFX branch)

Canonical design: `docs/UML.md` §3. This file tracks who writes what — it does not restate the
interface methods, so signatures cannot drift from the diagram.

## Purpose

Persistence behind small interfaces (`CustomerDao`, `TrainerDao`, …) with SQLite implementations
(`CustomerDaoImpl`, …). Services and controllers must never contain SQL — everything goes through
DAOs. Common connection handling lives in `util/DbConnection`.

## Rules

- One `XxxDao` interface + one `XxxDaoImpl` per entity. **The DAO name matches its entity name** —
  so it is `ProgressDao` (for `ProgressRecord`), not `ProgressRecordDao`.
- Every method throws the unchecked `DaoException` on `SQLException`. No `SQLException` escapes
  this package.
- DAO impls depend on the `DbConnection` **interface**, never on `SqliteDbConnection` directly, so
  the database can be swapped in one place.
- No JavaFX imports — enforced in review, and required so the `swing` branch can reuse the package.
- Revenue and balance queries must exclude `voided = 1` payments.

## Contents (to be written)

One interface + impl per entity: Customer, Trainer, Membership, WorkoutProgram, Session, Equipment,
Attendance, Payment, ProgressRecord, MedicalProfile, Administrator, TrainerSubscription,
TrainerAvailability.

Plus:

- `DbConnection.java` — interface (`getConnection()`, `initializeSchema()`).
- `SqliteDbConnection.java` — the SQLite implementation; creates `gym.db` and initialises the schema.
- `DaoException.java` — unchecked wrapper for `SQLException`.

### Queries the business rules depend on

These exist in `docs/UML.md` §3 and are easy to forget. Without them the matching rule cannot be
implemented:

| DAO method | Rule it enables |
|---|---|
| `AttendanceDao.findByCustomerAndDay(int, String)` | One check-in record per customer per day. Returns a **list**, so a stray duplicate row cannot be silently dropped. |
| `SessionDao.findByCustomerAndDay(int, String)` | Customer-side double-booking detection |
| `SessionDao.countByAvailability(int, SessionStatus)` | Slot capacity check. Pass `CONFIRMED` so cancelling frees capacity (rule 17) |
| `WorkoutProgramDao.findLatestVersion(int)` | Per-customer program versioning |
| `PaymentDao.revenueBetween(String from, String to)` | Revenue by range, voided excluded |
| `PaymentDao.findByReceipt(String)` | Looking a receipt up again |
| `MembershipDao.findByStatus(MembershipStatus)` | Expiry sweep and frozen memberships |
| `TrainerSubscriptionDao.findActive(int, int, String)` | "Already subscribed to this trainer?" |

`§3` is the **full DAO surface**: a service is expected to use a subset of it. `findById`, `findAll`
and `delete` exist so a future screen does not have to reopen the interface.

### Week 1 table specs for `schema.sql`

`medical_profiles(id, customerId, bloodType, conditions, allergies, medications, injuries,
doctorName, doctorPhone, notes, updatedDate)`,
`administrators(id, fullName, username, passwordHash, role, phone, email, active, createdDate)`,
`trainer_subscriptions(id, customerId, trainerId, startDate, endDate, ratePerPeriod, status)`,
`trainer_availability(id, trainerId, date, startTime, endTime, maxCustomers, status)`.

Payments need `CHECK` constraints, not just columns: at most one of
`membershipId`/`subscriptionId`/`sessionId` may be non-null, and `voided` defaults to 0. Payments and
attendance are **never deleted** (rule 19) — corrections go through `voidPayment`/`adjustPayment` and
`AttendanceService.correctRecord`. No FK cascades anywhere — "delete" means deactivate (rule 4), and
the admin hard delete asks `util/GuardService.countReferences` before it proceeds.

## Tasks

| Task | Owner | Status |
|------|-------|--------|
| Skeleton + this README created | Abdelrhman | Done |
| `DbConnection` + `SqliteDbConnection` + `schema.sql` v1 + DAO base pattern (Week 1, foundation) | Abdelrhman | To Do |
| `CustomerDao` + impl | Abdelrhman | To Do |
| `TrainerDao`, `MembershipDao` + impls | Ziad | To Do |
| `WorkoutProgramDao`, `SessionDao`, `EquipmentDao` + impls | Yousef | To Do |
| `AttendanceDao`, `PaymentDao` + impls | Abdel Raouf | To Do |
| `ProgressDao`, `ChartUtils` support + impls | Abdel Raouf | To Do |
| `MedicalProfileDao`, `AdminDao` + impls | Abdelrhman | To Do |
| `TrainerSubscriptionDao`, `TrainerAvailabilityDao` + impls | Yousef | To Do |

Mirror in the `swing` branch: DAO + service layers should be portable with minimal changes (no
JavaFX imports allowed here — enforced in review). That branch has no Java files yet.
