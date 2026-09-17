package io.github.aindriub.jresolve.scoring;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * How often each value occurs in a corpus, per field.
 *
 * <p>This exists for one purpose: agreement on a common value is weak
 * evidence and agreement on a rare one is strong. In a Fellegi-Sunter model
 * {@code u} is P(agreement | non-match), and for a value drawn from a
 * population that is roughly its frequency in that population. A model keyed
 * on {@code (field, category)} alone cannot express that — it makes agreement
 * on the commonest value in the corpus weigh exactly as much as agreement on
 * one that occurs once.
 *
 * <h2>The floor</h2>
 *
 * <p>A value the corpus never saw returns the {@linkplain #DEFAULT_FLOOR
 * floor}, never zero. A corpus is a sample, so absence from it means rare,
 * not impossible — and a zero would make {@code u} zero and {@code log2(m/u)}
 * infinite, which is a far worse answer than a small one. An observed
 * frequency below the floor is raised to it for the same reason.
 *
 * <h2>Keys</h2>
 *
 * <p>Keys are already-normalized values. The table applies no normalization
 * of its own, so a caller counting raw values and querying prepared ones will
 * miss every time.
 *
 * <p>The key for an agreeing comparison comes from
 * {@code FieldEvidence.getFrequencyKey()}, which
 * {@code ExactFieldComparator} derives from the value's {@code toString()}.
 * That is why its Javadoc requires {@code toString} to be consistent with
 * {@code equals}: where it is not, two occurrences of one value produce two
 * keys, each counted once, and <em>every</em> value looks rare. Nothing here
 * can detect that.
 *
 * <p>Immutable once built, and safe for concurrent reads.
 */
public final class TermFrequencyTable {

    /**
     * The default floor, one in a million.
     *
     * <p>An engineering default rather than a statistically validated one: it
     * is low enough not to distort a value the corpus genuinely saw, and high
     * enough to keep a log ratio finite. A consumer with a known corpus size
     * should set it deliberately — roughly the reciprocal of that size is a
     * defensible starting point.
     */
    public static final double DEFAULT_FLOOR = 0.000001;

    private final Map<String, Map<String, Long>> countsByField;
    private final Map<String, Long> totalsByField;
    private final double floor;

    private TermFrequencyTable(
            Map<String, Map<String, Long>> countsByField,
            Map<String, Long> totalsByField,
            double floor) {
        this.countsByField = countsByField;
        this.totalsByField = totalsByField;
        this.floor = floor;
    }

    public static Builder builder() {
        return new Builder(DEFAULT_FLOOR);
    }

    /**
     * @param floor the smallest frequency this table will report, within
     *              {@code (0, 1]}
     * @throws IllegalArgumentException if the floor is outside that range
     */
    public static Builder builder(double floor) {
        if (!(floor > 0.0) || floor > 1.0) {
            throw new IllegalArgumentException("floor must be within (0, 1], was " + floor);
        }
        return new Builder(floor);
    }

    /** The smallest frequency this table will report. */
    public double getFloor() {
        return floor;
    }

    /**
     * The observed relative frequency of a key within a field.
     *
     * <p>Never zero and never below the floor. A field the corpus never
     * covered returns the floor rather than throwing — a model may ask about
     * a field no corpus was supplied for, and that is a thin answer rather
     * than a programming error.
     *
     * @param field the field name, never null
     * @param key   an already-normalized value, never null
     * @return a frequency within {@code [floor, 1]}
     * @throws IllegalArgumentException if either argument is null
     */
    public double frequencyOf(String field, String key) {
        if (field == null) {
            throw new IllegalArgumentException("field must not be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        Long total = totalsByField.get(field);
        if (total == null || total == 0L) {
            return floor;
        }
        Map<String, Long> counts = countsByField.get(field);
        Long count = counts == null ? null : counts.get(key);
        if (count == null) {
            return floor;
        }
        double observed = (double) count / (double) total;
        return observed < floor ? floor : observed;
    }

    /**
     * Counts a corpus one observation at a time, so a consumer can stream a
     * candidate set rather than materialise it.
     */
    public static final class Builder {

        private final Map<String, Map<String, Long>> countsByField = new HashMap<>();
        private final Map<String, Long> totalsByField = new HashMap<>();
        private final double floor;

        private Builder(double floor) {
            this.floor = floor;
        }

        /**
         * Records one occurrence of a key for a field.
         *
         * @throws IllegalArgumentException if either argument is null. The
         *     message names the constraint and never the key, which is a
         *     prepared field value.
         */
        public Builder observe(String field, String key) {
            if (field == null) {
                throw new IllegalArgumentException("field must not be null");
            }
            if (key == null) {
                throw new IllegalArgumentException("key must not be null");
            }
            Map<String, Long> counts = countsByField.get(field);
            if (counts == null) {
                counts = new HashMap<>();
                countsByField.put(field, counts);
            }
            Long current = counts.get(key);
            counts.put(key, current == null ? 1L : current + 1L);
            Long total = totalsByField.get(field);
            totalsByField.put(field, total == null ? 1L : total + 1L);
            return this;
        }

        /** Builds an immutable table. The builder may be reused afterwards. */
        public TermFrequencyTable build() {
            Map<String, Map<String, Long>> counts = new HashMap<>();
            for (Map.Entry<String, Map<String, Long>> entry : countsByField.entrySet()) {
                counts.put(entry.getKey(),
                        Collections.unmodifiableMap(new HashMap<>(entry.getValue())));
            }
            return new TermFrequencyTable(
                    Collections.unmodifiableMap(counts),
                    Collections.unmodifiableMap(new HashMap<>(totalsByField)),
                    floor);
        }
    }
}
