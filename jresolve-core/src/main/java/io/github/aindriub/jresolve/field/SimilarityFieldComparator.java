package io.github.aindriub.jresolve.field;

import io.github.aindriub.jresolve.comparison.SimilarityMetric;
import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.DefaultFieldEvidence;
import io.github.aindriub.jresolve.evidence.FieldEvidence;

/**
 * Compares two normalized strings with a {@link SimilarityMetric} and bands
 * the result with {@link SimilarityBands}.
 *
 * <p>Returns {@code EXACT} for equal inputs and otherwise the band category
 * for the raw score, plus the shared null handling from {@link
 * AbstractNullSafeFieldComparator}. The raw similarity is always carried on
 * {@link FieldEvidence#getSimilarity()}. The frequency key is set only on
 * {@code EXACT}, since a banded, non-exact match did not produce a single
 * agreed value. Symmetry is inherited from the injected {@link
 * SimilarityMetric}: this comparator adds no asymmetric step of its own.
 */
public final class SimilarityFieldComparator extends AbstractNullSafeFieldComparator<String> {

    private final SimilarityMetric metric;
    private final SimilarityBands bands;

    public SimilarityFieldComparator(SimilarityMetric metric, SimilarityBands bands) {
        if (metric == null) {
            throw new IllegalArgumentException("metric must not be null");
        }
        if (bands == null) {
            throw new IllegalArgumentException("bands must not be null");
        }
        this.metric = metric;
        this.bands = bands;
    }

    @Override
    FieldEvidence compareNonNull(String left, String right) {
        double similarity = metric.similarity(left, right);
        if (left.equals(right)) {
            return new DefaultFieldEvidence(ComparisonCategory.EXACT, similarity, left);
        }
        ComparisonCategory category = bands.categoryFor(similarity);
        return new DefaultFieldEvidence(category, similarity, null);
    }
}
