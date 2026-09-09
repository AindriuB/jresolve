package io.github.aindriub.jresolve.field;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.FieldEvidence;
import org.junit.jupiter.api.Test;

class ExactFieldComparatorTest {

    private final ExactFieldComparator<String> comparator = new ExactFieldComparator<>();

    @Test
    void equalValuesAreExact() {
        FieldEvidence evidence = comparator.compare("widget-7", "widget-7");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.EXACT);
    }

    @Test
    void exactSetsFrequencyKeyToTheAgreedValue() {
        FieldEvidence evidence = comparator.compare("widget-7", "widget-7");

        assertThat(evidence.getFrequencyKey()).isEqualTo("widget-7");
    }

    @Test
    void differentValuesAreConflict() {
        FieldEvidence evidence = comparator.compare("widget-7", "widget-8");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.CONFLICT);
    }

    @Test
    void conflictFrequencyKeyIsNull() {
        FieldEvidence evidence = comparator.compare("widget-7", "widget-8");

        assertThat(evidence.getFrequencyKey()).isNull();
    }

    @Test
    void bothNullIsMissingBoth() {
        FieldEvidence evidence = comparator.compare(null, null);

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.MISSING_BOTH);
    }

    @Test
    void leftNullIsMissingOne() {
        FieldEvidence evidence = comparator.compare(null, "widget-7");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.MISSING_ONE);
    }

    @Test
    void rightNullIsMissingOne() {
        FieldEvidence evidence = comparator.compare("widget-7", null);

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.MISSING_ONE);
    }
}
