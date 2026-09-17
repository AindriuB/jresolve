package io.github.aindriub.jresolve.alias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Fixtures are neutral tokens throughout. The real tables belong to the
 * profiles module; nothing here is reference data.
 */
class DefaultAliasRepositoryTest {

    private static DefaultAliasRepository oneGroup() {
        return DefaultAliasRepository.builder()
                .group(Arrays.asList("alpha", "bravo"), ComparisonCategory.ALIAS_NICKNAME)
                .build();
    }

    // ---------------------------------------------------------- equivalents

    @Test
    void equivalentsIncludesTheQueriedValue() {
        assertThat(oneGroup().equivalents("alpha")).contains("alpha");
    }

    @Test
    void equivalentsReturnsTheWholeGroup() {
        assertThat(oneGroup().equivalents("alpha")).containsExactlyInAnyOrder("alpha", "bravo");
    }

    @Test
    void anUnknownValueYieldsASingletonOfItself() {
        assertThat(oneGroup().equivalents("zulu")).containsExactly("zulu");
    }

    @Test
    void equivalentsIsNeverEmpty() {
        assertThat(oneGroup().equivalents("zulu")).isNotEmpty();
    }

    @Test
    void equivalentsIsUnmodifiable() {
        Set<String> group = oneGroup().equivalents("alpha");

        assertThatThrownBy(() -> group.add("charlie"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void theSingletonForAnUnknownValueIsAlsoUnmodifiable() {
        Set<String> group = oneGroup().equivalents("zulu");

        assertThatThrownBy(() -> group.add("charlie"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void equivalentsRejectsNull() {
        assertThatThrownBy(() -> oneGroup().equivalents(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------- relation

    @Test
    void relationReportsTheDeclaredKind() {
        assertThat(oneGroup().relation("alpha", "bravo"))
                .isSameAs(ComparisonCategory.ALIAS_NICKNAME);
    }

    @Test
    void relationIsNullForUnrelatedValues() {
        assertThat(oneGroup().relation("alpha", "zulu")).isNull();
    }

    @Test
    void relationIsNullForTwoUnknownValues() {
        assertThat(oneGroup().relation("yankee", "zulu")).isNull();
    }

    @Test
    void identityIsNotAnAliasRelation() {
        // Identity is equality. A comparator resolves the exact case before
        // consulting a repository, so reporting a kind here would attribute an
        // alias relation to a pair that simply agreed.
        assertThat(oneGroup().relation("alpha", "alpha")).isNull();
    }

    @Test
    void identityIsNullEvenForAnUnknownValue() {
        assertThat(oneGroup().relation("zulu", "zulu")).isNull();
    }

    @Test
    void relationIsSymmetricAcrossAWholeGroup() {
        DefaultAliasRepository repository = DefaultAliasRepository.builder()
                .group(Arrays.asList("alpha", "bravo", "charlie", "delta"),
                        ComparisonCategory.ALIAS_VARIANT)
                .build();

        List<String> members = Arrays.asList("alpha", "bravo", "charlie", "delta");
        for (String left : members) {
            for (String right : members) {
                assertThat(repository.relation(left, right))
                        .isSameAs(repository.relation(right, left));
            }
        }
    }

    @Test
    void relationRejectsNullOnEitherSide() {
        assertThatThrownBy(() -> oneGroup().relation(null, "alpha"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> oneGroup().relation("alpha", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --------------------------------------------------- transitive closure

    @Test
    void closureIsTransitiveAcrossSeparatelyDeclaredEntries() {
        DefaultAliasRepository repository = DefaultAliasRepository.builder()
                .group(Arrays.asList("alpha", "bravo"), ComparisonCategory.ALIAS_NICKNAME)
                .group(Arrays.asList("alpha", "charlie"), ComparisonCategory.ALIAS_NICKNAME)
                .build();

        // bravo and charlie were never declared together.
        assertThat(repository.equivalents("bravo")).contains("charlie");
        assertThat(repository.relation("bravo", "charlie"))
                .isSameAs(ComparisonCategory.ALIAS_NICKNAME);
    }

    @Test
    void groupsSharingAMemberMergeIntoOne() {
        DefaultAliasRepository repository = DefaultAliasRepository.builder()
                .group(Arrays.asList("alpha", "bravo"), ComparisonCategory.ALIAS_NICKNAME)
                .group(Arrays.asList("bravo", "charlie", "delta"), ComparisonCategory.ALIAS_NICKNAME)
                .build();

        List<String> members = Arrays.asList("alpha", "bravo", "charlie", "delta");
        for (String member : members) {
            assertThat(repository.equivalents(member)).containsExactlyInAnyOrderElementsOf(members);
        }
    }

    @Test
    void disjointGroupsDoNotMerge() {
        DefaultAliasRepository repository = DefaultAliasRepository.builder()
                .group(Arrays.asList("alpha", "bravo"), ComparisonCategory.ALIAS_NICKNAME)
                .group(Arrays.asList("charlie", "delta"), ComparisonCategory.ALIAS_NICKNAME)
                .build();

        assertThat(repository.equivalents("alpha")).doesNotContain("charlie");
        assertThat(repository.relation("alpha", "charlie")).isNull();
    }

    // ------------------------------------------- kind across a mixed merge

    @Test
    void aDeclaredPairKeepsItsOwnKindAfterAMerge() {
        DefaultAliasRepository repository = mixedChain();

        assertThat(repository.relation("alpha", "bravo"))
                .isSameAs(ComparisonCategory.ALIAS_VARIANT);
    }

    @Test
    void aDerivedPairTakesTheWeakestLinkOnItsPath() {
        DefaultAliasRepository repository = mixedChain();

        // alpha reaches charlie only through a translation, so the pair cannot
        // claim to be the spelling difference that alpha/bravo is.
        assertThat(repository.relation("alpha", "charlie"))
                .isSameAs(ComparisonCategory.ALIAS_TRANSLATION);
    }

    @Test
    void aMixedMergeDoesNotDegradeEveryPairInTheGroup() {
        // The whole reason the kind is stored per pair rather than per group:
        // collapsing to one kind would lose the distinction a scoring model
        // needs, reporting the declared variant pair as a translation too.
        DefaultAliasRepository repository = mixedChain();

        assertThat(repository.relation("alpha", "bravo"))
                .isNotSameAs(repository.relation("alpha", "charlie"));
    }

    private static DefaultAliasRepository mixedChain() {
        return DefaultAliasRepository.builder()
                .group(Arrays.asList("alpha", "bravo"), ComparisonCategory.ALIAS_VARIANT)
                .group(Arrays.asList("bravo", "charlie"), ComparisonCategory.ALIAS_TRANSLATION)
                .build();
    }

    @Test
    void theStrongestAvailablePathWinsWhenTwoPathsConnectAPair() {
        // alpha-charlie via bravo is a translation chain; alpha-charlie is also
        // declared directly as a variant. The direct, stronger relation wins.
        DefaultAliasRepository repository = DefaultAliasRepository.builder()
                .group(Arrays.asList("alpha", "bravo"), ComparisonCategory.ALIAS_TRANSLATION)
                .group(Arrays.asList("bravo", "charlie"), ComparisonCategory.ALIAS_TRANSLATION)
                .group(Arrays.asList("alpha", "charlie"), ComparisonCategory.ALIAS_VARIANT)
                .build();

        assertThat(repository.relation("alpha", "charlie"))
                .isSameAs(ComparisonCategory.ALIAS_VARIANT);
    }

    // ------------------------------------------------------------ rejection

    @Test
    void rejectsANullGroup() {
        assertThatThrownBy(() -> DefaultAliasRepository.builder()
                .group(null, ComparisonCategory.ALIAS_NICKNAME))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAGroupWithFewerThanTwoDistinctMembers() {
        assertThatThrownBy(() -> DefaultAliasRepository.builder()
                .group(Collections.singletonList("alpha"), ComparisonCategory.ALIAS_NICKNAME))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DefaultAliasRepository.builder()
                .group(Arrays.asList("alpha", "alpha"), ComparisonCategory.ALIAS_NICKNAME))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANullMember() {
        assertThatThrownBy(() -> DefaultAliasRepository.builder()
                .group(Arrays.asList("alpha", null), ComparisonCategory.ALIAS_NICKNAME))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsABlankMember() {
        assertThatThrownBy(() -> DefaultAliasRepository.builder()
                .group(Arrays.asList("alpha", "  "), ComparisonCategory.ALIAS_NICKNAME))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANullKind() {
        assertThatThrownBy(() -> DefaultAliasRepository.builder()
                .group(Arrays.asList("alpha", "bravo"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsACategoryThatIsNotAnAliasKind() {
        assertThatThrownBy(() -> DefaultAliasRepository.builder()
                .group(Arrays.asList("alpha", "bravo"), ComparisonCategory.HIGH))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theRejectionMessageNamesTheConstraintAndNotAMemberValue() {
        assertThatThrownBy(() -> DefaultAliasRepository.builder()
                .group(Arrays.asList("supersecretvalue", "  "), ComparisonCategory.ALIAS_NICKNAME))
                .hasMessageNotContaining("supersecretvalue");
    }

    // ---------------------------------------------------------------- scale

    @Test
    void lookupDoesNotDegradeWithTheNumberOfGroups() {
        // Both lookups are hash lookups, so a repository with a thousand
        // unrelated groups resolves the same as one with a single group. This
        // asserts the structure holds at that size, not a timing.
        DefaultAliasRepository.Builder builder = DefaultAliasRepository.builder();
        for (int i = 0; i < 1000; i++) {
            builder.group(Arrays.asList("left" + i, "right" + i),
                    ComparisonCategory.ALIAS_NICKNAME);
        }
        DefaultAliasRepository repository = builder.build();

        assertThat(repository.equivalents("left500")).containsExactlyInAnyOrder("left500", "right500");
        assertThat(repository.relation("left500", "right500"))
                .isSameAs(ComparisonCategory.ALIAS_NICKNAME);
        assertThat(repository.relation("left500", "right501")).isNull();
    }

    @Test
    void aLongChainClosesCompletely() {
        DefaultAliasRepository.Builder builder = DefaultAliasRepository.builder();
        List<String> chain = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            chain.add("value" + i);
        }
        for (int i = 0; i < chain.size() - 1; i++) {
            builder.group(Arrays.asList(chain.get(i), chain.get(i + 1)),
                    ComparisonCategory.ALIAS_NICKNAME);
        }
        DefaultAliasRepository repository = builder.build();

        // The two ends were never declared together, and are 49 links apart.
        assertThat(repository.equivalents("value0")).hasSize(50);
        assertThat(repository.relation("value0", "value49"))
                .isSameAs(ComparisonCategory.ALIAS_NICKNAME);
    }

    // ------------------------------------------------------------ immutable

    @Test
    void buildingTwiceDoesNotShareMutableState() {
        DefaultAliasRepository.Builder builder = DefaultAliasRepository.builder()
                .group(Arrays.asList("alpha", "bravo"), ComparisonCategory.ALIAS_NICKNAME);
        DefaultAliasRepository first = builder.build();

        builder.group(Arrays.asList("charlie", "delta"), ComparisonCategory.ALIAS_NICKNAME);

        assertThat(first.equivalents("charlie")).containsExactly("charlie");
    }
}
