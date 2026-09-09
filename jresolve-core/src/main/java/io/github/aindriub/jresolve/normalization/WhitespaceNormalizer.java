package io.github.aindriub.jresolve.normalization;

/**
 * Trims leading and trailing whitespace and collapses interior runs of
 * whitespace — including tabs and non-breaking space — to a single space.
 */
public final class WhitespaceNormalizer implements StringNormalizer {

    private static final char NON_BREAKING_SPACE = '\u00A0';
    private static final char SPACE = ' ';

    @Override
    public String normalize(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder result = new StringBuilder(value.length());
        boolean pendingSpace = false;
        boolean started = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (isWhitespace(c)) {
                if (started) {
                    pendingSpace = true;
                }
            } else {
                if (pendingSpace) {
                    result.append(SPACE);
                    pendingSpace = false;
                }
                result.append(c);
                started = true;
            }
        }
        return result.toString();
    }

    private static boolean isWhitespace(char c) {
        return Character.isWhitespace(c) || c == NON_BREAKING_SPACE;
    }
}
