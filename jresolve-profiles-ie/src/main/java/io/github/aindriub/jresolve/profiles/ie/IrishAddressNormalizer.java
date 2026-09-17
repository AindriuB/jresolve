package io.github.aindriub.jresolve.profiles.ie;

import io.github.aindriub.jresolve.comparison.DefaultTokenSplitter;
import io.github.aindriub.jresolve.comparison.TokenSplitter;
import io.github.aindriub.jresolve.normalization.CaseFoldNormalizer;
import io.github.aindriub.jresolve.normalization.CombiningMarkNormalizer;
import io.github.aindriub.jresolve.normalization.CompositeNormalizer;
import io.github.aindriub.jresolve.normalization.StringNormalizer;
import io.github.aindriub.jresolve.normalization.UnicodeFormNormalizer;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;

/**
 * Normalizes an address into a canonical, space-separated token string.
 *
 * <p>Decomposes to NFD, drops combining marks so {@code ú} folds to {@code u},
 * lower-cases, then <em>splits and rejoins on the tokeniser's own rules</em>.
 * So {@code "12 Main Street, Dublin 4"} and {@code "12 Main Street Dublin 4"}
 * both prepare to {@code "12 main street dublin 4"}.
 *
 * <h2>Why the tokeniser does the punctuation work</h2>
 *
 * <p>An earlier version left punctuation alone, reasoning that the comparator
 * tokenises anyway so a comma could not change a verdict. That was true of
 * comparison and wrong about everything else: two addresses that differ only
 * in punctuation produced different <em>prepared</em> values. Preparation is
 * not only the input to a comparison — it is the value a blocking key would be
 * derived from, and the form a prepared candidate is cached in. Two records
 * that must compare equal should not prepare to two different strings.
 *
 * <p>Rewriting punctuation here with a second rule of its own would risk that
 * rule and the tokeniser drifting apart. Using {@link DefaultTokenSplitter}
 * itself removes the possibility: there is one rule about what separates
 * tokens, and both the normalizer and the comparator obey it because they are
 * running the same code.
 *
 * <p>This is the opposite choice from {@link IrishNameNormalizer}, which
 * removes apostrophes outright so that {@code O'Sullivan} folds to a single
 * token. A name is one token and its punctuation is internal; an address is
 * many tokens and its punctuation separates them.
 *
 * <p>Stateless and immutable; one instance may be shared across threads.
 */
public final class IrishAddressNormalizer implements StringNormalizer {

    private static final char TOKEN_SEPARATOR = ' ';

    private final StringNormalizer stages = new CompositeNormalizer(Arrays.asList(
            new UnicodeFormNormalizer(Normalizer.Form.NFD),
            new CombiningMarkNormalizer(),
            new CaseFoldNormalizer()));

    private final TokenSplitter splitter = new DefaultTokenSplitter();

    @Override
    public String normalize(String value) {
        String folded = stages.normalize(value);
        if (folded == null) {
            return null;
        }
        List<String> tokens = splitter.split(folded);
        StringBuilder result = new StringBuilder(folded.length());
        for (String token : tokens) {
            if (result.length() > 0) {
                result.append(TOKEN_SEPARATOR);
            }
            result.append(token);
        }
        return result.toString();
    }
}
