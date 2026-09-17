package io.github.aindriub.jresolve.profiles.ie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.FieldEvidence;
import io.github.aindriub.jresolve.evidence.TokenSubsumption;
import io.github.aindriub.jresolve.field.FieldPipeline;
import io.github.aindriub.jresolve.field.TokenSubsumptionComparator;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * The assembled pipelines, exercised end to end through {@code prepare} and
 * {@code compare} the way a resolver drives them.
 */
class IrishPipelineTest {

    private final FieldPipeline<String, String> address = IrishAddressPipeline.forSingleLine();
    private final FieldPipeline<java.util.List<String>, String> addressLines =
            IrishAddressPipeline.forLines();
    private final FieldPipeline<String, String> givenName = IrishNamePipeline.forGivenName();

    private static <T> FieldEvidence through(FieldPipeline<T, String> pipeline, T left, T right) {
        return pipeline.compare(pipeline.prepare(left), pipeline.prepare(right));
    }

    // ------------------------------------------------------ the name pipeline

    @Test
    void anIrishAndEnglishGivenNameAgreeAsATranslation() {
        // The single result this module exists for. Without the alias stage
        // these two score near zero on any string metric and count as evidence
        // against a match.
        assertThat(through(givenName, "Seán", "John").getCategory())
                .isSameAs(ComparisonCategory.ALIAS_TRANSLATION);
    }

    @Test
    void aSpellingDifferenceIsClosedByNormalizationNotByTheTable() {
        assertThat(through(givenName, "Seán", "Sean").getCategory())
                .isSameAs(ComparisonCategory.EXACT);
    }

    @Test
    void twoUnrelatedNamesFallThroughToSimilarity() {
        FieldEvidence evidence = through(givenName, "Mary", "Kevin");

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.LOW);
        assertThat(evidence.getSimilarity()).isNotNull();
    }

    @Test
    void aCallerSuppliedRepositoryIsUsedInsteadOfTheIllustrativeOne() {
        // The path a consumer takes once it has a sourced corpus.
        FieldPipeline<String, String> empty = IrishNamePipeline.withAliases(
                IrishNameAliases.repository());

        assertThat(through(empty, "Seán", "John").getCategory())
                .isSameAs(ComparisonCategory.ALIAS_TRANSLATION);
    }

    @Test
    void theNamePipelineRejectsANullRepository() {
        assertThatThrownBy(() -> IrishNamePipeline.withAliases(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theNamePipelinePrepareIsIdempotent() {
        String once = givenName.prepare("Ó Súilleabháin");

        assertThat(givenName.prepare(once)).isEqualTo(once);
    }

    @Test
    void theNamePipelinePreparesNullToNull() {
        assertThat(givenName.prepare(null)).isNull();
    }

    // --------------------------------------------------- the address pipeline

    @Test
    void aLessSpecificAddressIsSubsumed() {
        FieldEvidence evidence = through(address, "Dublin", "Dublin 4");

        assertThat(evidence.getCategory()).isSameAs(TokenSubsumptionComparator.SUBSUMED);
        assertThat(evidence.getSubsumption()).isSameAs(TokenSubsumption.LEFT_SUBSUMES_RIGHT);
    }

    @Test
    void theAddressPipelinePrepareIsIdempotent() {
        String once = address.prepare("12 Main Street,  Dublin 4");

        assertThat(address.prepare(once)).isEqualTo(once);
    }

    @Test
    void theAddressPipelinePreparesNullToNull() {
        assertThat(address.prepare(null)).isNull();
    }

    // ------------------------------------------------- the two address shapes

    @Test
    void separateLinesPrepareToTheSameValueAsOneLine() {
        // D1's asymmetric case: one side carries lines, the other a single
        // string, and both converge on one normalized form. Without that a
        // resolver could not compare across the two systems at all.
        assertThat(addressLines.prepare(Arrays.asList("12 Main Street", "Dublin 4")))
                .isEqualTo(address.prepare("12 Main Street, Dublin 4"));
    }

    @Test
    void aNullLineListPreparesToNull() {
        assertThat(addressLines.prepare(null)).isNull();
    }

    @Test
    void aListOfOnlyNullsPreparesToNull() {
        assertThat(addressLines.prepare(Arrays.asList(null, null))).isNull();
    }

    @Test
    void anEmptyListPreparesToNull() {
        assertThat(addressLines.prepare(Collections.<String>emptyList())).isNull();
    }

    @Test
    void aNullLineInsideAListIsSkippedRatherThanRendered() {
        assertThat(addressLines.prepare(Arrays.asList("12 Main Street", null, "Dublin 4")))
                .isEqualTo(address.prepare("12 Main Street Dublin 4"));
    }

    @Test
    void aMissingAddressOnOneSideIsMissingOne() {
        assertThat(through(addressLines, null, Arrays.asList("Dublin 4")).getCategory())
                .isSameAs(ComparisonCategory.MISSING_ONE);
    }
}
