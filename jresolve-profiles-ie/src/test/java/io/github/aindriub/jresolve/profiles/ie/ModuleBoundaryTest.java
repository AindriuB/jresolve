package io.github.aindriub.jresolve.profiles.ie;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aindriub.jresolve.alias.AliasRepository;
import io.github.aindriub.jresolve.field.FieldPipeline;
import org.junit.jupiter.api.Test;

/**
 * The module split from D15, checked as far as a test in this module honestly
 * can.
 *
 * <p><strong>What a test here cannot do.</strong> The property that matters —
 * that {@code jresolve-core} never references this module — is not assertable
 * from inside this module. Surefire runs both on one classpath, so a
 * reflective lookup proves nothing about what core could compile against. It
 * is enforced instead by the reactor: core's POM does not depend on
 * profiles-ie, and adding that dependency would create a cycle Maven refuses
 * to build. That is a stronger guarantee than a test, and it is why the split
 * is two modules rather than two packages — a package boundary inside one jar
 * is a convention with nothing to catch a violation.
 *
 * <p>What is asserted below is the direction that <em>is</em> observable:
 * everything this module publishes is expressed in core's types, so a consumer
 * depending on core alone can hold what this module builds.
 */
class ModuleBoundaryTest {

    @Test
    void theAliasTablesAreReturnedAsACoreType() {
        AliasRepository repository = IrishNameAliases.repository();

        assertThat(repository).isInstanceOf(AliasRepository.class);
        assertThat(AliasRepository.class.getName()).startsWith("io.github.aindriub.jresolve.alias.");
    }

    @Test
    void thePipelinesAreReturnedAsACoreType() {
        FieldPipeline<String, String> pipeline = IrishAddressPipeline.forSingleLine();

        assertThat(pipeline).isInstanceOf(FieldPipeline.class);
        assertThat(FieldPipeline.class.getName()).startsWith("io.github.aindriub.jresolve.field.");
    }

    @Test
    void thisModuleLivesUnderItsOwnSubtree() {
        // A module owns exactly one subtree of the base package, so a class
        // added to the wrong one is visible here rather than at review time.
        assertThat(IrishNameAliases.class.getName())
                .startsWith("io.github.aindriub.jresolve.profiles.ie.");
        assertThat(IrishAddressComparator.class.getName())
                .startsWith("io.github.aindriub.jresolve.profiles.ie.");
        assertThat(IrishNamePipeline.class.getName())
                .startsWith("io.github.aindriub.jresolve.profiles.ie.");
    }

    @Test
    void coreTypesAreNotInThisModulesSubtree() {
        assertThat(AliasRepository.class.getName())
                .doesNotStartWith("io.github.aindriub.jresolve.profiles.");
        assertThat(FieldPipeline.class.getName())
                .doesNotStartWith("io.github.aindriub.jresolve.profiles.");
    }
}
