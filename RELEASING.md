# Releasing to Maven Central

The coordinates are `io.github.aindriub:jresolve-core` (parent
`io.github.aindriub:jresolve`). The `io.github.aindriub` namespace is verified
against the GitHub account, so there is no domain to prove.

This is adapted from irc-client's runbook, with four differences:

- **Core only.** `jresolve-profiles-ie` ships illustrative alias tables and is
  gated off by D19 (`docs/design-decisions.md#d19`). It is never released until
  task 28 lifts the gate.
- **A local deploy.** GitHub Actions is blocked by billing, so releases are made
  from a workstation. `release.yml` is dormant.
- **A manual Publish.** The upload only stages (`autoPublish` is `false`).
- **A bundle you can only inspect by staging.** See "Inspecting the bundle".

Every Maven command below that signs, deploys or verifies a release carries
`-Prelease -pl jresolve-core -am`. Do not drop the `-pl`: over the whole reactor
the gate fails by design.

Steps marked **STOP AND CONFIRM** are irreversible or use up publishing
allowance. Do not run one without an explicit go-ahead from the maintainer.

## One-off setup

1. **Central Portal account.** Register at https://central.sonatype.com and
   claim `io.github.aindriub` (a GitHub check, using the account that owns this
   repository).

2. **Credentials in `~/.m2/settings.xml`.** The portal issues a user token, not
   the account password. Generate one under your Central account, then, under
   server id `central` (the id the pom's `publishingServerId` names):

   ```xml
   <settings>
     <servers>
       <server>
         <id>central</id>
         <username>TOKEN_USERNAME</username>
         <password>TOKEN_PASSWORD</password>
       </server>
     </servers>
   </settings>
   ```

   The placeholders are the two halves of the portal token. `settings.xml`
   holds secrets and belongs in `~/.m2`, never in this repository. The GPG
   passphrase does **not** go in it.

3. **The signing key**, `5F3261B55F4BEAE3`. Publish it to both keyservers.
   They do not all sync reliably and Central validates against whichever it
   asks:

   ```bash
   gpg --keyserver keyserver.ubuntu.com --send-keys 5F3261B55F4BEAE3
   gpg --keyserver keys.openpgp.org     --send-keys 5F3261B55F4BEAE3
   ```

   keys.openpgp.org only serves a key's user id after the emailed
   verification link is followed; do that too.

   **Send it well before you need it**, then confirm it can be fetched from
   each (use a throwaway keyring so a local copy does not mask a miss):

   ```bash
   export GNUPGHOME="$(mktemp -d)"
   gpg --keyserver keyserver.ubuntu.com --recv-keys 5F3261B55F4BEAE3
   gpg --keyserver keys.openpgp.org     --recv-keys 5F3261B55F4BEAE3
   rm -rf "$GNUPGHOME"; unset GNUPGHOME
   ```

   Back up the key and its revocation certificate somewhere other than the
   machine that holds them.

## Signing

Before any signing command, in the shell you will run Maven from:

```bash
export GPG_TTY="$(tty)"
export MAVEN_GPG_PASSPHRASE='your passphrase'   # environment only
```

Never pass the passphrase with `-Dgpg.passphrase=`: it lands in shell history
and the process list. Select the key with `-Dgpg.keyname=5F3261B55F4BEAE3`
(not secret; without it gpg signs with its default key).

Signing produces an `.asc` for each of core's jar, sources jar, javadoc jar and
pom, and for the parent pom. Central rejects the upload if any is missing.

### Pinentry and signing troubleshooting

| Symptom | Cause and fix |
| --- | --- |
| `gpg: signing failed: Inappropriate ioctl for device` | gpg wants a pinentry prompt and has no terminal. Set `GPG_TTY="$(tty)"`, or use loopback pinentry (below). |
| A pinentry dialog appears on the wrong screen, or hangs over SSH or in a tool shell | Same cause. Set `GPG_TTY`, or use loopback and `MAVEN_GPG_PASSPHRASE`. |
| Maven ignores `MAVEN_GPG_PASSPHRASE` and still prompts | gpg is not in loopback mode. Add `allow-loopback-pinentry` to `~/.gnupg/gpg-agent.conf`, run `gpgconf --kill gpg-agent`, and make sure `MAVEN_GPG_PASSPHRASE` is exported in the same shell that runs `mvn`. The plugin then passes `--pinentry-mode loopback` itself. |
| `gpg: skipped "...": No secret key` | `gpg.keyname` matches no key in the keyring. Check `gpg --list-secret-keys --keyid-format=long`. |
| `gpg: signing failed: Bad passphrase` | Wrong passphrase, or a stale agent has cached an old one. `gpgconf --kill gpg-agent` and retry. |
| Upload rejected, key not found | The key has not reached the keyserver Central queried. Wait, and check both keyservers as in setup. |

## How often

Batched, not per change. Work accumulates on the `-SNAPSHOT` and goes out
together. A release cannot be withdrawn or replaced, every consumer must decide
about every version, and the Central Portal counts files per month (each
release publishes the jars, poms and a signature for each). Cut a release when
what is waiting in `docs/plan/PLAN.md` stops growing.

## Each release (local deploy)

Substitute the real version for `X.Y.Z` throughout. The first release is 0.1.0.

1. **Prerequisites.** The credentials, key, `GPG_TTY` and
   `MAVEN_GPG_PASSPHRASE` from above are in place; the working tree is clean
   on `main`; the JDK 17 toolchain is available
   (`docs/architecture.md#building`).

2. **Set the version, verify, commit.**

   ```bash
   mvn -B -q versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false
   git diff --stat                       # only pom.xml files, only the version
   mvn -B -Prelease -pl jresolve-core -am -Dgpg.keyname=5F3261B55F4BEAE3 clean verify
   git commit -am "Release X.Y.Z"
   ```

   `verify` under the release profile signs, so it also proves the key and
   passphrase work before anything is uploaded. Check the signatures:

   ```bash
   ls jresolve-core/target/*.asc
   gpg --verify jresolve-core/target/jresolve-core-X.Y.Z.jar.asc jresolve-core/target/jresolve-core-X.Y.Z.jar
   ```

3. **Inspect the bundle.** See the next section. Do not skip it.

4. **STOP AND CONFIRM: stage.** Uploading uses up publishing allowance and
   cannot be undone, only dropped. `autoPublish` is `false`, so this uploads
   the bundle for validation and does **not** release it:

   ```bash
   mvn -B -Prelease -pl jresolve-core -am -Dgpg.keyname=5F3261B55F4BEAE3 clean deploy
   ```

5. **STOP AND CONFIRM: Publish.** Open https://central.sonatype.com/publishing,
   wait for the deployment to reach `VALIDATED`, check its contents against the
   list below, then press **Publish** (or **Drop** to discard it). Publishing is
   irreversible: a released version can never be withdrawn or replaced.

6. **STOP AND CONFIRM: tag and push.** Only after the release is confirmed
   (step 8) if you want a tag to mean "this exists on Central"; at the latest
   right after Publish:

   ```bash
   git tag -a vX.Y.Z -m "Release X.Y.Z"
   git push && git push --tags
   ```

   If GitHub Actions is live, read "Via CI" first: the tag push triggers
   `release.yml`.

7. **Reopen development.**

   ```bash
   mvn -B -q versions:set -DnewVersion=X.Y.(Z+1)-SNAPSHOT -DgenerateBackupPoms=false
   git commit -am "Back to snapshot" && git push
   ```

   Write the real next number, for example `0.1.1-SNAPSHOT`.

8. **Confirm on Central itself.** A release is confirmed only when
   `https://repo1.maven.org/maven2/io/github/aindriub/jresolve-core/X.Y.Z/`
   exists and lists the files. The portal returning 200, or showing
   `PUBLISHED`, does not confirm it. Expect up to roughly 30 minutes; the
   search index takes hours longer.

## Inspecting the bundle

**`-DskipPublishing=true` does not give you a bundle.** It skips bundle
creation as well as the upload, and a `-SNAPSHOT` version is never bundled at
all. A "dry run" with it therefore shows nothing to inspect. (The pom's hint at
`pom.xml:306` suggesting it is wrong for this purpose.) Use these instead.

### The class files (works on any version, offline)

```bash
mvn -B -Prelease -pl jresolve-core -am -Dgpg.skip clean verify
javap -v -cp jresolve-core/target/classes io.github.aindriub.jresolve.evidence.MatchEvidence | grep 'major version'
```

Expect `major version: 52` (Java 8). Anything higher means consumers on Java 8
cannot load the jar; stop.

### The jars (works on any version, offline)

```bash
unzip -l jresolve-core/target/jresolve-core-X.Y.Z.jar          # classes, LICENSE, NOTICE
unzip -l jresolve-core/target/jresolve-core-X.Y.Z-sources.jar
unzip -l jresolve-core/target/jresolve-core-X.Y.Z-javadoc.jar
```

`LICENSE` and `NOTICE` must be inside the jar, and the README's dependency
snippet must show the version being released.

### The staged upload (the real check)

The bundle Central receives is what matters, and the reliable way to see it is
the real staged upload of step 4 (`autoPublish=false`), inspected in the
portal, then Publish or Drop. Staging uploads without releasing, so this is
safe as long as you do not press Publish. In the portal, the deployment's file
list must be exactly:

```
io/github/aindriub/jresolve/X.Y.Z/jresolve-X.Y.Z.pom
io/github/aindriub/jresolve-core/X.Y.Z/jresolve-core-X.Y.Z.jar
io/github/aindriub/jresolve-core/X.Y.Z/jresolve-core-X.Y.Z-sources.jar
io/github/aindriub/jresolve-core/X.Y.Z/jresolve-core-X.Y.Z-javadoc.jar
io/github/aindriub/jresolve-core/X.Y.Z/jresolve-core-X.Y.Z.pom
```

That is the parent pom plus core's jar, sources jar, javadoc jar and pom, with
an `.asc` for each of the five (plus `.md5`, `.sha1`, `.sha256`, `.sha512`
checksums). **Nothing from `jresolve-profiles-ie` may appear.** If anything is
missing, extra or unsigned, press **Drop**, fix, and stage again. Dropping
costs an upload but no release.

The bundle zip is also left on disk after the deploy; locate and list it with
(the plugin's output directory is `target/central-publishing/`, in the module
that ran the upload):

```bash
find . -path '*central-publishing*' -name '*.zip'
unzip -l <that zip>
```

The same listing, without uploading, was produced for task 38 (commit
`421fc8a`) on a scratch copy at version 0.1.0 with `-Dgpg.skip` (so no `.asc`)
and the upload pointed at a dead `centralBaseUrl` so it failed after the bundle
was built. That is a workaround for inspecting the shape only; it proves
nothing about signatures, and it must not be run on the real tree at a release
version with real credentials in `settings.xml` and a working URL.

## Via CI

`.github/workflows/release.yml` is **dormant** while GitHub Actions is blocked
by billing; the release is made locally as above. It is core-only and every
Maven invocation in it carries `-Prelease -pl jresolve-core -am`.

**If Actions is live again, disable the workflow before pushing a `v*` tag.**
The tag push triggers it, and it would upload a second time a release that was
already made by hand, duplicating the upload. (`workflow_dispatch` with
`dry_run` builds and signs without uploading.) To use CI deliberately instead,
skip steps 4 and 5 of the local deploy, add repository secrets for the key
(`GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`) and the portal token
(`CENTRAL_TOKEN_USERNAME`, `CENTRAL_TOKEN_PASSWORD`) as the workflow expects,
and push the tag. The upload still only stages; Publish is still manual.

## Lifting the D19 gate (task 28)

Under the release profile, `jresolve-profiles-ie` fails the build unless its
pom sets `jresolve.aliasCorpus` to `sourced`. Set that **only** when task 28 has
replaced the illustrative alias tables with a licensed, sourced corpus. Then
the whole reactor can be released, and `-pl` dropped. That future command, not
for use while the gate holds:

```bash
mvn -B -Prelease -Dgpg.keyname=5F3261B55F4BEAE3 clean deploy
```

The pom's `excludeArtifacts` entry for profiles-ie must be removed in the same
change, or it would still be left out of the bundle.

**Never use `-Denforcer.skip` (or `-Denforcer.fail=false`) on a release.** It
bypasses the gate and would publish the illustrative tables, irreversibly.

## What the version number promises

While the version is 0.x, minor versions (0.1 to 0.2) may break the API and
patch versions (0.1.0 to 0.1.1) will not. From 1.0.0, semantic versioning
applies: breaking changes need a new major version, so anything that changes a
public signature or documented behaviour belongs before that release.
