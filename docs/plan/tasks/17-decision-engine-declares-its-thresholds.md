# 17 — Let a decision engine declare the thresholds it actually applies

**Repo:** .
**Depends on:** none
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/decision/MatchDecisionEngine.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/decision/ThresholdDecisionEngine.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/decision/DecisionThresholds.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/api/EntityResolverBuilder.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/decision/**
- jresolve-core/src/test/java/io/github/aindriub/jresolve/api/**
- jresolve-core/src/test/java/io/github/aindriub/jresolve/endtoend/EndToEndResolutionTest.java

## Goal
Close D6's remaining hole — the largest open correctness gap in the library,
carried through two milestones.

`EntityResolverBuilder.build()` validates the `DecisionThresholds` passed to
`.thresholds(...)` against the scorer's scale
(`api/EntityResolverBuilder.java:264`). That object never reaches the engine
that decides. `MatchDecisionEngine` declares exactly one method — `decide(...)`
— and `ThresholdDecisionEngine` holds its thresholds privately with no getter,
so `build()` cannot inspect the object that actually decides anything.

What ships today is a Javadoc contract saying the two must be the same
instance. Both the tester and the reviewer confirmed it is sufficient *when
followed*, which is precisely the property a construction-time check exists to
stop depending on.

## Read this before designing the fix
The obvious fix — expose the engine's `ScoreScale` — **is not sufficient**, and
the existing test proves it. `EndToEndResolutionTest:622`
(`twoDifferentThresholdsInstancesSilentlyDecideAgainstTheEnginesOwnInstance`)
diverges two instances that are *both* `POINTS`: one declaring a match
threshold of 200.0 to `build()`, the other applying 50.0 in the engine. A
scale-only check passes that configuration happily. D6's own note anticipated
this — "expose its scale (and ideally its thresholds)" — and the second half is
the part that matters.

## Context
- docs/design-decisions.md#d6 — the scale/algorithm distinction and why a margin is meaningless without a scale.
- docs/plan/PLAN.md, "Known gaps" — D6's entry, including the concretely reproduced wrong-scale case (PROBABILITY thresholds returning MATCH by comparing a 10.0-point score against 0.9).
- jresolve-core/src/main/java/io/github/aindriub/jresolve/decision/MatchDecisionEngine.java — the whole interface, one method.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/api/EntityResolverBuilder.java:184-205 — the Javadoc stating the contract, and :264 — the check that cannot see the engine.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/MatchScorer.java — `MatchScorer` already declares `ScoreScale scale()`. The precedent for an interface exposing what it operates on is in the codebase already.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/evidence/FieldEvidence.java — task 10's `default` method, the pattern for adding to an interface without breaking implementors.

## Acceptance
- [ ] `MatchDecisionEngine` gains a way to declare the thresholds it applies, as a Java 8 `default` method returning null for "does not declare any". An abstract method would break every existing implementor, including a consumer's own and the one in `EndToEndResolutionTest`.
- [ ] Its Javadoc states plainly what returning null costs: `build()` cannot check that engine, and the same-instance contract is all that protects the caller. An engine that *can* declare should.
- [ ] `ThresholdDecisionEngine` overrides it and returns the thresholds it was constructed with.
- [ ] `DecisionThresholds` gains `equals` and `hashCode` over all four fields, so divergence can be detected by value rather than by identity — two separately constructed but identical instances are not a defect and must not fail the build.
- [ ] `build()` fails when the engine declares thresholds whose scale does not match the scorer's, with a message naming both scales. This closes the wrong-scale case D6 reproduced.
- [ ] `build()` fails when the engine declares thresholds that differ from the ones passed to `.thresholds(...)`, with a message naming the constraint. This closes the same-scale divergence the existing test documents.
- [ ] Neither message contains a field value; both name the constraint (`docs/conventions.md#errors`).
- [ ] An engine declaring nothing still builds, and the existing scale check against `.thresholds(...)` still applies to it — the fix must not make a legitimate custom engine unbuildable.
- [ ] `EndToEndResolutionTest:622` is rewritten: it asserts the divergent configuration is now **rejected at `build()`** rather than silently deciding against the engine's own instance. Its Javadoc and the `resolverWith(...)` helper's Javadoc, which both describe the hole as open, are updated to describe what is enforced.
- [ ] `followingTheSameInstanceThresholdsContractProducesTheDeclaredDecision` still passes unchanged — the supported wiring must keep working.
- [ ] `EntityResolverBuilder.thresholds(...)`'s Javadoc no longer says there is no in-library way to prevent divergence, because there now is. Whatever residual hole remains (an engine that declares nothing) is stated precisely instead.
- [ ] A test constructs an engine that declares nothing and asserts it still builds and resolves.
- [ ] No type, member or Javadoc word in these files names a person, name, address, date of birth or country.

## Out of scope
- Removing `.thresholds(...)` or deriving it from the engine. That is a breaking API change and a separate decision; this task makes divergence *impossible to build*, which is the correctness fix.
- `FieldDefinition.isRequired()` and the dead null-rule branch in `DefaultEntityResolver` — both live in this area and neither is this task's.
- Any change to how a decision is made once thresholds are agreed.
