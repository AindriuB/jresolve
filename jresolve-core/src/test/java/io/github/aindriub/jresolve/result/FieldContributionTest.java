package io.github.aindriub.jresolve.result;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.TokenSubsumption;
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

    /**
     * A tripwire on this type's public shape, defending D10.
     *
     * <p>The list below is not arbitrary and adding to it is not a
     * formality. The criterion a member must meet is that <strong>its type
     * cannot carry a compared value</strong>: a field <em>name</em>, a
     * number, a template key, or a closed set of structural constants like
     * {@code ComparisonCategory} and {@code TokenSubsumption}. A prepared
     * value, a frequency key, or anything derived from the data being
     * compared fails that test and must not be exposed here, however useful
     * it would be — this object ends up in whatever a consumer logs.
     *
     * <p>So whoever adds an accessor judges it against that criterion first
     * and records the judgement, rather than appending a name to make a red
     * test go green.
     */
    @Test
    void exposesNoValueBearingAccessor() {
        Set<String> allowedAccessors = new HashSet<>();
        allowedAccessors.add("getField");
        allowedAccessors.add("getCategory");
        allowedAccessors.add("getContribution");
        allowedAccessors.add("getTemplateKey");
        // Added by task 18. TokenSubsumption is a five-constant enum
        // describing a relation between two token sets; it cannot hold a
        // compared value, which is why it clears the criterion above where
        // exposing the evidence object itself would not.
        allowedAccessors.add("getSubsumption");
        allowedAccessors.add("toString");
        allowedAccessors.add("equals");
        allowedAccessors.add("hashCode");

        for (Method method : FieldContribution.class.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            assertThat(allowedAccessors)
                    .as("unexpected public method %s exposes a shape beyond field/category/"
                            + "contribution/templateKey/subsumption — judge it against the "
                            + "criterion in this test's Javadoc before adding it",
                            method.getName())
                    .contains(method.getName());
        }
    }

    @Test
    void theFourArgumentConstructorDefaultsToNotApplicable() {
        FieldContribution contribution =
                new FieldContribution("field", ComparisonCategory.EXACT, 1.0, "key");

        assertThat(contribution.getSubsumption()).isSameAs(TokenSubsumption.NOT_APPLICABLE);
    }

    @Test
    void theFiveArgumentConstructorCarriesTheSubsumption() {
        FieldContribution contribution = new FieldContribution(
                "field", ComparisonCategory.EXACT, 1.0, "key", TokenSubsumption.LEFT_SUBSUMES_RIGHT);

        assertThat(contribution.getSubsumption()).isSameAs(TokenSubsumption.LEFT_SUBSUMES_RIGHT);
    }

    @Test
    void rejectsNullSubsumption() {
        assertThatThrownBy(() -> new FieldContribution(
                "field", ComparisonCategory.EXACT, 1.0, "key", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subsumption");
    }

    @Test
    void toStringIncludesTheSubsumption() {
        FieldContribution contribution = new FieldContribution(
                "field", ComparisonCategory.EXACT, 1.0, "key", TokenSubsumption.EQUIVALENT);

        assertThat(contribution.toString()).contains("EQUIVALENT");
    }

    @Test
    void toStringCarriesNoComparedValue() {
        // D10's property, re-asserted because adding a member to this type is
        // exactly when it gets lost. Only a field name, a category, a number
        // and a template key may appear.
        FieldContribution contribution = new FieldContribution(
                "field", ComparisonCategory.EXACT, 1.0, "key", TokenSubsumption.EQUIVALENT);

        assertThat(contribution.toString())
                .doesNotContain("widget-7")
                .contains("field", "EXACT", "key");
    }
}
