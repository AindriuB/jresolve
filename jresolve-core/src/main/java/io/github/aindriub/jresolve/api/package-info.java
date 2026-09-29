/**
 * The entry point: build an entity resolver and ask it to resolve a source
 * record against candidates.
 *
 * <p>The flow is: configure an
 * {@link io.github.aindriub.jresolve.api.EntityResolverBuilder} with fields,
 * optional {@link io.github.aindriub.jresolve.api.CandidateRule}s, a scorer and
 * a decision engine or thresholds; call {@code build()} to get an
 * {@link io.github.aindriub.jresolve.api.EntityResolver}, whose implementation
 * is {@link io.github.aindriub.jresolve.api.DefaultEntityResolver}; then call
 * {@code EntityResolver.resolve} to get a
 * {@link io.github.aindriub.jresolve.result.MatchResult}. The builder fails at
 * {@code build()} with an
 * {@link io.github.aindriub.jresolve.api.EntityResolutionConfigurationException}
 * when the configuration is inconsistent, for example thresholds expressed on
 * a different scale from the scorer's.
 *
 * <p>The source is prepared once per {@code resolve} call, not once per
 * candidate. Fields are compared cheapest first, and a hard rule may reject a
 * candidate between cost tiers.
 */
package io.github.aindriub.jresolve.api;
