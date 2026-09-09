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
}
