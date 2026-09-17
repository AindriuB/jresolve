# 32 — Core pins its own behaviour

**Repo:** .
**Depends on:** nothing
**Owns:**
- jresolve-core/src/test/java/io/github/aindriub/jresolve/evidence/FieldEvidenceTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/decision/MatchDecisionEngineTest.java

## Goal
Every `default` method on a core interface has a core test proving that an
implementation which does not override it receives the documented fallback.

## Context
- docs/plan/PLAN.md — "Raised in milestone 5, wave 1", fifth bullet. Task 29 found that core's `DefaultAliasRepositoryTest` never mixed translation with nickname, so 31 tests stayed green under a reorder of exactly those tiers; the behaviour was pinned only in `jresolve-profiles-ie`.
- The three `default` methods, which are behaviour living on an interface rather than in a class, and so the likeliest place for coverage to go missing:
  - `evidence/FieldEvidence.java` — `getSubsumption()`, added by task 10.
  - `decision/MatchDecisionEngine.java` — `declaredThresholds()`, added by task 17.
  - `scoring/FellegiSunterModel.java` — `compositeRuleFor(group)`, added by task 26 and **already covered** by `DefaultFellegiSunterModelTest`. Use that test as the shape to copy; do not duplicate it.
- docs/design-decisions.md#d6 — what `declaredThresholds()` returning null means, and why an engine declaring nothing still builds. The test must pin the documented behaviour, not merely observe the current one.

## Acceptance
- [ ] A test implements `FieldEvidence` without overriding `getSubsumption()` and asserts it returns `NOT_APPLICABLE` — the value task 10's Javadoc promises, quoted in a comment so a later change to either surfaces as a contradiction.
- [ ] A test implements `MatchDecisionEngine` without overriding `declaredThresholds()` and asserts it returns null, **and** that `EntityResolverBuilder.build()` accepts such an engine. D6's residual hole is deliberate; this pins it as intended behaviour rather than leaving it to be "fixed" by someone reading it as an oversight.
- [ ] Each test says in a comment why a `default` method needs its own test at all: an implementation that does not override it is exactly the implementation no existing test exercises.
- [ ] Neither test duplicates coverage that already exists. Where an existing test already pins the behaviour, say so in the commit body and add nothing.
- [ ] Both new suites are mutation-checked: change the `default` body and show the new test goes red. A test that passes whatever the default returns is not pinning anything.
- [ ] Fixtures are neutral tokens, and the files satisfy task 30's checker.
- [ ] `mvn clean verify` passes.

## A judgement this task owes
The gap says a core behaviour exercised only through a profile is one core does
not pin. This task closes that for the three `default` methods, which is the
bounded part of it.

Report in the commit body whether `default` methods were the right place to
look — whether the audit found anything genuinely unpinned, or whether the
three were already covered and the real risk lies somewhere this task did not
search. A negative result is worth as much here as a positive one, and belongs
in `PLAN.md` either way.

## Out of scope
- A general coverage audit of core, or a coverage tool. This task searches one named place.
- Changing any production behaviour. If a `default` method's documented fallback and its actual one disagree, stop and report — that is a defect, not this task's to fix.
- `jresolve-profiles-ie`.

---

## Outcome: no tests added, and why

**All three `default` methods were already pinned.** The audit found nothing to
close.

- `FieldEvidence.getSubsumption()` — `FieldEvidenceDefaultsTest` already
  implements the interface without overriding it and asserts `NOT_APPLICABLE`,
  with the reasoning in a comment: `NEITHER` would claim containment was
  computed and did not hold, which such an implementation never claimed. That
  is this task's acceptance criterion, written before this task existed.
- `MatchDecisionEngine.declaredThresholds()` — `EntityResolverBuilderTest:352`
  asserts an engine that does not override it returns null, that `build()`
  accepts that engine, and that the resolver still decides. Both halves of the
  criterion, already covered.
- `FellegiSunterModel.compositeRuleFor()` — covered by task 26, as this file
  predicted.

Adding tests here would have duplicated coverage, so none were added.

## What the audit did instead, and what it found

`default` methods were the wrong place to look, so the hypothesis was tested
directly by task 29's own method: mutate a core production behaviour, run
**core's tests alone**, and see whether core catches it or only
`jresolve-profiles-ie` does.

| Probe | Result |
|---|---|
| `SimilarityBands` returns `HIGH` where it should return `VERY_HIGH` | core catches it |
| The alias strength tiers are scrambled | core catches it |
| An alias hit carries a similarity it should not | core catches it |
| `DefaultTokenSplitter` drops digits | core catches it |

Four probes, four catches. **Core does pin the behaviours probed**, and the
blind spot task 29 found was not the general condition the gap described.

## The gap, corrected

What task 29 actually hit was narrower than "core behaviour covered only in
profiles". Every individual behaviour was pinned; what was missing was a
**combination** — the merge of a translation edge with a nickname edge, a pair
of categories no core test put together. Single-behaviour coverage was
complete and told nobody anything about the pair.

That is a different and harder problem than the gap as written, because the
combinations of a value type grow faster than anyone writes tests, and no
mutation probe of a single line finds a missing pair. `PLAN.md` should carry
this correction rather than record the gap as closed.

The probe method above is worth re-running when a core behaviour looks thinly
covered. It is cheap, and a negative result from it is meaningful.
