# Material Design 3 Expressive UI/UX Specification: Fast-Lane Attendance & Turnstile Scanner
## Screen ID: `ACT-16-ATTENDANCE` | Component Pairing: `AttendanceController.java` & `attendance.fxml`

---

# 1. Executive Overview & UX Philosophy

### 1.1 Core Purpose
The **Fast-Lane Attendance & Turnstile Scanner** screen is the critical throughput gateway of the gym facility. It provides sub-second member identity verification, turnstile barcode/RFID scanning, real-time live occupancy monitoring against fire safety limits, peak-hour density heatmapping, and attendance record correction workflows.

### 1.2 Target Personas
- **Front-Desk Receptionist / Turnstile Greeter**: Oversee the fast-lane check-in stream, resolve blocked turnstile alerts (expired memberships, frozen accounts), manually process member arrivals/departures, and correct erroneous timestamps.
- **Gym Floor Marshals / Safety Officers**: Monitor real-time inside headcount and zoned capacity distribution.
- **Operations Manager**: Analyze hourly check-in velocity and 24-hour attendance heatmaps to optimize staffing and HVAC loads.
- **Member / Athlete (Self-Service Kiosk Mode)**: Present digital RFID badges or QR codes to a high-contrast touch terminal for instantaneous door release.

### 1.3 Mental Models & Fast-Lane Ergonomics
1. **Zero-Latency Recognition**: Front-desk operations require instant visual validation in $< 150\text{ms}$. Successful scans produce an explosive green acrylic flash; failed scans generate a high-contrast coral alert with clear remedy instructions.
2. **Headcount Command HUD**: The top anchor provides a live telemetry gauge of members currently inside the facility ($N_{\text{inside}}$), peak velocity, and turnover rate.
3. **Dual-Stream Workspace**:
   - *Left Half (The Turnstile Pipeline)*: Active laser scanner HUD (`M3Scanline`), rapid manual search-to-check-in keypad, and instantaneous VIP member badge card.
   - *Right Half (Operational Intelligence)*: Live stream `M3DataTable` of today's check-ins with inline checkout buttons, and an interactive 7-day $\times$ 24-hour `M3Heatmap` illustrating peak attendance density.

### 1.4 Business Rule Enforcement
- **Rule 5**: A valid `checkIn` strictly requires `CustomerStatus.ACTIVE` and a linked `Membership` with `MembershipStatus.ACTIVE` and `endDate >= today`. If an athlete scans twice on the same day, the system retrieves their existing record without creating duplicate entries.
- **Rule 19**: Attendance records are indelible audit logs and are **never deleted**. Clock errors (e.g. forgot to check out) are modified strictly via `AttendanceService.correctRecord(attendanceId, checkIn, checkOut)`.

---

# 2. Visual Architecture & Wireframe ASCII Diagram

```
+---------------------------------------------------------------------------------------------------------------------------------------+
|  [M3TopAppBar: attendanceTopAppBar]                                                                                                   |
|  [=] GYM OS  ::  FAST-LANE ATTENDANCE & TURNSTILE GATEWAY    [Quick Member Scan: #ID / Phone / QR...]  [Date: 2026-10-28]  [Reports v]  |
+-------------------------------------------------------------------+-------------------------------------------------------------------+
|  FACILITY HEADCOUNT COMMAND HUD                                   |  PEAK-HOURS ATTENDANCE DENSITY MATRIX                             |
|  +-------------------------------------------------------------+  |  +-------------------------------------------------------------+  |
|  | [M3StatCard: Live Inside]   | [M3StatCard: Today Total]     |  |  | [M3Heatmap: 7-Day x 24-Hour Peak Density Matrix]            |  |
|  | 142 / 200 Max Capacity      | 684 Check-Ins Logged          |  |  | Mon [  . . : : : : : : : : X X X X X X X X : : . . ]        |  |
|  | [M3GaugeChart: 71% Full]    | Peak: 06:00 - 08:30 PM        |  |  | Tue [  . . : : : : : : : : X X X X X X X X : : . . ]        |  |
|  +-------------------------------------------------------------+  |  | Wed [  . . : : : : : : : : X X X X X X [NOW] : . . ]        |  |
|                                                                   |  +-------------------------------------------------------------+  |
+-------------------------------------------------------------------+-------------------------------------------------------------------+
|  [FAST-LANE TURNSTILE SCANNER: scanViewportPane]                  |  [TODAY'S ATTENDANCE STREAM: attendanceDataTable]                 |
|  +-------------------------------------------------------------+  |  +-------------------------------------------------------------+  |
|  | [M3GlassPane: Laser Scan Viewport]                           |  |  | [Search Logs...] [Filter: (x) Inside Only  ( ) Checked Out] |  |
|  |  /=======================================================\  |  |  +-----+-------------------+---------+----------+-------------+  |
|  |  |      [M3Scanline: Animated Laser Sweep Bar]           |  |  |  | ID  | Member Name       | CheckIn | CheckOut | Actions     |  |
|  |  |  . . . . . . . . . . . . . . . . . . . . . . . . . .  |  |  |  +-----+-------------------+---------+----------+-------------+  |
|  |  |   [QR / NFC Sensor Ready - Present Card or App]       |  |  |  | #88 | Elena Fisher      | 06:15   | 07:45    | [Correct]   |  |
|  |  |                                                       |  |  |  | #89 | Marcus Fenix      | 06:30   | --:--    | [Check-Out] |  |
|  |  \=======================================================/  |  |  |  | #90 | Sarah Connor      | 07:02   | --:--    | [Check-Out] |  |
|  +-------------------------------------------------------------+  |  |  | #91 | John Matrix       | 07:15   | 08:30    | [Correct]   |  |
|  | LAST SCANNED MEMBER VERIFICATION DOSSIER                     |  |  |  | #92 | Dave Bautista     | 08:00   | --:--    | [Check-Out] |  |
|  | +---------------------------------------------------------+ |  |  +-----+-------------------+---------+----------+-------------+  |
|  | | [PASS VERIFIED: -md-sys-color-primary-container]        | |  |  | [Export Daily CSV]   [Show Daily Report: showDailyReport]   |  |
|  | | Athlete: SARAH CONNOR (ID #1042)                        | |  |  +-------------------------------------------------------------+  |
|  | | Status: ACTIVE  | Plan: YEARLY VIP (Expires: 2027-04-12)| |  |                                                                   |
|  | | Assigned Coach: Marcus Vance                            | |  |  [RECORD CORRECTION DIALOG / SHEET: onCorrectRecord]              |
|  | | Turnstile: GATE 01 RELEASED  [Access Granted 07:02 AM]  | |  |  +-------------------------------------------------------------+  |
|  | +---------------------------------------------------------+ |  |  | Attendance ID: #89 | Member: Marcus Fenix                  |  |
|  | MANUAL FAST CHECK-IN / CHECK-OUT KEYPAD                     |  |  | Check-In: [06:30] | Check-Out: [08:15]                       |  |
|  | [ Member Phone / ID Field: txtMemberLookup ]                |  |  | Reason: Client forgot badge checkout at kiosk               |  |
|  | [M3Button: onCheckIn]  [M3Button: onCheckOut]               |  |  | [ Cancel ]                         [ Save Correction ]      |  |
|  +-------------------------------------------------------------+  |  +-------------------------------------------------------------+  |
+---------------------------------------------------------------------------------------------------------------------------------------+
|  STATUS BAR: [M3PulseBeacon: Gate Active] Hardware Interface: USB-HID RFID Reader Online | Turnstile Relay: OK | Security Audit Level 1|
+---------------------------------------------------------------------------------------------------------------------------------------+
```

---

# 3. Complete M3 & Custom Component Roster

| Component Class | Variant / Style Role | FXML `fx:id` | Functional Responsibility & Specs |
|---|---|---|---|
| `M3TopAppBar` | `CENTER_ALIGNED` | `attendanceTopAppBar` | Top navigation bar hosting screen title, manual quick-search field, date display, and daily report generator menu. |
| `M3TextField` | `OUTLINED_HERO` | `txtMemberLookup` | High-visibility input field for manual check-ins. Accepts phone number, member ID, or national ID with instant predictive drop-down. |
| `M3Button` | `FILLED` | `btnCheckIn` | Invokes `onCheckIn(customerId)`. Validates Rule 5 membership criteria and issues gate release signal. |
| `M3Button` | `TONAL` | `btnCheckOut` | Invokes `onCheckOut(customerId)`. Sets departure timestamp on current active daily session. |
| `M3Button` | `OUTLINED` | `btnDailyReport` | Invokes `showDailyReport(date)`. Generates consolidated summary of visits, peak hours, and average dwell time. |
| `M3DataTable` | `HIGH_DENSITY` | `attendanceDataTable` | Real-time virtualized table of all check-ins for the active date. Columns: ID, Member Name, Plan, Check-In, Check-Out, Duration, Action. |
| `M3Card` | `ELEVATED` | `memberDossierCard` | Live-updating VIP card displaying scanned member photo, medical alert badges, plan validity countdown, and turnstile status. |
| `M3Heatmap` | `SPATIAL_DENSITY` | `peakHoursHeatmap` | 7-day $\times$ 24-hour heat grid displaying facility traffic density. Color scale transitions from cool slate to bright cyan to fiery coral. |
| `M3GaugeChart` | `CIRCULAR_DIAL` | `occupancyGaugeChart` | Renders current gym floor occupancy percentage against maximum fire safety capacity ($N / N_{\max}$). |
| `M3Scanline` | `CYBER_SCAN` | `turnstileScanline` | Hardware sensor simulation widget. Sweeps an illuminated horizontal laser bar across the optical viewport at $1.5\text{Hz}$. |
| `M3PulseBeacon` | `NEON_BEACON` | `turnstileHardwareBeacon` | Dynamic hardware status light: Steady Green (Ready), Flashing Cyan (Scan In Progress), Crimson Pulse (Access Denied). |
| `M3Dialog` | `ALERT_SURFACE` | `correctionDialog` | Modal dialog for `onCorrectRecord(attendanceId)`. Prompts staff for adjusted check-in/out times and audit reasons. |
| `M3Chip` | `FILTER` | `chipInsideOnly` | Filters the table stream to show only athletes currently inside the building (`checkOut == null`). |
| `M3Snackbar` | `FLOATING` | `attendanceSnackbar` | Floating toast notifying the receptionist of gate unlocks, duplicate check-in warnings, or correction confirmations. |

---

# 4. Dynamic Theme & M3 Color Token Mapping

### 4.1 Theme Palette Seeds
- **Primary Fast-Lane Seed**: **Neon Teal** (`#006A6A`) — Conveys athletic energy, sanitation, and rapid operational throughput.
- **Access Granted Accent**: **Matrix Green** (`#386A20`) — Turnstile clearance, active membership validation.
- **Access Denied Accent**: **Apex Coral** (`#B3261E`) — Expired membership, suspended athlete, or payment overdue.

### 4.2 Looked-Up Color Token Mappings
```css
/* Fast-Lane Turnstile Tokens */
.attendance-root {
    -fx-background-color: -md-sys-color-surface;
    -fx-text-fill: -md-sys-color-on-surface;
}
.scan-viewport-pane {
    -fx-background-color: -md-sys-color-surface-container-lowest;
    -fx-border-color: -md-sys-color-outline;
    -fx-border-width: 2px;
    -fx-border-radius: 16px;
}
.dossier-pass-granted {
    -fx-background-color: -md-sys-color-primary-container;
    -fx-text-fill: -md-sys-color-on-primary-container;
    -fx-border-color: -md-sys-color-primary;
}
.dossier-pass-denied {
    -fx-background-color: -md-sys-color-error-container;
    -fx-text-fill: -md-sys-color-on-error-container;
    -fx-border-color: -md-sys-color-error;
}
.heatmap-cell-empty {
    -fx-background-color: -md-sys-color-surface-container-low;
}
.heatmap-cell-peak {
    -fx-background-color: -md-sys-color-tertiary;
}
```

### 4.3 Visual Depth & Elevation Hierarchy
- **Level 0**: Background surface canvas.
- **Level 1**: Attendance data table rows and heatmap matrix grid.
- **Level 2**: Telemetry stat cards and turnstile scanner container.
- **Level 3**: Member verification dossier popup (elevated for instantaneous prominence).
- **Level 4**: Real-time floating check-in notification snackbars.
- **Level 5**: Record correction modal dialog (`onCorrectRecord`).

---

# 5. Motion Physics, Animations & Shader Effects

### 5.1 Laser Scanline Physics & Viewport Sweeps
The `M3Scanline` component utilizes a linear sweep interpolator modulated by a sine-wave intensity falloff:
$$y(t) = H \cdot \frac{1 + \sin(\omega t)}{2}, \quad \text{Intensity}(y) = I_0 \cdot \exp\left(-\frac{(y - y_0)^2}{2\sigma^2}\right)$$
When a barcode or RFID is captured, the scanline collapses instantaneously into a horizontal laser flash, followed by a spring expansion of the member dossier card ($k = 380, \zeta = 0.65$).

### 5.2 Access Feedback Morphing
- **Access Granted Animation**: The member dossier card undergoes a rapid $1.08\times$ scale pop with an emerald green `M3GlowShadow` bloom ($0.3\text{s}$ duration), followed by a smooth settling to $1.0\times$.
- **Access Denied Shake Interaction**: On validation failure (e.g. `MembershipStatus.EXPIRED`), the card executes an expressive horizontal shake animation:
  $$x_{\text{offset}}(t) = A \cdot e^{-\lambda t} \sin(\omega t), \quad A = 12\text{px}, \quad \omega = 30\text{ rad/s}$$
  Coupled with a coral crimson rim pulse.

### 5.3 Acrylic Frosted Glassmorphism
The Turnstile Scanner Viewport leverages `M3GlassPane` with a $24\text{px}$ backdrop blur and $20\%$ tint of `-md-sys-color-surface-container-lowest`, simulating a high-end HUD overlay on top of camera/optical input feeds.

---

# 6. Interactive States, Micro-interactions & Error Handling

### 6.1 State Matrix: Turnstile & Action Controls
| Control State | Visual Presentation | Sound & Haptic Cue | Accessibility Announcement |
|---|---|---|---|
| **Turnstile Idle** | Soft pulsating cyan laser line, elevation 1 | Subtle ambient hum | "Turnstile ready for scan" |
| **Scan Reading** | Scanline concentrates into high-intensity bar | Short chirp tone | "Reading credentials..." |
| **Clearance Granted** | Full container shifts to emerald, gate unlocks | Ascending two-tone chime | "Access granted. Welcome Sarah." |
| **Clearance Blocked** | Full container shifts to crimson, gate locked | Low-frequency buzz | "Access denied. Membership expired." |
| **Table Hover** | Row background brightens by $+8\%$ | Pointer cursor | Announces row member name |

### 6.2 Rule-Driven Error Handling Workflows
1. **Rule 5 Violation (Expired Membership)**:
   - System evaluates `membership.getEndDate() < today`.
   - UI locks the gate and surfaces an instant action chip: `[ Renew Membership at POS ]` which deep-links to `PaymentController`.
2. **Rule 5 Violation (Suspended Member)**:
   - System detects `customer.getStatus() == CustomerStatus.SUSPENDED`.
   - Turnstile screen displays: *"Account Suspended: Visit Admin Desk for Identity Verification."*
3. **Duplicate Check-In Handling**:
   - Athlete scans badge while already recorded inside.
   - Per Rule 5, `AttendanceDao.findByCustomerAndDay` returns the existing record. UI emits an amber snackbar:
     *"Member already checked in today at 07:02 AM. Gate cycled."*

---

# 7. Declarative FXML Node Hierarchy & Controller Contract

### 7.1 Valid FXML Structural Layout (`attendance.fxml`)
```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import io.m3fx.controls.button.M3Button?>
<?import io.m3fx.controls.button.M3IconButton?>
<?import io.m3fx.controls.textfield.M3TextField?>
<?import io.m3fx.controls.card.M3Card?>
<?import io.m3fx.controls.card.M3StatCard?>
<?import io.m3fx.controls.table.M3DataTable?>
<?import io.m3fx.controls.chart.M3GaugeChart?>
<?import io.m3fx.controls.chart.M3Heatmap?>
<?import io.m3fx.controls.scanner.M3Scanline?>
<?import io.m3fx.controls.glass.M3GlassPane?>

<BorderPane xmlns="http://javafx.com/javafx/21" xmlns:fx="http://javafx.com/fxml/1"
            fx:controller="com.gym.controller.AttendanceController"
            styleClass="attendance-root" prefWidth="1280.0" prefHeight="840.0">

    <!-- Top App Bar & Turnstile Status -->
    <top>
        <VBox spacing="8.0" styleClass="top-bar-container">
            <HBox alignment="CENTER_LEFT" spacing="16.0">
                <padding><Insets top="12.0" right="24.0" bottom="12.0" left="24.0"/></padding>
                <Label text="FAST-LANE ATTENDANCE &amp; TURNSTILE GATEWAY" styleClass="title-large"/>
                <Spacer HBox.hgrow="ALWAYS"/>
                <Label fx:id="lblCurrentDate" text="2026-10-28" styleClass="title-medium"/>
                <M3Button fx:id="btnDailyReportAction" text="Daily Report" onAction="#onDailyReportClick" 
                          styleClass="m3-button-tonal"/>
            </HBox>
        </VBox>
    </top>

    <!-- Center Bento Command Grid -->
    <center>
        <GridPane hgap="16.0" vgap="16.0">
            <padding><Insets top="16.0" right="20.0" bottom="16.0" left="20.0"/></padding>
            <columnConstraints>
                <ColumnConstraints percentWidth="42.0"/>
                <ColumnConstraints percentWidth="58.0"/>
            </columnConstraints>
            <rowConstraints>
                <RowConstraints percentHeight="25.0"/>
                <RowConstraints percentHeight="75.0"/>
            </rowConstraints>

            <!-- Top Left: Occupancy Gauge HUD -->
            <HBox spacing="16.0" GridPane.columnIndex="0" GridPane.rowIndex="0">
                <M3StatCard fx:id="cardInsideCount" title="Currently Inside" value="142" subtitle="Safety Cap: 200" 
                            HBox.hgrow="ALWAYS"/>
                <M3GaugeChart fx:id="occupancyGauge" prefWidth="120.0" prefHeight="120.0"/>
            </HBox>

            <!-- Top Right: Peak Hours Density Heatmap -->
            <VBox spacing="8.0" GridPane.columnIndex="1" GridPane.rowIndex="0" styleClass="heatmap-container-card">
                <Label text="HOURLY ATTENDANCE DENSITY (PEAK HOURS)" styleClass="label-small"/>
                <M3Heatmap fx:id="peakHoursHeatmap" VBox.vgrow="ALWAYS"/>
            </VBox>

            <!-- Bottom Left: Fast-Lane Scanner & Member Dossier -->
            <VBox spacing="16.0" GridPane.columnIndex="0" GridPane.rowIndex="1">
                <!-- Laser Scanner Viewport -->
                <StackPane prefHeight="220.0" styleClass="scan-viewport-pane">
                    <M3GlassPane opacity="0.8"/>
                    <M3Scanline fx:id="turnstileScanline"/>
                    <VBox alignment="CENTER" spacing="8.0">
                        <Label text="PRESENT RFID BADGE / QR CODE" styleClass="label-large"/>
                        <Label text="Align barcode within illuminated sensor zone" styleClass="body-small"/>
                    </VBox>
                </StackPane>

                <!-- Last Scanned Member Dossier -->
                <M3Card fx:id="memberDossierCard" styleClass="dossier-pass-granted" VBox.vgrow="ALWAYS">
                    <padding><Insets top="16.0" right="16.0" bottom="16.0" left="16.0"/></padding>
                    <VBox spacing="10.0">
                        <HBox alignment="CENTER_LEFT">
                            <Label fx:id="lblScanResult" text="ACCESS GRANTED" styleClass="title-medium"/>
                            <Spacer HBox.hgrow="ALWAYS"/>
                            <Label fx:id="lblScanTimestamp" text="07:02 AM" styleClass="body-small"/>
                        </HBox>
                        <Label fx:id="lblScannedMemberName" text="Sarah Connor (ID #1042)" styleClass="headline-small"/>
                        <Label fx:id="lblScannedPlanInfo" text="Plan: Yearly VIP · Valid until 2027-04-12" styleClass="body-medium"/>
                        <Label fx:id="lblAssignedTrainer" text="Assigned Coach: Marcus Vance" styleClass="body-small"/>
                    </VBox>
                </M3Card>

                <!-- Manual Fast Check-In Row -->
                <HBox spacing="10.0">
                    <M3TextField fx:id="txtMemberLookup" label="Enter Member ID or Phone" HBox.hgrow="ALWAYS"/>
                    <M3Button fx:id="btnManualCheckIn" text="Check-In" onAction="#onManualCheckInClick" 
                              styleClass="m3-button-filled"/>
                    <M3Button fx:id="btnManualCheckOut" text="Check-Out" onAction="#onManualCheckOutClick" 
                              styleClass="m3-button-outlined"/>
                </HBox>
            </VBox>

            <!-- Bottom Right: Live Check-In Stream Table -->
            <VBox spacing="10.0" GridPane.columnIndex="1" GridPane.rowIndex="1" styleClass="table-container-card">
                <HBox alignment="CENTER_LEFT" spacing="12.0">
                    <Label text="TODAY'S ATTENDANCE STREAM" styleClass="title-medium"/>
                    <Spacer HBox.hgrow="ALWAYS"/>
                    <TextField fx:id="txtTableSearch" promptText="Filter attendees..." prefWidth="200.0"/>
                </HBox>
                <M3DataTable fx:id="attendanceDataTable" VBox.vgrow="ALWAYS"/>
            </VBox>
        </GridPane>
    </center>
</BorderPane>
```

### 7.2 Controller Contract Implementation (`AttendanceController.java`)
```java
package com.gym.controller;

import com.gym.model.Attendance;
import com.gym.service.AttendanceService;
import com.gym.util.DateUtils;
import com.gym.util.FxUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.card.M3StatCard;
import io.m3fx.controls.chart.M3GaugeChart;
import io.m3fx.controls.chart.M3Heatmap;
import io.m3fx.controls.table.M3DataTable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AttendanceController {

    private final AttendanceService attendanceService;
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private Label lblCurrentDate;
    @FXML private M3StatCard cardInsideCount;
    @FXML private M3GaugeChart occupancyGauge;
    @FXML private M3Heatmap peakHoursHeatmap;
    @FXML private M3Card memberDossierCard;
    @FXML private Label lblScanResult;
    @FXML private Label lblScanTimestamp;
    @FXML private Label lblScannedMemberName;
    @FXML private Label lblScannedPlanInfo;
    @FXML private Label lblAssignedTrainer;
    @FXML private TextField txtMemberLookup;
    @FXML private M3DataTable<Attendance> attendanceDataTable;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @FXML
    public void initialize() {
        String today = LocalDate.now().toString();
        lblCurrentDate.setText(today);
        refreshDailyStream(today);
    }

    @FXML
    public void onCheckIn(int customerId) {
        try {
            String today = LocalDate.now().toString();
            String now = LocalTime.now().format(timeFmt);
            Attendance record = attendanceService.checkIn(customerId, today, now);
            FxUtils.snackbar("Checked in member #" + customerId + " at " + record.getCheckIn());
            refreshDailyStream(today);
        } catch (Exception ex) {
            FxUtils.error("Check-in rejected: " + ex.getMessage());
        }
    }

    @FXML
    public void onCheckOut(int customerId) {
        try {
            String today = LocalDate.now().toString();
            String now = LocalTime.now().format(timeFmt);
            attendanceService.checkOut(customerId, today, now);
            FxUtils.snackbar("Member #" + customerId + " checked out at " + now);
            refreshDailyStream(today);
        } catch (Exception ex) {
            FxUtils.error("Check-out failed: " + ex.getMessage());
        }
    }

    @FXML
    public void showDailyReport(String date) {
        List<Attendance> report = attendanceService.dailyReport(date);
        FxUtils.info("Daily attendance summary for " + date + ": " + report.size() + " total visits recorded.");
    }

    @FXML
    public void onCorrectRecord(int attendanceId) {
        // Opens correction dialog to adjust checkIn/checkOut per Rule 19
        // Dialog captures corrected times and invokes attendanceService.correctRecord(attendanceId, in, out);
    }

    private void refreshDailyStream(String date) {
        List<Attendance> records = attendanceService.dailyReport(date);
        attendanceDataTable.getItems().setAll(records);
        long insideCount = records.stream().filter(a -> a.getCheckOut() == null || a.getCheckOut().isBlank()).count();
        cardInsideCount.setValue(String.valueOf(insideCount));
        occupancyGauge.setValue((double) insideCount / 200.0);
    }

    @FXML private void onManualCheckInClick() {
        try {
            int cid = Integer.parseInt(txtMemberLookup.getText().trim());
            onCheckIn(cid);
        } catch (NumberFormatException ex) {
            FxUtils.error("Invalid Customer ID.");
        }
    }

    @FXML private void onManualCheckOutClick() {
        try {
            int cid = Integer.parseInt(txtMemberLookup.getText().trim());
            onCheckOut(cid);
        } catch (NumberFormatException ex) {
            FxUtils.error("Invalid Customer ID.");
        }
    }

    @FXML private void onDailyReportClick() {
        showDailyReport(lblCurrentDate.getText());
    }
}
```

---

# 8. Ergonomics, Touch Targets & Responsive Layout

### 8.1 Touch Ergonomics & Kiosk Usability
- **Manual Check-In Action Buttons**: Full $52\text{dp}$ touch target height with large typography for easy gloved or wet-hand operation.
- **Table Row Action Buttons**: Inline `[Check-Out]` and `[Correct]` buttons maintain $48 \times 48\text{dp}$ clickable bounds with $8\text{dp}$ horizontal padding to eliminate mis-taps during high-throughput check-in rushes.

### 8.2 Responsive Layout Adaptations
- **Expanded (Multi-Monitor Front Desk $\ge 1200\text{dp}$)**:
  Full bento grid showing scanner viewport, live dossier, telemetry gauge, 7-day heatmap, and data table simultaneously.
- **Medium (Check-In Tablet $840\text{dp} - 1199\text{dp}$)**:
  Heatmap moves to a collapsible disclosure panel. Scanner viewport and live stream table share equal vertical space.
- **Compact (Self-Service Turnstile Kiosk $< 840\text{dp}$)**:
  Single-column kiosk interface. Center stage is dedicated exclusively to the `M3Scanline` viewport and large member approval dossier. Table stream is placed behind a "Recent Activity" tab.

---

# 9. Accessibility, Keyboard Navigation & Screen Readers

### 9.1 Keyboard Flow & Accelerators
- `Ctrl + I`: Focuses the `txtMemberLookup` field and selects all text for immediate barcode gun scanning.
- `Enter` inside `txtMemberLookup`: Dispatches automatic check-in.
- `Shift + Enter`: Dispatches automatic check-out.
- `F5`: Refreshes live stream and recalculates active occupancy count.

### 9.2 Screen Reader Announcements
- Turnstile gate triggers use `AccessibleRole.TEXT` live region notifications:
  - On pass: `"Turnstile Gate 1 Released. Welcome, {Name}."`
  - On fail: `"Turnstile Gate Locked. Access Denied: {Reason}."`

---

# 10. Quality Review Checklist & Pitfalls

### 10.1 Verification Checklist
1. [x] Controller signatures match `docs/UML.md` exactly (`AttendanceController`).
2. [x] Rule 5 (Active status + active membership + `endDate >= today`) strictly enforced by service call.
3. [x] Rule 5 (Duplicate daily check-in returns existing row without throwing SQL exceptions).
4. [x] Rule 19 (Attendance records are never hard-deleted; modified only via `correctRecord`).
5. [x] Date and time handling conform strictly to ISO `yyyy-MM-dd` and `HH:mm`.
6. [x] Zero hardcoded color literals; all styling consumes `-md-sys-color-*`.

### 10.2 Top 5 Developer Mistakes & How to Prevent Them
1. **Implementing Delete on Attendance**: Providing a "Delete Record" button. *Fix: Rule 19 forbids deleting attendance. Only provide `onCorrectRecord(attendanceId)`.*
2. **Inserting New Rows on Same-Day Scans**: Re-inserting a second check-in when an athlete steps outside and returns. *Fix: `AttendanceService.checkIn` relies on `AttendanceDao.findByCustomerAndDay` to reuse the existing daily record.*
3. **Checking In Inactive or Expired Members**: Bypassing membership validity checks in the UI layer. *Fix: Always catch the service exception and surface clear renewal options.*
4. **Hardcoding Facility Safety Limits**: Hardcoding `200` as max capacity throughout the controller. *Fix: Extract facility limit into configuration properties.*
5. **Blocking the FX Thread during Scanner Ingestion**: Running serial port / USB HID listening loops directly on the JavaFX Application Thread. *Fix: Ingest hardware events on a background worker thread and dispatch UI updates via `Platform.runLater`.*
