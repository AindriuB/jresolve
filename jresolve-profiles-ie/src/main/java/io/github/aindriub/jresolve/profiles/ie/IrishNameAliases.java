package io.github.aindriub.jresolve.profiles.ie;

import io.github.aindriub.jresolve.alias.AliasRepository;
import io.github.aindriub.jresolve.alias.DefaultAliasRepository;
import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.normalization.StringNormalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <strong>Illustrative alias tables. Not release data.</strong>
 *
 * <p>These groups are hand-written to exercise and demonstrate the alias
 * layer. They are not drawn from any sourced corpus, they are nowhere near
 * complete, and the provenance question — finding Irish/English and
 * diminutive tables under a licence compatible with this project — is
 * recorded as open in {@code docs/design-decisions.md} (D19) and has not been
 * answered. Shipping a release that treats this class as reference data would
 * be a mistake; replace the contents first.
 *
 * <p>No entry names a real person. These are name forms, which are public
 * linguistic facts rather than personal data.
 *
 * <h2>What the three kinds mean here</h2>
 *
 * <ul>
 *   <li>{@code ALIAS_TRANSLATION} — two languages' renderings of one name,
 *       including the Irish and English forms of a surname. This is where
 *       {@code Ó Súilleabháin} and {@code O'Sullivan} live, because
 *       normalization cannot bring them together (see
 *       {@link IrishNameNormalizer}).</li>
 *   <li>{@code ALIAS_NICKNAME} — a diminutive or familiar form.</li>
 *   <li>{@code ALIAS_VARIANT} — a spelling variant within one language that
 *       survives normalization.</li>
 * </ul>
 *
 * <p>Every value is stored already normalized, because a repository is looked
 * up on prepared values. The same {@link IrishNameNormalizer} that a pipeline
 * applies is applied here, so the two cannot drift.
 */
public final class IrishNameAliases {

    private IrishNameAliases() {
    }

    private static final String[][] TRANSLATIONS = {
        {"Seán", "John"},
        {"Séamus", "James"},
        {"Pádraig", "Patrick"},
        {"Máire", "Mary"},
        {"Liam", "William"},
        {"Caoimhín", "Kevin"},
        {"Ó Súilleabháin", "O'Sullivan"},
        {"Mac Cárthaigh", "McCarthy"},
        {"Ó Briain", "O'Brien"},
    };

    private static final String[][] NICKNAMES = {
        {"Patrick", "Paddy", "Pat"},
        {"William", "Will", "Bill"},
        {"Margaret", "Peggy", "Maggie"},
        {"Robert", "Bob", "Bobby"},
        {"James", "Jim", "Jimmy"},
    };

    /**
     * Spelling variants within one language that survive normalization.
     *
     * <p>A pair that normalization already closes does <em>not</em> belong
     * here: {@code Sinéad} and {@code Sinead} both prepare to {@code sinead},
     * so listing them would declare a group with one distinct member, and
     * {@link DefaultAliasRepository} rejects that at build time. That
     * rejection is a useful guard rather than an obstacle — an alias table is
     * for the residue normalization cannot reach, and an entry that folds away
     * is a sign the pair was already handled a layer earlier.
     */
    private static final String[][] VARIANTS = {
        {"Eoghan", "Eoin"},
        {"Conchúr", "Conchobhar"},
        {"Aoibheann", "Aoibhinn"},
    };

    /**
     * Builds the repository, closing every group transitively.
     *
     * <p>A fresh instance each call; the result is immutable, so a caller
     * that resolves repeatedly should build once and hold it.
     */
    public static AliasRepository repository() {
        StringNormalizer normalizer = new IrishNameNormalizer();
        DefaultAliasRepository.Builder builder = DefaultAliasRepository.builder();
        addAll(builder, normalizer, TRANSLATIONS, ComparisonCategory.ALIAS_TRANSLATION);
        addAll(builder, normalizer, NICKNAMES, ComparisonCategory.ALIAS_NICKNAME);
        addAll(builder, normalizer, VARIANTS, ComparisonCategory.ALIAS_VARIANT);
        return builder.build();
    }

    private static void addAll(
            DefaultAliasRepository.Builder builder,
            StringNormalizer normalizer,
            String[][] groups,
            ComparisonCategory kind) {
        for (String[] group : groups) {
            List<String> normalized = new ArrayList<>(group.length);
            for (String member : Arrays.asList(group)) {
                normalized.add(normalizer.normalize(member));
            }
            builder.group(normalized, kind);
        }
    }
}
