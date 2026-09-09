package io.github.aindriub.jresolve.normalization;

/**
 * Removes Unicode combining marks (general category {@code Mn}).
 *
 * <p>Meant to run after a stage that decomposes a string into base
 * characters plus combining marks, such as {@link UnicodeFormNormalizer}
 * configured with {@link java.text.Normalizer.Form#NFD}. Applied to a
 * string that has not been decomposed, it leaves precomposed characters
 * untouched — there is nothing to remove.
 *
 * <p>Iterates by {@code char} rather than by code point, so only combining
 * marks in the Basic Multilingual Plane are removed; supplementary-plane
 * combining marks (e.g. U+1D167, U+E0100) pass through unmodified, and the
 * surrogate halves of any non-BMP text are left intact rather than corrupted.
 */
public final class CombiningMarkNormalizer implements StringNormalizer {

    @Override
    public String normalize(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.getType(c) != Character.NON_SPACING_MARK) {
                result.append(c);
            }
        }
        return result.toString();
    }
}
