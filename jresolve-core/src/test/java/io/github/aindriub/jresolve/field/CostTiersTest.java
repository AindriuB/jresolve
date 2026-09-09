package io.github.aindriub.jresolve.field;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CostTiersTest {

    @Test
    void tiersAreStrictlyAscending() {
        assertThat(CostTiers.CHEAP).isLessThan(CostTiers.MODERATE);
        assertThat(CostTiers.MODERATE).isLessThan(CostTiers.EXPENSIVE);
    }
}
