package io.github.aindriub.jresolve.api;

import io.github.aindriub.jresolve.decision.MatchDecisionEngine;
import io.github.aindriub.jresolve.evidence.FieldEvidence;
import io.github.aindriub.jresolve.evidence.MatchEvidence;
import io.github.aindriub.jresolve.field.FieldDefinition;
import io.github.aindriub.jresolve.result.MatchResult;
import io.github.aindriub.jresolve.result.ScoredCandidate;
import io.github.aindriub.jresolve.scoring.MatchScorer;
import io.github.aindriub.jresolve.scoring.ScoringResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The default {@link EntityResolver}: prepares the source once per
 * {@link #resolve} call, compares fields tier by tier in ascending cost
 * order, runs every configured {@link CandidateRule} between tiers as a
 * veto, and scores whatever survives.
 *
 * <p>Immutable and holds no mutable field, so one built instance is safe for
 * concurrent {@link #resolve} calls. Built only by {@link
 * EntityResolverBuilder#build()}.
 *
 * @param <S> the source record type; owned entirely by the consumer
 * @param <C> the candidate record type; owned entirely by the consumer
 */
final class DefaultEntityResolver<S, C> implements EntityResolver<S, C> {

    private final List<List<FieldDefinition<S, C, ?>>> tiers;
    private final List<FieldDefinition<S, C, ?>> fields;
    private final List<CandidateRule<S, C>> rules;
    private final MatchScorer scorer;
    private final MatchDecisionEngine<C> decisionEngine;

    DefaultEntityResolver(
            List<List<FieldDefinition<S, C, ?>>> tiers,
            List<CandidateRule<S, C>> rules,
            MatchScorer scorer,
            MatchDecisionEngine<C> decisionEngine) {
        List<List<FieldDefinition<S, C, ?>>> tierCopy = new ArrayList<List<FieldDefinition<S, C, ?>>>();
        List<FieldDefinition<S, C, ?>> flat = new ArrayList<FieldDefinition<S, C, ?>>();
        for (List<FieldDefinition<S, C, ?>> tier : tiers) {
            List<FieldDefinition<S, C, ?>> tierList =
                    Collections.unmodifiableList(new ArrayList<FieldDefinition<S, C, ?>>(tier));
            tierCopy.add(tierList);
            flat.addAll(tierList);
        }
        this.tiers = Collections.unmodifiableList(tierCopy);
        this.fields = Collections.unmodifiableList(flat);
        List<CandidateRule<S, C>> ruleCopy = new ArrayList<CandidateRule<S, C>>();
        for (CandidateRule<S, C> rule : rules) {
            if (rule != null) {
                ruleCopy.add(rule);
            }
        }
        this.rules = Collections.unmodifiableList(ruleCopy);
        this.scorer = scorer;
        this.decisionEngine = decisionEngine;
    }

    @Override
    public MatchResult<C> resolve(S source, Collection<C> candidates) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        if (candidates == null) {
            throw new IllegalArgumentException("candidates must not be null");
        }

        // D2: every field is prepared exactly once here, before the candidate
        // loop, rather than once per candidate inside it.
        Map<String, Object> preparedSource = new LinkedHashMap<String, Object>();
        for (FieldDefinition<S, C, ?> field : fields) {
            preparedSource.put(field.getName(), prepareSource(field, source));
        }

        List<ScoredCandidate<C>> scored = new ArrayList<ScoredCandidate<C>>();
        for (C candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            ScoredCandidate<C> result = resolveOne(source, candidate, preparedSource);
            if (result != null) {
                scored.add(result);
            }
        }
        return decisionEngine.decide(scored);
    }

    private ScoredCandidate<C> resolveOne(S source, C candidate, Map<String, Object> preparedSource) {
        Map<String, FieldEvidence> evidenceSoFar = new LinkedHashMap<String, FieldEvidence>();
        for (List<FieldDefinition<S, C, ?>> tier : tiers) {
            for (FieldDefinition<S, C, ?> field : tier) {
                evidenceSoFar.put(field.getName(), compare(field, preparedSource.get(field.getName()), candidate));
            }
            MatchEvidence evidenceGatheredSoFar =
                    new MatchEvidence(evidenceSoFar, evidenceSoFar.size() == fields.size());
            for (CandidateRule<S, C> rule : rules) {
                if (rule.evaluate(source, candidate, evidenceGatheredSoFar) == RuleDecision.REJECT) {
                    return null;
                }
            }
        }

        MatchEvidence evidence = new MatchEvidence(evidenceSoFar, true);
        ScoringResult scoringResult = scorer.score(evidence);
        if (!scoringResult.isScorable()) {
            return null;
        }
        return new ScoredCandidate<C>(candidate, scoringResult.getScore(), scoringResult.getContributions());
    }

    private static <S, C, N> Object prepareSource(FieldDefinition<S, C, N> field, S source) {
        return field.getSourcePreparer().apply(source);
    }

    private static <S, C, N> FieldEvidence compare(FieldDefinition<S, C, N> field, Object preparedSourceValue,
            C candidate) {
        // Safe: preparedSourceValue was produced by this same field's source
        // preparer, so its erased type always matches N.
        @SuppressWarnings("unchecked")
        N sourceValue = (N) preparedSourceValue;
        N candidateValue = field.getCandidatePreparer().apply(candidate);
        return field.getComparator().compare(sourceValue, candidateValue);
    }
}
