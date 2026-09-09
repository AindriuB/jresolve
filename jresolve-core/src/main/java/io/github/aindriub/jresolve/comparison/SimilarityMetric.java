package io.github.aindriub.jresolve.comparison;

/**
 * A pure function scoring how alike two strings are.
 *
 * <p>{@link #similarity(String, String)} always returns a value in
 * {@code [0.0, 1.0]}, is exactly symmetric — {@code similarity(a, b) ==
 * similarity(b, a)} for every pair, with no tolerance — and never throws on a
 * {@code null} argument. A {@code null} argument is treated as the empty
 * string: whether missingness should count as a match, a mismatch or
 * something else is the concern of the layer that reads a field, not of the
 * metric.
 */
public interface SimilarityMetric {

    double similarity(String left, String right);
}
