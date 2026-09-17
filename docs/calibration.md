# Calibration

What the numbers in a scoring model mean, where they are expected to come
from, and what this library will and will not claim about them.

Four classes point here: `FellegiSunterModel` and `FellegiSunterScorer` for
the independence assumption, `UnlabelledMatchExample` for where `m` comes
from, and `ProbabilityModel` for what a model's probability is worth. Each
has a section below.

`docs/design-decisions.md` carries the arguments; this file carries the
consequences for someone configuring a model. Where the two disagree, that
file wins.

## The five words this library refuses to blur

The original specification (§94) asks that these be kept apart, and the type
system keeps them apart rather than the prose doing it alone.

| Term | What it is | What the library gives you |
|---|---|---|
| **Similarity** | A metric's output for two values, in `[0,1]` | `FieldEvidence.getSimilarity()`. A measurement, nothing more. |
| **Likelihood ratio** | `m/u` for one field's category | Not exposed directly; it is what a weight is the log of. |
| **Raw score** | The summed weight `W = Σ log₂(m/u)` | `Score.getValue()` on `LOG2_LIKELIHOOD_RATIO`. |
| **Probability** | A posterior, given a prior | `Score.getProbability()` — **null unless the model carries prior odds**. |
| **Calibrated probability** | A probability shown to match observed frequencies | **The library never claims this.** Nothing here is calibrated. |

Two rules from the specification are kept verbatim because they are the
errors this class of system fails on most often: a similarity is not a
probability (§5), and blocking is not proof of identity (§34).

## The conditional-independence assumption

Fellegi-Sunter assumes the fields are **conditionally independent given match
status**. Summing per-field weights *is* that assumption being used — there is
no separate place where it is switched on.

It is frequently false.

Consider two fields that co-vary within a household: a surname and an address.
Two records for different people at one address agree on both far more often
than independence predicts. The model, not knowing this, treats the two
agreements as two separate pieces of evidence and multiplies their likelihood
ratios. The shared signal is counted twice, and the resulting score is
**systematically overconfident** — not noisy, but biased in one direction, and
worst exactly where a reviewer is least likely to question it.

### What the library does about it

Nothing automatically. It cannot: whether two fields co-vary is a fact about
the data, not about the code.

What it offers is a declaration. `DefaultFellegiSunterModel.Builder.composite`
lets a consumer name two or more fields as one comparison, and
`FellegiSunterScorer` then weighs that group **once**.

How it combines them is declared per group, because correlation strength is a
property of the fields in a group rather than of the model holding them — one
model may hold a tightly coupled group and a barely coupled one, and a single
setting could not describe both.

| `CompositeRule` | The group contributes | Defensible when |
|---|---|---|
| `SMALLEST` *(default)* | its least favourable member's weight | always; it is the conservative answer |
| `AVERAGE` | the mean of its present members' weights | the correlation has been measured and is moderate |
| `STRONGEST` | its most favourable member's weight | the shared signal is known to be small |

`SMALLEST` is the default deliberately. When the scorer cannot know how much of
the signal is shared, the group should claim no more than its least favourable
member, and overconfidence is the failure mode already in play.

**There is no measurement procedure for this choice, and that is the thing to
know before changing it.** `u` can be counted from a corpus and `m` estimated
by expectation-maximisation, but nothing in this library or outside it tells
you which of these three rules matches your data. A consumer who has not
measured the within-group correlation has no basis to move off `SMALLEST`, and
a knob without a procedure is one somebody turns until the score looks better.

A suppressed member still appears in the explanation, with a template key
recording that it was counted as part of a composite rather than ignored. Under
`AVERAGE` the member carrying the group's weight is also marked, because the
value it reports is the group's and not its own — reporting it as an ordinary
contribution would claim that field measured something it did not.

## Where each number comes from

A model has three kinds of number, and they are **not** obtained the same way.
A consumer who measures one and guesses the others should know which is which.

### `u` — P(category | non-match)

For an agreement category, this is approximately how often the value occurs in
the population. It is the one number here you can genuinely **measure**: count
a candidate corpus with `TermFrequencyTable` and the model will use the
observed frequency in place of a flat figure.

Two cautions.

A corpus is a sample, so a value it never saw is rare rather than impossible.
The table returns a configurable **floor** instead of zero — a zero would make
`u` zero and `log₂(m/u)` infinite.

The table is **per field**. A field the corpus does not cover falls back to the
flat configured `u`, because the floor is the table's *rarest* answer, and
reading it as a frequency would give the largest possible weight to a field
nothing is known about. Supplying a corpus for one field does not silently
inflate the others.

### `m` — P(category | true match)

This is **not** a corpus frequency and cannot be counted from unlabelled data
directly. It comes from one of two places:

- **Labelled pairs**, where match status is known. `LabelledMatchExample` is
  the shape for those. In practice labels are the expensive thing nobody has
  enough of.
- **Expectation-maximisation over unlabelled pairs**, which is how `m` is
  usually estimated in practice. `UnlabelledMatchExample` exists for this
  reason, and its absence would have blocked the standard path.

Estimation happens outside this library. The library defines the
representations so that a consumer's estimator has something to consume, and
models are built rather than deserialized (see `docs/design-decisions.md#d16`).

### Prior odds — the base rate before any evidence

Neither measured nor estimated from the comparison data: this is the
consumer's own knowledge of how often a source record has a true match in the
candidate set at all. Resolving against a set that contains the right answer
almost always is a different problem from resolving against one that rarely
does, and no amount of field evidence tells you which you are in.

Prior odds are **optional**, and leaving them unset is a real choice rather
than an omission. A model without them produces a weight and no probability,
which is the honest output when nobody has supplied a base rate. The model
reports "not set" distinctly from "even odds" so the two cannot be confused.

## What a probability from this library is worth

Where prior odds are set, `FellegiSunterScorer` computes:

```
posterior odds = prior odds × 2^W
probability    = posterior odds / (1 + posterior odds)
```

That number is a probability **under the model's own assumptions**. It is
arithmetic performed correctly on the inputs supplied. It is not evidence that
those inputs were measured, that the independence assumption holds for the
data, or that the resulting figure matches any observed rate of true matches.

A model with plausible-looking `m` and `u` values and a plausible-looking
prior produces a plausible-looking probability whether or not any of the three
was ever checked against reality. Nothing in the library can tell the
difference, and it does not pretend to.

The same applies to any future `ProbabilityModel`: a value from such a model
is a probability under that model, and production confidence still depends on
whether its training data resembled the data being resolved (§94).

**The library ships no calibrated model, and no default worth trusting.**

## What this file is not

It is not a tutorial on estimating a model: it names the methods and their
inputs, and stops there.

It deliberately recommends **no specific `m` or `u` values**. Any number that
looked authoritative here would be copied into production by someone who
should have measured it, and would then carry this file's apparent endorsement
into a decision it was never entitled to influence.
