package io.github.aindriub.jresolve.profiles.ie.endtoend;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aindriub.jresolve.api.EntityResolver;
import io.github.aindriub.jresolve.api.EntityResolverBuilder;
import io.github.aindriub.jresolve.comparison.LevenshteinSimilarity;
import io.github.aindriub.jresolve.decision.DecisionThresholds;
import io.github.aindriub.jresolve.decision.ThresholdDecisionEngine;
import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.FieldEvidence;
import io.github.aindriub.jresolve.evidence.TokenSubsumption;
import io.github.aindriub.jresolve.field.CostTiers;
import io.github.aindriub.jresolve.field.ExactFieldComparator;
import io.github.aindriub.jresolve.field.FieldComparator;
import io.github.aindriub.jresolve.field.FieldPipeline;
import io.github.aindriub.jresolve.field.SimilarityBands;
import io.github.aindriub.jresolve.field.SimilarityFieldComparator;
import io.github.aindriub.jresolve.field.TokenSubsumptionComparator;
import io.github.aindriub.jresolve.profiles.ie.IrishAddressComparator;
import io.github.aindriub.jresolve.profiles.ie.IrishAddressPipeline;
import io.github.aindriub.jresolve.profiles.ie.IrishNameAliases;
import io.github.aindriub.jresolve.profiles.ie.IrishNamePipeline;
import io.github.aindriub.jresolve.result.Decision;
import io.github.aindriub.jresolve.result.FieldContribution;
import io.github.aindriub.jresolve.result.MatchResult;
import io.github.aindriub.jresolve.result.ScoreScale;
import io.github.aindriub.jresolve.result.ScoredCandidate;
import io.github.aindriub.jresolve.scoring.RuleBasedScorer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The milestone's thesis, tested as a falsifiable claim.
 *
 * <p>Milestone 1 closed with a specific charge from its reviewer: every
 * positive outcome in the end-to-end suite was carried by exact surname and
 * exact date of birth — the two columns a plain SQL join would match on — so
 * the layers were shown to compose, and resolution quality was not shown at
 * all. D7 (alias equivalence groups) and D9 (address subsumption) were named
 * as the gap between "the pieces compose" and "a consumer gets a better
 * answer than a join".
 *
 * <p>So the claim is stated here in the form that can fail: <strong>a resolve
 * that succeeds with the real pipelines must fail under exact-only
 * comparison</strong>, asserted in one test so the contrast cannot rot on one
 * side while the other keeps passing. If D7 and D9 bought nothing, that test
 * goes red rather than green.
 *
 * <p>The exact-only resolver it is measured against is a <em>generous</em>
 * join: it keeps the same normalization, the same fields, the same weights
 * and the same thresholds, and changes only the comparators. A real SQL join
 * would not fold a fada or a curly apostrophe for free. Beating that is a
 * stronger result than beating a literal string equality.
 *
 * <p>Every value below is invented; see {@link SourcePerson}.
 */
class BeatTheJoinTest {

    // --- Configuration ------------------------------------------------------

    private static FieldPipeline<String, String> namePipeline() {
        // One repository serves both name fields: it carries given-name
        // translations and diminutives as well as the surname forms.
        return IrishNamePipeline.withAliases(IrishNameAliases.repository());
    }

    private static final FieldPipeline<List<String>, String> ADDRESS_LINES =
            IrishAddressPipeline.forLines();
    private static final FieldPipeline<String, String> ADDRESS_SINGLE =
            IrishAddressPipeline.forSingleLine();

    /**
     * The resolver under test: alias-aware names, containment-aware address.
     *
     * @param nameComparator     null to use the real alias-aware pipeline,
     *                           or a comparator to stand in for it
     * @param addressComparator  likewise for the address field
     */
    private static EntityResolverBuilder<SourcePerson, CandidatePerson> builder(
            FieldComparator<String> nameComparator, FieldComparator<String> addressComparator) {
        FieldPipeline<String, String> names = namePipeline();
        EntityResolverBuilder<SourcePerson, CandidatePerson> builder =
                EntityResolverBuilder.<SourcePerson, CandidatePerson>builder();
        if (nameComparator == null) {
            builder.field("firstName", SourcePerson::getFirstName,
                            CandidatePerson::getFirstName, names)
                    .field("lastName", SourcePerson::getLastName,
                            CandidatePerson::getLastName, names);
        } else {
            builder.field("firstName", SourcePerson::getFirstName, CandidatePerson::getFirstName,
                            names::prepare, names::prepare, nameComparator)
                    .field("lastName", SourcePerson::getLastName, CandidatePerson::getLastName,
                            names::prepare, names::prepare, nameComparator);
        }
        return builder
                .field("dateOfBirth", SourcePerson::getDateOfBirth, CandidatePerson::getDateOfBirth,
                        value -> value, value -> value, new ExactFieldComparator<LocalDate>())
                .cost("dateOfBirth", CostTiers.MODERATE)
                .field("address", SourcePerson::getAddressLines, CandidatePerson::getAddress,
                        ADDRESS_LINES::prepare, ADDRESS_SINGLE::prepare, addressComparator)
                .cost("address", CostTiers.EXPENSIVE);
    }

    /** The real thing: D7's aliases and D9's containment. */
    private static EntityResolverBuilder<SourcePerson, CandidatePerson> realBuilder() {
        return builder(null, new IrishAddressComparator());
    }

    /**
     * The control: same normalization, same weights, same thresholds, only
     * equality where the real resolver compares. This is what "a join would
     * have got" means throughout this file.
     */
    private static EntityResolverBuilder<SourcePerson, CandidatePerson> exactOnlyBuilder() {
        return builder(new ExactFieldComparator<String>(), new ExactFieldComparator<String>());
    }

    /**
     * Weights in POINTS. Every category the comparators above can emit is
     * configured, because an unconfigured category silently scores zero —
     * which would make a missing weight look like a neutral finding.
     *
     * <p>{@code PARTIAL_OVERLAP} and address {@code CONFLICT} are configured
     * although {@link IrishAddressComparator} cannot currently emit them: it
     * bands those two cases instead. They are here so that a later change to
     * that comparator surfaces as a wrong score rather than as a silent zero.
     */
    private static RuleBasedScorer scorer() {
        return RuleBasedScorer.builder()
                .weight("firstName", ComparisonCategory.EXACT, 30.0)
                .weight("firstName", ComparisonCategory.ALIAS_VARIANT, 28.0)
                .weight("firstName", ComparisonCategory.ALIAS_NICKNAME, 25.0)
                .weight("firstName", ComparisonCategory.ALIAS_TRANSLATION, 25.0)
                .weight("firstName", ComparisonCategory.VERY_HIGH, 20.0)
                .weight("firstName", ComparisonCategory.HIGH, 12.0)
                .weight("firstName", ComparisonCategory.MEDIUM, 5.0)
                .weight("firstName", ComparisonCategory.LOW, -5.0)
                .weight("firstName", ComparisonCategory.CONFLICT, -10.0)
                .weight("firstName", ComparisonCategory.MISSING_ONE, 0.0)
                .weight("firstName", ComparisonCategory.MISSING_BOTH, 0.0)
                .weight("lastName", ComparisonCategory.EXACT, 35.0)
                .weight("lastName", ComparisonCategory.ALIAS_VARIANT, 32.0)
                .weight("lastName", ComparisonCategory.ALIAS_NICKNAME, 30.0)
                .weight("lastName", ComparisonCategory.ALIAS_TRANSLATION, 30.0)
                .weight("lastName", ComparisonCategory.VERY_HIGH, 22.0)
                .weight("lastName", ComparisonCategory.HIGH, 14.0)
                .weight("lastName", ComparisonCategory.MEDIUM, 6.0)
                .weight("lastName", ComparisonCategory.LOW, -5.0)
                .weight("lastName", ComparisonCategory.CONFLICT, -30.0)
                .weight("lastName", ComparisonCategory.MISSING_ONE, 0.0)
                .weight("lastName", ComparisonCategory.MISSING_BOTH, 0.0)
                .weight("dateOfBirth", ComparisonCategory.EXACT, 25.0)
                .weight("dateOfBirth", ComparisonCategory.CONFLICT, -100.0)
                .weight("dateOfBirth", ComparisonCategory.MISSING_ONE, -5.0)
                .weight("dateOfBirth", ComparisonCategory.MISSING_BOTH, 0.0)
                .weight("address", ComparisonCategory.EXACT, 30.0)
                .weight("address", TokenSubsumptionComparator.SUBSUMED, 20.0)
                .weight("address", TokenSubsumptionComparator.PARTIAL_OVERLAP, 5.0)
                .weight("address", ComparisonCategory.VERY_HIGH, 25.0)
                .weight("address", ComparisonCategory.HIGH, 15.0)
                .weight("address", ComparisonCategory.MEDIUM, 8.0)
                .weight("address", ComparisonCategory.LOW, -5.0)
                .weight("address", ComparisonCategory.CONFLICT, -10.0)
                .weight("address", ComparisonCategory.MISSING_ONE, 0.0)
                .weight("address", ComparisonCategory.MISSING_BOTH, 0.0)
                .baseScore(0.0)
                .build();
    }

    private static EntityResolver<SourcePerson, CandidatePerson> resolverWith(
            EntityResolverBuilder<SourcePerson, CandidatePerson> partial) {
        // One DecisionThresholds instance into both .thresholds(...) and the
        // engine — the same-instance contract task 07 documented but cannot
        // enforce (D6, still open).
        DecisionThresholds shared = new DecisionThresholds(50.0, 20.0, 10.0, ScoreScale.POINTS);
        return partial.scorer(scorer())
                .thresholds(shared)
                .decisionEngine(new ThresholdDecisionEngine<CandidatePerson>(shared))
                .build();
    }

    private static ComparisonCategory categoryOf(MatchResult<CandidatePerson> result, String field) {
        for (FieldContribution contribution : result.getCandidates().get(0).getContributions()) {
            if (field.equals(contribution.getField())) {
                return contribution.getCategory();
            }
        }
        throw new AssertionError("no contribution for field " + field);
    }

    private static TokenSubsumption subsumptionOf(
            ScoredCandidate<CandidatePerson> scored, String field) {
        for (FieldContribution contribution : scored.getContributions()) {
            if (field.equals(contribution.getField())) {
                return contribution.getSubsumption();
            }
        }
        throw new AssertionError("no contribution for field " + field);
    }

    private static List<String> rankedIds(MatchResult<CandidatePerson> result) {
        List<String> ids = new ArrayList<>();
        for (ScoredCandidate<CandidatePerson> scored : result.getCandidates()) {
            ids.add(scored.getCandidate().getId());
        }
        return ids;
    }

    // --- Fixtures -----------------------------------------------------------

    /** Irish given name, Irish surname, no date of birth in the feed. */
    private static SourcePerson sourceWithoutDateOfBirth() {
        return new SourcePerson("Seán", "Ó Súilleabháin", null,
                Arrays.asList("12 Main Street", "Dublin 4"));
    }

    /** The same person in the register, in English, with the same address. */
    private static CandidatePerson registeredInEnglish() {
        return new CandidatePerson("registered-1", "John", "O'Sullivan", null,
                "12 Main Street, Dublin 4");
    }

    private static CandidatePerson unrelatedPerson() {
        return new CandidatePerson("registered-2", "Michael", "Byrne", null, "9 Other Road");
    }

    // --- THE FALSIFICATION TEST --------------------------------------------

    @Test
    void aResolveThatSucceedsHereFailsUnderExactOnlyComparison() {
        SourcePerson source = sourceWithoutDateOfBirth();
        List<CandidatePerson> candidates =
                Arrays.asList(unrelatedPerson(), registeredInEnglish());

        // With D7 and D9:
        //   firstName  Seán / John            -> ALIAS_TRANSLATION  (+25)
        //   lastName   Ó Súilleabháin / O'Sullivan -> ALIAS_TRANSLATION (+30)
        //   dateOfBirth  absent on both sides -> MISSING_BOTH        (  0)
        //   address    both prepare to "12 main street dublin 4" -> EXACT (+30)
        //   total 85, above the 50-point match threshold.
        MatchResult<CandidatePerson> withPipelines =
                resolverWith(realBuilder()).resolve(source, candidates);

        // Without them, on the same records, the same weights and the same
        // thresholds — only equality in place of the two comparators:
        //   firstName  CONFLICT (-10), lastName CONFLICT (-30),
        //   dateOfBirth MISSING_BOTH (0), address still EXACT (+30)
        //   total -10, below even the 20-point review threshold.
        MatchResult<CandidatePerson> withoutPipelines =
                resolverWith(exactOnlyBuilder()).resolve(source, candidates);

        assertThat(withPipelines.getDecision()).isSameAs(Decision.MATCH);
        assertThat(withPipelines.getMatch().getId()).isEqualTo("registered-1");
        assertThat(withPipelines.getScore().getValue()).isEqualTo(85.0);

        assertThat(withoutPipelines.getDecision()).isSameAs(Decision.NO_MATCH);
        assertThat(withoutPipelines.getMatch()).isNull();

        // The join is handed an exact address and still cannot match, because
        // the two name fields are correct renderings it has no way to see.
        assertThat(categoryOf(withoutPipelines, "address")).isSameAs(ComparisonCategory.EXACT);
    }

    @Test
    void theTwoNameFieldsAreWhatTheJoinCannotSee() {
        SourcePerson source = sourceWithoutDateOfBirth();
        List<CandidatePerson> candidates = Collections.singletonList(registeredInEnglish());

        MatchResult<CandidatePerson> real = resolverWith(realBuilder()).resolve(source, candidates);
        MatchResult<CandidatePerson> exact =
                resolverWith(exactOnlyBuilder()).resolve(source, candidates);

        assertThat(categoryOf(real, "firstName")).isSameAs(ComparisonCategory.ALIAS_TRANSLATION);
        assertThat(categoryOf(real, "lastName")).isSameAs(ComparisonCategory.ALIAS_TRANSLATION);
        assertThat(categoryOf(exact, "firstName")).isSameAs(ComparisonCategory.CONFLICT);
        assertThat(categoryOf(exact, "lastName")).isSameAs(ComparisonCategory.CONFLICT);
    }

    // --- §88's expected bands ----------------------------------------------

    @Test
    void theBandsSection88ExpectsAreNowReached() {
        // Milestone 1 had to record both of these as unmet: §88 expects the
        // firstName pair to resolve as an alias and the address pair to band
        // VERY_HIGH, and that suite produced LOW and MEDIUM instead. Asserted
        // by value here rather than described, because a comment claiming a
        // band is not a test of it.
        SourcePerson source = new SourcePerson("Seán", "Ó Súilleabháin",
                LocalDate.of(1985, 6, 14), Arrays.asList("12 Main Street", "Dublin 4"));
        CandidatePerson candidate = new CandidatePerson("registered-3", "John", "O'Sullivan",
                LocalDate.of(1985, 6, 14), "12 Main St. Dublin 4");

        MatchResult<CandidatePerson> result = resolverWith(realBuilder())
                .resolve(source, Collections.singletonList(candidate));

        assertThat(categoryOf(result, "firstName")).isSameAs(ComparisonCategory.ALIAS_TRANSLATION);
        assertThat(categoryOf(result, "address")).isSameAs(ComparisonCategory.VERY_HIGH);
        assertThat(categoryOf(result, "lastName")).isSameAs(ComparisonCategory.ALIAS_TRANSLATION);
        assertThat(categoryOf(result, "dateOfBirth")).isSameAs(ComparisonCategory.EXACT);
        assertThat(result.getDecision()).isSameAs(Decision.MATCH);
        assertThat(result.getScore().getValue()).isEqualTo(105.0);
    }

    @Test
    void theAddressPipelineOutscoresAGenericStringComparatorOnTheSamePair() {
        // The same prepared pair, banded two ways. The generic comparator is
        // what milestone 1's suite used, and it is what the address field
        // would still be doing without D9.
        String left = ADDRESS_LINES.prepare(Arrays.asList("12 Main Street", "Dublin 4"));
        String right = ADDRESS_SINGLE.prepare("12 Main St. Dublin 4");

        FieldEvidence real = new IrishAddressComparator().compare(left, right);
        FieldEvidence generic = new SimilarityFieldComparator(
                new LevenshteinSimilarity(), new SimilarityBands()).compare(left, right);

        assertThat(real.getCategory()).isSameAs(ComparisonCategory.VERY_HIGH);
        assertThat(generic.getCategory()).isSameAs(ComparisonCategory.MEDIUM);
        assertThat(real.getCategory()).isNotSameAs(generic.getCategory());
    }

    // --- D9's ambiguous source (§90) ---------------------------------------

    @Test
    void aLessSpecificSourceAgainstTwoFullerCandidatesIsReviewed() {
        // §90: the source says "Dublin", two candidates say "Dublin 4" and
        // "Dublin 8". Both contain the source and neither contradicts it, so
        // both score identically and the margin is zero — REVIEW, and for the
        // right reason rather than by accident.
        SourcePerson source = new SourcePerson("Seán", "Ó Súilleabháin",
                LocalDate.of(1985, 6, 14), Collections.singletonList("Dublin"));
        CandidatePerson dublinFour = new CandidatePerson("registered-4", "Seán",
                "Ó Súilleabháin", LocalDate.of(1985, 6, 14), "Dublin 4");
        CandidatePerson dublinEight = new CandidatePerson("registered-8", "Seán",
                "Ó Súilleabháin", LocalDate.of(1985, 6, 14), "Dublin 8");

        MatchResult<CandidatePerson> result = resolverWith(realBuilder())
                .resolve(source, Arrays.asList(dublinFour, dublinEight));

        assertThat(result.getDecision()).isSameAs(Decision.REVIEW);
        assertThat(result.getMatch()).isNull();
        assertThat(result.hasSecondBest()).isTrue();
        assertThat(result.getMargin()).isEqualTo(0.0);

        // Containment, not conflict — the finding that makes the tie honest.
        assertThat(categoryOf(result, "address")).isSameAs(TokenSubsumptionComparator.SUBSUMED);
        assertThat(result.getCandidates()).hasSize(2);
        for (ScoredCandidate<CandidatePerson> scored : result.getCandidates()) {
            assertThat(scored.getScore().getValue()).isEqualTo(110.0);
        }
    }

    @Test
    void bothCandidatesSubsumeTheSourceInTheSameDirection() {
        // Asserted through MatchResult, which is the point. Task 16 had to
        // reach past the result into the pipeline for this, because
        // FieldContribution carried a category and no containment signal —
        // so a consumer could see *that* two candidates tied and not that
        // each contains the source. Task 18 closed that; this is the test
        // that proves it from a consumer's seat.
        SourcePerson source = new SourcePerson("Seán", "Ó Súilleabháin",
                LocalDate.of(1985, 6, 14), Collections.singletonList("Dublin"));
        CandidatePerson dublinFour = new CandidatePerson("registered-4", "Seán",
                "Ó Súilleabháin", LocalDate.of(1985, 6, 14), "Dublin 4");
        CandidatePerson dublinEight = new CandidatePerson("registered-8", "Seán",
                "Ó Súilleabháin", LocalDate.of(1985, 6, 14), "Dublin 8");

        MatchResult<CandidatePerson> result = resolverWith(realBuilder())
                .resolve(source, Arrays.asList(dublinFour, dublinEight));

        assertThat(result.getCandidates()).hasSize(2);
        for (ScoredCandidate<CandidatePerson> scored : result.getCandidates()) {
            assertThat(subsumptionOf(scored, "address"))
                    .isSameAs(TokenSubsumption.LEFT_SUBSUMES_RIGHT);
        }
    }

    @Test
    void aConsumerCanTellAGenuineTieFromACoincidentalOne() {
        // Two candidates tied on score. Reading only the score and the
        // category, these are indistinguishable from any other tie; the
        // containment signal is what says the tie is genuine — each
        // candidate contains the source and neither contradicts it — rather
        // than two unrelated records happening to total the same.
        SourcePerson source = new SourcePerson("Seán", "Ó Súilleabháin",
                LocalDate.of(1985, 6, 14), Collections.singletonList("Dublin"));
        MatchResult<CandidatePerson> result = resolverWith(realBuilder()).resolve(source, Arrays.asList(
                new CandidatePerson("registered-4", "Seán", "Ó Súilleabháin",
                        LocalDate.of(1985, 6, 14), "Dublin 4"),
                new CandidatePerson("registered-8", "Seán", "Ó Súilleabháin",
                        LocalDate.of(1985, 6, 14), "Dublin 8")));

        assertThat(result.getDecision()).isSameAs(Decision.REVIEW);
        assertThat(result.getMargin()).isEqualTo(0.0);
        for (ScoredCandidate<CandidatePerson> scored : result.getCandidates()) {
            assertThat(subsumptionOf(scored, "address")).isNotSameAs(TokenSubsumption.NEITHER);
            assertThat(subsumptionOf(scored, "address")).isNotSameAs(TokenSubsumption.NOT_APPLICABLE);
        }
    }

    @Test
    void aFieldWhoseComparatorIgnoresTokensReportsNotApplicableThroughTheResult() {
        // The distinction survives the trip to the consumer: the name fields
        // are compared by a comparator that does not reason about tokens, and
        // say so, rather than claiming containment was computed and failed.
        MatchResult<CandidatePerson> result = resolverWith(realBuilder())
                .resolve(sourceWithoutDateOfBirth(), Collections.singletonList(registeredInEnglish()));

        assertThat(subsumptionOf(result.getCandidates().get(0), "lastName"))
                .isSameAs(TokenSubsumption.NOT_APPLICABLE);
    }

    // --- The alias layer does not over-match --------------------------------

    @Test
    void twoGenuinelyDifferentPeopleDoNotMatch() {
        SourcePerson source = sourceWithoutDateOfBirth();

        MatchResult<CandidatePerson> result = resolverWith(realBuilder())
                .resolve(source, Collections.singletonList(unrelatedPerson()));

        assertThat(result.getDecision()).isSameAs(Decision.NO_MATCH);
        assertThat(result.getMatch()).isNull();
    }

    @Test
    void namesOutsideAnAliasGroupAreNotTreatedAsAliases() {
        // Seán and Kevin are both in the table, in different groups. Being
        // present is not being related — an alias layer that answered "yes"
        // for any two known values would be worse than none.
        FieldPipeline<String, String> names = namePipeline();

        FieldEvidence evidence = names.compare(names.prepare("Seán"), names.prepare("Kevin"));

        assertThat(evidence.getCategory()).isSameAs(ComparisonCategory.LOW);
        assertThat(evidence.getCategory()).isNotSameAs(ComparisonCategory.ALIAS_TRANSLATION);
    }

    @Test
    void anAliasMatchStillLosesToAConflictingDateOfBirth() {
        // The alias layer adds evidence; it does not override contradiction.
        SourcePerson source = new SourcePerson("Seán", "Ó Súilleabháin",
                LocalDate.of(1985, 6, 14), Arrays.asList("12 Main Street", "Dublin 4"));
        CandidatePerson wrongDate = new CandidatePerson("registered-9", "John", "O'Sullivan",
                LocalDate.of(1974, 2, 10), "12 Main Street, Dublin 4");

        MatchResult<CandidatePerson> result = resolverWith(realBuilder())
                .resolve(source, Collections.singletonList(wrongDate));

        assertThat(result.getDecision()).isSameAs(Decision.NO_MATCH);
    }

    // --- Determinism --------------------------------------------------------

    @Test
    void theRankingIsTheSameUnderEveryCandidateOrder() {
        SourcePerson source = sourceWithoutDateOfBirth();
        CandidatePerson first = registeredInEnglish();
        CandidatePerson second = unrelatedPerson();
        CandidatePerson third = new CandidatePerson("registered-5", "Séamus", "Ó Briain",
                null, "3 Elm Road, Cork");

        EntityResolver<SourcePerson, CandidatePerson> resolver = resolverWith(realBuilder());

        List<String> forward = rankedIds(resolver.resolve(source, Arrays.asList(first, second, third)));
        List<String> reversed = rankedIds(resolver.resolve(source, Arrays.asList(third, second, first)));
        List<String> shuffled = rankedIds(resolver.resolve(source, Arrays.asList(second, first, third)));

        assertThat(reversed).isEqualTo(forward);
        assertThat(shuffled).isEqualTo(forward);
        assertThat(forward.get(0)).isEqualTo("registered-1");
    }

    @Test
    void resolvingTwiceProducesTheSameScore() {
        SourcePerson source = sourceWithoutDateOfBirth();
        List<CandidatePerson> candidates = Collections.singletonList(registeredInEnglish());
        EntityResolver<SourcePerson, CandidatePerson> resolver = resolverWith(realBuilder());

        assertThat(resolver.resolve(source, candidates).getScore().getValue())
                .isEqualTo(resolver.resolve(source, candidates).getScore().getValue());
    }
}
