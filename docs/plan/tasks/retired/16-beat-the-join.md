# 16 — Prove the library beats a join

**Repo:** .
**Depends on:** 15
**Owns:**
- jresolve-profiles-ie/src/test/java/io/github/aindriub/jresolve/profiles/ie/endtoend/**
- jresolve-core/src/test/java/io/github/aindriub/jresolve/endtoend/EndToEndResolutionTest.java

## Goal
Task 08's reviewer closed milestone 1 with a specific charge: every positive
outcome in the end-to-end suite was carried by exact surname and exact date of
birth — the two columns a plain SQL join would match on — so composition was
proven and resolution quality was not. This task answers that charge or shows
it still stands.

The milestone's thesis is falsifiable, so test it as one: **a resolve that
succeeds here must fail under exact-only comparison.** If it does not, D7 and
D9 have not bought anything and the task should say so rather than pass.

## Context
- docs/plan/PLAN.md, "The milestone assessment (from task 08's reviewer)" — the charge in full, and why D7 and D9 were named as the gap rather than as polish.
- docs/plan/PLAN.md, "Known gaps" (wave 5) — three defects in the existing suite, all to be closed here.
- jresolve-core/src/test/java/io/github/aindriub/jresolve/endtoend/EndToEndResolutionTest.java — the milestone-1 suite. Read it before writing the new one; the scenarios are reusable, the comparators are not.
- docs/design-decisions.md#d9 and #d7.

## A constraint worth knowing before you start
`jresolve-core` cannot depend on `jresolve-profiles-ie` — the dependency runs
the other way and a test-scoped edge would cycle the reactor. So the core
`endtoend` suite can only ever exercise core comparators, and **the proof suite
lives in `jresolve-profiles-ie`**. Do not try to add a test dependency to
`jresolve-core/pom.xml`; that is the boundary D15 exists to make structural.

## Acceptance
- [ ] A new end-to-end suite in `jresolve-profiles-ie` wires a resolver from `IrishNameAliases` and `IrishAddressPipeline` against a synthetic candidate set.
- [ ] **The falsification test.** At least one scenario resolves to `MATCH` where surname is an alias rather than an exact agreement, and the same source and candidates under exact-only comparators yield `NO_MATCH`. Both halves are asserted **in the same test**, so the contrast cannot rot independently.
- [ ] A second scenario carries a positive outcome on address subsumption where generic Levenshtein on the normalized string would band lower; the test asserts both the band under the real pipeline and the band under the generic comparator, and that they differ.
- [ ] §88's expected bands are asserted by value, not described in a comment. Where the implementation still cannot reach the expected band, the test asserts what it *does* produce and names the gap — task 08 attempt 1 was rejected for exactly the inverse of this.
- [ ] D9's §90 ambiguous case resolves to `REVIEW` with both candidates subsuming the source, and the test asserts the subsumption direction — not merely that the decision was `REVIEW`. The point is that it is REVIEW for the right reason.
- [ ] A negative scenario asserts the alias layer does not over-match: two genuinely different values that share no alias group do not agree.
- [ ] Determinism holds across shuffled candidate order, as in the milestone-1 suite.
- [ ] The tied-scores determinism test uses **four** tied candidates, not two — an unstable sort would rarely swap exactly two, so the milestone-1 version does not test the stable-sort claim it names (PLAN.md, wave 5).
- [ ] `FieldContribution` values are pinned, not only categories, so a weight change with unchanged bands fails a test rather than riding on the total assertion (PLAN.md, wave 5).
- [ ] The garbled sentence at `EndToEndResolutionTest.java:67-69` is corrected: it says two exact-match fields net +10, when it is firstName (−5) and address (+15) that net +10 (PLAN.md, wave 5).
- [ ] The milestone-1 suite's address field is re-pointed or its comment records that it deliberately exercises the generic comparator, with the real pipeline now covered in the profiles suite. A passing end-to-end test standing in for a feature that has since arrived is the exact trap PLAN.md's wave-5 note warns about.
- [ ] `mvn clean verify` passes for the reactor. Record the new test total; milestone 1 closed at 262.

## The judgement this task owes
Return a verdict, not only a pass. Answer in one paragraph: **does a consumer
now get a better answer than a join, and which assertion proves it?** If the
honest answer is no, say so and name what is missing — that is a more useful
milestone close than a green suite. Task 08's lesson was that a correct
measurement can support a claim broader than it earned; the counter-question is
"is it wired to anything", not "does it pass".

## Out of scope
- Changing any comparator, pipeline or alias table. If a test cannot be made to pass without a production change, stop and report — the change belongs to the task that owns that file.
- Fellegi-Sunter, frequency adjustment, or blocking.
- Performance measurement.
