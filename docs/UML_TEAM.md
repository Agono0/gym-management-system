# UML — Team version (INTERNAL, not for submission)

> Same diagrams as `docs/UML.md`, color-coded per owner. Legend:
> <span>Abdelrhman</span> · <span>Ziad</span> · <span>Yousef</span> · <span>Abdel Raouf</span>
> (blue / green / amber / violet in the rendered diagram).

## 1. Layered architecture

```mermaid
flowchart TD
    APP[MainApp — Abdelrhman] --> CTRL[Controllers]
    CTRL --> SRV[Services]
    SRV --> DAO[DAOs]
    DAO --> DB[(SQLite gym.db)]
    UTIL[util — split owners] -. supports .-> SRV
    UTIL -. supports .-> CTRL
    UTIL -. supports .-> DAO
    M3FX[External: m3fx library — done] -. styles .-> CTRL
    style APP fill:#dbeafe
    style M3FX fill:#e5e7eb
```

## 2. Domain model

```mermaid
classDiagram
    class Customer {
        <<Abdelrhman>>
        +int id
        +String fullName
        +String phone
        +String email
        +String status
    }
    class Trainer {
        <<Ziad>>
        +int id
        +String fullName
        +String specialization
        +int experienceYears
        +String availability
    }
    class Membership {
        <<Ziad>>
        +int id
        +String plan
        +String startDate
        +String endDate
        +String status
    }
    class WorkoutProgram {
        <<Yousef>>
        +int id
        +String exercises
        +int version
    }
    class Session {
        <<Yousef>>
        +int id
        +String dateTime
        +int durationMin
    }
    class Equipment {
        <<Yousef>>
        +int id
        +String name
        +String category
        +String condition
        +String status
    }
    class Attendance {
        <<Abdel Raouf>>
        +int id
        +String date
        +String checkIn
        +String checkOut
    }
    class Payment {
        <<Abdel Raouf>>
        +int id
        +double amount
        +String method
        +String date
    }
    class ProgressRecord {
        <<Abdel Raouf>>
        +int id
        +String date
        +double weightKg
        +double bodyFatPct
        +String notes
    }
    Customer "1" --> "0..*" Membership : holds
    Customer "*" --> "0..1" Trainer : assigned to
    Membership "1" --> "0..*" Payment : paid by
    Customer "1" --> "0..*" WorkoutProgram : follows
    Trainer "1" --> "0..*" WorkoutProgram : creates
    Customer "1" --> "0..*" Session : books
    Trainer "1" --> "0..*" Session : coaches
    Customer "1" --> "0..*" Attendance : records
    Customer "1" --> "0..*" ProgressRecord : tracks
    classDef abdel fill:#dbeafe
    classDef ziad fill:#dcfce7
    classDef yousef fill:#fef3c7
    classDef raouf fill:#ede9fe
    class Customer:::abdel
    class Trainer:::ziad
    class Membership:::ziad
    class WorkoutProgram:::yousef
    class Session:::yousef
    class Equipment:::yousef
    class Attendance:::raouf
    class Payment:::raouf
    class ProgressRecord:::raouf
```

## 3. DAO layer

```mermaid
classDiagram
    class DbConnection {
        <<Abdelrhman>>
        +getConnection() Connection
        +initSchema() void
    }
    class DaoException {
        <<Abdelrhman>>
        +DaoException(String, Throwable)
    }
    class CustomerDao {
        <<Abdelrhman>>
        <<interface>>
        +save(Customer) int
        +findById(int) Customer
        +findAll() List
        +search(String) List
    }
    class CustomerDaoImpl {
        <<Abdelrhman>>
        +save(Customer) int
        +findById(int) Customer
        +findAll() List
        +search(String) List
    }
    class TrainerDao {
        <<Ziad>>
        <<interface>>
        +save(Trainer) int
        +findById(int) Trainer
        +findAll() List
    }
    class TrainerDaoImpl {
        <<Ziad>>
        +save(Trainer) int
        +findById(int) Trainer
        +findAll() List
    }
    class MembershipDao {
        <<Ziad>>
        <<interface>>
        +save(Membership) int
        +findExpiring(int) List
    }
    class MembershipDaoImpl {
        <<Ziad>>
        +save(Membership) int
        +findExpiring(int) List
    }
    class WorkoutProgramDao {
        <<Yousef>>
        <<interface>>
        +save(WorkoutProgram) int
        +findByCustomer(int) List
    }
    class WorkoutProgramDaoImpl {
        <<Yousef>>
        +save(WorkoutProgram) int
        +findByCustomer(int) List
    }
    class SessionDao {
        <<Yousef>>
        <<interface>>
        +save(Session) int
        +findByTrainer(int) List
        +findByDay(String) List
    }
    class SessionDaoImpl {
        <<Yousef>>
        +save(Session) int
        +findByTrainer(int) List
        +findByDay(String) List
    }
    class EquipmentDao {
        <<Yousef>>
        <<interface>>
        +save(Equipment) int
        +findAll() List
    }
    class EquipmentDaoImpl {
        <<Yousef>>
        +save(Equipment) int
        +findAll() List
    }
    class AttendanceDao {
        <<Abdel Raouf>>
        <<interface>>
        +save(Attendance) int
        +findByDay(String) List
    }
    class AttendanceDaoImpl {
        <<Abdel Raouf>>
        +save(Attendance) int
        +findByDay(String) List
    }
    class PaymentDao {
        <<Abdel Raouf>>
        <<interface>>
        +save(Payment) int
        +revenueBetween(String, String) double
    }
    class PaymentDaoImpl {
        <<Abdel Raouf>>
        +save(Payment) int
        +revenueBetween(String, String) double
    }
    class ProgressDao {
        <<Abdel Raouf>>
        <<interface>>
        +save(ProgressRecord) int
        +findByCustomer(int) List
    }
    class ProgressDaoImpl {
        <<Abdel Raouf>>
        +save(ProgressRecord) int
        +findByCustomer(int) List
    }
    CustomerDao <|.. CustomerDaoImpl : implements
    TrainerDao <|.. TrainerDaoImpl : implements
    MembershipDao <|.. MembershipDaoImpl : implements
    WorkoutProgramDao <|.. WorkoutProgramDaoImpl : implements
    SessionDao <|.. SessionDaoImpl : implements
    EquipmentDao <|.. EquipmentDaoImpl : implements
    AttendanceDao <|.. AttendanceDaoImpl : implements
    PaymentDao <|.. PaymentDaoImpl : implements
    ProgressDao <|.. ProgressDaoImpl : implements
    CustomerDaoImpl --> DbConnection : uses
    TrainerDaoImpl --> DbConnection : uses
    MembershipDaoImpl --> DbConnection : uses
    WorkoutProgramDaoImpl --> DbConnection : uses
    SessionDaoImpl --> DbConnection : uses
    EquipmentDaoImpl --> DbConnection : uses
    AttendanceDaoImpl --> DbConnection : uses
    PaymentDaoImpl --> DbConnection : uses
    ProgressDaoImpl --> DbConnection : uses
    classDef abdel fill:#dbeafe
    classDef ziad fill:#dcfce7
    classDef yousef fill:#fef3c7
    classDef raouf fill:#ede9fe
    class DbConnection:::abdel
    class DaoException:::abdel
    class CustomerDao:::abdel
    class CustomerDaoImpl:::abdel
    class TrainerDao:::ziad
    class TrainerDaoImpl:::ziad
    class MembershipDao:::ziad
    class MembershipDaoImpl:::ziad
    class WorkoutProgramDao:::yousef
    class WorkoutProgramDaoImpl:::yousef
    class SessionDao:::yousef
    class SessionDaoImpl:::yousef
    class EquipmentDao:::yousef
    class EquipmentDaoImpl:::yousef
    class AttendanceDao:::raouf
    class AttendanceDaoImpl:::raouf
    class PaymentDao:::raouf
    class PaymentDaoImpl:::raouf
    class ProgressDao:::raouf
    class ProgressDaoImpl:::raouf
```

## 4. Service layer

```mermaid
classDiagram
    class CustomerService {
        <<Abdelrhman>>
        +validate(Customer) List
        +search(String) List
        +setStatus(int, String) void
    }
    class TrainerService {
        <<Ziad>>
        +checkAvailability(int, String) bool
        +assignCustomer(int, int) void
    }
    class MembershipService {
        <<Ziad>>
        +sell(int, String) Membership
        +renew(int) void
        +freeze(int) void
        +cancel(int) void
        +expiryReminders() List
    }
    class WorkoutService {
        <<Yousef>>
        +buildProgram(int, String) WorkoutProgram
        +newVersion(int) WorkoutProgram
    }
    class SchedulingService {
        <<Yousef>>
        +book(int, int, String) Session
        +detectConflicts(int, String) List
    }
    class EquipmentService {
        <<Yousef>>
        +changeStatus(int, String) void
        +logMaintenance(int, String) void
    }
    class AttendanceService {
        <<Abdel Raouf>>
        +checkIn(int) Attendance
        +checkOut(int) void
        +dailyReport(String) List
    }
    class PaymentService {
        <<Abdel Raouf>>
        +record(Payment) int
        +balance(int) double
        +revenueSummary() String
    }
    class ProgressService {
        <<Abdel Raouf>>
        +aggregate(int) List
        +chartData(int) List
    }
    CustomerService --> CustomerDao : uses
    TrainerService --> TrainerDao : uses
    MembershipService --> MembershipDao : uses
    WorkoutService --> WorkoutProgramDao : uses
    SchedulingService --> SessionDao : uses
    EquipmentService --> EquipmentDao : uses
    AttendanceService --> AttendanceDao : uses
    PaymentService --> PaymentDao : uses
    ProgressService --> ProgressDao : uses
    MembershipService --> PaymentDao : uses
    SchedulingService --> TrainerDao : uses
    classDef abdel fill:#dbeafe
    classDef ziad fill:#dcfce7
    classDef yousef fill:#fef3c7
    classDef raouf fill:#ede9fe
    class CustomerService:::abdel
    class TrainerService:::ziad
    class MembershipService:::ziad
    class WorkoutService:::yousef
    class SchedulingService:::yousef
    class EquipmentService:::yousef
    class AttendanceService:::raouf
    class PaymentService:::raouf
    class ProgressService:::raouf
```

## 5. Controllers

```mermaid
classDiagram
    class MainApp {
        <<Abdelrhman>>
        +start(Stage) void
        +main(String[]) void
    }
    class DashboardController {
        <<Abdel Raouf>>
        +initialize() void
    }
    class CustomerListController {
        <<Abdelrhman>>
        +initialize() void
        +onSearch() void
    }
    class CustomerProfileController {
        <<Abdelrhman>>
        +showCustomer(int) void
        +onSave() void
    }
    class TrainerListController {
        <<Ziad>>
        +initialize() void
    }
    class TrainerProfileController {
        <<Ziad>>
        +showTrainer(int) void
        +onSave() void
    }
    class MembershipController {
        <<Ziad>>
        +onSell() void
        +onRenew() void
        +onFreeze() void
    }
    class WorkoutController {
        <<Yousef>>
        +onBuild() void
        +onAssign() void
    }
    class ScheduleController {
        <<Yousef>>
        +onBook() void
    }
    class EquipmentController {
        <<Yousef>>
        +onStatusChange() void
    }
    class AttendanceController {
        <<Abdel Raouf>>
        +onCheckIn() void
        +onCheckOut() void
    }
    class PaymentController {
        <<Abdel Raouf>>
        +onRecord() void
    }
    class ProgressController {
        <<Abdel Raouf>>
        +showProgress(int) void
    }
    MainApp --> DashboardController : opens
    DashboardController --> CustomerListController : navigates
    DashboardController --> ScheduleController : navigates
    CustomerListController --> CustomerProfileController : opens
    TrainerListController --> TrainerProfileController : opens
    CustomerProfileController --> CustomerService : uses
    CustomerProfileController --> MembershipService : uses
    TrainerProfileController --> TrainerService : uses
    MembershipController --> MembershipService : uses
    WorkoutController --> WorkoutService : uses
    ScheduleController --> SchedulingService : uses
    EquipmentController --> EquipmentService : uses
    AttendanceController --> AttendanceService : uses
    PaymentController --> PaymentService : uses
    ProgressController --> ProgressService : uses
    DashboardController --> AttendanceService : uses
    DashboardController --> PaymentService : uses
    classDef abdel fill:#dbeafe
    classDef ziad fill:#dcfce7
    classDef yousef fill:#fef3c7
    classDef raouf fill:#ede9fe
    class MainApp:::abdel
    class DashboardController:::raouf
    class CustomerListController:::abdel
    class CustomerProfileController:::abdel
    class TrainerListController:::ziad
    class TrainerProfileController:::ziad
    class MembershipController:::ziad
    class WorkoutController:::yousef
    class ScheduleController:::yousef
    class EquipmentController:::yousef
    class AttendanceController:::raouf
    class PaymentController:::raouf
    class ProgressController:::raouf
```

## 6. Who does what (checklist)

| Area | Owner | Status |
|---|---|---|
| Foundation: `DbConnection`, `DaoException`, `Validators`, `FxUtils`, `MainApp` shell, m3fx already built | Abdelrhman | In progress |
| `Customer` + DAO + service + list/profile screens | Abdelrhman | To do |
| `Trainer`, `Membership` + DAOs + services + screens | Ziad | To do |
| `WorkoutProgram`, `Session`, `Equipment` + DAOs + services + screens; `DateUtils` | Yousef | To do |
| `Attendance`, `Payment`, `ProgressRecord` + DAOs + services + screens; dashboard; `ChartUtils` | Abdel Raouf | To do |

Rules: JavaFX-first per vertical slice, then each owner ports their own modules to the `swing` branch. Update the Status column as work moves.
