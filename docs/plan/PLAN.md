# Plan

What is open, in priority order. Closed milestones keep a short summary here
and their detail in `HISTORY.md` — scan `HISTORY-INDEX.md`, never load
`HISTORY.md` whole.

## Milestone 1 — complete

The core engine skeleton and its first end-to-end path. Eight tasks
(01–08), four waves: build skeleton, similarity metrics, normalization
primitives, core value types, field layer, scoring and decision, resolver and
builder, end-to-end test. Closed at 262 tests, `BUILD SUCCESS`, commit
`9bd38c4`. See `HISTORY-INDEX.md` for the per-wave entries.

Its closing verdict was that the pieces compose but resolution quality was
unproven: every positive outcome was carried by exact surname and exact date
of birth, the two columns a plain SQL join would match on. D7 and D9 were
named as the gap. **Milestone 2 answers that** — see below.

## Milestone 2 — complete

Eight tasks (09–16), four waves, closing D7 (alias equivalence groups) and
D9 (address subsumption). Closed at 469 tests (393 core + 76 profiles),
`BUILD SUCCESS`, commit `5f8d0e1`.

- [x] **09 — `TokenSplitter` production implementation.** Flagged unimplemented
  in three consecutive waves; `DefaultTokenSplitter` closes it.
- [x] **10 — Subsumption signal on evidence.** `TokenSubsumption` plus a Java 8
  `default` method keeping `FieldEvidence` additive.
- [x] **11 — Alias equivalence groups.** `AliasRepository` and
  `DefaultAliasRepository`, closed transitively at construction.
- [x] **12 — Field-layer extension points opened.** So a comparator can be
  written outside `field/` at all.
- [x] **13 — Alias-aware comparator.** Three arms: equal, alias, delegate.
- [x] **14 — Token subsumption comparator.** Containment is never `CONFLICT`.
- [x] **15 — Irish profiles.** First real content in `jresolve-profiles-ie`.
- [x] **16 — The falsification test.** The milestone thesis, stated so it can
  fail.

**What shipped.** An alias layer that resolves two renderings of one value
across languages, an address comparator that distinguishes *less specific*
from *contradictory*, and a profiles module that finally exercises D15's
split rather than asserting it.

**What did not ship.** No Fellegi-Sunter, no frequency-adjusted `u`, no
blocking or candidate index, no logistic regression, no phonetics. `D19`'s
alias-corpus provenance question is untouched.

**The verdict (task 16).** Yes — a consumer now gets a better answer than a
join. The assertion that proves it resolves the same records twice, once with
the real pipelines and once with only equality substituted, and asserts
`MATCH` at 85 points against `NO_MATCH` at −10, in one test. The control is a
*generous* join: same normalization, fields, weights and thresholds, equality
only where the real resolver compares — and it is handed an exact address hit
and still cannot match, because the two name fields are correct renderings it
has no way to read. §88's two expected bands, both recorded unmet at
milestone 1, are now reached and asserted by value.

**The lesson, applied rather than relearned.** Milestone 1 recorded four
times that a correct measurement can support a claim broader than it earned,
and that the counter-question for any green suite is "is it wired to
anything". Task 16 passed on first run and was mutation-checked before being
believed: removing D7 and D9 from the resolver under test turns five of
eleven tests red. Milestone 2 also produced three instances of the same
class of error caught *before* merge — an acceptance criterion that was flatly
false (task 14), a Javadoc claim the test disproved (task 12), and an alias
entry whose two spellings normalization already closed (task 15).

## Milestone 3 — consolidation complete, capability not yet planned

Taking the two largest open correctness and explanation gaps before adding
capability. Both landed at 490 tests (412 core + 78 profiles),
`BUILD SUCCESS`.

- [x] **17 — A decision engine declares the thresholds it applies.** Closes
  D6. `build()` now inspects the object that actually decides and rejects
  both a wrong-scale engine and one whose thresholds diverge from the
  declared ones.
- [x] **18 — The explanation carries why two candidates tied.**
  `FieldContribution` carries the subsumption signal, so a consumer reading
  `MatchResult` can tell a genuine tie from a coincidental one.

**D5 followed as milestone 4** — see below.

## Milestone 4 — complete

Six tasks (19–24), four waves, closing D5 and the parts of D11, D16 and D17
that depend on it. Closed at 582 tests (504 core + 78 profiles),
`BUILD SUCCESS`.

- [x] **19 — `TermFrequencyTable`.** Corpus counting with a floor, so an
  unseen key never makes `u` zero.
- [x] **20 — `FellegiSunterModel`.** `m`/`u` tables, optional prior odds,
  composite declaration, declared missingness.
- [x] **21 — `FellegiSunterScorer`.** `Σ log₂(m/u)` on
  `LOG2_LIKELIHOOD_RATIO`, probability only where a prior exists.
- [x] **22 — Training representation and extension points.** Labelled and
  unlabelled examples; `FeatureExtractor` and `ProbabilityModel` unimplemented
  per D17.
- [x] **23 — `docs/calibration.md`.** The file D11 names; four classes cite it.
- [x] **24 — Fellegi-Sunter end to end.** The probabilistic path through a
  whole resolver.

**The verdict (task 24).** The probabilistic path gives a consumer one thing
the rule-based one cannot, and it is not the probability: **agreement on a
common value scores lower than agreement on a rare one.** The probability is
arithmetic on numbers nobody measured. The scale is a real but narrower gain —
a margin on `LOG2_LIKELIHOOD_RATIO` means the same thing at every score
level, which a points margin does not. **Nothing shipped is calibrated.**

**What the end-to-end test was worth.** It caught a live defect before merge:
a corpus covering one field inflated every *other* field to roughly 19 bits,
because the table's floor for an uncovered field is its rarest answer. The
ordering claim still held under the defect, so a test asserting only "rare
beats common" would have passed and shipped it — the hand-derived absolute
value is what failed.

## Milestone 5 — complete except for a gate no code closes

Five items, none of them new capability: every one is the consequence of a
decision taken after milestone 4. Both waves are complete at 621 tests
(543 core + 78 profiles), `BUILD SUCCESS`. Only task 28 remains, and no
amount of code closes it.

### Wave 1 — complete

- [x] **26 — The composite combination rule is selectable per group.**
  `SMALLEST | AVERAGE | STRONGEST` on each declared group; `composite(fields)`
  still means `SMALLEST`. Additive by the `default` method pattern of tasks 10
  and 17.
- [x] **27 — Core's test fixtures lost their domain vocabulary.** The
  documented grep returns nothing over `jresolve-core/src`. D1's asymmetry
  between the two fixture types was preserved deliberately.
- [x] **29 — The alias strength tiers are reordered.** `ALIAS_VARIANT` >
  `ALIAS_TRANSLATION` > `ALIAS_NICKNAME`, per D7. Two derived-pair expectations
  moved; every declared edge held; `BeatTheJoinTest` green before and after.

### Wave 2 — complete

- [x] **25 — Explainable rejection.** `MatchResult.getRejectedCandidates()`
  carries each vetoed candidate with the rule that vetoed it, so a consumer can
  tell one that scored badly from one that was never scored. Additive by
  copy-with: the resolver attaches rejections *after* the engine has decided,
  which makes it structurally impossible for a veto to reach the decision.
  Rules are identified by configured position rather than name — forced by
  measurement, since every `CandidateRule` is a lambda and a lambda's class
  name is not stable across builds.

### No wave — a gate, not a task

- [ ] **28 — A sourced alias corpus.** Not code: it needs a licensed source
  for the Irish/English and nickname tables. D19 makes this a release gate, so
  it blocks publication and nothing else. Deliberately carries no task file.

**What wave 1 was worth.** Two of the three tasks found something their own
contract had not anticipated, and both findings are about the checks rather
than the code — see the gaps below. The third, task 29, closed a coverage gap
that had made a whole test class blind to the behaviour it existed to pin.

## Milestone 6 — complete

Three tasks (30–32) closing the five gaps milestone 5 wave 1 raised. 602 tests
(524 core + 78 profiles), `BUILD SUCCESS`.

- [x] **30 — The domain-vocabulary rule is enforced by the build.**
  `DomainVocabularyTest` reads core's own sources and fails naming file and
  line, on `ModuleBoundaryTest`'s model. Takes three of the five gaps together,
  because they were one rule's problem.
- [x] **31 — A stale cross-file reference fails the build.** doclint at
  `verify`, `show=private`, over test sources as well as main, scoped
  `all,-missing`.
- [x] **32 — Core pins its own behaviour.** No tests added: the audit found
  nothing unpinned, which is the result rather than the absence of one.

**The verdict.** The thesis was that every rule this repository states about
itself either fails the build when violated or says in its own text that it
cannot. That now holds for the domain-vocabulary rule, which is the one that
had been quietly violated since task 01 — it fails the build, and
`docs/conventions.md` lists what it cannot catch. It does not hold generally,
and the honest position is that two of the five gaps are narrower or
differently shaped than when they were raised rather than gone.

**What writing the gates was worth, separately from the gates.** Each of the
three tasks found something by trying to prove its own work rather than by
running it. Task 30's first checker missed every accessor, because requiring a
word boundary means `getLastName` never matches — the same hole the hand-run
grep had, which is part of why the rule went unenforced. Task 31's first proof
was a false positive: the planted stale link contained "Person", so task 30's
gate failed the build and doclint was never reached. Task 32's answer was that
there was nothing to do. None of the three would have surfaced from a green
build.

## Notes for implementers

- `jresolve-core` needs a JDK 17 toolchain (`~/.m2/toolchains.xml`, not part
  of the repo) — see `docs/architecture.md#building`. In Claude Code on the
  web this is provisioned automatically by
  `.claude/hooks/session-start.sh`; on a local machine it is still manual.
- `mvn clean verify` baseline at task 25's close is 621 tests
  (543 core + 78 profiles), `BUILD SUCCESS`.
- Two gates now fail the build on things a reviewer used to catch by eye:
  `DomainVocabularyTest` on domain vocabulary in core, and javadoc doclint on a
  reference that no longer resolves. Both are described where the rules they
  enforce are written, not here.

## Known gaps (non-blocking, no task owns these)

Whoever next touches these files should close them in passing rather than
reopen the review. Closed items are not listed; see `HISTORY.md`.

### Raised in milestone 2

- **D19's alias-corpus provenance gates any release.** Decided after milestone
  4: `IrishNameAliases` ships illustrative, hand-written tables, and nothing
  publishes until a corpus with a compatible licence replaces them. Not a code
  defect and not fixable by code — it needs a source. Task 28 owns it.
- **`IrishAddressComparator` cannot emit `PARTIAL_OVERLAP` or `CONFLICT`** —
  it bands those two cases instead. Task 16's scorer configures weights for
  them anyway so a later change surfaces as a wrong score rather than a
  silent zero, but they are dead configuration today.
- **`IrishNamePipeline.forGivenName()` is used for surnames too.** One
  repository carries both, so the behaviour is right and the name is
  narrower than the use. Rename or add a surname-shaped factory when
  something else touches that file.

### Raised in milestone 3

- **The explanation projection is narrow by choice, so each new signal needs
  its own carrying.** Task 18 put the subsumption on `FieldContribution`
  rather than exposing `MatchEvidence` from `ScoredCandidate`, because the
  evidence carries a frequency key — a prepared value D10 keeps out of
  anything a consumer logs. The cost is that the next signal worth explaining
  needs the same deliberate step; it will not arrive for free. That is the
  intended trade, recorded so it is not mistaken for an oversight. The
  accessor guard in `FieldContributionTest` states the criterion any addition
  must meet.

### Raised in milestone 4

- **`FellegiSunterScorer`'s unscorable path is unreachable through the
  resolver, and that is now the intended design.** A cost-tier veto drops the
  candidate at `DefaultEntityResolver:101`. D4 was amended to say so, because
  `RuleBasedScorer` never inspects `isComplete()` — routing partial evidence to
  it would let a vetoed candidate score above `matchThreshold` and make a hard
  veto overridable. The scorer's refusal stays unit-tested and correct for a
  consumer assembling evidence directly. What dropping costs — a vetoed
  candidate being invisible to a consumer — is task 25, not this entry.
- **The library ships no calibrated model, and `docs/calibration.md` says so.**
  That is the honest position, not a gap to close by inventing defaults — but
  it does mean a consumer cannot get a trustworthy probability out of the box,
  and anyone planning a release should know that is by design.

### Raised in milestone 5 wave 1, resolved by milestone 6

Three closed, one narrowed, one corrected. Milestone 6 took all five; it did
not close all five, and the two below say so rather than being ticked.

- [closed] **The collision with rule 6, the incomplete token list, and the
  absence of enforcement.** One rule's problem, taken together by task 30.
  `DomainVocabularyTest` reads core's own sources and fails naming file and
  line; `docs/conventions.md` prescribes the synthetic-data wording rather than
  exempting the statement; the token list now catches the camelCase forms the
  hand-run grep missed.

- **Narrowed, not closed: nothing checks a claim one file makes about
  another.** Task 31's doclint gate fails the build on a `{@link}` to something
  that no longer exists, at `show=private` and over test sources as well as
  main. But the defect that raised this gap named no type — it was prose
  asserting what a neighbouring file's fixtures looked like, and doclint is
  indifferent to prose. The mechanically checkable subset is closed; **the gap
  itself remains open** and is the same class as milestone 1's invisible
  character: correct code, a comment that lies.

- **Corrected: the risk is combinations, not modules.** The gap said a core
  behaviour exercised only through a profile is one core does not pin. Task 32
  tested that directly — mutate a core behaviour, run core's tests alone — and
  core caught all four probes, while all three `default` methods turned out to
  be pinned already. What task 29 actually hit was narrower and harder: a
  **combination**, a translation edge merged with a nickname edge, a pair of
  categories no core test put together. Every individual behaviour was covered
  and none of that coverage said anything about the pair. Combinations grow
  faster than anyone writes tests, and no single-line mutation probe finds a
  missing one. Unsolved, and now correctly stated.

### Raised by task 25

- **`MatchResult` now holds two parallel lists that must not overlap, and
  nothing in the type prevents it.** A scored candidate and a rejected one are
  mutually exclusive by construction in `DefaultEntityResolver`, and the
  invariant is asserted by a test rather than made unrepresentable. Two lists
  is manageable; a third would be the point to stop and reshape the type. Named
  now so that a later reader finds a recorded trade rather than an oversight.
- **A rule is identified by position, which is stable but not descriptive.**
  `resolver.rule.2` tells a consumer which of their own rules vetoed a
  candidate, and nothing about why. A `default` accessor on `CandidateRule`
  would allow a name, and is additive whenever someone wants one — it was not
  added now because every rule in this library is a lambda, and a lambda cannot
  override a default method, so the accessor would today serve nobody.

### Raised in milestone 6

- **A planning measurement was wrong, and only implementation caught it.**
  Milestone 6 was planned on a reading that core emitted zero javadoc
  warnings; that run had no doclint configured, so it measured the lenient
  default rather than the gate. The real figure under `doclint=all` is 100.
  The plan was amended in the task file with its reason rather than quietly
  corrected — but the general point stands: a measurement taken to size a task
  should exercise the thing the task will turn on, not its neighbour.
- **100 javadoc `missing` warnings sit unaddressed by decision, not
  oversight.** `no @param`, `no @return`, `no comment`. Adopting doclint's
  `missing` group would be a documentation-completeness policy this project
  has never chosen, and task 31 deliberately did not choose it on the
  project's behalf. Worth deciding once; the gate is configured `all,-missing`
  until somebody does.
- **`DomainVocabularyTest` cannot police its own file.** It holds known-bad
  strings as fixtures, so it skips itself. Obfuscating the samples would test
  an obfuscation rather than the rule. The hole is one file wide and named in
  the test's own Javadoc.
- **`getDOB` escapes the vocabulary checker.** `dob` is the one token still
  needing a word boundary, so its capitalised camelCase form passes. Recorded
  in `docs/conventions.md` alongside the other limits rather than fixed,
  because every fix here trades one false negative for a false positive
  somewhere else.

### Answered after milestone 4

Both standing questions here were decided, and the decisions are recorded where
the rules themselves live rather than only here.

- **Rule 7 admits a design artefact.** `CLAUDE.md` rule 7 now reads: only
  `implementer` writes code; `scribe` writes the planning record; a doc whose
  content is a design output and which code cites — `docs/calibration.md` — is
  written by the role that owns the design. Task 23's exception is a rule now,
  so the next such artefact does not re-litigate it.
- **The domain-vocabulary rule binds tests too.** `docs/conventions.md` now says
  the grep takes no exceptions anywhere in `jresolve-core`, `src/test`
  included, and that domain-shaped fixtures belong in `jresolve-profiles-ie`.
  Core's `endtoend/` package does not comply yet — that is task 27, and the
  gap is dated rather than silent.

### Carried forward from milestone 1

- `JaroWinklerSimilarity` (task 02): the `maxPrefixLength` and
  `boostThreshold` constructor guards have no automated test — deleting
  either survives the suite.
- `UnicodeFormNormalizerTest:28` (task 03): feeds a precomposed literal to
  the NFD test. It passes today but proves nothing under an NFD-normalizing
  editor pass.
- `ScoredCandidate` (task 04) permits a null candidate. One such candidate
  scored above `matchThreshold` makes `ThresholdDecisionEngine` throw from
  `MatchResult`'s constructor rather than return a result.
- `@SafeVarargs` on a reifiable `Object[]...` in `ThresholdDecisionEngineTest`
  (task 06) is redundant.
- **D6's residual hole** (narrowed by task 17, not fully closed). `build()`
  now inspects an engine that declares its thresholds and rejects a wrong
  scale or a divergent configuration. An engine whose
  `declaredThresholds()` returns null still cannot be checked, so for that
  engine alone the caller's discipline is what keeps the configured and
  applied thresholds in agreement. Every engine in this library declares, so
  this only bites a consumer's own implementation. Narrow and named rather
  than open.
- `DefaultEntityResolver`'s constructor has a branch silently skipping null
  rules, dead now that `build()` rejects them before construction.
- `FieldDefinition.isRequired()`, set by `EntityResolverBuilder.required(...)`,
  is read by nothing; `RuleBasedScorer.Builder#requiredField(String)` is the
  mechanism that actually enforces required fields. A later milestone should
  decide whether the flag earns its place.
