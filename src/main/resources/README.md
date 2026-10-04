# `resources` — FXML, Theme, DB, Images (JavaFX branch)

## Purpose
All non-code assets. Subfolders:

- `fxml/` — one `.fxml` layout per controller in `controller/` (matching names).
- `theme/` — `material-theme.css`: M3 color tokens, typography, component
  styles, expressive motion keyframes. **Single source of truth for styling.**
- `db/` — `schema.sql` (SQLite DDL, written Week 1) + demo seed data.
- `images/` — logo, placeholder avatar, equipment icons.

## Contents
Currently only this README — asset files are created as features land.
(`.gitkeep` files keep the empty folders tracked.)

## Tasks
| Task | Owner | Status |
|------|-------|--------|
| Skeleton + this README created | Abdelrhman | Done |
| `schema.sql` v1 + seed data (Week 1, foundation) | Abdelrhman | To Do |
| `material-theme.css` M3 tokens baseline (Week 1, foundation) | Abdelrhman | To Do |
| FXML per owned screen (Weeks 2–5) | Each owner | To Do |
| Logo + placeholder images | Abdel Raouf | To Do |

Rules: no hardcoded colors in FXML/controllers — reference theme tokens;
`gym.db` is git-ignored, only `schema.sql` is committed.
