# 43 — Write `DEVELOPING.md`, the contributor guide

**Repo:** .
**Depends on:** 36, 37, 38, 39
**Owns:**
- DEVELOPING.md (new)
- jresolve-core/src/test/java/io/github/aindriub/jresolve/developing/DevelopingGuideExamplesTest.java (new)

## Goal
A contributor cloning the repo meets six build gates and a toolchain
requirement, and each one fails with a message that makes sense only if you
already know the rule. This guide should get them from clone to green build,
explain each gate by its real failure text, and show how to extend the library
with a comparator or a profile module.

## Context
- docs/architecture.md#building (:35-65) — the toolchain explanation and the example `toolchains.xml`. The scribe will point this section at `DEVELOPING.md` afterwards.
- .github/workflows/build.yml:29-74 — a toolchain setup that is known to work.
- pom.xml — animal-sniffer (:103-118, :176), the Maven-version enforcer rule (:147-170), and doclint (:222-270).
- jresolve-profiles-ie/pom.xml — the release gate (task 38).
- jresolve-core/src/test/java/io/github/aindriub/jresolve/DomainVocabularyTest.java and jresolve-profiles-ie/src/test/java/io/github/aindriub/jresolve/profiles/ie/ModuleBoundaryTest.java — the two source-reading gates.
- docs/conventions.md — test naming, synthetic data, invisible-character escapes, the Java 8 API list, and the commit format. Link to it; do not restate it.
- jresolve-core/src/test/java/io/github/aindriub/jresolve/extension/FieldComparatorExtensionTest.java and field/AbstractNullSafeFieldComparator.java — how to write a comparator outside `field/`.
- jresolve-profiles-ie/ — the reference profile module: `IrishNamePipeline.forNames()` (renamed by task 39), `ModuleBoundaryTest`, `package-info.java`, and the pom.
- Baseline: `mvn clean verify` on b80acc2 gives 634 tests (553 core + 81 profiles). Wave 1 changes the count, so state the figure on the merged `main` this task branches from.

## Acceptance
- [ ] `DEVELOPING.md` gives a ready-to-paste `~/.m2/toolchains.xml` for JDK 17, with the full namespace header, and quotes the error you get without it.
- [ ] It describes the module layout: core versus profiles-ie, what each may depend on, and the vocabulary rule.
- [ ] It has one subsection per build gate: animal-sniffer, doclint, `DomainVocabularyTest`, `ModuleBoundaryTest`, the Maven-version enforcer rule, and the D19 release gate. Each quotes the **real** failure text, which the implementer produces by making a deliberate break in their worktree and then reverting it, and says how to fix it. The commit body lists each break that was made and confirms it was reverted, and `git diff main --stat` touches only the owned files.
- [ ] It covers test conventions by linking `docs/conventions.md#tests` and summarising in at most five bullets.
- [ ] "Writing a comparator" shows a comparator built on `AbstractNullSafeFieldComparator`, in neutral vocabulary. It states the null-to-`MISSING_*` rule and the no-field-values-in-messages rule, and says a band-only scorer config is rejected (task 36).
- [ ] "Writing a profile module" covers the pom shape, the dependency direction, a `ModuleBoundaryTest` equivalent, and where domain vocabulary is allowed. It cites `jresolve-profiles-ie` by path, not by pasting it.
- [ ] Every Java code block in `DEVELOPING.md` is compiled and run by `DevelopingGuideExamplesTest`, which passes `DomainVocabularyTest`. XML and shell blocks are exempt.
- [ ] `mvn clean verify` passes. The commit body states the count: the base figure plus the tests added.

## Out of scope
- Release steps. They belong to `RELEASING.md` (task 44), so link to it.
- Editing `docs/architecture.md`, `CLAUDE.md`, `README.md` or `docs/conventions.md`.
- Changing any gate, or its message, to make it easier to document.

## Attempt 1 — failed

Tests PASS (649); review REQUEST CHANGES (commit f755ba5). Every gate quote was
verified against the pom and the tests. Two defects:

- **Raw non-breaking space in the sentence that forbids one.**
  `DEVELOPING.md:206`: the example "written as an escape such as `…`" holds a
  raw U+00A0 (bytes 0xC2 0xA0), not the escape text, so the code span renders
  empty. Write the literal escape text, and check with a byte scan that
  DEVELOPING.md has no U+00A0.
- **Contradicts the vocabulary rule.** `DEVELOPING.md:68-69` says core may not
  use "a name of any kind". `docs/conventions.md:47-52` bans `name` only in its
  personal sense and allows field identifiers (`fieldName`,
  `FieldDefinition.getName()`). Restate it to match conventions.md.
- Also fix while there:
  - `:147-151`: point to the sanctioned synthetic-data wording "none describes
    anyone real" (`docs/conventions.md:69-75`), since "none names a real person"
    is the likeliest trip.
  - `:143`, `:161`: the quoted `:98` and `:53` line numbers come from broken
    builds and drift (`:53` is already `:52`). Say the line varies, or drop it.
- Continue on branch `task/43-developing-guide`.

## Attempt 2 — failed

Review REQUEST CHANGES (commit 7df6dbe). The U+00A0, sanctioned-wording and
line-number points are resolved. The main defect was not changed, though the
commit body says it was.

- **Vocabulary rule still contradicts conventions.md.** `DEVELOPING.md:68-69`
  still says core may not name "a person, a name of any kind, …". 7df6dbe has no
  hunk there. `docs/conventions.md:47-52` bans `name` only in its personal
  sense (`firstName`, `surname`, …) and allows it as a field identifier
  (`fieldName`, `FieldDefinition.getName()`). Rewrite lines 68-69 to say that,
  and confirm with `git show HEAD:DEVELOPING.md | sed -n 66,70p` before claiming
  it is fixed.
- Also re-wrap `DEVELOPING.md:148`.
- Continue on branch `task/43-developing-guide`.
