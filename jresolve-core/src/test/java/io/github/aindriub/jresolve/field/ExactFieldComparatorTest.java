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

    /**
     * A type with value equality whose {@code toString()} is identity-based —
     * the shape the class Javadoc warns about.
     *
     * <p>The identity rendering is written out rather than inherited. An
     * inherited {@code Object.toString()} derives from {@code hashCode()},
     * which this class overrides for the {@code equals} contract, so equal
     * instances would render identically and the defect would not reproduce.
     * That is a property of this test's fixture, not a reason to think the
     * requirement is unnecessary: a real type that renders any per-instance
     * state — a creation timestamp, a source row number — breaks the key the
     * same way.
     */
    private static final class ValueEqualOnly {
        private final String value;

        ValueEqualOnly(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ValueEqualOnly && value.equals(((ValueEqualOnly) other).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public String toString() {
            return "ValueEqualOnly@" + System.identityHashCode(this);
        }
    }

    @Test
    void anInconsistentToStringBreaksTheFrequencyKey() {
        ExactFieldComparator<ValueEqualOnly> comparator = new ExactFieldComparator<>();

        FieldEvidence first = comparator.compare(new ValueEqualOnly("a"), new ValueEqualOnly("a"));
        FieldEvidence second = comparator.compare(new ValueEqualOnly("a"), new ValueEqualOnly("a"));

        // Both pairs agreed on the same value, so a frequency table must see
        // one key twice. With a default toString it sees two keys once each,
        // and every value looks rare. This is why the class Javadoc states a
        // toString-consistent-with-equals requirement on N: the library
        // cannot detect it, so it is documented rather than checked, and this
        // test pins the failure so the requirement is not quietly dropped.
        assertThat(first.getCategory()).isSameAs(ComparisonCategory.EXACT);
        assertThat(second.getCategory()).isSameAs(ComparisonCategory.EXACT);
        assertThat(first.getFrequencyKey()).isNotEqualTo(second.getFrequencyKey());
    }

    @Test
    void aConsistentToStringSharesTheFrequencyKey() {
        FieldEvidence first = comparator.compare("widget-7", "widget-7");
        FieldEvidence second = comparator.compare("widget-7", "widget-7");

        assertThat(first.getFrequencyKey()).isEqualTo(second.getFrequencyKey());
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
