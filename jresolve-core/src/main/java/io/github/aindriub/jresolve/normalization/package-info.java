/**
 * String normalizers that bring values to a common form before comparison.
 *
 * <p>Implement {@link io.github.aindriub.jresolve.normalization.StringNormalizer}
 * to add a step. The shipped steps are
 * {@link io.github.aindriub.jresolve.normalization.UnicodeFormNormalizer},
 * {@link io.github.aindriub.jresolve.normalization.CaseFoldNormalizer},
 * {@link io.github.aindriub.jresolve.normalization.CombiningMarkNormalizer},
 * {@link io.github.aindriub.jresolve.normalization.ApostropheVariantNormalizer},
 * {@link io.github.aindriub.jresolve.normalization.PunctuationNormalizer} and
 * {@link io.github.aindriub.jresolve.normalization.WhitespaceNormalizer};
 * {@link io.github.aindriub.jresolve.normalization.CompositeNormalizer} chains
 * them in order. Normalizers run in the prepare half of a field pipeline.
 */
package io.github.aindriub.jresolve.normalization;
