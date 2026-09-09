package io.github.aindriub.jresolve.normalization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Applies an ordered list of {@link StringNormalizer} stages in sequence.
 */
public final class CompositeNormalizer implements StringNormalizer {

    private final List<StringNormalizer> stages;

    public CompositeNormalizer(List<StringNormalizer> stages) {
        if (stages == null) {
            throw new IllegalArgumentException("stages must not be null");
        }
        List<StringNormalizer> copy = new ArrayList<>(stages);
        for (StringNormalizer stage : copy) {
            if (stage == null) {
                throw new IllegalArgumentException("stages must not contain null");
            }
        }
        this.stages = Collections.unmodifiableList(copy);
    }

    @Override
    public String normalize(String value) {
        if (value == null) {
            return null;
        }
        String result = value;
        for (StringNormalizer stage : stages) {
            result = stage.normalize(result);
        }
        return result;
    }
}
