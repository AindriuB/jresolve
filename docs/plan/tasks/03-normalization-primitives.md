# 03 — Implement the normalization primitives

**Repo:** .
**Depends on:** 01
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/normalization/**
- jresolve-core/src/test/java/io/github/aindriub/jresolve/normalization/**

## Goal
Provide the composable string normalization stages the field layer prepares
values with: Unicode form, case folding, diacritic folding, apostrophe
variants, punctuation and whitespace, plus a composite that runs an ordered,
immutable list of them. Each stage is deterministic, idempotent and leaves its
input untouched.

## Context
- docs/spec/original-design.md §12–§15 — the stage list, `java.text.Normalizer`, the apostrophe variant set. Read those sections only.
- docs/design-decisions.md#d8 — normalization resolves `Seán`/`Sean`, apostrophe variants and `Ó`/`O`, and nothing more; cross-language surname pairs are an alias problem and must not drive more aggressive rules here.
- docs/design-decisions.md#d2 — `prepare` is documented null-safe, deterministic and idempotent; where the normalized type is `String` the idempotence property is directly testable.
- docs/conventions.md#immutability-and-threads — `final` fields, defensive copies, no mutable static state.

## Acceptance
- [ ] `StringNormalizer` is a functional interface `String normalize(String value)`, documented as null-safe (null in, null out), deterministic and idempotent.
- [ ] Stages exist for: Unicode form (constructor takes a `java.text.Normalizer.Form`, default NFD), case folding (`Locale.ROOT`, never the default locale), combining-mark removal, apostrophe variants, punctuation and whitespace.
- [ ] The apostrophe stage folds at least `'`, `’`, `‘`, `ʼ`, `` ` `` and `´` to a single canonical character, and a test asserts three renderings of the same apostrophe-bearing token normalize identically.
- [ ] The punctuation stage takes the character set it acts on as a constructor argument rather than removing all punctuation unconditionally, and its Javadoc says why (§15).
- [ ] The whitespace stage trims and collapses runs of whitespace including non-breaking space and tab to a single space.
- [ ] `CompositeNormalizer` holds an ordered `List<StringNormalizer>` copied defensively into an unmodifiable list, applies them in order, and returns null immediately for a null input.
- [ ] A property test asserts, for every stage and for a composite, over a fixed synthetic corpus of at least 20 strings including Latin-1 diacritics and mixed whitespace: `normalize(normalize(x)).equals(normalize(x))` and two calls on the same input return equal results.
- [ ] A test asserts the input `String` reference is never mutated and that a stage returns the same string content when there is nothing to change.
- [ ] No type, member or Javadoc word in this package names a person, name, address, date of birth or country. `Seán`/`Sean`-style test data is synthetic and confined to test sources.

## Out of scope
- Alias lookup, equivalence groups, `AliasRepository` — a later milestone.
- Any Irish-specific profile or a `FieldPipelines` factory — that is `jresolve-profiles-ie`.
- Tokenisation used for comparison (task 02 owns the splitter it needs) and any `FieldNormalizer<V,N>` adapter — task 05 owns that.
- Phonetic keys, blocking keys.
