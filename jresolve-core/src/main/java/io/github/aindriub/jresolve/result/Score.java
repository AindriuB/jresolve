package io.github.aindriub.jresolve.result;

/**
 * The output of scoring one candidate: a value on a known {@link ScoreScale},
 * the algorithm that produced it, and an optional calibrated probability.
 *
 * <p>{@code probability} is null unless the scoring model genuinely produces
 * a calibrated probability of a true match — never a similarity or a
 * likelihood ratio dressed up as one. A caller that needs a probability and
 * gets null is looking at a model that does not offer one, not a bug.
 */
public final class Score {

    private final double value;
    private final ScoreScale scale;
    private final String algorithm;
    private final Double probability;

    public Score(double value, ScoreScale scale, String algorithm, Double probability) {
        if (scale == null) {
            throw new IllegalArgumentException("scale must not be null");
        }
        if (probability != null && (probability < 0.0 || probability > 1.0)) {
            throw new IllegalArgumentException("probability must be within [0,1] when present");
        }
        this.value = value;
        this.scale = scale;
        this.algorithm = algorithm;
        this.probability = probability;
    }

    public double getValue() {
        return value;
    }

    public ScoreScale getScale() {
        return scale;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * The calibrated probability of a true match, or null if the algorithm
     * that produced this score does not offer one. See the class Javadoc.
     */
    public Double getProbability() {
        return probability;
    }

    @Override
    public String toString() {
        return "Score{value=" + value + ", scale=" + scale + ", algorithm=" + algorithm
                + ", probability=" + probability + '}';
    }
}
