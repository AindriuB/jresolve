# 14 — Token subsumption comparator

**Repo:** .
**Depends on:** 09, 10, 12
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/field/TokenSubsumptionComparator.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/field/TokenSubsumptionComparatorTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/field/FieldComparatorNullSafetyTest.java *(amendment, see below)*

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
- [ ] Every `TokenSubsumption` value is reachable and tested: equal sets yield `EQUIVALENT`; a strict subset on the left yields `LEFT_SUBSUMES_RIGHT`; the converse yields `RIGHT_SUBSUMES_LEFT`; disjoint or partially-overlapping sets yield `NEITHER`. This comparator never returns `NOT_APPLICABLE` **when both values are present** — see the amendment; the plan's flat "never" was wrong.
- [ ] The naming direction of `LEFT_SUBSUMES_RIGHT` is stated in Javadoc with a worked token example and pinned by a test asserting the asymmetric case, so the two directions cannot be transposed silently.
- [ ] The category reported alongside the signal distinguishes containment from conflict: a strict subset is **not** `CONFLICT`. State the category mapping in Javadoc and pin every arm with a test.
- [ ] A test reproduces D9's §90 shape with neutral tokens: one source and two candidates that each strictly subsume it, asserting both yield the same category and the same subsumption direction — which is *why* the margin between them is small.
- [ ] `getSimilarity()` carries the overlap ratio the comparator actually computed, or is null; whichever is chosen is documented, and a test pins it. Do not report a number the comparator did not compute.
- [ ] The comparator is documented as **asymmetric**, and a test asserts `compare(a, b)` and `compare(b, a)` yield opposite subsumption directions for a strict-subset pair. D9 scopes the symmetry property to `SimilarityMetric`, so this is expected behaviour, not a defect — say so in the Javadoc.
- [ ] Empty token sets on one or both sides are defined and tested, and the choice is defensible against `MISSING_ONE` / `MISSING_BOTH` semantics — an empty-after-tokenising value is not the same as a null value. State the distinction.
- [ ] Null handling is inherited, not reimplemented.
- [ ] No type, member or Javadoc word in this file names a person, name, address, date of birth or country. In particular no test fixture uses a real locality; use neutral tokens.

## Planning amendment (at implementation)

`field/FieldComparatorNullSafetyTest.java` sweeps every comparator in the
package for null safety from a **manual** list — its `hasSize` assertion is a
tripwire on that list, not on the package, so a comparator added and never
listed is covered by nothing (see the corrected comment task 12 left there).
Tasks 13 and 14 both add a comparator to `field/` and neither owned the file.

It is assigned to **task 14**, which lands second and registers both new
comparators in one edit. Giving it to both would put two tasks in one file,
which is the clash `Owns` exists to prevent.

The gap this leaves is narrow and deliberate: between 13 and 14 landing, the
alias comparator's null handling is covered by its own test (task 13 asserts
all four combinations) but not by the package-wide sweep. Task 14 closes it.

## Second planning amendment (at implementation)

Acceptance said flatly that this comparator never returns `NOT_APPLICABLE`.
It does, and correctly so: a null on either side is answered by the inherited
null rule before `compareNonNull` runs, and that path computes no containment,
which is exactly what `NOT_APPLICABLE` means. The criterion is scoped to two
present values, and the null case is asserted separately.

The distinction is worth keeping rather than smoothing over. A punctuation-only
value and a null value reach the same *category*, because the evidence is the
same — neither tells us anything about the other side. They differ in the
signal: the empty value had containment computed and found none (`NEITHER`),
the null never got that far (`NOT_APPLICABLE`).

## Out of scope
- Anything address-shaped — naming, street/locality structure, postcode handling. That is task 15, in the profiles module.
- Scoring or decisioning on the subsumption signal; no scorer reads it this milestone.
- Weighted or positional token matching.
