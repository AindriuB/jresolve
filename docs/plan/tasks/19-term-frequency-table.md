# 19 — Term frequency table

**Repo:** .
**Depends on:** none
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/TermFrequencyTable.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/scoring/TermFrequencyTableTest.java

## Goal
The data behind D5's highest-value claim: agreement on a common value is weak
evidence and agreement on a rare one is strong. `u` is P(agreement |
non-match), and for a value drawn from a population that is roughly its
frequency in that population — so a model keyed on `(field, category)` alone
makes a common-surname agreement weigh exactly as much as a rare one.

This task ships only the counting. Task 20 consults it.

## Context
- docs/design-decisions.md#d5 — the frequency half of the entry, including the explicit floor and why `u = 0` must be unreachable.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/evidence/FieldEvidence.java — `getFrequencyKey()` is the agreed normalized value when the comparison agreed and null otherwise; that key is what this table is keyed on.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/field/ExactFieldComparator.java — its Javadoc states the `toString`-consistent-with-`equals` requirement on `N`. That requirement exists *for this table*: if two occurrences of one value produce different keys, every value looks rare. Cite it.
- docs/conventions.md#errors — configuration failures name the constraint, never the value.

## Acceptance
- [ ] `TermFrequencyTable` is final and immutable, built through a builder, and safe for concurrent reads; the Javadoc says so.
- [ ] The builder counts observations per `(field, key)` — a corpus is fed in one value at a time, so a consumer can stream a candidate set without materialising it.
- [ ] `frequencyOf(field, key)` returns the observed relative frequency of that key within that field: the count for the key over the total observations for the field.
- [ ] **An unseen key returns the floor, never zero.** A value absent from the corpus is rare, not impossible, and a zero would make `u = 0` and the log ratio infinite. A test asserts an unseen key returns exactly the floor.
- [ ] The floor is configurable and defaults to a documented value; construction rejects a floor outside `(0, 1]`.
- [ ] A key whose observed frequency falls *below* the floor also returns the floor; a test asserts this with a corpus large enough that one observation is rarer than the floor.
- [ ] An unknown field returns the floor rather than throwing — a model may be asked about a field the corpus never covered.
- [ ] `frequencyOf` rejects a null field or key with `IllegalArgumentException`; the message names the constraint, not the key.
- [ ] Frequencies for one field sum to at most 1.0 across observed keys; a test asserts this over a small corpus, allowing for the floor.
- [ ] A test with a deliberately skewed corpus asserts the property the whole feature exists for: a common key's frequency is materially higher than a rare key's, so a `u` derived from it is materially higher and the resulting weight materially lower.
- [ ] The Javadoc states that keys are already-normalized values and that the table applies no normalization of its own — the same requirement `AliasRepository` carries, and for the same reason.
- [ ] The Javadoc states the `toString`-consistent-with-`equals` requirement the keys depend on, citing `ExactFieldComparator`.
- [ ] Lookup is not linear in the number of keys; a test builds a corpus of at least a thousand distinct keys and asserts a lookup still resolves.
- [ ] No type, member or Javadoc word in this file names a person, name, address, date of birth or country. Fixtures are neutral tokens.

## Out of scope
- Anything that reads the table — task 20 owns the model that consults it.
- Estimating `m`. `m` is P(agreement | true match) and is not a corpus frequency; it comes from labelled data or EM, and `docs/calibration.md` (task 23) says so.
- Persisting or loading a table (D16: models are built, not deserialized).
- Smoothing schemes beyond the floor.
