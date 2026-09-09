package io.github.aindriub.jresolve.api;

import io.github.aindriub.jresolve.decision.DecisionThresholds;
import io.github.aindriub.jresolve.decision.MatchDecisionEngine;
import io.github.aindriub.jresolve.field.CostTiers;
import io.github.aindriub.jresolve.field.FieldComparator;
import io.github.aindriub.jresolve.field.FieldDefinition;
import io.github.aindriub.jresolve.field.FieldPipeline;
import io.github.aindriub.jresolve.scoring.MatchScorer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Builds an immutable {@link EntityResolver}. Every configuration mistake
 * that can be caught without a source or a candidate is deferred to
 * {@link #build()} rather than surfacing later from {@link
 * EntityResolver#resolve}.
 *
 * @param <S> the source record type; owned entirely by the consumer
 * @param <C> the candidate record type; owned entirely by the consumer
 */
public final class EntityResolverBuilder<S, C> {

    private final List<FieldSlot<S, C>> fieldSlots = new ArrayList<FieldSlot<S, C>>();
    private final Map<String, Integer> costOverrides = new LinkedHashMap<String, Integer>();
    private final Set<String> requiredFieldNames = new LinkedHashSet<String>();
    private final List<CandidateRule<S, C>> rules = new ArrayList<CandidateRule<S, C>>();
    private MatchScorer scorer;
    private MatchDecisionEngine<C> decisionEngine;
    private DecisionThresholds thresholds;

    private EntityResolverBuilder() {
    }

    public static <S, C> EntityResolverBuilder<S, C> builder() {
        return new EntityResolverBuilder<S, C>();
    }

    /**
     * The symmetric case: both sides extract the same raw type {@code V},
     * and {@code pipeline} normalizes each side and compares the results.
     * Sugar over the asymmetric overload — each extractor is composed with
     * {@code pipeline::prepare}, and {@code pipeline::compare} supplies the
     * comparator.
     *
     * @param name the field name; must be unique across every field
     * @param sourceGetter extracts the raw value from the source
     * @param candidateGetter extracts the raw value from the candidate
     * @param pipeline normalizes and compares the extracted values
     */
    public <V, N> EntityResolverBuilder<S, C> field(
            String name,
            Function<S, V> sourceGetter,
            Function<C, V> candidateGetter,
            FieldPipeline<V, N> pipeline) {
        List<String> nullArguments = new ArrayList<String>();
        if (sourceGetter == null) {
            nullArguments.add("source extractor");
        }
        if (candidateGetter == null) {
            nullArguments.add("candidate extractor");
        }
        if (pipeline == null) {
            nullArguments.add("pipeline");
        }
        Function<S, N> sourcePreparer = nullArguments.isEmpty() ? prepareWith(sourceGetter, pipeline) : null;
        Function<C, N> candidatePreparer = nullArguments.isEmpty() ? prepareWith(candidateGetter, pipeline) : null;
        FieldComparator<N> comparator = pipeline == null ? null : pipeline::compare;
        return addField(name, sourcePreparer, candidatePreparer, comparator, nullArguments);
    }

    private static <T, V, N> Function<T, N> prepareWith(Function<T, V> extractor, FieldPipeline<V, N> pipeline) {
        return value -> pipeline.prepare(extractor.apply(value));
    }

    /**
     * The asymmetric case: the source and candidate expose different raw
     * types, each with its own prepare function converging on the shared
     * normalized type {@code N}.
     *
     * @param name the field name; must be unique across every field
     * @param sourceExtractor extracts the raw value from the source
     * @param candidateExtractor extracts the raw value from the candidate
     * @param sourcePrepare normalizes the source's raw value
     * @param candidatePrepare normalizes the candidate's raw value
     * @param comparator compares the two normalized values
     */
    public <VS, VC, N> EntityResolverBuilder<S, C> field(
            String name,
            Function<S, VS> sourceExtractor,
            Function<C, VC> candidateExtractor,
            Function<VS, N> sourcePrepare,
            Function<VC, N> candidatePrepare,
            FieldComparator<N> comparator) {
        List<String> nullArguments = new ArrayList<String>();
        if (sourceExtractor == null) {
            nullArguments.add("source extractor");
        }
        if (candidateExtractor == null) {
            nullArguments.add("candidate extractor");
        }
        if (sourcePrepare == null) {
            nullArguments.add("source prepare function");
        }
        if (candidatePrepare == null) {
            nullArguments.add("candidate prepare function");
        }
        if (comparator == null) {
            nullArguments.add("comparator");
        }
        Function<S, N> sourcePreparer = (sourceExtractor == null || sourcePrepare == null)
                ? null : sourceExtractor.andThen(sourcePrepare);
        Function<C, N> candidatePreparer = (candidateExtractor == null || candidatePrepare == null)
                ? null : candidateExtractor.andThen(candidatePrepare);
        return addField(name, sourcePreparer, candidatePreparer, comparator, nullArguments);
    }

    private <N> EntityResolverBuilder<S, C> addField(
            String name,
            Function<S, N> sourcePreparer,
            Function<C, N> candidatePreparer,
            FieldComparator<N> comparator,
            List<String> nullArguments) {
        fieldSlots.add(new TypedFieldSlot<S, C, N>(name, sourcePreparer, candidatePreparer, comparator,
                Collections.unmodifiableList(nullArguments)));
        return this;
    }

    /**
     * Overrides the cost tier of the named field. Validated at {@link
     * #build()}: a name matching no configured field fails the build.
     */
    public EntityResolverBuilder<S, C> cost(String fieldName, int cost) {
        costOverrides.put(fieldName, cost);
        return this;
    }

    /**
     * Marks the named field required, per {@link
     * io.github.aindriub.jresolve.field.FieldDefinition#isRequired()}.
     * Validated at {@link #build()}: a name matching no configured field
     * fails the build.
     */
    public EntityResolverBuilder<S, C> required(String fieldName) {
        requiredFieldNames.add(fieldName);
        return this;
    }

    /**
     * Adds a hard veto evaluated between cost tiers. Rules run in the order
     * they were added.
     */
    public EntityResolverBuilder<S, C> rule(CandidateRule<S, C> rule) {
        rules.add(rule);
        return this;
    }

    public EntityResolverBuilder<S, C> scorer(MatchScorer scorer) {
        this.scorer = scorer;
        return this;
    }

    public EntityResolverBuilder<S, C> decisionEngine(MatchDecisionEngine<C> decisionEngine) {
        this.decisionEngine = decisionEngine;
        return this;
    }

    /**
     * The thresholds {@link #build()} checks against the configured
     * scorer's {@link MatchScorer#scale()} (D6): a mismatch fails the build
     * rather than misinterpreting a score at {@code resolve()} time.
     */
    public EntityResolverBuilder<S, C> thresholds(DecisionThresholds thresholds) {
        this.thresholds = thresholds;
        return this;
    }

    /**
     * Validates the configuration and builds an immutable {@link
     * EntityResolver}.
     *
     * @throws EntityResolutionConfigurationException naming the field or the
     *     constraint that failed, for every case listed in this class's
     *     Javadoc and in {@code docs/spec/original-design.md} §98
     */
    public EntityResolver<S, C> build() {
        if (fieldSlots.isEmpty()) {
            throw new EntityResolutionConfigurationException("no fields configured: call field(...) at least once");
        }

        Set<String> seenNames = new LinkedHashSet<String>();
        List<FieldDefinition<S, C, ?>> definitions = new ArrayList<FieldDefinition<S, C, ?>>();
        for (FieldSlot<S, C> slot : fieldSlots) {
            String name = slot.getName();
            if (name == null) {
                throw new EntityResolutionConfigurationException("field name must not be null");
            }
            if (!seenNames.add(name)) {
                throw new EntityResolutionConfigurationException("duplicate field name: '" + name + "'");
            }
            List<String> nullArguments = slot.getNullArguments();
            if (!nullArguments.isEmpty()) {
                throw new EntityResolutionConfigurationException(
                        "field '" + name + "': " + join(nullArguments) + " must not be null");
            }
            Integer cost = costOverrides.get(name);
            boolean required = requiredFieldNames.contains(name);
            definitions.add(slot.toFieldDefinition(cost != null ? cost : CostTiers.CHEAP, required));
        }

        for (String name : costOverrides.keySet()) {
            if (!seenNames.contains(name)) {
                throw new EntityResolutionConfigurationException("cost configured for unknown field: '" + name + "'");
            }
        }
        for (String name : requiredFieldNames) {
            if (!seenNames.contains(name)) {
                throw new EntityResolutionConfigurationException(
                        "required configured for unknown field: '" + name + "'");
            }
        }

        if (scorer == null) {
            throw new EntityResolutionConfigurationException("scorer must not be null");
        }
        if (thresholds == null) {
            throw new EntityResolutionConfigurationException("thresholds must not be null");
        }
        if (thresholds.getScale() != scorer.scale()) {
            throw new EntityResolutionConfigurationException(
                    "thresholds scale (" + thresholds.getScale() + ") does not match scorer scale ("
                            + scorer.scale() + ")");
        }
        if (decisionEngine == null) {
            throw new EntityResolutionConfigurationException("decision engine must not be null");
        }

        return new DefaultEntityResolver<S, C>(
                groupByCostAscending(definitions), new ArrayList<CandidateRule<S, C>>(rules), scorer, decisionEngine);
    }

    private static String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private static <S, C> List<List<FieldDefinition<S, C, ?>>> groupByCostAscending(
            List<FieldDefinition<S, C, ?>> definitions) {
        Map<Integer, List<FieldDefinition<S, C, ?>>> byCost = new TreeMap<Integer, List<FieldDefinition<S, C, ?>>>();
        for (FieldDefinition<S, C, ?> definition : definitions) {
            List<FieldDefinition<S, C, ?>> tier = byCost.get(definition.getCost());
            if (tier == null) {
                tier = new ArrayList<FieldDefinition<S, C, ?>>();
                byCost.put(definition.getCost(), tier);
            }
            tier.add(definition);
        }
        return new ArrayList<List<FieldDefinition<S, C, ?>>>(byCost.values());
    }

    /**
     * A field's not-yet-validated configuration, staged between a {@code
     * field(...)} call and {@link #build()} so nothing throws before {@code
     * build()} does.
     */
    private interface FieldSlot<S, C> {
        String getName();

        List<String> getNullArguments();

        FieldDefinition<S, C, ?> toFieldDefinition(int cost, boolean required);
    }

    private static final class TypedFieldSlot<S, C, N> implements FieldSlot<S, C> {

        private final String name;
        private final Function<S, N> sourcePreparer;
        private final Function<C, N> candidatePreparer;
        private final FieldComparator<N> comparator;
        private final List<String> nullArguments;

        TypedFieldSlot(
                String name,
                Function<S, N> sourcePreparer,
                Function<C, N> candidatePreparer,
                FieldComparator<N> comparator,
                List<String> nullArguments) {
            this.name = name;
            this.sourcePreparer = sourcePreparer;
            this.candidatePreparer = candidatePreparer;
            this.comparator = comparator;
            this.nullArguments = nullArguments;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<String> getNullArguments() {
            return nullArguments;
        }

        @Override
        public FieldDefinition<S, C, ?> toFieldDefinition(int cost, boolean required) {
            return new FieldDefinition<S, C, N>(name, sourcePreparer, candidatePreparer, comparator, cost, required);
        }
    }
}
