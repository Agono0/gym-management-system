# docs/agents/OPEN_QUESTIONS.md — unresolved / [VERIFY] items

Policy: anything a worker could not verify against the official spec goes here with
`// [VERIFY]` in code. Never silently invent spec values.

1. **Roboto bundling** — `M3Fonts` falls back to system fonts because no TTFs ship in
   `m3fx-controls/src/main/resources/io/m3fx/fonts/`. Adding binary font assets = user decision
   (repo policy: no new assets/deps without approval). Fallback styling is acceptable for now.
2. **Spring token constants vs v34** — `SpringToken` grid (SPATIAL_EXPRESSIVE 0.65/380 etc.)
   is flagged unverified against Compose v34 motion tokens (COVERAGE open item). The numbers in
   the multi-agent prompt (fast 0.6/800 … effects 1.0/1600) disagree with the bundled grid —
   verify against androidx.compose.animation.core MotionScheme before trusting either. Until
   then keep the bundled grid (it renders well) and mark `// [VERIFY]`.
3. **CI workflow** — user has not opted into a GitHub Actions file; charter gate stays local
   `./gradlew build`. One-file addition later if approved.
4. **Drawer/rail widths** — library drawer 280/320 vs token 360 vs app shell 220 (deliberate
   deviation, documented in COVERAGE). App size-class work (C1) should keep the documented
   deviation or align consciously.
5. **Slider track** — 16dp single-track slider deviation is a JavaFX 21 `SliderSkin` limitation.
6. **SPEC_2026 provenance — RESOLVED (A1, round 1)**: the earlier audit premise was wrong;
   `ColorSpec2026`, `Variant.CMF` and `SchemeCmf` are genuine upstream material-color-utilities
   `main` files (byte-identical modulo package renames — verified by Worker A's diff against
   upstream raw sources). Headers are authentic; the only real gap was the unreachable public
   API, fixed additively in A1. See `docs/SPEC_NOTES/color-system-versions.md`.
8. **Upstream drift in `DynamicScheme.maybeFallbackSpecVersion`** — vendored snapshot predates
   upstream's `(specVersion, variant, platform)` signature that keeps SPEC_2025 for
   WATCH+CONTENT. Unreachable from `M3Theme` (always PHONE). Parked for a future vendor
   refresh; semantics unchanged per A1.
7. **OS reduced-motion detection on Windows** — pure Java cannot read
   SPI_GETCLIENTAREAANIMATION without JNA (new dep = forbidden). Decision: app-level setting +
   `m3fx.reducedMotion` system property (documented in A3); revisit only if the user approves
   a JNA dependency.
