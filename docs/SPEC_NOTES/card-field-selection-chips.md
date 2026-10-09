# SPEC_NOTES — card, text field, selection, chips

Sources:
- https://m3.material.io/components/cards/overview (`_md-comp-elevated-card.scss`,
  `_md-comp-filled-card.scss`, `_md-comp-outlined-card.scss`)
- https://m3.material.io/components/text-fields/overview (`_md-comp-filled-field.scss`,
  `_md-comp-outlined-field.scss`)
- https://m3.material.io/components/checkbox/overview, .../radio-button/overview,
  .../switch/overview (`_md-comp-checkbox.scss`, `_md-comp-radio.scss`, `_md-comp-switch.scss`)
- https://m3.material.io/components/chips/overview (`_md-comp-assist-chip.scss`,
  `_md-comp-filter-chip.scss`, `_md-comp-input-chip.scss`, `_md-comp-suggestion-chip.scss`)

## Card (M3Card)
- Shape medium (12px). Elevated: surface-container-low + level1 (hover level2, pressed level1,
  dragged level3). Filled: surface-container-highest, level0. Outlined: surface + outline-variant
  1px border, level0.
- Content padding 16px; clickable cards get hover state layer + keyboard activation.

## Text field (M3TextField)
- Filled: surface-container-highest container, top 4px radius, 1px bottom active indicator
  (on-surface-variant; focused: 2px primary via `input-focused` pseudo-class).
- Outlined: 4px radius outline border (focused: 2px primary via `input-focused`).
- Input text body-large (16px); label body-large resting → body-small... actually label-large
  (12px... spec: floating label 12px) — implemented label 12px resting/floated.
- Supporting text body-small (12px); error recolors indicator+label+support to error.
- Leading/trailing icons on-surface-variant; counter body-small right-aligned.

## Selection (M3CheckBox / M3RadioButton / M3Switch)
- Checkbox container 18px, 2px radius... checkmark on-primary on primary container when selected;
  error variant for error state (pseudo-class, CSS hook present).
- Radio container 20px circle, selected dot primary 10px... (native JavaFX radio restyled).
- Switch track 52x32, full shape; thumb 16px (unselected, outline fill) → 24px selected with
  primary track; icon thumb optional (on-primary-container dot). Pressed thumb grows (CSS hook).
- Touch target 48px for all three.

## Chips (M3Chip)
- Height 32px, small shape (8px), label-large.
- Assist: elevated (level1), surface-container-low... spec assist = elevated flat? assist container
  surface-container-low with level1. Suggestion: outlined, transparent.
- Filter/input: 1px outline (unselected) → secondary-container flat (selected, no border),
  checkmark leading when selected (handled by selected pseudo-class + CSS).
- Input chips: avatar leading + close trailing (closable flag; close icon rendering is caller-side).
