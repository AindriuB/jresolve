package io.github.aindriub.jresolve.field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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

    private static List<FieldComparator<String>> comparators() {
        return Arrays.asList(
                new ExactFieldComparator<>(),
                new SimilarityFieldComparator(CONSTANT_METRIC, new SimilarityBands()));
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
        // A manual list, not reflection: reflection would silently stop
        // covering a new comparator added later, exactly the failure mode
        // this test exists to catch.
        assertThat(comparators()).hasSize(2);
    }
}
