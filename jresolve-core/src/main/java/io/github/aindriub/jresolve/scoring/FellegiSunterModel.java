package io.github.aindriub.jresolve.scoring;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import java.util.Collection;
import java.util.Set;

/**
 * The {@code m} and {@code u} probabilities a Fellegi-Sunter scorer weighs,
 * plus the prior odds that would turn its output into a posterior.
 *
 * <pre>
 *   m = P(category | true match)
 *   u = P(category | non-match)
 *   weight = log2(m / u)
 * </pre>
 *
 * <h2>The assumption this model rests on</h2>
 *
 * <p>Fellegi-Sunter assumes the fields are <strong>conditionally independent
 * given match status</strong>, and summing per-field weights is exactly that
 * assumption being used. It is frequently false. Two fields that co-vary
 * within a household are not independent, so a model over both counts the
 * shared signal twice and is systematically overconfident.
 *
 * <p>The library cannot fix that and must not hide it. What it offers is
 * {@link #compositeGroups()}: a consumer who knows two fields co-vary can
 * declare them one comparison, and a scorer weighs the group once. See
 * {@code docs/calibration.md}.
 *
 * <h2>Missingness is declared, never inferred</h2>
 *
 * <p>A missing value is not automatically a non-match. Every category a
 * comparator can emit — including {@code MISSING_ONE} and
 * {@code MISSING_BOTH} — is either configured with its own {@code m} and
 * {@code u}, or explicitly {@linkplain #isIgnored ignored}. A category that
 * is neither is a configuration error, not a silent zero.
 */
public interface FellegiSunterModel {

    /**
     * P(category | true match) for one field.
     *
     * @throws IllegalStateException if the pair is neither configured nor ignored
     */
    double mProbability(String field, ComparisonCategory category);

    /**
     * P(category | non-match) for one field, adjusted for how common the
     * agreed value is.
     *
     * <p>The frequency key is {@code FieldEvidence.getFrequencyKey()}: the
     * agreed normalized value where the comparison agreed, and null where it
     * did not. Where a model carries a {@link TermFrequencyTable} and the key
     * is non-null, {@code u} is derived from the observed frequency of that
     * value rather than a flat configured figure — which is the whole point,
     * because agreement on a common value is weak evidence and agreement on a
     * rare one is strong.
     *
     * <p>A null key falls back to the configured flat {@code u}: a comparison
     * that did not agree has no value to be common or rare. A model with no
     * frequency table ignores the argument entirely.
     *
     * <p>The argument is in the signature even for models that ignore it,
     * deliberately: a caller must not have to know which kind of model it is
     * holding in order to call this correctly.
     *
     * @throws IllegalStateException if the pair is neither configured nor ignored
     */
    double uProbability(String field, ComparisonCategory category, String frequencyKey);

    /**
     * Whether this field and category are declared to carry no evidence.
     *
     * <p>A scorer skips an ignored pair rather than weighing it. This is how
     * a consumer says "a missing value here tells us nothing" without having
     * to invent {@code m} and {@code u} figures that say the same thing less
     * clearly.
     */
    boolean isIgnored(String field, ComparisonCategory category);

    /**
     * Whether prior odds were configured.
     *
     * <p>Separate from {@link #priorOdds()} so that "nobody said" is
     * distinguishable from "the odds are even". Without this a model with no
     * prior would be indistinguishable from one asserting a 1:1 base rate,
     * and a scorer would happily publish a posterior nobody stood behind.
     */
    boolean hasPriorOdds();

    /**
     * The prior odds of a match before any evidence.
     *
     * @throws IllegalStateException if {@link #hasPriorOdds()} is false
     */
    double priorOdds();

    /**
     * Groups of fields a consumer has declared to be one comparison, because
     * they are not conditionally independent.
     *
     * @return the declared groups, never null; empty when none were declared
     */
    Collection<Set<String>> compositeGroups();
}
