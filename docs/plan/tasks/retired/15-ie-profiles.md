# 15 — Irish name aliases and the address pipeline

**Repo:** .
**Depends on:** 13, 14
**Owns:**
- jresolve-profiles-ie/src/main/java/io/github/aindriub/jresolve/profiles/ie/**
- jresolve-profiles-ie/src/test/java/io/github/aindriub/jresolve/profiles/ie/** *except* `.../profiles/ie/endtoend/**`, which task 16 owns

## Goal
The first real content in `jresolve-profiles-ie`, which has been a
`package-info.java` and a POM since task 01. This is where domain vocabulary is
allowed and where D15's module split stops being an assertion and starts being
exercised: core cannot reference this module, and the compiler enforces it.

Two deliverables: the alias tables that make task 13's comparator useful, and
the address pipeline that makes task 14's subsumption comparator useful.

## Context
- docs/design-decisions.md#d15 — why this is a separate module rather than an `ie` package, and why the data versions separately from the engine.
- docs/design-decisions.md#d8 — the Irish/English surname pairs belong in *these* fixtures, not in the normalization tests. `Ó Súilleabháin`/`O'Sullivan` is an alias entry.
- docs/design-decisions.md#d19 — alias corpus provenance is **unresolved**. Everything this task ships is illustrative, and must say so.
- docs/architecture.md, "Modules" — this module owns "Irish/English name and address profiles, alias data, phonetics".
- jresolve-profiles-ie/pom.xml — already depends on core with JUnit and AssertJ at test scope; no POM change is needed.
- CLAUDE.md rule 6 — no real personal data; fixtures are synthetic.

## Acceptance
- [ ] An `IrishNameAliases` factory returns a built `AliasRepository` covering at least: Irish/English given-name translations (`ALIAS_TRANSLATION`), common diminutives (`ALIAS_NICKNAME`), and orthographic variants that normalization does not close (`ALIAS_VARIANT`).
- [ ] The Irish/English **surname** pairs from D8 are present as alias entries, and a test asserts the `Ó Súilleabháin`/`O'Sullivan` pair resolves here — the case D8 says normalization cannot close.
- [ ] Every alias entry is fed through the same normalization the pipeline applies, so a lookup on a normalized value hits (D7 requires lookups on normalized values). A test asserts a diacritic-bearing input resolves.
- [ ] The class Javadoc states in its first paragraph that the tables are **illustrative and not release data**, and cites D19's open provenance question. A release must not be able to ship this believing it is sourced.
- [ ] A test asserts the transitive closure holds through the real table: a nickname of a translation is reachable from either end.
- [ ] An `IrishAddressPipeline` composes normalization, `DefaultTokenSplitter` and `TokenSubsumptionComparator` into a `FieldPipeline`, and is the only place in the repository where "address" and token subsumption meet.
- [ ] A test reproduces D9's §90 case with real values: source `Dublin` against `Dublin 4` and `Dublin 8` yields, for both candidates, containment rather than conflict and the same subsumption direction.
- [ ] A test asserts the address pipeline distinguishes subsumption from conflict on real values: a genuinely different locality yields neither subsumption direction, while a less-specific source yields one.
- [ ] Any comparator written here extends the now-public `AbstractNullSafeFieldComparator` and applies `SimilarityBands.categoryFor` rather than reimplementing either (task 12 opened both for exactly this).
- [ ] `mvn verify` passes for the reactor, and `jresolve-core` contains no reference to this module. A test or a note records that the dependency direction is compiler-enforced.
- [ ] No fixture anywhere in this task is a real person. Place names are not personal data and are fine; a name attached to an address is not.

## Out of scope
- Phonetics. D19 leaves whether they ship at all unanswered, and D8 argues they are low-yield next to a good alias table. Do not add Soundex or Double Metaphone here.
- Sourcing a licensed corpus. That is D19's open question and it is not this task's to close.
- Postcode (Eircode) structure, county inference, or any address parsing beyond tokens.
- Changing anything in `jresolve-core`.
