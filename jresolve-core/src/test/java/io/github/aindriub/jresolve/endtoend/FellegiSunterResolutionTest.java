package io.github.aindriub.jresolve.endtoend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import io.github.aindriub.jresolve.api.CandidateRule;
import io.github.aindriub.jresolve.api.EntityResolutionConfigurationException;
import io.github.aindriub.jresolve.api.EntityResolver;
import io.github.aindriub.jresolve.api.EntityResolverBuilder;
import io.github.aindriub.jresolve.api.RuleDecision;
import io.github.aindriub.jresolve.decision.DecisionThresholds;
import io.github.aindriub.jresolve.decision.ThresholdDecisionEngine;
import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.field.CostTiers;
import io.github.aindriub.jresolve.field.DefaultFieldPipeline;
import io.github.aindriub.jresolve.field.ExactFieldComparator;
import io.github.aindriub.jresolve.field.FieldPipeline;
import io.github.aindriub.jresolve.result.Decision;
import io.github.aindriub.jresolve.result.MatchResult;
import io.github.aindriub.jresolve.result.ScoreScale;
import io.github.aindriub.jresolve.result.ScoredCandidate;
import io.github.aindriub.jresolve.scoring.DefaultFellegiSunterModel;
import io.github.aindriub.jresolve.scoring.FellegiSunterScorer;
import io.github.aindriub.jresolve.scoring.TermFrequencyTable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * The probabilistic path through a whole resolver, from a consumer's seat.
 *
 * <p>Every expected weight below is hand-derived from the configured
 * {@code m} and {@code u}. The probabilities are chosen so the arithmetic is
 * exact:
 *
 * <pre>
 *   code EXACT:    m = 0.8, u = 0.1  -> m/u = 8     -> log2 = +3.0
 *   code CONFLICT: m = 0.1, u = 0.8  -> m/u = 0.125 -> log2 = -3.0
 *   tier EXACT:    m = 0.5, u = 0.25 -> m/u = 2     -> log2 = +1.0
 *   tier CONFLICT: m = 0.2, u = 0.8  -> m/u = 0.25  -> log2 = -2.0
 * </pre>
 *
 * <p>Nothing here is read off a run. Fixtures are two neutral record types
 * declared below; the domain-shaped fixtures in this package belong to the
 * rule-based suite and would name a person if imported here.
 */
class FellegiSunterResolutionTest {

    // --- Fixtures -----------------------------------------------------------

    private static final class SourceRecord {
        private final String code;
        private final String tier;

        SourceRecord(String code, String tier) {
            this.code = code;
            this.tier = tier;
        }

        String getCode() {
            return code;
        }

        String getTier() {
            return tier;
        }
    }

    private static final class CandidateRecord {
        private final String id;
        private final String code;
        private final String tier;

        CandidateRecord(String id, String code, String tier) {
            this.id = id;
            this.code = code;
            this.tier = tier;
        }

        String getId() {
            return id;
        }

        String getCode() {
            return code;
        }

        String getTier() {
            return tier;
        }

        @Override
        public String toString() {
            return "CandidateRecord{" + id + '}';
        }
    }

    // --- Configuration ------------------------------------------------------

    private static FieldPipeline<String, String> exactStrings() {
        return new DefaultFieldPipeline<>(value -> value, new ExactFieldComparator<String>());
    }

    private static DefaultFellegiSunterModel.Builder model() {
        return DefaultFellegiSunterModel.builder()
                .probabilities("code", ComparisonCategory.EXACT, 0.8, 0.1)
                .probabilities("code", ComparisonCategory.CONFLICT, 0.1, 0.8)
                .probabilities("tier", ComparisonCategory.EXACT, 0.5, 0.25)
                .probabilities("tier", ComparisonCategory.CONFLICT, 0.2, 0.8);
    }

    /** "alpha" nine times, "zulu" once, for the code field. */
    private static TermFrequencyTable skewedCorpus() {
        TermFrequencyTable.Builder builder = TermFrequencyTable.builder();
        for (int i = 0; i < 9; i++) {
            builder.observe("code", "alpha");
        }
        builder.observe("code", "zulu");
        return builder.build();
    }

    /**
     * Thresholds on the scorer's own scale. A match needs +3.5 bits, review
     * starts at 0.0, and two candidates within 1.0 bit of each other are too
     * close to separate.
     */
    private static DecisionThresholds thresholds() {
        return new DecisionThresholds(3.5, 0.0, 1.0, ScoreScale.LOG2_LIKELIHOOD_RATIO);
    }

    private static EntityResolverBuilder<SourceRecord, CandidateRecord> builder(
            java.util.function.Function<CandidateRecord, String> tierExtractor) {
        return EntityResolverBuilder.<SourceRecord, CandidateRecord>builder()
                .field("code", SourceRecord::getCode, CandidateRecord::getCode, exactStrings())
                .cost("code", CostTiers.CHEAP)
                .field("tier", SourceRecord::getTier, tierExtractor, exactStrings())
                .cost("tier", CostTiers.EXPENSIVE);
    }

    private static EntityResolver<SourceRecord, CandidateRecord> resolverWith(
            DefaultFellegiSunterModel model) {
        DecisionThresholds shared = thresholds();
        return builder(CandidateRecord::getTier)
                .scorer(new FellegiSunterScorer(model))
                .thresholds(shared)
                .decisionEngine(new ThresholdDecisionEngine<CandidateRecord>(shared))
                .build();
    }

    private static List<String> rankedIds(MatchResult<CandidateRecord> result) {
        List<String> ids = new ArrayList<>();
        for (ScoredCandidate<CandidateRecord> scored : result.getCandidates()) {
            ids.add(scored.getCandidate().getId());
        }
        return ids;
    }

    // --- The positive case --------------------------------------------------

    @Test
    void agreementOnBothFieldsResolvesToAMatch() {
        // code EXACT (+3.0) + tier EXACT (+1.0) = +4.0, above the 3.5 match
        // threshold, with no second candidate to leave a margin short.
        MatchResult<CandidateRecord> result = resolverWith(model().build()).resolve(
                new SourceRecord("zulu", "gold"),
                Collections.singletonList(new CandidateRecord("c-1", "zulu", "gold")));

        assertThat(result.getDecision()).isSameAs(Decision.MATCH);
        assertThat(result.getMatch().getId()).isEqualTo("c-1");
        assertThat(result.getScore().getValue()).isEqualTo(4.0, within(1e-9));
        assertThat(result.getScore().getScale()).isSameAs(ScoreScale.LOG2_LIKELIHOOD_RATIO);
    }

    @Test
    void disagreementOnBothFieldsResolvesToNoMatch() {
        // code CONFLICT (-3.0) + tier CONFLICT (-2.0) = -5.0, below the 0.0
        // review threshold.
        MatchResult<CandidateRecord> result = resolverWith(model().build()).resolve(
                new SourceRecord("zulu", "gold"),
                Collections.singletonList(new CandidateRecord("c-2", "alpha", "silver")));

        assertThat(result.getDecision()).isSameAs(Decision.NO_MATCH);
        assertThat(result.getMatch()).isNull();
    }

    @Test
    void twoIndistinguishableCandidatesAreReviewed() {
        // Both score +4.0, so the margin is 0.0 — short of the 1.0 minimum.
        MatchResult<CandidateRecord> result = resolverWith(model().build()).resolve(
                new SourceRecord("zulu", "gold"),
                Arrays.asList(new CandidateRecord("c-3", "zulu", "gold"),
                        new CandidateRecord("c-4", "zulu", "gold")));

        assertThat(result.getDecision()).isSameAs(Decision.REVIEW);
        assertThat(result.getMargin()).isEqualTo(0.0, within(1e-9));
    }

    // --- D5's headline claim, end to end ------------------------------------

    @Test
    void agreementOnARareValueOutscoresAgreementOnACommonOne() {
        // Two resolves rather than one ranking, necessarily: agreement means
        // the candidate matches the source, so a common agreement and a rare
        // one cannot share a source record. The claim is the comparison
        // between them, and both halves are here so neither can drift.
        //
        //   rare:   code u = 0.1 -> log2(0.8/0.1) = +3.0, plus tier +1.0 = +4.0
        //   common: code u = 0.9 -> log2(0.8/0.9) is negative, so the total
        //           falls below the +4.0 above. Agreeing on a value nine
        //           records in ten share is barely evidence at all.
        EntityResolver<SourceRecord, CandidateRecord> resolver =
                resolverWith(model().frequencies(skewedCorpus()).build());

        double rare = resolver.resolve(new SourceRecord("zulu", "gold"),
                Collections.singletonList(new CandidateRecord("c-5", "zulu", "gold")))
                .getScore().getValue();
        double common = resolver.resolve(new SourceRecord("alpha", "gold"),
                Collections.singletonList(new CandidateRecord("c-6", "alpha", "gold")))
                .getScore().getValue();

        assertThat(rare).isEqualTo(4.0, within(1e-9));
        assertThat(rare).isGreaterThan(common);
    }

    @Test
    void withoutACorpusTheTwoAgreementsScoreIdentically() {
        // The control. Without it the assertion above cannot distinguish
        // "the frequency adjustment works" from "these two records differ for
        // some other reason".
        EntityResolver<SourceRecord, CandidateRecord> resolver = resolverWith(model().build());

        double rare = resolver.resolve(new SourceRecord("zulu", "gold"),
                Collections.singletonList(new CandidateRecord("c-5", "zulu", "gold")))
                .getScore().getValue();
        double common = resolver.resolve(new SourceRecord("alpha", "gold"),
                Collections.singletonList(new CandidateRecord("c-6", "alpha", "gold")))
                .getScore().getValue();

        assertThat(rare).isEqualTo(common);
    }

    @Test
    void aCommonAgreementCanFallBelowTheMatchThreshold() {
        // What the adjustment buys a consumer, stated as an outcome rather
        // than a number: the same shape of agreement that matches on a rare
        // value does not match on a near-universal one.
        EntityResolver<SourceRecord, CandidateRecord> resolver =
                resolverWith(model().frequencies(skewedCorpus()).build());

        MatchResult<CandidateRecord> common = resolver.resolve(
                new SourceRecord("alpha", "gold"),
                Collections.singletonList(new CandidateRecord("c-7", "alpha", "gold")));

        assertThat(common.getScore().getValue()).isLessThan(3.5);
        assertThat(common.getDecision()).isNotSameAs(Decision.MATCH);
    }

    // --- D5's probability rule, end to end ----------------------------------

    @Test
    void aProbabilityAppearsOnlyWhenTheModelCarriesPriorOdds() {
        // Both halves in one test so the pair cannot drift.
        // With prior odds 0.25 and W = +4.0:
        //   posterior odds = 0.25 x 2^4 = 4.0
        //   probability    = 4.0 / (1 + 4.0) = 0.8
        SourceRecord source = new SourceRecord("zulu", "gold");
        List<CandidateRecord> candidates =
                Collections.singletonList(new CandidateRecord("c-8", "zulu", "gold"));

        MatchResult<CandidateRecord> withoutPrior =
                resolverWith(model().build()).resolve(source, candidates);
        MatchResult<CandidateRecord> withPrior =
                resolverWith(model().priorOdds(0.25).build()).resolve(source, candidates);

        assertThat(withoutPrior.getScore().getProbability()).isNull();
        assertThat(withPrior.getScore().getProbability()).isEqualTo(0.8, within(1e-9));
        assertThat(withoutPrior.getScore().getValue())
                .isEqualTo(withPrior.getScore().getValue(), within(1e-9));
    }

    // --- Task 17's guard, on a second scale ---------------------------------

    @Test
    void thresholdsOnTheWrongScaleFailAtBuild() {
        // Until this scorer existed the library had one scale, so the D6
        // check was guarding a case that could not arise. This is the first
        // time it does anything a single-scale library would not.
        DecisionThresholds points = new DecisionThresholds(50.0, 20.0, 10.0, ScoreScale.POINTS);

        assertThatThrownBy(() -> builder(CandidateRecord::getTier)
                .scorer(new FellegiSunterScorer(model().build()))
                .thresholds(points)
                .decisionEngine(new ThresholdDecisionEngine<CandidateRecord>(points))
                .build())
                .isInstanceOf(EntityResolutionConfigurationException.class)
                .hasMessageContaining("scale");
    }

    // --- The cost-tier veto -------------------------------------------------

    @Test
    void aVetoedCandidateIsDroppedBeforeTheExpensiveFieldIsPrepared() {
        // Note what this does *not* assert. DefaultEntityResolver:110 always
        // builds the scorer's evidence with complete = true, and a rule's
        // REJECT returns null at :101 rather than scoring what was gathered.
        // So the resolver never hands a scorer incomplete evidence, and
        // FellegiSunterScorer's unscorable path is unreachable from here —
        // it is covered by that scorer's own unit test instead. What
        // MatchEvidence.isComplete() actually feeds is the rule below, which
        // sees partial evidence between tiers at :102.
        AtomicInteger tierReads = new AtomicInteger();
        CandidateRule<SourceRecord, CandidateRecord> rejectOnCodeConflict =
                (source, candidate, evidence) -> {
                    if (evidence.getField("code") != null
                            && evidence.getField("code").getCategory()
                                    == ComparisonCategory.CONFLICT) {
                        return RuleDecision.REJECT;
                    }
                    return RuleDecision.CONTINUE;
                };
        DecisionThresholds shared = thresholds();
        EntityResolver<SourceRecord, CandidateRecord> resolver = builder(candidate -> {
                    tierReads.incrementAndGet();
                    return candidate.getTier();
                })
                .rule(rejectOnCodeConflict)
                .scorer(new FellegiSunterScorer(model().build()))
                .thresholds(shared)
                .decisionEngine(new ThresholdDecisionEngine<CandidateRecord>(shared))
                .build();

        MatchResult<CandidateRecord> result = resolver.resolve(
                new SourceRecord("zulu", "gold"),
                Collections.singletonList(new CandidateRecord("c-9", "alpha", "gold")));

        assertThat(result.getDecision()).isSameAs(Decision.NO_MATCH);
        assertThat(result.getCandidates()).isEmpty();
        assertThat(tierReads.get()).isZero();
    }

    @Test
    void aRuleSeesPartialEvidenceBetweenTiers() {
        // The consumer of MatchEvidence.isComplete() that does exist.
        List<Boolean> completenessSeen = new ArrayList<>();
        CandidateRule<SourceRecord, CandidateRecord> observer =
                (source, candidate, evidence) -> {
                    completenessSeen.add(evidence.isComplete());
                    return RuleDecision.CONTINUE;
                };
        DecisionThresholds shared = thresholds();
        EntityResolver<SourceRecord, CandidateRecord> resolver = builder(CandidateRecord::getTier)
                .rule(observer)
                .scorer(new FellegiSunterScorer(model().build()))
                .thresholds(shared)
                .decisionEngine(new ThresholdDecisionEngine<CandidateRecord>(shared))
                .build();

        resolver.resolve(new SourceRecord("zulu", "gold"),
                Collections.singletonList(new CandidateRecord("c-10", "zulu", "gold")));

        // Two tiers, so the rule runs twice: once with only the cheap field
        // gathered, once with everything.
        assertThat(completenessSeen).containsExactly(false, true);
    }

    // --- Determinism --------------------------------------------------------

    @Test
    void theRankingIsTheSameUnderEveryCandidateOrder() {
        EntityResolver<SourceRecord, CandidateRecord> resolver = resolverWith(model().build());
        SourceRecord source = new SourceRecord("zulu", "gold");
        CandidateRecord best = new CandidateRecord("c-best", "zulu", "gold");
        CandidateRecord middle = new CandidateRecord("c-middle", "zulu", "silver");
        CandidateRecord worst = new CandidateRecord("c-worst", "alpha", "silver");

        List<String> forward = rankedIds(resolver.resolve(source, Arrays.asList(best, middle, worst)));
        List<String> reversed = rankedIds(resolver.resolve(source, Arrays.asList(worst, middle, best)));

        assertThat(reversed).isEqualTo(forward);
        assertThat(forward.get(0)).isEqualTo("c-best");
    }

    @Test
    void resolvingTwiceProducesTheSameScore() {
        EntityResolver<SourceRecord, CandidateRecord> resolver = resolverWith(model().build());
        SourceRecord source = new SourceRecord("zulu", "gold");
        List<CandidateRecord> candidates =
                Collections.singletonList(new CandidateRecord("c-11", "zulu", "gold"));

        assertThat(resolver.resolve(source, candidates).getScore().getValue())
                .isEqualTo(resolver.resolve(source, candidates).getScore().getValue());
    }
}
