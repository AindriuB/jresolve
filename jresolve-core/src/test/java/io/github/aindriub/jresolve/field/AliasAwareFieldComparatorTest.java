package io.github.aindriub.jresolve.field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aindriub.jresolve.alias.AliasRepository;
import io.github.aindriub.jresolve.alias.DefaultAliasRepository;
import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.DefaultFieldEvidence;
import io.github.aindriub.jresolve.evidence.FieldEvidence;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Fixtures are neutral tokens. The real tables belong to the profiles module.
 */
class AliasAwareFieldComparatorTest {

    /** Records every call so a test can assert the delegate was not consulted. */
    private static final class RecordingDelegate implements FieldComparator<String> {

        private final List<String> calls = new ArrayList<>();
        private final FieldEvidence answer;

        RecordingDelegate(FieldEvidence answer) {
            this.answer = answer;
        }

        @Override
        public FieldEvidence compare(String left, String right) {
            calls.add(left + "|" + right);
            return answer;
        }
    }

    private static final FieldEvidence DELEGATE_ANSWER =
            new DefaultFieldEvidence(ComparisonCategory.LOW, 0.12, null);

    private static AliasRepository repository() {
        return DefaultAliasRepository.builder()
                .group(Arrays.asList("alpha", "zulu"), ComparisonCategory.ALIAS_TRANSLATION)
                .group(Arrays.asList("bravo", "brav"), ComparisonCategory.ALIAS_NICKNAME)
                .group(Arrays.asList("charlie", "charly"), ComparisonCategory.ALIAS_VARIANT)
                .build();
    }

    private static AliasAwareFieldComparator comparatorWith(RecordingDelegate delegate) {
        return new AliasAwareFieldComparator(repository(), delegate);
    }

    // ----------------------------------------------------------- the arms

    @Test
    void equalValuesAreExact() {
        FieldEvidence evidence = comparatorWith(new RecordingDelegate(DELEGATE_ANSWER))
                .compare("alpha", "alpha");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.EXACT);
    }

    @Test
    void equalValuesSetTheFrequencyKeyToTheAgreedValue() {
        FieldEvidence evidence = comparatorWith(new RecordingDelegate(DELEGATE_ANSWER))
                .compare("alpha", "alpha");

        assertThat(evidence.getFrequencyKey()).isEqualTo("alpha");
    }

    @Test
    void aTranslationPairYieldsTheRepositorysKind() {
        FieldEvidence evidence = comparatorWith(new RecordingDelegate(DELEGATE_ANSWER))
                .compare("alpha", "zulu");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.ALIAS_TRANSLATION);
    }

    @Test
    void aNicknamePairYieldsTheRepositorysKind() {
        FieldEvidence evidence = comparatorWith(new RecordingDelegate(DELEGATE_ANSWER))
                .compare("bravo", "brav");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.ALIAS_NICKNAME);
    }

    @Test
    void aVariantPairYieldsTheRepositorysKind() {
        FieldEvidence evidence = comparatorWith(new RecordingDelegate(DELEGATE_ANSWER))
                .compare("charlie", "charly");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.ALIAS_VARIANT);
    }

    @Test
    void anUnrelatedPairYieldsTheDelegatesAnswerUnchanged() {
        FieldEvidence evidence = comparatorWith(new RecordingDelegate(DELEGATE_ANSWER))
                .compare("alpha", "delta");

        assertThat(evidence).isSameAs(DELEGATE_ANSWER);
    }

    // ------------------------------------------------- delegate invocation

    @Test
    void theDelegateIsNotConsultedForEqualValues() {
        RecordingDelegate delegate = new RecordingDelegate(DELEGATE_ANSWER);

        comparatorWith(delegate).compare("alpha", "alpha");

        assertThat(delegate.calls).isEmpty();
    }

    @Test
    void theDelegateIsNotConsultedForAnAliasPair() {
        RecordingDelegate delegate = new RecordingDelegate(DELEGATE_ANSWER);

        comparatorWith(delegate).compare("alpha", "zulu");

        assertThat(delegate.calls).isEmpty();
    }

    @Test
    void theDelegateIsConsultedOnceForAnUnrelatedPair() {
        RecordingDelegate delegate = new RecordingDelegate(DELEGATE_ANSWER);

        comparatorWith(delegate).compare("alpha", "delta");

        assertThat(delegate.calls).containsExactly("alpha|delta");
    }

    // ----------------------------------------------- what an alias hit says

    @Test
    void anAliasHitCarriesNoFrequencyKey() {
        // The two sides did not agree on a value. A frequency table weighs
        // agreement on a common value less than on a rare one, and that only
        // means anything for pairs that actually agreed.
        FieldEvidence evidence = comparatorWith(new RecordingDelegate(DELEGATE_ANSWER))
                .compare("alpha", "zulu");

        assertThat(evidence.getFrequencyKey()).isNull();
    }

    @Test
    void anAliasHitCarriesNoSimilarity() {
        // A relation read from a table is not a measurement. Reporting a
        // number here would invite comparison against one that was computed.
        FieldEvidence evidence = comparatorWith(new RecordingDelegate(DELEGATE_ANSWER))
                .compare("alpha", "zulu");

        assertThat(evidence.getSimilarity()).isNull();
    }

    // ------------------------------------------------------------ symmetry

    @Test
    void isSymmetricAcrossAFixtureGroup() {
        AliasAwareFieldComparator comparator = comparatorWith(new RecordingDelegate(DELEGATE_ANSWER));
        List<String> values = Arrays.asList("alpha", "zulu", "bravo", "brav", "charlie", "delta");

        for (String left : values) {
            for (String right : values) {
                assertThat(comparator.compare(left, right).getCategory())
                        .isSameAs(comparator.compare(right, left).getCategory());
            }
        }
    }

    // --------------------------------------- the case normalization cannot fix

    @Test
    void resolvesAPairNoAmountOfNormalizingWouldBringTogether() {
        // "alpha" and "zulu" share not one character. No normalization rule
        // and no string metric closes that gap, because they are different
        // words for the same thing rather than two spellings of one word.
        // Only a table can know it, which is why this arm exists.
        RecordingDelegate delegate = new RecordingDelegate(
                new DefaultFieldEvidence(ComparisonCategory.CONFLICT, 0.0, null));

        FieldEvidence evidence = comparatorWith(delegate).compare("alpha", "zulu");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.ALIAS_TRANSLATION);
        assertThat(delegate.calls).isEmpty();
    }

    @Test
    void aTransitivelyClosedPairIsAlsoResolved() {
        // Never declared together; the repository closed the group.
        AliasRepository repository = DefaultAliasRepository.builder()
                .group(Arrays.asList("alpha", "bravo"), ComparisonCategory.ALIAS_NICKNAME)
                .group(Arrays.asList("alpha", "charlie"), ComparisonCategory.ALIAS_NICKNAME)
                .build();
        RecordingDelegate delegate = new RecordingDelegate(DELEGATE_ANSWER);

        FieldEvidence evidence =
                new AliasAwareFieldComparator(repository, delegate).compare("bravo", "charlie");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.ALIAS_NICKNAME);
    }

    // ------------------------------------------------------- null handling

    @Test
    void bothNullIsMissingBoth() {
        assertThat(comparatorWith(new RecordingDelegate(DELEGATE_ANSWER))
                .compare(null, null).getCategory())
                .isSameAs(ComparisonCategory.MISSING_BOTH);
    }

    @Test
    void leftNullIsMissingOne() {
        assertThat(comparatorWith(new RecordingDelegate(DELEGATE_ANSWER))
                .compare(null, "alpha").getCategory())
                .isSameAs(ComparisonCategory.MISSING_ONE);
    }

    @Test
    void rightNullIsMissingOne() {
        assertThat(comparatorWith(new RecordingDelegate(DELEGATE_ANSWER))
                .compare("alpha", null).getCategory())
                .isSameAs(ComparisonCategory.MISSING_ONE);
    }

    @Test
    void noNullCombinationReachesTheDelegateOrTheRepository() {
        RecordingDelegate delegate = new RecordingDelegate(DELEGATE_ANSWER);
        AliasAwareFieldComparator comparator = comparatorWith(delegate);

        comparator.compare(null, null);
        comparator.compare(null, "alpha");
        comparator.compare("alpha", null);

        // The repository throws on a null argument by contract, so reaching it
        // with one would surface as an exception rather than a wrong category.
        assertThat(delegate.calls).isEmpty();
    }

    // ------------------------------------------------------- construction

    @Test
    void rejectsANullRepository() {
        assertThatThrownBy(() -> new AliasAwareFieldComparator(
                null, new RecordingDelegate(DELEGATE_ANSWER)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANullDelegate() {
        assertThatThrownBy(() -> new AliasAwareFieldComparator(repository(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
