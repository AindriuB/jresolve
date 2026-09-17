# 30 — The domain-vocabulary rule is enforced by the build

**Repo:** .
**Depends on:** nothing
**Owns:**
- docs/conventions.md
- CLAUDE.md
- jresolve-core/src/test/java/io/github/aindriub/jresolve/DomainVocabularyTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/endtoend/EndToEndResolutionTest.java

## Goal
The rule that `jresolve-core` may not name a domain concept fails the build when
violated, and its text says what it cannot catch.

## Context
- docs/conventions.md — the rule as it stands, with the hand-run grep.
- docs/plan/PLAN.md — "Raised in milestone 5, wave 1": the three gaps this task closes are the first three bullets.
- jresolve-profiles-ie/src/test/java/io/github/aindriub/jresolve/profiles/ie/ModuleBoundaryTest.java — the precedent, and the model for tone. It asserts what it honestly can and states plainly what a test in its position cannot prove. Match that.
- CLAUDE.md rule 6 — the synthetic-data requirement this rule collides with.

## Three gaps, one rule

**The collision with rule 6.** Rule 6 requires a synthetic-data statement, and
the natural phrasing names a person. **Resolution: prescribe the wording rather
than exempt the statement.** An exemption is a hole a checker has to implement
and a reader has to remember; a sanctioned phrase costs one sentence in
`conventions.md` and keeps the check absolute. This is a convention and
therefore reversible by the maintainer — it is recorded as a decision, not
smuggled in as an implementation detail.

**The incomplete token list.** `dateOfBirth` passes the current grep because
"dob" is not a word inside it. Measured against an extended list, core has
exactly one real violation left — `EndToEndResolutionTest:403` says "The stored
record has no first name on file", which task 27's rename missed because the
grep only matches camelCase.

**The false positive that shapes the design.** `ComparisonCategory:42` says
"Returns the interned category with the given name" — ordinary English for "the
supplied name", not a forename. A checker banning the spaced form "given name"
fails on correct code. So: camelCase identifier forms are banned for every
variant, and spaced prose forms only for the unambiguous ones.

## Acceptance
- [ ] `DomainVocabularyTest` reads `jresolve-core`'s own `src/main/java` and `src/test/java` and fails on a violation, naming the file and line. It must read **sources**, not compiled classes: the rule covers Javadoc, and Javadoc is not in the bytecode.
- [ ] It catches the camelCase identifier forms — `firstName`, `lastName`, `fullName`, `givenName`, `familyName`, `surname`, `dateOfBirth`, `birthDate` — and the bare tokens `address`, `person`, `dob`, `irish`.
- [ ] It catches the unambiguous spaced prose forms (`first name`, `last name`, `date of birth`) and **does not** flag "the given name". A test asserts both halves against literal sample strings, so the false positive is pinned rather than avoided by luck.
- [ ] **The checker is tested on its own inputs.** Feed it a synthetic violating string and a synthetic clean one and assert the verdict, rather than only running it over the real tree. A checker that silently matches nothing passes a clean repository.
- [ ] It fails loudly if it finds no source files at all — a path or working-directory mistake must not look like a pass. This is the failure mode that makes a source-reading test worthless.
- [ ] `EndToEndResolutionTest:403`'s stale comment is corrected to the renamed vocabulary.
- [ ] `docs/conventions.md` carries the sanctioned synthetic-data wording, states that the check is enforced by `DomainVocabularyTest` rather than by hand, and says plainly what it cannot catch: an English word that happens to match, and a domain concept named in words the list does not hold.
- [ ] `CLAUDE.md` rule 6 points at the sanctioned wording so an author does not have to rediscover it.
- [ ] `jresolve-profiles-ie` is unaffected: the rule does not bind it, and the test must not read it.
- [ ] `mvn clean verify` passes.

## A judgement this task owes
The extended list found exactly one real violation and one false positive. That
is a ratio worth reporting.

Say in the commit body whether the check earns its place — whether a gate this
thin is worth a build step — or whether the honest finding is that the rule was
already nearly satisfied and the value is in the *next* violation rather than
this one.

## Out of scope
- Binding the rule to `jresolve-profiles-ie`.
- A general lint framework, or a plugin dependency. The precedent is a plain test.
- Catching semantic staleness in prose — task 31 narrows that, and neither closes it.
