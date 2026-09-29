# 36 — Close the two API sharp edges before the API is public

**Repo:** .
**Depends on:** none
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/RuleBasedScorer.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/evidence/ComparisonCategory.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/field/TokenSubsumptionComparator.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/scoring/RuleBasedScorerTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/evidence/ComparisonCategoryTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/field/TokenSubsumptionComparatorTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/readme/ReadmeExamplesTest.java
- README.md — only the code snippet mirroring the `ReadmeExamplesTest` example this task changes; nothing else

## Goal
Milestone 7's README had to warn about two traps the API should prevent.
First, a scorer that weights only the partial-agreement categories of a field
scores a perfect `EXACT` agreement as zero, silently. Second, `SUBSUMED` and
`PARTIAL_OVERLAP` cannot be reached from `ComparisonCategory`. 0.1.0 is the
last point at which fixing either costs nothing, so `build()` now refuses the
first, and `ComparisonCategory` carries the second.

## Context
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/RuleBasedScorer.java:98-191 — `weightFor` falls back to the default weight and then to 0.0. `Builder.build()` at :187 validates nothing today.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/evidence/ComparisonCategory.java:5-33 — the open value type, its interning, and the existing constants.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/field/TokenSubsumptionComparator.java:81-85 — where `SUBSUMED` and `PARTIAL_OVERLAP` are minted today.
- docs/conventions.md#errors — a configuration error fails at `build()`, and its message names the field and the constraint, never a value.
- Measured on b80acc2 by prototyping the rule: exactly four existing tests fail. They are `RuleBasedScorerTest` `totalEqualsTheSumOfTheContributions` (:47), `handWorkedFourFieldTotalIncludingANegativeConflictWeight` (:76) and `emitsOneContributionPerFieldInEvidenceIterationOrder` (:102), plus core `ReadmeExamplesTest.cheapFieldsAreComparedFirstAndARuleCanVetoBetweenTiers` (:203-221). All of `jresolve-profiles-ie` stays green, 81 of 81.
- Baseline: `mvn clean verify` on b80acc2 gives 634 tests (553 core + 81 profiles), `BUILD SUCCESS`.

## Acceptance
- [ ] `RuleBasedScorer.Builder.build()` throws for any field that has a weight for a *partial-agreement* category but has neither an `EXACT` weight nor a `defaultWeight`. The partial-agreement categories are `VERY_HIGH`, `HIGH`, `MEDIUM`, `LOW`, `ALIAS_TRANSLATION`, `ALIAS_NICKNAME`, `ALIAS_VARIANT`, `SUBSUMED` and `PARTIAL_OVERLAP`. The message names the field and says that `EXACT` is unweighted. It contains no field value.
- [ ] The exception type matches what the scoring package already throws for a configuration error. The Javadoc on `build()` states the rule and the type.
- [ ] Setting `.weight(field, EXACT, 0.0)` explicitly is the opt-out: that configuration builds. A test pins this.
- [ ] A field weighted only on `CONFLICT`, `MISSING_ONE`, `MISSING_BOTH` or a consumer-minted category still builds. A test pins at least one of these.
- [ ] A test pins each rejection path: a band-only field, an alias-only field and a subsumption-only field. A `defaultWeight` also satisfies the rule, and a test pins that too.
- [ ] `ComparisonCategory.SUBSUMED` and `ComparisonCategory.PARTIAL_OVERLAP` exist. `TokenSubsumptionComparator.SUBSUMED == ComparisonCategory.SUBSUMED`, and the same holds for `PARTIAL_OVERLAP`. A test asserts both identities with `isSameAs`.
- [ ] `ComparisonCategory`'s class Javadoc contains a table mapping each built-in constant to the core comparator or comparators that produce it. It uses the neutral vocabulary `DomainVocabularyTest` enforces.
- [ ] The four tests listed under Context are fixed by adding an `EXACT` weight. None of their asserted totals change, and each hand-worked comment still adds up.
- [ ] If the README shows the snippet that `ReadmeExamplesTest` carries at :203-221, it is changed identically. No other README text is touched.
- [ ] `mvn clean verify` passes. The commit body states the test count: 634 plus the tests this task adds.
- [ ] The version stays `0.1.0-SNAPSHOT`.

## Out of scope
- Rewriting the README's two sharp-edge warnings (README.md:156-160 and :278-282). That is task 41, against the merged API.
- Changing `FellegiSunterScorer` or `DefaultFellegiSunterModel`. The rule is about rule-based weights only.
- Removing `TokenSubsumptionComparator.SUBSUMED`/`PARTIAL_OVERLAP`. They stay, as aliases of the same objects.
- Any file in `jresolve-profiles-ie`.
- `package-info.java` files (task 40).

## Attempt 1 — failed

Tests PASS (642 = 634 + 8); review REQUEST CHANGES on one criterion.

- **Producer table is incomplete.** `ComparisonCategory.java:24-33`: the `EXACT`
  row omits `TokenSubsumptionComparator` (returns `EXACT` at
  `TokenSubsumptionComparator.java:115`) and the `CONFLICT` row names only
  `ExactFieldComparator` (the subsumption comparator also returns `CONFLICT` at
  `:126`). A scorer built from the table could leave `CONFLICT` unweighted.
  Re-derive every row from the comparators' actual return sites.
- Ruled acceptable, do not change: `IllegalArgumentException` (matches the
  scoring package; aligning the package with `conventions.md#errors` is a
  follow-up), and leaving `README.md` untouched (its copy of the cost-tiers
  example never shows the changed scorer lines).
- Optional tidy-ups while there: import `java.util.Arrays` at
  `RuleBasedScorer.java:38`; one blank line, not two, before the new tests at
  `ComparisonCategoryTest.java:101` and `TokenSubsumptionComparatorTest.java:273`.
- Continue on branch `task/36-api-sharp-edges` (commit 9597f06); do not start over.

## Attempt 2 — failed

Tests PASS (642); review REQUEST CHANGES, again on the producer table only
(`ComparisonCategory.java:19-39`, commit a491947). Every other criterion met.

- **Alias pass-through is applied row by row, and misses rows.**
  `AliasAwareFieldComparator.java:73` returns its delegate's category unchanged,
  so it can produce `SUBSUMED` and `PARTIAL_OVERLAP` too (e.g. wrapping a
  `TokenSubsumptionComparator`, "a b" vs "a b c", no alias → `SUBSUMED`). Drop
  the per-row "also Alias when its delegate…" wording and state it once, in a
  note under the table: when neither the exact check nor the repository
  decides, `AliasAwareFieldComparator` returns its delegate's category as is.
- **`MISSING_ONE`/`MISSING_BOTH` source is wrong.** Not only
  `AbstractNullSafeFieldComparator`: `TokenSubsumptionComparator` returns them
  itself from `compareNonNull` for token-less values (`:106`, `:109`; "" vs "x"
  → `MISSING_ONE`). Name both.
- Rows verified correct, keep: Exact (`:46`, `:48`), Similarity (`:36`, `:39`,
  bands via `SimilarityBands:91-99`), Alias own returns (`:67`, `:71`),
  TokenSubsumption (`:115-126`).
- Javadoc-only. Continue on `task/36-api-sharp-edges`; run full
  `mvn clean verify` before returning.
