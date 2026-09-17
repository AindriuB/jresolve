package io.github.aindriub.jresolve.readme;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aindriub.jresolve.api.CandidateRule;
import io.github.aindriub.jresolve.api.EntityResolver;
import io.github.aindriub.jresolve.api.EntityResolverBuilder;
import io.github.aindriub.jresolve.api.RuleDecision;
import io.github.aindriub.jresolve.comparison.JaroWinklerSimilarity;
import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.field.CostTiers;
import io.github.aindriub.jresolve.field.DefaultFieldPipeline;
import io.github.aindriub.jresolve.field.ExactFieldComparator;
import io.github.aindriub.jresolve.field.FieldPipeline;
import io.github.aindriub.jresolve.field.SimilarityBands;
import io.github.aindriub.jresolve.field.SimilarityFieldComparator;
import io.github.aindriub.jresolve.decision.DecisionThresholds;
import io.github.aindriub.jresolve.decision.ThresholdDecisionEngine;
import io.github.aindriub.jresolve.normalization.CaseFoldNormalizer;
import io.github.aindriub.jresolve.normalization.CombiningMarkNormalizer;
import io.github.aindriub.jresolve.normalization.CompositeNormalizer;
import io.github.aindriub.jresolve.normalization.StringNormalizer;
import io.github.aindriub.jresolve.normalization.UnicodeFormNormalizer;
import io.github.aindriub.jresolve.normalization.WhitespaceNormalizer;
import io.github.aindriub.jresolve.result.Decision;
import io.github.aindriub.jresolve.result.FieldContribution;
import io.github.aindriub.jresolve.result.MatchResult;
import io.github.aindriub.jresolve.result.RejectedCandidate;
import io.github.aindriub.jresolve.result.ScoreScale;
import io.github.aindriub.jresolve.scoring.CompositeRule;
import io.github.aindriub.jresolve.scoring.DefaultFellegiSunterModel;
import io.github.aindriub.jresolve.scoring.FellegiSunterScorer;
import io.github.aindriub.jresolve.scoring.RuleBasedScorer;
import io.github.aindriub.jresolve.scoring.TermFrequencyTable;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Every code example in {@code README.md} that does not need a domain profile,
 * compiled and run. The README is a confident wrong answer the moment it drifts
 * from the API, so it is not allowed to drift: these are the same snippets, and
 * a change that breaks one breaks the build.
 *
 * <p>The domain-shaped examples live in the profiles module's copy of this
 * file, because {@code docs/conventions.md} binds this module's tests too.
 * Fixtures here are neutral tokens; none describes anyone real.
 */
class ReadmeExamplesTest {

    // The two record types the README uses throughout. They are deliberately
    // different shapes: a source and a candidate need not share a value type.

    static final class Incoming {
        private final String reference;
        private final String label;

        Incoming(String reference, String label) {
            this.reference = reference;
            this.label = label;
        }

        String getReference() {
            return reference;
        }

        String getLabel() {
            return label;
        }
    }

    static final class Stored {
        private final String id;
        private final String reference;
        private final String label;

        Stored(String id, String reference, String label) {
            this.id = id;
            this.reference = reference;
            this.label = label;
        }

        String getId() {
            return id;
        }

        String getReference() {
            return reference;
        }

        String getLabel() {
            return label;
        }
    }

    // ------------------------------------------------ 1. the smallest resolver

    private static FieldPipeline<String, String> exactText() {
        return new DefaultFieldPipeline<>(value -> value, new ExactFieldComparator<>());
    }

    @Test
    void theSmallestResolver() {
        DecisionThresholds thresholds =
                new DecisionThresholds(15.0, 5.0, 1.0, ScoreScale.POINTS);

        EntityResolver<Incoming, Stored> resolver = EntityResolverBuilder
                .<Incoming, Stored>builder()
                .field("reference", Incoming::getReference, Stored::getReference, exactText())
                .field("label", Incoming::getLabel, Stored::getLabel, exactText())
                .scorer(RuleBasedScorer.builder()
                        .weight("reference", ComparisonCategory.EXACT, 10.0)
                        .weight("label", ComparisonCategory.EXACT, 10.0)
                        .baseScore(0.0)
                        .build())
                .thresholds(thresholds)
                .decisionEngine(new ThresholdDecisionEngine<>(thresholds))
                .build();

        MatchResult<Stored> result = resolver.resolve(
                new Incoming("AB-1", "widget"),
                Arrays.asList(new Stored("1", "AB-1", "widget"), new Stored("2", "ZZ-9", "gadget")));

        assertThat(result.getDecision()).isEqualTo(Decision.MATCH);
        assertThat(result.getMatch().getId()).isEqualTo("1");
        assertThat(result.getScore().getValue()).isEqualTo(20.0);
    }

    // ------------------------------------------------------- 2. fuzzy matching

    private static FieldPipeline<String, String> fuzzyText() {
        StringNormalizer normalizer = new CompositeNormalizer(Arrays.asList(
                new UnicodeFormNormalizer(),
                new CombiningMarkNormalizer(),
                new CaseFoldNormalizer(),
                new WhitespaceNormalizer()));

        return new DefaultFieldPipeline<>(
                normalizer::normalize,
                new SimilarityFieldComparator(new JaroWinklerSimilarity(), new SimilarityBands()));
    }

    @Test
    void fuzzyMatchingSurvivesCaseSpacingAndATypo() {
        DecisionThresholds thresholds =
                new DecisionThresholds(10.0, 4.0, 1.0, ScoreScale.POINTS);

        EntityResolver<Incoming, Stored> resolver = EntityResolverBuilder
                .<Incoming, Stored>builder()
                .field("reference", Incoming::getReference, Stored::getReference, exactText())
                .field("label", Incoming::getLabel, Stored::getLabel, fuzzyText())
                .scorer(RuleBasedScorer.builder()
                        .weight("reference", ComparisonCategory.EXACT, 8.0)
                        // EXACT is weighted even though this field is fuzzy.
                        // A similarity comparator reports EXACT when the two
                        // values are equal *after* normalization, so a config
                        // that weights only the bands scores a perfect
                        // agreement as zero.
                        .weight("label", ComparisonCategory.EXACT, 6.0)
                        .weight("label", ComparisonCategory.VERY_HIGH, 5.0)
                        .weight("label", ComparisonCategory.HIGH, 3.0)
                        .baseScore(0.0)
                        .build())
                .thresholds(thresholds)
                .decisionEngine(new ThresholdDecisionEngine<>(thresholds))
                .build();

        // Case and spacing are normalized away, so these two are equal by the
        // time the comparator sees them: EXACT, 8 + 6 = 14.
        MatchResult<Stored> normalized = resolver.resolve(
                new Incoming("AB-1", "Widget  Co"),
                Arrays.asList(new Stored("1", "AB-1", "widget co")));

        assertThat(normalized.getDecision()).isEqualTo(Decision.MATCH);
        assertThat(normalized.getScore().getValue()).isEqualTo(14.0);

        // A typo is not normalized away, so this one is banded. Derived by
        // hand: "widgit co" against "widget co" is 8 matches in 9 with no
        // transpositions, so Jaro is (8/9 + 8/9 + 8/8) / 3 = 0.9259; the
        // shared prefix "widg" is 4, so Jaro-Winkler is
        // 0.9259 + 4 x 0.1 x (1 - 0.9259) = 0.9556, which is >= 0.95 and
        // therefore VERY_HIGH. 8 + 5 = 13.
        MatchResult<Stored> typo = resolver.resolve(
                new Incoming("AB-1", "Widgit Co"),
                Arrays.asList(new Stored("1", "AB-1", "widget co")));

        assertThat(typo.getDecision()).isEqualTo(Decision.MATCH);
        assertThat(typo.getScore().getValue()).isEqualTo(13.0);
        assertThat(typo.getCandidates().get(0).getContributions().get(1).getCategory())
                .isEqualTo(ComparisonCategory.VERY_HIGH);

        // An unrelated label bands below anything weighted, so the reference
        // alone is 8.0 and does not reach the threshold.
        MatchResult<Stored> unrelated = resolver.resolve(
                new Incoming("AB-1", "zzzzzzz"),
                Arrays.asList(new Stored("1", "AB-1", "widget co")));

        assertThat(unrelated.getDecision()).isNotEqualTo(Decision.MATCH);
    }

    // ------------------------------------------------- 3. cost tiers and rules

    @Test
    void cheapFieldsAreComparedFirstAndARuleCanVetoBetweenTiers() {
        DecisionThresholds thresholds =
                new DecisionThresholds(10.0, 4.0, 1.0, ScoreScale.POINTS);

        CandidateRule<Incoming, Stored> referenceMustNotConflict = (source, candidate, evidence) ->
                evidence.getField("reference") != null
                        && evidence.getField("reference").getCategory() == ComparisonCategory.CONFLICT
                        ? RuleDecision.REJECT
                        : RuleDecision.CONTINUE;

        EntityResolver<Incoming, Stored> resolver = EntityResolverBuilder
                .<Incoming, Stored>builder()
                .field("reference", Incoming::getReference, Stored::getReference, exactText())
                .cost("reference", CostTiers.CHEAP)
                .field("label", Incoming::getLabel, Stored::getLabel, fuzzyText())
                .cost("label", CostTiers.EXPENSIVE)
                .rule(referenceMustNotConflict)
                .scorer(RuleBasedScorer.builder()
                        .weight("reference", ComparisonCategory.EXACT, 10.0)
                        .weight("label", ComparisonCategory.VERY_HIGH, 5.0)
                        .baseScore(0.0)
                        .build())
                .thresholds(thresholds)
                .decisionEngine(new ThresholdDecisionEngine<>(thresholds))
                .build();

        MatchResult<Stored> result = resolver.resolve(
                new Incoming("AB-1", "widget"),
                Arrays.asList(new Stored("1", "AB-1", "widget"), new Stored("2", "ZZ-9", "widget")));

        // The vetoed candidate never reached the expensive tier, and is
        // reported separately rather than silently dropped.
        assertThat(result.getCandidates()).hasSize(1);
        assertThat(result.getRejectedCandidates()).hasSize(1);
        assertThat(result.getRejectedCandidates().get(0).getCandidate().getId()).isEqualTo("2");
    }

    // --------------------------------------------------- 4. reading the result

    @Test
    void theResultExplainsItself() {
        DecisionThresholds thresholds =
                new DecisionThresholds(15.0, 5.0, 1.0, ScoreScale.POINTS);

        EntityResolver<Incoming, Stored> resolver = EntityResolverBuilder
                .<Incoming, Stored>builder()
                .field("reference", Incoming::getReference, Stored::getReference, exactText())
                .field("label", Incoming::getLabel, Stored::getLabel, exactText())
                .scorer(RuleBasedScorer.builder()
                        .weight("reference", ComparisonCategory.EXACT, 10.0)
                        .weight("label", ComparisonCategory.EXACT, 10.0)
                        .baseScore(0.0)
                        .build())
                .thresholds(thresholds)
                .decisionEngine(new ThresholdDecisionEngine<>(thresholds))
                .build();

        MatchResult<Stored> result = resolver.resolve(
                new Incoming("AB-1", "widget"),
                Arrays.asList(new Stored("1", "AB-1", "widget")));

        assertThat(result.getDecision()).isEqualTo(Decision.MATCH);
        assertThat(result.getScore().getScale()).isEqualTo(ScoreScale.POINTS);
        assertThat(result.getScore().getAlgorithm()).isNotNull();

        for (FieldContribution contribution : result.getCandidates().get(0).getContributions()) {
            assertThat(contribution.getField()).isNotNull();
            assertThat(contribution.getCategory()).isNotNull();
            assertThat(contribution.getContribution()).isNotNull();
            assertThat(contribution.getTemplateKey()).isNotNull();
            assertThat(contribution.getSubsumption()).isNotNull();
            // An explanation carries no field value, so a MatchResult is safe
            // to log. Rendering a message for a human is the consumer's job.
            assertThat(contribution.getTemplateKey()).doesNotContain("widget");
        }

        for (RejectedCandidate<Stored> rejected : result.getRejectedCandidates()) {
            assertThat(rejected.getRuleKey()).startsWith("resolver.rule.");
        }
    }

    // ------------------------------------------------------- 5. Fellegi-Sunter

    @Test
    void agreementOnARareValueOutscoresAgreementOnACommonOne() {
        // A corpus in which "common" occurs nine times and "rare" once.
        TermFrequencyTable.Builder corpus = TermFrequencyTable.builder();
        for (int i = 0; i < 9; i++) {
            corpus.observe("reference", "common");
        }
        corpus.observe("reference", "rare");

        DefaultFellegiSunterModel model = DefaultFellegiSunterModel.builder()
                .probabilities("reference", ComparisonCategory.EXACT, 0.9, 0.1)
                .probabilities("reference", ComparisonCategory.CONFLICT, 0.1, 0.9)
                .frequencies(corpus.build())
                .build();

        // LOG2_LIKELIHOOD_RATIO, not points: a margin means the same thing at
        // every score level, which a points margin does not.
        DecisionThresholds thresholds =
                new DecisionThresholds(2.0, 0.0, 1.0, ScoreScale.LOG2_LIKELIHOOD_RATIO);

        EntityResolver<Incoming, Stored> resolver = EntityResolverBuilder
                .<Incoming, Stored>builder()
                .field("reference", Incoming::getReference, Stored::getReference, exactText())
                .scorer(new FellegiSunterScorer(model))
                .thresholds(thresholds)
                .decisionEngine(new ThresholdDecisionEngine<>(thresholds))
                .build();

        double rare = resolver.resolve(new Incoming("rare", "x"),
                Arrays.asList(new Stored("1", "rare", "x"))).getScore().getValue();
        double common = resolver.resolve(new Incoming("common", "x"),
                Arrays.asList(new Stored("2", "common", "x"))).getScore().getValue();

        // This is what the probabilistic path buys, and it is not the
        // probability: agreeing on a rare value is stronger evidence.
        assertThat(rare).isGreaterThan(common);
    }

    @Test
    void correlatedFieldsAreDeclaredAsOneComparison() {
        // Two fields that co-vary carry one signal, not two, so a model
        // over both counts it twice and is overconfident. Declaring them
        // composite makes the group contribute once. The README names a
        // concrete correlated pair in prose; core's fixtures stay neutral.
        DefaultFellegiSunterModel model = DefaultFellegiSunterModel.builder()
                .probabilities("reference", ComparisonCategory.EXACT, 0.9, 0.1)
                .probabilities("label", ComparisonCategory.EXACT, 0.5, 0.25)
                .composite(Arrays.asList("reference", "label"), CompositeRule.SMALLEST)
                .build();

        DecisionThresholds thresholds =
                new DecisionThresholds(2.0, 0.0, 1.0, ScoreScale.LOG2_LIKELIHOOD_RATIO);

        EntityResolver<Incoming, Stored> resolver = EntityResolverBuilder
                .<Incoming, Stored>builder()
                .field("reference", Incoming::getReference, Stored::getReference, exactText())
                .field("label", Incoming::getLabel, Stored::getLabel, exactText())
                .scorer(new FellegiSunterScorer(model))
                .thresholds(thresholds)
                .decisionEngine(new ThresholdDecisionEngine<>(thresholds))
                .build();

        // reference is log2(0.9/0.1) = 3.17, label is log2(0.5/0.25) = 1.0.
        // Summed independently that is 4.17; as one composite group under
        // SMALLEST it is the least favourable member alone, 1.0.
        double total = resolver.resolve(new Incoming("AB-1", "widget"),
                Arrays.asList(new Stored("1", "AB-1", "widget"))).getScore().getValue();

        assertThat(total).isEqualTo(1.0, org.assertj.core.api.Assertions.within(1e-9));
    }

    @Test
    void thereIsNoProbabilityUnlessTheModelCarriesPriorOdds() {
        DefaultFellegiSunterModel withoutPrior = DefaultFellegiSunterModel.builder()
                .probabilities("reference", ComparisonCategory.EXACT, 0.9, 0.1)
                .build();

        DecisionThresholds thresholds =
                new DecisionThresholds(2.0, 0.0, 1.0, ScoreScale.LOG2_LIKELIHOOD_RATIO);

        EntityResolver<Incoming, Stored> resolver = EntityResolverBuilder
                .<Incoming, Stored>builder()
                .field("reference", Incoming::getReference, Stored::getReference, exactText())
                .scorer(new FellegiSunterScorer(withoutPrior))
                .thresholds(thresholds)
                .decisionEngine(new ThresholdDecisionEngine<>(thresholds))
                .build();

        MatchResult<Stored> result = resolver.resolve(new Incoming("rare", "x"),
                Arrays.asList(new Stored("1", "rare", "x")));

        // Leaving prior odds unset is a real choice, not an omission: a weight
        // and no probability is the honest output when nobody supplied a base
        // rate. See docs/calibration.md.
        assertThat(result.getScore().getProbability()).isNull();
    }
}
