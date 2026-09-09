package io.github.aindriub.jresolve.field;

import io.github.aindriub.jresolve.evidence.FieldEvidence;

/**
 * Composes one {@link FieldNormalizer} and one {@link FieldComparator} into
 * a {@link FieldPipeline}.
 *
 * @param <V> the raw field value type
 * @param <N> the normalized field value type
 */
public final class DefaultFieldPipeline<V, N> implements FieldPipeline<V, N> {

    private final FieldNormalizer<V, N> normalizer;
    private final FieldComparator<N> comparator;

    public DefaultFieldPipeline(FieldNormalizer<V, N> normalizer, FieldComparator<N> comparator) {
        if (normalizer == null) {
            throw new IllegalArgumentException("normalizer must not be null");
        }
        if (comparator == null) {
            throw new IllegalArgumentException("comparator must not be null");
        }
        this.normalizer = normalizer;
        this.comparator = comparator;
    }

    @Override
    public N prepare(V value) {
        return normalizer.normalize(value);
    }

    @Override
    public FieldEvidence compare(N left, N right) {
        return comparator.compare(left, right);
    }
}
