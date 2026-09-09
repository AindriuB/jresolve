package io.github.aindriub.jresolve.evidence;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchEvidenceTest {

    @Test
    void preservesInsertionOrderInGetFields() {
        Map<String, FieldEvidence> input = new LinkedHashMap<>();
        input.put("second", new DefaultFieldEvidence(ComparisonCategory.HIGH, 0.9, null));
        input.put("first", new DefaultFieldEvidence(ComparisonCategory.EXACT, 1.0, null));

        MatchEvidence evidence = new MatchEvidence(input, true);

        assertThat(evidence.getFields().keySet()).containsExactly("second", "first");
    }

    @Test
    void getFieldReturnsNullForAbsentField() {
        MatchEvidence evidence = new MatchEvidence(new LinkedHashMap<>(), true);

        assertThat(evidence.getField("nonexistent")).isNull();
    }

    @Test
    void copiesInputMapDefensively() {
        Map<String, FieldEvidence> input = new LinkedHashMap<>();
        input.put("one", new DefaultFieldEvidence(ComparisonCategory.EXACT, 1.0, null));

        MatchEvidence evidence = new MatchEvidence(input, true);
        input.put("two", new DefaultFieldEvidence(ComparisonCategory.HIGH, 0.9, null));

        assertThat(evidence.getFields()).hasSize(1);
    }

    @Test
    void getFieldsIsUnmodifiable() {
        MatchEvidence evidence = new MatchEvidence(new LinkedHashMap<>(), true);

        assertThatThrownBy(() -> evidence.getFields().put("x", null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void isCompleteIsSetAtConstruction() {
        MatchEvidence complete = new MatchEvidence(new LinkedHashMap<>(), true);
        MatchEvidence incomplete = new MatchEvidence(new LinkedHashMap<>(), false);

        assertThat(complete.isComplete()).isTrue();
        assertThat(incomplete.isComplete()).isFalse();
    }

    @Test
    void toStringDoesNotContainFrequencyKey() {
        Map<String, FieldEvidence> input = new LinkedHashMap<>();
        input.put("field", new DefaultFieldEvidence(ComparisonCategory.EXACT, 1.0, "sensitive-token"));

        MatchEvidence evidence = new MatchEvidence(input, true);

        assertThat(evidence.toString()).doesNotContain("sensitive-token");
    }

    @Test
    void rejectsNullFieldsMap() {
        assertThatThrownBy(() -> new MatchEvidence(null, true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
