package io.github.aindriub.jresolve.comparison;

/**
 * Levenshtein-derived similarity: one minus the edit distance normalized by
 * the longer string's length.
 *
 * <p>Bounded in {@code [0.0, 1.0]}, exactly symmetric and {@code null}-safe:
 * a {@code null} argument is treated as the empty string.
 */
public final class LevenshteinSimilarity implements SimilarityMetric {

    @Override
    public double similarity(String left, String right) {
        String a = left == null ? "" : left;
        String b = right == null ? "" : right;

        if (a.isEmpty() && b.isEmpty()) {
            return 1.0;
        }
        int maxLength = Math.max(a.length(), b.length());
        int distance = distance(a, b);
        return 1.0 - ((double) distance / maxLength);
    }

    /** Edit distance computed with O(min(m,n)) working memory. */
    private static int distance(String a, String b) {
        String shorter = a.length() <= b.length() ? a : b;
        String longer = a.length() <= b.length() ? b : a;

        int shortLength = shorter.length();
        int[] previousRow = new int[shortLength + 1];
        int[] currentRow = new int[shortLength + 1];

        for (int j = 0; j <= shortLength; j++) {
            previousRow[j] = j;
        }

        for (int i = 1; i <= longer.length(); i++) {
            currentRow[0] = i;
            char longerChar = longer.charAt(i - 1);
            for (int j = 1; j <= shortLength; j++) {
                int cost = longerChar == shorter.charAt(j - 1) ? 0 : 1;
                int deletion = previousRow[j] + 1;
                int insertion = currentRow[j - 1] + 1;
                int substitution = previousRow[j - 1] + cost;
                currentRow[j] = Math.min(Math.min(deletion, insertion), substitution);
            }
            int[] swap = previousRow;
            previousRow = currentRow;
            currentRow = swap;
        }

        return previousRow[shortLength];
    }
}
