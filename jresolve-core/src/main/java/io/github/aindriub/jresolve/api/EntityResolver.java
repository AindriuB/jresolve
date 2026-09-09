package io.github.aindriub.jresolve.api;

import io.github.aindriub.jresolve.result.MatchResult;

import java.util.Collection;

/**
 * Resolves one source record against a fixed collection of candidates.
 *
 * @param <S> the source record type; owned entirely by the consumer
 * @param <C> the candidate record type; owned entirely by the consumer
 */
public interface EntityResolver<S, C> {

    /**
     * Compares {@code source} against every candidate and returns the
     * resulting decision.
     *
     * @param source the record to resolve; must not be {@code null}
     * @param candidates every candidate to consider, in any order; must not
     *     be {@code null}. A {@code null} element is skipped rather than
     *     compared.
     * @return the decision produced by the configured decision engine; never
     *     {@code null}
     * @throws IllegalArgumentException if {@code source} or {@code
     *     candidates} is {@code null}
     */
    MatchResult<C> resolve(S source, Collection<C> candidates);
}
