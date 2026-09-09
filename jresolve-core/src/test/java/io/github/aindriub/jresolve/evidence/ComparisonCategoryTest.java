package io.github.aindriub.jresolve.evidence;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComparisonCategoryTest {

    @Test
    void ofInternsKnownConstantByName() {
        assertThat(ComparisonCategory.of("EXACT")).isSameAs(ComparisonCategory.EXACT);
    }

    @Test
    void ofReturnsSameInstanceForRepeatedName() {
        ComparisonCategory first = ComparisonCategory.of("SAME_SUBSCRIBER");
        ComparisonCategory second = ComparisonCategory.of("SAME_SUBSCRIBER");
        assertThat(first).isSameAs(second);
    }

    @Test
    void equalsAndHashCodeAreConsistentWithName() {
        ComparisonCategory a = ComparisonCategory.of("EQUALITY_CHECK");
        ComparisonCategory b = ComparisonCategory.of("EQUALITY_CHECK");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void ofRejectsNullName() {
        assertThatThrownBy(() -> ComparisonCategory.of(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ofRejectsEmptyName() {
        assertThatThrownBy(() -> ComparisonCategory.of("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ofRejectsWhitespaceOnlyName() {
        assertThatThrownBy(() -> ComparisonCategory.of("   ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ofTrimsTheNameBeforeUsingItAsTheInternKey() {
        assertThat(ComparisonCategory.of(" HIGH ")).isSameAs(ComparisonCategory.HIGH);
    }

    @Test
    void allStandardConstantsExist() {
        assertThat(ComparisonCategory.EXACT.getName()).isEqualTo("EXACT");
        assertThat(ComparisonCategory.ALIAS_TRANSLATION.getName()).isEqualTo("ALIAS_TRANSLATION");
        assertThat(ComparisonCategory.ALIAS_NICKNAME.getName()).isEqualTo("ALIAS_NICKNAME");
        assertThat(ComparisonCategory.ALIAS_VARIANT.getName()).isEqualTo("ALIAS_VARIANT");
        assertThat(ComparisonCategory.VERY_HIGH.getName()).isEqualTo("VERY_HIGH");
        assertThat(ComparisonCategory.HIGH.getName()).isEqualTo("HIGH");
        assertThat(ComparisonCategory.MEDIUM.getName()).isEqualTo("MEDIUM");
        assertThat(ComparisonCategory.LOW.getName()).isEqualTo("LOW");
        assertThat(ComparisonCategory.CONFLICT.getName()).isEqualTo("CONFLICT");
        assertThat(ComparisonCategory.MISSING_ONE.getName()).isEqualTo("MISSING_ONE");
        assertThat(ComparisonCategory.MISSING_BOTH.getName()).isEqualTo("MISSING_BOTH");
    }

    @Test
    void concurrentInterningOfANewNameYieldsOneIdentity() throws Exception {
        String name = "CONCURRENT_INTERN_TEST";
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);

            Future<ComparisonCategory> first = pool.submit(() -> {
                ready.countDown();
                go.await();
                return ComparisonCategory.of(name);
            });
            Future<ComparisonCategory> second = pool.submit(() -> {
                ready.countDown();
                go.await();
                return ComparisonCategory.of(name);
            });

            ready.await();
            go.countDown();

            ComparisonCategory result1 = first.get(5, TimeUnit.SECONDS);
            ComparisonCategory result2 = second.get(5, TimeUnit.SECONDS);

            assertThat(result1).isSameAs(result2);
        } finally {
            pool.shutdown();
        }
    }
}
