package io.github.aindriub.jresolve.field;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;

/**
 * The similarity thresholds {@link SimilarityFieldComparator} bands a raw
 * score into a {@link ComparisonCategory}.
 *
 * <p>Four strictly descending thresholds in {@code [0, 1]}: a score at or
 * above {@code veryHigh} is {@code VERY_HIGH}, at or above {@code high} is
 * {@code HIGH}, at or above {@code medium} is {@code MEDIUM}, and anything
 * below that is {@code LOW}. {@code low} is the floor of the strictly
 * descending sequence rather than a threshold {@code LOW} itself is compared
 * against — everything below {@code medium} is {@code LOW} regardless of its
 * value, but it still participates in the descending-order and range checks.
 *
 * <p>These are engineering defaults, not statistically validated thresholds,
 * and are configurable through the constructor.
 */
public final class SimilarityBands {

    public static final double DEFAULT_VERY_HIGH = 0.95;
    public static final double DEFAULT_HIGH = 0.85;
    public static final double DEFAULT_MEDIUM = 0.70;
    public static final double DEFAULT_LOW = 0.0;

    private final double veryHigh;
    private final double high;
    private final double medium;
    private final double low;

    public SimilarityBands() {
        this(DEFAULT_VERY_HIGH, DEFAULT_HIGH, DEFAULT_MEDIUM, DEFAULT_LOW);
    }

    public SimilarityBands(double veryHigh, double high, double medium, double low) {
        requireInRange(veryHigh, "veryHigh");
        requireInRange(high, "high");
        requireInRange(medium, "medium");
        requireInRange(low, "low");
        if (!(veryHigh > high && high > medium && medium > low)) {
            throw new IllegalArgumentException(
                    "thresholds must be strictly descending: veryHigh > high > medium > low, was "
                            + veryHigh + " > " + high + " > " + medium + " > " + low);
        }
        this.veryHigh = veryHigh;
        this.high = high;
        this.medium = medium;
        this.low = low;
    }

    private static void requireInRange(double threshold, String name) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException(name + " must be within [0, 1], was " + threshold);
        }
    }

    public double getVeryHigh() {
        return veryHigh;
    }

    public double getHigh() {
        return high;
    }

    public double getMedium() {
        return medium;
    }

    public double getLow() {
        return low;
    }

    /**
     * Bands a raw similarity score into a category. Bounds are inclusive at
     * the lower edge of each band.
     */
    ComparisonCategory categoryFor(double similarity) {
        if (similarity >= veryHigh) {
            return ComparisonCategory.VERY_HIGH;
        }
        if (similarity >= high) {
            return ComparisonCategory.HIGH;
        }
        if (similarity >= medium) {
            return ComparisonCategory.MEDIUM;
        }
        return ComparisonCategory.LOW;
    }
}
