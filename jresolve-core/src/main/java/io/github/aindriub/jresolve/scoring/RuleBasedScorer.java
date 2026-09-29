package io.github.aindriub.jresolve.scoring;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.FieldEvidence;
import io.github.aindriub.jresolve.evidence.MatchEvidence;
import io.github.aindriub.jresolve.result.FieldContribution;
import io.github.aindriub.jresolve.result.Score;
import io.github.aindriub.jresolve.result.ScoreScale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A transparent, deterministic {@link MatchScorer} that sums a configured
 * weight per field per {@link ComparisonCategory}, plus a base score.
 *
 * <p>Reports {@link ScoreScale#POINTS}: the resulting value has no fixed
 * meaning outside the weights that produced it. {@link Score#getProbability()}
 * is always null on scores this class produces — a rule sum is not a
 * calibrated probability of a true match, only a configured points scale.
 *
 * <p>Immutable once built; configure it with {@link #builder()}.
 */
public final class RuleBasedScorer implements MatchScorer {

    /**
     * The stable identifier stamped into {@link Score#getAlgorithm()} for
     * every score this class produces.
     */
    public static final String ALGORITHM = "RULE_BASED_V1";

    private static final List<ComparisonCategory> PARTIAL_AGREEMENT = Collections.unmodifiableList(
            Arrays.asList(
                    ComparisonCategory.VERY_HIGH, ComparisonCategory.HIGH,
                    ComparisonCategory.MEDIUM, ComparisonCategory.LOW,
                    ComparisonCategory.ALIAS_TRANSLATION, ComparisonCategory.ALIAS_NICKNAME,
                    ComparisonCategory.ALIAS_VARIANT, ComparisonCategory.SUBSUMED,
                    ComparisonCategory.PARTIAL_OVERLAP));

    private final Map<String, Map<ComparisonCategory, Double>> categoryWeights;
    private final Map<String, Double> defaultFieldWeights;
    private final double baseScore;
    private final Set<String> requiredFields;

    private RuleBasedScorer(Builder builder) {
        Map<String, Map<ComparisonCategory, Double>> copy = new LinkedHashMap<String, Map<ComparisonCategory, Double>>();
        for (Map.Entry<String, Map<ComparisonCategory, Double>> entry : builder.categoryWeights.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableMap(new LinkedHashMap<ComparisonCategory, Double>(entry.getValue())));
        }
        this.categoryWeights = Collections.unmodifiableMap(copy);
        this.defaultFieldWeights = Collections.unmodifiableMap(new LinkedHashMap<String, Double>(builder.defaultFieldWeights));
        this.baseScore = builder.baseScore;
        this.requiredFields = Collections.unmodifiableSet(new LinkedHashSet<String>(builder.requiredFields));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ScoreScale scale() {
        return ScoreScale.POINTS;
    }

    @Override
    public ScoringResult score(MatchEvidence evidence) {
        if (evidence == null) {
            throw new IllegalArgumentException("evidence must not be null");
        }
        for (String required : requiredFields) {
            FieldEvidence fieldEvidence = evidence.getField(required);
            if (fieldEvidence == null
                    || fieldEvidence.getCategory() == ComparisonCategory.MISSING_ONE
                    || fieldEvidence.getCategory() == ComparisonCategory.MISSING_BOTH) {
                return ScoringResult.unscorable("scoring.rule.requiredFieldMissing." + required);
            }
        }

        double total = baseScore;
        List<FieldContribution> contributions = new ArrayList<FieldContribution>();
        for (Map.Entry<String, FieldEvidence> entry : evidence.getFields().entrySet()) {
            String field = entry.getKey();
            FieldEvidence fieldEvidence = entry.getValue();
            ComparisonCategory category = fieldEvidence.getCategory();
            double weight = weightFor(field, category);
            total += weight;
            // The subsumption travels with the contribution. The weight is
            // keyed on the category alone — containment in either direction
            // is equally informative — but a consumer reading the result
            // needs the direction to tell a genuine tie from a coincidental
            // one.
            contributions.add(new FieldContribution(field, category, weight,
                    "scoring.rule.contribution." + field + '.' + category.getName(),
                    fieldEvidence.getSubsumption()));
        }

        Score score = new Score(total, ScoreScale.POINTS, ALGORITHM, null);
        return ScoringResult.scorable(score, contributions);
    }

    private double weightFor(String field, ComparisonCategory category) {
        Map<ComparisonCategory, Double> perCategory = categoryWeights.get(field);
        if (perCategory != null) {
            Double configured = perCategory.get(category);
            if (configured != null) {
                return configured;
            }
        }
        Double defaultWeight = defaultFieldWeights.get(field);
        return defaultWeight != null ? defaultWeight : 0.0;
    }

    /**
     * Builds an immutable {@link RuleBasedScorer}.
     */
    public static final class Builder {

        private final Map<String, Map<ComparisonCategory, Double>> categoryWeights = new LinkedHashMap<String, Map<ComparisonCategory, Double>>();
        private final Map<String, Double> defaultFieldWeights = new LinkedHashMap<String, Double>();
        private double baseScore;
        private final Set<String> requiredFields = new LinkedHashSet<String>();

        private Builder() {
        }

        /**
         * Configures the weight added when the given field's evidence
         * carries the given category. Overwrites any previously configured
         * weight for the same pair.
         *
         * @throws IllegalArgumentException if {@code field} or
         *     {@code category} is null
         */
        public Builder weight(String field, ComparisonCategory category, double weight) {
            if (field == null) {
                throw new IllegalArgumentException("field must not be null");
            }
            if (category == null) {
                throw new IllegalArgumentException("category must not be null");
            }
            Map<ComparisonCategory, Double> perCategory = categoryWeights.get(field);
            if (perCategory == null) {
                perCategory = new LinkedHashMap<ComparisonCategory, Double>();
                categoryWeights.put(field, perCategory);
            }
            perCategory.put(category, weight);
            return this;
        }

        /**
         * Configures the weight added when the given field's evidence
         * carries a category with no weight configured via
         * {@link #weight(String, ComparisonCategory, double)}.
         *
         * @throws IllegalArgumentException if {@code field} is null
         */
        public Builder defaultWeight(String field, double weight) {
            if (field == null) {
                throw new IllegalArgumentException("field must not be null");
            }
            defaultFieldWeights.put(field, weight);
            return this;
        }

        /**
         * Sets the score every candidate starts from before field weights
         * are added. Defaults to {@code 0.0}.
         */
        public Builder baseScore(double baseScore) {
            this.baseScore = baseScore;
            return this;
        }

        /**
         * Marks a field as required: evidence with no entry for it, or an
         * entry categorized {@code MISSING_ONE} or {@code MISSING_BOTH},
         * makes {@link RuleBasedScorer#score(MatchEvidence)} return an
         * unscorable result.
         *
         * @throws IllegalArgumentException if {@code field} is null
         */
        public Builder requiredField(String field) {
            if (field == null) {
                throw new IllegalArgumentException("field must not be null");
            }
            requiredFields.add(field);
            return this;
        }

        /**
         * Builds the scorer.
         *
         * <p>A field that has a weight for a partial-agreement category
         * ({@code VERY_HIGH}, {@code HIGH}, {@code MEDIUM}, {@code LOW},
         * {@code ALIAS_TRANSLATION}, {@code ALIAS_NICKNAME},
         * {@code ALIAS_VARIANT}, {@code SUBSUMED}, {@code PARTIAL_OVERLAP})
         * but neither an {@code EXACT} weight nor a
         * {@link #defaultWeight(String, double)} would score a perfect
         * agreement as zero, silently. That is refused. Weighting
         * {@code EXACT} explicitly at {@code 0.0} is the opt-out.
         *
         * @throws IllegalArgumentException if any field breaks that rule; the
         *     message names the field and never a value
         */
        public RuleBasedScorer build() {
            for (Map.Entry<String, Map<ComparisonCategory, Double>> entry : categoryWeights.entrySet()) {
                String field = entry.getKey();
                Map<ComparisonCategory, Double> perCategory = entry.getValue();
                if (perCategory.containsKey(ComparisonCategory.EXACT)
                        || defaultFieldWeights.containsKey(field)) {
                    continue;
                }
                for (ComparisonCategory partial : PARTIAL_AGREEMENT) {
                    if (perCategory.containsKey(partial)) {
                        throw new IllegalArgumentException("field '" + field
                                + "' weights a partial-agreement category but EXACT is unweighted;"
                                + " weight EXACT (0.0 to opt out) or set a defaultWeight");
                    }
                }
            }
            return new RuleBasedScorer(this);
        }
    }
}
