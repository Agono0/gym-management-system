# SPEC_NOTES — divider, badge, tooltip, progress, slider

Sources:
- https://m3.material.io/components/divider/overview (`_md-comp-divider.scss`)
- https://m3.material.io/components/badges/overview (`_md-comp-badge.scss`)
- https://m3.material.io/components/tooltips/overview
- https://m3.material.io/components/progress-indicators/overview
  (`_md-comp-circular-progress-indicator.scss`, `_md-comp-linear-progress-indicator.scss`)
- https://m3.material.io/components/sliders/overview (`_md-comp-slider.scss`)

## Divider — 1px, outline-variant; optional start/end insets.
## Badge — error container, on-error label-small (11px); number badge 16px height (min-width 16,
  999+ cap); dot badge 6px circle.
## Tooltip — plain: inverse-surface container, inverse-on-surface body-small... supporting text
  12px... implemented 12px; rich: surface-container, title title-small + body body-medium + action.
  Show delay ~500ms.
## Progress — linear track 4px, surface-container-highest, 4px radius; active indicator primary
  full-width rounded; stop indicator: 4px gap + primary-container dot at track end (CSS hook,
  simplified). Circular 48px, primary arc, 4px stroke (native indicator restyled; Expressive wavy
  variant NOT implemented — planned, see COVERAGE.md).
## Slider — active track 4px primary, inactive track 4px surface-container-highest; thumb 4x44px
  handle with 20px gap (approximated with CSS thumb block); discrete mode snaps to step with value
  indicator; range = two linked thumbs that cannot cross (M3RangeSlider). 48px touch height.
  Stop indicators on discrete steps: planned (COVERAGE.md).
