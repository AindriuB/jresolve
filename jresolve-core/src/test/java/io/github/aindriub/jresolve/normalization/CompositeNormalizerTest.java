package io.github.aindriub.jresolve.normalization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompositeNormalizerTest {

    @Test
    void returnsNullImmediatelyForANullInput() {
        CompositeNormalizer composite = new CompositeNormalizer(
                Collections.singletonList((StringNormalizer) value -> {
                    throw new AssertionError("stage must not run on a null input");
                }));

        assertThat(composite.normalize(null)).isNull();
    }

    @Test
    void appliesStagesInOrder() {
        List<StringNormalizer> pipeline = Arrays.asList(
                new UnicodeFormNormalizer(),
                new CaseFoldNormalizer(),
                new CombiningMarkNormalizer(),
                new ApostropheVariantNormalizer(),
                new WhitespaceNormalizer());
        CompositeNormalizer composite = new CompositeNormalizer(pipeline);

        assertThat(composite.normalize("  Seán O’Brien  ")).isEqualTo("sean o'brien");
    }

    @Test
    void anEmptyPipelineReturnsTheInputUnchanged() {
        CompositeNormalizer composite = new CompositeNormalizer(Collections.emptyList());

        assertThat(composite.normalize("unchanged")).isEqualTo("unchanged");
    }

    @Test
    void copiesTheStageListDefensively() {
        List<StringNormalizer> mutableStages = new ArrayList<>();
        mutableStages.add(new CaseFoldNormalizer());
        CompositeNormalizer composite = new CompositeNormalizer(mutableStages);

        mutableStages.add(new WhitespaceNormalizer());

        assertThat(composite.getStages()).hasSize(1);
    }

    @Test
    void exposesAnUnmodifiableStageList() {
        CompositeNormalizer composite = new CompositeNormalizer(
                Collections.singletonList(new CaseFoldNormalizer()));

        assertThatThrownBy(() -> composite.getStages().add(new WhitespaceNormalizer()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsANullStageList() {
        assertThatThrownBy(() -> new CompositeNormalizer(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAStageListContainingNull() {
        List<StringNormalizer> containsNull = new ArrayList<>();
        containsNull.add(null);

        assertThatThrownBy(() -> new CompositeNormalizer(containsNull))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
