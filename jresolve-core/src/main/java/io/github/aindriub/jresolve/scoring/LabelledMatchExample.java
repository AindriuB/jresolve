package io.github.aindriub.jresolve.scoring;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One historical pair, its features, and whether it was a match.
 *
 * <p>The library does not train. This type exists so that a consumer who
 * accumulates labelled decisions can hold them in a shape a future estimator
 * will accept, rather than inventing one and discovering later that it does
 * not fit.
 *
 * <p><strong>Named for what it is, not for what the specification called
 * it.</strong> The original design has a single {@code MatchTrainingExample}
 * carrying features plus a boolean. There are two types here — this one and
 * {@link UnlabelledMatchExample} — so neither can be "the" training example,
 * and a name that implied otherwise would mislead. A reader holding the
 * specification should look for both.
 *
 * <p>Immutable; the feature map is copied at construction.
 */
public final class LabelledMatchExample {

    private final Map<String, Double> features;
    private final boolean match;

    /**
     * @param features the feature vector, never null and never containing a
     *                 null name or value
     * @param match    whether this pair was a true match
     * @throws IllegalArgumentException if {@code features} is null or
     *     contains a null
     */
    public LabelledMatchExample(Map<String, Double> features, boolean match) {
        this.features = copyOf(features);
        this.match = match;
    }

    static Map<String, Double> copyOf(Map<String, Double> features) {
        if (features == null) {
            throw new IllegalArgumentException("features must not be null");
        }
        Map<String, Double> copy = new LinkedHashMap<>(features);
        for (Map.Entry<String, Double> entry : copy.entrySet()) {
            if (entry.getKey() == null) {
                throw new IllegalArgumentException("features must not contain a null name");
            }
            if (entry.getValue() == null) {
                throw new IllegalArgumentException("features must not contain a null value");
            }
        }
        return Collections.unmodifiableMap(copy);
    }

    /** The feature vector, unmodifiable. */
    public Map<String, Double> getFeatures() {
        return features;
    }

    public boolean isMatch() {
        return match;
    }

    @Override
    public String toString() {
        // Feature names and a label only. A feature name is a field name,
        // not a compared value, and no value appears.
        return "LabelledMatchExample{features=" + features.keySet() + ", match=" + match + '}';
    }
}
