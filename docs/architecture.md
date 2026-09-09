# Architecture

jresolve is a library, not an application. It compares two Java objects it knows
nothing about, produces field-level evidence, scores that evidence under a
configurable model, and returns a ranked decision with the evidence attached.

The one constraint everything else follows from:

> The resolver must know how to compare evidence, but must not know what the
> entities represent.

The second: **Java 8 is the compilation and runtime target**, enforced by the
build rather than by discipline.

## Modules

| Module | Artifact | Depends on | Owns |
|---|---|---|---|
| `jresolve-core` | `io.github.aindriub:jresolve-core` | JDK 8 only | the generic engine — extraction, normalization, comparison, evidence, blocking, scoring, decision, explanation |
| `jresolve-profiles-ie` | `io.github.aindriub:jresolve-profiles-ie` | core | Irish/English name and address profiles, alias data, phonetics |
| `jresolve-benchmarks` | not published | core, profiles, JMH | JMH harnesses, candidate-reduction measurement |

The split exists because the original specification asked for a library with no
domain knowledge and then put `IrishNameProfile` and `AddressComparator` in the
core package tree. Irish name handling is domain knowledge; it is not
application knowledge, but it still ships and versions separately from the
engine. A consumer resolving invoices against suppliers depends on core alone.

**Core contains no domain vocabulary.** No class, field or Javadoc in
`jresolve-core` may name a person, a name, an address, a date of birth or a
country. `FieldPipeline`, `SimilarityMetric` and `ComparisonCategory` are
generic; `irishName()` lives in the profiles module. This is checkable in a diff
and the reviewer checks it.

## Building

The build compiles on JDK 17, pinned by `maven-toolchains-plugin`; JDK 8 is
unsupported for building even though 8 is the compilation target. This needs a
`toolchains.xml` on the machine — it is not part of the repo and does not
arrive with a clone. Create `${user.home}/.m2/toolchains.xml` naming an
installed JDK 17 before running `mvn verify`, for example:

```xml
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides><version>17</version></provides>
    <configuration><jdkHome>/path/to/jdk-17</jdkHome></configuration>
  </toolchain>
</toolchains>
```

Without a matching `toolchains.xml`, the build fails with "Cannot find
matching toolchain definitions" rather than silently falling back to whatever
JDK is running Maven. Maven itself can run on any JDK 9+ (enforced by
`maven-enforcer-plugin`); only compilation is pinned to 17.

## The pipeline

```
                     S                          C
                     |                          |
        prepare (extract + normalize)   prepare, once per candidate
                     |                          |
                     +------------ compare -----+
                                   |
                             MatchEvidence          per field: category,
                                   |                similarity, features
                       +-----------+-----------+
                       |           |           |
                    rules   Fellegi-Sunter   logistic
                       |           |           |
                       +-----------+-----------+
                                   |
                          Score (value + scale)
                                   |
                            decision engine        thresholds + margin
                                   |
                             MatchResult<C>
```

Every arrow is a boundary between concepts that must not collapse into each
other. A Jaro-Winkler score of 0.96 is a *feature*. A Fellegi-Sunter weight of
15 is a *log likelihood ratio*. Neither is a probability, and the type system
says so: see `Score` and `ScoreScale` below.

## The type model

Six decisions shape everything. `docs/design-decisions.md` carries the argument
for each; this is the shape they produce.

**Preparation is hoisted out of the candidate loop.** A pipeline has two halves:

```java
public interface FieldPipeline<V, N> {
    N prepare(V value);                     // extract-side: Unicode, case, tokens
    FieldEvidence compare(N left, N right); // hot loop
}
```

The resolver prepares the source once per `resolve()` call, not once per
candidate, and a `CandidateIndex` may hold candidates already prepared. The
original design normalized both sides inside `compare`, which re-derives the
source's normal form once for every candidate examined.

**Source and candidate may have different types.** `FieldDefinition<S, C, N>`
holds `Function<S, N>` and `Function<C, N>` — two extractors converging on one
normalized type — rather than requiring both sides to expose the same `V`. Real
integrations have `String fullName` on one side and `firstName`/`lastName` on
the other.

**`ComparisonCategory` is an interned value object, not an enum.** The standard
set (`EXACT`, `ALIAS_TRANSLATION`, `ALIAS_NICKNAME`, `ALIAS_VARIANT`,
`VERY_HIGH`, `HIGH`, `MEDIUM`, `LOW`, `CONFLICT`, `MISSING_ONE`, `MISSING_BOTH`)
is exposed as constants, but a new comparator can mint its own without a core
change. Categories are the keys of every scoring model, so a closed enum would
make the scoring layer closed too.

`MISSING_ONE` and `MISSING_BOTH` are separate because they carry entirely
different evidence: one side absent is weak negative evidence, both absent is
none at all, and a single `MISSING` category cannot express both in a
Fellegi-Sunter model.

**A score knows its own scale.**

```java
public final class Score {
    double value;
    ScoreScale scale;     // LOG2_LIKELIHOOD_RATIO | PROBABILITY | POINTS
    Double probability;   // null unless the model produces one
}
```

Thresholds and margins are meaningless without the scale — a margin of 0.02 is
enormous at p=0.99 and negligible at p=0.5. The builder validates that the
decision engine's thresholds were expressed on the scorer's scale, and fails at
`build()` when they were not.

**Comparison is cost-ordered and can short-circuit.** Each field declares a cost
tier. The resolver compares cheap tiers first and evaluates hard
`CandidateRule`s between tiers, so a candidate whose date of birth conflicts
never pays for address tokenisation. Evidence produced this way is marked
incomplete, and a scorer that requires every field rejects it rather than
scoring a partial record silently.

**Aliases are equivalence groups, not directed pairs.** `AliasRepository`
answers "what is equivalent to this normalized value" and the underlying data is
closed transitively at construction. `Robert` to `Bob` and `Robert` to `Bobby`
must make `Bob` to `Bobby` an alias too, and a directed `findAliases(canonical)`
lookup cannot say which of a pair is the canonical one. Alias strength is not
stored in the data; the *kind* of alias is a comparison category and the scoring
model assigns its weight.

## Boundaries that must not be crossed

- `jresolve-core` depends on the JDK and nothing else at runtime. Test-scope
  dependencies are unrestricted.
- No core class names a domain concept (see above).
- No `java.util.logging`, SLF4J, or `System.out` anywhere in main sources.
  Diagnostics leave through result objects.
- **No personal value reaches a log, an exception message, a `toString()`, or an
  explanation string.** Explanations are value-free templates. Values appear
  only when a consumer explicitly opts into diagnostics, and that flag is not on
  by default.
- Nothing in main sources uses a post-Java-8 API. `mvn verify` runs
  animal-sniffer against the `java18` signature and fails the build.
- The resolver retains no source or candidate object after `resolve()` returns.
  A `CandidateIndex` is the deliberate exception: retention is its purpose.

## Non-goals

Deliberately out of scope, so nobody adds them by inference: Spring, JSON,
persistence, HTTP, model training, and **one-to-one assignment**. jresolve
resolves one source against candidates independently; nothing stops two sources
claiming the same candidate. Global assignment is the consuming application's
problem.

## Decisions worth knowing

One line each; the full argument is in `docs/design-decisions.md`.

- 2026-09-09 — Split `core` from `profiles-ie` rather than one artifact.
  Rejected: a single jar with an `ie` package, which would have let core code
  reference Irish helpers with nothing to catch it.
- 2026-09-09 — Two-phase `FieldPipeline` (`prepare` / `compare`). Rejected:
  memoising normalization behind a cache, which fixes the cost but not the
  shape, and adds a correctness surface.
- 2026-09-09 — `ComparisonCategory` as interned value object. Rejected: enum,
  which closes the scoring layer to new comparators.
- 2026-09-09 — Fellegi-Sunter carries prior odds and value-frequency-adjusted
  `u`. Rejected: category-only `u`, which makes a common-surname agreement weigh
  the same as a rare one.
- 2026-09-09 — Build on JDK 17 with `maven.compiler.release=8`, pinned by
  toolchain. Rejected: building on JDK 8 (no `--release` before JDK 9) and
  relying on `source`/`target` alone (no API-level checking).
