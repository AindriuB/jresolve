# 08 — Prove the milestone with an end-to-end resolve

**Repo:** .
**Depends on:** 05, 06, 07
**Owns:**
- jresolve-core/src/test/java/io/github/aindriub/jresolve/endtoend/**

## Goal
Assemble the whole pipeline from a consumer's seat — synthetic test-only source
and candidate types, a builder-configured resolver, rule scoring on the POINTS
scale, threshold decisioning — and assert a positive, a negative and an
ambiguous outcome. This is the milestone gate: it is the first evidence that the
layers compose.

## Context
- docs/spec/original-design.md §87–§90 — the end-to-end test model and the worked positive, negative and ambiguous examples. Read those four sections only, and treat their numbers as illustrative.
- docs/design-decisions.md#d9 — the §90 ambiguous case is a subsumption problem, not a similarity one; in this milestone reproduce ambiguity through a small margin between two candidates rather than by modelling subsumption.
- docs/conventions.md#tests — synthetic fixtures only, no real personal data including names drawn from public datasets; test names read as sentences; hand-calculated expectations with the arithmetic in a comment.
- docs/architecture.md — the domain-vocabulary ban is on `jresolve-core` main sources; these fixtures live in test sources and may name a person.

## Acceptance
- [ ] Test-only `ExternalPerson` and `Owner` fixture types exist under `endtoend/`, immutable, with deliberately different shapes on at least one field so the asymmetric `field(...)` overload is exercised.
- [ ] Every fixture value is invented; a comment at the top of the fixture file states that and names no external source.
- [ ] One resolver is built once per test with at least four fields spanning at least two cost tiers, a `RuleBasedScorer` with hand-chosen POINTS weights, and a `ThresholdDecisionEngine` whose thresholds declare `ScoreScale.POINTS`.
- [ ] A positive scenario returns `MATCH`, with the expected candidate, and the expected total score computed by hand in a test comment.
- [ ] A negative scenario returns `NO_MATCH` and `getMatch()` is null.
- [ ] An ambiguous scenario with two close candidates returns `REVIEW` because the margin falls below `minimumMargin`, asserted on the margin and not only on the decision.
- [ ] A scenario with a `CandidateRule` vetoing on a conflicting cheap field asserts the vetoed candidate is absent from the ranked list and that the expensive field's preparer was never invoked for it.
- [ ] A scenario with a missing value on one side asserts `MISSING_ONE` in the evidence and that it did not become a `CONFLICT`.
- [ ] Running the same resolve twice returns equal decisions, scores and candidate ordering.
- [ ] `mvn clean verify` passes at the tip of this branch, and the close-out reports the observed test count and the commit it was measured on.

## Note carried from wave 4 (task 07) — two gaps only this task can close

These do not change or add to the acceptance criteria above; they are context
for writing the fixtures and scenarios.

- **Determinism across shuffled candidate order has no persisted test
  anywhere in the repository.** Task 07's tester probed it directly for
  attempt 2 (20 shuffles of a four-candidate list against a fixed source;
  decision and match identical every time) and it holds today, but nothing
  would fail the build if it broke — the probe was ad hoc, not committed.
  §96 makes determinism a hard requirement, and this task, as the end-to-end
  gate, is where a persisted test belongs: run one resolve, shuffle the
  candidate collection, run it again, assert equal decision, match and
  ordering.
- **The `.thresholds(...)` same-instance contract (see `PLAN.md`'s Known
  gaps, task 07/D6) is enforced by nothing but documentation**, and this
  task's fixtures are the first real wiring where a violation would surface.
  Build the resolver's `ThresholdDecisionEngine` and its `.thresholds(...)`
  call from the *same* `DecisionThresholds` instance, and add an explicit
  assertion that doing so produces the correct decision — this both follows
  the documented contract and is the only test in the milestone that checks
  the contract is followable.

## Out of scope
- Any change under `jresolve-core/src/main/java/**` — if a scenario cannot be expressed, report the missing capability as a successor task rather than editing another task's files.
- Blocking, `CandidateIndex`, Fellegi-Sunter, logistic regression, and any profile-module fixture.
- A performance or candidate-count benchmark.
- Promoting these fixtures into a shared test-support module.
