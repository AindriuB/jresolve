package io.github.aindriub.jresolve.comparison;

/**
 * Jaro-Winkler similarity: the Jaro measure of matching characters and
 * transpositions, boosted for a shared leading prefix.
 *
 * <p>Bounded in {@code [0.0, 1.0]}, exactly symmetric and {@code null}-safe:
 * a {@code null} argument is treated as the empty string.
 */
public final class JaroWinklerSimilarity implements SimilarityMetric {

    private static final double DEFAULT_PREFIX_SCALE = 0.1;
    private static final int DEFAULT_MAX_PREFIX_LENGTH = 4;
    private static final double DEFAULT_BOOST_THRESHOLD = 0.7;

    private final double prefixScale;
    private final int maxPrefixLength;
    private final double boostThreshold;

    public JaroWinklerSimilarity() {
        this(DEFAULT_PREFIX_SCALE, DEFAULT_MAX_PREFIX_LENGTH, DEFAULT_BOOST_THRESHOLD);
    }

    public JaroWinklerSimilarity(double prefixScale, int maxPrefixLength, double boostThreshold) {
        if (prefixScale < 0.0 || prefixScale > 0.25) {
            throw new IllegalArgumentException(
                    "prefixScale must be within [0, 0.25], was " + prefixScale);
        }
        if (maxPrefixLength < 0) {
            throw new IllegalArgumentException(
                    "maxPrefixLength must not be negative, was " + maxPrefixLength);
        }
        if (boostThreshold < 0.0 || boostThreshold > 1.0) {
            throw new IllegalArgumentException(
                    "boostThreshold must be within [0, 1], was " + boostThreshold);
        }
        this.prefixScale = prefixScale;
        this.maxPrefixLength = maxPrefixLength;
        this.boostThreshold = boostThreshold;
    }

    @Override
    public double similarity(String left, String right) {
        String a = left == null ? "" : left;
        String b = right == null ? "" : right;

        if (a.isEmpty() && b.isEmpty()) {
            return 1.0;
        }
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        if (a.equals(b)) {
            return 1.0;
        }

        double jaro = jaro(a, b);
        if (jaro <= boostThreshold) {
            return jaro;
        }

        int prefixLength = commonPrefixLength(a, b, maxPrefixLength);
        return jaro + prefixLength * prefixScale * (1.0 - jaro);
    }

    private static double jaro(String a, String b) {
        int aLength = a.length();
        int bLength = b.length();

        int matchWindow = Math.max(0, Math.max(aLength, bLength) / 2 - 1);

        boolean[] aMatched = new boolean[aLength];
        boolean[] bMatched = new boolean[bLength];

        int matches = 0;
        for (int i = 0; i < aLength; i++) {
            int start = Math.max(0, i - matchWindow);
            int end = Math.min(bLength - 1, i + matchWindow);
            for (int j = start; j <= end; j++) {
                if (bMatched[j] || a.charAt(i) != b.charAt(j)) {
                    continue;
                }
                aMatched[i] = true;
                bMatched[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) {
            return 0.0;
        }

        double transpositions = 0.0;
        int bIndex = 0;
        for (int i = 0; i < aLength; i++) {
            if (!aMatched[i]) {
                continue;
            }
            while (!bMatched[bIndex]) {
                bIndex++;
            }
            if (a.charAt(i) != b.charAt(bIndex)) {
                transpositions++;
            }
            bIndex++;
        }
        transpositions /= 2.0;

        double m = matches;
        return (m / aLength + m / bLength + (m - transpositions) / m) / 3.0;
    }

    private static int commonPrefixLength(String a, String b, int limit) {
        int max = Math.min(limit, Math.min(a.length(), b.length()));
        int length = 0;
        while (length < max && a.charAt(length) == b.charAt(length)) {
            length++;
        }
        return length;
    }
}
