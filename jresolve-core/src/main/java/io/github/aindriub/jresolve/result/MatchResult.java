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
    private final List<RejectedCandidate<C>> rejected;

    /**
     * The legal combinations of {@code decision}, {@code match}, {@code
     * score} and {@code secondBestScore} are exactly these:
     * <ul>
     *   <li>{@code decision == MATCH}: {@code match} must be non-null and
     *       {@code score} must be non-null (a match is meaningless without
     *       both the candidate and the score that justified it);
     *       {@code secondBestScore} may be null or non-null.</li>
     *   <li>{@code decision == REVIEW}: {@code match} must be null (nothing
     *       is matched yet) and {@code score} must be non-null (review means
     *       a scored candidate is ambiguous enough to need a human, so there
     *       is always a score to be ambiguous about); {@code secondBestScore}
     *       may be null or non-null.</li>
     *   <li>{@code decision == NO_MATCH}: {@code match} must be null.
     *       {@code score} may be null (no candidates were evaluated at all)
     *       or non-null (candidates were evaluated but none qualified). If
     *       {@code score} is null, {@code secondBestScore} must also be
     *       null.</li>
     * </ul>
     * Independent of decision, {@code secondBestScore} must be null whenever
     * {@code score} is null: a runner-up score is only coherent alongside the
     * leader it trails.
     *
     * @param decision the outcome
     * @param match the matched candidate; must be non-null when
     *     {@code decision == Decision.MATCH} and must be null for every
     *     other decision
     * @param score the best candidate's score; null only when
     *     {@code decision == Decision.NO_MATCH} and no candidates were
     *     evaluated
     * @param secondBestScore the second-best candidate's score, or null if
     *     there was no second candidate; must be null whenever {@code score}
     *     is null
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
        if (match == null && decision == Decision.MATCH) {
            throw new IllegalArgumentException("match must be non-null when decision == Decision.MATCH");
        }
        if (score == null && (decision == Decision.MATCH || decision == Decision.REVIEW)) {
            throw new IllegalArgumentException(
                    "score must be non-null when decision == Decision.MATCH or Decision.REVIEW");
        }
        if (secondBestScore != null && score == null) {
            throw new IllegalArgumentException("secondBestScore must be null when score is null");
        }
        this.decision = decision;
        this.match = match;
        this.score = score;
        this.secondBestScore = secondBestScore;
        this.candidates = Collections.unmodifiableList(new ArrayList<ScoredCandidate<C>>(candidates));
        this.rejected = Collections.<RejectedCandidate<C>>emptyList();
    }

    private MatchResult(MatchResult<C> source, List<RejectedCandidate<C>> rejected) {
        this.decision = source.decision;
        this.match = source.match;
        this.score = source.score;
        this.secondBestScore = source.secondBestScore;
        this.candidates = source.candidates;
        this.rejected = Collections.unmodifiableList(new ArrayList<RejectedCandidate<C>>(rejected));
    }

    /**
     * Returns a copy of this result carrying the candidates a
     * {@code CandidateRule} vetoed.
     *
     * <p>A copy rather than a constructor parameter so that the change is
     * additive: every existing caller of the public constructor keeps
     * compiling, and a {@code MatchDecisionEngine} written before rejections
     * existed keeps producing a valid result. The resolver attaches them after
     * the engine has decided, which also makes it structurally impossible for
     * a veto to influence the decision.
     *
     * @param rejected the vetoed candidates; never null, copied defensively
     */
    public MatchResult<C> withRejected(List<RejectedCandidate<C>> rejected) {
        if (rejected == null) {
            throw new IllegalArgumentException("rejected must not be null");
        }
        return new MatchResult<C>(this, rejected);
    }

    public Decision getDecision() {
        return decision;
    }

    /**
     * The candidates a {@code CandidateRule} vetoed, each with the rule that
     * vetoed it, in the order the candidates were supplied.
     *
     * <p>Never null, and empty unless the resolver attached rejections. A
     * vetoed candidate never appears in {@link #getCandidates()}: it was
     * dropped before scoring, so it has no score to rank. That is the
     * distinction this accessor exists to make visible — without it a
     * consumer cannot tell a candidate that scored badly from one that was
     * never scored.
     */
    public List<RejectedCandidate<C>> getRejectedCandidates() {
        return rejected;
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
