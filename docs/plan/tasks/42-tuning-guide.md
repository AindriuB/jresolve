# 42 — Write `docs/tuning.md`: cutting false positives

**Repo:** .
**Depends on:** 36, 37
**Owns:**
- docs/tuning.md (new)
- jresolve-core/src/test/java/io/github/aindriub/jresolve/tuning/TuningGuideExamplesTest.java (new)

## Goal
A consumer whose resolver matches records it should not has no guide today.
They need to learn how to read why a candidate matched, and which of the
library's levers moves that outcome. This guide covers both, with every
example compiled, and it never implies that any number it shows is
calibrated.

## Context
- jresolve-core/src/main/java/io/github/aindriub/jresolve/result/MatchResult.java, FieldContribution.java and RejectedCandidate.java — contributions and `getRejectedCandidates()`, which are how you diagnose a match.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/decision/DecisionThresholds.java:37 — `(matchThreshold, reviewThreshold, minimumMargin, scale)`.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/RuleBasedScorer.java — negative `CONFLICT`/`LOW` weights, `requiredField`, and the `build()` rule from task 36.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/field/SimilarityBands.java:22-25 — the default band cut-offs.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/field/CostTiers.java and api/CandidateRule.java — tiers, and vetoes between tiers.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/DefaultFellegiSunterModel.java:169-257 — `probabilities`, `ignore`, `frequencies`, `priorOdds`, and `composite` with `CompositeRule`.
- docs/calibration.md — especially "The five words this library refuses to blur" and "What a probability from this library is worth". The guide must agree with it.
- jresolve-core/src/test/java/io/github/aindriub/jresolve/readme/ReadmeExamplesTest.java — the pattern of one test per documented example.
- docs/conventions.md#naming — the test lives in core, so every example uses neutral vocabulary.
- Baseline: `mvn clean verify` on b80acc2 gives 634 tests (553 core + 81 profiles). Wave 1 changes the count, so state the figure on the merged `main` this task branches from.

## Acceptance
- [ ] `docs/tuning.md` has a section on each of these topics:
  - diagnosing a false positive from `MatchResult`: per-field contributions, the score against the thresholds, and `getRejectedCandidates()`;
  - `matchThreshold` and `reviewThreshold`, and `minimumMargin` for near-ties;
  - negative `CONFLICT` and `LOW` weights, and `requiredField`;
  - custom `SimilarityBands`;
  - cost tiers and `CandidateRule` vetoes;
  - Fellegi-Sunter frequency tables, composite rules (`SMALLEST`/`AVERAGE`/`STRONGEST`) and prior odds;
  - sweeping thresholds against a hand-labelled sample.
- [ ] Each Java code block in `docs/tuning.md` is compiled and run by a test in `TuningGuideExamplesTest`. Each test asserts the effect its section claims, such as a margin turning a `MATCH` into `REVIEW`, or a veto moving a candidate into `getRejectedCandidates()`. The guide links to the test file.
- [ ] The threshold-sweep section describes a procedure the consumer runs on their own labelled data. It ships no sweep tool and no recommended threshold values.
- [ ] The guide states that nothing shipped is calibrated, and it links `docs/calibration.md` when it discusses Fellegi-Sunter or prior odds. No sentence calls a score or probability "calibrated", "accurate" or "the probability of a match" without that qualification. The reviewer checks this against calibration.md's five words.
- [ ] The guide promises no features that do not exist: no blocking, candidate index, phonetics, estimator or logistic regression.
- [ ] `TuningGuideExamplesTest` passes `DomainVocabularyTest`, and its fixtures carry the sanctioned synthetic-data wording.
- [ ] `mvn clean verify` passes. The commit body states the count: the base figure plus the tests added.

## Out of scope
- `README.md`, which links here (task 41). `docs/calibration.md` is a design artefact and is not edited.
- API changes. Record anything awkward in the commit body.
- Domain-shaped examples, and anything in `jresolve-profiles-ie`.
