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

- [x] **08 — End-to-end test.** Full-pipeline test exercising resolver against
  a synthetic fixture. Merged after 2 attempts; see `docs/plan/HISTORY.md`.

## Milestone 1 — complete

All eight tasks merged. `mvn clean verify` on `main` at close: 262 tests,
`BUILD SUCCESS`, commit `9bd38c4`.

**What shipped:** a Java 8 entity-resolution library with typed field
extraction, two-phase normalization, Jaro-Winkler/Levenshtein/token
similarity, an open comparison-category model, cost-tiered comparison with
rule vetoes, rule-based scoring on a declared score scale, threshold-and-
margin decisioning, and an explainable ranked result — verified end to end
by task 08.

**What did not ship:** no alias repository, no address pipeline, no blocking
or candidate index, no Fellegi-Sunter, no logistic regression, no Irish
profiles. `jresolve-profiles-ie` is still an empty module skeleton.

**The milestone assessment (from task 08's reviewer).** Does milestone 1
give a consumer reason to believe the library works? For composition: yes —
builder wiring, cost-tier ordering, the cheap-field veto short-circuit,
missing-versus-conflict evidence, threshold and margin decisioning, and
determinism across shuffled order and under ties are all genuinely exercised
end to end. For resolution quality: no — every positive outcome in the
end-to-end suite is carried by exact surname and exact date of birth, the
two columns a plain SQL join would match on. The fuzzy first name is a
penalty (`Seán`/`John` scores LOW with no alias repository); the address
contributes MEDIUM where §88 expects VERY_HIGH, because generic Levenshtein
on a normalized string is not an address pipeline. The one advance: the
ambiguous-margin scenario is the first place a fuzzy field decides
anything — an address similarity band is the sole differentiator between two
candidates — but it decides ranking, not whether a match exists. **D7 (alias
equivalence groups) and D9 (address subsumption) are not polish. They are
the gap between "the pieces compose" and "a consumer gets a better answer
than a join."**

**The recurring verification lesson, seen four times across this
milestone:** a correct measurement can support a claim broader than it
earned. The `C2 A0` byte scan that proved the NBSP class clean while reading
as proof about invisible characters generally (task 03); the `getMargin()`
probe that could not reach malformed constructions (task 04); the D6 check
that fires correctly while guarding nothing (task 07); and task 08 attempt
1, whose numbers were right while the record around them was not. The
counter-question to ask of any green test is "is it wired to anything", not
"does it work".

## Notes for implementers

- `jresolve-core` build needs a JDK 17 toolchain on the building machine —
  `~/.m2/toolchains.xml`, not part of the repo. See
  `docs/architecture.md#building`.
- `mvn clean verify` baseline at milestone 1's close (all 8 tasks merged) is
  262 tests, `BUILD SUCCESS`. Expect it to change as milestone 2 lands.

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

Raised in wave 5 (task 08):

- `EndToEndResolutionTest.java:67-69` carries a garbled sentence: it says the
  two exact-match fields net +10, when it is firstName (−5) and address
  (+15) that net +10 — lastName and dateOfBirth net 55, as the preceding
  sentence correctly says. The paragraph's conclusion is right; one sentence
  contradicts its neighbour. Fix on next touch.
- The tied-scores determinism test uses two candidates; an unstable sort
  would rarely swap exactly two. Three or four tied candidates would
  exercise the stable-sort claim properly.
- The positive test pins `FieldContribution` categories but not contribution
  values, so a weight change with unchanged bands still relies on the total
  assertion.
- The end-to-end suite's address field points at a generic string comparator
  and will need re-pointing at the real address pipeline once D9 exists. A
  passing end-to-end test is exactly what gets left behind when the feature
  it stands in for arrives.
