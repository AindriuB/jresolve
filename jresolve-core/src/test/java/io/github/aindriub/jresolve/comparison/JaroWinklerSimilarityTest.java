package io.github.aindriub.jresolve.comparison;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class JaroWinklerSimilarityTest {

    private final JaroWinklerSimilarity metric = new JaroWinklerSimilarity();

    @Test
    void bothNullArgumentsAreEqual() {
        assertThat(metric.similarity(null, null)).isEqualTo(1.0);
    }

    @Test
    void oneNullArgumentAgainstNonEmptyIsZero() {
        assertThat(metric.similarity(null, "a")).isEqualTo(0.0);
    }

    @Test
    void nullArgumentDoesNotThrow() {
        assertThatCode(() -> metric.similarity(null, "x")).doesNotThrowAnyException();
    }

    @Test
    void constructorRejectsPrefixScaleBelowZero() {
        assertThatThrownBy(() -> new JaroWinklerSimilarity(-0.01, 4, 0.7))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorRejectsPrefixScaleAboveQuarter() {
        assertThatThrownBy(() -> new JaroWinklerSimilarity(0.26, 4, 0.7))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorAcceptsPrefixScaleAtBounds() {
        new JaroWinklerSimilarity(0.0, 4, 0.7);
        new JaroWinklerSimilarity(0.25, 4, 0.7);
    }

    // MARTHA vs MARHTA, both length 6, match window = max(6,6)/2 - 1 = 2.
    // Walking the window finds matches M-M, A-A, R-R, T-H, H-T, A-A: 6 matches.
    // Comparing matched characters in index order surfaces two positions
    // (T/H then H/T) where the characters differ, giving 2 raw transpositions,
    // i.e. 1 after halving.
    // jaro = (6/6 + 6/6 + (6-1)/6) / 3 = (1 + 1 + 0.833333) / 3 = 0.944444
    // shared prefix "MAR" (T != H at index 3) is 3 characters, capped at 4.
    // jw = 0.944444 + 3 * 0.1 * (1 - 0.944444) = 0.944444 + 0.016667 = 0.961111
    @Test
    void marthaAndMarhtaMatchTheHandWorkedJaroWinklerScore() {
        assertThat(metric.similarity("MARTHA", "MARHTA")).isCloseTo(0.961111, within(0.000001));
    }

    // DWAYNE vs DUANE, lengths 6 and 5, match window = max(6,5)/2 - 1 = 2.
    // Matches: D-D, A-A, N-N, E-E (W and Y are unmatched): 4 matches, 0
    // transpositions since matched characters agree in index order.
    // jaro = (4/6 + 4/5 + 4/4) / 3 = (0.666667 + 0.8 + 1) / 3 = 0.822222
    // shared prefix "D" (W != U at index 1) is 1 character.
    // jw = 0.822222 + 1 * 0.1 * (1 - 0.822222) = 0.822222 + 0.017778 = 0.84
    @Test
    void dwayneAndDuaneMatchTheHandWorkedJaroWinklerScore() {
        assertThat(metric.similarity("DWAYNE", "DUANE")).isCloseTo(0.84, within(0.000001));
    }

    // DIXON vs DICKSONX, lengths 5 and 8, match window = max(5,8)/2 - 1 = 3.
    // Matches: D-D, I-I, O-O, N-N (X and C, K, S are unmatched): 4 matches, 0
    // transpositions since matched characters agree in index order.
    // jaro = (4/5 + 4/8 + 4/4) / 3 = (0.8 + 0.5 + 1) / 3 = 0.766667
    // shared prefix "DI" (X != C at index 2) is 2 characters.
    // jw = 0.766667 + 2 * 0.1 * (1 - 0.766667) = 0.766667 + 0.046667 = 0.813333
    @Test
    void dixonAndDicksonxMatchTheHandWorkedJaroWinklerScore() {
        assertThat(metric.similarity("DIXON", "DICKSONX")).isCloseTo(0.813333, within(0.000001));
    }

    // Fixed synthetic corpus: values are not drawn from any real dataset.
    private static final String[][] CORPUS = {
        {"alpha", "alpha"},
        {"alpha", "alphb"},
        {"bravo", "bravado"},
        {"charlie", "charlie"},
        {"charlie", "charliex"},
        {"delta", "deltaa"},
        {"echo", "ecko"},
        {"foxtrot", "foxtot"},
        {"golf", "gulf"},
        {"hotel", "motel"},
        {"india", "indiana"},
        {"juliet", "juilet"},
        {"kilo", "kolo"},
        {"lima", "lime"},
        {"mike", "mic"},
        {"november", "novembre"},
        {"oscar", "oskar"},
        {"papa", "poppa"},
        {"quebec", "quebeck"},
        {"romeo", "rodeo"},
        {"sierra", "seirra"},
        {"tango", "tangoo"},
        {"", ""},
        {"", "uniform"},
    };

    @Test
    void isBoundedOverTheCorpus() {
        for (String[] pair : CORPUS) {
            double score = metric.similarity(pair[0], pair[1]);
            assertThat(score).isBetween(0.0, 1.0);
        }
    }

    @Test
    void isReflexiveOverTheCorpus() {
        for (String[] pair : CORPUS) {
            assertThat(metric.similarity(pair[0], pair[0])).isEqualTo(1.0);
            assertThat(metric.similarity(pair[1], pair[1])).isEqualTo(1.0);
        }
    }

    @Test
    void isExactlySymmetricOverTheCorpus() {
        for (String[] pair : CORPUS) {
            assertThat(metric.similarity(pair[0], pair[1]))
                    .isEqualTo(metric.similarity(pair[1], pair[0]));
        }
    }
}
