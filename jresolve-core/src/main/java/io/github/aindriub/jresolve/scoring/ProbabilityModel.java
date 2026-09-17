package io.github.aindriub.jresolve.scoring;

import java.util.Map;

/**
 * Turns a feature vector into a probability of a true match.
 *
 * <p>An extension point, and <strong>nothing in this library implements
 * it.</strong> See {@link FeatureExtractor} for why the interface ships
 * without an implementation.
 *
 * <p><strong>What "probability" means here.</strong> A number from such a
 * model is a probability <em>under that model</em>. That is a real claim and
 * a narrow one: it says the model, given its training data and its
 * assumptions, assigns this pair that likelihood. It does not say the number
 * is calibrated against reality, and production confidence still depends on
 * whether the training data resembled the data being resolved.
 *
 * <p>So a consumer reading a value from here has a probability, not a
 * guarantee, and the library will not pretend otherwise. See
 * {@code docs/calibration.md}, which keeps similarity, likelihood ratio, raw
 * score, probability and calibrated probability apart, and says what this
 * library will and will not give for each.
 *
 * @see FeatureExtractor
 */
public interface ProbabilityModel {

    /**
     * @param features the feature vector, never null
     * @return a probability within {@code [0, 1]}
     */
    double probabilityOfMatch(Map<String, Double> features);
}
