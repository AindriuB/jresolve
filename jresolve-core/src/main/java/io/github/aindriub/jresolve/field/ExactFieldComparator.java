package io.github.aindriub.jresolve.field;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.DefaultFieldEvidence;
import io.github.aindriub.jresolve.evidence.FieldEvidence;

/**
 * Compares two normalized values with {@link Object#equals(Object)}.
 *
 * <p>Returns {@code EXACT} when the values are equal and {@code CONFLICT}
 * otherwise, plus the shared null handling from {@link
 * AbstractNullSafeFieldComparator}. The frequency key is set to the agreed
 * value's {@code toString()} on {@code EXACT} and left {@code null}
 * otherwise, since a conflicting or missing pair carries no agreed value.
 * This comparator is exactly symmetric: it only calls {@code equals}, which
 * {@code compare(a, b)} and {@code compare(b, a)} both do the same way.
 *
 * @param <N> the normalized field value type
 */
public final class ExactFieldComparator<N> extends AbstractNullSafeFieldComparator<N> {

    @Override
    FieldEvidence compareNonNull(N left, N right) {
        if (left.equals(right)) {
            return new DefaultFieldEvidence(ComparisonCategory.EXACT, null, left.toString());
        }
        return new DefaultFieldEvidence(ComparisonCategory.CONFLICT, null, null);
    }
}
