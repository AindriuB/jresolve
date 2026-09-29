# Tuning: cutting false positives

You have a resolver, and it matches records it should not. This guide shows how
to read why a candidate matched, and which lever moves the outcome.

## Read this first: no number here is calibrated

Every figure in this guide is arithmetic on weights invented for the example.
None is a recommended value, and none was measured against real data. The
library ships no calibrated model and no default worth trusting; see
[`docs/calibration.md`](calibration.md), especially "The five words this library
refuses to blur". In its terms:

- A **similarity** is a metric's output in `[0, 1]`. It is a measurement and
  nothing more.
- A **raw score** is the sum of the weights you configured. On the points scale
  it is your own arbitrary unit.
- A **probability** exists only where a Fellegi-Sunter model carries prior
  odds, and is then a probability *under that model's own assumptions*.
- A **calibrated probability** is one shown to match observed frequencies. The
  library never claims one.

So a threshold is a line you draw and then check against your own data. The
last section of this guide is how to do that check. Nothing before it tells
you where the line goes.

Every Java block below is compiled and run by
[`TuningGuideExamplesTest`](../jresolve-core/src/test/java/io/github/aindriub/jresolve/tuning/TuningGuideExamplesTest.java),
which asserts the effect each section claims. The blocks use the two record
types and the `resolverFor(scorer, thresholds)` helper defined at the top of
that file: `Incoming` and `Stored` each have a `reference` and a `label`, and
the helper builds a resolver over both fields with an exact comparator.
The fixtures are synthetic tokens; none describes anyone real.

## 1. Diagnose a false positive

Start with the `MatchResult`, not the configuration. Three things explain a
decision: the score against the thresholds, the per-field contributions, and
the candidates a rule removed.

```java
MatchResult<Stored> result = resolverFor(scorer, thresholds).resolve(
        new Incoming("AB-1", "widget"),
        Arrays.asList(new Stored("1", "AB-1", "gadget")));
```

The score against the line it had to clear:

```java
assertThat(result.getDecision()).isEqualTo(Decision.MATCH);
double score = result.getScore().getValue();
double headroom = score - thresholds.getMatchThreshold();
```

`headroom` is how far above `matchThreshold` the winner landed. A false
positive with a small headroom is a threshold problem; one with a large
headroom is a weighting problem.

The contributions say which fields carried it:

```java
List<String> explanation = new ArrayList<>();
for (FieldContribution contribution : result.getCandidates().get(0).getContributions()) {
    explanation.add(contribution.getField() + " " + contribution.getCategory().getName()
            + " " + contribution.getContribution());
}
```

Here the explanation is `reference EXACT 10.0` and `label CONFLICT 0.0`: the
label disagreed and the disagreement cost nothing, so the reference alone
cleared the line. That is the usual shape of a false positive, and sections 3
and 4 are the levers for it.

`result.getRejectedCandidates()` lists candidates a `CandidateRule` vetoed
(section 5). A candidate the scorer could not score, for example because a
required field was missing, is in neither list. It is dropped from
`getCandidates()` without a record, and section 3 shows it.

`FieldContribution` and `RejectedCandidate` carry field names, categories,
numbers and rule keys, never compared values, so a `MatchResult` is safe to
log.

## 2. Thresholds and margins

`DecisionThresholds` takes `(matchThreshold, reviewThreshold, minimumMargin,
scale)`. The best candidate is a `MATCH` at or above `matchThreshold`, a
`REVIEW` between the two, and a `NO_MATCH` below `reviewThreshold`.

Raising `matchThreshold` turns a `MATCH` into a `REVIEW`. Raising
`reviewThreshold` past the score turns it into a `NO_MATCH`:

```java
DecisionThresholds loose = new DecisionThresholds(10.0, 4.0, 1.0, ScoreScale.POINTS);
DecisionThresholds strict = new DecisionThresholds(12.0, 4.0, 1.0, ScoreScale.POINTS);
```

```java
DecisionThresholds stricter = new DecisionThresholds(14.0, 11.0, 1.0, ScoreScale.POINTS);
```

`minimumMargin` is for near-ties. When the best candidate clears
`matchThreshold` but the runner-up is within `minimumMargin`, the result is a
`REVIEW` with no match, because the engine will not pick between two
candidates the scorer could barely tell apart.

```java
DecisionThresholds noMargin = new DecisionThresholds(10.0, 4.0, 0.0, ScoreScale.POINTS);
DecisionThresholds wideMargin = new DecisionThresholds(10.0, 4.0, 2.0, ScoreScale.POINTS);
```

With candidates scoring 11.0 and 10.0, `noMargin` returns a `MATCH` and
`wideMargin` returns a `REVIEW`; `getMargin()` is 1.0. A margin measured in
points means different things at different score levels. On the
`LOG2_LIKELIHOOD_RATIO` scale, a margin is a ratio, which is easier to reason
about. Neither scale makes the number correct.

## 3. Negative weights and required fields

A weight of zero for `CONFLICT` says a disagreement is no evidence. Often that
is too kind: for a field that should agree, a disagreement is evidence against.
Give `CONFLICT` a negative weight, and give `LOW` one where a fuzzy field that
barely resembles its counterpart should count against the pair:

```java
MatchScorer scorer = RuleBasedScorer.builder()
        .weight("reference", ComparisonCategory.EXACT, 10.0)
        .weight("label", ComparisonCategory.EXACT, 2.0)
        .weight("label", ComparisonCategory.CONFLICT, -8.0)
        .baseScore(0.0)
        .build();
```

The score is now 10 + (-8) = 2, under the review threshold of 4, so the
false positive from section 1 becomes a `NO_MATCH`. A fuzzy field takes the
same treatment through `ComparisonCategory.LOW`:

```java
.weight("label", ComparisonCategory.LOW, -8.0)
```

Remember to weight `EXACT` on a fuzzy field. A similarity comparator reports
`EXACT` for values equal after normalization, and `build()` refuses a field
that weights a partial-agreement category with no `EXACT` weight and no
`defaultWeight`. Weight `EXACT` at `0.0` to opt out explicitly.

`requiredField` makes a field non-negotiable. If it is missing on either side,
the scorer reports the candidate as unscorable rather than scoring it low:

```java
MatchScorer scorer = RuleBasedScorer.builder()
        .weight("reference", ComparisonCategory.EXACT, 10.0)
        .weight("label", ComparisonCategory.EXACT, 2.0)
        .requiredField("label")
        .baseScore(0.0)
        .build();
```

An unscorable candidate is absent from both `getCandidates()` and
`getRejectedCandidates()`. That is a different outcome from a veto and the
library keeps the two apart.

## 4. Custom similarity bands

`SimilarityBands` turns a raw similarity into a category. The defaults are
`0.95`, `0.85`, `0.70` and `0.0` for `VERY_HIGH`, `HIGH`, `MEDIUM` and the floor. They are
engineering defaults, not validated thresholds. If your false positives are
near-misses that band too generously, tighten the cut-offs:

```java
SimilarityBands strict = new SimilarityBands(0.98, 0.90, 0.75, 0.0);
```

The cut-offs must strictly descend and lie in `[0, 1]`. A pair scoring about
0.9556 under Jaro-Winkler is `VERY_HIGH` under the defaults and `HIGH` under
`strict`, so whatever weight you gave each category now applies to it
differently. Bands change the category; the weights decide what that category
is worth. Change one and re-check the other.

## 5. Cost tiers and rule vetoes

Fields have a cost tier: `CostTiers.CHEAP`, `MODERATE` or `EXPENSIVE`. The
resolver compares one tier at a time, cheapest first, and offers the evidence
gathered so far to every `CandidateRule` between tiers. A rule that returns
`RuleDecision.REJECT` stops that candidate there: the costlier fields are never
compared and the candidate goes into `getRejectedCandidates()`.

```java
CandidateRule<Incoming, Stored> labelMustNotConflict = (source, candidate, evidence) ->
        evidence.getField("label") != null
                && evidence.getField("label").getCategory() == ComparisonCategory.CONFLICT
                ? RuleDecision.REJECT
                : RuleDecision.CONTINUE;
```

```java
EntityResolver<Incoming, Stored> resolver = EntityResolverBuilder
        .<Incoming, Stored>builder()
        .field("label", Incoming::getLabel, Stored::getLabel, exactText())
        .cost("label", CostTiers.CHEAP)
        .field("reference", Incoming::getReference, Stored::getReference, exactText())
        .cost("reference", CostTiers.EXPENSIVE)
        .rule(labelMustNotConflict)
        .scorer(scorer)
        .thresholds(thresholds)
        .decisionEngine(new ThresholdDecisionEngine<>(thresholds))
        .build();
```

A veto differs from a negative weight: a weight is one term in a sum that other
fields can outvote, and a veto cannot be outvoted. Use a veto for a
disagreement that no amount of other agreement should override. The rejected
candidate's `getRuleKey()` is `resolver.rule.` followed by the rule's position
in the order you registered them, so it names the rule without a value. The
veto never reaches the decision engine.

## 6. Fellegi-Sunter: frequencies, composites and prior odds

The Fellegi-Sunter path replaces hand-picked points with `m` and `u`
probabilities per field and category. Its weight is `log2(m/u)`, and it carries
an assumption you should read before relying on it: the fields are treated as
conditionally independent. See [`docs/calibration.md`](calibration.md).

**Frequency tables.** Without one, every `u` is the flat figure you configured.
With a `TermFrequencyTable`, agreeing on a value that is rare in your corpus
counts for more than agreeing on a common one, which is what cuts false
positives on common values:

```java
TermFrequencyTable.Builder corpus = TermFrequencyTable.builder();
for (int i = 0; i < 9; i++) {
    corpus.observe("reference", "common");
}
corpus.observe("reference", "rare");
```

```java
DefaultFellegiSunterModel weighted = DefaultFellegiSunterModel.builder()
        .probabilities("reference", ComparisonCategory.EXACT, 0.9, 0.1)
        .ignore("label", ComparisonCategory.EXACT)
        .frequencies(corpus.build())
        .build();
```

`ignore` declares that a field and category carry no evidence, which is
clearer than inventing `m` and `u` that mean the same thing.

**Composite rules.** Fields that co-vary carry one signal, and a model that
sums them counts it twice. `composite` declares them one comparison, and the
`CompositeRule` says how much the group claims:

```java
DefaultFellegiSunterModel model = DefaultFellegiSunterModel.builder()
        .probabilities("reference", ComparisonCategory.EXACT, 0.9, 0.1)
        .probabilities("label", ComparisonCategory.EXACT, 0.5, 0.25)
        .composite(Arrays.asList("reference", "label"), rule)
        .build();
```

With member weights of 3.17 and 1.0, `SMALLEST` gives 1.0, `AVERAGE` gives 2.08
and `STRONGEST` gives 3.17. `SMALLEST` is the default and the conservative
choice; `STRONGEST` claims the group is as strong as its best member and is
defensible only where the shared signal is known to be small. Choosing among
them is a judgement about your data, and the rule you pick is not measured
by the library.

**Prior odds.** Without `priorOdds`, a score is a weight and
`getProbability()` is null. With them, the library computes a posterior:

```java
DefaultFellegiSunterModel withPrior = DefaultFellegiSunterModel.builder()
        .probabilities("reference", ComparisonCategory.EXACT, 0.5, 0.25)
        .probabilities("label", ComparisonCategory.EXACT, 0.5, 0.25)
        .priorOdds(0.01)
        .build();
```

With a weight of 2 the posterior odds are 0.01 x 2^2 = 0.04 and the
probability is 0.04 / 1.04, about 0.0385. That figure is arithmetic done
correctly on your inputs. It is a probability under the model, not a
calibrated one. It says nothing about whether `m`, `u` or the prior was
ever measured. The same evidence with a prior of 1.0 gives 0.8. Treat prior
odds as one more lever you must check against labelled data, not as a fact.

## 7. Sweep thresholds against a hand-labelled sample

Every section above changes a number. This one is how to decide, on your data,
whether the change helped. The library ships no sweep tool and recommends no
threshold values; the procedure is yours to run.

1. Take a sample of source records and, for each, the candidate or candidates
   it might match. Have someone who knows the data label each pair: the same
   entity, or not.
2. Resolve every source and record the top candidate's score.
3. For each threshold you want to try, count the false positives (accepted but
   not the same entity) and the missed matches (rejected but the same entity).
4. Look at how the two counts trade off, and pick the point whose cost you can
   live with. That is a decision about your risk, not one the score makes.

```java
double[] topScores = new double[sources.length];
for (int i = 0; i < sources.length; i++) {
    topScores[i] = resolver.resolve(sources[i], Arrays.asList(candidates[i]))
            .getScore().getValue();
}
```

```java
int[] falsePositives = new int[3];
int[] missedMatches = new int[3];
double[] tried = {10.0, 11.0, 12.0};
for (int t = 0; t < tried.length; t++) {
    for (int i = 0; i < sources.length; i++) {
        boolean accepted = topScores[i] >= tried[t];
        if (accepted && !sameEntity[i]) {
            falsePositives[t]++;
        }
        if (!accepted && sameEntity[i]) {
            missedMatches[t]++;
        }
    }
}
```

In the test's four-pair sample the scores are 11, 11, 10 and 0, so a threshold
of 10 accepts one false positive, 11 makes no error, and 12 misses both true
matches. Those thresholds were chosen to make that sample's point. They are not
values to copy: your sample, your weights and your scale give different ones.

Two cautions. A sample small enough to label by hand is small, and a threshold
tuned to it is tuned to it. Keep some pairs aside that you did not tune on. And
if you set prior odds, a good sweep does not make the resulting probability
calibrated; it tells you where your threshold behaves acceptably on this sample.
