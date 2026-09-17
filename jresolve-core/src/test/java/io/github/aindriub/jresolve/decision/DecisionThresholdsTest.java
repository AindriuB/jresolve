package io.github.aindriub.jresolve.decision;

import io.github.aindriub.jresolve.result.ScoreScale;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DecisionThresholdsTest {

    @Test
    void exposesTheConfiguredValues() {
        DecisionThresholds thresholds = new DecisionThresholds(10.0, 5.0, 2.0, ScoreScale.POINTS);

        assertThat(thresholds.getMatchThreshold()).isEqualTo(10.0);
        assertThat(thresholds.getReviewThreshold()).isEqualTo(5.0);
        assertThat(thresholds.getMinimumMargin()).isEqualTo(2.0);
        assertThat(thresholds.getScale()).isEqualTo(ScoreScale.POINTS);
    }

    @Test
    void rejectsAReviewThresholdAboveTheMatchThreshold() {
        assertThatThrownBy(() -> new DecisionThresholds(5.0, 10.0, 2.0, ScoreScale.POINTS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANegativeMinimumMargin() {
        assertThatThrownBy(() -> new DecisionThresholds(10.0, 5.0, -1.0, ScoreScale.POINTS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANullScale() {
        assertThatThrownBy(() -> new DecisionThresholds(10.0, 5.0, 2.0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allowsAnEqualMatchAndReviewThreshold() {
        DecisionThresholds thresholds = new DecisionThresholds(10.0, 10.0, 0.0, ScoreScale.POINTS);

        assertThat(thresholds.getMatchThreshold()).isEqualTo(thresholds.getReviewThreshold());
    }

    @Test
    void equalsIsByValueAcrossAllFourFields() {
        DecisionThresholds one = new DecisionThresholds(50.0, 20.0, 10.0, ScoreScale.POINTS);
        DecisionThresholds two = new DecisionThresholds(50.0, 20.0, 10.0, ScoreScale.POINTS);

        // Two separately constructed instances describe the same decision
        // rule. build() compares by value precisely so that rebuilding an
        // identical configuration is not mistaken for a misconfiguration.
        assertThat(one).isEqualTo(two).hasSameHashCodeAs(two);
    }

    @Test
    void differingOnAnyFieldIsNotEqual() {
        DecisionThresholds base = new DecisionThresholds(50.0, 20.0, 10.0, ScoreScale.POINTS);

        assertThat(base).isNotEqualTo(new DecisionThresholds(51.0, 20.0, 10.0, ScoreScale.POINTS));
        assertThat(base).isNotEqualTo(new DecisionThresholds(50.0, 21.0, 10.0, ScoreScale.POINTS));
        assertThat(base).isNotEqualTo(new DecisionThresholds(50.0, 20.0, 11.0, ScoreScale.POINTS));
        assertThat(base).isNotEqualTo(new DecisionThresholds(50.0, 20.0, 10.0, ScoreScale.LOG2_LIKELIHOOD_RATIO));
    }

    @Test
    void isNotEqualToOtherTypesOrNull() {
        DecisionThresholds base = new DecisionThresholds(50.0, 20.0, 10.0, ScoreScale.POINTS);

        assertThat(base).isNotEqualTo(null).isNotEqualTo("50.0");
    }
}
