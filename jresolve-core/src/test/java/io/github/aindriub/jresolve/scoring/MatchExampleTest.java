package io.github.aindriub.jresolve.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Feature names here are neutral tokens. What a feature measures is a
 * consumer's business; these types only hold it.
 */
class MatchExampleTest {

    private static Map<String, Double> features() {
        Map<String, Double> features = new LinkedHashMap<>();
        features.put("alpha", 0.9);
        features.put("bravo", 0.1);
        return features;
    }

    // ------------------------------------------------------------- holding

    @Test
    void theLabelledFormCarriesFeaturesAndItsLabel() {
        LabelledMatchExample example = new LabelledMatchExample(features(), true);

        assertThat(example.getFeatures()).containsEntry("alpha", 0.9).containsEntry("bravo", 0.1);
        assertThat(example.isMatch()).isTrue();
    }

    @Test
    void theUnlabelledFormCarriesFeaturesAlone() {
        UnlabelledMatchExample example = new UnlabelledMatchExample(features());

        assertThat(example.getFeatures()).containsEntry("alpha", 0.9);
    }

    @Test
    void bothLabelsAreRepresentable() {
        assertThat(new LabelledMatchExample(features(), false).isMatch()).isFalse();
        assertThat(new LabelledMatchExample(features(), true).isMatch()).isTrue();
    }

    // ----------------------------------------------------------- immutable

    @Test
    void mutatingTheSourceMapAfterwardsDoesNotReachTheExample() {
        Map<String, Double> source = features();
        LabelledMatchExample labelled = new LabelledMatchExample(source, true);
        UnlabelledMatchExample unlabelled = new UnlabelledMatchExample(source);

        source.put("charlie", 0.5);
        source.put("alpha", 0.0);

        assertThat(labelled.getFeatures()).hasSize(2).containsEntry("alpha", 0.9);
        assertThat(unlabelled.getFeatures()).hasSize(2).containsEntry("alpha", 0.9);
    }

    @Test
    void theExposedMapIsUnmodifiable() {
        Map<String, Double> exposed = new LabelledMatchExample(features(), true).getFeatures();

        assertThatThrownBy(() -> exposed.put("charlie", 0.5))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ------------------------------------------------------------ rejection

    @Test
    void rejectsANullFeatureMap() {
        assertThatThrownBy(() -> new LabelledMatchExample(null, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UnlabelledMatchExample(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANullFeatureName() {
        Map<String, Double> withNullName = new HashMap<>();
        withNullName.put(null, 0.5);

        assertThatThrownBy(() -> new LabelledMatchExample(withNullName, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANullFeatureValue() {
        Map<String, Double> withNullValue = new HashMap<>();
        withNullValue.put("alpha", null);

        assertThatThrownBy(() -> new UnlabelledMatchExample(withNullValue))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anEmptyFeatureMapIsPermitted() {
        // Nothing here knows what a useful feature set looks like, so an
        // empty one is a consumer's problem rather than a rejection.
        assertThat(new UnlabelledMatchExample(Collections.<String, Double>emptyMap())
                .getFeatures()).isEmpty();
    }

    // ------------------------------------------------- no value in toString

    @Test
    void toStringCarriesFeatureNamesButNoFeatureValues() {
        Map<String, Double> features = new LinkedHashMap<>();
        features.put("alpha", 0.123456);
        LabelledMatchExample example = new LabelledMatchExample(features, true);

        assertThat(example.toString()).contains("alpha").doesNotContain("0.123456");
    }
}
