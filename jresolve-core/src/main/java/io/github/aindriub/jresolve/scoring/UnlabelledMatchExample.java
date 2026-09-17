package io.github.aindriub.jresolve.scoring;

import java.util.Map;

/**
 * One historical pair and its features, with no label.
 *
 * <p><strong>Why this exists.</strong> A labelled-only representation quietly
 * blocks the standard way Fellegi-Sunter models are actually fitted. In
 * practice {@code m} — P(agreement | true match) — is estimated by
 * expectation-maximisation over <em>unlabelled</em> pairs, precisely because
 * labels are the expensive thing nobody has enough of. A library that could
 * only express labelled examples would hand a consumer a representation that
 * fits the estimation method they are least likely to be able to use.
 *
 * <p>So this is not a redundant cousin of {@link LabelledMatchExample}. It is
 * the one an EM estimator consumes; the labelled form is for supervised
 * fitting and for evaluation.
 *
 * <p>See {@code docs/calibration.md} for which part of a model is expected to
 * come from which method.
 *
 * <p>Immutable; the feature map is copied at construction.
 */
public final class UnlabelledMatchExample {

    private final Map<String, Double> features;

    /**
     * @param features the feature vector, never null and never containing a
     *                 null name or value
     * @throws IllegalArgumentException if {@code features} is null or
     *     contains a null
     */
    public UnlabelledMatchExample(Map<String, Double> features) {
        this.features = LabelledMatchExample.copyOf(features);
    }

    /** The feature vector, unmodifiable. */
    public Map<String, Double> getFeatures() {
        return features;
    }

    @Override
    public String toString() {
        return "UnlabelledMatchExample{features=" + features.keySet() + '}';
    }
}
