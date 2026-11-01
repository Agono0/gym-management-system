# Liquid Glass (Apple iOS 26) — implementation spec for m3fx

Sources: user skill `skills/liquid-glass-design/SKILL.md`; `swiftui-liquid-glass` skill + its
`references/liquid-glass.md`; Apple HIG Materials + WWDC25 "Meet Liquid Glass" framing;
reverse-engineering: kube.io/blog/liquid-glass-css-svg (Snell's-law displacement math),
github.com/ybouane/liquidglass (pipeline + defaults), Gui Rambo "Inside Liquid Glass".
JavaFX mapping decisions are ours (documented, not claimed as Apple values).

## Apple behavior to replicate (in priority order)
1. **Optics**: blurs content behind it; REFRACTS at edges (light bends, background pulls inward
   near the rim); specular rim highlight (rim light, angle-dependent); subtle edge glow; slight
   saturation boost of the refracted backdrop; optional chromatic fringing at edges.
2. **Variants**: `regular` (frosted, default) and `clear` (more transparent, for content-rich
   backdrops). Tint optional (`.tint(color)` = prominence). Shapes: `capsule` (default), `rect(r)`,
   `circle`. Interactive opt-in (`.interactive()`): glass reacts to touch/pointer (flex + specular
   shift) — only on controls that react.
3. **Container**: multiple glass elements belong in ONE `GlassEffectContainer(spacing:)` — closer
   than `spacing` and their shapes BLEND/MERGE into one blob; enables MORPHING: elements appearing/
   disappearing (via `glassEffectID` + namespace) morph fluidly instead of popping, with the glass
   stretching between them.
4. **Adaptation**: dynamic light/dark; legibility is preserved (Apple layers a scrim/tint when the
   backdrop is busy); content sits UNDER the glass, controls float ABOVE.
5. **Physics/animation**: springs — our `SpringInterpolator.FLUID` (0.82/6.0, recovered from the
   user's stash) drives morph/flex; press flattens the bevel + deepens shadow (ybouane `button`
   behavior); shadow 0.30 opacity / 10 spread / +1 offsetY defaults.

## Physics math (the refraction displacement map)
- Height profile over the bezel, x in [0,1] outer edge → bezel end:
  - `convex(x) = (1 - (1-x)^4)^(1/4)` (squircle — chosen; circle is `(1-(1-x)^2)^(1/2)`)
  - `concave(x) = 1 - convex(x)`; `lip(x) = mix(convex, concave, smootherstep(x))`
- Surface normal from numeric derivative (central difference, dx=0.001), rotated -90°.
- Displacement = refracted ray offset (Snell, n=1.5, single refraction, orthographic), simulated
  on 127 rays per radius, magnitudes normalized to [0,1]; the real max pixel shift is the map scale.
- JavaFX encoding: `DisplacementMap` + `FloatMap` — channel offsets at 0.5, per-channel scale =
  maxDisplacement px; x-displacement in R channel, y in G (same as feDisplacementMap).
  `maxDisplacement ≈ refraction * min(w,h) * 0.08` (tuned so refraction=1.0 ≈ ybouane default 0.69
  visually).
- **Variable blur**: clearest center → diffused edges. Approximate with 2 backdrop layers:
  full-copy + blurred copy, the blurred one clipped by a bezel ring inset (blurred edge mask), so
  the center stays sharp. Cheap and close to the real thing.

## Rendering pipeline (JavaFX, per glass node)
1. Backdrop capture: `snapshot` of the node BEHIND the glass (parent background + earlier
   siblings), cached, invalidated on scene/layout/theme change — never per frame.
2. Blur layer (regular variant): GaussianBlur ~16px on the backdrop; clear variant ~4px.
3. Refraction: apply the DisplacementMap (rebuilt only on size/radius/shape change; the map is a
   pure function — unit-testable).
4. Saturation: ColorAdjust saturation +0.18 (regular) / +0.10 (clear).
5. Tint (optional): semi-transparent fill of the tint color at 0.25 alpha.
6. Specular rim: gradient rim light (default angle 110°, top-left), intensity 0.35; edge glow
   stroke 1px at 0.45 of outline-variant.
7. Shadow: DropShadow 0.30 / 10 / +1 (skip for `circle` in toolbars per Apple's floating chrome).
8. Content floats above, never blurred.

## Public API contract (all agents build exactly this)
package `io.m3fx.controls.glass`:
- `enum GlassVariant { REGULAR, CLEAR }`
- `enum GlassShape { CAPSULE, RECT, CIRCLE }` (RECT uses cornerRadius; CAPSULE = height/2)
- `class GlassEffectParams` — immutable: variant, tint (Color, nullable), interactive (boolean),
  shape, cornerRadius; fluent `tint(Color)`, `interactive()`, `in(GlassShape, double radius)`.
- `class M3GlassPane extends Control` — the real glass (Control + GlassPaneSkin):
  properties `effect` (ObjectProperty<GlassEffectParams>), `blurRadius`, `cornerRadius`,
  `glassOpacity` (kept for the legacy wrapper), `refraction`, `specular`, `specularAngle`,
  `saturation`, `maxDisplacement`; `setContent(Node)`/`getContent()`/`getContentBox()` (the legacy
  surface); factories `M3GlassPane.regular()`, `.clear()`, `.of(GlassEffectParams)`.
- `class M3GlassEffectContainer extends Pane` — `spacingProperty()` (default 40), children that
  are glass nodes merge when their bounds are closer than spacing (union render path), and
  `morphTo`/ID-based morphing: `registerGlass(Node, Object morphId)`, morph driven by
  `SpringAnimation` with `MotionTokens.fluidSpring()`.
- `class GlassRefraction` — static pure math: `heightProfile(x, profile)`, `normals(...)`,
  `buildFloatMap(width, height, cornerRadius, GlassShape, profile, refraction, maxDisplacement)`
  returning `javafx.scene.effect.FloatMap`. 100% unit-testable, no FX scene needed beyond FloatMap.
- `class GlassMorph` — static helpers to morph one glass node's bounds to another's using
  SpringAnimation.FLUID (for container appear/disappear morphs).
- `glass.css` under /io/m3fx/css/ — specular/tint/shadow theming via looked-up colors only;
  loaded by M3Stylesheets (GLASS_CSS constant — already referenced by the recovered wiring).

## Wiring (recovered from the user's stash, to be completed)
- `ButtonVariant.GLASS`, `GLASS_PROMINENT` → style classes `glass-button`, `glass-button-prominent`
  (M3Button.glass()/glassProminent() factories already in stash wiring); `CardVariant.GLASS` →
  `glass-card`; `ToolbarStyle.GLASS` → `glass-panel`. Their CSS lives in glass.css.
- `effect/M3GlassPane` becomes a thin backward-compatible wrapper extending
  `io.m3fx.controls.glass.M3GlassPane` (stash design), preserving blurRadius/cornerRadius/
  glassOpacity/getContentBox/getContent/setContent.
- `MotionTokens.fluidSpring()` / `SpringInterpolator.FLUID` (0.82/6.0) = the morph/flex spring.
