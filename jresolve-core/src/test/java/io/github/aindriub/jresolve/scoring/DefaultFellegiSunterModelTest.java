package io.github.aindriub.jresolve.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Fixtures are neutral tokens throughout.
 */
class DefaultFellegiSunterModelTest {

    private static DefaultFellegiSunterModel.Builder configured() {
        return DefaultFellegiSunterModel.builder()
                .probabilities("code", ComparisonCategory.EXACT, 0.9, 0.01)
                .probabilities("code", ComparisonCategory.CONFLICT, 0.1, 0.99);
    }

    /** "alpha" nine times, "zulu" once. */
    private static TermFrequencyTable skewedCorpus() {
        TermFrequencyTable.Builder builder = TermFrequencyTable.builder();
        for (int i = 0; i < 9; i++) {
            builder.observe("code", "alpha");
        }
        builder.observe("code", "zulu");
        return builder.build();
    }

    // ------------------------------------------------------- configuration

    @Test
    void reportsTheConfiguredProbabilities() {
        DefaultFellegiSunterModel model = configured().build();

        assertThat(model.mProbability("code", ComparisonCategory.EXACT)).isEqualTo(0.9);
        assertThat(model.uProbability("code", ComparisonCategory.EXACT, null)).isEqualTo(0.01);
    }

    @Test
    void rejectsProbabilitiesOutsideTheHalfOpenUnitInterval() {
        // Zero is rejected because log2(m/u) is undefined or infinite there.
        for (double bad : new double[] {0.0, -0.1, 1.1}) {
            assertThatThrownBy(() -> DefaultFellegiSunterModel.builder()
                    .probabilities("code", ComparisonCategory.EXACT, bad, 0.5))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> DefaultFellegiSunterModel.builder()
                    .probabilities("code", ComparisonCategory.EXACT, 0.5, bad))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void oneIsAValidProbability() {
        assertThat(DefaultFellegiSunterModel.builder()
                .probabilities("code", ComparisonCategory.EXACT, 1.0, 1.0)
                .build()
                .mProbability("code", ComparisonCategory.EXACT))
                .isEqualTo(1.0);
    }

    @Test
    void rejectsNullFieldOrCategory() {
        assertThatThrownBy(() -> DefaultFellegiSunterModel.builder()
                .probabilities(null, ComparisonCategory.EXACT, 0.9, 0.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DefaultFellegiSunterModel.builder()
                .probabilities("code", null, 0.9, 0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------- the frequency adjustment

    @Test
    void agreementOnACommonValueYieldsAHigherUThanOnARareOne() {
        // The entry's whole point. u is P(agreement | non-match), so a value
        // nine records in ten share is a poor discriminator and a value one
        // record has is a good one.
        DefaultFellegiSunterModel model = configured().frequencies(skewedCorpus()).build();

        double common = model.uProbability("code", ComparisonCategory.EXACT, "alpha");
        double rare = model.uProbability("code", ComparisonCategory.EXACT, "zulu");

        assertThat(common).isEqualTo(0.9);
        assertThat(rare).isEqualTo(0.1);
        assertThat(common).isGreaterThan(rare);
    }

    @Test
    void aHigherUMeansALowerWeight() {
        // Stated in the terms the scorer will use it in, so the consequence
        // is visible here rather than only two tasks later.
        DefaultFellegiSunterModel model = configured().frequencies(skewedCorpus()).build();
        double m = model.mProbability("code", ComparisonCategory.EXACT);

        double commonWeight =
                log2(m / model.uProbability("code", ComparisonCategory.EXACT, "alpha"));
        double rareWeight =
                log2(m / model.uProbability("code", ComparisonCategory.EXACT, "zulu"));

        assertThat(rareWeight).isGreaterThan(commonWeight);
    }

    private static double log2(double value) {
        return Math.log(value) / Math.log(2.0);
    }

    @Test
    void aNullFrequencyKeyFallsBackToTheFlatConfiguredU() {
        // A comparison that did not agree has no value to be common or rare.
        DefaultFellegiSunterModel model = configured().frequencies(skewedCorpus()).build();

        assertThat(model.uProbability("code", ComparisonCategory.CONFLICT, null)).isEqualTo(0.99);
    }

    @Test
    void aModelWithNoFrequencyTableIgnoresTheKey() {
        DefaultFellegiSunterModel model = configured().build();

        assertThat(model.uProbability("code", ComparisonCategory.EXACT, "alpha")).isEqualTo(0.01);
        assertThat(model.uProbability("code", ComparisonCategory.EXACT, null)).isEqualTo(0.01);
    }

    @Test
    void anAdjustedUStaysWithinTheOpenUnitInterval() {
        // Task 20's acceptance asked for a clamp here. There is none, and
        // adding one would guard nothing: TermFrequencyTable already returns
        // a value within [floor, 1] with floor > 0, so the range holds by
        // construction rather than by a defensive check. That property is
        // what this test pins — including the unseen key, which is where a
        // zero would otherwise arrive and make the weight infinite.
        DefaultFellegiSunterModel model = configured().frequencies(skewedCorpus()).build();

        for (String key : new String[] {"alpha", "zulu", "neverSeen"}) {
            double u = model.uProbability("code", ComparisonCategory.EXACT, key);
            assertThat(u).isGreaterThan(0.0).isLessThanOrEqualTo(1.0);
        }
    }

    // ---------------------------------------------------------- prior odds

    @Test
    void aModelWithoutPriorOddsSaysSoRatherThanDefaulting() {
        // "Nobody said" must be distinguishable from "the odds are even",
        // or a scorer would publish a posterior nobody stood behind.
        DefaultFellegiSunterModel model = configured().build();

        assertThat(model.hasPriorOdds()).isFalse();
        assertThatThrownBy(model::priorOdds).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aModelWithPriorOddsReportsThem() {
        DefaultFellegiSunterModel model = configured().priorOdds(0.001).build();

        assertThat(model.hasPriorOdds()).isTrue();
        assertThat(model.priorOdds()).isEqualTo(0.001);
    }

    @Test
    void evenOddsAreDistinguishableFromUnset() {
        DefaultFellegiSunterModel even = configured().priorOdds(1.0).build();

        assertThat(even.hasPriorOdds()).isTrue();
        assertThat(even.priorOdds()).isEqualTo(1.0);
        assertThat(configured().build().hasPriorOdds()).isFalse();
    }

    @Test
    void rejectsPriorOddsThatAreNotFiniteAndPositive() {
        for (double bad : new double[] {0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThatThrownBy(() -> DefaultFellegiSunterModel.builder().priorOdds(bad))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ------------------------------------------------------- missingness

    @Test
    void anUnconfiguredCategoryFailsLoudlyRatherThanScoringZero() {
        // §45: a missing value is not automatically a non-match. Nor is an
        // unconfigured one silently neutral — that would be the same guess
        // wearing a different hat.
        DefaultFellegiSunterModel model = configured().build();

        assertThatThrownBy(() -> model.mProbability("code", ComparisonCategory.MISSING_ONE))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> model.uProbability("code", ComparisonCategory.MISSING_BOTH, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void missingnessCanBeConfiguredWithItsOwnProbabilities() {
        DefaultFellegiSunterModel model = configured()
                .probabilities("code", ComparisonCategory.MISSING_ONE, 0.2, 0.3)
                .build();

        assertThat(model.mProbability("code", ComparisonCategory.MISSING_ONE)).isEqualTo(0.2);
        assertThat(model.isIgnored("code", ComparisonCategory.MISSING_ONE)).isFalse();
    }

    @Test
    void missingnessCanBeDeclaredToCarryNoEvidence() {
        DefaultFellegiSunterModel model = configured()
                .ignore("code", ComparisonCategory.MISSING_BOTH)
                .build();

        assertThat(model.isIgnored("code", ComparisonCategory.MISSING_BOTH)).isTrue();
    }

    @Test
    void ignoringAndConfiguringAreMutuallyExclusive() {
        DefaultFellegiSunterModel ignoredLast = configured()
                .probabilities("code", ComparisonCategory.MISSING_ONE, 0.2, 0.3)
                .ignore("code", ComparisonCategory.MISSING_ONE)
                .build();
        DefaultFellegiSunterModel configuredLast = configured()
                .ignore("code", ComparisonCategory.MISSING_ONE)
                .probabilities("code", ComparisonCategory.MISSING_ONE, 0.2, 0.3)
                .build();

        assertThat(ignoredLast.isIgnored("code", ComparisonCategory.MISSING_ONE)).isTrue();
        assertThat(configuredLast.isIgnored("code", ComparisonCategory.MISSING_ONE)).isFalse();
        assertThat(configuredLast.mProbability("code", ComparisonCategory.MISSING_ONE))
                .isEqualTo(0.2);
    }

    @Test
    void anIgnoredPairStillRefusesToSupplyProbabilities() {
        // Ignored means "do not weigh this", not "weigh it as nothing".
        DefaultFellegiSunterModel model = configured()
                .ignore("code", ComparisonCategory.MISSING_BOTH)
                .build();

        assertThatThrownBy(() -> model.mProbability("code", ComparisonCategory.MISSING_BOTH))
                .isInstanceOf(IllegalStateException.class);
    }

    // --------------------------------------------------------- composites

    @Test
    void aDeclaredCompositeIsReported() {
        DefaultFellegiSunterModel model = configured()
                .probabilities("tier", ComparisonCategory.EXACT, 0.8, 0.2)
                .composite(Arrays.asList("code", "tier"))
                .build();

        assertThat(model.compositeGroups()).hasSize(1);
        assertThat(model.compositeGroups().iterator().next()).containsExactlyInAnyOrder("code", "tier");
    }

    @Test
    void aModelWithNoCompositesReportsAnEmptyCollection() {
        assertThat(configured().build().compositeGroups()).isEmpty();
    }

    @Test
    void aCompositeNamingAnUnconfiguredFieldFailsAtBuild() {
        // A composite over a field nothing knows about could never
        // contribute; silently doing nothing is worse than failing here.
        assertThatThrownBy(() -> configured()
                .composite(Arrays.asList("code", "neverConfigured"))
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aCompositeNeedsAtLeastTwoDistinctFields() {
        assertThatThrownBy(() -> configured().composite(Collections.singletonList("code")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> configured().composite(Arrays.asList("code", "code")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aCompositeGroupIsUnmodifiable() {
        DefaultFellegiSunterModel model = configured()
                .probabilities("tier", ComparisonCategory.EXACT, 0.8, 0.2)
                .composite(Arrays.asList("code", "tier"))
                .build();

        assertThatThrownBy(() -> model.compositeGroups().iterator().next().add("extra"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ---------------------------------------------------------- immutable

    @Test
    void buildingTwiceDoesNotShareMutableState() {
        DefaultFellegiSunterModel.Builder builder = configured();
        DefaultFellegiSunterModel first = builder.build();

        builder.probabilities("tier", ComparisonCategory.EXACT, 0.8, 0.2);

        assertThatThrownBy(() -> first.mProbability("tier", ComparisonCategory.EXACT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void anUncoveredFieldFallsBackToTheFlatConfiguredU() {
        // The defect task 24 caught end to end. The corpus covers "code" and
        // not "tier", and the table answers its floor for an uncovered field
        // — so adjusting on it would read 1e-6 as a frequency and give "tier"
        // roughly 19 bits instead of 1. A partial corpus must not inflate
        // every field it happens to miss.
        DefaultFellegiSunterModel model = configured()
                .probabilities("tier", ComparisonCategory.EXACT, 0.5, 0.25)
                .frequencies(skewedCorpus())
                .build();

        assertThat(model.uProbability("tier", ComparisonCategory.EXACT, "gold")).isEqualTo(0.25);
    }

    @Test
    void aCoveredFieldIsStillAdjusted() {
        // The control: the fallback must not disable the adjustment where a
        // corpus genuinely exists.
        DefaultFellegiSunterModel model = configured()
                .probabilities("tier", ComparisonCategory.EXACT, 0.5, 0.25)
                .frequencies(skewedCorpus())
                .build();

        assertThat(model.uProbability("code", ComparisonCategory.EXACT, "alpha")).isEqualTo(0.9);
    }

    // ------------------------------------------------------ composite rules

    @Test
    void aGroupDeclaredWithoutARuleCombinesBySmallest() {
        DefaultFellegiSunterModel model = DefaultFellegiSunterModel.builder()
                .probabilities("code", ComparisonCategory.EXACT, 0.8, 0.1)
                .probabilities("tier", ComparisonCategory.EXACT, 0.5, 0.25)
                .composite(Arrays.asList("code", "tier"))
                .build();

        assertThat(model.compositeRuleFor(model.compositeGroups().iterator().next()))
                .isSameAs(CompositeRule.SMALLEST);
    }

    @Test
    void eachGroupKeepsTheRuleItWasDeclaredWith() {
        DefaultFellegiSunterModel model = DefaultFellegiSunterModel.builder()
                .probabilities("code", ComparisonCategory.EXACT, 0.8, 0.1)
                .probabilities("tier", ComparisonCategory.EXACT, 0.5, 0.25)
                .probabilities("alpha", ComparisonCategory.EXACT, 0.8, 0.2)
                .probabilities("bravo", ComparisonCategory.EXACT, 0.8, 0.05)
                .composite(Arrays.asList("code", "tier"), CompositeRule.STRONGEST)
                .composite(Arrays.asList("alpha", "bravo"), CompositeRule.AVERAGE)
                .build();

        assertThat(model.compositeRuleFor(new LinkedHashSet<>(Arrays.asList("code", "tier"))))
                .isSameAs(CompositeRule.STRONGEST);
        assertThat(model.compositeRuleFor(new LinkedHashSet<>(Arrays.asList("alpha", "bravo"))))
                .isSameAs(CompositeRule.AVERAGE);
    }

    @Test
    void anUndeclaredGroupReportsTheConservativeDefault() {
        DefaultFellegiSunterModel model = DefaultFellegiSunterModel.builder()
                .probabilities("code", ComparisonCategory.EXACT, 0.8, 0.1)
                .build();

        assertThat(model.compositeRuleFor(new LinkedHashSet<>(Arrays.asList("code", "tier"))))
                .isSameAs(CompositeRule.SMALLEST);
    }

    @Test
    void rejectsANullCompositeRule() {
        assertThatThrownBy(() -> DefaultFellegiSunterModel.builder()
                .probabilities("code", ComparisonCategory.EXACT, 0.8, 0.1)
                .probabilities("tier", ComparisonCategory.EXACT, 0.5, 0.25)
                .composite(Arrays.asList("code", "tier"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rule");
    }

    @Test
    void theRuleRejectionNamesTheConstraintAndNotTheFields() {
        // D10: a field value must not reach a message. The field names here
        // are the consumer's own, so the message names neither.
        assertThatThrownBy(() -> DefaultFellegiSunterModel.builder()
                .probabilities("supersecretfield", ComparisonCategory.EXACT, 0.8, 0.1)
                .probabilities("tier", ComparisonCategory.EXACT, 0.5, 0.25)
                .composite(Arrays.asList("supersecretfield", "tier"), null))
                .hasMessageNotContaining("supersecretfield");
    }

    @Test
    void anImplementationThatDoesNotOverrideTheRuleDeclaresSmallest() {
        // The default method is what keeps this interface change additive: a
        // model written before the rule existed still compiles and still
        // behaves as it did.
        FellegiSunterModel bare = new FellegiSunterModel() {
            @Override
            public double mProbability(String field, ComparisonCategory category) {
                return 0.8;
            }

            @Override
            public double uProbability(String field, ComparisonCategory category, String key) {
                return 0.1;
            }

            @Override
            public boolean isIgnored(String field, ComparisonCategory category) {
                return false;
            }

            @Override
            public boolean hasPriorOdds() {
                return false;
            }

            @Override
            public double priorOdds() {
                throw new IllegalStateException("no prior odds configured");
            }

            @Override
            public Collection<Set<String>> compositeGroups() {
                return Collections.<Set<String>>singletonList(
                        new LinkedHashSet<>(Arrays.asList("code", "tier")));
            }
        };

        assertThat(bare.compositeRuleFor(new LinkedHashSet<>(Arrays.asList("code", "tier"))))
                .isSameAs(CompositeRule.SMALLEST);
    }
}
