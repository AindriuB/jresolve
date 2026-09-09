package io.github.aindriub.jresolve.normalization;

import java.text.Normalizer;

/**
 * Applies a {@link java.text.Normalizer.Form} Unicode normalization form.
 *
 * <p>Defaults to {@link java.text.Normalizer.Form#NFD}, which decomposes a
 * precomposed character such as a letter with a diacritic into a base
 * character followed by combining marks — the shape a later stage such as
 * {@link CombiningMarkNormalizer} expects.
 */
public final class UnicodeFormNormalizer implements StringNormalizer {

    private final Normalizer.Form form;

    public UnicodeFormNormalizer() {
        this(Normalizer.Form.NFD);
    }

    public UnicodeFormNormalizer(Normalizer.Form form) {
        if (form == null) {
            throw new IllegalArgumentException("form must not be null");
        }
        this.form = form;
    }

    @Override
    public String normalize(String value) {
        if (value == null) {
            return null;
        }
        return Normalizer.normalize(value, form);
    }
}
