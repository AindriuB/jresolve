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

- [x] **05 — Field layer.** `FieldDefinition`, `FieldPipeline`, field
  comparators built on 02-04. Merged on first attempt; see
  `docs/plan/HISTORY.md`.

- [x] **06 — Scoring and decision.** Rule-based scorer, threshold decision
  engine, threshold/margin validation at `build()`. Merged on first attempt;
  see `docs/plan/HISTORY.md`.

- [x] **07 — Resolver and builder.** `EntityResolver`, its builder, candidate
  cost-ordering and short-circuit. Merged after 2 attempts; see
  `docs/plan/HISTORY.md`.

- [ ] **08 — End-to-end test.** Full-pipeline test exercising resolver against
  a synthetic fixture. Unblocked — 05, 06, 07 all merged. Wave 5, last, next.

## Notes for implementers

- `jresolve-core` build needs a JDK 17 toolchain on the building machine —
  `~/.m2/toolchains.xml`, not part of the repo. See
  `docs/architecture.md#building`.
- `mvn clean verify` baseline at wave 4's close (01-07 merged) is 251 tests,
  `BUILD SUCCESS`. Expect it to grow as later tasks land.

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

Raised in wave 3 (tasks 05, 06):

- `Score.algorithm` (task 04): unvalidated at construction, may be null.
  Task 06's `RuleBasedScorer` stamps a public constant on every score it
  produces — that closes the gap for the one path that exists today, and is
  all task 06 can do from its side. The residual is a caller constructing
  `Score` directly with a null `algorithm`, which needs a null check in
  `result/Score.java` — a file task 04 owns and neither 06 nor 07 does.
  Task 07 could reject it at `build()`, but that still leaves direct
  construction open, so this is **not task 07's to close**. Deferred to
  milestone 2; whoever next touches `result/` should add the constructor
  guard then.
- `ExactFieldComparator` (task 05) derives its frequency key from
  `toString()`, so for an `N` with value equality but a default `toString`,
  two agreeing pairs produce different frequency keys. Latent today because
  no consumer uses the frequency key yet, but milestone 2's frequency
  adjustment (D5) depends on common values sharing a key — the whole point
  is that common-value agreement weighs less than rare-value agreement. The
  Javadoc should state a `toString`-consistent-with-`equals` requirement on
  `N` before that lands.
- `SimilarityBands.categoryFor` (task 05) is package-private while the class
  and its getters are public. A comparator written outside `field/` cannot
  reuse the banding logic and could reimplement it with silently different
  (e.g. exclusive) bounds. Worth resolving before a later milestone adds
  field types outside that package.
- `ScoredCandidate` (task 04) permits a null candidate. A single such
  candidate scored above `matchThreshold` makes `ThresholdDecisionEngine`
  (task 06) throw from `MatchResult`'s constructor rather than return a
  result — nothing in the planned pipeline produces a null candidate today,
  but the failure would surface in the wrong package if something did.
- `TokenSplitter` (task 02) still has no production implementation; task 05's
  comparators did not need one, so it remains implemented only by a test
  lambda. The risk of a second, independently-invented tokenising
  abstraction now moves to whichever task first adds a token-based field
  comparator — the address pipeline in a later milestone. Flagged in three
  consecutive waves now; land a real implementation somewhere durable so
  this stops being rediscovered.
- Two test-hygiene items in task 05: a comment in
  `FieldComparatorNullSafetyTest` describes its own failure mode backwards,
  and `@SafeVarargs` on a reifiable `Object[]...` in
  `ThresholdDecisionEngineTest` (task 06) is redundant.

Raised in wave 4 (task 07):

- **D6 is not fully honoured.** `MatchDecisionEngine` exposes neither its
  scale nor its thresholds, so `EntityResolverBuilder.build()`'s scale check
  validates the `DecisionThresholds` passed to `.thresholds(...)` against the
  scorer, but that object never reaches the engine that actually decides. A
  caller who constructs `new ThresholdDecisionEngine<>(thresholdsA)` and then
  calls `.thresholds(thresholdsB)` with a different, scale-matching instance
  gets a clean `build()` and a resolver that silently interprets scores on
  the wrong scale at `resolve()` — reproduced concretely as an engine holding
  PROBABILITY thresholds (0.9/0.5/0.2) returning MATCH by comparing a
  10.0-point score against 0.9. Task 07 could not fix this from `api/`:
  `MatchDecisionEngine` must stay an interface callers can implement
  themselves, `.decisionEngine(...)` is itself an acceptance criterion, and
  `ThresholdDecisionEngine.thresholds` is private with no getter. What
  shipped is a Javadoc contract (`.thresholds(...)` must be given the same
  instance as the engine's constructor) that both the tester and reviewer
  confirmed is sufficient when followed, but weaker than a construction-time
  exception. Milestone 2 should give `MatchDecisionEngine` a way to expose
  its scale (and ideally its thresholds) so `build()` can inspect the object
  that actually decides.
- `DefaultEntityResolver`'s constructor still has a branch silently skipping
  null rules, now dead now that `build()` rejects them before construction.
  Package-private, only reachable through the builder — cosmetic, but
  misleading to a later reader who assumes the branch is live.
- `FieldDefinition.isRequired()`, set by `EntityResolverBuilder.required(...)`,
  is read by nothing; `RuleBasedScorer.Builder#requiredField(String)` is the
  mechanism that actually enforces required fields. Task 07 documented rather
  than enforced this deliberately — enforcing it a second time in the
  resolver would give one concept two mechanisms with different semantics —
  but the flag lives in `field/` (task 05's file) and a later milestone
  should decide whether it earns its place or should be removed.
