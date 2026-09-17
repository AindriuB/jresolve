package io.github.aindriub.jresolve.scoring;

/**
 * How a declared composite group combines its members' weights into the one
 * weight the group contributes.
 *
 * <p>A composite exists because its members are not conditionally independent,
 * so summing them would count a shared signal more than once. What it should
 * count instead is a statistical judgement rather than an engineering one, and
 * this enum is where that judgement is stated.
 *
 * <p><strong>There is no measurement procedure for this choice.</strong>
 * {@code u} can be measured from a corpus and {@code m} estimated by
 * expectation-maximisation, but nothing tells a consumer which of these rules
 * matches their data. A consumer who has not measured the within-group
 * correlation should leave the default alone — see {@code docs/calibration.md}.
 */
public enum CompositeRule {

    /**
     * The group contributes its least favourable member's weight.
     *
     * <p>The default, and the conservative answer: when the scorer cannot know
     * how much of the signal is shared, the group claims no more than its
     * weakest member. Overconfidence is the failure mode a composite exists to
     * avoid, so under-claiming is the safe direction to be wrong in.
     */
    SMALLEST,

    /**
     * The group contributes the arithmetic mean of its present members'
     * weights.
     *
     * <p>Treats the members as partially independent — reasonable where a
     * consumer has measured the correlation and found it moderate, and
     * overconfident where they have not.
     */
    AVERAGE,

    /**
     * The group contributes its most favourable member's weight.
     *
     * <p>The least conservative of the three: it claims the group is as strong
     * as its best member, which is only defensible where the shared signal is
     * known to be small.
     */
    STRONGEST
}
