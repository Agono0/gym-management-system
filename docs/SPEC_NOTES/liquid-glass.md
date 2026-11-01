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

## Physics (the liquidGL shader, ported per-pixel)
The displacement field reproduces the liquidGL fragment shader exactly
(`liquidGL-main/scripts/liquidGL.js`, main `fs`), evaluated per pixel over the cached backdrop
capture in `GlassRefraction.render` (a literal Java port of `main()`; no FloatMap approximations):

    d        = -udRoundBox(p, b, r)                                   signed distance, px
    edge     = 1 - smoothstep(0, bevelWidth*min(boxW,boxH), d)         1 at rim, 0 inward
    offset   = edge*refraction + edge^10 * bevelDepth                  UV units
    centre   = smoothstep(0.15, 0.45, |p| / boxH)                      flat centre, live rim
    dir      = cornerNormal(p, b, r, normalize(p+eps))                 rim-aware outward normal
    uv       = mapped + dir*offset*centre (+ OOB blend)                refracted sample
    frost    = 16 random taps in a frost*4-px radius (else a 5-tap cross)
    aberr    = R sampled at uv-chroma, B at uv+chroma, chroma = offset*aberration
    tint     = mix(rgb, rgb*tint.rgb, tint.a);  alpha *= inShape

Fluid interaction ports `deformPoint` + the deformed-SDF gradient blend verbatim (pointer centre,
smoothstep reach, viscous delta smoothing at 0.65). liquidGL's two time-animated specular lights
(`lp1/lp2`, bumps 0.10/0.08) are composited as animated radial overlays. Defaults are the engine's:
`refraction 0.01`, `bevelDepth 0.08`, `bevelWidth 0.15`, `magnify 1`.

The nikdelvin construction (`liquid-glass-main/src/utils/liquidGlass.ts`) ships as the
`SVG_DISPLACEMENT` map style: navy base, screen-blended linear gradients (insets `0.15*radius`),
gray rounded rect inset by `depth` (screen blend neutralizes the centre), box-blur approximation
of the SVG blur chain, `scale = strength` (defaults depth 10 / strength 100).

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
