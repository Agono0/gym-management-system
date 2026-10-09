# UML — Gym Management System (JavaFX) — Official submission diagram

> Main diagram first, detailed per-layer zoom-ins below. Planned classes included —
> code follows this design. `m3fx` is an external library dependency, not project code.
> Notation: `-` private, `+` public, `#` protected, `o--` shared aggregation, `..>` dependency,
> `..|>` realization. Every class carries its description inside the diagram.
> All entity methods (getters, setters, constructors, toString, equals, hashCode) are shown.

```mermaid
classDiagram
    direction TB
    class MainApp {
        +start(Stage) void
        +main(String[]) void
    }
    class Customer {
        -int id
        -String fullName
        -String phone
        -String email
        -String address
        -String dateOfBirth
        -String gender
        -String emergencyName
        -String emergencyPhone
        -CustomerStatus status
        -String joinDate
        +Customer()
        +Customer(int, String, String, String, String, String, String, String, String, CustomerStatus, String)
        +getId() int
        +setId(int) void
        +getFullName() String
        +setFullName(String) void
        +getPhone() String
        +setPhone(String) void
        +getEmail() String
        +setEmail(String) void
        +getAddress() String
        +setAddress(String) void
        +getDateOfBirth() String
        +setDateOfBirth(String) void
        +getGender() String
        +setGender(String) void
        +getEmergencyName() String
        +setEmergencyName(String) void
        +getEmergencyPhone() String
        +setEmergencyPhone(String) void
        +getStatus() CustomerStatus
        +setStatus(CustomerStatus) void
        +getJoinDate() String
        +setJoinDate(String) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Trainer {
        -int id
        -String fullName
        -String phone
        -String email
        -String specialization
        -int experienceYears
        -String certifications
        -String availability
        -String hireDate
        -double defaultRate
        +Trainer()
        +Trainer(int, String, String, String, String, int, String, String, String, double)
        +getId() int
        +setId(int) void
        +getFullName() String
        +setFullName(String) void
        +getPhone() String
        +setPhone(String) void
        +getEmail() String
        +setEmail(String) void
        +getSpecialization() String
        +setSpecialization(String) void
        +getExperienceYears() int
        +setExperienceYears(int) void
        +getCertifications() String
        +setCertifications(String) void
        +getAvailability() String
        +setAvailability(String) void
        +getHireDate() String
        +setHireDate(String) void
        +getDefaultRate() double
        +setDefaultRate(double) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Membership {
        -int id
        -int customerId
        -String plan
        -double price
        -String startDate
        -String endDate
        -MembershipStatus status
        +Membership()
        +Membership(int, int, String, double, String, String, MembershipStatus)
        +getId() int
        +setId(int) void
        +getCustomerId() int
        +setCustomerId(int) void
        +getPlan() String
        +setPlan(String) void
        +getPrice() double
        +setPrice(double) void
        +getStartDate() String
        +setStartDate(String) void
        +getEndDate() String
        +setEndDate(String) void
        +getStatus() MembershipStatus
        +setStatus(MembershipStatus) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class WorkoutProgram {
        -int id
        -int customerId
        -int creatorTrainerId
        -String name
        -String exercises
        -int version
        -String createdDate
        +WorkoutProgram()
        +WorkoutProgram(int, int, int, String, String, int, String)
        +getId() int
        +setId(int) void
        +getCustomerId() int
        +setCustomerId(int) void
        +getCreatorTrainerId() int
        +setCreatorTrainerId(int) void
        +getName() String
        +setName(String) void
        +getExercises() String
        +setExercises(String) void
        +getVersion() int
        +setVersion(int) void
        +getCreatedDate() String
        +setCreatedDate(String) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Session {
        -int id
        -int customerId
        -int trainerId
        -String dateTime
        -int durationMin
        -SessionStatus status
        -Integer subscriptionId
        -double price
        -String cancelReason
        -String sessionNotes
        +Session()
        +Session(int, int, int, String, int, SessionStatus, Integer, double, String, String)
        +getId() int
        +setId(int) void
        +getCustomerId() int
        +setCustomerId(int) void
        +getTrainerId() int
        +setTrainerId(int) void
        +getDateTime() String
        +setDateTime(String) void
        +getDurationMin() int
        +setDurationMin(int) void
        +getStatus() SessionStatus
        +setStatus(SessionStatus) void
        +getSubscriptionId() Integer
        +setSubscriptionId(Integer) void
        +getPrice() double
        +setPrice(double) void
        +getCancelReason() String
        +setCancelReason(String) void
        +getSessionNotes() String
        +setSessionNotes(String) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Equipment {
        -int id
        -String name
        -String category
        -String purchaseDate
        -EquipmentCondition condition
        -EquipmentStatus status
        -String lastMaintenance
        +Equipment()
        +Equipment(int, String, String, String, EquipmentCondition, EquipmentStatus, String)
        +getId() int
        +setId(int) void
        +getName() String
        +setName(String) void
        +getCategory() String
        +setCategory(String) void
        +getPurchaseDate() String
        +setPurchaseDate(String) void
        +getCondition() EquipmentCondition
        +setCondition(EquipmentCondition) void
        +getStatus() EquipmentStatus
        +setStatus(EquipmentStatus) void
        +getLastMaintenance() String
        +setLastMaintenance(String) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Attendance {
        -int id
        -int customerId
        -String date
        -String checkIn
        -String checkOut
        +Attendance()
        +Attendance(int, int, String, String, String)
        +getId() int
        +setId(int) void
        +getCustomerId() int
        +setCustomerId(int) void
        +getDate() String
        +setDate(String) void
        +getCheckIn() String
        +setCheckIn(String) void
        +getCheckOut() String
        +setCheckOut(String) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Payment {
        -int id
        -int customerId
        -int membershipId
        -double amount
        -PaymentMethod method
        -String date
        -String receiptNo
        +Payment()
        +Payment(int, int, int, double, PaymentMethod, String, String)
        +getId() int
        +setId(int) void
        +getCustomerId() int
        +setCustomerId(int) void
        +getMembershipId() int
        +setMembershipId(int) void
        +getAmount() double
        +setAmount(double) void
        +getMethod() PaymentMethod
        +setMethod(PaymentMethod) void
        +getDate() String
        +setDate(String) void
        +getReceiptNo() String
        +setReceiptNo(String) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class ProgressRecord {
        -int id
        -int customerId
        -String date
        -double weightKg
        -double bodyFatPct
        -String measurements
        -String notes
        +ProgressRecord()
        +ProgressRecord(int, int, String, double, double, String, String)
        +getId() int
        +setId(int) void
        +getCustomerId() int
        +setCustomerId(int) void
        +getDate() String
        +setDate(String) void
        +getWeightKg() double
        +setWeightKg(double) void
        +getBodyFatPct() double
        +setBodyFatPct(double) void
        +getMeasurements() String
        +setMeasurements(String) void
        +getNotes() String
        +setNotes(String) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class MedicalProfile {
        -int id
        -int customerId
        -String bloodType
        -String conditions
        -String allergies
        -String medications
        -String injuries
        -String doctorName
        -String doctorPhone
        -String notes
        -String updatedDate
        +MedicalProfile()
        +MedicalProfile(int, int, String, String, String, String, String, String, String, String, String)
        +getId() int
        +setId(int) void
        +getCustomerId() int
        +setCustomerId(int) void
        +getBloodType() String
        +setBloodType(String) void
        +getConditions() String
        +setConditions(String) void
        +getAllergies() String
        +setAllergies(String) void
        +getMedications() String
        +setMedications(String) void
        +getInjuries() String
        +setInjuries(String) void
        +getDoctorName() String
        +setDoctorName(String) void
        +getDoctorPhone() String
        +setDoctorPhone(String) void
        +getNotes() String
        +setNotes(String) void
        +getUpdatedDate() String
        +setUpdatedDate(String) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Administrator {
        -int id
        -String fullName
        -String username
        -String passwordHash
        -StaffRole role
        -String phone
        -String email
        -Boolean active
        -String createdDate
        +Administrator()
        +Administrator(int, String, String, String, StaffRole, String, String, Boolean, String)
        +getId() int
        +setId(int) void
        +getFullName() String
        +setFullName(String) void
        +getUsername() String
        +setUsername(String) void
        +getPasswordHash() String
        +setPasswordHash(String) void
        +getRole() StaffRole
        +setRole(StaffRole) void
        +getPhone() String
        +setPhone(String) void
        +getEmail() String
        +setEmail(String) void
        +getActive() Boolean
        +setActive(Boolean) void
        +getCreatedDate() String
        +setCreatedDate(String) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class TrainerSubscription {
        -int id
        -int customerId
        -int trainerId
        -String startDate
        -String endDate
        -double rate
        -SubscriptionStatus status
        +TrainerSubscription()
        +TrainerSubscription(int, int, int, String, String, double, SubscriptionStatus)
        +getId() int
        +setId(int) void
        +getCustomerId() int
        +setCustomerId(int) void
        +getTrainerId() int
        +setTrainerId(int) void
        +getStartDate() String
        +setStartDate(String) void
        +getEndDate() String
        +setEndDate(String) void
        +getRate() double
        +setRate(double) void
        +getStatus() SubscriptionStatus
        +setStatus(SubscriptionStatus) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class TrainerAvailability {
        -int id
        -int trainerId
        -String date
        -String startTime
        -String endTime
        -int maxCustomers
        -AvailabilityStatus status
        +TrainerAvailability()
        +TrainerAvailability(int, int, String, String, String, int, AvailabilityStatus)
        +getId() int
        +setId(int) void
        +getTrainerId() int
        +setTrainerId(int) void
        +getDate() String
        +setDate(String) void
        +getStartTime() String
        +setStartTime(String) void
        +getEndTime() String
        +setEndTime(String) void
        +getMaxCustomers() int
        +setMaxCustomers(int) void
        +getStatus() AvailabilityStatus
        +setStatus(AvailabilityStatus) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class CustomerStatus {
        <<enumeration>>
        ACTIVE
        INACTIVE
        SUSPENDED
        +values() CustomerStatus[]
        +valueOf(String) CustomerStatus
    }
    class MembershipStatus {
        <<enumeration>>
        ACTIVE
        FROZEN
        EXPIRED
        CANCELLED
        +values() MembershipStatus[]
        +valueOf(String) MembershipStatus
    }
    class SessionStatus {
        <<enumeration>>
        REQUESTED
        CONFIRMED
        COMPLETED
        CANCELLED
        +values() SessionStatus[]
        +valueOf(String) SessionStatus
    }
    class SubscriptionStatus {
        <<enumeration>>
        ACTIVE
        EXPIRED
        CANCELLED
        +values() SubscriptionStatus[]
        +valueOf(String) SubscriptionStatus
    }
    class AvailabilityStatus {
        <<enumeration>>
        OPEN
        FULL
        CLOSED
        +values() AvailabilityStatus[]
        +valueOf(String) AvailabilityStatus
    }
    class StaffRole {
        <<enumeration>>
        ADMIN
        MANAGER
        STAFF
        +values() StaffRole[]
        +valueOf(String) StaffRole
    }
    class PaymentMethod {
        <<enumeration>>
        CASH
        CARD
        TRANSFER
        +values() PaymentMethod[]
        +valueOf(String) PaymentMethod
    }
    class EquipmentStatus {
        <<enumeration>>
        AVAILABLE
        IN_MAINTENANCE
        RETIRED
        +values() EquipmentStatus[]
        +valueOf(String) EquipmentStatus
    }
    class EquipmentCondition {
        <<enumeration>>
        NEW
        GOOD
        FAIR
        POOR
        +values() EquipmentCondition[]
        +valueOf(String) EquipmentCondition
    }
    Customer "1" o-- "0..*" Membership : holds
    Customer "1" o-- "0..*" WorkoutProgram : follows
    Customer "1" o-- "0..*" Attendance : records
    Customer "1" o-- "0..*" ProgressRecord : tracks
    Customer "*" --> "0..1" Trainer : assigned to
    Customer "1" --> "0..*" Session : books
    Customer "1" *-- "1" MedicalProfile : has
    Customer "1" --> "0..*" TrainerSubscription : subscribes
    Customer "1" --> "0..*" Payment : makes
    Trainer --> MedicalProfile : views
    Trainer "1" --> "0..*" WorkoutProgram : creates
    Trainer "1" --> "0..*" Session : coaches
    Trainer "1" --> "0..*" TrainerSubscription : offers
    Trainer "1" --> "0..*" TrainerAvailability : publishes
    TrainerSubscription "1" o-- "0..*" Session : covers
    Payment "*" --> "1" Membership : pays for
    Administrator ..> MainApp : administers
    Administrator ..> Customer : manages
    Administrator ..> Trainer : manages
    Administrator ..> Payment : adjusts
    note for MainApp "Entry point, window, nav shell, theme"
    note for Customer "Member: info, contact, status, links"
    note for Trainer "Coach: info, specialization, availability"
    note for Membership "Plan with start and end dates"
    note for WorkoutProgram "Exercise list with version, creator"
    note for Session "Booked slot, customer plus trainer, with cancel reason and notes"
    note for TrainerSubscription "Period engagement at flat trainer rate"
    note for TrainerAvailability "Open days, caps, hourly slots"
    note for Equipment "Inventory item, condition, status"
    note for Attendance "Daily check-in and check-out record with customerId"
    note for Payment "Amount, method and date; linked to membership and customer"
    note for ProgressRecord "Dated body metrics and benchmarks"
    note for MedicalProfile "Blood type, conditions, meds, doctor"
    note for Administrator "Login identity, role, active flag; manages all entities"
    note for CustomerStatus "Active, inactive, suspended"
    note for MembershipStatus "Active, frozen, expired, cancelled"
    note for SessionStatus "Requested, confirmed, completed, cancelled"
    note for SubscriptionStatus "Active, expired, cancelled"
    note for AvailabilityStatus "Open, full, closed"
    note for StaffRole "Admin, manager, staff"
    note for PaymentMethod "Cash, card, transfer"
    note for EquipmentStatus "Available, maintenance, retired"
    note for EquipmentCondition "New, good, fair, poor"
```


## Class descriptions

### Entities

| Class | Description |
|---|---|
| `Customer` | Gym member; fields: id, fullName, phone, email, address, dateOfBirth, gender, emergencyName, emergencyPhone, status (enum), joinDate; links to membership, trainer, program |
| `Trainer` | Coach; fields: id, fullName, phone, email, specialization, experienceYears, certifications, availability, hireDate, defaultRate |
| `Membership` | Plan; fields: id, customerId, plan, price, startDate, endDate, status (active, frozen, expired, cancelled) |
| `WorkoutProgram` | Exercise list; fields: id, customerId, creatorTrainerId, name, exercises, version, createdDate |
| `Session` | Training slot; fields: id, customerId, trainerId, dateTime, durationMin, status, subscriptionId (nullable), price, cancelReason, sessionNotes |
| `Equipment` | Inventory item; fields: id, name, category, purchaseDate, condition (enum), status (enum), lastMaintenance |
| `Attendance` | One check-in/out record per customer per day; fields: id, customerId, date, checkIn, checkOut |
| `Payment` | fields: id, customerId, membershipId, amount, method, date, receiptNo; linked to a membership |
| `ProgressRecord` | fields: id, customerId, date, weightKg, bodyFatPct, measurements, notes |
| `MedicalProfile` | Blood type, conditions, allergies, medications, injuries, doctor, notes; one per customer |
| `Administrator` | Login identity with role (ADMIN/MANAGER/STAFF); manages all entities via overrides |
| `TrainerSubscription` | Period engagement at flat trainer-set rate; status uses SubscriptionStatus enum |
| `TrainerAvailability` | Open days, daily caps, hourly slots set by trainer; status uses AvailabilityStatus enum |

### Enumerations

| Enum | Literals |
|---|---|
| `CustomerStatus` | ACTIVE, INACTIVE, SUSPENDED |
| `MembershipStatus` | ACTIVE, FROZEN, EXPIRED, CANCELLED |
| `SessionStatus` | REQUESTED, CONFIRMED, COMPLETED, CANCELLED |
| `SubscriptionStatus` | ACTIVE, EXPIRED, CANCELLED |
| `AvailabilityStatus` | OPEN, FULL, CLOSED |
| `StaffRole` | ADMIN, MANAGER, STAFF |
| `PaymentMethod` | CASH, CARD, TRANSFER |
| `EquipmentStatus` | AVAILABLE, IN_MAINTENANCE, RETIRED |
| `EquipmentCondition` | NEW, GOOD, FAIR, POOR |

### Data access

| Class | Description |
|---|---|
| `DbConnection` | SQLite connection factory; creates `gym.db` and initializes the schema |
| `DaoException` | Unchecked wrapper for `SQLException`; translated to friendly messages upstream |
| `CustomerDao` / `CustomerDaoImpl` | Persist, load and search customers |
| `TrainerDao` / `TrainerDaoImpl` | Persist and load trainers |
| `MembershipDao` / `MembershipDaoImpl` | Persist memberships; query expiring ones for reminders |
| `WorkoutProgramDao` / `WorkoutProgramDaoImpl` | Persist programs; list per customer |
| `SessionDao` / `SessionDaoImpl` | Persist sessions; query by trainer, by day, by customer, by status |
| `EquipmentDao` / `EquipmentDaoImpl` | Persist and list equipment |
| `AttendanceDao` / `AttendanceDaoImpl` | Persist check-ins; daily attendance lists; query by customer |
| `PaymentDao` / `PaymentDaoImpl` | Persist payments; revenue sums over date ranges; query by customer |
| `ProgressDao` / `ProgressDaoImpl` | Persist metric records; history per customer |
| `MedicalProfileDao` / `MedicalProfileDaoImpl` | Persist profiles; lookup per customer |
| `AdminDao` / `AdminDaoImpl` | Persist operators; lookup by username |
| `TrainerSubscriptionDao` / `TrainerSubscriptionDaoImpl` | Persist engagements; active lookup |
| `TrainerAvailabilityDao` / `TrainerAvailabilityDaoImpl` | Persist slots, caps, open days; query by trainer and date |

### Services (validation + business rules, no JavaFX, no SQL)

| Class | Description |
|---|---|
| `CustomerService` | Customer validation, search/filter, status rules |
| `TrainerService` | Availability, rates, assign, medical view |
| `MembershipService` | Sell, renew, freeze, cancel, expiry reminders |
| `WorkoutService` | Program building rules, per-customer versioning |
| `SchedulingService` | Booking rules (slot, cap, conflicts), session reservation, subscribe, subscription pricing; distinguishes subscription (trainer) vs membership (gym access) |
| `EquipmentService` | Status transitions and maintenance-log rules |
| `PaymentService` | Balances, receipt data, revenue summary |
| `AttendanceService` | Check-in/out rules, daily report data |
| `ProgressService` | Metric aggregation for charts |
| `MedicalProfileService` | Medical CRUD, lookup by customer |
| `AdminService` | Auth, staff, edit-any/reassign/adjust/void overrides; bypasses ownership |

### UI (each controller backed by a matching FXML file)

| Class | Description |
|---|---|
| `MainApp` | Entry point: builds the window, nav shell, theme and overlay layer |
| `LoginController` | Login screen: authenticates admin via AdminService |
| `DashboardController` | Main dashboard: active members, today's sessions, revenue snapshot |
| `CustomerListController` | Searchable/filterable customer list |
| `CustomerProfileController` | Full profile: info, membership, trainer, program, schedule, progress |
| `TrainerListController` | Trainer roster with availability |
| `TrainerProfileController` | Trainer details, assigned customers, schedule |
| `MembershipController` | Plans; sell, renew, freeze, cancel |
| `WorkoutController` | Program builder and assignment to customers |
| `ScheduleController` | Agenda, booking dialog, session reservation, subscribe-to-trainer flow |
| `AvailabilityController` | Trainer calendar, daily caps, hourly slots |
| `EquipmentController` | Inventory list, status changes, maintenance log |
| `AttendanceController` | Check-in/out screen and daily report |
| `PaymentController` | Payment recording, receipts, balances |
| `ProgressController` | Body-metric charts per customer |
| `MedicalProfileController` | Medical profile view/edit per customer |

### Utilities

| Class | Description |
|---|---|
| `Validators` | Phone/email/number/date validation used by services |
| `DateUtils` | Membership periods, session slot math, date formatting |
| `FxUtils` | Friendly dialogs, alerts, scene switching, snackbar helper |
| `ChartUtils` | Progress-chart dataset builders |

---

## Detailed views (per-layer zoom-ins of the diagram above)

### A. Layered architecture

```mermaid
flowchart TD
    APP[MainApp<br/>entry point, nav shell] --> CTRL[Controllers<br/>14 FXML screens]
    CTRL --> SRV[Services<br/>validation + business rules]
    SRV --> DAO[DAOs<br/>SQLite persistence]
    DAO --> DB[(SQLite<br/>gym.db)]
    UTIL[util<br/>DbConnection, Validators<br/>DateUtils, FxUtils, ChartUtils] -. supports .-> SRV
    UTIL -. supports .-> CTRL
    UTIL -. supports .-> DAO
    M3FX[External: m3fx library<br/>M3 theme + components] -. styles .-> CTRL
```

Layer rules: controllers never contain SQL; services and DAOs never import JavaFX.

### B. Domain model zoom-in

```mermaid
classDiagram
    direction TB
    class Customer {
        -int id
        -String fullName
        -String phone
        -String email
        -String address
        -String dateOfBirth
        -String gender
        -String emergencyName
        -String emergencyPhone
        -CustomerStatus status
        -String joinDate
        +Customer()
        +Customer(int, String, String, String, String, String, String, String, String, CustomerStatus, String)
        +getId() int
        +setId(int) void
        +getFullName() String
        +setFullName(String) void
        +getPhone() String
        +setPhone(String) void
        +getEmail() String
        +setEmail(String) void
        +getAddress() String
        +setAddress(String) void
        +getDateOfBirth() String
        +setDateOfBirth(String) void
        +getGender() String
        +setGender(String) void
        +getEmergencyName() String
        +setEmergencyName(String) void
        +getEmergencyPhone() String
        +setEmergencyPhone(String) void
        +getStatus() CustomerStatus
        +setStatus(CustomerStatus) void
        +getJoinDate() String
        +setJoinDate(String) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Trainer {
        -int id
        -String fullName
        -String phone
        -String email
        -String specialization
        -int experienceYears
        -String certifications
        -String availability
        -String hireDate
        -double defaultRate
        +Trainer()
        +Trainer(int, String, String, String, String, int, String, String, String, double)
        +getId() int
        +setId(int) void
        +getFullName() String
        +setFullName(String) void
        +getPhone() String
        +setPhone(String) void
        +getEmail() String
        +setEmail(String) void
        +getSpecialization() String
        +setSpecialization(String) void
        +getExperienceYears() int
        +setExperienceYears(int) void
        +getCertifications() String
        +setCertifications(String) void
        +getAvailability() String
        +setAvailability(String) void
        +getHireDate() String
        +setHireDate(String) void
        +getDefaultRate() double
        +setDefaultRate(double) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Membership {
        -int id
        -int customerId
        -String plan
        -double price
        -String startDate
        -String endDate
        -MembershipStatus status
        +Membership()
        +Membership(int, int, String, double, String, String, MembershipStatus)
        +getId() int
        +setId(int) void
        +getCustomerId() int
        +setCustomerId(int) void
        +getPlan() String
        +setPlan(String) void
        +getPrice() double
        +setPrice(double) void
        +getStartDate() String
        +setStartDate(String) void
        +getEndDate() String
        +setEndDate(String) void
        +getStatus() MembershipStatus
        +setStatus(MembershipStatus) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class WorkoutProgram {
        -int id
        -int customerId
        -int creatorTrainerId
        -String name
        -String exercises
        -int version
        -String createdDate
        +WorkoutProgram()
        +WorkoutProgram(int, int, int, String, String, int, String)
        +getId() int
        +setId(int) void
        +getCustomerId() int
        +setCustomerId(int) void
        +getCreatorTrainerId() int
        +setCreatorTrainerId(int) void
        +getName() String
        +setName(String) void
        +getExercises() String
        +setExercises(String) void
        +getVersion() int
        +setVersion(int) void
        +getCreatedDate() String
        +setCreatedDate(String) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Session {
        -int id
        -int customerId
        -int trainerId
        -String dateTime
        -int durationMin
        -SessionStatus status
        -Integer subscriptionId
        -double price
        -String cancelReason
        -String sessionNotes
        +Session()
        +Session(int, int, int, String, int, SessionStatus, Integer, double, String, String)
        +getId() int
        +setId(int) void
        +getCustomerId() int
        +setCustomerId(int) void
        +getTrainerId() int
        +setTrainerId(int) void
        +getDateTime() String
        +setDateTime(String) void
        +getDurationMin() int
        +setDurationMin(int) void
        +getStatus() SessionStatus
        +setStatus(SessionStatus) void
        +getSubscriptionId() Integer
        +setSubscriptionId(Integer) void
        +getPrice() double
        +setPrice(double) void
        +getCancelReason() String
        +setCancelReason(String) void
        +getSessionNotes() String
        +setSessionNotes(String) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Equipment {
        -int id
        -String name
        -String category
        -String purchaseDate
        -EquipmentCondition condition
        -EquipmentStatus status
        -String lastMaintenance
        +Equipment()
        +Equipment(int, String, String, String, EquipmentCondition, EquipmentStatus, String)
        +getId() int
        +setId(int) void
        +getName() String
        +setName(String) void
        +getCategory() String
        +setCategory(String) void
        +getPurchaseDate() String
        +setPurchaseDate(String) void
        +getCondition() EquipmentCondition
        +setCondition(EquipmentCondition) void
        +getStatus() EquipmentStatus
        +setStatus(EquipmentStatus) void
        +getLastMaintenance() String
        +setLastMaintenance(String) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Attendance {
        -int id
        -int customerId
        -String date
        -String checkIn
        -String checkOut
        +Attendance()
        +Attendance(int, int, String, String, String)
        +getId() int
        +setId(int) void
        +getCustomerId() int
        +setCustomerId(int) void
        +getDate() String
        +setDate(String) void
        +getCheckIn() String
        +setCheckIn(String) void
        +getCheckOut() String
        +setCheckOut(String) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Payment {
        -int id
        -int customerId
        -int membershipId
        -double amount
        -PaymentMethod method
        -String date
        -String receiptNo
        +Payment()
        +Payment(int, int, int, double, PaymentMethod, String, String)
        +getId() int
        +setId(int) void
        +getCustomerId() int
        +setCustomerId(int) void
        +getMembershipId() int
        +setMembershipId(int) void
        +getAmount() double
        +setAmount(double) void
        +getMethod() PaymentMethod
        +setMethod(PaymentMethod) void
        +getDate() String
        +setDate(String) void
        +getReceiptNo() String
        +setReceiptNo(String) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class ProgressRecord {
        -int id
        -int customerId
        -String date
        -double weightKg
        -double bodyFatPct
        -String measurements
        -String notes
        +ProgressRecord()
        +ProgressRecord(int, int, String, double, double, String, String)
        +getId() int
        +setId(int) void
        +getCustomerId() int
        +setCustomerId(int) void
        +getDate() String
        +setDate(String) void
        +getWeightKg() double
        +setWeightKg(double) void
        +getBodyFatPct() double
        +setBodyFatPct(double) void
        +getMeasurements() String
        +setMeasurements(String) void
        +getNotes() String
        +setNotes(String) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class MedicalProfile {
        -int id
        -int customerId
        -String bloodType
        -String conditions
        -String allergies
        -String medications
        -String injuries
        -String doctorName
        -String doctorPhone
        -String notes
        -String updatedDate
        +MedicalProfile()
        +MedicalProfile(int, int, String, String, String, String, String, String, String, String, String)
        +getId() int
        +setId(int) void
        +getCustomerId() int
        +setCustomerId(int) void
        +getBloodType() String
        +setBloodType(String) void
        +getConditions() String
        +setConditions(String) void
        +getAllergies() String
        +setAllergies(String) void
        +getMedications() String
        +setMedications(String) void
        +getInjuries() String
        +setInjuries(String) void
        +getDoctorName() String
        +setDoctorName(String) void
        +getDoctorPhone() String
        +setDoctorPhone(String) void
        +getNotes() String
        +setNotes(String) void
        +getUpdatedDate() String
        +setUpdatedDate(String) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class Administrator {
        -int id
        -String fullName
        -String username
        -String passwordHash
        -StaffRole role
        -String phone
        -String email
        -Boolean active
        -String createdDate
        +Administrator()
        +Administrator(int, String, String, String, StaffRole, String, String, Boolean, String)
        +getId() int
        +setId(int) void
        +getFullName() String
        +setFullName(String) void
        +getUsername() String
        +setUsername(String) void
        +getPasswordHash() String
        +setPasswordHash(String) void
        +getRole() StaffRole
        +setRole(StaffRole) void
        +getPhone() String
        +setPhone(String) void
        +getEmail() String
        +setEmail(String) void
        +getActive() Boolean
        +setActive(Boolean) void
        +getCreatedDate() String
        +setCreatedDate(String) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class TrainerSubscription {
        -int id
        -int customerId
        -int trainerId
        -String startDate
        -String endDate
        -double rate
        -SubscriptionStatus status
        +TrainerSubscription()
        +TrainerSubscription(int, int, int, String, String, double, SubscriptionStatus)
        +getId() int
        +setId(int) void
        +getCustomerId() int
        +setCustomerId(int) void
        +getTrainerId() int
        +setTrainerId(int) void
        +getStartDate() String
        +setStartDate(String) void
        +getEndDate() String
        +setEndDate(String) void
        +getRate() double
        +setRate(double) void
        +getStatus() SubscriptionStatus
        +setStatus(SubscriptionStatus) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class TrainerAvailability {
        -int id
        -int trainerId
        -String date
        -String startTime
        -String endTime
        -int maxCustomers
        -AvailabilityStatus status
        +TrainerAvailability()
        +TrainerAvailability(int, int, String, String, String, int, AvailabilityStatus)
        +getId() int
        +setId(int) void
        +getTrainerId() int
        +setTrainerId(int) void
        +getDate() String
        +setDate(String) void
        +getStartTime() String
        +setStartTime(String) void
        +getEndTime() String
        +setEndTime(String) void
        +getMaxCustomers() int
        +setMaxCustomers(int) void
        +getStatus() AvailabilityStatus
        +setStatus(AvailabilityStatus) void
        +toString() String
        +equals(Object) boolean
        +hashCode() int
    }
    class CustomerStatus {
        <<enumeration>>
        ACTIVE
        INACTIVE
        SUSPENDED
        +values() CustomerStatus[]
        +valueOf(String) CustomerStatus
    }
    class MembershipStatus {
        <<enumeration>>
        ACTIVE
        FROZEN
        EXPIRED
        CANCELLED
        +values() MembershipStatus[]
        +valueOf(String) MembershipStatus
    }
    class SessionStatus {
        <<enumeration>>
        REQUESTED
        CONFIRMED
        COMPLETED
        CANCELLED
        +values() SessionStatus[]
        +valueOf(String) SessionStatus
    }
    class SubscriptionStatus {
        <<enumeration>>
        ACTIVE
        EXPIRED
        CANCELLED
        +values() SubscriptionStatus[]
        +valueOf(String) SubscriptionStatus
    }
    class AvailabilityStatus {
        <<enumeration>>
        OPEN
        FULL
        CLOSED
        +values() AvailabilityStatus[]
        +valueOf(String) AvailabilityStatus
    }
    class StaffRole {
        <<enumeration>>
        ADMIN
        MANAGER
        STAFF
        +values() StaffRole[]
        +valueOf(String) StaffRole
    }
    class PaymentMethod {
        <<enumeration>>
        CASH
        CARD
        TRANSFER
        +values() PaymentMethod[]
        +valueOf(String) PaymentMethod
    }
    class EquipmentStatus {
        <<enumeration>>
        AVAILABLE
        IN_MAINTENANCE
        RETIRED
        +values() EquipmentStatus[]
        +valueOf(String) EquipmentStatus
    }
    class EquipmentCondition {
        <<enumeration>>
        NEW
        GOOD
        FAIR
        POOR
        +values() EquipmentCondition[]
        +valueOf(String) EquipmentCondition
    }
    Customer "1" o-- "0..*" Membership : holds
    Customer "1" o-- "0..*" WorkoutProgram : follows
    Customer "1" o-- "0..*" Attendance : records
    Customer "1" o-- "0..*" ProgressRecord : tracks
    Customer "*" --> "0..1" Trainer : assigned to
    Customer "1" --> "0..*" Session : books
    Customer "1" *-- "1" MedicalProfile : has
    Customer "1" --> "0..*" TrainerSubscription : subscribes
    Customer "1" --> "0..*" Payment : makes
    Trainer --> MedicalProfile : views
    Trainer "1" --> "0..*" WorkoutProgram : creates
    Trainer "1" --> "0..*" Session : coaches
    Trainer "1" --> "0..*" TrainerSubscription : offers
    Trainer "1" --> "0..*" TrainerAvailability : publishes
    TrainerSubscription "1" o-- "0..*" Session : covers
    Payment "*" --> "1" Membership : pays for
    note for Customer "Member: info, contact, status, links"
    note for Trainer "Coach: info, specialization, availability"
    note for Membership "Plan with start and end dates"
    note for WorkoutProgram "Exercise list with version, creator"
    note for Session "Booked slot with cancel reason and notes"
    note for TrainerSubscription "Period engagement at flat trainer rate"
    note for TrainerAvailability "Open days, caps, hourly slots"
    note for Equipment "Inventory item, condition, status"
    note for Attendance "Daily check-in/out record with customerId"
    note for Payment "Amount, method, date per membership"
    note for ProgressRecord "Dated body metrics and benchmarks"
    note for MedicalProfile "Blood type, conditions, meds, doctor"
    note for Administrator "Login identity, role, active flag"
    note for CustomerStatus "Active, inactive, suspended"
    note for MembershipStatus "Active, frozen, expired, cancelled"
    note for SessionStatus "Requested, confirmed, completed, cancelled"
    note for SubscriptionStatus "Active, expired, cancelled"
    note for AvailabilityStatus "Open, full, closed"
    note for StaffRole "Admin, manager, staff"
    note for PaymentMethod "Cash, card, transfer"
    note for EquipmentStatus "Available, maintenance, retired"
    note for EquipmentCondition "New, good, fair, poor"
```

### C. DAO layer zoom-in

```mermaid
classDiagram
    direction TB
    class DbConnection {
        +getConnection() Connection
        +initializeSchema() void
    }
    class DaoException {
        -String message
        -Throwable cause
        +DaoException(String, Throwable)
        +getMessage() String
        +getCause() Throwable
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
    }
    class CustomerDaoImpl {
        +save(Customer) int
        +findById(int) Customer
        +search(String) List~Customer~
        +findAll() List~Customer~
        +update(Customer) void
        +delete(int) void
        +findByStatus(CustomerStatus) List~Customer~
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
    }
    class MembershipDaoImpl {
        +save(Membership) int
        +findExpiring(int) List~Membership~
        +findById(int) Membership
        +findAll() List~Membership~
        +update(Membership) void
        +delete(int) void
        +findByCustomer(int) List~Membership~
    }
    class WorkoutProgramDao {
        <<interface>>
        +save(WorkoutProgram) int
        +findById(int) WorkoutProgram
        +findAll() List~WorkoutProgram~
        +update(WorkoutProgram) void
        +delete(int) void
        +findByCustomer(int) List~WorkoutProgram~
    }
    class WorkoutProgramDaoImpl {
        +save(WorkoutProgram) int
        +findById(int) WorkoutProgram
        +findAll() List~WorkoutProgram~
        +update(WorkoutProgram) void
        +delete(int) void
        +findByCustomer(int) List~WorkoutProgram~
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
        +findByTrainer(int) List~Session~
        +findByTrainerAndDay(int, String) List~Session~
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
        +findByTrainer(int) List~Session~
        +findByTrainerAndDay(int, String) List~Session~
    }
    class EquipmentDao {
        <<interface>>
        +save(Equipment) int
        +findById(int) Equipment
        +findAll() List~Equipment~
        +update(Equipment) void
        +delete(int) void
    }
    class EquipmentDaoImpl {
        +save(Equipment) int
        +findById(int) Equipment
        +findAll() List~Equipment~
        +update(Equipment) void
        +delete(int) void
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
    }
    class AttendanceDaoImpl {
        +save(Attendance) int
        +findByDay(String) List~Attendance~
        +findById(int) Attendance
        +findAll() List~Attendance~
        +update(Attendance) void
        +delete(int) void
        +findByCustomer(int) List~Attendance~
    }
    class PaymentDao {
        <<interface>>
        +save(Payment) int
        +revenueBetween(String, String) double
        +findById(int) Payment
        +findAll() List~Payment~
        +update(Payment) void
        +delete(int) void
        +findByCustomer(int) List~Payment~
    }
    class PaymentDaoImpl {
        +save(Payment) int
        +revenueBetween(String, String) double
        +findById(int) Payment
        +findAll() List~Payment~
        +update(Payment) void
        +delete(int) void
        +findByCustomer(int) List~Payment~
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
    }
    class AdminDaoImpl {
        +save(Administrator) int
        +findByUsername(String) Administrator
        +findById(int) Administrator
        +findAll() List~Administrator~
        +update(Administrator) void
        +delete(int) void
    }
    class TrainerSubscriptionDao {
        <<interface>>
        +save(TrainerSubscription) int
        +findActive(int, String) TrainerSubscription
        +findById(int) TrainerSubscription
        +findAll() List~TrainerSubscription~
        +update(TrainerSubscription) void
        +delete(int) void
        +findByCustomer(int) List~TrainerSubscription~
        +findByTrainer(int) List~TrainerSubscription~
    }
    class TrainerSubscriptionDaoImpl {
        +save(TrainerSubscription) int
        +findActive(int, String) TrainerSubscription
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
        +openSlots(int, String) List~TrainerAvailability~
        +findById(int) TrainerAvailability
        +findAll() List~TrainerAvailability~
        +update(TrainerAvailability) void
        +delete(int) void
        +findByTrainerAndDate(int, String) TrainerAvailability
    }
    class TrainerAvailabilityDaoImpl {
        +save(TrainerAvailability) int
        +openSlots(int, String) List~TrainerAvailability~
        +findById(int) TrainerAvailability
        +findAll() List~TrainerAvailability~
        +update(TrainerAvailability) void
        +delete(int) void
        +findByTrainerAndDate(int, String) TrainerAvailability
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
    note for DbConnection "SQLite factory, creates gym.db, schema"
    note for DaoException "Unchecked SQLException wrapper"
    note for CustomerDao "Contract to save, find, search customers"
    note for CustomerDaoImpl "SQLite implementation, customers"
    note for TrainerDao "Contract to save, find trainers"
    note for TrainerDaoImpl "SQLite implementation, trainers"
    note for MembershipDao "Contract, expiring memberships query"
    note for MembershipDaoImpl "SQLite implementation, memberships"
    note for WorkoutProgramDao "Contract, programs per customer"
    note for WorkoutProgramDaoImpl "SQLite implementation, programs"
    note for SessionDao "Contract: by trainer, day, customer, status"
    note for SessionDaoImpl "SQLite implementation, sessions"
    note for EquipmentDao "Contract to save, list equipment"
    note for EquipmentDaoImpl "SQLite implementation, equipment"
    note for AttendanceDao "Contract, daily attendance, by customer"
    note for AttendanceDaoImpl "SQLite implementation, attendance"
    note for PaymentDao "Contract, revenue ranges, by customer"
    note for PaymentDaoImpl "SQLite implementation, payments"
    note for ProgressDao "Contract, history per customer"
    note for ProgressDaoImpl "SQLite implementation, progress"
    note for MedicalProfileDao "Contract, profile per customer"
    note for MedicalProfileDaoImpl "SQLite implementation, medical"
    note for AdminDao "Contract, lookup by username"
    note for AdminDaoImpl "SQLite implementation, admins"
    note for TrainerSubscriptionDao "Contract, engagements, by customer/trainer"
    note for TrainerSubscriptionDaoImpl "SQLite implementation, subscriptions"
    note for TrainerAvailabilityDao "Contract, slots, caps, by trainer+date"
    note for TrainerAvailabilityDaoImpl "SQLite implementation, availability"
```

### D. Service layer zoom-in

```mermaid
classDiagram
    direction TB
    class CustomerService {
        +validate(Customer) List~String~
        +search(String) List~Customer~
        +findAll() List~Customer~
        +findById(int) Customer
        +save(Customer) int
        +update(Customer) void
        +delete(int) void
        +changeStatus(int, CustomerStatus) void
    }
    class TrainerService {
        +checkAvailability(int, String) Boolean
        +assignCustomer(int, int) void
        +viewMedicalProfile(int, int) MedicalProfile
        +setAvailability(int, String) void
        +setRate(int, double) void
        +findAll() List~Trainer~
        +findById(int) Trainer
        +save(Trainer) int
        +update(Trainer) void
        +delete(int) void
    }
    class MembershipService {
        +sell(int, String) Membership
        +renew(int) void
        +freeze(int) void
        +cancel(int) void
        +expiryReminders() List~Membership~
        +findByCustomer(int) List~Membership~
    }
    class WorkoutService {
        +buildProgram(int, String) WorkoutProgram
        +newVersion(int) WorkoutProgram
        +findByCustomer(int) List~WorkoutProgram~
    }
    class SchedulingService {
        +book(int, int, String) Session
        +detectConflicts(int, String) List~Session~
        +subscribe(int, int, String, String, double) TrainerSubscription
        +subscriptionPrice(int) double
        +requestSession(int, int, String) Session
        +confirmSession(int) void
        +cancelSession(int) void
        +completeSession(int) void
        +reserveSession(int, int, String) Session
        +getAvailableSlots(int, String) List~TrainerAvailability~
        +getCustomerSessions(int) List~Session~
        +getTrainerSessions(int) List~Session~
    }
    class EquipmentService {
        +changeStatus(int, EquipmentStatus) void
        +logMaintenance(int, String) void
        +findAll() List~Equipment~
    }
    class AttendanceService {
        +checkIn(int) Attendance
        +checkOut(int) void
        +dailyReport(String) List~Attendance~
        +findByCustomer(int) List~Attendance~
    }
    class PaymentService {
        +record(Payment) int
        +balance(int) double
        +revenueSummary() String
        +findByCustomer(int) List~Payment~
    }
    class ProgressService {
        +aggregate(int) List~ProgressRecord~
        +save(ProgressRecord) int
        +findByCustomer(int) List~ProgressRecord~
    }
    class MedicalProfileService {
        +save(MedicalProfile) int
        +update(MedicalProfile) void
        +getByCustomer(int) MedicalProfile
    }
    class AdminService {
        +authenticate(String, String) Administrator
        +saveStaff(Administrator) int
        +editAny(String, int) void
        +reassignTrainer(int, int) void
        +adjustPayment(int, double) void
        +voidRecord(String, int) void
    }
    class Validators {
        +validPhone(String) Boolean
        +validEmail(String) Boolean
        +validDate(String) Boolean
        +validNumber(String) Boolean
    }
    class DateUtils {
        +addMonths(String, int) String
        +overlappingSlots(String, String, String, String) Boolean
        +formatDate(String) String
        +isBeforeNow(String) Boolean
    }
    class ChartUtils {
        +weightSeries(int) List~ProgressRecord~
        +revenueSeries(String, String) List~Double~
    }
    CustomerService ..> CustomerDao : persists via
    CustomerService ..> Validators : validates with
    TrainerService ..> TrainerDao : persists via
    TrainerService ..> MedicalProfileDao : reads profiles from
    TrainerService ..> TrainerAvailabilityDao : reads slots from
    MembershipService ..> MembershipDao : persists via
    MembershipService ..> DateUtils : dates with
    WorkoutService ..> WorkoutProgramDao : persists via
    SchedulingService ..> SessionDao : persists via
    SchedulingService ..> TrainerAvailabilityDao : checks slots from
    SchedulingService ..> TrainerSubscriptionDao : reads subs from
    SchedulingService ..> DateUtils : dates with
    EquipmentService ..> EquipmentDao : persists via
    AttendanceService ..> AttendanceDao : persists via
    PaymentService ..> PaymentDao : persists via
    ProgressService ..> ProgressDao : persists via
    ProgressService ..> ChartUtils : charts with
    MedicalProfileService ..> MedicalProfileDao : persists via
    AdminService ..> AdminDao : persists via
    note for CustomerService "Validation, search, status rules"
    note for TrainerService "Availability, assign, medical view"
    note for MembershipService "Sell, renew, freeze, cancel, reminders"
    note for WorkoutService "Build programs, versioning"
    note for SchedulingService "Booking, conflicts, reservation, subscribe"
    note for EquipmentService "Status changes, maintenance log"
    note for AttendanceService "Check-in and out, daily report"
    note for PaymentService "Record, balances, revenue summary"
    note for ProgressService "Metric aggregation for charts"
    note for MedicalProfileService "Medical CRUD, lookup by customer"
    note for AdminService "Authenticate, manage staff, overrides"
    note for Validators "Phone, email, date, number checks"
    note for DateUtils "Periods, slots, overlap, formatting"
    note for ChartUtils "Weight and revenue series"
```

### E. Controllers zoom-in

```mermaid
classDiagram
    direction TB
    class MainApp {
        +start(Stage) void
        +main(String[]) void
    }
    class LoginController {
        +initialize() void
        +onLogin() void
        +onForgotPassword() void
    }
    class DashboardController {
        +initialize() void
        +refreshData() void
    }
    class CustomerListController {
        +onSearch() void
        +initialize() void
        +onDelete(int) void
        +onAdd() void
    }
    class CustomerProfileController {
        +onSave() void
        +initialize() void
        +showCustomer(int) void
    }
    class TrainerListController {
        +initialize() void
        +onDelete(int) void
        +onAdd() void
    }
    class TrainerProfileController {
        +onSave() void
        +initialize() void
        +showTrainer(int) void
    }
    class MembershipController {
        +onSell() void
        +onRenew() void
        +onFreeze() void
        +onCancel() void
        +initialize() void
    }
    class WorkoutController {
        +onBuild() void
        +onAssign() void
        +initialize() void
    }
    class ScheduleController {
        +onBook() void
        +onReserve() void
        +onCancel() void
        +showAvailableTrainers() void
        +showTrainerSlots(int) void
        +initialize() void
    }
    class AvailabilityController {
        +setCalendar(int, String) void
        +setDailyCap(int, String, int) void
        +setSlots(int, String, String) void
        +initialize() void
    }
    class EquipmentController {
        +onStatusChange() void
        +onLogMaintenance() void
        +initialize() void
    }
    class AttendanceController {
        +onCheckIn() void
        +onCheckOut() void
        +initialize() void
    }
    class PaymentController {
        +onRecord() void
        +initialize() void
    }
    class ProgressController {
        +showProgress(int) void
        +onSaveRecord() void
        +initialize() void
    }
    class MedicalProfileController {
        +showProfile(int) void
        +onSave() void
        +initialize() void
    }
    class FxUtils {
        +info(String) void
        +confirm(String) Boolean
        +error(String) void
        +switchScene(String) void
        +snackbar(String) void
    }
    MainApp ..> LoginController : opens
    LoginController ..> AdminService : authenticates via
    LoginController ..> DashboardController : opens on success
    MainApp ..> DashboardController : opens
    DashboardController ..> CustomerService : reads from
    DashboardController ..> PaymentService : reads from
    DashboardController ..> SchedulingService : reads from
    CustomerListController ..> CustomerService : uses
    CustomerListController ..> CustomerProfileController : opens
    CustomerProfileController ..> CustomerService : saves via
    TrainerListController ..> TrainerService : uses
    TrainerListController ..> TrainerProfileController : opens
    TrainerProfileController ..> TrainerService : saves via
    MembershipController ..> MembershipService : uses
    WorkoutController ..> WorkoutService : uses
    ScheduleController ..> SchedulingService : uses
    AvailabilityController ..> TrainerService : uses
    EquipmentController ..> EquipmentService : uses
    AttendanceController ..> AttendanceService : uses
    AttendanceController ..> FxUtils : dialogs with
    PaymentController ..> PaymentService : uses
    ProgressController ..> ProgressService : uses
    MedicalProfileController ..> MedicalProfileService : uses
    note for MainApp "Entry point, window, nav shell, theme"
    note for LoginController "Admin login screen"
    note for DashboardController "Members, sessions, revenue snapshot"
    note for CustomerListController "Searchable customer list"
    note for CustomerProfileController "Full member profile screen"
    note for TrainerListController "Trainer roster, availability"
    note for TrainerProfileController "Trainer details and customers"
    note for MembershipController "Plans, sell, renew, freeze, cancel"
    note for WorkoutController "Program builder, assignment"
    note for ScheduleController "Agenda, booking, reservation, conflicts"
    note for AvailabilityController "Calendar, caps, slots UI"
    note for EquipmentController "Inventory, status, maintenance"
    note for AttendanceController "Check-in screen, daily report"
    note for PaymentController "Record payments, receipts"
    note for ProgressController "Body-metric charts screen"
    note for MedicalProfileController "Medical profile view and edit"
    note for FxUtils "Dialogs, confirms, error alerts, scene switch"
```
