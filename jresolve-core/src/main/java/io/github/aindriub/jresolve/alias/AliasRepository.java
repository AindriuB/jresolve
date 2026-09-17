package io.github.aindriub.jresolve.alias;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import java.util.Set;

/**
 * Equivalence groups of values that mean the same thing.
 *
 * <p>Groups, not directed pairs. A directed {@code findAliases(canonical)}
 * lookup has to say which of two values is the canonical one, and for two
 * renderings of one value across languages neither is. It also cannot close
 * transitively: if {@code alpha} relates to {@code bravo} and {@code alpha}
 * relates to {@code charlie}, then {@code bravo} and {@code charlie} are
 * related too, and a pair lookup will not tell you that.
 *
 * <p>Lookups are on <em>already normalized</em> values. The repository applies
 * no normalization of its own, so a caller passing a raw value will miss where
 * a prepared one would hit.
 *
 * <p>Alias strength is deliberately absent. The <em>kind</em> of alias is a
 * {@link ComparisonCategory}, and the scoring model assigns its weight — the
 * same table would need different weights under a rule scorer and a
 * probabilistic one, so a strength stored in the data would be a scoring
 * decision living in reference data.
 */
public interface AliasRepository {

    /**
     * Every value equivalent to the given one, including the value itself.
     *
     * @param normalizedValue an already normalized value, never {@code null}
     * @return an unmodifiable set containing at least the input; a singleton
     *         for a value the repository has never seen
     * @throws IllegalArgumentException if {@code normalizedValue} is null
     */
    Set<String> equivalents(String normalizedValue);

    /**
     * The kind of alias relating two different values.
     *
     * <p>Symmetric: {@code relation(a, b)} equals {@code relation(b, a)}.
     *
     * <p>Returns null for two values that are equal. Identity is equality, not
     * an alias relation, and a comparator resolves the exact case before ever
     * consulting a repository.
     *
     * @param left  an already normalized value, never {@code null}
     * @param right an already normalized value, never {@code null}
     * @return the alias kind, or null when the values are unrelated or equal
     * @throws IllegalArgumentException if either argument is null
     */
    ComparisonCategory relation(String left, String right);
}
