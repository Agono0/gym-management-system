# AGENTS.md — Gym Management System (multi-agent charter)

This repo is worked on by **multiple AI agents in parallel**. Read this whole file before
every task. It defines what the project is, the rules every agent must follow, and the exact
workflow for getting code from your branch into `main`.

> **Reality note (repo state, keep this section accurate when the repo changes):**
> - Build is **Gradle** (`./gradlew`, Java 21 bytecode on JDK 27), not Maven.
> - The app is the Gym Management System: `src/main/java/com/gym` with full layers
>   (`model`, `dao`, `service`, `controller`, `exception`, `util`) and a CLI module
>   (`com.gym.cli`), backed by SQLite (`gym.db`).
> - **Views are Java-built, not FXML.** Every screen controller constructs its own node tree
>   via the `com.gym.controller.Screen` registry (`ScreenController` interface). The 20
>   `.fxml` files in `src/main/resources/fxml/` are **kept on disk but are never loaded** —
>   do not delete them and do not reintroduce `FXMLLoader` into the app code.
> - `m3fx-core` and `m3fx-controls` are the in-repo Material Design 3 library subprojects the
>   GUI consumes; reference repos (`material-color-utilities-main/`, `material-web-2.5.0/`)
>   sit at the repo root and are **read-only**.
> - `submission/` is the standalone CLI hand-in copy of the project — it is built and edited
>   independently of `src/`; keep it compiling on its own.
> - The per-layer READMEs (`src/main/java/com/gym/README.md` and each layer folder, plus
>   `src/main/resources/README.md` and `docs/`) are learning documentation. `docs/UML.md`
>   is the single source of truth for class names, signatures and the business rules. When a
>   task touches a layer, read that layer's README and keep it in sync in the same change.

---

## 1. Non-negotiables (every agent, every task)

1. **Do not delete the `.fxml` files.** They are intentionally retained. The app must keep
   loading zero FXML — never add `FXMLLoader`, `javafx.fxml` imports, or `/fxml/` resource
   loads back into `src/main/java`.
2. **Do not edit** reference repos (`material-color-utilities-main/`, `material-web-2.5.0/`),
   `m3fx-*` reference material, or anything in `backups/`, `lectures/`, `skills/`,
   `submission.rar` without an explicit instruction.
3. **Tokens, not literals** inside `m3fx-*`: no hardcoded colors, radii, font sizes or
   durations in library controls — everything comes from the theme/token system.
4. **No new dependencies** without explicit approval from the user.
5. **Do not invent behavior.** Where the spec or `docs/UML.md` is silent, ask — never guess
   business rules. The 19 rules in `docs/UML.md` are authoritative; the UI must not
   re-implement them, only call the services.
6. **Honest reporting.** If a build or run fails, report it with the actual output. Never
   claim done what was not run.

## 2. Code quality — "high-quality" means all of this

Before you consider any task finished, the code must:

- **Compile clean**: `./gradlew compileJava compileTestJava` (or the smallest task set that
  covers your change) with zero errors. Warnings you introduced must be fixed, not ignored.
- **Match the surrounding code**: same naming, comment density, formatting and idioms as the
  files you touch. One formatter style across the file, not per-edit mixing.
- **Follow the layer README** you are editing (they document per-file rationale and the
  patterns each layer uses) and keep it in sync when behavior changes.
- **Small, single-purpose changes**: no drive-by refactors of unrelated code, no commented-out
  blocks, no dead code, no TODOs without a stated plan.
- **Public API documented** (one-line Javadoc minimum) and named so a reader needs no
  explanation.
- **Actually run**: for UI work, launch the app (`./gradlew runGym` or `runSamples`); for CLI
  work, run `./gradlew runCli` or the submission's runner. A task is not done if it was not
  executed at least once.
- **Keep the test/verification bar**: if tests exist for the area you touch, they must pass;
  if you add behavior, add or extend verification for it.

## 3. Multi-agent workflow — how code reaches `main`

Every agent follows this exact sequence. **Do not skip steps.**

### Step 1 — Branch and build on the latest state

```bash
git switch main
git pull --rebase origin main      # never start from a stale main
git switch -c <type>/<short-task-name>   # e.g. feat/membership-freeze-dialog
```

One task = one branch. Keep branches short-lived (days, not weeks).

### Step 2 — Work to completion

- Implement the whole task on the branch. Commit in logical units with conventional
  messages: `feat(scope): summary`, `fix(scope): summary`, `docs(scope): summary`,
  `chore(scope): summary` (matching existing history).
- **Continue working until the task is finished** — an agent does not stop mid-task, and does
  not park a half-done change. If you are blocked, resolve the block (or report and hand off
  cleanly with a written state), but never leave the branch in a state that fails the build.

### Step 3 — Sync, resolve, then push (in this order)

**Before every push** (and again right before opening the PR):

```bash
git fetch origin
git pull --rebase origin main      # bring your branch up to the latest main
# ... fix any conflicts locally now, re-run the build to prove the merge is sound ...
./gradlew compileJava compileTestJava   # green before you push — always
git push origin <branch>
```

Pushing without pulling first is forbidden: conflicts must be fixed **on your branch before
they reach the repo**, never after. If the rebase is large, pull again right before the PR so
you never merge against stale knowledge.

### Step 4 — CI/CD checks gate the merge

- Open the pull request against `main`.
- **CI/CD must be green before anything merges.** While this repo has no workflow file yet,
  the *current* CI gate is the Gradle build itself — the same command a workflow would run:

  ```bash
  ./gradlew build
  ```

  It must pass with zero errors on the PR's merged state (re-run it locally after the final
  rebase). Once a GitHub Actions workflow is added, its green check replaces this step and
  becomes mandatory.
- **Never merge red.** If checks fail: stay on the task, fix, re-push, re-check — see Step 5.

### Step 5 — Merge, and keep working

- After checks are green, merge the PR into `main` (squash or merge-commit, matching the
  branch's commit discipline). Delete the branch after merge.
- **Do not go idle while CI runs or while a PR is open.** Continue productive work: start the
  next task on a fresh branch, fix an earlier finding, improve docs — but never start work
  that conflicts with an open PR on the same files.
- When CI reports a failure on your PR, the failure is yours: fix it on the same branch and
  re-push. A task is **finished** only when its code is merged into `main` with green checks
  and the working tree is clean.

## 4. Build & run commands

| Command | Purpose |
|---|---|
| `./gradlew build` | Full build — **the merge gate** |
| `./gradlew compileJava compileTestJava` | Fast compile check |
| `./gradlew run` | m3fx gallery (samples showcase) |
| `./gradlew runGym` | Full Gym Management System GUI shell |
| `./gradlew runCli` | CLI application |
| `./gradlew test` | Test suite, when tests exist for the touched area |

On Windows use `gradlew.bat` where the shell script is unavailable.

## 5. Pointers

- **`docs/UML.md`** — class names, field lists, signatures, the 19 business rules (source of
  truth). When code and UML disagree, fix both in the same change.
- **Layer READMEs** — `src/main/java/com/gym/README.md` + one per layer, and
  `src/main/resources/README.md`. Read before editing a layer; update in the same change.
- **`docs/DECISIONS.md`** — why the architecture looks the way it does. Add a short entry for
  every non-obvious decision you make.
- **m3fx component work** (when a task targets the library): extract spec numbers into
  `docs/SPEC_NOTES/<component>.md` before writing Java, implement as Control + Skin + CSS,
  use shared helpers (state layer, ripple, elevation, motion), and track scope in
  `docs/COVERAGE.md`. Never translate Android/web code line-by-line and never invent spec
  values — ask.
