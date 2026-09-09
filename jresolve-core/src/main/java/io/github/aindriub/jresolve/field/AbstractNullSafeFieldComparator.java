package io.github.aindriub.jresolve.field;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.DefaultFieldEvidence;
import io.github.aindriub.jresolve.evidence.FieldEvidence;

/**
 * Applies the shared null rule every comparator in this package follows —
 * both-null maps to {@code MISSING_BOTH} and exactly-one-null maps to
 * {@code MISSING_ONE} — so each concrete comparator only has to implement
 * the non-null case.
 *
 * @param <N> the normalized field value type
 */
abstract class AbstractNullSafeFieldComparator<N> implements FieldComparator<N> {

    @Override
    public final FieldEvidence compare(N left, N right) {
        if (left == null && right == null) {
            return new DefaultFieldEvidence(ComparisonCategory.MISSING_BOTH, null, null);
        }
        if (left == null || right == null) {
            return new DefaultFieldEvidence(ComparisonCategory.MISSING_ONE, null, null);
        }
        return compareNonNull(left, right);
    }

    /**
     * Compares two values known not to be {@code null}.
     */
    abstract FieldEvidence compareNonNull(N left, N right);
}
