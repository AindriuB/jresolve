package io.github.aindriub.jresolve.field;

import io.github.aindriub.jresolve.alias.AliasRepository;
import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.DefaultFieldEvidence;
import io.github.aindriub.jresolve.evidence.FieldEvidence;

/**
 * Resolves two values as equal, as aliases of each other, or by delegating to
 * another comparator.
 *
 * <p>Three arms, tried in order:
 *
 * <ol>
 *   <li>equal values yield {@code EXACT};</li>
 *   <li>values an {@link AliasRepository} relates yield that relation's kind —
 *       one of {@code ALIAS_TRANSLATION}, {@code ALIAS_NICKNAME} or
 *       {@code ALIAS_VARIANT};</li>
 *   <li>anything else yields exactly what the delegate returned, unchanged.</li>
 * </ol>
 *
 * <p>The delegate is consulted only on the third arm. That is what makes this
 * comparator useful rather than merely additive: without it, two renderings of
 * one value across languages score as a near-total mismatch on any string
 * metric and act as evidence <em>against</em> a match, because a string metric
 * has no way to know they mean the same thing.
 *
 * <p>An alias hit carries no similarity. A relation drawn from a table is not
 * a measurement, and reporting a number here would invite a consumer to
 * compare it against one that was actually computed. It carries no frequency
 * key either: the two sides did not agree on a value, and a frequency table is
 * keyed on values that did.
 *
 * <p>This comparator is symmetric, because {@code equals} is and
 * {@link AliasRepository#relation} is required to be. Whether the third arm is
 * symmetric is the delegate's business, and a delegate that is not makes this
 * comparator not symmetric either on that arm alone.
 *
 * <p>Note what this comparator is <em>not</em> for. Two spellings that
 * normalization already reconciles — a diacritic, an apostrophe, a case
 * difference — never reach the repository, because normalization has made them
 * equal by the time a prepared value arrives here. What reaches the repository
 * is the residue: pairs that stay distinct however aggressively they are
 * normalized, because they are different words for the same thing. Chasing
 * those in the normalization layer is the trap this arm exists to avoid.
 */
public final class AliasAwareFieldComparator extends AbstractNullSafeFieldComparator<String> {

    private final AliasRepository repository;
    private final FieldComparator<String> delegate;

    /**
     * @param repository the alias table, consulted only when the values differ
     * @param delegate   the comparator for values the repository does not relate
     * @throws IllegalArgumentException if either argument is null
     */
    public AliasAwareFieldComparator(AliasRepository repository, FieldComparator<String> delegate) {
        if (repository == null) {
            throw new IllegalArgumentException("repository must not be null");
        }
        if (delegate == null) {
            throw new IllegalArgumentException("delegate must not be null");
        }
        this.repository = repository;
        this.delegate = delegate;
    }

    @Override
    protected FieldEvidence compareNonNull(String left, String right) {
        if (left.equals(right)) {
            return new DefaultFieldEvidence(ComparisonCategory.EXACT, null, left);
        }
        ComparisonCategory relation = repository.relation(left, right);
        if (relation != null) {
            return new DefaultFieldEvidence(relation, null, null);
        }
        return delegate.compare(left, right);
    }
}
