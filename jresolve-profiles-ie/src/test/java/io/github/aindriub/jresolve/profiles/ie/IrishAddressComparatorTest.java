package io.github.aindriub.jresolve.profiles.ie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aindriub.jresolve.comparison.DefaultTokenSplitter;
import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.FieldEvidence;
import io.github.aindriub.jresolve.evidence.TokenSubsumption;
import io.github.aindriub.jresolve.field.SimilarityBands;
import io.github.aindriub.jresolve.field.TokenSubsumptionComparator;
import io.github.aindriub.jresolve.normalization.StringNormalizer;
import org.junit.jupiter.api.Test;

class IrishAddressComparatorTest {

    private final IrishAddressComparator comparator = new IrishAddressComparator();
    private final StringNormalizer normalizer = new IrishAddressNormalizer();

    private FieldEvidence compare(String left, String right) {
        return comparator.compare(normalizer.normalize(left), normalizer.normalize(right));
    }

    // ------------------------------------------------- the containment stage

    @Test
    void anIdenticalAddressIsExact() {
        FieldEvidence evidence = compare("12 Main Street, Dublin 4", "12 Main Street, Dublin 4");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.EXACT);
        assertThat(evidence.getSubsumption()).isSameAs(TokenSubsumption.EQUIVALENT);
    }

    @Test
    void punctuationDoesNotChangeTheOutcome() {
        FieldEvidence evidence = compare("12 Main Street, Dublin 4", "12 Main Street Dublin 4");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.EXACT);
    }

    @Test
    void aLessSpecificSourceIsSubsumedRatherThanConflicting() {
        FieldEvidence evidence = compare("12 Main Street", "12 Main Street, Dublin 4");

        assertThat(evidence.getCategory()).isSameAs(TokenSubsumptionComparator.SUBSUMED);
        assertThat(evidence.getSubsumption()).isSameAs(TokenSubsumption.LEFT_SUBSUMES_RIGHT);
        assertThat(evidence.getCategory()).isNotSameAs(ComparisonCategory.CONFLICT);
    }

    // --------------------------------------------------- D9's ambiguous case

    @Test
    void aSourceInsideTwoCandidatesIsEquallyConsistentWithBoth() {
        // D9 §90: source "Dublin" against "Dublin 4" and "Dublin 8". Both
        // contain it and neither contradicts it, so the evidence is identical
        // on every axis — category, direction and similarity. That identity is
        // *why* there is no honest basis to prefer one, which is the whole
        // argument for recording containment separately from similarity.
        FieldEvidence first = compare("Dublin", "Dublin 4");
        FieldEvidence second = compare("Dublin", "Dublin 8");

        assertThat(first.getCategory()).isSameAs(TokenSubsumptionComparator.SUBSUMED);
        assertThat(first.getCategory()).isSameAs(second.getCategory());
        assertThat(first.getSubsumption()).isSameAs(TokenSubsumption.LEFT_SUBSUMES_RIGHT);
        assertThat(first.getSubsumption()).isSameAs(second.getSubsumption());
        assertThat(first.getSimilarity()).isEqualTo(second.getSimilarity());
    }

    @Test
    void theTwoCandidatesThemselvesAreNotSubsumed() {
        // "Dublin 4" and "Dublin 8" share a token but neither contains the
        // other, so no containment is reported for the pair.
        FieldEvidence evidence = compare("Dublin 4", "Dublin 8");

        assertThat(evidence.getCategory()).isNotSameAs(TokenSubsumptionComparator.SUBSUMED);
        assertThat(evidence.getSubsumption()).isSameAs(TokenSubsumption.NEITHER);
    }

    // ------------------------------------------------------ the spelling stage

    @Test
    void aMistypedAddressIsRescuedByTheSpellingStage() {
        // One transposed character leaves the two token sets sharing nothing
        // but the house number, so containment alone would call this a near
        // conflict. It is plainly the same place.
        FieldEvidence evidence = compare("12 Main Street", "12 Mian Street");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.VERY_HIGH);
    }

    @Test
    void aGenuinelyDifferentAddressStaysLow() {
        FieldEvidence evidence = compare("12 Main Street, Dublin 4", "99 Oak Avenue, Cork");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.LOW);
    }

    @Test
    void subsumptionAndContradictionAreDistinguishable() {
        FieldEvidence subsuming = compare("Dublin", "Dublin 4");
        FieldEvidence different = compare("12 Main Street, Dublin 4", "99 Oak Avenue, Cork");

        assertThat(subsuming.getCategory()).isNotSameAs(different.getCategory());
        assertThat(subsuming.getSubsumption()).isNotSameAs(different.getSubsumption());
    }

    @Test
    void theSpellingStageDoesNotOverrideContainment() {
        // A containment verdict stands even though the fallback would have had
        // something to say about the same pair.
        FieldEvidence evidence = compare("Dublin", "Dublin 4");

        assertThat(evidence.getCategory()).isSameAs(TokenSubsumptionComparator.SUBSUMED);
    }

    // ------------------------------------------ core's extension points, reused

    @Test
    void theConfiguredBandsAreApplied() {
        // The same pair that bands VERY_HIGH under the defaults must band HIGH
        // under a stricter top threshold. A comparator that reimplemented the
        // banding with its own constants would pass every other test here and
        // fail this one.
        IrishAddressComparator strict = new IrishAddressComparator(
                new DefaultTokenSplitter(), new SimilarityBands(0.99, 0.90, 0.80, 0.0));

        FieldEvidence evidence = strict.compare(
                normalizer.normalize("12 Main Street"), normalizer.normalize("12 Mian Street"));

        assertThat(compare("12 Main Street", "12 Mian Street").getCategory())
                .isSameAs(ComparisonCategory.VERY_HIGH);
        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.HIGH);
    }

    @Test
    void nullHandlingIsInheritedFromCore() {
        assertThat(comparator.compare(null, null).getCategory())
                .isSameAs(ComparisonCategory.MISSING_BOTH);
        assertThat(comparator.compare(null, "dublin").getCategory())
                .isSameAs(ComparisonCategory.MISSING_ONE);
        assertThat(comparator.compare("dublin", null).getCategory())
                .isSameAs(ComparisonCategory.MISSING_ONE);
    }

    @Test
    void noNullCombinationThrows() {
        assertThat(comparator.compare(null, null)).isNotNull();
        assertThat(comparator.compare(null, "dublin")).isNotNull();
        assertThat(comparator.compare("dublin", null)).isNotNull();
    }

    // ------------------------------------------------------------- symmetry

    @Test
    void swappingTheArgumentsSwapsTheDirectionOnly() {
        FieldEvidence forward = compare("Dublin", "Dublin 4");
        FieldEvidence reversed = compare("Dublin 4", "Dublin");

        assertThat(forward.getCategory()).isSameAs(reversed.getCategory());
        assertThat(forward.getSimilarity()).isEqualTo(reversed.getSimilarity());
        assertThat(forward.getSubsumption()).isSameAs(TokenSubsumption.LEFT_SUBSUMES_RIGHT);
        assertThat(reversed.getSubsumption()).isSameAs(TokenSubsumption.RIGHT_SUBSUMES_LEFT);
    }

    // --------------------------------------------------------- construction

    @Test
    void rejectsANullSplitter() {
        assertThatThrownBy(() -> new IrishAddressComparator(null, new SimilarityBands()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullBands() {
        assertThatThrownBy(() -> new IrishAddressComparator(new DefaultTokenSplitter(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
