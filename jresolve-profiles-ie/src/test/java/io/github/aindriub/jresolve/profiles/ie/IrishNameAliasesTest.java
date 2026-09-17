package io.github.aindriub.jresolve.profiles.ie;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aindriub.jresolve.alias.AliasRepository;
import io.github.aindriub.jresolve.evidence.ComparisonCategory;
import io.github.aindriub.jresolve.normalization.StringNormalizer;
import org.junit.jupiter.api.Test;

class IrishNameAliasesTest {

    private final AliasRepository repository = IrishNameAliases.repository();
    private final StringNormalizer normalizer = new IrishNameNormalizer();

    private ComparisonCategory relate(String left, String right) {
        return repository.relation(normalizer.normalize(left), normalizer.normalize(right));
    }

    // ------------------------------------------------------ the three kinds

    @Test
    void anIrishAndEnglishGivenNameRelateAsATranslation() {
        assertThat(relate("Seán", "John")).isSameAs(ComparisonCategory.ALIAS_TRANSLATION);
    }

    @Test
    void aDiminutiveRelatesAsANickname() {
        assertThat(relate("William", "Will")).isSameAs(ComparisonCategory.ALIAS_NICKNAME);
    }

    @Test
    void aSpellingVariantRelatesAsAVariant() {
        assertThat(relate("Eoghan", "Eoin")).isSameAs(ComparisonCategory.ALIAS_VARIANT);
    }

    // ------------------------------------------------- the D8 surname pairs

    @Test
    void theIrishAndEnglishFormsOfASurnameResolveHere() {
        // D8's worked example. Normalization leaves these as "o suilleabhain"
        // and "osullivan" and cannot do better; this is where the pair is
        // actually closed.
        assertThat(relate("Ó Súilleabháin", "O'Sullivan"))
                .isSameAs(ComparisonCategory.ALIAS_TRANSLATION);
    }

    @Test
    void anotherIrishAndEnglishSurnameFormAlsoResolves() {
        assertThat(relate("Mac Cárthaigh", "McCarthy"))
                .isSameAs(ComparisonCategory.ALIAS_TRANSLATION);
    }

    // ------------------------------------------------- normalization parity

    @Test
    void aDiacriticBearingLookupHits() {
        // The table is stored normalized, so a caller whose value still
        // carries a fada must be preparing it with the same normalizer. If
        // these two ever drift apart every lookup silently misses.
        //
        // Note this asserts the *declared* pair. Pádraig also reaches Paddy,
        // but only through Patrick, so that pair reports the weakest link on
        // the path — a nickname under D7's order — rather than the translation
        // declared here. See the mixed-merge test below.
        assertThat(relate("Pádraig", "Patrick")).isSameAs(ComparisonCategory.ALIAS_TRANSLATION);
    }

    @Test
    void aDiacriticBearingLookupAlsoReachesADerivedPair() {
        // Pádraig reaches Paddy only through Patrick: a translation edge then
        // a nickname edge. D7 ranks the nickname lowest, so it is the weakest
        // link and the derived pair claims it. Task 15 first wrote this
        // expectation as a nickname and was corrected by the old ordering;
        // the reorder in task 29 puts it back.
        assertThat(relate("Pádraig", "Paddy")).isSameAs(ComparisonCategory.ALIAS_NICKNAME);
    }

    @Test
    void aRawUnnormalizedLookupMisses() {
        // Stated so the requirement is visible rather than folklore: the
        // repository applies no normalization of its own.
        assertThat(repository.relation("Pádraig", "Paddy")).isNull();
    }

    // -------------------------------------------- closure through real data

    @Test
    void aNicknameOfATranslationIsReachableFromEitherEnd() {
        // "Pádraig"/"Patrick" is a translation group; "Patrick"/"Paddy"/"Pat"
        // is a nickname group. They share a member, so the closure joins them
        // and the Irish form reaches the diminutive it was never declared
        // with.
        assertThat(repository.equivalents(normalizer.normalize("Pádraig")))
                .contains(normalizer.normalize("Paddy"), normalizer.normalize("Pat"));
        assertThat(repository.equivalents(normalizer.normalize("Pat")))
                .contains(normalizer.normalize("Pádraig"));
    }

    @Test
    void aDerivedPairAcrossAMixedMergeTakesTheWeakestLink() {
        // liam/william is a translation, william/will a nickname. Under D7's
        // order the nickname is the weaker of the two, so the derived
        // liam/will pair reports it — while both declared edges keep the kind
        // they were declared with. A repository storing one kind per group
        // could not say all three.
        assertThat(relate("Liam", "William")).isSameAs(ComparisonCategory.ALIAS_TRANSLATION);
        assertThat(relate("William", "Will")).isSameAs(ComparisonCategory.ALIAS_NICKNAME);
        assertThat(relate("Liam", "Will")).isSameAs(ComparisonCategory.ALIAS_NICKNAME);
    }

    // --------------------------------------------------------- not too eager

    @Test
    void twoUnrelatedNamesAreNotAliases() {
        assertThat(relate("Seán", "Kevin")).isNull();
    }

    @Test
    void aNameOutsideTheTableIsItsOwnSingleton() {
        assertThat(repository.equivalents(normalizer.normalize("Fionnuala")))
                .containsExactly(normalizer.normalize("Fionnuala"));
    }

    // ------------------------------------------------------------- the table

    @Test
    void theTableBuildsWithoutACollapsedGroup() {
        // The builder rejects a group whose members all normalize to one
        // value. That makes this a real check on the data: an entry whose two
        // spellings normalization already closes does not belong in an alias
        // table, and adding one fails here rather than shipping dead data.
        assertThat(IrishNameAliases.repository()).isNotNull();
    }
}
