# Design decisions

`docs/spec/original-design.md` is the specification this project started from.
It is kept unedited because its numbered sections are a useful index and because
several of its rules — the anti-patterns in §109 especially — are worth keeping
verbatim.

This file records where the implementation departs from it, and why. **Where the
two disagree, this file wins.** Each entry names the original section, states
the problem concretely, and gives the resolution in the form the code takes.

The entries are ordered by how expensive they are to fix later. D1–D6 change
type signatures and must land before any code that depends on them; D7–D13
change behaviour within a fixed shape; D14–D19 are corrections and scope calls.

---

## D1 — Source and candidate need not share a value type

*Amends §10.*

`FieldDefinition<S, C, V>` typed both extractors as producing the same `V`. That
holds only when both sides of the integration model the field identically, which
is exactly what does not happen: the external feed carries `String fullName`
where the internal record has `firstName` and `lastName`, or one side has
`List<String>` address lines against the other's structured address type.

The extractors converge on the *normalized* type instead of the raw one:

```java
public final class FieldDefinition<S, C, N> {
    private final String name;
    private final Function<S, N> sourcePreparer;
    private final Function<C, N> candidatePreparer;
    private final FieldComparator<N> comparator;
    private final int cost;         // see D4
    private final boolean required; // see D12
}
```

The builder keeps the symmetric case as sugar — `field(name, S::get, C::get,
pipeline)` composes each extractor with the pipeline's `prepare` — and adds an
overload taking two different extractors and two different `prepare` functions
for the asymmetric case.

## D2 — Normalization is hoisted out of the candidate loop

*Amends §11, §12, §73.*

`FieldPipeline.compare(V source, V candidate)` normalized both sides on every
call. Resolving against 500 candidates therefore performed 500 identical Unicode
decompositions, case foldings and alias lookups of the source value. §73 says not
to add caching before profiling, which is right, but this is not a caching
question — it is the shape of the interface, and no cache fixes a shape.

```java
public interface FieldPipeline<V, N> {
    N prepare(V value);
    FieldEvidence compare(N left, N right);
}
```

`prepare` is documented as null-safe, deterministic and idempotent —
`prepare(prepare(x))` is meaningless for a changed type, so the property test
becomes `prepare(x).equals(prepare(render(prepare(x))))` only where `N` is
`String`; elsewhere determinism is the testable property.

Two consequences fall out for free. Blocking keys can be derived from `N` rather
than re-deriving their own normalization (§33 otherwise duplicates it), and a
`CandidateIndex` can store candidates already prepared, which removes the
candidate-side cost too.

## D3 — `ComparisonCategory` is an open value type

*Amends §19, §106.*

The enum closes the scoring layer. Categories are the keys of the
Fellegi-Sunter `m`/`u` tables and of the rule weights, so a comparator that
cannot mint a category cannot be scored — and §106 requires that new field types
(`phoneNumber()`, `email()`, `postcode()`) be addable without touching the core.
A phone comparator wants `SAME_SUBSCRIBER_DIFFERENT_FORMAT`; there is no honest
way to spell that as `HIGH`.

```java
public final class ComparisonCategory {
    public static final ComparisonCategory EXACT = of("EXACT");
    // ... standard set ...
    public static ComparisonCategory of(String name); // interned, equals by name
}
```

Interning gives identity comparison and a stable `hashCode` for map keys. The
cost is losing `switch`, which the core does not use — every consumer of a
category is a map lookup.

## D4 — `MISSING` splits, and comparison is cost-ordered

*Amends §19, §62, §70.*

**Missingness.** `MISSING` conflated "neither side has a value" with "one side
does". Those have very different `m` and `u`: both-absent is no evidence at all,
one-absent is weak negative evidence about a record that should have had one.
A single category cannot express both, so `MISSING_ONE` and `MISSING_BOTH` are
separate categories.

**Cost ordering.** §70 asks for cheap comparisons before expensive ones, but
`MatchScorer.score(evidence)` requires evidence for every field, so nothing can
be skipped. Fields declare a `cost` tier; the resolver compares in ascending
cost and evaluates hard `CandidateRule`s between tiers. A rejection short-
circuits the remaining tiers and yields evidence flagged `isComplete() == false`.
A scorer that cannot score partial evidence rejects it explicitly rather than
treating the absent fields as `MISSING_BOTH`, which would be a silent lie.

## D5 — Fellegi-Sunter gains prior odds and frequency-adjusted `u`

*Amends §43, §44, §46.*

Two omissions, the second more valuable than the first.

**Prior odds.** `W = Σ log₂(m/u)` is a log likelihood ratio. Turning it into a
posterior needs the prior odds of a match, and the model had nowhere to put them
— which makes §46's "a probability may only be exposed where the model is
calibrated" permanently unreachable on the FS path. `FellegiSunterModel` carries
`priorOdds()`. When it is set, the scorer emits `probability`; when it is not,
`probability` is null and only the weight is exposed. The Javadoc says plainly
that a posterior computed this way is conditional on the model's own assumptions
and is not evidence of empirical calibration.

**Frequency.** This is the single highest-value feature Fellegi-Sunter offers
and the interface as specified could not express it. `u` is P(agreement | non-
match), and for a surname that is roughly the frequency of the value in the
population — so agreement on a common surname is weak evidence and agreement on
a rare one is strong. Keying `u` on `(field, category)` alone makes them equal.

```java
double uProbability(String field, ComparisonCategory category, String frequencyKey);
```

`FieldEvidence.getFrequencyKey()` returns the agreed normalized value when the
comparison agreed, and null otherwise; a model with no frequency table ignores
the argument. `TermFrequencyTable` builds from a candidate corpus, with an
explicit floor so a value unseen in the corpus cannot produce `u = 0`.

## D6 — A score carries its scale, and margins are defined on it

*Amends §41, §53, §55, §69.*

`ScoreType` named the *algorithm*; what the decision engine needs is the
*scale*, and they are not the same thing. A margin of 0.02 is an enormous
evidence gap at p = 0.99 and a negligible one at p = 0.5, so `best −
secondBest` has no fixed meaning until the scale is known.

`Score` carries `ScoreScale` — `LOG2_LIKELIHOOD_RATIO`, `PROBABILITY`, or
`POINTS` — alongside the value and the algorithm. `DecisionThresholds` declares
the scale it was written against, and `build()` fails when it does not match the
configured scorer's. That turns "thresholds silently interpreted on the wrong
scale" from a production mystery into a construction-time exception.

Margin is computed on the score's own scale. For `LOG2_LIKELIHOOD_RATIO` the
margin *is* the log ratio of the two candidates' likelihoods, which is the
quantity the REVIEW threshold actually wants. §69's "infinity when there is no
second candidate" is replaced by `hasSecondBest()` plus a margin that is only
meaningful when it returns true — sentinel values in a double field get
arithmetic done to them eventually.

## D7 — Aliases are equivalence groups; strength leaves the data

*Amends §17, §65, §67.*

`Set<NameAlias> findAliases(String canonicalValue)` has a direction problem —
nothing says whether `Seán ↔ John` is found under `Seán` or under `John`, and
neither is canonical — and a transitivity problem: `Robert ↔ Bob` plus
`Robert ↔ Bobby` must make `Bob ↔ Bobby` an alias, which a pair lookup does not
give you.

```java
public interface AliasRepository {
    Set<String> equivalents(String normalizedValue);  // includes the input
    ComparisonCategory relation(String left, String right); // null when unrelated
}
```

Groups are closed transitively at construction, where the closure is computed
once and its cost is visible, rather than at lookup time where it is not.
Lookups are on the *normalized* value, so the repository never sees a diacritic
or an apostrophe variant.

`NameAlias.strength` is removed. It was a scoring weight living in reference
data, which contradicts the specification's own separation of evidence from
scoring — the same alias table would need different strengths under a rule
scorer and an FS model. Instead the *kind* of alias is the comparison category
(`ALIAS_TRANSLATION`, `ALIAS_NICKNAME`, `ALIAS_VARIANT`), and the scoring model
assigns each its weight. An FS model can then learn that a nickname agreement
and a translation agreement carry different evidence, which a single `strength`
number cannot represent.

## D8 — Ó Súilleabháin is an alias problem, not a normalization one

*Amends §64.*

§64 lists `Ó Súilleabháin` / `O'Suilleabhain` alongside `Seán` / `Sean` in the
normalization tests, which implies normalization resolves the pair against
`O'Sullivan`. It does not: after folding you have `o suilleabhain` against
`osullivan`, and Jaro-Winkler on that pair lands below the MEDIUM band. It is
the same class of problem as `Seán ↔ John` — two languages' renderings of one
name — and it belongs to the alias layer.

The normalization test set keeps only pairs that normalization genuinely
resolves (`Seán`/`Sean`, the apostrophe variants, `Ó`/`O`). The
Irish-to-English surname pairs move to the alias fixtures. Leaving them where
they were would push someone toward increasingly aggressive normalization rules
chasing a result normalization cannot produce.

Phonetics are a related trap: Soundex is anglocentric and does badly on Irish
orthography. If a phonetic comparator ships it is Double Metaphone or a
hand-written Irish ruleset, and it is scored as one weak feature among several
rather than as a matching decision.

## D9 — Address comparison is asymmetric, and the symmetry property narrows

*Amends §26, §84, §90.*

§90's ambiguous example — source `Dublin`, candidates `Dublin 4` and `Dublin 8`
— is not a similarity problem. The source is *less specific* than either
candidate, not in conflict with them, and a symmetric similarity score cannot
represent the difference between "the source omits detail the candidate has" and
"the two disagree".

`AddressEvidence` gains a subsumption signal: whether the source's tokens are a
subset of the candidate's (and the converse). That makes the ambiguous case
legible — both candidates *contain* the source, neither contradicts it, so the
margin is genuinely small and REVIEW is the correct outcome for the right
reason.

This puts §84's `similarity(a,b) ≈ similarity(b,a)` in tension with the address
comparator, so that property is scoped to `SimilarityMetric` implementations —
where Jaro-Winkler and Levenshtein are *exactly* symmetric, not approximately,
and an asymmetry is a bug rather than a tolerance. Field comparators are not
required to be symmetric, and each one documents which it is.

## D10 — Explanations are value-free by construction

*Amends §57, §104.*

§104 forbids logging personal values; §57 gives `FieldContribution` a free-form
`explanation` string, and the worked example in that section prints one. A
free-form string next to a personal value gets the value interpolated into it
within a month, and then it is in whatever the consumer logs.

`FieldContribution` carries a category, a contribution and a template key —
never a rendered sentence containing values. Rendering is the consumer's, from
data it already holds. A `diagnostics(true)` builder flag adds prepared values
for local debugging; it is off by default, and its Javadoc says what turning it
on means for a production log.

## D11 — Conditional independence is stated, not assumed silently

*Amends §50.*

§50 correctly warns that Jaro-Winkler and Levenshtein are correlated and must
not be summed into a probability. Fellegi-Sunter makes a stronger assumption in
the same family and the specification does not mention it: fields are
conditionally independent given match status. Surname and address are not —
household members share both — so an FS model over `surname` and `address` double
counts the household signal and is systematically overconfident.

The library cannot fix this, but it must not hide it. `docs/calibration.md`
states the assumption and where it fails, `FellegiSunterScorer`'s Javadoc
repeats it, and the model supports declaring two fields as one composite
comparison for the cases where a consumer wants to handle it.

## D12 — Redundant state removed from the result types

*Amends §41, §54, §56.*

Three cases of the same shape:

- `MatchResult` carried both `decision` and `matched`, which can disagree.
  `isMatch()` is derived from `decision == MATCH`, and `getMatch()` returns null
  for every other decision, documented on the method.
- `ScoredMatch.contributions` (`Map<String, Double>`) and
  `ScoredCandidate.contributions` (`List<FieldContribution>`) are two shapes for
  one thing. Only `List<FieldContribution>` survives; it is ordered, and order is
  how an explanation reads.
- `FieldPipeline<V, N>` declared an `N` it never used, which forced
  `FieldDefinition` to hold `FieldPipeline<V, ?>`. D2 gives `N` a job.

Also from §38: `CandidateRule` appears in the specification but in neither
architecture diagram nor the builder, so nothing says when it runs. It runs
between cost tiers, before scoring, as a veto (see D4), and the builder exposes
it as `.rule(...)`.

## D13 — `CandidateIndex` does not require string IDs

*Amends §35, §36, §97.*

`Set<String> find(BlockKey)` plus `C get(String id)` forces every candidate to
have a string ID — contradicting §97's "do not assume every candidate has an
`id`" — and produces an N+1 lookup per block key.

```java
public interface CandidateIndex<C> {
    Collection<C> lookup(Collection<BlockKey> keys); // union, deduplicated
}
```

One batched call, identity handled inside the index. Deduplication for the
direct-collection path uses the optional `Function<C, Object> identity` from
§97; without one, reference identity, documented.

## D14 — `maven.compiler.release=8` does not work on JDK 8

*Amends §3, §101.*

`--release` arrived in JDK 9. The configuration §3 recommends fails on the one
JDK it is meant to guarantee compatibility with. §3 half-notes this and leaves
the resolution open; the resolution is: **build on JDK 17, pinned by
maven-toolchains, with `maven.compiler.release=8`.** Building on JDK 8 is not
supported and the build says so rather than failing obscurely.

Two consequences worth knowing now. Modern JDKs warn that source 8 is obsolete
and the target will eventually be removed outright, so the toolchain pin is what
keeps this reproducible rather than a nice-to-have. And `release=8` checks the
API surface but not everything — animal-sniffer against the `java18` signature
runs in `verify` and is not optional, because §110 makes Java 8 compatibility an
acceptance criterion rather than an aspiration.

## D15 — Domain profiles ship as a separate module

*Amends §7, §8, §31.*

§31 requires that the library contain no knowledge of any particular consuming
application, and §8 then places `name/`, `address/` and `IrishNameProfile` in the
core package tree. Irish name handling is domain knowledge rather than
application knowledge, so it does not violate §31 literally — but a package
boundary inside one jar is a convention, and nothing stops core code importing
across it.

Two Maven modules make it structural: `jresolve-core` cannot reference
`jresolve-profiles-ie` because the dependency runs the other way and the compiler
enforces it. The alias data also versions independently of the engine, which is
what you want when the data changes more often than the code.

## D16 — Model configuration is built, not deserialized

*Amends §58, §60, §93.*

§60 forbids a JSON dependency; an FS model is a table of `m`/`u` values that has
to come from somewhere. The core answer is that models are plain immutable
objects with builders, and loading them from whatever format a consumer uses is
the consumer's job. If a serialization convenience is wanted later it is a
separate optional artifact, never a core dependency.

§93's `MatchTrainingExample` (features plus a boolean label) is also not enough
to calibrate anything in practice. `m` is normally estimated by EM over
*unlabelled* pairs, which needs feature vectors without labels. The
representation includes an unlabelled form so the future path is not blocked,
and `docs/calibration.md` says which estimation method each field of a model is
expected to come from.

## D17 — What v1 does not include

*Amends §100.*

The fourteen phases are a reasonable decomposition and a large first release.
Logistic regression (phase 9) and the JMH harness (phase 13) move behind v1:
rules and Fellegi-Sunter cover the stated problem, and the `FeatureExtractor` /
`ProbabilityModel` interfaces still ship unimplemented so §107's extension
requirement holds. Blocking moves *earlier* than phase 10 — it shapes
`CandidateIndex` and the prepared-value model from D2, and retrofitting it after
the resolver exists means changing the resolver.

## D18 — Kept from the original, deliberately

Not everything wanted changing, and the reasons are worth recording so nobody
re-opens them.

§109's ten anti-patterns stand verbatim. §34 (blocking is not proof of identity)
and §5 (a similarity is not a probability) are the two errors this class of
system fails on most often, and stating them as rules rather than as prose is
correct. §62's treatment of missing data as non-contradictory, §63's separation
of conflict from zero similarity, §96's determinism requirement and §104's
privacy rules are all kept as written.

## D19 — Open questions

Not decisions. These need an answer before the code that depends on them.

- **Maven coordinates.** `io.github.aindriub:jresolve-*` with base package
  `io.github.aindriub.jresolve` assumes publication to Maven Central under the
  GitHub-derived namespace. An internal-only library would use a different
  group, and changing it later is a breaking change for every consumer.
- **Alias corpus provenance.** The Irish/English and nickname tables need a
  source with a licence compatible with this project's. Hand-written
  illustrative fixtures are enough for tests but not for a release.
- **Whether phonetics ship at all in v1.** They are low-yield next to a good
  alias table (D8) and add a dependency or a nontrivial hand-written ruleset.
