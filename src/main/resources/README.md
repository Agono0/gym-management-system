# `resources` — FXML, Theme, DB, Images (JavaFX branch)

## Purpose
All non-code assets. Subfolders:

- `fxml/` — one `.fxml` layout per controller in `controller/` (matching names; 16 screens, see
  `docs/UML.md` §5).
- `theme/` — M3 theming is **dynamic**: `M3Stylesheets.applyTo(scene, M3Theme)` generates the
  `-md-sys-color-*` stylesheet at runtime (no static `material-theme.css` to maintain).
- `db/` — `schema.sql` (SQLite DDL, written Week 1) + demo seed data.
- `images/` — logo, placeholder avatar, equipment icons.

## Contents
- `fxml/main.fxml` — component showcase for eyeballing every m3fx control
  (testing only; **no `fx:controller`**). It is the m3fx gallery, not the application shell, and is
  replaced by the real nav shell once `MainApp` wires the theme and overlay layer.
- Everything else arrives as features land (`.gitkeep` files keep the empty folders tracked).

## Tasks
| Task | Owner | Status |
|------|-------|--------|
| Skeleton + this README created | Abdelrhman | Done |
| `schema.sql` v1 + seed data (Week 1, foundation) | Abdelrhman | To Do |
| M3 theme wiring via `M3Stylesheets.applyTo(scene, theme)` + overlay layer install (Week 1, foundation) | Abdelrhman | To Do |
| Replace the `main.fxml` showcase with the real nav shell | Abdelrhman | To Do |
| FXML per owned screen (Weeks 2–5) | Each owner | To Do |
| Logo + placeholder images | Abdel Raouf | To Do |

Rules: no hardcoded colors in FXML/controllers — reference `-md-sys-color-*` theme lookups;
`gym.db` is git-ignored, only `schema.sql` is committed.

Note: the overlay layer for dialogs, snackbars and sheets lives in
`io.m3fx.controls.internal`, which the `m3fx` module does **not** export. `MainApp` needs a public
entry point from m3fx before those components can be used — either upstream a public
`M3Overlay`/`OverlayLayer` facade or reword the shell to use the exported `M3Dialog` API.
