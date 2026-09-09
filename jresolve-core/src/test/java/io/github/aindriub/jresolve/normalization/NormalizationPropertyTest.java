package io.github.aindriub.jresolve.normalization;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Determinism and idempotence hold for every stage, and for a composite
 * built from all of them, over the shared corpus.
 */
class NormalizationPropertyTest {

    private static Set<Character> punctuationCharacters() {
        Set<Character> characters = new HashSet<>();
        for (char c : "!@#$%".toCharArray()) {
            characters.add(c);
        }
        return characters;
    }

    static Collection<StringNormalizer> stages() {
        return Arrays.asList(
                new UnicodeFormNormalizer(),
                new UnicodeFormNormalizer(Normalizer.Form.NFC),
                new CaseFoldNormalizer(),
                new CombiningMarkNormalizer(),
                new ApostropheVariantNormalizer(),
                new PunctuationNormalizer(punctuationCharacters()),
                new WhitespaceNormalizer(),
                compositePipeline());
    }

    private static CompositeNormalizer compositePipeline() {
        List<StringNormalizer> pipeline = Arrays.asList(
                new UnicodeFormNormalizer(),
                new CaseFoldNormalizer(),
                new CombiningMarkNormalizer(),
                new ApostropheVariantNormalizer(),
                new PunctuationNormalizer(punctuationCharacters()),
                new WhitespaceNormalizer());
        return new CompositeNormalizer(pipeline);
    }

    @ParameterizedTest
    @MethodSource("stages")
    void repeatedCallsOnTheSameInputAgree(StringNormalizer stage) {
        for (String value : NormalizationCorpus.VALUES) {
            assertThat(stage.normalize(value)).isEqualTo(stage.normalize(value));
        }
    }

    @ParameterizedTest
    @MethodSource("stages")
    void applyingAStageToItsOwnOutputIsANoOp(StringNormalizer stage) {
        for (String value : NormalizationCorpus.VALUES) {
            String once = stage.normalize(value);
            String twice = stage.normalize(once);
            assertThat(twice).isEqualTo(once);
        }
    }
}
