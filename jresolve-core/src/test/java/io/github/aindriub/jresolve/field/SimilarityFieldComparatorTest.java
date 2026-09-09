package io.github.aindriub.jresolve.field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aindriub.jresolve.comparison.SimilarityMetric;
import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.FieldEvidence;
import org.junit.jupiter.api.Test;

class SimilarityFieldComparatorTest {

    /** Returns a fixed score regardless of input, so expectations do not depend on any real metric's arithmetic. */
    private static final class FixedMetric implements SimilarityMetric {
        private final double score;

        FixedMetric(double score) {
            this.score = score;
        }

        @Override
        public double similarity(String left, String right) {
            return score;
        }
    }

    @Test
    void equalInputsAreExactRegardlessOfMetric() {
        SimilarityFieldComparator comparator =
                new SimilarityFieldComparator(new FixedMetric(0.3), new SimilarityBands());

        FieldEvidence evidence = comparator.compare("acme", "acme");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.EXACT);
    }

    @Test
    void exactCarriesTheRawSimilarity() {
        SimilarityFieldComparator comparator =
                new SimilarityFieldComparator(new FixedMetric(0.87), new SimilarityBands());

        FieldEvidence evidence = comparator.compare("acme", "acme");

        assertThat(evidence.getSimilarity()).isEqualTo(0.87);
    }

    @Test
    void exactSetsFrequencyKeyToTheAgreedValue() {
        SimilarityFieldComparator comparator =
                new SimilarityFieldComparator(new FixedMetric(0.5), new SimilarityBands());

        FieldEvidence evidence = comparator.compare("acme", "acme");

        assertThat(evidence.getFrequencyKey()).isEqualTo("acme");
    }

    @Test
    void nonExactBandsToHighAndCarriesSimilarity() {
        SimilarityFieldComparator comparator =
                new SimilarityFieldComparator(new FixedMetric(0.90), new SimilarityBands());

        FieldEvidence evidence = comparator.compare("acme", "acme corp");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.HIGH);
        assertThat(evidence.getSimilarity()).isEqualTo(0.90);
    }

    @Test
    void frequencyKeyIsNullForAHighResult() {
        SimilarityFieldComparator comparator =
                new SimilarityFieldComparator(new FixedMetric(0.90), new SimilarityBands());

        FieldEvidence evidence = comparator.compare("acme", "acme corp");

        assertThat(evidence.getFrequencyKey()).isNull();
    }

    @Test
    void nonExactBandsToLow() {
        SimilarityFieldComparator comparator =
                new SimilarityFieldComparator(new FixedMetric(0.10), new SimilarityBands());

        FieldEvidence evidence = comparator.compare("acme", "zulu");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.LOW);
    }

    @Test
    void bothNullIsMissingBoth() {
        SimilarityFieldComparator comparator =
                new SimilarityFieldComparator(new FixedMetric(1.0), new SimilarityBands());

        FieldEvidence evidence = comparator.compare(null, null);

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.MISSING_BOTH);
    }

    @Test
    void leftNullIsMissingOne() {
        SimilarityFieldComparator comparator =
                new SimilarityFieldComparator(new FixedMetric(1.0), new SimilarityBands());

        FieldEvidence evidence = comparator.compare(null, "acme");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.MISSING_ONE);
    }

    @Test
    void rejectsNullMetric() {
        assertThatThrownBy(() -> new SimilarityFieldComparator(null, new SimilarityBands()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullBands() {
        assertThatThrownBy(() -> new SimilarityFieldComparator(new FixedMetric(1.0), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
