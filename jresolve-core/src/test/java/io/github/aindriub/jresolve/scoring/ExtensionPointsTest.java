package io.github.aindriub.jresolve.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.DefaultFieldEvidence;
import io.github.aindriub.jresolve.evidence.FieldEvidence;
import io.github.aindriub.jresolve.evidence.MatchEvidence;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@link FeatureExtractor} and {@link ProbabilityModel} ship without an
 * implementation, so the thing worth testing is not behaviour but
 * <em>usability</em>: that the pair can actually be implemented and composed
 * by someone outside this library.
 *
 * <p>The stand-ins below live in this test. Nothing in main sources
 * implements either interface, and that is the milestone's intent — the
 * interfaces exist so a future model can be added without disturbing the
 * types §107 names.
 */
class ExtensionPointsTest {

    /** The shape an out-of-library author would write. */
    private static final class CategoryCountingExtractor implements FeatureExtractor {
        @Override
        public Map<String, Double> extract(MatchEvidence evidence) {
            Map<String, Double> features = new LinkedHashMap<>();
            for (Map.Entry<String, FieldEvidence> entry : evidence.getFields().entrySet()) {
                boolean agreed = entry.getValue().getCategory() == ComparisonCategory.EXACT;
                features.put(entry.getKey(), agreed ? 1.0 : 0.0);
            }
            return features;
        }
    }

    /** A deliberately trivial model: the mean of its features. */
    private static final class MeanModel implements ProbabilityModel {
        @Override
        public double probabilityOfMatch(Map<String, Double> features) {
            if (features.isEmpty()) {
                return 0.0;
            }
            double total = 0.0;
            for (Double value : features.values()) {
                total += value;
            }
            return total / features.size();
        }
    }

    private static MatchEvidence evidence() {
        Map<String, FieldEvidence> fields = new LinkedHashMap<>();
        fields.put("alpha", new DefaultFieldEvidence(ComparisonCategory.EXACT, null, null));
        fields.put("bravo", new DefaultFieldEvidence(ComparisonCategory.CONFLICT, null, null));
        return new MatchEvidence(fields, true);
    }

    @Test
    void evidenceFlowsThroughFeaturesToAProbability() {
        // The whole extension path, end to end: one EXACT and one CONFLICT
        // give features 1.0 and 0.0, whose mean is 0.5. Hand-derived, not
        // read off a run.
        Map<String, Double> features = new CategoryCountingExtractor().extract(evidence());

        assertThat(new MeanModel().probabilityOfMatch(features)).isEqualTo(0.5);
    }

    @Test
    void theExtractorConsumesMatchEvidenceUnchanged() {
        // §107: a future model must be addable without changing
        // FieldDefinition, FieldPipeline or MatchEvidence. The extractor
        // takes MatchEvidence as it stands, which is what makes that true —
        // if this signature ever needs widening, §107 has been broken.
        MatchEvidence source = evidence();

        Map<String, Double> features = new CategoryCountingExtractor().extract(source);

        assertThat(features).containsOnlyKeys("alpha", "bravo");
        assertThat(source.getFields()).containsOnlyKeys("alpha", "bravo");
    }

    @Test
    void anEmptyFeatureVectorIsTheModelsProblemNotTheInterfaces() {
        assertThat(new MeanModel().probabilityOfMatch(new LinkedHashMap<String, Double>()))
                .isEqualTo(0.0);
    }

    @Test
    void neitherInterfaceIsImplementedInMainSources() {
        // Asserted as a property of the shipped jar rather than left as a
        // claim in a Javadoc: the interfaces are extension points, and D17
        // holds every model family behind v1. A main-source implementation
        // appearing here would mean that decision was reversed without the
        // decision record changing.
        assertThat(FeatureExtractor.class.isInterface()).isTrue();
        assertThat(ProbabilityModel.class.isInterface()).isTrue();
        assertThat(CategoryCountingExtractor.class.getName()).contains("ExtensionPointsTest");
        assertThat(MeanModel.class.getName()).contains("ExtensionPointsTest");
    }
}
