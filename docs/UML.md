# UML — Gym Management System (JavaFX)

> **Status: design specification, not a description of shipped code.** This document is the agreed
> target design. As of this revision the repository contains the `m3fx` library, a component
> showcase (`fxml/main.fxml`, no controller) and `com.gym.MainApp` — the classes below are *planned*.
> See `docs/COVERAGE.md` for what is actually built.
>
> **Two domain diagrams, then five per-layer zoom-ins** (architecture, data access, services,
> controllers, utilities). Each class is drawn exactly once in full; the zoom-ins show layer members
> and wiring. `docs/UML_TEAM.md` holds ownership, not a second copy of the diagrams.
>
> **Structural diagrams carry no prose.** Mermaid lays `note for` blocks and unconnected classes out
> *beside* the graph rather than inside it, so descriptions widen a diagram instead of annotating it.
> Every descriptive note was therefore moved out of the drawings into §1b, which is the only diagram
> that still carries them. Effect on §1: **13193 × 3200 → 6309 × 3000 units**, and on §6,
> 12203 × 596 → 706 × 348. The 11 enumerations have their own diagram for the same reason — 11
> unconnected boxes cost roughly 1500 units of width. The rules those notes stated are unchanged and
> still stated in the "Business rules" table; nothing was dropped, only relocated.
>
> Notation:
> `-` private · `+` public · `#` protected · `~` package ·
> `-->` association · `o--` shared aggregation · `*--` composition ·
> `..>` dependency · `<|..` realization (drawn from the implementer side) ·
> `--|>` generalization (drawn from the subclass side).
>
> Symbols declared but deliberately unused, so their absence is not read as an omission:
> `#` and `~` (no inheritance and no same-package-only collaborator exists), `o--` and `*--`
> (no part in this model is lifetime-bound to its whole — see the note on `Equipment` in §1 and
> the reasoning under "Notes on scope"), and role names (Mermaid cannot place one at a single
> association end; the one role this model needs is stated in the `Payment` note).
>
> Diagram-wide conventions:
> - **Arrow direction:** the class that *holds the foreign key* is written on the **left**, and the
>   arrowhead lands on the referenced class on the right. `A --> B` draws the arrowhead at **B**.
>   So `Customer "0..*" --> "0..1" Trainer` means `Customer.trainerId` references `Trainer` —
>   correct, because `Customer` is the class holding the FK and it sits on the left.
> - **Label voice:** every association name is passive, read as "*{left}* *{label}* *{right}*".
>   `Membership --> Customer : is held by` reads "Membership is held by Customer". This voice is
>   applied to all 18 domain associations so direction and wording agree everywhere.
> - **Role names:** Mermaid cannot place a role name at one association end — the `"..."` slots
>   either side of the operator are *multiplicities only*, and putting a role there parses as a
>   multiplicity and breaks the diagram. Roles this model needs (`payer`) are stated in the
>   class notes instead.
> - **Boxed `Integer`/`Long`/`Boolean` marks a nullable column; primitives mark NOT NULL.**
> - **Money is `BigDecimal`** — never `double`.
> - **Dates are ISO-8601 strings**: `yyyy-MM-dd` for dates, `HH:mm` for times,
>   `yyyy-MM-dd HH:mm` for date-times. ISO-8601 sorts lexicographically in the same order as
>   chronologically, which is what lets `findByDay`, `findExpiring` and `revenueBetween` compare
>   strings directly. `DateUtils` owns every parse and format.
> - Entity getters and setters are omitted to keep the diagram small — every data member marked
>   «get/set» has a public getter and setter. **`«get/set»` is a project shorthand, not a UML
>   stereotype**: it expands to "this private field has a public `getX()` and `setX()`", so the 113
>   marked fields stand for 226 elided accessors. Enum `values()`/`valueOf(...)` are implicit in
>   Java and are not shown.
> - `m3fx` is an external library dependency, not project code.
>
> **Rendering note — what Mermaid can and cannot draw.** These diagrams are Mermaid, verified
> vertical with `direction TB` under both Mermaid 11 layouts (ELK and dagre), which render the same
> way because TB is also the default. Verified adornment support:
>
> | UML adornment | Mermaid syntax | Renders? |
> |---|---|---|
> | Visibility `+ - # ~` | prefix on the member | yes |
> | Stereotype (e.g. `«interface»`, `«enumeration»`, `«utility»`, `«description»`) | `<<interface>>` | yes, as «interface» |
> | Realization (dashed line + hollow triangle) | `<\|..` | yes |
> | Dependency (dashed line + open arrow) | `..>` | yes |
> | Generalization (solid line + hollow triangle) | `<\|--` | yes |
> | Aggregation / composition diamond | `o--` / `*--` | yes |
> | Multiplicity and association name | `"1" --> "0..*" : label` | yes |
> | Static member (underlined) | `$` suffix | **no — stripped at render** |
> | Abstract member (italic) | `*` suffix | **no — stripped at render** |
>
> The last two are Mermaid limitations, not omissions in this model. The `$` is kept in the source
> because it is the standard Mermaid marker and is visible in the raw text, and every stateless
> helper is additionally stereotyped `«utility»` so its static nature shows on the rendered
> diagram. Nothing in this model is abstract — every operation has a concrete implementation — so
> the stripped `*` costs nothing. The model's two generalizations to Java platform types are drawn
> in §7.

## 1. Main diagram (domain model)

```mermaid
classDiagram
    direction TB
    class Customer {
        -int id «get/set»
        -String fullName «get/set»
        -String phone «get/set»
        -String email «get/set»
        -String address «get/set»
        -String dateOfBirth «get/set»
        -String gender «get/set»
        -String emergencyName «get/set»
        -String emergencyPhone «get/set»
        -CustomerStatus status «get/set»
        -String joinDate «get/set»
        -Integer trainerId «get/set»
        +Customer()
        +Customer(int, String, String, String, String, String, String, String, String, CustomerStatus, String, Integer)
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Trainer {
        -int id «get/set»
        -String fullName «get/set»
        -String phone «get/set»
        -String email «get/set»
        -String specialization «get/set»
        -int experienceYears «get/set»
        -String certifications «get/set»
        -String hireDate «get/set»
        -BigDecimal hourlyRate «get/set»
        +Trainer()
        +Trainer(int, String, String, String, String, int, String, String, BigDecimal)
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Membership {
        -int id «get/set»
        -int customerId «get/set»
        -PlanType plan «get/set»
        -BigDecimal price «get/set»
        -String startDate «get/set»
        -String endDate «get/set»
        -MembershipStatus status «get/set»
        -int frozenDays «get/set»
        +Membership()
        +Membership(int, int, PlanType, BigDecimal, String, String, MembershipStatus, int)
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class WorkoutProgram {
        -int id «get/set»
        -int customerId «get/set»
        -int creatorTrainerId «get/set»
        -String name «get/set»
        -String exercises «get/set»
        -int version «get/set»
        -String createdDate «get/set»
        +WorkoutProgram()
        +WorkoutProgram(int, int, int, String, String, int, String)
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Session {
        -int id «get/set»
        -int customerId «get/set»
        -int trainerId «get/set»
        -String date «get/set»
        -String startTime «get/set»
        -int durationMin «get/set»
        -SessionStatus status «get/set»
        -Integer availabilityId «get/set»
        -Integer subscriptionId «get/set»
        -BigDecimal price «get/set»
        -String cancelReason «get/set»
        -String sessionNotes «get/set»
        +Session()
        +Session(int, int, int, String, String, int, SessionStatus, Integer, Integer, BigDecimal, String, String)
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Equipment {
        -int id «get/set»
        -String name «get/set»
        -String category «get/set»
        -String purchaseDate «get/set»
        -EquipmentCondition condition «get/set»
        -EquipmentStatus status «get/set»
        -String lastMaintenance «get/set»
        +Equipment()
        +Equipment(int, String, String, String, EquipmentCondition, EquipmentStatus, String)
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Attendance {
        -int id «get/set»
        -int customerId «get/set»
        -String date «get/set»
        -String checkIn «get/set»
        -String checkOut «get/set»
        +Attendance()
        +Attendance(int, int, String, String, String)
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Payment {
        -int id «get/set»
        -int customerId «get/set»
        -Integer membershipId «get/set»
        -Integer subscriptionId «get/set»
        -Integer sessionId «get/set»
        -BigDecimal amount «get/set»
        -PaymentMethod method «get/set»
        -String date «get/set»
        -String receiptNo «get/set»
        -boolean voided «get/set»
        -String voidReason «get/set»
        -String voidedDate «get/set»
        -BigDecimal previousAmount «get/set»
        +Payment()
        +Payment(int, int, Integer, Integer, Integer, BigDecimal, PaymentMethod, String, String, boolean)
        +forMembership(int customerId, int membershipId, BigDecimal amount, PaymentMethod method, String date, String receiptNo) Payment
        +forSubscription(int customerId, int subscriptionId, BigDecimal amount, PaymentMethod method, String date, String receiptNo) Payment
        +forSession(int customerId, int sessionId, BigDecimal amount, PaymentMethod method, String date, String receiptNo) Payment
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class ProgressRecord {
        -int id «get/set»
        -int customerId «get/set»
        -String date «get/set»
        -double weightKg «get/set»
        -double bodyFatPct «get/set»
        -String measurements «get/set»
        -Double targetWeightKg «get/set»
        -String notes «get/set»
        +ProgressRecord()
        +ProgressRecord(int, int, String, double, double, String, Double, String)
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class MedicalProfile {
        -int id «get/set»
        -int customerId «get/set»
        -String bloodType «get/set»
        -String conditions «get/set»
        -String allergies «get/set»
        -String medications «get/set»
        -String injuries «get/set»
        -String doctorName «get/set»
        -String doctorPhone «get/set»
        -String notes «get/set»
        -String updatedDate «get/set»
        +MedicalProfile()
        +MedicalProfile(int, int, String, String, String, String, String, String, String, String, String)
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Administrator {
        -int id «get/set»
        -String fullName «get/set»
        -String username «get/set»
        -String passwordHash «get/set»
        -StaffRole role «get/set»
        -String phone «get/set»
        -String email «get/set»
        -boolean active «get/set»
        -String createdDate «get/set»
        +Administrator()
        +Administrator(int, String, String, String, StaffRole, String, String, boolean, String)
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class TrainerSubscription {
        -int id «get/set»
        -int customerId «get/set»
        -int trainerId «get/set»
        -String startDate «get/set»
        -String endDate «get/set»
        -BigDecimal ratePerPeriod «get/set»
        -SubscriptionStatus status «get/set»
        +TrainerSubscription()
        +TrainerSubscription(int, int, int, String, String, BigDecimal, SubscriptionStatus)
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class TrainerAvailability {
        -int id «get/set»
        -int trainerId «get/set»
        -String date «get/set»
        -String startTime «get/set»
        -String endTime «get/set»
        -int maxCustomers «get/set»
        -AvailabilityStatus status «get/set»
        +TrainerAvailability()
        +TrainerAvailability(int, int, String, String, String, int, AvailabilityStatus)
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class CustomerStatus {
        <<enumeration>>
        ACTIVE
        INACTIVE
        SUSPENDED
    }
    class MembershipStatus {
        <<enumeration>>
        ACTIVE
        FROZEN
        EXPIRED
        CANCELLED
    }
    class PlanType {
        <<enumeration>>
        MONTHLY
        QUARTERLY
        YEARLY
    }
    class SessionStatus {
        <<enumeration>>
        REQUESTED
        CONFIRMED
        COMPLETED
        NO_SHOW
        CANCELLED
    }
    class SubscriptionStatus {
        <<enumeration>>
        ACTIVE
        EXPIRED
        CANCELLED
    }
    class AvailabilityStatus {
        <<enumeration>>
        OPEN
        FULL
        CLOSED
    }
    class StaffRole {
        <<enumeration>>
        ADMIN
        MANAGER
        STAFF
    }
    class PaymentMethod {
        <<enumeration>>
        CASH
        CARD
        TRANSFER
    }
    class EquipmentStatus {
        <<enumeration>>
        AVAILABLE
        IN_MAINTENANCE
        RETIRED
    }
    class EquipmentCondition {
        <<enumeration>>
        NEW
        GOOD
        FAIR
        POOR
    }
    class ProgressMetric {
        <<enumeration>>
        WEIGHT_KG
        BODY_FAT_PCT
    }
    class MainApp {
        +start(Stage) void
        +main(String[]) void
    }
    Membership "0..*" --> "1" Customer : is held by
    WorkoutProgram "0..*" --> "1" Customer : is followed by
    Attendance "0..*" --> "1" Customer : is recorded by
    ProgressRecord "0..*" --> "1" Customer : is tracked by
    Session "0..*" --> "1" Customer : is booked by
    Customer "0..*" --> "0..1" Trainer : is assigned to
    Payment "0..*" --> "1" Customer : is paid by
    WorkoutProgram "0..*" --> "1" Trainer : is created by
    Session "0..*" --> "1" Trainer : is coached by
    TrainerAvailability "0..*" --> "1" Trainer : is published by
    Session "0..*" --> "0..1" TrainerAvailability : is fitted into
    TrainerSubscription "0..*" --> "1" Customer : is covered by
    TrainerSubscription "0..*" --> "1" Trainer : is engaged by
    Session "0..*" --> "0..1" TrainerSubscription : is covered by
    MedicalProfile "0..1" --> "1" Customer : is described by
    Payment "0..*" --> "0..1" Membership : pays for membership dues
    Payment "0..*" --> "0..1" TrainerSubscription : pays for trainer subscription
    Payment "0..*" --> "0..1" Session : pays for a single session
    %% Mermaid lays notes and unconnected classes out beside the graph, not inside it. Descriptions
    %% therefore widen this diagram dramatically -- all 26 notes made it 13193 units wide against
    %% 6303 without them. They now live in the "Class descriptions" diagram below, and only the four
    %% notes that state a structural constraint stay here.
```

## 1b. Class descriptions (one row per class)

The prose for every class, kept out of §1 so the structural diagram stays vertical. Marked
`«enumeration»` where the row describes an enum.

```mermaid
classDiagram
    direction TB
    class Customer {
        <<description>>
        Member: contact, status, links
    }
    class Trainer {
        <<description>>
        Coach: specialization, hourlyRate
    }
    class Membership {
        <<description>>
        Plan, start and end dates
    }
    class WorkoutProgram {
        <<description>>
        Exercise list, version, creator
    }
    class Session {
        <<description>>
        Booked slot, cancel reason, notes
    }
    class Equipment {
        <<description>>
        Inventory item, condition, status
    }
    class Attendance {
        <<description>>
        One check-in per customer per day
    }
    class Payment {
        <<description>>
        Amount, method, date, receiptNo
    }
    class ProgressRecord {
        <<description>>
        Dated body metrics and benchmarks
    }
    class MedicalProfile {
        <<description>>
        Blood type, conditions, meds, doctor
    }
    class Administrator {
        <<description>>
        Login identity, role, active flag
    }
    class TrainerSubscription {
        <<description>>
        Period engagement at a flat rate
    }
    class TrainerAvailability {
        <<description>>
        One hourly slot per row, per-slot cap
    }
    class CustomerStatus {
        <<enumeration>>
        ACTIVE
        INACTIVE
        SUSPENDED
    }
    class MembershipStatus {
        <<enumeration>>
        ACTIVE
        FROZEN
        EXPIRED
        CANCELLED
    }
    class PlanType {
        <<enumeration>>
        MONTHLY
        QUARTERLY
        YEARLY
    }
    class SessionStatus {
        <<enumeration>>
        REQUESTED
        CONFIRMED
        COMPLETED
        NO_SHOW
        CANCELLED
    }
    class SubscriptionStatus {
        <<enumeration>>
        ACTIVE
        EXPIRED
        CANCELLED
    }
    class AvailabilityStatus {
        <<enumeration>>
        OPEN
        FULL
        CLOSED
    }
    class StaffRole {
        <<enumeration>>
        ADMIN
        MANAGER
        STAFF
    }
    class PaymentMethod {
        <<enumeration>>
        CASH
        CARD
        TRANSFER
    }
    class EquipmentStatus {
        <<enumeration>>
        AVAILABLE
        IN_MAINTENANCE
        RETIRED
    }
    class EquipmentCondition {
        <<enumeration>>
        NEW
        GOOD
        FAIR
        POOR
    }
    class ProgressMetric {
        <<enumeration>>
        WEIGHT_KG
        BODY_FAT_PCT
    }
    %% `~~` is not an invisible edge in Mermaid -- it creates no link, and Mermaid then lays these
    %% classes out in one unreadable row. Real associations are used to stack them; read the class
    %% bodies as the content here, not the arrows.
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
    note for Customer "price is BigDecimal; no double money anywhere."
    note for Trainer "May read the MedicalProfile of customers whose trainerId is this trainer, enforced by MedicalProfileService.getForTrainer."
    note for WorkoutProgram "version is unique per customer."
    note for Session "Booked slot with cancel reason and notes. price must be 0 when subscriptionId is set."
    note for Attendance "One check-in/out record per customer per day, enforced by AttendanceDao.findByCustomerAndDay."
    note for Payment "Amount, method and date. payer is the Customer; payee is the gym."
    note for ProgressRecord "targetWeightKg is the nullable benchmark."
    note for MedicalProfile "At most one per customer, created on demand."
    note for Administrator "authenticate() rejects active == false. Role ADMIN may mutate every entity through AdminService."
    note for TrainerAvailability "One hourly slot per row: open day, time window, per-slot customer cap."
    note for MembershipStatus "FROZEN is set by freeze(); EXPIRED is derived from endDate and never set by hand."
    note for SessionStatus "Legal transitions are enforced by SchedulingService.canTransition."
    note for SubscriptionStatus "EXPIRED is derived from endDate; CANCELLED is set by cancelSubscription."
    note for AvailabilityStatus "FULL is derived from bookedCount >= maxCustomers; only OPEN and CLOSED are set by hand."
    note for ProgressMetric "Selects which ProgressRecord measurement ProgressService.series returns."
```

## 2. Layered architecture

```mermaid
flowchart TD
    APP[MainApp<br/>entry point, nav shell, theme, overlay layer] --> CTRL[Controllers<br/>16 FXML screens]
    CTRL --> SRV[Services<br/>validation + business rules]
    SRV --> DAO[DAOs<br/>SQLite persistence]
    DAO --> DB[(SQLite<br/>gym.db)]
    M3FX[External: m3fx library<br/>M3 theme + components] -. styles .-> APP
    M3FX -. styles .-> CTRL
    UTIL[util<br/>DbConnection, DaoException, Validators, DateUtils,<br/>FxUtils, ChartUtils, SessionContext, PasswordUtils] -. supports .-> SRV
    UTIL -. supports .-> CTRL
    UTIL -. supports .-> DAO
```

Layer rules:
- Controllers never contain SQL and never touch a DAO directly — always through a service.
- Services and DAOs never import JavaFX. `FxUtils` is the only JavaFX-aware utility and only
  controllers depend on it.
- `DaoException` is thrown by every `*DaoImpl`, caught in the controller, and shown with
  `FxUtils.error(getMessage())`. Services never format a message for display.

Startup and session flow (the boundary path, shown here because `MainApp` and `SessionContext`
are the two classes that sit outside the four layers):

1. `MainApp.start(Stage)` applies the theme (`M3Stylesheets.applyTo`), builds the nav shell and
   overlay layer, and opens `LoginController`.
2. `LoginController.onLogin()` calls `AdminService.authenticate(username, password)`, which returns
   `null` for an unknown username, a wrong password, or `Administrator.active == false`.
3. On success `LoginController` stores the returned `Administrator` in `SessionContext` and opens
   `DashboardController`; `SessionContext.requireRole(StaffRole.ADMIN)` then gates `StaffController`.
4. `DashboardController.onLogout()` calls `SessionContext.clear()` and returns to `LoginController`.
   `MainApp` clears it again on exit as a safety net.

## 3. Data access layer

```mermaid
classDiagram
    direction TB
    class DbConnection {
        <<interface>>
        +getConnection() Connection
        +initializeSchema() void
    }
    class SqliteDbConnection {
        +getConnection() Connection
        +initializeSchema() void
    }
    class CustomerDao {
        <<interface>>
        +save(Customer) int
        +findById(int) Customer
        +search(String) List~Customer~
        +findAll() List~Customer~
        +update(Customer) void
        +delete(int) void
        +findByStatus(CustomerStatus) List~Customer~
        +findByTrainer(int) List~Customer~
    }
    class CustomerDaoImpl {
        +save(Customer) int
        +findById(int) Customer
        +search(String) List~Customer~
        +findAll() List~Customer~
        +update(Customer) void
        +delete(int) void
        +findByStatus(CustomerStatus) List~Customer~
        +findByTrainer(int) List~Customer~
    }
    class TrainerDao {
        <<interface>>
        +save(Trainer) int
        +findById(int) Trainer
        +findAll() List~Trainer~
        +update(Trainer) void
        +delete(int) void
    }
    class TrainerDaoImpl {
        +save(Trainer) int
        +findById(int) Trainer
        +findAll() List~Trainer~
        +update(Trainer) void
        +delete(int) void
    }
    class MembershipDao {
        <<interface>>
        +save(Membership) int
        +findExpiring(int) List~Membership~
        +findById(int) Membership
        +findAll() List~Membership~
        +update(Membership) void
        +delete(int) void
        +findByCustomer(int) List~Membership~
        +findByStatus(MembershipStatus) List~Membership~
    }
    class MembershipDaoImpl {
        +save(Membership) int
        +findExpiring(int) List~Membership~
        +findById(int) Membership
        +findAll() List~Membership~
        +update(Membership) void
        +delete(int) void
        +findByCustomer(int) List~Membership~
        +findByStatus(MembershipStatus) List~Membership~
    }
    class WorkoutProgramDao {
        <<interface>>
        +save(WorkoutProgram) int
        +findById(int) WorkoutProgram
        +findAll() List~WorkoutProgram~
        +update(WorkoutProgram) void
        +delete(int) void
        +findByCustomer(int) List~WorkoutProgram~
        +findLatestVersion(int) int
    }
    class WorkoutProgramDaoImpl {
        +save(WorkoutProgram) int
        +findById(int) WorkoutProgram
        +findAll() List~WorkoutProgram~
        +update(WorkoutProgram) void
        +delete(int) void
        +findByCustomer(int) List~WorkoutProgram~
        +findLatestVersion(int) int
    }
    class SessionDao {
        <<interface>>
        +save(Session) int
        +findByDay(String) List~Session~
        +findById(int) Session
        +findAll() List~Session~
        +update(Session) void
        +delete(int) void
        +findByStatus(SessionStatus) List~Session~
        +findByCustomer(int) List~Session~
        +findByCustomerAndDay(int, String) List~Session~
        +findByTrainerAndDay(int, String) List~Session~
        +countByAvailability(int availabilityId, SessionStatus status) int
    }
    class SessionDaoImpl {
        +save(Session) int
        +findByDay(String) List~Session~
        +findById(int) Session
        +findAll() List~Session~
        +update(Session) void
        +delete(int) void
        +findByStatus(SessionStatus) List~Session~
        +findByCustomer(int) List~Session~
        +findByCustomerAndDay(int, String) List~Session~
        +findByTrainerAndDay(int, String) List~Session~
        +countByAvailability(int availabilityId, SessionStatus status) int
    }
    class EquipmentDao {
        <<interface>>
        +save(Equipment) int
        +findById(int) Equipment
        +findAll() List~Equipment~
        +update(Equipment) void
        +delete(int) void
        +findByStatus(EquipmentStatus) List~Equipment~
        +findByCondition(EquipmentCondition) List~Equipment~
    }
    class EquipmentDaoImpl {
        +save(Equipment) int
        +findById(int) Equipment
        +findAll() List~Equipment~
        +update(Equipment) void
        +delete(int) void
        +findByStatus(EquipmentStatus) List~Equipment~
        +findByCondition(EquipmentCondition) List~Equipment~
    }
    class AttendanceDao {
        <<interface>>
        +save(Attendance) int
        +findByDay(String) List~Attendance~
        +findById(int) Attendance
        +findAll() List~Attendance~
        +update(Attendance) void
        +delete(int) void
        +findByCustomer(int) List~Attendance~
        +findByCustomerAndDay(int, String) List~Attendance~
    }
    class AttendanceDaoImpl {
        +save(Attendance) int
        +findByDay(String) List~Attendance~
        +findById(int) Attendance
        +findAll() List~Attendance~
        +update(Attendance) void
        +delete(int) void
        +findByCustomer(int) List~Attendance~
        +findByCustomerAndDay(int, String) List~Attendance~
    }
    class PaymentDao {
        <<interface>>
        +save(Payment) int
        +findById(int) Payment
        +findAll() List~Payment~
        +update(Payment) void
        +delete(int) void
        +findByCustomer(int customerId) List~Payment~
        +findByReceipt(String receiptNo) Payment
        +findByMethod(PaymentMethod method) List~Payment~
        +revenueBetween(String from, String to) BigDecimal
    }
    class PaymentDaoImpl {
        +save(Payment) int
        +findById(int) Payment
        +findAll() List~Payment~
        +update(Payment) void
        +delete(int) void
        +findByCustomer(int customerId) List~Payment~
        +findByReceipt(String receiptNo) Payment
        +findByMethod(PaymentMethod method) List~Payment~
        +revenueBetween(String from, String to) BigDecimal
    }
    class ProgressDao {
        <<interface>>
        +save(ProgressRecord) int
        +findById(int) ProgressRecord
        +findAll() List~ProgressRecord~
        +update(ProgressRecord) void
        +delete(int) void
        +findByCustomer(int) List~ProgressRecord~
    }
    class ProgressDaoImpl {
        +save(ProgressRecord) int
        +findById(int) ProgressRecord
        +findAll() List~ProgressRecord~
        +update(ProgressRecord) void
        +delete(int) void
        +findByCustomer(int) List~ProgressRecord~
    }
    class MedicalProfileDao {
        <<interface>>
        +save(MedicalProfile) int
        +findByCustomer(int) MedicalProfile
        +findById(int) MedicalProfile
        +findAll() List~MedicalProfile~
        +update(MedicalProfile) void
        +delete(int) void
    }
    class MedicalProfileDaoImpl {
        +save(MedicalProfile) int
        +findByCustomer(int) MedicalProfile
        +findById(int) MedicalProfile
        +findAll() List~MedicalProfile~
        +update(MedicalProfile) void
        +delete(int) void
    }
    class AdminDao {
        <<interface>>
        +save(Administrator) int
        +findByUsername(String) Administrator
        +findById(int) Administrator
        +findAll() List~Administrator~
        +update(Administrator) void
        +delete(int) void
        +findByRole(StaffRole) List~Administrator~
    }
    class AdminDaoImpl {
        +save(Administrator) int
        +findByUsername(String) Administrator
        +findById(int) Administrator
        +findAll() List~Administrator~
        +update(Administrator) void
        +delete(int) void
        +findByRole(StaffRole) List~Administrator~
    }
    class TrainerSubscriptionDao {
        <<interface>>
        +save(TrainerSubscription) int
        +findActive(int, int, String) TrainerSubscription
        +findById(int) TrainerSubscription
        +findAll() List~TrainerSubscription~
        +update(TrainerSubscription) void
        +delete(int) void
        +findByCustomer(int) List~TrainerSubscription~
        +findByTrainer(int) List~TrainerSubscription~
    }
    class TrainerSubscriptionDaoImpl {
        +save(TrainerSubscription) int
        +findActive(int, int, String) TrainerSubscription
        +findById(int) TrainerSubscription
        +findAll() List~TrainerSubscription~
        +update(TrainerSubscription) void
        +delete(int) void
        +findByCustomer(int) List~TrainerSubscription~
        +findByTrainer(int) List~TrainerSubscription~
    }
    class TrainerAvailabilityDao {
        <<interface>>
        +save(TrainerAvailability) int
        +findOpenSlots(int, String) List~TrainerAvailability~
        +findById(int) TrainerAvailability
        +findAll() List~TrainerAvailability~
        +update(TrainerAvailability) void
        +delete(int) void
        +findByTrainerAndDate(int, String) List~TrainerAvailability~
    }
    class TrainerAvailabilityDaoImpl {
        +save(TrainerAvailability) int
        +findOpenSlots(int, String) List~TrainerAvailability~
        +findById(int) TrainerAvailability
        +findAll() List~TrainerAvailability~
        +update(TrainerAvailability) void
        +delete(int) void
        +findByTrainerAndDate(int, String) List~TrainerAvailability~
    }
    SqliteDbConnection <|.. DbConnection : implements
    CustomerDao <|.. CustomerDaoImpl : implements
    TrainerDao <|.. TrainerDaoImpl : implements
    MembershipDao <|.. MembershipDaoImpl : implements
    WorkoutProgramDao <|.. WorkoutProgramDaoImpl : implements
    SessionDao <|.. SessionDaoImpl : implements
    EquipmentDao <|.. EquipmentDaoImpl : implements
    AttendanceDao <|.. AttendanceDaoImpl : implements
    PaymentDao <|.. PaymentDaoImpl : implements
    ProgressDao <|.. ProgressDaoImpl : implements
    MedicalProfileDao <|.. MedicalProfileDaoImpl : implements
    AdminDao <|.. AdminDaoImpl : implements
    TrainerSubscriptionDao <|.. TrainerSubscriptionDaoImpl : implements
    TrainerAvailabilityDao <|.. TrainerAvailabilityDaoImpl : implements
    CustomerDaoImpl ..> DbConnection : uses
    TrainerDaoImpl ..> DbConnection : uses
    MembershipDaoImpl ..> DbConnection : uses
    WorkoutProgramDaoImpl ..> DbConnection : uses
    SessionDaoImpl ..> DbConnection : uses
    EquipmentDaoImpl ..> DbConnection : uses
    AttendanceDaoImpl ..> DbConnection : uses
    PaymentDaoImpl ..> DbConnection : uses
    ProgressDaoImpl ..> DbConnection : uses
    MedicalProfileDaoImpl ..> DbConnection : uses
    AdminDaoImpl ..> DbConnection : uses
    TrainerSubscriptionDaoImpl ..> DbConnection : uses
    TrainerAvailabilityDaoImpl ..> DbConnection : uses
```

## 4. Service layer (validation + business rules, no JavaFX, no SQL)

```mermaid
classDiagram
    direction TB
    class CustomerService {
        +validate(Customer customer) List~String~
        +search(String term) List~Customer~
        +findAll() List~Customer~
        +findById(int customerId) Customer
        +save(Customer customer) int
        +update(Customer customer) void
        +changeStatus(int customerId, CustomerStatus status) void
        +findByTrainer(int trainerId) List~Customer~
    }
    class TrainerService {
        +assignCustomer(int customerId, int trainerId) void
        +setRate(int trainerId, BigDecimal hourlyRate) void
        +publishSlot(int trainerId, String date, String startTime, String endTime, int maxCustomers) TrainerAvailability
        +closeSlot(int availabilityId) void
        +setDailyCap(int trainerId, String date, int maxCustomersPerSlot) void
        +setSlotCap(int availabilityId, int maxCustomers) void
        +findOpenSlots(int trainerId, String date) List~TrainerAvailability~
        +findAll() List~Trainer~
        +findById(int trainerId) Trainer
        +save(Trainer trainer) int
        +update(Trainer trainer) void
    }
    class MembershipService {
        +sell(int customerId, PlanType plan) Membership
        +renew(int membershipId) void
        +freeze(int membershipId) void
        +unfreeze(int membershipId) void
        +cancel(int membershipId) void
        +freezeForSuspended(int customerId) int
        +expireOverdue() int
        +expiryReminders(int withinDays) List~Membership~
        +findAll() List~Membership~
        +findByCustomer(int customerId) List~Membership~
    }
    class WorkoutService {
        +buildProgram(int customerId, int trainerId, String name, String exercises) WorkoutProgram
        +reassign(int programId, int customerId) void
        +newVersion(int programId) WorkoutProgram
        +update(WorkoutProgram program) void
        +delete(int programId) void
        +findByCustomer(int customerId) List~WorkoutProgram~
    }
    class SchedulingService {
        +bookOpenSlot(int customerId, int availabilityId, String date, String startTime) Session
        +requestOutsideSlots(int customerId, int trainerId, String date, String startTime) Session
        +adminReserve(int customerId, int trainerId, String date, String startTime) Session
        +bookedCount(int availabilityId) int
        +isSlotFull(int availabilityId) boolean
        +detectConflicts(int customerId, int trainerId, String date, String startTime) List~Session~
        +canTransition(SessionStatus from, SessionStatus to) boolean
        +confirmSession(int sessionId) void
        +cancelSession(int sessionId, String reason) void
        +completeSession(int sessionId, String sessionNotes) void
        +markNoShow(int sessionId) void
        +getAvailableSlots(int trainerId, String date) List~TrainerAvailability~
        +getCustomerSessions(int customerId) List~Session~
        +getTrainerSessions(int trainerId) List~Session~
        +subscribe(int customerId, int trainerId, String startDate, String endDate) TrainerSubscription
        +activeSubscription(int customerId, int trainerId) TrainerSubscription
        +subscriptionPrice(int trainerId) BigDecimal
        +renewSubscription(int subscriptionId, String newEndDate) void
        +cancelSubscription(int subscriptionId) void
        +coveredBySubscription(Session session) boolean
        +expireOverdueSubscriptions() int
    }
    class EquipmentService {
        +save(Equipment equipment) int
        +update(Equipment equipment) void
        +changeStatus(int equipmentId, EquipmentStatus status) void
        +changeCondition(int equipmentId, EquipmentCondition condition) void
        +logMaintenance(int equipmentId, String date) void
        +delete(int equipmentId) void
        +findAll() List~Equipment~
        +findByStatus(EquipmentStatus status) List~Equipment~
        +findByCondition(EquipmentCondition condition) List~Equipment~
    }
    class AttendanceService {
        +checkIn(int customerId, String date, String time) Attendance
        +checkOut(int customerId, String date, String time) void
        +dailyReport(String date) List~Attendance~
        +correctRecord(int attendanceId, String checkIn, String checkOut) void
        +findByCustomer(int customerId) List~Attendance~
    }
    class PaymentService {
        +record(Payment payment) int
        +validLink(Payment payment) boolean
        +voidPayment(int paymentId, String reason) void
        +voidRecord(String entityName, int id) void
        +balance(int customerId) BigDecimal
        +revenueBetween(String from, String to) BigDecimal
        +findByCustomer(int customerId) List~Payment~
        +findByReceipt(String receiptNo) Payment
        +findByMethod(PaymentMethod method) List~Payment~
    }
    class ProgressService {
        +save(ProgressRecord record) int
        +update(ProgressRecord record) void
        +delete(int recordId) void
        +findByCustomer(int customerId) List~ProgressRecord~
        +series(int customerId, ProgressMetric metric) List~Double~
        +targetWeight(int customerId) Double
    }
    class MedicalProfileService {
        +save(MedicalProfile profile) int
        +update(MedicalProfile profile) void
        +getByCustomer(int customerId) MedicalProfile
        +getForTrainer(int customerId, int trainerId) MedicalProfile
        +delete(int profileId) void
    }
    class AdminService {
        +authenticate(String username, String password) Administrator
        +resetPassword(int adminId, String newPassword) void
        +saveStaff(Administrator admin) int
        +updateStaff(Administrator admin) void
        +deleteStaff(int adminId) void
        +findAllStaff() List~Administrator~
        +findByRole(StaffRole role) List~Administrator~
        +reassignTrainer(int customerId, int trainerId) void
        +adjustPayment(int paymentId, BigDecimal newAmount, String reason) void
        +deleteCustomer(int customerId) void
        +deleteTrainer(int trainerId) void
    }
    CustomerService ..> CustomerDao : persists via
    CustomerService ..> Validators : validates with
    CustomerService ..> MembershipService : freezes memberships when suspended
    TrainerService ..> TrainerDao : persists via
    TrainerService ..> TrainerAvailabilityDao : persists slots via
    TrainerService ..> MedicalProfileService : medical view via
    TrainerService ..> Validators : validates slot windows with
    TrainerService ..> DateUtils : dates with
    MembershipService ..> MembershipDao : persists via
    MembershipService ..> CustomerDao : reads owner from
    MembershipService ..> DateUtils : dates with
    WorkoutService ..> WorkoutProgramDao : persists via
    WorkoutService ..> CustomerDao : validates assignment with
    WorkoutService ..> TrainerDao : validates creator with
    SchedulingService ..> SessionDao : persists via
    SchedulingService ..> TrainerAvailabilityDao : checks slots from
    SchedulingService ..> TrainerSubscriptionDao : reads subs from
    SchedulingService ..> CustomerDao : reads customers from
    SchedulingService ..> TrainerDao : reads trainers from
    SchedulingService ..> DateUtils : dates with
    EquipmentService ..> EquipmentDao : persists via
    AttendanceService ..> AttendanceDao : persists via
    AttendanceService ..> CustomerDao : validates status with
    AttendanceService ..> MembershipDao : validates eligibility with
    PaymentService ..> PaymentDao : persists via
    PaymentService ..> CustomerDao : reads payer from
    PaymentService ..> MembershipDao : prices against
    PaymentService ..> SessionDao : prices against
    ProgressService ..> ProgressDao : persists via
    ProgressService ..> ChartUtils : charts with
    MedicalProfileService ..> MedicalProfileDao : persists via
    MedicalProfileService ..> CustomerDao : checks ownership with
    AdminService ..> AdminDao : persists via
    AdminService ..> PasswordUtils : hashes with
    AdminService ..> GuardService : reference check with
    AdminService ..> PaymentService : voids and adjusts through
    AdminService ..> CustomerDao : overrides with
    AdminService ..> TrainerDao : overrides with
```

## 5. Controllers (each backed by a matching FXML file)

`MainApp` is declared in §1 because it is the entry point rather than a screen; it appears in this
diagram only as the edge that opens the first screen.

```mermaid
classDiagram
    direction TB
    class LoginController {
        +initialize() void
        +onLogin() void
        +onForgotPassword() void
    }
    class DashboardController {
        +initialize() void
        +refreshData() void
        +onLogout() void
    }
    class CustomerListController {
        +initialize() void
        +onSearch() void
        +onAdd() void
        +onOpenProfile(int customerId) void
        +onDelete(int customerId) void
    }
    class CustomerProfileController {
        +initialize() void
        +showCustomer(int customerId) void
        +onSave() void
    }
    class TrainerListController {
        +initialize() void
        +onAdd() void
        +onOpenProfile(int trainerId) void
        +onDelete(int trainerId) void
    }
    class TrainerProfileController {
        +initialize() void
        +showTrainer(int trainerId) void
        +onSave() void
        +showMedicalProfile(int customerId) void
    }
    class MembershipController {
        +initialize() void
        +onSell() void
        +onRenew() void
        +onFreeze() void
        +onUnfreeze() void
        +onCancel() void
    }
    class WorkoutController {
        +initialize() void
        +onBuild() void
        +onReassign() void
        +onNewVersion() void
        +onDelete() void
    }
    class ScheduleController {
        +initialize() void
        +onBook() void
        +onRequest() void
        +onConfirm(int sessionId) void
        +onComplete(int sessionId) void
        +onNoShow(int sessionId) void
        +onReserve() void
        +onCancel() void
        +onSubscribe() void
        +onRenewSubscription() void
        +onCancelSubscription() void
        +showAvailableTrainers() void
        +showTrainerSlots(int trainerId) void
    }
    class AvailabilityController {
        +initialize() void
        +setCalendar(int trainerId, String date) void
        +setSlots(int trainerId, String date, String startTime) void
        +setSlotCap(int availabilityId, int maxCustomers) void
    }
    class EquipmentController {
        +initialize() void
        +onAdd() void
        +onStatusChange(int equipmentId, EquipmentStatus status) void
        +onConditionChange(int equipmentId, EquipmentCondition condition) void
        +onLogMaintenance(int equipmentId, String date) void
        +onDelete(int equipmentId) void
    }
    class AttendanceController {
        +initialize() void
        +onCheckIn(int customerId) void
        +onCheckOut(int customerId) void
        +showDailyReport(String date) void
        +onCorrectRecord(int attendanceId) void
    }
    class PaymentController {
        +initialize() void
        +onRecord() void
        +onVoid(int paymentId) void
        +showReceipt(String receiptNo) void
    }
    class ProgressController {
        +initialize() void
        +showProgress(int customerId) void
        +onSaveRecord() void
        +onDeleteRecord(int recordId) void
    }
    class MedicalProfileController {
        +initialize() void
        +showProfile(int customerId) void
        +onSave() void
        +onDelete() void
    }
    class StaffController {
        +initialize() void
        +onAddStaff() void
        +onEditStaff(int adminId) void
        +onDeleteStaff(int adminId) void
        +onResetPassword(int adminId) void
        +onReassign() void
        +onAdjustPayment(int paymentId) void
    }
    MainApp ..> LoginController : opens
    MainApp ..> DashboardController : opens after login
    LoginController ..> AdminService : authenticates via
    LoginController ..> SessionContext : stores user
    LoginController ..> DashboardController : opens on success
    DashboardController ..> SessionContext : clears on logout
    DashboardController ..> CustomerService : reads from
    DashboardController ..> PaymentService : reads from
    DashboardController ..> SchedulingService : reads from
    DashboardController ..> EquipmentService : reads from
    CustomerListController ..> CustomerService : uses
    CustomerListController ..> CustomerProfileController : opens
    CustomerProfileController ..> CustomerService : saves via
    CustomerProfileController ..> MembershipService : reads plans from
    CustomerProfileController ..> MedicalProfileService : reads profile from
    TrainerListController ..> TrainerService : uses
    TrainerListController ..> TrainerProfileController : opens
    TrainerProfileController ..> TrainerService : saves via
    TrainerProfileController ..> CustomerService : reads roster from
    TrainerProfileController ..> MedicalProfileService : reads medical section from
    MembershipController ..> MembershipService : uses
    WorkoutController ..> WorkoutService : uses
    ScheduleController ..> SchedulingService : uses
    ScheduleController ..> SessionContext : requires ADMIN for reserve
    AvailabilityController ..> TrainerService : uses
    EquipmentController ..> EquipmentService : uses
    AttendanceController ..> AttendanceService : uses
    PaymentController ..> PaymentService : uses
    ProgressController ..> ProgressService : uses
    MedicalProfileController ..> MedicalProfileService : uses
    StaffController ..> AdminService : uses
    StaffController ..> SessionContext : requires ADMIN role
    MainApp ..> SessionContext : clears on exit
    MainApp ..> FxUtils : installs overlay layer via
```

## 6. Utilities

```mermaid
classDiagram
    direction TB
    class DbConnection {
        <<interface>>
        +getConnection() Connection
        +initializeSchema() void
    }
    class SqliteDbConnection {
        +getConnection() Connection
        +initializeSchema() void
    }
    class DaoException {
        +DaoException(String message, Throwable cause)
    }
    class GuardService {
        <<utility>>
        +countReferences(String table, int id) int$
    }
    class Validators {
        <<utility>>
        +validPhone(String) boolean$
        +validEmail(String) boolean$
        +validDate(String) boolean$
        +validNumber(String) boolean$
        +validTimeRange(String from, String to) boolean$
    }
    class DateUtils {
        <<utility>>
        +addMonths(String date, int months) String$
        +formatDate(String date) String$
        +isBeforeNow(String date, String time) boolean$
        +hoursUntil(String date, String time) long$
    }
    class PasswordUtils {
        <<utility>>
        +hash(String plain) String$
        +verify(String plain, String hash) boolean$
    }
    class ChartUtils {
        <<utility>>
        +weightSeries(List~ProgressRecord~ records) List~Double~$
    }
    class SessionContext {
        -Administrator current
        +setAdministrator(Administrator admin) void
        +getAdministrator() Administrator
        +requireRole(StaffRole role) void
        +clear() void
    }
    class FxUtils {
        <<utility>>
        +info(String message) void$
        +confirm(String message) boolean$
        +error(String message) void$
        +switchScene(String fxml) void$
        +snackbar(String message) void$
    }
    GuardService ..> DbConnection : counts references via
```

---

## 7. Generalization to external types

The only two generalizations in the model both target Java platform types, so they are collected
here: `«external»` marks a superclass that is not project code.

```mermaid
classDiagram
    direction TB
    class MainApp {
        +start(Stage) void
        +main(String[]) void
    }
    class DaoException {
        +DaoException(String message, Throwable cause)
    }
    class Application {
        <<external>>
    }
    class RuntimeException {
        <<external>>
    }
    MainApp --|> Application : extends
    DaoException --|> RuntimeException : extends
```

**No project class extends another project class**, and that is a decision rather than an omission.
The three candidate hierarchies were each examined and rejected:

| Candidate | Verdict | Why |
|---|---|---|
| 13 `*DaoImpl` → `AbstractJdbcDao<T>` | stay flat | They vary in SQL and entity type, not structure. `DbConnection` already supplies the shared seam, and the 13 explicit `..> DbConnection` edges keep the wiring checkable. |
| 11 services → `BaseService` | stay flat | `validate` exists on exactly one service, so the base would be empty. |
| 16 controllers → `BaseController` | stay flat | `initialize()` is the JavaFX `Initializable` convention, differs in every controller, and must remain a public no-arg method for `FXMLLoader`. |

---

## Class descriptions

### Entities

| Class | Description |
|---|---|
| `Customer` | Gym member; fields: id, fullName, phone, email, address, dateOfBirth, gender, emergencyName, emergencyPhone, status (enum), joinDate, trainerId (nullable, assigned trainer); links to membership, trainer, program |
| `Trainer` | Coach; fields: id, fullName, phone, email, specialization, experienceYears, certifications, hireDate, hourlyRate; bookable slots live in TrainerAvailability |
| `Membership` | Plan; fields: id, customerId, plan (PlanType: monthly, quarterly, yearly), price, startDate, endDate, status (active, frozen, expired, cancelled), frozenDays |
| `WorkoutProgram` | Exercise list; fields: id, customerId, creatorTrainerId, name, exercises, version, createdDate |
| `Session` | Training slot; fields: id, customerId, trainerId, date, startTime, durationMin, status, availabilityId (nullable slot), subscriptionId (nullable), price, cancelReason, sessionNotes |
| `Equipment` | Standalone inventory item; fields: id, name, category, purchaseDate, condition (enum), status (enum), lastMaintenance. No foreign key to any other entity. |
| `Attendance` | One check-in/out record per customer per day; fields: id, customerId, date, checkIn, checkOut |
| `Payment` | fields: id, customerId, membershipId (nullable), subscriptionId (nullable), sessionId (nullable), amount, method, date, receiptNo, voided; exactly one of membership, trainer subscription, session |
| `ProgressRecord` | fields: id, customerId, date, weightKg, bodyFatPct, measurements, targetWeightKg (nullable), notes |
| `MedicalProfile` | Blood type, conditions, allergies, medications, injuries, doctor, notes; at most one per customer, created on demand |
| `Administrator` | Login identity with role (ADMIN/MANAGER/STAFF); may mutate all entities through AdminService |
| `TrainerSubscription` | Period engagement at a flat trainer-set rate; status uses SubscriptionStatus enum |
| `TrainerAvailability` | One hourly slot per row: date, startTime, endTime, maxCustomers, status. A trainer may publish several per day. |

### Enumerations

| Enum | Literals |
|---|---|
| `CustomerStatus` | ACTIVE, INACTIVE, SUSPENDED |
| `MembershipStatus` | ACTIVE, FROZEN, EXPIRED, CANCELLED |
| `PlanType` | MONTHLY, QUARTERLY, YEARLY |
| `SessionStatus` | REQUESTED, CONFIRMED, COMPLETED, NO_SHOW, CANCELLED |
| `SubscriptionStatus` | ACTIVE, EXPIRED, CANCELLED |
| `AvailabilityStatus` | OPEN, FULL, CLOSED |
| `StaffRole` | ADMIN, MANAGER, STAFF |
| `PaymentMethod` | CASH, CARD, TRANSFER |
| `EquipmentStatus` | AVAILABLE, IN_MAINTENANCE, RETIRED |
| `EquipmentCondition` | NEW, GOOD, FAIR, POOR |

`FULL`, `EXPIRED` and the two `CANCELLED` literals are the only ones with a non-obvious producer:
`FULL` is derived from occupancy, `MembershipStatus.EXPIRED` and `SubscriptionStatus.EXPIRED` are
derived from the end date, and both `CANCELLED` literals are set by an explicit cancel operation.

### Data access

| Class | Description |
|---|---|
| `DbConnection` | Interface for the SQLite connection factory; DAOs depend on this, not on the implementation |
| `SqliteDbConnection` | Implements `DbConnection`; creates `gym.db` and initializes the schema |
| `DaoException` | Unchecked wrapper for `SQLException`; translated to friendly messages in the controller via `FxUtils` |
| `CustomerDao` / `CustomerDaoImpl` | Persist, load and search customers; list by assigned trainer and by status |
| `TrainerDao` / `TrainerDaoImpl` | Persist and load trainers |
| `MembershipDao` / `MembershipDaoImpl` | Persist memberships; query expiring ones for reminders; query by status |
| `WorkoutProgramDao` / `WorkoutProgramDaoImpl` | Persist programs; list per customer; latest version per customer |
| `SessionDao` / `SessionDaoImpl` | Persist sessions; query by day, customer, customer+day, trainer+day, status; count occupancy of a slot |
| `EquipmentDao` / `EquipmentDaoImpl` | Persist and list equipment; query by status and condition |
| `AttendanceDao` / `AttendanceDaoImpl` | Persist check-ins; daily attendance lists; query by customer; one-row-per-day lookup |
| `PaymentDao` / `PaymentDaoImpl` | Persist payments; revenue sums over date ranges (voided excluded); query by customer, receipt number, method |
| `ProgressDao` / `ProgressDaoImpl` | Persist metric records; history per customer |
| `MedicalProfileDao` / `MedicalProfileDaoImpl` | Persist profiles; lookup per customer |
| `AdminDao` / `AdminDaoImpl` | Persist operators; lookup by username and by role |
| `TrainerSubscriptionDao` / `TrainerSubscriptionDaoImpl` | Persist engagements; active lookup; query by customer and trainer |
| `TrainerAvailabilityDao` / `TrainerAvailabilityDaoImpl` | Persist slots, caps, open days; query open slots by trainer and date |

### Services (validation + business rules, no JavaFX, no SQL)

| Class | Description |
|---|---|
| `CustomerService` | Customer validation, search/filter, status rules |
| `TrainerService` | Availability slots and caps (publish, close, cap), rates, assign, revoke medical access |
| `MembershipService` | Sell, renew, freeze, unfreeze, cancel, overdue expiry sweep, expiry reminders |
| `WorkoutService` | Program building rules, reassignment, per-customer versioning |
| `SchedulingService` | Booking rules (slot open, cap, trainer and customer conflicts), request/reserve, session state transitions, cancel-with-reason, subscribe and subscription lifecycle; distinguishes subscription (trainer) from membership (gym access) |
| `EquipmentService` | Add/edit inventory, status and condition transitions, last-maintenance date |
| `PaymentService` | Recording, link validation, voiding, balances, receipt lookup, revenue by range |
| `AttendanceService` | Check-in/out rules, eligibility checks, daily report data |
| `ProgressService` | Metric access for charts |
| `MedicalProfileService` | Medical CRUD, lookup by customer, assigned-trainer access check |
| `AdminService` | Auth (rejects inactive accounts), password reset, staff, reassign/adjust/void overrides, admin-only hard deletes; bypasses ownership |

### UI (each controller backed by a matching FXML file)

| Class | Description |
|---|---|
| `MainApp` | Entry point: builds the window, nav shell, theme and overlay layer |
| `LoginController` | Login screen: authenticates admin via AdminService, stores user in SessionContext |
| `DashboardController` | Main dashboard: active members, today's sessions, revenue snapshot, logout |
| `CustomerListController` | Searchable/filterable customer list |
| `CustomerProfileController` | Full profile: info, membership, trainer, program, schedule, progress |
| `TrainerListController` | Trainer roster with availability |
| `TrainerProfileController` | Trainer details, assigned customers, schedule |
| `MembershipController` | Plans; sell, renew, freeze, unfreeze, cancel |
| `WorkoutController` | Program builder and reassignment |
| `ScheduleController` | Agenda, booking dialog, session request/reservation, subscribe-to-trainer flow |
| `AvailabilityController` | Trainer calendar, per-slot caps, hourly slot publishing |
| `EquipmentController` | Inventory list, add/edit, status and condition changes, maintenance date |
| `AttendanceController` | Check-in/out screen and daily report |
| `PaymentController` | Payment recording, voiding, receipts, balances |
| `ProgressController` | Body-metric charts per customer |
| `MedicalProfileController` | Medical profile view/edit per customer |
| `StaffController` | Staff management and admin overrides (ADMIN role only) |

### Utilities

| Class | Description |
|---|---|
| `Validators` | Phone/email/number/date and time-range validation used by services; `«utility»`, all static, entity-free |
| `DateUtils` | Membership periods, session slot math, date formatting; owns every parse. `«utility»`, static |
| `PasswordUtils` | PBKDF2 password hashing and verification (Java built-in, no new deps); `«utility»`, static |
| `ChartUtils` | Progress chart dataset formatting; no DAO access, no money. `«utility»`, static |
| `GuardService` | Reference counter for the admin delete rule; `«utility»`, static. Reads which tables reference a row and counts them |
| `DaoException` | Unchecked `SQLException` wrapper extending `RuntimeException`, so it chains via `super(message, cause)`; `«utility»` |
| `SessionContext` | Holds the logged-in Administrator for role checks; `setAdministrator`/`getAdministrator`/`requireRole`/`clear`. Static mutable state, FX-thread confined |
| `FxUtils` | Friendly dialogs, alerts, scene switching, snackbar helper; the only JavaFX-aware utility, and it takes plain strings so it never sees `DaoException` |

### Enumerations (all stereotyped `«enumeration»`)

| Enum | Literals | Owner of the behaviour |
|---|---|---|
| `CustomerStatus` | ACTIVE, INACTIVE, SUSPENDED | `CustomerService.changeStatus` |
| `MembershipStatus` | ACTIVE, FROZEN, EXPIRED, CANCELLED | `MembershipService` |
| `PlanType` | MONTHLY, QUARTERLY, YEARLY | `MembershipService.sell` |
| `SessionStatus` | REQUESTED, CONFIRMED, COMPLETED, NO_SHOW, CANCELLED | `SchedulingService.canTransition` |
| `SubscriptionStatus` | ACTIVE, EXPIRED, CANCELLED | `SchedulingService` |
| `AvailabilityStatus` | OPEN, FULL, CLOSED | `TrainerService` |
| `StaffRole` | ADMIN, MANAGER, STAFF | `SessionContext.requireRole` |
| `PaymentMethod` | CASH, CARD, TRANSFER | `PaymentService.record` |
| `EquipmentStatus` | AVAILABLE, IN_MAINTENANCE, RETIRED | `EquipmentService.changeStatus` |
| `EquipmentCondition` | NEW, GOOD, FAIR, POOR | `EquipmentService.changeCondition` |
| `ProgressMetric` | WEIGHT_KG, BODY_FAT_PCT | `ProgressService.series` |

---

## Business rules

Agreed decisions that the diagrams alone cannot express. Every owner implements these identically.
(Team ownership split lives in `docs/UML_TEAM.md`.)

| # | Rule |
|---|---|
| 1 | `bookOpenSlot` = customer books a published `OPEN` slot → `CONFIRMED`. `requestOutsideSlots` = requested outside published slots → `REQUESTED`, trainer confirms. `adminReserve` = admin override that ignores slot rules. Default `durationMin` = 60. |
| 2 | `SessionStatus.NO_SHOW` exists and matters for trainer statistics; `markNoShow` sets it. |
| 3 | A customer may have **zero or one** medical profile; it is created on demand, not at registration. |
| 4 | "Delete" for `Customer`/`Trainer` means set `INACTIVE` / end active records. Hard `delete(int)` is admin-only, lives on `AdminService`, and refuses while references exist — it calls `GuardService.countReferences(table, id)` first and aborts if the result is non-zero, because `schema.sql` uses no FK cascades. |
| 5 | `checkIn` requires `CustomerStatus.ACTIVE` and a `Membership` with status `ACTIVE` and `endDate >= today`. A second check-in on the same day returns the existing row instead of inserting (`AttendanceDao.findByCustomerAndDay`). |
| 6 | `PaymentService.balance(int)` = sum of unpaid items: memberships sold without a matching non-voided payment, plus subscription and ad-hoc session prices without payments. Voided payments never count. |
| 7 | `freeze(int)` stores `frozenDays`; `unfreeze(int)` shifts `endDate` forward by that period and clears the counter. Only an `ACTIVE` membership may be frozen; renewing an `EXPIRED` one extends `endDate`, renewing a `CANCELLED` one is refused. |
| 8 | A trainer may publish several hourly `TrainerAvailability` slots per day; `findByTrainerAndDate` returns a list. `maxCustomers` is the cap **per slot**. `setDailyCap` applies one value across a day's slots and re-applies after any later publish, so it wins regardless of call order; `setSlotCap` overrides a single slot afterwards. `publishSlot` rejects `startTime >= endTime` via `Validators.validTimeRange`. |
| 9 | Dates are `yyyy-MM-dd`, `Session` times are `HH:mm`, and `DateUtils` owns parsing so `findByDay` and slot comparisons agree. Controllers never build a date or time string themselves. |
| 10 | Cancelling a session up to 2h before start is free; later cancellations are still marked `CANCELLED` with the reason recorded. `DateUtils.hoursUntil` backs the check. |
| 11 | A session covered by a subscription must have `price == 0`; `coveredBySubscription` is the guard and prevents double billing. `TrainerSubscription.trainerId` must equal the customer's assigned `trainerId`, and every session it covers must carry that same `trainerId`. |
| 12 | Exactly one of `membershipId`, `subscriptionId`, `sessionId` is set on a `Payment`; `validLink` rejects anything else and `record` calls it. `Payment.customerId` must equal the linked object's customer. |
| 13 | `AvailabilityStatus.FULL` and both `EXPIRED` literals are derived state — recomputed on read, never set by hand. Only `OPEN`/`CLOSED` and the `CANCELLED` literals are set explicitly. |
| 14 | A `SUSPENDED` customer implies their memberships are frozen. `CustomerService.changeStatus(SUSPENDED)` calls `MembershipService.freezeForSuspended(customerId)`. |
| 15 | `Customer.trainerId` is the assigned trainer and is authoritative for the trainer's roster and medical access. `TrainerSubscription` records only the paid engagement period. Reassigning a trainer (via `TrainerService.assignCustomer` or `AdminService.reassignTrainer`) therefore moves medical access with it. |
| 16 | `StaffRole.ADMIN` is required for every `AdminService` operation **and every override operation**, including `SchedulingService.adminReserve`. `authenticate` rejects `active == false` and returns null; `SessionContext.requireRole` gates the UI. |
| 17 | `bookedCount(availabilityId)` counts `CONFIRMED` sessions only (`SessionDao.countByAvailability(id, CONFIRMED)`), so cancelling or completing a session frees capacity in its slot. |
| 18 | Only `CONFIRMED` sessions may become `COMPLETED`, `NO_SHOW` or `CANCELLED`; only `REQUESTED` may become `CONFIRMED`. `SchedulingService.canTransition` is the single authority and every transition operation calls it. |
| 19 | `Attendance` and `Payment` records are never deleted. An error is corrected with `AttendanceService.correctRecord` or, for money, a void plus a new payment — `AdminService.adjustPayment` records `previousAmount` so a receipt can always be reconciled. |

---

## Notes on scope

- **`Equipment` is a standalone aggregate.** Its DAO takes no foreign key and no other entity joins to
  it, so it is drawn in the main diagram as a declared class with no relationships — the same shape a
  class has in any static structure diagram when nothing references it. Recording per-item maintenance
  history would need a new `MaintenanceRecord` entity (deliberately out of scope for T1).
- **Enumerations are leaf types.** They appear as attribute types and never own relationships, so the
  diagram shows them declared but unconnected. Each is reachable from at least one service operation
  (`findByStatus`, `findByCondition`, `findByMethod`, `findByRole`, `activeSubscription`, `series`).
- **Generalization appears only against Java platform types** — see §7. No project class extends
  another project class, and the three candidate hierarchies (DAOs, services, controllers) were each
  examined and rejected there.
- **The 13 `*DaoImpl` classes repeat the same five CRUD signatures on purpose.** There is no
  `AbstractJdbcDao<T>` template base. Each impl differs only in its SQL and its entity type, the
  `DbConnection` interface already supplies the shared seam, and an abstract base would hide the
  13 explicit `..> DbConnection` edges that make the wiring checkable. `§3` is the **full DAO
  surface**; a service is expected to use a subset of it.
- **Association classes were considered and rejected.** `Payment` genuinely models a link — `amount`,
  `method` and `date` belong to the act of paying rather than to either endpoint — but it XORs three
  possible targets, and Mermaid has no association-class operator. Converting it would need three
  dashed links and would read worse than the current `Payment` class plus the XOR note. `Attendance`,
  `ProgressRecord` and `TrainerAvailability` each carry attributes intrinsic to themselves, so they
  are plain entities, not link classes. See `DECISIONS.md` 36.
- **The one place a real association class *would* be justified is medical-access auditing** — a
  `Trainer` × `MedicalProfile` link with a timestamp recording who viewed whose record. It is out of
  scope: the current design models the read as a `..>` dependency with the ownership rule enforced
  inside `MedicalProfileService.getForTrainer`, and there is no audit-log requirement. Adding one
  would mean a new `MedicalAccessLog` entity, its DAO and its table.
- **`m3fx` provides the theme and components**, not the domain. `M3Stylesheets.applyTo(scene, theme)`
  is public API; the overlay layer used by dialogs and snackbars currently lives in
  `io.m3fx.controls.internal` and is not exported — `MainApp` will need a public entry point for it.
