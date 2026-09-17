# 35 — Maven Central readiness

**Repo:** .
**Depends on:** 34 (lands under its check)
**Owns:**
- pom.xml
- jresolve-core/pom.xml
- jresolve-profiles-ie/pom.xml

## Goal
A maintainer can publish a release without hand-assembling anything, and a
build that is not a release neither signs nor slows.

## Context
- docs/design-decisions.md#d19 — the coordinates are settled: `io.github.aindriub:jresolve-*`, published to Central under the GitHub-derived namespace. That decision is made; this task implements it.
- LICENSE — Apache 2.0, which is what the POM must declare.
- pom.xml — `maven-javadoc-plugin` is already configured for the doclint gate at `verify`. The javadoc **jar** is a different goal and must not disturb that execution.
- Central requires, and rejects a bundle without: `name`, `description`, `url`, `licenses`, `developers`, `scm`, plus sources and javadoc jars and a detached signature per artifact.

## Acceptance
- [ ] Every module carries `name`, `description` and `url`; the parent carries `licenses`, `developers` and `scm`, inherited by both modules. A description must describe *that* module, not be copied between them.
- [ ] `maven-source-plugin` and a javadoc **jar** execution produce the two required jars. The existing doclint execution at `verify` keeps working and keeps failing the build on a stale reference — a test of that is that the gate still fires.
- [ ] Signing and any publishing plugin sit in a **profile that is off by default**. An ordinary `mvn clean verify` must not invoke GPG, must not require a key, and must not slow down. State the profile's name in the POM comment.
- [ ] **No credentials, key material, tokens or server ids with secrets anywhere in the repository.** Where a publish would need them, name the mechanism (`settings.xml`, CI secrets) rather than embedding anything.
- [ ] `mvn clean verify` passes unchanged, with the same test count as before: this task adds no tests and must not perturb the build's default path.
- [ ] The release profile is **shown to assemble**, at least as far as it can without a signing key: run it and report what it produced and where it stopped. An untested release profile is discovered broken on release day.
- [ ] The version stays `0.1.0-SNAPSHOT`. Choosing a release version is the maintainer's call and is not this task's.

## A judgement this task owes
This task makes publishing *possible*. Task 28's gate makes it *impermissible*
until a licensed alias corpus replaces the illustrative tables.

State that plainly in the commit body, so nobody reads a working release
profile as permission to use it. If the two are easy to confuse from the POM
alone, say whether a comment in the POM is the right place to say so.

## Out of scope
- Publishing anything, tagging a release, or choosing a version.
- A release workflow in CI. Configuration first; automation is its own task, and one that touches secrets.
- Changing the coordinates. D19 settled them.
