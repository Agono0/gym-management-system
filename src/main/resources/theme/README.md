# `theme/` — app-level styling assets (JavaFX branch)

> **Signature authority:** `docs/DECISIONS.md` 4 (the theme is generated at runtime, not checked in) and
> `docs/UML.md` §2 step 1 (`MainApp` applies the theme before the first screen is shown).
> **Parent doc:** [`src/main/resources/README.md`](../README.md) — this file is the detail behind its
> `theme/` entry and must not contradict it.
> **Scope:** app-specific styling only. The palette, typography, shape and motion tokens belong to the
> external `m3fx` library, whose documentation lives in `docs/SPEC_NOTES/` — **not here**.

## 1. Purpose — the problem this folder solves

The application needs a look, and the look must change in one place. `theme/` is where the **app's own**
styling lives: the handful of rules that are about *this* window (layout chrome, a login backdrop, a
section-title class, the width of a settings column) rather than about Material Design 3.

Today the folder holds nothing but `.gitkeep`, and that is the correct state: **the M3 theme is generated
at runtime** by the library call `M3Stylesheets.applyTo(scene, theme)` (`DECISIONS.md` 4). There is no
checked-in `material-theme.css`, and there must not be one — a static file would be a second, silently
stale copy of the palette.

**What breaks if this folder is misunderstood.**

1. **A checked-in palette drifts.** Someone exports `material-theme.css`, commits it, and then the seed
   colour or the light/dark switch changes in code. The generated stylesheet updates, the committed file
   does not, and the two disagree about what "primary" is.
2. **A hex value in a screen kills dark mode.** `style="-fx-background-color: #FEF7FF"` looks correct in
   light mode and becomes a white rectangle in dark mode. Nobody reviewing the screen sees it, because
   the reviewer is in the other theme.
3. **Component rules end up here.** Restyling a button from this folder fights the library's own CSS and
   breaks the next time the library updates. Component look belongs to the component.
4. **Theme switching becomes a file swap plus a restart.** Users expect the light/dark toggle to repaint
   immediately; a design that reloads files cannot do that.

**What this folder deliberately does not do.** It does not hold colour values, font sizes, corner radii,
durations, component styles, or per-screen layout. It holds no Java, and nothing here is written at
runtime (resources inside the jar are read-only — see the parent doc §1).

## 2. What you learn here

| Concept | Where in this folder | Why the project needs it | How to apply it |
|---|---|---|---|
| Separation of concerns | `theme/` vs the library's generated stylesheet vs `fxml/` | Three owners, three files; a change to the palette cannot require editing 16 screens | App rule → here; palette → library; layout → FXML |
| Single source of truth | No `material-theme.css` exists | A second copy of a fact is a future contradiction (`DECISIONS.md` 28 makes the same argument for diagrams) | Never commit generated content |
| Indirection / late binding | `-md-sys-color-primary` (a looked-up colour) instead of `#6750A4` | The name resolves at render time, so one theme object repaints every node, including popups | Reference the *role name*, never the value |
| Information hiding | Screens know role names, not the palette | A screen that knows `#6750A4` also knows the brand; the theme is a design decision, not a screen decision | Style with `styleClass` |
| Data over branching | Light/dark is a `M3Theme` value passed to `applyTo`, not an `if (dark)` in each screen | A branch per screen is 16 places to get wrong; a value is one | Pass the theme; never branch on it in a screen |
| Declaration over inline configuration | `styleClass="m3-section-title"` in FXML | Inline `style=` cannot be themed, reused or reviewed | Add a class once, use it many times |
| Cascade and specificity | The stylesheet order set by the library call | Later stylesheets win; app rules must be able to override layout without fighting the palette | Keep app rules last and minimal |
| Naming as an API | `gym-app.css`, `m3-section-title` | File and class names are read by people in every other folder | Follow the naming tables |

## 3. Design patterns in this layer

| Pattern | Where | Why | If used carelessly |
|---|---|---|---|
| **Design tokens** (role names) | `-md-sys-color-*`, shape/type classes | The vocabulary between design and code | Token names invented per screen ("myBlue") destroy the shared vocabulary |
| **Facade** | `M3Stylesheets.applyTo(scene, theme)` | One call hides stylesheet generation and ordering | Calling the theme API per node instead of per scene |
| **Strategy (at the theme level)** | light/dark + seed chosen at runtime | Swapping the theme is passing a different object | A `ThemeManager` singleton that mutates global CSS |
| **DRY at the rule level** | One `gym-app.css`, one class per repeated rule | Repeated inline styles drift | Copy-pasting a 5-line inline style into 16 screens |
| **Do not do: static theme file** | — | It is a cache, and caches go stale | `material-theme.css` in git |

## 4. Rules you must follow (and the bug each rule prevents)

1. **No colour literal anywhere in the app** — not in FXML, not in an app stylesheet, not in Java.
   *Prevents:* a screen that is unreadable in dark mode (rule 2 above).
2. **An app stylesheet may reference only `-md-sys-color-*` look-ups** and app layout properties.
   *Prevents:* a second palette that no theme switch can reach.
3. **The theme is applied once per scene, by `MainApp`, before `stage.show()`.** *Prevents:* an unstyled
   first frame and popups that miss the look-ups (`../fxml/README.md`).
4. **Light/dark and the seed colour are runtime values**, not files and not a restart. *Prevents:* a
   toggle that lies to the user.
5. **Component-level styling is not written here** (button padding, field underline, ripple).
   *Prevents:* fighting the library's CSS and breaking on its next update.
6. **Never commit generated CSS or a palette export.** *Prevents:* drift plus a file nobody can edit
   safely (`DECISIONS.md` 4).
7. **Every new style class is added to the naming table in the parent README** in the same PR.
   *Prevents:* three spellings of the same class and dead rules nobody dares delete.
8. **An empty folder keeps its `.gitkeep`.** Git does not track directories. *Prevents:* the folder
   disappearing from a fresh clone and the docs pointing at nothing (parent doc rule 10).
9. **A toolbar/section style is a `styleClass`, never an inline `style=`** — FXML reviewer rule.
   *Prevents:* unreviewable styling and unthemed overrides.

## 5. The files in this folder

Write order: nothing to write today. If the app ever needs chrome rules, the order is
`gym-app.css` first (loaded last), then any further app stylesheet.

### `.gitkeep` — the placeholder that keeps the folder in git

**Status:** exists (0 bytes). **Canonical:** parent doc rule 10.

**What it is.** An empty file whose only job is to make Git keep an otherwise empty directory. It is
deleted in the same PR that adds the first real file.

**Common mistakes.** Deleting it "because the folder is empty" (the folder then vanishes from a clone);
putting content in it (it is not a README); leaving it *and* a real file behind (harmless, but confusing).

### `material-theme.css` — **a file that must not exist**

**Status:** must stay absent. **Canonical:** `DECISIONS.md` 4.

**What it is.** The tempting mistake: export the palette once, commit it, and load it by hand. It looks
convenient for exactly one afternoon.

**Why it is wrong.** The palette is a *function* of the seed colour, the variant, the contrast level and
the light/dark flag. A file can only be one output of that function. The moment any input changes, the
file and the running theme disagree, and the disagreement shows up as a control that keeps the old
colour — the hardest kind of visual bug to attribute, because the code that "sets the colour" is the file
nobody edited.

### `gym-app.css` — the app's own stylesheet, if and when it is needed

**Status:** planned only if a real need appears (**Open detail — decide + log in `DECISIONS.md`**).
**Canonical:** `DECISIONS.md` 4; parent doc §5 naming table.

**What it is.** The single place for chrome rules that are genuinely the app's: the login backdrop, a
section-title text class, a fixed-width sidebar, an empty-state block. It is registered **after** the
generated theme and the library stylesheet, so its layout rules win while its colour references still
resolve through the theme.

**How to write it.**

1. Only add the file when a rule is needed twice or cannot be expressed with a library class; otherwise
   prefer the existing `m3-*` classes.
2. Name it lower-case `kebab-case` with the app prefix: `gym-app.css`.
3. Put it in this folder and load it in `MainApp` **after** `M3Stylesheets.applyTo(scene, theme)`, one
   line, with a comment naming the order.
4. Write only layout/chrome properties; every colour is a `-md-sys-color-*` reference.
5. Add each new style class to the parent doc's naming table.

**Sketch — a legitimate rule and the counter-example beside it.**

```css
/* gym-app.css — LEGITIMATE: layout + a colour *reference*, no literal. */
.login-root {                       /* class used by login.fxml */
    -fx-background-color: -md-sys-color-surface;   /* resolves per theme */
    -fx-min-width: 420px;                          /* layout belongs to the app */
}
.gym-section-title {
    -fx-font-size: 1.125em;         /* if a token class does not fit, say why in the PR */
    -fx-text-fill: -md-sys-color-on-surface-variant;
}
```

```css
/* THE COUNTER-EXAMPLE — three separate mistakes in four lines. */
.root {
    -fx-background-color: #FEF7FF;              /* 1. literal: white rectangle in dark mode */
}
.m3-button {
    -fx-background-color: #6750A4;              /* 2. component rule: fights the library CSS */
    -fx-padding: 10px 24px;                     /*    and breaks when the library updates */
}
#saveButton { -fx-text-fill: white; }           /* 3. id selector: unreusable, unthemed */
```

**How to know it is right.**

1. Toggle light/dark at runtime: every colour in the app changes, and no rectangle stays light.
2. Grep the repository for `#` followed by three or six hex digits inside `src/main/resources` and
   `src/main/java` — the only hits may be in comments explaining why not to do it.
3. Resize the window: layout rules behave, and no rule scrolls away with the content.
4. Delete the file temporarily: the app still runs and still themes (only chrome spacing changes), which
   proves nothing essential leaked into it.

**Common mistakes.** Putting the palette here; styling library controls here; using `#id` selectors
instead of style classes; writing rules for one screen that another screen silently inherits; forgetting
to load the file (or loading it *before* the theme, so its look-ups resolve to nothing).

## 6. How this folder connects to the others

1. `MainApp.start(Stage)` builds the scene → calls `M3Stylesheets.applyTo(scene, theme)` (generated
   stylesheet) → optionally adds `gym-app.css` → then `stage.show()`. The order is the contract
   (`docs/UML.md` §2 step 1).
2. `fxml/*.fxml` names classes (`styleClass="login-root"`) and never colours
   ([`../fxml/README.md`](../fxml/README.md)).
3. Controllers switch the theme at runtime (a toggle calls `applyTo` with a new `M3Theme`) and never
   touch CSS files (`../../../java/com/gym/controller/README.md`).
4. Theme resources are packaged into the jar like every other resource; there is no writable theme folder
   at runtime (parent doc §1).

| This folder depends on | Who depends on this folder |
|---|---|
| The library's public theming API (`M3Theme`, `M3Stylesheets.applyTo`) | `MainApp` (loads it), every FXML file (uses its class names) |
| The parent doc's naming table | Reviewers (they check the table, not the file) |

## 7. Common mistakes and review checklist

| Symptom | Cause | Fix |
|---|---|---|
| One panel stays light in dark mode | a hex literal in that panel's style | replace with a `-md-sys-color-*` reference |
| The toggle changes some controls only | a stylesheet was applied to a node, or loaded before the theme | apply the theme to the `Scene`, once, first |
| A control's look "fights itself" | app CSS restyling a library control | delete the rule; change the library or use a different variant |
| Colours are dead (`-fx-text-fill` ignored) | the look-up name is misspelled and CSS fails silently | copy the name from the library's documented roles; verify visually in both themes |
| The same class has three spellings | no naming table update | one spelling, recorded in the parent doc |

**Review checklist.**

- [ ] No colour literal added anywhere in the app.
- [ ] Any new rule is chrome/layout, not a component restyle.
- [ ] The stylesheet (if any) is loaded **after** the generated theme.
- [ ] Each new class name is in the parent doc's naming table.
- [ ] Verified in light **and** dark, plus a different seed colour.
- [ ] No `.gitkeep` deleted while the folder is still empty; none left behind silently with a real file.
- [ ] `DECISIONS.md` updated if this change introduced or closed an open detail.

## 8. Learning exercises (with answers)

1. **Explain the missing file.** Why does the repository contain no `material-theme.css`, and what would
   have to be true for committing one to be safe? *Answer:* the theme is generated from
   (seed, variant, contrast, dark) at runtime (`DECISIONS.md` 4), so a file can only represent one
   combination; it would be safe only if the app froze all four inputs forever, which contradicts the
   runtime light/dark toggle.
2. **Find the bug.** A teammate adds `theme/material-theme.css` with
   `.root { -fx-background-color: #FEF7FF; }` and loads it from `MainApp`. What breaks, and in which
   theme? *Answer:* every surface in the app is forced to the light surface colour; in dark mode the
   window is a light rectangle with light-on-light text. It also becomes a fifth source of colour truth
   that the theme switch cannot reach.
3. **Write it correctly.** You need a two-column profile layout with a 320px sidebar. Write the CSS and
   say what belongs in FXML instead. *Answer:* the sidebar width is an app rule
   (`.profile-sidebar { -fx-min-width: 320px; -fx-pref-width: 320px; }`) and any colour is a look-up;
   the *structure* (a `HBox` with two children, the order, the labels) belongs in FXML.
4. **Reason about order.** Why must an app stylesheet be registered after the theme call, and what breaks
   if it is registered before? *Answer:* later stylesheets win in the cascade, and look-ups must already
   exist when the rules are applied; registering first means the app's rules are overridden by the
   generated ones and any look-up resolves against nothing during the first pass.
5. **Defend the rule.** Someone proposes a `ThemeManager` singleton holding the current palette so
   screens can ask for colours in Java. Give two reasons to refuse. *Answer:* (a) it is static mutable
   state beyond the one documented exception (`DECISIONS.md` 34) and it duplicates what the scene already
   resolves; (b) colours in Java bypass the stylesheet, so the theme switch cannot repaint what a screen
   painted — the exact bug look-ups exist to prevent.

## 9. Ownership and tasks

| Task | Owner | Status |
|------|-------|--------|
| Folder created with `.gitkeep` | Abdelrhman | Done |
| M3 theme wiring via `M3Stylesheets.applyTo(scene, theme)` in `MainApp` (Week 1, foundation) | Abdelrhman | To Do (blocked only by the overlay entry point, `DECISIONS.md` 35 — the theme call itself is not blocked) |
| Decide whether an app-level `gym-app.css` is needed at all, and log the answer | Abdelrhman | To Do |
| Document the light/dark + seed switch as a runtime preference | Abdelrhman | To Do |
| Add every new style class to the parent README's naming table | each owner | Ongoing |

### Definition of done for a file in this folder

- [ ] It is chrome/layout only, and every colour is a `-md-sys-color-*` reference.
- [ ] It is loaded after the generated theme, in `MainApp`, once per scene.
- [ ] Verified in light, dark and with a different seed colour.
- [ ] Its class names are recorded in `../README.md`, and no dead rule is left behind.
- [ ] The `DECISIONS.md` entry exists if the file's existence itself was a choice.
