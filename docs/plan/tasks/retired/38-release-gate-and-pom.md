# 38 — Make D19's gate mechanical, scope it to profiles-ie, and prepare core's release path

**Repo:** .
**Depends on:** none
**Owns:**
- pom.xml
- jresolve-profiles-ie/pom.xml
- jresolve-core/pom.xml — only if the bundle check proves it needs a change
- .github/workflows/release.yml (new)
- docs/design-decisions.md — the D19 section only

## Goal
D19's release gate is prose today: `-Prelease` over the whole reactor would
sign and upload illustrative alias tables. This task makes the gate a build
failure that applies to `jresolve-profiles-ie` only. That turns
`-pl jresolve-core -am` into the single release path that works. It also
brings the publishing plugins up to the versions irc-client has already
published with, and adds a CI release workflow that stays dormant until
Actions is available again.

## Context
- pom.xml:56-63 — plugin version properties. `maven-gpg-plugin` is 3.2.4 and `central-publishing-maven-plugin` is 0.5.0.
- pom.xml:280-331 — the `release` profile and the comment explaining that publishing is "POSSIBLE, not PERMITTED". That comment gets rewritten.
- pom.xml:147-170 — the existing `maven-enforcer-plugin` execution, the model for the new rule's shape.
- /srv/dev/projects/irc-client/pom.xml:214-245 — gpg 3.2.8, central-publishing 0.11.0, `autoPublish=false`. These are the published versions to match.
- /srv/dev/projects/irc-client/.github/workflows/release.yml — the workflow to model on: tag and `workflow_dispatch` triggers, a `dry_run` input, secrets passed through `setup-java`, and a check that the tag matches the pom version.
- .github/workflows/build.yml:29-74 — how this repo reads `toolchain.jdk.version` from the POM and writes `toolchains.xml`. The release workflow reuses it.
- docs/design-decisions.md:469-490 — D19. The provenance bullet says "No release until then" for the whole project.
- Baseline: `mvn clean verify` on b80acc2 gives 634 tests (553 core + 81 profiles), `BUILD SUCCESS`.

## Acceptance
- [ ] `maven-gpg-plugin.version` is `3.2.8` and `central-publishing-maven-plugin.version` is `0.11.0`.
- [ ] The central-publishing configuration excludes `jresolve-profiles-ie` through `excludeArtifacts`, and `autoPublish` stays `false`.
- [ ] `jresolve-profiles-ie/pom.xml` declares the property `jresolve.aliasCorpus` with value `illustrative`. Its own `release` profile runs a `maven-enforcer-plugin` `requireProperty` rule that fails unless the property equals `sourced`. The failure message names D19 and task 28, and says to release with `-pl jresolve-core -am`.
- [ ] `mvn -B -Prelease -Dgpg.skip clean verify` from the root **fails**, in `jresolve-profiles-ie`, with that message. The commit body quotes the message line.
- [ ] `mvn -B -Prelease -Dgpg.skip -pl jresolve-core -am clean verify` **passes**.
- [ ] `mvn -B -Prelease -Dgpg.skip -pl jresolve-core -am clean deploy -DskipPublishing=true` builds a bundle without uploading. `unzip -l` on it lists the parent `jresolve` pom and core's `.jar`, `-sources.jar`, `-javadoc.jar` and `.pom`, and no path containing `profiles-ie`. The commit body pastes the file list, names only, with no sizes or dates. With `-Dgpg.skip` there are no `.asc` files, and the body says so.
- [ ] Plain `mvn clean verify` still passes with 634 tests. This task adds no tests and must not change the default path.
- [ ] The pom comment above the release profile describes the gate as it now works: what fails, where, why, and the one command that releases core. It no longer says the whole release is forbidden.
- [ ] D19's provenance bullet is amended to say the gate binds `jresolve-profiles-ie`, because that module ships the tables, and that `jresolve-core` carries no alias data and may be published. It names the enforcer rule as the mechanism, and dates the amendment 2026-09-29. The rest of D19 is unchanged.
- [ ] `.github/workflows/release.yml` exists and parses (`python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/release.yml'))"`). It triggers on `v*` tags and on `workflow_dispatch` with a boolean `dry_run` input. It provisions the toolchain the way `build.yml` does, and every Maven invocation in it carries `-Prelease -pl jresolve-core -am`. Its header comment says it is dormant while GitHub Actions is blocked, that 0.1.0 is released by local deploy, and that it must be disabled before pushing a `v*` tag if Actions is live, or it will attempt a duplicate upload.
- [ ] No credentials, key material or tokens anywhere. The workflow references secrets by name only, and the pom names `settings.xml` server id `central` only.
- [ ] The version stays `0.1.0-SNAPSHOT`.

## Out of scope
- Publishing, staging, tagging or choosing a version. The release is manual, after the milestone.
- Removing `jresolve-profiles-ie` from the parent's `<modules>`. That is a noted risk, and deliberately not changed.
- `RELEASING.md` (task 44) and `DEVELOPING.md` (task 43), which describe this gate afterwards.
- Any other section of `docs/design-decisions.md`, including the `required` sketch at :37.
- `.github/workflows/build.yml`.
