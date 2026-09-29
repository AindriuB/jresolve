# 41 — Refresh the README for 0.1.0

**Repo:** .
**Depends on:** 36, 37, 38
**Owns:**
- README.md
- jresolve-core/src/test/java/io/github/aindriub/jresolve/readme/ReadmeExamplesTest.java
- jresolve-profiles-ie/src/test/java/io/github/aindriub/jresolve/profiles/ie/readme/ReadmeExamplesTest.java

## Goal
The README is the Central and GitHub landing page for 0.1.0. It should show the
real coordinates and say plainly that the profiles module is not published. It
should describe the behaviour that wave 1 turned into API rather than warning
about traps that no longer exist, and it should route readers to the new
guides.

## Context
- README.md:34-65 — Install. It shows `0.1.0-SNAPSHOT` for both modules (:40, :51).
- README.md:156-160 — the "Weight `EXACT` on fuzzy fields too" warning. Task 36 made `build()` reject that configuration.
- README.md:278-282 — "`SUBSUMED` is not a constant on `ComparisonCategory`". After task 36 it is.
- README.md:397-416 — "Where to go next" and "Building".
- docs/plan/tasks/36-api-sharp-edges.md — the exact `build()` rule and the `EXACT = 0.0` opt-out.
- docs/plan/tasks/38-release-gate-and-pom.md — why profiles-ie is unpublished (D19, task 28).
- The new files this wave creates, whose exact paths the links must use: `DEVELOPING.md` (task 43), `RELEASING.md` (task 44) and `docs/tuning.md` (task 42).
- Baseline: `mvn clean verify` on b80acc2 gives 634 tests (553 core + 81 profiles). Wave 1 changes the count, so state the figure on the merged `main` this task branches from.

## Acceptance
- [ ] The core install snippet reads `io.github.aindriub` / `jresolve-core` / `0.1.0`, with no `SNAPSHOT`.
- [ ] The profiles-ie install section states that the module is **not published** to Central in 0.1.0, because D19's gate holds until a licensed alias corpus lands (task 28). It shows how to build it from source with `mvn install` instead.
- [ ] The two sharp-edge warnings are replaced by statements of current behaviour. `RuleBasedScorer.Builder.build()` rejects partial-agreement weights without `EXACT` or a default, and `.weight(f, EXACT, 0.0)` is the explicit opt-out. `SUBSUMED` and `PARTIAL_OVERLAP` are on `ComparisonCategory`. `rg -n "not a constant on" README.md` returns nothing.
- [ ] Every README example touched above is still compiled and run by one of the two `ReadmeExamplesTest`s. Each test asserts the behaviour the prose now describes. The core test includes a case where `build()` rejects a band-only config.
- [ ] The README links to javadoc.io (`https://javadoc.io/doc/io.github.aindriub/jresolve-core`) and to Central (`https://central.sonatype.com/artifact/io.github.aindriub/jresolve-core`). It also links to `DEVELOPING.md`, `RELEASING.md` and `docs/tuning.md` by exactly those relative paths.
- [ ] The Building section points to `DEVELOPING.md` instead of repeating it.
- [ ] Every claim milestone 7 required is still present: a similarity is not a probability, nothing shipped is calibrated, and the Irish tables are illustrative (D19). The README still promises no blocking, candidate index, phonetics, estimator or logistic regression.
- [ ] Core's test file passes `DomainVocabularyTest`. `mvn clean verify` passes, and the commit body states the count.

## Out of scope
- Writing `DEVELOPING.md`, `RELEASING.md` or `docs/tuning.md`. Link to them only.
- Badges, and changing the pom version.
- API changes. If something is awkward to explain, record it in the commit body.
- `CLAUDE.md` and `docs/architecture.md`. The scribe routes those.
