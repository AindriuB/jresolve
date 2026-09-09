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
