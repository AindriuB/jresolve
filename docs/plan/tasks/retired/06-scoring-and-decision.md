# 06 — Add rule-based scoring and the threshold decision engine

**Repo:** .
**Depends on:** 04
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/decision/**
- jresolve-core/src/test/java/io/github/aindriub/jresolve/scoring/**
- jresolve-core/src/test/java/io/github/aindriub/jresolve/decision/**

## Goal
Turn evidence into a `Score` on the POINTS scale with per-field contributions,
and turn a ranked list of scored candidates into a `MatchResult`. Scoring and
decisioning stay separate types so a later Fellegi-Sunter scorer drops in
without touching the engine.

## Context
- docs/design-decisions.md#d6 — thresholds declare the scale they were written against; the scorer exposes its scale so the builder in task 07 can compare them.
- docs/design-decisions.md#d4 — a scorer that cannot score partial evidence rejects it explicitly rather than treating absent fields as `MISSING_BOTH`.
- docs/design-decisions.md#d10 — contributions carry a template key, never a rendered sentence.
- docs/design-decisions.md#d12 — `MatchResult` derives `isMatch()`; there is no `matched` flag to disagree with the decision.
- docs/spec/original-design.md §40, §41, §51, §53, §68, §69 — rule scoring, the scorer interface, decisioning, ranking and margin. Read those sections only.
- docs/conventions.md#tests — hand-calculated expectations with the arithmetic in a comment; never assert a score against a value the code produced.
- docs/conventions.md#errors — configuration errors surface at construction with a message naming the field and the constraint, never a value.

## Note carried from task 04's review (wave 2)
`Score.algorithm` is unvalidated by `Score`'s own constructor and may be null
today — task 04 left it that way deliberately, since only this task knows what
a stable identifier looks like. `RuleBasedScorer` stamping it (acceptance
criterion below) closes the gap in practice, but nothing stops a future caller
constructing a `Score` with a null `algorithm` directly. If that is worth
closing, it means rejecting null at `Score` construction — which is a change to
a task-04-owned file and out of this task's `Owns`; raise it rather than
patching `result/Score.java` from here.

## Acceptance
- [ ] `MatchScorer` declares `ScoringResult score(MatchEvidence evidence)` and `ScoreScale scale()`. `ScoringResult` is immutable and is either scorable — carrying a `Score` and an ordered `List<FieldContribution>` — or unscorable, carrying a template key and no score; `isScorable()` distinguishes them.
- [ ] `RuleBasedScorer` is immutable, built by a nested builder, and configures: a weight per `(field, ComparisonCategory)` pair, a default weight per field, a base score, and a set of required field names.
- [ ] `RuleBasedScorer` reports `ScoreScale.POINTS` and stamps `Score.algorithm` with a stable identifier; `Score.getProbability()` is null.
- [ ] Scoring sums the matched weights, emits one `FieldContribution` per field in the evidence iteration order, and a test asserts the total equals the sum of the contributions.
- [ ] A hand-worked test computes a total from at least four fields with the arithmetic written as a comment, including one negative weight for a `CONFLICT` category.
- [ ] A required field whose evidence is `MISSING_ONE`, `MISSING_BOTH` or absent yields an unscorable `ScoringResult` naming the field in its template key; a test asserts each of the three cases.
- [ ] Incomplete evidence (`isComplete() == false`) whose absent fields include a required field is unscorable; a test asserts a scorer with no required fields still scores incomplete evidence, and that absent fields contribute zero rather than being scored as `MISSING_BOTH`.
- [ ] `DecisionThresholds` holds `matchThreshold`, `reviewThreshold`, `minimumMargin` and the `ScoreScale` they were written against; construction rejects `reviewThreshold > matchThreshold`, a negative margin and a null scale.
- [ ] `MatchDecisionEngine<C>` declares `MatchResult<C> decide(List<ScoredCandidate<C>> candidates)`. `ThresholdDecisionEngine<C>` ranks by score value descending with a stable tie-break that preserves input order, and a test asserts the ranking is deterministic across repeated runs on a shuffled equal-score input.
- [ ] The decision rules are asserted individually: at or above `matchThreshold` with margin at or above `minimumMargin` gives `MATCH`; at or above `matchThreshold` with an insufficient margin gives `REVIEW`; between the thresholds gives `REVIEW`; below `reviewThreshold` gives `NO_MATCH`; an empty candidate list gives `NO_MATCH` with a null match and `hasSecondBest() == false`.
- [ ] A single candidate above `matchThreshold` is `MATCH` and `hasSecondBest()` is false; no sentinel margin value is used anywhere.
- [ ] No type, member or Javadoc word in these packages names a person, name, address, date of birth or country.

## Out of scope
- `EntityResolverBuilder` and the scale cross-validation between scorer and thresholds — task 07 owns that check; expose `scale()` and stop.
- `FellegiSunterModel`, `FellegiSunterScorer`, logistic regression, `FeatureExtractor`, `ProbabilityModel`.
- `MatchRule`/`RuleResult` soft rules and `CandidateRule` hard rules — task 07 owns the hard-rule veto; soft rules are not in this milestone.
- Any change to the types in `result/` or `evidence/` — task 04 owns those files.
