package io.github.aindriub.jresolve.field;

/**
 * Converts a raw field value into the normalized type a {@link
 * FieldComparator} operates on.
 *
 * <p>Implementations must be null-safe ({@code null} in, {@code null} out is
 * the typical shape but is not required — a normalizer may map {@code null}
 * to a non-null sentinel if that is meaningful for {@code N}), deterministic,
 * stateless once constructed and safe for concurrent use.
 *
 * @param <V> the raw field value type
 * @param <N> the normalized field value type
 */
@FunctionalInterface
public interface FieldNormalizer<V, N> {

    /**
     * Normalizes {@code value}.
     *
     * @param value the raw value, may be {@code null}
     * @return the normalized value
     */
    N normalize(V value);
}
