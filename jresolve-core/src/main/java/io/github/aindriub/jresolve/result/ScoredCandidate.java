package io.github.aindriub.jresolve.result;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * One candidate together with its score and the field-by-field contributions
 * that produced it.
 *
 * @param <C> the candidate type; owned entirely by the consumer
 */
public final class ScoredCandidate<C> {

    private final C candidate;
    private final Score score;
    private final List<FieldContribution> contributions;

    public ScoredCandidate(C candidate, Score score, List<FieldContribution> contributions) {
        if (score == null) {
            throw new IllegalArgumentException("score must not be null");
        }
        if (contributions == null) {
            throw new IllegalArgumentException("contributions must not be null");
        }
        this.candidate = candidate;
        this.score = score;
        this.contributions = Collections.unmodifiableList(new ArrayList<FieldContribution>(contributions));
    }

    public C getCandidate() {
        return candidate;
    }

    public Score getScore() {
        return score;
    }

    /**
     * The per-field contributions, in the order an explanation should read
     * them.
     */
    public List<FieldContribution> getContributions() {
        return contributions;
    }

    @Override
    public String toString() {
        // The candidate itself is deliberately excluded: it is a consumer type
        // whose toString() may carry a field value.
        return "ScoredCandidate{score=" + score + ", contributions=" + contributions + '}';
    }
}
