# SPEC_NOTES — dialog, snackbar, menu/select, list, sheets, pickers, search, segmented

Sources:
- https://m3.material.io/components/dialogs/overview
- https://m3.material.io/components/snackbar/overview
- https://m3.material.io/components/menus/overview
- https://m3.material.io/components/lists/overview
- https://m3.material.io/components/bottom-sheets/overview
- https://m3.material.io/components/side-sheets/overview
- https://m3.material.io/components/date-pickers/overview
- https://m3.material.io/components/time-pickers/overview
- https://m3.material.io/components/search/overview
- https://m3.material.io/components/segmented-buttons/overview

## Dialog — basic container surface-container-high, 28px radius, min-width 280 / max 560;
  icon (secondary-container... on-surface-variant 24px) + headline-small title (24px) + body-medium
  supporting text (14px on-surface-variant) + text-button actions right-aligned. Fullscreen variant
  (no radius). Scrim 32% + level3 elevation. Shown in OverlayLayer, never a Stage.
## Snackbar — inverse-surface container, 4px radius, min-height 48px; text inverse-on-surface 14px;
  action = inverse-primary text button; close icon; queue FIFO; 4s (10s with action).
## Menu — surface-container, 4px radius, level2; item min-height 48, label-large 14px on-surface,
  leading/trailing icons on-surface-variant; dividers outline-variant. Popup-based, keyboard nav
  via native ContextMenu. Select = filled-field-styled ComboBox (56px min-height).
## List — one-line 56px (icon) / list padding 8px... rows: 1-line 56, 2-line 72, 3-line 88;
  leading 16/24px padding; headline body-large 16px, supporting body-medium 14px, overline
  label-small 11px... implemented headline 16 / supporting 14 / overline 12.
## Bottom sheet — surface-container-low, top radius 28px, drag handle 32x4 on-surface-variant
  @40%; level1; modal with scrim. Side sheet — 256 docked / 320-400 wide, same color, modal adds
  level1 + scrim.
## Date picker — input = filled field 56px; calendar dialog surface-container-high 28px radius,
  headline + weekday + day cells (selected = primary circle). NOTE: full calendar-grid skin is
  planned work — M3DatePicker currently wraps JavaFX DatePicker with M3 field styling; the dialog
  tokens are documented here for the next pass.
## Time picker — dial surface-container-high (future), input mode = hour/minute 56px fields +
  AM/PM segmented toggle; implemented exactly as input mode.
## Search — collapsed bar 56px full shape, surface-container-high, ⌕ + hint (body-large 16px) +
  trailing avatar/close; expanded suggestions surface-container list with leading icons + supporting
  text; scrim on expand (popup autohide used instead — documented gap).
## Segmented button — 40px height, full-shape outline container, per-segment dividers; selected =
  secondary-container + check icon; 2-5 segments single-select. (Also covers Expressive button
  group look.)
