# 37 — Clean up core before its API is public

**Repo:** .
**Depends on:** none
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/result/ScoredCandidate.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/result/ScoredCandidateTest.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/field/FieldDefinition.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/field/FieldDefinitionTest.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/api/EntityResolverBuilder.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/api/EntityResolverBuilderTest.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/api/DefaultEntityResolver.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/api/DefaultEntityResolverTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/comparison/JaroWinklerSimilarityTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/normalization/UnicodeFormNormalizerTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/decision/ThresholdDecisionEngineTest.java

## Goal
Close the carried-forward milestone-1 gaps in `PLAN.md` that touch the public
surface, before 0.1.0 freezes it. The biggest is removing a `required` flag
that nothing reads: once published, a dead public method has to be deprecated
rather than just deleted.

## Context
- jresolve-core/src/main/java/io/github/aindriub/jresolve/result/ScoredCandidate.java:19-26 — the constructor accepts a null candidate.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/field/FieldDefinition.java:27,35,76 — the `required` field, its constructor parameter and `isRequired()`.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/api/EntityResolverBuilder.java:146-160,239,333,368-369 — `required(String)`, its Javadoc admitting the flag does nothing, and the slot plumbing. `requiredFieldNames` is validated at `build()`.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/api/DefaultEntityResolver.java:65-71 — the null-rule skip, dead now that `build()` rejects null rules.
- jresolve-core/src/test/java/io/github/aindriub/jresolve/comparison/JaroWinklerSimilarityTest.java:29-61 — guard tests exist for `prefixScale` and the product bound. Nothing covers `boostThreshold` outside `[0, 1]` (JaroWinklerSimilarity.java:33-35) or a negative `maxPrefixLength` (:29-31).
- jresolve-core/src/test/java/io/github/aindriub/jresolve/normalization/UnicodeFormNormalizerTest.java:21,28 — precomposed literals pasted into source.
- jresolve-core/src/test/java/io/github/aindriub/jresolve/decision/ThresholdDecisionEngineTest.java:134 — a redundant `@SafeVarargs`.
- docs/conventions.md#tests — invisible characters are written as escapes.
- Baseline: `mvn clean verify` on b80acc2 gives 634 tests (553 core + 81 profiles), `BUILD SUCCESS`.

## Acceptance
- [ ] `new ScoredCandidate<>(null, score, contributions)` throws `IllegalArgumentException`, and a test in `ScoredCandidateTest` pins it.
- [ ] `FieldDefinition` has no `required` field, no `isRequired()`, and no constructor parameter for it. `rg -n 'isRequired' jresolve-*/src README.md` returns nothing.
- [ ] `EntityResolverBuilder.required(String)` is gone, and so is its `requiredFieldNames` validation and plumbing. `rg -n '\.required\(' jresolve-*/src README.md` returns nothing. `RuleBasedScorer.Builder#requiredField` is untouched.
- [ ] The tests that exercised the removed members are deleted, not adapted. The commit body names each deleted test.
- [ ] `DefaultEntityResolver`'s constructor copies `rules` without a null check. `EntityResolverBuilderTest.buildThrowsForANullRule` (:194), which checks the guard at EntityResolverBuilder.java:255-256 that makes the branch dead, still passes unchanged.
- [ ] `JaroWinklerSimilarityTest` has tests that fail when either `boostThreshold` guard (below 0, above 1) or the `maxPrefixLength < 0` guard is deleted. Show this by deleting each guard once and reporting the red test in the commit body.
- [ ] `UnicodeFormNormalizerTest` builds its precomposed inputs with `\u` escapes. `rg -n '[^\x00-\x7F]' jresolve-core/src/test/java/io/github/aindriub/jresolve/normalization/UnicodeFormNormalizerTest.java` returns nothing.
- [ ] `ThresholdDecisionEngineTest` no longer carries `@SafeVarargs`, and the build shows no new compiler warning.
- [ ] `mvn clean verify` passes. The commit body states the count as 634, minus the tests deleted, plus the tests added, with the arithmetic.
- [ ] The version stays `0.1.0-SNAPSHOT`.

## Out of scope
- `docs/design-decisions.md`. Its code sketch at :37 mentions `required`, but that file is task 38's in this wave, and a historical sketch is not an API.
- `RuleBasedScorer` and its `requiredField` mechanism (task 36 owns that file).
- README text. Nothing in it uses `.required(`. Task 41 refreshes it.
- D6's residual hole, or any other gap in `PLAN.md` not listed above.
