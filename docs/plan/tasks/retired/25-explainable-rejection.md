# 25 — Explainable rejection

**Repo:** .
**Depends on:** 27 (reads the fixtures it renames)
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/result/MatchResult.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/result/RejectedCandidate.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/api/DefaultEntityResolver.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/result/RejectedCandidateTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/result/MatchResultTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/api/DefaultEntityResolverTest.java

## Goal
A consumer reading a `MatchResult` can tell a candidate that scored badly from
one that was never scored, and can see which rule vetoed it.

## Context
- docs/design-decisions.md#d4 — a veto drops the candidate, and that is now the intended design rather than a divergence. This task closes what dropping costs; it does **not** reopen the routing question.
- docs/design-decisions.md#d10 — explanations are value-free by construction. A rejected candidate must not carry a prepared value, a frequency key or a field value into anything a consumer logs.
- docs/design-decisions.md#d12 — redundant state was deliberately removed from the result types. Adding state back needs the same discipline: no field derivable from another.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/api/DefaultEntityResolver.java:95-115 — `resolveOne` returns null on a veto at `:101`; that null is where the candidate is lost.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/decision/MatchDecisionEngine.java:25 — `decide(List<ScoredCandidate<C>>)` has no parameter for rejections, so carrying them needs an additive change.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/evidence/FieldEvidence.java — the Java 8 `default` method pattern task 10 used to extend an interface without breaking implementations; task 17 used it again for `declaredThresholds()`. Follow it.

## Acceptance
- [ ] `RejectedCandidate<C>` carries the candidate and a **stable identifier for the rule that vetoed it**, and nothing else derivable from those two.
- [ ] The rule identifier is a template key or equivalent stable token, **not** a rendered message. A test asserts it contains no field value, per D10.
- [ ] `MatchResult` exposes the rejected candidates, never null, empty when nothing was vetoed. A result built by an existing caller that knows nothing about rejections still builds and reports empty.
- [ ] **No existing `MatchDecisionEngine` implementation breaks.** Whatever mechanism carries rejections from the resolver to the result is additive — a `default` method, an overload, or a copy-with on the immutable result. A test compiles and runs an engine that implements only the original `decide(List)` and asserts it still works.
- [ ] `DefaultEntityResolver` collects a vetoed candidate instead of discarding it at `:101`, and the veto still short-circuits the remaining cost tiers — the candidate is recorded, not compared further. A test asserts the expensive tier's comparator is never invoked for a vetoed candidate.
- [ ] A vetoed candidate is **absent from the scored candidates** and present in the rejected ones. A test asserts both halves, so the two lists cannot silently overlap.
- [ ] A veto still cannot influence the decision: a test asserts the `Decision` and `Score` are identical with and without rejections being carried.
- [ ] Rejected candidates are ordered deterministically, and a test asserts the same input yields the same order twice.
- [ ] No type, member or Javadoc word in these files names a person, name, address, date of birth or country; fixtures are neutral tokens (`docs/conventions.md`).

## A judgement this task owes
`MatchResult` was deliberately narrowed by D12, and task 18 recorded that the
explanation projection is narrow *by choice* — each new signal needs its own
carrying, and that cost is the intended trade rather than an oversight.

This task adds a signal. Report in the commit body whether the narrow
projection still looks right after adding a second one, or whether two
deliberate carryings is the point where exposing a richer object would have
been cheaper. Either answer is useful; the second is a finding about D12.

## Out of scope
- Changing when a veto happens, or routing partial evidence to a scorer. D4 is settled.
- Reporting *why* a scored candidate fell below a threshold — that is scoring, and `FieldContribution` already carries it.
- Any change to `scoring/`, `alias/` or `field/`.
