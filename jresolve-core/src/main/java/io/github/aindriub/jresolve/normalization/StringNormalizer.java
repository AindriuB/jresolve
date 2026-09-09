package io.github.aindriub.jresolve.normalization;

/**
 * A single stage in a string normalization pipeline.
 *
 * <p>Implementations must be null-safe ({@code null} in, {@code null} out),
 * deterministic — the same input always produces an equal output — and
 * idempotent: applying a stage to its own output must return an equal
 * string. Implementations must be stateless once constructed and safe for
 * concurrent use, and must never mutate the input.
 */
@FunctionalInterface
public interface StringNormalizer {

    /**
     * Normalizes {@code value}.
     *
     * @param value the value to normalize, may be {@code null}
     * @return the normalized value, or {@code null} if {@code value} is
     *     {@code null}
     */
    String normalize(String value);
}
