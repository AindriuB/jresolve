package io.github.aindriub.jresolve.field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.aindriub.jresolve.alias.AliasRepository;
import io.github.aindriub.jresolve.alias.DefaultAliasRepository;
import io.github.aindriub.jresolve.comparison.DefaultTokenSplitter;
import io.github.aindriub.jresolve.comparison.SimilarityMetric;
import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.FieldEvidence;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every {@link FieldComparator} in this package must be null-safe: no null
 * combination may throw, and the two missingness categories are the only
 * outcomes for a null combination.
 */
class FieldComparatorNullSafetyTest {

    private static final SimilarityMetric CONSTANT_METRIC = (left, right) -> 1.0;

    private static final AliasRepository EMPTY_REPOSITORY =
            DefaultAliasRepository.builder()
                    .group(Arrays.asList("alpha", "bravo"), ComparisonCategory.ALIAS_NICKNAME)
                    .build();

    private static List<FieldComparator<String>> comparators() {
        return Arrays.asList(
                new ExactFieldComparator<>(),
                new SimilarityFieldComparator(CONSTANT_METRIC, new SimilarityBands()),
                new AliasAwareFieldComparator(EMPTY_REPOSITORY, new ExactFieldComparator<String>()),
                new TokenSubsumptionComparator(new DefaultTokenSplitter()));
    }

    @ParameterizedTest
    @MethodSource("comparators")
    void doesNotThrowOnBothNull(FieldComparator<String> comparator) {
        assertThatCode(() -> comparator.compare(null, null)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("comparators")
    void doesNotThrowOnLeftNull(FieldComparator<String> comparator) {
        assertThatCode(() -> comparator.compare(null, "value")).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("comparators")
    void doesNotThrowOnRightNull(FieldComparator<String> comparator) {
        assertThatCode(() -> comparator.compare("value", null)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("comparators")
    void bothNullYieldsMissingBoth(FieldComparator<String> comparator) {
        FieldEvidence evidence = comparator.compare(null, null);

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.MISSING_BOTH);
    }

    @ParameterizedTest
    @MethodSource("comparators")
    void leftNullYieldsMissingOne(FieldComparator<String> comparator) {
        FieldEvidence evidence = comparator.compare(null, "value");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.MISSING_ONE);
    }

    @ParameterizedTest
    @MethodSource("comparators")
    void rightNullYieldsMissingOne(FieldComparator<String> comparator) {
        FieldEvidence evidence = comparator.compare("value", null);

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.MISSING_ONE);
    }

    @Test
    void comparatorListCoversEveryConcreteComparatorInThisPackage() {
        // The list is manual because these comparators do not share a
        // constructor signature, so reflection could not instantiate them
        // generically.
        //
        // Be clear about what that costs, because the previous comment here
        // had it backwards. Reflection would pick up a new comparator on its
        // own; a manual list is precisely the thing that can silently stop
        // covering one. This assertion does not read the package — it is a
        // tripwire on the list, and it fails only once someone edits the
        // list, prompting them to confirm the null contract holds for what
        // they added. A comparator added to this package and never added
        // here is covered by nothing, and nothing here will say so.
        //
        // So: whoever adds a comparator to this package adds it above and
        // updates this count.
        assertThat(comparators()).hasSize(4);
    }
}
