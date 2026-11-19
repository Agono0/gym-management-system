# docs/agents/STATE.md — run state

**Phase: P0 done → P1/P2/P3/P4 running as queued rounds.** Queue: `QUEUE.md` (authoritative
task list + round log). Audit details: `M3FX_AUDIT.md`, `APP_MAP.md`. Open items:
`OPEN_QUESTIONS.md`.

## Roles

- **Orchestrator** (main session): owns `docs/agents/*`, claims/review/merge, `./gradlew build`
  gate, pushes to origin. Does not implement lane tasks.
- **Worker A** (worktree `../gym-wt-A`, branch `agent/core`): lane A (m3fx-core), pools elsewhere.
- **Worker B** (worktree `../gym-wt-B`, branch `agent/controls`): lane B (m3fx-controls).
- **Worker C** (worktree `../gym-wt-C`, branch `agent/app`): lane C (gym app UI).

## What works (verified 2026-11-20)

- `./gradlew build` green at start commit; `:m3fx-core:test` 8 green; `:m3fx-controls:test`
  158 green. `runGym`/`runSamples` launch with zero exceptions.
- All 19 screens Java-built, zero FXML loading (20 .fxml archived, never delete).
- Theme pipeline seed→scheme→CSS works incl. dark toggle.

## What's broken / tracked

Everything in `M3FX_AUDIT.md` → mapped to tasks in `QUEUE.md`.

## Gates used at merge time

1. Worker-side: `./gradlew compileJava compileTestJava` + module tests green in worktree.
2. Orchestrator-side per merge: full diff review vs §9 checklist + anti-slop list; rebase onto
   main; `./gradlew build`; smoke-launch changed app (`runGym`/`runSamples`) — stderr clean
   (no exceptions, no CSS warnings, no unknown-icon warnings); screenshots for UI changes via
   `./gradlew screenshots`; then rebase-push to origin/main.
3. Never merge red. A worker's failing check = that worker fixes before re-submit.

## Verification evidence log

| Round | Gate | Result |
|---|---|---|
| 0 (baseline) | `./gradlew build` | green (pre-existing state) |
| 1 merges | `./gradlew build` after each ff-merge; `:m3fx-core:test` 23 green; `:m3fx-controls:test` 168 green | pushed a7f23de |
| 1 visual | customer-list screenshot on merged main | real 20-member roster, live KPIs, loading/empty states work; NEW finding: M3StatCard labels/badges truncate unreadably at 1024/1280 → task B10 |

## Round-1 learnings (workers, re-read)

- `M3LinearProgress` indeterminate was doubly broken (SPLINE crash + invisible bars) — fixed in
  B1; `UiKit.loadingState()` still uses `M3CircularProgress`, may switch back in C1 (polish).
- Worker A proved the vendored engine's pinned `ColorSpec2026` is upstream mcu-main design —
  OPEN_QUESTIONS #6 superseded; the genuine gap is only the unreachable API, now exposed (A1).
- Test harness: closing the last shown Stage kills the shared FX thread (implicit exit) —
  always `Platform.setImplicitExit(false)` when a test shows a Stage.
- First run on an empty DB seeds synchronously in the load task — screenshot settle may catch
  the loading state (re-run once seeded).

## Workflow rules (agents, re-read every task)

- Branch/worktree hygiene: claim → implement to completion → compile+tests → commit
  (conventional) → report to Orchestrator → wait for merge; before starting a new task,
  `git -C <worktree> rebase main` to pick up merged work.
- Spec-first: read the m3.material.io page + write `docs/SPEC_NOTES/<item>.md` before coding;
  unverifiable values → `// [VERIFY]` + OPEN_QUESTIONS.md.
- Tokens only in `m3fx-*`; no new runtime deps; no invented business rules (UML.md rules 1–19);
  no edits outside your task's files; `.fxml` files untouchable; honest reporting.
- Anti-slop (§3 of the prompt): no emoji icons, no lorem/fake filler, one primary action per
  region, real states (loading/empty/error/disabled), sentence-case verb copy, Material Symbols
  only, no hard-coded colors/radii/durations.
