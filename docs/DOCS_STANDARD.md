# DOCS_STANDARD.md — how the `com.gym` folder documentation is written

> **Who this is for:** every team member who edits a folder `README.md`, and any AI assistant asked
> to extend the docs. Read this file before touching `model/README.md`, `dao/README.md`,
> `service/README.md`, `controller/README.md`, `util/README.md`, `src/main/resources/README.md` or
> any sub-folder README.
>
> **Why it exists:** the folder READMEs used to be task trackers ("who writes what, status: To Do").
> They said *what* to build but never *why* it is built that way or *how* to write it, so a reader
> could not learn the design from them and a writer had to re-derive every decision. The expanded
> READMEs are now the **learning material** for this project: someone who reads all six layer docs
> should be able to explain the architecture, defend every design choice, and write the code.

---

## 1. Authority: who wins when two documents disagree

| Rank | Document | Authority over |
|---|---|---|
| 1 | `docs/UML.md` | Class names, fields, types, method signatures, multiplicities, enumerations, the 19 business rules, layer rules |
| 2 | `docs/DECISIONS.md` | *Why* a non-obvious choice was made (numbered decisions, e.g. "`DECISIONS.md` 31") |
| 3 | `docs/UML_TEAM.md` | Ownership and weekly status only |
| 4 | Folder `README.md` files (this standard) | Teaching: purpose, rationale, how-to, examples, pitfalls |
| 5 | `PLAN.md`, `AGENTS.md` | The `m3fx` library work, **not** the gym app |

Consequences you must respect while writing:

1. **Never invent a field, method, enum literal, or business rule.** If the app needs one, the change
   goes into `docs/UML.md` first (and `DECISIONS.md` if it was a real choice), then into the READMEs.
2. **Never contradict `UML.md`.** If you believe the design is wrong, write the objection in
   `DECISIONS.md` — do not silently document different behaviour.
3. **Cite instead of repeating.** Where a rule matters, quote its number: "`UML.md` rule 17
   (`bookedCount` counts `CONFIRMED` only)". Where a decision matters, quote it: "`DECISIONS.md` 33".
4. **`m3fx` is out of scope.** It is an external library that supplies the theme and the `M3*`
   controls. Reference only what the app calls — `M3Stylesheets.applyTo(scene, theme)`,
   `M3Theme`, `-md-sys-color-*` lookups, `FxUtils`-style dialogs. **Never document m3fx internals**,
   its tokens, its skins, or its roadmap: that is the library's own documentation (`docs/SPEC_NOTES/`,
   `docs/COVERAGE.md`).

---

## 2. Required structure of a layer README

Every folder README uses these sections, in this order, with these headings (a folder with fewer
files may merge sections 2 and 3, but must not drop them):

```markdown
# `<folder>` — <one-line layer name> (JavaFX branch)

> **Signature authority:** `docs/UML.md` §<n> owns every name, type and signature in this folder.
> This README explains *why* each one exists and *how* to write it. Code here is an illustrative
> sketch, not the specification — if a sketch and `UML.md` disagree, `UML.md` wins.

## 1. Purpose — the problem this layer solves
## 2. What you learn here (OOP concepts)
## 3. Design patterns in this layer
## 4. Rules you must follow (and the bug each rule prevents)
## 5. The files in this folder            <-- the bulk: one entry per file
## 6. How this layer connects to the others
## 7. Common mistakes and review checklist
## 8. Learning exercises (with answers)
## 9. Ownership and tasks                  <-- keep the existing tables, do not delete history
```

### Section 1 — Purpose
- One paragraph: what this layer *is*.
- A **"What breaks if this layer is missing"** paragraph — the concrete failure (e.g. "SQL inside a
  controller means every screen holds its own connection string and no query can be reused or
  tested").
- A short note on what this layer deliberately does **not** do (its boundary).

### Section 2 — What you learn here (OOP concepts)
A table with one row per concept. Columns: **Concept** · **Where in this folder** · **Why the project
needs it** · **How to apply it (one line)**. Use the concept names from
`docs/LEARNING_GUIDE.md` → "OOP concept index" so the two documents cross-link. Cover, where they
genuinely apply: encapsulation, abstraction, interfaces, polymorphism, inheritance (and why it is
*absent* here — see `UML.md` §7), composition, enums as types, immutability, value objects,
information hiding, exception design, generics, collections, static vs instance members,
dependency inversion, single responsibility.

### Section 3 — Design patterns in this layer
A table: **Pattern** · **Where** · **Why this pattern** · **What goes wrong if used carelessly**.
Name the pattern honestly (DAO, Factory Method, Value Object, Facade, Dependency Injection, MVC,
State machine over an enum, Template Method *rejected*, Singleton *avoided*). If a pattern was
considered and rejected, say so and link `DECISIONS.md` 36.

### Section 4 — Rules
Numbered "must / must-not" list. **Every rule states the consequence it prevents**, e.g.
"Money is `BigDecimal`, never `double` — `double` cannot represent `0.1` exactly, so summing receipts
drifts and a revenue report stops reconciling (`DECISIONS.md` 31)."

### Section 5 — The files in this folder
This is the main body. **One entry per planned file.** Use the entry template in §3 below.
Order the entries the way the reader should write them (foundation first), and say which order that is.

### Section 6 — How this layer connects
- The call flow as a numbered list (who calls whom, in what order) — e.g. "button → controller →
  service → DAO → SQLite → back up as an entity".
- A small table of the layer's dependencies: **depends on** / **is used by**.
- One honest note on the layer's *seam* (the interface that makes it swappable).

### Section 7 — Common mistakes and review checklist
- "Mistakes we have actually seen" (or realistically expect): each with the symptom and the fix.
- A tick-box **review checklist** that a reviewer runs before approving a PR that touches this folder.

### Section 8 — Learning exercises
- 3–6 exercises per folder, increasing in difficulty: *read the code and answer*, *write this small
  piece*, *find the bug in this snippet*, *extend without breaking rule X*.
- Answers in a collapsed `<details>` block, or a separate short "Answers" sub-section at the end.
- Keep exercises verifiable without the app running where possible (they should be answerable from
  the docs and the code).

### Section 9 — Ownership and tasks
Keep the existing ownership tables and status column exactly as they are (they are the team's
tracker). Add a **"Definition of done for a file in this folder"** checklist underneath.

---

## 3. Per-file entry template

Use this for every class/file. Keep the heading format so files can be linked: `### \`Customer.java\``.
Target length **40–90 lines** for a substantial class, **15–30 lines** for a trivial one. Do not pad;
every line must teach something.

```markdown
### `Customer.java` — <what it represents in the domain> (owner: <owner from UML_TEAM.md>)

**Status:** planned / exists as placeholder / done.
**Canonical:** `docs/UML.md` §1 (fields + constructors), rule <n> if a rule touches it.

**What it is.** Two or three sentences in plain language, as if explaining to a new teammate.

**Fields.** A table — **Field** · **Type** · **Nullable?** · **Why this type/name**.
Highlight the interesting ones (nullable FK, `BigDecimal`, boxed vs primitive, derived value).

**Why it is designed this way.** The *reasoning*: what alternative was rejected and which bug or
awkwardness that rejection avoids. Cite `DECISIONS.md` where a decision exists.

**How to write it.** Numbered steps in the order you actually type them (class declaration → fields →
constructors → getters/setters → `equals`/`hashCode`/`toString` → anything else), with the reason for
each step. Say where the file goes and what it must not import.

**Sketch.** A short Java block (10–35 lines). Mark it clearly as a sketch:
`// Sketch — field list is authoritative in docs/UML.md §1`.

**How to know it is right.** The checks a human runs: what to compile, what to print, which rule to
test by hand.

**Common mistakes.** 3+ bullets: the specific error, why it happens, how to fix it.
```

### Code-sketch policy

- Java 21+ syntax is fine (records for *internal* helpers, `switch` expressions, `var` only inside a
  method body). **Entities are plain classes with getters/setters** — `UML.md` §1 shows the
  constructors, so do not turn an entity into a record.
- Sketches must compile *in spirit*: correct imports, correct types, no invented methods.
- Comment the *why*, not the *what*: `// FK only, never a Customer object (keeps this layer JDBC-free)`.
- Never paste m3fx internals, never paste Android/web code, never invent SQLite syntax that SQLite
  does not accept (check `sqlite.org/lang.html` mentally: `AUTOINCREMENT`, `CHECK`, `PRAGMA`,
  `INSERT OR REPLACE`, `COALESCE`, `DATE()`/`strftime()` are all fine).
- Money examples: `new BigDecimal("45.00")` — always the `String` constructor, never `new BigDecimal(45.00)`
  (which carries binary-float error). This is a teaching point; call it out.

### Writing long files (for authors and assistants)

If the README is too long for a single write, do **not** shorten it to fit — write it in parts:

1. Write the file with a marker at the end: `<!-- PART-MARKER -->`.
2. Then repeatedly `edit` the file, replacing `<!-- PART-MARKER -->` with the next chunk of content
   followed by the marker again.
3. Remove the marker in the final edit.

Never leave a marker in a committed file.

---

## 4. Voice and formatting

- **Language:** English, matching the rest of the repository.
- **Person:** second person for instructions ("write the getter"), first person plural for team
  decisions ("we keep money as `BigDecimal`"). No marketing language, no hype.
- **Sentences:** short. One idea per sentence. If a sentence needs a comma-splice, split it.
- **Explain before naming:** say what a thing is for, then give its name.
- **Define jargon on first use** (JDBC, prepare-statement, DAO, FK, XOR, PBKDF2, FX thread), then use
  it freely. The glossary is `docs/LEARNING_GUIDE.md` → "Glossary".
- **Tables over paragraphs** for anything enumerable (fields, methods, states, owners, checks).
- **Bold the rule, not the whole sentence.**
- **File names in backticks**, always with the extension: `PaymentDaoImpl.java`.
- **Cite rules unambiguously.** `rule N` always means the canonical business rule N in
  `docs/UML.md` → "Business rules" (there are 19; there is no rule 20). A folder README may keep its
  own numbered rule list in §4 — refer to those as **`§4 rule N`** (or "the rule about …") so the two
  numbering systems can never be confused. Same for decisions: always `DECISIONS.md N`.
- Diagrams: only small Mermaid `flowchart`/`sequenceDiagram` blocks with plain syntax, and only when
  a picture genuinely beats a numbered list. Large structural diagrams stay in `docs/UML.md` (one
  copy only — `DECISIONS.md` 28).
- Do not use emoji except the existing ⚠️ convention when flagging a real trap.

---

## 5. Checklist before you commit a README

- [ ] Follows the §2 section order and headings.
- [ ] One entry per planned file in the folder, in write-order, with the §3 template.
- [ ] Every field, method, enum literal and rule matches `docs/UML.md` (spot-check the names).
- [ ] Every non-obvious choice has a *because* sentence, and cites `DECISIONS.md`/rule number where one exists.
- [ ] Every file entry has: purpose, why, how-to steps, sketch, verification, ≥3 mistakes.
- [ ] Money is `BigDecimal` everywhere it appears; dates are ISO-8601 strings; no `double` money.
- [ ] `m3fx` appears only as an external dependency (theme + components), never as documented internals.
- [ ] Ownership names are spelled as in `docs/UML_TEAM.md` (Abdelrhman, Ziad, Yousef, Abdel Raouf) and
      the `swing`-branch note is preserved where the original had it.
- [ ] No invented UI screens, no invented tables, no invented methods.
- [ ] Links resolve (relative links from the README's own folder; `docs/...` links from the repo root).
- [ ] Existing task/status tables preserved.
- [ ] No `<!-- PART-MARKER -->` left behind.
