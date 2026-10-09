# SPEC_NOTES — button, icon button, FAB

Sources:
- https://m3.material.io/components/buttons/overview
- https://m3.material.io/components/icon-buttons/overview
- https://m3.material.io/components/floating-action-button/overview
- material-web tokens v0_192: `_md-comp-filled-button.scss` (+ elevated/outlined/text/tonal),
  `_md-comp-filled-icon-button.scss` (+ standard/tonal/outlined), `_md-comp-fab.scss`,
  `_md-comp-extended-fab-{primary,secondary,surface}.scss`

## Button (M3Button)
- Container height 40px (`container-height: 40px` in filled-button tokens).
- Shape: `container-shape: corner-full` → 20px radius pill.
- Filled: container primary, label on-primary, label-large type, elevation level0
  (hover level1, pressed level0). State layers: hover/focus/pressed on-primary 8/10/10%.
- Disabled: container on-surface @12% opacity, label on-surface @38%.
- Tonal: container secondary-container, label on-secondary-container.
- Outlined: transparent container, outline 1px border, label primary.
- Elevated: container surface-container-low, label primary, elevation level1
  (hover level2). Disabled: no elevation.
- Text: transparent, label primary, tighter padding (12px sides).
- Leading/trailing space per tokens; icon size 20px when icon+label.

## Icon button (M3IconButton)
- Touch target 48x48; container 40px circle for filled/tonal/outlined.
- Standard: no container, icon on-surface-variant.
- Filled: primary container / on-primary icon. Tonal: secondary-container. Outlined: outline border.
- Toggle: unselected = standard look, selected = filled look (primary container); width 48.

## FAB (M3Fab)
- Sizes: small 40px, medium 56px (default), large 96px.
- Shape: small 12px radius... actually FAB shape = large (16px) default, small FAB 12px,
  large FAB 28px. Implemented in CSS per size class.
- Container primary-container, icon on-primary-container, elevation level3 (hover level4).
- Extended: height 56px, label-large text + icon, 16px extended shape radius, padding 20/16.
- FAB menu (Expressive): NOT implemented — listed as planned in COVERAGE.md.
