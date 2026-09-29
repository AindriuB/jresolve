/**
 * What was observed when two records were compared, before any scoring.
 *
 * <p>{@link io.github.aindriub.jresolve.evidence.FieldEvidence}
 * (implemented by
 * {@link io.github.aindriub.jresolve.evidence.DefaultFieldEvidence}) is one
 * field's outcome: a
 * {@link io.github.aindriub.jresolve.evidence.ComparisonCategory}, a
 * similarity, a frequency key and a token subsumption. The per-field values
 * are gathered into a
 * {@link io.github.aindriub.jresolve.evidence.MatchEvidence}, which scorers
 * consume.
 *
 * <p>Categories are an open, interned value type rather than an enum, so a
 * new comparator can add its own.
 * {@link io.github.aindriub.jresolve.evidence.ComparisonCategory} carries
 * the table mapping each built-in category to the comparator that produces
 * it. Token containment is described by
 * {@link io.github.aindriub.jresolve.evidence.TokenSubsumption}.
 */
package io.github.aindriub.jresolve.evidence;
