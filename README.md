# jresolve

Probabilistic entity resolution for Java 8: match an arbitrary source object
against candidate objects on fuzzy field evidence, and get back a ranked,
**explainable** decision.

No framework, no JSON, no database, no logging dependency. The library does
comparison, scoring and explanation; everything else is yours.

```java
MatchResult<Stored> result = resolver.resolve(incoming, candidates);

result.getDecision();   // MATCH, REVIEW or NO_MATCH
result.getScore();      // a value, its scale, and the algorithm that made it
result.getCandidates(); // ranked, each with per-field contributions
```

## What this is not

Worth knowing before you read further, because these are the errors this class
of system fails on most often:

- **A similarity is not a probability.** A Jaro-Winkler score of 0.93 is a
  measurement, not a 93% chance of a match.
- **Nothing here is calibrated.** The Fellegi-Sunter scorer computes a
  posterior correctly from the numbers you give it. Whether those numbers
  describe your data is your problem, and the library will not pretend
  otherwise. See [docs/calibration.md](docs/calibration.md).
- **There is no blocking or candidate index.** You hand `resolve` a candidate
  list; choosing that list is your job, and on a large corpus it is the job
  that matters most.
- **No phonetics, no estimator, no logistic regression.** Not in v1.

## Install

```xml
<dependency>
  <groupId>io.github.aindriub</groupId>
  <artifactId>jresolve-core</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

`jresolve-core` knows nothing about names, addresses or countries. Domain
knowledge ships separately and is optional:

```xml
<dependency>
  <groupId>io.github.aindriub</groupId>
  <artifactId>jresolve-profiles-ie</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

**Java 8 is a hard target**, enforced by animal-sniffer at build time. Building
the project needs a JDK 17 toolchain; using it does not.

> Every code example below is compiled and run by a test, so it cannot drift
> from the API: [`ReadmeExamplesTest`](jresolve-core/src/test/java/io/github/aindriub/jresolve/readme/ReadmeExamplesTest.java)
> for the neutral examples, and
> [the profiles copy](jresolve-profiles-ie/src/test/java/io/github/aindriub/jresolve/profiles/ie/readme/ReadmeExamplesTest.java)
> for the Irish ones.

---

## 1. The smallest resolver

Two fields, exact matching, points scoring. Your two record types need not
share a shape — a `Function` pulls each field from each side.

```java
private static FieldPipeline<String, String> exactText() {
    return new DefaultFieldPipeline<>(value -> value, new ExactFieldComparator<>());
}

DecisionThresholds thresholds =
        new DecisionThresholds(15.0, 5.0, 1.0, ScoreScale.POINTS);

EntityResolver<Incoming, Stored> resolver = EntityResolverBuilder
        .<Incoming, Stored>builder()
        .field("reference", Incoming::getReference, Stored::getReference, exactText())
        .field("label", Incoming::getLabel, Stored::getLabel, exactText())
        .scorer(RuleBasedScorer.builder()
                .weight("reference", ComparisonCategory.EXACT, 10.0)
                .weight("label", ComparisonCategory.EXACT, 10.0)
                .baseScore(0.0)
                .build())
        .thresholds(thresholds)
        .decisionEngine(new ThresholdDecisionEngine<>(thresholds))
        .build();

MatchResult<Stored> result = resolver.resolve(
        new Incoming("AB-1", "widget"),
        Arrays.asList(new Stored("1", "AB-1", "widget"), new Stored("2", "ZZ-9", "gadget")));

result.getDecision();          // MATCH
result.getMatch().getId();     // "1"
result.getScore().getValue();  // 20.0
```

`DecisionThresholds` takes **match**, **review**, **minimum margin** and a
scale. The margin is what stops two equally good candidates being declared a
match: if the best and second-best are closer together than the margin, the
result is `REVIEW` rather than `MATCH`.

**Configuration errors fail at `build()`, not at `resolve()`.** A duplicate
field name, a null extractor, thresholds on the wrong scale — all throw
`EntityResolutionConfigurationException` with a message naming the field and
the constraint.

---

## 2. Fuzzy matching

Two pieces: a **normalizer** that makes values comparable, and a **comparator**
that measures what is left.

```java
private static FieldPipeline<String, String> fuzzyText() {
    StringNormalizer normalizer = new CompositeNormalizer(Arrays.asList(
            new UnicodeFormNormalizer(),
            new CombiningMarkNormalizer(),
            new CaseFoldNormalizer(),
            new WhitespaceNormalizer()));

    return new DefaultFieldPipeline<>(
            normalizer::normalize,
            new SimilarityFieldComparator(new JaroWinklerSimilarity(), new SimilarityBands()));
}
```

Normalization is applied **once per source record**, not once per candidate —
so an expensive normalizer costs the same whether you compare against ten
candidates or ten thousand.

A similarity comparator does not hand you a raw number to threshold. It bands
the score into a `ComparisonCategory`, and you weight the categories:

| Category | Default band |
|---|---|
| `VERY_HIGH` | ≥ 0.95 |
| `HIGH` | ≥ 0.85 |
| `MEDIUM` | ≥ 0.70 |
| `LOW` | below that |

```java
.scorer(RuleBasedScorer.builder()
        .weight("reference", ComparisonCategory.EXACT, 8.0)
        .weight("label", ComparisonCategory.EXACT, 6.0)
        .weight("label", ComparisonCategory.VERY_HIGH, 5.0)
        .weight("label", ComparisonCategory.HIGH, 3.0)
        .baseScore(0.0)
        .build())
```

> **Weight `EXACT` on fuzzy fields too.** A similarity comparator returns
> `EXACT` when the two values are equal *after* normalization — which is the
> common case, since normalization exists to collapse differences. A config
> that weights only the bands scores a perfect agreement as **zero**. This is
> the single easiest mistake to make with this API.

So `"Widget  Co"` against `"widget co"` is `EXACT` (case and spacing
normalized away), scoring 8 + 6 = 14. And `"Widgit Co"` against `"widget co"`
is a genuine near-miss: Jaro is (8/9 + 8/9 + 8/8) / 3 = 0.9259, the shared
prefix `widg` boosts it to 0.9259 + 4 × 0.1 × (1 − 0.9259) = **0.9556**, which
lands in `VERY_HIGH` and scores 8 + 5 = 13.

Three metrics ship: `JaroWinklerSimilarity` (good on short strings with typos
and shared prefixes), `LevenshteinSimilarity` (edit distance, normalized), and
`TokenSimilarity` (set overlap, for multi-word values where order varies).

---

## 3. Cost tiers and rules

Comparing every field against every candidate is wasteful when a cheap field
could have ruled the candidate out. Fields declare a cost; the resolver
compares in ascending order and evaluates **hard rules between tiers**.

```java
CandidateRule<Incoming, Stored> referenceMustNotConflict = (source, candidate, evidence) ->
        evidence.getField("reference") != null
                && evidence.getField("reference").getCategory() == ComparisonCategory.CONFLICT
                ? RuleDecision.REJECT
                : RuleDecision.CONTINUE;

EntityResolverBuilder
        .<Incoming, Stored>builder()
        .field("reference", Incoming::getReference, Stored::getReference, exactText())
        .cost("reference", CostTiers.CHEAP)
        .field("label", Incoming::getLabel, Stored::getLabel, fuzzyText())
        .cost("label", CostTiers.EXPENSIVE)
        .rule(referenceMustNotConflict)
        // ...
```

A rejected candidate short-circuits: the expensive tier never runs for it. A
veto is **final** — it is not a very negative score that something else might
outweigh.

---

## 4. Reading the result

```java
result.getDecision();                  // MATCH | REVIEW | NO_MATCH
result.getScore().getValue();
result.getScore().getScale();          // POINTS | LOG2_LIKELIHOOD_RATIO | PROBABILITY
result.getScore().getAlgorithm();      // which scorer produced it
result.getScore().getProbability();    // null unless the model genuinely has one

for (FieldContribution c : result.getCandidates().get(0).getContributions()) {
    c.getField();
    c.getCategory();
    c.getContribution();   // how much this field moved the score
    c.getTemplateKey();    // a stable key, never a rendered message
    c.getSubsumption();    // whether one side contained the other
}

for (RejectedCandidate<Stored> rejected : result.getRejectedCandidates()) {
    rejected.getCandidate();
    rejected.getRuleKey();  // "resolver.rule.1" — which rule vetoed it
}
```

**Explanations carry no field values.** A contribution gives you a template key
and a category, never a rendered string containing someone's data — so a
`MatchResult` is safe to log. Rendering messages for a human is your job, in a
place you control.

`getRejectedCandidates()` is why a vetoed candidate is not simply lost: without
it you cannot tell a candidate that scored badly from one that was never
scored at all.

---

## 5. Aliases and subsumption — where a join stops being enough

Everything so far, a SQL join could approximate. These two cannot be.

**Aliases** relate values that are neither equal nor similar. `Seán` and `John`
share no letters worth measuring; they are the same name in two languages.

```java
AliasRepository aliases = IrishNameAliases.repository();

aliases.relation("sean", "john");   // ALIAS_TRANSLATION
aliases.equivalents("sean");        // contains "john"
aliases.relation("sean", "margaret"); // null — unrelated, not weakly related
```

Groups are closed transitively at construction, and the *kind* of relation is
stored per pair: if `Liam`–`William` is a translation and `William`–`Will` a
nickname, then `Liam`–`Will` reports the weaker of the two, while both declared
pairs keep what they were declared as.

> ⚠️ **The Irish alias tables are illustrative, hand-written fixtures — not
> reference data.** They exist to exercise the mechanism. A release that
> treated them as authoritative would be a mistake, and nothing publishes until
> a properly sourced corpus replaces them.

**Subsumption** distinguishes *less specific* from *contradictory*.
`Bandon, Co. Cork` is not a different address from
`12 Main Street, Bandon, Co. Cork` — it is the same place, described less
precisely. A similarity metric sees two strings of different lengths and calls
it a weak match; that is how a real match gets thrown away.

```java
FieldPipeline<String, String> pipeline = IrishAddressPipeline.forSingleLine();

String less = pipeline.prepare("Bandon, Co. Cork");
String more = pipeline.prepare("12 Main Street, Bandon, Co. Cork");

pipeline.compare(less, more).getSubsumption();  // LEFT_SUBSUMES_RIGHT
pipeline.compare(less, more).getCategory();     // SUBSUMED, never CONFLICT
```

`SUBSUMED` is not a constant on `ComparisonCategory`. Categories are an **open
value type** — a comparator mints the ones it can produce, and owns them:

```java
.weight("address", TokenSubsumptionComparator.SUBSUMED, 12.0)
```

Put together, a resolver using both matches records an equality join cannot
reach at all:

```java
MatchResult<Record> result = resolver.resolve(
        new Applicant("Seán", "Ó Súilleabháin",
                Arrays.asList("12 Main Street", "Bandon", "Co. Cork")),
        Arrays.asList(new Record("1", "John", "O'Sullivan", "12 Main Street, Bandon, Co. Cork")));

result.getDecision();  // MATCH
```

Note the address field uses the **asymmetric** `field(...)` overload: the
source carries a list of lines and the candidate a single joined string, so
each side gets its own normalizer and they converge on one compared type.

---

## 6. Fellegi-Sunter

The rule-based scorer asks you to invent weights. Fellegi-Sunter asks you for
two probabilities per field and category, and derives the weight:

```
weight = log₂(m / u)        m = P(agreement | the records match)
                            u = P(agreement | they do not)
W      = Σ weightᵢ
```

The gain is not the formula. It is this: **`u` can be measured.** For an
agreement category, `u` is roughly how often the value occurs in the
population — so agreement on a rare value is stronger evidence than agreement
on a common one, and the score says so.

```java
TermFrequencyTable.Builder corpus = TermFrequencyTable.builder();
for (int i = 0; i < 9; i++) {
    corpus.observe("reference", "common");
}
corpus.observe("reference", "rare");

DefaultFellegiSunterModel model = DefaultFellegiSunterModel.builder()
        .probabilities("reference", ComparisonCategory.EXACT, 0.9, 0.1)
        .probabilities("reference", ComparisonCategory.CONFLICT, 0.1, 0.9)
        .frequencies(corpus.build())
        .build();

DecisionThresholds thresholds =
        new DecisionThresholds(2.0, 0.0, 1.0, ScoreScale.LOG2_LIKELIHOOD_RATIO);

EntityResolver<Incoming, Stored> resolver = EntityResolverBuilder
        .<Incoming, Stored>builder()
        .field("reference", Incoming::getReference, Stored::getReference, exactText())
        .scorer(new FellegiSunterScorer(model))
        .thresholds(thresholds)
        .decisionEngine(new ThresholdDecisionEngine<>(thresholds))
        .build();
```

Agreeing on `"rare"` now outscores agreeing on `"common"`, from the same
configuration, with no rule written for it.

A corpus is **per field**. A field the corpus does not cover falls back to the
flat configured `u` — supplying frequencies for one field does not silently
inflate the others.

### The probability is the least of it

```java
result.getScore().getProbability();  // null
```

Null, unless the model carries **prior odds** — your own knowledge of how often
a source record has a true match in the candidate set at all. Leaving them
unset is a real choice, not an omission: a weight and no probability is the
honest output when nobody has supplied a base rate.

Where they are set, the posterior is arithmetic performed correctly on the
inputs you supplied. It is *not* evidence that those inputs were measured, that
fields are conditionally independent in your data, or that the figure matches
any observed rate. A model with plausible-looking numbers produces a
plausible-looking probability whether or not any of them was ever checked.

`docs/calibration.md` says where each number comes from and refuses to
recommend any values, deliberately — a number that looked authoritative there
would be copied into production by someone who should have measured it.

### Correlated fields

Fellegi-Sunter assumes fields are conditionally independent given match status.
Surname and address are not: household members share both, so a model over the
two counts the shared signal twice and is **systematically overconfident** —
biased in one direction, worst exactly where a reviewer is least likely to
question it.

Declare them as one comparison:

```java
DefaultFellegiSunterModel.builder()
        .probabilities("surname", ComparisonCategory.EXACT, 0.9, 0.1)
        .probabilities("address", ComparisonCategory.EXACT, 0.5, 0.25)
        .composite(Arrays.asList("surname", "address"), CompositeRule.SMALLEST)
        .build();
```

`SMALLEST` is the default and the conservative answer: the group claims no more
than its least favourable member. `AVERAGE` and `STRONGEST` exist, but note
that **no measurement procedure tells you which to choose** — if you have not
measured the within-group correlation, leave it alone.

---

## Where to go next

| You want | Read |
|---|---|
| Why the design departs from the original spec | [docs/design-decisions.md](docs/design-decisions.md) |
| Module boundaries and the type model | [docs/architecture.md](docs/architecture.md) |
| What a score means, and what it does not | [docs/calibration.md](docs/calibration.md) |
| What is still open | [docs/plan/PLAN.md](docs/plan/PLAN.md) |

## Building

```
mvn clean verify
```

Needs a JDK 17 toolchain in `~/.m2/toolchains.xml` — see
[docs/architecture.md](docs/architecture.md#building). The build enforces the
Java 8 target with animal-sniffer, fails on a Javadoc reference that no longer
resolves, and fails if `jresolve-core` names a domain concept.

## Licence

Apache 2.0. See [LICENSE](LICENSE).
