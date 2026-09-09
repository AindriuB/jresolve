package io.github.aindriub.jresolve.endtoend;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A synthetic, test-only "external" record: the shape a source record takes
 * before it is resolved against an {@link Owner}, per
 * {@code docs/spec/original-design.md} §87. Every value ever constructed with
 * this type in this package is invented for the test; none is drawn from a
 * real person, a public dataset or any external source.
 *
 * <p>{@code address} is deliberately shaped differently from {@link Owner}'s
 * — a list of lines here, a single joined string there — so the end-to-end
 * tests that use both types are forced through the asymmetric
 * {@code field(...)} overload (D1) rather than the symmetric sugar.
 */
public final class ExternalPerson {

    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final List<String> address;

    public ExternalPerson(String firstName, String lastName, LocalDate dateOfBirth, List<String> address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.address = address == null ? null : Collections.unmodifiableList(new ArrayList<String>(address));
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

    public List<String> getAddress() {
        return address;
    }
}
