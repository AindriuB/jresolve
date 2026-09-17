package io.github.aindriub.jresolve.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Fixtures are neutral tokens. What a field means is a consumer's business;
 * this table only counts.
 */
class TermFrequencyTableTest {

    private static TermFrequencyTable skewedCorpus() {
        TermFrequencyTable.Builder builder = TermFrequencyTable.builder();
        // "alpha" nine times, "zulu" once: ten observations for one field.
        for (int i = 0; i < 9; i++) {
            builder.observe("code", "alpha");
        }
        builder.observe("code", "zulu");
        return builder.build();
    }

    // ------------------------------------------------------------ counting

    @Test
    void reportsTheObservedRelativeFrequency() {
        TermFrequencyTable table = skewedCorpus();

        assertThat(table.frequencyOf("code", "alpha")).isEqualTo(0.9);
        assertThat(table.frequencyOf("code", "zulu")).isEqualTo(0.1);
    }

    @Test
    void observedFrequenciesForOneFieldSumToOne() {
        // No key here falls below the floor, so nothing is raised and the
        // sum is exact. A corpus large enough to floor a key would sum
        // higher, which is the floor doing its job rather than an error.
        TermFrequencyTable table = skewedCorpus();

        assertThat(table.frequencyOf("code", "alpha") + table.frequencyOf("code", "zulu"))
                .isEqualTo(1.0);
    }

    @Test
    void fieldsAreCountedIndependently() {
        TermFrequencyTable table = TermFrequencyTable.builder()
                .observe("code", "alpha")
                .observe("tier", "alpha")
                .observe("tier", "bravo")
                .build();

        assertThat(table.frequencyOf("code", "alpha")).isEqualTo(1.0);
        assertThat(table.frequencyOf("tier", "alpha")).isEqualTo(0.5);
    }

    // --------------------------------------------------------------- floor

    @Test
    void anUnseenKeyReturnsTheFloorRatherThanZero() {
        // Zero would make u zero and log2(m/u) infinite. A corpus is a
        // sample: absence means rare, not impossible.
        TermFrequencyTable table = skewedCorpus();

        assertThat(table.frequencyOf("code", "yankee")).isEqualTo(TermFrequencyTable.DEFAULT_FLOOR);
        assertThat(table.frequencyOf("code", "yankee")).isGreaterThan(0.0);
    }

    @Test
    void anUnknownFieldReturnsTheFloorRatherThanThrowing() {
        assertThat(skewedCorpus().frequencyOf("neverCounted", "alpha"))
                .isEqualTo(TermFrequencyTable.DEFAULT_FLOOR);
    }

    @Test
    void anObservedFrequencyBelowTheFloorIsRaisedToIt() {
        // One observation in a corpus of 1000 is 0.001; a floor of 0.01 is
        // above that, so the floor wins.
        TermFrequencyTable.Builder builder = TermFrequencyTable.builder(0.01);
        for (int i = 0; i < 999; i++) {
            builder.observe("code", "alpha");
        }
        builder.observe("code", "zulu");

        assertThat(builder.build().frequencyOf("code", "zulu")).isEqualTo(0.01);
    }

    @Test
    void theFloorIsConfigurable() {
        assertThat(TermFrequencyTable.builder(0.25).build().frequencyOf("code", "alpha"))
                .isEqualTo(0.25);
    }

    @Test
    void rejectsAFloorOutsideTheOpenUnitInterval() {
        assertThatThrownBy(() -> TermFrequencyTable.builder(0.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TermFrequencyTable.builder(-0.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TermFrequencyTable.builder(1.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aFloorOfOneIsPermitted() {
        assertThat(TermFrequencyTable.builder(1.0).build().getFloor()).isEqualTo(1.0);
    }

    // ------------------------------------------------------- what it is for

    @Test
    void aCommonValueIsMateriallyMoreFrequentThanARareOne() {
        // The property the whole feature exists for. A u derived from these
        // differs by an order of magnitude, so the weights do too — and that
        // is the difference between agreement on a common value counting for
        // as much as agreement on a rare one, and not.
        TermFrequencyTable table = skewedCorpus();

        assertThat(table.frequencyOf("code", "alpha"))
                .isGreaterThan(table.frequencyOf("code", "zulu") * 5);
    }

    // ------------------------------------------------------------ rejection

    @Test
    void rejectsNullOnLookup() {
        TermFrequencyTable table = skewedCorpus();

        assertThatThrownBy(() -> table.frequencyOf(null, "alpha"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.frequencyOf("code", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullWhileCounting() {
        assertThatThrownBy(() -> TermFrequencyTable.builder().observe(null, "alpha"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TermFrequencyTable.builder().observe("code", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theRejectionMessageNamesTheConstraintAndNotTheKey() {
        // A key is a prepared field value, so it must not reach a message.
        assertThatThrownBy(() -> TermFrequencyTable.builder().observe(null, "supersecretvalue"))
                .hasMessageNotContaining("supersecretvalue");
    }

    // ----------------------------------------------------------- immutable

    @Test
    void buildingTwiceDoesNotShareMutableState() {
        TermFrequencyTable.Builder builder = TermFrequencyTable.builder().observe("code", "alpha");
        TermFrequencyTable first = builder.build();

        builder.observe("code", "bravo");

        // first saw one observation; adding a second must not reach it.
        assertThat(first.frequencyOf("code", "alpha")).isEqualTo(1.0);
        assertThat(first.frequencyOf("code", "bravo")).isEqualTo(TermFrequencyTable.DEFAULT_FLOOR);
    }

    // --------------------------------------------------------------- scale

    @Test
    void lookupDoesNotDegradeWithTheNumberOfKeys() {
        // Hash lookups, so a thousand distinct keys resolve the same as two.
        // This asserts the structure holds at that size, not a timing.
        TermFrequencyTable.Builder builder = TermFrequencyTable.builder();
        for (int i = 0; i < 1000; i++) {
            builder.observe("code", "key" + i);
        }
        TermFrequencyTable table = builder.build();

        assertThat(table.frequencyOf("code", "key500")).isEqualTo(0.001);
        assertThat(table.frequencyOf("code", "absent")).isEqualTo(TermFrequencyTable.DEFAULT_FLOOR);
    }

    // ------------------------------------------------------------- coverage

    @Test
    void reportsWhetherItCoversAField() {
        TermFrequencyTable table = skewedCorpus();

        assertThat(table.covers("code")).isTrue();
        assertThat(table.covers("neverCounted")).isFalse();
    }

    @Test
    void anEmptyTableCoversNothing() {
        assertThat(TermFrequencyTable.builder().build().covers("code")).isFalse();
    }

    @Test
    void coversRejectsNull() {
        assertThatThrownBy(() -> skewedCorpus().covers(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anUncoveredFieldIsIndistinguishableFromARareKeyByFrequencyAlone() {
        // Why covers() has to exist. Both answers are the floor, but they
        // mean different things: one is "this value is rare in a corpus I
        // have", the other "I have no corpus here at all". A caller that
        // cannot tell them apart reads the second as maximal rarity and
        // produces the largest possible weight for a field it knows nothing
        // about.
        TermFrequencyTable table = skewedCorpus();

        assertThat(table.frequencyOf("code", "neverSeen"))
                .isEqualTo(table.frequencyOf("neverCounted", "alpha"));
        assertThat(table.covers("code")).isNotEqualTo(table.covers("neverCounted"));
    }
}
