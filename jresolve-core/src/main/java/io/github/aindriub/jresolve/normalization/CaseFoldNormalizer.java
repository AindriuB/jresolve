package io.github.aindriub.jresolve.normalization;

import java.util.Locale;

/**
 * Folds case using {@link Locale#ROOT}.
 *
 * <p>The root locale is used rather than the platform default so that
 * behaviour does not vary with the JVM's locale configuration — the
 * classic trap being a Turkish-locale JVM folding {@code "I"} to
 * {@code "ı"} instead of {@code "i"}.
 */
public final class CaseFoldNormalizer implements StringNormalizer {

    @Override
    public String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT);
    }
}
