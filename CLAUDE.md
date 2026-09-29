# jresolve

<!-- HARD CAP: 50 lines. A router, not a manual: detail belongs in docs/. -->

A Java 8 library for probabilistic entity resolution: match a source object `S`
against candidates `C` on fuzzy field evidence and return a ranked, explainable
decision. Maven multi-module; no framework, JSON, database or logging
dependency (`docs/architecture.md` says why).

## Read on demand, not up front

| Need | File |
|---|---|
| How we work (the loop, roles, parallelism) | `docs/workflow.md` |
| Code style, naming, commit format | `docs/conventions.md` |
| Module shape, boundaries, type model | `docs/architecture.md` |
| Why the design differs from the spec | `docs/design-decisions.md` |
| The original spec, unedited | `docs/spec/original-design.md` |
| What is open, in priority order | `docs/plan/PLAN.md` |
| What was built — scan; never open `HISTORY.md` whole | `docs/plan/HISTORY-INDEX.md` |
| One task's full contract | `docs/plan/tasks/<id>.md` |
| Building, testing, changing the code | `DEVELOPING.md` |
| Cutting a release (manual) | `RELEASING.md` |
| Tuning weights and thresholds | `docs/tuning.md` |

Load one when needed; do not preload. `docs/spec/original-design.md` is 3.5k
lines: read a numbered section only. Where it and `design-decisions.md`
disagree, the latter wins.

## Roles and the loop

Delegate; agents return a conclusion, not a transcript. Roles: `explorer`,
`architect`, `planner`, `implementer`, `tester`, `reviewer`, `scribe`
(`~/.claude/agents/`). Loop: `/plan` → `/fanout` → `/verify` → `/record`; any
time `/recon`, `/design`. Both described in `docs/workflow.md`.

## Rules that hold everywhere

1. One task = one worktree = one branch; never two agents in one tree.
2. A task file names the files it owns; editing outside it: stop and report.
3. Never paste file contents into a summary; cite `path:line`.
4. Prefer `rg` over `grep`; read ranges, not whole files.
5. Java 8 is a hard target; `mvn verify` enforces it.
6. No real personal data anywhere; fixtures are synthetic and say so. In
   `jresolve-core` use the wording `docs/conventions.md` prescribes, because
   the obvious phrasing trips `DomainVocabularyTest`.
7. Only `implementer` writes code. `scribe` writes the planning record —
   `PLAN.md`, `HISTORY.md`, `HISTORY-INDEX.md`, task files. A design artefact
   that code cites (e.g. `docs/calibration.md`) is written by the role that
   derived the design.
