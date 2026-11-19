# docs/agents/APP_MAP.md — Gym Management System app map (Phase 0, 2026-11-20)

Users: gym administrators / front desk. Purpose: manage members, trainers, sessions,
memberships, payments, attendance, equipment, staff. **GUI is currently 100% demo data**;
the 13 DAOs (`com.gym.dao`, SQLite `gym.db`) enforce the 19 business rules (docs/UML.md:1156–1182)
via `validate(...)` + are proven by the CLI. Service layer is retired (DECISIONS.md 77): the
target architecture is **controller → DAO directly, background `Task`s off the FX thread**.

## Navigation

- `GymApp` (runGym) → seed theme → `AppShellController` → Scene 1280×800, min 1024×700.
- `AppShellController` (963 ln): BorderPane + left `M3NavigationRail` (10 toggle destinations
  navDashboard…navHelp, `railButtonByKey` switch), `loadScreen(Screen)` via the `Screen` enum
  registry (create → ScrollPane wrap → applyCss → MotionUtil.enter), `openScreenViaRail(key,
  screen, setup)` for detail handoffs (customer/trainer profile), command palette (Ctrl+K),
  dark-mode toggle (re-seeds theme), manual rail collapse (220↔80px spring).
- `LaunchGateController` (825 ln, `SHOW_LAUNCH_GATES`): splash beats → prewarm dashboard →
  login card ("any email + 4+ char password" — demo auth) → morph into shell.
- Responsive: **none**. Theme: seed purple, dark default.

## Screens (19) — primary task, data target, weak spots

| Screen | Primary task | DAO target (rules) | Current weak spots |
|---|---|---|---|
| LOGIN | Sign in | AdministratorDao (16) | demo auth, fake telemetry copy |
| DASHBOARD | At-a-glance ops + check-in | Customer/Membership/Payment/Session/Attendance (5,6) | hardcoded KPIs "1,428/$3,840", hex colors, Random jitter |
| CUSTOMER_LIST | Find/manage members | CustomerDao (4,15) | Object[] rows, KPIs ≠ table, unused loadingBar, export = clipboard |
| CUSTOMER_PROFILE | Member dossier + lifecycle | CustomerDao, MembershipDao (4,6,7,14,15) | actions are label swaps, ~280-line ctor, findById copies |
| MEDICAL_PROFILE | Clinical record console | MedicalProfileDao (3) | demo save, stepper usage shallow |
| PROGRESS | Biometric analytics | ProgressRecordDao (13) | hex colors, fake 80.0 target, dead celebration canvas, hardcoded "today" |
| TRAINER_LIST | Coach roster | TrainerDao (4,15) | pipe-string rows, destructive sort mutation |
| TRAINER_PROFILE | Coach dossier + tabs | TrainerDao, TrainerSubscriptionDao (4,11,15) | data doesn't follow selection, identical demo medical for all |
| AVAILABILITY | Slot matrix + publish | TrainerAvailabilityDao (8) | dead M3Steppers swapped for sliders, no-op setSlots/setSlotCap, no back handler |
| WORKOUT | Program builder | WorkoutProgramDao (11) | **pure skeleton — all handlers empty**, unwired search/chips |
| LIVE_WORKOUT_HUD | Run a live session | SessionDao (18) | emoji buttons, no-op skip/recalibrate, label-hack minus button |
| SCHEDULE | Week agenda + booking | SessionDao, TrainerAvailabilityDao (1,10,17,18) | only Mon–Wed demo columns, week nav is a label swap, hex cyan, stub lifecycle |
| EQUIPMENT | Inventory + 3D twin | EquipmentDao (4,13) | **skeleton, empty table with fake "84" label**, stock TableView, emoji buttons, no-op handlers |
| GYM_FLOOR_MAP | Occupancy blueprint | derived: Attendance + Equipment (5) | hardcoded zone %, hex legend, all toggles/pins no-ops |
| ATTENDANCE | Check-in/out turnstile | AttendanceDao (5,19) | pipe-string stream, magic cap 200, static dossier |
| MEMBERSHIP | Tier + lifecycle + ledger | MembershipDao (7,14) + PaymentDao (6) | **cancel = destructive, no confirm**, silent actions, Math.random() ids, ✕ text button |
| PAYMENT | POS register + receipts | PaymentDao (6,12,19) | pipe rows, dead "Print Thermal Copy" button, stock TextField/RadioButton |
| STAFF | RBAC + audit | AdministratorDao (16) | **row actions always hit row 0**, hardcoded role badge, no empty state |

Cross-cutting: pipe-delimited/stringly-typed demo rows in 5 screens, `Object[]` rows in 2;
40-line `findById` tree-walk + `installFeedbackLayer` + `showMenuBelow` + rail-nav boilerplate
copied across ~8 controllers; "(demo)" suffixes everywhere; detail screens lack back-nav/rail
sync; no loading/empty/error states anywhere.

## Source of truth

`docs/UML.md` (19 rules, class model), `docs/DECISIONS.md` (77: controller→DAO, no services;
78: DAO validation), layer READMEs under `src/main/java/com/gym/**/README.md`.
