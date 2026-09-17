package io.github.aindriub.jresolve.comparison;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Splits a value on runs of anything that is neither a letter nor a digit.
 *
 * <p>Whitespace, hyphens, commas, full stops, slashes and apostrophes are all
 * separators, so one value yields the same tokens however its parts were
 * punctuated. A run of separators of any length, in any position, yields no
 * empty token.
 *
 * <p>This splitter expects an <em>already normalized</em> value (see the
 * two-phase pipeline: preparation happens once, comparison happens per
 * candidate). It does not case-fold, strip diacritics or reorder. A letter is
 * whatever the JDK considers one, so a diacritic-bearing character is part of
 * a token rather than a separator, and a value that has not been normalized
 * will tokenise but will not compare well against one that has.
 *
 * <p>Splitting an apostrophe-joined value into two tokens is deliberate.
 * Reconciling spelling variants is the normalization and alias layers' job,
 * not the splitter's; a splitter that special-cased them would be making a
 * linguistic judgement in the one place that has no vocabulary to make it.
 *
 * <p>This class is stateless and immutable. One instance may be shared across
 * threads.
 */
public final class DefaultTokenSplitter implements TokenSplitter {

    /**
     * Splits a value into tokens.
     *
     * @param value the normalized value, or {@code null}
     * @return the tokens in input order, never {@code null} and never
     *         containing an empty string; unmodifiable
     */
    @Override
    public List<String> split(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int index = 0;
        while (index < value.length()) {
            int codePoint = value.codePointAt(index);
            if (Character.isLetterOrDigit(codePoint)) {
                current.appendCodePoint(codePoint);
            } else if (current.length() > 0) {
                tokens.add(current.toString());
                current.setLength(0);
            }
            index += Character.charCount(codePoint);
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return Collections.unmodifiableList(tokens);
    }
}
