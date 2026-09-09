package io.github.aindriub.jresolve.comparison;

import java.util.List;

/**
 * Splits both strings into tokens with a {@link TokenSplitter} and scores
 * the pair as the symmetric best-match mean: every token on each side is
 * paired with its most similar token on the other side under a delegate
 * {@link SimilarityMetric}, and the mean is taken over the combined token
 * count from both sides. Summing "best match for each left token" and "best
 * match for each right token" before dividing by the combined count — rather
 * than, say, averaging two one-sided means — is what keeps this construction
 * exactly symmetric under swapping the arguments.
 *
 * <p>Bounded in {@code [0.0, 1.0]}, exactly symmetric and {@code null}-safe:
 * a {@code null} argument is treated as the empty string.
 *
 * <p>Thread-safety is inherited from the injected delegate and splitter: this
 * class holds no mutable state of its own, so it is safe for concurrent use
 * exactly when both the delegate {@link SimilarityMetric} and the
 * {@link TokenSplitter} passed to the constructor are.
 */
public final class TokenSimilarity implements SimilarityMetric {

    private final SimilarityMetric delegate;
    private final TokenSplitter splitter;

    public TokenSimilarity(SimilarityMetric delegate, TokenSplitter splitter) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate must not be null");
        }
        if (splitter == null) {
            throw new IllegalArgumentException("splitter must not be null");
        }
        this.delegate = delegate;
        this.splitter = splitter;
    }

    @Override
    public double similarity(String left, String right) {
        List<String> leftTokens = splitter.split(left == null ? "" : left);
        List<String> rightTokens = splitter.split(right == null ? "" : right);

        if (leftTokens.isEmpty() && rightTokens.isEmpty()) {
            return 1.0;
        }

        double total = sumOfBestMatches(leftTokens, rightTokens) + sumOfBestMatches(rightTokens, leftTokens);
        return total / (leftTokens.size() + rightTokens.size());
    }

    private double sumOfBestMatches(List<String> tokens, List<String> otherTokens) {
        double sum = 0.0;
        for (String token : tokens) {
            sum += bestMatch(token, otherTokens);
        }
        return sum;
    }

    private double bestMatch(String token, List<String> otherTokens) {
        double best = 0.0;
        for (String otherToken : otherTokens) {
            double score = delegate.similarity(token, otherToken);
            if (score > best) {
                best = score;
            }
        }
        return best;
    }
}
