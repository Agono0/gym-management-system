# TEST_READY: JavaFX 21 FXML E2E Verification Suite

## Status: READY

The headless E2E verification suite for all 19 JavaFX 21 FXML screens (+ `app-shell.fxml` and the
controller-free `main.fxml` showcase) is established and green: `./gradlew test` runs **9 test
classes / 59 tests** in the app module with 0 failures and 0 skips (JUnit XML totals in
`build/test-results/test/`), plus the m3fx suites in `m3fx-core` (8 tests) and `m3fx-controls`
(128 tests).

---

## 1. Test Harness Artifacts
- **Test Sources**: `src/test/java/com/gym/fxml/` — nine classes:
  - `FxmlIntegrityTest.java` — parses and loads every FXML via `FXMLLoader` (strict by default)
  - `ControllerBindingContractTest.java` — pins `fx:id`/`@FXML` binding counts per screen
  - `AllScreensScrollHeightTest.java` — computed scroll-height bounds per screen
  - `ScrollPaneBehaviorTest.java` — nested-scroll conflict elimination
  - `GymAppScreenViewerTest.java` — screen inspection through the real shell
  - `GymAppFullInteractiveAuditTest.java` — interactive audit pass over the shell
  - `GymAppDiagnosticsTest.java` — shell diagnostics
  - `FxmlStressHarnessTest.java` — repeated load stress
  - `WiredFlowSmokeTest.java` — cross-screen flows (trainer → profile, customer → profile,
    dashboard → sign-out, payment/attendance actions)
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
| **Progressive Test Run** | `./gradlew test` | **Strict by default:** a missing FXML screen fails the run |
| **Lenient Milestone Mode** | `./gradlew test "-Dtest.lenient=true" --rerun` | Skips not-yet-created screens via failed assumptions (opt-in only) |
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
| 8 | `trainer-list.fxml` | `docs/ui/trainers/TRAINER_LIST_UI.md` | Configured & Monitored |
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

(`main.fxml` — the controller-free m3fx component showcase — sits outside the 19-screen app
inventory and is covered by the integrity test's load pass.)

---

## 4. Test Integrity & Defect Escalation
The test harness is non-facade and executes genuine JavaFX parsing via `FXMLLoader.load(...)` on the JavaFX Application Thread.

### Defect log (all resolved)
- **Resolved**: `src/main/resources/fxml/trainer-list.fxml` declared an unsupported
  `elevation="2"` attribute on `<M3TopAppBar>` (`PropertyNotFoundException` at load). The attribute
  has been removed; the screen now loads cleanly (verified by `FxmlIntegrityTest` and by
  `WiredFlowSmokeTest.trainerListOpenActionNavigatesToProfileWithChosenCoach` in the
  `./gradlew test` run for this revision).
- **No open escalations.**
