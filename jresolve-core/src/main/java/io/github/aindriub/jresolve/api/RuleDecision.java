package io.github.aindriub.jresolve.api;

/**
 * The outcome of evaluating a {@link CandidateRule}: either the candidate is
 * vetoed, or evaluation continues to the next cost tier.
 *
 * <p>This is the whole vocabulary a hard rule has. It does not carry a
 * positive vote, a weight or an explanation — that is {@code MatchRule}'s
 * job, out of scope for this milestone.
 */
public enum RuleDecision {

    /** The candidate is excluded from scoring; no further tiers are compared. */
    REJECT,

    /** The candidate remains under consideration; the next tier proceeds. */
    CONTINUE
}
