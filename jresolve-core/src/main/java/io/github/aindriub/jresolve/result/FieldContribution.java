package io.github.aindriub.jresolve.result;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.TokenSubsumption;

/**
 * How much one field's evidence contributed to a candidate's score.
 *
 * <p>Deliberately value-free: it carries a template key rather than a
 * rendered sentence, so an explanation can be assembled without ever routing
 * a compared value through a string that might be logged. Rendering the key
 * into text is the consumer's responsibility, from data it already holds.
 *
 * <p>It also carries the {@link TokenSubsumption} the evidence reported,
 * because a category alone cannot say <em>why</em> two candidates are hard to
 * separate. Two candidates that each contain the source score identically and
 * tie; so do two candidates that happen to score the same for unrelated
 * reasons. Only the containment signal tells those apart, and a consumer
 * deciding what to do with a {@code REVIEW} needs the difference.
 *
 * <p>This is a narrow projection of the evidence rather than the evidence
 * itself, and deliberately so: {@code FieldEvidence} also carries a frequency
 * key, which is a prepared value. Exposing the whole evidence object here
 * would route a compared value into the result a consumer logs — the exact
 * thing this type's value-free design exists to prevent.
 */
public final class FieldContribution {

    private final String field;
    private final ComparisonCategory category;
    private final double contribution;
    private final String templateKey;
    private final TokenSubsumption subsumption;

    /**
     * A contribution from evidence that reported no containment relation.
     * The subsumption is {@link TokenSubsumption#NOT_APPLICABLE}.
     */
    public FieldContribution(String field, ComparisonCategory category, double contribution, String templateKey) {
        this(field, category, contribution, templateKey, TokenSubsumption.NOT_APPLICABLE);
    }

    public FieldContribution(String field, ComparisonCategory category, double contribution,
            String templateKey, TokenSubsumption subsumption) {
        if (field == null) {
            throw new IllegalArgumentException("field must not be null");
        }
        if (category == null) {
            throw new IllegalArgumentException("category must not be null");
        }
        if (subsumption == null) {
            throw new IllegalArgumentException("subsumption must not be null");
        }
        this.field = field;
        this.category = category;
        this.contribution = contribution;
        this.templateKey = templateKey;
        this.subsumption = subsumption;
    }

    public String getField() {
        return field;
    }

    public ComparisonCategory getCategory() {
        return category;
    }

    public double getContribution() {
        return contribution;
    }

    /**
     * A key identifying the explanation template for this contribution.
     * Never a rendered sentence and never a prepared value.
     */
    public String getTemplateKey() {
        return templateKey;
    }

    /**
     * Whether one side's tokens were contained in the other's, as the
     * evidence reported it.
     *
     * <p>{@link TokenSubsumption#NOT_APPLICABLE} where the comparator does
     * not reason about tokens — which is not the same as
     * {@link TokenSubsumption#NEITHER}, meaning containment was computed and
     * did not hold.
     *
     * @return the containment relation, never null
     */
    public TokenSubsumption getSubsumption() {
        return subsumption;
    }

    @Override
    public String toString() {
        // Every member rendered here is a field name, a category, a number
        // or a template key. None is a compared value, and adding a member
        // to this type is when that property is most easily lost.
        return "FieldContribution{field=" + field + ", category=" + category
                + ", contribution=" + contribution + ", templateKey=" + templateKey
                + ", subsumption=" + subsumption + '}';
    }
}
