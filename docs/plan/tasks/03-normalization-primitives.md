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

## Attempt 1 - failed

Tester PASS (58 tests, both mutations caught, committed bytes verified valid
UTF-8 with no mojibake), reviewer CHANGES. All nine acceptance criteria met; the
defect is a fragility the suite cannot see.

**Defect - must fix.** `WhitespaceNormalizer.java:9` declares
`NON_BREAKING_SPACE` with a literal non-breaking space character, one line above
`SPACE` with a literal ordinary space. The two are visually identical in source,
and the test's raw NBSP is equally invisible. Any editor's trim-whitespace pass
degrades both bytes to plain spaces, `isWhitespace` loses its NBSP arm, and the
suite stays green - the test's own NBSP degrades with it, so the assertion still
passes while the normalizer has silently stopped folding U+00A0. Write the
escape `'\u00A0'` in both the source and the test.

This is not contradicted by the tester confirming the committed bytes are
currently correct. They are. The defect is that nothing would notice when they
stop being correct, and the tester's own mutation run demonstrates the mechanism:
its whitespace mutation was caught *only* by
`NormalizationPropertyTest.applyingAStageToItsOwnOutputIsANoOp`, because every
corpus entry the unit tests exercise already contains ordinary whitespace.

**Also fix.** `CombiningMarkNormalizer` iterates by `char` rather than by code
point. The assumption is sound - `Character.getType` returns `SURROGATE` for
either half of a pair, so non-BMP text passes through intact rather than being
corrupted, and only astral combining marks (U+1D167, U+E0100) go unremoved - but
lines 4-11 say nothing about it, so the next reader has to rediscover it or read
it as a bug. One sentence of Javadoc stating the limit deliberately.

**Remove.** `CompositeNormalizer.getStages()` is permanent public surface added
for one assertion, against the small-public-API rule in docs/conventions.md and
docs/architecture.md. The defensive-copy property is observable without it: add a
stage to the source list after construction and assert `normalize` output is
unchanged. Delete the accessor and rewrite `CompositeNormalizerTest` around
lines 44-60 that way.

**Suggestion.** `NormalizationCorpus.java:10-11` - the disclaimer "No entry is
drawn from a real person, address or dataset" is the only place `person` or
`address` appears in the package, so it trips the domain-vocabulary grep. "No
entry is drawn from real data" says the same thing without the tripwire.

**Settled, keep.** Raw UTF-8 literals are right for the apostrophe set, where
`ApostropheVariantNormalizer` lines 18-20 name each code point next to the
characters and the tester confirmed every byte decodes as intended. The rule that
falls out: a literal a reader can *see* may stay literal; one that is invisible
by definition gets an escape.

## Attempt 2 - failed

Tester PASS (57 tests; the NBSP escape now bites - replacing it with a plain
space fails a named test, where under attempt 1 that mutation was invisible),
reviewer CHANGES. The four items from attempt 1 are all correctly closed. A
fifth instance of the same defect was found.

**Defect - must fix.** `UnicodeFormNormalizerTest.java:37` builds
`String decomposed = "e" + <combining acute U+0301>` with a raw invisible
character rather than an escape. It renders as an accented e, visually identical
to the precomposed character on line 41, so a reader cannot tell the two-char
input from the one-char expectation. Any pass that NFC-normalizes the source
rewrites it to precomposed, and `nfcRecomposesADecomposedCharacter` then asserts
that a precomposed character normalizes to itself - green, vacuous, and the NFC
arm untested. Write `"e\u0301"`, or build the input with
`Normalizer.normalize(..., NFD)` the way `CombiningMarkNormalizerTest` lines 19
and 36 already do. Either closes it.

**Why this keeps happening, and what it means for attempt 3.** The count so far:
review found two, the implementer generalised and found a third, this review
found a fifth. The tester's byte scan reported zero remaining raw NBSP bytes and
was correct - it searched for `C2 A0`, the sequence it was given. The combining
acute is `CC 81` and was never in scope of that scan. So "the NBSP class is
clean" was proven; "the invisible-character class is clean" was not, and the two
read identically in a summary.

Attempt 3 must scan for the class, not the instance: every non-ASCII byte
sequence in the package's committed sources, each one either visible in its
context or escaped. Report the full list with a verdict per entry, so the next
reader can see what was considered rather than what was found.

**Settled, do not undo.** The apostrophe raw literals stay - the tester
confirmed all five decode correctly and the Javadoc names each code point beside
them. Deleting `CompositeNormalizer.getStages()` cost no real coverage: with the
accessor gone the `unmodifiableList` wrapper is unreachable from outside, so the
deleted test asserted an unobservable property, and the rewritten test does fail
when the defensive copy is removed. The BMP-limit Javadoc and the corpus
disclaimer rewording are both accepted.

**Not this task's to fix.** The rule being applied here - a literal a reader can
see may stay literal, an invisible one gets an escape - is written down nowhere
in the repo. It has survived only in these review notes, which is why it was
applied four times and missed a fifth. It belongs in docs/conventions.md, which
is outside this task's `Owns`. Filed separately; do not edit that file from this
branch.
