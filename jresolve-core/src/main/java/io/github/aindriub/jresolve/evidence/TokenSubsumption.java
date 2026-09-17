package io.github.aindriub.jresolve.evidence;

/**
 * Whether one side's tokens are contained in the other's.
 *
 * <p>This exists because containment and conflict are different findings, and
 * a symmetric similarity score cannot tell them apart. A value that is
 * <em>less specific</em> than the one it is compared against does not
 * contradict it — the shorter value omits detail the longer one carries — yet
 * a raw string similarity scores that pair much the same as two values that
 * genuinely disagree.
 *
 * <p>Worked example, in neutral tokens. Source {@code "alpha"} against two
 * candidates {@code "alpha bravo"} and {@code "alpha charlie"}: both
 * candidates contain the source and neither contradicts it, so both report
 * {@code RIGHT_SUBSUMES_LEFT}. That is why the two candidates are hard to
 * separate, and recording it is what lets a consumer see the ambiguity is
 * genuine rather than an artefact of the metric.
 *
 * <p>Unlike {@link ComparisonCategory}, this set is closed: it enumerates the
 * four possible containment relations between two sets plus the case where no
 * containment was computed, and nothing keys a scoring model on it. An enum
 * is therefore the right shape where a category is not.
 */
public enum TokenSubsumption {

    /**
     * No subsumption was computed. This is the default for every comparator
     * that does not reason about tokens at all, and it is <em>not</em> a
     * finding about the values — contrast {@link #NEITHER}, which is.
     */
    NOT_APPLICABLE,

    /**
     * Subsumption was computed and neither side contains the other. The two
     * token sets are disjoint, or they overlap only partially.
     */
    NEITHER,

    /**
     * Every token on the left is present on the right, and the right carries
     * at least one the left does not. The left is the less specific value.
     */
    LEFT_SUBSUMES_RIGHT,

    /**
     * Every token on the right is present on the left, and the left carries
     * at least one the right does not. The right is the less specific value.
     */
    RIGHT_SUBSUMES_LEFT,

    /**
     * Both sides carry exactly the same tokens. Containment holds in both
     * directions, so neither side is the less specific one.
     */
    EQUIVALENT
}
