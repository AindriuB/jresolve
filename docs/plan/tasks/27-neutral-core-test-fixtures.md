# 27 — Core's test fixtures lose their domain vocabulary

**Repo:** .
**Depends on:** nothing
**Owns:**
- jresolve-core/src/test/java/io/github/aindriub/jresolve/endtoend/EndToEndResolutionTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/endtoend/ExternalPerson.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/endtoend/Owner.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/endtoend/FellegiSunterResolutionTest.java

## Goal
`jresolve-core` satisfies its own domain-vocabulary rule in `src/test` as well
as `src/main`, so the grep that rule calls itself checkable by returns nothing.

## Context
- docs/conventions.md — the rule as amended, and the exact grep command it now carries. That command is the acceptance criterion; do not invent a different one.
- docs/architecture.md — why the boundary exists at all: core may not know what a domain is, and a fixture that teaches it one is the same leak as a production type that does.
- docs/plan/HISTORY-INDEX.md — grep `Eight standing questions decided` for why this rule changed and what the measurement found.
- jresolve-profiles-ie/src/test/java/io/github/aindriub/jresolve/profiles/ie/endtoend/BeatTheJoinTest.java — the domain-shaped end-to-end test **already exists**, in the module where the vocabulary is the point. Core's version does not need to carry that weight, which is what makes this rename affordable.

## Acceptance
- [ ] The documented grep, run verbatim over `jresolve-core/src`, returns **no matches**. Paste the command and its empty output in the commit body.
- [ ] `ExternalPerson` and `Owner` are renamed. Both are type names, so this is a rename of the types and their file names, not only their members.
- [ ] The two records stay **structurally different from each other** — different field counts and shapes. D1's whole point is that source and candidate need not share a value type, and a rename that accidentally makes them symmetric destroys what `EndToEndResolutionTest` proves.
- [ ] Field-level semantics survive the rename: the fixture keeps one high-selectivity field, one low-selectivity field and one date-like field, because the tests' expectations depend on those roles even after the words change.
- [ ] `mvn clean verify` passes with **the same test count as before**, 582. A rename that changes the count has dropped or duplicated a test.
- [ ] No test's assertion values change. If one must, stop and report — a rename that alters an expectation is not a rename.
- [ ] Test method names are renamed too where they carry the vocabulary; a method called `resolvesAPersonByName` fails this task's own grep.
- [ ] `jresolve-profiles-ie` is untouched. Its domain vocabulary is deliberate and this rule does not bind it.

## A judgement this task owes
The objection to this rule change was that an end-to-end test should read like
a consumer's code, and neutral fixtures read less like one.

Report in the commit body whether the renamed tests still document what a
consumer does, or whether something real was lost. If something was lost, say
what — that is evidence the rule should have exempted `endtoend/` after all,
and it belongs in `PLAN.md` rather than being absorbed silently.

## Out of scope
- Changing what the tests assert, how many there are, or which resolver paths they cover.
- Touching `src/main`, which is already clean under this grep.
- Touching `jresolve-profiles-ie`.
- Adding a build-time enforcement of the grep. Worth doing, but it is its own task with its own failure modes.
