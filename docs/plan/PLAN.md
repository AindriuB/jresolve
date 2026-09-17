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

## Milestone 5 — wave 1 complete, wave 2 open

Five items, none of them new capability: every one is the consequence of a
decision taken after milestone 4. Wave 1 landed at 598 tests (520 core + 78
profiles), `BUILD SUCCESS`.

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

### Wave 2 — open

- [ ] **25 — Explainable rejection.** `MatchResult` carries the candidates a
  `CandidateRule` vetoed and the rule that vetoed each. Today a veto returns
  null from `DefaultEntityResolver:101` and the candidate vanishes, so a
  consumer cannot tell one that scored badly from one that was never scored.
  D4 is explicit that dropping is correct; this closes the explanation gap
  dropping leaves. Its task file is the only one left in `tasks/`.

### No wave — a gate, not a task

- [ ] **28 — A sourced alias corpus.** Not code: it needs a licensed source
  for the Irish/English and nickname tables. D19 makes this a release gate, so
  it blocks publication and nothing else. Deliberately carries no task file.

**What wave 1 was worth.** Two of the three tasks found something their own
contract had not anticipated, and both findings are about the checks rather
than the code — see the gaps below. The third, task 29, closed a coverage gap
that had made a whole test class blind to the behaviour it existed to pin.

## Notes for implementers

- `jresolve-core` needs a JDK 17 toolchain (`~/.m2/toolchains.xml`, not part
  of the repo) — see `docs/architecture.md#building`. In Claude Code on the
  web this is provisioned automatically by
  `.claude/hooks/session-start.sh`; on a local machine it is still manual.
- `mvn clean verify` baseline at milestone 5 wave 1's close is 598 tests
  (520 core + 78 profiles), `BUILD SUCCESS`.

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

### Raised in milestone 5, wave 1

Three of these are about the *checks* rather than the code, which is what wave
1 mostly found.

- **The domain-vocabulary rule collides with rule 6.** `CLAUDE.md` rule 6
  requires a synthetic-data statement on a fixture, and the natural way to
  write one is "none names a real person" — which
  `docs/conventions.md`'s vocabulary rule forbids. Task 27 wrote "none
  describes anyone real" instead, which works, but the collision is structural
  and the next fixture hits it. Either the vocabulary rule should exempt the
  synthetic-data statement explicitly, or rule 6 should prescribe wording that
  satisfies both. Deciding it once is cheaper than each author inventing a
  dodge.
- **The vocabulary grep is a proxy, and an incomplete one.** `dateOfBirth`
  passes it — "dob" is not a word inside that string — so the check misses the
  spelled-out form of a concept the rule bans. Task 27 renamed it anyway, on
  the spirit rather than the letter. The token list will keep drifting behind
  the vocabulary it is trying to catch; treat a clean grep as necessary and not
  sufficient.
- **Nothing enforces the vocabulary grep at build time.** It is run by hand,
  which means it is run when someone remembers. Task 27 named this out of
  scope deliberately — it is its own task, with its own failure modes around
  what a build-time check does to a module that legitimately needs the
  vocabulary. Worth doing before the rule has been quietly violated for another
  three milestones, which is how it got here.
- **Nothing checks a claim one file makes about another.** Task 27's rename
  made `FellegiSunterResolutionTest`'s Javadoc false — it said this package's
  shared fixtures "would name a person if imported here", which stopped being
  true the moment they were renamed. It was caught by reading, and nothing in
  the build would have caught it. This is the same class of defect as milestone
  1's invisible-character literal: correct code, a comment that lies.
- **Coverage can concentrate in the wrong module.** Task 29 found that core's
  `DefaultAliasRepositoryTest` mixed variant with translation and never
  translation with nickname, so all 31 of its tests stayed green under a
  reorder of exactly those tiers — the real coverage lived in
  `jresolve-profiles-ie`. Closed for this case by three new tests. The general
  risk is not closed: a core behaviour exercised only through a profile is a
  core behaviour core does not pin.

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
