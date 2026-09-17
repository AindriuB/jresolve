package io.github.aindriub.jresolve.alias;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An {@link AliasRepository} whose groups are closed transitively at
 * construction.
 *
 * <p>Closing at construction rather than at lookup is the point: the cost is
 * paid once, where it is visible, instead of on every comparison in the
 * candidate loop.
 *
 * <h2>What a merge does to the alias kind</h2>
 *
 * <p>Two declared groups sharing a member become one group, and the two may
 * have been declared with different kinds. Collapsing the merged group to a
 * single kind would destroy the distinction the kinds exist for — a model can
 * learn that a nickname agreement and a translation agreement carry different
 * evidence only if the repository still tells them apart.
 *
 * <p>So the kind is stored per pair, not per group. A pair declared together
 * keeps exactly the kind it was declared with. A pair connected only through
 * other members gets the <em>strongest available path</em>, where a path is
 * only as strong as its weakest link. Strength runs {@code ALIAS_VARIANT}
 * (a spelling difference, the closest relation), then {@code ALIAS_NICKNAME},
 * then {@code ALIAS_TRANSLATION} (two languages' renderings, the loosest).
 *
 * <p>So if {@code alpha} and {@code bravo} are declared variants and
 * {@code bravo} and {@code charlie} declared translations, then {@code alpha}
 * and {@code charlie} relate as translations — the chain passes through a
 * translation and cannot claim to be a mere spelling difference — while
 * {@code alpha} and {@code bravo} still relate as variants.
 *
 * <h2>Cost</h2>
 *
 * <p>Both lookups are hash lookups, independent of the number of groups:
 * {@code equivalents} reads a map from value to its closed group, and
 * {@code relation} reads a map keyed on the value pair. Construction is what
 * pays, and it is linear in the number of declared pairs plus quadratic in
 * the size of each closed group.
 *
 * <p>Immutable once built, and safe for concurrent reads.
 */
public final class DefaultAliasRepository implements AliasRepository {

    /**
     * The alias kinds this repository accepts, strongest first. A category
     * outside this list is rejected at build time: admitting one would mean
     * deciding where it sits in this ordering, and that is a decision to make
     * deliberately rather than by accepting whatever a caller passes.
     */
    private static final List<ComparisonCategory> STRENGTH_DESCENDING = Collections.unmodifiableList(
            Arrays.asList(
                    ComparisonCategory.ALIAS_VARIANT,
                    ComparisonCategory.ALIAS_NICKNAME,
                    ComparisonCategory.ALIAS_TRANSLATION));

    /**
     * Joins two values into one map key. A control character cannot appear in
     * a normalized value, so no pair of values can collide with another pair.
     */
    private static final String PAIR_SEPARATOR = String.valueOf((char) 1);

    private final Map<String, Set<String>> equivalentsByValue;
    private final Map<String, ComparisonCategory> relationByPair;

    private DefaultAliasRepository(
            Map<String, Set<String>> equivalentsByValue,
            Map<String, ComparisonCategory> relationByPair) {
        this.equivalentsByValue = equivalentsByValue;
        this.relationByPair = relationByPair;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Set<String> equivalents(String normalizedValue) {
        if (normalizedValue == null) {
            throw new IllegalArgumentException("normalizedValue must not be null");
        }
        Set<String> group = equivalentsByValue.get(normalizedValue);
        return group != null ? group : Collections.singleton(normalizedValue);
    }

    @Override
    public ComparisonCategory relation(String left, String right) {
        if (left == null) {
            throw new IllegalArgumentException("left must not be null");
        }
        if (right == null) {
            throw new IllegalArgumentException("right must not be null");
        }
        if (left.equals(right)) {
            return null;
        }
        return relationByPair.get(pairKey(left, right));
    }

    private static String pairKey(String left, String right) {
        return left.compareTo(right) <= 0
                ? left + PAIR_SEPARATOR + right
                : right + PAIR_SEPARATOR + left;
    }

    /**
     * Collects groups and closes them transitively on {@link #build()}.
     *
     * <p>This is the only way to construct a {@link DefaultAliasRepository}.
     */
    public static final class Builder {

        private final List<ComparisonCategory> edgeKinds = new ArrayList<>();
        private final List<String> edgeLeft = new ArrayList<>();
        private final List<String> edgeRight = new ArrayList<>();
        private final Set<String> values = new LinkedHashSet<>();

        private Builder() {
        }

        /**
         * Declares that every member of the group is an alias of every other,
         * with the given kind.
         *
         * @param members at least two distinct already-normalized values
         * @param kind    one of {@code ALIAS_VARIANT}, {@code ALIAS_NICKNAME}
         *                or {@code ALIAS_TRANSLATION}
         * @return this builder
         * @throws IllegalArgumentException on a null or undersized group, a
         *         null or blank member, or an unsupported kind. The message
         *         names the constraint, never a member value.
         */
        public Builder group(Collection<String> members, ComparisonCategory kind) {
            if (members == null) {
                throw new IllegalArgumentException("members must not be null");
            }
            if (kind == null) {
                throw new IllegalArgumentException("kind must not be null");
            }
            if (!STRENGTH_DESCENDING.contains(kind)) {
                throw new IllegalArgumentException(
                        "kind must be ALIAS_VARIANT, ALIAS_NICKNAME or ALIAS_TRANSLATION");
            }
            for (String member : members) {
                if (member == null) {
                    throw new IllegalArgumentException("members must not contain null");
                }
                if (member.trim().isEmpty()) {
                    throw new IllegalArgumentException("members must not contain a blank value");
                }
            }
            List<String> distinct = new ArrayList<>(new LinkedHashSet<>(members));
            if (distinct.size() < 2) {
                throw new IllegalArgumentException(
                        "a group must contain at least two distinct members, had " + distinct.size());
            }
            values.addAll(distinct);
            for (int i = 0; i < distinct.size(); i++) {
                for (int j = i + 1; j < distinct.size(); j++) {
                    edgeLeft.add(distinct.get(i));
                    edgeRight.add(distinct.get(j));
                    edgeKinds.add(kind);
                }
            }
            return this;
        }

        /**
         * Closes every declared group transitively and returns an immutable
         * repository.
         */
        public DefaultAliasRepository build() {
            Map<String, String> parent = new HashMap<>();
            for (String value : values) {
                parent.put(value, value);
            }

            Map<String, ComparisonCategory> relationByPair = new HashMap<>();

            // Strongest kind first. After each tier, any pair that has become
            // connected but has no relation recorded yet was connected by a
            // path whose weakest link is this tier — which is the strongest
            // path available between them.
            for (ComparisonCategory kind : STRENGTH_DESCENDING) {
                for (int i = 0; i < edgeKinds.size(); i++) {
                    if (edgeKinds.get(i) == kind) {
                        union(parent, edgeLeft.get(i), edgeRight.get(i));
                    }
                }
                for (List<String> component : componentsOf(parent).values()) {
                    for (int i = 0; i < component.size(); i++) {
                        for (int j = i + 1; j < component.size(); j++) {
                            String key = pairKey(component.get(i), component.get(j));
                            if (!relationByPair.containsKey(key)) {
                                relationByPair.put(key, kind);
                            }
                        }
                    }
                }
            }

            Map<String, Set<String>> equivalentsByValue = new HashMap<>();
            for (List<String> component : componentsOf(parent).values()) {
                Set<String> group = Collections.unmodifiableSet(new LinkedHashSet<>(component));
                for (String member : component) {
                    equivalentsByValue.put(member, group);
                }
            }

            return new DefaultAliasRepository(
                    Collections.unmodifiableMap(equivalentsByValue),
                    Collections.unmodifiableMap(relationByPair));
        }

        private Map<String, List<String>> componentsOf(Map<String, String> parent) {
            Map<String, List<String>> components = new HashMap<>();
            for (String value : values) {
                String root = find(parent, value);
                List<String> members = components.get(root);
                if (members == null) {
                    members = new ArrayList<>();
                    components.put(root, members);
                }
                members.add(value);
            }
            return components;
        }

        private static String find(Map<String, String> parent, String value) {
            String root = value;
            while (!root.equals(parent.get(root))) {
                root = parent.get(root);
            }
            String current = value;
            while (!current.equals(root)) {
                String next = parent.get(current);
                parent.put(current, root);
                current = next;
            }
            return root;
        }

        private static void union(Map<String, String> parent, String left, String right) {
            String leftRoot = find(parent, left);
            String rightRoot = find(parent, right);
            if (!leftRoot.equals(rightRoot)) {
                parent.put(leftRoot, rightRoot);
            }
        }
    }
}
