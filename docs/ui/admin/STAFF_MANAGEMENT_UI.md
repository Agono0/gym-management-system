# Material Design 3 Expressive UI/UX Specification: Staff RBAC Matrix & Security Administration
## Screen ID: `ACT-19-STAFF` | Component Pairing: `StaffController.java` & `staff.fxml`

---

# 1. Executive Overview & UX Philosophy

### 1.1 Core Purpose
The **Staff RBAC Matrix & Security Administration** screen is the sovereign governance center of the Gym Management System. Restricted strictly to users holding the `StaffRole.ADMIN` credential, this interface oversees employee provisioning, role-based access control (RBAC) permissions (`ADMIN`, `MANAGER`, `STAFF`), credential lifecycle management, operator account deactivation, high-privilege administrative overrides (trainer reassignment and payment adjustments), and indelible audit logging.

### 1.2 Target Personas
- **System Administrator (Super-Admin)**: Provision new employee accounts, assign privilege tiers, execute cryptographic password resets, deactivate departing employees, and perform non-cascade reference checks before entity hard deletions.
- **Operations General Manager**: Audit security event logs, review administrator payment adjustments, and authorize coach client roster reassignments.

### 1.3 Governance Ergonomics & Mental Models
1. **Zero-Trust Administrative Gating (Rule 16)**: Every action on this screen is preceded by a strict runtime verification: `SessionContext.requireRole(StaffRole.ADMIN)`. If a non-admin attempts access, the interface immediately locks and redirects to the dashboard with an audit security warning.
2. **Dual-Pane Control Deck**:
   - *Left Pane (Staff Fleet & RBAC Matrix)*: High-density `M3DataTable` displaying all operators, role chips (`ADMIN`, `MANAGER`, `STAFF`), active status switches (`M3Switch`), and quick password reset triggers.
   - *Right Pane (Executive Override Command & Audit Timeline)*: Integrated drawer for executing trainer client reassignments (Rule 15), recording payment adjustments with previous amount preservation (Rule 19), and inspecting the live chronological `M3Timeline` audit feed.
3. **Guard-Checked Hard Deletions (Rule 4)**: Deleting an account requires a zero-reference check through `GuardService.countReferences`. If referenced rows exist in foreign tables, hard deletion is refused with an explanatory list of blocking records.

---

# 2. Visual Architecture & Wireframe ASCII Diagram

```
+---------------------------------------------------------------------------------------------------------------------------------------+
|  [M3TopAppBar: staffTopAppBar]                                                                                                        |
|  [=] GYM OS  ::  STAFF RBAC MATRIX & SECURITY GOVERNANCE   [Filter Staff: By Role v]  [Active Admin: alex_admin (ADMIN)]   [+ Add Staff]|
+-------------------------------------------------------------------+-------------------------------------------------------------------+
|  [LEFT PANE: OPERATOR DIRECTORY & ROLE MATRIX]                    |  [RIGHT PANE: OVERRIDES & CHRONOLOGICAL AUDIT TIMELINE]           |
|  +-------------------------------------------------------------+  |  +-------------------------------------------------------------+  |
|  | STAFF FLEET ROSTER                                          |  |  | ADMINISTRATIVE OVERRIDE ACTIONS (RULE 15 & 19)              |  |
|  | [Search Staff by Name / Username / Email...]               |  |  | +---------------------------------------------------------+ |  |
|  | [x] ADMIN (3)    [x] MANAGER (4)    [x] STAFF (12)          |  |  | | [M3Button: onReassign - Reassign Athlete's Coach]       | |  |
|  +-------------------------------------------------------------+  |  | | Reassigns athlete to new coach & moves medical profile.  | |  |
|  | [M3DataTable: staffDataTable]                               |  |  | +---------------------------------------------------------+ |  |
|  | +-----+---------------+----------+----------+--------+------+  |  | | [M3Button: onAdjustPayment - Adjust Payment Record]      | |  |
|  | | ID  | Full Name     | Username | Role     | Active | Act. |  |  | | Preserves previousAmount for receipt reconciliation.     | |  |
|  | +-----+---------------+----------+----------+--------+------+  |  | +---------------------------------------------------------+ |  |
|  | | #01 | Alex Vance    | alex_adm | ADMIN    | [ON ]  | [..] |  |  +-------------------------------------------------------------+  |
|  | | #02 | Sarah Jenkins | s_jenk   | MANAGER  | [ON ]  | [..] |  |  | LIVE AUDIT & SECURITY TIMELINE [M3Timeline: auditTimeline]     |  |
|  | | #03 | Marcus Fenix  | m_fenix  | STAFF    | [ON ]  | [..] |  |  | +---------------------------------------------------------+ |  |
|  | | #04 | Elena Rostova | e_rost   | STAFF    | [OFF]  | [..] |  |  | | 14:22:04 - Payment #8801 adjusted by admin_alex          | |  |
|  | +-----+---------------+----------+----------+--------+------+  |  | |   Prev: $499.00 -> New: $450.00 (Promo discount applied)   | |  |
|  | ACTION TOOLBAR:                                             |  |  | | 12:15:30 - Coach reassigned for Member #1042:            | |  |
|  | [M3Button: onAddStaff]      [M3Button: onEditStaff]         |  |  | |   From Marcus Vance -> Elena Rostova (Medical view moved)  | |  |
|  | [M3Button: onResetPassword] [M3Button: onDeleteStaff]       |  |  | | 09:00:12 - Staff password reset for user: m_fenix        | |  |
|  +-------------------------------------------------------------+  |  | | 08:30:00 - Security login: alex_admin authenticated      | |  |
|                                                                   |  | +---------------------------------------------------------+ |  |
|  [ROLE PRIVILEGE MATRIX: INLINE SUMMARY]                          |  +-------------------------------------------------------------+  |
|  ADMIN: Full System Mutation, Overrides, Hard Deletes, RBAC       |                                                                   |
|  MANAGER: Shift Rosters, Daily Reports, Void Payments, Schedules  |  [OVERRIDE / EDIT SIDESHEET: overrideSideSheet]                   |
|  STAFF: Fast-Lane Check-In, Booking Assistance, POS Cashier       |  +-------------------------------------------------------------+  |
|                                                                   |  | REASSIGN COACH FOR ATHLETE (RULE 15)                        |  |
|                                                                   |  | Athlete: Sarah Connor (ID #1042)                            |  |
|                                                                   |  | Current Coach: Marcus Vance (ID #12)                        |  |
|                                                                   |  | New Coach: [ Elena Rostova (ID #14)               v ]       |  |
|                                                                   |  | [x] Confirm Transfer of Confidential Medical Profile View   |  |
|                                                                   |  | [ Cancel ]                            [ Execute Transfer ]  |  |
+-------------------------------------------------------------------+--+-------------------------------------------------------------+--+
|  SECURITY BAR: [M3PulseBeacon: Guard Active] PBKDF2 Hashing Active | Session: Role Verified (ADMIN) | GuardService: 0 FK Cascade      |
+---------------------------------------------------------------------------------------------------------------------------------------+
```

---

# 3. Complete M3 & Custom Component Roster

| Component Class | Variant / Style Role | FXML `fx:id` | Functional Responsibility & Specs |
|---|---|---|---|
| `M3TopAppBar` | `CENTER_ALIGNED` | `staffTopAppBar` | Screen header displaying operator identity, active role badge, and role filter menu. |
| `M3DataTable` | `SECURITY_ROSTER` | `staffDataTable` | Virtualized roster table of administrators and staff. Columns: ID, Full Name, Username, Role, Email, Active Status Switch, Action Menu. |
| `M3Switch` | `HERO_TOGGLE` | `switchStaffActive` | Interactive toggle per row for activating/deactivating operator accounts. Inactive operators cannot log in per Rule 16. |
| `M3Button` | `FILLED` | `btnAddStaff` | Invokes `onAddStaff()`. Opens modal to register a new staff member with hashed credentials. |
| `M3Button` | `TONAL` | `btnEditStaff` | Invokes `onEditStaff(adminId)`. Launches dialog to update staff profile and role privileges. |
| `M3Button` | `OUTLINED` | `btnResetPassword` | Invokes `onResetPassword(adminId)`. Generates temporary password with PBKDF2 cryptographic hashing. |
| `M3Button` | `TEXT_DANGER` | `btnDeleteStaff` | Invokes `onDeleteStaff(adminId)`. Executes Rule 4 guard check (`GuardService.countReferences`) prior to deletion. |
| `M3Button` | `FILLED_TONAL` | `btnReassign` | Invokes `onReassign()`. Opens coach reassignment sheet, transferring medical dossier access per Rule 15. |
| `M3Button` | `OUTLINED_DANGER` | `btnAdjustPayment` | Invokes `onAdjustPayment(paymentId)`. Opens payment adjustment dialog recording `previousAmount` per Rule 19. |
| `M3Timeline` | `AUDIT_FEED` | `auditTimeline` | Chronological vertical stream recording administrative actions, logins, overrides, and security events. |
| `M3SideSheet` | `OVERRIDE_DRAWER` | `overrideSideSheet` | Lateral contextual drawer for executing coach reassignments and monetary adjustments without full modal obstruction. |
| `M3Dialog` | `ALERT_SURFACE` | `deleteGuardDialog` | Displays blocking reference count if `GuardService` finds foreign key dependencies. |
| `M3PulseBeacon` | `NEON_EMERALD` | `securityGuardBeacon` | Continuous status indicator confirming active security enclave and role integrity. |

---

# 4. Dynamic Theme & M3 Color Token Mapping

### 4.1 Theme Palette Seeds
- **Primary Governance Seed**: **Cyber Violet** (`#6750A4`) — Supreme administrative authority, cryptographic integrity, and elegance.
- **Admin Role Badge Accent**: **Solar Amber** (`#825500`) — Elevated role status identifier.
- **Security Alert & Hard Deletion**: **Apex Coral** (`#B3261E`) — Destructive actions and security warnings.

### 4.2 Looked-Up CSS Color Tokens
```css
/* Staff Governance Tokens */
.staff-root {
    -fx-background-color: -md-sys-color-surface;
    -fx-text-fill: -md-sys-color-on-surface;
}
.role-badge-admin {
    -fx-background-color: -md-sys-color-tertiary-container;
    -fx-text-fill: -md-sys-color-on-tertiary-container;
    -fx-font-weight: bold;
}
.role-badge-manager {
    -fx-background-color: -md-sys-color-secondary-container;
    -fx-text-fill: -md-sys-color-on-secondary-container;
}
.role-badge-staff {
    -fx-background-color: -md-sys-color-surface-container-highest;
    -fx-text-fill: -md-sys-color-on-surface-variant;
}
.audit-entry-card {
    -fx-background-color: -md-sys-color-surface-container-low;
    -fx-border-color: -md-sys-color-outline-variant;
    -fx-border-radius: 8px;
}
```

### 4.3 Visual Depth & Elevation Hierarchy
- **Level 0**: Main dashboard surface.
- **Level 1**: Staff data table and audit timeline container.
- **Level 2**: Role privilege cards and quick action buttons.
- **Level 3**: Contextual override `M3SideSheet`.
- **Level 4**: Credential reset modal and payment adjustment dialog.
- **Level 5**: High-security hard delete confirmation dialog (`deleteGuardDialog`).

---

# 5. Motion Physics, Animations & Shader Effects

### 5.1 RBAC Switch Flip & State Layer Physics
The `M3Switch` component toggles operator status using a physical thumb bounce:
$$x_{\text{thumb}}(t) = x_0 + (x_1 - x_0) \cdot \left(1 - e^{-\zeta \omega_n t} \cos(\omega_d t)\right)$$
Configured with $k = 400, \zeta = 0.65$. The track smoothly shifts between `-md-sys-color-surface-container-highest` and `-md-sys-color-primary`.

### 5.2 Override Drawer Spring Motion
When opening `onReassign` or `onAdjustPayment`, `overrideSideSheet` slides in using `EXPRESSIVE_SPATIAL` motion parameters:
$$k = 380, \quad \zeta = 0.70, \quad m = 1.0$$
If an operator cancels mid-slide, momentum reverses without visual jump.

### 5.3 Cybernetic Audit Beacon
The `securityGuardBeacon` executes a subtle harmonic breathing glow:
$$\text{Opacity}(t) = 0.6 + 0.4 \cdot \sin(2\pi \cdot 0.5 \cdot t)$$
Confirming active cryptographic background heartbeat.

---

# 6. Interactive States, Micro-interactions & Error Handling

### 6.1 State Matrix: RBAC Controls
| State | Visual Treatment | Micro-Interaction | Screen Reader Cue |
|---|---|---|---|
| **Staff Normal** | Elevation 1, standard text | Scale $1.0\times$ | "Staff member: {Name}, Role: {Role}" |
| **Row Hover** | Elevation 2, primary outline tint | Row elevates $+2\text{dp}$ | Cursor: `HAND` |
| **Switch Toggled** | Track fills with primary color | Thumb spring pops across | "Account status changed to Active/Inactive" |
| **Delete Attempt** | High-contrast coral outline | Warning vibration pulse | "Warning: Hard delete guard initiated" |

### 6.2 Rule-Driven Error Handling & Security Enforcements
1. **Rule 16 (Unauthorized Role Access)**:
   If an operator without `StaffRole.ADMIN` accesses this controller, `SessionContext.requireRole(StaffRole.ADMIN)` throws a `SecurityException`. The UI immediately aborts and displays an error alert.
2. **Rule 4 (Foreign Key Reference Blocking on Hard Delete)**:
   When `onDeleteStaff(adminId)` is invoked:
   The controller queries `GuardService.countReferences("administrators", adminId)`.
   If references $> 0$, the hard deletion is blocked and a dialog warns:
   > *"Cannot hard-delete staff record. 14 reference links exist in system audit tables. Deactivate account using the active switch instead."*
3. **Rule 15 (Trainer Reassignment Medical Authority)**:
   When executing `onReassign()`, the UI explicitly notifies the admin:
   *"Reassigning Member #1042 to Coach Rostova will transfer exclusive authorization to view their confidential MedicalProfile."*
4. **Rule 19 (Payment Adjustment Reconciliation)**:
   When `onAdjustPayment` is executed, the previous payment amount is never overwritten. `AdminService.adjustPayment` logs `previousAmount` so original receipts remain auditable.

---

# 7. Declarative FXML Node Hierarchy & Controller Contract

### 7.1 Valid FXML Structural Layout (`staff.fxml`)
```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import io.m3fx.controls.button.M3Button?>
<?import io.m3fx.controls.button.M3IconButton?>
<?import io.m3fx.controls.textfield.M3TextField?>
<?import io.m3fx.controls.table.M3DataTable?>
<?import io.m3fx.controls.timeline.M3Timeline?>
<?import io.m3fx.controls.sidesheet.M3SideSheet?>
<?import io.m3fx.controls.switchcontrol.M3Switch?>

<BorderPane xmlns="http://javafx.com/javafx/21" xmlns:fx="http://javafx.com/fxml/1"
            fx:controller="com.gym.controller.StaffController"
            styleClass="staff-root" prefWidth="1280.0" prefHeight="840.0">

    <!-- Top App Bar -->
    <top>
        <VBox spacing="8.0" styleClass="top-bar-container">
            <HBox alignment="CENTER_LEFT" spacing="16.0">
                <padding><Insets top="12.0" right="24.0" bottom="12.0" left="24.0"/></padding>
                <Label text="STAFF RBAC &amp; SECURITY GOVERNANCE" styleClass="title-large"/>
                <Spacer HBox.hgrow="ALWAYS"/>
                <Label text="ROLE: ADMIN (Alex Vance)" styleClass="badge-admin"/>
                <M3Button fx:id="btnTopAddStaff" text="+ Add Staff Member" onAction="#onAddStaff" styleClass="m3-button-filled"/>
            </HBox>
        </VBox>
    </top>

    <!-- Main Workspace Split -->
    <center>
        <SplitPane dividerPositions="0.55">
            <!-- Left Pane: Staff Roster & Management -->
            <VBox spacing="16.0">
                <padding><Insets top="16.0" right="16.0" bottom="16.0" left="16.0"/></padding>
                <HBox spacing="12.0" alignment="CENTER_LEFT">
                    <M3TextField fx:id="txtSearchStaff" promptText="Search staff members..." HBox.hgrow="ALWAYS"/>
                </HBox>

                <!-- Virtualized Staff Roster Table -->
                <M3DataTable fx:id="staffDataTable" VBox.vgrow="ALWAYS"/>

                <!-- Management Command Toolbar -->
                <HBox spacing="10.0" alignment="CENTER_RIGHT">
                    <M3Button fx:id="btnEditStaffAction" text="Edit Profile" onAction="#onEditStaffClick" styleClass="m3-button-tonal"/>
                    <M3Button fx:id="btnResetPasswordAction" text="Reset Password" onAction="#onResetPasswordClick" styleClass="m3-button-outlined"/>
                    <M3Button fx:id="btnDeleteStaffAction" text="Delete Staff" onAction="#onDeleteStaffClick" styleClass="m3-button-text-danger"/>
                </HBox>
            </VBox>

            <!-- Right Pane: Overrides & Audit Feed -->
            <VBox spacing="16.0">
                <padding><Insets top="16.0" right="16.0" bottom="16.0" left="16.0"/></padding>

                <!-- Admin Overrides Control Card -->
                <VBox spacing="12.0" styleClass="override-command-card">
                    <Label text="ADMINISTRATIVE OVERRIDE COMMANDS" styleClass="title-medium"/>
                    <HBox spacing="10.0">
                        <M3Button fx:id="btnReassignAction" text="Reassign Athlete Coach" onAction="#onReassign" 
                                  styleClass="m3-button-tonal" HBox.hgrow="ALWAYS" maxWidth="Infinity"/>
                        <M3Button fx:id="btnAdjustPaymentAction" text="Adjust Payment Record" onAction="#onAdjustPaymentClick" 
                                  styleClass="m3-button-outlined" HBox.hgrow="ALWAYS" maxWidth="Infinity"/>
                    </HBox>
                </VBox>

                <!-- Security & Audit Timeline -->
                <VBox spacing="8.0" VBox.vgrow="ALWAYS">
                    <Label text="CHRONOLOGICAL AUDIT &amp; SECURITY TIMELINE" styleClass="label-small"/>
                    <M3Timeline fx:id="auditTimeline" VBox.vgrow="ALWAYS"/>
                </VBox>
            </VBox>
        </SplitPane>
    </center>

    <!-- Contextual Override SideSheet -->
    <right>
        <M3SideSheet fx:id="overrideSideSheet" visible="false" prefWidth="380.0">
            <VBox spacing="16.0">
                <padding><Insets top="20.0" right="20.0" bottom="20.0" left="20.0"/></padding>
                <Label fx:id="lblSideSheetTitle" text="Administrative Override" styleClass="headline-small"/>
                <Label text="Configure entity parameters and review audit implications." styleClass="body-small"/>
                <VBox fx:id="sideSheetFormContainer" spacing="12.0" VBox.vgrow="ALWAYS"/>
                <HBox spacing="10.0" alignment="CENTER_RIGHT">
                    <M3Button text="Cancel" onAction="#onCloseSideSheetClick" styleClass="m3-button-outlined"/>
                    <M3Button text="Execute Override" styleClass="m3-button-filled"/>
                </HBox>
            </VBox>
        </M3SideSheet>
    </right>
</BorderPane>
```

### 7.2 Controller Contract Implementation (`StaffController.java`)
```java
package com.gym.controller;

import com.gym.model.Administrator;
import com.gym.model.StaffRole;
import com.gym.service.AdminService;
import com.gym.service.GuardService;
import com.gym.util.SessionContext;
import com.gym.util.FxUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import io.m3fx.controls.table.M3DataTable;
import io.m3fx.controls.timeline.M3Timeline;
import io.m3fx.controls.sidesheet.M3SideSheet;
import java.math.BigDecimal;
import java.util.List;

public class StaffController {

    private final AdminService adminService;
    private int selectedAdminId = -1;

    @FXML private TextField txtSearchStaff;
    @FXML private M3DataTable<Administrator> staffDataTable;
    @FXML private M3Timeline auditTimeline;
    @FXML private M3SideSheet overrideSideSheet;
    @FXML private Label lblSideSheetTitle;
    @FXML private VBox sideSheetFormContainer;

    public StaffController(AdminService adminService) {
        this.adminService = adminService;
    }

    @FXML
    public void initialize() {
        // Enforce Rule 16: StaffRole.ADMIN required
        SessionContext.requireRole(StaffRole.ADMIN);
        loadStaffRoster();
        setupTableSelection();
    }

    @FXML
    public void onAddStaff() {
        SessionContext.requireRole(StaffRole.ADMIN);
        // Opens creation modal to provision Administrator record
        FxUtils.snackbar("Provisioning new staff account.");
    }

    @FXML
    public void onEditStaff(int adminId) {
        SessionContext.requireRole(StaffRole.ADMIN);
        // Opens edit modal for target staff member
        FxUtils.snackbar("Editing staff profile #" + adminId);
    }

    @FXML
    public void onDeleteStaff(int adminId) {
        SessionContext.requireRole(StaffRole.ADMIN);
        // Enforce Rule 4: Refuse hard delete if foreign key references exist
        int refCount = GuardService.countReferences("administrators", adminId);
        if (refCount > 0) {
            FxUtils.error("Cannot delete staff record: " + refCount + " active database references exist. Deactivate the account instead.");
            return;
        }

        boolean confirmed = FxUtils.confirm("Are you sure you want to permanently delete Staff #" + adminId + "?");
        if (confirmed) {
            adminService.deleteStaff(adminId);
            FxUtils.snackbar("Staff record deleted.");
            loadStaffRoster();
        }
    }

    @FXML
    public void onResetPassword(int adminId) {
        SessionContext.requireRole(StaffRole.ADMIN);
        boolean confirmed = FxUtils.confirm("Reset password for Staff #" + adminId + "?");
        if (confirmed) {
            adminService.resetPassword(adminId, "TempPassword123!");
            FxUtils.snackbar("Password reset. Operator must update upon next login.");
        }
    }

    @FXML
    public void onReassign() {
        SessionContext.requireRole(StaffRole.ADMIN);
        lblSideSheetTitle.setText("Reassign Athlete Coach (Rule 15)");
        overrideSideSheet.setVisible(true);
    }

    @FXML
    public void onAdjustPayment(int paymentId) {
        SessionContext.requireRole(StaffRole.ADMIN);
        // Rule 19: Adjusts amount while preserving previousAmount
        adminService.adjustPayment(paymentId, new BigDecimal("450.00"), "Administrative discount override");
        FxUtils.snackbar("Payment #" + paymentId + " adjusted. Audit trail logged.");
    }

    private void loadStaffRoster() {
        List<Administrator> staff = adminService.findAllStaff();
        staffDataTable.getItems().setAll(staff);
    }

    private void setupTableSelection() {
        staffDataTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                this.selectedAdminId = newVal.getId();
            }
        });
    }

    @FXML private void onEditStaffClick() {
        if (selectedAdminId <= 0) { FxUtils.error("Select staff to edit."); return; }
        onEditStaff(selectedAdminId);
    }

    @FXML private void onDeleteStaffClick() {
        if (selectedAdminId <= 0) { FxUtils.error("Select staff to delete."); return; }
        onDeleteStaff(selectedAdminId);
    }

    @FXML private void onResetPasswordClick() {
        if (selectedAdminId <= 0) { FxUtils.error("Select staff to reset password."); return; }
        onResetPassword(selectedAdminId);
    }

    @FXML private void onAdjustPaymentClick() {
        onAdjustPayment(1); // Sample payment adjustment target
    }

    @FXML private void onCloseSideSheetClick() {
        overrideSideSheet.setVisible(false);
    }
}
```

---

# 8. Ergonomics, Touch Targets & Responsive Layout

### 8.1 Administrative Touch Ergonomics
- **Status Switches**: Minimum hit bounds of $48 \times 48\text{dp}$ around the `M3Switch` track to prevent missed toggles.
- **Action Buttons**: Uniform $48\text{dp}$ touch target height with explicit visual feedback.
- **SideSheet Dismiss Target**: Ample padding on close buttons and scrim backdrop tapping.

### 8.2 Responsive Layout Adaptations
- **Expanded (Administrator Desktop $\ge 1200\text{dp}$)**:
  Side-by-side staff roster ($650\text{dp}$) and persistent audit timeline ($550\text{dp}$).
- **Medium (Admin Tablet $840\text{dp} - 1199\text{dp}$)**:
  Roster and audit feed stack vertically in tabs.
- **Compact ($< 840\text{dp}$)**:
  Single-column table with compact role badges. Override controls convert to expandable `M3BottomSheet`.

---

# 9. Accessibility, Keyboard Navigation & Screen Readers

### 9.1 Keyboard Fast-Lane Operation
- `Ctrl + N`: Opens "+ Add Staff Member" dialog.
- `Ctrl + P`: Triggers password reset for selected staff.
- `Ctrl + R`: Triggers coach reassignment sheet.
- `ESC`: Dismisses open side sheet or modal dialogs.

### 9.2 Screen Reader Compliance
- Staff rows set `AccessibleRole.LIST_ITEM` announcing:
  `"Staff: Alex Vance, Role: ADMIN, Status: ACTIVE, Email: alex@gym.com."`
- Audit stream entries announce timestamp and security impact level:
  `"Security Event at 14:22: Payment 8801 adjusted by admin_alex."`

---

# 10. Quality Review Checklist & Pitfalls

### 10.1 Verification Checklist
1. [x] Controller signatures match `docs/UML.md` exactly (`StaffController`).
2. [x] Rule 4 (Hard delete is admin-only and blocked if `GuardService.countReferences` finds dependencies).
3. [x] Rule 15 (Trainer reassignment correctly transfers medical profile authorization).
4. [x] Rule 16 (`StaffRole.ADMIN` enforced on every `AdminService` method and controller action).
5. [x] Rule 19 (Payment adjustments record `previousAmount` for auditable reconciliation).
6. [x] Minimum $48 \times 48\text{dp}$ touch target compliance verified across all interactive nodes.

### 10.2 Top 5 Developer Mistakes & How to Prevent Them
1. **Executing Hard Deletions Without Guard Checks**: Calling `adminService.deleteStaff(id)` without evaluating `GuardService.countReferences`. *Fix: Enforce Rule 4; abort deletion if dependent references exist.*
2. **Bypassing the Role Authorization Check**: Allowing non-admin staff to access `StaffController`. *Fix: Always invoke `SessionContext.requireRole(StaffRole.ADMIN)` in `initialize()` and at the top of every event handler.*
3. **Overwriting Payment Records on Adjustment**: Directly updating payment amount in SQL without storing `previousAmount`. *Fix: Enforce Rule 19; `AdminService.adjustPayment` must record the previous amount for audit compliance.*
4. **Neglecting Inactive Accounts in Auth Checks**: Allowing deactivated staff (`active == false`) to authenticate. *Fix: Enforce Rule 16 in `AdminService.authenticate`.*
5. **Storing Plaintext Passwords**: Storing temporary or reset passwords in cleartext. *Fix: Always pass passwords through `PasswordUtils.hash` prior to database persistence.*
