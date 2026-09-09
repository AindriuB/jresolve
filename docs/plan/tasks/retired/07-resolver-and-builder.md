# 07 — Add the resolver and its builder

**Repo:** .
**Depends on:** 04, 05, 06
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/api/**
- jresolve-core/src/test/java/io/github/aindriub/jresolve/api/**

## Goal
Wire the layers together: `DefaultEntityResolver` prepares the source once per
`resolve()` call, compares fields in ascending cost order, runs hard
`CandidateRule` vetoes between tiers, and hands scored candidates to the
decision engine. `EntityResolverBuilder` validates the configuration at
`build()` so nothing that can be caught at construction fails at `resolve()`.

## Context
- docs/design-decisions.md#d2 — the source is prepared once per `resolve()`, not once per candidate; that is the shape, not a cache.
- docs/design-decisions.md#d4 — compare in ascending cost, evaluate `CandidateRule`s between tiers, and flag short-circuited evidence `isComplete() == false`.
- docs/design-decisions.md#d12 — `CandidateRule` runs between cost tiers, before scoring, as a veto, and the builder exposes it as `.rule(...)`.
- docs/design-decisions.md#d6 — `build()` fails when the thresholds' scale does not match the scorer's.
- docs/spec/original-design.md §9, §38, §59, §75, §96, §98, §105 — the resolver interface, hard rules, the builder shape, error handling, determinism, the validation list, retention. Read those sections only.
- docs/conventions.md#errors — configuration errors throw `EntityResolutionConfigurationException` from `build()` with a message naming the field and the constraint, never the data.
- docs/conventions.md#immutability-and-threads — everything reachable from a built resolver is immutable and safe for concurrent `resolve()`.

## Note carried from wave 3 (task 05) — D1 symmetric-case sugar

D1 says the symmetric `field(name, sourceGetter, candidateGetter, pipeline)`
overload is sugar over the asymmetric one. Task 05 confirmed `field/` exposes
enough to build it with no change needed on that side:

```
field(name, S::get, C::get, pipeline)
  == field(name,
           s -> pipeline.prepare(S::get.apply(s)),
           c -> pipeline.prepare(C::get.apply(c)),
           pipeline::compare)
```

That is: wrap each extractor with `pipeline::prepare` to get the two
`Function<_, N>` preparers, and pass `pipeline::compare` (a `FieldPipeline`
already implements the `FieldComparator<N>` shape needed here) as the
`FieldComparator<N>`. This is the concrete expression to drop into the
symmetric overload's implementation — recorded here so it does not need
rediscovering. It does not change or add to the acceptance criteria below.

## Acceptance
- [ ] `EntityResolver<S, C>` declares `MatchResult<C> resolve(S source, Collection<C> candidates)`.
- [ ] `CandidateRule<S, C>` declares `RuleDecision evaluate(S source, C candidate, MatchEvidence evidence)`; `RuleDecision` distinguishes rejection from continuation and nothing else.
- [ ] `DefaultEntityResolver` prepares each source field exactly once per `resolve()` call: a test with counting preparers over 50 candidates asserts one source-side invocation per field and one candidate-side invocation per field per candidate.
- [ ] Fields are grouped into ascending cost tiers and compared tier by tier; after each tier every configured `CandidateRule` runs against the evidence gathered so far.
- [ ] A rejecting rule short-circuits the remaining tiers for that candidate: a test with a counting preparer on an `EXPENSIVE` field asserts that preparer is never invoked for the rejected candidate.
- [ ] Evidence produced by a short-circuit has `isComplete() == false`; evidence for a candidate that reached the last tier has `isComplete() == true`.
- [ ] A vetoed candidate and a candidate whose `ScoringResult` is unscorable are both excluded from the ranked list handed to the decision engine, and a test asserts a resolve where every candidate is vetoed returns `NO_MATCH` with a null match.
- [ ] `resolve()` retains no reference to the source or to any candidate after it returns, other than the candidates present in the returned `MatchResult`; the resolver holds no mutable field.
- [ ] Concurrency: a test runs `resolve()` from at least four threads on one built resolver with distinct sources and asserts each result equals the single-threaded result.
- [ ] `EntityResolverBuilder` offers a symmetric `field(name, sourceGetter, candidateGetter, pipeline)` composing each extractor with `prepare`, and an asymmetric overload taking two extractors and two prepare functions; plus `cost(...)`/`required(...)` per field, `rule(...)`, `scorer(...)`, `decisionEngine(...)` and `build()`.
- [ ] `build()` throws `EntityResolutionConfigurationException` for each of: no fields configured, duplicate field name, null extractor, null pipeline or comparator, null scorer, null decision engine, null thresholds, and thresholds whose `ScoreScale` differs from `scorer.scale()`. One test per case, asserting the message names the field or the constraint.
- [ ] A test asserts no exception message from `build()` contains a value taken from a source or candidate object.
- [ ] `EntityResolutionConfigurationException` extends `RuntimeException` and is in this package.
- [ ] A null source, a null candidate collection and a null element inside the collection are each handled without a `NullPointerException` escaping, with the documented behaviour asserted.
- [ ] No type, member or Javadoc word in this package names a person, name, address, date of birth or country.

## Out of scope
- `CandidateProvider`, `CandidateIndex`, `BlockKey`, blocking of any kind, and the `resolve(S source)` overload that would need one.
- The `diagnostics(true)` builder flag from D10.
- Soft `MatchRule`s and `RuleResult`.
- Any edit inside `field/`, `scoring/`, `decision/`, `evidence/` or `result/` — tasks 04, 05 and 06 own those files. If a signature there is wrong, report it rather than editing.
- The end-to-end fixtures and scenario test — task 08.

## Attempt 1 - failed

Tester PASS (250 tests; D2 proven at the extractor level - 1 source-side call
against 37 candidate-side; all ten of §98's invalid configurations rejected at
`build()`; short-circuit mutation caught by three named tests; determinism held
across shuffled orders). Reviewer CHANGES. Three defects, all the same shape: a
guard that does not guard.

**Defect 1 - must fix. The D6 scale check is vacuous.** `EntityResolverBuilder`
validates the `DecisionThresholds` passed to `.thresholds(...)` against the
scorer's scale, but that object never reaches the resolver - the engine uses the
thresholds it was constructed with. So this builds cleanly:

    .decisionEngine(new ThresholdDecisionEngine<>(POINTS thresholds))
    .thresholds(PROBABILITY thresholds matching the scorer)

and the engine then interprets POINTS thresholds against probability scores at
runtime. The check inspects an object with no runtime effect.

Note the tester and reviewer do not disagree here. The tester confirmed the
exception genuinely throws from inside `build()` before the resolver is
constructed - true, and worth having. The reviewer asked whether the checked
object controls anything, and it does not. Verifying that a mechanism works is
not the same as verifying it is wired to something. That is the third time this
milestone a correct measurement has supported a broader claim than it earned.

The method is genuinely necessary: `MatchDecisionEngine` exposes neither its
thresholds nor its scale, and `decision/` is outside this task's `Owns`. So the
in-scope fix is a Javadoc contract stating that the instance passed to
`.thresholds(...)` must be the same one the engine holds, and a note reported
upward that `MatchDecisionEngine` should expose its scale so a later milestone
can make the check real rather than contractual. Do not widen `Owns` to fix
`decision/`.

**Defect 2 - must fix.** `.rule(null)` is accepted and then silently discarded in
the resolver constructor, while every other null in this builder fails
`build()`. This is the sibling §98's enumerated list did not name - the same
pattern that cost tasks 03 and 04 an extra round each. Reject it at `build()`
with the other nulls.

**Defect 3 - must fix.** `.required(fieldName)` sets `FieldDefinition.isRequired()`,
which nothing in the library reads - `RuleBasedScorer` carries its own separate
required-field mechanism. A candidate missing a required field on both sides
still scores and can return MATCH. The method promises enforcement it does not
deliver. Either make the resolver honour it, or state plainly in the Javadoc that
it is metadata for a scorer to consult and name which scorer mechanism actually
enforces it. Say which you chose and why.

**Settled - do not change.** Two API doubts raised by the orchestrator were ruled
against, with reasons, and should not be revisited:

- `.cost(fieldName, cost)` and `.required(fieldName)` taking string names is
  consistent with §61. Extraction stays type-safe through method references; the
  logical field name is model configuration, which §61 explicitly keeps
  string-keyed, and both methods validate unknown names at `build()`.
- Silently skipping a null candidate element is acceptable because it is
  documented at `EntityResolver.java:21-22` and asserted by a test, making it a
  stated deterministic behaviour rather than an omission. §76 requires null
  behaviour be documented, not that it throw.

Also settled and correct: `DefaultEntityResolver` stays package-private and is
never returned by name; the D1 sugar matches the task-file note exactly; the
`isComplete()` flag is derived from field count rather than reporting absent
fields as `MISSING_BOTH`.

**Suggestions - take unless you disagree.**
- `DefaultEntityResolverTest.java:104` - the 50-candidate D2 test configures a
  single field, so "one preparation per field per source" is only exercised at
  n=1. Add a second field.
- `DefaultEntityResolverTest.java:276` - the eight "distinct sources" in the
  concurrency test are value-identical, so a shared-state bug keyed on values
  would not show. Make them differ.

## Attempt 2 - passed

Tester PASS (251 tests; determinism re-probed directly with 20 shuffles of a
four-candidate list against a fixed source, decision and match identical every
time). Reviewer APPROVE. Commit `4e9c7fd` closed defects 2 and 3 in code and
left defect 1 open by design.

**Defect 2 - closed.** `.rule(null)` now fails `build()` alongside the other
nulls, verified by the same one-test-per-case pattern as the rest of §98's list.

**Defect 3 - closed by documentation, not enforcement.** `.required(fieldName)`
is now Javadoc'd as metadata only, naming `RuleBasedScorer.Builder#requiredField`
as the mechanism that actually enforces required fields. The reviewer judged
this correct over enforcing it a second time in the resolver: two enforcement
paths for one concept would diverge in semantics. `FieldDefinition.isRequired()`
remains set and unread — flagged for milestone 2 in `PLAN.md`'s Known gaps.

**Defect 1 - deliberately not closed.** The reviewer checked the obvious fix —
have the builder construct the engine from the thresholds it just validated —
and refuted it: `MatchDecisionEngine` is an interface callers must be able to
supply their own implementation of, `.decisionEngine(...)` is itself an
acceptance criterion, and `ThresholdDecisionEngine.thresholds` is private with
no getter. Closing it properly means changing `decision/`, which this task does
not own, and widening `Owns` was ruled out. What shipped is the Javadoc contract
from attempt 1 (pass the exact same `DecisionThresholds` instance to both the
engine's constructor and `.thresholds(...)`), plus an explicit statement in the
Javadoc of the failure mode and why the library cannot detect it. Both tester
and reviewer independently reproduced the hole (an engine holding PROBABILITY
thresholds, a builder validated against a scale-matching POINTS object,
`resolve()` returning MATCH by comparing a 10.0-point score to a 0.9 probability
threshold) and independently confirmed the documented discipline is sufficient
to avoid it. This is not fully honoured D6: a documented contract is weaker than
a construction-time exception, and the gap is recorded in `PLAN.md`'s Known
gaps for milestone 2 (`MatchDecisionEngine` should expose its scale).

Merged to `main` in wave 4's close-out. Union build: 251 tests, `BUILD SUCCESS`,
no overlap with the 229-test baseline at wave 3's close.
