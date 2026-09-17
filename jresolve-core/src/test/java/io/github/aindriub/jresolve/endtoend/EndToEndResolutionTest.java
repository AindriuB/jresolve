package io.github.aindriub.jresolve.endtoend;

import io.github.aindriub.jresolve.api.CandidateRule;
import io.github.aindriub.jresolve.api.EntityResolutionConfigurationException;
import io.github.aindriub.jresolve.api.EntityResolver;
import io.github.aindriub.jresolve.api.EntityResolverBuilder;
import io.github.aindriub.jresolve.api.RuleDecision;
import io.github.aindriub.jresolve.comparison.LevenshteinSimilarity;
import io.github.aindriub.jresolve.decision.DecisionThresholds;
import io.github.aindriub.jresolve.decision.ThresholdDecisionEngine;
import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.FieldEvidence;
import io.github.aindriub.jresolve.field.CostTiers;
import io.github.aindriub.jresolve.field.DefaultFieldPipeline;
import io.github.aindriub.jresolve.field.ExactFieldComparator;
import io.github.aindriub.jresolve.field.FieldComparator;
import io.github.aindriub.jresolve.field.FieldPipeline;
import io.github.aindriub.jresolve.field.SimilarityBands;
import io.github.aindriub.jresolve.field.SimilarityFieldComparator;
import io.github.aindriub.jresolve.normalization.ApostropheVariantNormalizer;
import io.github.aindriub.jresolve.normalization.CaseFoldNormalizer;
import io.github.aindriub.jresolve.normalization.CombiningMarkNormalizer;
import io.github.aindriub.jresolve.normalization.CompositeNormalizer;
import io.github.aindriub.jresolve.normalization.PunctuationNormalizer;
import io.github.aindriub.jresolve.normalization.StringNormalizer;
import io.github.aindriub.jresolve.normalization.UnicodeFormNormalizer;
import io.github.aindriub.jresolve.normalization.WhitespaceNormalizer;
import io.github.aindriub.jresolve.result.Decision;
import io.github.aindriub.jresolve.result.FieldContribution;
import io.github.aindriub.jresolve.result.MatchResult;
import io.github.aindriub.jresolve.result.ScoreScale;
import io.github.aindriub.jresolve.result.ScoredCandidate;
import io.github.aindriub.jresolve.scoring.RuleBasedScorer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The milestone gate: assembles a whole resolver from a consumer's seat —
 * {@link IncomingRecord} as the source, {@link StoredRecord} as the candidate — and
 * proves the layers compose end to end.
 *
 * <p>Every source and candidate value used below is invented for this file;
 * none describes anyone real, and none is drawn from any public dataset.
 *
 * <p><strong>What this milestone does not yet cover:</strong> there is no
 * alias repository (D7) and no locator comparison pipeline (D9) in the
 * merged code, so a source/candidate pair such as {@code "Seán"} and
 * {@code "John"} does not score as an alias match here — it falls through to
 * a low string-similarity band, exactly as an unrelated pair would. §88
 * expects {@code ALIAS} for that label pair and {@code VERY_HIGH} for the
 * locator pair below; this milestone produces {@code LOW} and {@code MEDIUM}
 * instead, and closing that gap is D7's and D9's job, not this test's.
 *
 * <p><strong>Those now exist, and this suite still does not use them.</strong>
 * The alias repository and the locator pipeline live in
 * {@code jresolve-profiles-ie}, and {@code jresolve-core} cannot depend on
 * that module — the dependency runs the other way and a test-scoped edge
 * would cycle the reactor. So this file keeps the generic comparators on
 * purpose: what it proves is that the <em>core</em> layers compose, with
 * every band below derived from a hand-computed edit distance. The claim that
 * the library beats an exact-key join is a different claim, and it is tested
 * where the real pipelines are, in that module's {@code endtoend} package.
 * A reader who wants to know whether a fuzzy field decides anything should
 * look there, not here.
 *
 * <p><strong>What actually decides §88 today:</strong> serial EXACT (30)
 * plus issuedOn EXACT (25) already total 55, above the 50-point match
 * threshold, before label or locator contribute anything. The two fuzzy
 * fields — the ones §88 exists to demonstrate — net only +10 between them:
 * label is a −5 penalty and locator a +15 contribution. In plain terms,
 * the positive scenario below
 * would pass as a two-exact-key join — the same outcome a plain SQL join on
 * {@code serial} and {@code issuedOn} would produce. That is an honest statement of
 * where this library stands after milestone 1, not a weakened stand-in for
 * §88; D7 (alias repository) and D9 (locator pipeline) are what would make
 * the fuzzy fields actually carry weight.
 */
class EndToEndResolutionTest {

    // --- Shared pipeline and scorer configuration -------------------------
    //
    // label and locator both use Levenshtein similarity so every
    // expected category below is derived from a hand-computed edit distance
    // in each test's comment, never from a value produced by running the
    // comparator. serial and issuedOn are exact-match fields, so their
    // categories follow directly from equality after normalization.

    private static FieldPipeline<String, String> labelPipeline() {
        StringNormalizer normalizer = new CompositeNormalizer(Arrays.asList(
                new UnicodeFormNormalizer(), new CombiningMarkNormalizer(), new CaseFoldNormalizer()));
        return new DefaultFieldPipeline<>(
                normalizer::normalize, new SimilarityFieldComparator(new LevenshteinSimilarity(), new SimilarityBands()));
    }

    private static FieldPipeline<String, String> serialPipeline() {
        StringNormalizer normalizer = new CompositeNormalizer(Arrays.asList(
                new UnicodeFormNormalizer(), new CombiningMarkNormalizer(),
                new ApostropheVariantNormalizer(), new CaseFoldNormalizer()));
        return new DefaultFieldPipeline<>(normalizer::normalize, new ExactFieldComparator<>());
    }

    private static FieldPipeline<LocalDate, LocalDate> issuedOnPipeline() {
        return new DefaultFieldPipeline<>(value -> value, new ExactFieldComparator<>());
    }

    private static final StringNormalizer LOCATOR_NORMALIZER = new CompositeNormalizer(Arrays.asList(
            new CaseFoldNormalizer(),
            new PunctuationNormalizer(new LinkedHashSet<>(Arrays.asList('.', ','))),
            new WhitespaceNormalizer()));

    private static String normalizeLocatorLines(List<String> lines) {
        return LOCATOR_NORMALIZER.normalize(String.join(" ", lines));
    }

    private static String normalizeLocatorLine(String line) {
        return LOCATOR_NORMALIZER.normalize(line);
    }

    private static FieldComparator<String> locatorComparator() {
        return new SimilarityFieldComparator(new LevenshteinSimilarity(), new SimilarityBands());
    }

    /**
     * Four fields spanning two cost tiers: {@code label}/{@code
     * serial} at the default {@link CostTiers#CHEAP}, {@code issuedOn}
     * at {@link CostTiers#MODERATE}, {@code locator} at {@link
     * CostTiers#EXPENSIVE}. {@code locator} uses the asymmetric overload —
     * {@link IncomingRecord#getLocator()} returns {@code List<String>},
     * {@link StoredRecord#getLocator()} returns a single {@code String}.
     *
     * @param locatorCandidateExtractor lets a test observe or replace how the
     *     candidate-side locator is read, without duplicating the rest of
     *     the field configuration
     */
    private static EntityResolverBuilder<IncomingRecord, StoredRecord> builder(
            Function<StoredRecord, String> locatorCandidateExtractor) {
        return EntityResolverBuilder.<IncomingRecord, StoredRecord>builder()
                .field("label", IncomingRecord::getLabel, StoredRecord::getLabel, labelPipeline())
                .field("serial", IncomingRecord::getSerial, StoredRecord::getSerial, serialPipeline())
                .field("issuedOn", IncomingRecord::getIssuedOn, StoredRecord::getIssuedOn, issuedOnPipeline())
                .cost("issuedOn", CostTiers.MODERATE)
                .field("locator", IncomingRecord::getLocator, locatorCandidateExtractor,
                        EndToEndResolutionTest::normalizeLocatorLines, EndToEndResolutionTest::normalizeLocatorLine,
                        locatorComparator())
                .cost("locator", CostTiers.EXPENSIVE);
    }

    private static EntityResolverBuilder<IncomingRecord, StoredRecord> builder() {
        return builder(StoredRecord::getLocator);
    }

    /** Hand-chosen POINTS weights; every category any comparator above can emit is configured explicitly. */
    private static RuleBasedScorer scorer() {
        return RuleBasedScorer.builder()
                .weight("label", ComparisonCategory.EXACT, 35.0)
                .weight("label", ComparisonCategory.VERY_HIGH, 30.0)
                .weight("label", ComparisonCategory.HIGH, 20.0)
                .weight("label", ComparisonCategory.MEDIUM, 10.0)
                .weight("label", ComparisonCategory.LOW, -5.0)
                .weight("label", ComparisonCategory.MISSING_ONE, 0.0)
                .weight("label", ComparisonCategory.MISSING_BOTH, 0.0)
                .weight("serial", ComparisonCategory.EXACT, 30.0)
                .weight("serial", ComparisonCategory.CONFLICT, -30.0)
                .weight("serial", ComparisonCategory.MISSING_ONE, 0.0)
                .weight("serial", ComparisonCategory.MISSING_BOTH, 0.0)
                .weight("issuedOn", ComparisonCategory.EXACT, 25.0)
                .weight("issuedOn", ComparisonCategory.CONFLICT, -100.0)
                .weight("issuedOn", ComparisonCategory.MISSING_ONE, -5.0)
                .weight("issuedOn", ComparisonCategory.MISSING_BOTH, 0.0)
                .weight("locator", ComparisonCategory.EXACT, 40.0)
                .weight("locator", ComparisonCategory.VERY_HIGH, 35.0)
                .weight("locator", ComparisonCategory.HIGH, 20.0)
                .weight("locator", ComparisonCategory.MEDIUM, 15.0)
                .weight("locator", ComparisonCategory.LOW, -5.0)
                .weight("locator", ComparisonCategory.MISSING_ONE, 0.0)
                .weight("locator", ComparisonCategory.MISSING_BOTH, 0.0)
                .baseScore(0.0)
                .build();
    }

    private static DecisionThresholds thresholds() {
        return new DecisionThresholds(50.0, 20.0, 10.0, ScoreScale.POINTS);
    }

    /**
     * Builds the resolver from {@code partial}, wiring {@link #scorer()} and
     * one {@link DecisionThresholds} instance into both {@code
     * .thresholds(...)} and the {@link ThresholdDecisionEngine}.
     *
     * <p>That agreement is now enforced rather than merely contracted:
     * {@link EntityResolverBuilder#thresholds} checks the thresholds the
     * engine declares against both the scorer's scale and the value passed
     * to it, so a divergent configuration no longer builds (D6, closed by
     * task 17). Passing one instance to both is still the clearest way to
     * write it, but two equal instances would build just as well — the check
     * is by value. {@link #twoDifferentThresholdsInstancesAreRejectedAtBuild()},
     * {@link #anEngineOnTheWrongScaleIsRejectedAtBuild()} and
     * {@link #separatelyConstructedButEqualThresholdsStillBuild()} cover the
     * three cases directly.
     */
    private static EntityResolver<IncomingRecord, StoredRecord> resolverWith(EntityResolverBuilder<IncomingRecord, StoredRecord> partial) {
        DecisionThresholds sharedThresholds = thresholds();
        return partial
                .scorer(scorer())
                .thresholds(sharedThresholds)
                .decisionEngine(new ThresholdDecisionEngine<>(sharedThresholds))
                .build();
    }

    private static List<StoredRecord> rankedCandidates(MatchResult<StoredRecord> result) {
        List<StoredRecord> ranked = new ArrayList<>();
        for (ScoredCandidate<StoredRecord> scored : result.getCandidates()) {
            ranked.add(scored.getCandidate());
        }
        return ranked;
    }

    // --- Fixture values -----------------------------------------------------
    //
    // "Seán" and the curly apostrophe in "O’Sullivan" are pasted literally
    // rather than escaped: both are ordinary, visible glyphs in this file's
    // encoding (an accented letter and a printable punctuation mark), not the
    // invisible-by-definition characters (combining marks, zero-width,
    // non-breaking space) docs/conventions.md#tests requires an escape for.
    // The curly apostrophe (U+2019) deliberately differs from the straight
    // one (U+0027) used elsewhere, so ApostropheVariantNormalizer folding
    // them together is what serial's EXACT category below depends on.

    private static IncomingRecord positiveSource() {
        return new IncomingRecord("Seán", "O'Sullivan", LocalDate.of(1985, 6, 14),
                Arrays.asList("12 Main Street", "Dublin 4"));
    }

    @Test
    void positiveScenarioMatchesTheBestScoringCandidate() {
        IncomingRecord source = positiveSource();
        // Curly apostrophe (U+2019) on the candidate side; ApostropheVariantNormalizer
        // folds it to the canonical U+0027 the source already uses.
        StoredRecord strongCandidate = new StoredRecord("stored-1", "John", "O’Sullivan", LocalDate.of(1985, 6, 14),
                "12 Main St. Dublin 4");
        StoredRecord distractor = new StoredRecord("stored-2", "Michael", "Byrne", LocalDate.of(1960, 1, 1), "9 Other Road");

        EntityResolver<IncomingRecord, StoredRecord> resolver = resolverWith(builder());
        MatchResult<StoredRecord> result = resolver.resolve(source, Arrays.asList(distractor, strongCandidate));

        // label: "sean" vs "john" (4 chars each) — Levenshtein edit
        // distance is 3 (three substitutions, "n" already matches), so
        // similarity = 1 - 3/4 = 0.25, below the 0.70 MEDIUM floor -> LOW (-5).
        // §88 expects this pair to resolve as ALIAS; there is no alias
        // repository (D7) in the merged code, so it falls through to a plain
        // string-similarity band instead.
        // serial: both normalize to "o'sullivan" -> EXACT (30).
        // issuedOn: both 1985-06-14 -> EXACT (25).
        // locator: normalized source "12 main street dublin 4" (23 chars),
        // normalized candidate "12 main st dublin 4" (19 chars); deleting
        // "reet" from "street" turns one into the other, so distance = 4 and
        // similarity = 1 - 4/23 = 19/23 ~= 0.826, in [0.70, 0.85) -> MEDIUM (15).
        // §88 expects VERY_HIGH here; there is no locator comparison pipeline
        // (D9) in the merged code, so this is plain Levenshtein similarity on
        // normalized strings, not the locator-aware match §88 illustrates.
        // total = -5 + 30 + 25 + 15 = 65
        assertThat(result.getDecision()).isEqualTo(Decision.MATCH);
        assertThat(result.getMatch()).isSameAs(strongCandidate);
        assertThat(result.getScore().getValue()).isEqualTo(65.0);
        assertThat(result.getScore().getScale()).isEqualTo(ScoreScale.POINTS);
        assertThat(rankedCandidates(result)).containsExactly(strongCandidate, distractor);

        // Pin each field's category individually, not only the total: a
        // future change that shifts two bands in opposite directions (e.g.
        // label up a band and locator down a band) could leave the total
        // unchanged and pass silently without this.
        List<FieldContribution> contributions = result.getCandidates().get(0).getContributions();
        assertThat(contributions)
                .filteredOn(c -> "label".equals(c.getField()))
                .extracting(FieldContribution::getCategory)
                .containsExactly(ComparisonCategory.LOW);
        assertThat(contributions)
                .filteredOn(c -> "serial".equals(c.getField()))
                .extracting(FieldContribution::getCategory)
                .containsExactly(ComparisonCategory.EXACT);
        assertThat(contributions)
                .filteredOn(c -> "issuedOn".equals(c.getField()))
                .extracting(FieldContribution::getCategory)
                .containsExactly(ComparisonCategory.EXACT);
        assertThat(contributions)
                .filteredOn(c -> "locator".equals(c.getField()))
                .extracting(FieldContribution::getCategory)
                .containsExactly(ComparisonCategory.MEDIUM);

        // Categories alone leave a weight change riding on the total
        // assertion above: swap two weights and every category here still
        // holds. Pinning the per-field contributions makes the weights
        // themselves part of what this test guarantees.
        assertThat(contributions)
                .extracting(FieldContribution::getField, FieldContribution::getContribution)
                .containsExactlyInAnyOrder(
                        org.assertj.core.api.Assertions.tuple("label", -5.0),
                        org.assertj.core.api.Assertions.tuple("serial", 30.0),
                        org.assertj.core.api.Assertions.tuple("issuedOn", 25.0),
                        org.assertj.core.api.Assertions.tuple("locator", 15.0));
    }

    @Test
    void negativeScenarioIsNoMatchWithNoCandidateSelected() {
        IncomingRecord source = new IncomingRecord("Seán", "O'Sullivan", LocalDate.of(1985, 6, 14),
                Collections.singletonList("12 Main Street"));
        StoredRecord wrongIssuedOn = new StoredRecord("stored-3", "John", "O'Sullivan", LocalDate.of(1974, 2, 10),
                "12 Main Street");

        EntityResolver<IncomingRecord, StoredRecord> resolver = resolverWith(builder());
        MatchResult<StoredRecord> result = resolver.resolve(source, Arrays.asList(wrongIssuedOn));

        // label: LOW (-5), as in the positive scenario.
        // serial: both "o'sullivan" -> EXACT (30).
        // issuedOn: 1985-06-14 vs 1974-02-10 -> CONFLICT (-100), strong
        // negative evidence per the worked example's requirement.
        // locator: both normalize to "12 main street" -> EXACT (40).
        // total = -5 + 30 - 100 + 40 = -35, below reviewThreshold (20) -> NO_MATCH
        assertThat(result.getDecision()).isEqualTo(Decision.NO_MATCH);
        assertThat(result.getMatch()).isNull();
        assertThat(result.getScore().getValue()).isEqualTo(-35.0);
    }

    @Test
    void ambiguousScenarioReturnsReviewOnAMarginBelowTheMinimum() {
        // D9: reproduced as a small margin rather than locator subsumption —
        // this milestone has no subsumption signal. "Dublin 4" and "Dublin 8"
        // are symmetric single-token extensions of the source's "Dublin", so
        // they land in the same similarity band and tie exactly.
        IncomingRecord source = new IncomingRecord("John", "Murphy", LocalDate.of(1985, 6, 14),
                Collections.singletonList("Dublin"));
        StoredRecord storedA = new StoredRecord("stored-a", "John", "Murphy", LocalDate.of(1985, 6, 14), "Dublin 4");
        StoredRecord storedB = new StoredRecord("stored-b", "John", "Murphy", LocalDate.of(1985, 6, 14), "Dublin 8");

        EntityResolver<IncomingRecord, StoredRecord> resolver = resolverWith(builder());
        MatchResult<StoredRecord> result = resolver.resolve(source, Arrays.asList(storedA, storedB));

        // label, serial, issuedOn all agree exactly for both storeds:
        // EXACT + EXACT + EXACT = 35 + 30 + 25 = 90 for each.
        // locator: normalized source "dublin" (6 chars) is a prefix of both
        // normalized candidates "dublin 4" and "dublin 8" (8 chars each), so
        // the edit distance is exactly the length difference, 2, for both;
        // similarity = 1 - 2/8 = 0.75, in [0.70, 0.85) -> MEDIUM (15) for both.
        // total = 90 + 15 = 105 for both storeds -> margin = 0.0
        assertThat(result.getDecision()).isEqualTo(Decision.REVIEW);
        assertThat(result.getMatch()).isNull();
        assertThat(result.hasSecondBest()).isTrue();
        assertThat(result.getMargin()).isEqualTo(0.0);
        assertThat(result.getScore().getValue()).isEqualTo(105.0);
        assertThat(result.getSecondBestScore().getValue()).isEqualTo(105.0);
    }

    @Test
    void ambiguousScenarioReturnsReviewOnASmallNonZeroMarginBelowTheMinimum() {
        // A companion to the exact 0.0 tie above: margin here is a small
        // positive value still below minimumMargin (10.0), exercising the
        // comparison as D9's locator pipeline will eventually produce rather
        // than only the degenerate exact-tie case.
        IncomingRecord source = new IncomingRecord("John", "Murphy", LocalDate.of(1985, 6, 14),
                Collections.singletonList("abcdefghij"));
        // storedHigh differs from the source locator in one character
        // (position 9, "i" -> "k") — same length, one substitution, so
        // distance = 1, similarity = 1 - 1/10 = 0.9, in [0.85, 0.95) -> HIGH (20).
        StoredRecord storedHigh = new StoredRecord("stored-high", "John", "Murphy", LocalDate.of(1985, 6, 14), "abcdefghik");
        // storedMedium differs in two characters (positions 9 and 10,
        // "ij" -> "kl") — same length, two substitutions, so distance = 2,
        // similarity = 1 - 2/10 = 0.8, in [0.70, 0.85) -> MEDIUM (15).
        StoredRecord storedMedium = new StoredRecord("stored-medium", "John", "Murphy", LocalDate.of(1985, 6, 14), "abcdefghkl");

        EntityResolver<IncomingRecord, StoredRecord> resolver = resolverWith(builder());
        MatchResult<StoredRecord> result = resolver.resolve(source, Arrays.asList(storedHigh, storedMedium));

        // label, serial, issuedOn all agree exactly for both storeds:
        // EXACT + EXACT + EXACT = 35 + 30 + 25 = 90 for each.
        // storedHigh total = 90 + 20 = 110. storedMedium total = 90 + 15 = 105.
        // Both clear matchThreshold (50); margin = 110 - 105 = 5.0, below
        // minimumMargin (10.0) -> REVIEW.
        assertThat(result.getDecision()).isEqualTo(Decision.REVIEW);
        assertThat(result.getMatch()).isNull();
        assertThat(result.hasSecondBest()).isTrue();
        assertThat(result.getMargin()).isEqualTo(5.0);
        assertThat(result.getScore().getValue()).isEqualTo(110.0);
        assertThat(result.getSecondBestScore().getValue()).isEqualTo(105.0);
    }

    @Test
    void missingValueOnOneSideIsMissingOneNotConflict() {
        IncomingRecord source = new IncomingRecord("Seán", "O'Sullivan", LocalDate.of(1985, 6, 14),
                Collections.singletonList("12 Main Street"));
        // The stored record has no label on file.
        StoredRecord candidate = new StoredRecord("stored-4", null, "O'Sullivan", LocalDate.of(1985, 6, 14), "12 Main Street");

        EntityResolver<IncomingRecord, StoredRecord> resolver = resolverWith(builder());
        MatchResult<StoredRecord> result = resolver.resolve(source, Arrays.asList(candidate));

        FieldContribution labelContribution = null;
        for (FieldContribution contribution : result.getCandidates().get(0).getContributions()) {
            if ("label".equals(contribution.getField())) {
                labelContribution = contribution;
            }
        }

        assertThat(labelContribution).isNotNull();
        assertThat(labelContribution.getCategory()).isEqualTo(ComparisonCategory.MISSING_ONE);

        // label: MISSING_ONE (0). serial: EXACT (30). issuedOn:
        // EXACT (25). locator: both normalize to "12 main street" -> EXACT (40).
        // total = 0 + 30 + 25 + 40 = 95
        assertThat(result.getDecision()).isEqualTo(Decision.MATCH);
        assertThat(result.getScore().getValue()).isEqualTo(95.0);
    }

    @Test
    void ruleVetoingOnAConflictingCheapFieldNeverInvokesTheExpensiveFieldPreparer() {
        Set<StoredRecord> locatorInvocations = new HashSet<>();
        Function<StoredRecord, String> countingLocatorExtractor = stored -> {
            locatorInvocations.add(stored);
            return stored.getLocator();
        };

        CandidateRule<IncomingRecord, StoredRecord> rejectOnConflictingSerial = (source, candidate, evidence) -> {
            FieldEvidence serialEvidence = evidence.getField("serial");
            return serialEvidence != null && serialEvidence.getCategory() == ComparisonCategory.CONFLICT
                    ? RuleDecision.REJECT
                    : RuleDecision.CONTINUE;
        };

        IncomingRecord source = positiveSource();
        StoredRecord surviving = new StoredRecord("stored-5", "John", "O'Sullivan", LocalDate.of(1985, 6, 14), "12 Main St. Dublin 4");
        // serial is CHEAP-tier and conflicts, so the rule rejects this
        // candidate before issuedOn (MODERATE) or locator (EXPENSIVE) run.
        StoredRecord vetoed = new StoredRecord("stored-6", "John", "Murphy", LocalDate.of(1985, 6, 14), "12 Main St. Dublin 4");

        EntityResolver<IncomingRecord, StoredRecord> resolver =
                resolverWith(builder(countingLocatorExtractor).rule(rejectOnConflictingSerial));
        MatchResult<StoredRecord> result = resolver.resolve(source, Arrays.asList(surviving, vetoed));

        assertThat(rankedCandidates(result)).containsExactly(surviving);
        assertThat(rankedCandidates(result)).doesNotContain(vetoed);
        // Proves the spy is actually wired (non-vacuous): it fires for the
        // surviving candidate, which does reach the EXPENSIVE tier.
        assertThat(locatorInvocations).contains(surviving);
        assertThat(locatorInvocations).doesNotContain(vetoed);
    }

    @Test
    void repeatedResolvesAgreeOnDecisionScoreAndOrdering() {
        EntityResolver<IncomingRecord, StoredRecord> resolver = resolverWith(builder());
        IncomingRecord source = positiveSource();
        StoredRecord candidate = new StoredRecord("stored-7", "John", "O'Sullivan", LocalDate.of(1985, 6, 14), "12 Main St. Dublin 4");

        MatchResult<StoredRecord> first = resolver.resolve(source, Arrays.asList(candidate));
        MatchResult<StoredRecord> second = resolver.resolve(source, Arrays.asList(candidate));

        assertThat(second.getDecision()).isEqualTo(first.getDecision());
        assertThat(second.getScore().getValue()).isEqualTo(first.getScore().getValue());
        assertThat(rankedCandidates(second)).containsExactlyElementsOf(rankedCandidates(first));
    }

    /**
     * Persists the determinism probe task 07 ran by hand (§96): the same
     * resolve, repeated across twenty shuffles of a four-candidate list, must
     * agree on decision, match and full candidate ordering every time.
     * Nothing before this test would fail the build if that broke.
     */
    @Test
    void resolveIsDeterministicAcrossShuffledCandidateOrder() {
        IncomingRecord source = positiveSource();

        // Four storeds with widely separated, hand-verified total scores so no
        // two can tie by accident and the ordering assertion is meaningful.
        //
        // storedBest: label LOW (-5) + serial EXACT (30) + issuedOn
        // EXACT (25) + locator MEDIUM (15) = 65 (identical construction to
        // the positive scenario above).
        StoredRecord storedBest = new StoredRecord("stored-best", "John", "O'Sullivan", LocalDate.of(1985, 6, 14),
                "12 Main St. Dublin 4");
        // storedMid: label LOW (-5) + serial CONFLICT (-30) + issuedOn
        // EXACT (25) + locator EXACT (40, both normalize to
        // "12 main street dublin 4") = 30.
        StoredRecord storedMid = new StoredRecord("stored-mid", "John", "Murphy", LocalDate.of(1985, 6, 14),
                "12 Main Street Dublin 4");
        // storedLow: label EXACT (35, matches source exactly) + serial
        // CONFLICT (-30) + issuedOn MISSING_ONE (-5, candidate has none) +
        // locator MEDIUM (15) = 15.
        StoredRecord storedLow = new StoredRecord("stored-low", "Seán", "Murphy", null, "12 Main St. Dublin 4");
        // storedWorst: label LOW (-5) + serial CONFLICT (-30) +
        // issuedOn CONFLICT (-100) + locator LOW (-5): normalized source
        // is 23 characters and normalized candidate "9 random lane" is 13,
        // so the edit distance is at least |23-13| = 10 regardless of
        // content, giving similarity <= 1 - 10/23 = 13/23 ~= 0.565 < 0.70,
        // guaranteeing LOW without computing the exact distance.
        // total = -5 - 30 - 100 - 5 = -140
        StoredRecord storedWorst = new StoredRecord("stored-worst", "John", "Murphy", LocalDate.of(1974, 2, 10), "9 Random Lane");

        List<StoredRecord> candidates = Arrays.asList(storedBest, storedMid, storedLow, storedWorst);
        EntityResolver<IncomingRecord, StoredRecord> resolver = resolverWith(builder());

        MatchResult<StoredRecord> baseline = resolver.resolve(source, candidates);
        assertThat(baseline.getDecision()).isEqualTo(Decision.MATCH);
        assertThat(baseline.getMatch()).isSameAs(storedBest);
        List<StoredRecord> baselineOrder = rankedCandidates(baseline);
        assertThat(baselineOrder).containsExactly(storedBest, storedMid, storedLow, storedWorst);

        Random random = new Random(42);
        for (int shuffle = 0; shuffle < 20; shuffle++) {
            List<StoredRecord> shuffled = new ArrayList<>(candidates);
            Collections.shuffle(shuffled, random);

            MatchResult<StoredRecord> result = resolver.resolve(source, shuffled);

            assertThat(result.getDecision()).isEqualTo(baseline.getDecision());
            assertThat(result.getMatch()).isSameAs(baseline.getMatch());
            assertThat(result.getScore().getValue()).isEqualTo(baseline.getScore().getValue());
            assertThat(rankedCandidates(result)).containsExactlyElementsOf(baselineOrder);
        }
    }

    /**
     * The shuffle test above deliberately keeps every score distinct, so it
     * never exercises {@link ThresholdDecisionEngine}'s stable-sort claim.
     * Tied candidates keeping their input order is the one case where §96
     * determinism is actually order-sensitive: a stable sort's guarantee is
     * about relative order of equal elements in whatever order they arrive
     * in, not about reproducing some other baseline order after a shuffle.
     * This asserts the ranked order follows the candidate list's own input
     * order for four candidates tied on score, in two different input orders.
     *
     * <p>Four rather than two, deliberately. With a pair, an unstable sort
     * would have to swap the only two elements there are to be caught — which
     * most sorts will not do — so the two-candidate version of this test
     * passed under sorts that are not stable at all. Four tied elements in a
     * reversed order give an unstable implementation somewhere to go wrong.
     */
    @Test
    void tiedCandidatesKeepTheirInputOrderUnderTheStableSort() {
        IncomingRecord source = positiveSource();
        // All four are constructed identically on every field the scorer
        // sees, so each scores label LOW (-5) + serial EXACT (30) +
        // issuedOn EXACT (25) + locator MEDIUM (15) = 65, an exact tie.
        StoredRecord tiedOne = new StoredRecord("stored-tied-1", "John", "O'Sullivan", LocalDate.of(1985, 6, 14),
                "12 Main St. Dublin 4");
        StoredRecord tiedTwo = new StoredRecord("stored-tied-2", "John", "O'Sullivan", LocalDate.of(1985, 6, 14),
                "12 Main St. Dublin 4");
        StoredRecord tiedThree = new StoredRecord("stored-tied-3", "John", "O'Sullivan", LocalDate.of(1985, 6, 14),
                "12 Main St. Dublin 4");
        StoredRecord tiedFour = new StoredRecord("stored-tied-4", "John", "O'Sullivan", LocalDate.of(1985, 6, 14),
                "12 Main St. Dublin 4");

        EntityResolver<IncomingRecord, StoredRecord> resolver = resolverWith(builder());

        MatchResult<StoredRecord> firstOrder =
                resolver.resolve(source, Arrays.asList(tiedOne, tiedTwo, tiedThree, tiedFour));
        assertThat(rankedCandidates(firstOrder))
                .containsExactly(tiedOne, tiedTwo, tiedThree, tiedFour);

        MatchResult<StoredRecord> reversedOrder =
                resolver.resolve(source, Arrays.asList(tiedFour, tiedThree, tiedTwo, tiedOne));
        assertThat(rankedCandidates(reversedOrder))
                .containsExactly(tiedFour, tiedThree, tiedTwo, tiedOne);
    }

    /**
     * Task 07/D6's documented-but-unenforced contract: {@code
     * .thresholds(...)} and the {@link ThresholdDecisionEngine} passed to
     * {@code .decisionEngine(...)} must be built from the same {@link
     * DecisionThresholds} instance. Written out fully rather than through
     * {@link #resolverWith} so the wiring this test is about is visible in
     * the test itself, not hidden in a shared helper.
     */
    @Test
    void followingTheSameInstanceThresholdsContractProducesTheDeclaredDecision() {
        DecisionThresholds sharedThresholds = new DecisionThresholds(50.0, 20.0, 10.0, ScoreScale.POINTS);

        EntityResolver<IncomingRecord, StoredRecord> resolver = builder()
                .scorer(scorer())
                .thresholds(sharedThresholds)
                .decisionEngine(new ThresholdDecisionEngine<>(sharedThresholds))
                .build();

        IncomingRecord source = positiveSource();
        StoredRecord candidate = new StoredRecord("stored-8", "John", "O'Sullivan", LocalDate.of(1985, 6, 14), "12 Main St. Dublin 4");

        // Same arithmetic as the positive scenario: total = 65, which is
        // >= sharedThresholds.getMatchThreshold() (50) -> MATCH.
        MatchResult<StoredRecord> result = resolver.resolve(source, Arrays.asList(candidate));

        assertThat(result.getDecision()).isEqualTo(Decision.MATCH);
        assertThat(result.getMatch()).isSameAs(candidate);
    }

    /**
     * D6, now closed. {@code EntityResolverBuilder.build()} inspects the
     * thresholds the configured {@link ThresholdDecisionEngine} actually
     * applies, via {@link
     * io.github.aindriub.jresolve.decision.MatchDecisionEngine#declaredThresholds()},
     * and rejects a configuration where those differ from the ones passed to
     * {@code .thresholds(...)}.
     *
     * <p>This test used to be a characterization test asserting the bug: two
     * different instances both passed {@code build()}, and {@code resolve()}
     * silently applied whichever the engine held. Its own Javadoc said that
     * when a later milestone let the engine expose its thresholds, "this
     * test's assertion of {@code MATCH} should then fail ... that failure is
     * the signal the D6 gap has closed, and this test should be updated at
     * that point, not before." That is what happened, and this is that
     * update — the same configuration, asserted to be rejected rather than
     * tolerated.
     */
    @Test
    void twoDifferentThresholdsInstancesAreRejectedAtBuild() {
        // Declared to build() — a threshold no real score could ever reach.
        DecisionThresholds declaredButUnused = new DecisionThresholds(200.0, 20.0, 10.0, ScoreScale.POINTS);
        // What the engine is actually constructed with.
        DecisionThresholds actuallyApplied = new DecisionThresholds(50.0, 20.0, 10.0, ScoreScale.POINTS);

        // Both are POINTS, so a scale-only check would have passed this
        // configuration: the divergence is in the values, not the scale.
        // That is why the engine declares its thresholds rather than only
        // their scale.
        assertThatThrownBy(() -> builder()
                .scorer(scorer())
                .thresholds(declaredButUnused)
                .decisionEngine(new ThresholdDecisionEngine<>(actuallyApplied))
                .build())
                .isInstanceOf(EntityResolutionConfigurationException.class)
                .hasMessageContaining("different");
    }

    /**
     * The same rejection for the case D6's note reproduced concretely: an
     * engine holding PROBABILITY thresholds while the scorer produces POINTS,
     * which would have compared a 10.0-point score against 0.9.
     */
    @Test
    void anEngineOnTheWrongScaleIsRejectedAtBuild() {
        DecisionThresholds points = new DecisionThresholds(50.0, 20.0, 10.0, ScoreScale.POINTS);
        DecisionThresholds probability = new DecisionThresholds(0.9, 0.5, 0.2, ScoreScale.PROBABILITY);

        assertThatThrownBy(() -> builder()
                .scorer(scorer())
                .thresholds(points)
                .decisionEngine(new ThresholdDecisionEngine<>(probability))
                .build())
                .isInstanceOf(EntityResolutionConfigurationException.class)
                .hasMessageContaining("scale");
    }

    /**
     * Equal-but-separately-constructed instances describe the same decision
     * rule and must keep building — the check is by value, not identity, so
     * that a caller who rebuilds an identical configuration is not punished
     * for it.
     */
    @Test
    void separatelyConstructedButEqualThresholdsStillBuild() {
        EntityResolver<IncomingRecord, StoredRecord> resolver = builder()
                .scorer(scorer())
                .thresholds(new DecisionThresholds(50.0, 20.0, 10.0, ScoreScale.POINTS))
                .decisionEngine(new ThresholdDecisionEngine<>(
                        new DecisionThresholds(50.0, 20.0, 10.0, ScoreScale.POINTS)))
                .build();

        MatchResult<StoredRecord> result = resolver.resolve(positiveSource(), Arrays.asList(
                new StoredRecord("stored-9", "John", "O'Sullivan", LocalDate.of(1985, 6, 14),
                        "12 Main St. Dublin 4")));

        assertThat(result.getDecision()).isEqualTo(Decision.MATCH);
    }
}