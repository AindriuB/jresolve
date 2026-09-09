package io.github.aindriub.jresolve.normalization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApostropheVariantNormalizerTest {

    private final StringNormalizer normalizer = new ApostropheVariantNormalizer();

    @Test
    void returnsNullForNullInput() {
        assertThat(normalizer.normalize(null)).isNull();
    }

    @Test
    void threeRenderingsOfTheSameApostropheBearingTokenNormalizeIdentically() {
        String plain = normalizer.normalize("O'Brien");
        String rightSingleQuote = normalizer.normalize("O’Brien");
        String modifierLetterApostrophe = normalizer.normalize("OʼBrien");

        assertThat(rightSingleQuote).isEqualTo(plain);
        assertThat(modifierLetterApostrophe).isEqualTo(plain);
    }

    @Test
    void foldsEachSupportedVariantToAPlainApostrophe() {
        assertThat(normalizer.normalize("O’Brien")).isEqualTo("O'Brien");
        assertThat(normalizer.normalize("O‘Brien")).isEqualTo("O'Brien");
        assertThat(normalizer.normalize("OʼBrien")).isEqualTo("O'Brien");
        assertThat(normalizer.normalize("O`Brien")).isEqualTo("O'Brien");
        assertThat(normalizer.normalize("O´Brien")).isEqualTo("O'Brien");
    }

    @Test
    void leavesATokenWithoutAnApostropheUnchanged() {
        assertThat(normalizer.normalize("noapostrophe")).isEqualTo("noapostrophe");
    }

    @Test
    void doesNotMutateTheInputReference() {
        String input = "O’Brien";

        normalizer.normalize(input);

        assertThat(input).isEqualTo("O’Brien");
    }
}
