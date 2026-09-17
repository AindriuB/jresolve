package io.github.aindriub.jresolve.endtoend;

import java.time.LocalDate;

/**
 * A synthetic, test-only stored record resolved against, per
 * {@code docs/spec/original-design.md} §87. See {@link IncomingRecord}'s
 * Javadoc for the synthetic-data statement and the deliberate {@code
 * locator} shape divergence between the two types.
 */
public final class StoredRecord {

    private final String id;
    private final String label;
    private final String serial;
    private final LocalDate issuedOn;
    private final String locator;

    public StoredRecord(String id, String label, String serial, LocalDate issuedOn, String locator) {
        this.id = id;
        this.label = label;
        this.serial = serial;
        this.issuedOn = issuedOn;
        this.locator = locator;
    }

    public String getId() {
        return id;
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

    public String getLocator() {
        return locator;
    }
}
