package io.github.aindriub.jresolve.profiles.ie;

import io.github.aindriub.jresolve.alias.AliasRepository;
import io.github.aindriub.jresolve.comparison.JaroWinklerSimilarity;
import io.github.aindriub.jresolve.field.AliasAwareFieldComparator;
import io.github.aindriub.jresolve.field.DefaultFieldPipeline;
import io.github.aindriub.jresolve.field.FieldNormalizer;
import io.github.aindriub.jresolve.field.FieldPipeline;
import io.github.aindriub.jresolve.field.SimilarityBands;
import io.github.aindriub.jresolve.field.SimilarityFieldComparator;
import io.github.aindriub.jresolve.normalization.StringNormalizer;

/**
 * A ready-made name pipeline: normalize, then resolve as equal, as an alias,
 * or by spelling similarity.
 *
 * <p>The three stages answer three different questions, and the order matters.
 * Equality is cheapest and decisive. An alias relation is the only thing that
 * can connect two names that share no spelling at all — no metric reaches from
 * {@code sean} to {@code john}, and no normalization rule should try. What is
 * left over is genuine spelling distance, which is what a similarity metric is
 * actually good at.
 *
 * <p>Without the middle stage a fuzzy given name is worse than useless: two
 * correct renderings of one name score near zero and push a true match
 * <em>down</em>. That is the gap this module exists to close.
 */
public final class IrishNamePipeline {

    private IrishNamePipeline() {
    }

    /** Uses the illustrative tables in {@link IrishNameAliases}. */
    public static FieldPipeline<String, String> forGivenName() {
        return withAliases(IrishNameAliases.repository());
    }

    /**
     * Uses a caller-supplied repository — the path a consumer takes once it
     * has a sourced corpus rather than this module's illustrative one.
     *
     * @throws IllegalArgumentException if {@code repository} is null
     */
    public static FieldPipeline<String, String> withAliases(AliasRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("repository must not be null");
        }
        final StringNormalizer normalizer = new IrishNameNormalizer();
        FieldNormalizer<String, String> fieldNormalizer = new FieldNormalizer<String, String>() {
            @Override
            public String normalize(String value) {
                return normalizer.normalize(value);
            }
        };
        return new DefaultFieldPipeline<>(
                fieldNormalizer,
                new AliasAwareFieldComparator(
                        repository,
                        new SimilarityFieldComparator(
                                new JaroWinklerSimilarity(), new SimilarityBands())));
    }
}
