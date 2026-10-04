# `util` — Shared Helpers (Swing branch)

## Purpose
Cross-cutting helpers. `DbConnection`, `Validators`, and `DateUtils` are
ported as-is from `main` (toolkit-free); only dialog/scene helpers are
rewritten for Swing.

## Contents (to be written)
| Class | Responsibility | Owner |
|-------|----------------|-------|
| `DbConnection.java` | SQLite connection factory, `gym.db` bootstrap (ported) | Abdelrhman |
| `Validators.java` | Input validation (ported) | Abdelrhman |
| `DateUtils.java` | Period/slot math, formatting (ported) | Yousef |
| `SwingUtils.java` | Dialogs, option panes, panel switching, table helpers | Abdelrhman |
| `ChartUtils.java` | Progress-chart dataset builders (ported, renderer adapted) | Abdel Raouf |

## Tasks
| Task | Owner | Status |
|------|-------|--------|
| Skeleton + this README created | Abdelrhman | Done |
| Port `DbConnection`, `Validators`, `DateUtils` | Owners above | To Do |
| Write `SwingUtils` (Week 1–2) | Abdelrhman | To Do |
| Adapt `ChartUtils` | Abdel Raouf | To Do |
