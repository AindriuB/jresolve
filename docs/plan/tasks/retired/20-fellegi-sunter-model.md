# 20 — The Fellegi-Sunter model

**Repo:** .
**Depends on:** 19
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/FellegiSunterModel.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/DefaultFellegiSunterModel.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/scoring/DefaultFellegiSunterModelTest.java

## Goal
The `m` and `u` tables, the prior odds, and the composite-field declaration —
everything the scorer needs, with none of the scoring.

## Context
- docs/design-decisions.md#d5 — both halves. The `u` signature gains a frequency key; `priorOdds()` is what makes §46's "a probability may only be exposed where the model is calibrated" reachable at all.
- docs/design-decisions.md#d11 — fields are assumed conditionally independent given match status, surname and address are not, and the model must support declaring two fields as one composite comparison for consumers who want to handle it.
- docs/spec/original-design.md §44 — the original two-method interface and the validation `0 < m <= 1`, `0 < u <= 1`. Read that section only.
- docs/spec/original-design.md §45 — missingness must be explicit; a missing value is not automatically a non-match unless the model says so. Read that section only.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/TermFrequencyTable.java — task 19's table (read after 19 lands).
- docs/conventions.md#naming — interface named for the role, default implementation `Default<Role>`.

## Acceptance
- [ ] `FellegiSunterModel` is an interface declaring at least: `mProbability(field, category)`, `uProbability(field, category, frequencyKey)`, and a way to read the prior odds that distinguishes "set" from "not set".
- [ ] The `u` signature takes the frequency key as D5 specifies. A model with no frequency table ignores the argument, and its Javadoc says so — the argument is not optional in the signature even where it is unused, because a caller must not have to know which kind of model it holds.
- [ ] `DefaultFellegiSunterModel` is final, immutable, built through a builder, and safe for concurrent reads.
- [ ] Construction rejects any `m` or `u` outside `(0, 1]`, naming the field and the constraint but never a value. A test covers 0, negative, and above 1 for both.
- [ ] Construction rejects a null field, a null category and a null probability.
- [ ] **A frequency-adjusted `u` is the point of the entry.** When a `TermFrequencyTable` is configured and a non-null frequency key is given, `uProbability` returns a value derived from the observed frequency rather than the flat configured `u`. A test asserts that agreement on a common key yields a *higher* `u` — and therefore a lower weight — than agreement on a rare one.
- [ ] A null frequency key falls back to the configured flat `u`; a test asserts this, and the Javadoc says why: a comparison that did not agree has no value to be common or rare.
- [ ] A frequency-adjusted `u` stays within `(0, 1]`; a test asserts the range. **Amended at implementation** — the criterion asked for a clamp, and there is none. `TermFrequencyTable` already returns a value within `[floor, 1]` with `floor > 0`, so the range holds by construction and a clamp would guard nothing. Adding one would be the same defect PLAN.md records against task 07's D6 check: a guard that fires correctly while protecting nothing. The test pins the property instead, including the unseen key where a zero would otherwise arrive and make the weight infinite.
- [ ] **Prior odds are optional and their absence is legible.** A model without them reports that fact rather than defaulting to 1.0 — a caller must be able to tell "the odds are even" from "nobody said". A test asserts both states.
- [ ] Prior odds, when set, are rejected unless strictly positive.
- [ ] **Missingness is declared, never inferred** (§45). The model states, per field or globally, how `MISSING_ONE` and `MISSING_BOTH` are treated, and a category with no configured `m`/`u` is a configuration error rather than a silent default. A test asserts that an unconfigured category fails loudly.
- [ ] A consumer can declare two fields as one composite comparison (D11), and the model reports that declaration. Scoring it is task 21's; this task ships the declaration and its validation — a composite naming an unconfigured field fails at build time.
- [ ] The class Javadoc states the conditional-independence assumption in its own words, names surname and address as the standard counterexample, and points at `docs/calibration.md`.
- [ ] No type, member or Javadoc word in these files names a person, name, address, date of birth or country. **This constrains the counterexample above**: state it as two fields that co-vary within a household without naming which, or cite the design-decisions entry that is allowed to name them.
- [ ] Fixtures are neutral tokens.

## Out of scope
- Computing a weight or a score — task 21.
- Estimating `m` or `u` from data. The model is configured, not trained (D16).
- Loading a model from any serialized form (D16).
- Logistic regression (D17 puts it behind v1).
