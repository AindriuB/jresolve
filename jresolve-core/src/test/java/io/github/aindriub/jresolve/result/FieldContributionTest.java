package io.github.aindriub.jresolve.result;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldContributionTest {

    @Test
    void exposesFieldCategoryContributionAndTemplateKey() {
        FieldContribution contribution =
                new FieldContribution("someField", ComparisonCategory.HIGH, 4.1, "template.someField.highSimilarity");

        assertThat(contribution.getField()).isEqualTo("someField");
        assertThat(contribution.getCategory()).isSameAs(ComparisonCategory.HIGH);
        assertThat(contribution.getContribution()).isEqualTo(4.1);
        assertThat(contribution.getTemplateKey()).isEqualTo("template.someField.highSimilarity");
    }

    @Test
    void rejectsNullField() {
        assertThatThrownBy(() -> new FieldContribution(null, ComparisonCategory.HIGH, 1.0, "key"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullCategory() {
        assertThatThrownBy(() -> new FieldContribution("someField", null, 1.0, "key"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exposesNoValueBearingAccessor() {
        Set<String> allowedAccessors = new HashSet<>();
        allowedAccessors.add("getField");
        allowedAccessors.add("getCategory");
        allowedAccessors.add("getContribution");
        allowedAccessors.add("getTemplateKey");
        allowedAccessors.add("toString");
        allowedAccessors.add("equals");
        allowedAccessors.add("hashCode");

        for (Method method : FieldContribution.class.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            assertThat(allowedAccessors)
                    .as("unexpected public method %s exposes a shape beyond field/category/contribution/templateKey",
                            method.getName())
                    .contains(method.getName());
        }
    }
}
