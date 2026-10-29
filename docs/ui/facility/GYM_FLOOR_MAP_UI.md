# 2.5D Interactive Blueprint & 3D Linking UI Specification
## Screen: `GYM_FLOOR_MAP_UI.md` | Activity 15 of 19

> **Document Type:** Masterclass UI/UX Architectural Specification  
> **Package / Target:** `com.gym.controller.GymFloorMapController` (`gym-floor-map.fxml`)  
> **Design Standard:** Material Design 3 (M3) Expressive & Cybernetic Spatial Facility Blueprint  
> **Associated Domain Entities:** `Equipment`, `Attendance`, `Customer`, `EquipmentStatus`  
> **Business Rule Compliance:** Rules 5, 8, 13, 17 (`docs/UML.md`)

---

# 1. Executive Overview & UX Philosophy

### 1.1 Core Purpose
The **2.5D Interactive Blueprint & 3D Spatial Linking Map** is the facility's live spatial intelligence terminal. It bridges abstract database entities with physical reality by rendering an interactive, 2.5D isometric architectural floorplan of the athletic training complex (`M3GymFloorMap`). Gym operators, head coaches, and facility technicians can observe live machine occupancy pins, track foot-traffic heat density, identify equipment bottlenecks, inspect maintenance alarms via pulsating beacons (`M3PulseBeacon`), and trigger 1-click 3D spatial asset inspections that link directly into the hardware-accelerated `M3ModelViewer3D`.

### 1.2 Target Personas
- **Facility Operations Manager**: Monitors zone congestion, re-allocates floor space based on machine utilization heatmaps, and ensures emergency exit compliance.
- **Gym Floor Lead / Front Desk Supervisor**: Directs incoming athletes toward less congested training zones and verifies machine availability in real time.
- **Maintenance Technician**: Instantly locates malfunctioning machines on the physical floor grid without wandering the facility searching for serial tags.

### 1.3 Information Architecture & Spatial Mental Model
The spatial mental model operates as an **Athletic Building Information Model (BIM)**:
1. **Architectural Grid & Spatial Zones**: The facility is segmented into 5 distinct training sectors:
   - **Cardio Deck** (Treadmills, Rowers, AirBikes, Stairmills)
   - **Free Weights Arena** (Power Cages, Dumbbell Racks, Adjustable Benches)
   - **Functional Turf & Sprint Track** (Sled tracks, Kettlebells, Plyo boxes, Battling ropes)
   - **Olympic Lifting Platforms** (Oly Racks, Bumper plate storage, Chalk stands)
   - **Recovery & Stretch Lounge** (Hypervolt massage guns, Foam rollers, Infrared pods)
2. **Dynamic Equipment Status Pins**: Interactive markers positioned on the grid, color-coded by real-time status:
   - *Emerald Green*: `AVAILABLE` and unoccupied.
   - *Amber Pulse*: `IN_MAINTENANCE` or service requested.
   - *Neon Ruby*: Occupied / Live active set in progress.
3. **1-Click 3D Inspection Flyout (`M3GlassPane`)**: Clicking any pin deploys a floating glassmorphic popover containing the procedural `M3ModelViewer3D` to inspect that specific machine's 3D digital twin without navigating away from the spatial overview.
4. **Heat Density & Utilization Overlay**: Toggleable heat diffusion gradient reflecting cumulative daily check-ins and session bookings.

```mermaid
graph TD
    A[Spatial Coordinate Grid] --> B(M3GymFloorMap 2.5D Viewport)
    C[EquipmentService Asset Stream] -->|Status & Condition| D[Interactive Machine Pins]
    E[AttendanceService Check-In Stream] -->|Zone Density| F[Spatial Heatmap Shader]
    D & F --> B
    B -->|User Clicks Machine Pin| G[1-Click Floating Inspection Card]
    G -->|Embeds In-Situ| H[M3ModelViewer3D 3D Digital Twin]
```

### 1.4 2026 M3 Expressive Fitness UX Philosophy
- **Tactile Isometric Pan-and-Zoom**: Smooth multi-touch canvas physics with momentum glide, pinch-to-zoom, and boundary bounce damping.
- **Cybernetic Neon Floor Grid**: Subtle luminous isometric gridlines with specular room boundaries, conveying high-tech facility control.
- **Layered Progressive Disclosure**: Machine pins display minimalist status dots at low zoom, expanding to show machine name, condition chip, and current user avatar as the operator zooms closer.

---

# 2. Visual Architecture & Wireframe ASCII Diagram

```
+---------------------------------------------------------------------------------------------------------------------------------------+
| TOP APP BAR: Spatial Facility Blueprint — [Floor 1: Main Training Arena]       [Occupancy: 64/120 (53%)]  [Active Alerts: 2] [Ctrl+K] |
+---------------------------------------------------------------------------------------------------------------------------------------+
| [ZOOM: ( - ) [ 100% ] ( + ) ] | LAYER TOGGLES: [x] Machine Pins  [x] Heatmap Density  [ ] Trainer Beacons  [ ] Electrical/Sensors   |
+-------------------------------------------------------------------------------------------------------+-------------------------------+
| M3GymFloorMap: 2.5D ISOMETRIC FACILITY BLUEPRINT (Center Canvas 960dp)                                | SPATIAL TELEMETRY (320dp)     |
+-------------------------------------------------------------------------------------------------------+-------------------------------+
|   /===============================================================================================\   | ╭- ZONE OCCUPANCY ------------╮|
|  /  [ZONE 1: CARDIO DECK]                      [ZONE 2: OLYMPIC LIFTING PLATFORMS]                 \  | | Cardio Deck:       78% High ||
| /   [●]TM-01 (Avail)   [●]AB-02 (Active)       [●]PLAT-01 (In Use)     [●]PLAT-02 (Avail)           \ | | Free Weights:      62% Med  ||
|/    [●]TM-02 (Avail)   [●]ROW-01 (Avail)       [●]PLAT-03 (Avail)      [●]PLAT-04 (Maint ⚠️)         \| | Functional Turf:   34% Low  ||
|\                                                                                                    /| | Olympic Platforms:  85% High ||
| \   +--------------------+    +--------------------+    +--------------------+                     / | | Recovery Lounge:    20% Low  ||
|  \  | SLED RUN TURF 25M  |    | FREE WEIGHTS ARENA |    | RECOVERY & MOBILITY|                    /  | ╰--------------------------╯|
|   \ |                    |    |                    |    |                    |                   /   | ╭- SELECTED ASSET POPUP ---╮|
|    \| [●]Kettlebells     |    | [●]Power Cage #1   |    | [●]Infrared Pod #1 |                  /    | | Olympic Power Cage #01   ||
|     \                    |    | [●]Dumbbell Rack   |    | [●]Stretching Mats |                 /     | | Status: AVAILABLE [Green]||
|      \======================================================================================/      | | Condition: GOOD          ||
|                                                                                                       | | Last Maint: 2026-10-15   ||
|       ╭- FLOATING 1-CLICK 3D INSPECTION CARD (Appears over clicked pin) ------------------------╮     | | ╭- IN-SITU 3D PREVIEW --╮|
|       | [X] Olympic Power Cage #01 | Status: AVAILABLE | Condition: GOOD                        |     | |   [M3ModelViewer3D]     ||
|       | --------------------------------------------------------------------------------------- |     | |    | |=======| |        ||
|       | [ 3D Digital Twin Viewport: Orbit / Specular Rim / Wireframe Toggle ]                   |     | |    | |  [J]  | |        ||
|       | [ 🛠️ Schedule Service ]     [ 🔄 View Full 3D Asset Page ]     [ 📍 Re-Position Pin ]  |     | |   /_____________\       ||
|       ╰-----------------------------------------------------------------------------------------╯     | ╰-------------------------╯|
|                                                                                                       | [ Open Full Asset Dossier ] ||
|                                                                                                       | [ Mark In-Maintenance ]     ||
|                                                                                                       ╰-----------------------------╯|
+-------------------------------------------------------------------------------------------------------+-------------------------------+
| BLUEPRINT TOOLBAR (Bottom Bar 72dp)                                                                                                   |
| [ 🔄 Reset View (Home) ]   [ 2.5D Isometric / 2D Top-Down Toggle ]    [ 🌡️ Peak Traffic Timeline ]    [ ➕ Place New Equipment Pin ] |
+---------------------------------------------------------------------------------------------------------------------------------------+
```

---

# 3. Complete M3 & Custom Component Roster

| Component Class | Variant / Token Role | Functional Purpose on Screen | Technical Configuration & Properties |
|---|---|---|---|
| `M3GymFloorMap` | Hardware 2.5D Canvas | Isometric interactive blueprint render engine | Pan/zoom gesture listener, isometric coordinate projection |
| `M3ModelViewer3D`| In-Situ 3D Digital Twin | Embedded inside floating inspection card | Compact 3D subscene ($280\times 220\text{dp}$), auto-rotating orbit |
| `M3PulseBeacon` | Kinetic Status Pulsar | Radiates pulsating rings over broken/maintenance pins | $1.2\,\text{Hz}$ expanding wave, color `-md-sys-color-error` / Amber |
| `M3GlassPane` | Cyber Acrylic Scrim | Floating 1-click 3D equipment popover container | 24px gaussian blur, 1px specular highlight rim, rounded 20dp |
| `M3Heatmap` | Spatial Density Shader | GPU fragment shader mapping foot-traffic intensity | Multi-color gradient (Transparent $\to$ Cyan $\to$ Yellow $\to$ Crimson)|
| `M3TopAppBar` | Prominent Elevated | Floor selection, occupancy counters, search | Surface elevation 2, real-time member occupancy chip |
| `M3Button` | Filled Primary | "Place New Equipment Pin" CTA in bottom bar | `-md-sys-color-primary`, icon="pin_drop", $48\,\text{dp}$ touch target |
| `M3Button` | Outlined Secondary | "Reset View", "Mark In-Maintenance" | Border `-md-sys-color-outline`, hover elevation delta +1 |
| `M3IconButton` | Tonal Surface | Zoom in (`+`), Zoom out (`-`), 2D/2.5D Toggle | $48\times 48\text{dp}$ touch hitbox, tooltip enabled |
| `M3Chip` | Filter / Layer Toggle | Toggles layers: `Pins`, `Heatmap`, `Sensors` | Multi-select chip strip, animated checkmark pop |
| `M3Card` | Bento Telemetry Tile | Right-side zone occupancy distribution breakdown | Container low, progress bars for each zone |
| `M3LinearProgress`| Kinetic Rounded | Zone occupancy progress bar (e.g. Cardio 78%) | Dynamic color morphs: Green ($<60\%$), Amber, Red ($>85\%$) |
| `M3Snackbar` | Urgent Alert | Floating alerts when machine maintenance is triggered | Action CTA: "View Location" |

---

# 4. Dynamic Theme & M3 Color Token Mapping

### 4.1 Theme Recommendations
- **Primary Spatial Theme**: Cyber Matrix Green (`#386A20`) / Cyber Violet (`#6750A4`)
- **Cardio Zone Accent**: Neon Teal (`#006A6A`)
- **Heavy Iron & Strength Zone**: Apex Coral (`#B3261E`)
- **Turf & Functional Zone**: Solar Amber (`#825500`)

### 4.2 Exact Looked-Up CSS Color Tokens
```css
/* Gym Floor Map Master Stylesheet (gym-floor-map.css) */
.floor-map-root {
    -fx-background-color: -md-sys-color-surface;
    -fx-text-fill: -md-sys-color-on-surface;
}

/* 2.5D Blueprint Viewport */
.blueprint-canvas-container {
    -fx-background-color: #0A0D14; /* Deep architectural blueprint dark */
    -fx-border-color: -md-sys-color-outline-variant;
    -fx-border-width: 1px;
}

/* Floor Gridlines */
.isometric-grid-line {
    -fx-stroke: rgba(255, 255, 255, 0.08);
    -fx-stroke-width: 1px;
}

.isometric-zone-boundary {
    -fx-stroke: -md-sys-color-primary;
    -fx-stroke-width: 2px;
    -fx-stroke-dash-array: 6 4;
}

/* Dynamic Pin Markers */
.pin-available {
    -fx-fill: #00E676; /* Vibrant Emerald */
    -fx-effect: dropshadow(gaussian, #00E676, 8, 0.5, 0, 0);
}

.pin-maintenance {
    -fx-fill: #FF9100; /* Amber Warning */
    -fx-effect: dropshadow(gaussian, #FF9100, 12, 0.6, 0, 0);
}

.pin-occupied {
    -fx-fill: #FF1744; /* Neon Crimson */
    -fx-effect: dropshadow(gaussian, #FF1744, 8, 0.5, 0, 0);
}

/* Floating 3D Popover Glass Container */
.pin-popover-glass {
    -fx-background-color: rgba(20, 24, 38, 0.88);
    -fx-background-radius: 20px;
    -fx-border-color: rgba(255, 255, 255, 0.18);
    -fx-border-radius: 20px;
    -fx-border-width: 1px;
    -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.55), 24, 0.3, 0, 8);
}
```

### 4.3 Elevation & Container Hierarchy
- **Level 0 (`#0A0D14`)**: 2.5D floorplan canvas surface.
- **Level 1 (`surface-container-low`)**: Right telemetry sidebar and bottom toolbar.
- **Level 2 (`surface-container`)**: Zone occupancy cards, layer toggle chips.
- **Level 4 (`pin-popover-glass`)**: 1-Click 3D Inspection popover floating over the canvas.
- **Level 5**: Emergency maintenance snackbars and system modals.

---

# 5. Motion Physics, Animations & Shader Effects

### 5.1 2.5D Isometric Camera Projection Physics
Points in 3D floor space $(x, y, z)$ project into 2D screen coordinates $(u, v)$ via isometric transform:
$$u = (x - y) \cos(30^\circ) \cdot S + O_x$$
$$v = (x + y) \sin(30^\circ) \cdot S - z \cdot S + O_y$$
- **Momentum Pan & Zoom**: Mass $m = 1.0$, Friction damping $\gamma = 5.8\,\text{s}^{-1}$. Releasing a pan drag glides smoothly across the canvas.
- **Pinch-to-Zoom Snap**: Smooth exponential zoom $S(t)$ clamped between $0.4\times$ (full complex overview) and $2.8\times$ (single power cage closeup).

### 5.2 `M3PulseBeacon` Concentric Wave Shader
Pins with `IN_MAINTENANCE` status emit periodic circular shockwaves:
- Radius $R(t) = R_0 + v_{\text{wave}} \cdot t \pmod{R_{\max}}$.
- Opacity decays linearly with radius: $\alpha(t) = 1.0 - \frac{R(t)}{R_{\max}}$.
- Creates a continuous sonar ping effect drawing immediate operator attention.

### 5.3 1-Click Popover Bloom Transition
When an equipment pin is clicked:
- Popover scales outward from pin origin: $0.2 \to 1.0$ ($m=0.8$, $k=340$, $\zeta=0.68$).
- Embedded `M3ModelViewer3D` begins an automatic slow $15^\circ/\text{s}$ preview orbit.

---

# 6. Interactive States, Micro-interactions & Error Handling

### 6.1 Interactive State Matrix

| Element | Default / Normal | Hovered | Pressed / Clicked | Dragged | Disabled |
|---|---|---|---|---|---|
| **Equipment Map Pin** | 16dp circular dot, glow | Expands to 22dp, tooltip pops | Scale 0.90, launches popover | Repositions pin coordinate | Desaturated gray dot |
| **Layer Toggle Chip** | Tonal surface, 1px border | Elevation +1, hover tint | Scale 0.96, state layer | - | Dimmed 0.38 alpha |
| **Zone Perimeter Polygon**| Transparent, dashed line | Fills 10% primary tint | - | - | Hidden |
| **Close Popover [X]** | Transparent, 32dp | Crimson hover tint | Scale 0.92, spring pop | - | - |

### 6.2 Pin Repositioning Micro-Interaction
Facility managers can drag pins to update machine locations when gym floor arrangements change:
1. Long-press machine pin for $500\,\text{ms}$ $\to$ Pin elevates in z-space with an expanding drop shadow.
2. Dragging snaps to the nearest architectural grid intersection ($0.5\,\text{m}$ snap tolerance).
3. Releasing commits new coordinates; `M3Snackbar` confirms: *"Olympic Power Cage moved to Free Weights Grid (X: 14.5, Y: 8.0)"*.

### 6.3 Offline Sensor & Map Desync Recovery
- If real-time occupancy sensor feeds stall:
  - Affected zone displays an amber warning badge: *"Sensor Sync Delayed (30s)"*.
  - Map retains last verified machine coordinates and falls back to manual database status.

---

# 7. Declarative FXML Node Hierarchy & Controller Contract

### 7.1 FXML Structural Hierarchy (`gym-floor-map.fxml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import io.m3fx.controls.floormap.*?>
<?import io.m3fx.controls.button.*?>
<?import io.m3fx.controls.chip.*?>
<?import io.m3fx.controls.card.*?>
<?import io.m3fx.controls.glass.*?>
<?import io.m3fx.controls.view3d.*?>
<?import io.m3fx.controls.progress.*?>

<BorderPane xmlns="http://javafx.com/javafx/21" 
            xmlns:fx="http://javafx.com/fxml/1" 
            fx:controller="com.gym.controller.GymFloorMapController"
            stylesheets="@../../css/m3-theme.css,@../../css/gym-floor-map.css"
            styleClass="floor-map-root"
            prefWidth="1366.0" prefHeight="840.0">

    <!-- Top App Bar & Layer Selection Header -->
    <top>
        <VBox styleClass="top-bar-box">
            <HBox alignment="CENTER_LEFT" spacing="16.0" styleClass="header-row">
                <Label text="Gym Floor Blueprint &amp; 3D Spatial Twin" styleClass="header-title-text"/>
                <M3Chip fx:id="occupancyChip" text="Occupancy: 64 / 120 (53%)" styleClass="occupancy-badge"/>
                <Region HBox.hgrow="ALWAYS"/>
                <M3IconButton text="-" onAction="#onZoomOut" styleClass="zoom-btn"/>
                <Label fx:id="zoomPercentLabel" text="100%" styleClass="zoom-label"/>
                <M3IconButton text="+" onAction="#onZoomIn" styleClass="zoom-btn"/>
                <Separator orientation="VERTICAL"/>
                <M3Button text="Reset View" onAction="#onResetView" styleClass="text-btn-compact"/>
            </HBox>

            <!-- Layer Toggle Strip -->
            <HBox spacing="8.0" alignment="CENTER_LEFT" styleClass="layer-toggle-strip">
                <Label text="Layers:" styleClass="layer-label"/>
                <M3Chip fx:id="layerPinsChip" text="Machine Pins" selected="true" onAction="#onTogglePinsLayer"/>
                <M3Chip fx:id="layerHeatmapChip" text="Occupancy Heatmap" selected="true" onAction="#onToggleHeatmapLayer"/>
                <M3Chip fx:id="layerZonesChip" text="Zone Boundaries" selected="true" onAction="#onToggleZonesLayer"/>
                <M3Chip fx:id="layerSensorsChip" text="IoT Sensors" selected="false" onAction="#onToggleSensorsLayer"/>
            </HBox>
        </VBox>
    </top>

    <!-- Center: Interactive 2.5D Blueprint Canvas Stack -->
    <center>
        <StackPane fx:id="canvasStackPane" styleClass="blueprint-canvas-container">
            
            <!-- Core 2.5D Blueprint Viewport Component -->
            <M3GymFloorMap fx:id="gymFloorMap" prefWidth="Infinity" prefHeight="Infinity"/>

            <!-- Floating 1-Click 3D Inspection Card (Initially Hidden) -->
            <M3GlassPane fx:id="inspectionGlassCard" visible="false" maxWidth="380.0" maxHeight="440.0" 
                         StackPane.alignment="CENTER" styleClass="pin-popover-glass">
                <VBox spacing="10.0" styleClass="popover-inner-box">
                    <HBox alignment="CENTER_LEFT">
                        <Label fx:id="inspectEquipmentNameLabel" text="Olympic Power Cage #01" styleClass="popover-title"/>
                        <Region HBox.hgrow="ALWAYS"/>
                        <M3IconButton text="✕" onAction="#onCloseInspection" styleClass="popover-close-btn"/>
                    </HBox>
                    <HBox spacing="8.0">
                        <M3Chip fx:id="inspectStatusChip" text="AVAILABLE" styleClass="chip-status-available"/>
                        <M3Chip fx:id="inspectConditionChip" text="CONDITION: GOOD" styleClass="chip-condition-good"/>
                    </HBox>
                    
                    <!-- Embedded Compact 3D Digital Twin Viewport -->
                    <M3ModelViewer3D fx:id="inspectModelViewer3D" prefWidth="340.0" prefHeight="220.0" 
                                     styleClass="popover-3d-viewport"/>
                    
                    <Label fx:id="inspectLastMaintLabel" text="Last Maintenance: 2026-10-15" styleClass="popover-subtext"/>
                    
                    <HBox spacing="10.0" alignment="CENTER">
                        <M3Button text="Mark Maintenance" onAction="#onScheduleServiceFromMap" styleClass="tonal-btn-compact"/>
                        <M3Button text="Full Asset Page" onAction="#onOpenFullAssetDetails" styleClass="filled-btn-compact"/>
                    </HBox>
                </VBox>
            </M3GlassPane>

        </StackPane>
    </center>

    <!-- Right: Zone Occupancy & Environmental Telemetry -->
    <right>
        <VBox spacing="16.0" prefWidth="300.0" styleClass="spatial-telemetry-rail">
            <Label text="Zone Congestion Telemetry" styleClass="telemetry-section-title"/>
            
            <M3Card styleClass="zone-stat-card">
                <VBox spacing="8.0" styleClass="zone-stat-box">
                    <HBox alignment="CENTER_LEFT">
                        <Label text="Cardio Deck" styleClass="zone-name"/>
                        <Region HBox.hgrow="ALWAYS"/>
                        <Label text="78% (High)" styleClass="zone-val-warn"/>
                    </HBox>
                    <M3LinearProgress progress="0.78" styleClass="progress-warn"/>
                </VBox>
            </M3Card>

            <M3Card styleClass="zone-stat-card">
                <VBox spacing="8.0" styleClass="zone-stat-box">
                    <HBox alignment="CENTER_LEFT">
                        <Label text="Free Weights Arena" styleClass="zone-name"/>
                        <Region HBox.hgrow="ALWAYS"/>
                        <Label text="62% (Med)" styleClass="zone-val-normal"/>
                    </HBox>
                    <M3LinearProgress progress="0.62" styleClass="progress-normal"/>
                </VBox>
            </M3Card>

            <M3Card styleClass="zone-stat-card">
                <VBox spacing="8.0" styleClass="zone-stat-box">
                    <HBox alignment="CENTER_LEFT">
                        <Label text="Olympic Platforms" styleClass="zone-name"/>
                        <Region HBox.hgrow="ALWAYS"/>
                        <Label text="85% (Critical)" styleClass="zone-val-high"/>
                    </HBox>
                    <M3LinearProgress progress="0.85" styleClass="progress-high"/>
                </VBox>
            </M3Card>

            <M3Card styleClass="zone-stat-card">
                <VBox spacing="8.0" styleClass="zone-stat-box">
                    <HBox alignment="CENTER_LEFT">
                        <Label text="Functional Turf" styleClass="zone-name"/>
                        <Region HBox.hgrow="ALWAYS"/>
                        <Label text="34% (Low)" styleClass="zone-val-low"/>
                    </HBox>
                    <M3LinearProgress progress="0.34" styleClass="progress-low"/>
                </VBox>
            </M3Card>

            <Region VBox.hgrow="ALWAYS"/>
            
            <VBox spacing="8.0" styleClass="quick-legend-box">
                <Label text="Map Legend" styleClass="legend-header"/>
                <HBox spacing="8.0" alignment="CENTER_LEFT">
                    <Circle radius="6.0" fill="#00E676"/>
                    <Label text="Available / Operational" styleClass="legend-text"/>
                </HBox>
                <HBox spacing="8.0" alignment="CENTER_LEFT">
                    <Circle radius="6.0" fill="#FF1744"/>
                    <Label text="Occupied / Live Set" styleClass="legend-text"/>
                </HBox>
                <HBox spacing="8.0" alignment="CENTER_LEFT">
                    <Circle radius="6.0" fill="#FF9100"/>
                    <Label text="In Maintenance / Service Req" styleClass="legend-text"/>
                </HBox>
            </VBox>
        </VBox>
    </right>

    <!-- Bottom: Blueprint Toolbar & Pin Placement Controls -->
    <bottom>
        <HBox alignment="CENTER_RIGHT" spacing="16.0" styleClass="blueprint-bottom-dock">
            <M3Button text="2.5D / 2D Perspective Toggle" onAction="#onTogglePerspective" styleClass="outlined-btn"/>
            <Region HBox.hgrow="ALWAYS"/>
            <M3Button text="Export Blueprint Layout (DXF/PDF)" onAction="#onExportBlueprint" styleClass="text-btn"/>
            <M3Button text="+ Place Equipment Pin" onAction="#onPlacePin" styleClass="filled-primary-btn"/>
        </HBox>
    </bottom>

</BorderPane>
```

### 7.2 Controller Contract & JavaFX Injection Bindings

```java
package com.gym.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import io.m3fx.controls.floormap.M3GymFloorMap;
import io.m3fx.controls.glass.M3GlassPane;
import io.m3fx.controls.view3d.M3ModelViewer3D;
import io.m3fx.controls.chip.M3Chip;
import com.gym.model.Equipment;
import com.gym.model.EquipmentStatus;
import com.gym.service.EquipmentService;
import com.gym.service.AttendanceService;

/**
 * Controller for 2.5D Interactive Blueprint & 3D Spatial Linking.
 * Backed by gym-floor-map.fxml.
 */
public class GymFloorMapController {

    @FXML private StackPane canvasStackPane;
    @FXML private M3GymFloorMap gymFloorMap;
    @FXML private Label occupancyChip;
    @FXML private Label zoomPercentLabel;
    
    @FXML private M3Chip layerPinsChip;
    @FXML private M3Chip layerHeatmapChip;
    @FXML private M3Chip layerZonesChip;
    @FXML private M3Chip layerSensorsChip;

    @FXML private M3GlassPane inspectionGlassCard;
    @FXML private Label inspectEquipmentNameLabel;
    @FXML private M3Chip inspectStatusChip;
    @FXML private M3Chip inspectConditionChip;
    @FXML private M3ModelViewer3D inspectModelViewer3D;
    @FXML private Label inspectLastMaintLabel;

    private EquipmentService equipmentService;
    private AttendanceService attendanceService;
    private Equipment activeInspectedEquipment;

    @FXML
    public void initialize() {
        // 1. Initialize 2.5D Isometric projection engine and grid geometry
        // 2. Fetch active equipment inventory and plot coordinate pins
        // 3. Connect pin-click event listener to deploy inspectionGlassCard
        // 4. Bind real-time attendance streams to generate heat density overlay
    }

    @FXML
    public void onZoomIn() {
        gymFloorMap.zoomIn();
        updateZoomLabel();
    }

    @FXML
    public void onZoomOut() {
        gymFloorMap.zoomOut();
        updateZoomLabel();
    }

    @FXML
    public void onResetView() {
        gymFloorMap.resetCameraHome();
        updateZoomLabel();
    }

    @FXML
    public void onTogglePinsLayer() {
        gymFloorMap.setPinsVisible(layerPinsChip.isSelected());
    }

    @FXML
    public void onToggleHeatmapLayer() {
        gymFloorMap.setHeatmapVisible(layerHeatmapChip.isSelected());
    }

    @FXML
    public void onToggleZonesLayer() {
        gymFloorMap.setZonesVisible(layerZonesChip.isSelected());
    }

    @FXML
    public void onToggleSensorsLayer() {
        gymFloorMap.setSensorsVisible(layerSensorsChip.isSelected());
    }

    @FXML
    public void onCloseInspection() {
        inspectionGlassCard.setVisible(false);
    }

    @FXML
    public void onScheduleServiceFromMap() {
        if (activeInspectedEquipment != null) {
            equipmentService.changeStatus(activeInspectedEquipment.getId(), EquipmentStatus.IN_MAINTENANCE);
            inspectionGlassCard.setVisible(false);
            gymFloorMap.refreshPinStatus(activeInspectedEquipment.getId());
        }
    }

    @FXML
    public void onOpenFullAssetDetails() {
        // Navigates to Equipment Inventory (EQUIPMENT_INVENTORY_UI.md) focusing on active item
    }

    @FXML
    public void onPlacePin() {
        // Enters interactive drag-and-drop pin placement mode
    }

    @FXML
    public void onTogglePerspective() {
        gymFloorMap.togglePerspective(); // Switches between 2.5D Isometric and 2D Plan View
    }

    @FXML
    public void onExportBlueprint() {
        // Generates architectural blueprint PDF / DXF export
    }

    private void updateZoomLabel() {
        zoomPercentLabel.setText((int)(gymFloorMap.getZoomLevel() * 100) + "%");
    }
}
```

---

# 8. Ergonomics, Touch Targets & Responsive Layout

### 8.1 Sizing & Touch Ergonomics
- **Map Pin Target Hitbox**: While the visual dot is $16\,\text{dp}$, its touch/mouse interaction hitbox expands to **$48\times 48\,\text{dp}$**, preventing mis-clicks during fast floor navigation.
- **Floating Popover Close Trigger**: $44\times 44\,\text{dp}$ touch button placed at the top-right corner with high contrast.
- **Bottom Dock Spacing**: All mode toggles and placement CTAs maintain minimum $12\,\text{dp}$ horizontal breathing buffers.

### 8.2 Responsive Adaptability
- **Expanded (1366dp - 1920dp Wall Display)**: Blueprint canvas and telemetry rail displayed side-by-side; full 2.5D isometric rendering with shadow passes.
- **Medium (840dp - 1365dp Tablets)**: Telemetry rail collapses into a right slide-out `M3SideSheet`; canvas occupies full screen.
- **Compact (< 840dp Mobile Screens)**: Map switches to pure 2D Orthographic Top-Down mode for reduced rendering complexity and clean touch navigation.

---

# 9. Accessibility, Keyboard Navigation & Screen Readers

### 9.1 Keyboard Traversal Order
1. Zoom & View Controls (`Zoom In`, `Zoom Out`, `Reset Home`)
2. Layer Toggle Chips (`Pins`, `Heatmap`, `Zones`)
3. Canvas Pin Traversal (`Tab` loops through all visible equipment pins sequentially)
4. Inspection Popover Controls (when open: `Close`, `Schedule Service`, `Full Asset Page`)
5. Bottom Toolbar Action Items

### 9.2 Accelerators & Shortcuts
- `+` / `-`: Zoom in and Zoom out.
- `H`: Reset View to Home coordinates.
- `P`: Toggle Machine Pins layer.
- `M`: Toggle Occupancy Heatmap layer.
- `Space`: Open inspection card for focused pin.
- `ESC`: Dismiss inspection card or exit pin placement mode.

### 9.3 Screen Reader & ARIA Equivalence
- `gymFloorMap`: `accessibleRole="IMAGE_VIEW"`, `accessibleText="Interactive 2.5D Gym Floor Blueprint. 42 machines online, 2 maintenance alerts in Olympic Lifting Zone."`
- Equipment Pin: `accessibleRole="BUTTON"`, `accessibleText="Olympic Power Cage 01, Status: Available, Location: Free Weights Grid 14-8."`

---

# 10. Quality Review Checklist & Pitfalls

### 10.1 Quality Review Checklist
- [x] Conforms to architectural blueprint standards with all 5 training zones mapped.
- [x] Includes 1-click in-situ 3D inspection embedding `M3ModelViewer3D`.
- [x] Dynamic theme color tokens strictly utilized (`-md-sys-color-*`).
- [x] Pin hitboxes reach minimum $48\times 48\,\text{dp}$ touch target standard.
- [x] Smooth pan/zoom momentum physics with bounded clamping.

### 10.2 Top 5 Developer Implementation Pitfalls
1. **Rendering Isometric Map on CPU Graphics**: Drawing thousands of gridlines and heatmaps on a standard `Canvas` CPU graphics context causes frame drops below 15 FPS. Use a JavaFX `SubScene` with hardware mesh acceleration or an OpenGL fragment shader.
2. **Missing Boundary Clamping on Pan/Zoom**: Allowing operators to drag the blueprint canvas infinitely off-screen into empty void space creates user confusion. Always clamp camera translations to the physical building perimeter.
3. **Z-Ordering Glitches with 2.5D Isometric Pins**: In isometric projection, pins located further "down" the screen must render in front of objects higher up. Failing to sort nodes by $(x + y)$ causes visual overlap errors.
4. **Ignoring Stale Heatmap Caches**: Recalculating foot traffic density on every mouse drag stalls the UI. Update heat density data asynchronously every 15–30 seconds and upload as a static texture map.
5. **Memory Leaks in Embedded 3D Popover**: Opening and closing the 1-click 3D inspection popover repeatedly without disposing of the `M3ModelViewer3D` SubScene leaks native mesh memory. Always call `modelViewer3D.dispose()` or reuse a single cached instance.
