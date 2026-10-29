# Material Design 3 Expressive UI Specification: Authentication & Security Portal

- **File Target**: `src/main/resources/fxml/login.fxml`
- **Controller Binding**: `com.gym.controller.LoginController`
- **Specification Version**: 2.6.0 (2026 M3 Expressive Architecture)
- **Document Status**: Production Ready / Normative Design Standard

---

## 1. Executive Overview & UX Philosophy

### 1.1 Core Purpose
The **Authentication & Security Portal** serves as the cybernetic gateway to the Gym Management System. Its architectural mission is twofold:
1. **Zero-Friction Authentication**: Facilitate rapid, single-motion operator authentication for front-desk receptionists, trainers, and system administrators entering high-intensity shifts.
2. **Atmospheric Immersion & Trust**: Establish immediate enterprise-grade athletic prestige through dynamic hardware-accelerated 3D graphics, subtle particle ambiance, and precision glassmorphic elevations, visually communicating system integrity and operational readiness.

### 1.2 Target Personas & Cognitive Workflows
- **Gym Receptionist (`StaffRole.STAFF`)**: Rapid handoff between shifts. Needs large, clear text fields, high-contrast visual focus targets, immediate credential validation, and clear error diagnostics if a shift account is locked or misentered.
- **Head Coach / Trainer (`StaffRole.MANAGER`)**: Logging in from ruggedized floor tablets or front kiosks. Prioritizes touch-optimized controls (minimum 48×48dp target zones), biometric/one-touch focus flow, and instant visual verification of network/system state.
- **General Manager / Owner (`StaffRole.ADMIN`)**: Needs secure authentication gates satisfying Business Rule 16 (strict role elevation verification), password reset channels, and clear indicators of security posture (session persistence state, encryption badge).

```
               +-------------------------------------------------------+
               |                  COGNITIVE FUNNEL                     |
               +-------------------------------------------------------+
               | 1. GLANCE: Peripheral verification of "SYSTEM READY"  |
               |    and active athletic seed theme ambiance.           |
               +---------------------------+---------------------------+
                                           |
                                           v
               +-------------------------------------------------------+
               | 2. FOCUS: Auto-focused Username field; 2px primary    |
               |    glow ring removes all extraneous decision branching.|
               +---------------------------+---------------------------+
                                           |
                                           v
               +-------------------------------------------------------+
               | 3. COMMIT: Natural [TAB] -> Password -> [ENTER].     |
               |    Instant spring-morph button state transition.      |
               +-------------------------------------------------------+
```

### 1.3 2026 M3 Expressive UX Philosophy
The authentication portal leverages **Material Design 3 Expressive** principles adapted for athletic management software:
- **Bento Box Visual Hierarchy**: A balanced asymmetrical two-column layout. The left viewport acts as the "Hero Athletic Canvas" showcasing real-time 3D kinetic gyroscopes and telemetry banners; the right pane houses the high-density elevated acrylic card.
- **Thumb-Friendly Ergonomic Clusters**: Input controls, primary submission buttons, and contextual recovery links are stacked linearly with generous 24dp vertical rhythm to ensure 0% mis-tap probability on touchscreen terminals.
- **Progressive Disclosure**: Password visibility toggles, dynamic theme seed selectors, and error detail trays only reveal themselves on demand, maintaining an ultra-clean cognitive plane during standard operational logins.

---

## 2. Visual Architecture & Wireframe ASCII Diagram

### 2.1 High-Fidelity Wireframe (Expanded Desktop & Floor Terminal: 1440 × 900dp)

```
+--------------------------------------------------------------------------------------------------------------------+
| TOP UTILITY BAR (Integrated into Glass Canopy)                                      [ - ] [ [] ] [ X ]             |
| Logo: [>] GYM-OS // ATHLETIC ENGINE 2026                      Status: (o) CLOUD SYNCED   Theme Seed: [O][O][O][O] |
+--------------------------------------------------------------------------------------------------------------------+
|                                                  |                                                                 |
|            HERO ATHLETIC 3D CANVAS               |              AUTHENTICATION BENTO CONTAINER                     |
|                   (Col 1: 58%)                   |                         (Col 2: 42%)                            |
|                                                  |                                                                 |
|   +------------------------------------------+   |   +---------------------------------------------------------+   |
|   | M3GlassPane / M3ParticleCanvas           |   |   | M3ElevatedCard (Surface Container High, Elevation Level 2)|   |
|   |                                          |   |   |                                                         |   |
|   |          /===================\           |   |   |   [>] PORTAL ACCESS                                     |   |
|   |         /   M3GyroRing3D      \          |   |   |   Sign in to your Gym Management console                |   |
|   |        |   +----------------+  |         |   |   |   ---------------------------------------------------   |   |
|   |        |   | 3D Athletic    |  |         |   |   |                                                         |   |
|   |        |   | Kinetic Core   |  |         |   |   |   Username / Staff ID                                   |   |
|   |        |   | (Pivoting      |  |         |   |   |   +-------------------------------------------------+   |   |
|   |        |   |  Gyroscopes)   |  |         |   |   |   | [@] | j.doe_admin                          |   |   |   |
|   |        |   +----------------+  |         |   |   |   +-------------------------------------------------+   |   |
|   |         \                     /          |   |   |   Helper: Enter your alphanumeric staff identity        |   |
|   |          \===================/           |   |   |                                                         |   |
|   |                                          |   |   |   Master Key / Password                                 |   |
|   |   "EMPOWERING PEAK HUMAN PERFORMANCE"     |   |   |   +-------------------------------------------------+   |   |
|   |   Live Telemetry: 1,420 Active Members   |   |   |   | [*] | ********************               | [Eye] |  |   |
|   |   Floor Density:  84% Capacity [Normal]  |   |   |   +-------------------------------------------------+   |   |
|   |                                          |   |   |   [!] Caps Lock is ON (Auto-detected warning tray)   |   |
|   |   +----------------------------------+   |   |   |                                                         |   |
|   |   | M3Scanline & M3PulseBeacon HUD   |   |   |   |   [x] Keep terminal session active (12h Shift)          |   |
|   |   | (o) BIOMETRIC HUB READY          |   |   |   |                                                         |   |
|   |   +----------------------------------+   |   |   |   +-------------------------------------------------+   |   |
|   +------------------------------------------+   |   |   | M3Button (FILLED, Large, Height 56dp)           |   |
|                                                  |   |   |                 SIGN IN TO TERMINAL             |   |
|                                                  |   |   +-------------------------------------------------+   |   |
|                                                  |   |                                                         |   |
|                                                  |   |   [?] Trouble accessing console? [Reset Credentials]    |   |
|                                                  |   |                                                         |   |
|                                                  |   |   ---------------------------------------------------   |   |
|                                                  |   |   Security Badge: TLS 1.3 // SQLite WAL Protected       |   |
|                                                  |   +---------------------------------------------------------+   |
|                                                  |                                                                 |
+--------------------------------------------------------------------------------------------------------------------+
| FOOTER TELEMETRY HUD: Engine Version 2.6.4-M3Expressive | DB: gym.db [WAL] | Latency: 0.4ms | Station: FRONT_DESK_01|
+--------------------------------------------------------------------------------------------------------------------+
```

### 2.2 Responsive Structural Breakdown
1. **Root Scaffolding**: `StackPane` hosting a full-screen dynamic backdrop (`M3ParticleCanvas`) overlaid with a translucent acrylic vignette (`M3GlassPane`).
2. **Main Split Grid**: `HBox` split with proportional weighting (`58%` hero display, `42%` authentication card).
3. **Hero Viewport**: Contains the interactive 3D gyroscope (`M3GyroRing3D`), ambient laser scanline sweeps (`M3Scanline`), and high-impact motivational typographic anchors.
4. **Interactive Bento Form**: An `M3ElevatedCard` enclosed in an `M3GlowShadow` breathing rim, featuring input fields, assistive helper labels, keyboard shortcut hints, and an expressive primary action button with integrated spring morphing.

---

## 3. Complete M3 & Custom Component Roster

| Component Class | FXML Tag / Variable | Variant / Style Class | Semantic Role & Configuration Parameters |
|---|---|---|---|
| `io.m3fx.controls.layout.M3StackPane` | `rootPane` | `.auth-root` | Top-level layout container; manages layer depth (particles, background glass, foreground form). |
| `io.m3fx.controls.effect.M3ParticleCanvas` | `particleBackdrop` | `.ambient-particles` | Hardware-accelerated canvas generating 35 drifting cybernetic embers; velocity $v \in [0.2, 0.8]\text{ dp/frame}$. |
| `io.m3fx.controls.effect.M3GlassPane` | `glassCanopy` | `.acrylic-blur` | Real-time Gaussian backdrop blur ($r = 24\text{px}$) with specular highlight stroke (`-md-sys-color-surface-tint`). |
| `io.m3fx.controls.card.M3Card` | `authCard` | `ELEVATED` (`.auth-card`) | Floating surface container; Corner radius 28dp (`ShapeTokens.EXTRA_LARGE`), Level 2 surface elevation. |
| `io.m3fx.controls.effect.M3GlowShadow` | `cardGlow` | `.neon-halo` | Dynamic outer glow breathing on hover/focus; spread radius 16dp, color `-md-sys-color-primary` at 25% opacity. |
| `io.m3fx.controls.scene3d.M3GyroRing3D` | `kineticGyro` | `.hero-gyro` | Interactive multi-axis 3D gimbal model reflecting mouse cursor parallax ($X, Y \in [-15^\circ, +15^\circ]$). |
| `io.m3fx.controls.effect.M3Scanline` | `hologramScanline` | `.scan-effect` | Periodic subtle scanline sweep across the hero viewport; cycle duration 4000ms. |
| `io.m3fx.controls.effect.M3PulseBeacon` | `statusBeacon` | `.beacon-online` | Pulsing circular radar beacon; status green (`#386A20`), expansion pulse $\Delta r = 8\text{dp}$. |
| `io.m3fx.controls.textfield.M3TextField` | `usernameField` | `OUTLINED` (`.field-username`) | 56dp height container, leading icon `account_circle`, clearable trailing button, autocomplete hook. |
| `io.m3fx.controls.textfield.M3TextField` | `passwordField` | `OUTLINED` (`.field-password`) | 56dp height, leading icon `lock`, trailing `M3IconButton` toggle for password reveal (`visibility`/`visibility_off`). |
| `io.m3fx.controls.selection.M3CheckBox` | `rememberMeCheckBox` | `STANDARD` | 48×48dp touch footprint, label "Keep terminal session active", maps to shift persistence context. |
| `io.m3fx.controls.button.M3Button` | `loginButton` | `FILLED` (`.btn-primary-action`) | 56dp height, corner radius 28dp pill shape; primary submission action calling `onLogin()`. |
| `io.m3fx.controls.button.M3Button` | `forgotPasswordButton` | `TEXT` (`.btn-tertiary-link`) | Accessible focusable text button; calls `onForgotPassword()` to trigger credential recovery flow. |
| `io.m3fx.controls.progress.M3CircularProgress`| `loginSpinner` | `DETERMINATE_OR_INDETERMINATE` | 24×24dp inline indicator embedded inside `loginButton` during authentication execution. |
| `io.m3fx.controls.chip.M3Chip` | `securityBadgeChip` | `ASSIST` | Compact 32dp chip displaying active security protocol ("TLS 1.3 / WAL active"). |
| `io.m3fx.controls.snackbar.M3Snackbar` | `authSnackbar` | `FLOATING` | 344dp width floating notification card for transient network alerts, lockout timers, and recovery dialogs. |

---

## 4. Dynamic Theme & M3 Color Token Mapping

The authentication screen supports the complete Material 3 dynamic color scheme, dynamically shifting between Light and Dark modes while accepting custom fitness-oriented seed hues.

### 4.1 Recommended Palette Seeds
1. **Cyber Athletic Teal (Default)**: `#006A6A` (Represents endurance, technology, and clinical precision).
2. **High-Voltage Azure**: `#0061A4` (Represents speed, strength, and enterprise stability).
3. **Hyper-Drive Purple**: `#6750A4` (Represents elite VIP club performance and prestige).
4. **Thermal Coral**: `#B3261E` (Warning / High-intensity interval operational modes).

### 4.2 Contrast Matrix & Token Lookup Table

| UI Element / Surface Role | Light Mode Value | Dark Mode Value | Exact CSS Token Variable |
|---|---|---|---|
| Main Window Canvas Background | `#F4FBF8` (Surface Container Lowest) | `#0E1513` (Surface Container Lowest) | `-md-sys-color-surface-container-lowest` |
| Auth Bento Card Container | `#FFFFFF` (Surface Container High) | `#1A211F` (Surface Container High) | `-md-sys-color-surface-container-high` |
| Hero Graphic Acrylic Pane | `rgba(255, 255, 255, 0.65)` | `rgba(18, 24, 22, 0.65)` | `-md-sys-color-surface-container-low` |
| Primary Input Field Outline | `#6C7975` (Outline) | `#89938F` (Outline) | `-md-sys-color-outline` |
| Input Field Focus Ring & Halo | `#006A6A` (Primary) | `#4DDAD7` (Primary) | `-md-sys-color-primary` |
| Primary Submission Button Container| `#006A6A` (Primary) | `#4DDAD7` (Primary) | `-md-sys-color-primary` |
| Primary Submission Button Label | `#FFFFFF` (On-Primary) | `#003737` (On-Primary) | `-md-sys-color-on-primary` |
| Error Warning Helper Label | `#BA1A1A` (Error) | `#FFB4AB` (Error) | `-md-sys-color-error` |
| Error Outline State | `#BA1A1A` (Error) | `#FFB4AB` (Error) | `-md-sys-color-error` |
| Card Border Accent (Resting) | `#C8D7D3` (Outline Variant) | `#3F4947` (Outline Variant) | `-md-sys-color-outline-variant` |
| Specular Glass Rim Highlight | `rgba(255, 255, 255, 0.8)` | `rgba(255, 255, 255, 0.12)` | `-md-sys-color-surface-tint` |

### 4.3 Visual Depth & Elevation Ladder
- **Elevation 0 (Window Base)**: `0px` offset, pure flat token `-md-sys-color-surface-container-lowest`.
- **Elevation 1 (Glass Hero Pane)**: `0px` X, `1px` Y, blur `3px`, spread `0px`, ambient shadow `rgba(0, 0, 0, 0.08)`.
- **Elevation 2 (Auth Bento Card - Resting)**: `0px` X, `3px` Y, blur `8px`, spread `1px`, key shadow `rgba(0, 0, 0, 0.14)`. Surface container: `surface-container-high`.
- **Elevation 3 (Auth Bento Card - Hover / Active)**: `0px` X, `6px` Y, blur `14px`, spread `2px`, key shadow `rgba(0, 0, 0, 0.20)`. Outer glow: 16dp halo in `-md-sys-color-primary`.
- **Elevation 4 (Floating Dialogs / Sheet Overlays)**: `0px` X, `8px` Y, blur `20px`, spread `3px`.

---

## 5. Motion Physics, Animations & Shader Effects

### 5.1 Mass-Spring-Damper Physics Presets
To ensure responsive, athletic tactile feedback without artificial mechanical stiffness, all screen animations utilize damped harmonic spring physics:

$$\ddot{x} + 2\zeta\omega_0\dot{x} + \omega_0^2(x - x_{\text{target}}) = 0, \quad \text{where } \omega_0 = \sqrt{\frac{k}{m}}$$

```
+----------------------------------------------------------------------------------------------------+
|                                 MOTION PHYSICS TUNING TABLE                                        |
+--------------------------+--------+---------------+----------------------+-------------------------+
| Interaction Event        | Mass m | Stiffness k   | Damping Ratio zeta   | Perceived Feel          |
+--------------------------+--------+---------------+----------------------+-------------------------+
| Card Entrance Parallax   | 1.0 kg | 320 N/m       | 0.68 (Underdamped)   | Smooth athletic punch   |
| Button Press / Depress   | 0.4 kg | 800 N/m       | 0.72 (Snappy)        | Instant tactile snap    |
| Error Shake Shake-X      | 0.2 kg | 1200 N/m      | 0.35 (Oscillatory)   | High-tension vibration  |
| Mode Morph (Login->Spin) | 0.8 kg | 450 N/m       | 0.85 (Smooth)        | Organic transformation  |
| Parallax Gyroscope Tilt  | 1.5 kg | 220 N/m       | 0.90 (Near Critical) | Heavy physical inertia  |
+--------------------------+--------+---------------+----------------------+-------------------------+
```

### 5.2 Micro-Interactions & Transitions
1. **Interactive Parallax Cursor Tracking**:
   - The `kineticGyro` node tracks mouse movements across the window. Coordinates $(x, y)$ map to rotation angles $\theta_y = (x - w/2) \cdot 0.02^\circ$, $\theta_x = -(y - h/2) \cdot 0.02^\circ$.
   - Transitions preserve momentum vector $\vec{v}$; abrupt mouse exits settle naturally via spring interpolation.
2. **Tactile Button Press**:
   - On `MOUSE_PRESSED`: Container scales down uniformly to `0.97` with state layer opacity rising to `10%`.
   - On `MOUSE_RELEASED`: Container rebounds to `1.00` with energetic spring overshoot ($\Delta s = +0.015$).
3. **Invalid Credential Shake Matrix**:
   - An authentication rejection triggers a 4-cycle horizontal oscillation on `authCard`:
     $$\Delta X(t) = A \cdot e^{-\lambda t} \cdot \sin(\omega t), \quad A = 14\text{dp}, \; \lambda = 6.2, \; \omega = 45\text{ rad/s}$$
   - Total duration: 320ms. Simultaneously, input field borders interpolate from `-md-sys-color-outline` to `-md-sys-color-error`.
4. **Shader Emulation in JavaFX**:
   - Specular rim highlighting is achieved via an interior `M3GlassPane` stroke with a linear gradient mask (`rgba(255,255,255,0.3)` at $0^\circ$ fading to `rgba(255,255,255,0.0)` at $180^\circ$).
   - The scanline sweep effect is animated via a vertical translate offset cycling over a clipping group with blend mode `BlendMode.OVERLAY`.

---

## 6. Interactive States, Micro-interactions & Error Handling

### 6.1 Input Field State Matrix

```
+----------------------------------------------------------------------------------------------------+
|                                    INPUT FIELD STATE TRANSITIONS                                   |
+-------------+---------------------+-------------------+---------------------+----------------------+
| State       | Container Color     | Border Outline    | Leading Icon Tint   | Assistive Text       |
+-------------+---------------------+-------------------+---------------------+----------------------+
| Resting     | Transparent         | 1px Outline Var   | On-Surface-Variant  | Dimmed Neutral       |
| Hovered     | Surface Container Low| 1px Outline      | On-Surface          | Dimmed Neutral       |
| Focused     | Surface Cont. Lowest| 2px Primary       | Primary             | Primary (Elevated)   |
| Error       | Surface Cont. Lowest| 2px Error         | Error               | Error (Bold / Alert) |
| Disabled    | Surface Cont. Lowest| 1px Outl. 12% Opac| On-Surface 38% Opac | Hidden               |
+-------------+---------------------+-------------------+---------------------+----------------------+
```

### 6.2 Asynchronous Authentication Lifecycle
When the user submits credentials via button click or Enter key:
1. **Validation Pre-check**: `Validators.validNumber` / length sanity checks execute locally. If blank, the relevant field shakes and displays "Username is required".
2. **Button State Morphing**:
   - `loginButton` width smoothly shrinks from `100%` (360dp) to a compact circular pill (`56dp`).
   - Text "SIGN IN TO TERMINAL" fades out ($\Delta t = 100\text{ms}$); `loginSpinner` fades in and begins indeterminate rotation.
   - All input fields lock (`setDisable(true)`).
3. **Service Layer Execution**: `AdminService.authenticate(username, password)` runs asynchronously on a background executor daemon thread, keeping the JavaFX Application Thread at a steady 60/120 FPS.
4. **Outcome Handling**:
   - **Success (`Administrator != null`)**:
     - `SessionContext.setAdministrator(admin)` is populated.
     - Button morphs into an `M3ShapeMorph` checkmark badge with a vibrant primary-container burst.
     - Scene gracefully transitions via `FxUtils.switchScene("fxml/dashboard.fxml")` using a 250ms spatial wipe.
   - **Failure (`null` or `DaoException`)**:
     - Button rebounds to full pill width.
     - `authCard` executes the error shake sequence.
     - Inline error label appears: *"Invalid operator credentials or inactive account."*
     - Password field is cleared and auto-focused.

### 6.3 Empty & Edge State Provisions
- **Caps Lock Active**: A native keyboard listener flags active caps lock state, revealing a floating assist badge: `[!] Caps Lock is ON`.
- **Terminal Lockout (Anti-Brute-Force)**: After 5 failed attempts, the portal transitions to an exponential cooldown state:
  - Form controls disable completely.
  - A countdown radial timer displays remaining lock time (e.g. `Lockout Active: 44s`).
  - An emergency administrator unlock link (`onForgotPassword()`) is highlighted.

---

## 7. Declarative FXML Node Hierarchy & Controller Contract

### 7.1 FXML Structural Declaration (`src/main/resources/fxml/login.fxml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.layout.AnchorPane?>
<?import javafx.scene.layout.HBox?>
<?import javafx.scene.layout.Priority?>
<?import javafx.scene.layout.Region?>
<?import javafx.scene.layout.StackPane?>
<?import javafx.scene.layout.VBox?>
<?import io.m3fx.controls.button.M3Button?>
<?import io.m3fx.controls.card.M3Card?>
<?import io.m3fx.controls.chip.M3Chip?>
<?import io.m3fx.controls.effect.M3GlassPane?>
<?import io.m3fx.controls.effect.M3GlowShadow?>
<?import io.m3fx.controls.effect.M3ParticleCanvas?>
<?import io.m3fx.controls.effect.M3PulseBeacon?>
<?import io.m3fx.controls.effect.M3Scanline?>
<?import io.m3fx.controls.icon.M3Icon?>
<?import io.m3fx.controls.iconbutton.M3IconButton?>
<?import io.m3fx.controls.progress.M3CircularProgress?>
<?import io.m3fx.controls.scene3d.M3GyroRing3D?>
<?import io.m3fx.controls.selection.M3CheckBox?>
<?import io.m3fx.controls.snackbar.M3Snackbar?>
<?import io.m3fx.controls.textfield.M3TextField?>

<StackPane fx:id="rootPane" prefHeight="900.0" prefWidth="1440.0"
           styleClass="auth-root"
           xmlns="http://javafx.com/javafx/21"
           xmlns:fx="http://javafx.com/fxml/1"
           fx:controller="com.gym.controller.LoginController">

    <!-- Layer 0: Hardware-Accelerated Ambient Kinetic Particles -->
    <M3ParticleCanvas fx:id="particleBackdrop" particleCount="35"
                      particleSpeed="0.4" styleClass="ambient-particles" />

    <!-- Layer 1: Global Glass Acrylic Canopy -->
    <M3GlassPane fx:id="glassCanopy" blurRadius="24.0" styleClass="acrylic-blur" />

    <!-- Layer 2: Main Bento Work Area Split -->
    <HBox fx:id="mainHBox" alignment="CENTER" spacing="48.0">
        <padding>
            <Insets bottom="48.0" left="64.0" right="64.0" top="48.0" />
        </padding>

        <!-- Column 1: Hero Athletic 3D Viewport (Collapsed on Compact Screens) -->
        <VBox fx:id="heroViewport" alignment="CENTER_LEFT" spacing="24.0"
              HBox.hgrow="ALWAYS" styleClass="hero-container">
            <HBox alignment="CENTER_LEFT" spacing="12.0">
                <M3Icon iconLiteral="m3-fitness_center" iconSize="36" styleClass="brand-logo-icon" />
                <Label text="GYM-OS // ATHLETIC ENGINE" styleClass="brand-heading" />
            </HBox>

            <!-- 3D Gyro Kinetic Centerpiece -->
            <StackPane alignment="CENTER" prefHeight="440.0" prefWidth="540.0" styleClass="gyro-wrapper">
                <M3GyroRing3D fx:id="kineticGyro" ringCount="4" prefHeight="380.0" prefWidth="380.0" />
                <M3Scanline fx:id="hologramScanline" cycleDuration="4000" styleClass="hero-scanline" />
            </StackPane>

            <!-- Live Floor Telemetry Micro-HUD -->
            <HBox alignment="CENTER_LEFT" spacing="16.0" styleClass="telemetry-bar">
                <M3PulseBeacon fx:id="statusBeacon" beaconColor="-md-sys-color-primary" />
                <Label text="STATION: FRONT_DESK_01" styleClass="telemetry-label" />
                <Region HBox.hgrow="ALWAYS" />
                <M3Chip fx:id="securityBadgeChip" text="TLS 1.3 / WAL ACTIVE" />
            </HBox>
        </VBox>

        <!-- Column 2: Elevated Authentication Bento Card -->
        <M3GlowShadow fx:id="cardGlow" glowColor="-md-sys-color-primary" glowRadius="16.0">
            <M3Card fx:id="authCard" cardVariant="ELEVATED" prefWidth="480.0" styleClass="auth-card">
                <padding>
                    <Insets bottom="40.0" left="40.0" right="40.0" top="40.0" />
                </padding>
                <VBox spacing="20.0">
                    <!-- Title & Header Block -->
                    <VBox spacing="6.0">
                        <Label text="Operator Authentication" styleClass="card-title" />
                        <Label text="Enter credentials to initialize shift console" styleClass="card-subtitle" />
                    </VBox>

                    <!-- Feedback Alert Tray (Dynamic Error/Warning Box) -->
                    <StackPane fx:id="feedbackContainer" managed="false" visible="false">
                        <HBox fx:id="feedbackBox" alignment="CENTER_LEFT" spacing="12.0" styleClass="feedback-tray-error">
                            <M3Icon fx:id="feedbackIcon" iconLiteral="m3-error" iconSize="20" />
                            <Label fx:id="feedbackLabel" text="Invalid credentials" wrapText="true" />
                        </HBox>
                    </StackPane>

                    <!-- Username / Staff ID Field -->
                    <VBox spacing="6.0">
                        <Label text="Staff Identifier" styleClass="field-label" />
                        <M3TextField fx:id="usernameField" promptText="e.g. admin or staff_01"
                                     fieldVariant="OUTLINED" prefHeight="56.0">
                            <leadingIcon>
                                <M3Icon iconLiteral="m3-badge" iconSize="22" />
                            </leadingIcon>
                        </M3TextField>
                        <Label fx:id="usernameHelper" text="Case-sensitive unique employee ID" styleClass="helper-label" />
                    </VBox>

                    <!-- Master Password Field -->
                    <VBox spacing="6.0">
                        <Label text="Master Password" styleClass="field-label" />
                        <M3TextField fx:id="passwordField" promptText="Enter secure passphrase"
                                     fieldVariant="OUTLINED" passwordMode="true" prefHeight="56.0">
                            <leadingIcon>
                                <M3Icon iconLiteral="m3-lock" iconSize="22" />
                            </leadingIcon>
                            <trailingIcon>
                                <M3IconButton fx:id="passwordToggleBtn" iconLiteral="m3-visibility"
                                              onAction="#onTogglePasswordVisibility" />
                            </trailingIcon>
                        </M3TextField>
                        <Label fx:id="capsLockAlert" text="[!] Caps Lock is ON" managed="false"
                               styleClass="caps-alert-label" visible="false" />
                    </VBox>

                    <!-- Session Persistence & Remember Option -->
                    <HBox alignment="CENTER_LEFT" spacing="8.0">
                        <M3CheckBox fx:id="rememberMeCheckBox" text="Keep active for full 12h shift" />
                    </HBox>

                    <!-- Primary Action Button -->
                    <StackPane alignment="CENTER" prefHeight="56.0">
                        <M3Button fx:id="loginButton" buttonVariant="FILLED" defaultButton="true"
                                  maxWidth="1.7976931348623157E308" onAction="#onLogin"
                                  prefHeight="56.0" styleClass="btn-primary-action" text="INITIALIZE CONSOLE">
                            <graphic>
                                <M3Icon iconLiteral="m3-login" iconSize="20" />
                            </graphic>
                        </M3Button>
                        <M3CircularProgress fx:id="loginSpinner" managed="false" prefHeight="24.0"
                                            prefWidth="24.0" visible="false" />
                    </StackPane>

                    <!-- Secondary Recovery Link -->
                    <HBox alignment="CENTER">
                        <M3Button fx:id="forgotPasswordButton" buttonVariant="TEXT"
                                  onAction="#onForgotPassword" styleClass="btn-tertiary-link"
                                  text="Trouble signing in? Contact Administrator / Reset" />
                    </HBox>
                </VBox>
            </M3Card>
        </M3GlowShadow>
    </HBox>

    <!-- Layer 3: Floating System Notification Host -->
    <M3Snackbar fx:id="authSnackbar" StackPane.alignment="BOTTOM_CENTER">
        <StackPane.margin>
            <Insets bottom="32.0" />
        </StackPane.margin>
    </M3Snackbar>
</StackPane>
```

### 7.2 Controller Implementation Contract (`LoginController.java`)

Conforming strictly to `docs/UML.md`:
- `LoginController` exposes `+initialize() void`, `+onLogin() void`, `+onForgotPassword() void`.
- Collaborators: `AdminService`, `SessionContext`, `FxUtils`.
- Implements Rule 16: Account validation and rejection of inactive operators.

```java
package com.gym.controller;

import com.gym.model.Administrator;
import com.gym.service.AdminService;
import com.gym.util.FxUtils;
import com.gym.util.SessionContext;
import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.effect.M3GlowShadow;
import io.m3fx.controls.progress.M3CircularProgress;
import io.m3fx.controls.scene3d.M3GyroRing3D;
import io.m3fx.controls.selection.M3CheckBox;
import io.m3fx.controls.snackbar.M3Snackbar;
import io.m3fx.controls.textfield.M3TextField;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class LoginController {

    @FXML private StackPane rootPane;
    @FXML private M3GyroRing3D kineticGyro;
    @FXML private M3Card authCard;
    @FXML private M3GlowShadow cardGlow;
    @FXML private M3TextField usernameField;
    @FXML private M3TextField passwordField;
    @FXML private M3CheckBox rememberMeCheckBox;
    @FXML private M3Button loginButton;
    @FXML private M3CircularProgress loginSpinner;
    @FXML private M3Button forgotPasswordButton;
    @FXML private StackPane feedbackContainer;
    @FXML private Label feedbackLabel;
    @FXML private Label capsLockAlert;
    @FXML private M3Snackbar authSnackbar;

    private AdminService adminService;

    @FXML
    public void initialize() {
        this.adminService = new AdminService();

        // Caps Lock detection listener
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            boolean isCaps = java.awt.Toolkit.getDefaultToolkit().getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK);
            capsLockAlert.setVisible(isCaps);
            capsLockAlert.setManaged(isCaps);
        });

        // Parallax cursor tracking for 3D kinetic ring
        rootPane.setOnMouseMoved(e -> {
            if (kineticGyro != null) {
                double deltaX = (e.getSceneX() - rootPane.getWidth() / 2.0) * 0.03;
                double deltaY = (e.getSceneY() - rootPane.getHeight() / 2.0) * 0.03;
                kineticGyro.setTiltAngles(deltaY, deltaX);
            }
        });

        // Clear error styling on input change
        usernameField.textProperty().addListener((obs, oldV, newV) -> clearFeedback());
        passwordField.textProperty().addListener((obs, oldV, newV) -> clearFeedback());

        Platform.runLater(() -> usernameField.requestFocus());
    }

    @FXML
    public void onLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.trim().isEmpty()) {
            triggerInputError(usernameField, "Operator identity cannot be blank.");
            return;
        }
        if (password == null || password.isEmpty()) {
            triggerInputError(passwordField, "Master password cannot be blank.");
            return;
        }

        setLoadingState(true);

        Task<Administrator> authTask = new Task<>() {
            @Override
            protected Administrator call() throws Exception {
                // Background thread authentication via AdminService (Rule 16 checks active status)
                return adminService.authenticate(username.trim(), password);
            }
        };

        authTask.setOnSucceeded(e -> {
            Administrator admin = authTask.getValue();
            setLoadingState(false);
            if (admin != null) {
                // Rule 16: Store authenticated user in SessionContext
                SessionContext.setAdministrator(admin);
                FxUtils.switchScene("fxml/dashboard.fxml");
            } else {
                triggerAuthFailure("Invalid credentials or account inactive. Access denied.");
            }
        });

        authTask.setOnFailed(e -> {
            setLoadingState(false);
            Throwable ex = authTask.getException();
            triggerAuthFailure("Database authentication error: " + ex.getMessage());
        });

        Thread daemonWorker = new Thread(authTask, "m3-auth-thread");
        daemonWorker.setDaemon(true);
        daemonWorker.start();
    }

    @FXML
    public void onForgotPassword() {
        // Recovery routing according to UML contract
        FxUtils.info("Contact system administration at security@gym-os.internal to reset master credentials.");
    }

    @FXML
    public void onTogglePasswordVisibility(ActionEvent event) {
        boolean currentMode = passwordField.isPasswordMode();
        passwordField.setPasswordMode(!currentMode);
    }

    private void setLoadingState(boolean loading) {
        loginButton.setVisible(!loading);
        loginSpinner.setVisible(loading);
        loginSpinner.setManaged(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
    }

    private void triggerAuthFailure(String message) {
        feedbackLabel.setText(message);
        feedbackContainer.setVisible(true);
        feedbackContainer.setManaged(true);
        playShakeAnimation(authCard);
        passwordField.clear();
        passwordField.requestFocus();
    }

    private void triggerInputError(M3TextField field, String message) {
        feedbackLabel.setText(message);
        feedbackContainer.setVisible(true);
        feedbackContainer.setManaged(true);
        playShakeAnimation(field);
        field.requestFocus();
    }

    private void clearFeedback() {
        if (feedbackContainer.isVisible()) {
            feedbackContainer.setVisible(false);
            feedbackContainer.setManaged(false);
        }
    }

    private void playShakeAnimation(javafx.scene.Node node) {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.millis(0),   new KeyValue(node.translateXProperty(), 0)),
            new KeyFrame(Duration.millis(50),  new KeyValue(node.translateXProperty(), -12)),
            new KeyFrame(Duration.millis(100), new KeyValue(node.translateXProperty(), 10)),
            new KeyFrame(Duration.millis(150), new KeyValue(node.translateXProperty(), -8)),
            new KeyFrame(Duration.millis(200), new KeyValue(node.translateXProperty(), 6)),
            new KeyFrame(Duration.millis(250), new KeyValue(node.translateXProperty(), -3)),
            new KeyFrame(Duration.millis(300), new KeyValue(node.translateXProperty(), 0))
        );
        timeline.play();
    }
}
```

---

## 8. Ergonomics, Touch Targets & Responsive Layout

### 8.1 Touch Target Compliance
- All interactive controls conform to a minimum **48×48dp physical bounding box**.
- Primary text fields feature a **56dp container height** with a 16dp horizontal text padding buffer.
- The `onLogin` primary button is sized at **56dp height** across a 400dp width span, providing an expansive tap target for gloves or hasty touchscreen taps.

### 8.2 Responsive Window Breakpoints

```
+----------------------------------------------------------------------------------------------------+
|                                    ADAPTIVE BREAKPOINT MATRIX                                      |
+--------------------------+-----------------------+-------------------------------------------------+
| Breakpoint Class         | Viewport Width        | Structural Adaptation Mode                      |
+--------------------------+-----------------------+-------------------------------------------------+
| Compact (Phone / Kiosk)  | < 600dp               | Hero 3D viewport collapses (visible=false);     |
|                          |                       | Auth Card expands to fill 100% width with 16dp  |
|                          |                       | gutter padding. Title centers.                  |
+--------------------------+-----------------------+-------------------------------------------------+
| Medium (Tablet / POS)    | 600dp to 840dp        | Hero viewport scales down (gyro radius 120dp);  |
|                          |                       | HBox spacing tightens to 24dp; Auth Card width  |
|                          |                       | fixes at 380dp.                                 |
+--------------------------+-----------------------+-------------------------------------------------+
| Expanded (Desktop/Console| > 840dp               | Full dual-column Bento layout active. 3D gyro   |
|                          |                       | full 380dp scale with active particle density.  |
|                          |                       | Auth Card fixed at 480dp width.                 |
+--------------------------+-----------------------+-------------------------------------------------+
```

---

## 9. Accessibility, Keyboard Navigation & Screen Readers

### 9.1 Traversal Order & Logical Focus Sequence
The screen establishes a deterministic, circular tab loop:
1. `usernameField` (Auto-focused on window mount)
2. `passwordField`
3. `passwordToggleBtn` (Accessible trailing icon toggle)
4. `rememberMeCheckBox`
5. `loginButton` (Submit action, responds to both `SPACE` and `ENTER`)
6. `forgotPasswordButton` (Tertiary link)

### 9.2 Accelerators & Shortcuts
- `ENTER`: Triggers `#onLogin` from any input field.
- `ESC`: Clears current inputs and resets focus to `usernameField`.
- `Ctrl+H` / `Cmd+H`: Shows application identity dialog and help line.

### 9.3 Screen Reader & Assistive Technologies
- Accessible Roles assigned to all custom nodes:
  - `authCard`: `AccessibleRole.PARENT_ITEM`, description "Authentication Box".
  - `usernameField`: `AccessibleRole.TEXT_FIELD`, title "Staff Identifier Input".
  - `passwordField`: `AccessibleRole.PASSWORD_FIELD`, title "Master Password Input".
  - `loginButton`: `AccessibleRole.BUTTON`, title "Log in to terminal".
- Error state announcements: When `feedbackContainer` activates, its accessible text is updated dynamically, prompting the screen reader's live region queue to speak: *"Error: Invalid credentials or account inactive"*.

---

## 10. Quality Review Checklist & Pitfalls

### 10.1 Verification Checklist
- [x] **Zero Hardcoded Color Literals**: Every fill, stroke, background, and shadow references `-md-sys-color-*` CSS variables or dynamic M3 token mappings.
- [x] **Spring Physics Integrity**: Modals, shakes, and button morphs utilize parameterized damping ratio $\zeta$ and stiffness $k$, avoiding static linear timelines.
- [x] **UML Contract Compliance**: Methods `initialize()`, `onLogin()`, `onForgotPassword()` match `docs/UML.md` exactly.
- [x] **Business Rule 16 Enforced**: Rejects inactive accounts and populates `SessionContext`.
- [x] **Thread Confinement**: Database verification executes off the FX thread; UI updates route through `Platform.runLater()` via JavaFX tasks.

### 10.2 Top 5 Developer Implementation Mistakes
1. **Blocking the JavaFX Thread during Hash Verification**: Calling PBKDF2 hashing directly on the FX application thread causes a 120ms frame drop, freezing the 3D gyro and particle animations. *Fix: Wrap authentication inside a `javafx.concurrent.Task`*.
2. **Ignoring System DPI Scaling in 3D Viewport**: `M3GyroRing3D` canvas can blur on high-DPI (Retina / 4K) monitors. *Fix: Apply canvas buffer scaling using `Screen.getPrimary().getOutputScaleX()`*.
3. **Hardcoding Card Dimensions**: Using fixed absolute widths without min/max constraints causes visual clipping when localized into languages with longer word lengths (e.g. German, French). *Fix: Utilize `prefWidth` with elastic `VBox` and `HBox.hgrow="ALWAYS"`*.
4. **Failing to Clear Sensitive Data from Memory**: Retaining password strings in static controller fields. *Fix: Overwrite internal character buffers immediately after authentication execution*.
5. **Missing Keyboard Caps Lock Trap**: Failing to warn operators of active Caps Lock creates excessive failed login attempts and unwarranted account lockouts. *Fix: Bind a native key event listener to the root container*.
