package io.github.aindriub.jresolve.evidence;

/**
 * The standard {@link FieldEvidence} implementation: a category with an
 * optional similarity and an optional frequency key.
 */
public final class DefaultFieldEvidence implements FieldEvidence {

    private final ComparisonCategory category;
    private final Double similarity;
    private final String frequencyKey;

    public DefaultFieldEvidence(ComparisonCategory category, Double similarity, String frequencyKey) {
        if (category == null) {
            throw new IllegalArgumentException("category must not be null");
        }
        this.category = category;
        this.similarity = similarity;
        this.frequencyKey = frequencyKey;
    }

    @Override
    public ComparisonCategory getCategory() {
        return category;
    }

    @Override
    public Double getSimilarity() {
        return similarity;
    }

    @Override
    public String getFrequencyKey() {
        return frequencyKey;
    }

    @Override
    public String toString() {
        // frequencyKey is deliberately excluded: it may carry a normalized
        // field value, and no field value may reach a toString().
        return "DefaultFieldEvidence{category=" + category + ", similarity=" + similarity + '}';
    }
}
