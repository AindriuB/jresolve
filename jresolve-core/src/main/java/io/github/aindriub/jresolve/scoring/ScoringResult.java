package io.github.aindriub.jresolve.scoring;

import io.github.aindriub.jresolve.result.FieldContribution;
import io.github.aindriub.jresolve.result.Score;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The outcome of {@link MatchScorer#score(io.github.aindriub.jresolve.evidence.MatchEvidence)}.
 *
 * <p>A result is either scorable — carrying a {@link Score} and an ordered
 * list of {@link FieldContribution}s — or unscorable, carrying a template key
 * that names why scoring was not possible and no score at all.
 * {@link #isScorable()} distinguishes the two; calling the score-only or
 * unscorable-only accessor on the wrong kind throws rather than returning
 * null, so a caller cannot silently read a stale value.
 */
public final class ScoringResult {

    private final boolean scorable;
    private final Score score;
    private final List<FieldContribution> contributions;
    private final String unscorableTemplateKey;

    private ScoringResult(boolean scorable, Score score, List<FieldContribution> contributions,
            String unscorableTemplateKey) {
        this.scorable = scorable;
        this.score = score;
        this.contributions = contributions;
        this.unscorableTemplateKey = unscorableTemplateKey;
    }

    /**
     * A scorable result.
     *
     * @param score the score; must not be null
     * @param contributions the per-field contributions, in the order an
     *     explanation should read them; copied defensively, must not be null
     */
    public static ScoringResult scorable(Score score, List<FieldContribution> contributions) {
        if (score == null) {
            throw new IllegalArgumentException("score must not be null");
        }
        if (contributions == null) {
            throw new IllegalArgumentException("contributions must not be null");
        }
        return new ScoringResult(true, score,
                Collections.unmodifiableList(new ArrayList<FieldContribution>(contributions)), null);
    }

    /**
     * An unscorable result.
     *
     * @param templateKey a key identifying why scoring could not proceed,
     *     never a rendered sentence and never a value; must not be null,
     *     empty or whitespace-only
     */
    public static ScoringResult unscorable(String templateKey) {
        if (templateKey == null || templateKey.trim().isEmpty()) {
            throw new IllegalArgumentException("templateKey must not be null, empty or whitespace-only");
        }
        return new ScoringResult(false, null, Collections.<FieldContribution>emptyList(), templateKey);
    }

    /**
     * True if this result carries a {@link #getScore()} and
     * {@link #getContributions()}; false if it carries only
     * {@link #getUnscorableTemplateKey()}.
     */
    public boolean isScorable() {
        return scorable;
    }

    /**
     * @throws IllegalStateException if {@link #isScorable()} is false
     */
    public Score getScore() {
        requireScorable();
        return score;
    }

    /**
     * @throws IllegalStateException if {@link #isScorable()} is false
     */
    public List<FieldContribution> getContributions() {
        requireScorable();
        return contributions;
    }

    /**
     * @throws IllegalStateException if {@link #isScorable()} is true
     */
    public String getUnscorableTemplateKey() {
        if (scorable) {
            throw new IllegalStateException("no unscorable template key; check isScorable() first");
        }
        return unscorableTemplateKey;
    }

    private void requireScorable() {
        if (!scorable) {
            throw new IllegalStateException("result is unscorable; check isScorable() first");
        }
    }

    @Override
    public String toString() {
        return scorable
                ? "ScoringResult{scorable=true, score=" + score + ", contributions=" + contributions + '}'
                : "ScoringResult{scorable=false, unscorableTemplateKey=" + unscorableTemplateKey + '}';
    }
}
