# SPEC_NOTES — M3 Expressive Components & Motion Physics

Sources:
- https://m3.material.io/components/buttons/overview (Split buttons, expressive shapes)
- https://m3.material.io/components/toolbars/overview (Docked & Floating toolbars)
- https://m3.material.io/components/carousel/overview (Multi-browse & Hero carousels)
- https://m3.material.io/components/progress-indicators/overview (Circular progress arc sweep)
- https://m3.material.io/styles/motion/overview (Spring tokens, damping ratio & stiffness)

---

## 1. Split Button (M3SplitButton)
- **Anatomy**: Primary action button joined with a trailing dropdown menu button separated by a 1px vertical divider (`-md-sys-color-outline-variant`).
- **Variants**:
  - `FILLED`: Container `primary`, on-container `on-primary`.
  - `TONAL`: Container `secondary-container`, on-container `on-secondary-container`.
  - `OUTLINED`: 1px outline border, transparent container.
  - `ELEVATED`: Container `surface-container-low` with level 1 elevation.
- **Sizes**:
  - `SMALL`: 36dp container height, 18dp icon.
  - `MEDIUM` (Default): 40dp container height, 20dp icon.
  - `LARGE`: 48dp container height, 24dp icon.
- **Shapes**: Leading button rounded left corners (20dp/24dp) and flat/tight right corners; trailing button flat/tight left corners and rounded right corners (20dp/24dp).
- **Motion**: Menu button arrow rotates 180° with `SpringAnimation.BOUNCY` on open and returns to 0° on menu close.

---

## 2. Toolbar (M3Toolbar)
- **Variants**:
  - `FLOATING`: Floats above body content, horizontal or vertical layout, 28dp pill radius, level 2 elevation shadow.
  - `DOCKED`: Spans window edge or docked to screen bottom, 0dp radius, level 0 elevation, 1px outline border.
- **Color Schemes**:
  - `STANDARD`: `surface-container` background with on-surface controls.
  - `VIBRANT`: `primary-container` high emphasis background with on-primary-container controls.

---

## 3. Carousel (M3Carousel)
- **Layouts**:
  - `MULTI_BROWSE`: At least one large focal item, medium items, and small edge peek items.
  - `HERO`: Highlights a primary focal card flanked by smaller items.
- **Shapes & Clipping**: Items styled with 16dp to 28dp corner radii (`ShapeTokens.LARGE` / `EXTRA_LARGE`), track clipped to viewport.
- **Navigation & Physics**: Interactive drag/swipe and previous/next arrow icon buttons with `SpringAnimation.DEFAULT` snapping.

---

## 4. Circular Progress Indicator (M3CircularProgress)
- **Dimensions**: 48x48dp standard bounding box (24x24dp compact), 4dp stroke thickness with `StrokeLineCap.ROUND`.
- **Colors**: Active arc `primary`, background track ring `surface-container-highest`.
- **Indeterminate Sweep Physics**: Base continuous rotation (~260°/sec) coupled with oscillating sweep angle expansion and contraction between 25° and 270°, matching Android M3 `CircularProgressIndicator`.
- **Determinate**: 0.0 to 1.0 progress fraction sweeping from top (-90°).

---

## 5. M3 Expressive Motion Physics
- **Mass-Spring-Damper Model**: Integrates $a = -k(x - \text{target}) - c \cdot v$ with $c = 2\zeta\sqrt{k}$.
- **Presets**:
  - Spatial Expressive Default: $k = 380, \zeta = 0.65$ (natural energetic overshoot)
  - Spatial Expressive Bouncy: $k = 500, \zeta = 0.45$ (playful bounce)
  - Spatial Expressive Fast: $k = 800, \zeta = 0.70$ (quick micro-snap)
  - Spatial Expressive Slow: $k = 200, \zeta = 0.60$ (hero transitions)
  - Spatial Standard: $k = 350, \zeta = 0.90$ (functional smooth settling)
  - Effects (Fade/Color): $k = 300, \zeta = 1.00$ (critically damped, zero bounce)
- **Velocity Preservation**: Preserves velocity $v$ across interrupts and rapid retargets.

