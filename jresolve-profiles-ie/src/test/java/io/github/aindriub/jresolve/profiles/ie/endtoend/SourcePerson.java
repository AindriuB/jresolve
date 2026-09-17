package io.github.aindriub.jresolve.profiles.ie.endtoend;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The external feed's shape: a name in Irish, no guaranteed date of birth,
 * and an address split across lines.
 *
 * <p>Every value used with this type is invented. None names a real person
 * and none is drawn from any dataset; the name forms are ordinary public
 * linguistic facts.
 *
 * <p>The address is a {@code List<String>} where {@link CandidatePerson}
 * carries one string, which is the asymmetric case D1 exists for — two
 * systems that model one field differently and converge only after
 * preparation.
 */
final class SourcePerson {

    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final List<String> addressLines;

    SourcePerson(String firstName, String lastName, LocalDate dateOfBirth, List<String> addressLines) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.addressLines = addressLines == null
                ? null
                : Collections.unmodifiableList(new ArrayList<>(addressLines));
    }

    String getFirstName() {
        return firstName;
    }

    String getLastName() {
        return lastName;
    }

    LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    List<String> getAddressLines() {
        return addressLines;
    }
}
