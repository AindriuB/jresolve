package io.github.aindriub.jresolve.decision;

import io.github.aindriub.jresolve.result.Decision;
import io.github.aindriub.jresolve.result.MatchResult;
import io.github.aindriub.jresolve.result.Score;
import io.github.aindriub.jresolve.result.ScoreScale;
import io.github.aindriub.jresolve.result.ScoredCandidate;
import io.github.aindriub.jresolve.result.FieldContribution;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class ThresholdDecisionEngineTest {

    private static final DecisionThresholds THRESHOLDS = new DecisionThresholds(10.0, 5.0, 2.0, ScoreScale.POINTS);

    @Test
    void emptyCandidateListIsNoMatchWithNoScore() {
        ThresholdDecisionEngine<String> engine = new ThresholdDecisionEngine<>(THRESHOLDS);

        MatchResult<String> result = engine.decide(new ArrayList<>());

        assertThat(result.getDecision()).isEqualTo(Decision.NO_MATCH);
        assertThat(result.getMatch()).isNull();
        assertThat(result.getScore()).isNull();
        assertThat(result.hasSecondBest()).isFalse();
    }

    @Test
    void atOrAboveMatchThresholdWithSufficientMarginIsMatch() {
        ThresholdDecisionEngine<String> engine = new ThresholdDecisionEngine<>(THRESHOLDS);
        List<ScoredCandidate<String>> candidates = candidates(entry("best", 12.0), entry("second", 8.0));

        MatchResult<String> result = engine.decide(candidates);

        assertThat(result.getDecision()).isEqualTo(Decision.MATCH);
        assertThat(result.getMatch()).isEqualTo("best");
    }

    @Test
    void marginExactlyEqualToMinimumMarginIsMatch() {
        // matchThreshold=10, minimumMargin=2: best=12, second=10 -> margin == 2.0, the boundary itself.
        ThresholdDecisionEngine<String> engine = new ThresholdDecisionEngine<>(THRESHOLDS);
        List<ScoredCandidate<String>> candidates = candidates(entry("best", 12.0), entry("second", 10.0));

        MatchResult<String> result = engine.decide(candidates);

        assertThat(result.getDecision()).isEqualTo(Decision.MATCH);
        assertThat(result.getMatch()).isEqualTo("best");
    }

    @Test
    void atOrAboveMatchThresholdWithInsufficientMarginIsReview() {
        ThresholdDecisionEngine<String> engine = new ThresholdDecisionEngine<>(THRESHOLDS);
        List<ScoredCandidate<String>> candidates = candidates(entry("best", 12.0), entry("second", 11.0));

        MatchResult<String> result = engine.decide(candidates);

        assertThat(result.getDecision()).isEqualTo(Decision.REVIEW);
        assertThat(result.getMatch()).isNull();
    }

    @Test
    void betweenTheThresholdsIsReview() {
        ThresholdDecisionEngine<String> engine = new ThresholdDecisionEngine<>(THRESHOLDS);
        List<ScoredCandidate<String>> candidates = candidates(entry("best", 7.0), entry("second", 3.0));

        MatchResult<String> result = engine.decide(candidates);

        assertThat(result.getDecision()).isEqualTo(Decision.REVIEW);
        assertThat(result.getMatch()).isNull();
    }

    @Test
    void belowReviewThresholdIsNoMatch() {
        ThresholdDecisionEngine<String> engine = new ThresholdDecisionEngine<>(THRESHOLDS);
        List<ScoredCandidate<String>> candidates = candidates(entry("best", 3.0));

        MatchResult<String> result = engine.decide(candidates);

        assertThat(result.getDecision()).isEqualTo(Decision.NO_MATCH);
        assertThat(result.getMatch()).isNull();
    }

    @Test
    void singleCandidateAboveMatchThresholdIsMatchWithNoSecondBest() {
        ThresholdDecisionEngine<String> engine = new ThresholdDecisionEngine<>(THRESHOLDS);
        List<ScoredCandidate<String>> candidates = candidates(entry("best", 12.0));

        MatchResult<String> result = engine.decide(candidates);

        assertThat(result.getDecision()).isEqualTo(Decision.MATCH);
        assertThat(result.getMatch()).isEqualTo("best");
        assertThat(result.hasSecondBest()).isFalse();
    }

    @Test
    void rankingIsDeterministicAcrossRepeatedRunsOnAShuffledEqualScoreInput() {
        ThresholdDecisionEngine<String> engine = new ThresholdDecisionEngine<>(THRESHOLDS);
        List<ScoredCandidate<String>> candidates = candidates(
                entry("a", 7.0), entry("b", 7.0), entry("c", 7.0), entry("d", 7.0), entry("e", 7.0));
        Collections.shuffle(candidates, new Random(42));

        List<String> firstRun = candidateOrder(engine.decide(candidates));
        List<String> secondRun = candidateOrder(engine.decide(candidates));
        List<String> thirdRun = candidateOrder(engine.decide(candidates));

        assertThat(firstRun).isEqualTo(secondRun).isEqualTo(thirdRun);
    }

    @Test
    void tiedScoresPreserveInputOrder() {
        ThresholdDecisionEngine<String> engine = new ThresholdDecisionEngine<>(THRESHOLDS);
        List<ScoredCandidate<String>> candidates = candidates(entry("first", 7.0), entry("second", 7.0));

        MatchResult<String> result = engine.decide(candidates);

        assertThat(candidateOrder(result)).containsExactly("first", "second");
    }

    private static List<String> candidateOrder(MatchResult<String> result) {
        List<String> order = new ArrayList<>();
        for (ScoredCandidate<String> candidate : result.getCandidates()) {
            order.add(candidate.getCandidate());
        }
        return order;
    }

    @SafeVarargs
    private static List<ScoredCandidate<String>> candidates(Object[]... entries) {
        List<ScoredCandidate<String>> result = new ArrayList<>();
        for (Object[] entry : entries) {
            String candidate = (String) entry[0];
            double value = (Double) entry[1];
            Score score = new Score(value, ScoreScale.POINTS, "test", null);
            result.add(new ScoredCandidate<>(candidate, score, new ArrayList<>()));
        }
        return result;
    }

    private static Object[] entry(String candidate, double value) {
        return new Object[] {candidate, value};
    }

    @Test
    void declaresTheThresholdsItWasConstructedWith() {
        DecisionThresholds thresholds = new DecisionThresholds(50.0, 20.0, 10.0, ScoreScale.POINTS);

        assertThat(new ThresholdDecisionEngine<String>(thresholds).declaredThresholds())
                .isSameAs(thresholds);
    }

    @Test
    void whatItDeclaresIsWhatItApplies() {
        // The declaration is only useful if it cannot drift from the
        // thresholds the decision is actually made against, so assert the
        // declared values produce the decision they describe: a score below
        // the declared match threshold must not come back MATCH.
        DecisionThresholds thresholds = new DecisionThresholds(50.0, 20.0, 10.0, ScoreScale.POINTS);
        ThresholdDecisionEngine<String> engine = new ThresholdDecisionEngine<>(thresholds);

        MatchResult<String> result = engine.decide(Collections.singletonList(
                new ScoredCandidate<>("candidate",
                        new Score(49.0, ScoreScale.POINTS, "test", null),
                        Collections.<FieldContribution>emptyList())));

        assertThat(engine.declaredThresholds().getMatchThreshold()).isEqualTo(50.0);
        assertThat(result.getDecision()).isNotEqualTo(Decision.MATCH);
    }
}
