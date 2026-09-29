# 45 — Correct the pom's dry-run hint

**Repo:** .
**Depends on:** none
**Owns:**
- pom.xml (the `release` profile's leading comment only)

## Goal
The pom's release-profile comment tells readers that
`-DskipPublishing=true -Dgpg.skip` gives a dry run. It does not. In plugin
0.11.0, `-DskipPublishing` builds no bundle, and a SNAPSHOT is never bundled
either, so there is nothing to inspect. Replace the hint with a pointer to the
procedure that does work.

## Context
- pom.xml:306 — the line to replace: `(add -DskipPublishing=true -Dgpg.skip for a dry run that uploads nothing).` It sits in the comment above `<id>release</id>`.
- RELEASING.md:185 "Inspecting the bundle". Its subsections are "The class files" (:192) and "The jars" (:202), both offline checks, and "The staged upload (the real check)" (:216), which uploads with `autoPublish=false`, inspects in the portal, then Publishes or Drops.
- Task 38's review established the skipPublishing behaviour from the plugin 0.11.0 bytecode. Do not re-derive it.

## Acceptance
- [ ] `rg -n skipPublishing pom.xml` finds nothing, or only a line saying the flag builds no bundle.
- [ ] The comment names `RELEASING.md` ("Inspecting the bundle"). It says the real check is the staged upload with `autoPublish=false`, inspected in the portal, then Published or Dropped.
- [ ] Only comment text changes. No element, property or plugin configuration in pom.xml differs (`git diff pom.xml` shows changed lines inside the `<!-- -->` block only).
- [ ] `mvn clean verify` passes at 665 tests.
- [ ] `mvn -Prelease -Dgpg.skip -pl jresolve-core -am verify` passes.
- [ ] `mvn -Prelease -Dgpg.skip verify` still fails at profiles-ie with the D19 message.
- [ ] `git diff --stat` touches only pom.xml. The version stays `0.1.0-SNAPSHOT`.

## Out of scope
- Any deploy, staging or upload, and changing the version.
- Changing the central-publishing or gpg plugin configuration, and the profiles-ie gate.
- Editing RELEASING.md, `release.yml` or D19. If they are wrong, report it in the commit body.
