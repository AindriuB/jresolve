package io.github.aindriub.jresolve.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * {@link FieldEvidence#getSubsumption()} is a {@code default} method, so
 * adding it must not break an implementation written before it existed.
 */
class FieldEvidenceDefaultsTest {

    /**
     * An implementation declaring only the three original getters — the shape
     * every implementor had before the subsumption signal was added, including
     * any written outside this library.
     */
    private static FieldEvidence legacyImplementation() {
        return new FieldEvidence() {
            @Override
            public ComparisonCategory getCategory() {
                return ComparisonCategory.EXACT;
            }

            @Override
            public Double getSimilarity() {
                return null;
            }

            @Override
            public String getFrequencyKey() {
                return null;
            }
        };
    }

    @Test
    void anImplementationPredatingTheSignalStillCompilesAndAnswers() {
        assertThat(legacyImplementation().getCategory()).isSameAs(ComparisonCategory.EXACT);
    }

    @Test
    void theDefaultIsNotApplicableRatherThanNeither() {
        // NEITHER would assert that containment was computed and did not hold,
        // which is a claim this implementation never made.
        assertThat(legacyImplementation().getSubsumption())
                .isSameAs(TokenSubsumption.NOT_APPLICABLE);
    }

    @Test
    void theDefaultIsNeverNull() {
        assertThat(legacyImplementation().getSubsumption()).isNotNull();
    }
}
