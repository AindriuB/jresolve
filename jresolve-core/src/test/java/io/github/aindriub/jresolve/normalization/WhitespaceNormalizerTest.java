package io.github.aindriub.jresolve.normalization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WhitespaceNormalizerTest {

    private final StringNormalizer normalizer = new WhitespaceNormalizer();

    @Test
    void returnsNullForNullInput() {
        assertThat(normalizer.normalize(null)).isNull();
    }

    @Test
    void trimsLeadingAndTrailingWhitespace() {
        assertThat(normalizer.normalize("  padded  ")).isEqualTo("padded");
    }

    @Test
    void collapsesARunOfPlainSpacesToOne() {
        assertThat(normalizer.normalize("a   b")).isEqualTo("a b");
    }

    @Test
    void collapsesATabToASingleSpace() {
        assertThat(normalizer.normalize("a\tb")).isEqualTo("a b");
    }

    @Test
    void collapsesANonBreakingSpaceToASingleSpace() {
        String withNonBreakingSpace = "a" + '\u00A0' + "b";

        assertThat(normalizer.normalize(withNonBreakingSpace)).isEqualTo("a b");
    }

    @Test
    void leavesAlreadyNormalizedWhitespaceUnchanged() {
        assertThat(normalizer.normalize("a b c")).isEqualTo("a b c");
    }

    @Test
    void reducesAnAllWhitespaceStringToEmpty() {
        assertThat(normalizer.normalize("   \t  ")).isEqualTo("");
    }

    @Test
    void doesNotMutateTheInputReference() {
        String input = "  padded  ";

        normalizer.normalize(input);

        assertThat(input).isEqualTo("  padded  ");
    }
}
