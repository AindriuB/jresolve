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

- [x] **03 — Normalization primitives.** Unicode/case/token normalization used
  by `FieldPipeline.prepare`. Merged after 3 attempts; see `docs/plan/HISTORY.md`.

- [x] **04 — Core value types.** `ComparisonCategory`, `Score`/`ScoreScale`,
  `FieldEvidence`, `MatchEvidence` and related immutable types. Merged after 3
  attempts; see `docs/plan/HISTORY.md`.

- [ ] **05 — Field layer.** `FieldDefinition`, `FieldPipeline`, field
  comparators built on 02-04. Unblocked — 02, 03, 04 all merged. Wave 3,
  concurrent with 06.

- [ ] **06 — Scoring and decision.** Fellegi-Sunter and logistic scorers,
  decision engine, threshold/margin validation at `build()`. Unblocked — 04
  merged. Wave 3, concurrent with 05. Carries a note from 04's review about
  `Score.algorithm` validation — see the task file.

- [ ] **07 — Resolver and builder.** `EntityResolver`, its builder, candidate
  cost-ordering and short-circuit. Blocked on 05, 06. Wave 4 on its own.

- [ ] **08 — End-to-end test.** Full-pipeline test exercising resolver against
  a synthetic fixture. Blocked on 07. Wave 5 on its own.

## Notes for implementers

- `jresolve-core` build needs a JDK 17 toolchain on the building machine —
  `~/.m2/toolchains.xml`, not part of the repo. See
  `docs/architecture.md#building`.
- `mvn clean verify` baseline at wave 2's close (01-04 merged) is 142 tests
  across 21 test classes, `BUILD SUCCESS`. Expect it to grow as later tasks
  land.

## Known gaps (non-blocking, no task owns these)

Raised in review during wave 2 and left unfixed because fixing them meant
editing a file outside the reviewing task's `Owns`. Whoever next touches these
files should close them in passing rather than reopen the review:

- `JaroWinklerSimilarity` (task 02): the `maxPrefixLength` and
  `boostThreshold` constructor guards have no automated test — deleting either
  survives the suite. Verified only by a hand-written scratch class during
  review, not committed.
- `MatchEvidence.toString()` (task 04): the `evidence == null` branch is dead
  code now that the constructor rejects null values in the map. A later reader
  may infer nulls are possible from the branch's presence; delete it or make
  it a documented invariant check.
- `UnicodeFormNormalizerTest:28` (task 03): feeds a precomposed literal to the
  NFD test. It passes today but proves nothing under an NFD-normalizing
  editor pass — the reviewer judged a fourth review round not worth it for
  this one. See `docs/plan/tasks/retired/03-normalization-primitives.md`
  attempt 2 for the related (fixed) defect in the sibling NFC test.
- `Score.algorithm` (task 04): unvalidated, may be null. Task 06 is expected
  to stamp it with a stable identifier in practice; see the note in
  `docs/plan/tasks/06-scoring-and-decision.md`.
