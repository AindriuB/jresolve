package io.github.aindriub.jresolve.profiles.ie;

import io.github.aindriub.jresolve.field.DefaultFieldPipeline;
import io.github.aindriub.jresolve.field.FieldNormalizer;
import io.github.aindriub.jresolve.field.FieldPipeline;
import io.github.aindriub.jresolve.normalization.StringNormalizer;
import java.util.List;

/**
 * Ready-made address pipelines: normalize once, then compare by containment
 * with a spelling fallback.
 *
 * <p>Two shapes, because the two sides of a real integration rarely model an
 * address the same way — one system carries a single line, the other carries
 * the lines separately. Both prepare to the same normalized {@code String},
 * which is what lets a resolver compare across them.
 */
public final class IrishAddressPipeline {

    private IrishAddressPipeline() {
    }

    /** For a side that carries the whole address as one string. */
    public static FieldPipeline<String, String> forSingleLine() {
        final StringNormalizer normalizer = new IrishAddressNormalizer();
        FieldNormalizer<String, String> fieldNormalizer = new FieldNormalizer<String, String>() {
            @Override
            public String normalize(String value) {
                return normalizer.normalize(value);
            }
        };
        return new DefaultFieldPipeline<>(fieldNormalizer, new IrishAddressComparator());
    }

    /**
     * For a side that carries the address as separate lines.
     *
     * <p>The lines are joined with a single space before normalizing. Line
     * breaks carry no comparison meaning once the value is tokenised, and
     * joining here rather than in a caller's extractor keeps the two sides'
     * normalized forms produced by the same code.
     *
     * <p>A null list prepares to null, and so does a list whose entries are
     * all null — a value that is absent however it was spelled. Null entries
     * inside a non-empty list are skipped rather than rendered.
     */
    public static FieldPipeline<List<String>, String> forLines() {
        final StringNormalizer normalizer = new IrishAddressNormalizer();
        FieldNormalizer<List<String>, String> fieldNormalizer =
                new FieldNormalizer<List<String>, String>() {
                    @Override
                    public String normalize(List<String> value) {
                        if (value == null) {
                            return null;
                        }
                        StringBuilder joined = new StringBuilder();
                        for (String line : value) {
                            if (line == null) {
                                continue;
                            }
                            if (joined.length() > 0) {
                                joined.append(' ');
                            }
                            joined.append(line);
                        }
                        return joined.length() == 0 ? null : normalizer.normalize(joined.toString());
                    }
                };
        return new DefaultFieldPipeline<>(fieldNormalizer, new IrishAddressComparator());
    }
}
