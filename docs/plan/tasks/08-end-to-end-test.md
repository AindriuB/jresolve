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

## Attempt 1 - failed

Tester PASS (260 tests; all three gap claims independently recomputed and
accurate; determinism test fails when ranking is made order-dependent; both
thresholds-contract tests distinguish compliance from violation). Reviewer
CHANGES. The suite is honest about what it does not cover and not yet honest
enough about what it does.

**Defect 1 - must fix.** `EndToEndResolutionTest.java:225-234` derives the
address MEDIUM band as plain arithmetic and never states that §88 expects
`VERY_HIGH`. The same omission applies to firstName, where §88 expects `ALIAS`
and the file never names that expectation. The class Javadoc at :56-63 spells out
only the alias consequence. A test asserting MEDIUM with no comment reads to the
next person as "MEDIUM is correct", which is how a temporary gap becomes
permanent behaviour someone later defends. State the spec expectation at each
assertion that misses it, naming the milestone-2 feature that would close it -
D7 for the alias, D9 for the address.

**Defect 2 - must fix.** The class Javadoc says "the other three fields carry
it", which understates what actually decides §88. lastName EXACT (30) plus
dateOfBirth EXACT (25) reach 55 against a match threshold of 50 on their own. The
two evidence types §88 exists to demonstrate net +10 between them, and the fuzzy
first name is a penalty of -5. **The positive scenario would pass as a
two-exact-key join.** Say so plainly in the Javadoc. The test is not wrong and
should not be weakened or re-tuned to manufacture dependence on the weak fields -
the point is that a reader must not mistake this for evidence that fuzzy matching
works.

**Verified accurate, keep as they are.** All three gap claims check out against
the merged code, recomputed independently: `sean`/`john` distance 3 over length 4
gives 0.25, LOW; address 1 - 4/23 gives 0.826, MEDIUM under the 0.70/0.85 bands;
§90 ties at exactly 105.0 with margin 0.0, and REVIEW follows from
`margin < minimumMargin` while the score clears `matchThreshold`. The implementer
neither overstated nor understated what the library does.

Also correct and not to be undone: the determinism test is committed rather than
probed, uses four distinct scores so removing the sort genuinely changes
ordering, and fails by name when ranking is made order-dependent. The two
thresholds tests cover compliance and silent violation, and
`twoDifferentThresholdsInstancesSilentlyDecideAgainstTheEnginesOwnInstance` is a
deliberate characterization test of task 07's known D6 hole - when milestone 2
makes `MatchDecisionEngine` expose its scale, that test will fail, which is the
signal that the gap has been closed. Do not delete it; update it then.

**Suggestions - take unless you disagree.**
- `:239` - the positive test pins only the total score, so a future alias or
  address change that shifts two bands in opposite directions passes silently.
  Assert the four `FieldContribution` categories as well.
- `:407` - the shuffle test deliberately avoids ties, leaving
  `ThresholdDecisionEngine`'s stable-sort claim untested. Tied candidates keeping
  input order is the one case where §96 determinism is actually order-sensitive.
- `:275` - an exact 0.0 tie passes for any positive `minimumMargin`; a small
  non-zero margin would exercise the comparison D9 will eventually produce.
- `:319` - the `isNotEqualTo(CONFLICT)` assertion is tautological after the
  preceding assert.

**For the milestone record, from the reviewer, and it belongs in HISTORY rather
than only here.** Asked whether this suite gives a consumer reason to believe the
library works: for composition, yes - builder wiring, tier ordering, the
cheap-field veto short-circuit, missing-versus-conflict, threshold and margin
decisioning, and determinism are all genuinely exercised end to end. For
resolution quality, no - every positive outcome is carried by exact surname and
exact date of birth, the two fields a plain SQL join would match on. Milestone
2's D7 and D9 are not polish; they are the gap between "the pieces compose" and
"a consumer gets a better answer than a join".
