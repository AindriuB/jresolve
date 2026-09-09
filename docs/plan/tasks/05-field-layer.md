# 05 — Build the field layer

**Repo:** .
**Depends on:** 02, 03, 04
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/field/**
- jresolve-core/src/test/java/io/github/aindriub/jresolve/field/**

## Goal
Define how one field is prepared and compared: the two-phase pipeline, the
comparator contract, the field definition with two extractors converging on one
normalized type, and the cost tier a field declares. Add the two comparators the
end-to-end test needs — exact, and similarity-banded.

## Context
- docs/design-decisions.md#d1 — `FieldDefinition<S, C, N>` holds `Function<S, N>` and `Function<C, N>`; the symmetric case is sugar over the asymmetric one.
- docs/design-decisions.md#d2 — `FieldPipeline<V, N>` splits into `prepare(V)` and `compare(N, N)`; `prepare` is null-safe, deterministic and idempotent.
- docs/design-decisions.md#d4 — fields declare a cost tier; `MISSING_ONE` and `MISSING_BOTH` are distinct.
- docs/design-decisions.md#d5 — the frequency key is the agreed normalized value, null when the comparison did not agree.
- docs/design-decisions.md#d9 — field comparators need not be symmetric, and each documents which it is.
- docs/spec/original-design.md §10, §11, §20, §62, §63, §76 — the original shapes plus the missing/conflict rules. Read those sections only.
- docs/conventions.md#errors — a comparator never throws on null; null is data.

## Acceptance
- [ ] `FieldPipeline<V, N>` declares `N prepare(V value)` and `FieldEvidence compare(N left, N right)`, with null-safety, determinism and idempotence stated in Javadoc.
- [ ] `FieldNormalizer<V, N>` and `FieldComparator<N>` exist; `DefaultFieldPipeline<V, N>` composes one of each and is final and immutable.
- [ ] `FieldDefinition<S, C, N>` is final and holds name, `Function<S, N>` source preparer, `Function<C, N>` candidate preparer, `FieldComparator<N>`, `int cost` and `boolean required`, with getters and no setters; the constructor rejects a null name, a null preparer and a null comparator.
- [ ] A `CostTiers` constants type declares at least `CHEAP`, `MODERATE`, `EXPENSIVE` as ascending ints, documented as an ordering rather than a measured cost.
- [ ] Every comparator maps null-left/non-null-right and its converse to `MISSING_ONE`, and both-null to `MISSING_BOTH`; a parameterised test asserts no comparator in this package throws on any null combination.
- [ ] `ExactFieldComparator<N>` returns `EXACT` on `equals` and `CONFLICT` otherwise, sets the frequency key to the agreed value on `EXACT` and null otherwise, and documents that it is symmetric.
- [ ] `SimilarityBands` holds four configurable thresholds defaulting to `VERY_HIGH >= 0.95`, `HIGH >= 0.85`, `MEDIUM >= 0.70`, `LOW` below that; the constructor rejects thresholds that are not strictly descending or that fall outside `[0,1]`.
- [ ] `SimilarityFieldComparator` takes a `SimilarityMetric` and `SimilarityBands`, returns `EXACT` for equal inputs and otherwise the band category, and carries the raw similarity on `FieldEvidence.getSimilarity()`. Boundary tests assert the bands are inclusive at exactly 0.95, 0.85 and 0.70.
- [ ] `SimilarityFieldComparator` sets the frequency key only for `EXACT`; a test asserts it is null for a `HIGH` result.
- [ ] A test composes a `CompositeNormalizer` from task 03 into a `DefaultFieldPipeline` and asserts `prepare` is idempotent for a `String` normalized type.
- [ ] No type, member or Javadoc word in this package names a person, name, address, date of birth or country.

## Out of scope
- The resolver, the builder and cost-tier scheduling — task 07 consumes `cost` but owns the loop.
- Scoring weights and required-field enforcement at scoring time — task 06 owns those; `required` here is data on the definition only.
- Date, phone or address comparators, and any ready-made `FieldPipelines` factory.
- Blocking key derivation from a prepared value.
