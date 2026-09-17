package io.github.aindriub.jresolve.decision;

import io.github.aindriub.jresolve.result.Decision;
import io.github.aindriub.jresolve.result.MatchResult;
import io.github.aindriub.jresolve.result.Score;
import io.github.aindriub.jresolve.result.ScoredCandidate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A {@link MatchDecisionEngine} that ranks candidates by score value
 * descending and applies a fixed set of {@link DecisionThresholds}.
 *
 * <ul>
 *   <li>No candidates: {@code NO_MATCH}, no score.</li>
 *   <li>Best score at or above {@code matchThreshold}, with either no second
 *       candidate or a margin at or above {@code minimumMargin}:
 *       {@code MATCH}.</li>
 *   <li>Best score at or above {@code matchThreshold} but the margin over the
 *       second candidate falls short of {@code minimumMargin}: {@code
 *       REVIEW}.</li>
 *   <li>Best score at or above {@code reviewThreshold} but below {@code
 *       matchThreshold}: {@code REVIEW}.</li>
 *   <li>Best score below {@code reviewThreshold}: {@code NO_MATCH}.</li>
 * </ul>
 *
 * <p>Ranking uses a stable sort, so candidates tied on score value keep their
 * relative input order rather than an arbitrary one.
 *
 * @param <C> the candidate type; owned entirely by the consumer
 */
public final class ThresholdDecisionEngine<C> implements MatchDecisionEngine<C> {

    private final DecisionThresholds thresholds;

    public ThresholdDecisionEngine(DecisionThresholds thresholds) {
        if (thresholds == null) {
            throw new IllegalArgumentException("thresholds must not be null");
        }
        this.thresholds = thresholds;
    }

    /**
     * The thresholds passed to the constructor — the ones every decision
     * below is actually made against.
     */
    @Override
    public DecisionThresholds declaredThresholds() {
        return thresholds;
    }

    @Override
    public MatchResult<C> decide(List<ScoredCandidate<C>> candidates) {
        if (candidates == null) {
            throw new IllegalArgumentException("candidates must not be null");
        }
        if (candidates.isEmpty()) {
            return new MatchResult<C>(Decision.NO_MATCH, null, null, null, candidates);
        }

        List<ScoredCandidate<C>> ranked = rank(candidates);
        ScoredCandidate<C> best = ranked.get(0);
        ScoredCandidate<C> second = ranked.size() > 1 ? ranked.get(1) : null;
        Score bestScore = best.getScore();
        Score secondScore = second != null ? second.getScore() : null;

        if (bestScore.getValue() >= thresholds.getMatchThreshold()) {
            if (secondScore == null) {
                return new MatchResult<C>(Decision.MATCH, best.getCandidate(), bestScore, null, ranked);
            }
            double margin = bestScore.getValue() - secondScore.getValue();
            if (margin >= thresholds.getMinimumMargin()) {
                return new MatchResult<C>(Decision.MATCH, best.getCandidate(), bestScore, secondScore, ranked);
            }
            return new MatchResult<C>(Decision.REVIEW, null, bestScore, secondScore, ranked);
        }
        if (bestScore.getValue() >= thresholds.getReviewThreshold()) {
            return new MatchResult<C>(Decision.REVIEW, null, bestScore, secondScore, ranked);
        }
        return new MatchResult<C>(Decision.NO_MATCH, null, bestScore, secondScore, ranked);
    }

    private List<ScoredCandidate<C>> rank(List<ScoredCandidate<C>> candidates) {
        List<ScoredCandidate<C>> ranked = new ArrayList<ScoredCandidate<C>>(candidates);
        Collections.sort(ranked, new Comparator<ScoredCandidate<C>>() {
            @Override
            public int compare(ScoredCandidate<C> left, ScoredCandidate<C> right) {
                return Double.compare(right.getScore().getValue(), left.getScore().getValue());
            }
        });
        return ranked;
    }
}
