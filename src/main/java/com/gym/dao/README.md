# `dao` — Data Access Layer (Swing branch)

## Purpose
Persistence behind the same DAO interfaces as `main` (`CustomerDao`,
`TrainerDao`, …) with SQLite implementations. Ported from the `main`
branch — no Swing imports allowed here.

## Contents (ported from `main`)
- One `XxxDao` interface + `XxxDaoImpl` per entity, plus `DaoException`.

## Tasks
| Task | Owner | Status |
|------|-------|--------|
| Skeleton + this README created | Abdelrhman | Done |
| Port `CustomerDao` + impl | Abdelrhman | To Do |
| Port `TrainerDao`, `MembershipDao` + impls | Ziad | To Do |
| Port `WorkoutProgramDao`, `SessionDao`, `EquipmentDao` + impls | Yousef | To Do |
| Port `AttendanceDao`, `PaymentDao`, `ProgressRecordDao` + impls | Abdel Raouf | To Do |
