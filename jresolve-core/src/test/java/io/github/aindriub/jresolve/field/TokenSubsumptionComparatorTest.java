package io.github.aindriub.jresolve.field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aindriub.jresolve.comparison.DefaultTokenSplitter;
import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.FieldEvidence;
import io.github.aindriub.jresolve.evidence.TokenSubsumption;
import org.junit.jupiter.api.Test;

/**
 * Fixtures are neutral tokens throughout. Wiring this comparator to any
 * particular kind of field is the profiles module's job, not core's.
 */
class TokenSubsumptionComparatorTest {

    private final TokenSubsumptionComparator comparator =
            new TokenSubsumptionComparator(new DefaultTokenSplitter());

    // ------------------------------------------------- every relation arm

    @Test
    void equalTokenSetsAreExactAndEquivalent() {
        FieldEvidence evidence = comparator.compare("alpha bravo", "alpha bravo");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.EXACT);
        assertThat(evidence.getSubsumption()).isSameAs(TokenSubsumption.EQUIVALENT);
    }

    @Test
    void tokenOrderDoesNotMatter() {
        FieldEvidence evidence = comparator.compare("alpha bravo", "bravo alpha");

        assertThat(evidence.getSubsumption()).isSameAs(TokenSubsumption.EQUIVALENT);
    }

    @Test
    void aStrictSubsetOnTheLeftSubsumesRight() {
        FieldEvidence evidence = comparator.compare("alpha", "alpha bravo");

        assertThat(evidence.getCategory()).isSameAs(TokenSubsumptionComparator.SUBSUMED);
        assertThat(evidence.getSubsumption()).isSameAs(TokenSubsumption.LEFT_SUBSUMES_RIGHT);
    }

    @Test
    void aStrictSubsetOnTheRightSubsumesLeft() {
        FieldEvidence evidence = comparator.compare("alpha bravo", "alpha");

        assertThat(evidence.getCategory()).isSameAs(TokenSubsumptionComparator.SUBSUMED);
        assertThat(evidence.getSubsumption()).isSameAs(TokenSubsumption.RIGHT_SUBSUMES_LEFT);
    }

    @Test
    void partialOverlapIsNeither() {
        FieldEvidence evidence = comparator.compare("alpha bravo", "bravo charlie");

        assertThat(evidence.getCategory()).isSameAs(TokenSubsumptionComparator.PARTIAL_OVERLAP);
        assertThat(evidence.getSubsumption()).isSameAs(TokenSubsumption.NEITHER);
    }

    @Test
    void disjointTokenSetsConflict() {
        FieldEvidence evidence = comparator.compare("alpha bravo", "charlie delta");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.CONFLICT);
        assertThat(evidence.getSubsumption()).isSameAs(TokenSubsumption.NEITHER);
    }

    @Test
    void neverReportsNotApplicableWhenBothValuesArePresent() {
        // Whenever this comparator actually runs, it computes containment, so
        // the "not computed" value must not escape it. A null combination is
        // the deliberate exception and is asserted separately below: there the
        // inherited rule answers and nothing was computed.
        String[][] pairs = {
            {"alpha bravo", "alpha bravo"},
            {"alpha", "alpha bravo"},
            {"alpha bravo", "alpha"},
            {"alpha bravo", "bravo charlie"},
            {"alpha", "bravo"},
            {"---", "alpha"},
            {"---", "..."},
        };
        for (String[] pair : pairs) {
            assertThat(comparator.compare(pair[0], pair[1]).getSubsumption())
                    .isNotSameAs(TokenSubsumption.NOT_APPLICABLE);
        }
    }

    // ------------------------------------------ containment is not conflict

    @Test
    void strictContainmentIsNotAConflict() {
        // The specific error this comparator exists to stop: a value that says
        // less than the other does not disagree with it.
        FieldEvidence evidence = comparator.compare("alpha", "alpha bravo charlie");

        assertThat(evidence.getCategory()).isNotSameAs(ComparisonCategory.CONFLICT);
    }

    @Test
    void containmentAndDisjointnessAreDifferentCategories() {
        FieldEvidence contained = comparator.compare("alpha", "alpha bravo");
        FieldEvidence disjoint = comparator.compare("alpha", "bravo");

        assertThat(contained.getCategory()).isNotSameAs(disjoint.getCategory());
    }

    // ---------------------------------------------------- direction naming

    @Test
    void leftSubsumesRightMeansTheLeftIsTheLessSpecificValue() {
        // Pinned so the two directions cannot be transposed silently: the
        // shorter value is on the left, and the name says LEFT.
        FieldEvidence evidence = comparator.compare("alpha", "alpha bravo");

        assertThat(evidence.getSubsumption()).isSameAs(TokenSubsumption.LEFT_SUBSUMES_RIGHT);
    }

    @Test
    void swappingTheArgumentsSwapsTheDirection() {
        FieldEvidence forward = comparator.compare("alpha", "alpha bravo");
        FieldEvidence reversed = comparator.compare("alpha bravo", "alpha");

        assertThat(forward.getSubsumption()).isSameAs(TokenSubsumption.LEFT_SUBSUMES_RIGHT);
        assertThat(reversed.getSubsumption()).isSameAs(TokenSubsumption.RIGHT_SUBSUMES_LEFT);
    }

    @Test
    void categoryAndSimilarityStaySymmetricWhileTheDirectionFlips() {
        FieldEvidence forward = comparator.compare("alpha", "alpha bravo");
        FieldEvidence reversed = comparator.compare("alpha bravo", "alpha");

        assertThat(forward.getCategory()).isSameAs(reversed.getCategory());
        assertThat(forward.getSimilarity()).isEqualTo(reversed.getSimilarity());
        assertThat(forward.getSubsumption()).isNotSameAs(reversed.getSubsumption());
    }

    // ------------------------------------------- the ambiguous-source shape

    @Test
    void twoCandidatesThatBothContainTheSourceAreIndistinguishable() {
        // The shape that makes an ambiguous source legible: one less specific
        // value against two fuller ones. Both contain it, neither contradicts
        // it, and they report the same category and the same direction — which
        // is *why* there is no honest basis to prefer one, rather than an
        // artefact of the metric.
        FieldEvidence first = comparator.compare("alpha", "alpha bravo");
        FieldEvidence second = comparator.compare("alpha", "alpha charlie");

        assertThat(first.getCategory()).isSameAs(second.getCategory());
        assertThat(first.getSubsumption()).isSameAs(second.getSubsumption());
        assertThat(first.getSimilarity()).isEqualTo(second.getSimilarity());
    }

    @Test
    void aContradictingCandidateIsDistinguishableFromASubsumingOne() {
        FieldEvidence subsuming = comparator.compare("alpha", "alpha bravo");
        FieldEvidence contradicting = comparator.compare("alpha", "charlie delta");

        assertThat(subsuming.getCategory()).isNotSameAs(contradicting.getCategory());
    }

    // ------------------------------------------------------- repeated tokens

    @Test
    void aRepeatedTokenDoesNotChangeTheOutcome() {
        FieldEvidence once = comparator.compare("alpha bravo", "alpha");
        FieldEvidence twice = comparator.compare("alpha bravo alpha", "alpha alpha");

        assertThat(twice.getCategory()).isSameAs(once.getCategory());
        assertThat(twice.getSubsumption()).isSameAs(once.getSubsumption());
        assertThat(twice.getSimilarity()).isEqualTo(once.getSimilarity());
    }

    // ------------------------------------------------------------ similarity

    @Test
    void similarityIsTheSharedOverTheTotalDistinctTokens() {
        // {alpha} against {alpha, bravo}: one shared, two distinct overall.
        assertThat(comparator.compare("alpha", "alpha bravo").getSimilarity()).isEqualTo(0.5);
    }

    @Test
    void equalTokenSetsScoreOne() {
        assertThat(comparator.compare("alpha bravo", "bravo alpha").getSimilarity())
                .isEqualTo(1.0);
    }

    @Test
    void disjointTokenSetsScoreZero() {
        assertThat(comparator.compare("alpha", "bravo").getSimilarity()).isEqualTo(0.0);
    }

    @Test
    void theMissingArmsReportNoSimilarity() {
        // Nothing was measured on these arms, so no number is reported.
        assertThat(comparator.compare("---", "alpha").getSimilarity()).isNull();
        assertThat(comparator.compare("---", "...").getSimilarity()).isNull();
    }

    // ------------------------------------------- empty token sets vs nulls

    @Test
    void bothSidesTokenisingToNothingCarriesNoEvidence() {
        FieldEvidence evidence = comparator.compare("---", "...");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.MISSING_BOTH);
    }

    @Test
    void oneSideTokenisingToNothingCarriesNoEvidence() {
        FieldEvidence evidence = comparator.compare("---", "alpha bravo");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.MISSING_ONE);
    }

    @Test
    void anEmptyTokenSetIsNotReportedAsContainment() {
        // The empty set is a subset of everything. Reporting that as
        // containment would manufacture agreement out of a value that says
        // nothing at all.
        FieldEvidence evidence = comparator.compare("---", "alpha bravo");

        assertThat(evidence.getSubsumption()).isSameAs(TokenSubsumption.NEITHER);
        assertThat(evidence.getCategory()).isNotSameAs(TokenSubsumptionComparator.SUBSUMED);
    }

    @Test
    void anEmptyValueAndANullValueReachTheSameCategoryByDifferentPaths() {
        // Same evidence, different route: the null never reaches
        // compareNonNull, the punctuation-only value does.
        assertThat(comparator.compare("---", "alpha").getCategory())
                .isSameAs(comparator.compare(null, "alpha").getCategory());
    }

    // -------------------------------------------------------- null handling

    @Test
    void bothNullIsMissingBoth() {
        assertThat(comparator.compare(null, null).getCategory())
                .isSameAs(ComparisonCategory.MISSING_BOTH);
    }

    @Test
    void leftNullIsMissingOne() {
        assertThat(comparator.compare(null, "alpha").getCategory())
                .isSameAs(ComparisonCategory.MISSING_ONE);
    }

    @Test
    void rightNullIsMissingOne() {
        assertThat(comparator.compare("alpha", null).getCategory())
                .isSameAs(ComparisonCategory.MISSING_ONE);
    }

    @Test
    void aNullCombinationReportsNoSubsumptionAtAll() {
        // The inherited null rule never computed containment, so the signal is
        // NOT_APPLICABLE rather than NEITHER.
        assertThat(comparator.compare(null, "alpha").getSubsumption())
                .isSameAs(TokenSubsumption.NOT_APPLICABLE);
    }

    // --------------------------------------------------------- construction

    @Test
    void rejectsANullSplitter() {
        assertThatThrownBy(() -> new TokenSubsumptionComparator(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
