# 21 — The Fellegi-Sunter scorer

**Repo:** .
**Depends on:** 20
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/FellegiSunterScorer.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/scoring/FellegiSunterScorerTest.java

## Goal
`W = Σ log₂(m/u)`, on the `LOG2_LIKELIHOOD_RATIO` scale, with a probability
exposed only where the model is calibrated to produce one.

## Context
- docs/design-decisions.md#d5 — prior odds turn a likelihood ratio into a posterior; without them `probability` is null and only the weight is exposed.
- docs/design-decisions.md#d6 — a score carries its scale. This scorer's scale is `LOG2_LIKELIHOOD_RATIO`, and on that scale a margin *is* the log ratio of two candidates' likelihoods, which is what the REVIEW threshold wants.
- docs/design-decisions.md#d4 — evidence can be incomplete after a cost-tier short-circuit; a scorer that cannot score partial evidence rejects it explicitly rather than treating absent fields as `MISSING_BOTH`, which would be a silent lie.
- docs/design-decisions.md#d11 — the conditional-independence assumption, stated not assumed silently.
- docs/spec/original-design.md §43 and §46 — the formula, and that a raw score is not automatically a probability. Read those two sections only.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/RuleBasedScorer.java — the existing `MatchScorer`: how it builds contributions, stamps `ALGORITHM`, and returns `ScoringResult.scorable` / `.unscorable`.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/ScoringResult.java — already carries the scorable/unscorable split this task needs.

## Acceptance
- [ ] `FellegiSunterScorer` implements `MatchScorer`, is final and immutable, and rejects a null model at construction.
- [ ] `scale()` returns `LOG2_LIKELIHOOD_RATIO`, and the score it produces carries that scale and a stamped algorithm constant, as `RuleBasedScorer` does.
- [ ] The total is `Σ log₂(m/u)` over the fields in the evidence; a test hand-computes the expected weight for a two-field case from `m` and `u` values chosen so the arithmetic is checkable by eye, and asserts the total. **The expected value is derived by hand, never by running the scorer and pasting its output.**
- [ ] Each field contributes a `FieldContribution` carrying its own weight, so an explanation shows which field moved the score and by how much.
- [ ] The frequency key from each field's evidence is passed to `uProbability`; a test asserts that the same category on a common key scores lower than on a rare one, through the scorer rather than only through the model.
- [ ] **A probability is exposed only when the model carries prior odds.** Without them `Score.getProbability()` is null and only the weight is exposed; with them the posterior is computed from the prior odds and the summed weight. Both are tested.
- [ ] The Javadoc states plainly that a posterior computed this way is conditional on the model's own assumptions and is not evidence of empirical calibration (D5's wording is deliberate; keep its force).
- [ ] **Incomplete evidence is rejected, not guessed.** Evidence flagged incomplete yields `ScoringResult.unscorable` with a template key naming why, rather than a score computed over the fields that happen to be present. A test asserts this, and asserts the scorer does *not* treat absent fields as `MISSING_BOTH`.
- [ ] A category with no configured `m`/`u` surfaces the model's configuration error rather than contributing zero.
- [ ] A composite field declared in the model contributes **once**, not once per member field; a test asserts the total for a model declaring two fields composite differs from the same model without the declaration, and states in a comment which is the double-counting one.
- [ ] The class Javadoc repeats the conditional-independence assumption and points at `docs/calibration.md` (D11 requires the repetition; a reader of this class should not have to find the design decisions to learn it).
- [ ] A test asserts determinism: the same evidence scores identically twice.
- [ ] No type, member or Javadoc word in these files names a person, name, address, date of birth or country; fixtures are neutral tokens.

## A judgement this task owes
`RuleBasedScorer` exists and works. Adding a second `MatchScorer` is the first
time the interface has had two implementations, so it is the first real test
of whether that abstraction holds.

Report in the commit body whether anything in `MatchScorer`,
`ScoringResult` or `FieldContribution` had to bend to accommodate a
probabilistic scorer — and if something did, say what, because that is a
finding about the design rather than a detail of this task. If nothing bent,
say that too: it is evidence the shape was right.

## Out of scope
- Changing `MatchScorer`, `ScoringResult` or `FieldContribution`. If one genuinely must change, stop and report rather than editing outside this task's `Owns`.
- Estimating probabilities from data; `docs/calibration.md` (task 23) describes where they come from.
- Wiring the scorer into a resolver end to end — task 24.
- Logistic regression (D17).
