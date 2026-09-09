package io.github.aindriub.jresolve.field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.FieldEvidence;
import io.github.aindriub.jresolve.normalization.CaseFoldNormalizer;
import io.github.aindriub.jresolve.normalization.CompositeNormalizer;
import io.github.aindriub.jresolve.normalization.StringNormalizer;
import io.github.aindriub.jresolve.normalization.WhitespaceNormalizer;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultFieldPipelineTest {

    @Test
    void composesANormalizerAndComparator() {
        DefaultFieldPipeline<String, String> pipeline =
                new DefaultFieldPipeline<>(String::trim, new ExactFieldComparator<>());

        FieldEvidence evidence = pipeline.compare(pipeline.prepare("  x  "), pipeline.prepare("x"));

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.EXACT);
    }

    @Test
    void prepareIsIdempotentForACompositeNormalizerOverString() {
        List<StringNormalizer> stages = Arrays.asList(new CaseFoldNormalizer(), new WhitespaceNormalizer());
        CompositeNormalizer composite = new CompositeNormalizer(stages);
        DefaultFieldPipeline<String, String> pipeline =
                new DefaultFieldPipeline<>(composite::normalize, new ExactFieldComparator<>());

        String once = pipeline.prepare("  ACME  Corp  ");
        String twice = pipeline.prepare(once);

        // "  ACME  Corp  " case-folds to "  acme  corp  " then collapses
        // whitespace to "acme corp"; re-running the same two stages on
        // "acme corp" changes nothing, so the fixed point is reached in one pass.
        assertThat(twice).isEqualTo(once);
    }

    @Test
    void rejectsNullNormalizer() {
        assertThatThrownBy(() -> new DefaultFieldPipeline<String, String>(null, new ExactFieldComparator<>()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullComparator() {
        assertThatThrownBy(() -> new DefaultFieldPipeline<String, String>(v -> v, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
