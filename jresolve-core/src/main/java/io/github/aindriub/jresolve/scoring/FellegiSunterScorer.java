package io.github.aindriub.jresolve.scoring;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.FieldEvidence;
import io.github.aindriub.jresolve.evidence.MatchEvidence;
import io.github.aindriub.jresolve.result.FieldContribution;
import io.github.aindriub.jresolve.result.Score;
import io.github.aindriub.jresolve.result.ScoreScale;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scores evidence as a Fellegi-Sunter log likelihood ratio.
 *
 * <pre>
 *   weight_i = log2(m_i / u_i)
 *   W        = Σ weight_i
 * </pre>
 *
 * <p>The result is on {@link ScoreScale#LOG2_LIKELIHOOD_RATIO}. On that scale
 * a margin between two candidates <em>is</em> the log ratio of their
 * likelihoods, which is the quantity a review threshold actually wants —
 * unlike a points total, where the same numeric gap means different things at
 * different score levels.
 *
 * <h2>A weight is not a probability</h2>
 *
 * <p>{@code W} is a likelihood ratio and nothing more. A probability appears
 * only where the model carries prior odds, because turning a likelihood ratio
 * into a posterior requires a base rate and the library will not invent one:
 *
 * <pre>
 *   posterior odds = prior odds × 2^W
 *   probability    = posterior odds / (1 + posterior odds)
 * </pre>
 *
 * <p>Where that probability is exposed, it is <strong>conditional on the
 * model's own assumptions</strong> and is not evidence of empirical
 * calibration. A model with plausible-looking numbers and a plausible-looking
 * prior will produce a plausible-looking probability whether or not either
 * was ever measured. See {@code docs/calibration.md}.
 *
 * <h2>The independence assumption</h2>
 *
 * <p>Summing per-field weights assumes the fields are conditionally
 * independent given match status. That is frequently false — two fields that
 * co-vary within a household are not independent — and a model over such a
 * pair counts the shared signal twice and is systematically overconfident.
 *
 * <p>Where a consumer has declared a {@linkplain
 * FellegiSunterModel#compositeGroups() composite group}, this scorer weighs
 * the group <strong>once</strong>, contributing the smallest weight among its
 * present members. That is a deliberate choice and a conservative one: the
 * failure mode D11 names is overconfidence, and when the scorer cannot know
 * how much of the signal is shared, the group should claim no more than its
 * least favourable member. Averaging or taking the strongest member would
 * both claim more.
 *
 * <h2>Incomplete evidence is refused, not guessed</h2>
 *
 * <p>Evidence marked incomplete — a candidate short-circuited between cost
 * tiers — is returned {@linkplain ScoringResult#unscorable unscorable}. It is
 * not scored over whatever fields happen to be present, and absent fields are
 * not treated as though both sides were missing. Either would be a quiet
 * fabrication, and a partial likelihood ratio compared against a threshold
 * calibrated for a full one is worse than no answer.
 *
 * <p>Immutable and safe for concurrent use.
 */
public final class FellegiSunterScorer implements MatchScorer {

    /** Stamped on every score this scorer produces, so a result is attributable. */
    public static final String ALGORITHM = "FELLEGI_SUNTER_V1";

    private static final double LN_2 = Math.log(2.0);

    private final FellegiSunterModel model;

    public FellegiSunterScorer(FellegiSunterModel model) {
        if (model == null) {
            throw new IllegalArgumentException("model must not be null");
        }
        this.model = model;
    }

    @Override
    public ScoreScale scale() {
        return ScoreScale.LOG2_LIKELIHOOD_RATIO;
    }

    @Override
    public ScoringResult score(MatchEvidence evidence) {
        if (evidence == null) {
            throw new IllegalArgumentException("evidence must not be null");
        }
        if (!evidence.isComplete()) {
            return ScoringResult.unscorable("scoring.fellegiSunter.incompleteEvidence");
        }

        Map<String, Double> weightByField = new HashMap<>();
        for (Map.Entry<String, FieldEvidence> entry : evidence.getFields().entrySet()) {
            String field = entry.getKey();
            ComparisonCategory category = entry.getValue().getCategory();
            if (model.isIgnored(field, category)) {
                continue;
            }
            double m = model.mProbability(field, category);
            double u = model.uProbability(field, category, entry.getValue().getFrequencyKey());
            weightByField.put(field, Math.log(m / u) / LN_2);
        }

        Set<String> suppressed = suppressedByComposites(weightByField);

        double total = 0.0;
        List<FieldContribution> contributions = new ArrayList<>();
        for (Map.Entry<String, FieldEvidence> entry : evidence.getFields().entrySet()) {
            String field = entry.getKey();
            FieldEvidence fieldEvidence = entry.getValue();
            Double weight = weightByField.get(field);
            if (weight == null) {
                // Declared to carry no evidence; recorded so an explanation
                // shows the field was considered and deliberately skipped.
                contributions.add(new FieldContribution(field, fieldEvidence.getCategory(), 0.0,
                        "scoring.fellegiSunter.ignored." + field,
                        fieldEvidence.getSubsumption()));
                continue;
            }
            if (suppressed.contains(field)) {
                contributions.add(new FieldContribution(field, fieldEvidence.getCategory(), 0.0,
                        "scoring.fellegiSunter.compositeSuppressed." + field,
                        fieldEvidence.getSubsumption()));
                continue;
            }
            total += weight;
            contributions.add(new FieldContribution(field, fieldEvidence.getCategory(), weight,
                    "scoring.fellegiSunter.contribution." + field + '.'
                            + fieldEvidence.getCategory().getName(),
                    fieldEvidence.getSubsumption()));
        }

        Double probability = null;
        if (model.hasPriorOdds()) {
            double posteriorOdds = model.priorOdds() * Math.pow(2.0, total);
            probability = Double.isInfinite(posteriorOdds)
                    ? Double.valueOf(1.0)
                    : Double.valueOf(posteriorOdds / (1.0 + posteriorOdds));
        }

        Score score = new Score(total, ScoreScale.LOG2_LIKELIHOOD_RATIO, ALGORITHM, probability);
        return ScoringResult.scorable(score, contributions);
    }

    /**
     * For each declared composite group, keeps the member with the smallest
     * weight and suppresses the rest, so the group contributes once.
     *
     * <p>A group whose members are not all present contributes through
     * whichever members are: a composite is a statement about correlation,
     * not a requirement that every member be compared.
     */
    private Set<String> suppressedByComposites(Map<String, Double> weightByField) {
        Set<String> suppressed = new HashSet<>();
        for (Set<String> group : model.compositeGroups()) {
            String keep = null;
            double keepWeight = 0.0;
            for (String field : group) {
                Double weight = weightByField.get(field);
                if (weight == null || suppressed.contains(field)) {
                    continue;
                }
                if (keep == null || weight < keepWeight) {
                    keep = field;
                    keepWeight = weight;
                }
            }
            if (keep == null) {
                continue;
            }
            for (String field : group) {
                if (!field.equals(keep) && weightByField.containsKey(field)) {
                    suppressed.add(field);
                }
            }
        }
        return suppressed;
    }
}
