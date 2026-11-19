# Skin lifecycle — the dispose contract for every m3fx skin

This is a lifecycle/bugfix contract note, not a visual spec (no m3.material.io page governs
JavaFX skin teardown). It captures the rules every `SkinBase` subclass in `m3fx-controls` must
follow, and cites the audited bugs that motivate them.

## Why

Phase 0 audit (`docs/agents/M3FX_AUDIT.md`, task B1) found three skin lifecycle defects:

1. **`SwitchSkin` leaked event handlers.** The skin registered `MOUSE_CLICKED` and
   `KEY_PRESSED` handlers on the control via anonymous lambdas (`M3Switch.java:177–190`) but
   `dispose()` (`M3Switch.java:194–207`) removed only the property listeners. After a skin
   swap, the dead skin still received events: one click toggled the switch twice (net no-op)
   — verified pre-fix with a scratch JUnit test (`expected: <1> but was: <0>`).
2. **`LinearSkin` leaked its `sceneProperty` listener** (`M3LinearProgress.java:214`,
   dispose `339–350`), and the indeterminate `Timeline` was stopped, rebuilt and replayed on
   *every* width change (`startLoop()` from the width listener, `153–158`) — allocation churn
   during window resizes.
3. **`GlassPaneSkin` started its physics `AnimationTimer` unconditionally** in the constructor
   (`GlassPaneSkin.java:419`). Every glass pane in a scene woke the timer every pulse even
   when off-screen or hidden; `stepPhysics` compensated with early returns instead of the
   timer being idle.

## The contract

Every skin in this library, when `dispose()` is called (JavaFX 9+ calls it when the skin is
replaced via `setSkin` or the control is torn down; `SkinBase.dispose()` then nulls the
skinnable):

1. **Event handlers added via `control.addEventHandler`/`addEventFilter` are stored in fields
   and removed in `dispose()`.** Anonymous lambdas passed straight to `addEventHandler` cannot
   be removed — store the `EventHandler` reference first (or use instance methods).
2. **Property listeners are stored in fields and removed in `dispose()`** — including
   `sceneProperty`, `parentProperty`, `visibleProperty` and any property of the attached
   `Scene`/`Window` (scene properties, `Window.showingProperty()`).
3. **Bindings created against externally-owned properties are unbound in `dispose()`.**
   Skin-internal bindings (skin node → skin node) may remain.
4. **Animations, timelines and timers are stopped in `dispose()`.** Continuous/pulse-driven
   machinery (`AnimationTimer`, indefinite `Timeline`) must additionally be *activated
   lazily*: run only while the control is actually on screen — attached to a `Scene` whose
   `Window` is showing and whose whole node tree is visible — and stop as soon as any of
   those conditions turns false. `stepPhysics`-style early returns inside the callback are a
   safety net, not the mechanism.
5. **`dispose()` is idempotent and null-safe.** Every removal is guarded so a second call
   (skinnable already nulled by `SkinBase.dispose()`) neither throws nor resurrects anything.
6. **Width/size changes must not rebuild continuous animations.** Animate width-independent
   fractions and bind pixel outputs through `width`-multiplying bindings (the pattern used by
   `LinearSkin` since B1), so a running animation survives resizes untouched.

## Source-of-truth implementations

- `M3Switch.SwitchSkin` — handlers + listeners + springs stopped in `dispose()`.
- `M3LinearProgress.LinearSkin` — scene listener removed; single indeterminate timeline
  reused across width changes; `LinearSkin.indeterminateLoopForTesting()` exposes it for
  tests.
- `GlassPaneSkin` — physics timer gated by `isPhysicsWanted()`/`updatePhysicsTimer()`
  (scene + `Window.showingProperty()` + tree visibility) and exposed for tests via
  `isPhysicsTimerActive()`.

## Known boundary

An ancestor node becoming invisible does not itself fire a signal `GlassPaneSkin` listens to;
the ancestor walk inside `isPhysicsWanted()` re-evaluates on the next activation-affecting
event (scene/window/visibility change). The dominant hidden cases — window hidden, control
hidden (`setVisible(false)`, e.g. tab content), scene removed — are all directly listened to.
`Node.isTreeShowing()` would be the ideal signal but is package-private in JavaFX `[VERIFY:
checked via javap on javafx-graphics 21.0.4 — declared package-private]`.

## Related bug found during B1 (not lifecycle, fixed in the same change)

`LinearSkin.buildLoop()` used `Interpolator.SPLINE` with two material-web control points that
overshoot [0,1] (`sW2` y2=1.00432, `sW3` y1=-0.003163 / y2=1.38179). JavaFX's SPLINE rejects
any control point outside [0,1], so `buildLoop()` threw `IllegalArgumentException` on every
invocation — the indeterminate linear progress animation never actually ran (the exception
propagated out of the width listener into the pulse). The fix keeps every material-web value
verbatim and adds `LinearSkin.OvershootSpline`, a nested cubic-bezier interpolator with web
`cubic-bezier()` semantics (x in [0,1], y free), verified against `Interpolator.SPLINE` for
in-range points and against the expected overshoot for sW3. A future task could promote it to
`io.m3fx.core.token` as a shared easing token.
