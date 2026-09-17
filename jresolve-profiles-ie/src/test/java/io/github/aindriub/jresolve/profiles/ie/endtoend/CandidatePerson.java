package io.github.aindriub.jresolve.profiles.ie.endtoend;

import java.time.LocalDate;

/**
 * The internal register's shape: a name in English, a date of birth, and a
 * single-line address.
 *
 * <p>Every value used with this type is invented; see {@link SourcePerson}.
 */
final class CandidatePerson {

    private final String id;
    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final String address;

    CandidatePerson(String id, String firstName, String lastName, LocalDate dateOfBirth, String address) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
    }

    String getId() {
        return id;
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

    String getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "CandidatePerson{" + id + '}';
    }
}
