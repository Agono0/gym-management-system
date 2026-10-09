# SPEC_NOTES — tabs + segmented button rebuild

Sources:
- material-web tokens v0_192: `_md-comp-primary-navigation-tab.scss`,
  `_md-comp-secondary-navigation-tab.scss`, `_md-comp-outlined-segmented-button.scss`
- https://m3.material.io/components/tabs/overview
- https://m3.material.io/components/segmented-buttons/overview

## Tabs (M3Tabs, token-grounded)
- Container height 48px both variants (`container-height: 48px` in both token files).
- Primary: active indicator 3px (`active-indicator-height: 3px`), label-large text, icon 24px.
- Secondary: active indicator 2px, title-small text (14px/500), icon 24px.
- Container color surface, shape none; divider under header = surface-container-highest 1px.
- JavaFX specifics: modena chrome fully neutralized (tab insets/borders, focus glow,
  content-area border); hover = primary 8% layer; focus = primary 10% layer; disabled label
  on-surface 38%.

## Segmented button (M3SegmentedButton, token-grounded)
- Container height 40px, full shape, 1px outline (`container-height`, `outline-width`).
- Label-large text; selected container secondary-container; icon 18px (`with-icon-icon-size`).
- Selected segment shows a check icon (code-set graphic, 18px box) — spec selected-segment look.
- Outer corners: first segment `19 0 0 19`, last `0 19 19 0` (20px container radius minus 1px
  border); dividers only between segments (no double line at the trailing edge).
- Robustness: every state (`:hover`/`:armed`/`:pressed`/`:selected`/`:disabled` and
  `:selected:pressed` combos) re-declares background + zero insets, so equal-specificity modena
  state layers can never shrink the fill to a partial rect.
