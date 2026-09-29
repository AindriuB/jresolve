# Developing jresolve

This is the guide from a fresh clone to a green `mvn clean verify`, and from
there to a new comparator or a new profile module. A build gate here fails with
a message that only makes sense once you know the rule, so each gate below is
quoted by its real failure text.

Releasing is a different job: see [RELEASING.md](RELEASING.md).

## Getting to a green build

You need Maven 3.9 or newer, running on JDK 9 or newer, and a separate JDK 17
that the build uses as its compiler. The library targets Java 8 (`--release 8`),
but JDK 8 is unsupported for building.

The compiler JDK is pinned by `maven-toolchains-plugin`, and the pin needs a
file that is deliberately not in the repo. Create `~/.m2/toolchains.xml`, with
the real path of your JDK 17 in `jdkHome`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains xmlns="http://maven.apache.org/TOOLCHAINS/1.1.0"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/TOOLCHAINS/1.1.0 http://maven.apache.org/xsd/toolchains-1.1.0.xsd">
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>17</version>
    </provides>
    <configuration>
      <jdkHome>/path/to/jdk-17</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

Without it, the build stops before compiling anything:

```
[ERROR] No toolchain found for type jdk
[ERROR] Cannot find matching toolchain definitions for the following toolchain types:
jdk [ version='17' ]
[ERROR] Please make sure you define the required toolchains in your ~/.m2/toolchains.xml file.
```

The version comes from `toolchain.jdk.version` in the root `pom.xml`. Then:

```
mvn clean verify
```

On the `main` this guide was written against, that runs 649 tests (568 core plus
81 profiles). CI provisions the same file in
[`.github/workflows/build.yml`](.github/workflows/build.yml), which is a worked
example.

## Module layout

| Module | May depend on | Holds |
|---|---|---|
| `jresolve-core` | JDK 8 API only | The engine: `FieldPipeline`, `SimilarityMetric`, `ComparisonCategory`, comparators, scorers, decision engines. Generic. |
| `jresolve-profiles-ie` | `jresolve-core` | The Irish profile: name and address pipelines, alias tables. |

The direction is one way. Core never depends on a profile, and the reactor
enforces it: a profile dependency in core's pom is a cycle, and Maven refuses to
build. See [docs/architecture.md](docs/architecture.md) for the type model.

**The vocabulary rule.** `jresolve-core` may not name a domain concept: a
person, a name of any kind, an address, a date of birth, a country. Not in
identifiers, not in Javadoc, not in tests. A profile module exists so that the
domain words have somewhere to live. `DomainVocabularyTest` checks it, below.

## The build gates

Six checks fail a build for reasons that are easy to trigger and hard to read.

### Toolchain

Covered above. The failure is "Cannot find matching toolchain definitions", and
the fix is `~/.m2/toolchains.xml`.

### The Maven-version enforcer rule

`maven-enforcer-plugin` requires that Maven itself runs on JDK 9 or newer,
because the `--release` flag used to compile to Java 8 does not exist before
JDK 9. The compiler is the JDK 17 toolchain, a separate thing from the JDK
running Maven. Running Maven on JDK 8 fails with:

```
[ERROR] Rule 0: org.apache.maven.enforcer.rules.version.RequireJavaVersion failed with message:
[ERROR] jresolve is built with the JDK 17 toolchain pinned by maven-toolchains-plugin.
[ERROR]                     Maven itself must run on JDK 9 or newer (JDK 25 or JDK 17 are both fine) because
[ERROR]                     the `--release` flag used to compile to Java 8 does not exist before JDK 9.
[ERROR]                     Building directly on JDK 8 is unsupported.
```

Fix: point `JAVA_HOME` at a JDK 9 or newer. Do not lower the version range.

### animal-sniffer

`animal-sniffer-maven-plugin` checks compiled main classes against the Java 8
API signature (`java18`) in the `verify` phase. `--release 8` already rejects
most newer API at compile time, so the sniffer is the second net, and it fires
when something gets past the compiler. A call to `String.isBlank()`, added to
main code with the release flag overridden, produces:

```
[ERROR] .../CostTiers.java:20: Undefined reference: boolean String.isBlank()
[ERROR] Failed to execute goal org.codehaus.mojo:animal-sniffer-maven-plugin:1.23:check (animal-sniffer-check) on project jresolve-core: Signature errors found. Verify them and ignore them with the proper annotation if needed.
```

Under the normal `--release 8` you meet the compiler's own version of it first:
`error: cannot find symbol ... method isBlank()`. Both mean the same. Fix:
use the Java 8 spelling. The list of API to avoid is in
[docs/conventions.md](docs/conventions.md#java-8-project).

### doclint

Javadoc is built and checked on every `verify`, for main and test sources, at
`private` visibility, with `all,-missing` and warnings as failures. Its purpose
is the stale `{@link}`: a reference to something that was renamed. A link to a
type that no longer exists fails with:

```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-javadoc-plugin:3.6.3:javadoc-no-fork (javadoc-doclint) on project jresolve-core: An error has occurred in Javadoc report generation:
[ERROR] Exit code: 1
[ERROR] .../CostTiers.java:20: error: reference not found
[ERROR]     /** See {@link NoSuchThing}. */
[ERROR]                    ^
```

Fix: correct or remove the reference. A rename that leaves `{@link Old}` behind
in a private method's Javadoc, or in a test's, is the usual cause. The
`missing` group is switched off on purpose, so absent `@param` tags do not fail.

### DomainVocabularyTest

`jresolve-core/src/test/java/io/github/aindriub/jresolve/DomainVocabularyTest.java`
reads every `.java` file under core's `src/main` and `src/test` and fails on a
domain word. A stray `address` in a variable name gives:

```
[ERROR]   DomainVocabularyTest.coreNamesNoDomainConcept:<line> [docs/conventions.md forbids domain vocabulary in jresolve-core]
Expecting empty but was: ["src/main/java/io/github/aindriub/jresolve/field/CostTiers.java:20 — address"]
```

Each entry is `file:line — the matched text`. The line numbers after the test
names in these quotes vary with the build and the test file; do not rely on them. Fix: rename to a neutral word
(`label`, `code`, `reference`) or move the code to a profile module. The test
matches text, not meaning, so it cannot tell a domain concept from an English
word that looks like one, and a domain concept in words its list lacks passes.
A green run is necessary and not sufficient. The reviewer is the other half.

### ModuleBoundaryTest

`jresolve-profiles-ie/src/test/java/io/github/aindriub/jresolve/profiles/ie/ModuleBoundaryTest.java`
holds the profile module to its own subtree of the base package, and checks that
what it publishes is expressed in core's types. A profile class that ends up
outside `io.github.aindriub.jresolve.profiles.ie` fails with:

```
[ERROR]   ModuleBoundaryTest.thisModuleLivesUnderItsOwnSubtree:<line>
Expecting actual:
  "io.github.aindriub.jresolve.field.IrishAddressComparator"
to start with:
  "io.github.aindriub.jresolve.profiles.ie."
```

Fix: put the class back under the module's package. The other half of the
boundary, that core does not reference the profile, is held by the reactor
rather than a test. A dependency from core to the profile fails at project
loading, before any compiler runs:

```
[ERROR] The projects in the reactor contain a cyclic reference: Edge between 'Vertex{label='io.github.aindriub:jresolve-profiles-ie:0.1.0-SNAPSHOT'}' and 'Vertex{label='io.github.aindriub:jresolve-core:0.1.0-SNAPSHOT'}' introduces to cycle in the graph ...
```

Fix: remove the dependency. If core needs something a profile has, the thing
belongs in core, expressed without domain words.

### The D19 release gate

`jresolve-profiles-ie` still ships illustrative alias tables, so its pom refuses
to release under the `release` profile. An ordinary `mvn verify` never sees it.
`mvn -Prelease verify` over the whole reactor fails, on purpose:

```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-enforcer-plugin:3.4.1:enforce (enforce-alias-corpus-sourced) on project jresolve-profiles-ie:
[ERROR] Rule 0: org.apache.maven.enforcer.rules.property.RequireProperty failed with message:
[ERROR] D19 release gate: jresolve-profiles-ie still ships illustrative alias tables (jresolve.aliasCorpus must be "sourced", see task 28). Do not release it. Release core only, with: mvn -Prelease -pl jresolve-core -am clean deploy
```

Fix: you do not fix it in a feature branch. It clears when a sourced corpus
replaces the tables (task 28) and `jresolve.aliasCorpus` becomes `sourced`.
Until then, restrict a release build to core with `-pl jresolve-core -am`, as
the message says. [RELEASING.md](RELEASING.md) has the steps.

## Test conventions

The rules are in [docs/conventions.md](docs/conventions.md#tests). In short:

- JUnit 5 and AssertJ. Test names read as sentences: `missingOnOneSideIsNotAConflict()`.
- One behaviour per test. A name that needs "and" is two tests.
- Fixtures are synthetic. No real personal data in any file, and none in core's
  tests may use domain vocabulary either. When a fixture states its data is
  invented, use the sanctioned wording "none describes anyone real"; the
  natural phrasing with "names a real person" trips `DomainVocabularyTest`.
- An invisible character (non-breaking space, zero-width, combining mark) is
  written as an escape such as `\u00A0`, never pasted raw.
- No sleeps, no ordering between tests, no shared mutable static. Hand-calculate
  scoring expectations in a comment; never assert a value copied from a run.

## Writing a comparator

A `FieldComparator<N>` turns two prepared values into a `FieldEvidence`: a
category, an optional similarity, and an optional frequency key. Extend
`AbstractNullSafeFieldComparator<N>` and implement `compareNonNull`, and the
null handling is inherited. It is public so that code outside `field/`, in a
profile module or in a consumer's own project, can do exactly this.

```java
package com.example.profile;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.DefaultFieldEvidence;
import io.github.aindriub.jresolve.evidence.FieldEvidence;
import io.github.aindriub.jresolve.field.AbstractNullSafeFieldComparator;
import io.github.aindriub.jresolve.field.SimilarityBands;

/** Scores two codes by how much of their longer one they share as a prefix. */
public final class SharedPrefixComparator extends AbstractNullSafeFieldComparator<String> {

    private final SimilarityBands bands;

    public SharedPrefixComparator() {
        this(new SimilarityBands());
    }

    public SharedPrefixComparator(SimilarityBands bands) {
        if (bands == null) {
            // Names the parameter, never a value.
            throw new IllegalArgumentException("bands must not be null");
        }
        this.bands = bands;
    }

    @Override
    protected FieldEvidence compareNonNull(String left, String right) {
        if (left.equals(right)) {
            return new DefaultFieldEvidence(ComparisonCategory.EXACT, 1.0, null);
        }
        int limit = Math.min(left.length(), right.length());
        int shared = 0;
        while (shared < limit && left.charAt(shared) == right.charAt(shared)) {
            shared++;
        }
        double similarity = (double) shared / Math.max(left.length(), right.length());
        return new DefaultFieldEvidence(bands.categoryFor(similarity), similarity, null);
    }
}
```

The rules a comparator lives by:

- **Null is data.** Both null is `MISSING_BOTH` and exactly one null is
  `MISSING_ONE`. The base class applies this in a `final compare`, so
  `compareNonNull` never sees a null and must not re-check for one. A comparator
  that throws `NullPointerException` on a missing value is defective.
- **No field value in any message.** Not in an exception message, a
  `toString()`, or a log line. Name the field or the parameter; values are
  personal data in a profile's hands. This is why `DefaultFieldEvidence`
  leaves `frequencyKey` out of its `toString()`.
- **Use the shared bands.** `SimilarityBands.categoryFor` has inclusive lower
  bounds, and a hand-written cut-off differs from it exactly at the boundary.

Using the comparator with a scorer has one sharp edge, added in task 36. A
`RuleBasedScorer` field that weights a partial-agreement category (`HIGH`,
`MEDIUM`, `ALIAS_*`, `SUBSUMED` and so on) without an `EXACT` weight or a
`defaultWeight` would score a perfect match as zero, so `build()` rejects it
with an `IllegalArgumentException` naming the field. A band-only config is
refused; weight `EXACT`, at `0.0` if that is what you mean.

```java
package com.example.profile;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.scoring.RuleBasedScorer;

public final class SharedPrefixDemo {

    public static void main(String[] args) {
        SharedPrefixComparator comparator = new SharedPrefixComparator();

        check(comparator.compare(null, null).getCategory() == ComparisonCategory.MISSING_BOTH);
        check(comparator.compare(null, "AB12").getCategory() == ComparisonCategory.MISSING_ONE);
        check(comparator.compare("AB12", "AB12").getCategory() == ComparisonCategory.EXACT);
        check(comparator.compare("AB12", "AB19").getSimilarity() == 0.75);

        try {
            RuleBasedScorer.builder().weight("code", ComparisonCategory.HIGH, 3.0).build();
            throw new IllegalStateException("a band-only field should have been rejected");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("code"));
        }

        RuleBasedScorer accepted = RuleBasedScorer.builder()
                .weight("code", ComparisonCategory.EXACT, 5.0)
                .weight("code", ComparisonCategory.HIGH, 3.0)
                .build();
        check(accepted != null);
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new IllegalStateException("example no longer behaves as the guide says");
        }
    }
}
```

## Writing a profile module

A profile module packages domain knowledge (pipelines, alias tables,
domain-shaped comparators) on top of core. The reference is
`jresolve-profiles-ie/`; read it rather than copying from here.

**The pom shape.** Copy `jresolve-profiles-ie/pom.xml`: the root `jresolve` as
parent, its own `artifactId`, and a `compile` dependency on `jresolve-core` at
`${project.version}`. JUnit and AssertJ come in at `test` scope, versions from
the parent. Add the module to the `<modules>` list in the root `pom.xml`. If the
module ships data that is not yet fit to release, the `release` profile in that
pom shows how to make `-Prelease` refuse it.

**The dependency direction.** The profile depends on core, and core depends on
nothing of yours. Publish everything in core's types (`FieldPipeline`,
`AliasRepository`, a comparator extending `AbstractNullSafeFieldComparator`) so
that a consumer holding only core can use what you build.

**One subtree.** The module owns one package under the base package, for
example `io.github.aindriub.jresolve.profiles.ie`, and declares it in a
`package-info.java`. No class goes in core's packages.

**A boundary test.** Copy the idea of
`jresolve-profiles-ie/src/test/java/io/github/aindriub/jresolve/profiles/ie/ModuleBoundaryTest.java`:
your classes live under your subtree and core's do not. For the module above:

```java
package com.example.profile;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aindriub.jresolve.field.AbstractNullSafeFieldComparator;
import org.junit.jupiter.api.Test;

class ProfileBoundaryTest {

    @Test
    void theProfileLivesUnderItsOwnSubtree() {
        assertThat(SharedPrefixComparator.class.getName()).startsWith("com.example.profile.");
    }

    @Test
    void coreTypesAreNotInTheProfileSubtree() {
        assertThat(AbstractNullSafeFieldComparator.class.getName())
                .doesNotStartWith("com.example.profile.");
    }

    @Test
    void theProfileComparatorIsUsableAsACoreType() {
        assertThat(new SharedPrefixComparator()).isInstanceOf(AbstractNullSafeFieldComparator.class);
    }
}
```

That test cannot prove core never references the profile; the reactor does, as
described under `ModuleBoundaryTest`.

**Where domain vocabulary is allowed.** In the profile module, everywhere:
identifiers, Javadoc, tests, alias tables. That is its purpose, and
`DomainVocabularyTest` reads core only. It is not allowed in core, ever, and
the rest of `docs/conventions.md` (synthetic fixtures, no real data) binds the
profile's tests just the same.
