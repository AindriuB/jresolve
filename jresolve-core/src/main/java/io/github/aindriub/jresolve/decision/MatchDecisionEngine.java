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

    /**
     * The thresholds this engine actually applies, or null if it does not
     * decide by thresholds or declines to say.
     *
     * <p>This exists so that {@link
     * io.github.aindriub.jresolve.api.EntityResolverBuilder#build()} can
     * inspect the object that really decides, rather than only the
     * {@code DecisionThresholds} it was handed separately. Without it, a
     * caller can configure one set of thresholds and construct the engine
     * with another: the build succeeds and every later decision is made
     * against thresholds nobody declared — on the wrong scale entirely, in
     * the worst case.
     *
     * <p><strong>Returning null has a cost.</strong> {@code build()} cannot
     * check an engine that declares nothing, so for such an engine the
     * caller's own discipline is the only thing keeping the configured
     * thresholds and the applied ones in agreement. An engine that can
     * declare its thresholds should.
     *
     * <p>A {@code default} rather than an abstract method, so that adding it
     * does not break an existing implementation.
     *
     * @return the applied thresholds, or null when none are declared
     */
    default DecisionThresholds declaredThresholds() {
        return null;
    }
}
