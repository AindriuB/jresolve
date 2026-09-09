package io.github.aindriub.jresolve.scoring;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.DefaultFieldEvidence;
import io.github.aindriub.jresolve.evidence.FieldEvidence;
import io.github.aindriub.jresolve.evidence.MatchEvidence;
import io.github.aindriub.jresolve.result.FieldContribution;
import io.github.aindriub.jresolve.result.Score;
import io.github.aindriub.jresolve.result.ScoreScale;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleBasedScorerTest {

    @Test
    void reportsThePointsScale() {
        RuleBasedScorer scorer = RuleBasedScorer.builder().build();

        assertThat(scorer.scale()).isEqualTo(ScoreScale.POINTS);
    }

    @Test
    void stampsTheAlgorithmIdentifierAndLeavesProbabilityNull() {
        RuleBasedScorer scorer = RuleBasedScorer.builder()
                .weight("code", ComparisonCategory.EXACT, 5.0)
                .build();
        MatchEvidence evidence = evidenceOf("code", ComparisonCategory.EXACT);

        ScoringResult result = scorer.score(evidence);

        assertThat(result.isScorable()).isTrue();
        Score score = result.getScore();
        assertThat(score.getAlgorithm()).isEqualTo(RuleBasedScorer.ALGORITHM);
        assertThat(score.getProbability()).isNull();
        assertThat(score.getScale()).isEqualTo(ScoreScale.POINTS);
    }

    @Test
    void totalEqualsTheSumOfTheContributions() {
        RuleBasedScorer scorer = RuleBasedScorer.builder()
                .baseScore(1.0)
                .weight("code", ComparisonCategory.EXACT, 5.0)
                .weight("tier", ComparisonCategory.HIGH, 3.0)
                .weight("reference", ComparisonCategory.CONFLICT, -8.0)
                .defaultWeight("status", 2.0)
                .build();
        Map<String, FieldEvidence> fields = new LinkedHashMap<>();
        fields.put("code", new DefaultFieldEvidence(ComparisonCategory.EXACT, null, null));
        fields.put("tier", new DefaultFieldEvidence(ComparisonCategory.HIGH, null, null));
        fields.put("reference", new DefaultFieldEvidence(ComparisonCategory.CONFLICT, null, null));
        fields.put("status", new DefaultFieldEvidence(ComparisonCategory.MEDIUM, null, null));
        MatchEvidence evidence = new MatchEvidence(fields, true);

        ScoringResult result = scorer.score(evidence);

        assertThat(result.isScorable()).isTrue();
        double sumOfContributions = result.getContributions().stream()
                .mapToDouble(FieldContribution::getContribution)
                .sum();
        assertThat(result.getScore().getValue()).isEqualTo(1.0 + sumOfContributions);
    }

    /**
     * Hand-worked expectation, not derived from running the scorer:
     * base 1.0
     * + code   EXACT     5.0
     * + tier   HIGH      3.0
     * + reference CONFLICT -8.0
     * + status MEDIUM (default weight for the field) 2.0
     * = 1.0 + 5.0 + 3.0 - 8.0 + 2.0 = 3.0
     */
    @Test
    void handWorkedFourFieldTotalIncludingANegativeConflictWeight() {
        RuleBasedScorer scorer = RuleBasedScorer.builder()
                .baseScore(1.0)
                .weight("code", ComparisonCategory.EXACT, 5.0)
                .weight("tier", ComparisonCategory.HIGH, 3.0)
                .weight("reference", ComparisonCategory.CONFLICT, -8.0)
                .defaultWeight("status", 2.0)
                .build();
        Map<String, FieldEvidence> fields = new LinkedHashMap<>();
        fields.put("code", new DefaultFieldEvidence(ComparisonCategory.EXACT, null, null));
        fields.put("tier", new DefaultFieldEvidence(ComparisonCategory.HIGH, null, null));
        fields.put("reference", new DefaultFieldEvidence(ComparisonCategory.CONFLICT, null, null));
        fields.put("status", new DefaultFieldEvidence(ComparisonCategory.MEDIUM, null, null));
        MatchEvidence evidence = new MatchEvidence(fields, true);

        ScoringResult result = scorer.score(evidence);

        assertThat(result.getScore().getValue()).isEqualTo(3.0);
    }

    @Test
    void emitsOneContributionPerFieldInEvidenceIterationOrder() {
        RuleBasedScorer scorer = RuleBasedScorer.builder()
                .weight("code", ComparisonCategory.EXACT, 5.0)
                .weight("tier", ComparisonCategory.HIGH, 3.0)
                .build();
        Map<String, FieldEvidence> fields = new LinkedHashMap<>();
        fields.put("tier", new DefaultFieldEvidence(ComparisonCategory.HIGH, null, null));
        fields.put("code", new DefaultFieldEvidence(ComparisonCategory.EXACT, null, null));
        MatchEvidence evidence = new MatchEvidence(fields, true);

        List<FieldContribution> contributions = scorer.score(evidence).getContributions();

        assertThat(contributions).extracting(FieldContribution::getField).containsExactly("tier", "code");
    }

    @Test
    void requiredFieldWithMissingOneEvidenceIsUnscorable() {
        RuleBasedScorer scorer = RuleBasedScorer.builder().requiredField("code").build();
        MatchEvidence evidence = evidenceOf("code", ComparisonCategory.MISSING_ONE);

        ScoringResult result = scorer.score(evidence);

        assertThat(result.isScorable()).isFalse();
        assertThat(result.getUnscorableTemplateKey()).contains("code");
    }

    @Test
    void requiredFieldWithMissingBothEvidenceIsUnscorable() {
        RuleBasedScorer scorer = RuleBasedScorer.builder().requiredField("code").build();
        MatchEvidence evidence = evidenceOf("code", ComparisonCategory.MISSING_BOTH);

        ScoringResult result = scorer.score(evidence);

        assertThat(result.isScorable()).isFalse();
        assertThat(result.getUnscorableTemplateKey()).contains("code");
    }

    @Test
    void requiredFieldAbsentFromEvidenceIsUnscorable() {
        RuleBasedScorer scorer = RuleBasedScorer.builder().requiredField("code").build();
        MatchEvidence evidence = new MatchEvidence(new LinkedHashMap<>(), false);

        ScoringResult result = scorer.score(evidence);

        assertThat(result.isScorable()).isFalse();
        assertThat(result.getUnscorableTemplateKey()).contains("code");
    }

    @Test
    void scorerWithNoRequiredFieldsStillScoresIncompleteEvidenceAndAbsentFieldsContributeZero() {
        RuleBasedScorer scorer = RuleBasedScorer.builder()
                .weight("code", ComparisonCategory.EXACT, 5.0)
                // If an absent field were scored as MISSING_BOTH this weight would apply;
                // it must not, since "tier" carries no evidence at all.
                .weight("tier", ComparisonCategory.MISSING_BOTH, -100.0)
                .build();
        Map<String, FieldEvidence> fields = new LinkedHashMap<>();
        fields.put("code", new DefaultFieldEvidence(ComparisonCategory.EXACT, null, null));
        MatchEvidence evidence = new MatchEvidence(fields, false);

        ScoringResult result = scorer.score(evidence);

        assertThat(evidence.isComplete()).isFalse();
        assertThat(result.isScorable()).isTrue();
        assertThat(result.getContributions()).extracting(FieldContribution::getField).containsExactly("code");
        assertThat(result.getScore().getValue()).isEqualTo(5.0);
    }

    @Test
    void unconfiguredFieldContributesZero() {
        RuleBasedScorer scorer = RuleBasedScorer.builder().build();
        MatchEvidence evidence = evidenceOf("code", ComparisonCategory.EXACT);

        ScoringResult result = scorer.score(evidence);

        assertThat(result.getContributions()).extracting(FieldContribution::getContribution).containsExactly(0.0);
    }

    @Test
    void rejectsNullEvidence() {
        RuleBasedScorer scorer = RuleBasedScorer.builder().build();

        assertThatThrownBy(() -> scorer.score(null)).isInstanceOf(IllegalArgumentException.class);
    }

    private static MatchEvidence evidenceOf(String field, ComparisonCategory category) {
        Map<String, FieldEvidence> fields = new LinkedHashMap<>();
        fields.put(field, new DefaultFieldEvidence(category, null, null));
        return new MatchEvidence(fields, true);
    }
}
