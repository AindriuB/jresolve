package io.github.aindriub.jresolve.normalization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CaseFoldNormalizerTest {

    private final StringNormalizer normalizer = new CaseFoldNormalizer();

    @Test
    void returnsNullForNullInput() {
        assertThat(normalizer.normalize(null)).isNull();
    }

    @Test
    void lowercasesUsingTheRootLocale() {
        assertThat(normalizer.normalize("MixedCASE")).isEqualTo("mixedcase");
    }

    @Test
    void leavesAlreadyLowercaseContentUnchanged() {
        assertThat(normalizer.normalize("already lowercase"))
                .isEqualTo("already lowercase");
    }

    @Test
    void doesNotMutateTheInputReference() {
        String input = "MixedCASE";

        normalizer.normalize(input);

        assertThat(input).isEqualTo("MixedCASE");
    }
}
