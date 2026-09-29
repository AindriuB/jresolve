/**
 * The outcome of a resolution: the decision, the score and how it was reached.
 *
 * <p>Start from {@link io.github.aindriub.jresolve.result.MatchResult}, which
 * carries a {@link io.github.aindriub.jresolve.result.Decision}, the matched
 * candidate if any, and its {@link io.github.aindriub.jresolve.result.Score};
 * only seven of the possible field combinations are legal. A
 * {@link io.github.aindriub.jresolve.result.Score} records its
 * {@link io.github.aindriub.jresolve.result.ScoreScale} alongside its value.
 * Supporting types are {@link io.github.aindriub.jresolve.result.ScoredCandidate},
 * {@link io.github.aindriub.jresolve.result.RejectedCandidate} and the per-field
 * {@link io.github.aindriub.jresolve.result.FieldContribution}.
 */
package io.github.aindriub.jresolve.result;
