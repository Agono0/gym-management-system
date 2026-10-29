# `images/` — image and icon assets (JavaFX branch)

> **Signature authority:** `docs/UML.md` §1 for the data these assets stand in for
> (`Equipment.category`, `Equipment.name`, `Customer.fullName`, `Administrator.fullName`).
> **Parent doc:** [`src/main/resources/README.md`](../README.md) — this file is the detail behind its
> `images/` entry. Where the two disagree, the parent's naming table wins and this file is corrected.
> **Scope:** the app's own bitmaps and design sources. Icons rendered by the external `m3fx` library
> (`M3Icon.symbol("favorite")`, Material Symbols path data — `DECISIONS.md` 9) are **not** files in this
> folder.

## 1. Purpose — the problem this folder solves

Every screen that greets a human needs something to look at before the data arrives: the app logo, a
stand-in avatar for a member who has no photo, a category icon for a treadmill, a drawing for "this
member has no progress records yet". This folder is the single home for those assets, so they are named
consistently, licensed honestly and small enough not to bloat the jar.

**What breaks if this folder is a dumping ground.**

1. **The jar grows unnoticed.** A 2 MB PNG committed for a 24 px icon is downloaded by every teammate and
   shipped to the grader; nobody notices until the build artifact is 30 MB.
2. **Dark mode breaks.** A logo exported with a white background looks correct on a light window and
   becomes a glowing white rectangle on a dark one.
3. **A licence problem reaches the submission.** An icon pulled from the internet with no licence record
   is a legal risk in a graded, published repository.
4. **Nothing can be found.** `IMG_20251016.png`, `icon2-final-FINAL.png` and `logo.svg` in three folders
   make an asset impossible to replace, so it is never replaced.
5. **A missing file becomes a crash.** `new Image(null)` throws; a screen that assumes the avatar exists
   dies when someone deletes it.

**What this folder deliberately does not do.** It does not hold photos of real members (member photos are
not part of `docs/UML.md` §1 — adding one is a schema change), it does not hold theme colours (those are
`-md-sys-color-*` look-ups, see [`../theme/README.md`](../theme/README.md)), and it holds no Java or FXML.

## 2. What you learn here

| Concept | Where in this folder | Why the project needs it | How to apply it |
|---|---|---|---|
| Resources on the classpath | Loading `/images/logo-app.png` | The same code must work from the IDE, from `gradlew run` and from the packaged jar | Always a leading `/`; never a file-system path |
| Read-only assets | Nothing is written here at runtime | Resources live inside the jar; a "save the avatar here" design cannot work | Store user-generated files outside the resource tree (a future decision) |
| Naming as an API | `logo-app.png`, `avatar-placeholder.png`, `icon-equipment-dumbbell.svg` | A name is what the next person searches for | Lower-case `kebab-case`, kind prefix, no spaces, no version suffixes |
| Separation of asset and code | The `EquipmentController` maps a category string to an icon name | The mapping is code; the picture is data | Keep the map in the controller, the files here |
| Theme independence | "no baked-in colour" rule | The app must run in light and dark with one asset set | Transparent background, monochrome or neutral art |
| Graceful degradation | A missing asset falls back to a placeholder | A deleted file must not crash a screen | Check for `null` before building the `Image` |
| Licence hygiene | One note per non-original file | Attribution and provenance are part of shipping software (`NOTICE`, root `README.md` §9) | Keep the licence text beside the asset or in the table in §5 |
| Resolution awareness | `logo-app@2x.png` | A scaled-up icon looks blurry on a high-DPI screen | Ship enough pixels, scale **down**, keep the aspect ratio |

## 3. Design patterns in this layer

| Pattern | Where | Why | If used carelessly |
|---|---|---|---|
| **Naming convention as a contract** | §4 rule 3 and the parent doc's naming table | Reviewers and code both depend on the name | Two icons for the same concept |
| **Placeholder (Null Object)** | `avatar-placeholder.png`, empty-state art | A missing value still renders something deliberate | `null` reaching `ImageView` → `NullPointerException` |
| **Source vs shipped asset** | `.svg` design source vs the `.png` the app loads | JavaFX cannot render `.svg` files (see §4 rule 5) | An icon that "does not show up" with no error |
| **Do not do: a single sprite sheet** | — | JavaFX has no built-in atlas tooling; hand-computed viewports are a maintenance trap | Off-by-one crops nobody can debug |
| **Do not do: bundling a font for icons** | — | JavaFX has no ligature support; the library uses path data instead (`DECISIONS.md` 9) | Text that renders as the literal word "favorite" |

## 4. Rules you must follow (and the bug each rule prevents)

1. **One asset, one purpose, one file** — an icon file is not reused as a logo. *Prevents:* changing a
   logo breaking six unrelated screens.
2. **Lower-case `kebab-case`, kind prefix, no spaces, no `final`/`v2`/date suffixes**:
   `logo-app.png`, `avatar-placeholder.png`, `icon-equipment-dumbbell.svg`,
   `empty-state-no-records.svg`. *Prevents:* names that differ only by case (they collide on Windows and
   not on Linux) and files nobody can find.
3. **Keep each file small: aim for ≤ 50 KB; hard-stop before 200 KB.** Compress PNGs, drop metadata,
   resize to the largest size the UI actually needs. *Prevents:* a slow clone and a fat jar.
4. **Ship no baked-in background colour** where the app must work in both themes. *Prevents:* the
   dark-mode white rectangle.
5. **JavaFX cannot load `.svg` with `Image`.** Icons that must be *rendered* have two honest options:
   the library's icon catalog by name (`M3Icon.symbol("add")`, path data, no file needed), or a **PNG**
   in this folder. An `.svg` here is a **design source** (and the place the path data is copied from),
   not a runtime asset. *Prevents:* the classic "the SVG does not appear and there is no error" hour.
6. **Never write into this folder at runtime.** *Prevents:* code that works in the IDE (where the folder
   is writable) and fails silently from the jar.
7. **Check for `null` before using a loaded asset**, and fall back to a deliberate placeholder.
   *Prevents:* a crash caused by a deleted file.
8. **Every non-original asset carries a licence note** (a one-line entry in §5, plus the file's own
   licence if the licence requires it). *Prevents:* shipping someone else's work uncredited.
9. **Photographs of real people are out of scope** until `docs/UML.md` §1 grows a field for them.
   *Prevents:* a hidden personal-data store with no schema, no consent note and no backup rule.
10. **If a raster asset is replaced, delete the old file in the same PR.** *Prevents:* two logos, one of
    them still referenced from a screen nobody tested.

## 5. The files in this folder

Write order: the `.gitkeep` is already there; assets are added when the screen that needs them is built.
Each entry states format, size and the loader used.

### `.gitkeep` — keeps the empty folder in git

**Status:** exists (0 bytes). **Canonical:** parent doc rule 10. Delete it in the PR that adds the first
real asset.

### `logo-app.png` (+ `logo-app@2x.png`) — the application logo (owner: Abdel Raouf)

**Status:** planned. **Used by:** `MainApp` (window/title area or login backdrop) and, if a report is
ever exported, the report header.

| Property | Value | Why |
|---|---|---|
| Format | PNG, transparent background | Must sit on a light **and** a dark surface (rule 4) |
| Size | height ≈ 64 px (source ≈ 256 px for `@2x`) | Scale **down**; a 1024 px logo scaled to 32 px wastes memory and looks no better |
| Loading | `new Image(getClass().getResourceAsStream("/images/logo-app.png"))` | Leading `/` = classpath root (`../README.md` §1) |
| Fallback | a text title when the stream is `null` | The app must still start if the asset is missing |

**Common mistakes.** A logo with a white plate behind it; a JPEG (visible halo around the artwork on a
coloured surface); 3 MB of unused pixels; loading it in every screen instead of once in the shell.

### `avatar-placeholder.png` — the stand-in for a member with no photo (owner: Abdel Raouf)

**Status:** planned. **Used by:** `CustomerProfileController`, `CustomerListController` (a small variant
in the list cells), `StaffController`.

| Property | Value | Why |
|---|---|---|
| Format | PNG with transparency | Same light/dark rule as the logo |
| Size | one file at 96 × 96 px, displayed at 32–48 px | The largest use wins; scaling down is cheap, blurry upscaling is not |
| Loading | per-row `Image` instance or one shared instance | One shared `Image` is enough; JavaFX `Image` is immutable and shareable |
| Fallback | initials drawn as text when the asset is missing | Zero-asset screens still look intentional |

**Common mistakes.** Loading a fresh `Image` per table row inside a cell factory (the table then
re-decodes the PNG many times while scrolling); a placeholder that is a *photo* of a person, which reads
as a real member; a circle mask assumed to be in the file (mask in code — `setClip` — not in the pixels).

### `icon-equipment-<category>.svg` — design source for equipment category icons (owner: Abdel Raouf)

**Status:** planned; **not loaded by JavaFX** (rule 5). **Used by:** `EquipmentController` and the
equipment list.

**What it is.** The vector source of the icon for each `Equipment.category` value the team supports —
for example `icon-equipment-treadmill.svg`, `icon-equipment-dumbbell.svg`,
`icon-equipment-bike.svg`. Two things may consume it: the path data copied into the library's icon
mechanism, or a PNG exported from it at the sizes the screen needs.

**Why it is a source file, not an asset.** If a raster icon is required, export it once to
`icon-equipment-treadmill.png` (24 × 24 and 48 × 48 if the UI scales) and commit **both**: the SVG as the
editable original, the PNG as what the code loads. One of them is the truth for editing, the other for
rendering, and the PR that changes one changes the other.

**How the controller maps a category to an icon.**

```java
// Sketch — the mapping is code; the picture is an asset. Keep it in one place.
private static String iconFor(String category) {
    if (category == null) return "category";                       // never null into the icon lookup
    return switch (category.trim().toLowerCase()) {                // equipment.category is free text (UML.md §1)
        case "cardio", "treadmill" -> "treadmill";
        case "strength", "weights" -> "dumbbell";
        case "cycling", "bike"     -> "bike";
        default                    -> "category";                  // an unknown category still renders
    };
}
```

**Common mistakes.** Expecting `new Image("/images/icon-equipment-bike.svg")` to work; committing an SVG
whose licence forbids redistribution; per-category icons for categories the free-text field will never
match; an icon set in five styles (outline, filled, 2 px, 1.5 px, coloured).

### `empty-state-<screen>.svg` (or `.png`) — "nothing here yet" illustrations (owner: Abdel Raouf)

**Status:** planned for the lists that can legitimately be empty (a new member's progress, today's
attendance, a filtered search with no matches).

**What it is.** A small, neutral drawing shown where a table or chart would be, plus one sentence of
text ("No progress records yet — add the first measurement"). The text is the message; the drawing is
decoration, so it must not be the only signal (accessibility: a screen reader cannot read a picture).

**Common mistakes.** An illustration with embedded English text (untranslatable, unreadable at small
sizes); a 1 MB PNG; a drawing that implies an error when the state is simply "no data yet"; forgetting
the text line entirely.

## 6. How this folder connects to the others

1. `MainApp` loads the logo once for the shell; the file is packaged into the jar with everything else
   (parent doc §1).
2. Controllers load avatars, category icons and empty-state art. From Java, always through
   `getResourceAsStream("/images/…")` with a `null` check (rule 7).
3. From FXML, an image is referenced with the `@` prefix, which means "relative to this FXML file":
   since the screens live in `/fxml/`, `@../images/logo-app.png` resolves to `/images/logo-app.png`.
   Prefer setting images from the controller when a fallback is needed, because FXML cannot fall back.
4. Vector icons that the library renders come from **its** catalog by name, not from this folder
   (`M3Icon.symbol(...)`, `DECISIONS.md` 9) — so a missing icon is a name typo, not a missing file.
5. The Swing branch reuses this folder unchanged: image files are toolkit-independent (parent doc §9).

| This folder depends on | Who depends on this folder |
|---|---|
| Nothing (assets are self-contained) | `MainApp`, the controllers that render a logo/avatar/icon, the FXML files that reference them |
| A licence note for each non-original asset | The repository's licence hygiene (`NOTICE`, root `README.md` §9) |

## 7. Common mistakes and review checklist

| Symptom | Cause | Fix |
|---|---|---|
| The image does not appear, and there is no exception | an `.svg` passed to `Image`, or a path without the leading `/` | rule 5; PNG for runtime, `/images/...` for the path |
| `NullPointerException` when a screen opens | `new Image(url)` with a `null` URL (file missing/renamed) | check for `null`, fall back to the placeholder |
| A white/black rectangle around the logo in one theme | baked-in background colour | re-export with transparency (rule 4) |
| Scrolling a list is slow | a new `Image` per cell | share one `Image` instance per asset |
| The jar grew by megabytes | full-resolution photos committed as icons | resize, compress, target ≤ 50 KB |
| Two files for the same thing after a redesign | the old file was never deleted | delete in the same PR (rule 10) |
| A licence question during submission | no provenance record | add the §5 entry when the file is added |

**Review checklist.**

- [ ] Name follows the convention (lower-case, `kebab-case`, kind prefix); no spaces or version suffixes.
- [ ] Format matches the use: PNG for anything the app loads, `.svg` only as a design source.
- [ ] Transparent background where the asset sits on a themed surface.
- [ ] Size is the smallest that still looks right on a high-DPI screen; the file is under the budget.
- [ ] Loading code uses the leading `/`, checks `null`, and shares one `Image` instance per asset.
- [ ] A licence note exists for every non-original file.
- [ ] No personal data (member photos) and no written-in-stone text baked into a picture.
- [ ] The screen was opened in light **and** dark before the PR was merged.

## 8. Learning exercises (with answers)

1. **Why does `new Image("/images/logo-app.png")` work while `new Image("images/logo-app.png")` does
   not?** *Answer:* the leading `/` makes the path absolute on the classpath. Without it the path is
   resolved relative to the calling class's package (`com/gym/controller/images/...`), which does not
   exist, so the URL is `null` and `Image` throws.
2. **Find the bug.** A list cell factory calls `new Image(getClass().getResourceAsStream(
   "/images/avatar-placeholder.png"))` for every row. What is wrong, and what is the fix? *Answer:* the
   PNG is decoded once per visible row and again on every scroll refresh — measurable jank in a long
   list. Fix: decode one `Image` (static final or passed in) and give every `ImageView` that instance;
   `Image` is immutable and safely shareable.
3. **Decide and justify.** The UI needs a 24 px dumbbell icon that must work in light and dark.
   *Answer:* prefer a monochrome vector path rendered by the icon mechanism (tinted with a
   `-md-sys-color-*` look-up so it follows the theme); if it must be a file, a transparent PNG in a
   neutral colour with the tint applied in code — never a coloured PNG baked for one theme.
4. **Explain the `.svg` rule.** A teammate adds `icon-equipment-bike.svg` and reports "the image is
   missing". What happened, and what are the two acceptable fixes? *Answer:* JavaFX's `Image` supports
   BMP/GIF/JPEG/PNG (not SVG), so the load silently produces nothing; fix by either copying the path
   data into the icon mechanism or exporting a PNG and loading that, keeping the SVG as the source.
5. **Licence reasoning.** You find a nice empty-state drawing under CC BY 4.0. May it be committed?
   *Answer:* yes, with attribution (the licence requires it) — record it in the §5 table and keep the
   licence text; if the team prefers to avoid attribution obligations, draw a simple original instead.

## 9. Ownership and tasks

| Task | Owner | Status |
|------|-------|--------|
| Folder created with `.gitkeep` | Abdelrhman | Done |
| Logo + placeholder avatar | Abdel Raouf | To Do |
| Equipment category icons (source + PNG if needed) | Abdel Raouf | To Do |
| Empty-state art for the lists that can be empty | Abdel Raouf | To Do |
| Decide and log the high-DPI strategy (`@2x` file vs one large source) | Abdel Raouf | To Do |
| Record the licence of every non-original asset in §5 | each owner | Ongoing |

### Definition of done for an asset in this folder

- [ ] Named by the convention, one purpose, smallest useful size, transparent where the theme requires it.
- [ ] Loadable through the documented path with a `null` fallback in code.
- [ ] Verified in light and dark, and at the smallest size the UI displays it.
- [ ] Licence recorded if it is not original work.
- [ ] The old asset (if it replaced one) is deleted in the same PR, and no FXML or controller still
      references it (`grep -r "<old-file-name>" src/` returns nothing).
