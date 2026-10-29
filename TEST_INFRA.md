# JavaFX 21 FXML E2E Test Infrastructure

## 1. Overview & Objective
This test harness verifies **Requirement R4** for the Gym Management System:
> *"All created FXML files must be syntactically valid JavaFX 21 XML, resolve all component imports, load cleanly without parse errors, and pass `./gradlew check`."*

The harness validates that:
1. Every screen FXML exists on the application classpath (`/fxml/<screen>.fxml`).
2. The XML structure conforms to JavaFX 21 schema and namespace definitions (`xmlns="http://javafx.com/javafx/21" xmlns:fx="http://javafx.com/fxml/1"`).
3. All custom `m3fx-controls` tags (`io.m3fx.controls.*`) resolve to valid compiled classes and public properties.
4. If declared, `fx:controller` classes resolve, are instantiated via reflection, and bind all `@FXML` fields and event handlers without throwing `ClassNotFoundException` or `NoSuchMethodException`.
5. The parsed document produces a non-null JavaFX `Parent` hierarchy.

---

## 2. Test Architecture

### 2.1 File Location
- **Test Class**: `src/test/java/com/gym/fxml/FxmlIntegrityTest.java`
- **Root Configuration**: `build.gradle` (`test { useJUnitPlatform(); systemProperties System.properties }`)

### 2.2 Headless JavaFX Lifecycle
JavaFX controls require the JavaFX Toolkit and graphics pipeline. The test harness initializes the toolkit once for all tests using `@BeforeAll`:
```java
@BeforeAll
static void initToolkit() {
    try {
        Platform.startup(() -> {});
    } catch (IllegalStateException ignored) {
        // Platform already initialized
    }
}
```

### 2.3 FX Application Thread Safe Execution
Some JavaFX skins and custom controls access stage/scene properties or attach listeners upon node instantiation. To prevent `IllegalStateException: Not on FX application thread`, the loader executes within `Platform.runLater` with a latch-protected timeout mechanism:
```java
AtomicReference<Parent> rootRef = new AtomicReference<>();
AtomicReference<Throwable> errorRef = new AtomicReference<>();
CountDownLatch latch = new CountDownLatch(1);

Platform.runLater(() -> {
    try {
        FXMLLoader loader = new FXMLLoader(resource);
        Parent root = loader.load();
        rootRef.set(root);
    } catch (Throwable t) {
        errorRef.set(t);
    } finally {
        latch.countDown();
    }
});
```
Any parsing, reflection, or classloading exception is captured and cleanly rethrown so JUnit and Gradle report the exact file, line number, and stack trace.

---

## 3. Screen Inventory Under Test
All 19 screens defined in `ORIGINAL_REQUEST.md` and `PROJECT.md` are covered:

| # | Screen File | Target Specification |
|---|---|---|
| 1 | `login.fxml` | `docs/ui/auth/LOGIN_UI.md` |
| 2 | `app-shell.fxml` | `docs/ui/shell/APP_SHELL_UI.md` |
| 3 | `dashboard.fxml` | `docs/ui/dashboard/DASHBOARD_UI.md` |
| 4 | `customer-list.fxml` | `docs/ui/members/CUSTOMER_LIST_UI.md` |
| 5 | `customer-profile.fxml` | `docs/ui/members/CUSTOMER_PROFILE_UI.md` |
| 6 | `medical-profile.fxml` | `docs/ui/members/MEDICAL_PROFILE_UI.md` |
| 7 | `progress.fxml` | `docs/ui/members/MEMBER_PROGRESS_UI.md` |
| 8 | `trainer-list.fxml` | `docs/ui/trainers/TRAINER_LIST_UI.md` |
| 9 | `trainer-profile.fxml` | `docs/ui/trainers/TRAINER_PROFILE_UI.md` |
| 10 | `availability.fxml` | `docs/ui/trainers/TRAINER_AVAILABILITY_UI.md` |
| 11 | `workout.fxml` | `docs/ui/workouts/WORKOUT_BUILDER_UI.md` |
| 12 | `live-workout-hud.fxml` | `docs/ui/workouts/LIVE_WORKOUT_HUD_UI.md` |
| 13 | `schedule.fxml` | `docs/ui/workouts/SCHEDULE_CALENDAR_UI.md` |
| 14 | `equipment.fxml` | `docs/ui/facility/EQUIPMENT_INVENTORY_UI.md` |
| 15 | `gym-floor-map.fxml` | `docs/ui/facility/GYM_FLOOR_MAP_UI.md` |
| 16 | `attendance.fxml` | `docs/ui/facility/ATTENDANCE_CHECKIN_UI.md` |
| 17 | `membership.fxml` | `docs/ui/billing/MEMBERSHIP_PLANS_UI.md` |
| 18 | `payment.fxml` | `docs/ui/billing/PAYMENT_BILLING_UI.md` |
| 19 | `staff.fxml` | `docs/ui/admin/STAFF_MANAGEMENT_UI.md` |

---

## 4. Test Execution Modes

### 4.1 Progressive Milestone Execution (Default)
Supports incremental multi-agent milestone development.
- When an FXML file is present, it is **strictly verified** (must be valid XML, resolve all imports, instantiate without errors).
- When an FXML file is not yet created, it is reported as skipped via `Assumptions.assumeTrue(...)`.
- Allows `./gradlew check` to pass as milestones are incrementally delivered without false negatives.

Command:
```bash
./gradlew test
# or
./gradlew check
```

### 4.2 Strict Mode (Full Suite Verification)
Enforces that **all 19 screens must exist on the classpath** and load cleanly. Missing any screen triggers an immediate assertion failure.

Command (PowerShell):
```powershell
.\gradlew test "-Dtest.strict=true" --rerun
```
Command (Bash):
```bash
./gradlew test -Dtest.strict=true --rerun
```

---

## 5. Critical FXML Authoring Rules & Common Defects

1. **`M3TopAppBar`**:
   - Supported properties: `title`, `variant` (`SMALL`, `MEDIUM`, `LARGE`).
   - **Do not use `elevation="..."`** on `M3TopAppBar`. `M3TopAppBar` does not have an elevation property setter; declaring it causes `PropertyNotFoundException`.
2. **`M3Select` / `ComboBox` `<items>`**:
   - Do not nest `<String fx:value="..."/>` directly inside `<items>`.
   - If populating in FXML, wrap child strings inside `<FXCollections fx:factory="observableArrayList">` with `<?import javafx.collections.FXCollections?>`, or populate options programmatically in controller `initialize()`.
3. **Control Variants**:
   - `M3Button`: property is `variant` (`FILLED`, `TONAL`, `OUTLINED`, `ELEVATED`, `TEXT`).
   - `M3Card`: property is `variant` (`ELEVATED`, `FILLED`, `OUTLINED`).
   - `M3TextField`: property is `variant` (`OUTLINED`, `FILLED`).
4. **Icons**:
   - `M3Icon`: property is `symbolName="..."` (e.g. `<M3Icon symbolName="search"/>`), not `iconLiteral="..."`.
5. **Non-Node Controls**:
   - `M3GlowShadow` is a utility method, not a Node tag.
   - `M3Snackbar` is an overlay controller, not a Node tag. Represent notification hosts as `StackPane` in FXML.
