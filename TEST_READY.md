# TEST_READY: JavaFX 21 FXML E2E Verification Suite

## Status: READY

The headless E2E verification test harness for all 19 JavaFX 21 FXML screens has been established and verified.

---

## 1. Test Harness Artifacts
- **Test Source**: `src/test/java/com/gym/fxml/FxmlIntegrityTest.java`
- **Build Configuration**: `build.gradle`
  - Root project configured with JUnit 5 Jupiter engine:
    ```groovy
    testImplementation('org.junit.jupiter:junit-jupiter:5.11.3')
    testRuntimeOnly('org.junit.platform:junit-platform-launcher')
    test {
        useJUnitPlatform()
        systemProperties System.properties
    }
    ```
- **Documentation**: `TEST_INFRA.md`

---

## 2. Test Execution Commands

| Target / Mode | Command | Expected Output / Behavior |
|---|---|---|
| **Compilation Verification** | `./gradlew testClasses` | Compiles test harness with zero errors |
| **Progressive Test Run** | `./gradlew test` | Tests all present FXML screens; cleanly skips uncreated screens |
| **Strict All-Screen Verification** | `./gradlew test "-Dtest.strict=true" --rerun` | Fails if ANY of the 19 screens is missing from classpath |
| **Full Build Check** | `./gradlew check` | Runs full project build and verification pipeline |

---

## 3. Screen Coverage Inventory (19 Screens)
| # | Screen File | Target Specification | Status in Harness |
|---|---|---|---|
| 1 | `login.fxml` | `docs/ui/auth/LOGIN_UI.md` | Configured & Monitored |
| 2 | `app-shell.fxml` | `docs/ui/shell/APP_SHELL_UI.md` | Configured & Monitored |
| 3 | `dashboard.fxml` | `docs/ui/dashboard/DASHBOARD_UI.md` | Configured & Monitored |
| 4 | `customer-list.fxml` | `docs/ui/members/CUSTOMER_LIST_UI.md` | Configured & Monitored |
| 5 | `customer-profile.fxml` | `docs/ui/members/CUSTOMER_PROFILE_UI.md` | Configured & Monitored |
| 6 | `medical-profile.fxml` | `docs/ui/members/MEDICAL_PROFILE_UI.md` | Configured & Monitored |
| 7 | `progress.fxml` | `docs/ui/members/MEMBER_PROGRESS_UI.md` | Configured & Monitored |
| 8 | `trainer-list.fxml` | `docs/ui/trainers/TRAINER_LIST_UI.md` | Present — defect detected |
| 9 | `trainer-profile.fxml` | `docs/ui/trainers/TRAINER_PROFILE_UI.md` | Configured & Monitored |
| 10 | `availability.fxml` | `docs/ui/trainers/TRAINER_AVAILABILITY_UI.md` | Configured & Monitored |
| 11 | `workout.fxml` | `docs/ui/workouts/WORKOUT_BUILDER_UI.md` | Configured & Monitored |
| 12 | `live-workout-hud.fxml` | `docs/ui/workouts/LIVE_WORKOUT_HUD_UI.md` | Configured & Monitored |
| 13 | `schedule.fxml` | `docs/ui/workouts/SCHEDULE_CALENDAR_UI.md` | Configured & Monitored |
| 14 | `equipment.fxml` | `docs/ui/facility/EQUIPMENT_INVENTORY_UI.md` | Configured & Monitored |
| 15 | `gym-floor-map.fxml` | `docs/ui/facility/GYM_FLOOR_MAP_UI.md` | Configured & Monitored |
| 16 | `attendance.fxml` | `docs/ui/facility/ATTENDANCE_CHECKIN_UI.md` | Configured & Monitored |
| 17 | `membership.fxml` | `docs/ui/billing/MEMBERSHIP_PLANS_UI.md` | Configured & Monitored |
| 18 | `payment.fxml` | `docs/ui/billing/PAYMENT_BILLING_UI.md` | Configured & Monitored |
| 19 | `staff.fxml` | `docs/ui/admin/STAFF_MANAGEMENT_UI.md` | Configured & Monitored |

---

## 4. Test Integrity & Defect Escalation
The test harness is non-facade and executes genuine JavaFX parsing via `FXMLLoader.load(...)` on the JavaFX Application Thread.

### Early Defect Escalation:
- **Target File**: `src/main/resources/fxml/trainer-list.fxml` (line 37)
- **Defect**: Element `<M3TopAppBar fx:id="topAppBar" title="..." variant="LARGE" elevation="2" />` declares attribute `elevation="2"`.
- **Root Cause**: `M3TopAppBar` does not have an `elevation` property or setter.
- **Observed Error**: `com.sun.javafx.fxml.PropertyNotFoundException: Property "elevation" does not exist or is read-only.`
- **Action Required**: The implementing worker for `trainer-list.fxml` must remove `elevation="2"` from `<M3TopAppBar>`.
