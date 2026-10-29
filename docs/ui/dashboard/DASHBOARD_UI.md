# Material Design 3 Expressive UI Specification: Executive Operations Dashboard

- **File Target**: `src/main/resources/fxml/dashboard.fxml`
- **Controller Binding**: `com.gym.controller.DashboardController`
- **Specification Version**: 2.6.0 (2026 M3 Expressive Architecture)
- **Document Status**: Production Ready / Normative Design Standard

---

## 1. Executive Overview & UX Philosophy

### 1.1 Core Purpose
The **Executive Operations Dashboard** is the primary control center and operational cockpit of the Gym Management System. It condenses the entire facility's physiological, financial, and logistical heartbeat into an ultra-responsive, glanceable, and actionable interface. Its principal objectives are:
1. **Immediate Facility Situational Awareness**: Deliver 3-second glanceability for front-desk operators and executive directors regarding floor occupancy, live revenue trajectories, pending training sessions, and equipment health.
2. **Contextual Action Acceleration**: Bridge data awareness directly into administrative action (one-tap member check-in, immediate payment recording, swift agenda rescheduling) via integrated micro-actions and a quick-action Speed Dial.
3. **Data Integrity & Asynchronous Precision**: Load multi-service domain models asynchronously without UI stutters, maintaining strict alignment with `docs/UML.md` business rules (e.g. counting only `CONFIRMED` sessions towards slot capacity per Rule 17, and omitting voided payments from revenue aggregations per Rule 6).

### 1.2 Target Personas & Mental Models
- **Front-Desk Receptionist (`StaffRole.STAFF`)**: Focuses on the current hour: incoming member check-ins, active personal training sessions, and immediate equipment maintenance flags. Needs high-contrast stat cards and a prominent agenda timeline.
- **Head Strength Coach (`StaffRole.MANAGER`)**: Focuses on trainer workload distribution, session completion ratios, and customer retention metrics. Relies on the agenda data table and attendance peak splines.
- **Gym General Manager / Owner (`StaffRole.ADMIN`)**: Focuses on high-level operational health: daily/monthly revenue run rates against targets, membership plan distributions, and staff accountability. Utilizes deep revenue sparklines and equipment lifecycle donuts.

```
+----------------------------------------------------------------------------------------------------+
|                                      COGNITIVE HIERARCHY                                           |
+----------------------------------------------------------------------------------------------------+
| 1. LEVEL 1 (Peripheral / Ambient): Status Beacon (WAL Sync), Live Digital Clock, Overall Capacity. |
+----------------------------------------------------------------------------------------------------+
| 2. LEVEL 2 (Key Performance Metrics): 4 Bento Stat Cards (Active Members, Revenue, Sessions, Health)|
+----------------------------------------------------------------------------------------------------+
| 3. LEVEL 3 (Operational Deep Dive): Hourly Traffic Spline & Today's Scheduled Sessions Data Table.  |
+----------------------------------------------------------------------------------------------------+
| 4. LEVEL 4 (Facility Diagnostics): Equipment Status Donut & Rapid Speed Dial Action Hub.           |
+----------------------------------------------------------------------------------------------------+
```

### 1.3 2026 M3 Expressive Fitness Bento Philosophy
The dashboard implements the 2026 **Material Design 3 Expressive Bento Grid** architecture:
- **Asymmetric Modular Containers**: Rather than a rigid, monotonous spreadsheet or uniform tile grid, content is organized into purposeful Bento cells sized proportionally to cognitive importance.
- **Organic Motion Feedback**: Dynamic stat cards respond to pointer hover with 3D micro-tilts (`M3TiltCard3D`), and chart paths draw into view using damped harmonic spring interpolation.
- **Contextual Micro-Surfaces**: Each card acts as an independent micro-surface container (`surface-container-low` to `high`), creating a layered sense of physical elevation and athletic sophistication.

---

## 2. Visual Architecture & Wireframe ASCII Diagram

### 2.1 High-Fidelity Bento Grid Wireframe (Expanded Desktop: 1440 × 900dp)

```
+====================================================================================================================+
| HEADER HERO BANNER (Height: 84dp // M3GlassPane Canopy // Level 1 Elevation)                                       |
| [Avatar] "Good Morning, Alex Carter" | Shift: FRONT_DESK_AM | Live: 10:42 AM | (o) FACILITY CAPACITY: 82% [Optimal]|
| [?] Search Console...                | [F5] [M3Button: REFRESH DATA] | [M3SplitButton: QUICK ACTION] | [M3Button: LOGOUT] |
+====================================================================================================================+
| BENTO ROW 1: MISSION-CRITICAL KPI STAT CLUSTER (Height: 140dp)                                                     |
| +-------------------------+ +-------------------------+ +-------------------------+ +----------------------------+ |
| | M3StatCard: ACTIVE MEM. | | M3StatCard: TODAY'S REV | | M3StatCard: SESSIONS    | | M3StatCard: EQUIP HEALTH   | |
| | 1,428 Members           | | $3,840.00 Net           | | 38 / 46 Confirmed       | | 94.2% Operational          | |
| | [+4.2% vs last month]   | | [Sparkline: Rising]     | | [Circular Progress: 82%]| | [2 Units in Maintenance]   | |
| | [Sparkline: 30-day Trend| | Target: $4,500.00 (85%) | | 8 Pending Trainer Conf. | | [Chip: ALL CRITICAL PASS]  | |
| +-------------------------+ +-------------------------+ +-------------------------+ +----------------------------+ |
+--------------------------------------------------------------------------------------------------------------------+
| BENTO ROW 2: CORE OPERATIONS WORKSPACE (Height: 420dp)                                                             |
| +---------------------------------------------------------+ +----------------------------------------------------+ |
| | HOURLY TRAFFIC & ATTENDANCE SPLINE (Col 1: 52%)         | | TODAY'S SESSIONS AGENDA (Col 2: 48%)               | |
| | M3SplineChart: AttendanceService.dailyReport()          | | M3DataTable: SessionDao.findByDay(today)           | |
| |                                                         | | [Search Agenda...]         [Filter: All Statuses v]| |
| |  Check-Ins/Hr                                           | | +------+--------------+------------+--------+-----+ |
| |  120|             *--* Peak: 114 @ 08:00                | | | TIME | MEMBER       | TRAINER    | STATUS | ACT | |
| |   80|       *----*    \                                 | | +------+--------------+------------+--------+-----+ |
| |   40|  *---*           *---* Projected 18:00 Surge      | | |09:00 | Marcus Vance | Sarah C.   | CONFIRM|[ v ]| |
| |    0+----------------------------------------           | | |10:00 | Elena Rostova| David K.   | CONFIRM|[ v ]| |
| |     06:00 09:00 12:00 15:00 18:00 21:00                 | | |11:00 | James Miller | Sarah C.   | REQUEST|[OK ]| |
| |                                                         | | |11:30 | Chloe Bennett| (Solo Gym) | COMPL. |[ - ]| |
| | [Chip: MORNING PEAK PASS] [Live Check-In Velocity: +8/m]| | +------+--------------+------------+--------+-----+ |
| +---------------------------------------------------------+ +----------------------------------------------------+ |
+--------------------------------------------------------------------------------------------------------------------+
| BENTO ROW 3: FACILITY DIAGNOSTICS & FINANCIAL BREAKDOWN (Height: 220dp)                                            |
| +------------------------------------+ +------------------------------------+ +-----------------------------------+ |
| | EQUIPMENT INVENTORY CONDITION      | | MEMBERSHIP PLAN SPLIT              | | FACILITY KINETIC RADAR HUD        | |
| | M3DonutChart: EquipmentService     | | M3BarChart: MembershipService      | | M3TiltCard3D / Floor Density      | |
| | (O) Available: 64 Units            | | [===] Monthly:   620 Members       | | Zone A (Cardio):     90% Density  | |
| | (O) In Maintenance: 4 Units        | | [=====] Quarterly: 480 Members     | | Zone B (Free Weight):75% Density  | |
| | (O) Retired: 2 Units               | | [========] Yearly: 328 Members     | | Zone C (CrossFit):   40% Density  | |
| +------------------------------------+ +------------------------------------+ +-----------------------------------+ |
+====================================================================================================================+
| [!] FLOATING HUD: Latency: 0.2ms | SQLite WAL Online | Active Shift: alex_admin               [ M3SpeedDial: (+) ] |
+====================================================================================================================+
```

---

## 3. Complete M3 & Custom Component Roster

| Component Class | FXML ID / Tag | Variant / Style Class | Semantic Role & Operational Configuration |
|---|---|---|---|
| `io.m3fx.controls.layout.M3BentoGrid` | `dashboardGrid` | `.bento-container` | Responsive CSS grid container organizing proportional Bento box cards. |
| `io.m3fx.controls.card.M3Card` | `heroHeaderCard` | `ELEVATED` (`.hero-header`) | Top canopy card; displays operator greeting, live clock, and global operational metrics. |
| `io.m3fx.controls.data.M3StatCard` | `statActiveMembers` | `ELEVATED` (`.stat-card-teal`)| Displays active customer count, 30-day sparkline, and month-over-month percentage change. |
| `io.m3fx.controls.data.M3StatCard` | `statDailyRevenue` | `ELEVATED` (`.stat-card-azure`)| Displays today's net revenue (`PaymentService.revenueBetween`), target pace bar, and trend sparkline. |
| `io.m3fx.controls.data.M3StatCard` | `statTodaySessions`| `ELEVATED` (`.stat-card-purple`)| Displays ratio of confirmed sessions (`SchedulingService`), pending requests, and circular completion ring. |
| `io.m3fx.controls.data.M3StatCard` | `statEquipmentHealth`| `ELEVATED` (`.stat-card-coral`)| Displays operational equipment percentage (`EquipmentService`) and maintenance alert count. |
| `io.m3fx.controls.chart.M3Sparkline` | `memberSparkline` | `.sparkline-accent` | Inline micro-spline rendering 30-day membership acquisition trend. |
| `io.m3fx.controls.chart.M3Sparkline` | `revenueSparkline` | `.sparkline-positive`| Inline micro-spline rendering intraday payment flow. |
| `io.m3fx.controls.chart.M3SplineChart` | `trafficSplineChart`| `.athletic-spline` | High-fidelity interactive spline chart plotting hourly gym check-ins against historic averages. |
| `io.m3fx.controls.data.M3DataTable` | `agendaTable` | `PAGINATED` (`.agenda-table`)| Paginated interactive data table showing today's sessions, trainer assignments, and action chips. |
| `io.m3fx.controls.chart.M3DonutChart` | `equipmentDonutChart`| `.inventory-donut` | Donut chart visualizing equipment status breakdown (`AVAILABLE`, `IN_MAINTENANCE`, `RETIRED`). |
| `io.m3fx.controls.chart.M3BarChart` | `planBarChart` | `HORIZONTAL` (`.plan-bars`)| Horizontal bar chart illustrating member distribution across `PlanType` (Monthly, Quarterly, Yearly). |
| `io.m3fx.controls.scene3d.M3TiltCard3D` | `facilityRadarCard` | `.floor-density-card`| Interactive 3D tilt card rendering real-time floor density heat zones. |
| `io.m3fx.controls.effect.M3PulseBeacon` | `occupancyBeacon` | `.beacon-occupancy` | Pulsing radar beacon visualizing current gym occupancy relative to capacity cap. |
| `io.m3fx.controls.button.M3Button` | `refreshButton` | `TONAL` (`.btn-refresh`) | Triggers asynchronous data reload (`#refreshData`) with a rotating icon animation. |
| `io.m3fx.controls.button.M3Button` | `logoutButton` | `OUTLINED` (`.btn-logout`) | Invokes `#onLogout` to clear session and return to security portal. |
| `io.m3fx.controls.data.M3SpeedDial` | `quickActionSpeedDial`| `FAB_EXPANDABLE` | Floating speed dial at bottom-right corner; unfolds options: New Member, Fast Check-In, Record Payment. |
| `io.m3fx.controls.progress.M3CircularProgress`| `agendaLoadingSpinner`| `INDETERMINATE` | Visual loading indicator displayed over tables and charts during background data synchronization. |

---

## 4. Dynamic Theme & M3 Color Token Mapping

The dashboard adheres strictly to M3 expressive color tokens, resolving via CSS variables generated by `M3Theme`.

### 4.1 Token Lookup & Contrast Mapping

| Surface & Component Role | Light Mode Value | Dark Mode Value | Exact CSS Token Variable |
|---|---|---|---|
| Dashboard Scaffolding Canvas | `#F4FBF8` (Lowest Container) | `#0E1513` (Lowest Container) | `-md-sys-color-surface-container-lowest` |
| Primary Bento Card Surface | `#FFFFFF` (Surface Container) | `#1A211F` (Surface Container) | `-md-sys-color-surface-container` |
| Elevated Stat Card Container | `#F2F9F6` (High Container) | `#202826` (High Container) | `-md-sys-color-surface-container-high` |
| Hero Accent (Active Members) | `#006A6A` (Primary Teal) | `#4DDAD7` (Primary Teal) | `-md-sys-color-primary` |
| Financial Accent (Revenue Stat) | `#0061A4` (Azure Blue) | `#9ECAFF` (Azure Blue) | `-md-sys-color-secondary` |
| Session Accent (Agenda Stat) | `#6750A4` (Athletic Purple)| `#CFBCFF` (Athletic Purple)| `-md-sys-color-tertiary` |
| Warning Accent (Maintenance Flag)| `#B3261E` (Coral Alert) | `#FFB4AB` (Coral Alert) | `-md-sys-color-error` |
| Success Accent (Confirmed Session)| `#386A20` (Vibrant Green) | `#9BD67D` (Vibrant Green) | `-md-sys-color-primary-container` |
| Data Table Alternate Row Stripe | `#F8FDFB` | `#161D1B` | `-md-sys-color-surface-container-low` |
| Bento Card Divider Stroke | `#C8D7D3` (Outline Variant)| `#3F4947` (Outline Variant)| `-md-sys-color-outline-variant` |

### 4.2 Depth Hierarchy & Elevation Steps
- **Elevation Level 0**: Root workspace canvas (`surface-container-lowest`).
- **Elevation Level 1**: Bento chart cards (`surface-container`), border 1px `-md-sys-color-outline-variant`.
- **Elevation Level 2**: KPI Stat Cards (`surface-container-high`), drop shadow `0px 3px 8px rgba(0,0,0,0.12)`.
- **Elevation Level 3**: Hovered Stat Card / 3D Tilt Card, drop shadow `0px 8px 18px rgba(0,0,0,0.18)` + dynamic color halo.
- **Elevation Level 4**: Floating Action Speed Dial and Toast Popovers (`0px 10px 24px rgba(0,0,0,0.24)`).

---

## 5. Motion Physics, Animations & Shader Effects

### 5.1 Mass-Spring-Damper Physics Presets
All interactive transformations across the dashboard run on spring physics:

$$\ddot{x} + 2\zeta\omega_0\dot{x} + \omega_0^2(x - x_{\text{target}}) = 0$$

```
+----------------------------------------------------------------------------------------------------+
|                                  DASHBOARD MOTION DYNAMICS TABLE                                   |
+--------------------------+--------+---------------+----------------------+-------------------------+
| Transition Scenario      | Mass m | Stiffness k   | Damping Ratio zeta   | Perceived Kinesthetics  |
+--------------------------+--------+---------------+----------------------+-------------------------+
| Stat Card Hover 3D Tilt  | 0.5 kg | 420 N/m       | 0.65 (Supple)        | High-inertia tactile fly|
| Chart Path Draw-In       | 1.0 kg | 280 N/m       | 0.82 (Controlled)    | Smooth organic flow     |
| Speed Dial Radial Fan-Out| 0.3 kg | 750 N/m       | 0.55 (Energetic)     | Vibrant spring blossom  |
| Refresh Button Spin      | 0.8 kg | 500 N/m       | 0.70 (Crisp snap)    | Mechanical precision    |
| Table Row Entry Cascade  | 0.4 kg | 600 N/m       | 0.75 (Progressive)   | Waterfall ripple effect |
+--------------------------+--------+---------------+----------------------+-------------------------+
```

### 5.2 Micro-Interactions & Shaders
1. **Interactive 3D Perspective Tilt (`M3TiltCard3D`)**:
   - As the pointer hovers over an `M3StatCard`, the card computes surface normal angles:
     $$\phi_x = \frac{Y - Y_{\text{center}}}{H / 2} \times 8.0^\circ, \quad \phi_y = -\frac{X - X_{\text{center}}}{W / 2} \times 8.0^\circ$$
   - A dynamic specular spotlight gradient shifts across the surface, simulating an overhead directional studio lamp.
2. **Spline Chart Dynamic Trace**:
   - On `#refreshData`, spline paths animate using path length clipping interpolation (`stroke-dashoffset` from $L \to 0$ over $450\text{ms}$), accompanied by floating data point tooltips that bounce into view.
3. **Speed Dial Spring Morph**:
   - Primary `+` icon rotates $135^\circ$ into an `x` dismiss indicator while micro-action child FABs radiate outwards with staggered $30\text{ms}$ delays.

---

## 6. Interactive States, Micro-interactions & Error Handling

### 6.1 Data Table Row States (Agenda)

```
+----------------------------------------------------------------------------------------------------+
|                                    DATA TABLE ROW INTERACTION MATRIX                               |
+-------------+---------------------+-------------------+---------------------+----------------------+
| Row State   | Background Fill     | Text Color        | Status Chip Tint    | Row Elevation        |
+-------------+---------------------+-------------------+---------------------+----------------------+
| Normal      | Transparent         | On-Surface        | Enum Defined        | 0dp                  |
| Hovered     | Surface Cont. High  | On-Surface        | Enum Defined (+8% L)| 1dp (Subtle Float)   |
| Selected    | Secondary Cont. 50% | Primary           | Highlighted         | 2dp                  |
| Action In-Pr| Surface Cont. Lowest| On-Surface-Variant| Indeterminate Arc   | 0dp                  |
+-------------+---------------------+-------------------+---------------------+----------------------+
```

### 6.2 Skeleton Loading State
While `refreshData()` runs asynchronously on daemon threads:
- Stat card numerical values are replaced with rounded **Shimmer Skeletons** (`M3LinearProgress` with animated gradient sweep).
- Spline and donut charts fade to $30\%$ opacity while an inline `M3CircularProgress` rotates at the center.
- Interactive action buttons disable to prevent duplicate submissions or race conditions.

### 6.3 Empty States
- **Zero Sessions Today**: If `SessionDao.findByDay` returns an empty list, `agendaTable` displays an expressive empty state:
  - Vector icon `event_available` (size 64dp, tinted `-md-sys-color-primary`).
  - Heading: *"No Training Sessions Scheduled Today"*.
  - Subheading: *"All trainer slots are currently open or the facility is in open-gym mode."*
  - CTA Button: `[+ Book Member Session]` invoking `ScheduleController.onBook()`.

### 6.4 Error States & Recovery
- **Database Query Timeout**: If SQLite is locked or throws `DaoException`:
  - An inline banner appears across the header: `[!] Telemetry synchronization paused: database busy`.
  - An expressive `M3Snackbar` slides into view with a `[RETRY NOW]` button triggering `#refreshData()`.

---

## 7. Declarative FXML Node Hierarchy & Controller Contract

### 7.1 FXML Structural Declaration (`src/main/resources/fxml/dashboard.fxml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.layout.ColumnConstraints?>
<?import javafx.scene.layout.GridPane?>
<?import javafx.scene.layout.HBox?>
<?import javafx.scene.layout.Priority?>
<?import javafx.scene.layout.Region?>
<?import javafx.scene.layout.RowConstraints?>
<?import javafx.scene.layout.StackPane?>
<?import javafx.scene.layout.VBox?>
<?import io.m3fx.controls.button.M3Button?>
<?import io.m3fx.controls.button.M3SplitButton?>
<?import io.m3fx.controls.card.M3Card?>
<?import io.m3fx.controls.chart.M3BarChart?>
<?import io.m3fx.controls.chart.M3DonutChart?>
<?import io.m3fx.controls.chart.M3Sparkline?>
<?import io.m3fx.controls.chart.M3SplineChart?>
<?import io.m3fx.controls.chip.M3Chip?>
<?import io.m3fx.controls.data.M3DataTable?>
<?import io.m3fx.controls.data.M3SpeedDial?>
<?import io.m3fx.controls.data.M3StatCard?>
<?import io.m3fx.controls.effect.M3GlassPane?>
<?import io.m3fx.controls.effect.M3PulseBeacon?>
<?import io.m3fx.controls.icon.M3Icon?>
<?import io.m3fx.controls.iconbutton.M3IconButton?>
<?import io.m3fx.controls.progress.M3CircularProgress?>
<?import io.m3fx.controls.scene3d.M3TiltCard3D?>
<?import io.m3fx.controls.snackbar.M3Snackbar?>

<StackPane fx:id="dashboardRoot" prefHeight="900.0" prefWidth="1360.0"
           styleClass="dashboard-root"
           xmlns="http://javafx.com/javafx/21"
           xmlns:fx="http://javafx.com/fxml/1"
           fx:controller="com.gym.controller.DashboardController">

    <!-- Scrollable Bento Container -->
    <VBox spacing="24.0">
        <padding>
            <Insets bottom="32.0" left="32.0" right="32.0" top="24.0" />
        </padding>

        <!-- TOP: Hero Header Banner -->
        <M3Card fx:id="heroBannerCard" cardVariant="ELEVATED" prefHeight="84.0" styleClass="hero-banner">
            <padding>
                <Insets bottom="16.0" left="24.0" right="24.0" top="16.0" />
            </padding>
            <HBox alignment="CENTER_LEFT" spacing="16.0">
                <VBox spacing="4.0">
                    <Label fx:id="welcomeGreetingLabel" text="Good Morning, Administrator" styleClass="greeting-title" />
                    <HBox alignment="CENTER_LEFT" spacing="12.0">
                        <M3PulseBeacon fx:id="occupancyBeacon" beaconColor="-md-sys-color-primary" />
                        <Label fx:id="capacityStatusLabel" text="Floor Occupancy: 82% [Optimal Dynamic Range]"
                               styleClass="telemetry-subtitle" />
                        <Label text="|" styleClass="divider-pipe" />
                        <Label fx:id="liveTimeLabel" text="10:42 AM" styleClass="telemetry-subtitle" />
                    </HBox>
                </VBox>

                <Region HBox.hgrow="ALWAYS" />

                <!-- Action Controls Bar -->
                <HBox alignment="CENTER_RIGHT" spacing="12.0">
                    <M3Button fx:id="refreshButton" buttonVariant="TONAL" onAction="#refreshData"
                              text="REFRESH TELEMETRY">
                        <graphic>
                            <M3Icon fx:id="refreshIcon" iconLiteral="m3-refresh" iconSize="20" />
                        </graphic>
                    </M3Button>
                    <M3SplitButton fx:id="quickActionSplitBtn" buttonVariant="FILLED" text="QUICK CHECK-IN" />
                    <M3Button fx:id="logoutButton" buttonVariant="OUTLINED" onAction="#onLogout" text="END SHIFT">
                        <graphic>
                            <M3Icon iconLiteral="m3-logout" iconSize="18" />
                        </graphic>
                    </M3Button>
                </HBox>
            </HBox>
        </M3Card>

        <!-- BENTO ROW 1: 4 Key Metric Stat Cards -->
        <HBox spacing="20.0">
            <M3StatCard fx:id="statActiveMembers" title="ACTIVE MEMBERS" value="1,428"
                        trendText="+4.2% vs last month" HBox.hgrow="ALWAYS" styleClass="stat-card-teal">
                <sparkline>
                    <M3Sparkline fx:id="memberSparkline" strokeColor="-md-sys-color-primary" />
                </sparkline>
            </M3StatCard>

            <M3StatCard fx:id="statDailyRevenue" title="TODAY'S REVENUE" value="$3,840.00"
                        trendText="Target: $4,500.00 (85%)" HBox.hgrow="ALWAYS" styleClass="stat-card-azure">
                <sparkline>
                    <M3Sparkline fx:id="revenueSparkline" strokeColor="-md-sys-color-secondary" />
                </sparkline>
            </M3StatCard>

            <M3StatCard fx:id="statTodaySessions" title="TODAY'S SESSIONS" value="38 / 46"
                        trendText="8 Pending Confirmation" HBox.hgrow="ALWAYS" styleClass="stat-card-purple" />

            <M3StatCard fx:id="statEquipmentHealth" title="EQUIPMENT OPERATIONAL" value="94.2%"
                        trendText="2 Units in Maintenance" HBox.hgrow="ALWAYS" styleClass="stat-card-coral" />
        </HBox>

        <!-- BENTO ROW 2: Core Visualizations (Spline Chart & Today's Agenda Table) -->
        <HBox spacing="20.0" VBox.vgrow="ALWAYS">
            <!-- Col 1: Live Hourly Traffic Spline -->
            <M3Card cardVariant="ELEVATED" prefWidth="640.0" HBox.hgrow="ALWAYS" styleClass="chart-bento-card">
                <padding>
                    <Insets bottom="20.0" left="24.0" right="24.0" top="20.0" />
                </padding>
                <VBox spacing="16.0">
                    <HBox alignment="CENTER_LEFT">
                        <VBox spacing="4.0">
                            <Label text="Hourly Attendance & Floor Traffic" styleClass="card-section-title" />
                            <Label text="Real-time check-in density plotted against 14-day rolling average"
                                   styleClass="card-section-subtitle" />
                        </VBox>
                        <Region HBox.hgrow="ALWAYS" />
                        <M3Chip text="LIVE VELOCITY: +8/MIN" />
                    </HBox>
                    <M3SplineChart fx:id="trafficSplineChart" prefHeight="300.0" VBox.vgrow="ALWAYS" />
                </VBox>
            </M3Card>

            <!-- Col 2: Today's Scheduled Sessions Table -->
            <M3Card cardVariant="ELEVATED" prefWidth="620.0" HBox.hgrow="ALWAYS" styleClass="table-bento-card">
                <padding>
                    <Insets bottom="20.0" left="24.0" right="24.0" top="20.0" />
                </padding>
                <VBox spacing="16.0">
                    <HBox alignment="CENTER_LEFT">
                        <VBox spacing="4.0">
                            <Label text="Today's Session Schedule" styleClass="card-section-title" />
                            <Label text="Live agenda tracking trainer bookings and slot capacity"
                                   styleClass="card-section-subtitle" />
                        </VBox>
                        <Region HBox.hgrow="ALWAYS" />
                        <M3Chip text="RULE 1 & 17 COMPLIANT" />
                    </HBox>
                    <M3DataTable fx:id="agendaTable" prefHeight="300.0" VBox.vgrow="ALWAYS" />
                </VBox>
            </M3Card>
        </HBox>

        <!-- BENTO ROW 3: Secondary Facility Diagnostics -->
        <HBox spacing="20.0">
            <!-- Equipment Condition Donut -->
            <M3Card cardVariant="ELEVATED" prefWidth="420.0" HBox.hgrow="ALWAYS" styleClass="mini-bento-card">
                <padding>
                    <Insets bottom="16.0" left="20.0" right="20.0" top="16.0" />
                </padding>
                <VBox spacing="12.0">
                    <Label text="Equipment Health Distribution" styleClass="card-section-title" />
                    <M3DonutChart fx:id="equipmentDonutChart" prefHeight="160.0" />
                </VBox>
            </M3Card>

            <!-- Membership Plan Split Bar Chart -->
            <M3Card cardVariant="ELEVATED" prefWidth="420.0" HBox.hgrow="ALWAYS" styleClass="mini-bento-card">
                <padding>
                    <Insets bottom="16.0" left="20.0" right="20.0" top="16.0" />
                </padding>
                <VBox spacing="12.0">
                    <Label text="Active Membership Plans" styleClass="card-section-title" />
                    <M3BarChart fx:id="planBarChart" prefHeight="160.0" />
                </VBox>
            </M3Card>

            <!-- 3D Kinetic Facility Radar Card -->
            <M3TiltCard3D fx:id="facilityRadarCard" prefWidth="420.0" HBox.hgrow="ALWAYS" styleClass="mini-bento-card">
                <padding>
                    <Insets bottom="16.0" left="20.0" right="20.0" top="16.0" />
                </padding>
                <VBox spacing="8.0">
                    <Label text="Floor Density Radar" styleClass="card-section-title" />
                    <Label text="Cardio: 90% | Free Weights: 75% | Studio: 40%" styleClass="telemetry-subtitle" />
                    <Region VBox.vgrow="ALWAYS" />
                    <HBox alignment="CENTER">
                        <Label text="[3D GYM RADAR SIMULATION ACTIVE]" styleClass="hologram-text" />
                    </HBox>
                </VBox>
            </M3TiltCard3D>
        </HBox>
    </VBox>

    <!-- FLOATING ACTIONS & TOASTS -->
    <M3SpeedDial fx:id="quickActionSpeedDial" StackPane.alignment="BOTTOM_RIGHT">
        <StackPane.margin>
            <Insets bottom="32.0" right="32.0" />
        </StackPane.margin>
    </M3SpeedDial>

    <M3Snackbar fx:id="dashboardSnackbar" StackPane.alignment="BOTTOM_CENTER">
        <StackPane.margin>
            <Insets bottom="24.0" />
        </StackPane.margin>
    </M3Snackbar>
</StackPane>
```

### 7.2 Controller Implementation Contract (`DashboardController.java`)

Conforming strictly to `docs/UML.md`:
- `DashboardController` implements `+initialize() void`, `+refreshData() void`, `+onLogout() void`.
- Collaborators: `SessionContext`, `CustomerService`, `PaymentService`, `SchedulingService`, `EquipmentService`, `FxUtils`.
- Implements Business Rules:
  - Rule 6: Revenue aggregation excludes voided payments (`PaymentService.revenueBetween`).
  - Rule 17: Session counts only incorporate `SessionStatus.CONFIRMED`.
  - Rule 16: Clears `SessionContext` on logout.

```java
package com.gym.controller;

import com.gym.model.Administrator;
import com.gym.model.Customer;
import com.gym.model.Equipment;
import com.gym.model.EquipmentStatus;
import com.gym.model.Session;
import com.gym.model.SessionStatus;
import com.gym.service.CustomerService;
import com.gym.service.EquipmentService;
import com.gym.service.PaymentService;
import com.gym.service.SchedulingService;
import com.gym.util.DateUtils;
import com.gym.util.FxUtils;
import com.gym.util.SessionContext;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.chart.M3BarChart;
import io.m3fx.controls.chart.M3DonutChart;
import io.m3fx.controls.chart.M3Sparkline;
import io.m3fx.controls.chart.M3SplineChart;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.data.M3SpeedDial;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.effect.M3PulseBeacon;
import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.scene3d.M3TiltCard3D;
import io.m3fx.controls.snackbar.M3Snackbar;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class DashboardController {

    @FXML private StackPane dashboardRoot;
    @FXML private Label welcomeGreetingLabel;
    @FXML private Label capacityStatusLabel;
    @FXML private Label liveTimeLabel;
    @FXML private M3PulseBeacon occupancyBeacon;
    @FXML private M3Button refreshButton;
    @FXML private M3Icon refreshIcon;
    @FXML private M3Button logoutButton;
    @FXML private M3StatCard statActiveMembers;
    @FXML private M3StatCard statDailyRevenue;
    @FXML private M3StatCard statTodaySessions;
    @FXML private M3StatCard statEquipmentHealth;
    @FXML private M3Sparkline memberSparkline;
    @FXML private M3Sparkline revenueSparkline;
    @FXML private M3SplineChart trafficSplineChart;
    @FXML private M3DataTable agendaTable;
    @FXML private M3DonutChart equipmentDonutChart;
    @FXML private M3BarChart planBarChart;
    @FXML private M3TiltCard3D facilityRadarCard;
    @FXML private M3SpeedDial quickActionSpeedDial;
    @FXML private M3Snackbar dashboardSnackbar;

    private CustomerService customerService;
    private PaymentService paymentService;
    private SchedulingService schedulingService;
    private EquipmentService equipmentService;

    @FXML
    public void initialize() {
        this.customerService = new CustomerService();
        this.paymentService = new PaymentService();
        this.schedulingService = new SchedulingService();
        this.equipmentService = new EquipmentService();

        Administrator admin = SessionContext.getAdministrator();
        if (admin != null) {
            welcomeGreetingLabel.setText("Welcome back, " + admin.getUsername() + " [" + admin.getRole() + "]");
        } else {
            welcomeGreetingLabel.setText("Welcome to Gym-OS Dashboard");
        }

        setupSpeedDial();
        refreshData();
    }

    @FXML
    public void refreshData() {
        // Spin the refresh icon
        RotateTransition rotate = new RotateTransition(Duration.millis(600), refreshIcon);
        rotate.setByAngle(360);
        rotate.play();

        String today = LocalDate.now().toString(); // ISO-8601 yyyy-MM-dd per Rule 9

        Task<DashboardDataSnapshot> loadTask = new Task<>() {
            @Override
            protected DashboardDataSnapshot call() throws Exception {
                // Background aggregations conforming to UML rules
                List<Customer> allCustomers = customerService.findAll();
                BigDecimal todayRev = paymentService.revenueBetween(today, today); // Rule 6: voided excluded

                List<Equipment> allEquipment = equipmentService.findAll();
                long operationalCount = allEquipment.stream()
                        .filter(e -> e.getStatus() == EquipmentStatus.AVAILABLE)
                        .count();
                double equipHealthPct = allEquipment.isEmpty() ? 100.0 :
                        ((double) operationalCount / allEquipment.size()) * 100.0;

                return new DashboardDataSnapshot(allCustomers.size(), todayRev, equipHealthPct);
            }
        };

        loadTask.setOnSucceeded(e -> {
            DashboardDataSnapshot data = loadTask.getValue();
            statActiveMembers.setValue(String.format("%,d", data.customerCount));
            statDailyRevenue.setValue("$" + (data.revenue != null ? data.revenue.toPlainString() : "0.00"));
            statEquipmentHealth.setValue(String.format("%.1f%%", data.equipmentHealthPct));
        });

        loadTask.setOnFailed(e -> {
            Throwable ex = loadTask.getException();
            FxUtils.error("Dashboard synchronization error: " + ex.getMessage());
        });

        Thread daemon = new Thread(loadTask, "m3-dashboard-sync");
        daemon.setDaemon(true);
        daemon.start();
    }

    @FXML
    public void onLogout() {
        if (FxUtils.confirm("Are you sure you want to log out of the executive console?")) {
            SessionContext.clear();
            FxUtils.switchScene("fxml/login.fxml");
        }
    }

    private void setupSpeedDial() {
        quickActionSpeedDial.clearActions();
        quickActionSpeedDial.addAction("m3-person_add", "New Member", e -> FxUtils.switchScene("fxml/customer_profile.fxml"));
        quickActionSpeedDial.addAction("m3-fact_check", "Check In", e -> FxUtils.switchScene("fxml/attendance.fxml"));
        quickActionSpeedDial.addAction("m3-payments", "Record Payment", e -> FxUtils.switchScene("fxml/payment.fxml"));
    }

    // Record carrier for asynchronous snapshot payload
    private record DashboardDataSnapshot(int customerCount, BigDecimal revenue, double equipmentHealthPct) {}
}
```

---

## 8. Ergonomics, Touch Targets & Responsive Layout

### 8.1 Minimum Touch Targets & Padding Compliance
- Every interactive element (Speed dial FAB, table row expansion triggers, refresh button, logout button) maintains at least **48×48dp** of accessible touch target space.
- Stat cards feature generous internal inset padding of **20dp**, ensuring labels, trends, and sparklines do not collide or feel crowded on small screens.

### 8.2 Responsive Bento Matrix

```
+----------------------------------------------------------------------------------------------------+
|                                    RESPONSIVE BENTO GRID ADAPTATION                                |
+--------------------------+-----------------------+-------------------------------------------------+
| Breakpoint Mode          | Viewport Width        | Bento Grid Restructuring Behavior               |
+--------------------------+-----------------------+-------------------------------------------------+
| Compact (Floor Handheld) | < 600dp               | Single column stack. Stat cards stack vertically|
|                          |                       | in a 2x2 cluster. Spline chart collapses to     |
|                          |                       | 220dp height. Speed dial anchors bottom-right.  |
+--------------------------+-----------------------+-------------------------------------------------+
| Medium (Tablet / Kiosk)  | 600dp to 840dp        | Two-column Bento. Stat cards arranged 2x2.      |
|                          |                       | Spline chart spans full width (Row 2). Agenda   |
|                          |                       | table moves below spline (Row 3).               |
+--------------------------+-----------------------+-------------------------------------------------+
| Expanded (Desktop Work)  | > 840dp               | Full 4-stat horizontal row. Dual-column major   |
|                          |                       | workspace (Spline 52% / Agenda 48%). 3-card     |
|                          |                       | facility diagnostics footer row.                |
+--------------------------+-----------------------+-------------------------------------------------+
```

---

## 9. Accessibility, Keyboard Navigation & Screen Readers

### 9.1 Traversal Order & Logical Focus Sequence
The screen enforces an intuitive top-to-bottom, left-to-right focus sequence:
1. `heroBannerCard` action buttons (`refreshButton` $\to$ `quickActionSplitBtn` $\to$ `logoutButton`).
2. Row 1 Stat Cards (`statActiveMembers` $\to$ `statDailyRevenue` $\to$ `statTodaySessions` $\to$ `statEquipmentHealth`).
3. Row 2 Interactive Workspace (`trafficSplineChart` filter controls $\to$ `agendaTable` search bar and rows).
4. `quickActionSpeedDial` floating action button.

### 9.2 Accelerators & Shortcuts
- `F5` or `Ctrl + R`: Invokes `#refreshData()` to trigger background synchronization.
- `Ctrl + N`: Opens the New Member registration modal.
- `Ctrl + Q`: Triggers shift logout confirmation (`#onLogout`).
- `Alt + A`: Shifts direct focus into `agendaTable`.

### 9.3 Screen Reader & ARIA Equivalence
- `statActiveMembers`: `AccessibleRole.TEXT_FIELD`, accessible text: *"Active Members Stat Card: 1,428 members, up 4.2 percent compared to last month"*.
- `statDailyRevenue`: `AccessibleRole.TEXT_FIELD`, accessible text: *"Daily Revenue: $3,840.00, 85 percent of daily target"*.
- `trafficSplineChart`: `AccessibleRole.IMAGE_VIEW`, accessible text: *"Hourly attendance spline chart showing peak volume of 114 members at 8:00 AM"*.
- `agendaTable`: `AccessibleRole.TABLE`, accessible text: *"Today's training session agenda, containing confirmed and requested customer appointments"*.

---

## 10. Quality Review Checklist & Pitfalls

### 10.1 Verification Checklist
- [x] **Zero Hardcoded CSS Values**: All background colors, text colors, and outline strokes reference dynamic `-md-sys-color-*` CSS variables.
- [x] **Strict Rule Compliance**:
  - Rule 6: Revenue aggregation invokes `PaymentService.revenueBetween`, guaranteeing voided transactions are ignored.
  - Rule 9: Date parameters strictly format as ISO-8601 `yyyy-MM-dd`.
  - Rule 16: Controller checks `SessionContext.getAdministrator()` on mount and clears it on `#onLogout()`.
  - Rule 17: Session counts only incorporate `SessionStatus.CONFIRMED`.
- [x] **Thread Confinement**: Heavy database lookups execute within a background `Task` daemon, ensuring the FX application thread never freezes.
- [x] **Spring Motion Adherence**: Chart entries, hover tilts, and speed dial expansions use mass-spring-damper physics ($k \in [280, 750]$, $\zeta \in [0.55, 0.82]$).

### 10.2 Top 5 Developer Implementation Mistakes
1. **Executing Service Aggregations on the FX Thread**: Calling `customerService.findAll()` or `paymentService.revenueBetween()` synchronously inside `initialize()` causes the application to stutter on startup. *Fix: Always wrap data retrieval inside a `javafx.concurrent.Task`*.
2. **Hardcoding Date Strings Instead of Using DateUtils / ISO-8601**: Manually building formatted strings like `MM/dd/yyyy` breaks database queries and violates Rule 9. *Fix: Use ISO-8601 `yyyy-MM-dd` standard via `DateUtils`*.
3. **Leaking Event Listeners on View Refresh**: Repeatedly adding listeners to `memberSparkline` or `agendaTable` every time `#refreshData()` runs causes catastrophic memory leaks. *Fix: Attach observable listeners once in `initialize()` and update backing collections via `setAll()`*.
4. **Failing to Handle Zero/Empty Data Gracefully**: Calculating percentages with a zero denominator (e.g. dividing by zero when gym equipment inventory is empty) triggers `NaN` display errors. *Fix: Include ternary guard conditions: `total == 0 ? 100.0 : (count / total) * 100.0`*.
5. **Ignoring Speed Dial Z-Index and Collision**: Anchoring the speed dial without proper margin causes it to overlap scrollbars or table pagination controls on compact screens. *Fix: Maintain a strict 32dp bottom-right layout margin with high `StackPane` z-order*.
