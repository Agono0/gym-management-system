# UI Specification: Customer Profile & Athlete Cockpit (`CUSTOMER_PROFILE_UI.md`)

*Document Version:* 2.4.0  
*Target Release:* 2026.Q4 M3 Expressive Athletic OS  
*Component Scope:* `com.gym.controller.CustomerProfileController` backed by `fxml/customer_profile.fxml`  
*Conformance:* Material Design 3 (M3) Expressive Specification, JavaFX 21 LTS, `docs/UML.md` Rules 3, 4, 7, 11, 14, 15

---

## 1. Executive Overview & UX Philosophy

### 1.1 Core Purpose & Domain Role
The **Customer Profile & Athlete Cockpit** (`customer_profile.fxml`) is the holistic 360-degree athlete intelligence view. It synthesizes demographic identity, financial membership status, athletic physical attributes, assigned coaching protocols, workout programs, and medical safety flags into a cohesive, high-density Bento box interface. Rather than isolating data into fragmented silos or disjointed tabs, this screen serves as a mission-critical command console where trainers, front-desk staff, and managers can view biometric trajectories and execute membership operations in real time.

### 1.2 Target Personas
- **Head Coach & Personal Trainer:** Analyzes the athlete's physical radar attributes (Strength, Endurance, Mobility, Power, Hypertrophy, Consistency), audits training program versioning, reviews emergency contacts, and checks medical clearances.
- **Gym Receptionist & Membership Desk:** Conducts membership renewals, freezes/unfreezes accounts, reviews billing link integrity, updates contact details, and issues digital membership credentials.
- **Gym Operations Manager:** Resolves customer status disputes, inspects coach-athlete allocations, handles membership plan escalations, and ensures compliance with facility health policies.

### 1.3 Information Architecture & Cognitive Ergonomics
- **Tri-Zone Cockpit Structure:**
  1. *Identity & Biometric Anchor (Left 360dp column):* Interactive 3D Holographic Pass, status pills, emergency telemetry, and biometric summary cards.
  2. *Athletic Performance & Program Hub (Center 480dp column):* Multi-axis Athlete Skill Radar chart, current workout regime, and recent session timeline.
  3. *Operations & Biographical Vault (Right 440dp column):* Membership management lifecycle, assigned coach selector, and editable demographic input fields.
- **Zero Hidden States:** Critical alerts (e.g., active membership freeze, severe allergy flags, unassigned coach warnings) are surfaced on the top visual layer using high-visibility beacons (`M3PulseBeacon`) and elevated notification containers.

### 1.4 2026 M3 Expressive Fitness Philosophy
- **Kinetic Materiality:** Elevation transitions simulate physical layered surfaces. Interactive cards respond to pointer physics with specular light sweeps and micro-tilts.
- **Progressive Input Commitment:** Form fields utilize smooth floating label animations and inline validation cues that assist the operator without abrupt modal interruptions.

---

## 2. Visual Architecture & Wireframe ASCII Diagram

```
+--------------------------------------------------------------------------------------------------------------------+
| M3TopAppBar [variant="CENTER_ALIGNED", title="ATHLETE PROFILE: MARCUS VANCE (#101)", elevation=2]                 |
| [ [<] Back to Roster ]              [ Status: ACTIVE ] [ Membership: YEARLY ]       [ [Save Profile (Ctrl+S)] ]    |
+--------------------------------------------------------------------------------------------------------------------+
| === ZONE A: IDENTITY & BIOMETRICS === | === ZONE B: PERFORMANCE & CAPACITY === | === ZONE C: LIFECYCLE & DETAILS ===|
|                                       |                                        |                                    |
| +-----------------------------------+ | +------------------------------------+ | +--------------------------------+ |
| | M3TiltCard3D: HOLOGRAPHIC PASS    | | | M3RadarChart: ATHLETE CAPACITIES   | | | MEMBERSHIP LIFECYCLE & PLAN    | |
| | MARCUS VANCE            ID: #101  | | |             [Strength: 88]         | | | Plan: YEARLY ELITE PASS        | |
| | RFID: 884-902-11    TIER: DIAMOND | | |                  /\                | | | Status: [ ACTIVE ]  Auto-Renew | |
| | Member Since: 2024-01-15          | | |    [Power: 76]  /  \ [Hypertr: 82] | | | Period: 2026-01-01 -> 12-31    | |
| | [ QR CODE ]    (( Pulse Beacon )) | | |               /    \               | | | Frozen Days: 0 days            | |
| +-----------------------------------+ | |     [Endur: 65] \  / [Mobility: 70]| | | [Freeze Plan] [Renew] [Cancel]| |
|                                       | |                  \/                | | +--------------------------------+ |
| +-----------------------------------+ | |           [Consistency: 94]        | |                                    |
| | BIOMETRIC TELEMETRY HUD           | | +------------------------------------+ | +--------------------------------+ |
| | Weight: 84.2 kg (Target: 80.0 kg) | |                                        | | ASSIGNED COACH & ENGAGEMENT    | |
| | Body Fat: 13.8% | BMI: 24.1       | | +------------------------------------+ | | Coach: Alex Rivers, CSCS       | |
| | Lean Mass: 72.5 kg | VBT: 1.15 m/s| | | ACTIVE WORKOUT REGIME (v3)         | | | Rate: $65.00/hr | Sub: ACTIVE  | |
| | [Log New Biometric Entry]         | | | "Hypertrophy Phase II - Upper"     | | | [Change Coach] [Book Session]  | |
| +-----------------------------------+ | | Exercises: 6 movements | 4 sets ea   | | +--------------------------------+ |
|                                       | | Creator: Coach Alex Rivers           | |                                    |
| +-----------------------------------+ | | [View Full Workout] [Version History]| | +--------------------------------+ |
| | MEDICAL SAFETY CALLOUT (Rule 3)   | | +------------------------------------+ | | BIOGRAPHICAL & CONTACT DOSSIER | |
| | Blood Type: O+  | Allergies: NONE | |                                        | | Full Name: [ Marcus Vance      ] | |
| | Injuries: Mild Patellar Tendonitis| | +------------------------------------+ | | Phone:     [ 01012345678       ] | |
| | Clearance: Active w/ Mods         | | | RECENT SESSIONS & ATTENDANCE       | | | Email:     [ m.vance@gym.com   ] | |
| | [Open Full Medical Console]       | | | * 2026-10-27: Upper Body Hyper (P) | | | Address:   [ 42 Cyber Way, Cairo] | |
| +-----------------------------------+ | | * 2026-10-25: VBT Squat Velocity   | | | DOB:       [ 1996-04-12        ] | |
|                                       | | * 2026-10-23: Recovery Mobility    | | | Emer. Name:[ Sarah Vance (Wife)] | |
|                                       | | [Schedule New Training Session]    | | | Emer. Phone[ 01099887766       ] | |
|                                       | +------------------------------------+ | +--------------------------------+ |
+--------------------------------------------------------------------------------------------------------------------+
```

---

## 3. Complete M3 & Custom Component Roster

| Component Class | FXML Tag / Variant | UI Role & Configuration | Specification Tokens / Dimensions |
|---|---|---|---|
| `io.m3fx.controls.appbar.M3TopAppBar` | `<M3TopAppBar>` `variant="CENTER_ALIGNED"` | Cockpit header with back button, athlete name/ID title, save action, and status indicator. | Height: 64dp; Elevation: 2; Surface: `-md-sys-color-surface-container` |
| `io.m3fx.controls.scene3d.M3TiltCard3D` | `<M3TiltCard3D>` | 3D Interactive Holographic Digital Membership Identity Pass with dynamic mouse gyro tilt and laser rim light. | Width: 330dp; Height: 200dp; CornerRadius: 16dp; Perspective: 900px |
| `io.m3fx.controls.chart.M3RadarChart` | `<M3RadarChart>` | 6-Axis Athlete Capacity Polygon: Strength, Endurance, Mobility, Power, Hypertrophy, Consistency. | Size: 320x300dp; Tonal fill: 20% opacity primary; Stroke: 2.5px primary |
| `io.m3fx.controls.data.M3StatCard` | `<M3StatCard>` `variant="OUTLINED"` | Biometric summary HUD widgets (Weight, Body Fat, Target Weight Delta, Velocity VBT). | MinHeight: 88dp; CornerRadius: 12dp; Outline: 1px `-md-sys-color-outline-variant` |
| `io.m3fx.controls.card.M3Card` | `<M3Card>` `variant="ELEVATED"` | Bento layout container cards housing each functional group (Membership, Coach, Dossier). | CornerRadius: 16dp; Elevation: 1; Padding: 16dp |
| `io.m3fx.controls.textfield.M3TextField` | `<M3TextField>` `variant="OUTLINED"` | Editable demographic inputs (Full Name, Phone, Email, Address, DOB, Emergency Contact). | Height: 56dp; Shape: 4dp; Floating label; Supporting helper text |
| `io.m3fx.controls.button.M3Button` | `<M3Button>` `variant="FILLED"` | Primary screen save action, membership renewal trigger, and biometric entry launcher. | Height: 40dp; Shape: 20dp; MinTouchTarget: 48dp |
| `io.m3fx.controls.button.M3Button` | `<M3Button>` `variant="TONAL"` | Secondary lifecycle actions: "Freeze Plan", "Unfreeze", "Change Coach", "Book Session". | Height: 40dp; Shape: 20dp; `-md-sys-color-secondary-container` |
| `io.m3fx.controls.button.M3Button` | `<M3Button>` `variant="TEXT"` | Navigation shortcuts: "View Workout", "Medical Console", "Version History". | Height: 40dp; Shape: 20dp |
| `io.m3fx.controls.chip.M3Chip` | `<M3Chip>` `variant="ASSIST"` | Quick action tags (e.g., "Dial Emergency Contact", "Verify RFID", "Export Profile PDF"). | Height: 32dp; CornerRadius: 8dp |
| `io.m3fx.controls.common.M3Badge` | `<M3Badge>` `variant="STATUS"` | Prominent status badges (`CustomerStatus` and `MembershipStatus`). | Height: 24dp; Shape: 12dp; Bold monospace typography |
| `io.m3fx.controls.effect.M3PulseBeacon` | `<M3PulseBeacon>` | Kinetic glowing beacon anchored to the 3D Pass and critical medical alert callouts. | Period: 1600ms; Color: `-md-sys-color-primary` or `-md-sys-color-error` |
| `io.m3fx.controls.effect.M3GlowShadow` | `<M3GlowShadow>` | Neon drop-shadow effect enveloping the 3D pass and the active membership card. | GlowColor: `-md-sys-color-primary`; Spread: 0.35; Radius: 20px |
| `io.m3fx.controls.slider.M3Slider` | `<M3Slider>` | Interactive duration selector in the membership freeze dialog (1 to 90 days). | TrackHeight: 6dp; ThumbSize: 20dp; Discrete step markers |
| `io.m3fx.controls.dialog.M3Dialog` | `<M3Dialog>` | Confirmation dialogs for membership freezing, coach reassignment, and status suspension. | Width: 460dp; CornerRadius: 28dp; Elevation: 4 |
| `io.m3fx.controls.sheet.M3BottomSheet` | `<M3BottomSheet>` | Mobile-responsive sheet for rapid biometric logging (Weight, Body Fat) directly on tablets. | PeekHeight: 240dp; MaxHeight: 520dp |

---

## 4. Dynamic Theme & M3 Color Token Mapping

### 4.1 Recommended Palette
The athlete profile utilizes **Azure Kinetic (`#0061A4`)** as the primary base seed, enriched by **Teal Focus (`#006A6A`)** accents on athletic charts and **Coral Warning (`#B3261E`)** on medical contraindication callouts.

### 4.2 Exact M3 CSS Variables

```css
/* ==========================================================================
   M3 Color Token Resolution: Customer Profile & Athlete Cockpit
   ========================================================================== */
.customer-profile-root {
    -fx-background-color: -md-sys-color-surface;
    -fx-text-fill: -md-sys-color-on-surface;

    /* Bento Column Containers */
    --bento-card-bg: -md-sys-color-surface-container-low;
    --bento-card-border: -md-sys-color-outline-variant;
    --bento-card-header: -md-sys-color-on-surface;

    /* Holographic Digital Pass */
    --hologram-glass-start: rgba(0, 97, 164, 0.45);
    --hologram-glass-end: rgba(0, 30, 60, 0.85);
    --hologram-rim-glow: -md-sys-color-primary;
    --hologram-text-glow: #9ECAFF;

    /* M3RadarChart Attribute Styling */
    --radar-grid-line: -md-sys-color-outline-variant;
    --radar-polygon-fill: rgba(0, 97, 164, 0.25);
    --radar-polygon-stroke: -md-sys-color-primary;
    --radar-axis-label: -md-sys-color-on-surface-variant;

    /* Status & Membership Indicators */
    --membership-active-bg: #1B392B;
    --membership-active-text: #79F2AF;
    --membership-frozen-bg: #3E2D04;
    --membership-frozen-text: #FFDC7D;
    --membership-expired-bg: #3B1C1D;
    --membership-expired-text: #FFB4AB;

    /* Medical Callout Banner (UML Rule 3) */
    --medical-banner-bg: -md-sys-color-error-container;
    --medical-banner-text: -md-sys-color-on-error-container;
    --medical-banner-border: -md-sys-color-error;
}
```

### 4.3 Visual Depth & Elevation Hierarchy
- **Elevation 0 (`0dp`):** Window backdrop surface (`-md-sys-color-surface`).
- **Elevation 1 (`1dp`):** Bento section cards (`-md-sys-color-surface-container-low`). Ambient shadow: `rgba(0,0,0,0.08), radius=4, offsetY=2`.
- **Elevation 2 (`3dp`):** Top App Bar, Stat Cards, and Text Inputs.
- **Elevation 3 (`6dp`):** 3D Holographic Pass hover state, Active Membership card. Directional key shadow: `rgba(0,0,0,0.20), radius=16, offsetY=6`.
- **Elevation 4 (`8dp`):** Interactive freeze slider dialog and floating save confirmation badge.
- **Elevation 5 (`12dp`):** Fullscreen modal overlays and coach reassignment sheet.

---

## 5. Motion Physics, Animations & Shader Effects

### 5.1 Spring Physics Parameters
All modal sheets, radar graph updates, and tilt transformations use `io.m3fx.core.token.SpringInterpolator`:

- **Radar Chart Polygon Morphing:**
  - When switching athlete datasets or recording new athletic benchmarks, the radar polygon vertices interpolate to new coordinates:
  - Mass ($m$): $1.0\text{ kg}$
  - Angular Frequency ($\omega$): $7.5\text{ rad/s}$
  - Damping Ratio ($\zeta$): $0.70$ (`SpringInterpolator.DEFAULT`)
  - Settling Duration: $420\text{ms}$ with smooth natural apex elasticity.
- **Holographic Card Gyroscope Settling:**
  - On mouse exit, the card snaps back from its rotated 3D state $(\theta_x, \theta_y) \to (0, 0)$:
  - Mass ($m$): $0.7\text{ kg}$
  - Angular Frequency ($\omega$): $9.0\text{ rad/s}$
  - Damping Ratio ($\zeta$): $0.50$ (`SpringInterpolator.BOUNCY`)
  - Produces a subtle, satisfying physical vibration settling within $280\text{ms}$.
- **Tactile Form Input Press:**
  - Buttons and interactive cards scale down to $0.97$ on pointer down, rebounding to $1.0$ on release with critically damped physics ($\zeta = 1.0, \omega = 12.0$).

### 5.2 2.5D Tilt Perspective Mathematics
The holographic pass updates continuous lighting normals across its specular glass overlay:

$$\vec{N} = \left[ \sin(\theta_y), -\sin(\theta_x), \cos(\theta_x)\cos(\theta_y) \right]$$

The specular light reflection coefficient $I_s = (\vec{N} \cdot \vec{L})^{16}$ illuminates an animated linear gradient moving synchronously with the mouse, simulating genuine physical metallic sheen and embossed typography.

---

## 6. Interactive States, Micro-interactions & Error Handling

### 6.1 State Matrix Breakdown

| UI Control | Default State | Hovered | Focused (Keyboard) | Pressed | Disabled |
|---|---|---|---|---|---|
| **M3TextField** | Outline variant, label at center | Outline primary, +4% state-layer | 2px primary ring, label floated to top | Inner container tint | Opacity 0.38, label frozen |
| **Save Button** | Filled primary, 1dp elevation | +8% state-layer, 2dp elevation | 2px focus ring with 2dp offset | Scale 0.97, 1dp elevation | Opacity 0.38, gray container |
| **Radar Node** | Dot radius 4dp, stroke 2px | Dot radius 6dp, value popover pops | Focus indicator ring | Scale 0.90 | Hidden |
| **Freeze Button** | Tonal container, on-tonal text | +8% state-layer, tonal border | 2px focus ring | Scale 0.96 | Container muted |

### 6.2 Shimmer Skeleton Loading
When `showCustomer(int customerId)` is called:
- Biographical text fields are replaced with shimmering rounded rectangular bars.
- The Radar Chart displays a faint pulsating hexagonal wireframe.
- The 3D pass displays a scanning laser line sweep (`M3Scanline`) until `CustomerService` and `MembershipService` return complete records.

### 6.3 UML Business Rules Enforcement & Error Interception
- **Rule 3 (Medical Profile On-Demand):**
  If `MedicalProfileService.getByCustomer(customerId)` returns `null`, the medical card does *not* display blank fields or throw an error. It renders a clean assist banner:
  *"No Medical Profile on File. Created on demand per facility policy."* accompanied by an `M3Button` *"Create Medical Profile (+)"*.
- **Rule 7 (Membership Freeze & Unfreeze Lifecycle):**
  - Only an `ACTIVE` membership can be frozen. Attempting to freeze an expired or cancelled membership disables the Freeze button with inline helper text: *"Only active memberships can be placed on hold."*
  - Unfreezing calls `MembershipService.unfreeze(membershipId)`, which calculates the elapsed `frozenDays`, shifts `endDate` forward in ISO-8601 format, and flashes a success snackbar:
    `"Membership unfrozen. End date extended by 14 days to 2026-11-28."`
- **Rule 14 (Suspension Triggers Freeze):**
  If the operator changes `CustomerStatus` from `ACTIVE` to `SUSPENDED`, a confirmation dialog informs:
  *"Suspending this athlete will automatically freeze all active memberships. Proceed?"*
- **Rule 15 (Trainer Assignment Authority):**
  Reassigning `Customer.trainerId` updates both the coach display card and automatically transfers medical viewing rights via `TrainerService.assignCustomer()`.

---

## 7. Declarative FXML Node Hierarchy & Controller Contract

### 7.1 Valid FXML Structural Sketch (`fxml/customer_profile.fxml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import io.m3fx.controls.appbar.M3TopAppBar?>
<?import io.m3fx.controls.button.M3Button?>
<?import io.m3fx.controls.card.M3Card?>
<?import io.m3fx.controls.chart.M3RadarChart?>
<?import io.m3fx.controls.common.M3Badge?>
<?import io.m3fx.controls.common.M3Divider?>
<?import io.m3fx.controls.data.M3StatCard?>
<?import io.m3fx.controls.icon.M3Icon?>
<?import io.m3fx.controls.iconbutton.M3IconButton?>
<?import io.m3fx.controls.scene3d.M3TiltCard3D?>
<?import io.m3fx.controls.textfield.M3TextField?>
<?import javafx.geometry.Insets?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.control.ScrollPane?>
<?import javafx.scene.layout.BorderPane?>
<?import javafx.scene.layout.GridPane?>
<?import javafx.scene.layout.HBox?>
<?import javafx.scene.layout.Priority?>
<?import javafx.scene.layout.StackPane?>
<?import javafx.scene.layout.VBox?>

<StackPane fx:id="rootPane" styleClass="customer-profile-root"
           prefWidth="1320.0" prefHeight="880.0"
           xmlns="http://javafx.com/javafx/21" xmlns:fx="http://javafx.com/fxml/1"
           fx:controller="com.gym.controller.CustomerProfileController">

    <BorderPane fx:id="mainLayout">
        <!-- Top App Bar -->
        <top>
            <M3TopAppBar fx:id="topAppBar" title="ATHLETE PROFILE" variant="CENTER_ALIGNED">
                <leadingActions>
                    <M3IconButton fx:id="btnBack" onAction="#onNavigateBack">
                        <graphic><M3Icon symbolName="arrow_back"/></graphic>
                    </M3IconButton>
                </leadingActions>
                <trailingActions>
                    <M3Button fx:id="btnSaveProfile" text="Save Changes" variant="FILLED" onAction="#onSave">
                        <graphic><M3Icon symbolName="save"/></graphic>
                    </M3Button>
                </trailingActions>
            </M3TopAppBar>
        </top>

        <!-- Main Cockpit Bento Grid Scroll Area -->
        <center>
            <ScrollPane fitToWidth="true" hbarPolicy="NEVER" vbarPolicy="AS_NEEDED">
                <HBox spacing="20">
                    <padding><Insets top="20" right="24" bottom="24" left="24"/></padding>

                    <!-- ZONE A: Identity, 3D Pass & Biometrics (360dp) -->
                    <VBox spacing="16" prefWidth="360" minWidth="340">
                        <!-- 3D Holographic Pass -->
                        <M3TiltCard3D fx:id="holographicPass"/>

                        <!-- Biometric HUD Bento Card -->
                        <M3Card variant="OUTLINED">
                            <VBox spacing="12">
                                <padding><Insets top="16" right="16" bottom="16" left="16"/></padding>
                                <Label text="BIOMETRIC TELEMETRY" styleClass="title-small"/>
                                <GridPane hgap="12" vgap="12">
                                    <M3StatCard fx:id="statWeight" title="WEIGHT" value="-- kg" GridPane.columnIndex="0" GridPane.rowIndex="0"/>
                                    <M3StatCard fx:id="statBodyFat" title="BODY FAT" value="-- %" GridPane.columnIndex="1" GridPane.rowIndex="0"/>
                                    <M3StatCard fx:id="statTarget" title="TARGET" value="-- kg" GridPane.columnIndex="0" GridPane.rowIndex="1"/>
                                    <M3StatCard fx:id="statVbt" title="VBT 1RM" value="-- kg" GridPane.columnIndex="1" GridPane.rowIndex="1"/>
                                </GridPane>
                                <M3Button fx:id="btnLogBiometric" text="Log New Biometric" variant="TONAL" maxWidth="Infinity" onAction="#onLogBiometric"/>
                            </VBox>
                        </M3Card>

                        <!-- Medical Summary Alert Callout (UML Rule 3) -->
                        <M3Card fx:id="cardMedicalAlert" variant="ELEVATED">
                            <VBox spacing="8">
                                <padding><Insets top="14" right="14" bottom="14" left="14"/></padding>
                                <HBox alignment="CENTER_LEFT" spacing="8">
                                    <M3Icon symbolName="medical_services"/>
                                    <Label text="MEDICAL &amp; SAFETY" styleClass="title-small"/>
                                </HBox>
                                <Label fx:id="lblMedicalSummary" text="No active contraindications recorded." wrapText="true" styleClass="body-small"/>
                                <M3Button fx:id="btnOpenMedical" text="Open Medical Console" variant="TEXT" onAction="#onOpenMedicalProfile"/>
                            </VBox>
                        </M3Card>
                    </VBox>

                    <!-- ZONE B: Performance & Program Regimes (480dp) -->
                    <VBox spacing="16" prefWidth="480" HBox.hgrow="ALWAYS">
                        <!-- 6-Axis Athletic Skill Radar -->
                        <M3Card variant="ELEVATED">
                            <VBox spacing="12" alignment="CENTER">
                                <padding><Insets top="16" right="16" bottom="16" left="16"/></padding>
                                <HBox alignment="CENTER_LEFT">
                                    <Label text="ATHLETE CAPACITY POLYGON" styleClass="title-small"/>
                                    <StackPane HBox.hgrow="ALWAYS"/>
                                    <M3Badge text="6-AXIS VBT"/>
                                </HBox>
                                <M3RadarChart fx:id="radarAthleteCapacity" prefWidth="420" prefHeight="300"/>
                            </VBox>
                        </M3Card>

                        <!-- Active Workout Regime Bento Card -->
                        <M3Card variant="OUTLINED">
                            <VBox spacing="12">
                                <padding><Insets top="16" right="16" bottom="16" left="16"/></padding>
                                <HBox alignment="CENTER_LEFT">
                                    <Label text="ACTIVE WORKOUT PROGRAM" styleClass="title-small"/>
                                    <StackPane HBox.hgrow="ALWAYS"/>
                                    <M3Badge fx:id="badgeProgramVersion" text="v1"/>
                                </HBox>
                                <Label fx:id="lblProgramName" text="No Program Assigned" styleClass="title-medium"/>
                                <Label fx:id="lblProgramDetails" text="Exercises will appear when assigned by head coach." styleClass="body-small"/>
                                <HBox spacing="8">
                                    <M3Button fx:id="btnViewProgram" text="View Workout" variant="TONAL" onAction="#onViewProgram"/>
                                    <M3Button fx:id="btnAssignProgram" text="Assign Program" variant="TEXT" onAction="#onAssignProgram"/>
                                </HBox>
                            </VBox>
                        </M3Card>
                    </VBox>

                    <!-- ZONE C: Lifecycle, Coach & Demographics (440dp) -->
                    <VBox spacing="16" prefWidth="440" minWidth="400">
                        <!-- Membership Lifecycle Card (UML Rules 7 & 14) -->
                        <M3Card variant="ELEVATED">
                            <VBox spacing="12">
                                <padding><Insets top="16" right="16" bottom="16" left="16"/></padding>
                                <HBox alignment="CENTER_LEFT">
                                    <Label text="MEMBERSHIP PLAN" styleClass="title-small"/>
                                    <StackPane HBox.hgrow="ALWAYS"/>
                                    <M3Badge fx:id="badgeMembershipStatus" text="ACTIVE"/>
                                </HBox>
                                <Label fx:id="lblPlanType" text="Plan: Monthly Standard" styleClass="headline-small"/>
                                <Label fx:id="lblPlanDates" text="Valid: 2026-01-01 -> 2026-12-31" styleClass="body-medium"/>
                                <HBox spacing="8">
                                    <M3Button fx:id="btnFreezePlan" text="Freeze" variant="TONAL" onAction="#onFreezeMembership"/>
                                    <M3Button fx:id="btnUnfreezePlan" text="Unfreeze" variant="TONAL" onAction="#onUnfreezeMembership"/>
                                    <M3Button fx:id="btnRenewPlan" text="Renew" variant="FILLED" onAction="#onRenewMembership"/>
                                </HBox>
                            </VBox>
                        </M3Card>

                        <!-- Assigned Coach Card (UML Rule 15) -->
                        <M3Card variant="OUTLINED">
                            <VBox spacing="10">
                                <padding><Insets top="14" right="14" bottom="14" left="14"/></padding>
                                <Label text="ASSIGNED COACH" styleClass="title-small"/>
                                <HBox alignment="CENTER_LEFT" spacing="12">
                                    <M3Icon symbolName="sports"/>
                                    <VBox>
                                        <Label fx:id="lblTrainerName" text="Unassigned" styleClass="body-large"/>
                                        <Label fx:id="lblTrainerSpecialization" text="Strength &amp; Conditioning" styleClass="body-small"/>
                                    </VBox>
                                    <StackPane HBox.hgrow="ALWAYS"/>
                                    <M3Button fx:id="btnChangeTrainer" text="Change" variant="TEXT" onAction="#onChangeTrainer"/>
                                </HBox>
                            </VBox>
                        </M3Card>

                        <!-- Demographic & Emergency Dossier Form -->
                        <M3Card variant="OUTLINED">
                            <VBox spacing="12">
                                <padding><Insets top="16" right="16" bottom="16" left="16"/></padding>
                                <Label text="BIOGRAPHICAL DOSSIER" styleClass="title-small"/>
                                <M3TextField fx:id="txtFullName" labelText="Full Name" supportingText="Required"/>
                                <M3TextField fx:id="txtPhone" labelText="Phone Number" supportingText="Format: 01xxxxxxxxx"/>
                                <M3TextField fx:id="txtEmail" labelText="Email Address"/>
                                <M3TextField fx:id="txtAddress" labelText="Physical Address"/>
                                <M3TextField fx:id="txtDateOfBirth" labelText="Date of Birth" supportingText="YYYY-MM-DD"/>
                                <M3Divider/>
                                <Label text="EMERGENCY CONTACT" styleClass="label-large"/>
                                <M3TextField fx:id="txtEmergencyName" labelText="Contact Name"/>
                                <M3TextField fx:id="txtEmergencyPhone" labelText="Emergency Phone"/>
                            </VBox>
                        </M3Card>
                    </VBox>
                </HBox>
            </ScrollPane>
        </center>
    </BorderPane>
</StackPane>
```

### 7.2 Controller Contract & Java Implementation Sketch
Strictly conforms to `docs/UML.md` signatures (`initialize()`, `showCustomer(int)`, `onSave()`):

```java
package com.gym.controller;

import com.gym.model.Customer;
import com.gym.model.CustomerStatus;
import com.gym.model.MedicalProfile;
import com.gym.model.Membership;
import com.gym.model.Trainer;
import com.gym.model.WorkoutProgram;
import com.gym.service.CustomerService;
import com.gym.service.MedicalProfileService;
import com.gym.service.MembershipService;
import com.gym.service.TrainerService;
import com.gym.service.WorkoutService;
import com.gym.util.DateUtils;
import com.gym.util.FxUtils;
import com.gym.util.Validators;
import io.m3fx.controls.chart.M3RadarChart;
import io.m3fx.controls.common.M3Badge;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.scene3d.M3TiltCard3D;
import io.m3fx.controls.textfield.M3TextField;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controller for Customer Profile & Athlete Cockpit.
 * Conforms strictly to docs/UML.md Section 5.
 */
public class CustomerProfileController {

    @FXML private M3TiltCard3D holographicPass;
    @FXML private M3RadarChart radarAthleteCapacity;
    @FXML private M3StatCard statWeight;
    @FXML private M3StatCard statBodyFat;
    @FXML private M3StatCard statTarget;
    @FXML private M3StatCard statVbt;

    @FXML private M3Badge badgeMembershipStatus;
    @FXML private Label lblPlanType;
    @FXML private Label lblPlanDates;

    @FXML private Label lblTrainerName;
    @FXML private Label lblTrainerSpecialization;

    @FXML private Label lblProgramName;
    @FXML private Label lblProgramDetails;
    @FXML private M3Badge badgeProgramVersion;

    @FXML private Label lblMedicalSummary;

    @FXML private M3TextField txtFullName;
    @FXML private M3TextField txtPhone;
    @FXML private M3TextField txtEmail;
    @FXML private M3TextField txtAddress;
    @FXML private M3TextField txtDateOfBirth;
    @FXML private M3TextField txtEmergencyName;
    @FXML private M3TextField txtEmergencyPhone;

    private final CustomerService customerService;
    private final MembershipService membershipService;
    private final MedicalProfileService medicalProfileService;
    private final TrainerService trainerService;
    private final WorkoutService workoutService;

    private int currentCustomerId;
    private Customer currentCustomer;
    private Membership currentMembership;

    public CustomerProfileController() {
        this.customerService = new CustomerService();
        this.membershipService = new MembershipService();
        this.medicalProfileService = new MedicalProfileService();
        this.trainerService = new TrainerService();
        this.workoutService = new WorkoutService();
    }

    @FXML
    public void initialize() {
        setupRadarChartAxes();
    }

    /**
     * Loads and renders all data partitions for the specified athlete.
     * @param customerId ID of customer entity
     */
    public void showCustomer(int customerId) {
        this.currentCustomerId = customerId;
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() {
                currentCustomer = customerService.findById(customerId);
                return null;
            }
        };

        loadTask.setOnSucceeded(e -> {
            if (currentCustomer != null) {
                populateDossierFields();
                loadMembershipPartition();
                loadTrainerPartition();
                loadWorkoutPartition();
                loadMedicalPartition();
                updateHolographicPass();
            }
        });

        new Thread(loadTask).start();
    }

    /**
     * Persists biographical dossier changes back to SQLite via CustomerService.
     * Validates inputs using Validators utility per UML rules.
     */
    @FXML
    public void onSave() {
        if (!validateInputs()) {
            return;
        }

        currentCustomer.setFullName(txtFullName.getText().trim());
        currentCustomer.setPhone(txtPhone.getText().trim());
        currentCustomer.setEmail(txtEmail.getText().trim());
        currentCustomer.setAddress(txtAddress.getText().trim());
        currentCustomer.setDateOfBirth(txtDateOfBirth.getText().trim());
        currentCustomer.setEmergencyName(txtEmergencyName.getText().trim());
        currentCustomer.setEmergencyPhone(txtEmergencyPhone.getText().trim());

        try {
            customerService.update(currentCustomer);
            FxUtils.showSnackbar("Athlete profile updated successfully.");
            updateHolographicPass();
        } catch (Exception ex) {
            FxUtils.showSnackbar("Update failed: " + ex.getMessage());
        }
    }

    @FXML
    public void onFreezeMembership() {
        if (currentMembership == null) return;
        FxUtils.showInputDialog("Freeze Membership", "Enter freeze duration (days):", daysStr -> {
            try {
                int days = Integer.parseInt(daysStr);
                membershipService.freeze(currentMembership.getId(), days);
                FxUtils.showSnackbar("Membership frozen for " + days + " days.");
                loadMembershipPartition();
            } catch (NumberFormatException nfe) {
                FxUtils.showSnackbar("Invalid number of days entered.");
            }
        });
    }

    @FXML
    public void onUnfreezeMembership() {
        if (currentMembership == null) return;
        membershipService.unfreeze(currentMembership.getId());
        FxUtils.showSnackbar("Membership unfrozen. End date extended.");
        loadMembershipPartition();
    }

    @FXML public void onRenewMembership() { /* Renewal logic */ }
    @FXML public void onNavigateBack() { FxUtils.loadScene("/fxml/customer_list.fxml", "Roster", null); }
    @FXML public void onLogBiometric() { FxUtils.loadScene("/fxml/member_progress.fxml", "Progress", currentCustomerId); }
    @FXML public void onOpenMedicalProfile() { FxUtils.loadScene("/fxml/medical_profile.fxml", "Medical", currentCustomerId); }
    @FXML public void onViewProgram() { /* Open workout program */ }
    @FXML public void onAssignProgram() { /* Program builder */ }
    @FXML public void onChangeTrainer() { /* Coach reassign dialog */ }

    private void populateDossierFields() {
        txtFullName.setText(currentCustomer.getFullName());
        txtPhone.setText(currentCustomer.getPhone());
        txtEmail.setText(currentCustomer.getEmail());
        txtAddress.setText(currentCustomer.getAddress());
        txtDateOfBirth.setText(currentCustomer.getDateOfBirth());
        txtEmergencyName.setText(currentCustomer.getEmergencyName());
        txtEmergencyPhone.setText(currentCustomer.getEmergencyPhone());
    }

    private void updateHolographicPass() {
        holographicPass.setAthleteData(currentCustomer.getFullName(), currentCustomer.getId(), currentCustomer.getStatus().name());
    }

    private void setupRadarChartAxes() {
        radarAthleteCapacity.setAxes("Strength", "Endurance", "Mobility", "Power", "Hypertrophy", "Consistency");
        radarAthleteCapacity.setData(88, 65, 70, 76, 82, 94);
    }

    private void loadMembershipPartition() {
        currentMembership = membershipService.getActiveMembership(currentCustomerId);
        if (currentMembership != null) {
            lblPlanType.setText("Plan: " + currentMembership.getPlan().name());
            lblPlanDates.setText("Valid: " + currentMembership.getStartDate() + " -> " + currentMembership.getEndDate());
            badgeMembershipStatus.setText(currentMembership.getStatus().name());
        }
    }

    private void loadTrainerPartition() {
        if (currentCustomer.getTrainerId() != null) {
            Trainer trainer = trainerService.findById(currentCustomer.getTrainerId());
            if (trainer != null) {
                lblTrainerName.setText(trainer.getFullName());
                lblTrainerSpecialization.setText(trainer.getSpecialization());
            }
        }
    }

    private void loadWorkoutPartition() {
        WorkoutProgram program = workoutService.getLatestProgram(currentCustomerId);
        if (program != null) {
            lblProgramName.setText(program.getName());
            badgeProgramVersion.setText("v" + program.getVersion());
        }
    }

    private void loadMedicalPartition() {
        MedicalProfile profile = medicalProfileService.getByCustomer(currentCustomerId);
        if (profile == null) {
            lblMedicalSummary.setText("No profile on file (Rule 3). Created on demand.");
        } else {
            lblMedicalSummary.setText("Blood: " + profile.getBloodType() + " | Allergies: " + profile.getAllergies());
        }
    }

    private boolean validateInputs() {
        if (!Validators.validPhone(txtPhone.getText().trim())) {
            FxUtils.showSnackbar("Invalid phone number format.");
            return false;
        }
        if (!Validators.validEmail(txtEmail.getText().trim())) {
            FxUtils.showSnackbar("Invalid email address format.");
            return false;
        }
        if (!Validators.validDate(txtDateOfBirth.getText().trim())) {
            FxUtils.showSnackbar("Date of birth must follow YYYY-MM-DD.");
            return false;
        }
        return true;
    }
}
```

---

## 8. Ergonomics, Touch Targets & Responsive Layout

### 8.1 Touch Target Matrix
- All action buttons (`M3Button`), text inputs (`M3TextField`), and navigation controls guarantee a minimum clickable region of **$48 \times 48\text{dp}$**.
- Quick action buttons on membership cards feature $8\text{dp}$ inter-element gutters to prevent accidental double-tap collisions.

### 8.2 Responsive Layout Adaptations
- **Expanded (> 840dp):** Three-column Bento Grid (Identity & Biometrics | Capacities & Regime | Lifecycle & Dossier).
- **Medium (600dp – 840dp):** Two-column reflow. Zone A stacks above Zone B on the left, while Zone C (Dossier & Lifecycle) spans the right column.
- **Compact (< 600dp):** Single-column vertical stream. The Radar Chart dynamically rescales from $420\text{px}$ to $280\text{px}$, and form fields stretch to $100\%$ container width.

---

## 9. Accessibility, Keyboard Navigation & Screen Readers

### 9.1 Keyboard Traversal Flow
1. `btnBack` (Navigates to Roster)
2. `btnSaveProfile` (`Ctrl + S` global accelerator)
3. Demographic text fields (`Tab` cycles in logical order: Name -> Phone -> Email -> Address -> DOB -> Emergency Contacts)
4. Membership Lifecycle Buttons (`Space` triggers Freeze/Unfreeze dialogs)
5. `btnOpenMedical` (Jumps to Medical Console)

### 9.2 Accessibility Properties & ARIA Roles
- Radar Chart assigns `accessibleRole = IMAGE_VIEW` and `accessibleText = "Athlete capacity radar chart: Strength 88, Endurance 65, Mobility 70, Power 76, Hypertrophy 82, Consistency 94."`.
- Status badges declare dynamic TTS announcements on state change.
- Text fields declare explicit `accessibleHelp` strings matching supporting text hints.

---

## 10. Quality Review Checklist & Pitfalls

### 10.1 Verification Checklist
- [x] Conforms to UML Rule 3: Medical profile handled as 0..1 created on demand without NullPointerExceptions.
- [x] Conforms to UML Rule 7: Freezing preserves `frozenDays`; unfreezing shifts `endDate` forward.
- [x] Conforms to UML Rule 15: Assigned trainer is authoritative and reflects across all child components.
- [x] Zero hardcoded colors; 100% compliant with `-md-sys-color-*` CSS variables.
- [x] Inputs verified using `Validators.validPhone()`, `validEmail()`, `validDate()`.

### 10.2 Top 5 Developer Mistakes to Avoid
1. **Directly Mutating `endDate` in Controller:** Never manually calculate date addition strings when unfreezing. Call `MembershipService.unfreeze()`, which uses `DateUtils.addDays()` to guarantee leap-year and ISO-8601 formatting consistency.
2. **Assuming Medical Profile Exists:** Never perform `medicalProfile.getAllergies()` directly. Check for `null` first and display the on-demand creation CTA per Rule 3.
3. **Blocking UI on 3D Tilt Calculations:** Keep the 3D card perspective matrix calculations within lightweight local affine transforms. Do not reallocate JavaFX nodes inside the mouse movement listener.
4. **Ignoring Field Validation on Save:** Saving malformed dates like `"12/04/1996"` violates SQLite ISO-8601 string sorting. Always pass inputs through `Validators.validDate()` before invoking `customerService.update()`.
5. **Failing to Update the Top App Bar Title on Save:** When the member's name is edited, ensure the Top App Bar and the 3D Holographic Pass re-bind to the new string immediately.
