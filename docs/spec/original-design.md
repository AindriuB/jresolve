# Generic Java 8 Entity Resolution Library

## 1. Purpose

Build a production-quality, framework-independent Java library for **probabilistic entity resolution / record linkage**.

The library matches an arbitrary external Java object (`S`) against an arbitrary internal Java object (`C`) and produces:

- the best matching candidate;
- a score;
- optionally a calibrated probability/confidence;
- the second-best candidate;
- the score margin;
- a decision;
- detailed, explainable field-level evidence.

The library is intended to solve problems such as:

- slightly misspelled names;
- Irish/English name variations;
- diacritics and Unicode differences;
- apostrophe variations;
- nicknames;
- changed surnames;
- variations in address formatting;
- minor address errors;
- missing fields;
- conflicting fields;
- ambiguous candidates.

The library must be **generic** and must not contain knowledge of any particular consuming application.

For example, it must be possible to use:

```java
EntityResolver<ExternalPerson, InternalOwner>
```

but the library itself must not contain either `ExternalPerson` or `InternalOwner`.

---

# 2. Java Compatibility Requirement

## 2.1 Mandatory baseline

**Java 8 is the minimum supported runtime and compilation target.**

The resulting JAR must be usable by applications running on:

```text
Java 8
Java 11
Java 17
Java 21
...
```

but Java 8 is the compatibility boundary.

The library MUST NOT require a newer Java runtime.

---

## 2.2 Java language restrictions

The implementation MUST use only Java 8 language features.

Do NOT use:

```text
records
sealed classes
pattern matching
switch expressions
text blocks
var
local variable type inference
private interface methods
default interface methods requiring newer APIs
modules
```

Java 8 features that ARE permitted include:

```text
generics
lambdas
method references
functional interfaces
default interface methods where supported by Java 8
streams
Optional
java.time
CompletableFuture
```

Do not use newer APIs merely because the development environment is newer.

---

## 2.3 Java API restrictions

The implementation must only depend on APIs available in Java 8.

Do not use:

```text
List.of()
Set.of()
Map.of()
Map.copyOf()
List.copyOf()
Optional.isEmpty()
String.isBlank()
String.repeat()
Stream.toList()
```

Use Java 8 equivalents:

```java
Collections.emptyList()
Collections.singletonList(...)
Collections.unmodifiableList(...)
```

etc.

---

## 2.4 Dependency compatibility

Every runtime dependency MUST be compatible with Java 8.

Dependencies must be pinned to explicit versions.

Do not automatically use the newest version of a dependency without verifying its Java baseline.

For example, Apache Commons Text currently documents Java 8 as its minimum requirement, but dependency compatibility must still be verified when versions change.

Prefer implementing small algorithms internally where doing so materially reduces dependency footprint.

---

# 3. Build Requirements

Use Maven.

The project must compile for Java 8.

The Maven build should explicitly configure Java 8 compatibility.

For Maven Compiler Plugin versions supporting `release`, configure Java 8 appropriately. The Maven documentation notes that `release=8` can be used with recent compiler-plugin versions and that this provides stronger API-level checking than merely setting `source` and `target`.

A compatible build configuration should include the equivalent of:

```xml
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.release>8</maven.compiler.release>
</properties>
```

If compatibility with JDK 8 itself is required, use a compiler-plugin configuration that correctly translates the Java 8 release target when running under JDK 8. Recent Maven Compiler Plugin versions support this behaviour.

The build MUST also verify that no post-Java-8 APIs accidentally enter the codebase.

Where practical, use Animal Sniffer or an equivalent API compatibility check.

---

# 4. Non-Goals

The core library must NOT implement:

```text
Spring
Spring Boot
REST endpoints
HTTP clients
JSON parsing
database persistence
JPA
Hibernate
JDBC
Elasticsearch
Redis
Kafka
authentication
authorization
web UI
model-training infrastructure
```

The consuming application owns these concerns.

The library only performs entity resolution.

---

# 5. Architectural Principles

The implementation must maintain strict separation between:

```text
object extraction
        ↓
normalization
        ↓
similarity calculation
        ↓
comparison evidence
        ↓
scoring
        ↓
probability/calibration
        ↓
decision
```

Do not collapse these concepts.

For example:

```text
Jaro-Winkler = 0.96
```

does NOT mean:

```text
96% probability of identity
```

Jaro-Winkler is merely a comparison feature.

Similarly:

```text
Fellegi-Sunter weight = 15
```

is not inherently a probability.

Probability must only be exposed when a calibrated probabilistic model is being used.

---

# 6. High-Level Architecture

```text
                         Source Object S
                              |
                              v
                    +----------------------+
                    | Field Extraction     |
                    +----------------------+
                              |
                              v
                    +----------------------+
                    | Field Normalization  |
                    +----------------------+
                              |
                              v
                       Normalized Values
                              |
                              v
                    +----------------------+
                    | Candidate Provider   |
                    +----------------------+
                              |
                              v
                       Candidate Set
                              |
                              v
                    +----------------------+
                    | Field Comparison     |
                    +----------------------+
                              |
                              v
                       Match Evidence
                              |
             +----------------+----------------+
             |                |                |
             v                v                v
        Rule Scoring   Fellegi-Sunter   Logistic Regression
             |                |                |
             +----------------+----------------+
                              |
                              v
                     Scored Candidates
                              |
                              v
                      Decision Engine
                              |
                              v
                        Match Result
```

---

# 7. Project Structure

Create:

```text
entity-resolution/
│
├── pom.xml
├── README.md
├── LICENSE
├── CHANGELOG.md
├── docs/
│   ├── architecture.md
│   ├── algorithms.md
│   └── calibration.md
│
└── src/
    ├── main/
    │   └── java/
    │       └── com/
    │           └── example/
    │               └── entityresolution/
    │
    └── test/
        └── java/
            └── com/
                └── example/
                    └── entityresolution/
```

Use an appropriate real package name once the project is established.

---

# 8. Package Structure

Use:

```text
com.example.entityresolution
│
├── api
│   ├── EntityResolver.java
│   ├── EntityResolverBuilder.java
│   ├── MatchResult.java
│   ├── Decision.java
│   ├── Score.java
│   └── ScoredCandidate.java
│
├── field
│   ├── FieldDefinition.java
│   ├── FieldPipeline.java
│   ├── DefaultFieldPipeline.java
│   ├── FieldNormalizer.java
│   ├── FieldComparator.java
│   ├── FieldEvidence.java
│   └── ComparisonCategory.java
│
├── normalization
│   ├── StringNormalizer.java
│   ├── UnicodeNormalizer.java
│   ├── CompositeNormalizer.java
│   └── ...
│
├── comparison
│   ├── SimilarityMetric.java
│   ├── JaroWinklerSimilarity.java
│   ├── LevenshteinSimilarity.java
│   ├── TokenSimilarity.java
│   └── ExactComparator.java
│
├── name
│   ├── NameNormalizer.java
│   ├── NameComparator.java
│   ├── NameEvidence.java
│   ├── AliasRepository.java
│   ├── NameAlias.java
│   └── IrishNameProfile.java
│
├── address
│   ├── AddressNormalizer.java
│   ├── AddressComparator.java
│   └── AddressEvidence.java
│
├── blocking
│   ├── BlockKey.java
│   ├── BlockStrategy.java
│   ├── CandidateProvider.java
│   ├── CandidateIndex.java
│   ├── IndexedCandidateProvider.java
│   └── InMemoryCandidateIndex.java
│
├── scoring
│   ├── MatchScorer.java
│   ├── ScoredMatch.java
│   ├── RuleBasedScorer.java
│   ├── MatchRule.java
│   ├── FellegiSunterModel.java
│   ├── FellegiSunterScorer.java
│   ├── FeatureExtractor.java
│   ├── ProbabilityModel.java
│   ├── LogisticRegressionModel.java
│   └── LogisticRegressionScorer.java
│
├── decision
│   ├── MatchDecisionEngine.java
│   ├── DecisionThresholds.java
│   └── ThresholdDecisionEngine.java
│
├── explanation
│   ├── FieldContribution.java
│   └── MatchExplanation.java
│
└── profile
    └── FieldPipelines.java
```

Do not create unnecessary classes simply to fill the structure.

---

# 9. Core Generic API

## 9.1 EntityResolver

```java
public interface EntityResolver<S, C> {

    MatchResult<C> resolve(
            S source,
            Collection<C> candidates
    );
}
```

Optionally support:

```java
MatchResult<C> resolve(S source);
```

when a `CandidateProvider` is configured.

---

# 10. Field Definition

Use Java 8-compatible classes rather than records.

```java
public final class FieldDefinition<S, C, V> {

    private final String name;
    private final Function<S, V> sourceExtractor;
    private final Function<C, V> candidateExtractor;
    private final FieldPipeline<V, ?> pipeline;

    public FieldDefinition(
            String name,
            Function<S, V> sourceExtractor,
            Function<C, V> candidateExtractor,
            FieldPipeline<V, ?> pipeline) {
        this.name = name;
        this.sourceExtractor = sourceExtractor;
        this.candidateExtractor = candidateExtractor;
        this.pipeline = pipeline;
    }

    public String getName() {
        return name;
    }

    public Function<S, V> getSourceExtractor() {
        return sourceExtractor;
    }

    public Function<C, V> getCandidateExtractor() {
        return candidateExtractor;
    }

    public FieldPipeline<V, ?> getPipeline() {
        return pipeline;
    }
}
```

Use method references wherever possible.

Example:

```java
.field(
    "firstName",
    ExternalPerson::getFirstName,
    Owner::getFirstName,
    FieldPipelines.irishName()
)
```

---

# 11. Field Pipeline

```java
public interface FieldPipeline<V, N> {

    FieldEvidence compare(
            V source,
            V candidate
    );
}
```

Default implementation:

```java
public final class DefaultFieldPipeline<V, N>
        implements FieldPipeline<V, N> {

    private final FieldNormalizer<V, N> normalizer;
    private final FieldComparator<N> comparator;

    public DefaultFieldPipeline(
            FieldNormalizer<V, N> normalizer,
            FieldComparator<N> comparator) {
        this.normalizer = normalizer;
        this.comparator = comparator;
    }

    @Override
    public FieldEvidence compare(V source, V candidate) {

        N normalizedSource =
                normalizer.normalize(source);

        N normalizedCandidate =
                normalizer.normalize(candidate);

        return comparator.compare(
                normalizedSource,
                normalizedCandidate);
    }
}
```

---

# 12. Normalization

```java
public interface FieldNormalizer<V, N> {

    N normalize(V value);
}
```

Normalizers must:

- be deterministic;
- not mutate source objects;
- be thread-safe after construction;
- ideally be stateless.

---

# 13. Generic String Normalization

Implement configurable normalization stages:

```text
Unicode normalization
        ↓
case folding
        ↓
diacritic folding
        ↓
apostrophe normalization
        ↓
punctuation normalization
        ↓
whitespace normalization
```

The original source value must not be modified.

---

# 14. Unicode Normalization

Use:

```java
java.text.Normalizer
```

which is available in Java 8.

The Irish name profile should normally use NFD normalization followed by removal of combining marks.

For example:

```text
Seán
```

can become:

```text
Sean
```

and then:

```text
sean
```

depending on the configured normalization stages.

---

# 15. Apostrophe Normalization

Treat common apostrophe variants consistently.

At minimum support:

```text
'
’
‘
ʼ
`
´
```

where they are being used as apostrophe characters.

Examples:

```text
O'Sullivan
O’Sullivan
OʼSullivan
```

should normalize consistently under the Irish name profile.

Do not blindly remove all punctuation from all domains.

Normalization behaviour must be profile-specific.

---

# 16. Name Profile

Provide:

```java
FieldPipelines.irishName()
```

This is a convenience profile composed from generic primitives.

It should support:

- Unicode normalization;
- case normalization;
- diacritic handling;
- apostrophe normalization;
- punctuation normalization;
- aliases;
- nicknames;
- Irish/English equivalents;
- Jaro-Winkler;
- Levenshtein;
- optional phonetic comparison.

The Irish-specific functionality must remain configurable.

---

# 17. Name Aliases

Create:

```java
public interface AliasRepository {

    Set<NameAlias> findAliases(String canonicalValue);
}
```

and:

```java
public final class NameAlias {

    private final String canonicalName;
    private final String alias;
    private final AliasType type;
    private final double strength;

    // constructor + getters
}
```

```java
public enum AliasType {
    IRISH_ENGLISH,
    NICKNAME,
    COMMON_VARIANT,
    HISTORICAL
}
```

Aliases are evidence.

They are NOT equivalent to exact equality.

For example:

```text
John ↔ Seán
```

should not produce the same evidence category as:

```text
John ↔ John
```

---

# 18. Name Evidence

Implement:

```java
public final class NameEvidence
        implements FieldEvidence {

    private final boolean exact;
    private final boolean aliasMatch;
    private final boolean phoneticMatch;
    private final double jaroWinkler;
    private final double levenshteinSimilarity;
    private final ComparisonCategory category;

    // constructor + getters
}
```

---

# 19. ComparisonCategory

```java
public enum ComparisonCategory {

    EXACT,
    ALIAS,
    VERY_HIGH,
    HIGH,
    MEDIUM,
    LOW,
    CONFLICT,
    MISSING
}
```

The mapping from similarity to category must be configurable.

---

# 20. Similarity Bands

```java
public final class SimilarityBands {

    private final double veryHigh;
    private final double high;
    private final double medium;
    private final double low;

    // constructor + getters
}
```

Initial defaults may be:

```text
VERY_HIGH >= 0.95
HIGH       >= 0.85
MEDIUM     >= 0.70
LOW        <  0.70
```

These are engineering defaults, not statistically validated thresholds.

They must be configurable.

---

# 21. SimilarityMetric

```java
public interface SimilarityMetric {

    double similarity(
            String left,
            String right
    );
}
```

All implementations must return:

```text
0.0 <= score <= 1.0
```

---

# 22. Jaro-Winkler

Implement:

```java
public final class JaroWinklerSimilarity
        implements SimilarityMetric {
    ...
}
```

Jaro-Winkler is particularly useful for:

- names;
- short strings;
- transpositions;
- prefix-preserving typographical errors.

It must remain a feature, not the final match probability.

---

# 23. Levenshtein

Implement:

```java
public final class LevenshteinSimilarity
        implements SimilarityMetric {
    ...
}
```

Use:

```text
similarity =
    1 - distance / max(length(left), length(right))
```

Handle:

```text
null
empty strings
identical strings
```

explicitly.

---

# 24. Similarity Properties

Test:

```text
similarity(x,x) == 1
0 <= similarity <= 1
similarity(a,b) approximately equals similarity(b,a)
```

where mathematically applicable.

---

# 25. Date Comparison

Support Java 8:

```java
java.time.LocalDate
```

Provide:

```java
FieldPipelines.exactDate()
```

Behaviour:

```text
same date → EXACT
different date → CONFLICT
null vs value → MISSING
null vs null → MISSING
```

Do not fuzzy-match dates by default.

---

# 26. Address Pipeline

The expected consuming application may expose addresses as:

```java
List<String>
```

The library should provide:

```java
FieldPipelines.irishAddress()
```

The implementation should:

1. normalize each line;
2. normalize punctuation;
3. normalize whitespace;
4. tokenize;
5. detect likely house number;
6. optionally detect postcode;
7. compare address lines;
8. tolerate modest line reordering;
9. calculate aggregate similarity.

---

# 27. Address Evidence

```java
public final class AddressEvidence
        implements FieldEvidence {

    private final boolean exact;
    private final boolean houseNumberMatch;
    private final boolean postcodeMatch;
    private final double tokenSimilarity;
    private final double lineSimilarity;
    private final double aggregateSimilarity;
    private final ComparisonCategory category;

    // constructor + getters
}
```

---

# 28. Address Examples

The following should compare strongly:

```text
12 Main Street
Dublin 4
```

and:

```text
12 Main St.
Dublin 4
```

Likewise:

```text
12 Main Street
Dublin 4
```

and:

```text
Dublin 4
12 Main St
```

provided the token and structural evidence supports the comparison.

Do not rely solely on whole-string Levenshtein for addresses.

---

# 29. MatchEvidence

```java
public final class MatchEvidence {

    private final Map<String, FieldEvidence> fields;

    public MatchEvidence(
            Map<String, FieldEvidence> fields) {
        this.fields =
            Collections.unmodifiableMap(
                new LinkedHashMap<String, FieldEvidence>(fields));
    }

    public Map<String, FieldEvidence> getFields() {
        return fields;
    }

    public FieldEvidence getField(String name) {
        return fields.get(name);
    }
}
```

The evidence must remain generic.

Do not create hard-coded fields such as:

```text
firstNameEvidence
lastNameEvidence
dobEvidence
```

in the core engine.

---

# 30. FieldEvidence

```java
public interface FieldEvidence {

    ComparisonCategory getCategory();
}
```

Specialised evidence classes may expose additional information.

---

# 31. Evidence Comparator

```java
public interface EvidenceComparator<S, C> {

    MatchEvidence compare(
            S source,
            C candidate);
}
```

Implement:

```java
DefaultEvidenceComparator<S, C>
```

which iterates through configured field definitions.

---

# 32. Candidate Provider

```java
public interface CandidateProvider<S, C> {

    Collection<C> findCandidates(S source);
}
```

The library does not own the candidate dataset.

The consuming application decides how candidates are obtained.

---

# 33. Blocking

Blocking exists to reduce the number of candidates that require expensive fuzzy comparison.

Create:

```java
public final class BlockKey {

    private final String type;
    private final String value;

    // constructor + getters
}
```

and:

```java
public interface BlockStrategy<S, C> {

    Set<BlockKey> sourceKeys(S source);

    Set<BlockKey> candidateKeys(C candidate);
}
```

Examples:

```text
exact DOB
surname
phonetic surname
postcode
DOB year + surname
postcode + surname
```

---

# 34. Blocking Principle

Blocking is an optimisation.

It is NOT proof of identity.

Never use:

```text
same blocking key
```

as equivalent to:

```text
same entity
```

Multiple blocking strategies should be composable to reduce false negatives.

---

# 35. Candidate Index

```java
public interface CandidateIndex<C> {

    Set<String> find(BlockKey key);

    C get(String id);
}
```

Provide:

```java
InMemoryCandidateIndex<C>
```

for tests and small workloads.

The application may provide its own implementation backed by:

```text
database
Elasticsearch
Redis
search engine
in-memory cache
```

without changing the resolver.

---

# 36. IndexedCandidateProvider

Implement:

```java
public final class IndexedCandidateProvider<S, C>
        implements CandidateProvider<S, C> {
    ...
}
```

It should:

1. generate source block keys;
2. query the candidate index for each key;
3. union candidate IDs;
4. remove duplicates;
5. retrieve candidates;
6. return the resulting candidate collection.

---

# 37. Blocking Statistics

Optionally expose:

```java
public final class BlockingStatistics {

    private final int sourceKeyCount;
    private final int candidatesBeforeBlocking;
    private final int candidatesAfterBlocking;
    private final double reductionRatio;

    // constructor + getters
}
```

This is important for tuning production performance.

---

# 38. Rules

Separate hard candidate rules from soft scoring rules.

## Hard Rule

```java
public interface CandidateRule<S, C> {

    RuleDecision evaluate(
            S source,
            C candidate,
            MatchEvidence evidence);
}
```

## Soft Rule

```java
public interface MatchRule<S, C> {

    RuleResult evaluate(
            S source,
            C candidate,
            MatchEvidence evidence);
}
```

---

# 39. Rule Outcome

```java
public enum RuleOutcome {

    POSITIVE,
    NEGATIVE,
    NEUTRAL
}
```

```java
public final class RuleResult {

    private final String ruleId;
    private final RuleOutcome outcome;
    private final double weight;
    private final String explanation;

    // constructor + getters
}
```

---

# 40. Rule-Based Scoring

Implement a transparent deterministic scorer.

It must support:

- field-specific weights;
- comparison-category weights;
- positive rules;
- negative rules;
- required fields;
- conflict penalties.

Example configuration:

```text
DOB EXACT             +10
Surname EXACT          +6
Surname ALIAS          +4
First Name EXACT       +5
First Name ALIAS       +3
Address VERY_HIGH      +6
Address HIGH           +4
DOB CONFLICT          -20
```

These numbers are examples only.

All values must be configurable.

---

# 41. MatchScorer

```java
public interface MatchScorer {

    ScoredMatch score(
            MatchEvidence evidence);
}
```

```java
public final class ScoredMatch {

    private final double score;
    private final ScoreType scoreType;
    private final Map<String, Double> contributions;

    // constructor + getters
}
```

---

# 42. ScoreType

```java
public enum ScoreType {

    RULE_BASED,
    FELLEGI_SUNTER,
    LOGISTIC_REGRESSION
}
```

---

# 43. Fellegi-Sunter

Implement genuine Fellegi-Sunter probabilistic record linkage.

For each field and comparison category:

```text
m = P(category | true match)

u = P(category | non-match)
```

Calculate:

```text
weight_i = log2(m_i / u_i)
```

and:

```text
W = Σ weight_i
```

---

# 44. Fellegi-Sunter Model

```java
public interface FellegiSunterModel {

    double getMProbability(
            String field,
            ComparisonCategory category);

    double getUProbability(
            String field,
            ComparisonCategory category);
}
```

Validate:

```text
0 < m <= 1
0 < u <= 1
```

Zero probabilities must be avoided through validation or configurable smoothing.

---

# 45. Fellegi-Sunter Missing Values

Missingness must be explicitly handled.

Do not automatically treat:

```text
missing
```

as:

```text
non-match
```

unless the configured model explicitly defines that behaviour.

---

# 46. Fellegi-Sunter Interpretation

The raw Fellegi-Sunter score is a log-likelihood ratio.

It is not automatically a probability.

The API must therefore distinguish:

```text
score
probability
confidence
```

A probability may only be exposed where the model is calibrated accordingly.

---

# 47. Logistic Regression

Implement:

```java
public interface FeatureExtractor {

    Map<String, Double> extract(
            MatchEvidence evidence);
}
```

and:

```java
public interface ProbabilityModel {

    double predict(
            Map<String, Double> features);
}
```

---

# 48. Logistic Regression Model

Implement:

```java
public final class LogisticRegressionModel
        implements ProbabilityModel {
    ...
}
```

Use:

```text
z = β0 + β1x1 + β2x2 + ... + βnxn

P(match) = 1 / (1 + e^-z)
```

The implementation must support:

- intercept;
- named coefficients;
- deterministic inference;
- coefficient validation.

---

# 49. Logistic Regression Features

Provide a configurable feature extractor capable of producing:

```text
firstName.exact
firstName.alias
firstName.jaroWinkler
firstName.levenshtein

lastName.exact
lastName.alias
lastName.jaroWinkler
lastName.levenshtein

dateOfBirth.exact

address.exact
address.houseNumberExact
address.postcodeExact
address.tokenSimilarity
address.lineSimilarity
address.aggregateSimilarity
```

Do not require every domain to use these features.

---

# 50. Feature Independence

Do not assume that Jaro-Winkler and Levenshtein are independent.

They are correlated measurements.

Therefore do not naively add:

```text
0.25 * JaroWinkler
+
0.25 * Levenshtein
```

and call the result a probability.

Instead:

```text
raw metrics
     ↓
features
     ↓
model
     ↓
probability
```

---

# 51. Decision Engine

Scoring must be separate from decisioning.

```java
public interface MatchDecisionEngine<C> {

    MatchDecision<C> decide(
            List<ScoredCandidate<C>> candidates);
}
```

---

# 52. Decision

```java
public enum Decision {

    MATCH,
    REVIEW,
    NO_MATCH
}
```

---

# 53. Decision Thresholds

```java
public final class DecisionThresholds {

    private final double matchThreshold;
    private final double reviewThreshold;
    private final double minimumMargin;

    // constructor + getters
}
```

The meaning of thresholds depends on the scorer.

For a calibrated probability model:

```text
probability >= matchThreshold
AND
margin >= minimumMargin
→ MATCH
```

For a non-probabilistic scorer, the thresholds apply to the model's score scale.

---

# 54. Match Result

Use a Java 8-compatible class:

```java
public final class MatchResult<C> {

    private final Decision decision;
    private final C match;
    private final boolean matched;
    private final Score score;
    private final Score secondBestScore;
    private final double margin;
    private final List<ScoredCandidate<C>> candidates;

    // constructor + getters
}
```

Avoid using `Optional` in the public result if doing so makes Java 8 consumer code unnecessarily cumbersome.

Use:

```text
matched
match
```

where:

```text
matched == false
→ match == null
```

must be explicitly documented.

---

# 55. Score

```java
public final class Score {

    private final double value;
    private final ScoreType type;
    private final Double probability;

    // constructor + getters
}
```

`probability` must be:

```text
null
```

unless the scorer actually produces a calibrated probability.

---

# 56. Scored Candidate

```java
public final class ScoredCandidate<C> {

    private final C candidate;
    private final Score score;
    private final List<FieldContribution> contributions;

    // constructor + getters
}
```

---

# 57. Explainability

Create:

```java
public final class FieldContribution {

    private final String field;
    private final FieldEvidence evidence;
    private final double contribution;
    private final String explanation;

    // constructor + getters
}
```

The result should allow an application to explain:

```text
First name:
  Irish/English alias match
  contribution: +4.1

Last name:
  exact after normalization
  contribution: +5.9

Date of birth:
  exact
  contribution: +10.7

Address:
  high token similarity
  contribution: +4.8
```

---

# 58. Model Metadata

Provide:

```java
public final class ModelMetadata {

    private final String modelId;
    private final String version;
    private final String description;

    // constructor + getters
}
```

Where a model is used, results should optionally expose model metadata.

This supports auditability and future model replacement.

---

# 59. EntityResolver Builder

The preferred public API should look approximately like:

```java
EntityResolver<ExternalPerson, Owner> resolver =
    EntityResolverBuilder
        .<ExternalPerson, Owner>builder()
        .field(
            "firstName",
            ExternalPerson::getFirstName,
            Owner::getFirstName,
            FieldPipelines.irishName()
        )
        .field(
            "lastName",
            ExternalPerson::getLastName,
            Owner::getLastName,
            FieldPipelines.irishName()
        )
        .field(
            "dateOfBirth",
            ExternalPerson::getDateOfBirth,
            Owner::getDateOfBirth,
            FieldPipelines.exactDate()
        )
        .field(
            "address",
            ExternalPerson::getAddress,
            Owner::getAddress,
            FieldPipelines.irishAddress()
        )
        .scorer(
            Scorers.fellegiSunter(model)
        )
        .decisionEngine(
            Decisions.defaultEngine()
        )
        .build();
```

The exact builder implementation is left to the agent provided the resulting API remains clean and Java 8 compatible.

---

# 60. No JSON Knowledge

The library must never require JSON.

The consuming application may do:

```text
JSON
 ↓
Jackson/Gson/etc.
 ↓
ExternalPerson
 ↓
EntityResolver
```

The resolver begins at the Java object.

There must be no dependency on:

```text
ObjectMapper
JsonNode
JSON schema
REST
```

---

# 61. No Reflection by Default

Prefer:

```java
ExternalPerson::getFirstName
```

over:

```java
"firstName"
```

The logical field name remains a string because it is needed for:

- model configuration;
- evidence;
- explanations;
- feature names.

The actual field extraction should be type-safe.

---

# 62. Missing Values

For generic fields:

```text
null vs null
→ MISSING

null vs value
→ MISSING

value vs value
→ compare
```

Missing data should not automatically be considered contradictory.

The scorer determines how missing evidence contributes.

---

# 63. Conflicts

Strong contradictory evidence must be represented separately.

Example:

```text
source DOB:
1985-06-14

candidate DOB:
1974-02-10
```

This should produce:

```text
CONFLICT
```

rather than simply:

```text
similarity = 0
```

The distinction is important for probabilistic scoring.

---

# 64. Name Normalization Requirements

Tests MUST include:

```text
Seán
Sean

Mícheál
Micheal

O'Sullivan
O’Sullivan
OʼSullivan

Ó Súilleabháin
O Súilleabháin
O'Suilleabhain
```

The canonical representation must be deterministic.

Normalization must be idempotent:

```java
normalize(normalize(x)).equals(normalize(x))
```

---

# 65. Irish Name Handling

The library must support configurable Irish/English aliases.

Examples for the test profile may include:

```text
Seán ↔ John
Pádraig ↔ Patrick
Máire ↔ Mary
Liam ↔ William
```

These are examples and should not be treated as a definitive linguistic dataset.

The alias data must be replaceable.

Do not hard-code assumptions such as:

```text
every John = Seán
```

as exact identity.

---

# 66. Surname Changes

The library must allow alternative surnames to contribute evidence.

For example:

```text
source surname:
Smith

candidate surname:
Jones
```

may still be a plausible match if:

```text
DOB matches
first name matches
address matches
```

but surname mismatch must remain visible as evidence.

Do not make changed surnames automatic matches.

---

# 67. Nicknames

Nicknames use the same alias infrastructure.

Examples:

```text
Robert ↔ Bob
William ↔ Bill
Elizabeth ↔ Liz
```

Nickname evidence must remain distinguishable from exact equality.

---

# 68. Candidate Ranking

All candidates should be ranked descending by score.

If:

```text
candidate A = 0.993
candidate B = 0.991
```

the result may be:

```text
REVIEW
```

if the configured margin requirement is not satisfied.

Never select the first candidate arbitrarily.

---

# 69. Margin

Calculate:

```text
margin =
bestScore - secondBestScore
```

If there is no second candidate:

```text
margin =
positive infinity
```

or another explicitly documented representation.

Prefer explicit handling rather than magic sentinel values.

---

# 70. Performance

The architecture must support:

```text
blocking
candidate reduction
cheap comparisons before expensive comparisons
```

Do not run expensive fuzzy comparison across an entire million-record dataset.

Conceptually:

```text
N = total candidates

B = blocked candidates

B << N
```

The expected resolution process is:

```text
source
 ↓
blocking
 ↓
small candidate set
 ↓
fuzzy comparison
 ↓
scoring
```

---

# 71. Complexity

Without blocking:

```text
O(N * comparisonCost)
```

With blocking:

```text
O(B * comparisonCost)
```

where:

```text
B << N
```

for an effective blocking strategy.

---

# 72. Thread Safety

The resolver must be reusable across concurrent calls after construction.

Configuration should be immutable.

Avoid:

```text
mutable global state
static caches
global model mutation
```

unless explicitly designed for concurrency.

In-memory indexes should be immutable after construction or explicitly thread-safe.

---

# 73. Caching

Do not add caching until profiling demonstrates a need.

Potential future cache candidates include:

```text
normalized names
phonetic keys
normalized addresses
alias lookups
```

Caching must not alter correctness.

---

# 74. Logging

The core library must not require a logging framework.

Do not log by default.

Expose structured diagnostics through result objects.

The consuming application decides whether to use:

```text
SLF4J
Log4j
Logback
java.util.logging
```

---

# 75. Error Handling

Create library-specific configuration exceptions.

For example:

```java
public class EntityResolutionConfigurationException
        extends RuntimeException {
    ...
}
```

Fail fast on:

```text
duplicate field names
null required configuration
invalid thresholds
invalid probabilities
invalid coefficients
invalid alias strength
invalid similarity bands
```

Do not silently accept invalid scoring models.

---

# 76. Null Handling

Null handling must be deterministic.

Every public comparator must document its null behaviour.

Do not throw unexpected `NullPointerException` for normal missing-field scenarios.

---

# 77. Public API Size

Keep the public API small.

Only expose classes that consumers genuinely need.

Implementation classes may remain package-private where practical.

All public interfaces/classes require Javadocs.

---

# 78. Java 8 Collections

Do not use newer collection factory methods.

Instead of:

```java
List.of(a, b)
```

use:

```java
Arrays.asList(a, b)
```

Instead of:

```java
Collections.unmodifiableList(List.of(...))
```

construct the underlying collection explicitly.

Do not expose mutable internal collections.

---

# 79. Java 8 Optional

`Optional` is permitted because it exists in Java 8.

However, do not overuse it in the core API.

For performance-sensitive inner loops, avoid creating unnecessary temporary objects.

---

# 80. Streams

Java 8 streams may be used where they improve readability.

Do not use:

```java
Stream.toList()
```

because it is not available in Java 8.

Use:

```java
.collect(Collectors.toList())
```

where required.

---

# 81. Date API

Use:

```java
java.time.LocalDate
java.time.LocalDateTime
```

as available in Java 8.

Do not introduce dependencies solely to handle normal Java 8 date types.

---

# 82. Algorithm Dependencies

Jaro-Winkler and Levenshtein should preferably be implemented within the library unless there is a strong reason to use an external implementation.

This provides:

- predictable behaviour;
- no version incompatibility;
- control over edge cases;
- easier testing;
- minimal dependency footprint.

If Apache Commons Text is used, pin the version and verify Java 8 compatibility.

---

# 83. Unit Testing

Tests must cover:

```text
Unicode normalization
case normalization
diacritic normalization
apostrophe normalization
Jaro-Winkler
Levenshtein
token similarity
exact matching
null values
missing values
conflicts
Irish names
aliases
nicknames
addresses
blocking
rules
Fellegi-Sunter
logistic regression
decision thresholds
candidate ranking
```

---

# 84. Property Tests

Verify:

```text
0 <= similarity <= 1

similarity(x,x) == 1

similarity(a,b) ~= similarity(b,a)

normalize(normalize(x)) == normalize(x)
```

where mathematically applicable.

---

# 85. Fellegi-Sunter Tests

Use hand-calculated cases.

Verify:

```text
m > u → positive contribution

m == u → zero contribution

m < u → negative contribution
```

Verify:

```text
total score = sum(field weights)
```

within expected floating-point tolerance.

---

# 86. Logistic Regression Tests

Use manually calculated examples.

Given:

```text
β0
β1
β2
...
```

verify:

```text
z = β0 + Σ βixi

P = 1 / (1 + exp(-z))
```

against expected results.

---

# 87. End-to-End Test Model

Create test-only classes:

```java
public final class ExternalPerson {

    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private List<String> address;

    // constructors + getters
}
```

and:

```java
public final class Owner {

    private String id;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private List<String> address;

    // constructors + getters
}
```

These classes exist ONLY in tests.

---

# 88. Worked Positive Example

Source:

```text
firstName:
Seán

lastName:
O’Sullivan

dateOfBirth:
1985-06-14

address:
12 Main Street
Dublin 4
```

Candidate:

```text
firstName:
John

lastName:
O'Sullivan

dateOfBirth:
1985-06-14

address:
12 Main St.
Dublin 4
```

Expected evidence:

```text
firstName:
ALIAS

lastName:
EXACT after normalization

dateOfBirth:
EXACT

address:
VERY_HIGH
```

The candidate should rank highly.

The exact final score depends on the configured scoring model.

---

# 89. Worked Negative Example

Source:

```text
Seán O'Sullivan
1985-06-14
12 Main Street
```

Candidate:

```text
John O'Sullivan
1974-02-10
12 Main Street
```

Expected:

```text
firstName:
ALIAS or HIGH similarity

lastName:
EXACT

DOB:
CONFLICT

address:
VERY_HIGH
```

The DOB conflict must provide strong negative evidence.

The system must not blindly match this candidate.

---

# 90. Worked Ambiguous Example

Source:

```text
John Murphy
1985-06-14
Dublin
```

Candidates:

```text
Owner A:
John Murphy
1985-06-14
Dublin 4

Owner B:
John Murphy
1985-06-14
Dublin 8
```

If:

```text
bestScore - secondBestScore
```

is below the configured margin:

```text
Decision = REVIEW
```

rather than automatically selecting Owner A.

---

# 91. Test Dataset

Create a representative synthetic dataset containing at least:

```text
exact matches
minor spelling errors
major spelling errors
Irish diacritics
apostrophe variations
Irish/English names
nicknames
surname changes
address abbreviations
address line reordering
missing first name
missing surname
missing address
incorrect DOB
duplicate people
ambiguous people
```

Do not use real personal data.

---

# 92. Benchmark

If practical, create JMH benchmarks.

Benchmark:

```text
1,000 candidates
10,000 candidates
100,000 candidates
```

Compare:

```text
full scan
blocked candidate set
```

Measure:

```text
throughput
latency
allocation
candidate reduction
```

Benchmarking must not compromise Java 8 runtime compatibility.

---

# 93. Training Dataset Representation

Do not implement training infrastructure in the core library.

However, define a future-compatible representation:

```java
public final class MatchTrainingExample {

    private final Map<String, Double> features;
    private final boolean match;

    // constructor + getters
}
```

This allows labelled historical matching decisions to be used later to train models externally.

---

# 94. Model Calibration

The library must distinguish:

```text
similarity
likelihood ratio
raw score
probability
calibrated probability
```

A logistic regression output is mathematically a probability under its model, but production confidence still depends on representative training data and calibration.

Fellegi-Sunter weights should not be presented as probabilities.

Document this clearly.

---

# 95. Model Versioning

Every production scoring model should have:

```text
modelId
modelVersion
description
```

This permits:

```text
audit
reproducibility
model comparison
rollback
```

---

# 96. Determinism

Given:

```text
same source
same candidates
same configuration
same model
same alias data
```

the result must be deterministic.

Do not introduce random behaviour into matching.

---

# 97. Candidate Identity

For indexed candidate resolution, candidate IDs should be supplied by the consuming application.

For direct collection resolution, optionally support:

```java
Function<C, String> candidateIdentity
```

for deduplication and diagnostics.

Do not assume every candidate has an `id` property.

---

# 98. Configuration Validation

Builder `.build()` must validate:

```text
at least one field configured
unique field names
non-null extractors
non-null pipelines
valid scorer
valid decision engine
valid probability model
valid thresholds
```

Invalid configuration should fail immediately.

---

# 99. API Example

The desired consumer experience:

```java
EntityResolver<ExternalPerson, Owner> resolver =
    EntityResolverBuilder
        .<ExternalPerson, Owner>builder()
        .field(
            "firstName",
            ExternalPerson::getFirstName,
            Owner::getFirstName,
            FieldPipelines.irishName()
        )
        .field(
            "lastName",
            ExternalPerson::getLastName,
            Owner::getLastName,
            FieldPipelines.irishName()
        )
        .field(
            "dateOfBirth",
            ExternalPerson::getDateOfBirth,
            Owner::getDateOfBirth,
            FieldPipelines.exactDate()
        )
        .field(
            "address",
            ExternalPerson::getAddress,
            Owner::getAddress,
            FieldPipelines.irishAddress()
        )
        .scorer(
            Scorers.fellegiSunter(model)
        )
        .decisionEngine(
            Decisions.defaultEngine()
        )
        .build();

MatchResult<Owner> result =
    resolver.resolve(
        externalPerson,
        owners);
```

This code MUST compile on Java 8.

---

# 100. Implementation Phases

Implement incrementally.

## Phase 1 — Project Foundation

Create:

```text
pom.xml
package structure
Java 8 configuration
README
basic test framework
```

Verify Java 8 compatibility immediately.

---

## Phase 2 — Generic Core

Implement:

```text
EntityResolver
EntityResolverBuilder
FieldDefinition
FieldPipeline
FieldNormalizer
FieldComparator
FieldEvidence
MatchEvidence
```

Do not implement person-specific behaviour yet.

---

## Phase 3 — Similarity Algorithms

Implement:

```text
SimilarityMetric
JaroWinklerSimilarity
LevenshteinSimilarity
TokenSimilarity
```

Add exhaustive unit tests.

---

## Phase 4 — Normalization

Implement:

```text
UnicodeNormalizer
StringNormalizer
CompositeNormalizer
```

Test:

```text
diacritics
case
apostrophes
punctuation
whitespace
idempotency
```

---

## Phase 5 — Name Pipeline

Implement:

```text
NameNormalizer
NameComparator
NameEvidence
AliasRepository
IrishNameProfile
```

Add Irish-specific tests.

---

## Phase 6 — Address Pipeline

Implement:

```text
AddressNormalizer
AddressComparator
AddressEvidence
```

Add realistic variation tests.

---

## Phase 7 — Rule Scoring

Implement:

```text
MatchRule
RuleResult
RuleBasedScorer
```

Test positive/negative contributions.

---

## Phase 8 — Fellegi-Sunter

Implement:

```text
FellegiSunterModel
FellegiSunterScorer
```

Add hand-calculated validation tests.

---

## Phase 9 — Logistic Regression

Implement:

```text
FeatureExtractor
ProbabilityModel
LogisticRegressionModel
LogisticRegressionScorer
```

Add mathematical validation tests.

---

## Phase 10 — Blocking

Implement:

```text
BlockKey
BlockStrategy
CandidateProvider
CandidateIndex
InMemoryCandidateIndex
IndexedCandidateProvider
```

Add candidate-reduction tests.

---

## Phase 11 — Decision Engine

Implement:

```text
MatchDecisionEngine
DecisionThresholds
ThresholdDecisionEngine
```

Add ambiguity tests.

---

## Phase 12 — Explainability

Implement:

```text
FieldContribution
MatchExplanation
model metadata
blocking diagnostics
```

---

## Phase 13 — Performance

Add:

```text
JMH benchmark
candidate reduction metrics
normalization benchmarks
similarity benchmarks
```

---

## Phase 14 — Documentation

Complete:

```text
README.md
docs/architecture.md
docs/algorithms.md
docs/calibration.md
```

---

# 101. Maven Quality Requirements

Configure:

```text
compiler
surefire
javadoc
dependency management
```

Optionally:

```text
checkstyle
spotbugs
pmd
animal-sniffer
jmh
```

The build must fail if Java 8 compatibility is violated.

The Maven compiler configuration must explicitly target Java 8 rather than relying on whatever JDK happens to execute Maven. Maven's documentation specifically notes that merely setting `target` is insufficient to guarantee API compatibility, so API compatibility verification should be included where possible.

---

# 102. Dependency Policy

The dependency tree should be intentionally small.

Preferred:

```text
Java 8 standard library
+
minimal algorithm dependencies
+
test dependencies
```

Avoid large frameworks.

Do not introduce Spring merely for dependency injection.

Use constructors and builders.

---

# 103. Performance Policy

Do not prematurely optimise.

First establish:

```text
correctness
test coverage
clear abstractions
```

Then benchmark.

Optimise only demonstrated bottlenecks.

Potential optimisation areas:

```text
normalization
similarity calculations
candidate blocking
alias lookups
object allocation
```

---

# 104. Security / Privacy

The library may be used against personal data.

Therefore:

- do not log source values;
- do not log candidate values;
- do not include personal values in exception messages;
- do not retain source/candidate objects beyond the operation;
- avoid static caches containing personal data;
- make diagnostics opt-in;
- document that consumers remain responsible for data protection requirements.

The library should not copy personal data unnecessarily.

---

# 105. Memory Management

The resolver should not retain:

```text
source objects
candidate objects
field values
```

after resolution unless explicitly configured to do so.

In-memory indexes are an explicit exception because they intentionally retain candidates.

---

# 106. Extension Points

The architecture must make it straightforward to add:

```text
new field types
new normalizers
new similarity algorithms
new aliases
new blocking strategies
new scoring algorithms
new probability models
new decision engines
```

without modifying the core resolver.

For example:

```java
FieldPipelines.phoneNumber()
FieldPipelines.email()
FieldPipelines.companyName()
FieldPipelines.postcode()
```

could be added later.

---

# 107. Future Scoring Models

The architecture should permit future:

```text
gradient boosting
random forest
neural network
custom Bayesian model
external model inference
```

without changing:

```text
FieldDefinition
FieldPipeline
MatchEvidence
```

The model consumes evidence/features.

---

# 108. Future Multi-Stage Matching

The design should permit a future pipeline such as:

```text
Stage 1:
blocking

Stage 2:
cheap deterministic rules

Stage 3:
similarity scoring

Stage 4:
probabilistic scoring

Stage 5:
decision

Stage 6:
human review
```

Do not implement the entire workflow now unless required.

Design the interfaces so it can be added later.

---

# 109. Important Anti-Patterns

The implementation MUST NOT:

### Anti-pattern 1

Use:

```text
Jaro-Winkler > 0.9
```

as an automatic match.

### Anti-pattern 2

Treat:

```text
similarity = 0.95
```

as:

```text
probability = 95%
```

### Anti-pattern 3

Hard-code:

```text
firstName
lastName
dateOfBirth
address
```

into the resolver.

### Anti-pattern 4

Require JSON.

### Anti-pattern 5

Require Spring.

### Anti-pattern 6

Require a database.

### Anti-pattern 7

Use reflection for normal field extraction.

### Anti-pattern 8

Treat missing data as mismatch automatically.

### Anti-pattern 9

Treat blocking as proof of identity.

### Anti-pattern 10

Automatically match two records because they share a name and DOB.

---

# 110. Acceptance Criteria

The implementation is complete when all of the following are true.

## Java 8

The library:

```text
compiles on Java 8
runs on Java 8
uses only Java 8 language features
uses only Java 8 APIs
```

## Framework independence

There is no dependency on:

```text
Spring
Spring Boot
Jackson
JPA
database
HTTP
```

## Genericity

The library can resolve arbitrary:

```text
S → C
```

types without source modification.

## Field extraction

Consumers can configure arbitrary Java getters using Java 8 method references/lambdas.

## Normalization

Unicode, diacritics, apostrophes and whitespace are handled.

## Irish names

Configurable Irish/English aliases and name variations are supported.

## Similarity

Jaro-Winkler and Levenshtein are available independently.

## Address

Address collections can be compared using tolerant token/line-based logic.

## Rules

Rules can contribute positive and negative evidence.

## Fellegi-Sunter

A configurable implementation exists.

## Logistic regression

A configurable implementation exists.

## Blocking

Blocking is independent of scoring.

## Explainability

Every candidate can expose field-level scoring evidence.

## Decisioning

The engine supports:

```text
MATCH
REVIEW
NO_MATCH
```

## Ambiguity

The engine considers the difference between the best and second-best candidate.

## Determinism

Identical inputs/configuration/models produce identical results.

## Testing

All major algorithms and workflows have tests.

## Documentation

A developer can understand how to integrate the library without reading its implementation.

---

# 111. Final Architectural Principle

The completed system should be understood as a **generic evidence-generation and entity-resolution engine**.

The core abstraction is:

```text
                  Java Object S
                       |
                       v
                Field Extractors
                       |
                       v
                  Normalizers
                       |
                       v
                Field Comparators
                       |
                       v
                Match Evidence
                       |
              +--------+---------+
              |        |         |
              v        v         v
            Rules   Fellegi   Logistic
                     Sunter
              |        |         |
              +--------+---------+
                       |
                       v
                 Candidate Score
                       |
                       v
                  Decision
                       |
                       v
              MatchResult<C>
```

The consuming application supplies:

```text
source objects
candidate objects
field extractors
domain configuration
alias data
candidate storage
```

The library supplies:

```text
normalization
similarity
evidence
blocking abstractions
scoring
probabilistic models
decisioning
explainability
```

The most important design constraint is:

> **The resolver must know how to compare evidence, but must not know what the entities represent.**

The second most important constraint is:

> **Java 8 compatibility is a hard requirement, not merely a compilation preference.**

The implementation should therefore favour simple Java 8 classes, interfaces, constructors, builders, generics, lambdas and method references over newer language constructs.

---

# 112. Implementation Instruction to the Coding Agent

Implement this specification as a complete Maven library.

Do not implement everything as one large change.

Proceed phase-by-phase in the order specified above.

After each phase:

1. compile;
2. run tests;
3. inspect the public API;
4. verify Java 8 compatibility;
5. fix design problems before proceeding.

Do not invent additional framework dependencies.

Do not add Spring.

Do not add JSON handling.

Do not assume a database.

Do not hard-code the consuming application's domain model.

Where this document leaves an implementation detail unspecified, prefer:

1. Java 8 compatibility;
2. small public APIs;
3. immutability;
4. thread safety;
5. deterministic behaviour;
6. testability;
7. explainability;
8. minimal dependencies.

The final deliverable is a reusable **Java 8 entity-resolution library**, not an application.