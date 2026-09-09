# History

Append-only, newest first. See `docs/plan/HISTORY-INDEX.md` for a grep-first
index — do not load this file whole.

## 2026-09-09 — Similarity metrics land (task 02)

`jresolve-core` now has a `comparison/` package: `SimilarityMetric`, and three
implementations — `JaroWinklerSimilarity`, `LevenshteinSimilarity` and
`TokenSimilarity` (a delegate-plus-splitter best-match-mean over tokens). All
three are pure functions of two strings, treat `null` as empty, and are bounded
in `[0,1]` and exactly symmetric, each backed by a property test over a
20-plus-pair synthetic corpus. A `TokenSplitter` functional interface also
landed, used today only by a test lambda.

**Cost:** Took two attempts. Attempt 1 tested PASS — 29 tests, a full mutation
battery, hand-computed values agreeing — and was rejected in review anyway:
`TokenSimilarityTest.weightsBestMatchesBySideTokenCounts` derived its expected
value by calling `JaroWinklerSimilarity` at test time and then asserting the
code against its own output, so it could not detect a regression in the
delegate it was supposed to be exercising. Attempt 2 rewrote it against a
hand-derived expectation — a token pair where Jaro-Winkler is exactly 0.0
because no characters fall within the match window — and proved the fix by
perturbing the delegate and watching the test fail where it had previously
passed. The general lesson recurred in both 03 and 04 the same wave and is
worth restating here: a green suite is evidence about the tests, not about the
code, until something has been broken on purpose and observed to fail.

`TokenSplitter` was not named in the task file but was accepted after a
cross-branch check against 03's concurrent work: `normalization/` is
`String`-to-`String` throughout with no tokenising abstraction to collide
with. Nothing in 02's main sources implements `TokenSplitter` yet — only a
test lambda does — so task 05 is where a second splitter could be invented
independently; noted directly in `docs/plan/tasks/05-field-layer.md` so it
isn't rediscovered.

Two constructor guards added to `JaroWinklerSimilarity` in attempt 2 (prefix
scale bounds) have no automated test covering them — the tester verified them
by hand with a scratch class — so deleting either guard would survive the
suite unnoticed; worth a property or unit test in a later pass. `mvn clean
verify` now runs roughly 9-13 seconds from clean, up from 7-8 at task 01's
close.

## 2026-09-09 — Maven multi-module build skeleton lands (task 01)

The repo now has a real reactor build: a root `pom` with `jresolve-core` and
`jresolve-profiles-ie` modules, a JDK 17 toolchain requirement, `maven.compiler.release=8`,
JUnit 5 + AssertJ on the test classpath via root `dependencyManagement`, and
`animal-sniffer-maven-plugin` checking both modules against `java18` in
`verify`. `jresolve-core` has no compile/runtime dependency; `jresolve-profiles-ie`
depends on it and holds only an empty `io.github.aindriub.jresolve.profiles.ie`
package. `BuildSmokeTest` proves JUnit 5 and AssertJ are wired. `mvn clean
verify` runs green from a clean checkout in roughly 7-8 seconds, which is the
baseline for later tasks — expect it to grow.

**Cost:** Took two attempts. Attempt 1 tested PASS but was rejected in review:
it left animal-sniffer's `animal.sniffer.skip` user property live, so `mvn
verify -Danimal.sniffer.skip=true` silently bypassed the java18 check — the
exact gate this task existed to install. A `-D` on the command line can
override a POM value expressed through the property's own expression, but
cannot override an explicit `<skip>false</skip>` written directly in the
plugin's `<configuration>`; that is the fix, and it is the only way to make
the gate genuinely non-skippable. Attempt 1 also pinned
`<vendor>openjdk</vendor>` on the toolchain requirement, which would fail
against a `toolchains.xml` reading `temurin` or `zulu` (what a second
developer is likely to have installed) — dropped in attempt 2. The diff
between attempts was four lines.

Proving animal-sniffer non-vacuous needed `maven.compiler.release` temporarily
raised from 8 to 11: at `release=8`, javac's own `--release` check rejects a
post-Java-8 API before the sniffer ever runs, so the two gates are redundant
for this class of violation. Both are kept anyway — `release=8` is what
someone loosens to unblock themselves in a hurry, and the sniffer is what
catches them doing it when they do.

Also measured and worth not re-deriving: breaking the `toolchains.xml` version
makes the build fail with "Cannot find matching toolchain definitions" rather
than silently falling back to whichever JDK is running Maven (25 here); and
`mvn -pl jresolve-core dependency:list -DincludeScope=runtime` resolves
nothing, confirming core's JDK-only constraint by measurement rather than
inspection.

The build needs a JDK 17 toolchain that is not part of the repo — Temurin
17.0.20.1 at `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot` on
this machine, with `C:\Users\aindr\.m2\toolchains.xml` pointing at it. Anyone
else building this needs their own `toolchains.xml`; documented in
`docs/architecture.md#building`. Maven 3.8.6 itself runs on JDK 25 — only
compilation is pinned to 17.
