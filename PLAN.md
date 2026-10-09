# PLAN.md: m3fx roadmap

## Reality check

"Full M3 with all features" is a multi-year, multi-engineer scope (Google's own Compose M3 has dozens of
components, adaptive layouts, and spring-motion APIs). A realistic solo/college target is a **well-built
subset** with an architecture that makes adding the rest easy.

Also note: `material-web` is in maintenance mode and does not implement M3 Expressive. Use it for the
baseline M3 numbers only. For Expressive and newer components, use m3.material.io and the
androidx.compose.material3 source. Verify the current state of the spec before each phase, since it keeps changing.

## Scope tiers

| Tier | Meaning | Goal |
|---|---|---|
| **T1: Core** | Foundation + most-used components | Must finish (the graded deliverable) |
| **T2: Extended** | Navigation, overlays, richer inputs | Finish if time allows |
| **T3: Expressive + advanced** | Spring motion, shape morphing, adaptive layout, new components | Stretch / future work |

---

## Phase 0: Setup (2-3 days)

- Create repo, Maven multi-module (`core`, `controls`, `demo`), parent POM, `module-info.java`
- Put reference repos in `refs/` (read-only, add to `.gitignore` or use git submodules)
- Add formatter (spotless), JUnit 5, TestFX, CI (GitHub Actions: `mvn verify`)
- Copy `AGENTS.md`, `PLAN.md`; create `docs/COVERAGE.md` from the checklist below
- Demo app skeleton with a sidebar listing component pages and a light/dark toggle
- **Exit:** `mvn verify` green, demo window opens

## Phase 1: Foundation: m3fx-core (1 week)

1. Port `material-color-utilities` Java into `core/color` (HCT, TonalPalette, schemes, DynamicColor)
2. `M3Theme.fromSeed(seed, variant, dark, contrast)` with variants: TonalSpot, Vibrant, Expressive, Neutral,
   Monochrome, Fidelity, Content, Rainbow, FruitSalad
3. All color roles exposed + `toCss()` generating `-md-sys-color-*` looked-up colors
4. Tokens: `ShapeTokens` (none to full, plus Expressive larger sizes), `TypeScale` (15 roles, plus emphasized
   variants), `ElevationLevel` 0-5, `StateOpacity`, `MotionTokens`
5. Unit tests: known seed gives known hex values (compare against the Java utils' test data / m3.material.io theme builder)
6. **Exit:** demo shows a swatch page of every color role, type scale page, shape page, elevation page; seed picker changes everything live

## Phase 2: Shared infrastructure: controls/internal (3-4 days)

- `StateLayer` (hover/focus/pressed/dragged overlays)
- `Ripple` (clipped, animated, themeable)
- `ElevationShadow` (level to `DropShadow`, animates between levels)
- `M3Fonts.load()` (Roboto), `M3Icon` (Ikonli + SVG path support)
- `M3Stylesheets` helper: apply theme + component CSS to a Scene with one call
- **Exit:** a test node using all four helpers in the demo

## Phase 3: T1 components (3-4 weeks, one at a time)

Order matters; each builds on shared helpers. Do spec notes first, demo page last.

| # | Component | Variants / notes |
|---|---|---|
| 1 | Button | filled, tonal, outlined, elevated, text; icon + label; toggle; Expressive shapes/sizes |
| 2 | Icon button | standard, filled, tonal, outlined; toggle |
| 3 | FAB | FAB, small, large, extended; (Expressive: FAB menu if time) |
| 4 | Card | elevated, filled, outlined; clickable/draggable states |
| 5 | Text field | filled + outlined, floating label, supporting text, leading/trailing icon, error, counter |
| 6 | Checkbox / Radio / Switch | all states; switch with icon thumb |
| 7 | Chips | assist, filter, input, suggestion |
| 8 | Divider, Badge, Tooltip | small but frequently used |
| 9 | Progress indicators | linear + circular, determinate/indeterminate (Expressive wavy if time) |
| 10 | Slider | continuous, discrete, range |

**Exit T1:** demo gallery with all above, light/dark, switchable seed; COVERAGE.md updated; README with screenshots

## Phase 4: T2 components (3-4 weeks)

| # | Component | Notes |
|---|---|---|
| 11 | Top app bar | small, center-aligned, medium, large (collapse on scroll) |
| 12 | Navigation bar + rail + drawer | modal and standard drawer |
| 13 | Tabs | primary, secondary |
| 14 | Dialog | basic + fullscreen; scrim; uses a custom overlay layer, not `Stage` |
| 15 | Snackbar | queue + action; overlay layer |
| 16 | Menu / dropdown (select) | uses `Popup`; keyboard nav |
| 17 | List item | one/two/three line, leading/trailing content |
| 18 | Bottom sheet / side sheet | modal + standard |
| 19 | Date picker / time picker | the heaviest; consider last or skip |
| 20 | Search bar | with suggestions view |
| 21 | Segmented button, button groups | includes Expressive button group |

Shared prerequisite: an **`M3Overlay`/`OverlayLayer`** (StackPane-based) for dialog, snackbar, sheets, menus.
Build it before #14.

## Phase 5: T3: Expressive + advanced (stretch)

- `SpringInterpolator` and spring-based press/size animations
- Shape morphing (button shape change on press, morphing indicators)
- Wavy progress indicators, loading indicator, split button, FAB menu, toolbar
- Adaptive layout: window size classes (compact/medium/expanded) + scaffold (nav rail/bar switching, panes)
- Carousel, Expressive lists/menus/search per the latest guidance (check m3.material.io first)

## Phase 6: Polish and release (1-2 weeks)

- README with quick start, screenshots/GIFs, supported-components table
- Javadoc site, `COVERAGE.md` finalized
- Publish via JitPack (tag `v0.1.0`) or Maven Central
- Accessibility pass (keyboard, focus, contrast levels)
- Performance pass (many controls, no leaks, listeners removed in `dispose()`)
- Final demo + presentation notes

---

## Suggested timeline (about 4 months part-time)

| Weeks | Phase |
|---|---|
| 1 | 0 + 1 |
| 2 | 2 |
| 3-6 | 3 (T1) |
| 7-10 | 4 (T2, pick what fits) |
| 11-13 | 5 (pick 2-3 Expressive items) |
| 14-16 | 6 + buffer |

If the deadline is shorter, ship T1 + a few T2 items and list the rest as "planned" in COVERAGE.md.

---

## Prompt templates for the agent

**Phase 1 start**
> Read AGENTS.md and PLAN.md. Do Phase 1 step 1 only: port the Java sources from refs/material-color-utilities/java into m3fx-core/color, keep license headers, fix package names, make it compile, and add a test that a seed of #6750A4 reproduces the baseline M3 light primary (#6750A4) and primary container (#EADDFF). Summarize what you did.

**Component start (template)**
> Read AGENTS.md. Implement **<Component>** (Phase <n>, item <#>).
> First write docs/SPEC_NOTES/<component>.md with dimensions, shapes, state opacities, colors, and type roles, citing the source for each (m3.material.io, material-web tokens, or compose source).
> Then implement control, skin, and CSS using shared helpers, add a demo page showing all variants and states in light and dark, add tests, run `mvn verify`, update COVERAGE.md, and explain the classes to me in simple terms.

**Review pass**
> Review <Component> against its SPEC_NOTES. List every deviation, hardcoded value, missing state, or accessibility gap. Do not fix anything yet.

---

## COVERAGE.md starter checklist

Copy this into `docs/COVERAGE.md` and tick as you go.

**Systems:** [ ] Color/dynamic color  [ ] Dark theme  [ ] Contrast levels  [ ] Typography  [ ] Shape  [ ] Elevation
[ ] State layers  [ ] Ripple  [ ] Motion (easing)  [ ] Motion (spring)  [ ] Icons  [ ] Adaptive layout

**T1:** [ ] Button  [ ] Icon button  [ ] FAB  [ ] Card  [ ] Text field  [ ] Checkbox  [ ] Radio  [ ] Switch  [ ] Chips
[ ] Divider  [ ] Badge  [ ] Tooltip  [ ] Progress  [ ] Slider

**T2:** [ ] Top app bar  [ ] Navigation bar  [ ] Navigation rail  [ ] Navigation drawer  [ ] Tabs  [ ] Dialog
[ ] Snackbar  [ ] Menu  [ ] Select  [ ] List  [ ] Bottom sheet  [ ] Side sheet  [ ] Date picker  [ ] Time picker
[ ] Search  [ ] Segmented button  [ ] Button group

**T3:** [ ] Wavy progress  [ ] Loading indicator  [ ] Split button  [ ] FAB menu  [ ] Toolbar  [ ] Carousel
[ ] Shape morphing  [ ] Window size classes + scaffold
