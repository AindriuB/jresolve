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

**Not yet planned: D5 (Fellegi-Sunter).** Prior odds, frequency-adjusted `u`,
`TermFrequencyTable`, and `docs/calibration.md`. Deliberately not decomposed
yet — its shape depends on the frequency-key work, and planning it before 17
and 18 landed would have been speculative. It is now the obvious next
milestone, and better positioned than it was: task 12 documented the
`toString`-consistent-with-`equals` requirement frequency keys depend on, and
agreement is no longer confined to exact matches.

## Notes for implementers

- `jresolve-core` needs a JDK 17 toolchain (`~/.m2/toolchains.xml`, not part
  of the repo) — see `docs/architecture.md#building`. In Claude Code on the
  web this is provisioned automatically by
  `.claude/hooks/session-start.sh`; on a local machine it is still manual.
- `mvn clean verify` baseline at milestone 3's consolidation close is 490
  tests (412 core + 78 profiles), `BUILD SUCCESS`.

## Known gaps (non-blocking, no task owns these)

Whoever next touches these files should close them in passing rather than
reopen the review. Closed items are not listed; see `HISTORY.md`.

### Raised in milestone 2

- **D19's alias-corpus provenance is now urgent rather than theoretical.**
  `IrishNameAliases` ships illustrative, hand-written tables and says so in
  its first Javadoc paragraph. A release that treats them as reference data
  would be a mistake. This needs a sourced corpus under a compatible licence
  before any publication.
- **The alias strength ordering is a judgement worth a second opinion.**
  `DefaultAliasRepository` resolves a mixed merge by maximum-bottleneck path,
  ordering `ALIAS_VARIANT` > `ALIAS_NICKNAME` > `ALIAS_TRANSLATION` by how
  much each claims about closeness. The mechanism is sound and tested; the
  *ordering* is a call that a domain reviewer should confirm. The builder
  rejects any other category precisely because admitting one means placing it
  in that order.
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

### Open questions for the maintainer

- **Does the domain-vocabulary rule bind test fixtures?**
  `docs/conventions.md:43` forbids `name`, `address`, `person`, `dob`,
  `irish` in "any core type, member, package or Javadoc" and calls it
  grep-checkable. Core's `endtoend/` package has roughly fifty hits, all
  legitimate — an end-to-end test must model a consumer's domain objects, and
  a consumer's types are domain-shaped by definition. So the rule as written
  and as practised differ, and a reviewer running that grep on a future diff
  has to decide by hand each time. The rule should say which sources it
  binds. Not changed unilaterally: it is a convention, and conventions are
  the maintainer's.

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
