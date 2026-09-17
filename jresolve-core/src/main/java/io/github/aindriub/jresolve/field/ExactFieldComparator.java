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
 * <p><strong>{@code N} must have a {@code toString()} consistent with
 * {@code equals}.</strong> The frequency key is derived from
 * {@code toString()}, and a frequency-adjusted model weighs agreement on a
 * common value less than agreement on a rare one — which only works when
 * every occurrence of one value produces the same key. Where it does not,
 * two agreeing pairs yield two different keys and every value looks rare.
 *
 * <p>Two shapes break it. An {@code N} that renders per-instance state — a
 * creation timestamp, a source row number, an identity hash — produces a
 * different key for each instance of one value. So does an {@code N} that
 * overrides {@code equals} without overriding {@code hashCode}, because the
 * inherited {@code toString()} is built from the identity hash. Note that
 * merely inheriting {@code Object.toString()} is *not* enough to break it:
 * a type that honours the {@code equals}/{@code hashCode} contract renders
 * equal instances identically, since the inherited rendering derives from
 * the overridden {@code hashCode}.
 *
 * <p>Nothing in the library can detect any of this, so it is stated here as
 * a requirement on the type parameter rather than checked.
 *
 * @param <N> the normalized field value type, whose {@code toString()} must
 *            be consistent with its {@code equals}
 */
public final class ExactFieldComparator<N> extends AbstractNullSafeFieldComparator<N> {

    @Override
    protected FieldEvidence compareNonNull(N left, N right) {
        if (left.equals(right)) {
            return new DefaultFieldEvidence(ComparisonCategory.EXACT, null, left.toString());
        }
        return new DefaultFieldEvidence(ComparisonCategory.CONFLICT, null, null);
    }
}
