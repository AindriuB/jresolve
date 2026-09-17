package io.github.aindriub.jresolve.profiles.ie;

import io.github.aindriub.jresolve.comparison.DefaultTokenSplitter;
import io.github.aindriub.jresolve.comparison.JaroWinklerSimilarity;
import io.github.aindriub.jresolve.comparison.SimilarityMetric;
import io.github.aindriub.jresolve.comparison.TokenSimilarity;
import io.github.aindriub.jresolve.comparison.TokenSplitter;
import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.DefaultFieldEvidence;
import io.github.aindriub.jresolve.evidence.FieldEvidence;
import io.github.aindriub.jresolve.evidence.TokenSubsumption;
import io.github.aindriub.jresolve.field.AbstractNullSafeFieldComparator;
import io.github.aindriub.jresolve.field.SimilarityBands;
import io.github.aindriub.jresolve.field.TokenSubsumptionComparator;

/**
 * Compares two normalized address lines, containment first and spelling
 * second.
 *
 * <h2>Why two stages</h2>
 *
 * <p>Containment is the finding that matters and a string metric cannot
 * express it. A source reading {@code dublin} against a candidate reading
 * {@code dublin 4} is <em>less specific</em>, not in conflict; scoring that
 * pair on edit distance alone makes the shorter value look like a mismatch.
 * So the first stage is token containment, and its verdict stands whenever it
 * is decisive.
 *
 * <p>But token containment alone is brittle in the opposite direction. Two
 * addresses differing by one mistyped character share no token at all, so a
 * pure set comparison calls them a conflict when they are plainly the same
 * place. The second stage catches that: where containment finds nothing, the
 * values are scored with token-level Jaro-Winkler and banded through
 * {@link SimilarityBands}.
 *
 * <p>So: {@code EXACT} and {@link TokenSubsumptionComparator#SUBSUMED} come
 * straight from the containment stage and are never revisited, as do the two
 * missingness categories. Only {@code PARTIAL_OVERLAP} and {@code CONFLICT}
 * fall through to banding, and the band replaces that category. The
 * subsumption signal from the first stage is carried on the evidence either
 * way, so a consumer can still see that containment was computed and found
 * none.
 *
 * <h2>Reuse rather than reimplementation</h2>
 *
 * <p>Null handling is inherited from {@link AbstractNullSafeFieldComparator}
 * and banding is applied through {@link SimilarityBands#categoryFor}. Both are
 * core's, deliberately: a second null rule or a second set of band bounds
 * written here would compile, pass its own tests, and disagree with every
 * comparator in core at exactly the boundary values.
 *
 * <h2>Symmetry</h2>
 *
 * <p>Asymmetric, like the containment stage it wraps: swapping the arguments
 * swaps the subsumption direction. Categories and similarity are symmetric.
 */
public final class IrishAddressComparator extends AbstractNullSafeFieldComparator<String> {

    private final TokenSubsumptionComparator containment;
    private final SimilarityMetric metric;
    private final SimilarityBands bands;

    /** Uses the default token splitter, token-level Jaro-Winkler and default bands. */
    public IrishAddressComparator() {
        this(new DefaultTokenSplitter(), new SimilarityBands());
    }

    /**
     * @param splitter tokenises each address line
     * @param bands    the thresholds the fallback stage bands against
     * @throws IllegalArgumentException if either argument is null
     */
    public IrishAddressComparator(TokenSplitter splitter, SimilarityBands bands) {
        if (splitter == null) {
            throw new IllegalArgumentException("splitter must not be null");
        }
        if (bands == null) {
            throw new IllegalArgumentException("bands must not be null");
        }
        this.containment = new TokenSubsumptionComparator(splitter);
        this.metric = new TokenSimilarity(new JaroWinklerSimilarity(), splitter);
        this.bands = bands;
    }

    @Override
    protected FieldEvidence compareNonNull(String left, String right) {
        FieldEvidence contained = containment.compare(left, right);
        ComparisonCategory category = contained.getCategory();

        boolean decisive = !category.equals(TokenSubsumptionComparator.PARTIAL_OVERLAP)
                && !category.equals(ComparisonCategory.CONFLICT);
        if (decisive) {
            return contained;
        }

        double similarity = metric.similarity(left, right);
        return new DefaultFieldEvidence(
                bands.categoryFor(similarity), similarity, null, contained.getSubsumption());
    }
}
