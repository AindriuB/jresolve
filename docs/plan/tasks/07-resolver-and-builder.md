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
