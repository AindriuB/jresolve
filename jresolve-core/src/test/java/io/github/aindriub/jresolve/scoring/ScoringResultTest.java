package io.github.aindriub.jresolve.scoring;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.result.FieldContribution;
import io.github.aindriub.jresolve.result.Score;
import io.github.aindriub.jresolve.result.ScoreScale;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoringResultTest {

    @Test
    void scorableResultCarriesScoreAndContributions() {
        Score score = new Score(10.0, ScoreScale.POINTS, "test", null);
        List<FieldContribution> contributions = new ArrayList<>();
        contributions.add(new FieldContribution("code", ComparisonCategory.EXACT, 10.0, "template.key"));

        ScoringResult result = ScoringResult.scorable(score, contributions);

        assertThat(result.isScorable()).isTrue();
        assertThat(result.getScore()).isEqualTo(score);
        assertThat(result.getContributions()).containsExactly(contributions.get(0));
    }

    @Test
    void unscorableResultCarriesOnlyATemplateKey() {
        ScoringResult result = ScoringResult.unscorable("scoring.missing.code");

        assertThat(result.isScorable()).isFalse();
        assertThat(result.getUnscorableTemplateKey()).isEqualTo("scoring.missing.code");
    }

    @Test
    void getScoreThrowsWhenUnscorable() {
        ScoringResult result = ScoringResult.unscorable("scoring.missing.code");

        assertThatThrownBy(result::getScore).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getContributionsThrowsWhenUnscorable() {
        ScoringResult result = ScoringResult.unscorable("scoring.missing.code");

        assertThatThrownBy(result::getContributions).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getUnscorableTemplateKeyThrowsWhenScorable() {
        Score score = new Score(10.0, ScoreScale.POINTS, "test", null);
        ScoringResult result = ScoringResult.scorable(score, new ArrayList<>());

        assertThatThrownBy(result::getUnscorableTemplateKey).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsNullScoreForScorableResult() {
        assertThatThrownBy(() -> ScoringResult.scorable(null, new ArrayList<>()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullContributionsForScorableResult() {
        Score score = new Score(10.0, ScoreScale.POINTS, "test", null);
        assertThatThrownBy(() -> ScoringResult.scorable(score, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankUnscorableTemplateKey() {
        assertThatThrownBy(() -> ScoringResult.unscorable("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void contributionsAreUnmodifiable() {
        Score score = new Score(10.0, ScoreScale.POINTS, "test", null);
        ScoringResult result = ScoringResult.scorable(score, new ArrayList<>());

        assertThatThrownBy(() -> result.getContributions()
                .add(new FieldContribution("code", ComparisonCategory.EXACT, 1.0, "k")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
