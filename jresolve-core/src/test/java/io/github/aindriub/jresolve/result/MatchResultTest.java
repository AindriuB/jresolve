package io.github.aindriub.jresolve.result;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchResultTest {

    @Test
    void isMatchIsTrueOnlyForMatchDecision() {
        Score score = new Score(5.0, ScoreScale.POINTS, "rules", null);
        List<ScoredCandidate<String>> candidates = new ArrayList<>();

        MatchResult<String> matched = new MatchResult<>(Decision.MATCH, "candidateRef", score, null, candidates);

        assertThat(matched.isMatch()).isTrue();
    }

    @Test
    void getMatchIsNullForReview() {
        Score score = new Score(5.0, ScoreScale.POINTS, "rules", null);
        MatchResult<String> review = new MatchResult<>(Decision.REVIEW, null, score, null, new ArrayList<>());

        assertThat(review.isMatch()).isFalse();
        assertThat(review.getMatch()).isNull();
    }

    @Test
    void getMatchIsNullForNoMatch() {
        Score score = new Score(5.0, ScoreScale.POINTS, "rules", null);
        MatchResult<String> noMatch = new MatchResult<>(Decision.NO_MATCH, null, score, null, new ArrayList<>());

        assertThat(noMatch.isMatch()).isFalse();
        assertThat(noMatch.getMatch()).isNull();
    }

    @Test
    void hasSecondBestReflectsWhetherASecondScoreWasGiven() {
        Score best = new Score(5.0, ScoreScale.POINTS, "rules", null);
        Score second = new Score(3.0, ScoreScale.POINTS, "rules", null);

        MatchResult<String> withSecond = new MatchResult<>(Decision.MATCH, "candidateRef", best, second, new ArrayList<>());
        MatchResult<String> withoutSecond = new MatchResult<>(Decision.MATCH, "candidateRef", best, null, new ArrayList<>());

        assertThat(withSecond.hasSecondBest()).isTrue();
        assertThat(withoutSecond.hasSecondBest()).isFalse();
    }

    @Test
    void getMarginComputesGapOnTheScoresScaleWhenSecondBestExists() {
        Score best = new Score(5.0, ScoreScale.POINTS, "rules", null);
        Score second = new Score(3.0, ScoreScale.POINTS, "rules", null);

        MatchResult<String> result = new MatchResult<>(Decision.MATCH, "candidateRef", best, second, new ArrayList<>());

        assertThat(result.getMargin()).isEqualTo(2.0);
    }

    @Test
    void getMarginThrowsWhenThereIsNoSecondBest() {
        Score best = new Score(5.0, ScoreScale.POINTS, "rules", null);
        MatchResult<String> result = new MatchResult<>(Decision.MATCH, "candidateRef", best, null, new ArrayList<>());

        assertThatThrownBy(result::getMargin).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void copiesCandidatesDefensively() {
        Score score = new Score(5.0, ScoreScale.POINTS, "rules", null);
        List<ScoredCandidate<String>> candidates = new ArrayList<>();
        candidates.add(new ScoredCandidate<>("candidateRef", score, new ArrayList<>()));

        MatchResult<String> result = new MatchResult<>(Decision.MATCH, "candidateRef", score, null, candidates);
        candidates.add(new ScoredCandidate<>("anotherRef", score, new ArrayList<>()));

        assertThat(result.getCandidates()).hasSize(1);
    }

    @Test
    void getCandidatesIsUnmodifiable() {
        Score score = new Score(5.0, ScoreScale.POINTS, "rules", null);
        MatchResult<String> result = new MatchResult<>(Decision.MATCH, "candidateRef", score, null, new ArrayList<>());

        assertThatThrownBy(() -> result.getCandidates().add(new ScoredCandidate<>("x", score, new ArrayList<>())))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsNullDecision() {
        Score score = new Score(5.0, ScoreScale.POINTS, "rules", null);
        assertThatThrownBy(() -> new MatchResult<>(null, "candidateRef", score, null, new ArrayList<>()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullCandidatesList() {
        Score score = new Score(5.0, ScoreScale.POINTS, "rules", null);
        assertThatThrownBy(() -> new MatchResult<>(Decision.MATCH, "candidateRef", score, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
