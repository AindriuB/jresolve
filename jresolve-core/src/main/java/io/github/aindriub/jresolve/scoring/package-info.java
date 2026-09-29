/**
 * Scorers that turn match evidence into a score on an explicit scale.
 *
 * <p>Implement {@link io.github.aindriub.jresolve.scoring.MatchScorer} or use
 * {@link io.github.aindriub.jresolve.scoring.RuleBasedScorer}, which weights
 * categories by rule, or
 * {@link io.github.aindriub.jresolve.scoring.FellegiSunterScorer}, which sums
 * log-likelihood-ratio weights from a
 * {@link io.github.aindriub.jresolve.scoring.FellegiSunterModel}
 * ({@link io.github.aindriub.jresolve.scoring.DefaultFellegiSunterModel}).
 * Model parameters can be estimated from
 * {@link io.github.aindriub.jresolve.scoring.LabelledMatchExample}s or
 * {@link io.github.aindriub.jresolve.scoring.UnlabelledMatchExample}s.
 *
 * <p>A similarity is not a probability, and a raw score is not one either. A
 * probability is only produced when the model carries prior odds, and nothing
 * shipped here is calibrated: any figures depend on the parameters you
 * supply. See {@code docs/calibration.md} in the repository.
 */
package io.github.aindriub.jresolve.scoring;
