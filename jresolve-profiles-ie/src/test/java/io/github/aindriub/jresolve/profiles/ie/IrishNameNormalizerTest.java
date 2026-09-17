package io.github.aindriub.jresolve.profiles.ie;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IrishNameNormalizerTest {

    private final IrishNameNormalizer normalizer = new IrishNameNormalizer();

    @Test
    void nullNormalizesToNull() {
        assertThat(normalizer.normalize(null)).isNull();
    }

    @Test
    void aFadaIsFolded() {
        assertThat(normalizer.normalize("Seán")).isEqualTo("sean");
    }

    @Test
    void caseIsFolded() {
        assertThat(normalizer.normalize("SEÁN")).isEqualTo("sean");
    }

    @Test
    void anApostropheIsRemovedWhateverItsCharacter() {
        assertThat(normalizer.normalize("O'Sullivan")).isEqualTo("osullivan");
        assertThat(normalizer.normalize("O’Sullivan")).isEqualTo("osullivan");
    }

    @Test
    void whitespaceIsCollapsed() {
        assertThat(normalizer.normalize("  Ó   Súilleabháin  ")).isEqualTo("o suilleabhain");
    }

    @Test
    void isIdempotent() {
        String once = normalizer.normalize("Ó Súilleabháin");

        assertThat(normalizer.normalize(once)).isEqualTo(once);
    }

    @Test
    void twoSpellingsOfOneNameConverge() {
        assertThat(normalizer.normalize("Seán")).isEqualTo(normalizer.normalize("Sean"));
    }

    @Test
    void theIrishAndEnglishFormsOfASurnameDoNotConverge() {
        // The point of D8, asserted rather than assumed. No normalization rule
        // brings these together without also merging names that are genuinely
        // different, so the pair belongs to the alias layer instead. A future
        // change that makes this test fail has almost certainly made the
        // normalizer too aggressive rather than fixed anything.
        assertThat(normalizer.normalize("Ó Súilleabháin"))
                .isNotEqualTo(normalizer.normalize("O'Sullivan"));
    }

    @Test
    void twoDifferentNamesStayDifferent() {
        assertThat(normalizer.normalize("Seán")).isNotEqualTo(normalizer.normalize("John"));
    }
}
