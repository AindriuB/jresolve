package io.github.aindriub.jresolve.field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FieldDefinitionTest {

    private static final FieldComparator<String> COMPARATOR = new ExactFieldComparator<>();

    @Test
    void exposesNamePreparersComparatorCostAndRequired() {
        FieldDefinition<String, String, String> definition = new FieldDefinition<>(
                "widgetCode", s -> s, c -> c, COMPARATOR, CostTiers.CHEAP, true);

        assertThat(definition.getName()).isEqualTo("widgetCode");
        assertThat(definition.getSourcePreparer().apply("x")).isEqualTo("x");
        assertThat(definition.getCandidatePreparer().apply("y")).isEqualTo("y");
        assertThat(definition.getComparator()).isSameAs(COMPARATOR);
        assertThat(definition.getCost()).isEqualTo(CostTiers.CHEAP);
        assertThat(definition.isRequired()).isTrue();
    }

    @Test
    void rejectsNullName() {
        assertThatThrownBy(() -> new FieldDefinition<String, String, String>(
                null, s -> s, c -> c, COMPARATOR, CostTiers.CHEAP, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullSourcePreparer() {
        assertThatThrownBy(() -> new FieldDefinition<String, String, String>(
                "widgetCode", null, c -> c, COMPARATOR, CostTiers.CHEAP, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullCandidatePreparer() {
        assertThatThrownBy(() -> new FieldDefinition<String, String, String>(
                "widgetCode", s -> s, null, COMPARATOR, CostTiers.CHEAP, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullComparator() {
        assertThatThrownBy(() -> new FieldDefinition<String, String, String>(
                "widgetCode", s -> s, c -> c, null, CostTiers.CHEAP, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
