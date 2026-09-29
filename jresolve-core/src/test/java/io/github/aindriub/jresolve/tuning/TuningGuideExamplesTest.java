package io.github.aindriub.jresolve.tuning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.github.aindriub.jresolve.api.CandidateRule;
import io.github.aindriub.jresolve.api.EntityResolver;
import io.github.aindriub.jresolve.api.EntityResolverBuilder;
import io.github.aindriub.jresolve.api.RuleDecision;
import io.github.aindriub.jresolve.comparison.JaroWinklerSimilarity;
import io.github.aindriub.jresolve.decision.DecisionThresholds;
import io.github.aindriub.jresolve.decision.ThresholdDecisionEngine;
import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.field.CostTiers;
import io.github.aindriub.jresolve.field.DefaultFieldPipeline;
import io.github.aindriub.jresolve.field.ExactFieldComparator;
import io.github.aindriub.jresolve.field.FieldPipeline;
import io.github.aindriub.jresolve.field.SimilarityBands;
import io.github.aindriub.jresolve.field.SimilarityFieldComparator;
import io.github.aindriub.jresolve.result.Decision;
import io.github.aindriub.jresolve.result.FieldContribution;
import io.github.aindriub.jresolve.result.MatchResult;
import io.github.aindriub.jresolve.result.ScoreScale;
import io.github.aindriub.jresolve.scoring.CompositeRule;
import io.github.aindriub.jresolve.scoring.DefaultFellegiSunterModel;
import io.github.aindriub.jresolve.scoring.FellegiSunterScorer;
import io.github.aindriub.jresolve.scoring.MatchScorer;
import io.github.aindriub.jresolve.scoring.RuleBasedScorer;
import io.github.aindriub.jresolve.scoring.TermFrequencyTable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Every Java code block in {@code docs/tuning.md}, compiled and run. Each test
 * asserts the effect its section claims, so a guide that says a margin turns a
 * MATCH into a REVIEW is held to it by the build.
 *
 * <p>The numbers in these tests are arithmetic on invented weights. They are
 * not calibrated and not recommendations; the guide says so and so does this
 * file. Fixtures are synthetic tokens, and none describes anyone real.
 */
class TuningGuideExamplesTest {

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

    // ------------------------------------------------------------- set-up

    private static FieldPipeline<String, String> exactText() {
        return new DefaultFieldPipeline<>(value -> value, new ExactFieldComparator<>());
    }

    private static FieldPipeline<String, String> fuzzyText(SimilarityBands bands) {
        return new DefaultFieldPipeline<>(
                value -> value == null ? null : value.trim().toLowerCase(),
                new SimilarityFieldComparator(new JaroWinklerSimilarity(), bands));
    }

    private static EntityResolver<Incoming, Stored> resolverFor(
            MatchScorer scorer, DecisionThresholds thresholds) {
        return EntityResolverBuilder
                .<Incoming, Stored>builder()
                .field("reference", Incoming::getReference, Stored::getReference, exactText())
                .field("label", Incoming::getLabel, Stored::getLabel, exactText())
                .scorer(scorer)
                .thresholds(thresholds)
                .decisionEngine(new ThresholdDecisionEngine<>(thresholds))
                .build();
    }

    // ------------------------------------- 1. diagnosing a false positive

    @Test
    void aMatchCanBeExplainedFieldByField() {
        DecisionThresholds thresholds = new DecisionThresholds(10.0, 4.0, 1.0, ScoreScale.POINTS);
        MatchScorer scorer = RuleBasedScorer.builder()
                .weight("reference", ComparisonCategory.EXACT, 10.0)
                .weight("label", ComparisonCategory.EXACT, 2.0)
                .weight("label", ComparisonCategory.CONFLICT, 0.0)
                .baseScore(0.0)
                .build();

        MatchResult<Stored> result = resolverFor(scorer, thresholds).resolve(
                new Incoming("AB-1", "widget"),
                Arrays.asList(new Stored("1", "AB-1", "gadget")));

        // The decision, the score and the line it had to clear.
        assertThat(result.getDecision()).isEqualTo(Decision.MATCH);
        double score = result.getScore().getValue();
        double headroom = score - thresholds.getMatchThreshold();

        // Which fields carried it. The label disagreed and cost nothing.
        List<String> explanation = new ArrayList<>();
        for (FieldContribution contribution : result.getCandidates().get(0).getContributions()) {
            explanation.add(contribution.getField() + " " + contribution.getCategory().getName()
                    + " " + contribution.getContribution());
        }

        assertThat(headroom).isEqualTo(0.0);
        assertThat(explanation).containsExactly("reference EXACT 10.0", "label CONFLICT 0.0");
        assertThat(result.getRejectedCandidates()).isEmpty();
    }

    // ------------------------------------------------------ 2. thresholds

    @Test
    void raisingTheMatchThresholdTurnsAMatchIntoAReview() {
        MatchScorer scorer = RuleBasedScorer.builder()
                .weight("reference", ComparisonCategory.EXACT, 10.0)
                .weight("label", ComparisonCategory.EXACT, 2.0)
                .weight("label", ComparisonCategory.CONFLICT, 0.0)
                .baseScore(0.0)
                .build();
        Incoming source = new Incoming("AB-1", "widget");
        List<Stored> candidates = Arrays.asList(new Stored("1", "AB-1", "gadget"));

        DecisionThresholds loose = new DecisionThresholds(10.0, 4.0, 1.0, ScoreScale.POINTS);
        DecisionThresholds strict = new DecisionThresholds(12.0, 4.0, 1.0, ScoreScale.POINTS);

        assertThat(resolverFor(scorer, loose).resolve(source, candidates).getDecision())
                .isEqualTo(Decision.MATCH);
        assertThat(resolverFor(scorer, strict).resolve(source, candidates).getDecision())
                .isEqualTo(Decision.REVIEW);

        // Raising reviewThreshold past the score sends it to NO_MATCH instead.
        DecisionThresholds stricter = new DecisionThresholds(14.0, 11.0, 1.0, ScoreScale.POINTS);
        assertThat(resolverFor(scorer, stricter).resolve(source, candidates).getDecision())
                .isEqualTo(Decision.NO_MATCH);
    }

    @Test
    void aMinimumMarginTurnsANearTieIntoAReview() {
        MatchScorer scorer = RuleBasedScorer.builder()
                .weight("reference", ComparisonCategory.EXACT, 10.0)
                .weight("label", ComparisonCategory.EXACT, 1.0)
                .weight("label", ComparisonCategory.CONFLICT, 0.0)
                .baseScore(0.0)
                .build();
        Incoming source = new Incoming("AB-1", "widget");
        // Scores 11.0 and 10.0: a margin of 1.0.
        List<Stored> candidates = Arrays.asList(
                new Stored("1", "AB-1", "widget"),
                new Stored("2", "AB-1", "gadget"));

        DecisionThresholds noMargin = new DecisionThresholds(10.0, 4.0, 0.0, ScoreScale.POINTS);
        DecisionThresholds wideMargin = new DecisionThresholds(10.0, 4.0, 2.0, ScoreScale.POINTS);

        MatchResult<Stored> accepted = resolverFor(scorer, noMargin).resolve(source, candidates);
        MatchResult<Stored> tied = resolverFor(scorer, wideMargin).resolve(source, candidates);

        assertThat(accepted.getDecision()).isEqualTo(Decision.MATCH);
        assertThat(tied.getDecision()).isEqualTo(Decision.REVIEW);
        assertThat(tied.getMargin()).isEqualTo(1.0);
        assertThat(tied.getMatch()).isNull();
    }

    // ------------------------------------- 3. negative weights, required

    @Test
    void aNegativeConflictWeightPullsAFalsePositiveBelowTheReviewLine() {
        DecisionThresholds thresholds = new DecisionThresholds(10.0, 4.0, 1.0, ScoreScale.POINTS);
        MatchScorer scorer = RuleBasedScorer.builder()
                .weight("reference", ComparisonCategory.EXACT, 10.0)
                .weight("label", ComparisonCategory.EXACT, 2.0)
                .weight("label", ComparisonCategory.CONFLICT, -8.0)
                .baseScore(0.0)
                .build();

        MatchResult<Stored> result = resolverFor(scorer, thresholds).resolve(
                new Incoming("AB-1", "widget"),
                Arrays.asList(new Stored("1", "AB-1", "gadget")));

        // 10 + (-8) = 2, under the review threshold of 4.
        assertThat(result.getScore().getValue()).isEqualTo(2.0);
        assertThat(result.getDecision()).isEqualTo(Decision.NO_MATCH);
    }

    @Test
    void aNegativeLowWeightPenalisesAFuzzyFieldThatBarelyResembles() {
        DecisionThresholds thresholds = new DecisionThresholds(10.0, 4.0, 1.0, ScoreScale.POINTS);
        MatchScorer scorer = RuleBasedScorer.builder()
                .weight("reference", ComparisonCategory.EXACT, 10.0)
                .weight("label", ComparisonCategory.EXACT, 2.0)
                .weight("label", ComparisonCategory.LOW, -8.0)
                .baseScore(0.0)
                .build();
        EntityResolver<Incoming, Stored> resolver = EntityResolverBuilder
                .<Incoming, Stored>builder()
                .field("reference", Incoming::getReference, Stored::getReference, exactText())
                .field("label", Incoming::getLabel, Stored::getLabel, fuzzyText(new SimilarityBands()))
                .scorer(scorer)
                .thresholds(thresholds)
                .decisionEngine(new ThresholdDecisionEngine<>(thresholds))
                .build();

        MatchResult<Stored> result = resolver.resolve(
                new Incoming("AB-1", "widget"),
                Arrays.asList(new Stored("1", "AB-1", "zzzzzzz")));

        assertThat(result.getCandidates().get(0).getContributions().get(1).getCategory())
                .isEqualTo(ComparisonCategory.LOW);
        assertThat(result.getDecision()).isEqualTo(Decision.NO_MATCH);
    }

    @Test
    void aRequiredFieldThatIsMissingRemovesTheCandidateFromScoring() {
        DecisionThresholds thresholds = new DecisionThresholds(10.0, 4.0, 1.0, ScoreScale.POINTS);
        MatchScorer scorer = RuleBasedScorer.builder()
                .weight("reference", ComparisonCategory.EXACT, 10.0)
                .weight("label", ComparisonCategory.EXACT, 2.0)
                .requiredField("label")
                .baseScore(0.0)
                .build();

        MatchResult<Stored> result = resolverFor(scorer, thresholds).resolve(
                new Incoming("AB-1", "widget"),
                Arrays.asList(new Stored("1", "AB-1", null)));

        // Unscorable is not a veto: the candidate is absent from both lists.
        assertThat(result.getDecision()).isEqualTo(Decision.NO_MATCH);
        assertThat(result.getCandidates()).isEmpty();
        assertThat(result.getRejectedCandidates()).isEmpty();
    }

    // ----------------------------------------------- 4. similarity bands

    @Test
    void strictBandsDemoteASimilarityFromVeryHighToHigh() {
        SimilarityBands strict = new SimilarityBands(0.98, 0.90, 0.75, 0.0);
        DecisionThresholds thresholds = new DecisionThresholds(10.0, 4.0, 1.0, ScoreScale.POINTS);
        MatchScorer scorer = RuleBasedScorer.builder()
                .weight("label", ComparisonCategory.EXACT, 10.0)
                .weight("label", ComparisonCategory.VERY_HIGH, 10.0)
                .weight("label", ComparisonCategory.HIGH, 5.0)
                .baseScore(0.0)
                .build();

        // "widgit co" against "widget co" scores about 0.9556: VERY_HIGH under
        // the defaults (>= 0.95) and HIGH under the stricter cut-offs above.
        ComparisonCategory underDefaults = categoryOfTypo(new SimilarityBands(), scorer, thresholds);
        ComparisonCategory underStrict = categoryOfTypo(strict, scorer, thresholds);

        assertThat(underDefaults).isEqualTo(ComparisonCategory.VERY_HIGH);
        assertThat(underStrict).isEqualTo(ComparisonCategory.HIGH);
    }

    private static ComparisonCategory categoryOfTypo(
            SimilarityBands bands, MatchScorer scorer, DecisionThresholds thresholds) {
        EntityResolver<Incoming, Stored> resolver = EntityResolverBuilder
                .<Incoming, Stored>builder()
                .field("label", Incoming::getLabel, Stored::getLabel, fuzzyText(bands))
                .scorer(scorer)
                .thresholds(thresholds)
                .decisionEngine(new ThresholdDecisionEngine<>(thresholds))
                .build();
        return resolver.resolve(new Incoming("x", "Widgit Co"),
                        Arrays.asList(new Stored("1", "x", "widget co")))
                .getCandidates().get(0).getContributions().get(0).getCategory();
    }

    // ------------------------------------------- 5. cost tiers and vetoes

    @Test
    void aVetoMovesACandidateIntoTheRejectedList() {
        DecisionThresholds thresholds = new DecisionThresholds(10.0, 4.0, 1.0, ScoreScale.POINTS);
        MatchScorer scorer = RuleBasedScorer.builder()
                .weight("reference", ComparisonCategory.EXACT, 10.0)
                .weight("label", ComparisonCategory.EXACT, 2.0)
                .baseScore(0.0)
                .build();

        CandidateRule<Incoming, Stored> labelMustNotConflict = (source, candidate, evidence) ->
                evidence.getField("label") != null
                        && evidence.getField("label").getCategory() == ComparisonCategory.CONFLICT
                        ? RuleDecision.REJECT
                        : RuleDecision.CONTINUE;

        EntityResolver<Incoming, Stored> resolver = EntityResolverBuilder
                .<Incoming, Stored>builder()
                .field("label", Incoming::getLabel, Stored::getLabel, exactText())
                .cost("label", CostTiers.CHEAP)
                .field("reference", Incoming::getReference, Stored::getReference, exactText())
                .cost("reference", CostTiers.EXPENSIVE)
                .rule(labelMustNotConflict)
                .scorer(scorer)
                .thresholds(thresholds)
                .decisionEngine(new ThresholdDecisionEngine<>(thresholds))
                .build();

        MatchResult<Stored> result = resolver.resolve(
                new Incoming("AB-1", "widget"),
                Arrays.asList(new Stored("1", "AB-1", "widget"), new Stored("2", "AB-1", "gadget")));

        assertThat(result.getCandidates()).hasSize(1);
        assertThat(result.getRejectedCandidates()).hasSize(1);
        assertThat(result.getRejectedCandidates().get(0).getCandidate().getId()).isEqualTo("2");
        // The key names the rule by its position among the registered rules.
        assertThat(result.getRejectedCandidates().get(0).getRuleKey()).isEqualTo("resolver.rule.0");
    }

    // ------------------------------------------------ 6. Fellegi-Sunter

    @Test
    void frequenciesMakeAgreementOnARareValueWorthMore() {
        TermFrequencyTable.Builder corpus = TermFrequencyTable.builder();
        for (int i = 0; i < 9; i++) {
            corpus.observe("reference", "common");
        }
        corpus.observe("reference", "rare");

        DefaultFellegiSunterModel flat = DefaultFellegiSunterModel.builder()
                .probabilities("reference", ComparisonCategory.EXACT, 0.9, 0.1)
                .ignore("label", ComparisonCategory.EXACT)
                .build();
        DefaultFellegiSunterModel weighted = DefaultFellegiSunterModel.builder()
                .probabilities("reference", ComparisonCategory.EXACT, 0.9, 0.1)
                .ignore("label", ComparisonCategory.EXACT)
                .frequencies(corpus.build())
                .build();

        DecisionThresholds thresholds =
                new DecisionThresholds(2.0, 0.0, 1.0, ScoreScale.LOG2_LIKELIHOOD_RATIO);

        double flatRare = fellegiSunterScore(flat, thresholds, "rare", "x");
        double flatCommon = fellegiSunterScore(flat, thresholds, "common", "x");
        double rare = fellegiSunterScore(weighted, thresholds, "rare", "x");
        double common = fellegiSunterScore(weighted, thresholds, "common", "x");

        assertThat(flatRare).isEqualTo(flatCommon);
        assertThat(rare).isGreaterThan(common);
    }

    @Test
    void theCompositeRuleDecidesHowMuchACorrelatedGroupClaims() {
        DecisionThresholds thresholds =
                new DecisionThresholds(0.0, 0.0, 0.0, ScoreScale.LOG2_LIKELIHOOD_RATIO);

        double smallest = groupScore(CompositeRule.SMALLEST, thresholds);
        double average = groupScore(CompositeRule.AVERAGE, thresholds);
        double strongest = groupScore(CompositeRule.STRONGEST, thresholds);

        // reference is log2(0.9 / 0.1) = log2(9) = 3.1699, label is
        // log2(0.5 / 0.25) = 1.0. SMALLEST keeps the 1.0, STRONGEST keeps
        // 3.1699, AVERAGE is (3.1699 + 1.0) / 2 = 2.0850.
        assertThat(smallest).isEqualTo(1.0, within(1e-9));
        assertThat(average).isEqualTo(2.08496, within(1e-4));
        assertThat(strongest).isEqualTo(3.16993, within(1e-4));
    }

    private static double groupScore(CompositeRule rule, DecisionThresholds thresholds) {
        DefaultFellegiSunterModel model = DefaultFellegiSunterModel.builder()
                .probabilities("reference", ComparisonCategory.EXACT, 0.9, 0.1)
                .probabilities("label", ComparisonCategory.EXACT, 0.5, 0.25)
                .composite(Arrays.asList("reference", "label"), rule)
                .build();
        return fellegiSunterScore(model, thresholds, "AB-1", "widget");
    }

    private static double fellegiSunterScore(DefaultFellegiSunterModel model,
            DecisionThresholds thresholds, String reference, String label) {
        return resolverFor(new FellegiSunterScorer(model), thresholds)
                .resolve(new Incoming(reference, label),
                        Arrays.asList(new Stored("1", reference, label)))
                .getScore().getValue();
    }

    @Test
    void priorOddsAreWhatMakesAProbabilityAppearAtAll() {
        DecisionThresholds thresholds =
                new DecisionThresholds(0.0, 0.0, 0.0, ScoreScale.LOG2_LIKELIHOOD_RATIO);
        DefaultFellegiSunterModel withoutPrior = DefaultFellegiSunterModel.builder()
                .probabilities("reference", ComparisonCategory.EXACT, 0.5, 0.25)
                .probabilities("label", ComparisonCategory.EXACT, 0.5, 0.25)
                .build();
        DefaultFellegiSunterModel withPrior = DefaultFellegiSunterModel.builder()
                .probabilities("reference", ComparisonCategory.EXACT, 0.5, 0.25)
                .probabilities("label", ComparisonCategory.EXACT, 0.5, 0.25)
                .priorOdds(0.01)
                .build();

        Incoming source = new Incoming("AB-1", "widget");
        List<Stored> candidates = Arrays.asList(new Stored("1", "AB-1", "widget"));

        Double none = resolverFor(new FellegiSunterScorer(withoutPrior), thresholds)
                .resolve(source, candidates).getScore().getProbability();
        Double some = resolverFor(new FellegiSunterScorer(withPrior), thresholds)
                .resolve(source, candidates).getScore().getProbability();

        // W = 1 + 1 = 2, so posterior odds = 0.01 x 2^2 = 0.04 and the
        // probability under the model is 0.04 / 1.04 = 0.0385. Changing the
        // prior to 1.0 would give 4 / 5 = 0.8 from the same evidence.
        assertThat(none).isNull();
        assertThat(some).isEqualTo(0.04 / 1.04, within(1e-9));
    }

    // -------------------------------------------- 7. sweeping thresholds

    @Test
    void sweepingThresholdsCountsErrorsOnAHandLabelledSample() {
        DecisionThresholds thresholds =
                new DecisionThresholds(10.0, 4.0, 0.0, ScoreScale.POINTS);
        MatchScorer scorer = RuleBasedScorer.builder()
                .weight("reference", ComparisonCategory.EXACT, 10.0)
                .weight("label", ComparisonCategory.EXACT, 1.0)
                .weight("label", ComparisonCategory.CONFLICT, 0.0)
                .baseScore(0.0)
                .build();
        EntityResolver<Incoming, Stored> resolver = resolverFor(scorer, thresholds);

        // Your own sample: each source, the candidate to compare, and whether
        // someone who knows the data says they are the same entity.
        Incoming[] sources = {
                new Incoming("AB-1", "widget"), new Incoming("AB-2", "gadget"),
                new Incoming("AB-3", "sprocket"), new Incoming("ZZ-9", "flange")};
        Stored[] candidates = {
                new Stored("1", "AB-1", "widget"), new Stored("2", "AB-2", "gadget"),
                new Stored("3", "AB-3", "bracket"), new Stored("4", "QQ-0", "washer")};
        boolean[] sameEntity = {true, true, false, false};

        double[] topScores = new double[sources.length];
        for (int i = 0; i < sources.length; i++) {
            topScores[i] = resolver.resolve(sources[i], Arrays.asList(candidates[i]))
                    .getScore().getValue();
        }

        int[] falsePositives = new int[3];
        int[] missedMatches = new int[3];
        double[] tried = {10.0, 11.0, 12.0};
        for (int t = 0; t < tried.length; t++) {
            for (int i = 0; i < sources.length; i++) {
                boolean accepted = topScores[i] >= tried[t];
                if (accepted && !sameEntity[i]) {
                    falsePositives[t]++;
                }
                if (!accepted && sameEntity[i]) {
                    missedMatches[t]++;
                }
            }
        }

        // Scores are 11, 11, 10 and 0. At 10 the third is a false positive;
        // at 11 nothing is wrong; at 12 both true matches are missed.
        assertThat(falsePositives).containsExactly(1, 0, 0);
        assertThat(missedMatches).containsExactly(0, 0, 2);
    }

    // ------------------------------------------------- guide stays honest

    @Test
    void everyJavaBlockInTheGuideIsInThisFile() throws IOException {
        Path guide = Paths.get("..", "docs", "tuning.md");
        Path self = Paths.get("src", "test", "java", "io", "github", "aindriub", "jresolve",
                "tuning", "TuningGuideExamplesTest.java");
        Set<String> compiled = new HashSet<>();
        for (String line : Files.readAllLines(self, StandardCharsets.UTF_8)) {
            compiled.add(line.trim());
        }

        List<String> missing = new ArrayList<>();
        int blocks = 0;
        boolean inJava = false;
        for (String line : Files.readAllLines(guide, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                if (inJava) {
                    inJava = false;
                } else if (trimmed.equals("```java")) {
                    inJava = true;
                    blocks++;
                }
                continue;
            }
            if (inJava && !trimmed.isEmpty() && !compiled.contains(trimmed)) {
                missing.add(trimmed);
            }
        }

        assertThat(blocks).isGreaterThan(0);
        assertThat(missing).isEmpty();
    }
}
