/**
 * Alias lookup: which normalized values count as equivalent to a given value.
 *
 * <p>Start from {@link io.github.aindriub.jresolve.alias.AliasRepository},
 * which answers "what is equivalent to this normalized value", and its
 * in-memory implementation
 * {@link io.github.aindriub.jresolve.alias.DefaultAliasRepository}. Aliases are
 * equivalence groups closed transitively at construction, not directed pairs.
 * The strength of an alias is not stored here; the kind of alias is a
 * {@link io.github.aindriub.jresolve.evidence.ComparisonCategory} and the
 * scoring model assigns its weight.
 */
package io.github.aindriub.jresolve.alias;
