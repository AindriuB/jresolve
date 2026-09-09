package io.github.aindriub.jresolve.field;

/**
 * Ascending cost tiers a {@link FieldDefinition} may declare.
 *
 * <p>These constants express an ordering between fields — compare cheap ones
 * before expensive ones — not a measured unit of cost such as nanoseconds or
 * allocations. Any ascending {@code int} values work; the ones declared here
 * are simply convenient defaults with headroom for a caller to insert an
 * intermediate tier.
 */
public final class CostTiers {

    public static final int CHEAP = 0;
    public static final int MODERATE = 100;
    public static final int EXPENSIVE = 200;

    private CostTiers() {
    }
}
