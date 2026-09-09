package io.github.aindriub.jresolve.normalization;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;
import org.junit.jupiter.api.Test;

class UnicodeFormNormalizerTest {

    @Test
    void returnsNullForNullInput() {
        assertThat(new UnicodeFormNormalizer().normalize(null)).isNull();
    }

    @Test
    void defaultsToNfd() {
        StringNormalizer normalizer = new UnicodeFormNormalizer();

        String result = normalizer.normalize("café");

        assertThat(Normalizer.isNormalized(result, Normalizer.Form.NFD)).isTrue();
    }

    @Test
    void nfdDecomposesAPrecomposedCharacterIntoBaseAndCombiningMark() {
        StringNormalizer normalizer = new UnicodeFormNormalizer(Normalizer.Form.NFD);

        String result = normalizer.normalize("é");

        assertThat(result).hasSize(2);
        assertThat(result.charAt(0)).isEqualTo('e');
    }

    @Test
    void nfcRecomposesADecomposedCharacter() {
        StringNormalizer normalizer = new UnicodeFormNormalizer(Normalizer.Form.NFC);
        String decomposed = "é";

        String result = normalizer.normalize(decomposed);

        assertThat(result).isEqualTo("é");
    }

    @Test
    void doesNotMutateTheInputReference() {
        StringNormalizer normalizer = new UnicodeFormNormalizer();
        String input = "café";

        normalizer.normalize(input);

        assertThat(input).isEqualTo("café");
    }

    @Test
    void rejectsANullForm() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new UnicodeFormNormalizer(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
