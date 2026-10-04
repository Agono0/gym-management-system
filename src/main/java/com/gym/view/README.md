# `view` — Swing UI (Swing branch)

## Purpose
Swing frames, panels, and dialogs — the only layer rewritten (not copied)
from `main`. One panel per screen, FlatLaf + M3 theme properties, same
visual language and wording as the JavaFX counterpart.

## Contents (to be written, each ported after its counterpart on `main` merges)
| Panel | Screen | Owner |
|-------|--------|-------|
| `DashboardFrame` | Main window + navigation | Abdel Raouf |
| `CustomerPanel` | Customer list + full profile | Abdelrhman |
| `TrainerPanel` | Trainer list + profiles | Ziad |
| `MembershipPanel` | Plans, sell/renew/freeze | Ziad |
| `WorkoutPanel` | Program builder + assignment | Yousef |
| `SchedulePanel` | Agenda + booking dialog | Yousef |
| `EquipmentPanel` | Inventory + maintenance | Yousef |
| `AttendancePanel` | Check-in/out + daily report | Abdel Raouf |
| `PaymentPanel` | Payments + receipts | Abdel Raouf |
| `ProgressPanel` | Metrics + charts | Abdel Raouf |

## Tasks
| Task | Owner | Status |
|------|-------|--------|
| Skeleton + this README created | Abdelrhman | Done |
| `MainApp.java` + main frame/nav (Week 1–2) | Abdelrhman | To Do |
| Panels per owned screens, M3-styled (Weeks 2–5, after the `main` counterpart merges) | Each owner | To Do |
| Error handling per panel: validation + friendly DB-error dialogs (no stack traces) | Each owner | To Do |

Styling rule: FlatLaf theme properties from `resources/theme/` only —
no hardcoded colors/fonts. Note simplified motion vs JavaFX in each PR.
