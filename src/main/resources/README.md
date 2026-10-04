# `resources` — Theme, DB, Images (Swing branch)

## Purpose
All non-code assets. Subfolders:

- `theme/` — `material-swing.properties`: FlatLaf M3 color tokens, fonts,
  component defaults mirroring the JavaFX `material-theme.css`.
- `db/` — `schema.sql` (same design as `main`, created Week 1) + demo seed.
- `images/` — logo, placeholder avatar, equipment icons.

## Contents
Currently only this README — asset files are created as features land.
(`.gitkeep` files keep the empty folders tracked.)

## Tasks
| Task | Owner | Status |
|------|-------|--------|
| Skeleton + this README created | Abdelrhman | Done |
| `schema.sql` v1 + seed data, shared design with `main` (Week 1) | Abdelrhman | To Do |
| FlatLaf M3 theme properties baseline (Week 1) | Abdelrhman | To Do |
| Logo + placeholder images | Abdel Raouf | To Do |

Rules: theme properties are the single source of truth — no hardcoded
colors/fonts in `view/`; `gym.db` is git-ignored, only `schema.sql` commits.
