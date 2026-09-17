package io.github.aindriub.jresolve.result;

import io.github.aindriub.jresolve.scoring.RuleBasedScorer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoreTest {

    @Test
    void exposesValueScaleAlgorithmAndProbability() {
        Score score = new Score(4.2, ScoreScale.LOG2_LIKELIHOOD_RATIO, "fellegi-sunter", 0.87);

        assertThat(score.getValue()).isEqualTo(4.2);
        assertThat(score.getScale()).isEqualTo(ScoreScale.LOG2_LIKELIHOOD_RATIO);
        assertThat(score.getAlgorithm()).isEqualTo("fellegi-sunter");
        assertThat(score.getProbability()).isEqualTo(0.87);
    }

    @Test
    void probabilityIsNullByDefault() {
        Score score = new Score(4.2, ScoreScale.LOG2_LIKELIHOOD_RATIO, "fellegi-sunter", null);

        assertThat(score.getProbability()).isNull();
    }

    @Test
    void rejectsNullScale() {
        assertThatThrownBy(() -> new Score(1.0, null, "algorithm", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullAlgorithm() {
        assertThatThrownBy(() -> new Score(1.0, ScoreScale.POINTS, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("algorithm");
    }

    @Test
    void theRejectionMessageNamesTheConstraintAndNotAValue() {
        assertThatThrownBy(() -> new Score(99.5, ScoreScale.POINTS, null, null))
                .hasMessageNotContaining("99.5");
    }

    @Test
    void theScorerSuppliedAlgorithmIsStillAccepted() {
        // The guard must reject only the unattributed case. RuleBasedScorer
        // stamps a constant on every score it produces, and that path is the
        // one this constructor is actually on in the shipped pipeline.
        Score score = new Score(10.0, ScoreScale.POINTS, RuleBasedScorer.ALGORITHM, null);

        assertThat(score.getAlgorithm()).isEqualTo(RuleBasedScorer.ALGORITHM);
    }

    @Test
    void rejectsProbabilityBelowZero() {
        assertThatThrownBy(() -> new Score(1.0, ScoreScale.PROBABILITY, "algorithm", -0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsProbabilityAboveOne() {
        assertThatThrownBy(() -> new Score(1.0, ScoreScale.PROBABILITY, "algorithm", 1.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsProbabilityAtBoundaries() {
        assertThat(new Score(1.0, ScoreScale.PROBABILITY, "algorithm", 0.0).getProbability()).isEqualTo(0.0);
        assertThat(new Score(1.0, ScoreScale.PROBABILITY, "algorithm", 1.0).getProbability()).isEqualTo(1.0);
    }
}
