package io.github.aindriub.jresolve.decision;

import io.github.aindriub.jresolve.result.MatchResult;
import io.github.aindriub.jresolve.result.ScoredCandidate;

import java.util.List;

/**
 * Turns a ranked list of scored candidates into a {@link MatchResult}.
 *
 * <p>Kept separate from {@link io.github.aindriub.jresolve.scoring.MatchScorer}
 * so a scoring model can change without touching how a score becomes a
 * decision.
 *
 * @param <C> the candidate type; owned entirely by the consumer
 */
public interface MatchDecisionEngine<C> {

    /**
     * @param candidates every candidate scored for one source record, in any
     *     order; never null
     * @return the decision; {@code NO_MATCH} with a null match and no score
     *     when {@code candidates} is empty
     */
    MatchResult<C> decide(List<ScoredCandidate<C>> candidates);
}
