# 04 — Add the core value types

**Repo:** .
**Depends on:** 01
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/evidence/**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/result/**
- jresolve-core/src/test/java/io/github/aindriub/jresolve/evidence/**
- jresolve-core/src/test/java/io/github/aindriub/jresolve/result/**

## Goal
Define the immutable vocabulary every later layer reads and writes: the open
`ComparisonCategory`, the evidence types, and the result types carrying a score
with its scale. These are pure data with no behaviour beyond derivation, and
they freeze the shape tasks 05, 06 and 07 build against.

## Context
- docs/design-decisions.md#d3 — `ComparisonCategory` is an interned value object, not an enum, because it is the key of every scoring table.
- docs/design-decisions.md#d4 — `MISSING_ONE` and `MISSING_BOTH` are separate categories; short-circuited evidence is flagged incomplete.
- docs/design-decisions.md#d6 — `Score` carries `ScoreScale`; margin is defined on that scale and `hasSecondBest()` replaces a sentinel.
- docs/design-decisions.md#d10 — `FieldContribution` carries a template key, never a rendered sentence.
- docs/design-decisions.md#d12 — `matched` is derived from the decision; only `List<FieldContribution>` survives as the contribution shape.
- docs/design-decisions.md#d5 — `getFrequencyKey()` returns the agreed normalized value when the comparison agreed, null otherwise.
- docs/spec/original-design.md §19, §29, §30, §52, §54, §55, §56, §57 — the original shapes these amend. Read those sections only.
- docs/conventions.md#errors — no exception message or `toString()` contains a field value.

## Acceptance
- [ ] Package `evidence` contains `ComparisonCategory`, `FieldEvidence`, `DefaultFieldEvidence`, `MatchEvidence`. Package `result` contains `Score`, `ScoreScale`, `Decision`, `MatchResult`, `ScoredCandidate`, `FieldContribution`.
- [ ] `ComparisonCategory.of(String)` interns by name: `of("EXACT")` is asserted `isSameAs(ComparisonCategory.EXACT)`. `equals`/`hashCode` are consistent with the name, and `of` rejects null, empty and whitespace-only names.
- [ ] The constants `EXACT`, `ALIAS_TRANSLATION`, `ALIAS_NICKNAME`, `ALIAS_VARIANT`, `VERY_HIGH`, `HIGH`, `MEDIUM`, `LOW`, `CONFLICT`, `MISSING_ONE`, `MISSING_BOTH` all exist.
- [ ] The intern table is a `ConcurrentHashMap` of names only; a class comment states that this is the one permitted static and that no field value ever enters it. A test interns the same new name from two threads and asserts a single identity results.
- [ ] `FieldEvidence` exposes `getCategory()`, a nullable `Double getSimilarity()`, and `getFrequencyKey()` returning null when the comparison did not agree. `DefaultFieldEvidence` is immutable and rejects a null category.
- [ ] `MatchEvidence` copies its map defensively into an unmodifiable `LinkedHashMap`, preserves insertion order in `getFields()`, returns null from `getField(name)` for an absent field, and exposes `isComplete()` set at construction.
- [ ] `ScoreScale` has exactly `LOG2_LIKELIHOOD_RATIO`, `PROBABILITY`, `POINTS`.
- [ ] `Score` holds `double value`, a non-null `ScoreScale`, a `String algorithm` identifier and a nullable `Double probability`; construction rejects a null scale and a non-null probability outside `[0,1]`. Javadoc states the probability is null unless the model is calibrated.
- [ ] `FieldContribution` holds field name, `ComparisonCategory`, `double contribution` and `String templateKey` — no rendered sentence and no prepared value. A test asserts the type exposes no value-bearing accessor.
- [ ] `ScoredCandidate<C>` holds the candidate, its `Score` and an ordered unmodifiable `List<FieldContribution>`.
- [ ] `MatchResult<C>` holds decision, match, score, second-best score and the ranked candidate list; `isMatch()` is `decision == MATCH`; `getMatch()` returns null for every other decision and says so in Javadoc; `hasSecondBest()` exists and `getMargin()` documents it is only meaningful when that is true. A test asserts `getMatch()` is null for `REVIEW` and for `NO_MATCH`.
- [ ] Every `toString()` here emits field names, categories, scales and numbers only; a test builds evidence carrying a frequency key and asserts the key does not appear in the `toString()` output.
- [ ] All types are final with final fields, no setters, and defensively copy every collection argument.

## Out of scope
- `FieldDefinition`, `FieldPipeline`, `FieldComparator` — task 05.
- `MatchScorer`, `DecisionThresholds`, any scoring or decision logic — task 06.
- The `diagnostics(true)` opt-in from D10 and any explanation rendering.
- Fellegi-Sunter model types, `TermFrequencyTable`, `AliasRepository`.

## Attempt 1 - failed

Tester PASS (48 tests; defensive copies, interning and the privacy boundary all
verified by probe rather than assertion), reviewer CHANGES. Two defects, both in
`MatchResult`, both about states the type permits rather than states a
well-behaved caller reaches - which is why probing did not find them and reading
did.

**Defect 1 - must fix.** `MatchResult.java:42` -
`new MatchResult<>(Decision.NO_MATCH, candidate, ...)` builds a result where
`isMatch()` is false while `getMatch()` returns non-null, contradicting the
getter's own Javadoc. A consumer trusting that Javadoc uses a candidate it must
not use. Enforce the invariant in the constructor. D12 removed the redundant
`matched` field precisely so this state could not disagree with itself; leaving
the rule as documentation reinstates the same disagreement one indirection away,
and task 06 is about to become the caller that can violate it.

**Defect 2 - must fix.** `MatchResult.java:104` - `getMargin()` guards on
`secondBestScore` but dereferences `score`, so a result constructed with a
second-best score and no best score throws `NullPointerException` instead of the
documented `IllegalStateException`. Closing defect 1 in the constructor may make
this unreachable; the guard should still be correct rather than accidentally
unreachable.

**Settled - no change.** Throwing from `getMargin()` when there is no second best
is right, and the orchestrator's doubt about it was wrong. `Double` or
`OptionalDouble` reintroduces exactly the boxing-then-arithmetic risk D6 objected
to, docs/conventions.md warns against `Optional` in the core API, and task 06
checks `hasSecondBest()` once per result rather than per call.

**Settled - keep, and add a test.** Excluding `C` from every `toString()` because
a consumer type's own `toString()` could leak a field value is the correct
reading of the privacy boundary, applied consistently across both types that hold
a `C`. But no test asserts it, so a later edit can undo it silently - the
protection currently rests on a comment. Add the assertion the tester wrote as a
probe: a candidate whose `toString()` returns a sentinel, asserted absent from
`MatchResult.toString()` and `ScoredCandidate.toString()`.

**Suggestions.**
- `ComparisonCategory.java:53` - `trim()` validates but the untrimmed name is the
  intern key, so `of(" HIGH ")` mints a category distinct from `HIGH`. Since
  categories key every scoring model, two categories differing by whitespace is a
  silent scoring bug.
- `MatchEvidence.java:31` - null map values are accepted and print as "null" in
  `toString()`, so `getField` cannot distinguish absent from present-but-null.

## Attempt 2 - failed

Tester PASS (53 tests; 24-combination construction matrix, zero NPEs, privacy
sentinel tests confirmed non-vacuous), reviewer CHANGES. Both enumerated defects
were addressed, but the first was closed in one direction only.

**Defect 1 - must fix.** `MatchResult.java:39` rejects `REVIEW`/`NO_MATCH`
carrying a candidate, but `new MatchResult<>(Decision.MATCH, null, score, null,
list)` is still accepted. `isMatch()` then returns true while `getMatch()`
returns null, and a consumer following the documented `isMatch()` then
`getMatch()` path gets an NPE. This is the same self-disagreeing state D12
removed the `matched` field to prevent, and enforcing only the `match != null`
half implies the null half was considered legal. The tester's matrix states it as
data: 8 of 24 combinations are rejected, and all 8 are `match != null` with a
non-MATCH decision.

Note why the tester reported zero NPEs and was still right - `getMatch()`
returning null does not throw. The NPE lands in the consumer that trusted the
contract, which is not reachable from a probe of these types alone.

**Defect 2 - must fix.** `new MatchResult<>(NO_MATCH, null, null, secondBest,
list)` is still constructible: a runner-up with no leader. `hasSecondBest()`
returns true, so `getMargin()` throws `IllegalStateException` while its own
`@throws` at line 99 says it throws only when `hasSecondBest()` is false, and the
message misstates the cause. Attempt 2's guard stopped the NPE without rejecting
the illegal state where it is created. Reject `secondBestScore != null && score
== null` in the constructor and leave line 102 as the defensive backstop it
already reads as. The tester independently noticed this state and filed it as
"logically odd, not in scope"; the reviewer is right that it is a defect.

**Also.** `MatchResult.java:22-24` - the constructor Javadoc still says the
candidate "should" be null unless the decision is MATCH. It is now enforced, so
say "must", and state the converse once the sibling case above is closed.

**Settled, do not undo.** `getMargin()` throws rather than returning a sentinel
or an Optional. `C` stays excluded from every `toString()`, and the sentinel
tests protecting that are confirmed non-vacuous - adding the candidate back to
either `toString()` fails them. The `ComparisonCategory` trim fix is complete and
verified by reflection into the intern map: exactly one `HIGH` key, no
whitespace-variant duplicate. `MatchEvidence` null-value rejection works without
conflating a rejected null value with an absent field.

**For attempt 3, state the legal set explicitly.** Do not fix the two cases named
above one at a time. Write down which of the 24 combinations of decision, match,
score and second-best score are legal, enforce exactly that set in the
constructor, and let the tester's matrix confirm accepted equals legal. Fixing
enumerated instances is what produced two rounds of siblings.

## Attempt 3 - passed

Tester PASS (58 tests; 24-combination matrix confirms accepted equals legal in
both directions), reviewer APPROVE. Commit `974b26c`: `MatchResult`'s
constructor now enforces the legal set stated as data rather than as two
patched cases. Seven of twenty-four combinations of (decision, match, score,
secondBestScore) are accepted:

- `MATCH` requires a non-null `match` and a non-null `score` (1 combination).
- `REVIEW` requires a null `match` and a non-null `score` (1 combination) — a
  review is always a scored candidate that is ambiguous; there is no
  reviewing nothing.
- `NO_MATCH` requires a null `match` and permits `score` null or non-null (2
  combinations) — it is the only decision reachable with no candidates
  evaluated, hence the only one where a null score is legal.
- Each of the above forbids `secondBestScore` non-null when `score` is null
  (the remaining 3 legal combinations layer `secondBestScore` onto the cases
  above that already carry a `score`).

Constructor Javadoc changed from "should" to "must" for the candidate, per
attempt 2's note. `getMargin()`'s `@throws` for the null-score case is now
correct by construction rather than defensive: that arm is unreachable.

This closes both defects named across attempts 1 and 2 as one rule instead of
two patches, and is the reason neither produced a third sibling. Merged to
`main` in wave 2's close-out. The legal set is recorded for later tasks in
`docs/design-decisions.md#d12` (amendment) and `docs/architecture.md` under
"The type model".
