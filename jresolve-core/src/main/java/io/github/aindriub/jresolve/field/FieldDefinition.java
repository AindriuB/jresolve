package io.github.aindriub.jresolve.field;

import java.util.function.Function;

/**
 * Describes one field to compare between a source and a candidate: how to
 * pull the value out of each side, how to compare the results once
 * normalized, and where the field fits in cost ordering and requiredness.
 *
 * <p>The two preparers converge on a common normalized type {@code N} rather
 * than requiring both sides to expose the same raw type — the source and the
 * candidate frequently model the same field differently (a single combined
 * string on one side, two separate fields on the other), and forcing a
 * shared raw type would make that unrepresentable.
 *
 * @param <S> the source record type
 * @param <C> the candidate record type
 * @param <N> the normalized field value type
 */
public final class FieldDefinition<S, C, N> {

    private final String name;
    private final Function<S, N> sourcePreparer;
    private final Function<C, N> candidatePreparer;
    private final FieldComparator<N> comparator;
    private final int cost;
    private final boolean required;

    public FieldDefinition(
            String name,
            Function<S, N> sourcePreparer,
            Function<C, N> candidatePreparer,
            FieldComparator<N> comparator,
            int cost,
            boolean required) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (sourcePreparer == null) {
            throw new IllegalArgumentException("sourcePreparer must not be null");
        }
        if (candidatePreparer == null) {
            throw new IllegalArgumentException("candidatePreparer must not be null");
        }
        if (comparator == null) {
            throw new IllegalArgumentException("comparator must not be null");
        }
        this.name = name;
        this.sourcePreparer = sourcePreparer;
        this.candidatePreparer = candidatePreparer;
        this.comparator = comparator;
        this.cost = cost;
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public Function<S, N> getSourcePreparer() {
        return sourcePreparer;
    }

    public Function<C, N> getCandidatePreparer() {
        return candidatePreparer;
    }

    public FieldComparator<N> getComparator() {
        return comparator;
    }

    public int getCost() {
        return cost;
    }

    public boolean isRequired() {
        return required;
    }
}
