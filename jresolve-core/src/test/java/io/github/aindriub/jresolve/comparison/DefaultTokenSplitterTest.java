package io.github.aindriub.jresolve.comparison;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultTokenSplitterTest {

    private final DefaultTokenSplitter splitter = new DefaultTokenSplitter();

    @Test
    void nullYieldsNoTokens() {
        assertThat(splitter.split(null)).isEmpty();
    }

    @Test
    void emptyYieldsNoTokens() {
        assertThat(splitter.split("")).isEmpty();
    }

    @Test
    void whitespaceOnlyYieldsNoTokens() {
        assertThat(splitter.split("   \t\n ")).isEmpty();
    }

    @Test
    void punctuationOnlyYieldsNoTokens() {
        assertThat(splitter.split("--,.-")).isEmpty();
    }

    @Test
    void splitsOnWhitespace() {
        assertThat(splitter.split("alpha bravo charlie"))
                .containsExactly("alpha", "bravo", "charlie");
    }

    @Test
    void preservesInputOrder() {
        assertThat(splitter.split("charlie alpha bravo"))
                .containsExactly("charlie", "alpha", "bravo");
    }

    @Test
    void separatorKindDoesNotChangeTheTokens() {
        // The whole point of splitting on punctuation as well as whitespace:
        // one value tokenises the same however its parts were joined.
        List<String> spaced = splitter.split("alpha bravo");
        List<String> hyphenated = splitter.split("alpha-bravo");
        List<String> commaSeparated = splitter.split("alpha,bravo");

        assertThat(spaced).isEqualTo(hyphenated).isEqualTo(commaSeparated);
        assertThat(spaced).containsExactly("alpha", "bravo");
    }

    @Test
    void repeatedSeparatorsYieldNoEmptyToken() {
        assertThat(splitter.split("alpha  ,,--  bravo")).containsExactly("alpha", "bravo");
    }

    @Test
    void leadingAndTrailingSeparatorsYieldNoEmptyToken() {
        assertThat(splitter.split("  ,alpha bravo,.  ")).containsExactly("alpha", "bravo");
    }

    @Test
    void noTokenIsEverEmpty() {
        assertThat(splitter.split(" -- alpha ,, bravo .. ")).doesNotContain("");
    }

    @Test
    void digitsAreTokenCharacters() {
        assertThat(splitter.split("alpha 42 bravo7")).containsExactly("alpha", "42", "bravo7");
    }

    @Test
    void diacriticBearingLettersStayInsideTokens() {
        // A letter is whatever the JDK considers one, so normalization's
        // output is not re-split by this class.
        assertThat(splitter.split("ábc déf")).containsExactly("ábc", "déf");
    }

    @Test
    void supplementaryCharactersAreNotSplitMidPair() {
        // U+1D400 MATHEMATICAL BOLD CAPITAL A is a letter outside the BMP and
        // occupies two chars. Iterating by char rather than by code point
        // would break the surrogate pair.
        String supplementary = new String(Character.toChars(0x1D400));

        assertThat(splitter.split(supplementary + " alpha"))
                .containsExactly(supplementary, "alpha");
    }

    @Test
    void theReturnedListIsUnmodifiable() {
        List<String> tokens = splitter.split("alpha bravo");

        assertThatThrownBy(() -> tokens.add("charlie"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void theEmptyResultIsAlsoUnmodifiable() {
        List<String> tokens = splitter.split(null);

        assertThatThrownBy(() -> tokens.add("alpha"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void isDeterministic() {
        assertThat(splitter.split("alpha bravo charlie"))
                .isEqualTo(splitter.split("alpha bravo charlie"));
    }
}
