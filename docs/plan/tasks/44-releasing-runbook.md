# 44 — Write `RELEASING.md`, the core release runbook

**Repo:** .
**Depends on:** 38
**Owns:**
- RELEASING.md (new)

## Goal
The first Central release is irreversible and uses up publishing allowance.
The steps must be written down before anyone runs them. This means adapting
irc-client's runbook, which has already shipped, to this project's differences:
core only, a gate on profiles-ie, a local deploy while Actions is blocked, and
a manual Publish.

## Context
- /srv/dev/projects/irc-client/RELEASING.md — the source to adapt. Its sections are: one-off setup (:6), signing (:68), if signing fails (:108), how often (:117), each release via CI and by hand (:136-191), before pressing publish (:192), and what the version number promises (:203).
- pom.xml's `release` profile and jresolve-profiles-ie/pom.xml's gate, as task 38 left them.
- .github/workflows/release.yml — dormant (task 38).
- docs/design-decisions.md#d19 — as amended by task 38.
- The release procedure in the approved milestone plan:
  1. Settings token under server id `central`.
  2. Fetch GPG key `5F3261B55F4BEAE3` from keyserver.ubuntu.com and keys.openpgp.org, and set `MAVEN_GPG_PASSPHRASE` and `GPG_TTY`.
  3. `versions:set`, then verify, then commit `Release X.Y.Z`.
  4. A dry run with `-DskipPublishing=true`, then an `unzip -l` check and a `javap` major-version-52 check.
  5. Stage.
  6. Publish in the portal.
  7. Tag, push, set the next SNAPSHOT.
  8. Confirm on repo1.maven.org, not the portal.

## Acceptance
- [ ] Every Maven command that signs, deploys or verifies a release carries `-Prelease -pl jresolve-core -am`. `rg -n 'Prelease' RELEASING.md` shows no line without `-pl jresolve-core -am`. The one exception is the task-28 section, which describes the future whole-reactor command and labels it not for use while the gate holds.
- [ ] It has a one-off setup section. It covers the Central Portal user token in `~/.m2/settings.xml` under server id `central`, with a skeleton containing placeholders only. It covers publishing the GPG key to both keyservers, and checking it can be fetched.
- [ ] It has a bundle-listing check: the `-DskipPublishing=true` command, the `unzip -l` path, and the exact expected contents. Those are the parent pom, core's jar, sources jar, javadoc jar and pom, an `.asc` for each, and nothing from profiles-ie. It adds a `javap -v` check that the class files are major version 52.
- [ ] It has a pinentry and signing troubleshooting section: `GPG_TTY`, loopback pinentry, and `MAVEN_GPG_PASSPHRASE`.
- [ ] It marks each irreversible step (stage, Publish, tag push) as a stop-and-confirm point. It says `autoPublish=false` means staging uploads without releasing.
- [ ] It has a "Via CI" section saying `release.yml` is dormant while Actions is blocked. It says the workflow must be disabled before pushing a `v*` tag if Actions is live, or the upload is duplicated.
- [ ] It explains how task 28 lifts the gate: set `jresolve.aliasCorpus` to `sourced` in profiles-ie's pom only when a licensed corpus has replaced the tables, then drop `-pl`. It warns that `-Denforcer.skip` bypasses the gate and must never be used for a release.
- [ ] It states the 0.x API promise: minor versions may break the API before 1.0, and patch versions will not.
- [ ] It says a release is confirmed only by `repo1.maven.org/maven2/io/github/aindriub/jresolve-core/<v>/` existing, not by the portal returning 200.
- [ ] There are no credentials, real tokens or passphrases in it. `rg -in 'password|token' RELEASING.md` shows placeholders only.
- [ ] `mvn clean verify` still passes with the base figure unchanged. This task touches no code.

## Out of scope
- Running any release step, and changing the pom version.
- Editing `pom.xml`, `release.yml` or D19. If the runbook reveals a defect there, report it in the commit body.
- `DEVELOPING.md` (task 43) and `README.md` (task 41).

## Attempt 1 — failed

Review REQUEST CHANGES (commit cf1b9b7). Every other criterion met; the bundle
procedure, gate lift, CI note and troubleshooting were verified against the pom
and plugin 0.11.0 bytecode.

- **Next-snapshot step contradicts the 0.x promise.** `RELEASING.md:166-170`
  bumps to `X.Y.(Z+1)-SNAPSHOT` (`0.1.1-SNAPSHOT`); a breaking change would then
  ship as a patch, against `:279`. After 0.1.0 the next version is
  `0.2.0-SNAPSHOT`: bump the minor while on 0.x.
- **LICENSE/NOTICE-in-jar check cannot pass.** `:198`, `:203` came from
  irc-client. This repo has no NOTICE, and no pom packages LICENSE into the jar,
  so `unzip -l` of the real jar fails it. pom edits are out of scope: restate
  the check to match what the build actually produces, and name "LICENSE not in
  the jar" as a known gap in the commit body. Central does not require it.
- Also fix while there:
  - `:229-230`: in 0.11.0 the bundle lands under the *first* project defining
    the plugin, the root reactor, so it is `./target/central-publishing/central-bundle.zip`.
  - `:203-204`: `versions:set` does not touch README's dependency snippet. Add a
    step to update it (and check it) before the `Release X.Y.Z` commit, or drop
    the check.
  - `:151-153`: give one rule for when the tag is pushed. Recommended: after
    Publish is pressed and the portal shows PUBLISHING/PUBLISHED, before the
    snapshot bump.
- Continue on branch `task/44-releasing-runbook`.
