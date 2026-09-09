package io.github.aindriub.jresolve.field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import org.junit.jupiter.api.Test;

class SimilarityBandsTest {

    private final SimilarityBands defaults = new SimilarityBands();

    @Test
    void defaultVeryHighIs095() {
        assertThat(defaults.getVeryHigh()).isEqualTo(0.95);
    }

    @Test
    void defaultHighIs085() {
        assertThat(defaults.getHigh()).isEqualTo(0.85);
    }

    @Test
    void defaultMediumIs070() {
        assertThat(defaults.getMedium()).isEqualTo(0.70);
    }

    @Test
    void scoreAtExactly095IsVeryHigh() {
        assertThat(defaults.categoryFor(0.95)).isSameAs(ComparisonCategory.VERY_HIGH);
    }

    @Test
    void scoreJustBelow095IsHigh() {
        assertThat(defaults.categoryFor(0.9499999)).isSameAs(ComparisonCategory.HIGH);
    }

    @Test
    void scoreAtExactly085IsHigh() {
        assertThat(defaults.categoryFor(0.85)).isSameAs(ComparisonCategory.HIGH);
    }

    @Test
    void scoreJustBelow085IsMedium() {
        assertThat(defaults.categoryFor(0.8499999)).isSameAs(ComparisonCategory.MEDIUM);
    }

    @Test
    void scoreAtExactly070IsMedium() {
        assertThat(defaults.categoryFor(0.70)).isSameAs(ComparisonCategory.MEDIUM);
    }

    @Test
    void scoreJustBelow070IsLow() {
        assertThat(defaults.categoryFor(0.6999999)).isSameAs(ComparisonCategory.LOW);
    }

    @Test
    void constructorRejectsNonDescendingThresholds() {
        assertThatThrownBy(() -> new SimilarityBands(0.80, 0.85, 0.70, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorRejectsAThresholdAboveOne() {
        assertThatThrownBy(() -> new SimilarityBands(1.5, 0.85, 0.70, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorRejectsAThresholdBelowZero() {
        assertThatThrownBy(() -> new SimilarityBands(0.95, 0.85, 0.70, -0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorAcceptsCustomStrictlyDescendingThresholds() {
        SimilarityBands custom = new SimilarityBands(0.99, 0.90, 0.80, 0.10);

        assertThat(custom.getVeryHigh()).isEqualTo(0.99);
        assertThat(custom.getHigh()).isEqualTo(0.90);
        assertThat(custom.getMedium()).isEqualTo(0.80);
        assertThat(custom.getLow()).isEqualTo(0.10);
    }
}
