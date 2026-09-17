package io.github.aindriub.jresolve.result;

/**
 * A candidate a {@code CandidateRule} vetoed, and the rule that vetoed it.
 *
 * <p>A veto drops the candidate rather than scoring it
 * ({@code docs/design-decisions.md#d4}), which is deliberate: a hard rule's
 * rejection is final, and routing its partial evidence to a scorer would let a
 * vetoed candidate score above the match threshold. The cost of dropping is
 * that the candidate becomes invisible, so a consumer cannot tell one that
 * scored badly from one that was never scored at all. This type is that cost
 * being paid back.
 *
 * <p><strong>Why the rule is identified by position.</strong> {@code ruleKey}
 * is {@code resolver.rule.<i>n</i>}, where <i>n</i> is the rule's index in the
 * order the resolver was configured with. A rule's class name would read
 * better and is not usable: a {@code CandidateRule} is a functional interface,
 * every rule in this library and its tests is written as a lambda, and a
 * lambda's generated class name is not stable across builds. Position is
 * stable, and it is something the consumer chose. If named rules are wanted
 * later, a {@code default} accessor on {@code CandidateRule} is an additive
 * change that can be made then.
 *
 * <p>Carries no field value, no prepared value and no frequency key, per
 * {@code docs/design-decisions.md#d10}. The candidate is the consumer's own
 * object and is returned as given; it is deliberately excluded from
 * {@link #toString()}, whose output a consumer may log.
 */
public final class RejectedCandidate<C> {

    private final C candidate;
    private final String ruleKey;

    /**
     * @param candidate the vetoed candidate; never null
     * @param ruleKey a stable identifier for the rule that vetoed it; never
     *     null and never a rendered message
     */
    public RejectedCandidate(C candidate, String ruleKey) {
        if (candidate == null) {
            throw new IllegalArgumentException("candidate must not be null");
        }
        if (ruleKey == null) {
            throw new IllegalArgumentException("ruleKey must not be null");
        }
        this.candidate = candidate;
        this.ruleKey = ruleKey;
    }

    /** The vetoed candidate, as the consumer supplied it. */
    public C getCandidate() {
        return candidate;
    }

    /**
     * A stable identifier for the rule that vetoed this candidate, of the form
     * {@code resolver.rule.<i>n</i>}. A key, never a rendered message: a
     * message would carry values D10 keeps out of anything a consumer logs.
     */
    public String getRuleKey() {
        return ruleKey;
    }

    @Override
    public String toString() {
        // The candidate is deliberately excluded: it is a consumer type whose
        // toString() may carry a field value, as ScoredCandidate:49 notes.
        return "RejectedCandidate{ruleKey=" + ruleKey + '}';
    }
}
