/**
 * Per-field preparation and comparison, the building blocks of a pipeline.
 *
 * <p>A {@link io.github.aindriub.jresolve.field.FieldPipeline} has two halves:
 * {@code prepare}, run once per value on the extract side, and {@code compare},
 * the hot loop. {@link io.github.aindriub.jresolve.field.DefaultFieldPipeline}
 * joins a {@link io.github.aindriub.jresolve.field.FieldNormalizer} to a
 * {@link io.github.aindriub.jresolve.field.FieldComparator}.
 * {@link io.github.aindriub.jresolve.field.FieldDefinition} pairs a source
 * extractor and a candidate extractor that converge on one normalized type.
 *
 * <p>Ready-made comparators are
 * {@link io.github.aindriub.jresolve.field.ExactFieldComparator},
 * {@link io.github.aindriub.jresolve.field.SimilarityFieldComparator} (banded by
 * {@link io.github.aindriub.jresolve.field.SimilarityBands}),
 * {@link io.github.aindriub.jresolve.field.AliasAwareFieldComparator} and
 * {@link io.github.aindriub.jresolve.field.TokenSubsumptionComparator}.
 * {@link io.github.aindriub.jresolve.field.CostTiers} names the cost tiers that
 * order comparison.
 */
package io.github.aindriub.jresolve.field;
