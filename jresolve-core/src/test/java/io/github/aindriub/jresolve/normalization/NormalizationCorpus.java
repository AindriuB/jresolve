package io.github.aindriub.jresolve.normalization;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A fixed, synthetic corpus of strings shared by the normalization property
 * tests. Covers Latin-1 diacritics, apostrophe variants, mixed whitespace
 * (tabs, a non-breaking space, runs of plain spaces) and punctuation. No
 * entry is drawn from a real person, address or dataset.
 */
final class NormalizationCorpus {

    static final List<String> VALUES = Collections.unmodifiableList(Arrays.asList(
            "café",
            "naïve",
            "Zoë",
            "coöperate",
            "façade",
            "  leading and trailing  ",
            "tab\tseparated",
            "non breaking space",
            "multiple   spaces",
            "O'Brien",
            "O’Brien",
            "O‘Brien",
            "OʼBrien",
            "O`Brien",
            "O´Brien",
            "MixedCASE",
            "ALLCAPS",
            "Seán",
            "Sean",
            "über",
            "  ÀÉÎÕÜ  ",
            "punctuation!@#$%",
            "hyphen-ated",
            "",
            "x"
    ));

    private NormalizationCorpus() {
    }
}
