package io.github.aindriub.jresolve.comparison;

import java.util.List;

/**
 * Splits a string into the tokens {@link TokenSimilarity} compares
 * individually. Never receives a {@code null} argument; the empty string
 * splits to an empty list.
 */
public interface TokenSplitter {

    List<String> split(String value);
}
