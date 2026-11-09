# UML — Team version (INTERNAL, not for submission)

> **This file no longer duplicates the class diagrams.** They used to be copied here verbatim with
> colour added, which is how `PlanType`'s accessors drifted from `PlanType` to `String` and how six
> enum notes went missing. The master diagrams now live in **`docs/UML.md` only** — one copy each:
>
> | Diagram | Where | Was also here as |
> |---|---|---|
> | Main domain model | `UML.md` §1 | §2 "Main diagram" |
> | Layered architecture | `UML.md` §2 | §1 |
> | Data access | `UML.md` §3 | §4 |
> | Service layer | `UML.md` §4 (Retired — Decision 77) | §5 |
> | Controllers | `UML.md` §5 | §6 |
> | Utilities & exceptions | `UML.md` §6 | — |
>
> What stays here: **who owns what** (below), and the ownership overlay for slide decks.
> Agreed business rules moved to `UML.md` → "Business rules" so the submitted document states them.

## Ownership legend

blue = Abdelrhman · green = Ziad · amber = Yousef · violet = Abdel Raouf

## Ownership overlay (domain model)

Colour-only view of `UML.md` §1. No members are repeated here, so it cannot drift.

```mermaid
classDiagram
    direction TB
    class MainApp
    class Customer
    class Trainer
    class Membership
    class WorkoutProgram
    class Session
    class Equipment
    class Attendance
    class Payment
    class ProgressRecord
    class MedicalProfile
    class Administrator
    class TrainerSubscription
    class TrainerAvailability
    class CustomerStatus
    class MembershipStatus
    class PlanType
    class SessionStatus
    class SubscriptionStatus
    class AvailabilityStatus
    class StaffRole
    class PaymentMethod
    class EquipmentStatus
    class EquipmentCondition
    class ProgressMetric
    %% Mermaid lays a relation-less diagram out in a single row, which rendered this overlay as an
    %% unreadable 4090x99 strip. `~~` is NOT an invisible edge in Mermaid — it creates no link at all,
    %% so the stack needs real associations to rank the nodes vertically. Mermaid class diagrams have
    %% no `linkStyle`, so the arrows cannot be dimmed; read the fill colours as the content here, not
    %% the edges. Adding a class without adding it to this chain pushes it back out to the side.
    MainApp --> Customer
    Customer --> Trainer
    Trainer --> Membership
    Membership --> WorkoutProgram
    WorkoutProgram --> Session
    Session --> Equipment
    Equipment --> Attendance
    Attendance --> Payment
    Payment --> ProgressRecord
    ProgressRecord --> MedicalProfile
    MedicalProfile --> Administrator
    Administrator --> TrainerSubscription
    TrainerSubscription --> TrainerAvailability
    TrainerAvailability --> CustomerStatus
    CustomerStatus --> MembershipStatus
    MembershipStatus --> PlanType
    PlanType --> SessionStatus
    SessionStatus --> SubscriptionStatus
    SubscriptionStatus --> AvailabilityStatus
    AvailabilityStatus --> StaffRole
    StaffRole --> PaymentMethod
    PaymentMethod --> EquipmentStatus
    EquipmentStatus --> EquipmentCondition
    EquipmentCondition --> ProgressMetric
    classDef abdel fill:#dbeafe,stroke:#93c5fd
    classDef ziad fill:#dcfce7,stroke:#86efac
    classDef yousef fill:#fef3c7,stroke:#fcd34d
    classDef raouf fill:#ede9fe,stroke:#c4b5fd
    class MainApp:::abdel
    class Customer:::abdel
    class MedicalProfile:::abdel
    class Administrator:::abdel
    class CustomerStatus:::abdel
    class MembershipStatus:::ziad
    class PlanType:::ziad
    class Trainer:::ziad
    class Membership:::ziad
    class WorkoutProgram:::yousef
    class Session:::yousef
    class Equipment:::yousef
    class SessionStatus:::yousef
    class SubscriptionStatus:::yousef
    class AvailabilityStatus:::yousef
    class TrainerSubscription:::yousef
    class TrainerAvailability:::yousef
    class Attendance:::raouf
    class Payment:::raouf
    class ProgressRecord:::raouf
    class StaffRole:::raouf
    class PaymentMethod:::raouf
    class EquipmentStatus:::raouf
    class EquipmentCondition:::raouf
    class ProgressMetric:::raouf
```

Shared enumerations (`CustomerStatus` with Abdelrhman, `StaffRole`/`PaymentMethod`/
`EquipmentStatus`/`EquipmentCondition` with Abdel Raouf) are owned by whoever first needs them;
add a literal only after telling the other three.

## Layer ownership

| Layer | Owner | Notes |
|---|---|---|
| `util/DatabaseConnection`, `util/SerializationUtil`, `exception/*` (`GymException`, `ValidationException`, `DatabaseException`) | Abdelrhman | Foundation — SQLite DDL bootstrap, binary serialization, checked exceptions (`Done`) |
| `model`, `dao` for each area (service tier retired per Decision 77) | see the checklist below | One owner per vertical domain slice (`Done` in Milestone 2) |
| `controller` (19 Java-built screens + `LaunchGateController`) | see the checklist below | UI screens built in Java (`Screen`/`ScreenController`); wired to DAOs in Milestone 3 |
| `MainApp` / `GymApp` shells + m3fx wiring | Abdelrhman | m3fx library + both launchers built |

## Who does what (checklist)

| Area | Owner | Status |
|---|---|---|
| Foundation: `DatabaseConnection`, `SerializationUtil`, `GymException`/`ValidationException`/`DatabaseException`, `MainApp`/`GymApp` shells, m3fx library | Abdelrhman | Done (Milestone 2) |
| `Customer` + `MedicalProfile` + `Administrator` + enums + DAOs | Abdelrhman | Done (Milestone 2) |
| `Trainer` + `Membership` + `PlanType`/`MembershipStatus` enums + DAOs | Ziad | Done (Milestone 2) |
| `WorkoutProgram`, `Session`, `Equipment`, `TrainerSubscription`, `TrainerAvailability` + enums + DAOs | Yousef | Done (Milestone 2) |
| `Attendance`, `Payment`, `ProgressRecord` + enums + DAOs | Abdel Raouf | Done (Milestone 2) |
| 19 Java-built UI screens & 20 controllers (`Screen` registry, demo data) | All (vertical slices) | Done (UI Demo) |
| Service abstraction layer (`com.gym.service`) | All | Retired (`DECISIONS.md` 77) |
| Milestone 3: wire UI screens/controllers directly to the concrete DAOs & `SerializationUtil` | Each screen owner | To do (Next milestone) |

Rules: JavaFX-first per vertical slice, then each owner ports their own modules to the `swing`
branch. Update the Status column as work moves. `swing` currently has **no `.java` files** — the
port has not started, so the branch README claim "keep entities identical" is still untested.
Note (`DECISIONS.md` 77): app-level JUnit tests under `src/test/` were removed; unit testing stays
on the `m3fx-core` and `m3fx-controls` modules.

## Open business-rule decisions

**Resolved — all sixteen now live in `docs/UML.md` → "Business rules".** Do not re-decide them here;
change them there so the submitted document and the team stay in sync. The section below is kept
only as a pointer so anyone with the old link lands somewhere useful.

| Was | Now |
|---|---|
| `book` vs `requestSession` vs `reserveSession` | `UML.md` rule 1 |
| `SessionStatus.NO_SHOW` | rule 2 |
| `Customer *-- MedicalProfile` mandatory? | rule 3 |
| Meaning of "delete" | rule 4 |
| `checkIn` requires ACTIVE membership? | rule 5 |
| `balance(int)` definition | rule 6 |
| Does `freeze` pause `endDate`? | rule 7 |
| Several slots per day or one window? | rule 8 |
| Date/time format standard | rule 9 |
| Session cancellation window | rule 10 |
| Covered session pricing | rule 11 |
| Payment link XOR | rule 12 |
| Derived enum literals | rule 13 |
| Suspended customer vs frozen membership | rule 14 |
| Which trainer link is authoritative | rule 15 |
| Role enforcement | rule 16 |

Two rules changed behaviour relative to the old table, and both are now in `UML.md`:

- **Rule 3** accepted `0..1` for `MedicalProfile` (it was already drawn `*-- "1"` in the diagram).
- **Rule 8** resolves the "daily cap vs per-slot cap" contradiction: `maxCustomers` is per slot, and
  `setDailyCap` applies one value across a day's slots. `TrainerAvailabilityDao.findByTrainerAndDate`
  returns a **list**, not a single row.
