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
}
