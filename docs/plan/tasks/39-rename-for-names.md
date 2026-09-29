# 39 — Rename `IrishNamePipeline.forGivenName()` to `forNames()`

**Repo:** .
**Depends on:** none
**Owns:**
- jresolve-profiles-ie/src/main/java/io/github/aindriub/jresolve/profiles/ie/IrishNamePipeline.java
- jresolve-profiles-ie/src/test/java/io/github/aindriub/jresolve/profiles/ie/IrishPipelineTest.java

## Goal
The factory is used for surnames as well as given names, so its name is
narrower than its use. `PLAN.md` records this as a milestone-2 gap. The
profiles module is not published in 0.1.0, so the rename breaks nobody.

## Context
- jresolve-profiles-ie/src/main/java/io/github/aindriub/jresolve/profiles/ie/IrishNamePipeline.java:34 — `forGivenName()`. `withAliases(...)` is at :44.
- jresolve-profiles-ie/src/test/java/io/github/aindriub/jresolve/profiles/ie/IrishPipelineTest.java:24 — the only caller. Measured on b80acc2 with `rg -n forGivenName`, which found no other use in source or README.
- Baseline: `mvn clean verify` on b80acc2 gives 634 tests (553 core + 81 profiles), `BUILD SUCCESS`.

## Acceptance
- [ ] `IrishNamePipeline.forNames()` exists with the same behaviour, and its Javadoc says it serves given names and surnames alike.
- [ ] `forGivenName()` is removed, not deprecated. `rg -n forGivenName jresolve-* README.md` returns nothing.
- [ ] `IrishPipelineTest` calls `forNames()`. The local variable at :24 may keep its name if it still describes the field under test.
- [ ] `mvn clean verify` passes with 634 tests, unchanged.

## Out of scope
- Adding a surname-specific factory.
- `IrishNameAliases`, `IrishAddressComparator`, and the other profiles gaps in `PLAN.md`.
- `docs/plan/PLAN.md`. The scribe closes the gap.
