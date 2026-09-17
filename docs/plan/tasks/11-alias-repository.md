# 11 — Alias equivalence groups

**Repo:** .
**Depends on:** none
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/alias/**
- jresolve-core/src/test/java/io/github/aindriub/jresolve/alias/**

## Goal
D7's repository: the half of "beat the join" that lets `Seán` and `John` agree
at all. Today that pair scores LOW and costs the match five points. This task
ships the data structure and its transitive closure; task 13 wires it to a
comparator.

## Context
- docs/design-decisions.md#d7 — the two defects in the original `findAliases` (direction, transitivity), the replacement interface, and why `strength` left the data.
- docs/architecture.md, "Aliases are equivalence groups, not directed pairs" — the `Robert`/`Bob`/`Bobby` closure requirement.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/evidence/ComparisonCategory.java:24-26 — `ALIAS_TRANSLATION`, `ALIAS_NICKNAME` and `ALIAS_VARIANT` already exist; do not mint new ones.
- docs/conventions.md#errors — configuration errors fail at build time, and no exception message carries a field value.

## Acceptance
- [ ] `AliasRepository` declares exactly `Set<String> equivalents(String normalizedValue)` and `ComparisonCategory relation(String left, String right)`.
- [ ] `equivalents` includes the queried value itself, and returns a singleton set for a value the repository has never seen — never null, never empty.
- [ ] `equivalents` returns an unmodifiable set; a test asserts `add` throws.
- [ ] `relation` returns null when two values are unrelated, and is symmetric: `relation(a, b)` equals `relation(b, a)` for every pair in a group. A test asserts symmetry across a whole fixture group.
- [ ] `relation(x, x)` for a known value is documented and tested — state explicitly whether identity is a relation or null, and hold to it.
- [ ] `DefaultAliasRepository` closes groups transitively **at construction**, not at lookup. A test builds `a↔b` and `a↔c` as separate entries and asserts `equivalents(b)` contains `c`.
- [ ] Merging two groups that share a member yields one group; a test asserts the merged group's size and that every member sees every other.
- [ ] A builder adds groups with a `ComparisonCategory`, and is the only way to construct the default repository. The repository is immutable once built and safe for concurrent reads; the Javadoc says so.
- [ ] When a transitive merge joins groups declared with **different** categories, the resulting relation is resolved by a rule the Javadoc states and a test pins. Do not leave this to insertion order.
- [ ] Construction rejects a null or empty group, a null member and a null category, with `IllegalArgumentException` naming the constraint but never a member value. See the amendment below — the plan originally specified the wrong exception type.
- [ ] Lookups are on already-normalized values; the Javadoc states the repository never applies its own normalization and that a caller passing a raw value will miss (D7).
- [ ] Lookup is not linear in the number of groups; a test constructs a repository with at least a thousand groups and asserts a lookup still resolves. Cite the structure in Javadoc rather than measuring a time.
- [ ] No type, member or Javadoc word in this package names a person, name, address, date of birth or country. Test fixtures use neutral tokens (`alpha`, `beta`); the real tables are task 15's, in the profiles module.

## Planning amendment (at implementation)

The plan specified `EntityResolutionConfigurationException` for construction
failures. That is wrong on two counts, and the acceptance criterion above is
corrected to `IllegalArgumentException`:

- That exception lives in `api/` and is thrown by exactly one class,
  `api/EntityResolverBuilder.java`. Throwing it from `alias/` would add a
  dependency edge from a low-level data package up to the top layer, for no
  gain.
- Every other constructor-time check in core's lower layers uses
  `IllegalArgumentException` — `result/Score`, `evidence/DefaultFieldEvidence`,
  `field/SimilarityBands`, `scoring/ScoringResult`, `comparison/TokenSimilarity`.
  A repository builder is not the resolver builder, and
  `docs/conventions.md#errors` scopes the configuration exception to what
  `build()` can check on a resolver.

The privacy half of the criterion is unchanged and still holds: the message
names the constraint, never a member value.

## Out of scope
- Any comparator or pipeline wiring — task 13.
- Real Irish/English or nickname data — task 15, and D19's provenance question stays open regardless.
- Phonetics (D19 leaves whether they ship at all unanswered).
- Loading a repository from a file or any serialization format (D16).
