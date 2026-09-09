package io.github.aindriub.jresolve.comparison;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class LevenshteinSimilarityTest {

    private final LevenshteinSimilarity metric = new LevenshteinSimilarity();

    @Test
    void bothNullArgumentsAreEqual() {
        assertThat(metric.similarity(null, null)).isEqualTo(1.0);
    }

    @Test
    void oneNullArgumentAgainstNonEmptyIsZero() {
        assertThat(metric.similarity(null, "a")).isEqualTo(0.0);
    }

    @Test
    void identicalStringsAreEqual() {
        assertThat(metric.similarity("kitten", "kitten")).isEqualTo(1.0);
    }

    @Test
    void emptyStringsAreEqual() {
        assertThat(metric.similarity("", "")).isEqualTo(1.0);
    }

    // kitten -> sitting: substitute k/s, substitute e/i, insert g. Distance
    // is 3 over the longer string's length of 7.
    // similarity = 1 - 3/7 = 4/7 = 0.571428571...
    @Test
    void kittenAndSittingMatchTheHandWorkedDistance() {
        assertThat(metric.similarity("kitten", "sitting")).isCloseTo(4.0 / 7.0, within(0.000001));
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
