package io.github.aindriub.jresolve.result;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
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

    @Test
    void rejectsAMatchCandidateWhenDecisionIsNotMatch() {
        Score score = new Score(5.0, ScoreScale.POINTS, "rules", null);
        assertThatThrownBy(() -> new MatchResult<>(Decision.NO_MATCH, "candidateRef", score, null, new ArrayList<>()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMatchDecisionWithNullMatch() {
        Score score = new Score(5.0, ScoreScale.POINTS, "rules", null);
        assertThatThrownBy(() -> new MatchResult<>(Decision.MATCH, null, score, null, new ArrayList<>()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsSecondBestScoreWithoutABestScore() {
        Score second = new Score(3.0, ScoreScale.POINTS, "rules", null);
        assertThatThrownBy(() -> new MatchResult<>(Decision.NO_MATCH, null, null, second, new ArrayList<>()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsReviewDecisionWithNullScore() {
        assertThatThrownBy(() -> new MatchResult<>(Decision.REVIEW, null, null, null, new ArrayList<>()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allowsNoMatchDecisionWithNoCandidatesEvaluated() {
        MatchResult<String> result = new MatchResult<>(Decision.NO_MATCH, null, null, null, new ArrayList<>());

        assertThat(result.getScore()).isNull();
        assertThat(result.hasSecondBest()).isFalse();
    }

    /**
     * The legal set, as constructor Javadoc states it: 7 of 24 combinations
     * of (decision, match, score, secondBestScore) are legal.
     */
    @Test
    void constructionMatrixMatchesTheDocumentedLegalSet() {
        Score score = new Score(5.0, ScoreScale.POINTS, "rules", null);
        Score second = new Score(3.0, ScoreScale.POINTS, "rules", null);
        String match = "candidateRef";
        List<ScoredCandidate<String>> candidates = new ArrayList<>();

        for (Decision decision : Decision.values()) {
            for (Object matchValue : new Object[] {null, match}) {
                for (Object scoreValue : new Object[] {null, score}) {
                    for (Object secondValue : new Object[] {null, second}) {
                        boolean expectedLegal = isLegal(decision, matchValue != null, scoreValue != null,
                                secondValue != null);
                        try {
                            new MatchResult<>(decision, (String) matchValue, (Score) scoreValue,
                                    (Score) secondValue, candidates);
                            assertThat(expectedLegal)
                                    .as("decision=%s match=%s score=%s secondBest=%s should be legal",
                                            decision, matchValue != null, scoreValue != null, secondValue != null)
                                    .isTrue();
                        } catch (IllegalArgumentException e) {
                            assertThat(expectedLegal)
                                    .as("decision=%s match=%s score=%s secondBest=%s should be rejected",
                                            decision, matchValue != null, scoreValue != null, secondValue != null)
                                    .isFalse();
                        }
                    }
                }
            }
        }
    }

    private static boolean isLegal(Decision decision, boolean hasMatch, boolean hasScore, boolean hasSecondBest) {
        if (hasSecondBest && !hasScore) {
            return false;
        }
        switch (decision) {
            case MATCH:
                return hasMatch && hasScore;
            case REVIEW:
                return !hasMatch && hasScore;
            case NO_MATCH:
                return !hasMatch;
            default:
                throw new IllegalStateException("unhandled decision");
        }
    }

    @Test
    void toStringExcludesTheCandidatesToString() {
        Score score = new Score(5.0, ScoreScale.POINTS, "rules", null);
        Object sentinelCandidate = new Object() {
            @Override
            public String toString() {
                return "SENTINEL-CANDIDATE-VALUE";
            }
        };
        MatchResult<Object> result = new MatchResult<>(Decision.MATCH, sentinelCandidate, score, null, new ArrayList<>());

        assertThat(result.toString()).doesNotContain("SENTINEL-CANDIDATE-VALUE");
    }

    // ------------------------------------------------- rejected candidates

    @Test
    void aResultBuiltWithoutRejectionsReportsAnEmptyList() {
        // Every existing caller of the public constructor keeps working, and
        // gets empty rather than null.
        MatchResult<String> result =
                new MatchResult<>(Decision.NO_MATCH, null, null, null, emptyCandidates());

        assertThat(result.getRejectedCandidates()).isNotNull();
        assertThat(result.getRejectedCandidates()).isEmpty();
    }

    @Test
    void withRejectedCarriesThemAndLeavesTheOriginalAlone() {
        MatchResult<String> original =
                new MatchResult<>(Decision.NO_MATCH, null, null, null, emptyCandidates());

        MatchResult<String> carrying = original.withRejected(
                Arrays.asList(new RejectedCandidate<>("alpha", "resolver.rule.0")));

        assertThat(carrying.getRejectedCandidates()).hasSize(1);
        assertThat(original.getRejectedCandidates()).isEmpty();
    }

    @Test
    void withRejectedPreservesEveryOtherField() {
        // A copy-with that quietly dropped state would be worse than no
        // accessor at all.
        List<ScoredCandidate<String>> candidates = Arrays.asList(
                new ScoredCandidate<>("alpha", sampleScore(), java.util.Collections.emptyList()));
        MatchResult<String> original = new MatchResult<>(
                Decision.MATCH, "alpha", sampleScore(), null, candidates);

        MatchResult<String> carrying = original.withRejected(
                Arrays.asList(new RejectedCandidate<>("bravo", "resolver.rule.0")));

        assertThat(carrying.getDecision()).isEqualTo(original.getDecision());
        assertThat(carrying.getMatch()).isEqualTo(original.getMatch());
        assertThat(carrying.getScore()).isEqualTo(original.getScore());
        assertThat(carrying.getSecondBestScore()).isEqualTo(original.getSecondBestScore());
        assertThat(carrying.getCandidates()).isEqualTo(original.getCandidates());
    }

    @Test
    void withRejectedCopiesDefensively() {
        List<RejectedCandidate<String>> supplied = new ArrayList<>();
        supplied.add(new RejectedCandidate<>("alpha", "resolver.rule.0"));

        MatchResult<String> carrying =
                new MatchResult<String>(Decision.NO_MATCH, null, null, null, emptyCandidates())
                        .withRejected(supplied);
        supplied.add(new RejectedCandidate<>("bravo", "resolver.rule.1"));

        assertThat(carrying.getRejectedCandidates()).hasSize(1);
    }

    @Test
    void theRejectedListIsUnmodifiable() {
        MatchResult<String> carrying =
                new MatchResult<String>(Decision.NO_MATCH, null, null, null, emptyCandidates())
                        .withRejected(Arrays.asList(new RejectedCandidate<>("alpha", "resolver.rule.0")));

        assertThatThrownBy(() -> carrying.getRejectedCandidates()
                .add(new RejectedCandidate<>("bravo", "resolver.rule.1")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void withRejectedRejectsNull() {
        assertThatThrownBy(() ->
                new MatchResult<String>(Decision.NO_MATCH, null, null, null, emptyCandidates())
                        .withRejected(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<ScoredCandidate<String>> emptyCandidates() {
        return java.util.Collections.emptyList();
    }

    private static Score sampleScore() {
        return new Score(10.0, ScoreScale.POINTS, "TEST", null);
    }
}
