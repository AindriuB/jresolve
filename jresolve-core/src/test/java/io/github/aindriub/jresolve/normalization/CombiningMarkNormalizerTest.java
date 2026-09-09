package io.github.aindriub.jresolve.normalization;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;
import org.junit.jupiter.api.Test;

class CombiningMarkNormalizerTest {

    private final StringNormalizer normalizer = new CombiningMarkNormalizer();

    @Test
    void returnsNullForNullInput() {
        assertThat(normalizer.normalize(null)).isNull();
    }

    @Test
    void removesACombiningMarkLeftByDecomposition() {
        String decomposed = Normalizer.normalize("café", Normalizer.Form.NFD);

        assertThat(normalizer.normalize(decomposed)).isEqualTo("cafe");
    }

    @Test
    void leavesAPrecomposedCharacterUntouched() {
        assertThat(normalizer.normalize("café")).isEqualTo("café");
    }

    @Test
    void leavesAStringWithoutCombiningMarksUnchanged() {
        assertThat(normalizer.normalize("plain text")).isEqualTo("plain text");
    }

    @Test
    void doesNotMutateTheInputReference() {
        String input = Normalizer.normalize("café", Normalizer.Form.NFD);
        String copy = new String(input.toCharArray());

        normalizer.normalize(input);

        assertThat(input).isEqualTo(copy);
    }
}
