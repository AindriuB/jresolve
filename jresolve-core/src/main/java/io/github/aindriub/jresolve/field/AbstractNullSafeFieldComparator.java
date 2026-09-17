package io.github.aindriub.jresolve.field;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.DefaultFieldEvidence;
import io.github.aindriub.jresolve.evidence.FieldEvidence;

/**
 * Applies the shared null rule every comparator follows — both-null maps to
 * {@code MISSING_BOTH} and exactly-one-null maps to {@code MISSING_ONE} — so
 * each concrete comparator only has to implement the non-null case.
 *
 * <p>This class is public so that a comparator written outside this package
 * can inherit the rule rather than restate it. A comparator that hand-rolls
 * its own null handling is the defect this base class exists to prevent:
 * null is data, and a {@code NullPointerException} out of a comparator is a
 * defect rather than a caller error.
 *
 * <p>{@link #compare} is {@code final}. The null rule is a contract shared
 * across every comparator, not a default a subclass may reinterpret.
 *
 * @param <N> the normalized field value type
 */
public abstract class AbstractNullSafeFieldComparator<N> implements FieldComparator<N> {

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
     * Compares two values, both guaranteed non-null by {@link #compare}.
     *
     * <p>Implementations must not re-check for null. Doing so is dead code
     * that reads as though null can reach this method, which a later reader
     * may take as licence to return something other than the two missingness
     * categories for a null input.
     *
     * @param left  the prepared source-side value, never {@code null}
     * @param right the prepared candidate-side value, never {@code null}
     * @return evidence for this pair, never {@code null}
     */
    protected abstract FieldEvidence compareNonNull(N left, N right);
}
