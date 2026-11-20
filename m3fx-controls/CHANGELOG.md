# m3fx-controls changelog

Keep-a-Changelog-style notes for the component layer. `m3fx-core` releases in lockstep; see
`../m3fx-core/CHANGELOG.md` for the token/theme API changes of the same release.

## 0.1.1 — 2026-11-21

First tagged controls release. Everything below landed after the initial 0.1.0 snapshot; the
gym app dashboard on this version is rendered entirely from these components.

### Added

- **Icon catalog grew from 37 to 57 glyphs** — every Material Symbols path fetched verbatim
  from google/material-design-icons (Apache 2.0): `pan_tool`, `play_arrow`, `water_drop`,
  `3d_rotation`, `visibility`, `visibility_off`, `lock`, `pause`, `stop`, `stop_circle`,
  `timer`, `monitor_heart`, `monitor_weight`, `scale`, `directions_run`,
  `sports_gymnastics`, `logout`, `help`, `fitness_center`, plus a documented `fitness`
  alias of `fitness_center` for call sites that request it. Unknown names still render the
  placeholder ring and warn once per distinct name.
- **Charts repaint on theme changes.** All ten canvas charts (bar, spline, scatter, donut,
  gauge, radar, funnel, heatmap, sparkline, activity calendar) now follow the scene's
  `m3fx.theme` property, so switching dark/light recolors them immediately.
- **Reduced motion and the motion-scheme switch are honored by the controls internals**
  (motion utilities, springs, state layers): with `m3fx.reducedMotion` on, springs snap and
  effects stay at or under 150 ms; `MotionScheme.EXPRESSIVE/STANDARD` selects the preset
  family.
- **Stat card header policy**: the title keeps a sane minimum width and grows; the delta
  badge keeps its full-text width and hides whole (with tooltips) when it cannot fit — no
  more mid-word slivers like "TOTAL A…".
- **Auto-tooltips** on icon-only affordances (icon buttons, snackbar close, table
  pagination), and the icon button's accessible text is derived from its graphic.
- **Carousel keyboard navigation** and chip Space/Enter activation (toggle semantics for
  filter chips).
- **Top app bar announces itself as a toolbar** with a live summary line for screen readers.
- **Gym floor map layer API** for the equipment/floor-map screens.

### Changed

- **Text field resting label sits inside the container** (filled and outlined): centered at
  16px when empty and unfocused, floating to the container's top at 12px primary on focus or
  non-empty text. The input content is wrapped in a container pane — selectors targeting
  `.m3-text-field-box` now hit that pane, and the label's position is driven by the
  `:floated` pseudo-class plus code (Node translateY is not CSS-styleable).
- **Stat card delta badge may hide instead of truncate** on narrow cards; its full text
  stays reachable through tooltips (badge and title).
- **Data table search is debounced** (200 ms): typing filters at most once per quiet period;
  Enter applies immediately; sort and pagination behavior is unchanged.
- **FAB radii follow the spec** — the large shape (16dp) for small, medium, large and
  extended FABs alike (`md.comp.fab.*.shape`).
- **Token hygiene**: button/icon-button ripple clips derive from the actual control height
  (20px at the 40dp container, matching the CSS), the card ripple clip uses the medium
  shape (12dp) via `ShapeTokens`, FAB/app-bar/switch dimensions are named spec constants,
  `.side-nav-rail` uses a legal font weight (400), and `.headline-large/medium/small` type
  classes were added to `m3fx.css`.
- `MotionTokens.spring(...)` presets and `SpringInterpolator.DEFAULT` are deprecated in
  favor of `SpringToken.toInterpolator()` and the motion-scheme presets (see m3fx-core).

### Fixed

- **Glass panes no longer paint ghost text.** The backdrop capture photographed the whole
  ancestor flat, baking sibling UI above the pane (app-bar titles, overlay labels,
  scanline overlays) into the refracted image; the capture now hides content that paints
  above the pane and adjacent pad-only UI, runs after layout settles, and measures the pane
  by its layout bounds rather than its effect-inflated bounds.
- **Linear progress indicators actually animate**: the indeterminate timeline used
  `Interpolator.SPLINE` with out-of-range material-web control points, which threw
  `IllegalArgumentException` on every build; a CSS-semantics bezier easing (`OvershootSpline`)
  now drives the verbatim keyframe fractions. The loop is also created once and survives
  resizes instead of being torn down per width change.
- **Skin lifecycle leaks**: switch skins removed their event handlers on dispose (a skin
  swap used to double-toggle), the linear-progress scene listener is detached, the glass
  physics `AnimationTimer` only runs while the pane is shown and visible (hidden panes cost
  zero pulses), and dispose paths are idempotent.
- **Overlay correctness**: anchored panels reposition when the window moves or resizes,
  `M3Menu.showBelow` no longer falls back to the screen origin, and a hovered snackbar can
  no longer pause indefinitely.
- **M3Icon.symbol publishes the name on `symbolNameProperty`** (property-based consumers
  saw null before); raw `ofSvg`/`ofGlyph` semantics are unchanged.
- **Theme temp CSS files are tracked and deleted** with the scene, `ThemeColors`'s
  dark-baseline fallback is fixed, and `ElevationLevel.of()` no longer clones its value
  array per call.

### Migration notes

- **Icon button accessible text is derived from the graphic** — if you set custom
  accessible text on an icon button, verify it still announces what you expect.
- **`M3Button` accessibleText is bound** to the button text; overriding it on a text button
  will fight the binding — set a graphic-only button's text instead.
- **Text field label geometry changed** (label inside the container at rest, floating up on
  focus/text). Callers that positioned nodes relative to the old above-box label, or that
  read `getChildren()` indexes of an `M3TextField`, must adapt; the input content is wrapped
  in a container pane.
- **Stat card badges hide instead of truncating** on narrow cards — read the delta from the
  title tooltip, or shorten the delta text.
- **Data table search is debounced by 200 ms** — programmatic `setQuery` changes also
  debounce; call Enter semantics (focus the field and press Enter) for an immediate apply,
  or wait past the quiet period before asserting on filtered rows in tests.
- **Charts repaint on theme changes** — custom code that manually redrew charts after a
  theme switch can drop that workaround.
