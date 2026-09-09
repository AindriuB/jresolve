# 02 — Implement the similarity metrics

**Repo:** .
**Depends on:** 01
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/comparison/**
- jresolve-core/src/test/java/io/github/aindriub/jresolve/comparison/**

## Goal
Provide `SimilarityMetric` and three implementations — Jaro-Winkler,
Levenshtein-derived and token-set — each a pure function of two strings, exactly
symmetric, bounded in `[0,1]`, with the property tests the conventions require.
These are features, not probabilities, and nothing here knows about fields,
categories or scoring.

## Context
- docs/spec/original-design.md §21–§24 — the interface, the two string metrics and the property list. Read those sections only.
- docs/conventions.md#tests — every metric carries bounded / reflexive / **exactly** symmetric property tests, `isEqualTo` with no tolerance; never assert a score against a value produced by running the code.
- docs/design-decisions.md#d9 — the symmetry property is scoped to `SimilarityMetric` implementations, where asymmetry is a bug rather than a tolerance.
- docs/conventions.md#java-8-project — no `var`, no `List.of`, no streams API added after 8.

## Acceptance
- [ ] `SimilarityMetric` declares `double similarity(String left, String right)` and its Javadoc states the `[0,1]` bound, exact symmetry, and that a `null` argument is treated as the empty string because missingness is the field layer's business, not the metric's.
- [ ] No implementation throws on a `null` argument; `similarity(null, null) == 1.0` and `similarity(null, "a") == 0.0` are asserted.
- [ ] `JaroWinklerSimilarity` uses prefix scale 0.1, maximum prefix length 4 and boost threshold 0.7, all constructor-configurable with a no-arg constructor supplying those defaults; the constructor rejects a prefix scale outside `[0, 0.25]`.
- [ ] `JaroWinklerSimilarity` matches hand-worked expectations written as arithmetic in the test comment for at least `MARTHA`/`MARHTA`, `DWAYNE`/`DUANE` and `DIXON`/`DICKSONX`.
- [ ] `LevenshteinSimilarity` returns `1 - distance / max(length)`, uses O(min(m,n)) working memory, and asserts `kitten`/`sitting` = `1 - 3/7` from a hand-computed distance.
- [ ] `TokenSimilarity` takes a delegate `SimilarityMetric` plus a token splitter, and scores as the symmetric best-match mean: `(Σ over left tokens of max sim to a right token + Σ over right tokens of max sim to a left token) / (leftCount + rightCount)`. Its Javadoc states that this construction is what makes it exactly symmetric.
- [ ] `TokenSimilarity` returns 1.0 for the same tokens in a different order, and 1.0 when both sides have no tokens.
- [ ] Each of the three metrics has a property test over a fixed synthetic string corpus (declared in the test, at least 20 pairs) asserting bounded, `similarity(x,x) == 1.0`, and `similarity(a,b)` exactly equal to `similarity(b,a)`.
- [ ] No type, member or Javadoc word in this package names a person, name, address, date of birth or country.

## Out of scope
- `SimilarityBands`, any mapping from a similarity to a `ComparisonCategory`, and any `FieldComparator` — task 05 owns those.
- Phonetics, Soundex, Double Metaphone (see docs/design-decisions.md#d8).
- Caching or memoising a metric (docs/spec/original-design.md §73).
- Anything under `normalization/` — task 03 owns it; do not fold case folding into a metric.

## Attempt 1 - failed

Tester PASS (29 tests, all four mutations caught, hand-computed values agree),
reviewer CHANGES. Every acceptance criterion is met; one test does not test.

**Defect - must fix.** `TokenSimilarityTest.weightsBestMatchesBySideTokenCounts`
(around line 47) derives its expected value by calling
`delegate.similarity("bravo", "alpha")` at test time and then asserts the code
against it. docs/conventions.md#tests: never assert a score against a value
produced by running the code. Change `JaroWinklerSimilarity` to return anything
else for that pair and the test still passes, so it cannot detect a delegate
regression. Write the expectation as hand-worked arithmetic in a comment, the
way the sibling tests already do.

Note for whoever fixes it: the tester found this test *did* catch a broken
token-mean mutation, because breaking `TokenSimilarity`'s own construction moves
its output while the delegate-derived expectation stays fixed. Both findings
hold. The test is not inert - it is blind in exactly one direction, the delegate,
and that is the direction it names.

**Suggestions - take unless there is a reason not to.**
- `LevenshteinSimilarityTest` around line 36: `1 - 3/7` is exactly representable
  as `1.0 - 3.0/7.0`, so `isEqualTo` pins it harder than `within(0.000001)`.
- `JaroWinklerSimilarity` lines 24-31: `prefixScale` is validated but
  `maxPrefixLength` and `boostThreshold` are not, so a negative
  `maxPrefixLength` silently degrades to no boost.
- `TokenSimilarity` line 16: the class Javadoc does not say that its
  thread-safety is inherited from the injected delegate and splitter. For a type
  built from two caller-supplied objects that is load-bearing, not decorative.

**Cross-branch check, resolved.** `TokenSplitter` was the right call and
duplicates nothing: task 03's `normalization/` is `String` to `String`
throughout, with no tokenising abstraction to collide with. But nothing in 02's
main sources implements `TokenSplitter` - only a test lambda does - so **task 05
is where a second splitter could be invented independently**. That belongs in
05's task file before it runs.
