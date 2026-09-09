# Plan

Milestone 1 — the core engine skeleton and its first end-to-end path. Eight
tasks, four waves. Wave order below reflects dependency edges recorded in
`docs/plan/tasks/*.md` at planning time (milestone plan commit `8e0182f`).

## Milestone 1

- [x] **01 — Maven build skeleton.** Reactor build (`jresolve-core`,
  `jresolve-profiles-ie`), JDK 17 toolchain, `release=8`, animal-sniffer
  `java18` gate, JUnit 5 + AssertJ. Everything else in this milestone compiles
  against it. Merged; see `docs/plan/HISTORY.md`.

- [x] **02 — Similarity metrics.** Jaro-Winkler, Levenshtein-derived metrics
  with property tests (bounded `[0,1]`, `sim(x,x)==1`, exact symmetry). Merged;
  see `docs/plan/HISTORY.md`.

- [ ] **03 — Normalization primitives.** Unicode/case/token normalization used
  by `FieldPipeline.prepare`. No blocker; wave 2, concurrent with 02 and 04.
  In flight on attempt 3.

- [ ] **04 — Core value types.** `ComparisonCategory`, `Score`/`ScoreScale`,
  `FieldEvidence`, `MatchEvidence` and related immutable types. No blocker;
  wave 2, concurrent with 02 and 03. In flight on attempt 3.

- [ ] **05 — Field layer.** `FieldDefinition`, `FieldPipeline`, field
  comparators built on 02-04. Blocked on 02, 03, 04. Wave 3, concurrent with 06.

- [ ] **06 — Scoring and decision.** Fellegi-Sunter and logistic scorers,
  decision engine, threshold/margin validation at `build()`. Blocked on 04.
  Wave 3, concurrent with 05.

- [ ] **07 — Resolver and builder.** `EntityResolver`, its builder, candidate
  cost-ordering and short-circuit. Blocked on 05, 06. Wave 4 on its own.

- [ ] **08 — End-to-end test.** Full-pipeline test exercising resolver against
  a synthetic fixture. Blocked on 07. Wave 5 on its own.

## Notes for implementers

- `jresolve-core` build needs a JDK 17 toolchain on the building machine —
  `~/.m2/toolchains.xml`, not part of the repo. See
  `docs/architecture.md#building`.
- `mvn clean verify` baseline at task 01's close was roughly 7-8 seconds from
  clean; expect it to grow as later tasks land.
