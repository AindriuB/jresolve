package io.github.aindriub.jresolve.scoring;

import io.github.aindriub.jresolve.evidence.MatchEvidence;
import io.github.aindriub.jresolve.result.ScoreScale;

/**
 * Turns the evidence produced by comparing a source record against a
 * candidate into a {@link ScoringResult}.
 *
 * <p>Implementations declare the {@link ScoreScale} their scores are
 * expressed on so a decision engine's thresholds can be checked against it
 * rather than assumed.
 */
public interface MatchScorer {

    /**
     * Scores one candidate's evidence.
     *
     * @param evidence the evidence to score; never null
     * @return a scorable result carrying a score and contributions, or an
     *     unscorable result naming why scoring could not proceed
     */
    ScoringResult score(MatchEvidence evidence);

    /**
     * The scale every {@link io.github.aindriub.jresolve.result.Score} this
     * scorer produces is expressed on.
     */
    ScoreScale scale();
}
