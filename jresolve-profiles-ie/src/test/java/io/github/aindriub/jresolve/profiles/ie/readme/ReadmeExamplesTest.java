package io.github.aindriub.jresolve.profiles.ie.readme;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aindriub.jresolve.alias.AliasRepository;
import io.github.aindriub.jresolve.api.EntityResolver;
import io.github.aindriub.jresolve.api.EntityResolverBuilder;
import io.github.aindriub.jresolve.decision.DecisionThresholds;
import io.github.aindriub.jresolve.decision.ThresholdDecisionEngine;
import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.evidence.TokenSubsumption;
import io.github.aindriub.jresolve.field.FieldPipeline;
import io.github.aindriub.jresolve.field.TokenSubsumptionComparator;
import io.github.aindriub.jresolve.profiles.ie.IrishAddressComparator;
import io.github.aindriub.jresolve.profiles.ie.IrishAddressPipeline;
import io.github.aindriub.jresolve.profiles.ie.IrishNameAliases;
import io.github.aindriub.jresolve.profiles.ie.IrishNamePipeline;
import io.github.aindriub.jresolve.result.Decision;
import io.github.aindriub.jresolve.result.MatchResult;
import io.github.aindriub.jresolve.result.ScoreScale;
import io.github.aindriub.jresolve.scoring.RuleBasedScorer;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The {@code README.md} examples that need a domain profile, compiled and run.
 * The neutral examples live in {@code jresolve-core}'s copy of this file;
 * {@code docs/conventions.md} binds core's tests, so domain vocabulary belongs
 * only here.
 *
 * <p>Every value below is invented for this file. The alias tables these
 * examples use are illustrative and hand-written, not reference data — see
 * D19.
 */
class ReadmeExamplesTest {

    static final class Applicant {
        private final String givenName;
        private final String surname;
        private final List<String> addressLines;

        Applicant(String givenName, String surname, List<String> addressLines) {
            this.givenName = givenName;
            this.surname = surname;
            this.addressLines = addressLines;
        }

        String getGivenName() {
            return givenName;
        }

        String getSurname() {
            return surname;
        }

        List<String> getAddressLines() {
            return addressLines;
        }
    }

    static final class Record {
        private final String id;
        private final String givenName;
        private final String surname;
        private final String address;

        Record(String id, String givenName, String surname, String address) {
            this.id = id;
            this.givenName = givenName;
            this.surname = surname;
            this.address = address;
        }

        String getId() {
            return id;
        }

        String getGivenName() {
            return givenName;
        }

        String getSurname() {
            return surname;
        }

        String getAddress() {
            return address;
        }
    }

    // ------------------------------------------------ 1. the alias repository

    @Test
    void aliasesRelateTwoRenderingsOfOneName() {
        AliasRepository aliases = IrishNameAliases.repository();

        // Lookups are on the normalized value, so the repository never sees a
        // fada or an apostrophe variant.
        assertThat(aliases.relation("sean", "john"))
                .isEqualTo(ComparisonCategory.ALIAS_TRANSLATION);
        assertThat(aliases.equivalents("sean")).contains("john");

        // Unrelated values relate to nothing rather than weakly.
        assertThat(aliases.relation("sean", "margaret")).isNull();
    }

    // ----------------------------------------- 2. a resolver that beats a join

    @Test
    void aliasAndSubsumptionMatchWhereEqualityCannot() {
        FieldPipeline<String, String> namePipeline =
                IrishNamePipeline.withAliases(IrishNameAliases.repository());
        FieldPipeline<List<String>, String> sourceAddress = IrishAddressPipeline.forLines();
        FieldPipeline<String, String> candidateAddress = IrishAddressPipeline.forSingleLine();

        DecisionThresholds thresholds =
                new DecisionThresholds(40.0, 15.0, 1.0, ScoreScale.POINTS);

        EntityResolver<Applicant, Record> resolver = EntityResolverBuilder
                .<Applicant, Record>builder()
                .field("givenName", Applicant::getGivenName, Record::getGivenName, namePipeline)
                .field("surname", Applicant::getSurname, Record::getSurname, namePipeline)
                // The asymmetric overload: the source carries address lines
                // and the candidate a single joined string, so each side gets
                // its own normalizer and they converge on one compared type.
                .field("address", Applicant::getAddressLines, Record::getAddress,
                        sourceAddress::prepare, candidateAddress::prepare,
                        new IrishAddressComparator())
                .scorer(RuleBasedScorer.builder()
                        .weight("givenName", ComparisonCategory.EXACT, 25.0)
                        .weight("givenName", ComparisonCategory.ALIAS_TRANSLATION, 20.0)
                        .weight("givenName", ComparisonCategory.ALIAS_VARIANT, 22.0)
                        .weight("givenName", ComparisonCategory.ALIAS_NICKNAME, 18.0)
                        .weight("surname", ComparisonCategory.EXACT, 30.0)
                        .weight("surname", ComparisonCategory.ALIAS_TRANSLATION, 25.0)
                        .weight("surname", ComparisonCategory.ALIAS_VARIANT, 27.0)
                        .weight("address", ComparisonCategory.EXACT, 15.0)
                        // SUBSUMED is not a ComparisonCategory constant.
                        // Categories are an open value type (D3), and the
                        // comparator that can produce this one owns it.
                        .weight("address", TokenSubsumptionComparator.SUBSUMED, 12.0)
                        .weight("address", ComparisonCategory.VERY_HIGH, 12.0)
                        .weight("address", ComparisonCategory.HIGH, 8.0)
                        .baseScore(0.0)
                        .build())
                .thresholds(thresholds)
                .decisionEngine(new ThresholdDecisionEngine<>(thresholds))
                .build();

        // Seán/John and Ó Súilleabháin/O'Sullivan are the same two people
        // rendered in two languages. An equality join matches neither.
        MatchResult<Record> result = resolver.resolve(
                new Applicant("Seán", "Ó Súilleabháin",
                        Arrays.asList("12 Main Street", "Bandon", "Co. Cork")),
                Arrays.asList(new Record("1", "John", "O'Sullivan", "12 Main Street, Bandon, Co. Cork")));

        assertThat(result.getDecision()).isEqualTo(Decision.MATCH);
        assertThat(result.getMatch().getId()).isEqualTo("1");
    }

    // ------------------------------------------------------- 3. subsumption

    @Test
    void aShorterAddressIsSubsumedRatherThanInConflict() {
        FieldPipeline<String, String> pipeline = IrishAddressPipeline.forSingleLine();

        // "Bandon, Co. Cork" is less specific than the full address, not a
        // contradiction of it. A similarity metric cannot tell those apart;
        // this is the distinction that makes a REVIEW band meaningful.
        String less = pipeline.prepare("Bandon, Co. Cork");
        String more = pipeline.prepare("12 Main Street, Bandon, Co. Cork");

        assertThat(pipeline.compare(less, more).getSubsumption())
                .isEqualTo(TokenSubsumption.LEFT_SUBSUMES_RIGHT);
        assertThat(pipeline.compare(less, more).getCategory())
                .isNotEqualTo(ComparisonCategory.CONFLICT);
    }
}
