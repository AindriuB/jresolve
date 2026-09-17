package io.github.aindriub.jresolve.scoring;

import io.github.aindriub.jresolve.evidence.MatchEvidence;
import java.util.Map;

/**
 * Turns comparison evidence into a numeric feature vector.
 *
 * <p>An extension point, and <strong>nothing in this library implements
 * it.</strong> That is deliberate rather than an omission: logistic
 * regression and the other model families this interface exists to admit are
 * held behind v1, while the interface ships so that adding one later does not
 * require changing {@code FieldDefinition}, {@code FieldPipeline} or
 * {@link MatchEvidence}. An extension point added after the fact is an
 * extension point that changes the types it was supposed to leave alone.
 *
 * <p>Consumes {@link MatchEvidence} unchanged. A model sits downstream of
 * comparison and has no business reaching back into how a field was prepared
 * or compared.
 *
 * <p>Note what this interface is <em>not</em> a licence to do: raw metrics
 * become features, and features feed a model, but a similarity summed with
 * another similarity is still not a probability. Jaro-Winkler and Levenshtein
 * over the same pair are correlated measurements, and a feature vector that
 * contains both hands that correlation to the model rather than resolving it.
 *
 * @see ProbabilityModel
 */
public interface FeatureExtractor {

    /**
     * @param evidence the per-field comparison evidence, never null
     * @return a feature vector keyed by feature name, never null
     */
    Map<String, Double> extract(MatchEvidence evidence);
}
