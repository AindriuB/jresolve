# 04 — Add the core value types

**Repo:** .
**Depends on:** 01
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/evidence/**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/result/**
- jresolve-core/src/test/java/io/github/aindriub/jresolve/evidence/**
- jresolve-core/src/test/java/io/github/aindriub/jresolve/result/**

## Goal
Define the immutable vocabulary every later layer reads and writes: the open
`ComparisonCategory`, the evidence types, and the result types carrying a score
with its scale. These are pure data with no behaviour beyond derivation, and
they freeze the shape tasks 05, 06 and 07 build against.

## Context
- docs/design-decisions.md#d3 — `ComparisonCategory` is an interned value object, not an enum, because it is the key of every scoring table.
- docs/design-decisions.md#d4 — `MISSING_ONE` and `MISSING_BOTH` are separate categories; short-circuited evidence is flagged incomplete.
- docs/design-decisions.md#d6 — `Score` carries `ScoreScale`; margin is defined on that scale and `hasSecondBest()` replaces a sentinel.
- docs/design-decisions.md#d10 — `FieldContribution` carries a template key, never a rendered sentence.
- docs/design-decisions.md#d12 — `matched` is derived from the decision; only `List<FieldContribution>` survives as the contribution shape.
- docs/design-decisions.md#d5 — `getFrequencyKey()` returns the agreed normalized value when the comparison agreed, null otherwise.
- docs/spec/original-design.md §19, §29, §30, §52, §54, §55, §56, §57 — the original shapes these amend. Read those sections only.
- docs/conventions.md#errors — no exception message or `toString()` contains a field value.

## Acceptance
- [ ] Package `evidence` contains `ComparisonCategory`, `FieldEvidence`, `DefaultFieldEvidence`, `MatchEvidence`. Package `result` contains `Score`, `ScoreScale`, `Decision`, `MatchResult`, `ScoredCandidate`, `FieldContribution`.
- [ ] `ComparisonCategory.of(String)` interns by name: `of("EXACT")` is asserted `isSameAs(ComparisonCategory.EXACT)`. `equals`/`hashCode` are consistent with the name, and `of` rejects null, empty and whitespace-only names.
- [ ] The constants `EXACT`, `ALIAS_TRANSLATION`, `ALIAS_NICKNAME`, `ALIAS_VARIANT`, `VERY_HIGH`, `HIGH`, `MEDIUM`, `LOW`, `CONFLICT`, `MISSING_ONE`, `MISSING_BOTH` all exist.
- [ ] The intern table is a `ConcurrentHashMap` of names only; a class comment states that this is the one permitted static and that no field value ever enters it. A test interns the same new name from two threads and asserts a single identity results.
- [ ] `FieldEvidence` exposes `getCategory()`, a nullable `Double getSimilarity()`, and `getFrequencyKey()` returning null when the comparison did not agree. `DefaultFieldEvidence` is immutable and rejects a null category.
- [ ] `MatchEvidence` copies its map defensively into an unmodifiable `LinkedHashMap`, preserves insertion order in `getFields()`, returns null from `getField(name)` for an absent field, and exposes `isComplete()` set at construction.
- [ ] `ScoreScale` has exactly `LOG2_LIKELIHOOD_RATIO`, `PROBABILITY`, `POINTS`.
- [ ] `Score` holds `double value`, a non-null `ScoreScale`, a `String algorithm` identifier and a nullable `Double probability`; construction rejects a null scale and a non-null probability outside `[0,1]`. Javadoc states the probability is null unless the model is calibrated.
- [ ] `FieldContribution` holds field name, `ComparisonCategory`, `double contribution` and `String templateKey` — no rendered sentence and no prepared value. A test asserts the type exposes no value-bearing accessor.
- [ ] `ScoredCandidate<C>` holds the candidate, its `Score` and an ordered unmodifiable `List<FieldContribution>`.
- [ ] `MatchResult<C>` holds decision, match, score, second-best score and the ranked candidate list; `isMatch()` is `decision == MATCH`; `getMatch()` returns null for every other decision and says so in Javadoc; `hasSecondBest()` exists and `getMargin()` documents it is only meaningful when that is true. A test asserts `getMatch()` is null for `REVIEW` and for `NO_MATCH`.
- [ ] Every `toString()` here emits field names, categories, scales and numbers only; a test builds evidence carrying a frequency key and asserts the key does not appear in the `toString()` output.
- [ ] All types are final with final fields, no setters, and defensively copy every collection argument.

## Out of scope
- `FieldDefinition`, `FieldPipeline`, `FieldComparator` — task 05.
- `MatchScorer`, `DecisionThresholds`, any scoring or decision logic — task 06.
- The `diagnostics(true)` opt-in from D10 and any explanation rendering.
- Fellegi-Sunter model types, `TermFrequencyTable`, `AliasRepository`.
