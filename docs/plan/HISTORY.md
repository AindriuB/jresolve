# History

Append-only, newest first. See `docs/plan/HISTORY-INDEX.md` for a grep-first
index — do not load this file whole.

## 2026-09-17 — Milestone 5 wave 1: composite rules per group, neutral core fixtures, reordered alias tiers (tasks 26, 27, 29)

Three disjoint tasks, 598 tests (520 core + 78 profiles), `BUILD SUCCESS`.
None of it is new capability — every task implements a decision taken after
milestone 4.

**26 — the composite rule became selectable per group.** `SMALLEST | AVERAGE
| STRONGEST` on each declared group, `composite(fields)` still meaning
`SMALLEST`, additive by the `default` method pattern tasks 10 and 17
established. A test implements `FellegiSunterModel` without overriding the new
method and asserts it still reports `SMALLEST`, which is what keeps the
interface change additive rather than merely claimed to be.

The scorer had to change shape rather than gain a branch. It kept one member
and suppressed the rest, which works whenever the group's weight *is* some
member's weight — true for smallest and strongest, false for an average, whose
mean is nobody's. That exposed a question the task file had not anticipated:
under `AVERAGE` the carrying member reports a number that is not its own
weight, and the ordinary contribution key would claim that field measured
something it did not. It reports `compositeCombined` instead.

**That answers the rule 7 amendment's own question.** Task 26 was the first
task to write a design artefact under the amended rule, and owed a judgement on
whether writing `docs/calibration.md` beside the code beat recording it
afterwards. The `compositeCombined` key is the evidence: the choice only
exists once you have seen that `AVERAGE` breaks the keep-one-member shape. A
scribe recording the decision later would have documented all three rules
correctly and had no reason to notice that one of them makes the explanation
lie. The amendment earned its place on its first use.

**27 — core's fixtures went neutral.** `ExternalPerson` → `IncomingRecord`,
`Owner` → `StoredRecord`, and the members with them. The documented grep
returns nothing over `jresolve-core/src`. D1's asymmetry was preserved
deliberately — four members with a `List<String>` against five with a `String`
— because a rename that made the two types symmetric would have destroyed what
the suite proves. No assertion value moved, checked by diffing assertion
literals rather than by eye.

Its judgement: little was lost, but only because the loss was already paid for
elsewhere. The renamed tests still document the mechanics; what goes is the
motivation, and `BeatTheJoinTest` in `jresolve-profiles-ie` carries that. Had
that test not existed, this rename would have cost something real — which is
the argument for the rule, not against it.

**29 — the alias tiers were reordered.** One production line at
`DefaultAliasRepository:61`; everything else was tests. Exactly two
expectations moved, both derived pairs spanning a translation edge and a
nickname edge: `Pádraig`/`Paddy` through Patrick and `Liam`/`Will` through
William. Every declared edge kept its declared kind. `BeatTheJoinTest` was run
before and after — 13 tests, green both times, category assertions untouched —
because Seán/John and Ó Súilleabháin/O'Sullivan are declared translation pairs
rather than derived ones. Milestone 2's thesis does not move.

Its judgement: task 15's implementer first wrote `Pádraig`/`Paddy` as a
nickname and was corrected by the old ordering; the reorder puts their original
expectation back. One data point rather than a pattern, but it is the only
evidence for the new order that no argument produced — and the objection
recorded in D7 still stands unrefuted.

**The coverage gap task 29 found is the most reusable lesson.** Core's
`DefaultAliasRepositoryTest` mixed variant with translation and never
translation with nickname, so all 31 of its tests stayed green under a reorder
of exactly those two tiers. The behaviour was pinned only in
`jresolve-profiles-ie`. Three tests added, and reverting the production line
now turns two of them red. A core behaviour exercised only through a profile is
a core behaviour core does not pin.

**Both new suites were mutation-checked rather than trusted for passing first
time**, per the discipline milestone 1 established: forcing `compositeRuleFor`
to return `SMALLEST` turns four of task 26's tests red, and reverting the tier
order turns two of task 29's. In each case one new test stays green by design,
and the commit says which and why rather than claiming the whole suite catches
the regression.

**Two findings about the checks rather than the code**, both from task 27 and
both now in `PLAN.md`: the vocabulary rule collides with rule 6's required
synthetic-data statement, and `dateOfBirth` escapes the documented grep
entirely because "dob" is not a word inside it. The grep is necessary and not
sufficient, and nothing enforces it at build time.

**Two things were deliberately left for this pass rather than done in a task.**
`PLAN.md` still named `ExternalPerson` and `Owner` in task 27's own
description, and the three task files were unretired. Both sit outside every
task's `Owns`, so rule 2 kept them out of the tasks that noticed them.

## 2026-09-17 — Eight standing questions decided; D19 closes and milestone 5 is named (decisions)

No code changed. Every question this project had left to a maintainer was put
and answered, and each answer is recorded where the rule itself lives rather
than only here.

**D19 closes entirely.** Maven coordinates confirmed as
`io.github.aindriub:jresolve-*`, with publication to Maven Central intended —
settled while the cost of changing it is still zero. Phonetics do not ship in
v1: D8's low-yield reasoning stands, and the dependency route would reopen a
boundary `docs/architecture.md` argues is a boundary rather than a preference.
Alias-corpus provenance becomes a **release gate** — nothing publishes until a
licensed source replaces the hand-written tables. That is the only one of the
three that blocks anything, and it blocks publication rather than development.

**D4 was wrong and the code was right.** D4 said a `CandidateRule` veto yields
evidence flagged `isComplete() == false` and hands it to the scorer, which
refuses it. The resolver has always dropped the candidate at `:101`. The
decision amends D4, and the reason is checkable rather than aesthetic:
`RuleBasedScorer:72` refuses only on a missing *required* field and never
inspects `isComplete()`, so routing a vetoed candidate's partial evidence to it
would let the tiers that did run score above `matchThreshold` — making a hard
veto overridable by a score. Dropping keeps a veto final. What dropping costs is
that a vetoed candidate is invisible in `MatchResult`; that is an explanation
gap, now task 25, rather than an argument for changing the path.

**The alias strength ordering is swapped to `ALIAS_VARIANT` >
`ALIAS_TRANSLATION` > `ALIAS_NICKNAME`**, reversing what the code shipped with.
Decided twice: confirmed as-is first, then reopened and swapped. The case for is
that a translation is a name-identity mapping across languages where a nickname
is many-to-one and optional. The case against was put by this session and
overruled — Irish anglicisation is often arbitrary rather than semantic
(Siobhán→Judith, →Julia), which would make a translation edge the loosest of
the three. Both arguments are in D7 so the objection is not rediscovered from
scratch. This is the only decision in the pass that carries a code change: task
29, one line at `DefaultAliasRepository:61` and seven test files behind it,
`BeatTheJoinTest` among them.

**The illustrative alias accessor is not renamed.** Proposed alongside the
release gate and declined: the Javadoc already marks the tables illustrative in
its first paragraph, and task 28 replaces them outright, so a rename now is a
rename twice.

**The composite combination rule becomes selectable per group.**
`composite(fields, rule)` takes `SMALLEST | AVERAGE | STRONGEST` on each
declared group; `composite(fields)` still means `SMALLEST`, so nothing already
shipped changes. A single global setting was taken first and reversed the same
day: correlation strength is a property of the fields in a group, not of the
model that holds them, so a model with one tightly-coupled group and one barely
coupled group cannot express both with a single answer. The caveat
is in D11 and belongs in `calibration.md` — `u` is measurable and `m` is
estimable, but nothing tells a consumer how to choose a combination rule, and a
knob without a procedure is one somebody turns until the score looks better.

**Both maintainer questions are rules now.** Rule 7 admits a design artefact: a
doc whose content is a design output and which code cites is written by the role
that owns the design, so task 23's exception became a rule and the next such
file does not re-litigate it. The domain-vocabulary rule binds `src/test` as
well as `src/main`, with no exceptions to the grep; domain-shaped fixtures
belong in `jresolve-profiles-ie`, where the vocabulary is the point.

**Amending the rule exposed that it had never been checkable.** It banned the
bare token `name`, and `src/main` carries about a hundred uses of it —
`FieldDefinition.getName()`, `fieldName`, `requiredFieldNames` — because core
cannot describe a field without naming it. So the grep the rule called itself
checkable by had been failing on production code all along, and the ~50 figure
`PLAN.md` had carried for the test fixtures was an estimate nobody had run. The
rule now bans `address`, `person`, `dob` and `irish` outright and `name` only in
its personal sense (`firstName`, `lastName`, `surname`), and `conventions.md`
carries the exact command.

**One deliberate inconsistency, dated rather than silent.** Under that grep
`src/main` is clean and core's tests carry **112 hits across exactly four
files**, all in `endtoend/`: `EndToEndResolutionTest` (88), `ExternalPerson`
(12), `Owner` (11), `FellegiSunterResolutionTest` (1). Two are type names, so
task 27 renames the fixture types as well as their members. Until it lands the
rule outruns the code, and `PLAN.md` says so in those words.

**Milestone 5 is named, not planned.** Tasks 25–28: explainable rejection, the
selectable composite rule, the fixture rename, and the sourced corpus. No task
files exist yet and nothing is started.

## 2026-09-17 — Milestone 4 complete: Fellegi-Sunter ships, and its end-to-end test earns its keep (tasks 23, 24)

`FellegiSunterResolutionTest` drives the probabilistic path through a whole
resolver from a consumer's seat, and `docs/calibration.md` lands — the file
D11 names, which four classes cite.

**The verdict task 24 owed.** The probabilistic path gives a consumer one
thing the rule-based one cannot, and it is *not* the probability: agreement on
a common value scores lower than agreement on a rare one, shown as an outcome
rather than a number by `aCommonAgreementCanFallBelowTheMatchThreshold` — the
same shape of agreement that MATCHes on a rare value fails to match on a
near-universal one, with the no-corpus control beside it so the claim is
attributable. The probability is not a second win; it is arithmetic on numbers
nobody here measured. What the scale buys is real but narrower: a margin on
`LOG2_LIKELIHOOD_RATIO` is the log ratio of two candidates' likelihoods, so it
means the same thing at every score level, which a points margin does not.
Nothing shipped is calibrated and the milestone does not claim otherwise.

**The defect the end-to-end test caught, which is what it was for.**
Configuring a corpus for one field inflated every other field enormously:
`tier` scored 18.9 bits instead of 1.0. The model adjusted `u` for every field
carrying a frequency key, and `TermFrequencyTable` answers its *floor* for a
field it never saw — and the floor is its **rarest** answer, so an uncovered
field produced the largest possible weight. Task 19's own Javadoc had called
that "a thin answer rather than a programming error"; it is the loudest answer
available.

The part worth remembering: **the ordering claim still held under the
defect.** Rare still outscored common. A test asserting only the ordering
would have passed and shipped it. The hand-derived absolute value is what
failed — which is the concrete payoff of this project's rule that expectations
are derived by hand rather than read off a run.

Fixed with the maintainer's agreement: `TermFrequencyTable.covers(field)`, and
the model adjusts only where the corpus actually covers the field. Regression
tests sit in the two owning tasks' files, with a control asserting a covered
field is still adjusted.

**A second structural finding.** The resolver never hands a scorer incomplete
evidence — `DefaultEntityResolver:110` always builds it `complete = true`, and
a rule's `REJECT` returns null at `:101`. So `FellegiSunterScorer`'s
unscorable path is unreachable end to end; the real consumer of
`MatchEvidence.isComplete()` is `CandidateRule`, which sees partial evidence
between tiers at `:102`. Both are now asserted and the criterion was amended
in the task file rather than quietly dropped.

Task 17's D6 guard ran on a second scale for the first time: until this
scorer existed the library had one scale, so that check was guarding a case
that could not arise.

**On rule 7.** Task 23 wrote a doc, which `CLAUDE.md` reserves to `scribe`.
The exception is noted rather than passed over silently: D11 names
`docs/calibration.md` as a required design artefact and four classes cite it,
so it is content decided by whoever understands the model rather than a record
of work done. That is an argument; the rule is the maintainer's to amend.

`mvn clean verify`: 504 core + 78 profiles = 582 tests, `BUILD SUCCESS`.

## 2026-09-17 — The Fellegi-Sunter model and scorer land (tasks 20, 21)

`DefaultFellegiSunterModel` carries the `m`/`u` tables, the optional prior and
the composite declaration; `FellegiSunterScorer` sums `log2(m/u)` on the
`LOG2_LIKELIHOOD_RATIO` scale.

**Nothing bent.** Task 21 owed a judgement on whether `MatchScorer`,
`ScoringResult` or `FieldContribution` had to change to accommodate a
probabilistic scorer. None did. That is evidence the shape was right rather
than a non-finding: this is the first *second* implementation of
`MatchScorer`. Two parts earned their keep — `ScoringResult`'s
scorable/unscorable split was built for D4's partial-evidence case in task 06
and had one user until now, and `FieldContribution` took task 18's
five-argument constructor unchanged, so the subsumption signal reaches a
consumer through this scorer without anyone having planned it.

One place the existing contract was **better than assumed**: `getScore()` on
an unscorable result throws rather than returning null, so there is no number
to pick up by accident. The implementer's test asserted null; the assertion
was wrong and the design was right.

**Two design decisions, made and flagged.** Missingness is declared, never
inferred (§45): every category is either configured or explicitly ignored, and
one that is neither throws rather than scoring zero — a silent neutral is the
same guess §45 forbids wearing a different hat. And a composite group
contributes the **smallest** weight among its present members, not the sum,
average or strongest: D11 names overconfidence as the failure mode, so when
the scorer cannot know how much signal is shared, the group should claim no
more than its least favourable member.

Prior odds distinguish "nobody said" from "the odds are even", so a scorer
cannot publish a posterior nobody stood behind.

**An acceptance criterion was wrong and was amended.** Task 20 asked for a
clamp on the frequency-adjusted `u`. There is none: `TermFrequencyTable`
already returns a value within `[floor, 1]` with `floor > 0`, so the range
holds by construction, and a clamp would be the defect this register records
against task 07's D6 check — a guard that fires correctly while protecting
nothing. Dead instance state in the model (a `configuredFields` set copied but
never read) was also removed.

Every expected weight is hand-derived from probabilities chosen so the
arithmetic is exact: `0.8/0.1` is exactly +3.0, `0.5/0.25` exactly +1.0,
`0.1/0.8` exactly −3.0. The frequency test states the consequence plainly —
agreement on a value nine records in ten share scores **negative**, because
`m < u` there.

## 2026-09-17 — Wave 1 of D5: frequency table, training representation, extension points (tasks 19, 22)

`TermFrequencyTable` counts a corpus one observation at a time and reports a
key's relative frequency within a field. The **floor** is the point rather
than a detail: an unseen key returns it, never zero, because a corpus is a
sample and a zero would make `u` zero and `log2(m/u)` infinite.

Its Javadoc states why `ExactFieldComparator`'s
`toString`-consistent-with-`equals` requirement exists — it exists for this
table. Where it does not hold, two occurrences of one value produce two keys
counted once each and every value looks rare.

`LabelledMatchExample` and `UnlabelledMatchExample` replace §93's single
`MatchTrainingExample`. The unlabelled form is the substantive half: `m` is
normally estimated by EM over unlabelled pairs, so a labelled-only
representation hands a consumer the shape that fits the method they are least
likely to be able to use. Both are named away from the specification's name
deliberately — with two types, neither can be "the" training example — and a
Javadoc line records the rename.

`FeatureExtractor` and `ProbabilityModel` ship with **no implementation in
main sources**, which is the intent rather than an omission: D17 holds every
model family behind v1 while the interfaces ship so a future model does not
force changes to `FieldDefinition`, `FieldPipeline` or `MatchEvidence`. A test
composes trivial stand-ins through the whole path to show the extension point
is usable rather than merely declared.

## 2026-09-17 — The explanation carries why two candidates tied (task 18)

Milestone 2 gave the library a signal saying *why* two candidates are hard to
separate and then dropped it before the consumer. `FieldContribution` held a
category, a contribution and a template key; `RuleBasedScorer` had the
evidence in hand and threw the subsumption away. So a consumer reading
`MatchResult` saw *that* two candidates tied and could not see that each
contains the source — the entire explanation for the tie. Task 16 had to
reach past the result into the pipeline to assert it.

**The design question the task refused to assume.** Put the signal on
`FieldContribution`, or expose the whole `MatchEvidence` from
`ScoredCandidate`? The wider option is more general and would stop this gap
recurring for the next signal. It is also the opposite of what D10 asks for:
`FieldEvidence` carries a frequency key, which is a *prepared value*, so
exposing the evidence object would route a compared value into the result a
consumer logs. Narrow, and the reasoning is recorded in the type's Javadoc so
the next person does not re-open it.

**An existing guard caught the addition, correctly.**
`FieldContributionTest.exposesNoValueBearingAccessor` asserts this type's
public accessors are exactly the four D10 allows, and it went red.
`TokenSubsumption` is a five-constant enum describing a relation between
token sets and cannot hold a compared value, so it clears the bar that
`MatchEvidence` would not — but the guard was an allowlist of names, which
invites appending one to make a red test green. It now states the criterion
(*a member may join only if its type cannot carry a compared value*) in its
Javadoc and names it in the failure message.

Mutation-checked: making the scorer drop the signal again breaks the core
unit test and both end-to-end assertions. The `NOT_APPLICABLE` test correctly
survives, since dropping the signal makes everything not-applicable.

**A near-miss worth recording.** The first mutation run appeared to show only
*one* test catching the regression, which would have meant the end-to-end
assertions were vacuous. It was the reactor halting at core before the
profiles module ran. Re-running with failures ignored showed all three. The
weaker reading was the plausible one, and believing it would have shipped a
false claim about coverage.

`mvn clean verify`: 412 core + 78 profiles = 490 tests, `BUILD SUCCESS`.

## 2026-09-17 — D6 closed: a decision engine declares what it applies (task 17)

The largest open correctness gap, carried unfixed through two milestones.

`EntityResolverBuilder.build()` validated the `DecisionThresholds` passed to
`.thresholds(...)` against the scorer's scale, but that object never reached
the engine that decides. `MatchDecisionEngine` declared one method and
`ThresholdDecisionEngine` held its thresholds privately, so `build()` could
not inspect the object doing the deciding. What shipped was a Javadoc
contract saying the two must agree — sufficient *when followed*, which is
exactly the property a construction-time check exists to stop depending on.

**Exposing the engine's `ScoreScale` is the obvious fix and is not enough**,
and the existing characterization test proves it: it diverges two instances
that are both `POINTS`, one declaring a match threshold of 200 to `build()`
and the other applying 50 in the engine. A scale-only check passes that
configuration happily. D6's own note anticipated this in a parenthesis —
"expose its scale (and ideally its thresholds)" — and the parenthesis was the
load-bearing half. So the interface exposes `declaredThresholds()`, a Java 8
`default` returning null for "does not declare", following task 10's pattern;
`MatchScorer.scale()` was already the precedent for an interface exposing
what it operates on.

`build()` now rejects an engine on the wrong scale *and* an engine whose
thresholds differ from the declared ones. `DecisionThresholds` gains `equals`
and `hashCode` so the comparison is by value:
`EntityResolverBuilderTest.validBuilder()` already passed two separately
constructed instances, so identity comparison would have broken existing
callers for no gain. An engine declaring nothing still builds, with a test
holding that open — narrowing what is buildable only where something can
actually be verified.

**The characterization test asserted the bug, so closing the hole broke it.**
Its own Javadoc had anticipated the moment: "this test's assertion of
`MATCH` should then fail ... that failure is the signal the D6 gap has
closed, and this test should be updated at that point, not before." That is
what happened, and it was updated as instructed — the same configuration, now
asserted to be rejected, plus the wrong-scale case D6 reproduced and the
equal-but-separate case that must keep working. Task 17 owning that file was
identified while planning rather than discovered mid-implementation.

Mutation-checked: making `build()` blind to the engine turns exactly the five
rejection tests red and nothing else. One of the implementer's own test
expectations was wrong and corrected before commit — it hardcoded a threshold
of 50 against a scorer yielding 10, so the resolve came back `NO_MATCH`. The
D6 check had passed; the arithmetic had not.

`mvn clean verify`: 405 core + 76 profiles = 481 tests, `BUILD SUCCESS`.

## 2026-09-17 — Milestone 2 complete: the library beats a join (task 16)

`jresolve-profiles-ie` gains an `endtoend/` package whose `BeatTheJoinTest`
answers the charge task 08's reviewer closed milestone 1 with. The thesis is
stated as a falsifiable claim rather than demonstrated: one test resolves the
same records twice — once with the real pipelines, once with only equality
substituted for the two comparators — and asserts `MATCH` at 85 points against
`NO_MATCH` at −10. Both halves are in one test so the contrast cannot rot on
one side while the other keeps passing.

The control is deliberately generous: same normalization, same fields, same
weights, same thresholds, equality only where the real resolver compares. A
real SQL join would not fold a fada or a curly apostrophe for free. It is also
handed an exact address hit and still cannot match, because the two name
fields are correct renderings it has no way to read.

**§88's two expected bands are now reached**, asserted by value. Milestone 1
recorded both as unmet: it produced `LOW` where §88 expects an alias and
`MEDIUM` where it expects `VERY_HIGH`. A second test bands the identical
prepared pair through the generic comparator milestone 1 used and asserts the
two differ, so the improvement is attributed rather than assumed. §90 resolves
to `REVIEW` with both candidates at exactly 110 and a margin of zero, address
category `SUBSUMED` rather than `CONFLICT`.

**The suite passed on first run, so it was checked rather than trusted.**
Removing D7 and D9 from the resolver under test turned five of eleven tests
red, including the falsification test; the six that survived drive the
pipelines directly or are negative cases, which is correct. This is the
milestone-1 lesson applied deliberately for once instead of after the fact.

Three wave-5 defects closed in the milestone-1 suite: the garbled sentence at
`EndToEndResolutionTest.java:67-69` (it is the two *fuzzy* fields that net
+10, not the two exact ones), the tie test raised from two candidates to four
in two orders, and contribution values pinned rather than categories alone.
Its class Javadoc now records that it keeps the generic comparators
deliberately — core cannot depend on profiles-ie — and points a reader at the
profiles suite.

**Gap found, not closed:** `MatchResult` does not carry the subsumption
signal. `FieldContribution` holds a category, a contribution and a template
key (D10), so a consumer reading only the result sees *that* two candidates
tied but not that each contains the source — which is the entire explanation
for the tie. The direction is asserted through the pipeline instead, with the
reason written where it is asserted. Closing it means touching `result/`,
which no task in this milestone owned.

`mvn clean verify`: 393 core + 76 profiles = 469 tests, `BUILD SUCCESS`,
commit `5f8d0e1`.

## 2026-09-17 — Irish profiles land, and the module split stops being an assertion (task 15)

`jresolve-profiles-ie` gains its first real content, having been a
`package-info.java` and a POM since task 01: `IrishNameNormalizer`,
`IrishAddressNormalizer`, `IrishNameAliases`, `IrishAddressComparator`,
`IrishAddressPipeline` and `IrishNamePipeline`, with 65 tests. D15's split is
now exercised rather than asserted — everything the module publishes is
expressed in core's types, and core cannot reference it because the reactor
would cycle.

The two normalizers differ deliberately. The name normalizer removes
apostrophes so `O'Sullivan` folds to one token and does **not** try to reach
`Ó Súilleabháin`; a test asserts those two stay apart, and a change making
that test fail has almost certainly made the normalizer too aggressive rather
than fixed anything. That pair is closed in the alias table instead, which is
D8's whole point. The address normalizer canonicalises by splitting and
rejoining with `DefaultTokenSplitter` itself, so there is exactly one rule
about what separates tokens.

`IrishAddressComparator` runs containment first and spelling second, and is
task 12's first out-of-module use of the opened extension points: it extends
the now-public `AbstractNullSafeFieldComparator` and applies
`SimilarityBands.categoryFor` rather than restating either. A test constructs
it with stricter bands and asserts the same pair bands differently — a
comparator that reimplemented the banding with its own constants passes every
other test in that file and fails that one.

**Three defects found while building it, two of them the implementer's own.**
The alias table declared `Sinéad`/`Sineád` as a variant pair; both normalize
to `sinead`, so the group collapsed to one distinct member and
`DefaultAliasRepository` rejected it at build time. That rejection is correct
and is now a documented guard: a pair normalization already closes does not
belong in an alias table. Separately, the two address shapes did not converge
on a prepared value — one kept a comma — while the Javadoc claimed they did;
comparison was unaffected because the tokeniser drops punctuation, which is
exactly why it would have gone unnoticed, but a prepared value is also what a
blocking key is derived from and the form a cached candidate is held in.
Third, a test expected `Pádraig`/`Paddy` to be a nickname; it is a derived
pair reached through `Patrick`, so the weakest link on the path gives a
translation — design right, expectation wrong.

The tables are illustrative and say so in their first Javadoc paragraph.
**D19's provenance question is untouched and still open**; a release must
replace them.

## 2026-09-17 — Alias and subsumption comparators land (tasks 13, 14)

`field/` gains `AliasAwareFieldComparator` and `TokenSubsumptionComparator`.

The alias comparator tries three arms in order — equal, repository-related,
delegate — and consults the delegate only on the third, asserted with a
recording delegate that must show zero invocations on the other two. The test
that carries the point wires the delegate to return `CONFLICT` and compares
two values sharing not one character; the comparator returns
`ALIAS_TRANSLATION` and never asks it. An alias hit carries neither similarity
nor frequency key: a relation read from a table is not a measurement, and the
two sides did not agree on a value.

The subsumption comparator mints two categories rather than reusing the bands
— `SUBSUMED` for containment either way, `PARTIAL_OVERLAP` for sharing tokens
without containment — which D3 explicitly supports. Strict containment is
deliberately not `CONFLICT`. Both directions share one category and differ
only in the signal, because a scoring model keys on the category and being
less specific is equally informative whichever side is shorter. The vacuous
case is refused: the empty set is a subset of everything, but reporting that
as containment would manufacture agreement out of a value that says nothing.

**A correction to the task's own acceptance.** It stated flatly that the
comparator never returns `NOT_APPLICABLE`. It does, for a null combination,
and correctly — nothing was computed there — while the test only covered
non-null pairs, so it would have passed with the stated claim false. The
criterion and the test name are now scoped to two present values.

`FieldComparatorNullSafetyTest` was assigned to task 14 rather than both:
its list is a manual tripwire, not a package scan, so two tasks editing it
would have been the clash `Owns` exists to prevent. Both comparators are
registered and the count raised to four.

## 2026-09-17 — Wave 1: splitter, subsumption signal, alias repository, extension points (tasks 09, 10, 11, 12)

Four dependency-free tasks, the foundation the rest of the milestone builds on.

**09** gives `TokenSplitter` a production implementation after three waves of
being flagged as a test lambda only. `DefaultTokenSplitter` splits on runs of
anything that is neither a letter nor a digit, so one value yields the same
tokens however its parts were punctuated. Iteration is by code point, pinned
with U+1D400, so a letter outside the BMP is not split across its surrogate
pair. `TokenSimilarityTest` was re-pointed at it only after confirming every
value in that class is whitespace-separated and alphanumeric, so no assertion
changed.

**10** adds `TokenSubsumption` and `FieldEvidence.getSubsumption()` as a Java 8
`default` method, keeping the interface additive — pinned by a test
implementing only the three original getters. `NOT_APPLICABLE` and `NEITHER`
are deliberately distinct: no containment computed versus computed and not
held. `MatchEvidence.toString()`'s dead `evidence == null` branch is removed,
with a comment saying why there is no check.

**11** lands `AliasRepository` and `DefaultAliasRepository`, closing groups
transitively at construction. The design decision worth knowing: **the alias
kind is stored per pair, not per group.** Collapsing a merged group to one
kind would destroy exactly what D7 wants kept. A declared pair keeps its
declared kind; a derived pair gets the strongest available path, where a path
is only as strong as its weakest link — a maximum-bottleneck path computed by
unioning edges strongest-tier first, with strength running `ALIAS_VARIANT`,
`ALIAS_NICKNAME`, `ALIAS_TRANSLATION`. The builder accepts only those three
categories, because admitting a new kind means deciding where it sits in that
ordering.

**12** opens the field layer so a comparator can be written outside it:
`AbstractNullSafeFieldComparator` public with a `protected` hook,
`SimilarityBands.categoryFor` public, `Score` rejecting a null algorithm. The
proof is a new `io.github.aindriub.jresolve.extension` test package — every
other test of these types lives inside `field/`, where package-private access
makes them look usable whether or not they are.

**Two corrections in 12, both to claims rather than code.**
`FieldComparatorNullSafetyTest`'s comment said reflection would silently stop
covering a new comparator; it is the reverse, and the `hasSize` assertion is a
tripwire on the list rather than a package scan. And `ExactFieldComparator`'s
new Javadoc first cited "an inherited `Object.toString()`" as the shape that
breaks the frequency key — writing the test disproved it, since the inherited
rendering derives from `hashCode`, which any type honouring the `equals`
contract overrides. The failing shapes are a `toString` rendering per-instance
state, or an `equals` override without a `hashCode` override.

**Planning amendments, recorded in the task files.** 12's `Owns` was
insufficient as written: making `compareNonNull` protected forces
`SimilarityFieldComparator` to widen its override, and no path was listed for
the required out-of-package test. 11 specified
`EntityResolutionConfigurationException`, which lives in `api/` and is thrown
only by `EntityResolverBuilder`; every other constructor check in core's lower
layers uses `IllegalArgumentException`, and throwing the former from `alias/`
would add a dependency edge up to the top layer.

## 2026-09-17 — A SessionStart hook provisions the JDK 17 toolchain for web sessions (kit)

`mvn verify` could not run in a fresh Claude Code on the web container: the
image ships JDK 21 only, and D14 pins compilation to a JDK 17 toolchain
declared in `~/.m2/toolchains.xml`, which is deliberately not part of the
repo. The build failed at `maven-toolchains-plugin` with "No toolchain found
for type jdk" before reaching a single test.

`.claude/hooks/session-start.sh` installs the JDK, generates
`toolchains.xml` and warms the empty `~/.m2/repository`. It reads the required
version from the pom's `toolchain.jdk.version` rather than restating it, and
is a no-op unless `CLAUDE_CODE_REMOTE=true`, so local machines keep their own
setup. Two container quirks are handled because both bit first: the apt index
is stale, so the install 404s without an update, and third-party PPAs fail
behind the proxy, so that update's non-zero exit must not kill the hook under
`set -e`. A failing suite does not fail session start — the warm-up exists for
the cache, not the verdict.

`/.claude/` was ignored wholesale, which would have kept the hook out of the
repo, and a hook only runs for future sessions if it is committed. The rule
is now `/.claude/*` with narrow exceptions for `hooks/` and `settings.json`;
`agents/`, `commands/`, `scripts/` and `settings.local.json` stay local as
before.

## 2026-09-09 — Milestone 1 complete: end-to-end resolve proven, gaps recorded (task 08)

`jresolve-core` gains `endtoend/`: an `EndToEndResolutionTest` that builds one
`EntityResolver` from a consumer's seat — synthetic `ExternalPerson`/`Owner`
fixtures, four fields across two cost tiers, `RuleBasedScorer` on the POINTS
scale, `ThresholdDecisionEngine` — and asserts a positive `MATCH`, a negative
`NO_MATCH`, an ambiguous `REVIEW` on a small non-zero margin, a cheap-field
veto short-circuit (asserting the expensive field's preparer was never
invoked for the vetoed candidate), `MISSING_ONE` evidence distinct from
`CONFLICT`, determinism under shuffled candidate order, determinism under
tied scores through the stable sort, and the `.thresholds(...)` same-instance
contract from task 07's known D6 gap. This closes milestone 1: all eight
planned tasks are now merged to `main`. Union build: `mvn clean verify`,
`BUILD SUCCESS`, 262 tests, exit 0, commit `9bd38c4`.

**Cost:** Two attempts. Attempt 1 tested PASS with 260 tests — all three of
its documented gap claims independently recomputed and accurate, the
determinism test genuinely fails when ranking is made order-dependent, both
thresholds-contract tests distinguish compliance from violation — but was
rejected for two documentation defects, not code faults. First, the address
assertion derived MEDIUM as plain arithmetic and never said §88 expects
VERY_HIGH (same omission for firstName against ALIAS); a reader would take
the passing MEDIUM assertion as evidence MEDIUM is correct rather than a
known gap. Second, the class Javadoc said "the other three fields carry it,"
which understates what actually decides the positive scenario: lastName
EXACT (30) plus dateOfBirth EXACT (25) reach 55 against a match threshold of
50 on their own, so the positive scenario would pass as a two-exact-key SQL
join, and the fuzzy fields are not load-bearing.

Attempt 2 fixed both with documentation only. Verified by diff: one file,
+120/−5, and every weight, threshold, band and fixture literal byte-identical
to attempt 1. That mattered — the tempting wrong fix was to reweight until
the fuzzy fields looked load-bearing, producing a green suite that
misrepresents the library. Three mutation checks proved the new assertions
are not decorative: widening firstName's band to MEDIUM and narrowing
address to LOW while holding the total at exactly 65.0 — the change attempt
1 would have passed silently — failed
`positiveScenarioMatchesTheBestScoringCandidate`, so the per-field
`FieldContribution` assertions earn their place; reversing the ranked list
before the stable sort failed `tiedCandidatesKeepTheirInputOrderUnderTheStableSort`
by name; and changing the margin comparison to `margin != 0.0` left the
zero-margin tie test passing while the new non-zero-margin case failed,
proving the two margin tests exercise different branches rather than the
same degenerate path.

**The milestone assessment, from the reviewer.** Does milestone 1 give a
consumer reason to believe the library works? For composition: yes — builder
wiring, cost-tier ordering, the cheap-field veto short-circuit, missing-
versus-conflict evidence, threshold and margin decisioning, and determinism
across shuffled order and under ties are all genuinely exercised end to end.
For resolution quality: no — every positive outcome is carried by exact
surname and exact date of birth, the two columns a plain SQL join would
match on. The fuzzy first name is a penalty of −5 because `Seán`/`John`
scores LOW with no alias repository; the address contributes MEDIUM where
§88 expects VERY_HIGH, because generic Levenshtein on a normalized string is
not an address pipeline. The suite now says this in its own Javadoc rather
than leaving a reader to infer it. The one advance:
`ambiguousScenarioReturnsReviewOnASmallNonZeroMarginBelowTheMinimum` is the
first place a fuzzy field decides anything — an address similarity band is
the sole differentiator between two candidates. It decides ranking, not
whether a match exists. **D7 (alias equivalence groups) and D9 (address
subsumption) are not polish. They are the gap between "the pieces compose"
and "a consumer gets a better answer than a join."** That sentence should
survive into the milestone-2 planning notes.

Left for the next touch, recorded in `PLAN.md`'s Known gaps rather than
fixed here: a garbled sentence at `EndToEndResolutionTest.java:67-69` that
attributes the +10 net to "the two exact-match fields" when it is actually
firstName (−5) and address (+15); the tied-scores determinism test uses only
two candidates where three or four would exercise the stable-sort claim
properly; the positive test pins `FieldContribution` categories but not
values; and the address field's generic string comparator will need
re-pointing at the real address pipeline once D9 exists.

**Milestone 1 close-out.** All eight tasks are merged: Maven build skeleton,
similarity metrics, normalization primitives, core value types, field layer,
scoring and decision, resolver and builder, and this end-to-end proof. What
shipped is a Java 8 entity-resolution library with typed field extraction,
two-phase normalization, Jaro-Winkler/Levenshtein/token similarity, an open
comparison-category model, cost-tiered comparison with rule vetoes,
rule-based scoring on a declared score scale, threshold-and-margin
decisioning, and an explainable ranked result — verified end to end. What
did not ship: no alias repository, no address pipeline, no blocking or
candidate index, no Fellegi-Sunter, no logistic regression, no Irish
profiles — `jresolve-profiles-ie` is still an empty module skeleton.

The recurring verification lesson across this milestone, seen four times: a
correct measurement can support a claim broader than it earned. The `C2 A0`
byte scan that proved the NBSP class clean while reading as proof about
invisible characters generally (task 03); the `getMargin()` probe that could
not reach malformed constructions (task 04); the D6 check that fires
correctly while guarding nothing (task 07); and this task's attempt 1, whose
numbers were right while the record around them was not. The
counter-question to ask of any green test is "is it wired to anything," not
"does it work."

## 2026-09-09 — Resolver and builder land (task 07)

`jresolve-core` gains `api/`: `EntityResolver`, `CandidateRule`/`RuleDecision`,
`EntityResolverBuilder`, and the package-private `DefaultEntityResolver` that
wires 05's `field/`, 06's `scoring/` and `decision/` together — prepare the
source once per `resolve()`, compare fields in ascending cost tiers, run hard
`CandidateRule` vetoes between tiers with short-circuit, hand ranked
candidates to the decision engine. `EntityResolverBuilder.build()` rejects
every configuration error `§98` names, throwing
`EntityResolutionConfigurationException` with a message naming the field or
constraint. Union build on `main`: `mvn clean verify`, `BUILD SUCCESS`, 251
tests, exit 0 — exactly the 229-test baseline plus 22 new, no overlap.

**Cost:** Two attempts. Attempt 1 passed testing (250 tests) and was rejected
on review for three defects that were all the same shape: a guard that does
not guard. A D6 scale check validating an object with no runtime effect;
`.rule(null)` accepted and silently discarded while every other null failed
`build()`; `.required(fieldName)` setting a flag nothing in the library reads.
Attempt 2 closed two of the three in code and left the third open by design.

The D6 hole is real, open, and now documented rather than fixed:
`EntityResolverBuilder` validates the `DecisionThresholds` passed to
`.thresholds(...)` against the scorer's scale, but that object never reaches
the engine that actually decides — the engine decides using whatever
thresholds it was constructed with. A caller who passes one
`DecisionThresholds` instance to `new ThresholdDecisionEngine<>(...)` and a
different, scale-matching instance to `.thresholds(...)` gets a clean
`build()` and silently wrong decisions at `resolve()`. Both the tester and
the reviewer reproduced it concretely: an engine holding PROBABILITY
thresholds (0.9/0.5/0.2) with a scale-matching POINTS object passed to the
builder builds cleanly, and `resolve()` then returns MATCH by comparing a
10.0-point score against a 0.9 probability threshold.

It was not fixed inside `api/` because it cannot be. The reviewer checked the
obvious alternative — build the engine from the thresholds the builder just
validated — and refuted it: `MatchDecisionEngine` is an interface callers
must be able to implement themselves, `.decisionEngine(...)` is itself an
acceptance criterion, and `ThresholdDecisionEngine.thresholds` is private
with no getter. Fixing it means changing `decision/`, which task 07 does not
own, and widening `Owns` to fix it was explicitly ruled out. What shipped
instead is a Javadoc contract on `.thresholds(...)`: pass the exact same
instance used to construct the engine, plus a stated failure mode and why the
library cannot detect a violation. Both agents independently confirmed the
contract is sufficient — following it closes the hole entirely — but a
documented contract is weaker than a construction-time exception, and the
library still ships with a way to be silently wrong that only discipline
prevents. Recorded honestly in `PLAN.md`'s Known gaps for milestone 2:
`MatchDecisionEngine` should expose its scale so the check can inspect the
object that actually decides.

This is the third time this milestone the same failure mode has recurred
across independent probes: a byte scan for `C2 A0` proved the NBSP class
clean but was read as proving the invisible-character class was (task 03); a
probe of `getMargin()` on valid constructions could not reach the malformed
ones it was meant to guard (task 04); here, a tester confirming the D6
exception fires while the reviewer found it guards nothing it is wired to. In
every case the measurement was correct and the sentence describing it was
wider than the measurement. The counter is not "does the mechanism work" but
"is it wired to anything" — worth carrying into every future review round in
this project, not just this milestone's.

## 2026-09-09 — Field layer, scoring and decision land (tasks 05, 06)

`jresolve-core` gains `field/` (task 05) and `scoring/` + `decision/` (task
06). `field/`: `FieldPipeline` (the `prepare`/`compare` split), `FieldDefinition`,
`CostTiers`, and the two comparators the end-to-end test needs —
`ExactFieldComparator` and `SimilarityFieldComparator` with configurable
`SimilarityBands`. `scoring/`: `MatchScorer`, `RuleBasedScorer` (per-field,
per-category weights summing to a `Score` on `ScoreScale.POINTS` with ordered
`FieldContribution`s), `ScoringResult`. `decision/`: `DecisionThresholds` and
`ThresholdDecisionEngine`, ranking scored candidates and turning them into a
`MatchResult` via the legal-state set task 04 defined. Union build on `main`
after both merges: `mvn clean verify`, `BUILD SUCCESS`, 229 tests, exit 0 —
142 baseline + 53 from 05 + 34 from 06, exactly, no overlap. That settles a
discrepancy between task 06's implementer (175 tests) and tester (176) in
its own worktree: `scoring/` (11 + 9) plus `decision/` (5 + 9) sum to 34,
matching the tester's count and the union arithmetic. The implementer's 175
was an undercount by one; nothing in the union total is inconsistent with
34 new tests from 06.

**Cost:** Both tasks passed on the first attempt — the first wave in this
milestone to do so. Both briefs carried forward what the previous six
attempts (waves 1-2) cost: break something on purpose before reporting,
hand-derive expectations rather than asserting a value the code produced, and
state the legal set before enforcing anything. Neither implementer needed a
rejection to apply all three.

Two design claims moved from asserted to measured this wave, not just
implemented:

D2 (the prepare/compare split) is now demonstrated behaviourally, not just
inferred from the signature. Task 05's test built an invocation-counting
normalizer and drove ten candidate comparisons through a pipeline built on
it: `prepare` on the source value ran exactly once, `compare` never touched
the normalizer at all. That probe — count invocations on the object under
test, not on a mock that only records calls it was told to expect — is how
this claim should be re-verified once task 07 wires the resolver and the
same property has to hold across a whole `resolve()` call, not one pipeline.

Task 04's `MatchResult` legal set (7 of 24 combinations, closed in wave 2 by
enumerating the whole state space rather than patching named instances) met
its first real producer this wave. Task 06's reviewer walked every path
`ThresholdDecisionEngine` can take by reading the code; task 06's tester
independently drove eight scenarios through a probe of the built engine —
empty candidate list, a single candidate above/between/below the thresholds,
and the two-candidate match/review/no-match combinations. Neither found a
path that constructs an illegal combination. A constraint written in one
wave held against a producer written in the next, checked twice,
independently, without either side needing to consult the other's method.

Both implementers independently chose an inclusive-boundary operator
(`>=` to `>`) as their mutation-testing target, and in both cases a named
test caught it on the first try. Testers then swept every boundary rather
than trusting the one sampled mutant: three similarity bands in 05
(`VERY_HIGH`/`HIGH`/`MEDIUM` at 0.95/0.85/0.70) and three thresholds in 06
(`matchThreshold`, `reviewThreshold`, `minimumMargin`), each caught by
exactly one named test. This is the second wave running where a
boundary-operator flip has been the sharpest mutation-testing signal in the
codebase; it is where silent misclassification risk concentrates here.

Ownership of two open items got settled rather than left ambiguous:
`Score.algorithm`'s nullability is not task 07's to close (07 can only
reject a null at `build()`, which does not stop direct `Score` construction
elsewhere; the actual fix is a constructor guard in task 04's
`result/Score.java`) — moved from "task 07 should decide" to a milestone-2
item in `docs/plan/PLAN.md`'s Known gaps. D1's symmetric-case builder sugar
is task 07's, and task 05 confirmed `field/` needs no change to support it;
the concrete expression is recorded in
`docs/plan/tasks/07-resolver-and-builder.md` as a note, not an acceptance
criterion.

Five non-blocking items went into Known gaps rather than being fixed outside
their owning task's scope: `ExactFieldComparator`'s frequency key derives
from `toString()`, not `equals()`, which will misattribute D5's (milestone 2)
frequency adjustment for any `N` with value equality but a default
`toString`; `SimilarityBands.categoryFor` is package-private while its class
and getters are public, so an outside comparator can't reuse it and might
reimplement banding with different bounds; `ScoredCandidate` still permits a
null candidate, which would make `ThresholdDecisionEngine` throw from the
wrong package if the (currently nonexistent) producer ever made one;
`TokenSplitter` (flagged in three consecutive waves now, since task 02) still
has no production implementation; and two small test-hygiene notes — a
backwards comment in `FieldComparatorNullSafetyTest`, a redundant
`@SafeVarargs` in `ThresholdDecisionEngineTest`.

## 2026-09-09 — Normalization primitives and core value types land (tasks 03, 04)

`jresolve-core` gains two more packages. `normalization/` (task 03): a
`StringNormalizer` functional interface plus stages for Unicode form, case
folding, combining-mark removal, apostrophe variants, punctuation and
whitespace, composed by an immutable `CompositeNormalizer`. `evidence/` and
`result/` (task 04): `ComparisonCategory` (interned, not an enum),
`FieldEvidence`/`DefaultFieldEvidence`/`MatchEvidence`, and
`Score`/`ScoreScale`/`Decision`/`MatchResult`/`ScoredCandidate`/`FieldContribution`
— the immutable vocabulary tasks 05-07 build against. Union build on `main`
after both merges: `mvn clean verify`, `BUILD SUCCESS`, 142 tests across 21
test classes, exit 0.

**Cost:** Both tasks took three attempts, and both converged the same way —
production code was correct at attempt 1 in each case; every rejection was
about a state that could not fail while the suite stayed green.

03's defect was a non-breaking space written as a raw invisible character one
line above an ordinary space, visually identical to it. Any editor's
trim-whitespace pass would silently rewrite both the production literal and
the test's own literal to a plain space, so `WhitespaceNormalizer` would stop
folding NBSP and the test would keep passing — degrading together, not apart.
Found five times across the wave: twice by review, a third by the implementer
generalising the first two, a fourth (a combining acute, `CC 81`, in
`UnicodeFormNormalizerTest`) by the next review pass after attempt 2's byte
scan for `C2 A0` correctly found zero NBSP instances and incorrectly read as
proof the whole invisible-character class was clean, and closed on attempt 3
by enumerating every non-ASCII code point in both owned trees with a verdict
per entry rather than scanning for one more sequence. That enumeration, and
the rule it now backs in `docs/conventions.md#tests`, is the artifact that
outlives the fix.

04's defect was that `MatchResult` permitted states that contradicted its own
documented contract: `isMatch()` true with `getMatch()` null, or the reverse,
or a second-best score with no best score to be second to. Attempt 2 closed
the named instances one at a time and reopened a sibling each time — reject
`NO_MATCH` carrying a candidate, and `MATCH` with a null match was still
legal; guard `getMargin()`'s NPE, and the `IllegalStateException` it threw
instead still misdescribed its own cause. Attempt 3 stopped patching cases and
wrote down the legal set instead: 7 of 24 combinations of (decision, match,
score, secondBestScore), enforced exactly in the constructor, with a
24-combination matrix test asserting accepted equals legal in both
directions. Recorded as an amendment to `docs/design-decisions.md#d12` and in
`docs/architecture.md` under "The type model" — 05, 06 and 07 all consume
`MatchResult` and need the set, not just the current absence of a bug.

The lesson that generalizes past this wave: fixing the instances a review
names produces another review; fixing the class ends it. A green suite is
evidence about the tests, not the code, until something is broken on purpose
and observed to fail — attempt 3 in both tasks is the only attempt that left
behind an artifact that stays true on its own (the byte enumeration, the
matrix test) rather than one more instance that happened to be found.

Three items raised in review and left open because no task owns the file:
`JaroWinklerSimilarity`'s two constructor guards (from task 02) still have no
automated test; `MatchEvidence.toString()`'s `evidence == null` branch is now
dead code since the constructor rejects null values; and
`UnicodeFormNormalizerTest:28` feeds a precomposed literal to an NFD test,
which passes today but proves nothing under an NFD-normalizing editor pass.
Recorded in `docs/plan/PLAN.md` under "Known gaps" rather than fixed by this
close-out, per the retired task files' scope. `Score.algorithm` (task 04) is
unvalidated and may be null; task 06 is expected to stamp it in practice, and
a note to that effect is in `docs/plan/tasks/06-scoring-and-decision.md`.

## 2026-09-09 — Similarity metrics land (task 02)

`jresolve-core` now has a `comparison/` package: `SimilarityMetric`, and three
implementations — `JaroWinklerSimilarity`, `LevenshteinSimilarity` and
`TokenSimilarity` (a delegate-plus-splitter best-match-mean over tokens). All
three are pure functions of two strings, treat `null` as empty, and are bounded
in `[0,1]` and exactly symmetric, each backed by a property test over a
20-plus-pair synthetic corpus. A `TokenSplitter` functional interface also
landed, used today only by a test lambda.

**Cost:** Took two attempts. Attempt 1 tested PASS — 29 tests, a full mutation
battery, hand-computed values agreeing — and was rejected in review anyway:
`TokenSimilarityTest.weightsBestMatchesBySideTokenCounts` derived its expected
value by calling `JaroWinklerSimilarity` at test time and then asserting the
code against its own output, so it could not detect a regression in the
delegate it was supposed to be exercising. Attempt 2 rewrote it against a
hand-derived expectation — a token pair where Jaro-Winkler is exactly 0.0
because no characters fall within the match window — and proved the fix by
perturbing the delegate and watching the test fail where it had previously
passed. The general lesson recurred in both 03 and 04 the same wave and is
worth restating here: a green suite is evidence about the tests, not about the
code, until something has been broken on purpose and observed to fail.

`TokenSplitter` was not named in the task file but was accepted after a
cross-branch check against 03's concurrent work: `normalization/` is
`String`-to-`String` throughout with no tokenising abstraction to collide
with. Nothing in 02's main sources implements `TokenSplitter` yet — only a
test lambda does — so task 05 is where a second splitter could be invented
independently; noted directly in `docs/plan/tasks/05-field-layer.md` so it
isn't rediscovered.

Two constructor guards added to `JaroWinklerSimilarity` in attempt 2 (prefix
scale bounds) have no automated test covering them — the tester verified them
by hand with a scratch class — so deleting either guard would survive the
suite unnoticed; worth a property or unit test in a later pass. `mvn clean
verify` now runs roughly 9-13 seconds from clean, up from 7-8 at task 01's
close.

## 2026-09-09 — Maven multi-module build skeleton lands (task 01)

The repo now has a real reactor build: a root `pom` with `jresolve-core` and
`jresolve-profiles-ie` modules, a JDK 17 toolchain requirement, `maven.compiler.release=8`,
JUnit 5 + AssertJ on the test classpath via root `dependencyManagement`, and
`animal-sniffer-maven-plugin` checking both modules against `java18` in
`verify`. `jresolve-core` has no compile/runtime dependency; `jresolve-profiles-ie`
depends on it and holds only an empty `io.github.aindriub.jresolve.profiles.ie`
package. `BuildSmokeTest` proves JUnit 5 and AssertJ are wired. `mvn clean
verify` runs green from a clean checkout in roughly 7-8 seconds, which is the
baseline for later tasks — expect it to grow.

**Cost:** Took two attempts. Attempt 1 tested PASS but was rejected in review:
it left animal-sniffer's `animal.sniffer.skip` user property live, so `mvn
verify -Danimal.sniffer.skip=true` silently bypassed the java18 check — the
exact gate this task existed to install. A `-D` on the command line can
override a POM value expressed through the property's own expression, but
cannot override an explicit `<skip>false</skip>` written directly in the
plugin's `<configuration>`; that is the fix, and it is the only way to make
the gate genuinely non-skippable. Attempt 1 also pinned
`<vendor>openjdk</vendor>` on the toolchain requirement, which would fail
against a `toolchains.xml` reading `temurin` or `zulu` (what a second
developer is likely to have installed) — dropped in attempt 2. The diff
between attempts was four lines.

Proving animal-sniffer non-vacuous needed `maven.compiler.release` temporarily
raised from 8 to 11: at `release=8`, javac's own `--release` check rejects a
post-Java-8 API before the sniffer ever runs, so the two gates are redundant
for this class of violation. Both are kept anyway — `release=8` is what
someone loosens to unblock themselves in a hurry, and the sniffer is what
catches them doing it when they do.

Also measured and worth not re-deriving: breaking the `toolchains.xml` version
makes the build fail with "Cannot find matching toolchain definitions" rather
than silently falling back to whichever JDK is running Maven (25 here); and
`mvn -pl jresolve-core dependency:list -DincludeScope=runtime` resolves
nothing, confirming core's JDK-only constraint by measurement rather than
inspection.

The build needs a JDK 17 toolchain that is not part of the repo — Temurin
17.0.20.1 at `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot` on
this machine, with `C:\Users\aindr\.m2\toolchains.xml` pointing at it. Anyone
else building this needs their own `toolchains.xml`; documented in
`docs/architecture.md#building`. Maven 3.8.6 itself runs on JDK 25 — only
compilation is pinned to 17.
