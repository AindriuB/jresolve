package io.github.aindriub.jresolve.scoring;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A {@link FellegiSunterModel} configured through a builder.
 *
 * <p>Built, never deserialized. An {@code m}/{@code u} table has to come from
 * somewhere, but where is a consumer's problem — this is a plain immutable
 * object, and loading one from whatever format a consumer keeps is their job
 * rather than a dependency of this library.
 *
 * <p>Immutable once built, and safe for concurrent reads.
 */
public final class DefaultFellegiSunterModel implements FellegiSunterModel {

    private static final String PAIR_SEPARATOR = String.valueOf((char) 1);

    private final Map<String, Double> mByPair;
    private final Map<String, Double> uByPair;
    private final Set<String> ignoredPairs;
    private final TermFrequencyTable frequencies;
    private final Double priorOdds;
    private final Collection<Set<String>> compositeGroups;

    private DefaultFellegiSunterModel(Builder builder) {
        this.mByPair = Collections.unmodifiableMap(new HashMap<>(builder.mByPair));
        this.uByPair = Collections.unmodifiableMap(new HashMap<>(builder.uByPair));
        this.ignoredPairs = Collections.unmodifiableSet(new HashSet<>(builder.ignoredPairs));
        this.frequencies = builder.frequencies;
        this.priorOdds = builder.priorOdds;
        List<Set<String>> groups = new ArrayList<>();
        for (Set<String> group : builder.compositeGroups) {
            groups.add(Collections.unmodifiableSet(new LinkedHashSet<>(group)));
        }
        this.compositeGroups = Collections.unmodifiableList(groups);
    }

    public static Builder builder() {
        return new Builder();
    }

    private static String pairKey(String field, ComparisonCategory category) {
        return field + PAIR_SEPARATOR + category.getName();
    }

    private void requireConfigured(String field, ComparisonCategory category, String key) {
        if (!mByPair.containsKey(key)) {
            throw new IllegalStateException("no m/u configured for field '" + field
                    + "' and category " + category.getName()
                    + "; configure it or declare it ignored");
        }
    }

    @Override
    public double mProbability(String field, ComparisonCategory category) {
        String key = pairKey(requireField(field), requireCategory(category));
        requireConfigured(field, category, key);
        return mByPair.get(key);
    }

    @Override
    public double uProbability(String field, ComparisonCategory category, String frequencyKey) {
        String key = pairKey(requireField(field), requireCategory(category));
        requireConfigured(field, category, key);
        if (frequencies == null || frequencyKey == null || !frequencies.covers(field)) {
            // No corpus, no agreed value, or no corpus *for this field*: the
            // flat configured u is the honest answer. Adjusting on an
            // uncovered field would read the table's floor as a frequency,
            // and the floor is its rarest answer — so a consumer who supplied
            // a corpus for one field would get wildly overconfident weights
            // on every other. Found end to end by task 24, where an uncovered
            // field scored 18.9 bits instead of 1.0.
            return uByPair.get(key);
        }
        // u is P(agreement | non-match), and for a value drawn from a
        // population that is roughly how often the value occurs in it. The
        // table already guarantees a result within [floor, 1], so there is
        // nothing to clamp here — see the note in this class's test.
        return frequencies.frequencyOf(field, frequencyKey);
    }

    @Override
    public boolean isIgnored(String field, ComparisonCategory category) {
        return ignoredPairs.contains(pairKey(requireField(field), requireCategory(category)));
    }

    @Override
    public boolean hasPriorOdds() {
        return priorOdds != null;
    }

    @Override
    public double priorOdds() {
        if (priorOdds == null) {
            throw new IllegalStateException(
                    "no prior odds configured; check hasPriorOdds() before calling");
        }
        return priorOdds;
    }

    @Override
    public Collection<Set<String>> compositeGroups() {
        return compositeGroups;
    }

    private static String requireField(String field) {
        if (field == null) {
            throw new IllegalArgumentException("field must not be null");
        }
        return field;
    }

    private static ComparisonCategory requireCategory(ComparisonCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("category must not be null");
        }
        return category;
    }

    /** Collects probabilities, composites and the optional prior. */
    public static final class Builder {

        private final Map<String, Double> mByPair = new HashMap<>();
        private final Map<String, Double> uByPair = new HashMap<>();
        private final Set<String> ignoredPairs = new HashSet<>();
        private final Set<String> configuredFields = new HashSet<>();
        private final List<Set<String>> compositeGroups = new ArrayList<>();
        private TermFrequencyTable frequencies;
        private Double priorOdds;

        private Builder() {
        }

        /**
         * Configures {@code m} and {@code u} for one field and category.
         *
         * @throws IllegalArgumentException if either probability is outside
         *     {@code (0, 1]}, or either argument is null. Zero is rejected
         *     because {@code log2(m/u)} is undefined or infinite at zero;
         *     avoid it by validation or smoothing, never by letting it
         *     through.
         */
        public Builder probabilities(String field, ComparisonCategory category,
                double mProbability, double uProbability) {
            requireField(field);
            requireCategory(category);
            requireProbability(mProbability, "m", field, category);
            requireProbability(uProbability, "u", field, category);
            String key = pairKey(field, category);
            mByPair.put(key, mProbability);
            uByPair.put(key, uProbability);
            ignoredPairs.remove(key);
            configuredFields.add(field);
            return this;
        }

        /**
         * Declares that this field and category carry no evidence, so a
         * scorer skips them.
         *
         * <p>This is how §45's rule is honoured: a missing value is not
         * treated as a non-match unless the model says so, and saying "this
         * tells us nothing" is clearer than inventing {@code m} and {@code u}
         * figures that amount to the same claim.
         */
        public Builder ignore(String field, ComparisonCategory category) {
            requireField(field);
            requireCategory(category);
            String key = pairKey(field, category);
            ignoredPairs.add(key);
            mByPair.remove(key);
            uByPair.remove(key);
            configuredFields.add(field);
            return this;
        }

        /**
         * Supplies the corpus frequencies that adjust {@code u} for agreeing
         * comparisons. Without one, every {@code u} is the flat configured
         * figure.
         */
        public Builder frequencies(TermFrequencyTable table) {
            if (table == null) {
                throw new IllegalArgumentException("table must not be null");
            }
            this.frequencies = table;
            return this;
        }

        /**
         * Sets the prior odds of a match before any evidence.
         *
         * <p>Leaving them unset is a real choice, not an omission: a scorer
         * then exposes a weight and no probability, which is the honest
         * answer when nobody has supplied a base rate.
         *
         * @throws IllegalArgumentException if the odds are not strictly positive
         */
        public Builder priorOdds(double odds) {
            if (!(odds > 0.0) || Double.isInfinite(odds) || Double.isNaN(odds)) {
                throw new IllegalArgumentException("prior odds must be finite and strictly positive");
            }
            this.priorOdds = odds;
            return this;
        }

        /**
         * Declares two or more fields to be one comparison because they are
         * not conditionally independent.
         *
         * @throws IllegalArgumentException if fewer than two distinct members,
         *     or a null member
         */
        public Builder composite(Collection<String> fields) {
            if (fields == null) {
                throw new IllegalArgumentException("fields must not be null");
            }
            Set<String> group = new LinkedHashSet<>();
            for (String field : fields) {
                group.add(requireField(field));
            }
            if (group.size() < 2) {
                throw new IllegalArgumentException(
                        "a composite must name at least two distinct fields, named " + group.size());
            }
            compositeGroups.add(group);
            return this;
        }

        /**
         * @throws IllegalArgumentException if a composite names a field with
         *     no configured or ignored categories — a composite over a field
         *     nothing knows about could never contribute, and silently doing
         *     nothing is worse than failing here.
         */
        public DefaultFellegiSunterModel build() {
            for (Set<String> group : compositeGroups) {
                for (String field : group) {
                    if (!configuredFields.contains(field)) {
                        throw new IllegalArgumentException(
                                "composite names field '" + field
                                        + "', which has no configured or ignored categories");
                    }
                }
            }
            return new DefaultFellegiSunterModel(this);
        }

        private static void requireProbability(
                double value, String which, String field, ComparisonCategory category) {
            if (!(value > 0.0) || value > 1.0) {
                throw new IllegalArgumentException(which + " must be within (0, 1] for field '"
                        + field + "' and category " + category.getName() + ", was " + value);
            }
        }
    }
}
