# 13 — Alias-aware field comparator

**Repo:** .
**Depends on:** 11, 12
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/field/AliasAwareFieldComparator.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/field/AliasAwareFieldComparatorTest.java

## Goal
Turn task 11's repository into evidence. This is the comparator that makes
`Seán` and `John` agree as `ALIAS_TRANSLATION` instead of scoring LOW on
Jaro-Winkler, which is the single change that moves a fuzzy first name from a
penalty to a contribution.

## Context
- docs/design-decisions.md#d7 — the *kind* of alias is the category; the scoring model assigns its weight, and no strength lives in the data.
- docs/design-decisions.md#d8 — `Ó Súilleabháin`/`O'Sullivan` is this comparator's problem, not normalization's. Normalization cannot close it and chasing it there is the trap D8 exists to prevent.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/alias/AliasRepository.java — task 11's interface (read after 11 lands).
- jresolve-core/src/main/java/io/github/aindriub/jresolve/field/AbstractNullSafeFieldComparator.java — extend this rather than re-checking null; task 12 makes it public.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/field/SimilarityFieldComparator.java — the delegate shape and how it carries `getSimilarity()`.

## Acceptance
- [ ] `AliasAwareFieldComparator` extends `AbstractNullSafeFieldComparator<String>` and implements only `compareNonNull`.
- [ ] It is constructed with an `AliasRepository` and a delegate `FieldComparator<String>`; both are rejected as null at construction.
- [ ] Resolution order is exact, then alias, then delegate, and a test asserts each arm in isolation:
  - equal values yield `EXACT` and the delegate is never consulted;
  - an alias pair yields the repository's category (`ALIAS_TRANSLATION`, `ALIAS_NICKNAME` or `ALIAS_VARIANT`);
  - an unrelated pair yields exactly what the delegate returned, unmodified.
- [ ] The delegate is consulted **only** on the unrelated arm; a counting delegate asserts zero invocations for the exact and alias arms.
- [ ] An alias hit sets the frequency key to null, not to either value — the two sides did not agree on a value, and D5's frequency table is keyed on agreed values. A test asserts this, with the reason in a comment.
- [ ] An alias hit leaves `getSimilarity()` null rather than fabricating a number; the Javadoc states that an alias relation is not a similarity measurement.
- [ ] The comparator is symmetric: for every pair, `compare(a, b)` and `compare(b, a)` yield equal categories. A test asserts this across a fixture group, and the Javadoc records symmetry explicitly (D9 requires each comparator to document which it is).
- [ ] Null handling is inherited, not reimplemented; a test asserts all four null combinations and the diff contains no null check in `compareNonNull`.
- [ ] A test asserts the D8 case reaches this comparator rather than normalization: two values that normalization leaves distinct are resolved here via the repository.
- [ ] No type, member or Javadoc word in this file names a person, name, address, date of birth or country. Fixtures are neutral tokens.

## Out of scope
- The alias data itself — task 15, in the profiles module.
- Token- or subsumption-based comparison — task 14.
- Any change to `SimilarityFieldComparator` or `SimilarityBands` — task 12 owns those files.
- Scoring weights for the `ALIAS_*` categories; the end-to-end wiring in task 16 sets them.
