/**
 * String similarity metrics and tokenisation.
 *
 * <p>Implement {@link io.github.aindriub.jresolve.comparison.SimilarityMetric}
 * to add a metric; the shipped ones are
 * {@link io.github.aindriub.jresolve.comparison.JaroWinklerSimilarity},
 * {@link io.github.aindriub.jresolve.comparison.LevenshteinSimilarity} and the
 * token-level {@link io.github.aindriub.jresolve.comparison.TokenSimilarity},
 * which splits with a {@link io.github.aindriub.jresolve.comparison.TokenSplitter}
 * ({@link io.github.aindriub.jresolve.comparison.DefaultTokenSplitter} by
 * default). A similarity is a feature, a measurement in [0,1], not a
 * probability. Metrics are wrapped into comparators by the
 * {@code field} package.
 */
package io.github.aindriub.jresolve.comparison;
