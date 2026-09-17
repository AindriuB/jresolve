# 26 — The composite combination rule becomes selectable per group

**Repo:** .
**Depends on:** nothing
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/CompositeRule.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/FellegiSunterModel.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/DefaultFellegiSunterModel.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/FellegiSunterScorer.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/scoring/DefaultFellegiSunterModelTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/scoring/FellegiSunterScorerTest.java
- docs/calibration.md

## Goal
A consumer declaring two composite groups of different correlation strength can
say how each combines, instead of one answer being forced on both.

## Context
- docs/design-decisions.md#d11 — the decision and its reasoning: the rule is selected on the group, not the model, because correlation strength is a property of the fields rather than of the model holding them.
- docs/calibration.md — the "What the library does about it" section currently states the smallest-member rule as fixed. It has to say *selectable, defaulting to smallest*, and carry the caveat below.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/FellegiSunterModel.java:104 — `Collection<Set<String>> compositeGroups()`. A per-group rule has to reach the scorer without breaking this interface.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/DefaultFellegiSunterModel.java:224-238 — `composite(Collection<String>)` and its two-distinct-members guard.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/evidence/FieldEvidence.java — the Java 8 `default` method pattern for additive interface change (tasks 10, 17). A `default CompositeRule compositeRuleFor(Set<String> group)` returning `SMALLEST` keeps `compositeGroups()` untouched and every existing implementation compiling; take that route unless you find a reason it fails.

## Acceptance
- [ ] `CompositeRule` is an enum with exactly `SMALLEST`, `AVERAGE`, `STRONGEST`, each Javadoc'd with what it claims about the shared signal.
- [ ] `composite(fields, rule)` declares a group with an explicit rule; `composite(fields)` still declares one with `SMALLEST`. A test asserts the one-argument form is exactly equivalent to passing `SMALLEST`.
- [ ] **Nothing already built changes behaviour.** A test builds a model with the pre-existing API only and asserts the same total as before this task.
- [ ] Two groups in one model may carry different rules, and a test asserts a model with one `SMALLEST` group and one `STRONGEST` group applies each to its own group — this is the whole point of the task and must fail if the rule is read globally.
- [ ] Each rule is asserted by a hand-derived total: `SMALLEST` takes the least member weight, `STRONGEST` the greatest, `AVERAGE` the arithmetic mean over **present** members only. **Expected values are derived by hand, never pasted from a run.**
- [ ] `AVERAGE` over one present member equals that member's weight, and a test pins it — the degenerate case is where an off-by-one in the denominator hides.
- [ ] A suppressed member still appears in the explanation with a template key recording it was counted as part of a composite, for every rule and not only `SMALLEST`.
- [ ] A null rule is rejected at `composite(...)` with a message naming the constraint and not the field values (D10).
- [ ] `docs/calibration.md` states the rule is per group and selectable, and carries the caveat: `u` is measurable from a corpus and `m` is estimable by EM, but **no measurement procedure exists for this choice**, so a consumer who has not measured the within-group correlation should leave it at `SMALLEST`.
- [ ] No type, member or Javadoc word in these files names a person, name, address, date of birth or country.

## A judgement this task owes
`docs/calibration.md` is a design artefact, so under `CLAUDE.md` rule 7 as
amended it is written by whoever owns the design rather than by `scribe`. This
task is the first to exercise that amended rule.

Report in the commit body whether writing the caveat alongside the code was
better than recording it separately — the amendment was argued on exactly that
ground, and this is the first chance to check the argument against practice.

## Out of scope
- Estimating the within-group correlation, or offering a default other than `SMALLEST`.
- Changing how `u` is frequency-adjusted, or anything in `TermFrequencyTable`.
- A fourth rule. Three cover the argument; a fourth needs its own case.
