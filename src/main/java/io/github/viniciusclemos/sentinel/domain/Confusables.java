package io.github.viniciusclemos.sentinel.domain;

import java.text.Normalizer;
import java.util.Map;

/**
 * Reduces a domain label to the plain letters it looks like.
 *
 * <p>Two different tricks end up in the same place here: letters from other alphabets that look
 * Latin (Cyrillic "а", Greek "ο", Turkish dotless "ı") and digits or letter pairs that look like a
 * letter ("0" for "o", "rn" for "m"). This is a hand-picked subset of Unicode's confusables list,
 * the characters that actually show up in phishing domains, not the full table.
 */
public final class Confusables {

    private static final Map<Character, Character> LOOKALIKE_LETTERS = Map.ofEntries(
            // Cyrillic
            Map.entry('а', 'a'), Map.entry('е', 'e'), Map.entry('о', 'o'), Map.entry('р', 'p'),
            Map.entry('с', 'c'), Map.entry('у', 'y'), Map.entry('х', 'x'), Map.entry('і', 'i'),
            Map.entry('ј', 'j'), Map.entry('ѕ', 's'), Map.entry('ԁ', 'd'), Map.entry('һ', 'h'),
            Map.entry('к', 'k'), Map.entry('м', 'm'), Map.entry('т', 't'), Map.entry('в', 'b'),
            Map.entry('ո', 'n'), Map.entry('ս', 'u'),
            // Greek
            Map.entry('ο', 'o'), Map.entry('α', 'a'), Map.entry('ν', 'v'), Map.entry('ι', 'i'),
            Map.entry('κ', 'k'), Map.entry('τ', 't'), Map.entry('ρ', 'p'), Map.entry('υ', 'u'),
            // Latin lookalikes
            Map.entry('ı', 'i'), Map.entry('ɩ', 'i'), Map.entry('ł', 'l'), Map.entry('ɡ', 'g'),
            Map.entry('ß', 'b'));

    private static final Map<Character, Character> LOOKALIKE_DIGITS = Map.of(
            '0', 'o', '1', 'l', '3', 'e', '4', 'a', '5', 's', '7', 't', '8', 'b');

    private Confusables() {}

    /** True when the label has characters outside a-z, 0-9 and "-". */
    public static boolean hasNonAscii(String label) {
        return !label.matches("[a-z0-9-]*");
    }

    /** Swaps lookalike letters from other alphabets for the Latin ones, and drops accents. */
    public static String latinSkeleton(String label) {
        StringBuilder sb = new StringBuilder();
        for (char c : label.toCharArray()) {
            sb.append(LOOKALIKE_LETTERS.getOrDefault(c, c));
        }
        String noAccents = Normalizer.normalize(sb, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
        return noAccents;
    }

    /**
     * Also swaps digits and letter pairs that read as a letter: "g00gle" gives "google" and
     * "rnicrosoft" gives "microsoft". "1" can pass for "l" or "i", so both readings come back.
     */
    public static java.util.Set<String> visualSkeletons(String label) {
        String s = latinSkeleton(label).replace("rn", "m").replace("vv", "w").replace("cl", "d");
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        out.add(swapDigits(s, 'l'));
        out.add(swapDigits(s, 'i'));
        return out;
    }

    private static String swapDigits(String s, char one) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            sb.append(c == '1' ? one : LOOKALIKE_DIGITS.getOrDefault(c, c));
        }
        return sb.toString();
    }
}
