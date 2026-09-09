package io.github.aindriub.jresolve;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Proves JUnit 5 and AssertJ are wired on the {@code jresolve-core} test
 * classpath. Not a test of any production behaviour.
 */
class BuildSmokeTest {

    @Test
    void assertJAssertionPasses() {
        assertThat(1 + 1).isEqualTo(2);
    }
}
