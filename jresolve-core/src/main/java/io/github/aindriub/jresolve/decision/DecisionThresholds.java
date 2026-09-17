package io.github.aindriub.jresolve.decision;

import io.github.aindriub.jresolve.result.ScoreScale;

/**
 * The thresholds a {@link MatchDecisionEngine} applies, and the
 * {@link ScoreScale} they were written against.
 *
 * <p>Thresholds are only meaningful on the scale they were authored for: a
 * value of 0.9 means something different as a {@code POINTS} sum than as a
 * {@code PROBABILITY}. This class does not check that a scorer's scale
 * matches its own; that cross-check belongs to whatever assembles a scorer
 * and a decision engine together.
 */
public final class DecisionThresholds {

    private final double matchThreshold;
    private final double reviewThreshold;
    private final double minimumMargin;
    private final ScoreScale scale;

    /**
     * @param matchThreshold the minimum score, on {@code scale}, for a
     *     candidate to be eligible for {@code MATCH}
     * @param reviewThreshold the minimum score, on {@code scale}, for a
     *     candidate to be eligible for {@code REVIEW}; must not exceed
     *     {@code matchThreshold}
     * @param minimumMargin the minimum gap, on {@code scale}, between the
     *     best and second-best score required for {@code MATCH} rather than
     *     {@code REVIEW}; must not be negative
     * @param scale the scale {@code matchThreshold}, {@code reviewThreshold}
     *     and {@code minimumMargin} are expressed on
     * @throws IllegalArgumentException if {@code reviewThreshold} exceeds
     *     {@code matchThreshold}, if {@code minimumMargin} is negative, or if
     *     {@code scale} is null
     */
    public DecisionThresholds(double matchThreshold, double reviewThreshold, double minimumMargin, ScoreScale scale) {
        if (scale == null) {
            throw new IllegalArgumentException("scale must not be null");
        }
        if (reviewThreshold > matchThreshold) {
            throw new IllegalArgumentException("reviewThreshold must not exceed matchThreshold");
        }
        if (minimumMargin < 0) {
            throw new IllegalArgumentException("minimumMargin must not be negative");
        }
        this.matchThreshold = matchThreshold;
        this.reviewThreshold = reviewThreshold;
        this.minimumMargin = minimumMargin;
        this.scale = scale;
    }

    public double getMatchThreshold() {
        return matchThreshold;
    }

    public double getReviewThreshold() {
        return reviewThreshold;
    }

    public double getMinimumMargin() {
        return minimumMargin;
    }

    public ScoreScale getScale() {
        return scale;
    }

    /**
     * Value equality over all four fields.
     *
     * <p>Two separately constructed but identical instances describe the same
     * decision rule, and nothing should treat that as a misconfiguration —
     * which is why {@code build()} compares thresholds by value rather than
     * by identity.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DecisionThresholds)) {
            return false;
        }
        DecisionThresholds that = (DecisionThresholds) other;
        return Double.compare(matchThreshold, that.matchThreshold) == 0
                && Double.compare(reviewThreshold, that.reviewThreshold) == 0
                && Double.compare(minimumMargin, that.minimumMargin) == 0
                && scale == that.scale;
    }

    @Override
    public int hashCode() {
        int result = Double.valueOf(matchThreshold).hashCode();
        result = 31 * result + Double.valueOf(reviewThreshold).hashCode();
        result = 31 * result + Double.valueOf(minimumMargin).hashCode();
        result = 31 * result + scale.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DecisionThresholds{matchThreshold=" + matchThreshold + ", reviewThreshold=" + reviewThreshold
                + ", minimumMargin=" + minimumMargin + ", scale=" + scale + '}';
    }
}
