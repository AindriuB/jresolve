# 09 — Give `TokenSplitter` a production implementation

**Repo:** .
**Depends on:** none
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/comparison/TokenSplitter.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/comparison/DefaultTokenSplitter.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/comparison/DefaultTokenSplitterTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/comparison/TokenSimilarityTest.java

## Goal
`TokenSplitter` has existed since task 02 with no implementation in main sources
— only a test lambda. Task 14's subsumption comparator and task 15's address
pipeline both need to tokenise, and without a real implementation each will
invent its own. Land one.

## Context
- jresolve-core/src/main/java/io/github/aindriub/jresolve/comparison/TokenSplitter.java — the whole interface: `List<String> split(String value)`.
- docs/plan/PLAN.md, "Known gaps" — this has been flagged in three consecutive waves; the flag names this milestone's address pipeline as the place the duplicate would appear.
- docs/conventions.md#errors — null is data, never an exception.
- docs/plan/tasks/retired/02-similarity-metrics.md — how `TokenSimilarity` consumes a splitter today.

## Acceptance
- [ ] `DefaultTokenSplitter` is final, immutable and stateless; a single instance is safe for concurrent use and the Javadoc says so.
- [ ] `split(null)` returns an empty list, not null and not an exception.
- [ ] `split("")` and a whitespace-only input both return an empty list.
- [ ] Splitting is on runs of whitespace **and** punctuation, so that a single input yields the same tokens whether its separator is a space, a hyphen or a comma; a test asserts the three agree.
- [ ] Repeated and leading/trailing separators never produce an empty token; a test asserts no returned token is empty for an input built from doubled separators.
- [ ] The returned list is unmodifiable; a test asserts `add` throws.
- [ ] `split` is deterministic and preserves input order; a test asserts order for a three-token input.
- [ ] The Javadoc states that `split` expects an already-normalized value (D2) and does not itself case-fold or strip diacritics.
- [ ] `TokenSimilarity` is re-pointed at `DefaultTokenSplitter` in any test that previously used a lambda, and the existing `TokenSimilarity` tests still pass unchanged in their assertions.
- [ ] No type, member or Javadoc word in this package names a person, name, address, date of birth or country.

## Out of scope
- Any comparator. This task ships a splitter and nothing that compares.
- Stemming, stop-word removal, phonetics, or locale-specific tokenisation rules.
- Changing `TokenSimilarity`'s algorithm or its public shape.
