# DECISIONS.md — design decisions and why

1. **Gradle multi-module, not Maven.** AGENTS.md describes a Maven layout, but this repo already
   builds with Gradle (Java 27 + JavaFX plugin). The library ships as two Gradle subprojects
   (`m3fx-core`, `m3fx-controls`) producing plain jars — reusable from any Gradle/Maven project
   via `implementation(project(':m3fx-core'))` or a published jar. Same package/module split as
   AGENTS.md specifies.
2. **Java 21 language level on JDK 27.** Only JDK 27 is installed; modules compile with
   `--release 21` semantics (source level 21) so the jars run on Java 21+.
3. **material-color-utilities ported verbatim.** Only package names changed
   (`io.m3fx.core.color.*`); `androidx`/`errorprone` annotations stripped (desktop has no androidx).
   Algorithms untouched per AGENTS.md.
4. **Theme via temp-file stylesheet, not data-URL.** `M3Stylesheets.applyTo(scene, theme)` writes
   `M3Theme.toCss()` to a temp file and puts it before `m3fx.css` in the scene stylesheets, so
   `-md-sys-color-*` lookups resolve for every control. Switching theme = one call.
5. **Extend standard JavaFX controls instead of custom Control+Skin everywhere.** Only `M3Card`,
   `M3Switch`, `M3LinearProgress` use custom skins; the rest extend Button/ToggleButton/CheckBox/
   TabPane/etc. with variant properties + CSS. Same visuals, far less code, native keyboard and
   accessibility behavior preserved.
6. **No Ikonli dependency yet.** AGENTS.md names Ikonli but also forbids new runtime deps without
   asking. `M3Icon` renders glyph text or SVG paths with zero deps; `m3-icon-glyph` CSS class is
   the hook for an icon font later.
7. **Date picker is styled-input first.** Full calendar-grid dialog skin is the heaviest T2 item;
   `M3DatePicker` ships M3 field styling now, dialog tokens documented in SPEC_NOTES for the next pass.
8. **OverlayLayer instead of extra Stages.** Dialog/snackbar/sheets render in a scene-root
   StackPane with a scrim — simpler focus handling and testable without window management.
9. **Icons: bundled SVG catalog instead of Ikonli.** Ikonli would add a runtime dependency
   (needs approval). `io.m3fx.controls.icon` is now public API: `M3Icons` ships 26 Material
   Symbols paths (Google, Apache 2.0, see NOTICE) and `M3Icon.symbol(name)` renders them as
   vector `SVGPath` nodes tinted via `-fx-fill` CSS hooks. No ligature fonts, no new deps.
10. **Button toggle = secondary-container selected state.** `M3Button` moved from `Button` to
    `ToggleButton` base with a `toggleable` flag (same pattern as `M3IconButton`); internal reset
    uses `addEventHandler(ACTION)` so caller `setOnAction` handlers compose instead of being
    overwritten.
11. **Real springs via semi-implicit Euler.** `SpringAnimation` integrates
    `a = -k(x-t) - c·v` per frame (mass 1, `c = 2ζ√k`) with DEFAULT/GENTLE/BOUNCY presets; the
    pure `step()` function is unit-tested (settle/overshoot), the `AnimationTimer` driver is thin.
12. **Canvas skins read theme via probe region.** `ThemeColors.resolve()` styles a 1px probe with
    the looked-up variable and reads back the fill (baseline-theme fallback off-scene). Used by
    the wavy indicator; probe once and cache, never per frame.
13. **Adaptive scaffold owns its nav bindings.** `M3AdaptiveScaffold` unbinds the previous nav
    control before rebinding on size-class change — rebinding without unbinding throws.
14. **Search has two suggestion modes.** Popup by default (zero setup); `setOverlay(layer)` moves
    the same suggestion panel into the overlay anchored under the bar with a dismissing scrim
    (`OverlayLayer.showAnchored`). The panel reparents between the two hosts as needed.
15. **Buttons are real Control+Skin (not restyled stock buttons).** `M3Button`/`M3IconButton`
    extend a shared `PressableControl` (Toggle impl, press-intent tracking, Space/Enter) with skins
    layering background + CSS state region + content + clipped ripple pane. Ripple ink and shadow
    colors resolve live via `ThemeColors` so dark themes ripple correctly; elevation animates
    (filled 0→1, elevated 1→2→1 on hover/press). Breaking change from the 0.1.0 ToggleButton base,
    acceptable pre-release.
16. **State colors are layered translucent theme vars, not opacity hacks.** `M3Theme.toCss()`
    emits `-a08/-a10/-a12/-a16/-a38` for every role; components layer e.g.
    `on-primary at 8% over primary` in a single `-fx-background-color` list. Nothing fades whole
    controls anymore (text stays crisp), and dark/high-contrast themes compute correct alphas.
17. **No competing transitions.** `StateLayer` keeps one fade per overlay (stops the running one),
    `MotionUtil` press-scale stops its predecessor, the wavy indicator runs on `AnimationTimer`,
    and the switch thumb rides the real spring — this pass targeted animation stacking as the
    main smoothness fix.
18. **Native date popup styled, not replaced.** The JavaFX `DatePicker` calendar popup is fully
    covered with M3 dialog tokens (surface-container-high, primary selected circle, primary-ring
    today, dimmed adjacent months) — full behavior (year/month spinners, keyboard) for free.
19. **Overlays size to content, sheets dock.** `M3Dialog`/`M3BottomSheet` cap `maxHeight` to pref
    (VBox otherwise fills the overlay); `OverlayLayer.showSheet` docks sheets BOTTOM_CENTER with a
    scrim while dialogs stay centered; row-straddling inputs (`M3DatePicker`, `M3TimePicker`,
    `M3Select`) cap `maxHeight` so HBox siblings don't stretch them.
20. **FAB menu is a right column, not a full-width row.** `M3FabMenu` hugs content width with
    right-aligned rows so action FABs stack above the main FAB and rise from it.
21. **Galleries dogfood the library for dark mode.** The theme engine was verified (no hardcoded
    hex in component CSS; dark surface/primary/container-order locked by `M3ThemeTest`), but the
    demo chrome itself used modena `ListView`/`ComboBox`/`ToggleButton`/`TabPane` which stay light.
    Both galleries now use drawer/select/toggle-button/M3Tabs chrome plus `m3-section-title` text
    so dark mode is correct end to end.
22. **Nav destinations carry icon suppliers, not nodes.** A node can have only one parent, so one
    destination list shared by bar+rail+drawer (or scaffold rebuilds) stole icons from earlier
    builds. `M3NavDestination.of(label, iconName)` creates fresh `M3Icon`s per build and swaps the
    selected icon automatically.
23. **Ripple paints under content.** Skin order is background → ripple → content → state layer, so
    the expanding ink covers the container, never the text.
24. **Modena is neutralized explicitly.** Stock bases (`ToggleButton`, `Button`, `TextField`,
    `ComboBox`) leak a light-blue focus glow and square insets for any property our CSS doesn't
    set — invisible in light mode, glaring in dark. A dedicated CSS block zeroes focus colors and
    background insets on all M3 stock-based controls; popup list cells and spinner editors get
    theme-aware rules too.
25. **FAB menu anchors the main FAB.** The menu is a `StackPane` with an always-managed (but
    hidden when closed) action column, so its width never changes and the main FAB never shifts;
    rows rise with a wider stagger and decelerate easing.
26. **State rules must be complete, not just present.** Modena's `:hover`/`:armed`/`:selected`
    rules carry equal-or-higher specificity than single-class resets, so any state where we don't
    declare `background-color` + `background-insets` leaks a default layer (this was the
    segmented-selected partial fill and the gray FAB hover). A trailing "state completeness"
    block re-zeroes insets/focus in every state and declares every background explicitly,
    including `:selected:pressed` combos.
27. **Date/time inputs match filled text fields.** `M3DatePicker` gets the same 1px indicator
    (2px primary via an `input-focused` pseudo driven from editor focus) and the time spinners
    become 48px filled fields with themed editors/arrows instead of stock boxes.
