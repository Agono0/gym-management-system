# Material Design 3 Expressive & Cybernetic UI/UX Architecture
## Gym Management System (`com.gym`) — Comprehensive Screen Specifications

> **System Authority:** This document serves as the master catalog and architectural foundation for the entire user interface and user experience (UI/UX) of the **Gym Management System**. It governs layout conventions, component selections, motion physics, dynamic theming, and accessibility across all **19 screens and activities**.
>
> All screens are designed around **Material Design 3 (M3) Expressive**, sports science data visualization, hardware-accelerated 3D viewports, and glassmorphic telemetry dashboards.

---

## 1. Master Design System Philosophy

The Gym Management System is built to feel like an athletic command center from the near future: fast, tactile, data-dense, and effortless to operate under high-stress front-desk rushes or intense gym floor workouts.

### Key Pillars
1. **Bento-Grid Modular Hierarchy**: Rather than cluttered forms or monotonous tables, screens use structured Bento-Box layouts. High-priority Key Performance Indicators (KPIs) act as structural anchors with large pill-shaped containers, while secondary telemetry occupies balanced modular cards.
2. **Dynamic M3 Theming**: Every surface, text role, and accent border references dynamic theme variables (`-md-sys-color-*`). The entire system updates instantaneously across 6 seed color palettes, 9 M3 variants (Expressive, Vibrant, Tonal Spot, etc.), and standard/medium/high contrast modes in both Dark and Light schemes.
3. **Physical Spring Motion & Interruption Resilience**: Transitions do not use rigid linear timers. They are driven by mass-spring-damper physics (`SpringAnimation`) with continuous momentum. If an operator clicks or swipes mid-transition, the velocity is preserved without sudden jumps or visual glitches.
4. **Tactile Micro-Interactions**: Buttons, cards, and interactive pins feature haptic-like visual feedback: press-scale contraction (scale down to `0.97`) followed by a spring bounce (overshoot to `1.04` before settling to `1.0`).
5. **Cybernetic Atmospheric Effects**: Key telemetry widgets leverage frosted acrylic glassmorphism (`M3GlassPane`), specular highlight rims, neon glow shadows (`M3GlowShadow`), laser scanline sweeps (`M3Scanline`), and ambient particle physics (`M3ParticleCanvas`).

---

## 2. Global M3 Theme & Token System

### A. Seed Color Presets
The app defaults to **Cyber Violet** (`#6750A4`) but dynamically supports instant switching to:
- **Neon Teal** (`#006A6A`): High-energy cardio & pool operations.
- **Apex Coral** (`#B3261E`): High-intensity interval & boxing telemetry.
- **Electric Azure** (`#0061A4`): Enterprise management & financial reporting.
- **Matrix Green** (`#386A20`): Biometric health, recovery & wellness tracking.
- **Solar Amber** (`#825500`): VIP access & championship achievements.

### B. M3 Color Role Mapping
All screens strictly use looked-up CSS colors:
```css
.root {
  -fx-background-color: -md-sys-color-surface;
  -fx-text-fill: -md-sys-color-on-surface;
}
/* Surface elevation container hierarchy */
-md-sys-color-surface-container-lowest   /* Deep background wells */
-md-sys-color-surface-container-low      /* Base canvas containers */
-md-sys-color-surface-container          /* Standard cards and tiles */
-md-sys-color-surface-container-high     /* Hovered cards, floating menus */
-md-sys-color-surface-container-highest  /* Tooltips, input fields, badges */
```

### C. Motion Presets (`MotionTokens.java`)
| Preset | Stiffness ($k$) | Damping ($\zeta$) | Intended Experience |
|---|---|---|---|
| **`EXPRESSIVE_SPATIAL`** | 380 | 0.70 | Fast, slightly bouncy card morphs and sheet expansions |
| **`EXPRESSIVE_EFFECTS`** | 200 | 0.85 | Smooth dialog popups and glass panel fades |
| **`BOUNCY`** | 300 | 0.55 | Rep counter increments, PR celebrations, FAB speed dial |
| **`GENTLE`** | 180 | 1.00 | Non-distracting background telemetries and sliders |

---

## 3. Master Screen & Activity Catalog (All 19 Screens)

The application is structured into **8 modular activity bulks**, completely documented in their dedicated subfolders:

```
docs/ui/
├── README.md (This Master Guide)
├── auth/
│   └── LOGIN_UI.md                  # Activity 01: Biometric & Credential Auth Vault
├── shell/
│   └── APP_SHELL_UI.md              # Activity 02: Adaptive Navigation Shell & Command Palette
├── dashboard/
│   └── DASHBOARD_UI.md              # Activity 03: Executive Command Center & Bento Dashboard
├── members/
│   ├── CUSTOMER_LIST_UI.md          # Activity 04: Member Directory & High-Density Roster
│   ├── CUSTOMER_PROFILE_UI.md       # Activity 05: 360° Athlete VIP Dossier & Hologram Card
│   ├── MEDICAL_PROFILE_UI.md        # Activity 06: Clinical Health Assessment & 3D Anatomy
│   └── MEMBER_PROGRESS_UI.md        # Activity 07: VBT Performance, Splines & 52-Week Heatmap
├── trainers/
│   ├── TRAINER_LIST_UI.md           # Activity 08: Coach Roster, Badges & Specialty Chips
│   ├── TRAINER_PROFILE_UI.md        # Activity 09: Coach Bio, Skill Radar & Client Gauges
│   └── TRAINER_AVAILABILITY_UI.md   # Activity 10: Weekly Booking Matrix & Slot Cap Heatmap
├── workouts/
│   ├── WORKOUT_BUILDER_UI.md        # Activity 11: Kinetic Routine Builder & Pipeline Editor
│   ├── LIVE_WORKOUT_HUD_UI.md       # Activity 12: Kinetic Rep Counter & Cardiac HUD Cockpit
│   └── SCHEDULE_CALENDAR_UI.md      # Activity 13: Master Agenda, Classes & Seat Allocation
├── facility/
│   ├── EQUIPMENT_INVENTORY_UI.md    # Activity 14: Equipment Catalog & 12 Procedural 3D Models
│   ├── GYM_FLOOR_MAP_UI.md          # Activity 15: 2.5D Interactive Blueprint & 3D Linking
│   └── ATTENDANCE_CHECKIN_UI.md     # Activity 16: Fast-Lane Check-In Scanner & Density Map
├── billing/
│   ├── MEMBERSHIP_PLANS_UI.md       # Activity 17: Tier Carousel & Conversion Funnel Chart
│   └── PAYMENT_BILLING_UI.md        # Activity 18: Cashless POS Register & Split Billing
└── admin/
    └── STAFF_MANAGEMENT_UI.md       # Activity 19: RBAC Security Matrix & Audit Timeline
```

---

## 4. Comprehensive Screen Index & Architectural Mapping

| # | Screen / Activity | Dedicated UI Spec Document | Primary Controller | FXML File Pairing | Key M3 & Cyber Components |
|---|---|---|---|---|---|
| **01** | **Authentication Vault** | [`auth/LOGIN_UI.md`](file:///c:/Users/pc/Desktop/JAVA-Project/docs/ui/auth/LOGIN_UI.md) | `LoginController` | `login.fxml` | `M3GlassPane`, `M3TextField`, `M3Button`, `M3ShapeMorph`, `M3PulseBeacon` |
| **02** | **Global Adaptive Shell** | [`shell/APP_SHELL_UI.md`](file:///c:/Users/pc/Desktop/JAVA-Project/docs/ui/shell/APP_SHELL_UI.md) | `ShellController` | Shell / Layout | `M3TopAppBar`, `M3NavigationRail`, `M3NavigationDrawer`, `M3CommandPalette`, Theme Strip |
| **03** | **Executive Dashboard** | [`dashboard/DASHBOARD_UI.md`](file:///c:/Users/pc/Desktop/JAVA-Project/docs/ui/dashboard/DASHBOARD_UI.md) | `DashboardController` | `dashboard.fxml` | `M3StatCard`, `M3GlowShadow`, `M3SplineChart`, `M3DonutChart`, `M3Timeline`, `M3SpeedDial` |
| **04** | **Member Directory** | [`members/CUSTOMER_LIST_UI.md`](file:///c:/Users/pc/Desktop/JAVA-Project/docs/ui/members/CUSTOMER_LIST_UI.md) | `CustomerListController` | `customer-list.fxml` | `M3DataTable`, `M3SearchBar`, `M3Chip` (Filter), `M3SplitButton`, `M3Badge` |
| **05** | **Athlete VIP Dossier** | [`members/CUSTOMER_PROFILE_UI.md`](file:///c:/Users/pc/Desktop/JAVA-Project/docs/ui/members/CUSTOMER_PROFILE_UI.md) | `CustomerProfileController` | `customer-profile.fxml` | `M3TiltCard3D` (Hologram Pass), `M3RadarChart`, `M3Timeline`, `M3StatCard` |
| **06** | **Medical Assessment** | [`members/MEDICAL_PROFILE_UI.md`](file:///c:/Users/pc/Desktop/JAVA-Project/docs/ui/members/MEDICAL_PROFILE_UI.md) | `MedicalProfileController` | `medical-profile.fxml` | `M3AnatomyViewer3D` (3D Muscles), `M3Stepper`, `M3Card`, `M3Chip` |
| **07** | **Athletic Progress** | [`members/MEMBER_PROGRESS_UI.md`](file:///c:/Users/pc/Desktop/JAVA-Project/docs/ui/members/MEMBER_PROGRESS_UI.md) | `ProgressController` | `progress.fxml` | `M3ScatterPlot` (VBT OLS Regression), `M3ActivityCalendar` (52-Week), `M3SplineChart` |
| **08** | **Coach Roster** | [`trainers/TRAINER_LIST_UI.md`](file:///c:/Users/pc/Desktop/JAVA-Project/docs/ui/trainers/TRAINER_LIST_UI.md) | `TrainerListController` | `trainer-list.fxml` | `M3Carousel` (Coach cards), `M3Chip`, `M3StatCard`, `M3DataTable`, `M3Badge` |
| **09** | **Coach Master Dossier** | [`trainers/TRAINER_PROFILE_UI.md`](file:///c:/Users/pc/Desktop/JAVA-Project/docs/ui/trainers/TRAINER_PROFILE_UI.md) | `TrainerProfileController` | `trainer-profile.fxml` | `M3RadarChart` (Skills), `M3GaugeChart` (Capacity), `M3Timeline` (Certs), `M3Card` |
| **10** | **Coach Availability** | [`trainers/TRAINER_AVAILABILITY_UI.md`](file:///c:/Users/pc/Desktop/JAVA-Project/docs/ui/trainers/TRAINER_AVAILABILITY_UI.md) | `AvailabilityController` | `availability.fxml` | `M3Heatmap` (Booking density), `M3Switch`, `M3Slider`, `M3Button` (Pills) |
| **11** | **Workout Builder** | [`workouts/WORKOUT_BUILDER_UI.md`](file:///c:/Users/pc/Desktop/JAVA-Project/docs/ui/workouts/WORKOUT_BUILDER_UI.md) | `WorkoutController` | `workout.fxml` | Drag Pipeline, `M3AnatomyViewer3D`, `M3SplitButton`, `M3Card`, `M3Chip` |
| **12** | **Live Workout HUD** | [`workouts/LIVE_WORKOUT_HUD_UI.md`](file:///c:/Users/pc/Desktop/JAVA-Project/docs/ui/workouts/LIVE_WORKOUT_HUD_UI.md) | `LiveWorkoutController` | HUD / Overlay | `M3RepCounter`, `M3HeartRateZones`, `M3IntervalTimer`, `M3WorkoutHUD`, `M3PulseBeacon` |
| **13** | **Class & Master Schedule** | [`workouts/SCHEDULE_CALENDAR_UI.md`](file:///c:/Users/pc/Desktop/JAVA-Project/docs/ui/workouts/SCHEDULE_CALENDAR_UI.md) | `ScheduleController` | `schedule.fxml` | `M3DonutChart` (Capacities), `M3Calendar`, `M3Chip`, `M3Button`, `M3Badge` |
| **14** | **Equipment Catalog 3D** | [`facility/EQUIPMENT_INVENTORY_UI.md`](file:///c:/Users/pc/Desktop/JAVA-Project/docs/ui/facility/EQUIPMENT_INVENTORY_UI.md) | `EquipmentController` | `equipment.fxml` | `M3ModelViewer3D` (12 3D Models), Wireframe Shader, Orbit Camera, `M3DataTable` |
| **15** | **Gym Blueprint Floorplan**| [`facility/GYM_FLOOR_MAP_UI.md`](file:///c:/Users/pc/Desktop/JAVA-Project/docs/ui/facility/GYM_FLOOR_MAP_UI.md) | `GymFloorMapController` | Floorplan View | `M3GymFloorMap` (2.5D Blueprint), Status Pins, 1-Click 3D Link Popup, `M3Card` |
| **16** | **Fast-Lane Attendance** | [`facility/ATTENDANCE_CHECKIN_UI.md`](file:///c:/Users/pc/Desktop/JAVA-Project/docs/ui/facility/ATTENDANCE_CHECKIN_UI.md) | `AttendanceController` | `attendance.fxml` | `M3Scanline`, `M3PulseBeacon`, `M3Heatmap` (Peak Hours), `M3DataTable`, `M3Button` |
| **17** | **Membership Plans** | [`billing/MEMBERSHIP_PLANS_UI.md`](file:///c:/Users/pc/Desktop/JAVA-Project/docs/ui/billing/MEMBERSHIP_PLANS_UI.md) | `MembershipController` | `membership.fxml` | `M3Carousel` (Plan tiers), `M3FunnelChart` (Conversion drop-offs), `M3Dialog` |
| **18** | **Cashless POS Billing** | [`billing/PAYMENT_BILLING_UI.md`](file:///c:/Users/pc/Desktop/JAVA-Project/docs/ui/billing/PAYMENT_BILLING_UI.md) | `PaymentController` | `payment.fxml` | Split Register, `M3SplitButton`, `M3DataTable`, `M3BottomSheet`, Receipt Generator |
| **19** | **Staff RBAC & Security** | [`admin/STAFF_MANAGEMENT_UI.md`](file:///c:/Users/pc/Desktop/JAVA-Project/docs/ui/admin/STAFF_MANAGEMENT_UI.md) | `StaffController` | `staff.fxml` | RBAC Matrix, `M3Switch`, `M3Timeline` (Audit logs), `M3SideSheet` (Overrides) |

---

## 5. Universal Ergonomic & Accessibility Standards

Every screen adheres to strict ergonomics and inclusive design:
1. **Target Sizing**: All touch, mouse, and stylus interactive hitboxes meet or exceed **48×48 dp**, with minimum 8dp inter-element breathing margins.
2. **Global Spotlight Shortcut**: Typing `Ctrl+K` (or `Cmd+K` on macOS) anywhere opens `M3CommandPalette`, offering instant fuzzy-search jumping to any screen, athlete dossier, or 3D equipment inspection.
3. **Keyboard Traversal Order**: Forms maintain logical vertical and reading-order tab loops (`Tab` to advance, `Shift+Tab` to reverse, `ESC` to dismiss floating sheets/dialogs).
4. **Accessible Contrast Ratios**: Text-to-background contrast ratios strictly satisfy **WCAG 2.1 AA** (minimum $4.5:1$ for body copy, $3.0:1$ for large titles and graphical controls).
5. **No Color-Alone Semantics**: Statuses (e.g. `ACTIVE`, `FROZEN`, `CRITICAL`) pair distinct semantic colors with descriptive vector icons (`M3Icon.symbol(...)`) and plain-language badge text.

---

## 6. Implementation Guide for Developers

When writing an FXML or JavaFX Controller for any of these screens:
1. Open the screen's dedicated document from `docs/ui/<folder>/<SCREEN>_UI.md`.
2. Inspect Section 2 (Visual Wireframe) to understand the layout structure.
3. Reference Section 7 (FXML Declaration) to copy the exact XML node hierarchy, namespaces, and `@FXML fx:id` bindings matching `docs/UML.md`.
4. Check Section 10 (Pitfalls) to avoid common runtime errors like hardcoded colors, broken controller bindings, or missing event signatures.

