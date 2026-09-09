package io.github.aindriub.jresolve.field;

import io.github.aindriub.jresolve.evidence.FieldEvidence;

/**
 * Compares two already-normalized field values and produces evidence.
 *
 * <p>Implementations must never throw on a {@code null} argument: null is
 * data. Every implementation in this package maps null-left/non-null-right
 * (and its converse) to {@code MISSING_ONE}, and both-null to {@code
 * MISSING_BOTH}. Implementations are not required to be symmetric —
 * {@code compare(a, b)} need not equal {@code compare(b, a)} — and each one
 * documents which it is.
 *
 * @param <N> the normalized field value type
 */
@FunctionalInterface
public interface FieldComparator<N> {

    /**
     * Compares two normalized values.
     *
     * @param left the source-side value, may be {@code null}
     * @param right the candidate-side value, may be {@code null}
     * @return the evidence produced by the comparison; never {@code null}
     */
    FieldEvidence compare(N left, N right);
}
