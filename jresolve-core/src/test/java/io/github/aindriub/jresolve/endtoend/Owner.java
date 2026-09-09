package io.github.aindriub.jresolve.endtoend;

import java.time.LocalDate;

/**
 * A synthetic, test-only "owner" record resolved against, per
 * {@code docs/spec/original-design.md} §87. See {@link ExternalPerson}'s
 * Javadoc for the synthetic-data statement and the deliberate {@code
 * address} shape divergence between the two types.
 */
public final class Owner {

    private final String id;
    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final String address;

    public Owner(String id, String firstName, String lastName, LocalDate dateOfBirth, String address) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
    }

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getAddress() {
        return address;
    }
}
