package io.github.aindriub.jresolve.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Fixtures are neutral tokens; none describes anyone real.
 */
class RejectedCandidateTest {

    private static final class Candidate {
        private final String value;

        private Candidate(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            // A consumer type whose toString carries a field value, which is
            // exactly what D10 keeps out of a logged explanation.
            return "Candidate{" + value + '}';
        }
    }

    @Test
    void carriesTheCandidateAsGiven() {
        Candidate candidate = new Candidate("supersecretvalue");

        assertThat(new RejectedCandidate<Candidate>(candidate, "resolver.rule.0").getCandidate())
                .isSameAs(candidate);
    }

    @Test
    void carriesTheRuleKey() {
        assertThat(new RejectedCandidate<Candidate>(new Candidate("a"), "resolver.rule.2").getRuleKey())
                .isEqualTo("resolver.rule.2");
    }

    // ------------------------------------------------------------------ D10

    @Test
    void toStringCarriesNoFieldValue() {
        // The candidate is the consumer's own type and its toString may carry
        // anything. A rejection is something a consumer logs, so the candidate
        // stays out of it, as ScoredCandidate:49 does for the same reason.
        RejectedCandidate<Candidate> rejected =
                new RejectedCandidate<Candidate>(new Candidate("supersecretvalue"), "resolver.rule.0");

        assertThat(rejected.toString()).doesNotContain("supersecretvalue");
        assertThat(rejected.toString()).contains("resolver.rule.0");
    }

    @Test
    void theRuleKeyIsAKeyRatherThanARenderedMessage() {
        // A rendered message would be the natural place for a field value to
        // reach a log. The key names the rule's position and nothing else.
        RejectedCandidate<Candidate> rejected =
                new RejectedCandidate<Candidate>(new Candidate("supersecretvalue"), "resolver.rule.1");

        assertThat(rejected.getRuleKey()).doesNotContain("supersecretvalue");
        assertThat(rejected.getRuleKey()).matches("resolver\\.rule\\.\\d+");
    }

    // ------------------------------------------------------------ rejection

    @Test
    void rejectsNulls() {
        assertThatThrownBy(() -> new RejectedCandidate<Candidate>(null, "resolver.rule.0"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RejectedCandidate<Candidate>(new Candidate("a"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theRejectionMessageNamesTheConstraintAndNotTheValue() {
        assertThatThrownBy(() -> new RejectedCandidate<Candidate>(new Candidate("a"), null))
                .hasMessageContaining("ruleKey")
                .hasMessageNotContaining("Candidate{");
    }
}
