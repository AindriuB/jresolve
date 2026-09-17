package io.github.aindriub.jresolve.profiles.ie;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IrishAddressNormalizerTest {

    private final IrishAddressNormalizer normalizer = new IrishAddressNormalizer();

    @Test
    void nullNormalizesToNull() {
        assertThat(normalizer.normalize(null)).isNull();
    }

    @Test
    void caseIsFolded() {
        assertThat(normalizer.normalize("12 MAIN STREET")).isEqualTo("12 main street");
    }

    @Test
    void aFadaIsFolded() {
        assertThat(normalizer.normalize("Dún Laoghaire")).isEqualTo("dun laoghaire");
    }

    @Test
    void whitespaceIsCollapsed() {
        assertThat(normalizer.normalize("  12   Main  Street ")).isEqualTo("12 main street");
    }

    @Test
    void isIdempotent() {
        String once = normalizer.normalize("  12 Main Street,  Dún Laoghaire ");

        assertThat(normalizer.normalize(once)).isEqualTo(once);
    }

    @Test
    void punctuationBecomesATokenSeparator() {
        // Deliberately unlike the name normalizer, which removes apostrophes
        // so a name folds to one token. An address is many tokens and its
        // punctuation separates them.
        assertThat(normalizer.normalize("12 Main Street, Dublin 4"))
                .isEqualTo("12 main street dublin 4");
    }

    @Test
    void twoAddressesDifferingOnlyInPunctuationPrepareIdentically() {
        // Not merely a comparison property. A prepared value is what a
        // blocking key is derived from and the form a cached candidate is held
        // in, so two records that must compare equal must not prepare to two
        // different strings.
        assertThat(normalizer.normalize("12 Main Street, Dublin 4"))
                .isEqualTo(normalizer.normalize("12 Main Street Dublin 4"));
    }

    @Test
    void punctuationSeparatesRatherThanDisappears() {
        // Removing punctuation instead of replacing it would run two tokens
        // together whenever a separator carried no adjacent space.
        assertThat(normalizer.normalize("Dublin,4")).isEqualTo("dublin 4");
    }
}
