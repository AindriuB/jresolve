# 01 — Stand up the Maven multi-module build

**Repo:** .
**Depends on:** none
**Owns:**
- pom.xml
- .gitignore
- jresolve-core/pom.xml
- jresolve-core/src/test/java/io/github/aindriub/jresolve/BuildSmokeTest.java
- jresolve-profiles-ie/**

## Goal
Create the reactor build that every later task compiles against: a `pom` root
with two modules, JDK 17 toolchain, `maven.compiler.release=8`, surefire, JUnit 5
and AssertJ on the test classpath, and animal-sniffer against the `java18`
signature bound to `verify`. Nothing else in this milestone can compile until
this lands, so it is wave 1 on its own.

## Context
- docs/design-decisions.md#d14 — build on JDK 17 pinned by maven-toolchains, `release=8`; building on JDK 8 is unsupported and the build must say so rather than fail obscurely.
- docs/architecture.md#modules — coordinates `io.github.aindriub:jresolve-core` and `io.github.aindriub:jresolve-profiles-ie`, base package `io.github.aindriub.jresolve`; core depends on the JDK and nothing else at runtime.
- docs/conventions.md#java-8-project — the language/API subset the reviewer greps for.
- docs/conventions.md#logs-kit — `logs/` and `*.log` are gitignored.
- docs/spec/original-design.md §101 (Maven quality requirements), §102 (dependency policy) — read those two sections only.

## Acceptance
- [ ] Root `pom.xml` is `<packaging>pom</packaging>`, groupId `io.github.aindriub`, and lists modules `jresolve-core` and `jresolve-profiles-ie` in that order.
- [ ] `maven.compiler.release` is `8` and `project.build.sourceEncoding` is `UTF-8`, both set once in the root pom.
- [ ] `maven-toolchains-plugin` requires a JDK 17 toolchain. If no `toolchains.xml` exists in this environment, the implementer creates one at `${user.home}/.m2/toolchains.xml` for the installed JDK 17 and says so in the close-out; the pom is not weakened to make the build pass without a toolchain.
- [ ] `maven-enforcer-plugin` fails the build on a Maven JDK older than 9, with a message naming JDK 17 as the supported build JDK.
- [ ] `animal-sniffer-maven-plugin` runs `check` against `org.codehaus.mojo.signature:java18` in the `verify` phase for both modules, and is not skippable by a property.
- [ ] `jresolve-core` has no `compile` or `runtime` scoped dependency: `mvn -pl jresolve-core dependency:list -DincludeScope=runtime` lists none.
- [ ] JUnit 5 (`junit-jupiter`) and AssertJ are `test` scope, declared once in the root `dependencyManagement`, and surefire is configured to run `*Test` classes.
- [ ] `jresolve-profiles-ie` is a jar module depending on `jresolve-core`, with `src/main/java/io/github/aindriub/jresolve/profiles/ie/` present (a `package-info.java` only) and no other source.
- [ ] `BuildSmokeTest` contains one JUnit 5 test using an AssertJ assertion, proving both are wired.
- [ ] `mvn -q clean verify` succeeds from a clean checkout, and the implementer reports the observed command and its exit status.
- [ ] `.gitignore` covers `target/`, `logs/`, `*.log`, and IDE directories.

## Out of scope
- Any `src/main/java` under `jresolve-core` — every later task owns its own package subtree.
- The `jresolve-benchmarks` module, JMH, javadoc/source/gpg release plugins, CI configuration.
- Checkstyle, SpotBugs, JaCoCo — not required by this milestone; do not add a gate later tasks must then satisfy.
- Any content in `jresolve-profiles-ie` beyond the empty package.

## Attempt 1 — failed

Tester PASS, reviewer CHANGES. The build works; the gate it installs is
bypassable, which is the one thing this task existed to prevent.

**Defect — must fix.** `pom.xml` leaves animal-sniffer's own
`animal.sniffer.skip` user property live, so `mvn verify
-Danimal.sniffer.skip=true` silently skips the `java18` check. The acceptance
list requires the check not be skippable by a property. Add `<skip>false</skip>`
inside the plugin's existing `<configuration>` and re-verify that passing the
property no longer skips it.

**Suggestion — take it unless there is a reason not to.** The toolchain
requirement pins `<vendor>openjdk</vendor>`. That matches the `toolchains.xml`
written for this machine, but it fails against one whose vendor string reads
`temurin` or `zulu`, which is what a second developer is likely to have.
Dropping the `<vendor>` element keeps the JDK 17 requirement without the
brittleness.

**Measured, and worth keeping.** The tester found that with
`maven.compiler.release=8`, javac's own `--release` check rejects a post-Java-8
API before animal-sniffer ever runs — proving sniffer non-vacuous needed
`release` temporarily raised to 11 to let the probe compile. Animal-sniffer then
failed correctly: `Undefined reference: java.util.List java.util.List.of(Object,
Object)`. Both gates work, and they are genuinely redundant for this class of
violation. Keep both anyway: `release=8` is what someone weakens to unblock
themselves in a hurry, and sniffer is what catches it when they do.

Also confirmed by measurement, so no later task needs to re-derive it: breaking
the `toolchains.xml` version makes the build fail with `Cannot find matching
toolchain definitions`, not fall back to Maven's JDK 25; and
`dependency:list -DincludeScope=runtime` on core resolves nothing.
