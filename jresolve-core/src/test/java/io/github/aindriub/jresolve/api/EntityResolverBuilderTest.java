package io.github.aindriub.jresolve.api;

import io.github.aindriub.jresolve.decision.DecisionThresholds;
import io.github.aindriub.jresolve.decision.MatchDecisionEngine;
import io.github.aindriub.jresolve.decision.ThresholdDecisionEngine;
import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.field.DefaultFieldPipeline;
import io.github.aindriub.jresolve.field.ExactFieldComparator;
import io.github.aindriub.jresolve.field.FieldComparator;
import io.github.aindriub.jresolve.field.FieldPipeline;
import io.github.aindriub.jresolve.result.Decision;
import io.github.aindriub.jresolve.result.MatchResult;
import io.github.aindriub.jresolve.result.ScoreScale;
import io.github.aindriub.jresolve.scoring.MatchScorer;
import io.github.aindriub.jresolve.scoring.RuleBasedScorer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityResolverBuilderTest {

    private static final String SENTINEL_RECORD_VALUE = "sentinel-value-9f2c";

    private static final class Source {
        private final String value;

        Source(String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }
    }

    private static final class Candidate {
        private final String value;

        Candidate(String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }
    }

    private static FieldPipeline<String, String> exactStringPipeline() {
        return new DefaultFieldPipeline<>(v -> v, new ExactFieldComparator<>());
    }

    private static RuleBasedScorer pointsScorer() {
        return RuleBasedScorer.builder().weight("value", ComparisonCategory.EXACT, 10.0).build();
    }

    private static DecisionThresholds pointsThresholds() {
        return new DecisionThresholds(5.0, 2.0, 1.0, ScoreScale.POINTS);
    }

    private static EntityResolverBuilder<Source, Candidate> validBuilder() {
        return EntityResolverBuilder.<Source, Candidate>builder()
                .field("value", Source::getValue, Candidate::getValue, exactStringPipeline())
                .scorer(pointsScorer())
                .thresholds(pointsThresholds())
                .decisionEngine(new ThresholdDecisionEngine<>(pointsThresholds()));
    }

    @Test
    void symmetricAndAsymmetricFieldOverloadsBothProduceAWorkingResolver() {
        EntityResolver<Source, Candidate> resolver = EntityResolverBuilder.<Source, Candidate>builder()
                .field("value", Source::getValue, Candidate::getValue,
                        (Function<String, String>) v -> v, (Function<String, String>) v -> v,
                        new ExactFieldComparator<String>())
                .cost("value", 0)
                .required("value")
                .scorer(pointsScorer())
                .thresholds(pointsThresholds())
                .decisionEngine(new ThresholdDecisionEngine<>(pointsThresholds()))
                .build();

        MatchResult<Candidate> result = resolver.resolve(new Source("a"), Arrays.asList(new Candidate("a")));

        assertThat(result.getDecision()).isEqualTo(Decision.MATCH);
    }

    @Test
    void buildThrowsWhenNoFieldsAreConfigured() {
        EntityResolverBuilder<Source, Candidate> builder = EntityResolverBuilder.<Source, Candidate>builder()
                .scorer(pointsScorer())
                .thresholds(pointsThresholds())
                .decisionEngine(new ThresholdDecisionEngine<>(pointsThresholds()));

        assertThatThrownBy(builder::build)
                .isInstanceOf(EntityResolutionConfigurationException.class)
                .hasMessageContaining("no fields configured");
    }

    @Test
    void buildThrowsForADuplicateFieldName() {
        EntityResolverBuilder<Source, Candidate> builder = validBuilder()
                .field("value", Source::getValue, Candidate::getValue, exactStringPipeline());

        assertThatThrownBy(builder::build)
                .isInstanceOf(EntityResolutionConfigurationException.class)
                .hasMessageContaining("duplicate field name")
                .hasMessageContaining("value");
    }

    @Test
    void buildThrowsForANullSourceExtractor() {
        EntityResolverBuilder<Source, Candidate> builder = EntityResolverBuilder.<Source, Candidate>builder()
                .field("value", (Function<Source, String>) null, Candidate::getValue, exactStringPipeline())
                .scorer(pointsScorer())
                .thresholds(pointsThresholds())
                .decisionEngine(new ThresholdDecisionEngine<>(pointsThresholds()));

        assertThatThrownBy(builder::build)
                .isInstanceOf(EntityResolutionConfigurationException.class)
                .hasMessageContaining("value")
                .hasMessageContaining("source extractor");
    }

    @Test
    void buildThrowsForANullPipeline() {
        EntityResolverBuilder<Source, Candidate> builder = EntityResolverBuilder.<Source, Candidate>builder()
                .field("value", Source::getValue, Candidate::getValue, (FieldPipeline<String, String>) null)
                .scorer(pointsScorer())
                .thresholds(pointsThresholds())
                .decisionEngine(new ThresholdDecisionEngine<>(pointsThresholds()));

        assertThatThrownBy(builder::build)
                .isInstanceOf(EntityResolutionConfigurationException.class)
                .hasMessageContaining("value")
                .hasMessageContaining("pipeline");
    }

    @Test
    void buildThrowsForANullComparatorOnTheAsymmetricOverload() {
        EntityResolverBuilder<Source, Candidate> builder = EntityResolverBuilder.<Source, Candidate>builder()
                .field("value", Source::getValue, Candidate::getValue,
                        (Function<String, String>) v -> v, (Function<String, String>) v -> v,
                        (FieldComparator<String>) null)
                .scorer(pointsScorer())
                .thresholds(pointsThresholds())
                .decisionEngine(new ThresholdDecisionEngine<>(pointsThresholds()));

        assertThatThrownBy(builder::build)
                .isInstanceOf(EntityResolutionConfigurationException.class)
                .hasMessageContaining("value")
                .hasMessageContaining("comparator");
    }

    @Test
    void buildThrowsForANullScorer() {
        EntityResolverBuilder<Source, Candidate> builder = EntityResolverBuilder.<Source, Candidate>builder()
                .field("value", Source::getValue, Candidate::getValue, exactStringPipeline())
                .thresholds(pointsThresholds())
                .decisionEngine(new ThresholdDecisionEngine<>(pointsThresholds()));

        assertThatThrownBy(builder::build)
                .isInstanceOf(EntityResolutionConfigurationException.class)
                .hasMessageContaining("scorer");
    }

    @Test
    void buildThrowsForANullDecisionEngine() {
        EntityResolverBuilder<Source, Candidate> builder = EntityResolverBuilder.<Source, Candidate>builder()
                .field("value", Source::getValue, Candidate::getValue, exactStringPipeline())
                .scorer(pointsScorer())
                .thresholds(pointsThresholds());

        assertThatThrownBy(builder::build)
                .isInstanceOf(EntityResolutionConfigurationException.class)
                .hasMessageContaining("decision engine");
    }

    @Test
    void buildThrowsForNullThresholds() {
        EntityResolverBuilder<Source, Candidate> builder = EntityResolverBuilder.<Source, Candidate>builder()
                .field("value", Source::getValue, Candidate::getValue, exactStringPipeline())
                .scorer(pointsScorer())
                .decisionEngine(new ThresholdDecisionEngine<>(pointsThresholds()));

        assertThatThrownBy(builder::build)
                .isInstanceOf(EntityResolutionConfigurationException.class)
                .hasMessageContaining("thresholds");
    }

    @Test
    void buildThrowsForANullRule() {
        EntityResolverBuilder<Source, Candidate> builder = EntityResolverBuilder.<Source, Candidate>builder()
                .field("value", Source::getValue, Candidate::getValue, exactStringPipeline())
                .rule(null)
                .scorer(pointsScorer())
                .thresholds(pointsThresholds())
                .decisionEngine(new ThresholdDecisionEngine<>(pointsThresholds()));

        assertThatThrownBy(builder::build)
                .isInstanceOf(EntityResolutionConfigurationException.class)
                .hasMessageContaining("rule");
    }

    @Test
    void buildThrowsWhenThresholdsScaleDoesNotMatchScorerScale() {
        MatchScorer scorer = pointsScorer(); // ScoreScale.POINTS
        DecisionThresholds mismatched = new DecisionThresholds(0.5, 0.2, 0.05, ScoreScale.PROBABILITY);
        EntityResolverBuilder<Source, Candidate> builder = EntityResolverBuilder.<Source, Candidate>builder()
                .field("value", Source::getValue, Candidate::getValue, exactStringPipeline())
                .scorer(scorer)
                .thresholds(mismatched)
                .decisionEngine(new ThresholdDecisionEngine<>(pointsThresholds()));

        assertThatThrownBy(builder::build)
                .isInstanceOf(EntityResolutionConfigurationException.class)
                .hasMessageContaining("scale");
    }

    @Test
    void exceptionExtendsRuntimeExceptionAndLivesInThisPackage() {
        EntityResolutionConfigurationException exception = new EntityResolutionConfigurationException("x");

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(EntityResolutionConfigurationException.class.getPackage().getName())
                .isEqualTo("io.github.aindriub.jresolve.api");
    }

    @Test
    void noBuildFailureMessageContainsARecordValue() {
        // A Source/Candidate carrying the sentinel exists, but build() never
        // receives either instance — resolve() is never called — so if the
        // sentinel shows up in a build() failure message, something is
        // routing record data into configuration errors that should only
        // ever name fields and constraints.
        Source source = new Source(SENTINEL_RECORD_VALUE);
        Candidate candidate = new Candidate(SENTINEL_RECORD_VALUE);
        assertThat(source.getValue()).isEqualTo(SENTINEL_RECORD_VALUE);
        assertThat(candidate.getValue()).isEqualTo(SENTINEL_RECORD_VALUE);

        String[] messages = new String[] {
                catchMessage(() -> EntityResolverBuilder.<Source, Candidate>builder().build()),
                catchMessage(() -> validBuilder()
                        .field("value", Source::getValue, Candidate::getValue, exactStringPipeline())
                        .build()),
                catchMessage(() -> EntityResolverBuilder.<Source, Candidate>builder()
                        .field("value", Source::getValue, Candidate::getValue, exactStringPipeline())
                        .thresholds(pointsThresholds())
                        .decisionEngine(new ThresholdDecisionEngine<>(pointsThresholds()))
                        .build()),
        };

        for (String message : messages) {
            assertThat(message).doesNotContain(SENTINEL_RECORD_VALUE);
        }
    }

    private static String catchMessage(Runnable action) {
        try {
            action.run();
        } catch (EntityResolutionConfigurationException e) {
            return e.getMessage();
        }
        throw new AssertionError("expected build() to throw EntityResolutionConfigurationException");
    }

    // --- D6: build() inspects the engine that actually decides -------------

    @Test
    void buildRejectsAnEngineApplyingThresholdsOnADifferentScale() {
        // The case D6 reproduced: the engine would compare a POINTS score
        // against a probability threshold of 0.9 and call it a match.
        EntityResolverBuilder<Source, Candidate> builder = EntityResolverBuilder.<Source, Candidate>builder()
                .field("value", Source::getValue, Candidate::getValue, exactStringPipeline())
                .scorer(pointsScorer())
                .thresholds(pointsThresholds())
                .decisionEngine(new ThresholdDecisionEngine<Candidate>(
                        new DecisionThresholds(0.9, 0.5, 0.2, ScoreScale.PROBABILITY)));

        assertThatThrownBy(builder::build)
                .isInstanceOf(EntityResolutionConfigurationException.class)
                .hasMessageContaining("scale");
    }

    @Test
    void buildRejectsAnEngineApplyingDifferentThresholdsOnTheSameScale() {
        // A scale check alone would pass this: both are POINTS. The
        // divergence is in the values, which is why the engine declares the
        // thresholds rather than only their scale.
        EntityResolverBuilder<Source, Candidate> builder = EntityResolverBuilder.<Source, Candidate>builder()
                .field("value", Source::getValue, Candidate::getValue, exactStringPipeline())
                .scorer(pointsScorer())
                .thresholds(new DecisionThresholds(200.0, 20.0, 10.0, ScoreScale.POINTS))
                .decisionEngine(new ThresholdDecisionEngine<Candidate>(
                        new DecisionThresholds(50.0, 20.0, 10.0, ScoreScale.POINTS)));

        assertThatThrownBy(builder::build)
                .isInstanceOf(EntityResolutionConfigurationException.class)
                .hasMessageContaining("different");
    }

    @Test
    void theRejectionMessageNamesTheConstraintAndNotAValue() {
        EntityResolverBuilder<Source, Candidate> builder = EntityResolverBuilder.<Source, Candidate>builder()
                .field("value", Source::getValue, Candidate::getValue, exactStringPipeline())
                .scorer(pointsScorer())
                .thresholds(new DecisionThresholds(987.65, 20.0, 10.0, ScoreScale.POINTS))
                .decisionEngine(new ThresholdDecisionEngine<Candidate>(
                        new DecisionThresholds(50.0, 20.0, 10.0, ScoreScale.POINTS)));

        assertThatThrownBy(builder::build).hasMessageNotContaining("987.65");
    }

    @Test
    void buildAcceptsEqualButSeparatelyConstructedThresholds() {
        // Two instances, same values as pointsThresholds(), so the resolve
        // below genuinely clears the match threshold. The point of the test
        // is that build() does not reject them for being separate objects.
        EntityResolver<Source, Candidate> resolver = EntityResolverBuilder.<Source, Candidate>builder()
                .field("value", Source::getValue, Candidate::getValue, exactStringPipeline())
                .scorer(pointsScorer())
                .thresholds(new DecisionThresholds(5.0, 2.0, 1.0, ScoreScale.POINTS))
                .decisionEngine(new ThresholdDecisionEngine<Candidate>(
                        new DecisionThresholds(5.0, 2.0, 1.0, ScoreScale.POINTS)))
                .build();

        assertThat(resolver.resolve(new Source("a"), Arrays.asList(new Candidate("a"))).getDecision())
                .isEqualTo(Decision.MATCH);
    }

    @Test
    void anEngineThatDeclaresNothingStillBuildsAndResolves() {
        // A custom engine need not decide by thresholds at all. The check
        // must narrow what is buildable only where it can actually verify
        // something — otherwise it would make a legitimate engine unusable.
        MatchDecisionEngine<Candidate> alwaysNoMatch = new MatchDecisionEngine<Candidate>() {
            @Override
            public MatchResult<Candidate> decide(java.util.List<io.github.aindriub.jresolve.result.ScoredCandidate<Candidate>> candidates) {
                return new MatchResult<Candidate>(Decision.NO_MATCH, null, null, null, candidates);
            }
        };

        EntityResolver<Source, Candidate> resolver = EntityResolverBuilder.<Source, Candidate>builder()
                .field("value", Source::getValue, Candidate::getValue, exactStringPipeline())
                .scorer(pointsScorer())
                .thresholds(pointsThresholds())
                .decisionEngine(alwaysNoMatch)
                .build();

        assertThat(alwaysNoMatch.declaredThresholds()).isNull();
        assertThat(resolver.resolve(new Source("a"), Arrays.asList(new Candidate("a"))).getDecision())
                .isEqualTo(Decision.NO_MATCH);
    }
}
