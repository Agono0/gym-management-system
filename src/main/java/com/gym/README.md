# `com.gym` — Application Entry Point (JavaFX branch)

Canonical design: `docs/UML.md` §1 (declaration), §2 (startup flow), §5 (it opens the first screen).

## Purpose

`MainApp` is the JavaFX `Application` subclass and the only class in this package. It sits outside
the four layers: it builds the window and nav shell, applies the M3 theme, installs the overlay
layer, opens `LoginController`, and clears `SessionContext` on exit.

## Contents

| File | Responsibility | Owner | Status |
|------|----------------|-------|--------|
| `MainApp.java` | Entry point: window, nav shell, theme, overlay layer | Abdelrhman | Skeleton only |

The current `MainApp.java` is a placeholder: hardcoded title `"JavaFX Application"`, a fixed
600x400 scene, no `M3Stylesheets.applyTo(...)` call and no `SessionContext` handling. It does not
yet match the design.

## Tasks

| Task | Owner | Status |
|------|-------|--------|
| Skeleton created | Abdelrhman | Done |
| Apply the M3 theme (`M3Stylesheets.applyTo(scene, theme)`) | Abdelrhman | To Do |
| Install the overlay layer and nav shell | Abdelrhman | To Do |
| Open `LoginController` and clear `SessionContext` on exit | Abdelrhman | To Do |

Rules: all UI code runs on the FX thread. No static mutable state in this package. Do not
reference DAOs or SQL from here — controller → service → DAO only.
