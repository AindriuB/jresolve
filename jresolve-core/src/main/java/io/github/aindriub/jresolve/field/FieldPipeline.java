package io.github.aindriub.jresolve.field;

import io.github.aindriub.jresolve.evidence.FieldEvidence;

/**
 * How one field is prepared and compared, split into two phases so that
 * preparation happens once per record while comparison happens once per
 * pair.
 *
 * <p>A resolver calls {@link #prepare(Object)} once per source and once per
 * candidate, then calls {@link #compare(Object, Object)} for every candidate
 * pairing without repeating the work {@code prepare} already did. Collapsing
 * the two phases back into a single {@code compare(V, V)} would repeat
 * normalization on the source side for every candidate, so the split is the
 * point of this interface and must not be undone by a caller that only ever
 * calls both together.
 *
 * @param <V> the raw field value type, as extracted from a record
 * @param <N> the normalized field value type the comparator operates on
 */
public interface FieldPipeline<V, N> {

    /**
     * Normalizes a raw value into the form {@link #compare(Object, Object)}
     * operates on.
     *
     * <p>Null-safe, deterministic — the same input always produces an equal
     * output — and idempotent in the sense that preparing an already-prepared
     * value again yields an equal result whenever {@code V} and {@code N} are
     * the same type; where they differ, determinism is the property that is
     * actually testable.
     *
     * @param value the raw value, may be {@code null}
     * @return the normalized value; may be {@code null} if {@code value} is
     *     {@code null}
     */
    N prepare(V value);

    /**
     * Compares two already-prepared values and returns the evidence.
     *
     * <p>Never throws on a {@code null} argument: null is data, and maps to
     * {@code MISSING_ONE} or {@code MISSING_BOTH}.
     *
     * @param left the prepared source-side value, may be {@code null}
     * @param right the prepared candidate-side value, may be {@code null}
     * @return the evidence produced by the comparison; never {@code null}
     */
    FieldEvidence compare(N left, N right);
}
