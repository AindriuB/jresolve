/**
 * Turns a score into a match, review or no-match decision.
 *
 * <p>{@link io.github.aindriub.jresolve.decision.MatchDecisionEngine} is the
 * contract; {@link io.github.aindriub.jresolve.decision.ThresholdDecisionEngine}
 * is the implementation, driven by
 * {@link io.github.aindriub.jresolve.decision.DecisionThresholds} (match and
 * review thresholds plus a margin over the runner-up). Thresholds are only
 * meaningful on the scale of the scorer that produced the scores, which the
 * builder checks. The outcome is reported as a
 * {@link io.github.aindriub.jresolve.result.MatchResult}.
 */
package io.github.aindriub.jresolve.decision;
