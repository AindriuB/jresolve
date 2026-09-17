package io.github.aindriub.jresolve.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenSubsumptionTest {

    @Test
    void enumeratesExactlyTheFiveRelations() {
        assertThat(TokenSubsumption.values())
                .containsExactly(
                        TokenSubsumption.NOT_APPLICABLE,
                        TokenSubsumption.NEITHER,
                        TokenSubsumption.LEFT_SUBSUMES_RIGHT,
                        TokenSubsumption.RIGHT_SUBSUMES_LEFT,
                        TokenSubsumption.EQUIVALENT);
    }

    @Test
    void notApplicableIsDistinctFromNeither() {
        // The distinction carries the whole point of the type: NOT_APPLICABLE
        // means no containment was computed, NEITHER means it was computed and
        // did not hold. Collapsing them would make "this comparator ignores
        // tokens" indistinguishable from "these values genuinely diverge".
        assertThat(TokenSubsumption.NOT_APPLICABLE).isNotEqualTo(TokenSubsumption.NEITHER);
    }
}
