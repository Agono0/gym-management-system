# `controller` — JavaFX UI Controllers (JavaFX branch)

## Purpose
FXML controllers: bind FXML layouts to services, handle user events, apply
Material Design 3 styling via theme tokens. One controller per screen; FXML
files live in `src/main/resources/fxml/` with matching names.

## Contents (to be written)
| Controller (↔ FXML) | Screen | Owner |
|---------------------|--------|-------|
| `DashboardController` | Main dashboard + navigation | Abdel Raouf |
| `CustomerListController`, `CustomerProfileController` | Customers + full profile (editable medical section) | Abdelrhman |
| `TrainerListController`, `TrainerProfileController` | Trainers + profiles (read-only medical section) | Ziad |
| `AvailabilityController` | Trainer calendar, daily caps, hourly slots management | Ziad |
| `MembershipController` | Plans, sell/renew/freeze | Ziad |
| `WorkoutController` | Program builder + assignment | Yousef |
| `ScheduleController` | Agenda, booking dialog, subscribe-to-trainer flow | Yousef |
| `EquipmentController` | Inventory + maintenance | Yousef |
| `AttendanceController` | Check-in/out + daily report | Abdel Raouf |
| `PaymentController` | Payments + receipts | Abdel Raouf |
| `ProgressController` | Metrics + charts | Abdel Raouf |

## Tasks
| Task | Owner | Status |
|------|-------|--------|
| Skeleton + this README created | Abdelrhman | Done |
| `MainApp.java` + main window/nav shell (Week 1–2) | Abdelrhman | To Do |
| Controllers per owned screens (see table, M3-styled) | Each owner | To Do |
| Error handling per screen: validation + friendly DB-error dialogs (no stack traces) | Each owner | To Do |

Styling rule: use tokens from `resources/theme/material-theme.css` only —
no hardcoded colors. Port each finished screen to `swing`/`view/` promptly
(Week 5) instead of batching everything at the end.
