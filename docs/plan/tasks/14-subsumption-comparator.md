# 14 — Token subsumption comparator

**Repo:** .
**Depends on:** 09, 10, 12
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/field/TokenSubsumptionComparator.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/field/TokenSubsumptionComparatorTest.java

## Goal
The generic half of D9. A comparator that reports whether one side's tokens are
contained in the other's, so that "less specific" stops being scored as
"conflicting". Core may not name an address (`docs/architecture.md`, "Core
contains no domain vocabulary"), so this ships as token subsumption and task 15
applies it.

## Context
- docs/design-decisions.md#d9 — the whole entry. The §90 case is source `Dublin` against candidates `Dublin 4` and `Dublin 8`: both contain the source, neither contradicts it, so the margin is genuinely small and REVIEW is correct *for the right reason*.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/evidence/TokenSubsumption.java — task 10's enum (read after 10 lands).
- jresolve-core/src/main/java/io/github/aindriub/jresolve/comparison/DefaultTokenSplitter.java — task 09's splitter; use it, do not tokenise inline.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/field/AbstractNullSafeFieldComparator.java — public after task 12.

## Acceptance
- [ ] `TokenSubsumptionComparator` extends `AbstractNullSafeFieldComparator<String>`, takes a `TokenSplitter` at construction, and rejects a null splitter.
- [ ] Both sides are tokenised into sets; a test asserts that a repeated token does not change the outcome.
- [ ] Every `TokenSubsumption` value is reachable and tested: equal sets yield `EQUIVALENT`; a strict subset on the left yields `LEFT_SUBSUMES_RIGHT`; the converse yields `RIGHT_SUBSUMES_LEFT`; disjoint or partially-overlapping sets yield `NEITHER`. This comparator never returns `NOT_APPLICABLE`.
- [ ] The naming direction of `LEFT_SUBSUMES_RIGHT` is stated in Javadoc with a worked token example and pinned by a test asserting the asymmetric case, so the two directions cannot be transposed silently.
- [ ] The category reported alongside the signal distinguishes containment from conflict: a strict subset is **not** `CONFLICT`. State the category mapping in Javadoc and pin every arm with a test.
- [ ] A test reproduces D9's §90 shape with neutral tokens: one source and two candidates that each strictly subsume it, asserting both yield the same category and the same subsumption direction — which is *why* the margin between them is small.
- [ ] `getSimilarity()` carries the overlap ratio the comparator actually computed, or is null; whichever is chosen is documented, and a test pins it. Do not report a number the comparator did not compute.
- [ ] The comparator is documented as **asymmetric**, and a test asserts `compare(a, b)` and `compare(b, a)` yield opposite subsumption directions for a strict-subset pair. D9 scopes the symmetry property to `SimilarityMetric`, so this is expected behaviour, not a defect — say so in the Javadoc.
- [ ] Empty token sets on one or both sides are defined and tested, and the choice is defensible against `MISSING_ONE` / `MISSING_BOTH` semantics — an empty-after-tokenising value is not the same as a null value. State the distinction.
- [ ] Null handling is inherited, not reimplemented.
- [ ] No type, member or Javadoc word in this file names a person, name, address, date of birth or country. In particular no test fixture uses a real locality; use neutral tokens.

## Out of scope
- Anything address-shaped — naming, street/locality structure, postcode handling. That is task 15, in the profiles module.
- Scoring or decisioning on the subsumption signal; no scorer reads it this milestone.
- Weighted or positional token matching.
