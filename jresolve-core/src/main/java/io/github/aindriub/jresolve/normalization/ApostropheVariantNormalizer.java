package io.github.aindriub.jresolve.normalization;

/**
 * Folds apostrophe-like characters to a single canonical {@code '}
 * (U+0027).
 *
 * <p>Handles the right single quotation mark, left single quotation mark,
 * modifier letter apostrophe, grave accent and acute accent -- all commonly
 * substituted for a plain apostrophe by different input sources -- so that
 * differently rendered copies of the same token compare equal after this
 * stage.
 */
public final class ApostropheVariantNormalizer implements StringNormalizer {

    private static final char CANONICAL = '\'';

    // U+2019 right single quotation mark, U+2018 left single quotation mark,
    // U+02BC modifier letter apostrophe, U+0060 grave accent, U+00B4 acute
    // accent.
    private static final String VARIANTS = "’‘ʼ`´";

    @Override
    public String normalize(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            result.append(VARIANTS.indexOf(c) >= 0 ? CANONICAL : c);
        }
        return result.toString();
    }
}
