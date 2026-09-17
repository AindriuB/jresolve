# 18 — Carry the subsumption signal into the explanation

**Repo:** .
**Depends on:** none
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/result/FieldContribution.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/RuleBasedScorer.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/result/FieldContributionTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/scoring/RuleBasedScorerTest.java
- jresolve-profiles-ie/src/test/java/io/github/aindriub/jresolve/profiles/ie/endtoend/BeatTheJoinTest.java

## Goal
Milestone 2 gave the library a signal that says *why* two candidates are hard
to separate, and then did not carry it as far as the consumer.

`FieldEvidence.getSubsumption()` records that a source's tokens are contained
in a candidate's. `MatchResult` exposes `ScoredCandidate`, which exposes
`FieldContribution`, which holds a category, a contribution and a template key
(D10) — and not the signal. So a consumer reading the result sees *that* two
candidates tied and cannot see that each contains the source, which is the
entire explanation for the tie.

Task 16 had to assert the direction through the pipeline instead of through
the result, and said so where it asserts it. That is the gap to close.

## Context
- docs/design-decisions.md#d9 — what the signal means and the §90 case it exists to make legible.
- docs/design-decisions.md#d10 — explanations are value-free by construction: a category, a contribution and a template key, never a rendered sentence containing values. Whatever is added here must keep that property.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/result/FieldContribution.java — four fields today.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/RuleBasedScorer.java:78-85 — the loop that builds a `FieldContribution` from each `FieldEvidence`; the evidence is in hand here and the signal is dropped.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/evidence/TokenSubsumption.java — the enum, including why `NOT_APPLICABLE` and `NEITHER` differ.
- jresolve-profiles-ie/src/test/java/io/github/aindriub/jresolve/profiles/ie/endtoend/BeatTheJoinTest.java — `bothCandidatesSubsumeTheSourceInTheSameDirection` documents the gap and is where the fix gets proven end to end.

## Acceptance
- [ ] `FieldContribution` carries the `TokenSubsumption` from the evidence it was built from, defaulting to `NOT_APPLICABLE`, with the existing four-argument constructor retained so no caller breaks.
- [ ] The constructor rejects a null subsumption, in the existing message style.
- [ ] `RuleBasedScorer` populates it from the evidence rather than leaving the default; a test asserts a contribution built from subsuming evidence reports the direction, and one built from a comparator that does not reason about tokens reports `NOT_APPLICABLE`.
- [ ] `FieldContribution.toString()` includes the subsumption and contains no field value — D10's property is not weakened by this addition, and a test asserts a frequency-key-shaped value does not appear.
- [ ] `BeatTheJoinTest.bothCandidatesSubsumeTheSourceInTheSameDirection` is rewritten to assert the direction **through `MatchResult`**, not through the pipeline. Its comment explaining why the result could not carry it is replaced by what the result now says.
- [ ] A test asserts the §90 case end to end through the result alone: two candidates, equal scores, and both contributions reporting the same non-`NEITHER` direction — so a consumer can distinguish "tied because both contain the source" from "tied by coincidence".
- [ ] `mvn verify` passes with no change to any assertion outside this task's `Owns`.
- [ ] No type, member or Javadoc word in the core files names a person, name, address, date of birth or country.

## A question this task should answer, not assume
Whether the signal belongs on `FieldContribution` at all, or whether
`ScoredCandidate` should expose the `MatchEvidence` it was scored from.

The second is more general — it would carry similarity and frequency key too,
and stop this same gap recurring for the next signal added. It is also a
larger API surface, and D10's argument is that an explanation should be a
narrow, value-free projection rather than the whole evidence object, precisely
so that values do not leak into whatever a consumer logs.

Take the narrow option unless the wider one turns out to be needed, and record
the reasoning in the commit body. If the wider option is chosen, it changes
this task's `Owns` and must be raised before implementing.

## Out of scope
- Rendering explanations. Template keys stay keys; a consumer renders.
- Any change to how subsumption is computed — tasks 14 and 15 own that.
- Exposing the raw `FieldEvidence` from `MatchResult`, unless the question above is answered that way and the change is raised first.
