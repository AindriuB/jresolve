package io.github.aindriub.jresolve.comparison;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class TokenSimilarityTest {

    private static final TokenSplitter WHITESPACE_SPLITTER = value -> {
        if (value.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(value.trim().split("\\s+"));
    };

    private final TokenSimilarity metric =
            new TokenSimilarity(new JaroWinklerSimilarity(), WHITESPACE_SPLITTER);

    @Test
    void bothNullArgumentsAreEqual() {
        assertThat(metric.similarity(null, null)).isEqualTo(1.0);
    }

    @Test
    void oneNullArgumentAgainstNonEmptyIsZero() {
        assertThat(metric.similarity(null, "a b")).isEqualTo(0.0);
    }

    @Test
    void sameTokensInADifferentOrderAreEqual() {
        assertThat(metric.similarity("alpha bravo charlie", "charlie alpha bravo")).isEqualTo(1.0);
    }

    @Test
    void bothSidesWithNoTokensAreEqual() {
        assertThat(metric.similarity("", "")).isEqualTo(1.0);
    }

    // left = "alpha bravo", right = "alpha". Best match for "alpha" against
    // {"alpha"} is 1.0, best match for "bravo" against {"alpha"} is its
    // Jaro-Winkler score, call it x. Best match for "alpha" (right) against
    // {"alpha", "bravo"} is 1.0.
    //
    // Hand-computing x = jaroWinkler("bravo", "alpha"): both strings have
    // length 5, so the Jaro match window is max(5, 5) / 2 - 1 = 1. Checking
    // each character of "bravo" against "alpha" within a window of 1:
    // 'b'(0) vs {a,l} no match; 'r'(1) vs {a,l,p} no match; 'a'(2) vs
    // {l,p,h} no match; 'v'(3) vs {p,h,a} no match; 'o'(4) vs {h,a} no
    // match. Zero matching characters means the Jaro score, and therefore
    // the Jaro-Winkler score (no prefix boost is applied below the boost
    // threshold), is exactly 0.0. So x = 0.0.
    // score = (1.0 + x + 1.0) / (2 + 1) = (2.0 + 0.0) / 3.0 = 2.0 / 3.0
    @Test
    void weightsBestMatchesBySideTokenCounts() {
        double expected = 2.0 / 3.0;

        assertThat(metric.similarity("alpha bravo", "alpha")).isEqualTo(expected);
    }

    // Fixed synthetic corpus: values are not drawn from any real dataset.
    private static final String[][] CORPUS = {
        {"alpha bravo", "alpha bravo"},
        {"alpha bravo", "bravo alpha"},
        {"alpha bravo", "alpha"},
        {"charlie delta", "delta charlie echo"},
        {"foxtrot", "foxtot"},
        {"golf hotel", "hotel golf india"},
        {"juliet kilo lima", "juliet kilo"},
        {"mike november", "mike"},
        {"oscar papa", "papa oscar quebec"},
        {"romeo sierra", "sierra romeo"},
        {"tango uniform", "uniform tango victor"},
        {"whiskey xray", "xray whiskey"},
        {"yankee zulu", "yankee"},
        {"alpha", "alpha alpha"},
        {"bravo charlie delta", "delta bravo charlie"},
        {"echo foxtrot", "foxtrot echo golf"},
        {"hotel india", "india hotel"},
        {"juliet", "juliet kilo lima"},
        {"", ""},
        {"", "mike"},
        {"november oscar", ""},
        {"papa", "papa"},
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
