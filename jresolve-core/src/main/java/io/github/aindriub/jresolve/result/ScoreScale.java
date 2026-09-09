package io.github.aindriub.jresolve.result;

/**
 * The scale a {@link Score}'s value is expressed on. A margin between two
 * scores only has a fixed meaning once the scale is known: a difference of
 * 0.02 is enormous on {@link #PROBABILITY} near 0.99 and negligible near 0.5.
 */
public enum ScoreScale {

    /** A base-2 log likelihood ratio, as produced by Fellegi-Sunter. */
    LOG2_LIKELIHOOD_RATIO,

    /** A calibrated probability in {@code [0, 1]}. */
    PROBABILITY,

    /** An arbitrary points scale defined by a rule-based scorer. */
    POINTS
}
