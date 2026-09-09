package io.github.aindriub.jresolve.result;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;

/**
 * How much one field's evidence contributed to a candidate's score.
 *
 * <p>Deliberately value-free: it carries a template key rather than a
 * rendered sentence, so an explanation can be assembled without ever routing
 * a compared value through a string that might be logged. Rendering the key
 * into text is the consumer's responsibility, from data it already holds.
 */
public final class FieldContribution {

    private final String field;
    private final ComparisonCategory category;
    private final double contribution;
    private final String templateKey;

    public FieldContribution(String field, ComparisonCategory category, double contribution, String templateKey) {
        if (field == null) {
            throw new IllegalArgumentException("field must not be null");
        }
        if (category == null) {
            throw new IllegalArgumentException("category must not be null");
        }
        this.field = field;
        this.category = category;
        this.contribution = contribution;
        this.templateKey = templateKey;
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

    @Override
    public String toString() {
        return "FieldContribution{field=" + field + ", category=" + category
                + ", contribution=" + contribution + ", templateKey=" + templateKey + '}';
    }
}
