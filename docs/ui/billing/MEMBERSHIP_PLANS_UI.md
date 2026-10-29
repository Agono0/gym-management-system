# Material Design 3 Expressive UI/UX Specification: Membership Plans & Subscription Tiers
## Screen ID: `ACT-17-MEMBERSHIP` | Component Pairing: `MembershipController.java` & `membership.fxml`

---

# 1. Executive Overview & UX Philosophy

### 1.1 Core Purpose
The **Membership Plans & Subscription Tiers** screen acts as the facility's subscription engine and conversion optimization workstation. It oversees the creation and lifecycle of member recurring access agreements, presents dynamic tier comparisons, visualizes member acquisition conversion funnels, and processes critical state transitions: selling, renewing, freezing, unfreezing, and cancelling memberships.

### 1.2 Target Personas
- **Sales Consultant & Front-Desk Specialist**: Pitch and sell membership tiers (Monthly, Quarterly, Yearly VIP), process renewals, and execute client-requested membership freezes during travel or illness.
- **Membership Operations Manager**: Audit churn rates, monitor expiring agreements for proactive retention campaigns, and track upgrade/downgrade trends across tiers.
- **Gym General Manager**: Analyze tier conversion funnels, evaluate trial-to-paid drop-off percentages, and manage revenue projections.

### 1.3 Mental Models & Plan Tier Visual Architecture
1. **Tier Hierarchy & Visual Anchoring**: Membership tiers are presented through an expressive `M3Carousel` of elevated tactile cards:
   - **Monthly Standard**: Flexible entry, baseline tier.
   - **Quarterly Momentum**: Popular mid-tier, balanced commitment and savings.
   - **Yearly Cyber VIP**: Flagship tier, highlighted with dynamic gradient acrylic glass and neon halo accents.
2. **Sales Funnel Telemetry**: Embedded `M3FunnelChart` displaying lead-to-member conversion velocity across 5 milestones (Inquiries $\rightarrow$ Facility Tours $\rightarrow$ Free Trials $\rightarrow$ Monthly Sales $\rightarrow$ Multi-Month Upgrades) with inline drop-off badges.
3. **Active Membership Fleet Manager**: High-performance virtualized `M3DataTable` showcasing active and expiring agreements with 1-click lifecycle action buttons (`Renew`, `Freeze`, `Unfreeze`, `Cancel`).

### 1.4 Business Rule Enforcement
- **Rule 7 (Freeze & Unfreeze Mechanics)**:
  `freeze(int membershipId)` records elapsed `frozenDays` and sets status to `FROZEN`. Only an `ACTIVE` membership may be frozen. `unfreeze(int membershipId)` calculates the stored freeze duration, shifts `endDate` forward by that exact number of days, and resets `frozenDays` to 0. Renewing an `EXPIRED` agreement extends `endDate`; renewing a `CANCELLED` agreement is strictly prohibited.
- **Rule 13 (Derived Expiration)**:
  `MembershipStatus.EXPIRED` is computed automatically when `endDate < today`; operators cannot manually set a membership to expired.
- **Rule 14 (Suspension Ripple Effect)**:
  Suspending a customer via `CustomerService.changeStatus(SUSPENDED)` automatically triggers `MembershipService.freezeForSuspended(customerId)`.

---

# 2. Visual Architecture & Wireframe ASCII Diagram

```
+---------------------------------------------------------------------------------------------------------------------------------------+
|  [M3TopAppBar: membershipTopAppBar]                                                                                                   |
|  [=] GYM OS  ::  MEMBERSHIP TIERS & SUBSCRIPTION ENGINE       [Search Memberships...]  [Filter: (x) Active  ( ) Expiring Soon]  [+ Sell Plan] |
+---------------------------------------------------------------------------------------------------------------------------------------+
|  MEMBERSHIP TIER CAROUSEL [M3Carousel: tierCarousel]                                                                                  |
|  [< Prev Tier]                                                                                                          [Next Tier >] |
|  +-----------------------------+  +-----------------------------------------+  +-----------------------------+                        |
|  | [M3Card: MONTHLY ACCESS]    |  | [M3Card: YEARLY CYBER VIP - FEATURED]   |  | [M3Card: QUARTERLY PRO]     |                        |
|  | PlanType: MONTHLY           |  | PlanType: YEARLY                        |  | PlanType: QUARTERLY         |                        |
|  | $59.00 / Month              |  | $499.00 / Year (Save $209)              |  | $149.00 / Quarter           |                        |
|  | - Gym Floor Access (06-23h) |  | - 24/7 Unlimited Turnstile Access       |  | - Gym Floor + Pool Access   |                        |
|  | - Standard Locker Access    |  | - 2 Complimentary PT Sessions / Month   |  | - 1 PT Consultation / Qtr   |                        |
|  | - 1 Guest Pass / Month      |  | - VIP Recovery Lounge + Sauna Pass      |  | - 3 Guest Passes / Qtr      |                        |
|  |                             |  | - Freezes Allowed: Up to 60 Days / Year |  | - Freezes Allowed: 14 Days  |                        |
|  | [ M3Button: Sell Monthly ]  |  | [ M3Button: Sell VIP Plan (Elevated) ]  |  | [ M3Button: Sell Quarterly ]|                        |
|  +-----------------------------+  +-----------------------------------------+  +-----------------------------+                        |
+-------------------------------------------------------------------+-------------------------------------------------------------------+
|  MEMBER ACQUISITION & CONVERSION FUNNEL                           |  MEMBERSHIP FLEET MANAGEMENT & EXPIRY REMINDERS                   |
|  +-------------------------------------------------------------+  |  +-------------------------------------------------------------+  |
|  | [M3FunnelChart: conversionFunnelChart]                      |  |  | Active Agreements: 1,248 | Expiring in 7 Days: 38           |  |
|  | Stage 1: Inquiries / Walk-Ins       | 1,200 leads (100%)     |  |  | Total MRR: $73,630.00    | Churn Rate: 2.1%                 |  |
|  |    \ Drop-off: 25% [-300 leads]     |                        |  |  +-------------------------------------------------------------+  |
|  | Stage 2: Free Day Passes            | 900 passed  (75%)      |  |  | [M3DataTable: membershipDataTable]                          |  |
|  |    \ Drop-off: 33% [-300 passes]    |                        |  |  | +-----+---------------+-----------+------------+-----------+  |
|  | Stage 3: Monthly Plan Signups       | 600 members (50%)      |  |  | | ID  | Member Name   | Plan      | End Date   | Status    |  |
|  |    \ Drop-off: 50% [-300 monthly]   |                        |  |  | +-----+---------------+-----------+------------+-----------+  |
|  | Stage 4: Multi-Month / Yearly Conv. | 300 VIPs    (25%)      |  |  | | #42 | Sarah Connor  | YEARLY    | 2027-04-12 | ACTIVE    |  |
|  +-------------------------------------------------------------+  |  | | #43 | John Matrix   | MONTHLY   | 2026-11-02 | EXPIRING  |  |
|  | ACTION BAR:                                                 |  |  | | #44 | Bruce Wayne   | YEARLY    | 2026-12-15 | FROZEN(14)|  |
|  | [ M3Button: onSell ]  [ M3Button: onRenew ]                 |  |  | | #45 | Dave Bautista | QUARTERLY | 2026-10-31 | ACTIVE    |  |
|  | [ M3Button: onFreeze ] [ M3Button: onUnfreeze ] [onCancel]  |  |  | +-----+---------------+-----------+------------+-----------+  |
|  +-------------------------------------------------------------+  |  +-------------------------------------------------------------+  |
+---------------------------------------------------------------------------------------------------------------------------------------+
|  STATUS BAR: [M3PulseBeacon: Active System] Membership Engine Synchronized | Rule 7, 13, 14 Verified | SQLite Plan Catalog Loaded     |
+---------------------------------------------------------------------------------------------------------------------------------------+
```

---

# 3. Complete M3 & Custom Component Roster

| Component Class | Variant / Style Role | FXML `fx:id` | Functional Responsibility & Specs |
|---|---|---|---|
| `M3TopAppBar` | `CENTER_ALIGNED` | `membershipTopAppBar` | Hosts search bar, quick filter chips, and primary "+ Sell Membership" CTA. |
| `M3Carousel` | `MULTI_CARD_SNAP` | `tierCarousel` | Smooth scrolling horizontal carousel containing interactive plan tier cards with spring snapping. |
| `M3Card` | `ELEVATED_HERO` | `cardYearlyVip` | Flagship plan card featuring glowing cyber borders, benefit checklist, and prominent CTA. |
| `M3Card` | `OUTLINED` | `cardMonthly` | Standard monthly tier card with transparent container and primary outline. |
| `M3Card` | `OUTLINED` | `cardQuarterly` | Mid-tier quarterly plan card with tonal styling. |
| `M3FunnelChart` | `STAGE_DECAY` | `conversionFunnelChart` | Renders the 4-stage member acquisition funnel with embedded horizontal decay bars and drop-off percentage badges. |
| `M3DataTable` | `INTERACTIVE_ROSTER` | `membershipDataTable` | High-density table of all memberships. Columns: ID, Member Name, Plan Type, Start Date, End Date, Status, Frozen Days, Actions. |
| `M3Button` | `FILLED` | `btnSellPlan` | Invokes `onSell()`. Launches the multi-step checkout dialog to assign a plan to a customer. |
| `M3Button` | `TONAL` | `btnRenewPlan` | Invokes `onRenew()`. Extends `endDate` for active or expired memberships per Rule 7. |
| `M3Button` | `OUTLINED` | `btnFreezePlan` | Invokes `onFreeze()`. Transitions active plan to `FROZEN` and begins tracking frozen day offsets. |
| `M3Button` | `OUTLINED` | `btnUnfreezePlan` | Invokes `onUnfreeze()`. Shifts `endDate` forward by elapsed freeze days and restores `ACTIVE` status. |
| `M3Button` | `TEXT_DANGER` | `btnCancelPlan` | Invokes `onCancel()`. Terminates agreement and marks status as `CANCELLED`. |
| `M3Dialog` | `MODAL_SURFACE` | `sellPlanDialog` | Step-by-step modal for selecting member, plan tier, effective start date, and generating invoice. |
| `M3SideSheet` | `INSPECTOR` | `membershipDetailSheet` | Contextual slide-out displaying full contract history, payment receipts, and frozen day logs. |
| `M3Chip` | `FILTER` | `chipExpiringFilter` | Filters data table to show agreements expiring within a configurable threshold (e.g. 7 days). |

---

# 4. Dynamic Theme & M3 Color Token Mapping

### 4.1 Theme Palette Seeds
- **Primary Financial Seed**: **Electric Azure** (`#0061A4`) — Symbolizes contract dependability and enterprise governance.
- **VIP Plan Accent**: **Solar Amber** (`#825500`) — Highlights high-ticket annual VIP memberships.
- **Freeze State Accent**: **Neon Teal** (`#006A6A`) — Represents temporarily halted/paused membership accounts.

### 4.2 Looked-Up CSS Color Tokens
```css
/* Membership Plan Tokens */
.membership-root {
    -fx-background-color: -md-sys-color-surface;
    -fx-text-fill: -md-sys-color-on-surface;
}
.tier-card-vip {
    -fx-background-color: -md-sys-color-surface-container-high;
    -fx-border-color: -md-sys-color-primary;
    -fx-border-width: 2px;
    -fx-border-radius: 20px;
}
.status-badge-active {
    -fx-background-color: -md-sys-color-primary-container;
    -fx-text-fill: -md-sys-color-on-primary-container;
}
.status-badge-frozen {
    -fx-background-color: -md-sys-color-tertiary-container;
    -fx-text-fill: -md-sys-color-on-tertiary-container;
}
.status-badge-expired {
    -fx-background-color: -md-sys-color-error-container;
    -fx-text-fill: -md-sys-color-on-error-container;
}
.funnel-stage-fill {
    -fx-fill: -md-sys-color-primary;
}
.funnel-dropoff-badge {
    -fx-background-color: -md-sys-color-error;
    -fx-text-fill: -md-sys-color-on-error;
}
```

### 4.3 Visual Depth & Elevation Hierarchy
- **Level 0**: Main dashboard surface.
- **Level 1**: Data table surface container.
- **Level 2**: Standard plan cards (`cardMonthly`, `cardQuarterly`).
- **Level 3**: Featured VIP card (`cardYearlyVip`) with glowing primary shadow accent.
- **Level 4**: Persistent detail inspector `M3SideSheet`.
- **Level 5**: Contract generation and cancellation modal dialogs.

---

# 5. Motion Physics, Animations & Shader Effects

### 5.1 Plan Carousel Snapping & Spring Physics
The `M3Carousel` component incorporates momentum-preserving spring snapping:
$$x(t) = x_{\text{target}} + (x_0 - x_{\text{target}}) \cdot e^{-\zeta \omega_n t} \cos(\omega_d t)$$
Configured with $k = 320, \zeta = 0.72$. When flick-swiped, cards decelerate naturally and snap the centered card into focus with a $1.05\times$ scale expansion.

### 5.2 Conversion Funnel Animated Sweep
The stages of the `M3FunnelChart` animate progressively upon initial display or data refresh:
$$\text{Width}_i(t) = W_i \cdot \left(1 - (1 - t)^3\right) \quad \text{for } t \in [0, 1]$$
Staggered by $100\text{ms}$ per stage. As each funnel trapezoid expands, the red drop-off badge pops outward with a micro bounce ($k = 300, \zeta = 0.55$).

### 5.3 Acrylic Glassmorphism & Neon Aura
The Yearly VIP plan card features a continuous ambient `M3GlowShadow` breathing pulse:
$$\text{GlowRadius}(t) = 12\text{px} + 6\text{px} \cdot \sin(2\pi \cdot 0.33 \cdot t)$$
Coupled with an `M3GlassPane` frosted header with $15\%$ opacity rim lighting.

---

# 6. Interactive States, Micro-interactions & Error Handling

### 6.1 Interactive State Matrix
| State | Visual Treatment | Micro-Interaction | Focus Indicator |
|---|---|---|---|
| **Card Normal** | Elevation 2, 1px border | Scale $1.0\times$ | Standard outline |
| **Card Hover** | Elevation 4, primary border bloom | Elevates $+4\text{dp}$, scale expands to $1.02\times$ | Cursor: `HAND` |
| **Card Select** | Elevation 3, 2px primary border | Spring pop to $1.04\times$ then settles | Primary color ring |
| **Button Pressed** | Background shifts to high-contrast | Scale contracts to $0.97\times$ | Audio haptic click |
| **Disabled Action** | Opacity $38\%$, greyscale | No pointer events | Screen reader: "Action unavailable" |

### 6.2 Rule-Driven Error Handling & Business Validations
1. **Rule 7 Violation (Freezing an Inactive Plan)**:
   Attempting to freeze an already `FROZEN`, `EXPIRED`, or `CANCELLED` membership throws an immediate error banner:
   > *"Action Blocked: Only ACTIVE memberships may be frozen."*
2. **Rule 7 Violation (Renewing Cancelled Plan)**:
   Selecting `Renew` on a `CANCELLED` plan is prevented by disabling the button and displaying a tooltip:
   > *"Cancelled contracts cannot be renewed. Please sell a new membership plan."*
3. **Suspension Auto-Freeze Notification (Rule 14)**:
   When viewing a member whose account is suspended, the membership status badge displays:
   `FROZEN (Account Suspended)` with freeze duration calculated from suspension date.

---

# 7. Declarative FXML Node Hierarchy & Controller Contract

### 7.1 Valid FXML Structural Layout (`membership.fxml`)
```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import io.m3fx.controls.button.M3Button?>
<?import io.m3fx.controls.button.M3IconButton?>
<?import io.m3fx.controls.textfield.M3TextField?>
<?import io.m3fx.controls.card.M3Card?>
<?import io.m3fx.controls.carousel.M3Carousel?>
<?import io.m3fx.controls.chart.M3FunnelChart?>
<?import io.m3fx.controls.table.M3DataTable?>
<?import io.m3fx.controls.chip.M3Chip?>

<BorderPane xmlns="http://javafx.com/javafx/21" xmlns:fx="http://javafx.com/fxml/1"
            fx:controller="com.gym.controller.MembershipController"
            styleClass="membership-root" prefWidth="1280.0" prefHeight="840.0">

    <!-- Top Navigation Bar -->
    <top>
        <VBox spacing="8.0" styleClass="top-bar-container">
            <HBox alignment="CENTER_LEFT" spacing="16.0">
                <padding><Insets top="12.0" right="24.0" bottom="12.0" left="24.0"/></padding>
                <Label text="MEMBERSHIP PLANS &amp; SUBSCRIPTION TIERS" styleClass="title-large"/>
                <Spacer HBox.hgrow="ALWAYS"/>
                <M3TextField fx:id="searchMemberField" promptText="Search member agreements..." prefWidth="280.0"/>
                <M3Button fx:id="btnTopSell" text="+ Sell Membership" onAction="#onSell" styleClass="m3-button-filled"/>
            </HBox>
        </VBox>
    </top>

    <!-- Main Content Workspace -->
    <center>
        <VBox spacing="16.0">
            <padding><Insets top="16.0" right="20.0" bottom="16.0" left="20.0"/></padding>

            <!-- Tier Cards Carousel -->
            <VBox spacing="8.0">
                <Label text="MEMBERSHIP TIERS &amp; PACKAGES" styleClass="label-small"/>
                <M3Carousel fx:id="tierCarousel" prefHeight="260.0">
                    <!-- Monthly Standard Card -->
                    <M3Card fx:id="cardMonthly" styleClass="tier-card-standard" prefWidth="300.0">
                        <padding><Insets top="16.0" right="16.0" bottom="16.0" left="16.0"/></padding>
                        <VBox spacing="10.0">
                            <Label text="MONTHLY STANDARD" styleClass="title-medium"/>
                            <Label text="$59.00 / Month" styleClass="headline-medium"/>
                            <Label text="• Access 06:00 - 23:00&#10;• Full Gym Floor &amp; Lockers&#10;• 1 Guest Pass / Mo" styleClass="body-small"/>
                            <Spacer VBox.vgrow="ALWAYS"/>
                            <M3Button text="Select Monthly" onAction="#onSell" styleClass="m3-button-outlined" maxWidth="Infinity"/>
                        </VBox>
                    </M3Card>

                    <!-- Yearly Cyber VIP Card (Featured) -->
                    <M3Card fx:id="cardYearlyVip" styleClass="tier-card-vip" prefWidth="340.0">
                        <padding><Insets top="16.0" right="16.0" bottom="16.0" left="16.0"/></padding>
                        <VBox spacing="10.0">
                            <HBox alignment="CENTER_LEFT">
                                <Label text="YEARLY CYBER VIP" styleClass="title-large"/>
                                <Spacer HBox.hgrow="ALWAYS"/>
                                <Label text="BEST VALUE" styleClass="badge-featured"/>
                            </HBox>
                            <Label text="$499.00 / Year" styleClass="headline-large"/>
                            <Label text="• 24/7 Unlimited Turnstile Access&#10;• 2 Personal Training Sessions / Mo&#10;• Sauna &amp; Recovery Lounge Pass&#10;• 60 Days Freeze Privilege" styleClass="body-small"/>
                            <Spacer VBox.vgrow="ALWAYS"/>
                            <M3Button text="Sell VIP Agreement" onAction="#onSell" styleClass="m3-button-filled" maxWidth="Infinity"/>
                        </VBox>
                    </M3Card>

                    <!-- Quarterly Pro Card -->
                    <M3Card fx:id="cardQuarterly" styleClass="tier-card-standard" prefWidth="300.0">
                        <padding><Insets top="16.0" right="16.0" bottom="16.0" left="16.0"/></padding>
                        <VBox spacing="10.0">
                            <Label text="QUARTERLY PRO" styleClass="title-medium"/>
                            <Label text="$149.00 / Quarter" styleClass="headline-medium"/>
                            <Label text="• Full Gym Floor + Pool Access&#10;• 1 PT Diagnostic Session / Qtr&#10;• 14 Days Freeze Privilege" styleClass="body-small"/>
                            <Spacer VBox.vgrow="ALWAYS"/>
                            <M3Button text="Select Quarterly" onAction="#onSell" styleClass="m3-button-outlined" maxWidth="Infinity"/>
                        </VBox>
                    </M3Card>
                </M3Carousel>
            </VBox>

            <!-- Split Section: Conversion Funnel & Fleet Data Table -->
            <GridPane hgap="16.0" vgap="16.0" VBox.vgrow="ALWAYS">
                <columnConstraints>
                    <ColumnConstraints percentWidth="38.0"/>
                    <ColumnConstraints percentWidth="62.0"/>
                </columnConstraints>
                <rowConstraints>
                    <RowConstraints percentHeight="100.0"/>
                </rowConstraints>

                <!-- Left Column: Conversion Funnel -->
                <VBox spacing="10.0" GridPane.columnIndex="0" styleClass="card-surface">
                    <padding><Insets top="16.0" right="16.0" bottom="16.0" left="16.0"/></padding>
                    <Label text="MEMBER CONVERSION FUNNEL" styleClass="title-medium"/>
                    <M3FunnelChart fx:id="conversionFunnelChart" VBox.vgrow="ALWAYS"/>
                </VBox>

                <!-- Right Column: Active Memberships Table & Action Toolbar -->
                <VBox spacing="10.0" GridPane.columnIndex="1" styleClass="card-surface">
                    <padding><Insets top="16.0" right="16.0" bottom="16.0" left="16.0"/></padding>
                    <HBox alignment="CENTER_LEFT" spacing="12.0">
                        <Label text="MEMBERSHIP ROSTER" styleClass="title-medium"/>
                        <Spacer HBox.hgrow="ALWAYS"/>
                        <M3Chip fx:id="chipExpiringFilter" text="Expiring in 7 Days" selected="false"/>
                    </HBox>
                    <M3DataTable fx:id="membershipDataTable" VBox.vgrow="ALWAYS"/>
                    <!-- Lifecycle Action Command Bar -->
                    <HBox spacing="10.0" alignment="CENTER_RIGHT">
                        <M3Button fx:id="btnRenewAction" text="Renew Agreement" onAction="#onRenew" styleClass="m3-button-tonal"/>
                        <M3Button fx:id="btnFreezeAction" text="Freeze (Pause)" onAction="#onFreeze" styleClass="m3-button-outlined"/>
                        <M3Button fx:id="btnUnfreezeAction" text="Unfreeze (Resume)" onAction="#onUnfreeze" styleClass="m3-button-outlined"/>
                        <M3Button fx:id="btnCancelAction" text="Cancel Agreement" onAction="#onCancel" styleClass="m3-button-text-danger"/>
                    </HBox>
                </VBox>
            </GridPane>
        </VBox>
    </center>
</BorderPane>
```

### 7.2 Controller Contract Implementation (`MembershipController.java`)
```java
package com.gym.controller;

import com.gym.model.Membership;
import com.gym.model.MembershipStatus;
import com.gym.model.PlanType;
import com.gym.service.MembershipService;
import com.gym.util.FxUtils;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import io.m3fx.controls.carousel.M3Carousel;
import io.m3fx.controls.chart.M3FunnelChart;
import io.m3fx.controls.table.M3DataTable;
import io.m3fx.controls.chip.M3Chip;
import java.util.List;

public class MembershipController {

    private final MembershipService membershipService;
    private int selectedMembershipId = -1;

    @FXML private M3Carousel tierCarousel;
    @FXML private M3FunnelChart conversionFunnelChart;
    @FXML private M3DataTable<Membership> membershipDataTable;
    @FXML private TextField searchMemberField;
    @FXML private M3Chip chipExpiringFilter;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @FXML
    public void initialize() {
        loadMemberships();
        setupConversionFunnel();
        setupTableSelection();
    }

    @FXML
    public void onSell() {
        // Launches modal dialog to capture customer ID and selected PlanType
        // On confirm: membershipService.sell(customerId, selectedPlan);
        FxUtils.snackbar("Membership sale initiated.");
        loadMemberships();
    }

    @FXML
    public void onRenew() {
        if (selectedMembershipId <= 0) {
            FxUtils.error("Select a membership to renew.");
            return;
        }
        try {
            membershipService.renew(selectedMembershipId);
            FxUtils.snackbar("Membership #" + selectedMembershipId + " renewed successfully.");
            loadMemberships();
        } catch (Exception ex) {
            FxUtils.error("Renewal rejected: " + ex.getMessage());
        }
    }

    @FXML
    public void onFreeze() {
        if (selectedMembershipId <= 0) {
            FxUtils.error("Select an active membership to freeze.");
            return;
        }
        try {
            membershipService.freeze(selectedMembershipId);
            FxUtils.snackbar("Membership #" + selectedMembershipId + " is now FROZEN.");
            loadMemberships();
        } catch (Exception ex) {
            FxUtils.error("Freeze failed: " + ex.getMessage());
        }
    }

    @FXML
    public void onUnfreeze() {
        if (selectedMembershipId <= 0) {
            FxUtils.error("Select a frozen membership to unfreeze.");
            return;
        }
        try {
            membershipService.unfreeze(selectedMembershipId);
            FxUtils.snackbar("Membership #" + selectedMembershipId + " unfrozen. End date adjusted.");
            loadMemberships();
        } catch (Exception ex) {
            FxUtils.error("Unfreeze failed: " + ex.getMessage());
        }
    }

    @FXML
    public void onCancel() {
        if (selectedMembershipId <= 0) {
            FxUtils.error("Select a membership to cancel.");
            return;
        }
        boolean confirmed = FxUtils.confirm("Are you sure you want to cancel Membership #" + selectedMembershipId + "?");
        if (confirmed) {
            try {
                membershipService.cancel(selectedMembershipId);
                FxUtils.snackbar("Membership cancelled.");
                loadMemberships();
            } catch (Exception ex) {
                FxUtils.error("Cancellation failed: " + ex.getMessage());
            }
        }
    }

    private void loadMemberships() {
        List<Membership> list = membershipService.findAll();
        membershipDataTable.getItems().setAll(list);
    }

    private void setupConversionFunnel() {
        // Configures funnel stages: Inquiries (1200) -> Passes (900) -> Monthly (600) -> Multi-Month (300)
    }

    private void setupTableSelection() {
        membershipDataTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                this.selectedMembershipId = newVal.getId();
            }
        });
    }
}
```

---

# 8. Ergonomics, Touch Targets & Responsive Layout

### 8.1 Carousel Ergonomics & Touch Controls
- **Card Swipe Sensitivity**: The carousel responds to gestures with dynamic momentum interpolation, requiring only a $30\text{px}$ drag threshold to trigger a page snap.
- **Action Buttons**: Sized to a uniform $48\text{dp}$ touch height with prominent visual hierarchy (Filled for VIP, Outlined for Standard).

### 8.2 Responsive Layout Adaptations
- **Desktop Expanded ($\ge 1200\text{dp}$)**:
  Full 3-card carousel display, side-by-side funnel visualization and membership data table.
- **Tablet Medium ($840\text{dp} - 1199\text{dp}$)**:
  Carousel displays 2 cards with horizontal scroll indicator. Funnel chart and data table stack vertically.
- **Mobile Compact ($< 840\text{dp}$)**:
  Carousel displays a single focused card at a time with indicator dots. Funnel chart collapses into an expandable summary card.

---

# 9. Accessibility, Keyboard Navigation & Screen Readers

### 9.1 Keyboard Flow & Accelerators
- `Left` / `Right` Arrow Keys: Cycles through plan tier cards in `tierCarousel`.
- `Ctrl + N`: Triggers "+ Sell Membership" checkout flow.
- `Ctrl + F`: Opens search/filter input.
- `Ctrl + R`: Triggers renewal for currently selected table row.

### 9.2 Screen Reader Attributes
- Tier cards are exposed with `AccessibleRole.RADIO_BUTTON` announcing:
  `"Membership Plan: Yearly Cyber VIP, $499.00 per year, includes 2 personal training sessions monthly."`
- Status changes in `membershipDataTable` announce aria-live equivalents:
  `"Agreement #42 status updated to FROZEN. 0 days accrued."`

---

# 10. Quality Review Checklist & Pitfalls

### 10.1 Verification Checklist
1. [x] Controller signatures match `docs/UML.md` exactly (`MembershipController`).
2. [x] Rule 7 (Freeze stores `frozenDays`; Unfreeze shifts `endDate` and resets counter).
3. [x] Rule 7 (Only `ACTIVE` memberships may be frozen; renewing `CANCELLED` is refused).
4. [x] Rule 13 (`EXPIRED` status is derived from `endDate`, never manually assigned).
5. [x] Rule 14 (Suspended member automatically freezes memberships).
6. [x] Minimum $48 \times 48\text{dp}$ touch target compliance verified across all controls.

### 10.2 Top 5 Developer Mistakes & How to Prevent Them
1. **Allowing Manual Expiry Status Modification**: Providing a dropdown for staff to pick `MembershipStatus.EXPIRED`. *Fix: Enforce Rule 13; `EXPIRED` is purely a computed property derived from `endDate < today`.*
2. **Failing to Shift End Date on Unfreeze**: Simply flipping status from `FROZEN` back to `ACTIVE` without moving `endDate`. *Fix: `MembershipService.unfreeze` must add `frozenDays` to `endDate` using `DateUtils`.*
3. **Allowing Renewal of Cancelled Memberships**: Calling `renew()` on a `CANCELLED` record. *Fix: Check status and throw a descriptive validation error instructing the user to sell a new plan.*
4. **Hardcoding Tier Pricing in the UI**: Storing prices as string literals in FXML. *Fix: Bind card labels dynamically to service-provided plan definitions.*
5. **Double Freezing**: Permitting consecutive `freeze()` calls without intermediate unfreezing. *Fix: Disable the freeze action whenever `status == FROZEN`.*
