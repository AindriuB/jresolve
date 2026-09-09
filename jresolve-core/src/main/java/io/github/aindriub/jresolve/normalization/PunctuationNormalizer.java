package io.github.aindriub.jresolve.normalization;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Removes a configured set of characters from a string.
 *
 * <p>The set is a constructor argument rather than a fixed "remove all
 * punctuation" rule, because whether a character is noise depends on the
 * field it appears in: a hyphen is noise in one field and structurally
 * meaningful in another, and blanket removal of every punctuation character
 * would destroy that distinction across every domain that uses this stage.
 * A caller composing a pipeline decides what this stage removes.
 */
public final class PunctuationNormalizer implements StringNormalizer {

    private final Set<Character> characters;

    public PunctuationNormalizer(Set<Character> characters) {
        if (characters == null) {
            throw new IllegalArgumentException("characters must not be null");
        }
        this.characters = Collections.unmodifiableSet(new LinkedHashSet<>(characters));
    }

    @Override
    public String normalize(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!characters.contains(c)) {
                result.append(c);
            }
        }
        return result.toString();
    }
}
