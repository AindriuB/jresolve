# 29 — The alias strength tiers are reordered

**Repo:** .
**Depends on:** nothing
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/alias/DefaultAliasRepository.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/alias/DefaultAliasRepositoryTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/field/AliasAwareFieldComparatorTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/field/FieldComparatorNullSafetyTest.java
- jresolve-profiles-ie/src/test/java/io/github/aindriub/jresolve/profiles/ie/IrishNameAliasesTest.java
- jresolve-profiles-ie/src/test/java/io/github/aindriub/jresolve/profiles/ie/IrishPipelineTest.java
- jresolve-profiles-ie/src/test/java/io/github/aindriub/jresolve/profiles/ie/endtoend/BeatTheJoinTest.java

## Goal
`ALIAS_VARIANT` > `ALIAS_TRANSLATION` > `ALIAS_NICKNAME`, so a derived pair
bottlenecked on a nickname edge claims the least.

## Context
- docs/design-decisions.md#d7 — the decision, **and the recorded objection to it**. Read both paragraphs. The objection is not an argument against doing this task; it is the reason a future reviewer may revisit it, and it must survive in the file.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/alias/DefaultAliasRepository.java:61-65 — `STRENGTH_DESCENDING`, the one production line this task changes.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/alias/DefaultAliasRepository.java:150 — the builder's guard rejects any category outside these three; it reads the same list, so it needs no change, and a test should prove that.
- docs/plan/HISTORY-INDEX.md — grep `Irish profiles land` for task 15, where a Pádraig/Paddy expectation of `ALIAS_NICKNAME` was corrected to `ALIAS_TRANSLATION` by the *old* ordering. This task flips it back, so that correction is now the wrong way round.

## Acceptance
- [ ] `STRENGTH_DESCENDING` is `ALIAS_VARIANT`, `ALIAS_TRANSLATION`, `ALIAS_NICKNAME`, in that order.
- [ ] A derived pair whose strongest path bottlenecks on a nickname edge reports `ALIAS_NICKNAME`; one bottlenecking on a translation edge reports `ALIAS_TRANSLATION`. Both asserted directly, not inferred from a resolver outcome.
- [ ] The Pádraig/Paddy pair — derived through Patrick — now reports `ALIAS_NICKNAME`. Its test comment is rewritten to explain the new ordering rather than being left stating the old rule's reasoning.
- [ ] A **directly declared** pair still reports exactly the kind it was declared with, for all three categories. Reordering the tiers must not disturb declared edges, only derived ones.
- [ ] Maximum-bottleneck is still maximum-bottleneck: a test covers a pair with two paths of different bottlenecks and asserts the **stronger** path wins, since reordering the tiers is exactly the change that could invert this.
- [ ] The builder still rejects a category outside the three, with the same message.
- [ ] **`BeatTheJoinTest` still proves its thesis.** Run it first, record what it asserted before the change, and state in the commit body whether its outcome moved. If the MATCH/NO_MATCH verdicts change, stop and report — milestone 2's thesis is not this task's to alter.
- [ ] `mvn clean verify` passes across both modules. Test count stays 582 unless a test is added, in which case say which and why.

## A judgement this task owes
This reorder was decided twice — confirmed as-is, then reopened and swapped —
and the swap went against the objection recorded in D7.

Report in the commit body how many test expectations actually moved, and
whether any of them looked *more* natural afterwards than before. Task 15's
implementer originally expected Pádraig/Paddy to be a nickname and was
corrected by the old ordering; if several expectations shift back toward what
their authors first wrote, that is evidence for the new order that no argument
produced, and it belongs in `PLAN.md`.

## Out of scope
- Making the ordering configurable. That was offered and not chosen.
- Changing which categories exist, or admitting a fourth.
- Changing any `m`/`u` value, or any scoring model that weights these categories.
- Touching `IrishNameAliases`'s table content — task 28 replaces it wholesale.
