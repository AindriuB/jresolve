package io.github.aindriub.jresolve.evidence;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The outcome of comparing one field between two records.
 *
 * <p>This is an open value type rather than an enum: it is the key of every
 * scoring table (Fellegi-Sunter {@code m}/{@code u} pairs, rule weights), and a
 * field type that cannot mint its own category cannot be scored. New
 * categories are created by field comparators outside this module.
 *
 * <p>Instances are interned by name so that {@code ==} and {@link #equals}
 * agree and an instance is safe to use as a map key. The interning table below
 * is the one static field this class keeps; it holds category <em>names</em>
 * only — short, fixed, developer-chosen identifiers such as {@code "EXACT"} —
 * and no field value from a compared record is ever stored in it.
 *
 * <p>The built-in constants and the core comparators that produce them:
 *
 * <table>
 * <caption>Built-in categories and their producers</caption>
 * <tr><th>Constant</th><th>Produced by</th></tr>
 * <tr><td>{@code EXACT}</td><td>{@code ExactFieldComparator},
 *     {@code SimilarityFieldComparator}, {@code AliasAwareFieldComparator}</td></tr>
 * <tr><td>{@code VERY_HIGH}, {@code HIGH}, {@code MEDIUM}, {@code LOW}</td>
 *     <td>{@code SimilarityFieldComparator} (via {@code SimilarityBands})</td></tr>
 * <tr><td>{@code ALIAS_TRANSLATION}, {@code ALIAS_NICKNAME}, {@code ALIAS_VARIANT}</td>
 *     <td>{@code AliasAwareFieldComparator}</td></tr>
 * <tr><td>{@code SUBSUMED}, {@code PARTIAL_OVERLAP}</td>
 *     <td>{@code TokenSubsumptionComparator}</td></tr>
 * <tr><td>{@code CONFLICT}</td><td>{@code ExactFieldComparator}</td></tr>
 * <tr><td>{@code MISSING_ONE}, {@code MISSING_BOTH}</td>
 *     <td>every comparator, via {@code AbstractNullSafeFieldComparator}</td></tr>
 * </table>
 */
public final class ComparisonCategory {

    private static final ConcurrentHashMap<String, ComparisonCategory> INSTANCES = new ConcurrentHashMap<String, ComparisonCategory>();

    public static final ComparisonCategory EXACT = of("EXACT");
    public static final ComparisonCategory ALIAS_TRANSLATION = of("ALIAS_TRANSLATION");
    public static final ComparisonCategory ALIAS_NICKNAME = of("ALIAS_NICKNAME");
    public static final ComparisonCategory ALIAS_VARIANT = of("ALIAS_VARIANT");
    public static final ComparisonCategory VERY_HIGH = of("VERY_HIGH");
    public static final ComparisonCategory HIGH = of("HIGH");
    public static final ComparisonCategory MEDIUM = of("MEDIUM");
    public static final ComparisonCategory LOW = of("LOW");
    /** One side's tokens are strictly contained in the other's. */
    public static final ComparisonCategory SUBSUMED = of("SUBSUMED");
    /** The sides share tokens, but neither contains the other. */
    public static final ComparisonCategory PARTIAL_OVERLAP = of("PARTIAL_OVERLAP");
    public static final ComparisonCategory CONFLICT = of("CONFLICT");
    public static final ComparisonCategory MISSING_ONE = of("MISSING_ONE");
    public static final ComparisonCategory MISSING_BOTH = of("MISSING_BOTH");

    private final String name;

    private ComparisonCategory(String name) {
        this.name = name;
    }

    /**
     * Returns the interned category with the given name, creating it on first
     * use. Two calls with the same name always return the same instance.
     *
     * @throws IllegalArgumentException if {@code name} is null, empty or
     *     whitespace-only
     */
    public static ComparisonCategory of(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("category name must not be null, empty or whitespace-only");
        }
        String key = name.trim();
        ComparisonCategory existing = INSTANCES.get(key);
        if (existing != null) {
            return existing;
        }
        ComparisonCategory created = new ComparisonCategory(key);
        ComparisonCategory raced = INSTANCES.putIfAbsent(key, created);
        return raced != null ? raced : created;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ComparisonCategory)) {
            return false;
        }
        return name.equals(((ComparisonCategory) other).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
