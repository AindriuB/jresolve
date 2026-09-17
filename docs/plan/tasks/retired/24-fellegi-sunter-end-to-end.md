# 24 — Fellegi-Sunter through a whole resolver

**Repo:** .
**Depends on:** 21
**Owns:**
- jresolve-core/src/test/java/io/github/aindriub/jresolve/endtoend/FellegiSunterResolutionTest.java

## Goal
Prove the probabilistic path composes, the way task 08 proved the rule-based
one did — and prove the two claims D5 exists to make, rather than describing
them.

Milestone 1's lesson, recorded four times: the counter-question for a green
suite is "is it wired to anything". This task is where the FS types stop being
unit-tested in isolation.

## Where the fixtures live
This task owns **one file**. The synthetic source and candidate types it needs
are nested static classes inside `FellegiSunterResolutionTest` rather than
separate files, so the task stays inside its `Owns`.

Do **not** reuse `ExternalPerson` and `Owner` from the existing `endtoend`
package. They are domain-shaped by design — an end-to-end test models a
consumer's objects — but this test's own acceptance forbids naming a person,
and reusing them would break that on the first import. Two neutral record
types with two or three fields are enough here.

## Context
- docs/plan/tasks/retired/08-end-to-end-test.md and `EndToEndResolutionTest` — the shape of a whole-resolver test in this repo: a consumer's seat, hand-derived expectations, and every band computed by hand rather than pasted from a run.
- docs/design-decisions.md#d5, #d6 — the scale and the probability rule.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/decision/DecisionThresholds.java — thresholds declare the scale they were written against, and on `LOG2_LIKELIHOOD_RATIO` a margin is the log ratio of two candidates' likelihoods.

## Acceptance
- [ ] A resolver is assembled from a consumer's seat with `FellegiSunterScorer`, synthetic source and candidate types, and `DecisionThresholds` declared on `LOG2_LIKELIHOOD_RATIO`. It resolves a positive case to `MATCH` with a hand-derived expected weight.
- [ ] **The frequency claim, end to end.** Two candidates agreeing with the source on the same field and the same category, one on a corpus-common value and one on a rare value, rank with the rare agreement above the common one — and the test asserts the *ordering* and the fact that the two weights differ, not merely that both matched. This is D5's headline claim and the reason the frequency table exists; if it does not hold end to end, the milestone has not delivered it.
- [ ] The same scenario under a model with **no** frequency table ranks the two candidates equally, asserted in the same test. Without that control the first assertion cannot distinguish "frequency adjustment works" from "these two candidates differ for some other reason".
- [ ] **The probability claim, end to end.** The same resolve with a model carrying no prior odds yields a result whose score exposes a null probability; with prior odds set, a non-null one. Asserted in one test so the pair cannot drift.
- [ ] A negative case resolves to `NO_MATCH`, and an ambiguous one to `REVIEW` on a margin below the configured minimum — showing the decision engine reads this scale as readily as `POINTS`.
- [ ] **Task 17's guard is exercised on this scale.** Configuring `DecisionThresholds` on `POINTS` against this scorer fails at `build()`. That check was written against a rule scorer; this is the first scorer on a different scale, so it is the first time the check is doing anything a single-scale library would not.
- [ ] A cost-tier veto drops the candidate before the expensive field is prepared, asserted through a resolve with an invocation-counting extractor. **Amended at implementation — the original criterion was not achievable, and the reason is a finding.** `DefaultEntityResolver:110` always constructs the scorer's evidence with `complete = true`, and a rule's `REJECT` returns null at `:101` rather than scoring partial evidence. So the resolver never hands a scorer incomplete evidence, and `FellegiSunterScorer`'s unscorable path is unreachable through it. What *does* consume `MatchEvidence.isComplete()` is `CandidateRule`, which sees partial evidence between tiers at `:102`. The scorer's behaviour stays unit-tested in task 21; this records that it is not reachable end to end, rather than asserting it is.
- [ ] Determinism holds across shuffled candidate order.
- [ ] Every expected weight in this file is hand-derived in a comment from the `m` and `u` values configured, and the comment shows the arithmetic. **No expectation is produced by running the code and pasting the result** — the defect that rejected task 02's first attempt.
- [ ] Fixtures are synthetic and neutral; no type, member or Javadoc word in this file names a person, name, address, date of birth or country.

## The verdict this task owes
Answer in the commit body, in one paragraph: **does the probabilistic path
give a consumer anything the rule-based one does not, and which assertion
shows it?**

The honest answer may be "not yet, because nothing here is calibrated" — D5's
own note says a posterior from prior odds is conditional on the model's
assumptions and is not evidence of empirical calibration. If that is the
answer, say so plainly and name what would change it. A milestone that ships
Fellegi-Sunter and claims more than frequency adjustment and an explicit scale
would be claiming more than it earned.

## Out of scope
- Changing any production class. If a test cannot pass without one, stop and report — the change belongs to the task that owns the file.
- Comparing FS against the rule scorer on accuracy. There is no labelled corpus here, and a comparison without one would be theatre.
- Performance measurement (D17 puts the JMH harness behind v1).
