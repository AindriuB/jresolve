package io.github.aindriub.jresolve.result;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreScaleTest {

    @Test
    void hasExactlyTheThreeDeclaredScales() {
        assertThat(ScoreScale.values()).containsExactly(
                ScoreScale.LOG2_LIKELIHOOD_RATIO,
                ScoreScale.PROBABILITY,
                ScoreScale.POINTS);
    }
}
