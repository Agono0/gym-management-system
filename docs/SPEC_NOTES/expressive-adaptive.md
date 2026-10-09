# SPEC_NOTES — Expressive + adaptive gap closures

Sources:
- https://m3.material.io/components/floating-action-button/overview (FAB menu guidance)
- https://m3.material.io/components/progress-indicators/overview (wavy expressive indicator)
- https://m3.material.io/foundations/layout/applying-layout/window-size-classes
- https://m3.material.io/components/date-pickers/overview (calendar grid)
- https://m3.material.io/components/search/overview (full-screen/scrim search)

## FAB menu (M3FabMenu)
- Main FAB 56px primary-container; open actions are small 40px FABs in surface-container-high
  with 14px labels; main icon rotates 45°; rows stagger 30ms with emphasized easing; Escape
  collapses; action click runs handler then collapses.

## Wavy progress (M3WavyProgress)
- 16px tall canvas: 4dp rounded track (surface-container-highest) + primary sine wave
  (wavelength 28px, amplitude 5px, 3px round-cap stroke) travelling ~60fps; determinate clips the
  wave to the progress fraction; indeterminate spans full width.

## Springs (SpringAnimation)
- Semi-implicit Euler, mass 1: DEFAULT k=300 ζ=0.8 (mild overshoot), GENTLE k=150 ζ=1.0 (none),
  BOUNCY k=600 ζ=0.45 (playful). Settles to epsilon = max(1e-4, |target|·5e-4).

## Adaptive (WindowSizeClass + M3AdaptiveScaffold)
- Width: compact <600, medium 600-840, expanded ≥840. Height: compact <480, medium 480-900,
  expanded ≥900 (m3.material.io window size classes).
- Scaffold: compact → bottom NavigationBar; medium → NavigationRail; expanded → standard
  NavigationDrawer; selection synced; top bar + content caller-supplied.

## Date dialog (M3Calendar + M3DateDialog)
- Calendar: 320px min width, month header (full month + year, prev/next 40px buttons),
  locale-first weekday row (narrow names), 6×7 day grid of 40px circular toggles, adjacent-month
  days at 38% opacity, selected day primary circle; dialog wraps it with Cancel/OK text buttons
  in the standard 28px-radius container (min 360px wide).

## Search overlay mode
- Same suggestion panel, two hosts: popup (default) or overlay anchored under the bar at anchor
  width with a dismissing scrim (m3.material.io search scrim behavior).

## Text field focus (2px active indicator)
- `input-focused` pseudo-class set from the inner field's focus: filled → 2px primary bottom
  indicator; outlined → 2px primary border. (Closes the 1px gap noted in the first pass.)

## Slider stops
- Discrete factory enables tick marks styled in on-surface-variant (approximation of spec stop
  dots within native JavaFX Slider).
