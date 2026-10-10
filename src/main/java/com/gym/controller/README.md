# `controller` — JavaFX UI Controllers (JavaFX branch)

Canonical design: `docs/UML.md` §5. This file tracks ownership and screen inventory — it does not
restate method signatures.

## Purpose

FXML controllers: bind FXML layouts to services, handle user events, apply Material Design 3
styling via theme tokens. One controller per screen; FXML files live in
`src/main/resources/fxml/` with matching names.

## Rules

- **Controllers never contain SQL and never touch a DAO.** Always go through a service.
- A controller catches `DaoException` and shows it with `FxUtils.error(e.getMessage())`. Never let a
  stack trace reach the user.
- Styling only through theme tokens: call `M3Stylesheets.applyTo(scene, theme)` and reference
  `-md-sys-color-*` looked-up colors in FXML/CSS — no hardcoded colors.
- Capability checks go through `SessionContext.requireRole(StaffRole.ADMIN)`; `StaffController` is
  the only screen that needs it today.
- `MainApp` is the entry point, not a screen: it builds the window, nav shell, theme and overlay
  layer, then opens `LoginController`.

## Contents (to be written)

| Controller (↔ FXML) | Screen | Owner |
|---------------------|--------|-------|
| `LoginController` | Login screen (authenticates via `AdminService`, stores user in `SessionContext`) | Abdelrhman |
| `DashboardController` | Main dashboard + navigation + logout | Abdel Raouf |
| `CustomerListController` | Searchable customer list | Abdelrhman |
| `CustomerProfileController` | Full member profile, editable medical section | Abdelrhman |
| `MedicalProfileController` | Medical profile view/edit per customer | Abdelrhman |
| `TrainerListController` | Trainer roster + availability | Ziad |
| `TrainerProfileController` | Trainer profile, assigned customers (read-only medical section) | Ziad |
| `AvailabilityController` | Trainer calendar, per-slot caps, hourly slot publishing | Ziad |
| `MembershipController` | Plans, sell/renew/freeze/unfreeze/cancel | Ziad |
| `WorkoutController` | Program builder + reassignment | Yousef |
| `ScheduleController` | Agenda, booking dialog, request/reserve, subscribe-to-trainer flow | Yousef |
| `EquipmentController` | Inventory add/edit + status + condition + maintenance date | Yousef |
| `AttendanceController` | Check-in/out + daily report | Abdel Raouf |
| `PaymentController` | Payments, voiding, receipts | Abdel Raouf |
| `ProgressController` | Metrics + charts | Abdel Raouf |
| `StaffController` | Staff management and admin overrides (ADMIN only) | Abdelrhman |

Sixteen screens. `main.fxml` currently holds a component showcase with **no `fx:controller`** — it is
the m3fx gallery, not one of these screens, and it will be replaced by the real shell.

## Tasks

| Task | Owner | Status |
|------|-------|--------|
| Skeleton + this README created | Abdelrhman | Done |
| `MainApp.java` + main window/nav shell + theme/overlay wiring (Week 1–2) | Abdelrhman | To Do |
| Controllers per owned screens (see table, M3-styled) | Each owner | To Do |
| Error handling per screen: validation + friendly DB-error dialogs (no stack traces) | Each owner | To Do |
| Replace the `main.fxml` showcase with the real nav shell | Abdelrhman | To Do |

Port each finished screen to the `swing`/`view/` branch promptly (Week 5) instead of batching
everything at the end. That branch has no Java files yet.
