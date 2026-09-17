package io.github.aindriub.jresolve.endtoend;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A synthetic, test-only "external" record: the shape a source record takes
 * before it is resolved against a {@link StoredRecord}, per
 * {@code docs/spec/original-design.md} §87. Every value ever constructed with
 * this type in this package is invented for the test; none describes anyone
 * real, and none is drawn from a public dataset or any external source.
 *
 * <p>The field names are deliberately neutral. {@code docs/conventions.md}
 * binds {@code src/test} as well as {@code src/main}, so a core fixture may
 * not teach core what a domain is — domain-shaped fixtures live in
 * {@code jresolve-profiles-ie}, where the vocabulary is the point.
 *
 * <p>{@code locator} is deliberately shaped differently from {@link StoredRecord}'s
 * — a list of lines here, a single joined string there — so the end-to-end
 * tests that use both types are forced through the asymmetric
 * {@code field(...)} overload (D1) rather than the symmetric sugar.
 */
public final class IncomingRecord {

    private final String label;
    private final String serial;
    private final LocalDate issuedOn;
    private final List<String> locator;

    public IncomingRecord(String label, String serial, LocalDate issuedOn, List<String> locator) {
        this.label = label;
        this.serial = serial;
        this.issuedOn = issuedOn;
        this.locator = locator == null ? null : Collections.unmodifiableList(new ArrayList<String>(locator));
    }

    public String getLabel() {
        return label;
    }

    public String getSerial() {
        return serial;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }

    public List<String> getLocator() {
        return locator;
    }
}
