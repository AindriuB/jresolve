package io.github.aindriub.jresolve.result;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoredCandidateTest {

    @Test
    void exposesCandidateScoreAndContributionsInOrder() {
        Score score = new Score(1.0, ScoreScale.POINTS, "rules", null);
        List<FieldContribution> contributions = new ArrayList<>();
        contributions.add(new FieldContribution("first", ComparisonCategory.EXACT, 2.0, "template.first"));
        contributions.add(new FieldContribution("second", ComparisonCategory.HIGH, 1.0, "template.second"));

        ScoredCandidate<String> candidate = new ScoredCandidate<>("candidateRef", score, contributions);

        assertThat(candidate.getCandidate()).isEqualTo("candidateRef");
        assertThat(candidate.getScore()).isSameAs(score);
        assertThat(candidate.getContributions()).extracting(FieldContribution::getField)
                .containsExactly("first", "second");
    }

    @Test
    void copiesContributionsDefensively() {
        Score score = new Score(1.0, ScoreScale.POINTS, "rules", null);
        List<FieldContribution> contributions = new ArrayList<>();
        contributions.add(new FieldContribution("first", ComparisonCategory.EXACT, 2.0, "template.first"));

        ScoredCandidate<String> candidate = new ScoredCandidate<>("candidateRef", score, contributions);
        contributions.add(new FieldContribution("second", ComparisonCategory.HIGH, 1.0, "template.second"));

        assertThat(candidate.getContributions()).hasSize(1);
    }

    @Test
    void getContributionsIsUnmodifiable() {
        Score score = new Score(1.0, ScoreScale.POINTS, "rules", null);
        ScoredCandidate<String> candidate = new ScoredCandidate<>("candidateRef", score, new ArrayList<>());

        assertThatThrownBy(() -> candidate.getContributions()
                .add(new FieldContribution("x", ComparisonCategory.EXACT, 1.0, "key")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsNullScore() {
        assertThatThrownBy(() -> new ScoredCandidate<>("candidateRef", null, new ArrayList<>()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullContributions() {
        Score score = new Score(1.0, ScoreScale.POINTS, "rules", null);
        assertThatThrownBy(() -> new ScoredCandidate<>("candidateRef", score, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toStringExcludesTheCandidatesToString() {
        Score score = new Score(1.0, ScoreScale.POINTS, "rules", null);
        Object sentinelCandidate = new Object() {
            @Override
            public String toString() {
                return "SENTINEL-CANDIDATE-VALUE";
            }
        };
        ScoredCandidate<Object> candidate = new ScoredCandidate<>(sentinelCandidate, score, new ArrayList<>());

        assertThat(candidate.toString()).doesNotContain("SENTINEL-CANDIDATE-VALUE");
    }
}
