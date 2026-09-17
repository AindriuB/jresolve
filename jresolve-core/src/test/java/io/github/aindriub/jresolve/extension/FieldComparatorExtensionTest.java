package io.github.aindriub.jresolve.extension;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.DefaultFieldEvidence;
import io.github.aindriub.jresolve.evidence.FieldEvidence;
import io.github.aindriub.jresolve.field.AbstractNullSafeFieldComparator;
import io.github.aindriub.jresolve.field.SimilarityBands;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The field layer's extension points, exercised from outside
 * {@code io.github.aindriub.jresolve.field}.
 *
 * <p>This package exists for one reason: every other test of these types
 * lives inside {@code field/}, where package-private access makes them look
 * usable whether or not they actually are. A comparator shipped in another
 * module — the profiles module, or a consumer's own code — sees only what
 * this package sees. If these types are ever narrowed again, this test stops
 * compiling, which is the point.
 */
class FieldComparatorExtensionTest {

    /**
     * A comparator of the shape an out-of-module author would write: it
     * inherits the null rule and implements only the non-null case.
     */
    private static final class RecordingComparator extends AbstractNullSafeFieldComparator<String> {

        private final List<String> nonNullCalls = new ArrayList<>();

        @Override
        protected FieldEvidence compareNonNull(String left, String right) {
            nonNullCalls.add(left + "|" + right);
            return new DefaultFieldEvidence(ComparisonCategory.EXACT, null, null);
        }
    }

    @Test
    void aComparatorOutsideTheFieldPackageCanInheritTheNullRule() {
        RecordingComparator comparator = new RecordingComparator();

        assertThat(comparator.compare(null, null).getCategory())
                .isSameAs(ComparisonCategory.MISSING_BOTH);
    }

    @Test
    void leftNullYieldsMissingOne() {
        RecordingComparator comparator = new RecordingComparator();

        assertThat(comparator.compare(null, "alpha").getCategory())
                .isSameAs(ComparisonCategory.MISSING_ONE);
    }

    @Test
    void rightNullYieldsMissingOne() {
        RecordingComparator comparator = new RecordingComparator();

        assertThat(comparator.compare("alpha", null).getCategory())
                .isSameAs(ComparisonCategory.MISSING_ONE);
    }

    @Test
    void bothPresentReachesTheSubclass() {
        RecordingComparator comparator = new RecordingComparator();

        assertThat(comparator.compare("alpha", "beta").getCategory())
                .isSameAs(ComparisonCategory.EXACT);
    }

    @Test
    void noNullCombinationReachesTheSubclass() {
        RecordingComparator comparator = new RecordingComparator();

        comparator.compare(null, null);
        comparator.compare(null, "alpha");
        comparator.compare("alpha", null);

        // The inherited rule is not merely producing the right categories, it
        // is absorbing the call entirely — which is what lets a subclass
        // dereference both arguments without checking.
        assertThat(comparator.nonNullCalls).isEmpty();
    }

    @Test
    void theSubclassReceivesBothArgumentsInOrder() {
        RecordingComparator comparator = new RecordingComparator();

        comparator.compare("alpha", "beta");

        assertThat(comparator.nonNullCalls).containsExactly("alpha|beta");
    }

    @Test
    void bandingIsReusableOutsideTheFieldPackage() {
        SimilarityBands bands = new SimilarityBands();

        assertThat(bands.categoryFor(0.99)).isSameAs(ComparisonCategory.VERY_HIGH);
        assertThat(bands.categoryFor(0.90)).isSameAs(ComparisonCategory.HIGH);
        assertThat(bands.categoryFor(0.75)).isSameAs(ComparisonCategory.MEDIUM);
        assertThat(bands.categoryFor(0.10)).isSameAs(ComparisonCategory.LOW);
    }

    @Test
    void bandLowerBoundsAreInclusiveWhenAppliedFromOutside() {
        SimilarityBands bands = new SimilarityBands();

        // The reimplementation risk this visibility exists to prevent is an
        // exclusive bound, which differs from the shipped banding only at
        // exactly these three values.
        assertThat(bands.categoryFor(0.95)).isSameAs(ComparisonCategory.VERY_HIGH);
        assertThat(bands.categoryFor(0.85)).isSameAs(ComparisonCategory.HIGH);
        assertThat(bands.categoryFor(0.70)).isSameAs(ComparisonCategory.MEDIUM);
    }

    @Test
    void configuredBandsAreAppliedRatherThanTheDefaults() {
        SimilarityBands bands = new SimilarityBands(0.8, 0.6, 0.4, 0.0);

        // 0.75 is MEDIUM under the defaults and HIGH here. An out-of-package
        // comparator that read the getters but hard-coded the comparison
        // would get this wrong.
        assertThat(bands.categoryFor(0.75)).isSameAs(ComparisonCategory.HIGH);
    }
}
