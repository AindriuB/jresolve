# jresolve

<!-- HARD CAP: 50 lines. This file is a router, not a manual.
     If you are adding detail here, it belongs in docs/ instead. -->

A Java 8 library for probabilistic entity resolution: match an arbitrary source
object `S` against candidate objects `C` on fuzzy field evidence, and return a
ranked, explainable decision. Maven multi-module. No framework, no JSON, no
database, no logging dependency — `docs/architecture.md` says why each of those
is a boundary rather than a preference.

## Read on demand, not up front

| Need | File |
|---|---|
| How we work (the loop, roles, parallelism) | `docs/workflow.md` |
| Code style, naming, commit format | `docs/conventions.md` |
| Module shape, boundaries, the type model | `docs/architecture.md` |
| Why the design differs from the original spec | `docs/design-decisions.md` |
| The original specification, unedited | `docs/spec/original-design.md` |
| What is open, in priority order | `docs/plan/PLAN.md` |
| What was already built — scan, never open `HISTORY.md` whole | `docs/plan/HISTORY-INDEX.md` |
| One task's full contract | `docs/plan/tasks/<id>.md` |

Load exactly one of these when the task needs it. Do not preload the set.
`docs/spec/original-design.md` is 3.5k lines: read a numbered section, never the
whole file. Where it and `docs/design-decisions.md` disagree, the latter wins.

## Roles

Delegate rather than doing it inline; each agent returns a conclusion, not a
transcript. Seven roles — `explorer`, `architect`, `planner`, `implementer`,
`tester`, `reviewer`, `scribe` — defined in `~/.claude/agents/` and shared by
every project. `docs/workflow.md` describes what each is for.

## The loop

`/plan <goal>` → `/fanout` → `/verify` → `/record`. See `docs/workflow.md`.
Out of band, any time: `/recon <question>`, `/design <question>`.

## Rules that hold everywhere

1. One task = one worktree = one branch. Never two agents in one tree.
2. A task file names the files it owns. Editing outside that set is a bug —
   stop and report instead.
3. Never paste file contents into a summary. Cite `path:line`.
4. Prefer `rg` over `grep`, and read ranges over whole files.
5. Java 8 is a hard target, not a preference. `mvn verify` enforces it.
6. No real personal data anywhere — test fixtures are synthetic.
7. Only `implementer` writes code. `scribe` writes the planning record —
   `PLAN.md`, `HISTORY.md`, `HISTORY-INDEX.md`, task files. A *design artefact*
   — a doc whose content is a design output and which code cites, such as
   `docs/calibration.md` — is written by the role that owns the design, because
   its content is decided by whoever derived it rather than by whoever records
   the work.
