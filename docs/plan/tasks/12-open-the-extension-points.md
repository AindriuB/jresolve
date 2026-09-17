# 12 — Open the field-layer extension points

**Repo:** .
**Depends on:** none
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/field/AbstractNullSafeFieldComparator.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/field/SimilarityBands.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/field/ExactFieldComparator.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/result/Score.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/field/SimilarityBandsTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/field/ExactFieldComparatorTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/result/ScoreTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/field/FieldComparatorNullSafetyTest.java

## Goal
Task 15 writes a comparator in `jresolve-profiles-ie`. Two things in `field/`
make that impossible to do correctly today, and both were predicted in review:

- `AbstractNullSafeFieldComparator` is package-private
  (`field/AbstractNullSafeFieldComparator.java:6`), so a comparator outside
  `field/` cannot inherit the `MISSING_ONE` / `MISSING_BOTH` handling and must
  hand-roll it.
- `SimilarityBands.categoryFor` is package-private
  (`field/SimilarityBands.java:78`) while the class and its getters are public,
  so the same comparator can read the thresholds but not apply them — and will
  reimplement the banding with silently different bounds.

This is the wave PLAN.md's known-gaps list was waiting for. Close the two
visibility gaps and the `Score.algorithm` guard, so tasks 13, 14 and 15 build
on a field layer that can actually be extended.

## Context
- docs/plan/PLAN.md, "Known gaps" (wave 3) — `SimilarityBands.categoryFor` visibility, `Score.algorithm` nullability, and the `ExactFieldComparator` frequency-key requirement, all recorded with the reason each was left open.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/field/AbstractNullSafeFieldComparator.java — the whole class; note `compare` is `final` and the hook is `compareNonNull`.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/result/Score.java:19-28 — the constructor assigns `algorithm` with no check.
- docs/conventions.md#errors and docs/conventions.md#naming.

## Acceptance
- [ ] `AbstractNullSafeFieldComparator` is public; `compare` stays `final` and the `compareNonNull` hook is `protected abstract`. A test in a **different package** subclasses it and asserts all four null combinations still map to `MISSING_ONE` / `MISSING_BOTH`.
- [ ] Its Javadoc states that subclasses receive two non-null arguments and must not re-check for null.
- [ ] `SimilarityBands.categoryFor` is public, with Javadoc stating the bands are inclusive at their lower bound and that reimplementing them elsewhere is the defect this visibility exists to prevent.
- [ ] The existing boundary tests at exactly 0.95, 0.85 and 0.70 still pass unchanged, and one is exercised through the now-public method from outside `field/`.
- [ ] `Score`'s constructor rejects a null `algorithm` with `IllegalArgumentException` in the existing message style, naming the constraint and not the value. A test asserts it throws, and asserts the existing `RuleBasedScorer` path still constructs successfully.
- [ ] `ExactFieldComparator`'s Javadoc states that `N` must have a `toString` consistent with `equals`, because the frequency key is derived from `toString()` and D5's frequency adjustment requires common values to share a key. A test documents the failure: two `equals` instances with a default `toString` produce different frequency keys.
- [ ] `FieldComparatorNullSafetyTest`'s comment that describes its own failure mode backwards is corrected (PLAN.md, wave 3).
- [ ] `mvn verify` passes with no change to any assertion outside this task's `Owns`. Widening visibility must not require editing a sibling test.
- [ ] No type, member or Javadoc word in these files names a person, name, address, date of birth or country.

## Note on scope
This task deliberately changes public API surface (two members widened, one
constructor made stricter). The stricter constructor can break a caller
constructing `Score` with a null algorithm — that is the point, and D6's
argument is that a wrong-scale or unattributed score is worth failing on. Record
the widening in the task's commit body so `scribe` can carry it into
`HISTORY.md`.

## Out of scope
- New comparators — tasks 13 and 14.
- `MatchDecisionEngine`'s scale exposure (wave 4's D6 gap). It is a real gap but it lives in `api/` and `decision/`, and no task in this milestone needs it. It stays open.
- `FieldDefinition.isRequired()`'s fate (wave 4) — decide it in a milestone that touches required-field semantics.
