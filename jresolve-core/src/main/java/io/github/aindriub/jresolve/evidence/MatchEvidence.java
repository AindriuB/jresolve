package io.github.aindriub.jresolve.evidence;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The full set of per-field evidence produced by comparing two records.
 *
 * <p>Fields are generic: this class carries no field name known ahead of
 * time, so the core never hard-codes a particular field.
 */
public final class MatchEvidence {

    private final Map<String, FieldEvidence> fields;
    private final boolean complete;

    /**
     * @param fields the per-field evidence, keyed by field name; copied
     *     defensively and iterated in insertion order
     * @param complete whether evidence exists for every configured field.
     *     False when a cost-tiered comparison was short-circuited by a
     *     rejecting rule before every field was compared.
     */
    public MatchEvidence(Map<String, FieldEvidence> fields, boolean complete) {
        if (fields == null) {
            throw new IllegalArgumentException("fields must not be null");
        }
        for (Map.Entry<String, FieldEvidence> entry : fields.entrySet()) {
            if (entry.getValue() == null) {
                throw new IllegalArgumentException("fields must not contain a null value for key " + entry.getKey());
            }
        }
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<String, FieldEvidence>(fields));
        this.complete = complete;
    }

    public Map<String, FieldEvidence> getFields() {
        return fields;
    }

    /**
     * Returns the evidence for the named field, or null if no evidence was
     * recorded for it.
     */
    public FieldEvidence getField(String name) {
        return fields.get(name);
    }

    /**
     * Whether evidence exists for every configured field. False when
     * comparison was short-circuited before every field was evaluated; a
     * scorer that cannot score partial evidence should check this rather than
     * treat an absent field as missingness.
     */
    public boolean isComplete() {
        return complete;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("MatchEvidence{complete=").append(complete).append(", fields={");
        boolean first = true;
        for (Map.Entry<String, FieldEvidence> entry : fields.entrySet()) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            FieldEvidence evidence = entry.getValue();
            builder.append(entry.getKey()).append('=');
            // No null check: the constructor rejects a null value for any key,
            // so every value here is non-null by construction. A defensive
            // branch would read as though null can occur and invite a later
            // reader to relax that invariant.
            //
            // Only the category and similarity are printed; getFrequencyKey()
            // may carry a normalized field value and must never reach toString().
            builder.append(evidence.getCategory()).append('/').append(evidence.getSimilarity());
        }
        return builder.append("}}").toString();
    }
}
