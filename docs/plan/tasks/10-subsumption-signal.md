# 10 — Carry a subsumption signal on field evidence

**Repo:** .
**Depends on:** none
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/evidence/TokenSubsumption.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/evidence/FieldEvidence.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/evidence/DefaultFieldEvidence.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/evidence/MatchEvidence.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/evidence/**

## Goal
D9's core claim is that "the source omits detail the candidate has" and "the two
disagree" are different findings, and a symmetric similarity cannot tell them
apart. That needs a signal on the evidence, alongside the category rather than
instead of it. Add it generically — core may not name an address (see
`docs/architecture.md`, "Core contains no domain vocabulary"), so this ships as
token subsumption and task 15 applies it to addresses from the profiles module.

## Context
- docs/design-decisions.md#d9 — the subsumption signal and the §90 `Dublin` / `Dublin 4` / `Dublin 8` case it exists to make legible.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/evidence/FieldEvidence.java — three getters today; this adds a fourth.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/evidence/DefaultFieldEvidence.java — note its `toString` deliberately excludes `frequencyKey`; keep that property.
- docs/architecture.md, "Boundaries that must not be crossed" — no field value in a `toString`.
- docs/plan/PLAN.md, "Known gaps" — `MatchEvidence.toString()`'s `evidence == null` branch is dead code; close it here rather than reopening the review later.

## Decision to implement, not re-litigate
`FieldEvidence` gains `getSubsumption()` as a **Java 8 `default` method**
returning `TokenSubsumption.NOT_APPLICABLE`. Adding an abstract method would
break every existing implementor including consumers'; a default keeps the
interface additive. `TokenSubsumption` is an `enum` — unlike `ComparisonCategory`
(D3) its set is genuinely closed, and nothing keys a scoring model on it.

## Acceptance
- [ ] `TokenSubsumption` is an enum with exactly `NOT_APPLICABLE`, `NEITHER`, `LEFT_SUBSUMES_RIGHT`, `RIGHT_SUBSUMES_LEFT` and `EQUIVALENT`, each with Javadoc stating what it asserts about the two token sets.
- [ ] The Javadoc distinguishes `NOT_APPLICABLE` (this comparator does not compute subsumption) from `NEITHER` (it computed it and neither side subsumes the other). A test asserts the default is `NOT_APPLICABLE`, not `NEITHER`.
- [ ] `FieldEvidence.getSubsumption()` is a `default` method returning `NOT_APPLICABLE`, and its Javadoc says a comparator that does not reason about tokens should leave it alone.
- [ ] An existing implementation of `FieldEvidence` that predates this change still compiles and returns `NOT_APPLICABLE`; a test pins this with an anonymous implementor declaring only the three original getters.
- [ ] `DefaultFieldEvidence` gains a constructor taking a `TokenSubsumption`, rejecting null with the existing message style; the three-argument constructor is retained and delegates with `NOT_APPLICABLE`.
- [ ] `DefaultFieldEvidence.toString()` includes the subsumption and still excludes `frequencyKey`; a test asserts a frequency key value does not appear in the rendered string.
- [ ] `MatchEvidence.toString()`'s `evidence == null` branch is removed, and a test or a documented constructor invariant shows why it is unreachable.
- [ ] No type, member or Javadoc word in this package names a person, name, address, date of birth or country. In particular `TokenSubsumption`'s Javadoc illustrates with neutral token sets, never with a locality.

## Out of scope
- Any comparator that computes subsumption — that is task 14.
- Scoring or decisioning on the signal; no scorer reads it in this milestone.
- `Score.algorithm`'s null guard — task 12 owns `result/`.
