# 33 — README as a developer guide

**Repo:** .
**Depends on:** 34 (lands under its check)
**Owns:**
- README.md
- jresolve-core/src/test/java/io/github/aindriub/jresolve/readme/ReadmeExamplesTest.java
- jresolve-profiles-ie/src/test/java/io/github/aindriub/jresolve/profiles/ie/readme/ReadmeExamplesTest.java

## Goal
A developer who has never seen this repository can go from the front page to a
working resolver without opening a test.

## Context
- docs/architecture.md — the module split and the type model, which the guide must describe accurately rather than approximately.
- docs/calibration.md — what the library will and will not claim about a probability. The guide must not undo that in the interest of a friendly example.
- jresolve-core/src/test/java/io/github/aindriub/jresolve/endtoend/EndToEndResolutionTest.java — the canonical rule-based construction, in neutral vocabulary.
- jresolve-profiles-ie/src/test/java/io/github/aindriub/jresolve/profiles/ie/endtoend/BeatTheJoinTest.java — the canonical domain-shaped construction, and the milestone-2 thesis.
- jresolve-core/src/test/java/io/github/aindriub/jresolve/endtoend/FellegiSunterResolutionTest.java — the probabilistic path end to end.
- docs/conventions.md — the vocabulary rule binds `jresolve-core`, tests included. Domain-shaped examples therefore live in the profiles module's test, never in core's.

## Shape
Build up, never sideways. Each section should leave the reader able to run
something, and should say what it still cannot do:

1. What this is, and what it is not — in particular that nothing here is calibrated.
2. Install, and the Java 8 target.
3. The smallest working resolver: two exact fields.
4. Fuzzy matching: normalization, similarity metrics, bands.
5. Cost tiers and rules — cheap comparisons first, hard vetoes between tiers.
6. Reading the result: decision, score, contributions, rejected candidates.
7. Aliases and subsumption — where a join stops being enough.
8. Fellegi-Sunter: `m`, `u`, frequency adjustment, and why the probability is the least of it.
9. Where to go next, and the honest limits.

## Acceptance
- [ ] **Every code example in the README is compiled and run by one of the two tests**, and the README says so with a link. An example that only appears in prose is one that rots.
- [ ] The two tests do not duplicate each other: neutral examples in core, domain-shaped in profiles. Core's file must satisfy `DomainVocabularyTest`.
- [ ] Each example is self-contained enough to paste and run, with its imports implied by the test that carries it.
- [ ] The guide states the Java 8 target, the module split, and that `jresolve-profiles-ie` is optional.
- [ ] **It repeats, rather than softens, the two claims this library exists to keep straight**: a similarity is not a probability, and nothing shipped is calibrated. A guide that makes the Fellegi-Sunter section look turnkey would undo `docs/calibration.md`.
- [ ] It states that the Irish alias tables are illustrative and not reference data, per D19, at the point where it first shows them.
- [ ] It does not promise features that do not exist: no blocking, no candidate index, no phonetics, no estimator, no logistic regression.
- [ ] `mvn clean verify` passes with the new tests.

## A judgement this task owes
Writing a guide means using the API as a stranger would, which is the first
time anyone has done that here.

Report in the commit body anything that was awkward to explain or to construct
— a builder step whose order matters and is not obvious, a type a consumer must
assemble by hand that the library could have offered. A guide that was hard to
write is evidence about the API, not about the guide, and it belongs in
`PLAN.md`.

## Out of scope
- Changing the API to make the guide easier. Record the awkwardness; do not fix it here.
- Javadoc, `docs/`, or anything under `docs/plan/`.
- Badges for services that do not exist yet.
