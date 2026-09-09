package io.github.aindriub.jresolve.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The result of resolving one source record against its candidates.
 *
 * @param <C> the candidate type; owned entirely by the consumer
 */
public final class MatchResult<C> {

    private final Decision decision;
    private final C match;
    private final Score score;
    private final Score secondBestScore;
    private final List<ScoredCandidate<C>> candidates;

    /**
     * @param decision the outcome
     * @param match the matched candidate; should be null unless
     *     {@code decision == Decision.MATCH}
     * @param score the best candidate's score, or null if there were no
     *     candidates
     * @param secondBestScore the second-best candidate's score, or null if
     *     there was no second candidate
     * @param candidates every candidate considered, ranked best first; copied
     *     defensively
     */
    public MatchResult(Decision decision, C match, Score score, Score secondBestScore,
            List<ScoredCandidate<C>> candidates) {
        if (decision == null) {
            throw new IllegalArgumentException("decision must not be null");
        }
        if (candidates == null) {
            throw new IllegalArgumentException("candidates must not be null");
        }
        if (match != null && decision != Decision.MATCH) {
            throw new IllegalArgumentException("match must be null unless decision == Decision.MATCH");
        }
        this.decision = decision;
        this.match = match;
        this.score = score;
        this.secondBestScore = secondBestScore;
        this.candidates = Collections.unmodifiableList(new ArrayList<ScoredCandidate<C>>(candidates));
    }

    public Decision getDecision() {
        return decision;
    }

    /**
     * Derived from {@link #getDecision()}: true if and only if the decision
     * is {@link Decision#MATCH}.
     */
    public boolean isMatch() {
        return decision == Decision.MATCH;
    }

    /**
     * The matched candidate. Null for every decision other than
     * {@link Decision#MATCH}.
     */
    public C getMatch() {
        return match;
    }

    /**
     * The best candidate's score, or null if there were no candidates.
     */
    public Score getScore() {
        return score;
    }

    /**
     * Whether a second-best candidate was scored. {@link #getMargin()} is
     * only meaningful when this returns true.
     */
    public boolean hasSecondBest() {
        return secondBestScore != null;
    }

    /**
     * The second-best candidate's score, or null if there was no second
     * candidate.
     */
    public Score getSecondBestScore() {
        return secondBestScore;
    }

    /**
     * The gap between the best and second-best score, on the best score's
     * scale. Only meaningful when {@link #hasSecondBest()} is true; there is
     * no honest value to return when there is no second candidate, so this
     * throws rather than returning a sentinel that arithmetic could later be
     * done to.
     *
     * @throws IllegalStateException if {@link #hasSecondBest()} is false
     */
    public double getMargin() {
        if (secondBestScore == null || score == null) {
            throw new IllegalStateException("margin is not meaningful without a second-best score; check hasSecondBest() first");
        }
        return score.getValue() - secondBestScore.getValue();
    }

    /**
     * Every candidate considered, ranked best first.
     */
    public List<ScoredCandidate<C>> getCandidates() {
        return candidates;
    }

    @Override
    public String toString() {
        // getMatch()'s candidate is deliberately excluded: it is a consumer
        // type whose toString() may carry a field value.
        return "MatchResult{decision=" + decision + ", score=" + score
                + ", secondBestScore=" + secondBestScore + ", candidateCount=" + candidates.size() + '}';
    }
}
