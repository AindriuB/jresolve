package io.github.aindriub.jresolve;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * {@code docs/conventions.md} forbids this module from naming a domain
 * concept. Until this test existed the rule was checked by a grep somebody
 * remembered to run, and it had been violated in {@code src/main} since task
 * 01 without anyone noticing.
 *
 * <p><strong>What this test cannot do.</strong> It matches text, and text does
 * not distinguish a domain concept from an English word that looks like one.
 * {@code ComparisonCategory} says "the interned category with the given name",
 * meaning the supplied name, and a checker banning the spaced form "given
 * name" would fail on correct code. So the identifier forms are banned for
 * every variant and the spaced prose forms only for the unambiguous ones,
 * which leaves a real hole: a domain concept named in words this list does not
 * hold passes silently. A clean run here is necessary and not sufficient.
 *
 * <p>It reads <em>sources</em> rather than compiled classes deliberately. The
 * rule covers Javadoc, and Javadoc is not in the bytecode.
 *
 * <p>It does not read {@code jresolve-profiles-ie}. The rule does not bind it:
 * domain vocabulary is that module's whole point.
 */
class DomainVocabularyTest {

    /**
     * Identifier spellings, banned wherever they appear, with no word
     * boundaries required: {@code getLastName} has no boundary before
     * {@code Last}, and requiring one silently misses every accessor.
     *
     * <p>The name forms admit an underscore but not a space, which is what
     * separates {@code givenName} from the English "given name".
     */
    private static final Pattern IDENTIFIERS = Pattern.compile(
            "(first|last|full|given|family|maiden)_?name"
                    + "|date_?of_?birth|birth_?date"
                    + "|address|person|people|irish|surname|forename"
                    + "|\\bdob\\w*",
            Pattern.CASE_INSENSITIVE);

    /**
     * Prose spellings, banned only where the phrase is unambiguous. "given
     * name" is deliberately absent — see this class's Javadoc.
     */
    private static final Pattern PROSE = Pattern.compile(
            "\\b(first|last|full|maiden) name\\b|\\bdate of birth\\b",
            Pattern.CASE_INSENSITIVE);

    private static final List<Path> ROOTS = Arrays.asList(
            Paths.get("src", "main", "java"),
            Paths.get("src", "test", "java"));

    // -------------------------------------------------- the rule, enforced

    @Test
    void coreNamesNoDomainConcept() {
        List<String> violations = new ArrayList<>();
        int filesRead = 0;

        for (Path root : ROOTS) {
            for (Path file : javaFilesUnder(root)) {
                filesRead++;
                if (isThisChecker(file)) {
                    // This file holds known-bad strings on purpose. A checker
                    // cannot police its own fixtures, and pretending otherwise
                    // would mean writing the samples obfuscated -- which would
                    // test an obfuscation rather than the rule.
                    continue;
                }
                violations.addAll(violationsIn(file));
            }
        }

        // A path or working-directory mistake must not look like a pass. This
        // is the failure mode that makes a source-reading test worthless: it
        // reads nothing, finds nothing, and reports success.
        assertThat(filesRead)
                .as("java sources found under %s — zero means this test read nothing", ROOTS)
                .isGreaterThan(50);

        assertThat(violations)
                .as("docs/conventions.md forbids domain vocabulary in jresolve-core")
                .isEmpty();
    }

    // ------------------------------------------- the checker, checked itself

    @Test
    void theCheckerCatchesWhatTheRuleBans() {
        // A checker that matches nothing passes a clean repository, so it is
        // fed known-bad input rather than trusted for a green run.
        assertThat(matches("private final String firstName;")).isTrue();
        assertThat(matches("public String getLastName() {")).isTrue();
        assertThat(matches("private final LocalDate dateOfBirth;")).isTrue();
        assertThat(matches("private final String birthDate;")).isTrue();
        assertThat(matches("String address = line;")).isTrue();
        assertThat(matches("// a real person's record")).isTrue();
        assertThat(matches("IrishNameAliases.repository()")).isTrue();
        assertThat(matches("private final String surname;")).isTrue();
        assertThat(matches("// the record has no first name on file")).isTrue();
        assertThat(matches("// nothing here is a date of birth")).isTrue();
    }

    @Test
    void theCheckerPassesTheEnglishThatMerelyLooksLikeIt() {
        // The false positive this design exists to avoid. "the given name"
        // means the supplied one; ComparisonCategory:42 says exactly this and
        // is correct code.
        assertThat(matches("Returns the interned category with the given name")).isFalse();
        assertThat(matches("the field name this definition carries")).isFalse();
        assertThat(matches("requiredFieldNames.add(name);")).isFalse();
        assertThat(matches("throw new IllegalArgumentException(\"name must not be null\");")).isFalse();
    }

    @Test
    void theCheckerCatchesTheIdentifierFormOfAnAmbiguousWord() {
        // "given name" spaced is English; givenName is not. The ambiguity is
        // in the prose form alone, so the identifier form stays banned.
        assertThat(matches("private final String givenName;")).isTrue();
        assertThat(matches("private final String familyName;")).isTrue();
    }

    // ------------------------------------------------------------- plumbing

    private static boolean isThisChecker(Path file) {
        return file.getFileName().toString().equals("DomainVocabularyTest.java");
    }

    private static boolean matches(String line) {
        return IDENTIFIERS.matcher(line).find() || PROSE.matcher(line).find();
    }

    private static List<String> violationsIn(Path file) {
        List<String> found = new ArrayList<>();
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("could not read " + file, e);
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String hit = firstHit(line);
            if (hit != null) {
                found.add(file + ":" + (i + 1) + " — " + hit);
            }
        }
        return found;
    }

    private static String firstHit(String line) {
        Matcher identifier = IDENTIFIERS.matcher(line);
        if (identifier.find()) {
            return identifier.group();
        }
        Matcher prose = PROSE.matcher(line);
        return prose.find() ? prose.group() : null;
    }

    private static List<Path> javaFilesUnder(Path root) {
        if (!Files.isDirectory(root)) {
            return new ArrayList<>();
        }
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> files = new ArrayList<>();
            for (Path path : (Iterable<Path>) paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))::iterator) {
                files.add(path);
            }
            return files;
        } catch (IOException e) {
            throw new IllegalStateException("could not walk " + root, e);
        }
    }
}
