package io.github.aindriub.jresolve.api;

/**
 * Thrown by {@link EntityResolverBuilder#build()} when a configuration
 * cannot possibly resolve correctly — a missing field, a duplicate name, a
 * threshold on the wrong scale — rather than deferring the failure to
 * {@link EntityResolver#resolve}.
 *
 * <p>Every message names the field or the constraint that failed; never a
 * value taken from a source or candidate object, since none has been seen
 * yet at construction time.
 */
public class EntityResolutionConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EntityResolutionConfigurationException(String message) {
        super(message);
    }

    public EntityResolutionConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
