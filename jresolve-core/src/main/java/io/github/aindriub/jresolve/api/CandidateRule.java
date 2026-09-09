package io.github.aindriub.jresolve.api;

import io.github.aindriub.jresolve.evidence.MatchEvidence;

/**
 * A hard veto evaluated between cost tiers, against the evidence gathered so
 * far.
 *
 * <p>Runs after every tier of field comparisons, before scoring. A
 * {@link RuleDecision#REJECT} short-circuits the remaining tiers for that
 * candidate and excludes it from scoring; nothing about the rejection reason
 * survives into the {@link io.github.aindriub.jresolve.result.MatchResult}.
 * Compare with {@code MatchRule}, a soft, weighted vote — out of scope here.
 *
 * @param <S> the source record type; owned entirely by the consumer
 * @param <C> the candidate record type; owned entirely by the consumer
 */
public interface CandidateRule<S, C> {

    /**
     * @param source the source record; never {@code null}
     * @param candidate the candidate under consideration; never {@code null}
     * @param evidence the field evidence gathered through the most recently
     *     completed tier; {@link MatchEvidence#isComplete()} is true only
     *     when every configured field has been compared
     * @return {@link RuleDecision#REJECT} to veto {@code candidate}, or
     *     {@link RuleDecision#CONTINUE} to proceed to the next tier; never
     *     {@code null}
     */
    RuleDecision evaluate(S source, C candidate, MatchEvidence evidence);
}
