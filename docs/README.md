# `docs/` — documentation index

This folder holds **every** design and learning document for the Gym Management System. Two rules keep
it usable:

1. **One copy of each fact.** A class's field list lives in `docs/UML.md` only (`DECISIONS.md` 28).
   Folder READMEs explain *why* and *how*; they never become a second specification.
2. **Every document names its owner and its authority.** See the table below.

## 1. Which document answers which question

| Question | Document |
|---|---|
| What are we building, with what, and who owns it? | [`../README.md`](../README.md) |
| What exactly does each class look like (fields, methods, types)? | [`UML.md`](UML.md) §1 (domain), §3 (DAOs), §4 (services), §5 (controllers), §6 (utilities) |
| What are the rules the whole team must implement identically? | [`UML.md`](UML.md) → "Business rules" (19 rules) |
| Why is it built this way? | [`DECISIONS.md`](DECISIONS.md) (41 numbered decisions) |
| How do I learn this project / prepare the defence? | [`LEARNING_GUIDE.md`](LEARNING_GUIDE.md) |
| How do I write or extend these docs without breaking them? | [`DOCS_STANDARD.md`](DOCS_STANDARD.md) |
| How do I build, run and debug it? | [`BUILD_AND_RUN.md`](BUILD_AND_RUN.md) |
| What is actually finished? | [`COVERAGE.md`](COVERAGE.md) |
| Who does what, and what is the status? | [`UML_TEAM.md`](UML_TEAM.md) |
| How do I write *this layer*? | the folder README: [`model`](../src/main/java/com/gym/model/README.md) · [`dao`](../src/main/java/com/gym/dao/README.md) · [`service`](../src/main/java/com/gym/service/README.md) · [`controller`](../src/main/java/com/gym/controller/README.md) · [`util`](../src/main/java/com/gym/util/README.md) · [`resources`](../src/main/resources/README.md) — the last links one sub-doc per asset kind: [`fxml/`](../src/main/resources/fxml/README.md) · [`db/`](../src/main/resources/db/README.md) · [`theme/`](../src/main/resources/theme/README.md) · [`images/`](../src/main/resources/images/README.md) |
| What is the entry point, and what happens at startup and shutdown? | [`../src/main/java/com/gym/README.md`](../src/main/java/com/gym/README.md) (`MainApp`) |
| How do the m3fx library modules work? (not the gym app) | [`../PLAN.md`](../PLAN.md), [`../AGENTS.md`](../AGENTS.md), [`COVERAGE.md`](COVERAGE.md), [`SPEC_NOTES/`](SPEC_NOTES) |

## 2. Authority when two documents disagree

`UML.md` (names, types, signatures, rules) → `DECISIONS.md` (rationale) → `UML_TEAM.md` (ownership) →
folder READMEs (teaching). **The gym-app documentation never overrides `UML.md`.** If the design must
change, change `UML.md` first, then `DECISIONS.md` if the change was a real choice, then the READMEs
that mention it. The full policy, including how to write a folder README, is in
[`DOCS_STANDARD.md`](DOCS_STANDARD.md).

## 3. Recommended reading order

1. [`../README.md`](../README.md) — the product in 15 minutes.
2. [`UML.md`](UML.md) §2 — the four layer rules (memorize them).
3. [`LEARNING_GUIDE.md`](LEARNING_GUIDE.md) §2 — the study path for your week.
4. The folder README for the layer you are writing, while you write it.
5. [`DECISIONS.md`](DECISIONS.md) — before you ask "why is it like this?" out loud.
6. [`BUILD_AND_RUN.md`](BUILD_AND_RUN.md) — before your first build.
7. [`../AGENTS.md`](../AGENTS.md) — only if you touch the `m3fx` library modules.

## 4. Housekeeping rules for this folder

- **Do not add a second copy of a diagram** (`DECISIONS.md` 28). Link to `UML.md` instead.
- **Mermaid only if it renders.** A broken fence is invisible, so validate syntax before committing
  (`DECISIONS.md` 29).
- **Numbered decisions are append-only.** Never renumber `DECISIONS.md`; supersede an entry with a new
  one that says which it replaces (see 41, which closes the deferral in 7).
- **Every new document must be linked from this index** and from the root `README.md` §10.
- **Status is not a promise.** Anything not ticked in `COVERAGE.md` is planned work — say so in the
  first paragraph of any document that describes it.

## 5. Files in this folder

| File | Owner | Purpose |
|---|---|---|
| `UML.md` | All (single source of truth) | The design: domain model, layers, 19 business rules |
| `UML_TEAM.md` | Abdelrhman | Ownership + status overlay only (no diagrams) |
| `DECISIONS.md` | All (append-only) | Why each non-obvious choice was made |
| `COVERAGE.md` | Abdelrhman | Honest built-vs-planned checklist (library + app) |
| `LEARNING_GUIDE.md` | Abdelrhman | Study path, OOP concept index, glossary, 40 defence questions |
| `DOCS_STANDARD.md` | Abdelrhman | The template and rules the folder READMEs follow |
| `BUILD_AND_RUN.md` | Abdelrhman | Gradle files explained, commands, troubleshooting |
| `SPEC_NOTES/` | m3fx work | m3fx component spec extracts — **not** gym-app documentation |
