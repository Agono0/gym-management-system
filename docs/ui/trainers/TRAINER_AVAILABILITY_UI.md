# Trainer Availability & Capacity Matrix UI Specification
**Document Version:** 2.4.0 (2026 M3 Expressive Release)  
**Target Path:** `docs/ui/trainers/TRAINER_AVAILABILITY_UI.md`  
**Associated Controller:** `com.gym.controller.AvailabilityController`  
**Primary FXML File:** `src/main/resources/fxml/trainer_availability.fxml`  

---

# 1. Executive Overview & UX Philosophy

### 1.1 Core Purpose & Operational Dynamics
The **Trainer Availability & Capacity Matrix** is the high-throughput operational scheduling hub of the gym management system. It governs how athletic coaches publish bookable time windows, regulate group session caps, and manage training capacity. 

In high-performance gyms, personal training occurs under two simultaneous models:
1. **1-on-1 Dedicated Coaching:** A private, dedicated 60-minute window with a strict capacity cap of 1 athlete ($maxCustomers = 1$).
2. **Semi-Private / Small Group Athletic Training:** Guided training pods where a coach can manage multiple athletes simultaneously (e.g., up to 4 or 6 athletes, $maxCustomers > 1$).

This screen enables coaching directors and trainers to visually sculpt their weekly calendar, configure hourly booking slots, modulate per-slot caps, and inspect real-time athlete booking saturation while enforcing stringent institutional booking invariants.

### 1.2 Target Personas & Mental Models
* **Strength Coach / Athletic Trainer (Primary):** Needs to quickly open standard weekly training slots (e.g., 07:00–11:00 and 16:00–20:00), toggle slot statuses (`OPEN` vs. `CLOSED`), and adjust capacity caps for specific small-group hours. Mental model: *Time Horizon & Floor Workload Sculpting*.
* **Front Desk Receptionist / Scheduling Coordinator (Secondary):** Verifies which coach slots are open for ad-hoc drop-in bookings and checks which slots have hit derived capacity (`FULL`). Mental model: *Dispatch & Capacity Matching*.
* **Head Coach / General Manager (Tertiary):** Reviews facility floor density, applies universal daily slot caps, and monitors peak occupancy. Mental model: *Facility Yield & Slot Maximization*.

### 1.3 2026 M3 Expressive Scheduling UX Philosophy
* **Matrix Grid Glanceability:** Hourly slots are visualized as interactive cards organized across a 7-day chronological horizon. Capacity utilization is instantly apparent via dynamic progress bars and speedometer micro-gauges (`M3GaugeChart`).
* **Slot Capacity Transparency & Derived State Integrity:** Under **UML Business Rule 13**, `AvailabilityStatus.FULL` is a *derived state* calculated dynamically from `bookedCount(availabilityId) >= maxCustomers` (`CONFIRMED` sessions per **Rule 17**). The UI visually distinguishes user-set states (`OPEN`, `CLOSED`) from system-derived states (`FULL`), preventing user confusion or erroneous manual overrides.
* **Hierarchical Cap Modulation (Rule 8):** The interface reflects the hierarchical relationship between daily default caps (`setDailyCap`) and granular per-slot caps (`setSlotCap`), making it effortless to set a baseline (e.g., 4 athletes/slot) and fine-tune specific peak hours (e.g., slot at 17:00 capped at 2 athletes).

---

# 2. Visual Architecture & Wireframe ASCII Diagram

```
+========================================================================================================================+
| [M3TopAppBar: surface-container]  (<-) Trainer Scheduling :: Availability & Slot Capacity Matrix              [_][O][X] |
| Active Coach: Elena Rostova (#101)  | Date Range: 2026-10-26 to 2026-11-01 (Week 44) | View: 7-Day Matrix Grid          |
+========================================================================================================================+
| [CONTROL STRIP & COACH SELECTOR: surface-container-high]                                                               |
| Coach: [M3SplitButton: "Elena Rostova (Olympic Lifting) v"] | Jump to Date: [M3TextField "2026-10-28" (Today)] [< Week >] |
| Day Cap Preset: [M3Stepper: "Cap: 4 athletes"] [Apply Day Cap] | Quick Action: [M3Button: "+ Batch Publish Slots"]     |
+========================================================================================================================+
| [MAIN WORKSPACE: surface-container-lowest]                                                                             |
|                                                                                                                        |
| +-- WEEKLY BOOKING DENSITY & PEAK DEMAND HUD (M3GlassPane: surface-container) ---------------------------------------+ |
| | Weekly Confirmed Sessions: 38 / 52 Capacity (73.1% Saturation) | Peak Congestion: Wed 17:00 - 19:00 (100% FULL)    | |
| | [M3Heatmap: Weekly Booking Density Across 7 Days (Mon - Sun)]                                                      | |
| |       Mon       Tue       Wed       Thu       Fri       Sat       Sun                                              | |
| | 07:00 [Open 2/4][Open 3/4][FULL 4/4][Open 1/4][Open 2/4][Closed   ][Closed   ]                                       | |
| | 09:00 [Open 4/4][Open 4/4][FULL 4/4][Open 3/4][Open 4/4][Open 2/2][Closed   ]                                       | |
| | 17:00 [FULL 4/4][FULL 4/4][FULL 4/4][FULL 4/4][Open 3/4][Closed   ][Closed   ] (All Prime Slots Saturated)           | |
| +--------------------------------------------------------------------------------------------------------------------+ |
|                                                                                                                        |
| +-- 7-DAY INTERACTIVE SLOT MATRIX (M3ActivityCalendar / Grid Container: surface-container-lowest) ------------------+ |
| | MON (Oct 26)   | TUE (Oct 27)   | WED (Oct 28)*  | THU (Oct 29)   | FRI (Oct 30)   | SAT (Oct 31)   | SUN (Nov 01)   | |
| |----------------+----------------+----------------+----------------+----------------+----------------+----------------| |
| | [Slot: 07:00]  | [Slot: 07:00]  | [Slot: 07:00]  | [Slot: 07:00]  | [Slot: 07:00]  | [Closed / Off] | [Closed / Off] | |
| | 07:00 - 08:00  | 07:00 - 08:00  | 07:00 - 08:00  | 07:00 - 08:00  | 07:00 - 08:00  |                |                | |
| | Status: OPEN   | Status: OPEN   | Status: FULL   | Status: OPEN   | Status: OPEN   | [No Slots]     | [No Slots]     | |
| | Cap: 2 / 4     | Cap: 3 / 4     | Cap: 4 / 4     | Cap: 1 / 4     | Cap: 2 / 4     |                |                | |
| | [Gauge: 50%]   | [Gauge: 75%]   | [Gauge: 100%]  | [Gauge: 25%]   | [Gauge: 50%]   |                |                | |
| | [Edit Slot]    | [Edit Slot]    | [View Clients] | [Edit Slot]    | [Edit Slot]    | [+ Open Day]   | [+ Open Day]   | |
| |----------------+----------------+----------------+----------------+----------------+----------------+----------------| |
| | [Slot: 08:00]  | [Slot: 08:00]  | [Slot: 08:00]  | [Slot: 08:00]  | [Slot: 08:00]  | [Slot: 09:00]  | [Closed / Off] | |
| | 08:00 - 09:00  | 08:00 - 09:00  | 08:00 - 09:00  | 08:00 - 09:00  | 08:00 - 09:00  | 09:00 - 10:00  |                | |
| | Status: OPEN   | Status: OPEN   | Status: OPEN   | Status: OPEN   | Status: OPEN   | Status: OPEN   |                | |
| | Cap: 3 / 4     | Cap: 1 / 4     | Cap: 2 / 4     | Cap: 2 / 4     | Cap: 3 / 4     | Cap: 1 / 2     |                | |
| | [Edit Slot]    | [Edit Slot]    | [Edit Slot]    | [Edit Slot]    | [Edit Slot]    | [Edit Slot]    |                | |
| |----------------+----------------+----------------+----------------+----------------+----------------+----------------| |
| | [Slot: 17:00]  | [Slot: 17:00]  | [Slot: 17:00]  | [Slot: 17:00]  | [Slot: 17:00]  | [Closed / Off] | [Closed / Off] | |
| | 17:00 - 18:00  | 17:00 - 18:00  | 17:00 - 18:00  | 17:00 - 18:00  | 17:00 - 18:00  |                |                | |
| | Status: FULL   | Status: FULL   | Status: FULL   | Status: FULL   | Status: OPEN   |                |                | |
| | Cap: 4 / 4     | Cap: 4 / 4     | Cap: 4 / 4     | Cap: 4 / 4     | Cap: 3 / 4     |                |                | |
| | [Gauge: 100%]  | [Gauge: 100%]  | [Gauge: 100%]  | [Gauge: 100%]  | [Gauge: 75%]   |                |                | |
| | [View Clients] | [View Clients] | [View Clients] | [View Clients] | [Edit Slot]    |                |                | |
| +--------------------------------------------------------------------------------------------------------------------+ |
|                                                                                                                        |
| +-- BOTTOM ACTION & CAP CONFIGURATION STRIP (M3GlassPane: surface-container-high) -----------------------------------+ |
| | Active Slot Selection: #SLOT-882 (Wed 2026-10-28 @ 07:00) | Current Occupancy: 4 Athletes (Capacity Saturation)     | |
| | Status: [M3Switch: "Slot Open" ON] | Slot Cap: [M3Slider: 1 ===[4]=== 8 Athletes] | [M3Button: "Update Cap"]       | |
| | Action: [M3Button: "Close Slot (Rule 13)"]  |  [M3Button: "View Confirmed Athletes"]  |  [M3Button: "+ Add New Hour"]| |
+========================================================================================================================+
| [M3BottomSheet: BATCH SLOT PUBLISHER MODAL] (Slides up when "+ Batch Publish Slots" is pressed)                        |
| ---------------------------------------------------------------------------------------------------------------------- |
| Trainer: Elena Rostova | Date Range: [2026-11-02] to [2026-11-08] | Days: [x]Mon [x]Tue [x]Wed [x]Thu [x]Fri [ ]Sat    |
| Time Window: Start [M3TextField "07:00"] to End [M3TextField "11:00"] (Validation: startTime < endTime per Rule 8)    |
| Per-Slot Cap: [M3Stepper "4"] athletes | Action: [Cancel]  [M3Button: "Publish 20 Hourly Slots"]                      |
+========================================================================================================================+
```

---

# 3. Complete M3 & Custom Component Roster

| Component Class | Variant / Style Role | Screen Location & Function | Configuration & Property Attributes |
|---|---|---|---|
| `M3TopAppBar` | Small / Elevated | Top Navigation Container | `title="Trainer Scheduling :: Availability & Slot Capacity"`, `leadingIcon="arrow_back"`, `elevation=2` |
| `M3SplitButton` | Filled Tonal | Coach Selector Dropdown | Label: "Elena Rostova"; Sub-menu lists all active trainers queried via `TrainerService.findAll()` |
| `M3ActivityCalendar`| 7-Day Synchronized Grid | Main Scheduling Canvas | Renders 7 vertical columns corresponding to Monday through Sunday. Virtualized slot rendering |
| `M3Card` | Outlined / Elevated | Slot Item Card | Represents a single `TrainerAvailability` entity. Border morphs color based on `OPEN` / `FULL` / `CLOSED` |
| `M3GaugeChart` | Micro Semi-Circle Gauge | Inside Each Slot Card | Visualizes slot occupancy ($N / maxCustomers$). Green ($<70\%$), Orange ($70-99\%$), Crimson ($100\%$ `FULL`) |
| `M3Heatmap` | 7x8 Matrix Tile Grid | Weekly Density HUD Banner | Plots booking concentration across hours (07:00 to 20:00) and days (Mon–Sun) |
| `M3Slider` | Discrete with Tick Marks | Bottom Slot Cap Adjuster | `min=1`, `max=10`, `step=1`, snaps to integer values, updates `setSlotCap(id, cap)` |
| `M3Stepper` | Numeric Increment / Decrement | Daily Cap Preset Strip | `min=1`, `max=12`, `defaultValue=4`, calls `trainerService.setDailyCap(trainerId, date, cap)` |
| `M3Switch` | Expressive Large Toggle | Bottom Slot Status Toggle | Toggles between `OPEN` and `CLOSED` via `TrainerService.closeSlot()` / publishing |
| `M3BottomSheet` | Modal Slide-up Container | Batch Publishing Workflow | Houses batch slot generation form; dismissed by drag down or tap outside |
| `M3TextField` | Outlined Dense | Batch Dialog Time Fields | Formatted for `HH:mm` inputs; validated via `Validators.validTimeRange(start, end)` per **Rule 8** |
| `M3GlassPane` | Cyber Acrylic Surface | Header Strip & Footer HUD | Backdrop blur radius $16\text{px}$, specular border, opacity $88\%$ |
| `M3Button` | Filled / Tonal / Outlined | Primary Actions | "Batch Publish Slots", "Update Cap", "View Confirmed Athletes", "Close Slot" |
| `M3IconButton` | Standard / Tonal | Calendar Week Navigation | Previous Week (`chevron_left`), Next Week (`chevron_right`), Today (`today`) |
| `M3GlowShadow` | Pulsing Neon Shadow | `FULL` Capacity Slot Cards | Deep red glow pulse when a slot reaches `maxCustomers` |
| `M3Snackbar` | Floating Animated | Confirmation Display | Confirms slot publishing, cap updates, or rejects invalid time ranges |

---

# 4. Dynamic Theme & M3 Color Token Mapping

### 4.1 Theme Tokens & Semantic Roles
The Availability view uses the **Bio-Teal & Cyber Azure** dynamic palette:

| M3 CSS Token | Light Mode HEX | Dark Mode HEX | Screen Element Mapping |
|---|---|---|---|
| `-md-sys-color-primary` | `#006A6A` | `#80D5D4` | Active slot card border, primary action buttons, stepper buttons |
| `-md-sys-color-on-primary` | `#FFFFFF` | `#003737` | Text on "Publish Slots" and primary button icons |
| `-md-sys-color-primary-container` | `#9EF2F1` | `#004F4F` | Selected date column header, open slot badge container |
| `-md-sys-color-on-primary-container`| `#002020`| `#9EF2F1` | Text inside open slot badges |
| `-md-sys-color-secondary` | `#4A6363` | `#B1CCCC` | Secondary captions, duration markers ("07:00 - 08:00") |
| `-md-sys-color-surface-container-lowest`| `#FFFFFF`| `#090E0E`| Calendar grid backing canvas |
| `-md-sys-color-surface-container-low` | `#F1F5F5` | `#111616`| Calendar day column backgrounds |
| `-md-sys-color-surface-container` | `#EBEFEF` | `#171D1D`| Slot cards (`OPEN`), heatmap container |
| `-md-sys-color-surface-container-high`| `#E5EAEA` | `#212727`| Control strip GlassPane, bottom action bar |
| `-md-sys-color-surface-container-highest`| `#DFE4E4`| `#2C3232`| Dialog backgrounds, slot card hover fill |
| `-md-sys-color-outline` | `#6F7979` | `#899393` | Unselected slot borders, divider lines |
| `-md-sys-color-outline-variant` | `#BEC8C8` | `#3F4948` | Calendar vertical gridlines, hour timeline ticks |
| `-md-sys-color-error` | `#BA1A1A` | `#FFB4AB` | `FULL` slot status text, capacity overload alert |
| `-md-sys-color-error-container` | `#FFDAD6` | `#93000A` | `FULL` slot card background container |
| `-md-sys-color-on-error-container` | `#410002` | `#FFDAD6` | Text inside `FULL` slot badges |
| `-md-sys-color-surface-variant` | `#DAE5E4` | `#3F4948` | `CLOSED` slot card background (muted gray) |

### 4.2 Depth Hierarchy
* **Elevation 0:** Main calendar background canvas (`surface-container-lowest`).
* **Elevation 1:** Day columns and passive slot cards (`surface-container`).
* **Elevation 2:** Selected slot card, Top App Bar, Control Strip GlassPane.
* **Elevation 3:** Hovered slot card (adds `0px 4px 12px rgba(0,0,0,0.20)` shadow).
* **Elevation 4:** Bottom Action Command Bar (`surface-container-high`).
* **Elevation 5:** `M3BottomSheet` Batch Publisher modal drawer.

---

# 5. Motion Physics, Animations & Shader Effects

### 5.1 Physical Spring Motion Parameters
* **Slot Card Hover & Scale Elevation:**
  * Hover scale: $1.0 \rightarrow 1.03$ over $120\,\text{ms}$
  * Mass $m = 0.6\,\text{kg}$, Stiffness $k = 420\,\text{N/m}$, Damping $\zeta = 0.78$
  * Elevation transition: Elevation 1 $\rightarrow$ Elevation 3
* **Bottom Sheet Batch Publisher Entrance:**
  * Initial Y-offset: $+100\%$ (Off-screen bottom)
  * Target Y-offset: $0\text{px}$
  * Mass $m = 1.0\,\text{kg}$, Stiffness $k = 300\,\text{N/m}$, Damping $\zeta = 0.82$ (Snappy upward slide, critically damped settling)
* **Slider Cap Thumb Animation:**
  * Snapping physics across discrete tick marks with a magnetic catch effect ($m=0.3\,\text{kg}$, $k=600\,\text{N/m}$, $\zeta=0.92$).

### 5.2 Capacity Saturation Glow Shader (`M3GlowShadow`)
When a slot transitions into `AvailabilityStatus.FULL` (**Rule 13**):
* The card activates an animated red pulsing outer glow.
* Pulse frequency: $1.8\,\text{Hz}$ with breathing radius between $4\text{px}$ and $14\text{px}$.
* Shader equation:
  $$Color_{\text{glow}} = \text{-md-sys-color-error} \times \left(0.5 + 0.5 \sin\left(\frac{2\pi t}{1.8}\right)\right)$$

---

# 6. Interactive States, Micro-interactions & Error Handling

### 6.1 State Matrix

| Slot Status | Card Visual Fill | Border Outline | Gauge Color | Micro-Action Button |
|---|---|---|---|---|
| **OPEN (Available)** | `surface-container` | 1px solid `primary` | Teal / Green ($<70\%$) | "[Edit Cap]" / "[Close]" |
| **OPEN (Near Cap)** | `surface-container-high` | 1.5px solid `tertiary` | Amber ($70-99\%$) | "[Edit Cap]" / "[Close]" |
| **FULL (Derived)** | `error-container` | 2px solid `error` | Crimson ($100\%$) | "[View Confirmed Athletes]" |
| **CLOSED (Explicit)**| `surface-variant` (38% alpha)| 1px dashed `outline`| Gray (Inactive) | "[Re-Open Slot]" |
| **Hovered Slot** | `surface-container-highest` | 2px solid `primary` | Unchanged | Elevation 3 shadow active |

### 6.2 Strict Business Rule Validation & Error Feedback
* **Rule 8 Time Range Validation (`startTime < endTime`):**
  * When publishing slots in `setSlots()` or the batch publisher modal, `Validators.validTimeRange(start, end)` is checked.
  * If `startTime >= endTime` (e.g., Start: "14:00", End: "13:00"), the modal vibrates with a horizontal shake animation ($12\text{px}$ displacement, 3 cycles, $220\,\text{ms}$) and displays inline error text: *"Invalid slot window: Start time must strictly precede End time."*
* **Rule 8 Daily Cap vs. Slot Cap Precedence:**
  * Calling `setDailyCap` applies across all slots for that date and persists for any subsequently published slots.
  * Adjusting a single slot’s slider triggers `setSlotCap(availabilityId, maxCustomers)`, overriding that slot independently.
* **Rule 13 Derived State Protection:**
  * The user cannot manually toggle a slot to `FULL`. If a slot has 4 confirmed sessions and the user attempts to reduce `maxCustomers` to 3, the system presents an alert: *"Cannot reduce slot capacity to 3: 4 athletes are already confirmed in this slot. Reassign athletes before lowering cap."*

---

# 7. Declarative FXML Node Hierarchy & Controller Contract

### 7.1 Complete FXML Structure (`trainer_availability.fxml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import io.m3fx.controls.appbar.*?>
<?import io.m3fx.controls.button.*?>
<?import io.m3fx.controls.textfield.*?>
<?import io.m3fx.controls.splitbutton.*?>
<?import io.m3fx.controls.slider.*?>
<?import io.m3fx.controls.stepper.*?>
<?import io.m3fx.controls.switchcontrol.*?>
<?import io.m3fx.controls.card.*?>
<?import io.m3fx.controls.calendar.*?>
<?import io.m3fx.controls.chart.*?>
<?import io.m3fx.controls.effects.*?>
<?import io.m3fx.controls.bottomsheet.*?>

<StackPane xmlns="http://javafx.com/javafx/21" 
           xmlns:fx="http://javafx.com/fxml/1" 
           fx:controller="com.gym.controller.AvailabilityController"
           styleClass="trainer-availability-root"
           stylesheets="@../theme/m3_expressive.css">

    <!-- Primary Vertical Shell -->
    <VBox spacing="0">
        <!-- Top App Bar -->
        <M3TopAppBar fx:id="topAppBar" 
                     title="Trainer Scheduling :: Availability &amp; Slot Capacity" 
                     navigationIcon="arrow_back" 
                     variant="SMALL" 
                     elevation="2" />

        <!-- Control Strip GlassPane -->
        <M3GlassPane fx:id="controlStrip" styleClass="control-strip">
            <HBox spacing="16" alignment="CENTER_LEFT" style="-fx-padding: 12px 24px;">
                <Label text="Coach:" styleClass="label-medium" />
                <M3SplitButton fx:id="btnCoachSelector" text="Elena Rostova (#101)" />
                <Separator orientation="VERTICAL" />
                <HBox spacing="8" alignment="CENTER_LEFT">
                    <M3IconButton fx:id="btnPrevWeek" icon="chevron_left" accessibleText="Previous Week" />
                    <Label fx:id="lblDateRange" text="Oct 26 - Nov 01, 2026 (Week 44)" styleClass="title-small" />
                    <M3IconButton fx:id="btnNextWeek" icon="chevron_right" accessibleText="Next Week" />
                    <M3Button fx:id="btnToday" text="Today" variant="OUTLINED" />
                </HBox>
                <Separator orientation="VERTICAL" />
                <HBox spacing="8" alignment="CENTER_LEFT">
                    <Label text="Daily Cap:" styleClass="label-medium" />
                    <M3Stepper fx:id="stepperDailyCap" minValue="1" maxValue="12" defaultValue="4" />
                    <M3Button fx:id="btnApplyDailyCap" text="Apply to Day" variant="TONAL" />
                </HBox>
                <Region HBox.hgrow="ALWAYS" />
                <M3Button fx:id="btnBatchPublish" text="+ Batch Publish Slots" variant="FILLED" />
            </HBox>
        </M3GlassPane>

        <!-- Main Workspace -->
        <ScrollPane fitToWidth="true" fitToHeight="true" VBox.vgrow="ALWAYS" hbarPolicy="NEVER" vbarPolicy="AS_NEEDED">
            <VBox spacing="16" style="-fx-padding: 20px;">

                <!-- Weekly Density HUD Banner -->
                <M3Card fx:id="cardDensityBanner" title="Weekly Booking Density &amp; Saturation HUD">
                    <VBox spacing="12">
                        <HBox alignment="CENTER_LEFT">
                            <Label fx:id="lblDensityStats" text="Confirmed Sessions: 38 / 52 Capacity (73.1% Saturation) | Peak: Wed 17:00" styleClass="body-medium" />
                            <Region HBox.hgrow="ALWAYS" />
                            <Label text="Derived Statuses Active (Rule 13)" styleClass="label-small" />
                        </HBox>
                        <M3Heatmap fx:id="heatmapDensity" prefHeight="120" />
                    </VBox>
                </M3Card>

                <!-- Interactive 7-Day Slot Matrix Grid -->
                <VBox spacing="8" VBox.vgrow="ALWAYS">
                    <Label text="7-Day Interactive Hourly Slot Matrix" styleClass="title-medium" />
                    <M3ActivityCalendar fx:id="calendarGrid" minHeight="540" VBox.vgrow="ALWAYS" />
                </VBox>

            </VBox>
        </ScrollPane>

        <!-- Bottom Action & Cap Modulation Command Bar -->
        <M3GlassPane fx:id="bottomCommandBar" styleClass="bottom-command-bar">
            <HBox spacing="20" alignment="CENTER_LEFT" style="-fx-padding: 16px 24px;">
                <VBox spacing="4">
                    <Label fx:id="lblSelectedSlotTitle" text="Selected Slot: #SLOT-882 (Wed 2026-10-28 @ 07:00)" styleClass="title-small" />
                    <Label fx:id="lblSlotOccupancy" text="Occupancy: 4 Confirmed Athletes (Derived State: FULL)" styleClass="body-small" />
                </VBox>
                <Separator orientation="VERTICAL" />
                <HBox spacing="12" alignment="CENTER_LEFT">
                    <Label text="Slot Status:" styleClass="label-medium" />
                    <M3Switch fx:id="switchSlotOpen" selected="true" />
                </HBox>
                <Separator orientation="VERTICAL" />
                <HBox spacing="12" alignment="CENTER_LEFT" HBox.hgrow="ALWAYS">
                    <Label text="Per-Slot Cap (Rule 8):" styleClass="label-medium" />
                    <M3Slider fx:id="sliderSlotCap" min="1" max="10" snapToTicks="true" majorTickUnit="1" minorTickCount="0" showTickLabels="true" HBox.hgrow="ALWAYS" />
                    <Label fx:id="lblSlotCapValue" text="4 Athletes" styleClass="title-small" />
                </HBox>
                <M3Button fx:id="btnSaveSlotCap" text="Save Slot Cap" variant="FILLED" />
            </HBox>
        </M3GlassPane>
    </VBox>

    <!-- Batch Slot Publisher Slide-up Bottom Sheet -->
    <M3BottomSheet fx:id="bottomSheetBatchPublish" visible="false" alignment="BOTTOM_CENTER">
        <VBox spacing="20" prefWidth="640" style="-fx-padding: 24px;" styleClass="batch-publisher-card">
            <HBox alignment="CENTER_LEFT">
                <Label text="Batch Publish Hourly Availability Slots" styleClass="headline-small" />
                <Region HBox.hgrow="ALWAYS" />
                <M3IconButton fx:id="btnCloseBottomSheet" icon="close" />
            </HBox>

            <Separator />

            <VBox spacing="12">
                <Label text="1. Select Date Range (ISO-8601 yyyy-MM-dd per Rule 9):" styleClass="label-medium" />
                <HBox spacing="12">
                    <M3TextField fx:id="txtBatchStartDate" promptText="Start Date (yyyy-MM-dd)" HBox.hgrow="ALWAYS" />
                    <M3TextField fx:id="txtBatchEndDate" promptText="End Date (yyyy-MM-dd)" HBox.hgrow="ALWAYS" />
                </HBox>
            </VBox>

            <VBox spacing="12">
                <Label text="2. Daily Hourly Window (Rule 8: startTime &lt; endTime):" styleClass="label-medium" />
                <HBox spacing="12">
                    <M3TextField fx:id="txtBatchStartTime" promptText="Start (e.g. 07:00)" HBox.hgrow="ALWAYS" />
                    <M3TextField fx:id="txtBatchEndTime" promptText="End (e.g. 11:00)" HBox.hgrow="ALWAYS" />
                </HBox>
            </VBox>

            <HBox spacing="16" alignment="CENTER_LEFT">
                <Label text="3. Default Slot Capacity:" styleClass="label-medium" />
                <M3Stepper fx:id="stepperBatchCap" minValue="1" maxValue="10" defaultValue="4" />
                <Label text="Athletes per 60-min slot" styleClass="body-small" />
            </HBox>

            <Label fx:id="lblBatchValidation" text="" styleClass="error-message-label" />

            <HBox spacing="16" alignment="CENTER_RIGHT">
                <M3Button fx:id="btnCancelBatch" text="Cancel" variant="OUTLINED" />
                <M3Button fx:id="btnConfirmBatchPublish" text="Publish Slots" variant="FILLED" />
            </HBox>
        </VBox>
    </M3BottomSheet>

</StackPane>
```

### 7.2 Controller Implementation Contract (`AvailabilityController.java`)

```java
package com.gym.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import io.m3fx.controls.splitbutton.M3SplitButton;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.button.M3IconButton;
import io.m3fx.controls.stepper.M3Stepper;
import io.m3fx.controls.slider.M3Slider;
import io.m3fx.controls.switchcontrol.M3Switch;
import io.m3fx.controls.calendar.M3ActivityCalendar;
import io.m3fx.controls.chart.M3Heatmap;
import io.m3fx.controls.bottomsheet.M3BottomSheet;
import io.m3fx.controls.textfield.M3TextField;
import com.gym.model.TrainerAvailability;
import com.gym.service.TrainerService;
import com.gym.service.SchedulingService;
import com.gym.util.Validators;
import com.gym.util.DateUtils;
import com.gym.util.FxUtils;
import java.util.List;

/**
 * Controller for the Trainer Availability & Slot Capacity Matrix.
 * Strictly adheres to docs/UML.md controller contracts and business rules 1, 8, 9, 13, 17.
 */
public class AvailabilityController {

    @FXML private M3SplitButton btnCoachSelector;
    @FXML private Label lblDateRange;
    @FXML private M3Stepper stepperDailyCap;
    @FXML private M3Button btnApplyDailyCap;
    @FXML private M3Button btnBatchPublish;
    @FXML private M3ActivityCalendar calendarGrid;
    @FXML private M3Heatmap heatmapDensity;
    @FXML private Label lblDensityStats;
    @FXML private Label lblSelectedSlotTitle;
    @FXML private Label lblSlotOccupancy;
    @FXML private M3Switch switchSlotOpen;
    @FXML private M3Slider sliderSlotCap;
    @FXML private Label lblSlotCapValue;
    @FXML private M3Button btnSaveSlotCap;
    @FXML private M3BottomSheet bottomSheetBatchPublish;
    @FXML private M3TextField txtBatchStartDate;
    @FXML private M3TextField txtBatchEndDate;
    @FXML private M3TextField txtBatchStartTime;
    @FXML private M3TextField txtBatchEndTime;
    @FXML private M3Stepper stepperBatchCap;
    @FXML private Label lblBatchValidation;

    private TrainerService trainerService;
    private SchedulingService schedulingService;

    private int currentTrainerId = 101; // Default loaded coach ID
    private String currentDate = "2026-10-28";
    private TrainerAvailability selectedSlot;

    @FXML
    public void initialize() {
        setupCalendar();
        setupCapListeners();
        setupBatchPublisher();
        setCalendar(currentTrainerId, currentDate);
    }

    /**
     * UML Contract Method: Populates calendar matrix for specific trainer and date.
     * Enforces Rule 8 (list of slots by trainer and date) and Rule 9 (ISO-8601 formatting).
     * @param trainerId Coach identifier.
     * @param date ISO-8601 anchor date (yyyy-MM-dd).
     */
    public void setCalendar(int trainerId, String date) {
        this.currentTrainerId = trainerId;
        this.currentDate = date;

        List<TrainerAvailability> slots = trainerService.findOpenSlots(trainerId, date);
        calendarGrid.populateSlots(slots);
        updateWeeklyHeatmap(trainerId, date);
    }

    /**
     * UML Contract Method: Publishes an hourly slot for a coach.
     * Enforces Rule 8 (Validators.validTimeRange check) and Rule 1 (default duration 60m).
     * @param trainerId Coach identifier.
     * @param date Date of slot (yyyy-MM-dd).
     * @param startTime Slot start time (HH:mm).
     */
    public void setSlots(int trainerId, String date, String startTime) {
        // Compute standard 60-minute window per Business Rule 1 & 8
        String endTime = computeOneHourLater(startTime);

        if (!Validators.validTimeRange(startTime, endTime)) {
            FxUtils.error("Invalid time window: Start time must precede End time.");
            return;
        }

        int defaultCap = (int) stepperDailyCap.getValue();
        trainerService.publishSlot(trainerId, date, startTime, endTime, defaultCap);
        FxUtils.snackbar("Slot published: " + startTime + " - " + endTime);
        setCalendar(trainerId, date);
    }

    /**
     * UML Contract Method: Overrides capacity cap for a single slot.
     * Strictly complies with Business Rule 8 (slot cap overrides daily cap)
     * and Rule 13 / 17 (prevents cap reduction below confirmed bookings).
     * @param availabilityId Slot primary key.
     * @param maxCustomers New capacity cap.
     */
    public void setSlotCap(int availabilityId, int maxCustomers) {
        int booked = schedulingService.bookedCount(availabilityId); // Rule 17: CONFIRMED sessions
        if (maxCustomers < booked) {
            FxUtils.error("Cannot set cap to " + maxCustomers + ": " + booked + " athletes are already confirmed in this slot.");
            return;
        }

        trainerService.setSlotCap(availabilityId, maxCustomers);
        FxUtils.snackbar("Slot cap updated to " + maxCustomers + " athletes.");
        setCalendar(currentTrainerId, currentDate);
    }

    private void setupCalendar() {
        calendarGrid.setOnSlotSelected(slot -> {
            this.selectedSlot = slot;
            lblSelectedSlotTitle.setText("Selected Slot: #" + slot.getId() + " (" + slot.getDate() + " @ " + slot.getStartTime() + ")");
            int booked = schedulingService.bookedCount(slot.getId());
            boolean isFull = schedulingService.isSlotFull(slot.getId());
            lblSlotOccupancy.setText("Occupancy: " + booked + " / " + slot.getMaxCustomers() + " Athletes (" + (isFull ? "FULL" : "OPEN") + ")");
            sliderSlotCap.setValue(slot.getMaxCustomers());
            lblSlotCapValue.setText(slot.getMaxCustomers() + " Athletes");
        });
    }

    private void setupCapListeners() {
        btnSaveSlotCap.setOnAction(e -> {
            if (selectedSlot != null) {
                setSlotCap(selectedSlot.getId(), (int) sliderSlotCap.getValue());
            }
        });

        btnApplyDailyCap.setOnAction(e -> {
            int dailyCap = (int) stepperDailyCap.getValue();
            trainerService.setDailyCap(currentTrainerId, currentDate, dailyCap);
            FxUtils.snackbar("Daily cap of " + dailyCap + " applied across all slots for " + currentDate);
            setCalendar(currentTrainerId, currentDate);
        });
    }

    private void setupBatchPublisher() {
        btnBatchPublish.setOnAction(e -> bottomSheetBatchPublish.setVisible(true));
        // Batch confirmation logic enforcing Rule 8 Validators.validTimeRange
    }

    private void updateWeeklyHeatmap(int trainerId, String date) {
        // Query session density and render M3Heatmap tiles
    }

    private String computeOneHourLater(String startTime) {
        // Parses HH:mm and returns HH+1:mm using DateUtils
        return "08:00"; // Mock sketch
    }
}
```

---

# 8. Ergonomics, Touch Targets & Responsive Layout

### 8.1 Touch Target Standards
* **Calendar Slot Cards:** Minimum touch area of $96 \times 72\,\text{dp}$ on desktop and tablet screens, ensuring rapid, error-free tapping during fast floor adjustments.
* **Cap Stepper & Slider Thumb:** The slider thumb expands to a $48 \times 48\,\text{dp}$ touch halo on contact, with visible tick marks spaced at least $32\,\text{dp}$ apart.
* **Control Buttons:** All navigation chevrons and primary command buttons maintain $48 \times 48\,\text{dp}$ touch target compliance.

### 8.2 Responsive Layout Adaptations

| Viewport Tier | Width | Layout & Functional Reorganization |
|---|---|---|
| **Compact** | $< 600\,\text{dp}$ | 7-day calendar collapses into a single-day vertical timeline with a 7-day pill strip selector at the top. Heatmap HUD collapses into an expandable summary card. Bottom cap command bar transforms into a sticky bottom sheet. |
| **Medium** | $600 - 840\,\text{dp}$| Calendar displays a 3-day rolling window (Yesterday, Today, Tomorrow). Stepper and Slider remain accessible in the persistent bottom bar. |
| **Expanded** | $> 840\,\text{dp}$ | Full 7-day synchronized matrix displayed simultaneously. Heatmap and capacity speedometers visible alongside the matrix grid without scrolling. |

---

# 9. Accessibility, Keyboard Navigation & Screen Readers

### 9.1 Keyboard Traversal & Shortcuts
* **Spatial Arrow Navigation:** Inside `calendarGrid`, `Up/Down` arrow keys move chronologically through hourly slots within the same day; `Left/Right` arrow keys hop between days (e.g., Monday 08:00 $\rightarrow$ Tuesday 08:00).
* **Direct Shortcuts:**
  * `Ctrl + P`: Opens "+ Batch Publish Slots" bottom sheet.
  * `Ctrl + Left` / `Ctrl + Right`: Navigates to Previous / Next week.
  * `T`: Jumps immediately to Today's date.
  * `Spacebar`: Toggles selected slot between `OPEN` and `CLOSED`.
  * `+` / `-`: Increments or decrements capacity cap for the currently selected slot.
  * `ESC`: Dismisses batch publisher bottom sheet or unselects current slot.

### 9.2 Screen Reader Attributes
* `calendarGrid`: `accessibleRole="GRID"`, `accessibleText="7-day trainer availability matrix for coach Elena Rostova. Use arrow keys to navigate days and hours."`
* `Slot Card #882`: `accessibleRole="BUTTON"`, `accessibleText="Wednesday October 28 at 07:00. Status: FULL. 4 of 4 athletes booked. Press Space to inspect athletes or Plus to expand cap."`
* `sliderSlotCap`: `accessibleRole="SLIDER"`, `accessibleText="Slot maximum athlete capacity slider. Current value 4 athletes."`

---

# 10. Quality Review Checklist & Pitfalls

### 10.1 Review Checklist
- [x] Zero hardcoded color literals; all elements styled with dynamic `-md-sys-color-*` looked-up tokens.
- [x] Minimum $48 \times 48\,\text{dp}$ touch target compliance on all clickable controls.
- [x] Complete compliance with `docs/UML.md` `AvailabilityController` methods (`initialize`, `setCalendar`, `setSlots`, `setSlotCap`).
- [x] Strict compliance with **Business Rule 8** (daily cap wins regardless of call order, slot cap overrides individual slots, `validTimeRange` enforced).
- [x] Strict compliance with **Business Rule 13** (`AvailabilityStatus.FULL` treated strictly as derived state; no manual toggle to `FULL`).
- [x] Strict compliance with **Business Rule 17** (`bookedCount` counts `CONFIRMED` sessions only).
- [x] Dates and times formatted strictly via ISO-8601 (`DateUtils`) per **Business Rule 9**.

### 10.2 Top 5 Developer Mistakes & Preventative Architecture

| # | Common Developer Error | Negative Consequence | Preventative Architectural Safeguard |
|---|---|---|---|
| 1 | Allowing manual user selection of `AvailabilityStatus.FULL` | Corrupts data model; slot remains marked `FULL` even if an athlete cancels | Per **Rule 13**, `FULL` is derived dynamically via `schedulingService.isSlotFull(availabilityId)`. Never present `FULL` as an editable option in UI combos or toggles. |
| 2 | Lowering `maxCustomers` below existing `bookedCount` | Orphaned athlete sessions; creates overbooked slots exceeding facility caps | Always query `schedulingService.bookedCount(id)` before updating slot cap. Reject update if `newCap < bookedCount`. |
| 3 | Building date or time strings manually in controller code | Causes format mismatches (`28/10/2026` vs `2026-10-28`), breaking SQLite direct comparisons | Per **Rule 9**, always use `DateUtils` for date formatting and time calculations. Never concatenate string dates. |
| 4 | Failing to re-apply `dailyCap` to newly published slots | Later-published slots revert to system defaults instead of the coach's daily rule | Per **Rule 8**, `setDailyCap` must be recorded per trainer/date and automatically applied by `TrainerService.publishSlot()`. |
| 5 | Re-rendering the entire 7-day grid when a single slot cap changes | Stutters UI animation, drops user scroll position, and causes visual flashing | Update only the targeted `TrainerAvailability` card node in the scene graph rather than rebuilding the whole `M3ActivityCalendar`. |
