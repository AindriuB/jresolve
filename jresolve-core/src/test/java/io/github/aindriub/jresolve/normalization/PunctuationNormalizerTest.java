package io.github.aindriub.jresolve.normalization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PunctuationNormalizerTest {

    @Test
    void returnsNullForNullInput() {
        StringNormalizer normalizer = new PunctuationNormalizer(hyphen());

        assertThat(normalizer.normalize(null)).isNull();
    }

    @Test
    void removesOnlyTheConfiguredCharacters() {
        StringNormalizer normalizer = new PunctuationNormalizer(hyphen());

        assertThat(normalizer.normalize("hyphen-ated, punctuated!"))
                .isEqualTo("hyphenated, punctuated!");
    }

    @Test
    void leavesUnconfiguredPunctuationInPlace() {
        Set<Character> exclamationOnly = new HashSet<>();
        exclamationOnly.add('!');
        StringNormalizer normalizer = new PunctuationNormalizer(exclamationOnly);

        assertThat(normalizer.normalize("hyphen-ated!")).isEqualTo("hyphen-ated");
    }

    @Test
    void anEmptyCharacterSetRemovesNothing() {
        StringNormalizer normalizer = new PunctuationNormalizer(Collections.emptySet());

        assertThat(normalizer.normalize("hyphen-ated!")).isEqualTo("hyphen-ated!");
    }

    @Test
    void rejectsANullCharacterSet() {
        assertThatThrownBy(() -> new PunctuationNormalizer(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doesNotMutateTheInputReference() {
        StringNormalizer normalizer = new PunctuationNormalizer(hyphen());
        String input = "hyphen-ated";

        normalizer.normalize(input);

        assertThat(input).isEqualTo("hyphen-ated");
    }

    private static Set<Character> hyphen() {
        Set<Character> characters = new HashSet<>();
        characters.add('-');
        return characters;
    }
}
