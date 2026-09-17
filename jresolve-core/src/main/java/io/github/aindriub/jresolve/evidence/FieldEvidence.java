package io.github.aindriub.jresolve.evidence;

/**
 * The evidence produced by comparing one field between two records.
 *
 * <p>Implementations may expose additional, field-type-specific information;
 * this interface only fixes the shape every scorer can rely on.
 */
public interface FieldEvidence {

    /**
     * The outcome of the comparison. Never null.
     */
    ComparisonCategory getCategory();

    /**
     * The raw similarity that produced the category, if the comparator
     * computed one. Null when the category was not derived from a similarity
     * score (for example an exact-match or missingness category).
     */
    Double getSimilarity();

    /**
     * The normalized value the two sides agreed on, if the comparison
     * agreed. Null when the sides did not agree, including every missingness
     * and conflict outcome.
     */
    String getFrequencyKey();

    /**
     * Whether one side's tokens are contained in the other's.
     *
     * <p>Defaults to {@link TokenSubsumption#NOT_APPLICABLE}. A comparator
     * that does not reason about tokens should leave this alone rather than
     * reporting {@link TokenSubsumption#NEITHER}, which asserts that
     * containment was computed and did not hold.
     *
     * <p>This is a {@code default} method so that adding it does not break
     * an existing implementation, including one outside this library.
     *
     * @return the containment relation, never {@code null}
     */
    default TokenSubsumption getSubsumption() {
        return TokenSubsumption.NOT_APPLICABLE;
    }
}
