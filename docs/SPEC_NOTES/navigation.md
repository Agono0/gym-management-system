# SPEC_NOTES — top app bar, navigation, tabs

Sources:
- https://m3.material.io/components/top-app-bar/overview
- https://m3.material.io/components/navigation-bar/overview
- https://m3.material.io/components/navigation-rail/overview
- https://m3.material.io/components/navigation-drawer/overview
- https://m3.material.io/components/tabs/overview

## Top app bar — small 64px single row (title-large 22px on-surface); center-aligned same with
  centered title; medium 112px (small title row + headline 28px... title-medium/small row +
  headline-small 24px... implemented headline 28px); large 152px (headline-medium 28px...
  actually headline-large-ish 32px — implemented 32px). Container surface; scrolled state adds
  surface-container + level2 (caller switches elevation on scroll; CSS hook present).
## Navigation bar — 80px height, surface-container; destinations 3-5; icon container 64x32
  full shape, selected = secondary-container pill; label label-medium (12px), selected on-surface
  bold. Badge anchoring: caller overlays M3Badge on the icon.
## Navigation rail — 80px wide, surface; icon container 56x32 pill; optional header (menu/FAB);
  alignment top; modal variant not in spec (drawer covers it).
## Navigation drawer — standard 320-360px (implemented 280/320), surface-container-low... actually
  surface-container-low for standard; modal same + scrim + level1. Item 56px height, 28px full
  active pill (secondary-container), label-large. Dividers + section labels per spec (caller-side).
## Tabs — primary: title-small... label-large 14px active primary + 3px primary indicator;
  secondary: compact 12-14px with optional icons. Scrollable tabs: native TabPane scrolls via CSS.
  Dividers under tab row per spec (CSS hook).
