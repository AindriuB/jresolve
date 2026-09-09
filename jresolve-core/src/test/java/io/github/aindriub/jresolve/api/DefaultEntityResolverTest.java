package io.github.aindriub.jresolve.api;

import io.github.aindriub.jresolve.decision.DecisionThresholds;
import io.github.aindriub.jresolve.decision.ThresholdDecisionEngine;
import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.MatchEvidence;
import io.github.aindriub.jresolve.field.CostTiers;
import io.github.aindriub.jresolve.field.DefaultFieldPipeline;
import io.github.aindriub.jresolve.field.ExactFieldComparator;
import io.github.aindriub.jresolve.field.FieldPipeline;
import io.github.aindriub.jresolve.result.Decision;
import io.github.aindriub.jresolve.result.MatchResult;
import io.github.aindriub.jresolve.result.ScoreScale;
import io.github.aindriub.jresolve.scoring.RuleBasedScorer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultEntityResolverTest {

    // A source and candidate carrying two independent string fields, one
    // deliberately declared CHEAP and one EXPENSIVE, so tiering and
    // short-circuiting have something to bite on.
    private static final class TwoFieldSource {
        private final String cheapValue;
        private final String expensiveValue;

        TwoFieldSource(String cheapValue, String expensiveValue) {
            this.cheapValue = cheapValue;
            this.expensiveValue = expensiveValue;
        }

        String getCheapValue() {
            return cheapValue;
        }

        String getExpensiveValue() {
            return expensiveValue;
        }
    }

    private static final class TwoFieldCandidate {
        private final String cheapValue;
        private final String expensiveValue;

        TwoFieldCandidate(String cheapValue, String expensiveValue) {
            this.cheapValue = cheapValue;
            this.expensiveValue = expensiveValue;
        }

        String getCheapValue() {
            return cheapValue;
        }

        String getExpensiveValue() {
            return expensiveValue;
        }
    }

    private static FieldPipeline<String, String> exactStringPipeline() {
        return new DefaultFieldPipeline<>(v -> v, new ExactFieldComparator<>());
    }

    private static RuleBasedScorer basicScorer() {
        return RuleBasedScorer.builder()
                .weight("cheap", ComparisonCategory.EXACT, 10.0)
                .weight("expensive", ComparisonCategory.EXACT, 10.0)
                .baseScore(0.0)
                .build();
    }

    private static DecisionThresholds basicThresholds() {
        return new DecisionThresholds(5.0, 2.0, 1.0, ScoreScale.POINTS);
    }

    @Test
    void sourceIsPreparedExactlyOnceAcrossFiftyCandidates() {
        AtomicInteger sourceInvocations = new AtomicInteger();
        AtomicInteger candidateInvocations = new AtomicInteger();
        Function<TwoFieldSource, String> countingSourceGetter = s -> {
            sourceInvocations.incrementAndGet();
            return s.getCheapValue();
        };
        Function<TwoFieldCandidate, String> countingCandidateGetter = c -> {
            candidateInvocations.incrementAndGet();
            return c.getCheapValue();
        };

        EntityResolver<TwoFieldSource, TwoFieldCandidate> resolver = EntityResolverBuilder
                .<TwoFieldSource, TwoFieldCandidate>builder()
                .field("cheap", countingSourceGetter, countingCandidateGetter, exactStringPipeline())
                .scorer(basicScorer())
                .thresholds(basicThresholds())
                .decisionEngine(new ThresholdDecisionEngine<>(basicThresholds()))
                .build();

        List<TwoFieldCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            candidates.add(new TwoFieldCandidate("value-" + i, "x"));
        }

        resolver.resolve(new TwoFieldSource("value-0", "x"), candidates);

        // D2: the source is prepared once per resolve() call, not once per
        // candidate — 50 candidates must not mean 50 source-side calls.
        assertThat(sourceInvocations.get()).isEqualTo(1);
        assertThat(candidateInvocations.get()).isEqualTo(50);
    }

    @Test
    void rejectingRuleAtCheapTierPreventsTheExpensivePreparerFromRunning() {
        AtomicInteger expensiveCandidateInvocations = new AtomicInteger();
        Function<TwoFieldCandidate, String> countingExpensiveCandidateGetter = c -> {
            expensiveCandidateInvocations.incrementAndGet();
            return c.getExpensiveValue();
        };

        CandidateRule<TwoFieldSource, TwoFieldCandidate> rejectAfterCheapTier =
                (source, candidate, evidence) -> RuleDecision.REJECT;

        EntityResolver<TwoFieldSource, TwoFieldCandidate> resolver = EntityResolverBuilder
                .<TwoFieldSource, TwoFieldCandidate>builder()
                .field("cheap", TwoFieldSource::getCheapValue, TwoFieldCandidate::getCheapValue, exactStringPipeline())
                .cost("cheap", CostTiers.CHEAP)
                .field("expensive", TwoFieldSource::getExpensiveValue, countingExpensiveCandidateGetter,
                        exactStringPipeline())
                .cost("expensive", CostTiers.EXPENSIVE)
                .rule(rejectAfterCheapTier)
                .scorer(basicScorer())
                .thresholds(basicThresholds())
                .decisionEngine(new ThresholdDecisionEngine<>(basicThresholds()))
                .build();

        resolver.resolve(new TwoFieldSource("a", "b"), Arrays.asList(new TwoFieldCandidate("a", "b")));

        assertThat(expensiveCandidateInvocations.get()).isZero();
    }

    @Test
    void evidenceIsIncompleteAfterAnEarlierTierAndCompleteAfterTheLastTier() {
        List<MatchEvidence> captured = new ArrayList<>();
        CandidateRule<TwoFieldSource, TwoFieldCandidate> capturingRule = (source, candidate, evidence) -> {
            captured.add(evidence);
            return RuleDecision.CONTINUE;
        };

        EntityResolver<TwoFieldSource, TwoFieldCandidate> resolver = EntityResolverBuilder
                .<TwoFieldSource, TwoFieldCandidate>builder()
                .field("cheap", TwoFieldSource::getCheapValue, TwoFieldCandidate::getCheapValue, exactStringPipeline())
                .cost("cheap", CostTiers.CHEAP)
                .field("expensive", TwoFieldSource::getExpensiveValue, TwoFieldCandidate::getExpensiveValue,
                        exactStringPipeline())
                .cost("expensive", CostTiers.EXPENSIVE)
                .rule(capturingRule)
                .scorer(basicScorer())
                .thresholds(basicThresholds())
                .decisionEngine(new ThresholdDecisionEngine<>(basicThresholds()))
                .build();

        resolver.resolve(new TwoFieldSource("a", "b"), Arrays.asList(new TwoFieldCandidate("a", "b")));

        assertThat(captured).hasSize(2);
        assertThat(captured.get(0).isComplete()).isFalse();
        assertThat(captured.get(1).isComplete()).isTrue();
    }

    @Test
    void vetoedAndUnscorableCandidatesAreExcludedFromTheRankedList() {
        CandidateRule<TwoFieldSource, TwoFieldCandidate> vetoMarked =
                (source, candidate, evidence) -> "veto".equals(candidate.getExpensiveValue())
                        ? RuleDecision.REJECT
                        : RuleDecision.CONTINUE;

        RuleBasedScorer requiringCheap = RuleBasedScorer.builder()
                .requiredField("cheap")
                .weight("cheap", ComparisonCategory.EXACT, 10.0)
                .weight("expensive", ComparisonCategory.EXACT, 10.0)
                .baseScore(0.0)
                .build();

        EntityResolver<TwoFieldSource, TwoFieldCandidate> resolver = EntityResolverBuilder
                .<TwoFieldSource, TwoFieldCandidate>builder()
                .field("cheap", TwoFieldSource::getCheapValue, TwoFieldCandidate::getCheapValue, exactStringPipeline())
                .cost("cheap", CostTiers.CHEAP)
                .field("expensive", TwoFieldSource::getExpensiveValue, TwoFieldCandidate::getExpensiveValue,
                        exactStringPipeline())
                .cost("expensive", CostTiers.EXPENSIVE)
                .rule(vetoMarked)
                .scorer(requiringCheap)
                .thresholds(new DecisionThresholds(5.0, 2.0, 1.0, ScoreScale.POINTS))
                .decisionEngine(new ThresholdDecisionEngine<>(new DecisionThresholds(5.0, 2.0, 1.0, ScoreScale.POINTS)))
                .build();

        TwoFieldCandidate scorable = new TwoFieldCandidate("a", "b");
        TwoFieldCandidate vetoed = new TwoFieldCandidate("a", "veto");
        TwoFieldCandidate unscorable = new TwoFieldCandidate(null, "b");

        MatchResult<TwoFieldCandidate> result = resolver.resolve(
                new TwoFieldSource("a", "b"), Arrays.asList(scorable, vetoed, unscorable));

        assertThat(result.getCandidates()).hasSize(1);
        assertThat(result.getCandidates().get(0).getCandidate()).isSameAs(scorable);
    }

    @Test
    void resolveWhereEveryCandidateIsVetoedIsNoMatchWithNullMatch() {
        CandidateRule<TwoFieldSource, TwoFieldCandidate> rejectAll =
                (source, candidate, evidence) -> RuleDecision.REJECT;

        EntityResolver<TwoFieldSource, TwoFieldCandidate> resolver = EntityResolverBuilder
                .<TwoFieldSource, TwoFieldCandidate>builder()
                .field("cheap", TwoFieldSource::getCheapValue, TwoFieldCandidate::getCheapValue, exactStringPipeline())
                .rule(rejectAll)
                .scorer(basicScorer())
                .thresholds(basicThresholds())
                .decisionEngine(new ThresholdDecisionEngine<>(basicThresholds()))
                .build();

        MatchResult<TwoFieldCandidate> result = resolver.resolve(new TwoFieldSource("a", "b"),
                Arrays.asList(new TwoFieldCandidate("a", "b"), new TwoFieldCandidate("a", "b")));

        assertThat(result.getDecision()).isEqualTo(Decision.NO_MATCH);
        assertThat(result.getMatch()).isNull();
        assertThat(result.getCandidates()).isEmpty();
    }

    @Test
    void nullSourceThrowsIllegalArgumentExceptionWithoutANullPointerException() {
        EntityResolver<TwoFieldSource, TwoFieldCandidate> resolver = simpleResolver();

        assertThatThrownBy(() -> resolver.resolve(null, Arrays.asList(new TwoFieldCandidate("a", "b"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullCandidateCollectionThrowsIllegalArgumentExceptionWithoutANullPointerException() {
        EntityResolver<TwoFieldSource, TwoFieldCandidate> resolver = simpleResolver();

        assertThatThrownBy(() -> resolver.resolve(new TwoFieldSource("a", "b"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullElementInsideTheCandidateCollectionIsSkippedRatherThanThrowing() {
        EntityResolver<TwoFieldSource, TwoFieldCandidate> resolver = simpleResolver();
        TwoFieldCandidate match = new TwoFieldCandidate("a", "b");

        MatchResult<TwoFieldCandidate> result = resolver.resolve(
                new TwoFieldSource("a", "b"), Arrays.asList(match, null));

        assertThat(result.getDecision()).isEqualTo(Decision.MATCH);
        assertThat(result.getMatch()).isSameAs(match);
    }

    @Test
    void concurrentResolveCallsFromMultipleThreadsMatchTheSingleThreadedResult() throws InterruptedException, ExecutionException {
        EntityResolver<TwoFieldSource, TwoFieldCandidate> resolver = simpleResolver();
        List<TwoFieldCandidate> candidates = Arrays.asList(
                new TwoFieldCandidate("a", "b"), new TwoFieldCandidate("a", "c"));

        int threadCount = 8;
        List<TwoFieldSource> sources = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            sources.add(new TwoFieldSource("a", "b"));
        }
        List<Decision> expected = new ArrayList<>();
        List<TwoFieldCandidate> expectedMatches = new ArrayList<>();
        for (TwoFieldSource source : sources) {
            MatchResult<TwoFieldCandidate> single = resolver.resolve(source, candidates);
            expected.add(single.getDecision());
            expectedMatches.add(single.getMatch());
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Callable<MatchResult<TwoFieldCandidate>>> tasks = new ArrayList<>();
            for (TwoFieldSource source : sources) {
                tasks.add(() -> resolver.resolve(source, candidates));
            }
            List<Future<MatchResult<TwoFieldCandidate>>> futures = executor.invokeAll(tasks);
            for (int i = 0; i < futures.size(); i++) {
                MatchResult<TwoFieldCandidate> result = futures.get(i).get();
                assertThat(result.getDecision()).isEqualTo(expected.get(i));
                assertThat(result.getMatch()).isEqualTo(expectedMatches.get(i));
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private static EntityResolver<TwoFieldSource, TwoFieldCandidate> simpleResolver() {
        return EntityResolverBuilder
                .<TwoFieldSource, TwoFieldCandidate>builder()
                .field("cheap", TwoFieldSource::getCheapValue, TwoFieldCandidate::getCheapValue, exactStringPipeline())
                .field("expensive", TwoFieldSource::getExpensiveValue, TwoFieldCandidate::getExpensiveValue,
                        exactStringPipeline())
                .scorer(basicScorer())
                .thresholds(basicThresholds())
                .decisionEngine(new ThresholdDecisionEngine<>(basicThresholds()))
                .build();
    }
}
