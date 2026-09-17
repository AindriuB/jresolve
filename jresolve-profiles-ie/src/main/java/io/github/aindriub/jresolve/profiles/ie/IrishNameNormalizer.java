package io.github.aindriub.jresolve.profiles.ie;

import io.github.aindriub.jresolve.normalization.ApostropheVariantNormalizer;
import io.github.aindriub.jresolve.normalization.CaseFoldNormalizer;
import io.github.aindriub.jresolve.normalization.CombiningMarkNormalizer;
import io.github.aindriub.jresolve.normalization.CompositeNormalizer;
import io.github.aindriub.jresolve.normalization.PunctuationNormalizer;
import io.github.aindriub.jresolve.normalization.StringNormalizer;
import io.github.aindriub.jresolve.normalization.UnicodeFormNormalizer;
import io.github.aindriub.jresolve.normalization.WhitespaceNormalizer;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Normalizes a personal name for Irish and English orthography.
 *
 * <p>The stages, in order: decompose to NFD, drop combining marks so
 * {@code á} folds to {@code a}, canonicalise the several apostrophe
 * characters to one, remove apostrophes and full stops entirely, lower-case,
 * and collapse whitespace.
 *
 * <p><strong>What this deliberately does not do.</strong> It does not try to
 * bring {@code Ó Súilleabháin} and {@code O'Sullivan} together. After folding
 * those are {@code o suilleabhain} and {@code osullivan}, and no further
 * normalization rule closes that gap without also merging names that are
 * genuinely different. They are two languages' renderings of one name, which
 * is an alias relation — see {@link IrishNameAliases}. Chasing such pairs by
 * adding ever more aggressive normalization is the trap this class exists to
 * stay out of.
 *
 * <p>What it does close is the spelling noise that is genuinely mechanical:
 * a fada, a typographic apostrophe, a case difference, a doubled space.
 *
 * <p>Stateless and immutable; one instance may be shared across threads.
 */
public final class IrishNameNormalizer implements StringNormalizer {

    private static final Set<Character> REMOVED = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList('\'', '.')));

    private final StringNormalizer stages = new CompositeNormalizer(Arrays.asList(
            new UnicodeFormNormalizer(Normalizer.Form.NFD),
            new CombiningMarkNormalizer(),
            new ApostropheVariantNormalizer(),
            new PunctuationNormalizer(REMOVED),
            new CaseFoldNormalizer(),
            new WhitespaceNormalizer()));

    @Override
    public String normalize(String value) {
        return stages.normalize(value);
    }
}
