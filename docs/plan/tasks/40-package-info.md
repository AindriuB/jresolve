# 40 — Give every core package a `package-info.java`

**Repo:** .
**Depends on:** 36, 37
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/alias/package-info.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/api/package-info.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/comparison/package-info.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/decision/package-info.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/evidence/package-info.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/field/package-info.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/normalization/package-info.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/result/package-info.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/package-info.java

## Goal
The javadoc jar will be the first thing a stranger reads on javadoc.io, and
today its package index has nine blank rows. Each core package should say what
it is for, which types to start from, and how it relates to the pipeline. A
reader should be able to get from the package index to a working resolver.

## Context
- jresolve-profiles-ie/src/main/java/io/github/aindriub/jresolve/profiles/ie/package-info.java — the only existing `package-info`, and the shape to follow.
- docs/architecture.md#the-pipeline (:66-94) and #the-type-model (:95-174) — the accurate description of each package's role. Summarise it; do not contradict it.
- docs/calibration.md — `scoring`'s description must not suggest that anything shipped is calibrated.
- pom.xml:222-270 — doclint runs `all,-missing` at `show=private` over main and test sources. A malformed `{@link}` in a `package-info` fails the build.
- jresolve-core/src/test/java/io/github/aindriub/jresolve/DomainVocabularyTest.java:179-182 — it walks every `.java` under core, `package-info.java` included.
- Baseline: `mvn clean verify` on b80acc2 gives 634 tests (553 core + 81 profiles). Wave 1 changes the count, so compare against the merged `main` this task branches from, and state that figure and its commit.

## Acceptance
- [ ] All nine files exist, and each carries a package Javadoc whose first sentence summarises the package on its own (javadoc uses it as the index summary).
- [ ] Each names its entry-point types with `{@link}`: the interfaces a consumer implements or calls first, and their `Default*` implementations where they exist.
- [ ] `api`'s documentation walks the flow `EntityResolverBuilder` → `EntityResolver.resolve` → `MatchResult`. `evidence`'s documentation points at `ComparisonCategory`'s category-to-comparator table, which task 36 added. `scoring`'s documentation says a similarity is not a probability and links nothing that claims calibration.
- [ ] After `mvn clean verify`, `unzip -p jresolve-core/target/jresolve-core-*-javadoc.jar index.html` shows a non-empty description for each of the nine packages. The commit body lists the nine summary sentences.
- [ ] `mvn clean verify` passes, doclint and `DomainVocabularyTest` included, with the same test count as the base commit. This task adds no tests.

## Out of scope
- Changing any class's own Javadoc. If you spot an inaccuracy, report it in the commit body rather than fixing it.
- An `overview.html`, javadoc stylesheet or pom javadoc configuration.
- `jresolve-profiles-ie`'s `package-info`.
- Adopting doclint's `missing` group, which is an undecided policy in `PLAN.md`.

## Attempt 1 — failed

Tests PASS (646); review REQUEST CHANGES (commit 6b55ed3). Every criterion
met except one false claim.

- **Implies an estimator that does not exist.**
  `scoring/package-info.java:11-13` says model parameters "can be estimated
  from" `LabelledMatchExample`/`UnlabelledMatchExample`. The library does not
  train (`LabelledMatchExample` says so, and calls itself a shape for a future
  estimator). Reword: these types hold examples for estimating parameters
  outside the library.
- Also fix while there:
  - `evidence/package-info.java:7-8`: "a similarity and any features" is not
    true, since `FieldEvidence` has no features accessor. Name what it carries:
    category, similarity, frequency key, subsumption.
  - `scoring/package-info.java:16`: "A probability is only produced when the
    model carries prior odds" should say "the Fellegi-Sunter model", because
    `ProbabilityModel` is an unimplemented extension point that also returns a
    probability.
- Every other claim was checked and is accurate. Continue on branch
  `task/40-package-info`.
