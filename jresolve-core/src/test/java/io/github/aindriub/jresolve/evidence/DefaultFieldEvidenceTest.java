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

    @Test
    void theThreeArgumentConstructorDefaultsToNotApplicable() {
        DefaultFieldEvidence evidence =
                new DefaultFieldEvidence(ComparisonCategory.EXACT, null, null);

        assertThat(evidence.getSubsumption()).isSameAs(TokenSubsumption.NOT_APPLICABLE);
    }

    @Test
    void theFourArgumentConstructorCarriesTheSubsumption() {
        DefaultFieldEvidence evidence = new DefaultFieldEvidence(
                ComparisonCategory.MEDIUM, 0.8, null, TokenSubsumption.RIGHT_SUBSUMES_LEFT);

        assertThat(evidence.getSubsumption()).isSameAs(TokenSubsumption.RIGHT_SUBSUMES_LEFT);
    }

    @Test
    void rejectsNullSubsumption() {
        assertThatThrownBy(() -> new DefaultFieldEvidence(ComparisonCategory.EXACT, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subsumption");
    }

    @Test
    void toStringIncludesTheSubsumption() {
        DefaultFieldEvidence evidence = new DefaultFieldEvidence(
                ComparisonCategory.MEDIUM, 0.8, null, TokenSubsumption.EQUIVALENT);

        assertThat(evidence.toString()).contains("EQUIVALENT");
    }

    @Test
    void toStringStillExcludesTheFrequencyKey() {
        // The frequency key is a normalized field value. Adding a field to
        // toString is exactly when this property gets broken by accident.
        DefaultFieldEvidence evidence = new DefaultFieldEvidence(
                ComparisonCategory.EXACT, null, "widget-7", TokenSubsumption.EQUIVALENT);

        assertThat(evidence.toString()).doesNotContain("widget-7");
    }
}
