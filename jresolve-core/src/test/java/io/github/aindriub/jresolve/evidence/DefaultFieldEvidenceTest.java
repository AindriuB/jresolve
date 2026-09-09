package io.github.aindriub.jresolve.evidence;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultFieldEvidenceTest {

    @Test
    void exposesCategorySimilarityAndFrequencyKey() {
        DefaultFieldEvidence evidence = new DefaultFieldEvidence(ComparisonCategory.HIGH, 0.91, "agreed-key");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.HIGH);
        assertThat(evidence.getSimilarity()).isEqualTo(0.91);
        assertThat(evidence.getFrequencyKey()).isEqualTo("agreed-key");
    }

    @Test
    void frequencyKeyIsNullWhenComparisonDidNotAgree() {
        DefaultFieldEvidence evidence = new DefaultFieldEvidence(ComparisonCategory.CONFLICT, 0.2, null);

        assertThat(evidence.getFrequencyKey()).isNull();
    }

    @Test
    void similarityIsNullableForNonSimilarityCategories() {
        DefaultFieldEvidence evidence = new DefaultFieldEvidence(ComparisonCategory.MISSING_BOTH, null, null);

        assertThat(evidence.getSimilarity()).isNull();
    }

    @Test
    void rejectsNullCategory() {
        assertThatThrownBy(() -> new DefaultFieldEvidence(null, 0.5, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toStringDoesNotContainFrequencyKey() {
        DefaultFieldEvidence evidence = new DefaultFieldEvidence(ComparisonCategory.EXACT, 1.0, "sensitive-token");

        assertThat(evidence.toString()).doesNotContain("sensitive-token");
    }
}
