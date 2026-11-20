# Icons — bundled Material icon catalog

Source of truth for `M3Icons.java` naming, provenance and the app-audit contract.

## Provenance

Every catalog entry is the verbatim 24px glyph path from the official
**google/material-design-icons** GitHub repository (Apache 2.0), file
`src/<category>/<name>/materialicons/24px.svg` — the filled legacy Material Icons set, which
matches the style of the pre-existing 37 entries (legacy `menu`/`check`/`settings` paths).
Nothing is hand-drawn or approximated. The only mechanical transform is for
`monitor_weight`, whose official SVG contains three 1×1 `<rect>` tick dots; SVGPath renders
path data only, so the rects are expressed as path subpaths (`M10 8.5h1v1H10z` …). The
bounding-box `<path d="M0 0h24v24H0z" fill="none"/>` wrappers are stripped.

## Naming contract

- Catalog keys are Material icon names (snake_case) — call sites use
  `M3Icon.symbol("water_drop")` etc. Unknown names render the neutral placeholder ring and log
  one `[m3fx] M3Icon: unknown icon name` warning per distinct name (`M3Icon.renderSymbol`).
- **`fitness` is a documented app-facing alias of `fitness_center`** — it is not a Material
  icon name, but the gallery and shell request it; the alias renders the dumbbell instead of a
  ring. New aliases need the same two-line comment treatment next to their `PATHS.put`.
- `M3IconsTest.requiredAppAndGymIconNamesResolve` pins every name the B3 call-site audit found
  in the app plus the gym/common set; do not remove entries without updating that test and the
  app call sites in the same change.

## Raw-SVG dedupe (for the app side, lane C)

`LoginController` / `LaunchGateController` still pass hand-copied eye and lock SVG strings to
`M3Icon.ofSvg(...)` (`EYE_PATH`, `EYE_OFF_PATH`, `LOCK_PATH`). The catalog now carries the
authoritative glyphs (`visibility`, `visibility_off`, `lock`); those controllers can switch to
`M3Icon.symbol(...)` and delete the constants. Note the hand-copied eye paths are close but
not identical to the official glyph (arc vs Bézier inner ring) — replacing them is a visual
alignment, not a regression.

## Sanity test

`M3IconsTest.everyNameResolvesToWellFormedPathData` enforces per entry: starts with a move
command, only SVG path-grammar characters (plain decimals — no exponent notation), trimmed,
no line breaks, length between 4 and 4000 characters (largest bundled entry is ~1500).
