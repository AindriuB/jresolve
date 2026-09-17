package io.github.aindriub.jresolve.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.DefaultFieldEvidence;
import io.github.aindriub.jresolve.evidence.FieldEvidence;
import io.github.aindriub.jresolve.evidence.MatchEvidence;
import io.github.aindriub.jresolve.result.FieldContribution;
import io.github.aindriub.jresolve.result.ScoreScale;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Every expected weight below is hand-derived from the configured {@code m}
 * and {@code u}, never read off a run. The probabilities are chosen so the
 * arithmetic is exact and checkable by eye:
 *
 * <pre>
 *   code EXACT: m = 0.8, u = 0.1  ->  m/u = 8    ->  log2 = +3.0
 *   tier EXACT: m = 0.5, u = 0.25 ->  m/u = 2    ->  log2 = +1.0
 *   code CONFLICT: m = 0.1, u = 0.8 -> m/u = 0.125 -> log2 = -3.0
 * </pre>
 *
 * Fixtures are neutral tokens.
 */
class FellegiSunterScorerTest {

    private static DefaultFellegiSunterModel.Builder model() {
        return DefaultFellegiSunterModel.builder()
                .probabilities("code", ComparisonCategory.EXACT, 0.8, 0.1)
                .probabilities("code", ComparisonCategory.CONFLICT, 0.1, 0.8)
                .probabilities("tier", ComparisonCategory.EXACT, 0.5, 0.25);
    }

    private static MatchEvidence evidence(boolean complete, String... fieldsAndCategories) {
        Map<String, FieldEvidence> fields = new LinkedHashMap<>();
        for (int i = 0; i < fieldsAndCategories.length; i += 2) {
            fields.put(fieldsAndCategories[i], new DefaultFieldEvidence(
                    ComparisonCategory.of(fieldsAndCategories[i + 1]), null, null));
        }
        return new MatchEvidence(fields, complete);
    }

    private static MatchEvidence bothExact() {
        return evidence(true, "code", "EXACT", "tier", "EXACT");
    }

    private static double contributionFor(ScoringResult result, String field) {
        for (FieldContribution contribution : result.getContributions()) {
            if (field.equals(contribution.getField())) {
                return contribution.getContribution();
            }
        }
        throw new AssertionError("no contribution for field " + field);
    }

    // ----------------------------------------------------------- the weight

    @Test
    void sumsLog2OfMOverUAcrossFields() {
        // +3.0 and +1.0, hand-derived above.
        ScoringResult result = new FellegiSunterScorer(model().build()).score(bothExact());

        assertThat(result.isScorable()).isTrue();
        assertThat(result.getScore().getValue()).isEqualTo(4.0, within(1e-9));
    }

    @Test
    void disagreementCarriesANegativeWeight() {
        // code CONFLICT is log2(0.1 / 0.8) = log2(0.125) = -3.0.
        ScoringResult result = new FellegiSunterScorer(model().build())
                .score(evidence(true, "code", "CONFLICT"));

        assertThat(result.getScore().getValue()).isEqualTo(-3.0, within(1e-9));
    }

    @Test
    void eachFieldContributesItsOwnWeight() {
        ScoringResult result = new FellegiSunterScorer(model().build()).score(bothExact());

        assertThat(contributionFor(result, "code")).isEqualTo(3.0, within(1e-9));
        assertThat(contributionFor(result, "tier")).isEqualTo(1.0, within(1e-9));
    }

    @Test
    void theScoreCarriesItsScaleAndAlgorithm() {
        ScoringResult result = new FellegiSunterScorer(model().build()).score(bothExact());

        assertThat(result.getScore().getScale()).isSameAs(ScoreScale.LOG2_LIKELIHOOD_RATIO);
        assertThat(result.getScore().getAlgorithm()).isEqualTo(FellegiSunterScorer.ALGORITHM);
        assertThat(new FellegiSunterScorer(model().build()).scale())
                .isSameAs(ScoreScale.LOG2_LIKELIHOOD_RATIO);
    }

    // ------------------------------------------------- the frequency effect

    private static TermFrequencyTable skewedCorpus() {
        TermFrequencyTable.Builder builder = TermFrequencyTable.builder();
        for (int i = 0; i < 9; i++) {
            builder.observe("code", "alpha");
        }
        builder.observe("code", "zulu");
        return builder.build();
    }

    private static MatchEvidence agreedOn(String key) {
        Map<String, FieldEvidence> fields = new LinkedHashMap<>();
        fields.put("code", new DefaultFieldEvidence(ComparisonCategory.EXACT, null, key));
        return new MatchEvidence(fields, true);
    }

    @Test
    void agreementOnARareValueOutweighsAgreementOnACommonOne() {
        // Through the scorer, not only the model. With the corpus, u is the
        // observed frequency: 0.9 for the common value and 0.1 for the rare.
        //   rare:   log2(0.8 / 0.1) = log2(8)      = +3.0
        //   common: log2(0.8 / 0.9) = log2(0.888…) ≈ -0.1699, negative
        //           because m < u — agreement on a value nine records in ten
        //           share is evidence *against* a match, not for one.
        FellegiSunterScorer scorer = new FellegiSunterScorer(
                model().frequencies(skewedCorpus()).build());

        double rare = scorer.score(agreedOn("zulu")).getScore().getValue();
        double common = scorer.score(agreedOn("alpha")).getScore().getValue();

        assertThat(rare).isEqualTo(3.0, within(1e-9));
        assertThat(common).isLessThan(0.0);
        assertThat(rare).isGreaterThan(common);
    }

    @Test
    void withoutACorpusBothValuesScoreTheSame() {
        // The control. Without it the assertion above cannot distinguish
        // "the frequency adjustment works" from "these two keys differ for
        // some other reason".
        FellegiSunterScorer scorer = new FellegiSunterScorer(model().build());

        assertThat(scorer.score(agreedOn("zulu")).getScore().getValue())
                .isEqualTo(scorer.score(agreedOn("alpha")).getScore().getValue());
    }

    // ------------------------------------------------------- the probability

    @Test
    void withoutPriorOddsNoProbabilityIsExposed() {
        ScoringResult result = new FellegiSunterScorer(model().build()).score(bothExact());

        assertThat(result.getScore().getProbability()).isNull();
        assertThat(result.getScore().getValue()).isEqualTo(4.0, within(1e-9));
    }

    @Test
    void withPriorOddsTheposteriorIsExposed() {
        // posterior odds = 0.25 × 2^4 = 4; probability = 4 / (1 + 4) = 0.8.
        ScoringResult result = new FellegiSunterScorer(model().priorOdds(0.25).build())
                .score(bothExact());

        assertThat(result.getScore().getProbability()).isEqualTo(0.8, within(1e-9));
    }

    @Test
    void theWeightIsUnchangedByThePresenceOfAPrior() {
        // A prior turns a likelihood ratio into a posterior; it does not
        // alter the ratio.
        double without = new FellegiSunterScorer(model().build())
                .score(bothExact()).getScore().getValue();
        double with = new FellegiSunterScorer(model().priorOdds(0.25).build())
                .score(bothExact()).getScore().getValue();

        assertThat(with).isEqualTo(without);
    }

    // ------------------------------------------------- incomplete evidence

    @Test
    void incompleteEvidenceIsUnscorableRatherThanScoredPartially() {
        ScoringResult result = new FellegiSunterScorer(model().build())
                .score(evidence(false, "code", "EXACT"));

        assertThat(result.isScorable()).isFalse();
        assertThat(result.getUnscorableTemplateKey()).contains("incomplete");
    }

    @Test
    void incompleteEvidenceIsNotTreatedAsBothSidesMissing() {
        // The quiet fabrication this refuses: scoring the fields that happen
        // to be present, or filling the absent ones in as MISSING_BOTH, both
        // produce a number that looks like a full likelihood ratio and is
        // not one.
        ScoringResult result = new FellegiSunterScorer(model().build())
                .score(evidence(false, "code", "EXACT", "tier", "EXACT"));

        assertThat(result.isScorable()).isFalse();
        // ScoringResult refuses to hand back a score at all rather than
        // returning null — so there is no number here for a caller to use by
        // accident, which is a stronger guarantee than the one this test
        // originally asserted.
        assertThatThrownBy(result::getScore).isInstanceOf(IllegalStateException.class);
    }

    // -------------------------------------------------------- missingness

    @Test
    void anIgnoredCategoryIsSkippedRatherThanWeighed() {
        FellegiSunterScorer scorer = new FellegiSunterScorer(
                model().ignore("code", ComparisonCategory.MISSING_ONE).build());

        ScoringResult result = scorer.score(
                evidence(true, "code", "MISSING_ONE", "tier", "EXACT"));

        assertThat(result.getScore().getValue()).isEqualTo(1.0, within(1e-9));
        assertThat(contributionFor(result, "code")).isEqualTo(0.0);
    }

    @Test
    void anIgnoredFieldStillAppearsInTheExplanation() {
        // Considered and deliberately skipped is a different statement from
        // never looked at, and an explanation should be able to say which.
        FellegiSunterScorer scorer = new FellegiSunterScorer(
                model().ignore("code", ComparisonCategory.MISSING_ONE).build());

        ScoringResult result = scorer.score(evidence(true, "code", "MISSING_ONE"));

        assertThat(result.getContributions()).hasSize(1);
        assertThat(result.getContributions().get(0).getTemplateKey()).contains("ignored");
    }

    @Test
    void anUnconfiguredCategorySurfacesTheModelsErrorRatherThanScoringZero() {
        FellegiSunterScorer scorer = new FellegiSunterScorer(model().build());

        assertThatThrownBy(() -> scorer.score(evidence(true, "code", "MISSING_BOTH")))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---------------------------------------------------------- composites

    @Test
    void aCompositeGroupContributesOnceRatherThanOncePerMember() {
        // Without the declaration: 3.0 + 1.0 = 4.0 — and that 4.0 is the
        // double-counting one if the two fields co-vary, because the shared
        // signal is added twice. With it, the group contributes its smallest
        // member's weight alone: 1.0.
        double independent = new FellegiSunterScorer(model().build())
                .score(bothExact()).getScore().getValue();
        double composite = new FellegiSunterScorer(
                model().composite(Arrays.asList("code", "tier")).build())
                .score(bothExact()).getScore().getValue();

        assertThat(independent).isEqualTo(4.0, within(1e-9));
        assertThat(composite).isEqualTo(1.0, within(1e-9));
        assertThat(composite).isLessThan(independent);
    }

    @Test
    void aSuppressedCompositeMemberIsRecordedInTheExplanation() {
        ScoringResult result = new FellegiSunterScorer(
                model().composite(Arrays.asList("code", "tier")).build()).score(bothExact());

        assertThat(contributionFor(result, "code")).isEqualTo(0.0);
        assertThat(contributionFor(result, "tier")).isEqualTo(1.0, within(1e-9));
        for (FieldContribution contribution : result.getContributions()) {
            if ("code".equals(contribution.getField())) {
                assertThat(contribution.getTemplateKey()).contains("compositeSuppressed");
            }
        }
    }

    @Test
    void aCompositeWithOnlyOneMemberPresentContributesThroughThatMember() {
        // A composite is a statement about correlation, not a requirement
        // that every member be compared.
        ScoringResult result = new FellegiSunterScorer(
                model().composite(Arrays.asList("code", "tier")).build())
                .score(evidence(true, "code", "EXACT"));

        assertThat(result.getScore().getValue()).isEqualTo(3.0, within(1e-9));
    }

    // ------------------------------------------------------- housekeeping

    @Test
    void isDeterministic() {
        FellegiSunterScorer scorer = new FellegiSunterScorer(model().build());

        assertThat(scorer.score(bothExact()).getScore().getValue())
                .isEqualTo(scorer.score(bothExact()).getScore().getValue());
    }

    @Test
    void rejectsANullModel() {
        assertThatThrownBy(() -> new FellegiSunterScorer(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullEvidence() {
        assertThatThrownBy(() -> new FellegiSunterScorer(model().build()).score(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void evidenceWithNoFieldsScoresZeroRatherThanFailing() {
        // A likelihood ratio over no evidence is 1, whose log is 0 — the
        // honest answer, and distinct from an unscorable one.
        ScoringResult result = new FellegiSunterScorer(model().build())
                .score(new MatchEvidence(new LinkedHashMap<String, FieldEvidence>(), true));

        assertThat(result.isScorable()).isTrue();
        assertThat(result.getScore().getValue()).isEqualTo(0.0);
    }
}
