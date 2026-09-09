package io.github.aindriub.jresolve.result;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionTest {

    @Test
    void hasExactlyTheThreeDeclaredOutcomes() {
        assertThat(Decision.values()).containsExactly(Decision.MATCH, Decision.REVIEW, Decision.NO_MATCH);
    }
}
