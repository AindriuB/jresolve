package io.github.aindriub.jresolve.field;

import io.github.aindriub.jresolve.comparison.TokenSplitter;
import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.DefaultFieldEvidence;
import io.github.aindriub.jresolve.evidence.FieldEvidence;
import io.github.aindriub.jresolve.evidence.TokenSubsumption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Compares two values by whether one side's tokens are contained in the
 * other's.
 *
 * <p>This exists because <em>less specific</em> and <em>contradictory</em> are
 * different findings, and a symmetric string similarity cannot tell them
 * apart. A value that omits detail the other carries does not disagree with
 * it, yet a raw edit distance scores that pair much like two values that
 * genuinely conflict.
 *
 * <h2>What it reports</h2>
 *
 * <table border="1">
 *   <caption>Category and signal for each relation between the token sets</caption>
 *   <tr><th>Token sets</th><th>Category</th><th>Subsumption</th></tr>
 *   <tr><td>equal, non-empty</td><td>{@code EXACT}</td><td>{@code EQUIVALENT}</td></tr>
 *   <tr><td>left strictly inside right</td><td>{@link #SUBSUMED}</td><td>{@code LEFT_SUBSUMES_RIGHT}</td></tr>
 *   <tr><td>right strictly inside left</td><td>{@link #SUBSUMED}</td><td>{@code RIGHT_SUBSUMES_LEFT}</td></tr>
 *   <tr><td>overlapping, neither inside the other</td><td>{@link #PARTIAL_OVERLAP}</td><td>{@code NEITHER}</td></tr>
 *   <tr><td>disjoint, both non-empty</td><td>{@code CONFLICT}</td><td>{@code NEITHER}</td></tr>
 *   <tr><td>exactly one empty</td><td>{@code MISSING_ONE}</td><td>{@code NEITHER}</td></tr>
 *   <tr><td>both empty</td><td>{@code MISSING_BOTH}</td><td>{@code NEITHER}</td></tr>
 * </table>
 *
 * <p>The table covers two values that are both present. A null on either
 * side is answered by the inherited null rule before this comparator runs, so
 * it reports {@code MISSING_ONE} or {@code MISSING_BOTH} with the subsumption
 * left at {@code NOT_APPLICABLE} — nothing was computed, which is precisely
 * what that value means. Contrast the both-empty and one-empty rows above,
 * which reach the same categories having computed containment and found none.
 *
 * <p>Strict containment is deliberately <strong>not</strong> {@code CONFLICT}.
 * Recording it as a conflict is the specific error this comparator exists to
 * stop: it turns "the source said less" into evidence against a match.
 *
 * <p>The two containment directions share one category and differ only in the
 * signal. A scoring model keys on the category, and being less specific is
 * equally (in)formative whichever side is the shorter one; a consumer that
 * needs the direction reads {@link FieldEvidence#getSubsumption()}.
 *
 * <h2>Reading the direction</h2>
 *
 * <p>{@code LEFT_SUBSUMES_RIGHT} means the <em>left</em> value is the less
 * specific one — every token it has appears on the right, and the right has
 * more. So {@code compare("alpha", "alpha bravo")} reports
 * {@code LEFT_SUBSUMES_RIGHT}.
 *
 * <h2>An empty token set is not a missing value</h2>
 *
 * <p>A value that tokenises to nothing — punctuation only, say — reaches this
 * comparator as a real value, where a null never does. The categories still
 * coincide with the null case, because the <em>evidence</em> is the same: a
 * value carrying no tokens tells us nothing about the other side. What is
 * refused is the vacuous truth that the empty set is a subset of everything;
 * reporting that as containment would manufacture agreement out of an empty
 * value.
 *
 * <h2>Symmetry</h2>
 *
 * <p>This comparator is <strong>asymmetric</strong> by design: swapping the
 * arguments swaps the subsumption direction. That is expected rather than a
 * defect — the exact-symmetry property belongs to {@code SimilarityMetric}
 * implementations, and a comparator that could not distinguish "the left said
 * less" from "the right said less" would have nothing to contribute.
 * Categories and similarity are symmetric; only the signal flips.
 */
public final class TokenSubsumptionComparator extends AbstractNullSafeFieldComparator<String> {

    /** One side's tokens are strictly contained in the other's. */
    public static final ComparisonCategory SUBSUMED = ComparisonCategory.of("SUBSUMED");

    /** The sides share tokens, but neither contains the other. */
    public static final ComparisonCategory PARTIAL_OVERLAP =
            ComparisonCategory.of("PARTIAL_OVERLAP");

    private final TokenSplitter splitter;

    /**
     * @param splitter tokenises each prepared value
     * @throws IllegalArgumentException if {@code splitter} is null
     */
    public TokenSubsumptionComparator(TokenSplitter splitter) {
        if (splitter == null) {
            throw new IllegalArgumentException("splitter must not be null");
        }
        this.splitter = splitter;
    }

    @Override
    protected FieldEvidence compareNonNull(String left, String right) {
        Set<String> leftTokens = tokenSetOf(left);
        Set<String> rightTokens = tokenSetOf(right);

        if (leftTokens.isEmpty() && rightTokens.isEmpty()) {
            return evidence(ComparisonCategory.MISSING_BOTH, null, TokenSubsumption.NEITHER);
        }
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return evidence(ComparisonCategory.MISSING_ONE, null, TokenSubsumption.NEITHER);
        }

        double overlap = jaccard(leftTokens, rightTokens);

        if (leftTokens.equals(rightTokens)) {
            return evidence(ComparisonCategory.EXACT, overlap, TokenSubsumption.EQUIVALENT);
        }
        if (rightTokens.containsAll(leftTokens)) {
            return evidence(SUBSUMED, overlap, TokenSubsumption.LEFT_SUBSUMES_RIGHT);
        }
        if (leftTokens.containsAll(rightTokens)) {
            return evidence(SUBSUMED, overlap, TokenSubsumption.RIGHT_SUBSUMES_LEFT);
        }
        if (overlap > 0.0) {
            return evidence(PARTIAL_OVERLAP, overlap, TokenSubsumption.NEITHER);
        }
        return evidence(ComparisonCategory.CONFLICT, overlap, TokenSubsumption.NEITHER);
    }

    private Set<String> tokenSetOf(String value) {
        List<String> tokens = splitter.split(value);
        return new HashSet<>(tokens);
    }

    /**
     * Shared tokens over total distinct tokens. Reported on
     * {@link FieldEvidence#getSimilarity()} because it is genuinely computed
     * here, and left null on the two missing arms where nothing was measured.
     */
    private static double jaccard(Set<String> left, Set<String> right) {
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        return (double) intersection.size() / union.size();
    }

    private static FieldEvidence evidence(
            ComparisonCategory category, Double similarity, TokenSubsumption subsumption) {
        // No frequency key: even an EXACT here is agreement on a token set
        // rather than on a value, and two different strings can produce it.
        return new DefaultFieldEvidence(category, similarity, null, subsumption);
    }
}
