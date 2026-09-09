package io.github.aindriub.jresolve.endtoend;

import io.github.aindriub.jresolve.api.CandidateRule;
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

/**
 * The milestone gate: assembles a whole resolver from a consumer's seat —
 * {@link ExternalPerson} as the source, {@link Owner} as the candidate — and
 * proves the layers compose end to end.
 *
 * <p>Every source and candidate value used below is invented for this file;
 * none names a real person, and none is drawn from any public dataset.
 *
 * <p><strong>What this milestone does not yet cover:</strong> there is no
 * alias repository (D7) and no address comparison pipeline (D9) in the
 * merged code, so a source/candidate pair such as {@code "Seán"} and
 * {@code "John"} does not score as an alias match here — it falls through to
 * a low string-similarity band, exactly as an unrelated pair would. The
 * positive scenario below still resolves to {@code MATCH} because the other
 * three fields carry it, which is an honest demonstration of what this
 * library can do today, not a weakened stand-in for §88.
 */
class EndToEndResolutionTest {

    // --- Shared pipeline and scorer configuration -------------------------
    //
    // firstName and address both use Levenshtein similarity so every
    // expected category below is derived from a hand-computed edit distance
    // in each test's comment, never from a value produced by running the
    // comparator. lastName and dateOfBirth are exact-match fields, so their
    // categories follow directly from equality after normalization.

    private static FieldPipeline<String, String> firstNamePipeline() {
        StringNormalizer normalizer = new CompositeNormalizer(Arrays.asList(
                new UnicodeFormNormalizer(), new CombiningMarkNormalizer(), new CaseFoldNormalizer()));
        return new DefaultFieldPipeline<>(
                normalizer::normalize, new SimilarityFieldComparator(new LevenshteinSimilarity(), new SimilarityBands()));
    }

    private static FieldPipeline<String, String> lastNamePipeline() {
        StringNormalizer normalizer = new CompositeNormalizer(Arrays.asList(
                new UnicodeFormNormalizer(), new CombiningMarkNormalizer(),
                new ApostropheVariantNormalizer(), new CaseFoldNormalizer()));
        return new DefaultFieldPipeline<>(normalizer::normalize, new ExactFieldComparator<>());
    }

    private static FieldPipeline<LocalDate, LocalDate> dateOfBirthPipeline() {
        return new DefaultFieldPipeline<>(value -> value, new ExactFieldComparator<>());
    }

    private static final StringNormalizer ADDRESS_NORMALIZER = new CompositeNormalizer(Arrays.asList(
            new CaseFoldNormalizer(),
            new PunctuationNormalizer(new LinkedHashSet<>(Arrays.asList('.', ','))),
            new WhitespaceNormalizer()));

    private static String normalizeAddressLines(List<String> lines) {
        return ADDRESS_NORMALIZER.normalize(String.join(" ", lines));
    }

    private static String normalizeAddressLine(String line) {
        return ADDRESS_NORMALIZER.normalize(line);
    }

    private static FieldComparator<String> addressComparator() {
        return new SimilarityFieldComparator(new LevenshteinSimilarity(), new SimilarityBands());
    }

    /**
     * Four fields spanning two cost tiers: {@code firstName}/{@code
     * lastName} at the default {@link CostTiers#CHEAP}, {@code dateOfBirth}
     * at {@link CostTiers#MODERATE}, {@code address} at {@link
     * CostTiers#EXPENSIVE}. {@code address} uses the asymmetric overload —
     * {@link ExternalPerson#getAddress()} returns {@code List<String>},
     * {@link Owner#getAddress()} returns a single {@code String}.
     *
     * @param addressCandidateExtractor lets a test observe or replace how the
     *     candidate-side address is read, without duplicating the rest of
     *     the field configuration
     */
    private static EntityResolverBuilder<ExternalPerson, Owner> builder(
            Function<Owner, String> addressCandidateExtractor) {
        return EntityResolverBuilder.<ExternalPerson, Owner>builder()
                .field("firstName", ExternalPerson::getFirstName, Owner::getFirstName, firstNamePipeline())
                .field("lastName", ExternalPerson::getLastName, Owner::getLastName, lastNamePipeline())
                .field("dateOfBirth", ExternalPerson::getDateOfBirth, Owner::getDateOfBirth, dateOfBirthPipeline())
                .cost("dateOfBirth", CostTiers.MODERATE)
                .field("address", ExternalPerson::getAddress, addressCandidateExtractor,
                        EndToEndResolutionTest::normalizeAddressLines, EndToEndResolutionTest::normalizeAddressLine,
                        addressComparator())
                .cost("address", CostTiers.EXPENSIVE);
    }

    private static EntityResolverBuilder<ExternalPerson, Owner> builder() {
        return builder(Owner::getAddress);
    }

    /** Hand-chosen POINTS weights; every category any comparator above can emit is configured explicitly. */
    private static RuleBasedScorer scorer() {
        return RuleBasedScorer.builder()
                .weight("firstName", ComparisonCategory.EXACT, 35.0)
                .weight("firstName", ComparisonCategory.VERY_HIGH, 30.0)
                .weight("firstName", ComparisonCategory.HIGH, 20.0)
                .weight("firstName", ComparisonCategory.MEDIUM, 10.0)
                .weight("firstName", ComparisonCategory.LOW, -5.0)
                .weight("firstName", ComparisonCategory.MISSING_ONE, 0.0)
                .weight("firstName", ComparisonCategory.MISSING_BOTH, 0.0)
                .weight("lastName", ComparisonCategory.EXACT, 30.0)
                .weight("lastName", ComparisonCategory.CONFLICT, -30.0)
                .weight("lastName", ComparisonCategory.MISSING_ONE, 0.0)
                .weight("lastName", ComparisonCategory.MISSING_BOTH, 0.0)
                .weight("dateOfBirth", ComparisonCategory.EXACT, 25.0)
                .weight("dateOfBirth", ComparisonCategory.CONFLICT, -100.0)
                .weight("dateOfBirth", ComparisonCategory.MISSING_ONE, -5.0)
                .weight("dateOfBirth", ComparisonCategory.MISSING_BOTH, 0.0)
                .weight("address", ComparisonCategory.EXACT, 40.0)
                .weight("address", ComparisonCategory.VERY_HIGH, 35.0)
                .weight("address", ComparisonCategory.HIGH, 20.0)
                .weight("address", ComparisonCategory.MEDIUM, 15.0)
                .weight("address", ComparisonCategory.LOW, -5.0)
                .weight("address", ComparisonCategory.MISSING_ONE, 0.0)
                .weight("address", ComparisonCategory.MISSING_BOTH, 0.0)
                .baseScore(0.0)
                .build();
    }

    private static DecisionThresholds thresholds() {
        return new DecisionThresholds(50.0, 20.0, 10.0, ScoreScale.POINTS);
    }

    /**
     * Builds the resolver from {@code partial}, wiring {@link #scorer()} and
     * one {@link DecisionThresholds} instance into both {@code
     * .thresholds(...)} and the {@link ThresholdDecisionEngine} — the
     * same-instance contract {@link EntityResolverBuilder#thresholds}
     * documents but cannot enforce (task 07, D6 "Known gaps"). Using two
     * separately constructed instances here would build cleanly and then
     * silently decide against whichever thresholds the engine actually
     * holds; {@link #followingTheSameInstanceThresholdsContractProducesTheDeclaredDecision()}
     * and {@link #twoDifferentThresholdsInstancesSilentlyDecideAgainstTheEnginesOwnInstance()}
     * test that directly.
     */
    private static EntityResolver<ExternalPerson, Owner> resolverWith(EntityResolverBuilder<ExternalPerson, Owner> partial) {
        DecisionThresholds sharedThresholds = thresholds();
        return partial
                .scorer(scorer())
                .thresholds(sharedThresholds)
                .decisionEngine(new ThresholdDecisionEngine<>(sharedThresholds))
                .build();
    }

    private static List<Owner> rankedCandidates(MatchResult<Owner> result) {
        List<Owner> ranked = new ArrayList<>();
        for (ScoredCandidate<Owner> scored : result.getCandidates()) {
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
    // them together is what lastName's EXACT category below depends on.

    private static ExternalPerson positiveSource() {
        return new ExternalPerson("Seán", "O'Sullivan", LocalDate.of(1985, 6, 14),
                Arrays.asList("12 Main Street", "Dublin 4"));
    }

    @Test
    void positiveScenarioMatchesTheBestScoringCandidate() {
        ExternalPerson source = positiveSource();
        // Curly apostrophe (U+2019) on the candidate side; ApostropheVariantNormalizer
        // folds it to the canonical U+0027 the source already uses.
        Owner strongCandidate = new Owner("owner-1", "John", "O’Sullivan", LocalDate.of(1985, 6, 14),
                "12 Main St. Dublin 4");
        Owner distractor = new Owner("owner-2", "Michael", "Byrne", LocalDate.of(1960, 1, 1), "9 Other Road");

        EntityResolver<ExternalPerson, Owner> resolver = resolverWith(builder());
        MatchResult<Owner> result = resolver.resolve(source, Arrays.asList(distractor, strongCandidate));

        // firstName: "sean" vs "john" (4 chars each) — Levenshtein edit
        // distance is 3 (three substitutions, "n" already matches), so
        // similarity = 1 - 3/4 = 0.25, below the 0.70 MEDIUM floor -> LOW (-5).
        // lastName: both normalize to "o'sullivan" -> EXACT (30).
        // dateOfBirth: both 1985-06-14 -> EXACT (25).
        // address: normalized source "12 main street dublin 4" (23 chars),
        // normalized candidate "12 main st dublin 4" (19 chars); deleting
        // "reet" from "street" turns one into the other, so distance = 4 and
        // similarity = 1 - 4/23 = 19/23 ~= 0.826, in [0.70, 0.85) -> MEDIUM (15).
        // total = -5 + 30 + 25 + 15 = 65
        assertThat(result.getDecision()).isEqualTo(Decision.MATCH);
        assertThat(result.getMatch()).isSameAs(strongCandidate);
        assertThat(result.getScore().getValue()).isEqualTo(65.0);
        assertThat(result.getScore().getScale()).isEqualTo(ScoreScale.POINTS);
        assertThat(rankedCandidates(result)).containsExactly(strongCandidate, distractor);
    }

    @Test
    void negativeScenarioIsNoMatchWithNoCandidateSelected() {
        ExternalPerson source = new ExternalPerson("Seán", "O'Sullivan", LocalDate.of(1985, 6, 14),
                Collections.singletonList("12 Main Street"));
        Owner wrongDateOfBirth = new Owner("owner-3", "John", "O'Sullivan", LocalDate.of(1974, 2, 10),
                "12 Main Street");

        EntityResolver<ExternalPerson, Owner> resolver = resolverWith(builder());
        MatchResult<Owner> result = resolver.resolve(source, Arrays.asList(wrongDateOfBirth));

        // firstName: LOW (-5), as in the positive scenario.
        // lastName: both "o'sullivan" -> EXACT (30).
        // dateOfBirth: 1985-06-14 vs 1974-02-10 -> CONFLICT (-100), strong
        // negative evidence per the worked example's requirement.
        // address: both normalize to "12 main street" -> EXACT (40).
        // total = -5 + 30 - 100 + 40 = -35, below reviewThreshold (20) -> NO_MATCH
        assertThat(result.getDecision()).isEqualTo(Decision.NO_MATCH);
        assertThat(result.getMatch()).isNull();
        assertThat(result.getScore().getValue()).isEqualTo(-35.0);
    }

    @Test
    void ambiguousScenarioReturnsReviewOnAMarginBelowTheMinimum() {
        // D9: reproduced as a small margin rather than address subsumption —
        // this milestone has no subsumption signal. "Dublin 4" and "Dublin 8"
        // are symmetric single-token extensions of the source's "Dublin", so
        // they land in the same similarity band and tie exactly.
        ExternalPerson source = new ExternalPerson("John", "Murphy", LocalDate.of(1985, 6, 14),
                Collections.singletonList("Dublin"));
        Owner ownerA = new Owner("owner-a", "John", "Murphy", LocalDate.of(1985, 6, 14), "Dublin 4");
        Owner ownerB = new Owner("owner-b", "John", "Murphy", LocalDate.of(1985, 6, 14), "Dublin 8");

        EntityResolver<ExternalPerson, Owner> resolver = resolverWith(builder());
        MatchResult<Owner> result = resolver.resolve(source, Arrays.asList(ownerA, ownerB));

        // firstName, lastName, dateOfBirth all agree exactly for both owners:
        // EXACT + EXACT + EXACT = 35 + 30 + 25 = 90 for each.
        // address: normalized source "dublin" (6 chars) is a prefix of both
        // normalized candidates "dublin 4" and "dublin 8" (8 chars each), so
        // the edit distance is exactly the length difference, 2, for both;
        // similarity = 1 - 2/8 = 0.75, in [0.70, 0.85) -> MEDIUM (15) for both.
        // total = 90 + 15 = 105 for both owners -> margin = 0.0
        assertThat(result.getDecision()).isEqualTo(Decision.REVIEW);
        assertThat(result.getMatch()).isNull();
        assertThat(result.hasSecondBest()).isTrue();
        assertThat(result.getMargin()).isEqualTo(0.0);
        assertThat(result.getScore().getValue()).isEqualTo(105.0);
        assertThat(result.getSecondBestScore().getValue()).isEqualTo(105.0);
    }

    @Test
    void missingValueOnOneSideIsMissingOneNotConflict() {
        ExternalPerson source = new ExternalPerson("Seán", "O'Sullivan", LocalDate.of(1985, 6, 14),
                Collections.singletonList("12 Main Street"));
        // The owner record has no first name on file.
        Owner candidate = new Owner("owner-4", null, "O'Sullivan", LocalDate.of(1985, 6, 14), "12 Main Street");

        EntityResolver<ExternalPerson, Owner> resolver = resolverWith(builder());
        MatchResult<Owner> result = resolver.resolve(source, Arrays.asList(candidate));

        FieldContribution firstNameContribution = null;
        for (FieldContribution contribution : result.getCandidates().get(0).getContributions()) {
            if ("firstName".equals(contribution.getField())) {
                firstNameContribution = contribution;
            }
        }

        assertThat(firstNameContribution).isNotNull();
        assertThat(firstNameContribution.getCategory()).isEqualTo(ComparisonCategory.MISSING_ONE);
        assertThat(firstNameContribution.getCategory()).isNotEqualTo(ComparisonCategory.CONFLICT);

        // firstName: MISSING_ONE (0). lastName: EXACT (30). dateOfBirth:
        // EXACT (25). address: both normalize to "12 main street" -> EXACT (40).
        // total = 0 + 30 + 25 + 40 = 95
        assertThat(result.getDecision()).isEqualTo(Decision.MATCH);
        assertThat(result.getScore().getValue()).isEqualTo(95.0);
    }

    @Test
    void ruleVetoingOnAConflictingCheapFieldNeverInvokesTheExpensiveFieldPreparer() {
        Set<Owner> addressInvocations = new HashSet<>();
        Function<Owner, String> countingAddressExtractor = owner -> {
            addressInvocations.add(owner);
            return owner.getAddress();
        };

        CandidateRule<ExternalPerson, Owner> rejectOnConflictingLastName = (source, candidate, evidence) -> {
            FieldEvidence lastNameEvidence = evidence.getField("lastName");
            return lastNameEvidence != null && lastNameEvidence.getCategory() == ComparisonCategory.CONFLICT
                    ? RuleDecision.REJECT
                    : RuleDecision.CONTINUE;
        };

        ExternalPerson source = positiveSource();
        Owner surviving = new Owner("owner-5", "John", "O'Sullivan", LocalDate.of(1985, 6, 14), "12 Main St. Dublin 4");
        // lastName is CHEAP-tier and conflicts, so the rule rejects this
        // candidate before dateOfBirth (MODERATE) or address (EXPENSIVE) run.
        Owner vetoed = new Owner("owner-6", "John", "Murphy", LocalDate.of(1985, 6, 14), "12 Main St. Dublin 4");

        EntityResolver<ExternalPerson, Owner> resolver =
                resolverWith(builder(countingAddressExtractor).rule(rejectOnConflictingLastName));
        MatchResult<Owner> result = resolver.resolve(source, Arrays.asList(surviving, vetoed));

        assertThat(rankedCandidates(result)).containsExactly(surviving);
        assertThat(rankedCandidates(result)).doesNotContain(vetoed);
        // Proves the spy is actually wired (non-vacuous): it fires for the
        // surviving candidate, which does reach the EXPENSIVE tier.
        assertThat(addressInvocations).contains(surviving);
        assertThat(addressInvocations).doesNotContain(vetoed);
    }

    @Test
    void repeatedResolvesAgreeOnDecisionScoreAndOrdering() {
        EntityResolver<ExternalPerson, Owner> resolver = resolverWith(builder());
        ExternalPerson source = positiveSource();
        Owner candidate = new Owner("owner-7", "John", "O'Sullivan", LocalDate.of(1985, 6, 14), "12 Main St. Dublin 4");

        MatchResult<Owner> first = resolver.resolve(source, Arrays.asList(candidate));
        MatchResult<Owner> second = resolver.resolve(source, Arrays.asList(candidate));

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
        ExternalPerson source = positiveSource();

        // Four owners with widely separated, hand-verified total scores so no
        // two can tie by accident and the ordering assertion is meaningful.
        //
        // ownerBest: firstName LOW (-5) + lastName EXACT (30) + dateOfBirth
        // EXACT (25) + address MEDIUM (15) = 65 (identical construction to
        // the positive scenario above).
        Owner ownerBest = new Owner("owner-best", "John", "O'Sullivan", LocalDate.of(1985, 6, 14),
                "12 Main St. Dublin 4");
        // ownerMid: firstName LOW (-5) + lastName CONFLICT (-30) + dateOfBirth
        // EXACT (25) + address EXACT (40, both normalize to
        // "12 main street dublin 4") = 30.
        Owner ownerMid = new Owner("owner-mid", "John", "Murphy", LocalDate.of(1985, 6, 14),
                "12 Main Street Dublin 4");
        // ownerLow: firstName EXACT (35, matches source exactly) + lastName
        // CONFLICT (-30) + dateOfBirth MISSING_ONE (-5, candidate has none) +
        // address MEDIUM (15) = 15.
        Owner ownerLow = new Owner("owner-low", "Seán", "Murphy", null, "12 Main St. Dublin 4");
        // ownerWorst: firstName LOW (-5) + lastName CONFLICT (-30) +
        // dateOfBirth CONFLICT (-100) + address LOW (-5): normalized source
        // is 23 characters and normalized candidate "9 random lane" is 13,
        // so the edit distance is at least |23-13| = 10 regardless of
        // content, giving similarity <= 1 - 10/23 = 13/23 ~= 0.565 < 0.70,
        // guaranteeing LOW without computing the exact distance.
        // total = -5 - 30 - 100 - 5 = -140
        Owner ownerWorst = new Owner("owner-worst", "John", "Murphy", LocalDate.of(1974, 2, 10), "9 Random Lane");

        List<Owner> candidates = Arrays.asList(ownerBest, ownerMid, ownerLow, ownerWorst);
        EntityResolver<ExternalPerson, Owner> resolver = resolverWith(builder());

        MatchResult<Owner> baseline = resolver.resolve(source, candidates);
        assertThat(baseline.getDecision()).isEqualTo(Decision.MATCH);
        assertThat(baseline.getMatch()).isSameAs(ownerBest);
        List<Owner> baselineOrder = rankedCandidates(baseline);
        assertThat(baselineOrder).containsExactly(ownerBest, ownerMid, ownerLow, ownerWorst);

        Random random = new Random(42);
        for (int shuffle = 0; shuffle < 20; shuffle++) {
            List<Owner> shuffled = new ArrayList<>(candidates);
            Collections.shuffle(shuffled, random);

            MatchResult<Owner> result = resolver.resolve(source, shuffled);

            assertThat(result.getDecision()).isEqualTo(baseline.getDecision());
            assertThat(result.getMatch()).isSameAs(baseline.getMatch());
            assertThat(result.getScore().getValue()).isEqualTo(baseline.getScore().getValue());
            assertThat(rankedCandidates(result)).containsExactlyElementsOf(baselineOrder);
        }
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

        EntityResolver<ExternalPerson, Owner> resolver = builder()
                .scorer(scorer())
                .thresholds(sharedThresholds)
                .decisionEngine(new ThresholdDecisionEngine<>(sharedThresholds))
                .build();

        ExternalPerson source = positiveSource();
        Owner candidate = new Owner("owner-8", "John", "O'Sullivan", LocalDate.of(1985, 6, 14), "12 Main St. Dublin 4");

        // Same arithmetic as the positive scenario: total = 65, which is
        // >= sharedThresholds.getMatchThreshold() (50) -> MATCH.
        MatchResult<Owner> result = resolver.resolve(source, Arrays.asList(candidate));

        assertThat(result.getDecision()).isEqualTo(Decision.MATCH);
        assertThat(result.getMatch()).isSameAs(candidate);
    }

    /**
     * The documented gap itself: {@code EntityResolverBuilder.build()} only
     * checks the {@link ScoreScale} of the {@code DecisionThresholds}
     * instance passed to {@code .thresholds(...)}; it cannot see, and does
     * not check, whether that instance is the same one backing the {@link
     * ThresholdDecisionEngine} passed to {@code .decisionEngine(...)}. Two
     * different instances with different values both pass {@code build()}
     * and {@code resolve()} silently applies whichever one the engine
     * actually holds, not the one named in {@code .thresholds(...)}.
     */
    @Test
    void twoDifferentThresholdsInstancesSilentlyDecideAgainstTheEnginesOwnInstance() {
        // Declared to build() — a threshold no real score could ever reach.
        DecisionThresholds declaredButUnused = new DecisionThresholds(200.0, 20.0, 10.0, ScoreScale.POINTS);
        // What the engine is actually constructed with.
        DecisionThresholds actuallyApplied = new DecisionThresholds(50.0, 20.0, 10.0, ScoreScale.POINTS);

        EntityResolver<ExternalPerson, Owner> resolver = builder()
                .scorer(scorer())
                .thresholds(declaredButUnused)
                .decisionEngine(new ThresholdDecisionEngine<>(actuallyApplied))
                .build();

        ExternalPerson source = positiveSource();
        Owner candidate = new Owner("owner-9", "John", "O'Sullivan", LocalDate.of(1985, 6, 14), "12 Main St. Dublin 4");

        // total = 65 (same arithmetic as the positive scenario): below
        // declaredButUnused's matchThreshold (200) but above
        // actuallyApplied's (50). build() did not fail despite the two
        // instances disagreeing, and the decision below follows
        // actuallyApplied, proving the instance passed to .thresholds(...)
        // was never consulted at resolve() time.
        MatchResult<Owner> result = resolver.resolve(source, Arrays.asList(candidate));

        assertThat(result.getDecision()).isEqualTo(Decision.MATCH);
    }
}
